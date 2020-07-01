/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.animation.TickCalculation;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.util.Duration;

/**
 * An instance of ClipEnvelope handles the loop-part of a clip.
 *
 * The functionality to react on a pulse-signal from the timer is implemented in
 * two classes: ClipEnvelope and ClipCore. ClipEnvelope is responsible for the
 * "loop-part" (keeping track of the number of cycles, handling the direction of
 * the clip etc.). ClipCore takes care of the inner part (interpolating the
 * values, triggering the action-functions, ...)
 *
 * Both classes have an abstract public definition and can only be created using
 * the factory method create(). The intent is to provide a general
 * implementation plus eventually some fast-track implementations for common use
 * cases.
 */
public abstract class ClipEnvelope {

    protected static final long INDEFINITE = Long.MAX_VALUE;
    protected static final double EPSILON = 1e-12;

    protected Animation animation;

    /**
     * The rate of the animation that is used to calculate the current rate of an animation.
     * It is the same as animation.rate, only ignores animation.rate = 0, so can never be 0.
     */
    protected double rate = 1;

    /**
     * The number of ticks in a single cycle. Calculated from the cycle duration. Always >=0.
     */
    protected long cycleTicks = 0;

    protected long deltaTicks = 0;

    /**
     * The current position of the play head. 0 <= ticks <= totalTicks for finite cycles, any value for infinite cycles.
     */
    protected long ticks = 0;
    protected boolean inTimePulse = false;
    protected boolean aborted = false;

    protected ClipEnvelope(Animation animation) {
        this.animation = animation;
        if (animation != null) {
            updateCycleTicks(animation.getCycleDuration());
            rate = animation.getRate();
        }
    }

    public static ClipEnvelope create(Animation animation) {
        if ((animation.getCycleCount() == 1) || (animation.getCycleDuration().isIndefinite())) {
            return new SingleLoopClipEnvelope(animation);
        } else if (animation.getCycleCount() == Animation.INDEFINITE) {
            return new InfiniteClipEnvelope(animation);
        } else {
            return new FiniteClipEnvelope(animation);
        }
    }

    /**
     * Called by the Animation to updates its ClipEnvelope's autoReverse value.
     * @see Animation#autoReverseProperty()
     */
    public abstract void setAutoReverse(boolean autoReverse);

    /**
     * Called by the Animation to updates its ClipEnvelope's cycleDuration value.
     * @see Animation#cycleDurationProperty()
     */
    public abstract ClipEnvelope setCycleDuration(Duration cycleDuration);

    /**
     * Called by the Animation to updates its ClipEnvelope's cycleCount value.
     * @see Animation#cycleCountProperty()
     */
    public abstract ClipEnvelope setCycleCount(int cycleCount);

    /**
     * Calculates the cycle number the animation is currently at. The number is relative to the starting position of the
     * animation; if the animation if played from the end, ticks == totalTicks will be at cycle 0, if played from the start,
     * it will be at the value of cycleCount.
     */
    public abstract int getCycleNum();

    /**
     * Called by the Animation to updates its ClipEnvelope's rate value. Animation guarantees that newRate != 0.
     * @see Animation#rateProperty()
     */
    public void setRate(double newRate) {
        if (animation.getStatus() != Status.STOPPED) {
            deltaTicks = ticks - Math.round((ticks - deltaTicks) * newRate / rate);
            abortCurrentPulse();
        }
        rate = newRate;
    }

    /**
     * Calculates the {@link Animation#currentRateProperty() currentRate} for a running animation. An Animation will call
     * this method to update its currentRate property.
     * <p>
     * If the animation is not running, this value represents the rate at which the animation will run once it's played.
     * The value is always +rate or -rate (since STOPPED and PAUSED states are ignored). The sign is determined by the
     * position of the play head and by the values of autoReverse and startPositive:<br>
     * If autoReverse == false then currentRate = +rate. Otherwise, if {@link #getCycleNum()} is even then currentRate = +rate,
     * and if odd then currentRate = -rate.
     */
    public abstract double calculateCurrentRunningRate();

    /**
     * Sets the current rate of the Animation by its ClipEnvelope. Called when a cycle ended with autoReverse and the
     * current rate should be flipped.
     */
    protected final void setAnimationCurrentRate(double currentRate) {
        AnimationAccessor.getDefault().setCurrentRate(animation, currentRate);
    }

    protected final void updateCycleTicks(Duration cycleDuration) {
        cycleTicks = TickCalculation.fromDuration(cycleDuration);
    }

    public boolean wasSynched() {
        return cycleTicks != 0;
    }

    public void start() {
        deltaTicks = ticks; // after stopping, ticks is always 0, so this is relevant only if jumpedTo after stop, then start
    }

    public void timePulse(long newDest) {
        if (cycleTicks == 0L) {
            return;
        }
        aborted = false;
        inTimePulse = true;

        try {
            doTimePulse(newDest);
        } finally {
            inTimePulse = false;
        }
    }

    private void doTimePulse(long newDest) {
        if (!animation.getCuePoints().isEmpty())
            System.out.println("dest = " + newDest);

        double currentRate = calculateCurrentRunningRate();
        if (!animation.getCuePoints().isEmpty())
            System.out.println("rate, curRate = " + rate + ", " + currentRate);

        newDest = Math.round(newDest * rate);
        if (!animation.getCuePoints().isEmpty())
            System.out.println("new dest = " + newDest);

        final long oldTicks = ticks;
        ticks = calculatePulseTicks(newDest);
        long ticksChange = Math.abs(ticks - oldTicks);
        if (!animation.getCuePoints().isEmpty()) {
            System.out.println("deltaTicks = " + deltaTicks);
            System.out.println("ticks: " + oldTicks + " -> " + ticks + " = " + ticksChange);
        }

        if (ticksChange == 0) {
            new Throwable().printStackTrace();
            if (!animation.getCuePoints().isEmpty())
                System.out.println("delta = 0");
            return;
        }

        final boolean reachedEnd = hasReachedEnd();
        if (!animation.getCuePoints().isEmpty())
            System.out.println("reachedEnd = " + reachedEnd);

        doPlayTo(currentRate, ticksChange, reachedEnd);

        if (reachedEnd && !aborted) {
            if (!animation.getCuePoints().isEmpty())
                System.out.println("finished");
            AnimationAccessor.getDefault().finished(animation);
        }
        if (!animation.getCuePoints().isEmpty())
            System.out.println();
    }

    protected abstract long calculatePulseTicks(long newDest);
    protected abstract boolean hasReachedEnd();
    protected abstract void doPlayTo(double currentRate, long ticksChange, boolean reachedEnd);

    public abstract void jumpTo(long ticks);

    public final void abortCurrentPulse() {
        if (inTimePulse) {
            aborted = true;
            inTimePulse = false;
        }
    }
}
