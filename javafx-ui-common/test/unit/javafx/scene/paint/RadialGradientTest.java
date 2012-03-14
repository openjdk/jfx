/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */

package javafx.scene.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RadialGradientTest {

    private final Color color1 = Color.rgb(0, 0, 0);
    private final Color color2 = Color.rgb(255, 255, 255);
    private final Stop stop1 = new Stop(0.1f, color1);
    private final Stop stop2 = new Stop(0.2f, color2);
    private final Stop[] noStop = new Stop[0];
    private final Stop[] oneStop = new Stop[] { stop1 };
    private final Stop[] twoStops = new Stop[] { stop1, stop2 };
    private final Stop[] twoStopsWithNulls =
            new Stop[] { stop1, null, stop2, null };
    private final List<Stop> normalizedTwoStops = Arrays.asList(
        new Stop(0.0, color1),
        stop1, stop2,
        new Stop(1.0, color2)
    );

    @Test
    public void testRadialGradient() {
        RadialGradient gradient = new RadialGradient(1f, 2f, 3f, 4f, 5f, true,
                CycleMethod.REPEAT, twoStops);

        assertEquals(1f, gradient.getFocusAngle(), 0.0001);
        assertEquals(2f, gradient.getFocusDistance(), 0.0001);
        assertEquals(3f, gradient.getCenterX(), 0.0001);
        assertEquals(4f, gradient.getCenterY(), 0.0001);
        assertEquals(5f, gradient.getRadius(), 0.0001);
        assertTrue(gradient.isProportional());
        assertEquals(CycleMethod.REPEAT, gradient.getCycleMethod());
        assertEquals(normalizedTwoStops, gradient.getStops());
    }

    @Test
    public void testGetStopsNullsRemoved() {
        RadialGradient gradient = new RadialGradient(0, 0, 1, 1, 2, true,
                CycleMethod.NO_CYCLE, twoStopsWithNulls);

        assertEquals(normalizedTwoStops, gradient.getStops());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testGetStopsCannotChangeGradient() {
        RadialGradient gradient = new RadialGradient(0, 0, 1, 1, 2, true,
                CycleMethod.NO_CYCLE, twoStops);

        List<Stop> returned = gradient.getStops();
        returned.set(0, stop2);
    }

    @Test
    public void testEquals() {
        RadialGradient basic = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient equal = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStopsWithNulls);
        RadialGradient focusAngle = new RadialGradient(2, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient focusDistance = new RadialGradient(1, 1, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient centerX = new RadialGradient(1, 2, 4, 4, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient centerY = new RadialGradient(1, 2, 3, 3, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient radius = new RadialGradient(1, 2, 3, 4, 6, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient proportional = new RadialGradient(1, 2, 3, 4, 5, false,
                CycleMethod.REPEAT, twoStops);
        RadialGradient cycleMethod = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REFLECT, twoStops);
        RadialGradient stops = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, oneStop);

        assertFalse(basic.equals(null));
        assertFalse(basic.equals(color1));
        assertFalse(basic.equals(color2));
        assertTrue(basic.equals(basic));
        assertTrue(basic.equals(equal));
        assertFalse(basic.equals(focusAngle));
        assertFalse(basic.equals(focusDistance));
        assertFalse(basic.equals(centerX));
        assertFalse(basic.equals(centerY));
        assertFalse(basic.equals(radius));
        assertFalse(basic.equals(proportional));
        assertFalse(basic.equals(cycleMethod));
        assertFalse(basic.equals(stops));
    }

    @Test
    public void testHashCode() {
        RadialGradient basic = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient equal = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStops);
        RadialGradient different = new RadialGradient(1, 2, 3, 4, 5, false,
                CycleMethod.REPEAT, twoStops);
        RadialGradient different2 = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, oneStop);

        int code = basic.hashCode();
        int second = basic.hashCode();
        assertTrue(code == second);
        assertTrue(code == equal.hashCode());
        assertFalse(code == different.hashCode());
        assertFalse(code == different2.hashCode());
    }

    @Test
    public void testToString() {
        RadialGradient empty = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, noStop);
        RadialGradient nonempty = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, twoStops);

        String s = empty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());

        s = nonempty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
    
    @Test
    public void testToStringEquals() {
        RadialGradient rg =
            RadialGradient.valueOf("radial-gradient(radius 100%, red  0% , blue 30%,  black 100%)");
        assertEquals(rg, RadialGradient.valueOf(rg.toString()));

        rg = RadialGradient.valueOf("radial-gradient(center 10px 10, radius 100, red 0px, blue 50px,  black 100px)");
        assertEquals(rg, RadialGradient.valueOf(rg.toString()));
        
        rg = RadialGradient.valueOf("radial-gradient(radius 10%, red  0%, blue 30%, black 100%)");
        assertEquals(rg, RadialGradient.valueOf(rg.toString()));
        
        rg = RadialGradient.valueOf("radial-gradient(focus-angle 3.1415926535rad, radius 10%, red  0%, blue 30%, black 100%)");
        assertEquals(rg, RadialGradient.valueOf(rg.toString()));
    }

    @Test
    public void testImpl_getPlatformPaint() {
        RadialGradient gradient = new RadialGradient(1, 2, 3, 4, 5, true,
                CycleMethod.REPEAT, noStop);

        Object paint = gradient.impl_getPlatformPaint();
        assertNotNull(paint);
        assertSame(paint, gradient.impl_getPlatformPaint());
    }

    @Test
    public void testBuilder() {
        RadialGradient gradient = RadialGradientBuilder.create()
                .centerX(2.0)
                .centerY(3.0)
                .cycleMethod(CycleMethod.REPEAT)
                .focusAngle(23)
                .focusDistance(24)
                .proportional(false)
                .radius(17)
                .stops(StopBuilder.create().color(Color.RED).offset(0).build(),
                       StopBuilder.create().color(Color.BLUE).offset(1).build())
                .build();
        assertEquals(2.0, gradient.getCenterX(), 0);
        assertEquals(3.0, gradient.getCenterY(), 0);
        assertSame(CycleMethod.REPEAT, gradient.getCycleMethod());
        assertEquals(23.0, gradient.getFocusAngle(), 0);
        assertEquals(24.0, gradient.getFocusDistance(), 0);
        assertFalse(gradient.isProportional());
        assertEquals(17.0, gradient.getRadius(), 0);
        assertEquals(2, gradient.getStops().size());
        assertSame(Color.BLUE, gradient.getStops().get(1).getColor());
        assertEquals(1.0, gradient.getStops().get(1).getOffset(), 0);
    }

    @Test(expected=NullPointerException.class)
    public void testValueOfNullValue() {
        RadialGradient.valueOf(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testValueOfEmpty() {
        RadialGradient.valueOf("");
    }

    @Test
    public void testValueOfIllegalNotations() {
        try {
            RadialGradient.valueOf("abcd");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            RadialGradient.valueOf("radial-gradient()");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        
        // specifying radius is mandatory
        try {
            RadialGradient.valueOf("radial-gradient(red 30%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testValueOfRelativeAbsoluteMixed() {
        try {
            RadialGradient.valueOf("radial-gradient(center 0% 100%, radius 10px, red  0% , blue 30%,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            RadialGradient.valueOf("radial-gradient(center 0 100, radius 10%, red  0% , blue 30%,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testValueOfRelativeValues() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(radius 100%, red  0% , blue 30%,  black 100%)");
        RadialGradient expected =
                RadialGradientBuilder.create().radius(1)
                .proportional(true)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfAbsoluteValues() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(center 10px 10, radius 100, red 0px, blue 50px,  black 100px)");
        RadialGradient expected =
                RadialGradientBuilder.create().centerX(10).centerY(10).radius(100)
                .proportional(false)
                .stops(new Stop(0, Color.RED),
                       new Stop(.5, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfDefaults() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(radius 10%, red  0%, blue 30%, black 100%)");
        RadialGradient expected =
                RadialGradientBuilder.create().centerX(0).centerY(0)
                .focusAngle(0).focusDistance(0)
                .radius(0.1)
                .proportional(true)
                .cycleMethod(CycleMethod.NO_CYCLE)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfCenter() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(center 20% 40%, radius 10%, red  0%, blue 30%, black 100%)");
        RadialGradient expected =
                RadialGradientBuilder.create().centerX(0.2).centerY(0.4)
                .focusAngle(0).focusDistance(0)
                .radius(0.1)
                .proportional(true)
                .cycleMethod(CycleMethod.NO_CYCLE)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfFocusAngle() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(focus-angle 45deg, radius 10%, red  0%, blue 30%, black 100%)");
        RadialGradientBuilder builder =
                RadialGradientBuilder.create().centerX(0).centerY(0)
                .focusAngle(45).focusDistance(0)
                .radius(0.1)
                .proportional(true)
                .cycleMethod(CycleMethod.NO_CYCLE)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK));
        RadialGradient expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                RadialGradient.valueOf("radial-gradient(focus-angle 3.1415926535rad, radius 10%, red  0%, blue 30%, black 100%)");
        builder.focusAngle(180);
        expected = builder.build();
        assertEquals(expected.getFocusAngle(), actual.getFocusAngle(), 1e-5);

        actual =
                RadialGradient.valueOf("radial-gradient(focus-angle 0.5turn, radius 10%, red  0%, blue 30%, black 100%)");
        builder.focusAngle(180);
        expected = builder.build();
        assertEquals(expected, actual);

        actual =
                RadialGradient.valueOf("radial-gradient(focus-angle 1grad, radius 10%, red  0%, blue 30%, black 100%)");
        builder.focusAngle(0.9);
        expected = builder.build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfFocusDistance() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(focus-distance 20%, radius 10%, red  0%, blue 30%, black 100%)");
        RadialGradient expected =
                RadialGradientBuilder.create().centerX(0).centerY(0)
                .focusAngle(0).focusDistance(0.2)
                .radius(0.1)
                .proportional(true)
                .cycleMethod(CycleMethod.NO_CYCLE)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfCycleMethod() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(radius 10%, repeat, red 0%, blue 30%, black 100%)");
        RadialGradientBuilder builder =
                RadialGradientBuilder.create().centerX(0).centerY(0)
                .focusAngle(0).focusDistance(0)
                .radius(0.1)
                .proportional(true)
                .cycleMethod(CycleMethod.REPEAT)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK));
        RadialGradient expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                RadialGradient.valueOf("radial-gradient(radius 10%, reflect, red 0%, blue 30%, black 100%)");
        builder.cycleMethod(CycleMethod.REFLECT);
        expected = builder.build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfSpecifyAll() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(focus-angle 45deg, focus-distance 20%, center 25% 25%, radius 50%, reflect, gray, darkgray 75%, dimgray)");
        RadialGradient expected =
                RadialGradientBuilder.create().centerX(.25).centerY(0.25).radius(0.5)
                .proportional(true)
                .cycleMethod(CycleMethod.REFLECT)
                .focusAngle(45)
                .focusDistance(0.2)
                .stops(new Stop(0, Color.GRAY),
                       new Stop(0.75, Color.DARKGRAY),
                       new Stop(1.0, Color.DIMGRAY))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfStopsNormalizeLargerValues() {
        RadialGradient actual =
                RadialGradient.valueOf("radial-gradient(radius 10, red  10%, blue 9%, red 8%, black 100%)");
        RadialGradient expected =
                RadialGradientBuilder.create().centerX(0).centerY(0).radius(10)
                .proportional(false)
                .stops(new Stop(0.1, Color.RED),
                       new Stop(0.1, Color.BLUE),
                       new Stop(0.1, Color.RED),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }
}
