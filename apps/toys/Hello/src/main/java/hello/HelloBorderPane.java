/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class HelloBorderPane extends Application {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {

        stage.setTitle("Hello BorderPane");

        BorderPane borderpane = new BorderPane();
        borderpane.setPadding(new Insets(10,10,10,10));
        ToolBar toolbar = new ToolBar();
        toolbar.getItems().addAll(new Button("Insert"), new Button("Delete"));
        borderpane.setTop(toolbar);

        FlowPane flow = new FlowPane();
        flow.setHgap(4);
        flow.setVgap(10);
        flow.setPrefWrapLength(400);

        InnerShadow shadow = new InnerShadow();
        for (int r = 70; r > 3; r -= 4) {
            Circle circle = new Circle();
            circle.setEffect(shadow);
            circle.setRadius(r);
            circle.setFill(Color.RED);
            flow.getChildren().add(circle);
        }

        borderpane.setCenter(flow);

        borderpane.setBottom(new Label("My status is idle right now"));
        borderpane.setLeft(new Separator());
        borderpane.setRight(new Separator());

        Scene scene = new Scene(borderpane, 500, 500);
        stage.setScene(scene);
        stage.show();
    }
}
