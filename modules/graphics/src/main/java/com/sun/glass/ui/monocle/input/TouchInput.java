/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.events.TouchEvent;
import com.sun.glass.ui.GestureSupport;
import com.sun.glass.ui.TouchInputSupport;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.monocle.MonocleWindow;
import com.sun.glass.ui.monocle.input.filters.TouchPipeline;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Processes touch input events based on changes to touch state. Not
 * thread-safe.
 */
public class TouchInput {

    /**
     * This property determines the sensitivity of move events from touch. The
     * bigger the value the less sensitive is the touch screen. In practice move
     * events with a delta smaller then the value of this property will be
     * filtered out.The value of the property is in pixels.
     */
    private final int touchRadius = AccessController.doPrivileged(
            new PrivilegedAction<Integer>() {
                @Override
                public Integer run() {
                    return Integer.getInteger(
                            "monocle.input.touchRadius", 20);
                }
            });

    private static TouchInput instance = new TouchInput();
    private TouchPipeline basePipeline;

    private TouchState state = new TouchState();
    private final GestureSupport gestures = new GestureSupport(false);
    private final TouchInputSupport touches =
            new TouchInputSupport(gestures.createTouchCountListener(), false);

    public static TouchInput getInstance() {
        return instance;
    }

    private TouchInput() {
    }

    /** Gets the base touch filter pipeline common to all touch devices */
    public TouchPipeline getBasePipeline() {
        if (basePipeline == null) {
            basePipeline = new TouchPipeline();
            String[] touchFilterNames = AccessController.doPrivileged(
                    new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return System.getProperty(
                                    "monocle.input.touchFilters",
                                    "SmallMove");
                        }
                    }).split(",");
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
    public void getState(TouchState result) {
        state.copyTo(result);
    }

    /** Returns the touch point for the given ID in the current TouchState,
     *  or null if no match was found.
     *
     * @param id The touch point ID to check for. 0 matches any ID.
     * @return A matching touch point if available, or null if none was found
     */
    TouchState.Point getPointForID(int id) {
        return state.getPointForID(id, false);
    }

    /** Called from the input processor to update the touch state and send
     * touch and mouse events.
     *
     * @param newState The updated touch state
     */
    public void setState(TouchState newState) {
        newState.sortPointsByID();
        newState.assignPrimaryID();
        // Get the cached window for the old state and compute the window for
        // the new state
        MonocleWindow oldWindow = state.getWindow(false, null);
        MonocleWindow window = newState.getWindow(true, oldWindow);
        View oldView = oldWindow == null ? null : oldWindow.getView();
        View view = window == null ? null : window.getView();
        if (!newState.equalsSorted(state)) {
            // Post touch events
            if (view != oldView) {
                dispatchAllPoints(state, TouchEvent.TOUCH_RELEASED);
                dispatchAllPoints(newState,
                                  TouchEvent.TOUCH_PRESSED);
            } else if (view != null) {
                dispatchPoints(window, view, newState);
            }
            // Post mouse events
            MouseInputSynthesizer.getInstance().setState(newState);
        }
        newState.copyTo(state);
    }

    private void dispatchPoint(Window window, View view, int state,
                               TouchState.Point p) {
        touches.notifyNextTouchEvent(view, state, p.id,
                                     p.x, p.y,
                                     p.x - window.getX(), p.y - window.getY());
    }

    /** Sends the same event type for all points in the given state
     *
     * @param state The state for which to process all points
     * @param eventType The type of TouchEvent to send (e.g. TouchEvent.PRESSED)
     */
    private void dispatchAllPoints(TouchState state, int eventType) {

        Window window = state.getWindow(false, null);
        View view = window == null ? null : window.getView();
        if (view != null) {
            touches.notifyBeginTouchEvent(view, 0, true, state.getPointCount());
            for (int i = 0; i < state.getPointCount(); i++) {
                TouchState.Point oldPoint = state.getPoint(i);
                try {
                    dispatchPoint(window, view, eventType, oldPoint);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            touches.notifyEndTouchEvent(view);
        }
    }

    /** Sends updated touch points within the same View as last processed
     * touch points.
     *
     * @param window The current Window
     * @param view The current View
     * @param newState The updated touch points
     */
    private void dispatchPoints(MonocleWindow window,
                                View view,
                                TouchState newState) {
        touches.notifyBeginTouchEvent(view, 0, true, countEvents(newState));
        for (int i = 0; i < state.getPointCount(); i++) {
            TouchState.Point oldPoint = state.getPoint(i);
            TouchState.Point newPoint = newState.getPointForID(oldPoint.id, false);
            if (newPoint != null) {
                if (newPoint.x == oldPoint.x && newPoint.y == oldPoint.y) {
                    dispatchPoint(window, view, TouchEvent.TOUCH_STILL,
                                  newPoint);
                } else {
                    dispatchPoint(window, view, TouchEvent.TOUCH_MOVED,
                                  newPoint);
                }
            } else {
                dispatchPoint(window, view, TouchEvent.TOUCH_RELEASED, oldPoint);
            }
        }
        // Once we have dealt with updates to old points, all that are left
        // are new points.
        for (int i = 0; i < newState.getPointCount(); i++) {
            TouchState.Point newPoint = newState.getPoint(i);
            TouchState.Point oldPoint = state.getPointForID(newPoint.id, false);
            if (oldPoint == null) {
                dispatchPoint(window, view, TouchEvent.TOUCH_PRESSED, newPoint);
            }
        }
        touches.notifyEndTouchEvent(view);
    }


    /** Calculate the number of touch point events that will be sent by
     * dispatchPoints(). This is the union of the touch points in the old and
     * new states.
     */
    private int countEvents(TouchState newState) {
        int count = state.getPointCount();
        for (int i = 0; i < newState.getPointCount(); i++) {
            TouchState.Point newPoint = newState.getPoint(i);
            TouchState.Point oldPoint = state.getPointForID(newPoint.id, false);
            if (oldPoint == null) {
                count ++;
            }
        }
        return count;
    }

    public int getTouchRadius() {
        return touchRadius;
    }

}
