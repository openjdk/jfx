/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.FocusModel;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ListView page
 */
public class ListViewPage extends TestPaneBase implements HasSkinnable {
    enum Data {
        EMPTY("Empty"),
        LARGE("Large"),
        SMALL("Small"),
        VARIABLE("Variable Height"),
        ;

        private final String text;
        Data(String text) { this.text = text; }
        public String toString() { return text; }
    }

    public enum Selection {
        SINGLE("single selection"),
        MULTIPLE("multiple selection"),
        NULL("null selection model");

        private final String text;
        Selection(String text) { this.text = text; }
        public String toString() { return text; }
    }

    public enum Cmd {
        ROWS,
        VARIABLE_ROWS,
    }

    private enum Cells {
        DEFAULT,
        EDITABLE_TEXT_FIELD,
        LARGE_ICON,
        VARIABLE,
    }

    private final ComboBox<Data> dataSelector;
    private final ComboBox<Cells> cellFactorySelector;
    private final ComboBox<Selection> selectionSelector;
    private final CheckBox nullFocusModel;
    private final CheckBox editable;
    private final ListView<Object> control;
    private FocusModel<Object> defaultFocusModel;
    private MultipleSelectionModel<Object> defaultSelectionModel;

    public ListViewPage() {
        FX.name(this, "ListViewPage");
        
        control = new ListView<>();
        control.setTooltip(new Tooltip("edit to 'update' to commit the change"));
        control.setOnEditCommit((ev) -> {
            if ("update".equals(ev.getNewValue())) {
                int ix = ev.getIndex();
                ev.getSource().getItems().set(ix, "UPDATED!");
                System.out.println("committing the value `UPDATED!`");
            } else {
                System.out.println("discarding the new value: " + ev.getNewValue());
            }
        });
        defaultFocusModel = control.getFocusModel();
        defaultSelectionModel = control.getSelectionModel();
        setContent(new BorderPane(control));

        dataSelector = new ComboBox<>();
        FX.name(dataSelector, "demoSelector");
        dataSelector.getItems().addAll(Data.values());
        dataSelector.setEditable(false);
        onChange(dataSelector, true, () -> {
            updateData();
        });

        cellFactorySelector = new ComboBox<>();
        FX.name(cellFactorySelector, "cellSelector");
        cellFactorySelector.getItems().addAll(Cells.values());
        cellFactorySelector.setEditable(false);
        cellFactorySelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateCellFactory();
        });

        selectionSelector = new ComboBox<>();
        FX.name(selectionSelector, "selectionSelector");
        selectionSelector.getItems().addAll(Selection.values());
        selectionSelector.setEditable(false);
        onChange(selectionSelector, true, () -> {
            updateSelectionModel();
        });

        nullFocusModel = new CheckBox("null focus model");
        FX.name(nullFocusModel, "nullFocusModel");
        onChange(nullFocusModel, true, () -> {
            updateFocusModel();
        });

        Button addButton = new Button("Add Item");
        addButton.setOnAction((ev) -> {
            control.getItems().add(newItem(""));
        });

        Button clearButton = new Button("Clear Items");
        clearButton.setOnAction((ev) -> {
            control.getItems().clear();
        });

        Button jumpButton = new Button("Jump w/VirtualFlow");
        jumpButton.setOnAction((ev) -> {
            jump();
        });

        Button refresh = new Button("Refresh");
        refresh.setOnAction((ev) -> {
            control.refresh();
        });

        editable = new CheckBox("editable");
        editable.setOnAction((ev) -> {
            updateEditable();
        });
        FX.name(editable, "editable");

        // layout

        OptionPane op = new OptionPane();
        op.label("Data:");
        op.option(dataSelector);
        op.option(addButton);
        op.option(clearButton);
        op.option(editable);
        op.label("Cell Factory:");
        op.option(cellFactorySelector);
        op.label("Selection Model:");
        op.option(selectionSelector);
        op.option(nullFocusModel);
        op.option(jumpButton);
        op.option(refresh);
        setOptions(op);

        dataSelector.getSelectionModel().selectFirst();
        selectionSelector.getSelectionModel().select(Selection.MULTIPLE);
    }
    
    protected void updateData() {
        Data d = dataSelector.getSelectionModel().getSelectedItem();
        ObservableList<Object> items = createData(d);
        control.setItems(items);
    }

    private ObservableList<Object> createData(Data d) {
        ObservableList<Object> items = FXCollections.observableArrayList();
        if (d != null) {
            switch (d) {
            case EMPTY:
                break;
            case LARGE:
                createItems(items, 10_000, this::newItem);
                break;
            case SMALL:
                createItems(items, 3, this::newItem);
                break;
            case VARIABLE:
                createItems(items, 500, this::newVariableItem);
                break;
            default:
                throw new Error("?" + d);
            }
        }
        return items;
    }

    private void createItems(ObservableList<Object> items, int count, Function<Integer, Object> gen) {
        for (int i = 0; i < count; i++) {
            Object v = gen.apply(i);
            items.add(v);
        }
    }

    protected void updateFocusModel() {
        FocusModel<Object> m;
        if (nullFocusModel.isSelected()) {
            m = null;
        } else {
            m = defaultFocusModel;
        }
        control.setFocusModel(m);
    }

    protected void updateSelectionModel() {
        MultipleSelectionModel<Object> sm = defaultSelectionModel;
        SelectionMode selectionMode = SelectionMode.SINGLE;
        Selection sel = selectionSelector.getSelectionModel().getSelectedItem();
        if (sel != null) {
            switch (sel) {
            case MULTIPLE:
                selectionMode = SelectionMode.MULTIPLE;
                break;
            case NULL:
                sm = null;
                break;
            case SINGLE:
                break;
            default:
                throw new Error("?" + sel);
            }
        }

        control.getSelectionModel().setSelectionMode(selectionMode);
        control.setSelectionModel(sm);
    }

    protected String newItem(Object n) {
        return n + "." + SequenceNumber.next();
    }

    protected String newVariableItem(Object n) {
        int rows = 1 << new Random().nextInt(5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i);
        }
        return n + "." + SequenceNumber.next() + "." + sb;
    }

    protected void jump() {
        int sz = control.getItems().size();
        int ix = sz / 2;

        control.getSelectionModel().select(ix);
        VirtualFlow f = findVirtualFlow(control);
        f.scrollTo(ix);
        f.scrollPixels(-1.0);
    }

    private VirtualFlow findVirtualFlow(Parent parent) {
        for (Node node: parent.getChildrenUnmodifiable()) {
            if (node instanceof VirtualFlow f) {
                return f;
            }

            if (node instanceof Parent p) {
                VirtualFlow f = findVirtualFlow(p);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ListViewSkin(control));
    }

    private void updateCellFactory() {
        Cells t = cellFactorySelector.getSelectionModel().getSelectedItem();
        Callback<ListView<Object>, ListCell<Object>> f = getCellFactory(t);
        control.setCellFactory(f);
    }

    private static Image createImage(String s) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("sha-256").digest(s.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            hash = new byte[3];
        }
        Color color = Color.rgb(hash[0] & 0xff, hash[1] & 0xff, hash[2] & 0xff);
        Canvas c = new Canvas(512, 512);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(color);
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        return c.snapshot(null, null);
    }
    
    private Callback getCellFactory(Cells t) {
        if (t != null) {
            switch (t) {
            case EDITABLE_TEXT_FIELD:
                return TextFieldListCell.forListView();
            case LARGE_ICON:
                return (r) -> {
                    return new ListCell<Object>() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null) {
                                super.setText(null);
                                super.setGraphic(null);
                            } else {
                                String s = item.toString();
                                super.setText(s);
                                Node n = new ImageView(createImage(s));
                                super.setGraphic(n);
                            }
                        }
                    };
                };
            case VARIABLE:
                return (r) -> {
                    return new ListCell<Object>() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            String s =
                                "111111111111111111111111111111111111111111111" +
                                "11111111111111111111111111111111111111111\n2\n3\n";
                            Text t = new Text(s);
                            t.wrappingWidthProperty().bind(widthProperty());
                            setPrefHeight(USE_COMPUTED_SIZE);
                            setGraphic(t);
                        }
                    };
                };
            }
        }

        // ListViewSkin
        return (r) -> new ListCell<Object>() {
            @Override
            public void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof Node) {
                    setText(null);
                    Node currentNode = getGraphic();
                    Node newNode = (Node)item;
                    if (currentNode == null || !currentNode.equals(newNode)) {
                        setGraphic(newNode);
                    }
                } else {
                    setText(item == null ? "null" : item.toString());
                    setGraphic(null);
                }
            }
        };
    }

    protected void updateEditable() {
        boolean on = editable.isSelected();
        control.setEditable(on);
        if (on) {
            cellFactorySelector.getSelectionModel().select(Cells.EDITABLE_TEXT_FIELD);
        }
    }
}
