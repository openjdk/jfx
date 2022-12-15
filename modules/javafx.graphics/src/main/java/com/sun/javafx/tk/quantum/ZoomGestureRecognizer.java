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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.TouchEvent;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.ZoomEvent;
import javafx.util.Duration;

class ZoomGestureRecognizer implements GestureRecognizer {
    private static final double ZOOM_INERTIA_MILLIS = 500;
    private static final double MAX_ZOOM_IN_FACTOR = 10;
    private static final double MAX_ZOOM_OUT_FACTOR = 0.1;
    private static final long ZOOM_INERTIA_THRESHOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(200);

    // gesture will be activated if |zoomFactor - 1| > zoomFactorThreshold
    private static double zoomFactorThreshold = 0.1;
    private static boolean zoomInertiaEnabled = true;

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            String s = System.getProperty("com.sun.javafx.gestures.zoom.threshold");
            if (s != null) {
                zoomFactorThreshold = Double.valueOf(s);
            }
            s = System.getProperty("com.sun.javafx.gestures.zoom.inertia");
            if (s != null) {
                zoomInertiaEnabled = Boolean.valueOf(s);
            }
            return null;
        });
    }

    private final Timeline inertiaTimeline = new Timeline();
    private final DoubleProperty inertiaZoomVelocity = new SimpleDoubleProperty();
    private final Map<Long, TouchPointTracker> trackers = new HashMap<>();

    private ViewScene scene;
    private double initialInertiaZoomVelocity;
    private long zoomStartNanos;

    private ZoomRecognitionState state = ZoomRecognitionState.IDLE;
    private int modifiers;
    private boolean direct;

    private int currentTouchCount = 0;
    private boolean touchPointsSetChanged;
    private boolean touchPointsPressed;

    private double centerX, centerY;
    private double centerAbsX, centerAbsY;
    private double distanceReference;
    private double zoomFactor = 1.0;
    private double totalZoomFactor = 1.0;
    private double inertiaLastTime;

    ZoomGestureRecognizer(final ViewScene scene) {
        this.scene = scene;
        inertiaZoomVelocity.addListener(valueModel -> {
            double currentTime = inertiaTimeline.getCurrentTime().toSeconds();
            double timePassed = currentTime - inertiaLastTime;
            inertiaLastTime = currentTime;
            double prevTotalZoomFactor = totalZoomFactor;
            totalZoomFactor += timePassed * inertiaZoomVelocity.get(); // zoom += dz/dt * time
            zoomFactor = totalZoomFactor / prevTotalZoomFactor;

            //send inertia zoom event
            sendZoomEvent(true);
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
                touchPressed(touchId, x, y, xAbs, yAbs);
                break;
            case TouchEvent.TOUCH_STILL:
                break;
            case TouchEvent.TOUCH_MOVED:
                touchMoved(touchId, x, y, xAbs, yAbs);
                break;
            case TouchEvent.TOUCH_RELEASED:
                touchPointsSetChanged = true;
                touchReleased(touchId);
                break;
            default:
                throw new RuntimeException("Error in Zoom gesture recognition: "
                        + "unknown touch state: " + state);
        }
    }

    private void calculateCenter() {
        if (currentTouchCount <= 0) {
            throw new RuntimeException("Error in Zoom gesture recognition: "
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

    private double calculateMaxDistance() {
        //calculate max square distance from a touch point to the center
        double maxSquareDist = 0.0;
        for (TouchPointTracker tracker : trackers.values()) {
            double deltaX = tracker.getAbsX() - centerAbsX;
            double deltaY = tracker.getAbsY() - centerAbsY;

            double squareDist = deltaX * deltaX + deltaY * deltaY;
            if (squareDist > maxSquareDist) {
                maxSquareDist = squareDist;
            }
        }
        return Math.sqrt(maxSquareDist);
    }

    @Override
    public void notifyEndTouchEvent(long nanos) {
        if (currentTouchCount != trackers.size()) {
            throw new RuntimeException("Error in Zoom gesture recognition: "
                    + "touch count is wrong: " + currentTouchCount);
        }

        if (currentTouchCount == 0) {
            if (state == ZoomRecognitionState.ACTIVE) {
                sendZoomFinishedEvent();
            }
            if (zoomInertiaEnabled && (state == ZoomRecognitionState.PRE_INERTIA || state == ZoomRecognitionState.ACTIVE)) {
                long nanosSinceLastZoom = nanos - zoomStartNanos;

                if (initialInertiaZoomVelocity != 0 && nanosSinceLastZoom < ZOOM_INERTIA_THRESHOLD_NANOS) {
                    state = ZoomRecognitionState.INERTIA;
                    // activate inertia
                    inertiaLastTime = 0;
                    double duration = ZOOM_INERTIA_MILLIS / 1000;
                    double newZoom = totalZoomFactor + initialInertiaZoomVelocity * duration;
                    if (initialInertiaZoomVelocity > 0) {
                        //zoom in
                        if (newZoom / totalZoomFactor > MAX_ZOOM_IN_FACTOR) {
                            newZoom = totalZoomFactor * MAX_ZOOM_IN_FACTOR;
                            duration = (newZoom - totalZoomFactor) / initialInertiaZoomVelocity;
                        }
                    } else {
                        //zoom out
                        if (newZoom / totalZoomFactor < MAX_ZOOM_OUT_FACTOR) {
                            newZoom = totalZoomFactor * MAX_ZOOM_OUT_FACTOR;
                            duration = (newZoom - totalZoomFactor) / initialInertiaZoomVelocity;
                        }
                    }

                    inertiaTimeline.getKeyFrames().setAll(
                        new KeyFrame(
                            Duration.millis(0),
                            new KeyValue(inertiaZoomVelocity, initialInertiaZoomVelocity, Interpolator.LINEAR)),
                        new KeyFrame(
                            Duration.seconds(duration),
                            event -> reset(),  // stop inertia
                            new KeyValue(inertiaZoomVelocity, 0, Interpolator.LINEAR))
                        );
                    inertiaTimeline.playFromStart();
                } else {
                    reset();
                }
            } else {
                reset();
            }
        } else {
            // currentTouchCount >= 1
            if (touchPointsPressed && state == ZoomRecognitionState.INERTIA) {
                //Stop inertia
                inertiaTimeline.stop();
                reset();
            }

            if (currentTouchCount == 1) {
                if (state == ZoomRecognitionState.ACTIVE) {
                    sendZoomFinishedEvent();
                    if (zoomInertiaEnabled) {
                        //prepare for inertia
                        state = ZoomRecognitionState.PRE_INERTIA;
                    } else {
                        reset();
                    }
                }

            } else {
                // currentTouchCount >= 2
                if (state == ZoomRecognitionState.IDLE) {
                    state = ZoomRecognitionState.TRACKING;
                    zoomStartNanos = nanos;
                }

                calculateCenter();
                double currentDistance = calculateMaxDistance();

                if (touchPointsSetChanged) {
                    //No zoom event.
                    //Just update the distance reference. Keep the total zoomfactor
                    distanceReference = currentDistance;
                } else {
                    zoomFactor = currentDistance / distanceReference;
                    if (state == ZoomRecognitionState.TRACKING) {
                        if ( Math.abs(zoomFactor - 1) > zoomFactorThreshold) {
                            state = ZoomRecognitionState.ACTIVE;
                            sendZoomStartedEvent();
                        }
                    }
                    if (state == ZoomRecognitionState.ACTIVE) {
                        double prevTotalZoomFactor = totalZoomFactor;
                        totalZoomFactor *= zoomFactor;
                        sendZoomEvent(false);
                        distanceReference = currentDistance;
                        long nanosPassed = nanos - zoomStartNanos;

                        if (nanosPassed > INITIAL_VELOCITY_THRESHOLD_NANOS) {
                            initialInertiaZoomVelocity = (totalZoomFactor - prevTotalZoomFactor) / nanosPassed * NANOS_TO_SECONDS;
                            zoomStartNanos = nanos;
                        } else {
                            initialInertiaZoomVelocity = 0;
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private void sendZoomStartedEvent() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.zoomEvent(ZoomEvent.ZOOM_STARTED,
                    1, 1,
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
    private void sendZoomEvent(boolean isInertia) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.zoomEvent(ZoomEvent.ZOOM,
                    zoomFactor, totalZoomFactor,
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
    private void sendZoomFinishedEvent() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (scene.sceneListener != null) {
                scene.sceneListener.zoomEvent(ZoomEvent.ZOOM_FINISHED,
                    1, totalZoomFactor,
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

    private void params(int modifiers, boolean direct) {
        this.modifiers = modifiers;
        this.direct = direct;
    }

    private void touchPressed(long id, int x, int y, int xAbs, int yAbs) {
        currentTouchCount++;
        TouchPointTracker tracker = new TouchPointTracker();
        tracker.update(x, y, xAbs, yAbs);
        trackers.put(id, tracker);
    }

    private void touchReleased(long id) {
        if (state != ZoomRecognitionState.FAILURE) {
            TouchPointTracker tracker = trackers.get(id);
            if (tracker == null) {
                // we don't know this ID, something went completely wrong
                state = ZoomRecognitionState.FAILURE;
                throw new RuntimeException("Error in Zoom gesture "
                        + "recognition: released unknown touch point");
            }
            trackers.remove(id);
        }
        currentTouchCount--;
    }

    private void touchMoved(long id, int x, int y, int xAbs, int yAbs) {
        if (state == ZoomRecognitionState.FAILURE) {
            return;
        }

        TouchPointTracker tracker = trackers.get(id);
        if (tracker == null) {
            // we don't know this ID, something went completely wrong
            state = ZoomRecognitionState.FAILURE;
            throw new RuntimeException("Error in zoom gesture "
                    + "recognition: reported unknown touch point");
        }
        tracker.update(x, y, xAbs, yAbs);
    }

    private void reset() {
        state = ZoomRecognitionState.IDLE;
        zoomFactor = 1.0;
        totalZoomFactor = 1.0;
    }

    private static class TouchPointTracker {
        double x, y;
        double absX, absY;

        public void update(double x, double y, double absX, double absY) {
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

    private enum ZoomRecognitionState {
        IDLE,       // <2 touch points available
        TRACKING,   // 2+ touch points, distance is tracked
        ACTIVE,     // threshold accepted, gesture is started
        PRE_INERTIA,    // prepare for inertia
        INERTIA,        // inertia is active
        FAILURE
    }
}
