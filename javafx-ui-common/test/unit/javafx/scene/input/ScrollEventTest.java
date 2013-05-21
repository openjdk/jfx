/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class ScrollEventTest {

    private boolean scrolled;
    private boolean scrolled2;
    private PickResult pickRes;

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle();
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);

        pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        ScrollEvent e = new ScrollEvent(
                ScrollEvent.SCROLL_STARTED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                2, 3, 4, 5, ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 6,
                ScrollEvent.VerticalTextScrollUnits.PAGES, 7, 8, pickRes);

        assertSame(ScrollEvent.SCROLL_STARTED, e.getEventType());
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
        assertEquals(2.0, e.getDeltaX(), 10e-20);
        assertEquals(3.0, e.getDeltaY(), 10e-20);
        assertEquals(4.0, e.getTotalDeltaX(), 10e-20);
        assertEquals(5.0, e.getTotalDeltaY(), 10e-20);
        assertSame(ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, e.getTextDeltaXUnits());
        assertEquals(6.0, e.getTextDeltaX(), 10e-20);
        assertSame(ScrollEvent.VerticalTextScrollUnits.PAGES, e.getTextDeltaYUnits());
        assertEquals(7.0, e.getTextDeltaY(), 10e-20);
        assertEquals(8.0, e.getTouchCount(), 10e-20);
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isConsumed());

        e = new ScrollEvent(
                ScrollEvent.SCROLL_STARTED, 10, 20, 30, 40,
                true, false, true, false, true, false,
                2, 3, 4, 5, ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 6,
                ScrollEvent.VerticalTextScrollUnits.PAGES, 7, 8, pickRes);
        assertTrue(e.isShiftDown());
        assertFalse(e.isControlDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isMetaDown());
        assertTrue(e.isDirect());
        assertFalse(e.isInertia());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        ScrollEvent e = new ScrollEvent(
                ScrollEvent.SCROLL_STARTED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                2, 3, 4, 5, ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 6,
                ScrollEvent.VerticalTextScrollUnits.PAGES, 7, 8, null);

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
        ScrollEvent e = new ScrollEvent(n1, n2,
                ScrollEvent.SCROLL_STARTED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                2, 3, 4, 5, ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 6,
                ScrollEvent.VerticalTextScrollUnits.PAGES, 7, 8, pickRes);

        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(ScrollEvent.SCROLL_STARTED, e.getEventType());
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
        assertEquals(2.0, e.getDeltaX(), 10e-20);
        assertEquals(3.0, e.getDeltaY(), 10e-20);
        assertEquals(4.0, e.getTotalDeltaX(), 10e-20);
        assertEquals(5.0, e.getTotalDeltaY(), 10e-20);
        assertSame(ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, e.getTextDeltaXUnits());
        assertEquals(6.0, e.getTextDeltaX(), 10e-20);
        assertSame(ScrollEvent.VerticalTextScrollUnits.PAGES, e.getTextDeltaYUnits());
        assertEquals(7.0, e.getTextDeltaY(), 10e-20);
        assertEquals(8.0, e.getTouchCount(), 10e-20);
        assertFalse(e.isConsumed());

        e = new ScrollEvent(n1, n2,
                ScrollEvent.SCROLL_STARTED, 10, 20, 30, 40,
                true, false, true, false, true, false,
                2, 3, 4, 5, ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 6,
                ScrollEvent.VerticalTextScrollUnits.PAGES, 7, 8, pickRes);
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
        ScrollEvent e = new ScrollEvent(n1, n2,
                ScrollEvent.SCROLL_STARTED, 10, 20, 30, 40,
                false, true, false, true, false, true,
                2, 3, 4, 5, ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 6,
                ScrollEvent.VerticalTextScrollUnits.PAGES, 7, 8, null);

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
    public void shouldDeliverScrollEventToPickedNode() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                50, 50, 50, 50, false, false, false, false, false, false);
        
        assertFalse(scrolled);

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);
        
        assertTrue(scrolled);
    }
    
    @Test
    public void shouldUseMultiplier() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(66.0, event.getDeltaX(), 0.0001);
                assertEquals(99.0, event.getDeltaY(), 0.0001);
                assertEquals(132.0, event.getTotalDeltaX(), 0.0001);
                assertEquals(198.0, event.getTotalDeltaY(), 0.0001);
                scrolled = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);
        
        assertTrue(scrolled);
    }

    @Test
    public void shouldUseTextDeltasForUnitsAndValues() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(0.0, event.getTextDeltaX(), 0.0001);
                assertEquals(0.0, event.getTextDeltaY(), 0.0001);
                assertSame(ScrollEvent.HorizontalTextScrollUnits.NONE, event.getTextDeltaXUnits());
                assertSame(ScrollEvent.VerticalTextScrollUnits.NONE, event.getTextDeltaYUnits());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 0, 0, 0, 0,
                150, 150, 150, 150, false, false, false, false, false, false);
        assertTrue(scrolled);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(8.0, event.getTextDeltaX(), 0.0001);
                assertEquals(15.0, event.getTextDeltaY(), 0.0001);
                assertSame(ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, event.getTextDeltaXUnits());
                assertSame(ScrollEvent.VerticalTextScrollUnits.LINES, event.getTextDeltaYUnits());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 4, 5, 3, 3,
                150, 150, 150, 150, false, false, false, false, false, false);
        assertTrue(scrolled);
        
        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(0.0, event.getTextDeltaX(), 0.0001);
                assertEquals(3.0, event.getTextDeltaY(), 0.0001);
                assertSame(ScrollEvent.HorizontalTextScrollUnits.NONE, event.getTextDeltaXUnits());
                assertSame(ScrollEvent.VerticalTextScrollUnits.PAGES, event.getTextDeltaYUnits());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 5, -1, -1, 3, 3,
                150, 150, 150, 150, false, false, false, false, false, false);
        assertTrue(scrolled);
        
    }
    
    @Test
    public void shouldPassModifiers() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertTrue(event.isShiftDown());
                assertFalse(event.isControlDown());
                assertTrue(event.isAltDown());
                assertFalse(event.isMetaDown());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, false, false);
        assertTrue(scrolled);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertFalse(event.isShiftDown());
                assertTrue(event.isControlDown());
                assertFalse(event.isAltDown());
                assertTrue(event.isMetaDown());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 3, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(scrolled);
    }

    @Test
    public void shouldPassDirect() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertTrue(event.isDirect());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertFalse(event.isDirect());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 3, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(scrolled);
    }

    @Test
    public void shouldPassInertia() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertTrue(event.isInertia());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, false, true);
        assertTrue(scrolled);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertFalse(event.isInertia());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 1, 1, 1, 3, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(scrolled);
    }

    @Test
    public void shouldPassTouchCount() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(0, event.getTouchCount());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);

        scrolled = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(5, event.getTouchCount());
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 5, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);
    }
    
    @Test
    public void shouldPassEventType() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        scrolled = false;
        rect.setOnScrollStarted(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);

        scrolled = false;
        rect.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 5, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);
    }

    @Test
    public void handlingAnyShouldGetAllTypes() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });

        scrolled = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);

        scrolled = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 5, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);

        scrolled = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 5, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);
    }

    @Test
    public void shouldDeliverWholeGestureToOneNode() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });
        rect2.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled2 = true;
            }
        });

        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);
        assertFalse(scrolled2);

        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(scrolled);
        assertFalse(scrolled2);

        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(scrolled);
        assertFalse(scrolled2);

        // inertia
        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, true);
        assertTrue(scrolled);
        assertFalse(scrolled2);
    }

    @Test
    public void shouldCompute3dCoordinates() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setTranslateZ(50);

        scrolled = false;
        scrolled2 = false;
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(150, event.getX(), 0.00001);
                assertEquals(150, event.getY(), 0.00001);
                assertEquals(0, event.getZ(), 0.00001);
                scrolled = true;
            }
        });

        scene.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(150, event.getX(), 0.00001);
                assertEquals(150, event.getY(), 0.00001);
                assertEquals(50, event.getZ(), 0.00001);
                scrolled2 = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);

        assertTrue(scrolled);
        assertTrue(scrolled2);
    }

    @Test
    public void shouldContainPickResult() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                pickRes = event.getPickResult();
                scrolled = true;
            }
        });
        rect2.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                pickRes = event.getPickResult();
                scrolled2 = true;
            }
        });

        scrolled = false;
        scrolled2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(scrolled);
        assertFalse(scrolled2);
        assertNotNull(pickRes);
        assertSame(rect1, pickRes.getIntersectedNode());
        assertEquals(150, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(150, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        scrolled = false;
        scrolled2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(scrolled);
        assertFalse(scrolled2);
        assertNotNull(pickRes);
        assertSame(rect2, pickRes.getIntersectedNode());
        assertEquals(250, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(250, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        scrolled = false;
        scrolled2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(scrolled);
        assertFalse(scrolled2);
        assertNotNull(pickRes);
        assertSame(rect2, pickRes.getIntersectedNode());
        assertEquals(250, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(250, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);

        // inertia
        scrolled = false;
        scrolled2 = false;
        pickRes = null;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, true);
        assertTrue(scrolled);
        assertFalse(scrolled2);
        assertNotNull(pickRes);
        assertSame(rect2, pickRes.getIntersectedNode());
        assertEquals(250, pickRes.getIntersectedPoint().getX(), 0.00001);
        assertEquals(250, pickRes.getIntersectedPoint().getY(), 0.00001);
        assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);
    }

    @Test
    public void shouldPickForMouseWheelDuringInertia() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });
        rect2.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled2 = true;
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);

        // inertia
        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, true);
        assertTrue(scrolled);
        assertFalse(scrolled2);

        // wheel
        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertFalse(scrolled);
        assertTrue(scrolled2);

        // inertia
        scrolled = false;
        scrolled2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, true);
        assertTrue(scrolled);
        assertFalse(scrolled2);
    }

    @Test
    public void unknownLocationShouldBeReplacedByMouseLocation() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);
        rect1.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });

        MouseEventGenerator generator = new MouseEventGenerator();

        scrolled = false;
        scrolled2 = false;
        rect2.setOnScrollStarted(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                scrolled2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(scrolled);
        assertTrue(scrolled2);

        scrolled = false;
        scrolled2 = false;
        rect2.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                scrolled2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(scrolled);
        assertTrue(scrolled2);

        scrolled = false;
        scrolled2 = false;
        rect2.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                scrolled2 = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(scrolled);
        assertTrue(scrolled2);
    }

    @Test
    public void finishedLocationShouldBeFixed() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                scrolled = true;
            }
        });

        scrolled = false;

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_STARTED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                150, 150, 150, 150, true, false, true, false, true, false);

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                250, 250, 250, 250, true, false, true, false, true, false);

        assertFalse(scrolled);

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL_FINISHED, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(scrolled);
    }

    @Test
    public void unknownLocalShouldBeFixedByMousePosition() {
        MouseEventGenerator gen = new MouseEventGenerator();
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                scrolled = true;
            }
        });

        scrolled = false;

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertFalse(scrolled);

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 150, 150));

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                ScrollEvent.SCROLL, 2, 3, 4, 6, 33, 33, 0, 1, 1, 3, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(scrolled);
    }

    @Test public void testToString() {
        ScrollEvent e = new ScrollEvent(ScrollEvent.SCROLL,
            100, 100, 200, 200,
            false, false, false, false,
            true, false, 10, 10, 20, 20,ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
            ScrollEvent.VerticalTextScrollUnits.NONE, 0,
            3, null);

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
