/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngineShim;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;

import test.util.Util;

public class PageFillTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static PageFillTestApp pageFillTestApp;

    private WebView webView;

    public static class PageFillTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            PageFillTest.pageFillTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    private static String colorToString(Color c) {
        int r = (int)(c.getRed() * 255.0);
        int g = (int)(c.getGreen() * 255.0);
        int b = (int)(c.getBlue() * 255.0);
        int a = (int)(c.getOpacity() * 255.0);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private void assertColorEquals(String msg, Color expected, Color actual, double delta) {
        if (!testColorEquals(expected, actual, delta)) {
            fail(msg + " expected:" + colorToString(expected)
                    + " but was:" + colorToString(actual));
        }
    }

    private void assertColorNotEquals(String msg, Color notExpected, Color actual, double delta) {
        if (testColorEquals(notExpected, actual, delta)) {
            fail(msg + " not expected:" + colorToString(notExpected)
                    + " but was:" + colorToString(actual));
        }
    }

    protected boolean testColorEquals(Color expected, Color actual, double delta) {
        double deltaRed = Math.abs(expected.getRed() - actual.getRed());
        double deltaGreen = Math.abs(expected.getGreen() - actual.getGreen());
        double deltaBlue = Math.abs(expected.getBlue() - actual.getBlue());
        double deltaOpacity = Math.abs(expected.getOpacity() - actual.getOpacity());
        return (deltaRed <= delta && deltaGreen <= delta && deltaBlue <= delta && deltaOpacity <= delta);
    }

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, PageFillTestApp.class);
    }

    @AfterClass
    public static void tearDownOnce() {
        Util.shutdown();
    }

    @Before
    public void setupTestObjects() {
        Platform.runLater(() -> {
            webView = new WebView();
            Scene scene = new Scene(webView, Color.web("#00ff00"));
            pageFillTestApp.primaryStage.setScene(scene);
            pageFillTestApp.primaryStage.show();
        });
    }

    @Test public void testPageFillRendering() {
        final CountDownLatch webViewStateLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            assertNotNull(webView);

            webView.setPageFill(Color.TRANSPARENT);

            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    webView.requestFocus();
                }
            });

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webViewStateLatch.countDown();
                }
            });

            String content = "<html>" + "<head></head>" +
                    "<body>" +
                    "<span style=\"color: black; font-family: Arial,Helvetica,sans-serif\">" +
                    "<br>this is a line".repeat(100) +
                    "</span></body></html>";
            webView.getEngine().loadContent(content);
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));
        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = pageFillTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            Color color = Color.rgb(0, 255, 0); // scene color + transparent = scene color
            assertColorEquals("Color 1 should be:",
                    color, pr.getColor(0, 0), delta);
            assertColorEquals("Color 2 should be:",
                    color, pr.getColor(50, 10), delta);
            assertColorEquals("Color 3 should be:",
                    color, pr.getColor(50, 50), delta);
            assertColorEquals("Color 4 should be:",
                    color, pr.getColor(110, 50), delta);

            assertColorNotEquals("Color 5 should not be:",
                    color, pr.getColor(10, 50), delta);
        });

        Util.sleep(1000);

        Util.runAndWait(() -> {
            final WebPage page = WebEngineShim.getPage(webView.getEngine());
            assertNotNull(page);
            WebPageShim.scroll(page, 1, 1, 0, 200);
        });

        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = pageFillTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            Color color = Color.rgb(0, 255, 0); // scene color + transparent = scene color
            assertColorEquals("Color 6 should be:",
                    color, pr.getColor(0, 0), delta);
            assertColorEquals("Color 7 should be:",
                    color, pr.getColor(50, 10), delta);
            assertColorEquals("Color 8 should be:",
                    color, pr.getColor(110, 50), delta);
            assertColorEquals("Color 9 should be:",
                    color, pr.getColor(110, 100), delta);

            assertColorNotEquals("Color 10 should not be:",
                    color, pr.getColor(10, 50), delta);
        });

        Util.runAndWait(() -> {
            webView.setStyle("-fx-page-fill: orange;");
        });

        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = pageFillTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            Color color = Color.ORANGE; // scene color + opaque orange = orange
            assertColorEquals("Color 11 should be:",
                    color, pr.getColor(0, 0), delta);
            assertColorEquals("Color 12 should be:",
                    color, pr.getColor(50, 10), delta);
            assertColorEquals("Color 13 should be:",
                    color, pr.getColor(110, 50), delta);
            assertColorEquals("Color 14 should be:",
                    color, pr.getColor(110, 100), delta);

            assertColorNotEquals("Color 15 should not be:",
                    color, pr.getColor(10, 50), delta);
        });

        Util.runAndWait(() -> {
            webView.setStyle("-fx-page-fill: #ccddeecf;");
            final WebPage page = WebEngineShim.getPage(webView.getEngine());
            assertNotNull(page);
            WebPageShim.scroll(page, 1, 1, 0, 200);
        });

        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = pageFillTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            Color color = Color.rgb(165,227,193); // scene color + #ccddeecf = scene opaque #A5E3C1
            assertColorEquals("Color 16 should be:",
                    color, pr.getColor(0, 0), delta);
            assertColorEquals("Color 17 should be:",
                    color, pr.getColor(50, 10), delta);
            assertColorEquals("Color 18 should be:",
                    color, pr.getColor(110, 50), delta);
            assertColorEquals("Color 19 should be:",
                    color, pr.getColor(110, 100), delta);

            assertColorNotEquals("Color 20 should not be:",
                    color, pr.getColor(10, 50), delta);
        });

        Util.runAndWait(() -> {
            String content = "<html>" + "<head></head>" +
                    "<body style=\"background-color:#da10a2\">" +
                    "<span style=\"color: black; font-family: Arial,Helvetica,sans-serif\">" +
                    "<br>this is another line".repeat(100) +
                    "</span></body></html>";
            webView.getEngine().loadContent(content);
        });

        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = pageFillTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            Color color = Color.web("#da10a2"); // scene color + #ccddeecf + web #da10a2 = #da10a2
            assertColorEquals("Color 21 should be:",
                    color, pr.getColor(0, 0), delta);
            assertColorEquals("Color 22 should be:",
                    color, pr.getColor(50, 10), delta);
            assertColorEquals("Color 23 should be:",
                    color, pr.getColor(110, 50), delta);
            assertColorEquals("Color 24 should be:",
                    color, pr.getColor(110, 100), delta);

            assertColorNotEquals("Color 25 should not be:",
                    color, pr.getColor(10, 50), delta);
        });
    }
}
