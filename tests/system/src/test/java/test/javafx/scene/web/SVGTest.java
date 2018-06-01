/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.util.Util.TIMEOUT;

public class SVGTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static TestApp testApp;

    private static WebView webView;

    public static class TestApp extends Application {
        Stage primaryStage = null;

        public TestApp() {
            super();
        }

        @Override
        public void init() {
            SVGTest.testApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            final WebView webView = new WebView();

            Platform.setImplicitExit(false);
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.setScene(new Scene(webView));
            primaryStage.show();

            SVGTest.webView = webView;
            this.primaryStage = primaryStage;

            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() throws InterruptedException {
        // Start the Test Application
        new Thread(() -> Application.launch(TestApp.class,
            (String[]) null)).start();

        assertTrue("Timeout waiting for FX runtime to start",
            launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        );
    }

    @AfterClass
    public static void tearDownOnce() {
        Platform.exit();
    }

    @Test public void testCrashOnScrollableSVG() throws InterruptedException {
        final CountDownLatch loadComplete = new CountDownLatch(1);
        Util.runAndWait(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    loadComplete.countDown();
                }
            });
            final URL url = TestApp.class.getResource(
                "crash-on-scrollable-svg.html"
            );
            assertNotNull(url);
            webView.getEngine().load(url.toExternalForm());
        });

        assertTrue("Timeout waiting for page load", loadComplete.await(TIMEOUT, TimeUnit.MILLISECONDS));

        // Check pixel is as expected to ensure SVG image rendering
        Util.runAndWait(() -> {
            final Image snap = webView.snapshot(null, null);
            assertNotNull(snap);
            assertTrue(snap.getWidth() > 100);
            assertTrue(snap.getHeight() > 100);
            final PixelReader reader = snap.getPixelReader();
            assertNotNull(reader);
            assertEquals(String.format("Pixel (50, 50) is not RED"),
                    Color.RED, reader.getColor(50, 50));
        });
    }
}
