/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

/**
 * Tests for depth test features.
 *
 */
public class DepthTestTest {

    /**
     * Tests the default value for a single node with no parent
     */
    @Test public void testDepthTestSingleDefault() {
        Node node = new Rectangle();
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
    }

    /**
     * Tests the default values for a group node with a child node
     */
    @Test public void testDepthTestParentChildDefaults() {
        Group group = new Group();
        Node node = new Rectangle();
        group.getChildren().add(node);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);
    }

    /**
     * Tests setting the value for a single node with no parent
     */
    @Test public void testDepthTestSingleSet() {
        Node node = new Rectangle();
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());

        node.setDepthTest(DepthTest.DISABLE);
        assertEquals(node.getDepthTest(), DepthTest.DISABLE);
        assertFalse(node.isDerivedDepthTest());

        node.setDepthTest(DepthTest.ENABLE);
        assertEquals(node.getDepthTest(), DepthTest.ENABLE);
        assertTrue(node.isDerivedDepthTest());

        node.setDepthTest(DepthTest.INHERIT);
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
    }

    /**
     * Tests setting values for a group node with a child node
     */
    @Test public void testDepthTestParentChildSet() {
        Group group = new Group();
        Node node = new Rectangle();
        group.getChildren().add(node);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.DISABLE);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.ENABLE);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.ENABLE);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.INHERIT);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.DISABLE);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.ENABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.ENABLE);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.INHERIT);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.INHERIT);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.ENABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.ENABLE);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        node.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.DISABLE);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.INHERIT);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.DISABLE);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.ENABLE);
        assertEquals(group.getDepthTest(), DepthTest.ENABLE);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.DISABLE);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);
    }

    /**
     * Tests setting values for a group node with a child node, and ensures
     * that the state is correct when adding and removing the child node.
     */
    @Test public void testDepthTestParentChildRemove() {
        Group group = new Group();
        Node node = new Rectangle();
        group.getChildren().add(node);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        group.getChildren().remove(node);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);
        validate(node, true);

        group.getChildren().add(node);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.INHERIT);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);

        group.getChildren().remove(node);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);
        validate(node, true);

        group.setDepthTest(DepthTest.DISABLE);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);
        validate(node, true);

        group.getChildren().add(node);
        assertEquals(group.getDepthTest(), DepthTest.DISABLE);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(group, true);

        group.setDepthTest(DepthTest.INHERIT);
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(group, true);
    }

    /**
     * Tests two levels of children and ensures that the state is correct when
     * adding and removing the second group.
     */
    @Test public void testDepthTestMutliParentChildRemove() {
        Group root = new Group();
        Group group = new Group();
        root.getChildren().add(group);
        Node node = new Rectangle();
        group.getChildren().add(node);
        validate(root, true);

        root.setDepthTest(DepthTest.DISABLE);
        assertEquals(root.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(root, true);

        root.getChildren().remove(group);
        assertEquals(root.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(root, true);
        validate(group, true);

        root.getChildren().add(group);
        assertEquals(root.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(root, true);

        root.setDepthTest(DepthTest.INHERIT);
        assertEquals(root.getDepthTest(), DepthTest.INHERIT);
        assertTrue(root.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(root, true);
    }

    /**
     * Tests two levels of children and ensures that the state is correct when
     * reparenting a group of nodes.
     */
    @Test public void testDepthTestReparent() {
        Group root1 = new Group();
        Group root2 = new Group();

        Group group = new Group();
        root1.getChildren().add(group);
        Node node = new Rectangle();
        group.getChildren().add(node);
        validate(root1, true);
        validate(root2, true);

        root1.setDepthTest(DepthTest.DISABLE);
        assertEquals(root1.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root1.isDerivedDepthTest());
        assertEquals(root2.getDepthTest(), DepthTest.INHERIT);
        assertTrue(root2.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(root1, true);
        validate(root2, true);

        root2.getChildren().add(group);
        assertEquals(root1.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root1.isDerivedDepthTest());
        assertEquals(root2.getDepthTest(), DepthTest.INHERIT);
        assertTrue(root2.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertTrue(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertTrue(node.isDerivedDepthTest());
        validate(root1, true);
        validate(root2, true);

        root2.setDepthTest(DepthTest.DISABLE);
        assertEquals(root1.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root1.isDerivedDepthTest());
        assertEquals(root2.getDepthTest(), DepthTest.DISABLE);
        assertFalse(root2.isDerivedDepthTest());
        assertEquals(group.getDepthTest(), DepthTest.INHERIT);
        assertFalse(group.isDerivedDepthTest());
        assertEquals(node.getDepthTest(), DepthTest.INHERIT);
        assertFalse(node.isDerivedDepthTest());
        validate(root1, true);
        validate(root2, true);
    }

    private void validate(Node n, boolean parentDerivedDepthTest) {
        boolean nodeDerivedDepthTest = n.getDepthTest() == DepthTest.INHERIT
                ? parentDerivedDepthTest
                : n.getDepthTest() == DepthTest.ENABLE;
        assertEquals(nodeDerivedDepthTest, n.isDerivedDepthTest());
        if (n instanceof Group) {
            Group g = (Group) n;
            for (Node child : g.getChildren()) {
                validate(child, nodeDerivedDepthTest);
            }
        }
    }

}
