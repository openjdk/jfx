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
package ensemble.samples.media.streamingmediaplayer;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/**
 * A media player with controls for play, pause, stop, seek, and volume. This
 * media player is playing media via HTTP Live Streaming, also known as HLS.
 *
 * @sampleName Streaming Media Player
 * @preview preview.png
 * @see javafx.scene.media.MediaPlayer
 * @see javafx.scene.media.Media
 * @related /Media/Advanced Media
 * @related /Media/Alpha Media Player
 * @related /Media/Overlay Media Player
 * @conditionalFeatures WEB, MEDIA
 */
public class StreamingMediaPlayerApp extends Application {

    private String streamingMediaPlayerCss = StreamingMediaPlayerApp.class.getResource("StreamingMediaPlayer.css").toExternalForm();
    private static final String MEDIA_URL = "http://download.oracle.com/otndocs/products/javafx/JavaRap/prog_index.m3u8";
    private MediaPlayer mediaPlayer;

    public Parent createContent() {
        mediaPlayer = new MediaPlayer(new Media(MEDIA_URL));
        mediaPlayer.setAutoPlay(true);
        PlayerPane playerPane = new PlayerPane(mediaPlayer);
        playerPane.setMinSize(480, 360);
        playerPane.setPrefSize(480, 360);
        playerPane.setMaxSize(480, 360);
        playerPane.getStylesheets().add(streamingMediaPlayerCss);

        return playerPane;
    }

    public void play() {
        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) {
            return;
        }
        if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.READY) {
            mediaPlayer.play();
        }
    }

    @Override
    public void stop() {
        mediaPlayer.stop();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
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
