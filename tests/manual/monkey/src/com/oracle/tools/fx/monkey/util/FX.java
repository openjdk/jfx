/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
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
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.oracle.tools.fx.monkey.settings.FxSettingsSchema;

/**
 * Shortcuts and convenience methods that perhaps could be added to JavaFX.
 */
public class FX {
    private static final String os = System.getProperty("os.name");
    private static final boolean WINDOWS = os.startsWith("Windows");
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux");

    public static Menu menu(MenuBar b, String text) {
        Menu m = new Menu(text);
        applyMnemonic(m);
        b.getMenus().add(m);
        return m;
    }

    public static Menu menu(ContextMenu cm, String text) {
        Menu m = new Menu(text);
        applyMnemonic(m);
        cm.getItems().add(m);
        return m;
    }

    public static final MenuItem menuItem(String text, Runnable action) {
        MenuItem mi = new MenuItem(text);
        if (action == null) {
            mi.setDisable(true);
        } else {
            mi.setOnAction((ev) -> action.run());
        }
        return mi;
    }

    public static MenuItem item(MenuBar b, String text, Runnable action) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        if (action == null) {
            mi.setDisable(true);
        } else {
            mi.setOnAction((ev) -> action.run());
        }
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static CheckMenuItem checkItem(MenuBar b, String name, BooleanProperty prop) {
        CheckMenuItem mi = new CheckMenuItem(name);
        mi.selectedProperty().bindBidirectional(prop);
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static CheckMenuItem checkItem(ContextMenu m, String name, boolean selected, Consumer<Boolean> client) {
        CheckMenuItem mi = new CheckMenuItem(name);
        mi.setSelected(selected);
        mi.selectedProperty().addListener((s, p, on) -> {
            client.accept(on);
        });
        m.getItems().add(mi);
        return mi;
    }

    public static MenuItem item(MenuBar b, MenuItem mi) {
        applyMnemonic(mi);
        lastMenu(b).getItems().add(mi);
        return mi;
    }

    public static MenuItem item(MenuBar b, String text) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        lastMenu(b).getItems().add(mi);
        return mi;
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

    public static final SeparatorMenuItem separator(ContextMenu m) {
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

    public static MenuItem item(ContextMenu cm, String text, Runnable action) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        if (action == null) {
            mi.setDisable(true);
        } else {
            mi.setOnAction((ev) -> action.run());
        }
        cm.getItems().add(mi);
        return mi;
    }

    public static MenuItem item(ContextMenu m, String name) {
        return item(m, name, null);
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

    public static Window getParentWindow(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof Window w) {
            return w;
        } else if (x instanceof Node n) {
            Scene s = n.getScene();
            if (s != null) {
                return s.getWindow();
            }
            return null;
        } else if (x instanceof MenuItem m) {
            ContextMenu cm = m.getParentPopup();
            return cm == null ? null : cm.getOwnerWindow();
        } else {
            throw new Error("Node, Window, or MenuItem only: " + x);
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

    public static String getName(Node n) {
        return FxSettingsSchema.getName(n);
    }

    /** adds a name property to the Window for the purposes of storing the preferences */
    public static void name(Window w, String name) {
        FxSettingsSchema.setName(w, name);
    }

    public static String getName(Window w) {
        return FxSettingsSchema.getName(w);
    }

    /** perhaps it should be a method in TextFlow: getTextLength() */
    public static int getTextLength(TextFlow f) {
        int len = 0;
        for (Node n : f.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                len += t.getText().length();
            } else {
                // treat non-Text nodes as having 1 character
                len++;
            }
        }
        return len;
    }

    public static boolean isWindows() {
        return WINDOWS;
    }

    public static boolean isMac() {
        return MAC;
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
        setPopupMenuLocal(owner, generator);
    }

    public static void setPopupMenu(Node owner, Function<PickResult,ContextMenu> generator) {
        setPopupMenuLocal(owner, generator);
    }

    private static void setPopupMenuLocal(Node owner, Object generator) {
        if (owner == null) {
            throw new NullPointerException("cannot attach popup menu to null");
        }

        owner.setOnContextMenuRequested((ev) -> {
            if (generator != null) {
                ContextMenu m;
                if (generator instanceof Supplier sup) {
                    m = (ContextMenu)sup.get();
                } else if (generator instanceof Function func) {
                    PickResult pick = ev.getPickResult();
                    m = (ContextMenu)func.apply(pick);
                } else {
                    m = null;
                }

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
                ev.consume();
            }
        });
    }

    public static void tooltip(Control n, String text) {
        if (text != null) {
            n.setTooltip(new Tooltip(text));
        }
    }

    public static Button button(String text, String tooltip, Runnable r) {
        Button b = button(text, r);
        tooltip(b, tooltip);
        return b;
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

    public static void style(Node n, String style) {
        n.getStyleClass().add(style);
    }

    public static void style(Node n, String style, boolean on) {
        if (on) {
            n.getStyleClass().add(style);
        } else {
            n.getStyleClass().remove(style);
        }
    }

    // borrowed from
    // https://github.com/andy-goryachev/AppFramework/blob/1e9f2197ce510a77ec5f719a2cb7112b0b6cf7be/src/goryachev/fx/FX.java#L1081
    // with the author's permission
    /** returns a parent of the specified type, or null.  if node is an instance of the specified class, returns node */
    public static <T> T getAncestorOfClass(Class<T> c, Node node) {
        if (Window.class.isAssignableFrom(c)) {
            Scene sc = node.getScene();
            if (sc != null) {
                Window w = sc.getWindow();
                while (w != null) {
                    if (w.getClass().isAssignableFrom(c)) {
                        return (T)w;
                    }

                    // the window can be a dialog, check the owner
                    if (w instanceof Stage stage) {
                        w = stage.getOwner();
                    }
                }
            }
            return null;
        } else {
            while (node != null) {
                if (c.isInstance(node)) {
                    return (T)node;
                }

                node = node.getParent();
            }
        }
        return null;
    }

    /**
     * Creates a new color with the same RGB values and the specified alpha (opacity).
     *
     * @param c the color to borrow RGB values from
     * @param alpha the opacity
     * @return the new color instance
     */
    public static Color alpha(Color c, double alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static Menu menu(Menu menu, String text) {
        Menu m = new Menu(text);
        applyMnemonic(m);
        menu.getItems().add(m);
        return m;
    }

    public static MenuItem item(Menu m, String text) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        m.getItems().add(mi);
        return mi;
    }

    public static MenuItem item(Menu m, String text, Runnable action) {
        MenuItem mi = new MenuItem(text);
        applyMnemonic(mi);
        if (action == null) {
            mi.setDisable(true);
        } else {
            mi.setOnAction((ev) -> action.run());
        }
        m.getItems().add(mi);
        return mi;
    }
}
