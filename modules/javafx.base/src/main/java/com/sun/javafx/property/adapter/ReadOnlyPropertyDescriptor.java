/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.WeakListener;
import javafx.beans.property.adapter.ReadOnlyJavaBeanProperty;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.sun.javafx.reflect.ReflectUtil;

import static java.util.Locale.ENGLISH;

/**
 */
public class ReadOnlyPropertyDescriptor {

    private static final String ADD_LISTENER_METHOD_NAME = "addPropertyChangeListener";
    private static final String REMOVE_LISTENER_METHOD_NAME = "removePropertyChangeListener";
    private static final String ADD_PREFIX = "add";
    private static final String REMOVE_PREFIX = "remove";
    private static final String SUFFIX = "Listener";

    private static final int ADD_LISTENER_TAKES_NAME = 1;
    private static final int REMOVE_LISTENER_TAKES_NAME = 2;

    protected final String name;
    protected final Class<?> beanClass;
    private final Method getter;
    private final Class<?> type;

    private final Method addChangeListener;
    private final Method removeChangeListener;
    private final int flags;

    public String getName() {return name;}
    public Method getGetter() {return getter;}
    public Class<?> getType() {return type;}

    public ReadOnlyPropertyDescriptor(String propertyName, Class<?> beanClass, Method getter) {
        ReflectUtil.checkPackageAccess(beanClass);

        this.name = propertyName;
        this.beanClass = beanClass;
        this.getter = getter;
        this.type = getter.getReturnType();

        Method tmpAddChangeListener = null;
        Method tmpRemoveChangeListener = null;
        int tmpFlags = 0;

        try {
            final String methodName = ADD_PREFIX + capitalizedName(name) + SUFFIX;
            tmpAddChangeListener = beanClass.getMethod(methodName, PropertyChangeListener.class);
        } catch (NoSuchMethodException e) {
            try {
                tmpAddChangeListener = beanClass.getMethod(ADD_LISTENER_METHOD_NAME, String.class, PropertyChangeListener.class);
                tmpFlags |= ADD_LISTENER_TAKES_NAME;
            } catch (NoSuchMethodException e1) {
                try {
                    tmpAddChangeListener = beanClass.getMethod(ADD_LISTENER_METHOD_NAME, PropertyChangeListener.class);
                } catch (NoSuchMethodException e2) {
                    // ignore
                }
            }
        }

        try {
            final String methodName = REMOVE_PREFIX + capitalizedName(name) + SUFFIX;
            tmpRemoveChangeListener = beanClass.getMethod(methodName, PropertyChangeListener.class);
        } catch (NoSuchMethodException e) {
            try {
                tmpRemoveChangeListener = beanClass.getMethod(REMOVE_LISTENER_METHOD_NAME, String.class, PropertyChangeListener.class);
                tmpFlags |= REMOVE_LISTENER_TAKES_NAME;
            } catch (NoSuchMethodException e1) {
                try {
                    tmpRemoveChangeListener = beanClass.getMethod(REMOVE_LISTENER_METHOD_NAME, PropertyChangeListener.class);
                } catch (NoSuchMethodException e2) {
                    // ignore
                }
            }
        }

        addChangeListener = tmpAddChangeListener;
        removeChangeListener = tmpRemoveChangeListener;
        flags = tmpFlags;
    }

    public static String capitalizedName(String name) {
        return ((name == null) || (name.length() == 0))? name : name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
    }

    public void addListener(ReadOnlyListener listener) {
        if (addChangeListener != null) {
            try {
                if ((flags & ADD_LISTENER_TAKES_NAME) > 0) {
                    addChangeListener.invoke(listener.getBean(), name, listener);
                } else {
                    addChangeListener.invoke(listener.getBean(), listener);
                }
            } catch (IllegalAccessException e) {
                // ignore
            } catch (InvocationTargetException e) {
                // ignore
            }
        }
    }

    public void removeListener(ReadOnlyListener listener) {
        if (removeChangeListener != null) {
            try {
                if ((flags & REMOVE_LISTENER_TAKES_NAME) > 0) {
                    removeChangeListener.invoke(listener.getBean(), name, listener);
                } else {
                    removeChangeListener.invoke(listener.getBean(), listener);
                }
            } catch (IllegalAccessException e) {
                // ignore
            } catch (InvocationTargetException e) {
                // ignore
            }
        }
    }

    public class ReadOnlyListener<T> implements PropertyChangeListener, WeakListener {

        protected final Object bean;
        private final WeakReference<ReadOnlyJavaBeanProperty<T>> propertyRef;

        public Object getBean() {return bean;}

        public ReadOnlyListener(Object bean, ReadOnlyJavaBeanProperty<T> property) {
            this.bean = bean;
            this.propertyRef = new WeakReference<ReadOnlyJavaBeanProperty<T>>(property);
        }

        protected ReadOnlyJavaBeanProperty<T> checkRef() {
            final ReadOnlyJavaBeanProperty<T> result = propertyRef.get();
            if (result == null) {
                removeListener(this);
            }
            return result;
        }

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            if (bean.equals(propertyChangeEvent.getSource()) && name.equals(propertyChangeEvent.getPropertyName())) {
                final ReadOnlyJavaBeanProperty<T> property = checkRef();
                if (property != null) {
                    property.fireValueChangedEvent();
                }
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return checkRef() == null;
        }
    }
}
