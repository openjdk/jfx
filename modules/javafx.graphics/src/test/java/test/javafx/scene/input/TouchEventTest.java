/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.ParentShim;
import javafx.scene.input.PickResult;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class TouchEventTest {
    private static final int SANE_BENCHMARK_CYCLES = 1000000;
    private static final int CRAZY_BENCHMARK_CYCLES = 500000;

    private int touched;

    @Test public void testTouchPointConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        TouchPoint tp = new TouchPoint(2, TouchPoint.State.STATIONARY,
                10, 20, 30, 40, node, pickRes);

        assertEquals(2, tp.getId());
        assertSame(TouchPoint.State.STATIONARY, tp.getState());
        assertEquals(18, tp.getX(), 10e-20);
        assertEquals(27, tp.getY(), 10e-20);
        assertEquals(150, tp.getZ(), 10e-20);
        assertEquals(10, tp.getSceneX(), 10e-20);
        assertEquals(20, tp.getSceneY(), 10e-20);
        assertEquals(30, tp.getScreenX(), 10e-20);
        assertEquals(40, tp.getScreenY(), 10e-20);
        assertSame(node, tp.getTarget());
        assertSame(pickRes, tp.getPickResult());
    }

    @Test public void testTouchPointConstructorWithoutPickResult() {
        Rectangle node = new Rectangle();
        TouchPoint tp = new TouchPoint(2, TouchPoint.State.STATIONARY,
                10, 20, 30, 40, node, null);

        assertEquals(10, tp.getX(), 10e-20);
        assertEquals(20, tp.getY(), 10e-20);
        assertEquals(0, tp.getZ(), 10e-20);
        assertEquals(10, tp.getSceneX(), 10e-20);
        assertEquals(20, tp.getSceneY(), 10e-20);
        assertEquals(30, tp.getScreenX(), 10e-20);
        assertEquals(40, tp.getScreenY(), 10e-20);
        assertNotNull(tp.getPickResult());
        assertNotNull(tp.getPickResult().getIntersectedPoint());
        assertEquals(10, tp.getPickResult().getIntersectedPoint().getX(), 10e-20);
        assertEquals(20, tp.getPickResult().getIntersectedPoint().getY(), 10e-20);
        assertEquals(0, tp.getPickResult().getIntersectedPoint().getZ(), 10e-20);
    }

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        PickResult pickRes1 = new PickResult(node, new Point3D(15, 25, 100), 33);
        PickResult pickRes2 = new PickResult(node, new Point3D(16, 26, 101), 33);
        TouchPoint tp1 = new TouchPoint(2, TouchPoint.State.STATIONARY, 10, 20, 30, 40, node, pickRes1);
        TouchPoint tp2 = new TouchPoint(3, TouchPoint.State.PRESSED, 11, 21, 31, 41, node, pickRes2);

        TouchEvent e = new TouchEvent(
                TouchEvent.TOUCH_PRESSED, tp2,
                new ArrayList<>(Arrays.asList(tp1, tp2)), 158,
                false, true, false, true);

        assertSame(TouchEvent.TOUCH_PRESSED, e.getEventType());
        assertSame(tp2, e.getTouchPoint());
        assertEquals(2, e.getTouchPoints().size());
        assertSame(tp1, e.getTouchPoints().get(0));
        assertSame(tp2, e.getTouchPoints().get(1));
        assertEquals(158, e.getEventSetId());
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isConsumed());

        e = new TouchEvent(
                TouchEvent.TOUCH_PRESSED, tp2,
                new ArrayList<>(Arrays.asList(tp1, tp2)), 158,
                true, false, true, false);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
    }

    @Test public void testLongConstructor() {
        Rectangle node = new Rectangle(10, 10);
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);

        PickResult pickRes1 = new PickResult(node, new Point3D(15, 25, 100), 33);
        PickResult pickRes2 = new PickResult(node, new Point3D(16, 26, 101), 33);
        TouchPoint tp1 = new TouchPoint(2, TouchPoint.State.STATIONARY, 10, 20, 30, 40, node, pickRes1);
        TouchPoint tp2 = new TouchPoint(3, TouchPoint.State.PRESSED, 11, 21, 31, 41, node, pickRes2);

        TouchEvent e = new TouchEvent(n1, n2,
                TouchEvent.TOUCH_PRESSED, tp2,
                new ArrayList<>(Arrays.asList(tp1, tp2)), 158,
                false, true, false, true);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(TouchEvent.TOUCH_PRESSED, e.getEventType());
        assertSame(tp2, e.getTouchPoint());
        assertEquals(2, e.getTouchPoints().size());
        assertSame(tp1, e.getTouchPoints().get(0));
        assertSame(tp2, e.getTouchPoints().get(1));
        assertEquals(158, e.getEventSetId());
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isConsumed());

        e = new TouchEvent(n1, n2,
                TouchEvent.TOUCH_PRESSED, tp2,
                new ArrayList<>(Arrays.asList(tp1, tp2)), 158,
                true, false, true, false);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
    }

    @Test
    public void shouldPassModifiers() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(true, event.isShiftDown());
                    Assert.assertEquals(false, event.isControlDown());
                    Assert.assertEquals(true, event.isAltDown());
                    Assert.assertEquals(false, event.isMetaDown());
                    break;
                case 2:
                    Assert.assertEquals(false, event.isShiftDown());
                    Assert.assertEquals(true, event.isControlDown());
                    Assert.assertEquals(false, event.isAltDown());
                    Assert.assertEquals(true, event.isMetaDown());
                    break;
                case 3:
                    Assert.assertEquals(false, event.isShiftDown());
                    Assert.assertEquals(true, event.isControlDown());
                    Assert.assertEquals(true, event.isAltDown());
                    Assert.assertEquals(false, event.isMetaDown());
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, true, false, true);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(3, touched);
    }

    @Test
    public void shouldCountTouchesCorrectly() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(1, event.getTouchCount());
                    Assert.assertEquals(1, event.getTouchPoints().size());
                    break;
                case 2:
                case 3:
                    Assert.assertEquals(2, event.getTouchCount());
                    Assert.assertEquals(2, event.getTouchPoints().size());
                    break;
                case 4:
                case 5:
                    Assert.assertEquals(2, event.getTouchCount());
                    Assert.assertEquals(2, event.getTouchPoints().size());
                    break;
                case 6:
                    Assert.assertEquals(1, event.getTouchCount());
                    Assert.assertEquals(1, event.getTouchPoints().size());
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, true, false, true);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(6, touched);
    }

    @Test
    public void shouldGenerateCorrectEventSetIDs() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(1, event.getEventSetId());
                    break;
                case 2:
                case 3:
                    Assert.assertEquals(2, event.getEventSetId());
                    break;
                case 4:
                case 5:
                    Assert.assertEquals(3, event.getEventSetId());
                    break;
                case 6:
                    Assert.assertEquals(4, event.getEventSetId());
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, true, false, true);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(6, touched);
    }

    @Test
    public void shouldReIDTouchPoints() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.setOnTouchPressed(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 1:
                    Assert.assertEquals(110.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(110.0, event.getTouchPoint().getY(), 0.0001);
                    assertEquals(1, touched);
                    break;
                case 2:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    assertEquals(2, touched);
                    break;
                case 3:
                    Assert.assertEquals(130.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoint().getY(), 0.0001);
                    assertEquals(3, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 3, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 152, 130, 130, 130, 130);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(3, touched);
    }

    @Test
    public void shouldNotReuseTouchPointID() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.setOnTouchPressed(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 1:
                    Assert.assertEquals(110.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(110.0, event.getTouchPoint().getY(), 0.0001);
                    assertEquals(1, touched);
                    break;
                case 2:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    assertEquals(2, touched);
                    break;
                case 3:
                    Assert.assertEquals(130.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoint().getY(), 0.0001);
                    assertEquals(3, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 130, 130, 130, 130);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(3, touched);
    }

    @Test
    public void shouldMaintainPressOrder() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.setOnTouchPressed(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 1:
                    Assert.assertEquals(110.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(110.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(1, touched);
                    break;
                case 2:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(2, touched);
                    break;
                case 3:
                    Assert.assertEquals(130.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(3, event.getTouchPoints().get(1).getId());
                    assertEquals(3, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 130, 130, 130, 130);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(3, touched);
    }

    @Test
    public void shouldMaintainIDMapping() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.setOnTouchPressed(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 1:
                    Assert.assertEquals(110.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(110.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(1, touched);
                    break;
                case 2:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(2, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        rect.setOnTouchMoved(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 1:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(3, touched);
                    break;
                case 2:
                    Assert.assertEquals(110.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(110.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(4, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 127, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 127, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 1368, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void shouldMaintainIDMappingInDynamicConditions() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.setOnTouchPressed(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 1:
                    Assert.assertEquals(110.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(110.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(1, touched);
                    break;
                case 2:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(1, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(2, event.getTouchPoints().get(1).getId());
                    assertEquals(2, touched);
                    break;
                case 3:
                    Assert.assertEquals(160.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(160.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(2, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(3, event.getTouchPoints().get(1).getId());
                    assertEquals(3, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        rect.setOnTouchMoved(event -> {
            touched++;
            switch(event.getTouchPoint().getId()) {
                case 2:
                    Assert.assertEquals(120.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(120.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(2, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(3, event.getTouchPoints().get(1).getId());
                    assertEquals(4, touched);
                    break;
                case 3:
                    Assert.assertEquals(160.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(160.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(2, event.getTouchPoints().get(0).getId());
                    Assert.assertEquals(3, event.getTouchPoints().get(1).getId());
                    assertEquals(5, touched);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 127, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 127, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1368, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 11, 160, 160, 160, 160);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 127, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 11, 160, 160, 160, 160);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 127, 120, 120, 120, 120);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(5, touched);
    }

    @Test
    public void shouldResetIDsAfterGesture() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                case 2:
                    Assert.assertEquals(1, event.getEventSetId());
                    Assert.assertEquals(touched, event.getTouchPoint().getId());
                    break;
                case 3:
                case 4:
                    Assert.assertEquals(2, event.getEventSetId());
                    Assert.assertEquals(touched - 2, event.getTouchPoint().getId());
                    break;
                case 5:
                case 6:
                    Assert.assertEquals(1, event.getEventSetId());
                    Assert.assertEquals(touched - 4, event.getTouchPoint().getId());
                    break;
                case 7:
                case 8:
                    Assert.assertEquals(2, event.getEventSetId());
                    Assert.assertEquals(touched - 6, event.getTouchPoint().getId());
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();


        assertEquals(8, touched);
    }

    @Test
    public void touchPointsShouldContainTouchPoint() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                case 3:
                    assertSame(event.getTouchPoint(), event.getTouchPoints().get(0));
                    break;
                case 2:
                case 4:
                    assertSame(event.getTouchPoint(), event.getTouchPoints().get(1));
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 120, 120, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void touchPointsShouldHaveCorrectTarget() {
        Scene scene = createScene();

        final Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        final Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            assertSame(rect1, event.getTouchPoint().getTarget());
            assertSame(rect2, event.getTouchPoints().get(1).getTarget());
        });
        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            assertSame(rect2, event.getTouchPoint().getTarget());
            assertSame(rect1, event.getTouchPoints().get(0).getTarget());
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 220, 220, 220, 220);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 220, 220, 220, 220);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void shouldCompute3dCoordinates() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setTranslateZ(50);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            Assert.assertEquals(110, event.getTouchPoint().getX(), 0.00001);
            Assert.assertEquals(110, event.getTouchPoint().getY(), 0.00001);
            Assert.assertEquals(0, event.getTouchPoint().getZ(), 0.00001);
        });

        scene.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            Assert.assertEquals(110, event.getTouchPoint().getX(), 0.00001);
            Assert.assertEquals(110, event.getTouchPoint().getY(), 0.00001);
            Assert.assertEquals(50, event.getTouchPoint().getZ(), 0.00001);
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, true, false, true);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, true, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(6, touched);
    }

    @Test
    public void touchPointsShouldHaveCorrectPickResult() {
        Scene scene = createScene();

        final Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        final Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            assertSame(rect1, event.getTouchPoint().getTarget());
            assertSame(rect2, event.getTouchPoints().get(1).getTarget());
            PickResult res1 = event.getTouchPoint().getPickResult();
            PickResult res2 = event.getTouchPoints().get(1).getPickResult();
            switch(touched) {
                case 1:
                    assertNotNull(res1);
                    assertSame(rect1, res1.getIntersectedNode());
                    assertEquals(110, res1.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(110, res1.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);

                    assertNotNull(res2);
                    assertSame(rect2, res2.getIntersectedNode());
                    assertEquals(220, res2.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(220, res2.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);
                    break;
                case 3:
                    assertNotNull(res1);
                    assertSame(rect2, res1.getIntersectedNode());
                    assertEquals(220, res1.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(220, res1.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);

                    assertNotNull(res2);
                    assertSame(rect1, res2.getIntersectedNode());
                    assertEquals(110, res2.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(110, res2.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);
                    break;
                default:
                    fail("Wrong event delivered");
            }
        });
        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            assertSame(rect2, event.getTouchPoint().getTarget());
            assertSame(rect1, event.getTouchPoints().get(0).getTarget());
            PickResult res2 = event.getTouchPoint().getPickResult();
            PickResult res1 = event.getTouchPoints().get(0).getPickResult();
            switch(touched) {
                case 2:
                    assertNotNull(res1);
                    assertSame(rect1, res1.getIntersectedNode());
                    assertEquals(110, res1.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(110, res1.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);

                    assertNotNull(res2);
                    assertSame(rect2, res2.getIntersectedNode());
                    assertEquals(220, res2.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(220, res2.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);
                    break;
                case 4:
                    assertNotNull(res1);
                    assertSame(rect2, res1.getIntersectedNode());
                    assertEquals(220, res1.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(220, res1.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);

                    assertNotNull(res2);
                    assertSame(rect1, res2.getIntersectedNode());
                    assertEquals(110, res2.getIntersectedPoint().getX(), 0.00001);
                    assertEquals(110, res2.getIntersectedPoint().getY(), 0.00001);
                    assertEquals(0, res1.getIntersectedPoint().getZ(), 0.00001);
                    break;
                default:
                    fail("Wrong event delivered");
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 220, 220, 220, 220);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 1, 220, 220, 220, 220);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 2, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void testTouchPointsBelongsTo() {
        final Scene scene = createScene();

        final Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        final Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            assertTrue(event.getTouchPoint().belongsTo(rect1));
            assertTrue(event.getTouchPoint().belongsTo(scene.getRoot()));
            assertTrue(event.getTouchPoint().belongsTo(scene));
            assertFalse(event.getTouchPoint().belongsTo(rect2));

            assertFalse(event.getTouchPoints().get(1).belongsTo(rect1));
            assertTrue(event.getTouchPoints().get(1).belongsTo(scene.getRoot()));
            assertTrue(event.getTouchPoints().get(1).belongsTo(scene));
            assertTrue(event.getTouchPoints().get(1).belongsTo(rect2));
        });
        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            assertTrue(event.getTouchPoint().belongsTo(rect2));
            assertTrue(event.getTouchPoint().belongsTo(scene.getRoot()));
            assertTrue(event.getTouchPoint().belongsTo(scene));
            assertFalse(event.getTouchPoint().belongsTo(rect1));

            assertFalse(event.getTouchPoints().get(0).belongsTo(rect2));
            assertTrue(event.getTouchPoints().get(0).belongsTo(scene.getRoot()));
            assertTrue(event.getTouchPoints().get(0).belongsTo(scene));
            assertTrue(event.getTouchPoints().get(0).belongsTo(rect1));
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 220, 220, 220, 220);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 220, 220, 220, 220);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void shouldPickAndGrabTouchPoints() {
        Scene scene = createScene();
        final Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        final Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(150.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(150.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1150.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1155.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                case 3:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(250.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(250.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1250.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1255.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 2:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(260.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(260.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1260.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1265.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                case 4:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(160.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(160.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1160.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1165.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 3, 150, 155, 1150, 1155);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 4, 260, 265, 1260, 1265);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 3, 250, 255, 1250, 1255);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 4, 160, 165, 1160, 1165);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void ungrabShouldEnablePickingForTouchPoints() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(150.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(150.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1150.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1155.0, event.getTouchPoint().getScreenY(), 0.0001);
                    event.getTouchPoint().ungrab();
                    break;
                case 4:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(160.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(160.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1160.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1165.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 2:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(260.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(260.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1260.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1265.0, event.getTouchPoint().getScreenY(), 0.0001);
                    event.getTouchPoint().ungrab();
                    break;
                case 3:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(250.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(250.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1250.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1255.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 3, 150, 155, 1150, 1155);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 4, 260, 265, 1260, 1265);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 3, 250, 255, 1250, 1255);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 4, 160, 165, 1160, 1165);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void grabWithArgShouldAffectDelivery() {
        Scene scene = createScene();
        final Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        final Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(150.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(150.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1150.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1155.0, event.getTouchPoint().getScreenY(), 0.0001);
                    event.getTouchPoints().get(1).grab(rect1);
                    break;
                case 3:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(250.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(250.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1250.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1255.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                case 4:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(160.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(160.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1160.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1165.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 2:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(260.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(260.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1260.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1265.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 3, 150, 155, 1150, 1155);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 4, 260, 265, 1260, 1265);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 3, 250, 255, 1250, 1255);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 4, 160, 165, 1160, 1165);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void grabWithoutArgShouldAffectDelivery() {
        Scene scene = createScene();
        final Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        final Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        touched = 0;
        rect1.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(150.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(150.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(155.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1150.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1155.0, event.getTouchPoint().getScreenY(), 0.0001);
                    event.getTouchPoints().get(1).grab();
                    break;
                case 3:
                    Assert.assertEquals(1, event.getTouchPoint().getId());
                    Assert.assertEquals(250.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(250.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(255.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1250.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1255.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                case 4:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(160.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(160.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(165.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1160.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1165.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        rect2.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 2:
                    Assert.assertEquals(2, event.getTouchPoint().getId());
                    Assert.assertEquals(260.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(260.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(265.0, event.getTouchPoint().getSceneY(), 0.0001);
                    Assert.assertEquals(1260.0, event.getTouchPoint().getScreenX(), 0.0001);
                    Assert.assertEquals(1265.0, event.getTouchPoint().getScreenY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point delivery");
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 3, 150, 155, 1150, 1155);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 4, 260, 265, 1260, 1265);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 3, 250, 255, 1250, 1255);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 4, 160, 165, 1160, 1165);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        assertEquals(4, touched);
    }

    @Test
    public void pointsShouldBeTransformedCorrectly() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setTranslateX(15);
        rect.setTranslateY(5);

        touched = 0;
        rect.addEventHandler(TouchEvent.ANY, event -> {
            touched++;
            switch(touched) {
                case 1:
                    Assert.assertEquals(115.0, event.getTouchPoint().getX(), 0.0001);
                    Assert.assertEquals(125.0, event.getTouchPoint().getY(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoint().getSceneX(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoint().getSceneY(), 0.0001);
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                    Assert.assertEquals(115.0, event.getTouchPoints().get(0).getX(), 0.0001);
                    Assert.assertEquals(125.0, event.getTouchPoints().get(0).getY(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoints().get(0).getSceneX(), 0.0001);
                    Assert.assertEquals(130.0, event.getTouchPoints().get(0).getSceneY(), 0.0001);
                    Assert.assertEquals(125.0, event.getTouchPoints().get(1).getX(), 0.0001);
                    Assert.assertEquals(135.0, event.getTouchPoints().get(1).getY(), 0.0001);
                    Assert.assertEquals(140.0, event.getTouchPoints().get(1).getSceneX(), 0.0001);
                    Assert.assertEquals(140.0, event.getTouchPoints().get(1).getSceneY(), 0.0001);
                    break;
                default:
                    fail("Wrong touch point id " + event.getTouchPoint().getId());
            }
        });

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 130, 130, 130, 130);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.STATIONARY, 1, 130, 130, 130, 130);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 2, 140, 140, 140, 140);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 130, 130, 130, 130);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 2, 140, 140, 140, 140);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();


        assertEquals(5, touched);
    }

    @Test
    public void shouldIgnoreIndirectTouchEvents() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(TouchEvent.ANY, event -> fail("Delivered indirect touch event"));

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, false, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, false, true, false, true, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.RELEASED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowREOnWrongSmallId() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 2, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowREOnWrongLargeId() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.MOVED, 127, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowREOnBigTPNumber() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowREOnSmallTPNumber() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 2, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowREOnLostRelease() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1368, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();

        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventBegin(
                System.currentTimeMillis(), 1, true, false, false, false, false);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventNext(
                TouchPoint.State.PRESSED, 1, 110, 110, 110, 110);
        ((StubScene) SceneHelper.getPeer(scene)).getListener().touchEventEnd();
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
