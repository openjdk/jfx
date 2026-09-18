/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package attenuation;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;

/// Responsible for performance measurements.
final class Benchmark {

    private final Environment environment;
    private final FPSCounter fpsCouner = new FPSCounter();
    private Animation animation = new PauseTransition();

    Benchmark(Environment environment) {
        this.environment = environment;
    }

    Button createStopButton() {
        var stopGraphic = createGraphic("⏹");
        stopGraphic.setFill(Color.RED);
        stopGraphic.setFont(Font.font(20));

        var stopButton = new Button("", stopGraphic);
        stopButton.setPadding(new Insets(2.5));
        stopButton.setAlignment(Pos.CENTER_RIGHT);
        stopButton.setTooltip(new Tooltip("Stop measurements"));
        stopButton.setOnAction(_ -> stopMeasurement());
        return stopButton;
    }

    Button createPlayButton() {
        var playGraphic = createGraphic("▶");
        playGraphic.setFill(Color.GREEN);

        var playButton = new Button("", playGraphic);
        playButton.setPadding(new Insets(1, 2, 2, 3));
        playButton.setTooltip(new Tooltip("Start measurements"));
        playButton.setOnAction(_ -> startMeasurement());
        return playButton;
    }

    HBox createSphereControls() {
        var subdivisionSlider = createSlider();
        HBox sliderControl = Controls.createSliderControl(subdivisionSlider);

        var button = createButton("Sphere", "◍", "Sphere subdivisions");
        button.setOnAction(_ -> switchTo(Models.createSphere((int) subdivisionSlider.getValue())));

        return new HBox(button, sliderControl);
    }

    HBox createStackedQuadsControls() {
        var quadSlider = createSlider();
        HBox sliderControl = Controls.createSliderControl(quadSlider);

        var button = createButton("Quads", "□", "Stacked quads");
        button.setOnAction(_ -> switchTo(Models.createStackedQuads((int) quadSlider.getValue())));

        return new HBox(button, sliderControl);
    }

    HBox createSpreadQuadsControls() {
        var quadSlider = createSlider();
        HBox sliderControl = Controls.createSliderControl(quadSlider);

        var button = createButton("Quads", "▦", "Spread quads");
        button.setOnAction(_ -> switchTo(Models.createSpreadQuads((int) quadSlider.getValue())));

        return new HBox(button, sliderControl);
    }

    HBox createStackedMeshesControls() {
        var meshSlider = createSlider();
        HBox sliderControl = Controls.createSliderControl(meshSlider);

        var button = createButton("Meshes", "□", "Stacked MeshViews");
        button.setOnAction(_ -> {
            var group = new Group();
            for (int i = 0; i < meshSlider.getValue(); i++) {
                var meshView = Models.createStackedQuads(1);
                group.getChildren().add(meshView);
            }
            switchTo(group);
        });

        return new HBox(button, sliderControl);
    }

    HBox createSpreadMeshesControls() {
        var meshSlider = createSlider();
        HBox sliderControl = Controls.createSliderControl(meshSlider);

        var button = createButton("Meshes", "▦", "Spread MeshViews (single animation)");
        button.setOnAction(_ -> switchTo(Models.createSpreadMeshes((int) meshSlider.getValue())));

        return new HBox(button, sliderControl);
    }

    HBox createSpreadMeshesAnimControls() {
        var meshSlider = createSlider();
        HBox sliderControl = Controls.createSliderControl(meshSlider);

        var button = createButton("Meshes", "▦▶", "Spread MeshViews multi-animation");
        button.setOnAction(_ -> {
            var meshesAnim = new ParallelTransition();
            Group spreadMeshes = Models.createSpreadMeshes((int) meshSlider.getValue());
            spreadMeshes.getChildren().forEach(meshView -> {
                TranslateTransition animation = createAnimation(meshView);
                animation.setRate(Math.random() * 2);
                meshesAnim.getChildren().add(animation);
            });
            switchTo(spreadMeshes, meshesAnim);
        });

        return new HBox(button, sliderControl);
    }

    private static Slider createSlider() {
        var slider = new Slider(0, 5000, 1000);
        slider.setMajorTickUnit(100);
        slider.setMinorTickCount(0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        return slider;
    }

    private static Button createButton(String text, String icon, String tooltip) {
        var graphic = createGraphic(icon);
        var button = new Button(text, graphic);
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private static Text createGraphic(String icon) {
        var graphic = new Text(icon);
        graphic.setBoundsType(TextBoundsType.VISUAL);
        graphic.setFont(new Font(40));
        return graphic;
    }

    private void switchTo(Node node) {
        switchTo(node, createAnimation(node));
    }

    private void switchTo(Node node, Animation anim) {
        stopMeasurement();
        animation = anim;
        environment.switchTo(node);
    }

    private void startMeasurement() {
        animation.playFromStart();
        fpsCouner.start();
    }

    private void stopMeasurement() {
        fpsCouner.stop();
        fpsCouner.reset();
        animation.stop();
    }

    private static TranslateTransition createAnimation(Node node) {
        var anim = new TranslateTransition(Duration.seconds(1), node);
        anim.setAutoReverse(true);
        anim.setCycleCount(Animation.INDEFINITE);
        anim.setFromZ(Environment.LIGHT_Z_DIST);
        anim.setToZ(10);
        return anim;
    }

    private final class FPSCounter extends AnimationTimer {

        private int skipFrames = 100;
        private long lastTime = -1;
        private long elapsedTime;
        private int elapsedFrames;
        private long totalElapsedTime;
        private int totalElapsedFrames;

        @Override
        public void handle(long now) {
            if (skipFrames > 0) {
                --skipFrames;
                return;
            }

            if (lastTime < 0) {
                lastTime = System.nanoTime();
                elapsedTime = 0;
                elapsedFrames = 0;
                totalElapsedTime = 0;
                totalElapsedFrames = 0;
                return;
            }

            long currTime = System.nanoTime();
            elapsedTime += currTime - lastTime;
            elapsedFrames += 1;
            totalElapsedTime += currTime - lastTime;
            totalElapsedFrames += 1;

            double elapsedSeconds = elapsedTime / 1e9;
            double totalElapsedSeconds = totalElapsedTime / 1e9;
            if (elapsedSeconds >= 5.0) {
                double fps = elapsedFrames / elapsedSeconds;
                System.out.println();
                System.out.println("instant fps: " + fps);
                double avgFps = totalElapsedFrames / totalElapsedSeconds;
                System.out.println("average fps: " + avgFps);
                System.out.flush();
                elapsedTime = 0;
                elapsedFrames = 0;
            }

            lastTime = currTime;
        }

        private void reset() {
            skipFrames = 100;
            lastTime = -1;
            elapsedTime = 0;
            elapsedFrames = 0;
            totalElapsedTime = 0;
            totalElapsedFrames = 0;
            System.out.println();
            System.out.println("reset benchmark");
        }
    }
}
