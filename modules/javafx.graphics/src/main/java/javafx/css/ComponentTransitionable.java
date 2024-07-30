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

package javafx.css;

import javafx.animation.Interpolatable;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;

/**
 * Identifies a class that supports component-wise CSS transitions.
 * <p>
 * Component-wise transitions offer more flexibility than {@link Interpolatable} transitions.
 * While an {@code Interpolatable} object can only transition homogeneously from one value to another (i.e. using
 * the same easing function, delay, and duration for all of its constituent values), a {@code ComponentTransitionable}
 * object can use different easing functions, delays and durations for each of its constituent values.
 * <p>
 * All transitionable components must be exposed to the CSS subsystem using {@link CssMetaData} descriptors.
 * Classes that support component-wise transitions must also implement the {@link StyleConverter#convertBack} method
 * of their respective {@link StyleConverter}.
 * <p>
 * When the CSS subsystem encounters a {@code ComponentTransitionable} value for which transitions are defined, it
 * first deconstructs the start value and the end value using the respective {@code StyleConverter}, and then applies
 * individual transition animations for its components. For each frame, the instantaneous component values are
 * reconstructed with the {@code StyleConverter} to form the combined value.
 *
 * @see Border
 * @see Background
 * @since 24
 */
public interface ComponentTransitionable {
}
