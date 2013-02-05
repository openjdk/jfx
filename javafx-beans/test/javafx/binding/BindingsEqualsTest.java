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

import java.util.Arrays;
import java.util.Collection;

import javafx.beans.InvalidationListenerMock;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BindingsEqualsTest<T> {

    private static final float EPSILON_FLOAT = 1e-5f;
    private static final double EPSILON_DOUBLE = 1e-10;

    public static interface Functions<T> {
        BooleanBinding generateExpressionExpression(Object op1, Object op2);
        BooleanBinding generateExpressionPrimitive(Object op1, T op2);
        BooleanBinding generatePrimitiveExpression(T op1, Object op2);
        void setOp1(T value);
        void setOp2(T value);
        void check(T op1, T op2, BooleanBinding exp);
    }

    private final ObservableValue op1;
    private final ObservableValue op2;
    private final Functions<T> func;
    private final T[] v;
    private InvalidationListenerMock observer;

    public BindingsEqualsTest(ObservableValue op1, ObservableValue op2, Functions<T> func, T... v) {
        this.op1 = op1;
        this.op2 = op2;
        this.func = func;
        this.v = v;
    }

    private static String makeSafe(String value) {
        return value == null? "" : value;
    }

    @Before
    public void setUp() {
        func.setOp1(v[0]);
        func.setOp2(v[1]);
        observer = new InvalidationListenerMock();
    }

    @Test
    public void test_Expression_Expression() {
        final BooleanBinding binding = func.generateExpressionExpression(op1, op2);
        binding.addListener(observer);

        // check initial value
        func.check(v[0], v[1], binding);
        DependencyUtils.checkDependencies(binding.getDependencies(), op1, op2);

        // change first operand
        observer.reset();
        func.setOp1(v[1]);
        func.check(v[1], v[1], binding);
        observer.check(binding, 1);

        // change second operand
        func.setOp2(v[0]);
        func.check(v[1], v[0], binding);
        observer.check(binding, 1);

        // change both operands
        func.setOp1(v[0]);
        func.setOp2(v[1]);
        func.check(v[0], v[1], binding);
        observer.check(binding, 1);
    }

    @Test
    public void test_Self() {
        // using same FloatValue twice
        final BooleanBinding binding = func.generateExpressionExpression(op1, op1);
        binding.addListener(observer);

        // check initial value
        func.check(v[0], v[0], binding);

        // change value
        func.setOp1(v[1]);
        func.check(v[1], v[1], binding);
        observer.check(binding, 1);
    }

    @Test(expected=NullPointerException.class)
    public void test_null_Expression() {
        func.generateExpressionExpression(null, op1);
    }

    @Test(expected=NullPointerException.class)
    public void test_Expression_null() {
        func.generateExpressionExpression(op1, null);
    }

    @Test
    public void test_Expression_Primitive() {
        final BooleanBinding binding = func.generateExpressionPrimitive(op1, v[1]);
        binding.addListener(observer);

        // check initial value
        func.check(v[0], v[1], binding);
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);

        // change first operand
        observer.reset();
        func.setOp1(v[1]);
        func.check(v[1], v[1], binding);
        observer.check(binding, 1);

        // change to highest value
        func.setOp1(v[2]);
        func.check(v[2], v[1], binding);
        observer.check(binding, 1);
    }

    @Test(expected=NullPointerException.class)
    public void test_null_Primitive() {
        func.generateExpressionPrimitive(null, v[0]);
    }

    @Test
    public void test_Primitive_Expression() {
        final BooleanBinding binding = func.generatePrimitiveExpression(v[1], op1);
        binding.addListener(observer);

        // check initial value
        func.check(v[1], v[0], binding);
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);

        // change first operand
        observer.reset();
        func.setOp1(v[1]);
        func.check(v[1], v[1], binding);
        observer.check(binding, 1);

        // change to highest value
        func.setOp1(v[2]);
        func.check(v[1], v[2], binding);
        observer.check(binding, 1);
    }

    @Test(expected=NullPointerException.class)
    public void test_Primitive_null() {
        func.generatePrimitiveExpression(v[0], null);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final FloatProperty float1 = new SimpleFloatProperty();
        final FloatProperty float2 = new SimpleFloatProperty();
        final Float[] floatData = new Float[] {-EPSILON_FLOAT, 0.0f, EPSILON_FLOAT};

        final DoubleProperty double1 = new SimpleDoubleProperty();
        final DoubleProperty double2 = new SimpleDoubleProperty();
        final Double[] doubleData = new Double[] {-EPSILON_DOUBLE, 0.0, EPSILON_DOUBLE};

        final IntegerProperty int1 = new SimpleIntegerProperty();
        final IntegerProperty int2 = new SimpleIntegerProperty();
        final Integer[] integerData = new Integer[] {-1, 0, 1};

        final LongProperty long1 = new SimpleLongProperty();
        final LongProperty long2 = new SimpleLongProperty();
        final Long[] longData = new Long[] {-1L, 0L, 1L};

        final StringProperty string1 = new SimpleStringProperty();
        final StringProperty string2 = new SimpleStringProperty();
        final String[] stringData = new String[] {null, "Hello", "Hello World"};
        final String[] ciStringData = new String[] {null, "hello", "HELLO"};

        final ObjectProperty<Object> object1 = new SimpleObjectProperty<Object>();
        final ObjectProperty<Object> object2 = new SimpleObjectProperty<Object>();
        final Object[] objectData = new Object[] {new Object(), new Object(), new Object()};

        return Arrays.asList(new Object[][] {
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableFloatValue)op1, (ObservableFloatValue)op2, EPSILON_FLOAT);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.equal((ObservableFloatValue)op1, op2.floatValue(), EPSILON_FLOAT);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.equal(op1.floatValue(), (ObservableFloatValue)op2, EPSILON_FLOAT);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) <= EPSILON_FLOAT, exp.get());
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableFloatValue)op1, (ObservableFloatValue)op2, EPSILON_FLOAT);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.notEqual((ObservableFloatValue)op1, op2.floatValue(), EPSILON_FLOAT);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.notEqual(op1.floatValue(), (ObservableFloatValue)op2, EPSILON_FLOAT);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) > EPSILON_FLOAT, exp.get());
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThan((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.greaterThan((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.greaterThan(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, BooleanBinding exp) {
                        assertEquals(op1 > op2, exp.get());
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThan((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.lessThan((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.lessThan(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, BooleanBinding exp) {
                        assertEquals(op1 < op2, exp.get());
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThanOrEqual((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.greaterThanOrEqual((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.greaterThanOrEqual(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, BooleanBinding exp) {
                        assertEquals(op1 >= op2, exp.get());
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThanOrEqual((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.lessThanOrEqual((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.lessThanOrEqual(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, BooleanBinding exp) {
                        assertEquals(op1 <= op2, exp.get());
                    }
                },
                floatData
            },



            // double
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableDoubleValue)op1, (ObservableDoubleValue)op2, EPSILON_DOUBLE);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.equal((ObservableDoubleValue)op1, op2.doubleValue(), EPSILON_DOUBLE);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.equal(op1.doubleValue(), (ObservableDoubleValue)op2, EPSILON_DOUBLE);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) <= EPSILON_DOUBLE, exp.get());
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableDoubleValue)op1, (ObservableDoubleValue)op2, EPSILON_DOUBLE);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.notEqual((ObservableDoubleValue)op1, op2.doubleValue(), EPSILON_DOUBLE);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.notEqual(op1.doubleValue(), (ObservableDoubleValue)op2, EPSILON_DOUBLE);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) > EPSILON_DOUBLE, exp.get());
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThan((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.greaterThan((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.greaterThan(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, BooleanBinding exp) {
                        assertEquals(op1 > op2, exp.get());
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThan((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.lessThan((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.lessThan(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, BooleanBinding exp) {
                        assertEquals(op1 < op2, exp.get());
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThanOrEqual((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.greaterThanOrEqual((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.greaterThanOrEqual(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, BooleanBinding exp) {
                        assertEquals(op1 >= op2, exp.get());
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThanOrEqual((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.lessThanOrEqual((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.lessThanOrEqual(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, BooleanBinding exp) {
                        assertEquals(op1 <= op2, exp.get());
                    }
                },
                doubleData
            },



            // integer
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.equal((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.equal(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(op1.equals(op2), exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableIntegerValue)op1, (ObservableIntegerValue)op2, 1);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.equal((ObservableIntegerValue)op1, op2.intValue(), 1);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.equal(op1.intValue(), (ObservableIntegerValue)op2, 1);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) <= 1, exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.notEqual((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.notEqual(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(!op1.equals(op2), exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableIntegerValue)op1, (ObservableIntegerValue)op2, 1);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.notEqual((ObservableIntegerValue)op1, op2.intValue(), 1);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.notEqual(op1.intValue(), (ObservableIntegerValue)op2, 1);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) > 1, exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThan((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.greaterThan((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.greaterThan(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(op1 > op2, exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThan((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.lessThan((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.lessThan(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(op1 < op2, exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThanOrEqual((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.greaterThanOrEqual((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.greaterThanOrEqual(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(op1 >= op2, exp.get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThanOrEqual((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.lessThanOrEqual((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.lessThanOrEqual(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, BooleanBinding exp) {
                        assertEquals(op1 <= op2, exp.get());
                    }
                },
                integerData
            },



            // long
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.equal((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.equal(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(op1.equals(op2), exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableLongValue)op1, (ObservableLongValue)op2, 1);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.equal((ObservableLongValue)op1, op2.longValue(), 1);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.equal(op1.longValue(), (ObservableLongValue)op2, 1);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) <= 1, exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.notEqual((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.notEqual(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(!op1.equals(op2), exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableLongValue)op1, (ObservableLongValue)op2, 1);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.notEqual((ObservableLongValue)op1, op2.longValue(), 1);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.notEqual(op1.longValue(), (ObservableLongValue)op2, 1);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(Math.abs(op1 - op2) > 1, exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThan((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.greaterThan((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.greaterThan(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(op1 > op2, exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThan((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.lessThan((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.lessThan(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(op1 < op2, exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThanOrEqual((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.greaterThanOrEqual((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.greaterThanOrEqual(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(op1 >= op2, exp.get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThanOrEqual((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.lessThanOrEqual((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.lessThanOrEqual(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, BooleanBinding exp) {
                        assertEquals(op1 <= op2, exp.get());
                    }
                },
                longData
            },



            // String
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.equal((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.equal(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).equals(makeSafe(op2)), exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.equal((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.equal(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).equals(makeSafe(op2)), exp.get());
                    }
                },
                ciStringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equalIgnoreCase((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.equalIgnoreCase((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.equalIgnoreCase(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).equalsIgnoreCase(makeSafe(op2)), exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equalIgnoreCase((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.equalIgnoreCase((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.equalIgnoreCase(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).equalsIgnoreCase(makeSafe(op2)), exp.get());
                    }
                },
                ciStringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.notEqual((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.notEqual(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(!makeSafe(op1).equals(makeSafe(op2)), exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.notEqual((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.notEqual(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(!makeSafe(op1).equals(makeSafe(op2)), exp.get());
                    }
                },
                ciStringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqualIgnoreCase((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.notEqualIgnoreCase((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.notEqualIgnoreCase(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(!makeSafe(op1).equalsIgnoreCase(makeSafe(op2)), exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqualIgnoreCase((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.notEqualIgnoreCase((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.notEqualIgnoreCase(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(!makeSafe(op1).equalsIgnoreCase(makeSafe(op2)), exp.get());
                    }
                },
                ciStringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThan((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.greaterThan((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.greaterThan(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).compareTo(makeSafe(op2)) > 0, exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThan((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.lessThan((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.lessThan(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).compareTo(makeSafe(op2)) < 0, exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.greaterThanOrEqual((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.greaterThanOrEqual((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.greaterThanOrEqual(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).compareTo(makeSafe(op2)) >= 0, exp.get());
                    }
                },
                stringData
            },
            {
                string1, string2,
                new Functions<String>() {
                    @Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.lessThanOrEqual((ObservableStringValue)op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, String op2) {
                        return Bindings.lessThanOrEqual((ObservableStringValue)op1, op2);
                    }
                    @Override
                    public BooleanBinding generatePrimitiveExpression(String op1, Object op2) {
                        return Bindings.lessThanOrEqual(op1, (ObservableStringValue)op2);
                    }
                    @Override
                    public void setOp1(String value) {string1.set(value);}
                    @Override
                    public void setOp2(String value) {string2.set(value);}
                    @Override
                    public void check(String op1, String op2, BooleanBinding exp) {
                        assertEquals(makeSafe(op1).compareTo(makeSafe(op2)) <= 0, exp.get());
                    }
                },
                stringData
            },



            // Object
            {
                object1, object2,
                new Functions<Object>() {
                    @SuppressWarnings("unchecked")
					@Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.equal((ObservableObjectValue<Object>)op1, (ObservableObjectValue<Object>)op2);
                    }
                    @SuppressWarnings("unchecked")
					@Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Object op2) {
                        return Bindings.equal((ObservableObjectValue<Object>)op1, op2);
                    }
                    @SuppressWarnings("unchecked")
					@Override
                    public BooleanBinding generatePrimitiveExpression(Object op1, Object op2) {
                        return Bindings.equal(op1, (ObservableObjectValue<Object>)op2);
                    }
                    @Override
                    public void setOp1(Object value) {object1.set(value);}
                    @Override
                    public void setOp2(Object value) {object2.set(value);}
                    @Override
                    public void check(Object op1, Object op2, BooleanBinding exp) {
                        assertEquals(op1.equals(op2), exp.get());
                    }
                },
                objectData
            },
            {
                object1, object2,
                new Functions<Object>() {
                    @SuppressWarnings("unchecked")
					@Override
                    public BooleanBinding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableObjectValue<Object>)op1, (ObservableObjectValue<Object>)op2);
                    }
                    @SuppressWarnings("unchecked")
					@Override
                    public BooleanBinding generateExpressionPrimitive(Object op1, Object op2) {
                        return Bindings.notEqual((ObservableObjectValue<Object>)op1, op2);
                    }
                    @SuppressWarnings("unchecked")
					@Override
                    public BooleanBinding generatePrimitiveExpression(Object op1, Object op2) {
                        return Bindings.notEqual(op1, (ObservableObjectValue<Object>)op2);
                    }
                    @Override
                    public void setOp1(Object value) {object1.set(value);}
                    @Override
                    public void setOp2(Object value) {object2.set(value);}
                    @Override
                    public void check(Object op1, Object op2, BooleanBinding exp) {
                        assertEquals(!op1.equals(op2), exp.get());
                    }
                },
                objectData
            },
        });
    };

}
