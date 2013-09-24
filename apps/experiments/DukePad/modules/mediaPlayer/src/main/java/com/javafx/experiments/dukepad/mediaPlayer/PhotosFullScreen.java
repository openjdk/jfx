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

import com.javafx.experiments.dukepad.core.CoreActivator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

/**
 *
 */
public class PhotosFullScreen extends Group {
    private Stage stage;
    private ImageView imageView1 = new ImageView();
    private ImageView imageView2 = new ImageView();
    private HBox toolBar = new HBox(10);
    private Button back;
    private Button forward;
    private List<File> photos;
    private int index;
    private Timeline slideShowTimeline;
    private Timeline transitionTimeline;
    private Timeline hideToolbarTimeline;


    public PhotosFullScreen() {
        System.out.println("PhotosFullScreen.PhotosFullScreen");
        stage = new Stage(StageStyle.UNDECORATED);
        Scene scene = new Scene(this);
        // Load Theme CSS
        scene.getStylesheets().add(CoreActivator.class.getResource("/DukePadTheme.css").toExternalForm());
        scene.setFill(Color.BLACK);
        stage.setScene(scene);

        back = new Button();
        back.setBackground(new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-left.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT
        )));
        back.setPrefSize(55, 54);
        back.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                gotoPic(index-1, false);
            }
        });

        final Background playBackground = new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-play.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        ));
        final Background pauseBackground = new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-pause.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        ));
        final ToggleButton play = new ToggleButton();
        play.setPrefSize(55, 54);
        play.backgroundProperty().bind(new ObjectBinding<Background>() {
            { bind(play.selectedProperty()); }
            @Override protected Background computeValue() {
                return play.isSelected() ? pauseBackground : playBackground;
            }
        });
        play.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                if (slideShowTimeline != null) {
                    slideShowTimeline.stop();
                    slideShowTimeline = null;
                }
                if (play.isSelected()) {
                    slideShowTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(2), new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent event) {
                                    int nextIndex = index + 1;
                                    if (nextIndex >= photos.size()) nextIndex = 0;
                                    gotoPic(nextIndex,true);
                                }
                            })
                    );
                    slideShowTimeline.setCycleCount(Timeline.INDEFINITE);
                    slideShowTimeline.play();
                }
            }
        });

        Button close = new Button();
        close.setBackground(new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-close.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT
        )));
        close.setPrefSize(55, 54);
        close.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                if (slideShowTimeline != null) {
                    slideShowTimeline.stop();
                    slideShowTimeline = null;
                }
                if (transitionTimeline != null) {
                    transitionTimeline.stop();
                    transitionTimeline = null;
                }
                stage.close();
            }
        });

        forward = new Button();
        forward.setBackground(new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-right.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT
        )));
        forward.setPrefSize(55, 54);
        forward.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                gotoPic(index+1, false);
            }
        });

        toolBar.getChildren().addAll(back, play, close, forward);
        toolBar.setCache(true);
        getChildren().addAll(imageView1,imageView2,toolBar);

        hideToolbarTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), new KeyValue(toolBar.opacityProperty(),1)),
            new KeyFrame(Duration.seconds(5.5), new KeyValue(toolBar.opacityProperty(),0))
        );
        hideToolbarTimeline.playFromStart();
        addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                hideToolbarTimeline.stop();
                toolBar.setOpacity(1);
                hideToolbarTimeline.playFromStart();
            }
        });
    }

    @Override protected void layoutChildren() {
        final int w = (int)stage.getWidth();
        final int h = (int)stage.getHeight();
        final int tbw = (int)(toolBar.prefWidth(-1)+.5);
        final int tbh = (int)(toolBar.prefHeight(-1)+.5);
        imageView1.setFitHeight(w);
        imageView1.setFitHeight(h);
        imageView1.setPreserveRatio(true);
        imageView2.setFitHeight(w);
        imageView2.setFitHeight(h);
        imageView2.setPreserveRatio(true);
        toolBar.resizeRelocate(
                (int)((w-tbw)/2),
                h - 20 - tbh,
                tbw, tbh
        );
    }

    public void show(List<File> photos, int index) {
        this.photos = photos;
        this.index = index;
        gotoPic(index,false);
        Rectangle2D mainScreen = Screen.getPrimary().getBounds();
        stage.setX(mainScreen.getMinX());
        stage.setY(mainScreen.getMinY());
        stage.setWidth(mainScreen.getWidth());
        stage.setHeight(mainScreen.getHeight());
        stage.show();
    }

    public void gotoPic(int index, boolean animate) {
        this.index = index;
        System.out.println("PhotosFullScreen.gotoPic");
        System.out.println("    index = " + index);
        System.out.println("    photos.size() = " + photos.size());
        System.out.println("    (index <= 0) = " + (index <= 0));
        System.out.println("    (index > photos.size()-2) = " + (index > photos.size() - 2));
        back.setDisable(index <= 0);
        forward.setDisable(index > photos.size()-2);
        File photo = photos.get(index);
        if (transitionTimeline != null) {
            transitionTimeline.stop();
            transitionTimeline = null;
        }
        if (!animate) {
            setImage1(photo);
            setImage2(null);
        } else {
            imageView2.setOpacity(1);
            imageView2.setImage(imageView1.getImage());
            setImage1(photo);
            transitionTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,new KeyValue(imageView2.opacityProperty(),1)),
                    new KeyFrame(Duration.seconds(.5),new KeyValue(imageView2.opacityProperty(),0))
            );
            transitionTimeline.play();
        }
    }

    private void setImage1(File imageFile) {
        if (imageFile == null) {
            imageView1.setImage(null);
        } else {
            final Image image = new Image(imageFile.toURI().toString(),false);
//            image.progressProperty().addListener(new ChangeListener<Number>() {
//                @Override public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
//                    System.out.println("progress = " + number2);
//                }
//            });
            // TODO center image
            imageView1.setImage(image);
        }
    }

    private void setImage2(File imageFile) {
        if (imageFile == null) {
            imageView2.setImage(null);
        } else {
            Image image = new Image(imageFile.toURI().toString(),false);
            // TODO center image
            imageView2.setImage(image);
        }
    }
}
