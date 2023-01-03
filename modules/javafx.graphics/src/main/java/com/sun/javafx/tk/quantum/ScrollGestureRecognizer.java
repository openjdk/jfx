/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.util.Duration;
import javafx.scene.input.ScrollEvent;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

class ScrollGestureRecognizer implements GestureRecognizer {
    // gesture will be activated if |scroll amount| > SCROLL_THRESHOLD
    private static double SCROLL_THRESHOLD = 10; //in pixels
    private static boolean SCROLL_INERTIA_ENABLED = true;
    private static double MAX_INITIAL_VELOCITY = 1000;
    private static double SCROLL_INERTIA_MILLIS = 1500;
    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            String s = System.getProperty("com.sun.javafx.gestures.scroll.threshold");
            if (s != null) {
                SCROLL_THRESHOLD = Double.valueOf(s);
            }
            s = System.getProperty("com.sun.javafx.gestures.scroll.inertia");
            if (s != null) {
                SCROLL_INERTIA_ENABLED = Boolean.valueOf(s);
            }
            return null;
        });
    }

    private ViewScene scene;

    private ScrollRecognitionState state = ScrollRecognitionState.IDLE;
    private Timeline inertiaTimeline = new Timeline();
    private DoubleProperty inertiaScrollVelocity = new SimpleDoubleProperty();
    private double initialInertiaScrollVelocity = 0;
    private double scrollStartTime = 0;
    private double lastTouchEventTime = 0;

    private Map<Long, TouchPointTracker> trackers = new HashMap<>();

    private int modifiers;
    private boolean direct;

    private int currentTouchCount = 0;
    private int lastTouchCount;
    private boolean touchPointsSetChanged;
    private boolean touchPointsPressed;

    private double centerX, centerY;
    private double centerAbsX, centerAbsY;
    private double lastCenterAbsX, lastCenterAbsY;

    private double deltaX, deltaY;
    private double totalDeltaX, totalDeltaY;
    private double factorX, factorY;
    double inertiaLastTime = 0;

    ScrollGestureRecognizer(final ViewScene scene) {
        this.scene = scene;
        inertiaScrollVelocity.addListener(valueModel -> {
            double currentTime = inertiaTimeline.getCurrentTime().toSeconds();
            double timePassed = currentTime - inertiaLastTime;
            inertiaLastTime = currentTime;

            double scrollVectorSize = timePassed * inertiaScrollVelocity.get();
            deltaX = scrollVectorSize * factorX;
            totalDeltaX += deltaX;
            deltaY = scrollVectorSize * factorY;
            totalDeltaY += deltaY;

            //send inertia scroll event
            sendScrollEvent(true, centerAbsX, centerAbsY, currentTouchCount);
        });
    }

    @Override
    public void notifyBeginTouchEvent(long time, int modifiers, boolean isDirect,
            int touchEventCount) {
        params(modifiers, isDirect);
        touchPointsSetChanged = false;
        touchPointsPressed = false;
    }

    @Override
    public void notifyNextTouchEvent(long time, int type, long touchId,
                                     int x, int y, int xAbs, int yAbs) {
        switch(type) {
            case TouchEvent.TOUCH_PRESSED:
                touchPointsSetChanged = true;
                touchPointsPressed = true;
                touchPressed(touchId, time, x, y, xAbs, yAbs);
                break;
            case TouchEvent.TOUCH_STILL:
                break;
            case TouchEvent.TOUCH_MOVED:
                touchMoved(touchId, time, x, y, xAbs, yAbs);
                break;
            case TouchEvent.TOUCH_RELEASED:
                touchPointsSetChanged = true;
                touchReleased(touchId, time, x, y, xAbs, yAbs);
                break;
            default:
                throw new RuntimeException("Error in Scroll gesture recognition: "
                        + "unknown touch state: " + state);
        }
    }

    private void calculateCenter() {
        if (currentTouchCount <= 0) {
            throw new RuntimeException("Error in Scroll gesture recognition: "
                    + "touch count is zero!");
        }
        double totalX = 0.0;
        double totalY = 0.0;
        double totalAbsX = 0.0;
        double totalAbsY = 0.0;
        for (TouchPointTracker tracker : trackers.values()) {
            totalX += tracker.getX();
            totalY += tracker.getY();
            totalAbsX += tracker.getAbsX();
            totalAbsY += tracker.getAbsY();
        }
        centerX = totalX / currentTouchCount;
        centerY = totalY / currentTouchCount;
        centerAbsX = totalAbsX / currentTouchCount;
        centerAbsY = totalAbsY / currentTouchCount;
    }

    @Override
    public void notifyEndTouchEvent(long time) {
        lastTouchEventTime = time;
        if (currentTouchCount != trackers.size()) {
            throw new RuntimeException("Error in Scroll gesture recognition: "
                    + "touch count is wrong: " + currentTouchCount);
        }

        if (currentTouchCount < 1) {
            if (state == ScrollRecognitionState.ACTIVE) {
                sendScrollFinishedEvent(lastCenterAbsX, lastCenterAbsY, lastTouchCount);

                if (SCROLL_INERTIA_ENABLED) {
                    double timeFromLastScroll = ((double)time - scrollStartTime) / 1000000;
                    if (timeFromLastScroll < 300) {
                        state = ScrollRecognitionState.INERTIA;
                        // activate inertia
                        inertiaLastTime = 0;
                        if (initialInertiaScrollVelocity > MAX_INITIAL_VELOCITY)
                            initialInertiaScrollVelocity = MAX_INITIAL_VELOCITY;

                        inertiaTimeline.getKeyFrames().setAll(
                            new KeyFrame(
                                Duration.millis(0),
                                new KeyValue(inertiaScrollVelocity, initialInertiaScrollVelocity, Interpolator.LINEAR)),
                            new KeyFrame(
                                Duration.millis(SCROLL_INERTIA_MILLIS * Math.abs(initialInertiaScrollVelocity) / MAX_INITIAL_VELOCITY),
                                event -> {
                                    //stop inertia
                                    reset();
                                },
                                new KeyValue(inertiaScrollVelocity, 0, Interpolator.LINEAR))
                            );
                        inertiaTimeline.playFromStart();
                    } else {
                        reset();
                    }
                } else {
                    reset();
                }
            }
        } else {
            // currentTouchCount >= 1
            calculateCenter();

            if (touchPointsPressed && state == ScrollRecognitionState.INERTIA) {
                //Stop inertia
                inertiaTimeline.stop();
                reset();
            }

            if (touchPointsSetChanged) {
                if (state == ScrollRecognitionState.IDLE) {
                    state = ScrollRecognitionState.TRACKING;
                }
                if (state == ScrollRecognitionState.ACTIVE) {
                    //finish previous gesture
                    sendScrollFinishedEvent(lastCenterAbsX, lastCenterAbsY, lastTouchCount);
                    totalDeltaX = 0.0;
                    totalDeltaY = 0.0;
                    //start previous gesture
                    sendScrollStartedEvent(centerAbsX, centerAbsY, currentTouchCount);
                }
                lastTouchCount = currentTouchCount;
                lastCenterAbsX = centerAbsX;
                lastCenterAbsY = centerAbsY;
            } else {
                //state should be either TRACKING or ACTIVE
                deltaX = centerAbsX - lastCenterAbsX;
                deltaY = centerAbsY - lastCenterAbsY;
                if (state == ScrollRecognitionState.TRACKING) {
                    if ( Math.abs(deltaX) > SCROLL_THRESHOLD || Math.abs(deltaY) > SCROLL_THRESHOLD) {
                        state = ScrollRecognitionState.ACTIVE;
                        sendScrollStartedEvent(centerAbsX, centerAbsY, currentTouchCount);
                    }
                }
                if (state == ScrollRecognitionState.ACTIVE) {
                    totalDeltaX += deltaX;
                    totalDeltaY += deltaY;

                    sendScrollEvent(false, centerAbsX, centerAbsY, currentTouchCount);
                    double timePassed = ((double)time - scrollStartTime) / 1000000000;
                    if (timePassed > 1e-4) {
                        //capture radius (pytaguras) or init to variables x,y ???
                        double scrollMagnitude = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        factorX = deltaX / scrollMagnitude;
                        factorY = deltaY / scrollMagnitude;
                        initialInertiaScrollVelocity = scrollMagnitude / timePassed;

                        scrollStartTime = time;
                    }

                    lastCenterAbsX = centerAbsX;
                    lastCenterAbsY = centerAbsY;
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private void sendScrollStartedEvent(double xAbs, double yAbs, int touchCount) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.scrollEvent(ScrollEvent.SCROLL_STARTED,
                    0, 0,
                    0, 0,
                    1 /*xMultiplier*/, 1 /*yMultiplier*/,
                    touchCount,
                    0 /*scrollTextX*/, 0 /*scrollTextY*/,
                    0 /*defaultTextX*/, 0 /*defaultTextY*/,
                    centerX, centerY, xAbs, yAbs,
                    (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                    (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                    (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                    (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                    direct, false /*inertia*/);
            }
            return null;
        }, scene.getAccessControlContext());
    }

    @SuppressWarnings("removal")
    private void sendScrollEvent(boolean isInertia, double xAbs, double yAbs, int touchCount) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.scrollEvent(ScrollEvent.SCROLL,
                    deltaX, deltaY,
                    totalDeltaX, totalDeltaY,
                    1 /*xMultiplier*/, 1 /*yMultiplier*/,
                    touchCount,
                    0 /*scrollTextX*/, 0 /*scrollTextY*/,
                    0 /*defaultTextX*/, 0 /*defaultTextY*/,
                    centerX, centerY, xAbs, yAbs,
                    (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                    (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                    (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                    (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                    direct, isInertia);
            }
            return null;
        }, scene.getAccessControlContext());
    }

    @SuppressWarnings("removal")
    private void sendScrollFinishedEvent(double xAbs, double yAbs, int touchCount) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.scrollEvent(ScrollEvent.SCROLL_FINISHED,
                    0, 0,
                    totalDeltaX, totalDeltaY,
                    1 /*xMultiplier*/, 1 /*yMultiplier*/,
                    touchCount,
                    0 /*scrollTextX*/, 0 /*scrollTextY*/,
                    0 /*defaultTextX*/, 0 /*defaultTextY*/,
                    centerX, centerY, xAbs, yAbs,
                    (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                    (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                    (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                    (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                    direct, false /*inertia*/);
            }
            return null;
        }, scene.getAccessControlContext());
    }

    public void params(int modifiers, boolean direct) {
        this.modifiers = modifiers;
        this.direct = direct;
    }

    public void touchPressed(long id, long nanos, int x, int y, int xAbs, int yAbs) {
        currentTouchCount++;
        TouchPointTracker tracker = new TouchPointTracker();
        tracker.update(nanos, x, y, xAbs, yAbs);
        trackers.put(id, tracker);
    }

    public void touchReleased(long id, long nanos, int x, int y, int xAbs, int yAbs) {
        if (state != ScrollRecognitionState.FAILURE) {
            TouchPointTracker tracker = trackers.get(id);
            if (tracker == null) {
                // we don't know this ID, something went completely wrong
                state = ScrollRecognitionState.FAILURE;
                throw new RuntimeException("Error in Scroll gesture "
                        + "recognition: released unknown touch point");
            }
            trackers.remove(id);
        }
        currentTouchCount--;
    }

    public void touchMoved(long id, long nanos, int x, int y, int xAbs, int yAbs) {
        if (state == ScrollRecognitionState.FAILURE) {
            return;
        }

        TouchPointTracker tracker = trackers.get(id);
        if (tracker == null) {
            // we don't know this ID, something went completely wrong
            state = ScrollRecognitionState.FAILURE;
            throw new RuntimeException("Error in scroll gesture "
                    + "recognition: reported unknown touch point");
        }
        tracker.update(nanos, x, y, xAbs, yAbs);
    }

    void reset() {
        state = ScrollRecognitionState.IDLE;
        totalDeltaX = 0.0;
        totalDeltaY = 0.0;
    }

    private static class TouchPointTracker {
        double x, y;
        double absX, absY;

        public void update(long nanos, double x, double y, double absX, double absY) {
            this.x = x;
            this.y = y;
            this.absX = absX;
            this.absY = absY;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getAbsX() {
            return absX;
        }

        public double getAbsY() {
            return absY;
        }
    }

    private enum ScrollRecognitionState {
        IDLE,       // no touch points available
        TRACKING,   // 1+ touch points, center position is tracked
        ACTIVE,     // threshold accepted, gesture is started
        INERTIA,        // inertia is active
        FAILURE
    }
}
