/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloColorPicker extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }
    @Override public void start(Stage stage) {
        stage.setTitle("ColorPicker");

        EventHandler<ActionEvent> actionEventHandler = t -> {
            ColorPicker cp = (ColorPicker) t.getTarget();
            Color c = cp.getValue();
            System.out.println("New Color's RGB = "+c.getRed()+" "+c.getGreen()+" "+c.getBlue());
        };

        // default mode combobox
        final ColorPicker normalColorPicker = new ColorPicker();
        normalColorPicker.setOnAction(actionEventHandler);

        // simple button mode
        final ColorPicker buttonColorPicker = new ColorPicker();
        buttonColorPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_BUTTON);
        buttonColorPicker.setOnAction(actionEventHandler);

        // SplitMenuButton mode
        final ColorPicker splitMenuColorPicker = new ColorPicker();
        splitMenuColorPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        splitMenuColorPicker.setOnAction(actionEventHandler);

        // Hidden label mode
        final ColorPicker noLabelColorPicker = new ColorPicker();
        noLabelColorPicker.setStyle("-fx-color-label-visible: false;");
        noLabelColorPicker.setOnAction(actionEventHandler);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Default ColorPicker: "), 1, 1);
        grid.add(normalColorPicker, 2, 1);

        grid.add(new Label("'Button' ColorPicker: "), 1, 2);
        grid.add(buttonColorPicker, 2, 2);

        grid.add(new Label("'SplitButton' ColorPicker: "), 1, 3);
        grid.add(splitMenuColorPicker, 2, 3);

        grid.add(new Label("'Hidden Label' ColorPicker: "), 1, 4);
        grid.add(noLabelColorPicker, 2, 4);

        Scene scene = new Scene(grid, 620, 190);

        stage.setScene(scene);
        stage.show();
    }
}
