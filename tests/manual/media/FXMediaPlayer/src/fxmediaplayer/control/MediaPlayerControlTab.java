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

package fxmediaplayer.control;

import fxmediaplayer.FXMediaPlayerControlInterface;
import fxmediaplayer.FXMediaPlayerInterface;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class MediaPlayerControlTab implements FXMediaPlayerControlInterface {

    private FXMediaPlayerInterface FXMediaPlayer = null;
    private Duration duration = null;
    private Tab controlTab = null;
    private VBox control = null;
    private final Slider timeSlider = new Slider();
    private boolean disableTimeSliderUpdate = false;
    private Slider volumeSlider = null;
    private Slider balanceSlider = null;
    private Slider rateSlider = null;
    private Button buttonResetSlider = null;
    private TextField startTimeTextField = null;
    private TextField stopTimeTextField = null;
    private Button buttonSetStartStopTime = null;
    private TextField cycleCountTextField = null;
    private Button buttonSetCycleCount = null;
    private InvalidationListener durationPropertyListener = null;
    private ChangeListener<Duration> currentTimePropertyListener = null;
    private InvalidationListener volumePropertyListener = null;
    private InvalidationListener balancePropertyListener = null;
    private InvalidationListener ratePropertyListener = null;
    private InvalidationListener cycleCountPropertyListener = null;
    private InvalidationListener statusPropertyListener = null;

    public MediaPlayerControlTab(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public Tab getControlTab() {
        if (controlTab == null) {
            controlTab = new Tab();
            controlTab.setText("Control");

            control = new VBox(15);
            control.setId("mediaPlayerTab");
            control.setAlignment(Pos.CENTER);

            controlTab.setContent(control);

            // Create time slider
            timeSlider.setMinWidth(50);
            timeSlider.setMaxWidth(Double.MAX_VALUE);
            timeSlider.setOnMousePressed((MouseEvent me) -> {
                onTimeSliderPressed();
            });
            timeSlider.setOnMouseReleased((MouseEvent me) -> {
                onTimeSliderReleased();
            });
            timeSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onTimeSlider();
            });
            timeSlider.setDisable(true);

            control.getChildren().add(timeSlider);

            // Volume, Balance and Rate
            HBox hBox = new HBox(5);
            hBox.setAlignment(Pos.CENTER);

            // Volume
            volumeSlider = new Slider(0, 100, 1);
            volumeSlider.setValue(100);
            volumeSlider.setPrefWidth(70);
            volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
            volumeSlider.setMinWidth(30);
            volumeSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onVolumeSlider();
            });
            Label label = new Label("Volume:", volumeSlider);
            label.setContentDisplay(ContentDisplay.RIGHT);
            hBox.getChildren().add(label);

            // Balance
            balanceSlider = new Slider(-100, 100, 1);
            balanceSlider.setValue(0);
            balanceSlider.setPrefWidth(140);
            balanceSlider.setMaxWidth(Region.USE_PREF_SIZE);
            balanceSlider.setMinWidth(60);
            balanceSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onBalanceSlider();
            });
            label = new Label("Balance:", balanceSlider);
            label.setContentDisplay(ContentDisplay.RIGHT);
            hBox.getChildren().add(label);

            // Rate
            rateSlider = new Slider(0, 800, 1);
            rateSlider.setValue(100);
            rateSlider.setPrefWidth(140);
            rateSlider.setMaxWidth(Region.USE_PREF_SIZE);
            rateSlider.setMinWidth(60);
            rateSlider.setOnMouseReleased((MouseEvent me) -> {
                onRateSlider();
            });
            label = new Label("Rate:", rateSlider);
            label.setContentDisplay(ContentDisplay.RIGHT);
            hBox.getChildren().add(label);

            buttonResetSlider = new Button("Reset");
            buttonResetSlider.setOnAction((ActionEvent event) -> {
                onButtonResetSlider();
            });
            hBox.getChildren().add(buttonResetSlider);

            control.getChildren().add(hBox);

            hBox = new HBox(5);
            hBox.setAlignment(Pos.CENTER);

            startTimeTextField = new TextField();
            startTimeTextField.setPrefWidth(50);
            label = new Label("Start (s):", startTimeTextField);
            label.setContentDisplay(ContentDisplay.RIGHT);
            hBox.getChildren().add(label);

            stopTimeTextField = new TextField();
            stopTimeTextField.setPrefWidth(50);
            label = new Label("Stop (s):", stopTimeTextField);
            label.setContentDisplay(ContentDisplay.RIGHT);
            hBox.getChildren().add(label);

            buttonSetStartStopTime = new Button("Set");
            buttonSetStartStopTime.setOnAction((ActionEvent event) -> {
                onButtonSetStartStopTime();
            });
            hBox.getChildren().add(buttonSetStartStopTime);

            cycleCountTextField = new TextField();
            cycleCountTextField.setPrefWidth(50);
            label = new Label("Cycle count:", cycleCountTextField);
            label.setContentDisplay(ContentDisplay.RIGHT);
            hBox.getChildren().add(label);

            buttonSetCycleCount = new Button("Set");
            buttonSetCycleCount.setOnAction((ActionEvent event) -> {
                onButtonSetCycleCount();
            });
            hBox.getChildren().add(buttonSetCycleCount);

            control.getChildren().add(hBox);

            createListeners();
            addListeners();
        }

        return controlTab;
    }

    @Override
    public void onMediaPlayerChanged(MediaPlayer oldMediaPlayer) {
        if (oldMediaPlayer != null) {
            removeListeners(oldMediaPlayer);
        }

        duration = null;
        startTimeTextField.setText("");
        stopTimeTextField.setText("");
        cycleCountTextField.setText("");

        volumeSlider.setValue(100);
        balanceSlider.setValue(0);
        rateSlider.setValue(100);

        addListeners();
    }

    @SuppressWarnings("unchecked")
    private void createListeners() {
        durationPropertyListener = (Observable o) -> {
            ReadOnlyObjectProperty property = (ReadOnlyObjectProperty) o;
            Duration d = ((Duration) property.getValue());
            if (d.isIndefinite()) {
                timeSlider.setDisable(true);
            } else {
                if (d.toMillis() > 0) {
                    if (duration == null || !duration.equals(duration)) {
                        duration = d;
                        timeSlider.setDisable(false);
                    }
                }
            }
        };

        currentTimePropertyListener =
                (ObservableValue<? extends Duration> ov, Duration o, Duration n) -> {
            if (duration != null) {
                final Duration currentTime = n;
                Platform.runLater(() -> {
                    synchronized (timeSlider) {
                        if (!disableTimeSliderUpdate) {
                            if (duration != null) {
                                timeSlider.setValue(currentTime.divide(duration.toMillis()).toMillis() * 100.0);
                            } else {
                                timeSlider.setValue(0.0);
                            }
                        }
                    }
                });
            }
        };

        volumePropertyListener = (Observable o) -> {
            DoubleProperty prop = (DoubleProperty) o;
            final double value = prop.getValue();
            Platform.runLater(() -> {
                volumeSlider.setValue(value * 100.0);
            });
        };

        balancePropertyListener = (Observable o) -> {
            DoubleProperty prop = (DoubleProperty) o;
            final double value = prop.getValue();
            Platform.runLater(() -> {
                balanceSlider.setValue(value * 100.0);
            });
        };

        ratePropertyListener = (Observable o) -> {
            DoubleProperty prop = (DoubleProperty) o;
            final double value = prop.getValue();
            Platform.runLater(() -> {
                rateSlider.setValue(value * 100.0);
            });
        };

        cycleCountPropertyListener = (Observable o) -> {
            IntegerProperty prop = (IntegerProperty) o;
            final int value = prop.getValue();
            Platform.runLater(() -> {
                switch (value) {
                    case MediaPlayer.INDEFINITE:
                        cycleCountTextField.setText("-1");
                        break;
                    case 1:
                        cycleCountTextField.setText("");
                        break;
                    default:
                        cycleCountTextField.setText(String.valueOf(value));
                        break;
                }
            });
        };

        statusPropertyListener = (Observable o) -> {
            ReadOnlyObjectProperty<MediaPlayer.Status> prop =
                    (ReadOnlyObjectProperty<MediaPlayer.Status>) o;
            MediaPlayer.Status status = prop.getValue();
            if (status == MediaPlayer.Status.READY) {
                controlTab.setDisable(false);
            } else if (status == MediaPlayer.Status.DISPOSED ||
                    status == MediaPlayer.Status.HALTED) {
                controlTab.setDisable(true);
            }
        };
    }

    private void addListeners() {
        MediaPlayer mediaPlayer = FXMediaPlayer.getMediaPlayer();
        if (mediaPlayer != null) {
            mediaPlayer.getMedia().durationProperty()
                    .addListener(durationPropertyListener);
            mediaPlayer.currentTimeProperty()
                    .addListener(currentTimePropertyListener);
            mediaPlayer.volumeProperty()
                    .addListener(volumePropertyListener);
            mediaPlayer.balanceProperty()
                    .addListener(balancePropertyListener);
            mediaPlayer.rateProperty()
                    .addListener(ratePropertyListener);
            mediaPlayer.cycleCountProperty()
                    .addListener(cycleCountPropertyListener);
            mediaPlayer.statusProperty()
                    .addListener(statusPropertyListener);
        }
    }

    private void removeListeners(MediaPlayer mediaPlayer) {
        mediaPlayer.getMedia().durationProperty()
                .removeListener(durationPropertyListener);
        mediaPlayer.currentTimeProperty()
                .removeListener(currentTimePropertyListener);
        mediaPlayer.volumeProperty()
                .removeListener(volumePropertyListener);
        mediaPlayer.balanceProperty()
                .removeListener(balancePropertyListener);
        mediaPlayer.rateProperty()
                .removeListener(ratePropertyListener);
        mediaPlayer.statusProperty()
                .removeListener(statusPropertyListener);
    }

    private void onTimeSliderPressed() {
        synchronized (timeSlider) {
            disableTimeSliderUpdate = true;
        }
    }

    private void onTimeSliderReleased() {
        synchronized (timeSlider) {
            if (!FXMediaPlayer.getScrubbing()) {
                FXMediaPlayer.getMediaPlayer()
                        .seek(duration.multiply(timeSlider.getValue() / 100.0));
            }
            disableTimeSliderUpdate = false;
        }
    }

    private void onTimeSlider() {
        if (FXMediaPlayer.getScrubbing()) {
            if (timeSlider.isValueChanging()) {
                FXMediaPlayer.getMediaPlayer()
                        .seek(duration.multiply(timeSlider.getValue() / 100.0));
            }
        }
    }

    private void onVolumeSlider() {
        if (volumeSlider.isValueChanging()) {
            FXMediaPlayer.getMediaPlayer()
                    .setVolume(volumeSlider.getValue() / 100.0);
        }
    }

    private void onBalanceSlider() {
        if (balanceSlider.isValueChanging()) {
            FXMediaPlayer.getMediaPlayer()
                    .setBalance(balanceSlider.getValue() / 100.0);
        }
    }

    private void onRateSlider() {
        FXMediaPlayer.getMediaPlayer().setRate(rateSlider.getValue() / 100.0);
    }

    private void onButtonResetSlider() {
        FXMediaPlayer.getMediaPlayer().setVolume(1.0);
        FXMediaPlayer.getMediaPlayer().setBalance(0.0);
        FXMediaPlayer.getMediaPlayer().setRate(1.0);
    }

    private void onButtonSetStartStopTime() {
        if (startTimeTextField.getText() == null ||
                startTimeTextField.getText().isEmpty()) {
            if (FXMediaPlayer.getMediaPlayer().getStartTime() != Duration.ZERO) {
                FXMediaPlayer.getMediaPlayer().setStartTime(Duration.ZERO);
            }
        } else {
            FXMediaPlayer.getMediaPlayer().setStartTime(
                    new Duration(Double.parseDouble(startTimeTextField.getText()) * 1000));
        }

        if (stopTimeTextField.getText() == null ||
                stopTimeTextField.getText().isEmpty()) {
            FXMediaPlayer.getMediaPlayer().setStopTime(
                    FXMediaPlayer.getMediaPlayer().getMedia().getDuration());
        } else {
            FXMediaPlayer.getMediaPlayer().setStopTime(
                    new Duration(Double.parseDouble(stopTimeTextField.getText()) * 1000));
        }
    }

    private void onButtonSetCycleCount() {
        if (cycleCountTextField.getText() == null ||
                cycleCountTextField.getText().isEmpty()) {
            FXMediaPlayer.getMediaPlayer().setCycleCount(1);
        } else {
            int value = Integer.parseInt(cycleCountTextField.getText());
            if (value < 0) {
                FXMediaPlayer.getMediaPlayer().setCycleCount(MediaPlayer.INDEFINITE);
            } else {
                FXMediaPlayer.getMediaPlayer().setCycleCount(value);
            }
        }
    }
}
