/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import com.sun.glass.events.GestureEvent;

public final class GestureSupport {

    private static class GestureState {

        enum StateId {
            Idle, Running, Inertia
        }

        private StateId id = StateId.Idle;

        void setIdle() {
            id = StateId.Idle;
        }

        boolean isIdle() {
            return id == StateId.Idle;
        }

        int updateProgress(final boolean isInertia) {
            int eventID = GestureEvent.GESTURE_PERFORMED;

            if (doesGestureStart(isInertia) && !isInertia) {
                eventID = GestureEvent.GESTURE_STARTED;
            }

            id = isInertia ? StateId.Inertia : StateId.Running;

            return eventID;
        }

        boolean doesGestureStart(final boolean isInertia) {
            switch (id) {
                case Running:
                    return isInertia;
                case Inertia:
                    return !isInertia;
            }
            return true;
        }
    }

    private final static double THRESHOLD_SCROLL = 1.0;
    private final static double THRESHOLD_SCALE = 0.01;
    private final static double THRESHOLD_EXPANSION = 0.01;
    private final static double THRESHOLD_ROTATE = Math.toDegrees(Math.PI / 180);

    private final GestureState scrolling = new GestureState();
    private final GestureState rotating = new GestureState();
    private final GestureState zooming = new GestureState();
    private final GestureState swiping = new GestureState();

    private double totalScrollX = Double.NaN;
    private double totalScrollY = Double.NaN;
    private double totalScale = 1.0;
    private double totalExpansion = Double.NaN;
    private double totalRotation = 0.0;
    private double multiplierX = 1.0;
    private double multiplierY = 1.0;

    private boolean zoomWithExpansion;

    public GestureSupport(boolean zoomWithExpansion) {
        this.zoomWithExpansion = zoomWithExpansion;
    }

    private static double multiplicativeDelta(double from, double to) {
        if (from == 0.0) {
            return View.GESTURE_NO_DOUBLE_VALUE;
        }
        return (to / from);
    }

    private int setScrolling(boolean isInertia) {
        return scrolling.updateProgress(isInertia);
    }

    private int setRotating(boolean isInertia) {
        return rotating.updateProgress(isInertia);
    }

    private int setZooming(boolean isInertia) {
        return zooming.updateProgress(isInertia);
    }

    private int setSwiping(boolean isInertia) {
        return swiping.updateProgress(isInertia);
    }

    public boolean isScrolling() {
        return !scrolling.isIdle();
    }

    public boolean isRotating() {
        return !rotating.isIdle();
    }

    public boolean isZooming() {
        return !zooming.isIdle();
    }

    public boolean isSwiping() {
        return !swiping.isIdle();
    }

    public void handleScrollingEnd(View view, int modifiers, int touchCount,
                                   boolean isDirect, boolean isInertia, int x,
                                   int y, int xAbs, int yAbs) {
        scrolling.setIdle();
        if (isInertia) {
            return;
        }
        view.notifyScrollGestureEvent(GestureEvent.GESTURE_FINISHED, modifiers,
                                      isDirect, isInertia, touchCount, x, y,
                                      xAbs, yAbs, 0, 0,
                                      totalScrollX, totalScrollY,
                                      multiplierX, multiplierY);
    }

    public void handleRotationEnd(View view, int modifiers, boolean isDirect,
                                  boolean isInertia, int x, int y, int xAbs,
                                  int yAbs) {
        rotating.setIdle();
        if (isInertia) {
            return;
        }
        view.notifyRotateGestureEvent(GestureEvent.GESTURE_FINISHED, modifiers,
                                      isDirect, isInertia, x, y, xAbs, yAbs, 0,
                                      totalRotation);
    }

    public void handleZoomingEnd(View view, int modifiers, boolean isDirect,
                                 boolean isInertia, int x, int y, int xAbs,
                                 int yAbs) {
        zooming.setIdle();
        if (isInertia) {
            return;
        }
        view.notifyZoomGestureEvent(GestureEvent.GESTURE_FINISHED, modifiers,
                                    isDirect, isInertia, x, y, xAbs, yAbs,
                                    View.GESTURE_NO_DOUBLE_VALUE, 0, totalScale,
                                    totalExpansion);
    }

    public void handleSwipeEnd(View view, int modifiers, boolean isDirect,
                               boolean isInertia, int x, int y, int xAbs,
                               int yAbs) {
        swiping.setIdle();
        if (isInertia) {
            return;
        }
        view.notifySwipeGestureEvent(GestureEvent.GESTURE_FINISHED, modifiers,
                                     isDirect, isInertia, View.GESTURE_NO_VALUE,
                                     View.GESTURE_NO_VALUE, x, y, xAbs, yAbs);
    }

    public void handleTotalZooming(View view, int modifiers, boolean isDirect,
                                   boolean isInertia, int x, int y, int xAbs,
                                   int yAbs, double scale, double expansion) {

        double baseScale = totalScale;
        double baseExpansion = totalExpansion;
        if (zooming.doesGestureStart(isInertia)) {
            baseScale = 1.0;
            baseExpansion = 0.0;
        }

        if (Math.abs(scale - baseScale) < THRESHOLD_SCALE &&
                (!zoomWithExpansion ||
                    Math.abs(expansion - baseExpansion) < THRESHOLD_SCALE)) {
            return;
        }

        double deltaExpansion = View.GESTURE_NO_DOUBLE_VALUE;
        if (zoomWithExpansion) {
            deltaExpansion = expansion - baseExpansion;
        } else {
            expansion = View.GESTURE_NO_DOUBLE_VALUE;
        }

        totalScale = scale;
        totalExpansion = expansion;
        final int eventID = setZooming(isInertia);

        view.notifyZoomGestureEvent(eventID, modifiers, isDirect, isInertia, x,
                                    y, xAbs, yAbs,
                                    multiplicativeDelta(baseScale, totalScale),
                                    deltaExpansion, scale, expansion);
    }

    public void handleTotalRotation(View view, int modifiers, boolean isDirect,
                                    boolean isInertia, int x, int y, int xAbs,
                                    int yAbs, double rotation) {

        double baseRotation = totalRotation;
        if (rotating.doesGestureStart(isInertia)) {
            baseRotation = 0.0;
        }

        if (Math.abs(rotation - baseRotation) < THRESHOLD_ROTATE) {
            return;
        }

        totalRotation = rotation;
        final int eventID = setRotating(isInertia);

        view.notifyRotateGestureEvent(eventID, modifiers, isDirect, isInertia, x,
                                      y, xAbs, yAbs, rotation - baseRotation,
                                      rotation);
    }

    public void handleTotalScrolling(View view, int modifiers, boolean isDirect,
                                     boolean isInertia, int touchCount, int x,
                                     int y, int xAbs, int yAbs,
                                     double dx, double dy,
                                     double multiplierX, double multiplierY) {
        this.multiplierX = multiplierX;
        this.multiplierY = multiplierY;

        double baseScrollX = totalScrollX;
        double baseScrollY = totalScrollY;
        if (scrolling.doesGestureStart(isInertia)) {
            baseScrollX = 0;
            baseScrollY = 0;
        }

        if (Math.abs(dx - totalScrollX) < THRESHOLD_SCROLL &&
                Math.abs(dy - totalScrollY) < THRESHOLD_SCROLL) {
            return;
        }

        totalScrollX = dx;
        totalScrollY = dy;
        final int eventID = setScrolling(isInertia);

        view.notifyScrollGestureEvent(eventID, modifiers, isDirect, isInertia,
                                      touchCount, x, y, xAbs, yAbs,
                                      dx - baseScrollX,
                                      dy - baseScrollY, dx, dy,
                                      multiplierX, multiplierY);
    }

    public void handleDeltaZooming(View view, int modifiers, boolean isDirect,
                                   boolean isInertia, int x, int y, int xAbs,
                                   int yAbs, double scale, double expansion) {

        double baseScale = totalScale;
        double baseExpansion = totalExpansion;
        if (zooming.doesGestureStart(isInertia)) {
            baseScale = 1.0;
            baseExpansion = 0.0;
        }

        // The algorithm to calculate scale factor was grabbed from OSX
        // documentation at
        // http://developer.apple.com/library/mac/#documentation/cocoa/conceptual/EventOverview/HandlingTouchEvents/HandlingTouchEvents.html
        //
        // Important: when used on other platforms "totalScale" may be out of
        // [0.0; 1.0] range as value of "scale" parameter is platform specific.
        totalScale = baseScale * (1.0 + scale);
        if (zoomWithExpansion) {
            totalExpansion = baseExpansion + expansion;
        } else {
            totalExpansion = View.GESTURE_NO_DOUBLE_VALUE;
        }

        final int eventID = setZooming(isInertia);

        view.notifyZoomGestureEvent(eventID, modifiers, isDirect, isInertia, x,
                                    y, xAbs, yAbs,
                                    multiplicativeDelta(baseScale, totalScale),
                                    expansion, totalScale, totalExpansion);
    }

    public void handleDeltaRotation(View view, int modifiers, boolean isDirect,
                                    boolean isInertia, int x, int y, int xAbs,
                                    int yAbs, double rotation) {

        double baseRotation = totalRotation;
        if (rotating.doesGestureStart(isInertia)) {
            baseRotation = 0.0;
        }

        totalRotation = baseRotation + rotation;
        final int eventID = setRotating(isInertia);

        view.notifyRotateGestureEvent(eventID, modifiers, isDirect, isInertia, x,
                                      y, xAbs, yAbs, rotation, totalRotation);
    }

    public void handleDeltaScrolling(View view, int modifiers, boolean isDirect,
                                     boolean isInertia, int touchCount, int x,
                                     int y, int xAbs, int yAbs,
                                     double dx, double dy,
                                     double multiplierX, double multiplierY) {
        this.multiplierX = multiplierX;
        this.multiplierY = multiplierY;

        double baseScrollX = totalScrollX;
        double baseScrollY = totalScrollY;
        if (scrolling.doesGestureStart(isInertia)) {
            baseScrollX = 0;
            baseScrollY = 0;
        }

        totalScrollX = baseScrollX + dx;
        totalScrollY = baseScrollY + dy;

        final int eventID = setScrolling(isInertia);

        view.notifyScrollGestureEvent(eventID, modifiers, isDirect, isInertia,
                                      touchCount, x, y, xAbs, yAbs, dx, dy,
                                      totalScrollX, totalScrollY,
                                      multiplierX, multiplierY);
    }

    public void handleSwipe(View view, int modifiers, boolean isDirect,
                            boolean isInertia, int touchCount, int dir, int x,
                            int y, int xAbs, int yAbs) {
        final int eventID = setSwiping(isInertia);
        view.notifySwipeGestureEvent(eventID, modifiers, isDirect, isInertia,
                                     touchCount, dir, x, y, xAbs, yAbs);
    }

    public static void handleSwipePerformed(View view, int modifiers,
                                            boolean isDirect, boolean isInertia,
                                            int touchCount, int dir, int x,
                                            int y, int xAbs, int yAbs) {
        view.notifySwipeGestureEvent(GestureEvent.GESTURE_PERFORMED, modifiers,
                                     isDirect, isInertia, touchCount, dir, x, y,
                                     xAbs, yAbs);
    }

    public static void handleScrollingPerformed(View view, int modifiers,
                                                boolean isDirect,
                                                boolean isInertia,
                                                int touchCount, int x, int y,
                                                int xAbs, int yAbs, double dx,
                                                double dy, double multiplierX,
                                                double multiplierY) {
        view.notifyScrollGestureEvent(GestureEvent.GESTURE_PERFORMED, modifiers,
                                      isDirect, isInertia, touchCount, x, y,
                                      xAbs, yAbs, dx, dy, dx, dy, multiplierX, multiplierY);
    }

    public TouchInputSupport.TouchCountListener createTouchCountListener() {
        Application.checkEventThread();
        return (sender, view, modifiers, isDirect) -> {
            final boolean isInertia = false;

            if (isScrolling()) {
                handleScrollingEnd(view, modifiers, sender.getTouchCount(),
                                   isDirect, isInertia,
                                   View.GESTURE_NO_VALUE,
                                   View.GESTURE_NO_VALUE,
                                   View.GESTURE_NO_VALUE,
                                   View.GESTURE_NO_VALUE);
            }

            if (isRotating()) {
                handleRotationEnd(view, modifiers, isDirect, isInertia,
                                  View.GESTURE_NO_VALUE,
                                  View.GESTURE_NO_VALUE,
                                  View.GESTURE_NO_VALUE,
                                  View.GESTURE_NO_VALUE);
            }

            if (isZooming()) {
                handleZoomingEnd(view, modifiers, isDirect, isInertia,
                                 View.GESTURE_NO_VALUE,
                                 View.GESTURE_NO_VALUE,
                                 View.GESTURE_NO_VALUE,
                                 View.GESTURE_NO_VALUE);
            }
        };
    }
}
