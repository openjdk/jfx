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

//see JDK-8193502
public class DialogWithOwnerSizingTest {
    static Robot robot;
    static Button button;
    static Stage stage;
    static Scene scene;
    static Dialog<ButtonType> dialog;
    static Dialog<ButtonType> dialog2;
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch dialogShownLatch;
    static CountDownLatch dialogHideLatch;

    @Test(timeout = 15000)
    public void dialogWithOwnerSizingTest() throws Exception {
        Thread.sleep(500);
        clickButton();
        Thread.sleep(500);

        try {
            Assert.assertEquals(dialog2.getDialogPane().getWidth(), dialog.getDialogPane().getWidth(), 2.0);
            Assert.assertEquals(dialog2.getDialogPane().getHeight(), dialog.getDialogPane().getHeight(), 2.0);
        } finally {
            hide();
        }
    }

    private void clickButton() throws Exception {
        dialogShownLatch = new CountDownLatch(2);
        mouseClick(button.getLayoutX() + button.getWidth() / 2, button.getLayoutY() + button.getHeight() / 2);

        Util.waitForLatch(dialogShownLatch, 10, "Failed to show Dialog");
    }

    private void hide() throws Exception {
        dialogHideLatch = new CountDownLatch(2);
        Platform.runLater(() -> {
            dialog.close();
            dialog2.close();
        });
        Util.waitForLatch(dialogHideLatch, 10, "Failed to hide Dialog");
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, SizingTestApp.class);
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

    public static class SizingTestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;
            stage.setAlwaysOnTop(true);

            button = new Button("Open Dialogs");

            scene = new Scene(button, 200, 200);
            stage.setScene(scene);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(e -> Platform.runLater(startupLatch::countDown));

            dialog = getTestDialog(true);
            dialog2 = getTestDialog(false);

            button.setOnAction(evt -> {
                dialog.show();
                dialog2.show();
            });

            stage.show();
        }

        private static Dialog<ButtonType> getTestDialog(boolean withOwner) {
            final Dialog<ButtonType> testDialog = new Dialog<>();
            testDialog.setTitle("Multi-line Dialog");
            testDialog.setContentText("This\nis\na\ntest\nmulti\nline\nblah");
            testDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            testDialog.getDialogPane().getScene().getWindow().addEventHandler(WindowEvent.WINDOW_SHOWN,
                    e -> Platform.runLater(dialogShownLatch::countDown));

            testDialog.getDialogPane().getScene().getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN,
                    e -> Platform.runLater(dialogHideLatch::countDown));

            if (withOwner) {
                testDialog.initOwner(stage);
            }

            return testDialog;
        }
    }
}
