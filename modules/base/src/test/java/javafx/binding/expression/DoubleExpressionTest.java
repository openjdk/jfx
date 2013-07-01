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

package javafx.binding.expression;

import static org.junit.Assert.assertTrue;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValueStub;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueStub;
import javafx.collections.FXCollections;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DoubleExpressionTest {

    private static final float EPSILON = 1e-6f;

    private double data;
    private DoubleProperty op1;
    private double double1;
    private float float1;
    private long long1;
    private int integer1;
    private short short1;
    private byte byte1;

    @Before
    public void setUp() {
        data = -67.0975;
        op1 = new SimpleDoubleProperty(data);
        double1 = -234.234;
        float1 = 111.9f;
        long1 = 2009234L;
        integer1 = -234734;
        short1 = 9824;
        byte1 = -123;
    }

    @Test
    public void testGetters() {
        assertEquals((double)data, op1.doubleValue(), EPSILON);
        assertEquals((float)data, op1.floatValue(), EPSILON);
        assertEquals((long)data, op1.longValue());
        assertEquals((int)data, op1.intValue());
    }

    @Test
    public void testNegation() {
        final DoubleBinding binding1 = op1.negate();
        assertEquals(-data, binding1.doubleValue(), EPSILON);
    }

    @Test
    public void testPlus() {
        final DoubleBinding binding1 = op1.add(double1);
        assertEquals(data + double1, binding1.doubleValue(), EPSILON);

        final DoubleBinding binding2 = op1.add(float1);
        assertEquals(data + float1, binding2.doubleValue(), EPSILON);

        final DoubleBinding binding3 = op1.add(long1);
        assertEquals(data + long1, binding3.doubleValue(), EPSILON);

        final DoubleBinding binding4 = op1.add(integer1);
        assertEquals(data + integer1, binding4.doubleValue(), EPSILON);

        final DoubleBinding binding5 = op1.add(short1);
        assertEquals(data + short1, binding5.doubleValue(), EPSILON);

        final DoubleBinding binding6 = op1.add(byte1);
        assertEquals(data + byte1, binding6.doubleValue(), EPSILON);
    }

    @Test
    public void testMinus() {
        final DoubleBinding binding1 = op1.subtract(double1);
        assertEquals(data - double1, binding1.doubleValue(), EPSILON);

        final DoubleBinding binding2 = op1.subtract(float1);
        assertEquals(data - float1, binding2.doubleValue(), EPSILON);

        final DoubleBinding binding3 = op1.subtract(long1);
        assertEquals(data - long1, binding3.doubleValue(), EPSILON);

        final DoubleBinding binding4 = op1.subtract(integer1);
        assertEquals(data - integer1, binding4.doubleValue(), EPSILON);

        final DoubleBinding binding5 = op1.subtract(short1);
        assertEquals(data - short1, binding5.doubleValue(), EPSILON);

        final DoubleBinding binding6 = op1.subtract(byte1);
        assertEquals(data - byte1, binding6.doubleValue(), EPSILON);
    }

    @Test
    public void testTimes() {
        final DoubleBinding binding1 = op1.multiply(double1);
        assertEquals(data * double1, binding1.doubleValue(), EPSILON);

        final DoubleBinding binding2 = op1.multiply(float1);
        assertEquals(data * float1, binding2.doubleValue(), EPSILON);

        final DoubleBinding binding3 = op1.multiply(long1);
        assertEquals(data * long1, binding3.doubleValue(), EPSILON);

        final DoubleBinding binding4 = op1.multiply(integer1);
        assertEquals(data * integer1, binding4.doubleValue(), EPSILON);

        final DoubleBinding binding5 = op1.multiply(short1);
        assertEquals(data * short1, binding5.doubleValue(), EPSILON);

        final DoubleBinding binding6 = op1.multiply(byte1);
        assertEquals(data * byte1, binding6.doubleValue(), EPSILON);
    }

    @Test
    public void testDividedBy() {
        final DoubleBinding binding1 = op1.divide(double1);
        assertEquals(data / double1, binding1.doubleValue(), EPSILON);

        final DoubleBinding binding2 = op1.divide(float1);
        assertEquals(data / float1, binding2.doubleValue(), EPSILON);

        final DoubleBinding binding3 = op1.divide(long1);
        assertEquals(data / long1, binding3.doubleValue(), EPSILON);

        final DoubleBinding binding4 = op1.divide(integer1);
        assertEquals(data / integer1, binding4.doubleValue(), EPSILON);

        final DoubleBinding binding5 = op1.divide(short1);
        assertEquals(data / short1, binding5.doubleValue(), EPSILON);

        final DoubleBinding binding6 = op1.divide(byte1);
        assertEquals(data / byte1, binding6.doubleValue(), EPSILON);
    }
    
    @Test
    public void testAsObject() { 
        final ObservableDoubleValueStub valueModel = new ObservableDoubleValueStub();
        final ObjectExpression<Double> exp = DoubleExpression.doubleExpression(valueModel).asObject();
        
        assertEquals(0.0, exp.getValue(), EPSILON);
        valueModel.set(data);
        assertEquals(data, exp.getValue(), EPSILON);
        valueModel.set(double1);
        assertEquals(double1, exp.getValue(), EPSILON);
    }

    @Test
    public void testFactory() {
        final ObservableDoubleValueStub valueModel = new ObservableDoubleValueStub();
        final DoubleExpression exp = DoubleExpression.doubleExpression(valueModel);

        assertTrue(exp instanceof DoubleBinding);
        assertEquals(FXCollections.singletonObservableList(valueModel), ((DoubleBinding)exp).getDependencies());

        assertEquals(0.0f, exp.doubleValue(), EPSILON);
        valueModel.set(data);
        assertEquals(data, exp.doubleValue(), EPSILON);
        valueModel.set(double1);
        assertEquals(double1, exp.doubleValue(), EPSILON);

        // make sure we do not create unnecessary bindings
        assertEquals(op1, DoubleExpression.doubleExpression(op1));
    }
    
    @Test
    public void testObjectToDouble() {
        final ObservableValueStub<Double> valueModel = new ObservableValueStub<Double>();
        final DoubleExpression exp = DoubleExpression.doubleExpression(valueModel);

        assertTrue(exp instanceof DoubleBinding);
        assertEquals(FXCollections.singletonObservableList(valueModel), ((DoubleBinding)exp).getDependencies());

        assertEquals(0.0, exp.doubleValue(), EPSILON);
        valueModel.set(data);
        assertEquals(data, exp.doubleValue(), EPSILON);
        valueModel.set(double1);
        assertEquals(double1, exp.doubleValue(), EPSILON);

        // make sure we do not create unnecessary bindings
        assertEquals(op1, DoubleExpression.doubleExpression((ObservableValue)op1));
    }

    @Test(expected=NullPointerException.class)
    public void testFactory_Null() {
        DoubleExpression.doubleExpression(null);
    }
}
