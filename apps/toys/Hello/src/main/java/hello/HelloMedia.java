/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import java.util.List;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaErrorEvent;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.Track;
import javafx.stage.Stage;

public class HelloMedia extends Application {
    private static final String DEFAULT_SOURCE = 
        "http://download.oracle.com/otndocs/products/javafx/oow2010-2.flv";
    private static String argSource = null;

    @Override
    public void start(Stage stage) {
        String source = argSource == null ? DEFAULT_SOURCE : argSource;
        stage.setTitle("Hello Media [" + source + "]");
        final Scene scene = new Scene(new Group(), 1280, 720);
        stage.setScene(scene);
        stage.show();

        try {
            final Media media = new Media(source);

            if (media.getError() == null) {
                media.setOnError(new Runnable() {
                    public void run() {
                        System.err.println(">>> Media ERR: " + media.getError());
                        exitOnError();
                    }
                });
                media.getMetadata().addListener(new MapChangeListener<String, Object>() {
                    public void onChanged(Change<? extends String, ? extends Object> change) {
                        String key = change.getKey();

                        // Print metadata tag and value to console.
                        System.out.println(change.getKey() + " (" + change.getValueAdded().getClass().getName() + "): " + change.getValueAdded());

                        // Display album cover tag value if present.
                        if (key.equals("image")) {
                            Image image = (Image) change.getValueAdded();
                            ImageView imageView = new ImageView(image);
                            ((Group) scene.getRoot()).getChildren().add(imageView);
                        }
                    }
                });
                
                media.getTracks().addListener(new ListChangeListener<Track>() {
                    public void onChanged(Change<? extends Track> change) {
                        try {
                            while (change.next()) {
                                if (!change.wasPermutated() && !change.wasUpdated()) {
                                    for (Track trk : change.getAddedSubList()) {
                                        System.out.println("Media Track: "+trk);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Exception getting tracks changes: "+e);
                        }
                    }
                });
                
                try {
                    final MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setOnError(new Runnable() {
                        public void run() {
                            System.err.println(">>> MediaPlayer ERR: " + mediaPlayer.getError());
                            exitOnError();
                        }
                    });

                    if (mediaPlayer.getError() == null) {
                        mediaPlayer.setAutoPlay(true);
                        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

                        MediaView mediaView = new MediaView(mediaPlayer);
                        mediaView.setOnError(new EventHandler<MediaErrorEvent>() {
                            public void handle(MediaErrorEvent t) {
                                System.err.println(">>> MediaView ERR: " + t);
                                exitOnError();
                            }
                        });
                        mediaView.getProperties().addListener(new MapChangeListener<Object, Object>() {
                            public void onChanged(Change<? extends Object, ? extends Object> change) {
                                System.err.println(change.getKey()
                                        + ": " + change.getValueRemoved() + " -> " + change.getValueAdded());
                            }
                        });

                        ((Group) scene.getRoot()).getChildren().add(mediaView);
                    } else {
                        System.err.println(">>> MediaPlayer ERR: " + mediaPlayer.getError());
                        exitOnError();
                    }
                } catch (NullPointerException npe) {
                    System.err.println(">>> MediaPlayer ERR: " + npe.toString());
                    exitOnError();
                } catch (MediaException me) {
                    System.err.println(">>> MediaPlayer ERR: " + me.toString());
                    exitOnError();
                }
            } else {
                System.err.println(">>> Media ERR: " + media.getError());
                exitOnError();
            }
        } catch (NullPointerException npe) {
            System.err.println(">>> Media ERR: " + npe.toString());
            exitOnError();
        } catch (IllegalArgumentException iae) {
            System.err.println(">>> Media ERR: " + iae.toString());
            exitOnError();
        } catch (UnsupportedOperationException uoe) {
            System.err.println(">>> Media ERR: " + uoe.toString());
            exitOnError();
        } catch (MediaException me) {
            System.err.println(">>> Media ERR: " + me.toString());
            exitOnError();
        }
    }

    private static void exitOnError() {
        boolean exitOnError = true;
        if (exitOnError) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            argSource = args[0];
        }
        Application.launch(args);
    }
}
