/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.ContextMenuContentShim;
import com.sun.javafx.tk.Toolkit;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.skin.ChoiceBoxSkin;
import javafx.scene.control.skin.ChoiceBoxSkinNodesShim;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.application.Platform;
import javafx.scene.robot.Robot;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/*
 * Test for verifying if ChoiceBox items are displayed after
 * scrolling large number of items and replacing with 2 items.
 *
 * There is 1 test in this file.
 * Steps for testChoicBoxScrollOnCollectionChange()
 * 1. Create a ChoiceBox and add 150 items to it.
 * 2. Display the ChoiceBox and scroll down.
 * 3. Verify if both up and down scroll arrows are displayed.
 * 4. Replace the ChoiceBox items with 2 items.
 * 5. Display the ChoiceBox again.
 * 6. Verify that ChoiceBox items are displayed and no scroll
 *    arrows are displayed.
 */

public class ChoiceBoxScrollUpOnCollectionChangeTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch scrollLatch = new CountDownLatch(1);
    static CountDownLatch choiceBoxDisplayLatch = new CountDownLatch(1);
    static CountDownLatch choiceBoxHiddenLatch = new CountDownLatch(1);
    static Robot robot;
    static ChoiceBox<String> choiceBox;

    static volatile Stage stage;
    static volatile Scene scene;

    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;

    private void mouseClick(double x, double y) {
        int xCoordinate = (int) (scene.getWindow().getX() + scene.getX() + x);
        int yCoordinate = (int) (scene.getWindow().getY() + scene.getY() + y);
        Util.runAndWait(() -> {
            robot.mouseMove(xCoordinate, yCoordinate);
            robot.mouseClick(MouseButton.PRIMARY);
        });
    }

    private void scrollChoiceBox(int scrollAmt) throws Exception {
        Util.runAndWait(() -> {
            for (int i = 0; i < scrollAmt; i++) {
                robot.keyType(KeyCode.DOWN);
                Toolkit.getToolkit().firePulse();
            }
            scrollLatch.countDown();
        });

        Util.waitForLatch(scrollLatch, 5, "Timeout waiting for choicebox to be hidden.");
        Thread.sleep(400); // Wait for up arrow to get loaded in UI

        Util.runAndWait(() -> {
            robot.keyType(KeyCode.ENTER);
        });
    }

    private void showChoiceBox() throws Exception {
        double x = choiceBox.getLayoutX() + choiceBox.getWidth() / 2;
        double y = choiceBox.getLayoutY() + choiceBox.getHeight() / 2;
        mouseClick(x, y);

        Util.waitForLatch(choiceBoxDisplayLatch, 5, "Timeout waiting for choicebox to be displayed.");
    }

    private void addChoiceBoxItems(int count) {
        Util.runAndWait(() -> {
            ObservableList<String> items = FXCollections.observableArrayList();
            for (int i = 0; i < count; i++) {
                items.add("item " + (i + 1));
            }
            choiceBox.getItems().setAll(items);
        });
    }

    @Test
    public void testChoiceBoxScrollOnCollectionChange() throws Exception {
        Util.waitForLatch(startupLatch, 5, "Timeout waiting for stage to load.");
        ContextMenu popup = ChoiceBoxSkinNodesShim.getChoiceBoxPopup((ChoiceBoxSkin<?>) choiceBox.getSkin());

        addChoiceBoxItems(150);
        showChoiceBox();

        Thread.sleep(400); // Small delay to avoid test failure due to slow UI loading.

        Assert.assertFalse(ContextMenuContentShim.isContextMenuUpArrowVisible(popup));
        Assert.assertTrue(ContextMenuContentShim.isContextMenuDownArrowVisible(popup));

        double rowHeight = ContextMenuContentShim.getContextMenuRowHeight(popup);
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        scrollChoiceBox((int) Math.ceil(screenHeight / rowHeight));

        Util.waitForLatch(choiceBoxHiddenLatch, 5, "Timeout waiting for choicebox to be hidden.");
        Assert.assertTrue(ContextMenuContentShim.isContextMenuUpArrowVisible(popup));
        Assert.assertTrue(ContextMenuContentShim.isContextMenuDownArrowVisible(popup));

        addChoiceBoxItems(2);
        choiceBoxDisplayLatch = new CountDownLatch(1);
        showChoiceBox();

        Assert.assertFalse(ContextMenuContentShim.isContextMenuUpArrowVisible(popup));
        Assert.assertFalse(ContextMenuContentShim.isContextMenuDownArrowVisible(popup));
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

            choiceBox = new ChoiceBox<String>();
            choiceBox.setOnShown(event -> {
                choiceBoxDisplayLatch.countDown();
            });

            choiceBox.setOnHidden(event -> {
                choiceBoxHiddenLatch.countDown();
            });

            scene = new Scene(choiceBox, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
