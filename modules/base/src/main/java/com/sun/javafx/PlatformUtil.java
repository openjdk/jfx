/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class PlatformUtil {

    private static final String os = System.getProperty("os.name");
    private static final String version = System.getProperty("os.version");
    private static final boolean embedded;
    private static final boolean embedded3DEnabled;
    private static final String embeddedType;
    private static final boolean useEGL;
    private static final boolean doEGLCompositing;
    // a property used to denote a non-default impl for this host
    private static String javafxPlatform;

    static {
        javafxPlatform = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override public String run() {
                return System.getProperty("javafx.platform");
            }
        });
        loadProperties();
        embedded = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override public Boolean run() {
                return Boolean.getBoolean("com.sun.javafx.isEmbedded");
            }
        });
        embedded3DEnabled = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override public Boolean run() {
                return Boolean.getBoolean("com.sun.javafx.experimental.embedded.3d");
            }
        });
        embeddedType = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override public String run() {
                return System.getProperty("embedded");
            }
        });
        useEGL = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override public Boolean run() {
                return Boolean.getBoolean("use.egl");
            }
        });
        if (useEGL) {
            doEGLCompositing = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override public Boolean run() {
                    return Boolean.getBoolean("doNativeComposite");
                }
            });
        } else
            doEGLCompositing = false;
    }

    private static final boolean ANDROID = "android".equals(System.getProperty("javafx.platform")) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean WINDOWS = os.startsWith("Windows");
    private static final boolean WINDOWS_VISTA_OR_LATER = WINDOWS && versionNumberGreaterThanOrEqualTo(6.0f);
    private static final boolean WINDOWS_7_OR_LATER = WINDOWS && versionNumberGreaterThanOrEqualTo(6.1f);
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux") && !ANDROID;
    private static final boolean SOLARIS = os.startsWith("SunOS");
    private static final boolean IOS = os.startsWith("iOS");    

    /**
     * Utility method used to determine whether the version number as
     * reported by system properties is greater than or equal to a given
     * value.
     *
     * @param value The value to test against.
     * @return false if the version number cannot be parsed as a float,
     *         otherwise the comparison against value.
     */
    private static boolean versionNumberGreaterThanOrEqualTo(float value) {
        try {
            return Float.parseFloat(version) >= value;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the operating system is a form of Windows.
     */
    public static boolean isWindows(){
        return WINDOWS;
    }
    
    /**
     * Returns true if the operating system is at least Windows Vista(v6.0).
     */
    public static boolean isWinVistaOrLater(){
        return WINDOWS_VISTA_OR_LATER;
    }
    
    /**
     * Returns true if the operating system is at least Windows 7(v6.1).
     */
    public static boolean isWin7OrLater(){
        return WINDOWS_7_OR_LATER;
    }

    /**
     * Returns true if the operating system is a form of Mac OS.
     */
    public static boolean isMac(){
        return MAC;
    }

    /**
     * Returns true if the operating system is a form of Linux.
     */
    public static boolean isLinux(){
        return LINUX;
    }

    public static boolean useEGL() {
        return useEGL;
    }

    public static boolean useEGLWindowComposition() {
        return doEGLCompositing;
    }

    public static boolean useGLES2() {
        String useGles2 = "false";
        useGles2 =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty("use.gles2");
                    }
                });
        if ("true".equals(useGles2))
            return true;
        else
            return false;
    }

    /**
     * Returns true if the operating system is a form of Unix, including Linux.
     */
    public static boolean isSolaris(){
        return SOLARIS;
    }

    /**
     * Returns true if the operating system is a form of Linux or Solaris
     */
    public static boolean isUnix(){
        return LINUX || SOLARIS;
    }

    /**
     * Returns true if the platform is embedded.
     */
    public static boolean isEmbedded() {
        return embedded;
    }

    /**
     * Returns true if the embedded platform is 3D enabled.
     */
    public static boolean isEmbedded3DEnabled() {
        return embedded3DEnabled;
    }

    /**
     * Returns a string with the embedded type - ie eglx11, eglfb, dfb or null.
     */
    public static String getEmbeddedType() {
        return embeddedType;
    }
    
    /**
     * Returns true if the operating system is iOS
     */
    public static boolean isIOS(){
        return IOS;
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

    /** Returns the directory containing the JavaFX runtime, or null
     * if the directory cannot be located
     */
    private static File getRTDir() {
        try {
            String theClassFile = "PlatformUtil.class";
            Class theClass = PlatformUtil.class;
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
            return new File(new URL(s.substring(0, lastIndexOfSlash + 1)).getPath())
                    .getParentFile();
        } catch (MalformedURLException e) {
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
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                final File rtDir = getRTDir();
                final String propertyFilename = "javafx.platform.properties";
                File rtProperties = new File(rtDir, propertyFilename);
                // First look for javafx.platform.properties in the JavaFX runtime
                // Then in the installation directory of the JRE
                if (rtProperties.exists()) {
                    loadPropertiesFromFile(rtProperties);
                    return null;
                } 
                String javaHome = System.getProperty("java.home");
                File javaHomeProperties = new File(javaHome,
                                                   "lib" + File.separator
                                                   + propertyFilename);
                if (javaHomeProperties.exists()) {
                    loadPropertiesFromFile(javaHomeProperties);
                    return null;
                } 
                
                String javafxRuntimePath = System.getProperty("javafx.runtime.path");
                File javafxRuntimePathProperties = new File(javafxRuntimePath,
                                                         File.separator + propertyFilename);
                if (javafxRuntimePathProperties.exists()) {
                   loadPropertiesFromFile(javafxRuntimePathProperties);
                   return null;
                }
                return null;
            }
        });
    }
    
    public static boolean isAndroid() {
       return ANDROID;
    }
}
