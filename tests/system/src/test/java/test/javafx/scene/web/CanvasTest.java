/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class CanvasTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static CanvasTestApp canvasTestApp;

    private WebView webView;

    public static class CanvasTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            CanvasTest.canvasTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, CanvasTestApp.class);

        assertTrue("Timeout waiting for FX runtime to start", Util.await(launchLatch));
    }

    @AfterClass
    public static void tearDownOnce() {
        Util.shutdown();
    }

    @Before
    public void setupTestObjects() {
        Platform.runLater(() -> {
            webView = new WebView();
            canvasTestApp.primaryStage.setScene(new Scene(webView));
            canvasTestApp.primaryStage.show();
        });
    }

    /**
     * @test
     * @bug 8234471
     * Summary Check if canvas displays the whole rectangle
     */
    @Test
    public void testCanvasRect() throws Exception {
        final CountDownLatch webViewStateLatch = new CountDownLatch(1);
        final String htmlCanvasContent = "\n"
            + "<canvas id='canvas' width='100' height='100'></canvas>\n"
            + "<script>\n"
            + "var ctx = document.getElementById('canvas').getContext('2d');\n"
            + "ctx.fillStyle = 'red';\n"
            + "ctx.fillRect(0, 0, 100, 100);\n"
            + "</script>\n";

        Util.runAndWait(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    webView.requestFocus();
                }
            });

            assertNotNull(webView);
            webView.getEngine().loadContent(htmlCanvasContent);

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webViewStateLatch.countDown();
                }
            });
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));

        Util.runAndWait(() -> {
            int redColor = 255;
            assertEquals("Rect top-left corner", redColor, (int) webView.getEngine().executeScript(
                "document.getElementById('canvas').getContext('2d').getImageData(1, 1, 1, 1).data[0]"));
            assertEquals("Rect bottom-right corner", redColor, (int) webView.getEngine().executeScript(
                "document.getElementById('canvas').getContext('2d').getImageData(99, 99, 1, 1).data[0]"));
        });
    }
}
