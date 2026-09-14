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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DestroyWindowTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final CountDownLatch doneLatch = new CountDownLatch(1);
    private static volatile Throwable exception;
    private static volatile Stage stage;
    private static volatile Stage secondaryStage;

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void teardown() {
        Util.shutdown();
    }

    @Test
    public void closingOwnerWindowDoesNotThrowTest() throws Throwable {
        assertTrue(stage.isShowing());
        Util.waitForLatch(doneLatch, 10, "The test didn't finish");
        assertFalse(stage.isShowing());
        assertTrue(secondaryStage.isShowing());
        if (exception != null) {
            throw new AssertionError("Unexpected exception", exception);
        }
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) {
            Thread.currentThread().setUncaughtExceptionHandler((_, throwable) -> {
                exception = throwable;
                doneLatch.countDown();
            });

            stage = primaryStage;

            stage.setScene(new Scene(new StackPane(new Label("Main stage")), 300, 240));
            stage.setOnShown(_ -> startupLatch.countDown());
            stage.show();

            secondaryStage = new Stage();
            secondaryStage.setScene(new Scene(new StackPane(new Label("Secondary stage")), 240, 200));
            secondaryStage.show();

            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(new StackPane(new Label("Modal dialog")), 220, 140));

            dialog.setOnHiding(_ -> secondaryStage.toFront());
            dialog.setOnHidden(_ -> doneLatch.countDown());
            dialog.show();

            // Close primary stage, which should hide the dialog, which should bring secondary stage to front
            Platform.runLater(stage::close);
        }
    }

}
