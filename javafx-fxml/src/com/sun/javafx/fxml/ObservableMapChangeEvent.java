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

import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * Observable map change event.
 */
public class ObservableMapChangeEvent<K, V> extends Event {
    private static final long serialVersionUID = 0;

    private EventType<ObservableMapChangeEvent<?, ?>> type;
    private K key;
    private V removed;

    public static final EventType<ObservableMapChangeEvent<?, ?>> ADD =
        new EventType<ObservableMapChangeEvent<?, ?>>(EventType.ROOT, ObservableMapChangeEvent.class.getName() + "_ADD");
    public static final EventType<ObservableMapChangeEvent<?, ?>> UPDATE =
        new EventType<ObservableMapChangeEvent<?, ?>>(EventType.ROOT, ObservableMapChangeEvent.class.getName() + "_UPDATE");
    public static final EventType<ObservableMapChangeEvent<?, ?>> REMOVE =
        new EventType<ObservableMapChangeEvent<?, ?>>(EventType.ROOT, ObservableMapChangeEvent.class.getName() + "_REMOVE");

    public ObservableMapChangeEvent(ObservableMap<K, V> source, EventType<ObservableMapChangeEvent<?, ?>> type,
        K key, V removed) {
        super(source, null, type);

        this.type = type;
        this.key = key;
        this.removed = removed;
    }

    /**
     * The key associated with the change.
     */
    public K getKey() {
        return key;
    }

    /**
     * They value that was removed as part of the change, or <tt>null</tt> if
     * this change did not cause a value to be removed.
     */
    public V getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + type + ": " + key + " "
            + ((removed == null) ? "" : removed);
    }
}
