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
 * {@code TransitionDefinition} defines how a property changes smoothly from one value to another.
 *
 * @since 20
 */
public class TransitionDefinition {

    private final TransitionPropertySelector selector;
    private final String property;
    private final Duration duration;
    private final Duration delay;
    private final Interpolator interpolator;

    /**
     * Creates a new {@code TransitionDefinition} instance with zero delay and linear interpolation.
     *
     * @param selector property selector
     * @param property name of the property (may be {@code null} when {@code selector} is {@code PropertySelector.ALL})
     * @param duration duration of the transition (must be larger than 0)
     * @throws NullPointerException if any of the arguments is {@code null}
     * @throws IllegalArgumentException if the duration is negative
     */
    public TransitionDefinition(TransitionPropertySelector selector, String property, Duration duration) {
        this(selector, property, duration, Duration.ZERO, Interpolator.LINEAR);
    }

    /**
     * Creates a new {@code TransitionDefinition} instance.
     *
     * @param selector property selector
     * @param property name of the property (may be {@code null} when {@code selector} is {@code PropertySelector.ALL})
     * @param duration duration of the transition (must be larger than 0)
     * @param delay delay after which the transition is started; if negative, the transition starts
     *              immediately, but will appear to have begun at an earlier point in time
     * @param interpolator interpolator for the transition
     * @throws NullPointerException if any of the arguments is {@code null}
     * @throws IllegalArgumentException if the duration is negative
     */
    public TransitionDefinition(TransitionPropertySelector selector, String property, Duration duration,
                                Duration delay, Interpolator interpolator) {
        this.selector = Objects.requireNonNull(selector, "selector cannot be null");
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.delay = Objects.requireNonNull(delay, "delay cannot be null");
        this.interpolator = Objects.requireNonNull(interpolator, "interpolator cannot be null");

        if (selector != TransitionPropertySelector.ALL) {
            this.property = Objects.requireNonNull(property, "property cannot be null");
        } else {
            this.property = null;
        }

        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            throw new IllegalArgumentException("duration cannot be zero or negative");
        }
    }

    /**
     * Gets the property selector of the transition.
     */
    public TransitionPropertySelector getSelector() {
        return selector;
    }

    /**
     * Gets the name of the property for which the transition is specified.
     */
    public String getProperty() {
        return property;
    }

    /**
     * Gets the duration of the transition.
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Gets the delay after which the transition starts.
     */
    public Duration getDelay() {
        return delay;
    }

    /**
     * Gets the interpolator for the transition.
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }

}
