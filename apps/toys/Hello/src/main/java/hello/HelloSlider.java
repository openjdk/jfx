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
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloSlider extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Slider");
        
        VBox vbox = new VBox(4);
        Slider slider = new Slider();
        vbox.getChildren().add(slider);
        
        slider = new Slider();
        slider.setBlockIncrement(30);
        vbox.getChildren().add(slider);
        
        slider = new Slider();
        slider.setSnapToTicks(true);
        vbox.getChildren().add(slider);
        
        slider = new Slider();
        slider.setShowTickMarks(true);
        vbox.getChildren().add(slider);

        slider = new Slider(0, 1.0, 1.0);
        slider.setBlockIncrement(0.2d);
        slider.setMajorTickUnit(0.2d);
        slider.setMinorTickCount(3);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);

        ImageView view = new ImageView(new Image("hello/duke_with_guitar.png"));
        view.opacityProperty().bind(slider.valueProperty());
        vbox.getChildren().add(view);
        vbox.getChildren().add(slider);

        slider = new Slider(0, 180.0, 0.0);
        slider.setBlockIncrement(10d);
        slider.setMajorTickUnit(30d);
        slider.setMinorTickCount(10);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        view.rotateProperty().bind(slider.valueProperty());
        vbox.getChildren().add(slider);
        
        HBox hbox = new HBox(5);
        hbox.getChildren().add(vbox);
        
        slider = new Slider(0, 300, 0.0);
        slider.setOrientation(Orientation.VERTICAL);
        slider.setBlockIncrement(10d);
        slider.setMajorTickUnit(30d);
        slider.setMinorTickCount(10);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        view.fitWidthProperty().bind(slider.valueProperty());
        hbox.getChildren().add(slider);
        
        slider = new Slider(0, 256, 0.0);
        slider.setOrientation(Orientation.VERTICAL);
        slider.setBlockIncrement(10d);
        slider.setMajorTickUnit(30d);
        slider.setMinorTickCount(10);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        view.fitHeightProperty().bind(slider.valueProperty());
        hbox.getChildren().add(slider);
        

        Scene scene = new Scene(hbox);
        stage.setScene(scene);
        stage.show();
    }
}
