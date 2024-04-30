/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates.
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
package ensemble.samples.graphics2d.effects.reflection;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how a reflection effect is affected by various settings.
 *
 * @sampleName Reflection
 * @preview preview.png
 * @docUrl http://docs.oracle.com/javase/8/javafx/visual-effects-tutorial/visual_effects.htm#JFXTE191 JavaFX Visual Effects
 * @playground reflection.bottomOpacity (name="Reflection Bottom Opacity", min=0, max=1)
 * @playground reflection.topOpacity (name="Reflection Top Opacity", min=0, max=1)
 * @playground reflection.fraction (name="Reflection Fraction", min=0, max=1)
 * @playground reflection.topOffset (name="Reflection Top Offset", min=-10, max=10)
 *
 * @see javafx.scene.effect.Reflection
 * @see javafx.scene.effect.Effect
 * @embedded
 *
 * @related /Graphics 2d/Bouncing Balls
 * @related /Graphics 2d/Canvas Fireworks
 * @related /Graphics 2d/Display Shelf
 * @related /Graphics 2d/Effects/Drop Shadow
 * @related /Graphics 2d/Effects/Gaussian Blur
 * @related /Graphics 2d/Effects/Inner Shadow
 * @related /Graphics 2d/Effects/Sepia Tone
 */
public class ReflectionApp extends Application {

    private Reflection reflection = new Reflection();

    public Parent createContent() {
        String BOAT = "/ensemble/samples/shared-resources/boat.jpg";
        Image image = new Image(getClass().getResourceAsStream(BOAT));
        ImageView sample = new ImageView(image);
        sample.setPreserveRatio(true);
        sample.setEffect(reflection);

        return new Group(sample);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
