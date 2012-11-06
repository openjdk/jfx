/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.scene.Node;

import org.junit.Test;

public class MouseEventTest {

    private final Node node1 = new TestNode(5);
    private final Node node2 = new TestNode(10);
    private final MouseEvent doubleclick = new MouseEvent(
            MouseEvent.MOUSE_CLICKED,
            11, 12, 13, 14, MouseButton.PRIMARY, 2,
            true, false, true, false, false, true, false, false, true);

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
