/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

public class PlatformUtil {

    private static final String OS = System.getProperty("os.name");
    private static final boolean EMBEDDED;
    // a property used to denote a non-default impl for this host
    private static String javafxPlatform;

    static {
        javafxPlatform = System.getProperty("javafx.platform");

        loadProperties();

        EMBEDDED = Boolean.getBoolean("com.sun.javafx.isEmbedded");
    }

    private static final boolean ANDROID = "android".equals(javafxPlatform) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean WINDOWS = OS.startsWith("Windows");
    private static final boolean MAC = OS.startsWith("Mac");
    private static final boolean LINUX = OS.startsWith("Linux") && !ANDROID;
    private static final boolean SOLARIS = OS.startsWith("SunOS");
    private static final boolean IOS = OS.startsWith("iOS");
    private static final boolean STATIC_BUILD = "Substrate VM".equals(System.getProperty("java.vm.name"));

    /**
     * Returns true if the operating system is a form of Windows.
     */
    public static boolean isWindows() {
        return WINDOWS;
    }

    /**
     * Returns true if the operating system is a form of macOS.
     */
    public static boolean isMac() {
        return MAC;
    }

    /**
     * Returns true if the operating system is a form of Linux.
     */
    public static boolean isLinux() {
        return LINUX;
    }

    /**
     * Returns true if the operating system is a form of Linux or Solaris
     */
    public static boolean isUnix() {
        return LINUX || SOLARIS;
    }

    /**
     * Returns true if the platform is embedded.
     */
    public static boolean isEmbedded() {
        return EMBEDDED;
    }

    /**
     * Returns true if the operating system is iOS
     */
    public static boolean isIOS() {
        return IOS;
    }

    /**
     * Returns true if the operating system is Android.
     */
    public static boolean isAndroid() {
        return ANDROID;
    }

    /**
     * Returns true if the current runtime is a statically linked image.
     */
    public static boolean isStaticBuild() {
        return STATIC_BUILD;
    }

    private static void loadPropertiesFromFile(final File file) {
        Properties p = new Properties();
        try {
            InputStream in = new FileInputStream(file);
            p.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (javafxPlatform == null) {
            javafxPlatform = p.getProperty("javafx.platform");
        }
        String prefix = javafxPlatform + ".";
        int prefixLength = prefix.length();
        boolean foundPlatform = false;
        for (Object o : p.keySet()) {
            String key = (String) o;
            if (key.startsWith(prefix)) {
                foundPlatform = true;
                String systemKey = key.substring(prefixLength);
                if (System.getProperty(systemKey) == null) {
                    String value = p.getProperty(key);
                    System.setProperty(systemKey, value);
                }
            }
        }
        if (!foundPlatform) {
            System.err.println(
                    "Warning: No settings found for javafx.platform='"
                    + javafxPlatform + "'");
        }
    }

    /**
     * Returns the directory containing the JavaFX runtime, or null
     * if the directory cannot be located
     */
    private static File getRTDir() {
        try {
            String theClassFile = "PlatformUtil.class";
            Class<?> theClass = PlatformUtil.class;
            URL url = theClass.getResource(theClassFile);
            if (url == null) return null;
            String classUrlString = url.toString();
            if (!classUrlString.startsWith("jar:file:")
                    || classUrlString.indexOf('!') == -1) {
                return null;
            }
            // Strip out the "jar:" and everything after and including the "!"
            String s = classUrlString.substring(4,
                    classUrlString.lastIndexOf('!'));
            // Strip everything after the last "/" or "\" to get rid of the jar filename
            int lastIndexOfSlash = Math.max(
                    s.lastIndexOf('/'), s.lastIndexOf('\\'));
            return new File(new URI(s.substring(0, lastIndexOfSlash + 1)).toURL().getPath());
        } catch (MalformedURLException | URISyntaxException e) {
            return null;
        }
    }

    private static void loadProperties() {
        final String vmname = System.getProperty("java.vm.name");
        final String arch = System.getProperty("os.arch");

        if (! (javafxPlatform != null ||
                (arch != null && arch.equals("arm")) ||
                (vmname != null && vmname.indexOf("Embedded") > 0))) {
            return;
        }

        final File rtDir = getRTDir();
        final String propertyFilename = "javafx.platform.properties";
        File rtProperties = new File(rtDir, propertyFilename);
        // First look for javafx.platform.properties in the JavaFX runtime
        // Then in the installation directory of the JRE
        if (rtProperties.exists()) {
            loadPropertiesFromFile(rtProperties);
            return;
        }
        String javaHome = System.getProperty("java.home");
        File javaHomeProperties = new File(javaHome,
                                           "lib" + File.separator
                                           + propertyFilename);
        if (javaHomeProperties.exists()) {
            loadPropertiesFromFile(javaHomeProperties);
            return;
        }

        String javafxRuntimePath = System.getProperty("javafx.runtime.path");
        File javafxRuntimePathProperties = new File(javafxRuntimePath,
                                                 File.separator + propertyFilename);
        if (javafxRuntimePathProperties.exists()) {
           loadPropertiesFromFile(javafxRuntimePathProperties);
        }
    }
}
