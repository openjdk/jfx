/*
 * Copyright (c) 2013 Oracle and/or its affiliates.
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

package nodecount;

import javafx.animation.Animation;
import javafx.animation.FillTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 */
public abstract class BenchTest {
    public static final int GAP = 6;

    double maxFPS;
    private final BenchBase benchmark;
    private final int rows, cols;
    private final boolean pixelSnap;
    
    public BenchTest(BenchBase benchmark, int rows, int cols, boolean pixelSnap) {
        this.benchmark = benchmark;
        this.rows = rows;
        this.cols = cols;
        this.pixelSnap = pixelSnap;
    }

    public double getMaxFPS() { return maxFPS; }

    public int getNodeCount() {
        return rows * cols;
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
    
    protected Animation createBenchmarkDriver(Scene scene) {
        Rectangle background = (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        FillTransition t = new FillTransition(Duration.seconds(5), background, Color.WHITE, Color.BLACK);
        t.setAutoReverse(true);
        t.setCycleCount(2);
        return t;
    }

    public void setup(Scene scene) {
        final Rectangle background = new Rectangle();
        final Node[][] nodes = new Node[rows][cols];
        final Pane root = new Pane() {
            @Override protected void layoutChildren() {
                background.setWidth(getWidth());
                background.setHeight(getHeight());
                double rectWidth = (getWidth() - ((cols-1) * GAP)) / cols;
                double rectHeight = (getHeight() - ((rows-1) * GAP)) / rows;
                for (int r=0; r<rows; r++) {
                    for (int c=0; c<cols; c++) {
                        double x = c * (GAP + rectWidth);
                        double y = r * (GAP + rectHeight);
                        Node n = nodes[r][c];
                        benchmark.resizeAndRelocate(n,
                                pixelSnap ? (int) x : x,
                                pixelSnap ? (int) y : y,
                                pixelSnap ? (int) rectWidth : rectWidth,
                                pixelSnap ? (int) rectHeight : rectHeight);
                    }
                }
            }
        };
        root.getChildren().add(background);

        for (int r=0; r<rows; r++) {
            for (int c=0; c<cols; c++) {
                Node node = benchmark.createNode();
                nodes[r][c] = node;
                root.getChildren().add(node);
            }
        }

        scene.setRoot(root);
    }

    public void tearDown() { }

    @Override public String toString() {
        return getClass().getSimpleName() + " " + rows + "x" + cols + ": " + maxFPS;
    }
}
