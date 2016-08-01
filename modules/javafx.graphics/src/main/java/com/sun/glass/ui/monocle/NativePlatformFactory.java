/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

/** Abstract factory class to instantiate a NativePlatform */
public abstract class NativePlatformFactory {

    /**
     * Checks whether this NativePlatformFactory can work with the platform on
     * which we are running
     *
     * @return true if we can run on this platform.
     */
    protected abstract boolean matches();

    /**
     * Creates a NativePlatform. This is only called if matches() was
     * previously called and returned true.
     *
     * @return a new NativePlatform.
     */
    protected abstract NativePlatform createNativePlatform();

    /**
     * Queries the major version number supported by this NativePlatformFactory.
     *
     * @return the major version supported
     */
    protected abstract int getMajorVersion();

    /**
     * Queries the minor version number supported by this NativePlatformFactory.
     *
     * @return the minor version supported
     */
    protected abstract int getMinorVersion();

    private static NativePlatform platform;
    private static final int majorVersion = 1;
    private static final int minorVersion = 0;

    /**
     * Obtains a NativePlatform that matches the platform on which we are running.
     *
     * The system property monocle.platform defines a series of cascading
     * fallbacks for what NativePlatform types to attempt to create. monocle
     * .platform can be overridden to select a specific platform. For
     * example, running with -Dmonocle.platform=Dispman,
     * Linux selects the NativePlatform that works with the dispmanx
     * libraries on the Raspberry Pi, but falls back to a generic
     * software-rendered Linux framebuffer implementation if we are not
     * running on a device with dispmanx libraries.
     *
     * @return a new NativePlatform
     */
    public static synchronized NativePlatform getNativePlatform() {
        if (platform == null) {
            String platformFactoryProperty =
                    AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("monocle.platform",
                                              "MX6,OMAP,Dispman,Android,X11,Linux,Headless"));
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
                    final ClassLoader loader = NativePlatformFactory.class.getClassLoader();
                    final Class<?> clazz = Class.forName(factoryClassName, false, loader);
                    if (!NativePlatformFactory.class.isAssignableFrom(clazz)) {
                        throw new IllegalArgumentException("Unrecognized Monocle platform: "
                                + factoryClassName);
                    }
                    NativePlatformFactory npf = (NativePlatformFactory) clazz.newInstance();
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
