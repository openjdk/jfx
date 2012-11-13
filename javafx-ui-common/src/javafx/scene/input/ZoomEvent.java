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
 * Zoom event indicates that user performed zooming gesture such as
 * dragging two fingers apart on track pad, touch screen or other
 * similar device.
 * <p>
 * The event is delivered to the top-most
 * node picked on the gesture coordinates in time of the gesture start - the
 * whole gesture is delivered to the same node even if the coordinates change
 * during the gesture.
 * <p>
 * The event provides two values: {@code zoomFactor} is the zooming amount
 * of this event, {@code totalZoomFactor} is the zooming amount of the whole
 * gesture. The values work well when multiplied with the node's {@code scale}
 * properties (values greater than {@code 1} for zooming in).
 * <p>
 * As all gestures, zooming can be direct (performed directly at
 * the concrete coordinates as on touch screen - the center point among all
 * the touches is usually used as the gesture coordinates) or indirect (performed
 * indirectly as on track pad - the mouse cursor location is usually used
 * as the gesture coordinates).
 * <p>
 * The gesture's {@code ZOOM} events are surounded by {@code ZOOM_STARTED}
 * and {@code ZOOM_FINISHED} events. If zooming inertia is active on the
 * given platform, some {@code ZOOM} events with {@code isInertia()} returning
 * {@code true} can come after {@code ZOOM_FINISHED}.
 *
 * @since 2.2
 */
public final class ZoomEvent extends GestureEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all zoom event types.
     */
    public static final EventType<ZoomEvent> ANY =
            new EventType<ZoomEvent>(GestureEvent.ANY, "ANY_ZOOM");

    /**
     * This event occurs when user performs a zooming gesture such as
     * dragging two fingers apart.
     */
    public static final EventType<ZoomEvent> ZOOM =
            new EventType<ZoomEvent>(ZoomEvent.ANY, "ZOOM");

    /**
     * This event occurs when a zooming gesture is detected.
     */
    public static final EventType<ZoomEvent> ZOOM_STARTED =
            new EventType<ZoomEvent>(ZoomEvent.ANY, "ZOOM_STARTED");

    /**
     * This event occurs when a zooming gesture ends.
     */
    public static final EventType<ZoomEvent> ZOOM_FINISHED =
            new EventType<ZoomEvent>(ZoomEvent.ANY, "ZOOM_FINISHED");

    /**
     * Constructs new ZoomEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
     * @param y The y with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param touchCount number of touch points
     * @param inertia if represents inertia of an already finished gesture.
     * @param zoomFactor zoom amount
     * @param totalZoomFactor cumulative zoom amount
     */
    public ZoomEvent(Object source, EventTarget target, final EventType<ZoomEvent> eventType,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia,
            double zoomFactor,
            double totalZoomFactor) {

        super(source, target, eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia);
        this.zoomFactor = zoomFactor;
        this.totalZoomFactor = totalZoomFactor;
    }

    /**
     * Constructs new ZoomEvent event with null source and target.
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
     * @param touchCount number of touch points
     * @param inertia if represents inertia of an already finished gesture.
     * @param zoomFactor zoom amount
     * @param totalZoomFactor cumulative zoom amount
     */
    public ZoomEvent(final EventType<ZoomEvent> eventType,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia,
            double zoomFactor,
            double totalZoomFactor) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, zoomFactor, totalZoomFactor);
    }

    private final double zoomFactor;

    /**
     * Gets the zooming amount of this event. The factor value works well when
     * multiplied with the node's {@code scale} properties (values greater
     * than {@code 1} for zooming in, values between {@code 0} and {@code 1}
     * for zooming out).
     * @return The zooming amount of this event
     */
    public double getZoomFactor() {
        return zoomFactor;
    }

    private final double totalZoomFactor;

    /**
     * Gets the zooming amount of this gesture. The factor value works well when
     * multiplied with the node's {@code scale} properties (values greater
     * than {@code 1} for zooming in, values between {@code 0} and {@code 1}
     * for zooming out).
     * @return The cumulative zooming amount of this gesture
     */
    public double getTotalZoomFactor() {
        return totalZoomFactor;
    }

    /**
     * Returns a string representation of this {@code ZoomEvent} object.
     * @return a string representation of this {@code ZoomEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("ZoomEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", zoomFactor = ").append(getZoomFactor());
        sb.append(", totalZoomFactor = ").append(getTotalZoomFactor());
        sb.append(", x = ").append(getX()).append(", y = ").append(getY());
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

        return sb.append("]").toString();
    }

    @Override
    public ZoomEvent copyFor(Object newSource, EventTarget newTarget) {
        return (ZoomEvent) super.copyFor(newSource, newTarget);
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param source the new source of the copied event
     * @param target the new target of the copied event
     * @param eventType the new eventType
     * @return the event copy with the fields substituted
     */
    public ZoomEvent copyFor(Object newSource, EventTarget newTarget, EventType<ZoomEvent> type) {
        ZoomEvent e = copyFor(newSource, newTarget);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<ZoomEvent> getEventType() {
        return (EventType<ZoomEvent>) super.getEventType();
    }
    
    
}
