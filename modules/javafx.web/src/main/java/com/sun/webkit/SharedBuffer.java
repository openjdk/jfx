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


    /**
     * Wraps a native {@code WebCore::SharedBuffer} the library already owns, which is the
     * {@code graphics.create_shared_buffer} callback slot.
     * <p>
     * It is public rather than private only because the upcall target that calls it lives in
     * {@code com.sun.webkit.graphics}, beside the slot's one caller in
     * {@code graphics/java/FontCustomPlatformData.cpp}. That package is not exported by
     * {@code javafx.web}, so this stays module internal either way.
     *
     * @param nativePointer the {@code WebCore::SharedBuffer*}, must not be zero
     * @return the wrapper
     */
    public static SharedBuffer fwkCreate(long nativePointer) {
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

    private static long twkCreate() {
        return SharedBufferNative.create();
    }

    private static long twkSize(long nativePointer) {
        return SharedBufferNative.size(nativePointer);
    }

    private static int twkGetSomeData(long nativePointer,
                                      long position,
                                      byte[] buffer,
                                      int offset,
                                      int length) {
        return SharedBufferNative.getSomeData(nativePointer, position, buffer, offset, length);
    }

    private static void twkAppend(long nativePointer,
                                  byte[] buffer,
                                  int offset,
                                  int length) {
        SharedBufferNative.append(nativePointer, buffer, offset, length);
    }

    private static void twkDispose(long nativePointer) {
        SharedBufferNative.dispose(nativePointer);
    }
}
