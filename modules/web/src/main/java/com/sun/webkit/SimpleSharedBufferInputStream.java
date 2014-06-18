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

package com.sun.webkit;

import java.io.InputStream;

public final class SimpleSharedBufferInputStream extends InputStream {

    private final SharedBuffer sharedBuffer;
    private long position;


    public SimpleSharedBufferInputStream(SharedBuffer sharedBuffer) {
        if (sharedBuffer == null) {
            throw new NullPointerException("sharedBuffer is null");
        }
        this.sharedBuffer = sharedBuffer;
    }


    @Override
    public int read() {
        byte[] buffer = new byte[1];
        int length = sharedBuffer.getSomeData(position, buffer, 0, 1);
        if (length != 0) {
            position++;
            return buffer[0] & 0xff;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException("b is null");
        }
        if (off < 0) {
            throw new IndexOutOfBoundsException("off is negative");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        if (len > b.length - off) {
            throw new IndexOutOfBoundsException(
                    "len is greater than b.length - off");
        }
        if (len == 0) {
            return 0;
        }
        int length = sharedBuffer.getSomeData(position, b, off, len);
        if (length != 0) {
            position += length;
            return length;
        } else {
            return -1;
        }
    }

    @Override
    public long skip(long n) {
        long k = sharedBuffer.size() - position;
        if (n < k) {
            k = n < 0 ? 0 : n;
        }
        position += k;
        return k;
    }

    @Override
    public int available() {
        return (int) Math.min(sharedBuffer.size() - position,
                              Integer.MAX_VALUE);
    }
}
