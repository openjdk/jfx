/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.cubeGame.utils;


import javafx.beans.property.Property;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;


/**
 * Utility class that binds simple mouse gestures to number properties so that
 * their values can be controlled with mouse drag events.
 */
public class DragSupport {
    public EventHandler<KeyEvent> keyboardEventHandler;
    public EventHandler<MouseEvent> mouseEventHandler;
    private Number anchor;
    private double dragAnchor;
    private MouseEvent lastMouseEvent;
    private Node target;

    /**
     * Creates DragSupport instance that attaches EventHandlers to the given scene 
     * and responds to mouse and keyboard events in order to change given 
     * property values according to mouse drag events of given orientation
     * @param target scene
     * @param modifier null if no modifier needed
     * @param orientation vertical or horizontal
     * @param property number property to control
     * @see #DragSupport(javafx.scene.Node, javafx.scene.input.KeyCode, javafx.geometry.Orientation, javafx.beans.property.Property, double)
     */
    public DragSupport(Node target, final KeyCode modifier, final Orientation orientation, final Property<Number> property) {
        this(target, modifier, null, orientation, property, 1);
    }
    
    public DragSupport(Node target, final KeyCode modifier, MouseButton mouseButton, final Orientation orientation, final Property<Number> property) {
        this(target, modifier, mouseButton, orientation, property, 1);
    }

    /**
     * Removes event handlers of this DragSupport instance from the target scene
     */
    public void detach() {
        target.removeEventHandler(MouseEvent.ANY, mouseEventHandler);
        target.removeEventHandler(KeyEvent.ANY, keyboardEventHandler);
    }

    /**
     * Creates DragSupport instance that attaches EventHandlers to the given scene 
     * and responds to mouse and keyboard events in order to change given 
     * property values according to mouse drag events of given orientation.
     * Mouse movement amount is multiplied by given factor.
     * @param target scene
     * @param modifier null if no modifier needed
     * @param orientation vertical or horizontal
     * @param property number property to control
     * @param factor multiplier for mouse movement amount
     */
    public DragSupport(Node target, final KeyCode modifier, final Orientation orientation, final Property<Number> property, final double factor) {
        this(target, modifier, MouseButton.PRIMARY, orientation, property, factor);
    }

    public DragSupport(Node target, final KeyCode modifier, final MouseButton mouseButton, final Orientation orientation, final Property<Number> property, final double factor) {
        this.target = target;
        mouseEventHandler = new EventHandler<MouseEvent>() {
            
            @Override
            public void handle(MouseEvent t) {
                if (t.getEventType() != MouseEvent.MOUSE_ENTERED_TARGET
                        && t.getEventType() != MouseEvent.MOUSE_EXITED_TARGET) {
                    lastMouseEvent = t;
                }
                if (t.getEventType() == MouseEvent.MOUSE_PRESSED) {
                    if ((mouseButton == null || t.getButton() == mouseButton)
                            && isModifierCorrect(t, modifier)) {
                        anchor = property.getValue();
                        dragAnchor = getCoord(t, orientation);
                        t.consume();
                    }
                } else if (t.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                    if ((mouseButton == null || t.getButton() == mouseButton)
                            && isModifierCorrect(t, modifier)) {
                        property.setValue(anchor.doubleValue()
                                + (getCoord(t, orientation) - dragAnchor) * factor);
                        t.consume();
                    }
                }
            }
        };
        keyboardEventHandler = new EventHandler<KeyEvent>() {
            
            @Override
            public void handle(KeyEvent t) {
                if (t.getEventType() == KeyEvent.KEY_PRESSED) {
                    if (t.getCode() == modifier) {
                        anchor = property.getValue();
                        if (lastMouseEvent != null) {
                            dragAnchor = getCoord(lastMouseEvent, orientation);
                        }
                        t.consume();
                    }
                } else if (t.getEventType() == KeyEvent.KEY_RELEASED) {
                    if (t.getCode() != modifier && isModifierCorrect(t, modifier)) {
                        anchor = property.getValue();
                        if (lastMouseEvent != null) {
                            dragAnchor = getCoord(lastMouseEvent, orientation);
                        }
                        t.consume();
                    }
                }
            }
        };
        target.addEventHandler(MouseEvent.ANY, mouseEventHandler);
        target.addEventHandler(KeyEvent.ANY, keyboardEventHandler);
    }

    private boolean isModifierCorrect(KeyEvent t, KeyCode keyCode) {
        return (keyCode != KeyCode.ALT ^ t.isAltDown()) 
                && (keyCode != KeyCode.CONTROL ^ t.isControlDown()) 
                && (keyCode != KeyCode.SHIFT ^ t.isShiftDown()) 
                && (keyCode != KeyCode.META ^ t.isMetaDown());
    }

    private boolean isModifierCorrect(MouseEvent t, KeyCode keyCode) {
        return (keyCode != KeyCode.ALT ^ t.isAltDown()) 
                && (keyCode != KeyCode.CONTROL ^ t.isControlDown()) 
                && (keyCode != KeyCode.SHIFT ^ t.isShiftDown()) 
                && (keyCode != KeyCode.META ^ t.isMetaDown());
    }

    private double getCoord(MouseEvent t, Orientation orientation) {
        switch (orientation) {
            case HORIZONTAL:
                return t.getX();
            case VERTICAL:
                return t.getY();
            default:
                throw new IllegalArgumentException("This orientation is not supported: " + orientation);
        }
    }
    
}
