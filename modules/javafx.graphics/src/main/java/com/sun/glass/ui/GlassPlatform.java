/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import com.sun.javafx.PlatformUtil;

import java.util.Locale;

/**
 * Graphics related platform checks, based on the Glass and Prism system properties.
 */
public final class GlassPlatform {

    private static final String MAC = "Mac";
    private static final String WINDOWS = "Win";
    private static final String GTK = "Gtk";
    private static final String IOS = "Ios";
    private static final String HEADLESS = "Headless";
    private static final String MONOCLE = "Monocle";

    private static final String PLATFORM_FACTORY;

    private static final boolean USE_EGL;
    private static final boolean IS_EMBEDDED;
    private static final boolean IS_HEADLESS;
    private static final boolean IS_MONOCLE;
    private static final boolean IS_SWT;
    private static final boolean IS_ACCESSIBILITY_ENABLED;

    static {
        // PlatformUtil must be initialized first (in getPlatform()), as it may set the system properties read below.
        String platform = getPlatform();
        if (platform != null) {
            PLATFORM_FACTORY = "com.sun.glass.ui." + platform.toLowerCase(Locale.ROOT) + "." + platform + "PlatformFactory";
        } else {
            PLATFORM_FACTORY = null;
        }

        USE_EGL = Boolean.getBoolean("use.egl");
        IS_EMBEDDED = Boolean.getBoolean("com.sun.javafx.isEmbedded");

        IS_HEADLESS = HEADLESS.equals(platform);
        IS_MONOCLE = MONOCLE.equals(platform);
        IS_SWT = "swt".equals(platform);

        String override = System.getProperty("glass.accessible.force");
        if (override != null) {
            IS_ACCESSIBILITY_ENABLED = Boolean.parseBoolean(override);
        } else {
            IS_ACCESSIBILITY_ENABLED = PlatformUtil.isMac() || PlatformUtil.isWindows();
        }
    }

    /**
     * Returns the fully qualified Glass platform factory class (name), or {@code null} if it could not be determined.
     */
    public static String getPlatformFactory() {
        return PLATFORM_FACTORY;
    }

    /**
     * Returns true if the Headless glass platform is selected.
     */
    public static boolean isHeadless() {
        return IS_HEADLESS;
    }

    /**
     * Returns true if EGL is used.
     */
    public static boolean useEGL() {
        return USE_EGL;
    }

    /**
     * Returns true if the Monocle glass platform is selected.
     */
    public static boolean isMonocle() {
        return IS_MONOCLE;
    }

    /**
     * Returns true if the platform is embedded.
     */
    public static boolean isEmbedded() {
        return IS_EMBEDDED;
    }

    /**
     * Returns true if the platform is SWT.
     */
    public static boolean isSWT() {
        return IS_SWT;
    }

    /**
     * Returns true if accessibility is enabled for the current platform.
     */
    public static boolean isAccessibilityEnabled() {
        return IS_ACCESSIBILITY_ENABLED;
    }

    private static String getPlatform() {
        String osType = null;
        if (PlatformUtil.isMac()) {
            osType = MAC;
        } else if (PlatformUtil.isWindows()) {
            osType = WINDOWS;
        } else if (PlatformUtil.isLinux()) {
            osType = GTK;
        } else if (PlatformUtil.isIOS()) {
            osType = IOS;
        }

        // Provide for a runtime override, allowing EGL for example
        String userPlatform = System.getProperty("glass.platform");
        if (userPlatform == null) {
            return osType;
        } else {
            return switch (userPlatform) {
                case "macosx" -> MAC;
                case "windows" -> WINDOWS;
                case "linux", "gtk" -> GTK;
                case "ios" -> IOS;
                case "headless" -> HEADLESS;
                case "monocle" -> MONOCLE;
                default -> userPlatform;
            };
        }
    }
}
