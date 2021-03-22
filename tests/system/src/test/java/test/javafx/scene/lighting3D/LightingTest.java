/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.lighting3D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SpotLight;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import test.util.Util;

// Since there appears to be a bug in snapshot with subscene, we are taking a snapshot of the scene and not
// the box, so the center of the box will be at the top left, (0, 0), of the image, and the light is
// straight in front. Without attenuation, at (0, 0) it will give its full color.
//
//       x
//  -----------
//  |         /
//  |        /     d - the distance of the light from the object
//  |       /      x - the horizontal distance of the sample point on the object
//  |      /       a - the angle to the sample point
// d|     /
//  |    /         tan(a) = x/d
//  |   /          cos(a) = cos(atan(x/d)) = 1 / sqrt((x/d)^2 + 1)
//  |a /
//  | /
//  |/
//
public class LightingTest {

    // 1d/255 is the smallest color resolution, but we use 10d/255 to avoid precision problems
    private static final double DELTA = 10d/255;

    private static final String FAIL_MESSAGE = "Wrong color value";

    private static final int LIGTH_DIST = 60;

    // X coordinates for the point used in Lambert tests
    private static final int[] LAMBERT_SAMPLE_DISTS = new int[] {0, 30, 60};

    // X coordinate for the point used in attenuation tests
    private static final int ATTN_SAMPLE_DIST = LIGTH_DIST;

    // Angles for points used in spotlight factor tests
    private static final double INNER_ANGLE = 20;
    private static final double OUTER_ANGLE = 40;
    private static final int INSIDE_ANGLE_SAMPLE = 18;
    private static final int MIDDLE_ANGLE_SAMPLE = 30;
    private static final int OUTSIDE_ANGLE_SAMPLE = 42;

    private static final SpotLight LIGHT = new SpotLight(Color.BLUE);
    private static final Box BOX = new Box(150, 150, 1);

    private static CountDownLatch startupLatch;
    private static Stage stage;

    public static void main(String[] args) throws Exception {
        initFX();
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage mainStage) {
            stage = mainStage;
            LIGHT.setTranslateZ(-LIGTH_DIST);
            var root = new Group(LIGHT, BOX);
            var scene = new Scene(root);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }

    @Before
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
        LIGHT.setInnerAngle(0);
        LIGHT.setOuterAngle(30);
        LIGHT.setFalloff(1);
        LIGHT.setConstantAttenuation(1);
        LIGHT.setLinearAttenuation(0);
        LIGHT.setQuadraticAttenuation(0);
        LIGHT.setMaxRange(Double.POSITIVE_INFINITY);
    }

    // The Lambert term is dot(N,L) = cos(a)
    @Test
    public void testLambert() {
        Util.runAndWait(() -> {
            // eliminating the spotlight factor contribution
            LIGHT.setFalloff(0);
            LIGHT.setOuterAngle(180);

            var snapshot = BOX.getScene().snapshot(null);
            for (int x : LAMBERT_SAMPLE_DISTS) {
                double sampledBlue = snapshot.getPixelReader().getColor(x, 0).getBlue();
                assertEquals(FAIL_MESSAGE, calculateLambertTerm(x), sampledBlue, DELTA);
            }
        });
    }

    @Test
    public void testAttenuation() {
        Util.runAndWait(() -> {
            // eliminating the spotlight factor contribution
            LIGHT.setFalloff(0);
            LIGHT.setOuterAngle(180);

            double diagDist = Math.sqrt(LIGTH_DIST * LIGTH_DIST + ATTN_SAMPLE_DIST * ATTN_SAMPLE_DIST);
            double lambertCenter = calculateLambertTerm(0);
            double lambertSample = calculateLambertTerm(ATTN_SAMPLE_DIST);

            LIGHT.setLinearAttenuation(0.01);
            var snapshot = BOX.getScene().snapshot(null);

            double attn = calculateAttenuationFactor(LIGTH_DIST);
            double sampledBlue = snapshot.getPixelReader().getColor(0, 0).getBlue();
            assertEquals(FAIL_MESSAGE, lambertCenter * attn, sampledBlue, DELTA);

            attn = calculateAttenuationFactor(diagDist);
            sampledBlue = snapshot.getPixelReader().getColor(ATTN_SAMPLE_DIST, 0).getBlue();
            assertEquals(FAIL_MESSAGE, lambertSample * attn, sampledBlue, DELTA);

            LIGHT.setLinearAttenuation(0);
            LIGHT.setQuadraticAttenuation(0.01);
            snapshot = BOX.getScene().snapshot(null);

            attn = calculateAttenuationFactor(LIGTH_DIST);
            sampledBlue = snapshot.getPixelReader().getColor(0, 0).getBlue();
            assertEquals(FAIL_MESSAGE, lambertCenter * attn, sampledBlue, DELTA);

            attn = calculateAttenuationFactor(diagDist);
            sampledBlue = snapshot.getPixelReader().getColor(ATTN_SAMPLE_DIST, 0).getBlue();
            assertEquals(FAIL_MESSAGE, lambertSample * attn, sampledBlue, DELTA);
        });
    }

    @Test
    public void testRange() {
        Util.runAndWait(() -> {
            // eliminating the spotlight factor contribution
            LIGHT.setFalloff(0);
            LIGHT.setOuterAngle(180);

            double diagDist = Math.sqrt(LIGTH_DIST * LIGTH_DIST + ATTN_SAMPLE_DIST * ATTN_SAMPLE_DIST);
            LIGHT.setMaxRange((LIGTH_DIST + diagDist) / 2);
            var snapshot = BOX.getScene().snapshot(null);

            double sampledBlue = snapshot.getPixelReader().getColor(0, 0).getBlue();
            assertEquals(FAIL_MESSAGE + ", should be in range", 1, sampledBlue, DELTA);

            sampledBlue = snapshot.getPixelReader().getColor(ATTN_SAMPLE_DIST, 0).getBlue();
            assertEquals(FAIL_MESSAGE + ", should be out of range", 0, sampledBlue, DELTA);
        });
    }

    @Test
    public void testSpotlightAttenuation() {
        Util.runAndWait(() -> {
            LIGHT.setInnerAngle(INNER_ANGLE);
            LIGHT.setOuterAngle(OUTER_ANGLE);
            var snapshot = BOX.getScene().snapshot(null);

            int innerX = angleToHorizontalDistance(INSIDE_ANGLE_SAMPLE);
            double spotFactor = 1;
            double sampledBlue = snapshot.getPixelReader().getColor(innerX, 0).getBlue();
            assertEquals(FAIL_MESSAGE, calculateLambertTerm(innerX) * spotFactor, sampledBlue, DELTA);

            int middleX = angleToHorizontalDistance(MIDDLE_ANGLE_SAMPLE);
            spotFactor = calculateSpotlightFactor(MIDDLE_ANGLE_SAMPLE);
            sampledBlue = snapshot.getPixelReader().getColor(middleX, 0).getBlue();
            assertEquals(FAIL_MESSAGE, calculateLambertTerm(middleX) * spotFactor, sampledBlue, DELTA);

            int outerX = angleToHorizontalDistance(OUTSIDE_ANGLE_SAMPLE);
            spotFactor = 0;
            sampledBlue = snapshot.getPixelReader().getColor(outerX, 0).getBlue();
            assertEquals(FAIL_MESSAGE, calculateLambertTerm(outerX) * spotFactor, sampledBlue, DELTA);
        });
    }

    private static double calculateLambertTerm(double x) {
        return Math.cos(Math.atan(x/LIGTH_DIST));
    }

    private static double calculateAttenuationFactor(double dist) {
        return 1 / (LIGHT.getConstantAttenuation() + LIGHT.getLinearAttenuation() * dist
                      + LIGHT.getQuadraticAttenuation() * dist * dist);
    }

    // I = pow((cosAngle - cosOuter) / (cosInner - cosOuter), falloff)
    private static double calculateSpotlightFactor(double degrees) {
        double numerator = degCos(degrees) - degCos(LIGHT.getOuterAngle());
        double denom = degCos(LIGHT.getInnerAngle()) - degCos(LIGHT.getOuterAngle());
        return Math.pow(numerator / denom, LIGHT.getFalloff());
    }

    private static double degCos(double degrees) {
        return Math.cos(Math.toRadians(degrees));
    }

    private static int angleToHorizontalDistance(double degrees) {
        return (int) (LIGTH_DIST * Math.tan(Math.toRadians(degrees)));
    }


    @AfterClass
    public static void teardown() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }
}
