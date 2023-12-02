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

import javafx.scene.control.Control;

/**
 * Defines the behavior for a {@link Control}. Behaviors listen to events that
 * bubble up to the control level, interprete them and perform state changes on
 * the control. Behaviors can handle events that have the control as target,
 * as well as events that have its children as target.
 *
 * @param <C> the control type suited for this behavior
 */
public interface Behavior<C extends Control> {

    /**
     * Configures the given installer with the defined behavior for a specific
     * control. Many controls can be configured with the same behavior without
     * having to create a new behavior instance. The behavior should return a
     * {@link StateFactory} which takes the {@link Control} as argument. This
     * per control state is then provided to any callbacks the behavior configured.
     *
     * @param installer a control provided installer, never {@code null}
     * @return a {@code StateFactory}, never {@code null}
     */
    StateFactory<? super C> configure(BehaviorInstaller<? extends C> installer);

}
