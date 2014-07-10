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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import javafx.application.Preloader.PreloaderNotification;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.sun.javafx.application.LauncherImpl;
import com.sun.javafx.application.ParametersImpl;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.css.StyleManager;

/**
 * Application class from which JavaFX applications extend.
 *
 * <p><b>Life-cycle</b></p>
 * <p>
 * The entry point for JavaFX applications is the Application class. The
 * JavaFX runtime does the following, in order, whenever an application is
 * launched:
 * </p>
 * <ol>
 * <li>Constructs an instance of the specified Application class</li>
 * <li>Calls the {@link #init} method</li>
 * <li>Calls the {@link #start} method</li>
 * <li>Waits for the application to finish, which happens when either of
 * the following occur:
 * <ul>
 * <li>the application calls {@link Platform#exit}</li>
 * <li>the last window has been closed and the {@code implicitExit}
 * attribute on {@code Platform} is true</li>
 * </ul></li>
 * <li>Calls the {@link #stop} method</li>
 * </ol>
 * <p>Note that the {@code start} method is abstract and must be overridden.
 * The {@code init} and {@code stop} methods have concrete implementations
 * that do nothing.</p>
 *
 * <p>Calling {@link Platform#exit} is the preferred way to explicitly terminate
 * a JavaFX Application. Directly calling {@link System#exit} is
 * an acceptable alternative, but doesn't allow the Application {@link #stop}
 * method to run.
 * </p>
 *
 * <p>A JavaFX Application should not attempt to use JavaFX after the
 * FX toolkit has terminated or from a ShutdownHook, that is, after the
 * {@link #stop} method returns or {@link System#exit} is called.
 * </p>
 *
 * <p><b>Parameters</b></p>
 * <p>
 * Application parameters are available by calling the {@link #getParameters}
 * method from the {@link #init} method, or any time after the {@code init}
 * method has been called.
 * </p>
 *
 * <p><b>Threading</b></p>
 * <p>
 * JavaFX creates an application thread for running the application start
 * method, processing input events, and running animation timelines. Creation
 * of JavaFX {@link Scene} and {@link Stage} objects as well as modification of
 * scene graph operations to <em>live</em> objects (those objects already
 * attached to a scene) must be done on the JavaFX application thread.
 * </p>
 *
 * <p>
 * The Java launcher loads and initializes the specified Application class
 * on the JavaFX Application Thread. If there is no main method in the
 * Application class, or if the main method calls Application.launch(), then
 * an instance of the Application is then constructed on the JavaFX Application
 * Thread.
 * </p>
 *
 * <p>
 * The {@code init} method is called on the launcher thread, not on the
 * JavaFX Application Thread.
 * This means that an application must not construct a {@link Scene}
 * or a {@link Stage} in the {@code init} method.
 * An application may construct other JavaFX objects in the {@code init}
 * method.
 * </p>
 *
 * <p>
 * All the unhandled exceptions on the JavaFX application thread that occur during
 * event dispatching, running animation timelines, or any other code, are forwarded
 * to the thread's {@link java.lang.Thread.UncaughtExceptionHandler uncaught
 * exception handler}.
 * </p>
 *
 * <p><b>Example</b></p>
 * <p>The following example will illustrate a simple JavaFX application.</p>
 * <pre><code>
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class MyApp extends Application {
    public void start(Stage stage) {
        Circle circ = new Circle(40, 40, 30);
        Group root = new Group(circ);
        Scene scene = new Scene(root, 400, 300);

        stage.setTitle("My JavaFX Application");
        stage.setScene(scene);
        stage.show();
    }
}
 * </code></pre>
 *
 * <p>The above example will produce the following:</p>
 * <p><img src="doc-files/Application.png"/></p>
 * @since JavaFX 2.0
 */
public abstract class Application {
    /**
     * Constant for user agent stylesheet for the "Caspian" theme. Caspian
     * is the theme that shipped as default in JavaFX 2.x.
     * @since JavaFX 8.0
     */
    public static final String STYLESHEET_CASPIAN = "CASPIAN";
    /**
     * Constant for user agent stylesheet for the "Modena" theme. Modena
     * is the default theme for JavaFX 8.x.
     * @since JavaFX 8.0
     */
    public static final String STYLESHEET_MODENA = "MODENA";

    /**
     * Launch a standalone application. This method is typically called
     * from the main method(). It must not be called more than once or an
     * exception will be thrown.
     *
     * <p>
     * The launch method does not return until the application has exited,
     * either via a call to Platform.exit or all of the application windows
     * have been closed.
     *
     * <p>
     * Typical usage is:
     * <ul>
     * <pre>
     * public static void main(String[] args) {
     *     Application.launch(MyApp.class, args);
     * }
     * </pre>
     * </ul>
     * where <code>MyApp</code> is a subclass of Application.
     *
     * @param appClass the application class that is constructed and executed
     *        by the launcher.
     * @param args the command line arguments passed to the application.
     *             An application may get these parameters using the
     *             {@link #getParameters()} method.
     *
     * @throws IllegalStateException if this method is called more than once.
     * @throws IllegalArgumentException if <code>appClass</code> is not a
     *         subclass of <code>Application</code>.
     */
    public static void launch(Class<? extends Application> appClass, String... args) {
        LauncherImpl.launchApplication(appClass, args);
    }

    /**
     * Launch a standalone application. This method is typically called
     * from the main method(). It must not be called more than once or an
     * exception will be thrown.
     * This is equivalent to launch(TheClass.class, args) where TheClass is the
     * immediately enclosing class of the method that called launch. It must
     * be a subclass of Application or a RuntimeException will be thrown.
     *
     * <p>
     * The launch method does not return until the application has exited,
     * either via a call to Platform.exit or all of the application windows
     * have been closed.
     *
     * <p>
     * Typical usage is:
     * <ul>
     * <pre>
     * public static void main(String[] args) {
     *     Application.launch(args);
     * }
     * </pre>
     * </ul>
     *
     * @param args the command line arguments passed to the application.
     *             An application may get these parameters using the
     *             {@link #getParameters()} method.
     *
     * @throws IllegalStateException if this method is called more than once.
     */
    public static void launch(String... args) {
        // Figure out the right class to call
        StackTraceElement[] cause = Thread.currentThread().getStackTrace();

        boolean foundThisMethod = false;
        String callingClassName = null;
        for (StackTraceElement se : cause) {
            // Skip entries until we get to the entry for this class
            String className = se.getClassName();
            String methodName = se.getMethodName();
            if (foundThisMethod) {
                callingClassName = className;
                break;
            } else if (Application.class.getName().equals(className)
                    && "launch".equals(methodName)) {

                foundThisMethod = true;
            }
        }

        if (callingClassName == null) {
            throw new RuntimeException("Error: unable to determine Application class");
        }

        try {
            Class theClass = Class.forName(callingClassName, true,
                               Thread.currentThread().getContextClassLoader());
            if (Application.class.isAssignableFrom(theClass)) {
                Class<? extends Application> appClass = theClass;
                LauncherImpl.launchApplication(appClass, args);
            } else {
                throw new RuntimeException("Error: " + theClass
                        + " is not a subclass of javafx.application.Application");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Constructs a new {@code Application} instance.
     */
    public Application() {
    }

    /**
     * The application initialization method. This method is called immediately
     * after the Application class is loaded and constructed. An application may
     * override this method to perform initialization prior to the actual starting
     * of the application.
     *
     * <p>
     * The implementation of this method provided by the Application class does nothing.
     * </p>
     *
     * <p>
     * NOTE: This method is not called on the JavaFX Application Thread. An
     * application must not construct a Scene or a Stage in this
     * method.
     * An application may construct other JavaFX objects in this method.
     * </p>
     */
    public void init() throws Exception {
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     *
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set. The primary stage will be embedded in
     * the browser if the application was launched as an applet.
     * Applications may create other stages, if needed, but they will not be
     * primary stages and will not be embedded in the browser.
     */
    public abstract void start(Stage primaryStage) throws Exception;

    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     *
     * <p>
     * The implementation of this method provided by the Application class does nothing.
     * </p>
     *
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     */
    public void stop() throws Exception {
    }

    private HostServices hostServices = null;

    /**
     * Gets the HostServices provider for this application. This provides
     * the ability to get the code base and document base for this application,
     * and to access the enclosing web page.
     *
     * @return the HostServices provider
     */
    public final HostServices getHostServices() {
        synchronized (this) {
            if (hostServices == null) {
                hostServices = new HostServices(this);
            }
            return hostServices;
        }
    }

    /**
     * Retrieves the parameters for this Application, including any arguments
     * passed on the command line and any parameters specified in a JNLP file
     * for an applet or WebStart application.
     *
     * <p>
     * NOTE: this method should not be called from the Application constructor,
     * as it will return null. It may be called in the init() method or any
     * time after that.
     * </p>
     *
     * @return the parameters for this Application, or null if called from the
     * constructor.
     */
    public final Parameters getParameters() {
        return ParametersImpl.getParameters(this);
    }

    /**
     * Notifies the preloader with an application-generated notification.
     * Application code calls this method with a PreloaderNotification that is
     * delivered to the preloader's handleApplicationNotification() method.
     * This is primarily useful for cases where an application wants the
     * preloader to show progress during a long application initialization
     * step.
     *
     * @param info the application-generated preloader notification
     */
    public final void notifyPreloader(PreloaderNotification info) {
        LauncherImpl.notifyPreloader(this, info);
    }

    /**
     * Encapsulates the set of parameters for an application. This includes
     * arguments passed on the command line, unnamed parameters specified
     * in a JNLP file, and &lt;name,value&gt; pairs specified in a JNLP file.
     *
     * <p>
     * Note that the application and the preloader both get the same set
     * of parameters for a given run of an application.
     * </p>
     * @since JavaFX 2.0
     */
    public static abstract class Parameters {

        /**
         * Constructs a new {@code Parameters} instance.
         */
        public Parameters() {
        }

        /**
         * Retrieves a read-only list of the raw arguments. This list
         * may be empty, but is never null. In the case of a standalone
         * application, it is the ordered list of arguments specified on the
         * command line. In the case of an applet or WebStart application,
         * it includes unnamed parameters as well as named parameters. For
         * named parameters, each &lt;name,value&gt; pair is represented as
         * a single argument of the form: "--name=value".
         *
         * @return a read-only list of raw application arguments
         */
        public abstract List<String> getRaw();

        /**
         * Retrieves a read-only list of the unnamed parameters. This list
         * may be empty, but is never null. The named parameters, that is
         * the parameters that are represented as &lt;name,value&gt; pairs, are
         * filtered out.
         *
         * @return a read-only list of unnamed parameters.
         */
        public abstract List<String> getUnnamed();

        /**
         * Retrieves a read-only map of the named parameters. It may be
         * empty, but is never null.
         * Named parameters include those &lt;name,value&gt; pairs explicitly
         * specified in a JNLP file. It also includes any command line
         * arguments of the form: "--name=value".
         *
         * @return a read-only map of named parameters.
         */
        public abstract Map<String, String> getNamed();

    }

    private static String userAgentStylesheet = null;

    /**
     * Get the user agent stylesheet used by the whole application. This is
     * used to provide default styling for all ui controls and other nodes.
     * A value of null means the platform default stylesheet is being used.
     * <p>
     * NOTE: This method must be called on the JavaFX Application Thread.
     * </p>
     *
     * @return The URL to the stylesheet as a String.
     * @since JavaFX 8.0
     */
    public static String getUserAgentStylesheet() {
        return userAgentStylesheet;
    }

    /**
     * Set the user agent stylesheet used by the whole application. This is used
     * to provide default styling for all ui controls and other nodes. Each
     * release of JavaFX may have a new default value for this so if you need
     * to guarantee consistency you will need to call this method and choose
     * what default you would like for your application. A value of null will
     * restore the platform default stylesheet. This property can also be set
     * on the command line with {@code -Djavafx.userAgentStylesheetUrl=[URL]}
     * Setting it on the command line overrides anything set using this method
     * in code.
     * <p>
     * NOTE: This method must be called on the JavaFX Application Thread.
     * </p>
     *
     *
     * @param url The URL to the stylesheet as a String.
     * @since JavaFX 8.0
     */
    public static void setUserAgentStylesheet(String url) {
        userAgentStylesheet = url;
        if (url == null) {
            PlatformImpl.setDefaultPlatformUserAgentStylesheet();
        } else {
            PlatformImpl.setPlatformUserAgentStylesheet(url);
        }
    }
}
