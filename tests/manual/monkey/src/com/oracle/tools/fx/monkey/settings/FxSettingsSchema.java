/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Shape;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
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

    // TODO add type-specific suffix
    private static String getName(WindowMonitor m, Node n) {
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
        // FIX parent is null, so it's not yet connected (probably because of the skin)
        if (p != null) {
            if (collectNames(sb, p)) {
                return true;
            }
        }
        sb.append('.');
        String name = n.getId();
        if ((name == null) || (name.trim().length() == 0)) {
            name = n.getClass().getSimpleName();
        }
        sb.append(name);
        return false;
    }

    public static void storeNode(WindowMonitor m, Node n) {
        //System.out.println("storeNode " + n); // FIX
        if (n instanceof ListView lv) {
            storeListView(m, lv);
        } else if (n instanceof ComboBox cb) {
            storeComboBox(m, cb);
        } else if (n instanceof CheckBox cb) {
            storeCheckBox(m, cb);
        }

        if (n instanceof SplitPane sp) {
            for (Node ch: sp.getItems()) {
                storeNode(m, ch);
            }
        }

        if (n instanceof Parent p) {
            for (Node ch: p.getChildrenUnmodifiable()) {
                storeNode(m, ch);
            }
        }
    }

    public static void restoreNode(Node n) {
        if (checkNoScene(n)) {
            return;
        }

        WindowMonitor m = WindowMonitor.getFor(n);

        //System.out.println("restoreNode " + n); // FIX
        if (n instanceof ListView lv) {
            restoreListView(m, lv);
        } else if (n instanceof ComboBox cb) {
            restoreComboBox(m, cb);
        } else if (n instanceof CheckBox cb) {
            restoreCheckBox(m, cb);
        }

        if (n instanceof SplitPane sp) {
            for (Node ch: sp.getItems()) {
                restoreNode(ch);
            }
        }

        if (n instanceof Parent p) {
            for (Node ch: p.getChildrenUnmodifiable()) {
                restoreNode(ch);
            }
        }
    }

    private static void storeComboBox(WindowMonitor m, ComboBox n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        int ix = n.getSelectionModel().getSelectedIndex();
        if (ix < 0) {
            return;
        }

        String name = getName(m, n);
        if (name == null) {
            return;
        }

        FxSettings.setInt(PREFIX + name, ix);
    }

    // TODO perhaps operate with selection model instead
    private static void restoreComboBox(WindowMonitor m, ComboBox n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        if (checkNoScene(n)) {
            return;
        }

        String name = getName(m, n);
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

    private static boolean checkNoScene(Node n) {
        if (n.getScene() == null) {
            class ChLi implements ChangeListener<Scene> {
                private final Node node;

                public ChLi(Node n) {
                    this.node = n;
                }

                @Override
                public void changed(ObservableValue<? extends Scene> src, Scene old, Scene scene) {
                    if (scene != null) {
                        Window w = scene.getWindow();
                        if (w != null) {
                            n.sceneProperty().removeListener(this);
                            restoreNode(n);
                            FxSettings.restore(n);
                        }
                    }
                }
            }
            ;

            n.sceneProperty().addListener(new ChLi(n));

            return true;
        }
        return false;
    }

    private static void storeListView(WindowMonitor m, ListView n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        int ix = n.getSelectionModel().getSelectedIndex();
        if (ix < 0) {
            return;
        }

        String name = getName(m, n);
        if (name == null) {
            return;
        }

        FxSettings.setInt(PREFIX + name, ix);
    }

    private static void restoreListView(WindowMonitor m, ListView n) {
        if (n.getSelectionModel() == null) {
            return;
        }

        if (checkNoScene(n)) {
            return;
        }

        String name = getName(m, n);
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

    private static void storeCheckBox(WindowMonitor m, CheckBox n) {
        String name = getName(m, n);
        if (name == null) {
            return;
        }

        boolean sel = n.isSelected();
        FxSettings.setBoolean(PREFIX + name, sel);
    }

    private static void restoreCheckBox(WindowMonitor m, CheckBox n) {
        if (checkNoScene(n)) {
            return;
        }

        String name = getName(m, n);
        if (name == null) {
            return;
        }

        Boolean sel = FxSettings.getBoolean(PREFIX + name);
        if (sel == null) {
            return;
        }

        n.setSelected(sel);
    }
}
