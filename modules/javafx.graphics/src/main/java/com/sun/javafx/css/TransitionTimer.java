/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.util.Utils;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.css.StyleableProperty;
import javafx.css.TransitionDefinition;

/**
 * {@code TransitionTimer} is the base class for timers that compute intermediate
 * values for implicit transitions of a {@link StyleableProperty}.
 */
public abstract class TransitionTimer extends AnimationTimer {

    private final long startTime, duration;
    private final Interpolator interpolator;
    private boolean updating;

    protected TransitionTimer(TransitionDefinition transition) {
        this.startTime = System.nanoTime() + (long)(transition.getDelay().toMillis() * 1000000);
        this.duration = (long)(transition.getDuration().toMillis() * 1000000);
        this.interpolator = transition.getInterpolator();
    }

    /**
     * Stops the specified timer if it is currently running, but only if this method
     * was not called from the timer's {@link #onUpdate(double)} method (i.e. a timer
     * will not stop itself).
     * If {@code timer} is {@code null}, it is considered to be trivially stopped, so
     * the method returns {@code true}.
     *
     * @param timer the timer
     * @return {@code true} if the timer was stopped or {@code timer} is {@code null},
     *         {@code false} otherwise
     */
    public static boolean tryStop(TransitionTimer timer) {
        if (timer == null) {
            return true;
        }

        if (timer.updating) {
            return false;
        }

        timer.stop();
        return true;
    }

    @Override
    public final void handle(long now) {
        double progress = Utils.clamp(
            interpolator.isValidBeforeInterval() ? Double.NEGATIVE_INFINITY : 0,
            (double)(now - startTime) / (double)duration,
            1);

        try {
            updating = true;
            onUpdate(interpolator.interpolate(0D, 1D, progress));
        } finally {
            updating = false;
        }

        if (progress == 1) {
            stop();
        }
    }

    /**
     * Derived classes should implement this method to compute a new intermediate value
     * based on the current progress, and update the {@code StyleableProperty} accordingly.
     *
     * @param progress the progress of the transition, ranging from 0 to 1
     */
    protected abstract void onUpdate(double progress);

}
