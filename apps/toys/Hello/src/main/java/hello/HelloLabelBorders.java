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

import com.sun.javafx.css.Rule;
import com.sun.javafx.css.Selector;
import com.sun.javafx.css.SimpleSelector;
import com.sun.javafx.css.Stylesheet;
import com.sun.javafx.css.parser.CSSParser;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Different stroke and image borders around a Label
 */
public class HelloLabelBorders  extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(HelloLabelBorders.class, args);
    }

    @Override
    public void start(Stage primaryStage) {

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 400, 600);

        root.getChildren().add(new Label("no-border"));

        try {
            URL url = HelloLabelBorders.class.getResource("LabelBorders.css");
            System.out.println(url.toExternalForm());

            scene.getStylesheets().add(url.toExternalForm());

            Stylesheet ss = CSSParser.getInstance().parse(url);
            for (Rule rule : ss.getRules()) {
                for(Selector selector : rule.getSelectors()) {
                    if (selector instanceof SimpleSelector) {

                        SimpleSelector simpleSelector = (SimpleSelector)selector;
                        String id = simpleSelector.getId();

                        System.out.println("add Label \"" + id + "\"");

                        Label lbl = new Label(id);
                        lbl.setId(id);
                        root.getChildren().add(lbl);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        primaryStage.setScene(scene);
        primaryStage.show();

    }
}
