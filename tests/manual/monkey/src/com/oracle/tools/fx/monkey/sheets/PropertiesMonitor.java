/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.sheets;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import com.oracle.tools.fx.monkey.util.OptionWindow;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Monitors Public Properties
 */
public class PropertiesMonitor extends BorderPane {
    private final TreeTableView<Entry> table;

    public PropertiesMonitor(Node owner) {
        table = new TreeTableView<>();
        table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        {
            TreeTableColumn<Entry, String> c = new TreeTableColumn<>("Name");
            c.setCellFactory((tc) -> createCell());
            c.setCellValueFactory((f) -> new SimpleStringProperty(f.getValue().getValue().getName()));
            c.setPrefWidth(120);
            table.getColumns().add(c);
        }
        {
            TreeTableColumn<Entry, String> c = new TreeTableColumn<>("Type");
            c.setCellFactory((tc) -> createCell());
            c.setCellValueFactory((f) -> new SimpleStringProperty(f.getValue().getValue().getType()));
            c.setPrefWidth(100);
            table.getColumns().add(c);
        }
        {
            TreeTableColumn<Entry, Object> c = new TreeTableColumn<>("Value");
            c.setCellFactory((tc) -> createCell());
            c.setCellValueFactory((f) -> f.getValue().getValue().getValue());
            c.setPrefWidth(300);
            table.getColumns().add(c);
        }
        table.setShowRoot(false);
        table.setRoot(collectProperties(owner));
        setCenter(table);
    }

    public static void open(Node node) {
        if (node != null) {
            String name = node.getClass().getSimpleName();
            PropertiesMonitor p = new PropertiesMonitor(node);
            OptionWindow.open(node, "Properties: " + name, 800, 900, p);
        }
    }

    private TreeTableCell createCell() {
        return new TreeTableCell<Object, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null) {
                    super.setText(null);
                    super.setGraphic(null);
                } else if (item instanceof Node) {
                    super.setText(null);
                    super.setGraphic((Node)item);
                } else {
                    super.setText(item.toString());
                    super.setGraphic(null);
                }
                Object x = getTableRow().getItem();
                if (x instanceof Entry en) {
                    boolean hdr = en.isHeader();
                    setBackground(hdr ? Background.fill(Color.rgb(0, 0, 0, 0.1)) : null);
                    setStyle(hdr ? "-fx-font-weight:bold;" : "-fx-font-weight:normal;");
                }
            }
        };
    }

    private static TreeItem<Entry> collectProperties(Node n) {
        TreeItem<Entry> root = new TreeItem<>(null);
        root.setExpanded(true);
        boolean expand = true;
        while (n != null) {
            collectProperties(root, n, expand);
            n = n.getParent();
            expand = false;
        }
        return root;
    }

    private static void collectProperties(TreeItem<Entry> root, Node n, boolean expand) {
        ArrayList<Entry> a = new ArrayList<>();
        try {
            BeanInfo inf = Introspector.getBeanInfo(n.getClass());
            PropertyDescriptor[] ps = inf.getPropertyDescriptors();
            for (PropertyDescriptor p: ps) {
                Entry en = createEntry(n, p);
                if (en != null) {
                    a.add(en);
                }
            }

            a.add(new Entry("styleClass", "ObservableList", n.getStyleClass()));
            a.add(new Entry("pseudoClassStates", "ObservableSet", n.getPseudoClassStates()));

            Collections.sort(a, new Comparator<Entry>() {
                @Override
                public int compare(Entry a, Entry b) {
                    return a.getName().compareTo(b.getName());
                }
            });
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

        String type = n.getClass().getSimpleName();
        if (Utils.isBlank(type)) {
            type = n.getClass().getName();
            int ix = type.lastIndexOf('.');
            if (ix >= 0) {
                type = type.substring(ix + 1);
            }
        }
        TreeItem<Entry> ti = new TreeItem<>(new Entry(type, null, null));
        ti.setExpanded(expand);
        root.getChildren().add(ti);

        for (Entry en: a) {
            ti.getChildren().add(new TreeItem<>(en));
        }
    }

    private static Entry createEntry(Node n, PropertyDescriptor pd) {
        Class<?> t = pd.getPropertyType();
        if (t == null) {
            return null;
        }
        if (t.isAssignableFrom(EventHandler.class)) {
            return null;
        }
        if (t.isAssignableFrom(EventDispatcher.class)) {
            return null;
        }
        String name = pd.getName();
        String pname = name + "Property";
        try {
            Method m = n.getClass().getMethod(pname);
            if (m != null) {
                Object v = m.invoke(n);
                if (v instanceof ObservableValue val) {
                    Class<?> tp = pd.getPropertyType();
                    String type = tp == null ? "<null>" : tp.getSimpleName();
                    return new Entry(pd.getName(), type, val);
                }
            }
        } catch (Throwable e) {
            // ignore
        }
        return null;
    }

    static class Entry {
        private final String name;
        private final String type;
        private final Observable prop;
        private SimpleObjectProperty<Object> value;

        public Entry(String name, String type, Observable p) {
            this.name = name;
            this.type = type;
            this.prop = p;
        }

        public boolean isHeader() {
            return type == null;
        }

        public String getName() {
            return name;
        }

        public SimpleObjectProperty<Object> getValue() {
            if (value == null) {
                value = new SimpleObjectProperty<>();

                if (prop != null) {
                    if (prop instanceof ObservableValue p) {
                        p.addListener((src, prev, c) -> {
                            setValue(c);
                        });
                        Object y = p.getValue();
                        setValue(p.getValue());
                    } else if (prop instanceof ObservableList p) {
                        p.addListener((Observable x) -> {
                            setValue(p.toString());
                        });
                        setValue(p.toString());
                    } else if (prop instanceof ObservableSet p) {
                        p.addListener((Observable x) -> {
                            setValue(p.toString());
                        });
                        setValue(p.toString());
                    }
                }
            }
            return value;
        }

        private void setValue(Object x) {
            if (x instanceof Node) {
                // do not set nodes!
                x = x.getClass().getSimpleName();
            }
            value.set(x);
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
