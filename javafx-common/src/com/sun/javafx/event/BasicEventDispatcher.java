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

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;

/**
 * Event dispatcher which introduces event dispatch phase specific methods -
 * {@code dispatchCapturingEvent} and {@code dispatchBubblingEvent}. These
 * are used in the {@code BasicEventDispatcher.dispatchEvent} implementation,
 * but because they are public they can be called directly as well. Their
 * default implementation does nothing and is expected to be overridden in
 * subclasses. The {@code BasicEventDispatcher} also adds possibility to chain
 * event dispatchers. This is used together with the direct access to the phase
 * specific dispatch methods to implement {@code CompositeEventDispatcher}.
 * <p>
 * An event dispatcher derived from {@code BasicEventDispatcher} can act as
 * a standalone event dispatcher or can be used to form a dispatch chain in
 * {@code CompositeEventDispatcher}.
 */
public abstract class BasicEventDispatcher implements EventDispatcher {
    private BasicEventDispatcher previousDispatcher;
    private BasicEventDispatcher nextDispatcher;

    @Override
    public Event dispatchEvent(Event event, final EventDispatchChain tail) {
        event = dispatchCapturingEvent(event);
        if (event.isConsumed()) {
            return null;
        }
        event = tail.dispatchEvent(event);
        if (event != null) {
            event = dispatchBubblingEvent(event);
            if (event.isConsumed()) {
                return null;
            }
        }

        return event;
    }

    public Event dispatchCapturingEvent(Event event) {
        return event;
    }

    public Event dispatchBubblingEvent(Event event) {
        return event;
    }

    public final BasicEventDispatcher getPreviousDispatcher() {
        return previousDispatcher;
    }

    public final BasicEventDispatcher getNextDispatcher() {
        return nextDispatcher;
    }

    public final void insertNextDispatcher(
            final BasicEventDispatcher newDispatcher) {
        if (nextDispatcher != null) {
            nextDispatcher.previousDispatcher = newDispatcher;
        }
        newDispatcher.nextDispatcher = nextDispatcher;
        newDispatcher.previousDispatcher = this;
        nextDispatcher = newDispatcher;
    }
}
