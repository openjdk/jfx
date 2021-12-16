/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Optional;

public abstract class Application {

    private final static String DEFAULT_NAME = "java";
    protected String name = DEFAULT_NAME;

    public static class EventHandler {
        // currently used only on Mac OS X
        public void handleWillFinishLaunchingAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleDidFinishLaunchingAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleWillBecomeActiveAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleDidBecomeActiveAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleWillResignActiveAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleDidResignActiveAction(Application app, long time) {
        }
        // currently used only on iOS
        public void handleDidReceiveMemoryWarning(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleWillHideAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleDidHideAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleWillUnhideAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        public void handleDidUnhideAction(Application app, long time) {
        }
        // currently used only on Mac OS X
        // the open files which started up the app will arrive before app becomes active
        public void handleOpenFilesAction(Application app, long time, String files[]) {
        }
        // currently used only on Mac OS X
        public void handleQuitAction(Application app, long time) {
        }
        public boolean handleThemeChanged(String themeName) {
            return false;
        }
    }

    private EventHandler eventHandler;
    private boolean initialActiveEventReceived = false;
    private String initialOpenedFiles[] = null;

    private static boolean loaded = false;
    private static Application application;
    private static Thread eventThread;
    @SuppressWarnings("removal")
    private static final boolean disableThreadChecks =
        AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            final String str =
                    System.getProperty("glass.disableThreadChecks", "false");
            return "true".equalsIgnoreCase(str);
        });

    // May be called on any thread.
    protected static synchronized void loadNativeLibrary(final String libname) {
        // load the native library of the specified libname.
        // the platform default by convention is "glass", all others should have a suffix, ie glass-x11
        if (!loaded) {
            com.sun.glass.utils.NativeLibLoader.loadLibrary(libname);
            loaded = true;
        }
    }

    // May be called on any thread.
    protected static synchronized void loadNativeLibrary() {
        // use the "platform default" name of "glass"
        loadNativeLibrary("glass");
    }

    private static volatile Map deviceDetails = null;

    // provides a means for the user to pass platorm specific details
    // to the native glass impl. Can be null.
    // May need be called before Run.
    // May be called on any thread.
    public static void setDeviceDetails(Map details) {
        deviceDetails = details;
    }

    // May be called on any thread.
    public static Map getDeviceDetails() {
        return deviceDetails;
    }

    protected Application() {
    }

    // May be called on any thread.
    public static void run(final Runnable launchable) {
        if (application != null) {
            throw new IllegalStateException("Application is already running");
        }
        application = PlatformFactory.getPlatformFactory().createApplication();
        // each concrete Application should set the app name using its own platform mechanism:
        // on Mac OS X - use NSBundle info, which can be overriden by -Xdock:name
        // on Windows - TODO
        // on Linux - TODO
        //application.name = DEFAULT_NAME; // default
        try {
            application.runLoop(() -> {
                Screen.initScreens();
                launchable.run();
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // runLoop never exits until app terminates
    protected abstract void runLoop(Runnable launchable);

    // should return after loop termination completion
    protected void finishTerminating() {
        // To make sure application object is not used outside of the run loop
        application = null;
        // The eventThread is null at this point, no need to check it
    }

    /**
     * Gets the name for the application.  The application name may
     * be used to identify the application in the user interface or
     * as part of the platform specific path used to store application
     * data.
     *
     * This is a hint and may not be used on some platforms.
     *
     * @return the application name
     */
    public String getName() {
        checkEventThread();
        return name;
    }

    /**
     * Sets the name for the application.  The application name may
     * be used to identify the application in the user interface or
     * as part of the platform specific path used to store application
     * data.
     *
     * The name could be set only once. All subsequent calls are ignored.
     *
     * This is a hint and may not be used on some platforms.
     *
     * @param name the new application name
     */
    public void setName(String name) {
        checkEventThread();
        if (name != null && DEFAULT_NAME.equals(this.name)) {
            this.name = name;
        }
    }

    /**
     * Gets a platform specific path that can be used to store
     * application data.  The application name typically appears
     * as part of the path.
     *
     * On some platforms, the path may not yet exist and the caller
     * will need to create it.
     *
     * @return the platform specific path for the application data
     */
    public String getDataDirectory() {
        checkEventThread();
        @SuppressWarnings("removal")
        String userHome = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("user.home"));
        return userHome + File.separator + "." + name + File.separator;
    }

    // Subclasses can override the following notify methods.
    // Overridden methods need to call super.

    protected void notifyWillFinishLaunching() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleWillFinishLaunchingAction(this, System.nanoTime());
        }
    }

    protected void notifyDidFinishLaunching() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleDidFinishLaunchingAction(this, System.nanoTime());
        }
    }

    protected void notifyWillBecomeActive() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleWillBecomeActiveAction(this, System.nanoTime());
        }
    }

    protected void notifyDidBecomeActive() {
        this.initialActiveEventReceived = true;
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleDidBecomeActiveAction(this, System.nanoTime());
        }
    }

    protected void notifyWillResignActive() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleWillResignActiveAction(this, System.nanoTime());
        }
    }

    protected boolean notifyThemeChanged(String themeName) {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            return handler.handleThemeChanged(themeName);
        }
        return false;
    }

    protected void notifyDidResignActive() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleDidResignActiveAction(this, System.nanoTime());
        }
    }

    protected void notifyDidReceiveMemoryWarning() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleDidReceiveMemoryWarning(this, System.nanoTime());
        }
    }

    protected void notifyWillHide() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleWillHideAction(this, System.nanoTime());
        }
    }

    protected void notifyDidHide() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleDidHideAction(this, System.nanoTime());
        }
    }

    protected void notifyWillUnhide() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleWillUnhideAction(this, System.nanoTime());
        }
    }

    protected void notifyDidUnhide() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleDidUnhideAction(this, System.nanoTime());
        }
    }

    // notificiation when user drag and drops files onto app icon
    protected void notifyOpenFiles(String files[]) {
        if ((this.initialActiveEventReceived == false) && (this.initialOpenedFiles == null)) {
            // rememeber the initial opened files
            this.initialOpenedFiles = files;
        }
        EventHandler handler = getEventHandler();
        if ((handler != null) && (files != null)) {
            handler.handleOpenFilesAction(this, System.nanoTime(), files);
        }
    }

    protected void notifyWillQuit() {
        EventHandler handler = getEventHandler();
        if (handler != null) {
            handler.handleQuitAction(this, System.nanoTime());
        }
    }

    /**
     * Install app's default native menus:
     * on Mac OS X - Apple menu (showing the app name) with a single Quit menu item
     * on Windows - NOP
     * on Linux - NOP
     */
    public void installDefaultMenus(MenuBar menubar) {
        checkEventThread();
        // To override in subclasses
    }

    public EventHandler getEventHandler() {
        //checkEventThread(); // Glass (Mac)
        // When an app is closing, Mac calls notify- Will/DidHide, Will/DidResignActive
        // on a thread other than the Main thread
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        checkEventThread();
        boolean resendOpenFiles = ((this.eventHandler != null) && (this.initialOpenedFiles != null));
        this.eventHandler = eventHandler;
        if (resendOpenFiles == true) {
            // notify the new event handler with initial opened files
            notifyOpenFiles(this.initialOpenedFiles);
    }
    }

    private boolean terminateWhenLastWindowClosed = true;
    public final boolean shouldTerminateWhenLastWindowClosed() {
        checkEventThread();
        return terminateWhenLastWindowClosed;
    }
    public final void setTerminateWhenLastWindowClosed(boolean b) {
        checkEventThread();
        terminateWhenLastWindowClosed = b;
    }

    public boolean shouldUpdateWindow() {
        checkEventThread();
        return false; // overridden in platform application class
    }

    public boolean hasWindowManager() {
        //checkEventThread(); // Prism (Mac)
        return true; // overridden in platform application class
    }

    /**
     * Notifies the Application that rendering has completed for current pulse.
     *
     * This is called on the render thread.
     */
    public void notifyRenderingFinished() {
    }

    public void terminate() {
        checkEventThread();
        try {
                final List<Window> windows = new LinkedList<>(Window.getWindows());
                for (Window window : windows) {
                    // first make windows invisible
                    window.setVisible(false);
                }
                for (Window window : windows) {
                    // now we can close windows
                    window.close();
                }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            finishTerminating();
        }
    }

    // May be called on any thread
    static public Application GetApplication() {
        return Application.application;
    }

    // May be called on any thread
    protected static void setEventThread(Thread thread) {
        Application.eventThread = thread;
    }

    // May be called on any thread
    protected static Thread getEventThread() {
        return Application.eventThread;
    }

    /**
     * Returns {@code true} if the current thread is the event thread.
     */
    public static boolean isEventThread() {
        return Thread.currentThread() == Application.eventThread;
    }

    /**
     * Verifies that the current thread is the event thread, and throws
     * an exception if this is not so.
     *
     * The check can be disabled by setting the "glass.disableThreadChecks"
     * system property. It is preferred, however, to fix the application code
     * instead.
     *
     * @throws IllegalStateException if the current thread is not the event thread
     */
    public static void checkEventThread() {
        //TODO: we do NOT advertise the "glass.disableThreadChecks".
        //      If we never get a complaint about this check, we can consider
        //      dropping the system property and perform the check unconditionally
        if (!disableThreadChecks &&
                Thread.currentThread() != Application.eventThread)
        {
            throw new IllegalStateException(
                    "This operation is permitted on the event thread only; currentThread = "
                    + Thread.currentThread().getName());

        }
    }

    // Called from native, when a JNI exception has occurred
    public static void reportException(Throwable t) {
        Thread currentThread = Thread.currentThread();
        Thread.UncaughtExceptionHandler handler =
                currentThread.getUncaughtExceptionHandler();
        handler.uncaughtException(currentThread, t);
    }

    abstract protected void _invokeAndWait(java.lang.Runnable runnable);
    /**
     * Block the current thread and wait until the given  runnable finishes
     * running on the native event loop thread.
     */
    public static void invokeAndWait(java.lang.Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (isEventThread()) {
            runnable.run();
        } else {
            GetApplication()._invokeAndWait(runnable);
        }
    }

    abstract protected void _invokeLater(java.lang.Runnable runnable);
    /**
     * Schedule the given runnable to run on the native event loop thread
     * some time in the future, and return immediately.
     */
    public static void invokeLater(java.lang.Runnable runnable) {
        if (runnable == null) {
            return;
        }
        GetApplication()._invokeLater(runnable);
    }

    protected abstract Object _enterNestedEventLoop();
    protected abstract void _leaveNestedEventLoop(Object retValue);

    private static int nestedEventLoopCounter = 0;

    /**
     * Starts a nested event loop.
     *
     * Calling this method temporarily blocks processing of the current event,
     * and starts a nested event loop to handle other native events.  To
     * proceed with the blocked execution path, the application should call the
     * {@link #leaveNestedEventLoop(Object)} method.
     *
     * Note that this method may only be invoked on the main (event handling)
     * thread.
     *
     * An application may enter several nested loops recursively. There's no
     * limit of recursion other than that imposed by the native stack size.
     *
     * @return an object passed to the leaveNestedEventLoop() method
     * @throws RuntimeException if the current thread is not the main thread
     */
    static Object enterNestedEventLoop() {
        checkEventThread();

        nestedEventLoopCounter++;
        try {
            return GetApplication()._enterNestedEventLoop();
        } finally {
            nestedEventLoopCounter--;
        }
    }

    /**
     * Terminates the current nested event loop.
     *
     * After calling this method and returning from the current event handler,
     * the execusion returns to the point where the {@link #enterNestedEventLoop}
     * was called previously. You may specify a return value for the
     * enterNestedEventLoop() method by passing the argument {@code retValue} to
     * the leaveNestedEventLoop().
     *
     * Note that this method may only be invoked on the main (event handling)
     * thread.
     *
     * @throws RuntimeException if the current thread is not the main thread
     * @throws IllegalStateException if the application hasn't started a nested
     *                               event loop
     */
    static void leaveNestedEventLoop(Object retValue) {
        checkEventThread();

        if (nestedEventLoopCounter == 0) {
            throw new IllegalStateException("Not in a nested event loop");
        }

        GetApplication()._leaveNestedEventLoop(retValue);
    }

    public static boolean isNestedLoopRunning() {
        checkEventThread();
        return nestedEventLoopCounter > 0;
    }

    //TODO: move to the EventHandler
    public void menuAboutAction() {
        System.err.println("about");
    }


    // FACTORY METHODS

    /**
     * Create a window.
     *
     * The styleMask argument is a bitmask of window styles as defined in the
     * Window class.  Note, however, that visual kinds (UNTITLED, TITLED,
     * or TRANSPARENT) can't be combined together.  Also, functional types
     * (NORMAL, POPUP, or UTILITY) can't be combined together.  A window is
     * allowed to be of exactly one visual kind, and exactly one functional
     * type.
     */
    public abstract Window createWindow(Window owner, Screen screen, int styleMask);

    /**
     * Create a window.
     *
     * The styleMask argument is a bitmask of window styles as defined in the
     * Window class.  Note, however, that visual kinds (UNTITLED, TITLED,
     * or TRANSPARENT) can't be combined together.  Also, functional types
     * (NORMAL, POPUP, or UTILITY) can't be combined together.  A window is
     * allowed to be of exactly one visual kind, and exactly one functional
     * type.
     */
    public final Window createWindow(Screen screen, int styleMask) {
        return createWindow(null, screen, styleMask);
    }

    public abstract View createView();

    public abstract Cursor createCursor(int type);
    public abstract Cursor createCursor(int x, int y, Pixels pixels);

    protected abstract void staticCursor_setVisible(boolean visible);
    protected abstract Size staticCursor_getBestSize(int width, int height);

    public final Menu createMenu(String title) {
        return new Menu(title);
    }

    public final Menu createMenu(String title, boolean enabled) {
        return new Menu(title, enabled);
    }

    public final MenuBar createMenuBar() {
        return new MenuBar();
    }

    public final MenuItem createMenuItem(String title) {
        return createMenuItem(title, null);
    }

    public final MenuItem createMenuItem(String title, MenuItem.Callback callback) {
        return createMenuItem(title, callback, KeyEvent.VK_UNDEFINED, KeyEvent.MODIFIER_NONE);
    }

    public final MenuItem createMenuItem(String title, MenuItem.Callback callback,
            int shortcutKey, int shortcutModifiers) {
        return createMenuItem(title, callback, shortcutKey, shortcutModifiers, null);
    }

    public final MenuItem createMenuItem(String title, MenuItem.Callback callback,
            int shortcutKey, int shortcutModifiers, Pixels pixels) {
        return new MenuItem(title, callback, shortcutKey, shortcutModifiers, pixels);
    }

    public abstract Pixels createPixels(int width, int height, ByteBuffer data);
    public abstract Pixels createPixels(int width, int height, IntBuffer data);
    public abstract Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley);
    protected abstract int staticPixels_getNativeFormat();

    /* utility method called from native code */
    static Pixels createPixels(int width, int height, int[] data, float scalex, float scaley) {
        return Application.GetApplication().createPixels(width, height, IntBuffer.wrap(data), scalex, scaley);
    }

    /* utility method called from native code */
    static float getScaleFactor(final int x, final int y, final int w, final int h) {
        float scale = 0.0f;
        // Find the maximum scale for screens this area overlaps
        for (Screen s : Screen.getScreens()) {
            final int sx = s.getX(), sy = s.getY(), sw = s.getWidth(), sh = s.getHeight();
            if (x < (sx + sw) && (x + w) > sx && y < (sy + sh) && (y + h) > sy) {
                if (scale < s.getRecommendedOutputScaleX()) {
                    scale = s.getRecommendedOutputScaleX();
                }
                if (scale < s.getRecommendedOutputScaleY()) {
                    scale = s.getRecommendedOutputScaleY();
                }
            }
        }
        return scale == 0.0f ? 1.0f : scale;
    }


    public abstract GlassRobot createRobot();

    protected abstract double staticScreen_getVideoRefreshPeriod();
    protected abstract Screen[] staticScreen_getScreens();

    public abstract Timer createTimer(Runnable runnable);
    protected abstract int staticTimer_getMinPeriod();
    protected abstract int staticTimer_getMaxPeriod();

    public final EventLoop createEventLoop() {
        return new EventLoop();
    }

    public Accessible createAccessible() { return null; }

    protected abstract FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename, String title, int type,
                                                     boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex);

    protected abstract File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title);

    protected abstract long staticView_getMultiClickTime();
    protected abstract int staticView_getMultiClickMaxX();
    protected abstract int staticView_getMultiClickMaxY();

    public String getHighContrastScheme(String themeName) {
        return themeName;
    }

    /**
     * Gets the Name of the currently active high contrast theme.
     * If null, then high contrast is not enabled.
     */
    public String getHighContrastTheme() {
        checkEventThread();
        return null;
    }

    protected boolean _supportsInputMethods() {
        // Overridden in subclasses
        return false;
    }
    public final boolean supportsInputMethods() {
        checkEventThread();
        return _supportsInputMethods();
    }

    protected abstract boolean _supportsTransparentWindows();
    public final boolean supportsTransparentWindows() {
        checkEventThread();
        return _supportsTransparentWindows();
    }

    public boolean hasTwoLevelFocus() {
        return false;
    }

    public boolean hasVirtualKeyboard() {
        return false;
    }

    public boolean hasTouch() {
        return false;
    }

    public boolean hasMultiTouch() {
        return false;
    }

    public boolean hasPointer() {
        return true;
    }

    protected abstract boolean _supportsUnifiedWindows();
    public final boolean supportsUnifiedWindows() {
        checkEventThread();
        return _supportsUnifiedWindows();
    }

    protected boolean _supportsSystemMenu() {
        // Overridden in subclasses
        return false;
    }
    public final boolean supportsSystemMenu() {
        checkEventThread();
        return _supportsSystemMenu();
    }

    protected abstract int _getKeyCodeForChar(char c);
    /**
     * Returns a VK_ code of a key capable of producing the given unicode
     * character with respect to the currently active keyboard layout or
     * VK_UNDEFINED if the character isn't present in the current layout.
     *
     * @param c the character
     * @return integer code for the given char
     */
    public static int getKeyCodeForChar(char c) {
        return application._getKeyCodeForChar(c);
    }

    protected int _isKeyLocked(int keyCode) {
        // Overridden in subclasses
        return KeyEvent.KEY_LOCK_UNKNOWN;
    }

    public final Optional<Boolean> isKeyLocked(int keyCode) {
        checkEventThread();
        int lockState = _isKeyLocked(keyCode);
        switch (lockState) {
            case KeyEvent.KEY_LOCK_OFF:
                return Optional.of(false);
            case KeyEvent.KEY_LOCK_ON:
                return Optional.of(true);
            default:
                return Optional.empty();
        }
    }
}
