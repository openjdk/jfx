/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class PointLightAttenuationTest extends LightingTest {

    // X coordinates for the point used in Lambert tests
    private static final int[] LAMBERT_SAMPLE_DISTS = new int[] {0, 30, 60};

    // X coordinate for the point used in attenuation tests
    private static final int ATTN_SAMPLE_DIST = LIGHT_DIST;

    private static final PointLight LIGHT = new PointLight(Color.BLUE);

    public static void main(String[] args) throws Exception {
        initFX();
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        LightingTest.light = LIGHT;

        Util.launch(startupLatch, TestApp.class);
    }

    @Before
    public void setupEach() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
        LIGHT.setLinearAttenuation(0);
        LIGHT.setQuadraticAttenuation(0);
        LIGHT.setMaxRange(Double.POSITIVE_INFINITY);
    }

    // The Lambert term is dot(N,L) = cos(a)
    @Test
    public void testLambert() {
        Util.runAndWait(() -> {
            var snapshot = snapshot();
            for (int x : LAMBERT_SAMPLE_DISTS) {
                double sampledBlue = snapshot.getPixelReader().getColor(x, 0).getBlue();
                assertEquals(FAIL_MESSAGE + " for " + x, calculateLambertTerm(x), sampledBlue, DELTA);
            }
        });
    }

    @Test
    public void testAttenuation() {
        Util.runAndWait(() -> {
            double diagDist = Math.sqrt(LIGHT_DIST * LIGHT_DIST + ATTN_SAMPLE_DIST * ATTN_SAMPLE_DIST);
            double lambertCenter = calculateLambertTerm(0);
            double lambertSample = calculateLambertTerm(ATTN_SAMPLE_DIST);

            LIGHT.setLinearAttenuation(0.01);
            doAttenuationTest(diagDist, lambertCenter, lambertSample);

            LIGHT.setLinearAttenuation(0);
            LIGHT.setQuadraticAttenuation(0.01);
            doAttenuationTest(diagDist, lambertCenter, lambertSample);
        });
    }

    private void doAttenuationTest(double diagDist, double lambertCenter, double lambertSample) {
        var snapshot = snapshot();

        var attn = calculateAttenuationFactor(LIGHT_DIST);
        var sampledBlue = snapshot.getPixelReader().getColor(0, 0).getBlue();
        assertEquals(FAIL_MESSAGE, lambertCenter * attn, sampledBlue, DELTA);

        attn = calculateAttenuationFactor(diagDist);
        sampledBlue = snapshot.getPixelReader().getColor(ATTN_SAMPLE_DIST, 0).getBlue();
        assertEquals(FAIL_MESSAGE, lambertSample * attn, sampledBlue, DELTA);
    }

    @Test
    public void testRange() {
        Util.runAndWait(() -> {
            double diagDist = Math.sqrt(LIGHT_DIST * LIGHT_DIST + ATTN_SAMPLE_DIST * ATTN_SAMPLE_DIST);
            LIGHT.setMaxRange((LIGHT_DIST + diagDist) / 2);
            var snapshot = snapshot();

            double sampledBlue = snapshot.getPixelReader().getColor(0, 0).getBlue();
            assertEquals(FAIL_MESSAGE + ", should be in range", 1, sampledBlue, DELTA);

            sampledBlue = snapshot.getPixelReader().getColor(ATTN_SAMPLE_DIST, 0).getBlue();
            assertEquals(FAIL_MESSAGE + ", should be out of range", 0, sampledBlue, DELTA);
        });
    }

    private double calculateAttenuationFactor(double dist) {
        return 1 / (LIGHT.getConstantAttenuation() + LIGHT.getLinearAttenuation() * dist
                      + LIGHT.getQuadraticAttenuation() * dist * dist);
    }
}
