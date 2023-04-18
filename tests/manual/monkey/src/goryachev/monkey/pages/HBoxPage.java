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
package goryachev.monkey.pages;

import java.util.Random;
import goryachev.monkey.util.FX;
import goryachev.monkey.util.OptionPane;
import goryachev.monkey.util.TestPaneBase;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * HBox page
 */
public class HBoxPage extends TestPaneBase {
    enum Demo {
        FILL_MAX("fill + max"),
        PREF("pref only"),
        ALL("all set: min, pref, max"),
        MIN_WIDTH("min width"),
        MAX_WIDTH("max width progressive"),
        MIN_WIDTH2("min width (middle)"),
        MAX_WIDTH2("max width (middle)"),
        MIN_WIDTH3("min width (beginning)"),
        MAX_WIDTH3("max width (beginning)"),
        FIXED_MIDDLE("fixed in the middle"),
        ALL_FIXED("all fixed"),
        ALL_MAX("all with maximum width"),
        MIN_IN_CENTER("min widths set in middle columns"),
        MAX_IN_CENTER("max widths set in middle columns"),
        VARIOUS("various"),
        MANY_COLUMNS("many columns"),
        MANY_COLUMNS_SAME("many columns, same pref"),
        ;
        private final String text;
        Demo(String text) { this.text = text; }
        public String toString() { return text; }
    }

    public enum Cmd {
        COL,
        MIN,
        PREF,
        MAX,
        FILL,
    }

    protected final Cmd COL = Cmd.COL;
    protected final Cmd MIN = Cmd.MIN;
    protected final Cmd PREF = Cmd.PREF;
    protected final Cmd MAX = Cmd.MAX;
    protected final Cmd FILL = Cmd.FILL;

    protected final ComboBox<Demo> demoSelector;
    protected final CheckBox grow;
    protected HBox hbox;

    public HBoxPage() {
        setId("HBoxPage");

        // selector
        demoSelector = new ComboBox<>();
        demoSelector.setId("demoSelector");
        demoSelector.getItems().addAll(Demo.values());
        demoSelector.setEditable(false);
        demoSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updatePane();
        });

        Button addButton = new Button("Add Item");
        addButton.setOnAction((ev) -> {
            hbox.getChildren().add(newItem());
        });

        Button clearButton = new Button("Clear Items");
        clearButton.setOnAction((ev) -> {
            hbox.getChildren().clear();
        });

        grow = new CheckBox("grow");
        grow.setId("grow");
        grow.selectedProperty().addListener((s, p, on) -> {
            setGrow(on);
        });

        // layout

        OptionPane p = new OptionPane();
        p.label("Configuration:");
        p.option(demoSelector);
        p.option(addButton);
        p.option(clearButton);
        p.option(grow);
        setOptions(p);

        FX.selectFirst(demoSelector);
    }

    protected void setGrow(boolean on) {
        Priority p = on ? Priority.ALWAYS : Priority.NEVER;
        for (Node n: hbox.getChildren()) {
            HBox.setHgrow(n, p);
        }
    }

    protected Object[] createSpec(Demo d) {
        switch (d) {
        case ALL:
            return new Object[] {
                COL,
                COL, MIN, 20, PREF, 20, MAX, 20,
                COL, PREF, 200,
                COL, PREF, 300, MAX, 400,
                COL
            };
        case FILL_MAX:
            return new Object[] {
                COL, FILL,
                COL, MAX, 200
            };
        case PREF:
            return new Object[] {
                COL, PREF, 100,
                COL, PREF, 200,
                COL, PREF, 300,
                COL, PREF, 400
            };
        case MIN_WIDTH:
            return new Object[] {
                COL,
                COL,
                COL,
                COL, MIN, 300
            };
        case MAX_WIDTH:
            return new Object[] {
                COL, MAX, 30, FILL,
                COL, MAX, 31, FILL,
                COL, MAX, 32, FILL,
                COL, MAX, 33, FILL,
                COL, MAX, 34, FILL,
                COL, MAX, 35, FILL,
                COL, MAX, 36, FILL,
                COL, MAX, 37, FILL,
                COL, MAX, 38, FILL,
            };
        case MIN_WIDTH2:
            return new Object[] {
                COL,
                COL,
                COL, MIN, 300,
                COL
            };
        case MAX_WIDTH2:
            return new Object[] {
                COL,
                COL,
                COL, MAX, 100,
                COL
            };
        case MIN_WIDTH3:
            return new Object[] {
                COL, MIN, 300,
                COL,
                COL,
                COL
            };
        case MAX_WIDTH3:
            return new Object[] {
                COL, MAX, 100,
                COL,
                COL,
                COL
            };
        case MIN_IN_CENTER:
            return new Object[] {
                COL,
                COL, MIN, 20,
                COL, MIN, 30,
                COL, MIN, 40,
                COL, MIN, 50,
                COL, MIN, 60,
                COL
            };
        case MAX_IN_CENTER:
            return new Object[] {
                COL,
                COL, MAX, 20,
                COL, MAX, 30,
                COL, MAX, 40,
                COL, MAX, 50,
                COL, MAX, 60,
                COL
            };
        case FIXED_MIDDLE:
            return new Object[] {
                COL,
                COL,
                COL,
                COL, MIN, 100, MAX, 100,
                COL, MIN, 100, MAX, 100,
                COL,
                COL,
                COL
            };
        case ALL_FIXED:
            return new Object[] {
                COL, MIN, 50, MAX, 50,
                COL, MIN, 50, MAX, 50,
                COL, MIN, 50, MAX, 50
            };
        case ALL_MAX:
            return new Object[] {
                COL, MAX, 50,
                COL, MAX, 50,
                COL, MAX, 50
            };
        case VARIOUS:
            return new Object[] {
                COL, PREF, 100,
                COL, PREF, 200,
                COL, PREF, 300,
                COL, MIN, 100, MAX, 100,
                COL, PREF, 100,
                COL, MIN, 100,
                COL, MAX, 100,
                COL, PREF, 400,
                COL
            };
        case MANY_COLUMNS:
            return new Object[] {
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL,
                COL
            };
        case MANY_COLUMNS_SAME:
            return new Object[] {
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30,
                COL, PREF, 30
            };
        default:
            throw new Error("?" + d);
        }
    }

    protected HBox createPane(Demo demo, Object[] spec) {
        if ((demo == null) || (spec == null)) {
            return new HBox();
        }
        
        HBox b = new HBox();
        Region region = null;

        for (int i = 0; i < spec.length;) {
            Object x = spec[i++];
            if (x instanceof Cmd cmd) {
                switch (cmd) {
                case COL:
                    {
                        Region c = newItem();
                        b.getChildren().add(c);
                        HBox.setHgrow(c, grow.isSelected() ? Priority.ALWAYS : Priority.NEVER);
                        region = c;
                    }
                    break;
                case MAX:
                    {
                        int w = (int)(spec[i++]);
                        region.setMaxWidth(w);
                    }
                    break;
                case MIN: 
                    {
                        int w = (int)(spec[i++]);
                        region.setMinWidth(w);
                    }
                    break;
                case PREF:
                    {
                        int w = (int)(spec[i++]);
                        region.setPrefWidth(w);
                    }
                    break;
                case FILL:
                    {
                        HBox.setHgrow(region, Priority.ALWAYS);
                    }
                    break;
                default:
                    throw new Error("?" + cmd);
                }
            } else {
                throw new Error("?" + x);
            }
        }

        return b;
    }

    protected Region newItem() {
        Region r = new Region();
        r.setPrefWidth(30);
        r.setMinWidth(10);
        r.setBackground(bg());
        ContextMenu m = new ContextMenu();
        r.setOnContextMenuRequested((ev) -> {
            m.getItems().setAll(
                new MenuItem("width=" + r.getWidth()),
                new SeparatorMenuItem(),
                new MenuItem("min width=" + r.getMinWidth()),
                new MenuItem("pref width=" + r.getPrefWidth()),
                new MenuItem("max width=" + r.getMaxWidth())
            );
            m.show(r, ev.getScreenX(), ev.getScreenY());
        });
        return r;
    }

    protected Background bg() {
        double h = new Random().nextInt(360);
        Color c = Color.hsb(h, 0.2, 0.99);
        return new Background(new BackgroundFill(c, null, null));
    }

    protected void updatePane() {
        Demo d = demoSelector.getSelectionModel().getSelectedItem();
        Object[] spec = createSpec(d);
        hbox = createPane(d, spec);
        setContent(hbox);
    }
}
