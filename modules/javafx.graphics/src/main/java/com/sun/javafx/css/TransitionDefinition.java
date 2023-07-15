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

import javafx.animation.Interpolator;
import javafx.css.StyleableProperty;
import javafx.util.Duration;
import java.util.Objects;

/**
 * {@code TransitionDefinition} describes how a {@link StyleableProperty} changes from one value to
 * another when its value is changed implicitly by the CSS subsystem. The transition can be smooth,
 * for example using linear or Bézier interpolation, or discrete using stepwise interpolation.
 * <p>
 * In this example, the button's opacity property is configured to change smoothly using linear
 * interpolation over a duration of 0.5 seconds:
 *
 * <pre>{@code
 * var button = new Button();
 *
 * button.getTransitions().add(
 *     new TransitionDefinition(
 *         "-fx-opacity",
 *         Duration.seconds(0.5),
 *         Duration.ZERO,
 *         Interpolator.LINEAR));
 * }</pre>
 */
public class TransitionDefinition {

    private final String propertyName;
    private final Duration duration;
    private final Duration delay;
    private final Interpolator interpolator;

    /**
     * Creates a new {@code TransitionDefinition} instance with zero delay and linear interpolation.
     *
     * @param propertyName the CSS property name, or "all" to target any property
     * @param duration duration of the transition
     *
     * @throws NullPointerException if any of the arguments is {@code null}
     * @throws IllegalArgumentException if the duration is negative
     */
    public TransitionDefinition(String propertyName, Duration duration) {
        this(propertyName, duration, Duration.ZERO, Interpolator.LINEAR);
    }

    /**
     * Creates a new {@code TransitionDefinition} instance.
     *
     * @param propertyName the CSS property name, or "all" to target any property
     * @param duration duration of the transition
     * @param delay delay after which the transition is started; if negative, the transition starts
     *              immediately, but will appear to have begun at an earlier point in time
     * @param interpolator interpolator for the transition
     *
     * @throws NullPointerException if any of the arguments is {@code null}
     * @throws IllegalArgumentException if the duration is negative
     */
    public TransitionDefinition(String propertyName, Duration duration,
                                Duration delay, Interpolator interpolator) {
        Objects.requireNonNull(propertyName, "propertyName cannot be null");
        this.propertyName = "all".equalsIgnoreCase(propertyName) ? "all" : propertyName;
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.delay = Objects.requireNonNull(delay, "delay cannot be null");
        this.interpolator = Objects.requireNonNull(interpolator, "interpolator cannot be null");

        if (duration.lessThan(Duration.ZERO)) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
    }

    /**
     * Gets the name of the CSS property targeted by this transition.
     *
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gets the duration of the transition.
     *
     * @return the duration of the transition
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Gets the delay after which the transition starts.
     *
     * @return the delay after which the transition starts
     */
    public Duration getDelay() {
        return delay;
    }

    /**
     * Gets the interpolator for the transition.
     *
     * @return the {@code Interpolator}
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }

}
