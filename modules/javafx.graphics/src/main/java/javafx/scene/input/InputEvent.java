/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * An event indicating a user input.
 * @since JavaFX 2.0
 */
public class InputEvent extends Event {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all input event types.
     */
    public static final EventType<InputEvent> ANY =
            new EventType<> (Event.ANY, "INPUT");

    /**
     * Creates new instance of InputEvent.
     * @param eventType Type of the event
     */
    public InputEvent(final @NamedArg("eventType") EventType<? extends InputEvent> eventType) {
        super(eventType);
    }

    /**
     * Creates new instance of InputEvent.
     * @param source Event source
     * @param target Event target
     * @param eventType Type of the event
     */
    public InputEvent(final @NamedArg("source") Object source,
                      final @NamedArg("target") EventTarget target,
                      final @NamedArg("eventType") EventType<? extends InputEvent> eventType) {
        super(source, target, eventType);
    }

    @Override
    public EventType<? extends InputEvent> getEventType() {
        return (EventType<? extends InputEvent>) super.getEventType();
    }

}
