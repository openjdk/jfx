/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates.
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
package ensemble.samples.graphics2d.shapes.quadcurve;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.QuadCurve;
import javafx.stage.Stage;

/**
 * An example of how various settings affect a quadratic Bézier parametric curve.
 *
 * @sampleName Quad Curve
 * @preview preview.png
 * @playground quadCurve.fill (name="Quad Curve Fill")
 * @playground quadCurve.stroke (name="Quad Curve Stroke")
 * @playground quadCurve.startX (name="Quad Curve Start X", min=0, max=170)
 * @playground quadCurve.startY (name="Quad Curve Start Y", min=10, max=80)
 * @playground quadCurve.controlX (name="Quad Curve Control X", min=0, max=180)
 * @playground quadCurve.controlY (name="Quad Curve Control Y", min=0, max=90)
 * @playground quadCurve.endX (name="Quad Curve End X", min=10, max=180)
 * @playground quadCurve.endY (name="Quad Curve End Y", min=10, max=80)
 * @see javafx.scene.layout.Region
 * @see javafx.scene.shape.QuadCurve
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @embedded
 *
 * @related /Graphics 2d/Shapes/Arc
 * @related /Graphics 2d/Shapes/Circle
 * @related /Graphics 2d/Shapes/Cubic Curve
 * @related /Graphics 2d/Shapes/Ellipse
 * @related /Graphics 2d/Shapes/Line
 * @related /Graphics 2d/Shapes/Path
 * @related /Graphics 2d/Shapes/Polygon
 * @related /Graphics 2d/Shapes/Polyline
 * @related /Graphics 2d/Shapes/Rectangle
 */
public class QuadCurveApp extends Application {
    // Create quadCurve shape
    QuadCurve quadCurve = new QuadCurve();

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(184, 100);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        quadCurve.setStartX(0);
        quadCurve.setStartY(45);
        quadCurve.setControlX(50);
        quadCurve.setControlY(10);
        quadCurve.setEndX(180);
        quadCurve.setEndY(45);
        quadCurve.setStroke(Color.RED);
        quadCurve.setFill(Color.ROSYBROWN);
        quadCurve.setStrokeWidth(2d);
        root.getChildren().add(quadCurve);
        return root;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
