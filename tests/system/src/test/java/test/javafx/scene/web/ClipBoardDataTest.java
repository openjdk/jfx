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

import com.sun.javafx.PlatformUtil;
import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.web.WebEngineShim;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.io.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;

import java.util.concurrent.CountDownLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClipBoardDataTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);
    private static final String DATA_COUNT = "2";

    // Maintain one application instance
    static ClipBoardDataTestApp clipBoardDataTestApp;

    private WebView webView;

    public static class ClipBoardDataTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            ClipBoardDataTest.clipBoardDataTestApp = this;
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
        // Start the Test Application
        new Thread(() -> Application.launch(ClipBoardDataTestApp.class, (String[])null)).start();

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
            Scene scene = new Scene(webView);
            clipBoardDataTestApp.primaryStage.setScene(scene);
            clipBoardDataTestApp.primaryStage.show();
        });
    }

    @Test public void testDataCount() {
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

            webView.getEngine().loadContent(
                    "<html>\n" +
                            "<head> \n" +
                            "   \n" +
                            "</head>\n" +
                            "<body>\n" +
                            "<p id=\"data\">This is a test of the clipboard. The content of the clipboard will be displayed below after pressing ctrl+v:</p>\n" +
                            "<div id=\"clipboardData\" contenteditable='true'></div>\n" +
                            " <script>\n" +
                            "        document.addEventListener('paste', e => {\n" +
                            "            let messages = [];\n" +
                            "            if (e.clipboardData.types) {\n" +
                            "                let message_index = 0;\n" +
                            "                e.clipboardData.types.forEach(type => {\n" +
                            "                    messages.push( type + \": \" + e.clipboardData.getData(type));\n" +
                            "                    const para = document.createElement(\"p\");\n" +
                            "                    para.innerText = type + \": \" + e.clipboardData.getData(type);\n" +
                            "                    document.getElementById(\"clipboardData\").innerText = ++message_index;\n" +
                            "                });\n" +
                            "            }\n" +
                            "        });\n" +
                            "</script>\n" +
                            "</body>\n" +
                            "</html>");
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(webViewStateLatch));

        Util.sleep(2000);
        Util.runAndWait(() -> {
            // Select entire text using Ctrl+A (on mac Cmd + A)
                KeyEventFirer keyboard = new KeyEventFirer(webView, clipBoardDataTestApp.primaryStage.getScene());

                keyboard.doKeyPress(KeyCode.A,
                        PlatformUtil.isMac()? KeyModifier.META : KeyModifier.CTRL);
                Util.sleep(500);

                keyboard.doKeyPress(KeyCode.V,
                    PlatformUtil.isMac()? KeyModifier.META : KeyModifier.CTRL);
                Util.sleep(500);
                assertEquals(DATA_COUNT, webView.getEngine().
                    executeScript("document.getElementById(\"clipboardData\").innerText;").toString());
        });
    }
}