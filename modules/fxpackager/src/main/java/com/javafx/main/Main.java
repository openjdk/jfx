/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * This class loads com.sun.javafx.application.LauncherImpl and calls the
 * launchApplication method.
 *
 * It is used by packager to include it as part of the application jar file
 * so that we can run and locate the JavaFX runtime. Note that we cannot
 * assume that JavaFX is on the classpath so we must use reflection and not
 * have any direct reference to any JavaFX runtime class.
 *
 * We will do the following:
 *
 * 1. Verify the version of Java and produce error message if not JDK6+
 *
 * 2. Locate the jar file from which the Main class was launched. Read the
 *    jar manifest and extract
 *    the application class using the JavaFX-Application-Class manifest entry.
 *    Alternatively, we will read the application class from a system property.
 *
 * 3. Try to locate the JavaFX runtime by loading the
 * com.sun.javafx.application.LauncherImpl class using the following mechanisms
 * in order:
 *
 *     A. Try loading it directly in case it is on the classpath.
 *     B. If the javafx.runtime.path System Property is set, try
 *        loading it from ${javafx.runtime.path}/lib/ext/jfxrt.jar
 *        (or lib/jfxrt.jar)
 *     C. Look for a cobundled JavaFX in the current jre
 *     D. If on Windows, read the registry key associated with the JavaFX
 *        runtime (if running in a 64-bit JVM, use the 64-bit path)
 *
 * 4. Create a custom URLClassLoader from the appropriate jar files, and then
 *    call the launchApplication method. If the application class is not a
 *    subclass of javafx.application.Application then we will call the main
 *    method in the application class instead.
 *
 * 5. If the LauncherImpl class cannot be found, then show a Swing dialog
 *    (again, using reflection).
 */

public class Main {
    private static boolean verbose = false;
    private static final String fxApplicationClassName = "javafx.application.Application";
    private static final String fxLaunchClassName = "com.sun.javafx.application.LauncherImpl";
    private static final String manifestAppClass = "JavaFX-Application-Class";
    private static final String manifestPreloaderClass = "JavaFX-Preloader-Class";
    private static final String manifestFallbackClass = "JavaFX-Fallback-Class";
    private static final String manifestClassPath = "JavaFX-Class-Path";

    //Manifest entry to explicitly disable autoproxy config
    //  Unless it has "Auto" value it will disable proxy
    private static final String manifestAutoProxy = "JavaFX-Feature-Proxy";

    //Experimental hook to simplify adding "au" logic to native bundles
    private static final String manifestUpdateHook = "X-JavaFX-Update-Hook";

    // JavaFX family version that this Launcher is compatible with
    private static final String JAVAFX_FAMILY_VERSION = "2.";

    // Minimum JavaFX version required to run the app
    // (keep separate from JAVAFX_FAMILY_VERSION check as
    //   we want 2.2.1 SDK to be ok to run app that needs 2.1.0
    //   and prefix based match is not enough)
    // NOTE: This should be refactored so that the version gets supplied
    //   from the build environment, but we do NOT want another class or
    //   property file in the app bundle! Are there any other options
    //   besides java source code preprocessing?)
    private static final String JAVAFX_REQUIRED_VERSION = "2.1.0";

    private static final String ZERO_VERSION = "0.0.0";

    //application jar attributes
    private static Attributes attrs = null;

    private static URL fileToURL(File file) throws IOException {
        return file.getCanonicalFile().toURI().toURL();
    }

    private static Method findLaunchMethod(File jfxRtPath, String fxClassPath) {
        final Class[] argTypes =
                new Class[] { Class.class, Class.class, (new String[0]).getClass() };

        try {
            ArrayList urlList = new ArrayList();

            // Add in the elements of the classpath
            String cp = System.getProperty("java.class.path");
            if (cp != null) {
                while (cp.length() > 0) {
                    int pathSepIdx = cp.indexOf(File.pathSeparatorChar);
                    if (pathSepIdx < 0) {
                        String pathElem = cp;
                        urlList.add(fileToURL(new File(pathElem)));
                        break;
                    } else if (pathSepIdx > 0) {
                        String pathElem = cp.substring(0, pathSepIdx);
                        urlList.add(fileToURL(new File(pathElem)));
                    }
                    cp = cp.substring(pathSepIdx + 1);
                }
            }

            // Add in the jars from the JavaFX-Class-Path entry
            cp = fxClassPath;
            if (cp != null) {
                //these are relative paths. if app is not in the current dir
                // we may resolve them incorrectly ...
                // try to find main jar and build absolute paths
                File baseDir = null;
                try {
                    String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                    //path will have encoded spaces, etc. => need to restore
                    String decodedPath = URLDecoder.decode(path, "UTF-8");
                    baseDir = new File(decodedPath).getParentFile();
                    if (!baseDir.exists()) {
                        baseDir = null;
                    }
                } catch (Exception e) {}
                while (cp.length() > 0) {
                    int pathSepIdx = cp.indexOf(" ");
                    if (pathSepIdx < 0) {
                        String pathElem = cp;
                        File f = (baseDir == null) ?
                                new File(pathElem) : new File(baseDir, pathElem);
                        urlList.add(fileToURL(f));
                        break;
                    } else if (pathSepIdx > 0) {
                        String pathElem = cp.substring(0, pathSepIdx);
                        File f = (baseDir == null) ?
                                new File(pathElem) : new File(baseDir, pathElem);
                        urlList.add(fileToURL(f));
                    }
                    cp = cp.substring(pathSepIdx + 1);
                }
            }

            // Add JavaFX runtime jar and deployment jars
            if (jfxRtPath != null) {
                File jfxRtLibPath = new File(jfxRtPath, "lib");
                File jfxRtLibExtPath = new File(jfxRtLibPath, "ext");
                File jfxRtJar = new File(jfxRtLibExtPath, "jfxrt.jar");
                if (!jfxRtJar.canRead()) {
                    // Legacy support for old file location
                    jfxRtJar = new File(jfxRtLibPath, "jfxrt.jar");
                }
                urlList.add(fileToURL(jfxRtJar));
                File deployJar = new File(jfxRtLibPath, "deploy.jar");
                //in the dev environment deploy.jars will not be part of
                // built SDK unless it is windows
                //However, hopefully java is used from relatively new java
                // and we can add deploy jars from there?
                //If no deploy jars are found we will treat it as runtime error
                if (!deployJar.exists()) {
                    deployJar = getDeployJarFromJRE();
                }
                if (deployJar != null) {
                    URL deployJarURL = fileToURL(deployJar);
                    urlList.add(deployJarURL);
                    urlList.add(new URL(deployJarURL, "plugin.jar"));
                    urlList.add(new URL(deployJarURL, "javaws.jar"));
                } else {
                    if (verbose) {
                        System.err.println("Skip JavaFX Runtime at "
                                + jfxRtPath + " as no deploy jars found.");
                    }
                    return null;
                }
            }

            URL[] urls = (URL[])urlList.toArray(new URL[0]);
            if (verbose) {
                System.err.println("===== URL list");
                for (int i = 0; i < urls.length; i++) {
                    System.err.println("" + urls[i]);
                }
                System.err.println("=====");
            }

            ClassLoader urlClassLoader = new URLClassLoader(urls, null);
            Class launchClass = Class.forName(fxLaunchClassName, true,
                    urlClassLoader);
            Method m = launchClass.getMethod("launchApplication", argTypes);
            if (m != null) {
                Thread.currentThread().setContextClassLoader(urlClassLoader);
                return m;
            }
        } catch (Exception ex) {
            if (jfxRtPath != null) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    private static Method findLaunchMethodInClasspath(String fxClassPath) {
        return findLaunchMethod(null, fxClassPath);
    }

    private static Method findLaunchMethodInJar(String jfxRtPathName, String fxClassPath) {
        File jfxRtPath = new File(jfxRtPathName);

        // Verify that we can read <jfxRtPathName>/lib/ext/jfxrt.jar
        File jfxRtLibPath = new File(jfxRtPath, "lib");
        File jfxRtLibExtPath = new File(jfxRtLibPath, "ext");
        File jfxRtJar = new File(jfxRtLibExtPath, "jfxrt.jar");
        if (!jfxRtJar.canRead()) {
            File jfxRtJar2 = new File(jfxRtLibPath, "jfxrt.jar");
            if (!jfxRtJar2.canRead()) {
                if (verbose) {
                    System.err.println("Unable to read " + jfxRtJar.toString()
                            + " or " + jfxRtJar2.toString());
                }
                return null;
            }
        }

        return findLaunchMethod(jfxRtPath, fxClassPath);
    }

    // convert version string in the form of x.y.z into int array of (x,y.z)
    // return the array if version string can be converted.
    // otherwise retun null
    private static int[] convertVersionStringtoArray(String version) {
        int[] v = new int[3];
        if (version == null) {
            return null;
        }

        String s[] = version.split("\\.");
        if (s.length == 3) {
            v[0] = Integer.parseInt(s[0]);
            v[1] = Integer.parseInt(s[1]);
            v[2] = Integer.parseInt(s[2]);
            return v;
        }
        // version string passed in is bad
        return null;
    }

    // compare the two version array a1 and a2
    // return 0 if the two array contains the same version information
    // (or both are invalid version specs)
    // return 1 if a2 is greater than a1
    // return -1 if a2 is less than a1
    private static int compareVersionArray(int[] a1, int[] a2) {
        boolean isValid1 = (a1 != null) && (a1.length == 3);
        boolean isValid2 = (a2 != null) && (a2.length == 3);

        // both bad
        if (!isValid1 && !isValid2) {
            return 0;
        }

        // a2 < a1
        if (!isValid2) {
            return -1;
        }

        // a2 > a1
        if (!isValid1) {
            return 1;
        }

        for (int i = 0; i < a1.length; i++) {
            if (a2[i] > a1[i]) {
                return 1;
            }
            if (a2[i] < a1[i]) {
                return -1;
            }
        }

        return 0;
    }

    private static File getDeployJarFromJRE() {
        final String javaHome = System.getProperty("java.home");
        if (verbose) {
            System.err.println("java.home = " + javaHome);
        }
        if (javaHome == null || javaHome.equals("")) {
            return null;
        }

        File jreLibPath = new File(javaHome, "lib");
        File deployJar = new File(jreLibPath, "deploy.jar");

        if (deployJar.exists()) {
            return deployJar;
        }
        return null;
    }

    /**
     * If we are on Windows, look in the system registry for the
     * installed JavaFX runtime.
     *
     * @return the path to the JavaFX Runtime or null
     */
    private static String lookupRegistry() {
        if (!System.getProperty("os.name").startsWith("Win")) {
            return null;
        }

        try {
            // Load deploy.jar, get a Config instance and load the native
            // libraries; then load the windows registry class and lookup
            // the method to get the windows registry entry

            File deployJar = getDeployJarFromJRE();
            if (deployJar == null) {
                return null;
            }

            URL[] urls = new URL[]{fileToURL(deployJar)};
            if (verbose) {
                System.err.println(">>>> URL to deploy.jar = " + urls[0]);
            }

            ClassLoader deployClassLoader = new URLClassLoader(urls, null);

            try {
                // Load and initialize the native deploy library, ignore exception
                String configClassName = "com.sun.deploy.config.Config";
                Class configClass = Class.forName(configClassName, true,
                        deployClassLoader);
                Method m = configClass.getMethod("getInstance", null);
                Object config = m.invoke(null, null);
                m = configClass.getMethod("loadDeployNativeLib", null);
                m.invoke(config, null);
            } catch (Exception ex) {
                // Ignore any exception, since JDK7 no longer has this method
            }

            String winRegistryWrapperClassName =
                    "com.sun.deploy.association.utility.WinRegistryWrapper";

            Class winRegistryWrapperClass = Class.forName(
                    winRegistryWrapperClassName, true, deployClassLoader);

            Method mGetSubKeys = winRegistryWrapperClass.getMethod(
                    "WinRegGetSubKeys", new Class[]{
                        Integer.TYPE,
                        String.class,
                        Integer.TYPE
                    });

            Field HKEY_LOCAL_MACHINE_Field2 =
                    winRegistryWrapperClass.getField("HKEY_LOCAL_MACHINE");
            final int HKEY_LOCAL_MACHINE2 = HKEY_LOCAL_MACHINE_Field2.getInt(null);
            final String registryKey = "Software\\Oracle\\JavaFX\\";

            // Read the registry and find all installed JavaFX runtime versions
            // under HKLM\Software\Oracle\JavaFX\
            String[] fxVersions = (String[]) mGetSubKeys.invoke(null, new Object[]{
                        new Integer(HKEY_LOCAL_MACHINE2),
                        registryKey,
                        new Integer(255)
                    });

            if (fxVersions == null) {
                // No JavaFX runtime installed in the system
                return null;
            }
            String version = ZERO_VERSION;
            // Iterate thru all installed JavaFX runtime verions in the system
            for (int i = 0; i < fxVersions.length; i++) {
                // get the latest version that is compatibible with the
                // launcher JavaFX family version and meets minimum version requirement
                if (fxVersions[i].startsWith(JAVAFX_FAMILY_VERSION)
                        && fxVersions[i].compareTo(JAVAFX_REQUIRED_VERSION) >= 0) {
                    int[] v1Array = convertVersionStringtoArray(version);
                    int[] v2Array = convertVersionStringtoArray(fxVersions[i]);
                    if (compareVersionArray(v1Array, v2Array) > 0) {
                        version = fxVersions[i];
                    }
                } else {
                    if (verbose) {
                        System.err.println("  Skip version " + fxVersions[i]
                                + " (required=" + JAVAFX_REQUIRED_VERSION + ")");
                    }
                }
            }

            if (version.equals(ZERO_VERSION)) {
                // No installed JavaFX runtime compatible with this Launcher
                return null;
            }

            // Read the registry entry for: Software\Oracle\JavaFX\<version>
            String winRegistryClassName = "com.sun.deploy.util.WinRegistry";
            Class winRegistryClass = Class.forName(winRegistryClassName, true,
                    deployClassLoader);
            Method mGet = winRegistryClass.getMethod("getString", new Class[]{
                        Integer.TYPE,
                        String.class,
                        String.class
                    });
            Field HKEY_LOCAL_MACHINE_Field = winRegistryClass.getField("HKEY_LOCAL_MACHINE");
            final int HKEY_LOCAL_MACHINE = HKEY_LOCAL_MACHINE_Field.getInt(null);
            String path = (String) mGet.invoke(null, new Object[]{
                        new Integer(HKEY_LOCAL_MACHINE),
                        registryKey + version,
                        "Path"
                    });
            if (verbose) {
                System.err.println("FOUND KEY: " + registryKey + version + " = " + path);
            }
            return path;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private static Attributes getJarAttributes() throws Exception {
        String theClassFile = "Main.class";
        Class theClass = Main.class;
        String classUrlString = theClass.getResource(theClassFile).toString();
        if (!classUrlString.startsWith("jar:file:") || classUrlString.indexOf("!") == -1){
            return null;
        }
        // Strip out the "jar:" and everything after and including the "!"
        String urlString = classUrlString.substring(4, classUrlString.lastIndexOf("!"));
        File jarFile = new File(new URI(urlString).getPath());
        String jarName = jarFile.getCanonicalPath();

        Attributes attr;
        JarFile jf = null;
        try {
            jf = new JarFile(jarName);
            Manifest mf = jf.getManifest();
            attr = mf.getMainAttributes();
        } finally {
            if (jf != null) {
                try {
                    jf.close();
                } catch (Exception ex) {
                    /* swallow the exception */
                }
            }
        }
        return attr;
    }

    private static String decodeBase64(String inp) throws IOException {
        return new String(Base64.getDecoder().decode(inp));
    }

    private static String[] getAppArguments(Attributes attrs) {
        List args = new LinkedList();

        try {
            int idx = 1;
            String argNamePrefix = "JavaFX-Argument-";
            while (attrs.getValue(argNamePrefix + idx) != null) {
                args.add(decodeBase64(attrs.getValue(argNamePrefix + idx)));
                idx++;
            }

            String paramNamePrefix = "JavaFX-Parameter-Name-";
            String paramValuePrefix = "JavaFX-Parameter-Value-";
            idx = 1;
            while (attrs.getValue(paramNamePrefix + idx) != null) {
                String k = decodeBase64(attrs.getValue(paramNamePrefix + idx));
                String v = null;
                if (attrs.getValue(paramValuePrefix + idx) != null) {
                    v = decodeBase64(attrs.getValue(paramValuePrefix + idx));
                }
                args.add("--" + k + "=" + (v != null ? v : ""));
                idx++;
            }
        } catch (IOException ioe) {
            System.err.println("Failed to extract application parameters");
            ioe.printStackTrace();
        }


        return (String[]) args.toArray(new String[0]);
    }

    // Return the application class name, either from the property or from the
    // jar file
    private static String getAppName(Attributes attrs, boolean preloader) {
        String propName = preloader
                ? "javafx.preloader.class"
                : "javafx.application.class";

        String className = System.getProperty(propName);
        if (className != null && className.length() != 0) {
            return className;
        }

        String appName;

        //this only true in the dev environment if run out of jar
        if (attrs == null) {
            return "TEST";
        }

        if (preloader) {
            appName = (String)attrs.getValue(manifestPreloaderClass);
            if (appName == null || appName.length() == 0) {
                if (verbose) {
                    System.err.println("Unable to find preloader class name");
                }
                return null;
            }
            return appName;
        } else {
            appName = (String)attrs.getValue(manifestAppClass);
            if (appName == null || appName.length() == 0) {
                System.err.println("Unable to find application class name");
                return null;
            }
            return appName;
        }
    }

    private static Class getAppClass(String appName) {
        try {
            // load the user's JavaFX class but do *not* initialize!
            if (verbose) {
                System.err.println("Try calling Class.forName(" + appName
                        + ") using classLoader = "
                        + Thread.currentThread().getContextClassLoader());
            }
            Class appClass = Class.forName(appName, false,
                    Thread.currentThread().getContextClassLoader());
            if (verbose) {
                System.err.println("found class: " + appClass);
            }
            return appClass;
        } catch (NoClassDefFoundError ncdfe) {
            ncdfe.printStackTrace();
            errorExit("Unable to find class: " + appName);
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            errorExit("Unable to find class: " + appName);
        }

        return null;
    }

    //try to install webstart proxy to avoid asking user for proxy info
    private static void tryToSetProxy() {
        try {
            if (attrs != null) {
                String proxySetting = attrs.getValue(manifestAutoProxy);
                if (proxySetting != null && !"auto".equals(proxySetting.toLowerCase())) {
                   if (verbose) {
                       System.out.println("Auto proxy detection is disabled in manifest.");
                   }
                   return;
                }
            }

            //if explicit proxy settings are proxided we will skip autoproxy
            //Note: we only check few most popular settings.
            if (System.getProperty("http.proxyHost") != null
                 || System.getProperty("https.proxyHost") != null
                 || System.getProperty("ftp.proxyHost") != null
                 || System.getProperty("socksProxyHost") != null) {
               if (verbose) {
                   System.out.println("Explicit proxy settings detected. Skip autoconfig.");
                   System.out.println("  http.proxyHost=" + System.getProperty("http.proxyHost"));
                   System.out.println("  https.proxyHost=" + System.getProperty("https.proxyHost"));
                   System.out.println("  ftp.proxyHost=" + System.getProperty("ftp.proxyHost"));
                   System.out.println("  socksProxyHost=" + System.getProperty("socksProxyHost"));
               }
               return;
            }
            if (System.getProperty("javafx.autoproxy.disable") != null) {
                if (verbose) {
                    System.out.println("Disable autoproxy on request.");
                }
                return;
            }

            Class sm = Class.forName("com.sun.deploy.services.ServiceManager",
                    true,
                    Thread.currentThread().getContextClassLoader());
            Class params[] = {Integer.TYPE};
            Method setservice = sm.getDeclaredMethod("setService", params);
            String osname = System.getProperty("os.name");

            String servicename = null;
            if (osname.startsWith("Win")) {
                servicename = "STANDALONE_TIGER_WIN32";

            } else if (osname.contains("Mac")) {
                servicename = "STANDALONE_TIGER_MACOSX";
            } else {
                servicename = "STANDALONE_TIGER_UNIX";
            }
            Object values[] = new Object[1];
            Class pt = Class.forName("com.sun.deploy.services.PlatformType",
                    true,
                    Thread.currentThread().getContextClassLoader());
            values[0] = pt.getField(servicename).get(null);
            setservice.invoke(null, values);

            Class dps = Class.forName(
                    "com.sun.deploy.net.proxy.DeployProxySelector",
                    true,
                    Thread.currentThread().getContextClassLoader());
            Method m = dps.getDeclaredMethod("reset", new Class[0]);
            m.invoke(null, new Object[0]);
            if (verbose) {
                System.out.println("Autoconfig of proxy is completed.");
            }
        } catch (Exception e) {
            if (verbose) {
                System.out.println("Failed to autoconfig proxy due to "+e);
            }
        }
    }

    private static void processUpdateHook(String updateHookName) {
        if (updateHookName == null) {
            return;
        }

        try {
            // load UpdateHook class
            if (verbose) {
                System.err.println("Try calling Class.forName(" + updateHookName
                        + ") using classLoader = "
                        + Thread.currentThread().getContextClassLoader());
            }
            Class hookClass = Class.forName(updateHookName, false,
                    Thread.currentThread().getContextClassLoader());
            if (verbose) {
                System.err.println("found class: " + hookClass.getCanonicalName());
            }

            Method mainMethod = hookClass.getMethod("main",
                        new Class[] { (new String[0]).getClass() });
            String args[] = null;
            mainMethod.invoke(null, new Object[] {args});

        } catch (Exception ex) {
            if (verbose) {
                System.err.println("Failed to run update hook: "+ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private static void launchApp(Method launchMethod,
            String appName,
            String preloaderName,
            String updateHookName,
            String[] args) {

        Class preloaderClass = null;
        if (preloaderName != null) {
            preloaderClass = getAppClass(preloaderName);
        }
        Class appClass = getAppClass(appName);
        Class fxApplicationClass = null;
        try {
            fxApplicationClass = Class.forName(fxApplicationClassName,
                true, Thread.currentThread().getContextClassLoader());
        } catch (NoClassDefFoundError ex) {
            errorExit("Cannot find " + fxApplicationClassName);
        } catch (ClassNotFoundException ex) {
            errorExit("Cannot find " + fxApplicationClassName);
        }

        if (fxApplicationClass.isAssignableFrom(appClass)) {
            try {
                if (verbose) {
                    System.err.println("launchApp: Try calling "
                            + launchMethod.getDeclaringClass().getName() + "."
                            + launchMethod.getName());
                }
                tryToSetProxy();
                processUpdateHook(updateHookName);
                launchMethod.invoke(null, new Object[] { appClass, preloaderClass, args });
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
                errorExit("Exception while running Application");
            } catch (Exception ex) {
                ex.printStackTrace();
                errorExit("Unable to invoke launch method");
            }
        } else {
            try {
                if (verbose) {
                    System.err.println("Try calling " + appClass.getName()
                            + ".main(String[])");
                }
                Method mainMethod = appClass.getMethod("main",
                        new Class[] { (new String[0]).getClass() });
                mainMethod.invoke(null, new Object[] { args });
            } catch (Exception ex) {
                ex.printStackTrace();
                errorExit("Unable to invoke main method");
            }
        }
    }

    // Check the JRE version. Exit with error if < 1.6
    private static boolean checkJre() {
        if (verbose) {
            System.err.println("java.version = "
                    + System.getProperty("java.version"));
            System.err.println("java.runtime.version = "
                    + System.getProperty("java.runtime.version"));
        }

        // Check for minimum JRE version
        if (isOldJRE()) {
            showFallback(true);
            return false;
        }
        return true;
    }

    private static Method findLaunchMethod(String fxClassPath) {
        Method launchMethod;

        // Try to find JavaFX LauncherImpl class on classpath
        if (verbose) {
            System.err.println("1) Try existing classpath...");
        }
        launchMethod = findLaunchMethodInClasspath(fxClassPath);
        if (launchMethod != null) {
            return launchMethod;
        }

        // Check for javafx.runtime.path variable; if set, look for the
        // JavaFX LauncherImpl class there.
        if (verbose) {
            System.err.println("2) Try javafx.runtime.path property...");
        }
        String javafxRuntimePath = System.getProperty("javafx.runtime.path");
        if (javafxRuntimePath != null) {
            if (verbose) {
                System.err.println("    javafx.runtime.path = " + javafxRuntimePath);
            }
            launchMethod = findLaunchMethodInJar(javafxRuntimePath, fxClassPath);
        }
        if (launchMethod != null) {
            return launchMethod;
        }

        if (verbose) {
            System.err.println("3) Look for cobundled JavaFX ... " +
                    "[java.home="+System.getProperty("java.home"));
        }
        launchMethod = findLaunchMethodInJar(
                System.getProperty("java.home"), fxClassPath);
        if (launchMethod != null) {
            return launchMethod;
        }

        // Check the platform registry for this architecture.
        if (verbose) {
            System.err.println("4) Look in the OS platform registry...");
        }
        javafxRuntimePath = lookupRegistry();
        if (javafxRuntimePath != null) {
            if (verbose) {
                System.err.println("    Installed JavaFX runtime found in: "
                        + javafxRuntimePath);
            }
            launchMethod = findLaunchMethodInJar(javafxRuntimePath, fxClassPath);
            if (launchMethod != null) {
                return launchMethod;
            }
        }

        return launchMethod;
    }

    public static void main(String [] args) {
        // Set verbose flag
        verbose = Boolean.getBoolean("javafx.verbose");

        // First check the minimum JRE
        if (!checkJre()) {
            return;
        }

        // Load the main jar manifest attributes
        try {
            attrs = getJarAttributes();
        } catch (Exception ex) {
            ex.printStackTrace();
            errorExit("Unable to load jar manifest");
        }

        // Next get the application name
        String appName = getAppName(attrs, false);
        if (verbose) {
            System.err.println("appName = " + appName);
        }
        if (appName == null) {
            errorExit("Unable to find application class name");
        }

        // Next get the preloader name
        String preloaderName = getAppName(attrs, true);
        if (verbose) {
            System.err.println("preloaderName = " + preloaderName);
        }

        String embeddedArgs[] = getAppArguments(attrs);
        if (verbose) {
            System.err.println("embeddedArgs = " + Arrays.toString(embeddedArgs));
            System.err.println("commandLineArgs = " + Arrays.toString(args));
        }

        String updateHook = (String) attrs.getValue(manifestUpdateHook);
        if (verbose && updateHook != null) {
             System.err.println("updateHook = " + updateHook);
        }

        // Get JavaFX-Class-Path entry
        String fxClassPath;
        if (attrs != null) {
           fxClassPath = (String)attrs.getValue(manifestClassPath);
        } else {
           fxClassPath = "";
        }

        Method launchMethod = findLaunchMethod(fxClassPath);
        if (launchMethod != null) {
            launchApp(launchMethod, appName, preloaderName, updateHook,
                    args.length > 0 ? args: embeddedArgs);
            return;
        }

        showFallback(false);
    }

    private static void showFallback(final boolean jreError) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("JavaFX Launcher");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JApplet japp = null;

                //try to use custom fallback if available
                if (attrs != null && attrs.getValue(manifestFallbackClass) != null) {
                    Class customFallback = null;
                    try {
                        customFallback = Class.forName(
                                attrs.getValue(manifestFallbackClass), false,
                                Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException ce) {
                        System.err.println("Custom fallback class is not found: "
                                + attrs.getValue(manifestFallbackClass));
                    }

                    //If custom fallback attribute actually points to the
                    // default JavaFX fallback we want to use other way to launch it
                    if (customFallback != null
                            && !NoJavaFXFallback.class.getName().equals(
                                   customFallback.getName())) {
                        try {
                            japp = (JApplet) customFallback.newInstance();
                        } catch (Exception e) {
                            System.err.println("Failed to instantiate custom fallback "
                                    + customFallback.getName() + " due to " + e);
                        }
                    }
                }

                //custom fallback missing or we fail to init it
                if (japp == null) {
                    //custom fallback will need to figure reason of error
                    //on its own. Generic fallback gets extra input.
                    japp = new NoJavaFXFallback(
                            jreError, !jreError, JAVAFX_REQUIRED_VERSION);
                    f.getContentPane().add(japp); //could be old JRE! use content pane
                    f.pack();
                    f.setVisible(true);
                } else {
                    japp.init();
                    f.getContentPane().add(japp); //could be old JRE! use content pane
                    japp.start();
                    f.pack();
                    f.setVisible(true);
                }

            }
        });
    }

    private static void errorExit(final String string) {
        try {
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        Class componentClass = Class.forName("java.awt.Component");
                        Class jOptionPaneClass = Class.forName("javax.swing.JOptionPane");
                        Field ERROR_MESSAGE_Field = jOptionPaneClass.getField("ERROR_MESSAGE");
                        final int ERROR_MESSAGE = ERROR_MESSAGE_Field.getInt(null);
                        Method showMessageDialogMethod = jOptionPaneClass.getMethod(
                                "showMessageDialog",
                                new Class[] { componentClass,
                                              Object.class,
                                              String.class,
                                              Integer.TYPE });
                        showMessageDialogMethod.invoke(null, new Object[] {
                                    null, string, "JavaFX Launcher Error",
                                    new Integer(ERROR_MESSAGE) });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };

            Class swingUtilsClass = Class.forName("javax.swing.SwingUtilities");
            Method invokeAndWaitMethod = swingUtilsClass.getMethod("invokeAndWait",
                    new Class[] { Runnable.class });
            invokeAndWaitMethod.invoke(null, new Object[] { runnable });
            if (verbose) {
                System.err.println("Done with invoke and wait");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.exit(1);
    }

    // Package-scope method used to check minimum JRE version
    static boolean isOldJRE() {
        return getJavaVersionAsFloat() < 160.18f; //< 6u18
}

    static float getJavaVersionAsFloat() {
        String versionString = System.getProperty("java.version", "1.5.0");

        StringBuffer sb = new StringBuffer();

        int firstDot = versionString.indexOf(".");
        sb.append(versionString.substring(0,firstDot));

        int secondDot = versionString.indexOf(".", firstDot+1);
        sb.append(versionString.substring(firstDot+1, secondDot));

        int underscore = versionString.indexOf("_", secondDot+1);
        if (underscore >= 0) {
            int dash = versionString.indexOf("-", underscore+1);
            if (dash < 0) {
                dash = versionString.length();
            }
            sb.append(versionString.substring(secondDot+1, underscore)).
                append(".").
                append(versionString.substring(underscore+1, dash));
        } else {
            int dash = versionString.indexOf("-", secondDot+1);
            if (dash < 0) {
                dash = versionString.length();
            }
            sb.append(versionString.substring(secondDot+1, dash));
        }

        float version = 150.0f;
        try {
            version = Float.parseFloat(sb.toString());
        } catch (NumberFormatException e) {}

        return version;
    }

}
