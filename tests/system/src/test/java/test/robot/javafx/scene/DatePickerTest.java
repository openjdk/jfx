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

import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
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

import com.sun.javafx.PlatformUtil;

import test.util.Util;

public class DatePickerTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    static VBox root;
    int onShownCount = 0;
    int onActionCount = 0;
    DatePicker datePicker;
    CountDownLatch onShownLatch;
    CountDownLatch onActionLatch;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void showDatePickerCalendarPopup() throws Exception {
        onShownLatch = new CountDownLatch(1);
        mouseClick(datePicker.getLayoutX() + datePicker.getWidth() - 15,
                    datePicker.getLayoutY() + datePicker.getHeight() / 2);
        Thread.sleep(400); // DatePicker takes some time to display the calendar popup.
        Util.waitForLatch(onShownLatch, 10, "Failed to show Calendar popup.");
    }

    private void clickDatePickerCalendarPopup(int yFactor) throws Exception {
        onActionLatch = new CountDownLatch(1);
        mouseClick(datePicker.getLayoutX() + datePicker.getWidth() / 2,
                    datePicker.getLayoutY() + datePicker.getHeight() * yFactor);
        Thread.sleep(400);
        Util.waitForLatch(onActionLatch, 10, "Failed to receive onAction call.");
    }

    // This test is for verifying a specific behavior.
    // 1. Remove DatePicker, call DatePicker.show() & add back DatePicker.
    // 2. Verify that the DatePicker calendar popup is shown using onShown,
    // 2.1 Confirm that calendar popup is shown, using listener onAction.
    // 3. Click on DatePicker, calendar popup should get shown.
    // 4. Repeat step 2.
    // 4.1 Repeat step 2.1
    // 5. Click on DatePicker, calendar popup should get shown.
    // 6. Remove & add back DatePicker, calendar popup should get shown.
    // 7. Confirm that calendar popup is shown, using listener onAction.

    @Test
    public void testDatePickerSceneChange() throws Exception {
        // Disable on mac untill JDK-8208523 is fixed.
        assumeTrue(!PlatformUtil.isMac());
        Thread.sleep(1000); // Wait for stage to layout

        // 1.
        onShownLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            root.getChildren().clear();
            datePicker.show();
            root.getChildren().add(datePicker);
        });
        Util.waitForLatch(onShownLatch, 10, "Failed to show calendar popup.");
        Thread.sleep(400); // DatePicker takes some time to display the calendar popup.
        // 2.
        Assert.assertEquals("DatePicker calendar popup should be shown once.", 1, onShownCount);

        // 2.1
        clickDatePickerCalendarPopup(5);
        Assert.assertEquals("DatePicker calendar popup should be clicked once.", 1, onActionCount);

        // 3.
        showDatePickerCalendarPopup();
        // 4.
        Assert.assertEquals("DatePicker calendar popup should have been shown two times.", 2, onShownCount);

        // 4.1
        clickDatePickerCalendarPopup(6);
        Assert.assertEquals("DatePicker calendar popup have been clicked two times.", 2, onActionCount);

        // 5.
        showDatePickerCalendarPopup();
        Assert.assertEquals("DatePicker calendar popup should have been shown three times.", 3, onShownCount);

        // 6.
        Util.runAndWait(() -> {
            root.getChildren().clear();
            root.getChildren().add(datePicker);
        });
        Thread.sleep(400); // DatePicker takes some time to display the calendar popup.

        // 7.
        clickDatePickerCalendarPopup(5);
        Assert.assertEquals("DatePicker calendar popup should have been clicked three times.", 3, onActionCount);
    }

    @After
    public void resetUI() {
        Platform.runLater(() -> {
            datePicker.setOnShown(null);
            datePicker.setOnAction(null);
            root.getChildren().clear();
        });
    }

    @Before
    public void setupUI() {
        Platform.runLater(() -> {
            datePicker = new DatePicker();
            datePicker.setOnShown(event -> {
                onShownCount++;
                onShownLatch.countDown();
            });
            datePicker.setOnAction(event -> {
                onActionCount++;
                onActionLatch.countDown();
            });
            root.getChildren().add(datePicker);
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
