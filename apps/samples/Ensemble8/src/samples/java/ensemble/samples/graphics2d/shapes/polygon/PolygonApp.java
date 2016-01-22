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
package ensemble.samples.graphics2d.shapes.polygon;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

/**
 * A sample that demonstrates polygon construction.
 *
 * @sampleName Polygon
 * @preview preview.png
 * @see javafx.scene.shape.Polygon
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @playground polygon1.fill (name="Polygon 1 Fill")
 * @playground polygon2.stroke (name="Polygon 2 Stroke")
 * @embedded
 */
public class PolygonApp extends Application {

    // Will be a simple red-filled triangle
    private Polygon polygon1 = new Polygon(new double[]{
            45 , 10 ,
            10 , 80 ,
            80 , 80 ,
        });
    // Will be a blue stroked polygon
    private Polygon polygon2 = new Polygon(new double[]{
            135, 15,
            160, 30,
            160, 60,
            135, 75,
            110, 60,
            110, 30
        });

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(180, 100);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        polygon1.setFill(Color.RED);

        // Blue stroked polygon
        polygon2.setStroke(Color.DODGERBLUE);
        polygon2.setStrokeWidth(2);
        polygon2.setFill(null);
        root.getChildren().addAll(polygon1, polygon2);
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
