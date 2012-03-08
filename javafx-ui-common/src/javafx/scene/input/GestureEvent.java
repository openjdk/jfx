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

package javafx.scene.input;

import com.sun.javafx.scene.input.InputEventUtils;
import com.sun.javafx.tk.Toolkit;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;

/**
 * An event indicating gesture input. Gestures are typically caused by
 * direct (touch screen) or indirect (track pad) touch events.
 */
public class GestureEvent extends InputEvent {

    /**
     * Common supertype for all gestures.
     */
    public static final EventType<GestureEvent> ANY =
            new EventType<GestureEvent>(InputEvent.ANY, "GESTURE");

    /**
     * Creates a new instance of {@code GestureEvent}.
     * @param eventType Type of the event
     */
    protected GestureEvent(final EventType<? extends GestureEvent> eventType) {
        super(eventType);
    }

    /**
     * Creates a new instance of {@code GestureEvent}.
     * @param source Event source
     * @param target Event target
     * @param eventType Type of the event
     */
    protected GestureEvent(Object source, EventTarget target,
            final EventType<? extends GestureEvent> eventType) {
        super(source, target, eventType);
    }

    GestureEvent(final EventType<? extends GestureEvent> eventType,
            double x, double y, double screenX, double screenY,
            boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown, boolean direct, boolean inertia) {
        super(eventType);
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.sceneX = x;
        this.sceneY = y;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        this.direct = direct;
        this.inertia = inertia;
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object.
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(GestureEvent newEvent, Object newSource) {

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(x, y), source, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
        newEvent.sceneX = getSceneX();
        newEvent.sceneY = getSceneY();
    }

    /**
     * @InheritDoc
     */
    @Override
    public Event copyFor(Object newSource, EventTarget newTarget) {
        GestureEvent e = (GestureEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    /**
     * Copies all private fields (except of event type) from one event to
     * another event. This is for implementing impl_copy in subclasses.
     */
    static void copyFields(GestureEvent from, GestureEvent to,
            Object source, EventTarget target) {
        to.x = from.x;
        to.y = from.y;
        to.screenX = from.screenX;
        to.screenY = from.screenY;
        to.sceneX = from.sceneX;
        to.sceneY = from.sceneY;
        to.shiftDown = from.shiftDown;
        to.controlDown = from.controlDown;
        to.altDown = from.altDown;
        to.metaDown = from.metaDown;
        to.source = source;
        to.target = target;

        from.recomputeCoordinatesToSource(to, source);
    }

    private double x;

    /**
     * Gets the horizontal position of the event relative to the
     * origin of the event's source.
     *
     * @return the horizontal position of the event relative to the
     * origin of the event's source.
     *
     * @see #isDirect() 
     */
    public final double getX() {
        return x;
    }

    private double y;

    /**
     * Gets the vertical position of the event relative to the
     * origin of the event's source.
     *
     * @return the vertical position of the event relative to the
     * origin of the event's source.
     *
     * @see #isDirect()
     */
    public final double getY() {
        return y;
    }

    private double screenX;

    /**
     * Gets the absolute horizontal position of the event.
     * @return the absolute horizontal position of the event
     *
     * @see #isDirect()
     */
    public final double getScreenX() {
        return screenX;
    }

    private double screenY;

    /**
     * Gets the absolute vertical position of the event.
     * @return the absolute vertical position of the event
     *
     * @see #isDirect()
     */
    public final double getScreenY() {
        return screenY;
    }

    private double sceneX;

    /**
     * Gets the horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the event's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the event's node.
     *
     * @return the horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the event's source
     *
     * @see #isDirect()
     */
    public final double getSceneX() {
        return sceneX;
    }

    private double sceneY;

    /**
     * Gets the vertical position of the event relative to the
     * origin of the {@code Scene} that contains the event's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the event's node.
     *
     * @return the vertical position of the event relative to the
     * origin of the {@code Scene} that contains the event's source
     *
     * @see #isDirect()
     */
    public final double getSceneY() {
        return sceneY;
    }

    private boolean shiftDown;

    /**
     * Indicates whether or not the Shift modifier is down on this event.
     * @return true if the Shift modifier is down on this event
     */
    public final boolean isShiftDown() {
        return shiftDown;
    }

    private boolean controlDown;

    /**
     * Indicates whether or not the Control modifier is down on this event.
     * @return true if the Control modifier is down on this event
     */
    public final boolean isControlDown() {
        return controlDown;
    }

    private boolean altDown;

    /**
     * Indicates whether or not the Alt modifier is down on this event.
     * @return true if the Alt modifier is down on this event
     */
    public final boolean isAltDown() {
        return altDown;
    }

    private boolean metaDown;

    /**
     * Indicates whether or not the Meta modifier is down on this event.
     * @return true if the Meta modifier is down on this event
     */
    public final boolean isMetaDown() {
        return metaDown;
    }

    private boolean direct;

    /**
     * Indicates whether this gesture is caused by a direct or indirect input
     * device. With direct input device the gestures are performed directly at
     * the concrete coordinates, a typical example would be a touch screen.
     * With indirect device the gestures are performed indirectly and usually
     * mouse cursor position is used as the gesture coordinates, a typical
     * example would be a track pad.
     * @return true if this event is caused by a direct input device
     */
    public final boolean isDirect() {
        return direct;
    }

    private boolean inertia;

    /**
     * Indicates if this event represents an inertia of an already finished
     * gesture.
     * @return true if this is an inertia event
     */
    public boolean isInertia() {
        return inertia;
    }

    /**
     * Indicates whether or not the host platform common shortcut modifier is
     * down on this event. This common shortcut modifier is a modifier key which
     * is used commonly in shortcuts on the host platform. It is for example
     * {@code control} on Windows and {@code meta} (command key) on Mac.
     *
     * @return {@code true} if the shortcut modifier is down, {@code false}
     *      otherwise
     */
    public final boolean isShortcutDown() {
        switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
            case SHIFT:
                return shiftDown;

            case CONTROL:
                return controlDown;

            case ALT:
                return altDown;

            case META:
                return metaDown;

            default:
                return false;
        }
    }

    /**
     * Returns a string representation of this {@code GestureEvent} object.
     * @return a string representation of this {@code GestureEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("GestureEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY());
        sb.append(isDirect() ? ", direct" : ", indirect");

        if (isShiftDown()) {
            sb.append(", shiftDown");
        }
        if (isControlDown()) {
            sb.append(", controlDown");
        }
        if (isAltDown()) {
            sb.append(", altDown");
        }
        if (isMetaDown()) {
            sb.append(", metaDown");
        }
        if (isShortcutDown()) {
            sb.append(", shortcutDown");
        }

        return sb.append("]").toString();
    }
}
