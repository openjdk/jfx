/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.FocusModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * TreeView Page.
 */
public class TreeViewPage extends TestPaneBase implements HasSkinnable {
    private final TreeView<Object> control;
    private int seq;

    public TreeViewPage() {
        super("TreeViewPage");

        control = new TreeView<>(new CheckBoxTreeItem<>("root"));
        control.getRoot().setExpanded(true);
        addChild(true, true);

        control.setOnEditCommit((ev) -> {
            TreeItem<Object> item = ev.getTreeItem();
            item.setValue(ev.getNewValue());
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

        OptionPane op = new OptionPane();
        op.section("TreeView");
        op.option("Cell Factory:", createCellFactoryOptions());
        op.option(new BooleanOption("editable", "editable", control.editableProperty()));
        op.option("Fixed Cell Size:", Options.fixedSizeOption("fixedCellSize", control.fixedCellSizeProperty()));
        op.option("Focus Model:", createFocusModelOptions("focusModel", control.focusModelProperty()));
        op.option("Root:", createRootOptions("root", control.rootProperty()));
        op.option(Utils.buttons(addButton, removeButton));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));
        op.option(new BooleanOption("showRoot", "show root", control.showRootProperty()));

        op.separator();
        op.option(indeterminate);
        op.option(selected);

        ControlPropertySheet.appendTo(op, control);
        
        setContent(control);
        setOptions(op);

        //control.getSelectionModel().select(control.getRoot());
    }

    private void addChild(boolean indeterminate, boolean selected) {
        CheckBoxTreeItem<Object> item = new CheckBoxTreeItem<>("child " + seq++);
        item.setSelected(selected);
        item.setIndeterminate(indeterminate);
        item.setExpanded(true);

        if (control.getSelectionModel().getSelectedItem() != null) {
            control.getSelectionModel().getSelectedItem().getChildren().add(item);
        }
    }

    private void removeChild() {
        TreeItem<Object> sel = control.getSelectionModel().getSelectedItem();
        if (sel != null) {
            TreeItem<Object> parent = sel.getParent();
            if (parent != null) {
                parent.getChildren().remove(sel);
            }
        }
    }

    private Node createFocusModelOptions(String name, ObjectProperty<FocusModel<TreeItem<Object>>> p) {
        var original = p.get();
        ObjectOption<FocusModel<TreeItem<Object>>> s = new ObjectOption<>(name, p);
        s.addChoice("<default>", original);
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }

    private Supplier<TreeItem<Object>> mk(int count) {
        return () -> {
            TreeItem<Object> root = new TreeItem<>("ROOT");
            for (int i = 0; i < count; i++) {
                root.getChildren().add(new TreeItem<>(String.valueOf("Item_" + (seq++))));
            }
            return root;
        };
    }

    private Node createRootOptions(String name, ObjectProperty<TreeItem<Object>> p) {
        ObjectOption<TreeItem<Object>> s = new ObjectOption(name, p);
        s.addChoiceSupplier("1 Row", mk(1));
        s.addChoiceSupplier("10 Rows", mk(10));
        s.addChoiceSupplier("1,000 Rows", mk(1_000));
        s.addChoice("<null>", null);
        return s;
    }

    private Node createCellFactoryOptions() {
        var original = control.getCellFactory();
        ObjectOption<Callback> s = new ObjectOption("cellFactory", control.cellFactoryProperty());
        s.addChoice("<default>", original);
        s.addChoiceSupplier("CheckBoxTreeCell", () -> CheckBoxTreeCell.<Object>forTreeView());
        s.addChoiceSupplier("TextFieldTreeCell", () -> TextFieldTreeCell.forTreeView());
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }

    private Node createSelectionModelOptions(String name) {
        var original = control.getSelectionModel();
        ObjectSelector<Boolean> s = new ObjectSelector<>(name, (v) -> {
            control.setSelectionModel(v == null ? null : original);
            original.setSelectionMode(Boolean.TRUE.equals(v) ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        });
        s.addChoice("Single", Boolean.FALSE);
        s.addChoice("Multiple", Boolean.TRUE);
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new TreeViewSkin(control));
    }
}
