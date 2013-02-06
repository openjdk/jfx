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
package javafx.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.FloatExpression;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.LongExpression;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableNumberValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BindingsNumberCastTest {

    public static interface Functions {
        Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2);
        void check(double op1, double op2, Binding binding);
    }

    private static final double EPSILON = 1e-5;

    private final Functions func;

    private Double double0;
    private Float float0;
    private Long long0;
    private Integer integer0;

    private DoubleProperty double1;
    private FloatProperty float1;
    private LongProperty long1;
    private IntegerProperty integer1;

    public BindingsNumberCastTest(Functions func) {
        this.func = func;
    }

    @Before
    public void setUp() {
        double0 = Double.valueOf(3.1415);
        float0 = Float.valueOf(2.71f);
        long0 = Long.valueOf(111L);
        integer0 = Integer.valueOf(42);

        double1 = new SimpleDoubleProperty(double0);
        float1 = new SimpleFloatProperty(float0);
        long1 = new SimpleLongProperty(long0);
        integer1 = new SimpleIntegerProperty(integer0);
    }

    @Test
    public void testDouble() {
        Binding binding = func.generateExpression(double1, double1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(double0, double0, binding);

        binding = func.generateExpression(double1, float1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(double0, float0, binding);

        binding = func.generateExpression(double1, long1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(double0, long0, binding);

        binding = func.generateExpression(double1, integer1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(double0, integer0, binding);
    }

    @Test
    public void testFloat() {
        Binding binding = func.generateExpression(float1, double1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(float0, double0, binding);

        binding = func.generateExpression(float1, float1);
        assertTrue(binding instanceof FloatExpression || binding instanceof BooleanExpression);
        func.check(float0, float0, binding);

        binding = func.generateExpression(float1, long1);
        assertTrue(binding instanceof FloatExpression || binding instanceof BooleanExpression);
        func.check(float0, long0, binding);

        binding = func.generateExpression(float1, integer1);
        assertTrue(binding instanceof FloatExpression || binding instanceof BooleanExpression);
        func.check(float0, integer0, binding);
    }

    @Test
    public void testLong() {
        Binding binding = func.generateExpression(long1, double1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(long0, double0, binding);

        binding = func.generateExpression(long1, float1);
        assertTrue(binding instanceof FloatExpression || binding instanceof BooleanExpression);
        func.check(long0, float0, binding);

        binding = func.generateExpression(long1, long1);
        assertTrue(binding instanceof LongExpression || binding instanceof BooleanExpression);
        func.check(long0, long0, binding);

        binding = func.generateExpression(long1, integer1);
        assertTrue(binding instanceof LongExpression || binding instanceof BooleanExpression);
        func.check(long0, integer0, binding);
    }

    @Test
    public void testInteger() {
        Binding binding = func.generateExpression(integer1, double1);
        assertTrue(binding instanceof DoubleExpression || binding instanceof BooleanExpression);
        func.check(integer0, double0, binding);

        binding = func.generateExpression(integer1, float1);
        assertTrue(binding instanceof FloatExpression || binding instanceof BooleanExpression);
        func.check(integer0, float0, binding);

        binding = func.generateExpression(integer1, long1);
        assertTrue(binding instanceof LongExpression || binding instanceof BooleanExpression);
        func.check(integer0, long0, binding);

        binding = func.generateExpression(integer1, integer1);
        assertTrue(binding instanceof IntegerExpression || binding instanceof BooleanExpression);
        func.check(integer0, integer0, binding);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.add(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof NumberExpression);
                        assertEquals(op1 + op2, ((NumberExpression)binding).doubleValue(), EPSILON);
                    }

                }
            },
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.multiply(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof NumberExpression);
                        assertEquals(op1 * op2, ((NumberExpression)binding).doubleValue(), EPSILON);
                    }

                }
            },
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.divide(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof NumberExpression);
                        if ((binding instanceof DoubleExpression) || (binding instanceof FloatExpression)) {
                            assertEquals(op1 / op2, ((NumberExpression)binding).doubleValue(), EPSILON);
                        } else {
                            assertEquals((long)op1 / (long)op2, ((NumberExpression)binding).longValue());
                        }
                    }

                }
            },
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.min(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof NumberExpression);
                        assertEquals(Math.min(op1, op2), ((NumberExpression)binding).doubleValue(), EPSILON);
                    }

                }
            },
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.max(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof NumberExpression);
                        assertEquals(Math.max(op1, op2), ((NumberExpression)binding).doubleValue(), EPSILON);
                    }

                }
            },
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.equal(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof BooleanExpression);
                        assertEquals(Math.abs(op1 - op2) < EPSILON, ((BooleanExpression)binding).get());
                    }

                }
            },
            {
                new Functions() {
                    @Override
                    public Binding generateExpression(ObservableNumberValue op1, ObservableNumberValue op2) {
                        return Bindings.greaterThan(op1, op2);
                    }

                    @Override
                    public void check(double op1, double op2, Binding binding) {
                        assertTrue(binding instanceof BooleanExpression);
                        assertEquals(op1 > op2, ((BooleanExpression)binding).get());
                    }

                }
            },
        });
    }

}
