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

package test.robot.javafx.web;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import test.util.Util;

public class TextSelectionTest {

    private static final String html = "<html>" +
        "<head></head> " +
        "<body>&nbsp&nbsp&nbsp&nbsp some text</body>" +
        "</html>";

    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static CountDownLatch webviewLoadLatch = new CountDownLatch(1);
    private static Scene scene;
    private static Robot robot;
    private Color colorBefore;
    private Color colorAfter;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {

            robot = new Robot();
            WebView webview = new WebView();
            scene = new Scene(webview, 400, 300);
            primaryStage.setScene(scene);
            primaryStage.setAlwaysOnTop(true);

            webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    webviewLoadLatch.countDown();
                }
            });
            webview.getEngine().loadContent(html);
            primaryStage.setOnShown(e -> startupLatch.countDown());
            primaryStage.show();
        }
    }

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
        Util.waitForLatch(webviewLoadLatch, 10, "Timeout waiting for web content to load");
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    // ========================== TEST CASE ==========================
    @Test
    @Timeout(value=20)
    public void testTextSelection() throws Exception {

        int x = (int)(scene.getWindow().getX() + scene.getX() + 22);
        int y = (int)(scene.getWindow().getY() + scene.getY() + 15);

        Util.parkCursor(robot);
        Util.runAndWait(() -> colorBefore = robot.getPixelColor(x, y));

        Util.runAndWait(() -> robot.mouseMove(x, y));
        Util.doubleClick(robot);
        Util.sleep(500); // Wait for the selection highlight to be drawn

        Util.parkCursor(robot);
        Util.runAndWait(() -> colorAfter = robot.getPixelColor(x, y));

        Assertions.assertNotEquals(colorBefore, colorAfter,
            "Selection color did not change after double click");
    }
}
