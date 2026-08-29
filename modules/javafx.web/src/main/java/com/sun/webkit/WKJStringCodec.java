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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * The UTF-16 string protocol shared by every hand written {@code *Native} facade in this module
 * (see {@code modules/javafx.web/FFM-ABI-CONTRACT.md} sections 12 and 13).
 * <p>
 * Into native code a string is a {@code const uint16_t* s, int32_t s_len} pair, where a
 * {@link MemorySegment#NULL} pointer is Java {@code null} and a non null pointer with length zero
 * is the empty string. Out of native code the caller provides the buffer:
 * {@code int32_t f(..., uint16_t* result_buf, int32_t result_cap, int32_t* result_length)}
 * returning {@link #OK}, {@link #NULL} or {@link #OVERFLOW}, so nothing is returned that could
 * dangle and there is no "valid until the next call" rule for a facade to get wrong.
 * <p>
 * The generated DOM facades carry an identical protocol in
 * {@code com.sun.webkit.dom.DOMStringCodec}, which is package private to {@code com.sun.webkit.dom}
 * and emitted by {@code buildtools/ffm-web/dom-java-to-ffm.pl}, so it cannot be reused from
 * {@code com.sun.webkit} or {@code com.sun.javafx.webkit.drt}. This class is the one copy the hand
 * written facades share rather than five copies, and it deliberately holds no native state: unlike
 * {@link WebKitNative} it loads no library, so touching it from the DumpRenderTree facade cannot
 * pull {@code jfxwebkit} in as a side effect of decoding a {@code DumpRenderTreeJava} string.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
public final class WKJStringCodec {

    /** {@code WKJ_STR_OK}: {@code result_length} code units were written. */
    public static final int OK = 0;

    /** {@code WKJ_STR_NULL}: the Java-visible value is {@code null}. */
    public static final int NULL = 1;

    /** {@code WKJ_STR_OVERFLOW}: nothing was written; {@code result_length} is the capacity needed. */
    public static final int OVERFLOW = 2;

    /**
     * The capacity, in UTF-16 code units, of the buffer a string getter offers on its first attempt.
     * A frame name, a URL, a user agent, an encoding name and a tooltip all fit; the inner text and
     * render tree getters are the ones that overflow, and they pay one extra downcall for an exactly
     * sized buffer.
     */
    public static final int CAPACITY = 256;

    private WKJStringCodec() {
    }

    /**
     * Allocates the UTF-16 form of a string for a {@code const uint16_t* s, int32_t s_len} parameter
     * pair. A {@code null} string becomes {@link MemorySegment#NULL}, an empty string a non null
     * segment whose {@link #length} is zero.
     *
     * @param allocator the allocator to allocate from
     * @param s the string, may be {@code null}
     * @return the UTF-16 characters of {@code s}, or {@link MemorySegment#NULL} if {@code s} is null
     */
    public static MemorySegment encode(SegmentAllocator allocator, String s) {
        if (s == null) {
            return MemorySegment.NULL;
        }
        if (s.isEmpty()) {
            // A zero byte allocation is not guaranteed to have a non zero address, and a zero
            // address would read as Java null on the C side, so the empty string gets one zeroed
            // character.
            return allocator.allocate(JAVA_CHAR);
        }
        return allocator.allocateFrom(JAVA_CHAR, s.toCharArray());
    }

    /**
     * Returns the length in UTF-16 code units of the buffer {@link #encode} produces.
     *
     * @param s the string, may be {@code null}
     * @return the length, zero for {@code null} and for the empty string
     */
    public static int length(String s) {
        return s == null ? 0 : s.length();
    }

    /**
     * Turns a completed string call into its Java value.
     *
     * @param status the {@code WKJ_STR_*} status the call returned
     * @param buffer the buffer the call wrote into
     * @param length the segment the call wrote the code unit count into
     * @return the string, or {@code null} if the status is {@link #NULL}
     */
    public static String decode(int status, MemorySegment buffer, MemorySegment length) {
        if (status == NULL) {
            return null;
        }
        if (status != OK) {
            // The caller has already grown the buffer to the size the library asked for, so a
            // second overflow means the two sides disagree about the protocol.
            throw new IllegalStateException("the C library returned string status " + status
                    + " for a buffer of the size it asked for");
        }
        int count = length.get(JAVA_INT, 0);
        if (count == 0) {
            return "";
        }
        char[] chars = new char[count];
        MemorySegment.copy(buffer, JAVA_CHAR, 0L, chars, 0, count);
        return new String(chars);
    }

    /**
     * Writes a Java string into a caller-provided buffer, the way a string-returning upcall slot
     * must. This is {@link #decode} in reverse: it is what a callback whose C prototype ends in
     * {@code uint16_t* result_buf, int32_t result_cap, int32_t* result_length} returns.
     *
     * @param s the value, may be {@code null}
     * @param resultBuffer the buffer the caller provided, may be {@link MemorySegment#NULL}
     * @param resultCapacity the capacity of that buffer, in code units
     * @param resultLength the {@code int32_t*} the caller provided
     * @return {@link #OK}, {@link #NULL} or {@link #OVERFLOW}
     */
    public static int emit(String s, MemorySegment resultBuffer, int resultCapacity,
                           MemorySegment resultLength) {
        if (s == null) {
            setInt(resultLength, 0);
            return NULL;
        }
        int count = s.length();
        if (resultBuffer.address() == 0L || count > resultCapacity) {
            setInt(resultLength, count);
            return OVERFLOW;
        }
        if (count > 0) {
            MemorySegment.copy(s.toCharArray(), 0, resultBuffer, JAVA_CHAR, 0L, count);
        }
        setInt(resultLength, count);
        return OK;
    }

    /**
     * Encodes a string as <em>modified</em> UTF-8, the encoding {@code GetStringUTFChars} produced.
     * Exactly one function of this ABI needs it, {@code wkj_frame_load}, whose JNI ancestor read its
     * argument that way; the C header documents the hazard and requires it preserved, so this is
     * deliberately not standard UTF-8.
     * <p>
     * The two encodings differ only for {@code U+0000}, which becomes {@code C0 80}, and for
     * supplementary characters, whose two surrogates are each encoded as three bytes.
     *
     * @param allocator the allocator to allocate from
     * @param s the string, must not be {@code null}
     * @return the encoded bytes, without a trailing NUL
     */
    public static MemorySegment encodeModifiedUtf8(SegmentAllocator allocator, String s) {
        int count = s.length();
        int bytes = 0;
        for (int i = 0; i < count; i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                bytes += 1;
            } else if (c <= 0x07FF) {
                bytes += 2;
            } else {
                bytes += 3;
            }
        }
        if (bytes == 0) {
            // Same reason as the empty UTF-16 case above: a zero length allocation may have a zero
            // address, and the C side reads a NULL pointer as the Java value having been null.
            return allocator.allocate(JAVA_BYTE);
        }
        byte[] encoded = new byte[bytes];
        int at = 0;
        for (int i = 0; i < count; i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                encoded[at++] = (byte) c;
            } else if (c <= 0x07FF) {
                encoded[at++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                encoded[at++] = (byte) (0x80 | (c & 0x3F));
            } else {
                encoded[at++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                encoded[at++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                encoded[at++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return allocator.allocateFrom(JAVA_BYTE, encoded);
    }

    private static void setInt(MemorySegment target, int value) {
        if (target.address() != 0L) {
            target.set(JAVA_INT, 0, value);
        }
    }
}
