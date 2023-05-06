/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.VBox;

/**
 * ScrollBar Page
 */
public class ScrollBarPage extends TestPaneBase {
    private ScrollBar scroll;
    private Label status;
    private static Long[] VALUES = {
        0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L
    };

    public ScrollBarPage() {
        setId("ScrollBarPage");

        scroll = new ScrollBar();

        ComboBox<Long> min = new ComboBox<>();
        min.setId("min");
        min.getItems().setAll(VALUES);
        min.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(min);
            scroll.setMin(v);
        });

        ComboBox<Long> val = new ComboBox<>();
        val.setId("val");
        val.getItems().setAll(VALUES);
        val.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(val);
            scroll.setValue(v);
        });

        ComboBox<Long> visible = new ComboBox<>();
        visible.setId("visible");
        visible.getItems().setAll(VALUES);
        visible.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(visible);
            scroll.setVisibleAmount(v);
        });

        ComboBox<Long> max = new ComboBox<>();
        max.setId("max");
        max.getItems().setAll(VALUES);
        max.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(max);
            scroll.setMax(v);
        });

        OptionPane p = new OptionPane();
        p.label("Min:");
        p.option(min);
        p.label("Value:");
        p.option(val);
        p.label("Visible:");
        p.option(visible);
        p.label("Max:");
        p.option(max);

        status = new Label();

        scroll.minProperty().addListener((s, pr, c) -> {
            updateStatus();
        });
        scroll.valueProperty().addListener((s, pr, c) -> {
            updateStatus();
        });
        scroll.visibleAmountProperty().addListener((s, pr, c) -> {
            updateStatus();
        });
        scroll.maxProperty().addListener((s, pr, c) -> {
            updateStatus();
        });

        VBox b = new VBox(scroll, status);
        b.setSpacing(5);

        setContent(b);
        setOptions(p);

        min.getSelectionModel().select(0L);
        val.getSelectionModel().select(5L);
        visible.getSelectionModel().select(1L);
        max.getSelectionModel().select(10L);
    }

    protected int parse(ComboBox<Long> c) {
        Long v = c.getSelectionModel().getSelectedItem();
        return (v == null) ? 0 : v.intValue();
    }

    protected void updateStatus() {
        status.setText(
            "min=" + scroll.getMin() +
            " value=" + scroll.getValue() +
            " visible=" + scroll.getVisibleAmount() +
            " max=" + scroll.getMax()
        );
    }
}
