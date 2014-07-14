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

package javafx.application;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import com.sun.javafx.application.PlatformImpl;

/**
 * Application platform support class.
 * @since JavaFX 2.0
 */
public final class Platform {

    // To prevent instantiation
    private Platform() {
    }

    /**
     * Run the specified Runnable on the JavaFX Application Thread at some
     * unspecified
     * time in the future. This method, which may be called from any thread,
     * will post the Runnable to an event queue and then return immediately to
     * the caller. The Runnables are executed in the order they are posted.
     * A runnable passed into the runLater method will be
     * executed before any Runnable passed into a subsequent call to runLater.
     * If this method is called after the JavaFX runtime has been shutdown, the
     * call will be ignored: the Runnable will not be executed and no
     * exception will be thrown.
     *
     * <p>
     * NOTE: applications should avoid flooding JavaFX with too many
     * pending Runnables. Otherwise, the application may become unresponsive.
     * Applications are encouraged to batch up multiple operations into fewer
     * runLater calls.
     * Additionally, long-running operations should be done on a background
     * thread where possible, freeing up the JavaFX Application Thread for GUI
     * operations.
     * </p>
     *
     * <p>
     * This method must not be called before the FX runtime has been
     * initialized. For standard JavaFX applications that extend
     * {@see Application}, and use either the Java launcher or one of the
     * launch methods in the Application class to launch the application,
     * the FX runtime is initialized by the launcher before the Application
     * class is loaded.
     * For Swing applications that use JFXPanel to display FX content, the FX
     * runtime is initialized when the first JFXPanel instance is constructed.
     * For SWT application that use FXCanvas to display FX content, the FX
     * runtime is initialized when the first FXCanvas instance is constructed.
     * </p>
     *
     * @param runnable the Runnable whose run method will be executed on the
     * JavaFX Application Thread
     *
     * @throws IllegalStateException if the FX runtime has not been initialized
     */
    public static void runLater(Runnable runnable) {
        PlatformImpl.runLater(runnable);
    }

    // NOTE: Add the following if we decide to expose it publicly
//    public static void runAndWait(Runnable runnable) {
//        PlatformImpl.runAndWait(runnable);
//    }

    /**
     * Returns true if the calling thread is the JavaFX Application Thread.
     * Use this call the ensure that a given task is being executed
     * (or not being executed) on the JavaFX Application Thread.
     *
     * @return true if running on the JavaFX Application Thread
     */
    public static boolean isFxApplicationThread() {
        return PlatformImpl.isFxApplicationThread();
    }

    /**
     * Causes the JavaFX application to terminate. If this method is called
     * after the Application start method is called, then the JavaFX launcher
     * will call the Application stop method and terminate the JavaFX
     * application thread. The launcher thread will then shutdown. If there
     * are no other non-daemon threads that are running, the Java VM will exit.
     * If this method is called from the Preloader or the Application init
     * method, then the Application stop method may not be called.
     *
     * <p>This method may be called from any thread.</p>
     *
     * <p>Note: if the application is embedded in a browser, then this method
     * may have no effect.
     */
    public static void exit() {
        PlatformImpl.exit();
    }

    /**
     * Sets the implicitExit attribute to the specified value. If this
     * attribute is true, the JavaFX runtime will implicitly shutdown
     * when the last window is closed; the JavaFX launcher will call the
     * {@link Application#stop} method and terminate the JavaFX
     * application thread.
     * If this attribute is false, the application will continue to
     * run normally even after the last window is closed, until the
     * application calls {@link #exit}.
     * The default value is true.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @param implicitExit a flag indicating whether or not to implicitly exit
     * when the last window is closed.
     * @since JavaFX 2.2
     */
    public static void setImplicitExit(boolean implicitExit) {
        PlatformImpl.setImplicitExit(implicitExit);
    }

    /**
     * Gets the value of the implicitExit attribute.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @return the implicitExit attribute
     * @since JavaFX 2.2
     */
    public static boolean isImplicitExit() {
        return PlatformImpl.isImplicitExit();
    }

    /**
     * Queries whether a specific conditional feature is supported
     * by the platform.
     * <p>
     * For example:
     * <pre>
     * // Query whether filter effects are supported
     * if (Platform.isSupported(ConditionalFeature.EFFECT)) {
     *    // use effects
     * }
     * </pre>
     *
     * @param feature the conditional feature in question.
     */
    public static boolean isSupported(ConditionalFeature feature) {
        return PlatformImpl.isSupported(feature);
    }

    private static ReadOnlyBooleanWrapper accessibilityActiveProperty;

    public static boolean isAccessibilityActive() {
        return accessibilityActiveProperty == null ? false : accessibilityActiveProperty.get();
    }

    /**
     * Indicates whether or not accessibility is active.
     * This property is typically set to true the first time an
     * assistive technology, such as a screen reader, requests
     * information about any JavaFX window or its children.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @return the read-only boolean property indicating if accessibility is active
     *
     * @since JavaFX 8u40 
     */
    public static ReadOnlyBooleanProperty accessibilityActiveProperty() {
        if (accessibilityActiveProperty == null) {
            accessibilityActiveProperty = new ReadOnlyBooleanWrapper(Platform.class, "accessibilityActive");
            accessibilityActiveProperty.bind(PlatformImpl.accessibilityActiveProperty());
        }
        return accessibilityActiveProperty.getReadOnlyProperty();
    }
}
