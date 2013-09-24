/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.mediaPlayer;

import com.javafx.experiments.dukepad.core.Fonts;
import com.javafx.experiments.dukepad.core.Palette;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;

/**
 * Pane that lists all music files and allows you to play them
 */
public class MusicPane extends VBox {
    private static final Image PLAY_ICON = new Image(MediaFolder.class.getResource("/images/play.png").toExternalForm());
    private static final Image PAUSE_ICON = new Image(MediaFolder.class.getResource("/images/pause.png").toExternalForm());
    private static final Image VOL_UP_ICON = new Image(MediaFolder.class.getResource("/images/volume-up.png").toExternalForm());
    private static final Image VOL_DOWN_ICON = new Image(MediaFolder.class.getResource("/images/volume-down.png").toExternalForm());
    private Label currentTimeLabel = new Label("0:00");
    private Slider positionSlider = new Slider(0,1,0);
    private Label remainingTimeLabel = new Label("0:00");
    private ImageView volDownIcon = new ImageView(VOL_DOWN_ICON);
    private Slider volumeSlider = new Slider(0,1,1);
    private ImageView volUpIcon = new ImageView(VOL_UP_ICON);
    private HBox controlsBox = new HBox(5, currentTimeLabel,positionSlider, remainingTimeLabel,volDownIcon,volumeSlider,volUpIcon);

    public MusicPane() {
        super(5);
        getChildren().add(controlsBox);
        controlsBox.setManaged(false);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.visibleProperty().bind(new BooleanBinding() {
            { bind(MusicPlayer.currentFileProperty()); }
            @Override protected boolean computeValue() {
                return MusicPlayer.getCurrentFile() != null;
            }
        });
        positionSlider.setPrefWidth(300);
        positionSlider.valueProperty().bindBidirectional(MusicPlayer.playPositionProperty());
        volumeSlider.setPrefWidth(150);
        volumeSlider.valueProperty().bindBidirectional(MusicPlayer.volumeProperty());
        currentTimeLabel.setMinWidth(USE_PREF_SIZE);
        currentTimeLabel.setPrefWidth(50);
        currentTimeLabel.setFont(Fonts.dosisExtraLight(12));
        currentTimeLabel.setTextFill(Palette.MID_GREY);
        currentTimeLabel.textProperty().bind(new StringBinding() {
            { bind(MusicPlayer.currentTimeProperty()); }
            @Override protected String computeValue() {
                int timeInSeconds = MusicPlayer.getCurrentTime();
                int minutes = (int) (timeInSeconds / 60d);
                int seconds = timeInSeconds - (minutes * 60);
                return String.format("%02d:%02d", minutes, seconds);
            }
        });
        remainingTimeLabel.setMinWidth(USE_PREF_SIZE);
        remainingTimeLabel.setPrefWidth(50);
        remainingTimeLabel.setFont(Fonts.dosisExtraLight(12));
        remainingTimeLabel.setTextFill(Palette.MID_GREY);
        remainingTimeLabel.textProperty().bind(new StringBinding() {
            { bind(MusicPlayer.remainingTimeProperty()); }
            @Override protected String computeValue() {
                int timeInSeconds = MusicPlayer.getRemainingTime();
                int minutes = (int)(timeInSeconds/60d);
                int seconds = timeInSeconds - (minutes*60);
                return String.format("%02d:%02d",minutes,seconds);
            }
        });
    }

    public void addFile(final File musicFile, String name) {
        final Button songBtn = new Button(name);
        final ImageView icon = new ImageView();
        icon.imageProperty().bind(new ObjectBinding<Image>() {
            { bind(MusicPlayer.pausedProperty(), MusicPlayer.currentFileProperty()); }
            @Override protected Image computeValue() {
                return (MusicPlayer.getCurrentFile() ==  musicFile && !MusicPlayer.getPaused()) ? PAUSE_ICON : PLAY_ICON;
            }
        });
        songBtn.setGraphic(icon);
        songBtn.setPadding(new Insets(5, 0, 5, 40));
        songBtn.setGraphicTextGap(20);
        songBtn.getStyleClass().clear();
        songBtn.setOnAction(event -> {
            if (MusicPlayer.getCurrentFile() == musicFile) {
                MusicPlayer.pauseUnpause();
            } else {
                MusicPlayer.play(musicFile);
                controlsBox.autosize();
                controlsBox.setLayoutX(songBtn.getBoundsInParent().getMaxX() + 20);
                System.out.println(songBtn.getText() +" ---> "+songBtn.getBoundsInParent());
                controlsBox.setLayoutY(
                        songBtn.getBoundsInParent().getMinY() + (int)((songBtn.getBoundsInParent().getHeight() - controlsBox.getHeight())/2));
                System.out.println("    currentTimeLabel = " + currentTimeLabel.getWidth());
            }
        });
        getChildren().add(songBtn);
    }
}
