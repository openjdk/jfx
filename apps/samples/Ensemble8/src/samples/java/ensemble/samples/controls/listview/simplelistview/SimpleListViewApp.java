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
package ensemble.samples.controls.listview.simplelistview;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

/**
 * A simple implementation of the ListView control, in which a list of items is
 * displayed vertically.  ListView is a powerful multirow control, in which each
 * of a virtually unlimited number of horizontal or vertical rows is defined as
 * a cell. The control also supports dynamically variable nonhomogenous row
 * heights.
 *
 * @sampleName Simple ListView
 * @preview preview.png
 * @see javafx.scene.control.ListView
 * @see javafx.scene.control.SelectionModel
 * @related /Controls/Listview/HorizontalListView
 * @related /Controls/Listview/ListViewCellFactory
 * @embedded
 */
public class SimpleListViewApp extends Application {

    public Parent createContent() {
        final ListView<String> listView = new ListView<String>();
        listView.setItems(FXCollections.observableArrayList(
                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20"));
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
