/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Screen;
import java.util.List;

/**
 * The FX to Windows pixel transform of {@code GlassScreen.cpp} - {@code GlassScreen::FX2Win} with its
 * {@code distSqTo} and {@code convert}, and the {@code Win2FX} it paired with - in Java. The C is gone:
 * {@code Win2FX} was deleted once nothing called it, and {@code FX2Win} with its monitor table after it.
 * <p>
 * {@code WinRobot} is the only caller: {@code Robot.cpp} ran every coordinate it received or returned
 * through {@code GlassScreen::FX2Win} or {@code Win2FX}, which read the monitor table {@code g_MonitorInfos}
 * that the JNI {@code WinApplication.staticScreen_getScreens} filled inside {@code glass.dll} - at start-up and again
 * after every {@code WM_DISPLAYCHANGE}, through {@code Screen.notifySettingsChanged}. Now
 * that the input synthesis is Java over {@code user32!SendInput} the transform has to be Java as well,
 * and the same numbers reach it through {@link Screen}: {@code rcMonitor}, the Windows monitor rect, is
 * {@link Screen#getPlatformX platformX/Y/Width/Height} and {@code fxMonitor} is
 * {@link Screen#getX x/y/width/height} - {@code CreateJavaMonitorFromMIS} passes exactly those two
 * rectangles to the {@code Screen} constructor, in table order.
 * <p>
 * The arithmetic is the C's, float for float: pick the monitor whose rectangle centre is nearest in
 * squared distance - strictly nearer, so the first monitor in table order wins a tie - then rescale
 * each axis linearly from that monitor's source rectangle to its destination rectangle. An empty
 * monitor table left the point unchanged and returned {@code FALSE}; here the equivalent is a
 * {@link Screen} list that is empty or not yet initialized.
 */
final class WinScreenTransform {

    private WinScreenTransform() {
    }

    /**
     * {@code GlassScreen::FX2Win}: FX coordinates to Windows pixels, in place.
     *
     * @param point {@code [x, y]}, replaced by the transformed pair
     * @return whether a monitor was found; when false the point is unchanged
     */
    static boolean fx2Win(float[] point) {
        return transform(screens(), true, point);
    }

    /**
     * {@code GlassScreen::Win2FX}: Windows pixels to FX coordinates, in place.
     *
     * @param point {@code [x, y]}, replaced by the transformed pair
     * @return whether a monitor was found; when false the point is unchanged
     */
    static boolean win2Fx(float[] point) {
        return transform(screens(), false, point);
    }

    /**
     * The screens in the order {@code WinApplication.staticScreen_getScreens} reported them, or an empty list before
     * graphics is initialized - which is the state the native table was in at the same moment, and where
     * both C functions returned {@code FALSE} without touching the point.
     */
    private static List<Screen> screens() {
        try {
            return Screen.getScreens();
        } catch (RuntimeException e) {
            // Screen.getScreens() throws until Screen.initScreens() has run.
            return List.of();
        }
    }

    /** The transform over an explicit screen list, so that it can be tested without a Glass application. */
    static boolean transform(List<Screen> screens, boolean fxToWindows, float[] point) {
        if (screens.isEmpty()) {
            return false;
        }
        int nearest = 0;
        float nearestDistance = distanceSquared(screens.get(0), fxToWindows, point[0], point[1]);
        for (int i = 1; i < screens.size(); i++) {
            float distance = distanceSquared(screens.get(i), fxToWindows, point[0], point[1]);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        convert(screens.get(nearest), fxToWindows, point);
        return true;
    }

    /** {@code distSqTo}: the squared float distance from the source rectangle's centre. */
    private static float distanceSquared(Screen screen, boolean fxToWindows, float x, float y) {
        int left = sourceLeft(screen, fxToWindows);
        int top = sourceTop(screen, fxToWindows);
        int right = left + sourceWidth(screen, fxToWindows);
        int bottom = top + sourceHeight(screen, fxToWindows);
        float relativeX = x - (left + right) * 0.5f;
        float relativeY = y - (top + bottom) * 0.5f;
        return relativeX * relativeX + relativeY * relativeY;
    }

    /** {@code convert}: the per-axis linear rescale from the source rectangle to the destination one. */
    private static void convert(Screen screen, boolean fxToWindows, float[] point) {
        int fromLeft = sourceLeft(screen, fxToWindows);
        int fromTop = sourceTop(screen, fxToWindows);
        int fromWidth = sourceWidth(screen, fxToWindows);
        int fromHeight = sourceHeight(screen, fxToWindows);
        int toLeft = sourceLeft(screen, !fxToWindows);
        int toTop = sourceTop(screen, !fxToWindows);
        int toWidth = sourceWidth(screen, !fxToWindows);
        int toHeight = sourceHeight(screen, !fxToWindows);
        float t = (point[0] - fromLeft) / fromWidth;
        point[0] = toLeft + t * toWidth;
        t = (point[1] - fromTop) / fromHeight;
        point[1] = toTop + t * toHeight;
    }

    private static int sourceLeft(Screen screen, boolean fxToWindows) {
        return fxToWindows ? screen.getX() : screen.getPlatformX();
    }

    private static int sourceTop(Screen screen, boolean fxToWindows) {
        return fxToWindows ? screen.getY() : screen.getPlatformY();
    }

    private static int sourceWidth(Screen screen, boolean fxToWindows) {
        return fxToWindows ? screen.getWidth() : screen.getPlatformWidth();
    }

    private static int sourceHeight(Screen screen, boolean fxToWindows) {
        return fxToWindows ? screen.getHeight() : screen.getPlatformHeight();
    }
}
