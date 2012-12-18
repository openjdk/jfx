/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

public class ClipEnvelopeMock extends ClipEnvelope {

    public long getTimelineTicks() {
        return cycleTicks;
    }

    public double getRate() {
        return rate;
    }
    private boolean autoReverse;

    public boolean getAutoReverse() {
        return autoReverse;
    }

    @Override
    public void setAutoReverse(boolean autoReverse) {
        this.autoReverse = autoReverse;
    }
    private int cycleCount;

    public int getCycleCount() {
        return cycleCount;
    }
    private long lastJumpTo;

    public long getLastJumpTo() {
        final long v = lastJumpTo;
        lastJumpTo = 0L;
        return v;
    }
    private long lastTimePulse;

    public long getLastTimePulse() {
        final long v = lastTimePulse;
        lastTimePulse = 0L;
        return v;
    }

    public ClipEnvelopeMock() {
        super(null);
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
        setCycleDuration(animation.getCycleDuration());
    }

    @Override
    public boolean wasSynched() {
        return true;
    }

    @Override
    public ClipEnvelope setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
        return this;
    }

    @Override
    public void timePulse(long currentTick) {
        lastTimePulse = currentTick;
    }

    @Override
    public void jumpTo(long ticks) {
        lastJumpTo = ticks;
        // Emulate what ClipEnvelope is supposed to do
        while (ticks > cycleTicks) {
            ticks -= cycleTicks;
        }
        AnimationAccessor.getDefault().jumpTo(animation, ticks, cycleTicks, false);
    }

    @Override
    public ClipEnvelope setCycleDuration(Duration cycleDuration) {
        updateCycleTicks(cycleDuration);
        return this;
    }

    @Override
    public void setRate(double rate) {
        this.rate = rate;
    }

    @Override
    protected double calculateCurrentRate() {
        return rate;
    }
}