/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * An event indicating a {@link WebEngine} error.
 * Holds an optional text message and an optional exception
 * associated with the error.
 *
 * @see WebEngine#onErrorProperty WebEngine.onError
 * @since JavaFX 8.0
 */
public final class WebErrorEvent extends Event {

    /**
     * Common supertype for all {@code WebErrorEvent} types.
     */
    public static final EventType<WebErrorEvent> ANY =
            new EventType<WebErrorEvent>(Event.ANY, "WEB_ERROR");

    /**
     * This event occurs when a {@link WebEngine} detects that its
     * user data directory is already in use by a {@code WebEngine}
     * running in a different VM.
     *
     * <p>In general, multiple {@code WebEngine} instances may share
     * a single user data directory as long as they run in the same
     * VM. {@code WebEngine} instances running in different VMs are
     * not allowed to share the same user data directory.
     *
     * <p>When a {@code WebEngine} is about to start loading a web
     * page or executing a script for the first time, it checks whether
     * its {@link WebEngine#userDataDirectoryProperty userDataDirectory}
     * is already in use by a {@code WebEngine} running in a different
     * VM. If the latter is the case, the {@code WebEngine} invokes the
     * {@link WebEngine#onErrorProperty WebEngine.onError} event handler,
     * if any, with a {@code USER_DATA_DIRECTORY_ALREADY_IN_USE} event.
     * If the invoked event handler modifies the {@code userDataDirectory}
     * property, the {@code WebEngine} retries with the new user
     * data directory as soon as the handler returns. If the handler
     * does not modify the {@code userDataDirectory} property (which
     * is the default), the {@code WebEngine} continues without the
     * user data directory.
     */
    public static final EventType<WebErrorEvent>
            USER_DATA_DIRECTORY_ALREADY_IN_USE = new EventType<WebErrorEvent>(
                    WebErrorEvent.ANY, "USER_DATA_DIRECTORY_ALREADY_IN_USE");

    /**
     * This event occurs when a {@link WebEngine} encounters
     * an I/O error while trying to create or access the user
     * data directory.
     *
     * <p>When a {@code WebEngine} is about to start loading a web
     * page or executing a script for the first time, it checks whether
     * it can create or access its
     * {@link WebEngine#userDataDirectoryProperty userDataDirectory}.
     * If the check fails with an I/O error (such as {@code
     * java.io.IOException}), the {@code WebEngine} invokes the {@link
     * WebEngine#onErrorProperty WebEngine.onError} event handler,
     * if any, with a {@code USER_DATA_DIRECTORY_IO_ERROR} event.
     * If the invoked event handler modifies the {@code userDataDirectory}
     * property, the {@code WebEngine} retries with the new user
     * data directory as soon as the handler returns. If the handler
     * does not modify the {@code userDataDirectory} property (which
     * is the default), the {@code WebEngine} continues without the
     * user data directory.
     */
    public static final EventType<WebErrorEvent>
            USER_DATA_DIRECTORY_IO_ERROR = new EventType<WebErrorEvent>(
                    WebErrorEvent.ANY, "USER_DATA_DIRECTORY_IO_ERROR");

    /**
     * This event occurs when a {@link WebEngine} encounters
     * a security error while trying to create or access the user
     * data directory.
     *
     * <p>When a {@code WebEngine} is about to start loading a web
     * page or executing a script for the first time, it checks whether
     * it can create or access its
     * {@link WebEngine#userDataDirectoryProperty userDataDirectory}.
     * If the check fails with a security error (such as {@code
     * java.lang.SecurityException}), the {@code WebEngine} invokes the
     * {@link WebEngine#onErrorProperty WebEngine.onError} event handler,
     * if any, with a {@code USER_DATA_DIRECTORY_SECURITY_ERROR} event.
     * If the invoked event handler modifies the {@code userDataDirectory}
     * property, the {@code WebEngine} retries with the new user
     * data directory as soon as the handler returns. If the handler
     * does not modify the {@code userDataDirectory} property (which
     * is the default), the {@code WebEngine} continues without the
     * user data directory.
     */
    public static final EventType<WebErrorEvent>
            USER_DATA_DIRECTORY_SECURITY_ERROR = new EventType<WebErrorEvent>(
                    WebErrorEvent.ANY, "USER_DATA_DIRECTORY_SECURITY_ERROR");


    private final String message;
    private final Throwable exception;


    /**
     * Creates a new {@code WebErrorEvent}.
     * @param source the event source which sent the event
     * @param type the event type
     * @param message the text message associated with the event;
     *        may be {@code null}
     * @param exception the exception associated with the event;
     *        may be {@code null}
     */
    public WebErrorEvent(@NamedArg("source") Object source, @NamedArg("type") EventType<WebErrorEvent> type,
                         @NamedArg("message") String message, @NamedArg("exception") Throwable exception)
    {
        super(source, null, type);
        this.message = message;
        this.exception = exception;
    }


    /**
     * Returns the text message associated with this event.
     * @return the text message associated with this event, or
     *         {@code null} if there is no such message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the exception associated with this event.
     * @return the exception associated with this event, or
     *         {@code null} if there is no such exception
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        return String.format("WebErrorEvent [source = %s, eventType = %s, "
                + "message = \"%s\", exception = %s]",
                getSource(), getEventType(), getMessage(), getException());
    }
}
