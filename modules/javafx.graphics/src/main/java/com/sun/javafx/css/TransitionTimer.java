/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.css.TransitionDefinition;
import javafx.css.TransitionEvent;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * {@code TransitionTimer} is the base class for timers that compute intermediate
 * values for implicit transitions of a {@link StyleableProperty}.
 */
public abstract class TransitionTimer extends AnimationTimer {

    private final long startTime, delay, duration;
    private final Interpolator interpolator;
    private boolean updating;
    private boolean started;

    protected TransitionTimer(TransitionDefinition transition) {
        this.delay = (long)(transition.getDelay().toMillis() * 1000000);
        this.startTime = AnimationTimerHelper.getPrimaryTimer(this).nanos() + delay;
        this.duration = (long)(transition.getDuration().toMillis() * 1000000);
        this.interpolator = transition.getInterpolator();
    }

    /**
     * Adds the specified transition timer to the list of running timers
     * and fires the {@link TransitionEvent#RUN} event.
     */
    public static TransitionTimer run(Property<?> property, TransitionTimer timer) {
        timer.start();

        if (property.getBean() instanceof Node node) {
            NodeHelper.addTransitionTimer(node, timer);

            if (property instanceof StyleableProperty<?> styleableProperty) {
                // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
                // The elapsed time for this event is equal to min(max(-'delay', 0), 'duration')
                double elapsed = (double)Math.min(Math.max(-timer.delay, 0), timer.duration) / 1_000_000.0;
                node.fireEvent(new TransitionEvent(TransitionEvent.RUN, styleableProperty, Duration.millis(elapsed)));
            }
        }

        return timer;
    }

    /**
     * Stops the specified timer if it is currently running, but only if this method
     * was not called from the timer's {@link #onUpdate(double)} method (i.e. a timer
     * will not stop itself).
     * If {@code timer} is {@code null}, it is considered to be trivially stopped, so
     * the method returns {@code true}.
     *
     * @param timer the timer
     * @return {@code true} if the timer was stopped or {@code timer} is {@code null},
     *         {@code false} otherwise
     */
    public static boolean tryStop(TransitionTimer timer) {
        if (timer == null) {
            return true;
        }

        if (timer.updating) {
            return false;
        }

        timer.stop();
        return true;
    }

    @Override
    public final void handle(long now) {
        if (!started && now - startTime > 0) {
            started = true;

            // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            // The elapsed time for this event is equal to min(max(-'delay', 0), 'duration')
            double elapsed = (double)Math.min(Math.max(-delay, 0), duration) / 1_000_000.0;
            fireEvent(TransitionEvent.START, Duration.millis(elapsed));
        }

        double progress = Utils.clamp(
            interpolator.isValidBeforeInterval() ? Double.NEGATIVE_INFINITY : 0,
            (double)(now - startTime) / (double)duration,
            1);

        update(progress);
    }

    /**
     * Updates the transition timer by mapping the specified input progress to an output progress
     * value using the timer's interpolator, and then calling {@link #onUpdate(double)}} with the
     * output progress value.
     * When the specified input progress value is 1, the timer is automatically stopped and the
     * {@link TransitionEvent#END} event is fired.
     *
     * @param progress the input progress value
     */
    public final void update(double progress) {
        try {
            updating = true;
            onUpdate(interpolator.interpolate(0D, 1D, progress));
        } finally {
            updating = false;
        }

        if (progress == 1) {
            stop();

            // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            // The elapsedTime for this event is equal to the value of 'duration'.
            fireEvent(TransitionEvent.END, Duration.millis((double)duration / 1_000_000.0));
        }
    }

    /**
     * Skips the rest of a running transition and updates the value to the target value.
     * Calling this method fires the {@link TransitionEvent#CANCEL} event.
     */
    public final void cancel() {
        try {
            updating = true;
            onUpdate(interpolator.interpolate(0D, 1D, 1D));
        } finally {
            updating = false;
        }

        stop();

        // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
        // The elapsedTime for this event is equal to the number of seconds from the end
        // of the transition's delay to the moment when the transition was canceled.
        long elapsed = AnimationTimerHelper.getPrimaryTimer(this).nanos() - startTime;
        fireEvent(TransitionEvent.CANCEL, Duration.millis((double)Math.max(0, elapsed) / 1_000_000.0));
    }

    @Override
    public void stop() {
        super.stop();

        Property<?> property = getProperty();
        if (property != null && property.getBean() instanceof Node node) {
            NodeHelper.removeTransitionTimer(node, this);
        }
    }

    protected abstract Property<?> getProperty();

    /**
     * Derived classes should implement this method to compute a new intermediate value
     * based on the current progress, and update the {@code StyleableProperty} accordingly.
     *
     * @param progress the progress of the transition, ranging from 0 to 1
     */
    protected abstract void onUpdate(double progress);

    private void fireEvent(EventType<TransitionEvent> eventType, Duration elapsedTime) {
        Property<?> property = getProperty();
        if (property != null
                && property.getBean() instanceof Node node
                && property instanceof StyleableProperty<?> styleableProperty) {
            node.fireEvent(new TransitionEvent(eventType, styleableProperty, elapsedTime));
        }
    }

}
