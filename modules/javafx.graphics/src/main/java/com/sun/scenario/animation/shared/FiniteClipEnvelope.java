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

import com.sun.javafx.util.Utils;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.util.Duration;

/**
 * Clip envelope implementation for multi-cycles: cycleCount != (1 or indefinite) and cycleDuration != indefinite
 */
public class FiniteClipEnvelope extends MultiLoopClipEnvelope {

    private int cycleCount;
    private long totalTicks;

    protected FiniteClipEnvelope(Animation animation) {
        super(animation);
        if (!animation.getCuePoints().isEmpty())
            System.out.println("FiniteClipEnvelope");
        if (animation != null) {
            autoReverse = animation.isAutoReverse();
            cycleCount = animation.getCycleCount();
        }
        updateTotalTicks();
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

    private void updateTotalTicks() {
        totalTicks = cycleCount * cycleTicks;
    }

    @Override
    public int getCycleNum() {
        long effectiveTicks = startedPositive ? ticks : totalTicks - ticks;
        return (int) (effectiveTicks / cycleTicks);
    }

    @Override
    protected boolean hasReachedEnd() {
        return rate > 0 ? ticks == totalTicks : ticks == 0;
    }

    @Override
    protected long calculatePulseTicks(long newDest) {
        return Utils.clamp(0, deltaTicks + newDest, totalTicks);
    }

    @Override
    public void jumpTo(long newTicks) {
        if (cycleTicks == 0L) {
            return;
        }

        final long oldTicks = ticks;
        ticks = Utils.clamp(0, newTicks, totalTicks);
        final long delta = ticks - oldTicks;
//        System.out.println("delta = " + delta);
        if (delta == 0) {
//            System.out.println();
//            return;
        }
        deltaTicks += delta;
        cyclePos = ticks % cycleTicks;
        if (autoReverse && animation.getStatus() == Status.RUNNING) {
             setAnimationCurrentRate(calculateCurrentRunningRate()); // needed? a pulse calculates the rate anyway, but needed if cached
        } 
        if ((cyclePos == 0) && (ticks != 0)) { // TODO: check when pos = 0 and when pos = cycleticks
            cyclePos = cycleTicks;
        }

//        System.out.println("pos = " + TickCalculation.toMillis(pos));
//        System.out.println("rate = " + rate);
        AnimationAccessor.getDefault().jumpTo(animation, cyclePos, cycleTicks, false);
        abortCurrentPulse();

//        System.out.println();
    }
}
