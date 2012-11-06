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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.javafx.tk.TKScene;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.test.MouseEventGenerator;

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
    /*                         DRAG INITIATION                              */
    /************************************************************************/
    
    
    @Test
    public void dragDetectionShouldUseHysteresis() {
        Node n = oneNode();
        MouseEventGenerator gen = new MouseEventGenerator();
        
        EventHandler<MouseEvent> thirdEventFailsHysteresis = 
                new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                counter++;
                assertTrue((counter != 3 && !event.isDragDetect()) ||
                        (counter == 3 && event.isDragDetect()));
            }
        };

        n.addEventHandler(MouseEvent.MOUSE_PRESSED, thirdEventFailsHysteresis);
        n.addEventHandler(MouseEvent.MOUSE_DRAGGED, thirdEventFailsHysteresis);
        n.addEventHandler(MouseEvent.MOUSE_RELEASED, thirdEventFailsHysteresis);
        
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 52, 48));
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertFalse(detected);

        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));
        assertFalse(detected);

        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertFalse(detected);
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 52, 48));
        assertFalse(detected);
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
        assertFalse(detected);

        /* doDetect fires detection */
        n.setOnMouseDragged(doDetect);
        
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        assertTrue(detected);
        detected = false;

        /* but fires it only once */
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
        assertFalse(detected);
        
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertFalse(detected);
        
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        assertTrue(detected);
        detected = false;

        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));
        assertFalse(detected);
    }
    
    @Test
    public void startDragShouldNotBeCalledIfNothingPutOnDragboard() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();
        
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                n.startDragAndDrop(TransferMode.COPY);
            }
        });

        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        n.getScene().impl_processMouseEvent(
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

        n.getScene().impl_processMouseEvent(
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
        n.setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(dragSource, event.getGestureSource());
                counter++;
            }
        });

        n.getScene().impl_processMouseEvent(
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
        n.setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(dragSource, event.getGestureSource());
                counter++;
            }
        });

        n.getScene().impl_processMouseEvent(
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
        n.setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.ANY);
                if (counter == 0) {
                    assertNull(event.getGestureTarget());
                } else {
                    assertSame(n, event.getGestureTarget());
                }
                counter++;
            }
        });
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(n, event.getGestureTarget());
                counter++;
            }
        });

        n.getScene().impl_processMouseEvent(
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
        n.setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.ANY);
                if (counter == 0) {
                    assertNull(event.getGestureTarget());
                } else {
                    assertSame(n.getParent(), event.getGestureTarget());
                }
                counter++;
            }
        });
        n.getParent().setOnDragOver(acceptAny);
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(n.getParent(), event.getGestureTarget());
                counter++;
            }
        });

        n.getScene().impl_processMouseEvent(
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
        n.getScene().setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                Dragboard db = n.getScene().startDragAndDrop(TransferMode.ANY);
                ClipboardContent cc = new ClipboardContent();
                cc.putString("Hello");
                db.setContent(cc);
            }
        });
        n.setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(n.getScene(), event.getGestureSource());
                counter++;
            }
        });

        n.getScene().impl_processMouseEvent(
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
        n.getScene().setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.ANY);
                if (counter == 0) {
                    assertNull(event.getGestureTarget());
                } else {
                    assertSame(n.getScene(), event.getGestureTarget());
                }
                counter++;
            }
        });
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(n.getScene(), event.getGestureTarget());
                counter++;
            }
        });

        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
                assertSame(TransferMode.COPY, event.getTransferMode());
                assertSame(TransferMode.COPY, event.getAcceptedTransferMode());
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertSame(TransferMode.COPY, event.getTransferMode());
                assertSame(TransferMode.COPY, event.getAcceptedTransferMode());
                event.acceptTransferModes(TransferMode.MOVE);
                event.setDropCompleted(true);
                assertSame(TransferMode.COPY, event.getTransferMode());
                assertSame(TransferMode.MOVE, event.getAcceptedTransferMode());
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                try {
                    event.acceptTransferModes(TransferMode.LINK);
                    fail("Exception was not thrown");
                } catch (IllegalStateException e) {
                    /* expceted */
                    counter++;
                }
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.ANY);
                event.setDropCompleted(true);
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.setDropCompleted(true);
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.setDropCompleted(false);
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.setDropCompleted(false);
            }
        });
        n.getParent().setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.setDropCompleted(true);
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        trgt.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                event.setDropCompleted(true);
            }
        });
        src.getParent().setOnDragDone(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                assertEquals(TransferMode.MOVE, event.getTransferMode());
                assertEquals(TransferMode.MOVE, event.getAcceptedTransferMode());
                counter++;
            }
        });

        /* start drag */
        src.getScene().impl_processMouseEvent(
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
        n.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.getParent().setOnDragEntered(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);
        
        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(1, counter);
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
                new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        n.setOnDragExited(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
                gen.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        assertEquals(0, counter);
        
        /* drag */
        toolkit.dragTo(52, 52, TransferMode.MOVE);
        assertEquals(0, counter);

        toolkit.dragTo(150, 52, TransferMode.MOVE);
        assertEquals(1, counter);
    }
    
    @Test 
    public void dragSourceShouldGetEnteredWhenReturned() {
        final Node n = oneNode();
        final MouseEventGenerator gen = new MouseEventGenerator();

        dragSource = n;
        n.setOnMousePressed(doDetect);
        n.setOnDragDetected(stringSource(TransferMode.ANY));
        n.setOnDragOver(acceptAny);
        n.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        n.getScene().impl_processMouseEvent(
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
        trgt.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        src.getScene().impl_processMouseEvent(
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
        trgt.setOnDragExited(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        src.getScene().impl_processMouseEvent(
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
        trgt.getParent().setOnDragExited(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        src.getScene().impl_processMouseEvent(
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
                new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                counter++;
            }
        });

        /* start drag */
        src.getScene().impl_processMouseEvent(
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
    /*                             HELPER CODE                              */
    /************************************************************************/
    
    // Event handlers
    
    private final EventHandler<MouseEvent> dontDetect = 
            new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent event) {
            event.setDragDetect(false);
        }
    };

    private final EventHandler<MouseEvent> doDetect = 
            new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent event) {
            event.setDragDetect(true);
        }
    };
    
    private final EventHandler<MouseEvent> detector = 
            new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent event) {
            detected = true;
        }
    };
    
    private EventHandler<MouseEvent> stringSource(final TransferMode... tms) { 
        return new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                Dragboard db = dragSource.startDragAndDrop(tms);
                ClipboardContent cc = new ClipboardContent();
                cc.putString("Hello");
                db.setContent(cc);
            }
        };
    }
    
    private final EventHandler<DragEvent> acceptAny = 
            new EventHandler<DragEvent>() {
        @Override public void handle(DragEvent event) {
            event.acceptTransferModes(TransferMode.ANY);
        }
    };

    private final EventHandler<DragEvent> acceptCopyOrMove = 
            new EventHandler<DragEvent>() {
        @Override public void handle(DragEvent event) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
    };
    
    private final EventHandler<DragEvent> acceptCopy = 
            new EventHandler<DragEvent>() {
        @Override public void handle(DragEvent event) {
            event.acceptTransferModes(TransferMode.COPY);
        }
    };
    
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
        
        @Override public void initSecurityContext() {
        }
    }
    
    private class DndToolkit implements StubToolkit.DndDelegate {

        boolean dragging = false;
        TKDragSourceListener srcListener;
        TKDragGestureListener gstrListener;
        TKDropTargetListener trgListener;
        Dragboard db;

        @Override
        public void registerListener(TKDragGestureListener l) {
            this.gstrListener = l;
        }

        @Override
        public void enableDrop(TKDropTargetListener l) {
            this.trgListener = l;
        }
        
        
        @Override
        public Dragboard createDragboard() {
            db = new Dragboard(new ClipboardImpl());
            return db;
        }

        @Override
        public void startDrag(TKScene scene, Set<TransferMode> tm, 
                TKDragSourceListener l, Dragboard dragboard) {
            ((ClipboardImpl) db.impl_getPeer()).setTransferModes(tm);
            dragging = true;
            srcListener = l;
        }

        @Override
        public DragEvent convertDragEventToFx(Object event, Dragboard dragboard) {
            DragEvent de = (DragEvent) event;
            return de.copyFor(de.getSource(), de.getTarget(), 
                    de.getGestureSource(), de.getGestureTarget(), dragboard);
        }
        
        public TransferMode dragTo(double x, double y, TransferMode tm) {
            return trgListener.dragOver(x, y, x, y, tm, db);
        }
        
        public TransferMode drop(double x, double y, TransferMode tm) {
            return trgListener.drop(x, y, x, y, tm, db);
        }
        
        public void done(TransferMode tm) {
            srcListener.dragDropEnd(0, 0, 0, 0, tm, db);
        }
        
        public void stopDrag() {
            dragging = false;
        }
        
    }
}
