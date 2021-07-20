/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;

import javafx.animation.Interpolator;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;
import javafx.util.Callback;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import test.util.Util;

import static org.junit.Assert.*;

/**
 * Test program for showAndWait functionality.
 */
@RunWith(Parameterized.class)
public class Snapshot2Test extends SnapshotCommon {

    @BeforeClass
    public static void setupOnce() {
        doSetupOnce();
    }

    @AfterClass
    public static void teardownOnce() {
        doTeardownOnce();
    }

    // Flag indicating snapshot should be taken on a live scene, that is a
    // scene attached to the primary stage
    private final boolean live;

    // Flag indicating to use an existing image
    private final boolean useImage;

    // Temporary stage, scene, and node used for testing
    private TestStage tmpStage = null;
    private Scene tmpScene = null;
    private Node tmpNode = null;

    private static Collection params = null;

    private static final Object[] pLive = { Boolean.FALSE, Boolean.TRUE };
    private static final Object[] pUseImage = { Boolean.FALSE, Boolean.TRUE };

    @Parameters
    public static Collection getParams() {
        if (params == null) {
            params = new ArrayList();
            for (Object o0 : pLive) {
                for (Object o1 : pUseImage) {
                    params.add(new Object[] { o0, o1 });
                }
            }
        }
        return params;
    }

    public Snapshot2Test(Boolean live, Boolean useImage) {
        this.live = live;
        this.useImage = useImage;
    }

    @Before
    public void setupEach() {
        assertNotNull(myApp);
        assertNotNull(myApp.primaryStage);
        assertTrue(myApp.primaryStage.isShowing());
    }

    @After
    public void teardownEach() {
        Util.runAndWait(() -> {
            if (tmpStage != null && tmpStage.isShowing()) {
                tmpStage.hide();
            }
        });
    }

    // ========================== TEST CASES ==========================

    private void setupEmptyScene() {
        Util.runAndWait(() -> {
            Group root = new Group();
            tmpScene = new Scene(root);
            if (live) {
                tmpStage = new TestStage(tmpScene);
                assertNotNull(tmpScene.getWindow());
                tmpStage.show();
            } else {
                assertNull(tmpScene.getWindow());
            }
        });
    }

    // Verify a snapshot of an empty scene / root node
    @Test
    public void testSnapshotEmptySceneImm() {
        setupEmptyScene();

        final WritableImage img = useImage ? new WritableImage(1, 1) : null;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.snapshot(img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(1, (int)wimg.getWidth());
            assertEquals(1, (int)wimg.getHeight());
        });
    }

    @Test
    public void testSnapshotEmptySceneDefer() {
        setupEmptyScene();
        final WritableImage img = useImage ? new WritableImage(1, 1) : null;
        runDeferredSnapshotWait(tmpScene, result -> {
            assertSame(tmpScene, result.getSource());
            assertNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(1, (int)result.getImage().getWidth());
            assertEquals(1, (int)result.getImage().getHeight());

            return null;
        }, img);
    }

    private void doTestSnapshotEmptyNodeImm(final SnapshotParameters snapshotParams) {
        setupEmptyScene();
        final WritableImage img = useImage ? new WritableImage(1, 1) : null;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.getRoot().snapshot(snapshotParams, img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(1, (int)wimg.getWidth());
            assertEquals(1, (int)wimg.getHeight());
        });
    }

    @Test
    public void testSnapshotEmptyNodeImmNoParams() {
        doTestSnapshotEmptyNodeDefer(null);
    }

    @Test
    public void testSnapshotEmptyNodeImm() {
        doTestSnapshotEmptyNodeDefer(new SnapshotParameters());
    }

    private void doTestSnapshotEmptyNodeDefer(final SnapshotParameters snapshotParams) {
        setupEmptyScene();
        final WritableImage img = useImage ? new WritableImage(1, 1) : null;
        runDeferredSnapshotWait(tmpScene.getRoot(), result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(1, (int)result.getImage().getWidth());
            assertEquals(1, (int)result.getImage().getHeight());

            return null;
        }, snapshotParams, img);
    }

    @Test
    public void testSnapshotEmptyNodeDeferNoParams() {
        doTestSnapshotEmptyNodeImm(null);
    }

    @Test
    public void testSnapshotEmptyNodeDefer() {
        doTestSnapshotEmptyNodeImm(new SnapshotParameters());
    }

    private static final int SCENE_W = 200;
    private static final int SCENE_H = 100;
    private static final int NODE_W = SCENE_W - 2*10;
    private static final int NODE_H = SCENE_H - 2*5;

    private void setupSimpleScene() {
        Util.runAndWait(() -> {
            tmpNode = new Rectangle(10, 5, NODE_W, NODE_H);
            Group root = new Group();
            tmpScene = new Scene(root, SCENE_W, SCENE_H);
            root.getChildren().add(tmpNode);
            if (live) {
                tmpStage = new TestStage(tmpScene);
                assertNotNull(tmpScene.getWindow());
                tmpStage.show();
            } else {
                assertNull(tmpScene.getWindow());
            }
        });
    }

    private void setupImageScene(int width, int height) {
        Util.runAndWait(() -> {
            WritableImage image = new WritableImage(width, height);
            // Initialize image with a bilinear gradient
            var pixWriter = image.getPixelWriter();
            assertNotNull(pixWriter);
            double stepX = 1.0 / (width - 1);
            double stepY = 1.0 / (height - 1);
            double tX = 0;
            double tY = 0;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    pixWriter.setColor(x, y, (Color) Interpolator.LINEAR.interpolate(
                            Interpolator.LINEAR.interpolate(Color.CYAN, Color.YELLOW, tX),
                            Interpolator.LINEAR.interpolate(Color.MAGENTA, Color.WHITE, tX),
                            tY));
                    tX += stepX;
                    tX = tX > 1 ? 1 : tX;
                }
                tY += stepY;
                tY = tY > 1 ? 1 : tY;
                tX = 0;
            }
            tmpNode = new ImageView(image);
            Group root = new Group();
            tmpScene = new Scene(root, width, height);
            root.getChildren().add(tmpNode);
            if (live) {
                tmpStage = new TestStage(tmpScene);
                assertNotNull(tmpScene.getWindow());
                tmpStage.show();
            }
            else {
                assertNull(tmpScene.getWindow());
            }
        });
    }

    // Test snapshot of a simple scene

    @Test
    public void testSnapshotSimpleSceneImm() {
        setupSimpleScene();

        final WritableImage img = useImage ? new WritableImage(SCENE_W, SCENE_H) : null;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.snapshot(img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(SCENE_W, (int)wimg.getWidth());
            assertEquals(SCENE_H, (int)wimg.getHeight());
        });
    }

    @Test
    public void testSnapshotSimpleSceneDefer() {
        setupSimpleScene();

        final WritableImage img = useImage ? new WritableImage(SCENE_W, SCENE_H) : null;
        runDeferredSnapshotWait(tmpScene, result -> {
            assertSame(tmpScene, result.getSource());
            assertNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(SCENE_W, (int)result.getImage().getWidth());
            assertEquals(SCENE_H, (int)result.getImage().getHeight());

            return null;
        }, img);
    }

    @Test
    public void testSnapshotSimpleNodeImm() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        final WritableImage img = useImage ? new WritableImage(NODE_W, NODE_H) : null;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.getRoot().snapshot(snapshotParams, img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(NODE_W, (int)wimg.getWidth());
            assertEquals(NODE_H, (int)wimg.getHeight());
        });
    }

    @Test
    public void testSnapshotSimpleNodeDefer() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        final WritableImage img = useImage ? new WritableImage(NODE_W, NODE_H) : null;
        runDeferredSnapshotWait(tmpScene.getRoot(), result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(NODE_W, (int)result.getImage().getWidth());
            assertEquals(NODE_H, (int)result.getImage().getHeight());

            return null;
        }, snapshotParams, img);
    }

    // Test tiled snapshots

    private void doTestTiledSnapshotImm(int w, int h) {
        setupImageScene(w, h);
        Image original = ((ImageView) tmpNode).getImage();
        assertNotNull(original);
        WritableImage buffer = useImage ? new WritableImage(w, h) : null;
        Util.runAndWait(() -> {
            WritableImage snapshot = tmpNode.snapshot(null, buffer);
            assertNotNull(snapshot);
            if (buffer != null) {
                assertSame(buffer, snapshot);
            }
            assertTrue(comparePixels(original, snapshot));
        });
    }

    private void doTestTiledSnapshotDefer(int w, int h) {
        setupImageScene(w, h);
        Image original = ((ImageView) tmpNode).getImage();
        assertNotNull(original);
        WritableImage buffer = useImage ? new WritableImage(w, h) : null;
        runDeferredSnapshotWait(tmpScene.getRoot(), result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (buffer != null) {
                assertSame(buffer, result.getImage());
            }
            assertTrue(comparePixels(original, result.getImage()));
            return null;
        }, null, buffer);
    }

    private boolean comparePixels(Image imageA, Image imageB) {
        if (imageA == null) {
            return false;
        }
        if (imageB == null) {
            return false;
        }
        int width = (int)imageA.getWidth();
        if (width != (int)imageB.getWidth()) {
            return false;
        }
        int height = (int)imageA.getHeight();
        if (height != (int)imageB.getHeight()) {
            return false;
        }
        var pixRdrA = imageA.getPixelReader();
        var pixRdrB = imageB.getPixelReader();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pixRdrA.getArgb(x, y) != pixRdrB.getArgb(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testSnapshot2x1TilesSameSizeImm() {
        doTestTiledSnapshotImm(4100, 10);
    }

    @Test
    public void testSnapshot2x1TilesDifferentSizeImm() {
        doTestTiledSnapshotImm(4099, 10);
    }

    @Test
    public void testSnapshot1x2TilesSameSizeImm() {
        doTestTiledSnapshotImm(10, 4100);
    }

    @Test
    public void testSnapshot1x2TilesDifferentSizeImm() {
        doTestTiledSnapshotImm(10, 4099);
    }

    @Test
    public void testSnapshot2x2TilesSameSizeImm() {
        doTestTiledSnapshotImm(4100, 4100);
    }

    @Test
    public void testSnapshot2x2TilesDifferentSizeImm() {
        doTestTiledSnapshotImm(4099, 4099);
    }

    @Test
    public void testSnapshot2x2TilesSameHeightImm() {
        doTestTiledSnapshotImm(4099, 4100);
    }

    @Test
    public void testSnapshot2x2TilesSameWidthImm() {
        doTestTiledSnapshotImm(4100, 4099);
    }

    @Test
    public void testSnapshot2x1TilesSameSizeDefer() {
        doTestTiledSnapshotDefer(4100, 10);
    }

    @Test
    public void testSnapshot2x1TilesDifferentSizeDefer() {
        doTestTiledSnapshotDefer(4099, 10);
    }

    @Test
    public void testSnapshot1x2TilesSameSizeDefer() {
        doTestTiledSnapshotDefer(10, 4100);
    }

    @Test
    public void testSnapshot1x2TilesDifferentSizeDefer() {
        doTestTiledSnapshotDefer(10, 4099);
    }

    @Test
    public void testSnapshot2x2TilesSameSizeDefer() {
        doTestTiledSnapshotDefer(4100, 4100);
    }

    @Test
    public void testSnapshot2x2TilesDifferentSizeDefer() {
        doTestTiledSnapshotDefer(4099, 4099);
    }

    @Test
    public void testSnapshot2x2TilesSameHeightDefer() {
        doTestTiledSnapshotDefer(4099, 4100);
    }

    @Test
    public void testSnapshot2x2TilesSameWidthDefer() {
        doTestTiledSnapshotDefer(4100, 4099);
    }

    // Test node snapshot with a scale transform

    private void doTestSnapshotScaleNodeImm(int xScale, int yScale) {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setTransform(Transform.scale(xScale, yScale));
        final int WIDTH = NODE_W * xScale;
        final int HEIGHT = NODE_H * yScale;
        final WritableImage img = useImage ? new WritableImage(WIDTH, HEIGHT) : null;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.getRoot().snapshot(snapshotParams, img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(WIDTH, (int)wimg.getWidth());
            assertEquals(HEIGHT, (int)wimg.getHeight());
        });
    }

    private void doTestSnapshotScaleNodeDefer(int xScale, int yScale) {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setTransform(Transform.scale(xScale, yScale));
        final int WIDTH = NODE_W * xScale;
        final int HEIGHT = NODE_H * yScale;
        final WritableImage img = useImage ? new WritableImage(WIDTH, HEIGHT) : null;
        runDeferredSnapshotWait(tmpScene.getRoot(), result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(WIDTH, (int)result.getImage().getWidth());
            assertEquals(HEIGHT, (int)result.getImage().getHeight());

            return null;
        }, snapshotParams, img);
    }

    @Test
    public void testSnapshotScaleNodeImm() {
        doTestSnapshotScaleNodeImm(3, 3);
    }

    @Test
    public void testSnapshotScaleNodeDefer() {
        doTestSnapshotScaleNodeDefer(3, 3);
    }

    @Test
    public void testSnapshotBigXScaleNodeImm() {
        doTestSnapshotScaleNodeImm(100, 1);
    }

    @Test
    public void testSnapshotBigXScaleNodeDefer() {
        doTestSnapshotScaleNodeDefer(100, 1);
    }

    @Test
    public void testSnapshotBigYScaleNodeImm() {
        doTestSnapshotScaleNodeImm(1, 200);
    }

    @Test
    public void testSnapshotBigYScaleNodeDefer() {
        doTestSnapshotScaleNodeDefer(1, 200);
    }

    // Test node snapshot with a 90 degree rotate transform

    @Test
    public void testSnapshotRotateNodeImm() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        // Rotate by 90 degrees, which will swap width and height
        snapshotParams.setTransform(Transform.rotate(90, 0, 0));
        final int WIDTH = NODE_H;
        final int HEIGHT = NODE_W;
        final WritableImage img = useImage ? new WritableImage(WIDTH, HEIGHT) : null;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.getRoot().snapshot(snapshotParams, img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(WIDTH, (int)wimg.getWidth());
            assertEquals(HEIGHT, (int)wimg.getHeight());
        });
    }

    @Test
    public void testSnapshotRotateNodeDefer() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        // Rotate by 90 degrees, which will swap width and height
        snapshotParams.setTransform(Transform.rotate(90, 0, 0));
        final int WIDTH = NODE_H;
        final int HEIGHT = NODE_W;
        final WritableImage img = useImage ? new WritableImage(WIDTH, HEIGHT) : null;
        runDeferredSnapshotWait(tmpScene.getRoot(), result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(WIDTH, (int)result.getImage().getWidth());
            assertEquals(HEIGHT, (int)result.getImage().getHeight());

            return null;
        }, snapshotParams, img);
    }

    // Test viewport
    private static final int VP_X = 105;
    private static final int VP_Y = 20;
    private static final int VP_WIDTH = 160;
    private static final int VP_HEIGHT = 100;

    @Test
    public void testSnapshotViewportNodeImm() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setViewport(new Rectangle2D(VP_X, VP_Y, VP_WIDTH, VP_HEIGHT));
        final WritableImage img = useImage ? new WritableImage(NODE_W, NODE_H) : null;
        final int WIDTH = useImage ? NODE_W : VP_WIDTH;
        final int HEIGHT = useImage ? NODE_H : VP_HEIGHT;
        Util.runAndWait(() -> {
            WritableImage wimg = tmpScene.getRoot().snapshot(snapshotParams, img);
            assertNotNull(wimg);
            if (img != null) {
                assertSame(img, wimg);
            }

            assertEquals(WIDTH, (int)wimg.getWidth());
            assertEquals(HEIGHT, (int)wimg.getHeight());
        });
    }

    @Test
    public void testSnapshotViewportNodeDefer() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setViewport(new Rectangle2D(VP_X, VP_Y, VP_WIDTH, VP_HEIGHT));
        final WritableImage img = useImage ? new WritableImage(NODE_W, NODE_H) : null;
        final int WIDTH = useImage ? NODE_W : VP_WIDTH;
        final int HEIGHT = useImage ? NODE_H : VP_HEIGHT;
        runDeferredSnapshotWait(tmpScene.getRoot(), result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(WIDTH, (int)result.getImage().getWidth());
            assertEquals(HEIGHT, (int)result.getImage().getHeight());

            return null;
        }, snapshotParams, img);
    }

    // Test updating the node after the call to a deferred snapshot, in
    // the same runLater call. Verify that the change to the node is
    // reflected in the result

    private static final int NEW_WIDTH = 70;
    private static final int NEW_HEIGHT = 35;

    @Test
    public void testSnapshotUpdateNodeDefer() {
        setupSimpleScene();
        final SnapshotParameters snapshotParams = new SnapshotParameters();
        final WritableImage img = useImage ? new WritableImage(NODE_W, NODE_H) : null;
        final int WIDTH = useImage ? NODE_W : NEW_WIDTH;
        final int HEIGHT = useImage ? NODE_H : NEW_HEIGHT;
        Callback<SnapshotResult, Void> cb = result -> {
            assertSame(tmpScene.getRoot(), result.getSource());
            assertNotNull(result.getSnapshotParameters());
            assertNotNull(result.getImage());
            if (img != null) {
                assertSame(img, result.getImage());
            }

            assertEquals(WIDTH, (int)result.getImage().getWidth());
            assertEquals(HEIGHT, (int)result.getImage().getHeight());

            return null;
        };
        Runnable runAfter = () -> {
            assertTrue(tmpNode instanceof Rectangle);
            Rectangle rect = (Rectangle)tmpNode;
            rect.setWidth(NEW_WIDTH);
            rect.setHeight(NEW_HEIGHT);
        };

        runDeferredSnapshotWait(tmpScene.getRoot(), cb, snapshotParams, img, runAfter);
    }

}
