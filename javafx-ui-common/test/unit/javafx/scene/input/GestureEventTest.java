/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.Event;
import javafx.geometry.Point3D;
import javafx.scene.shape.Rectangle;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GestureEventTest {

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        GestureEvent e = new GestureEvent(
                GestureEvent.ANY, 10, 20, 30, 40,
                false, true, false, true, false, true, pickRes);

        assertSame(GestureEvent.ANY, e.getEventType());
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
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isConsumed());

        e = new GestureEvent(
                GestureEvent.ANY, 10, 20, 30, 40,
                true, false, true, false, true, false, null);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isDirect());
        assertFalse(e.isInertia());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        GestureEvent e = new GestureEvent(
                GestureEvent.ANY, 10, 20, 30, 40,
                false, true, false, true, false, true, null);

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
        GestureEvent e = new GestureEvent(n1, n2,
                GestureEvent.ANY, 10, 20, 30, 40,
                false, true, false, true, false, true, pickRes);
        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(GestureEvent.ANY, e.getEventType());
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
        assertFalse(e.isConsumed());

        e = new GestureEvent(n1, n2,
                GestureEvent.ANY, 10, 20, 30, 40,
                true, false, true, false, true, false, null);
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
        GestureEvent e = new GestureEvent(n1, n2,
                GestureEvent.ANY, 10, 20, 30, 40,
                false, true, false, true, false, true, null);

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

}
