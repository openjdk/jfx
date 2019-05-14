/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.stage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// See JDK-8210973
public class FocusParentWindowOnChildCloseTest {
    static Robot robot;
    static Button button;
    static Stage stage;
    static Stage stage2;
    static Scene scene;
    static Alert alert;
    static CountDownLatch startupLatch = new CountDownLatch(2);
    static CountDownLatch alertShownLatch;
    static CountDownLatch alertCloseLatch;

    @Test(timeout = 15000)
    public void focusRightParentOnChildWindowClose() throws Exception {
        Thread.sleep(400);
        clickButton();
        fireOkInAlert();
        Assert.assertTrue("Stage 1 should be focused", stage.isFocused());
    }

    private void clickButton() throws Exception {
        alertShownLatch = new CountDownLatch(1);
        mouseClick(button.getLayoutX() + button.getWidth() / 2, button.getLayoutY() + button.getHeight() / 2);

        Thread.sleep(400);
        waitForLatch(alertShownLatch, 10, "Failed to show Alert");
    }

    @BeforeClass
    public static void initFX() throws Exception {
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        waitForLatch(startupLatch, 10, "FX runtime failed to start.");
    }

    @AfterClass
    public static void exit() {
        Platform.runLater(() -> {
            stage.hide();
            stage2.hide();
        });
        Platform.exit();
    }

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private static void fireOkInAlert() throws Exception {
        alertCloseLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
                    Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
                    okButton.fire();
                });
        Thread.sleep(400);
        waitForLatch(alertCloseLatch, 10, "Failed to close alert.");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            createWindow1(stage);
            createWindow2(stage);
        }

        private void createWindow1(Stage stage) {
            stage.setTitle("Stage - 1");
            button = new Button();
            button.setText("Show Alert");
            button.setOnAction(event -> showAlert(stage));

            StackPane root = new StackPane();
            root.getChildren().add(button);
            scene = new Scene(root, 300, 250);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }

        private void showAlert(Stage primaryStage) {
            alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setOnShown(event -> Platform.runLater(alertShownLatch::countDown));
            alert.setOnHidden(event -> Platform.runLater(alertCloseLatch::countDown));
            alert.show();
        }

        private void createWindow2(Stage stage1) {
            stage2 = new Stage();
            stage2.setX(stage1.getX() + 300);
            stage2.setY(stage1.getY());

            stage2.setTitle("Stage - 2");
            StackPane root = new StackPane();
            stage2.setScene(new Scene(root, 300, 250));
            stage2.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage2.show();
        }
    }

    public static void waitForLatch(CountDownLatch latch, int seconds, String msg) throws Exception {
        Assert.assertTrue("Timeout: " + msg, latch.await(seconds, TimeUnit.SECONDS));
    }
}
