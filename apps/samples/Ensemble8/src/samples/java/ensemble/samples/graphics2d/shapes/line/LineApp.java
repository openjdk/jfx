/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.samples.graphics2d.shapes.line;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how various settings affect a line shape.
 *
 * @sampleName Line
 * @preview preview.png
 * @see javafx.scene.shape.Line
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @playground exampleLine.startX  (min=50, max=550)
 * @playground exampleLine.startY  (min=50, max=350)
 * @playground exampleLine.endX (min=50, max=550)
 * @playground exampleLine.endY (min=50, max=350)
 * @playground exampleLine.stroke
 * @playground exampleLine.strokeWidth  (min=0.1, max=50)
 * @playground exampleLine.strokeLineCap
 * @playground exampleLine.getStrokeDashArray
 * @playground exampleLine.strokeDashOffset (min=0, max=500)
 * @embedded
 */
public class LineApp extends Application {

    private static final int LINES_NUMBER = 100;
    private static final double WIDTH = 600;
    private static final double HEIGHT = 400;
    private Line exampleLine;

    public Line createRandomLine() {
        double startX = Math.random() * WIDTH;
        double startY = Math.random() * HEIGHT;
        double endX = Math.random() * WIDTH;
        double endY = Math.random() * HEIGHT;
        double width = Math.random() * 3 + 0.5;

        // Create line shape
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.RED);
        line.setStrokeWidth(width);
        return line;
    }

    public Parent createContent() {
        Pane root = new Pane();
        root.setMinSize(WIDTH, HEIGHT);
        root.setMaxSize(WIDTH, HEIGHT);

        // add many lines
        for (int i = 0; i < LINES_NUMBER; i++) {
            root.getChildren().add(createRandomLine());
        }

        // add line for the playground
        exampleLine = new Line(50, 50, 550, 350);
        exampleLine.setStroke(Color.RED);
        exampleLine.setStrokeWidth(3);
        root.getChildren().add(exampleLine);

        return root;
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) { launch(args); }
}
