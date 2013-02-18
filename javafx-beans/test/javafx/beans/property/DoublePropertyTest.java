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

public class DoublePropertyTest {
	
	private static final Object NO_BEAN = null;
    private static final String NO_NAME_1 = null;
    private static final String NO_NAME_2 = "";
    private static final double VALUE_1 = Math.PI;
    private static final double VALUE_2 = -Math.E;
    private static final double DEFAULT = 0.0;
    private static final double EPSILON = 1e-12;

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

            final DoubleProperty p = new SimpleDoubleProperty(VALUE_1);
            p.setValue(null);
            assertEquals(DEFAULT, p.get(), EPSILON);
            log.check(0, "INFO", 1, "NullPointerException");
        }
    }

    @Test
    public void testBindBidirectional() {
        final DoubleProperty p1 = new SimpleDoubleProperty(VALUE_2);
        final DoubleProperty p2 = new SimpleDoubleProperty(VALUE_1);

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
        final DoubleProperty v0 = new DoublePropertyStub(NO_BEAN, NO_NAME_1);
        assertEquals("DoubleProperty [value: " + DEFAULT + "]", v0.toString());
        
        final DoubleProperty v1 = new DoublePropertyStub(NO_BEAN, NO_NAME_2);
        assertEquals("DoubleProperty [value: " + DEFAULT + "]", v1.toString());
        
        final Object bean = new Object();
        final String name = "My name";
        final DoubleProperty v2 = new DoublePropertyStub(bean, name);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", name: My name, value: " + DEFAULT + "]", v2.toString());
        v2.set(VALUE_1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", name: My name, value: " + VALUE_1 + "]", v2.toString());

        final DoubleProperty v3 = new DoublePropertyStub(bean, NO_NAME_1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v3.toString());
        v3.set(VALUE_1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v3.toString());

        final DoubleProperty v4 = new DoublePropertyStub(bean, NO_NAME_2);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + DEFAULT + "]", v4.toString());
        v4.set(VALUE_1);
        assertEquals("DoubleProperty [bean: " + bean.toString() + ", value: " + VALUE_1 + "]", v4.toString());

        final DoubleProperty v5 = new DoublePropertyStub(NO_BEAN, name);
        assertEquals("DoubleProperty [name: My name, value: " + DEFAULT + "]", v5.toString());
        v5.set(VALUE_1);
        assertEquals("DoubleProperty [name: My name, value: " + VALUE_1 + "]", v5.toString());
    }

    private class DoublePropertyStub extends DoubleProperty {

        private final Object bean;
        private final String name;
        private double value;

        private DoublePropertyStub(Object bean, String name) {
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
        public double get() {
            return value;
        }

        @Override
        public void set(double value) {
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
