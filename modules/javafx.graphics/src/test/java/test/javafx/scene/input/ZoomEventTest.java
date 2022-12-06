/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.input;

import com.sun.javafx.scene.SceneHelper;
import test.com.sun.javafx.pgstub.StubScene;
import test.com.sun.javafx.test.MouseEventGenerator;
import javafx.event.Event;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ZoomEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZoomEventTest {

    private boolean zoomed;
    private boolean zoomed2;
    private PickResult pickRes;

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        ZoomEvent e = new ZoomEvent(
                ZoomEvent.ZOOM_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                0.75, 1.5, pickRes);

        assertSame(ZoomEvent.ZOOM_FINISHED, e.getEventType());
        assertSame(pickRes, e.getPickResult());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isDirect());
        assertTrue(e.isInertia());
        assertEquals(0.75, e.getZoomFactor(), 10e-20);
        assertEquals(1.5, e.getTotalZoomFactor(), 10e-20);
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isConsumed());

        e = new ZoomEvent(
                ZoomEvent.ZOOM_FINISHED, 10, 20, 30, 40,
                true, false, true, false, true, false,
                0.75, 1.5, pickRes);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isDirect());
        assertFalse(e.isInertia());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        ZoomEvent e = new ZoomEvent(
                ZoomEvent.ZOOM_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                0.75, 1.5, null);

        assertEquals(10, e.getX(), 10e-20);
        assertEquals(20, e.getY(), 10e-20);
        assertEquals(0, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertNotNull(e.getPickResult());
        assertNotNull(e.getPickResult().getIntersectedPoint());
        assertEquals(10, e.getPickResult().getIntersectedPoint().getX(), 10e-20);
        assertEquals(20, e.getPickResult().getIntersectedPoint().getY(), 10e-20);
        assertEquals(0, e.getPickResult().getIntersectedPoint().getZ(), 10e-20);
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
    }

    @Test public void testLongConstructor() {
        Rectangle node = new Rectangle(10, 10);
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);

        pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        ZoomEvent e = new ZoomEvent(n1, n2,
                ZoomEvent.ZOOM_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                0.75, 1.5, pickRes);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(ZoomEvent.ZOOM_FINISHED, e.getEventType());
        assertSame(pickRes, e.getPickResult());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isDirect());
        assertTrue(e.isInertia());
        assertEquals(0.75, e.getZoomFactor(), 10e-20);
        assertEquals(1.5, e.getTotalZoomFactor(), 10e-20);
        assertFalse(e.isConsumed());

        e = new ZoomEvent(n1, n2,
                ZoomEvent.ZOOM_FINISHED, 10, 20, 30, 40,
                true, false, true, false, true, false,
                0.75, 1.5, pickRes);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isDirect());
        assertFalse(e.isInertia());
    }

    @Test public void testLongConstructorWithoutPickResult() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        ZoomEvent e = new ZoomEvent(n1, n2,
                ZoomEvent.ZOOM_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                0.75, 1.5, null);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertEquals(10, e.getX(), 10e-20);
        assertEquals(20, e.getY(), 10e-20);
        assertEquals(0, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertNotNull(e.getPickResult());
        assertNotNull(e.getPickResult().getIntersectedPoint());
        assertEquals(10, e.getPickResult().getIntersectedPoint().getX(), 10e-20);
        assertEquals(20, e.getPickResult().getIntersectedPoint().getY(), 10e-20);
        assertEquals(0, e.getPickResult().getIntersectedPoint().getZ(), 10e-20);
    }

    @Test
    public void shouldDeliverZoomEventToPickedNode() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        zoomed = false;
        rect.setOnZoom(event -> {
            zoomed = true;
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 1, 1,
                50, 50, 50, 50, false, false, false, false, false, false);

        assertFalse(zoomed);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);

        assertTrue(zoomed);
    }

    @Test
    public void shouldPassFactors() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        zoomed = false;
        rect.setOnZoom(event -> {
            Assert.assertEquals(1.2, event.getZoomFactor(), 0.0001);
            Assert.assertEquals(2.4, event.getTotalZoomFactor(), 0.0001);
            zoomed = true;
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 1.2, 2.4,
                150, 150, 150, 150, false, false, false, false, false, false);

        assertTrue(zoomed);
    }

    @Test
    public void shouldPassModifiers() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        zoomed = false;
        rect.setOnZoom(event -> {
            assertTrue(event.isShiftDown());
            assertFalse(event.isControlDown());
            assertTrue(event.isAltDown());
            assertFalse(event.isMetaDown());
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, false, false);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoom(event -> {
            assertFalse(event.isShiftDown());
            assertTrue(event.isControlDown());
            assertFalse(event.isAltDown());
            assertTrue(event.isMetaDown());
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(zoomed);
    }

    @Test
    public void shouldPassDirect() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        zoomed = false;
        rect.setOnZoom(event -> {
            assertTrue(event.isDirect());
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoom(event -> {
            assertFalse(event.isDirect());
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(zoomed);
    }

    @Test
    public void shouldPassInertia() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        zoomed = false;
        rect.setOnZoom(event -> {
            assertTrue(event.isInertia());
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, false, true);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoom(event -> {
            assertFalse(event.isInertia());
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(zoomed);
    }

    @Test
    public void shouldPassEventType() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        zoomed = false;
        rect.setOnZoomStarted(event -> {
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoomFinished(event -> {
            zoomed = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);
    }

    @Test
    public void handlingAnyShouldGetAllTypes() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(ZoomEvent.ANY, event -> {
            zoomed = true;
        });

        zoomed = false;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);
    }

    @Test
    public void shouldDeliverWholeGestureToOneNode() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(ZoomEvent.ANY, event -> {
            zoomed = true;
        });
        rect2.addEventHandler(ZoomEvent.ANY, event -> {
            zoomed2 = true;
        });

        zoomed = false;
        zoomed2 = false;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);

        zoomed = false;
        zoomed2 = false;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);

        zoomed = false;
        zoomed2 = false;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);
    }

    @Test
    public void shouldCompute3dCoordinates() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setTranslateZ(50);

        zoomed = false;
        zoomed2 = false;
        rect.setOnZoom(event -> {
            Assert.assertEquals(150, event.getX(), 0.00001);
            Assert.assertEquals(150, event.getY(), 0.00001);
            Assert.assertEquals(0, event.getZ(), 0.00001);
            zoomed = true;
        });

        scene.setOnZoom(event -> {
            Assert.assertEquals(150, event.getX(), 0.00001);
            Assert.assertEquals(150, event.getY(), 0.00001);
            Assert.assertEquals(50, event.getZ(), 0.00001);
            zoomed2 = true;
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);

        assertTrue(zoomed);
        assertTrue(zoomed2);
    }

    @Test
    public void shouldContainPickResult() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(ZoomEvent.ANY, event -> {
            zoomed = true;
            pickRes = event.getPickResult();
        });
        rect2.addEventHandler(ZoomEvent.ANY, event -> {
            zoomed2 = true;
            pickRes = event.getPickResult();
        });

        zoomed = false;
        zoomed2 = false;
        pickRes = null;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);
        assertNotNull(pickRes);
        assertSame(rect1, pickRes.getIntersectedNode());
        assertEquals(150, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(150, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        zoomed = false;
        zoomed2 = false;
        pickRes = null;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);
        assertNotNull(pickRes);
        assertSame(rect2, pickRes.getIntersectedNode());
        assertEquals(250, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(250, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        zoomed = false;
        zoomed2 = false;
        pickRes = null;
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);
        assertNotNull(pickRes);
        assertSame(rect2, pickRes.getIntersectedNode());
        assertEquals(250, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(250, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);
    }

    @Test
    public void unknownLocationShouldBeReplacedByMouseLocation() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);
        rect1.addEventHandler(ZoomEvent.ANY, event -> {
            zoomed = true;
        });

        MouseEventGenerator generator = new MouseEventGenerator();

        zoomed = false;
        zoomed2 = false;
        rect2.setOnZoomStarted(event -> {
            Assert.assertEquals(250.0, event.getSceneX(), 0.0001);
            Assert.assertEquals(250.0, event.getSceneY(), 0.0001);
            zoomed2 = true;
        });
        SceneHelper.processMouseEvent(scene, generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(zoomed);
        assertTrue(zoomed2);

        zoomed = false;
        zoomed2 = false;
        rect2.setOnZoom(event -> {
            Assert.assertEquals(150.0, event.getSceneX(), 0.0001);
            Assert.assertEquals(150.0, event.getSceneY(), 0.0001);
            zoomed2 = true;
        });
        SceneHelper.processMouseEvent(scene, generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(zoomed);
        assertTrue(zoomed2);

        zoomed = false;
        zoomed2 = false;
        rect2.setOnZoomFinished(event -> {
            Assert.assertEquals(150.0, event.getSceneX(), 0.0001);
            Assert.assertEquals(150.0, event.getSceneY(), 0.0001);
            zoomed2 = true;
        });
        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(zoomed);
        assertTrue(zoomed2);
    }

    @Test
    public void finishedLocationShouldBeFixed() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setOnZoomFinished(event -> {
            Assert.assertEquals(250.0, event.getSceneX(), 0.0001);
            Assert.assertEquals(250.0, event.getSceneY(), 0.0001);
            zoomed = true;
        });

        zoomed = false;

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);

        assertFalse(zoomed);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(zoomed);
    }

    @Test
    public void unknownLocalShouldBeFixedByMousePosition() {
        MouseEventGenerator gen = new MouseEventGenerator();
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.setOnZoom(event -> {
            Assert.assertEquals(150.0, event.getSceneX(), 0.0001);
            Assert.assertEquals(150.0, event.getSceneY(), 0.0001);
            zoomed = true;
        });

        zoomed = false;

        SceneHelper.processMouseEvent(scene,
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertFalse(zoomed);

        SceneHelper.processMouseEvent(scene,
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 150, 150));

        ((StubScene) SceneHelper.getPeer(scene)).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(zoomed);
    }

    @Test public void testToString() {
        ZoomEvent e = new ZoomEvent(ZoomEvent.ZOOM,
            100, 100, 200, 200,
            false, false, false, false,
            true, false, 10, 20, null);

        String s = e.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private Scene createScene() {
        final Group root = new Group();

        final Scene scene = new Scene(root, 400, 400);

        Rectangle rect = new Rectangle(100, 100, 100, 100);
        Rectangle rect2 = new Rectangle(200, 200, 100, 100);

        ParentShim.getChildren(root).addAll(rect, rect2);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        return scene;
    }
}
