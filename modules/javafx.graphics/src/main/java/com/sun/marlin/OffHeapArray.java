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
import java.lang.reflect.Field;
import sun.misc.Unsafe;

// KCR FIXME: We must replace the terminally deprecated sun.misc.Unsafe
// memory access methods; see JDK-8334137
@SuppressWarnings("removal")
final class OffHeapArray  {

    // unsafe reference
    private static final Unsafe UNSAFE;
    // size of int / float
    static final int SIZE_INT;

    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new InternalError("Unable to get sun.misc.Unsafe instance", e);
        }

        SIZE_INT = Unsafe.ARRAY_INT_INDEX_SCALE;
    }

    /* members */
    private long address;
    private long length;
    private int used;

    OffHeapArray(final Object parent, final long len) {
        // note: may throw OOME:
        this.address = UNSAFE.allocateMemory(len);
        this.length  = len;
        this.used    = 0;
        if (LOG_UNSAFE_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.allocateMemory =   "
                                + len + " to addr = " + this.address);
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
        // note: may throw OOME:
        this.address = UNSAFE.reallocateMemory(address, len);
        this.length  = len;
        if (LOG_UNSAFE_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.reallocateMemory = "
                                + len + " to addr = " + this.address);
        }
    }

    void free() {
        UNSAFE.freeMemory(this.address);
        if (LOG_UNSAFE_MALLOC) {
            MarlinUtils.logInfo(System.currentTimeMillis()
                                + ": OffHeapArray.freeMemory =       "
                                + this.length
                                + " at addr = " + this.address);
        }
        this.address = 0L;
    }

    void fill(final byte val) {
        UNSAFE.setMemory(this.address, this.length, val);
    }

    void putByte(long offset, byte val) {
        UNSAFE.putByte(address + offset, val);
    }

    void putInt(long offset, int val) {
        UNSAFE.putInt(address + offset, val);
    }

    byte getByte(long offset) {
        return UNSAFE.getByte(address + offset);
    }

    int getInt(long offset) {
        return UNSAFE.getInt(address + offset);
    }

}
