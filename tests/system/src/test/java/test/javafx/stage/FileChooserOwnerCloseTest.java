/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests, whether closing the owner actually also closes the FileChooser.
 */
@EnabledOnOs(OS.MAC)
class FileChooserOwnerCloseTest {

    Stage mainStage = null;
    Stage subStage = null;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void teardown() {
        Util.shutdown();
    }

    @Test
    public void closingOwnerClosesFilechooser() throws Exception {

        Util.runAndWait(() -> {
            mainStage = new Stage();
            mainStage.setScene(new Scene(new VBox(new Label("Main Stage"), new TextField())));
            mainStage.show();
        });
        waitForStageFocus(mainStage);

        Util.runAndWait(() -> {
            subStage = new Stage();
            subStage.initOwner(mainStage);
            subStage.setScene(new Scene(new Label("Sub Stage")));
            subStage.show();
        });
        waitForStageFocus(subStage);

        CountDownLatch dialogReturned = new CountDownLatch(1);
        Platform.runLater(() -> {
            new FileChooser().showOpenDialog(subStage);
            dialogReturned.countDown();
        });

        // let the dialog open, then close its owner window
        Thread.sleep(2000);
        Platform.runLater(subStage::hide);

        assertTrue(dialogReturned.await(10, TimeUnit.SECONDS),
                "FileChooser.showOpenDialog did not return after its owner window was closed");

        // If this doesn't happen - we land in a state
        // where the main-stage can no longer be focused at all.
        // Refocusing on OS then only results in a beep sounds without receiving focus.
        waitForStageFocus(mainStage);

        Util.runAndWait(mainStage::hide);
    }

    public static void waitForStageFocus(Stage stage) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            stage.focusedProperty().subscribe(focused -> {
                if(focused) {
                    latch.countDown();
                }
            });
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS),
                "Stage never became focuse");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            Platform.setImplicitExit(false);
            startupLatch.countDown();
        }
    }
}