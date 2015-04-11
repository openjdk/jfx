/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.utils3d.animation;

import javafx.util.Duration;

public class TickCalculation {
    public static final int TICKS_PER_SECOND = 6000;
    private static final double TICKS_PER_MILI = TICKS_PER_SECOND / 1000.0;
    private static final double TICKS_PER_NANO = TICKS_PER_MILI * 1e-6;

    private TickCalculation() {
    }

    public static long add(long op1, long op2) {
        assert (op1 >= 0);

        if (op1 == Long.MAX_VALUE || op2 == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        } else if (op2 == Long.MIN_VALUE) {
            return 0;
        }

        if (op2 >= 0) {
            final long result = op1 + op2;
            return (result < 0) ? Long.MAX_VALUE : result;
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
