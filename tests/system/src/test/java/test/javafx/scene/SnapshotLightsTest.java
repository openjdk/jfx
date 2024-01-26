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

package test.javafx.scene;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static org.junit.Assert.assertNotNull;

public class SnapshotLightsTest extends SnapshotCommon {

    static final int BOX_DIM = 50;

    @BeforeClass
    public static void setupOnce() {
        doSetupOnce();
    }

    @AfterClass
    public static void teardownOnce() {
        doTeardownOnce();
    }

    @Before
    public void setupEach() {
        assertNotNull(myApp);
        assertNotNull(myApp.primaryStage);
        assertTrue(myApp.primaryStage.isShowing());
    }

    @After
    public void teardownEach() {
    }

    private Scene buildScene(boolean inSubScene) {
        Box boxNode = new Box(BOX_DIM, BOX_DIM, BOX_DIM - 10);
        boxNode.setMaterial(new PhongMaterial(Color.WHITE));

        StackPane pane = new StackPane(boxNode);
        pane.setAlignment(Pos.CENTER);

        PointLight light = new PointLight(Color.BLUE);
        light.setTranslateZ(-150);
        pane.getChildren().add(light);

        if (inSubScene) {
            SubScene ss = new SubScene(pane, BOX_DIM, BOX_DIM);
            StackPane subSceneRoot = new StackPane(ss);
            subSceneRoot.setAlignment(Pos.CENTER);
            return new Scene(subSceneRoot, BOX_DIM, BOX_DIM);
        } else {
            return new Scene(pane, BOX_DIM, BOX_DIM);
        }
    }

    private void compareSnapshots(WritableImage base, WritableImage comp) {
        assertEquals(base.getWidth(), comp.getWidth(), 0.1);
        assertEquals(base.getHeight(), comp.getHeight(), 0.1);

        PixelReader baseReader = base.getPixelReader();
        PixelReader compReader = comp.getPixelReader();

        assertEquals(baseReader.getArgb(BOX_DIM / 2, BOX_DIM / 2), compReader.getArgb(BOX_DIM / 2, BOX_DIM / 2));
    }

    public SnapshotLightsTest() {
    }

    @Test
    public void testSceneNodeSnapshotLighting() throws Exception {
        Util.runAndWait(() -> {
            Scene scene = buildScene(false);
            WritableImage baseSnapshot = scene.snapshot(null);

            Node boxNode = scene.getRoot().getChildrenUnmodifiable().get(0);
            WritableImage nodeSnapshot = boxNode.snapshot(null, null);

            compareSnapshots(baseSnapshot, nodeSnapshot);
        });
    }

    @Test
    public void testSubSceneNodeSnapshotLighting() throws Exception {
        Util.runAndWait(() -> {
            Scene scene = buildScene(true);
            WritableImage baseSnapshot = scene.snapshot(null);

            SubScene ss = (SubScene)scene.getRoot().getChildrenUnmodifiable().get(0);
            Node boxNode = ss.getRoot().getChildrenUnmodifiable().get(0);
            WritableImage nodeSnapshot = boxNode.snapshot(null, null);

            compareSnapshots(baseSnapshot, nodeSnapshot);
        });
    }

    @Test
    public void testSubSceneSnapshotWithSceneLights() throws Exception {
        Util.runAndWait(() -> {
            Scene scene = buildScene(true);

            // SubScene is "separated" from Scene, so Scene's lights should not be included
            // Add an extra red light to make sure it is actually not included
            PointLight light = new PointLight(Color.RED);
            light.setTranslateZ(-150);
            StackPane sceneRootPane = (StackPane)scene.getRoot();
            sceneRootPane.getChildren().add(light);

            WritableImage baseSnapshot = scene.snapshot(null);

            SubScene ss = (SubScene)scene.getRoot().getChildrenUnmodifiable().get(0);
            WritableImage subSceneSnapshot = ss.snapshot(null, null);

            Node boxNode = ss.getRoot().getChildrenUnmodifiable().get(0);
            WritableImage nodeSnapshot = boxNode.snapshot(null, null);

            compareSnapshots(baseSnapshot, subSceneSnapshot);
            compareSnapshots(baseSnapshot, nodeSnapshot);
        });
    }
}
