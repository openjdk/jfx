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

import javafx.animation.AnimationTimer;

/**
 * {@code StyleableTimer} is the base class for timers that compute intermediate
 * values for styleable properties.
 *
 * @see TransitionTimer
 */
public abstract class StyleableTimer extends AnimationTimer {

    /**
     * Stops the specified timer if it is currently running, but only if this method was not
     * called from the timer's {@code onUpdate} method (i.e. a timer will not stop itself).
     * If {@code timer} is {@code null}, it is considered to be trivially stopped, so the
     * method returns {@code true}.
     *
     * @param timer the timer
     * @return {@code true} if the timer was stopped or {@code timer} is {@code null},
     *         {@code false} otherwise
     */
    public static boolean tryStop(StyleableTimer timer) {
        if (timer == null) {
            return true;
        }

        if (timer.isUpdating()) {
            return false;
        }

        timer.stop();
        return true;
    }

    /**
     * Returns whether the timer is currently updating the value of the {@code StyleableProperty}.
     */
    public abstract boolean isUpdating();

}
