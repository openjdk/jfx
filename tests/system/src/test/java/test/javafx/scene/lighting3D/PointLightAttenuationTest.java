/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertEquals;

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
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import test.util.Util;

public class PointLightAttenuationTest {

    // 1d/255 is the smallest color resolution, but we use 10d/255 to avoid precision problems
    private static final double DELTA = 10d/255;
    private static final int LIGTH_DIST = 60;
    private static final int SAMPLE_DIST = 60;

    private static CountDownLatch startupLatch;
    private static Stage stage;
    private static PointLight light = new PointLight(Color.BLUE);
    private static Box box = new Box(150, 150, 1);

    public static void main(String[] args) throws Exception {
        initFX();
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }

    @Before
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage mainStage) {
            stage = mainStage;
            light.setTranslateZ(-LIGTH_DIST);
            var root = new Group(light, box);
            var scene = new Scene(root);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }

    @Test
    public void testAttenuation() {
        Util.runAndWait(() -> {
            // Since there appears to be a bug in snapshot with subscene, we are taking a snapshot of the scene and not
            // the box, so the center of the box will be at the top left, (0, 0), of the image, and the light is
            // straight in front. Without attenuation, at (0, 0) it will give its full color. At (SAMPLE_DIST, 0) and
            // LIGTH_DIST == SAMPLE_DIST, it will give cos(45) = 1/sqrt(2) of its color.
            var snapshot = box.getScene().snapshot(null);
            double nonAttenBlueCenter = snapshot.getPixelReader().getColor(0, 0).getBlue();
            double nonAttenBlueDiag = snapshot.getPixelReader().getColor(SAMPLE_DIST, 0).getBlue();
            assertEquals("Wrong color value", 1, nonAttenBlueCenter, DELTA);
            assertEquals("Wrong color value", 1/Math.sqrt(2), nonAttenBlueDiag, DELTA);

            double diagDist = Math.sqrt(LIGTH_DIST * LIGTH_DIST + SAMPLE_DIST * SAMPLE_DIST);

            light.setLinearAttenuation(0.01);
            double attnCenter = 1 / (1 + 0.01 * LIGTH_DIST);
            double attnDiag = 1 / (1 + 0.01 * diagDist);
            snapshot = box.getScene().snapshot(null);
            double attenBlueCenter = snapshot.getPixelReader().getColor(0, 0).getBlue();
            double attenBlueDiag = snapshot.getPixelReader().getColor(SAMPLE_DIST, 0).getBlue();
            assertEquals("Wrong color value", nonAttenBlueCenter * attnCenter, attenBlueCenter, DELTA);
            assertEquals("Wrong color value", nonAttenBlueDiag * attnDiag, attenBlueDiag, DELTA);

            light.setLinearAttenuation(0);
            light.setQuadraticAttenuation(0.01);
            attnCenter = 1 / (1 + 0.01 * LIGTH_DIST * LIGTH_DIST);
            attnDiag = 1 / (1 + 0.01 * diagDist * diagDist);
            snapshot = box.getScene().snapshot(null);
            attenBlueCenter = snapshot.getPixelReader().getColor(0, 0).getBlue();
            attenBlueDiag = snapshot.getPixelReader().getColor(SAMPLE_DIST, 0).getBlue();
            assertEquals("Wrong color value", nonAttenBlueCenter * attnCenter, attenBlueCenter, DELTA);
            assertEquals("Wrong color value", nonAttenBlueDiag * attnDiag, attenBlueDiag, DELTA);

            light.setQuadraticAttenuation(0);
            light.setMaxRange((LIGTH_DIST + diagDist) / 2);
            snapshot = box.getScene().snapshot(null);
            nonAttenBlueCenter = snapshot.getPixelReader().getColor(0, 0).getBlue();
            nonAttenBlueDiag = snapshot.getPixelReader().getColor(SAMPLE_DIST, 0).getBlue();
            assertEquals("Wrong color value, should be in range", 1, nonAttenBlueCenter, DELTA);
            assertEquals("Wrong color value, should be out of range", 0, nonAttenBlueDiag, DELTA);
        });
    }

    @AfterClass
    public static void teardown() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }
}
