/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sun.javafx.scene.traversal.TraversalEngine;

public class RegistrationTest {
    /**
     * Asserts that this node is registered with this traversal engine.
     */
    void assertIsRegistered(Node n, TraversalEngine te) {
        assertTrue(te.registeredNodes.contains(n));
    }

    /**
     * Asserts that this node is not registered with this traversal engine.
     */
    void assertNotRegistered(Node n, TraversalEngine te) {
        assertTrue(te.registeredNodes.contains(n) == false);
    }

    /**
     * Helper function to return a node for testing.
     * The boolean parameter determines whether it's traversable.
     */
    Node node(boolean traversable) {
        Group g = new Group();
        g.setFocusTraversable(traversable);
        return g;
    }

    @Test
    public void testAddRemoveTraversable() {
        Node n = node(true);
        Group root = new Group();
        root.getChildren().addAll(n);
        Scene s = new Scene(root);

        System.out.println("n.scene="+n.getScene()+" s.traversalRegistry="+s.traversalRegistry);

        assertTrue(s.traversalRegistry.containsKey(n));
        TraversalEngine te = (TraversalEngine)s.traversalRegistry.get(n);
        assertNotNull(te);
        assertIsRegistered(n, te);

        root.getChildren().remove(n);
        assertFalse(s.traversalRegistry.containsKey(n));
        assertNotRegistered(n, te);
    }

    @Test
    public void testToggleTraversable() {
        Node n = node(false);
        Group root = new Group();
        root.getChildren().addAll(n);
        Scene s = new Scene(root);

        assertTrue(s.traversalRegistry == null || !s.traversalRegistry.containsKey(n));

        n.setFocusTraversable(true);
        // note: reg is most likely null until we make a node traversable!
        assertNotNull(s.traversalRegistry);
        assertTrue(s.traversalRegistry.containsKey(n));
        TraversalEngine te = (TraversalEngine)s.traversalRegistry.get(n);
        assertNotNull(te);
        assertIsRegistered(n, te);

        n.setFocusTraversable(false);
        assertFalse(s.traversalRegistry.containsKey(n));
        assertNotRegistered(n, te);
    }
}
