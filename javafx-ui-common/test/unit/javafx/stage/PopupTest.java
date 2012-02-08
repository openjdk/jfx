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

package javafx.stage;

import com.sun.javafx.test.MouseEventGenerator;
import javafx.scene.input.MouseEvent;
import static org.junit.Assert.assertEquals;
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
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

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
    
    // Test using various objects as parents
//    @Test
//    public void testParent() {
//       Popup p1 = new Popup();
//       // using stage as parent
//       p1.setParent(stage);
//       assertEquals(stage, p1.getParent());
//       assertEquals(stage, p1.getParentWindow());
//       // using scene as parent
//       p1.setParent(scene);
//       assertEquals(scene, p1.getParent());
//       assertEquals(stage, p1.getParentWindow());
//       // using other popup as parent
//       Popup p2 = new Popup();
//       p1.show();
//       p2.setParent(p1);
//       assertEquals(p1, p2.getParent());
//       assertEquals(p1.getPopupWindow(), p2.getParentWindow());
//       // using Node (not in scene) as parent    
//       Rectangle rect = new Rectangle();
//       p1.setParent(rect);
//       assertEquals(rect, p1.getParent());
//       assertEquals(null, p1.getParentWindow()); 
//       // using different Object (not Node) as parent    
//       Integer i = new Integer(1);
//       p1.setParent(i);
//       assertEquals(i, p1.getParent());
//       assertEquals(null, p1.getParentWindow());
//       // using Node (in scene) as parent
//       ((Group)scene.getRoot()).getChildren().add(rect);
//       p1.setParent(rect);
//       assertEquals(rect, p1.getParent());
//       assertEquals(stage, p1.getParentWindow());
//    }
    
//    @Test
//    public void testChangeParent() {
//       Popup p1 = new Popup();
//       p1.setParent(stage);
//       Popup p2 = new Popup();
//       p1.show();
//       p2.setParent(p1);
//       assertEquals(p1, p2.getParent());
//       assertEquals(p1.getPopupWindow(), p2.getParentWindow());
//       assertTrue(p1.children.contains(p2));
//       p2.setParent(stage);
//       assertEquals(stage, p2.getParent());
//       assertEquals(stage, p2.getParentWindow());       
//       assertFalse(p1.children.contains(p2));
//    }
    
//    @Test
//    public void testGetPopup() {
//        Popup p1 = new Popup();
//        p1.setParent(stage);
//        p1.show();
//        assertEquals(p1, Popup.impl_getPopup(p1.getPopupWindow()));
//        // should be null for non-popup stage
//        assertEquals(null, Popup.impl_getPopup(stage));
//
//        // adding different stage extension
//        stage.getExtensions().add(new WindowExtension() {
//            @Override
//            protected void impl_windowUpdated(Window window) {
//            }
//        });
//        // should be null for non-popup stage
//        assertEquals(null, Popup.impl_getPopup(stage));
//    }
        
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
    
//    @Test
//    public void testAutoHidingWithoutParent() {                
//        Popup p1 = new Popup();
//        p1.show(stage);
//        assertTrue(p1.isVisible());
//        Popup p2 = new Popup();
//        p2.show(p1);
//        assertTrue(p2.isVisible());
//        p1.setParent(null);
//        // test autohiding without root parent
//        p2.doAutoHide();
//        assertFalse(p2.isVisible());
//        // Commenting this assersion for now : need to decide if
//        // doAutoHide on popup should hide just itself or its parent popup as well.
//        // PopupEventRedirector should probably take care of it.
//        //assertFalse(p1.isVisible());
//    }

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
}
