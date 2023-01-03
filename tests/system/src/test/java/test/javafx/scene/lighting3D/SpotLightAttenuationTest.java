/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.SpotLight;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class SpotLightAttenuationTest extends LightingTest {

    // Angles for points used in spotlight factor tests
    private static final double INNER_ANGLE = 20;
    private static final double OUTER_ANGLE = 40;
    private static final int INSIDE_ANGLE_SAMPLE = 18;
    private static final int MIDDLE_ANGLE_SAMPLE = 30;
    private static final int OUTSIDE_ANGLE_SAMPLE = 42;

    private static final double[] FALLOFF_FACTORS = new double[] {0.5, 1, 1.5};

    private static final SpotLight LIGHT = new SpotLight(Color.BLUE);

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
    }

    @Test
    public void testSpotlightAttenuation() {
        Util.runAndWait(() -> {
            LIGHT.setInnerAngle(INNER_ANGLE);
            LIGHT.setOuterAngle(OUTER_ANGLE);
            for (double falloff : FALLOFF_FACTORS) {
                LIGHT.setFalloff(falloff);
                var snapshot = snapshot();

                int innerX = angleToDist(INSIDE_ANGLE_SAMPLE);
                double spotFactor = 1;
                double sampledBlue = snapshot.getPixelReader().getColor(innerX, 0).getBlue();
                assertEquals(FAIL_MESSAGE, calculateLambertTerm(innerX) * spotFactor, sampledBlue, DELTA);

                int middleX = angleToDist(MIDDLE_ANGLE_SAMPLE);
                spotFactor = calculateSpotlightFactor(MIDDLE_ANGLE_SAMPLE);
                sampledBlue = snapshot.getPixelReader().getColor(middleX, 0).getBlue();
                assertEquals(FAIL_MESSAGE, calculateLambertTerm(middleX) * spotFactor, sampledBlue, DELTA);

                int outerX = angleToDist(OUTSIDE_ANGLE_SAMPLE);
                spotFactor = 0;
                sampledBlue = snapshot.getPixelReader().getColor(outerX, 0).getBlue();
                assertEquals(FAIL_MESSAGE, calculateLambertTerm(outerX) * spotFactor, sampledBlue, DELTA);
            }
        });
    }

    // I = pow((cosAngle - cosOuter) / (cosInner - cosOuter), falloff)
    private double calculateSpotlightFactor(double degrees) {
        double numerator = degCos(degrees) - degCos(LIGHT.getOuterAngle());
        double denom = degCos(LIGHT.getInnerAngle()) - degCos(LIGHT.getOuterAngle());
        return Math.pow(numerator / denom, LIGHT.getFalloff());
    }

    private double degCos(double degrees) {
        return Math.cos(Math.toRadians(degrees));
    }

    private int angleToDist(double degrees) {
        return (int) (LIGHT_DIST * Math.tan(Math.toRadians(degrees)));
    }
}
