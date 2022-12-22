/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import javafx.beans.NamedArg;

/**
 * Used in event handler registration in place of its associated event handler.
 * Its sole purpose is to break the otherwise strong reference between an event
 * handler container and its associated event handler. While the container still
 * holds strong reference to the registered {@code WeakEventHandler} proxy, the
 * proxy itself references the original handler only weakly and so doesn't
 * prevent it from being garbage collected. Until this weak reference is broken,
 * any event notification received by the proxy is forwarded to the original
 * handler.
 *
 * @param <T> the event class this handler can handle
 * @since JavaFX 8.0
 */
public final class WeakEventHandler<T extends Event>
        implements EventHandler<T> {
    private final WeakReference<EventHandler<T>> weakRef;

    /**
     * Creates a new instance of {@code WeakEventHandler}.
     *
     * @param eventHandler the original event handler to which to forward event
     *      notifications
     */
    public WeakEventHandler(final @NamedArg("eventHandler") EventHandler<T> eventHandler) {
        weakRef = new WeakReference<>(eventHandler);
    }

    /**
     * Indicates whether the associated event handler has been garbage
     * collected. Used by containers to detect when the storage of corresponding
     * references to this {@code WeakEventHandler} is no longer necessary.
     *
     * @return {@code true} if the associated handler has been garbage
     *      collected, {@code false} otherwise
     */
    public boolean wasGarbageCollected() {
        return weakRef.get() == null;
    }

    /**
     * Forwards event notification to the associated event handler.
     *
     * @param event the event which occurred
     */
    @Override
    public void handle(final T event) {
        final EventHandler<T> eventHandler = weakRef.get();
        if (eventHandler != null) {
            eventHandler.handle(event);
        }
    }

    /* Used for testing. */
    void clear() {
        weakRef.clear();
    }
}
