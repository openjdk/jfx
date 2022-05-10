/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static org.junit.Assert.*;

public class DualWindowTest {

    static final double STAGE_SIZE = 200;

    static final double STAGE1_X = 100;
    static final double STAGE1_Y = 100;

    static final double STAGE2_X = 400;
    static final double STAGE2_Y = 120;

    static CountDownLatch startupLatch;
    static Stage stage1;
    static Stage stage2;
    static TestButton button1;
    static TestButton button2;

    Robot robot;

    static class TestButton extends Button {
        CountDownLatch latch;

        TestButton(String text) {
            super(text);
            this.setOnAction(e -> latch.countDown());
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage stage1) {
            Platform.setImplicitExit(false);

            DualWindowTest.stage1 = stage1;
            TestButton button1 = new TestButton("Button 1");
            DualWindowTest.button1 = button1;
            StackPane root1 = new StackPane(button1);
            stage1.setScene(new Scene(root1, STAGE_SIZE, STAGE_SIZE));
            stage1.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> {
                    stage1.setX(STAGE1_X);
                    stage1.setY(STAGE1_Y);
                    startupLatch.countDown();
                });
            });
            stage1.show();

            Stage stage2 = new Stage();
            DualWindowTest.stage2 = stage2;
            TestButton button2 = new TestButton("Button 2");
            DualWindowTest.button2 = button2;
            StackPane root2 = new StackPane(button2);
            stage2.setScene(new Scene(root2, STAGE_SIZE, STAGE_SIZE));
            stage2.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> {
                    stage2.setX(STAGE2_X);
                    stage2.setY(STAGE2_Y);
                    startupLatch.countDown();
                });
            });
            stage2.show();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        startupLatch = new CountDownLatch(2);
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        assertTrue("Timeout waiting for FX runtime to start",
                startupLatch.await(15, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void teardown() {
        Platform.runLater(() -> {
            if (stage1 != null) stage1.hide();
            if (stage2 != null) stage2.hide();
            Platform.exit();
        });
    }

    @Before
    public void setup() {
        Util.runAndWait(() -> robot = new Robot());
    }

    private void mouseClick(Scene scene, double x, double y) {
        Util.sleep(200);
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
        });
        Util.sleep(200);
        Util.runAndWait(() -> {
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    void clickButton(TestButton button) throws Exception {
        button.latch = new CountDownLatch(1);
        mouseClick(button.getScene(),
                   button.getLayoutX() + button.getWidth() / 2,
                   button.getLayoutY() + button.getHeight() / 2);
        assertTrue(button.getText() + " action not fired",
                   button.latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testTwoStages() throws Exception {
        Util.sleep(1000);
        Util.runAndWait(() -> {
            assertEquals(STAGE1_X, stage1.getX(), 1.0);
            assertEquals(STAGE1_Y, stage1.getY(), 1.0);
            assertEquals(STAGE2_X, stage2.getX(), 1.0);
            assertEquals(STAGE2_Y, stage2.getY(), 1.0);
        });
        clickButton(button1);
        clickButton(button2);
    }

}
