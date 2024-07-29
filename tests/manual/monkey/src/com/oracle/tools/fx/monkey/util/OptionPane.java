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
package com.oracle.tools.fx.monkey.util;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;

/**
 * Option Pane - a vertical option sheet.
 */
public class OptionPane extends Accordion {

    public OptionPane() {
        FX.name(this, "OptionPane");
    }

    public void label(String text) {
        lastSection().add(new Label(text));
    }

    public void option(Node n) {
        lastSection().add(n);
    }

    public void option(String text, Node n) {
        lastSection().add(new Label(text));
        if (n != null) {
            lastSection().add(n);
        }
    }

    public void add(Node n) {
        lastSection().add(n);
    }

    public void separator() {
        lastSection().add(new Separator(Orientation.HORIZONTAL));
    }

    public void section(String name) {
        section(name, new OptionGridPane());
    }

    public void section(String name, OptionGridPane content) {
        TitledPane t = new TitledPane(name, content);
        getPanes().add(t);

        List<TitledPane> panes = getPanes();
        if (panes.size() == 1) {
            setExpandedPane(panes.get(0));
        }
    }

    private OptionGridPane lastSection() {
        List<TitledPane> panes = getPanes();
        if (panes.size() == 0) {
            section("Properties");
        }
        TitledPane t = panes.get(panes.size() - 1);
        return (OptionGridPane)t.getContent();
    }

    private static class OptionGridPane extends GridPane {
        private int row;
        private int column;
        private static final Insets MARGIN = new Insets(1, 4, 0, 4);
        private static final Insets PADDING = new Insets(0, 0, 2, 0);

        public OptionGridPane() {
            setPadding(PADDING);
        }

        public void label(String text) {
            add(new Label(text));
        }

        public void option(Node n) {
            add(n);
        }

        public void add(Node n) {
            add(n, column, row++);
            setMargin(n, MARGIN);
            setFillHeight(n, Boolean.TRUE);
            setFillWidth(n, Boolean.TRUE);
        }
    }
}
