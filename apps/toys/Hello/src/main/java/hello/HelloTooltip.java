/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloTooltip extends Application {
  
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        Button button1 = new Button("Cut");
        button1.setTooltip(new Tooltip("Tooltip Button 1"));

        Button button2 = new Button("Copy");
        button2.setTooltip(new Tooltip("Tooltip Button 2"));

        Button button3 = new Button("Paste");
        button3.setTooltip(new Tooltip("Tooltip Button 3"));

        Button button4 = new Button("WrapTooltip");
        Tooltip t = new Tooltip("This is a long tooltip with wrapText set to true; and width set to 80. So should wrap!");
        t.setPrefWidth(80);
        t.setWrapText(true);
        button4.setTooltip(t);

        HBox hbox = new HBox(5);
        hbox.getChildren().addAll(button1, button2, button3, button4);

        Scene scene = new Scene(hbox, 400, 300);
        scene.setFill(Color.CHOCOLATE);
        stage.setScene(scene);
        stage.setTitle("Hello Tooltip");
        stage.show();
    }
}
