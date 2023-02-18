/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.tk.Toolkit;
import java.util.Optional;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.input.KeyCode;

/**
 * Application platform support class.
 * @since JavaFX 2.0
 */
public final class Platform {

    // To prevent instantiation
    private Platform() {
    }

    /**
     * This method starts the JavaFX runtime. The specified Runnable will then be
     * called on the JavaFX Application Thread. In general it is not necessary to
     * explicitly call this method, since it is invoked as a consequence of
     * how most JavaFX applications are built. However there are valid use cases
     * for calling this method directly. Because this method starts the JavaFX
     * runtime, there is not yet any JavaFX Application Thread, so it is normal
     * that this method is called directly on the main thread of the application.
     *
     * <p>
     * This method may or may not return to the caller before the run method
     * of the specified Runnable has been called. In any case, this
     * method is not blocked on the specified Runnable being executed.
     * Once this method
     * returns, you may call {@link #runLater(Runnable)} with additional Runnables.
     * Those Runnables will be called, also on the JavaFX Application Thread,
     * after the Runnable passed into this method has been called.
     * </p>
     *
     * <p>As noted, it is normally the case that the JavaFX Application Thread
     * is started automatically. It is important that this method only be called
     * when the JavaFX runtime has not yet been initialized. Situations where
     * the JavaFX runtime is started automatically include:
     * </p>
     *
     * <ul>
     *   <li>For standard JavaFX applications that extend {@link Application}, and
     *   use either the Java launcher or one of the launch methods in the
     *   Application class to launch the application, the FX runtime is
     *   initialized automatically by the launcher before the {@code Application}
     *   class is loaded.</li>
     *   <li>For Swing applications that use {@link javafx.embed.swing.JFXPanel}
     *   to display FX content, the
     *   FX runtime is initialized when the first {@code JFXPanel} instance is
     *   constructed.</li>
     *   <li>For SWT application that use {@code FXCanvas} to display FX content,
     *   the FX runtime is initialized when the first {@code FXCanvas} instance is
     *   constructed.</li>
     * </ul>
     *
     * <p>When an application does not follow any of these common approaches,
     * then it becomes the responsibility of the developer to manually start the
     * JavaFX runtime by calling this startup method.
     * </p>
     *
     * <p>Calling this method when the JavaFX runtime is already running will result in an
     * {@link IllegalStateException} being thrown - it is only valid to request
     * that the JavaFX runtime be started once.
     * </p>
     *
     * <p><b>Note:</b> The JavaFX classes must be loaded from a set of
     * named {@code javafx.*} modules on the <em>module path</em>.
     * Loading the JavaFX classes from the classpath is not supported.
     * A warning is logged when the JavaFX runtime is started if the JavaFX
     * classes are not loaded from the expected named module.
     * This warning is logged regardless of whether the JavaFX runtime was
     * started by calling this method or automatically as described above.
     *
     * @throws IllegalStateException if the JavaFX runtime is already running
     *
     * @param runnable the Runnable whose run method will be executed on the
     * JavaFX Application Thread once it has been started
     *
     * @see Application
     *
     * @since 9
     */
    public static void startup(Runnable runnable) {
        PlatformImpl.startup(runnable, true);
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
     * {@link Application}, and use either the Java launcher or one of the
     * launch methods in the Application class to launch the application,
     * the FX runtime is initialized by the launcher before the Application
     * class is loaded.
     * For Swing applications that use JFXPanel to display FX content, the FX
     * runtime is initialized when the first JFXPanel instance is constructed.
     * For SWT application that use FXCanvas to display FX content, the FX
     * runtime is initialized when the first FXCanvas instance is constructed.
     * For applications that do not follow any of these approaches, then it is
     * necessary to manually start the JavaFX runtime by calling
     * {@link #startup(Runnable)} once.
     * </p>
     *
     * <p>
     * Memory consistency effects: Actions in a thread prior to submitting a
     * {@code runnable} to this method <i>happen-before</i> actions performed
     * by the runnable in the JavaFX Application Thread.
     * </p>
     *
     * @param runnable the Runnable whose run method will be executed on the
     * JavaFX Application Thread
     *
     * @throws IllegalStateException if the FX runtime has not been initialized
     *
     * @see Application
     */
    public static void runLater(Runnable runnable) {
        PlatformImpl.runLater(runnable);
    }

    // NOTE: Add the following if we decide to expose it publicly
//    public static void runAndWait(Runnable runnable) {
//        PlatformImpl.runAndWait(runnable);
//    }

    /**
     * Requests the Java Runtime to perform a pulse. This will run a pulse
     * even if there are no animation timers, scene graph modifications,
     * or window events that would otherwise cause the pulse to run.
     * If no pulse is in progress, then one will be scheduled to
     * run the next time the pulse timer fires.
     * If there is already a pulse running, then
     * at least one more pulse after the current pulse will be scheduled.
     * This method may be called on any thread.
     *
     * @since 9
     */
    public static void requestNextPulse() {
        Toolkit.getToolkit().requestNextPulse();
    }

    /**
     * Returns true if the calling thread is the JavaFX Application Thread.
     * Use this call to ensure that a given task is being executed
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
     * @return true if a specific conditional feature is supported by the
     * platform, otherwise false
     */
    public static boolean isSupported(ConditionalFeature feature) {
        return PlatformImpl.isSupported(feature);
    }

    /**
     * Enter a nested event loop and block until the corresponding
     * exitNestedEventLoop call is made.
     * The key passed into this method is used to
     * uniquely identify the matched enter/exit pair. This method creates a
     * new nested event loop and blocks until the corresponding
     * exitNestedEventLoop method is called with the same key.
     * The return value of this method will be the {@code rval}
     * object supplied to the exitNestedEventLoop method call that unblocks it.
     *
     * <p>
     * This method must either be called from an input event handler or
     * from the run method of a Runnable passed to
     * {@link javafx.application.Platform#runLater Platform.runLater}.
     * It must not be called during animation or layout processing.
     * </p>
     *
     * @param key the Object that identifies the nested event loop, which
     * must not be null
     *
     * @throws IllegalArgumentException if the specified key is associated
     * with a nested event loop that has not yet returned
     *
     * @throws NullPointerException if the key is null
     *
     * @throws IllegalStateException if this method is called during
     * animation or layout processing.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     *
     * @return the value passed into the corresponding call to exitEventLoop
     *
     * @since 9
     */
    public static Object enterNestedEventLoop(Object key) {
        return Toolkit.getToolkit().enterNestedEventLoop(key);
    }

    /**
     * Exit a nested event loop and unblock the caller of the
     * corresponding enterNestedEventLoop.
     * The key passed into this method is used to
     * uniquely identify the matched enter/exit pair. This method causes the
     * nested event loop that was previously created with the key to exit and
     * return control to the caller. If the specified nested event loop is not
     * the inner-most loop then it will not return until all other inner loops
     * also exit.
     *
     * @param key the Object that identifies the nested event loop, which
     * must not be null
     *
     * @param rval an Object that is returned to the caller of the
     * corresponding enterNestedEventLoop. This may be null.
     *
     * @throws IllegalArgumentException if the specified key is not associated
     * with an active nested event loop
     *
     * @throws NullPointerException if the key is null
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the FX Application thread
     *
     * @since 9
     */
    public static void exitNestedEventLoop(Object key, Object rval) {
        Toolkit.getToolkit().exitNestedEventLoop(key, rval);
    }

    /**
     * Returns a flag indicating whether the key corresponding to {@code keyCode}
     * is in the locked (or "on") state.
     * {@code keyCode} must be one of: {@link KeyCode#CAPS} or
     * {@link KeyCode#NUM_LOCK}.
     * If the underlying system is not able to determine the state of the
     * specified {@code keyCode}, an empty {@code Optional} is returned.
     * If the keyboard attached to the system doesn't have the specified key,
     * an {@code Optional} containing {@code false} is returned.
     * This method must be called on the JavaFX Application thread.
     *
     * @param keyCode the {@code KeyCode} of the lock state to query
     *
     * @return the lock state of the key corresponding to {@code keyCode},
     * or an empty {@code Optional} if the system cannot determine its state
     *
     * @throws IllegalArgumentException if {@code keyCode} is not one of the
     * valid {@code KeyCode} values
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread
     *
     * @since 17
     */
    public static Optional<Boolean> isKeyLocked(KeyCode keyCode) {
        Toolkit.getToolkit().checkFxUserThread();

        switch (keyCode) {
            case CAPS:
            case NUM_LOCK:
                break;
            default:
                throw new IllegalArgumentException("Invalid KeyCode");
        }
        return Toolkit.getToolkit().isKeyLocked(keyCode);
    }

    /**
     * Checks whether a nested event loop is running, returning true to indicate
     * that one is, and false if there are no nested event loops currently
     * running.
     * This method must be called on the JavaFX Application thread.
     *
     * @return true if there is a nested event loop running, and false otherwise.
     *
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     *
     * @since 9
     */
    public static boolean isNestedLoopRunning() {
        return Toolkit.getToolkit().isNestedLoopRunning();
    }

    /**
     * Indicates whether a nested event loop can be started from the current thread in the current state.
     * A nested event loop can be started from an event handler or from a {@code Runnable} passed to
     * {@link #runLater(Runnable)}.
     * This method must be called on the JavaFX Application thread.
     *
     * @return {@code true} if a nested event loop can be started, and {@code false} otherwise.
     *
     * @throws IllegalStateException if this method is called on a thread other than the JavaFX Application Thread.
     *
     * @since 21
     */
    public static boolean canStartNestedEventLoop() {
        return Toolkit.getToolkit().canStartNestedEventLoop();
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
