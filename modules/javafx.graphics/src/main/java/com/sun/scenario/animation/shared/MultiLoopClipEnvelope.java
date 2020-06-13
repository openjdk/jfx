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
        double curRate = (!isAutoReverse() || isDuringEvenCycle()) ? rate : -rate;
//        if (autoReverse && !isDuringEvenCycle()) {
//            curRate = -curRate;
//        }
        return curRate;
    }

    protected boolean isDuringEvenCycle() {
//      if (rate > 0) {
        // 0 - 11999 T
        // 12k - 23999 F
        // 24k - 25999 T
        // 36k F
        boolean b = ticks % (2 * cycleTicks) < cycleTicks;
        System.out.println("isDuringEvenCycle = " + b);
        return b;
//      } else {
        // 0 F
        // 1 - 12k T
        // 12001 - 24k F
        // 24001 - 36k T
//          boolean b = ticks % (2 * cycleTicks) < cycleTicks;
//          return b;
//      }
    }

    protected boolean isDirectionChanged(double newRate) {
        return newRate * rate < 0;
    }

    @Override
    public void start() {
        super.start();
        startedPositive = rate >= 0; // TODO: rate == 0 should be handled. it's in Animation
    }
}
