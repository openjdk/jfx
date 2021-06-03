/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class ModuleHelper {
    private static final Method getModuleMethod;
    private static final Method getResourceAsStreamMethod;

    private static final boolean verbose;

    static {
        @SuppressWarnings("removal")
        boolean tmp = AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                Boolean.getBoolean("javafx.verbose"));
        verbose = tmp;

        if (verbose) {
            System.err.println("" + ModuleHelper.class.getName() + " : <clinit>");
        }
        Method mGetModule = null;
        Method mGetResourceAsStream = null;
        try {
            mGetModule = Class.class.getMethod("getModule");
            Class<?> moduleClass = mGetModule.getReturnType();
            mGetResourceAsStream = moduleClass.getMethod("getResourceAsStream", String.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        getModuleMethod = mGetModule;
        getResourceAsStreamMethod = mGetResourceAsStream;
        if (verbose) {
            System.err.println("getModuleMethod = " + getModuleMethod);
            System.err.println("getResourceAsStreamMethod = " + getResourceAsStreamMethod);
        }
    }

    public static Object getModule(Class clazz) {
        if (getModuleMethod != null) {
            try {
                return getModuleMethod.invoke(clazz);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    // FIXME: JIGSAW -- remove this method if not needed
    public static InputStream getResourceAsStream(Object thisModule, String name) {
        if (getResourceAsStreamMethod != null) {
            try {
                return (InputStream)getResourceAsStreamMethod.invoke(thisModule, name);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    public static Object invoke(Method m, Object obj, Object[] params)
            throws InvocationTargetException, IllegalAccessException
    {
        Object thisModule = getModule(ModuleHelper.class);
        Object methodModule = getModule(m.getDeclaringClass());
        if (verbose) {
            System.out.println("thisModule = " + thisModule);
            System.out.println("methodModule = " + methodModule);
            System.out.println("m = " + m);
        }
        if (methodModule != thisModule) {
            return MethodHelper.invoke(m, obj, params);
        } else {
            return m.invoke(obj, params);
        }
    }
}
