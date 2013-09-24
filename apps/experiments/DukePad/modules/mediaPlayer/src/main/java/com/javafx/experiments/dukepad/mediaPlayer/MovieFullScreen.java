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
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Fullscreen media player with controls toolbar
 */
public class MovieFullScreen extends Group {
    private Stage stage;
    private Stage primaryStage;
    private HBox toolBar = new HBox(10);
    private ToggleButton play;
    private Timeline hideToolbarTimeline;
    private File movie;

    public MovieFullScreen(Stage primaryStage) {
        System.out.println("PhotosFullScreen.PhotosFullScreen");
        this.primaryStage = primaryStage;
        stage = new Stage(StageStyle.TRANSPARENT);
        Scene scene = new Scene(this);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        final Background playBackground = new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-play.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        ));
        final Background pauseBackground = new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-pause.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        ));
        play = new ToggleButton();
        play.setPrefSize(55, 54);
        play.backgroundProperty().bind(new ObjectBinding<Background>() {
            { bind(play.selectedProperty()); }
            @Override protected Background computeValue() {
                return play.isSelected() ? pauseBackground : playBackground;
            }
        });
        play.setSelected(true);
        play.setOnAction(event -> playPause());

        Button volDown = new Button();
        volDown.setBackground(new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-vol-down.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        )));
        volDown.setPrefSize(55,54);
        volDown.setOnAction(event -> volDown());

        Button volUp = new Button();
        volUp.setBackground(new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-vol-up.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        )));
        volUp.setPrefSize(55,54);
        volUp.setOnAction(event -> volUp());

        Button close = new Button();
        close.setBackground(new Background(new BackgroundImage(
                new Image(MovieFullScreen.class.getResource("/images/fs-close.png").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,BackgroundSize.DEFAULT
        )));
        close.setPrefSize(55,54);
        close.setOnAction(event -> close());

        toolBar.getChildren().addAll(play, volDown, volUp, close);
        toolBar.setCache(true);
        getChildren().addAll(toolBar);

        hideToolbarTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), new KeyValue(toolBar.opacityProperty(),1)),
            new KeyFrame(Duration.seconds(5.5), new KeyValue(toolBar.opacityProperty(),0))
        );
        hideToolbarTimeline.playFromStart();
        scene.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                hideToolbarTimeline.stop();
                toolBar.setOpacity(1);
                hideToolbarTimeline.playFromStart();
            }
        });
    }

    private Image loadIcon(String name) {
        return new Image(MovieFullScreen.class.getResource("/images/"+name).toExternalForm());
    }

    @Override protected void layoutChildren() {
        final int w = (int)stage.getWidth();
        final int h = (int)stage.getHeight();
        final int tbw = (int)(toolBar.prefWidth(-1)+.5);
        final int tbh = (int)(toolBar.prefHeight(-1)+.5);
        toolBar.resizeRelocate(
                (int)((w-tbw)/2),
                h - 20 - tbh,
                tbw, tbh
        );
    }

    public void show(File movie) {
        this.movie = movie;
        System.out.println("MovieFullScreen.show");
        System.out.println("    movie = [" + movie + "]");
        System.out.println("    primaryStage = " + primaryStage);

//        primaryStage.getScene().getRoot().setOpacity(0.05);
        primaryStage.hide();
        Rectangle2D mainScreen = Screen.getPrimary().getBounds();
        stage.setX(mainScreen.getMinX());
        stage.setY(mainScreen.getMinY());
        stage.setWidth(mainScreen.getWidth());
        stage.setHeight(mainScreen.getHeight());
        stage.show();
        playPause();
    }

    private Process playerProcess;
    private PrintWriter playerOut;
//    private OutputStream playerOut;
    private InputStream playerIn;
    private InputStream playerErr;

    private void playPause() {
        System.out.println("MovieFullScreen.playPause");
        System.out.println("        playerProcess = " + playerProcess);
        System.out.println("        playerProcess.isAlive() = " + ((playerProcess==null)?"null":playerProcess.isAlive()));
        try {
            if (playerProcess == null || !playerProcess.isAlive()) {
                ProcessBuilder pb = new ProcessBuilder("omxplayer",movie.getAbsolutePath().toString());
                System.out.println("    pb = " + pb);
                System.out.println("    pb.command() = " + Arrays.toString(pb.command().toArray()));
                System.out.println("    movie.getAbsolutePath().toString() = " + movie.getAbsolutePath().toString());
                playerProcess = pb
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .start();
//                playerOut = playerProcess.getOutputStream();
                playerOut = new PrintWriter(playerProcess.getOutputStream());
//                playerIn = playerProcess.getInputStream();
//                playerErr = playerProcess.getErrorStream();
//                new Thread(new Runnable() {
//                    @Override public void run() {
//                        System.out.println("        in playerProcess.isAlive() = " + playerProcess.isAlive());
//                        while (playerProcess != null && playerProcess.isAlive()) {
//                            try {
//                                System.out.print('.');
//                                System.out.write(playerIn.read());
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        System.out.println("in thread finished!");
//                    }
//                }).start();
//                new Thread(new Runnable() {
//                    @Override public void run() {
//                        System.out.println("        in playerProcess.isAlive() = " + playerProcess.isAlive());
//                        while (playerProcess != null && playerProcess.isAlive()) {
//                            try {
//                                System.out.print(',');
//                                System.err.write(playerErr.read());
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        System.out.println("err thread finished!");
//                    }
//                }).start();
            } else if (play.isSelected()) {
                playerOut.print(' ');
                playerOut.flush();
//                playerProcess.getOutputStream().write(32);
                play.setSelected(false);
            } else {
                playerOut.print(' ');
                playerOut.flush();
//                playerProcess.getOutputStream().write(32);
                play.setSelected(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void close() {
        System.out.println("MovieFullScreen.close");
        System.out.println("        playerProcess = " + playerProcess);
        System.out.println("        playerProcess.isAlive() = " + playerProcess.isAlive());
        if (playerProcess != null || playerProcess.isAlive()) {
            playerOut.print('q');
            playerOut.flush();
            playerProcess.destroy();
            playerProcess = null;
        }
        stage.close();
        primaryStage.show();
    }

    private void volUp() {
        if (playerProcess != null || playerProcess.isAlive()) {
            playerOut.print('+');
            playerOut.flush();
        }
    }

    private void volDown() {
        if (playerProcess != null || playerProcess.isAlive()) {
            playerOut.print('-');
            playerOut.flush();
        }
    }
}
