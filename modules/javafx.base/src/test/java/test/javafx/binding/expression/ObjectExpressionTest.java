/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.binding.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValueStub;
import test.javafx.binding.DependencyUtils;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ObjectExpressionTest {

    private Object data1;
    private Object data2;
    private ObjectProperty<Object> op1;
    private ObjectProperty<Object> op2;

    @BeforeEach
    public void setUp() {
        data1 = new Object();
        data2 = new Object();
        op1 = new SimpleObjectProperty<>(data1);
        op2 = new SimpleObjectProperty<>(data2);
    }

    @Test
    public void testEquals() {
        BooleanBinding binding = op1.isEqualTo(op1);
        assertEquals(true, binding.get());

        binding = op1.isEqualTo(op2);
        assertEquals(false, binding.get());

        binding = op1.isEqualTo(data1);
        assertEquals(true, binding.get());

        binding = op1.isEqualTo(data2);
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

        binding = op1.isNotEqualTo(data2);
        assertEquals(true, binding.get());
    }

    @Test
    public void testIsNull() {
        BooleanBinding binding = op1.isNull();
        assertEquals(false, binding.get());

        ObjectProperty<Object> op3 = new SimpleObjectProperty<>(null);
        binding = op3.isNull();
        assertEquals(true, binding.get());
    }

    @Test
    public void testIsNotNull() {
        BooleanBinding binding = op1.isNotNull();
        assertEquals(true, binding.get());

        ObjectProperty<Object> op3 = new SimpleObjectProperty<>(null);
        binding = op3.isNotNull();
        assertEquals(false, binding.get());
    }

    @Test
    public void testFactory() {
        final ObservableObjectValueStub<Object> valueModel = new ObservableObjectValueStub<>();
        final ObjectExpression<Object> exp = ObjectExpression.objectExpression(valueModel);

        assertTrue(exp instanceof ObjectBinding);
        assertEquals(FXCollections.singletonObservableList(valueModel), ((ObjectBinding<Object>)exp).getDependencies());

        assertEquals(null, exp.get());
        valueModel.set(data1);
        assertEquals(data1, exp.get());
        valueModel.set(data2);
        assertEquals(data2, exp.get());

        // make sure we do not create unnecessary bindings
        assertEquals(op1, ObjectExpression.objectExpression(op1));
    }

    @Test
    public void testFactory_Null() {
        assertThrows(NullPointerException.class, () -> {
            ObjectExpression.objectExpression(null);
        });
    }


    @Test
    public void testAsString() {
        final StringBinding binding = op1.asString();
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);

        assertEquals(op1.get().toString(), binding.get());

        op1.set(new Object() {

            @Override
            public String toString() {
                return "foo";
            }

        });
        assertEquals("foo", binding.get());
    }

    @Test
    public void testAsString_Format() {
        final StringBinding binding = op1.asString("%h");
        op1.set(new Object() {

            @Override
            public String toString() {
                return "foo";
            }

        });
        DependencyUtils.checkDependencies(binding.getDependencies(), op1);
        assertEquals(Integer.toHexString(op1.get().hashCode()), binding.get());

    }
}
