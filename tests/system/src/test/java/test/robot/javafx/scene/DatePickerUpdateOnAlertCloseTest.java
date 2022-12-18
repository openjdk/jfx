/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.time.LocalDate;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.input.MouseButton;
import javafx.application.Platform;
import javafx.scene.robot.Robot;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/*
 * Test for verifying DatePicker update on closing the Alert dialog.
 *
 * There is 1 test in this file.
 * Steps for testDatePickerUpdateOnAlertClose()
 * 1. Create a alert dialog and add date picker to it.
 * 2. Add button to scene and show alert dialog on button click.
 * 3. Click on date picker and select a date from popup.
 * 4. Verify that selected date is updated in the date picker.
 */
public class DatePickerUpdateOnAlertCloseTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch onDatePickerShownLatch = new CountDownLatch(1);
    static CountDownLatch onAlertShownLatch = new CountDownLatch(1);
    static Robot robot;

    static volatile Stage stage;
    static volatile Scene scene;
    static Button button;

    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    static final int Y_FACTOR = 5;

    DatePicker datePicker;
    Alert dialog;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                                (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void mouseClickOnAlertDialog(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (dialog.getX() + x), (int) (dialog.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void selectDatePicker() throws Exception {
        mouseClickOnAlertDialog(datePicker.getLayoutX() + datePicker.getWidth() - 15,
                                    datePicker.getLayoutY() + datePicker.getHeight() / 2);
        Thread.sleep(400); // Wait for DatePicker popup to display.
        Util.waitForLatch(onDatePickerShownLatch, 5, "Failed to show DatePicker popup.");
    }

    private void showAlertDialog() throws Exception {
        mouseClick(button.getLayoutX() + button.getWidth() / 2,
                    button.getLayoutY() + button.getHeight() / 2);
        Thread.sleep(400); // Wait for Alert dialog to display.
        Util.waitForLatch(onAlertShownLatch, 5, "Failed to show Alert dialog.");
    }

    @Test
    public void testDatePickerUpdateOnAlertClose() throws Exception {
        Thread.sleep(1000); // Wait for stage to layout

        showAlertDialog();
        selectDatePicker();

        // Select date from date picker popup
        mouseClick(datePicker.getLayoutX() + datePicker.getWidth() / 2,
                    datePicker.getLayoutY() + datePicker.getHeight() * Y_FACTOR);
        Thread.sleep(400); // Wait for date to be selected.

        Assert.assertFalse(LocalDate.now().isEqual(datePicker.getValue()));
    }

    @After
    public void resetUI() {
        Util.runAndWait(() -> {
            datePicker.setOnShown(null);
            datePicker.setOnAction(null);
        });
    }

    @Before
    public void setupUI() {
        Util.runAndWait(() -> {
            datePicker = new DatePicker(LocalDate.now());
            datePicker.setOnShown(event -> {
                onDatePickerShownLatch.countDown();
            });
            datePicker.valueProperty().addListener(event -> {
                dialog.close();
            });

            dialog = new Alert(AlertType.INFORMATION);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setOnShown(event -> {
                onAlertShownLatch.countDown();
            });

            button.setOnAction(event -> {
                dialog.initOwner(stage);
                dialog.getDialogPane().setContent(datePicker);
                dialog.show();
            });
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

            button = new Button("Show dialog");
            scene = new Scene(button, SCENE_WIDTH, SCENE_HEIGHT);

            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
