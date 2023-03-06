/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import test.util.Util;

/*
 * [openjdk/jfx] 8173321: Click on trough has no effect when cell height > viewport (PR #985):
 * Test to verify
 * - click on trough moves the Scrollbar even if the table cell's height is greater than the table's height
 * - size of ScrollBar is greater than minimum size even if the table cell's height is greater than the table's height
 */
public class TableViewClickOnTroughTest {

    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 800;
    static final int SCENE_HEIGHT = 250;
    static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static TableView<TableEntry> table;

    public static void main(String[] args) {
        TableViewClickOnTroughTest test = new TableViewClickOnTroughTest();
        test.moveTroughTest();
    }

    @Test
    public void moveTroughTest() {
        ScrollBar verticalBar = (ScrollBar) table.lookup(".scroll-bar:vertical");
        assertNotNull(verticalBar);
        StackPane thumb = (StackPane) verticalBar.getChildrenUnmodifiable().stream()
                .filter(c -> c.getStyleClass().contains("thumb")).findFirst().orElse(null);
        assertNotNull(thumb);
        Bounds verticalBarBoundsInScreen = verticalBar.localToScreen(verticalBar.getBoundsInLocal());
        Bounds thumbBoundsInScreen = thumb.localToScreen(thumb.getBoundsInLocal());
        double posX = verticalBarBoundsInScreen.getCenterX();
        // set posY to point into the middle of the area of the verticalBar under the
        // thumb.
        double posY = verticalBarBoundsInScreen.getMaxY()
                - ((verticalBarBoundsInScreen.getMaxY() - thumbBoundsInScreen.getMaxY()) / 2.0);
        double oldPosition = verticalBar.getValue();

        Util.runAndWait(() -> {
            robot.mouseMove((int) posX, (int) posY);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        Util.sleep(1000); // Delay for table moving Scrollbar
        double newPosition = verticalBar.getValue();
        Assert.assertNotEquals(oldPosition, newPosition, 0.1);
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            primaryStage.setTitle("TableView Test");
            TableColumn<TableEntry, String> col = new TableColumn<>("First Name");

            table = new TableView<TableEntry>();
            table.getColumns().addAll(col);
            table.getItems().addAll(new TableEntry("First"), new TableEntry("Second"), new TableEntry("Third"));
            col.setCellValueFactory(new PropertyValueFactory<TableEntry, String>("name"));

            col.setCellFactory(MyCellFactory.forTableColumn());

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

    public static class MyCellFactory extends TableCell<TableEntry, String> {
        public MyCellFactory() {
            setFont(new Font("Times New Roman", 192));
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            setText(item);
        }

        public static Callback<TableColumn<TableEntry, String>, TableCell<TableEntry, String>> forTableColumn() {
            return e -> new MyCellFactory();
        }
    }
}
