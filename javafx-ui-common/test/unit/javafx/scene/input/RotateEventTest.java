/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class RotateEventTest {

    private boolean rotated;
    private boolean rotated2;
    private PickResult pickRes;

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        RotateEvent e = new RotateEvent(
                RotateEvent.ROTATION_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                45, 55, pickRes);

        assertSame(RotateEvent.ROTATION_FINISHED, e.getEventType());
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
        assertEquals(45.0, e.getAngle(), 10e-20);
        assertEquals(55.0, e.getTotalAngle(), 10e-20);
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isConsumed());

        e = new RotateEvent(
                RotateEvent.ROTATION_FINISHED, 10, 20, 30, 40,
                true, false, true, false, true, false,
                45, 55, pickRes);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isDirect());
        assertFalse(e.isInertia());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        RotateEvent e = new RotateEvent(
                RotateEvent.ROTATION_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                45, 55, null);

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
        RotateEvent e = new RotateEvent(n1, n2,
                RotateEvent.ROTATION_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                45, 55, pickRes);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(RotateEvent.ROTATION_FINISHED, e.getEventType());
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
        assertEquals(45.0, e.getAngle(), 10e-20);
        assertEquals(55.0, e.getTotalAngle(), 10e-20);
        assertFalse(e.isConsumed());

        e = new RotateEvent(n1, n2,
                RotateEvent.ROTATION_FINISHED, 10, 20, 30, 40,
                true, false, true, false, true, false,
                45, 55, pickRes);
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
        RotateEvent e = new RotateEvent(n1, n2,
                RotateEvent.ROTATION_FINISHED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                45, 55, null);

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
    public void shouldDeliverRotateEventToPickedNode() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 1, 1,
                50, 50, 50, 50, false, false, false, false, false, false);
        
        assertFalse(rotated);

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);
        
        assertTrue(rotated);
    }
    
    @Test
    public void shouldPassAngles() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(90, event.getAngle(), 0.0001);
                assertEquals(-180, event.getTotalAngle(), 0.0001);
                rotated = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 90, -180,
                150, 150, 150, 150, false, false, false, false, false, false);
        
        assertTrue(rotated);
    }

    @Test
    public void shouldPassModifiers() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertTrue(event.isShiftDown());
                assertFalse(event.isControlDown());
                assertTrue(event.isAltDown());
                assertFalse(event.isMetaDown());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, false, false);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertFalse(event.isShiftDown());
                assertTrue(event.isControlDown());
                assertFalse(event.isAltDown());
                assertTrue(event.isMetaDown());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldPassDirect() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertTrue(event.isDirect());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertFalse(event.isDirect());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldPassInertia() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertTrue(event.isInertia());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, true);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertFalse(event.isInertia());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, false, true, false, true, true, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldPassEventType() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rotated = false;
        rect.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
    }

    @Test
    public void handlingAnyShouldGetAllTypes() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });

        rotated = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldDeliverWholeGestureToOneNode() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        rect2.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated2 = true;
            }
        });

        rotated = false;
        rotated2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);

        rotated = false;
        rotated2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);

        rotated = false;
        rotated2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);
    }

    @Test
    public void shouldCompute3dCoordinates() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setTranslateZ(50);

        rotated = false;
        rotated2 = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150, event.getX(), 0.00001);
                assertEquals(150, event.getY(), 0.00001);
                assertEquals(0, event.getZ(), 0.00001);
                rotated = true;
            }
        });

        scene.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150, event.getX(), 0.00001);
                assertEquals(150, event.getY(), 0.00001);
                assertEquals(50, event.getZ(), 0.00001);
                rotated2 = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);

        assertTrue(rotated);
        assertTrue(rotated2);
    }

    @Test
    public void shouldContainPickResult() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                pickRes = event.getPickResult();
                rotated = true;
            }
        });
        rect2.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                pickRes = event.getPickResult();
                rotated2 = true;
            }
        });

        rotated = false;
        rotated2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);
        assertNotNull(pickRes);
        assertSame(rect1, pickRes.getIntersectedNode());
        assertEquals(150, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(150, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        rotated = false;
        rotated2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);
        assertNotNull(pickRes);
        assertSame(rect2, pickRes.getIntersectedNode());
        assertEquals(250, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(250, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        rotated = false;
        rotated2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);
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
        rect1.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });

        MouseEventGenerator generator = new MouseEventGenerator();

        rotated = false;
        rotated2 = false;
        rect2.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                rotated2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(rotated);
        assertTrue(rotated2);

        rotated = false;
        rotated2 = false;
        rect2.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                rotated2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(rotated);
        assertTrue(rotated2);

        rotated = false;
        rotated2 = false;
        rect2.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                rotated2 = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(rotated);
        assertTrue(rotated2);
    }

    @Test
    public void finishedLocationShouldBeFixed() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                rotated = true;
            }
        });

        rotated = false;

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);

        assertFalse(rotated);

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(rotated);
    }

    @Test
    public void unknownLocalShouldBeFixedByMousePosition() {
        MouseEventGenerator gen = new MouseEventGenerator();
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                rotated = true;
            }
        });

        rotated = false;

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertFalse(rotated);

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 150, 150));

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(rotated);
    }

    @Test public void testToString() {
        RotateEvent e = new RotateEvent(RotateEvent.ROTATE,
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

        root.getChildren().addAll(rect, rect2);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        return scene;
    }
}
