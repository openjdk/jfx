/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.glass.ui.gtk.screencast.XdgDesktopPortal;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Screen;

/**
 * The GTK robot. Its XTest, GDK and XKB calls are bound directly by {@link GtkGlassNative}, where
 * {@code GlassRobot.cpp} wrapped them in JNI up to commit {@code 033187ad90}.
 */
final class GtkRobot extends GlassRobot {

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
        if (XdgDesktopPortal.isRemoteDesktop() && ScreencastHelper.isAvailable()) {
            ScreencastHelper.remoteDesktopKey(true, code.getCode());
        } else {
            _keyPress(code.getCode());
        }
    }

    protected void _keyPress(int code) {
        GtkGlassNative.robotKeyPress(code);
    }

    @Override
    public void keyRelease(KeyCode code) {
        Application.checkEventThread();
        if (XdgDesktopPortal.isRemoteDesktop() && ScreencastHelper.isAvailable()) {
            ScreencastHelper.remoteDesktopKey(false, code.getCode());
        } else {
            _keyRelease(code.getCode());
        }
    }

    protected void _keyRelease(int code) {
        GtkGlassNative.robotKeyRelease(code);
    }

    public void _mouseMove(int x, int y) {
        GtkGlassNative.robotMouseMove(x, y);
    }

    @Override
    public void mouseMove(double x, double y) {
        Application.checkEventThread();
        _mouseMove((int) x, (int) y);
        if (XdgDesktopPortal.isRemoteDesktop() && ScreencastHelper.isAvailable()) {
            // We still call _mouseMove on purpose to change the mouse position
            // within the XWayland server so that we can retrieve it later.
            ScreencastHelper.remoteDesktopMouseMove((int) x, (int) y);
        }
    }

    @Override
    public void mousePress(MouseButton... buttons) {
        Application.checkEventThread();
        if (XdgDesktopPortal.isRemoteDesktop() && ScreencastHelper.isAvailable()) {
            ScreencastHelper.remoteDesktopMouseButton(true, GlassRobot.convertToRobotMouseButton(buttons));
        } else {
            _mousePress(GlassRobot.convertToRobotMouseButton(buttons));
        }
    }

    protected void _mousePress(int button) {
        GtkGlassNative.robotMousePress(button);
    }

    @Override
    public void mouseRelease(MouseButton... buttons) {
        Application.checkEventThread();
        if (XdgDesktopPortal.isRemoteDesktop() && ScreencastHelper.isAvailable()) {
            ScreencastHelper.remoteDesktopMouseButton(false, GlassRobot.convertToRobotMouseButton(buttons));
        } else {
            _mouseRelease(GlassRobot.convertToRobotMouseButton(buttons));
        }
    }

    protected void _mouseRelease(int buttons) {
        GtkGlassNative.robotMouseRelease(buttons);
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        Application.checkEventThread();
        if (XdgDesktopPortal.isRemoteDesktop() && ScreencastHelper.isAvailable()) {
            ScreencastHelper.remoteDesktopMouseWheel(wheelAmt);
        } else {
            _mouseWheel(wheelAmt);
        }
    }

    protected void _mouseWheel(int wheelAmt) {
        GtkGlassNative.robotMouseWheel(wheelAmt);
    }

    @Override
    public double getMouseX() {
        Application.checkEventThread();
        return _getMouseX();
    }

    protected int _getMouseX() {
        return GtkGlassNative.robotGetMouseX();
    }

    @Override
    public double getMouseY() {
        Application.checkEventThread();
        return _getMouseY();
    }

    protected int _getMouseY() {
        return GtkGlassNative.robotGetMouseY();
    }

    @Override
    public Color getPixelColor(double x, double y) {
        Application.checkEventThread();
        Screen mainScreen = Screen.getMainScreen();
        x = (int) Math.floor((x + 0.5) * mainScreen.getPlatformScaleX());
        y = (int) Math.floor((y + 0.5) * mainScreen.getPlatformScaleY());
        int[] result = new int[1];
        if ((XdgDesktopPortal.isScreencast()
                || XdgDesktopPortal.isRemoteDesktop()) && ScreencastHelper.isAvailable()) {
            ScreencastHelper.getRGBPixels((int) x, (int) y, 1, 1, result);
        } else {
            _getScreenCapture((int) x, (int) y, 1, 1, result);
        }
        return GlassRobot.convertFromIntArgb(result[0]);
    }

    protected void _getScreenCapture(int x, int y, int width, int height, int[] data) {
        GtkGlassNative.robotGetScreenCapture(x, y, width, height, data);
    }

    @Override
    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        Application.checkEventThread();
        if ((XdgDesktopPortal.isScreencast()
                || XdgDesktopPortal.isRemoteDesktop()) && ScreencastHelper.isAvailable()) {
            ScreencastHelper.getRGBPixels(x, y, width, height, data);
        } else {
            _getScreenCapture(x, y, width, height, data);
        }
    }
}
