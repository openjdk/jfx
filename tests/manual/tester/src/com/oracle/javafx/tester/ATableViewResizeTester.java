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

package com.oracle.javafx.tester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ConstrainedColumnResizeBase;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Tests TableView/JTable constrained column resize modes.
 */
public class ATableViewResizeTester extends Application {

    enum Demo {
        PREF("pref only"),
        ALL("all set: min, pref, max"),
        EMPTY("empty with pref"),
        MIN_WIDTH("min width"),
        MAX_WIDTH("max width"),
        MIN_WIDTH2("min width (middle)"),
        MAX_WIDTH2("max width (middle)"),
        MIN_WIDTH3("min width (beginning)"),
        MAX_WIDTH3("max width (beginning)"),
        FIXED_MIDDLE("fixed in the middle"),
        ALL_FIXED("all fixed"),
        ALL_MAX("all with maximum width"),
        MIN_IN_CENTER("min widths set in middle columns"),
        MAX_IN_CENTER("max widths set in middle columns"),
        NO_NESTED("no nested columns"),
        NESTED("nested columns"),
        MILLION("million rows"),
        MANY_COLUMNS("many columns"),
        MANY_COLUMNS_SAME("many columns, same pref");

        private final String text;
        Demo(String text) { this.text = text; }
        public String toString() { return text; }
    }

    public enum Policy {
        AUTO_RESIZE_FLEX_NEXT_COLUMN(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS),
        AUTO_RESIZE_FLEX_LAST_COLUMN(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS),
        AUTO_RESIZE_NEXT_COLUMN(JTable.AUTO_RESIZE_NEXT_COLUMN),
        AUTO_RESIZE_SUBSEQUENT_COLUMNS(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS),
        AUTO_RESIZE_LAST_COLUMN(JTable.AUTO_RESIZE_LAST_COLUMN),
        AUTO_RESIZE_ALL_COLUMNS(JTable.AUTO_RESIZE_ALL_COLUMNS),
        USER_DEFINED_EQUAL_WIDTHS(JTable.AUTO_RESIZE_ALL_COLUMNS),
        UNCONSTRAINED_RESIZE_POLICY(JTable.AUTO_RESIZE_OFF),
        CONSTRAINED_RESIZE_POLICY(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        private final int value;
        Policy(int v) { this.value = v; }
        public int getValue() { return value; }
    }

    public enum Cmd {
        ROWS,
        COL,
        MIN,
        PREF,
        MAX,
        COMBINE
    }

    protected BorderPane contentPane;
    protected ComboBox<Demo> demoSelector;
    protected ComboBox<Policy> policySelector;
    protected SwingPanel swingPanel;
    protected static Callback<TableColumn<String, String>, TableCell<String, String>> CELL_FACTORY = initCellFactory();

    public static void main(String[] args) {
        Application.launch(ATableViewResizeTester.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        contentPane = new BorderPane();

        // selector
        demoSelector = new ComboBox<>();
        demoSelector.getItems().addAll(Demo.values());
        demoSelector.setEditable(false);
        demoSelector.getSelectionModel().selectedItemProperty().addListener((s,p,c) -> {
            updatePane();
        });

        policySelector = new ComboBox<>();
        policySelector.getItems().addAll(Policy.values());
        policySelector.setEditable(false);
        policySelector.getSelectionModel().selectedItemProperty().addListener((s,p,c) -> {
            updatePane();
        });

        // layout

        SplitPane split = new SplitPane(contentPane, new BorderPane());

        HBox hb = new HBox(
            new Label("Data:"),
            demoSelector,
            new Label(" Policy:"),
            policySelector
        );
        hb.setSpacing(5);

        BorderPane bp = new BorderPane();
        bp.setTop(hb);
        bp.setCenter(split);

        SwingNode swn = new SwingNode();
        EventQueue.invokeLater(() -> {
            swingPanel = new SwingPanel();
            swn.setContent(swingPanel);
        });

        SplitPane sp = new SplitPane(bp, new BorderPane(swn));
        sp.setOrientation(Orientation.VERTICAL);

        stage.setScene(new Scene(sp));
        stage.setWidth(1000);
        stage.setHeight(500);
        stage.show();

        stage.renderScaleXProperty().addListener((s,p,c) -> updateTitle(stage));
        updateTitle(stage);

        demoSelector.getSelectionModel().
            selectFirst();
            //select(Demo.FIXED_MIDDLE);
        policySelector.getSelectionModel().
            selectFirst();
            //select(Policy.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    protected void updateTitle(Stage s) {
        s.setTitle("TableView/JTable Resize Tester " + System.getProperty("java.version") + " scaleX=" + s.getRenderScaleX());
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

    protected String describe(TableColumn c) {
        StringBuilder sb = new StringBuilder();
        if(c.getMinWidth() != 10.0) {
            sb.append("m");
        }
        if(c.getPrefWidth() != 80.0) {
            sb.append("p");
        }
        if(c.getMaxWidth() != 5000.0) {
            sb.append("X");
        }
        return sb.toString();
    }

    protected Callback<ResizeFeatures, Boolean> createPolicy(Policy p) {
        switch(p) {
        case AUTO_RESIZE_FLEX_NEXT_COLUMN:
            return TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN;
        case AUTO_RESIZE_FLEX_LAST_COLUMN:
            return TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN;
        case AUTO_RESIZE_ALL_COLUMNS:
            return TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS;
        case AUTO_RESIZE_LAST_COLUMN:
            return TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN;
        case AUTO_RESIZE_NEXT_COLUMN:
            return TableView.CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN;
        case AUTO_RESIZE_SUBSEQUENT_COLUMNS:
            return TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS;
        case CONSTRAINED_RESIZE_POLICY:
            return TableView.CONSTRAINED_RESIZE_POLICY;
        case UNCONSTRAINED_RESIZE_POLICY:
            return TableView.UNCONSTRAINED_RESIZE_POLICY;
        case USER_DEFINED_EQUAL_WIDTHS:
            return new UserDefinedResizePolicy();
        default:
            throw new Error("?" + p);
        }
    }

    protected Object[] createSpec(Demo d) {
        switch(d) {
        case ALL:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL, Cmd.MIN, 20, Cmd.PREF, 20, Cmd.MAX, 20,
                Cmd.COL, Cmd.PREF, 200,
                Cmd.COL, Cmd.PREF, 300, Cmd.MAX, 400,
                Cmd.COL
            };
        case PREF:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.PREF, 100,
                Cmd.COL, Cmd.PREF, 200,
                Cmd.COL, Cmd.PREF, 300,
                Cmd.COL, Cmd.PREF, 400
            };
        case EMPTY:
            return new Object[] {
                Cmd.COL, Cmd.PREF, 100,
                Cmd.COL, Cmd.PREF, 200,
                Cmd.COL, Cmd.PREF, 300
            };
        case MIN_WIDTH:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL, Cmd.MIN, 300
            };
        case MAX_WIDTH:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL, Cmd.MAX, 100
            };
        case MIN_WIDTH2:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL, Cmd.MIN, 300,
                Cmd.COL
            };
        case MAX_WIDTH2:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL, Cmd.MAX, 100,
                Cmd.COL
            };
        case MIN_WIDTH3:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.MIN, 300,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL
            };
        case MAX_WIDTH3:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.MAX, 100,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL
            };
        case MIN_IN_CENTER:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL, Cmd.MIN, 20,
                Cmd.COL, Cmd.MIN, 30,
                Cmd.COL, Cmd.MIN, 40,
                Cmd.COL, Cmd.MIN, 50,
                Cmd.COL, Cmd.MIN, 60,
                Cmd.COL
            };
        case MAX_IN_CENTER:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL, Cmd.MAX, 20,
                Cmd.COL, Cmd.MAX, 30,
                Cmd.COL, Cmd.MAX, 40,
                Cmd.COL, Cmd.MAX, 50,
                Cmd.COL, Cmd.MAX, 60,
                Cmd.COL
            };
        case FIXED_MIDDLE:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL, Cmd.MIN, 100, Cmd.MAX, 100,
                Cmd.COL, Cmd.MIN, 100, Cmd.MAX, 100,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL
            };
        case ALL_FIXED:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.MIN, 50, Cmd.MAX, 50,
                Cmd.COL, Cmd.MIN, 50, Cmd.MAX, 50,
                Cmd.COL, Cmd.MIN, 50, Cmd.MAX, 50
            };
        case ALL_MAX:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.MAX, 50,
                Cmd.COL, Cmd.MAX, 50,
                Cmd.COL, Cmd.MAX, 50
            };
       case NO_NESTED:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.PREF, 100,
                Cmd.COL, Cmd.PREF, 200,
                Cmd.COL, Cmd.PREF, 300,
                Cmd.COL, Cmd.MIN, 100, Cmd.MAX, 100,
                Cmd.COL, Cmd.PREF, 100,
                Cmd.COL, Cmd.MIN, 100,
                Cmd.COL, Cmd.MAX, 100,
                Cmd.COL, Cmd.PREF, 400,
                Cmd.COL
            };
        case NESTED:
            return new Object[] {
                Cmd.ROWS, 3,
                Cmd.COL, Cmd.PREF, 100,
                Cmd.COL, Cmd.PREF, 200,
                Cmd.COL, Cmd.PREF, 300,
                Cmd.COL, Cmd.MIN, 100, Cmd.MAX, 100,
                Cmd.COL, Cmd.PREF, 100,
                Cmd.COL, Cmd.MIN, 100,
                Cmd.COL, Cmd.MAX, 100,
                Cmd.COL, Cmd.PREF, 400,
                Cmd.COL,
                Cmd.COMBINE, 0, 3,
                Cmd.COMBINE, 1, 2
            };
        case MANY_COLUMNS:
            return new Object[] {
                Cmd.ROWS, 300,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL,
                Cmd.COL
            };
        case MANY_COLUMNS_SAME:
            return new Object[] {
                Cmd.ROWS, 300,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30,
                Cmd.COL, Cmd.PREF, 30
            };
        case MILLION:
            return new Object[] {
                Cmd.ROWS, 1_000_000,
                Cmd.COL,
                Cmd.COL
            };
        default:
            throw new Error("?" + d);
        }
    }

    protected void updatePane() {
        Policy p = policySelector.getSelectionModel().getSelectedItem();
        Demo d = demoSelector.getSelectionModel().getSelectedItem();
        Object[] spec = createSpec(d);

        Pane n = createPane(p, spec);
        contentPane.setCenter(n);

        EventQueue.invokeLater(() -> {
            swingPanel.updatePane(p, spec);
        });
    }

    protected void combineColumns(TableView<String> t, int ix, int count, int name) {
        TableColumn<String, String> tc = new TableColumn<>();
        tc.setCellFactory(CELL_FACTORY);
        tc.setText("N" + name);

        for (int i = 0; i < count; i++) {
            TableColumn<String,?> c = t.getColumns().remove(ix);
            tc.getColumns().add(c);
        }
        t.getColumns().add(ix, tc);
    }

    protected Pane createPane(Policy policy, Object[] spec) {
        if ((spec == null) || (policy == null)) {
            return new BorderPane();
        }

        TableView<String> table = new TableView<>();
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Callback<ResizeFeatures,Boolean> p = createPolicy(policy);
        table.setColumnResizePolicy(p);

        TableColumn<String,String> lastColumn = null;
        int id = 1;

        for (int i = 0; i < spec.length;) {
            Object x = spec[i++];
            if (x instanceof Cmd cmd) {
                switch (cmd) {
                case COL:
                    TableColumn<String, String> c = new TableColumn<>();
                    c.setCellFactory(CELL_FACTORY);
                    table.getColumns().add(c);
                    c.setText("C" + table.getColumns().size());
//                    if (table.getColumns().size() == 1) {
//                        c.setText("Really really really really really really really really really really really really really long");
//                    }
                    c.setCellValueFactory((f) -> new SimpleStringProperty(describe(c)));
                    lastColumn = c;
                    break;
                case MAX: {
                    int w = (int)(spec[i++]);
                    lastColumn.setMaxWidth(w);
                }
                    break;
                case MIN: {
                    int w = (int)(spec[i++]);
                    lastColumn.setMinWidth(w);
                }
                    break;
                case PREF: {
                    int w = (int)(spec[i++]);
                    lastColumn.setPrefWidth(w);
                }
                    break;
                case ROWS:
                    int n = (int)(spec[i++]);
                    for (int j = 0; j < n; j++) {
                        table.getItems().add(String.valueOf(n));
                    }
                    break;
                case COMBINE:
                    int ix = (int)(spec[i++]);
                    int ct = (int)(spec[i++]);
                    combineColumns(table, ix, ct, id++);
                    break;
                default:
                    throw new Error("?" + cmd);
                }
            } else {
                throw new Error("?" + x);
            }
        }

        BorderPane bp = new BorderPane();
        bp.setCenter(table);
        return bp;
    }

    protected static class SwingPanel extends JPanel {
        public SwingPanel() {
            super(new BorderLayout());
        }

        public void updatePane(Policy policy, Object[] spec) {
            JComponent p = createPanel(policy, spec);
            removeAll();
            if (p != null) {
                add(p);
            }
            validate();
            repaint();
        }

        private int createHSBPolicy(Policy p) {
            switch (p) {
            case UNCONSTRAINED_RESIZE_POLICY:
                return JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
            default:
                return JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
            }
        }

        protected JComponent createPanel(Policy policy, Object[] spec) {
            if ((spec == null) || (policy == null)) {
                return null;
            }

            DefaultTableModel m = new DefaultTableModel() {
                @Override
                public Object getValueAt(int row, int column) {
                    return "";
                }
            };

            JTable t = new JTable(m);
            t.setShowHorizontalLines(true);
            t.setShowVerticalLines(true);
            t.setGridColor(Color.LIGHT_GRAY);
            t.setAutoResizeMode(policy.getValue());

            javax.swing.table.TableColumn lastColumn = null;

            for (int i = 0; i < spec.length;) {
                Object x = spec[i++];
                if (x instanceof Cmd cmd) {
                    switch (cmd) {
                    case COL:
                        javax.swing.table.TableColumn c = new javax.swing.table.TableColumn();
                        t.getColumnModel().addColumn(c);
                        c.setHeaderValue("C" + t.getColumnCount());
                        lastColumn = c;
                        break;
                    case MAX: {
                        int w = (int)(spec[i++]);
                        lastColumn.setMaxWidth(w);
                    }
                        break;
                    case MIN: {
                        int w = (int)(spec[i++]);
                        lastColumn.setMinWidth(w);
                    }
                        break;
                    case PREF: {
                        int w = (int)(spec[i++]);
                        lastColumn.setPreferredWidth(w);
                    }
                        break;
                    case ROWS:
                        int n = (int)(spec[i++]);
                        for (int j = 0; j < n; j++) {
                            m.addRow((Object[])null);
                        }
                        break;
                    case COMBINE:
                        // ignored
                        int ix = (int)(spec[i++]);
                        int ct = (int)(spec[i++]);
                        break;
                    default:
                        throw new Error("?" + cmd);
                    }
                } else {
                    throw new Error("?" + x);
                }
            }

            EmptyBorder b = new EmptyBorder(0, 0, 0, 0);

            int hsp = createHSBPolicy(policy);
            JScrollPane scroll = new JScrollPane(t, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, hsp);
            scroll.setBorder(b);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, new JPanel());
            split.setContinuousLayout(true);
            return split;
        }
    }

    private static Callback<TableColumn<String, String>, TableCell<String, String>> initCellFactory() {
        return new Callback<TableColumn<String, String>, TableCell<String, String>>() {
            public TableCell call(TableColumn column) {
                return new TableCell<String, String>() {
                    {
                        widthProperty().addListener((s, p, c) -> {
                            setStyle(computeStyle());
                        });
                    }

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                    }

                    private String computeStyle() {
                        boolean empty = isEmpty();
                        if (!empty) {
                            double w = getWidth();
                            if (isClose(w, column.getMinWidth())) {
                                return "-fx-background-color:#ffeeee;";
                            } else if (isClose(w, column.getPrefWidth())) {
                                return "-fx-background-color:#eeffee;";
                            } else if (isClose(w, column.getMaxWidth())) {
                                return "-fx-background-color:#eeeeff;";
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }

    private static boolean isClose(double a, double b) {
        return Math.abs(a - b) < 0.00001;
    }

    /**
     * a user-defined policy demonstrates that we can indeed create a custom policy
     * using the new API. this policy simply sizes all columns equally.
     */
    protected static class UserDefinedResizePolicy
        extends ConstrainedColumnResizeBase
        implements Callback<TableView.ResizeFeatures,Boolean> {

        @SuppressWarnings("unchecked")
        @Override
        public Boolean call(ResizeFeatures rf) {
            List<? extends TableColumnBase<?,?>> visibleLeafColumns = rf.getTable().getVisibleLeafColumns();
            int sz = visibleLeafColumns.size();
            // using added public method getContentWidth()
            double w = rf.getContentWidth() / sz;
            for (TableColumnBase<?,?> c: visibleLeafColumns) {
                // using added public method setColumnWidth()
                rf.setColumnWidth(c, w);
            }
            return false;
        }
    }
}
