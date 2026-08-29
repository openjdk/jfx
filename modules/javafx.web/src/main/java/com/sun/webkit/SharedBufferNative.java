/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_shared_buffer_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@link SharedBuffer}.
 * <p>
 * The peer is a {@code WebCore::SharedBufferBuilder*} for {@link #create}, {@link #append} and
 * {@link #dispose} and is read as a {@code WebCore::FragmentedSharedBuffer*} by {@link #size} and
 * {@link #getSomeData}. That split is what the JNI functions did, and it is preserved rather than
 * tidied.
 * <p>
 * {@link #dispose} does not free the builder. That is not an omission here either: the JNI
 * {@code twkDispose} cast its pointer and then did nothing with it, so every buffer
 * {@link #create} makes has always leaked. Reproducing the leak is what keeps this migration
 * behaviour neutral; the fix belongs in its own commit, with evidence that the object really is
 * unreachable by then, because a double free is a far worse failure than a leak.
 * <p>
 * Neither of the two array entry points uses {@code Linker.Option.critical(true)}: the ABI forbids
 * it outright (contract section 13.1, finding 6), so each copies through a confined arena. The
 * arrays involved are the read buffers of {@code SimpleSharedBufferInputStream}, a few kilobytes at
 * most.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class SharedBufferNative {

    private static final MethodHandle CREATE = WebKitNative.downcall(
            "wkj_shared_buffer_create",
            FunctionDescriptor.of(JAVA_LONG));
    private static final MethodHandle SIZE = WebKitNative.downcall(
            "wkj_shared_buffer_size",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
    private static final MethodHandle GET_SOME_DATA = WebKitNative.downcall(
            "wkj_shared_buffer_get_some_data",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle APPEND = WebKitNative.downcall(
            "wkj_shared_buffer_append",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle DISPOSE = WebKitNative.downcall(
            "wkj_shared_buffer_dispose",
            FunctionDescriptor.ofVoid(JAVA_LONG));

    private SharedBufferNative() {
    }

    static long create() {
        try {
            return (long) CREATE.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static long size(long buffer) {
        try {
            return (long) SIZE.invokeExact(buffer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Copies at most {@code length} bytes from {@code position} into {@code dst} at {@code offset}.
     * The whole array is handed over and {@code offset} stays a separate argument, as the C
     * prototype expects, so that the segment the library writes into has the same shape the
     * {@code byte[]} had.
     *
     * @param buffer the buffer handle
     * @param position the position to read from
     * @param dst the destination array, already bounds checked by {@link SharedBuffer}
     * @param offset the offset into {@code dst}
     * @param length the maximum number of bytes to copy
     * @return the number of bytes copied, zero at or past the end
     */
    static int getSomeData(long buffer, long position, byte[] dst, int offset, int length) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_BYTE, dst.length);
            int copied;
            try {
                copied = (int) GET_SOME_DATA.invokeExact(buffer, position, out, offset, length);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            if (copied > 0) {
                MemorySegment.copy(out, JAVA_BYTE, offset, dst, offset, copied);
            }
            return copied;
        }
    }

    /**
     * Appends {@code length} bytes read from {@code src} at {@code offset}.
     *
     * @param buffer the buffer handle
     * @param src the source array, already bounds checked by {@link SharedBuffer}
     * @param offset the offset into {@code src}
     * @param length the number of bytes to append
     */
    static void append(long buffer, byte[] src, int offset, int length) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment in = arena.allocate(JAVA_BYTE, src.length);
            MemorySegment.copy(src, offset, in, JAVA_BYTE, offset, length);
            try {
                APPEND.invokeExact(buffer, in, offset, length);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static void dispose(long buffer) {
        try {
            DISPOSE.invokeExact(buffer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
