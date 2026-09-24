/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.TouchInputSupport;
import com.sun.glass.ui.GestureSupport;
import com.sun.glass.ui.View;

final class WinGestureSupport {

    /**
     * Runs this class's static initializer, which builds the {@link GestureSupport} and
     * {@link TouchInputSupport} below and nothing else. {@code WinApplication.runLoop} calls it on the
     * toolkit thread before the launchable runs, because the initializer must run there -
     * {@code TouchInputSupport}'s constructor calls {@code Application.checkEventThread()} - and must
     * have run before the first {@code WM_TOUCH}: under JNI, {@code GlassApplication::ClassForName}
     * initialised this class eagerly inside {@code GlassWindow::Create}, and once the
     * {@code GwinGestureCallbacks} table is what delivers touch and gesture events, nothing in C does
     * that any more. Without this call the first touch event would initialise the class inside an
     * upcall, and a failure in the initializer would surface there as "touch does nothing" instead of
     * at startup.
     */
    static void ensureInitialized() {
    }

    // The multiplier used to convert scroll units to pixels
    private static final double multiplier = 1.0;

    private final static GestureSupport gestures = new GestureSupport(true);
    private final static TouchInputSupport touches =
            new TouchInputSupport(gestures.createTouchCountListener(), true);
    private static int modifiers;
    private static boolean isDirect;

    public static void notifyBeginTouchEvent(View view, int modifiers,
                                             boolean isDirect,
                                             int touchEventCount) {
        touches.notifyBeginTouchEvent(view, modifiers, isDirect, touchEventCount);
    }

    public static void notifyNextTouchEvent(View view, int state, long id, int x,
                                            int y, int xAbs, int yAbs) {
        touches.notifyNextTouchEvent(view, state, id, x, y, xAbs, yAbs);
    }

    public static void notifyEndTouchEvent(View view) {
        touches.notifyEndTouchEvent(view);
        gestureFinished(view, touches.getTouchCount(), false);
    }

    private static void gestureFinished(View view, int touchCount,
                                        boolean isInertia) {
        if (gestures.isScrolling() && touchCount == 0) {
            gestures.handleScrollingEnd(view, modifiers, touchCount, isDirect,
                                        isInertia,
                                        View.GESTURE_NO_VALUE,
                                        View.GESTURE_NO_VALUE,
                                        View.GESTURE_NO_VALUE,
                                        View.GESTURE_NO_VALUE);
        }

        if (gestures.isRotating() && touchCount < 2) {
            gestures.handleRotationEnd(view, modifiers, isDirect, isInertia,
                                       View.GESTURE_NO_VALUE,
                                       View.GESTURE_NO_VALUE,
                                       View.GESTURE_NO_VALUE,
                                       View.GESTURE_NO_VALUE);
        }

        if (gestures.isZooming() && touchCount < 2) {
            gestures.handleZoomingEnd(view, modifiers, isDirect, isInertia,
                                      View.GESTURE_NO_VALUE,
                                      View.GESTURE_NO_VALUE,
                                      View.GESTURE_NO_VALUE,
                                      View.GESTURE_NO_VALUE);
        }
    }

    public static void inertiaGestureFinished(View view) {
        gestureFinished(view, 0, true);
    }

    public static void gesturePerformed(View view, int modifiers,
                                        boolean isDirect, boolean isInertia,
                                        int x, int y, int xAbs,
                                        int yAbs, float dx, float dy,
                                        float totaldx, float totaldy,
                                        float totalscale, float totalexpansion,
                                        float totalrotation) {
        WinGestureSupport.modifiers = modifiers;
        WinGestureSupport.isDirect = isDirect;

        final int touchCount = touches.getTouchCount();

        if (touchCount >= 2) {
            gestures.handleTotalZooming(view, modifiers, isDirect, isInertia, x,
                                        y, xAbs, yAbs, totalscale,
                                        totalexpansion);

            gestures.handleTotalRotation(view, modifiers, isDirect, isInertia, x,
                                         y, xAbs, yAbs, Math.toDegrees(
                    totalrotation));
        }

        gestures.handleTotalScrolling(view, modifiers, isDirect, isInertia,
                                      touchCount, x, y, xAbs, yAbs, totaldx,
                                      totaldy, multiplier, multiplier);
    }
}
