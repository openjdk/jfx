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
package com.oracle.tools.fx.monkey.pages;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Test code from CheckBoxTreeEditor, see https://bugs.openjdk.org/browse/JDK-8209017
 */
public class TreeViewPage extends TestPaneBase {
    private enum Cells {
        DEFAULT,
        EDITABLE_TEXT_FIELD,
    }
    
    private final TreeView<String> control;
    private final CheckBox editable;
    private final ComboBox<Cells> cellFactorySelector;
    private int childNum;
    private Callback<TreeView<String>, TreeCell<String>> defaultCellFactory;

    public TreeViewPage() {
        FX.name(this, "TreeViewPage");
        
        control = new TreeView<>(new CheckBoxTreeItem<>("root"));
        control.getRoot().setExpanded(true);
        control.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        control.setTooltip(new Tooltip("edit to 'update' to commit the change"));
        addChild(true, true);
        setContent(control);
        defaultCellFactory = control.getCellFactory();
        control.setOnEditCommit((ev) -> {
            if ("update".equals(ev.getNewValue())) {
                TreeItem<String> item = ev.getTreeItem();
                item.setValue("UPDATED!");
                System.out.println("committing the value `UPDATED!`");
            } else {
                System.out.println("discarding the new value: " + ev.getNewValue());
            }
        });

        CheckBox indeterminate = new CheckBox("Indeterminate");
        FX.name(indeterminate, "indeterminate");

        CheckBox selected = new CheckBox("Selected");
        FX.name(selected, "selected");

        Button addButton = new Button("Add");
        addButton.setOnAction((ev) -> {
            addChild(indeterminate.isSelected(), selected.isSelected());
        });

        Button removeButton = new Button("Remove");
        removeButton.setOnAction((ev) -> {
            removeChild();
        });
        
        editable = new CheckBox("editable");
        editable.setOnAction((ev) -> {
            updateEditable();
        });
        FX.name(editable, "editable");

        cellFactorySelector = new ComboBox<>();
        FX.name(cellFactorySelector, "cellSelector");
        cellFactorySelector.getItems().addAll(Cells.values());
        cellFactorySelector.setEditable(false);
        cellFactorySelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateCellFactory();
        });

        OptionPane op = new OptionPane();
        op.option(addButton);
        op.option(indeterminate);
        op.option(selected);
        op.option(removeButton);
        op.option(editable);
        op.label("Cell Factory:");
        op.option(cellFactorySelector);
        setOptions(op);

        control.getSelectionModel().select(control.getRoot());
        FX.selectFirst(cellFactorySelector);
    }

    protected void updateEditable() {
        boolean on = editable.isSelected();
        control.setEditable(on);
        if (on) {
            cellFactorySelector.getSelectionModel().select(Cells.EDITABLE_TEXT_FIELD);
        }
    }

    protected void updateCellFactory() {
        Cells t = cellFactorySelector.getSelectionModel().getSelectedItem();
        var f = getCellFactory(t);
        control.setCellFactory(f);
    }

    private Callback<TreeView<String>, TreeCell<String>> getCellFactory(Cells t) {
        if (t != null) {
            switch (t) {
            case EDITABLE_TEXT_FIELD:
                return TextFieldTreeCell.forTreeView();
            }
        }
        return defaultCellFactory;
    }

    private void addChild(boolean indeterminate, boolean selected) {
        CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>("child " + childNum++);
        item.setSelected(selected);
        item.setIndeterminate(indeterminate);
        item.setExpanded(true);

        if (control.getSelectionModel().getSelectedItem() != null) {
            control.getSelectionModel().getSelectedItem().getChildren().add(item);
        }
    }

    private void removeChild() {
        TreeItem<String> sel = control.getSelectionModel().getSelectedItem();
        if (sel != null) {
            TreeItem<String> parent = sel.getParent();
            if (parent != null) {
                parent.getChildren().remove(sel);
            }
        }
    }
}
