/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.input.InputEventUtils;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;

// PENDING_DOC_REVIEW
/**
 * When the user requests a context menu, this event occurs.  Context
 * menus can be triggered by the mouse or the keyboard.  The exact
 * sequence of mouse or keyboard events that is used to request a
 * menu is platform specific.  For example, on Windows, Shift+F10
 * requests a context menu.
 * <p>
 */
public class ContextMenuEvent extends InputEvent {

    /**
     * This event occurs when a context menu is requested.
     */
    public static final EventType<ContextMenuEvent> CONTEXT_MENU_REQUESTED =
            new EventType<ContextMenuEvent>(ContextMenuEvent.ANY, "CONTEXT_MENU_REQUESTED");

    private ContextMenuEvent(final EventType<? extends ContextMenuEvent> eventType) {
        super(eventType);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated
     */
    @Deprecated
    public static ContextMenuEvent impl_contextEvent(double _x, double _y,
          double _screenX, double _screenY, boolean _keyboardTrigger,
          EventType<? extends ContextMenuEvent> _eventType
          )
    {
        ContextMenuEvent e = new ContextMenuEvent(_eventType);
        e.x = _x;
        e.y = _y;
        e.screenX = _screenX;
        e.screenY = _screenY;
        e.sceneX = _x;
        e.sceneY = _y;
        e.keyboardTrigger = _keyboardTrigger;
        return e;
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(ContextMenuEvent newEvent, Object newSource) {

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(x, y), source, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
        newEvent.sceneX = getSceneX();
        newEvent.sceneY = getSceneY();
    }

    @Override
    public Event copyFor(Object newSource, EventTarget newTarget) {
        ContextMenuEvent e = (ContextMenuEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }
    
    /**
     * The boolean that indicates the event was triggered by a keyboard gesture.
     */
    private boolean keyboardTrigger;
    
    /**
     * Determines whether this event originated from the keyboard.
     *
     * @return true if the event was caused by the keyboard
     */
    public boolean isKeyboardTrigger() {
        return keyboardTrigger;
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private double x;

    public final double getX() {
        return x;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private double y;

    public final double getY() {
        return y;
    }

    /**
     * Absolute horizontal x position of the event.
     */
    private double screenX;

    public final double getScreenX() {
        return screenX;
    }

    /**
     * Absolute vertical y position of the event.
     */
    private double screenY;

    public final double getScreenY() {
        return screenY;
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
     */
    private double sceneX;

    public final double getSceneX() {
        return sceneX;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
     */
    private double sceneY;

    public final double getSceneY() {
        return sceneY;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("ContextMenuEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY());

        return sb.append("]").toString();
    }
}
