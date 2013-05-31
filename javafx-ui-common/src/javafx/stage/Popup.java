/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * A Popup is a special window-like container for a scene graph. It is typically
 * used for tooltip like notification, drop down boxes, menus, and so forth.
 * The popup has no decorations, and essentially acts as a specialized
 * scene/window which has no decorations, is transparent, and with a null fill.
 *
 * @since JavaFX 2.0
 */
public class Popup extends PopupWindow {

    /**
     * The ObservableList of {@code Node}s to be rendered on this
     * {@code Popup}. The content forms the complete visual representation of
     * the Popup. Popups have no intrinsic visuals.
     */
    @Override public final ObservableList<Node> getContent() {
        return super.getContent();
    }
}
