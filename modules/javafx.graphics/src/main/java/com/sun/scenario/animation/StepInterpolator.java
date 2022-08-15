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

package com.sun.scenario.animation;

import javafx.animation.Interpolator;
import javafx.animation.StepPosition;
import java.util.Objects;

/**
 * Implementation of a step interpolator as described by
 * <a href="https://www.w3.org/TR/css-easing-1/#step-easing-algo">CSS Easing Functions Level 1</a>
 */
public class StepInterpolator extends Interpolator {

    private final int intervals;
    private final StepPosition position;

    public StepInterpolator(int intervals, StepPosition position) {
        if (position == StepPosition.NONE && intervals <= 1) {
            throw new IllegalArgumentException("intervals must be greater than 1");
        }

        if (intervals <= 0) {
            throw new IllegalArgumentException("intervals must be greater than 0");
        }

        this.position = Objects.requireNonNull(position, "position cannot be null");
        this.intervals = intervals;
    }

    @Override
    public boolean isValidBeforeInterval() {
        return true;
    }

    @Override
    protected double curve(double t) {
        boolean before = t < 0;

        if (before) {
            t = 0;
        } else if (t > 1) {
            t = 1;
        }

        int step = (int)(t * intervals);

        if (position == StepPosition.START || position == StepPosition.BOTH) {
            ++step;
        }

        if (before && (t * intervals % 1 == 0)) {
            --step;
        }

        if (t >= 0 && step < 0) {
            step = 0;
        }

        int jumps = switch (position) {
            case START, END -> intervals;
            case NONE -> intervals - 1;
            case BOTH -> intervals + 1;
        };

        if (t <= 1 && step > jumps) {
            step = jumps;
        }

        return (double)step / jumps;
    }

    @Override
    public String toString() {
        return "StepInterpolator [intervals=" + intervals + ", position=" + position + "]";
    }

}
