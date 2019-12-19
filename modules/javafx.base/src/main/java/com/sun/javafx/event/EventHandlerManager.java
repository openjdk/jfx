/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.event;

import java.util.HashMap;
import java.util.Map;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

/**
 * An {@code EventDispatcher} which allows user event handler / filter
 * registration and when used in an event dispatch chain it forwards received
 * events to the appropriate registered handlers / filters.
 */
public class EventHandlerManager extends BasicEventDispatcher {
    private final Map<EventType<? extends Event>,
                      CompositeEventHandler<? extends Event>> eventHandlerMap;

    private final Object eventSource;

    public EventHandlerManager(final Object eventSource) {
        this.eventSource = eventSource;

        eventHandlerMap =
                new HashMap<EventType<? extends Event>,
                            CompositeEventHandler<? extends Event>>();
    }

    /**
     * Registers an event handler in {@code EventHandlerManager}.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the type of the events to receive by the handler
     * @param eventHandler the handler to register
     * @throws NullPointerException if the event type or handler is null
     */
    public final <T extends Event> void addEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        validateEventType(eventType);
        validateEventHandler(eventHandler);

        final CompositeEventHandler<T> compositeEventHandler =
                createGetCompositeEventHandler(eventType);

        compositeEventHandler.addEventHandler(eventHandler);
    }

    /**
     * Unregisters a previously registered event handler.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type from which to unregister
     * @param eventHandler the handler to unregister
     * @throws NullPointerException if the event type or handler is null
     */
    public final <T extends Event> void removeEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        validateEventType(eventType);
        validateEventHandler(eventHandler);

        final CompositeEventHandler<T> compositeEventHandler =
                (CompositeEventHandler<T>) eventHandlerMap.get(eventType);

        if (compositeEventHandler != null) {
            compositeEventHandler.removeEventHandler(eventHandler);
        }
    }

    /**
     * Registers an event filter in {@code EventHandlerManager}.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the type of the events to receive by the filter
     * @param eventFilter the filter to register
     * @throws NullPointerException if the event type or filter is null
     */
    public final <T extends Event> void addEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        validateEventType(eventType);
        validateEventFilter(eventFilter);

        final CompositeEventHandler<T> compositeEventHandler =
                createGetCompositeEventHandler(eventType);

        compositeEventHandler.addEventFilter(eventFilter);
    }

    /**
     * Unregisters a previously registered event filter.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the event type from which to unregister
     * @param eventFilter the filter to unregister
     * @throws NullPointerException if the event type or filter is null
     */
    public final <T extends Event> void removeEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        validateEventType(eventType);
        validateEventFilter(eventFilter);

        final CompositeEventHandler<T> compositeEventHandler =
                (CompositeEventHandler<T>) eventHandlerMap.get(eventType);

        if (compositeEventHandler != null) {
            compositeEventHandler.removeEventFilter(eventFilter);
        }
    }

    /**
     * Sets the specified singleton handler. There can only be one such handler
     * specified at a time.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type to associate with the given eventHandler
     * @param eventHandler the handler to register, or null to unregister
     * @throws NullPointerException if the event type is null
     */
    public final <T extends Event> void setEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        validateEventType(eventType);

        CompositeEventHandler<T> compositeEventHandler =
                (CompositeEventHandler<T>) eventHandlerMap.get(eventType);

        if (compositeEventHandler == null) {
            if (eventHandler == null) {
                return;
            }
            compositeEventHandler = new CompositeEventHandler<T>();
            eventHandlerMap.put(eventType, compositeEventHandler);
        }

        compositeEventHandler.setEventHandler(eventHandler);
    }

    public final <T extends Event> EventHandler<? super T> getEventHandler(
            final EventType<T> eventType) {
        final CompositeEventHandler<T> compositeEventHandler =
                (CompositeEventHandler<T>) eventHandlerMap.get(eventType);

        return (compositeEventHandler != null)
                       ? compositeEventHandler.getEventHandler()
                       : null;
    }

    @Override
    public final Event dispatchCapturingEvent(Event event) {
        EventType<? extends Event> eventType = event.getEventType();
        do {
            event = dispatchCapturingEvent(eventType, event);
            eventType = eventType.getSuperType();
        } while (eventType != null);

        return event;
    }

    @Override
    public final Event dispatchBubblingEvent(Event event) {
        EventType<? extends Event> eventType = event.getEventType();
        do {
            event = dispatchBubblingEvent(eventType, event);
            eventType = eventType.getSuperType();
        } while (eventType != null);

        return event;
    }

    private <T extends Event> CompositeEventHandler<T>
            createGetCompositeEventHandler(final EventType<T> eventType) {
        CompositeEventHandler<T> compositeEventHandler =
                (CompositeEventHandler<T>) eventHandlerMap.get(eventType);
        if (compositeEventHandler == null) {
            compositeEventHandler = new CompositeEventHandler<T>();
            eventHandlerMap.put(eventType, compositeEventHandler);
        }

        return compositeEventHandler;
    }

    protected Object getEventSource() {
        return eventSource;
    }

    private Event dispatchCapturingEvent(
            final EventType<? extends Event> handlerType, Event event) {
        final CompositeEventHandler<? extends Event> compositeEventHandler =
                eventHandlerMap.get(handlerType);

        if (compositeEventHandler != null && compositeEventHandler.hasFilter()) {
            event = fixEventSource(event, eventSource);
            compositeEventHandler.dispatchCapturingEvent(event);
        }

        return event;
    }

    private Event dispatchBubblingEvent(
            final EventType<? extends Event> handlerType, Event event) {
        final CompositeEventHandler<? extends Event> compositeEventHandler =
                eventHandlerMap.get(handlerType);

        if (compositeEventHandler != null && compositeEventHandler.hasHandler()) {
            event = fixEventSource(event, eventSource);
            compositeEventHandler.dispatchBubblingEvent(event);
        }

        return event;
    }

    private static Event fixEventSource(final Event event,
                                        final Object eventSource) {
        return (event.getSource() != eventSource)
                ? event.copyFor(eventSource, event.getTarget())
                : event;
    }

    private static void validateEventType(final EventType<?> eventType) {
        if (eventType == null) {
            throw new NullPointerException("Event type must not be null");
        }
    }

    private static void validateEventHandler(
            final EventHandler<?> eventHandler) {
        if (eventHandler == null) {
            throw new NullPointerException("Event handler must not be null");
        }
    }

    private static void validateEventFilter(
            final EventHandler<?> eventFilter) {
        if (eventFilter == null) {
            throw new NullPointerException("Event filter must not be null");
        }
    }
}
