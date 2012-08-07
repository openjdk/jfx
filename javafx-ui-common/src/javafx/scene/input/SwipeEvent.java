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
 * @since 2.2
 */
public class SwipeEvent extends GestureEvent {

    /**
     * Common supertype for all swipe event types.
     */
    public static final EventType<SwipeEvent> ANY =
            EventTypeUtil.registerInternalEventType(GestureEvent.ANY, "ANY_SWIPE");

    /**
     * This event occurs when user performs leftward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_LEFT =
            EventTypeUtil.registerInternalEventType(SwipeEvent.ANY, "SWIPE_LEFT");

    /**
     * This event occurs when user performs rightward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_RIGHT =
            EventTypeUtil.registerInternalEventType(SwipeEvent.ANY, "SWIPE_RIGHT");

    /**
     * This event occurs when user performs upward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_UP =
            EventTypeUtil.registerInternalEventType(SwipeEvent.ANY, "SWIPE_UP");

    /**
     * This event occurs when user performs downward swipe gesture.
     */
    public static final EventType<SwipeEvent> SWIPE_DOWN =
            EventTypeUtil.registerInternalEventType(SwipeEvent.ANY, "SWIPE_DOWN");

    private SwipeEvent(final EventType<? extends SwipeEvent> eventType) {
        super(eventType);
    }

    private SwipeEvent(Object source, EventTarget target,
            final EventType<? extends SwipeEvent> eventType) {
        super(source, target, eventType);
    }

    private SwipeEvent(final EventType<? extends SwipeEvent> eventType,
            int touchCount,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct) {

        super(eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, false);
        this.touchCount = touchCount;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    public static SwipeEvent impl_swipeEvent(final EventType<? extends SwipeEvent> eventType,
            int touchCount,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct) {
        return new SwipeEvent(eventType, touchCount,
                x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct);
    }


    private int touchCount;

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
