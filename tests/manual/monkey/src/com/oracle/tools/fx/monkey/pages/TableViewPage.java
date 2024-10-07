/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ConstrainedColumnResizeBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.sheets.TableColumnPropertySheet;
import com.oracle.tools.fx.monkey.util.ColumnBuilder;
import com.oracle.tools.fx.monkey.util.DataRow;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * TableView Page.
 */
public class TableViewPage extends TestPaneBase implements HasSkinnable {
    private final TableView<DataRow> control;

    public TableViewPage() {
        super("TableViewPage");

        control = new TableView<>() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        Button addDataItemButton = FX.button("Add Data Item", () -> {
            control.getItems().add(new DataRow());
        });

        Button clearDataItemsButton = FX.button("Clear Data Items", () -> {
            control.getItems().clear();
        });

        Button refresh = FX.button("Refresh", () -> {
            control.refresh();
        });

        OptionPane op = new OptionPane();
        op.section("TableView");
        op.option("Columns:", createColumnsSelector("columns", control.getColumns()));
        op.option("Column Resize Policy:", createColumnResizePolicy("columnResizePolicy", control.columnResizePolicyProperty()));
        op.option(new BooleanOption("editable", "editable", control.editableProperty()));
        op.option("Fixed Cell Size:", Options.fixedSizeOption("fixedCellSize", control.fixedCellSizeProperty()));
        op.option("Focus Model:", createFocusModelOptions("focusModel", control.focusModelProperty()));
        op.option("Items:", createItemsOptions("items", control.getItems()));
        op.option(Utils.buttons(addDataItemButton, clearDataItemsButton));
        op.option("Placeholder:", Options.placeholderNode("placeholder", control.placeholderProperty()));
        op.option("Row Factory:", createRowFactoryOptions("rowFactory", control.rowFactoryProperty()));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));
        op.option("Sort Policy: TODO", null); // TODO
        op.option(new BooleanOption("tableMenuButtonVisible", "table menu button visible", control.tableMenuButtonVisibleProperty()));
        op.separator();
        op.option(refresh);
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private ContextMenu createPopupMenu(TableColumn<?,?> tc) {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Add Column Before", () -> addColumn(tc, false));
        FX.item(m, "Add Column After", () -> addColumn(tc, true));
        FX.separator(m);
        FX.item(m, "Remove Column", () -> control.getColumns().remove(tc));
        FX.item(m, "Remove All Columns", () -> control.getColumns().clear());
        FX.separator(m);
        FX.item(m, "Properties...", () -> TableColumnPropertySheet.open(this, tc));
        return m;
    }

    private TableColumn<DataRow, Object> newColumn() {
        TableColumn<DataRow, Object> tc = new TableColumn();
        tc.setCellFactory(TextFieldTableCell.<DataRow, Object>forTableColumn(DataRow.converter()));
        tc.setCellValueFactory((cdf) -> {
            Object v = cdf.getValue();
            if (v instanceof DataRow r) {
                return r.getValue(tc);
            }
            return new SimpleObjectProperty(v);
        });
        tc.setContextMenu(createPopupMenu(tc));
        return tc;
    }

    private void addColumn(TableColumn<?, ?> ref, boolean after) {
        int ix = control.getColumns().indexOf(ref);
        if (ix < 0) {
            return;
        }
        if (after) {
            ix++;
        }

        TableColumn<DataRow, Object> c = newColumn();
        c.setText("C" + System.currentTimeMillis());
        control.getColumns().add(ix, c);
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new TableViewSkin<>(control));
    }

    /**
     * a user-defined policy demonstrates that we can indeed create a custom policy using the new API.
     * this policy simply sizes all columns equally.
     */
    static class UserDefinedResizePolicy
        extends ConstrainedColumnResizeBase
        implements Callback<TableView.ResizeFeatures, Boolean>
    {
        @SuppressWarnings("unchecked")
        @Override
        public Boolean call(ResizeFeatures rf) {
            List<? extends TableColumnBase<?, ?>> visibleLeafColumns = rf.getTable().getVisibleLeafColumns();
            int sz = visibleLeafColumns.size();
            // using added public method getContentWidth()
            double w = rf.getContentWidth() / sz;
            for (TableColumnBase<?, ?> c: visibleLeafColumns) {
                // using added public method setColumnWidth()
                rf.setColumnWidth(c, w);
            }
            return false;
        }
    }

    private ColumnBuilder<TableColumn<DataRow, ?>> columnBuilder() {
        return new ColumnBuilder<>(this::newColumn);
    }

    private Node createColumnsSelector(String name, ObservableList<TableColumn<DataRow, ?>> columns) {
        ObjectSelector<List<TableColumn<DataRow, ?>>> s = new ObjectSelector<>(name, (v) -> {
            columns.setAll(v);
        });
        s.addChoice("With All Constraints", columnBuilder().
            col("Fixed").min(70).max(70).
            col("MinPrefMax").min(50).pref(200).max(300).
            col("Min").min(50).
            col("Pref").pref(200).
            col("Max").max(150).
            col("Std").
            asList()
        );
        s.addChoiceSupplier("20 Equal", () -> {
            var cs = columnBuilder();
            for (int i = 1; i < 20; i++) {
                cs.col("C" + i);
            }
            return cs.asList();
        });
        s.addChoiceSupplier("20 Equal, Pref=30", () -> {
            var cs = columnBuilder();
            for (int i = 1; i < 20; i++) {
                cs.col("C" + i).pref(30);
            }
            return cs.asList();
        });
        s.addChoice("Fixed in the Middle", columnBuilder().
            col("C1").
            col("C2").
            col("C3").
            col("Fixed4").fixed(100).
            col("Fixed5").fixed(100).
            col("Fixed6").fixed(100).
            col("C7").
            col("C8").
            col("C9").
            asList()
        );
        s.addChoice("5 Fixed", columnBuilder().
            col("Fixed1").fixed(50).
            col("Fixed2").fixed(50).
            col("Fixed3").fixed(50).
            col("Fixed4").fixed(50).
            col("Fixed5").fixed(50).
            asList()
        );
        s.addChoice("5 Max", columnBuilder().
            col("Max1").max(90).
            col("Max2").max(90).
            col("Max3").max(90).
            col("Max4").max(90).
            col("Max5").max(90).
            asList()
        );
        s.addChoice("Nested Columns", columnBuilder().
            col("Pref100").pref(100).
            col("Pref200").pref(200).
            col("Pref300").pref(300).
            col("Fixed100").fixed(100).
            col("Pref100").pref(100).
            col("Min100").min(100).
            col("Max100").max(100).
            col("Pref400").pref(400).
            col("C").
            combine(0, 3).
            combine(1, 2).
            asList()
        );
        s.addChoice("<empty>", FXCollections.observableArrayList());
        return s;
    }

    private Node createColumnResizePolicy(String name, ObjectProperty<Callback<ResizeFeatures, Boolean>> p) {
        ObjectOption<Callback<ResizeFeatures, Boolean>> s = new ObjectOption<>(name, p);
        s.addChoice("AUTO_RESIZE_FLEX_NEXT_COLUMN", TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        s.addChoice("AUTO_RESIZE_FLEX_LAST_COLUMN", TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        s.addChoice("AUTO_RESIZE_ALL_COLUMNS", TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        s.addChoice("AUTO_RESIZE_LAST_COLUMN", TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        s.addChoice("AUTO_RESIZE_NEXT_COLUMN", TableView.CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN);
        s.addChoice("AUTO_RESIZE_SUBSEQUENT_COLUMNS", TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        s.addChoice("UNCONSTRAINED_RESIZE_POLICY", TableView.UNCONSTRAINED_RESIZE_POLICY);
        s.addChoice("user defined, equal width", new UserDefinedResizePolicy());
        return s;
    }

    private Node createFocusModelOptions(String name, ObjectProperty<TableView.TableViewFocusModel<DataRow>> p) {
        ObjectOption<TableView.TableViewFocusModel<DataRow>> s = new ObjectOption<>(name, p);
        s.addChoiceSupplier("<default>", () -> new TableView.TableViewFocusModel(control));
        s.addChoice("<null>", null);
        return s;
    }

    private static record SelectionChoice(boolean isNull, boolean isMultiple, boolean isCells) { }

    private Node createSelectionModelOptions(String name) {
        var original = control.getSelectionModel();
        ObjectSelector<SelectionChoice> s = new ObjectSelector<>(name, (v) -> {
            control.setSelectionModel(v.isNull() ? null : original);
            original.setSelectionMode(v.isMultiple() ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
            original.setCellSelectionEnabled(v.isCells());
        });
        s.addChoice("Single Row", new SelectionChoice(false, false, false));
        s.addChoice("Multiple Rows", new SelectionChoice(false, true, false));
        s.addChoice("Single Cell", new SelectionChoice(false, false, true));
        s.addChoice("Multiple Cells", new SelectionChoice(false, true, true));
        s.addChoice("<null>", new SelectionChoice(true, false, false));
        s.selectFirst();
        return s;
    }

    private List<DataRow> createRows(int count) {
        ArrayList<DataRow> rv = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rv.add(new DataRow());
        }
        return rv;
    }

    private Node createItemsOptions(String name, ObservableList<DataRow> items) {
        ObjectSelector<List<DataRow>> s = new ObjectSelector<>(name, (v) -> {
            items.setAll(v);
        });
        s.addChoiceSupplier("1 Row", () -> createRows(1));
        s.addChoiceSupplier("10 Rows", () -> createRows(10));
        s.addChoiceSupplier("100 Rows", () -> createRows(100));
        s.addChoiceSupplier("1,000 Rows", () -> createRows(1000));
        s.addChoiceSupplier("10,000 Rows", () -> createRows(10_000));
        s.addChoiceSupplier("<empty>", () -> createRows(0));
        return s;
    }

    private Callback<TableView<DataRow>, TableRow<DataRow>> createRowFactory(Color c) {
        return (v) -> {
            TableRow<DataRow> row = new TableRow<>();
            row.setBackground(Background.fill(c));
            return row;
        };
    }

    private Node createRowFactoryOptions(String name, ObjectProperty<Callback<TableView<DataRow>, TableRow<DataRow>>> p) {
        Callback<TableView<DataRow>, TableRow<DataRow>> defaultValue = p.get();
        ObjectOption<Callback<TableView<DataRow>, TableRow<DataRow>>> s = new ObjectOption<>(name, p);
        s.addChoice("<default>", defaultValue);
        s.addChoice("Red Background", createRowFactory(Color.RED));
        s.addChoice("Green Background", createRowFactory(Color.GREEN));
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }
}
