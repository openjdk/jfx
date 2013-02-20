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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableIntegerValueStub;
import javafx.binding.DependencyUtils;

import org.junit.Before;
import org.junit.Test;

public class StringExpressionTest {

    private String data1;
    private String data1_ic;
    private String data2;
    private StringProperty op1;
    private StringProperty op2;

    @Before
    public void setUp() {
        data1 = "Hello";
        data1_ic = "HeLlO";
        data2 = "Goodbye";
        op1 = new SimpleStringProperty(data1);
        op2 = new SimpleStringProperty(data2);
    }

    @Test
    public void testConcat() {
        StringExpression expression = op1.concat(op2);
        assertEquals(data1 + data2, expression.get());

        expression = op1.concat(data1);
        assertEquals(data1 + data1, expression.get());
    }

    @Test
    public void testEquals() {
        BooleanBinding binding = op1.isEqualTo(op1);
        assertEquals(true, binding.get());

        binding = op1.isEqualTo(op2);
        assertEquals(false, binding.get());

        binding = op1.isEqualTo(data1);
        assertEquals(true, binding.get());

        binding = op1.isEqualTo(data1_ic);
        assertEquals(false, binding.get());

        binding = op1.isEqualTo(data2);
        assertEquals(false, binding.get());
    }

    @Test
    public void testEqualsIgnoringCase() {
        BooleanBinding binding = op1.isEqualToIgnoreCase(op1);
        assertEquals(true, binding.get());

        binding = op1.isEqualToIgnoreCase(op2);
        assertEquals(false, binding.get());

        binding = op1.isEqualToIgnoreCase(data1);
        assertEquals(true, binding.get());

        binding = op1.isEqualToIgnoreCase(data1_ic);
        assertEquals(true, binding.get());

        binding = op1.isEqualToIgnoreCase(data2);
        assertEquals(false, binding.get());
    }

    @Test
    public void testNotEquals() {
        BooleanBinding binding = op1.isNotEqualTo(op1);
        assertEquals(false, binding.get());

        binding = op1.isNotEqualTo(op2);
        assertEquals(true, binding.get());

        binding = op1.isNotEqualTo(data1);
        assertEquals(false, binding.get());

        binding = op1.isNotEqualTo(data1_ic);
        assertEquals(true, binding.get());

        binding = op1.isNotEqualTo(data2);
        assertEquals(true, binding.get());
    }

    @Test
    public void testNotEqualsIgnoringCase() {
        BooleanBinding binding = op1.isNotEqualToIgnoreCase(op1);
        assertEquals(false, binding.get());

        binding = op1.isNotEqualToIgnoreCase(op2);
        assertEquals(true, binding.get());

        binding = op1.isNotEqualToIgnoreCase(data1);
        assertEquals(false, binding.get());

        binding = op1.isNotEqualToIgnoreCase(data1_ic);
        assertEquals(false, binding.get());

        binding = op1.isNotEqualToIgnoreCase(data2);
        assertEquals(true, binding.get());
    }

    @Test
    public void testIsNull() {
        BooleanBinding binding = op1.isNull();
        assertEquals(false, binding.get());

        StringProperty op3 = new SimpleStringProperty(null);
        binding = op3.isNull();
        assertEquals(true, binding.get());
    }

    @Test
    public void testIsNotNull() {
        BooleanBinding binding = op1.isNotNull();
        assertEquals(true, binding.get());

        StringProperty op3 = new SimpleStringProperty(null);
        binding = op3.isNotNull();
        assertEquals(false, binding.get());
    }

    @Test
    public void testGreater() {
        BooleanBinding binding = op1.greaterThan(op1);
        assertEquals(data1.compareTo(data1) > 0, binding.get());

        binding = op1.greaterThan(op2);
        assertEquals(data1.compareTo(data2) > 0, binding.get());

        binding = op1.greaterThan(data1);
        assertEquals(data1.compareTo(data1) > 0, binding.get());

        binding = op1.greaterThan(data1_ic);
        assertEquals(data1.compareTo(data1_ic) > 0, binding.get());

        binding = op1.greaterThan(data2);
        assertEquals(data1.compareTo(data2) > 0, binding.get());
    }

    @Test
    public void testLesser() {
        BooleanBinding binding = op1.lessThan(op1);
        assertEquals(data1.compareTo(data1) < 0, binding.get());

        binding = op1.lessThan(op2);
        assertEquals(data1.compareTo(data2) < 0, binding.get());

        binding = op1.lessThan(data1);
        assertEquals(data1.compareTo(data1) < 0, binding.get());

        binding = op1.lessThan(data1_ic);
        assertEquals(data1.compareTo(data1_ic) < 0, binding.get());

        binding = op1.lessThan(data2);
        assertEquals(data1.compareTo(data2) < 0, binding.get());
    }

    @Test
    public void testGreaterOrEqual() {
        BooleanBinding binding = op1.greaterThanOrEqualTo(op1);
        assertEquals(data1.compareTo(data1) >= 0, binding.get());

        binding = op1.greaterThanOrEqualTo(op2);
        assertEquals(data1.compareTo(data2) >= 0, binding.get());

        binding = op1.greaterThanOrEqualTo(data1);
        assertEquals(data1.compareTo(data1) >= 0, binding.get());

        binding = op1.greaterThanOrEqualTo(data1_ic);
        assertEquals(data1.compareTo(data1_ic) >= 0, binding.get());

        binding = op1.greaterThanOrEqualTo(data2);
        assertEquals(data1.compareTo(data2) >= 0, binding.get());
    }

    @Test
    public void testLesserOrEqual() {
        BooleanBinding binding = op1.lessThanOrEqualTo(op1);
        assertEquals(data1.compareTo(data1) <= 0, binding.get());

        binding = op1.lessThanOrEqualTo(op2);
        assertEquals(data1.compareTo(data2) <= 0, binding.get());

        binding = op1.lessThanOrEqualTo(data1);
        assertEquals(data1.compareTo(data1) <= 0, binding.get());

        binding = op1.lessThanOrEqualTo(data1_ic);
        assertEquals(data1.compareTo(data1_ic) <= 0, binding.get());

        binding = op1.lessThanOrEqualTo(data2);
        assertEquals(data1.compareTo(data2) <= 0, binding.get());
    }

    @Test
    public void testLength() {
        IntegerBinding binding = op1.length();
        assertEquals(data1.length(), binding.get());

        StringProperty op3 = new SimpleStringProperty(null);
        binding = op3.length();
        assertEquals(0, binding.get());
    }

    @Test
    public void testIsEmpty() {
        BooleanBinding binding = op1.isEmpty();
        assertEquals(data1.isEmpty(), binding.get());

        StringProperty op3 = new SimpleStringProperty(null);
        binding = op3.isEmpty();
        assertEquals(true, binding.get());
    }

    @Test
    public void testIsNotEmpty() {
        BooleanBinding binding = op1.isNotEmpty();
        assertEquals(!data1.isEmpty(), binding.get());

        StringProperty op3 = new SimpleStringProperty(null);
        binding = op3.isNotEmpty();
        assertEquals(false, binding.get());
    }

    @Test
    public void testGetValueSafe() {
        assertEquals(data1, op1.get());
        assertEquals(data1, op1.getValueSafe());

        op1.set(null);
        assertNull(op1.get());
        assertEquals("", op1.getValueSafe());
    }

    @Test
    public void testFactory() {
        final ObservableIntegerValueStub valueModel = new ObservableIntegerValueStub();
        final StringExpression exp = StringExpression.stringExpression(valueModel);

        assertTrue(exp instanceof StringBinding);
        DependencyUtils.checkDependencies(((StringBinding)exp).getDependencies(), valueModel);

        assertEquals("0", exp.get());
        valueModel.set(42);
        assertEquals("42", exp.get());
        valueModel.set(7);
        assertEquals("7", exp.get());

        // make sure we do not create unnecessary bindings
        assertEquals(op1, StringExpression.stringExpression(op1));
    }

    @Test(expected=NullPointerException.class)
    public void testFactory_Null() {
        StringExpression.stringExpression(null);
    }
    
}
