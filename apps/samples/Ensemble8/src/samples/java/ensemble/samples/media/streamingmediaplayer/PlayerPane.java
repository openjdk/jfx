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
package ensemble.samples.media.streamingmediaplayer;

import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class PlayerPane extends BorderPane {

    private MediaPlayer mp;
    private MediaView mediaView;
    private final boolean repeat = false;
    private boolean stopRequested = false;
    private boolean atEndOfMedia = false;
    private Duration duration;
    private Slider timeSlider;
    private Label playTime;
    private Slider volumeSlider;
    private HBox mediaTopBar;
    private HBox mediaBottomBar;
    private ParallelTransition transition = null;

    @Override
    protected void layoutChildren() {
        if (mediaView != null && getBottom() != null) {
            mediaView.setFitWidth(getWidth());
            mediaView.setFitHeight(getHeight() - getBottom().prefHeight(-1));
        }
        super.layoutChildren();
        if (mediaView != null) {
            mediaView.setTranslateX((((Pane) getCenter()).getWidth() - mediaView.prefWidth(-1)) / 2);
            mediaView.setTranslateY((((Pane) getCenter()).getHeight() - mediaView.prefHeight(-1)) / 2);
        }
    }

    @Override
    protected double computeMinWidth(double height) {
        return mediaBottomBar.prefWidth(-1);
    }

    @Override
    protected double computeMinHeight(double width) {
        return 200;
    }

    @Override
    protected double computePrefWidth(double height) {
        return Math.max(mp.getMedia().getWidth(), mediaBottomBar.prefWidth(height));
    }

    @Override
    protected double computePrefHeight(double width) {
        return mp.getMedia().getHeight() + mediaBottomBar.prefHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    public PlayerPane(final MediaPlayer mp) {
        this.mp = mp;
        setId("player-pane");

        mediaView = new MediaView(mp);
        Pane mvPane = new Pane() {
        };
        mvPane.setId("media-pane");
        mvPane.getChildren().add(mediaView);
        setCenter(mvPane);

        mediaTopBar = new HBox();
        mediaTopBar.setPadding(new Insets(5, 10, 5, 10));
        mediaTopBar.setAlignment(Pos.CENTER);
        mediaTopBar.setOpacity(1);
        BorderPane.setAlignment(mediaTopBar, Pos.CENTER);


        mp.currentTimeProperty().addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
            updateValues();
        });
        mp.setOnPlaying(() -> {
            if (stopRequested) {
                mp.pause();
                stopRequested = false;
            }
        });
        mp.setOnReady(() -> {
            duration = mp.getMedia().getDuration();
            updateValues();
        });
        mp.setOnEndOfMedia(() -> {
            if (!repeat) {
                stopRequested = true;
                atEndOfMedia = true;
            }
        });
        mp.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);

        // Time label
        Label timeLabel = new Label("Time");
        timeLabel.setMinWidth(Control.USE_PREF_SIZE);
        timeLabel.setTextFill(Color.WHITE);
        mediaTopBar.getChildren().add(timeLabel);

        // Time slider
        timeSlider = new Slider();
        timeSlider.setId("media-slider");
        timeSlider.setMinWidth(240);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        timeSlider.valueProperty().addListener((Observable ov) -> {
            if (timeSlider.isValueChanging()) {
                // multiply duration by percentage calculated by slider position
                if (duration != null) {
                    mp.seek(duration.multiply(timeSlider.getValue() / 100.0));
                }
                updateValues();
                
            }
        });
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        mediaTopBar.getChildren().add(timeSlider);

        // Play label
        playTime = new Label();
        playTime.setPrefWidth(130);
        playTime.setMinWidth(50);
        playTime.setTextFill(Color.WHITE);
        mediaTopBar.getChildren().add(playTime);

        // Volume label
        Label volumeLabel = new Label("Vol");
        volumeLabel.setMinWidth(Control.USE_PREF_SIZE);
        volumeLabel.setTextFill(Color.WHITE);
        mediaTopBar.getChildren().add(volumeLabel);

        // Volume slider
        volumeSlider = new Slider();
        volumeSlider.setId("media-slider");
        volumeSlider.setPrefWidth(120);
        volumeSlider.setMinWidth(30);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.valueProperty().addListener((Observable ov) -> {
        });
        volumeSlider.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (volumeSlider.isValueChanging()) {
                mp.setVolume(volumeSlider.getValue() / 100.0);
            }
        });
        mediaTopBar.getChildren().add(volumeSlider);

        setTop(mediaTopBar);

        final EventHandler<ActionEvent> backAction = (ActionEvent e) -> {
            mp.seek(Duration.ZERO);
        };
        final EventHandler<ActionEvent> stopAction = (ActionEvent e) -> {
            mp.stop();
        };
        final EventHandler<ActionEvent> playAction = (ActionEvent e) -> {
            mp.play();
        };
        final EventHandler<ActionEvent> pauseAction = (ActionEvent e) -> {
            mp.pause();
        };
        final EventHandler<ActionEvent> forwardAction = (ActionEvent e) -> {
            Duration currentTime = mp.getCurrentTime();
            mp.seek(Duration.seconds(currentTime.toSeconds() + 5.0));
        };

        Button backButton = new Button("Back");
        backButton.setId("back-button");
        backButton.setOnAction(backAction);
 
        Button stopButton = new Button("Stop");
        stopButton.setId("stop-button");
        stopButton.setOnAction(stopAction);
 
        Button playButton = new Button("Play");
        playButton.setId("play-button");
        playButton.setOnAction(playAction);
 
        Button pauseButton = new Button("Pause");
        pauseButton.setId("pause-button");
        pauseButton.setOnAction(pauseAction);
 
        Button forwardButton = new Button("Forward");
        forwardButton.setId("forward-button");
        forwardButton.setOnAction(forwardAction);
        
        mediaBottomBar = new HBox();
        mediaBottomBar.setId("bottom");
        mediaBottomBar.setSpacing(0);
        mediaBottomBar.setAlignment(Pos.CENTER);
        mediaBottomBar.getChildren().addAll(backButton, stopButton, playButton, pauseButton, forwardButton);
        BorderPane.setAlignment(mediaBottomBar, Pos.CENTER);
        setBottom(mediaBottomBar);
    }

    protected void updateValues() {
        if (playTime != null && timeSlider != null && volumeSlider != null && duration != null) {
            Platform.runLater(() -> {
                Duration currentTime = mp.getCurrentTime();
                playTime.setText(formatTime(currentTime, duration));
                timeSlider.setDisable(duration.isUnknown());
                if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !timeSlider.isValueChanging()) {
                    timeSlider.setValue(currentTime.divide(duration).toMillis() * 100.0);
                }
                if (!volumeSlider.isValueChanging()) {
                    volumeSlider.setValue((int) Math.round(mp.getVolume() * 100));
                }
            });
        }
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;

            if (durationHours > 0) {
                return String.format("%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d",
                        elapsedMinutes, elapsedSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d",
                        elapsedMinutes, elapsedSeconds);
            }
        }
    }
}
