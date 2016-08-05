/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.property;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javafx.beans.property.ReadOnlyObjectProperty;

public final class JavaBeanAccessHelper {

    private static Method JAVA_BEAN_QUICK_ACCESSOR_CREATE_RO;

    private static boolean initialized;

    private JavaBeanAccessHelper() {

    }

    public static <T> ReadOnlyObjectProperty<T> createReadOnlyJavaBeanProperty(Object bean, String propertyName) throws NoSuchMethodException{
        init();
        if (JAVA_BEAN_QUICK_ACCESSOR_CREATE_RO == null) {
            throw new UnsupportedOperationException("Java beans are not supported.");
        }
        try {
            return (ReadOnlyObjectProperty<T>) JAVA_BEAN_QUICK_ACCESSOR_CREATE_RO.invoke(null, bean, propertyName);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedOperationException("Java beans are not supported.");
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof NoSuchMethodException) {
                throw (NoSuchMethodException)ex.getCause();
            }
            throw new UnsupportedOperationException("Java beans are not supported.");
        }
    }

    private static void init() {
        if (!initialized) {
            try {
                Class accessor = Class.forName(
                        "com.sun.javafx.property.adapter.JavaBeanQuickAccessor",
                        true, JavaBeanAccessHelper.class.getClassLoader());
                JAVA_BEAN_QUICK_ACCESSOR_CREATE_RO =
                        accessor.getDeclaredMethod("createReadOnlyJavaBeanObjectProperty",
                        Object.class, String.class);
            } catch (ClassNotFoundException ex) {
                //ignore
            } catch (NoSuchMethodException ex) {
                //ignore
            }
            initialized = true;
        }
    }

}
