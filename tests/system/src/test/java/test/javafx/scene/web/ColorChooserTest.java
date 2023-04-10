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

import java.util.List;
import java.util.concurrent.CountDownLatch;

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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.javafx.scene.control.CustomColorDialog;

import test.util.Util;

public class ColorChooserTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    static volatile Stage stage;
    static volatile ColorChooserTestApp colorChooserTestApp;

    @Test
    public void testColorChooser() {
        colorChooserTestApp.startTest();
    }

    @BeforeAll
    public static void setupOnce() {
        Util.launch(launchLatch, ColorChooserTestApp.class);
    }

    @AfterAll
    public static void tearDownOnce() {
        Util.shutdown(stage);
    }

    public static class ColorChooserTestApp extends Application {

        private WebView webView;
        private Scene scene;
        private int counter;
        private final List<String> colors = List.of("#ff3300", "#3367ff", "#cc072d");

        @Override
        public void init() {
            colorChooserTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            stage = primaryStage;

            webView = new WebView();
            scene = new Scene(webView);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.setWidth(300);
            stage.setHeight(300);
            stage.setOnShown(e -> launchLatch.countDown());
            stage.show();
        }

        public void startTest() {
            final CountDownLatch webViewStateLatch = new CountDownLatch(2);

            Util.runAndWait(() -> {
                Assertions.assertNotNull(webView);
                Window.getWindows().addListener((ListChangeListener<Window>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList().stream()
                                    .filter(w -> w.getScene().getRoot() instanceof CustomColorDialog)
                                    .findFirst()
                                    .map(w -> (CustomColorDialog) w.getScene().getRoot())
                                    .ifPresent(dialog -> {
                                        Assertions.assertEquals(dialog.getCurrentColor(), Color.web(colors.get(counter - 1)));
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
                    Assertions.assertNotNull(event.getData());
                    Assertions.assertEquals(event.getData(), "color: " + colors.get(counter++));
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
                            15, (int) (stage.getX() + scene.getX() + 30),
                            (int) (stage.getY() + scene.getY() + 15), MouseButton.PRIMARY, 1,
                            false, false, false, false, true, false, false, true, false, false, null));
                    webView.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED, 30,
                            15, (int) (stage.getX() + scene.getX() + 30),
                            (int) (stage.getY() + scene.getY() + 15), MouseButton.PRIMARY, 1,
                            false, false, false, false, false, false, false, true, false, false, null));
                });
            }
            Assertions.assertTrue(Util.await(webViewStateLatch), "Timeout when waiting for color chooser");
        }
    }

}
