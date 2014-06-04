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
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloSimpleCheckBox extends Application {
        
    @Override public void start(Stage stage) {
        CheckBox cbox = new CheckBox("Choose this item");
        cbox.setIndeterminate(true);
        cbox.setAllowIndeterminate(true);

        Label label = new Label();
        label.textProperty().bind(
                Bindings.when(cbox.indeterminateProperty()).
                        then("The check box is indeterminate").
                        otherwise(
                                Bindings.when(cbox.selectedProperty()).
                                        then("The check box is selected").
                                        otherwise("The check box is not selected"))
        );

        VBox vbox = new VBox(7);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(label, cbox, new Button("OK"));
        
        Scene scene = new Scene(vbox, 400, 400);
        scene.setFill(Color.SKYBLUE);
        stage.setTitle("Hello CheckBox");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
