/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class to check for the presence of a security manager.
 */
public class SecurityUtil {

    // Prevent class from being instantiated.
    private SecurityUtil() {}

    /**
     * Check for the presence of a security manager (from an older JDK) and
     * throw UnsupportedOperationException if enabled. Use reflection to avoid
     * a dependency on an API that is deprecated for removal. This method does
     * nothing if the security manager is not enabled or if
     * System::getSecurityManager cannot be invoked.
     *
     * @throws UnsupportedOperationException if the security manager is enabled
     */
    public static void checkSecurityManager() {
        try {
            // Call System.getSecurityManager() using reflection. Throw an
            // UnsupportedOperationException if it returns a non-null object.
            // If we cannot find or invoke getSecurityManager, ignore the error.
            Method meth = System.class.getMethod("getSecurityManager");
            Object sm = meth.invoke(null);
            if (sm != null) {
                throw new UnsupportedOperationException("JavaFX does not support running with the Security Manager");
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // Ignore the error
        }
    }
}
