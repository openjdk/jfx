/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates.
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
package ensemble.samples.graphics2d.gauge;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

/**
 * A sample that demonstrates a simple gauge.
 *
 * @sampleName Speed Gauge
 * @preview preview.png
 * @embedded
 * @see javafx.scene.image.Image
 * @see javafx.scene.image.ImageView
 */
public class SpeedGaugeApp extends Application {

    private final Image backgroundImg = new Image(SpeedGaugeApp.class.getResource("/ensemble/samples/shared-resources/gaugeBackground.png").toExternalForm());
    private final Image needleImg = new Image(SpeedGaugeApp.class.getResource("/ensemble/samples/shared-resources/needle.png").toExternalForm());
    private final SimpleDoubleProperty speed = new SimpleDoubleProperty(0);
    private static final double NEEDLE_CENTER_X = 35;
    private static final double NEEDLE_CENTER_Y = 218;
    private static final double SPEED_CENTER_X = 313;
    private static final double SPEED_CENTER_Y = 226;
    private  AnimationTimer timer;

    public Parent createContent() {

        ImageView speedNeedle = new ImageView(needleImg);

        Rotate speedRotate = new Rotate(0, SPEED_CENTER_X, SPEED_CENTER_Y);
        speedNeedle.getTransforms().addAll(
                speedRotate,
                new Translate(SPEED_CENTER_X - NEEDLE_CENTER_X, SPEED_CENTER_Y - NEEDLE_CENTER_Y));

        speedRotate.angleProperty().bind(speed.divide(3).subtract(120));

        ImageView dial = new ImageView(backgroundImg);
        Pane root = new Pane();
        root.setPrefSize(680, 450);
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.getChildren().addAll(new ImageView(backgroundImg), speedNeedle);

        dial.setFitWidth(640);
        dial.setPreserveRatio(true);

        timer = new AnimationTimer() {
            boolean ascending = true;

            @Override
            public void handle(long l) {
                if (ascending) {
                    speed.setValue(speed.getValue() + 1);
                } else {
                    speed.setValue(speed.getValue() - 1);
                }
                if (speed.getValue() >= 360) { // Ends up making max speed ~80MPH
                    ascending = false;
                }
                if (speed.getValue() <= 0) {
                    ascending = true;
                }
            }
        };
        timer.start();

        return root;
    }

    @Override
    public void start(Stage stage) throws Exception {
        final Scene scene = new Scene(createContent(), 640, 450);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        timer.stop();
    }

    public void play() {
        timer.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
