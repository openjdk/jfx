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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MediaFolder extends TitledPane {
    private enum MediaType {PHOTO,MUSIC,MOVIE};
    private static final Image THUMB_RECT = new Image(MediaFolder.class.getResource("/images/thumb-rect.png").toExternalForm());
    private final File folder;
    private final ScrollPane content = new ScrollPane();
    private Pane container;
    private PhotosFullScreen photosFullScreen;
    private final List<File> photos = new ArrayList<>();

    public MediaFolder(File folder) {
        this.folder = folder;
        setText(folder.getName());
        setContent(content);
        new Thread(() -> {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Platform.runLater(this::rebuild);
        }).start();
    }

    private void rebuild() {
        photos.clear();
        container = null;
        // look at first file and work out if photo, music or movie folder
        boolean first = true;
        MediaType folderMediaType = null;
        for (final File file: folder.listFiles()) {
            final String name = file.getName();
            // ignore thumbnails
            if (name.endsWith("-thumb.jpg")) continue;
            // detect media type
            final int dotIndex = name.lastIndexOf('.');
            MediaType mediaType = null;
            if (dotIndex >0) {
                final String ext = name.substring(dotIndex+1).toLowerCase();
                System.out.println("ext = " + ext);
                switch (ext) {
                    case "mp4":
                        mediaType = MediaType.MOVIE;
                        break;
                    case "mp3":
                        mediaType = MediaType.MUSIC;
                        break;
                    case "jpg":
                        mediaType = MediaType.PHOTO;
                        break;
                }
            }
            // skip unknown files
            if (mediaType == null) continue;
            // handle first media file
            if (first) {
                folderMediaType = mediaType;
                first = false;
                switch(folderMediaType) {
                    case PHOTO:
                    case MOVIE:
                        container = new TilePane(20,20);
                        break;
                    case MUSIC:
                        container = new MusicPane();
                        expandedProperty().addListener((observableValue, oldExpanded, expanded) -> {
                            if (!expanded) MusicPlayer.quit();
                        });
                        break;
                }
                content.setContent(container);
            }
            final String readableName = name.substring(0,dotIndex);
            switch(folderMediaType) {
                case PHOTO:
                    final int index = photos.size();
                    photos.add(file);
                    File thumbnail = getThumbnailFile(file);
                    if (!thumbnail.exists()) {
                        makeThumbnail(file);
                    }
                    final Button thumbBtn = new Button();
                    thumbBtn.setPrefSize(105,105);
                    thumbBtn.getStyleClass().clear();
                    thumbBtn.setBackground(new Background(
                            new BackgroundImage(THUMB_RECT, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT),
                            new BackgroundImage(new Image(thumbnail.toURI().toString(), true),
                                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                    new BackgroundPosition(Side.LEFT, 5, false, Side.TOP, 5, false), BackgroundSize.DEFAULT)
                    ));
                    thumbBtn.setOnAction(event -> {
                        if (photosFullScreen == null) photosFullScreen = new PhotosFullScreen();
                        photosFullScreen.show(photos,index);
                    });
                    Platform.runLater(() -> container.getChildren().add(thumbBtn));
                    break;
                case MUSIC:

                    Platform.runLater(() -> ((MusicPane)container).addFile(file,readableName));
                    break;
                case MOVIE:
                    thumbnail = getThumbnailFile(file);
                    if (!thumbnail.exists()) {
                        System.err.println("Missing thumbnail for movie file: "+file);
                    }
                    final Button movieButton = new Button();
                    movieButton.setPrefSize(105, 105);
                    movieButton.getStyleClass().clear();
                    if (thumbnail.exists()) {
                        movieButton.setBackground(new Background(
                                new BackgroundImage(THUMB_RECT, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT),
                                new BackgroundImage(new Image(thumbnail.toURI().toString(), true),
                                        BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                        new BackgroundPosition(Side.LEFT, 5, false, Side.TOP, 5, false), BackgroundSize.DEFAULT)
                        ));
                    } else {
                        movieButton.setBackground(new Background(
                                new BackgroundImage(THUMB_RECT, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
                    }
                    movieButton.setOnAction(new EventHandler<ActionEvent>() {
                        @Override public void handle(ActionEvent event) {
                            MovieFullScreen movieFullScreen = new MovieFullScreen((Stage) getScene().getWindow());
                            movieFullScreen.show(file);
                        }
                    });
                    Platform.runLater(() -> container.getChildren().add(movieButton));
                    break;
            }
        }
    }

    private File getThumbnailFile(File mediaFile) {
        String name = mediaFile.getName();
        name = name.substring(0,name.lastIndexOf('.')) + "-thumb.jpg";
        return new File(mediaFile.getParent(),name);
    }

    private void makeThumbnail(File image) {
        try {
            BufferedImage img = ImageIO.read(image);
            int w = img.getWidth();
            int h = img.getHeight();
            int x = 0, y = 0, s = 0;
            if (w > h) {
                x = (w-h)/2;
                s = h;
            } else {
                y = (h-w)/2;
                s = w;
            }
            BufferedImage outputImg = new BufferedImage(95,95,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outputImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img,0,0,95,95,x,y,s,s,null);
            g.dispose();
            ImageIO.write(outputImg,"jpg",getThumbnailFile(image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
