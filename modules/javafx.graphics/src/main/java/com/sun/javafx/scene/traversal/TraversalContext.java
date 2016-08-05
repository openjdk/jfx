/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;

public interface TraversalContext {

    /**
     * Returns all possible targets within the context
     */
    List<Node> getAllTargetNodes();

    /**
     * Returns layout bounds of the Node in the relevant (Sub)Scene. Note that these bounds are the most important for traversal
     * as they define the final position within the scene.
     */
    Bounds getSceneLayoutBounds(Node node);

    /**
     * The root for this context, Traversal should be done only within the root
     */
    Parent getRoot();

    /**
     * If the TraversalEngine does not want to handle traversal inside some inner child (Parent), it can use this method to apply
     * default algorithm inside that Parent and return the first Node
     */
    Node selectFirstInParent(Parent parent);

    /**
     * If the TraversalEngine does not want to handle traversal inside some inner child (Parent), it can use this method to apply
     * default algorithm inside that Parent and return the last Node
     */
    Node selectLastInParent(Parent parent);

    /**
     * If the TraversalEngine does not want to handle traversal inside some inner child (Parent), it can use this method to apply
     * default algorithm inside that Parent and return the next Node within the Parent or null if there's no successor.
     * @param  subTreeRoot this will be used as a root of the traversal. Should be a Node that is still handled by the current TraversalEngine,
     *                     but it's content is not.
     */
    Node selectInSubtree(Parent subTreeRoot, Node from, Direction dir);
}
