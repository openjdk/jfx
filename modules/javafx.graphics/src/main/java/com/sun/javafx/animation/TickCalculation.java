/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.animation;

import javafx.util.Duration;

public class TickCalculation {
    public static final int TICKS_PER_SECOND = 6000;
    private static final double TICKS_PER_MILI = TICKS_PER_SECOND / 1000.0;
    private static final double TICKS_PER_NANO =  TICKS_PER_MILI * 1e-6;

    private TickCalculation() {}

    public static long add(long op1, long op2) {
        assert (op1 >= 0);

        if (op1 == Long.MAX_VALUE || op2 == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        } else if (op2 == Long.MIN_VALUE) {
            return 0;
        }

        if (op2 >= 0) {
            final long result = op1 + op2;
            return (result < 0)? Long.MAX_VALUE : result;
        } else {
            return Math.max(0, op1 + op2);
        }

    }

    public static long sub(long op1, long op2) {
        assert (op1 >= 0);

        if (op1 == Long.MAX_VALUE || op2 == Long.MIN_VALUE) {
            return Long.MAX_VALUE;
        } else if (op2 == Long.MAX_VALUE) {
            return 0;
        }

        if (op2 >= 0) {
            return Math.max(0, op1 - op2);
        } else {
            final long result = op1 - op2;
            return result < 0 ? Long.MAX_VALUE : result;
        }

    }

    public static long fromMillis(double millis) {
        return Math.round(TICKS_PER_MILI * millis);
    }

    public static long fromNano(long nano) {
        return Math.round(TICKS_PER_NANO * nano);
    }

    public static long fromDuration(Duration duration) {
        return fromMillis(duration.toMillis());
    }

    public static long fromDuration(Duration duration, double rate) {
        return Math.round(TICKS_PER_MILI * duration.toMillis() / Math.abs(rate));
    }

    public static Duration toDuration(long ticks) {
        return Duration.millis(toMillis(ticks));
    }

    public static double toMillis(long ticks) {
        return ticks / TICKS_PER_MILI;
    }


}
