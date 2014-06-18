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
package ensemble.samples.graphics2d.shapes.path;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;
import javafx.stage.Stage;

/**
 * A sample that demonstrates two path shapes.
 *
 * @sampleName Path
 * @preview preview.png
 * @see javafx.scene.shape.Path
 * @see javafx.scene.shape.ArcTo
 * @see javafx.scene.shape.ClosePath
 * @see javafx.scene.shape.CubicCurveTo
 * @see javafx.scene.shape.HLineTo
 * @see javafx.scene.shape.LineTo
 * @see javafx.scene.shape.MoveTo
 * @see javafx.scene.shape.QuadCurveTo
 * @see javafx.scene.shape.VLineTo
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Color
 * @playground path1.stroke (name="Path 1 Stroke")
 * @playground path2.stroke (name="Path 2 Stroke")
 * @embedded
 */
public class PathApp extends Application {

    private Path path1 = new Path();
    private Path path2 = new Path();

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(245, 100);
        root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // Create path shape - square
        path1.getElements().addAll(
                new MoveTo(25, 25),
                new HLineTo(65),
                new VLineTo(65),
                new LineTo(25, 65),
                new ClosePath());
        path1.setFill(null);
        path1.setStroke(Color.RED);
        path1.setStrokeWidth(2);

        // Create path shape - curves
        path2.getElements().addAll(
                new MoveTo(100, 45),
                new CubicCurveTo(120, 20, 130, 80, 140, 45),
                new QuadCurveTo(150, 0, 160, 45),
                new ArcTo(20, 40, 0, 180, 45, true, true));
        path2.setFill(null);
        path2.setStroke(Color.DODGERBLUE);
        path2.setStrokeWidth(2);
        path2.setTranslateY(36);
        root.getChildren().addAll(path1, path2);
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
