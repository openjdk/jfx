/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package jfx.incubator.scene.control.input;

import javafx.event.Event;
import javafx.scene.control.Skinnable;

/**
 * A functional interface which denotes code associated with a {@code FunctionTag} or a key binding.
 *
 * @param <C> the type of the skinnable
 * @since 999 TODO
 */
@FunctionalInterface
public interface FunctionHandler<C extends Skinnable> {
    /**
     * Handles the event associated with a function tag or a key binding.
     * @param control the control instance
     */
    public void handle(C control);

    /**
     * This method is called by the InputMap when handling the corresponding KeyEvent.
     * Implementors may override this method to conditionally consume the event.
     * @param ev the event
     * @param control the control instance
     */
    public default void handleKeyBinding(Event ev, C control) {
        handle(control);
        ev.consume();
    }
}
