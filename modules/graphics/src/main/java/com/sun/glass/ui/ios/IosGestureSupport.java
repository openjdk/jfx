/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.ios;

import com.sun.glass.ui.GestureSupport;
import com.sun.glass.ui.TouchInputSupport;
import com.sun.glass.ui.View;

// Used from native code. When native event occurs (e.g. notifyBeginTouchEvent())
// we are notified about it from native code through IosGestureSupport callbacks.
final class IosGestureSupport {

    private native static void _initIDs();
    static {
        _initIDs();
    }

    // The multiplier used to convert scroll units to pixels
    private final static double multiplier = 1.0;

    private final static boolean isDirect = true;

    private final static GestureSupport gestures = new GestureSupport(false);
    private final static TouchInputSupport touches =
            new TouchInputSupport(gestures.createTouchCountListener(), false);


    public static void notifyBeginTouchEvent(
        View view, int modifiers, int touchEventCount) {
        touches.notifyBeginTouchEvent(view, modifiers, isDirect, touchEventCount);
    }


    public static void notifyNextTouchEvent(
        View view, int state, long id, float x, float y) {
        touches.notifyNextTouchEvent(view, state, id, (int)x, (int)y, (int)x, (int)y);
    }


    public static void notifyEndTouchEvent(View view) {
        touches.notifyEndTouchEvent(view);
    }


    public static void rotateGesturePerformed(View view, int modifiers, int x,
                                              int y, int xAbs, int yAbs,
                                              float rotation) {
        gestures.handleDeltaRotation(view, modifiers, isDirect, false, x, y,
                                     xAbs, yAbs, (180.0f / Math.PI) * rotation);
    }


    public static void scrollGesturePerformed(View view, int modifiers,
            boolean inertia, float x,
            float y, float xAbs, float yAbs,
            float dx, float dy) {
        gestures.handleDeltaScrolling(view, modifiers, isDirect, inertia,
                touches.getTouchCount(), (int)x, (int)y,
                (int)xAbs, (int)yAbs, dx, dy, multiplier, multiplier);
    }


    public static void swipeGesturePerformed(View view, int modifiers, int dir,
                                             int x, int y, int xAbs, int yAbs) {
        GestureSupport.handleSwipePerformed(view, modifiers, isDirect, false, touches.
                getTouchCount(), dir, x, y, xAbs, yAbs);
    }


    public static void magnifyGesturePerformed(View view, int modifiers, int x,
                                               int y, int xAbs, int yAbs,
                                               float scale) {
        System.out.println(scale);
        gestures.handleDeltaZooming(view, modifiers, isDirect, false, x, y, xAbs,
                                    yAbs, scale, View.GESTURE_NO_DOUBLE_VALUE);
    }


    public static void gestureFinished(View view, int modifiers, int x, int y,
                                       int xAbs, int yAbs) {
        if (gestures.isScrolling()) {
            gestures.handleScrollingEnd(view, modifiers, touches.getTouchCount(),
                                        isDirect, false, x, y, xAbs, yAbs);
        }

        if (gestures.isRotating()) {
            gestures.handleRotationEnd(view, modifiers, isDirect, false, x, y,
                                       xAbs, yAbs);
        }

        if (gestures.isZooming()) {
            gestures.handleZoomingEnd(view, modifiers, isDirect, false, x, y,
                                      xAbs, yAbs);
        }
    }
}
