/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.mac;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.*;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import java.security.AccessController;
import java.security.PrivilegedAction;

final class MacApplication extends Application implements InvokeLaterDispatcher.InvokeLaterSubmitter {

    private native static void _initIDs(boolean disableSyncRendering);
    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Application.loadNativeLibrary();
            return null;
        });
        boolean disableSyncRendering = AccessController
                .doPrivileged((PrivilegedAction<Boolean>) () ->
                        Boolean.getBoolean("glass.disableSyncRendering"));
        _initIDs(disableSyncRendering);
    }

    native static int _getMacKey(int code);

    private boolean isTaskbarApplication = false;
    private final InvokeLaterDispatcher invokeLaterDispatcher;

    MacApplication() {
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

    private Menu appleMenu;

    native void _runLoop(ClassLoader classLoader, Runnable launchable,
                         boolean isTaskbarApplication);
    @Override
    protected void runLoop(final Runnable launchable) {
        isTaskbarApplication =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                String taskbarAppProp = System.getProperty("glass.taskbarApplication");
                return  !"false".equalsIgnoreCase(taskbarAppProp);
            });

        ClassLoader classLoader = MacApplication.class.getClassLoader();
        _runLoop(classLoader, launchable, isTaskbarApplication);
    }

    native private void _finishTerminating();
    @Override
    protected void finishTerminating() {
        _finishTerminating();

        super.finishTerminating();
    }

    private void notifyApplicationDidTerminate() {
        setEventThread(null);
    }

    // Called from the native code
    private void setEventThread() {
        setEventThread(Thread.currentThread());
    }

    native private Object _enterNestedEventLoopImpl();
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

    native private void _leaveNestedEventLoopImpl(Object retValue);
    @Override protected void _leaveNestedEventLoop(Object retValue) {
        if (invokeLaterDispatcher != null) {
            invokeLaterDispatcher.notifyLeavingNestedEventLoop();
        }
        _leaveNestedEventLoopImpl(retValue);
    }

    native private void _hide();
    native private void _hideOtherApplications();
    native private void _unhideAllApplications();

    public void installAppleMenu(MenuBar menubar) {
        this.appleMenu = createMenu("Apple");

        MenuItem hideMenu = createMenuItem("Hide " + getName(), new MenuItem.Callback() {
            @Override public void action() {
                MacApplication.this._hide();
            }
            @Override public void validate() {
            }
        }, 'h', KeyEvent.MODIFIER_COMMAND);
        this.appleMenu.add(hideMenu);

        MenuItem hideOthersMenu = createMenuItem("Hide Others", new MenuItem.Callback() {
            @Override public void action() {
                MacApplication.this._hideOtherApplications();
            }
            @Override public void validate() {
            }
        }, 'h', KeyEvent.MODIFIER_COMMAND | KeyEvent.MODIFIER_ALT);
        this.appleMenu.add(hideOthersMenu);

        MenuItem unhideAllMenu = createMenuItem("Show All", new MenuItem.Callback() {
            @Override public void action() {
                MacApplication.this._unhideAllApplications();
            }
            @Override public void validate() {
            }
        });
        this.appleMenu.add(unhideAllMenu);

        this.appleMenu.add(MenuItem.Separator);

        MenuItem quitMenu = createMenuItem("Quit " + getName(), new MenuItem.Callback() {
            @Override public void action() {
                Application.EventHandler eh = getEventHandler();
                if (eh != null) {
                    eh.handleQuitAction(Application.GetApplication(), System.nanoTime());
                }
            }
            @Override public void validate() {
            }
        }, 'q', KeyEvent.MODIFIER_COMMAND);
        this.appleMenu.add(quitMenu);

        menubar.add(this.appleMenu);
    }

    public Menu getAppleMenu() {
        return this.appleMenu;
    }

    @Override public void installDefaultMenus(MenuBar menubar) {
        installAppleMenu(menubar);
    }


    // FACTORY METHODS

    @Override public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new MacWindow(owner, screen, styleMask);
    }

    final static long BROWSER_PARENT_ID = -1L;
    @Override public Window createWindow(long parent) {
        Window window = new MacWindow(parent);
        if (parent == BROWSER_PARENT_ID) {
            // Special case: a Mac embedded window, which is a parent to other child Windows.
            // Needs implicit view, with a layer that will be provided to the plugin
            window.setView(createView());
        }
        return window;
    }

    @Override public View createView() {
        return new MacView();
    }

    @Override public Cursor createCursor(int type) {
        return new MacCursor(type);
    }

    @Override public Cursor createCursor(int x, int y, Pixels pixels) {
        return new MacCursor(x, y, pixels);
    }

    @Override protected void staticCursor_setVisible(boolean visible) {
        MacCursor.setVisible_impl(visible);
    }

    @Override protected Size staticCursor_getBestSize(int width, int height) {
        return MacCursor.getBestSize_impl(width, height);
    }

    @Override public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new MacPixels(width, height, data);
    }

    @Override public Pixels createPixels(int width, int height, IntBuffer data) {
        return new MacPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        return new MacPixels(width, height, data, scalex, scaley);
    }

    @Override protected int staticPixels_getNativeFormat() {
        return MacPixels.getNativeFormat_impl();
    }

    @Override public GlassRobot createRobot() {
        return new MacRobot();
    }

    @Override native protected double staticScreen_getVideoRefreshPeriod();
    @Override native protected Screen[] staticScreen_getScreens();

    @Override public Timer createTimer(Runnable runnable) {
        return new MacTimer(runnable);
    }

    @Override protected int staticTimer_getMinPeriod() {
        return MacTimer.getMinPeriod_impl();
    }

    @Override protected int staticTimer_getMaxPeriod() {
        return MacTimer.getMaxPeriod_impl();
    }

    @Override public Accessible createAccessible() {
        return new MacAccessible();
    }

    @Override protected FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename, String title, int type,
                                                     boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        return MacCommonDialogs.showFileChooser_impl(owner, folder, filename,
                title, type, multipleMode, extensionFilters, defaultFilterIndex);
    }

    @Override protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        return MacCommonDialogs.showFolderChooser_impl(owner, folder, title);
    }

    @Override protected long staticView_getMultiClickTime() {
        return MacView.getMultiClickTime_impl();
    }

    @Override protected int staticView_getMultiClickMaxX() {
        return MacView.getMultiClickMaxX_impl();
    }

    @Override protected int staticView_getMultiClickMaxY() {
        return MacView.getMultiClickMaxY_impl();
    }

    @Override native protected void _invokeAndWait(Runnable runnable);

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

    @Override
    protected boolean _supportsInputMethods() {
        return true;
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return true;
    }

    @Override protected boolean _supportsUnifiedWindows() {
        return true;
    }

    @Override native protected boolean _supportsSystemMenu();

    native protected String _getRemoteLayerServerName();
    public String getRemoteLayerServerName() {
        return _getRemoteLayerServerName();
    }

    private native String _getDataDirectory();
    public String getDataDirectory() {
        checkEventThread();
        String baseDirectory = _getDataDirectory();
        if (baseDirectory == null || baseDirectory.length() == 0) {
            return super.getDataDirectory();
        }
        return baseDirectory + File.separator + name + File.separator;
    }

    @Override
    protected native int _getKeyCodeForChar(char c);
}
