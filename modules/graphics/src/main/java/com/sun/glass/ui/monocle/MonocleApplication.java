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
package com.sun.glass.ui.monocle;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Robot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.monocle.input.InputDevice;
import com.sun.glass.ui.monocle.input.MouseInput;
import com.sun.glass.ui.monocle.input.MouseState;
import javafx.collections.SetChangeListener;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class MonocleApplication extends Application {

    private final NativePlatform platform =
            NativePlatformFactory.getNativePlatform();
    private final RunnableProcessor runnableProcessor = platform.getRunnableProcessor();

    /** Bit to indicate that a device has touch support */
    private static final int DEVICE_TOUCH = 0;
    /** Bit to indicate that a device has multitouch support */
    private static final int DEVICE_MULTITOUCH = 1;
    /** Bit to indicate that a device has relative motion pointer support */
    private static final int DEVICE_POINTER = 2;
    /** Bit to indicate that a device has arrow keys and a select key */
    private static final int DEVICE_5WAY = 3;
    /** Bit to indicate that a device has a full PC keyboard */
    private static final int DEVICE_PC_KEYBOARD = 4;
    /** Largest bit used in device capability bitmasks */
    private static final int DEVICE_MAX = 4;
    /** A running count of the numbers of devices with each device capability */
    private int[] deviceFlags = new int[DEVICE_MAX + 1];

    private Runnable renderEndNotifier = new Runnable() {
        public void run() {
            platform.getScreen().swapBuffers();
        }
    };

    MonocleApplication() {
        for (InputDevice device : platform.getInputDeviceRegistry().getInputDevices()) {
            updateDeviceFlags(device, true);
        }
        platform.getInputDeviceRegistry().getInputDevices().addListener(
                new SetChangeListener<InputDevice>() {
            @Override
            public void onChanged(
                    Change<? extends InputDevice> change) {
                if (change.wasAdded()) {
                    InputDevice device = change.getElementAdded();
                    updateDeviceFlags(device, true);
                } else if (change.wasRemoved()) {
                    InputDevice device = change.getElementRemoved();
                    updateDeviceFlags(device, false);
                }
            }
        });
    }

    private void updateDeviceFlags(InputDevice device, boolean added) {
        int modifier = added ? 1 : -1;
        if (device.isTouch()) {
            deviceFlags[DEVICE_TOUCH] += modifier;
        }
        if (device.isMultiTouch()) {
            deviceFlags[DEVICE_MULTITOUCH] += modifier;
        }
        if (device.isRelative()) {
            deviceFlags[DEVICE_POINTER] += modifier;
        }
        if (device.isFullKeyboard()) {
            deviceFlags[DEVICE_PC_KEYBOARD] += modifier;
        }
        if (device.is5Way()) {
            deviceFlags[DEVICE_5WAY] += modifier;
        }
    }

    @Override
    protected void runLoop(Runnable launchable) {
        runnableProcessor.invokeLater(launchable);
        Thread t = new Thread(runnableProcessor);
        setEventThread(t);
        t.start();
    }

    @Override
    protected void _invokeAndWait(Runnable runnable) {
        runnableProcessor.invokeAndWait(runnable);
    }

    @Override
    protected void _invokeLater(Runnable runnable) {
        runnableProcessor.invokeLater(runnable);
    }

    @Override
    protected Object _enterNestedEventLoop() {
        return runnableProcessor.enterNestedEventLoop();
    }

    @Override
    protected void _leaveNestedEventLoop(Object retValue) {
        runnableProcessor.leaveNestedEventLoop(retValue);
    }

    @Override
    public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new MonocleWindow(owner, screen, styleMask);
    }

    @Override
    public Window createWindow(long parent) {
        return new MonocleWindow(parent);
    }

    @Override
    public View createView() {
        return new MonocleView();
    }

    @Override
    public Cursor createCursor(int type) {
        return new MonocleCursor(type);
    }

    @Override
    public Cursor createCursor(int x, int y, Pixels pixels) {
        return new MonocleCursor(x, y, pixels);
    }

    @Override
    protected void staticCursor_setVisible(boolean visible) {
        NativeCursor cursor = NativePlatformFactory.getNativePlatform().getCursor();
        cursor.setVisibility(visible);
    }

    @Override
    protected Size staticCursor_getBestSize(int width, int height) {
        NativeCursor cursor = NativePlatformFactory.getNativePlatform().getCursor();
        return cursor.getBestSize();
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new MonoclePixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data) {
        return new MonoclePixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data,
                               float scale) {
        return new MonoclePixels(width, height, data, scale);
    }

    @Override
    protected int staticPixels_getNativeFormat() {
        return platform.getScreen().getNativeFormat();
    }

    @Override
    public Robot createRobot() {
        return new MonocleRobot();
    }

    @Override
    protected double staticScreen_getVideoRefreshPeriod() {
        return 0.0;
    }

    @Override
    protected Screen[] staticScreen_getScreens() {
        Screen screen = null;
        try {
            NativeScreen ns = platform.getScreen();
            Constructor c = AccessController.doPrivileged(
                    new PrivilegedAction<Constructor>() {
                        @Override
                        public Constructor run() {
                            try {
                                Constructor c = Screen.class.getDeclaredConstructor(
                                        Long.TYPE,
                                        Integer.TYPE,
                                        Integer.TYPE, Integer.TYPE,
                                        Integer.TYPE, Integer.TYPE,
                                        Integer.TYPE, Integer.TYPE,
                                        Integer.TYPE, Integer.TYPE,
                                        Integer.TYPE, Integer.TYPE, Float.TYPE);
                                c.setAccessible(true);
                                return c;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    });
            if (c != null) {
                screen = (Screen) c.newInstance(
                        1l, // dummy native pointer;
                        ns.getDepth(),
                        0, 0, ns.getWidth(), ns.getHeight(),
                        0, 0, ns.getWidth(), ns.getHeight(),
                        ns.getDPI(), ns.getDPI(),
                        1.0f);
                // Move the cursor to the middle of the screen
                MouseState mouseState = new MouseState();
                mouseState.setX(ns.getWidth() / 2);
                mouseState.setY(ns.getHeight() / 2);
                MouseInput.getInstance().setState(mouseState, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        return new Screen[] { screen };
    }

    @Override
    public Timer createTimer(Runnable runnable) {
        return new MonocleTimer(runnable);
    }

    @Override
    protected int staticTimer_getMinPeriod() {
        return MonocleTimer.getMinPeriod_impl();
    }

    @Override
    protected int staticTimer_getMaxPeriod() {
        return MonocleTimer.getMaxPeriod_impl();
    }

    public boolean hasWindowManager() {
        return false;
    }

    @Override
    protected FileChooserResult staticCommonDialogs_showFileChooser(
            Window owner, String folder, String filename, String title,
            int type, boolean multipleMode,
            ExtensionFilter[] extensionFilters,
            int defaultFilterIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected File staticCommonDialogs_showFolderChooser(Window owner,
                                                         String folder,
                                                         String title) {
        Thread.dumpStack();
        throw new UnsupportedOperationException();
    }

    @Override
    protected long staticView_getMultiClickTime() {
        return MonocleView._getMultiClickTime();
    }

    @Override
    protected int staticView_getMultiClickMaxX() {
        return MonocleView._getMultiClickMaxX();
    }

    @Override
    protected int staticView_getMultiClickMaxY() {
        return MonocleView._getMultiClickMaxY();
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return true;
    }

    @Override
    protected boolean _supportsUnifiedWindows() {
        return false;
    }

    @Override
    public boolean hasTwoLevelFocus() {
        return deviceFlags[DEVICE_PC_KEYBOARD] == 0 && deviceFlags[DEVICE_5WAY] > 0;
    }

    @Override
    public boolean hasVirtualKeyboard() {
        return deviceFlags[DEVICE_PC_KEYBOARD] == 0 && deviceFlags[DEVICE_TOUCH] > 0;
    }

    @Override
    public boolean hasTouch() {
        return deviceFlags[DEVICE_TOUCH] > 0;
    }

    @Override
    public boolean hasMultiTouch() {
        return deviceFlags[DEVICE_MULTITOUCH] > 0;
    }

    @Override
    public boolean hasPointer() {
        return deviceFlags[DEVICE_POINTER] > 0;
    }

    @Override
    public void notifyRenderingFinished() {
        invokeLater(renderEndNotifier);
    }

    @Override
    protected void finishTerminating() {
        setEventThread(null);
        platform.shutdown();
        super.finishTerminating();
    }

}
