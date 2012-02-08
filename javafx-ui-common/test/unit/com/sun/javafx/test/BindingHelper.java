/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.test;

import java.lang.reflect.Method;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.WritableObjectValue;

import com.sun.javafx.test.binding.BindingProxy;
import com.sun.javafx.test.binding.BindingProxyRefImpl;
import com.sun.javafx.test.binding.ReflectionHelper;

public final class BindingHelper {
    private BindingHelper() {
    }

    public static Object getPropertyModel(
            final Object bean,
            final PropertyReference propertyReference) {
        final Method modelMethod = ReflectionHelper.getMethod(
                                         bean.getClass(),
                                         propertyReference.getPropertyName()
                                             + "Property");

        return ReflectionHelper.invokeMethod(bean, modelMethod);
    }

    public static Object createVariable(final Object value) {
        final Class<?> typeClass = (value != null) ? value.getClass()
                                                   : Object.class;
        final Object variable = createVariable(typeClass);
        setWritableValue(typeClass, variable, value);
        return variable;
    }

    public static Object createVariable(final Class<?> typeClass) {
        final BindingProxy bindingProxy = getBindingProxy(typeClass);
        return bindingProxy.getVariableFactory().createVariable();
    }

    public static Object getObservableValue(final Class<?> typeClass,
                                            final Object observableValue) {
        final BindingProxy bindingProxy = getBindingProxy(typeClass);
        return bindingProxy.getObservableValueProxy().getValue(observableValue);
    }

    public static void setWritableValue(final Object writableValue,
                                        final Object value) {
        setWritableValue(value.getClass(), writableValue, value);
    }
    
    public static void setWritableValue(final Class<?> typeClass,
                                        final Object writableValue,
                                        final Object value) {
        final BindingProxy bindingProxy = getBindingProxy(typeClass);
        bindingProxy.getWritableValueProxy().setValue(writableValue, value);
    }

    public static void bind(final Object bean,
                            final PropertyReference propertyReference,
                            final Object observableValue) {
        final Object propertyModel = getPropertyModel(bean, propertyReference);
        bind(propertyReference.getValueType(), propertyModel, observableValue);
    }

    public static void bind(
            final Class<?> typeClass,
            final Object propertyModel,
            final Object observableValue) {
        final BindingProxy bindingProxy = getBindingProxy(typeClass);
        bindingProxy.getPropertyModelProxy().bind(propertyModel,
                                                  observableValue);
    }

    public static void unbind(final Object bean,
                              final PropertyReference propertyReference) {
        final Object propertyModel = getPropertyModel(bean, propertyReference);
        unbind(propertyReference.getValueType(), propertyModel);
    }

    public static void unbind(final Class<?> typeClass,
                              final Object propertyModel) {
        final BindingProxy bindingProxy = getBindingProxy(typeClass);
        bindingProxy.getPropertyModelProxy().unbind(propertyModel);
    }

    private static final BindingProxy[] BINDING_PROXY_LIST = {
        BindingProxyRefImpl.autoLookup("Boolean"),
        BindingProxyRefImpl.autoLookup("Integer"),
        BindingProxyRefImpl.autoLookup("Long"),
        BindingProxyRefImpl.autoLookup("Float"),
        BindingProxyRefImpl.autoLookup("Double"),
        // special handling of String
        // (WritableStringValue is not defined)
        new BindingProxyRefImpl(String.class,
                                ObservableStringValue.class,
                                WritableObjectValue.class,
                                SimpleStringProperty.class,
                                SimpleStringProperty.class),
        BindingProxyRefImpl.autoLookup("Object")
    };

    private static BindingProxy getBindingProxy(
            Class<?> typeClass) {
        if (typeClass.isPrimitive()) {
            typeClass = getWrapperClassForPrimitiveType(typeClass);
        }

        for (final BindingProxy proxy: BINDING_PROXY_LIST) {
            if (proxy.getTypeClass().isAssignableFrom(typeClass)) {
                return proxy;
            }
        }

        // won't happen
        throw new RuntimeException();
    }

    private static Class<?> getWrapperClassForPrimitiveType(
            final Class<?> typeClass) {
        if (typeClass == boolean.class) {
            return Boolean.class;
        } else if (typeClass == int.class) {
            return Integer.class;
        } else if (typeClass == long.class) {
            return Long.class;
        } else if (typeClass == float.class) {
            return Float.class;
        } else if (typeClass == double.class) {
            return Double.class;
        } else {
            return null;
        }
    }
}
