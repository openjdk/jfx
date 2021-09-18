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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.SepiaTone;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;

public class MediaPlayerEffectsTab implements FXMediaPlayerControlInterface {

    private FXMediaPlayerInterface FXMediaPlayer = null;
    private ColorAdjust colorAdjust = null;
    private SepiaTone sepiaTone = null;
    private Tab effectsTab = null;
    private ToggleButton enableButton = null;
    private Button resetButton = null;
    private GridPane controlGrid = null;
    private Label brightnessLabel = null;
    private Slider brightnessSlider = null;
    private Label contrastLabel = null;
    private Slider contrastSlider = null;
    private Label hueLabel = null;
    private Slider hueSlider = null;
    private Label saturationLabel = null;
    private Slider saturationSlider = null;
    private Label sepiaToneLabel = null;
    private Slider sepiaToneSlider = null;
    private InvalidationListener statusPropertyListener = null;

    public MediaPlayerEffectsTab(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public Tab getColorAdjustTab() {
        if (effectsTab == null) {
            colorAdjust = new ColorAdjust();
            sepiaTone = new SepiaTone();

            effectsTab = new Tab();
            effectsTab.setText("Effects");

            VBox colorAdjustTabContent = new VBox(0);
            colorAdjustTabContent.setId("mediaPlayerTab");
            colorAdjustTabContent.setAlignment(Pos.TOP_CENTER);

            // ToolBar
            ToolBar toolBar = new ToolBar();

            enableButton = new ToggleButton("Enable");
            enableButton.setOnAction((ActionEvent event) -> {
                onEnableButton();
            });
            toolBar.getItems().add(enableButton);

            resetButton = new Button("Reset");
            resetButton.setDisable(true);
            resetButton.setOnAction((ActionEvent event) -> {
                onResetButton();
            });
            toolBar.getItems().add(resetButton);

            colorAdjustTabContent.getChildren().add(toolBar);

            controlGrid = new GridPane();
            controlGrid.setPadding(new Insets(10, 0, 0, 0));
            controlGrid.setAlignment(Pos.CENTER);
            controlGrid.setHgap(5);
            controlGrid.setVgap(7);

            brightnessSlider = new Slider(-100, 100, 1);
            brightnessSlider.setValue(0);
            brightnessSlider.setPrefWidth(140);
            brightnessSlider.setMaxWidth(Region.USE_PREF_SIZE);
            brightnessSlider.setMinWidth(60);
            brightnessSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onBrightnessSlider();
            });
            brightnessLabel = new Label("Brightness:");
            controlGrid.add(brightnessLabel, 0, 0);
            controlGrid.add(brightnessSlider, 1, 0);

            contrastSlider = new Slider(-100, 100, 1);
            contrastSlider.setValue(0);
            contrastSlider.setPrefWidth(140);
            contrastSlider.setMaxWidth(Region.USE_PREF_SIZE);
            contrastSlider.setMinWidth(60);
            contrastSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onContrastSlider();
            });
            contrastLabel = new Label("Contrast:");
            controlGrid.add(contrastLabel, 0, 1);
            controlGrid.add(contrastSlider, 1, 1);

            hueSlider = new Slider(-100, 100, 1);
            hueSlider.setValue(0);
            hueSlider.setPrefWidth(140);
            hueSlider.setMaxWidth(Region.USE_PREF_SIZE);
            hueSlider.setMinWidth(60);
            hueSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onHueSlider();
            });
            hueLabel = new Label("Hue:");
            controlGrid.add(hueLabel, 0, 2);
            controlGrid.add(hueSlider, 1, 2);

            saturationSlider = new Slider(-100, 100, 1);
            saturationSlider.setValue(0);
            saturationSlider.setPrefWidth(140);
            saturationSlider.setMaxWidth(Region.USE_PREF_SIZE);
            saturationSlider.setMinWidth(60);
            saturationSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onSaturationSlider();
            });
            saturationLabel = new Label("Saturation:");
            controlGrid.add(saturationLabel, 0, 3);
            controlGrid.add(saturationSlider, 1, 3);

            sepiaToneSlider = new Slider(0, 100, 1);
            sepiaToneSlider.setValue(0);
            sepiaTone.setLevel(0.0);
            sepiaToneSlider.setPrefWidth(140);
            sepiaToneSlider.setMaxWidth(Region.USE_PREF_SIZE);
            sepiaToneSlider.setMinWidth(60);
            sepiaToneSlider.valueProperty().addListener(
                    (ObservableValue<? extends Number> ov, Number o, Number n) -> {
                onSepiaToneSlider();
            });
            sepiaToneLabel = new Label("Sepia tone:");
            controlGrid.add(sepiaToneLabel, 0, 4);
            controlGrid.add(sepiaToneSlider, 1, 4);

            colorAdjustTabContent.getChildren().add(controlGrid);

            effectsTab.setContent(colorAdjustTabContent);

            controlGrid.setDisable(true);
        }

        return effectsTab;
    }

    @Override
    public void onMediaPlayerChanged(MediaPlayer oldMediaPlayer) {
        removeListeners(oldMediaPlayer);
        addListeners();
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
                effectsTab.setDisable(false);
            } else if (status == MediaPlayer.Status.DISPOSED ||
                    status == MediaPlayer.Status.HALTED) {
                effectsTab.setDisable(true);
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

    private void onEnableButton() {
        if (enableButton.isSelected()) {
            enableButton.setText("Enabled");
            resetButton.setDisable(false);
            controlGrid.setDisable(false);
            sepiaTone.setInput(colorAdjust);
            FXMediaPlayer.getMediaView().setEffect(sepiaTone);
        } else {
            enableButton.setText("Enable");
            resetButton.setDisable(true);
            controlGrid.setDisable(true);
            sepiaTone.setInput(null);
            FXMediaPlayer.getMediaView().setEffect(null);
        }
    }

    private void onResetButton() {
        brightnessSlider.setValue(0.0);
        colorAdjust.setBrightness(0.0);

        contrastSlider.setValue(0.0);
        colorAdjust.setContrast(0.0);

        hueSlider.setValue(0.0);
        colorAdjust.setHue(0.0);

        saturationSlider.setValue(0.0);
        colorAdjust.setSaturation(0.0);

        sepiaToneSlider.setValue(0.0);
        sepiaTone.setLevel(0.0);
    }

    private void onBrightnessSlider() {
        if (brightnessSlider.isValueChanging()) {
            colorAdjust.setBrightness(brightnessSlider.getValue() / 100.0);
        }
    }

    private void onContrastSlider() {
        if (contrastSlider.isValueChanging()) {
            colorAdjust.setContrast(contrastSlider.getValue() / 100.0);
        }
    }

    private void onHueSlider() {
        if (hueSlider.isValueChanging()) {
            colorAdjust.setHue(hueSlider.getValue() / 100.0);
        }
    }

    private void onSaturationSlider() {
        if (saturationSlider.isValueChanging()) {
            colorAdjust.setSaturation(saturationSlider.getValue() / 100.0);
        }
    }

    private void onSepiaToneSlider() {
        if (sepiaToneSlider.isValueChanging()) {
            sepiaTone.setLevel(sepiaToneSlider.getValue() / 100.0);
        }
    }
}
