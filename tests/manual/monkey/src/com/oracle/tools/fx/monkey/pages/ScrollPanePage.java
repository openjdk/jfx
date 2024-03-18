/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ScrollPane Page.
 */
public class ScrollPanePage extends TestPaneBase implements HasSkinnable {
    private final ScrollPane control;
    private final Label content;

    public ScrollPanePage() {
        super("ScrollPanePage");

        content = new Label();
        content.setAlignment(Pos.CENTER);
        
        control = new ScrollPane(content);

        ComboBox<Dimension2D> prefSize = new ComboBox<>();
        FX.name(prefSize, "prefSize");
        prefSize.getItems().setAll(
            new Dimension2D(50, 50),
            new Dimension2D(100, 100),
            new Dimension2D(1000, 1000),
            new Dimension2D(5000, 5000),
            new Dimension2D(5000, 50)
        );
        prefSize.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updatePrefSize(c);
        });

        OptionPane p = new OptionPane();
        p.option("Preferred size:", prefSize);

        setContent(control);
        setOptions(p);

//        min.getSelectionModel().select(0L);
    }

    private void updatePrefSize(Dimension2D d) {
        double w = d.getWidth();
        double h = d.getHeight();
        content.setPrefSize(w, h);
        String s = "Preferred size: " + w + " x " + h;
        content.setText(s);
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ScrollPaneSkin(control));
    }
}
