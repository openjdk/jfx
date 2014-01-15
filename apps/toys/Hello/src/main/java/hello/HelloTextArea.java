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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloTextArea extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Hello TextArea");
        stage.setWidth(800);
        stage.setHeight(600);
        Scene scene = new Scene(new Group());
        scene.setFill(Color.GHOSTWHITE);

        FlowPane root = new FlowPane();
        root.setOrientation(Orientation.VERTICAL);
        scene.setRoot(root);

        root.setPadding(new Insets(8, 8, 8, 8));
        root.setVgap(8);

        TextField textField = new TextField("abcdefghijklmnop");
        textField.setPrefColumnCount(8);
        root.getChildren().add(textField);

        TextArea textArea = new TextArea();
        textArea.setPrefColumnCount(24);
        root.getChildren().add(textArea);

        textArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                // System.out.println(newValue);
            }
        });

        ToggleButton wrapButton = new ToggleButton("Wrap Text");
        textArea.wrapTextProperty().bind(wrapButton.selectedProperty());
        wrapButton.setSelected(true);
        root.getChildren().add(wrapButton);

        ToggleButton editableButton = new ToggleButton("Editable");
        textArea.editableProperty().bind(editableButton.selectedProperty());
        editableButton.setSelected(true);
        root.getChildren().add(editableButton);

        wrapButton.requestFocus();

        for (int i = 0; i < 20; i++) {
            textArea.insertText(textArea.getLength(), i + ". Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n");
        }
        textArea.selectRange(0, 0);

        stage.setScene(scene);
        stage.show();

        textArea.requestFocus();

        /*
        textArea.setSelection(0, 3);
        */
    }

    public static void main(String[] args) {
        Application.launch(HelloTextArea.class, args);
    }
}
