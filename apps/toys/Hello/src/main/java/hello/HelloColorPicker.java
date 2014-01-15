/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloColorPicker extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }
    @Override public void start(Stage stage) {
        stage.setTitle("ColorPicker");
        
        Scene scene = new Scene(new VBox(20), 620, 190);
        VBox box = (VBox)scene.getRoot();
        
        final ColorPicker colorPicker = new ColorPicker();
        // default mode is combobox (above)
        // uncomment the line below for simple button mode
//        colorPicker.getStyleClass().add("button");
        // uncomment the line below for SplitMenuButton mode
//        colorPicker.getStyleClass().add("split-button");
        // Uncomment the line below if you do not wish to see the label next the color.
//        colorPicker.setStyle("-fx-color-label-visible: false;");
        box.getChildren().addAll(colorPicker);
        
        stage.setScene(scene);
        stage.show();
        colorPicker.setOnAction(new EventHandler() {
            public void handle(Event t) {
                Color c = colorPicker.getValue();
                System.out.println("New Color's RGB = "+c.getRed()+" "+c.getGreen()+" "+c.getBlue());
            }
        });
    }
}
