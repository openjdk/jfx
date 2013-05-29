/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import com.sun.javafx.pgstub.StubScene;
import com.sun.javafx.test.MouseEventGenerator;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;

public class SwipeEventTest {

    private boolean swiped;
    private boolean swiped2;

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        SwipeEvent e = new SwipeEvent(
                SwipeEvent.SWIPE_UP, 10, 20, 30, 40,
                false, true, false, true, false, 3, pickRes);

        assertSame(SwipeEvent.SWIPE_UP, e.getEventType());
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
        assertFalse(e.isInertia());
        assertEquals(3.0, e.getTouchCount(), 10e-20);
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isConsumed());

        e = new SwipeEvent(
                SwipeEvent.SWIPE_UP, 10, 20, 30, 40,
                true, false, true, false, true, 3, pickRes);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isDirect());
        assertFalse(e.isInertia());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        SwipeEvent e = new SwipeEvent(
                SwipeEvent.SWIPE_UP, 10, 20, 30, 40,
                false, true, false, true, false, 3, null);

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

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        SwipeEvent e = new SwipeEvent(n1, n2,
                SwipeEvent.SWIPE_UP, 10, 20, 30, 40,
                false, true, false, true, false, 3, pickRes);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(SwipeEvent.SWIPE_UP, e.getEventType());
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
        assertFalse(e.isInertia());
        assertEquals(3.0, e.getTouchCount(), 10e-20);
        assertFalse(e.isConsumed());

        e = new SwipeEvent(n1, n2,
                SwipeEvent.SWIPE_UP, 10, 20, 30, 40,
                true, false, true, false, true, 3, pickRes);
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
        SwipeEvent e = new SwipeEvent(n1, n2,
                SwipeEvent.SWIPE_UP, 10, 20, 30, 40,
                false, true, false, true, false, 3, null);

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
    public void shouldDeliverSwipeLeftEventToPickedNode() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        swiped = false;
        rect.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                swiped = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_LEFT, 1, 1, 1, 1, 1,
                false, false, false, false, false);

        assertFalse(swiped);

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_LEFT, 1, 150, 150, 150, 150,
                false, false, false, false, false);

        assertTrue(swiped);
    }

    @Test
    public void shouldPassTouchCount() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        swiped = false;

        rect.setOnSwipeUp(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                assertEquals(3, event.getTouchCount());
                swiped = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_UP, 3, 150, 150, 150, 150,
                false, false, false, false, false);

        assertTrue(swiped);
    }

    @Test
    public void shouldPassCoordinates() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        swiped = false;

        rect.setOnSwipeUp(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                assertEquals(151.0, event.getX(), 0.0001);
                assertEquals(152.0, event.getY(), 0.0001);
                assertEquals(153.0, event.getScreenX(), 0.0001);
                assertEquals(154.0, event.getScreenY(), 0.0001);
                swiped = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_UP, 3, 151, 152, 153, 154,
                false, false, false, false, false);

        assertTrue(swiped);
    }

    @Test
    public void shouldPassDirect() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        swiped = false;

        rect.setOnSwipeDown(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                assertEquals(swiped, event.isDirect());
                swiped = !swiped;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_DOWN, 3, 151, 152, 153, 154,
                false, false, false, false, false);

        assertTrue(swiped);

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_DOWN, 3, 151, 152, 153, 154,
                false, false, false, false, true);

        assertFalse(swiped);
    }

    @Test
    public void shouldCompute3dCoordinates() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setTranslateZ(50);

        swiped = false;
        swiped2 = false;
        rect.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                assertEquals(150, event.getX(), 0.00001);
                assertEquals(150, event.getY(), 0.00001);
                assertEquals(0, event.getZ(), 0.00001);
                swiped = true;
            }
        });

        scene.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                assertEquals(150, event.getX(), 0.00001);
                assertEquals(150, event.getY(), 0.00001);
                assertEquals(50, event.getZ(), 0.00001);
                swiped2 = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_LEFT, 1, 150, 150, 150, 150,
                false, false, false, false, false);

        assertTrue(swiped);
        assertTrue(swiped2);
    }

    @Test
    public void shouldContainPickResult() {
        Scene scene = createScene();
        final Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        swiped = false;

        rect.setOnSwipeUp(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                PickResult pickRes = event.getPickResult();
                assertNotNull(pickRes);
                assertSame(rect, pickRes.getIntersectedNode());
                assertEquals(151, pickRes.getIntersectedPoint().getX(), 0.00001);
                assertEquals(152, pickRes.getIntersectedPoint().getY(), 0.00001);
                assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);
                swiped = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_UP, 3, 151, 152, 153, 154,
                false, false, false, false, false);

        assertTrue(swiped);
    }

    @Test
    public void handlingAnyShouldGetAllTypes() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(SwipeEvent.ANY, new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                swiped = true;
            }
        });

        swiped = false;
        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_UP, 3, 151, 152, 153, 154,
                false, false, false, false, false);
        assertTrue(swiped);

        swiped = false;
        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_DOWN, 3, 151, 152, 153, 154,
                false, false, false, false, false);
        assertTrue(swiped);

        swiped = false;
        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_LEFT, 3, 151, 152, 153, 154,
                false, false, false, false, false);
        assertTrue(swiped);

        swiped = false;
        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_RIGHT, 3, 151, 152, 153, 154,
                false, false, false, false, false);
        assertTrue(swiped);
    }

    @Test
    public void unknownLocalShouldBeFixedByMousePosition() {
        MouseEventGenerator gen = new MouseEventGenerator();
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.setOnSwipeRight(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                swiped = true;
            }
        });

        swiped = false;

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_RIGHT, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                false, false, false, false, false);

        assertFalse(swiped);

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 150, 150));

        ((StubScene) scene.impl_getPeer()).getListener().swipeEvent(
                SwipeEvent.SWIPE_RIGHT, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                false, false, false, false, false);

        assertTrue(swiped);
    }

    @Test public void testToString() {
        SwipeEvent e = new SwipeEvent(SwipeEvent.SWIPE_RIGHT,
            100, 100, 200, 200,
            false, false, false, false,
            true, 3, null);

        String s = e.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private Scene createScene() {
        final Group root = new Group();

        final Scene scene = new Scene(root, 400, 400);

        Rectangle rect = new Rectangle(100, 100, 100, 100);
        Rectangle rect2 = new Rectangle(200, 200, 100, 100);

        root.getChildren().addAll(rect, rect2);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        return scene;
    }
}
