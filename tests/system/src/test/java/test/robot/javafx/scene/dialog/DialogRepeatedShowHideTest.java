/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene.dialog;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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

//see JDK8193502
public class DialogRepeatedShowHideTest {
    static final double DIALOG_WIDTH = 300.0d;
    static final double DIALOG_HEIGHT = DIALOG_WIDTH;
    static Robot robot;
    static Button button;
    static Stage stage;
    static Scene scene;
    static Dialog<Void> dialog;
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch dialogShownLatch;
    static CountDownLatch dialogHideLatch;

    @Test(timeout = 15000)
    public void dialogSizeOnReShownTest() throws Exception {
        Thread.sleep(400);
        clickButton();
        hide();
        clickButton();
        Assert.assertEquals("Dialog width should remain the same", DIALOG_WIDTH, dialog.getDialogPane().getWidth(),
                0.0);
        Assert.assertEquals("Dialog height should remain the same", DIALOG_HEIGHT, dialog.getDialogPane().getHeight(),
                0.0);
        hide();
    }

    private void clickButton() throws Exception {
        dialogShownLatch = new CountDownLatch(1);
        mouseClick(button.getLayoutX() + button.getWidth() / 2, button.getLayoutY() + button.getHeight() / 2);

        Thread.sleep(400);
        Util.waitForLatch(dialogShownLatch, 10, "Failed to show Dialog");
    }

    private void hide() throws Exception {
        dialogHideLatch = new CountDownLatch(1);
        Platform.runLater(() -> dialog.close());
        Thread.sleep(600);
        Util.waitForLatch(dialogHideLatch, 10, "Failed to hide Dialog");
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            dialog = getTestDialog();
            button = new Button("Open Dialog");
            button.setOnAction(evt -> dialog.show());

            scene = new Scene(button, 200, 200);
            stage.setScene(scene);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));

            stage.show();
        }

        private static Dialog<Void> getTestDialog() {
            final Label dialogContent = new Label();
            dialogContent.setText("Dialog content");

            final Dialog<Void> testDialog = new Dialog<>();

            testDialog.getDialogPane().setContent(dialogContent);
            testDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            testDialog.getDialogPane().setPrefSize(DIALOG_WIDTH, DIALOG_HEIGHT);

            testDialog.getDialogPane().getScene().getWindow().addEventHandler(WindowEvent.WINDOW_SHOWN,
                    e -> Platform.runLater(dialogShownLatch::countDown));

            testDialog.getDialogPane().getScene().getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN,
                    e -> Platform.runLater(dialogHideLatch::countDown));

            return testDialog;
        }
    }
}
