/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/*
 * Test to verify treeTableView resizeColumnToFitContent with
 * column resize policy set to CONSTRAINED_RESIZE_POLICY.
 */
public class TreeTableViewResizeColumnToFitContentTest {

    static Robot robot;
    static TreeTableView<Person> treeTableView;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 450;
    static final int SCENE_HEIGHT = 100;
    static CountDownLatch startupLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        TreeTableViewResizeColumnToFitContentTest test =
                new TreeTableViewResizeColumnToFitContentTest();
        test.resizeColumnToFitContentTest();
    }

    @Test
    public void resizeColumnToFitContentTest() {
        double colOneWidth = treeTableView.getColumns().get(0).getWidth();
        double colTwoWidth = treeTableView.getColumns().get(1).getWidth();
        double colThreeWidth = treeTableView.getColumns().get(2).getWidth();
        double colsWidthBeforeResize = colOneWidth + colTwoWidth + colThreeWidth;
        double colHeaderHeight = 25;
        double posX = scene.getWindow().getX() + treeTableView.getLayoutX() +
                colOneWidth + colTwoWidth;
        double posY = scene.getWindow().getY() + treeTableView.getLayoutY() +
                colHeaderHeight / 2;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            robot.mouseMove((int) posX, (int) posY);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
            latch.countDown();
        });
        Util.waitForLatch(latch, 5, "Timeout while waiting for mouse double click");
        try {
            Thread.sleep(1000); // Delay for table resizing of table columns.
        } catch (Exception e) {
            fail("Thread was interrupted." + e);
        }
        Assert.assertTrue("resizeColumnToFitContent failed",
                (colTwoWidth != treeTableView.getColumns().get(1).getWidth()));
        colTwoWidth = treeTableView.getColumns().get(1).getWidth();
        colThreeWidth = treeTableView.getColumns().get(2).getWidth();
        double colsWidthAfterResize = colOneWidth + colTwoWidth + colThreeWidth;
        Assert.assertEquals("TreeTableView.CONSTRAINED_RESIZE_POLICY ignored.",
                colsWidthBeforeResize, colsWidthAfterResize, 0);
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

            treeTableView = new TreeTableView<>();
            TreeTableColumn<Person, String> firstNameCol
                    = new TreeTableColumn<>("First Name");

            TreeTableColumn<Person, String> lastNameCol
                    = new TreeTableColumn<>("Last Name");

            TreeTableColumn<Person, String> descriptionCol
                    = new TreeTableColumn<>("Description");

            firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));
            descriptionCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("description"));
            lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

            treeTableView.getColumns().addAll(firstNameCol, descriptionCol, lastNameCol);
            treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

            Person person = new Person("John", "Currently wearing brown pants", "Doe" );
            TreeItem<Person> itemRoot = new TreeItem<Person>(person);
            treeTableView.setRoot(itemRoot);

            scene = new Scene(treeTableView, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    public static final class Person {

        private String firstName;
        private String description;
        private String lastName;

        public Person(String firstName, String description, String lastName) {
            this.firstName = firstName;
            this.description = description;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
