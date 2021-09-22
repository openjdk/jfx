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

package fxmediaplayer.info;

import fxmediaplayer.FXMediaPlayerControlInterface;
import fxmediaplayer.FXMediaPlayerInterface;
import fxmediaplayer.FXMediaPlayerUtils;
import java.util.Map;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioTrack;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.Track;
import javafx.scene.media.VideoTrack;
import javafx.util.Duration;

public class MediaPlayerInfo implements FXMediaPlayerControlInterface {

    private static final double WIDTH = 210;
    private FXMediaPlayerInterface FXMediaPlayer = null;
    private VBox info = null;
    private ListView<String> mediaInfo = null;
    private final ObservableList<String> mediaInfoItems =
            FXCollections.observableArrayList();
    private Duration currentDuration = null;
    private Duration currentCycleDuration = null;
    private Duration currentTotalDuration = null;
    private int durationIndex = -1;
    private int cycleDurationIndex = -1;
    private int totalDurationIndex = -1;
    private int widthIndex = -1;
    private int heightIndex = -1;
    private InvalidationListener durationPropertyListener = null;
    private InvalidationListener cycleDurationPropertyListener = null;
    private InvalidationListener totalDurationPropertyListener = null;
    private InvalidationListener widthPropertyListener = null;
    private InvalidationListener heightPropertyListener = null;
    private ListView<String> metadataTrackInfo = null;
    private final ObservableList<String> metadataTrackInfoItems =
            FXCollections.observableArrayList();
    private MapChangeListener<String, Object> metadataListener = null;
    private ListChangeListener<Track> trackListener = null;

    public MediaPlayerInfo(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public VBox getInfo() {
        if (info == null) {
            info = new VBox();

            info.setMinWidth(WIDTH);
            info.setMaxWidth(WIDTH);
            info.setPrefWidth(WIDTH);
            info.setAlignment(Pos.TOP_CENTER);

            Label label = new Label("Media info:");
            label.setAlignment(Pos.BASELINE_LEFT);
            info.getChildren().add(label);
            mediaInfo = new ListView<>();
            mediaInfo.setItems(mediaInfoItems);
            mediaInfo.setPrefHeight(125);
            mediaInfo.setMinWidth(WIDTH - 10);
            mediaInfo.setMaxWidth(WIDTH - 10);
            mediaInfo.setPrefWidth(WIDTH - 10);
            info.getChildren().add(mediaInfo);

            label = new Label("Metadata/Track info:");
            label.setAlignment(Pos.BASELINE_LEFT);
            info.getChildren().add(label);
            metadataTrackInfo = new ListView<>();
            metadataTrackInfo.setItems(metadataTrackInfoItems);
            metadataTrackInfo.setPrefHeight(295);
            metadataTrackInfo.setMinWidth(WIDTH - 10);
            metadataTrackInfo.setMaxWidth(WIDTH - 10);
            metadataTrackInfo.setPrefWidth(WIDTH - 10);
            info.getChildren().add(metadataTrackInfo);

            createListeners();
            addListeners();
        }

        return info;
    }

    @Override
    public void onMediaPlayerChanged(MediaPlayer oldMediaPlayer) {
        if (oldMediaPlayer != null) {
            removeListeners(oldMediaPlayer);
        }

        currentDuration = null;
        currentCycleDuration = null;
        currentTotalDuration = null;
        durationIndex = -1;
        widthIndex = -1;
        heightIndex = -1;
        totalDurationIndex = -1;
        cycleDurationIndex = -1;
        mediaInfoItems.clear();

        metadataTrackInfoItems.clear();

        addListeners();
    }

    private void createListeners() {
        durationPropertyListener = (Observable o) -> {
            ReadOnlyObjectProperty property = (ReadOnlyObjectProperty) o;
            Duration duration = ((Duration) property.getValue());
            if (currentDuration == null || !currentDuration.equals(duration)) {
                if (durationIndex == -1) {
                    if (mediaInfoItems.add("Duration: " +
                            FXMediaPlayerUtils.secondsToString(duration.toSeconds()))) {
                        durationIndex = mediaInfoItems.size() - 1;
                    }
                } else {
                    mediaInfoItems.set(durationIndex, "Duration: " +
                            FXMediaPlayerUtils.secondsToString(duration.toSeconds()));
                }
                currentDuration = duration;
            }
        };

        cycleDurationPropertyListener = (Observable o) -> {
            ReadOnlyObjectProperty property = (ReadOnlyObjectProperty) o;
            Duration duration = ((Duration) property.getValue());
            if (currentCycleDuration == null || !currentCycleDuration.equals(duration)) {
                if (cycleDurationIndex == -1) {
                    if (mediaInfoItems.add("Cycle duration: " +
                            FXMediaPlayerUtils.secondsToString(duration.toSeconds()))) {
                        cycleDurationIndex = mediaInfoItems.size() - 1;
                    }
                } else {
                    mediaInfoItems.set(cycleDurationIndex, "Cycle duration: " +
                            FXMediaPlayerUtils.secondsToString(duration.toSeconds()));
                }
                currentCycleDuration = duration;
            }
        };

        totalDurationPropertyListener = (Observable o) -> {
            ReadOnlyObjectProperty property = (ReadOnlyObjectProperty) o;
            Duration duration = ((Duration) property.getValue());
            if (currentTotalDuration == null || !currentTotalDuration.equals(duration)) {
                if (totalDurationIndex == -1) {
                    if (mediaInfoItems.add("Total duration: " +
                            FXMediaPlayerUtils.secondsToString(duration.toSeconds()))) {
                        totalDurationIndex = mediaInfoItems.size() - 1;
                    }
                } else {
                    mediaInfoItems.set(totalDurationIndex, "Total duration: " +
                            FXMediaPlayerUtils.secondsToString(duration.toSeconds()));
                }
                currentTotalDuration = duration;
            }
        };

        widthPropertyListener = (Observable o) -> {
            ReadOnlyIntegerProperty property = (ReadOnlyIntegerProperty) o;
            int width = property.getValue();
            if (widthIndex == -1) {
                if (mediaInfoItems.add("Width: " + width)) {
                    widthIndex = mediaInfoItems.size() - 1;
                }
            } else {
                mediaInfoItems.set(widthIndex, "Width: " + width);
            }
        };

        heightPropertyListener = (Observable o) -> {
            ReadOnlyIntegerProperty property = (ReadOnlyIntegerProperty) o;
            int height = property.getValue();
            if (heightIndex == -1) {
                if (mediaInfoItems.add("Height: " + height)) {
                    heightIndex = mediaInfoItems.size() - 1;
                }
            } else {
                mediaInfoItems.set(heightIndex, "Height: " + height);
            }
        };

        metadataListener = (Change<? extends String, ? extends Object> change) -> {
            synchronized (metadataTrackInfoItems) {
                String key = change.getKey();
                Object value = change.getValueAdded();

                if (value instanceof java.lang.String) {
                    metadataTrackInfoItems.add(key + ": " + (String) value);
                } else if (value instanceof java.lang.Integer) {
                    metadataTrackInfoItems.add(key + ": " + (Integer) value);
                } else if (value instanceof javafx.util.Duration) {
                    metadataTrackInfoItems.add(key + ": " + ((Duration) value).toSeconds());
                } else if (value instanceof java.lang.Double) {
                    metadataTrackInfoItems.add(key + ": " + (Double) value);
                } else if (value instanceof javafx.scene.image.Image) {
                    Image image = (Image) change.getValueAdded();
                    FXMediaPlayer.onImageAvailable(image);
                }
            }
        };

        trackListener = (ListChangeListener.Change<? extends Track> change) -> {
            synchronized (metadataTrackInfoItems) {
                while (change.next()) {
                    change.getAddedSubList().forEach(track -> {
                        if (track instanceof AudioTrack) {
                            AudioTrack audioTrack = (AudioTrack) track;
                            metadataTrackInfoItems.add("AudioTrack name: " +
                                    audioTrack.getName());
                            metadataTrackInfoItems.add("AudioTrack ID: " +
                                    audioTrack.getTrackID());
                            metadataTrackInfoItems.add("AudioTrack locale: " +
                                    audioTrack.getLocale());
                            Map<String, Object> metadata = track.getMetadata();
                            metadata.keySet().forEach(key -> {
                                metadataTrackInfoItems.add("AudioTrack: " +
                                        key + " - " + metadata.get(key).toString());
                            });
                        } else if (track instanceof VideoTrack) {
                            VideoTrack videoTrack = (VideoTrack) track;
                            metadataTrackInfoItems.add("VideoTrack name: " +
                                    videoTrack.getName());
                            metadataTrackInfoItems.add("VideoTrack ID: " +
                                    videoTrack.getTrackID());
                            metadataTrackInfoItems.add("VideoTrack locale: " +
                                    videoTrack.getLocale());
                            metadataTrackInfoItems.add("VideoTrack width: " +
                                    videoTrack.getWidth());
                            metadataTrackInfoItems.add("VideoTrack height: " +
                                    videoTrack.getHeight());
                            Map<String, Object> metadata = track.getMetadata();
                            metadata.keySet().forEach(key -> {
                                metadataTrackInfoItems.add("VideoTrack: " +
                                        key + " - " + metadata.get(key).toString());
                            });
                        }
                    });
                }
            }
        };
    }

    private void addListeners() {
        if (FXMediaPlayer.getMediaPlayer() != null) {
            FXMediaPlayer.getMediaPlayer().getMedia().durationProperty()
                    .addListener(durationPropertyListener);
            FXMediaPlayer.getMediaPlayer().cycleDurationProperty()
                    .addListener(cycleDurationPropertyListener);
            FXMediaPlayer.getMediaPlayer().totalDurationProperty()
                    .addListener(totalDurationPropertyListener);
            FXMediaPlayer.getMediaPlayer().getMedia().widthProperty()
                    .addListener(widthPropertyListener);
            FXMediaPlayer.getMediaPlayer().getMedia().heightProperty()
                    .addListener(heightPropertyListener);
            FXMediaPlayer.getMediaPlayer().getMedia().getMetadata()
                    .addListener(metadataListener);
            FXMediaPlayer.getMediaPlayer().getMedia().getTracks()
                    .addListener(trackListener);
        }
    }

    private void removeListeners(MediaPlayer mediaPlayer) {
        mediaPlayer.getMedia().durationProperty()
                .removeListener(durationPropertyListener);
        mediaPlayer.cycleDurationProperty()
                .removeListener(cycleDurationPropertyListener);
        mediaPlayer.totalDurationProperty()
                .removeListener(totalDurationPropertyListener);
        mediaPlayer.getMedia().widthProperty()
                .removeListener(widthPropertyListener);
        mediaPlayer.getMedia().heightProperty()
                .removeListener(heightPropertyListener);
        mediaPlayer.getMedia().getMetadata()
                .removeListener(metadataListener);
        mediaPlayer.getMedia().getTracks()
                .removeListener(trackListener);
    }
}
