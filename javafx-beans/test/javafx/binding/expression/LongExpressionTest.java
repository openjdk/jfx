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
package javafx.binding.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.LongExpression;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableLongValueStub;
import javafx.collections.FXCollections;

import org.junit.Before;
import org.junit.Test;

public class LongExpressionTest {

    private static final float EPSILON = 1e-6f;

    private long data;
    private LongProperty op1;
    private double double1;
    private float float1;
    private long long1;
    private int integer1;
    private short short1;
    private byte byte1;

    @Before
    public void setUp() {
        data = 34258;
        op1 = new SimpleLongProperty(data);
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
        final LongBinding binding1 = op1.negate();
        assertEquals(-data, binding1.longValue());
    }

    @Test
    public void testPlus() {
        final DoubleBinding binding1 = op1.add(double1);
        assertEquals(data + double1, binding1.doubleValue(), EPSILON);

        final FloatBinding binding2 = op1.add(float1);
        assertEquals(data + float1, binding2.floatValue(), EPSILON);

        final LongBinding binding3 = op1.add(long1);
        assertEquals(data + long1, binding3.longValue());

        final LongBinding binding4 = op1.add(integer1);
        assertEquals(data + integer1, binding4.longValue());

        final LongBinding binding5 = op1.add(short1);
        assertEquals(data + short1, binding5.longValue());

        final LongBinding binding6 = op1.add(byte1);
        assertEquals(data + byte1, binding6.longValue());
    }

    @Test
    public void testMinus() {
        final DoubleBinding binding1 = op1.subtract(double1);
        assertEquals(data - double1, binding1.doubleValue(), EPSILON);

        final FloatBinding binding2 = op1.subtract(float1);
        assertEquals(data - float1, binding2.floatValue(), EPSILON);

        final LongBinding binding3 = op1.subtract(long1);
        assertEquals(data - long1, binding3.longValue());

        final LongBinding binding4 = op1.subtract(integer1);
        assertEquals(data - integer1, binding4.longValue());

        final LongBinding binding5 = op1.subtract(short1);
        assertEquals(data - short1, binding5.longValue());

        final LongBinding binding6 = op1.subtract(byte1);
        assertEquals(data - byte1, binding6.longValue());
    }

    @Test
    public void testTimes() {
        final DoubleBinding binding1 = op1.multiply(double1);
        assertEquals(data * double1, binding1.doubleValue(), EPSILON);

        final FloatBinding binding2 = op1.multiply(float1);
        assertEquals(data * float1, binding2.floatValue(), EPSILON);

        final LongBinding binding3 = op1.multiply(long1);
        assertEquals(data * long1, binding3.longValue());

        final LongBinding binding4 = op1.multiply(integer1);
        assertEquals(data * integer1, binding4.longValue());

        final LongBinding binding5 = op1.multiply(short1);
        assertEquals(data * short1, binding5.longValue());

        final LongBinding binding6 = op1.multiply(byte1);
        assertEquals(data * byte1, binding6.longValue());
    }

    @Test
    public void testDividedBy() {
        final DoubleBinding binding1 = op1.divide(double1);
        assertEquals(data / double1, binding1.doubleValue(), EPSILON);

        final FloatBinding binding2 = op1.divide(float1);
        assertEquals(data / float1, binding2.floatValue(), EPSILON);

        final LongBinding binding3 = op1.divide(long1);
        assertEquals(data / long1, binding3.longValue());

        final LongBinding binding4 = op1.divide(integer1);
        assertEquals(data / integer1, binding4.longValue());

        final LongBinding binding5 = op1.divide(short1);
        assertEquals(data / short1, binding5.longValue());

        final LongBinding binding6 = op1.divide(byte1);
        assertEquals(data / byte1, binding6.longValue());
    }

    @Test
    public void testFactory() {
        final ObservableLongValueStub valueModel = new ObservableLongValueStub();
        final LongExpression exp = LongExpression.longExpression(valueModel);

        assertTrue(exp instanceof LongBinding);
        assertEquals(FXCollections.singletonObservableList(valueModel), ((LongBinding)exp).getDependencies());

        assertEquals(0L, exp.longValue());
        valueModel.set(data);
        assertEquals(data, exp.longValue());
        valueModel.set(long1);
        assertEquals(long1, exp.longValue());

        // make sure we do not create unnecessary bindings
        assertEquals(op1, LongExpression.longExpression(op1));
    }

    @Test(expected=NullPointerException.class)
    public void testFactory_Null() {
        LongExpression.longExpression(null);
    }
}