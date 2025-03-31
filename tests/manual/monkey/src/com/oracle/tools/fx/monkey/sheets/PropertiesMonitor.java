/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
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
import javafx.util.Duration;
import com.oracle.tools.fx.monkey.util.OptionWindow;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Monitors Public Properties or Platform Preferences.
 */
public class PropertiesMonitor extends BorderPane {
    private static final long HIGHLIGHT_DURATION = 3_000;
    private final TreeTableView<Entry> table;
    private static Timeline timeline;
    private static HashSet<Entry> highlighted = new HashSet<>();

    private PropertiesMonitor(boolean wideKey, TreeItem<Entry> root, Runnable onHiding) {
        table = new TreeTableView<>();
        table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        {
            TreeTableColumn<Entry, String> c = new TreeTableColumn<>("Name");
            c.setCellFactory((tc) -> createCell(false));
            c.setCellValueFactory((f) -> new SimpleStringProperty(f.getValue().getValue().getName()));
            c.setPrefWidth(wideKey ? 300 : 120);
            table.getColumns().add(c);
        }
        {
            TreeTableColumn<Entry, String> c = new TreeTableColumn<>("Type");
            c.setCellFactory((tc) -> createCell(false));
            c.setCellValueFactory((f) -> new SimpleStringProperty(f.getValue().getValue().getType()));
            c.setPrefWidth(100);
            table.getColumns().add(c);
        }
        {
            TreeTableColumn<Entry, Object> c = new TreeTableColumn<>("Value");
            c.setCellFactory((tc) -> createCell(true));
            c.setCellValueFactory((f) -> f.getValue().getValue().valueProperty());
            c.setPrefWidth(300);
            table.getColumns().add(c);
        }
        table.setShowRoot(false);
        table.setRoot(root);
        setCenter(table);

        // disconnect listeners on window hiding
        if (onHiding != null) {
            sceneProperty().addListener((s, p, scene) -> {
                if (scene != null) {
                    if (scene.getWindow() == null) {
                        scene.windowProperty().addListener((s2, p2, win) -> {
                            if (win != null) {
                                win.setOnHiding((ev) -> {
                                    onHiding.run();
                                });
                            }
                        });
                    } else {
                        scene.getWindow().setOnHiding((ev) -> {
                            onHiding.run();
                        });
                    }
                }
            });
        }
    }

    public static void open(Node node) {
        if (node != null) {
            String name = node.getClass().getSimpleName();
            TreeItem<Entry> root = collectProperties(node);
            PropertiesMonitor p = new PropertiesMonitor(false, root, null);
            OptionWindow.open(node, "Properties: " + name, 800, 900, p);
        }
    }

    public static void openPreferences(Object parent) {
        PrefRoot root = new PrefRoot();
        PropertiesMonitor p = new PropertiesMonitor(true, root, root::disconnect);
        OptionWindow.open(parent, "Platform Preferences Monitor", 1190, 900, p);
    }

    private TreeTableCell createCell(boolean trackChanges) {
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
                    backgroundProperty().unbind();
                    setBackground(hdr ? Background.fill(Color.rgb(0, 0, 0, 0.1)) : null);
                    setStyle(hdr ? "-fx-font-weight:bold;" : "-fx-font-weight:normal;");
                    if (trackChanges) {
                        backgroundProperty().bind(Bindings.createObjectBinding(
                            () -> {
                                return
                                    en.highlightedProperty().get() ?
                                    Background.fill(Color.rgb(255, 255, 0, 0.5)) :
                                    null;
                            },
                            en.highlightedProperty())
                        );
                    }
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
                Entry en = createEntry(n.getClass(), n, p);
                if (en != null) {
                    a.add(en);
                }
            }

            a.add(new Entry("styleClass", "ObservableList", n.getStyleClass()));
            a.add(new Entry("pseudoClassStates", "ObservableSet", n.getPseudoClassStates()));
            a.add(new Entry("properties", "ObservableMap", n.getProperties()));

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

        addSorted(ti, a);
    }

    static void addSorted(TreeItem<Entry> item, ArrayList<Entry> a) {
        Collections.sort(a, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return a.getName().compareTo(b.getName());
            }
        });

        for (Entry en: a) {
            item.getChildren().add(new TreeItem<>(en));
        }
    }

    private static Entry createEntry(Class<?> cs, Object n, PropertyDescriptor pd) {
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
            Method m = cs.getMethod(pname);
            if (m != null) {
                Object v = m.invoke(n);
                if (v instanceof ObservableValue val) {
                    Class<?> tp = pd.getPropertyType();
                    String type = tp == null ? "<null>" : tp.getSimpleName();
                    return new Entry(name, type, val);
                }
            }
        } catch (Throwable e) {
            // ignore
        }
        return null;
    }

    private static void highlight(Entry en) {
        if (en.setHighlight()) {
            highlighted.add(en);
            if (timeline == null) {
                timeline = new Timeline(
                    new KeyFrame(Duration.millis(250), (ev) -> {
                        clearExpiredHighlights();
                    }));
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
            }
        }
    }

    private static void clearExpiredHighlights() {
        Iterator<Entry> it = highlighted.iterator();
        while (it.hasNext()) {
            Entry en = it.next();
            if (en.checkHighlightExpired()) {
                it.remove();
            }
        }
        if (highlighted.isEmpty()) {
            timeline.stop();
            timeline = null;
        }
    }

    // a name-value or a header
    static class Entry {
        private final String name;
        private String type;
        private final Observable prop;
        private SimpleObjectProperty<Object> value;
        private SimpleBooleanProperty highlighted;
        private long expiration = -1;

        public Entry(String name, String type, Observable p) {
            this.name = name;
            this.type = type;
            this.prop = p;
        }

        public SimpleBooleanProperty highlightedProperty() {
            if (highlighted == null) {
                highlighted = new SimpleBooleanProperty();
            }
            return highlighted;
        }

        public boolean setHighlight() {
            if(expiration < 0) {
                // suppress first initialization
                expiration = 0;
                return false;
            }
            highlightedProperty().set(true);
            expiration = System.currentTimeMillis() + HIGHLIGHT_DURATION;
            return true;
        }

        public boolean checkHighlightExpired() {
            if (System.currentTimeMillis() >= expiration) {
                highlightedProperty().set(false);
                return true;
            }
            return false;
        }

        public boolean isHeader() {
            return type == null;
        }

        public String getName() {
            return name;
        }

        public SimpleObjectProperty<Object> valueProperty() {
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
                    } else if (prop instanceof ObservableMap p) {
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
            valueProperty().set(x);
            highlight(this);
        }

        public String getType() {
            return type;
        }

        public void setType(String s) {
            type = s;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    // for use with Platform Preferences
    static class PrefRoot
        extends TreeItem<Entry>
        implements MapChangeListener<String, Object>
    {
        private HashMap<String,Entry> props = new HashMap<>();

        public PrefRoot() {
            super(null);

            TreeItem<Entry> ti = new TreeItem<>(new Entry("Platform", null, null));
            ti.setExpanded(true);
            getChildren().add(ti);
            {
                Entry en = new Entry("accessibilityActive", "Boolean", Platform.accessibilityActiveProperty());
                ti.getChildren().add(new TreeItem<>(en));
            }

            Platform.Preferences pref = Platform.getPreferences();

            ti = new TreeItem<>(new Entry("Platform.Preferences", null, null));
            ti.setExpanded(true);
            getChildren().add(ti);
            {
                ArrayList<Entry> a = new ArrayList<>();
                try {
                    BeanInfo inf = Introspector.getBeanInfo(Platform.Preferences.class);
                    PropertyDescriptor[] ps = inf.getPropertyDescriptors();
                    for (PropertyDescriptor p: ps) {
                        Entry en = createEntry(Platform.Preferences.class, pref, p);
                        if (en != null) {
                            a.add(en);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                addSorted(ti, a);
            }

            ti = new TreeItem<>(new Entry("All Properties", null, null));
            ti.setExpanded(true);
            getChildren().add(ti);
            {
                ArrayList<Entry> a = new ArrayList<>();
                for (String k: pref.keySet()) {
                    Object v = pref.get(k);
                    SimpleObjectProperty p = new SimpleObjectProperty(v);
                    String type = v == null ? "<null>" : v.getClass().getSimpleName();
                    Entry en = new Entry(k, type, p);
                    props.put(k, en);
                    a.add(en);
                }
                addSorted(ti, a);
            }

            pref.addListener(this);
        }

        public void disconnect() {
            Platform.getPreferences().removeListener(this);
        }

        @Override
        public void onChanged(Change<? extends String, ? extends Object> change) {
            String key = change.getKey();
            Entry en = props.get(key);
            if (en != null) {
                Object v = Platform.getPreferences().get(key);
                if (v != null) {
                    en.setType(v.getClass().getSimpleName());
                }
                en.setValue(v);
            }
        }
    }
}
