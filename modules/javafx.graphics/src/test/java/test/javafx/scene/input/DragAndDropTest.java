/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.javafx.tk.TKScene;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Pair;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.test.MouseEventGenerator;
import javafx.event.Event;
import javafx.geometry.Point3D;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.Dragboard;
import javafx.scene.input.DragboardShim;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.PickResult;
import javafx.scene.input.TransferMode;
import javafx.scene.input.TransferMode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DragAndDropTest {

    private DndToolkit toolkit = new DndToolkit();
    private int counter;
    private boolean detected;
    private Node dragSource;

    @Before
    public void setUp() {
        counter = 0;
        detected = false;
        toolkit = new DndToolkit();
        ((StubToolkit) Toolkit.getToolkit()).setDndDelegate(toolkit);
    }

    @After
    public void tearDown() {
        ((StubToolkit) Toolkit.getToolkit()).setDndDelegate(null);
        toolkit = null;
    }

    /************************************************************************/
    /*                      DRAG EVENT CONSTRUCTOR                          */
    /************************************************************************/

    @Test public void testShortConstructor() {
        Rectangle node = new Rectangle(10, 10);
        node.setTranslateX(3);
        node.setTranslateY(2);
        node.setTranslateZ(50);
        Dragboard db = DragboardShim.getDragboard(new ClipboardImpl());
        Rectangle gsrc = new Rectangle(10, 10);
        Rectangle gtrg = new Rectangle(10, 10);

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        DragEvent e = new DragEvent(DragEvent.DRAG_ENTERED, db, 10, 20, 30, 40,
            TransferMode.LINK, gsrc, gtrg, pickRes);

        assertSame(DragEvent.DRAG_ENTERED, e.getEventType());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertSame(db, e.getDragboard());
        assertSame(gsrc, e.getGestureSource());
        assertSame(gtrg, e.getGestureTarget());
        assertSame(TransferMode.LINK, e.getTransferMode());
        assertSame(pickRes, e.getPickResult());
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
        assertFalse(e.isAccepted());
        assertNull(e.getAcceptedTransferMode());
        assertFalse(e.isConsumed());
    }

    @Test public void testShortConstructorWithoutPickResult() {
        Dragboard db = DragboardShim.getDragboard(new ClipboardImpl());
        Rectangle gsrc = new Rectangle(10, 10);
        Rectangle gtrg = new Rectangle(10, 10);
        DragEvent e = new DragEvent(DragEvent.DRAG_ENTERED, db, 10, 20, 30, 40,
            TransferMode.LINK, gsrc, gtrg, null);

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
        Dragboard db = DragboardShim.getDragboard(new ClipboardImpl());
        Rectangle gsrc = new Rectangle(10, 10);
        Rectangle gtrg = new Rectangle(10, 10);

        PickResult pickRes = new PickResult(node, new Point3D(15, 25, 100), 33);
        DragEvent e = new DragEvent(n1, n2, DragEvent.DRAG_ENTERED, db,
                10, 20, 30, 40, TransferMode.LINK, gsrc, gtrg, pickRes);
        assertSame(n1, e.getSource());
        assertSame(n2, e.getTarget());
        assertSame(DragEvent.DRAG_ENTERED, e.getEventType());
        assertEquals(18, e.getX(), 10e-20);
        assertEquals(27, e.getY(), 10e-20);
        assertEquals(150, e.getZ(), 10e-20);
        assertEquals(10, e.getSceneX(), 10e-20);
        assertEquals(20, e.getSceneY(), 10e-20);
        assertEquals(30, e.getScreenX(), 10e-20);
        assertEquals(40, e.getScreenY(), 10e-20);
        assertSame(TransferMode.LINK, e.getTransferMode());
        assertSame(db, e.getDragboard());
        assertSame(gsrc, e.getGestureSource());
        assertSame(gtrg, e.getGestureTarget());
        assertSame(pickRes, e.getPickResult());
        assertFalse(e.isConsumed());
        assertFalse(e.isAccepted());
        assertNull(e.getAcceptedTransferMode());
    }

    @Test public void testLongConstructorWithoutPickResult() {
        Rectangle n1 = new Rectangle(10, 10);
        Rectangle n2 = new Rectangle(10, 10);
        Dragboard db = DragboardShim.getDragboard(new ClipboardImpl());
        Rectangle gsrc = new Rectangle(10, 10);
        Rectangle gtrg = new Rectangle(10, 10);
        DragEvent e = new DragEvent(n1, n2, DragEvent.DRAG_ENTERED, db,
                10, 20, 30, 40, TransferMode.LINK, gsrc, gtrg, null);

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

    @Test public void shortConstructorMakesDropAccepted() {
        DragEvent e = new DragEvent(DragEvent.DRAG_DROPPED,
                DragboardShim.getDragboard(
                        new ClipboardImpl()), 10, 20, 30, 40,
                TransferMode.LINK, new Rectangle(), new Rectangle(), null);
        assertSame(DragEvent.DRAG_DROPPED, e.getEventType());
        assertTrue(e.isAccepted());
        assertSame(TransferMode.LINK, e.getAcceptedTransferMode());
    }

    @Test
    public void shortConstructorMakesDoneAccepted() {
        DragEvent e = new DragEvent(DragEvent.DRAG_DONE,
                DragboardShim.getDragboard(
                        new ClipboardImpl()), 10, 20, 30, 40,
                TransferMode.LINK, new Rectangle(), new Rectangle(), null);
        assertSame(DragEvent.DRAG_DONE, e.getEventType());
        assertTrue(e.isAccepted());
        assertSame(TransferMode.LINK, e.getAcceptedTransferMode());
    }

    @Test public void longConstructorMakesDropAccepted() {
        DragEvent e = new DragEvent(new Rectangle(), new Rectangle(),
                DragEvent.DRAG_DROPPED,
                DragboardShim.getDragboard(
                new ClipboardImpl()), 10, 20, 30, 40,
                TransferMode.LINK, new Rectangle(), new Rectangle(), null);
        assertSame(DragEvent.DRAG_DROPPED, e.getEventType());
        assertTrue(e.isAccepted());
        assertSame(TransferMode.LINK, e.getAcceptedTransferMode());
    }

    @Test public void longConstructorMakesDoneAccepted() {
        DragEvent e = new DragEvent(new Rectangle(), new Rectangle(),
                DragEvent.DRAG_DONE,
                DragboardShim.getDragboard(
                        new ClipboardImpl()), 10, 20, 30, 40,
                TransferMode.LINK, new Rectangle(), new Rectangle(), null);
        assertSame(DragEvent.DRAG_DONE, e.getEventType());
        assertTrue(e.isAccepted());
        assertSame(TransferMode.LINK, e.getAcceptedTransferMode());
    }

    /************************************************************************/
    /*                         DRAG INITIATION                              */
    /************************************************************************/


    @Test
    public void dragDetectionShouldUseHysteresis() {
        Node n = oneNode();
        MouseEventGenerator gen = new MouseEventGenerator();

        EventHandler<MouseEvent> thirdEventFailsHysteresis =
                event -> {
                    counter++;
                    assertTrue((counter != 3 && !event.isDragDetect()) ||
                            (counter == 3 && event.isDragDetect()));
                };

        n.addEventHandler(MouseEvent.MOUSE_PRESSED, thirdEventFailsHysteresis);
        n.addEventHandler(MouseEvent.MOUSE_DRAGGED, thirdEventFailsHysteresis);
        n.addEventHandler(MouseEvent.MOUSE_RELEASED, thirdEventFailsHysteresis);

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 52, 48));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));

        assertEquals(5, counter);
    }

    @Test
    public void dragShouldNotBeDetectedBasedOnMoveOrRelase() {
        Node n = oneNode();
        MouseEventGenerator gen = new MouseEventGenerator();

        n.setOnDragDetected(detector);
        n.setOnMousePressed(dontDetect);
        n.setOnMouseMoved(doDetect);
        n.setOnMouseReleased(doDetect);

        /* dontDetect prevents detection */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertFalse(detected);

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));
        assertFalse(detected);

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_MOVED, 60, 60));
        assertFalse(detected);
    }

    @Test
    public void dragShouldBeDetectedBasedOnMouseEvent() {
        Node n = oneNode();
        MouseEventGenerator gen = new MouseEventGenerator();

        n.setOnDragDetected(detector);
        n.setOnMousePressed(dontDetect);
        n.setOnMouseDragged(dontDetect);
        n.setOnMouseReleased(doDetect);

        /* dontDetect prevents detection */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertFalse(detected);
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 52, 48));
        assertFalse(detected);
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
        assertFalse(detected);

        /* doDetect fires detection */
        n.setOnMouseDragged(doDetect);

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        assertTrue(detected);
        detected = false;

        /* but fires it only once */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
        assertFalse(detected);

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));
        assertFalse(detected);
    }

    @Test
    public void dragDetectionShouldBeOverridable() {
        Node n = oneNode();
        MouseEventGenerator gen = new MouseEventGenerator();

        n.setOnDragDetected(detector);
        n.setOnMousePressed(doDetect);
        n.setOnMouseDragged(dontDetect);
        n.getParent().setOnMousePressed(dontDetect);
        n.getParent().setOnMouseDragged(doDetect);

        /* dontDetect prevents detection */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertFalse(detected);

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        assertTrue(detected);
        detected = false;

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));
        assertFalse(detected);
    }

    @Test
    public void startDragShouldNotBeCalledIfNothingPutOnDragboard() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(event -> {
            n.startDragAndDrop(TransferMode.COPY);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));

        assertFalse(toolkit.dragging);
    }

    @Test
    public void startDragShouldBeCalledIfStringPutOnDragboard() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertTrue(toolkit.dragging);
    }

    /************************************************************************/
    /*                           SOURCE/TARGET                              */
    /************************************************************************/

    @Test
    public void nodeThatCallsStartDndShouldBecomeGestureSource() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n.getParent();
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(event -> {
            assertSame(dragSource, event.getGestureSource());
            counter++;
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(52, 52, TransferMode.COPY);

        assertEquals(1, counter);
    }

    @Test
    public void parentThatCallsStartDndShouldBecomeGestureSource() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n.getParent();
        n.setOnMousePressed(doDetect);
        n.getParent().setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(event -> {
            assertSame(dragSource, event.getGestureSource());
            counter++;
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(52, 52, TransferMode.COPY);

        assertEquals(1, counter);
    }

    @Test
    public void nodeThatAcceptsDragShouldBecomeGestureTarget() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            if (counter == 0) {
                assertNull(event.getGestureTarget());
            } else {
                assertSame(n, event.getGestureTarget());
            }
            counter++;
        });
        n.setOnDragDropped(event -> {
            assertSame(n, event.getGestureTarget());
            counter++;
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(51, 51, TransferMode.COPY);
        toolkit.dragTo(52, 52, TransferMode.COPY);
        toolkit.drop(52, 52, TransferMode.COPY);

        assertEquals(3, counter);
    }

    @Test
    public void parentThatAcceptsDragShouldBecomeGestureTarget() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            if (counter == 0) {
                assertNull(event.getGestureTarget());
            } else {
                assertSame(n.getParent(), event.getGestureTarget());
            }
            counter++;
        });
        n.getParent().setOnDragOver(acceptAny);
        n.setOnDragDropped(event -> {
            assertSame(n.getParent(), event.getGestureTarget());
            counter++;
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(51, 51, TransferMode.COPY);
        toolkit.dragTo(52, 52, TransferMode.COPY);
        toolkit.drop(52, 52, TransferMode.COPY);

        assertEquals(3, counter);
    }

    @Test
    public void sceneCanBecomeGestureSource() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        n.setOnMousePressed(doDetect);
        n.getScene().setOnDragDetected(event -> {
            Dragboard db = n.getScene().startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("Hello");
            db.setContent(cc);
        });
        n.setOnDragOver(event -> {
            assertSame(n.getScene(), event.getGestureSource());
            counter++;
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(52, 52, TransferMode.COPY);

        assertEquals(1, counter);
    }

    @Test
    public void sceneCanBecomeGestureTarget() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.getScene().setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            if (counter == 0) {
                assertNull(event.getGestureTarget());
            } else {
                assertSame(n.getScene(), event.getGestureTarget());
            }
            counter++;
        });
        n.setOnDragDropped(event -> {
            assertSame(n.getScene(), event.getGestureTarget());
            counter++;
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(51, 51, TransferMode.COPY);
        toolkit.dragTo(52, 52, TransferMode.COPY);
        toolkit.drop(52, 52, TransferMode.COPY);

        assertEquals(3, counter);
    }

    /************************************************************************/
    /*                           TRANSFER MODES                             */
    /************************************************************************/

    @Test
    public void defaultTransferModeShouldBeUsedIfSupported() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertSame(TransferMode.LINK, toolkit.dragTo(52, 52, TransferMode.LINK));
    }

    @Test
    public void defaultTransferModeShouldNotBeUsedIfNotSupported() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.COPY));
        n.setOnDragOver(acceptAny);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertSame(TransferMode.COPY, toolkit.dragTo(52, 52, TransferMode.LINK));
    }

    @Test
    public void mostCommonTransferModeShouldBeChosen() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.COPY_OR_MOVE));
        n.setOnDragOver(acceptAny);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertSame(TransferMode.MOVE, toolkit.dragTo(52, 52, TransferMode.LINK));
    }

    @Test
    public void transferModeAcceptanceShouldBeOverridable_restriction() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.COPY_OR_MOVE));
        n.setOnDragOver(acceptAny);
        n.getParent().setOnDragOver(acceptCopy);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertSame(TransferMode.COPY, toolkit.dragTo(52, 52, TransferMode.MOVE));
    }

    @Test
    public void transferModeAcceptanceShouldBeOverridable_loosening() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.COPY_OR_MOVE));
        n.setOnDragOver(acceptCopy);
        n.getParent().setOnDragOver(acceptAny);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertSame(TransferMode.MOVE, toolkit.dragTo(52, 52, TransferMode.MOVE));
    }

    @Test
    public void noTransferShouldHappenWhenUnsupportedModeIsAccepted() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.LINK));
        n.setOnDragOver(acceptCopyOrMove);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertNull(toolkit.dragTo(52, 52, TransferMode.MOVE));
    }

    @Test
    public void noTransferShouldHappenWhenNotAccepted() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag */
        assertNull(toolkit.dragTo(52, 52, TransferMode.MOVE));
    }

    @Test
    public void dropShouldGetAcceptedTransferMode() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopy);
        n.setOnDragDropped(event -> {
            counter++;
            assertSame(TransferMode.COPY, event.getTransferMode());
            assertSame(TransferMode.COPY, event.getAcceptedTransferMode());
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        toolkit.drop(52, 52, TransferMode.MOVE);

        assertEquals(1, counter);
    }

    @Test
    public void shouldBePossibleToAcceptInDrop() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopy);
        n.setOnDragDropped(event -> {
            assertSame(TransferMode.COPY, event.getTransferMode());
            assertSame(TransferMode.COPY, event.getAcceptedTransferMode());
            event.acceptTransferModes(TransferMode.MOVE);
            event.setDropCompleted(true);
            assertSame(TransferMode.COPY, event.getTransferMode());
            assertSame(TransferMode.MOVE, event.getAcceptedTransferMode());
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(TransferMode.MOVE, toolkit.drop(52, 52, TransferMode.LINK));
    }

    @Test
    public void acceptingNonSupportedTransferModeInDropShouldThrowException() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.COPY_OR_MOVE));
        n.setOnDragOver(acceptCopy);
        n.setOnDragDropped(event -> {
            try {
                event.acceptTransferModes(TransferMode.LINK);
                fail("Exception was not thrown");
            } catch (IllegalStateException e) {
                /* expceted */
                counter++;
            }
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        toolkit.drop(52, 52, TransferMode.LINK);

        assertEquals(1, counter);
    }

    @Test
    public void modifyingStaticArraysShouldNotInfluenceResult() {
        TransferMode.ANY[0] = TransferMode.LINK;
        TransferMode.ANY[1] = TransferMode.LINK;
        TransferMode.ANY[2] = TransferMode.LINK;

        TransferMode.COPY_OR_MOVE[0] = TransferMode.LINK;
        TransferMode.COPY_OR_MOVE[1] = TransferMode.LINK;

        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopyOrMove);
        n.setOnDragDropped(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.setDropCompleted(true);
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        assertSame(TransferMode.COPY, toolkit.dragTo(52, 52, TransferMode.COPY));
        assertSame(TransferMode.COPY, toolkit.drop(52, 52, TransferMode.COPY));

        TransferMode.ANY[0] = TransferMode.COPY;
        TransferMode.ANY[1] = TransferMode.MOVE;
        TransferMode.ANY[2] = TransferMode.LINK;

        TransferMode.COPY_OR_MOVE[0] = TransferMode.COPY;
        TransferMode.COPY_OR_MOVE[1] = TransferMode.MOVE;
    }

    /************************************************************************/
    /*                           GESTURE FINISH                             */
    /************************************************************************/

    @Test
    public void dropShouldBeAcceptedByCompletion() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopy);
        n.setOnDragDropped(event -> {
            event.setDropCompleted(true);
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertSame(TransferMode.COPY, toolkit.drop(52, 52, TransferMode.MOVE));
    }

    @Test
    public void dropShouldNotBeAcceptedWithUnsuccessfulCompletion() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopy);
        n.setOnDragDropped(event -> {
            event.setDropCompleted(false);
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertNull(toolkit.drop(52, 52, TransferMode.MOVE));
    }

    @Test
    public void dropShouldNotBeAcceptedWithoutCompletion() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopy);

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertNull(toolkit.drop(52, 52, TransferMode.MOVE));
    }

    @Test
    public void dropCompletionShouldBeOverridable() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptCopyOrMove);
        n.setOnDragDropped(event -> {
            event.setDropCompleted(false);
        });
        n.getParent().setOnDragDropped(event -> {
            event.setDropCompleted(true);
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertSame(TransferMode.MOVE, toolkit.drop(52, 52, TransferMode.MOVE));
    }

    @Test
    public void dropDoneShouldBeSentToGestureSource() {
        final Node[] ns = twoNodes();
        Node src = ns[0];
        Node trgt = ns[1];

        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = src;
        src.setOnMousePressed(doDetect);
        src.setOnDragDetected(stringSource(TransferMode.ANY));
        trgt.setOnDragOver(acceptCopyOrMove);
        trgt.setOnDragDropped(event -> {
            event.setDropCompleted(true);
        });
        src.getParent().setOnDragDone(event -> {
            Assert.assertEquals(TransferMode.MOVE, event.getTransferMode());
            Assert.assertEquals(TransferMode.MOVE, event.getAcceptedTransferMode());
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(src.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /* drag and drop*/
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        toolkit.dragTo(252, 52, TransferMode.MOVE);
        toolkit.drop(252, 52, TransferMode.COPY);
        toolkit.done(TransferMode.MOVE);
        assertEquals(counter, 1);
    }

    /************************************************************************/
    /*                            ENTERED/EXITED                            */
    /************************************************************************/

    @Test
    public void dragSourceShouldGetEnteredImmediately() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        n.setOnDragEntered(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(1, counter);
    }

    @Test
    public void dragSourcesParentShouldGetEnteredImmediately() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        n.getParent().setOnDragEntered(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(1, counter);
    }

    @Test
    public void dragSourcesSubScenesParentShouldGetEnteredImmediately() {
        final Node[] ns = oneNodeInSubScene();
        final Node n = ns[0];
        final Node subScene = ns[1];

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        subScene.setOnDragEntered(event -> {
            counter++;
        });
        subScene.getParent().setOnDragEntered(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                MouseEventGenerator.generateMouseEvent(
                    MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(2, counter);
    }

    @Test
    public void dragSourcesParentShouldGetEnteredTargetTwice() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        n.getParent().addEventHandler(DragEvent.DRAG_ENTERED_TARGET,
                event -> {
                    counter++;
                }
        );

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(2, counter);
    }

    @Test
    public void dragSourceShouldGetExitedWhenLeft() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        n.setOnDragExited(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(150, 52, TransferMode.MOVE);
        assertEquals(1, counter);
    }

    @Test
    public void dragSourcesSubScenesParentShouldGetExitedWhenLeft() {
        final Node[] ns = oneNodeInSubScene();
        final Node n = ns[0];
        final Node subScene = ns[1];

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        subScene.setOnDragExited(event -> {
            counter++;
        });
        subScene.getParent().setOnDragExited(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                MouseEventGenerator.generateMouseEvent(
                    MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(250, 52, TransferMode.MOVE);
        assertEquals(2, counter);
    }

    @Test
    public void dragSourceShouldGetEnteredWhenReturned() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        n.setOnDragEntered(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(1, counter);

        toolkit.dragTo(150, 52, TransferMode.MOVE);
        assertEquals(1, counter);

        toolkit.dragTo(60, 52, TransferMode.MOVE);
        assertEquals(2, counter);
    }

    @Test
    public void anotherNodeShouldGetEntered() {
        final Node[] ns = twoNodes();
        Node src = ns[0];
        Node trgt = ns[1];
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = src;
        src.setOnMousePressed(doDetect);
        src.setOnDragDetected(stringSource(TransferMode.ANY));
        src.setOnDragOver(acceptAny);
        trgt.setOnDragEntered(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(src.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(250, 52, TransferMode.MOVE);
        assertEquals(1, counter);
    }

    @Test
    public void anotherNodeShouldGetExited() {
        final Node[] ns = twoNodes();
        Node src = ns[0];
        Node trgt = ns[1];
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = src;
        src.setOnMousePressed(doDetect);
        src.setOnDragDetected(stringSource(TransferMode.ANY));
        src.setOnDragOver(acceptAny);
        trgt.setOnDragExited(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(src.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(250, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(150, 52, TransferMode.MOVE);
        assertEquals(1, counter);
    }

    @Test
    public void parentShouldNotGetExitedWhenDraggingOverChildren() {
        final Node[] ns = twoNodes();
        Node src = ns[0];
        Node trgt = ns[1];
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = src;
        src.setOnMousePressed(doDetect);
        src.setOnDragDetected(stringSource(TransferMode.ANY));
        src.setOnDragOver(acceptAny);
        trgt.getParent().setOnDragExited(event -> {
            counter++;
        });

        /* start drag */
        SceneHelper.processMouseEvent(src.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(250, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(50, 52, TransferMode.MOVE);
        assertEquals(0, counter);
    }

    @Test
    public void parentShouldGetExitedTargetWhenDraggingOverChildren() {
        final Node[] ns = twoNodes();
        Node src = ns[0];
        Node trgt = ns[1];
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = src;
        src.setOnMousePressed(doDetect);
        src.setOnDragDetected(stringSource(TransferMode.ANY));
        src.setOnDragOver(acceptAny);
        trgt.getParent().addEventHandler(DragEvent.DRAG_EXITED_TARGET,
                event -> {
                    counter++;
                }
        );

        /* start drag */
        SceneHelper.processMouseEvent(src.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);

        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(250, 52, TransferMode.MOVE);
        assertEquals(1, counter);

        toolkit.dragTo(50, 52, TransferMode.MOVE);
        assertEquals(2, counter);
    }

    /************************************************************************/
    /*                              DRAGVIEW                                */
    /************************************************************************/
    @Test
    public void startDragShouldNotBeCalledIfNothingPutOnDragboardWithDragView() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();
        final Image img = new Image("file:testImg_" + 100 + "x" + 100 + ".png");

        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(event -> {
            Dragboard db = n.startDragAndDrop(TransferMode.COPY);
            db.setDragView(img);
            db.setDragViewOffsetX(20);
            db.setDragViewOffsetX(15);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));

        assertFalse(toolkit.dragging);
    }

    @Test
    public void startDragShouldBeCalledIfStringPutOnDragboardsWithDragView() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();
        final Image img = new Image("file:testImg_" + 100 + "x" + 100 + ".png");

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("Hello");
            db.setContent(cc);
            db.setDragView(img);
            db.setDragViewOffsetX(20);
            db.setDragViewOffsetX(15);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        assertTrue(toolkit.dragging);
    }

    @Test
    public void changeDragViewInParentHandlerShouldBePossible() {
        final Node n = oneNode();
        final Node parent = n.getParent();
        final MouseEventGenerator gen = new MouseEventGenerator();
        final Image img = new Image("file:testImg_" + 100 + "x" + 100 + ".png");
        final Image imgParent = new Image("file:testImg_" + 200 + "x" + 200 + ".png");

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("Hello");
            db.setContent(cc);
            db.setDragView(img);
            db.setDragViewOffsetX(20);
            db.setDragViewOffsetY(15);
        });

        parent.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("HelloParent");
            db.setContent(cc);

            assertEquals(img, db.getDragView());
            assertEquals(20, db.getDragViewOffsetX(), 1e-10);
            assertEquals(15, db.getDragViewOffsetY(), 1e-10);

            db.setDragView(imgParent);
            db.setDragViewOffsetX(40);
            db.setDragViewOffsetY(55);

            assertEquals(imgParent, db.getDragView());
            assertEquals(40, db.getDragViewOffsetX(), 1e-10);
            assertEquals(55, db.getDragViewOffsetY(), 1e-10);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
    }

    @Test
    public void settingDragViewAndCursorPositionShouldReturnSameResults() {
        final Node n = oneNode();
        final Node parent = n.getParent();
        final MouseEventGenerator gen = new MouseEventGenerator();
        final Image img = new Image("file:testImg_" + 100 + "x" + 100 + ".png");
        final Image imgParent = new Image("file:testImg_" + 200 + "x" + 200 + ".png");

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("Hello");
            db.setContent(cc);
            db.setDragView(img, 20, 15);
        });

        parent.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.ANY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("HelloParent");
            db.setContent(cc);

            assertEquals(img, db.getDragView());
            assertEquals(20, db.getDragViewOffsetX(), 1e-10);
            assertEquals(15, db.getDragViewOffsetY(), 1e-10);

            db.setDragView(imgParent);
            db.setDragViewOffsetX(40);
            db.setDragViewOffsetY(55);

            assertEquals(imgParent, db.getDragView());
            assertEquals(40, db.getDragViewOffsetX(), 1e-10);
            assertEquals(55, db.getDragViewOffsetY(), 1e-10);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
    }

    @Test
    public void dragViewShouldBeClearedInSubsequentDragDetectedCall() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();
        final Image img = new Image("file:testImg_" + 100 + "x" + 100 + ".png");

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(event -> {
            Dragboard db = dragSource.startDragAndDrop(TransferMode.ANY);

            assertNull(db.getDragView());
            assertEquals(0, db.getDragViewOffsetX(), 1e-10);
            assertEquals(0, db.getDragViewOffsetY(), 1e-10);

            ClipboardContent cc = new ClipboardContent();
            cc.putString("Hello");
            db.setContent(cc);
            db.setDragView(img);
            db.setDragViewOffsetX(20);
            db.setDragViewOffsetX(15);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 40, 40));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 55, 55));
        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 81, 81));
    }

    /************************************************************************/
    /*                             PICK RESULT                              */
    /************************************************************************/

    @Test
    public void shouldCompute3dCoordinates() {
        Node n = twoNodes()[0];
        n.setTranslateZ(50);
        dragSource = n;

        MouseEventGenerator gen = new MouseEventGenerator();

        counter = 0;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(event -> {
            counter++;
            Assert.assertEquals(52, event.getX(), 0.00001);
            Assert.assertEquals(52, event.getY(), 0.00001);
            Assert.assertEquals(0, event.getZ(), 0.00001);
        });

        n.getScene().setOnDragOver(event -> {
            counter++;
            Assert.assertEquals(52, event.getX(), 0.00001);
            Assert.assertEquals(52, event.getY(), 0.00001);
            Assert.assertEquals(50, event.getZ(), 0.00001);
        });

        SceneHelper.processMouseEvent(n.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(52, 52, TransferMode.COPY);
        toolkit.drop(252, 52, TransferMode.COPY);
        toolkit.done(TransferMode.COPY);

        assertEquals(2, counter);
    }

    @Test
    public void dragEventsHavePickResult() {
        final Node[] nodes = twoNodes();
        final Node n1 = nodes[0];
        final Node n2 = nodes[1];
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n1;
        n1.setOnMousePressed(doDetect);
        n1.setOnDragDetected(stringSource(TransferMode.ANY));
        n1.setOnDragOver(event -> {
            PickResult pickRes = event.getPickResult();
            assertNotNull(pickRes);
            assertSame(n1, pickRes.getIntersectedNode());
            assertEquals(52, pickRes.getIntersectedPoint().getX(), 0.00001);
            assertEquals(52, pickRes.getIntersectedPoint().getY(), 0.00001);
            assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);
            counter++;
        });
        EventHandler<DragEvent> switchNodeHandler = event -> {
            PickResult pickRes = event.getPickResult();
            assertNotNull(pickRes);
            assertSame(n2, pickRes.getIntersectedNode());
            assertEquals(252, pickRes.getIntersectedPoint().getX(), 0.00001);
            assertEquals(52, pickRes.getIntersectedPoint().getY(), 0.00001);
            assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);
            event.acceptTransferModes(TransferMode.COPY);
            counter++;
        };
        n1.setOnDragExited(switchNodeHandler);
        n2.setOnDragEntered(switchNodeHandler);
        n2.setOnDragOver(switchNodeHandler);
        n2.setOnDragDropped(switchNodeHandler);
        n1.setOnDragDone(event -> {
            PickResult pickRes = event.getPickResult();
            assertNotNull(pickRes);
            assertNull(pickRes.getIntersectedNode());
            assertEquals(0, pickRes.getIntersectedPoint().getX(), 0.00001);
            assertEquals(0, pickRes.getIntersectedPoint().getY(), 0.00001);
            assertEquals(0, pickRes.getIntersectedPoint().getZ(), 0.00001);
            counter++;
        });

        SceneHelper.processMouseEvent(n1.getScene(),
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        toolkit.dragTo(52, 52, TransferMode.COPY);
        toolkit.dragTo(252, 52, TransferMode.COPY);
        toolkit.drop(252, 52, TransferMode.COPY);
        toolkit.done(TransferMode.COPY);

        assertEquals(6, counter);
    }


    /************************************************************************/
    /*                             HELPER CODE                              */
    /************************************************************************/

    // Event handlers

    private final EventHandler<MouseEvent> dontDetect =
            event -> event.setDragDetect(false);

    private final EventHandler<MouseEvent> doDetect =
            event -> event.setDragDetect(true);

    private final EventHandler<MouseEvent> detector =
            new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent event) {
            detected = true;
        }
    };

    private EventHandler<MouseEvent> stringSource(final TransferMode... tms) {
        return event -> {
            Dragboard db = dragSource.startDragAndDrop(tms);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("Hello");
            db.setContent(cc);
        };
    }

    private final EventHandler<DragEvent> acceptAny =
            event -> event.acceptTransferModes(TransferMode.ANY);

    private final EventHandler<DragEvent> acceptCopyOrMove =
            event -> event.acceptTransferModes(TransferMode.COPY_OR_MOVE);

    private final EventHandler<DragEvent> acceptCopy =
            event -> event.acceptTransferModes(TransferMode.COPY);

    // Scenes

    private static Node oneNode() {
        Group root = new Group();
        getScene(root);

        Rectangle r = new Rectangle(100, 100);
        root.getChildren().add(r);
        return r;
    }

    private static Node[] twoNodes() {
        Group root = new Group();
        getScene(root);

        Rectangle r = new Rectangle(100, 100);
        root.getChildren().add(r);

        Rectangle r2 = new Rectangle(200, 0, 100, 100);
        root.getChildren().add(r2);
        return new Node[] { r, r2 };
    }

    private static Node[] oneNodeInSubScene() {
        Group root = new Group();
        getScene(root);

        Rectangle r = new Rectangle(100, 100);
        SubScene subScene = new SubScene(new Group(r), 200, 200);
        root.getChildren().add(subScene);

        return new Node[] { subScene, r };
    }


    private static Scene getScene(Group root) {
        final Scene scene = new Scene(root, 400, 400);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        return scene;
    }

    // Mock toolkit

    private class ClipboardImpl implements TKClipboard {

        private List<Pair<DataFormat, Object>> content;
        private Set<TransferMode> transferModes;
        private Image image;
        private double offsetX;
        private double offsetY;

        @Override
        public void setSecurityContext(@SuppressWarnings("removal") AccessControlContext ctx) {
        }

        @Override
        public Object getContent(DataFormat df) {
            for (Pair<DataFormat, Object> pair : content) {
                if (pair.getKey() == df) {
                    return pair.getValue();
                }
            }
            return null;
        }

        @Override
        public Set<DataFormat> getContentTypes() {
            Set<DataFormat> types = new HashSet<DataFormat>();
            for (Pair<DataFormat, Object> pair : content) {
                types.add(pair.getKey());
            }
            return types;
        }

        @Override
        public Set<TransferMode> getTransferModes() {
            return transferModes;
        }

        public void setTransferModes(Set<TransferMode> tms) {
            this.transferModes = tms;
        }

        @Override
        public boolean hasContent(DataFormat df) {
            return content != null && !content.isEmpty();
        }

        @Override
        public boolean putContent(Pair<DataFormat, Object>... pairs) {
            content = new ArrayList<Pair<DataFormat, Object>>();
            content.addAll(Arrays.asList(pairs));
            return true;
        }

        @Override
        public void setDragView(Image image) {
            this.image = image;
        }

        @Override
        public void setDragViewOffsetX(double offsetX) {
            this.offsetX = offsetX;
        }

        @Override
        public void setDragViewOffsetY(double offsetY) {
            this.offsetY = offsetY;
        }

        @Override
        public Image getDragView() {
            return image;
        }

        @Override
        public double getDragViewOffsetX() {
            return offsetX;
        }

        @Override
        public double getDragViewOffsetY() {
            return offsetY;
        }

        public void flush() {
            image = null;
            offsetX = offsetY = 0;
        }
    }

    private class DndToolkit implements StubToolkit.DndDelegate {

        boolean dragging = false;
        TKDragSourceListener srcListener;
        TKDragGestureListener gstrListener;
        TKDropTargetListener trgListener;
        TKClipboard db;

        @Override
        public void registerListener(TKDragGestureListener l) {
            this.gstrListener = l;
        }

        @Override
        public void enableDrop(TKDropTargetListener l) {
            this.trgListener = l;
        }


        @Override
        public TKClipboard createDragboard() {
            db = new ClipboardImpl();
            return db;
        }

        @Override
        public void startDrag(TKScene scene, Set<TransferMode> tm,
                TKDragSourceListener l, Dragboard dragboard) {
            ((ClipboardImpl)db).setTransferModes(tm);
            ((ClipboardImpl)db).flush();
            dragging = true;
            srcListener = l;
        }

        @Override
        public DragEvent convertDragEventToFx(Object event, Dragboard dragboard) {
            DragEvent de = (DragEvent) event;
            return new DragEvent(de.getSource(), de.getTarget(), de.getEventType(),
                    dragboard, de.getSceneX(), de.getSceneY(),
                    de.getScreenX(), de.getScreenY(), de.getTransferMode(),
                    de.getGestureSource(), de.getGestureTarget(), de.getPickResult());
        }

        public TransferMode dragTo(double x, double y, TransferMode tm) {
            return trgListener.dragOver(x, y, x, y, tm);
        }

        public TransferMode drop(double x, double y, TransferMode tm) {
            return trgListener.drop(x, y, x, y, tm);
        }

        public void done(TransferMode tm) {
            srcListener.dragDropEnd(0, 0, 0, 0, tm);
        }

        public void stopDrag() {
            dragging = false;
        }

    }
}
