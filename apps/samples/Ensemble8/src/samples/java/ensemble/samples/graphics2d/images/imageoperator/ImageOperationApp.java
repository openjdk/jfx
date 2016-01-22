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
package ensemble.samples.graphics2d.images.imageoperator;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * A sample that demonstrates the use of two different constructors in the Image
 * class.
 *
 * @sampleName Image Operation
 * @preview preview.png
 * @playground gridSize (name="Grid Size", min=0, max=10)
 * @playground hueFactor (name="Hue Factor", min=0, max=32)
 * @playground hueOffset (name="Hue Offset", min=0, max=360)
 * @see javafx.scene.image.Image
 * @see javafx.scene.image.ImageView
 */
public class ImageOperationApp extends Application {
    private SimpleDoubleProperty gridSize = new SimpleDoubleProperty(3.0);
    private SimpleDoubleProperty hueFactor = new SimpleDoubleProperty(12.0);
    private SimpleDoubleProperty hueOffset = new SimpleDoubleProperty(240.0);

       private static void renderImage(WritableImage img, double gridSize, double hueFactor, double hueOffset) {
        PixelWriter pw = img.getPixelWriter();
        double w = img.getWidth();
        double h = img.getHeight();
        double xRatio = 0.0;
        double yRatio = 0.0;
        double hue = 0.0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                xRatio = x/w;
                yRatio = y/h;
                hue = Math.sin(yRatio*(gridSize*Math.PI))*Math.sin(xRatio*(gridSize*Math.PI))*Math.tan(hueFactor/20.0)*360.0 + hueOffset;
                Color c = Color.hsb(hue, 1.0, 1.0);
                pw.setColor(x, y, c);
            }
        }
    }
    public Parent createContent() {
         StackPane root = new StackPane();
        final WritableImage img = new WritableImage(200, 200);
        gridSize.addListener((Observable observable) -> {
            renderImage(img, gridSize.doubleValue(), hueFactor.doubleValue(), hueOffset.doubleValue());
         });
        hueFactor.addListener((Observable observable) -> {
            renderImage(img, gridSize.doubleValue(), hueFactor.doubleValue(), hueOffset.doubleValue());
         });
        hueOffset.addListener((Observable observable) -> {
            renderImage(img, gridSize.doubleValue(), hueFactor.doubleValue(), hueOffset.doubleValue());
         });
        renderImage(img, 3.0, 12.0, 240.0);

        ImageView view = new ImageView(img);

         root.getChildren().add(view);

        return root;
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /** Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) { launch(args); }
}
