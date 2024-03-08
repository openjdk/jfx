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
package javafx.scene.control.input;

import javafx.scene.control.Skinnable;

/**
 * A functional interface which denotes code associated with a {@code FunctionTag} or a key binding.
 * Unlike {@link FunctionHandler}, this handler allows for controlling whether the matching KeyEvent
 * will be consumed or not.
 *
 * @param <C> the type of the skinnable
 * @since 999 TODO
 */
@FunctionalInterface
public interface FunctionHandlerConditional<C extends Skinnable> {
    /**
     * Handles the event associated with a function tag or a key binding.
     * @param control the control instance
     * @return true to consume the key event, false otherwise
     */
    public boolean handle(C control);
}
