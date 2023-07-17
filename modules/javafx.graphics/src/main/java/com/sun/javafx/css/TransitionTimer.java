/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.animation.AnimationTimerHelper;
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
public abstract class TransitionTimer<T, P extends Property<T> & StyleableProperty<T>> extends AnimationTimer {

    private final P property;
    private Interpolator interpolator;
    private long startTime, endTime, delay, duration;
    private double reversingShorteningFactor;
    private long currentTime;
    private boolean updating;
    private boolean started;

    protected TransitionTimer(P property) {
        this.property = property;
    }

    /**
     * Adds the specified transition timer to the list of running timers and fires the
     * {@link TransitionEvent#RUN} event. If the combined duration of the transition is
     * zero or if the targeted node is not showing, no transition timer is started, no
     * event is dispatched, and this method returns {@code null}.
     */
    public static <T, P extends Property<T> & StyleableProperty<T>> TransitionTimer<T, P> run(
            TransitionTimer<T, P> timer, TransitionDefinition transition) {
        long now = AnimationTimerHelper.getPrimaryTimer(timer).nanos();
        timer.interpolator = transition.getInterpolator();
        timer.delay = (long)(transition.getDelay().toMillis() * 1_000_000);
        timer.duration = (long)(transition.getDuration().toMillis() * 1_000_000);
        timer.currentTime = now;
        timer.startTime = now + timer.delay;
        timer.endTime = timer.startTime + timer.duration;
        timer.reversingShorteningFactor = 1;
        P property = timer.getProperty();
        long combinedDuration = Math.max(timer.duration, 0) + timer.delay;

        // The transition timer is only started if the targeted node is showing, i.e. if it is part
        // of the scene graph and the node is visible.
        if (property.getBean() instanceof Node node && NodeHelper.isTreeShowing(node)) {
            TransitionTimer<?, ?> existingTimer = NodeHelper.findTransitionTimer(node, property);
            if (existingTimer != null) {
                if (combinedDuration > 0) {
                    adjustReversingTransition(existingTimer, timer, transition, now);
                }

                existingTimer.cancel();
            }

            if (combinedDuration > 0) {
                NodeHelper.addTransitionTimer(node, timer);
                fireTransitionEvent(timer, TransitionEvent.RUN);
                timer.start();
                return timer;
            }
        }

        // If the combined duration is zero or the node isn't showing, we just call onUpdate without
        // starting the timer. This updates the value of the target property to the end value, and no
        // events will be fired.
        timer.onUpdate(property, 1D);
        return null;
    }

    /**
     * Cancels the specified timer if it is currently running. If {@code forceStop} is {@code false}, the
     * timer will only be stopped if this method was not called from the timer's {@link #update(double)}
     * method (i.e. a timer will not stop itself while trying to set the new value of a styleable property).
     * If {@code timer} is {@code null}, it is considered to be trivially cancelled, so the
     * method returns {@code true}.
     *
     * @param timer the timer
     * @param forceStop if {@code true}, the timer is stopped unconditionally
     * @return {@code true} if the timer was cancelled or {@code timer} is {@code null},
     *         {@code false} otherwise
     */
    public static boolean cancel(TransitionTimer<?, ?> timer, boolean forceStop) {
        if (timer == null) {
            return true;
        }

        if (forceStop || !timer.pollUpdating()) {
            timer.cancel();
            return true;
        }

        return false;
    }

    /**
     * If a running transition is interrupted by a new transition, we adjust the start time and
     * end time of the new transition with the reversing shortening factor of the old transition.
     * Note that the reversing shortening factor is computed in output progress space (value),
     * not in input progress space (time).
     *
     * @see <a href="https://www.w3.org/TR/css-transitions-1/#reversing">Faster reversing of interrupted transitions</a>
     */
    private static void adjustReversingTransition(TransitionTimer<?, ?> existingTimer, TransitionTimer<?, ?> newTimer,
                                                  TransitionDefinition transition, long now) {
        double valueProgress = 0;
        double timeProgress = existingTimer.getTimeProgress();

        if (timeProgress > 0 && timeProgress < 1) {
            valueProgress = existingTimer.interpolator.interpolate(0D, 1D, timeProgress);
        }

        if (valueProgress > 0 && valueProgress < 1) {
            double newReversingShorteningFactor = valueProgress * existingTimer.reversingShorteningFactor
                    + (1 - existingTimer.reversingShorteningFactor);
            newTimer.reversingShorteningFactor = Utils.clamp(0, newReversingShorteningFactor, 1);
        }

        if (newTimer.delay < 0) {
            double adjustedDelay = transition.getDelay().toMillis() * newTimer.reversingShorteningFactor;
            newTimer.startTime = now + (long)(adjustedDelay * 1_000_000);
        }

        double adjustedDuration = transition.getDuration().toMillis() * newTimer.reversingShorteningFactor;
        newTimer.endTime = newTimer.startTime + (long)(adjustedDuration * 1_000_000);
    }

    private static <T, P extends Property<T> & StyleableProperty<T>> void fireTransitionEvent(
            TransitionTimer<T, P> timer, EventType<TransitionEvent> eventType) {
        try {
            double elapsedTime;

            // Elapsed time specification: https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            if (eventType == TransitionEvent.RUN || eventType == TransitionEvent.START) {
                elapsedTime = (double)Math.min(Math.max(-timer.delay, 0), timer.duration) / 1_000_000.0;
            } else if (eventType == TransitionEvent.CANCEL) {
                elapsedTime = (double)Math.max(0, timer.currentTime - timer.startTime) / 1_000_000.0;
            } else if (eventType == TransitionEvent.END) {
                elapsedTime = (double)timer.duration / 1_000_000.0;
            } else {
                throw new IllegalArgumentException("eventType");
            }

            Node node = (Node)timer.getProperty().getBean();
            node.fireEvent(new TransitionEvent(eventType, timer.getProperty(), Duration.millis(elapsedTime)));
        } catch (Throwable ex) {
            Thread currentThread = Thread.currentThread();
            currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
        }
    }

    /**
     * Gets the styleable property targeted by this timer, or {@code null} if the
     * targeted property was garbage-collected.
     */
    public final P getProperty() {
        return property;
    }

    /**
     * Polls whether the timer is currently updating the value of the property.
     * After this method is called, the {@link #updating} flag is {@code false}.
     */
    public final boolean pollUpdating() {
        boolean updating = this.updating;
        this.updating = false;
        return updating;
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
    public final void handle(long now) {
        currentTime = now;

        if (!started && now - startTime > 0) {
            started = true;
            fireTransitionEvent(this, TransitionEvent.START);
        }

        if (started) {
            double progress = getTimeProgress();
            if (progress >= 0) {
                update(progress);
            }

            if (progress == 1) {
                stop();
                fireTransitionEvent(this, TransitionEvent.END);
            }
        }
    }

    /**
     * Updates the transition timer by mapping the specified input progress to an output progress
     * value using the timer's interpolator, and then calling {@link #onUpdate(P, double)}} with the
     * output progress value.
     *
     * @param progress the input progress value
     */
    public final void update(double progress) {
        try {
            updating = true;
            onUpdate(property, interpolator.interpolate(0D, 1D, progress));
        } catch (Throwable ex) {
            Thread currentThread = Thread.currentThread();
            currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, ex);
        } finally {
            updating = false;
        }
    }

    /**
     * Skips the rest of a running transition and updates the property to the target value.
     * This happens when the targeted node is removed from the scene graph or becomes invisible.
     * <p>
     * Calling this method fires the {@link TransitionEvent#CANCEL} event.
     */
    public final void complete() {
        stop();
        update(1);
        fireTransitionEvent(this, TransitionEvent.CANCEL);
    }

    /**
     * Cancels the running transition without updating the target value.
     * This happens when the value of a CSS property targeted by a transition is changed by the user,
     * or if the transition is interrupted by another transition.
     * <p>
     * Calling this method fires the {@link TransitionEvent#CANCEL} event.
     */
    public final void cancel() {
        stop();
        fireTransitionEvent(this, TransitionEvent.CANCEL);
    }

    /**
     * Stops this {@code TransitionTimer} and removes it from the list of running timers.
     * This method is implicitly called by {@link #complete()} and {@link #cancel()}.
     * <p>
     * Calling this method does not fire a {@link TransitionEvent}.
     */
    @Override
    public final void stop() {
        super.stop();
        onStop(property);
        NodeHelper.removeTransitionTimer((Node)property.getBean(), this);
    }

    /**
     * Derived classes should implement this method to compute a new intermediate value
     * based on the current progress, and update the {@link StyleableProperty} accordingly.
     *
     * @param property the targeted {@code StyleableProperty}
     * @param progress the progress of the transition, ranging from 0 to 1
     */
    protected abstract void onUpdate(P property, double progress);

    /**
     * Occurs when the timer has stopped and should be discarded.
     * Derived classes should implement this method to clear any references to this timer.
     *
     * @param property the targeted {@code StyleableProperty}
     */
    protected abstract void onStop(P property);

    /**
     * Gets the progress of this timer along the time axis, ranging from 0 to 1.
     */
    private double getTimeProgress() {
        if (currentTime <= startTime) {
            return 0.0;
        }

        if (currentTime < endTime) {
            return (double)(currentTime - startTime) / (double)(endTime - startTime);
        }

        return 1.0;
    }

}
