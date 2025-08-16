/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import test.com.sun.javafx.test.MouseEventGenerator;
import javafx.event.Event;
import javafx.geometry.Point3D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MouseDragEventTest {

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        Rectangle gsrc = new Rectangle();

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);

        MouseDragEvent e = new MouseDragEvent(MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, pickRes, gsrc);

        assertSame(MouseDragEvent.MOUSE_DRAG_OVER, e.getEventType());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertSame(MouseButton.MIDDLE, e.getButton());
        assertEquals(3, e.getClickCount());
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isPrimaryButtonDown());
        assertTrue(e.isMiddleButtonDown());
        assertFalse(e.isSecondaryButtonDown());
        assertFalse(e.isBackButtonDown());
        assertFalse(e.isForwardButtonDown());
        assertTrue(e.isSynthesized());
        assertFalse(e.isPopupTrigger());
        assertSame(gsrc, e.getGestureSource());
        assertFalse(e.isConsumed());
        assertSame(pickRes, e.getPickResult());
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());

        e = new MouseDragEvent(MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true,
                false, true, pickRes, gsrc);

        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isPrimaryButtonDown());
        assertFalse(e.isMiddleButtonDown());
        assertTrue(e.isSecondaryButtonDown());
        assertFalse(e.isSynthesized());
        assertTrue(e.isPopupTrigger());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        MouseDragEvent e = new MouseDragEvent(MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, null, new Rectangle());
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
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        Rectangle gsrc = new Rectangle();

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);

        MouseDragEvent e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, pickRes, gsrc);

        assertSame(MouseDragEvent.MOUSE_DRAG_OVER, e.getEventType());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertSame(MouseButton.MIDDLE, e.getButton());
        assertEquals(3, e.getClickCount());
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isPrimaryButtonDown());
        assertTrue(e.isMiddleButtonDown());
        assertFalse(e.isSecondaryButtonDown());
        assertFalse(e.isBackButtonDown());
        assertFalse(e.isForwardButtonDown());
        assertTrue(e.isSynthesized());
        assertFalse(e.isPopupTrigger());
        assertSame(gsrc, e.getGestureSource());
        assertFalse(e.isConsumed());
        assertSame(pickRes, e.getPickResult());
        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());

        e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true,
                false, true, pickRes, gsrc);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isPrimaryButtonDown());
        assertFalse(e.isMiddleButtonDown());
        assertTrue(e.isSecondaryButtonDown());
        assertFalse(e.isSynthesized());
        assertTrue(e.isPopupTrigger());
    }

    @Test public void testLongConstructorWithoutPickResult() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        MouseDragEvent e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, null, new Rectangle());
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

    @Test public void testFullConstructor() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        Rectangle gsrc = new Rectangle();

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);

        MouseDragEvent e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false, true, true,
                true, false, pickRes, gsrc);

        assertSame(MouseDragEvent.MOUSE_DRAG_OVER, e.getEventType());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertSame(MouseButton.MIDDLE, e.getButton());
        assertEquals(3, e.getClickCount());
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertFalse(e.isAltDown());
        assertTrue(e.isMetaDown());
        assertFalse(e.isPrimaryButtonDown());
        assertTrue(e.isMiddleButtonDown());
        assertFalse(e.isSecondaryButtonDown());
        assertTrue(e.isBackButtonDown());
        assertTrue(e.isForwardButtonDown());
        assertTrue(e.isSynthesized());
        assertFalse(e.isPopupTrigger());
        assertSame(gsrc, e.getGestureSource());
        assertFalse(e.isConsumed());
        assertSame(pickRes, e.getPickResult());
        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());

        e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true, true, false,
                false, true, pickRes, gsrc);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isPrimaryButtonDown());
        assertFalse(e.isMiddleButtonDown());
        assertTrue(e.isSecondaryButtonDown());
        assertTrue(e.isBackButtonDown());
        assertFalse(e.isForwardButtonDown());
        assertFalse(e.isSynthesized());
        assertTrue(e.isPopupTrigger());

        e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true, false, true,
                false, true, pickRes, gsrc);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isPrimaryButtonDown());
        assertFalse(e.isMiddleButtonDown());
        assertTrue(e.isSecondaryButtonDown());
        assertFalse(e.isBackButtonDown());
        assertTrue(e.isForwardButtonDown());
        assertFalse(e.isSynthesized());
        assertTrue(e.isPopupTrigger());

        e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true, false, false,
                false, true, pickRes, gsrc);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertFalse(e.isShiftDown());
        assertTrue(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isPrimaryButtonDown());
        assertFalse(e.isMiddleButtonDown());
        assertTrue(e.isSecondaryButtonDown());
        assertFalse(e.isBackButtonDown());
        assertFalse(e.isForwardButtonDown());
        assertFalse(e.isSynthesized());
        assertTrue(e.isPopupTrigger());
    }

    @Test public void testFullConstructorWithoutPickResult() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        MouseDragEvent e = new MouseDragEvent(n1, n2, MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false, true, true,
                true, false, null, new Rectangle());
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
    public void fullPDRShouldNotStartAutomatically() {
        World w = new World(false, false);

        // Press and move
        w.event(MouseEvent.MOUSE_PRESSED, 400, 150);
        w.event(MouseEvent.MOUSE_PRESSED, 410, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 150, 150);
        w.event(MouseEvent.MOUSE_RELEASED, 160, 150);

        w.getScene().getAny().assertNotCalled();
        w.getSource().getAny().assertNotCalled();
        w.getTarget().getAny().assertNotCalled();

        w.clear();
    }

    @Test
    public void activatingFullPDRShouldCauseEnteredEvents() {
        World w = new World(false, false);

        // Press
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);

        w.getSource().getDragEntered().assertNotCalled();
        w.getSource().getAny().assertNotCalled();

        // Move - initiates the gesture
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 151);

        // Move - first event in the gesture, causes all the entered events
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragEntered().assertCalled();
        w.getSource().getDragEntered().assertCoords(60, 50);
        w.getSource().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getSource().getDragEnteredTarget().assertCalled();
        w.getSource().getDragEnteredTarget().assertCoords(60, 50);
        w.getSource().getDragEnteredTarget().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragEntered().assertNotCalled();
        w.getSourceParent().getDragEntered().assertCalled();
        w.getSourceParent().getDragEntered().assertCoords(160, 150);
        w.getSourceParent().getDragEnteredTarget().assertCalled();
        w.getSourceParent().getDragEnteredTarget().assertCoords(160, 150);
        w.getScene().getDragEntered().assertCalled();
        w.getScene().getDragEntered().assertCoords(160, 150);

        w.event(MouseEvent.MOUSE_RELEASED, 160, 150);

        w.clear();
    }

    @Test
    public void activatingFullPDRShouldCauseEnteredEventsRepeatedly() {
        World w = new World(false, false);

        // First time
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);
        w.event(MouseEvent.MOUSE_RELEASED, 160, 150);
        w.clear();

        //Second time
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 151);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragEntered().assertCalled();
        w.getSource().getDragEntered().assertCoords(60, 50);
        w.getSource().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getSource().getDragEnteredTarget().assertCalled();
        w.getSource().getDragEnteredTarget().assertCoords(60, 50);
        w.getSource().getDragEnteredTarget().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragEntered().assertNotCalled();
        w.getSourceParent().getDragEntered().assertCalled();
        w.getSourceParent().getDragEntered().assertCoords(160, 150);
        w.getSourceParent().getDragEnteredTarget().assertCalled();
        w.getSourceParent().getDragEnteredTarget().assertCoords(160, 150);
        w.getScene().getDragEntered().assertCalled();
        w.getScene().getDragEntered().assertCoords(160, 150);

        w.event(MouseEvent.MOUSE_RELEASED, 160, 150);

        w.clear();
    }

    @Test
    public void activatingFullPDRShouldCauseEnteredEventsForUnderlyingNode() {
        World w = new World(true, true);

        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 151);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragEntered().assertNotCalled();
        w.getBelowSource().getDragEntered().assertCalled();
        w.getBelowSource().getDragEntered().assertCoords(160, 150);
        w.getBelowSource().getDragEntered().assertGestureSource(w.getSource().getNode());

        w.event(MouseEvent.MOUSE_RELEASED, 160, 150);

        w.clear();
    }


    @Test
    public void fullPDRShouldProduceDragOverEvents() {
        World w = new World(false, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 159, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragOver().assertCalled();
        w.getSource().getDragOver().assertCoords(60, 50);
        w.getSource().getDragOver().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragOver().assertNotCalled();
        w.getSourceParent().getDragOver().assertCalled();
        w.getSourceParent().getDragOver().assertCoords(160, 150);
        w.getScene().getDragOver().assertCalled();
        w.getScene().getDragOver().assertCoords(160, 150);

        w.event(MouseEvent.MOUSE_RELEASED, 160, 150);

        w.clear();
    }

    @Test
    public void endingFullPDRShouldProduceReleaseAndDoneEvents() {
        World w = new World(false, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragReleased().assertNotCalled();
        w.getScene().getDragReleased().assertNotCalled();

        w.event(MouseEvent.MOUSE_RELEASED, 170, 150);

        w.getSource().getDragReleased().assertCalled();
        w.getSource().getDragReleased().assertCoords(70, 50);
        w.getSource().getDragReleased().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragReleased().assertNotCalled();
        w.getSourceParent().getDragReleased().assertCalled();
        w.getSourceParent().getDragReleased().assertCoords(170, 150);
        w.getScene().getDragReleased().assertCalled();
        w.getScene().getDragReleased().assertCoords(170, 150);

        w.getSource().getDragDone().assertCalled();
        w.getBelowSource().getDragDone().assertNotCalled();
        w.getScene().getDragDone().assertCalled();
        w.getScene().getDragDone().assertGestureSource(w.getSource().getNode());

        w.clear();
    }

    @Test
    public void endingFullPDRShouldCuaseExitedEvents() {
        World w = new World(false, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragExited().assertNotCalled();
        w.getScene().getDragExited().assertNotCalled();

        w.event(MouseEvent.MOUSE_RELEASED, 170, 150);

        w.getSource().getDragExited().assertCalled();
        w.getSource().getDragExited().assertCoords(70, 50);
        w.getSource().getDragExited().assertGestureSource(w.getSource().getNode());
        w.getSource().getDragExitedTarget().assertCalled();
        w.getSource().getDragExitedTarget().assertCoords(70, 50);
        w.getSource().getDragExitedTarget().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragExited().assertNotCalled();
        w.getSourceParent().getDragExited().assertCalled();
        w.getSourceParent().getDragExited().assertCoords(170, 150);
        w.getSourceParent().getDragExitedTarget().assertCalled();
        w.getSourceParent().getDragExitedTarget().assertCoords(170, 150);
        w.getScene().getDragExited().assertCalled();
        w.getScene().getDragExited().assertCoords(170, 150);

        w.clear();
    }

    @Test
    public void draggedNodeGetsAllTheEvents() {
        World w = new World(true, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        // Move to target (source is dragged)
        w.event(MouseEvent.MOUSE_DRAGGED, 199, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 248, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 297, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 346, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 395, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 410, 150);

        w.getTarget().getAny().assertNotCalled();
        w.getBelowTarget().getAny().assertNotCalled();
        w.getTargetParent().getAny().assertNotCalled();
        w.getBelowSource().getAny().assertNotCalled();
        w.getSource().getDragOver().assertCalled();
        w.getSource().getDragOver().assertCoords(50, 50);
        w.getSource().getDragOver().assertGestureSource(w.getSource().getNode());

        w.clear();

        w.event(MouseEvent.MOUSE_RELEASED, 410, 150);

        w.getTarget().getAny().assertNotCalled();
        w.getBelowTarget().getAny().assertNotCalled();
        w.getTargetParent().getAny().assertNotCalled();
        w.getBelowSource().getAny().assertNotCalled();
        w.getSource().getDragReleased().assertCalled();
        w.getSource().getDragReleased().assertCoords(50, 50);
        w.getSource().getDragReleased().assertGestureSource(w.getSource().getNode());
        w.getSource().getDragDone().assertCalled();
    }

    @Test
    public void fullPDRShouldPickAfterMouseEventHandlers() {
        World w = new World(true, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        // Move to target (jumps temporarily out of the source)
        w.event(MouseEvent.MOUSE_DRAGGED, 410, 150);

        w.getTarget().getAny().assertNotCalled();
        w.getBelowTarget().getAny().assertNotCalled();
        w.getTargetParent().getAny().assertNotCalled();
        w.getBelowSource().getAny().assertNotCalled();
        w.getSource().getDragOver().assertCalled();
        w.getSource().getDragOver().assertCoords(50, 50);
        w.getSource().getDragOver().assertGestureSource(w.getSource().getNode());

        w.clear();

        w.event(MouseEvent.MOUSE_RELEASED, 410, 150);

        w.getTarget().getAny().assertNotCalled();
        w.getBelowTarget().getAny().assertNotCalled();
        w.getTargetParent().getAny().assertNotCalled();
        w.getBelowSource().getAny().assertNotCalled();
        w.getSource().getDragReleased().assertCalled();
        w.getSource().getDragReleased().assertCoords(50, 50);
        w.getSource().getDragReleased().assertGestureSource(w.getSource().getNode());

        w.getSource().getDragDone().assertCalled();
        w.getSource().getDragDone().assertGestureSource(w.getSource().getNode());
    }

    @Test
    public void transparentNodeShouldBeDraggable() {
        World w = new World(true, true);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        // Move to target (source is dragged)
        w.event(MouseEvent.MOUSE_DRAGGED, 410, 150);

        w.getSource().assertTranslate(360, 100);

        w.event(MouseEvent.MOUSE_RELEASED, 410, 150);
    }

    @Test
    public void transparentNodeShouldAllowUnderlyingTarget() {
        World w = new World(true, true);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        // Move to target (source is dragged)
        w.event(MouseEvent.MOUSE_DRAGGED, 199, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 248, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 297, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 346, 150);

        w.clear();

        w.event(MouseEvent.MOUSE_DRAGGED, 395, 150);

        w.getSource().getAny().assertNotCalled();
        w.getBelowTarget().getDragExited().assertCalled();
        w.getTarget().getDragEntered().assertCalled();
        w.getTarget().getDragEntered().assertCoords(45, 50);
        w.getTarget().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getTarget().getDragOver().assertCalled();
        w.getTarget().getDragOver().assertCoords(45, 50);
        w.getTarget().getDragOver().assertGestureSource(w.getSource().getNode());

        w.event(MouseEvent.MOUSE_RELEASED, 410, 150);

        w.getTarget().getDragReleased().assertCalled();
        w.getTarget().getDragReleased().assertCoords(60, 50);
        w.getTarget().getDragReleased().assertGestureSource(w.getSource().getNode());
        w.getTarget().getDragDone().assertNotCalled();
        w.getSource().getDragDone().assertCalled();
        w.getSource().getAny().clear();
    }

    @Test
    public void gestureSourceShouldBeGivenByStartFullDragCall() {
        World w = new World(false, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 225, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 135, 150);

        w.clear();

        w.event(MouseEvent.MOUSE_DRAGGED, 400, 150);

        w.getTarget().getDragOver().assertGestureSource(w.sourceParent.getNode());
    }

    @Test
    public void fullPDRShouldRecognizeLeavingScene() {
        World w = new World(false, false);

        // Initiate the gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 151);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getScene().getDragEntered().assertCalled();

        w.clear();

        // Move inside the scene
        w.event(MouseEvent.MOUSE_DRAGGED, 1, 1);
        w.getScene().getDragExited().assertNotCalled();

        // Leave the scene
        w.event(MouseEvent.MOUSE_DRAGGED, 1000, 1);
        w.getScene().getDragExited().assertCalled();

        w.clear();

        // Move outside of scene
        w.event(MouseEvent.MOUSE_DRAGGED, -10, 10);
        w.getScene().getAny().assertNotCalled();

        // Move back to scene
        w.event(MouseEvent.MOUSE_DRAGGED, 10, 10);
        w.getScene().getDragEntered().assertCalled();

        // Move out again
        w.event(MouseEvent.MOUSE_DRAGGED, -5, -5);
        w.getScene().getDragExited().assertCalled();
        w.clear();

        // Release there
        w.event(MouseEvent.MOUSE_RELEASED, -5, -5);
        w.getScene().getDragReleased().assertNotCalled();

        // Done called even outside the scene
        w.getScene().getDragDone().assertCalled();
    }

    @Test
    public void testFullGesture() {
        World w = new World(false, false);

        // Initiate gesture
        w.event(MouseEvent.MOUSE_PRESSED, 150, 150);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 151);
        w.event(MouseEvent.MOUSE_DRAGGED, 160, 150);

        w.getSource().getDragEntered().assertCalled();
        w.getSource().getDragEntered().assertCoords(60, 50);
        w.getSource().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getSource().getDragEnteredTarget().assertCalled();
        w.getSource().getDragEnteredTarget().assertCoords(60, 50);
        w.getSource().getDragEnteredTarget().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragEntered().assertNotCalled();
        w.getSourceParent().getDragEntered().assertCalled();
        w.getSourceParent().getDragEntered().assertCoords(160, 150);
        w.getSourceParent().getDragEnteredTarget().assertCalled();
        w.getSourceParent().getDragEnteredTarget().assertCoords(160, 150);
        w.getScene().getDragEntered().assertCalled();

        w.clear();

        // Move to covered node
        w.event(MouseEvent.MOUSE_DRAGGED, 225, 150);

        w.getSource().getDragExited().assertCalled();
        w.getSource().getDragExited().assertCoords(125, 50);
        w.getSource().getDragExited().assertGestureSource(w.getSource().getNode());
        w.getSource().getDragExitedTarget().assertCalled();
        w.getSource().getDragExitedTarget().assertCoords(125, 50);
        w.getSource().getDragExitedTarget().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragEntered().assertCalled();
        w.getBelowSource().getDragEntered().assertCoords(225, 150);
        w.getBelowSource().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getBelowSource().getDragExitedTarget().assertNotCalled();
        w.getSourceParent().getDragEntered().assertNotCalled();
        w.getSourceParent().getDragExited().assertNotCalled();
        w.getSourceParent().getDragEnteredTarget().assertCalled();
        w.getSourceParent().getDragExitedTarget().assertCalled();
        w.getScene().getDragEntered().assertNotCalled();
        w.getScene().getDragExited().assertNotCalled();
        w.getScene().getDragEnteredTarget().assertCalled();
        w.getScene().getDragExitedTarget().assertCalled();

        w.clear();

        // Move a bit
        w.event(MouseEvent.MOUSE_DRAGGED, 226, 150);

        w.getSource().getDragOver().assertNotCalled();
        w.getBelowSource().getDragOver().assertCalled();
        w.getSourceParent().getDragOver().assertCalled();
        w.getSourceParent().getDragOver().assertCoords(226, 150);
        w.getSourceParent().getDragOver().assertGestureSource(w.getSource().getNode());
        w.getScene().getDragOver().assertCalled();
        w.getScene().getDragEnteredTarget().assertNotCalled();
        w.getScene().getDragExitedTarget().assertNotCalled();
        w.getScene().getDragReleased().assertNotCalled();
        w.getScene().getDragDone().assertNotCalled();

        w.clear();

        // Move to node covered by target
        w.event(MouseEvent.MOUSE_DRAGGED, 325, 150);

        w.getSource().getAny().assertNotCalled();
        w.getBelowSource().getDragExited().assertCalled();
        w.getSourceParent().getDragExited().assertCalled();
        w.getBelowTarget().getDragEntered().assertCalled();
        w.getBelowTarget().getDragEntered().assertCoords(325, 150);
        w.getBelowTarget().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getTargetParent().getDragEntered().assertCalled();
        w.getBelowTarget().getDragOver().assertCalled();
        w.getBelowTarget().getDragOver().assertCoords(325,150);
        w.getBelowTarget().getDragOver().assertGestureSource(w.getSource().getNode());
        w.getScene().getDragEntered().assertNotCalled();
        w.getScene().getDragExited().assertNotCalled();
        w.getScene().getDragEnteredTarget().assertCalled();
        w.getScene().getDragExitedTarget().assertCalled();

        w.clear();

        // Move to the target
        w.event(MouseEvent.MOUSE_DRAGGED, 400, 150);

        w.getSource().getAny().assertNotCalled();
        w.getSourceParent().getAny().assertNotCalled();
        w.getBelowSource().getAny().assertNotCalled();
        w.getBelowTarget().getDragExited().assertCalled();
        w.getBelowTarget().getDragOver().assertNotCalled();
        w.getTarget().getDragEntered().assertCalled();
        w.getTarget().getDragEntered().assertCoords(50, 50);
        w.getTarget().getDragEntered().assertGestureSource(w.getSource().getNode());
        w.getTarget().getDragOver().assertCalled();
        w.getTarget().getDragOver().assertCoords(50, 50);
        w.getTarget().getDragOver().assertGestureSource(w.getSource().getNode());
        w.getTargetParent().getDragEntered().assertNotCalled();
        w.getTargetParent().getDragExited().assertNotCalled();
        w.getTargetParent().getDragEnteredTarget().assertCalled();
        w.getTargetParent().getDragExitedTarget().assertCalled();

        w.clear();

        // Release
        w.event(MouseEvent.MOUSE_RELEASED, 410, 150);

        w.getBelowSource().getAny().assertNotCalled();
        w.getBelowTarget().getAny().assertNotCalled();
        w.getSource().getDragDone().assertCalled();
        w.getSourceParent().getDragDone().assertCalled();
        w.getScene().getDragDone().assertCalled();
        w.getTarget().getDragReleased().assertCalled();
        w.getTarget().getDragReleased().assertCoords(60, 50);
        w.getTarget().getDragReleased().assertGestureSource(w.getSource().getNode());
        w.getTargetParent().getDragReleased().assertCalled();
        w.getTargetParent().getDragReleased().assertCoords(410, 150);
        w.getTargetParent().getDragReleased().assertGestureSource(w.getSource().getNode());
        w.getScene().getDragReleased().assertCalled();
        w.getScene().getDragReleased().assertCoords(410, 150);
        w.getScene().getDragReleased().assertGestureSource(w.getSource().getNode());
        w.getTarget().getDragExited().assertCalled();
        w.getTarget().getDragExited().assertCoords(60, 50);
        w.getTarget().getDragExited().assertGestureSource(w.getSource().getNode());
        w.getTargetParent().getDragExited().assertCalled();
        w.getTargetParent().getDragExited().assertCoords(410, 150);
        w.getTargetParent().getDragExited().assertGestureSource(w.getSource().getNode());
        w.getScene().getDragExited().assertCalled();
        w.getScene().getDragExited().assertCoords(410, 150);
        w.getScene().getDragExited().assertGestureSource(w.getSource().getNode());

        w.clear();
    }

    @Test
    public void shouldGetNonEmptyDescription() {
        String s = new MouseDragEvent(MouseDragEvent.MOUSE_DRAG_OVER,
                10, 20, 20, 30, MouseButton.NONE, 0, true, true, true, true,
                true, true, true, false, false, null, null)
                .toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private class World {
        private HandledNode scene;
        private HandledNode source;
        private HandledNode belowSource;
        private HandledNode sourceParent;
        private HandledNode target;
        private HandledNode belowTarget;
        private HandledNode targetParent;
        private HandledNode root;
        private Scene sceneNode;

        private double anchorX, anchorY;
        private MouseEventGenerator generator;


        public World(boolean sourceDragged, final boolean sourceMouseTransparent) {

            generator = new MouseEventGenerator();

            final Group rootNode = new Group();
            sceneNode = new Scene(rootNode, 550, 300);

            final Group sourceParentNode = new Group();
            final Rectangle belowSourceNode = new Rectangle(50, 50, 200, 200);
            final Rectangle sourceNode = new Rectangle(100, 100);
            sourceNode.setTranslateX(100);
            sourceNode.setTranslateY(100);

            final Group targetParentNode = new Group();
            final Rectangle belowTargetNode = new Rectangle(300, 50, 200, 200);
            final Rectangle targetNode = new Rectangle(100, 100);
            targetNode.setTranslateX(350);
            targetNode.setTranslateY(100);

            sourceParentNode.getChildren().addAll(belowSourceNode, sourceNode);
            targetParentNode.getChildren().addAll(belowTargetNode, targetNode);
            rootNode.getChildren().addAll(targetParentNode, sourceParentNode);

            scene = new HandledNode(sceneNode);
            source = new HandledNode(sourceNode);
            belowSource = new HandledNode(belowSourceNode);
            sourceParent = new HandledNode(sourceParentNode);
            target = new HandledNode(targetNode);
            belowTarget = new HandledNode(belowTargetNode);
            targetParent = new HandledNode(targetParentNode);
            root = new HandledNode(rootNode);

            sourceNode.setOnDragDetected(event -> sourceNode.startFullDrag());

            belowSourceNode.setOnDragDetected(event -> sourceParentNode.startFullDrag());

            if (sourceDragged) {
                sourceNode.setOnMousePressed(event -> {
                    anchorX = event.getSceneX();
                    anchorY = event.getSceneY();
                    if (sourceMouseTransparent) {
                        sourceNode.setMouseTransparent(true);
                    }
                });

                sourceNode.setOnMouseDragged(event -> {
                    sourceNode.setTranslateX(sourceNode.getTranslateX()
                            + event.getSceneX() - anchorX);
                    sourceNode.setTranslateY(sourceNode.getTranslateY()
                            + event.getSceneY() - anchorY);
                    anchorX = event.getSceneX();
                    anchorY = event.getSceneY();
                });

                sourceNode.setOnMouseReleased(event -> {
                    if (sourceMouseTransparent) {
                        sourceNode.setMouseTransparent(false);
                    }
                });
            }

            Stage stage = new Stage();
            stage.setScene(sceneNode);
            stage.show();
        }

        public HandledNode getBelowSource() {
            return belowSource;
        }

        public HandledNode getBelowTarget() {
            return belowTarget;
        }

        public HandledNode getRoot() {
            return root;
        }

        public HandledNode getScene() {
            return scene;
        }

        public HandledNode getSource() {
            return source;
        }

        public HandledNode getSourceParent() {
            return sourceParent;
        }

        public HandledNode getTarget() {
            return target;
        }

        public HandledNode getTargetParent() {
            return targetParent;
        }

        public void event(EventType<MouseEvent> type, double x, double y) {
            SceneHelper.processMouseEvent(sceneNode,
                    generator.generateMouseEvent(type, x, y));
        }

        public void clear() {
            scene.clear();
            source.clear();
            belowSource.clear();
            sourceParent.clear();
            target.clear();
            belowTarget.clear();
            targetParent.clear();
            root.clear();
        }

    }

    private class HandledNode {
        private Node node;
        private Handler dragOver;
        private Handler dragReleased;
        private Handler dragDone;
        private Handler dragEntered;
        private Handler dragExited;
        private Handler dragEnteredTarget;
        private Handler dragExitedTarget;
        private Handler any;

        public HandledNode(Node node) {
            this.node = node;
            node.setOnMouseDragOver(dragOver = new Handler());
            node.setOnMouseDragReleased(dragReleased = new Handler());
            node.setOnMouseDragDone(dragDone = new Handler());
            node.setOnMouseDragEntered(dragEntered = new Handler());
            node.setOnMouseDragExited(dragExited = new Handler());
            node.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED_TARGET,
                    dragEnteredTarget = new Handler());
            node.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED_TARGET,
                    dragExitedTarget = new Handler());
            node.addEventHandler(MouseDragEvent.ANY,
                    any = new Handler());
        }

        public HandledNode(Scene scene) {
            this.node = null;
            scene.setOnMouseDragOver(dragOver = new Handler());
            scene.setOnMouseDragReleased(dragReleased = new Handler());
            scene.setOnMouseDragDone(dragDone = new Handler());
            scene.setOnMouseDragEntered(dragEntered = new Handler());
            scene.setOnMouseDragExited(dragExited = new Handler());
            scene.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED_TARGET,
                    dragEnteredTarget = new Handler());
            scene.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED_TARGET,
                    dragExitedTarget = new Handler());
            scene.addEventHandler(MouseDragEvent.ANY,
                    any = new Handler());
        }

        public Handler getAny() {
            return any;
        }

        public Handler getDragEntered() {
            return dragEntered;
        }

        public Handler getDragEnteredTarget() {
            return dragEnteredTarget;
        }

        public Handler getDragExited() {
            return dragExited;
        }

        public Handler getDragExitedTarget() {
            return dragExitedTarget;
        }

        public Handler getDragOver() {
            return dragOver;
        }

        public Handler getDragReleased() {
            return dragReleased;
        }

        public Handler getDragDone() {
            return dragDone;
        }

        public void assertTranslate(double x, double y) {
            assertEquals(x, node.getTranslateX(), 0.0001);
            assertEquals(y, node.getTranslateY(), 0.0001);
        }

        public Node getNode() {
            return node;
        }

        public void clear() {
            dragOver.clear();
            dragReleased.clear();
            dragDone.clear();
            dragEntered.clear();
            dragExited.clear();
            dragEnteredTarget.clear();
            dragExitedTarget.clear();
            any.clear();
        }
    }

    private class Handler implements EventHandler<MouseDragEvent> {

        private boolean called = false;
        private double x = 0.0;
        private double y = 0.0;
        private EventType type = null;
        private Object gestureSource = null;

        @Override public void handle(MouseDragEvent event) {
            called = true;
            x = event.getX();
            y = event.getY();
            type = event.getEventType();
            gestureSource = event.getGestureSource();
        }

        public void assertCalled() {
            assertTrue(called);
        }

        public void assertNotCalled() {
            assertFalse(called);
        }

        public void assertCoords(double x, double y) {
            assertEquals(x, this.x, 0.0001);
            assertEquals(y, this.y, 0.0001);
        }

        public void assertType(EventType type) {
            assertSame(type, this.type);
        }

        public void assertGestureSource(Object gestureSource) {
            assertSame(gestureSource, this.gestureSource);
        }

        public void clear() {
            called = false;
            x = 0.0;
            y = 0.0;
            type = null;
            gestureSource = null;
        }
    }
}
