/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
