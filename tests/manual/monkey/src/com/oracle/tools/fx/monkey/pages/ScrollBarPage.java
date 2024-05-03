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
package com.oracle.tools.fx.monkey.pages;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.ScrollBarSkin;
import javafx.scene.layout.VBox;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ScrollBar Page.
 */
public class ScrollBarPage extends TestPaneBase implements HasSkinnable {
    private ScrollBar control;
    private Label status;
    private static Long[] VALUES = {
        0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L
    };

    public ScrollBarPage() {
        super("ScrollBarPage");

        control = new ScrollBar();

        ComboBox<Long> min = new ComboBox<>();
        FX.name(min, "min");
        min.getItems().setAll(VALUES);
        min.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(min);
            control.setMin(v);
        });

        ComboBox<Long> val = new ComboBox<>();
        FX.name(val, "val");
        val.getItems().setAll(VALUES);
        val.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(val);
            control.setValue(v);
        });

        ComboBox<Long> visible = new ComboBox<>();
        FX.name(visible, "visible");
        visible.getItems().setAll(VALUES);
        visible.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(visible);
            control.setVisibleAmount(v);
        });

        ComboBox<Long> max = new ComboBox<>();
        FX.name(max, "max");
        max.getItems().setAll(VALUES);
        max.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            int v = parse(max);
            control.setMax(v);
        });

        OptionPane p = new OptionPane();
        p.option("Min:", min);
        p.option("Value:", val);
        p.option("Visible:", visible);
        p.option("Max:", max);

        status = new Label();

        control.minProperty().addListener((s, pr, c) -> {
            updateStatus();
        });
        control.valueProperty().addListener((s, pr, c) -> {
            updateStatus();
        });
        control.visibleAmountProperty().addListener((s, pr, c) -> {
            updateStatus();
        });
        control.maxProperty().addListener((s, pr, c) -> {
            updateStatus();
        });

        VBox b = new VBox(control, status);
        b.setSpacing(5);

        setContent(b);
        setOptions(p);

        min.getSelectionModel().select(0L);
        val.getSelectionModel().select(5L);
        visible.getSelectionModel().select(1L);
        max.getSelectionModel().select(10L);
    }

    private int parse(ComboBox<Long> c) {
        Long v = c.getSelectionModel().getSelectedItem();
        return (v == null) ? 0 : v.intValue();
    }

    private void updateStatus() {
        status.setText(
            "min=" + control.getMin() +
            " value=" + control.getValue() +
            " visible=" + control.getVisibleAmount() +
            " max=" + control.getMax()
        );
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ScrollBarSkin(control));
    }
}
