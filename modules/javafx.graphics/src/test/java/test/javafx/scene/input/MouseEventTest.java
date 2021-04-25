/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.Event;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MouseEventTest {

    private final Node node1 = new TestNode(5);
    private final Node node2 = new TestNode(10);
    private final MouseEvent doubleclick = new MouseEvent(
            MouseEvent.MOUSE_CLICKED,
            11, 12, 13, 14, MouseButton.PRIMARY, 2,
            true, false, true, false, false, true, false, false, true, false, null);

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);

        MouseEvent e = new MouseEvent(MouseEvent.MOUSE_DRAGGED,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, true, pickRes);

        assertSame(MouseEvent.MOUSE_DRAGGED, e.getEventType());
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
        assertTrue(e.isStillSincePress());
        assertFalse(e.isConsumed());
        assertSame(pickRes, e.getPickResult());
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());

        e = new MouseEvent(MouseEvent.MOUSE_DRAGGED,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true,
                false, true, false, pickRes);

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
        assertFalse(e.isStillSincePress());
    }

    @Test public void testToStringMatchingBrackets() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);

        MouseEvent e = new MouseEvent(MouseEvent.MOUSE_PRESSED,
                10, 20, 30, 40, MouseButton.PRIMARY, 1,
                false, false, false, false,
                true, false, false,
                false, false, false, pickRes);

        assertSame(pickRes, e.getPickResult());

        // Check the String returned by MouseEvent::toString method to ensure
        // that all of the square brackets are matching.
        // Note that this will fail if the toString method of any of the
        // components that make up the MouseEvent returns a String with
        // mismatched brackets, including the source and target
        // objects, the PickResult, the picked Node, or the picked Point3D.
        String str = e.toString();
        int bracketCount = 0;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case '[':
                    ++bracketCount;
                    break;
                case ']':
                    --bracketCount;
                    assertTrue("Too many closing brackets: " + str, bracketCount >= 0);
                    break;
            }
        }
        assertEquals("Too few closing brackets: " + str, 0, bracketCount);
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

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);

        MouseEvent e = new MouseEvent(n1, n2, MouseEvent.MOUSE_DRAGGED,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, true, pickRes);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(MouseEvent.MOUSE_DRAGGED, e.getEventType());
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
        assertTrue(e.isStillSincePress());
        assertFalse(e.isConsumed());
        assertSame(pickRes, e.getPickResult());

        e = new MouseEvent(n1, n2, MouseEvent.MOUSE_DRAGGED,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                false, true, true, false,
                true, false, true,
                false, true, false, pickRes);

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
        assertFalse(e.isStillSincePress());
    }

    @Test public void testLongConstructorWithoutPickResult() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        MouseEvent e = new MouseEvent(n1, n2, MouseEvent.MOUSE_DRAGGED,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                true, false, true, null);
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

    @Test public void testFullConstructorWithoutPickResult() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        MouseEvent e = new MouseEvent(n1, n2, MouseEvent.MOUSE_DRAGGED,
                10, 20, 30, 40, MouseButton.MIDDLE, 3,
                true, false, false, true,
                false, true, false,
                false, false, true,
                false, true, null);
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
    public void shouldCreateDoubleClickMouseEvent() {
        /* constructor called during initialization */
        assertEquals(11f, doubleclick.getX(), 0.0001);
        assertEquals(12f, doubleclick.getY(), 0.0001);
        assertEquals(11f, doubleclick.getSceneX(), 0.0001);
        assertEquals(12f, doubleclick.getSceneY(), 0.0001);
        assertEquals(13f, doubleclick.getScreenX(), 0.0001);
        assertEquals(14f, doubleclick.getScreenY(), 0.0001);
        assertSame(MouseButton.PRIMARY, doubleclick.getButton());
        assertEquals(2, doubleclick.getClickCount());
        assertTrue(doubleclick.isShiftDown());
        assertFalse(doubleclick.isControlDown());
        assertTrue(doubleclick.isAltDown());
        assertFalse(doubleclick.isMetaDown());
        assertTrue(doubleclick.isPopupTrigger());
        assertFalse(doubleclick.isPrimaryButtonDown());
        assertTrue(doubleclick.isMiddleButtonDown());
        assertFalse(doubleclick.isSecondaryButtonDown());
        assertFalse(doubleclick.isBackButtonDown());
        assertFalse(doubleclick.isForwardButtonDown());
        assertSame(MouseEvent.MOUSE_CLICKED, doubleclick.getEventType());
        assertSame(MouseEvent.NULL_SOURCE_TARGET, doubleclick.getSource());
    }

    @Test
    public void shouldCopyMouseEvent() {
        MouseEvent copy = doubleclick.copyFor(node1, node2);

        assertEquals(6f, copy.getX(), 0.0001);
        assertEquals(7f, copy.getY(), 0.0001);
        assertEquals(11f, copy.getSceneX(), 0.0001);
        assertEquals(12f, copy.getSceneY(), 0.0001);
        assertEquals(13f, copy.getScreenX(), 0.0001);
        assertEquals(14f, copy.getScreenY(), 0.0001);
        assertSame(doubleclick.getButton(), copy.getButton());
        assertEquals(doubleclick.getClickCount(), copy.getClickCount());
        assertEquals(doubleclick.isShiftDown(), copy.isShiftDown());
        assertEquals(doubleclick.isControlDown(), copy.isControlDown());
        assertEquals(doubleclick.isAltDown(), copy.isAltDown());
        assertEquals(doubleclick.isMetaDown(), copy.isMetaDown());
        assertEquals(doubleclick.isPopupTrigger(),
                     copy.isPopupTrigger());
        assertEquals(doubleclick.isPrimaryButtonDown(), copy.isPrimaryButtonDown());
        assertEquals(doubleclick.isMiddleButtonDown(), copy.isMiddleButtonDown());
        assertEquals(doubleclick.isSecondaryButtonDown(), copy.isSecondaryButtonDown());
        assertEquals(doubleclick.isBackButtonDown(), copy.isBackButtonDown());
        assertEquals(doubleclick.isForwardButtonDown(), copy.isForwardButtonDown());
        assertSame(doubleclick.getEventType(), copy.getEventType());
        assertSame(node1, copy.getSource());
        assertSame(node2, copy.getTarget());
    }

    @Test
    public void shouldCopyMouseEventWithEventId() {
        MouseEvent copy = doubleclick.copyFor(node1, node2,
                MouseEvent.MOUSE_ENTERED);

        assertEquals(6f, copy.getX(), 0.0001);
        assertEquals(7f, copy.getY(), 0.0001);
        assertEquals(11f, copy.getSceneX(), 0.0001);
        assertEquals(12f, copy.getSceneY(), 0.0001);
        assertEquals(13f, copy.getScreenX(), 0.0001);
        assertEquals(14f, copy.getScreenY(), 0.0001);
        assertSame(doubleclick.getButton(), copy.getButton());
        assertEquals(doubleclick.getClickCount(), copy.getClickCount());
        assertEquals(doubleclick.isShiftDown(), copy.isShiftDown());
        assertEquals(doubleclick.isControlDown(), copy.isControlDown());
        assertEquals(doubleclick.isAltDown(), copy.isAltDown());
        assertEquals(doubleclick.isMetaDown(), copy.isMetaDown());
        assertEquals(doubleclick.isPopupTrigger(),
                     copy.isPopupTrigger());
        assertEquals(doubleclick.isPrimaryButtonDown(), copy.isPrimaryButtonDown());
        assertEquals(doubleclick.isMiddleButtonDown(), copy.isMiddleButtonDown());
        assertEquals(doubleclick.isSecondaryButtonDown(), copy.isSecondaryButtonDown());
        assertEquals(doubleclick.isBackButtonDown(), copy.isBackButtonDown());
        assertEquals(doubleclick.isForwardButtonDown(), copy.isForwardButtonDown());
        assertSame(MouseEvent.MOUSE_ENTERED, copy.getEventType());
        assertSame(node1, copy.getSource());
        assertSame(node2, copy.getTarget());
    }

    @Test
    public void shouldCopyMouseEventWithNode() {
        MouseEvent temp = doubleclick.copyFor(node1, node2);
        MouseEvent copy = temp.copyFor(node2, node1);

        assertEquals(1f, copy.getX(), 0.0001);
        assertEquals(2f, copy.getY(), 0.0001);
        assertEquals(11f, copy.getSceneX(), 0.0001);
        assertEquals(12f, copy.getSceneY(), 0.0001);
        assertEquals(13f, copy.getScreenX(), 0.0001);
        assertEquals(14f, copy.getScreenY(), 0.0001);
        assertSame(doubleclick.getButton(), copy.getButton());
        assertEquals(doubleclick.getClickCount(), copy.getClickCount());
        assertEquals(doubleclick.isShiftDown(), copy.isShiftDown());
        assertEquals(doubleclick.isControlDown(), copy.isControlDown());
        assertEquals(doubleclick.isAltDown(), copy.isAltDown());
        assertEquals(doubleclick.isMetaDown(), copy.isMetaDown());
        assertEquals(doubleclick.isPopupTrigger(),
                     copy.isPopupTrigger());
        assertEquals(doubleclick.isPrimaryButtonDown(), copy.isPrimaryButtonDown());
        assertEquals(doubleclick.isMiddleButtonDown(), copy.isMiddleButtonDown());
        assertEquals(doubleclick.isSecondaryButtonDown(), copy.isSecondaryButtonDown());
        assertEquals(doubleclick.isBackButtonDown(), copy.isBackButtonDown());
        assertEquals(doubleclick.isForwardButtonDown(), copy.isForwardButtonDown());
        assertSame(doubleclick.getEventType(), copy.getEventType());
        assertSame(node2, copy.getSource());
        assertSame(node1, copy.getTarget());
    }

    @Test
    public void shouldCopyMouseEventWithDrag() {
        MouseEvent copy = doubleclick.copyFor(node1, node2);

        assertEquals(6f, copy.getX(), 0.0001);
        assertEquals(7f, copy.getY(), 0.0001);
        assertEquals(11f, copy.getSceneX(), 0.0001);
        assertEquals(12f, copy.getSceneY(), 0.0001);
        assertEquals(13f, copy.getScreenX(), 0.0001);
        assertEquals(14f, copy.getScreenY(), 0.0001);
        assertSame(doubleclick.getButton(), copy.getButton());
        assertEquals(doubleclick.getClickCount(), copy.getClickCount());
        assertEquals(doubleclick.isShiftDown(), copy.isShiftDown());
        assertEquals(doubleclick.isControlDown(), copy.isControlDown());
        assertEquals(doubleclick.isAltDown(), copy.isAltDown());
        assertEquals(doubleclick.isMetaDown(), copy.isMetaDown());
        assertEquals(doubleclick.isPopupTrigger(),
                     copy.isPopupTrigger());
        assertEquals(doubleclick.isPrimaryButtonDown(), copy.isPrimaryButtonDown());
        assertEquals(doubleclick.isMiddleButtonDown(), copy.isMiddleButtonDown());
        assertEquals(doubleclick.isSecondaryButtonDown(), copy.isSecondaryButtonDown());
        assertEquals(doubleclick.isBackButtonDown(), copy.isBackButtonDown());
        assertEquals(doubleclick.isForwardButtonDown(), copy.isForwardButtonDown());
        assertSame(doubleclick.getEventType(), copy.getEventType());
        assertSame(node1, copy.getSource());
        assertSame(node2, copy.getTarget());
    }


    @Test
    public void shouldGetNonEmptyDescription() {
        String s = doubleclick.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }


}
