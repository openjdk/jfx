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

package test.robot.javafx.scene.tableview;

import com.sun.javafx.PlatformUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TableViewContextMenuSortTest {

    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 800;
    static final int SCENE_HEIGHT = 250;
    static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final List<TableEntry> unsortedList = List.of(new TableEntry("One"), new TableEntry("Two"), new TableEntry("Three"), new TableEntry("Four"));

    private static TableView<TableEntry> table;

    public static void main(String[] args) {
        TableViewContextMenuSortTest test = new TableViewContextMenuSortTest();
        test.testContextMenuRequestDoesNotSort();
    }

    @Test
    public void testContextMenuRequestDoesNotSort() {
        assumeTrue(!PlatformUtil.isWindows()); // JDK-8364116

        Node header = table.lookupAll(".column-header").stream()
                .filter(Objects::nonNull)
                .filter(n -> n.getStyleClass().contains("table-column"))
                .findFirst()
                .orElseThrow();
        Bounds bounds = header.localToScreen(header.getLayoutBounds());
        double posX = bounds.getMinX() + 10;
        double posY = bounds.getMinY() + 5;

        AtomicInteger counter = new AtomicInteger();
        header.addEventFilter(MouseEvent.ANY, e -> {
            if (e.isPopupTrigger()) {
                counter.incrementAndGet();
            }
        });
        Util.runAndWait(() -> {
            robot.mouseMove((int) posX, (int) posY);
            robot.mousePress(MouseButton.SECONDARY);
            robot.mouseRelease(MouseButton.SECONDARY);
        });
        Util.sleep(1000);

        assertEquals(counter.get(), 1);
        for (int i = 0; i < 4; i++) {
            assertEquals(unsortedList.get(i).getName(), table.getItems().get(i).getName());
        }

        /*
        // Skipped due to JDK-8367566
        // This part of test is causing intermittent test failures on MacOS, see: JDK-8359154
        // This code should be re-enabled with a more robust approach.

        // macOS only: Ctrl + Left click also triggers the context menu
        if (PlatformUtil.isMac()) {
            Util.runAndWait(() -> {
                robot.keyPress(KeyCode.ESCAPE);
                robot.keyRelease(KeyCode.ESCAPE);
            });
            Util.sleep(100);

            Util.runAndWait(() -> {
                robot.keyPress(KeyCode.CONTROL);
                robot.mousePress(MouseButton.PRIMARY);
                robot.mouseRelease(MouseButton.PRIMARY);
                robot.keyRelease(KeyCode.CONTROL);
            });
            Util.sleep(1000);

            assertEquals(counter.get(), 2);
            for (int i = 0; i < 4; i++) {
                assertEquals(unsortedList.get(i).getName(), table.getItems().get(i).getName());
            }
        }
        */
    }

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            primaryStage.setTitle("TableView Test");

            TableColumn<TableEntry, String> col = new TableColumn<>("First Name");
            col.setSortable(true);
            col.setContextMenu(new ContextMenu(new MenuItem("Item")));

            table = new TableView<>();
            table.getColumns().addAll(col);
            table.getItems().addAll(unsortedList);
            col.setCellValueFactory(new PropertyValueFactory<>("name"));

            StackPane root = new StackPane();
            root.getChildren().add(table);
            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    public static class TableEntry {
        StringProperty name = new SimpleStringProperty();

        public TableEntry(String name) {
            this.name.set(name);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        @Override
        public String toString() {
            return "TableEntry [name=" + name + "]";
        }
    }
}
