/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngineShim;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.io.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import static org.junit.Assert.assertNotEquals;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StraightLineTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static StraightLineTestApp straightLineTestApp;

    private WebView webView;

    public static class StraightLineTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            StraightLineTest.straightLineTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            this.primaryStage = primaryStage;
            this.primaryStage.setWidth(80);
            this.primaryStage.setHeight(60);
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


    @BeforeClass
    public static void setupOnce() {
        // Start the Test Application
        new Thread(() -> Application.launch(StraightLineTestApp.class, (String[])null)).start();

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
            Scene scene = new Scene(webView,80,60);
            straightLineTestApp.primaryStage.setScene(scene);
            straightLineTestApp.primaryStage.show();
        });
    }

    @Test public void testLine() {
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

            webView.getEngine().loadContent("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<style>\n" +
                    "div {\n" +
                    "padding:0px;\n"+
                    "  text-decoration: underline;\n" +
                    "  text-decoration-thickness: 20px;\n" +
                    "font-size: 16px; \n" +
                    "}\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div>TEST</dv>\n" +
                    "</body>\n" +
                    "</html>");

        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));
        Util.sleep(1000);

        Util.runAndWait(() -> {
            WritableImage snapshot = straightLineTestApp.primaryStage.getScene().snapshot(null);
            PixelReader pr = snapshot.getPixelReader();

            int start_x = (int)webView.getEngine().executeScript("document.getElementsByTagName('div')[0].getBoundingClientRect().x");
            int start_y = (int)webView.getEngine().executeScript("document.getElementsByTagName('div')[0].getBoundingClientRect().y");
            int height = (int)webView.getEngine().executeScript("document.getElementsByTagName('div')[0].getBoundingClientRect().height");
            int width = (int)webView.getEngine().executeScript("document.getElementsByTagName('div')[0].getBoundingClientRect().width");

            // buttom start x position of underline ( startx + font size + thickness -1)
            int line_start_x = start_x + height + 20 - 1;
            // buttom start y position of underline ( startx + height)
            int line_start_y = start_y + height;
            String line_color = "rgba(0,0,0,255)"; // color of line
            for (int i = line_start_y; i < snapshot.getHeight(); i++) {
                String expected = colorToString(pr.getColor(line_start_x, i));
                if(expected.equals(line_color))
                    continue;
                else
                    fail("Each pixel color of line should be" + line_color + " but was:" + expected);
            }
        });
    }
}
