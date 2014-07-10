/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property.adapter;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.lang.reflect.UndeclaredThrowableException;

import static org.junit.Assert.*;

/**
*/
public abstract class JavaBeanPropertyTestBase<T> {
    private BeanStub<T> bean;
    private JavaBeanProperty<T> property;

    protected abstract BeanStub<T> createBean(T value);
    protected abstract void check(T actual, T expected);
    protected abstract T getValue(int index);
    protected abstract Property<T> createObservable(T value);
    protected abstract JavaBeanProperty<T> extractProperty(Object bean) throws NoSuchMethodException;

    @Before
    public void setUp() {
        this.bean = createBean(getValue(0));
        try {
            this.property = extractProperty(bean);
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSetup_WithNull() {
        try {
            this.property = extractProperty(null);
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testContext() {
        assertEquals("x", property.getName());
        assertEquals(bean, property.getBean());
    }

    @Test
    public void testGetAndSet() {
        check(property.getValue(), getValue(0));

        try {
            bean.setValue(getValue(1));
            assertEquals(property.getValue(), getValue(1));
        } catch (PropertyVetoException e) {
            fail();
        }

        property.setValue(getValue(0));
        assertEquals(property.getValue(), getValue(0));
        assertEquals(bean.getValue(), getValue(0));
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void testGet_Exception() {
        bean.setFailureMode(true);
        property.getValue();
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void testSet_Exception() {
        bean.setFailureMode(true);
        property.setValue(getValue(1));
    }

    @Test(expected = RuntimeException.class)
    public void testSet_Bound() {
        final Property<T> observable = createObservable(getValue(1));
        property.bind(observable);
        property.setValue(getValue(0));
    }

    @Test
    public void testInvalidationListener() {
        final BooleanProperty fired = new SimpleBooleanProperty(false);
        final InvalidationListener listener = observable -> {
            fired.set(true);
        };

        property.addListener(listener);
        fired.set(false);
        property.setValue(getValue(1));
        assertTrue(fired.get());

        property.removeListener(listener);
        fired.set(false);
        property.setValue(getValue(0));
        assertFalse(fired.get());
    }

    @Test
    public void testChangeListener() {
        final BooleanProperty fired = new SimpleBooleanProperty(false);
        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            fired.set(true);
        };

        property.addListener(listener);
        fired.set(false);
        property.setValue(getValue(1));
        assertTrue(fired.get());

        property.removeListener(listener);
        fired.set(false);
        property.setValue(getValue(0));
        assertFalse(fired.get());
    }
    
    @Test
    public void testDispose() {
        assertTrue(bean.hasListeners());
        property.dispose();
        assertFalse(bean.hasListeners());
    }

    @Test
    public void testBinding() {
        assertFalse(property.isBound());

        final Property<T> observable1 = createObservable(getValue(1));
        property.bind(observable1);

        assertTrue(property.isBound());
        check(property.getValue(), getValue(1));
        check(bean.getValue(), getValue(1));

        observable1.setValue(getValue(0));
        check(property.getValue(), getValue(0));
        check(bean.getValue(), getValue(0));

        // bind again - should not have any effect
        property.bind(observable1);
        assertTrue(property.isBound());

        // bind to another observable
        final Property<T> observable2 = createObservable(getValue(1));
        property.bind(observable2);
        assertTrue(property.isBound());
        check(property.getValue(), getValue(1));
        check(bean.getValue(), getValue(1));

        observable2.setValue(getValue(0));
        check(property.getValue(), getValue(0));
        check(bean.getValue(), getValue(0));

        observable1.setValue(getValue(1));
        check(property.getValue(), getValue(0));
        check(bean.getValue(), getValue(0));

        // unbind
        property.unbind();
        assertFalse(property.isBound());
        observable2.setValue(getValue(1));
        check(property.getValue(), getValue(0));
        check(bean.getValue(), getValue(0));

        // unbind again - should not have any effect
        property.unbind();
        assertFalse(property.isBound());
    }

    @Test(expected = NullPointerException.class)
    public void testBindWithNull() {
        property.bind(null);
    }

    public static abstract class BeanStub<U> {
        private int listenerCount;
        
        public abstract U getValue();
        public abstract void setValue(U value) throws PropertyVetoException;
        public abstract void setFailureMode(boolean failureMode);

        private boolean hasListeners() {
            return listenerCount > 0;
        }

        public void addXListener(PropertyChangeListener listener) {
            listenerCount++;
        }

        public void removeXListener(PropertyChangeListener listener) {
            listenerCount = Math.max(0, listenerCount-1);
        }

        public void addXListener(VetoableChangeListener listener) {
            listenerCount++;
        }

        public void removeXListener(VetoableChangeListener listener) {
            listenerCount = Math.max(0, listenerCount-1);
        }
    }
}
