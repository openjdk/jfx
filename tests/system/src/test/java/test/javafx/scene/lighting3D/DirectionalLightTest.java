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
import javafx.geometry.Point3D;
import javafx.scene.DirectionalLight;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class DirectionalLightTest extends LightingTest {

    private static final Point3D[] DIRECTIONS = { new Point3D(0, 0, 1), new Point3D(0, 1, 1), new Point3D(0, 0, -1) };

    private static final DirectionalLight LIGHT = new DirectionalLight(Color.BLUE);

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
    public void testDirectionalLight() {
        Util.runAndWait(() -> {
            for (Point3D direction : DIRECTIONS) {
                LIGHT.setDirection(direction);
                double sampledBlue = snapshot().getPixelReader().getColor(0, 0).getBlue();
                assertEquals(FAIL_MESSAGE, dotProduct(direction), sampledBlue, DELTA);
            }
        });
    }

    private double dotProduct(Point3D direction) {
        double value = -direction.normalize().dotProduct(0, 0, -1); // the normal of the front of the box is (0, 0, -1)
        return value < 0 ? 0 : value;
    }
}
