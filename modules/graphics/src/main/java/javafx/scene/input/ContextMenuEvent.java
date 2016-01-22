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

package javafx.scene.input;

import com.sun.javafx.scene.input.InputEventUtils;
import java.io.IOException;
import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;
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
 * @since JavaFX 2.1
 */
public class ContextMenuEvent extends InputEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * This event occurs when a context menu is requested.
     */
    public static final EventType<ContextMenuEvent> CONTEXT_MENU_REQUESTED =
            new EventType<ContextMenuEvent>(InputEvent.ANY, "CONTEXTMENUREQUESTED");

    /**
     * Common supertype for all context menu event types.
     * @since JavaFX 8.0
     */
    public static final EventType<ContextMenuEvent> ANY = CONTEXT_MENU_REQUESTED;

    /**
     * Constructs new ContextMenu event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the scene
     * @param y The y with respect to the scene
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param keyboardTrigger true if this event was triggered by keyboard.
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and the target
     * @since JavaFX 8.0
     */
    public ContextMenuEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<ContextMenuEvent> eventType, @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY, @NamedArg("keyboardTrigger") boolean keyboardTrigger,
            @NamedArg("pickResult") PickResult pickResult) {
        super(source, target, eventType);
        this.screenX = screenX;
        this.screenY = screenY;
        this.sceneX = x;
        this.sceneY = y;
        this.x = x;
        this.y = y;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        final Point3D p = InputEventUtils.recomputeCoordinates(this.pickResult, null);
        this.x = p.getX();
        this.y = p.getY();
        this.z = p.getZ();
        this.keyboardTrigger = keyboardTrigger;
     }

    /**
     * Constructs new ContextMenu event with empty source and target.
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param keyboardTrigger true if this event was triggered by keyboard.
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public ContextMenuEvent(@NamedArg("eventType") EventType<ContextMenuEvent> eventType, @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY, @NamedArg("keyboardTrigger") boolean keyboardTrigger,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, keyboardTrigger,
                pickResult);
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(ContextMenuEvent newEvent, Object newSource) {

        final Point3D newCoordinates = InputEventUtils.recomputeCoordinates(
                pickResult, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
        newEvent.z = newCoordinates.getZ();
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
     * Depth z position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private transient double z;

    /**
     * Depth position of the event relative to the
     * origin of the MouseEvent's source.
     *
     * @return depth position of the event relative to the
     * origin of the MouseEvent's source.
     * @since JavaFX 8.0
     */
    public final double getZ() {
        return z;
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
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
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
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return vertical position of the event relative to the
     * origin of the {@code Scene} that contains the ContextMenuEvent's source
     */
    public final double getSceneY() {
        return sceneY;
    }

    /**
     * Information about the pick if the picked {@code Node} is a
     * {@code Shape3D} node and its pickOnBounds is false.
     */
    private PickResult pickResult;

    /**
     * Returns information about the pick.
     *
     * @return new PickResult object that contains information about the pick
     * @since JavaFX 8.0
     */
    public final PickResult getPickResult() {
        return pickResult;
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

        sb.append(", x = ").append(getX()).append(", y = ").append(getY())
                .append(", z = ").append(getZ());
        sb.append(", pickResult = ").append(getPickResult());

        return sb.append("]").toString();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        x = sceneX;
        y = sceneY;
    }
}
