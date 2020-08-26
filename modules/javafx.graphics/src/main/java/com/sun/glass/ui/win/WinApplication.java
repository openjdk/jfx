/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import com.sun.glass.ui.*;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.prism.impl.PrismSettings;
import com.sun.javafx.tk.Toolkit;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class WinApplication extends Application implements InvokeLaterDispatcher.InvokeLaterSubmitter {
    static float   overrideUIScale;

    private static boolean getBoolean(String propname, boolean defval, String description) {
        String str = System.getProperty(propname);
        if (str == null) {
            str = System.getenv(propname);
        }
        if (str == null) {
            return defval;
        }
        Boolean ret = Boolean.parseBoolean(str);
        if (PrismSettings.verbose) {
            System.out.println((ret ? "" : "not ")+description);
        }
        return ret;
    }

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

    private static native void initIDs(float overrideUIScale);
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                verbose = Boolean.getBoolean("javafx.verbose");
                if (PrismSettings.allowHiDPIScaling) {
                    overrideUIScale = getFloat("glass.win.uiScale", -1.0f, "Forcing UI scaling factor: ");
                    // We only parse these if verbose, to inform the user...
                    if (PrismSettings.verbose) {
                        getFloat("glass.win.renderScale", -1.0f,
                                 "(No longer supported) Rendering scaling factor: ");
                        getFloat("glass.win.minHiDPI", 1.5f,
                                 "(No longer supported) UI scaling threshold: ");
                        getBoolean("glass.win.forceIntegerRenderScale", true,
                                   "(No longer supported) force integer rendering scale");
                    }
                } else {
                    overrideUIScale = 1.0f;
                }
                // Load required Microsoft runtime DLLs on Windows platforms
                Toolkit.loadMSWindowsLibraries();
                Application.loadNativeLibrary();
                return null;
            }
        });
        initIDs(overrideUIScale);
    }

    private final InvokeLaterDispatcher invokeLaterDispatcher;
    WinApplication() {
        // Embedded in SWT, with shared event thread
        boolean isEventThread = AccessController
                .doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.embed.isEventThread"));
        if (!isEventThread) {
            invokeLaterDispatcher = new InvokeLaterDispatcher(this);
            invokeLaterDispatcher.start();
        } else {
            invokeLaterDispatcher = null;
        }
    }

    private static boolean verbose;

    // returng toolkit window HWND
    private native long _init(int awarenessRequested);
    private native void _setClassLoader(ClassLoader classLoader);
    private native void _runLoop(Runnable launchable);
    private native void _terminateLoop();

    private static final int Process_DPI_Unaware            = 0;
    private static final int Process_System_DPI_Aware       = 1;
    private static final int Process_Per_Monitor_DPI_Aware  = 2;

    private static int getDesiredAwarenesslevel() {
        if (!PrismSettings.allowHiDPIScaling) {
            return Process_DPI_Unaware;
        }
        String awareRequested = AccessController
            .doPrivileged((PrivilegedAction<String>) () ->
                          System.getProperty("javafx.glass.winDPIawareness"));
        if (awareRequested != null) {
            awareRequested = awareRequested.toLowerCase();
            if (awareRequested.equals("aware")) {
                return Process_System_DPI_Aware;
            } else if (awareRequested.equals("permonitor")) {
                return Process_Per_Monitor_DPI_Aware;
            } else {
                if (!awareRequested.equals("unaware")) {
                    System.err.println("unrecognized DPI awareness request, defaulting to unaware: "+awareRequested);
                }
                return Process_DPI_Unaware;
            }
        }
        return Process_Per_Monitor_DPI_Aware;
    }

    @Override
    protected void runLoop(final Runnable launchable) {
        boolean isEventThread = AccessController
            .doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.embed.isEventThread"));
        int awareness = getDesiredAwarenesslevel();

        ClassLoader classLoader = WinApplication.class.getClassLoader();
        _setClassLoader(classLoader);

        if (isEventThread) {
            _init(awareness);
            setEventThread(Thread.currentThread());
            launchable.run();
            return;
        }
        final Thread toolkitThread =
            AccessController.doPrivileged((PrivilegedAction<Thread>) () -> new Thread(() -> {
                _init(awareness);
                _runLoop(launchable);
            }, "WindowsNativeRunloopThread"));
        setEventThread(toolkitThread);
        toolkitThread.start();
    }

    @Override protected void finishTerminating() {
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

    native private Object _enterNestedEventLoopImpl();
    native private void _leaveNestedEventLoopImpl(Object retValue);

    @Override protected Object _enterNestedEventLoop() {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        try {
            return _enterNestedEventLoopImpl();
        } finally {
            if (invokeLaterDispatcher != null) {
                invokeLaterDispatcher.notifyLeftNestedEventLoop();
            }
        }
    }

    @Override protected void _leaveNestedEventLoop(Object retValue) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyLeavingNestedEventLoop();
        }
        _leaveNestedEventLoopImpl(retValue);
    }

    // FACTORY METHODS

    @Override public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new WinWindow(owner, screen, styleMask);
    }

    @Override public Window createWindow(long parent) {
        return new WinChildWindow(parent);
    }

    @Override public View createView() {
        return new WinView();
    }

    @Override public Cursor createCursor(int type) {
        return new WinCursor(type);
    }

    @Override public Cursor createCursor(int x, int y, Pixels pixels) {
        return new WinCursor(x, y, pixels);
    }

    @Override protected void staticCursor_setVisible(boolean visible) {
        WinCursor.setVisible_impl(visible);
    }

    @Override protected Size staticCursor_getBestSize(int width, int height) {
        return WinCursor.getBestSize_impl(width, height);
    }

    @Override public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new WinPixels(width, height, data);
    }

    @Override public Pixels createPixels(int width, int height, IntBuffer data) {
        return new WinPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        return new WinPixels(width, height, data, scalex, scaley);
    }

    @Override protected int staticPixels_getNativeFormat() {
        return WinPixels.getNativeFormat_impl();
    }

    @Override public GlassRobot createRobot() {
        return new WinRobot();
    }

    @Override protected double staticScreen_getVideoRefreshPeriod() {
        return 0.0;     // indicate millisecond resolution
    }

    @Override native protected Screen[] staticScreen_getScreens();

    @Override public Timer createTimer(Runnable runnable) {
        return new WinTimer(runnable);
    }

    @Override protected int staticTimer_getMinPeriod() {
        return WinTimer.getMinPeriod_impl();
    }

    @Override protected int staticTimer_getMaxPeriod() {
        return WinTimer.getMaxPeriod_impl();
    }

    @Override public Accessible createAccessible() {
        return new WinAccessible();
    }

    @Override protected FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename, String title, int type,
                                             boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        return WinCommonDialogs.showFileChooser_impl(owner, folder, filename, title, type, multipleMode, extensionFilters, defaultFilterIndex);
    }

    @Override protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyEnteringNestedEventLoop();
        }
        return WinCommonDialogs.showFolderChooser_impl(owner, folder, title);
    }

    @Override protected long staticView_getMultiClickTime() {
        return WinView.getMultiClickTime_impl();
    }

    @Override protected int staticView_getMultiClickMaxX() {
        return WinView.getMultiClickMaxX_impl();
    }

    @Override protected int staticView_getMultiClickMaxY() {
        return WinView.getMultiClickMaxY_impl();
    }

    @Override native protected void _invokeAndWait(Runnable runnable);

    native private void _submitForLaterInvocation(Runnable r);
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

    private native String _getHighContrastTheme();
    @Override public String getHighContrastTheme() {
        checkEventThread();
        return _getHighContrastTheme();
    }

    @Override
    protected boolean _supportsInputMethods() {
        return true;
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return true;
    }

    @Override native protected boolean _supportsUnifiedWindows();

    public String getDataDirectory() {
        checkEventThread();
        String baseDirectory = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getenv("APPDATA"));
        if (baseDirectory == null || baseDirectory.length() == 0) {
            return super.getDataDirectory();
        }
        return baseDirectory + File.separator + name + File.separator;
    }

    @Override
    protected native int _getKeyCodeForChar(char c);
}
