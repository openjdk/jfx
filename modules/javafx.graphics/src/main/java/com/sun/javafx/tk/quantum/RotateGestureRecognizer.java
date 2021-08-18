/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Set;
import javafx.util.Duration;
import javafx.event.EventType;
import javafx.scene.input.RotateEvent;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

class RotateGestureRecognizer implements GestureRecognizer {
    private ViewScene scene;

    // gesture will be activated if |rotation| > ROTATATION_THRESHOLD
    private static double ROTATATION_THRESHOLD = 5; //in degrees
    private static boolean ROTATION_INERTIA_ENABLED = true;
    private static double MAX_INITIAL_VELOCITY = 500;
    private static double ROTATION_INERTIA_MILLIS = 1500;
    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            String s = System.getProperty("com.sun.javafx.gestures.rotate.threshold");
            if (s != null) {
                ROTATATION_THRESHOLD = Double.valueOf(s);
            }
            s = System.getProperty("com.sun.javafx.gestures.rotate.inertia");
            if (s != null) {
                ROTATION_INERTIA_ENABLED = Boolean.valueOf(s);
            }
            return null;
        });
    }

    private RotateRecognitionState state = RotateRecognitionState.IDLE;
    private Timeline inertiaTimeline = new Timeline();
    private DoubleProperty inertiaRotationVelocity = new SimpleDoubleProperty();
    private double initialInertiaRotationVelocity = 0;
    private double rotationStartTime = 0;
    private double lastTouchEventTime = 0;

    // from MultiTouchTracker
    Map<Long, TouchPointTracker> trackers =
            new HashMap<Long, TouchPointTracker>();

    int modifiers;
    boolean direct;

    //private int touchCount;
    private int currentTouchCount = 0;
    private boolean touchPointsSetChanged;
    private boolean touchPointsPressed;
    int touchPointsInEvent;
    long touchPointID1 = -1;
    long touchPointID2 = -1;
    double centerX, centerY;
    double centerAbsX, centerAbsY;

    double currentRotation;
    double angleReference;
    double totalRotation = 0;
    double inertiaLastTime = 0;

    RotateGestureRecognizer(final ViewScene scene) {
        this.scene = scene;
        inertiaRotationVelocity.addListener(valueModel -> {
            double currentTime = inertiaTimeline.getCurrentTime().toSeconds();
            double timePassed = currentTime - inertiaLastTime;
            inertiaLastTime = currentTime;
            currentRotation = timePassed * inertiaRotationVelocity.get();
            totalRotation += currentRotation;
            //send inertia rotate event
            sendRotateEvent(true);
        });
    }

    @Override
    public void notifyBeginTouchEvent(long time, int modifiers, boolean isDirect,
            int touchEventCount) {
        params(modifiers, isDirect);
        touchPointsSetChanged = false;
        touchPointsPressed = false;
        touchPointsInEvent = 0;
    }

    @Override
    public void notifyNextTouchEvent(long time, int type, long touchId,
                                     int x, int y, int xAbs, int yAbs) {
        touchPointsInEvent++;
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
                throw new RuntimeException("Error in Rotate gesture recognition: "
                        + "unknown touch state: " + state);
        }
    }

    private void calculateCenter() {
        if (currentTouchCount <= 0) {
            throw new RuntimeException("Error in Rotate gesture recognition: "
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

    private double getAngle(TouchPointTracker tp1, TouchPointTracker tp2) {
        double dx = tp2.getAbsX() - tp1.getAbsX();
        double dy = -(tp2.getAbsY() - tp1.getAbsY()); //standard math y-axis direction (increases upwards) is opposite to touchscreen y-axis direction (increases downwards)
        double newAngle = Math.toDegrees(Math.atan2(dy, dx)); // Result in range [-180,+180]
        return newAngle;
    }

    //oldAngle, newAngle expected to be in rangle [-180,+180]
    private double getNormalizedDelta(double oldAngle, double newAngle) {
        // while the input angles reflect the normal polar angle (angle increase with anti-clockwise rotation)
        //  for rotation events positive values are used for clockwise rotation, therefore the negation.
        double delta = -(newAngle - oldAngle);


        //delta now in [-360,+360]. Normalize to [-180,+180]
        if (delta > 180) {
            delta -= 360;
        } else if (delta < -180) {
            delta += 360;
        }
        return delta;
    }

    private void assignActiveTouchpoints() {
        boolean needToReassign = false;
        if (!trackers.containsKey(touchPointID1)) {
            touchPointID1 = -1;
            needToReassign = true;
        }
        if (!trackers.containsKey(touchPointID2)) {
            touchPointID2 = -1;
            needToReassign = true;
        }

        if (needToReassign) {
            for (Long id : trackers.keySet()) {
                if (id == touchPointID1 || id == touchPointID2) {
                    //already used, skip
                } else {
                    if (touchPointID1 == -1) {
                        // assign to first touch point
                        touchPointID1 = id;
                    } else if (touchPointID2 == -1) {
                        // assign to second touch point
                        touchPointID2 = id;
                    } else {
                        // 2 touch points assigned
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void notifyEndTouchEvent(long time) {
        lastTouchEventTime = time;
        if (currentTouchCount != trackers.size()) {
            throw new RuntimeException("Error in Rotate gesture recognition: "
                    + "touch count is wrong: " + currentTouchCount);
        }

        if (currentTouchCount == 0) {
            if (state == RotateRecognitionState.ACTIVE) {
                sendRotateFinishedEvent();
            }
            if (ROTATION_INERTIA_ENABLED && (state == RotateRecognitionState.PRE_INERTIA || state == RotateRecognitionState.ACTIVE)) {
                double timeFromLastRotation = ((double)time - rotationStartTime) / 1000000;
                if (timeFromLastRotation < 300) {
                    state = RotateRecognitionState.INERTIA;
                    // activate inertia
                    inertiaLastTime = 0;
                    if (initialInertiaRotationVelocity > MAX_INITIAL_VELOCITY)
                        initialInertiaRotationVelocity = MAX_INITIAL_VELOCITY;
                    else if (initialInertiaRotationVelocity < -MAX_INITIAL_VELOCITY)
                        initialInertiaRotationVelocity = -MAX_INITIAL_VELOCITY;

                    inertiaTimeline.getKeyFrames().setAll(
                        new KeyFrame(
                            Duration.millis(0),
                            new KeyValue(inertiaRotationVelocity, initialInertiaRotationVelocity, Interpolator.LINEAR)),
                        new KeyFrame(
                            Duration.millis(ROTATION_INERTIA_MILLIS * Math.abs(initialInertiaRotationVelocity) / MAX_INITIAL_VELOCITY),
                            event -> {
                                //stop inertia
                                reset();
                            },
                            new KeyValue(inertiaRotationVelocity, 0, Interpolator.LINEAR))
                        );
                    inertiaTimeline.playFromStart();
                } else {
                    reset();
                }
            }
        } else {
            // currentTouchCount >= 1
            if (touchPointsPressed && state == RotateRecognitionState.INERTIA) {
                //Stop inertia
                inertiaTimeline.stop();
                reset();
            }
            if (currentTouchCount == 1) {
                if (state == RotateRecognitionState.ACTIVE) {
                    sendRotateFinishedEvent();
                    if (ROTATION_INERTIA_ENABLED) {
                        //prepare for inertia
                        state = RotateRecognitionState.PRE_INERTIA;
                    } else {
                        reset();
                    }
                }
            } else {
                // currentTouchCount >= 2
                if (state == RotateRecognitionState.IDLE) {
                    state = RotateRecognitionState.TRACKING;
                    assignActiveTouchpoints();
                }

                calculateCenter();

                if (touchPointsSetChanged) {
                    assignActiveTouchpoints();
                }
                TouchPointTracker tp1 = trackers.get(touchPointID1);
                TouchPointTracker tp2 = trackers.get(touchPointID2);
                double newAngle = getAngle(tp1, tp2);

                if (touchPointsSetChanged) {
                    //No rotate event, just update the current angle. Keep total rotation
                    angleReference = newAngle;
                } else {
                    currentRotation = getNormalizedDelta(angleReference, newAngle);
                    if (state == RotateRecognitionState.TRACKING) {
                        if (Math.abs(currentRotation) > ROTATATION_THRESHOLD) {
                            state = RotateRecognitionState.ACTIVE;
                            sendRotateStartedEvent();
                        }
                    }

                    if (state == RotateRecognitionState.ACTIVE) {
                        totalRotation += currentRotation;
                        sendRotateEvent(false);
                        angleReference = newAngle;
                        double timePassed = ((double)time - rotationStartTime) / 1000000000;
                        if (timePassed > 1e-4) {
                            initialInertiaRotationVelocity = currentRotation / timePassed;
                            rotationStartTime = time;
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private void sendRotateStartedEvent() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.rotateEvent(RotateEvent.ROTATION_STARTED,
                    0, 0,
                    centerX, centerY,
                    centerAbsX, centerAbsY,
                    (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                    (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                    (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                    (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                    direct,
                    false /*inertia*/);
            }
            return null;
        }, scene.getAccessControlContext());
    }

    @SuppressWarnings("removal")
    private void sendRotateEvent(boolean isInertia) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.rotateEvent(RotateEvent.ROTATE,
                    currentRotation, totalRotation,
                    centerX, centerY,
                    centerAbsX, centerAbsY,
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
    private void sendRotateFinishedEvent() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.rotateEvent(RotateEvent.ROTATION_FINISHED,
                    0, totalRotation,
                    centerX, centerY,
                    centerAbsX, centerAbsY,
                    (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                    (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                    (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                    (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                    direct,
                    false /*inertia*/);
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
        if (state != RotateRecognitionState.FAILURE) {
            TouchPointTracker tracker = trackers.get(id);
            if (tracker == null) {
                // we don't know this ID, something went completely wrong
                state = RotateRecognitionState.FAILURE;
                throw new RuntimeException("Error in Rotate gesture "
                        + "recognition: released unknown touch point");
            }
            trackers.remove(id);
        }
        currentTouchCount--;
    }

    public void touchMoved(long id, long nanos, int x, int y, int xAbs, int yAbs) {
        if (state == RotateRecognitionState.FAILURE) {
            return;
        }

        TouchPointTracker tracker = trackers.get(id);
        if (tracker == null) {
            // we don't know this ID, something went completely wrong
            state = RotateRecognitionState.FAILURE;
            throw new RuntimeException("Error in rotate gesture "
                    + "recognition: reported unknown touch point");
        }
        tracker.update(nanos, x, y, xAbs, yAbs);
    }

    void reset() {
        state = RotateRecognitionState.IDLE;
        touchPointID1 = -1;
        touchPointID2 = -1;
        currentRotation = 0;
        totalRotation = 0;
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

    private enum RotateRecognitionState {
        IDLE,           // <2 touch points available
        TRACKING,       // 2+ touch points, angle is tracked
        ACTIVE,         // threshold accepted, gesture is started
        PRE_INERTIA,    // prepare for inertia
        INERTIA,        // inertia is active
        FAILURE
    }
}
