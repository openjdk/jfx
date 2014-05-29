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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.When;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class HelloSimpleListView extends Application {

    public void start(Stage stage) {
        stage.setTitle(getClass().getSimpleName());
        ListView<String> listView = new ListView<>();
        ObservableList<String> list = FXCollections.observableArrayList();
        for (int i=0; i<128; i++) {
            list.add("JavaFX item " + i);
        }
        listView.setItems(list);
        listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                System.out.println("SelectedIndex: " + listView.getSelectionModel().getSelectedIndex());
            }
        });
        listView.setPlaceholder(new Text("place holder for emptyness"));
        ToggleButton button = new ToggleButton("empty");
        button.setOnAction(t-> {
            if (list.size() == 0) {
                for (int i=0; i<128; i++) {
                    list.add("JavaFX item " + i);
                }
            } else {
                list.setAll();
            }
        });
        ToggleButton multi = new ToggleButton("multi");
        listView.getSelectionModel().selectionModeProperty().bind(new When(multi.selectedProperty()).then(SelectionMode.MULTIPLE).otherwise(SelectionMode.SINGLE));
        VBox group = new VBox(listView, button, multi);
        stage.setScene(new Scene(group, 800, 600));
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
