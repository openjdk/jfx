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

package test.robot.javafx.scene.treeview;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.FocusModel;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import javafx.scene.robot.Robot;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.util.Util;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TreeViewInitialFocusTest {

    private static final int SCENE_WIDTH = 600;
    private static final int SCENE_HEIGHT = 500;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final PseudoClass FOCUSED_PSEUDOCLASS = PseudoClass.getPseudoClass("focused");

    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static volatile TreeView<String> treeView;

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    @Test
    public void testInitialFocusDoesNotMoveToSecondVisibleItemWhenRootHidden() {
        Util.waitForLatch(startupLatch, 10, "Timeout waiting for test application to start");

        Util.runAndWait(() -> {
            Bounds bounds = treeView.localToScreen(treeView.getBoundsInLocal());
            double x = bounds.getMinX() + Math.min(40, bounds.getWidth() / 2.0);
            double y = bounds.getMinY() + 20;

            robot.mouseMove((int) x, (int) y);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });

        Util.sleep(300);

        AtomicInteger focusedIndex = new AtomicInteger(Integer.MIN_VALUE);
        AtomicReference<TreeItem<String>> focusedItem = new AtomicReference<>();

        Util.runAndWait(() -> {
            FocusModel<TreeItem<String>> fm = treeView.getFocusModel();
            focusedIndex.set(fm.getFocusedIndex());
            focusedItem.set(fm.getFocusedItem());

            Set<Node> cells = treeView.lookupAll(".tree-cell");
            int count = 0;
            String text = null;

            for (Node n : cells) {
                if (n instanceof TreeCell<?> cell &&
                        cell.getPseudoClassStates().contains(FOCUSED_PSEUDOCLASS)) {
                    count++;
                    Object item = cell.getItem();
                    text = item == null ? null : item.toString();
                }
            }
        });

        assertEquals(-1, focusedIndex.get(), "Focused index must be cleared");
        assertNull(focusedItem.get(), "Focused item must be null");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            treeView = new TreeView<>();

            TreeItem<String> root = new TreeItem<>("Root");
            treeView.setRoot(root);
            treeView.setShowRoot(false);

            root.getChildren().add(new TreeItem<>("Foo"));
            root.getChildren().add(new TreeItem<>("Bar"));
            root.getChildren().add(new TreeItem<>("Baz"));

            VBox layout = new VBox(treeView);
            scene = new Scene(layout, SCENE_WIDTH, SCENE_HEIGHT);

            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN,
                    e -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
