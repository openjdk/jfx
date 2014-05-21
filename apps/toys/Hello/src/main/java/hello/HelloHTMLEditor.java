/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;

public class HelloHTMLEditor extends Application {
    private HTMLEditor htmlEditor = null;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Hello HTMLEditor");
        stage.setWidth(800);
        stage.setHeight(600);
        Scene scene = new Scene(new Group());
        scene.setFill(Color.GHOSTWHITE);

        FlowPane root = new FlowPane();
        root.setOrientation(Orientation.VERTICAL);
        scene.setRoot(root);

        root.setPadding(new Insets(8, 8, 8, 8));
        root.setVgap(8);

        htmlEditor = new HTMLEditor();
        root.getChildren().add(htmlEditor);

        Button dumpHTMLButton = new Button("Dump HTML");
        dumpHTMLButton.setOnAction(arg0 -> System.out.println(htmlEditor.getHtmlText()));

        root.getChildren().add(dumpHTMLButton);

        htmlEditor.setHtmlText("<html><body>Hello, World!</body></html>");

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(HelloHTMLEditor.class, args);
    }
}
