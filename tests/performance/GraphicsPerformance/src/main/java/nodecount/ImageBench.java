/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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

import javafx.animation.FillTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 */
public class ImageBench extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        final int GAP = 6;
        final int NUM_COLS = 60;
        final int NUM_ROWS = 60;

        Rectangle background = new Rectangle();
        Image image = new Image(ImageBench.class.getResource("Duke_Wave_Opaque.png").toExternalForm());
        ImageView[][] images = new ImageView[NUM_ROWS][NUM_COLS];
        Pane root = new Pane() {
            @Override protected void layoutChildren() {
                background.setWidth(getWidth());
                background.setHeight(getHeight());
                double imageWidth = (getWidth() - ((NUM_COLS-1) * GAP)) / NUM_COLS;
                double imageHeight = (getHeight() - ((NUM_ROWS-1) * GAP)) / NUM_ROWS;
                for (int r=0; r<NUM_ROWS; r++) {
                    for (int c=0; c<NUM_COLS; c++) {
                        double x = c * (GAP + imageWidth);
                        double y = r * (GAP + imageHeight);
                        ImageView i = images[r][c];
                        i.setX(x);
                        i.setY(y);
                        i.setFitWidth(imageWidth);
                        i.setFitHeight(imageHeight);
                    }
                }
            }
        };
        root.getChildren().add(background);
        Scene scene = new Scene(root);

        for (int r=0; r<NUM_ROWS; r++) {
            for (int c=0; c<NUM_COLS; c++) {
                ImageView i = new ImageView();
                i.setImage(image);
                images[r][c] = i;
                root.getChildren().add(i);
            }
        }

        FillTransition tx = new FillTransition(Duration.seconds(5), background, Color.WHITE, Color.BLACK);
        tx.setCycleCount(1000);
        tx.play();

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
