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

import javafx.animation.AnimationTimer;
import javafx.animation.FillTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.sun.javafx.perf.PerformanceTracker;

/**
 */
public abstract class BenchBase<T extends Node> extends Application {
    private FillTransition driver;
    private BenchTest currentTest;

    @Override
    public void start(Stage stage) throws Exception {

        int[][] sizes = new int[][] {
                {8, 8},
                {10, 10},
                {14, 14},
                {20, 20},
                {40, 40},
                {60, 60},
        };
        BenchTest[] tests = new BenchTest[3 * 6];
        int sizeIndex = 0;
        for (int i=0; i<tests.length; i+=3) {
            int rows = sizes[sizeIndex][0];
            int cols = sizes[sizeIndex][1];
            tests[i] = new SimpleGrid(this, rows, cols);
            tests[i+1] = new PixelGrid(this, rows, cols);
            tests[i+2] = new RotatingGrid(this, rows, cols);
            sizeIndex++;
        }

        Scene scene = new Scene(new Group(), 640, 480);
        stage.setScene(scene);
        stage.show();

        final PerformanceTracker tracker = PerformanceTracker.getSceneTracker(scene);
        AnimationTimer fpsTimer = new AnimationTimer() {
            long lastReset;
            @Override
            public void handle(long now) {
                if (now - lastReset > 5000 * 1000000) {
                    float fps = tracker.getAverageFPS();
                    if (currentTest != null) {
                        currentTest.maxFPS = Math.max(currentTest.maxFPS, fps);
                    }
                    tracker.resetAverageFPS();
                    lastReset = now;
                }
            }
        };
        fpsTimer.start();

        runTest(scene, tests, 0);
    }

    protected abstract void resizeAndRelocate(T node, double x, double y, double width, double height);

    protected abstract T createNode();

    private void runTest(Scene scene, BenchTest[] tests, int index) {
        if (index >= tests.length) {
            for (int i=0; i<tests.length; i+=3) {
                // There are 3 types of tests involved, all at different pixel sizes
                int nodeCount = tests[i].getNodeCount();
                assert nodeCount == tests[i+1].getNodeCount() && nodeCount == tests[i+2].getNodeCount();
                System.out.print(nodeCount + "\t");
                System.out.print(tests[i].maxFPS + "\t");
                System.out.print(tests[i+1].maxFPS + "\t");
                System.out.println(tests[i+2].maxFPS + "\t");
            }
            return; // we're done.
        }

        if (currentTest != null) {
            currentTest.tearDown();
        }

        currentTest = tests[index];
        tests[index].setup(scene);
        Rectangle background = (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        driver = new FillTransition(Duration.seconds(5), background, Color.WHITE, Color.BLACK);
        driver.setAutoReverse(true);
        driver.setCycleCount(2);
        driver.play();
        driver.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                runTest(scene, tests, index+1);
            }
        });
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}

