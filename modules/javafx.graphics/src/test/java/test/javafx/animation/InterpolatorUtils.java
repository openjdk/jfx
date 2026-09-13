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

package test.javafx.animation;

import org.junit.jupiter.api.Assertions;
import javafx.animation.Interpolator;

public class InterpolatorUtils {

    /**
     * Asserts that both interpolators are equal by sampling their outputs.
     */
    public static void assertInterpolatorEquals(Interpolator expected, Interpolator actual) {
        Assertions.assertTrue(equals(expected, actual), "Interpolators do not produce equal outputs");
    }

    /**
     * Determines whether two interpolators are equal by sampling their outputs.
     */
    public static boolean equals(Interpolator int1, Interpolator int2) {
        final int numSamples = 16;

        for (int i = 0; i < numSamples; ++i) {
            double d1 = int1.interpolate(0D, 1D, (double)i / numSamples);
            double d2 = int2.interpolate(0D, 1D, (double)i / numSamples);
            if (Math.abs(d2 - d1) > 0.001) {
                return false;
            }
        }

        return true;
    }

}
