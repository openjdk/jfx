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
import java.util.ArrayList;
import java.util.Iterator;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;

public class MediaPlayerEqualizerTab implements FXMediaPlayerControlInterface {

    private FXMediaPlayerInterface FXMediaPlayer = null;
    private Tab equalizerTab = null;
    private ToggleButton buttonEnable = null;
    private Button buttonReset = null;
    private Label bandIndexLabel = null;
    private Label bandIndexInfoLabel = null;
    private Label centerFrequencyLabel = null;
    private Label centerFrequencyInfoLabel = null;
    private Label bandwidthLabel = null;
    private Label bandwidthInfoLabel = null;
    private Label gainLabel = null;
    private Label gainInfoLabel = null;
    private HBox equalizerControl = null;
    private ArrayList<EqualizerBandControl> equalizerBandControls = null;
    private InvalidationListener statusPropertyListener = null;

    public MediaPlayerEqualizerTab(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public Tab getEqualizerTab() {
        if (equalizerTab == null) {
            equalizerBandControls = new ArrayList<>();

            equalizerTab = new Tab();
            equalizerTab.setText("Equalizer");

            VBox equalizerTabContent = new VBox();
            equalizerTabContent.setId("mediaPlayerTab");
            equalizerTabContent.setAlignment(Pos.TOP_CENTER);

            // ToolBar
            ToolBar toolBar = new ToolBar();

            buttonEnable = new ToggleButton("Enable");
            if (FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                buttonEnable.setText("Enabled");
                buttonEnable.setSelected(true);
            } else {
                buttonEnable.setSelected(false);
            }
            buttonEnable.setOnAction((ActionEvent event) -> {
                onButtonEnable();
            });
            toolBar.getItems().add(buttonEnable);

            buttonReset = new Button("Reset");
            if (!FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                buttonReset.setDisable(true);
            }
            buttonReset.setOnAction((ActionEvent event) -> {
                onButtonReset();
            });
            toolBar.getItems().add(buttonReset);

            bandIndexInfoLabel = new Label();
            bandIndexLabel = new Label("Band index:", bandIndexInfoLabel);
            bandIndexLabel.setContentDisplay(ContentDisplay.RIGHT);
            if (!FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                bandIndexLabel.setDisable(true);
            }
            toolBar.getItems().add(bandIndexLabel);

            centerFrequencyInfoLabel = new Label();
            centerFrequencyLabel = new Label("Frequency:", centerFrequencyInfoLabel);
            centerFrequencyLabel.setContentDisplay(ContentDisplay.RIGHT);
            if (!FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                centerFrequencyLabel.setDisable(true);
            }
            toolBar.getItems().add(centerFrequencyLabel);

            bandwidthInfoLabel = new Label();
            bandwidthLabel = new Label("Bandwidth:", bandwidthInfoLabel);
            bandwidthLabel.setContentDisplay(ContentDisplay.RIGHT);
            if (!FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                bandwidthLabel.setDisable(true);
            }
            toolBar.getItems().add(bandwidthLabel);

            gainInfoLabel = new Label();
            gainLabel = new Label("Gain:", gainInfoLabel);
            gainLabel.setContentDisplay(ContentDisplay.RIGHT);
            if (!FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                gainLabel.setDisable(true);
            }
            toolBar.getItems().add(gainLabel);

            equalizerTabContent.getChildren().add(toolBar);

            // Equalizer control
            equalizerControl = new HBox(10);
            if (!FXMediaPlayer.getMediaPlayer().getAudioEqualizer().isEnabled()) {
                equalizerControl.setDisable(true);
            }
            equalizerControl.setAlignment(Pos.CENTER);
            equalizerTabContent.getChildren().add(equalizerControl);
            equalizerTab.setContent(equalizerTabContent);

            // Create our own list of bands based on default ones
            createBands();

            // Clear default bands
            clearBands(FXMediaPlayer.getMediaPlayer());

            // Add our own bands
            addBands();
        }

        return equalizerTab;
    }

    @Override
    public void onMediaPlayerChanged(MediaPlayer oldMediaPlayer) {
        removeListeners(oldMediaPlayer);

        if (oldMediaPlayer != null) {
            oldMediaPlayer.getAudioEqualizer().setEnabled(false);
            clearBands(oldMediaPlayer);
        }

        addListeners();

        if (FXMediaPlayer.getMediaPlayer() != null) {
            clearBands(FXMediaPlayer.getMediaPlayer());
            addBands();
            onButtonEnable();
        }
    }

    @SuppressWarnings("unchecked")
    private void addListeners() {
        if (FXMediaPlayer.getMediaPlayer() == null) {
            return;
        }

        statusPropertyListener = (Observable o) -> {
            ReadOnlyObjectProperty<MediaPlayer.Status> prop =
                    (ReadOnlyObjectProperty<MediaPlayer.Status>) o;
            MediaPlayer.Status status = prop.getValue();
            if (status == MediaPlayer.Status.READY) {
                equalizerTab.setDisable(false);
            } else if (status == MediaPlayer.Status.DISPOSED ||
                    status == MediaPlayer.Status.HALTED) {
                equalizerTab.setDisable(true);
            }
        };

        FXMediaPlayer.getMediaPlayer()
                .statusProperty().addListener(statusPropertyListener);
    }

    private void removeListeners(MediaPlayer mediaPlayer) {
        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.statusProperty()
                .removeListener(statusPropertyListener);
    }

    private void createBands() {
        Iterator<EqualizerBand> bandsInterator = FXMediaPlayer.getMediaPlayer()
                .getAudioEqualizer().getBands().iterator();
        int bandIndex = 0;
        while (bandsInterator.hasNext()) {
            EqualizerBand equalizerBand = bandsInterator.next();
            onEqualizerBandAdded(bandIndex, equalizerBand.getCenterFrequency(),
                    equalizerBand.getBandwidth(), equalizerBand.getGain());
            bandIndex++;
        }
    }

    private void clearBands(MediaPlayer mediaPlayer) {
        mediaPlayer.getAudioEqualizer().getBands().clear();
    }

    private void addBands() {
        Iterator<EqualizerBandControl> controlsInterator
                = equalizerBandControls.iterator();
        while (controlsInterator.hasNext()) {
            EqualizerBandControl equalizerBandControl = controlsInterator.next();
            FXMediaPlayer.getMediaPlayer().getAudioEqualizer().getBands()
                    .add(equalizerBandControl.getBand());
        }
    }

    private void onButtonEnable() {
        if (buttonEnable.isSelected()) {
            FXMediaPlayer.getMediaPlayer().getAudioEqualizer().setEnabled(true);
            buttonReset.setDisable(false);
            bandIndexLabel.setDisable(false);
            centerFrequencyLabel.setDisable(false);
            bandwidthLabel.setDisable(false);
            gainLabel.setDisable(false);
            equalizerControl.setDisable(false);
        } else {
            FXMediaPlayer.getMediaPlayer().getAudioEqualizer().setEnabled(false);
            buttonReset.setDisable(true);
            bandIndexLabel.setDisable(true);
            centerFrequencyLabel.setDisable(true);
            bandwidthLabel.setDisable(true);
            gainLabel.setDisable(true);
            equalizerControl.setDisable(true);
        }
    }

    private void onButtonReset() {
        Iterator<EqualizerBandControl> interator = equalizerBandControls.iterator();
        while (interator.hasNext()) {
            EqualizerBandControl equalizerBandControl = interator.next();
            equalizerBandControl.setGain(0.0);
        }
    }

    private void onEqualizerBandAdded(int bandIndex, double centerFrequency,
            double bandwidth, double gain) {
        EqualizerBandControl equalizerBandControl
                = new EqualizerBandControl(bandIndex, centerFrequency, bandwidth, gain);
        equalizerBandControls.add(equalizerBandControl);
        equalizerControl.getChildren()
                .add(equalizerBandControl.getEqualizerBandControl());
    }

    private synchronized void onEqualizerBandControl(int bandIndex,
            double centerFrequency, double bandwidth, double gain) {
        bandIndexInfoLabel.setText(String.format("%d", bandIndex));
        centerFrequencyInfoLabel.setText(String.format("%.2f", centerFrequency));
        bandwidthInfoLabel.setText(String.format("%.2f", bandwidth));
        gainInfoLabel.setText(String.format("%.2f", gain));
    }

    private class EqualizerBandControl {

        private int bandIndex = 0;
        private EqualizerBand equalizerBand = null;
        private VBox control = null;
        private Slider slider = null;

        public EqualizerBandControl(int bandIndex, double centerFrequency,
                double bandwidth, double gain) {
            this.bandIndex = bandIndex;
            equalizerBand = new EqualizerBand(centerFrequency, bandwidth, gain);
            if (bandIndex == 0) {
                onEqualizerBandControl(bandIndex,
                        equalizerBand.getCenterFrequency(),
                        equalizerBand.getBandwidth(),
                        equalizerBand.getGain());
            }
        }

        public VBox getEqualizerBandControl() {
            if (control == null) {
                control = new VBox();
                control.setAlignment(Pos.CENTER);

                slider = new Slider(EqualizerBand.MIN_GAIN,
                        EqualizerBand.MAX_GAIN, 1);
                slider.setOrientation(Orientation.VERTICAL);
                slider.setValue(equalizerBand.getGain());
                slider.setPrefHeight(100);
                slider.valueProperty().addListener(
                        (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                    onSlider();
                });
                slider.setOnMouseClicked((MouseEvent me) -> {
                    onEqualizerBandControl(bandIndex,
                            equalizerBand.getCenterFrequency(),
                            equalizerBand.getBandwidth(),
                            equalizerBand.getGain());
                });

                Label label = new Label(String.valueOf(bandIndex), slider);
                label.setContentDisplay(ContentDisplay.TOP);

                control.getChildren().add(label);
            }

            return control;
        }

        public void setGain(double gain) {
            equalizerBand.setGain(gain);
            slider.setValue(gain);
        }

        public EqualizerBand getBand() {
            return equalizerBand;
        }

        private void onSlider() {
            if (slider.isValueChanging()) {
                equalizerBand.setGain(slider.getValue());
            }
        }
    }
}
