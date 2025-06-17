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

/**
 * Specifies whether a node is a draggable part of a {@link HeaderBar}.
 *
 * @since 25
 * @deprecated This is a preview feature which may be changed or removed in a future release.
 * @see HeaderBar#setDragType(Node, HeaderDragType)
 */
@Deprecated(since = "25")
public enum HeaderDragType {

    /**
     * The node is not a draggable part of the {@code HeaderBar}.
     * <p>
     * If the node inherits {@link #DRAGGABLE_SUBTREE} from its parent, the inheritance stops and
     * descendants of the node will not inherit {@code DRAGGABLE_SUBTREE}.
     */
    NONE,

    /**
     * The node is a draggable part of the {@code HeaderBar}.
     * <p>
     * This drag type does not apply to descendants of the node. However, it does not stop an inherited
     * {@link #DRAGGABLE_SUBTREE} drag type from being inherited by descendants of the node.
     */
    DRAGGABLE,

    /**
     * The node and its descendants are a draggable part of the {@code HeaderBar}.
     * <p>
     * This drag type is inherited by descendants of the node until a descendant specifies {@link #NONE}.
     */
    DRAGGABLE_SUBTREE
}
