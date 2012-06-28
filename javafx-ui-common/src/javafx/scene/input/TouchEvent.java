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

import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;
import com.sun.javafx.event.EventTypeUtil;
import java.util.Collections;
import java.util.List;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Touch event indicates a touch screen action. It contains detailed information
 * about each particular touch point.
 * <p>
 * Touch point represents a single touched finger and has its location,
 * state (pressed/moved/released/stationary) and an ID unique in scope of a
 * single gesture. For detailed reference see {@link TouchPoint}.
 * <p>
 * For each multi-touch action a set of touch events is generated - for each
 * touch point one. The event has type corresponds to its touch point's state.
 * Each of the events also contain
 * list of all the touch points. This design allows for handling complicated
 * multi-touch actions from one place while keeping it possible to
 * filter/consume each touch point separately. To recognize
 * which events belong into a single set there is {@code getEventSetId()}
 * method.
 * <p>
 * Each touch point is - similarly to mouse dragging - delivered to a single
 * node on which it was pressed, regardless of where it moves then. It is
 * possible to change this behavior by using a grabbing mechanism described
 * in {@link TouchPoint} documentation.
 */
public final class TouchEvent extends InputEvent {

    /**
     * Common supertype for all touch event types.
     */
    public static final EventType<TouchEvent> ANY =
            EventTypeUtil.registerInternalEventType(InputEvent.ANY, "TOUCH");

    /**
     * This event occurs when the touch point is pressed (touched for the
     * first time).
     */
    public static final EventType<TouchEvent> TOUCH_PRESSED =
            EventTypeUtil.registerInternalEventType(ANY, "TOUCH_PRESSED");

    /**
     * This event occurs when the touch point is moved.
     */
    public static final EventType<TouchEvent> TOUCH_MOVED =
            EventTypeUtil.registerInternalEventType(ANY, "TOUCH_MOVED");

    /**
     * This event occurs when the touch point is released.
     */
    public static final EventType<TouchEvent> TOUCH_RELEASED =
            EventTypeUtil.registerInternalEventType(ANY, "TOUCH_RELEASED");

    /**
     * This event occurs when the touch point is pressed and still (doesn't
     * move).
     */
    public static final EventType<TouchEvent> TOUCH_STATIONARY =
            EventTypeUtil.registerInternalEventType(ANY, "TOUCH_STATIONARY");

    private TouchEvent(EventType<? extends TouchEvent> eventType) {
        super(eventType);
    }

    private TouchEvent(EventType<? extends TouchEvent> eventType,
            TouchPoint touchPoint, List<TouchPoint> touchPoints, int eventSetId,
            boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown) {
        super(eventType);
        if (touchPoints != null) {
            this.touchPoints = Collections.unmodifiableList(touchPoints);
        }
        this.eventSetId = eventSetId;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        this.touchPoint = touchPoint;
    }

    /**
     * Returns number of touch points represented by this touch event set.
     * The returned number matches the size of the {@code touchPoints} list.
     * @return The number of touch points represented by this touch event set.
     */
    public int getTouchCount() {
        return touchPoints.size();
    }

    /**
     * Recomputes touch event for the given event source object.
     * @param event Event to modify
     * @param oldSource Source object of the current values
     * @param newSource Source object to compute values for
     */
    private static void recomputeToSource(TouchEvent event, Object oldSource,
            Object newSource) {

        for (TouchPoint tp : event.touchPoints) {
            tp.recomputeToSource(oldSource, newSource);
        }
    }


    /**
     * @inheritDoc
     */
    @Override
    public Event copyFor(Object newSource, EventTarget newTarget) {
        TouchEvent e = (TouchEvent) super.copyFor(newSource, newTarget);
        recomputeToSource(e, getSource(), newSource);

        return e;
    }

    // isDirect doesn't currently have public getter because we are currently
    // ignoring indirect touch events and claim that touch events always
    // represent a direct touch-screen action
    private boolean isDirect;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public boolean impl_isDirect() {
        return isDirect;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setDirect(boolean direct) {
        isDirect = direct;
    }

    private int eventSetId;

    /**
     * Gets sequential number of the set of touch events representing the same
     * multi-touch action. For a multi-touch user action, number of touch points
     * may exist; each of them produces a touch event, each of those touch
     * events carry the same list of touch points - and all of them return the
     * same number from this method. Then state of some of the touch points
     * changes and the new set of events has new id. The id is guaranteed
     * to be sequential and unique in scope of one gesture (is reset when
     * all touch points are released).
     *
     * @return Sequential id of event set unique in scope of a gesture
     */
    public final int getEventSetId() {
        return eventSetId;
    }


    /**
     * Whether or not the Shift modifier is down on this event.
     */
    private boolean shiftDown;

    /**
     * Whether or not the Shift modifier is down on this event.
     * @return true if the Shift modifier is down on this event
     */
    public final boolean isShiftDown() {
        return shiftDown;
    }

    /**
     * Whether or not the Control modifier is down on this event.
     */
    private boolean controlDown;

    /**
     * Whether or not the Control modifier is down on this event.
     * @return true if the Control modifier is down on this event
     */
    public final boolean isControlDown() {
        return controlDown;
    }

    /**
     * Whether or not the Alt modifier is down on this event.
     */
    private boolean altDown;

    /**
     * Whether or not the Alt modifier is down on this event.
     * @return true if the Alt modifier is down on this event
     */
    public final boolean isAltDown() {
        return altDown;
    }

    /**
     * Whether or not the Meta modifier is down on this event.
     */
    private boolean metaDown;

    /**
     * Whether or not the Meta modifier is down on this event.
     * @return true if the Meta modifier is down on this event
     */
    public final boolean isMetaDown() {
        return metaDown;
    }

    private TouchPoint touchPoint;

    /**
     * Gets the touch point of this event.
     * @return Touch point of this event
     */
    public TouchPoint getTouchPoint() {
        return touchPoint;
    }

    private List<TouchPoint> touchPoints;

    /**
     * Gets all the touch points represented by this set of touch events,
     * including the touch point of this event. The list is unmodifiable and 
     * is sorted by their IDs, which means it is also sorted by the time
     * they were pressed. To distinguish between touch points belonging to
     * a node and unrelated touch points, TouchPoint's {@code belongsTo}
     * method can be used.
     * @return All current touch points in an unmodifiable list
     */
    @ReturnsUnmodifiableCollection 
    public List<TouchPoint> getTouchPoints() {
        return touchPoints;
    }

    /**
     * Returns a string representation of this {@code TouchEvent} object.
     * @return a string representation of this {@code TouchEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("TouchEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());
        sb.append(", touchCount = ").append(getTouchCount());
        sb.append(", eventSetId = ").append(getEventSetId());

        sb.append(", touchPoint = ").append(getTouchPoint().toString());

        return sb.append("]").toString();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static TouchEvent impl_touchEvent(EventType<? extends TouchEvent> eventType,
            TouchPoint touchPoint, List<TouchPoint> touchPoints, int eventSetId,
            boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown) {
        return new TouchEvent(eventType, touchPoint, touchPoints, eventSetId,
                shiftDown, controlDown, altDown, metaDown);
    }

}
