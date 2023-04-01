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

package javafx.stage;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event related to window showing/hiding actions.
 * @since JavaFX 2.0
 */
public class WindowEvent extends Event {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all window event types.
     */
    public static final EventType<WindowEvent> ANY =
            new EventType<>(Event.ANY, "WINDOW");

    /**
     * This event occurs on window just before it is shown.
     */
    public static final EventType<WindowEvent> WINDOW_SHOWING =
            new EventType<>(WindowEvent.ANY, "WINDOW_SHOWING");

    /**
     * This event occurs on window just after it is shown.
     */
    public static final EventType<WindowEvent> WINDOW_SHOWN =
            new EventType<>(WindowEvent.ANY, "WINDOW_SHOWN");

    /**
     * This event occurs on window just before it is hidden.
     */
    public static final EventType<WindowEvent> WINDOW_HIDING =
            new EventType<>(WindowEvent.ANY, "WINDOW_HIDING");

    /**
     * This event occurs on window just after it is hidden.
     */
    public static final EventType<WindowEvent> WINDOW_HIDDEN =
            new EventType<>(WindowEvent.ANY, "WINDOW_HIDDEN");

    /**
     * This event is delivered to a
     * window when there is an external request to close that window. If the
     * event is not consumed by any installed window event handler, the default
     * handler for this event closes the corresponding window.
     */
    public static final EventType<WindowEvent> WINDOW_CLOSE_REQUEST =
            new EventType<>(WindowEvent.ANY, "WINDOW_CLOSE_REQUEST");

    /**
     * Construct a new {@code Event} with the specified event source, target
     * and type. If the source or target is set to {@code null}, it is replaced
     * by the {@code NULL_SOURCE_TARGET} value.
     *
     * @param source    the event source which sent the event
     * @param eventType the event type
     */
    public WindowEvent(final @NamedArg("source") Window source, final @NamedArg("eventType") EventType<? extends Event> eventType) {
        super(source, source, eventType);
    }

    /**
     * Returns a string representation of this {@code WindowEvent} object.
     * @return a string representation of this {@code WindowEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("WindowEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        return sb.append("]").toString();
    }

    @Override
    public WindowEvent copyFor(Object newSource, EventTarget newTarget) {
        return (WindowEvent) super.copyFor(newSource, newTarget);
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param newSource the new source of the copied event
     * @param newTarget the new target of the copied event
     * @param type the new eventType
     * @return the event copy with the fields substituted
     * @since JavaFX 8.0
     */
    public WindowEvent copyFor(Object newSource, EventTarget newTarget, EventType<WindowEvent> type) {
        WindowEvent e = copyFor(newSource, newTarget);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<WindowEvent> getEventType() {
        return (EventType<WindowEvent>) super.getEventType();
    }


}
