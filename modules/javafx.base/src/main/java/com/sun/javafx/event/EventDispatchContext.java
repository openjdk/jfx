/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class EventDispatchContext implements AutoCloseable {

    private final static ThreadLocal<Deque<EventDispatchContext>> context =
            ThreadLocal.withInitial(ArrayDeque::new);

    private Event currentEvent;
    private Object defaultHandlers;
    private boolean preventDefault;

    public EventDispatchContext() {
        context.get().push(this);
    }

    public static EventDispatchContext getCurrent() {
        return context.get().peek();
    }

    public void setCurrentEvent(Event event) {
        currentEvent = event;
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> void addDefaultHandler(Event event, EventHandler<E> handler) {
        if (currentEvent == null) {
            throw new IllegalStateException("Event delivery is not in progress");
        }

        if (currentEvent != event) {
            throw new IllegalStateException("Default event handler can only be added for the currently dispatched event");
        }

        if (preventDefault) {
            return;
        }

        if (defaultHandlers == null) {
            defaultHandlers = new DefaultEventHandler(event, (EventHandler<Event>)handler);
        } else if (defaultHandlers instanceof List<?> list) {
            var typedList = (List<DefaultEventHandler>)list;
            typedList.add(new DefaultEventHandler(event, (EventHandler<Event>)handler));
        } else {
            var list = new ArrayList<DefaultEventHandler>(4);
            list.add((DefaultEventHandler)defaultHandlers);
            list.add(new DefaultEventHandler(event, (EventHandler<Event>)handler));
            defaultHandlers = list;
        }
    }

    public void preventDefault(Event event) {
        if (currentEvent == null) {
            throw new IllegalStateException("Event delivery is not in progress");
        }

        if (currentEvent != event) {
            throw new IllegalStateException("Default event handling can only be prevented for the currently dispatched event");
        }

        preventDefault = true;
        defaultHandlers = null;
    }

    @SuppressWarnings("unchecked")
    public Event dispatchEvent(EventDispatchChain eventDispatchChain, Event event) {
        Event resultEvent = eventDispatchChain.dispatchEvent(event);
        if (resultEvent == null) {
            return null;
        }

        if (defaultHandlers instanceof List<?> list) {
            for (DefaultEventHandler handler : (List<DefaultEventHandler>)list) {
                if (handler.handle()) {
                    return null;
                }
            }
        } else if (defaultHandlers != null && ((DefaultEventHandler)defaultHandlers).handle()) {
            return null;
        }

        return resultEvent;
    }

    @Override
    public void close() {
        context.get().pop();
    }

    private record DefaultEventHandler(Event originalEvent, EventHandler<Event> handler) {

        public DefaultEventHandler {
            Objects.requireNonNull(originalEvent, "originalEvent cannot be null");
            Objects.requireNonNull(handler, "handler cannot be null");
        }

        public boolean handle() {
            handler.handle(originalEvent);
            return originalEvent.isConsumed();
        }
    }
}
