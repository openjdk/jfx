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

package com.sun.javafx.event;

import java.util.LinkedList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;

public final class StubEventDispatchChain implements EventDispatchChain {
    private final LinkedList<EventDispatcher> eventDispatchers;

    public static final EventDispatchChain EMPTY_CHAIN =
            new StubEventDispatchChain();

    private StubEventDispatchChain() {
        this(new LinkedList<EventDispatcher>());
    }

    private StubEventDispatchChain(
            final LinkedList<EventDispatcher> eventDispatchers) {
        this.eventDispatchers = eventDispatchers;
    }

    @Override
    public EventDispatchChain append(final EventDispatcher eventDispatcher) {
        final LinkedList<EventDispatcher> newDispatchers =
                copyDispatchers(eventDispatchers);
        newDispatchers.addLast(eventDispatcher);
        return new StubEventDispatchChain(newDispatchers);
    }

    @Override
    public EventDispatchChain prepend(final EventDispatcher eventDispatcher) {
        final LinkedList<EventDispatcher> newDispatchers =
                copyDispatchers(eventDispatchers);
        newDispatchers.addFirst(eventDispatcher);
        return new StubEventDispatchChain(newDispatchers);
    }

    @Override
    public Event dispatchEvent(final Event event) {
        if (eventDispatchers.isEmpty()) {
            return event;
        }

        final LinkedList<EventDispatcher> tailDispatchers =
                copyDispatchers(eventDispatchers.subList(
                                    1, eventDispatchers.size()));
        return eventDispatchers.element().dispatchEvent(
                event, new StubEventDispatchChain(tailDispatchers));
    }

    private static LinkedList<EventDispatcher> copyDispatchers(
            final List<EventDispatcher> dispatchers) {
        return new LinkedList<EventDispatcher>(dispatchers);
    }
}
