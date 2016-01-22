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
package ensemble.samples.language.concurrency.service;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A sample showing use of a Service to retrieve data in a background thread.
 * Selecting the Refresh button restarts the Service.
 *
 * @sampleName Service
 * @preview preview.png
 * @see javafx.collections.FXCollections
 * @see javafx.concurrent.Service
 * @see javafx.concurrent.Task
 * @see javafx.scene.control.ProgressIndicator
 * @see javafx.scene.control.TableColumn
 * @see javafx.scene.control.TableView
 * @embedded
 */
public class ServiceApp extends Application {

 final GetDailySalesService service = new GetDailySalesService();

    public Parent createContent() {
       VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(12));
        TableView tableView = new TableView();
        Button button = new Button("Refresh");
        button.setOnAction((ActionEvent t) -> {
            service.restart();
       });
        vbox.setPrefHeight(160);
        vbox.getChildren().addAll(tableView, button);

        Region veil = new Region();
        veil.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        veil.setPrefSize(240, 160);
        ProgressIndicator p = new ProgressIndicator();
        p.setMaxSize(140, 140);

        //Define table columns
        TableColumn idCol = new TableColumn();
        idCol.setText("ID");
        idCol.setCellValueFactory(new PropertyValueFactory("dailySalesId"));
        idCol.setPrefWidth(32);
        tableView.getColumns().add(idCol);
        TableColumn qtyCol = new TableColumn();
        qtyCol.setText("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory("quantity"));
        qtyCol.setPrefWidth(60);
        tableView.getColumns().add(qtyCol);
        TableColumn dateCol = new TableColumn();
        dateCol.setText("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory("date"));
        dateCol.setMinWidth(240);
        tableView.getColumns().add(dateCol);

        p.progressProperty().bind(service.progressProperty());
        veil.visibleProperty().bind(service.runningProperty());
        p.visibleProperty().bind(service.runningProperty());
        tableView.itemsProperty().bind(service.valueProperty());
        tableView.setMinSize(240, 140);
        StackPane stack = new StackPane();
        stack.getChildren().addAll(vbox, veil, p);

        service.start();
        return stack;
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
