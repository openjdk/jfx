/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import com.sun.javafx.pgstub.StubToolkit.ScreenConfiguration;
import com.sun.javafx.test.MouseEventGenerator;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.pgstub.StubPopupStage;
import com.sun.javafx.pgstub.StubStage;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import junit.framework.Assert;
import static org.junit.Assert.assertEquals;

public class PopupTest {

    private StubToolkit toolkit;
    private Stage stage;
    private Scene scene;
    private boolean done = false;

    @Before
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);
        stage.show();
        done = false;
        toolkit = (StubToolkit) Toolkit.getToolkit();
    }

    @After
    public void tearDown() {
        stage.hide();
        toolkit.resetScreens();
    }

    private void pulse() {
        toolkit.fireTestPulse();
    }    

    @Test
    public void testShow() {
        // test showing popup with visible parent
        Popup p1 = new Popup();
        p1.show(stage);
        assertTrue(p1.isShowing());
        
        // test showing popup with invisible parent
        stage.hide();
        Popup p2 = new Popup();
        p2.show(stage);
        assertFalse(p2.isShowing());
        
        // test showing popup without parent
        // TODO should result in an exception
//        Popup p3 = new Popup();
//        p3.show(null);
//        assertFalse(p3.isVisible());
    }
        
    @Test
    public void testShowNoAutofix() {
        Popup p1 = new Popup();
        p1.setAutoFix(false);
        p1.show(stage);
        assertTrue(p1.isShowing());
    }
            
    @Test
    public void testShowLocation() {
        Popup p1 = new Popup();
        p1.show(stage, 10, 20);
        assertTrue(p1.isShowing());
        assertEquals(10, p1.getX(), 1e-100);
        assertEquals(20, p1.getY(), 1e-100);
        pulse();
        StubPopupStage peer = (StubPopupStage) p1.impl_getPeer();
        assertEquals(10, peer.x, 1e-100);
        assertEquals(20, peer.y, 1e-100);
    }

    private static final class PopupRoot extends Parent {
        private final Rectangle geomBoundsRect;

        private double layoutBoundsX;
        private double layoutBoundsY;
        private double layoutBoundsWidth;
        private double layoutBoundsHeight;

        public PopupRoot() {
            geomBoundsRect = new Rectangle(0, 0, 100, 100);
            layoutBoundsWidth = 100;
            layoutBoundsHeight = 100;

            getChildren().add(geomBoundsRect);
        }

        public void setGeomBounds(final double x, final double y,
                                  final double width,
                                  final double height) {
            geomBoundsRect.setX(x);
            geomBoundsRect.setY(y);
            geomBoundsRect.setWidth(width);
            geomBoundsRect.setHeight(height);
        }

        public void setLayoutBounds(final double x, final double y,
                                    final double width,
                                    final double height) {
            layoutBoundsX = x;
            layoutBoundsY = y;
            layoutBoundsWidth = width;
            layoutBoundsHeight = height;

            impl_layoutBoundsChanged();
        }

        @Override
        protected Bounds impl_computeLayoutBounds() {
            return new BoundingBox(layoutBoundsX, layoutBoundsY,
                                   layoutBoundsWidth, layoutBoundsHeight);
        }
    }

    @Test
    public void testAnchorPositioning() {
        final Popup popup = new Popup();
        final PopupRoot root = new PopupRoot();

        root.setGeomBounds(-10, 20, 120, 100);
        root.setLayoutBounds(0, 0, 100, 140);

        popup.getScene().setRoot(root);

        popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_BOTTOM_RIGHT);
        popup.show(stage, 400, 400);

        final StubPopupStage peer = (StubPopupStage) popup.impl_getPeer();

        pulse();
        assertEquals(280.0, peer.x, 1e-100);
        assertEquals(260.0, peer.y, 1e-100);

        popup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
        assertEquals(290.0, popup.getAnchorX(), 1e-100);
        assertEquals(260.0, popup.getAnchorY(), 1e-100);

        pulse();
        assertEquals(280.0, peer.x, 1e-100);
        assertEquals(260.0, peer.y, 1e-100);

        popup.setAnchorX(200);
        popup.setAnchorY(100);

        pulse();
        assertEquals(190.0, peer.x, 1e-100);
        assertEquals(100.0, peer.y, 1e-100);
    }

    @Test
    public void testAnchorKeepsPositionOnContentChange() {
        final Popup popup = new Popup();
        final PopupRoot root = new PopupRoot();

        root.setGeomBounds(0, 0, 100, 140);
        root.setLayoutBounds(-10, 20, 120, 100);

        popup.getScene().setRoot(root);

        popup.setAnchorLocation(
                PopupWindow.AnchorLocation.CONTENT_BOTTOM_RIGHT);
        popup.show(stage, 400, 300);

        final StubPopupStage peer = (StubPopupStage) popup.impl_getPeer();

        assertEquals(280.0, peer.x, 1e-100);
        assertEquals(180.0, peer.y, 1e-100);

        root.setLayoutBounds(10, -10, 80, 160);

        pulse();

        assertEquals(400.0, popup.getAnchorX(), 1e-100);
        assertEquals(300.0, popup.getAnchorY(), 1e-100);
        assertEquals(310.0, peer.x, 1e-100);
        assertEquals(140.0, peer.y, 1e-100);
    }

    @Test
    public void testHide() {
        Popup p1 = new Popup();
        p1.show(stage);
        assertTrue(p1.isShowing());
        p1.hide();
        assertFalse(p1.isShowing());
    }
    
    @Test
    public void testHideAll() {
        Popup p1 = new Popup();
        p1.show(stage);
        Popup p2 = new Popup();
        p2.show(p1);
        assertTrue(p1.isShowing());
        assertTrue(p2.isShowing());
        p1.hide();
        assertFalse(p1.isShowing());
        assertFalse(p2.isShowing());
        p1.show(stage);
        assertTrue(p1.isShowing());
        assertFalse(p2.isShowing());
        p1.hide();
        assertFalse(p1.isShowing());
        assertFalse(p2.isShowing());
    }
    
    @Test
    public void testAutoHiding() {
        Popup p1 = new Popup();
        p1.setAutoHide(true);
        p1.show(stage);
        
        Rectangle rect = new Rectangle();
        p1.getContent().add(rect);
        rect.requestFocus();
        assertTrue(p1.isShowing());
      
        // hiding popup stage removes the focus (in stubbed environment)
        p1.hide();
        assertFalse(p1.isShowing());
    }

    @Test
    public void testAutoHidingWithFocusedChild() {
        Popup p1 = new Popup();
        p1.setAutoHide(true);
        p1.show(stage);
        // setting initial focus
        Rectangle rect = new Rectangle();
        p1.getContent().add(rect);
        rect.requestFocus();
        assertTrue(p1.isShowing());

        Popup p2 = new Popup();
        p2.setAutoHide(true);
        p2.show(p1);
        Rectangle rect2 = new Rectangle();
        p2.getContent().add(rect2);
        rect2.requestFocus();
        assertTrue(p1.isShowing());
        assertTrue(p2.isShowing());

        p1.hide();
        // child has focus, popup should stay visible
        // Again this should be handled by PopupEventRedirector
        // all the AutoHide features are not implemented yet.
        //assertTrue(p1.isVisible());
    }
    
    @Test
    public void testAutoHidingTree() {
        Popup p0 = new Popup();
        p0.setAutoHide(true);
        p0.show(stage);
        // setting initial focus
        Rectangle rect = new Rectangle();
        p0.getContent().add(rect);
        rect.requestFocus();
        
        Popup p1 = new Popup();
        p1.setAutoHide(true);
        p1.show(p0);

        Popup p2 = new Popup();
        p2.setAutoHide(true);
        p2.show(p1);
        Rectangle rect2 = new Rectangle();
        p2.getContent().add(rect2);
        rect2.requestFocus();
    
        assertTrue(p1.isShowing());
        assertTrue(p2.isShowing());
        assertTrue(p0.isShowing());

        // after autohide the whole popup tree should be hidden
        // up to the parent popup with focus
        p2.hide();
        // Commenting this assersion for now : need to decide if
        // doAutoHide on popup should hide just itself or its parent popup as well.
        // PopupEventRedirector should probably take care of it.
        //assertFalse(p1.isVisible());
        assertFalse(p2.isShowing());
        assertTrue(p0.isShowing());
    }
    
    @Test
    public void testOnAutohide() {
        Popup p1 = new Popup();
        p1.setAutoHide(true);
        p1.show(stage);
        p1.setOnAutoHide(new EventHandler<Event>() {
            @Override public void handle(Event e) {
                done = true;
            }
        });
        Rectangle rect = new Rectangle(20, 20, 50, 50);
        p1.getContent().add(rect);
        assertFalse(done);

        final MouseEventGenerator generator = new MouseEventGenerator();
        scene.impl_processMouseEvent(
                generator.generateMouseEvent(
                        MouseEvent.MOUSE_PRESSED, 0, 0));

        // test whether runnable ran
        assertTrue(done);
    }
 
    
    @Ignore ("Not sure how this ever worked, or what the point is")
    @Test
    public void testPeerListener() {
        Popup p = new Popup();
        p.setAutoHide(true);
        p.show(stage);
        
        StubPopupStage peer = (StubPopupStage) p.impl_getPeer();
        p.sizeToScene();        

        double width = p.getWidth();
        double height = p.getHeight();
        
        // test changing dimensions to same values
        p.sizeToScene();        
        assertEquals(width, p.getWidth(), 1e-100);
        assertEquals(height, p.getHeight(), 1e-100);
        
        // these methods shouldn't do anything for popups,
        // width and height should stay the same
        peer.close();
        assertEquals(width, p.getWidth(), 1e-100);
        assertEquals(height, p.getHeight(), 1e-100);
        
        peer.setFullScreen(true);
        assertEquals(width, p.getWidth(), 1e-100);
        assertEquals(height, p.getHeight(), 1e-100);
        
        peer.setIconified(true);
        assertEquals(width, p.getWidth(), 1e-100);
        assertEquals(height, p.getHeight(), 1e-100);
        
        peer.setResizable(true);
        assertEquals(width, p.getWidth(), 1e-100);
        assertEquals(height, p.getHeight(), 1e-100);
        
        peer.setLocation(0, 0);
        assertEquals(0, p.getX(), 1e-100);
        assertEquals(0, p.getY(), 1e-100);
        peer.setSize(100, 100);
        assertEquals(100, p.getWidth(), 1e-100);
        assertEquals(100, p.getHeight(), 1e-100);
    }

    @Test
    public void testDefautValueOfAutofix() {
        Popup p = new Popup();
        assertTrue(p.isAutoFix());
        assertTrue(p.autoFixProperty().get());
    }

    @Test
    public void testBasicAutofix() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        0, 200, 1920, 1000,
                                        96));

        final Popup popup = new Popup();
        popup.getContent().add(new Rectangle(0, 0, 50, 50));
        popup.show(stage, 1900, 100);
        assertEquals(1920, popup.getX() + popup.getWidth(), 1e-100);
        assertEquals(200, popup.getY(), 1e-100);
    }

    @Test
    public void testDoubleShowAutofix() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        0, 200, 1920, 1000,
                                        96));

        final Popup popup = new Popup();
        popup.getContent().add(new Rectangle(0, 0, 50, 50));

        popup.show(stage, 1900, 100);
        assertEquals(1920, popup.getX() + popup.getWidth(), 1e-100);
        assertEquals(200, popup.getY(), 1e-100);

        popup.show(stage, 1900, 100);
        assertEquals(1920, popup.getX() + popup.getWidth(), 1e-100);
        assertEquals(200, popup.getY(), 1e-100);
    }

    @Test
    public void testAutofixActivationAfterShow() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        0, 200, 1920, 1000,
                                        96));

        final Popup popup = new Popup();
        popup.setAutoFix(false);
        popup.getContent().add(new Rectangle(0, 0, 50, 50));
        popup.show(stage, 1900, 100);

        assertEquals(1900, popup.getX(), 1e-100);
        assertEquals(100, popup.getY(), 1e-100);

        popup.setAutoFix(true);
        assertEquals(1920, popup.getX() + popup.getWidth(), 1e-100);
        assertEquals(200, popup.getY(), 1e-100);
    }

    @Test
    public void testAutofixOnContentChange() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        0, 0, 1920, 1172,
                                        96));

        final Popup popup = new Popup();
        popup.getContent().add(new Rectangle(0, 0, 50, 50));
        popup.show(stage, 100, 1120);
        assertEquals(100, popup.getX(), 1e-100);
        assertEquals(1120, popup.getY(), 1e-100);

        popup.getContent().add(new Rectangle(0, 0, 50, 100));
        assertEquals(100, popup.getX(), 1e-100);
        assertEquals(1172, popup.getY() + popup.getHeight(), 1e-100);
    }

    @Test
    public void testAutofixOnScreenChange() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        0, 0, 1920, 1172,
                                        96));

        final Popup popup = new Popup();
        popup.getContent().add(new Rectangle(0, 0, 50, 50));
        popup.show(stage, 100, 1120);
        assertEquals(100, popup.getX(), 1e-100);
        assertEquals(1120, popup.getY(), 1e-100);

        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        120, 0, 1800, 1172,
                                        96));

        assertEquals(120, popup.getX(), 1e-100);
        assertEquals(1120, popup.getY(), 1e-100);
    }

    @Test
    public void testAutofixWithFullScreen() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200,
                                        0, 0, 1920, 1172,
                                        96));

        final Popup popup = new Popup();
        popup.getContent().add(new Rectangle(0, 0, 50, 50));

        stage.setFullScreen(true);
        popup.show(stage, 100, 1160);

        assertEquals(100, popup.getX(), 1e-100);
        assertEquals(1150, popup.getY(), 1e-100);
    }

    @Test
    public void testSetPopupContentByChangingRootNode() {
        final Popup popup = new Popup();

        popup.getScene().setRoot(new Group(new Rectangle(0, 0, 300, 200)));
        assertEquals(300, popup.getWidth(), 1e-100);
        assertEquals(200, popup.getHeight(), 1e-100);

        popup.getScene().setRoot(new Group(new Rectangle(0, 0, 200, 300)));
        assertEquals(200, popup.getWidth(), 1e-100);
        assertEquals(300, popup.getHeight(), 1e-100);
    }

    @Test
    public void testConsumeAutoHidingEventsProperty() {
        final EventCounter mouseEventCounter = new EventCounter();
        final EventCounter keyEventCounter = new EventCounter();

        stage.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEventCounter);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, keyEventCounter);
        try {
            final MouseEventGenerator mouseEventGenerator =
                    new MouseEventGenerator();

            final Popup popup = new Popup();
            popup.setAutoHide(true);

            assertTrue(popup.getConsumeAutoHidingEvents());

            popup.show(stage);
            Event.fireEvent(stage,
                            mouseEventGenerator.generateMouseEvent(
                                MouseEvent.MOUSE_PRESSED, 0, 0));
            assertEquals(0, mouseEventCounter.getValue());

            popup.show(stage);
            Event.fireEvent(stage,
                            new KeyEvent(null, stage,
                                KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED,
                                KeyCode.ESCAPE.getName(),
                                KeyCode.ESCAPE,
                                false, false, false, false));
            assertEquals(0, keyEventCounter.getValue());

            popup.setConsumeAutoHidingEvents(false);

            popup.show(stage);
            Event.fireEvent(stage,
                            mouseEventGenerator.generateMouseEvent(
                                MouseEvent.MOUSE_PRESSED, 0, 0));
            assertEquals(1, mouseEventCounter.getValue());

            popup.show(stage);
            Event.fireEvent(stage,
                            new KeyEvent(null, stage,
                                KeyEvent.KEY_PRESSED,
                                KeyEvent.CHAR_UNDEFINED,
                                KeyCode.ESCAPE.getName(),
                                KeyCode.ESCAPE,
                                false, false, false, false));
            assertEquals(1, keyEventCounter.getValue());
            
        } finally {
            stage.removeEventHandler(MouseEvent.MOUSE_PRESSED,
                                     mouseEventCounter);
            stage.removeEventHandler(KeyEvent.KEY_PRESSED, keyEventCounter);
        }
    }

    @Test(expected=NullPointerException.class)
    public void testShowWithNullOwner() {
        final Popup popup = new Popup();
        popup.show(null);
    }

    @Test(expected=NullPointerException.class)
    public void testShowXYWithNullOwner() {
        final Popup popup = new Popup();
        popup.show((Window) null, 10, 10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testShowWithOwnerThatWouldCreateCycle1() {
        final Popup popup = new Popup();
        popup.show(popup);                
    }

    @Test(expected=IllegalArgumentException.class)
    public void testShowWithOwnerThatWouldCreateCycle2() {
        final Popup popup1 = new Popup();
        final Popup popup2 = new Popup();

        popup1.show(stage);
        popup2.show(popup1);
        popup1.hide();
        popup1.show(popup2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testShowXYWithOwnerThatWouldCreateCycle1() {
        final Popup popup = new Popup();
        popup.show(popup, 10, 20);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testShowXYWithOwnerThatWouldCreateCycle2() {
        final Popup popup1 = new Popup();
        final Popup popup2 = new Popup();

        popup1.show(stage);
        popup2.show(popup1);
        popup1.hide();
        popup1.show(popup2, 10, 20);
    }

    @Test
    public void testFocusGrabbedWhenNecessary() {
        final Popup popup = new Popup();

        popup.show(stage);
        stage.requestFocus();

        final StubStage peer = (StubStage) stage.impl_getPeer();
        assertFalse(peer.isFocusGrabbed());

        popup.setAutoHide(true);
        assertTrue(peer.isFocusGrabbed());

        popup.hide();
        assertFalse(peer.isFocusGrabbed());
    }

    @Test
    public void testPopupRootStyle() {
        final Popup popup = new Popup();

        final Parent oldRoot = popup.getScene().getRoot();
        Assert.assertTrue(oldRoot.getStyleClass().contains("popup"));

        final Group newRoot = new Group(new Rectangle(0, 0, 200, 300));
        popup.getScene().setRoot(newRoot);

        Assert.assertTrue(newRoot.getStyleClass().contains("popup"));
        Assert.assertFalse(oldRoot.getStyleClass().contains("popup"));

        System.out.println(javafx.scene.shape.Sphere.class.getResource("Sphere.class"));
    }
    
    @Test
    public void testCursorInheritance() {
        stage.getScene().setCursor(Cursor.CLOSED_HAND);
        
        final Popup popup = new Popup();

        popup.show(stage);
        assertEquals(Cursor.CLOSED_HAND, popup.getScene().getCursor());

    }

    private static final class EventCounter implements EventHandler<Event> {
        private int counter;

        public int getValue() {
            return counter;
        }

        public void handle(final Event event) {
            ++counter;
        }
    }
}
