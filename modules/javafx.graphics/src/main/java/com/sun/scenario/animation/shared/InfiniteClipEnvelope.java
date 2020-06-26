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
    public int getCycleNum() {
        if (startedPositive) {
            long cycleNum = ticks / cycleTicks;
            long i = ticks >= 0 ? cycleNum : cycleNum - 1;
            System.out.println("getCycleNum effectiveNum = " + i);
            return (int) i;
        } else {
            long cycleNum = -(ticks - cycleTicks) / cycleTicks;
            long i = ticks <= cycleTicks ? cycleNum : cycleNum - 1;
            System.out.println("getCycleNum effectiveNum = " + i);
            return (int) i;
        }
    }

    @Override
    protected boolean hasReachedEnd() {
        return false;
    }

    @Override
    protected long calculateNewTicks(long newDest) {
        return deltaTicks + newDest;
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
        ticks = Math.max(0, newTicks) % (2 * cycleTicks);
        
        System.out.println("jump new ticks = " + ticks);
        final long delta = ticks - oldTicks;
        if (delta == 0) {
            return;
        }
        deltaTicks += delta;
        cyclePos = ticks % cycleTicks;
        if (autoReverse && animation.getStatus() == Status.RUNNING) {
//            double currentRate = calculateCurrentRunningRate();
//            setCurrentRate(currentRate); // needed? a pulse calculates the rate anyway, but needed if cached
//            System.out.println("jump new cur rate = " + currentRate);
        }
        if ((cyclePos == 0) && (ticks != 0)) { // TODO: check when pos = 0 and when pos = cycleticks
            cyclePos = cycleTicks;
        }
        AnimationAccessor.getDefault().jumpTo(animation, cyclePos, cycleTicks, false);
        abortCurrentPulse();
        System.out.println();
    }
}
