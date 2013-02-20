/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BindingsNumberCalculationsTest<T> {

    private static final float EPSILON_FLOAT = 1e-5f;
    private static final double EPSILON_DOUBLE = 1e-10;

    public static interface Functions<S> {
        Binding generateExpressionExpression(Object op1, Object op2);
        Binding generateExpressionPrimitive(Object op1, S op2);
        Binding generatePrimitiveExpression(S op1, Object op2);
        void setOp1(S value);
        void setOp2(S value);
        void check(S op1, S op2, ObservableValue exp);
    }

    private final ObservableValue op1;
    private final ObservableValue op2;
    private final Functions<T> func;
    private final T[] v;
    private InvalidationListenerMock observer;

    public BindingsNumberCalculationsTest(ObservableValue op1, ObservableValue op2, Functions<T> func, T[] v) {
        this.op1 = op1;
        this.op2 = op2;
        this.func = func;
        this.v = v;
    }

    @Before
    public void setUp() {
        func.setOp1(v[0]);
        func.setOp2(v[1]);
        observer = new InvalidationListenerMock();
    }

    @Test
    public void test_Expression_Expression() {
        final Binding binding = func.generateExpressionExpression(op1, op2);
        binding.addListener(observer);

        // check initial value
        func.check(v[0], v[1], binding);
        DependencyUtils.checkDependencies(binding.getDependencies(), op1, op2);

        // change first operand
        observer.reset();
        func.setOp1(v[2]);
        func.check(v[2], v[1], binding);
        observer.check(binding, 1);

        // change second operand
        func.setOp2(v[3]);
        func.check(v[2], v[3], binding);
        observer.check(binding, 1);

        // change both operands
        func.setOp1(v[4]);
        func.setOp2(v[5]);
        func.check(v[4], v[5], binding);
        observer.check(binding, 1);
    }

    @Test
    public void test_Self() {
        // using same FloatValue twice
        final Binding binding = func.generateExpressionExpression(op1, op1);
        binding.addListener(observer);

        // check initial value
        func.check(v[0], v[0], binding);

        // change value
        observer.reset();
        func.setOp1(v[7]);
        func.check(v[7], v[7], binding);
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
        final Binding binding = func.generateExpressionPrimitive(op1, v[7]);
        binding.addListener(observer);

        // check initial value
        func.check(v[0], v[7], binding);
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);

        // change first operand
        observer.reset();
        func.setOp1(v[8]);
        func.check(v[8], v[7], binding);
        observer.check(binding, 1);
    }

    @Test(expected=NullPointerException.class)
    public void test_null_Primitive() {
        func.generateExpressionPrimitive(null, v[0]);
    }

    @Test
    public void test_Primitive_Expression() {
        final Binding binding = func.generatePrimitiveExpression(v[9], op1);
        binding.addListener(observer);

        // check initial value
        func.check(v[9], v[0], binding);
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);

        // change first operand
        observer.reset();
        func.setOp1(v[10]);
        func.check(v[9], v[10], binding);
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
        final Float[] floatData = new Float[] {-3592.9f, 234872.8347f, 3897.274f, 3958.938745f, -8347.3478f, 217.902874f, -2784.827f, -28723.7824f, 82.8274f, -12.23478f, 0.92874f};

        final DoubleProperty double1 = new SimpleDoubleProperty();
        final DoubleProperty double2 = new SimpleDoubleProperty();
        final Double[] doubleData = new Double[] {2348.2345, -92.214, -214.0214, -908.214, 67.124, 0.214, 2893.124, -214.987234, -89724.897234, 234.25, 8721.234};

        final IntegerProperty int1 = new SimpleIntegerProperty();
        final IntegerProperty int2 = new SimpleIntegerProperty();
        final Integer[] integerData = new Integer[] {248, -9384, -234, -34, -450809, 342345, 8923489, 23789, -89234, -13134, 23134879};

        final LongProperty long1 = new SimpleLongProperty();
        final LongProperty long2 = new SimpleLongProperty();
        final Long[] longData = new Long[] {9823984L, 2908934L, -234234L, 9089234L, 132323L, -89324L, -12424L, -8923442L, 78234L, -233487L, 988998L};

        return Arrays.asList(new Object[][] {
            // float
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.add((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.add((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.add(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, ObservableValue exp) {
                        assertEquals(op1 + op2, ((ObservableFloatValue)exp).get(), EPSILON_FLOAT);
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.subtract((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.subtract((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.subtract(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, ObservableValue exp) {
                        assertEquals(op1 - op2, ((ObservableFloatValue)exp).get(), EPSILON_FLOAT);
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.multiply((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.multiply((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.multiply(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, ObservableValue exp) {
                        assertEquals(op1 * op2, ((ObservableFloatValue)exp).get(), EPSILON_FLOAT);
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.divide((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.divide((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.divide(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, ObservableValue exp) {
                        assertEquals(op1 / op2, ((ObservableFloatValue)exp).get(), EPSILON_FLOAT);
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.min((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.min((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.min(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, ObservableValue exp) {
                        assertEquals(Math.min(op1, op2), ((ObservableFloatValue)exp).get(), EPSILON_FLOAT);
                    }
                },
                floatData
            },
            {
                float1, float2,
                new Functions<Float>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.max((ObservableFloatValue)op1, (ObservableFloatValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Float op2) {
                        return Bindings.max((ObservableFloatValue)op1, op2.floatValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Float op1, Object op2) {
                        return Bindings.max(op1.floatValue(), (ObservableFloatValue)op2);
                    }
                    @Override
                    public void setOp1(Float value) {float1.set(value);}
                    @Override
                    public void setOp2(Float value) {float2.set(value);}
                    @Override
                    public void check(Float op1, Float op2, ObservableValue exp) {
                        assertEquals(Math.max(op1, op2), ((ObservableFloatValue)exp).get(), EPSILON_FLOAT);
                    }
                },
                floatData
            },



            // double
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.add((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.add((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.add(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, ObservableValue exp) {
                        assertEquals(op1 + op2, ((ObservableDoubleValue)exp).get(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.subtract((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.subtract((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.subtract(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, ObservableValue exp) {
                        assertEquals(op1 - op2, ((ObservableDoubleValue)exp).get(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.multiply((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.multiply((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.multiply(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, ObservableValue exp) {
                        assertEquals(op1 * op2, ((ObservableDoubleValue)exp).get(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.divide((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.divide((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.divide(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, ObservableValue exp) {
                        assertEquals(op1 / op2, ((ObservableDoubleValue)exp).get(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.min((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.min((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.min(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, ObservableValue exp) {
                        assertEquals(Math.min(op1, op2), ((ObservableDoubleValue)exp).get(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },
            {
                double1, double2,
                new Functions<Double>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.max((ObservableDoubleValue)op1, (ObservableDoubleValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Double op2) {
                        return Bindings.max((ObservableDoubleValue)op1, op2.doubleValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Double op1, Object op2) {
                        return Bindings.max(op1.doubleValue(), (ObservableDoubleValue)op2);
                    }
                    @Override
                    public void setOp1(Double value) {double1.set(value);}
                    @Override
                    public void setOp2(Double value) {double2.set(value);}
                    @Override
                    public void check(Double op1, Double op2, ObservableValue exp) {
                        assertEquals(Math.max(op1, op2), ((ObservableDoubleValue)exp).get(), EPSILON_DOUBLE);
                    }
                },
                doubleData
            },



            // int
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.add((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.add((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.add(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, ObservableValue exp) {
                        assertEquals(op1 + op2, ((ObservableIntegerValue)exp).get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.subtract((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.subtract((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.subtract(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, ObservableValue exp) {
                        assertEquals(op1 - op2, ((ObservableIntegerValue)exp).get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.multiply((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.multiply((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.multiply(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, ObservableValue exp) {
                        assertEquals(op1 * op2, ((ObservableIntegerValue)exp).get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.divide((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.divide((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.divide(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, ObservableValue exp) {
                        assertEquals(op1 / op2, ((ObservableIntegerValue)exp).get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.min((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.min((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.min(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, ObservableValue exp) {
                        assertEquals(Math.min(op1, op2), ((ObservableIntegerValue)exp).get());
                    }
                },
                integerData
            },
            {
                int1, int2,
                new Functions<Integer>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.max((ObservableIntegerValue)op1, (ObservableIntegerValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Integer op2) {
                        return Bindings.max((ObservableIntegerValue)op1, op2.intValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Integer op1, Object op2) {
                        return Bindings.max(op1.intValue(), (ObservableIntegerValue)op2);
                    }
                    @Override
                    public void setOp1(Integer value) {int1.set(value);}
                    @Override
                    public void setOp2(Integer value) {int2.set(value);}
                    @Override
                    public void check(Integer op1, Integer op2, ObservableValue exp) {
                        assertEquals(Math.max(op1, op2), ((ObservableIntegerValue)exp).get());
                    }
                },
                integerData
            },



            // long
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.add((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.add((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.add(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, ObservableValue exp) {
                        assertEquals(op1 + op2, ((ObservableLongValue)exp).get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.subtract((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.subtract((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.subtract(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, ObservableValue exp) {
                        assertEquals(op1 - op2, ((ObservableLongValue)exp).get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.multiply((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.multiply((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.multiply(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, ObservableValue exp) {
                        assertEquals(op1 * op2, ((ObservableLongValue)exp).get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.divide((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.divide((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.divide(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, ObservableValue exp) {
                        assertEquals(op1 / op2, ((ObservableLongValue)exp).get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.min((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.min((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.min(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, ObservableValue exp) {
                        assertEquals(Math.min(op1, op2), ((ObservableLongValue)exp).get());
                    }
                },
                longData
            },
            {
                long1, long2,
                new Functions<Long>() {
                    @Override
                    public Binding generateExpressionExpression(Object op1, Object op2) {
                        return Bindings.max((ObservableLongValue)op1, (ObservableLongValue)op2);
                    }
                    @Override
                    public Binding generateExpressionPrimitive(Object op1, Long op2) {
                        return Bindings.max((ObservableLongValue)op1, op2.longValue());
                    }
                    @Override
                    public Binding generatePrimitiveExpression(Long op1, Object op2) {
                        return Bindings.max(op1.longValue(), (ObservableLongValue)op2);
                    }
                    @Override
                    public void setOp1(Long value) {long1.set(value);}
                    @Override
                    public void setOp2(Long value) {long2.set(value);}
                    @Override
                    public void check(Long op1, Long op2, ObservableValue exp) {
                        assertEquals(Math.max(op1, op2), ((ObservableLongValue)exp).get());
                    }
                },
                longData
            },
        });
    };
}
