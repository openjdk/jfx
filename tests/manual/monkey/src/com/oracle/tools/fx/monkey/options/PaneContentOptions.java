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
package com.oracle.tools.fx.monkey.options;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.Utils;
import com.oracle.tools.fx.monkey.util.VerticalLabel;

/**
 * Pane Content Options.
 */
public class PaneContentOptions {
    public static Node createOptions(ObservableList<Node> children, Supplier<Builder> b) {
        ObjectSelector<List<Node>> s = new ObjectSelector<>("children", (cs) -> {
            children.setAll(cs);
        });
        s.addChoiceSupplier("progressive max", () -> {
            return b.get().
                d().max(30).
                d().max(31).
                d().max(32).
                d().max(33).
                d().max(34).
                d().max(35).
                d().max(36).
                d().max(37).
                d().max(38).
                d().max(39).
                d().max(40).
                d().max(41).
                d().max(30).
                build();
        });
        s.addChoiceSupplier("progressive min", () -> {
            return b.get().
                d().min(30).
                d().min(31).
                d().min(32).
                d().min(33).
                d().min(34).
                d().min(35).
                d().min(36).
                d().min(37).
                d().min(38).
                d().min(39).
                d().min(40).
                d().min(41).
                d().min(30).
                build();
        });
        s.addChoiceSupplier("fractional prefs", () -> {
            return b.get().
                d().pref(25.3).
                d().pref(25.3).
                d().pref(25.4).
                d().pref(25.3).max(100).
                d().pref(25.3).max(101).
                d().pref(25.4).
                build();
        });
        s.addChoiceSupplier("fill + max", () -> {
            return b.get().
                d().fill().
                d().max(200).
                build();
        });
        s.addChoiceSupplier("pref only", () -> {
            return b.get().
                d().pref(100).
                d().pref(150).
                d().pref(200).
                d().pref(250).
                build();
        });
        s.addChoiceSupplier("all set: min, pref, max", () -> {
            return b.get().
                d().
                d().min(20).pref(30).max(50).
                d().pref(200).
                d().pref(300).max(400).
                d().
                build();
        });
        s.addChoiceSupplier("min width", () -> {
            return b.get().
                d().
                d().
                d().
                d().min(300).
                build();
        });
        s.addChoiceSupplier("max width progressive", () -> {
            return b.get().
                d().max(30).fill().
                d().max(31).fill().
                d().max(32).fill().
                d().max(33).fill().
                d().max(34).fill().
                d().max(35).fill().
                d().max(36).fill().
                d().max(37).fill().
                d().max(38).fill().
                d().max(39).fill().
                build();
        });
        s.addChoiceSupplier("min width (beginning)", () -> {
            return b.get().
                d().min(300).
                d().min(300).
                d().
                d().
                d().
                d().
                build();
        });
        s.addChoiceSupplier("max width (beginning)", () -> {
            return b.get().
                d().max(300).
                d().max(300).
                d().
                d().
                d().
                d().
                build();
        });
        s.addChoiceSupplier("fixed width (beginning)", () -> {
            return b.get().
                d().min(100).max(100).
                d().min(100).max(100).
                d().
                d().
                d().
                d().
                build();
        });
        s.addChoiceSupplier("min width (middle)", () -> {
            return b.get().
                d().
                d().
                d().min(300).
                d().min(300).
                d().
                d().
                build();
        });
        s.addChoiceSupplier("max width (middle)", () -> {
            return b.get().
                d().
                d().
                d().max(300).
                d().max(300).
                d().
                d().
                build();
        });
        s.addChoiceSupplier("fixed width (middle)", () -> {
            return b.get().
                d().
                d().
                d().min(100).max(100).
                d().min(100).max(100).
                d().
                d().
                build();
        });
        s.addChoiceSupplier("min width (end)", () -> {
            return b.get().
                d().
                d().
                d().
                d().
                d().min(300).
                d().min(300).
                build();
        });
        s.addChoiceSupplier("max width (end)", () -> {
            return b.get().
                d().
                d().
                d().
                d().
                d().max(300).
                d().max(300).
                build();
        });
        s.addChoiceSupplier("fixed width (end)", () -> {
            return b.get().
                d().
                d().
                d().
                d().
                d().min(100).max(100).
                d().min(100).max(100).
                build();
        });
        s.addChoiceSupplier("all fixed", () -> {
            return b.get().
                d().min(70).max(70).
                d().min(70).max(70).
                d().min(70).max(70).
                d().min(70).max(70).
                build();
        });
        s.addChoiceSupplier("all max", () -> {
            return b.get().
                d().max(70).
                d().max(70).
                d().max(70).
                d().max(70).
                build();
        });
        s.addChoiceSupplier("16 items, pref=30", () -> {
            return b.get().
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                d().pref(30).
                build();
        });
        s.addChoiceSupplier("48 items, pref=20", () -> {
            return b.get().
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                d().pref(20).
                build();
        });
        s.addChoiceSupplier("various", () -> {
            return b.get().
                d().pref(100).
                d().pref(200).
                d().pref(300).
                d().min(100).max(100).
                d().pref(100).
                d().min(100).
                d().max(100).
                d().pref(300).
                d().
                build();
        });
        childChoices((text, gen) -> {
            s.addChoiceSupplier(text, () -> {
                Node n = gen.get();
                return List.of(n);
            });
        });
        s.selectFirst();
        return s;
    }

    public static Node manyTextNodes(int count) {
        TextFlow f = new TextFlow();
        for (int i = 0; i < count; i++) {
            Text t = new Text(i + " ");
            t.setFill(i % 2 == 0 ? Color.BLACK : Color.GREEN);
            t.getStyleClass().add("T1000");
            f.getChildren().add(t);

            t.setOnContextMenuRequested((ev) -> {
                ContextMenu m = new ContextMenu();
                FX.item(m, "Show Properties Monitor...", () -> {
                    PropertiesMonitor.open(t);
                });
                FX.item(m, "Delete", () -> {
                    if (t.getParent() instanceof Pane p) {
                        p.getChildren().remove(t);
                    }
                });
                m.show(t, ev.getScreenX(), ev.getScreenY());
            });
        }
        return f;
    }

    public static Region rectangle(double minw, double minh, double prefw, double prefh, double maxw, double maxh) {
        Region r = new Region();
        if (minw > 0) {
            r.setMinWidth(minw);
        }
        if (minh > 0) {
            r.setMinHeight(minh);
        }
        if (prefw > 0) {
            r.setPrefWidth(prefw);
        }
        if (prefh > 0) {
            r.setPrefHeight(prefh);
        }
        if (maxw > 0) {
            r.setMaxWidth(maxw);
        }
        if (maxh > 0) {
            r.setMaxHeight(maxh);
        }
        r.setBackground(Background.fill(Utils.nextColor()));
        return r;
    }

    public static Node verticalLabel(VerticalDirection dir) {
        VerticalLabel n = new VerticalLabel(dir, "Label with the VERTICAL content orientation; direction = " + dir);
        n.setBackground(Background.fill(Utils.nextColor()));
        return n;
    }

    public static Node horizontalLabel() {
        Label n = new Label("Label with the HORIZONTAL content orientation.");
        n.setWrapText(true);
        n.setBackground(Background.fill(Utils.nextColor()));
        return n;
    }

    public static void addChildOption(ObservableList<MenuItem> menu, ObservableList<Node> children, Consumer<Node> cx) {
        childChoices((text, gen) -> {
            MenuItem mi = new MenuItem(text);
            mi.setOnAction((ev) -> {
                Node n = gen.get();
                if (cx != null) {
                    cx.accept(n);
                }
                children.add(n);
            });
            menu.add(mi);
        });
    }

    public static Node childOption(String name, ObjectProperty<Node> p, Consumer<Node> cx) {
        ObjectOption<Node> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        childChoices((text, gen) -> {
            op.addChoiceSupplier(text, () -> {
                Node n = gen.get();
                if (cx != null) {
                    cx.accept(n);
                }
                return n;
            });
        });
        op.selectInitialValue();
        return op;
    }

    private static void childChoices(BiConsumer<String, Supplier<Node>> client) {
        client.accept("1000 text nodes", () -> {
            return PaneContentOptions.manyTextNodes(1000);
        });
        client.accept("Label", () -> {
            return new Label("Label");
        });
        client.accept("Biased Label: DOWN", () -> {
            return verticalLabel(VerticalDirection.DOWN);
        });
        client.accept("Biased Label: UP", () -> {
            return verticalLabel(VerticalDirection.UP);
        });
        client.accept("Biased Label: HORIZONTAL", () -> {
            return horizontalLabel();
        });
        client.accept("Min (200 x 100)", () -> {
            return rectangle(200, 100, -1, -1, -1, -1);
        });
        client.accept("Pref (333.3 x 222.2)", () -> {
            return rectangle(-1, -1, 333.3, 222.2, -1, -1);
        });
        client.accept("Max (600 x 500)", () -> {
            return rectangle(-1, -1, -1, -1, 600, 500);
        });
        client.accept("Min (50 x 75), Pref(150 x 99)", () -> {
            return rectangle(50, 75, 150, 99, -1, -1);
        });
        client.accept("Pref (300 x 30), Max (600 x 500)", () -> {
            return rectangle(-1, -1, 300, 30, 600, 500);
        });
    }

    public static abstract class Builder {
        protected abstract void setMin(Region r, double v);

        protected abstract void setPref(Region r, double v);

        protected abstract void setMax(Region r, double v);

        protected abstract void setGrow(Node n, Priority p);

        private final Function<List<Node>, Region> creator;
        private final ArrayList<Node> children = new ArrayList<>();

        public Builder(Function<List<Node>, Region> creator) {
            this.creator = creator;
        }

        public Builder d() {
            creator.apply(children);
            return this;
        }

        public Builder max(double v) {
            setMax(last(), v);
            return this;
        }

        public Builder min(double v) {
            setMin(last(), v);
            return this;
        }

        public Builder pref(double v) {
            setPref(last(), v);
            return this;
        }

        public Builder fill() {
            setGrow(last(), Priority.ALWAYS);
            return this;
        }

        private Region last() {
            return (Region)children.get(children.size() - 1);
        }

        public List<Node> build() {
            return children;
        }
    }
}
