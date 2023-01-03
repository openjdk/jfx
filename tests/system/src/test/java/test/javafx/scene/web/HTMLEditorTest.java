/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.PlatformUtil;

import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.util.Util;

public class HTMLEditorTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static HTMLEditorTestApp htmlEditorTestApp;

    private HTMLEditor htmlEditor;
    private WebView webView;
    private Scene scene;

    public static class HTMLEditorTestApp extends Application {
        Stage primaryStage = null;

        public HTMLEditorTestApp() {
            super();
        }

        @Override
        public void init() {
            // Used by selectFontFamilysWithSpace() for JDK-8230492
            Font.loadFont(
                HTMLEditorTest.class.getResource("WebKit_Layout_Tests_2.ttf").toExternalForm(),
                10
            );

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
        Util.launch(launchLatch, HTMLEditorTestApp.class);
    }

    @AfterClass
    public static void tearDownOnce() {
        Util.shutdown();
    }

    @Before
    public void setupTestObjects() {
        Platform.runLater(() -> {
            htmlEditor = new HTMLEditor();
            scene = new Scene(htmlEditor);
            htmlEditorTestApp.primaryStage.setScene(scene);
            htmlEditorTestApp.primaryStage.show();

            webView = (WebView) htmlEditor.lookup(".web-view");
            assertNotNull(webView);
            // Cancel the existing load to make our stateProperty listener
            // usable
            webView.getEngine().getLoadWorker().cancel();
        });
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
        final CountDownLatch editorStateLatch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();
        Platform.runLater(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    webView.getEngine().executeScript(
                        "document.body.style.backgroundColor='red';" +
                        "document.body.onfocusout = function() {" +
                        "document.body.style.backgroundColor = 'yellow';" +
                        "}");
                    htmlEditor.requestFocus();
                }
            });
            htmlEditor.setHtmlText(htmlEditor.getHtmlText());

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
                    result.set(webView.getEngine().
                        executeScript("document.body.style.backgroundColor").
                        toString());
                    htmlEditorTestApp.primaryStage.hide();
                    editorStateLatch.countDown();
                }
            });

        });

        assertTrue("Timeout when waiting for focus change ", Util.await(editorStateLatch));
        assertEquals("Focus Change with design mode enabled ", "red", result.get());
    }

    /**
     * @test
     * @bug 8088769
     * Summary Verify CSS styling in HTMLEditor
     */
    @Test
    public void checkStyleWithCSS() throws Exception {
        final CountDownLatch editorStateLatch = new CountDownLatch(1);
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
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                }
            });
            htmlEditor.setHtmlText(htmlEditor.getHtmlText());
            assertNotNull(webView);

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    editorStateLatch.countDown();
                }
            });

        });

        assertTrue("Timeout when waiting for focus change ", Util.await(editorStateLatch));

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
        final CountDownLatch editorStateLatch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();
        Util.runAndWait(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                }
            });

            htmlEditor.setHtmlText("<body style='font-weight: bold'> <p> Test </p> </body>");

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webView.getEngine().
                        executeScript("document.body.focus();");
                    webView.getEngine().
                        executeScript("document.execCommand('selectAll', false, 'true');");
                    webView.getEngine().
                        executeScript("document.execCommand('removeFormat', false, null);");
                    result.set(webView.getEngine().
                        executeScript("document.body.style.fontWeight").
                        toString());
                    editorStateLatch.countDown();
                }
            });

        });

        assertTrue("Timeout when waiting for focus change ", Util.await(editorStateLatch));
        assertNotNull("result must have a valid reference ", result.get());
        assertEquals("document.body.style.fontWeight must be bold ", "bold", result.get());
    }

    /**
     * @test
     * @bug 8230492
     * Summary Check font-family change on font name with numbers
     */
    @Test
    public void selectFontFamilyWithSpace() {
        final CountDownLatch editorStateLatch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();

        Util.runAndWait(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                }
            });

            htmlEditor.setHtmlText("<body>Sample Text</body>");

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    ComboBox<String> fontFamilyComboBox = null;
                    int i = 0;
                    for (Node comboBox : htmlEditor.lookupAll(".font-menu-button")) {
                        // 0 - Format, 1 - Font Family, 2 - Font Size
                        if (i == 1) {
                            assertTrue("fontFamilyComboBox must be ComboBox",
                                comboBox instanceof ComboBox);
                            fontFamilyComboBox = (ComboBox<String>) comboBox;
                            assertNotNull("fontFamilyComboBox must not be null",
                                fontFamilyComboBox);
                        }
                        i++;
                    }
                    webView.getEngine().
                        executeScript("document.execCommand('selectAll', false, 'true');");
                    fontFamilyComboBox.getSelectionModel().select("WebKit Layout Tests 2");
                    result.set(htmlEditor.getHtmlText());
                    editorStateLatch.countDown();
                }
            });
        });

        assertTrue("Timeout when waiting for focus change ", Util.await(editorStateLatch));
        assertNotNull("result must have a valid reference ", result.get());
        assertTrue("font-family must be 'WebKit Layout Test 2' ", result.get().
            contains("font-family: &quot;WebKit Layout Tests 2&quot;"));
    }

    /**
     * @test
     * @bug 8230809
     * Summary HTMLEditor formatting lost when selecting all (CTRL-A)
     */
    @Test
    public void checkFontSizeOnSelectAll_ctrl_A() throws Exception {
        final CountDownLatch editorStateLatch = new CountDownLatch(1);

        final String editorCommand1 =
            "document.execCommand('fontSize', false, '7');" +
            "document.execCommand('insertText', false, 'First_word ');";
        final String editorCommand2 =
            "document.execCommand('fontSize', false, '1');" +
            "document.execCommand('insertText', false, 'Second_word');";

        Util.runAndWait(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                }
            });

            htmlEditor.setHtmlText(htmlEditor.getHtmlText());

            webView.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webView.getEngine().executeScript("document.body.focus();");
                    webView.getEngine().executeScript(editorCommand1);
                    webView.getEngine().executeScript(editorCommand2);

                    editorStateLatch.countDown();
                }
            });
        });

        assertTrue("Timeout while waiting for test html text setup", Util.await(editorStateLatch));

        String expectedHtmlText = htmlEditor.getHtmlText();

        // Select entire text using Ctrl+A (on mac Cmd + A)
        Util.runAndWait(() -> {
            KeyEventFirer keyboard = new KeyEventFirer(htmlEditor, scene);

            keyboard.doKeyPress(KeyCode.A,
                                PlatformUtil.isMac()? KeyModifier.META : KeyModifier.CTRL);
        });

        String actualHtmlText = htmlEditor.getHtmlText();

        assertEquals("Expected and Actual HTML text does not match. ", expectedHtmlText, actualHtmlText);
    }


    @Test
    public void checkFontSizeOnSelectAll_Shift_LeftArrowKey() throws Exception {
        final CountDownLatch editorStateLatch = new CountDownLatch(1);

        final String editorCommand1 =
            "document.execCommand('fontSize', false, '7');" +
            "document.execCommand('insertText', false, 'Hello');";
        final String editorCommand2 =
            "document.execCommand('fontSize', false, '1');" +
            "document.execCommand('insertText', false, 'World');";

        Util.runAndWait(() -> {
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    htmlEditor.requestFocus();
                }
            });

            htmlEditor.setHtmlText(htmlEditor.getHtmlText());

            webView.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webView.getEngine().executeScript("document.body.focus();");
                    webView.getEngine().executeScript(editorCommand1);
                    webView.getEngine().executeScript(editorCommand2);

                    editorStateLatch.countDown();
                }
            });
        });

        assertTrue("Timeout while waiting for test html text setup", Util.await(editorStateLatch));

        String expectedHtmlText = htmlEditor.getHtmlText();

        // Select entire text using SHIFT + series of Left arrows
        Util.runAndWait(() -> {
            KeyEventFirer keyboard = new KeyEventFirer(htmlEditor, scene);
            for (int i = 0; i < 10; i++) {
                keyboard.doLeftArrowPress(KeyModifier.SHIFT);
            }
        });

        String actualHtmlText = htmlEditor.getHtmlText();

        assertEquals("Expected and Actual HTML text does not match. ", expectedHtmlText, actualHtmlText);
    }
}
