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

package testapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Modular test application with main method.
 * This is launched by ModuleLauncherTest.
 */
public class TestApp extends Application {

    private static final int SHOWTIME = 2500;

    // NOTE: these constants must match those in test.launchertest.Constants
    private static final int ERROR_NONE = 2;
    private static final int ERROR_TOOLKIT_NOT_RUNNING = 3;
    private static final int ERROR_UNEXPECTED_EXCEPTION = 4;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Application.launch(args);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    public static Parent createGraph() {
        Label label = new Label("JavaFX Modular App Test");
        label.setStyle("-fx-font-size: 24; -fx-text-fill: orange");
        StackPane root = new StackPane();
        root.getChildren().add(label);
        return root;
    }

    @Override
    public void start(final Stage stage) {
        try {
            stage.setTitle("JavaFX Modular App");
            Scene scene = new Scene(createGraph(), 400, 300);
            stage.setScene(scene);
            stage.setX(0);
            stage.setY(0);
            stage.show();

            // Hide the stage after the specified amount of time
            KeyFrame kf = new KeyFrame(Duration.millis(SHOWTIME), e -> stage.hide());
            Timeline timeline = new Timeline(kf);
            timeline.play();
        } catch (Error | Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    @Override public void stop() {
        System.exit(ERROR_NONE);
    }

    static {
        try {
            Platform.runLater(() -> {
                // do nothing
            });
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            System.exit(ERROR_TOOLKIT_NOT_RUNNING);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

}
