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

import com.sun.javafx.test.binding.ReflectionHelper;

public final class PropertyReference {
    private final String propertyName;
    private final Class<?> valueType;
    private final Method getterMethod;
    private final Method setterMethod;

    public PropertyReference(final String propertyName,
                             final Class<?> valueType,
                             final Method getterMethod,
                             final Method setterMethod) {
        this.propertyName = propertyName;
        this.valueType = valueType;
        this.getterMethod = getterMethod;
        this.setterMethod = setterMethod;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public Object getValue(final Object object) {
        if (getterMethod == null) {
            throw new RuntimeException("No getter associated with "
                                           + propertyName + "!");
        }

        return ReflectionHelper.invokeMethod(object, getterMethod);
    }

    public void setValue(final Object object, final Object value) {
        if (setterMethod == null) {
            throw new RuntimeException("No setter associated with "
                                           + propertyName + "!");
        }

        ReflectionHelper.invokeMethod(object, setterMethod, value);
    }

    public static PropertyReference createForBean(final Class<?> beanClass,
                                                  final String propertyName) {
        final String capitalizedPropertyName = capitalizeName(propertyName);

        Method propertyGetterMethod;
        try {
            propertyGetterMethod = ReflectionHelper.getMethod(
                                           beanClass,
                                           "get" + capitalizedPropertyName);
        } catch (final RuntimeException eget) {
            // second try with is
            try {
                propertyGetterMethod = ReflectionHelper.getMethod(
                                               beanClass,
                                               "is" + capitalizedPropertyName);
            } catch (final RuntimeException eis) {
                throw new RuntimeException("Failed to obtain getter for "
                                               + propertyName + "!");
            }
        }

        final Class<?> propertyValueType = propertyGetterMethod.getReturnType();

        Method propertySetterMethod;
        try {
            propertySetterMethod = ReflectionHelper.getMethod(
                                           beanClass,
                                           "set" + capitalizedPropertyName,
                                           propertyValueType);
        } catch (final RuntimeException e) {
            // no setter
            propertySetterMethod = null;
        }

        return new PropertyReference(
                           propertyName,
                           propertyValueType,
                           propertyGetterMethod,
                           propertySetterMethod);
    }

    public static PropertyReference createForBuilder(
            final Class<?> builderClass,
            final String propertyName,
            final Class<?> propertyValueType) {
        try {
            final Method propertySetterMethod =
                    ReflectionHelper.getMethod(
                            builderClass,
                            propertyName,
                            propertyValueType);

            return new PropertyReference(
                               propertyName,
                               propertyValueType,
                               null,
                               propertySetterMethod);
        } catch (final RuntimeException e) {
            throw new RuntimeException("Failed to obtain setter for "
                                           + propertyName + "!");
        }
    }

    private static String capitalizeName(final String input) {
        return !input.isEmpty() 
                ? Character.toUpperCase(input.charAt(0)) + input.substring(1)
                : input;
    }
}
