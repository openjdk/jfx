/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxDock
package com.oracle.demo.richtext.settings;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
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
 *
 * @author Andy Goryachev
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
        }

        if (n instanceof Parent p) {
            for (Node ch: p.getChildrenUnmodifiable()) {
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
        }

        if (n instanceof Parent p) {
            for (Node ch: p.getChildrenUnmodifiable()) {
                restoreNode(ch);
            }
        }
    }

    private static void storeSplitPane(SplitPane sp) {
        double[] div = sp.getDividerPositions();
        SStream ss = SStream.writer();
        ss.add(div.length);
        for (int i = 0; i < div.length; i++) {
            ss.add(div[i]);
        }
        String name = computeName(sp);
        FxSettings.setStream(PREFIX + name, ss);

        for (Node ch: sp.getItems()) {
            storeNode(ch);
        }
    }

    private static void restoreSplitPane(SplitPane sp) {
        for (Node ch: sp.getItems()) {
            restoreNode(ch);
        }

        /** FIX getting smaller and smaller
        String name = getName(m, sp);
        SStream ss = FxSettings.getStream(PREFIX + name);
        if (ss != null) {
            int ct = ss.nextInt(-1);
            if (ct > 0) {
                for (int i = 0; i < ct; i++) {
                    double div = ss.nextDouble(-1);
                    if (div < 0) {
                        break;
                    }
                    sp.setDividerPosition(i, div);
                }
            }
        }
        */
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
}
