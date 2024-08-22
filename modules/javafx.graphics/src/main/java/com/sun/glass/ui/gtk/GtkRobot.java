/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.gtk.screencast.ScreencastHelper;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Screen;

import java.security.AccessController;
import java.security.PrivilegedAction;

final class GtkRobot extends GlassRobot {

    private static final String screenshotMethod;
    private static final String METHOD_GTK = "gtk";
    private static final String METHOD_SCREENCAST = "dbusScreencast";

    static {
        @SuppressWarnings("removal")
        boolean isOnWayland = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
            return waylandDisplay != null && !waylandDisplay.isBlank();
        });

        @SuppressWarnings("removal")
        String method = AccessController
                .doPrivileged((PrivilegedAction<String>) () ->
                        System.getProperty(
                                "javafx.robot.screenshotMethod",
                                isOnWayland ? METHOD_SCREENCAST : METHOD_GTK
                        ));
        screenshotMethod = method;
    }

    @Override
    public void create() {
        // no-op
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void keyPress(KeyCode code) {
        Application.checkEventThread();
        _keyPress(code.getCode());
    }

    protected native void _keyPress(int code);

    @Override
    public void keyRelease(KeyCode code) {
        Application.checkEventThread();
        _keyRelease(code.getCode());
    }

    protected native void _keyRelease(int code);

    public native void _mouseMove(int x, int y);

    @Override
    public void mouseMove(double x, double y) {
        Application.checkEventThread();
        _mouseMove((int) x, (int) y);
    }

    @Override
    public void mousePress(MouseButton... buttons) {
        Application.checkEventThread();
        _mousePress(GlassRobot.convertToRobotMouseButton(buttons));
    }

    protected native void _mousePress(int button);

    @Override
    public void mouseRelease(MouseButton... buttons) {
        Application.checkEventThread();
        _mouseRelease(GlassRobot.convertToRobotMouseButton(buttons));
    }

    protected native void _mouseRelease(int buttons);

    @Override
    public void mouseWheel(int wheelAmt) {
        Application.checkEventThread();
        _mouseWheel(wheelAmt);
    }

    protected native void _mouseWheel(int wheelAmt);

    @Override
    public double getMouseX() {
        Application.checkEventThread();
        return _getMouseX();
    }

    protected native int _getMouseX();

    @Override
    public double getMouseY() {
        Application.checkEventThread();
        return _getMouseY();
    }

    protected native int _getMouseY();

    @Override
    public Color getPixelColor(double x, double y) {
        Application.checkEventThread();
        Screen mainScreen = Screen.getMainScreen();
        x = (int) Math.floor((x + 0.5) * mainScreen.getPlatformScaleX());
        y = (int) Math.floor((y + 0.5) * mainScreen.getPlatformScaleY());
        int[] result = new int[1];
        if (METHOD_SCREENCAST.equals(screenshotMethod)) {
            ScreencastHelper.getRGBPixels((int) x, (int) y, 1, 1, result);
        } else {
            _getScreenCapture((int) x, (int) y, 1, 1, result);
        }
        return GlassRobot.convertFromIntArgb(result[0]);
    }

    protected native void _getScreenCapture(int x, int y, int width, int height, int[] data);

    @Override
    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        Application.checkEventThread();
        if (METHOD_SCREENCAST.equals(screenshotMethod)) {
            ScreencastHelper.getRGBPixels(x, y, width, height, data);
        } else {
            _getScreenCapture(x, y, width, height, data);
        }
    }
}
