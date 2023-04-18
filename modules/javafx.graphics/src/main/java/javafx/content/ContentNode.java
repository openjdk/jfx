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

package javafx.content;

import javafx.css.Selector;
import javafx.css.Styleable;
import javafx.scene.Node;
import java.util.Set;

/**
 * The {@code ContentNode} class represents a node in the content graph, which is the structure that defines
 * the content model of an application. Although the content graph is primarily composed of {@code ContentNode}
 * instances, its leaf nodes can also be objects of any kind.
 * <p>
 * The content model contains all significant parts of the user interface, for example:
 * <ul>
 *     <li>The children of a layout container like {@link javafx.scene.layout.StackPane}.
 *     <li>The text or graphic of a {@link javafx.scene.control.Button}
 *     <li>The items of a {@link javafx.scene.control.ListView}
 *     <li>The menu items of a {@link javafx.scene.control.Menu}
 *     <li>The title, graphic, and content of a {@link javafx.scene.control.Tab}
 * </ul>
 * The content graph does not include nodes created by skins or cell factories, which makes it independent
 * of an application's visualization. For this reason, the scene graph will usually contain many more nodes
 * than the content graph.
 * <p>
 * Since the content graph is not limited to {@link Node} instances, it can also span across nodes that are
 * not part of the scene graph.
 * For example, {@link javafx.scene.control.Tab} or {@link javafx.scene.control.MenuItem} are not scene
 * graph nodes, but they are included in the content graph.
 * <p>
 * While an application will only have a single scene graph, it can have any number of disconnected content
 * graphs. The main content graph corresponds to the root node of the user interface. Additional content
 * graphs can appear in several scenarios, for example when a skin creates scene graph nodes. A skin's
 * content graph is not reachable from the main content graph.
 *
 * @since 21
 */
public sealed interface ContentNode permits Node, ContentParent {

    /**
     * Returns the parent of this {@link ContentNode}.
     *
     * @return the {@code ContentParent}, or {@code null} if this node has no parent
     */
    ContentParent getContentParent();

    /**
     * Finds this {@code ContentNode}, or the first sub-node, based on the given CSS selector.
     * If this node is a {@code ContentParent}, then this function will traverse down into the
     * branch until it finds a match. If more than one sub-node matches the specified selector,
     * this function returns the first of them.
     * <p>
     * For example, if a node is given the id of "myId", then the lookup method can be used to
     * find this node as follows: <code>scene.lookup("#myId");</code>.
     *
     * @param selector The CSS selector of the node to find
     * @return The first node, starting from this {@code ContentNode}, which matches
     *         the CSS {@code selector}, {@code null} if none is found.
     */
    default Styleable lookupContent(String selector) {
        if (selector == null || !(this instanceof Styleable styleable)) {
            return null;
        }

        Selector s = Selector.createSelector(selector);
        return s != null && s.applies(styleable) ? styleable : null;
    }

    /**
     * Finds all content nodes, including this one and any children, which match the given
     * CSS selector. If no matches are found, an empty unmodifiable set is returned.
     * The set is explicitly unordered.
     *
     * @param selector The CSS selector of the nodes to find
     * @return All nodes, starting from and including this {@code ContentNode}, which match
     *         the CSS {@code selector}. The returned set is always unordered and
     *         unmodifiable, and never {@code null}.
     */
    default Set<Styleable> lookupAllContent(String selector) {
        Styleable styleable = lookupContent(selector);
        return styleable != null ? Set.of(styleable) : Set.of();
    }

}
