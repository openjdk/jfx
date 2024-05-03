/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Canvas Page.
 */
public class CanvasPage extends TestPaneBase {
    private Pane pane;

    public CanvasPage() {
        super("CanvasPage");

        pane = new Pane();
        setContent(pane);

        pane.widthProperty().addListener((x) -> updateCanvas());
        pane.heightProperty().addListener((x) -> updateCanvas());

        updateCanvas();
    }

    protected void updateCanvas() {
        double w = pane.getWidth();
        double h = pane.getHeight();
        String text = "width=" + w + " height=" + h;
        Font f = Font.font("System", 12);
        Canvas c = new Canvas(w, h);

        GraphicsContext g = c.getGraphicsContext2D();

        g.setFont(f);
        g.setFill(Color.BLACK);
        g.fillText(text, 2, 14);

        g.setLineWidth(0.5);
        g.setStroke(Color.RED);
        g.beginPath();
        g.moveTo(0, h / 2);
        g.lineTo(w, h / 2);
        g.stroke();

        pane.getChildren().setAll(c);
    }
}
