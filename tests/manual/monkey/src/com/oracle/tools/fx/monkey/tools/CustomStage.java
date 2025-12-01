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
package com.oracle.tools.fx.monkey.tools;

import java.util.Arrays;
import java.util.Comparator;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.oracle.tools.fx.monkey.util.CustomPane;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * Custom Stage Tester.
 */
public class CustomStage extends Stage {
    public CustomStage(StageStyle style) {
        super(style);

        setTitle("Stage [" + style + "]");
        setWidth(700);
        setHeight(500);

        setUiPanel();
    }

    private void setContent(Parent n) {
        Scene sc = new Scene(n);
        sc.setFill(Color.TRANSPARENT);
        sc.setOnContextMenuRequested(this::createPopupMenu);
        setScene(sc);
    }

    private void createPopupMenu(ContextMenuEvent ev) {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Irregular Shape", this::setIrregularShape);
        FX.item(m, "UI Panel", this::setUiPanel);
        FX.item(m, "TextArea", this::setTextArea);
        FX.item(m, "Empty", this::setEmpty);
        FX.separator(m);
        FX.item(m, "Size to Scene", this::sizeToScene);
        FX.item(m, "To Back", this::toBack);
        FX.item(m, "To Front", this::toFront);
        FX.separator(m);
        FX.checkItem(m, "Full Screen", isFullScreen(), this::setFullScreen);
        FX.checkItem(m, "Iconified", isIconified(), this::setIconified);
        FX.checkItem(m, "Maximize", isMaximized(), this::setMaximized);
        FX.separator(m);
        FX.item(m, "Close", this::hide);
        m.show(this, ev.getScreenX(), ev.getScreenY());
    }

    private void setEmpty() {
        setContent(new Group());
    }

    private void setIrregularShape() {
        Circle c = new Circle(100, Color.RED);
        StackPane g = new StackPane(c);
        g.setBorder(new Border(new BorderStroke(Color.rgb(0, 0, 0, 0.3), BorderStrokeStyle.SOLID, null, new BorderWidths(4))));
        g.setBackground(Background.fill(Color.TRANSPARENT));
        setContent(g);
    }

    private void setUiPanel() {
        CustomPane p = CustomPane.create();
        setContent(p);
    }

    private void setTextArea() {
        setContent(new TextArea());
    }

    public static void addMenu(MenuBar m) {
        StageStyle[] styles = StageStyle.values();
        Arrays.sort(styles, new Comparator<StageStyle>() {
            @Override
            public int compare(StageStyle a, StageStyle b) {
                return a.toString().compareTo(b.toString());
            }
        });

        for (StageStyle st: styles) {
            FX.item(m, st.toString(), () -> {
                new CustomStage(st).show();
            });
        }
    }
}
