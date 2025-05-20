/*
 * Copyright (c) 2007, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.marlin.MarlinConst.LOG_UNSAFE_MALLOC;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

final class OffHeapArray  {

    private Arena arena;

    // size of int / float
    static final int SIZE_INT;
    // FFM stuff
    private static final ValueLayout.OfByte BYTE_LAYOUT = ValueLayout.JAVA_BYTE;
    private static final ValueLayout.OfInt INT_LAYOUT = ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN);

    static {
        // KCR FIXME: get this from FFM
        SIZE_INT = 4;
    }

    /* members */
    private MemorySegment segment;
//    private long address;
    private long length;
    private int used;

    OffHeapArray(final Object parent, final long len) {
        arena = Arena.ofShared();

        // note: may throw OOME:
        // KCR FIXME: Set a MemoryLayout
        this.segment = arena.allocate(len);
        this.length  = len;
        this.used    = 0;
        if (LOG_UNSAFE_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.allocateMemory =   "
                                + len + " for segment = " + this.segment);
        }

        // Register a cleaning function to ensure freeing off-heap memory:
        MarlinUtils.getCleaner().register(parent, this::free);
    }

    /**
     * Gets the length of this array.
     *
     * @return the length in bytes
     */
    long getLength() {
        return length;
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
     * Curr used + incr used must be <= length
     * @param used number of used bytes to increment
     */
    void incrementUsed(int used) {
        this.used += used;
    }

    /*
     * As realloc may change the address, updating address is MANDATORY
     * @param len new array length
     * @throws OutOfMemoryError if the allocation is refused by the system
     */
    void resize(final long len) {
        Arena newArena = Arena.ofShared();
        MemorySegment newSegment = newArena.allocate(len);

        // KCR FIXME: We can probably limit the copy to "used" bytes, which
        // should be set to zero (although it currently isn't) when resizing
        // down to default size, which is done in dispose()
        MemorySegment.copy(segment, 0, newSegment, 0, Math.min(this.length, len));
//        if (this.used > 0) {
//            MemorySegment.copy(segment, 0, newSegment, 0, Math.min(this.used, len));
//        }

        this.arena.close();
        this.arena = newArena;
        this.segment = newSegment;
        this.length  = len;

        if (LOG_UNSAFE_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.reallocateMemory = "
                                + len + " for segment = " + this.segment);
        }
    }

    void free() {
        arena.close();
        if (LOG_UNSAFE_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.freeMemory =       "
                                + this.length
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
