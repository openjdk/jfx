/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.scenario.animation;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.javafx.tk.Toolkit;


public class AnimationPulse implements AnimationPulseMBean {
    public static AnimationPulse getDefaultBean() {
        return AnimationPulseHolder.holder;
    }
    private static class AnimationPulseHolder {
        private static final AnimationPulse holder = new AnimationPulse();
    }

    private static class PulseData {
        private final long startNanos;
        private final long scheduledNanos;

        private long animationEndNanos = Long.MIN_VALUE;
        private long paintingStartNanos = Long.MIN_VALUE;
        private long paintingEndNanos = Long.MIN_VALUE;
        private long scenePaintingStartNanos = Long.MIN_VALUE;
        private long scenePaintingEndNanos = Long.MIN_VALUE;
        private long endNanos = Long.MIN_VALUE;

        PulseData(long shiftNanos) {
            startNanos = Toolkit.getToolkit().getPrimaryTimer().nanos();
            scheduledNanos = startNanos + shiftNanos;
        }

        //time from the scheduledNanos
        long getPulseStart(TimeUnit unit) {
            return unit.convert(startNanos - scheduledNanos, TimeUnit.NANOSECONDS);
        }

        void recordAnimationEnd() {
            animationEndNanos = Toolkit.getToolkit().getPrimaryTimer().nanos();
        }

        long getAnimationDuration(TimeUnit unit) {
            return (animationEndNanos > Long.MIN_VALUE)
              ? unit.convert(animationEndNanos - startNanos, TimeUnit.NANOSECONDS)
              : 0;
        }

        long getPaintingDuration(TimeUnit unit) {
            return (paintingEndNanos > Long.MIN_VALUE && paintingStartNanos > Long.MIN_VALUE)
              ? unit.convert(paintingEndNanos - paintingStartNanos, TimeUnit.NANOSECONDS)
              : 0;
        }

        long getScenePaintingDuration(TimeUnit unit) {
            return (scenePaintingEndNanos > Long.MIN_VALUE && scenePaintingStartNanos > Long.MIN_VALUE)
              ? unit.convert(scenePaintingEndNanos - scenePaintingStartNanos, TimeUnit.NANOSECONDS)
              : 0;
        }

        long getPaintingFinalizationDuration(TimeUnit unit) {
            return (scenePaintingEndNanos > Long.MIN_VALUE && paintingEndNanos > Long.MIN_VALUE)
              ? unit.convert(paintingEndNanos - scenePaintingEndNanos, TimeUnit.NANOSECONDS)
              : 0;
        }

        void recordEnd() {
            endNanos = Toolkit.getToolkit().getPrimaryTimer().nanos();
        }

        long getPulseDuration(TimeUnit unit) {
            return unit.convert(endNanos - startNanos, TimeUnit.NANOSECONDS);
        }

        //time from the scheduledNanos
        long getPulseEnd(TimeUnit unit) {
            return unit
                    .convert(endNanos - scheduledNanos, TimeUnit.NANOSECONDS);
        }

        long getPulseStartFromNow(TimeUnit unit) {
            return unit.convert(Toolkit.getToolkit().getPrimaryTimer().nanos() - startNanos,
                    TimeUnit.NANOSECONDS);
        }

        long getSkippedPulses() {
            return getPulseEnd(TimeUnit.MILLISECONDS)
              / AnimationPulse.getDefaultBean().getPULSE_DURATION();
        }

        static interface Accessor {
            public long get(PulseData pulseData, TimeUnit unit);
        }

        static final Accessor PulseStartAccessor = (pulseData1, unit) -> pulseData1.getPulseStart(unit);

        static final Accessor AnimationDurationAccessor = (pulseData1, unit) -> pulseData1.getAnimationDuration(unit);

        static final Accessor PaintingDurationAccessor = (pulseData1, unit) -> pulseData1.getPaintingDuration(unit);

        static final Accessor ScenePaintingDurationAccessor = (pulseData1, unit) -> pulseData1.getScenePaintingDuration(unit);

        static final Accessor PulseDurationAccessor = (pulseData1, unit) -> pulseData1.getPulseDuration(unit);

        static final Accessor PulseEndAccessor = (pulseData1, unit) -> pulseData1.getPulseEnd(unit);

        static final Accessor PaintingPreparationDuration = (pulseData1, unit) -> pulseData1.getPaintingDuration(unit);

        static final Accessor PaintingFinalizationDuration = (pulseData1, unit) -> pulseData1.getPaintingFinalizationDuration(unit);

//        @Override
//        public String toString() {
//            StringBuilder sb = new StringBuilder(super.toString());
//            TimeUnit unit = TimeUnit.MILLISECONDS;
//            sb.append(" start: ").append(getPulseStart(unit))
//            .append(" animation: ").append(getAnimationDuration(unit))
//            .append(" painting: ").append(getPaintingDuration(unit))
//            .append(" pulseDuration: ").append(getPulseDuration(unit))
//            .append(" pulseEnd: ").append(getPulseEnd(unit));
//            return sb.toString();
//        }
    }

    private final Queue<PulseData> pulseDataQueue = new ConcurrentLinkedQueue<>();

    //to be accessed from the EDT
    private PulseData pulseData = null;



    private volatile boolean isEnabled = false;
    @Override
    public boolean getEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled) {
            return;
        }
        isEnabled = enabled;
        //we may want to clean the state on setEanbled(false)
    }

    @Override
    public long getPULSE_DURATION() {
        return Toolkit.getToolkit().getPrimaryTimer().getPulseDuration(1000);
    }


    @Override
    public long getSkippedPulses() {
        return skippedPulses.get();
    }

    @Override
    public long getSkippedPulsesIn1Sec() {
        long rv = 0;
        for (PulseData pulseData : pulseDataQueue) {
            if (pulseData.getPulseStartFromNow(TimeUnit.SECONDS) == 0) {
                rv += pulseData.getSkippedPulses();
            }
        }
        return rv;
    }


    public void recordStart(long shiftMillis) {
        if (! getEnabled()) {
            return;
        }
        pulseData = new PulseData(TimeUnit.MILLISECONDS.toNanos(shiftMillis));
    }

    // cleans items older than 1sec from the queue
    private void purgeOldPulseData() {
        Iterator<PulseData> iterator = pulseDataQueue.iterator();
        while (iterator.hasNext()
                && iterator.next().getPulseStartFromNow(TimeUnit.SECONDS) > 1) {
            iterator.remove();
        }
    }

    private final AtomicLong pulseCounter = new AtomicLong();

    private final AtomicLong startMax = new AtomicLong();
    private final AtomicLong startSum = new AtomicLong();
    private final AtomicLong startAv = new AtomicLong();

    private final AtomicLong endMax = new AtomicLong();
    private final AtomicLong endSum = new AtomicLong();
    private final AtomicLong endAv = new AtomicLong();

    private final AtomicLong animationDurationMax = new AtomicLong();
    private final AtomicLong animationDurationSum = new AtomicLong();
    private final AtomicLong animationDurationAv = new AtomicLong();

    private final AtomicLong paintingDurationMax = new AtomicLong();
    private final AtomicLong paintingDurationSum = new AtomicLong();
    private final AtomicLong paintingDurationAv = new AtomicLong();

    private final AtomicLong pulseDurationMax = new AtomicLong();
    private final AtomicLong pulseDurationSum = new AtomicLong();
    private final AtomicLong pulseDurationAv = new AtomicLong();

    private final AtomicLong[] maxAndAv = new AtomicLong[] {
            startMax, startSum, startAv,
            endMax, endSum, endAv,
            animationDurationMax, animationDurationSum, animationDurationAv,
            paintingDurationMax, paintingDurationSum, paintingDurationAv,
            pulseDurationMax, pulseDurationSum, pulseDurationAv
    };
    private final PulseData.Accessor[] maxAndAvAccessors = new PulseData.Accessor[] {
            PulseData.PulseStartAccessor,
            PulseData.PulseEndAccessor,
            PulseData.AnimationDurationAccessor,
            PulseData.PaintingDurationAccessor,
            PulseData.PulseDurationAccessor
    };

    private void updateMaxAndAv() {
        long pulseCounterLong = pulseCounter.incrementAndGet();
        for (int i = 0; i < maxAndAvAccessors.length; i++) {
            int j = i * 3;
            long tmpLong = maxAndAvAccessors[i].get(pulseData, TimeUnit.MILLISECONDS);
            maxAndAv[j].set(Math.max(maxAndAv[j].get(), tmpLong));
            maxAndAv[j + 1].addAndGet(tmpLong);
            maxAndAv[j + 2].set(maxAndAv[j + 1].get() / pulseCounterLong);
        }
    }

    private final AtomicLong skippedPulses = new AtomicLong();

    private int skipPulses = 100;
    public void recordEnd() {
        if (! getEnabled()) {
            return;
        }
        if (skipPulses > 0) {
            //do not gather data for the first 'skipPulses' pulses
            //let the application to warm up
            skipPulses--;
            pulseData = null;
            return;
        }
        pulseData.recordEnd();
        purgeOldPulseData();
        updateMaxAndAv();
        skippedPulses.addAndGet(pulseData.getSkippedPulses());
        pulseDataQueue.add(pulseData);
        pulseData = null;
    }

    /*
     * implementation detail: I wish we had deque in 1.5 but we do not so here we
     * iterate over the whole thing.
     */
    private long getAv(PulseData.Accessor accessor, long timeOut, TimeUnit unit) {
        if (! getEnabled()) {
            return 0;
        }
        long time = 0;
        long items = 0;
        for (PulseData currentPulseData : pulseDataQueue) {
            if (currentPulseData.getPulseStartFromNow(unit) <= timeOut) {
                time += accessor.get(currentPulseData, unit);
                items++;
            }
        }
        return (items == 0) ? 0 : time / items;
    }

    private long getMax(PulseData.Accessor accessor, long timeOut, TimeUnit unit) {
        if (! getEnabled()) {
            return 0;
        }
        long max = 0;
        for (PulseData currentPulseData : pulseDataQueue) {
            if (currentPulseData.getPulseStartFromNow(unit) <= timeOut) {
                max = Math.max(accessor.get(currentPulseData, unit), max);
            }
        }
        return max;
    }

    @Override
    public long getStartMax() {
        return startMax.get();
    }

    @Override
    public long getStartAv() {
        return startAv.get();
    }

    @Override
    public long getStartMaxIn1Sec() {
        return getMax(PulseData.PulseStartAccessor, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getStartAvIn100Millis() {
        return getAv(PulseData.PulseStartAccessor, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getEndMax() {
        return endMax.get();
    }

    @Override
    public long getEndMaxIn1Sec() {
        return getMax(PulseData.PulseEndAccessor, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getEndAv() {
        return endAv.get();
    }

    @Override
    public long getEndAvIn100Millis() {
        return getAv(PulseData.PulseEndAccessor, 100, TimeUnit.MILLISECONDS);
    }

    public void recordAnimationEnd() {
        if (getEnabled() && pulseData != null) {
            pulseData.recordAnimationEnd();
        }
    }

    @Override
    public long getAnimationDurationMax() {
        return animationDurationMax.get();
    }

    @Override
    public long getAnimationMaxIn1Sec() {
        return getMax(PulseData.AnimationDurationAccessor, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getAnimationDurationAv() {
        return animationDurationAv.get();
    }

    @Override
    public long getAnimationDurationAvIn100Millis() {
        return getAv(PulseData.AnimationDurationAccessor, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getPaintingDurationMax() {
        return paintingDurationMax.get();
    }

    @Override
    public long getPaintingDurationMaxIn1Sec() {
        return getMax(PulseData.PaintingDurationAccessor, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getPaintingDurationAv() {
        return paintingDurationAv.get();
    }

    @Override
    public long getPaintingDurationAvIn100Millis() {
        return getAv(PulseData.PaintingDurationAccessor, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getScenePaintingDurationMaxIn1Sec() {
        return getMax(PulseData.ScenePaintingDurationAccessor, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getPulseDurationMax() {
        return pulseDurationMax.get();
    }

    @Override
    public long getPulseDurationMaxIn1Sec() {
        return getMax(PulseData.PulseDurationAccessor, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getPulseDurationAv() {
        return pulseDurationAv.get();
    }

    @Override
    public long getPulseDurationAvIn100Millis() {
        return getAv(PulseData.PulseDurationAccessor, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getPaintingPreparationDurationMaxIn1Sec() {
        return getMax(PulseData.PaintingPreparationDuration, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getPaintingFinalizationDurationMaxIn1Sec() {
        return getMax(PulseData.PaintingFinalizationDuration, 1000, TimeUnit.MILLISECONDS);
    }
}
