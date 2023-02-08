/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * Swipe event indicates that user performed a swipe gesture such as
 * dragging a finger in one direction on touch screen.
 * <p>
 * Unlike some other gestures, the swipe gesture is not continual - the whole
 * gesture produces only one event. The event is delivered to the top-most
 * node picked on the gesture coordinates.
 * <p>
 * The swipe gesture has four types according to the movement direction.
 * The gesture can be performed by any number of touch points, the number
 * is provided by {@code getTouchCount()} method.
 * <p>
 * Note that swipe and scroll gestures are not exclusive. A single touch screen
 * action can result in both gestures being delivered.
 * <p>
 * Note that the capability to produce swipes is dependent on the used input
 * devices and underlying platform's capabilities and settings (especially
 * without touch-screen user's possibilities of producing swipes are
 * significantly reduced).
 * <p>
 * As all gestures, swipe can be direct (performed directly at
 * the concrete coordinates as on touch screen - the center of the gesture
 * is used as gesture coordinates) or indirect (performed
 * indirectly as on track pad - the mouse cursor location is usually used
 * as the gesture coordinates in this case).
 *
 * @since JavaFX 2.2
 */
public final class SwipeEvent extends GestureEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all swipe event types.
     */
    public static final EventType<SwipeEvent> ANY =
            new EventType<>(GestureEvent.ANY, "ANY_SWIPE");

    /**
     * This event occurs when user performs leftward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_LEFT =
            new EventType<>(SwipeEvent.ANY, "SWIPE_LEFT");

    /**
     * This event occurs when user performs rightward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_RIGHT =
            new EventType<>(SwipeEvent.ANY, "SWIPE_RIGHT");

    /**
     * This event occurs when user performs upward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_UP =
            new EventType<>(SwipeEvent.ANY, "SWIPE_UP");

    /**
     * This event occurs when user performs downward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_DOWN =
            new EventType<>(SwipeEvent.ANY, "SWIPE_DOWN");

    /**
     * Constructs new SwipeEvent event.
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
     * @param touchCount number of touch points
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and the target
     * @since JavaFX 8.0
     */
    public SwipeEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            final @NamedArg("eventType") EventType<SwipeEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct,
            @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {

        super(source, target, eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, false,
                pickResult);
        this.touchCount = touchCount;
    }

    /**
     * Constructs new SwipeEvent event with null source and target.
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
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public SwipeEvent(final @NamedArg("eventType") EventType<SwipeEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct,
            @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, touchCount, pickResult);
    }

    private final int touchCount;

    /**
     * Gets number of touch points that caused this event.
     * @return Number of touch points that caused this event
     */
    public int getTouchCount() {
        return touchCount;
    }

    /**
     * Returns a string representation of this {@code SwipeEvent} object.
     * @return a string representation of this {@code SwipeEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("SwipeEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());
        sb.append(", touchCount = ").append(getTouchCount());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY())
                .append(", z = ").append(getZ());
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
        sb.append(", pickResult = ").append(getPickResult());

        return sb.append("]").toString();
    }

    @Override
    public SwipeEvent copyFor(Object newSource, EventTarget newTarget) {
        return (SwipeEvent) super.copyFor(newSource, newTarget);
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param newSource the new source of the copied event
     * @param newTarget the new target of the copied event
     * @param type the new eventType
     * @return the event copy with the fields substituted
     * @since JavaFX 8.0
     */
    public SwipeEvent copyFor(Object newSource, EventTarget newTarget, EventType<SwipeEvent> type) {
        SwipeEvent e = copyFor(newSource, newTarget);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<SwipeEvent> getEventType() {
        return (EventType<SwipeEvent>) super.getEventType();
    }


}
