/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;
import static org.junit.Assert.assertEquals;

/**
 * A visual styling regression has been fixed under JDK8183100.
 * This test validates it.
 */
public class JDK8183100Test extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;
    private int count = 0;
    private final double TOLERANCE = 0.07;
    static List<TabContents> TAB_CONTENTS = new ArrayList<>();

    class TabContents extends StackPane {
        public TabContents() {
            // It is important that this node has some styling,
            // either via setStyle() or getStyleClass().add().
            setStyle("-fx-background-color: blue");

            Platform.runLater(() -> {
                TAB_CONTENTS.add(this);
                TAB_CONTENTS.forEach(TabContents::fillWithFreshYellowPane);
            });
        }

        void fillWithFreshYellowPane() {
            Pane yellowPane = new Pane();
            yellowPane.setStyle("-fx-background-color: yellow");
            getChildren().setAll(yellowPane);
        }
    }

    @Test(timeout=15000)
    public void stackPaneColorTest() {
        final int WIDTH = 200;
        final int HEIGHT = 100;

        // Top half of the app: Horizontal navigation bar above the application.
        Button addTabButton = new Button("Add tab");
        HBox tabBar = new HBox(addTabButton);

        // Bottom half of the app: The actual tab contents.
        StackPane container = new StackPane();

        // It is important that this node has some styling,
        // either via setStyle() or getStyleClass().add().
        container.setStyle("-fx-background-color: red");
        VBox.setVgrow(container, Priority.ALWAYS);

        VBox root = new VBox(tabBar, container);

        ToggleGroup group = new ToggleGroup();

        addTabButton.setOnAction(unused -> {
            ToggleButton tb = new ToggleButton("Tab "+count);
            count++;

            TabContents contents = new TabContents();
            runAndWait(() -> {
                tb.setToggleGroup(group);
                tb.setSelected(true);
                tabBar.getChildren().add(tb);

                // Immediately select the new tab...
                container.getChildren().setAll(contents);
            });

            // Add key accelerator for 'Tab 0' button only
            if (count == 1) {
                tb.getScene().getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DIGIT0, KeyCodeCombination.CONTROL_DOWN),
                    new Runnable() {
                        @Override public void run() {
                           tb.fire();
                        }
                    });
            }

            tb.setOnAction(actionEvent -> {
                container.getChildren().setAll(contents);
            });
        });

        runAndWait(() -> {
            testStage = getStage();
            testScene = new Scene(root, WIDTH, HEIGHT);

            testStage.setScene(testScene);

            // Add key accelerator for addTabButton
            addTabButton.getScene().getAccelerators().put(
            new KeyCodeCombination(KeyCode.A, KeyCodeCombination.CONTROL_DOWN),
            new Runnable() {
                @Override public void run() {
                    addTabButton.fire();
                }
            });

            testStage.show();
        });

        waitFirstFrame();

        KeyEventFirer keyboard = new KeyEventFirer(testScene);

        // Add 3 buttons by pressing Ctrl+A on keyboard
        for (int i = 0; i < 3; i++) {
            keyboard.doKeyPress(KeyCode.A, KeyModifier.CTRL);
            assertEquals(i+1, count);
        }

        // Select Tab0 by pressing Ctrl+0
        runAndWait(() -> {
            keyboard.doKeyPress(KeyCode.DIGIT0, KeyModifier.CTRL);
        });

        runAndWait(() -> {
            Color color = getColor(testScene, 100, 50); // center pixel of scene
            assertColorEquals(Color.YELLOW, color, TOLERANCE);
        });
    }
}
