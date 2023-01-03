/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import fxmediaplayer.FXMediaPlayerInterface;
import fxmediaplayer.FXMediaPlayerUtils;
import fxmediaplayer.MediaPlayerDefaults;
import fxmediaplayer.media.FXMedia;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.*;
import javafx.scene.text.Font;

public class MediaPlayerPlayListTab {

    private static final int ITEM_WIDTH = 160;
    private static final int ITEM_HEIGHT = 120;
    private static final int ITEM_VIEW_WIDTH = 160;
    private static final int ITEM_VIEW_HEIGHT = 110;
    private FXMediaPlayerInterface FXMediaPlayer = null;
    private PlayListLoader playListLoader = null;
    private final List<String> sources = new ArrayList<>();
    private Tab playListTab = null;
    private ScrollPane scrollPane = null;
    private HBox playListContent = null;

    public MediaPlayerPlayListTab(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public Tab getPlayListTab() {
        if (playListTab == null) {
            playListTab = new Tab();
            playListTab.setText("Play List");
            playListTab.setOnSelectionChanged((Event me) -> {
                onTabSelectionChanged();
            });

            scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setOnDragOver((DragEvent event) -> {
                onDragOver(event);
            });
            scrollPane.setOnDragDropped((DragEvent event) -> {
                onDragDropped(event);
            });

            playListContent = new HBox(15);
            playListContent.setId("mediaPlayerTab");
            playListContent.setAlignment(Pos.CENTER);
            playListContent.setPadding(new Insets(5, 0, 0, 0));

            scrollPane.setContent(playListContent);
            playListTab.setContent(scrollPane);
        }

        return playListTab;
    }

    private synchronized void onTabSelectionChanged() {
        if (playListLoader == null && playListTab.isSelected()) {
            playListLoader = new PlayListLoader();
            playListLoader.start();
        }
    }

    private void onDragOver(DragEvent event) {
        String source = FXMediaPlayerUtils.getSourceFromDragboard(event.getDragboard());
        if (source != null && !sources.contains(source)) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        try {
            String source = FXMediaPlayerUtils.getSourceFromDragboard(event.getDragboard());
            addPlayListItem(source);
            event.setDropCompleted(true);
            event.consume();
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }
    }

    private void addPlayListItem(String source) throws IOException {
        if (!sources.contains(source)) {
            sources.add(source);
            savePlayListProperties();
        }

        PlayListItem item = new PlayListItem(source);
        playListContent.getChildren().add(item.getItem());
    }

    private void removePlayListItem(String source, PlayListItem item) throws IOException {
        sources.remove(source);
        savePlayListProperties();
        playListContent.getChildren().remove(item.getItem());
    }

    private void loadPlayList() throws InvalidPropertiesFormatException, IOException {
        List<String> embededSources = FXMedia.getEmbededMediaFiles();
        if (embededSources.size() > 0) {
            sources.addAll(embededSources);
        } else {
            loadPlayListProperties();
        }

        for (int i = 0; i < sources.size(); i++) {
            addPlayListItem(sources.get(i));
        }
    }

    private void loadPlayListProperties() throws InvalidPropertiesFormatException, IOException {
        Properties properties = new Properties();
        try {
            properties.loadFromXML(new FileInputStream(MediaPlayerDefaults.PLAYLIST_FILE));
            Object[] keys = properties.keySet().toArray();
            Arrays.sort(keys);
            for (Object key : keys) {
                sources.add(properties.getProperty((String) key));
            }
        } catch (FileNotFoundException ex) {
            sources.addAll(Arrays.asList(MediaPlayerDefaults.PLAYLIST));
            savePlayListProperties();
        }
    }

    private void savePlayListProperties() throws IOException {
        Properties properties = new Properties();
        for (int i = 0; i < sources.size(); i++) {
            properties.setProperty(Integer.toString(i), sources.get(i));
        }
        properties.storeToXML(new FileOutputStream(MediaPlayerDefaults.PLAYLIST_FILE), null);
    }

    private class PlayListLoader extends Thread {

        @Override
        public void run() {
            Platform.runLater(() -> {
                try {
                    loadPlayList();
                } catch (InvalidPropertiesFormatException ex) {
                    System.err.println(ex.toString());
                } catch (IOException ex) {
                    System.err.println(ex.toString());
                }
            });
        }
    }

    private class PlayListItem {

        private String source = null;
        private Media media = null;
        private MediaPlayer mediaPlayer = null;
        private VBox item = null;
        private VBox view = null;
        private Button buttonLoad = null;
        private ToggleButton buttonMute = null;
        private Button buttonRemove = null;
        private Node itemMediaView = null;
        private Image image = null;
        private boolean isVideoTrackPresent = false;

        public PlayListItem(String source) {
            this.source = source;
        }

        public VBox getItem() {
            if (item == null) {
                item = new VBox(5);

                view = new VBox();
                view.setMinSize(ITEM_WIDTH, ITEM_HEIGHT);
                view.setAlignment(Pos.CENTER);
                view.setStyle("-fx-background-color: gray;");
                view.setOnMouseEntered((MouseEvent me) -> {
                    onMouseEntered();
                });
                view.setOnMouseExited((MouseEvent me) -> {
                    onMouseExited();
                });
                view.setOnMouseClicked((MouseEvent me) -> {
                    onMouseClicked(me);
                });

                media = new Media(source);
                media.getMetadata().addListener(
                        (MapChangeListener.Change<? extends String,
                                ? extends Object> change) -> {
                    Object value = change.getValueAdded();

                    if (value instanceof javafx.scene.image.Image) {
                        image = (Image) change.getValueAdded();
                        updateView();
                    }
                });
                media.getTracks().addListener((Change<? extends Track> change) -> {
                    for (Track track : change.getList()) {
                        if (track instanceof VideoTrack) {
                            isVideoTrackPresent = true;
                            updateView();
                        }
                    }
                });

                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);

                TextField textField = new TextField("Audio");
                textField.setFont(Font.font ("Verdana", 20));
                textField.setPrefWidth(ITEM_VIEW_WIDTH);
                textField.setPrefHeight(ITEM_VIEW_HEIGHT);
                textField.setAlignment(Pos.CENTER);
                textField.setDisable(true);
                textField.setStyle("-fx-background-color: transparent; -fx-opacity: 1.0;");
                itemMediaView = textField;
                view.getChildren().add(itemMediaView);

                item.getChildren().add(view);

                HBox hBox = new HBox(5);
                hBox.setAlignment(Pos.CENTER);
                buttonLoad = new Button("Load");
                buttonLoad.setOnAction((ActionEvent event) -> {
                    onButtonLoad();
                });
                buttonMute = new ToggleButton("Mute");
                buttonMute.setSelected(true);
                buttonMute.setOnAction((ActionEvent event) -> {
                    onButtonMute();
                });
                buttonRemove = new Button("Del.");
                buttonRemove.setOnAction((ActionEvent event) -> {
                    onButtonDelete();
                });
                hBox.getChildren().addAll(buttonLoad, buttonMute, buttonRemove);
                item.getChildren().add(hBox);
            }

            return item;
        }

        private void onMouseEntered() {
            mediaPlayer.play();
        }

        private void onMouseExited() {
            mediaPlayer.pause();
        }

        private void onMouseClicked(MouseEvent me) {
            if (me.getButton() == MouseButton.PRIMARY &&
                    me.getClickCount() >= 2) {
                onButtonLoad();
                FXMediaPlayer.getMediaPlayer().play();
            }
        }

        private void onButtonLoad() {
            FXMediaPlayer.onSourceChanged(source);
        }

        private void onButtonMute() {
            mediaPlayer.setMute(buttonMute.isSelected());
        }

        private void onButtonDelete() {
            try {
                removePlayListItem(source, this);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        }

        private void updateView() {
            if (isVideoTrackPresent) {
                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(ITEM_WIDTH);
                mediaView.setFitHeight(ITEM_HEIGHT);
                view.getChildren().remove(itemMediaView);
                itemMediaView = mediaView;
                view.getChildren().add(itemMediaView);
                view.setStyle("-fx-background-color: black;");
            } else {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(ITEM_WIDTH);
                imageView.setFitHeight(ITEM_HEIGHT);
                imageView.setPreserveRatio(true);
                view.getChildren().remove(itemMediaView);
                itemMediaView = imageView;
                view.getChildren().add(itemMediaView);
                view.setStyle("-fx-background-color: gray;");
            }

        }
    }
}
