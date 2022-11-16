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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

// When a key equivalent closes a window it can be passed
// to the new key window and processed twice.
public class DoubleShortcutProcessingTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch dialogLatch = new CountDownLatch(1);

    static volatile Stage stage;
    static volatile TestApp testApp;

    @Test
    void testDoubleShortcut() {
        Assumptions.assumeTrue(PlatformUtil.isMac());
        testApp.startTest();
        Util.waitForLatch(dialogLatch, 5, "Dialog never received shortcut");
        if (testApp.failed()) {
            Assertions.fail("performKeyEquivalent was handled twice in separate windows");
        }
    }

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void exit() {
        Util.shutdown(stage);
    }

    public static class TestApp extends Application {

        private boolean failure = false;
        private Dialog dialog = null;

        @Override
        public void start(Stage primaryStage) {

            testApp = this;
            stage = primaryStage;

            // Main window
            Label label = new Label("Testing double performKeyEquivalent");
            Scene scene = new Scene(new VBox(label), 200, 200);
            scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
                    failure = true;
                    event.consume();
                }
            });
            stage.setScene(scene);

            // Dialog
            VBox pane = new VBox(new Label("Pressing Cmd+Enter"));
            dialog = new Dialog(stage, pane);

            stage.setOnShown(e -> { startupLatch.countDown(); });
            stage.show();
        }

        public void startTest() {
            Platform.runLater(() -> {
                // Need to ensure Cmd is present so this is handled
                // as a key equivalent.
                dialog.setOnShown(e -> {
                    Robot robot = new Robot();
                    robot.keyPress(KeyCode.COMMAND);
                    robot.keyPress(KeyCode.ENTER);
                    robot.keyRelease(KeyCode.ENTER);
                    robot.keyRelease(KeyCode.COMMAND);
                });
                dialog.showAndWait();
                dialogLatch.countDown();
            });
        }

        public boolean failed() {
            return failure;
        }

        private static class Dialog extends Stage {

            public Dialog(Stage owner, Parent layout) {
                super(StageStyle.DECORATED);
                Scene layoutScene = new Scene(layout, 100, 100);
                this.setScene(layoutScene);

                layoutScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
                        close();
                        event.consume();
                    }
                });

                this.hide();
                this.initModality(Modality.APPLICATION_MODAL);
                this.initOwner(owner);
                this.setResizable(true);
            }
        }
    }
}
