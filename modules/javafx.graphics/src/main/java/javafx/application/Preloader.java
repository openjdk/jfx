/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Class that is extended to define an optional preloader for a
 * JavaFX Application.
 * An application may contain a preloader that is used
 * to improve the application loading experience.
 *
 * <p>
 * A preloader is a small application that is started
 * before the main application to customize the startup experience.
 * The preloader:
 * </p>
 * <ul>
 * <li>gets notification of progress of loading application resources</li>
 * <li>gets notification of errors</li>
 * <li>gets notification of application initialization and startup</li>
 * <li>decides when application should become visible</li>
 * </ul>
 *
 * <p>
 * The default preloader is shown on top of the application Stage, which is not
 * visible until the preloader is visible. The preloader need to hide itself
 * to make the application visible. Good practice is to do this no earlier than
 * right before application.start() is called, as otherwise application itself
 * is not visible.
 * </p>
 *
 * <p>
 * The preloader may also cooperate with the application to achieve advanced
 * visual effects or share data (e.g. to implement login screen).
 * The preloader gets a reference to the application and may pull data it
 * needs for cooperation from the application if the application implements
 * an interface that the preloader knows about and relies upon. Generally it
 * is not recommended to design preloaders in such a way that an application
 * would call them directly, as this will result in bad user experience if
 * the application is signed and the preloader is not.
 * </p>
 *
 * <p>
 * If the application does not specify a preloader, then the default preloader
 * is used. Default preloader appearance can be customized
 * (set of parameters is TBD).
 * </p>
 *
 * <p>
 * Custom preloader implementations should follow these rules:
 * </p>
 * <ol>
 *  <li>a custom preloader class should extend Preloader</li>
 *  <li>classes needed for preloader need to be packaged in the separate jar.</li>
 * </ol>
 *
 * <p>
 * Applications may also send custom notification to the preloader using the
 * {@link #notifyPreloader notifyPreloader} method. This way a preloader may
 * also show application initialization progress.
 * </p>
 *
 * <p>
 * Note that preloaders are subject to the same rules as other JavaFX
 * applications including FX threading rules. In particular, the class
 * constructor and init() method will be called on a non-FX thread and start()
 * will be executed on the FX application thread.
 * This also means that the application constructor/init() will run concurrently
 * with preloader start().
 * </p>
 *
 * <p>
 * Callbacks on preloader notification will be delivered on the FX
 * application thread.
 * </p>
 *
 * <p>
 * Shutdown (including when stop() is called) is TBD.
 * </p>
 * @since JavaFX 2.0
 */
public abstract class Preloader extends Application {

    // Too bad this isn't already available in a Java core class
    private static final String lineSeparator;

    static {
        @SuppressWarnings("removal")
        String prop = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("line.separator"));
        lineSeparator = prop != null ? prop : "\n";
    }

    /**
     * Constructor for subclasses to call.
     */
    public Preloader() {
    }

    /**
     * Indicates download progress.
     * This method is called by the FX runtime to indicate progress while
     * application resources are being loaded. It will not be called to deliver
     * a ProgressNotification sent to {@link #notifyPreloader notifyPreloader}.
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * does nothing.
     * </p>
     *
     * @param info the progress notification
     */
    public void handleProgressNotification(ProgressNotification info) {
    }

    /**
     * Indicates a change in application state.
     * This method is called by the FX runtime as part of the
     * application life-cycle.
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * does nothing.
     * </p>
     *
     * @param info the state change notification
     */
    public void handleStateChangeNotification(StateChangeNotification info) {
    }

    /**
     * Indicates an application-generated notification.
     * This method is called by the FX runtime to deliver a notification sent
     * via {@link #notifyPreloader notifyPreloader}.
     *
     * <p>
     * Applications should not call this method directly, but should use
     * {@link #notifyPreloader notifyPreloader} instead to avoid mixed code dialog issues.
     * </p>
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * does nothing.
     * </p>
     *
     * @param info the application-generated notification
     */
    public void handleApplicationNotification(PreloaderNotification info) {
    }

    /**
     * Called when an error occurs.
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * returns false, indicating that the default error handler should
     * show the message to the user.
     * </p>
     *
     * @param info the error notification describing the cause of this error
     *
     * @return true if error was shown to the user by preloader and no
     * additional visualization is needed; otherwise, false.
     */
    public boolean handleErrorNotification(ErrorNotification info) {
        return false;
    }

//    /**
//     * Called when security or other system modal dialog is shown or hidden
//     * (such as proxy auth dialog).
//     *
//     * <p>
//     * The implementation of this method provided by the Preloader class
//     * does nothing.
//     * </p>
//     *
//     * @param info the UI notification
//     */
//    public void handleUINotification(UINotification info) {
//        // TODO RT-19601: not used for now pending completion of JRE work
////        System.err.println("Preloader: handleUINotification = " + info);
//    }

    // ------------------------------------------------------------------------

    /**
     * Marker interface for all Preloader notification.
     * @since JavaFX 2.0
     */
    public static interface PreloaderNotification {
    }

    /**
     * Preloader notification that reports an error.
     * This is delivered to preloader in case of problem with application startup.
     * @since JavaFX 2.0
     */
    public static class ErrorNotification implements PreloaderNotification {
        private String location;
        private String details = "";
        private Throwable cause;

        /**
         * Constructs an error notification.
         *
         * @param location the URL associated with an error (if any); may be null
         * @param details a string describing the error; must be non-null
         * @param cause the cause of the error; may be null
         */
        public ErrorNotification(String location, String details, Throwable cause) {
            if (details == null) throw new NullPointerException();

            this.location = location;
            this.details = details;
            this.cause = cause;
        }

        /**
         * Retrieves the URL associated with this error, if any.
         * For example, if there is a download or singing check error, this
         * will be the URL of the jar file that has the problem.
         * It may be null.
         *
         * @return the location, or null
         */
        public String getLocation() {
            return location;
        }

        /**
         * Retrieves the description of the error.
         * It may be the empty string, but is always non-null.
         *
         * @return the description of the error
         */
        public String getDetails() {
            return details;
        }

        /**
         * Retrieves the Exception or Error associated with this error
         * notification, if any. It may be null.
         *
         * @return the cause of the error, or null
         */
        public Throwable getCause() {
            return cause;
        }

        /**
         * Returns a string representation of this {@code ErrorNotification} object.
         * @return a string representation of this {@code ErrorNotification} object.
         */
        @Override public String toString() {
            StringBuilder str = new StringBuilder("Preloader.ErrorNotification: ");
            str.append(details);
            if (cause != null) {
                str.append(lineSeparator).append("Caused by: ").append(cause.toString());
            }
            if (location != null) {
                str.append(lineSeparator).append("Location: ").append(location);
            }
            return str.toString();
        }
    }

    /**
     * Preloader notification that reports progress. This is typically used to
     * report progress while downloading and initializing the application.
     * @since JavaFX 2.0
     */
    public static class ProgressNotification implements PreloaderNotification {
        private final double progress;
        private final String details;

        /**
         * Constructs a progress notification.
         *
         * @param progress a value indicating the progress.
         * A negative value for progress indicates that the progress is
         * indeterminate. A value between 0 and 1 indicates the amount
         * of progress. Any value greater than 1 is interpreted as 1.
         */
        public ProgressNotification(double progress) {
            this(progress, "");
        }

        // NOTE: We could consider exposing details in the future, but currently
        // have no plan to do so. This method is private for now.
        /**
         * Constructs a progress notification.
         *
         * @param progress a value indicating the progress.
         * A negative value for progress indicates that the progress is
         * indeterminate. A value between 0 and 1 indicates the amount
         * of progress. Any value greater than 1 is interpreted as 1.
         *
         * @param details the details of this notification
         */
        private ProgressNotification(double progress, String details) {
            this.progress = progress;
            this.details = details;
        }

        /**
         * Retrieves the progress for this notification. Progress is in the
         * range of 0 to 1, or is negative for indeterminate progress.
         *
         * @return the progress
         */
        public double getProgress() {
            return progress;
        }

        /**
         * Retrieves the details of the progress notification
         *
         * @return the details of this notification
         */
        private String getDetails() {
            return details;
        }
    }

    /**
     * A notification that signals a change in the application state.
     * A state change notification is sent to a preloader immediately prior
     * to loading
     * the application class (and constructing an instance), calling the
     * application init method, or calling the application start method.
     * @since JavaFX 2.0
     */
    public static class StateChangeNotification implements PreloaderNotification {

        /**
         * Enum that defines the type of change associated with this notification
         * @since JavaFX 2.0
         */
        public enum Type {
            /**
             * Indicates that the application class is about to be loaded and
             * constructed.
             */
            BEFORE_LOAD,

            /**
             * Indicates that the application's init method is about to be called.
             */
            BEFORE_INIT,

            /**
             * Indicates that the application's start method is about to be called.
             */
            BEFORE_START
        }

        private final Type notificationType;
        private final Application application;

        /**
         * Constructs a StateChangeNotification of the specified type.
         *
         * @param notificationType the type of this notification.
         */
        public StateChangeNotification(Type notificationType){
            this.notificationType = notificationType;
            this.application = null;
        }

        /**
         * Constructs an StateChangeNotification of the specified type for the
         * specified application.
         *
         * @param notificationType the type of this notification.
         * @param application the application instance associated with this
         * notification.
         */
        public StateChangeNotification(Type notificationType, Application application) {
            this.notificationType = notificationType;
            this.application = application;
        }

        /**
         * Returns the type of notification.
         *
         * @return one of: BEFORE_LOAD, BEFORE_INIT, BEFORE_START
         */
        public Type getType() {
            return notificationType;
        }

        /**
         * Returns the Application instance associated with this notification.
         * This is null for a BEFORE_LOAD notification and non-null for other
         * notification types.
         *
         * @return the Application instance or null.
         */
        public Application getApplication() {
            return application;
        }
    }

//    /**
//     * Used to signal about global modal dialogs to be shown that block
//     * application launch. In particular proxy and security dialogs
//     */
//    public static class UINotification implements PreloaderNotification {
//       //TODO RT-19601: implementation pending JRE work
//    }

}
