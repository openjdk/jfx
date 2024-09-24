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

package test.javafx.beans.property;

import javafx.beans.InvalidationListener;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import test.com.sun.javafx.binding.ErrorLoggingUtiltity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FloatPropertyTest {

    private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final float VALUE_1 = (float)Math.PI;
    private static final float VALUE_2 = (float)-Math.E;
    private static final float DEFAULT = 0.0f;
    private static final float EPSILON = 1e-6f;

    @BeforeAll
    public static void setUpClass() {
        ErrorLoggingUtiltity.reset();
    }

    @Test
    public void testSetValue_Null() {
        final FloatProperty p = new SimpleFloatProperty(VALUE_1);
        p.setValue(null);
        assertEquals(DEFAULT, p.get(), EPSILON);
        ErrorLoggingUtiltity.checkFine(NullPointerException.class);
    }

    @Test
    public void testBindBidirectional() {
        final FloatProperty p1 = new SimpleFloatProperty(VALUE_2);
        final FloatProperty p2 = new SimpleFloatProperty(VALUE_1);

        p1.bindBidirectional(p2);
        assertEquals(VALUE_1, p1.get(), EPSILON);
        assertEquals(VALUE_1, p2.get(), EPSILON);

        p1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get(), EPSILON);
        assertEquals(VALUE_2, p2.get(), EPSILON);

        p2.set(VALUE_1);
        assertEquals(VALUE_1, p1.get(), EPSILON);
        assertEquals(VALUE_1, p2.get(), EPSILON);

        p1.unbindBidirectional(p2);
        p1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get(), EPSILON);
        assertEquals(VALUE_1, p2.get(), EPSILON);

        p1.set(VALUE_1);
        p2.set(VALUE_2);
        assertEquals(VALUE_1, p1.get(), EPSILON);
        assertEquals(VALUE_2, p2.get(), EPSILON);
    }

    @Test
    public void testToString() {
        final FloatProperty v0 = new FloatPropertyStub(NO_BEAN, NO_NAME_1);
        assertEquals("FloatProperty [value: " + DEFAULT + "]", v0.toString());

        final FloatProperty v1 = new FloatPropertyStub(NO_BEAN, NO_NAME_2);
        assertEquals("FloatProperty [value: " + DEFAULT + "]", v1.toString());

        final Object bean = new Object();
        final String name = "My name";
        final FloatProperty v2 = new FloatPropertyStub(bean, name);
        assertEquals("FloatProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.toString());
        v2.set(VALUE_1);
        assertEquals("FloatProperty [bean: " + bean.toString() + ", name: My name, value: " + VALUE_1 + "]", v2.toString());

        final FloatProperty v3 = new FloatPropertyStub(bean, NO_NAME_1);
        assertEquals("FloatProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.toString());
        v3.set(VALUE_1);
        assertEquals("FloatProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v3.toString());

        final FloatProperty v4 = new FloatPropertyStub(bean, NO_NAME_2);
        assertEquals("FloatProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        v4.set(VALUE_1);
        assertEquals("FloatProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v4.toString());

        final FloatProperty v5 = new FloatPropertyStub(NO_BEAN, name);
        assertEquals("FloatProperty [name: My name, value: " + DEFAULT + "]", v5.toString());
        v5.set(VALUE_1);
        assertEquals("FloatProperty [name: My name, value: " + VALUE_1 + "]", v5.toString());
    }

    @Test
    public void testAsObject() {
        final FloatProperty valueModel = new SimpleFloatProperty();
        final ObjectProperty<Float> exp = valueModel.asObject();

        assertEquals(0.0f, exp.getValue(), EPSILON);
        valueModel.set(-4354.3f);
        assertEquals(-4354.3f, exp.getValue(), EPSILON);
        valueModel.set(5e11f);
        assertEquals(5e11f, exp.getValue(), EPSILON);

        exp.set(1234.0f);
        assertEquals(1234.0f, valueModel.floatValue(), EPSILON);

    }

    @Test
    public void testObjectToFloat() {
        final ObjectProperty<Float> valueModel = new SimpleObjectProperty<>(2f);
        final FloatProperty exp = FloatProperty.floatProperty(valueModel);

        assertEquals(2f, exp.floatValue(), EPSILON);
        valueModel.set(-4354.3f);
        assertEquals(-4354.3f, exp.floatValue(), EPSILON);
        valueModel.set(5e11f);
        assertEquals(5e11f, exp.floatValue(), EPSILON);

        exp.set(1234.0f);
        assertEquals(1234.0f, valueModel.getValue(), EPSILON);
    }

    private class FloatPropertyStub extends FloatProperty {

        private final Object bean;
        private final String name;
        private float value;

        private FloatPropertyStub(Object bean, String name) {
            this.bean = bean;
            this.name = name;
        }

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public float get() {
            return value;
        }

        @Override
        public void set(float value) {
            this.value = value;
        }

        @Override
        public void bind(ObservableValue<? extends Number> observable) {
            fail("Not in use");
        }

        @Override
        public void unbind() {
            fail("Not in use");
        }

        @Override
        public boolean isBound() {
            fail("Not in use");
            return false;
        }

        @Override
        public void addListener(ChangeListener<? super Number> listener) {
            fail("Not in use");
        }

        @Override
        public void removeListener(ChangeListener<? super Number> listener) {
            fail("Not in use");
        }

        @Override
        public void addListener(InvalidationListener listener) {
            fail("Not in use");
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            fail("Not in use");
        }

    }
}
