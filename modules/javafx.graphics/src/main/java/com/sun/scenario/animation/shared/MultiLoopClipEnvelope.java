/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Clip envelope for multi-cycle animations. In this case, autoReverse and cyclePosition (which can be different from ticks)
 * are important.
 */
abstract class MultiLoopClipEnvelope extends ClipEnvelope {

    protected boolean autoReverse;

    /**
     * true if the animations was started in the positive direction, false if negative.
     * This is needed to resolve ambiguities in current rate calculation. For example, we have an animation
     * with auto-reverse and a cycle count of 2, and we put the play-head in the middle of cycle 0 from the
     * positive side (or cycle 1 from the negative side). If we set the rate to -1 and play, the play direction
     * can be either:
     * <ul>
     *  <li> positive because we are in a reverse cycle (cycle 1 from the end) of a negative rate
     *  <li> negative because we are playing a positive cycle (cycle 0 from the start) in reverse
     * <ul>
     */
    protected boolean startedPositive;

    /**
     * The current position of the play head in its current cycle.
     * cyclePos = ticks % cycleTicks, so 0 <= cyclePos <= cycleTicks.
     */
    protected long cyclePos;

    protected MultiLoopClipEnvelope(Animation animation) {
        super(animation);
    }

    protected boolean isAutoReverse() {
        return autoReverse;
    }

    @Override
    public void setAutoReverse(boolean autoReverse) {
        this.autoReverse = autoReverse;
    }

    public double calculateCurrentRunningRate() {
        boolean negateRate = isAutoReverse() && !isDuringEffectiveEvenCycle();
        if (!animation.getCuePoints().isEmpty())
            System.out.println("shouldNegateRate = " + negateRate);
        double curRate = negateRate ? -rate : rate;
        return curRate;
    }

    private boolean isDuringEffectiveEvenCycle() {
        int cycleNum = getCycleNum();
        if (!animation.getCuePoints().isEmpty())
            System.out.println("getCycleNum() = " + cycleNum);
        boolean effectiveEven = cycleNum % 2 == 0;
        if (!animation.getCuePoints().isEmpty())
            System.out.println("isDuringEffectiveEvenCycle = " + effectiveEven);
        return effectiveEven;
    }

    @Override
    protected void doPlayTo(double currentRate, long overallDelta, boolean reachedEnd) {
        // delta to reach end of cycle, always >= 0. 0 if at the start/end of a cycle
        long cycleDelta = currentRate > 0 ? cycleTicks - cyclePos : cyclePos;
        System.out.println("cycleDelta = " + cycleDelta);

        // check if the end of the cycle is inside the range of [currentTick, destinationTick]
        // If yes, advance to the end of the cycle and pass the rest of the ticks to the next cycle.
        // If the next cycle is completed, continue to the next etc.
        //long leftoverTicks = Math.abs(overallDelta) - cycleDelta;
        while (overallDelta >= cycleDelta) {
            cyclePos = (currentRate > 0) ? cycleTicks : 0;
            System.out.println("finishing cycle cyclePos = " + cyclePos + " ------------------------");
            AnimationAccessor.getDefault().playTo(animation, cyclePos, cycleTicks);
            if (aborted) {
                return;
            }
            overallDelta -= cycleDelta;
            System.out.println("leftover delta = " + overallDelta);

            if (overallDelta > 0 || !reachedEnd) {
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

        if (overallDelta > 0/* && !reachedEnd */) {
//            cyclePos += Math.signum(currentRate) * overallDelta;
            cyclePos += (currentRate > 0) ? overallDelta : -overallDelta;
            System.out.println("new cyclePos = " + cyclePos);
            AnimationAccessor.getDefault().playTo(animation, cyclePos, cycleTicks);
        }
    }

    @Override
    public void start() {
        super.start();
        startedPositive = rate >= 0; // TODO: rate == 0 should be handled. it's in Animation
    }
}
