/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class GrowableDataBufferTest {

    public boolean encodableBooleans[] = {
        false, true
    };

    public char encodableChars[] = {
        ' ', 'A', 'Z', 'a', 'z', '0', '9'
    };

    public byte encodableBytes[] = {
        -1, 0, 1, Byte.MIN_VALUE, Byte.MAX_VALUE
    };

    public short encodableShorts[] = {
        -1, 0, 1, Short.MIN_VALUE, Short.MAX_VALUE
    };

    public int encodableInts[] = {
        -1, 0, 1, Integer.MIN_VALUE, Integer.MAX_VALUE
    };

    public long encodableLongs[] = {
        -1L, 0L, 1L, Long.MIN_VALUE, Long.MAX_VALUE
    };

    public float encodableFloats[] = {
        -1.0f, 0.0f, 1.0f, Float.MIN_VALUE, Float.MAX_VALUE,
        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
    };

    public double encodableDoubles[] = {
        -1.0, 0.0, 1.0, Double.MIN_VALUE, Double.MAX_VALUE,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
    };

    int NUM_VALUES =
            encodableBooleans.length +
            encodableChars.length +
            encodableBytes.length +
            encodableShorts.length +
            encodableInts.length +
            encodableLongs.length +
            encodableFloats.length +
            encodableDoubles.length;
    int NUM_BYTES =
            encodableBooleans.length +
            encodableChars.length * 2 +
            encodableBytes.length +
            encodableShorts.length * 2 +
            encodableInts.length * 4 +
            encodableLongs.length * 8 +
            encodableFloats.length * 4 +
            encodableDoubles.length * 8;

    void putBooleans(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (boolean b : encodableBooleans) {
            if (putPrim) gdb.putBoolean(b);
            if (putObj) gdb.putObject(Boolean.valueOf(b));
        }
    }

    void getBooleans(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (boolean b : encodableBooleans) {
            if (getPrim) assertTrue(gdb.getBoolean() == b);
            if (getObj) assertTrue(gdb.getObject().equals(Boolean.valueOf(b)));
        }
    }

    void putChars(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (char c : encodableChars) {
            if (putPrim) gdb.putChar(c);
            if (putObj) gdb.putObject(Character.valueOf(c));
        }
    }

    void getChars(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (char c : encodableChars) {
            if (getPrim) assertTrue(gdb.getChar() == c);
            if (getObj) assertTrue(gdb.getObject().equals(Character.valueOf(c)));
        }
    }

    void putBytes(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (byte b : encodableBytes) {
            if (putPrim) gdb.putByte(b);
            if (putObj) gdb.putObject(Byte.valueOf(b));
        }
    }

    void getBytes(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (byte b : encodableBytes) {
            if (getPrim) assertTrue(gdb.getByte() == b);
            if (getObj) assertTrue(gdb.getObject().equals(Byte.valueOf(b)));
        }
    }

    void putUBytes(GrowableDataBuffer gdb) {
        for (byte b : encodableBytes) {
            gdb.putByte(b);
        }
    }

    void getUBytes(GrowableDataBuffer gdb) {
        for (byte b : encodableBytes) {
            assertTrue(gdb.getUByte() == (b & 0xff));
        }
    }

    void putShorts(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (short s : encodableShorts) {
            if (putPrim) gdb.putShort(s);
            if (putObj) gdb.putObject(Short.valueOf(s));
        }
    }

    void getShorts(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (short s : encodableShorts) {
            if (getPrim) assertTrue(gdb.getShort() == s);
            if (getObj) assertTrue(gdb.getObject().equals(Short.valueOf(s)));
        }
    }

    void putInts(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (int i : encodableInts) {
            if (putPrim) gdb.putInt(i);
            if (putObj) gdb.putObject(Integer.valueOf(i));
        }
    }

    void getInts(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (int i : encodableInts) {
            if (getPrim) assertTrue(gdb.getInt() == i);
            if (getObj) assertTrue(gdb.getObject().equals(Integer.valueOf(i)));
        }
    }

    void putLongs(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (long l : encodableLongs) {
            if (putPrim) gdb.putLong(l);
            if (putObj) gdb.putObject(Long.valueOf(l));
        }
    }

    void getLongs(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (long c : encodableLongs) {
            if (getPrim) assertTrue(gdb.getLong() == c);
            if (getObj) assertTrue(gdb.getObject().equals(Long.valueOf(c)));
        }
    }

    void putFloats(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (float f : encodableFloats) {
            if (putPrim) gdb.putFloat(f);
            if (putObj) gdb.putObject(Float.valueOf(f));
        }
    }

    void getFloats(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (float f : encodableFloats) {
            if (getPrim) assertTrue(gdb.getFloat() == f);
            if (getObj) assertTrue(gdb.getObject().equals(Float.valueOf(f)));
        }
    }

    void putFloatNaN(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        if (putPrim) gdb.putFloat(Float.NaN);
        if (putObj) gdb.putObject(Float.valueOf(Float.NaN));
    }

    void getFloatNaN(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        if (getPrim) assertTrue(Float.isNaN(gdb.getFloat()));
        if (getObj) assertTrue(gdb.getObject().equals(Float.valueOf(Float.NaN)));
    }

    void putDoubles(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        for (double d : encodableDoubles) {
            if (putPrim) gdb.putDouble(d);
            if (putObj) gdb.putObject(Double.valueOf(d));
        }
    }

    void getDoubles(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        for (double d : encodableDoubles) {
            if (getPrim) assertTrue(gdb.getDouble() == d);
            if (getObj) assertTrue(gdb.getObject().equals(Double.valueOf(d)));
        }
    }

    void putDoubleNaN(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        if (putPrim) gdb.putDouble(Double.NaN);
        if (putObj) gdb.putObject(Double.valueOf(Double.NaN));
    }

    void getDoubleNaN(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        if (getPrim) assertTrue(Double.isNaN(gdb.getDouble()));
        if (getObj) assertTrue(gdb.getObject().equals(Double.valueOf(Double.NaN)));
    }

    void fill(GrowableDataBuffer gdb, boolean putPrim, boolean putObj) {
        putBooleans(gdb, putPrim, putObj);
        putChars(gdb, putPrim, putObj);
        putBytes(gdb, putPrim, putObj);
        putShorts(gdb, putPrim, putObj);
        putInts(gdb, putPrim, putObj);
        putLongs(gdb, putPrim, putObj);
        putFloats(gdb, putPrim, putObj);
        putFloatNaN(gdb, putPrim, putObj);
        putDoubles(gdb, putPrim, putObj);
        putDoubleNaN(gdb, putPrim, putObj);
    }

    void test(GrowableDataBuffer gdb, boolean getPrim, boolean getObj) {
        getBooleans(gdb, getPrim, getObj);
        getChars(gdb, getPrim, getObj);
        getBytes(gdb, getPrim, getObj);
        getShorts(gdb, getPrim, getObj);
        getInts(gdb, getPrim, getObj);
        getLongs(gdb, getPrim, getObj);
        getFloats(gdb, getPrim, getObj);
        getFloatNaN(gdb, getPrim, getObj);
        getDoubles(gdb, getPrim, getObj);
        getDoubleNaN(gdb, getPrim, getObj);
    }

    @Test public void testCapacities() {
        for (int i = 1; i < 100000; i += 100) {
            GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(i, i);
            assertFalse(gdb.hasValues());
            assertFalse(gdb.hasObjects());
            assertTrue(gdb.isEmpty());
            assertTrue(gdb.valueCapacity() >= i);
            assertTrue(gdb.objectCapacity() >= i);
        }
    }

    @Test public void testWriteAndReadValues() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, true, false);
        assertTrue(gdb.hasValues());
        assertFalse(gdb.hasObjects());
        test(gdb, true, false);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testWriteAndReadUbytes() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        putUBytes(gdb);
        assertTrue(gdb.hasValues());
        assertFalse(gdb.hasObjects());
        getUBytes(gdb);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testWriteAndReadObjects() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, false, true);
        assertFalse(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        test(gdb, false, true);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testWriteAndReadValuesAndObjects() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, true, true);
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        test(gdb, true, true);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testWriteAndReadGrowableValues() {
        for (int i = 0; i < NUM_BYTES; i++) {
            GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(i, NUM_VALUES);
            fill(gdb, true, false);
            assertTrue(gdb.hasValues());
            assertFalse(gdb.hasObjects());
            test(gdb, true, false);
            assertFalse(gdb.hasValues());
            assertFalse(gdb.hasObjects());
        }
    }

    @Test public void testWriteAndReadGrowableObjects() {
        for (int i = 0; i < NUM_VALUES; i++) {
            GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, i);
            fill(gdb, false, true);
            assertFalse(gdb.hasValues());
            assertTrue(gdb.hasObjects());
            test(gdb, false, true);
            assertFalse(gdb.hasValues());
            assertFalse(gdb.hasObjects());
        }
    }

    @Test public void testWriteAndMultipleReads() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, true, true);
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        gdb.save();
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        test(gdb, true, true);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
        gdb.restore();
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        test(gdb, true, true);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testPeekValues() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, true, false);
        assertTrue(gdb.hasValues());
        assertFalse(gdb.hasObjects());
        for (int i = 0; i < gdb.writeValuePosition(); i++) {
            gdb.peekByte(i);
        }
        assertTrue(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testPeekObjects() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, false, true);
        assertFalse(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        for (int i = 0; i < gdb.writeValuePosition(); i++) {
            gdb.peekByte(i);
        }
        assertFalse(gdb.hasValues());
        assertTrue(gdb.hasObjects());
    }

    @Test public void testAppend() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb, true, true);
        GrowableDataBuffer gdb2 = GrowableDataBuffer.getBuffer(NUM_BYTES, NUM_VALUES);
        fill(gdb2, true, true);
        gdb.append(gdb2);
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        test(gdb, true, true);
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        test(gdb, true, true);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }

    @Test public void testReset() {
        GrowableDataBuffer gdb = GrowableDataBuffer.getBuffer(0, 0);
        fill(gdb, true, true);
        assertTrue(gdb.hasValues());
        assertTrue(gdb.hasObjects());
        int valcapacity = gdb.valueCapacity();
        int objcapacity = gdb.objectCapacity();
        for (int i = 0; i < 5; i++) {
            gdb.reset();
            assertFalse(gdb.hasValues());
            assertFalse(gdb.hasObjects());
            fill(gdb, true, true);
            assertTrue(gdb.hasValues());
            assertTrue(gdb.hasObjects());
        }
        assertTrue(gdb.valueCapacity() == valcapacity);
        assertTrue(gdb.objectCapacity() == objcapacity);
        test(gdb, true, true);
        assertFalse(gdb.hasValues());
        assertFalse(gdb.hasObjects());
    }
}
