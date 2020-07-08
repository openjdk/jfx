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
        long cycleNum;
        if (startedPositive) {
            cycleNum = ticks / cycleTicks;
            cycleNum = ticks >= 0 ? cycleNum : cycleNum - 1;
        } else {
            cycleNum = -(ticks - cycleTicks) / cycleTicks;
            cycleNum = ticks <= cycleTicks ? cycleNum : cycleNum - 1;
        }
        System.out.println("getCycleNum cycleNum = " + cycleNum);
        return (int) cycleNum;
    }

    @Override
    protected boolean hasReachedEnd() {
        return false;
    }

    @Override
    protected long calculateNewTicks(long newDest) {
        return deltaTicks + newDest;
        // ticks = Math.max(0, newTicks) % (2 * cycleTicks); for jump?
        /**
         * {@link Animation#jumpTo(Duration)} already deals with jumping to the end of an infinite animation by jumping to
         * the end of a cycle. 
         */
    }
}
