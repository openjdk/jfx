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
package ensemble.samples.graphics2d.shapes.ellipse;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how various settings affect two elliptical shapes.
 *
 * @sampleName Ellipse
 * @preview preview.png
 * @see javafx.scene.shape.Ellipse
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @related /Graphics 2d/Shapes/Circle
 * @related /Graphics 2d/Shapes/Arc
 * @playground ellipse1.fill (name="Ellipse 1 Fill")
 * @playground ellipse1.radiusX (name="Ellipse 1 Width", min=10, max=40)
 * @playground ellipse1.radiusY (name="Ellipse 1 Height", min=10, max=45)
 * @playground ellipse2.stroke (name="Ellipse 2 Stroke")
 * @playground ellipse2.strokeWidth (name="Ellipse 2 Stroke Width", min=1, max=5)
 * @playground ellipse2.radiusX (name="Ellipse 2 Width", min=10, max=40)
 * @playground ellipse2.radiusY (name="Ellipse 2 Height", min=10, max=45)
 * @embedded
 */
public class EllipseApp extends Application {

    private Ellipse ellipse1 = new Ellipse(45, 45, 30, 45);
    private Ellipse ellipse2 = new Ellipse(135, 45, 30, 45);

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(200, 100);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // Red ellipse
        ellipse1.setFill(Color.RED);
        // Blue stroked ellipse
        ellipse2.setStroke(Color.DODGERBLUE);
        ellipse2.setFill(null);
        root.getChildren().addAll(ellipse1, ellipse2);
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
