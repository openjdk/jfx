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
 * Clip envelope implementation for a single cycle: cycleCount = 1 or cycleDuration = indefinite
 */
public class SingleLoopClipEnvelope extends ClipEnvelope {

    private int cycleCount;

    protected SingleLoopClipEnvelope(Animation animation) {
        super(animation);
        if (animation != null) {
            cycleCount = animation.getCycleCount();
        }
    }

    @Override
    public void setAutoReverse(boolean autoReverse) {
        // ignore autoReverse
    }

    @Override
    public ClipEnvelope setCycleDuration(Duration cycleDuration) {
        if ((cycleCount != 1) && !cycleDuration.isIndefinite()) {
            return create(animation);
        }
        updateCycleTicks(cycleDuration);
        return this;
    }

    @Override
    public ClipEnvelope setCycleCount(int cycleCount) {
        if ((cycleCount != 1) && (cycleTicks != ClipEnvelope.INDEFINITE)) {
            return create(animation);
        }
        this.cycleCount = cycleCount;
        return this;
    }

    @Override
    public void setRate(double newRate) {
        final Status status = animation.getStatus();
        if (status != Status.STOPPED) {
            setInternalCurrentRate((Math.abs(currentRate - rate) < EPSILON) ? newRate : -newRate);
            deltaTicks = ticks - ticksRateChange(newRate);
            abortCurrentPulse();
        }
        rate = newRate;
    }

    @Override
    protected double calculateCurrentRate() {
        return rate;
    }

    @Override
    public boolean wasSynched() {
        return super.wasSynched() && cycleCount != 0;
    }

    @Override
    public void timePulse(long currentTick) {
        if (cycleTicks == 0L) {
            return;
        }
        aborted = false;
        inTimePulse = true;

        try {
            long ticksChange = Math.round(currentTick * currentRate);
            ticks = Utils.clamp(0, deltaTicks + ticksChange, cycleTicks);
            AnimationAccessor.getDefault().playTo(animation, ticks, cycleTicks);

            final boolean reachedEnd = (currentRate > 0)? (ticks == cycleTicks) : (ticks == 0);
            if(reachedEnd && !aborted) {
                AnimationAccessor.getDefault().finished(animation);
            }
        } finally {
            inTimePulse = false;
        }
    }

    @Override
    public void jumpTo(long ticks) {
        if (cycleTicks == 0L) {
            return;
        }
        final long newTicks = Utils.clamp(0, ticks, cycleTicks);
        deltaTicks += (newTicks - this.ticks);
        this.ticks = newTicks;

        AnimationAccessor.getDefault().jumpTo(animation, newTicks, cycleTicks, false);

        abortCurrentPulse();
    }

}
