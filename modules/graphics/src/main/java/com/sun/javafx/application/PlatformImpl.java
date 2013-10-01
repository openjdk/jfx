/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.runtime.SystemProperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Application;
import javafx.application.ConditionalFeature;

import com.sun.javafx.tk.TKListener;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class PlatformImpl {

    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static AtomicBoolean platformExit = new AtomicBoolean(false);
    private static AtomicBoolean toolkitExit = new AtomicBoolean(false);
    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static AtomicBoolean listenersRegistered = new AtomicBoolean(false);
    private static TKListener toolkitListener = null;
    private static volatile boolean implicitExit = true;
    private static boolean taskbarApplication = true;
    private static boolean contextual2DNavigation;
    private static AtomicInteger pendingRunnables = new AtomicInteger(0);
    private static AtomicInteger numWindows = new AtomicInteger(0);
    private static volatile boolean firstWindowShown = false;
    private static volatile boolean lastWindowClosed = false;
    private static AtomicBoolean reallyIdle = new AtomicBoolean(false);
    private static Set<FinishListener> finishListeners =
            new CopyOnWriteArraySet<FinishListener>();
    private final static Object runLaterLock = new Object();
    private static Boolean isGraphicsSupported;
    private static Boolean isControlsSupported;
    private static Boolean isMediaSupported;
    private static Boolean isWebSupported;
    private static Boolean isSWTSupported;
    private static Boolean isSwingSupported;
    private static Boolean isFXMLSupported;
    private static Boolean hasTwoLevelFocus;
    private static Boolean hasVirtualKeyboard;
    private static Boolean hasTouch;
    private static Boolean hasMultiTouch;
    private static Boolean hasPointer;
    private static boolean isThreadMerged = false;

    /**
     * Set a flag indicating whether this application should show up in the
     * task bar. The default value is true.
     *
     * @param taskbarApplication the new value of this attribute
     */
    public static void setTaskbarApplication(boolean taskbarApplication) {
        PlatformImpl.taskbarApplication = taskbarApplication;
    }

    /**
     * Returns the current value of the taskBarApplication flag.
     *
     * @return the current state of the flag.
     */
    public static boolean isTaskbarApplication() {
        return taskbarApplication;
    }

    /**
     * Sets the name of the this application based on the Application class.
     * This method is called by the launcher or by the deploy code, and is not
     * called from the FX Application Thread, so we need to do it in a runLater.
     * We do not need to wait for the result since it will complete before the
     * Application start() method is called regardless.
     *
     * @param appClass the Application class.
     */
    public static void setApplicationName(final Class appClass) {
        runLater(new Runnable() {
            @Override public void run() {
                com.sun.glass.ui.Application.GetApplication().setName(appClass.getName());
            }
        });
    }

    /**
     * Return whether or not focus navigation between controls is context-
     * sensitive.
     * @return true if the context-sensitive algorithm for focus navigation is
     * used
     */
     public static boolean isContextual2DNavigation() {
         return contextual2DNavigation;
     }

    /**
     * This method is invoked typically on the main thread. At this point,
     * the JavaFX Application Thread has not been started. Any attempt
     * to call startup twice results in an exception.
     * @param r
     */
    public static void startup(final Runnable r) {

        // NOTE: if we ever support re-launching an application and/or
        // launching a second application in the same VM/classloader
        // this will need to be changed.
        if (platformExit.get()) {
            throw new IllegalStateException("Platform.exit has been called");
        }

        if (initialized.getAndSet(true)) {
            // If we've already initialized, just put the runnable on the queue.
            runLater(r);
            return;
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override public Void run() {
                contextual2DNavigation = Boolean.getBoolean(
                        "com.sun.javafx.isContextual2DNavigation");
                String s = System.getProperty("com.sun.javafx.twoLevelFocus");
                if (s != null) {
                    hasTwoLevelFocus = Boolean.valueOf(s);
                }
                s = System.getProperty("com.sun.javafx.virtualKeyboard");
                if (s != null) {
                    if (s.equalsIgnoreCase("none")) {
                        hasVirtualKeyboard = false;
                    } else if (s.equalsIgnoreCase("javafx")) {
                        hasVirtualKeyboard = true;
                    } else if (s.equalsIgnoreCase("native")) {
                        hasVirtualKeyboard = true;
                    }
                }
                s = System.getProperty("com.sun.javafx.touch");
                if (s != null) {
                    hasTouch = Boolean.valueOf(s);
                }
                s = System.getProperty("com.sun.javafx.multiTouch");
                if (s != null) {
                    hasMultiTouch = Boolean.valueOf(s);
                }
                s = System.getProperty("com.sun.javafx.pointer");
                if (s != null) {
                    hasPointer = Boolean.valueOf(s);
                }
                s = System.getProperty("javafx.embed.singleThread");
                if (s != null) {
                    isThreadMerged = Boolean.valueOf(s);
                }
                return null;
            }
        });

        if (!taskbarApplication) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override public Void run() {
                    System.setProperty("glass.taskbarApplication", "false");
                    return null;
                }
            });
        }

        // Create Toolkit listener and register it with the Toolkit.
        // Call notifyFinishListeners when we get notified.
        toolkitListener = new TKListener() {
            @Override public void changedTopLevelWindows(List<TKStage> windows) {
                numWindows.set(windows.size());
                checkIdle();
            }

            @Override
            public void exitedLastNestedLoop() {
                checkIdle();
            }
        };
        Toolkit.getToolkit().addTkListener(toolkitListener);

        Toolkit.getToolkit().startup(new Runnable() {
            @Override public void run() {
                startupLatch.countDown();
                r.run();
            }
        });

        //Initialize the thread merging mechanism
        if (isThreadMerged) {
            installFwEventQueue();
        }
    }

    private static void installFwEventQueue() {
        invokeSwingFXUtilsMethod("installFwEventQueue");
    }

    private static void removeFwEventQueue() {
        invokeSwingFXUtilsMethod("removeFwEventQueue");
    }

    private static void invokeSwingFXUtilsMethod(final String methodName) {
        //Use reflection in case we are running compact profile
        try {
            Class swingFXUtilsClass = Class.forName("javafx.embed.swing.SwingFXUtils");
            Method installFwEventQueue = swingFXUtilsClass.getMethod(methodName);

            waitForStart();
            installFwEventQueue.invoke(null);

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Property javafx.embed.singleThread is not supported");
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForStart() {
        // If the startup runnable has not yet been called, then wait it.
        // Note that we check the count before calling await() to avoid
        // the try/catch which is unnecessary after startup.
        if (startupLatch.getCount() > 0) {
            try {
                startupLatch.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static boolean isFxApplicationThread() {
        return Toolkit.getToolkit().isFxUserThread();
    }

    public static void runLater(final Runnable r) {
        runLater(r, false);
    }

    private static void runLater(final Runnable r, boolean exiting) {
        if (!initialized.get()) {
            throw new IllegalStateException("Toolkit not initialized");
        }

        pendingRunnables.incrementAndGet();
        waitForStart();

        if (SystemProperties.isDebug()) {
            Toolkit.getToolkit().pauseCurrentThread();
        }

        synchronized (runLaterLock) {
            if (!exiting && toolkitExit.get()) {
                // Don't schedule a runnable after we have exited the toolkit
                pendingRunnables.decrementAndGet();
                return;
            }

            final AccessControlContext acc = AccessController.getContext();
            // Don't catch exceptions, they are handled by Toolkit.defer()
            Toolkit.getToolkit().defer(new Runnable() {
                @Override public void run() {
                    try {
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                r.run();
                                return null;
                            }
                        }, acc);
                    } finally {
                        pendingRunnables.decrementAndGet();
                        checkIdle();
                    }
                }
            });
        }
    }

    public static void runAndWait(final Runnable r) {
        runAndWait(r, false);
    }

    private static void runAndWait(final Runnable r, boolean exiting) {
        if (SystemProperties.isDebug()) {
            Toolkit.getToolkit().pauseCurrentThread();
        }

        if (isFxApplicationThread()) {
             try {
                 r.run();
             } catch (Throwable t) {
                 System.err.println("Exception in runnable");
                 t.printStackTrace();
             }
        } else {
            final CountDownLatch doneLatch = new CountDownLatch(1);
            runLater(new Runnable() {
                @Override public void run() {
                    try {
                        r.run();
                    } finally {
                        doneLatch.countDown();
                    }
                }
            }, exiting);

            if (!exiting && toolkitExit.get()) {
                throw new IllegalStateException("Toolkit has exited");
            }

            try {
                doneLatch.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void setImplicitExit(boolean implicitExit) {
        PlatformImpl.implicitExit = implicitExit;
        checkIdle();
    }

    public static boolean isImplicitExit() {
        return implicitExit;
    }

    public static void addListener(FinishListener l) {
        listenersRegistered.set(true);
        finishListeners.add(l);
    }

    public static void removeListener(FinishListener l) {
        finishListeners.remove(l);
        listenersRegistered.set(!finishListeners.isEmpty());
        if (!listenersRegistered.get()) {
            checkIdle();
        }
    }

    private static void notifyFinishListeners(boolean exitCalled) {
        // Notify listeners if any are registered, else exit directly
        if (listenersRegistered.get()) {
            for (FinishListener l : finishListeners) {
                if (exitCalled) {
                    l.exitCalled();
                } else {
                    l.idle(implicitExit);
                }
            }
        } else if (implicitExit || platformExit.get()) {
            tkExit();
        }
    }

    // Check for idle, meaning the last top-level window has been closed and
    // there are no pending Runnables waiting to be run.
    private static void checkIdle() {
        boolean doNotify = false;

        synchronized (PlatformImpl.class) {
            int numWin = numWindows.get();
            if (numWin > 0) {
                firstWindowShown = true;
                lastWindowClosed = false;
                reallyIdle.set(false);
            } else if (numWin == 0 && firstWindowShown) {
                lastWindowClosed = true;
            }

            // In case there is an event in process, allow for it to show
            // another window. If no new window is shown before all pending
            // runnables (including this one) are done and there is no running
            // nested loops, then we will shutdown.
            if (lastWindowClosed && pendingRunnables.get() == 0
                    && (toolkitExit.get() || !Toolkit.getToolkit().isNestedLoopRunning())) {
//                System.err.println("Last window closed and no pending runnables");
                if (reallyIdle.getAndSet(true)) {
//                    System.err.println("Really idle now");
                    doNotify = true;
                    lastWindowClosed = false;
                } else {
//                    System.err.println("Queuing up a dummy idle check runnable");
                    runLater(new Runnable() {
                        @Override public void run() {
//                            System.err.println("Dummy runnable");
                        }
                    });
                }
            }
        }

        if (doNotify) {
            notifyFinishListeners(false);
        }
    }

    // package scope method for testing
    private static final CountDownLatch platformExitLatch = new CountDownLatch(1);
    static CountDownLatch test_getPlatformExitLatch() {
        return platformExitLatch;
    }

    public static void tkExit() {
        if (toolkitExit.getAndSet(true)) {
            return;
        }

        if (initialized.get()) {
            // Always call toolkit exit on FX app thread
//            System.err.println("PlatformImpl.tkExit: scheduling Toolkit.exit");
            PlatformImpl.runAndWait(new Runnable() {
                @Override public void run() {
//                    System.err.println("PlatformImpl.tkExit: calling Toolkit.exit");
                    Toolkit.getToolkit().exit();
                }
            }, true);

            if (isThreadMerged) {
                removeFwEventQueue();
            }

            Toolkit.getToolkit().removeTkListener(toolkitListener);
            toolkitListener = null;
            platformExitLatch.countDown();
        }
    }

    public static void exit() {
        platformExit.set(true);
        notifyFinishListeners(true);
    }

    private static Boolean checkForClass(String classname) {
        try {
            Class.forName(classname, false, PlatformImpl.class.getClassLoader());
            return Boolean.TRUE;
        } catch (ClassNotFoundException cnfe) {
            return Boolean.FALSE;
        }
    }

    public static boolean isSupported(ConditionalFeature feature) {
        switch (feature) {
            case GRAPHICS:
                if (isGraphicsSupported == null) {
                    isGraphicsSupported = checkForClass("javafx.stage.Stage");
                }
                return isGraphicsSupported;
            case CONTROLS:
                if (isControlsSupported == null) {
                    isControlsSupported = checkForClass(
                            "javafx.scene.control.Control");
                }
                return isControlsSupported;
            case MEDIA:
                if (isMediaSupported == null) {
                    isMediaSupported = checkForClass(
                            "javafx.scene.media.MediaView");
                }
                return isMediaSupported;
            case WEB:
                if (isWebSupported == null) {
                    isWebSupported = checkForClass("javafx.scene.web.WebView");
                }
                return isWebSupported;
            case SWT:
                if (isSWTSupported == null) {
                    isSWTSupported = checkForClass("javafx.embed.swt.FXCanvas");
                }
                return isSWTSupported;
            case SWING:
                if (isSwingSupported == null) {
                    isSwingSupported = 
                        // check for JComponent first, it may not be present
                        checkForClass("javax.swing.JComponent") &&
                        checkForClass("javafx.embed.swing.JFXPanel");
                }
                return isSwingSupported;
            case FXML:
                if (isFXMLSupported == null) {
                    isFXMLSupported = checkForClass("javafx.fxml.FXMLLoader")
                            && checkForClass("javax.xml.stream.XMLInputFactory");
                }
                return isFXMLSupported;
            case TWO_LEVEL_FOCUS:
                if (hasTwoLevelFocus == null) {
                    return Toolkit.getToolkit().isSupported(feature);
                }
                return hasTwoLevelFocus;
            case VIRTUAL_KEYBOARD:
                if (hasVirtualKeyboard == null) {
                    return Toolkit.getToolkit().isSupported(feature);
                }
                return hasVirtualKeyboard;
            case INPUT_TOUCH:
                if (hasTouch == null) {
                    return Toolkit.getToolkit().isSupported(feature);
                }
                return hasTouch;
            case INPUT_MULTITOUCH:
                if (hasMultiTouch == null) {
                    return Toolkit.getToolkit().isSupported(feature);
                }
                return hasMultiTouch;
            case INPUT_POINTER:
                if (hasPointer == null) {
                    return Toolkit.getToolkit().isSupported(feature);
                }
                return hasPointer;
            default:
                return Toolkit.getToolkit().isSupported(feature);
        }
    }

    public static interface FinishListener {
        public void idle(boolean implicitExit);
        public void exitCalled();
    }

    /**
     * Set the platform user agent stylesheet to the default.
     */
    public static void setDefaultPlatformUserAgentStylesheet() {
        setPlatformUserAgentStylesheet(Application.STYLESHEET_MODENA);
    }

    private static boolean isModena = false;
    private static boolean isCaspian = false;

    /**
     * Current Platform User Agent Stylesheet is Modena.
     *
     * Note: Please think hard before using this as we really want to avoid special cases in the platform for specific
     * themes. This was added to allow tempory work arounds in the platform for bugs.
     *
     * @return true if using modena stylesheet
     */
    public static boolean isModena() {
        return isModena;
    }

    /**
     * Current Platform User Agent Stylesheet is Caspian.
     *
     * Note: Please think hard before using this as we really want to avoid special cases in the platform for specific
     * themes. This was added to allow tempory work arounds in the platform for bugs.
     *
     * @return true if using caspian stylesheet
     */
    public static boolean isCaspian() {
        return isCaspian;
    }

    /**
     * Set the platform user agent stylesheet to the given URL. This method has special handling for platform theme
     * name constants.
     */
    public static void setPlatformUserAgentStylesheet(final String stylesheetUrl) {
        if (isFxApplicationThread()) {
            _setPlatformUserAgentStylesheet(stylesheetUrl);
        } else {
            runLater(new Runnable() {
                @Override public void run() {
                    _setPlatformUserAgentStylesheet(stylesheetUrl);
                }
            });
        }
    }

    private static void _setPlatformUserAgentStylesheet(String stylesheetUrl) {
        isModena = isCaspian = false;
        // check for command line override
        final String overrideStylesheetUrl = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                @Override public String run() {
                    return System.getProperty("javafx.userAgentStylesheetUrl");
                }
            });

        if (overrideStylesheetUrl != null) {
            stylesheetUrl = overrideStylesheetUrl;
        }

        // check for named theme constants for modena and caspian
        if (Application.STYLESHEET_CASPIAN.equalsIgnoreCase(stylesheetUrl)) {
            isCaspian = true;
            AccessController.doPrivileged(
                    new PrivilegedAction() {
                        @Override public Object run() {
                            StyleManager.getInstance().setDefaultUserAgentStylesheet("com/sun/javafx/scene/control/skin/caspian/caspian.css");

                            if (isSupported(ConditionalFeature.INPUT_TOUCH)) {
                                StyleManager.getInstance().addUserAgentStylesheet("com/sun/javafx/scene/control/skin/caspian/embedded.css");                              
                                if (com.sun.javafx.Utils.isQVGAScreen()) {
                                    StyleManager.getInstance().addUserAgentStylesheet("com/sun/javafx/scene/control/skin/caspian/embedded-qvga.css");
                                }
                                if (PlatformUtil.isAndroid()) {
                                    StyleManager.getInstance().addUserAgentStylesheet("com/sun/javafx/scene/control/skin/caspian/android.css");
                                }
                            }
                            return null;
                        }
                    });
        } else if (Application.STYLESHEET_MODENA.equalsIgnoreCase(stylesheetUrl)) {
            isModena = true;
            AccessController.doPrivileged(
                    new PrivilegedAction() {
                        @Override public Object run() {
                            StyleManager.getInstance().setDefaultUserAgentStylesheet("com/sun/javafx/scene/control/skin/modena/modena.css");
                            if (isSupported(ConditionalFeature.INPUT_TOUCH)) {
                                StyleManager.getInstance().addUserAgentStylesheet(
                                        "com/sun/javafx/scene/control/skin/modena/touch.css");
                            }
                            // when running on embedded add a extra stylesheet to tune performance of modena theme
                            if (PlatformUtil.isEmbedded()) {
                                StyleManager.getInstance().addUserAgentStylesheet(
                                        "com/sun/javafx/scene/control/skin/modena/modena-embedded-performance.css");
                            }
                            if (PlatformUtil.isAndroid()) {
                                StyleManager.getInstance().addUserAgentStylesheet("com/sun/javafx/scene/control/skin/modena/android.css");
                            }
                            return null;
                        }
                    });

            // check to see if there is an override to enable a high-contrast theme
            final String highContrastName = AccessController.doPrivileged(
                    new PrivilegedAction<String>() {
                        @Override public String run() {
                            return System.getProperty("com.sun.javafx.highContrastTheme");
                        }
                    });
            if (highContrastName != null) {
                switch (highContrastName.toUpperCase()) {
                    case "BLACKONWHITE": StyleManager.getInstance().addUserAgentStylesheet(
                                                "com/sun/javafx/scene/control/skin/modena/blackOnWhite.css");
                                         break;
                    case "WHITEONBLACK": StyleManager.getInstance().addUserAgentStylesheet(
                                                "com/sun/javafx/scene/control/skin/modena/whiteOnBlack.css");
                                         break;
                    case "YELLOWONBLACK": StyleManager.getInstance().addUserAgentStylesheet(
                                                "com/sun/javafx/scene/control/skin/modena/yellowOnBlack.css");
                                         break;
                }
            }
        } else {
            StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheetUrl);
        }
    }
}
