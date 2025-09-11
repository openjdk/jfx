/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;

import static com.sun.marlin.MarlinConst.LOG_OFF_HEAP_MALLOC;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

final class OffHeapArray {

    private final boolean global;
    private Arena arena;

    // size of int / float
    static final int SIZE_INT = 4;

    // FFM stuff
    private static final int ALIGNMENT = 16;
    private static final ValueLayout.OfByte BYTE_LAYOUT = ValueLayout.JAVA_BYTE;
    private static final ValueLayout.OfInt INT_LAYOUT = ValueLayout.JAVA_INT;

    /* members */
    private MemorySegment segment;
    private int used;

    /**
     * Creates an OffHeapArray of the specified length using either the global
     * arena or a confined arena. If the global arena is specified, the array
     * may be accessed on any thread, but it must not be resized or freed.
     * If a confined arena is specified, all access to this array must be done
     * on the same thread that constructed it.
     *
     * @param parent the object that will be used to register a cleaner to
     * free the off-heap array when {@code parent} becomes phantom reachable;
     * this is unused if the global arena is specified
     * @param len the number of bytes to allocate
     * @param global if {@code true} use the global arena, otherwise use a
     * confined arena
     */
    OffHeapArray(final Object parent, final long len, boolean global) {
        this.global = global;

        // Allocate the specified type of arena
        this.arena = global ? Arena.global() : Arena.ofConfined();

        // note: may throw OOME:
        this.segment = arena.allocate(len, ALIGNMENT);
        this.used    = 0;
        if (LOG_OFF_HEAP_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.allocateMemory =   "
                                + len + " for segment = " + this.segment
                                + " global = " + global);
        }

        if (!global) {
            // Register a cleaning function to ensure freeing off-heap memory:
            MarlinUtils.getCleaner().register(parent, this::free);
        }
    }

    /**
     * Gets the length of this array.
     * @return the length in bytes
     */
    long getLength() {
        return segment.byteSize();
    }

    /**
     * Gets the number of bytes currently being used. Always <= length
     * @return number of used bytes
     */
    int getUsed() {
        return used;
    }

    /**
     * Sets the number of bytes currently being used. Always <= length
     * @param used number of used bytes
     */
    void setUsed(int used) {
        this.used = used;
    }

    /**
     * Increments the number of bytes currently being used.
     * Current used + increment must be <= length
     * @param increment number of used bytes to increment
     */
    void incrementUsed(int increment) {
        this.used += increment;
    }

    /*
     * Reallocate the array, copying "used" bytes from the existng array.
     * @param len new array length
     * @throws OutOfMemoryError if the allocation is refused by the system
     */
    void resize(final long len) {
        if (global) {
            throw new UnsupportedOperationException("Cannot resize a global OffHeapArray");
        }

        Arena newArena = Arena.ofConfined();

        // note: may throw OOME:
        MemorySegment newSegment = newArena.allocate(len, ALIGNMENT);

        // If there are any bytes in use, copy them to the newly reallocated array
        if (this.used > 0) {
            MemorySegment.copy(segment, 0, newSegment, 0, Math.min(this.used, len));
        }

        this.arena.close();
        this.arena = newArena;
        this.segment = newSegment;

        if (LOG_OFF_HEAP_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.reallocateMemory = "
                                + len + " for segment = " + this.segment);
        }
    }

    void free() {
        arena.close();
        if (LOG_OFF_HEAP_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.freeMemory =       "
                                + this.segment.byteSize()
                                + " for segment = " + this.segment);
        }
    }

    void fill(final byte val) {
        segment.fill(val);
    }

    void putByte(long offset, byte val) {
        segment.set(BYTE_LAYOUT, offset, val);
    }

    void putInt(long offset, int val) {
        segment.set(INT_LAYOUT, offset, val);
    }

    byte getByte(long offset) {
        return segment.get(BYTE_LAYOUT, offset);
    }

    int getInt(long offset) {
        return segment.get(INT_LAYOUT, offset);
    }
}
