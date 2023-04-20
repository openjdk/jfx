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

package com.sun.javafx.property.adapter;

import com.sun.javafx.property.MethodHelper;
import javafx.beans.property.Property;
import javafx.beans.property.adapter.ReadOnlyJavaBeanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 */
public class PropertyDescriptor<T> extends ReadOnlyPropertyDescriptor<T> {

    private static final String ADD_VETOABLE_LISTENER_METHOD_NAME = "addVetoableChangeListener";
    private static final String REMOVE_VETOABLE_LISTENER_METHOD_NAME = "removeVetoableChangeListener";
    private static final String ADD_PREFIX = "add";
    private static final String REMOVE_PREFIX = "remove";
    private static final String SUFFIX = "Listener";

    private static final int ADD_VETOABLE_LISTENER_TAKES_NAME = 1;
    private static final int REMOVE_VETOABLE_LISTENER_TAKES_NAME = 2;

    private final Method setter;
    private final Method addVetoListener;
    private final Method removeVetoListener;
    private final int flags;

    public Method getSetter() {return setter;}

    public PropertyDescriptor(String propertyName, Class<?> beanClass, Method getter, Method setter) {
        super(propertyName, beanClass, getter);
        this.setter = setter;

        Method tmpAddVetoListener = null;
        Method tmpRemoveVetoListener = null;
        int tmpFlags = 0;

        // reflect addVetoListenerMethod
        final String addMethodName = ADD_PREFIX + capitalizedName(name) + SUFFIX;
        try {
            tmpAddVetoListener = beanClass.getMethod(addMethodName, VetoableChangeListener.class);
        } catch (NoSuchMethodException e) {
            try {
                tmpAddVetoListener = beanClass.getMethod(ADD_VETOABLE_LISTENER_METHOD_NAME, String.class, VetoableChangeListener.class);
                tmpFlags |= ADD_VETOABLE_LISTENER_TAKES_NAME;
            } catch (NoSuchMethodException e1) {
                try {
                    tmpAddVetoListener = beanClass.getMethod(ADD_VETOABLE_LISTENER_METHOD_NAME, VetoableChangeListener.class);
                } catch (NoSuchMethodException e2) {
                    // ignore
                }
            }
        }

        // reflect removeVetoListenerMethod
        final String removeMethodName = REMOVE_PREFIX + capitalizedName(name) + SUFFIX;
        try {
            tmpRemoveVetoListener = beanClass.getMethod(removeMethodName, VetoableChangeListener.class);
        } catch (NoSuchMethodException e) {
            try {
                tmpRemoveVetoListener = beanClass.getMethod(REMOVE_VETOABLE_LISTENER_METHOD_NAME, String.class, VetoableChangeListener.class);
                tmpFlags |= REMOVE_VETOABLE_LISTENER_TAKES_NAME;
            } catch (NoSuchMethodException e1) {
                try {
                    tmpRemoveVetoListener = beanClass.getMethod(REMOVE_VETOABLE_LISTENER_METHOD_NAME, VetoableChangeListener.class);
                } catch (NoSuchMethodException e2) {
                    // ignore
                }
            }
        }

        addVetoListener = tmpAddVetoListener;
        removeVetoListener = tmpRemoveVetoListener;
        flags = tmpFlags;
    }

    @Override
    public void addListener(ReadOnlyListener listener) {
        super.addListener(listener);
        if (addVetoListener != null) {
            try {
                if ((flags & ADD_VETOABLE_LISTENER_TAKES_NAME) > 0) {
                    addVetoListener.invoke(listener.getBean(), name, listener);
                } else {
                    addVetoListener.invoke(listener.getBean(), listener);
                }
            } catch (IllegalAccessException e) {
                // ignore
            } catch (InvocationTargetException e) {
                // ignore
            }
        }
    }



    @Override
    public void removeListener(ReadOnlyListener listener) {
        super.removeListener(listener);
        if (removeVetoListener != null) {
            try {
                if ((flags & REMOVE_VETOABLE_LISTENER_TAKES_NAME) > 0) {
                    removeVetoListener.invoke(listener.getBean(), name, listener);
                } else {
                    removeVetoListener.invoke(listener.getBean(), listener);
                }
            } catch (IllegalAccessException e) {
                // ignore
            } catch (InvocationTargetException e) {
                // ignore
            }
        }
    }

    public class Listener extends ReadOnlyListener implements ChangeListener<T>, VetoableChangeListener {

        private boolean updating;

        public Listener(Object bean, ReadOnlyJavaBeanProperty<T> property) {
            super(bean, property);
        }

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            final ReadOnlyJavaBeanProperty<T> property = checkRef();
            if (property == null) {
                observable.removeListener(this);
            } else if (!updating) {
                updating = true;
                try {
                    MethodHelper.invoke(setter, bean, new Object[] {newValue});
                    property.fireValueChangedEvent();
                } catch (IllegalAccessException e) {
                    // ignore
                } catch (InvocationTargetException e) {
                    // ignore
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void vetoableChange(PropertyChangeEvent propertyChangeEvent) throws PropertyVetoException {
            if (bean.equals(propertyChangeEvent.getSource()) && name.equals(propertyChangeEvent.getPropertyName())) {
                if ((checkRef() instanceof Property<?> property) && property.isBound() && !updating) {
                    throw new PropertyVetoException("A bound value cannot be set.", propertyChangeEvent);
                }
            }
        }
    }

}
