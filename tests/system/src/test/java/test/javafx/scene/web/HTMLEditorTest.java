/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static test.util.Util.TIMEOUT;

public class HTMLEditorTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static HTMLEditorTestApp htmlEditorTestApp;

    public static class HTMLEditorTestApp extends Application {
        Stage primaryStage = null;

        public HTMLEditorTestApp() {
            super();
        }

        @Override
        public void init() {
            HTMLEditorTest.htmlEditorTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        // Start the Test Application
        new Thread(() -> Application.launch(HTMLEditorTestApp.class,
            (String[]) null)).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Timeout waiting for FX runtime to start");
            }
        } catch (InterruptedException exception) {
            fail("Unexpected exception: " + exception);
        }

    }

    @AfterClass
    public static void tearDownOnce() {
        Platform.exit();
    }

    /**
     * @test
     * @bug 8090011
     * Summary Check document focus change behavior on tab key press
     */
    @Test
    public void checkFocusChange() throws Exception {
        final CountDownLatch editorStateLatch = new CountDownLatch(2);
        final AtomicBoolean result = new AtomicBoolean(false);
        Platform.runLater(() -> {
            HTMLEditor htmlEditor = new HTMLEditor();
            Scene scene = new Scene(htmlEditor);
            htmlEditorTestApp.primaryStage.setScene(scene);
            WebView webView = (WebView)htmlEditor.lookup(".web-view");
            assertNotNull(webView);

            KeyEvent tabKeyEvent = new KeyEvent(null, webView,
                                KeyEvent.KEY_PRESSED, "", "",
                                KeyCode.TAB, false, false, false, false);

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webView.getEngine().
                        executeScript("document.body.focus();");
                    // Check focus change on repeated tab key press
                    for (int i = 0; i < 10; ++i) {
                        Event.fireEvent(webView, tabKeyEvent);
                    }
                    result.set("red".equals(webView.getEngine().
                        executeScript("document.body.style.backgroundColor").
                        toString()));
                    htmlEditorTestApp.primaryStage.hide();
                    editorStateLatch.countDown();
                }
            });

            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    webView.getEngine().executeScript(
                        "document.body.style.backgroundColor='red';" +
                        "document.body.onfocusout = function() {" +
                        "document.body.style.backgroundColor = 'yellow';" +
                        "}");
                    htmlEditor.requestFocus();
                    editorStateLatch.countDown();
                }
            });
            htmlEditorTestApp.primaryStage.show();
        });

        try {
            editorStateLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        } finally {
            assertTrue("Focus Change with design mode enabled ", result.get());
        }
    }
}
