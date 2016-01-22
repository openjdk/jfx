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
package ensemble.samples.graphics2d.shapes.arc;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how various settings affect a line shape.
 *
 * @sampleName Arc
 * @preview preview.png
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @related /Graphics 2d/Shapes/Circle
 * @related /Graphics 2d/Shapes/Ellipse
 * @playground arc1.fill (name="Arc1 Fill")
 * @playground arc1.startAngle (name="Arc 1 Start Angle", min=0, max=360)
 * @playground arc1.length (name="Arc 1 Length", min=0, max=360)
 * @playground arc2.stroke (name="Arc 2 Stroke")
 * @playground arc2.strokeWidth (name="Arc 2 Stroke Width", min=0, max=5)
 * @playground arc2.radiusX (name="Arc 2 Radius X", min=0, max=50)
 * @playground arc2.radiusY (name="Arc 2 Radius Y", min=0, max=50)
 * @embedded
 */
public class ArcApp extends Application {

    private Arc arc1 = new Arc(45, 60, 45, 45, 40, 100);
    private Arc arc2 = new Arc(155, 60, 45, 45, 40, 100);

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(245, 100);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // Red arc
        arc1.setFill(Color.RED);
        // Blue stroked arc
        arc2.setStroke(Color.DODGERBLUE);
        arc2.setFill(null);
        root.getChildren().addAll(arc1, arc2);
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
