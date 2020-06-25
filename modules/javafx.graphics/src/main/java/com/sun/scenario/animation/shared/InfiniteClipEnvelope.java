/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Clip envelope implementation for infinite cycles: cycleCount = indefinite
 */
public class InfiniteClipEnvelope extends MultiLoopClipEnvelope {

    protected InfiniteClipEnvelope(Animation animation) {
        super(animation);
        if (!animation.getCuePoints().isEmpty())
            System.out.println("InfiniteClipEnvelope");
        if (animation != null) {
            autoReverse = animation.isAutoReverse();
        }
    }

    @Override
    public ClipEnvelope setCycleDuration(Duration cycleDuration) {
        if (cycleDuration.isIndefinite()) {
            return create(animation);
        }
        updateCycleTicks(cycleDuration);
        return this;
    }

    @Override
    public ClipEnvelope setCycleCount(int cycleCount) {
       return (cycleCount != Animation.INDEFINITE) ? create(animation) : this;
    }

    @Override
    public void setRate(double newRate) {
        if (animation.getStatus() != Status.STOPPED) {
//            boolean switchedDirection = false;// isDirectionChanged(rate);
//            deltaTicks = ticks + (switchedDirection ? ticksRateChange(rate) : -ticksRateChange(rate));
//            if (switchedDirection) {
//                final long delta = 2 * cycleTicks - cyclePos;
//                deltaTicks += delta;
//                ticks += delta;
//            }
            deltaTicks = ticks - ticksRateChange(newRate);
            abortCurrentPulse();
        }
        System.out.println("");
        rate = newRate;
    }

    @Override
    public int getCycleNum() {
        long effectiveTicks = startedPositive ? ticks : ticks +  cycleTicks;
        return (int) (effectiveTicks / cycleTicks);
    }

    @Override
    public void timePulse(long newDest) {
        if (cycleTicks == 0L) {
            return;
        }
        aborted = false;
        inTimePulse = true;

        System.out.println("dest = " + newDest);

        try {
            double currentRate = calculateCurrentRunningRate();
            System.out.println("rate, curRate = " + rate + ", " + currentRate);

            newDest = Math.round(newDest * rate);
            System.out.println("new dest = " + newDest);

            final long oldTicks = ticks;
            ticks = newDest + deltaTicks;
            long overallDelta = Math.abs(ticks - oldTicks); // overall delta between current position and new position. always >= 0
            System.out.println("deltaTicks = " + deltaTicks);
            System.out.println("ticks: " + oldTicks + " -> " + ticks + " = " + overallDelta);

            if (overallDelta == 0) {
                System.out.println("delta = 0");
//                return;
            }

            System.out.println("cyclePos = " + cyclePos);

            // delta to reach end of cycle, always >= 0. 0 if at the start/end of a cycle
            long cycleDelta = currentRate > 0 ? cycleTicks - cyclePos : cyclePos;
            System.out.println("cycleDelta = " + cycleDelta);

            // check if the end of the cycle is inside the range of [currentTick, destinationTick]
            // If yes, advance step by step
            while (overallDelta >= cycleDelta) {
                cyclePos = (currentRate > 0) ? cycleTicks : 0;
                System.out.println("finishing cycle cyclePos = " + cyclePos + " ------------------------");
                AnimationAccessor.getDefault().playTo(animation, cyclePos, cycleTicks);
                if (aborted) {
                    return;
                }
                overallDelta -= cycleDelta;
                System.out.println("leftover delta = " + overallDelta);

                if (overallDelta > 0) {
                    if (autoReverse) { // change direction
                        setCurrentRate(-currentRate);
                        currentRate = -currentRate;
                        System.out.println("switching direction to " + currentRate + " ------------------------");
                    } else { // jump back to the the cycle
                        cyclePos = (currentRate > 0) ? 0 : cycleTicks;
                        System.out.println("restaring cycle cyclePos = " + cyclePos + " ------------------------");
                        AnimationAccessor.getDefault().jumpTo(animation, cyclePos, cycleTicks, false);
                    }
                }
                cycleDelta = cycleTicks;
            }

            if (overallDelta > 0) {
                cyclePos += (currentRate > 0) ? overallDelta : -overallDelta;
                System.out.println("new cyclePos = " + cyclePos);
                AnimationAccessor.getDefault().playTo(animation, cyclePos, cycleTicks);
            }

            System.out.println();

        } finally {
            inTimePulse = false;
        }
    }

    /**
     * {@link Animation#jumpTo(Duration)} already deals with jumping to the end of an infinite animation by jumping to
     * the end of a cycle. 
     */
    @Override
    public void jumpTo(long newTicks) {
        if (cycleTicks == 0L) {
            return;
        }
        final long oldTicks = ticks;
        ticks = Math.max(0, newTicks) % (2 * cycleTicks + 1);
        
        System.out.println("jump new ticks = " + ticks);
        final long delta = ticks - oldTicks;
        if (delta == 0) {
            return;
        }
        deltaTicks += delta;
        cyclePos = ticks % cycleTicks;
        if (autoReverse && animation.getStatus() == Status.RUNNING) {
            double currentRate = calculateCurrentRunningRate();
            setCurrentRate(currentRate); // needed? a pulse calculates the rate anyway, but needed if cached
            System.out.println("jump new cur rate = " + currentRate);
        }
        if ((cyclePos == 0) && (ticks != 0)) { // TODO: check when pos = 0 and when pos = cycleticks
            cyclePos = cycleTicks;
        }
        AnimationAccessor.getDefault().jumpTo(animation, cyclePos, cycleTicks, false);
        abortCurrentPulse();
        System.out.println();
    }
}
