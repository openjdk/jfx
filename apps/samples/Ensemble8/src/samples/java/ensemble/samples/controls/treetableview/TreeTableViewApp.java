/*
 * Copyright (c) 2013, Oracle and/or its affiliates.
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
package ensemble.samples.controls.treetableview;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;

/**
 * A simple implementation of TreeTableView. The Notes column is editable.
 *
 * @sampleName TreeTableView
 * @preview preview.png
 * @see javafx.scene.control.cell.TextFieldTreeTableCell
 * @see javafx.scene.control.cell.TreeItemPropertyValueFactory
 * @see javafx.scene.control.TreeItem
 * @see javafx.scene.control.TreeTableCell
 * @see javafx.scene.control.TreeTableColumn
 * @see javafx.scene.control.TreeTableView
 * @see javafx.beans.property.SimpleStringProperty
 * @see javafx.beans.property.StringProperty
 * @see javafx.beans.property.ObjectProperty
 * @see javafx.beans.property.SimpleObjectProperty
 * @embedded
 */
public class TreeTableViewApp extends Application {

    private TreeItem<Inventory> getData() {
        final TreeItem<Inventory> rootItem = new TreeItem<>(
                new Inventory("Root", new Data("Root data"), ""));
        final TreeItem<Inventory> child1Item = new TreeItem<>(
                new Inventory("Child 1", new Data("Child 1 data"), "My notes"));
        final TreeItem<Inventory> child2Item = new TreeItem<>(
                new Inventory("Child 2", new Data("Child 2 data"), "Notes"));
        TreeItem<Inventory> child3Item = new TreeItem<>(
                new Inventory("Child 3", new Data("Child 3 data"), "Observations"));
        rootItem.setExpanded(true);
        rootItem.getChildren().addAll(child1Item, child2Item);
        child1Item.getChildren().add(child3Item);
        return rootItem;
    }

    public Parent createContent() {

        final TreeTableColumn<Inventory, String> nameColumn = new TreeTableColumn<>("Name");
        nameColumn.setEditable(false);
        nameColumn.setMinWidth(130);
        nameColumn.setCellValueFactory(new TreeItemPropertyValueFactory("name"));

        final TreeTableColumn<Inventory, String> dataColumn = new TreeTableColumn<>("Data");
        dataColumn.setEditable(false);
        dataColumn.setMinWidth(150);
        dataColumn.setCellValueFactory(new TreeItemPropertyValueFactory("data"));

        final TreeTableColumn<Inventory, String> notesColumn = new TreeTableColumn<>("Notes (editable)");
        notesColumn.setEditable(true);
        notesColumn.setMinWidth(150);
        notesColumn.setCellValueFactory(new TreeItemPropertyValueFactory("notes"));
        notesColumn.setCellFactory(TextFieldTreeTableCell.<Inventory>forTreeTableColumn());

        final TreeTableView treeTableView = new TreeTableView(getData());
        treeTableView.setEditable(true);
        treeTableView.setPrefSize(430, 200);
        treeTableView.getColumns().setAll(nameColumn, dataColumn, notesColumn);

        return treeTableView;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("TreeTableViewApp");
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
