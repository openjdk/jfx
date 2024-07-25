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

package com.oracle.demo.rich.notebook;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;

/**
 * A mock script engine for the notebook.
 */
public class ScriptEngine {
    public ScriptEngine() {
    }

    /**
     * Executes the script and returns the result.
     * Result object can be one of the following:
     * - a Throwable (either returned or thrown when executing the script)
     * - a String for a text result
     * - a Supplier that creates a Node to be inserted into the output pane
     * @param src the source script
     * @return the result of computation
     */
    public Object executeScript(String src) throws Throwable {
        // simulate processing
        Thread.sleep(500); // FIX

        if (src == null) {
            return null;
        } else if (src.contains("text")) {
            return """
                Multi-line execution result.
                Line 1.
                Line 2.
                Line 3.
                Completed.
                """;
        } else if (src.contains("json")) {
            JsonContentWithAsyncUpdate c = new JsonContentWithAsyncUpdate(10_000_000);
            return new CodeTextModel(c) {
                {
                    c.setUpdater((ix) -> {
                        TextPos p = new TextPos(ix, 0);
                        int len = getPlainText(ix).length();
                        fireChangeEvent(p, p, len, 0, 0);
                    });
                }
            };
        } else if (src.contains("node")) {
            return new Supplier<Node>() {
                @Override
                public Node get() {
                    return new ListView(FXCollections.observableArrayList(
                        "one",
                        "two",
                        "three",
                        "four",
                        "five",
                        "six",
                        "seven",
                        "eight",
                        "nine",
                        "ten",
                        "eleven",
                        "twelve",
                        "thirteen",
                        "fourteen",
                        "fifteen",
                        "sixteen",
                        "seventeen",
                        "nineteen",
                        "twenty"
                    ));
                }
            };
        } else if (src.contains("image")) {
            return executeInFx(this::generateImage);
        } else {
            throw new Error("script failed");
        }
    }

    private Image generateImage() {
        int w = 700;
        int h = 500;
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.gray(1.0));
        g.fillRect(0, 0, w, h);

        g.setLineWidth(0.25);

        Random rnd = new Random();
        for(int i=0; i<128; i++) {
            double x = rnd.nextInt(w);
            double y = rnd.nextInt(h);
            double r = rnd.nextInt(64);
            int hue = rnd.nextInt(360);

            g.setFill(Color.hsb(hue, 0.5, 1.0, 0.5));
            g.fillOval(x - r, y - r, r + r, r + r);

            g.setStroke(Color.hsb(hue, 0.5, 0.5, 1.0));
            g.strokeOval(x - r, y - r, r + r, r + r);
        }
        return c.snapshot(null, null);
    }

    private static Object executeInFx(Supplier gen) {
        AtomicReference<Object> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Object r = gen.get();
                result.set(r);
            } catch (Throwable e) {
                result.set(e);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
            return result.get();
        } catch (InterruptedException e) {
            return e;
        }
    }
}
