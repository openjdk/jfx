/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;

/**
 * Responsible for performance measurements.
 */
final class Benchmark {

    private final Environment environment;
    private final FPSCounter fpsCouner = new FPSCounter();
    private final TranslateTransition animation = createAnimation();

    Benchmark(Environment environment) {
        this.environment = environment;
    }

    private TranslateTransition createAnimation() {
        var anim = new TranslateTransition(Duration.seconds(2));
        anim.setAutoReverse(true);
        anim.setCycleCount(Animation.INDEFINITE);
        anim.setFromZ(150);
        anim.setToZ(0);
        return anim;
    }

    Button createStopButton() {
        var stopGraphic = new Text("⏹");
        stopGraphic.setBoundsType(TextBoundsType.VISUAL);
        stopGraphic.setFill(Color.RED);
        stopGraphic.setFont(Font.font(20));

        var stopButton = new Button("", stopGraphic);
        stopButton.setPadding(new Insets(2.5));
        stopButton.setAlignment(Pos.CENTER_RIGHT);
        stopButton.setTooltip(new Tooltip("Stop measurements"));
        stopButton.setOnAction(e -> stopMeasurement());
        return stopButton;
    }

    Button createPlayButton() {
        var playGraphic = new Text("▶");
        playGraphic.setBoundsType(TextBoundsType.VISUAL);
        playGraphic.setFill(Color.GREEN);
        playGraphic.setFont(Font.font(40));

        var playButton = new Button("", playGraphic);
        playButton.setPadding(new Insets(1, 2, 2, 3));
        playButton.setTooltip(new Tooltip("Start measurements"));
        playButton.setOnAction(e -> startMeasurement());
        return playButton;
    }

    HBox createSphereControls() {
        var subdivisionSlider = new Slider(10, 1000, 60);
        subdivisionSlider.setMajorTickUnit(50);
        setupSlider(subdivisionSlider);

        var subdivisionLabel = new Label();
        subdivisionLabel.textProperty().bindBidirectional(subdivisionSlider.valueProperty(), new NumberStringConverter("#"));

        var sphere = new Button("Sphere");
        sphere.setOnAction(e -> switchTo(Models.createSphere((int) subdivisionSlider.getValue())));

        return new HBox(sphere, subdivisionSlider, subdivisionLabel);
    }

    HBox createMeshControls() {
        var quadSlider = new Slider(100, 5000, 1000);
        quadSlider.setMajorTickUnit(100);
        setupSlider(quadSlider);

        var quadLabel = new Label();
        quadLabel.textProperty().bindBidirectional(quadSlider.valueProperty(), new NumberStringConverter("#"));

        var mesh = new Button("Mesh");
        mesh.setOnAction(e -> switchTo(Models.createMeshView((int) quadSlider.getValue())));

        return new HBox(mesh, quadSlider, quadLabel);
    }

    private void setupSlider(Slider slider) {
        slider.setMinorTickCount(0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
    }

    private void switchTo(Node node) {
        stopMeasurement();
        animation.setNode(node);
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
