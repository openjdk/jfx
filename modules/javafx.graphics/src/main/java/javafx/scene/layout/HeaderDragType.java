/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
import javafx.stage.StageStyle;

/**
 * Specifies how a {@link Node} participates in {@link HeaderBar} draggable-area hit testing.
 * <p>
 * In stages with the {@link StageStyle#EXTENDED} style, the window can be moved (and on some platforms,
 * resized near the top edge) by interacting with a draggable area. {@code HeaderBar} provides such an
 * area in the scene graph, and uses {@code HeaderDragType} flags on nodes to decide which parts of the
 * scene graph count as draggable, block an underlying draggable area, or are ignored during draggable
 * area hit testing.
 *
 * <h2>Recommended usage</h2>
 * <ul>
 *   <li>Mark non-interactive "blank" areas as {@link #DRAGGABLE} / {@link #DRAGGABLE_SUBTREE}.
 *   <li>Do <em>not</em> mark interactive controls (buttons, menu bars, text inputs) as draggable; use
 *       {@link #NONE} on controls if you need them to explicitly opt out or stop inheritance.
 *   <li>Mark overlays that may cover the header area as {@link #TRANSPARENT} / {@link #TRANSPARENT_SUBTREE}
 *       so they do not obstruct existing draggable areas.
 * </ul>
 *
 * @since 25
 * @deprecated This is a preview feature which may be changed or removed in a future release.
 * @see HeaderBar#setDragType(Node, HeaderDragType)
 */
@Deprecated(since = "25")
public enum HeaderDragType {

    /**
     * The node and its descendants are not a draggable part of the {@code HeaderBar}, and not transparent
     * in regard to draggable-area hit testing. Explicitly opting out of draggability can be useful for
     * controls that are flush with the top edge (for example a {@code MenuBar}), where some platforms
     * may otherwise prioritize a window resize border over control interaction.
     * <p>
     * If the node inherits {@link #DRAGGABLE_SUBTREE} or {@link #TRANSPARENT_SUBTREE} from its parent,
     * the inheritance stops and descendants of the node will not inherit either drag type.
     */
    NONE,

    /**
     * The node is a draggable part of the {@code HeaderBar}.
     * <p>
     * This drag type is only relevant for nodes that are descendants of the header bar. When set on a node,
     * the node participates in draggable-area hit testing. If the node extends beyond the header bar, the
     * effective draggable area is extended accordingly.
     * <p>
     * An interactive node (for example, a {@code Control}) should not be draggable. This can cause problems
     * as both the header bar and the interactive node may react to mouse events in incompatible ways.
     * <p>
     * This drag type does not apply to descendants of the node on which it is set. However, it does not stop
     * an inherited {@link #DRAGGABLE_SUBTREE} drag type from being inherited by descendants of the node.
     */
    DRAGGABLE,

    /**
     * The node and its descendants are a draggable part of the {@code HeaderBar}.
     * <p>
     * This drag type is only relevant for nodes that are descendants of the header bar. When set on a node,
     * the node and its descendants participate in draggable-area hit testing. If the node or its descendants
     * extend beyond the header bar, the effective draggable area is extended accordingly.
     * <p>
     * An interactive node (for example, a {@code Control}) should not be draggable. This can cause problems
     * as both the header bar and the interactive node may react to mouse events in incompatible ways.
     * <p>
     * This drag type is inherited by descendants of the node until a descendant specifies {@link #NONE}.
     */
    DRAGGABLE_SUBTREE,

    /**
     * The node is transparent in regard to draggable-area hit testing.
     * <p>
     * In contrast to {@link #DRAGGABLE}, which positively identifies a node as a draggable part of the
     * {@code HeaderBar}, this option excludes a node from draggable-area hit testing: the header bar
     * behaves as if the node was not present and continues hit testing unimpeded.
     * <p>
     * This drag type can be used not only on descendants of the header bar, but also on other nodes that
     * may overlap it (for example, a sibling shown on top of the header bar). In that case, the overlapping
     * node behaves as if it were draggable, but only where it overlaps a draggable area of the header bar;
     * it does not create any additional draggable area.
     * <p>
     * An interactive node (for example, a {@code Control}) should not be transparent in regard to draggable
     * area hit testing. This can cause problems as both the header bar and the interactive node may react to
     * mouse events in incompatible ways.
     * <p>
     * This drag type does not apply to descendants of the node on which it is set. However, it does
     * not stop an inherited {@link #TRANSPARENT_SUBTREE} drag type from being inherited by descendants
     * of the node.
     *
     * @since 26
     */
    TRANSPARENT,

    /**
     * The node and its descendants are transparent in regard to draggable-area hit testing.
     * <p>
     * In contrast to {@link #DRAGGABLE_SUBTREE}, which positively identifies a node and its descendants
     * as a draggable part of the {@code HeaderBar}, this option excludes a node and its descendants from
     * draggable-area hit testing: the header bar behaves as if the node and its descendants were not
     * present and continues hit testing unimpeded.
     * <p>
     * This drag type can be used not only on descendants of the header bar, but also on other nodes that
     * may overlap it (for example, a sibling shown on top of the header bar). In that case, the overlapping
     * node and its descendants behave as if they were draggable, but only where they overlap a draggable
     * area of the header bar; they do not create any additional draggable area.
     * <p>
     * An interactive node (for example, a {@code Control}) should not be transparent in regard to draggable
     * area hit testing. This can cause problems as both the header bar and the interactive node may react to
     * mouse events in incompatible ways.
     * <p>
     * This drag type is inherited by descendants of the node until a descendant specifies {@link #NONE}.
     *
     * @since 26
     */
    TRANSPARENT_SUBTREE
}
