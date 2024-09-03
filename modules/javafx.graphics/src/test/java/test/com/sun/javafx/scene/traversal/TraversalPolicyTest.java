/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.traversal;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.traversal.FocusTraversal;
import javafx.scene.traversal.TraversalDirection;
import javafx.scene.traversal.TraversalEvent;
import javafx.scene.traversal.TraversalMethod;
import javafx.scene.traversal.TraversalPolicy;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;

/**
 * Tests TraversalPolicy APIs using the default and a custom traversal policies.
 */
public final class TraversalPolicyTest {
    private static StubToolkit tk;
    private static Stage stage;
    private static GridPane grid;
    private static Node t0;
    private static Node t1;
    private static Node t2;
    private static Node t3;
    private static Node b00;
    private static Node b01;
    private static Node b02;
    private static Node b10;
    private static Node b11;
    private static Node b12;
    private static Node b20;
    private static Node b21;
    private static Node b22;
    private Node fromEvent;

    /**
     * [T.0] [T.1] [T.2] [T.3]
     * -----------------------
     * [G.0.0] [G.1.0] [G.2.0]
     * [G.0.1] [G.1.1] [G.2.1]
     * [G.0.2] [G.1.2] [G.2.2]
     */
    @BeforeEach
    void beforeClass() {
        tk = (StubToolkit)Toolkit.getToolkit();

        t0 = b("T.0");
        t1 = b("T.1");
        t2 = b("T.2");
        t3 = b("T.3");

        b00 = b("b.0.0");
        b01 = b("b.0.1");
        b02 = b("b.0.2");
        b10 = b("b.1.0");
        b11 = b("b.1.1");
        b12 = b("b.1.2");
        b20 = b("b.2.0");
        b21 = b("b.2.1");
        b22 = b("b.2.2");

        grid = new GridPane();
        grid.add(b00, 0, 0);
        grid.add(b01, 0, 1);
        grid.add(b02, 0, 2);
        grid.add(b10, 1, 0);
        grid.add(b11, 1, 1);
        grid.add(b12, 1, 2);
        grid.add(b20, 2, 0);
        grid.add(b21, 2, 1);
        grid.add(b22, 2, 2);

        BorderPane bp = new BorderPane(grid);
        bp.setTop(new HBox(
            t0,
            t1,
            t2,
            t3
        ));

        stage = new Stage();
        stage.setScene(new Scene(bp, 500, 400));
        stage.addEventHandler(TraversalEvent.ANY, (ev) -> {
            //System.out.println(ev);
            fromEvent = ev.getNode();
        });
        stage.show();

        fromEvent = null;
    }

    @BeforeEach
    void beforeEach() {
        grid.setTraversalPolicy(null);
        stage.requestFocus();
        firePulse();
        t0.requestFocus();
        firePulse();
    }

    @AfterEach
    void afterEach() {
    }

    @AfterClass
    static void afterClass() {
        if (stage != null) {
            stage.hide();
            stage = null;
        }
    }

    void traverse(Node from, TraversalDirection dir, Node... nodes) {
        from.requestFocus();
        firePulse();
        checkFocused(from);

        for (Node n : nodes) {
            fromEvent = null;
            boolean success = FocusTraversal.traverse(from, dir, TraversalMethod.DEFAULT);
            Assertions.assertTrue(success, "failed to traverse from node: " + from);
            firePulse();
            checkFocused(n);
            checkEventNode(n);
            from = n;
        }
    }

    void checkFocused(Node n) {
        Assertions.assertTrue(n.isFocused(), "expecting focused node: " + n);
    }

    void checkEventNode(Node n) {
        Assertions.assertTrue(fromEvent == n, "TraversalEvent.node is wrong, expecting=" + n + ", observed=" + fromEvent);
        //System.out.println(fromEvent);
    }

    static void setCustomPolicy() {
      grid.setTraversalPolicy(customTraversalPolicy(
          b00,
          b10,
          b20,
          b01,
          b11,
          b21,
          b02,
          b12,
          b22
      ));
    }
    
    private static Rectangle b(String text) {
        Rectangle b = new Rectangle() {
            @Override
            public String toString() {
                return text;
            }
        };
        b.setWidth(80);
        b.setHeight(40);
        b.setFocusTraversable(true);
        return b;
    }

    private static void firePulse() {
        tk.firePulse();
    }

    static TraversalPolicy customTraversalPolicy(Node... nodes) {
        // This custom policy differs from default by explicitly specifying the traversal order.
        return new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                int ix = indexOf(owner);
                if (ix < 0) {
                    return null;
                }

                switch (dir) {
                case NEXT:
                    if (ix >= (nodes.length - 1)) {
                        // traversing up the stack from last node
                        return findNextFocusableNode(root, owner);
                    }
                    ix++;
                    break;
                case NEXT_IN_LINE:
                    if (ix >= (nodes.length - 1)) {
                        // traversing up the stack from last node
                        return findNextInLineFocusableNode(root, owner);
                    }
                    ix++;
                    break;
                case PREVIOUS:
                    if (ix <= 0) {
                        // traversing up the stack from the first node
                        return findPreviousFocusableNode(root, owner);
                    }
                    ix--;
                    break;
                case LEFT:
                case UP:
                    ix--;
                    break;
                case DOWN:
                case RIGHT:
                default:
                    ix++;
                }

                if (ix < 0) {
                    return selectLast(root);
                } else if (ix >= nodes.length) {
                    return selectFirst(root);
                }
                return nodes[ix];
            }

            @Override
            public Node selectFirst(Parent root) {
                return nodes[0];
            }

            @Override
            public Node selectLast(Parent root) {
                int ix = nodes.length - 1;
                if (ix < 0) {
                    return null;
                }
                return nodes[ix];
            }

            private int indexOf(Node n) {
                for (int i = nodes.length - 1; i >= 0; --i) {
                    if (nodes[i] == n) {
                        return i;
                    }
                }
                return -1;
            }
        };
    }

    // direction: DOWN, default policy
    @Test
    void testDefaultPolicy_DOWN() {
        traverse(
            t0,
            TraversalDirection.DOWN,
            b00, b01, b02
        );
    }

    // direction: DOWN, custom policy
    @Test
    void testCustomPolicy_DOWN() {
        setCustomPolicy();
        traverse(
            t0,
            TraversalDirection.DOWN,
            b00, b10, b20,
            b01, b11, b21,
            b02, b12, b22,
            b00, b10
        );
    }

    // direction: LEFT, default policy
    @Test
    void testDefaultPolicy_LEFT() {
        traverse(
            t3,
            TraversalDirection.LEFT,
            t2, t1, t0
        );
    }

    // direction: LEFT, default policy
    @Test
    void testDefaultPolicy_LEFT2() {
        traverse(
            b20,
            TraversalDirection.LEFT,
            b10, b00
        );
    }

    // direction: LEFT, custom policy, start at B20
    @Test
    void testCustomPolicy_LEFT() {
        setCustomPolicy();
        traverse(
            b20,
            TraversalDirection.LEFT,
            b10, b00,
            b22, b12, b02,
            b21, b11, b01,
            b20, b10, b00,
            b22
        );
    }

    // direction: NEXT, default policy
    @Test
    void testDefaultPolicy_NEXT() {
        traverse(
            t0,
            TraversalDirection.NEXT,
            t1, t2, t3,
            b00, b01, b02,
            b10, b11, b12,
            b20, b21, b22,
            t0, t1, t2, t3
        );
    }

    // direction: NEXT, custom policy
    @Test
    void testCustomPolicy_NEXT() {
        setCustomPolicy();
        traverse(
            t0,
            TraversalDirection.NEXT,
            t1, t2, t3,
            b00, b10, b20,
            b01, b11, b21,
            b02, b12, b22,
            t0, t1, t2, t3
        );
    }

    // direction: NEXT_IN_LINE, default policy
    @Test
    void testDefaultPolicy_NEXT_IN_LINE() {
        traverse(
            t0,
            TraversalDirection.NEXT_IN_LINE,
            t1, t2, t3,
            b00, b01, b02,
            b10, b11, b12,
            b20, b21, b22,
            t0, t1, t2, t3
        );
    }

    // direction: NEXT_IN_LINE, custom policy
    @Test
    void testCustomPolicy_NEXT_IN_LINE() {
        setCustomPolicy();
        traverse(
            t0,
            TraversalDirection.NEXT_IN_LINE,
            t1, t2, t3,
            b00, b10, b20,
            b01, b11, b21,
            b02, b12, b22,
            t0, t1, t2, t3
        );
    }

    // direction: PREVIOUS, default policy
    @Test
    void testDefaultPolicy_PREVIOUS() {
        traverse(
            t3,
            TraversalDirection.PREVIOUS,
            t2, t1, t0,
            b22
        );
    }

    // direction: PREVIOUS, custom policy
    @Test
    void testCustomPolicy_PREVIOUS() {
        setCustomPolicy();
        traverse(
            t3,
            TraversalDirection.PREVIOUS,
            t2, t1, t0,
            b22, b12, b02,
            b21, b11, b01,
            b20, b10, b00,
            t3, t2, t1, t0,
            b22
        );
    }

    // direction: RIGHT, default policy
    @Test
    void testDefaultPolicy_RIGHT() {
        traverse(
            t0,
            TraversalDirection.RIGHT,
            t1, t2, t3
        );
    }

    // direction: RIGHT, default policy, start at B00
    @Test
    void testDefaultPolicy_RIGHT2() {
        traverse(
            b00,
            TraversalDirection.RIGHT,
            b10, b20,
            t3
        );
    }

    // direction: RIGHT, custom policy
    @Test
    void testCustomPolicy_RIGHT() {
        setCustomPolicy();
        traverse(
            b00,
            TraversalDirection.RIGHT,
            b10, b20,
            b01, b11, b21,
            b02, b12, b22,
            b00
        );
    }

    // direction: UP, default policy
    @Test
    void testDefaultPolicy_UP() {
        traverse(
            b02,
            TraversalDirection.UP,
            b01, b00, t0
        );
    }

    // direction: UP, custom policy
    @Test
    void testCustomPolicy_UP() {
        setCustomPolicy();
        traverse(
            b02,
            TraversalDirection.UP,
            b21, b11, b01,
            b20, b10, b00,
            b22, b12, b02
        );
    }
}
