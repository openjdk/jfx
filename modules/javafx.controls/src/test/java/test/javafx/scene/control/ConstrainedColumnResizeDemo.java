/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.control;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Skin;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 *
 */
public class ConstrainedColumnResizeDemo extends Application {
    protected static final boolean CELLS_WITH_BINDINGS = !true;
    protected static final boolean SNAP_TO_PIXEL = !true;
    protected static final boolean MIN_WIDTH = !true;

    protected TableView<String> table;
    protected TableViewSelectionModel<String> oldTableSelectionModel;
    protected TextArea textArea;

    public static void main(String[] args) {
        Application.launch(ConstrainedColumnResizeDemo.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // table
        table = new TableView();
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setSkin(new TableViewSkin(table));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setSnapToPixel(SNAP_TO_PIXEL);
        table.skinProperty().addListener((src, pre, cur) -> {
            Skin<?> skin = table.getSkin();
            if (skin != null) {
                Node nd = skin.getNode();
                if (nd instanceof Region r) {
                    r.setSnapToPixel(SNAP_TO_PIXEL);
                }
            }
        });

        table.getColumns().addAll(
            createColumn("C0", 50),
            createColumn("C1", 100),
            createColumn("C2", 200));

        // https://bugs.openjdk.org/browse/JDK-8087673
        //        {
        //            table.setTableMenuButtonVisible(true);
        //            table.getColumns().get(2).setGraphic(new Slider());
        //        }

        for (TableColumnBase<?,?> c: table.getVisibleLeafColumns()) {
            Node nd = c.getStyleableNode();
            if (nd instanceof Region r) {
                r.setSnapToPixel(SNAP_TO_PIXEL);
            }
        }

        CheckBox addContent = new CheckBox("table content");
        addContent.selectedProperty().addListener((s, p, on) -> {
            if (on) {
                table.getItems().addAll(
                    "",
                    "",
                    "",
                    "",
                    "");
            } else {
                table.getItems().clear();
            }
        });
        addContent.setSelected(true);

        CheckBox tableCellSelectionEnabled = new CheckBox("cell selection");
        table.getSelectionModel().cellSelectionEnabledProperty().bind(tableCellSelectionEnabled.selectedProperty());

        CheckBox nullTableSelectionModel = new CheckBox("null cell selection model");
        nullTableSelectionModel.selectedProperty().addListener((src, prev, on) -> {
            if (on) {
                oldTableSelectionModel = table.getSelectionModel();
                table.setSelectionModel(null);
            } else {
                table.setSelectionModel(oldTableSelectionModel);
            }
        });

        CheckBox constrainedTableModel = new CheckBox("new constrained resize policy");
        constrainedTableModel.setSelected(true);
        constrainedTableModel.selectedProperty().addListener((src, prev, on) -> {
            if (on) {
                // TODO table.setColumnResizePolicy(wrap(ConstrainedColumnResize.forTable()));
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {
                table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            }
        });

        VBox vb = new VBox();
        vb.setPadding(new Insets(5));
        vb.setSpacing(5);
        vb.getChildren().addAll(
            addContent,
            tableCellSelectionEnabled,
            nullTableSelectionModel,
            constrainedTableModel);

        BorderPane p = new BorderPane();
        p.setCenter(table);
        p.setBottom(vb);

        // text area

        textArea = new TextArea();
        textArea.setEditable(false);

        // layout

        SplitPane split = new SplitPane(p, textArea);
        split.setSnapToPixel(SNAP_TO_PIXEL);

        Scene sc = new Scene(split);

        stage.setScene(sc);
        stage.setMinWidth(1500);
        stage.setTitle("TableView Tester " + System.getProperty("java.version"));
        stage.show();
    }

    protected Callback<ResizeFeatures,Boolean> wrap(Callback<ResizeFeatures,Boolean> policy) {
        return new Callback<ResizeFeatures,Boolean>() {
            @Override
            public Boolean call(ResizeFeatures f) {
                Boolean rv = policy.call(f);
                int ix = f.getTable().getColumns().indexOf(f.getColumn());
                System.out.println(
                    "col=" + (ix < 0 ? f.getColumn() : ix) +
                    " delta=" + f.getDelta() +
                    " w=" + f.getTable().getWidth() +
                    " rv=" + rv
                    );
                return rv;
            }
        };
    }

    protected static TableColumn<String,String> createColumn(String name, int prefWidth) {
        TableColumn<String,String> c = new TableColumn<>(name);
        c.setPrefWidth(prefWidth);
        if (MIN_WIDTH) {
            c.setMinWidth(0);
        }
        c.setCellValueFactory((f) -> new SimpleStringProperty("yo"));
        return c;
    }
}
