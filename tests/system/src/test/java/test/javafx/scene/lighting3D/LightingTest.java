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

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;

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
public abstract class LightingTest {

    // 1d/255 is the smallest color resolution, but we use 10d/255 to avoid precision problems
    protected static final double DELTA = 10d/255;
    protected static final String FAIL_MESSAGE = "Wrong color value";

    protected static final int LIGHT_DIST = 60;

    protected static LightBase light;

    private static final Box BOX = new Box(150, 150, 1);

    protected static CountDownLatch startupLatch;
    private static Stage stage;

    public static class TestApp extends Application {

        @Override
        public void start(Stage mainStage) {
            stage = mainStage;
            light.setTranslateZ(-LIGHT_DIST);
            stage.setScene(new Scene(new Group(light, BOX)));
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }

    protected WritableImage snapshot() {
        return BOX.getScene().snapshot(null);
    }

    protected double calculateLambertTerm(double x) {
        return Math.cos(Math.atan(x/LIGHT_DIST));
    }

    @AfterClass
    public static void teardown() {
        Util.shutdown(stage);
    }
}
