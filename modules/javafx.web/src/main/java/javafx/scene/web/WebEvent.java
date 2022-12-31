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

package javafx.scene.web;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * {@code WebEvent} instances are passed into {@code EventHandler}s registered
 * with a {@link WebEngine} by JavaScript running on a Web page. An event holds
 * a single data item of type {@code T}.
 *
 * @see WebEngine
 * @see WebEngine#setOnAlert
 * @see WebEngine#setOnResized
 * @see WebEngine#setOnStatusChanged
 * @see WebEngine#setOnVisibilityChanged
 * @since JavaFX 2.0
 */
final public class WebEvent<T> extends Event {

    /**
     * Common supertype for all Web event types.
     */
    public static final EventType<WebEvent> ANY =
            new EventType<>(Event.ANY, "WEB");

    /**
     * This event occurs when a script changes location of the JavaScript
     * {@code window} object.
     */
    public static final EventType<WebEvent> RESIZED =
            new EventType<>(WebEvent.ANY, "WEB_RESIZED");

    /**
     * This event occurs when a script changes status line text.
     */
    public static final EventType<WebEvent> STATUS_CHANGED =
            new EventType<>(WebEvent.ANY, "WEB_STATUS_CHANGED");

    /**
     * This event occurs when a script changes visibility of the JavaScript
     * {@code window} object.
     */
    public static final EventType<WebEvent> VISIBILITY_CHANGED =
            new EventType<>(WebEvent.ANY, "WEB_VISIBILITY_CHANGED");

    /**
     * This event occurs when a script calls the JavaScript {@code alert}
     * function.
     */
    public static final EventType<WebEvent> ALERT =
            new EventType<>(WebEvent.ANY, "WEB_ALERT");

    private final T data;

    /**
     * Creates a new event object.
     *
     * @param source the event source
     * @param type the event type
     * @param data the data item
     */
    public WebEvent(@NamedArg("source") Object source, @NamedArg("type") EventType<WebEvent> type, @NamedArg("data") T data) {
        super(source, null, type);
        this.data = data;
    }

    /**
     * Returns data item carried by this event.
     * @return the data item
     */
    public T getData() {
        return data;
    }

    /**
     * Returns a string representation of this {@code WebEvent} object.
     * @return a string representation of this {@code WebEvent} object.
     */
    @Override public String toString() {
        return String.format(
                "WebEvent [source = %s, eventType = %s, data = %s]",
                getSource(), getEventType(), getData());
    }
}
