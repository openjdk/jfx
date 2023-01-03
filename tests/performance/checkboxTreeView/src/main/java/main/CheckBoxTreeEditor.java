/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package main;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class CheckBoxTreeEditor extends Application {

    private final TreeView<String> tree = new TreeView<>(new CheckBoxTreeItem<>("root"));
    private int childNum;

    @Override
    public void start(Stage stage) {
        setupTree();
        var borderPane = new BorderPane(tree);
        borderPane.setTop(createToolbar());
        var scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
    }

    private void setupTree() {
        tree.setCellFactory(CheckBoxTreeCell.forTreeView());

        var button = new Button("0");
        tree.getRoot().setGraphic(button);
        tree.getRoot().setExpanded(true);
        tree.getSelectionModel().select(tree.getRoot());

        // add children for initial setup as needed
        addChild(true, true);
//      var c2 = addChild(true, false);
//
//      c1.setSelected(true);
//      c1.setIndeterminate(true);
//
//      c2.setSelected(false);
//      c2.setIndeterminate(true);
//
//      c1.setIndeterminate(false);
//      c2.setIndeterminate(false);
    }

    private Parent createToolbar() {
        var indeterminate = new CheckBox("Indeterminate");
        var selected = new CheckBox("Selected");

        var add = new Button("Add");
        add.setOnAction(e -> addChild(indeterminate.isSelected(), selected.isSelected()));

        var remove = new Button("Remove");
        remove.setOnAction(e -> removeChild());

        var toolbar = new HBox(5, add, remove, indeterminate, selected);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private CheckBoxTreeItem<String> addChild(boolean indeterminate, boolean selected) {
        var item = new CheckBoxTreeItem<>("child " + childNum++);
        var button = new Button("" + childNum);
        item.setGraphic(button);
        item.setSelected(selected);
        item.setIndeterminate(indeterminate);
        item.setExpanded(true);
        tree.getSelectionModel().getSelectedItem().getChildren().add(item);
        return item;
    }

    private void removeChild() {
        var selectedItem = tree.getSelectionModel().getSelectedItem();
        selectedItem.getParent().getChildren().remove(selectedItem);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
