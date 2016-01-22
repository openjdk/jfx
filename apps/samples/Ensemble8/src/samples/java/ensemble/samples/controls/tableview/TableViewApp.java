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
package ensemble.samples.controls.tableview;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * A simple table with a header row.
 *
 * @sampleName TableView
 * @preview preview.png
 * @see javafx.scene.control.TableColumn
 * @see javafx.scene.control.TablePosition
 * @see javafx.scene.control.TableRow
 * @see javafx.scene.control.TableView
 * @related /Controls/TableCellFactory
 * @embedded
 */
public class TableViewApp extends Application {

    public Parent createContent() {
        final ObservableList<Person> data = FXCollections.observableArrayList(
            new Person("Jacob",     "Smith",    "jacob.smith@example.com" ),
            new Person("Isabella",  "Johnson",  "isabella.johnson@example.com" ),
            new Person("Ethan",     "Williams", "ethan.williams@example.com" ),
            new Person("Emma",      "Jones",    "emma.jones@example.com" ),
            new Person("Michael",   "Brown",    "michael.brown@example.com" )
        );
        TableColumn firstNameCol = new TableColumn();
        firstNameCol.setText("First");
        firstNameCol.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn lastNameCol = new TableColumn();
        lastNameCol.setText("Last");
        lastNameCol.setCellValueFactory(new PropertyValueFactory("lastName"));
        TableColumn emailCol = new TableColumn();
        emailCol.setText("Email");
        emailCol.setMinWidth(200);
        emailCol.setCellValueFactory(new PropertyValueFactory("email"));
        TableView tableView = new TableView();
        tableView.setItems(data);
        tableView.getColumns().addAll(firstNameCol, lastNameCol, emailCol);
        return tableView;
    }

    @Override public void start(Stage primaryStage) throws Exception {
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
