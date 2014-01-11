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
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloControls extends Application {
    private static String[] hellos = new String[] {
        "Hello World",
        "привет мир",
        "Hola Mundo"
    };

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Controls");
        Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.CHOCOLATE);
        int offset = 0;
        for(String hello : hellos) {
            offset += 40;
            Button button = new Button();
            button.setLayoutX(25);
            button.setLayoutY(offset);
            button.setText(hello);
            ((Group)scene.getRoot()).getChildren().add(button);
        }
        stage.setScene(scene);
        stage.show();

        // add a slider
        offset += 40;
        Slider slider = new Slider();
        slider.setLayoutX(25);
        slider.setLayoutY(offset);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);

        ((Group)scene.getRoot()).getChildren().add(slider);

        // add a vertical slider
        offset += 40;
        Slider vslider = new Slider();
        vslider.setOrientation(Orientation.VERTICAL);
        vslider.setLayoutX(25);
        vslider.setLayoutY(offset);
        ((Group)scene.getRoot()).getChildren().add(vslider);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
