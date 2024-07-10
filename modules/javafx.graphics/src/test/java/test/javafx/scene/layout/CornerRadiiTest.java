/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import javafx.scene.layout.CornerRadii;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class CornerRadiiTest {
    @Test
    public void instanceCreation_singleConstructor() {
        CornerRadii r = new CornerRadii(1);
        assertEquals(1, r.getTopLeftHorizontalRadius(), 0);
        assertEquals(1, r.getTopLeftVerticalRadius(), 0);
        assertEquals(1, r.getTopRightVerticalRadius(), 0);
        assertEquals(1, r.getTopRightHorizontalRadius(), 0);
        assertEquals(1, r.getBottomRightHorizontalRadius(), 0);
        assertEquals(1, r.getBottomRightVerticalRadius(), 0);
        assertEquals(1, r.getBottomLeftVerticalRadius(), 0);
        assertEquals(1, r.getBottomLeftHorizontalRadius(), 0);
        assertFalse(r.isTopLeftHorizontalRadiusAsPercentage());
        assertFalse(r.isTopLeftVerticalRadiusAsPercentage());
        assertFalse(r.isTopRightVerticalRadiusAsPercentage());
        assertFalse(r.isTopRightHorizontalRadiusAsPercentage());
        assertFalse(r.isBottomRightHorizontalRadiusAsPercentage());
        assertFalse(r.isBottomRightVerticalRadiusAsPercentage());
        assertFalse(r.isBottomLeftVerticalRadiusAsPercentage());
        assertFalse(r.isBottomLeftHorizontalRadiusAsPercentage());
    }

    @Test
    public void negativeRadiusNotAllowed_singleConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new CornerRadii(-1));
    }

    @Nested
    class IsUniformTests {
        @Test
        public void isUniform_1ArgConstructor() {
            assertTrue(new CornerRadii(1).isUniform());
        }

        @Test
        public void isUniform_2ArgConstructor() {
            assertTrue(new CornerRadii(1, false).isUniform());
            assertTrue(new CornerRadii(1, true).isUniform());
        }

        @Test
        public void isUniform_5ArgConstructor() {
            for (boolean percent : new boolean[] { true, false }) {
                assertTrue(new CornerRadii(0, 0, 0, 0, percent).isUniform());
                assertTrue(new CornerRadii(1, 1, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 0, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 0, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 0, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 0, 0, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 1, 0, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 0, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 0, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 0, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 1, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 1, 0, 1, percent).isUniform());
            }
        }

        @Test
        public void isUniform_16ArgConstructor() {
            final int max = 1 << 16;
            double[] arg = new double[16];

            // Test all combinations of constructor arguments with a radius of either 0 or 1.
            for (int i = 0; i < max; ++i) {
                boolean uniform = true;

                for (int j = 0; j < 16; ++j) {
                    arg[j] = (i >> j) & 1;
                    uniform &= arg[j] == arg[j < 8 ? 0 : 8]; // args 0-8 must be equal, and args 8-16 must be equal
                }

                var cornerRadii = new CornerRadii(
                    arg[0], arg[1], arg[2], arg[3], arg[4], arg[5], arg[6], arg[7],
                    arg[8] > 0, arg[9] > 0, arg[10] > 0, arg[11] > 0, arg[12] > 0, arg[13] > 0, arg[14] > 0, arg[15] > 0);

                final boolean expectUniform = uniform;

                assertEquals(expectUniform, cornerRadii.isUniform(), () ->
                    "Expected " + (expectUniform ? "" : "not ") + "isUniform for constructor: " +
                    String.join(", ", DoubleStream.of(arg).limit(8).mapToObj(Double::toString).toList()) + ", " +
                    String.join(", ", DoubleStream.of(arg).skip(8).mapToObj(d -> Boolean.toString(d > 0)).toList()));
            }
        }
    }
}
