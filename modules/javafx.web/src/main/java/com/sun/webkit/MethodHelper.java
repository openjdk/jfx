/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.reflect.MethodUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.javafx.reflect.ReflectUtil;

/**
 * Utility class to wrap method invocation.
 */
public class MethodHelper {
    private static final boolean logAccessErrors
            = AccessController.doPrivileged((PrivilegedAction<Boolean>) ()
                    -> Boolean.getBoolean("sun.reflect.debugModuleAccessChecks"));

    private static final Module trampolineModule = MethodUtil.getTrampolineModule();

    public static Object invoke(Method m, Object obj, Object[] params)
            throws InvocationTargetException, IllegalAccessException {

        // Check that the class in question is in a package that is open to
        // this module (or exported unconditionally). If so, then we will open
        // the containing package to the unnamed trampoline module. If not,
        // we will throw an IllegalAccessException in order to generate a
        // clearer error message.
        final Class<?> clazz = m.getDeclaringClass();
        final String packageName = clazz.getPackage().getName();
        final Module module = clazz.getModule();
        final Module thisModule = MethodHelper.class.getModule();
        try {
            // Verify that the module being called either exports the package
            // in question unconditionally or opens the package in question to
            // this module.
            if (!module.isExported(packageName)) {
                if (!module.isOpen(packageName, thisModule)) {
                    throw new IllegalAccessException(
                            "module " + thisModule.getName()
                            + " cannot access class " + clazz.getName()
                            + " (in module " + module.getName()
                            + ") because module " + module.getName()
                            + " does not open " + packageName
                            + " to " + thisModule.getName());
                }
                if (!module.isOpen(packageName, trampolineModule)) {
                    ReflectUtil.checkPackageAccess(packageName);
                    module.addOpens(packageName, trampolineModule);
                }
            }
        } catch (IllegalAccessException ex) {
            if (logAccessErrors) {
                ex.printStackTrace(System.err);
            }
            throw ex;
        }

        return MethodUtil.invoke(m, obj, params);
    }

    // Utility class, do not instantiate
    private MethodHelper() {
    }

}
