/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.oracle.tools.fx.monkey.options.BooleanOption;

/**
 * Option Pane - a vertical option sheet.
 */
public class OptionPane extends VBox {

    public static final Insets INDENT = new Insets(0, 0, 0, 10);

    public OptionPane() {
        FX.name(this, "OptionPane");
        FX.style(this, "option-pane");
    }

    public void label(String text) {
        lastSection().addFull(new Label(text));
    }

    public void option(Node n) {
        lastSection().addFull(n);
    }

    public void option(BooleanOption op) {
        op.setPadding(INDENT);
        lastSection().addFull(new HBox(op));
    }

    public void option(String text, Node n) {
        lastSection().add(new Label(text), n);
    }

    public void add(Node n) {
        lastSection().addFull(n);
    }

    public void separator() {
        lastSection().addFull(new Separator(Orientation.HORIZONTAL));
    }

    public void section(String name) {
        section(name, new OptionGridPane());
    }

    public void section(String name, boolean expanded) {
        TitledPane p = section(name, new OptionGridPane());
        p.setExpanded(expanded);
    }

    private List<TitledPane> getPanes() {
        return getChildren().
            stream().
            filter((n) -> n instanceof TitledPane).
            map((n) -> (TitledPane)n).
            collect(Collectors.toList());
    }

    public TitledPane section(String name, OptionGridPane content) {
        TitledPane t = new TitledPane(name, content);
        getChildren().add(t);
        return t;
    }

    private OptionGridPane lastSection() {
        List<TitledPane> panes = getPanes();
        if (panes.size() == 0) {
            section("Properties");
        }
        panes = getPanes();
        TitledPane t = panes.get(panes.size() - 1);
        return (OptionGridPane)t.getContent();
    }

    private static class OptionGridPane extends GridPane {
        private static final Insets PADDING = new Insets(2);
        private int row;

        public OptionGridPane() {
            setPadding(PADDING);
            setMaxWidth(Double.MAX_VALUE);
            setVgap(2);
            setHgap(5);
            // we might consider using CSS to reduce padding on all the controls
            setStyle("-fx-font-size:90%;");
        }

        void add(Node label, Node n) {
            if (label != null) {
                add(label, 0, row);
            }
            if (n != null) {
                add(n, 1, row);
                setFillHeight(n, Boolean.TRUE);
                setFillWidth(n, Boolean.TRUE);
            }
            row++;
        }

        void addFull(Node n) {
            add(n, 0, row++, 2, 1);
            setFillHeight(n, Boolean.TRUE);
            setFillWidth(n, Boolean.TRUE);
        }
    }
}
