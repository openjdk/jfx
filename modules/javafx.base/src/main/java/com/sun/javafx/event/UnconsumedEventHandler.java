/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import java.util.Objects;

/**
 * Captures an {@code EventHandler} that will handle an unconsumed event, as well as the
 * event instance as it existed at the time the handler was captured.
 *
 * @param originalEvent the original event
 * @param handler the event handler
 */
public record UnconsumedEventHandler(Event originalEvent, EventHandler<Event> handler) {

    public UnconsumedEventHandler {
        Objects.requireNonNull(originalEvent, "originalEvent cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
    }

    /**
     * Invokes the handler with the original event.
     *
     * @return {@code true} if the event was consumed, {@code false} otherwise
     */
    public boolean handle() {
        EventUtil.markDeliveryCompleted(originalEvent);
        handler.handle(originalEvent);
        return originalEvent.isConsumed();
    }
}
