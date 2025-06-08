/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import java.io.File;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.sheets.NodePropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * MediaView Test Page.
 */
public class MediaPlayerPage extends TestPaneBase {
    private final MediaView mediaView;
    private final Label currentTime = new Label();
    private final Label status = new Label();
    private String sourceURI;
    private Media media;
    private MediaPlayer player;
    private final SimpleBooleanProperty autoPlay = new SimpleBooleanProperty();
    private final SimpleBooleanProperty mute = new SimpleBooleanProperty();
    private final SimpleDoubleProperty volume = new SimpleDoubleProperty(1.0);
    // TODO
    //  setAudioSpectrumInterval(double)
    //  setAudioSpectrumListener(AudioSpectrumListener)
    //  setAudioSpectrumNumBands(int)
    //  setAudioSpectrumThreshold(int)
    //  setBalance(double)
    //  setCycleCount(int)
    //  setRate(double)
    //  setStartTime(Duration)
    //  setStopTime(Duration)
    //  setVolume(double)

    public MediaPlayerPage() {
        super("MediaPlayerPage");

        sceneProperty().addListener((src, p, scene) -> {
            if (scene == null) {
                if (player != null) {
                    player.stop();
                    player.dispose();
                    player = null;
                }
            }
        });

        Button playButton = new Button("Play");
        playButton.setOnAction((ev) -> play());

        Button stopButton = new Button("Stop");
        stopButton.setOnAction((ev) -> stop());

        mediaView = new MediaView();
        mediaView.setOnError((ev) -> {
            System.out.println(ev);
        });

        OptionPane op = new OptionPane();
        // media
        op.section("Media");
        op.option("Source URI:", createSourceOption("source"));
        op.option(new HBox(5, playButton, stopButton));
        // player
        op.section("MediaPlayer");
        op.option("Current Time:", currentTime);
        op.option("Status:", status);
//            setAudioSpectrumInterval(double)
//            setAudioSpectrumListener(AudioSpectrumListener)
//            setAudioSpectrumNumBands(int)
//            setAudioSpectrumThreshold(int)
        op.option(new BooleanOption("autoPlay", "auto play", autoPlay));
//            setBalance(double)
//            setCycleCount(int)
        op.option(new BooleanOption("mute", "mute", mute));
//            setRate(double)
//            setStartTime(Duration)
//            setStopTime(Duration)
        op.option("Volume:", DoubleOption.of("volume", volume, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
        // view
        op.section("MediaView");
        op.option("Fit Height:", DoubleOption.of("fitHeight", mediaView.fitHeightProperty(), -1.0, 10.0, 100.0, 500.0));
        op.option("Fit Width:", DoubleOption.of("fitWidth", mediaView.fitWidthProperty(), -1.0, 10.0, 100.0, 500.0));
        op.option(new BooleanOption("preserveRatio", "preserve ratio", mediaView.preserveRatioProperty()));
        op.option(new BooleanOption("smooth", "smooth", mediaView.smoothProperty()));
//            setViewport(Rectangle2D)
        op.option("X:", DoubleOption.of("x", mediaView.xProperty(), -10.0, 0.0, 10));
        op.option("Y:", DoubleOption.of("y", mediaView.yProperty(), -10.0, 0.0, 10));
        NodePropertySheet.appendTo(op, mediaView);

        setOptions(op);
        setContent(new ScrollPane(mediaView));
    }

    private Node createSourceOption(String name) {
        TextField uri = new TextField();
        uri.setPromptText("URI");
        Button button = new Button("Browse...");
        button.setOnAction((ev) -> {
            FileChooser fc = new FileChooser();
            if (sourceURI != null) {
                File f = parseFileURI(sourceURI);
                if (f != null) {
                    fc.setInitialDirectory(f.getParentFile());
                    fc.setInitialFileName(f.getName());
                }
            }
            File file = fc.showOpenDialog(FX.getParentWindow(this));
            if (file != null) {
                String s = file.toURI().toString();
                uri.setText(s);
                sourceURI = s;
            }
        });
        HBox hb = new HBox(5, uri, button);
        HBox.setHgrow(uri, Priority.ALWAYS);
        return hb;
    }

    private static File parseFileURI(String text) {
        // FIX
        return null;
    }

    private MediaPlayer player() {
        if (player == null) {
            String uri = sourceURI;
            if (Utils.isBlank(uri)) {
                player = null;
            } else {
                try {
                    Media m = new Media(uri);

                    player = new MediaPlayer(m);
                    player.autoPlayProperty().bind(autoPlay);
                    player.muteProperty().bind(mute);
                    player.volumeProperty().bind(volume);
                    // TODO
//                  setAudioSpectrumInterval(double)
//                  setAudioSpectrumListener(AudioSpectrumListener)
//                  setAudioSpectrumNumBands(int)
//                  setAudioSpectrumThreshold(int)
//                  setBalance(double)
//                  setCycleCount(int)
//                  setRate(double)
//                  setStartTime(Duration)
//                  setStopTime(Duration)
                    currentTime.textProperty().bind(Bindings.createStringBinding(() -> {
                        Duration t = player.getCurrentTime();
                        return String.valueOf(t);
                    }, player.currentTimeProperty()));
                    status.textProperty().bind(Bindings.createStringBinding(() -> {
                        MediaPlayer.Status s = player.getStatus();
                        return String.valueOf(s);
                    }, player.statusProperty()));
                    player.setOnError(() -> {
                        p("ON ERROR");
                        player.getError().printStackTrace();
                    });
                    mediaView.setMediaPlayer(player);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return player;
    }

    private void play() {
        MediaPlayer p = player();
        if(p != null) {
            p.play();
        }
    }

    private void stop() {
        if (player != null) {
            player.stop();
        }
    }

    private static void p(Object x) {
        System.out.println(x);
    }
}
