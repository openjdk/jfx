/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package a11y;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloToolBar extends Application {
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("ToolBar");
        final VBox box = new VBox(10);
        final Scene scene = new Scene(box, 500, 500);

        final ToolBar tb = new ToolBar();
        tb.setOrientation(Orientation.HORIZONTAL);
        for (int i=0; i< 12; i++) {
            tb.getItems().add(new Button("button " + i));
        }

        final ToolBar tb2 = new ToolBar();
        tb2.setOrientation(Orientation.VERTICAL);
        for (int i=0; i< 12; i++) {
            tb2.getItems().add(new Button("button " + i));
        }

        box.getChildren().add(tb);

        HBox hbox = new HBox();
        hbox.getChildren().add(tb2);
        box.getChildren().add(hbox);

        stage.setScene(scene);
        stage.show();
    }
}
