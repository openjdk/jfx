/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HelloTextField extends Application {
    private double fontSize = Font.getDefault().getSize();
    private Insets insets = new Insets(3, 5, 3, 5);

    @Override public void start(Stage stage) {
        // We have 2 boxes, a TextField and a PasswordField. Then there are a bunch of buttons
        // which cycle various states of these controls, such as the font size, or trying to paste
        // illegal characters, etc.

        final TextField textField = new TextField();
        final PasswordField passwordField = new PasswordField();

        Button setIllegalCharsBtn = new Button("Set Illegal Characters");
        setIllegalCharsBtn.setOnAction(event -> textField.setText("Illegal characters here -->" + '\05' + '\07' + '\0' + "<--"));

        Button increasePrefColumnCountBtn = new Button("Increase prefColumnCount");
        increasePrefColumnCountBtn.setOnAction(event -> {
            textField.setPrefColumnCount(textField.getPrefColumnCount() + 5);
            passwordField.setPrefColumnCount(passwordField.getPrefColumnCount() + 5);
        });

        Button decreasePrefColumnCountBtn = new Button("Decrease prefColumnCount");
        decreasePrefColumnCountBtn.setOnAction(event -> {
            textField.setPrefColumnCount(textField.getPrefColumnCount() - 5);
            passwordField.setPrefColumnCount(passwordField.getPrefColumnCount() - 5);
        });

        Button increaseFontSizeBtn = new Button("Increase Font Size");
        increaseFontSizeBtn.setOnAction(event -> {
            fontSize += 1;
            textField.setStyle("-fx-font-size: " + fontSize + "pt");
            passwordField.setStyle("-fx-font-size: " + fontSize + "pt");
        });

        Button decreaseFontSizeBtn = new Button("Decrease Font Size");
        decreaseFontSizeBtn.setOnAction(event -> {
            fontSize -= 1;
            textField.setStyle("-fx-font-size: " + fontSize + "pt");
            passwordField.setStyle("-fx-font-size: " + fontSize + "pt");
        });

        Button defaultBtn = new Button("Default Action");
        defaultBtn.setDefaultButton(true);
        defaultBtn.setOnAction(event -> System.out.println("Default Action"));

        VBox fieldBox = new VBox(5);
        fieldBox.setFillWidth(false);
        fieldBox.getChildren().addAll(textField, passwordField);

        VBox buttonBox = new VBox(5);
        buttonBox.getChildren().addAll(
                setIllegalCharsBtn,
                increasePrefColumnCountBtn,
                decreasePrefColumnCountBtn,
                increaseFontSizeBtn,
                decreaseFontSizeBtn,
                defaultBtn);

        HBox root = new HBox(7);
        root.setPadding(new Insets(11, 12, 12, 11));
        root.getChildren().addAll(fieldBox, buttonBox);
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
