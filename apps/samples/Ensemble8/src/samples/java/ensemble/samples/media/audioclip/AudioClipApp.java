/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.media.audioclip;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A sample that demonstrates the basics of AudioClips.
 *
 * @sampleName Audio Clip
 * @preview preview.png
 * @see javafx.scene.media.AudioClip
 * @related /Graphics 3d/Xylophone
 * @highlight
 * @conditionalFeatures WEB, MEDIA
 */
public class AudioClipApp extends Application {
    public Parent createContent() {
        final double xStart = 12;
        final double xOffset = 30.0;
        final double barWidth = 22.0;

        Rectangle r1 = new Rectangle(0, 15, barWidth * 11.5, 10);
        r1.setFill(new Color(0.2, 0.12, 0.1, 1.0));
        Rectangle r2 = new Rectangle(0, -25, barWidth * 11.5, 10);
        r2.setFill(new Color(0.2, 0.12, 0.1, 1.0));

        final Group content = new Group(
                r1,
                r2,
                createKey(Color.PURPLE, xStart + 0 * xOffset, barWidth, 100, "/ensemble/samples/shared-resources/Note1.wav"),
                createKey(Color.BLUEVIOLET, xStart + 1 * xOffset, barWidth, 95, "/ensemble/samples/shared-resources/Note2.wav"),
                createKey(Color.BLUE, xStart + 2 * xOffset, barWidth, 90, "/ensemble/samples/shared-resources/Note3.wav"),
                createKey(Color.GREEN, xStart + 3 * xOffset, barWidth, 85, "/ensemble/samples/shared-resources/Note4.wav"),
                createKey(Color.GREENYELLOW, xStart + 4 * xOffset, barWidth, 80, "/ensemble/samples/shared-resources/Note5.wav"),
                createKey(Color.YELLOW, xStart + 5 * xOffset, barWidth, 75, "/ensemble/samples/shared-resources/Note6.wav"),
                createKey(Color.ORANGE, xStart + 6 * xOffset, barWidth, 70, "/ensemble/samples/shared-resources/Note7.wav"),
                createKey(Color.RED, xStart + 7 * xOffset, barWidth, 65, "/ensemble/samples/shared-resources/Note8.wav"));

        // A StackPane by default centers its children, here we extend it to
        // scale the content to fill the StackPane first.
        StackPane root = new StackPane() {
            @Override protected void layoutChildren() {
                // find biggest scale that will fit while keeping proportions
                double scale = Math.min(
                    (getWidth()-20) / content.getBoundsInLocal().getWidth(),
                    (getHeight()-20) / content.getBoundsInLocal().getHeight()
                );
                content.setScaleX(scale);
                content.setScaleY(scale);
                super.layoutChildren();
            }
        };
        root.getChildren().add(content);
        return root;
    }

    public static Rectangle createKey(Color color, double x, double width, double height, String sound) {
        // create a audio clip that this key will play
        final AudioClip barNote = new AudioClip(
                AudioClipApp.class.getResource(sound).toExternalForm());
        // create the rectangle that draws the key
        Rectangle rectangle = new Rectangle(x, -(height / 2), width, height);
        rectangle.setFill(color);
        Lighting lighting = new Lighting(new Light.Point(-20, -20, 100, Color.WHITE));
        lighting.setSurfaceScale(1);
        rectangle.setEffect(lighting);
        rectangle.setOnMousePressed((MouseEvent me) -> {
            barNote.play();
        });
        return rectangle;
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) { launch(args); }
}
