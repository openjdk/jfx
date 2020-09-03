/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CSSFilterTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static CSSFilterTestApp cssFilterTestApp;

    private WebView webView;

    public static class CSSFilterTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            CSSFilterTest.cssFilterTestApp = this;
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
        // Start the Test Application
        new Thread(() -> Application.launch(CSSFilterTestApp.class, (String[])null)).start();

        assertTrue("Timeout waiting for FX runtime to start", Util.await(launchLatch));
    }

    @AfterClass
    public static void tearDownOnce() {
        Platform.exit();
    }

    @Before
    public void setupTestObjects() {
        Platform.runLater(() -> {
            webView = new WebView();
            cssFilterTestApp.primaryStage.setScene(new Scene(webView));
            cssFilterTestApp.primaryStage.show();
        });
    }

    @Test public void testCSSFilterRendering() {
        final CountDownLatch webViewStateLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            assertNotNull(webView);
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

            final String urlString = CSSFilterTest.class.getResource("simpleImagewithfilter.html").toExternalForm();
            webView.getEngine().load(urlString);
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));
        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = cssFilterTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            Color whiteColor = Color.rgb(255, 255, 255);
            Color blueColor = Color.rgb(0, 0, 255);

            assertColorEquals("Color should be opaque white:",
                    whiteColor, pr.getColor(0, 0), delta);
            assertColorEquals("Color should be opaque white:",
                    whiteColor, pr.getColor(5, 0), delta);
            assertColorEquals("Color should be opaque white:",
                    whiteColor, pr.getColor(0, 5), delta);

            assertColorEquals("Color should be opaque blue:",
                    blueColor, pr.getColor(25, 25), delta);
            assertColorEquals("Color should be opaque blue:",
                    blueColor, pr.getColor(190, 200), delta);
            assertColorEquals("Color should be opaque blue:",
                    blueColor, pr.getColor(200, 190), delta);
            assertColorEquals("Color should be opaque blue:",
                    blueColor, pr.getColor(200, 200), delta);

            assertColorNotEquals("Color should not be opaque white:",
                    whiteColor, pr.getColor(220, 220), delta);
            assertColorNotEquals("Color should not be opaque blue:",
                    blueColor, pr.getColor(220, 220), delta);
        });
    }
}
