/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class WrongStageFocusWithApplicationModalityTest {
    private static Robot robot;
    private static Stage stage;
    private static Alert alert;
    private static CountDownLatch startupLatch = new CountDownLatch(4);
    private static CountDownLatch alertCloseLatch = new CountDownLatch(3);

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    @Test(timeout = 25000)
    public void testWindowFocusByClosingAlerts() throws Exception {
        Thread.sleep(3000);
        mouseClick();
        Thread.sleep(1000);
        keyPress(KeyCode.ESCAPE);
        Thread.sleep(500);
        keyPress(KeyCode.ESCAPE);
        Thread.sleep(500);
        keyPress(KeyCode.ESCAPE);
        Thread.sleep(500);
        Util.waitForLatch(alertCloseLatch, 10, "Alerts not closed, wrong focus");
    }

    private static void keyPress(KeyCode code) throws Exception {
        Util.runAndWait(() -> {
            robot.keyPress(code);
            robot.keyRelease(code);
        });
    }

    private static void mouseClick() {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (alert.getX() + alert.getWidth() / 2),
                    (int) (alert.getY() + alert.getHeight() / 2));
            Util.sleep(100);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            BorderPane root = new BorderPane();
            stage.setScene(new Scene(root, 500, 500));
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();

            showAlert("Alert 1");
            showAlert("Alert 2");
            alert = showAlert("Alert 3");
        }

        private Alert showAlert(String title) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.setTitle(title);
            alert.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            alert.setOnHidden(event -> Platform.runLater(alertCloseLatch::countDown));
            alert.show();
            return alert;
        }
    }
}
