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
package ensemble.samples.media.alphamediaplayer;

import javafx.animation.ParallelTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class PlanetaryPlayerPane extends BorderPane {

    private MediaPlayer mp;
    public Group mediaViewer1;
    public Group mediaViewer2;
    private Group mediaViewerGroup;
    private final boolean repeat = true;
    private boolean stopRequested = false;
    private boolean atEndOfMedia = false;
    private Duration duration;
    private HBox mediaBottomBar;
    private ParallelTransition transition = null;

    public PlanetaryPlayerPane(final MediaPlayer mp1, final MediaPlayer mp2) {
        this.mp = mp1;
        setId("player-pane");

        mediaViewer1 = createViewer(mp1, 0.4, false);
        mediaViewer2 = createViewer(mp2, 0.55, false);

        mediaViewerGroup = new Group();
        mediaViewerGroup.getChildren().add(mediaViewer2);
        mediaViewerGroup.getChildren().add(mediaViewer1);
        mediaViewerGroup.setTranslateX(-17.0);
        mediaViewerGroup.setTranslateY(-115.0);
        setTranslate1(-90.0);
        setTranslate2(50.0);

        Pane mvPane = new Pane() {
        };
        mvPane.setId("media-pane");
        mvPane.getChildren().add(mediaViewerGroup);
        setCenter(mvPane);

        mp1.setOnPlaying(() -> {
            if (stopRequested) {
                mp1.pause();
                stopRequested = false;
            }
        });
        mp1.setOnEndOfMedia(() -> {
            if (!repeat) {
                stopRequested = true;
                atEndOfMedia = true;
            }
        });
        mp1.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);

        mp2.setOnPlaying(() -> {
            if (stopRequested) {
                mp2.pause();
                stopRequested = false;
            }
        });
        mp2.setOnEndOfMedia(() -> {
            if (!repeat) {
                stopRequested = true;
                atEndOfMedia = true;
            }
        });
        mp2.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);

        final EventHandler<ActionEvent> backAction = (ActionEvent e) -> {
            mp1.seek(Duration.ZERO);
            mp2.seek(Duration.ZERO);
        };
        final EventHandler<ActionEvent> stopAction = (ActionEvent e) -> {
            mp1.stop();
            mp2.stop();
        };
        final EventHandler<ActionEvent> playAction = (ActionEvent e) -> {
            mp1.play();
            mp2.play();
        };
        final EventHandler<ActionEvent> pauseAction = (ActionEvent e) -> {
            mp1.pause();
            mp2.pause();
        };
        final EventHandler<ActionEvent> forwardAction = (ActionEvent e) -> {
            Duration currentTime = mp1.getCurrentTime();
            mp1.seek(Duration.seconds(currentTime.toSeconds() + 0.1));
            mp2.seek(Duration.seconds(currentTime.toSeconds() + 0.1));
        };

        mediaBottomBar = new HBox();
        mediaBottomBar.setId("bottom");
        mediaBottomBar.setSpacing(0);
        mediaBottomBar.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(mediaBottomBar, Pos.CENTER);

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

        mediaBottomBar.getChildren().addAll(backButton, stopButton, playButton, pauseButton, forwardButton);
        setBottom(mediaBottomBar);
    }

    public void setTranslate1(double tx) {
        mediaViewer1.setTranslateX(tx);
    }

    public void setTranslate2(double tx) {
        mediaViewer2.setTranslateX(tx);
    }

    private static Group createViewer(final MediaPlayer player, final double scale, boolean blur) {
        Group mediaGroup = new Group();

        final MediaView mediaView = new MediaView(player);

        if (blur) {
            BoxBlur bb = new BoxBlur();
            bb.setWidth(4);
            bb.setHeight(4);
            bb.setIterations(1);
            mediaView.setEffect(bb);
        }

        double width = player.getMedia().getWidth();
        double height = player.getMedia().getHeight();

        mediaView.setFitWidth(width);
        mediaView.setTranslateX(-width / 2.0);
        mediaView.setScaleX(-scale);

        mediaView.setFitHeight(height);
        mediaView.setTranslateY(-height / 2.0);
        mediaView.setScaleY(scale);

        mediaView.setDepthTest(DepthTest.ENABLE);
        mediaGroup.getChildren().add(mediaView);
        return mediaGroup;
    }
}