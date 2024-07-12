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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxDock
package com.oracle.tools.fx.monkey.settings;

import java.util.List;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Shape;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Constants and methods used to persist settings.
 */
public class FxSettingsSchema {
    private static final String PREFIX = "FX.";
    private static final String WINDOW_NORMAL = "N";
    private static final String WINDOW_ICONIFIED = "I";
    private static final String WINDOW_MAXIMIZED = "M";
    private static final String WINDOW_FULLSCREEN = "F";
    private static final Object NAME_PROP = new Object();

    public static void storeWindow(WindowMonitor m, Window w) {
        SStream ss = SStream.writer();
        ss.add(m.getX());
        ss.add(m.getY());
        ss.add(m.getWidth());
        ss.add(m.getHeight());
        if (w instanceof Stage s) {
            if (s.isIconified()) {
                ss.add(WINDOW_ICONIFIED);
            } else if (s.isMaximized()) {
                ss.add(WINDOW_MAXIMIZED);
            } else if (s.isFullScreen()) {
                ss.add(WINDOW_FULLSCREEN);
            } else {
                ss.add(WINDOW_NORMAL);
            }
        }
        FxSettings.setStream(PREFIX + m.getID(), ss);
    }

    public static void restoreWindow(WindowMonitor m, Window win) {
        SStream ss = FxSettings.getStream(PREFIX + m.getID());
        if (ss == null) {
            return;
        }

        double x = ss.nextDouble(-1);
        double y = ss.nextDouble(-1);
        double w = ss.nextDouble(-1);
        double h = ss.nextDouble(-1);
        String t = ss.nextString(WINDOW_NORMAL);

        if ((w > 0) && (h > 0)) {
            if (isValid(x, y)) {
                win.setX(x);
                win.setY(y);
            }

            if (win instanceof Stage s) {
                if (s.isResizable()) {
                    s.setWidth(w);
                    s.setHeight(h);
                }

                switch (t) {
                case WINDOW_FULLSCREEN:
                    s.setFullScreen(true);
                    break;
                case WINDOW_MAXIMIZED:
                    s.setMaximized(true);
                    break;
                // TODO iconified?
                }
            }
        }
    }

    private static boolean isValid(double x, double y) {
        for (Screen s: Screen.getScreens()) {
            Rectangle2D r = s.getVisualBounds();
            if (r.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static String computeName(Node n) {
        WindowMonitor m = WindowMonitor.getFor(n);
        if (m == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (collectNames(sb, n)) {
            return null;
        }

        String id = m.getID();
        return id + sb;
    }

    // returns true if Node should be ignored
    private static boolean collectNames(StringBuilder sb, Node n) {
        if (n instanceof MenuBar) {
            return true;
        } else if (n instanceof Shape) {
            return true;
        } else if (n instanceof ImageView) {
            return true;
        }

        Parent p = n.getParent();
        if (p != null) {
            if (collectNames(sb, p)) {
                return true;
            }
        }

        String name = getNodeName(n);
        if (name == null) {
            return true;
        }

        sb.append('.');
        sb.append(name);
        return false;
    }

    private static String getNodeName(Node n) {
        if (n != null) {
            String name = getName(n);
            if (name != null) {
                return name;
            }

            if (n instanceof Pane) {
                if (n instanceof AnchorPane) {
                    return "AnchorPane";
                } else if (n instanceof BorderPane) {
                    return "BorderPane";
                } else if (n instanceof DialogPane) {
                    return "DialogPane";
                } else if (n instanceof FlowPane) {
                    return "FlowPane";
                } else if (n instanceof GridPane) {
                    return "GridPane";
                } else if (n instanceof HBox) {
                    return "HBox";
                } else if (n instanceof StackPane) {
                    return "StackPane";
                } else if (n instanceof TilePane) {
                    return "TilePane";
                } else if (n instanceof VBox) {
                    return "VBox";
                } else {
                    return "Pane";
                }
            } else if (n instanceof Group) {
                return "Group";
            } else if (n instanceof Region) {
                return "Region";
            }
        }
        return null;
    }

    private static List<? extends Node> getChildren(Node n) {
        if(n instanceof Accordion a) {
            return a.getPanes();
        }

        if (n instanceof Parent p) {
            return p.getChildrenUnmodifiable();
        }

        return null;
    }

    private static void storeSplitPane(SplitPane sp) {
        String name = computeName(sp);
        if (name == null) {
            return;
        }

        double[] div = sp.getDividerPositions();
        SStream ss = SStream.writer();
        ss.add(div.length);
        for (int i = 0; i < div.length; i++) {
            ss.add(div[i]);
        }
        FxSettings.setStream(PREFIX + name, ss);

        for (Node ch: sp.getItems()) {
            storeNode(ch);
        }
    }

    private static void restoreSplitPane(SplitPane sp) {
        if (checkNoScene(sp)) {
            return;
        }

        String name = computeName(sp);
        if (name == null) {
            return;
        }

        for (Node ch: sp.getItems()) {
            restoreNode(ch);
        }

        SStream ss = FxSettings.getStream(PREFIX + name);
        if (ss != null) {
            int sz = ss.nextInt(-1);
            if (sz > 0) {
                double[] divs = new double[sz];
                for (int i = 0; i < sz; i++) {
                    double v = ss.nextDouble(-1);
                    if (v < 0) {
                        return;
                    }
                    divs[i] = v;
                }

                // FIX some kind of a bug, the dividers move slightly each time
                sp.setDividerPositions(divs);
                Platform.runLater(() -> {
                    sp.setDividerPositions(divs);
                });
            }
        }
    }

    private static void storeComboBox(ComboBox n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        int ix = n.getSelectionModel().getSelectedIndex();
        if (ix < 0) {
            return;
        }

        String name = computeName(n);
        if (name == null) {
            return;
        }

        FxSettings.setInt(PREFIX + name, ix);
    }

    // TODO perhaps operate with selection model instead
    private static void restoreComboBox(ComboBox n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        if (checkNoScene(n)) {
            return;
        }

        String name = computeName(n);
        if (name == null) {
            return;
        }

        int ix = FxSettings.getInt(PREFIX + name, -1);
        if (ix < 0) {
            return;
        } else if (ix >= n.getItems().size()) {
            return;
        }

        n.getSelectionModel().select(ix);
    }

    private static boolean checkNoScene(Node node) {
        if (node == null) {
            return true;
        } else if (node.getScene() == null) {
            // delay restore until node becomes a part of the scene
            node.sceneProperty().addListener(new ChangeListener<Scene>() {
                @Override
                public void changed(ObservableValue<? extends Scene> src, Scene old, Scene scene) {
                    if (scene != null) {
                        Window w = scene.getWindow();
                        if (w != null) {
                            node.sceneProperty().removeListener(this);
                            restoreNode(node);
                        }
                    }
                }
            });
            return true;
        }
        return false;
    }

    private static void storeListView(ListView n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        int ix = n.getSelectionModel().getSelectedIndex();
        if (ix < 0) {
            return;
        }

        String name = computeName(n);
        if (name == null) {
            return;
        }

        FxSettings.setInt(PREFIX + name, ix);
    }

    private static void restoreListView(ListView n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        if (checkNoScene(n)) {
            return;
        }

        String name = computeName(n);
        if (name == null) {
            return;
        }

        int ix = FxSettings.getInt(PREFIX + name, -1);
        if (ix < 0) {
            return;
        } else if (ix >= n.getItems().size()) {
            return;
        }

        n.getSelectionModel().select(ix);
    }

    private static void storeCheckBox(CheckBox n) {
        String name = computeName(n);
        if (name == null) {
            return;
        }

        boolean sel = n.isSelected();
        FxSettings.setBoolean(PREFIX + name, sel);
    }

    private static void restoreCheckBox(CheckBox n) {
        if (checkNoScene(n)) {
            return;
        }

        String name = computeName(n);
        if (name == null) {
            return;
        }

        Boolean sel = FxSettings.getBoolean(PREFIX + name);
        if (sel == null) {
            return;
        }

        n.setSelected(sel);
    }

    /** sets the name for the purposes of storing user preferences */
    public static void setName(Node n, String name) {
        n.getProperties().put(NAME_PROP, name);
    }

    /** sets the name for the purposes of storing user preferences */
    public static void setName(Window w, String name) {
        w.getProperties().put(NAME_PROP, name);
    }

    /**
     * Returns the name for the purposes of storing user preferences,
     * set previously by {@link #setName(Node, String)},
     * or null.
     */
    public static String getName(Node n) {
        if (n != null) {
            Object x = n.getProperties().get(NAME_PROP);
            if (x instanceof String s) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns the name for the purposes of storing user preferences,
     * set previously by {@link #setName(Window, String)},
     * or null.
     */
    public static String getName(Window w) {
        if (w != null) {
            Object x = w.getProperties().get(NAME_PROP);
            if (x instanceof String s) {
                return s;
            }
        }
        return null;
    }

    public static void storeNode(Node n) {
        if (n instanceof ListView lv) {
            storeListView(lv);
            return;
        } else if (n instanceof ComboBox cb) {
            storeComboBox(cb);
            return;
        } else if (n instanceof CheckBox cb) {
            storeCheckBox(cb);
            return;
        } else if (n instanceof SplitPane sp) {
            storeSplitPane(sp);
            return;
        } else if (n instanceof ScrollPane sp) {
            storeNode(sp.getContent());
            return;
        } else if(n instanceof TitledPane tp) {
            storeNode(tp.getContent());
        }

        List<? extends Node> children = getChildren(n);
        if(children != null) {
            for (Node ch: children) {
                storeNode(ch);
            }
        }
    }

    public static void restoreNode(Node n) {
        if (checkNoScene(n)) {
            return;
        }

        if (n instanceof ListView lv) {
            restoreListView(lv);
        } else if (n instanceof ComboBox cb) {
            restoreComboBox(cb);
        } else if (n instanceof CheckBox cb) {
            restoreCheckBox(cb);
        } else if (n instanceof SplitPane sp) {
            restoreSplitPane(sp);
        } else if (n instanceof ScrollPane sp) {
            restoreNode(sp.getContent());
        } else if(n instanceof TitledPane tp) {
            restoreNode(tp.getContent());
        }

        List<? extends Node> children = getChildren(n);
        if(children != null) {
            for (Node ch: children) {
                restoreNode(ch);
            }
        }
    }
}
