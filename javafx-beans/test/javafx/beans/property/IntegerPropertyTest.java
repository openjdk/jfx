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

package javafx.beans.property;


import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import com.sun.javafx.binding.ErrorLoggingUtiltity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntegerPropertyTest {
    
    private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final int VALUE_1 = 42;
    private static final int VALUE_2 = -13;
    private static final int DEFAULT = 0;

    private static final ErrorLoggingUtiltity log = new ErrorLoggingUtiltity();

    @BeforeClass
    public static void setUpClass() {
        log.start();
    }

    @AfterClass
    public static void tearDownClass() {
        log.stop();
    }

    @Ignore("RT-27128")
    @Test
    public void testSetValue_Null() {
        synchronized(log) {
            log.reset();

            final IntegerProperty p = new SimpleIntegerProperty(VALUE_1);
            p.setValue(null);
            assertEquals(DEFAULT, p.get());
            log.check(0, "INFO", 1, "NullPointerException");
        }
    }

    @Test
    public void testBindBidirectional() {
        final IntegerProperty p1 = new SimpleIntegerProperty(VALUE_2);
        final IntegerProperty p2 = new SimpleIntegerProperty(VALUE_1);
        
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
        final IntegerProperty v0 = new IntegerPropertyStub(NO_BEAN, NO_NAME_1);
        assertEquals("IntegerProperty [value: " + DEFAULT + "]", v0.toString());
        
        final IntegerProperty v1 = new IntegerPropertyStub(NO_BEAN, NO_NAME_2);
        assertEquals("IntegerProperty [value: " + DEFAULT + "]", v1.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final IntegerProperty v2 = new IntegerPropertyStub(bean, name);
        assertEquals("IntegerProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.toString());
        v2.set(VALUE_1);
        assertEquals("IntegerProperty [bean: " + bean.toString() + ", name: My name, value: " + VALUE_1 + "]", v2.toString());
        
        final IntegerProperty v3 = new IntegerPropertyStub(bean, NO_NAME_1);
        assertEquals("IntegerProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.toString());
        v3.set(VALUE_1);
        assertEquals("IntegerProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v3.toString());

        final IntegerProperty v4 = new IntegerPropertyStub(bean, NO_NAME_2);
        assertEquals("IntegerProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        v4.set(VALUE_1);
        assertEquals("IntegerProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v4.toString());

        final IntegerProperty v5 = new IntegerPropertyStub(NO_BEAN, name);
        assertEquals("IntegerProperty [name: My name, value: " + DEFAULT + "]", v5.toString());
        v5.set(VALUE_1);
        assertEquals("IntegerProperty [name: My name, value: " + VALUE_1 + "]", v5.toString());
    }
    
    @Test
    public void testAsObject() {
        final IntegerProperty valueModel = new SimpleIntegerProperty();
        final ObjectProperty<Integer> exp = valueModel.asObject();
        
        assertEquals(Integer.valueOf(0), exp.getValue());
        valueModel.set(-4354);
        assertEquals(Integer.valueOf(-4354), exp.getValue());
        valueModel.set(5);
        assertEquals(Integer.valueOf(5), exp.getValue());
        
        exp.set(10);
        assertEquals(10, valueModel.intValue());

    }
    
    @Test
    public void testObjectToInteger() {
        final ObjectProperty<Integer> valueModel = new SimpleObjectProperty<Integer>();
        final IntegerProperty exp = IntegerProperty.integerProperty(valueModel);
        
        assertEquals(0, exp.intValue());
        valueModel.set(-4354);
        assertEquals(-4354, exp.intValue());
        valueModel.set(5);
        assertEquals(5, exp.intValue());
        
        exp.set(10);
        assertEquals(Integer.valueOf(10), valueModel.getValue());

    }
    
    private class IntegerPropertyStub extends IntegerProperty {
        
        private final Object bean;
        private final String name;
        private int value;
        
        private IntegerPropertyStub(Object bean, String name) {
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
        public int get() {
            return value;
        }

        @Override
        public void set(int value) {
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
