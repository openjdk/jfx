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
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import test.com.sun.javafx.binding.ErrorLoggingUtiltity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LongPropertyTest {

    private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final long VALUE_1 = 1234567890L;
    private static final long VALUE_2 = -987654321L;
    private static final long DEFAULT = 0L;

    @BeforeAll
    public static void setUpClass() {
        ErrorLoggingUtiltity.reset();
    }

    @Test
    public void testSetValue_Null() {
        final LongProperty p = new SimpleLongProperty(VALUE_1);
        p.setValue(null);
        assertEquals(DEFAULT, p.get());
        ErrorLoggingUtiltity.checkFine(NullPointerException.class);
    }

    @Test
    public void testBindBidirectional() {
        final LongProperty p1 = new SimpleLongProperty(VALUE_2);
        final LongProperty p2 = new SimpleLongProperty(VALUE_1);

        p1.bindBidirectional(p2);
        assertEquals(VALUE_1, p1.get());
        assertEquals(VALUE_1, p2.get());

        p1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get());
        assertEquals(VALUE_2, p2.get());

        p2.set(VALUE_1);
        assertEquals(VALUE_1, p1.get());
        assertEquals(VALUE_1, p2.get());

        p1.unbindBidirectional(p2);
        p1.set(VALUE_2);
        assertEquals(VALUE_2, p1.get());
        assertEquals(VALUE_1, p2.get());

        p1.set(VALUE_1);
        p2.set(VALUE_2);
        assertEquals(VALUE_1, p1.get());
        assertEquals(VALUE_2, p2.get());
    }

    @Test
    public void testToString() {
        final LongProperty v0 = new LongPropertyStub(NO_BEAN, NO_NAME_1);
        assertEquals("LongProperty [value: " + DEFAULT + "]", v0.toString());

        final LongProperty v1 = new LongPropertyStub(NO_BEAN, NO_NAME_2);
        assertEquals("LongProperty [value: " + DEFAULT + "]", v1.toString());

        final Object bean = new Object();
        final String name = "My name";
        final LongProperty v2 = new LongPropertyStub(bean, name);
        assertEquals("LongProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.toString());
        v2.set(VALUE_1);
        assertEquals("LongProperty [bean: " + bean.toString() + ", name: My name, value: " + VALUE_1 + "]", v2.toString());

        final LongProperty v3 = new LongPropertyStub(bean, NO_NAME_1);
        assertEquals("LongProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.toString());
        v3.set(VALUE_1);
        assertEquals("LongProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v3.toString());

        final LongProperty v4 = new LongPropertyStub(bean, NO_NAME_2);
        assertEquals("LongProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        v4.set(VALUE_1);
        assertEquals("LongProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v4.toString());

        final LongProperty v5 = new LongPropertyStub(NO_BEAN, name);
        assertEquals("LongProperty [name: My name, value: " + DEFAULT + "]", v5.toString());
        v5.set(VALUE_1);
        assertEquals("LongProperty [name: My name, value: " + VALUE_1 + "]", v5.toString());
    }

    @Test
    public void testAsObject() {
        final LongProperty valueModel = new SimpleLongProperty();
        final ObjectProperty<Long> exp = valueModel.asObject();

        assertEquals(Long.valueOf(0L), exp.getValue());
        valueModel.set(-4354L);
        assertEquals(Long.valueOf(-4354L), exp.getValue());
        valueModel.set(5L);
        assertEquals(Long.valueOf(5L), exp.getValue());

        exp.set(10L);
        assertEquals(10L, valueModel.longValue());
    }

    @Test
    public void testObjectToLong() {
        final ObjectProperty<Long> valueModel = new SimpleObjectProperty<>(2L);
        final LongProperty exp = LongProperty.longProperty(valueModel);

        assertEquals(2L, exp.longValue());
        valueModel.set(-4354L);
        assertEquals(-4354L, exp.longValue());
        valueModel.set(5L);
        assertEquals(5L, exp.longValue());

        exp.set(10L);
        assertEquals(Long.valueOf(10L), valueModel.getValue());
    }

    private class LongPropertyStub extends LongProperty {

        private final Object bean;
        private final String name;
        private long value;

        private LongPropertyStub(Object bean, String name) {
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
        public long get() {
            return value;
        }

        @Override
        public void set(long value) {
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
