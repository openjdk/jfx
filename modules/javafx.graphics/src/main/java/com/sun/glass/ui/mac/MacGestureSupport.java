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
package com.sun.glass.ui.mac;

import com.sun.glass.ui.TouchInputSupport;
import com.sun.glass.ui.GestureSupport;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.View;
import java.security.AccessController;
import java.security.PrivilegedAction;

final class MacGestureSupport {

    private native static void _initIDs();
    static {
        _initIDs();
    }

    private final static int GESTURE_ROTATE = 100;
    private final static int GESTURE_MAGNIFY = 101;
    private final static int GESTURE_SWIPE = 102;

    private final static int SCROLL_SRC_WHEEL = 50;
    private final static int SCROLL_SRC_GESTURE = 51;
    private final static int SCROLL_SRC_INERTIA = 52;

    /**
     * The value used on Mac to convert scroll wheel rotations to pixels.
     */
    private final static double multiplier = 10.0;

    private final static boolean isDirect = false;

    private final static GestureSupport gestures = new GestureSupport(false);
    private final static TouchInputSupport touches =
            new MacTouchInputSupport(gestures.createTouchCountListener(), false);

    public static void notifyBeginTouchEvent(View view, int modifiers,
                                             int touchEventCount) {
        touches.notifyBeginTouchEvent(view, modifiers, isDirect, touchEventCount);
    }

    public static void notifyNextTouchEvent(View view, int state, long id,
                                            float x, float y) {
        // 'x' and 'y' address normalized position. The normalized position
        // is a scaled value between (0.0) and (1.0,1.0), where (0.0,0.0) is
        // the lower-left position on the touch device.
        //
        // So translate the touch position to integer coordinate where (0.0)
        // is the top-left position and (10000,10000) is the bottom-right
        // position on the touch device.

        final int intX = (int) (10000 * x);
        final int intY = 10000 - (int) (10000 * y);
        touches.notifyNextTouchEvent(view, state, id, intX, intY, intX, intY);
    }

    public static void notifyEndTouchEvent(View view) {
        touches.notifyEndTouchEvent(view);
    }

    public static void rotateGesturePerformed(View view, int modifiers, int x,
                                              int y, int xAbs, int yAbs,
                                              float rotation) {
        gestures.handleDeltaRotation(view, modifiers, isDirect, false, x, y,
                                     xAbs, yAbs, -rotation);
    }

    public static void scrollGesturePerformed(View view, int modifiers,
                                              int sender, int x, int y,
                                              int xAbs, int yAbs, float dx,
                                              float dy) {
        final int touchCount = touches.getTouchCount();
        final boolean isInertia = (sender == SCROLL_SRC_INERTIA);
        switch (sender) {
            case SCROLL_SRC_WHEEL:
                // fall through
            case SCROLL_SRC_INERTIA:
                // When inertial scrolling occurs system sends a number of
                // scroll (mouse wheel notifications to be precise)
                // notifications.
                // Handle these notifications in a special way like swipe
                // gestures, i.e. notify view with only
                // GestureEvent#GESTURE_PERFORMED event type.
                //
                // Note: inertial scrolling may occur even when some
                // touch points still present.
                GestureSupport.handleScrollingPerformed(view, modifiers, isDirect,
                                                  isInertia, touchCount, x, y,
                                                  xAbs, yAbs, dx, dy, multiplier,
                                                  multiplier);
                break;
            case SCROLL_SRC_GESTURE:
                gestures.handleDeltaScrolling(view, modifiers, isDirect,
                                              isInertia, touchCount, x, y, xAbs,
                                              yAbs, dx, dy, multiplier, multiplier);
                break;
            default:
                System.err.println("Unknown scroll gesture sender: " + sender);
                break;
        }
    }

    public static void swipeGesturePerformed(View view, int modifiers, int dir,
                                             int x, int y, int xAbs, int yAbs) {
        // On OS X system doesn't call NSResponder#endGestureWithEvent
        // for swipe gestures. So notify view with only
        // GestureEvent#GESTURE_PERFORMED event type.
        gestures.handleSwipePerformed(view, modifiers, isDirect, false, touches.
                getTouchCount(), dir, x, y, xAbs, yAbs);
    }

    public static void magnifyGesturePerformed(View view, int modifiers, int x,
                                               int y, int xAbs, int yAbs,
                                               float scale) {
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
