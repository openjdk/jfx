/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.jmx.MXExtension;
import com.sun.javafx.runtime.SystemProperties;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.application.Preloader.ErrorNotification;
import javafx.application.Preloader.PreloaderNotification;
import javafx.application.Preloader.StateChangeNotification;
import javafx.stage.Stage;


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

    private static final String MF_MAIN_CLASS = "Main-Class";
    private static final String MF_JAVAFX_MAIN = "JavaFX-Application-Class";
    private static final String MF_JAVAFX_PRELOADER = "JavaFX-Preloader-Class";

    // Set to true to simulate a slow download progress
    private static final boolean simulateSlowProgress = false;

    // Ensure that launchApplication method is only called once
    private static AtomicBoolean launchCalled = new AtomicBoolean(false);

    // Exception found during launching
    private static volatile RuntimeException launchException = null;

    // The current preloader, used for notification in the standalone
    // launcher mode
    private static Preloader currentPreloader = null;

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
    public static void launchApplication(final Class<? extends Application> appClass,
            final String[] args) {

        launchApplication(appClass, null, args);
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
        Thread launcherThread = new Thread(new Runnable() {
            @Override public void run() {
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
        /*
         * For now, just open the jar and get JavaFX-Application-Class and
         * JavaFX-Preloader and pass them to launchApplication. In the future
         * we'll need to load requested jar files and set up the proxy
         */
        String mainClassName = null;
        String preloaderClassName = null;

        if (launchMode.equals(LAUNCH_MODE_JAR)) {
            Attributes jarAttrs = getJarAttributes(launchName);
            if (jarAttrs == null) {
                abort(null, "Can't get manifest attributes from jar");
            }

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

            preloaderClassName = jarAttrs.getValue(MF_JAVAFX_PRELOADER);
            if (preloaderClassName != null) {
                preloaderClassName = preloaderClassName.trim();
            }
        } else if (launchMode.equals(LAUNCH_MODE_CLASS)) {
            mainClassName = launchName;
        } else {
            abort(new IllegalArgumentException("The launchMode argument must be one of LM_CLASS or LM_JAR"),
                    "Invalid launch mode: %1$s", launchMode);
        }

        if (mainClassName == null) {
            abort(null, "No main JavaFX class to launch");
        }

        // FIXME: need to create a new classloader to support JavaFX-Class-Path
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (loader == null) {
            abort(null, "Unable to load JavaFX application class");
        }

        Class<? extends Application> appClass = null;
        Class<? extends Preloader> preClass = null;
        Class<?> tempClass = null;

        try {
            tempClass = loader.loadClass(mainClassName);
        } catch (ClassNotFoundException cnfe) {
            abort(cnfe, "Missing JavaFX application class %1$s", mainClassName);
        }

        // Verify appClass extends Application
        if (!Application.class.isAssignableFrom(tempClass)) {
            // See if it has a main(String[]) method
            try {
                Method mainMethod = tempClass.getMethod("main",
                        new Class[] { (new String[0]).getClass() });
                mainMethod.invoke(null, new Object[] { args });
            } catch (NoSuchMethodException ex) {
                abort(null, "JavaFX application class %1$s does not extend javafx.application.Application", tempClass.getName());
            } catch (IllegalAccessException ex) {
                abort(ex, "JavaFX application class %1$s does not extend javafx.application.Application", tempClass.getName());
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
                abort(null, "Exception running application %1$s", tempClass.getName());
            }
            return;
        }
        appClass = tempClass.asSubclass(Application.class);

        if (preloaderClassName != null) {
            try {
                tempClass = loader.loadClass(preloaderClassName);
            } catch (ClassNotFoundException cnfe) {
                abort(cnfe, "Missing JavaFX preloader class %1$s", preloaderClassName);
            }

            if (!Preloader.class.isAssignableFrom(tempClass)) {
                abort(null, "JavaFX preloader class %1$s does not extend javafx.application.Preloader", preClass.getName());
            }
            preClass = tempClass.asSubclass(Preloader.class);
        }

        /*
         * FIXME: Missing support for the following Manifest attributes:
         * JavaFX-Class-Path
         * JavaFX-Feature-Proxy
         * JavaFX-Version
         * JavaFX-Fallback-Class
         * JavaFX-Argument-XXX
         * JavaFX-Parameter-Name-XXX, JavaFX-Parameter-Value-XXX
         */

        // For now just hand off to the other launchApplication method
        launchApplication(appClass, preClass, args);
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

        if (SystemProperties.isDebug()) {
            MXExtension.initializeIfAvailable();
        }

        final CountDownLatch startupLatch = new CountDownLatch(1);
        PlatformImpl.startup(new Runnable() {
            // Note, this method is called on the FX Application Thread
            @Override public void run() {
                startupLatch.countDown();
            }
        });

        // Wait for FX platform to start
        startupLatch.await();

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
            Preloader pldr = null;
            if (preloaderClass != null) {
                // Construct an instance of the preloader and call its init
                // method on this thread. Then call the start method on the FX thread.
                try {
                    Constructor<? extends Preloader> c = preloaderClass.getConstructor();
                    pldr = c.newInstance();
                    // Set startup parameters
                    ParametersImpl.registerParameters(pldr, new ParametersImpl(args));
                } catch (Throwable t) {
                    System.err.println("Exception in Preloader constructor");
                    pConstructorError = t;
                    error = true;
                }
            }
            currentPreloader = pldr;

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
                PlatformImpl.runAndWait(new Runnable() {
                    @Override public void run() {
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
                    }
                });

                // Notify preloader of progress
                if (!error && !exitCalled.get()) {
                    notifyProgress(currentPreloader, 0.0);
                }
            }

            // Construct an instance of the application and call its init
            // method on this thread. Then call the start method on the FX thread.
            Application app = null;
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

                try {
                    Constructor<? extends Application> c = appClass.getConstructor();
                    app = c.newInstance();
                    // Set startup parameters
                    ParametersImpl.registerParameters(app, new ParametersImpl(args));
                } catch (Throwable t) {
                    System.err.println("Exception in Application constructor");
                    constructorError = t;
                    error = true;
                }
            }
            final Application theApp = app;

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
                PlatformImpl.runAndWait(new Runnable() {
                    @Override public void run() {
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
                PlatformImpl.runAndWait(new Runnable() {
                    @Override public void run() {
                        try {
                            theApp.stop();
                        } catch (Throwable t) {
                            System.err.println("Exception in Application stop method");
                            stopError = t;
                            error = true;
                        }
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

        PlatformImpl.runAndWait(new Runnable() {
            @Override public void run() {
                preloader.handleStateChangeNotification(
                    new Preloader.StateChangeNotification(type, app));
            }
        });
    }

    private static void notifyProgress(final Preloader preloader, final double d) {
        PlatformImpl.runAndWait(new Runnable() {
            @Override public void run() {
                preloader.handleProgressNotification(
                        new Preloader.ProgressNotification(d));
            }
        });
    }

    private static boolean notifyError(final String msg, final Throwable constructorError) {
        final AtomicBoolean result = new AtomicBoolean(false);
        PlatformImpl.runAndWait(new Runnable() {
            @Override public void run() {
                if (currentPreloader != null) {
                    try {
                        ErrorNotification evt = new ErrorNotification(null, msg, constructorError);
                        boolean rval = currentPreloader.handleErrorNotification(evt);
                        result.set(rval);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });

        return result.get();
    }

    private static void notifyCurrentPreloader(final PreloaderNotification pe) {
        PlatformImpl.runAndWait(new Runnable() {
            @Override public void run() {
                if (currentPreloader != null) {
                    currentPreloader.handleApplicationNotification(pe);
                }
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
