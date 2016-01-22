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
package ensemble.samples.animation.timeline;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A sample that demonstrates the basics of timeline creation.
 *
 * @sampleName Timeline
 * @preview preview.png
 * @see javafx.animation.KeyFrame
 * @see javafx.animation.KeyValue
 * @see javafx.animation.Timeline
 * @see javafx.util.Duration
 * @related /Animation/Timeline Events
 * @embedded
 */
public class TimelineApp extends Application {

    private Timeline timeline;

    public Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(253, 100);
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

        //create a circle
        final Circle circle = new Circle(25, 25, 20, Color.web("1c89f4"));
        circle.setEffect(new Lighting());

        //create a timeline for moving the circle
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);

        //one can start/pause/stop/play animation by
        //timeline.play();
        //timeline.pause();
        //timeline.stop();
        //timeline.playFromStart();

        //add the following keyframes to the timeline
        timeline.getKeyFrames().addAll(new KeyFrame(Duration.ZERO,
                new KeyValue(circle.translateXProperty(), 0)),
                new KeyFrame(new Duration(4000),
                new KeyValue(circle.translateXProperty(), 205)));
        root.getChildren().addAll(createNavigation(), circle);

        return root;
    }

    private VBox createNavigation() {
        //method for creating navigation panel
        //start/stop/pause/play from start buttons
        Button buttonStart = new Button("Start");
        buttonStart.setOnAction((ActionEvent t) -> {
            //start timeline
            timeline.play();
        });
        Button buttonStop = new Button("Stop");
        buttonStop.setOnAction((ActionEvent t) -> {
            //stop timeline
            timeline.stop();
        });
        Button buttonPlayFromStart = new Button("Restart");
        buttonPlayFromStart.setOnAction((ActionEvent t) -> {
            //play from start
            timeline.playFromStart();
        });
        Button buttonPause = new Button("Pause");
        buttonPause.setOnAction((ActionEvent t) -> {
            //pause from start
            timeline.pause();
        });
        //text showing current time
        final Text currentRateText = new Text("Current time: 0 ms");
        currentRateText.setBoundsType(TextBoundsType.VISUAL);
        timeline.currentTimeProperty().addListener((Observable ov) -> {
            int time = (int) timeline.getCurrentTime().toMillis();
            currentRateText.setText("Current time: " + time + " ms");
        });
        //Autoreverse checkbox
        final CheckBox checkBoxAutoReverse = new CheckBox("Auto Reverse");
        checkBoxAutoReverse.setSelected(true);
        checkBoxAutoReverse.selectedProperty().addListener((Observable ov) -> {
            timeline.setAutoReverse(checkBoxAutoReverse.isSelected());
        });
        //add all navigation to layout
        HBox hBox1 = new HBox(10);
        hBox1.setPadding(new Insets(0, 0, 0, 5));
        hBox1.getChildren().addAll(buttonStart, buttonPause, buttonStop, buttonPlayFromStart);
        hBox1.setAlignment(Pos.CENTER_LEFT);
        HBox hBox2 = new HBox(10);
        hBox2.setPadding(new Insets(0, 0, 0, 5));
        hBox2.getChildren().addAll(checkBoxAutoReverse, currentRateText);
        hBox2.setAlignment(Pos.CENTER_LEFT);
        VBox vBox = new VBox(10);
        vBox.setLayoutY(60);
        vBox.getChildren().addAll(hBox1, hBox2);
        return vBox;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
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
