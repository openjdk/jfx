/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.testharness;

import static org.junit.jupiter.api.Assertions.fail;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import test.util.Util;

/**
 * Base class for robot-based tests which creates a stage with the the BorderPane content
 * to be used by individual tests.
 */
public class RobotTestBase {
    protected static final int STAGE_WIDTH = 400;
    protected static final int STAGE_HEIGHT = 300;
    private static CountDownLatch startupLatch;
    /** Scene valid only during test */
    protected static Scene scene;
    /** Stage valid only during test */
    protected static Stage stage;
    protected static BorderPane contentPane;
    /** The Robot instance */
    protected static Robot robot;

    public static class App extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            robot = new Robot();
            contentPane = new BorderPane();
            contentPane.setPrefWidth(STAGE_WIDTH);
            contentPane.setPrefHeight(STAGE_HEIGHT);
            scene = new Scene(contentPane);

            stage = primaryStage;
            stage.setScene(scene);
            stage.setOnShown(l -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    @BeforeAll
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        Util.launch(startupLatch, App.class);
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    /**
     * Triggers and waits for 10 pulses to complete in this test's scene.
     */
    protected void waitForIdle() {
        Util.waitForIdle(scene);
    }

    /**
     * Performs the mouse click with the {@code MouseButton.PRIMARY} via Robot.
     * Must be called from the FX Application thread.
     */
    protected void mouseClick() {
        mouseClick(MouseButton.PRIMARY);
    }

    /**
     * Performs the mouse click with the specified button via Robot.
     * Must be called from the FX Application thread.
     * @param b the button
     */
    protected void mouseClick(MouseButton b) {
        robot.mouseClick(b);
    }

    /**
     * Performs the mouse press with the {@code MouseButton.PRIMARY} via Robot.
     * Must be called from the FX Application thread.
     */
    protected void mousePress() {
        mousePress(MouseButton.PRIMARY);
    }

    /**
     * Performs the mouse press with the specified button via Robot.
     * Must be called from the FX Application thread.
     * @param b the button
     */
    protected void mousePress(MouseButton b) {
        robot.mousePress(b);
    }

    /**
     * Performs the mouse release with the {@code MouseButton.PRIMARY} via Robot.
     * Must be called from the FX Application thread.
     */
    protected void mouseRelease() {
        mouseRelease(MouseButton.PRIMARY);
    }

    /**
     * Performs the mouse release with the specified button via Robot.
     * Must be called from the FX Application thread.
     * @param b the button
     */
    protected void mouseRelease(MouseButton b) {
        robot.mouseRelease(b);
    }

    // debugging aid
    protected void p(Object v) {
        System.out.println(v);
    }

    /**
     * Sets the center Node inside the main Scene's BorderPane,
     * waits for idle.
     * @param n the node
     */
    public void setContent(Node n) {
        runAndWait(() -> {
            contentPane.setCenter(n);
        });
        waitForIdle();
    }

    /**
     * Sets the main window title.
     * @param title the title
     */
    public void setTitle(String title) {
        runAndWait(() -> {
            stage.setTitle(title);
        });
    }

    /**
     * Executes code in the FX Application thread.
     * @param r the code to execute
     */
    public void runAndWait(Runnable r) {
        Util.runAndWait(r);
    }

    /**
     * Thread.sleep() alias that fails the test if an {@code InterruptedException} gets thrown.
     * @param milliseconds the number of milliseconds to sleep
     */
    protected void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            fail(e);
        }
    }
}
