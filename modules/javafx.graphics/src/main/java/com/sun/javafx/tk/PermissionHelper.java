/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk;

import java.lang.reflect.Constructor;
import java.security.Permission;
import static com.sun.javafx.FXPermissions.ACCESS_CLIPBOARD_PERMISSION;
import java.security.AccessControlContext;
import java.security.AccessControlException;

public class PermissionHelper {

    private static boolean awtInitialized = false;
    private static Permission awtClipboardPermission;

    // Method to get the AWT access clipboard permission. It should
    // only be called if the FXPermission check fails, since it will load
    // and initialize an AWT class from the java.desktop module.
    private static synchronized Permission getAWTClipboardPermission() {
        if (!awtInitialized) {
            // Use refelction to avoid hard dependency on AWT.
            // If the class cannot be loaded, then no fallback is possible, so
            // just set it to null

            try {
                Class clazz = Class.forName("java.awt.AWTPermission",
                        false, PermissionHelper.class.getClassLoader());
                // FIXME JIGSAW: add read edge
                Constructor c = clazz.getConstructor(String.class);
                awtClipboardPermission = (Permission) c.newInstance("accessClipboard");
            } catch (Exception ex) {
                awtClipboardPermission = null;
            }

            awtInitialized = true;
        }

        return awtClipboardPermission;
    }

    public static void checkClipboardPermission() {
        @SuppressWarnings("removal")
        final SecurityManager securityManager = System.getSecurityManager();

        // Always succeed if no security manager installed
        if (securityManager == null) return;

        // Check for FXPermission, using AWTPermission as fallback for compatibility
        try {
            securityManager.checkPermission(ACCESS_CLIPBOARD_PERMISSION);
        } catch (SecurityException ex) {
            // Try fallback if available
            final Permission perm = getAWTClipboardPermission();
            if (perm == null) throw ex;

            try {
                securityManager.checkPermission(perm);
            } catch (SecurityException ex2) {
                // Rethrow original exception
                throw ex;
            }
        }
    }

    @SuppressWarnings("removal")
    public static void checkClipboardPermission(AccessControlContext context) {
        final SecurityManager securityManager = System.getSecurityManager();

        // Always succeed if no security manager installed
        if (securityManager == null) return;

        if (context == null) {
            throw new AccessControlException("AccessControlContext is null");
        }

        // Check for FXPermission, using AWTPermission as fallback for compatibility
        try {
            //
            securityManager.checkPermission(ACCESS_CLIPBOARD_PERMISSION, context);
        } catch (SecurityException ex) {
            // Try fallback if available
            final Permission perm = getAWTClipboardPermission();
            if (perm == null) throw ex;

            try {
                securityManager.checkPermission(perm, context);
            } catch (SecurityException ex2) {
                // Rethrow original exception
                throw ex;
            }
        }
    }

    // Static helper class; do not construct an instance
    private PermissionHelper() {}
}
