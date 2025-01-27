/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import javafx.scene.Node;
import javafx.stage.Stage;

/**
 * Identifies the semantic type of a button in a custom {@link HeaderBar}, which enables integrations
 * with the platform window manager. For example, hovering over a {@link #MAXIMIZE} button on Windows
 * will summon snap layouts.
 * <p>
 * This property can be set on any {@link Node}. Applications are still required to provide their own
 * click handlers to programmatically trigger the actions associated with header buttons, i.e. call
 * appropriate stage methods like {@link Stage#setIconified(boolean)}, {@link Stage#setMaximized(boolean)}
 * or {@link Stage#close()}.
 *
 * @since 25
 * @see HeaderBarBase#setHeaderButtonType(Node, HeaderButtonType)
 */
public enum HeaderButtonType {
    /**
     * Identifies the minimize button.
     *
     * @see Stage#isIconified()
     * @see Stage#setIconified(boolean)
     */
    MINIMIZE,

    /**
     * Identifies the maximize button.
     *
     * @see Stage#isMaximized()
     * @see Stage#setMaximized(boolean)
     */
    MAXIMIZE,

    /**
     * Identifies the close button.
     *
     * @see Stage#close()
     */
    CLOSE
}
