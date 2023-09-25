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
package com.oracle.tools.fx.monkey.pages;

import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 *
 */
public class LabelPage extends TestPaneBase {
    enum Demo {
        TEXT_ONLY("text only"),
        TEXT_GRAPHIC_LEFT("text + graphic left"),
        TEXT_GRAPHIC_RIGHT("text + graphic right"),
        TEXT_GRAPHIC_TOP("text + graphic top"),
        TEXT_GRAPHIC_BOTTOM("text + graphic bottom"),
        TEXT_GRAPHIC_TEXT_ONLY("text + graphic (text only)"),
        TEXT_GRAPHIC_GRAPHIC_ONLY("text + graphic (graphic only)"),
        GRAPHIC("graphic"),
        ;
        private final String text;
        Demo(String text) { this.text = text; }
        public String toString() { return text; }
    }

    private final ComboBox<Demo> label1Selector;
    private final ComboBox<Demo> label2Selector;
    private final ComboBox<Pos> alignmentSelector;
    private final Image im;

    public LabelPage() {
        FX.name(this, "LabelPage");

        im = createImage();

        label1Selector = new ComboBox<>();
        FX.name(label1Selector, "label1Selector");
        label1Selector.getItems().addAll(Demo.values());
        label1Selector.setEditable(false);
        label1Selector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateControl();
        });

        label2Selector = new ComboBox<>();
        FX.name(label2Selector, "label2Selector");
        label2Selector.getItems().addAll(Demo.values());
        label2Selector.setEditable(false);
        label2Selector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateControl();
        });

        alignmentSelector = new ComboBox<>();
        FX.name(alignmentSelector, "alignmentSelector");
        alignmentSelector.getItems().addAll(Pos.values());
        alignmentSelector.setEditable(false);
        alignmentSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateControl();
        });

        OptionPane p = new OptionPane();
        p.label("Label 1:");
        p.option(label1Selector);
        p.label("Label 2:");
        p.option(label2Selector);
        p.label("HBox Alignment:");
        p.option(alignmentSelector);
        setOptions(p);

        FX.select(label1Selector, Demo.TEXT_ONLY);
        FX.select(label2Selector, Demo.TEXT_ONLY);
    }

    protected void updateControl() {
        Demo d1 = FX.getSelectedItem(label1Selector);
        Label label1 = create(d1);

        Demo d2 = FX.getSelectedItem(label2Selector);
        Label label2 = create(d2);

        HBox b = new HBox(label1, label2);
        Pos a = FX.getSelectedItem(alignmentSelector);
        if (a != null) {
            b.setAlignment(a);
        }
        setContent(b);
    }

    protected Label create(Demo d) {
        if(d == null) {
            return new Label();
        }

        switch(d) {
        case TEXT_GRAPHIC_LEFT:
            {
                Label t = new Label("text + graphic left");
                t.setGraphic(new ImageView(im));
                t.setContentDisplay(ContentDisplay.LEFT);
                return t;
            }
        case TEXT_GRAPHIC_RIGHT:
            {
                Label t = new Label("text + graphic right");
                t.setGraphic(new ImageView(im));
                t.setContentDisplay(ContentDisplay.RIGHT);
                return t;
            }
        case TEXT_ONLY:
            {
                return new Label("text only");
            }
        case TEXT_GRAPHIC_TOP:
            {
                Label t = new Label("text + graphic top");
                t.setGraphic(new ImageView(im));
                t.setContentDisplay(ContentDisplay.TOP);
                return t;
            }
        case TEXT_GRAPHIC_BOTTOM:
            {
                Label t = new Label("text + graphic bottom");
                t.setGraphic(new ImageView(im));
                t.setContentDisplay(ContentDisplay.BOTTOM);
                return t;
            }
        case TEXT_GRAPHIC_TEXT_ONLY:
            {
                Label t = new Label("text + graphic text only");
                t.setGraphic(new ImageView(im));
                t.setContentDisplay(ContentDisplay.TEXT_ONLY);
                return t;
            }
        case TEXT_GRAPHIC_GRAPHIC_ONLY:
            {
                Label t = new Label("text + graphic (graphic only)");
                t.setGraphic(new ImageView(im));
                t.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return t;
            }
        case GRAPHIC:
            {
                Label t = new Label();
                t.setGraphic(new ImageView(im));
                return t;
            }
        default:
            return new Label("??" + d);
        }
    }

    private static Image createImage() {
        int w = 24;
        int h = 16;
        Color c = Color.GREEN;

        WritableImage im = new WritableImage(w, h);
        PixelWriter wr = im.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                wr.setColor(x, y, c);
            }
        }

        return im;
    }
}
