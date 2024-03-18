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

import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ConstrainedColumnResizeBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.ResizeFeatures;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.skin.TreeTableViewSkin;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.ColumnBuilder;
import com.oracle.tools.fx.monkey.util.DataRow;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * TreeTableView Page.
 */
public class TreeTableViewPage extends TestPaneBase implements HasSkinnable {
    private final TreeTableView<DataRow> control;

    public TreeTableViewPage() {
        super("TreeTableViewPage");

        control = new TreeTableView<>();

        Button addDataItemButton = new Button("Add Data Item");
        addDataItemButton.setOnAction((ev) -> {
            addDataItem();
        });
        addDataItemButton.setDisable(true); // FIX

        Button clearDataItemsButton = new Button("Clear Data Items");
        clearDataItemsButton.setOnAction((ev) -> {
            control.setRoot(new TreeItem(null));
            control.setShowRoot(false);
        });

        SplitMenuButton addColumnButton = new SplitMenuButton(
            menuItem("at the beginning", () -> addColumn(0)),
            menuItem("in the middle", () -> addColumn(1)),
            menuItem("at the end", () -> addColumn(2))
        );
        addColumnButton.setText("Add Column");

        SplitMenuButton removeColumnButton = new SplitMenuButton(
            menuItem("at the beginning", () -> removeColumn(0)),
            menuItem("in the middle", () -> removeColumn(1)),
            menuItem("at the end", () -> removeColumn(2)),
            menuItem("all", () -> removeAllColumns())
        );
        removeColumnButton.setText("Remove Column");

        Button refresh = new Button("Refresh");
        refresh.setOnAction((ev) -> {
            control.refresh();
        });

        OptionPane op = new OptionPane();
        op.section("TreeTableView");
        op.option("Columns:", createColumnsSelector("columns", control.getColumns()));
        op.option(Utils.buttons(addColumnButton, removeColumnButton));
        op.option("Column Resize Policy:", createColumnResizePolicy("columnResizePolicy", control.columnResizePolicyProperty()));
        op.option(new BooleanOption("editable", "editable", control.editableProperty()));
        op.option("Fixed Cell Size:", Options.fixedSizeOption("fixedCellSize", control.fixedCellSizeProperty()));
        op.option("Focus Model:", createFocusModelOptions("focusModel", control.focusModelProperty()));
        op.option("Placeholder:", Options.placeholderNode("placeholder", control.placeholderProperty()));
        op.option("Root:", createRootOptions("root", control.rootProperty()));
        op.option(Utils.buttons(addDataItemButton, clearDataItemsButton));
        op.option("Row Factory:", createRowFactoryOptions("rowFactory", control.rowFactoryProperty()));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));
        op.option(new BooleanOption("showRoot", "show root", control.showRootProperty()));
        op.option("Sort Mode:", new EnumOption("sortMode", TreeSortMode.class, control.sortModeProperty()));
        op.option("Sort Policy: TODO", null); // TODO
        op.option(new BooleanOption("tableMenuButtonVisible", "table menu button visible", control.tableMenuButtonVisibleProperty()));
        op.option("Tree Column: TODO", null); // TODO
        ControlPropertySheet.appendTo(op, control);

        // TODO op.option("Cell Factory:", cellFactorySelector);
        //op.option(addGraphics);
        //op.option(addSubNodes);

        op.separator();
        op.option(refresh);

        setContent(control);
        setOptions(op);
    }

    private MenuItem menuItem(String text, Runnable r) {
        MenuItem m = new MenuItem(text);
        m.setOnAction((ev) -> r.run());
        return m;
    }

    private void addColumn(int where) {
        TreeTableColumn<DataRow, Object> c = newColumn();
        c.setText("C" + System.currentTimeMillis());
        //c.setCellValueFactory((f) -> new SimpleStringProperty(describe(c)));

        int ct = control.getColumns().size();
        int ix;
        switch (where) {
        case 0:
            ix = 0;
            break;
        case 1:
            ix = ct / 2;
            break;
        case 2:
        default:
            ix = ct;
            break;
        }
        if ((ct == 0) || (ix >= ct)) {
            control.getColumns().add(c);
        } else {
            control.getColumns().add(ix, c);
        }
    }

    private void removeColumn(int where) {
        int ct = control.getColumns().size();
        int ix;
        switch (where) {
        case 0:
            ix = 0;
            break;
        case 1:
            ix = ct / 2;
            break;
        case 2:
        default:
            ix = ct - 1;
            break;
        }

        if ((ct >= 0) && (ix < ct)) {
            control.getColumns().remove(ix);
        }
    }

    private void removeAllColumns() {
        control.getColumns().clear();
    }

//    protected Pane createPane(Data demo, ResizePolicy policy, Object[] spec) {
//        TreeTableColumn<String, String> lastColumn = null;
//        int id = 1;
//
//        for (int i = 0; i < spec.length;) {
//            Object x = spec[i++];
//            if (x instanceof Cmd cmd) {
//                switch (cmd) {
//                case COL_WITH_GRAPHIC:
//                    lastColumn = makeColumn((c) -> {
//                        c.setCellValueFactory((f) -> new SimpleStringProperty(describe(c)));
//                        c.setCellFactory((r) -> {
//                            return new TreeTableCell<>() {
//                                @Override
//                                protected void updateItem(String item, boolean empty) {
//                                    super.updateItem(item, empty);
//                                    if (empty) {
//                                        setGraphic(null);
//                                    } else {
//                                        Text t = new Text("11111111111111111111111111111111111111111111111111111111111111111111111111111111111111\n2\n3\n");
//                                        t.wrappingWidthProperty().bind(widthProperty());
//                                        setGraphic(t);
//                                    }
//                                    setPrefHeight(USE_COMPUTED_SIZE);
//                                }
//                            };
//                        });
//                    });
//                case ROWS:
//                    {
//                        int n = (int)(spec[i++]);
//                        TreeItem subNodeTreeItem = null;
//                        for (int j = 0; j < n; j++) {
//                            TreeItem treeItem = new TreeItem(newItem());
//                            if (addSubNodes.isSelected()) {
//                                subNodeTreeItem = new TreeItem(newItem());
//                                treeItem.getChildren().add(subNodeTreeItem);
//                            }
//                            if (addGraphics.isSelected()) {
//                                treeItem.setGraphic(new Rectangle(10, 10, Color.RED));
//                                if (subNodeTreeItem != null) {
//                                    subNodeTreeItem.setGraphic(new Rectangle(10, 10));
//                                }
//                            }
//                            control.getRoot().getChildren().add(treeItem);
//                        }
//                    }

//    protected TreeTableColumn<String, String> makeColumn(Consumer<TreeTableColumn<String, String>> updater) {
//        TreeTableColumn<String, String> c = new TreeTableColumn<>();
//        control.getColumns().add(c);
//        c.setText("C" + control.getColumns().size());
//        updater.accept(c);
//
//        if (defaultCellFactory == null) {
//            defaultCellFactory = c.getCellFactory();
//        }
//
//        Cells t = cellFactorySelector.getSelectionModel().getSelectedItem();
//        Callback<TreeTableColumn<String, String>, TreeTableCell<String, String>> f = getCellFactory(t);
//        c.setCellFactory(f);
//
//        c.setOnEditCommit((ev) -> {
//            if ("update".equals(ev.getNewValue())) {
//                var item = ev.getRowValue();
//                item.setValue("UPDATED!");
//                System.out.println("committing the value `UPDATED!`");
//            } else {
//                System.out.println("discarding the new value: " + ev.getNewValue());
//            }
//        });
//
//        return c;
//    }

    protected String newItem() {
        return SequenceNumber.next();
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new TreeTableViewSkin<>(control));
    }

    /**
     * a user-defined policy demonstrates that we can indeed create a custom policy using the new API.
     * this policy simply sizes all columns equally.
     */
    protected static class UserDefinedResizePolicy
        extends ConstrainedColumnResizeBase
        implements Callback<TreeTableView.ResizeFeatures, Boolean>
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

    private TreeTableColumn<DataRow, Object> newColumn() {
        TreeTableColumn<DataRow, Object> tc = new TreeTableColumn();
        tc.setCellFactory(TextFieldTreeTableCell.<DataRow, Object>forTreeTableColumn(DataRow.converter()));
        tc.setCellValueFactory((cdf) -> {
            Object v = cdf.getValue().getValue();
            if (v instanceof DataRow r) {
                return r.getValue(tc);
            }
            return new SimpleObjectProperty(v);
        });
        return tc;
    }

    private ColumnBuilder<TreeTableColumn<DataRow, ?>> columnBuilder() {
        return new ColumnBuilder<>(this::newColumn);
    }

    private Node createColumnsSelector(String name, ObservableList<TreeTableColumn<DataRow, ?>> columns) {
        ObjectSelector<List<TreeTableColumn<DataRow, ?>>> s = new ObjectSelector<>(name, (v) -> {
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
            for(int i=1; i<20; i++) {
                cs.col("C" + i);
            }
            return cs.asList();
        });
        s.addChoiceSupplier("20 Equal, Pref=30", () -> {
            var cs = columnBuilder();
            for(int i=1; i<20; i++) {
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
        s.addChoice("AUTO_RESIZE_FLEX_NEXT_COLUMN", TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        s.addChoice("AUTO_RESIZE_FLEX_LAST_COLUMN", TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        s.addChoice("AUTO_RESIZE_ALL_COLUMNS", TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        s.addChoice("AUTO_RESIZE_LAST_COLUMN", TreeTableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        s.addChoice("AUTO_RESIZE_NEXT_COLUMN", TreeTableView.CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN);
        s.addChoice("AUTO_RESIZE_SUBSEQUENT_COLUMNS", TreeTableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        //s.addChoice("CONSTRAINED_RESIZE_POLICY", TreeTableView.CONSTRAINED_RESIZE_POLICY);
        s.addChoice("UNCONSTRAINED_RESIZE_POLICY", TreeTableView.UNCONSTRAINED_RESIZE_POLICY);
        s.addChoice("user defined, equal width", new UserDefinedResizePolicy());
        return s;
    }

    private Node createFocusModelOptions(String name, ObjectProperty<TreeTableView.TreeTableViewFocusModel<DataRow>> p) {
        ObjectOption<TreeTableView.TreeTableViewFocusModel<DataRow>> s = new ObjectOption<>(name, p);
        s.addChoiceSupplier("<default>", () -> new TreeTableView.TreeTableViewFocusModel(control));
        s.addChoice("<null>", null);
        return s;
    }

    private Callback<TreeTableView<DataRow>, TreeTableRow<DataRow>> createRowFactory(Color c) {
        return (v) -> {
            TreeTableRow<DataRow> row = new TreeTableRow<>();
            row.setBackground(Background.fill(c));
            return row;
        };
    }

    private Node createRowFactoryOptions(String name, ObjectProperty<Callback<TreeTableView<DataRow>, TreeTableRow<DataRow>>> p) {
        Callback<TreeTableView<DataRow>, TreeTableRow<DataRow>> defaultValue = p.get();
        ObjectOption<Callback<TreeTableView<DataRow>, TreeTableRow<DataRow>>> s = new ObjectOption<>(name, p);
        s.addChoice("<default>", defaultValue);
        s.addChoice("Red Background", createRowFactory(Color.RED));
        s.addChoice("Green Background", createRowFactory(Color.GREEN));
        s.addChoice("<null>", null);
        s.selectFirst();
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

    private Supplier<TreeItem<DataRow>> mk(int count) {
        return () -> {
            TreeItem<DataRow> root = new TreeItem<>();
            for (int i = 0; i < count; i++) {
                root.getChildren().add(new TreeItem<>(new DataRow()));
            }
            return root;
        };
    }

    private Node createRootOptions(String name, ObjectProperty<TreeItem<DataRow>> p) {
        ObjectOption<TreeItem<DataRow>> s = new ObjectOption(name, p);
        s.addChoiceSupplier("1 Row", mk(1));
        s.addChoiceSupplier("10 Rows", mk(10));
        s.addChoiceSupplier("1_000 Rows", mk(1_000));
        s.addChoice("<null>", null);
        return s;
    }

    private void addDataItem() {
        // TODO
    }
}
