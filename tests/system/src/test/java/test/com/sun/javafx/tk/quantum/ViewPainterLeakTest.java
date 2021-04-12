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

package test.com.sun.javafx.tk.quantum;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;
import test.util.memory.JMemoryBuddy;

import static org.junit.Assert.*;

public class ViewPainterLeakTest {

    private static CountDownLatch startupLatch;
    private static Stage stage;
    private static ScrollPane scrollPane;
    private static WeakReference<Scene> sceneRef;

    private static final String text =
            "The quick brown fox jumps over the lazy dog." +
            " " +
            "The quick brown fox jumps over the lazy dog.";

    public static class TestApp extends Application {
        @Override
        public void start(Stage stage) {
            ViewPainterLeakTest.stage = stage;

            Platform.setImplicitExit(false);

            TextFlow content = new TextFlow(new Text(text));
            scrollPane = new ScrollPane(content);
            StackPane root = new StackPane(scrollPane);

            Scene scene = new Scene(root, 200, 100);
            sceneRef = new WeakReference<>(scene);
            stage.setScene(scene);

            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> {
                    startupLatch.countDown();
                });
            });
            stage.show();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        assertTrue("Timeout waiting for FX runtime to start",
                startupLatch.await(15, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void teardown() {
        Platform.exit();
    }

    @Test
    public void testViewPainterLeak() {
        // Wait for some time to make sure the initial content is displayed
        Util.sleep(500);

        // This will trigger the leak
        Util.runAndWait(() -> scrollPane.setHvalue(0.5));

        // Wait for some time to make sure the scrolled content is displayed
        Util.sleep(500);
        Util.runAndWait(() -> {
            stage.hide();
            stage = null;
            scrollPane = null;
        });
        JMemoryBuddy.assertCollectable(sceneRef);
    }

}
