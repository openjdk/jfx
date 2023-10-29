/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

/**
 * Base class for individual control test Pane.
 */
public class TestPaneBase extends BorderPane {
    private final BorderPane contentPane;

    public TestPaneBase() {
        contentPane = new BorderPane();
        contentPane.setOpacity(1.0);

        updateContent();
    }

    public void updateContent() {
        SplitPane hsplit = new SplitPane(contentPane, pane());
        FX.name(hsplit, "hsplit");
        hsplit.setBorder(Border.EMPTY);
        hsplit.setDividerPositions(1.0);
        hsplit.setOrientation(Orientation.HORIZONTAL);

        SplitPane vsplit = new SplitPane(hsplit, pane());
        FX.name(vsplit, "vsplit");
        vsplit.setBorder(Border.EMPTY);
        vsplit.setDividerPositions(1.0);
        vsplit.setOrientation(Orientation.VERTICAL);

        setCenter(vsplit);
    }

    protected static Pane pane() {
        Pane p = new Pane();
        SplitPane.setResizableWithParent(p, false);
        p.setStyle("-fx-background-color:#dddddd;");
        return p;
    }

    public Button addButton(String name, Runnable action) {
        Button b = new Button(name);
        b.setOnAction((ev) -> {
            action.run();
        });

        toolbar().add(b);
        return b;
    }

    public TBar toolbar() {
        if (getTop() instanceof TBar) {
            return (TBar)getTop();
        }

        TBar t = new TBar();
        setTop(t);
        return t;
    }

    public Window getWindow() {
        Scene s = getScene();
        if (s != null) {
            return s.getWindow();
        }
        return null;
    }

    public void setContent(Node content) {
        contentPane.setCenter(content);
        BorderPane.setAlignment(content, Pos.TOP_LEFT);
    }

    public void setOptions(Node n) {
        if (n == null) {
            setRight(null);
        } else {
            ScrollPane sp = new ScrollPane(n);
            sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
            sp.setHbarPolicy(ScrollBarPolicy.NEVER);
            setRight(sp);
        }
    }

    protected void onChange(ComboBox<?> cb, boolean immediately, Runnable client) {
        cb.getSelectionModel().selectedItemProperty().addListener((x) -> {
            client.run();
        });

        if (immediately) {
            client.run();
        }
    }

    protected void onChange(CheckBox cb, boolean immediately, Runnable client) {
        cb.selectedProperty().addListener((x) -> {
            client.run();
        });

        if (immediately) {
            client.run();
        }
    }

    /** Local toolbar */
    public static class TBar extends HBox {
        public TBar() {
            setFillHeight(true);
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(2);
        }

        public <T extends Node> T add(T n) {
            getChildren().add(n);
            return n;
        }

        public void addAll(Node... nodes) {
            for (Node n: nodes) {
                add(n);
            }
        }
    }
}
