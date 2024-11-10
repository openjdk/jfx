/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.event;

import com.sun.javafx.event.EventUtil;
import com.sun.javafx.event.UnconsumedEventHandler;
import java.util.ArrayList;
import java.util.EventObject;
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import javafx.beans.NamedArg;

// PENDING_DOC_REVIEW
/**
 * Base class for FX events. Each FX event has associated an event source,
 * event target and an event type. The event source specifies for an event
 * handler the object on which that handler has been registered and which sent
 * the event to it. The event target defines the path through which the event
 * will travel when posted. The event type provides additional classification
 * to events of the same {@code Event} class.
 * @since JavaFX 2.0
 */
public class Event extends EventObject implements Cloneable {

    static {
        EventUtil.setAccessor(new EventUtil.Accessor() {
            @Override
            public List<UnconsumedEventHandler> getUnconsumedEventHandlers(Event event) {
                return event.unconsumedEventHandlers;
            }

            @Override
            public void markDeliveryCompleted(Event event) {
                event.completed = true;
            }
        });
    }

    @Serial
    private static final long serialVersionUID = 20241110L;

    /**
     * The constant which represents an unknown event source / target.
     */
    public static final EventTarget NULL_SOURCE_TARGET = tail -> tail;

    /**
     * Common supertype for all event types.
     */
    public static final EventType<Event> ANY = EventType.ROOT;

    /**
     * Type of the event.
     */
    protected EventType<? extends Event> eventType;

    /**
     * Event target that defines the path through which the event
     * will travel when posted.
     */
    protected transient EventTarget target;

    /**
     * The list of handlers that have expressed their interest in handling the event
     * if it is still unconsumed at the end of the bubble phase of event delivery.
     */
    private transient List<UnconsumedEventHandler> unconsumedEventHandlers;

    /**
     * Whether this event has been consumed by any filter or handler.
     */
    protected boolean consumed;

    /**
     * Indicates whether this event has completed both delivery phases and can no
     * longer accept registrations of unconsumed event handlers.
     */
    private boolean completed;

    /**
     * Construct a new {@code Event} with the specified event type. The source
     * and target of the event is set to {@code NULL_SOURCE_TARGET}.
     *
     * @param eventType the event type
     */
    public Event(final @NamedArg("eventType") EventType<? extends Event> eventType) {
        this(null, null, eventType);
    }

    /**
     * Construct a new {@code Event} with the specified event source, target
     * and type. If the source or target is set to {@code null}, it is replaced
     * by the {@code NULL_SOURCE_TARGET} value.
     *
     * @param source the event source which sent the event
     * @param target the event target to associate with the event
     * @param eventType the event type
     */
    public Event(final @NamedArg("source") Object source,
                 final @NamedArg("target") EventTarget target,
                 final @NamedArg("eventType") EventType<? extends Event> eventType) {
        super((source != null) ? source : NULL_SOURCE_TARGET);
        this.target = (target != null) ? target : NULL_SOURCE_TARGET;
        this.eventType = eventType;
    }

    /**
     * Returns the event target of this event. The event target specifies
     * the path through which the event will travel when posted.
     *
     * @return the event target
     */
    public EventTarget getTarget() {
        return target;
    }

    /**
     * Gets the event type of this event. Objects of the same {@code Event}
     * class can have different event types. These event types further specify
     * what kind of event occurred.
     *
     * @return the event type
     */
    public EventType<? extends Event> getEventType() {
        return eventType;
    }

    /**
     * Creates and returns a copy of this event with the specified event source
     * and target. If the source or target is set to {@code null}, it is
     * replaced by the {@code NULL_SOURCE_TARGET} value.
     *
     * @param newSource the new source of the copied event
     * @param newTarget the new target of the copied event
     * @return the event copy with the new source and target
     */
    public Event copyFor(final Object newSource, final EventTarget newTarget) {
        final Event newEvent = (Event) clone();

        newEvent.source = (newSource != null) ? newSource : NULL_SOURCE_TARGET;
        newEvent.target = (newTarget != null) ? newTarget : NULL_SOURCE_TARGET;
        newEvent.consumed = false;

        return newEvent;
    }

    /**
     * Indicates whether this {@code Event} has been consumed by any filter or
     * handler.
     *
     * @return {@code true} if this {@code Event} has been consumed,
     *     {@code false} otherwise
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Marks this {@code Event} as consumed. This stops its further propagation.
     */
    public void consume() {
        consumed = true;
    }

    /**
     * Specifies an event handler that will handle this event if it is still unconsumed after both phases
     * of event delivery have completed. The unconsumed event handlers are invoked in the order they were
     * registered. As soon as an event handler consumes the event, further propagation is stopped.
     * <p>
     * This method can only be called from an event filter or event handler during event delivery; any
     * attempt to call it after event delivery is complete will fail with {@link IllegalStateException}.
     *
     * @param handler the event handler
     * @param <E> the type of the event
     * @throws IllegalStateException when event delivery has already been completed
     * @since 24
     */
    public final <E extends Event> void ifUnconsumed(EventHandler<E> handler) {
        if (completed) {
            throw new IllegalStateException("Event delivery is not in progress");
        }

        if (unconsumedEventHandlers == null) {
            unconsumedEventHandlers = new ArrayList<>(2); // most of the time we only expect a single handler
        }

        @SuppressWarnings("unchecked")
        EventHandler<Event> untypedHandler = (EventHandler<Event>)handler;
        unconsumedEventHandlers.add(new UnconsumedEventHandler(this, untypedHandler));
    }

    /**
     * Discards all event handlers that were added with {@link #ifUnconsumed(EventHandler)}.
     *
     * @since 24
     */
    public final void discardUnconsumedEventHandlers() {
        if (!completed && unconsumedEventHandlers != null) {
            unconsumedEventHandlers.clear();
        }
    }

    /**
     * Creates and returns a copy of this {@code Event}.
     * @return a new instance of {@code Event} with all values copied from
     * this {@code Event}.
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            // we implement Cloneable, this shouldn't happen
            throw new RuntimeException("Can't clone Event");
        }
    }

    @SuppressWarnings("doclint:missing")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        source = NULL_SOURCE_TARGET;
        target = NULL_SOURCE_TARGET;
    }


    // PENDING_DOC_REVIEW
    /**
     * Fires the specified event. The given event target specifies the path
     * through which the event will travel.
     *
     * @param eventTarget the target for the event
     * @param event the event to fire
     * @throws NullPointerException if eventTarget or event is null
     */
    public static void fireEvent(EventTarget eventTarget, Event event) {
        if (eventTarget == null) {
            throw new NullPointerException("Event target must not be null!");
        }

        if (event == null) {
            throw new NullPointerException("Event must not be null!");
        }

        EventUtil.fireEvent(eventTarget, event);
    }
}
