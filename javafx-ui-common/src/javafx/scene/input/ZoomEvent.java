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

import com.sun.javafx.event.EventTypeUtil;
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
 */
public class ZoomEvent extends GestureEvent {

    /**
     * Common supertype for all zoom event types.
     */
    public static final EventType<ZoomEvent> ANY =
            EventTypeUtil.registerInternalEventType(GestureEvent.ANY, "ANY_ZOOM");

    /**
     * This event occurs when user performs a zooming gesture such as
     * dragging two fingers apart.
     */
    public static final EventType<ZoomEvent> ZOOM =
            EventTypeUtil.registerInternalEventType(ZoomEvent.ANY, "ZOOM");

    /**
     * This event occurs when a zooming gesture is detected.
     */
    public static final EventType<ZoomEvent> ZOOM_STARTED =
            EventTypeUtil.registerInternalEventType(ZoomEvent.ANY, "ZOOM_STARTED");

    /**
     * This event occurs when a zooming gesture ends.
     */
    public static final EventType<ZoomEvent> ZOOM_FINISHED =
            EventTypeUtil.registerInternalEventType(ZoomEvent.ANY, "ZOOM_FINISHED");

    private ZoomEvent(final EventType<? extends ZoomEvent> eventType) {
        super(eventType);
    }

    private ZoomEvent(Object source, EventTarget target,
            final EventType<? extends ZoomEvent> eventType) {
        super(source, target, eventType);
    }

    private ZoomEvent(final EventType<? extends ZoomEvent> eventType,
            double zoomFactor,
            double totalZoomFactor,
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
        this.zoomFactor = zoomFactor;
        this.totalZoomFactor = totalZoomFactor;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    public static ZoomEvent impl_zoomEvent(final EventType<? extends ZoomEvent> eventType,
            double zoomFactor,
            double totalZoomFactor,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia) {
        return new ZoomEvent(eventType, zoomFactor, totalZoomFactor,
                x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia);
    }


    private double zoomFactor;

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

    private double totalZoomFactor;

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
