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

import java.util.Random;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ListView page
 */
public class ListViewPage extends TestPaneBase {
    enum Demo {
        EMPTY("Empty"),
        LARGE("Large"),
        SMALL("Small"),
        VARIABLE("Variable Height"),
        ;

        private final String text;
        Demo(String text) { this.text = text; }
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

    protected final ComboBox<Demo> demoSelector;
    protected final ComboBox<Selection> selectionSelector;
    protected final CheckBox nullFocusModel;
    protected ListView<Object> control;

    public ListViewPage() {
        setId("ListViewPage");

        // selector
        demoSelector = new ComboBox<>();
        demoSelector.setId("demoSelector");
        demoSelector.getItems().addAll(Demo.values());
        demoSelector.setEditable(false);
        demoSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updatePane();
        });

        selectionSelector = new ComboBox<>();
        selectionSelector.setId("selectionSelector");
        selectionSelector.getItems().addAll(Selection.values());
        selectionSelector.setEditable(false);
        selectionSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updatePane();
        });

        nullFocusModel = new CheckBox("null focus model");
        nullFocusModel.setId("nullFocusModel");
        nullFocusModel.selectedProperty().addListener((s, p, c) -> {
            updatePane();
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

        // layout

        OptionPane p = new OptionPane();
        p.label("Data:");
        p.option(demoSelector);
        p.option(addButton);
        p.option(clearButton);
        p.label("Selection Model:");
        p.option(selectionSelector);
        p.option(nullFocusModel);
        p.option(jumpButton);
        setOptions(p);

        demoSelector.getSelectionModel().selectFirst();
        selectionSelector.getSelectionModel().select(Selection.MULTIPLE);
    }

    protected Object[] createSpec(Demo d) {
        switch (d) {
        case EMPTY:
            return new Object[] {
            };
        case LARGE:
            return new Object[] {
                Cmd.ROWS, 10_000,
            };
        case SMALL:
            return new Object[] {
                Cmd.ROWS, 3,
            };
        case VARIABLE:
            return new Object[] {
                Cmd.VARIABLE_ROWS, 500,
            };
        default:
            throw new Error("?" + d);
        }
    }

    protected void updatePane() {
        Demo d = demoSelector.getSelectionModel().getSelectedItem();
        Object[] spec = createSpec(d);

        Pane n = createPane(d, spec);
        setContent(n);
    }

    protected Pane createPane(Demo demo, Object[] spec) {
        if ((demo == null) || (spec == null)) {
            return new BorderPane();
        }

        boolean nullSelectionModel = false;
        SelectionMode selectionMode = SelectionMode.SINGLE;
        Selection sel = selectionSelector.getSelectionModel().getSelectedItem();
        if (sel != null) {
            switch (sel) {
            case MULTIPLE:
                selectionMode = SelectionMode.MULTIPLE;
                break;
            case NULL:
                nullSelectionModel = true;
                break;
            case SINGLE:
                break;
            default:
                throw new Error("?" + sel);
            }
        }

        control = new ListView<>();
        control.getSelectionModel().setSelectionMode(selectionMode);
        if (nullSelectionModel) {
            control.setSelectionModel(null);
        }
        if (nullFocusModel.isSelected()) {
            control.setFocusModel(null);
        }

        for (int i = 0; i < spec.length;) {
            Object x = spec[i++];
            if (x instanceof Cmd cmd) {
                switch (cmd) {
                case ROWS: {
                    int n = (int)(spec[i++]);
                    for (int j = 0; j < n; j++) {
                        control.getItems().add(newItem(i));
                    }
                }
                    break;
                case VARIABLE_ROWS: {
                    int n = (int)(spec[i++]);
                    for (int j = 0; j < n; j++) {
                        control.getItems().add(newVariableItem(j));
                    }
                }
                    break;
                default:
                    throw new Error("?" + cmd);
                }
            } else {
                throw new Error("?" + x);
            }
        }

        BorderPane bp = new BorderPane();
        bp.setCenter(control);
        return bp;
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
}
