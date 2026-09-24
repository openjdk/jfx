/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;

/**
 * MS Windows platform implementation class for Robot.
 * <p>
 * The ten former {@code native} methods are gone: eight of them synthesised input through
 * {@code user32!SendInput} or {@code user32!mouse_event}, which {@link WinGlassNative} now calls
 * directly, and the two screen reads go through the {@code gwin_robot_*} C ABI of {@code glass.dll}.
 * What stays here is what the JNI entry points did on the Java side of the boundary: the
 * {@code Application.checkEventThread()} of every public method, the {@code (int)} truncation of the
 * incoming FX coordinates, the half-pixel offset the C added to them, and the FX to Windows transform
 * that {@code Robot.cpp} took from {@code GlassScreen} and {@link WinScreenTransform} now computes
 * from {@link com.sun.glass.ui.Screen}.
 */
final class WinRobot extends GlassRobot {

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
        WinGlassNative.robotKeyPress(code.getCode());
    }

    @Override
    public void keyRelease(KeyCode code) {
        Application.checkEventThread();
        WinGlassNative.robotKeyRelease(code.getCode());
    }

    @Override
    public void mouseMove(double x, double y) {
        Application.checkEventThread();
        float[] point = {(int) x + 0.5f, (int) y + 0.5f};
        WinScreenTransform.fx2Win(point);
        WinGlassNative.robotMouseMove(point[0], point[1]);
    }

    @Override
    public void mousePress(MouseButton... buttons) {
        Application.checkEventThread();
        WinGlassNative.robotMousePress(GlassRobot.convertToRobotMouseButton(buttons));
    }

    @Override
    public void mouseRelease(MouseButton... buttons) {
        Application.checkEventThread();
        WinGlassNative.robotMouseRelease(GlassRobot.convertToRobotMouseButton(buttons));
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        Application.checkEventThread();
        WinGlassNative.robotMouseWheel(wheelAmt);
    }

    @Override
    public double getMouseX() {
        Application.checkEventThread();
        return mouseLocation()[0];
    }

    @Override
    public double getMouseY() {
        Application.checkEventThread();
        return mouseLocation()[1];
    }

    /**
     * The cursor position in FX coordinates. Each accessor asks for it separately, as the two JNI
     * entry points did: {@code _getMouseX} and {@code _getMouseY} called {@code GetCursorPos}
     * themselves, so a caller reading both could see the cursor move in between.
     */
    private static float[] mouseLocation() {
        int[] cursor = WinGlassNative.cursorPosition();
        float[] point = {cursor[0] + 0.5f, cursor[1] + 0.5f};
        WinScreenTransform.win2Fx(point);
        return point;
    }

    @Override
    public Color getPixelColor(double x, double y) {
        Application.checkEventThread();
        float[] point = {(int) x + 0.5f, (int) y + 0.5f};
        WinScreenTransform.fx2Win(point);
        return GlassRobot.convertFromIntArgb(WinGlassNative.robotPixelColorAt((int) point[0], (int) point[1]));
    }

    @Override
    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        Application.checkEventThread();
        // x and y are Windows pixels already; the status is ignored, exactly as the JNI ignored every
        // GDI failure and handed Java whatever the buffer held.
        WinGlassNative.robotCapture(x, y, width, height, data);
    }
}
