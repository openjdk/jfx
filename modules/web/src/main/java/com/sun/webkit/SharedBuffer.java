/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

public final class SharedBuffer {

    private long nativePointer;


    SharedBuffer() {
        this.nativePointer = twkCreate();
    }

    private SharedBuffer(long nativePointer) {
        if (nativePointer == 0) {
            throw new IllegalArgumentException("nativePointer is 0");
        }
        this.nativePointer = nativePointer;
    }


    private static SharedBuffer fwkCreate(long nativePointer) {
        return new SharedBuffer(nativePointer);
    }

    long size() {
        if (nativePointer == 0) {
            throw new IllegalStateException("nativePointer is 0");
        }
        return twkSize(nativePointer);
    }

    int getSomeData(long position, byte[] buffer, int offset, int length) {
        if (nativePointer == 0) {
            throw new IllegalStateException("nativePointer is 0");
        }
        if (position < 0) {
            throw new IndexOutOfBoundsException("position is negative");
        }
        if (position > size()) {
            throw new IndexOutOfBoundsException(
                    "position is greater than size");
        }
        if (buffer == null) {
            throw new NullPointerException("buffer is null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("offset is negative");
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("length is negative");
        }
        if (length > buffer.length - offset) {
            throw new IndexOutOfBoundsException(
                    "length is greater than buffer.length - offset");
        }
        return twkGetSomeData(nativePointer, position, buffer, offset, length);
    }

    void append(byte[] buffer, int offset, int length) {
        if (nativePointer == 0) {
            throw new IllegalStateException("nativePointer is 0");
        }
        if (buffer == null) {
            throw new NullPointerException("buffer is null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("offset is negative");
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("length is negative");
        }
        if (length > buffer.length - offset) {
            throw new IndexOutOfBoundsException(
                    "length is greater than buffer.length - offset");
        }
        twkAppend(nativePointer, buffer, offset, length);
    }

    void dispose() {
        if (nativePointer == 0) {
            throw new IllegalStateException("nativePointer is 0");
        }
        twkDispose(nativePointer);
        nativePointer = 0;
    }

    private static native long twkCreate();

    private static native long twkSize(long nativePointer);

    private static native int twkGetSomeData(long nativePointer,
                                             long position,
                                             byte[] buffer,
                                             int offset,
                                             int length);

    private static native void twkAppend(long nativePointer,
                                         byte[] buffer,
                                         int offset,
                                         int length);

    private static native void twkDispose(long nativePointer);
}
