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

package com.oracle.demo.richtext.notebook;

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
 * A demo script engine for the notebook.
 *
 * @author Andy Goryachev
 */
public class DemoScriptEngine {
    public DemoScriptEngine() {
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
        // pretent we are working
        Thread.sleep(500);

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
            JsonContentWithAsyncUpdate content = new JsonContentWithAsyncUpdate(10_000_000);
            return new CodeTextModel(content)
            {
                {
                    content.setUpdater((ix) -> {
                        TextPos p = TextPos.ofLeading(ix, 0);
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
