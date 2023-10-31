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
package test.robot.javafx.scene;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import test.util.Util;

public class DatePickerOnFocusLostTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static volatile Stage stage;
    static volatile Scene scene;
    static final int SCENE_WIDTH = 200;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    static VBox root;
    DatePicker datePicker;
    Button button; // a button to steal the focus

    int onChangeListenerCalled; // counter for onChangeListener callbacks

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    @Test
    public void testdatePickerCommitNoTypo() throws Exception {
        testdatePickerCommit(false);
    }

    @Test
    public void testdatePickerCannotCommitTypo() throws Exception {
        testdatePickerCommit(true);
    }

    // This test is for verifying a specific behavior.
    // 1. Click on datePicker
    // 2. Modify with keystrokes the day of month value, optionally with typo
    // 3. Click on button to grab the focus and hence attempt to datePicker.commitValue()
    // 4. Verify that in case of typo, the datePicker kept the focus, onChangeListener was not called
    //    and the property value remained unchanged.
    public void testdatePickerCommit(boolean typo) throws Exception {

        Thread.sleep(1000); // Wait for stage to layout
        onChangeListenerCalled = 0;

        // 1 datepicker gets focus
        mouseClick(datePicker.getLayoutX() + datePicker.getWidth() / 2,
                datePicker.getLayoutY() + datePicker.getHeight() / 2);

        // 2 type, maybe with typo
        Util.runAndWait(() -> {
            try {
                robot.keyType(KeyCode.BACK_SPACE);
                robot.keyType(KeyCode.BACK_SPACE);
                Thread.sleep(100);
                if (typo) {
                    robot.keyType(KeyCode.X);
                    robot.keyType(KeyCode.X);
                } else {
                    robot.keyType(KeyCode.NUMPAD1);
                    robot.keyType(KeyCode.NUMPAD5);

                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(100);

        // 3 button want focus
        mouseClick(button.getLayoutX() + button.getWidth() / 2, button.getLayoutY() + button.getHeight() / 2);
        Thread.sleep(100);

        // 4 check that the focus ends up on button only if there is no typo (fixed by patch for JDK-8303478)
        Assert.assertEquals(typo, datePicker.isFocused());
        Assert.assertEquals(!typo, button.isFocused());

        // 5 check that in case of typo, the value has not changed (already working as expected before the patch)
        Assert.assertEquals(typo, datePicker.valueProperty().get().equals(now));

        // 6 check if onChangeListener was called (already working as expected before the patch)
        if (typo)
            Assert.assertEquals(onChangeListenerCalled, 0);
        else
            Assert.assertEquals(onChangeListenerCalled, 1);

    }

    @After
    public void resetUI() {
        Util.runAndWait(() -> {
            root.getChildren().clear();
        });
    }

    LocalDate now = LocalDate.now();

    @Before
    public void setupUI() {
        Util.runAndWait(() -> {

            datePicker = new DatePicker(now);

            // set up a specific time format, coz it is not known what will be used when running the test
            datePicker.setConverter(
                    new StringConverter<>() {
                        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                        @Override
                        public String toString(LocalDate date) {
                            return (date != null) ? dateFormatter.format(date) : "";
                        }

                        @Override
                        public LocalDate fromString(String string) {
                            return (string != null && !string.isEmpty())
                                    ? LocalDate.parse(string, dateFormatter)
                                    : null;
                        }
                    });

            // Note that change listener was already beeing called as expected before the patch
            ChangeListener chListener = (observable, oldValue, newValue) -> {
                onChangeListenerCalled++;
            };
            datePicker.valueProperty().addListener(chListener);

            root.getChildren().add(datePicker);
            button = new Button("...");
            root.getChildren().add(button);

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
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

}
