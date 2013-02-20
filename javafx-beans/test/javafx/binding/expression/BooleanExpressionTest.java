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
import static org.junit.Assert.assertTrue;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValueStub;
import javafx.binding.DependencyUtils;
import javafx.collections.FXCollections;

import org.junit.Before;
import org.junit.Test;

public class BooleanExpressionTest {

    private BooleanProperty op1;
    private BooleanProperty op2;

    @Before
    public void setUp() {
        op1 = new SimpleBooleanProperty(true);
        op2 = new SimpleBooleanProperty(false);
    }

    @Test
    public void testGetters() {
        assertEquals(true, op1.get());
        assertEquals(Boolean.TRUE, op1.getValue());

        assertEquals(false, op2.get());
        assertEquals(Boolean.FALSE, op2.getValue());
    }

    @Test
    public void testAND() {
        final BooleanExpression exp = op1.and(op2);
        assertEquals(true && false, exp.get());

        op1.set(false);
        assertEquals(false && false, exp.get());

        op2.set(true);
        assertEquals(false && true, exp.get());

        op1.set(true);
        assertEquals(true && true, exp.get());
    }

    @Test
    public void testOR() {
        final BooleanExpression exp = op1.or(op2);
        assertEquals(true || false, exp.get());

        op1.set(false);
        assertEquals(false || false, exp.get());

        op2.set(true);
        assertEquals(false || true, exp.get());

        op1.set(true);
        assertEquals(true || true, exp.get());
    }

    @Test
    public void testNOT() {
        final BooleanExpression exp = op1.not();
        assertEquals(false, exp.get());

        op1.set(false);
        assertEquals(true, exp.get());

        op1.set(true);
        assertEquals(false, exp.get());
    }

    @Test
    public void testEquals() {
        final BooleanExpression exp = op1.isEqualTo(op2);
        assertEquals(true == false, exp.get());

        op1.set(false);
        assertEquals(false == false, exp.get());

        op2.set(true);
        assertEquals(false == true, exp.get());

        op1.set(true);
        assertEquals(true == true, exp.get());
    }

    @Test
    public void testNotEquals() {
        final BooleanExpression exp = op1.isNotEqualTo(op2);
        assertEquals(true != false, exp.get());

        op1.set(false);
        assertEquals(false != false, exp.get());

        op2.set(true);
        assertEquals(false != true, exp.get());

        op1.set(true);
        assertEquals(true != true, exp.get());
    }

    @Test
    public void testAsString() {
        final StringBinding binding = op1.asString();
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);
        assertEquals("true", binding.get());

        op1.set(false);
        assertEquals("false", binding.get());

        op1.set(true);
        assertEquals("true", binding.get());
    }
    
    @Test
    public void testFactory() {
        final ObservableBooleanValueStub valueModel = new ObservableBooleanValueStub();
        final BooleanExpression exp = BooleanExpression.booleanExpression(valueModel);
        
        assertTrue(exp instanceof BooleanBinding);
        assertEquals(FXCollections.singletonObservableList(valueModel), ((BooleanBinding)exp).getDependencies());

        assertEquals(false, exp.get());
        valueModel.set(true);
        assertEquals(true, exp.get());
        valueModel.set(false);
        assertEquals(false, exp.get());

        // make sure we do not create unnecessary bindings
        assertEquals(op1, BooleanExpression.booleanExpression(op1));
    }

    @Test(expected=NullPointerException.class)
    public void testFactory_Null() {
        BooleanExpression.booleanExpression(null);
    }
}
