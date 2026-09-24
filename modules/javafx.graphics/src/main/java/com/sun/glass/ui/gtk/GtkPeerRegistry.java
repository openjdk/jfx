/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.gtk;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * The {@code long} id to peer registry of {@link GtkWindow} and {@link GtkView}: where the window, view and
 * drag-and-drop slots of {@code glass_gtk_api.h} look up the peer their id names, once or twice per input event. At
 * commit {@code 033187ad90} the C held those peers as JNI references ({@code jwindow}, {@code jview} of
 * {@code glass_window.cpp}) and called them without any lookup.
 * <p>
 * Keyed by the primitive id, so that a lookup allocates nothing for any id; a {@code Map<Long, ...>} boxes every id
 * above 127, and ids are never reused. The table is an immutable open-addressing snapshot in a volatile field:
 * {@link #get} reads it without a lock, and {@link #put}, {@link #putWeak} and {@link #remove} build and publish a
 * new one under this object's lock. Every operation is therefore atomic and a {@code get} sees the last completed
 * write, as with the {@code ConcurrentHashMap} this replaces. Writes are rare - a window or view created, handed to a
 * window, closed or destroyed - and copy the live entries.
 * <p>
 * An entry holds its peer strongly ({@link #put}), or through a {@link WeakReference} ({@link #putWeak}) for a peer
 * that no native object refers to: {@link #get} then answers it only while something else keeps it alive, and the
 * next write drops the entry once the reference has been cleared.
 *
 * @param <T> the peer type
 */
final class GtkPeerRegistry<T> {

    private static final int MIN_CAPACITY = 8;

    /**
     * An immutable table: {@code keys[i] == 0} is a free slot; a value is the peer or a {@link WeakReference} to it.
     * At most half of the slots are used, so a probe always ends at a free one.
     */
    private static final class Table {
        final long[] keys;
        final Object[] values;

        Table(int capacity) {
            keys = new long[capacity];
            values = new Object[capacity];
        }
    }

    private volatile Table table = new Table(MIN_CAPACITY);

    /**
     * The peer registered under {@code id}, {@code null} if there is none, it was removed, or it was held weakly
     * and has been collected. Lock-free, allocation-free.
     */
    T get(long id) {
        if (id == 0) {
            return null;
        }
        Table t = table;
        long[] keys = t.keys;
        int mask = keys.length - 1;
        for (int i = slot(id, mask); ; i = (i + 1) & mask) {
            long key = keys[i];
            if (key == id) {
                return peer(t.values[i]);
            }
            if (key == 0) {
                return null;
            }
        }
    }

    /** Whether {@link #get} answers a peer for {@code id}. */
    boolean containsKey(long id) {
        return get(id) != null;
    }

    /** How many ids {@link #get} answers a peer for. */
    int size() {
        Table t = table;
        int n = 0;
        for (int i = 0; i < t.keys.length; i++) {
            if (t.keys[i] != 0 && peer(t.values[i]) != null) {
                n++;
            }
        }
        return n;
    }

    /** Registers {@code peer} under {@code id}, held strongly; replaces what was registered under it. */
    synchronized void put(long id, T peer) {
        write(id, Objects.requireNonNull(peer));
    }

    /** Registers {@code peer} under {@code id}, held weakly; replaces what was registered under it. */
    synchronized void putWeak(long id, T peer) {
        write(id, new WeakReference<>(Objects.requireNonNull(peer)));
    }

    /**
     * Removes the entry of {@code id}; answers the peer it named, if that was still alive. The id 0, which no entry
     * has, removes nothing.
     */
    synchronized T remove(long id) {
        if (id == 0) {
            return null;
        }
        T previous = get(id);
        write(id, null);
        return previous;
    }

    /** Publishes a new table: the live entries of the current one except {@code id}, then {@code id} if non-null. */
    private void write(long id, Object value) {
        if (id == 0) {
            throw new IllegalArgumentException("the id 0 names no peer");
        }
        Table current = table;
        int live = value == null ? 0 : 1;
        for (int i = 0; i < current.keys.length; i++) {
            if (keep(current, i, id)) {
                live++;
            }
        }
        int capacity = MIN_CAPACITY;
        while (capacity < 2 * live + 1) {
            capacity <<= 1;
        }
        Table next = new Table(capacity);
        for (int i = 0; i < current.keys.length; i++) {
            if (keep(current, i, id)) {
                insert(next, current.keys[i], current.values[i]);
            }
        }
        if (value != null) {
            insert(next, id, value);
        }
        table = next;
    }

    /** Whether slot {@code i} of {@code t} holds an entry other than {@code id} whose peer is still alive. */
    private static boolean keep(Table t, int i, long id) {
        return t.keys[i] != 0 && t.keys[i] != id && peer(t.values[i]) != null;
    }

    private static void insert(Table t, long id, Object value) {
        int mask = t.keys.length - 1;
        int i = slot(id, mask);
        while (t.keys[i] != 0) {
            i = (i + 1) & mask;
        }
        t.keys[i] = id;
        t.values[i] = value;
    }

    /** The first slot to probe for {@code id}: Fibonacci hashing, so that consecutive ids spread. */
    private static int slot(long id, int mask) {
        long h = id * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32)) & mask;
    }

    @SuppressWarnings("unchecked")
    private static <P> P peer(Object value) {
        return (P) (value instanceof WeakReference<?> ref ? ref.get() : value);
    }
}
