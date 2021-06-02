/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.javafx.reflect;

import java.lang.reflect.Proxy;

public final class ReflectUtil {

    private ReflectUtil() {
    }

    /**
     * Checks package access on the given class.
     *
     * If it is a {@link Proxy#isProxyClass(java.lang.Class)} that implements
     * a non-public interface (i.e. may be in a non-restricted package),
     * also check the package access on the proxy interfaces.
     */
    public static void checkPackageAccess(Class<?> clazz) {
        @SuppressWarnings("removal")
        SecurityManager s = System.getSecurityManager();
        if (s != null) {
            privateCheckPackageAccess(s, clazz);
        }
    }

    /**
     * NOTE: should only be called if a SecurityManager is installed
     */
    private static void privateCheckPackageAccess(@SuppressWarnings("removal") SecurityManager s, Class<?> clazz) {
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }

        String pkg = clazz.getPackageName();
        if (pkg != null && !pkg.isEmpty()) {
            s.checkPackageAccess(pkg);
        }

        if (isNonPublicProxyClass(clazz)) {
            privateCheckProxyPackageAccess(s, clazz);
        }
    }

    /**
     * Checks package access on the given classname.
     * This method is typically called when the Class instance is not
     * available and the caller attempts to load a class on behalf
     * the true caller (application).
     */
    public static void checkPackageAccess(String name) {
        @SuppressWarnings("removal")
        SecurityManager s = System.getSecurityManager();
        if (s != null) {
            String cname = name.replace('/', '.');
            if (cname.startsWith("[")) {
                int b = cname.lastIndexOf('[') + 2;
                if (b > 1 && b < cname.length()) {
                    cname = cname.substring(b);
                }
            }
            int i = cname.lastIndexOf('.');
            if (i != -1) {
                s.checkPackageAccess(cname.substring(0, i));
            }
        }
    }

    public static boolean isPackageAccessible(Class<?> clazz) {
        try {
            checkPackageAccess(clazz);
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    /**
     * NOTE: should only be called if a SecurityManager is installed
     */
    private static void privateCheckProxyPackageAccess(@SuppressWarnings("removal") SecurityManager s, Class<?> clazz) {
        // check proxy interfaces if the given class is a proxy class
        if (Proxy.isProxyClass(clazz)) {
            for (Class<?> intf : clazz.getInterfaces()) {
                privateCheckPackageAccess(s, intf);
            }
        }
    }

    // Note that bytecode instrumentation tools may exclude 'sun.*'
    // classes but not generated proxy classes and so keep it in com.sun.*
    public static final String PROXY_PACKAGE = "com.sun.proxy";

    /**
     * Test if the given class is a proxy class that implements
     * non-public interface.  Such proxy class may be in a non-restricted
     * package that bypasses checkPackageAccess.
     */
    public static boolean isNonPublicProxyClass(Class<?> cls) {
        if (!Proxy.isProxyClass(cls)) {
            return false;
        }
        String pkg = cls.getPackageName();
        return pkg == null || !pkg.startsWith(PROXY_PACKAGE);
    }
}
