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
package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class ComboBoxTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 200;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    static VBox root;
    int onShownCount = 0;
    int onSelectedCount = 0;
    final int ITEM_COUNT = 2;
    ComboBox comboBox;
    CountDownLatch showLatch;
    CountDownLatch selectedLatch;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }
    // This test is for verifying a specific behavior.
    // 1. Click ComboBox to show the popup list.
    // 2. Click a choice to select.
    // 3. Remove ComboBox and call ComboBox.show() and add it back to scene when a choice is selected.
    // 4. Verify that the ComboBox functions correctly after adding back.
    @Test
    public void testComboBoxSceneChange1() throws Exception {
        Thread.sleep(1000); // Wait for stage to layout
        comboBox.setOnShown(event -> {
            onShownCount++;
            showLatch.countDown();
        });
        ChangeListener chListener = (observable, oldValue, newValue) -> {
            onSelectedCount++;
            root.getChildren().clear();
            comboBox.show();
            // Called ITEM_COUNT times.
            root.getChildren().add(comboBox);
            selectedLatch.countDown();
        };
        comboBox.getSelectionModel().selectedItemProperty().addListener(chListener);
        showLatch = new CountDownLatch(1);

        // Show ComboBox popup list. popup list shown once by mouse click. (ITEM_COUNT + 1)
        mouseClick(comboBox.getLayoutX() + comboBox.getWidth() / 2,
                    comboBox.getLayoutY() + comboBox.getHeight() / 2);

        for (int i = 0; i < ITEM_COUNT; i++) {
            Thread.sleep(300); // ComboBox is removed and added back. Time for layout.
            Util.waitForLatch(showLatch, 10, "Failed to show ComboBox popup list. " + i);
            showLatch = new CountDownLatch(1);
            selectedLatch = new CountDownLatch(1);
            final int k = i;
            // Select a choice.
            mouseClick(comboBox.getLayoutX() + comboBox.getWidth() / 2,
                        comboBox.getLayoutY() + comboBox.getHeight() * (k + 1.2f));

            Util.waitForLatch(selectedLatch, 10, "Failed to select " + i + "th choice.");
        }
        Assert.assertEquals("ComboBox popup list should have been displayed " +
            (ITEM_COUNT + 1) + " times.", (ITEM_COUNT + 1), onShownCount);
        Assert.assertEquals("ComboBox choice should have been selected " +
            ITEM_COUNT + " times.", ITEM_COUNT, onSelectedCount);
    }

    // This test is for verifying a specific behavior.
    // 1. Click ComboBox to show the popup list.
    // 2. Click a choice to select.
    // 3. Remove ComboBox and add it back to scene when a choice is selected.
    // 4. Verify that the ComboBox functions correctly after adding back.
    @Test
    public void testComboBoxSceneChange2() throws Exception {
        Thread.sleep(1000); // Wait for stage to layout
        comboBox.setOnShown(event -> {
            onShownCount++;
            showLatch.countDown();
        });
        ChangeListener chListener = (observable, oldValue, newValue) -> {
            onSelectedCount++;
            root.getChildren().clear();
            root.getChildren().add(comboBox);
            selectedLatch.countDown();
        };
        comboBox.getSelectionModel().selectedItemProperty().addListener(chListener);
        for (int i = 0; i < ITEM_COUNT; i++) {
            Thread.sleep(300); // ComboBox is removed and added back. Time for layout.
            showLatch = new CountDownLatch(1);
            selectedLatch = new CountDownLatch(1);

            // Show ComboBox popup list.
            mouseClick(comboBox.getLayoutX() + comboBox.getWidth() / 2,
                    comboBox.getLayoutY() + comboBox.getHeight() / 2);

            Thread.sleep(200); // ComboBox takes some time to display the popup list.
            Util.waitForLatch(showLatch, 10, "Failed to show ComboBox popup list. " + i);
            final int k = i;
            // Select a choice.
            mouseClick(comboBox.getLayoutX() + comboBox.getWidth() / 2,
                        comboBox.getLayoutY() + comboBox.getHeight() * (k + 1.2f));

            Util.waitForLatch(selectedLatch, 10, "Failed to select " + i + "th choice.");
        }
        Assert.assertEquals("ComboBox popup list should be displayed " +
            ITEM_COUNT + " times.", ITEM_COUNT, onShownCount);
        Assert.assertEquals("ComboBox choice should have been selected " +
            ITEM_COUNT + " times.", ITEM_COUNT, onSelectedCount);
    }

    @After
    public void resetUI() {
        Util.runAndWait(() -> {
            root.getChildren().clear();
        });
    }

    @Before
    public void setupUI() {
        Util.runAndWait(() -> {
            comboBox = new ComboBox();
            for (int i = 0; i < ITEM_COUNT; i++) {
                comboBox.getItems().add("Op" + i);
            }
            root.getChildren().add(comboBox);
            onShownCount = 0;
            onSelectedCount = 0;
        });
    }

    @BeforeClass
    public static void initFX() throws Exception {
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
            root = new VBox();
            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
