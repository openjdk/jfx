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
package com.oracle.demo.richtext.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.oracle.demo.richtext.settings.FxSettingsSchema;

/**
 * Shortcuts and convenience methods that perhaps could be added to JavaFX.
 */
public class FX {
    public static Menu menu(MenuBar b, String text) {
        Menu m = new Menu(text);
        applyMnemonic(m);
        b.getMenus().add(m);
        return m;
    }

    public static Menu menu(ContextMenu b, String text) {
        Menu m = new Menu(text);
        applyMnemonic(m);
        b.getItems().add(m);
        return m;
    }

    public static MenuItem item(MenuBar b, String text, Runnable action) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        mi.setOnAction((ev) -> action.run());
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static MenuItem item(MenuBar b, MenuItem mi) {
        applyMnemonic(mi);
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static MenuItem item(MenuBar b, String text) {
        MenuItem mi = new MenuItem(text);
        mi.setDisable(true);
        applyMnemonic(mi);
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static MenuItem item(MenuBar b, String text, FxAction a) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        lastMenu(b).getItems().add(mi);
        a.attach(mi);
        return mi;
    }

    public static CheckMenuItem checkItem(MenuBar b, String text, FxAction a) {
        CheckMenuItem mi = new CheckMenuItem(text);
        applyMnemonic(mi);
        lastMenu(b).getItems().add(mi);
        a.attach(mi);
        return mi;
    }

    public static MenuItem item(ContextMenu cm, String text, FxAction a) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        cm.getItems().add(mi);
        a.attach(mi);
        return mi;
    }

    public static MenuItem item(ContextMenu cm, String text) {
        MenuItem mi = new MenuItem(text);
        mi.setDisable(true);
        applyMnemonic(mi);
        cm.getItems().add(mi);
        return mi;
    }

    public static MenuItem item(Menu b, String text) {
        MenuItem mi = new MenuItem(text);
        mi.setDisable(true);
        applyMnemonic(mi);
        b.getItems().add(mi);
        return mi;
    }

    public static MenuItem item(Menu b, String text, Runnable r) {
        MenuItem mi = new MenuItem(text);
        mi.setOnAction((ev) -> r.run());
        applyMnemonic(mi);
        b.getItems().add(mi);
        return mi;
    }

    public static Menu submenu(MenuBar b, String text) {
        Menu m = new Menu(text);
        applyMnemonic(m);
        lastMenu(b).getItems().add(m);
        return m;
    }

    private static void applyMnemonic(MenuItem m) {
        String text = m.getText();
        if (text != null) {
            if (text.contains("_")) {
                m.setMnemonicParsing(true);
            }
        }
    }

    private static Menu lastMenu(MenuBar b) {
        List<Menu> ms = b.getMenus();
        return ms.get(ms.size() - 1);
    }

    public static SeparatorMenuItem separator(MenuBar b) {
        SeparatorMenuItem s = new SeparatorMenuItem();
        lastMenu(b).getItems().add(s);
        return s;
    }

    public static SeparatorMenuItem separator(ContextMenu m) {
        SeparatorMenuItem s = new SeparatorMenuItem();
        m.getItems().add(s);
        return s;
    }

    public static RadioMenuItem radio(MenuBar b, String text, KeyCombination accelerator, ToggleGroup g) {
        RadioMenuItem mi = new RadioMenuItem(text);
        mi.setAccelerator(accelerator);
        mi.setToggleGroup(g);
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static CheckMenuItem checkItem(ContextMenu c, String name, boolean selected, Consumer<Boolean> client) {
        CheckMenuItem m = new CheckMenuItem(name);
        m.setSelected(selected);
        m.setOnAction((ev) -> {
            boolean on = m.isSelected();
            client.accept(on);
        });
        c.getItems().add(m);
        return m;
    }

    public static CheckMenuItem checkItem(Menu c, String name, boolean selected, Consumer<Boolean> client) {
        CheckMenuItem m = new CheckMenuItem(name);
        m.setSelected(selected);
        m.setOnAction((ev) -> {
            boolean on = m.isSelected();
            client.accept(on);
        });
        c.getItems().add(m);
        return m;
    }

    public static ToggleButton toggleButton(ToolBar t, String text, FxAction a) {
        ToggleButton b = new ToggleButton(text);
        a.attach(b);
        t.getItems().add(b);
        return b;
    }

    public static ToggleButton toggleButton(ToolBar t, String text, String tooltip, FxAction a) {
        ToggleButton b = new ToggleButton(text);
        b.setTooltip(new Tooltip(tooltip));
        a.attach(b);
        t.getItems().add(b);
        return b;
    }

    public static ToggleButton toggleButton(ToolBar t, String text, String tooltip) {
        ToggleButton b = new ToggleButton(text);
        b.setTooltip(new Tooltip(tooltip));
        b.setDisable(true);
        t.getItems().add(b);
        return b;
    }

    public static Button button(ToolBar t, String text, String tooltip, FxAction a) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip(tooltip));
        a.attach(b);
        t.getItems().add(b);
        return b;
    }

    public static Button button(ToolBar t, String text, String tooltip) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip(tooltip));
        b.setDisable(true);
        t.getItems().add(b);
        return b;
    }

    public static <N extends Node> N add(ToolBar t, N child) {
        t.getItems().add(child);
        return child;
    }

    public static void space(ToolBar t) {
        Pane p = new Pane();
        p.setPrefSize(10, 10);
        t.getItems().add(p);
    }

    public static void tooltip(Control c, String text) {
        c.setTooltip(new Tooltip(text));
    }

    public static void add(GridPane p, Node n, int col, int row) {
        p.getChildren().add(n);
        GridPane.setConstraints(n, col, row);
    }

    public static <T> void select(ComboBox<T> cb, T value) {
        cb.getSelectionModel().select(value);
    }

    public static <T> void selectFirst(ComboBox<T> cb) {
        cb.getSelectionModel().selectFirst();
    }

    public static <T> T getSelectedItem(ComboBox<T> cb) {
        return cb.getSelectionModel().getSelectedItem();
    }

    public static Window getParentWindow(Object nodeOrWindow) {
        if (nodeOrWindow == null) {
            return null;
        } else if (nodeOrWindow instanceof Window w) {
            return w;
        } else if (nodeOrWindow instanceof Node n) {
            Scene s = n.getScene();
            if (s != null) {
                return s.getWindow();
            }
            return null;
        } else {
            throw new Error("Node or Window only");
        }
    }

    /** cascades the window relative to its owner, if any */
    public static void cascade(Stage w) {
        if (w != null) {
            Window p = w.getOwner();
            if (p != null) {
                double x = p.getX();
                double y = p.getY();
                double off = 20;
                w.setX(x + off);
                w.setY(y + off);
            }
        }
    }

    /** adds a name property to the Node for the purposes of storing the preferences */
    public static void name(Node n, String name) {
        FxSettingsSchema.setName(n, name);
    }

    /** adds a name property to the Window for the purposes of storing the preferences */
    public static void name(Window w, String name) {
        FxSettingsSchema.setName(w, name);
    }

    /**
     * attach a popup menu to a node.
     * WARNING: sometimes, as the case is with TableView/FxTable header,
     * the requested node gets created by the skin at some later time.
     * In this case, additional dance must be performed, see for example
     * FxTable.setHeaderPopupMenu()
     */
    // https://github.com/andy-goryachev/MP3Player/blob/8b0ff12460e19850b783b961f214eacf5e1cdaf8/src/goryachev/fx/FX.java#L1251
    public static void setPopupMenu(Node owner, Supplier<ContextMenu> generator) {
        if (owner == null) {
            throw new NullPointerException("cannot attach popup menu to null");
        }

        owner.setOnContextMenuRequested((ev) -> {
            if (generator != null) {
                ContextMenu m = generator.get();
                if (m != null) {
                    if (m.getItems().size() > 0) {
                        Platform.runLater(() -> {
                            // javafx does not dismiss the popup when the user
                            // clicks on the owner node
                            EventHandler<MouseEvent> li = new EventHandler<MouseEvent>() {
                                @Override
                                public void handle(MouseEvent event) {
                                    m.hide();
                                    owner.removeEventFilter(MouseEvent.MOUSE_PRESSED, this);
                                    event.consume();
                                }
                            };

                            owner.addEventFilter(MouseEvent.MOUSE_PRESSED, li);
                            m.show(owner, ev.getScreenX(), ev.getScreenY());
                        });
                        ev.consume();
                    }
                }
            }
            ev.consume();
        });
    }

    /**
     * Sets opacity (alpha) value.
     * @param c the initial color
     * @param opacity the opacity value
     * @return the new Color with specified opacity
     */
    public static Color alpha(Color c, double opacity) {
        double r = c.getRed();
        double g = c.getGreen();
        double b = c.getBlue();
        return new Color(r, g, b, opacity);
    }

    /**
     * Returns the node of type {@code type}, which is either the ancestor or the specified node,
     * or the specified node itself.
     * @param <N> the class of Node
     * @param type the class of Node
     * @param n the node to look at
     * @return the ancestor of type N, or null
     */
    public static <N extends Node> N findParentOf(Class<N> type, Node n) {
        for (;;) {
            if (n == null) {
                return null;
            } else if (type.isAssignableFrom(n.getClass())) {
                return (N)n;
            }
            n = n.getParent();
        }
    }

    /**
     * Adds the specified style name to the Node's style list.
     * @param n the node
     * @param name the style name to add
     */
    public static void style(Node n, String name) {
        if (n != null) {
            n.getStyleClass().add(name);
        }
    }

    /**
     * Adds or removes the specified style name to the Node's style list.
     * @param n the node
     * @param name the style name to add
     * @param add whether to add or remove the style
     */
    public static void style(Node n, String name, boolean add) {
        if (n != null) {
            if (add) {
                n.getStyleClass().add(name);
            } else {
                n.getStyleClass().remove(name);
            }
        }
    }

    /**
     * Adds or removes the specified pseudo class to the Node's style list.
     * @param n the node
     * @param name the style name to add
     * @param on whether to add or remove the pseudo class
     */
    public static void style(Node n, PseudoClass name, boolean on) {
        if (n != null) {
            n.pseudoClassStateChanged(name, on);
        }
    }

    public static Button button(String text, Runnable r) {
        Button b = new Button(text);
        if (r == null) {
            b.setDisable(true);
        } else {
            b.setOnAction((ev) -> r.run());
        }
        return b;
    }
}
