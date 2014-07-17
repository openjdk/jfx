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

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/**
 * An alpha media player with 2 different media views and alpha channels.
 *
 * @sampleName Alpha Media Player
 * @preview preview.png
 * @see javafx.scene.media.MediaPlayer
 * @see javafx.scene.media.Media
 * @related /Media/Advanced Media
 * @related /Media/Overlay Media Player
 * @related /Media/Streaming Media Player
 * @playground arthPos (name="Arth Position", min=-100, max=100)
 * @playground fierPos (name="Fier Position", min=-100, max=100)
 * @playground arthRate (name="Arth Rate", min=0.1, max=1)
 * @playground fierRate (name="Fier Rate", min=0.1, max=1)
 * @conditionalFeatures WEB, MEDIA
 */
public class AlphaMediaPlayerApp extends Application {

    private String alphaMediaPlayerCss = AlphaMediaPlayerApp.class.getResource("AlphaMediaPlayer.css").toExternalForm();
    private static final String ARTH_URL = "http://download.oracle.com/otndocs/products/javafx/arth_512.flv";
    private static final String FIER_URL = "http://download.oracle.com/otndocs/products/javafx/fier_512.flv";
    PlanetaryPlayerPane planetaryPlayerPane;
    private MediaPlayer arthPlayer;
    private MediaPlayer fierPlayer;
    SimpleDoubleProperty arthPos = new SimpleDoubleProperty(-90.0);
    SimpleDoubleProperty fierPos = new SimpleDoubleProperty(50.0);
    SimpleDoubleProperty arthRate = new SimpleDoubleProperty(1.0);
    SimpleDoubleProperty fierRate = new SimpleDoubleProperty(1.0);

    public Parent createContent() {
        arthPlayer = new MediaPlayer(new Media(ARTH_URL));
        arthPlayer.setAutoPlay(true);
        fierPlayer = new MediaPlayer(new Media(FIER_URL));
        fierPlayer.setAutoPlay(true);

        arthPos.addListener((Observable observable) -> {
            planetaryPlayerPane.setTranslate1(arthPos.doubleValue());
        });

        fierPos.addListener((Observable observable) -> {
            planetaryPlayerPane.setTranslate2(fierPos.doubleValue());
        });

        arthRate.addListener((Observable observable) -> {
            arthPlayer.setRate(arthRate.doubleValue());
        });

        fierRate.addListener((Observable observable) -> {
            fierPlayer.setRate(fierRate.doubleValue());
        });

        planetaryPlayerPane = new PlanetaryPlayerPane(arthPlayer, fierPlayer);

        planetaryPlayerPane.setMinSize(480, 320);
        planetaryPlayerPane.setPrefSize(480, 320);
        planetaryPlayerPane.setMaxSize(480, 320);
        planetaryPlayerPane.getStylesheets().add(alphaMediaPlayerCss);
        return planetaryPlayerPane;
    }

    public void play() {
        MediaPlayer.Status status = fierPlayer.getStatus();
        if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) {
            return;
        }
        if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.READY) {
            fierPlayer.play();
            arthPlayer.play();
        }
    }

    @Override
    public void stop() {
        fierPlayer.stop();
        arthPlayer.stop();
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
