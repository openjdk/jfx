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
        protected Stage stage;
        protected Button button;
        protected boolean wasClicked = false;

        protected void testButtonClick() {
            robot.mouseMove(400, 400);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        }

        protected Scene createTestScene() {
            button = new Button("I should be centered");
            button.setOnAction((ActionEvent e) -> wasClicked = true);

            VBox box = new VBox(button);
            box.setAlignment(Pos.CENTER);
            return new Scene(box);
        }

        protected abstract void test();
        protected abstract void sceneShowSetup();

        public void runTest() {
            start();

            Assert.assertNotNull(stage);
            Assert.assertNotNull(button);

            test();
        }

        public void start() {
            Util.runAndWait(() -> {
                stage = new Stage(StageStyle.UNDECORATED);
                stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                                        Platform.runLater(shownLatch::countDown));
                stage.setX(200);
                stage.setY(200);
                stage.setWidth(400);
                stage.setHeight(400);
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
        protected void test() {
            wasClicked = false;
            Util.runAndWait(() -> testButtonClick());
            Assert.assertTrue(wasClicked);
        }

        @Override
        public void sceneShowSetup() {
            stage.setScene(createTestScene());
            stage.show();
        }
    }

    public class TestShowSetSceneApp extends TestApp {
        @Override
        protected void test() {
            wasClicked = false;
            Util.runAndWait(() -> testButtonClick());
            Assert.assertTrue(wasClicked);
        }

        @Override
        public void sceneShowSetup() {
            stage.show();
            stage.setScene(createTestScene());
        }
    }

    public class TestSecondSetSceneApp extends TestApp {
        @Override
        protected void test() {
            // Test that everything is okay for start
            wasClicked = false;
            Util.runAndWait(() -> testButtonClick());
            Assert.assertTrue(wasClicked);

            // Recreate scene and set it
            Util.runAndWait(() -> stage.setScene(createTestScene()));

            // retest - if DPI scaling is mishandled the button should
            // NOT be where it was (and thus, the test fails)
            wasClicked = false;
            Util.runAndWait(() -> testButtonClick());
            Assert.assertTrue(wasClicked);
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
