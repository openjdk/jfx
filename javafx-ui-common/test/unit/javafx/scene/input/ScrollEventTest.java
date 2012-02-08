/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScrollEventTest {

    private boolean scrolled;
    
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
                1, 1, 1, 1, 1, 1, 1, 1, 50, 50, 50, 50, false, false, false, false);
        
        assertFalse(scrolled);

        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                1, 1, 1, 1, 1, 1, 1, 1, 150, 150, 150, 150, false, false, false, false);
        
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
                scrolled = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().scrollEvent(
                2, 3, 33, 33, 1, 1, 1, 1, 150, 150, 150, 150, false, false, false, false);
        
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
                2, 3, 33, 33, 0, 0, 0, 0, 150, 150, 150, 150, false, false, false, false);
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
                2, 3, 33, 33, 4, 5, 3, 3, 150, 150, 150, 150, false, false, false, false);
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
                2, 3, 33, 33, -1, -1, 3, 3, 150, 150, 150, 150, false, false, false, false);
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
                2, 3, 33, 33, 1, 1, 3, 3, 150, 150, 150, 150, true, false, true, false);
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
                2, 3, 33, 33, 1, 1, 3, 3, 150, 150, 150, 150, false, true, false, true);
        assertTrue(scrolled);
    }
    
    private Scene createScene() {
        final Group root = new Group();
        
        final Scene scene = new Scene(root, 400, 400);

        Rectangle rect = new Rectangle(100, 100, 100, 100);

        root.getChildren().add(rect);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        return scene;
    }
}
