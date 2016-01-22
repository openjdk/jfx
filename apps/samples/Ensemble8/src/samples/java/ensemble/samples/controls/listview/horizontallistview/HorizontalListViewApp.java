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
package ensemble.samples.controls.listview.horizontallistview;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * A sample showing an implementation of the ListView control, in which a list
 * of items is displayed in a horizontal row. ListView is a powerful multi-row
 * control, in which each of a virtually unlimited number of horizontal or
 * vertical rows is defined as a cell. The control also supports dynamically
 * variable non-homogenous row heights.
 *
 * @sampleName HorizontalListView
 * @preview preview.png
 * @see javafx.scene.control.ListView
 * @see javafx.scene.control.SelectionModel
 * @related /Controls/Listview/ListViewCellFactory
 * @related /Controls/Listview/Simple ListView
 * @embedded
 */
public class HorizontalListViewApp extends Application {

    public Parent createContent() {
        Label[] rows = new Label[10];
        for (int i = 0; i < 10; i++) {
            if (i == 2) {
                // A different row
                rows[i] = new Label("Long Row " + (i + 1));
            } else {
                rows[i] = new Label("Row " + (i + 1));
            }
            rows[i].setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        ListView horizontalListView = new ListView();
        horizontalListView.setOrientation(Orientation.HORIZONTAL);
        horizontalListView.setItems(FXCollections.observableArrayList(
                rows[0], rows[1], rows[2], rows[3], rows[4], rows[5],
                rows[6], rows[7], rows[8], rows[9]));
        return horizontalListView;
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
