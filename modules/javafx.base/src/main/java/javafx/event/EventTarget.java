/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

// PENDING_DOC_REVIEW
/**
 * Represents an event target.
 * @since JavaFX 2.0
 */
public interface EventTarget {
    /**
     * Construct an event dispatch chain for this target. The event dispatch
     * chain contains event dispatchers which might be interested in processing
     * of events targeted at this {@code EventTarget}. This event target is
     * not automatically added to the chain, so if it wants to process events,
     * it needs to add an {@code EventDispatcher} for itself to the chain.
     * <p>
     * In the case the event target is part of some hierarchy, the chain for it
     * is usually built from event dispatchers collected from the root of the
     * hierarchy to the event target.
     * <p>
     * The event dispatch chain is constructed by modifications to the provided
     * initial event dispatch chain. The returned chain should have the initial
     * chain at its end so the dispatchers should be prepended to the initial
     * chain.
     * <p>
     * The caller shouldn't assume that the initial chain remains unchanged nor
     * that the returned value will reference a different chain.
     *
     * @param tail the initial chain to build from
     * @return the resulting event dispatch chain for this target
     */
    EventDispatchChain buildEventDispatchChain(EventDispatchChain tail);

    /**
     * Registers an event handler for this target.
     * <p>
     * The handler is called when the target receives an {@link Event} of the specified
     * type during the bubbling phase of event delivery.
     *
     * @param <E> the event class of the handler
     * @param eventType the type of the events received by the handler
     * @param eventHandler the event handler
     * @throws NullPointerException if {@code eventType} or {@code eventHandler} is {@code null}
     * @throws UnsupportedOperationException if this target does not support event handlers
     * @implSpec The default implementation of this method throws {@code UnsupportedOperationException}.
     * @since 21
     */
    default <E extends Event> void addEventHandler(EventType<E> eventType, EventHandler<? super E> eventHandler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unregisters a previously registered event handler from this target.
     * <p>
     * Since it is possible to register a single {@link EventHandler} instance for different event types,
     * the caller needs to specify the event type from which the handler should be unregistered.
     *
     * @param <E> the event class of the handler
     * @param eventType the event type from which to unregister
     * @param eventHandler the event handler
     * @throws NullPointerException if {@code eventType} or {@code eventHandler} is {@code null}
     * @throws UnsupportedOperationException if this target does not support event handlers
     * @implSpec The default implementation of this method throws {@code UnsupportedOperationException}.
     * @since 21
     */
    default <E extends Event> void removeEventHandler(EventType<E> eventType, EventHandler<? super E> eventHandler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers an event filter for this target.
     * <p>
     * The filter is called when the target receives an {@link Event} of the specified
     * type during the capturing phase of event delivery.
     *
     * @param <E> the event class of the filter
     * @param eventType the type of the events received by the filter
     * @param eventFilter the event filter
     * @throws NullPointerException if {@code eventType} or {@code eventFilter} is {@code null}
     * @throws UnsupportedOperationException if this target does not support event filters
     * @implSpec The default implementation of this method throws {@code UnsupportedOperationException}.
     * @since 21
     */
    default <E extends Event> void addEventFilter(EventType<E> eventType, EventHandler<? super E> eventFilter) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unregisters a previously registered event filter from this target.
     * <p>
     * Since it is possible to register a single {@link EventHandler} instance for different event types,
     * the caller needs to specify the event type from which the filter should be unregistered.
     *
     * @param <E> the event class of the filter
     * @param eventType the event type from which to unregister
     * @param eventFilter the event filter
     * @throws NullPointerException if {@code eventType} or {@code eventFilter} is {@code null}
     * @throws UnsupportedOperationException if this target does not support event filters
     * @implSpec The default implementation of this method throws {@code UnsupportedOperationException}.
     * @since 21
     */
    default <E extends Event> void removeEventFilter(EventType<E> eventType, EventHandler<? super E> eventFilter) {
        throw new UnsupportedOperationException();
    }
}
