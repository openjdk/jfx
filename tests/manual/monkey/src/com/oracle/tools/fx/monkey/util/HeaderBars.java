/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.paint.Color;

/**
 * HeaderBar (Preview Feature) choices and related methods.
 */
public class HeaderBars {

    public static enum Choice {
        NONE,
        SIMPLE,
        SPLIT;

        @Override
        public String toString() {
            return switch(this) {
                case NONE -> "<none>";
                case SIMPLE ->"Simple";
                case SPLIT -> "Split";
            };
        }
    }

    public static Parent createSimple(Parent n) {
        HeaderBar headerBar = headerBar();
        headerBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));
        headerBar.setCenter(searchField());
        headerBar.setLeft(button("Left"));
        headerBar.setRight(label("Right"));

        BorderPane bp = new BorderPane();
        bp.setTop(headerBar);
        bp.setCenter(n);
        return bp;
    }

    public static Parent createSplit(Parent n) {
        HeaderBar leftHeaderBar = headerBar();
        leftHeaderBar.setBackground(Background.fill(Color.VIOLET));
        leftHeaderBar.setLeft(button("Left"));
        leftHeaderBar.setCenter(searchField());
        leftHeaderBar.setRightSystemPadding(false);

        HeaderBar rightHeaderBar = headerBar();
        rightHeaderBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));
        rightHeaderBar.setLeftSystemPadding(false);
        rightHeaderBar.setRight(button("Right"));

        BorderPane left = new BorderPane();
        left.setTop(leftHeaderBar);
        left.setCenter(n);

        BorderPane right = new BorderPane();
        right.setTop(rightHeaderBar);

        return new SplitPane(left, right);
    }

    private static TextField searchField() {
        TextField f = new TextField();
        f.setPromptText("Search...");
        f.setMaxWidth(300);
        return f;
    }

    public static HeaderBar headerBar() {
        HeaderBar h = new HeaderBar();
        FX.setPopupMenu(h, () -> {
            Menu m2;
            ContextMenu m = new ContextMenu();
            m2 = FX.menu(m, "Drag Type (Children)");
            FX.item(m2, "NONE", () -> setDragType(h, HeaderDragType.NONE));
            FX.item(m2, "DRAGGABLE", () -> setDragType(h, HeaderDragType.DRAGGABLE));
            FX.item(m2, "DRAGGABLE_SUBTREE", () -> setDragType(h, HeaderDragType.DRAGGABLE_SUBTREE));
            FX.item(m2, "TRANSPARENT", () -> setDragType(h, HeaderDragType.TRANSPARENT));
            FX.item(m2, "TRANSPARENT_SUBTREE", () -> setDragType(h, HeaderDragType.TRANSPARENT_SUBTREE));
            return m;
        });
        return h;
    }

    private static void setDragType(HeaderBar h, HeaderDragType t) {
        for (Node n: h.getChildrenUnmodifiable()) {
            HeaderBar.setDragType(n, t);
        }
    }

    private static Button button(String text) {
        Button b = new Button(text);
        setDnD(b, text);
        return b;
    }

    private static Label label(String text) {
        Label b = new Label(text);
        setDnD(b, text);
        return b;
    }

    private static void setDnD(Node n, String text) {
        n.setOnDragDetected((ev) -> {
            Dragboard db = n.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(text);
            db.setContent(cc);
            ev.consume();
        });
        n.setOnDragOver((ev) -> {
            if (ev.getDragboard().hasString()) {
                ev.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            ev.consume();
        });
        n.setOnDragDropped((ev) -> {
            Dragboard db = ev.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                System.out.println("accepted drop: " + db.getString());
                success = true;
            }
            ev.setDropCompleted(success);
            ev.consume();
        });
    }
}
