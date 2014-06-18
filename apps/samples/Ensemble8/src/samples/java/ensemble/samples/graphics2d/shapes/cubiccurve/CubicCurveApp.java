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
package ensemble.samples.graphics2d.shapes.cubiccurve;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.stage.Stage;

/**
 * A sample showing how various settings change a cubic Bézier parametric curve.
 *
 * @sampleName Cubic Curve
 * @preview preview.png
 * @see javafx.scene.shape.CubicCurve
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @playground cubicCurve.fill (name="Cubic Curve Fill")
 * @playground cubicCurve.stroke (name="Cubic Curve Stroke")
 * @playground cubicCurve.startX (name="Cubic Curve Start X", min=0, max=170)
 * @playground cubicCurve.startY (name="Cubic Curve Start Y", min=10, max=80)
 * @playground cubicCurve.controlX1 (name="Cubic Curve Control X1", min=0, max=180)
 * @playground cubicCurve.controlY1 (name="Cubic Curve Control Y1", min=0, max=90)
 * @playground cubicCurve.controlX2 (name="Cubic Curve Control X2", min=0, max=180)
 * @playground cubicCurve.controlY2 (name="Cubic Curve Control Y2", min=0, max=90)
 * @playground cubicCurve.endX (name="Cubic Curve End X", min=10, max=180)
 * @playground cubicCurve.endY (name="Cubic Curve End Y", min=0, max=80)
 * @embedded
 */
public class CubicCurveApp extends Application {

    private CubicCurve cubicCurve = new CubicCurve();

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(245, 100);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cubicCurve.setStartX(0);
        cubicCurve.setStartY(45);
        cubicCurve.setControlX1(30);
        cubicCurve.setControlY1(10);
        cubicCurve.setControlX2(150);
        cubicCurve.setControlY2(80);
        cubicCurve.setEndX(180);
        cubicCurve.setEndY(45);
        cubicCurve.setStroke(Color.RED);
        cubicCurve.setFill(Color.ROSYBROWN);
        cubicCurve.setStrokeWidth(2d);
        root.getChildren().add(cubicCurve);
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
