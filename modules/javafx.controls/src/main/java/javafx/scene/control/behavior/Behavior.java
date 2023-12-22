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

package javafx.scene.control.behavior;

import javafx.scene.control.BehaviorConfiguration;
import javafx.scene.control.Control;

/**
 * A behavior interprets events which target a control or its children and
 * translates these into control state changes. The behavior needs to hook
 * into many aspects of a control, and as such provides the collection of
 * handlers, listeners and key bindings packaged as a {@link BehaviorConfiguration}.
 * The configuration is immutable, and can be applied to as many controls as
 * desired.
 *
 * <p>Behaviors must follow strict rules to ensure they are easy to reuse, and
 * won't interfere with handlers and listeners set on a control by the user:
 *
 * <ul>
 * <li>Never directly install an event handler or listener on a {@link Control}. All
 * handlers must be specified in a {@link BehaviorConfiguration}. This declarative
 * approach allows behaviors to be seamlessly installed and removed.</li>
 * <li>Never rely on a specific {@link javafx.scene.control.Skin} being present on the
 * control. If skin specific behavior is provided, such behavior should gracefully
 * be disabled if an incompatible skin is present.</li>
 * <li>Only consume events that are necessary for the behavior's function. Generally,
 * events that would not result in a state change should be left to bubble up, unless
 * there is a specific requirement to block such events.</li>
 *
 * <li>TODO a word about Timeline clean-up</li>
 * </ul>
 *
 * @see Control#setBehavior(Behavior)
 * @param <N> the control type suited for this behavior
 */
public interface Behavior<N extends Control> {

    /**
     * Gets the behavior configuration associated with this behavior.
     *
     * @return a behavior configuration, never {@code null}
     */
    BehaviorConfiguration<N> getConfiguration();
}
