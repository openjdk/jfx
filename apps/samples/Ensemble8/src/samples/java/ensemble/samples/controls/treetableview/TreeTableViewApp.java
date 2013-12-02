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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

/**
 * A simple implementation of TreeTableView. The Notes column is editable.
 *
 * @sampleName TreeTableView
 * @preview preview.png
 * @see javafx.scene.control.cell.TextFieldTreeTableCell
 * @see javafx.scene.control.TreeItem
 * @see javafx.scene.control.TreeTableCell
 * @see javafx.scene.control.TreeTableColumn
 * @see javafx.scene.control.TreeTableView
 * @embedded
 */
public class TreeTableViewApp extends Application {

    public Parent createContent() {

        final TreeItem<Inventory> rootItem = new TreeItem<>(new Inventory(false, "Root", "Data", "Data2", new Part("data 1", "part 1", "part 2")));

        final TreeItem<Inventory> child1Item = new TreeItem<>(new Inventory(true, "Child 1", "Data 1", "My notes", new Part("my data", "part 1", "part 2")));
        final TreeItem<Inventory> child2Item = new TreeItem<>(new Inventory(false, "Child 2", "Data", "Notes", new Part("no, my data", "part 3", "part 4")));
        TreeItem<Inventory> child3Item = new TreeItem<>(new Inventory(false, "Child 3", "Data 3", "Observations", new Part("even I have data", "part 3", "part 4")));

        rootItem.setExpanded(true);
        rootItem.getChildren().addAll(child1Item, child2Item);
        child1Item.getChildren().add(child3Item);

        // Name column
        final TreeTableColumn<Inventory, String> nameColumn = new TreeTableColumn<>("Name");
        nameColumn.setEditable(false);
        nameColumn.setMinWidth(130);
        nameColumn.setCellValueFactory(
                new Callback<TreeTableColumn.CellDataFeatures<Inventory, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Inventory, String> p) {
                Inventory inv = p.getValue().getValue();
                return new ReadOnlyObjectWrapper(inv.nameProperty().getValue());
            }
        });

        // Data column
        final TreeTableColumn<Inventory, String> dataColumn = new TreeTableColumn<>("Data");
        dataColumn.setEditable(true);
        dataColumn.setMinWidth(150);
        dataColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Inventory, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Inventory, String> p) {
                final Inventory value = p.getValue().getValue();
                if (value.equals(rootItem.getValue())) {
                    return new ReadOnlyStringWrapper(" ");
                } else {
                    return new ReadOnlyStringWrapper(value.ob1Property().getValue().dataProperty().getValue());
                }
            }
        });

        // Note column
        final TreeTableColumn<Inventory, String> noteColumn = new TreeTableColumn<>("Notes (editable)");
        noteColumn.setEditable(true);
        noteColumn.setMinWidth(150);
        noteColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Inventory, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Inventory, String> p) {
                final Inventory value = p.getValue().getValue();
                if (value.equals(rootItem.getValue())) {
                    return new ReadOnlyStringWrapper(" ");
                } else {
                    return new ReadOnlyStringWrapper(value.p2Property().getValue());
                }
            }
        });
        noteColumn.setOnEditCommit(new EventHandler<TreeTableColumn.CellEditEvent<Inventory, String>>() {
            @Override
            public void handle(TreeTableColumn.CellEditEvent<Inventory, String> t) {
                System.out.println("Note column entry was edited. Old value = " + t.getOldValue() + " New value = " + t.getNewValue());
            }
        });
        noteColumn.setCellFactory(new Callback<TreeTableColumn<Inventory, String>, TreeTableCell<Inventory, String>>() {
            @Override
            public TreeTableCell<Inventory, String> call(TreeTableColumn<Inventory, String> p) {
                return new TextFieldTreeTableCell(new DefaultStringConverter());
            }
        });

        final TreeTableView treeTableView = new TreeTableView(rootItem);
        treeTableView.setEditable(true);
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.setPrefSize(430, 200);
        treeTableView.setLayoutX(10);
        treeTableView.setLayoutY(10);
        treeTableView.getColumns().setAll(nameColumn, dataColumn, noteColumn);

        return treeTableView;
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
