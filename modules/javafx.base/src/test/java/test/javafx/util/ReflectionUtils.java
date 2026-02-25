/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    /**
     * Returns the value of a potentially private field of the specified object.
     * The field can be declared on any of the object's inherited classes.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object object, String fieldName) {
        Function<Class<?>, Field> getField = cls -> {
            try {
                var field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                return null;
            }
        };

        Class<?> cls = object.getClass();
        while (cls != null) {
            Field field = getField.apply(cls);
            if (field != null) {
                try {
                    return (T)field.get(object);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }

            cls = cls.getSuperclass();
        }

        throw new AssertionError("Field not found: " + fieldName);
    }

    /**
     * Invokes the specified method on the object, and returns a value.
     * The method can be declared on any of the object's inherited classes.
     *
     * @param object the object on which the method will be invoked
     * @param methodName the method name
     * @param args the arguments
     * @return the return value
     */
    public static Object invokeMethod(Object object, String methodName, Class<?>[] parameterTypes, Object... args) {
        Function<Class<?>, Method> getMethod = cls -> {
            try {
                var method = cls.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                return null;
            }
        };

        Class<?> cls = object.getClass();
        while (cls != null) {
            Method method = getMethod.apply(cls);
            if (method != null) {
                try {
                    return method.invoke(object, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new AssertionError(e);
                }
            }

            cls = cls.getSuperclass();
        }

        throw new AssertionError("Method not found: " + methodName);
    }
}
