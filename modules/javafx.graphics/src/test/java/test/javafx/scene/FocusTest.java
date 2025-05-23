/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import com.sun.javafx.scene.SceneHelper;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.scene.NodeShim;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import test.com.sun.javafx.pgstub.StubScene;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.SceneShim;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FocusTest {

    private Stage stage;
    private Scene scene;
    private List<Node> nodes;
    private int nodeIndex;
    private StubToolkit toolkit;
    private boolean actionTaken;

    @BeforeEach
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);
        stage.show();
        stage.requestFocus();
        nodes = new ArrayList();
        nodeIndex = 0;

        toolkit = (StubToolkit) Toolkit.getToolkit();
    }

    @AfterEach
    public void tearDown() {
        stage.hide();
        stage = null;
        scene = null;
    }

    void fireTestPulse() {
        // TODO: an actual pulse doesn't work in the stub environment.
        // Just clean up focus instead.
        SceneShim.focusCleanup(scene);
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

    private void assertIsFocused(Node n) {
        assertTrue(n.isFocused());
        assertTrue(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focused")));
    }

    private void assertIsFocused(Node... ns) {
        for (Node n : ns) {
            assertIsFocused(n);
        }
    }

    private void assertIsFocused(Scene s, Node n) {
        assertEquals(n, s.getFocusOwner());
        assertIsFocused(n);
    }

    private void assertNotFocused(Node n) {
        assertFalse(n.isFocused());
        assertFalse(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focused")));
    }

    private void assertNotFocused(Node... ns) {
        for (Node n : ns) {
            assertNotFocused(n);
        }
    }

    private void assertNotFocused(Scene s, Node n) {
        assertTrue(n != s.getFocusOwner());
        assertNotFocused(n);
    }

    private void assertNullFocus(Scene s) {
        assertNull(s.getFocusOwner());
    }

    private void assertIsFocusVisible(Node n) {
        assertTrue(n.isFocusVisible());
        assertTrue(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focus-visible")));
    }

    private void assertNotFocusVisible(Node n) {
        assertFalse(n.isFocusVisible());
        assertFalse(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focus-visible")));
    }

    private void assertIsFocusWithin(Node n) {
        assertTrue(n.isFocusWithin());
        assertTrue(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focus-within")));
    }

    private void assertNotFocusWithin(Node n) {
        assertFalse(n.isFocusWithin());
        assertFalse(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focus-within")));
    }

    private void assertIsFocusWithinParents(Node n) {
        do {
            assertTrue(n.isFocusWithin());
            assertTrue(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focus-within")));
            n = n.getParent();
        } while (n != null);
    }

    private void assertNotFocusWithinParents(Node n) {
        do {
            assertFalse(n.isFocusWithin());
            assertFalse(n.getPseudoClassStates().stream().anyMatch(pc -> pc.getPseudoClassName().equals("focus-within")));
            n = n.getParent();
        } while (n != null);
    }

    /**
     * Test setting of initial focus.
     */
    @Test
    public void testInitial() {
        assertNullFocus(scene);
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * Test requestFocus() on eligible and ineligible nodes.
     */
    @Test
    public void testRequest() {
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).addAll(
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
        ParentShim.getChildren(scene.getRoot()).add(n());
        nodes.get(0).requestFocus();
        ParentShim.getChildren(scene.getRoot()).remove(nodes.get(0));
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
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n());
        nodes.get(0).requestFocus();
        ParentShim.getChildren(scene.getRoot()).remove(nodes.get(0));
        fireTestPulse();
        assertNotFocused(scene, nodes.get(0));
        assertIsFocused(scene, nodes.get(1));
    }

    /**
     * Test removing the focus owner without another eligible node in the scene.
     */
    @Test
    public void testRemove_ClearsFocusOnRemovedNode1() {
        Node n = n();
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).add(n);
        n.requestFocus();
        ParentShim.getChildren(scene.getRoot()).remove(n);
        fireTestPulse();
        assertNotFocused(scene, n);
    }

    /**
     * Test removing the focus owner with another eligible node in the scene.
     */
    @Test
    public void testRemove_ClearsFocusOnRemovedNode2() {
        Node n = n();
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).addAll(n, n());
        n.requestFocus();
        ParentShim.getChildren(scene.getRoot()).remove(n);
        fireTestPulse();
        assertNotFocused(scene, n);
        assertIsFocused(scene, ParentShim.getChildren(scene.getRoot()).get(0));
    }

    /**
     * Test removing the focus owner without another eligible node in the scene.
     */
    @Test
    public void testRemoveChildOfGroup_ClearsFocusOnRemovedNode1() {
        Node n = n();
        Group g = new Group(n);
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).add(g);
        n.requestFocus();
        ParentShim.getChildren(g).remove(n);
        fireTestPulse();
        assertNotFocused(scene, n);
    }

    /**
     * Test making the focus owner invisible without another eligible
     * node in the scene.
     */
    @Test
    public void testInvisible1() {
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).add(n());
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
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n());
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
        ParentShim.getChildren(scene.getRoot()).add(n());
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
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n());
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
        ParentShim.getChildren(scene.getRoot()).add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus null, test making a node traversable.
     */
    @Test
    public void testBecomeTraversable() {
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).addAll(n(F, T, T), n(F, T, T));
        fireTestPulse();
        assertNullFocus(scene);
        toolkit.clearPulseRequested();
        assertFalse(toolkit.isPulseRequested());
        nodes.get(0).setFocusTraversable(true);
        assertTrue(toolkit.isPulseRequested());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus null, test making a node visible.
     */
    @Test
    public void testBecomeVisible() {
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).add(n(T, F, T));
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
        ParentShim.getChildren(scene.getRoot()).add(n(T, T, F));
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
        ParentShim.getChildren(scene.getRoot()).add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        ParentShim.getChildren(scene.getRoot()).add(n());
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
    }

    /**
     * When focus exists, test making a node traversable.
     */
    @Test
    public void testBecomeTraversable2() {
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n(F, T, T));
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
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n(T, F, T));
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
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n(T, T, F));
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        ParentShim.getChildren(g2).add(nodes.get(0));
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        ParentShim.getChildren(g2).add(nodes.get(0));
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2, n());
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        ParentShim.getChildren(g2).add(nodes.get(0));
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2);
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        ParentShim.getChildren(g2).add(nodes.get(0));
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2, n());
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        ParentShim.getChildren(g2).add(nodes.get(0));
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2);
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2, n());
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2);
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
        ParentShim.getChildren(scene.getRoot()).addAll(g1, g2, n());
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
        ParentShim.getChildren(scene.getRoot()).addAll(n(), n());
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
        ParentShim.getChildren(scene.getRoot()).add(n());
        nodes.get(0).requestFocus();
        assertIsFocused(scene, nodes.get(0));
        Scene scene2 = new Scene(new Group());
        ParentShim.getChildren(scene2.getRoot()).add(nodes.get(0));
        fireTestPulse();
        assertNullFocus(scene);
        assertNullFocus(scene2);
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertNullFocus(scene);
        assertEquals(nodes.get(0), scene2.getFocusOwner());
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
        ParentShim.getChildren(scene.getRoot()).add(n());
        nodes.get(0).requestFocus();
        stage.show();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        stage.hide();
        nodes.get(0).requestFocus();
        fireTestPulse();
        assertEquals(nodes.get(0), scene.getFocusOwner());
        assertFalse(nodes.get(0).isFocused());
    }

    /**
     * Test switching two scenes in one stage. Focus owner in scenes should
     * remain the same while focused node should change.
     */
    @Test
    public void testSwitchScenes(){
        scene.setRoot(new Group());
        ParentShim.getChildren(scene.getRoot()).add(n());
        nodes.get(0).requestFocus();
        Scene scene2 = new Scene(new Group());
        ParentShim.getChildren(scene2.getRoot()).add(n());
        nodes.get(1).requestFocus();
        fireTestPulse();
        assertIsFocused(scene, nodes.get(0));
        assertFalse(nodes.get(1).isFocused());
        assertEquals(nodes.get(1), scene2.getFocusOwner());
        stage.setScene(scene2);
        fireTestPulse();
        assertFalse(nodes.get(0).isFocused());
        assertEquals(nodes.get(0), scene.getFocusOwner());
        assertIsFocused(scene2, nodes.get(1));
    }

    @Test public void nestedFocusRequestsShouldResultInOneFocusedNode() {
        final Node n1 = n();
        final Node n2 = n();
        scene.setRoot(new Group(n1, n2));

        n1.focusedProperty().addListener((ov, lostFocus, getFocus) -> {
            if (lostFocus) {
                n1.requestFocus();
            }
        });

        n2.focusedProperty().addListener(o -> {
            // n2 can be invalidated, but should not have focus
            assertTrue(n1.isFocused());
            assertFalse(n2.isFocused());
        });

        n2.focusedProperty().addListener((ov, lostFocus, getFocus) -> fail("n2 should never get focus"));

        stage.show();
        n1.requestFocus();
        assertTrue(n1.isFocused());
        assertFalse(n2.isFocused());

        n2.requestFocus();
        assertTrue(n1.isFocused());
        assertFalse(n2.isFocused());
    }

    @Test public void shouldCancelInputMethodWhenLosingFocus() {
        final Node n1 = n();
        final Node n2 = n();
        scene.setRoot(new Group(n1, n2));

        stage.show();

        Toolkit.getToolkit().firePulse();

        n1.requestFocus();
        assertSame(n1, scene.getFocusOwner());
        actionTaken = false;

        ((StubScene) SceneHelper.getPeer(scene)).setInputMethodCompositionFinishDelegate(
                () -> {
                    assertSame(n1, scene.getFocusOwner());
                    actionTaken = true;
                }
        );

        n2.requestFocus();

        ((StubScene) SceneHelper.getPeer(scene)).setInputMethodCompositionFinishDelegate(
                null);

        assertSame(n2, scene.getFocusOwner());
        assertTrue(actionTaken);
    }

    private void fireTabKeyEvent(Node node) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.TAB, false, false, false, false));
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_RELEASED, null, null, KeyCode.TAB, false, false, false, false));
    }

    private void fireMousePressedEvent(EventTarget target) {
        double x = 10, y = 10;
        PickResult pickResult = new PickResult(target, x, y);
        Event.fireEvent(target, new MouseEvent(
                MouseEvent.MOUSE_PRESSED, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false,
                true, false, false,
                false, false, false, pickResult));
    }

    private void fireTouchPressedEvent(EventTarget target) {
        double x = 10, y = 10;
        PickResult pickResult = new PickResult(scene, x, y);
        Event.fireEvent(target, new TouchEvent(
                TouchEvent.TOUCH_PRESSED,
                new TouchPoint(0, TouchPoint.State.PRESSED, x, y, x, y, target, pickResult),
                Collections.emptyList(), 0, false, false, false, false));
    }

    /**
     * If a node acquires focus by calling {@link Node#requestFocus()}, it does not acquire visible focus.
     */
    @Test public void testDefaultFocusTraversalDoesNotSetFocusVisible() {
        Node node = n();
        scene.setRoot(new Group(node));

        assertNotFocused(scene, node);
        assertNotFocusVisible(node);

        node.requestFocus();

        assertIsFocused(scene, node);
        assertNotFocusVisible(node);
    }

    /**
     * If a node acquires focus when the TAB key is pressed, it also acquires visible focus.
     */
    @Test public void testKeyFocusTraversalSetsFocusVisible() {
        Node node = n();
        Group g = new Group(node);
        scene.setRoot(g);

        assertNotFocused(scene, node);
        assertNotFocusVisible(node);

        fireTabKeyEvent(g);

        assertIsFocused(scene, node);
        assertIsFocusVisible(node);
    }

    /**
     * If {@link Node#requestFocus()} is called on a node that has acquired visible focus,
     * visible focus is removed from the node.
     */
    @Test public void testFocusVisibleIsRemovedByDefaultRequestFocus() {
        Node node = n();
        Group g = new Group(node);
        scene.setRoot(g);
        fireTabKeyEvent(g);

        assertIsFocused(scene, node);
        assertIsFocusVisible(node);

        node.requestFocus();

        assertIsFocused(scene, node);
        assertNotFocusVisible(node);
    }

    /**
     * When a node loses focus, it also loses visible focus.
     */
    @Test public void testVisibleFocusIsRemovedWhenFocusIsRemoved() {
        Node node1 = n();
        Node node2 = n();
        Group g = new Group(node1, node2);
        scene.setRoot(g);
        fireTabKeyEvent(g);

        assertIsFocused(scene, node1);
        assertIsFocusVisible(node1);

        node2.requestFocus();

        assertNotFocused(scene, node1);
        assertNotFocusVisible(node1);
        assertIsFocused(scene, node2);
        assertNotFocusVisible(node2);
    }

    /**
     * When any region of the window is clicked, the focus owner loses visible focus
     * even when the focus owner doesn't change.
     */
    @Test public void testMousePressedClearsFocusVisible() {
        Node node1 = n(), node2 = n();
        Group g = new Group(node1, node2);
        scene.setRoot(g);
        fireTabKeyEvent(g);

        assertIsFocused(scene, node1);
        assertIsFocusVisible(node1);

        fireMousePressedEvent(scene);

        assertIsFocused(scene, node1);
        assertNotFocusVisible(node1);
    }

    /**
     * When any region of the window is touched, the focus owner loses visible focus
     * even when the focus owner doesn't change.
     */
    @Test public void testTouchPressedClearsFocusVisible() {
        Node node1 = n(), node2 = n();
        Group g = new Group(node1, node2);
        scene.setRoot(g);
        fireTabKeyEvent(g);

        assertIsFocused(scene, node1);
        assertIsFocusVisible(node1);

        fireTouchPressedEvent(scene);

        assertIsFocused(scene, node1);
        assertNotFocusVisible(node1);
    }

    /**
     * When a node acquires focus, the focusWithin property is set on the node
     * and all of its parents.
     */
    @Test public void testFocusWithinIsTrueOnAllParents() {
        Node node1 = n();
        Group g = new Group(new Group(new Group(node1)));
        scene.setRoot(g);

        assertNotFocusWithinParents(node1);

        node1.requestFocus();

        assertIsFocusWithinParents(node1);
    }

    /**
     * When a node loses focus, the focusWithin property of its parents is cleared.
     */
    @Test public void testFocusWithinIsRemovedFromParentsAfterChangingFocusOwner() {
        Node node1 = n(), node2 = n();
        Group g = new Group(new Group(new Group(node1)), new Group(new Group(node2)));
        scene.setRoot(g);

        assertNotFocusWithinParents(node1);
        assertNotFocusWithinParents(node2);

        node1.requestFocus();

        assertIsFocusWithinParents(node1);
        assertNotFocusWithin(node2);
        assertNotFocusWithin(node2.getParent());
        assertNotFocusWithin(node2.getParent().getParent());

        node2.requestFocus();

        assertIsFocusWithinParents(node2);
        assertNotFocusWithin(node1);
        assertNotFocusWithin(node1.getParent());
        assertNotFocusWithin(node1.getParent().getParent());
    }

    /**
     * When a node loses focus, all of its parents also lose focusWithin.
     * However, if focus transitions to a new node, and the new node is also a child of the
     * parent that just lost focusWithin, the parent will re-gain focusWithin.
     *
     * Since focus traversal is specified to be an atomic operation, the fact that
     * the parent technically lost and re-gained focusWithin must not be observable.
     */
    @Test public void testFocusWithinListenerIsNotInvokedIfPropertyDidNotEffectivelyChange() {
        Node node1 = n(), node2 = n();
        Group g = new Group(new Group(new Group(node1)), new Group(new Group(node2)));
        scene.setRoot(g);

        List<Boolean> focusWithinValues = new ArrayList<>();
        g.focusWithinProperty().addListener((observable, oldValue, newValue) -> focusWithinValues.add(newValue));

        node1.requestFocus();
        assertEquals(1, focusWithinValues.size());
        assertEquals(Boolean.TRUE, focusWithinValues.get(0));

        node2.requestFocus();
        assertEquals(1, focusWithinValues.size());
        assertEquals(Boolean.TRUE, focusWithinValues.get(0));
    }

    /**
     * When a focused node is removed from the scene graph, the focus states
     * of its former parents are cleared.
     */
    @Test public void testFocusStatesAreClearedFromFormerParentsOfFocusedNode() {
        Node node1 = n(), node2 = n();
        Group g2, g3, g1 = new Group(g2 = new Group(g3 = new Group(node1)), new Group(new Group(node2)));
        scene.setRoot(g1);

        node1.requestFocus();
        assertIsFocusWithin(g1);
        assertIsFocusWithin(g2);
        assertIsFocusWithin(g3);

        g2.getChildren().remove(0);
        assertNotFocusWithin(g1);
        assertNotFocusWithin(g2);
    }

    /**
     * When a scene graph contains multiple nested focused nodes, the focusWithin bits that are
     * cleared when a focused node is removed must only be cleared as long as we don't encounter
     * another focused node up the tree.
     */
    @Test public void testMultiLevelFocusWithinIsPreservedWhenFocusedNodeIsRemoved() {
        class N extends Group {
            N(Node... children) {
                super(children);
                setFocusTraversable(true);
            }

            void setFocused() {
                setFocused(true);
            }
        }

        N node1, node2, node3, node4, node5;

        scene.setRoot(
            node1 = new N(                  // focusWithin=3
                node2 = new N(              // focusWithin=3, focused
                    node3 = new N(          // focusWithin=2
                        node4 = new N(      // focusWithin=2, focused
                            node5 = new N() // focusWithin=1, focused
                        )
                    )
                )
            ));

        node2.setFocused();
        node4.setFocused();
        node5.setFocused();

        // Detach node4 from the scene graph:
        //     node1           focusWithin=1
        //     |--> node2      focusWithin=1, focused
        //         |--> node3
        //
        //     node4           focusWithin=2, focused
        //     |--> node5      focusWithin=1, focused
        //
        node3.getChildren().clear();

        assertIsFocusWithin(node1);
        assertIsFocusWithin(node2);
        assertNotFocusWithin(node3);
        assertIsFocusWithin(node4);
        assertIsFocusWithin(node5);

        assertNotFocused(node1);
        assertIsFocused(node2);
        assertNotFocused(node3);
        assertIsFocused(node4);
        assertIsFocused(node5);
    }

    /**
     * When a scene graph contains multiple nested focused nodes, the focusWithin bits that would
     * be cleared when a focused node is de-focused must not be cleared when we have another
     * focused downstream node.
     */
    @Test public void testMultiLevelFocusWithinIsPreservedWhenIntermediateFocusedNodeIsDefocused() {
        class N extends Group {
            N(Node... children) { super(children); }
            void _setFocused(boolean value) { setFocused(value); }
        }

        N node1, node2, node3, node4;

        scene.setRoot(
            node1 = new N(
                node2 = new N(
                    node3 = new N(
                        node4 = new N()
                    )
                )
            ));

        node2._setFocused(true);
        node4._setFocused(true);
        assertIsFocusWithin(node1);
        assertIsFocusWithin(node2);
        assertIsFocusWithin(node3);
        assertIsFocusWithin(node4);

        node2._setFocused(false);
        assertIsFocusWithin(node1);
        assertIsFocusWithin(node2);
        assertIsFocusWithin(node3);
        assertIsFocusWithin(node4);

        node4._setFocused(false);
        assertNotFocusWithin(node1);
        assertNotFocusWithin(node2);
        assertNotFocusWithin(node3);
        assertNotFocusWithin(node4);
    }

    @Test public void testFocusWithinBitsAreSetOnParentsWhenAddedNodeIsAlreadyFocused() {
        class N extends Group {
            N(Node... children) { super(children); }
            void _setFocused(boolean value) { setFocused(value); }
        }

        N node1, node2, node3 = new N();

        scene.setRoot(
            node1 = new N(
                node2 = new N()
            )
        );

        node3._setFocused(true);
        assertNotFocusWithin(node1);
        assertNotFocusWithin(node2);

        node2.getChildren().add(node3);
        assertIsFocusWithin(node1);
        assertIsFocusWithin(node2);
        assertIsFocusWithin(node3);
    }

    @Test public void testFocusWithinBitsAreSetAndClearedOnParentsOfNodeContainingMultipleFocuses() {
        class N extends Group {
            N(Node... children) { super(children); }
            void _setFocused(boolean value) { setFocused(value); }
        }

        N node1, node2, node3, node4;

        scene.setRoot(
            node1 = new N(
                node2 = new N()
            )
        );

        node3 = new N(
            node4 = new N()
        );

        node3._setFocused(true);
        node4._setFocused(true);
        assertNotFocusWithin(node1);
        assertNotFocusWithin(node2);

        node2.getChildren().add(node3);
        assertIsFocusWithin(node1);
        assertIsFocusWithin(node2);
        assertIsFocusWithin(node3);

        node2.getChildren().clear();
        assertNotFocusWithin(node1);
        assertNotFocusWithin(node2);
    }

    @Nested
    class FocusDelegateTest {
        static class N extends Group {
            final Node delegate;
            final String name;

            N(String name, Node child, Node delegate) {
                super(child == null ? new Node[0] : new Node[] { child });
                this.delegate = delegate;
                this.name = name;
            }

            N(Node child, Node delegate) {
                this(null, child, delegate);
            }

            @Override
            protected Node getFocusDelegate(Node hoistingNode) {
                return delegate;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        static class Trace extends ArrayList<String> {
            Trace(Node... nodes) {
                for (Node node : nodes) {
                    node.addEventFilter(
                        Event.ANY,
                        e -> add(String.format("EventFilter[source=%s, target=%s]", e.getSource(), e.getTarget())));

                    node.addEventHandler(
                        Event.ANY,
                        e -> add(String.format("EventHandler[source=%s, target=%s]", e.getSource(), e.getTarget())));
                }
            }
        }

        /**
         * Focusing a node that delegates focus to a descendant will focus both the original node
         * and its descendant. This test asserts that this also works in nested scenarios, when the
         * delegation target delegates focus further down the scene graph.
         */
        @Test
        void focusIsDelegatedToDescendants() {
            N node1, node2, node3, node4, node5;

            // node1 . . . . . . . . . focusDelegate = node3
            // └── node2
            //     └── node3 . . . . . focusDelegate = node5
            //         └── node4
            //             └── node5
            scene.setRoot(
                node1 = new N(
                    node2 = new N(
                        node3 = new N(
                            node4 = new N(
                                node5 = new N(null, null), null
                            ), node5
                        ), null
                    ), node3
                ));

            assertNotFocused(node1, node2, node3, node4, node5);

            node1.requestFocus();

            assertIsFocused(node1, node3, node5);
            assertIsFocused(scene, node1);
        }

        /**
         * This test is similar to {@link #focusIsDelegatedToDescendants()}, except that instead of
         * focusing the root node, the intermediate focus delegate is focused. This will result in
         * the root node being unfocused, while the intermediate focus delegate and its delegation
         * target are focused.
         */
        @Test
        void focusIsDelegatedToSubtreeDescendants() {
            N node1, node2, node3, node4, node5;

            // node1 . . . . . . . . . focusDelegate = node3
            // └── node2
            //     └── node3 . . . . . focusDelegate = node5
            //         └── node4
            //             └── node5
            scene.setRoot(
                node1 = new N(
                    node2 = new N(
                        node3 = new N(
                            node4 = new N(
                                node5 = new N(null, null), null
                            ), node5
                        ), null
                    ), node3
                ));

            assertNotFocused(node1, node2, node3, node4, node5);

            node3.requestFocus();

            assertIsFocused(node3, node5);
            assertIsFocused(scene, node3);
        }

        /**
         * When a scene sends a key event to the focus owner, it is delivered up to the final target
         * of delegation. The event target changes as the event travels through the scene graph:
         * First, the event is targeted at the focus owner. For each descendant node, the event is
         * targeted at the next focus delegate. When the capturing phase completes and the event starts
         * to bubble up, it is targeted back to its original targets as it crosses the boundary between
         * focus delegates.
         */
        @Test
        void keyEventIsDeliveredToFocusedDescendants() {
            N node1, node2, node3, node4, node5;

            // node1 . . . . . . . . . focusDelegate = node3, focusOwner
            // └── node2
            //     └── node3 . . . . . focusDelegate = node5
            //         └── node4
            //             └── node5
            scene.setRoot(
                node1 = new N("node1",
                    node2 = new N("node2",
                        node3 = new N("node3",
                            node4 = new N("node4",
                                node5 = new N("node5",null, null), null
                            ), node5
                        ), null
                    ), node3
                ));

            node1.requestFocus();

            var trace = new Trace(node1, node2, node3, node4, node5);

            SceneShim.processKeyEvent(
                scene, new KeyEvent(KeyEvent.KEY_PRESSED, null, null, KeyCode.A, false, false, false, false));

            assertEquals(
                List.of(
                    "EventFilter[source=node1, target=node1]", // <-- target is node1 (focusOwner)
                    "EventFilter[source=node2, target=node3]", // <-- change target to node3 (delegate)
                    "EventFilter[source=node3, target=node3]",
                    "EventFilter[source=node4, target=node5]", // <-- change target to node5 (delegate)
                    "EventFilter[source=node5, target=node5]",
                    "EventHandler[source=node5, target=node5]",
                    "EventHandler[source=node4, target=node5]",
                    "EventHandler[source=node3, target=node3]", // <-- change target back to node3
                    "EventHandler[source=node2, target=node3]",
                    "EventHandler[source=node1, target=node1]" // <-- change target back to node1
                ),
                trace
            );
        }
    }

    @Nested
    class FocusScopeTest {
        static class N extends Group {
            final boolean focusScope;

            N(boolean focusScope, boolean hoistFocus, Node child) {
                super(child == null ? new Node[0] : new Node[] { child });
                this.focusScope = focusScope;
                setHoistFocus(hoistFocus);
            }

            @Override
            protected boolean isFocusScope() {
                return focusScope;
            }

            Node getHoistingNode() {
                try {
                    var method = focusedProperty().getClass().getDeclaredMethod("getHoistingNode");
                    method.setAccessible(true);
                    return (Node)method.invoke(focusedProperty(), (Object[])null);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            }
        }

        /**
         * A node with the {@link Node#hoistFocusProperty()} flag will hoist a focus request to the next
         * focus scope. The result is that the node that defines the focus scope is focused, and the node
         * that initially received the focus request is not.
         */
        @Test
        void hoistFocusRequestToNextScope() {
            N node1, node2, node3, node4, node5;

            // node1 . . . . . . . . . . [hoistFocus]
            // └── node2 . . . . . . . . [hoistFocus]
            //     └── node3 . . . . . . [focusScope, hoistFocus]
            //         └── node4 . . . . [hoistFocus]
            //             └── node5 . . [hoistFocus]
            scene.setRoot(
                node1 = new N(false, false,
                    node2 = new N(false, true,
                        node3 = new N(true, true,
                            node4 = new N(false, true,
                                node5 = new N(false, true, null)
                            )
                        )
                    )
                ));

            node4.requestFocus();
            assertIsFocused(node3);
            assertNotFocused(node1, node2, node4, node5);
            assertTrue(Stream.of(node1, node2, node4, node5).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node4, node3.getHoistingNode());

            node5.requestFocus();
            assertIsFocused(node3);
            assertNotFocused(node1, node2, node4, node5);
            assertTrue(Stream.of(node1, node2, node4, node5).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node5, node3.getHoistingNode());
        }

        /**
         * A node with the {@link Node#hoistFocusProperty()} flag will hoist a focus request to the next
         * focus scope. In this test, the result is that the node that defines the outermost focus scope
         * is focused, and all other nodes are not focused (this includes the intermediate focus scope).
         */
        @Test
        void hoistFocusRequestToOutermostScope() {
            N node1, node2, node3, node4, node5;

            // node1 . . . . . . . . . . [focusScope]
            // └── node2 . . . . . . . . [hoistFocus]
            //     └── node3 . . . . . . [focusScope, hoistFocus]
            //         └── node4 . . . . [hoistFocus]
            //             └── node5 . . [hoistFocus]
            scene.setRoot(
                node1 = new N(true, false,
                    node2 = new N(false, true,
                        node3 = new N(true, true,
                            node4 = new N(false, true,
                                node5 = new N(false, true, null)
                            )
                        )
                    )
                ));

            node4.requestFocus();
            assertIsFocused(node1);
            assertNotFocused(node2, node3, node4, node5);
            assertTrue(Stream.of(node2, node4, node5).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node4, node3.getHoistingNode());
            assertSame(node3, node1.getHoistingNode());

            node5.requestFocus();
            assertIsFocused(node1);
            assertNotFocused(node2, node3, node4, node5);
            assertTrue(Stream.of(node2, node4, node5).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node5, node3.getHoistingNode());
            assertSame(node3, node1.getHoistingNode());
        }

        /**
         * A node with the {@link Node#hoistFocusProperty()} flag will hoist a focus request to the next
         * focus scope. If the next focus scope does not have the {@link Node#hoistFocusProperty()} flag,
         * the focus request will not be hoisted again, even when an ancestor defines another focus scope.
         */
        @Test
        void hoistFocusRequestStopsWhenHoistFocusIsFalse() {
            N node1, node2, node3, node4, node5;

            // node1 . . . . . . . . . . [focusScope]
            // └── node2 . . . . . . . . [hoistFocus]
            //     └── node3 . . . . . . [focusScope]
            //         └── node4 . . . . [hoistFocus]
            //             └── node5 . . [hoistFocus]
            scene.setRoot(
                node1 = new N(true, false,
                    node2 = new N(false, true,
                        node3 = new N(true, false,
                            node4 = new N(false, true,
                                node5 = new N(false, true, null)
                            )
                        )
                    )
                ));

            node4.requestFocus();
            assertIsFocused(node3);
            assertNotFocused(node1, node2, node4, node5);
            assertTrue(Stream.of(node1, node2, node4, node5).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node4, node3.getHoistingNode());

            node5.requestFocus();
            assertIsFocused(node3);
            assertNotFocused(node1, node2, node4, node5);
            assertTrue(Stream.of(node1, node2, node4, node5).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node5, node3.getHoistingNode());
        }

        /**
         * When a node hoists a focus request to its focus scope, the receiving node will track the hoisting
         * node internally. Once the focus scope node loses focus, it will clear the reference to the node
         * that hoisted the focus request.
         */
        @Test
        void losingFocusClearsTrackedHoistingNode() {
            N node1, node2, node3;

            // node1 . . . . . . . . . . [focusScope]
            // └── node2 . . . . . . . . [hoistFocus]
            //     └── node3 . . . . . . [hoistFocus]
            scene.setRoot(
                node1 = new N(true, false,
                    node2 = new N(false, true,
                        node3 = new N(false, true, null)
                    )
                ));

            node3.requestFocus();
            assertIsFocused(node1);
            assertNotFocused(node2, node3);
            assertTrue(Stream.of(node2, node3).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node3, node1.getHoistingNode());

            NodeShim.setFocusQuietly(node1, false, false);
            NodeShim.notifyFocusListeners(node1);
            assertNotFocused(node1, node2, node3);
            assertTrue(Stream.of(node1, node2, node3).allMatch(n -> n.getHoistingNode() == null));
        }

        /**
         * When a node hoists a focus request to its focus scope, the receiving node will track the hoisting
         * node internally. If the hoisting node is moved to a different place in the scene graph such that
         * it is no longer a descendant of the focus scope node, the tracking is cleared.
         */
        @Test
        void hoistingNodeIsNotResolvedWhenMovedOutOfDescendantSubtree() {
            N node1, node2, node3, node4;

            // node1 . . . . . . . . . . []
            // └── node2 . . . . . . . . [focusScope]
            //     └── node3 . . . . . . [hoistFocus]
            //         └── node4 . . . . [hoistFocus]
            scene.setRoot(
                node1 = new N(false, false,
                    node2 = new N(true, false,
                        node3 = new N(false, true,
                            node4 = new N(false, true, null)
                        )
                    )
                ));

            node4.requestFocus();
            assertIsFocused(node2);
            assertNotFocused(node1, node3, node4);
            assertTrue(Stream.of(node1, node3, node4).allMatch(n -> n.getHoistingNode() == null));
            assertSame(node4, node2.getHoistingNode());

            // node1 . . . . . . . . . . []
            // └── node2 . . . . . . . . [focusScope]
            // |   └── node3 . . . . . . [hoistFocus]
            // └── node4 . . . . . . . . [hoistFocus]
            node3.getChildren().clear();
            node1.getChildren().add(node4);
            assertIsFocused(node2);
            assertNotFocused(node1, node3, node4);
            assertTrue(Stream.of(node1, node2, node3, node4).allMatch(n -> n.getHoistingNode() == null));
        }
    }
}
