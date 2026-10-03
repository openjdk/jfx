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

package test.javafx.scene.control.css;

import com.sun.javafx.tk.Toolkit;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.css.CssParser;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests CSS together with Controls and their Skins.
 */
public class ControlCssTest {

    private StageLoader stageLoader;
    private ObservableList<CssParser.ParseError> errors;

    @BeforeEach
    void setUp() {
        errors = CssParser.errorsProperty();
        errors.clear();
    }

    @AfterEach
    void tearDown() {
        errors.clear();
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    /**
     * When we swap the root of a scene in a listener of a child node, it should still correctly resolve the CSS.
     * The listener will run when the tab pane creates its skin, which will then add the tab content
     * with the label to the scene graph.
     *
     * <pre>{@code
     * Before:
     *
     * VBox
     * └── Button
     *
     * After:
     *
     * StackPane
     * └── VBox
     *     ├── Button
     *     └── TabPane
     *         └── Tab
     *             └── Label
     * }</pre>
     */
    @Test
    void testLookupResolvesAfterSceneListenerRootSwap() {
        TabPane tabPane = new TabPane();
        Label label = new Label("Test");
        tabPane.getTabs().add(new Tab("TestTab", label));

        swapRootWhenAddedToScene(label);

        Button btn = new Button("Add Child");
        VBox root = new VBox(btn);
        btn.setOnAction(_ -> root.getChildren().add(tabPane));

        stageLoader = new StageLoader(new Scene(root));

        btn.fire();
        Toolkit.getToolkit().firePulse();

        assertEquals(0, errors.size(), errors::toString);
    }

    /**
     * When we swap a pane and transfer the style classes in a listener of a child node,
     * the CSS for the children based of the pane should correctly resolve.
     * The listener will run when the tab pane creates its skin, which will then add the tab content
     * with the label to the scene graph.
     *
     * <pre>{@code
     * Before:
     *
     * VBox
     * ├── Button
     * └── Pane
     *
     * After:
     *
     * VBox
     * ├── Button
     * └── Pane
     *     └── Pane
     *         └── TabPane
     *             └── Tab
     *                 └── Label
     * }</pre>
     */
    @Test
    void testPaneClassLookupResolvesAfterSceneListenerPaneSwap() {
        TabPane tabPane = new TabPane();
        Label label = new Label("Test");
        tabPane.getTabs().add(new Tab("TestTab", label));

        Pane oldPaneRoot = createPaneWithStyle("my-pane");

        AtomicBoolean swapped = new AtomicBoolean(false);
        label.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null && !swapped.getAndSet(true)) {
                Pane newPaneRoot = new Pane();
                Pane paneRoot = (Pane) oldPaneRoot.getParent();

                paneRoot.getChildren().remove(oldPaneRoot);
                paneRoot.getChildren().add(newPaneRoot);

                oldPaneRoot.getStyleClass().remove("my-pane");
                newPaneRoot.getStyleClass().add("my-pane");

                newPaneRoot.getChildren().setAll(oldPaneRoot);
            }
        });

        Button btn = new Button("Add Child");
        VBox root = new VBox(btn, oldPaneRoot);
        btn.setOnAction(_ -> oldPaneRoot.getChildren().add(tabPane));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(toBase64("""
                .my-pane { -color: green; }
                .my-pane .label { -fx-text-fill: -color; }
                """));
        stageLoader = new StageLoader(scene);

        btn.fire();
        Toolkit.getToolkit().firePulse();

        assertEquals(0, errors.size(), errors::toString);
    }

    /**
     * A node that is styled after the root was swapped in a listener of a child node
     * must still resolve the looked-up colors of the new root.
     * The listener will run when the tab pane creates its skin, which will then add the tab content
     * with the label to the scene graph.
     *
     * <pre>{@code
     * Before:
     *
     * VBox
     * ├── Button
     * └── Pane
     *
     * After:
     *
     * StackPane
     * └── VBox
     *     ├── Button
     *     ├── TabPane
     *     │   └── Tab
     *     │       └── Label
     *     └── Pane
     * }</pre>
     */
    @Test
    void testLookupResolvesForSiblingStyledAfterSceneListenerRootSwap() {
        TabPane tabPane = new TabPane();
        Label label = new Label("Test");
        tabPane.getTabs().add(new Tab("TestTab", label));

        swapRootWhenAddedToScene(label);

        Pane leaf = createPaneWithStyle("leaf");

        Button btn = new Button("Add Child");
        VBox root = new VBox(btn, leaf);
        // Inserted before the leaf, so that the leaf is styled after the tab pane swapped the root.
        btn.setOnAction(_ -> root.getChildren().add(1, tabPane));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(toBase64("""
                .root { -color: green; }
                .leaf { -fx-background-color: -color; }
                """));
        stageLoader = new StageLoader(scene);

        btn.fire();
        Toolkit.getToolkit().firePulse();

        assertEquals(0, errors.size(), errors::toString);
    }

    /**
     * While the properties of a replaced style helper are being reset, no font relative property may be recalculated.
     * Otherwise, we will get wrong intermediate values in between. A listener should never observe any.
     */
    @Test
    void testRelativeSizesAreNotRecalculatedWhileResettingProperties() {
        Label label = new Label("Test");
        label.getStyleClass().add("old");

        Pane container = createPaneWithStyle("container", label);

        Scene scene = new Scene(container);
        scene.getStylesheets().add(toBase64("""
                .container { -fx-font-size: 40px; }
                .old { -fx-font-size: 20px; -fx-padding: 1em; }
                .new { -fx-padding: 2em; }
                """));
        stageLoader = new StageLoader(scene);

        assertEquals(new Insets(20), label.getPadding());

        List<Insets> observed = new ArrayList<>();
        label.paddingProperty().addListener((_, _, newValue) -> observed.add(newValue));

        // Resets the font, which recalculates the font relative padding.
        label.getStyleClass().set(1, "new");
        Toolkit.getToolkit().firePulse();

        // There should be only a single change.
        assertEquals(List.of(new Insets(80)), observed);
    }

    private static void swapRootWhenAddedToScene(Node node) {
        AtomicBoolean swapped = new AtomicBoolean(false);
        node.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null && !swapped.getAndSet(true)) {
                Parent oldRoot = newScene.getRoot();
                StackPane newRoot = new StackPane();
                newScene.setRoot(newRoot);
                newRoot.getChildren().setAll(oldRoot);
            }
        });
    }

    private static Pane createPaneWithStyle(String styleClass, Region... children) {
        Pane pane = new Pane(children);
        pane.getStyleClass().add(styleClass);
        return pane;
    }

    private static String toBase64(String css) {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }
}
