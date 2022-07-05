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

import com.sun.javafx.scene.control.CustomColorDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ColorChooserTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    static ColorChooserTestApp colorChooserTestApp;

    private WebView webView;
    private Scene scene;
    private int counter;
    private final List<String> colors = List.of("#ff3300", "#3367ff", "#cc072d");

    public static class ColorChooserTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            ColorChooserTest.colorChooserTestApp = this;
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
        new Thread(() -> Application.launch(ColorChooserTestApp.class, (String[])null)).start();

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
            scene = new Scene(webView);
            colorChooserTestApp.primaryStage.setScene(scene);
            colorChooserTestApp.primaryStage.setAlwaysOnTop(true);
            colorChooserTestApp.primaryStage.setWidth(300);
            colorChooserTestApp.primaryStage.setHeight(300);
            colorChooserTestApp.primaryStage.show();
        });
    }

    @Test public void testColorChooser() {
        final CountDownLatch webViewStateLatch = new CountDownLatch(2);

        Util.runAndWait(() -> {
            assertNotNull(webView);
            Window.getWindows().addListener((ListChangeListener<Window>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().stream()
                                .filter(w -> w.getScene().getRoot() instanceof CustomColorDialog)
                                .findFirst()
                                .map(w -> (CustomColorDialog) w.getScene().getRoot())
                                .ifPresent(dialog -> {
                                    assertEquals(dialog.getCurrentColor(), Color.web(colors.get(counter - 1)));
                                    dialog.setCustomColor(Color.web(colors.get(counter)));
                                    HBox box = (HBox) dialog.lookup("#buttons-hbox");
                                    Button ok = (Button) box.getChildren().get(0);
                                    Platform.runLater(() -> {
                                        ok.fire();
                                        webViewStateLatch.countDown();
                                    });
                                });
                    }
                }
            });
            webView.getEngine().setOnAlert(event -> {
                assertNotNull(event.getData());
                assertEquals(event.getData(), "color: " + colors.get(counter++));
            });
            webView.getEngine().loadContent("<head>" +
                    "<script>" +
                    "   function logColor(event) {" +
                    "        var color = document.querySelector(\"#color\");\n" +
                    "        alert(\"color: \" + color.value);" +
                    "   }\n" +
                    "   setTimeout(\n" +
                    "     () => {\n" +
                    "        var color = document.querySelector(\"#color\");\n" +
                    "        color.addEventListener(\"change\", logColor, false);" +
                    "        alert(\"color: \" + color.value);" +
                    "     }, 100);" +
                    "</script>" +
                    "</head>" +
                    "<body>" +
                    "   <input id=\"color\" type=\"color\" value=\"" + colors.get(0) + "\">" +
                    "</body>");
        });

        Util.sleep(100);
        Util.runAndWait(() -> webView.requestFocus());
        for (int i = 0; i < 2; i++) {
            // the first time, it creates the dialog (fwkCreateAndShowColorChooser),
            // the second time, it reuses it (fwkShowColorChooser)
            Util.sleep(100);
            Util.runAndWait(() -> {
                webView.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 30,
                        15, (int) (scene.getWindow().getX() + scene.getX() + 30),
                        (int) (scene.getWindow().getY() + scene.getY() + 15), MouseButton.PRIMARY, 1,
                        false, false, false, false, true, false, false, true, false, false, null));
                webView.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED, 30,
                        15, (int) (scene.getWindow().getX() + scene.getX() + 30),
                        (int) (scene.getWindow().getY() + scene.getY() + 15), MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, true, false, false, null));
            });
        }
        assertTrue("Timeout when waiting for color chooser ", Util.await(webViewStateLatch));
    }
}
