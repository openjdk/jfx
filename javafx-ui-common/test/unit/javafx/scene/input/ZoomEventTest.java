/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZoomEventTest {

    private boolean zoomed;
    private boolean zoomed2;
    
    @Test
    public void shouldDeliverZoomEventToPickedNode() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        zoomed = false;
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 1, 1,
                50, 50, 50, 50, false, false, false, false, false, false);
        
        assertFalse(zoomed);

        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertEquals(1.2, event.getZoomFactor(), 0.0001);
                assertEquals(2.4, event.getTotalZoomFactor(), 0.0001);
                zoomed = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertTrue(event.isShiftDown());
                assertFalse(event.isControlDown());
                assertTrue(event.isAltDown());
                assertFalse(event.isMetaDown());
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, false, false);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertFalse(event.isShiftDown());
                assertTrue(event.isControlDown());
                assertFalse(event.isAltDown());
                assertTrue(event.isMetaDown());
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertTrue(event.isDirect());
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertFalse(event.isDirect());
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertTrue(event.isInertia());
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, false, true);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertFalse(event.isInertia());
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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
        rect.setOnZoomStarted(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        rect.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);
    }

    @Test
    public void handlingAnyShouldGetAllTypes() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(ZoomEvent.ANY, new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed = true;
            }
        });

        zoomed = false;
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);

        zoomed = false;
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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

        rect1.addEventHandler(ZoomEvent.ANY, new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed = true;
            }
        });
        rect2.addEventHandler(ZoomEvent.ANY, new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed2 = true;
            }
        });

        zoomed = false;
        zoomed2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);

        zoomed = false;
        zoomed2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);

        zoomed = false;
        zoomed2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_FINISHED, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(zoomed);
        assertFalse(zoomed2);
    }

    @Test
    public void unknownLocationShouldBeReplacedByMouseLocation() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);
        rect1.addEventHandler(ZoomEvent.ANY, new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomed = true;
            }
        });

        MouseEventGenerator generator = new MouseEventGenerator();

        zoomed = false;
        zoomed2 = false;
        rect2.setOnZoomStarted(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                zoomed2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(zoomed);
        assertTrue(zoomed2);

        zoomed = false;
        zoomed2 = false;
        rect2.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                zoomed2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(zoomed);
        assertTrue(zoomed2);

        zoomed = false;
        zoomed2 = false;
        rect2.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                zoomed2 = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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
        rect.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                zoomed = true;
            }
        });

        zoomed = false;

        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);

        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);

        assertFalse(zoomed);

        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
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

        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                zoomed = true;
            }
        });

        zoomed = false;

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));

        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertFalse(zoomed);

        scene.impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 150, 150));

        ((StubScene) scene.impl_getPeer()).getListener().zoomEvent(
                ZoomEvent.ZOOM, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(zoomed);
    }

    @Test public void testToString() {
        ZoomEvent e = new ZoomEvent(ZoomEvent.ZOOM,
            100, 100, 200, 200,
            false, false, false, false,
            true, false, 10, 20);

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
