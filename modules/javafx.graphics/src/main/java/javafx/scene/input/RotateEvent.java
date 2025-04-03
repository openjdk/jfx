/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Rotate event indicates that user performed rotating gesture such as
 * dragging two fingers around each other on track pad,
 * touch screen or other similar device.
 * <p>
 * The event is delivered to the top-most
 * node picked on the gesture coordinates in time of the gesture start - the
 * whole gesture is delivered to the same node even if the coordinates change
 * during the gesture.
 * <p>
 * The event provides two values: {@code angle} is the rotation angle of this
 * event, {@code totalAngle} is the rotation angle of the whole gesture. Both
 * values are in degrees and work well when added to the node's {@code rotate}
 * property value (positive values for clockwise rotation).
 * <p>
 * As all gestures, rotation can be direct (performed directly at
 * the concrete coordinates as on touch screen - the center point among all
 * the touches is usually used as the gesture coordinates) or indirect (performed
 * indirectly as on track pad - the mouse cursor location is usually used
 * as the gesture coordinates).
 * <p>
 * The gesture's {@code ROTATE} events are surrounded by {@code ROTATION_STARTED}
 * and {@code ROTATION_FINISHED} events. If rotation inertia is active on the
 * given platform, some {@code ROTATE} events with {@code isInertia()} returning
 * {@code true} can come after {@code ROTATION_FINISHED}.
 *
 * @since JavaFX 2.2
 */
public final class RotateEvent extends GestureEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all rotate event types.
     */
    public static final EventType<RotateEvent> ANY =
            new EventType<>(GestureEvent.ANY, "ANY_ROTATE");

    /**
     * This event occurs when user performs a rotating gesture such as
     * dragging two fingers around each other.
     */
    public static final EventType<RotateEvent> ROTATE =
            new EventType<>(RotateEvent.ANY, "ROTATE");

    /**
     * This event occurs when a rotating gesture is detected.
     */
    public static final EventType<RotateEvent> ROTATION_STARTED =
            new EventType<>(RotateEvent.ANY, "ROTATION_STARTED");

    /**
     * This event occurs when a rotating gesture ends.
     */
    public static final EventType<RotateEvent> ROTATION_FINISHED =
            new EventType<>(RotateEvent.ANY, "ROTATION_FINISHED");

    /**
     * Constructs new RotateEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param angle the rotational angle
     * @param totalAngle the cumulative rotational angle
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and the target
     * @since JavaFX 8.0
     */
    public RotateEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            final @NamedArg("eventType") EventType<RotateEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct,
            @NamedArg("inertia") boolean inertia, @NamedArg("angle") double angle, @NamedArg("totalAngle") double totalAngle,
            @NamedArg("pickResult") PickResult pickResult) {

        super(source, target, eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia,
                pickResult);
        this.angle = angle;
        this.totalAngle = totalAngle;
    }

    /**
     * Constructs new RotateEvent event with null source and target
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param angle the rotational angle
     * @param totalAngle the cumulative rotational angle
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public RotateEvent(final @NamedArg("eventType") EventType<RotateEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct,
            @NamedArg("inertia") boolean inertia, @NamedArg("angle") double angle, @NamedArg("totalAngle") double totalAngle,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, angle, totalAngle, pickResult);
    }

    @SuppressWarnings("doclint:missing")
    private final double angle;

    /**
     * Gets the rotation angle of this event.
     * The angle is in degrees and work well when added to the node's
     * {@code rotate} property value (positive values for clockwise rotation).
     * @return The rotation angle of this event
     */
    public double getAngle() {
        return angle;
    }

    @SuppressWarnings("doclint:missing")
    private final double totalAngle;

    /**
     * Gets the cumulative rotation angle of this gesture.
     * The angle is in degrees and work well when added to the node's
     * {@code rotate} property value (positive values for clockwise rotation).
     * @return The cumulative rotation angle of this gesture
     */
    public double getTotalAngle() {
        return totalAngle;
    }

    /**
     * Returns a string representation of this {@code RotateEvent} object.
     * @return a string representation of this {@code RotateEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("RotateEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", angle = ").append(getAngle());
        sb.append(", totalAngle = ").append(getTotalAngle());
        sb.append(", x = ").append(getX()).append(", y = ").append(getY())
                .append(", z = ").append(getZ());
        sb.append(isDirect() ? ", direct" : ", indirect");

        if (isInertia()) {
            sb.append(", inertia");
        }

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
        sb.append(", pickResult = ").append(getPickResult());

        return sb.append("]").toString();
    }

    @Override
    public RotateEvent copyFor(Object newSource, EventTarget newTarget) {
        return (RotateEvent) super.copyFor(newSource, newTarget);
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param newSource the new source of the copied event
     * @param newTarget the new target of the copied event
     * @param type the new eventType
     * @return the event copy with the fields substituted
     * @since JavaFX 8.0
     */
    public RotateEvent copyFor(Object newSource, EventTarget newTarget, EventType<RotateEvent> type) {
        RotateEvent e = copyFor(newSource, newTarget);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<RotateEvent> getEventType() {
        return (EventType<RotateEvent>) super.getEventType();
    }


}
