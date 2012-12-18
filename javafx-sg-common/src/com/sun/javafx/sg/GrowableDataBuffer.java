/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg;

import java.nio.BufferOverflowException;
import java.util.Arrays;

/**
 */
public class GrowableDataBuffer<T> {
    static final int MIN_BUF_GROW = 1024;
    static final int MIN_REF_GROW = 32;

    byte buf[];
    T refs[];
    int pos;
    int mark;
    int savepos;
    int refpos;
    int refmark;
    int refsavepos;

    public GrowableDataBuffer(int initsize) {
        buf = new byte[initsize];
    }

    public int position() {
        return pos;
    }

    public void save() {
        savepos = pos;
        refsavepos = refpos;
    }

    public void restore() {
        pos = savepos;
        refpos = refsavepos;
    }

    public boolean isEmpty() {
        return (pos >= mark && refpos >= refmark);
    }

    public void switchToRead() {
        mark = pos;
        refmark = refpos;
        pos = 0;
        refpos = 0;
    }

    public void resetForWrite() {
        pos = mark = 0;
        if (refpos > 0 || refmark > 0) {
            Arrays.fill(refs, 0, Math.max(refpos, refmark), null);
            refpos = refmark = 0;
        }
    }

    public void ensureWriteCapacity(int newbytes) {
        if (pos + newbytes > buf.length) {
            if (newbytes < MIN_BUF_GROW) newbytes = MIN_BUF_GROW;
            buf = Arrays.copyOf(buf, pos + newbytes);
        }
    }

    public void ensureReadCapacity(int bytesneeded) {
        if (pos + bytesneeded > mark) {
            throw new BufferOverflowException();
        }
    }

    public void putBoolean(boolean b) {
        putByte(b ? (byte) 1 : (byte) 0);
    }

    public void putByte(byte b) {
        ensureWriteCapacity(1);
        buf[pos++] = b;
    }

    public void putChar(char c) {
        ensureWriteCapacity(2);
        buf[pos++] = (byte) (c >>  8);
        buf[pos++] = (byte) (c      );
    }

    public void putShort(short s) {
        ensureWriteCapacity(2);
        buf[pos++] = (byte) (s >>  8);
        buf[pos++] = (byte) (s      );
    }

    public void putInt(int i) {
        ensureWriteCapacity(4);
        buf[pos++] = (byte) (i >> 24);
        buf[pos++] = (byte) (i >> 16);
        buf[pos++] = (byte) (i >>  8);
        buf[pos++] = (byte) (i      );
    }

    public void putLong(long l) {
        ensureWriteCapacity(8);
        buf[pos++] = (byte) (l >> 56);
        buf[pos++] = (byte) (l >> 48);
        buf[pos++] = (byte) (l >> 40);
        buf[pos++] = (byte) (l >> 32);
        buf[pos++] = (byte) (l >> 24);
        buf[pos++] = (byte) (l >> 16);
        buf[pos++] = (byte) (l >>  8);
        buf[pos++] = (byte) (l      );
    }

    public void putFloat(float f) {
        putInt(Float.floatToIntBits(f));
    }

    public void putDouble(double d) {
        putLong(Double.doubleToLongBits(d));
    }

    public void putObject(T o) {
        if (refs == null) {
            refs = (T[]) new Object[MIN_REF_GROW];
        } else if (refpos >= refs.length) {
            refs = Arrays.copyOf(refs, refpos+MIN_REF_GROW);
        }
        refs[refpos++] = o;
    }

    public boolean getBoolean() {
        ensureReadCapacity(1);
        return buf[pos++] != 0;
    }

    public byte getByte() {
        ensureReadCapacity(1);
        return buf[pos++];
    }

    public int getUByte() {
        ensureReadCapacity(1);
        return buf[pos++] & 0xff;
    }

    public char getChar() {
        ensureReadCapacity(2);
        int c = buf[pos++];
        c = (c << 8) | (buf[pos++] & 0xff);
        return (char) c;
    }

    public short getShort() {
        ensureReadCapacity(2);
        int s = buf[pos++];
        s = (s << 8) | (buf[pos++] & 0xff);
        return (short) s;
    }

    public int getInt() {
        ensureReadCapacity(4);
        int i = buf[pos++];
        i = (i << 8) | (buf[pos++] & 0xff);
        i = (i << 8) | (buf[pos++] & 0xff);
        i = (i << 8) | (buf[pos++] & 0xff);
        return i;
    }

    public long getLong() {
        ensureReadCapacity(8);
        long l = buf[pos++];
        l = (l << 8) | (buf[pos++] & 0xff);
        l = (l << 8) | (buf[pos++] & 0xff);
        l = (l << 8) | (buf[pos++] & 0xff);
        l = (l << 8) | (buf[pos++] & 0xff);
        l = (l << 8) | (buf[pos++] & 0xff);
        l = (l << 8) | (buf[pos++] & 0xff);
        l = (l << 8) | (buf[pos++] & 0xff);
        return l;
    }

    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    public T getObject() {
        if (refs == null || refpos >= refs.length) {
            throw new BufferOverflowException();
        }
        return refs[refpos++];
    }
}
