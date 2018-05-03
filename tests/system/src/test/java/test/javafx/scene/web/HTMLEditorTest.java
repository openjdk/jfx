/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.Ignore;
import org.junit.Test;
import test.util.Util;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static test.util.Util.TIMEOUT;

public class HTMLEditorTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static HTMLEditorTestApp htmlEditorTestApp;

    private HTMLEditor htmlEditor;
    private WebView webView;

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
            Platform.setImplicitExit(false);
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
    // Currently ignoring this test case due to regression (JDK-8200418).
    // The newly cloned issue (JDK-8202542) needs to be fixed before
    // re-enabling this test case.
    @Test @Ignore("JDK-8202542")
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
            editorStateLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        } finally {
            assertTrue("Focus Change with design mode enabled ", result.get());
        }
    }

    /**
     * @test
     * @bug 8088769
     * Summary Verify CSS styling in HTMLEditor
     */
    @Test
    public void checkStyleWithCSS() throws Exception {
        final CountDownLatch editorStateLatch = new CountDownLatch(2);
        final String editorCommand1 =
            "document.execCommand('bold', false, 'true');" +
            "document.execCommand('italic', false, 'true');" +
            "document.execCommand('insertText', false, 'Hello World');";
        final String editorCommand2 =
            "document.execCommand('selectAll', false, 'true');" +
            "document.execCommand('delete', false, 'true');" +
            "document.execCommand('bold', false, 'false');" +
            "document.execCommand('italic', false, 'false');" +
            "document.execCommand('underline', false, 'true');" +
            "document.execCommand('forecolor', false," +
                " 'rgba(255, 155, 0, 0.4)');" +
            "document.execCommand('backcolor', false," +
                " 'rgba(150, 90, 5, 0.5)');" +
            "document.execCommand('insertText', false, 'Hello HTMLEditor');";
        final String expectedHTML = "<html dir=\"ltr\"><head></head><body " +
            "contenteditable=\"true\"><span style=\"font-weight: bold; " +
            "font-style: italic;\">Hello World</span></body></html>";

        Util.runAndWait(() -> {
            htmlEditor = new HTMLEditor();
            Scene scene = new Scene(htmlEditor);
            htmlEditorTestApp.primaryStage.setScene(scene);
            webView = (WebView)htmlEditor.lookup(".web-view");
            assertNotNull(webView);

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    editorStateLatch.countDown();
                }
            });

            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                    editorStateLatch.countDown();
                }
            });
            htmlEditorTestApp.primaryStage.show();
        });

        try {
            if (!editorStateLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Timeout waiting for callbacks");
            }
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        }

        Util.runAndWait(() -> {
            webView.getEngine().executeScript("document.body.focus();");
            webView.getEngine().executeScript(editorCommand1);
            assertEquals(expectedHTML, htmlEditor.getHtmlText());
            webView.getEngine().executeScript(editorCommand2);
            assertEquals(webView.getEngine().executeScript(
                "document.getElementsByTagName('span')[0].style.textDecoration")
                .toString(),
                "underline");
            assertEquals(webView.getEngine().executeScript(
                "document.getElementsByTagName('span')[0].style.fontWeight")
                .toString(), "");
            assertEquals(webView.getEngine().executeScript(
                "document.getElementsByTagName('span')[0].style.fontStyle")
                .toString(), "");
            testColorEquality("rgba(255, 155, 0, 0.4)",
                webView.getEngine().executeScript(
                "document.getElementsByTagName('span')[0].style.color")
                .toString(), 0.01);
            testColorEquality("rgba(150, 90, 5, 0.5)",
                webView.getEngine().executeScript(
                "document.getElementsByTagName('span')[0].style.backgroundColor")
                .toString(), 0.01);
            htmlEditorTestApp.primaryStage.hide();
        });
    }

    private void testColorEquality(String expectedColor, String actualColor,
                                   double delta) {
        assertTrue(actualColor.startsWith("rgba"));
        final String[] actualValues =
            actualColor.substring(actualColor.indexOf('(') + 1,
            actualColor.lastIndexOf(')')).split(",");
        final String[] expectedValues =
            expectedColor.substring(expectedColor.indexOf('(') + 1,
            expectedColor.lastIndexOf(')')).split(",");
        for (int i = 0; i < 3; i++) {
            assertEquals(Integer.parseInt(actualValues[i].trim()),
                         Integer.parseInt(expectedValues[i].trim()));
        }
        assertEquals(Double.parseDouble(actualValues[3].trim()),
                     Double.parseDouble(expectedValues[3].trim()), delta);
    }

    /**
     * @test
     * @bug 8200418
     * Summary Check Style property after removeformat
     */
    @Test
    public void checkStyleProperty() throws Exception {
        final CountDownLatch editorStateLatch = new CountDownLatch(2);
        final AtomicBoolean result = new AtomicBoolean(false);
        Platform.runLater(() -> {
            HTMLEditor htmlEditor = new HTMLEditor();
            Scene scene = new Scene(htmlEditor);
            htmlEditorTestApp.primaryStage.setScene(scene);
            htmlEditor.setHtmlText("<html>" +
                "<head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                "</head>" +
                "<body style=\"font-weight: bold\">" +
                "<p>Test</p>" +
                "</body>" +
                "</html>");

            WebView webView = (WebView)htmlEditor.lookup(".web-view");
            assertNotNull(webView);

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webView.getEngine().
                        executeScript("document.body.focus();");
                    webView.getEngine().
                        executeScript("document.execCommand('selectAll', false, 'true');");
                    webView.getEngine().
                        executeScript("document.execCommand('removeFormat', false, null);");
                    result.set("bold".equals(webView.getEngine().
                        executeScript("document.body.style.fontWeight").
                        toString()));
                    htmlEditorTestApp.primaryStage.hide();
                    editorStateLatch.countDown();
                }
            });

            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                    editorStateLatch.countDown();
                }
            });
            htmlEditorTestApp.primaryStage.show();
        });

        try {
            editorStateLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        } finally {
            assertTrue("check Style Property with removeFormat ", result.get());
        }
    }
}
