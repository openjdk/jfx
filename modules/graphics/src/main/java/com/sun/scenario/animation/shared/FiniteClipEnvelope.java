/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.animation.shared;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.util.Duration;

public class FiniteClipEnvelope extends ClipEnvelope {

    private boolean autoReverse;
    private int cycleCount;
    private long totalTicks;
    private long pos;

    protected FiniteClipEnvelope(Animation animation) {
        super(animation);
        if (animation != null) {
            autoReverse = animation.isAutoReverse();
            cycleCount = animation.getCycleCount();
        }
        updateTotalTicks();
    }

    @Override
    public void setAutoReverse(boolean autoReverse) {
        this.autoReverse = autoReverse;
    }

    @Override
    protected double calculateCurrentRate() {
        return !autoReverse? rate
                : (ticks % (2 * cycleTicks) < cycleTicks) == (rate > 0)? rate : -rate;
    }

    @Override
    public ClipEnvelope setCycleDuration(Duration cycleDuration) {
        if (cycleDuration.isIndefinite()) {
            return create(animation);
        }
        updateCycleTicks(cycleDuration);
        updateTotalTicks();
        return this;
    }

    @Override
    public ClipEnvelope setCycleCount(int cycleCount) {
        if ((cycleCount == 1) || (cycleCount == Animation.INDEFINITE)) {
            return create(animation);
        }
        this.cycleCount = cycleCount;
        updateTotalTicks();
        return this;
    }

    @Override
    public void setRate(double rate) {
        final boolean toggled = rate * this.rate < 0;
        final long newTicks = toggled? totalTicks - ticks : ticks;
        final Status status = animation.getStatus();
        if (status != Status.STOPPED) {
            if (status == Status.RUNNING) {
                setCurrentRate((Math.abs(currentRate - this.rate) < EPSILON) ? rate : -rate);
            }
            deltaTicks = newTicks - Math.round((ticks - deltaTicks) * Math.abs(rate / this.rate));
            abortCurrentPulse();
        }
        ticks = newTicks;
        this.rate = rate;
    }

    private void updateTotalTicks() {
        totalTicks = cycleCount * cycleTicks;
    }

    @Override
    public void timePulse(long currentTick) {
        if (cycleTicks == 0L) {
            return;
        }
        aborted = false;
        inTimePulse = true;

        try {
            final long oldTicks = ticks;
            ticks = ClipEnvelope.checkBounds(deltaTicks + Math.round(currentTick * Math.abs(rate)), totalTicks);

            final boolean reachedEnd = ticks >= totalTicks;

            long overallDelta = ticks - oldTicks; // overall delta between current position and new position
            if (overallDelta == 0) {
                return;
            }

            long cycleDelta = (currentRate > 0)? cycleTicks - pos : pos; // delta to reach end of cycle

            while (overallDelta >= cycleDelta) {
                if (cycleDelta > 0) {
                    pos = (currentRate > 0)? cycleTicks : 0;
                    overallDelta -= cycleDelta;
                    AnimationAccessor.getDefault().playTo(animation, pos, cycleTicks);
                    if (aborted) {
                        return;
                    }
                }

                if (!reachedEnd || (overallDelta > 0)) {
                    if (autoReverse) {
                        setCurrentRate(-currentRate);
                    } else {
                        pos = (currentRate > 0)? 0 : cycleTicks;
                        AnimationAccessor.getDefault().jumpTo(animation, pos, cycleTicks, false);
                    }
                }
                cycleDelta = cycleTicks;
            }

            if (overallDelta > 0 && !reachedEnd) {
                pos += (currentRate > 0)? overallDelta : -overallDelta;
                AnimationAccessor.getDefault().playTo(animation, pos, cycleTicks);
            }

            if(reachedEnd && !aborted) {
                AnimationAccessor.getDefault().finished(animation);
            }

        } finally {
            inTimePulse = false;
        }
    }

    @Override
    public void jumpTo(long newTicks) {
        if (cycleTicks == 0L) {
            return;
        }

        final long oldTicks = ticks;
        if (rate < 0) {
            newTicks = totalTicks - newTicks;
        }
        ticks = ClipEnvelope.checkBounds(newTicks, totalTicks);
        final long delta = ticks - oldTicks;
        if (delta != 0) {
            deltaTicks += delta;
            if (autoReverse) {
                final boolean forward = ticks % (2 * cycleTicks) < cycleTicks;
                if (forward == (rate > 0)) {
                    pos = ticks % cycleTicks;
                    if (animation.getStatus() == Status.RUNNING) {
                        setCurrentRate(Math.abs(rate));
                    }
                } else {
                    pos = cycleTicks - (ticks % cycleTicks);
                    if (animation.getStatus() == Status.RUNNING) {
                        setCurrentRate(-Math.abs(rate));
                    }
                }
            } else {
                pos = ticks % cycleTicks;
                if (rate < 0) {
                    pos = cycleTicks - pos;
                }
                if ((pos == 0) && (ticks > 0)) {
                    pos = cycleTicks;
                }
            }

            AnimationAccessor.getDefault().jumpTo(animation, pos, cycleTicks, false);
            abortCurrentPulse();
        }
    }

}
