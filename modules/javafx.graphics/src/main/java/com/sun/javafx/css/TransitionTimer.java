/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.animation.InterpolatorHelper;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.util.Utils;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.beans.property.Property;
import javafx.css.StyleableProperty;
import javafx.css.TransitionEvent;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * {@code TransitionTimer} is the base class for timers that compute intermediate
 * values for implicit transitions of a {@link StyleableProperty}.
 */
public final class TransitionTimer extends AnimationTimer {

    /**
     * A token that can be used to cancel a running timer.
     */
    public interface CancellationToken {
        void cancel();
    }

    private final Node targetNode;
    private final String targetPropertyName;
    private final Interpolator interpolator;
    private final TransitionMediator mediator;
    private double reversingShorteningFactor;
    private long startTime, endTime, delay, duration; // in nanoseconds
    private long currentTime; // in nanoseconds
    private boolean started;

    private TransitionTimer(TransitionMediator mediator,
                            TransitionDefinition definition,
                            String targetPropertyName,
                            long nanoNow) {
        this.delay = millisToNanos(definition.delay().toMillis());
        this.duration = millisToNanos(definition.duration().toMillis());
        this.targetNode = (Node)((Property<?>)mediator.getStyleableProperty()).getBean();
        this.targetPropertyName = targetPropertyName;
        this.interpolator = definition.interpolator();
        this.mediator = mediator;
        this.currentTime = nanoNow;
        this.startTime = nanoNow + delay;
        this.endTime = startTime + duration;
        this.reversingShorteningFactor = 1;
    }

    /**
     * Starts the specified transition timer with the specified transition definition.
     * If the combined duration of the transition is zero or if the targeted node is not
     * showing, no transition timer is started, no events are dispatched, and this method
     * returns {@code null}.
     *
     * @param mediator the {@code TransitionMediator} for the targeted property
     * @param definition the {@code TransitionDefinition} used to initialize the {@code timer}
     * @param targetPropertyName the name of the CSS property targeted by the transition
     * @param nanoNow the current time in nanoseconds
     * @return a {@code CancellationToken} if the timer was started, {@code null} otherwise
     */
    public static CancellationToken run(TransitionMediator mediator,
                                        TransitionDefinition definition,
                                        String targetPropertyName,
                                        long nanoNow) {
        // The transition timer is only started if the targeted node is showing, i.e. if it is part
        // of the scene graph and the node is visible.
        if (!(mediator.getStyleableProperty() instanceof Property<?> property)
                || !(property.getBean() instanceof Node node)
                || !NodeHelper.isTreeShowing(node)) {
            return null;
        }

        long delay = millisToNanos(definition.delay().toMillis());
        long duration = millisToNanos(definition.duration().toMillis());
        long combinedDuration = Math.max(duration, 0) + delay;

        var existingTimer = NodeHelper.findTransitionTimer(node, targetPropertyName);
        if (existingTimer != null) {
            if (combinedDuration > 0) {
                var newTimer = new TransitionTimer(mediator, definition, targetPropertyName, nanoNow);

                // If we already have a timer for the styleable property, the new timer might be a reversing
                // timer that needs to be adjusted by the reversing shortening algorithm.
                if (mediator.updateReversingAdjustedStartValue(existingTimer.getMediator())) {
                    newTimer.adjustReversingTimings(existingTimer);
                }

                existingTimer.interrupt();
                newTimer.start();
                return newTimer::stop;
            }

            existingTimer.stop();
            return null;
        }

        // We only need a timer if the combined duration is non-zero.
        if (combinedDuration > 0) {
            var timer = new TransitionTimer(mediator, definition, targetPropertyName, nanoNow);
            timer.start();
            return timer::stop;
        }

        return null;
    }

    /**
     * Returns the {@code TransitionMediator} associated with this timer.
     *
     * @return the {@code TransitionMediator}
     */
    public TransitionMediator getMediator() {
        return mediator;
    }

    /**
     * Called once per frame to update the {@code TransitionTimer}.
     * <p>
     * This method fires {@link TransitionEvent#START} when the timer enters its active
     * interval, and {@link TransitionEvent#END} event when the timer has reached the end
     * of its active interval.
     *
     * @param now the timestamp of the current frame, in nanoseconds
     */
    @Override
    public void handle(long now) {
        currentTime = Math.min(now, endTime);

        if (!started && currentTime >= startTime) {
            started = true;
            fireTransitionEvent(TransitionEvent.START);
        }

        if (started) {
            double progress = getProgress();
            if (progress >= 0) {
                update(progress);
            }

            if (progress == 1) {
                stopTimer(TransitionEvent.END);
            }
        }
    }

    /**
     * Starts this timer, and adds it to the list of running transitions.
     */
    @Override
    public void start() {
        super.start();
        NodeHelper.addTransitionTimer(targetNode, targetPropertyName, this);
        fireTransitionEvent(TransitionEvent.RUN);
    }

    /**
     * Stops this timer without updating the property to the target value.
     * This happens when the value of the styleable property is changed by the user, or when a
     * running timer is cancelled by a transition with zero duration.
     */
    @Override
    public void stop() {
        stopTimer(TransitionEvent.CANCEL);
    }

    /**
     * Skips the rest of a running transition and updates the property to the target value.
     * This happens when the targeted node is removed from the scene graph or becomes invisible.
     */
    public void complete() {
        update(1);
        stopTimer(TransitionEvent.CANCEL);
    }

    /**
     * Stops this timer without invoking {@link TransitionMediator#onStop()}.
     * This form of completion only happens when a timer is interrupted by a reversing timer.
     */
    private void interrupt() {
        super.stop();
        NodeHelper.removeTransitionTimer(targetNode, targetPropertyName);
        fireTransitionEvent(TransitionEvent.CANCEL);
    }

    /**
     * Stops the running timer and fires the specified event.
     *
     * @param eventType the event type that is fired after the timer is stopped
     */
    private void stopTimer(EventType<TransitionEvent> eventType) {
        super.stop();
        mediator.onStop();
        NodeHelper.removeTransitionTimer(targetNode, targetPropertyName);
        fireTransitionEvent(eventType);
    }

    /**
     * Updates the transition mediator by mapping the specified input progress to an output progress
     * value using the timer's interpolator, and then calling {@link TransitionMediator#onUpdate(double)}}
     * with the output progress value.
     *
     * @param progress the input progress value
     */
    private void update(double progress) {
        try {
            mediator.onUpdate(InterpolatorHelper.curve(interpolator, progress));
        } catch (Throwable ex) {
            Thread currentThread = Thread.currentThread();
            currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
        }
    }

    /**
     * Gets the progress of this timer along the input progress axis.
     *
     * @return the input progress, ranging from 0 to 1
     */
    private double getProgress() {
        if (currentTime <= startTime) {
            return 0.0;
        }

        if (currentTime < endTime) {
            return (double)(currentTime - startTime) / (double)(endTime - startTime);
        }

        return 1.0;
    }

    /**
     * If a running transition is interrupted by a new transition, we adjust the start time and
     * end time of the new transition with the reversing shortening factor of the old transition.
     * Note that the reversing shortening factor is computed in output progress space (value),
     * not in input progress space (time).
     * <p>
     * This algorithm fixes transition asymmetries that can happen when a transition is interrupted
     * by a reverse transition. Consider a linear transition that animates a value over 4s, but is
     * interrupted after 1s. When the transition is interrupted, the value has progressed one
     * quarter of the value space in one quarter of the duration. However, the reverse transition
     * now takes the entire duration (4s) to progress just one quarter of the original value space,
     * which means that the transition speed is much slower than what would be expected.
     *
     * @param existingTimer the timer of the running transition
     * @see <a href="https://www.w3.org/TR/css-transitions-1/#reversing">Faster reversing of interrupted transitions</a>
     */
    private void adjustReversingTimings(TransitionTimer existingTimer) {
        double progress = InterpolatorHelper.curve(existingTimer.interpolator, existingTimer.getProgress());

        if (progress > 0 && progress < 1) {
            double oldFactor = existingTimer.reversingShorteningFactor;
            double newFactor = progress * oldFactor + (1 - oldFactor);
            reversingShorteningFactor = Utils.clamp(0, newFactor, 1);
        }

        if (delay < 0) {
            delay = (long)(delay * reversingShorteningFactor);
            startTime = currentTime + delay;
        }

        duration = (long)(duration * reversingShorteningFactor);
        endTime = startTime + duration;
    }

    /**
     * Fires a {@link TransitionEvent} of the specified type.
     * The elapsed time is computed according to the CSS Transitions specification.
     *
     * @param eventType the event type
     */
    private void fireTransitionEvent(EventType<TransitionEvent> eventType) {
        try {
            long elapsedTime; // nanoseconds

            // Elapsed time specification: https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            if (eventType == TransitionEvent.RUN || eventType == TransitionEvent.START) {
                elapsedTime = Math.min(Math.max(-delay, 0), duration);
            } else if (eventType == TransitionEvent.CANCEL) {
                elapsedTime = Math.max(0, currentTime - startTime);
            } else if (eventType == TransitionEvent.END) {
                elapsedTime = duration;
            } else {
                throw new IllegalArgumentException("eventType");
            }

            targetNode.fireEvent(
                new TransitionEvent(
                    eventType,
                    mediator.getStyleableProperty(),
                    targetPropertyName,
                    Duration.millis(nanosToMillis(elapsedTime))));
        } catch (Throwable ex) {
            Thread currentThread = Thread.currentThread();
            currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
        }
    }

    /**
     * Converts the specified duration in nanoseconds to fractional milliseconds.
     *
     * @param nanos the duration in nanoseconds
     * @return the duration in fractional milliseconds
     */
    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    /**
     * Converts the specified duration in fractional milliseconds to nanoseconds.
     *
     * @param millis the duration in fractional milliseconds
     * @return the duration in nanoseconds
     */
    private static long millisToNanos(double millis) {
        return (long)(millis * 1_000_000.0);
    }
}
