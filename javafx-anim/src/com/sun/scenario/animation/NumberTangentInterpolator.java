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
package com.sun.scenario.animation;

import javafx.animation.Interpolator;
import javafx.util.Duration;

public abstract class NumberTangentInterpolator extends Interpolator {

    public abstract double getInValue();

    public abstract double getOutValue();

    public abstract double getInMillis();

    public abstract double getOutMillis();

    public static NumberTangentInterpolator create(final double inValue,
            final Duration inDuration, final double outValue,
            final Duration outDuration) {
        return new NumberTangentInterpolator() {
            @Override
            public double getInValue() {
                return inValue;
            }

            @Override
            public double getOutValue() {
                return outValue;
            }

            @Override
            public double getInMillis() {
                return inDuration.toMillis();
            }

            @Override
            public double getOutMillis() {
                return outDuration.toMillis();
            }

            @Override
            public String toString() {
                return "NumberTangentInterpolator [inValue=" + inValue
                        + ", inDuration=" + inDuration + ", outValue="
                        + outValue + ", outDuration=" + outDuration + "]";
            }
        };
    }

    public static NumberTangentInterpolator create(final double value,
            final Duration duration) {
        return new NumberTangentInterpolator() {
            @Override
            public double getInValue() {
                return value;
            }

            @Override
            public double getOutValue() {
                return value;
            }

            @Override
            public double getInMillis() {
                return duration.toMillis();
            }

            @Override
            public double getOutMillis() {
                return duration.toMillis();
            }

            @Override
            public String toString() {
                return "NumberTangentInterpolator [inValue=" + value
                        + ", inDuration=" + duration + ", outValue=" + value
                        + ", outDuration=" + duration + "]";
            }
        };
    }

    @Override
    protected double curve(double t) {
        // Fallback: If NumberTangentInterpolator is used with a target, that is
        // not a number,
        // it behaves like linear interpolation.
        return t;
    }
}
