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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SVGTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static SVGTestApp svgTestApp;

    private WebView webView;

    public static class SVGTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            SVGTest.svgTestApp = this;
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
        new Thread(() -> Application.launch(SVGTestApp.class, (String[])null)).start();

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
            svgTestApp.primaryStage.setScene(new Scene(webView));
            svgTestApp.primaryStage.show();
        });
    }

    /**
     * @test
     * @bug 8223298
     * summary Checks if svg pattern is displayed properly
     */
    @Test public void testSVGRenderingWithPattern() {
        final CountDownLatch webViewStateLatch = new CountDownLatch(1);
        final String htmlSVGContent = "\n"
            + "<html>\n"
            + "<body style='margin: 0px 0px;'>\n"
            + "<svg width='400' height='150'>\n"
            + "<defs>\n"
            + "<pattern id='pattern1' x='0' y='0' width='30' height='30' patternUnits='userSpaceOnUse'>\n"
            + "<rect width='20' height='20' fill='red' />\n"
            + "</pattern>\n"
            + "</defs>\n"
            + "<rect width='400' height='150' fill='url(#pattern1)' />\n"
            + "</svg>\n"
            + "</body>\n"
            + "</html>";

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

            webView.getEngine().loadContent(htmlSVGContent);
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));
        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = svgTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            Color redColor = Color.color(1, 0, 0);
            assertEquals("Color should be opaque red:", redColor, pr.getColor(0, 0));
            assertEquals("Color should be opaque red:", redColor, pr.getColor(30, 30));
            assertEquals("Color should be opaque red:", redColor, pr.getColor(49, 49));
        });
    }

    /**
     * @test
     * @bug 8218973
     * summary Checks if svg draws correctly with mask
     */
    @Test public void testSVGRenderingWithMask() {
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
            final String urlString = SVGTest.class.getResource("svgMask.html").toExternalForm();
            webView.getEngine().load(urlString);
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));
        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = svgTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            final double delta = 0.07;
            final Color greenColor = Color.rgb(0, 128, 0);
            final Color cinnamonColor = Color.rgb(128, 64, 0);
            final Color redColor = Color.rgb(255, 0, 0);

            // Test path pixels
            assertColorEquals("Color should be opaque green:",
                    Color.rgb(65, 95, 0), pr.getColor(50, 150), delta);
            assertColorEquals("Color should be opaque red:",
                    Color.rgb(192, 32, 0), pr.getColor(150, 150), delta);

            // Test Rect pixels
            assertColorEquals("Color should be opaque green:",
                    greenColor, pr.getColor(200, 0), delta);
            assertColorEquals("Color should be opaque green:",
                    greenColor, pr.getColor(200, 199), delta);
            assertColorEquals("Color should be opaque Cinnamon:",
                    cinnamonColor, pr.getColor(300, 0), delta);
            assertColorEquals("Color should be opaque Cinnamon:",
                    cinnamonColor, pr.getColor(300, 199), delta);
            assertColorEquals("Color should be opaque red:",
                    redColor, pr.getColor(399, 0), delta);
            assertColorEquals("Color should be opaque red:",
                    redColor, pr.getColor(399, 199), delta);

            // Test RoundedRect pixels
            assertColorEquals("Color should be opaque green:",
                    Color.rgb(65, 95, 0), pr.getColor(50, 250), delta);
            assertColorEquals("Color should be opaque green:",
                    Color.rgb(65, 95, 0), pr.getColor(50, 350), delta);
            assertColorEquals("Color should be opaque red:",
                    Color.rgb(192, 32, 0), pr.getColor(150, 250), delta);
            assertColorEquals("Color should be opaque red:",
                    Color.rgb(192, 32, 0), pr.getColor(150, 350), delta);

            // Test Stroke pixels
            assertColorEquals("Color should be opaque green:",
                    greenColor, pr.getColor(203, 203), delta);
            assertColorEquals("Color should be opaque green:",
                    greenColor, pr.getColor(203, 401), delta);
            assertColorEquals("Color should be opaque red:",
                    redColor, pr.getColor(401, 203), delta);
            assertColorEquals("Color should be opaque red:",
                    redColor, pr.getColor(401, 401), delta);

        });
    }


}
