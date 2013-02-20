/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

//package javafx.beans.property.adapter;
//
//import javafx.beans.InvalidationListener;
//import javafx.beans.Observable;
//import javafx.beans.property.BooleanProperty;
//import javafx.beans.property.ReadOnlyObjectProperty;
//import javafx.beans.property.SimpleBooleanProperty;
//import javafx.beans.value.ChangeListener;
//import javafx.beans.value.ObservableValue;
//import org.junit.Test;
//
//import java.beans.*;
//import java.lang.reflect.Method;
//
//import static org.junit.Assert.*;
//
///**
//*/
//public class ReadOnlyJavaBeanPropertyBuilder_General_Test {
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNameIsNull() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder(null, POJOBean.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNameIsEmpty() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder("", POJOBean.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithBeanClassIsNull() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder("x", null);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void testSetup_WithNonStandardNames() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", POJOBeanWithNonStandardNames.class, "readX");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final Object initialValue = new Object();
//        final Object secondValue = new Object();
//        final POJOBeanWithNonStandardNames bean = new POJOBeanWithNonStandardNames(initialValue);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//        assertEquals(initialValue, property.get());
//        bean.updateX(secondValue);
//        assertEquals(secondValue, bean.readX());
//        assertEquals(secondValue, property.get());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardNames_WithNameIsNull() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder(null, POJOBeanWithNonStandardNames.class, "readX");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardNames_WithBeanClassIsNull() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder("x", null, "readX");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardNames_WithGetterNameIsNull() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder("x", POJOBeanWithNonStandardNames.class, (String)null);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNonStandardNames_WithNameIsEmpty() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder("", POJOBeanWithNonStandardNames.class, "readX");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNonStandardNames_WithGetterNameIsEmpty() {
//        try {
//            new ReadOnlyJavaBeanPropertyBuilder("x", POJOBeanWithNonStandardNames.class, "");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void testSetup_WithNonStandardAccessors() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", POJOBeanWithNonStandardNames.class, getter);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//        final Object initialValue = new Object();
//        final Object secondValue = new Object();
//        final POJOBeanWithNonStandardNames bean = new POJOBeanWithNonStandardNames(initialValue);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//        assertEquals(initialValue, property.get());
//        bean.updateX(secondValue);
//        assertEquals(secondValue, bean.readX());
//        assertEquals(secondValue, property.get());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardAccessors_WithNameIsNull() {
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            new ReadOnlyJavaBeanPropertyBuilder(null, POJOBeanWithNonStandardNames.class, getter);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardAccessors_WithBeanClassIsNull() {
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            new ReadOnlyJavaBeanPropertyBuilder("x", null, getter);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void testSetup_WithNonStandardAccessors_WithGetterIsNull() {
//        new ReadOnlyJavaBeanPropertyBuilder("x", POJOBeanWithNonStandardNames.class, (Method)null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSetup_WithNonStandardAccessors_WithNameIsEmpty() {
//        try {
//            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
//            new ReadOnlyJavaBeanPropertyBuilder("", POJOBeanWithNonStandardNames.class, getter);
//        } catch (NoSuchMethodException e) {
//            fail("Error in test code. Should not happen.");
//        }
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testCreatePropertyWithWrongType_Boolean() {
//        final POJOBean bean = new POJOBean(new Object());
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", POJOBean.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        builder.createBooleanProperty(bean);
//    }
//
//    @Test
//    public void testDisposal_GeneralAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//        ReadOnlyJavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        // initial state
//        assertEquals(value0, property.get());
//        assertTrue(bean.hasChangeListeners());
//
//        // dispose
//        property.dispose();
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            e.printStackTrace();
//            fail();
//        }
//        assertFalse(bean.hasChangeListeners());
//    }
//
//    @Test
//    public void testDisposal_ParameterizedAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithParameterizedAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithParameterizedAddRemove bean = new BeanWithParameterizedAddRemove(value0);
//        ReadOnlyJavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        // initial state
//        assertEquals(value0, property.get());
//        assertTrue(bean.hasChangeListeners());
//
//        // dispose
//        property.dispose();
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            e.printStackTrace();
//            fail();
//        }
//        assertFalse(bean.hasChangeListeners());
//    }
//
//    @Test
//    public void testDisposal_NamedAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithNamedAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithNamedAddRemove bean = new BeanWithNamedAddRemove(value0);
//        ReadOnlyJavaBeanObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        // initial state
//        assertEquals(value0, property.get());
//        assertTrue(bean.hasChangeListeners());
//
//        // dispose
//        property.dispose();
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            e.printStackTrace();
//            fail();
//        }
//        assertFalse(bean.hasChangeListeners());
//    }
//
//    @Test
//    public void testInvalidationListener_GeneralAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final BooleanProperty fired = new SimpleBooleanProperty(false);
//        final InvalidationListener listener = new InvalidationListener() {
//            @Override
//            public void invalidated(Observable observable) {
//                fired.set(true);
//            }
//        };
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        property.addListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertTrue(fired.get());
//
//        property.removeListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value0);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertFalse(fired.get());
//    }
//
//    @Test
//    public void testChangeListener_GeneralAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithGeneralAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final BooleanProperty fired = new SimpleBooleanProperty(false);
//        final ChangeListener<Object> listener = new ChangeListener<Object>() {
//            @Override
//            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
//                fired.set(true);
//            }
//        };
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithGeneralAddRemove bean = new BeanWithGeneralAddRemove(value0);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        property.addListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertTrue(fired.get());
//
//        property.removeListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value0);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertFalse(fired.get());
//    }
//
//    @Test
//    public void testInvalidationListener_ParameterizedAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithParameterizedAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final BooleanProperty fired = new SimpleBooleanProperty(false);
//        final InvalidationListener listener = new InvalidationListener() {
//            @Override
//            public void invalidated(Observable observable) {
//                fired.set(true);
//            }
//        };
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithParameterizedAddRemove bean = new BeanWithParameterizedAddRemove(value0);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        property.addListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertTrue(fired.get());
//
//        property.removeListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value0);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertFalse(fired.get());
//    }
//
//    @Test
//    public void testChangeListener_ParameterizedAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithParameterizedAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final BooleanProperty fired = new SimpleBooleanProperty(false);
//        final ChangeListener<Object> listener = new ChangeListener<Object>() {
//            @Override
//            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
//                fired.set(true);
//            }
//        };
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithParameterizedAddRemove bean = new BeanWithParameterizedAddRemove(value0);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        property.addListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertTrue(fired.get());
//
//        property.removeListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value0);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertFalse(fired.get());
//    }
//
//    @Test
//    public void testInvalidationListener_NamedAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithNamedAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final BooleanProperty fired = new SimpleBooleanProperty(false);
//        final InvalidationListener listener = new InvalidationListener() {
//            @Override
//            public void invalidated(Observable observable) {
//                fired.set(true);
//            }
//        };
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithNamedAddRemove bean = new BeanWithNamedAddRemove(value0);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        property.addListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertTrue(fired.get());
//
//        property.removeListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value0);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertFalse(fired.get());
//    }
//
//    @Test
//    public void testChangeListener_NamedAddRemove() {
//        ReadOnlyJavaBeanPropertyBuilder builder = null;
//        try {
//            builder = new ReadOnlyJavaBeanPropertyBuilder("x", BeanWithNamedAddRemove.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            fail();
//        }
//        final BooleanProperty fired = new SimpleBooleanProperty(false);
//        final ChangeListener<Object> listener = new ChangeListener<Object>() {
//            @Override
//            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
//                fired.set(true);
//            }
//        };
//        final Object value0 = new Object();
//        final Object value1 = new Object();
//        final BeanWithNamedAddRemove bean = new BeanWithNamedAddRemove(value0);
//        final ReadOnlyObjectProperty<Object> property = builder.createObjectProperty(bean);
//
//        property.addListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value1);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertTrue(fired.get());
//
//        property.removeListener(listener);
//        fired.set(false);
//        try {
//            bean.updateX(value0);
//        } catch (PropertyVetoException e) {
//            fail();
//        }
//        assertFalse(fired.get());
//    }
//
//    public static class POJOBean {
//        private Object x;
//
//        public POJOBean(Object x) {this.x = x;}
//
//        public Object getX() {return x;}
//    }
//
//    public static class POJOBeanWithNonStandardNames {
//        private Object x;
//
//        public POJOBeanWithNonStandardNames(Object x) {this.x = x;}
//
//        public Object readX() {return x;}
//        private void updateX(Object x) {this.x = x;}
//    }
//
//    public static class BeanWithGeneralAddRemove {
//        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//
//        private Object x;
//        private int changeListenerCount;
//
//        public Object getX() {return x;}
//        private void updateX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
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
//        public void addPropertyChangeListener(PropertyChangeListener listener) {
//            changeListenerCount++;
//            propertyChangeSupport.addPropertyChangeListener(listener);
//        }
//
//        public void removePropertyChangeListener(PropertyChangeListener listener) {
//            changeListenerCount = Math.max(0, changeListenerCount-1);
//            propertyChangeSupport.removePropertyChangeListener(listener);
//        }
//    }
//
//    public static class BeanWithParameterizedAddRemove {
//        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//
//        private Object x;
//        private int changeListenerCount;
//
//        public Object getX() {return x;}
//        private void updateX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
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
//        public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
//            changeListenerCount++;
//            propertyChangeSupport.addPropertyChangeListener(name, listener);
//        }
//
//        public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
//            changeListenerCount = Math.max(0, changeListenerCount-1);
//            propertyChangeSupport.removePropertyChangeListener(name, listener);
//        }
//    }
//
//    public static class BeanWithNamedAddRemove {
//        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
//
//        private Object x;
//        private int changeListenerCount;
//
//        public Object getX() {return x;}
//        private void updateX(Object x) throws PropertyVetoException {
//            final Object oldX = this.x;
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
//    }
//
//}
