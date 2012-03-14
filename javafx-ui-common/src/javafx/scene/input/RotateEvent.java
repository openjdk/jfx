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
 * the concrete coordinates as on touch screen) or indirect (performed
 * indirectly as on track pad - the mouse cursor location is usually used
 * as the gesture coordinates).
 * <p>
 * The gesture's {@code ROTATE} events are surounded by {@code ROTATION_STARTED}
 * and {@code ROTATION_FINISHED} events. If rotation inertia is active on the
 * given platform, some {@code ROTATE} events with {@code isInertia()} returning
 * {@code true} can come after {@code ROTATION_FINISHED}.
 */
public class RotateEvent extends GestureEvent {

    /**
     * Common supertype for all rotate event types.
     */
    public static final EventType<RotateEvent> ANY =
            new EventType<RotateEvent>(GestureEvent.ANY, "ANY_ROTATE");

    /**
     * This event occurs when user performs a rotating gesture such as
     * dragging two fingers around each other.
     */
    public static final EventType<RotateEvent> ROTATE =
            new EventType<RotateEvent>(RotateEvent.ANY, "ROTATE");

    /**
     * This event occurs when a rotating gesture is detected.
     */
    public static final EventType<RotateEvent> ROTATION_STARTED =
            new EventType<RotateEvent>(RotateEvent.ANY, "ROTATION_STARTED");

    /**
     * This event occurs when a rotating gesture ends.
     */
    public static final EventType<RotateEvent> ROTATION_FINISHED =
            new EventType<RotateEvent>(RotateEvent.ANY, "ROTATION_FINISHED");

    private RotateEvent(final EventType<? extends RotateEvent> eventType) {
        super(eventType);
    }

    private RotateEvent(Object source, EventTarget target,
            final EventType<? extends RotateEvent> eventType) {
        super(source, target, eventType);
    }

    private RotateEvent(final EventType<? extends RotateEvent> eventType,
            double angle, double totalAngle,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia) {

        super(eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia);
        this.angle = angle;
        this.totalAngle = totalAngle;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    public static RotateEvent impl_rotateEvent(final EventType<? extends RotateEvent> eventType,
            double angle, double totalAngle,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia) {
        return new RotateEvent(eventType, angle, totalAngle,
                x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia);
    }

    private double angle;

    /**
     * Gets the rotation angle of this event.
     * The angle is in degrees and work well when added to the node's
     * {@code rotate} property value (positive values for clockwise rotation).
     * @return The rotation angle of this event
     */
    public double getAngle() {
        return angle;
    }

    private double totalAngle;

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
