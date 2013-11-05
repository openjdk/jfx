/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.ipack.util;

import java.io.DataInput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class LsbDataInputStream extends FilterInputStream
                                      implements DataInput {
    public LsbDataInputStream(final InputStream is) {
        super(is);
    }

    @Override
    public void readFully(final byte[] buffer) throws IOException {
        readFully(buffer, 0, buffer.length);
    }

    @Override
    public void readFully(final byte[] buffer,
                          final int offset,
                          final int length) throws IOException {
        int wrpos = offset;
        int remaining = length;
        while (remaining > 0) {
            final int read = in.read(buffer, wrpos, remaining);
            if (read == -1) {
                throw new EOFException("No more data");
            }

            wrpos += read;
            remaining -= read;
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte readByte() throws IOException {
        final int value = in.read();
        if (value == -1) {
            throw new EOFException("No more data");
        }
        return (byte) value;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short readShort() throws IOException {
        return (short) ((readByte() & 0xff) | (readByte() << 8));
    }

    @Override
    public int readUnsignedShort() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int readInt() throws IOException {
        return ((readByte() & 0xff) | ((readByte() & 0xff) << 8)
                                    | ((readByte() & 0xff) << 16)
                                    | ((readByte() << 24)));
    }

    @Override
    public long readLong() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float readFloat() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
