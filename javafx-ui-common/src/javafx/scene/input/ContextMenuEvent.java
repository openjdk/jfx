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
import java.io.IOException;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;

// PENDING_DOC_REVIEW
/**
 * When the user requests a context menu, this event occurs.  Context
 * menus can be triggered by the mouse or the keyboard.  The exact
 * sequence of mouse or keyboard events that is used to request a
 * menu is platform specific.  For example, on Windows, Shift+F10
 * requests a context menu.
 * <p>
 * The event coordinates contain default position for the context menu.
 * For mouse-triggered events it is the position of the
 * mouse cursor, for keyboard-triggered events it is a point
 * inside of bounds of current focus owner (which is the event's target).
 */
public class ContextMenuEvent extends InputEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * This event occurs when a context menu is requested.
     */
    public static final EventType<ContextMenuEvent> CONTEXT_MENU_REQUESTED =
            new EventType<ContextMenuEvent>(ContextMenuEvent.ANY, "CONTEXTMENUREQUESTED");

    /**
     * Constructs new ContextMenu event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
     * @param y The y with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param keyboardTrigger true if this event was triggered by keyboard.
     */
    public ContextMenuEvent(Object source, EventTarget target, EventType<ContextMenuEvent> eventType, double x, double y,
            double screenX, double screenY, boolean keyboardTrigger) {
        super(source, target, eventType);
        this.x = x;
        this.y = y;
        if (source != null && source instanceof Node) {
            Node sourceNode = (Node) source;
            Point2D localToScene = sourceNode.localToScene(x, y);
            this.sceneX = localToScene.getX();
            this.sceneY = localToScene.getY();
        } else {
            this.sceneX = x;
            this.sceneY = y;
        }
        this.screenX = screenX;
        this.screenY = screenY;
        this.keyboardTrigger = keyboardTrigger;
     }

    /**
     * Constructs new ContextMenu event with empty source and target.
     * @param eventType The type of the event.
     * @param x The x with respect to the screen.
     * @param y The y with respect to the screen.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param keyboardTrigger true if this event was triggered by keyboard.
     */
    public ContextMenuEvent(EventType<ContextMenuEvent> eventType, double x, double y,
            double screenX, double screenY, boolean keyboardTrigger) {
        this(null, null, eventType, x, y, screenX, screenY, keyboardTrigger);
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(ContextMenuEvent newEvent, Object newSource) {

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(sceneX, sceneY), null, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
    }

    @Override
    public ContextMenuEvent copyFor(Object newSource, EventTarget newTarget) {
        ContextMenuEvent e = (ContextMenuEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    @Override
    public EventType<ContextMenuEvent> getEventType() {
        return (EventType<ContextMenuEvent>) super.getEventType();
    }
    
    /**
     * The boolean that indicates the event was triggered by a keyboard gesture.
     */
    private final boolean keyboardTrigger;
    
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
     * origin of the ContextMenuEvent's node.
     */
    private transient double x;

    /**
     * Horizontal position of the event relative to the
     * origin of the ContextMenuEvent's source.
     * For more information about this event's coordinate semantics please see
     * the general description of {@link ContextMenuEvent}.
     *
     * @return horizontal position of the event relative to the
     * origin of the ContextMenuEvent's source.
     */
    public final double getX() {
        return x;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the ContextMenuEvent's node.
     */
    private transient double y;

    /**
     * Vertical position of the event relative to the
     * origin of the ContextMenuEvent's source.
     * For more information about this event's coordinate semantics please see
     * the general description of {@link ContextMenuEvent}.
     *
     * @return vertical position of the event relative to the
     * origin of the ContextMenuEvent's source.
     */
    public final double getY() {
        return y;
    }

    /**
     * Absolute horizontal x position of the event.
     */
    private final double screenX;

    /**
     * Returns absolute horizontal position of the event.
     * For more information about this event's coordinate semantics please see
     * the general description of {@link ContextMenuEvent}.
     * @return absolute horizontal position of the event
     */
    public final double getScreenX() {
        return screenX;
    }

    /**
     * Absolute vertical y position of the event.
     */
    private final double screenY;

    /**
     * Returns absolute vertical position of the event.
     * For more information about this event's coordinate semantics please see
     * the general description of {@link ContextMenuEvent}.
     * @return absolute vertical position of the event
     */
    public final double getScreenY() {
        return screenY;
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the ContextMenuEvent's node.
     */
    private final double sceneX;

    /**
     * Returns horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the ContextMenuEvent's node.
     * For more information about this event's coordinate semantics please see
     * the general description of {@link ContextMenuEvent}.
     *
     * @return horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's source
     */
    public final double getSceneX() {
        return sceneX;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the ContextMenuEvent's node.
     */
    private final double sceneY;

    /**
     * Returns vertical position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the ContextMenuEvent's node.
     * For more information about this event's coordinate semantics please see
     * the general description of {@link ContextMenuEvent}.
     *
     * @return vertical position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's source
     */
    public final double getSceneY() {
        return sceneY;
    }

    /**
     * Returns a string representation of this {@code ContextMenuEvent} object.
     * @return a string representation of this {@code ContextMenuEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("ContextMenuEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY());

        return sb.append("]").toString();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        x = sceneX;
        y = sceneY;
    }
}
