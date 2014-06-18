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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import nodecount.BenchTest;

/**
 * Uses a ScrollPane for scrolling a grid of items, using "pure" fractional
 * positions.
 */
public class ScrollPaneGridTest extends BenchTest {
    ScrollPaneGridTest(ScrollingBenchBase benchmark, int rows, int cols) {
        // Pixel snap the grid itself, always, so we know we are measuring
        // the effect of translating fractionally, and not the cost of having
        // nodes on fractional boundaries inherently.
        super(benchmark, rows, cols, true);
    }

    // For each test, we need to start our timeline which will scroll stuff...
    @Override protected Animation createBenchmarkDriver(Scene scene) {
        final ScrollPane root = (ScrollPane) scene.getRoot();
        final IntegerProperty vpos = new SimpleIntegerProperty() {
            @Override
            public void set(int newValue) {
                super.set(newValue);
                root.setVvalue(newValue);
            }
        };
        Timeline t = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(vpos, 0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(vpos, root.getVmax())));
        t.setAutoReverse(true);
        t.setCycleCount(2);

        // Uncomment to see that the scroll pane is in fact pixel snapping already!
//        final InvalidationListener layoutListener = new InvalidationListener() {
//            @Override
//            public void invalidated(Observable observable) {
//                System.out.println(root.getContent().getParent().getLayoutY());
//            }
//        };
//        root.getContent().parentProperty().addListener(new ChangeListener<Parent>() {
//            @Override
//            public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
//                if (oldValue != null) oldValue.layoutYProperty().removeListener(layoutListener);
//                if (newValue != null) newValue.layoutYProperty().addListener(layoutListener);
//            }
//        });

        return t;
    }

    @Override
    public void setup(Scene scene) {
        super.setup(scene);

        // Wrap in a ScrollPane
        double side = ((scene.getWidth() + GAP) / getCols()) - GAP;
        double height = (side * getRows()) + (GAP * (getRows() - 1));
        final Pane pane = (Pane) scene.getRoot();
        final ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.heightProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                pane.setPrefHeight(height);
                sp.setVmin(0);
                sp.setVmax(pane.getPrefHeight());
            }
        });
//        pane.resize(scene.getWidth(), height);
        scene.setRoot(sp);
    }
}
