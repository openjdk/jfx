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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TextArea;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import test.util.Util;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class KeyEventClosesStageTest {
    private static Robot robot;
    private static Stage stage;
    private static Scene scene;
    private static BorderPane borderPane;
    private static TextArea textArea;

    private static boolean typedEventArrived = false;
    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static CountDownLatch pressedEventLatch = new CountDownLatch(1);

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);

        // When run from the command line Windows does not want to
        // activate the window.
        Util.runAndWait(() -> {
            int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                    textArea.getLayoutX() + textArea.getLayoutBounds().getWidth() / 2);
            int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                    textArea.getLayoutY() + textArea.getLayoutBounds().getHeight() / 2);
            robot.mouseMove(mouseX, mouseY);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.runAndWait(() -> {
            borderPane.requestFocus();
        });
    }

    @Test
    @Order(1)
    public void pressedEventClosesStage() throws Exception {
        Util.runAndWait(() -> {
            robot.keyPress(KeyCode.ESCAPE);
            robot.keyRelease(KeyCode.ESCAPE);
        });
        assertTrue(pressedEventLatch.await(1000, TimeUnit.MILLISECONDS),
            "Pressed event arrived without crash");
    }

    @Test
    @Order(2)
    public void typedEventNeverArrives() throws Exception {
        assertFalse(typedEventArrived, "Unexpected typed event arrived");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            textArea = new TextArea();
            borderPane = new BorderPane(textArea);
            borderPane.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    stage.close();
                    Platform.runLater(pressedEventLatch::countDown);
                }
            });
            borderPane.setOnKeyTyped(e -> {
                typedEventArrived = true;
            });

            scene = new Scene(borderPane, 200, 200);
            stage.setScene(scene);
            stage.setOnShown(event -> {
                Platform.runLater(startupLatch::countDown);
            });
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
