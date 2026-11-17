/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.tk.Toolkit;
import javafx.css.PseudoClass;
import javafx.scene.NodeShim;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that pseudo-class changes on a node or its ancestors trigger a CSS update exactly
 * when a selector depends on them (trigger states).
 */
public class CssTriggerStatesTest {

    private final PseudoClass ps1 = PseudoClass.getPseudoClass("ps1");
    private final PseudoClass ps2 = PseudoClass.getPseudoClass("ps2");

    private StackPane root;
    private Stage stage;

    @BeforeEach
    void setup() {
        root = new StackPane();
        stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @AfterEach
    void cleanup() {
        stage.hide();
    }

    @Test
    void testOwnPseudoClass() {
        root.getStylesheets().add(toDataURL("""
                .a { -fx-background-color: green; }
                .a:ps1 { -fx-background-color: red; }
                """));

        Pane pane = createPaneWithStyle("a");
        root.getChildren().add(pane);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(pane));

        pane.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(pane));

        pane.pseudoClassStateChanged(ps1, false);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(pane));
    }

    @Test
    void testStyledAncestorPseudoClass() {
        root.getStylesheets().add(toDataURL("""
                .a { -fx-background-color: blue; }
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leaf = createPaneWithStyle("b");
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.BLUE, getBackgroundColor(parent));
        assertEquals(Color.GREEN, getBackgroundColor(leaf));

        parent.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf));

        parent.pseudoClassStateChanged(ps1, false);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leaf));
    }

    @Test
    void testUnstyledIntermediateAncestor() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leafLeaf = createPaneWithStyle("b");
        Pane leaf = createPaneWithStyle("c", leafLeaf);
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        parent.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leafLeaf));
    }

    @Test
    void testPseudoClassesOnMultipleAncestors() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .c:ps2 .b { -fx-background-color: red; }
                """));

        Pane leafLeaf = createPaneWithStyle("b");
        Pane leaf = createPaneWithStyle("c", leafLeaf);
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        parent.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leafLeaf));

        leaf.pseudoClassStateChanged(ps2, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leafLeaf));

        parent.pseudoClassStateChanged(ps1, false);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leafLeaf));
    }

    @Test
    void testSiblingsWithSameStylesAndDifferentAncestorStates() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leaf1 = createPaneWithStyle("b");
        Pane parent1 = createPaneWithStyle("a", leaf1);
        Pane leaf2 = createPaneWithStyle("b");
        Pane parent2 = createPaneWithStyle("a", leaf2);
        root.getChildren().addAll(parent1, parent2);
        Toolkit.getToolkit().firePulse();

        parent1.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf1));
        assertEquals(Color.GREEN, getBackgroundColor(leaf2));

        parent2.pseudoClassStateChanged(ps1, true);
        parent1.pseudoClassStateChanged(ps1, false);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leaf1));
        assertEquals(Color.RED, getBackgroundColor(leaf2));
    }

    @Test
    void testOnlyTriggerStatesMarkCssDirty() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leaf = createPaneWithStyle("b");
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();
        assertFalse(isCssDirty());

        parent.pseudoClassStateChanged(ps2, true);
        leaf.pseudoClassStateChanged(ps1, true);
        root.pseudoClassStateChanged(ps1, true);
        assertFalse(isCssDirty());

        parent.pseudoClassStateChanged(ps1, true);
        assertTrue(isCssDirty());
    }

    @Test
    void testTriggerStatesSurviveReapplyOfAncestor() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leaf = createPaneWithStyle("b");
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        // Will trigger a CSS reapply.
        parent.getStyleClass().add("x");
        Toolkit.getToolkit().firePulse();
        parent.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf));
    }

    @Test
    void testTriggerStatesSurviveReapplyOfStyledAncestor() {
        root.getStylesheets().add(toDataURL("""
                .a { -fx-background-color: blue; }
                .x { -fx-background-color: yellow; }
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leaf = createPaneWithStyle("b");
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        // Will trigger a CSS reapply.
        parent.getStyleClass().add("x");
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.YELLOW, getBackgroundColor(parent));

        parent.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf));
    }

    @Test
    void testMovedNodeRegistersTriggerStatesOnMove() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane leaf1 = createPaneWithStyle("b");
        Pane parent1 = createPaneWithStyle("a", leaf1);
        Pane parent2 = createPaneWithStyle("a");
        root.getChildren().addAll(parent1, parent2);
        Toolkit.getToolkit().firePulse();

        parent2.getChildren().add(leaf1);
        Toolkit.getToolkit().firePulse();
        assertFalse(isCssDirty());

        parent2.pseudoClassStateChanged(ps1, true);
        assertTrue(isCssDirty());
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf1));
        assertFalse(isCssDirty());
    }

    @Test
    void testAddedNodeRegistersTriggerStatesOnAncestor() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane parent = createPaneWithStyle("a");
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        // CSS not dirty as nobody is interested in the change yet.
        parent.pseudoClassStateChanged(ps1, true);
        assertFalse(isCssDirty());
        parent.pseudoClassStateChanged(ps1, false);

        Pane leaf = createPaneWithStyle("b");
        parent.getChildren().add(leaf);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leaf));

        parent.pseudoClassStateChanged(ps1, true);
        assertTrue(isCssDirty());
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf));
    }

    @Test
    void testAddedNodeUsesTriggerStates() {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                .a:ps1 .b { -fx-background-color: red; }
                """));

        Pane parent = createPaneWithStyle("a");
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        // CSS not dirty as nobody is interested in the change yet.
        parent.pseudoClassStateChanged(ps1, true);
        assertFalse(isCssDirty());

        Pane leaf = createPaneWithStyle("b");
        parent.getChildren().add(leaf);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf));

        parent.pseudoClassStateChanged(ps1, false);
        assertTrue(isCssDirty());
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leaf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ".a:ps1 .b",
            ".a:ps1 > .b",
            ".a:ps1 *",
            ".a:ps1 > *"
    })
    void testPseudoClassWithSelector(String selector) {
        root.getStylesheets().add(toDataURL("""
                .b { -fx-background-color: green; }
                %s { -fx-background-color: red; }
                """.formatted(selector)));

        Pane leaf = createPaneWithStyle("b");
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leaf));

        parent.pseudoClassStateChanged(ps1, true);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, getBackgroundColor(leaf));

        parent.pseudoClassStateChanged(ps1, false);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, getBackgroundColor(leaf));
    }

    @Test
    void testInheritSkipsUnstyledAncestorWithTriggerStates() {
        root.getStylesheets().add(toDataURL("""
                .a { -fx-background-color: blue; }
                .c { -fx-background-color: inherit; }
                .b:ps1 .c { -fx-opacity: 0.5; }
                """));

        Pane leafLeaf = createPaneWithStyle("c");
        Pane leaf = createPaneWithStyle("b", leafLeaf);
        Pane parent = createPaneWithStyle("a", leaf);
        root.getChildren().add(parent);
        Toolkit.getToolkit().firePulse();

        assertEquals(Color.BLUE, getBackgroundColor(leafLeaf));
    }

    private String toDataURL(String stylesheet) {
        return "data:text/plain;base64," + Base64.getEncoder().encodeToString(stylesheet.getBytes(StandardCharsets.UTF_8));
    }

    private Pane createPaneWithStyle(String styleClass, Region... children) {
        Pane pane = new Pane(children);
        pane.getStyleClass().add(styleClass);
        return pane;
    }

    private Paint getBackgroundColor(Region region) {
        return region.getBackground().getFills().getFirst().getFill();
    }

    private boolean isCssDirty() {
        return NodeShim.isDirty(root, DirtyBits.NODE_CSS);
    }
}
