/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

/**
 * Implementation of a step interpolator as described by
 * <a href="https://www.w3.org/TR/css-easing-1/#step-easing-algo">CSS Easing Functions Level 1</a>
 */
public final class StepInterpolator extends Interpolator {

    private final int intervalCount;
    private final StepPosition position;

    public StepInterpolator(int intervalCount, StepPosition position) {
        if (position == StepPosition.NONE && intervalCount <= 1) {
            throw new IllegalArgumentException("intervalCount must be greater than 1");
        }

        if (intervalCount <= 0) {
            throw new IllegalArgumentException("intervalCount must be greater than 0");
        }

        this.position = Objects.requireNonNull(position, "position cannot be null");
        this.intervalCount = intervalCount;
    }

    @Override
    protected double curve(double t) {
        // JavaFX interpolators are not usually valid outside the interval [0..1], but
        // this implementation ensures that the output value is correct even for points
        // on the curve that are outside of this interval.
        boolean before = t < 0;

        if (before) {
            t = 0;
        } else if (t > 1) {
            t = 1;
        }

        int step = (int)(t * intervalCount);

        if (position == StepPosition.START || position == StepPosition.BOTH) {
            ++step;
        }

        if (before && (t * intervalCount % 1 == 0)) {
            --step;
        }

        if (t >= 0 && step < 0) {
            step = 0;
        }

        int jumps = switch (position) {
            case START, END -> intervalCount;
            case NONE -> intervalCount - 1;
            case BOTH -> intervalCount + 1;
        };

        if (t <= 1 && step > jumps) {
            step = jumps;
        }

        return (double)step / jumps;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(intervalCount) + 31 * position.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StepInterpolator other
            && intervalCount == other.intervalCount
            && position == other.position;
    }

    @Override
    public String toString() {
        return "StepInterpolator [intervalCount=" + intervalCount + ", position=" + position + "]";
    }

}
