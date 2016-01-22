/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble;

import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

public class ScrollEventSynthesizer implements EventHandler {
    private static final int INERTIA_DURATION = 2400;
    private static final double CLICK_THRESHOLD = 20;
    private static final double CLICK_TIME_THRESHOLD = Integer.parseInt(System.getProperty("click", "400"));
    private long startDrag;
    private long lastDrag;
    private long lastDragDelta;
    private int startDragX;
    private int startDragY;
    private int lastDragX;
    private int lastDragY;
    private int lastDragStepX;
    private int lastDragStepY;
    private double dragVelocityX;
    private double dragVelocityY;
    private boolean clickThresholdBroken;
    private Timeline inertiaTimeline = null;
    private long lastClickTime = -1;
    private boolean isFiredByMe = false;

    public ScrollEventSynthesizer(Scene scene) {
        scene.addEventFilter(MouseEvent.ANY, this);
        scene.addEventFilter(ScrollEvent.ANY, this);
    }

    @Override public void handle(final Event e) {
        if (isFiredByMe) return;
        if (e instanceof ScrollEvent) {
            final ScrollEvent se = (ScrollEvent)e;
//            System.out.println("SCROLL touch = "+se.getTouchCount()+" target = "+se.getTarget()+" e = "+se.getEventType()+"  dx="+se.getDeltaX()+"  dy="+se.getDeltaY()+"  tdx="+se.getTotalDeltaX()+"  tdy="+se.getTotalDeltaY());
            if (se.getTouchCount() != 0) se.consume();
        } else if (e instanceof MouseEvent) {
            final MouseEvent me = (MouseEvent)e;
//            System.out.println("MOUSE "+e.getEventType()+" --> "+e.getTarget());
            if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                lastDragX = startDragX = (int)me.getX();
                lastDragY = startDragY = (int)me.getY();
                lastDrag = startDrag = System.currentTimeMillis();
                lastDragDelta = 0;
                if(inertiaTimeline != null) inertiaTimeline.stop();
                clickThresholdBroken = false;
            } else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                // Delta of this drag vs. last drag location (or start)
                lastDragStepX = (int)me.getX() - lastDragX;
                lastDragStepY = (int)me.getY() - lastDragY;

                // Duration of this drag step.
                lastDragDelta = System.currentTimeMillis() - lastDrag;

                // Velocity of last drag increment.
                dragVelocityX = (double)lastDragStepX/(double)lastDragDelta;
                dragVelocityY = (double)lastDragStepY/(double)lastDragDelta;

                // Snapshot of this drag event.
                lastDragX = (int)me.getX();
                lastDragY = (int)me.getY();
                lastDrag = System.currentTimeMillis();

                // Calculate distance so far -- have we dragged enough to scroll?
                final int dragX = (int)me.getX() - startDragX;
                final int dragY = (int)me.getY() - startDragY;
                double distance = Math.abs(Math.sqrt((dragX*dragX) + (dragY*dragY)));

                int scrollDistX = lastDragStepX;
                int scrollDistY = lastDragStepY;
                if (!clickThresholdBroken && distance > CLICK_THRESHOLD) {
                    clickThresholdBroken = true;
                    scrollDistX = dragX;
                    scrollDistY = dragY;
                }
                if (clickThresholdBroken) {
                    Event.fireEvent(e.getTarget(), new ScrollEvent(
                            ScrollEvent.SCROLL,
                            me.getX(), me.getY(),
                            me.getSceneX(), me.getSceneY(),
                            me.isShiftDown(), me.isControlDown(), me.isAltDown(), me.isMetaDown(), true, false,
                            scrollDistX, scrollDistY,
                            scrollDistX, scrollDistY,
                            ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                            ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                            0,new PickResult(me.getTarget(), me.getSceneX(), me.getSceneY())
                            ));

                    /*
                     * final EventType<ScrollEvent> eventType,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia,
            double deltaX, double deltaY,
            double gestureDeltaX, double gestureDeltaY,
            HorizontalTextScrollUnits textDeltaXUnits, double textDeltaX,
            VerticalTextScrollUnits textDeltaYUnits, double textDeltaY,
            int touchCount,
            PickResult pickResult)
                     */
                }
            } else if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
                handleRelease(me);
            } else if (e.getEventType() == MouseEvent.MOUSE_CLICKED) {
                final long time = System.currentTimeMillis();
//                System.out.println("CLICKED   clickThresholdBroken="+clickThresholdBroken+"   timeSinceLast = "+ (time-lastClickTime)+" CONSUMED="+((time-lastClickTime) < CLICK_TIME_THRESHOLD));
                if (clickThresholdBroken || (lastClickTime != -1 && (time-lastClickTime) < CLICK_TIME_THRESHOLD)) e.consume();
                lastClickTime = time;
            }
        }
    }

    private void handleRelease(final MouseEvent me) {
        if (clickThresholdBroken) {
            // Calculate last instantaneous velocity. User may have stopped moving
            // before they let go of the mouse.
            final long time = System.currentTimeMillis() - lastDrag;
            dragVelocityX = (double)lastDragStepX/(time + lastDragDelta);
            dragVelocityY = (double)lastDragStepY/(time + lastDragDelta);

            // determin if click or drag/flick
            final int dragX = (int)me.getX() - startDragX;
            final int dragY = (int)me.getY() - startDragY;

            // calculate complete time from start to end of drag
            final long totalTime = System.currentTimeMillis() - startDrag;

            // if time is less than 300ms then considered a quick flick and whole time is used
            final boolean quick = totalTime < 300;

            // calculate velocity
            double velocityX = quick ? (double)dragX / (double)totalTime : dragVelocityX; // pixels/ms
            double velocityY = quick ? (double)dragY / (double)totalTime : dragVelocityY; // pixels/ms

//            System.out.printf("dragVelocityX = %f, dragVelocityY = %f\n", dragVelocityX, dragVelocityY);
//            System.out.printf("velocityX = %f, dragX = %d; velocityY = %f, dragY = %d; totalTime = %d\n",
//                    velocityX, dragX, velocityY, dragY, totalTime);

            // calculate distance we would travel at this speed for INERTIA_DURATION ms of travel
            final int distanceX = (int)(velocityX * INERTIA_DURATION); // distance
            final int distanceY = (int)(velocityY * INERTIA_DURATION); // distance
            //
            DoubleProperty animatePosition = new SimpleDoubleProperty() {
                double lastMouseX = me.getX();
                double lastMouseY = me.getY();
                @Override protected void invalidated() {
                    final double mouseX = me.getX() + (distanceX * get());
                    final double mouseY = me.getY() + (distanceY * get());
                    final double dragStepX = mouseX - lastMouseX;
                    final double dragStepY = mouseY - lastMouseY;

                    if (Math.abs(dragStepX) >= 1.0 || Math.abs(dragStepY) >= 1.0) {
                        Event.fireEvent(me.getTarget(), new ScrollEvent(
                                ScrollEvent.SCROLL,
                                me.getX(), me.getY(),
                                me.getSceneX(), me.getSceneY(),
                                me.isShiftDown(), me.isControlDown(), me.isAltDown(), me.isMetaDown(),
                                true, true,
                                dragStepX, dragStepY,
                                (distanceX * get()), (distanceY * get()),
                                ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                                ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                                0,new PickResult(me.getTarget(), me.getSceneX(), me.getSceneY())
                                ));
                    }
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                }
            };

            // animate a slow down from current velocity to zero
            inertiaTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(animatePosition, 0)),
                    new KeyFrame(Duration.millis(INERTIA_DURATION), new KeyValue(animatePosition, 1d,
                            Interpolator.SPLINE(0.0513, 0.1131, 0.1368, 1.0000)))
            );
            inertiaTimeline.play();
        }
    }
}
