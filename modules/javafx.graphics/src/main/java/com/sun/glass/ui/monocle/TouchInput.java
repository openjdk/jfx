/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.events.TouchEvent;
import com.sun.glass.ui.GestureSupport;
import com.sun.glass.ui.TouchInputSupport;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Processes touch input events based on changes to touch state. Not
 * thread-safe.
 */
class TouchInput {

    /**
     * This property determines the sensitivity of move events from touch. The
     * bigger the value the less sensitive is the touch screen. In practice move
     * events with a delta smaller then the value of this property will be
     * filtered out.The value of the property is in pixels.
     */
    @SuppressWarnings("removal")
    private final int touchRadius = AccessController.doPrivileged(
            (PrivilegedAction<Integer>) () -> Integer.getInteger(
                    "monocle.input.touchRadius", 20)
    );

    private static TouchInput instance = new TouchInput();
    private TouchPipeline basePipeline;

    private TouchState state = new TouchState();
    private final GestureSupport gestures = new GestureSupport(false);
    private final TouchInputSupport touches =
            new TouchInputSupport(gestures.createTouchCountListener(), false);

    static TouchInput getInstance() {
        return instance;
    }

    private TouchInput() {
    }

    /** Gets the base touch filter pipeline common to all touch devices */
    TouchPipeline getBasePipeline() {
        if (basePipeline == null) {
            basePipeline = new TouchPipeline();
            @SuppressWarnings("removal")
            String[] touchFilterNames = AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> System.getProperty(
                            "monocle.input.touchFilters",
                            "SmallMove")
            ).split(",");
            if (touchFilterNames != null) {
                for (String touchFilterName : touchFilterNames) {
                    basePipeline.addNamedFilter(touchFilterName.trim());
                }
            }
        }
        return basePipeline;
    }

    /** Copies the current state into the TouchState provided.
     *
     * @param result target into which to copy the touch state
     */
    void getState(TouchState result) {
        state.copyTo(result);
    }

    /** Called from the input processor to update the touch state and send
     * touch and mouse events.
     *
     * @param newState The updated touch state
     */
    void setState(TouchState newState) {
        if (MonocleSettings.settings.traceEvents) {
            MonocleTrace.traceEvent("Set %s", newState);
        }
        newState.sortPointsByID();
        newState.assignPrimaryID();
        // Get the cached window for the old state and compute the window for
        // the new state
        MonocleWindow oldWindow = state.getWindow(false, null);
        boolean recalculateWindow = state.getPointCount() == 0;
        MonocleWindow window = newState.getWindow(recalculateWindow, oldWindow);
        View oldView = oldWindow == null ? null : oldWindow.getView();
        View view = window == null ? null : window.getView();
        if (!newState.equalsSorted(state)) {
            // Post touch events
            if (view != oldView) {
                postTouchEvent(state, TouchEvent.TOUCH_RELEASED);
                postTouchEvent(newState,
                               TouchEvent.TOUCH_PRESSED);
            } else if (view != null) {
                postTouchEvent(window, view, newState);
            }
            // Post mouse events
            MouseInputSynthesizer.getInstance().setState(newState);
        }
        newState.copyTo(state);
        newState.clearWindow();
    }

    private void dispatchPoint(Window window, View view, int state,
                               int id, int x, int y) {
        touches.notifyNextTouchEvent(view, state, id,
                                     x - window.getX(), y - window.getY(),
                                     x, y);
    }

    private void postPoints(Window window, View view,
                            int[] states, int[] ids, int[] xs, int[] ys) {
        RunnableProcessor.runLater(() -> {
            touches.notifyBeginTouchEvent(view, 0, true, states.length);
            for (int i = 0; i < states.length; i++) {
                dispatchPoint(window, view, states[i], ids[i],
                              xs[i], ys[i]);
            }
            touches.notifyEndTouchEvent(view);
        });
    }

    private void postPoint(Window window, View view,
                            int state, TouchState.Point p) {
        int id = p.id;
        int x = p.x;
        int y = p.y;
        RunnableProcessor.runLater(() -> {
            touches.notifyBeginTouchEvent(view, 0, true, 1);
            dispatchPoint(window, view, state, id, x, y);
            touches.notifyEndTouchEvent(view);
        });
    }

    private void postNoPoints(View view) {
        RunnableProcessor.runLater(() -> {
            touches.notifyBeginTouchEvent(view, 0, true, 0);
            touches.notifyEndTouchEvent(view);
        });
    }

    /** Sends the same event type for all points in the given state
     *
     * @param state The state for which to process all points
     * @param eventType The type of TouchEvent to send (e.g. TouchEvent.PRESSED)
     */
    private void postTouchEvent(TouchState state, int eventType) {
        Window window = state.getWindow(false, null);
        View view = window == null ? null : window.getView();
        if (view != null) {
            switch (state.getPointCount()) {
                case 0:
                    postNoPoints(view);
                    break;
                case 1:
                    postPoint(window, view, eventType, state.getPoint(0));
                    break;
                default: {
                    int count = state.getPointCount();
                    int[] states = new int[count];
                    int[] ids = new int[count];
                    int[] xs = new int[count];
                    int[] ys = new int[count];
                    for (int i = 0; i < count; i++) {
                        states[i] = eventType;
                        TouchState.Point p = state.getPoint(i);
                        ids[i] = p.id;
                        xs[i] = p.x;
                        ys[i] = p.y;
                    }
                    postPoints(window, view, states, ids, xs, ys);
                }
            }
        }
    }

    /** Sends updated touch points within the same View as last processed
     * touch points.
     *
     * @param window The current Window
     * @param view The current View
     * @param newState The updated touch points
     */
    private void postTouchEvent(MonocleWindow window,
                                View view,
                                TouchState newState) {
        int count = countEvents(newState);
        switch (count) {
            case 0:
                postNoPoints(view);
                break;
            case 1:
                if (state.getPointCount() == 1) {
                    // There is one point and it already existed
                    TouchState.Point oldPoint = state.getPoint(0);
                    TouchState.Point newPoint = newState.getPointForID(
                            oldPoint.id);
                    if (newPoint != null) {
                        if (newPoint.x == oldPoint.x
                                && newPoint.y == oldPoint.y) {
                            postPoint(window, view, TouchEvent.TOUCH_STILL, newPoint);
                        } else {
                            postPoint(window, view, TouchEvent.TOUCH_MOVED, newPoint);
                        }
                    } else {
                        postPoint(window, view, TouchEvent.TOUCH_RELEASED, oldPoint);
                    }
                } else {
                    // There is one point and it is newly pressed
                    postPoint(window, view, TouchEvent.TOUCH_PRESSED, newState.getPoint(0));
                }
                break;
            default: {
                int[] states = new int[count];
                int[] ids = new int[count];
                int[] xs = new int[count];
                int[] ys = new int[count];
                for (int i = 0; i < state.getPointCount(); i++) {
                    TouchState.Point oldPoint = state.getPoint(i);
                    TouchState.Point newPoint = newState.getPointForID(
                            oldPoint.id);
                    if (newPoint != null) {
                        ids[i] = newPoint.id;
                        xs[i] = newPoint.x;
                        ys[i] = newPoint.y;
                        if (newPoint.x == oldPoint.x
                                && newPoint.y == oldPoint.y) {
                            states[i] = TouchEvent.TOUCH_STILL;
                        } else {
                            states[i] = TouchEvent.TOUCH_MOVED;
                        }
                    } else {
                        states[i] = TouchEvent.TOUCH_RELEASED;
                        ids[i] = oldPoint.id;
                        xs[i] = oldPoint.x;
                        ys[i] = oldPoint.y;
                    }
                }
                // Once we have dealt with updates to old points, all that are left
                // are new points.
                for (int i = 0, j = state.getPointCount();
                        i < newState.getPointCount(); i++) {
                    TouchState.Point newPoint = newState.getPoint(i);
                    TouchState.Point oldPoint = state.getPointForID(
                            newPoint.id);
                    if (oldPoint == null) {
                        states[j] = TouchEvent.TOUCH_PRESSED;
                        ids[j] = newPoint.id;
                        xs[j] = newPoint.x;
                        ys[j] = newPoint.y;
                        j++;
                    }
                }
                postPoints(window, view, states, ids, xs, ys);
            }
        }
    }


    /** Calculate the number of touch point events that will be sent by
     * dispatchPoints(). This is the union of the touch points in the old and
     * new states.
     */
    private int countEvents(TouchState newState) {
        int count = state.getPointCount();
        for (int i = 0; i < newState.getPointCount(); i++) {
            TouchState.Point newPoint = newState.getPoint(i);
            TouchState.Point oldPoint = state.getPointForID(newPoint.id);
            if (oldPoint == null) {
                count ++;
            }
        }
        return count;
    }

    int getTouchRadius() {
        return touchRadius;
    }

}
