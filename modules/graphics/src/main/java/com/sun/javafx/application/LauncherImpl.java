/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.application.Preloader.ErrorNotification;
import javafx.application.Preloader.PreloaderNotification;
import javafx.application.Preloader.StateChangeNotification;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.Base64;
import com.sun.javafx.jmx.MXExtension;
import com.sun.javafx.runtime.SystemProperties;


public class LauncherImpl {
    /**
     * When passed as launchMode to launchApplication, tells the method that
     * launchName is the name of the JavaFX application class to launch.
     */
    public static final String LAUNCH_MODE_CLASS = "LM_CLASS";
    
    /**
     * When passed as launchMode to launchApplication, tells the method that
     * launchName is a path to a JavaFX application jar file to be launched.
     */
    public static final String LAUNCH_MODE_JAR = "LM_JAR";
    
    // set to true to debug launch issues from Java launcher
    private static final boolean trace = false;

    // set system property javafx.verbose to true to make the launcher noisy
    private static boolean verbose = false;
    
    private static final String MF_MAIN_CLASS = "Main-Class";
    private static final String MF_JAVAFX_MAIN = "JavaFX-Application-Class";
    private static final String MF_JAVAFX_PRELOADER = "JavaFX-Preloader-Class";
    private static final String MF_JAVAFX_CLASS_PATH = "JavaFX-Class-Path";
    private static final String MF_JAVAFX_FEATURE_PROXY = "JavaFX-Feature-Proxy";
    private static final String MF_JAVAFX_ARGUMENT_PREFIX = "JavaFX-Argument-";
    private static final String MF_JAVAFX_PARAMETER_NAME_PREFIX = "JavaFX-Parameter-Name-";
    private static final String MF_JAVAFX_PARAMETER_VALUE_PREFIX = "JavaFX-Parameter-Value-";

    // Set to true to simulate a slow download progress
    private static final boolean simulateSlowProgress = false;

    // Ensure that launchApplication method is only called once
    private static AtomicBoolean launchCalled = new AtomicBoolean(false);

    // Flag indicating that the toolkit has been started
    private static final AtomicBoolean toolkitStarted = new AtomicBoolean(false);

    // Exception found during launching
    private static volatile RuntimeException launchException = null;

    // The current preloader, used for notification in the standalone
    // launcher mode
    private static Preloader currentPreloader = null;

    // Saved preloader class from the launchApplicationWithArgs method (called
    // from the Java 8 launcher). It is used in the case where we call main,
    // which is turn calls into launchApplication.
    private static Class<? extends Preloader> savedPreloaderClass = null;

    // The following is used to determine whether the main() method
    // has set the CCL in the case where main is called after the FX toolkit
    // is started.
    private static ClassLoader savedMainCcl = null;

    /**
     * This method is called by the Application.launch method.
     * It must not be called more than once or an exception will be thrown.
     *
     * Note that it is always called on a thread other than the FX application
     * thread, since that thread is only created at startup.
     *
     * @param appClass application class
     * @param args command line arguments
     */
    @SuppressWarnings("unchecked")
    public static void launchApplication(final Class<? extends Application> appClass,
            final String[] args) {
        
        Class<? extends Preloader> preloaderClass = savedPreloaderClass;
        
        if (preloaderClass == null) {
            String preloaderByProperty = AccessController.doPrivileged((PrivilegedAction<String>) () ->
                    System.getProperty("javafx.preloader"));
            if (preloaderByProperty != null) {
                try {
                    preloaderClass = (Class<? extends Preloader>) Class.forName(preloaderByProperty, 
                            false, appClass.getClassLoader());
                } catch (Exception e) {
                    System.err.printf("Could not load preloader class '" + preloaderByProperty + 
                            "', continuing without preloader.");
                    e.printStackTrace();
                }
            }
        }
        
        launchApplication(appClass, preloaderClass, args);
    }

    /**
     * This method is called by the standalone launcher.
     * It must not be called more than once or an exception will be thrown.
     *
     * Note that it is always called on a thread other than the FX application
     * thread, since that thread is only created at startup.
     *
     * @param appClass application class
     * @param preloaderClass preloader class, may be null
     * @param args command line arguments
     */
    public static void launchApplication(final Class<? extends Application> appClass,
            final Class<? extends Preloader> preloaderClass,
            final String[] args) {

        if (launchCalled.getAndSet(true)) {
            throw new IllegalStateException("Application launch must not be called more than once");
        }

        if (! Application.class.isAssignableFrom(appClass)) {
            throw new IllegalArgumentException("Error: " + appClass.getName()
                    + " is not a subclass of javafx.application.Application");
        }

        if (preloaderClass != null && ! Preloader.class.isAssignableFrom(preloaderClass)) {
            throw new IllegalArgumentException("Error: " + preloaderClass.getName()
                    + " is not a subclass of javafx.application.Preloader");
        }

//        System.err.println("launch standalone app: preloader class = "
//                + preloaderClass);

        // Create a new Launcher thread and then wait for that thread to finish
        final CountDownLatch launchLatch = new CountDownLatch(1);
        Thread launcherThread = new Thread(() -> {
            try {
                launchApplication1(appClass, preloaderClass, args);
            } catch (RuntimeException rte) {
                launchException = rte;
            } catch (Exception ex) {
                launchException =
                    new RuntimeException("Application launch exception", ex);
            } catch (Error err) {
                launchException =
                    new RuntimeException("Application launch error", err);
            } finally {
                launchLatch.countDown();
            }
        });
        launcherThread.setName("JavaFX-Launcher");
        launcherThread.start();

        // Wait for FX launcher thread to finish before returning to user
        try {
            launchLatch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Unexpected exception: ", ex);
        }

        if (launchException != null) {
            throw launchException;
        }
    }

    /**
     * This method is called by the Java launcher. This allows us to be launched
     * directly from the command line via "java -jar fxapp.jar" or
     * "java -cp path some.fx.App". The launchMode argument must be one of
     * "LM_CLASS" or "LM_JAR" or execution will abort with an error.
     * 
     * @param launchName Either the path to a jar file or the application class
     * name to launch
     * @param launchMode The method of launching the application, either LM_JAR
     * or LM_CLASS
     * @param args Application arguments from the command line
     */
    public static void launchApplication(final String launchName,
            final String launchMode,
            final String[] args) {

        if (verbose) {
            System.err.println("Java 8 launchApplication method: launchMode="
                    + launchMode);
        }

        /*
         * For now, just open the jar and get JavaFX-Application-Class and
         * JavaFX-Preloader and pass them to launchApplication. In the future
         * we'll need to load requested jar files and set up the proxy
         */
        String mainClassName = null;
        String preloaderClassName = null;
        String[] appArgs = args;
        ClassLoader appLoader = null;
        
        verbose = Boolean.getBoolean("javafx.verbose");
        
        if (launchMode.equals(LAUNCH_MODE_JAR)) {
            Attributes jarAttrs = getJarAttributes(launchName);
            if (jarAttrs == null) {
                abort(null, "Can't get manifest attributes from jar");
            }

            // If we ever need to check JavaFX-Version, do that here...
            
            // Support JavaFX-Class-Path, but warn that it's deprecated if used
            String fxClassPath = jarAttrs.getValue(MF_JAVAFX_CLASS_PATH);
            if (fxClassPath != null) {
                if (fxClassPath.trim().length() == 0) {
                    fxClassPath = null;
                } else {
                    if (verbose) {
                        System.err.println("WARNING: Application jar uses deprecated JavaFX-Class-Path attribute."
                               +" Please use Class-Path instead.");
                    }
                    
                    /*
                     * create a new ClassLoader to pull in the requested jar files
                     * OK if it returns null, that just means we didn't need to load
                     * anything
                     */
                    appLoader = setupJavaFXClassLoader(new File(launchName), fxClassPath);
                }
            }
            
            // Support JavaFX-Feature-Proxy (only supported setting is 'auto', anything else is ignored)
            String proxySetting = jarAttrs.getValue(MF_JAVAFX_FEATURE_PROXY);
            if (proxySetting != null && "auto".equals(proxySetting.toLowerCase())) {
                trySetAutoProxy();
            }
            
            // process arguments and parameters if no args have been passed by the launcher
            if (args.length == 0) {
                appArgs = getAppArguments(jarAttrs);
            }
            
            // grab JavaFX-Application-Class
            mainClassName = jarAttrs.getValue(MF_JAVAFX_MAIN);
            if (mainClassName == null) {
                // fall back on Main-Class if no JAC
                mainClassName = jarAttrs.getValue(MF_MAIN_CLASS);
                if (mainClassName == null) {
                    // Should not happen as the launcher enforces the presence of Main-Class
                    abort(null, "JavaFX jar manifest requires a valid JavaFX-Appliation-Class or Main-Class entry");
                }
            }
            mainClassName = mainClassName.trim();

            // grab JavaFX-Preloader-Class
            preloaderClassName = jarAttrs.getValue(MF_JAVAFX_PRELOADER);
            if (preloaderClassName != null) {
                preloaderClassName = preloaderClassName.trim();
            }
        } else if (launchMode.equals(LAUNCH_MODE_CLASS)) {
            mainClassName = launchName;
            preloaderClassName = System.getProperty("javafx.preloader");
        } else {
            abort(new IllegalArgumentException("The launchMode argument must be one of LM_CLASS or LM_JAR"),
                    "Invalid launch mode: %1$s", launchMode);
        }

        if (mainClassName == null) {
            abort(null, "No main JavaFX class to launch");
        }

        // check if we have to load through a custom classloader
        if (appLoader != null) {
            try {
                // reload this class through the app classloader
                Class<?> launcherClass = appLoader.loadClass(LauncherImpl.class.getName());
                
                // then invoke the second part of this launcher using reflection
                Method lawa = launcherClass.getMethod("launchApplicationWithArgs",
                        new Class[] { String.class, String.class, (new String[0]).getClass()});
                
                // set the thread context class loader before we continue, or it won't load properly
                Thread.currentThread().setContextClassLoader(appLoader);
                lawa.invoke(null, new Object[] {mainClassName, preloaderClassName, appArgs});
            } catch (Exception e) {
                abort(e, "Exception while launching application");
            }
        } else {
            launchApplicationWithArgs(mainClassName, preloaderClassName, appArgs);
        }
    }
    
    // Must be public since we could be called from a different class loader
    public static void launchApplicationWithArgs(final String mainClassName,
            final String preloaderClassName, String[] args) {

        try {
            startToolkit();
        } catch (InterruptedException ex) {
            abort(ex, "Toolkit initialization error", mainClassName);
        }

        Class<? extends Application> appClass;
        Class<? extends Preloader> preClass = null;
        Class<?> tempAppClass = null;

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final AtomicReference<Class<?>> tmpClassRef = new AtomicReference<>();
        final AtomicReference<Class<? extends Preloader>> preClassRef = new AtomicReference<>();
        PlatformImpl.runAndWait(() -> {
            Class<?> clz = null;
            try {
                clz = Class.forName(mainClassName, true, loader);
            } catch (ClassNotFoundException cnfe) {
                abort(cnfe, "Missing JavaFX application class %1$s", mainClassName);
            }
            tmpClassRef.set(clz);

            if (preloaderClassName != null) {
                try {
                    clz = Class.forName(preloaderClassName, true, loader);
                } catch (ClassNotFoundException cnfe) {
                    abort(cnfe, "Missing JavaFX preloader class %1$s", preloaderClassName);
                }

                if (!Preloader.class.isAssignableFrom(clz)) {
                    abort(null, "JavaFX preloader class %1$s does not extend javafx.application.Preloader", clz.getName());
                }
                preClassRef.set(clz.asSubclass(Preloader.class));
            }
        });
        preClass = preClassRef.get();
        tempAppClass = tmpClassRef.get();

        // Save the preloader class in a static field for later use when
        // main calls back into launchApplication.
        savedPreloaderClass = preClass;

        // If there is a public static void main(String[]) method then call it
        // otherwise just hand off to the other launchApplication method

        Exception theEx = null;
        try {
            Method mainMethod = tempAppClass.getMethod("main",
                    new Class[] { (new String[0]).getClass() });
            if (verbose) {
                System.err.println("Calling main(String[]) method");
            }
            savedMainCcl = Thread.currentThread().getContextClassLoader();
            mainMethod.invoke(null, new Object[] { args });
            return;
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            theEx = ex;
            savedPreloaderClass = null;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            abort(null, "Exception running application %1$s", tempAppClass.getName());
            return;
        }

        // Verify appClass extends Application
        if (!Application.class.isAssignableFrom(tempAppClass)) {
            abort(theEx, "JavaFX application class %1$s does not extend javafx.application.Application", tempAppClass.getName());
        }
        appClass = tempAppClass.asSubclass(Application.class);

        if (verbose) {
            System.err.println("Launching application directly");
        }
        launchApplication(appClass, preClass, args);
    }

    private static URL fileToURL(File file) throws IOException {
        return file.getCanonicalFile().toURI().toURL();
    }

    private static ClassLoader setupJavaFXClassLoader(File appJar, String fxClassPath) {
        try {
            File baseDir = appJar.getParentFile();
            ArrayList jcpList = new ArrayList();

            // Add in the jars from the JavaFX-Class-Path entry
            // TODO: should check current classpath for duplicate entries and ignore them
            String cp = fxClassPath;
            if (cp != null) {
                // these paths are relative to baseDir, which should be the
                // directory containing the app jar file
                while (cp.length() > 0) {
                    int pathSepIdx = cp.indexOf(" ");
                    if (pathSepIdx < 0) {
                        String pathElem = cp;
                        File f = (baseDir == null) ?
                                new File(pathElem) : new File(baseDir, pathElem);
                        if (f.exists()) {
                            jcpList.add(fileToURL(f));
                        } else if (verbose) {
                            System.err.println("Class Path entry \""+pathElem
                                    +"\" does not exist, ignoring");
                        }
                        break;
                    } else if (pathSepIdx > 0) {
                        String pathElem = cp.substring(0, pathSepIdx);
                        File f = (baseDir == null) ?
                                new File(pathElem) : new File(baseDir, pathElem);
                        if (f.exists()) {
                            jcpList.add(fileToURL(f));
                        } else if (verbose) {
                            System.err.println("Class Path entry \""+pathElem
                                    +"\" does not exist, ignoring");
                        }
                    }
                    cp = cp.substring(pathSepIdx + 1);
                }
            }

            // don't bother if there's nothing to add
            if (!jcpList.isEmpty()) {
                ArrayList<URL> urlList = new ArrayList<URL>();
                
                // prepend the existing classpath
                // this will already have the app jar, so no need to worry about it
                cp = System.getProperty("java.class.path");
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

                // we have to add jfxrt.jar to the new class loader, or the app won't load
                URL jfxRtURL = LauncherImpl.class.getProtectionDomain().getCodeSource().getLocation();
                urlList.add(jfxRtURL);
                
                // and finally append the JavaFX-Class-Path entries
                urlList.addAll(jcpList);
                
                URL[] urls = (URL[])urlList.toArray(new URL[0]);
                if (verbose) {
                    System.err.println("===== URL list");
                    for (int i = 0; i < urls.length; i++) {
                        System.err.println("" + urls[i]);
                    }
                    System.err.println("=====");
                }
                return new URLClassLoader(urls, null);
            }
        } catch (Exception ex) {
            if (trace) {
                System.err.println("Exception creating JavaFX class loader: "+ex);
                ex.printStackTrace();
            }
        }
        return null;
    }
    
    private static void trySetAutoProxy() {
        // if explicit proxy settings are proxided we will skip autoproxy
        // Note: we only check few most popular settings.
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
        
        // grab deploy.jar
        // Note that we don't need to keep deploy.jar in the JavaFX classloader
        // it is only needed long enough to configure the proxy
        String javaHome = System.getProperty("java.home");
        File jreLibDir = new File(javaHome, "lib");
        File deployJar = new File(jreLibDir, "deploy.jar");
        
        URL[] deployURLs;
        try {
            deployURLs = new URL[] {
                deployJar.toURI().toURL()
            };
        } catch (MalformedURLException ex) {
            if (trace) {
                System.err.println("Unable to build URL to deploy.jar: "+ex);
                ex.printStackTrace();
            }
            return; // give up setting proxy, usually silently
        }
        
        try {
            URLClassLoader dcl = new URLClassLoader(deployURLs);
            Class sm = Class.forName("com.sun.deploy.services.ServiceManager",
                    true,
                    dcl);
            Class params[] = {Integer.TYPE};
            Method setservice = sm.getDeclaredMethod("setService", params);
            String osname = System.getProperty("os.name");

            String servicename;
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
                    dcl);
            values[0] = pt.getField(servicename).get(null);
            setservice.invoke(null, values);

            Class dps = Class.forName(
                    "com.sun.deploy.net.proxy.DeployProxySelector",
                    true,
                    dcl);
            Method m = dps.getDeclaredMethod("reset", new Class[0]);
            m.invoke(null, new Object[0]);
            
            if (verbose) {
                System.out.println("Autoconfig of proxy is completed.");
            }
        } catch (Exception e) {
            if (verbose) {
                System.err.println("Failed to autoconfig proxy due to "+e);
            }
            if (trace) {
                e.printStackTrace();
            }
        }
    }

    private static String decodeBase64(String inp) throws IOException {
        return new String(Base64.getDecoder().decode(inp));
    }

    private static String[] getAppArguments(Attributes attrs) {
        List args = new LinkedList();

        try {
            int idx = 1;
            String argNamePrefix = MF_JAVAFX_ARGUMENT_PREFIX;
            while (attrs.getValue(argNamePrefix + idx) != null) {
                args.add(decodeBase64(attrs.getValue(argNamePrefix + idx)));
                idx++;
            }

            String paramNamePrefix = MF_JAVAFX_PARAMETER_NAME_PREFIX;
            String paramValuePrefix = MF_JAVAFX_PARAMETER_VALUE_PREFIX;
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
            if (verbose) {
                System.err.println("Failed to extract application parameters");
            }
            ioe.printStackTrace();
        }

        return (String[]) args.toArray(new String[0]);
    }

    // FIXME: needs localization, since these are presented to the user
    private static void abort(final Throwable cause, final String fmt, final Object... args) {
        String msg = String.format(fmt, args);
        if (msg != null) {
            System.err.println(msg);
        }
        
        if (trace) {
            if (cause != null) {
                cause.printStackTrace();
            } else {
                Thread.dumpStack();
            }
        }
        System.exit(1);
    }

    private static Attributes getJarAttributes(String jarPath) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                abort(null, "No manifest in jar file %1$s", jarPath);
            }
            return manifest.getMainAttributes();
        } catch (IOException ioe) {
            abort(ioe, "Error launching jar file %1%s", jarPath);
        } finally {
            try {
                jarFile.close();
            } catch (IOException ioe) {}
        }
        return null;
    }

    private static void startToolkit() throws InterruptedException {
        if (toolkitStarted.getAndSet(true)) {
            return;
        }

        if (SystemProperties.isDebug()) {
            MXExtension.initializeIfAvailable();
        }

        final CountDownLatch startupLatch = new CountDownLatch(1);

        // Note, this method is called on the FX Application Thread
        PlatformImpl.startup(() -> startupLatch.countDown());

        // Wait for FX platform to start
        startupLatch.await();
    }

    private static volatile boolean error = false;
    private static volatile Throwable pConstructorError = null;
    private static volatile Throwable pInitError = null;
    private static volatile Throwable pStartError = null;
    private static volatile Throwable pStopError = null;
    private static volatile Throwable constructorError = null;
    private static volatile Throwable initError = null;
    private static volatile Throwable startError = null;
    private static volatile Throwable stopError = null;

    private static void launchApplication1(final Class<? extends Application> appClass,
            final Class<? extends Preloader> preloaderClass,
            final String[] args) throws Exception {

        startToolkit();

        if (savedMainCcl != null) {
            /*
             * The toolkit was already started by the java launcher, and the
             * main method of the application class was called. Check to see
             * whether the CCL has been changed. If so, then we need
             * to pass the context class loader to the FX app thread so that it
             * correctly picks up the current setting.
             */
            final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            if (ccl != null && ccl != savedMainCcl) {
                PlatformImpl.runLater(() -> {
                    Thread.currentThread().setContextClassLoader(ccl);
                });
            }
        }

        final AtomicBoolean pStartCalled = new AtomicBoolean(false);
        final AtomicBoolean startCalled = new AtomicBoolean(false);
        final AtomicBoolean exitCalled = new AtomicBoolean(false);
        final AtomicBoolean pExitCalled = new AtomicBoolean(false);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        final CountDownLatch pShutdownLatch = new CountDownLatch(1);

        final PlatformImpl.FinishListener listener = new PlatformImpl.FinishListener() {
            @Override public void idle(boolean implicitExit) {
                if (!implicitExit) {
                    return;
                }

//                System.err.println("JavaFX Launcher: system is idle");
                if (startCalled.get()) {
                    shutdownLatch.countDown();
                } else if (pStartCalled.get()) {
                    pShutdownLatch.countDown();
                }
            }

            @Override public void exitCalled() {
//                System.err.println("JavaFX Launcher: received exit notification");
                exitCalled.set(true);
                shutdownLatch.countDown();
            }
        };
        PlatformImpl.addListener(listener);

        try {
            final AtomicReference<Preloader> pldr = new AtomicReference<>();
            if (preloaderClass != null) {
                // Construct an instance of the preloader on the FX thread, then
                // call its init method on this (launcher) thread. Then call
                // the start method on the FX thread.
                PlatformImpl.runAndWait(() -> {
                    try {
                        Constructor<? extends Preloader> c = preloaderClass.getConstructor();
                        pldr.set(c.newInstance());
                        // Set startup parameters
                        ParametersImpl.registerParameters(pldr.get(), new ParametersImpl(args));
                    } catch (Throwable t) {
                        System.err.println("Exception in Preloader constructor");
                        pConstructorError = t;
                        error = true;
                    }
                });
            }
            currentPreloader = pldr.get();

            // Call init method unless exit called or error detected
            if (currentPreloader != null && !error && !exitCalled.get()) {
                try {
                    // Call the application init method (on the Launcher thread)
                    currentPreloader.init();
                } catch (Throwable t) {
                    System.err.println("Exception in Preloader init method");
                    pInitError = t;
                    error = true;
                }
            }

            // Call start method unless exit called or error detected
            if (currentPreloader != null && !error && !exitCalled.get()) {
                // Call the application start method on FX thread
                PlatformImpl.runAndWait(() -> {
                    try {
                        pStartCalled.set(true);

                        // Create primary stage and call preloader start method
                        final Stage primaryStage = new Stage();
                        primaryStage.impl_setPrimary(true);
                        currentPreloader.start(primaryStage);
                    } catch (Throwable t) {
                        System.err.println("Exception in Preloader start method");
                        pStartError = t;
                        error = true;
                    }
                });

                // Notify preloader of progress
                if (!error && !exitCalled.get()) {
                    notifyProgress(currentPreloader, 0.0);
                }
            }

            // Construct an instance of the application on the FX thread, then
            // call its init method on this (launcher) thread. Then call
            // the start method on the FX thread.
            final AtomicReference<Application> app = new AtomicReference<>();
            if (!error && !exitCalled.get()) {
                if (currentPreloader != null) {
                    if (simulateSlowProgress) {
                        for (int i = 0; i < 100; i++) {
                            notifyProgress(currentPreloader, (double)i / 100.0);
                            Thread.sleep(10);
                        }
                    }
                    notifyProgress(currentPreloader, 1.0);
                    notifyStateChange(currentPreloader,
                            StateChangeNotification.Type.BEFORE_LOAD, null);
                }

                PlatformImpl.runAndWait(() -> {
                    try {
                        Constructor<? extends Application> c = appClass.getConstructor();
                        app.set(c.newInstance());
                        // Set startup parameters
                        ParametersImpl.registerParameters(app.get(), new ParametersImpl(args));
                        PlatformImpl.setApplicationName(appClass);
                    } catch (Throwable t) {
                        System.err.println("Exception in Application constructor");
                        constructorError = t;
                        error = true;
                    }
                });
            }
            final Application theApp = app.get();

            // Call init method unless exit called or error detected
            if (!error && !exitCalled.get()) {
                if (currentPreloader != null) {
                    notifyStateChange(currentPreloader,
                            StateChangeNotification.Type.BEFORE_INIT, theApp);
                }

                try {
                    // Call the application init method (on the Launcher thread)
                    theApp.init();
                } catch (Throwable t) {
                    System.err.println("Exception in Application init method");
                    initError = t;
                    error = true;
                }
            }

            // Call start method unless exit called or error detected
            if (!error && !exitCalled.get()) {
                if (currentPreloader != null) {
                    notifyStateChange(currentPreloader,
                            StateChangeNotification.Type.BEFORE_START, theApp);
                }
                // Call the application start method on FX thread
                PlatformImpl.runAndWait(() -> {
                    try {
                        startCalled.set(true);

                        // Create primary stage and call application start method
                        final Stage primaryStage = new Stage();
                        primaryStage.impl_setPrimary(true);
                        theApp.start(primaryStage);
                    } catch (Throwable t) {
                        System.err.println("Exception in Application start method");
                        startError = t;
                        error = true;
                    }
                });
            }

            if (!error) {
                shutdownLatch.await();
//                System.err.println("JavaFX Launcher: time to call stop");
            }

            // Call stop method if start was called
            if (startCalled.get()) {
                // Call Application stop method on FX thread
                PlatformImpl.runAndWait(() -> {
                    try {
                        theApp.stop();
                    } catch (Throwable t) {
                        System.err.println("Exception in Application stop method");
                        stopError = t;
                        error = true;
                    }
                });
            }

            if (error) {
                if (pConstructorError != null) {
                    throw new RuntimeException("Unable to construct Preloader instance: "
                            + appClass, pConstructorError);
                } else if (pInitError != null) {
                    throw new RuntimeException("Exception in Preloader init method",
                            pInitError);
                } else if(pStartError != null) {
                    throw new RuntimeException("Exception in Preloader start method",
                            pStartError);
                } else if (pStopError != null) {
                    throw new RuntimeException("Exception in Preloader stop method",
                            pStopError);
                } else if (constructorError != null) {
                    String msg = "Unable to construct Application instance: " + appClass;
                    if (!notifyError(msg, constructorError)) {
                        throw new RuntimeException(msg, constructorError);
                    }
                } else if (initError != null) {
                    String msg = "Exception in Application init method";
                    if (!notifyError(msg, initError)) {
                        throw new RuntimeException(msg, initError);
                    }
                } else if(startError != null) {
                    String msg = "Exception in Application start method";
                    if (!notifyError(msg, startError)) {
                        throw new RuntimeException(msg, startError);
                    }
                } else if (stopError != null) {
                    String msg = "Exception in Application stop method";
                    if (!notifyError(msg, stopError)) {
                        throw new RuntimeException(msg, stopError);
                    }
                }
            }
        } finally {
            PlatformImpl.removeListener(listener);
            // Workaround until RT-13281 is implemented
            // Don't call exit if we detect an error in javaws mode
//            PlatformImpl.tkExit();
            final boolean isJavaws = System.getSecurityManager() != null;
            if (error && isJavaws) {
                System.err.println("Workaround until RT-13281 is implemented: keep toolkit alive");
            } else {
                PlatformImpl.tkExit();
            }
        }
    }

    private static void notifyStateChange(final Preloader preloader,
            final StateChangeNotification.Type type,
            final Application app) {

        PlatformImpl.runAndWait(() -> preloader.handleStateChangeNotification(
            new StateChangeNotification(type, app)));
    }

    private static void notifyProgress(final Preloader preloader, final double d) {
        PlatformImpl.runAndWait(() -> preloader.handleProgressNotification(
                new Preloader.ProgressNotification(d)));
    }

    private static boolean notifyError(final String msg, final Throwable constructorError) {
        final AtomicBoolean result = new AtomicBoolean(false);
        PlatformImpl.runAndWait(() -> {
            if (currentPreloader != null) {
                try {
                    ErrorNotification evt = new ErrorNotification(null, msg, constructorError);
                    boolean rval = currentPreloader.handleErrorNotification(evt);
                    result.set(rval);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });

        return result.get();
    }

    private static void notifyCurrentPreloader(final PreloaderNotification pe) {
        PlatformImpl.runAndWait(() -> {
            if (currentPreloader != null) {
                currentPreloader.handleApplicationNotification(pe);
            }
        });
    }

    private static Method notifyMethod = null;

    public static void notifyPreloader(Application app, final PreloaderNotification info) {
        if (launchCalled.get()) {
            // Standalone launcher mode
            notifyCurrentPreloader(info);
            return;
        }

        synchronized (LauncherImpl.class) {
            if (notifyMethod == null) {
                final String fxPreloaderClassName =
                        "com.sun.deploy.uitoolkit.impl.fx.FXPreloader";
                try {
                    Class fxPreloaderClass = Class.forName(fxPreloaderClassName);
                    notifyMethod = fxPreloaderClass.getMethod(
                            "notifyCurrentPreloader", PreloaderNotification.class);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
            }
        }

        try {
            // Call using reflection: FXPreloader.notifyCurrentPreloader(pe)
            notifyMethod.invoke(null, info);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Not an instantiable class.
    private LauncherImpl() {
        // Should never get here.
        throw new InternalError();
    }

}
