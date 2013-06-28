/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventType;

/**
 * This event dispatcher redirects received events to the registered child
 * dispatchers before dispatching them to the rest of the dispatch chain. The
 * redirected events are wrapped in {@code RedirectedEvent} instances, so they
 * can be easily recognized from normal direct events. If an original event
 * wrapped in the {@code RedirectedEvent} is consumed by any of the child
 * dispatchers, it won't be sent by the {@code EventRedirector} to the rest of
 * the original dispatch chain.
 * <p>
 * The child dispatchers can also be instances of {@code EventRedirector} and
 * might receive both, the normal events (from other sources) and the redirected
 * events from the parent {@code EventRedirector}. If a {@code RedirectedEvent}
 * is received, it is forwarded to the child event dispatchers without any
 * additional wrapping.
 * <p>
 * For this hierarchical arrangement of {@code EventRedirector} instances the
 * class defines the {@code handleRedirectedEvent} method, which is called with
 * a received redirected event, after the event has been forwarded to the child
 * dispatchers. By default this method is empty, but can be overridden in
 * derived classes to define specific handling of these redirected events.
 */
public class EventRedirector extends BasicEventDispatcher {
    private final EventDispatchChainImpl eventDispatchChain;

    private final List<EventDispatcher> eventDispatchers;
    private final Object eventSource;

    /**
     * Constructs a new {@code EventRedirector}.
     *
     * @param eventSource the object for which to redirect the events
     *      ({@code RedirectedEvent} event source)
     */
    public EventRedirector(final Object eventSource) {
        this.eventDispatchers = new CopyOnWriteArrayList<EventDispatcher>();
        this.eventDispatchChain = new EventDispatchChainImpl();
        this.eventSource = eventSource;
    }

    /**
     * Called when a redirected event is received by this instance.
     *
     * @param eventSource the object from which the event has been redirected
     * @param event the event which has been redirected
     */
    protected void handleRedirectedEvent(
            final Object eventSource,
            final Event event) {
    }

    public final void addEventDispatcher(
            final EventDispatcher eventDispatcher) {
        eventDispatchers.add(eventDispatcher);
    }

    public final void removeEventDispatcher(
            final EventDispatcher eventDispatcher) {
        eventDispatchers.remove(eventDispatcher);
    }

    @Override
    public final Event dispatchCapturingEvent(Event event) {
        final EventType<?> eventType = event.getEventType();

        if (eventType == DirectEvent.DIRECT) {
            // unwrap event, but don't redirect
            event = ((DirectEvent) event).getOriginalEvent();
        } else {
            redirectEvent(event);

            if (eventType == RedirectedEvent.REDIRECTED) {
                handleRedirectedEvent(
                        event.getSource(),
                        ((RedirectedEvent) event).getOriginalEvent());
            }
        }

        return event;
    }

    private void redirectEvent(final Event event) {
        if (!eventDispatchers.isEmpty()) {
            final RedirectedEvent redirectedEvent =
                    (event.getEventType() == RedirectedEvent.REDIRECTED)
                            ? (RedirectedEvent) event
                            : new RedirectedEvent(event, eventSource, null);

            for (final EventDispatcher eventDispatcher: eventDispatchers) {
                eventDispatchChain.reset();
                eventDispatcher.dispatchEvent(
                        redirectedEvent, eventDispatchChain);
            }
        }
    }
}
