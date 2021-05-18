/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.test.binding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionHelper {
    private ReflectionHelper() {
    }

    public static Class<?> classForName(final String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException e) {
            throw convertToRuntimeException(e);
        }
    }

    public static Object newInstance(final Class<?> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw convertToRuntimeException(e);
        }
    }

    public static Method getMethod(final Class<?> cls,
                                   final String methodName,
                                   final Class<?>... parameterTypes) {
        try {
            return cls.getMethod(methodName, parameterTypes);
        } catch (final NoSuchMethodException e) {
            throw convertToRuntimeException(e);
        } catch (final SecurityException e) {
            throw convertToRuntimeException(e);
        }
    }

    public static Object invokeMethod(final Object object,
                                      final Method method,
                                      final Object... args) {
        try {
            return method.invoke(object, args);
        } catch (final IllegalAccessException e) {
            throw convertToRuntimeException(e);
        } catch (final IllegalArgumentException e) {
            throw convertToRuntimeException(e);
        } catch (final InvocationTargetException e) {
            throw convertToRuntimeException(e);
        }
    }

    private static RuntimeException convertToRuntimeException(
            final Exception e) {
        return new RuntimeException(e);
    }
}
