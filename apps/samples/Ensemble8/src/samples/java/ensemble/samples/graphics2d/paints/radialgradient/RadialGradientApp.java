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
package ensemble.samples.graphics2d.paints.radialgradient;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A sample that demonstrates two circles, one filled with a simple radial
 * gradient and one filled with a more complex radial gradient.
 *
 *
 * @sampleName Radial Gradient
 * @preview preview.png
 * @see javafx.scene.paint.RadialGradient
 * @see javafx.scene.shape.Shape
 * @see javafx.scene.paint.Paint
 * @see javafx.scene.paint.Color
 * @related /Graphics 2d/Paints/Color
 * @related /Graphics 2d/Paints/Linear Gradient
 */
public class RadialGradientApp extends Application {

    public Parent createContent() {
        //create simple radial gradient
        RadialGradient gradient1 = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(0, Color.DODGERBLUE),
                    new Stop(1, Color.BLACK)
                });
        Circle circle1 = new Circle(45, 45, 40, gradient1);

        //create complex radial gradient
        RadialGradient gradient2 = new RadialGradient(20, 1, 0.5, 0.5, 0.6, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(0, Color.TRANSPARENT),
                    new Stop(0.5, Color.DARKGRAY),
                    new Stop(0.64, Color.WHITESMOKE),
                    new Stop(0.65, Color.YELLOW),
                    new Stop(1, Color.GOLD)
                });
        Circle circle2 = new Circle(145, 45, 40, gradient2);

        HBox hb = new HBox(10);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(circle1, circle2);

        return hb;
    }

    private Rectangle createRectangle(Color color) {
        Rectangle rect1 = new Rectangle(0, 45, 20, 20);
        //Fill rectangle with color
        rect1.setFill(color);
        return rect1;
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
