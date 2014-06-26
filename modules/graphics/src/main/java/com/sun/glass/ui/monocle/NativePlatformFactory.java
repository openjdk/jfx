/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class NativePlatformFactory {

    protected abstract boolean matches();

    protected abstract NativePlatform createNativePlatform();

    protected abstract int getMajorVersion();

    protected abstract int getMinorVersion();

    private static NativePlatform platform;
    private static final int majorVersion = 1;
    private static final int minorVersion = 0;
    public static synchronized NativePlatform getNativePlatform() {
        if (platform == null) {
            String platformFactoryProperty =
                    AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("monocle.platform",
                                              "MX6,OMAP,Dispman,X11,Linux,Headless"));
            String[] platformFactories = platformFactoryProperty.split(",");
            for (int i = 0; i < platformFactories.length; i++) {
                String factoryName = platformFactories[i].trim();
                String factoryClassName;
                if (factoryName.contains(".")) {
                    factoryClassName = factoryName;
                } else {
                    factoryClassName = "com.sun.glass.ui.monocle."
                            + factoryName + "PlatformFactory";
                }
                if (MonocleSettings.settings.tracePlatformConfig) {
                    MonocleTrace.traceConfig("Trying platform %s with class %s",
                                             factoryName, factoryClassName);
                }
                try {
                    NativePlatformFactory npf = (NativePlatformFactory)
                            Class.forName(factoryClassName)
                            .newInstance();
                    if (npf.matches() &&
                        npf.getMajorVersion() == majorVersion &&
                        npf.getMinorVersion() == minorVersion) {
                        platform = npf.createNativePlatform();
                        if (MonocleSettings.settings.tracePlatformConfig) {
                            MonocleTrace.traceConfig("Matched %s", factoryName);
                        }
                        return platform;
                    }
                } catch (Exception e) {
                    if (MonocleSettings.settings.tracePlatformConfig) {
                        MonocleTrace.traceConfig("Failed to create platform %s",
                                                 factoryClassName);
                    }
                    e.printStackTrace();
                }
            }
            throw new UnsupportedOperationException(
                    "Cannot load a native platform from: '"
                    + platformFactoryProperty + "'");
        }
        return platform;
    }

}
