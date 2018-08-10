/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class ModuleHelper {
    private static final Method getModuleMethod;
    private static final Method addReadsMethod;
    private static final Method addExportsMethod;

    private static final boolean verbose;

    static {
        verbose = AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                Boolean.getBoolean("javafx.verbose"));

        if (verbose) {
            System.err.println("" + ModuleHelper.class.getName() + " : <clinit>");
        }
        Method mGetModule = null;
        Method mAddReads = null;
        Method mAddExports = null;
        try {
            mGetModule = Class.class.getMethod("getModule");
            Class<?> moduleClass = mGetModule.getReturnType();
            mAddReads = moduleClass.getMethod("addReads", moduleClass);
            mAddExports = moduleClass.getMethod("addExports", String.class, moduleClass);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        getModuleMethod = mGetModule;
        addReadsMethod = mAddReads;
        addExportsMethod = mAddExports;
        if (verbose) {
            System.err.println("getModuleMethod = " + getModuleMethod);
            System.err.println("addReadsMethod = " + addReadsMethod);
            System.err.println("addExportsMethod = " + addExportsMethod);
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

    public static void addReads(Object thisModule, Object targetModule) {
        if (addReadsMethod != null) {
            try {
                addReadsMethod.invoke(thisModule, targetModule);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void addExports(Object thisModule, String packageName, Object targetModule) {
        if (addExportsMethod != null) {
            try {
                addExportsMethod.invoke(thisModule, packageName, targetModule);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
