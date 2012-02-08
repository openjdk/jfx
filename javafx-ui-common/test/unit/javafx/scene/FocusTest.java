/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FocusTest {

    private Stage stage;
    private Scene scene;
    private List<Node> nodes;
    private int nodeIndex;

    @Before
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);
        stage.show();
        nodes = new ArrayList();
        nodeIndex = 0;
    }

    @After
    public void tearDown() {
        stage.hide();
        stage = null;
        scene = null;
    }

    void fireTestPulse() {
        // TODO: an actual pulse doesn't work in the stub environment.
        // Just clean up focus instead.
        scene.focusCleanup();
    }
    
    boolean T = true;
    boolean F = false;

    Node n(boolean trav, boolean vis, boolean enable) {
        Rectangle node = new Rectangle();
        node.setId("Rect-" + nodeIndex);
        node.setFocusTraversable(trav);
        node.setVisible(vis);
        node.setDisable(!enable);
        nodes.add(node);
        nodeIndex++;

        return node;
    }

    Node n() {
        return n(T, T, T);
    }

    private void assertIsFocused(Scene s, Node n) {
        assertEquals(n, s.impl_getFocusOwner());
        assertTrue(n.isFocused());
    }

    private void assertNotFocused(Scene s, Node n) {
        assertTrue(n != s.impl_getFocusOwner());
        assertFalse(n.isFocused());
    }

    private void assertNullFocus(Scene s) {
        assertNull(s.impl_getFocusOwner());
    }

    /**
     * Test setting of initial focus.
     */
    @Test
    public void testInitial() {
        assertNullFocus(scene);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * Test requestFocus() on eligible and ineligible nodes.
     */
    @Test
    public void testRequest() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(
                n(T, T, T), // 0 - focusable
                n(F, T, T), // 1 - focusable
                n(T, T, F), // 2 - not focusable
                n(F, T, F), // 3 - not focusable
                n(T, F, T), // 4 - not focusable
                n(F, F, T), // 5 - not focusable
                n(T, F, F), // 6 - not focusable
                n(F, F, F) // 7 - not focusable
                );
        n(); // 8 - not focusable, because not in scene

        nodes.get(0).requestFocus();
        assertIsFocused(scene, nodes.get(0));
        nodes.get(1).requestFocus();
        assertIsFocused(scene, nodes.get(1));
        nodes.get(2).requestFocus();
        assertNotFocused(scene, nodes.get(2));
        nodes.get(3).requestFocus();
        assertNotFocused(scene, nodes.get(3));
        nodes.get(4).requestFocus();
        assertNotFocused(scene, nodes.get(4));
        nodes.get(5).requestFocus();
        assertNotFocused(scene, nodes.get(5));
        nodes.get(6).requestFocus();
        assertNotFocused(scene, nodes.get(6));
        nodes.get(7).requestFocus();
        assertNotFocused(scene, nodes.get(7));
        nodes.get(8).requestFocus();
        assertNotFocused(scene, nodes.get(8));
    }

    /**
     * Test removing the focus owner without another eligible node in the scene.
     */
    @Test
    public void testRemove1() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        nodes.get(0).requestFocus();
        scene.getRoot().getChildren().remove(nodes.get(0));
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertNullFocus(scene);
    }

    /**
     * Test removing the focus owner with another eligible node in the scene.
     */
    @Test
    public void testRemove2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n());
        nodes.get(0).requestFocus();
        scene.getRoot().getChildren().remove(nodes.get(0));
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Test making the focus owner invisible without another eligible
     * node in the scene.
     */
    @Test
    public void testInvisible1() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        nodes.get(0).requestFocus();
        assertIsFocused(scene, nodes.get(0));
        nodes.get(0).setVisible(false);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertNullFocus(scene);
    }

    /**
     * Test making the focus owner invisible with another eligible
     * node in the scene.
     */
    @Test
    public void testInvisible2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n());
        nodes.get(0).requestFocus();
        assertIsFocused(scene, nodes.get(0));
        nodes.get(0).setVisible(false);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Test making the focus owner disabled without another eligible
     * node in the scene.
     */
    @Test
    public void testDisable1() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        nodes.get(0).requestFocus();
        nodes.get(0).setDisable(true);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertNullFocus(scene);
    }

    /**
     * Test making the focus owner disabled with another eligible
     * node in the scene.
     */
    @Test
    public void testDisable2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n());
        nodes.get(0).requestFocus();
        nodes.get(0).setDisable(true);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * When focus null, test adding an eligible, traversable node.
     */
    @Test
    public void testAddEligible() {
        fireTestPulse(); // make sure focus is clean
        assertNullFocus(scene);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus null, test making a node traversable.
     */
    @Test
    public void testBecomeTraversable() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(F, T, T), n(F, T, T));
        fireTestPulse();
        assertNullFocus(scene);
        nodes.get(0).setFocusTraversable(true);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus null, test making a node visible.
     */
    @Test
    public void testBecomeVisible() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n(T, F, T));
        fireTestPulse();
        assertNullFocus(scene);
        nodes.get(0).setVisible(true);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus null, test making a node enabled.
     */
    @Test
    public void testBecomeEnabled() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n(T, T, F));
        fireTestPulse();
        assertNullFocus(scene);
        nodes.get(0).setDisable(false);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus exists, test adding an eligible, traversable node.
     */
    @Test
    public void testAddEligible2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        scene.getRoot().getChildren().add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus exists, test making a node traversable.
     */
    @Test
    public void testBecomeTraversable2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n(F, T, T));
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        nodes.get(1).setFocusTraversable(true);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus exists, test making a node visible.
     */
    @Test
    public void testBecomeVisible2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n(T, F, T));
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        nodes.get(1).setVisible(true);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus exists, test making a node enabled.
     */
    @Test
    public void testBecomeEnabled2() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n(T, T, F));
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        nodes.get(1).setDisable(false);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * Test moving the focus within a scene.
     */
    @Test
    public void testMoveWithinScene() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g2.getChildren().add(nodes.get(0));
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * Test moving the focus into an invisible group, with no other
     * eligible nodes in the scene.
     */
    @Test
    public void testMoveIntoInvisible() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setVisible(false);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g2.getChildren().add(nodes.get(0));
        fireTestPulse();
        assertNullFocus(scene);
    }

    /**
     * Test moving the focus into an invisible group, with another
     * eligible node in the scene.
     */
    @Test
    public void testMoveIntoInvisible2() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setVisible(false);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2, n());
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g2.getChildren().add(nodes.get(0));
        fireTestPulse();
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Test moving the focus into a disabled group, with no other
     * eligible nodes in the scene.
     */
    @Test
    public void testMoveIntoDisabled() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setDisable(true);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g2.getChildren().add(nodes.get(0));
        fireTestPulse();
        assertNullFocus(scene);
    }

    /**
     * Test moving the focus into a disabled group, with another
     * eligible nodes in the scene.
     */
    @Test
    public void testMoveIntoDisabled2() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setDisable(true);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2, n());
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g2.getChildren().add(nodes.get(0));
        fireTestPulse();
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Test making the parent of focused node disabled, with no other
     * eligible nodes in the scene.
     */
    @Test
    public void testMakeParentDisabled() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setVisible(false);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g1.setDisable(true);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertNullFocus(scene);
    }

    /**
     * Test making the parent of focused node disabled, with another
     * eligible nodes in the scene.
     */
    @Test
    public void testMakeParentDisabled2() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setVisible(false);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2, n());
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g1.setDisable(true);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Test making the parent of focused node invisible, with no other
     * eligible nodes in the scene.
     */
    @Test
    public void testMakeParentInvisible() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setVisible(false);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g1.setVisible(false);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertNullFocus(scene);
    }

    /**
     * Test making the parent of focused node invisible, with another
     * eligible nodes in the scene.
     */
    @Test
    public void testMakeParentInvisible2() {
        Group g1 = new Group(n());
        Group g2 = new Group();
        g2.setVisible(false);
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(g1, g2, n());
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        g1.setVisible(false);
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Focus should not move if stacking order changes.
     */
    @Test
    public void testToFront() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().addAll(n(), n());
        nodes.get(0).requestFocus();
        assertIsFocused(scene, nodes.get(0));
        assertNotFocused(scene, nodes.get(1));
        nodes.get(0).toFront();
        assertIsFocused(scene, nodes.get(0));
        assertNotFocused(scene, nodes.get(1));
        nodes.get(0).toBack();
        assertIsFocused(scene, nodes.get(0));
        assertNotFocused(scene, nodes.get(1));
    }

    /**
     * Test moving focused node into scene which is not in active stage.
     */
    @Test
    public void testMoveIntoInactiveScene() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        nodes.get(0).requestFocus();
        assertIsFocused(scene, nodes.get(0));
        Scene scene2 = new Scene(new Group());
        scene2.getRoot().getChildren().add(nodes.get(0));
        fireTestPulse();
        assertNullFocus(scene);
        assertNullFocus(scene2);
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertNullFocus(scene);
        assertEquals(nodes.get(0), scene2.impl_getFocusOwner());
        assertFalse(nodes.get(0).isFocused());
        stage.setScene(scene2);
        fireTestPulse();
        assertNullFocus(scene);
        assertIsFocused(scene2, nodes.get(0));
    }

    /**
     * Test making stage invisible.
     */
    @Test
    public void testInvisibleStage() {
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        nodes.get(0).requestFocus();
        stage.show();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        stage.hide();
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertEquals(nodes.get(0), scene.impl_getFocusOwner());
        assertFalse(nodes.get(0).isFocused());     
    }
    
    /**
     * Test switching two scenes in one stage. Focus owner in scenes should
     * remain the same while focused node should change.
     */
    @Test
    public void testSwitchScenes(){
        scene.setRoot(new Group());
        scene.getRoot().getChildren().add(n());
        nodes.get(0).requestFocus();
        Scene scene2 = new Scene(new Group());
        scene2.getRoot().getChildren().add(n());
        nodes.get(1).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        assertFalse(nodes.get(1).isFocused());
        assertEquals(nodes.get(1), scene2.impl_getFocusOwner());
        stage.setScene(scene2);
        fireTestPulse();
        assertFalse(nodes.get(0).isFocused());
        assertEquals(nodes.get(0), scene.impl_getFocusOwner());
        assertIsFocused(scene2, nodes.get(1));
    }
    
    // TODO: tests for moving nodes between scenes
    // and active and inactive stages
}
