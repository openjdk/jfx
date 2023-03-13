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
package test.robot.javafx.stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class SetSceneScalingTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;

    TestApp app;


    public abstract class TestApp {
        protected CountDownLatch shownLatch = new CountDownLatch(1);
        protected CountDownLatch buttonLatch;
        protected Stage stage;
        protected Button button;

        private final int WIDTH = 400;
        private final int HEIGHT = 400;

        protected void testButtonClick() {
            robot.mouseMove((int)(stage.getX() + stage.getScene().getX()) + (WIDTH / 2),
                            (int)(stage.getY() + stage.getScene().getY()) + (HEIGHT / 2));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        }

        protected Scene createTestScene() {
            buttonLatch = new CountDownLatch(1);

            button = new Button("I should be centered");
            button.setOnAction((ActionEvent e) -> buttonLatch.countDown());

            VBox box = new VBox(button);
            box.setAlignment(Pos.CENTER);
            return new Scene(box);
        }

        protected abstract void test() throws Exception;
        protected abstract void sceneShowSetup();

        public void runTest() throws Exception {
            start();

            Assert.assertNotNull(stage);
            Assert.assertNotNull(button);

            test();
        }

        public void start() {
            Util.runAndWait(() -> {
                stage = new Stage(StageStyle.UNDECORATED);
                stage.setOnShown(e -> Platform.runLater(() -> shownLatch.countDown()));
                stage.setWidth(WIDTH);
                stage.setHeight(HEIGHT);
                stage.setAlwaysOnTop(true);

                sceneShowSetup();
            });

            Util.waitForLatch(shownLatch, 5, "Stage not shown");
        }

        public void hideStage() {
            stage.hide();
        }
    }

    public class TestSetSceneShowApp extends TestApp {
        @Override
        protected void test() throws Exception {
            Platform.runLater(() -> testButtonClick());
            Assert.assertTrue(buttonLatch.await(3, TimeUnit.SECONDS));
        }

        @Override
        public void sceneShowSetup() {
            stage.setScene(createTestScene());
            stage.show();
        }
    }

    public class TestShowSetSceneApp extends TestApp {
        @Override
        protected void test() throws Exception {
            Platform.runLater(() -> testButtonClick());
            Assert.assertTrue(buttonLatch.await(3, TimeUnit.SECONDS));
        }

        @Override
        public void sceneShowSetup() {
            stage.show();
            stage.setScene(createTestScene());
        }
    }

    public class TestSecondSetSceneApp extends TestApp {
        @Override
        protected void test() throws Exception {
            // Test that everything is okay for start
            Platform.runLater(() -> testButtonClick());
            Assert.assertTrue(buttonLatch.await(3, TimeUnit.SECONDS));

            // Recreate scene and set it
            Util.runAndWait(() -> stage.setScene(createTestScene()));

            // retest - if DPI scaling is mishandled the button should
            // NOT be where it was (and thus, the test fails)
            Platform.runLater(() -> testButtonClick());
            Assert.assertTrue(buttonLatch.await(3, TimeUnit.SECONDS));
        }

        @Override
        public void sceneShowSetup() {
            stage.setScene(createTestScene());
            stage.show();
        }
    }


    @BeforeClass
    public static void initFX() {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);

        Util.runAndWait(() -> robot = new Robot());
    }

    @After
    public void cleanupTest() {
        if (app != null) {
            Platform.runLater(() -> app.hideStage());
        }
    }

    @AfterClass
    public static void teardownFX() {
        Util.shutdown();
    }


    @Test
    public void testSetSceneAndShow() throws Exception {
        app = new TestSetSceneShowApp();
        app.runTest();
    }

    @Test
    public void testShowAndSetScene() throws Exception {
        app = new TestShowSetSceneApp();
        app.runTest();
    }

    @Test
    public void testSecondSetScene() throws Exception {
        app = new TestSecondSetSceneApp();
        app.runTest();
    }
}
