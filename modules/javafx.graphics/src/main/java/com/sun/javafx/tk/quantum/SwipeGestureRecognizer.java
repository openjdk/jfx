/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.TouchEvent;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import javafx.event.EventType;
import javafx.scene.input.SwipeEvent;

class SwipeGestureRecognizer implements GestureRecognizer {

    private static final double TANGENT_30_DEGREES = 0.577;

    private static final double TANGENT_45_DEGREES = 1;

    private static final boolean VERBOSE = false;

    // Swipes must be longer than that
    private static final double DISTANCE_THRESHOLD = 10; // pixel

    // Traveling this distance against the swipe direction at its end cancels it
    private static final double BACKWARD_DISTANCE_THRASHOLD = 5; // pixel

    private SwipeRecognitionState state = SwipeRecognitionState.IDLE;
    MultiTouchTracker tracker = new MultiTouchTracker();
    private ViewScene scene;

    SwipeGestureRecognizer(final ViewScene scene) {
        this.scene = scene;
    }

    @Override
    public void notifyBeginTouchEvent(long time, int modifiers, boolean isDirect,
            int touchEventCount) {
        tracker.params(modifiers, isDirect);
    }

    @Override
    public void notifyNextTouchEvent(long time, int type, long touchId,
                                     int x, int y, int xAbs, int yAbs) {
        switch(type) {
            case TouchEvent.TOUCH_PRESSED:
                tracker.pressed(touchId, time, x, y, xAbs, yAbs);
                break;
            case TouchEvent.TOUCH_STILL:
                /* NOBREAK */
            case TouchEvent.TOUCH_MOVED:
                tracker.progress(touchId, time, xAbs, yAbs);
                break;
            case TouchEvent.TOUCH_RELEASED:
                tracker.released(touchId, time, x, y, xAbs, yAbs);
                break;
            default:
                throw new RuntimeException("Error in swipe gesture recognition: "
                        + "unknown touch state: " + state);
        }
    }

    @Override
    public void notifyEndTouchEvent(long time) {
        // nothing to do
    }

    private EventType<SwipeEvent> calcSwipeType(TouchPointTracker tracker) {

        final double distanceX = tracker.getDistanceX();
        final double distanceY = tracker.getDistanceY();
        final double absDistanceX = Math.abs(distanceX);
        final double absDistanceY = Math.abs(distanceY);

        final boolean horizontal = absDistanceX > absDistanceY;

        final double primaryDistance = horizontal ? distanceX : distanceY;
        final double absPrimaryDistance = horizontal ? absDistanceX : absDistanceY;
        final double absSecondaryDistance = horizontal ? absDistanceY : absDistanceX;
        final double absPrimaryLength = horizontal ?
                tracker.lengthX : tracker.lengthY;
        final double maxSecondaryDeviation = horizontal ?
                tracker.maxDeviationY : tracker.maxDeviationX;
        final double lastPrimaryMovement = horizontal ?
                tracker.lastXMovement : tracker.lastYMovement;

        if (absPrimaryDistance <= DISTANCE_THRESHOLD) {
            // too small movement
            return null;
        }

        if (absSecondaryDistance > absPrimaryDistance * TANGENT_30_DEGREES) {
            // too diagonal - in range of 60 degrees
            return null;
        }

        if (maxSecondaryDeviation > absPrimaryDistance * TANGENT_45_DEGREES) {
            // maximum deviation on the secondary axis, is too big
            return null;
        }

        int swipeMaxDuration = Integer.getInteger("com.sun.javafx.gestures.swipe.maxduration", 300);
        if (tracker.getDuration() > swipeMaxDuration) {
            return null;
        }

        if (absPrimaryLength > absPrimaryDistance * 1.5) {
            // too much back and forth
            return null;
        }

        if (Math.signum(primaryDistance) != Math.signum(lastPrimaryMovement) &&
                Math.abs(lastPrimaryMovement) > BACKWARD_DISTANCE_THRASHOLD) {
            // gesture finished in the oposite direction
            return null;
        }

        if (horizontal) {
            return tracker.getDistanceX() < 0
                    ? SwipeEvent.SWIPE_LEFT : SwipeEvent.SWIPE_RIGHT;
        } else {
            return tracker.getDistanceY() < 0
                    ? SwipeEvent.SWIPE_UP : SwipeEvent.SWIPE_DOWN;
        }
    }

    @SuppressWarnings("removal")
    private void handleSwipeType(final EventType<SwipeEvent> swipeType,
            final CenterComputer cc, final int touchCount, final int modifiers, final boolean isDirect)
    {
        if (swipeType == null) {
            return;
        }
        if (VERBOSE) {
            System.err.println("handleSwipeType swipeType=" + swipeType);
        }

        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.swipeEvent(swipeType, touchCount,
                    cc.getX(), cc.getY(),
                    cc.getAbsX(), cc.getAbsY(),
                    (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                    (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                    (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                    (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                    isDirect);
            }
            return null;
        }, scene.getAccessControlContext());
    }

    private static class CenterComputer {
        double totalAbsX = 0, totalAbsY = 0;
        double totalX = 0, totalY = 0;
        int count = 0;

        public void add(double x, double y, double xAbs, double yAbs) {
            totalAbsX += xAbs;
            totalAbsY += yAbs;
            totalX += x;
            totalY += y;

            count++;
        }

        public double getX() {
            return count == 0 ? 0 : totalX / count;
        }

        public double getY() {
            return count == 0 ? 0 : totalY / count;
        }

        public double getAbsX() {
            return count == 0 ? 0 : totalAbsX / count;
        }

        public double getAbsY() {
            return count == 0 ? 0 : totalAbsY / count;
        }

        public void reset() {
            totalX = 0;
            totalY = 0;
            totalAbsX = 0;
            totalAbsY = 0;
            count = 0;
        }
    }

    private class MultiTouchTracker {
        SwipeRecognitionState state = SwipeRecognitionState.IDLE;
        Map<Long, TouchPointTracker> trackers =
                new HashMap<Long, TouchPointTracker>();
        CenterComputer cc = new CenterComputer();
        int modifiers;
        boolean direct;
        private int touchCount;
        private int currentTouchCount;
        private EventType<SwipeEvent> type;

        public void params(int modifiers, boolean direct) {
            this.modifiers = modifiers;
            this.direct = direct;
        }

        public void pressed(long id, long nanos, int x, int y, int xAbs, int yAbs) {
            currentTouchCount++;
            switch (state) {
                case IDLE:
                    currentTouchCount = 1;
                    state = SwipeRecognitionState.ADDING;
                    /* NOBREAK */
                case ADDING:
                    TouchPointTracker tracker = new TouchPointTracker();
                    tracker.start(nanos, x, y, xAbs, yAbs);
                    trackers.put(id, tracker);
                    break;
                case REMOVING:
                    // we don't allow for swipes with varying touch count
                    state = SwipeRecognitionState.FAILURE;
                    break;
                default:
                    break;
            }
        }

        public void released(long id, long nanos, int x, int y, int xAbs, int yAbs) {
            if (state != SwipeRecognitionState.FAILURE) {
                TouchPointTracker tracker = trackers.get(id);

                if (tracker == null) {
                    // we don't know this ID, something went completely wrong
                    state = SwipeRecognitionState.FAILURE;
                    throw new RuntimeException("Error in swipe gesture "
                            + "recognition: released unknown touch point");
                }

                tracker.end(nanos, x, y, xAbs, yAbs);
                cc.add(tracker.beginX, tracker.beginY,
                        tracker.beginAbsX, tracker.beginAbsY);
                cc.add(tracker.endX, tracker.endY,
                        tracker.endAbsX, tracker.endAbsY);

                final EventType<SwipeEvent> swipeType = calcSwipeType(tracker);

                switch (state) {
                    case IDLE:
                        reset();
                        throw new RuntimeException("Error in swipe gesture "
                                + "recognition: released touch point outside "
                                + "of gesture");
                    case ADDING:
                        state = SwipeRecognitionState.REMOVING;
                        touchCount = currentTouchCount;
                        type = swipeType;
                        break;
                    case REMOVING:
                        if (type != swipeType) {
                            // each finger does something else
                            state = SwipeRecognitionState.FAILURE;
                        }
                        break;
                    default:
                        break;
                }
                trackers.remove(id);
            }

            currentTouchCount--;

            if (currentTouchCount == 0) {
                if (state == SwipeRecognitionState.REMOVING) {
                    handleSwipeType(type, cc, touchCount, modifiers, direct);
                }

                state = SwipeRecognitionState.IDLE;
                reset();
            }
        }

        public void progress(long id, long nanos, int x, int y) {

            if (state == SwipeRecognitionState.FAILURE) {
                return;
            }

            TouchPointTracker tracker = trackers.get(id);

            if (tracker == null) {
                // we don't know this ID, something went completely wrong
                state = SwipeRecognitionState.FAILURE;
                throw new RuntimeException("Error in swipe gesture "
                        + "recognition: reported unknown touch point");
            }

            tracker.progress(nanos, x, y);
        }

        void reset() {
            trackers.clear();
            cc.reset();
            state = SwipeRecognitionState.IDLE;
        }
    }

    private static class TouchPointTracker {
        long beginTime, endTime;
        double beginX, beginY, endX, endY;
        double beginAbsX, beginAbsY, endAbsX, endAbsY;
        double lengthX, lengthY;
        double maxDeviationX, maxDeviationY;
        double lastXMovement, lastYMovement;
        double lastX, lastY;

        public void start(long nanos, double x, double y, double absX, double absY) {
            beginX = x;
            beginY = y;
            beginAbsX = absX;
            beginAbsY = absY;
            lastX = absX;
            lastY = absY;
            beginTime = nanos / 1000000;
        }

        public void end(long nanos, double x, double y, double absX, double absY) {
            progress(nanos, absX, absY);
            endX = x;
            endY = y;
            endAbsX = absX;
            endAbsY = absY;
            endTime = nanos / 1000000;
        }

        public void progress(long nanos, double x, double y) {
            final double deltaX = x - lastX;
            final double deltaY = y - lastY;

            lengthX += Math.abs(deltaX);
            lengthY += Math.abs(deltaY);
            lastX = x;
            lastY = y;

            final double devX = Math.abs(x - beginAbsX);
            if (devX > maxDeviationX) { maxDeviationX = devX; }

            final double devY = Math.abs(y - beginAbsY);
            if (devY > maxDeviationY) { maxDeviationY = devY; }

            if (Math.signum(deltaX) == Math.signum(lastXMovement)) {
                lastXMovement += deltaX;
            } else {
                lastXMovement = deltaX;
            }

            if (Math.signum(deltaY) == Math.signum(lastYMovement)) {
                lastYMovement += deltaY;
            } else {
                lastYMovement = deltaY;
            }
        }

        public double getDistanceX() {
            return endX - beginX;
        }

        public double getDistanceY() {
            return endY - beginY;
        }

        public long getDuration() {
            return endTime - beginTime;
        }
    }

    private enum SwipeRecognitionState {
        IDLE,
        ADDING,
        REMOVING,
        FAILURE
    }
}
