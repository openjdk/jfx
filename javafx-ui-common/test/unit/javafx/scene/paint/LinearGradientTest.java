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

public class LinearGradientTest {

    private final Color color1 = Color.rgb(0, 0, 0);
    private final Color color2 = Color.rgb(255, 255, 255);
    private final Stop stop1 = new Stop(0.1, color1);
    private final Stop stop2 = new Stop(0.2, color2);
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
    public void testLinearGradient() {
        LinearGradient gradient = new LinearGradient(1f, 2f, 3f, 4f, true,
                CycleMethod.REPEAT, twoStops);

        assertEquals(1f, gradient.getStartX(), 0.0001);
        assertEquals(2f, gradient.getStartY(), 0.0001);
        assertEquals(3f, gradient.getEndX(), 0.0001);
        assertEquals(4f, gradient.getEndY(), 0.0001);
        assertTrue(gradient.isProportional());
        assertEquals(CycleMethod.REPEAT, gradient.getCycleMethod());
        assertEquals(normalizedTwoStops, gradient.getStops());
    }

    @Test
    public void testGetStopsNullsRemoved() {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE, twoStopsWithNulls);

        assertEquals(normalizedTwoStops, gradient.getStops());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testGetStopsCannotChangeGradient() {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE, twoStopsWithNulls);

        List<Stop> returned = gradient.getStops();
        returned.set(0, stop2);
    }

    @Test
    public void testEquals() {
        LinearGradient basic = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient equal = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStopsWithNulls);
        LinearGradient beginX = new LinearGradient(2, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient beginY = new LinearGradient(1, 1, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient endX = new LinearGradient(1, 2, 4, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient endY = new LinearGradient(1, 2, 3, 3, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient proportional = new LinearGradient(1, 2, 3, 4, false,
                CycleMethod.REPEAT, twoStops);
        LinearGradient cycleMethod = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REFLECT, twoStops);
        LinearGradient stops = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, oneStop);

        assertFalse(basic.equals(null));
        assertFalse(basic.equals(color1));
        assertFalse(basic.equals(color2));
        assertTrue(basic.equals(basic));
        assertTrue(basic.equals(equal));
        assertFalse(basic.equals(beginX));
        assertFalse(basic.equals(beginY));
        assertFalse(basic.equals(endX));
        assertFalse(basic.equals(endY));
        assertFalse(basic.equals(proportional));
        assertFalse(basic.equals(cycleMethod));
        assertFalse(basic.equals(stops));
    }

    @Test
    public void testHashCode() {
        LinearGradient basic = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient equal = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient different = new LinearGradient(1, 2, 3, 4, false,
                CycleMethod.REPEAT, twoStops);
        LinearGradient different2 = new LinearGradient(1, 2, 3, 4, true,
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
        LinearGradient empty = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, noStop);
        LinearGradient nonempty = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);

        String s = empty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());

        s = nonempty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    public void testImpl_getPlatformPaint() {
        LinearGradient gradient = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, noStop);
        
        Object paint = gradient.impl_getPlatformPaint();
        assertNotNull(paint);
        assertSame(paint, gradient.impl_getPlatformPaint());
    }

    @Test(expected=NullPointerException.class)
    public void testValueOfNullValue() {
        LinearGradient.valueOf(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testValueOfEmpty() {
        LinearGradient.valueOf("");
    }

    @Test
    public void testValueOfIllegalNotations() {
        try {
            LinearGradient.valueOf("abcd");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient()");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
    
    @Test
    public void testValueOfRelativeAbsoluteMixed() {
        try {
            LinearGradient.valueOf("linear-gradient(from 0px 100% to 100% 100%, red  0% , blue 30%,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0% 100% to 100px 100%, red  0% , blue 30%,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0% 100% to 100% 100%, red  0% , blue 30px,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0 100 to 100 100%, red  0% , blue 30px,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

    }

    @Test
    public void testValueOfCycleMethod() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(repeat, red  0%, blue  30%,black 100%)");
        LinearGradientBuilder builder =
                LinearGradientBuilder.create().startX(0).startY(0).endX(0).endY(1)
                .proportional(true)
                .cycleMethod(CycleMethod.REPEAT)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK));
        LinearGradient expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(reflect, red  0%, blue 30%, black 100%)");
        builder.cycleMethod(CycleMethod.REFLECT);
        expected = builder.build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfFromTo() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(from 10% 20% to 30% 40%, red  0%, blue  30%,black 100%)");
        LinearGradientBuilder builder =
                LinearGradientBuilder.create().startX(0.1).startY(0.2).endX(0.3).endY(0.4)
                .proportional(true)
                .cycleMethod(CycleMethod.NO_CYCLE)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK));
        LinearGradient expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(from 10px 20px to 30px 40px, red  0%, blue 30%, black 100%)");
        builder.startX(10).startY(20).endX(30).endY(40).proportional(false);
        expected = builder.build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfTo() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(to top, red  0%, blue  30%,black 100%)");
        LinearGradientBuilder builder =
                LinearGradientBuilder.create().startX(0).startY(1).endX(0).endY(0)
                .proportional(true)
                .cycleMethod(CycleMethod.NO_CYCLE)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK));
        LinearGradient expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(to bottom, red  0%, blue  30%,black 100%)");
        builder.startX(0).startY(0).endX(0).endY(1);
        expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(to left, red  0%, blue  30%,black 100%)");
        builder.startX(1).startY(0).endX(0).endY(0);
        expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(to right, red  0%, blue  30%,black 100%)");
        builder.startX(0).startY(0).endX(1).endY(0);
        expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(to bottom left, red  0%, blue  30%,black 100%)");
        builder.startX(1).startY(0).endX(0).endY(1);
        expected = builder.build();
        assertEquals(expected, actual);
        
        actual =
                LinearGradient.valueOf("linear-gradient(to right top, red  0%, blue  30%,black 100%)");
        builder.startX(0).startY(1).endX(1).endY(0);
        expected = builder.build();
        assertEquals(expected, actual);
    }
    
    @Test
    public void testValueOfRelativeValues() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(from 0% 0% to 100% 100%, red  0% , blue 30%,  black 100%)");
        LinearGradient expected =
                LinearGradientBuilder.create().startX(0).startY(0).endX(1).endY(1)
                .proportional(true)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }
    
    @Test
    public void testValueOfAbsoluteValues() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(from 100px 0 to 200px 0px, red 0px, blue 50px,  black 100px)");
        LinearGradient expected =
                LinearGradientBuilder.create().startX(100).startY(0).endX(200).endY(0)
                .proportional(false)
                .stops(new Stop(0, Color.RED),
                       new Stop(.5, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }
    
    @Test
    public void testValueOfDefaultsToBottom() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(red  0%, blue 30%, black 100%)");
        LinearGradient expected =
                LinearGradientBuilder.create().startX(0).startY(0).endX(0).endY(1)
                .proportional(true)
                .stops(new Stop(0, Color.RED),
                       new Stop(.3, Color.BLUE),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfStopsNormalizeLargerValues() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(red  10%, blue 9%, red 8%, black 100%)");
        LinearGradient expected =
                LinearGradientBuilder.create().startX(0).startY(0).endX(0).endY(1)
                .proportional(true)
                .stops(new Stop(0.1, Color.RED),
                       new Stop(0.1, Color.BLUE),
                       new Stop(0.1, Color.RED),
                       new Stop(1.0, Color.BLACK))
                .build();
        assertEquals(expected, actual);
    }
}
