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

package com.sun.javafx.fxml;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * Observable list change event.
 */
public class ObservableListChangeEvent<E> extends Event {
    private static final long serialVersionUID = 0;

    private EventType<ObservableListChangeEvent<?>> type;
    private int from;
    private int to;
    private List<E> removed;

    public static final EventType<ObservableListChangeEvent<?>> ADD =
        new EventType<ObservableListChangeEvent<?>>(EventType.ROOT, ObservableListChangeEvent.class.getName() + "_ADD");
    public static final EventType<ObservableListChangeEvent<?>> UPDATE =
        new EventType<ObservableListChangeEvent<?>>(EventType.ROOT, ObservableListChangeEvent.class.getName() + "_UPDATE");
    public static final EventType<ObservableListChangeEvent<?>> REMOVE =
        new EventType<ObservableListChangeEvent<?>>(EventType.ROOT, ObservableListChangeEvent.class.getName() + "_REMOVE");

    public ObservableListChangeEvent(ObservableList<E> source, EventType<ObservableListChangeEvent<?>> type,
        int from, int to, List<E> removed) {
        super(source, null, type);

        this.from = from;
        this.to = to;
        this.removed = removed;
    }

    /**
     * Returns the first index affected by the change.
     */
    public int getFrom() {
        return from;
    }

    /**
     * Returns the index immediately following the last index affected by the
     * change.
     */
    public int getTo() {
        return to;
    }

    /**
     * Returns any elements that were removed as part of the change, or
     * <tt>null</tt> if either this change did not cause any values to be
     * removed or the removed values could not be determined.
     */
    public List<E> getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + type + ": [" + from + ".." + to + ") "
            + ((removed == null) ? "" : removed);
    }
}
