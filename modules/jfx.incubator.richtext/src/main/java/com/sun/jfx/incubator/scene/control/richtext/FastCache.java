/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * A simple cache implementation which provides a cheap invalidation via {@link #clear()}
 * and a cheap random eviction via {@link #evict()}.
 * This object must be accessed from the FX application thread, although it does not check.
 */
public class FastCache<T> {
    private static record Entry<V>(int index, V cell) { }

    private int size;
    private final Entry<T>[] linear;
    private final HashMap<Integer, T> data;
    private final static Random random = new Random();

    public FastCache(int capacity) {
        linear = new Entry[capacity];
        data = new HashMap<>(capacity);
    }

    public T get(int row) {
        return data.get(row);
    }

    /**
     * Adds a new cell to the cache. When the cache is full, this method evicts a
     * random cell from the cache first. NOTE: this method does not check whether
     * another cell for the given row is present, so this call must be preceded by a
     * {@link #get(int)}.
     */
    public void add(int index, T cell) {
        int ix;
        if (size >= capacity()) {
            ix = evict();
        } else {
            ix = size++;
        }

        data.put(index, cell);
        linear[ix] = new Entry<>(index, cell);
    }

    /** returns an index in the linear array of the cell that has been evicted */
    protected int evict() {
        int ix = random.nextInt(size);
        // does not clear the slot because it will get overwritten by the caller
        Entry<T> en = linear[ix];
        int index = en.index();
        data.remove(index);
        return ix;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return linear.length;
    }

    public void clear() {
        size = 0;
        Arrays.fill(linear, null);
        data.clear();
    }
}
