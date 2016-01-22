/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.util.Arrays;

/**
 * A growable buffer that can contain both byte-encoded primitive values
 * and a list of Objects stored for communication between a writer that fills
 * it with data and a reader that empties the data behind the writer.
 *
 * Both buffers (the byte-encoded array and the Object array) grow as needed
 * with no hard limits and the two are kept separately so it is up to the
 * reader and writer to read the two streams in a predetermined synchronicity
 * of the two streams.
 *
 * The methods on a given GrowableDataBuffer object are not synchronized or
 * thread-safe and writing to or reading from the object from more than one
 * thread at a time is unsupported.  In particular, multiple writer threads
 * and/or multiple reader threads will definitely cause problems.
 *
 * The static getBuffer() factory methods and the static returnBuffer() method
 * are all synchronized so that they can be called from any thread at any
 * time, but any given buffer should only be returned to the pool once.
 */
public class GrowableDataBuffer {
    static final int VAL_GROW_QUANTUM = 1024;
    static final int MAX_VAL_GROW = 1024 * 1024;
    static final int MIN_OBJ_GROW = 32;

    static class WeakLink {
        WeakReference<GrowableDataBuffer> bufref;
        WeakLink next;
    }
    static WeakLink buflist = new WeakLink(); // Dummy "head" link object

    /**
     * Retrieve a buffer with an initial byte-encoding capacity of at least
     * {@code minsize} bytes.
     * The initial capacity of the object buffer will be the default size.
     *
     * @param minsize the minimum initial size of the byte-encoding buffer
     * @return a {@code GrowableDataBuffer} object of the requested size
     */
    public static GrowableDataBuffer getBuffer(int minsize) {
        return getBuffer(minsize, MIN_OBJ_GROW);
    }

    /**
     * Retrieve a buffer with an initial byte-encoding capacity of at least
     * {@code minvals} bytes and an initial object buffer capacity of at
     * least {@code minobjs} Objects.
     *
     * @param minvals the minimum initial size of the byte-encoding buffer
     * @param minobjs the minimum initial size of the Object buffer
     * @return a {@code GrowableDataBuffer} object of the requested sizes
     */
    public synchronized static GrowableDataBuffer getBuffer(int minvals, int minobjs) {
        WeakLink prev = buflist;
        WeakLink cur = buflist.next;
        while (cur != null) {
            GrowableDataBuffer curgdb = cur.bufref.get();
            WeakLink next = cur.next;
            if (curgdb == null) {
                prev.next = cur = next;
                continue;
            }
            if (curgdb.valueCapacity() >= minvals && curgdb.objectCapacity() >= minobjs) {
                prev.next = next;
                return curgdb;
            }
            prev = cur;
            cur = next;
        }
        return new GrowableDataBuffer(minvals, minobjs);
    }

    /**
     * Return the indicated {@code GrowableDataBuffer} object to the pool
     * for reuse.
     * A given {@code GrowableDataBuffer} object should only be returned to
     * the pool once per retrieval from the {@code getBuffer()} methods.
     *
     * @param gdb the {@code GrowableDataBuffer} object to be reused.
     */
    public synchronized static void returnBuffer(GrowableDataBuffer retgdb) {
        int retvlen = retgdb.valueCapacity();
        int retolen = retgdb.objectCapacity();
        retgdb.reset();

        WeakLink prev = buflist;
        WeakLink cur = buflist.next;
        while (cur != null) {
            GrowableDataBuffer curgdb = cur.bufref.get();
            WeakLink next = cur.next;
            if (curgdb == null) {
                prev.next = cur = next;
                continue;
            }
            int curvlen = curgdb.valueCapacity();
            int curolen = curgdb.objectCapacity();
            if (curvlen > retvlen ||
                (curvlen == retvlen && curolen >= retolen))
            {
                break;
            }
            prev = cur;
            cur = next;
        }
        WeakLink retlink = new WeakLink();
        retlink.bufref = new WeakReference<>(retgdb);
        prev.next = retlink;
        retlink.next = cur;
    }

    byte vals[];
    int writevalpos;     // next vals location to write encoded values
    int readvalpos;      // next vals location to read encoded values
    int savevalpos;      // saved valpos for reading data multiple times

    Object objs[];
    int writeobjpos;     // next objs location to write data objects
    int readobjpos;      // next objs location to read data objects
    int saveobjpos;      // saved objpos for reading objects multiple times

    private GrowableDataBuffer(int initvalsize, int initobjsize) {
        vals = new byte[initvalsize];
        objs = new Object[initobjsize];
    }

    /**
     * The location of the next byte to be read from the encoded value
     * buffer.
     * This must always be less than or equal to the
     * {@code writeValuePosition()}.
     *
     * @return the byte position of the next byte data to be read.
     */
    public int readValuePosition() {
        return readvalpos;
    }

    /**
     * The location of the next byte to be written to the encoded value
     * buffer.
     *
     * @return the byte position of the next byte data to be written.
     */
    public int writeValuePosition() {
        return writevalpos;
    }

    /**
     * The location of the next object to be read from the object buffer.
     * This must always be less than or equal to the
     * {@code writeObjectPosition()}.
     *
     * @return the position of the next object to be read.
     */
    public int readObjectPosition() {
        return readobjpos;
    }

    /**
     * The location of the next object to be written to the object buffer.
     *
     * @return the position of the next object to be written.
     */
    public int writeObjectPosition() {
        return writeobjpos;
    }

    /**
     * The capacity, in bytes, of the byte-encoding buffer.
     *
     * @return the capacity of the byte-encoding buffer
     */
    public int valueCapacity() {
        return vals.length;
    }

    /**
     * The capacity, in objects, of the {@code Object} buffer.
     *
     * @return the capacity of the {@code Object} buffer
     */
    public int objectCapacity() {
        return objs.length;
    }

    /**
     * Save aside the current read positions of both the byte-encoding
     * buffer and the {@code Object} buffer for a later {@code restore()}
     * operation.
     */
    public void save() {
        savevalpos = readvalpos;
        saveobjpos = readobjpos;
    }

    /**
     * Restore the read positions of both the byte-encoding buffer and
     * the {@code Object} buffer to their last saved positions.
     */
    public void restore() {
        readvalpos = savevalpos;
        readobjpos = saveobjpos;
    }

    /**
     * Indicates whether or not there are values in the byte-encoding
     * buffer waiting to be read.
     *
     * @return true iff there are data values to be read
     */
    public boolean hasValues() {
        return (readvalpos < writevalpos);
    }

    /**
     * Indicates whether or not there are objects in the object
     * buffer waiting to be read.
     *
     * @return true iff there are objects to be read
     */
    public boolean hasObjects() {
        return (readobjpos < writeobjpos);
    }

    /**
     * Indicates whether the byte-encoding buffer is completely empty.
     * Note that this is different from whether or not there is unread
     * data in the byte-encoding buffer.  A buffer which has been written
     * and then later fully emptied by reading is not considered "empty".
     *
     * @return true iff there is no data at all stored in the byte buffer
     */
    public boolean isEmpty() {
        return (writevalpos == 0);
    }

    /**
     * Clears out all data and resets all positions to the start of the
     * buffers so that a new sequence of writing, then reading of data
     * and objects can begin.
     * Note that the {@code Object} array is cleared to nulls here and
     * those objects will finally become collectable by the garbage collector.
     */
    public void reset() {
        readvalpos = savevalpos = writevalpos = 0;
        readobjpos = saveobjpos = 0;
        if (writeobjpos > 0) {
            Arrays.fill(objs, 0, writeobjpos, null);
            writeobjpos = 0;
        }
    }

    /**
     * Appends the contents of both the byte and {@code Object} buffers in
     * the indicated {@code GrowableDataBuffer} to this object.
     * The data in the other indicated {@code GrowableDataBuffer} object
     * is not disturbed in any way.
     *
     * @param gdb the {@code GrowableDataBuffer} to append to this object
     */
    public void append(GrowableDataBuffer gdb) {
        ensureWriteCapacity(gdb.writevalpos);
        System.arraycopy(gdb.vals, 0, vals, writevalpos, gdb.writevalpos);
        writevalpos += gdb.writevalpos;
        if (writeobjpos + gdb.writeobjpos > objs.length) {
            objs = Arrays.copyOf(objs, writeobjpos + gdb.writeobjpos);
        }
        System.arraycopy(gdb.objs, 0, objs, writeobjpos, gdb.writeobjpos);
        writeobjpos += gdb.writeobjpos;
    }

    private void ensureWriteCapacity(int newbytes) {
        if (newbytes > vals.length - writevalpos) {
            newbytes = writevalpos + newbytes - vals.length;
            // Double in size up to MAX_VAL_GROW
            int growbytes = Math.min(vals.length, MAX_VAL_GROW);
            // And at least by the number of new bytes
            if (growbytes < newbytes) growbytes = newbytes;
            int newsize = vals.length + growbytes;
            newsize = (newsize + (VAL_GROW_QUANTUM - 1)) & ~(VAL_GROW_QUANTUM - 1);
            vals = Arrays.copyOf(vals, newsize);
        }
    }

    private void ensureReadCapacity(int bytesneeded) {
        if (readvalpos + bytesneeded > writevalpos) {
            throw new BufferOverflowException();
        }
    }

    /**
     * Encode a boolean value and write it to the end of the byte-encoding array
     *
     * @param b the boolean value to be written
     */
    public void putBoolean(boolean b) {
        putByte(b ? (byte) 1 : (byte) 0);
    }

    /**
     * Write a byte value to the end of the byte-encoding array
     *
     * @param b the byte value to be written
     */
    public void putByte(byte b) {
        ensureWriteCapacity(1);
        vals[writevalpos++] = b;
    }

    /**
     * Encode a char value and write it to the end of the byte-encoding array
     *
     * @param c the char value to be written
     */
    public void putChar(char c) {
        ensureWriteCapacity(2);
        vals[writevalpos++] = (byte) (c >>  8);
        vals[writevalpos++] = (byte) (c      );
    }

    /**
     * Encode a short value and write it to the end of the byte-encoding array
     *
     * @param s the short value to be written
     */
    public void putShort(short s) {
        ensureWriteCapacity(2);
        vals[writevalpos++] = (byte) (s >>  8);
        vals[writevalpos++] = (byte) (s      );
    }

    /**
     * Encode an int value and write it to the end of the byte-encoding array
     *
     * @param i the int value to be written
     */
    public void putInt(int i) {
        ensureWriteCapacity(4);
        vals[writevalpos++] = (byte) (i >> 24);
        vals[writevalpos++] = (byte) (i >> 16);
        vals[writevalpos++] = (byte) (i >>  8);
        vals[writevalpos++] = (byte) (i      );
    }

    /**
     * Encode a long value and write it to the end of the byte-encoding array
     *
     * @param l the long value to be written
     */
    public void putLong(long l) {
        ensureWriteCapacity(8);
        vals[writevalpos++] = (byte) (l >> 56);
        vals[writevalpos++] = (byte) (l >> 48);
        vals[writevalpos++] = (byte) (l >> 40);
        vals[writevalpos++] = (byte) (l >> 32);
        vals[writevalpos++] = (byte) (l >> 24);
        vals[writevalpos++] = (byte) (l >> 16);
        vals[writevalpos++] = (byte) (l >>  8);
        vals[writevalpos++] = (byte) (l      );
    }

    /**
     * Encode a float value and write it to the end of the byte-encoding array
     *
     * @param f the float value to be written
     */
    public void putFloat(float f) {
        putInt(Float.floatToIntBits(f));
    }

    /**
     * Encode a double value and write it to the end of the byte-encoding array
     *
     * @param d the double value to be written
     */
    public void putDouble(double d) {
        putLong(Double.doubleToLongBits(d));
    }

    /**
     * Write an {@code Object} to the end of the object array
     *
     * @param o the {@code Object} to be written
     */
    public void putObject(Object o) {
        if (writeobjpos >= objs.length) {
            objs = Arrays.copyOf(objs, writeobjpos+MIN_OBJ_GROW);
        }
        objs[writeobjpos++] = o;
    }

    /**
     * Read a single byte from the byte-encoded stream, ignoring any read
     * position, but honoring the current write position as a limit.
     * The read and saved positions are not used or modified in any way
     * by this method
     *
     * @param i the absolute byte location to return from the byte-encoding array
     * @return the byte stored at the indicated location in the byte array
     */
    public byte peekByte(int i) {
        if (i >= writevalpos) {
            throw new BufferOverflowException();
        }
        return vals[i];
    }

    /**
     * Read a single {@code Object} from the object buffer, ignoring any read
     * position, but honoring the current write position as a limit.
     * The read and saved positions are not used or modified in any way
     * by this method
     *
     * @param i the absolute index to return from the {@code Object} array
     * @return the {@code Object} stored at the indicated index
     */
    public Object peekObject(int i) {
        if (i >= writeobjpos) {
            throw new BufferOverflowException();
        }
        return objs[i];
    }

    /**
     * Decodes and returns a single boolean value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded boolean value
     */
    public boolean getBoolean() {
        ensureReadCapacity(1);
        return vals[readvalpos++] != 0;
    }

    /**
     * Returns a single byte value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the returned value.
     *
     * @return the decoded byte value
     */
    public byte getByte() {
        ensureReadCapacity(1);
        return vals[readvalpos++];
    }

    /**
     * Decodes a single unsigned byte value from the current read
     * position in the byte-encoded stream and returns the value cast to
     * an int and bumps the read position
     * past the decoded value.
     *
     * @return the decoded unsigned byte value as an int
     */
    public int getUByte() {
        ensureReadCapacity(1);
        return vals[readvalpos++] & 0xff;
    }

    /**
     * Decodes and returns a single char value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded char value
     */
    public char getChar() {
        ensureReadCapacity(2);
        int c = vals[readvalpos++];
        c = (c << 8) | (vals[readvalpos++] & 0xff);
        return (char) c;
    }

    /**
     * Decodes and returns a single short value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded short value
     */
    public short getShort() {
        ensureReadCapacity(2);
        int s = vals[readvalpos++];
        s = (s << 8) | (vals[readvalpos++] & 0xff);
        return (short) s;
    }

    /**
     * Decodes and returns a single int value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded int value
     */
    public int getInt() {
        ensureReadCapacity(4);
        int i = vals[readvalpos++];
        i = (i << 8) | (vals[readvalpos++] & 0xff);
        i = (i << 8) | (vals[readvalpos++] & 0xff);
        i = (i << 8) | (vals[readvalpos++] & 0xff);
        return i;
    }

    /**
     * Decodes and returns a single long value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded long value
     */
    public long getLong() {
        ensureReadCapacity(8);
        long l = vals[readvalpos++];
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        l = (l << 8) | (vals[readvalpos++] & 0xff);
        return l;
    }

    /**
     * Decodes and returns a single float value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded float value
     */
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    /**
     * Decodes and returns a single double value from the current read
     * position in the byte-encoded stream and bumps the read position
     * past the decoded value.
     *
     * @return the decoded double value
     */
    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    /**
     * Returns a single {@code Object} from the current object read
     * position in the {@code Object} stream and bumps the read position
     * past the returned value.
     *
     * @return the {@code Object} read from the buffer
     */
    public Object getObject() {
        if (readobjpos >= objs.length) {
            throw new BufferOverflowException();
        }
        return objs[readobjpos++];
    }
}
