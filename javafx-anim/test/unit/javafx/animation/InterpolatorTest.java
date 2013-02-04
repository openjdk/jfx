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
package javafx.animation;

import com.sun.javafx.animation.TickCalculation;
import com.sun.scenario.animation.shared.InterpolationInterval;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;
import javafx.util.Duration;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InterpolatorTest {

    private static final double EPSILON = 1e-12;
    private static final float EPSILON_FLOAT = 1e-6f;
    private static final DummyInterpolatable START = new DummyInterpolatable(0);
    private static final DummyInterpolatable END   = new DummyInterpolatable(10);
    
    @Test
    public void testInterpolateWithObjects() {
    	final DummyInterpolatable i1 = new DummyInterpolatable(1);
    	final DummyInterpolatable i2 = new DummyInterpolatable(4);
    	final Object o = new Object();
    	
    	assertEquals(Double.valueOf(2.5), (Double)Interpolator.LINEAR.interpolate(Double.valueOf(1), Integer.valueOf(4), 0.5), EPSILON);
    	assertEquals(Double.valueOf(2.5), (Double)Interpolator.LINEAR.interpolate(Integer.valueOf(4), Double.valueOf(1), 0.5), EPSILON);
    	assertEquals(Float.valueOf(2.5f), (Float)Interpolator.LINEAR.interpolate(Float.valueOf(1), Integer.valueOf(4), 0.5), EPSILON_FLOAT);
    	assertEquals(Float.valueOf(2.5f), (Float)Interpolator.LINEAR.interpolate(Integer.valueOf(4), Float.valueOf(1), 0.5), EPSILON_FLOAT);
    	assertEquals(Long.valueOf(3L), Interpolator.LINEAR.interpolate(Long.valueOf(1), Integer.valueOf(4),  0.5));
    	assertEquals(Long.valueOf(3L), Interpolator.LINEAR.interpolate(Integer.valueOf(4), Long.valueOf(1), 0.5));
    	assertEquals(Integer.valueOf(3), Interpolator.LINEAR.interpolate(Integer.valueOf(1), Integer.valueOf(4), 0.5));
    	assertEquals(2.5, ((DummyInterpolatable)Interpolator.LINEAR.interpolate(i1, i2, 0.5)).value, EPSILON);
    	
    	assertEquals(o, Interpolator.LINEAR.interpolate(o, Integer.MIN_VALUE, 1.0-2*EPSILON));
    	assertEquals(Integer.MIN_VALUE, Interpolator.LINEAR.interpolate(o, Integer.MIN_VALUE, 1.0));
    	
    	assertEquals(Integer.MIN_VALUE, Interpolator.LINEAR.interpolate(Integer.MIN_VALUE, o, 1.0-2*EPSILON));
    	assertEquals(o, Interpolator.LINEAR.interpolate(Integer.MIN_VALUE, o, 1.0));
    	
        assertEquals(i1, Interpolator.LINEAR.interpolate(i1, o, 1.0-2*EPSILON));
        assertEquals(o, Interpolator.LINEAR.interpolate(i1, o, 1.0));
    }

    @Test
    public void testDISCRETE() {
    	assertEquals(false, Interpolator.DISCRETE.interpolate(false, true, 1.0-2*EPSILON));
    	assertEquals(true, Interpolator.DISCRETE.interpolate(false, true, 1.0));
		
        assertEquals(1.0, Interpolator.DISCRETE.interpolate(1.0, 2.0, 1.0-2*EPSILON), EPSILON);
        assertEquals(2.0, Interpolator.DISCRETE.interpolate(1.0, 2.0, 1.0), EPSILON);

        assertEquals(-3, Interpolator.DISCRETE.interpolate(-3, 7, 1.0-2*EPSILON));
        assertEquals( 7, Interpolator.DISCRETE.interpolate(-3, 7, 1.0));

        assertEquals(  12L, Interpolator.DISCRETE.interpolate(12L, -201L, 1.0-2*EPSILON));
        assertEquals(-201L, Interpolator.DISCRETE.interpolate(12L, -201L, 1.0));

        assertEquals( 0, ((DummyInterpolatable)Interpolator.DISCRETE.interpolate(START, END, 1.0-2*EPSILON)).value, EPSILON);
        assertEquals(10, ((DummyInterpolatable)Interpolator.DISCRETE.interpolate(START, END, 1.0)).value, EPSILON);
    }

    @Test
    public void testLINEAR() {
        assertEquals(false, Interpolator.LINEAR.interpolate(false, true, 0.0));
        assertEquals(false, Interpolator.LINEAR.interpolate(false, true, 0.1));
        assertEquals(false, Interpolator.LINEAR.interpolate(false, true, 0.5));
        assertEquals(false, Interpolator.LINEAR.interpolate(false, true, 0.9));
        assertEquals(true, Interpolator.LINEAR.interpolate(false, true, 1.0));

        assertEquals(1.0, Interpolator.LINEAR.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.1, Interpolator.LINEAR.interpolate(1.0, 2.0, 0.1), EPSILON);
        assertEquals(1.5, Interpolator.LINEAR.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.9, Interpolator.LINEAR.interpolate(1.0, 2.0, 0.9), EPSILON);
        assertEquals(2.0, Interpolator.LINEAR.interpolate(1.0, 2.0, 1.0), EPSILON);

        assertEquals(-3, Interpolator.LINEAR.interpolate(-3, 7, 0.0));
        assertEquals(-2, Interpolator.LINEAR.interpolate(-3, 7, 0.1));
        assertEquals( 2, Interpolator.LINEAR.interpolate(-3, 7, 0.5));
        assertEquals( 6, Interpolator.LINEAR.interpolate(-3, 7, 0.9));
        assertEquals( 7, Interpolator.LINEAR.interpolate(-3, 7, 1.0));

        assertEquals(-3L, Interpolator.LINEAR.interpolate(-3L, 7L, 0.0));
        assertEquals(-2L, Interpolator.LINEAR.interpolate(-3L, 7L, 0.1));
        assertEquals( 2L, Interpolator.LINEAR.interpolate(-3L, 7L, 0.5));
        assertEquals( 6L, Interpolator.LINEAR.interpolate(-3L, 7L, 0.9));
        assertEquals( 7L, Interpolator.LINEAR.interpolate(-3L, 7L, 1.0));

        assertEquals( 0, ((DummyInterpolatable)Interpolator.LINEAR.interpolate(START, END, 0.0)).value, EPSILON);
        assertEquals( 1, ((DummyInterpolatable)Interpolator.LINEAR.interpolate(START, END, 0.1)).value, EPSILON);
        assertEquals( 5, ((DummyInterpolatable)Interpolator.LINEAR.interpolate(START, END, 0.5)).value, EPSILON);
        assertEquals( 9, ((DummyInterpolatable)Interpolator.LINEAR.interpolate(START, END, 0.9)).value, EPSILON);
        assertEquals(10, ((DummyInterpolatable)Interpolator.LINEAR.interpolate(START, END, 1.0)).value, EPSILON);
    }

    @Test
    public void testEASE_BOTH() {
        // Expected results calculated with JavaFX SDK 1.3
    	
        assertEquals(false, Interpolator.EASE_BOTH.interpolate(false, true, 0.0));
        assertEquals(false, Interpolator.EASE_BOTH.interpolate(false, true, 0.1));
        assertEquals(false, Interpolator.EASE_BOTH.interpolate(false, true, 0.5));
        assertEquals(false, Interpolator.EASE_BOTH.interpolate(false, true, 0.9));
        assertEquals(true, Interpolator.EASE_BOTH.interpolate(false, true, 1.0));
        
        assertEquals(1.0,   Interpolator.EASE_BOTH.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.125, Interpolator.EASE_BOTH.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.5,   Interpolator.EASE_BOTH.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.875, Interpolator.EASE_BOTH.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0,   Interpolator.EASE_BOTH.interpolate(1.0, 2.0, 1.0), EPSILON);

        assertEquals(-3, Interpolator.EASE_BOTH.interpolate(-3, 7, 0.0));
        assertEquals(-2, Interpolator.EASE_BOTH.interpolate(-3, 7, 0.2));
        assertEquals( 2, Interpolator.EASE_BOTH.interpolate(-3, 7, 0.5));
        assertEquals( 6, Interpolator.EASE_BOTH.interpolate(-3, 7, 0.8));
        assertEquals( 7, Interpolator.EASE_BOTH.interpolate(-3, 7, 1.0));

        assertEquals(-3L, Interpolator.EASE_BOTH.interpolate(-3L, 7L, 0.0));
        assertEquals(-2L, Interpolator.EASE_BOTH.interpolate(-3L, 7L, 0.2));
        assertEquals( 2L, Interpolator.EASE_BOTH.interpolate(-3L, 7L, 0.5));
        assertEquals( 6L, Interpolator.EASE_BOTH.interpolate(-3L, 7L, 0.8));
        assertEquals( 7L, Interpolator.EASE_BOTH.interpolate(-3L, 7L, 1.0));

        assertEquals( 0,    ((DummyInterpolatable)Interpolator.EASE_BOTH.interpolate(START, END, 0.0)).value, EPSILON);
        assertEquals( 1.25, ((DummyInterpolatable)Interpolator.EASE_BOTH.interpolate(START, END, 0.2)).value, EPSILON);
        assertEquals( 5,    ((DummyInterpolatable)Interpolator.EASE_BOTH.interpolate(START, END, 0.5)).value, EPSILON);
        assertEquals( 8.75, ((DummyInterpolatable)Interpolator.EASE_BOTH.interpolate(START, END, 0.8)).value, EPSILON);
        assertEquals(10,    ((DummyInterpolatable)Interpolator.EASE_BOTH.interpolate(START, END, 1.0)).value, EPSILON);
    }

    @Test
    public void testEASE_IN() {
        // Expected results calculated with JavaFX SDK 1.3
    	
        assertEquals(false, Interpolator.EASE_IN.interpolate(false, true, 0.0));
        assertEquals(false, Interpolator.EASE_IN.interpolate(false, true, 0.1));
        assertEquals(false, Interpolator.EASE_IN.interpolate(false, true, 0.5));
        assertEquals(false, Interpolator.EASE_IN.interpolate(false, true, 0.9));
        assertEquals(true, Interpolator.EASE_IN.interpolate(false, true, 1.0));
        
        assertEquals(1.0,      Interpolator.EASE_IN.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.1111111111111112, Interpolator.EASE_IN.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.4444444444444444, Interpolator.EASE_IN.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.777777777777778, Interpolator.EASE_IN.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0,      Interpolator.EASE_IN.interpolate(1.0, 2.0, 1.0), EPSILON);

        assertEquals(-3, Interpolator.EASE_IN.interpolate(-3, 7, 0.0));
        assertEquals(-2, Interpolator.EASE_IN.interpolate(-3, 7, 0.2));
        assertEquals( 1, Interpolator.EASE_IN.interpolate(-3, 7, 0.5));
        assertEquals( 5, Interpolator.EASE_IN.interpolate(-3, 7, 0.8));
        assertEquals( 7, Interpolator.EASE_IN.interpolate(-3, 7, 1.0));

        assertEquals(-3L, Interpolator.EASE_IN.interpolate(-3L, 7L, 0.0));
        assertEquals(-2L, Interpolator.EASE_IN.interpolate(-3L, 7L, 0.2));
        assertEquals( 1L, Interpolator.EASE_IN.interpolate(-3L, 7L, 0.5));
        assertEquals( 5L, Interpolator.EASE_IN.interpolate(-3L, 7L, 0.8));
        assertEquals( 7L, Interpolator.EASE_IN.interpolate(-3L, 7L, 1.0));

        assertEquals( 0,        ((DummyInterpolatable)Interpolator.EASE_IN.interpolate(START, END, 0.0)).value, EPSILON);
        assertEquals( 1.1111111111111114, ((DummyInterpolatable)Interpolator.EASE_IN.interpolate(START, END, 0.2)).value, EPSILON);
        assertEquals( 4.444444444444445, ((DummyInterpolatable)Interpolator.EASE_IN.interpolate(START, END, 0.5)).value, EPSILON);
        assertEquals( 7.777777777777779, ((DummyInterpolatable)Interpolator.EASE_IN.interpolate(START, END, 0.8)).value, EPSILON);
        assertEquals(10,        ((DummyInterpolatable)Interpolator.EASE_IN.interpolate(START, END, 1.0)).value, EPSILON);
    }

    @Test
    public void testEASE_OUT() {
        // Expected results calculated with JavaFX SDK 1.3
    	
        assertEquals(false, Interpolator.EASE_OUT.interpolate(false, true, 0.0));
        assertEquals(false, Interpolator.EASE_OUT.interpolate(false, true, 0.1));
        assertEquals(false, Interpolator.EASE_OUT.interpolate(false, true, 0.5));
        assertEquals(false, Interpolator.EASE_OUT.interpolate(false, true, 0.9));
        assertEquals(true, Interpolator.EASE_OUT.interpolate(false, true, 1.0));
        
        assertEquals(1.0,      Interpolator.EASE_OUT.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.2222222222222223, Interpolator.EASE_OUT.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.5555555555555556, Interpolator.EASE_OUT.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.8888888888888888, Interpolator.EASE_OUT.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0,      Interpolator.EASE_OUT.interpolate(1.0, 2.0, 1.0), EPSILON);

        assertEquals(-3, Interpolator.EASE_OUT.interpolate(-3, 7, 0.0));
        assertEquals(-1, Interpolator.EASE_OUT.interpolate(-3, 7, 0.2));
        assertEquals( 3, Interpolator.EASE_OUT.interpolate(-3, 7, 0.5));
        assertEquals( 6, Interpolator.EASE_OUT.interpolate(-3, 7, 0.8));
        assertEquals( 7, Interpolator.EASE_OUT.interpolate(-3, 7, 1.0));

        assertEquals(-3L, Interpolator.EASE_OUT.interpolate(-3L, 7L, 0.0));
        assertEquals(-1L, Interpolator.EASE_OUT.interpolate(-3L, 7L, 0.2));
        assertEquals( 3L, Interpolator.EASE_OUT.interpolate(-3L, 7L, 0.5));
        assertEquals( 6L, Interpolator.EASE_OUT.interpolate(-3L, 7L, 0.8));
        assertEquals( 7L, Interpolator.EASE_OUT.interpolate(-3L, 7L, 1.0));

        assertEquals( 0,        ((DummyInterpolatable)Interpolator.EASE_OUT.interpolate(START, END, 0.0)).value, EPSILON);
        assertEquals( 2.2222222222222223, ((DummyInterpolatable)Interpolator.EASE_OUT.interpolate(START, END, 0.2)).value, EPSILON);
        assertEquals( 5.555555555555555, ((DummyInterpolatable)Interpolator.EASE_OUT.interpolate(START, END, 0.5)).value, EPSILON);
        assertEquals( 8.88888888888889, ((DummyInterpolatable)Interpolator.EASE_OUT.interpolate(START, END, 0.8)).value, EPSILON);
        assertEquals(10,        ((DummyInterpolatable)Interpolator.EASE_OUT.interpolate(START, END, 1.0)).value, EPSILON);
    }

    @Test
    public void testSPLINE_Concave() {
        Interpolator i = Interpolator.SPLINE(0.0, 0.5, 0.5, 1.0);
        assertEquals(1.0, i.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.5573742287206063, i.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.8400223953585164, i.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.9742173260814238, i.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0, i.interpolate(1.0, 2.0, 1.0), EPSILON);
    }

    @Test
    public void testSPLINE_Convex() {
        Interpolator i = Interpolator.SPLINE(0.5, 0.0, 1.0, 0.5);
        assertEquals(1.0, i.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.0257826739185762, i.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.1599776046414838, i.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.4426257712793937, i.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0, i.interpolate(1.0, 2.0, 1.0), EPSILON);
    }

    @Test
    public void testSPLINE_WithInflectionPoint() {
        Interpolator i = Interpolator.SPLINE(0.0, 1.0, 1.0, 0.0);

        assertEquals(1.0, i.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.4614221762502215, i.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.5, i.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.5385778237497787, i.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0, i.interpolate(1.0, 2.0, 1.0), EPSILON);
    }

    @Test
    public void testSPLINE_Linear() {
        Interpolator i = Interpolator.SPLINE(1/3, 1/3, 2/3, 2/3);

        assertEquals(1.0, i.interpolate(1.0, 2.0, 0.0), EPSILON);
        assertEquals(1.2, i.interpolate(1.0, 2.0, 0.2), EPSILON);
        assertEquals(1.5, i.interpolate(1.0, 2.0, 0.5), EPSILON);
        assertEquals(1.8, i.interpolate(1.0, 2.0, 0.8), EPSILON);
        assertEquals(2.0, i.interpolate(1.0, 2.0, 1.0), EPSILON);
    }

    @Test
    public void testTANGENT_Linear() {
        SimpleLongProperty property = new SimpleLongProperty();

        Interpolator i0 = Interpolator.TANGENT(Duration.seconds(1), 20);
        Interpolator i1 = Interpolator.TANGENT(Duration.seconds(1), 40);

        InterpolationInterval interval = InterpolationInterval.create(new KeyValue(property, 60L, i1),
                TickCalculation.fromDuration(Duration.seconds(3)),
                new KeyValue(property, 0L, i0), TickCalculation.fromDuration(Duration.seconds(3)));

        interval.interpolate(1.0/3.0);
        assertEquals(20L, (long)property.getValue());
        interval.interpolate(1.0/2.0);
        assertEquals(30L, (long)property.getValue());
        interval.interpolate(2.0/3.0);
        assertEquals(40L, (long)property.getValue());
    }
    
    private static class DummyInterpolatable implements Interpolatable<DummyInterpolatable> {

        final double value;

        private DummyInterpolatable(double value) {
                this.value = value;
        }

        @Override
        public DummyInterpolatable interpolate(DummyInterpolatable endVal, double t) {
                if (Math.abs(t) < EPSILON) {
                        return this;
                } else if (Math.abs(t-1.0) < EPSILON) {
                        return endVal;
                } else {
                        return new DummyInterpolatable(value + t * (endVal.value - value));
                }
        }
    }
}
