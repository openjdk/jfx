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
package ensemble.samples.animation.transitions.paralleltransition;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
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
 * A sample in which various transitions are executed in parallel.
 *
 * @sampleName Parallel Transition
 * @preview preview.png
 * @see javafx.animation.ParallelTransition
 * @see javafx.animation.Transition
 * @related /Animation/Transitions/Fade Transition
 * @related /Animation/Transitions/Fill Transition
 * @related /Animation/Transitions/Path Transition
 * @related /Animation/Transitions/Pause Transition
 * @related /Animation/Transitions/Rotate Transition
 * @related /Animation/Transitions/Scale Transition
 * @related /Animation/Transitions/Sequential Transition
 * @related /Animation/Transitions/Stroke Transition
 * @related /Animation/Transitions/Translate Transition
 * @embedded
 */
public class ParallelTransitionApp extends Application {

    private ParallelTransition parallelTransition;

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(400, 200);
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

        Rectangle rect = new Rectangle(-25,-25,50, 50);
        rect.setArcHeight(15);
        rect.setArcWidth(15);
        rect.setFill(Color.CRIMSON);
        rect.setTranslateX(50);
        rect.setTranslateY(75);
        root.getChildren().add(rect);
        // create parallel transition to do all 4 transitions at the same time
        FadeTransition fadeTrans = new FadeTransition(Duration.seconds(3), rect);
        fadeTrans.setFromValue(1);
        fadeTrans.setToValue(0.3);
        fadeTrans.setAutoReverse(true);

        TranslateTransition translateTran = new TranslateTransition(Duration.seconds(2));
        translateTran.setFromX(50);
        translateTran.setToX(320);
        translateTran.setCycleCount(2);
        translateTran.setAutoReverse(true);

        RotateTransition rotateTran = new RotateTransition(Duration.seconds(3));
        rotateTran.setByAngle(180);
        rotateTran.setCycleCount(4);
        rotateTran.setAutoReverse(true);

        ScaleTransition scaleTran = new ScaleTransition(Duration.seconds(2));
        scaleTran.setToX(2);
        scaleTran.setToY(2);
        scaleTran.setCycleCount(2);
        scaleTran.setAutoReverse(true);

        parallelTransition = new ParallelTransition(rect, fadeTrans,
                translateTran, rotateTran, scaleTran);
        parallelTransition.setCycleCount(Timeline.INDEFINITE);
        parallelTransition.setAutoReverse(true);

        return root;
    }

    public void play() {
        parallelTransition.play();
    }

    @Override
    public void stop() {
        parallelTransition.stop();
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
