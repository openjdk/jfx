/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Set;

public abstract class Utilities {

    private static Utilities instance;

    public static synchronized void setUtilities(Utilities util) {
        instance = util;
    }

    public static synchronized Utilities getUtilities() {
        return instance;
    }

    protected abstract Pasteboard createPasteboard();
    protected abstract PopupMenu createPopupMenu();
    protected abstract ContextMenu createContextMenu();

    // List of Class methods to allow
    private static final Set<String> CLASS_METHODS_ALLOW_LIST = Set.of(
        "getCanonicalName",
        "getEnumConstants",
        "getFields",
        "getMethods",
        "getName",
        "getPackageName",
        "getSimpleName",
        "getSuperclass",
        "getTypeName",
        "getTypeParameters",
        "isAssignableFrom",
        "isArray",
        "isEnum",
        "isInstance",
        "isInterface",
        "isLocalClass",
        "isMemberClass",
        "isPrimitive",
        "isSynthetic",
        "toGenericString",
        "toString"
    );

    // List of classes to reject
    private static final Set<String> CLASSES_REJECT_LIST = Set.of(
        "java.lang.ClassLoader",
        "java.lang.Module",
        "java.lang.Runtime",
        "java.lang.System"
    );

    // List of packages to reject
    private static final List<String> PACKAGES_REJECT_LIST = List.of(
        "java.lang.invoke",
        "java.lang.module",
        "java.lang.reflect",
        "java.security",
        "sun.misc"
    );

    @SuppressWarnings("removal")
    private static Object fwkInvokeWithContext(final Method method,
                                               final Object instance,
                                               final Object[] args,
                                               AccessControlContext acc)
            throws Throwable {

        final Class<?> clazz = method.getDeclaringClass();
        if (clazz.equals(java.lang.Class.class)) {
            // check list of allowed Class methods
            if (!CLASS_METHODS_ALLOW_LIST.contains(method.getName())) {
                throw new UnsupportedOperationException("invocation not supported");
            }
        } else {
            // check list of rejected class names
            final String className = clazz.getName();
            if (CLASSES_REJECT_LIST.contains(className)) {
                throw new UnsupportedOperationException("invocation not supported");
            }
            // check list of rejected packages
            PACKAGES_REJECT_LIST.forEach(packageName -> {
                if (className.startsWith(packageName + ".")) {
                    throw new UnsupportedOperationException("invocation not supported");
                }
            });
        }

        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Object>)
                    () -> MethodHelper.invoke(method, instance, args), acc);
        } catch (PrivilegedActionException ex) {
            Throwable cause = ex.getCause();
            if (cause == null)
                cause = ex;
            else if (cause instanceof InvocationTargetException
                && cause.getCause() != null)
                cause = cause.getCause();
            throw cause;
        }
    }
}
