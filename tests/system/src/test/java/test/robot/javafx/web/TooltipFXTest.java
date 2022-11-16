/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static test.util.Util.TIMEOUT;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class TooltipFXTest {

    private static final String html = "<html>" +
        "<head><style> button { position:relative; height: 100%; width: 100%; }</style></head> " +
        "<body> <button id=\"mybtn\" title=\"Tooltip\" type=\"button\">Show tooltip</button></body>" +
        "</html>";

    private WeakReference<WebView> webViewRef;

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Sleep time to allow tooltip to show in milliseconds
    private static final int TOOLTIP_SLEEP_TIME = 3000;

    private static CountDownLatch startupLatch = new CountDownLatch(1);

    private Scene scene;

    private Stage stageToHide;

    static Robot robot;

    private int offset = 30;

    public static void main(String[] args) throws Exception {
        initFX();
        new TooltipFXTest().testTooltipLeak();
        teardown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            robot = new Robot();
            primaryStage.setTitle("Primary Stage");
            BorderPane root = new BorderPane();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setX(20);
            primaryStage.setY(20);
            primaryStage.setWidth(100);
            primaryStage.setHeight(100);
            Platform.runLater(startupLatch::countDown);
            primaryStage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void teardown() {
        Util.shutdown();
    }

// ========================== TEST CASE ==========================
    @Test(timeout = 20000) public void testTooltipLeak() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            final Stage stage = new Stage();
            stage.setAlwaysOnTop(true);
            stageToHide = stage;
            stage.setTitle("Stage ");
            WebView webview = new WebView();
            webViewRef = new WeakReference<WebView>(webview);
            scene = new Scene(webview);
            scene.setFill(Color.LIGHTYELLOW);
            stage.setWidth(610);
            stage.setHeight(580);
            stage.setScene(scene);

            webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    latch.countDown();
                }
            });
                webview.getEngine().loadContent(html);
                stage.show();
        });

        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for web content to load");
        }

        Util.runAndWait(() -> {
            // Note that we need to click somewhere in the Stage to make sure
            // we will get focus on the Windows platform.
            robot.mouseMove((int)(scene.getWindow().getX() + scene.getX()),
                (int)(scene.getWindow().getY() + scene.getY()));
            robot.mouseClick(MouseButton.PRIMARY);
        });

        Util.sleep(SLEEP_TIME);

        for (int i = 0; i < 3; ++i) {
            Util.runAndWait(() -> {
                robot.mouseMove((int)(scene.getWindow().getX() + scene.getX() + offset),
                      (int)(scene.getWindow().getY() + scene.getY() + offset));
                offset += 20;
            });
        }

        Util.sleep(TOOLTIP_SLEEP_TIME);

        Util.runAndWait(() -> {
            stageToHide.hide();
            stageToHide = null;
            scene = null;
        });

        for (int j = 0; j < 5; ++j) {
            System.gc();
            if (webViewRef.get() == null) {
                break;
            }
            Util.sleep(SLEEP_TIME);
        }
        assertNull("webViewRef is not null", webViewRef.get());
    }
}
