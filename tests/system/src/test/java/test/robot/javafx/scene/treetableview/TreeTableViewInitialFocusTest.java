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

package test.robot.javafx.scene.treetableview;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.FocusModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TreeTableViewInitialFocusTest {

    private static final int SCENE_WIDTH = 600;
    private static final int SCENE_HEIGHT = 500;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    static volatile TreeTableView<Object> treeTableView;

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    @Test
    public void testInitialFocusClearedWhenHiddenRootChildrenAreReplaced() {
        Util.sleep(300);

        AtomicInteger focusedIndex = new AtomicInteger(Integer.MIN_VALUE);
        AtomicReference<TreeItem<Object>> focusedItem = new AtomicReference<>();

        Util.runAndWait(() -> {
            FocusModel<TreeItem<Object>> fm = treeTableView.getFocusModel();
            focusedIndex.set(fm.getFocusedIndex());
            focusedItem.set(fm.getFocusedItem());
        });

        assertEquals(-1, focusedIndex.get(), "Focused index must be cleared");
        assertNull(focusedItem.get(), "Focused item must be null");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            treeTableView = new TreeTableView<>();
            TreeItem<Object> root = new TreeItem<>("Root");
            treeTableView.setRoot(root);
            treeTableView.setShowRoot(false);

            TreeTableColumn<Object, String> c1 = new TreeTableColumn<>("C1");
            c1.setCellValueFactory(param ->
                    new SimpleStringProperty(String.valueOf(param.getValue().getValue())));
            c1.setPrefWidth(300);

            root.getChildren().add(new TreeItem<>("Foo"));
            root.getChildren().add(new TreeItem<>("Bar"));
            root.getChildren().add(new TreeItem<>("Baz"));

            Scene scene = new Scene(new StackPane(treeTableView), SCENE_WIDTH, SCENE_HEIGHT);

            primaryStage.setScene(scene);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            primaryStage.show();
        }
    }
}
