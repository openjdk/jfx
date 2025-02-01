/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Test that we can shutdown the JavaFX runtime cleanly via Platform::exit
 * while in full screen mode.
 *
 * @bug 8335630 8299738
 */
public class FullScreenExitTest {
    private static Stage stage;
    private static Scene scene;
    private static BorderPane borderPane;

    private static volatile Throwable exception;

    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static CountDownLatch stopLatch = new CountDownLatch(1);

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            Thread.currentThread().setUncaughtExceptionHandler((thr, ex) -> {
                System.err.println("Exception caught in thread: " + thr);
                ex.printStackTrace();
                exception = ex;
            });

            stage = primaryStage;

            borderPane = new BorderPane();

            scene = new Scene(borderPane, 400, 300);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setOnShown(e -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }

        @Override
        public void stop() {
            stopLatch.countDown();
        }
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
        Util.runAndWait(() -> {
            assertTrue(stage.isShowing());
            assertTrue(stage.isFullScreen());
        });
    }

    @Test
    public void exitWhileInFullScreenDoesNotCrash() {
        // Ensure that the window has had time to transition to full screen
        Util.sleep(3000);

        // Shutdown the JavaFX runtime with full-screen stage showing
        Platform.exit();

        // Wait until the application has been stopped and then sleep for
        // one second to verify that it will not crash nor throw an exception
        assertTrue(Util.await(stopLatch), "Timeout waiting for Application::stop");
        Util.sleep(1000);

        assertSame(null, exception, "Unexpected exception");
    }

}
