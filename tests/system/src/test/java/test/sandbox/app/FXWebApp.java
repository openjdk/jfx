/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.sandbox.app;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.sun.javafx.scene.control.CustomColorDialog;

import java.util.Objects;

import static test.sandbox.Constants.ERROR_NONE;
import static test.sandbox.Constants.ERROR_NO_SECURITY_EXCEPTION;
import static test.sandbox.Constants.ERROR_SECURITY_EXCEPTION;
import static test.sandbox.Constants.ERROR_UNEXPECTED_EXCEPTION;
import static test.sandbox.Constants.SHOWTIME;

/**
 * FX application to test running with a security manager installed. Note that
 * the toolkit will be initialized by the Java 8 launcher.
 */
public class FXWebApp extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Util.setupTimeoutThread();

        try {
            try {
                // Ensure that we are running with a restrictive
                // security manager
                System.getProperty("sun.something");
                System.err.println("*** Did not get expected security exception");
                System.exit(ERROR_NO_SECURITY_EXCEPTION);
            } catch (SecurityException ex) {
                // This is expected
            }
            Application.launch(args);
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_SECURITY_EXCEPTION);
        } catch (RuntimeException ex) {
            ex.printStackTrace(System.err);
            Throwable cause = ex.getCause();
            if (cause instanceof ExceptionInInitializerError) {
                cause = cause.getCause();
                if (cause instanceof SecurityException) {
                    System.exit(ERROR_SECURITY_EXCEPTION);
                }
            }
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        } catch (Error | Exception t) {
            t.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    private String selectedColor;

    @Override
    public void start(final Stage stage) {
        try {
            WebView webView = new WebView();
            webView.getEngine().setOnAlert(event -> selectedColor = event.getData());
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
                    "</head><body><input id=\"color\" type=\"color\" value=\"#000000\"></body>");
            Scene scene = new Scene(webView, 400, 300);
            stage.setScene(scene);
            stage.setX(0);
            stage.setY(0);
            stage.show();

            // Simulate click to show the ColorChooser dialog after
            // the specified amount of time
            KeyFrame kf0 = new KeyFrame(Duration.millis(500), e -> {

                    webView.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, 20,
                            20, (int) (stage.getX() + scene.getX() + 20),
                            (int) (stage.getY() + scene.getY() + 20), MouseButton.PRIMARY, 1,
                            false, false, false, false, true, false, false, true, false, false, null));
                    webView.fireEvent(new MouseEvent(MouseEvent.MOUSE_RELEASED, 20,
                            20, (int) (stage.getX() + scene.getX() + 20),
                            (int) (stage.getY() + scene.getY() + 20), MouseButton.PRIMARY, 1,
                            false, false, false, false, false, false, false, true, false, false, null));

            });
            // Interact with the ColorChooserDialog window
            KeyFrame kf1 = new KeyFrame(Duration.millis(1000), e -> {
                Window.getWindows().stream()
                        .filter(w -> w.getScene().getRoot() instanceof CustomColorDialog)
                        .findFirst()
                        .map(w -> (CustomColorDialog) w.getScene().getRoot())
                        .ifPresentOrElse(dialog -> {
                            if (Double.isNaN(dialog.getDialog().getMinWidth()) ||
                                    Double.isNaN(dialog.getDialog().getMinHeight())) {
                                // Unexpected, the ColorChooserDialog window should
                                // have valid dimensions
                                System.exit(ERROR_UNEXPECTED_EXCEPTION);
                            }
                            dialog.setCustomColor(Color.web("#ff0000"));
                            HBox box = (HBox) dialog.lookup("#buttons-hbox");
                            Button ok = (Button) box.getChildren().get(0);
                            ok.fire();
                        }, () -> {
                            // Unexpected, there should be a ColorChooserDialog
                            System.exit(ERROR_UNEXPECTED_EXCEPTION);
                        });
            });
            // Hide the stage after the specified amount of time
            KeyFrame kf2 = new KeyFrame(Duration.millis(SHOWTIME), e -> stage.hide());
            Timeline timeline = new Timeline(kf0, kf1, kf2);
            timeline.play();
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_SECURITY_EXCEPTION);
        } catch (Error | Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    @Override public void stop() {
        if (Objects.equals(selectedColor, "color: #ff0000")) {
            System.exit(ERROR_NONE);
        }
        // Unexpected, the color wasn't changed
        System.exit(ERROR_UNEXPECTED_EXCEPTION);
    }

}
