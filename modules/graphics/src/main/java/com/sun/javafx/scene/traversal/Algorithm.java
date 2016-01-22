/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.traversal;

import javafx.scene.Node;

/**
 * An algorithm to be used in a traversal engine.
 *
 * Note that in order to avoid cycles or dead-ends in traversal the algorithms should respect the following order:
 * * for NEXT: node -> node's subtree -> node siblings (first sibling then it's subtree) -> NEXT_IN_LINE for node's parent
 * * for NEXT_IN_LINE: node -> node siblings (first sibling then it's subtree) -> NEXT_IN_LINE for node's parent
 * * for PREVIOUS: node -> node siblings ( ! first subtree then the node itself ! ) -> PREVIOUS for node's parent
 *
 * Basically it ensures that next direction will traverse the same nodes as previous, in the opposite order.
 *
 */
public interface Algorithm {

    /**
     * Traverse from owner, in direction dir.
     * Return a the new target Node or null if no suitable target is found.
     *
     * Typically, the implementation of override algorithm handles only parent's direct children and looks like this:
     * 1) Find the nearest parent of the "owner" that is handled by this algorithm (i.e. it's a direct child of the root).
     * 2) select the next node within this direct child using the context.selectInSubtree() and return it
     * 2a) if no such node exists, move to the next direct child in the direction (this is where the different order of direct children is defined)
     *     or if direct children are not traversable, the select the first node in the next direct child
     */
    public Node select(Node owner, Direction dir, TraversalContext context);

    /**
     * Return the first traversable node for the specified context (root).
     * @param context the context that contains the root
     * @return the first node
     */
    public Node selectFirst(TraversalContext context);

    /**
     * Return the last traversable node for the specified context (root).
     * @param context the context that contains the root
     * @return the last node
     */
    public Node selectLast(TraversalContext context);

}
