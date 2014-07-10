/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class HelloComboBox extends Application {
    
    private final ObservableList<String> strings = FXCollections.observableArrayList(
            "Option 1", "Option 2", "Option 3", 
            "Option 4", "Option 5", "Option 6",
            "Long ComboBox item 1 2 3 4 5 6 7 8 9",
            "Option 7", "Option 8", "Option 9", "Option 10", "Option 12", "Option 13",
            "Option 14", "Option 15", "Option 16", "Option 17", "Option 18", "Option 19",
            "Option 20", "Option 21", "Option 22", "Option 23", "Option 24", "Option 25",
            "Option 26", "Option 27", "Option 28", "Option 29", "Option 30", "Option 31",
            "Option 32", "Option 33", "Option 34", "Option 35", "Option 36", "Option 37",
            "Option 38", "Option 39", "Option 40", "Option 41", "Option 42", "Option 43",
            "Option 44", "Option 45", "Option 46", "Option 47", "Option 48", "Option 49",
            "Option 50", "Option 51", "Option 52", "Option 53", "Option 54", "Option 55",
            "Option 56", "Option 57", "Option 58", "Option 59", "Option 60", "Option 61",
            "Option 62", "Option 63", "Option 64", "Option 65", "Option 66", "Option 67",
            "Option 68", "Option 69", "Option 70", "Option 71", "Option 72", "Option 73",
            "Option 74", "Option 75"
        );
    
    private final ObservableList<String> fonts = FXCollections.observableArrayList(Font.getFamilies());

    public static void main(String[] args) {
        launch(args);
    }
    
    @Override public void start(Stage stage) {
        stage.setTitle("ComboBox");
        
        // non-editable column
        VBox nonEditBox = new VBox(15);
        
        ComboBox emptyPromptComboBox = new ComboBox();
        emptyPromptComboBox.setPromptText("Prompting you here");
        emptyPromptComboBox.setPlaceholder(new Label("There are no options available!"));
        nonEditBox.getChildren().add(emptyPromptComboBox);
        
        ComboBox shortComboBox = new ComboBox();
        shortComboBox.setItems(FXCollections.observableArrayList(strings.subList(0, 4)));
        nonEditBox.getChildren().add(shortComboBox);
        
        ComboBox longComboBox = new ComboBox();
        longComboBox.setPromptText("Make a choice...");
        longComboBox.setItems(strings);
        nonEditBox.getChildren().add(longComboBox);
        
        ComboBox fontComboBox = new ComboBox();
        fontComboBox.setItems(fonts);
        fontComboBox.setCellFactory(param -> {
            final ListCell<String> cell = new ListCell<String>() {
                @Override public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item);
                        setFont(new Font(item, 14));
                    }
                }
            };
            return cell;
        });
        nonEditBox.getChildren().add(fontComboBox);
        
//        ColorPicker colorPicker = new ColorPicker();
//        longComboBox.setItems(strings);
//        nonEditBox.getChildren().add(colorPicker);
        
        // editable column
        VBox editBox = new VBox(15);
        
        ComboBox comboBox2 = new ComboBox();
        comboBox2.setId("first-editable");
        comboBox2.setItems(FXCollections.observableArrayList(strings.subList(0, 4)));
        comboBox2.setEditable(true);
        editBox.getChildren().add(comboBox2);
        
        ComboBox<String> comboBox3 = new ComboBox<String>();
        comboBox3.setId("second-editable");
        comboBox3.setPromptText("Make a choice...");
        comboBox3.setItems(strings);
        comboBox3.setEditable(true);
        editBox.getChildren().add(comboBox3);
        comboBox3.valueProperty().addListener((ov, t, t1) -> System.out.println("new value: " + t1));
        
        ComboBox editFontComboBox = new ComboBox();
        editFontComboBox.setId("third-editable");
        editFontComboBox.setItems(fonts);
        editFontComboBox.setEditable(true);
        editFontComboBox.setCellFactory(param -> {
            final ListCell<String> cell = new ListCell<String>() {
                @Override public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item);
                        setFont(new Font(item, 14));
                    }
                }
            };
            return cell;
        });
        editBox.getChildren().add(editFontComboBox);
        
        
        HBox vbox = new HBox(20);
        vbox.setLayoutX(40);
        vbox.setLayoutY(25);
        
        vbox.getChildren().addAll(nonEditBox, editBox);
        Scene scene = new Scene(new Group(vbox), 620, 190);

        stage.setScene(scene);
        stage.show();
        
//        scene.impl_focusOwnerProperty().addListener(new ChangeListener<Node>() {
//            public void changed(ObservableValue<? extends Node> ov, Node t, Node t1) {
//                System.out.println("focus moved from " + t + " to " + t1);
//            }
//        });
    }
}
