/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.controls.listview.listviewcellfactory;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * A simple implementation of the ListView control that uses a CellFactory to
 * customize the ListView cell contents. Positive values in the list are green,
 * and negative values are red and enclosed in parentheses. Zero values are black.
 *
 * @sampleName ListViewCellFactory
 * @preview preview.png
 * @see javafx.scene.control.ListView
 * @see javafx.scene.control.SelectionModel
 * @related /Controls/Listview/HorizontalListView
 * @related /Controls/Listview/Simple ListView
 * @embedded
 */
public class ListViewCellFactoryApp extends Application {

    public Parent createContent() {
         final ListView<Number> listView = new ListView<>();
        listView.setItems(FXCollections.<Number>observableArrayList(
                100.00, -12.34, 33.01, 71.00, 23000.00, -6.00, 0, 42223.00, -12.05, 500.00,
                430000.00, 1.00, -4.00, 1922.01, -90.00, 11111.00, 3901349.00, 12.00, -1.00, -2.00,
                15.00, 47.50, 12.11

        ));

        listView.setCellFactory((ListView<java.lang.Number> list) -> new MoneyFormatCell());

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        return listView;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
