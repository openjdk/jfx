/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

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
 */
final public class WebEvent<T> extends Event {

    /**
     * Common supertype for all Web event types.
     */
    public static final EventType<WebEvent> ANY =
            new EventType<WebEvent>(Event.ANY, "WEB");

    /**
     * This event occurs when a script changes location of the JavaScript
     * {@code window} object.
     */
    public static final EventType<WebEvent> RESIZED =
            new EventType<WebEvent>(WebEvent.ANY, "WEB_RESIZED");

    /**
     * This event occurs when a script changes status line text.
     */
    public static final EventType<WebEvent> STATUS_CHANGED =
            new EventType<WebEvent>(WebEvent.ANY, "WEB_STATUS_CHANGED");

    /**
     * This event occurs when a script changes visibility of the JavaScript
     * {@code window} object.
     */
    public static final EventType<WebEvent> VISIBILITY_CHANGED =
            new EventType<WebEvent>(WebEvent.ANY, "WEB_VISIBILITY_CHANGED");

    /**
     * This event occurs when a script calls the JavaScript {@code alert}
     * function.
     */
    public static final EventType<WebEvent> ALERT =
            new EventType<WebEvent>(WebEvent.ANY, "WEB_ALERT");

    private final T data;

    /**
     * Creates a new event object.
     */
    public WebEvent(Object source, EventType<WebEvent> type, T data) {
        super(source, null, type);
        this.data = data;
    }

    /**
     * Returns data item carried by this event.
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
