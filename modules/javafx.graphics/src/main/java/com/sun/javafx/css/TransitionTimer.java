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
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.beans.property.Property;
import javafx.css.StyleableProperty;
import javafx.css.TransitionEvent;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.util.Duration;
import java.lang.ref.WeakReference;

/**
 * {@code TransitionTimer} is the base class for timers that compute intermediate
 * values for implicit transitions of a {@link StyleableProperty}.
 */
public abstract class TransitionTimer<T extends StyleableProperty<?>> extends AnimationTimer {

    private final WeakReference<T> wref;
    private final long startTime, delay, duration;
    private final Interpolator interpolator;
    private boolean updating;
    private boolean started;

    protected TransitionTimer(T property, TransitionDefinition transition) {
        this.wref = new WeakReference<>(property);
        this.delay = (long)(transition.getDelay().toMillis() * 1000000);
        this.startTime = AnimationTimerHelper.getPrimaryTimer(this).nanos() + delay;
        this.duration = (long)(transition.getDuration().toMillis() * 1000000);
        this.interpolator = transition.getInterpolator();
    }

    /**
     * Adds the specified transition timer to the list of running timers
     * and fires the {@link TransitionEvent#RUN} event.
     */
    public static <T extends StyleableProperty<?>> TransitionTimer<T> run(T property, TransitionTimer<T> timer) {
        timer.start();

        if (property instanceof Property<?> p && p.getBean() instanceof Node node) {
            NodeHelper.addTransitionTimer(node, timer);

            // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            // The elapsed time for this event is equal to min(max(-'delay', 0), 'duration')
            double elapsed = (double)Math.min(Math.max(-timer.delay, 0), timer.duration) / 1_000_000.0;
            node.fireEvent(new TransitionEvent(TransitionEvent.RUN, property, Duration.millis(elapsed)));
        }

        return timer;
    }

    /**
     * Stops the specified timer if it is currently running, but only if this method was not
     * called from the timer's {@link #update(double)} method (i.e. a timer will not stop itself
     * while trying to set the new value of a styleable property).
     * If {@code timer} is {@code null}, it is considered to be trivially stopped, so the
     * method returns {@code true}.
     *
     * @param timer the timer
     * @return {@code true} if the timer was stopped or {@code timer} is {@code null},
     *         {@code false} otherwise
     */
    public static boolean stop(TransitionTimer<?> timer, boolean forceStop) {
        if (timer == null) {
            return true;
        }

        if (forceStop || !timer.pollUpdating()) {
            timer.stop();
            return true;
        }

        return false;
    }

    /**
     * Polls whether the timer is currently updating the value of the property.
     * After this method was called, the {@link #updating} flag is {@code false}.
     */
    public boolean pollUpdating() {
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
        T property = wref.get();
        if (property == null) {
            super.stop();
            return;
        }

        if (!started && now - startTime > 0) {
            started = true;

            // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            // The elapsed time for this event is equal to min(max(-'delay', 0), 'duration')
            double elapsed = (double)Math.min(Math.max(-delay, 0), duration) / 1_000_000.0;
            fireEvent(property, TransitionEvent.START, Duration.millis(elapsed));
        }

        double progress = Math.min((double)(now - startTime) / (double)duration, 1.0);

        if (progress >= 0) {
            update(progress);
        }

        if (progress == 1) {
            stop();

            // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
            // The elapsedTime for this event is equal to the value of 'duration'.
            fireEvent(property, TransitionEvent.END, Duration.millis((double)duration / 1_000_000.0));
        }
    }

    /**
     * Updates the transition timer by mapping the specified input progress to an output progress
     * value using the timer's interpolator, and then calling {@link #onUpdate(T, double)}} with the
     * output progress value.
     *
     * @param progress the input progress value
     */
    public final void update(double progress) {
        T property = wref.get();
        if (property == null) {
            super.stop();
            return;
        }

        try {
            updating = true;
            onUpdate(property, interpolator.interpolate(0D, 1D, progress));
        } finally {
            updating = false;
        }
    }

    /**
     * Skips the rest of a running transition and updates the property to the target value.
     * Calling this method fires the {@link TransitionEvent#CANCEL} event.
     */
    public final void complete() {
        T property = wref.get();
        if (property == null) {
            super.stop();
            return;
        }

        try {
            updating = true;
            onUpdate(property, interpolator.interpolate(0D, 1D, 1D));
        } finally {
            updating = false;
        }

        stop();

        // https://www.w3.org/TR/css-transitions-1/#event-transitionevent
        // The elapsedTime for this event is equal to the number of seconds from the end
        // of the transition's delay to the moment when the transition was canceled.
        long elapsed = AnimationTimerHelper.getPrimaryTimer(this).nanos() - startTime;
        fireEvent(property, TransitionEvent.CANCEL, Duration.millis((double)Math.max(0, elapsed) / 1_000_000.0));
    }

    /**
     * Stops this {@code TransitionTimer} and removes it from the list of running timers.
     */
    @Override
    public final void stop() {
        super.stop();

        T property = wref.get();
        if (property != null) {
            onStop(property);
            if (property instanceof Property<?> p && p.getBean() instanceof Node node) {
                NodeHelper.removeTransitionTimer(node, this);
            }
        }
    }

    /**
     * Derived classes should implement this method to compute a new intermediate value
     * based on the current progress, and update the {@link StyleableProperty} accordingly.
     *
     * @param property the targeted {@code StyleableProperty}
     * @param progress the progress of the transition, ranging from 0 to 1
     */
    protected abstract void onUpdate(T property, double progress);

    /**
     * Occurs when the timer has stopped and should be discarded.
     * Derived classes should implement this method to clear any references to this timer.
     *
     * @param property the targeted {@code StyleableProperty}
     */
    protected abstract void onStop(T property);

    private void fireEvent(StyleableProperty<?> property, EventType<TransitionEvent> eventType, Duration elapsedTime) {
        if (property instanceof Property<?> p && p.getBean() instanceof Node node) {
            node.fireEvent(new TransitionEvent(eventType, property, elapsedTime));
        }
    }

}
