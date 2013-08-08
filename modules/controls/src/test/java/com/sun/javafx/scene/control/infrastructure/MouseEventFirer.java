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

package com.sun.javafx.scene.control.infrastructure;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.List;


public final class MouseEventFirer {
    private final EventTarget target;

    public MouseEventFirer(EventTarget target) {
        this.target = target;
    }

    public void fireMousePressAndRelease(KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, modifiers);
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, modifiers);
    }
    
    public void fireMouseClicked() {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED);
    }
    
    public void fireMouseClicked(MouseButton button) {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED, button, 0, 0);
    }
    
    public void fireMouseClicked(double deltaX, double deltaY) {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED, deltaX, deltaY);
    }

    public void fireMouseClicked(double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED, deltaX, deltaY, modifiers);
    }
    
    public void fireMousePressed() {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED);
    }
    
    public void fireMousePressed(MouseButton button) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, button, 0, 0);
    }
    
    public void fireMousePressed(double deltaX, double deltaY) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, deltaX, deltaY);
    }

    public void fireMousePressed(double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, deltaX, deltaY, modifiers);
    }
    
    public void fireMouseReleased() {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED);
    }
    
    public void fireMouseReleased(MouseButton button) {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, button, 0, 0);
    }
    
    public void fireMouseReleased(double deltaX, double deltaY) {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, deltaX, deltaY);
    }

    public void fireMouseReleased(double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, deltaX, deltaY, modifiers);
    }
    
    public void fireMouseEvent(EventType<MouseEvent> evtType, KeyModifier... modifiers) {
        fireMouseEvent(evtType, 0, 0 , modifiers);
    }
    
    public void fireMouseEvent(EventType<MouseEvent> evtType, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(evtType, MouseButton.PRIMARY, deltaX, deltaY, modifiers);
    }
    
    public void fireMouseEvent(EventType<MouseEvent> evtType, MouseButton button, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(evtType, button, 1, deltaX, deltaY, modifiers);
    }
    
    private void fireMouseEvent(EventType<MouseEvent> evtType, MouseButton button, int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
        Scene scene = null;
        Bounds targetBounds = null;
        
        // Force the target node onto a stage so that it is accessible
        if (target instanceof Node) {
            Node n = (Node)target;
            new StageLoader(n);
            scene = n.getScene();
            targetBounds = n.getLayoutBounds();
        } else if (target instanceof Scene) {
            scene = (Scene)target;
            new StageLoader(scene);
            targetBounds = new BoundingBox(0, 0, scene.getWidth(), scene.getHeight());
        }

        // calculate bounds
        final Stage stage = (Stage) scene.getWindow();
        
        // width / height of target node
        final double w = targetBounds.getWidth();
        final double h = targetBounds.getHeight();
        
        // x / y click position is centered
        final double x = w / 2.0 + deltaX;
        final double y = h / 2.0 + deltaY;
        
        final double sceneX = x + scene.getX() + deltaX;
        final double sceneY = y + scene.getY() + deltaY;
        
        final double screenX = sceneX + stage.getX();
        final double screenY = sceneY + stage.getY();
        
        final List<KeyModifier> ml = Arrays.asList(modifiers);
        
        final PickResult pickResult = new PickResult(target, sceneX, sceneY);
        
        MouseEvent evt = new MouseEvent(
                target, 
                target, 
                evtType, 
                x, y, 
                screenX, screenY, 
                button, 
                clickCount,
                ml.contains(KeyModifier.SHIFT),    // shiftDown
                ml.contains(KeyModifier.CTRL),     // ctrlDown
                ml.contains(KeyModifier.ALT),      // altDown
                ml.contains(KeyModifier.META),     // metaData
                button == MouseButton.PRIMARY,     // primary button
                button == MouseButton.MIDDLE,      // middle button
                button == MouseButton.SECONDARY,   // secondary button
                false,                             // synthesized 
                button == MouseButton.SECONDARY,   // is popup trigger
                true,                              // still since pick
                pickResult);                       // pick result
        
//        // lets see the click position.
//        // Unfortunately this doesn't work at present because StubToolkit 
//        // cannot generate snapshots
//        WritableImage image = target.snapshot(null, null);
//        Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
//        GraphicsContext g = canvas.getGraphicsContext2D();
//        g.drawImage(image, 0, 0);
//        
//        g.setFill(Color.RED);
//        g.setStroke(Color.WHITE);
//        g.fillOval(deltaX - 2, deltaY - 2, 4, 4);
//        g.strokeOval(deltaX - 2, deltaY - 2, 4, 4);
//        
//        Stage stage = new Stage();
//        stage.setScene(new Scene(new Pane(canvas), image.getWidth(), image.getHeight()));
//        stage.show();
        
        Event.fireEvent(target, evt);
    }
    
//    public void fireMouseEvent(Scene target, EventType<MouseEvent> evtType, MouseButton button, int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
//        List<KeyModifier> ml = Arrays.asList(modifiers);
//        
//        double screenX = target.getWindow().getX() + target.getX() + deltaX;
//        double screenY = target.getWindow().getY() + target.getY() + deltaY;
//        
//        MouseEvent evt = new MouseEvent(
//                target, 
//                target, 
//                evtType, 
//                deltaX, deltaY, 
//                screenX, screenY, 
//                button, 
//                clickCount,
//                ml.contains(KeyModifier.SHIFT),    // shiftDown
//                ml.contains(KeyModifier.CTRL),     // ctrlDown
//                ml.contains(KeyModifier.ALT),      // altDown
//                ml.contains(KeyModifier.META),     // metaData
//                button == MouseButton.PRIMARY,     // primary button
//                button == MouseButton.MIDDLE,      // middle button
//                button == MouseButton.SECONDARY,   // secondary button
//                true,                              // synthesized 
//                button == MouseButton.SECONDARY,   // is popup trigger
//                false,                             // still since pick
//                null);                             // pick result
//        
//        Event.fireEvent(target, evt);
//    }
}
