/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 */
public class HelloTask extends Application {
    private ProgressBar bar = new ProgressBar();
    private Label label = new Label();

    @Override
    public void init() throws Exception {
        Task<String> task = new Task<String>() {
            @Override protected String call() throws Exception {
                for (int i=0; i<100; i++) {
                    Thread.sleep(100);
                    updateProgress(i, 99);
                }
                return "Finished!";
            }
        };
        bar.progressProperty().bind(task.progressProperty());
        label.textProperty().bind(task.valueProperty());
        Thread th = new Thread(task);
        th.setDaemon(false);
        th.start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = new VBox(15, bar, label);
        root.setAlignment(Pos.CENTER);
        Scene s = new Scene(root, 640, 480);
        stage.setScene(s);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
