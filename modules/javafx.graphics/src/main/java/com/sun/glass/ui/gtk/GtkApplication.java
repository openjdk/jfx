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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.InvokeLaterDispatcher;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.javafx.util.Logging;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.prism.impl.PrismSettings;
import com.sun.javafx.logging.PlatformLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.lang.annotation.Native;

final class GtkApplication extends Application implements
                                    InvokeLaterDispatcher.InvokeLaterSubmitter {
    private static final String SWT_INTERNAL_CLASS =
            "org.eclipse.swt.internal.gtk.OS";
    private static final int forcedGtkVersion;


    static  {
        //check for SWT-GTK lib presence
        @SuppressWarnings("removal")
        Class<?> OS = AccessController.
                doPrivileged((PrivilegedAction<Class<?>>) () -> {
                    try {
                        return Class.forName(SWT_INTERNAL_CLASS, true,
                                ClassLoader.getSystemClassLoader());
                    } catch (Exception e) {}
                    try {
                        return Class.forName(SWT_INTERNAL_CLASS, true,
                                Thread.currentThread().getContextClassLoader());
                    } catch (Exception e) {}
                    return null;
                });
        if (OS != null) {
            PlatformLogger logger = Logging.getJavaFXLogger();
            logger.fine("SWT-GTK library found. Try to obtain GTK version.");
            @SuppressWarnings("removal")
            Method method = AccessController.
                    doPrivileged((PrivilegedAction<Method>) () -> {
                        try {
                            return OS.getMethod("gtk_major_version");
                        } catch (Exception e) {
                            return null;
                        }
                    });
            int ver = 0;
            if (method != null) {
                try {
                    ver = ((Number)method.invoke(OS)).intValue();
                } catch (Exception e) {
                    logger.warning("Method gtk_major_version() of " +
                         "the org.eclipse.swt.internal.gtk.OS class " +
                         "returns error. SWT GTK version cannot be detected. " +
                         "GTK3 will be used as default.");
                    ver = 3;
                }
            }
            if (ver < 2 || ver > 3) {
                logger.warning("SWT-GTK uses unsupported major GTK version "
                        + ver + ". GTK3 will be used as default.");
                ver = 3;
            }
            forcedGtkVersion = ver;
        } else {
            forcedGtkVersion = 0;
        }

        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Application.loadNativeLibrary();
            return null;
        });
    }

    public static  int screen = -1;
    public static  long display = 0;
    public static  long visualID = 0;

    static float overrideUIScale;

    private final InvokeLaterDispatcher invokeLaterDispatcher;

    private static float getFloat(String propname, float defval, String description) {
        String str = System.getProperty(propname);
        if (str == null) {
            str = System.getenv(propname);
        }
        if (str == null) {
            return defval;
        }
        str = str.trim();
        float val;
        if (str.endsWith("%")) {
            val = Integer.parseInt(str.substring(0, str.length()-1)) / 100.0f;
        } else if (str.endsWith("DPI") || str.endsWith("dpi")) {
            val = Integer.parseInt(str.substring(0, str.length()-3)) / 96.0f;
        } else {
            val = Float.parseFloat(str);
        }
        if (PrismSettings.verbose) {
            System.out.println(description+val);
        }
        return val;
    }

    GtkApplication() {

        @SuppressWarnings("removal")
        final int gtkVersion = forcedGtkVersion == 0 ?
            AccessController.doPrivileged((PrivilegedAction<Integer>) () -> {
                String v = System.getProperty("jdk.gtk.version","3");
                int ret = 0;
                if ("3".equals(v) || v.startsWith("3.")) {
                    ret = 3;
                } else if ("2".equals(v) || v.startsWith("2.")) {
                    ret = 2;
                }
                return ret;
            }) : forcedGtkVersion;
        @SuppressWarnings("removal")
        boolean gtkVersionVerbose =
                AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            return Boolean.getBoolean("jdk.gtk.verbose");
        });
        if (PrismSettings.allowHiDPIScaling) {
            @SuppressWarnings("removal")
            float tmp = AccessController.doPrivileged((PrivilegedAction<Float>) () ->
                    getFloat("glass.gtk.uiScale", -1.0f, "Forcing UI scaling factor: "));
            overrideUIScale = tmp;
        } else {
            overrideUIScale = -1.0f;
        }

        int libraryToLoad = _queryLibrary(gtkVersion, gtkVersionVerbose);

        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (libraryToLoad == QUERY_NO_DISPLAY) {
                throw new UnsupportedOperationException("Unable to open DISPLAY");
            } else if (libraryToLoad == QUERY_USE_CURRENT) {
                if (gtkVersionVerbose) {
                    System.out.println("Glass GTK library to load is already loaded");
                }
            } else if (libraryToLoad == QUERY_LOAD_GTK2) {
                if (gtkVersionVerbose) {
                    System.out.println("Glass GTK library to load is glassgtk2");
                }
                NativeLibLoader.loadLibrary("glassgtk2");
            } else if (libraryToLoad == QUERY_LOAD_GTK3) {
                if (gtkVersionVerbose) {
                    System.out.println("Glass GTK library to load is glassgtk3");
                }
                NativeLibLoader.loadLibrary("glassgtk3");
            } else {
                throw new UnsupportedOperationException("Internal Error");
            }
            return null;
        });

        int version = _initGTK(gtkVersion, gtkVersionVerbose, overrideUIScale);

        if (version == -1) {
            throw new RuntimeException("Error loading GTK libraries");
        }

        // Embedded in SWT, with shared event thread
        @SuppressWarnings("removal")
        boolean isEventThread = AccessController
                .doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.embed.isEventThread"));
        if (!isEventThread) {
            invokeLaterDispatcher = new InvokeLaterDispatcher(this);
            invokeLaterDispatcher.start();
        } else {
            invokeLaterDispatcher = null;
        }
    }

    @Native private static final int QUERY_ERROR = -2;
    @Native private static final int QUERY_NO_DISPLAY = -1;
    @Native private static final int QUERY_USE_CURRENT = 1;
    @Native private static final int QUERY_LOAD_GTK2 = 2;
    @Native private static final int QUERY_LOAD_GTK3 = 3;
    /*
     * check the system and return an indication of which library to load
     *  return values are the QUERY_ constants
     */
    private static native int _queryLibrary(int version, boolean verbose);

    private static native int _initGTK(int version, boolean verbose, float overrideUIScale);

    private void initDisplay() {
        Map ds = getDeviceDetails();
        if (ds != null) {
            Object value;
            value = ds.get("XDisplay");
            if (value != null) {
                display = (Long)value;
            }
            value = ds.get("XVisualID");
            if (value != null) {
                visualID = (Long)value;
            }
            value = ds.get("XScreenID");
            if (value != null) {
                screen = (Integer)value;
            }
        }
    }

    private void init() {
        initDisplay();
        long eventProc = 0;
        Map map = getDeviceDetails();
        if (map != null) {
            Long result = (Long) map.get("javafx.embed.eventProc");
            eventProc = result == null ? 0 : result;
        }

        @SuppressWarnings("removal")
        final boolean disableGrab = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("sun.awt.disablegrab") ||
               Boolean.getBoolean("glass.disableGrab"));

        _init(eventProc, disableGrab);
    }

    @Override
    protected void runLoop(final Runnable launchable) {
        // Embedded in SWT, with shared event thread
        @SuppressWarnings("removal")
        final boolean isEventThread = AccessController
            .doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.embed.isEventThread"));

        if (isEventThread) {
            init();
            setEventThread(Thread.currentThread());
            launchable.run();
            return;
        }

        @SuppressWarnings("removal")
        final boolean noErrorTrap = AccessController
            .doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("glass.noErrorTrap"));

        @SuppressWarnings("removal")
        final Thread toolkitThread =
            AccessController.doPrivileged((PrivilegedAction<Thread>) () -> new Thread(() -> {
                init();
                _runLoop(launchable, noErrorTrap);
            }, "GtkNativeMainLoopThread"));
        setEventThread(toolkitThread);
        toolkitThread.start();
    }

    @Override
    protected void finishTerminating() {
        final Thread toolkitThread = getEventThread();
        if (toolkitThread != null) {
            _terminateLoop();
            setEventThread(null);
        }
        super.finishTerminating();
    }

    @Override public boolean shouldUpdateWindow() {
        return true;
    }

    private native void _terminateLoop();

    private native void _init(long eventProc, boolean disableGrab);

    private native void _runLoop(Runnable launchable, boolean noErrorTrap);

    @Override
    protected void _invokeAndWait(final Runnable runnable) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.invokeAndWait(runnable);
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            submitForLaterInvocation(() -> {
                if (runnable != null) runnable.run();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                //FAIL SILENTLY
            }
        }
    }

    private native void _submitForLaterInvocation(Runnable r);
    // InvokeLaterDispatcher.InvokeLaterSubmitter
    @Override public void submitForLaterInvocation(Runnable r) {
        _submitForLaterInvocation(r);
    }

    @Override protected void _invokeLater(Runnable runnable) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.invokeLater(runnable);
        } else {
            submitForLaterInvocation(runnable);
        }
    }

    private Object eventLoopExitEnterPassValue;

    private native void enterNestedEventLoopImpl();

    private native void leaveNestedEventLoopImpl();

    @Override
    protected Object _enterNestedEventLoop() {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        try {
            enterNestedEventLoopImpl();
            final Object retValue = eventLoopExitEnterPassValue;
            eventLoopExitEnterPassValue = null;
            return retValue;
        } finally {
            if (invokeLaterDispatcher != null) {
                invokeLaterDispatcher.notifyLeftNestedEventLoop();
            }
        }
    }

    @Override
    protected void _leaveNestedEventLoop(Object retValue) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyLeavingNestedEventLoop();
        }
        eventLoopExitEnterPassValue = retValue;
        leaveNestedEventLoopImpl();
    }

    @Override
    public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new GtkWindow(owner, screen, styleMask);
    }

    @Override
    public Window createWindow(long parent) {
        return new GtkChildWindow(parent);
    }

    @Override
    public View createView() {
        return new GtkView();
    }

    @Override
    public Cursor createCursor(int type) {
        return new GtkCursor(type);
    }

    @Override
    public Cursor createCursor(int x, int y, Pixels pixels) {
        return new GtkCursor(x, y, pixels);
    }

    @Override
    protected void staticCursor_setVisible(boolean visible) {
    }

    @Override
    protected Size staticCursor_getBestSize(int width, int height) {
        return GtkCursor._getBestSize(width, height);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new GtkPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data) {
        return new GtkPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        return new GtkPixels(width, height, data, scalex, scaley);
    }

    @Override
    protected int staticPixels_getNativeFormat() {
        return Pixels.Format.BYTE_BGRA_PRE; // TODO
    }

    @Override
    public GlassRobot createRobot() {
        return new GtkRobot();
    }

    @Override
    public Timer createTimer(Runnable runnable) {
        return new GtkTimer(runnable);
    }

    @Override
    protected native int staticTimer_getMinPeriod();

    @Override
    protected native int staticTimer_getMaxPeriod();

    @Override protected double staticScreen_getVideoRefreshPeriod() {
        return 0.0;     // indicate millisecond resolution
    }

    @Override native protected Screen[] staticScreen_getScreens();

    @Override
    protected FileChooserResult staticCommonDialogs_showFileChooser(
            Window owner, String folder, String filename, String title,
            int type, boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {

        return GtkCommonDialogs.showFileChooser(owner, folder, filename, title,
                type, multipleMode, extensionFilters, defaultFilterIndex);
    }

    @Override
    protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        return GtkCommonDialogs.showFolderChooser(owner, folder, title);
    }

    @Override
    protected native long staticView_getMultiClickTime();

    @Override
    protected native int staticView_getMultiClickMaxX();

    @Override
    protected native int staticView_getMultiClickMaxY();

    @Override
    protected boolean _supportsInputMethods() {
        return true;
    }

    @Override
    protected native boolean _supportsTransparentWindows();

    @Override protected boolean _supportsUnifiedWindows() {
        return false;
    }

    @Override
    protected native int _getKeyCodeForChar(char c);

    @Override
    protected native int _isKeyLocked(int keyCode);

}
