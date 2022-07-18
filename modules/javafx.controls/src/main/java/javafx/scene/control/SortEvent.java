/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event related to {@link TableView} and {@link TreeTableView} sorting.
 * @param <C> the type of control
 * @since JavaFX 8.0
 */
public class SortEvent<C> extends Event {

    /**
     * Common supertype for all sort event types.
     */
    public static final EventType<SortEvent> ANY =
            new EventType<SortEvent> (Event.ANY, "SORT");

    /**
     * Gets the default singleton {@code SortEvent}.
     * @param <C> the type of control
     * @return the default singleton {@code SortEvent}
     */
    @SuppressWarnings("unchecked")
    public static <C> EventType<SortEvent<C>> sortEvent() {
        return (EventType<SortEvent<C>>) SORT_EVENT;
    }

    private static final EventType<?> SORT_EVENT = new EventType<>(SortEvent.ANY, "SORT_EVENT");

    /**
     * Constructs a new {@code SortEvent} with the specified event source and target.
     * If the source or target is set to {@code null}, it is replaced by
     * the {@code NULL_SOURCE_TARGET} value.
     *
     * @param source the event source which sent the event
     * @param target the target of the event
     */
    public SortEvent(@NamedArg("source") C source, @NamedArg("target") EventTarget target) {
        super(source, target, sortEvent());
    }

    @SuppressWarnings("unchecked")
    @Override public C getSource() {
        return (C) super.getSource();
    }
}
