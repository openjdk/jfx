/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package fxmediaplayer.states;

import fxmediaplayer.FXMediaPlayerControlInterface;
import fxmediaplayer.FXMediaPlayerInterface;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class MediaPlayerStates implements FXMediaPlayerControlInterface {

    private static final double WIDTH = 150;
    private static final float ON_OPACITY = 1.0f;
    private static final float OFF_OPACITY = 0.1f;
    private FXMediaPlayerInterface FXMediaPlayer = null;
    private VBox states = null;
    private Label labelInfo = null;
    private final String labelInfoText =
            "Time: %s\nVolume: %.2f\nBalance: %.2f\nRate: %.2f\nPlay count: %d";
    private boolean infoOn = false;
    private Duration time = Duration.ZERO;
    private double volume = 1.0;
    private double balance = 0.0;
    private double rate = 1.0;
    private int count = 0;
    private Label labelReady = null;
    private Label labelPlaying = null;
    private Label labelPaused = null;
    private Label labelStopped = null;
    private Label labelStalled = null;
    private Label labelDisposed = null;
    private Label labelHalted = null;
    private Label labelEndOfMedia = null;
    private Label labelRepeat = null;
    private Label labelError = null;
    private Label labelCurrentState = null;
    private ProgressIndicator bufferIndicator = null;
    private double mediaDuration = 0.0;
    private ChangeListener<Duration> currentTimePropertyListener = null;
    private InvalidationListener volumePropertyListener = null;
    private InvalidationListener balancePropertyListener = null;
    private InvalidationListener ratePropertyListener = null;
    private InvalidationListener currentCountPropertyListener = null;
    private InvalidationListener statusPropertyListener = null;
    private Runnable onReadyRunnable = null;
    private Runnable onPlayingRunnable = null;
    private Runnable onPausedRunnable = null;
    private Runnable onStoppedRunnable = null;
    private Runnable onStalledRunnable = null;
    private Runnable onHaltedRunnable = null;
    private Runnable onEndOfMediaRunnable = null;
    private Runnable onRepeatRunnable = null;
    private Runnable onErrorRunnable = null;
    private InvalidationListener bufferProgressTimeProperty = null;

    public MediaPlayerStates(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public VBox getStates() {
        if (states == null) {
            states = new VBox();

            states.setMinWidth(WIDTH);
            states.setMaxWidth(WIDTH);
            states.setPrefWidth(WIDTH);
            states.setAlignment(Pos.TOP_CENTER);

            labelInfo = createInfoLabel(getInfoText(), Color.LIGHTGRAY, 80);
            states.getChildren().add(labelInfo);

            labelReady = createStateLabel(MediaPlayer.Status.READY.toString(),
                    Color.LIGHTGRAY);
            states.getChildren().add(labelReady);
            labelPlaying = createStateLabel(MediaPlayer.Status.PLAYING.toString(),
                    Color.LIGHTGRAY);
            states.getChildren().add(labelPlaying);
            labelPaused = createStateLabel(MediaPlayer.Status.PAUSED.toString(),
                    Color.LIGHTGRAY);
            states.getChildren().add(labelPaused);
            labelStopped = createStateLabel(MediaPlayer.Status.STOPPED.toString(),
                    Color.LIGHTGRAY);
            states.getChildren().add(labelStopped);
            labelStalled = createStateLabel(MediaPlayer.Status.STALLED.toString(),
                    Color.LIGHTGRAY);
            states.getChildren().add(labelStalled);
            labelDisposed = createStateLabel(MediaPlayer.Status.DISPOSED.toString(),
                    Color.LIGHTGRAY);
            states.getChildren().add(labelDisposed);
            labelHalted = createStateLabel(MediaPlayer.Status.HALTED.toString(),
                    Color.RED);
            states.getChildren().add(labelHalted);
            labelEndOfMedia = createStateLabel("End Of Media",
                    Color.LIGHTGRAY);
            states.getChildren().add(labelEndOfMedia);
            labelRepeat = createStateLabel("Repeat",
                    Color.GRAY);
            states.getChildren().add(labelRepeat);
            labelError = createStateLabel("Error",
                    Color.RED);
            states.getChildren().add(labelError);

            bufferIndicator = new ProgressIndicator();
            bufferIndicator.setPrefSize(50, 50);
            bufferIndicator.setProgress(0.0);
            states.getChildren().add(bufferIndicator);

            createListeners();
            addListeners();
        }

        return states;
    }

    @Override
    public void onMediaPlayerChanged(MediaPlayer oldMediaPlayer) {
        if (oldMediaPlayer != null) {
            removeListeners(oldMediaPlayer);
        }

        addListeners();
    }

    private void createListeners() {
        currentTimePropertyListener =
                (ObservableValue<? extends Duration> ov, Duration o, Duration n) -> {
            onCurrentTime(n);
        };

        volumePropertyListener = (Observable o) -> {
            onVolume(o);
        };

        balancePropertyListener = (Observable o) -> {
            onBalance(o);
        };

        ratePropertyListener = (Observable o) -> {
            onRate(o);
        };

        currentCountPropertyListener = (Observable o) -> {
            onCurrentCount(o);
        };

        statusPropertyListener = (Observable o) -> {
            onStatus(o);
        };

        onReadyRunnable = () -> {
            onReady();
        };

        onPlayingRunnable = () -> {
            onPlaying();
        };

        onPausedRunnable = () -> {
            onPaused();
        };

        onStoppedRunnable = () -> {
            onStopped();
        };

        onStalledRunnable = () -> {
            onStalled();
        };

        onHaltedRunnable = () -> {
            onHalted();
        };

        onEndOfMediaRunnable = () -> {
            onEndOfMedia();
        };

        onRepeatRunnable = () -> {
            onRepeat();
        };

        onErrorRunnable = () -> {
            onError();
        };

        bufferProgressTimeProperty = (Observable o) -> {
            onBufferProgressTime(o);
        };
    }

    private void addListeners() {
        if (FXMediaPlayer.getMediaPlayer() != null) {
            FXMediaPlayer.getMediaPlayer()
                    .currentTimeProperty().addListener(currentTimePropertyListener);
            FXMediaPlayer.getMediaPlayer()
                    .volumeProperty().addListener(volumePropertyListener);
            FXMediaPlayer.getMediaPlayer()
                    .balanceProperty().addListener(balancePropertyListener);
            FXMediaPlayer.getMediaPlayer()
                    .rateProperty().addListener(ratePropertyListener);
            FXMediaPlayer.getMediaPlayer()
                    .currentCountProperty().addListener(currentCountPropertyListener);
            FXMediaPlayer.getMediaPlayer()
                    .statusProperty().addListener(statusPropertyListener);
            FXMediaPlayer.getMediaPlayer()
                    .setOnReady(onReadyRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnPlaying(onPlayingRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnPaused(onPausedRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnStopped(onStoppedRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnStalled(onStalledRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnHalted(onHaltedRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnEndOfMedia(onEndOfMediaRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnRepeat(onRepeatRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .setOnError(onErrorRunnable);
            FXMediaPlayer.getMediaPlayer()
                    .bufferProgressTimeProperty().addListener(bufferProgressTimeProperty);
        }
    }

    private void removeListeners(MediaPlayer mediaPlayer) {
        mediaPlayer.currentTimeProperty()
                .removeListener(currentTimePropertyListener);
        mediaPlayer.volumeProperty()
                .removeListener(volumePropertyListener);
        mediaPlayer.balanceProperty()
                .removeListener(balancePropertyListener);
        mediaPlayer.rateProperty()
                .removeListener(ratePropertyListener);
        mediaPlayer.currentCountProperty()
                .removeListener(currentCountPropertyListener);
        mediaPlayer.statusProperty()
                .removeListener(statusPropertyListener);
        mediaPlayer.bufferProgressTimeProperty()
                .removeListener(bufferProgressTimeProperty);
        mediaPlayer.setOnReady(null);
        mediaPlayer.setOnPlaying(null);
        mediaPlayer.setOnPaused(null);
        mediaPlayer.setOnStopped(null);
        mediaPlayer.setOnStalled(null);
        mediaPlayer.setOnHalted(null);
        mediaPlayer.setOnEndOfMedia(null);
        mediaPlayer.setOnRepeat(null);
        mediaPlayer.setOnError(null);
    }

    private void onCurrentTime(Duration newValue) {
        time = newValue;
        Platform.runLater(() -> {
            labelInfo.setText(getInfoText());
        });
    }

    private void onVolume(Observable o) {
        DoubleProperty prop = (DoubleProperty) o;
        volume = prop.getValue();
        labelInfo.setText(getInfoText());
    }

    private void onBalance(Observable o) {
        DoubleProperty prop = (DoubleProperty) o;
        balance = prop.getValue();
        labelInfo.setText(getInfoText());
    }

    private void onRate(Observable o) {
        DoubleProperty prop = (DoubleProperty) o;
        rate = prop.getValue();
        labelInfo.setText(getInfoText());
    }

    private void onCurrentCount(Observable o) {
        try {
            ReadOnlyIntegerProperty prop = (ReadOnlyIntegerProperty) o;
            count = prop.getValue();
            labelInfo.setText(getInfoText());
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void onStatus(Observable o) {
        try {
            ReadOnlyObjectProperty<MediaPlayer.Status> prop =
                    (ReadOnlyObjectProperty<MediaPlayer.Status>) o;
            MediaPlayer.Status status = prop.getValue();
            if (status == MediaPlayer.Status.DISPOSED) {
                onDisposed();
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private void onReady() {
        switchState(labelReady);
        infoON(labelInfo);
    }

    private void onPlaying() {
        switchState(labelPlaying);
    }

    private void onPaused() {
        switchState(labelPaused);
    }

    private void onStopped() {
        switchState(labelStopped);
    }

    private void onStalled() {
        switchState(labelStalled);
    }

    private void onDisposed() {
        switchState(labelDisposed);
        infoOFF(labelInfo);
    }

    private void onHalted() {
        switchState(labelHalted);
        infoOFF(labelInfo);
    }

    private void onEndOfMedia() {
        stateONOFF(labelEndOfMedia);
    }

    private void onRepeat() {
        stateONOFF(labelRepeat);
    }

    private void onError() {
        System.err.println(FXMediaPlayer.getMediaPlayer().getError().toString());
        switchState(labelError);
        infoOFF(labelInfo);
    }

    private void onBufferProgressTime(Observable o) {
        if (mediaDuration == 0.0) {
            Duration duration = FXMediaPlayer.getMediaPlayer().getMedia().getDuration();
            if (duration != null && duration != Duration.UNKNOWN) {
                mediaDuration = duration.toMillis();
            }
        }

        if (mediaDuration != 0.0) {
            ReadOnlyObjectProperty prop = (ReadOnlyObjectProperty) o;
            double bufferProgressTime = ((Duration) prop.getValue()).toMillis();
            bufferIndicator.setProgress(bufferProgressTime / mediaDuration);
        }
    }

    private Label createInfoLabel(String state, Color color, int height) {
        Rectangle rect = new Rectangle(0, 0, 100, height);
        rect.setArcHeight(20);
        rect.setArcWidth(20);
        rect.setFill(color);

        Label label = new Label(state, rect);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.setOpacity(OFF_OPACITY);

        return label;
    }

    private String getInfoText() {
        return String.format(labelInfoText, getStringTime(),
                volume, balance, rate, count);
    }

    private String getStringTime() {
        int intElapsed = (int) Math.floor(time.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        int elapsedMinutes = (intElapsed - elapsedHours * 60 * 60) / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (elapsedHours > 0) {
            return String.format("%d:%02d:%02d",
                    elapsedHours, elapsedMinutes, elapsedSeconds);
        } else {
            return String.format("%02d:%02d",
                    elapsedMinutes, elapsedSeconds);
        }
    }

    private void infoON(Label labelInfo) {
        if (infoOn) {
            return;
        }

        FadeTransition ft = new FadeTransition(Duration.millis(500), labelInfo);
        ft.setFromValue(OFF_OPACITY);
        ft.setToValue(ON_OPACITY);
        ft.play();

        bufferIndicator.setDisable(false);

        infoOn = true;
    }

    private void infoOFF(Label labelInfo) {
        if (!infoOn) {
            return;
        }

        FadeTransition ft = new FadeTransition(Duration.millis(500), labelInfo);
        ft.setFromValue(ON_OPACITY);
        ft.setToValue(OFF_OPACITY);
        ft.play();

        bufferIndicator.setDisable(true);

        infoOn = false;
    }

    private Label createStateLabel(String state, Color color) {
        Rectangle rect = new Rectangle(0, 0, 100, 30);
        rect.setArcHeight(20);
        rect.setArcWidth(20);
        rect.setFill(color);

        Label label = new Label(state, rect);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.setOpacity(OFF_OPACITY);

        return label;
    }

    private void switchState(Label labelState) {
        if (labelCurrentState != null) {
            stateOFF(labelCurrentState);
        }
        stateON(labelState);
        labelCurrentState = labelState;
    }

    private void stateON(Label labelState) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), labelState);
        ft.setFromValue(OFF_OPACITY);
        ft.setToValue(ON_OPACITY);
        ft.play();
    }

    private void stateOFF(Label labelState) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), labelState);
        ft.setFromValue(ON_OPACITY);
        ft.setToValue(OFF_OPACITY);
        ft.play();
    }

    private void stateONOFF(Label labelState) {
        SequentialTransition st = new SequentialTransition();
        FadeTransition ftON = new FadeTransition(Duration.millis(500), labelState);
        ftON.setFromValue(OFF_OPACITY);
        ftON.setToValue(ON_OPACITY);
        FadeTransition ftOFF = new FadeTransition(Duration.millis(500), labelState);
        ftOFF.setFromValue(ON_OPACITY);
        ftOFF.setToValue(OFF_OPACITY);
        st.getChildren().addAll(ftON, ftOFF);
        st.play();
    }
}
