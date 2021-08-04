/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.launchertest;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import static test.launchertest.Constants.*;

/**
 * Test application that calls Platform.exit while Stage is still showing
 * the Scene.
 *
 * This is launched by PlatformExitTest.
 */
public class PlatformExitApp extends Application {

    // Timeout in milliseconds (must be at least 15 seconds)
    private static final int TIMEOUT = 20000;

    public static void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    public static void setupTimeoutThread() {
        // Timeout thread
        Thread th = new Thread(() -> {
            sleep(TIMEOUT);
            System.exit(ERROR_TIMEOUT);
        });
        th.setDaemon(true);
        th.start();
    }

    @Override public void start(Stage stage) throws Exception {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 400, 300);

        final Label label = new Label("Hello");

        root.getChildren().add(label);

        stage.setScene(scene);
        stage.show();

        // Show window for 1 second before calling Platform.exit
        Thread thr = new Thread(() -> {
            sleep(1000);
            Platform.exit();
        });
        thr.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        setupTimeoutThread();
        Application.launch(args);

        // Short delay to allow any pending output to be flushed
        sleep(500);
        System.exit(ERROR_NONE);
    }

}
