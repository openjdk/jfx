/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
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
package ensemble.samples.charts.area.audio;


import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;


/**
 * An area chart that shows audio spectrum of a music file being played.
 *
 * @sampleName Audio Area Chart
 * @preview preview.png
 * @see javafx.scene.chart.AreaChart
 * @see javafx.scene.chart.Chart
 * @see javafx.scene.chart.NumberAxis
 * @see javafx.scene.chart.XYChart
 * @see javafx.scene.media.AudioSpectrumListener
 * @see javafx.scene.media.Media
 * @see javafx.scene.media.MediaPlayer
 * @docUrl https://docs.oracle.com/javafx/2/charts/jfxpub-charts.htm Using JavaFX Charts Tutorial
 * @conditionalFeatures MEDIA
 */
public class AudioAreaChartApp extends Application {

    private XYChart.Data<Number, Number>[] series1Data;
    private AudioSpectrumListener audioSpectrumListener;
    private static final String AUDIO_URI = System.getProperty("demo.audio.url",
            "http://download.oracle.com/otndocs/javafx/JavaRap_Audio.mp4");
    private MediaPlayer audioMediaPlayer;
    private static final boolean PLAY_AUDIO = Boolean.parseBoolean(
            System.getProperty("demo.play.audio", "true"));

    public AudioAreaChartApp() {
        audioSpectrumListener = (double timestamp, double duration, float[] magnitudes, float[] phases) -> {
            for (int i = 0; i < series1Data.length; i++) {
                series1Data[i].setYValue(magnitudes[i] + 60);
            }
        };
    }

    public void play() {
        startAudio();
    }

    @Override
    public void stop() {
        stopAudio();
    }

    public Parent createContent() {
        final NumberAxis xAxis = new NumberAxis(0, 128, 8);
        final NumberAxis yAxis = new NumberAxis(0, 50, 10);
        final AreaChart<Number, Number> ac = new AreaChart<>(xAxis, yAxis);
        // setup chart
        ac.getStylesheets().add(AudioAreaChartApp.class
                .getResource("AudioAreaChart.css").toExternalForm());
        ac.setLegendVisible(false);
        ac.setTitle("Live Audio Spectrum Data");
        ac.setAnimated(false);
        xAxis.setLabel("Frequency Bands");
        yAxis.setLabel("Magnitudes");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, null, "dB"));
        // add starting data
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Audio Spectrum");
        //noinspection unchecked
        series1Data = new XYChart.Data[(int) xAxis.getUpperBound()];
        for (int i = 0; i < series1Data.length; i++) {
            series1Data[i] = new XYChart.Data<Number, Number>(i, 50);
            series.getData().add(series1Data[i]);
        }
        ac.getData().add(series);
        return ac;
    }

    private void startAudio() {
        if (PLAY_AUDIO) {
            getAudioMediaPlayer().
                    setAudioSpectrumListener(audioSpectrumListener);
            getAudioMediaPlayer().play();
        }
    }

    private void stopAudio() {
        if (getAudioMediaPlayer().getAudioSpectrumListener() == audioSpectrumListener) {
            getAudioMediaPlayer().pause();
        }
    }

    private MediaPlayer getAudioMediaPlayer() {
        if (audioMediaPlayer == null) {
            Media audioMedia = new Media(AUDIO_URI);
            audioMediaPlayer = new MediaPlayer(audioMedia);
            audioMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        }
        return audioMediaPlayer;
    }

    @Override public void start(Stage primaryStage) throws Exception {
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
