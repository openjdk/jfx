/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/*
 * Test to verify the events when scene of stage is changed.
 * Steps of Test:
 * 1. Create a stage with a scene with a button.
 * 2. Add MOUSE_EXITED listener & WindowProperty Listener to the same scene.
 * 3. In onAction of the button change stage's scene to a new scene.
 * 4. Verify that MOUSE_EXITED & WindowPropery change listener are called in
 *    sequence.
 */

public class SceneChangeEventsTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static volatile Stage stage;
    boolean mouseExited = false;
    boolean mouseWindowEventOrder = false;
    boolean windowChanged = false;

    public static void main(String[] args) {
        SceneChangeEventsTest test = new SceneChangeEventsTest();
        test.testSceneChange();
        exit();
    }

    @Test
    public void testSceneChange() {

        Button button = new Button("onAction");
        CountDownLatch onActionLatch = new CountDownLatch(1);
        button.setOnAction(event -> {
            stage.setScene(new Scene(new HBox()));
            onActionLatch.countDown();
        });
        HBox root = new HBox();
        root.getChildren().add(button);
        Scene scene = new Scene(root);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        stage.sceneProperty().addListener(observable -> setSceneLatch.countDown());
        Platform.runLater(() -> {
            stage.setScene(scene);
        });
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");

        scene.setOnMouseExited(event -> {
            mouseExited = true;
        });
        scene.windowProperty().addListener(observable -> {
            mouseWindowEventOrder = mouseExited;
            windowChanged = true;
        });
        Platform.runLater(() -> {
            robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + button.getLayoutX() + button.getLayoutBounds().getWidth() / 2),
                    (int)(scene.getWindow().getY() + scene.getY() +  button.getLayoutY() + button.getLayoutBounds().getHeight() / 2));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        Util.waitForLatch(onActionLatch, 5, "Timeout while waiting for button.onAction().");

        Assert.assertTrue("MOUSE_EXITED should be received when scene is " +
            " changed.", mouseExited);
        Assert.assertTrue("scene.windowProperty() listener should be received" +
            "on scene change.", windowChanged);
        Assert.assertTrue("MOUSE_EXITED should have been received before " +
            "scene.windowProperty().", mouseWindowEventOrder);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }
}
