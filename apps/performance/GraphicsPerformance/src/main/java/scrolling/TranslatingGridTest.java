/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates.
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

package scrolling;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import nodecount.BenchTest;

/**
 * Translates a grid of items to their "pure" fractional positions.
 */
public class TranslatingGridTest extends BenchTest {
    private Rectangle clip = new Rectangle();
    protected double height;

    TranslatingGridTest(ScrollingBenchBase benchmark, int rows, int cols) {
        // Pixel snap the grid itself, always, so we know we are measuring
        // the effect of translating fractionally, and not the cost of having
        // nodes on fractional boundaries inherently.
        super(benchmark, rows, cols, true);
    }

    // For each test, we need to start our timeline which will scroll stuff...
    @Override protected Animation createBenchmarkDriver(Scene scene) {
        final Parent root = (Parent) scene.getRoot().getChildrenUnmodifiable().get(0);
        Timeline t = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(root.translateYProperty(), 0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(root.translateYProperty(), scene.getHeight()-height)));
        t.setAutoReverse(true);
        t.setCycleCount(2);
        return t;
    }

    @Override
    public void setup(Scene scene) {
        super.setup(scene);

        double side = ((scene.getWidth() + GAP) / getCols()) - GAP;
        height = (side * getRows()) + (GAP * (getRows() - 1));

        // Gotta handle layout so the old root pane is taller than the window but wide as the window
        final Pane pane = (Pane) scene.getRoot();
        Pane root = new Pane(pane) {
            @Override
            protected void layoutChildren() {
                pane.resize(getWidth(), height);
            }
        };
        pane.resize(scene.getWidth(), height);
        clip.widthProperty().bind(scene.widthProperty());
        clip.heightProperty().bind(scene.heightProperty());
        Pane p = new StackPane(root);
        p.setCache(true);
        p.setClip(clip);
        scene.setRoot(p);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        clip.widthProperty().unbind();
        clip.heightProperty().unbind();
    }
}
