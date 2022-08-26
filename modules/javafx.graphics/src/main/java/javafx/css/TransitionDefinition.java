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

package javafx.css;

import javafx.animation.Interpolator;
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
 *         TransitionPropertyKind.BEAN,
 *         "opacity",
 *         Duration.seconds(0.5),
 *         Duration.ZERO,
 *         Interpolator.LINEAR));
 * }</pre>
 *
 * Note that {@link TransitionPropertyKind#BEAN} is used to match the specified property name
 * against JavaFX Bean property names instead of JavaFX CSS property names.
 *
 * @since 20
 */
public class TransitionDefinition {

    private final TransitionPropertyKind propertyKind;
    private final String propertyName;
    private final Duration duration;
    private final Duration delay;
    private final Interpolator interpolator;

    /**
     * Creates a new {@code TransitionDefinition} instance with zero delay and linear interpolation.
     *
     * @param propertyKind property selector
     * @param propertyName name of the property (may be {@code null} when {@code propertyKind}
     *                     is {@link TransitionPropertyKind#ALL})
     * @param duration duration of the transition
     *
     * @throws NullPointerException if any of the arguments is {@code null}
     * @throws IllegalArgumentException if the duration is negative
     */
    public TransitionDefinition(TransitionPropertyKind propertyKind, String propertyName, Duration duration) {
        this(propertyKind, propertyName, duration, Duration.ZERO, Interpolator.LINEAR);
    }

    /**
     * Creates a new {@code TransitionDefinition} instance.
     *
     * @param propertyKind the property kind
     * @param propertyName name of the property (may be {@code null} when {@code propertyKind}
     *                     is {@link TransitionPropertyKind#ALL})
     * @param duration duration of the transition
     * @param delay delay after which the transition is started; if negative, the transition starts
     *              immediately, but will appear to have begun at an earlier point in time
     * @param interpolator interpolator for the transition
     *
     * @throws NullPointerException if any of the arguments is {@code null}
     * @throws IllegalArgumentException if the duration is negative
     */
    public TransitionDefinition(TransitionPropertyKind propertyKind, String propertyName, Duration duration,
                                Duration delay, Interpolator interpolator) {
        this.propertyKind = Objects.requireNonNull(propertyKind, "propertyKind cannot be null");
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.delay = Objects.requireNonNull(delay, "delay cannot be null");
        this.interpolator = Objects.requireNonNull(interpolator, "interpolator cannot be null");

        if (propertyKind != TransitionPropertyKind.ALL) {
            this.propertyName = Objects.requireNonNull(propertyName, "propertyName cannot be null");
        } else {
            this.propertyName = "all";
        }

        if (duration.lessThan(Duration.ZERO)) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
    }

    /**
     * Gets the property kind targeted by this transition.
     *
     * @return the {@code TransitionPropertyKind}
     */
    public TransitionPropertyKind getPropertyKind() {
        return propertyKind;
    }

    /**
     * Gets the name of the property targeted by this transition.
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
