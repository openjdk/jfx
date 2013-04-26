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

package javafx.scene;

import com.sun.javafx.test.MouseEventGenerator;
import static org.junit.Assert.*;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.Test;

import com.sun.javafx.pgstub.StubScene;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.paint.Color;

public class MouseTest {

    private boolean called;

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
                new EventHandler<MouseEvent>() {

            @Override public void handle(MouseEvent event) {
                assertEquals(Double.NaN, event.getX(), 0.0001);
                assertEquals(Double.NaN, event.getY(), 0.0001);
                assertEquals(251.0, event.getSceneX(), 0.0001);
                assertEquals(251.0, event.getSceneY(), 0.0001);
            }
        });

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
        final MouseEventGenerator generator = new MouseEventGenerator();
        final Group root = new Group();
        final Scene scene = new Scene(root, 400, 400);
        final SubScene sub = new SubScene(new Group(), 100, 100);
        sub.setTranslateX(100);
        sub.setTranslateY(100);
        sub.setFill(Color.BLACK);
        root.getChildren().add(sub);

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                assertSame(sub, event.getTarget());
                called = true;
            }
        });

        called = false;
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        assertTrue(called);
    }

    @Test
    public void subSceneShouldNotBePickedWithoutFill() {
        final MouseEventGenerator generator = new MouseEventGenerator();
        final Group root = new Group();
        final Scene scene = new Scene(root, 400, 400);
        final SubScene sub = new SubScene(new Group(), 100, 100);
        sub.setTranslateX(100);
        sub.setTranslateY(100);
        root.getChildren().add(sub);

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                assertSame(scene, event.getTarget());
                called = true;
            }
        });

        called = false;
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        assertTrue(called);
    }

    private static class SimpleTestScene {

        MouseEventTracker sceneTracker;
        MouseEventTracker groupTracker;
        MouseEventTracker bigSquareTracker;
        MouseEventTracker smallSquareTracker;
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
            Rectangle smallSquare = new Rectangle(200, 200, 100, 100);
            bigSquare.setTranslateZ(-1);

            smallSquare.setCursor(Cursor.HAND);
            group.setCursor(Cursor.TEXT);

            group.getChildren().add(bigSquare);
            group.getChildren().add(smallSquare);

            root.getChildren().add(group);

            sceneTracker = new MouseEventTracker(scene);
            groupTracker = new MouseEventTracker(group);
            bigSquareTracker = new MouseEventTracker(bigSquare);
            smallSquareTracker = new MouseEventTracker(smallSquare);

            scene.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    moused = true;
                }
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

            node.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    pressed = true;
                }
            });

            node.setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    released = true;
                }
            });

            node.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() == 1) {
                        clicked = true;
                    } else if (event.getClickCount() == 2) {
                        doubleClicked = true;
                    }
                }
            });

            node.setOnMouseMoved(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    moved = true;
                }
            });

            node.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    dragged = true;
                }
            });

            node.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getTarget() == MouseEventTracker.this.node) {
                        enteredMe = true;
                    } else {
                        enteredChild = true;
                    }
                }
            });

            node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getTarget() == MouseEventTracker.this.node) {
                        exitedMe = true;
                    } else {
                        exitedChild = true;
                    }
                }
            });
            
            node.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    if (event.getEventType() != MouseEvent.MOUSE_CLICKED) {
                        lastClickCount = event.getClickCount();
                        lastIsStill = event.isStillSincePress();
                    }
                }
            });
        }

        public MouseEventTracker(Scene scene) {
            scene.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    enteredMe = true;
                }
            });

            scene.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    exitedMe = true;
                }
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
}
