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

import java.util.Arrays;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public final class MouseEventFirer {
    
    private MouseEventFirer() {
        // no-op
    }
    
    public static void fireMouseClickAndRelease(Node target, KeyModifier... modifiers) {
        fireMouseEvent(target, MouseEvent.MOUSE_PRESSED, modifiers);
        fireMouseEvent(target, MouseEvent.MOUSE_RELEASED, modifiers);
    }
    
    public static void fireMouseClicked(Node target) {
        fireMouseEvent(target, MouseEvent.MOUSE_CLICKED);
    }
    
    public static void fireMouseClicked(Node target, MouseButton button) {
        fireMouseEvent(target, MouseEvent.MOUSE_CLICKED, button, 0, 0);
    }
    
    public static void fireMouseClicked(Node target, double deltaX, double deltaY) {
        fireMouseEvent(target, MouseEvent.MOUSE_CLICKED, deltaX, deltaY);
    }
    
    public static void fireMousePressed(Node target) {
        fireMouseEvent(target, MouseEvent.MOUSE_PRESSED);
    }
    
    public static void fireMousePressed(Node target, MouseButton button) {
        fireMouseEvent(target, MouseEvent.MOUSE_PRESSED, button, 0, 0);
    }
    
    public static void fireMousePressed(Node target, double deltaX, double deltaY) {
        fireMouseEvent(target, MouseEvent.MOUSE_PRESSED, deltaX, deltaY);
    }
    
    public static void fireMouseReleased(Node target) {
        fireMouseEvent(target, MouseEvent.MOUSE_RELEASED);
    }
    
    public static void fireMouseReleased(Node target, MouseButton button) {
        fireMouseEvent(target, MouseEvent.MOUSE_RELEASED, button, 0, 0);
    }
    
    public static void fireMouseReleased(Node target, double deltaX, double deltaY) {
        fireMouseEvent(target, MouseEvent.MOUSE_RELEASED, deltaX, deltaY);
    }
    
    public static void fireMouseEvent(Node target, EventType<MouseEvent> evtType, KeyModifier... modifiers) {
        fireMouseEvent(target, evtType, 0, 0 , modifiers);
    }
    
    public static void fireMouseEvent(Node target, EventType<MouseEvent> evtType, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(target, evtType, MouseButton.PRIMARY, deltaX, deltaY, modifiers);
    }
    
    public static void fireMouseEvent(Node target, EventType<MouseEvent> evtType, MouseButton button, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(target, evtType, button, 1, deltaX, deltaY, modifiers);
    }
    
    public static void fireMouseEvent(Node target, EventType<MouseEvent> evtType, MouseButton button, int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
        List<KeyModifier> ml = Arrays.asList(modifiers);
        
        Bounds screenBounds = target.localToScreen(target.getLayoutBounds());
        double screenX = screenBounds.getMaxX() - screenBounds.getWidth() / 2.0 + deltaX;
        double screenY = screenBounds.getMaxY() - screenBounds.getHeight() / 2.0 + deltaY;
        
        MouseEvent evt = new MouseEvent(
                target, 
                target, 
                evtType, 
                deltaX, deltaY, 
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
                true,                              // synthesized 
                button == MouseButton.SECONDARY,   // is popup trigger
                true,                              // still since pick
                null);                             // pick result
        
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
    
//    public static void fireMouseEvent(Scene target, EventType<MouseEvent> evtType, MouseButton button, int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
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
