/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.property.adapter;

//package com.sun.javafx.property.adapter;
//
//import javafx.beans.property.ObjectProperty;
//import javafx.beans.property.adapter.JavaBeanObjectProperty;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.beans.*;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//
//**
//*/
//public class PropertyDescriptorTest {
//
//    @Before
//    public void setUp() {
//    }
//
//    @Test
//    public void testSetup() throws NoSuchMethodException {
//        final Object initialValue = new Object();
//        final Object secondValue = new Object();
//        final POJOBean bean = new POJOBean(initialValue);
//        helperPOJOBean.bean(bean);
//        final ObjectProperty<Object> property = new JavaBeanObjectProperty<Object>(helperPOJOBean.getDescriptor(), bean);
//        assertEquals(initialValue, property.get());
//        property.set(secondValue);
//        assertEquals(secondValue, bean.getX());
//        assertEquals(secondValue, property.get());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNameIsNull() {
//        try {
//            helperPOJOBean.name(null);
//            helperPOJOBean.getDescriptor();
//        } catch (NoSuchMethodException e) {
//            fail();
//        }
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNameIsEmpty() {
//        try {
//            helperPOJOBean.name("");
//            helperPOJOBean.getDescriptor();
//        } catch (NoSuchMethodException e) {
//            fail();
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithBeanClassIsNull() {
//        try {
//            helperPOJOBean.beanClass(null);
//            helperPOJOBean.getDescriptor();
//        } catch (NoSuchMethodException e) {
//            fail();
//        }
//    }
//
//    @Test
//    public void testSetup_WithNonStandardNames() {
//        final Object initialValue = new Object();
//        final Object secondValue = new Object();
//        final POJOBeanWithNonStandardNames bean = new POJOBeanWithNonStandardNames(initialValue);
//        helperPOJOBeanWithNonStandardNames.bean(bean);
//        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//        assertEquals(initialValue, property.get());
//        property.set(secondValue);
//        assertEquals(secondValue, bean.readX());
//        assertEquals(secondValue, property.get());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardNames_WithNameIsNull() {
//        try {
//            helperPOJOBeanWithNonStandardNames.name(null);
//            helperPOJOBeanWithNonStandardNames.getDescriptor();
//        } catch (NoSuchMethodException e) {
//            fail();
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardNames_WithBeanClassIsNull() {
//        try {
//            helperPOJOBeanWithNonStandardNames.beanClass(null);
//            helperPOJOBeanWithNonStandardNames.getDescriptor();
//        } catch (NoSuchMethodException e) {
//            fail();
//        }
//    }
//
//    @Test(expected = NoSuchMethodException.class)
//    public void testSetup_WithNonStandardNames_WithGetterNameIsNull() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.getterName(null);
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//    @Test(expected = NoSuchMethodException.class)
//    public void testSetup_WithNonStandardNames_WithSetterNameIsNull() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.setterName(null);
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNonStandardNames_WithNameIsEmpty() {
//        try {
//            helperPOJOBeanWithNonStandardNames.name("");
//            helperPOJOBeanWithNonStandardNames.getDescriptor();
//        } catch (NoSuchMethodException e) {
//            fail();
//        }
//    }
//
//    @Test(expected = NoSuchMethodException.class)
//    public void testSetup_WithNonStandardNames_WithGetterNameIsEmpty() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.getterName("");
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//    @Test(expected = NoSuchMethodException.class)
//    public void testSetup_WithNonStandardNames_WithSetterNameIsEmpty() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.setterName("");
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//--    @Test
//--    public void testSetup_WithNonStandardAccessors() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//--            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
//--            builder = new JavaBeanPropertyBuilder("x", POJOBeanWithNonStandardNames.class, getter, setter);
//--        } catch (NoSuchMethodException e) {
//--            fail("Error in test code. Should not happen.");
//--        }
//--        final Object initialValue = new Object();
//--        final Object secondValue = new Object();
//--        final POJOBeanWithNonStandardNames bean = new POJOBeanWithNonStandardNames(initialValue);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--        assertEquals(initialValue, property.get());
//--        property.set(secondValue);
//--        assertEquals(secondValue, bean.readX());
//--        assertEquals(secondValue, property.get());
//--    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardAccessors_WithNameIsNull() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.getterName(null);
//        helperPOJOBeanWithNonStandardNames.setterName(null);
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
//            helperPOJOBeanWithNonStandardNames.getter(getter);
//            helperPOJOBeanWithNonStandardNames.setter(setter);
//
//            helperPOJOBeanWithNonStandardNames.name(null);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//    @Test(expected = NoSuchMethodException.class)
//    public void testSetup_WithNonStandardAccessors_WithGetterIsNull() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.getterName(null);
//        helperPOJOBeanWithNonStandardNames.setterName(null);
//        try {
//            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
//            helperPOJOBeanWithNonStandardNames.setter(setter);
//
//            helperPOJOBeanWithNonStandardNames.getter(null);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//    @Test(expected = NoSuchMethodException.class)
//    public void testSetup_WithNonStandardAccessors_WithSetterIsNull() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.getterName(null);
//        helperPOJOBeanWithNonStandardNames.setterName(null);
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            helperPOJOBeanWithNonStandardNames.getter(getter);
//
//            helperPOJOBeanWithNonStandardNames.setter(null);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNonStandardAccessors_WithNameIsEmpty() throws NoSuchMethodException {
//        helperPOJOBeanWithNonStandardNames.getterName(null);
//        helperPOJOBeanWithNonStandardNames.setterName(null);
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
//            helperPOJOBeanWithNonStandardNames.getter(getter);
//            helperPOJOBeanWithNonStandardNames.setter(setter);
//
//            helperPOJOBeanWithNonStandardNames.name("");
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//        helperPOJOBeanWithNonStandardNames.getDescriptor();
//    }
//
//--    @Test(expected = IllegalArgumentException.class)
//--    public void testCreatePropertyWithWrongType_Boolean() {
//--        final POJOBean bean = new POJOBean(new Object());
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", POJOBean.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        builder.createBooleanProperty(bean);
//--    }
//--
//--    @Test
//--    public void testDisposal_GeneralAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//--        JavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        // initial state
//--        assertEquals(value0, property.get());
//--        assertTrue(bean.hasChangeListeners());
//--        assertTrue(bean.hasVetoListeners());
//--
//--        // dispose
//--        property.dispose();
//--        try {
//--            bean.setX(value1);
//--        } catch (PropertyVetoException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        assertFalse(bean.hasChangeListeners());
//--        assertFalse(bean.hasVetoListeners());
//--    }
//--
//--    @Test
//--    public void testDisposal_ParameterizedAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithParameterizedAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithParameterizedAddRemove bean = new BeanWithParameterizedAddRemove(value0);
//--        JavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        // initial state
//--        assertEquals(value0, property.get());
//--        assertTrue(bean.hasChangeListeners());
//--        assertTrue(bean.hasVetoListeners());
//--
//--        // dispose
//--        property.dispose();
//--        try {
//--            bean.setX(value1);
//--        } catch (PropertyVetoException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        assertFalse(bean.hasChangeListeners());
//--        assertFalse(bean.hasVetoListeners());
//--    }
//--
//--    @Test
//--    public void testDisposal_NamedAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithNamedAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithNamedAddRemove bean = new BeanWithNamedAddRemove(value0);
//--        JavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        // initial state
//--        assertEquals(value0, property.get());
//--        assertTrue(bean.hasChangeListeners());
//--        assertTrue(bean.hasVetoListeners());
//--
//--        // dispose
//--        property.dispose();
//--        try {
//--            bean.setX(value1);
//--        } catch (PropertyVetoException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        assertFalse(bean.hasChangeListeners());
//--        assertFalse(bean.hasVetoListeners());
//--    }
//--
//--    @Test
//--    public void testDisposal_Bound() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//--        JavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//--        final ObjectProperty<Object> observable = new SimpleObjectProperty<Object>(value1);
//--        property.bind(observable);
//--
//--        // initial state
//--        assertEquals(value1, property.get());
//--        assertTrue(bean.hasChangeListeners());
//--        assertTrue(bean.hasVetoListeners());
//--
//--        // dispose
//--        property.dispose();
//--        observable.set(value0);
//--        assertFalse(bean.hasChangeListeners());
//--        assertFalse(bean.hasVetoListeners());
//--    }
//--
//--    @Test
//--    public void testInvalidationListener_GeneralAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final BooleanProperty fired = new SimpleBooleanProperty(false);
//--        final InvalidationListener listener = new InvalidationListener() {
//--            @Override
//--            public void invalidated(Observable observable) {
//--                fired.set(true);
//--            }
//--        };
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        property.addListener(listener);
//--        fired.set(false);
//--        property.setValue(value1);
//--        assertTrue(fired.get());
//--
//--        property.removeListener(listener);
//--        fired.set(false);
//--        property.setValue(value0);
//--        assertFalse(fired.get());
//--    }
//--
//--    @Test
//--    public void testChangeListener_GeneralAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final BooleanProperty fired = new SimpleBooleanProperty(false);
//--        final ChangeListener<Object> listener = new ChangeListener<Object>() {
//--            @Override
//--            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
//--                fired.set(true);
//--            }
//--        };
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        property.addListener(listener);
//--        fired.set(false);
//--        property.setValue(value1);
//--        assertTrue(fired.get());
//--
//--        property.removeListener(listener);
//--        fired.set(false);
//--        property.setValue(value0);
//--        assertFalse(fired.get());
//--    }
//--
//--    @Test
//--    public void testInvalidationListener_ParameterizedAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithParameterizedAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final BooleanProperty fired = new SimpleBooleanProperty(false);
//--        final InvalidationListener listener = new InvalidationListener() {
//--            @Override
//--            public void invalidated(Observable observable) {
//--                fired.set(true);
//--            }
//--        };
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithParameterizedAddRemove bean = new BeanWithParameterizedAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        property.addListener(listener);
//--        fired.set(false);
//--        property.setValue(value1);
//--        assertTrue(fired.get());
//--
//--        property.removeListener(listener);
//--        fired.set(false);
//--        property.setValue(value0);
//--        assertFalse(fired.get());
//--    }
//--
//--    @Test
//--    public void testChangeListener_ParameterizedAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithParameterizedAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final BooleanProperty fired = new SimpleBooleanProperty(false);
//--        final ChangeListener<Object> listener = new ChangeListener<Object>() {
//--            @Override
//--            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
//--                fired.set(true);
//--            }
//--        };
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithParameterizedAddRemove bean = new BeanWithParameterizedAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        property.addListener(listener);
//--        fired.set(false);
//--        property.setValue(value1);
//--        assertTrue(fired.get());
//--
//--        property.removeListener(listener);
//--        fired.set(false);
//--        property.setValue(value0);
//--        assertFalse(fired.get());
//--    }
//--
//--    @Test
//--    public void testInvalidationListener_NamedAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithNamedAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final BooleanProperty fired = new SimpleBooleanProperty(false);
//--        final InvalidationListener listener = new InvalidationListener() {
//--            @Override
//--            public void invalidated(Observable observable) {
//--                fired.set(true);
//--            }
//--        };
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithNamedAddRemove bean = new BeanWithNamedAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        property.addListener(listener);
//--        fired.set(false);
//--        property.setValue(value1);
//--        assertTrue(fired.get());
//--
//--        property.removeListener(listener);
//--        fired.set(false);
//--        property.setValue(value0);
//--        assertFalse(fired.get());
//--    }
//--
//--    @Test
//--    public void testChangeListener_NamedAddRemove() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithNamedAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final BooleanProperty fired = new SimpleBooleanProperty(false);
//--        final ChangeListener<Object> listener = new ChangeListener<Object>() {
//--            @Override
//--            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
//--                fired.set(true);
//--            }
//--        };
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithNamedAddRemove bean = new BeanWithNamedAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--
//--        property.addListener(listener);
//--        fired.set(false);
//--        property.setValue(value1);
//--        assertTrue(fired.get());
//--
//--        property.removeListener(listener);
//--        fired.set(false);
//--        property.setValue(value0);
//--        assertFalse(fired.get());
//--    }
//--
//--    @Test
//--    public void testSet_Bound() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--        final Property<Object> observable = new SimpleObjectProperty<Object>();
//--        property.bind(observable);
//--
//--        try {
//--            bean.setX(value1);
//--            fail();
//--        } catch (PropertyVetoException e) {
//--            final PropertyChangeEvent event = e.getPropertyChangeEvent();
//--            assertEquals("x", event.getPropertyName());
//--            assertEquals(bean, event.getSource());
//--        }
//--    }
//--
//--    @Test
//--    public void testListenerWithOtherParameters() {
//--        JavaBeanPropertyBuilder builder = null;
//--        try {
//--            builder = new JavaBeanPropertyBuilder("x", BeanWithRawListenerSupport.class);
//--        } catch (NoSuchMethodException e) {
//--            e.printStackTrace();
//--            fail();
//--        }
//--        final Object value0 = new Object();
//--        final Object value1 = new Object();
//--        final BeanWithRawListenerSupport bean = new BeanWithRawListenerSupport(value0);
//--        final ObjectProperty<Object> property = builder.createObjectProperty(bean);
//--        final Property<Object> observable = new SimpleObjectProperty<Object>(value1);
//--        property.bind(observable);
//--
//--        try {
//--            bean.fireVetoableChange(new PropertyChangeEvent(new Object(), "x", value1, value0));
//--            assertEquals(value1, property.get());
//--            assertEquals(value1, bean.getX());
//--            bean.fireVetoableChange(new PropertyChangeEvent(bean, "y", value1, value0));
//--            assertEquals(value1, property.get());
//--            assertEquals(value1, bean.getX());
//--        } catch (PropertyVetoException e) {
//--            fail();
//--        }
//--
//--        bean.firePropertyChange(new PropertyChangeEvent(new Object(), "x", value1, value0));
//--        assertEquals(value1, property.get());
//--        assertEquals(value1, bean.getX());
//--        bean.firePropertyChange(new PropertyChangeEvent(bean, "y", value1, value0));
//--        assertEquals(value1, property.get());
//--        assertEquals(value1, bean.getX());
//--    }
//
//    public static class POJOBean {
//        private Object x;
//
//        public POJOBean(Object x) {this.x = x;}
//
//        public Object getX() {return x;}
//        public void setX(Object x) {this.x = x;}
//    }
//
//    public static class POJOBeanWithNonStandardNames {
//        private Object x;
//
//        public POJOBeanWithNonStandardNames(Object x) {this.x = x;}
//
//        public Object readX() {return x;}
//        public void writeX(Object x) {this.x = x;}
//    }
//
//    public static class BeanWithGeneralAddRemove {
//        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//        private final VetoableChangeSupport vetoableChangeSupport = new VetoableChangeSupport(this);
//
//        private Object x;
//        private int changeListenerCount;
//        private int vetoListenerCount;
//
//        public Object getX() {return x;}
//        public void setX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
//            vetoableChangeSupport.fireVetoableChange("x", oldX, x);
//            this.x = x;
//            propertyChangeSupport.firePropertyChange("x", oldX, x);
//        }
//
//        public BeanWithGeneralAddRemove(Object x) {this.x = x;}
//
//        public boolean hasChangeListeners() {
//            return changeListenerCount > 0;
//        }
//
//        public boolean hasVetoListeners() {
//            return vetoListenerCount > 0;
//        }
//
//        public void addPropertyChangeListener(PropertyChangeListener listener) {
//            changeListenerCount++;
//            propertyChangeSupport.addPropertyChangeListener(listener);
//        }
//
//        public void removePropertyChangeListener(PropertyChangeListener listener) {
//            changeListenerCount = Math.max(0, changeListenerCount-1);
//            propertyChangeSupport.removePropertyChangeListener(listener);
//        }
//
//        public void addVetoableChangeListener(VetoableChangeListener listener) {
//            vetoListenerCount++;
//            vetoableChangeSupport.addVetoableChangeListener(listener);
//        }
//
//        public void removeVetoableChangeListener(VetoableChangeListener listener) {
//            vetoListenerCount = Math.max(0, vetoListenerCount-1);
//            vetoableChangeSupport.removeVetoableChangeListener(listener);
//        }
//    }
//
//    public static class BeanWithParameterizedAddRemove {
//        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//        private final VetoableChangeSupport vetoableChangeSupport = new VetoableChangeSupport(this);
//
//        private Object x;
//        private int changeListenerCount;
//        private int vetoListenerCount;
//
//        public Object getX() {return x;}
//        public void setX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
//            vetoableChangeSupport.fireVetoableChange("x", oldX, x);
//            this.x = x;
//            propertyChangeSupport.firePropertyChange("x", oldX, x);
//        }
//
//        public BeanWithParameterizedAddRemove(Object x) {this.x = x;}
//
//        public boolean hasChangeListeners() {
//            return changeListenerCount > 0;
//        }
//
//        public boolean hasVetoListeners() {
//            return vetoListenerCount > 0;
//        }
//
//        public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
//            changeListenerCount++;
//            propertyChangeSupport.addPropertyChangeListener(name, listener);
//        }
//
//        public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
//            changeListenerCount = Math.max(0, changeListenerCount-1);
//            propertyChangeSupport.removePropertyChangeListener(name, listener);
//        }
//
//        public void addVetoableChangeListener(String name, VetoableChangeListener listener) {
//            vetoListenerCount++;
//            vetoableChangeSupport.addVetoableChangeListener(name, listener);
//        }
//
//        public void removeVetoableChangeListener(String name, VetoableChangeListener listener) {
//            vetoListenerCount = Math.max(0, vetoListenerCount-1);
//            vetoableChangeSupport.removeVetoableChangeListener(name, listener);
//        }
//    }
//
//    public static class BeanWithNamedAddRemove {
//        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//        private final VetoableChangeSupport vetoableChangeSupport = new VetoableChangeSupport(this);
//
//        private Object x;
//        private int changeListenerCount;
//        private int vetoListenerCount;
//
//        public Object getX() {return x;}
//        public void setX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
//            vetoableChangeSupport.fireVetoableChange("x", oldX, x);
//            this.x = x;
//            propertyChangeSupport.firePropertyChange("x", oldX, x);
//        }
//
//        public BeanWithNamedAddRemove(Object x) {this.x = x;}
//
//        public boolean hasChangeListeners() {
//            return changeListenerCount > 0;
//        }
//
//        public boolean hasVetoListeners() {
//            return vetoListenerCount > 0;
//        }
//
//        public void addXListener(PropertyChangeListener listener) {
//            changeListenerCount++;
//            propertyChangeSupport.addPropertyChangeListener("x", listener);
//        }
//
//        public void removeXListener(PropertyChangeListener listener) {
//            changeListenerCount = Math.max(0, changeListenerCount-1);
//            propertyChangeSupport.removePropertyChangeListener("x", listener);
//        }
//
//        public void addXListener(VetoableChangeListener listener) {
//            vetoListenerCount++;
//            vetoableChangeSupport.addVetoableChangeListener("x", listener);
//        }
//
//        public void removeXListener(VetoableChangeListener listener) {
//            vetoListenerCount = Math.max(0, vetoListenerCount-1);
//            vetoableChangeSupport.removeVetoableChangeListener("x", listener);
//        }
//    }
//
//    public class BeanWithRawListenerSupport {
//        private final List<VetoableChangeListener> vetoListeners = new ArrayList<VetoableChangeListener>();
//        private final List<PropertyChangeListener> changeListeners = new ArrayList<PropertyChangeListener>();
//
//        private Object x;
//
//        public Object getX() {return x;}
//        public void setX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
//            final PropertyChangeEvent event = new PropertyChangeEvent(this, "x", oldX, x);
//            fireVetoableChange(event);
//            this.x = x;
//            firePropertyChange(event);
//        }
//
//        private void fireVetoableChange(PropertyChangeEvent event) throws PropertyVetoException {
//            for (final VetoableChangeListener listener : vetoListeners) {
//                listener.vetoableChange(event);
//            }
//        }
//
//        private void firePropertyChange(PropertyChangeEvent event) {
//            for (final PropertyChangeListener listener : changeListeners) {
//                listener.propertyChange(event);
//            }
//        }
//
//        public BeanWithRawListenerSupport(Object x) {this.x = x;}
//
//        public boolean hasChangeListeners() {
//            return !changeListeners.isEmpty();
//        }
//
//        public boolean hasVetoListeners() {
//            return !vetoListeners.isEmpty();
//        }
//
//        public void addPropertyChangeListener(PropertyChangeListener listener) {
//            changeListeners.add(listener);
//        }
//
//        public void removePropertyChangeListener(PropertyChangeListener listener) {
//            changeListeners.remove(listener);
//        }
//
//        public void addVetoableChangeListener(VetoableChangeListener listener) {
//            vetoListeners.add(listener);
//        }
//
//        public void removeVetoableChangeListener(VetoableChangeListener listener) {
//            vetoListeners.remove(listener);
//        }
//    }
//}
