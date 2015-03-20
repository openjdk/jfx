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
package com.sun.glass.ui.win;

import com.sun.glass.ui.*;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.utils.NativeLibLoader;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

final class WinApplication extends Application implements InvokeLaterDispatcher.InvokeLaterSubmitter {

    private static native void initIDs();
    static {
        // This loading of msvcr120.dll and msvcp120.dll (VS2013) is required when run with Java 8
        // since it was build with VS2010 and doesn't include msvcr120.dll in its JRE.
        // Note: See README-builds.html on MSVC requirement: VS2013 is required.
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                verbose = Boolean.getBoolean("javafx.verbose");
                try {
                    NativeLibLoader.loadLibrary("msvcr120");
                } catch (Throwable t) {
                    if (verbose) {
                        System.err.println("Error: failed to load msvcr120.dll : " + t);
                    }
                }
                try {
                    NativeLibLoader.loadLibrary("msvcp120");
                } catch (Throwable t) {
                    if (verbose) {
                        System.err.println("Error: failed to load msvcp120.dll : " + t);
                    }
                }
                Application.loadNativeLibrary();
                return null;
            }
        });
        initIDs();
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
    private native long _init();
    private native void _setClassLoader(ClassLoader classLoader);
    private native void _runLoop(Runnable launchable);
    private native void _terminateLoop();

    @Override
    protected void runLoop(final Runnable launchable) {
        boolean isEventThread = AccessController
            .doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.embed.isEventThread"));

        ClassLoader classLoader = WinApplication.class.getClassLoader();
        _setClassLoader(classLoader);

        if (isEventThread) {
            _init();
            setEventThread(Thread.currentThread());
            launchable.run();
            return;
        }
        final Thread toolkitThread =
            AccessController.doPrivileged((PrivilegedAction<Thread>) () -> new Thread(() -> {
                _init();
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
    public Pixels createPixels(int width, int height, IntBuffer data, float scale) {
        return new WinPixels(width, height, data, scale);
    }

    @Override protected int staticPixels_getNativeFormat() {
        return WinPixels.getNativeFormat_impl();
    }

    @Override public Robot createRobot() {
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
