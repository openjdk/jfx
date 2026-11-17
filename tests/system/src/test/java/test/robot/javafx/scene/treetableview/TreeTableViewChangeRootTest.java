/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene.treetableview;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeTableViewChangeRootTest {

    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 600;
    static final int SCENE_HEIGHT = 500;
    static final CountDownLatch startupLatch = new CountDownLatch(1);
    static TreeTableView<String> tree;
    static Button changeRootButton;

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    @Test
    public void testChangeRoot() {
        Node buttonNode = scene.lookup(".button");
        Bounds bounds = buttonNode.localToScreen(buttonNode.getBoundsInLocal());
        double x = bounds.getMinX() + bounds.getWidth() / 2;
        double y = bounds.getMinY() + bounds.getHeight() / 2;

        Util.runAndWait(() -> {
            robot.mouseMove((int) x, (int) y);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {

            Node disclosure = scene.lookup(".tree-disclosure-node");
            if (disclosure == null) {
                throw new IllegalStateException("Disclosure node not found.");
            }

            Bounds dbounds = disclosure.localToScreen(disclosure.getBoundsInLocal());
            double dx = dbounds.getMinX() + dbounds.getWidth() / 2;
            double dy = dbounds.getMinY() + dbounds.getHeight() / 2;

            robot.mouseMove((int) dx, (int) dy);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });

        Util.sleep(1000);

        Set<Node> renderedRows = tree.lookupAll(".tree-table-row-cell");
        List<String> rendered = renderedRows.stream()
                .filter(n -> n instanceof TreeTableRow<?>)
                .map(n -> ((TreeTableRow<?>) n).getItem())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();

        assertTrue(rendered.containsAll(List.of("V0", "V1", "V2", "V3", "V4")),
                "Rows should contain 5 children after expanding the root");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;
            tree = new TreeTableView<>();
            tree.setShowRoot(true);

            TreeTableColumn<String, String> column = new TreeTableColumn<>("Name");
            column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));
            column.setPrefWidth(300);
            tree.getColumns().add(column);

            changeRootButton = new Button("Change Root");
            changeRootButton.setOnAction(e -> {
                TreeItem<String> newRoot = new TreeItem<>();
                for (int i = 0; i < 5; i++) {
                    newRoot.getChildren().add(new TreeItem<>("V" + i));
                }
                tree.setRoot(newRoot);
            });

            VBox layout = new VBox(10, changeRootButton, tree);
            scene = new Scene(layout, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.show();

            // Pre-populate with 1 item to start
            TreeItem<String> initialRoot = new TreeItem<>();
            initialRoot.getChildren().add(new TreeItem<>("V0"));
            tree.setRoot(initialRoot);
        }
    }
}
