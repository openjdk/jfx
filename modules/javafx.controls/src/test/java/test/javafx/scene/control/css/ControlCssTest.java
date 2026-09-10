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
        if (stageLoader != null) {
            stageLoader.dispose();
        }
        errors.clear();
    }

    /**
     * When we swap the root of a scene, it should still correctly resolve the CSS.
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
     * When we swap a pane with a style class, the CSS for the children based of the pane should correctly resolve.
     */
    @Test
    void testPaneClassLookupResolvesAfterSceneListenerPaneSwap() {
        String theme = toBase64("""
                .my-pane {
                    -color: green;
                }
                .my-pane .label {
                    -fx-text-fill: -color;
                }
                """);

        TabPane tabPane = new TabPane();
        Label label = new Label("Test");
        tabPane.getTabs().add(new Tab("TestTab", label));

        StackPane myPane = new StackPane();
        myPane.getStyleClass().add("my-pane");

        AtomicBoolean swapped = new AtomicBoolean(false);
        label.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null && !swapped.getAndSet(true)) {
                StackPane newPaneRoot = new StackPane();
                Pane paneRoot = (Pane) myPane.getParent();

                paneRoot.getChildren().remove(myPane);
                paneRoot.getChildren().add(newPaneRoot);

                myPane.getStyleClass().remove("my-pane");
                newPaneRoot.getStyleClass().add("my-pane");

                newPaneRoot.getChildren().setAll(myPane);
            }
        });

        Button btn = new Button("Add Child");
        VBox root = new VBox(btn, myPane);
        btn.setOnAction(_ -> myPane.getChildren().add(tabPane));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(theme);
        stageLoader = new StageLoader(scene);

        btn.fire();
        Toolkit.getToolkit().firePulse();

        assertEquals(0, errors.size(), errors::toString);
    }

    /**
     * A node that is styled after the root was swapped must still resolve the looked-up colors of the new root.
     */
    @Test
    void testLookupResolvesForSiblingStyledAfterSceneListenerRootSwap() {
        String theme = toBase64("""
                .root {
                    -color: green;
                }
                .leaf {
                    -fx-background-color: -color;
                }
                """);

        TabPane tabPane = new TabPane();
        Label label = new Label("Test");
        tabPane.getTabs().add(new Tab("TestTab", label));

        swapRootWhenAddedToScene(label);

        StackPane leaf = new StackPane();
        leaf.getStyleClass().add("leaf");

        Button btn = new Button("Add Child");
        VBox root = new VBox(btn, leaf);
        // Inserted before the leaf, so that the leaf is styled after the tab pane swapped the root.
        btn.setOnAction(_ -> root.getChildren().add(1, tabPane));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(theme);
        stageLoader = new StageLoader(scene);

        btn.fire();
        Toolkit.getToolkit().firePulse();

        assertEquals(0, errors.size(), errors::toString);
    }

    /**
     * While the properties of a replaced style helper are being reset, no font relative property may be
     * recalculated, because the new styles are not applied yet.
     */
    @Test
    void testRelativeSizesAreNotRecalculatedWhileResettingProperties() {
        String theme = toBase64("""
                .container {
                    -fx-font-size: 40px;
                }
                .old {
                    -fx-font-size: 20px;
                    -fx-padding: 1em;
                }
                .new {
                    -fx-padding: 2em;
                }
                """);

        Label label = new Label("Test");
        label.getStyleClass().add("old");

        StackPane container = new StackPane(label);
        container.getStyleClass().add("container");

        Scene scene = new Scene(container);
        scene.getStylesheets().add(theme);
        stageLoader = new StageLoader(scene);

        assertEquals(new Insets(20), label.getPadding());

        List<Insets> observed = new ArrayList<>();
        label.paddingProperty().addListener((_, _, newValue) -> observed.add(newValue));

        // Resets the font, which recalculates the font relative padding.
        label.getStyleClass().setAll("new");
        Toolkit.getToolkit().firePulse();

        // A single change: recalculating during the reset would set the padding from the reset font first.
        assertEquals(List.of(new Insets(80)), observed, "padding must not be calculated with the old font-size");
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

    private static String toBase64(String css) {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

}
