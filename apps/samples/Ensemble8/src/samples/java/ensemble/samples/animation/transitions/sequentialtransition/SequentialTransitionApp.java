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
package ensemble.samples.animation.transitions.sequentialtransition;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A sample in which various transitions are executed sequentially.
 *
 * @sampleName Sequential Transition
 * @preview preview.png
 * @see javafx.animation.SequentialTransition
 * @see javafx.animation.Transition
 * @related /Animation/Transitions/Fade Transition
 * @related /Animation/Transitions/Fill Transition
 * @related /Animation/Transitions/Parallel Transition
 * @related /Animation/Transitions/Path Transition
 * @related /Animation/Transitions/Pause Transition
 * @related /Animation/Transitions/Rotate Transition
 * @related /Animation/Transitions/Scale Transition
 * @related /Animation/Transitions/Stroke Transition
 * @related /Animation/Transitions/Translate Transition
 * @embedded
 */
public class SequentialTransitionApp extends Application {

    private SequentialTransition sequentialTransition;

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(245, 100);
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

        // create rectangle
        Rectangle rect = new Rectangle(-25,-25,50, 50);
        rect.setArcHeight(15);
        rect.setArcWidth(15);
        rect.setFill(Color.CRIMSON);
        rect.setTranslateX(50);
        rect.setTranslateY(50);
        root.getChildren().add(rect);
        // create 4 transitions
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1));
        fadeTransition.setFromValue(1);
        fadeTransition.setToValue(0.3);
        fadeTransition.setCycleCount(2);
        fadeTransition.setAutoReverse(true);

        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(2));
        translateTransition.setFromX(50);
        translateTransition.setToX(220);
        translateTransition.setCycleCount(2);
        translateTransition.setAutoReverse(true);

        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(2));
        rotateTransition.setByAngle(180);
        rotateTransition.setCycleCount(4);
        rotateTransition.setAutoReverse(true);

        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(2));
        scaleTransition.setToX(2);
        scaleTransition.setToY(2);
        scaleTransition.setCycleCount(2);
        scaleTransition.setAutoReverse(true);
        // create sequential transition to do 4 transitions one after another
        sequentialTransition = new SequentialTransition(rect,
                fadeTransition, translateTransition, rotateTransition, scaleTransition);
        sequentialTransition.setCycleCount(Timeline.INDEFINITE);
        sequentialTransition.setAutoReverse(true);

        return root;
    }

    public void play() {
        sequentialTransition.play();
    }

    @Override
    public void stop() {
        sequentialTransition.stop();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
        play();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
