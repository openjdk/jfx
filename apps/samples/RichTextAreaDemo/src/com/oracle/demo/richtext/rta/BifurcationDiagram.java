/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.rta;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Illustrate chaos arising from a simple formula.
 *
 * @author Andy Goryachev
 */
public class BifurcationDiagram {
    private static final double min = 2.4;
    private static final double max = 4.0;

    public static Pane generate() {
        Pane p = new Pane();
        p.setPrefSize(600, 200);
        p.widthProperty().addListener((x) -> update(p));
        p.heightProperty().addListener((x) -> update(p));
        update(p);
        return p;
    }

    protected static void update(Pane p) {
        double w = p.getWidth();
        double h = p.getHeight();

        if ((w < 1) || (h < 1)) {
            return;
        } else if (w > 600) {
            w = 600;
        }

        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();

        g.setFill(Color.gray(0.9));
        g.fillRect(0, 0, w, h);

        int count = 1000;
        int start = 500;
        double r = 0.3;
        g.setFill(Color.rgb(0, 0, 0, 0.2));

        for (double λ = min; λ < max; λ += 0.001) {
            double x = 0.5;
            for (int i = 0; i < count; i++) {
                x = λ * x * (1.0 - x);
                if (i > start) {
                    double px = w * (λ - min) / (max - min);
                    double py = h * (1.0 - x);

                    g.fillOval(px - r, py - r, r + r, r + r);
                }
            }
        }

        p.getChildren().setAll(c);
    }
}
