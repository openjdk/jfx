/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.test.MouseEventGenerator;
import static org.junit.Assert.*;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;

import com.sun.javafx.pgstub.StubScene;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

public class MouseTest {

    private static final double DONT_TEST = 0.0;

    @Test
    public void moveShouldBubbleToParent() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasMoved());
        assertFalse(scene.bigSquareTracker.wasMoved());
        assertTrue(scene.groupTracker.wasMoved());
    }

    @Test
    public void moveOutsideNodesShouldntBeNoticed() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 350, 350));

        assertFalse(scene.smallSquareTracker.wasMoved());
        assertFalse(scene.bigSquareTracker.wasMoved());
        assertFalse(scene.groupTracker.wasMoved());
    }

    @Test
    public void moveOutsideNodesShouldGoToScene() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 350, 350));

        assertTrue(scene.wasMoused());
    }

    @Test
    public void ImpossibleToComputeCoordsShouldResultInNaNs() {
        final SimpleTestScene scene = new SimpleTestScene();

        scene.smallSquareTracker.node.setOnMouseMoved(event -> scene.smallSquareTracker.node.setScaleX(0));

        scene.smallSquareTracker.node.setOnMouseExited(event -> {
            assertTrue(Double.isNaN(event.getX()));
            assertTrue(Double.isNaN(event.getY()));
            assertTrue(Double.isNaN(event.getZ()));
        });

        scene.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        scene.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 260, 260));
        assertTrue(scene.smallSquareTracker.enteredMe);
    }

    @Test
    public void PdrGestureShouldGoToOneNode() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        /* gesture */
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 150, 150));

        assertTrue(scene.smallSquareTracker.wasPressed());
        assertTrue(scene.smallSquareTracker.wasDragged());
        assertTrue(scene.smallSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasPressed());
        assertFalse(scene.bigSquareTracker.wasDragged());
        assertFalse(scene.bigSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasMoved());

        /* gesture ended */
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 140, 140));

        assertTrue(scene.bigSquareTracker.wasMoved());
    }

    @Test
    public void PdrGestureOnSceneShouldGoOnlyToScene() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        /* gesture */
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 50, 50));

        assertTrue(scene.wasMoused());
        assertFalse(scene.smallSquareTracker.wasPressed());
        assertFalse(scene.smallSquareTracker.wasDragged());
        assertFalse(scene.smallSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasPressed());
        assertFalse(scene.bigSquareTracker.wasDragged());
        assertFalse(scene.bigSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasMoved());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        assertTrue(scene.wasMoused());
        assertFalse(scene.smallSquareTracker.wasPressed());
        assertFalse(scene.smallSquareTracker.wasDragged());
        assertFalse(scene.smallSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasPressed());
        assertFalse(scene.bigSquareTracker.wasDragged());
        assertFalse(scene.bigSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasMoved());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 150, 150));

        assertTrue(scene.wasMoused());
        assertFalse(scene.smallSquareTracker.wasPressed());
        assertFalse(scene.smallSquareTracker.wasDragged());
        assertFalse(scene.smallSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasPressed());
        assertFalse(scene.bigSquareTracker.wasDragged());
        assertFalse(scene.bigSquareTracker.wasReleased());
        assertFalse(scene.bigSquareTracker.wasMoved());

        /* gesture ended */
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 140, 140));

        assertTrue(scene.bigSquareTracker.wasMoved());
    }

    @Test
    public void testEnteredExitedGeneration() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 350, 350));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertTrue(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertTrue(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertTrue(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertTrue(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 50, 50));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertTrue(scene.bigSquareTracker.wasExitedMe());
        assertTrue(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, -1, 50));
        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertTrue(scene.sceneTracker.wasExitedMe());
        scene.clear();
    }

    @Test
    public void testEnteredExitedGenerationDuringPdr() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 255, 255));

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertTrue(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertTrue(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertTrue(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 50, 50));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertTrue(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, -1, 50));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertTrue(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertTrue(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertTrue(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 350, 350));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertTrue(scene.groupTracker.wasExitedMe());
        assertFalse(scene.sceneTracker.wasEnteredMe());
        assertFalse(scene.sceneTracker.wasExitedMe());
    }

    @Test
    public void testEnteredExitedGenerationAfterPdr() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 155, 155));

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertTrue(scene.bigSquareTracker.wasEnteredMe());
        assertTrue(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 250, 250));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertTrue(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
    }

    @Test
    public void testEnteredExitedGenerationDuringDndOnScene() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 50, 50));

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 250, 250));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 50, 50));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 350, 350));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
    }

    @Test
    public void testEnteredExitedMeChildSwitching() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertTrue(scene.bigSquareTracker.wasEnteredMe());
        assertTrue(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.smallSquareTracker.wasEnteredChild());
        assertFalse(scene.bigSquareTracker.wasEnteredChild());
        assertTrue(scene.groupTracker.wasEnteredChild());
        assertFalse(scene.smallSquareTracker.wasExitedChild());
        assertFalse(scene.bigSquareTracker.wasExitedChild());
        assertFalse(scene.groupTracker.wasExitedChild());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertFalse(scene.smallSquareTracker.wasExitedMe());
        assertTrue(scene.bigSquareTracker.wasExitedMe());
        assertFalse(scene.groupTracker.wasExitedMe());
        assertFalse(scene.smallSquareTracker.wasEnteredChild());
        assertFalse(scene.bigSquareTracker.wasEnteredChild());
        assertTrue(scene.groupTracker.wasEnteredChild());
        assertFalse(scene.smallSquareTracker.wasExitedChild());
        assertFalse(scene.bigSquareTracker.wasExitedChild());
        assertTrue(scene.groupTracker.wasExitedChild());
        scene.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 350, 350));

        assertFalse(scene.smallSquareTracker.wasEnteredMe());
        assertFalse(scene.bigSquareTracker.wasEnteredMe());
        assertFalse(scene.groupTracker.wasEnteredMe());
        assertTrue(scene.smallSquareTracker.wasExitedMe());
        assertFalse(scene.bigSquareTracker.wasExitedMe());
        assertTrue(scene.groupTracker.wasExitedMe());
        assertFalse(scene.smallSquareTracker.wasEnteredChild());
        assertFalse(scene.bigSquareTracker.wasEnteredChild());
        assertFalse(scene.groupTracker.wasEnteredChild());
        assertFalse(scene.smallSquareTracker.wasExitedChild());
        assertFalse(scene.bigSquareTracker.wasExitedChild());
        assertTrue(scene.groupTracker.wasExitedChild());
    }

    @Test
    public void platformCursorShouldBeUpdated() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertSame(Cursor.HAND.getCurrentFrame(), scene.getCursorFrame());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));

        assertSame(Cursor.TEXT.getCurrentFrame(), scene.getCursorFrame());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 50, 50));

        assertSame(Cursor.DEFAULT.getCurrentFrame(),
                   scene.getCursorFrame());
    }
    
    @Test
    public void platformCursorShouldBeUpdatedOnPulse() {
        
        final StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertSame(Cursor.HAND.getCurrentFrame(), scene.getCursorFrame());

        scene.scene.setCursor(Cursor.TEXT);
        toolkit.firePulse();
        assertSame(Cursor.TEXT.getCurrentFrame(), scene.getCursorFrame());
        
    }

    @Test
    public void testHover() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertTrue(scene.smallSquareTracker.isHover());
        assertFalse(scene.bigSquareTracker.isHover());
        assertTrue(scene.groupTracker.isHover());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));

        assertFalse(scene.smallSquareTracker.isHover());
        assertTrue(scene.bigSquareTracker.isHover());
        assertTrue(scene.groupTracker.isHover());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 50, 50));

        assertFalse(scene.smallSquareTracker.isHover());
        assertFalse(scene.bigSquareTracker.isHover());
        assertFalse(scene.groupTracker.isHover());
    }

    @Test
    public void clickShouldBeGeneratedFromPressRelease() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
    }
    
    @Test
    public void clickShouldBeGeneratedWhenReleasedOnTheSameNode() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 50, 50));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 270, 270));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 270, 270));

        assertTrue(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
    }
    
    @Test
    public void clickShouldBeGeneratedWhenReleasedOnParent() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 150, 150));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 150, 150));

        assertFalse(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
    }
    
    @Test
    public void doubleClickShouldBeGeneratedFromPressedReleasedTwoTimes() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertTrue(scene.groupTracker.wasDoubleClicked());
    }

    @Test
    public void doubleClickShouldBeGeneratedEvenIfNodeChangesInBetween() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 199, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 199, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 201, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 201, 250));

        assertTrue(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertTrue(scene.bigSquareTracker.wasClicked());
    }

    @Test
    public void doubleClickShouldBeGeneratedFromPressedReleasedTwoTimesWithHysteresis() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 251, 251));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 251, 251));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 252, 252));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 252, 252));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 253, 253));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 253, 253));

        assertTrue(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertTrue(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void doubleClickShouldNotBeGeneratedWhenMovedFarAwayBetweenClick() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 258, 258));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 258, 258));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 258, 258));

        assertTrue(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
        assertFalse(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertFalse(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void doubleClickShouldNotBeGeneratedWhenMovedFarAwayInFirstClick() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 258, 258));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 258, 258));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 258, 258));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 258, 258));

        assertTrue(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
        assertFalse(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertFalse(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void doubleClickShouldBeGeneratedWhenMovedFarAwayInSecondClick() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 258, 258));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 258, 258));

        assertTrue(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertTrue(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void doubleClickShouldNotBeGeneratedWithLongTimeBetweenClicks() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        ((StubToolkit) Toolkit.getToolkit()).setAnimationTime(0);
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));
        
        ((StubToolkit) Toolkit.getToolkit()).setAnimationTime(2000);

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
        assertFalse(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertFalse(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void doubleClickShouldNotBeGeneratedWithLongTimeInFirstClick() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        ((StubToolkit) Toolkit.getToolkit()).setAnimationTime(0);
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        
        ((StubToolkit) Toolkit.getToolkit()).setAnimationTime(2000);

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasClicked());
        assertFalse(scene.bigSquareTracker.wasClicked());
        assertTrue(scene.groupTracker.wasClicked());
        assertFalse(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertFalse(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void doubleClickShouldBeGeneratedWithLongTimeInSecondClick() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        ((StubToolkit) Toolkit.getToolkit()).setAnimationTime(0);
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        
        ((StubToolkit) Toolkit.getToolkit()).setAnimationTime(2000);

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 250, 250));

        assertTrue(scene.smallSquareTracker.wasDoubleClicked());
        assertFalse(scene.bigSquareTracker.wasDoubleClicked());
        assertTrue(scene.groupTracker.wasDoubleClicked());
    }
    
    @Test
    public void testClickCountOfMouseEvents() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        assertEquals(0, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        assertEquals(1, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 251, 251));
        assertEquals(1, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 251, 251));
        assertEquals(1, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 252, 252));
        assertEquals(0, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 252, 252));
        assertEquals(2, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 280, 280));
        assertEquals(2, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 280, 280));
        assertEquals(2, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 280, 280));
        assertEquals(0, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 280, 280));
        assertEquals(1, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 280, 280));
        assertEquals(1, scene.smallSquareTracker.getLastClickCount());
        scene.smallSquareTracker.clear();        
    }
    
    @Test
    public void testIsStillOfMouseEvents() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        assertFalse(scene.smallSquareTracker.wasLastStill());
    
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        assertFalse(scene.smallSquareTracker.wasLastStill());
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 252, 252));
        assertTrue(scene.smallSquareTracker.wasLastStill());
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 252, 252));
        assertTrue(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 254, 254));
        assertTrue(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 254, 254));
        assertTrue(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 257, 257));
        assertTrue(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 265, 265));
        assertFalse(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 265, 265));
        assertFalse(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 266, 266));
        assertFalse(scene.smallSquareTracker.wasLastStill());

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 266, 266));
        assertFalse(scene.smallSquareTracker.wasLastStill());
        
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 266, 266));
        assertTrue(scene.smallSquareTracker.wasLastStill());        
    }

    @Test
    public void nodeMovementsWithStillMouseShouldFireEnteredExited() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_ENTERED, 250, 250));
        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        scene.smallSquareTracker.clear();

        assertFalse(scene.smallSquareTracker.exitedMe);
        ((Rectangle) scene.smallSquareTracker.node).setX(400);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertTrue(scene.smallSquareTracker.exitedMe);

        scene.smallSquareTracker.clear();

        assertFalse(scene.smallSquareTracker.enteredMe);
        ((Rectangle) scene.smallSquareTracker.node).setX(200);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertTrue(scene.smallSquareTracker.enteredMe);
    }

    @Test
    public void nodeWithNoninvertibleTransformShouldGetNanCoords() {
        SimpleTestScene scene = new SimpleTestScene();
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        scene.smallSquareTracker.clear();

        scene.smallSquareTracker.node.addEventHandler(MouseEvent.MOUSE_RELEASED,
                event -> {
                    Assert.assertEquals(Double.NaN, event.getX(), 0.0001);
                    Assert.assertEquals(Double.NaN, event.getY(), 0.0001);
                    Assert.assertEquals(251.0, event.getSceneX(), 0.0001);
                    Assert.assertEquals(251.0, event.getSceneY(), 0.0001);
                }
        );

        ((Rectangle) scene.smallSquareTracker.node).setScaleX(0);

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 251, 251));

        assertTrue(scene.smallSquareTracker.released);
    }

    @Test
    public void topMostNodeShouldBePickedWithDepthBufferByPerspectiveCamera() {
        SimpleTestScene scene = new SimpleTestScene(true, true);
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertFalse(scene.smallSquareTracker.wasMoved());
        assertTrue(scene.bigSquareTracker.wasMoved());
        assertTrue(scene.groupTracker.wasMoved());
    }

    @Test
    public void topMostNodeShouldBePickedWithDepthBufferByParallelCamera() {
        SimpleTestScene scene = new SimpleTestScene(true, false);
        MouseEventGenerator generator = new MouseEventGenerator();

        scene.processEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));

        assertFalse(scene.smallSquareTracker.wasMoved());
        assertTrue(scene.bigSquareTracker.wasMoved());
        assertTrue(scene.groupTracker.wasMoved());
    }

    @Test
    public void subSceneShouldBePickedOnFill() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.subScene1.setFill(Color.BLACK);

        s.scene.addEventFilter(MouseEvent.MOUSE_MOVED,
                s.handler(s.subScene1, DONT_TEST, DONT_TEST));

        s.processEvent(MouseEvent.MOUSE_MOVED, 22, 12);
        s.assertCalled();
    }

    @Test
    public void subSceneShouldNotBePickedWithoutFill() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.scene.addEventFilter(MouseEvent.MOUSE_MOVED,
                s.handler(s.scene, DONT_TEST, DONT_TEST));

        s.processEvent(MouseEvent.MOUSE_MOVED, 22, 12);
        s.assertCalled();
    }

    @Test
    public void eventShouldBubbleOutOfSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent1.setOnMouseMoved(s.handler(s.innerRect1, 120, 20));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void enteredEventShouldBeGeneratedInsideSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnMouseEntered(s.handler(s.innerRect1, 10, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void enteredEventShouldBubbleOutOfSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent1.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET,
                event -> {
                    if (event.getTarget() == s.innerRect1) {
                        s.called = true;
                    }
                }
        );

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void enteredEventShouldBeGeneratedOutsideSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent1.setOnMouseEntered(s.handler(s.parent1, 120, 20));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void exitedEventShouldBeGeneratedInsideSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnMouseExited(s.handler(s.innerRect1, 60, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 180, 30);
        s.assertCalled();
    }

    @Test
    public void exitedEventShouldBeGeneratedInsideSubSceneWhenMovedOut() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnMouseExited(s.handler(s.innerRect1, 60, -15));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 180, 5);
        s.assertCalled();
    }

    @Test
    public void exitedEventShouldBeGeneratedInsideSubSceneWhenMovedToOtherSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnMouseExited(s.handler(s.innerRect1, 240, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }

    @Test
    public void cursorShouldBePropagatedFromSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setCursor(Cursor.HAND);
        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        assertSame(Cursor.HAND.getCurrentFrame(), s.getCursorFrame());
    }

    @Test
    public void cursorShouldBeTakenFromOutsideOfSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent1.setCursor(Cursor.HAND);
        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        assertSame(Cursor.HAND.getCurrentFrame(), s.getCursorFrame());
    }

    @Test
    public void coordinatesShouldBeFineWhenDraggedOutOfSubSecne() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnMouseDragged(s.handler(s.innerRect1, 60, -15));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_PRESSED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_DRAGGED, 180, 5);
        s.processEvent(MouseEvent.MOUSE_RELEASED, 180, 5);

        s.assertCalled();
        assertNotNull(s.lastPickResult);
        assertNull(s.lastPickResult.getIntersectedNode());
    }

    @Test
    public void coordinatesShouldBeFineWhenDraggedToOtherSubSecne() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnMouseDragged(s.handler(s.innerRect1, 240, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_PRESSED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_DRAGGED, 360, 30);
        s.processEvent(MouseEvent.MOUSE_RELEASED, 360, 30);

        s.assertCalled();
        assertNotNull(s.lastPickResult);
        assertSame(s.innerRect2, s.lastPickResult.getIntersectedNode());
    }

    @Test
    public void fullDragShouldDeliverToOtherSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnDragDetected(event -> s.innerRect1.startFullDrag());
        s.innerRect2.setOnMouseDragOver(s.handler(s.innerRect2, 10, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_PRESSED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_DRAGGED, 360, 29);
        s.processEvent(MouseEvent.MOUSE_DRAGGED, 360, 30);
        s.processEvent(MouseEvent.MOUSE_RELEASED, 360, 30);

        s.assertCalled();
    }

    @Test
    public void fullDragEnterShouldDeliverToOtherSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnDragDetected(event -> s.innerRect1.startFullDrag());
        s.innerRect2.setOnMouseDragEntered(s.handler(s.innerRect2, 10, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_PRESSED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_DRAGGED, 360, 30);
        s.processEvent(MouseEvent.MOUSE_RELEASED, 360, 30);

        s.assertCalled();
    }

    @Test
    public void fullDragEnterShouldDeliverToOtherSubScenesParent() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.innerRect1.setOnDragDetected(event -> s.innerRect1.startFullDrag());
        s.parent2.setOnMouseDragEntered(s.handler(s.parent2, 130, 20));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_PRESSED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_DRAGGED, 360, 30);
        s.processEvent(MouseEvent.MOUSE_RELEASED, 360, 30);

        s.assertCalled();
    }

    @Test
    public void depthBufferShouldWorkInsideSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes(false, true);

        Rectangle r = new Rectangle(50, 50);
        r.setTranslateX(100);
        r.setTranslateY(10);
        r.setTranslateZ(1);
        s.subScene1.getRoot().getChildren().add(r);

        s.innerRect1.addEventFilter(MouseEvent.MOUSE_MOVED,
                s.handler(s.innerRect1, DONT_TEST, DONT_TEST));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void sceneDepthBufferShouldNotInfluenceSubSceneDepthBuffer() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes(true, false);

        Rectangle r = new Rectangle(50, 50);
        r.setTranslateX(100);
        r.setTranslateY(10);
        r.setTranslateZ(1);
        s.subScene1.getRoot().getChildren().add(r);

        r.addEventFilter(MouseEvent.MOUSE_MOVED,
                s.handler(r, DONT_TEST, DONT_TEST));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void sceneShouldConsiderSubScenesFlat() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes(false, true);

        s.parent2.setTranslateX(10);
        s.innerRect2.setTranslateZ(-10);
        s.parent2.setTranslateZ(1);

        s.innerRect2.addEventFilter(MouseEvent.MOUSE_MOVED,
                s.handler(s.innerRect2, DONT_TEST, DONT_TEST));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.assertCalled();
    }

    @Test
    public void coordinatesShouldBeFlattenedWhenBubblingOutOfSubScene() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.subScene1.setTranslateZ(5);
        s.innerRect1.setTranslateZ(10);

        s.parent1.setOnMouseMoved(event -> {
            Assert.assertEquals(5, event.getZ(), 0.00001);
            assertSame(s.innerRect1, event.getTarget());
            s.called = true;
        });

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }
    
    @Test
    public void coordinatesShouldBeReComputedToSubScenePlane() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent2.setTranslateZ(10);
        s.innerRect2.setTranslateZ(10);

        s.innerRect1.setOnMouseExited(s.handler(s.innerRect1, 240, 10));

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }

    @Test
    public void coordinatesShouldBeRecomputedToSubSceneProjectionPlane() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.scene.setCamera(new PerspectiveCamera());
        s.subScene1.setCamera(new PerspectiveCamera());
        s.subScene2.setCamera(new PerspectiveCamera());

        s.subScene1.setRotationAxis(Rotate.Y_AXIS);
        s.subScene1.setRotate(45);
        s.subScene2.setRotationAxis(Rotate.Y_AXIS);
        s.subScene2.setRotate(-45);
        ((Group) s.subScene1.getRoot()).getChildren().add(s.subScene1.getCamera());
        s.subScene1.getCamera().setTranslateZ(-3.0);

        s.innerRect1.setOnMouseExited(event -> {
            assertSame(s.innerRect2, event.getPickResult().getIntersectedNode());
            assertSame(s.innerRect1, event.getTarget());
            Assert.assertEquals(16.58, event.getPickResult().getIntersectedPoint().getX(), 0.1);
            Assert.assertEquals(7.33, event.getPickResult().getIntersectedPoint().getY(), 0.1);
            Assert.assertEquals(0.0, event.getPickResult().getIntersectedPoint().getZ(), 0.0001);
            Assert.assertEquals(295.81, event.getX(), 0.1);
            Assert.assertEquals(57.64, event.getY(), 0.1);
            Assert.assertEquals(-3.0, event.getZ(), 0.1);

            s.called = true;
        });

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }

    @Test
    public void coordinatesShouldBeRecomputedToNodeLocalCoordsOfSubSceneProjectionPlane() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.scene.setCamera(new PerspectiveCamera());
        s.subScene1.setCamera(new PerspectiveCamera());
        s.subScene2.setCamera(new PerspectiveCamera());

        s.subScene1.setRotationAxis(Rotate.Y_AXIS);
        s.subScene1.setRotate(45);
        s.innerRect1.setRotationAxis(Rotate.Y_AXIS);
        s.innerRect1.setRotate(-45);
        s.subScene2.setRotationAxis(Rotate.Y_AXIS);
        s.subScene2.setRotate(-45);
        s.innerRect2.setRotationAxis(Rotate.Y_AXIS);
        s.innerRect2.setRotate(45);

        s.innerRect1.setOnMouseExited(event -> {
            assertSame(s.innerRect2, event.getPickResult().getIntersectedNode());
            assertSame(s.innerRect1, event.getTarget());
            Assert.assertEquals(14.07, event.getPickResult().getIntersectedPoint().getX(), 0.1);
            Assert.assertEquals(5.97, event.getPickResult().getIntersectedPoint().getY(), 0.1);
            Assert.assertEquals(0.0, event.getPickResult().getIntersectedPoint().getZ(), 0.0001);
            Assert.assertEquals(216.49, event.getX(), 0.1);
            Assert.assertEquals(57.64, event.getY(), 0.1);
            Assert.assertEquals(-191.49, event.getZ(), 0.1);

            s.called = true;
        });

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }

    @Test
    public void ImpossibleToComputeSubSceneInnerNodeCoordsShouldResultInNaNs() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent2.setTranslateZ(10);
        s.innerRect2.setTranslateZ(10);

        s.innerRect1.setOnMouseMoved(event -> s.innerRect1.setScaleX(0.0));

        s.innerRect1.setOnMouseExited(event -> {
            assertSame(s.innerRect1, event.getTarget());
            assertTrue(Double.isNaN(event.getX()));
            assertTrue(Double.isNaN(event.getY()));
            assertTrue(Double.isNaN(event.getZ()));
            s.called = true;
        });

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }

    @Test
    public void ImpossibleToComputeSubSceneCoordsShouldResultInNaNs() {
        final TestSceneWithSubScenes s = new TestSceneWithSubScenes();

        s.parent2.setTranslateZ(10);
        s.innerRect2.setTranslateZ(10);

        s.innerRect1.setOnMouseMoved(event -> {
            s.subScene1.setRotationAxis(Rotate.Y_AXIS);
            s.subScene1.setRotate(90);
        });

        s.innerRect1.setOnMouseExited(event -> {
            assertSame(s.innerRect1, event.getTarget());
            assertTrue(Double.isNaN(event.getX()));
            assertTrue(Double.isNaN(event.getY()));
            assertTrue(Double.isNaN(event.getZ()));
            s.called = true;
        });

        s.processEvent(MouseEvent.MOUSE_MOVED, 130, 30);
        s.processEvent(MouseEvent.MOUSE_MOVED, 360, 30);
        s.assertCalled();
    }

    @Test
    public void mouseExitedShouldBeGeneratedBeforeNodeRemoval() {
        final SimpleTestScene s = new SimpleTestScene();
        s.smallSquareTracker.node.addEventHandler(MouseEvent.MOUSE_EXITED,
                event -> assertNotNull(s.smallSquareTracker.node.getScene())
        );

        s.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        assertTrue(s.smallSquareTracker.enteredMe);
        assertFalse(s.smallSquareTracker.exitedMe);

        s.scene.getRoot().getChildren().clear();

        assertTrue(s.smallSquareTracker.exitedMe);
    }

    @Test
    public void pdrGestureShouldHandleDraggedNodeRemoval() {
        final SimpleTestScene s = new SimpleTestScene();

        s.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        assertTrue(s.smallSquareTracker.node.isHover());
        assertFalse(s.smallSquareTracker.node.isPressed());

        s.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_PRESSED, 250, 250));
        assertTrue(s.smallSquareTracker.node.isHover());
        assertTrue(s.smallSquareTracker.node.isPressed());
        assertTrue(s.smallSquareTracker.wasPressed());
        assertTrue(s.groupTracker.wasPressed());

        s.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 255, 255));
        assertTrue(s.smallSquareTracker.node.isHover());
        assertTrue(s.smallSquareTracker.node.isPressed());
        assertTrue(s.smallSquareTracker.wasDragged());
        assertTrue(s.groupTracker.wasDragged());

        s.smallSquareTracker.clear();
        s.groupTracker.clear();
        s.rootTracker.clear();

        s.scene.getRoot().getChildren().clear();
        assertFalse(s.smallSquareTracker.node.isHover());
        assertFalse(s.smallSquareTracker.node.isPressed());
        assertFalse(s.groupTracker.node.isHover());
        assertFalse(s.groupTracker.node.isPressed());
        assertTrue(s.rootTracker.node.isHover());
        assertTrue(s.rootTracker.node.isPressed());
        assertTrue(s.smallSquareTracker.wasExitedMe());
        assertTrue(s.groupTracker.wasExitedMe());
        assertFalse(s.rootTracker.wasExitedMe());

        s.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_DRAGGED, 260, 260));
        assertFalse(s.smallSquareTracker.wasDragged());
        assertFalse(s.groupTracker.wasDragged());
        assertTrue(s.rootTracker.wasDragged());

        s.processEvent(MouseEventGenerator.generateMouseEvent(
                MouseEvent.MOUSE_RELEASED, 270, 270));
        assertFalse(s.smallSquareTracker.wasReleased());
        assertFalse(s.groupTracker.wasReleased());
        assertTrue(s.rootTracker.wasReleased());
    }
    
    @Test
    public void testMouseEventCanEditCursor() {
        final SimpleTestScene s = new SimpleTestScene();
        
        s.smallSquare.setOnMousePressed(event -> s.smallSquare.setCursor(Cursor.CROSSHAIR));
        
        s.smallSquare.setOnMouseReleased(event -> s.smallSquare.setCursor(Cursor.WAIT));
        
        s.processEvent(MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));
        assertSame(Cursor.HAND.getCurrentFrame(), s.getCursorFrame());
        s.processEvent(MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 250, 250));
        assertSame(Cursor.CROSSHAIR.getCurrentFrame(), s.getCursorFrame());
        s.processEvent(MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 250, 250));
        assertSame(Cursor.WAIT.getCurrentFrame(), s.getCursorFrame());
        
        s.smallSquare.setOnMouseClicked(event -> s.smallSquare.setCursor(Cursor.TEXT));
        
        s.smallSquare.setCursor(Cursor.HAND);
        
        s.processEvent(MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_MOVED, 250, 250));
        assertSame(Cursor.HAND.getCurrentFrame(), s.getCursorFrame());
        s.processEvent(MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 250, 250));
        assertSame(Cursor.CROSSHAIR.getCurrentFrame(), s.getCursorFrame());
        s.processEvent(MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 250, 250));
        assertSame(Cursor.TEXT.getCurrentFrame(), s.getCursorFrame());
    }
    
    private static class SimpleTestScene {

        MouseEventTracker sceneTracker;
        MouseEventTracker rootTracker;
        MouseEventTracker groupTracker;
        MouseEventTracker bigSquareTracker;
        MouseEventTracker smallSquareTracker;
        
        Rectangle smallSquare;
        private boolean moused = false;
        private Scene scene;

        public SimpleTestScene() {
            this(false, false);
        }

        public SimpleTestScene(boolean depthBuffer, boolean perspective) {
            final Group root = new Group();
            scene = new Scene(root, 400, 400, depthBuffer);
            if (perspective) {
                scene.setCamera(new PerspectiveCamera());
            }

            Group group = new Group();
            Rectangle bigSquare = new Rectangle(100, 100, 200, 200);
            smallSquare = new Rectangle(200, 200, 100, 100);
            bigSquare.setTranslateZ(-1);

            smallSquare.setCursor(Cursor.HAND);
            group.setCursor(Cursor.TEXT);

            group.getChildren().add(bigSquare);
            group.getChildren().add(smallSquare);

            root.getChildren().add(group);

            sceneTracker = new MouseEventTracker(scene);
            rootTracker = new MouseEventTracker(root);
            groupTracker = new MouseEventTracker(group);
            bigSquareTracker = new MouseEventTracker(bigSquare);
            smallSquareTracker = new MouseEventTracker(smallSquare);

            scene.addEventHandler(MouseEvent.ANY, event -> {
                moused = true;
            });

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
        }

        public void processEvent(MouseEvent e) {
            scene.impl_processMouseEvent(e);
        }

        public Object getCursorFrame() {
            return ((StubScene) scene.impl_getPeer()).getCursor();
        }

        public boolean wasMoused() {
            return moused;
        }

        public void clear() {
            moused = false;
            sceneTracker.clear();
            groupTracker.clear();
            bigSquareTracker.clear();
            smallSquareTracker.clear();
        }
    }

    private static class MouseEventTracker {

        private Node node;
        private boolean pressed = false;
        private boolean released = false;
        private boolean clicked = false;
        private boolean doubleClicked = false;
        private boolean moved = false;
        private boolean dragged = false;
        private boolean enteredMe = false;
        private boolean exitedMe = false;
        private boolean enteredChild = false;
        private boolean exitedChild = false;
        private int lastClickCount = 0;
        private boolean lastIsStill = false;

        public MouseEventTracker(Node node) {
            this.node = node;

            node.setOnMousePressed(event -> {
                pressed = true;
            });

            node.setOnMouseReleased(event -> {
                released = true;
            });

            node.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1) {
                    clicked = true;
                } else if (event.getClickCount() == 2) {
                    doubleClicked = true;
                }
            });

            node.setOnMouseMoved(event -> {
                moved = true;
            });

            node.setOnMouseDragged(event -> {
                dragged = true;
            });

            node.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, event -> {
                if (event.getTarget() == MouseEventTracker.this.node) {
                    enteredMe = true;
                } else {
                    enteredChild = true;
                }
            });

            node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, event -> {
                if (event.getTarget() == MouseEventTracker.this.node) {
                    exitedMe = true;
                } else {
                    exitedChild = true;
                }
            });
            
            node.addEventHandler(MouseEvent.ANY, event -> {
                if (event.getEventType() != MouseEvent.MOUSE_CLICKED) {
                    lastClickCount = event.getClickCount();
                    lastIsStill = event.isStillSincePress();
                }
            });
        }

        public MouseEventTracker(Scene scene) {
            scene.setOnMouseEntered(event -> {
                enteredMe = true;
            });

            scene.setOnMouseExited(event -> {
                exitedMe = true;
            });
        }

        public boolean wasClicked() {
            return clicked;
        }

        public boolean wasDoubleClicked() {
            return doubleClicked;
        }

        public boolean wasDragged() {
            return dragged;
        }

        public boolean wasEnteredMe() {
            return enteredMe;
        }

        public boolean wasExitedMe() {
            return exitedMe;
        }

        public boolean wasMoved() {
            return moved;
        }

        public boolean wasPressed() {
            return pressed;
        }

        public boolean wasReleased() {
            return released;
        }

        public boolean wasEnteredChild() {
            return enteredChild;
        }

        public boolean wasExitedChild() {
            return exitedChild;
        }

        public boolean isHover() {
            return node.isHover();
        }
        
        public int getLastClickCount() {
            return lastClickCount;
        }
        
        public boolean wasLastStill() {
            return lastIsStill;
        }

        public void clear() {
            pressed = false;
            released = false;
            clicked = false;
            doubleClicked = false;
            moved = false;
            dragged = false;
            enteredMe = false;
            exitedMe = false;
            enteredChild = false;
            exitedChild = false;
            lastClickCount = 0;
        }
    }

    private static class TestSceneWithSubScenes {
        private SubScene subScene1, subScene2;
        private Rectangle innerRect1, innerRect2;
        private Group root, parent1, parent2;
        private Scene scene;
        private boolean called = false;
        private PickResult lastPickResult = null;

        public TestSceneWithSubScenes() {
            this(false, false);
        }

        public TestSceneWithSubScenes(boolean depthBuffer, boolean subDepthBuffer) {
            root = new Group();
            scene = new Scene(root, 500, 400, depthBuffer);

            innerRect1 = new Rectangle(50, 50);
            innerRect1.setTranslateX(100);
            innerRect1.setTranslateY(10);
            subScene1 = new SubScene(new Group(innerRect1), 200, 100,
                    subDepthBuffer, null);
            subScene1.setTranslateX(10);
            parent1 = new Group(subScene1);
            parent1.setTranslateX(10);
            parent1.setTranslateY(10);

            innerRect2 = new Rectangle(50, 50);
            innerRect2.setTranslateX(100);
            innerRect2.setTranslateY(10);
            subScene2 = new SubScene(new Group(innerRect2), 200, 100);
            subScene2.setTranslateX(20);

            parent2 = new Group(subScene2);
            parent2.setTranslateX(230);
            parent2.setTranslateY(10);


            root.getChildren().addAll(parent1, parent2);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
        }

        public void processEvent(EventType<MouseEvent> type, double x, double y) {
            scene.impl_processMouseEvent(
                    MouseEventGenerator.generateMouseEvent(type, x, y));
        }

        public Object getCursorFrame() {
            return ((StubScene) scene.impl_getPeer()).getCursor();
        }

        public EventHandler<MouseEvent> handler(final EventTarget target,
                final double x, final double y) {

            return event -> {
                if (target != null) assertSame(target, event.getTarget());
                if (x != DONT_TEST) assertEquals(x, event.getX(), 0.00001);
                if (y != DONT_TEST) assertEquals(y, event.getY(), 0.00001);
                assertEquals(0.0, event.getZ(), 0.00001);
                lastPickResult = event.getPickResult();
                called = true;
            };
        }

        public void assertCalled() {
            assertTrue(called);
        }
    }

}
