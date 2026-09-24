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

package com.sun.javafx.font;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * The two string conversions the Linux font JNI code left to HotSpot, reproduced in Java: {@code GetStringUTFChars}
 * (Java to C) and {@code NewStringUTF} (C to Java), as {@code pango.c} and {@code fontpath_linux.c} of commit
 * {@code 7b43255b30} used them for {@code pango_font_description_set_family}, {@code FcConfigAppFontAddFile}, the
 * fontconfig locale, {@code pango_font_description_get_family} and every family, style, full name and file name
 * fontconfig reports. A Java binding to {@code libpango-1.0.so.0} and {@code libfontconfig.so.1} that converts through
 * this class hands C the bytes the JNI code handed it and gives Java the {@code char}s the JNI code gave it, for any
 * input - not only for the ASCII the production callers pass.
 *
 * <h2>Java to C: modified UTF-8</h2>
 * {@code GetStringUTFChars} encodes UTF-16 code units one at a time ({@code UNICODE::utf8_size} and
 * {@code UNICODE::utf8_write}, {@code hotspot/share/utilities/utf8.cpp}): {@code U+0001..U+007F} as one byte,
 * {@code U+0000} and {@code U+0080..U+07FF} as two, everything else as three. A supplementary character is therefore
 * its two surrogates, three bytes each ({@code F0 9F 98 80} never appears), an unpaired surrogate is encoded like any
 * other code unit, and the string C sees never contains a {@code 00} byte before its terminator.
 *
 * <h2>C to Java: {@code NewStringUTF} on arbitrary bytes</h2>
 * {@code jni_NewStringUTF} calls {@code java_lang_String::create_from_str}
 * ({@code hotspot/share/classfile/javaClasses.cpp}), which never rejects input. It reads up to the first {@code 00}
 * byte and
 * <ol>
 * <li>counts the characters with {@code UTF8::unicode_length}: every byte that is not a continuation byte
 * ({@code (b & 0xC0) != 0x80}) counts as one;</li>
 * <li>decodes exactly that many characters from the start with {@code UTF8::next}: a {@code 110xxxxx 10xxxxxx} pair
 * or a {@code 1110xxxx 10xxxxxx 10xxxxxx} triple gives one character, overlong forms and encoded surrogates included;
 * any other byte - a lead byte whose continuation bytes are missing, a stray continuation byte, a four-byte lead
 * ({@code F0..F7}), {@code F8..FF} - gives the byte's own value as one character and is consumed alone.</li>
 * </ol>
 * The count and the decoder disagree on malformed input, which is what makes the result differ from
 * {@code new String(bytes, UTF_8)}: a stray continuation byte consumed as a character is not counted, so as many
 * characters are dropped from the end ({@code 41 F0 9F 98 80 42} gives {@code A U+00F0 U+009F} and the {@code B} is
 * lost); nothing ever becomes {@code U+FFFD}. {@code create_from_str} also chooses the Latin-1 or UTF-16 coder of
 * the new {@code String} by a byte-level rule ({@code is_latin1} stays true unless a continuation byte follows a byte
 * above {@code C3}); that choice never changes the characters, only the representation, and a {@code String} built
 * here is always compacted by the JDK, so the two can differ in {@code String.equals} for malformed input whose
 * characters are all below {@code U+0100} but whose bytes are not - never in content.
 * <p>
 * The eight byte strings measured through the JNI {@code NewStringUTF} of JDK 25.0.4 (Linux) and JDK 26+35 (Windows)
 * are pinned by {@code test.com.sun.javafx.font.JniStringCodecTest};
 * {@code test.com.sun.javafx.font.JniStringOracleTest} compares both directions with the running JVM's own
 * {@code NewStringUTF} and {@code GetStringUTFRegion} (the encoder of {@code GetStringUTFChars}) on random and
 * structured input.
 */
public final class JniStringCodec {

    private JniStringCodec() {
    }

    /**
     * The number of bytes {@link #toModifiedUtf8} produces for {@code s} before the terminator: what
     * {@code GetStringUTFLength} reports.
     */
    public static int modifiedUtf8Length(String s) {
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            length += encodedSize(s.charAt(i));
        }
        return length;
    }

    /**
     * {@code s} as {@code GetStringUTFChars} returns it: modified UTF-8 followed by a {@code 00} terminator, so the
     * array is a complete C string and {@code allocator.allocateFrom(JAVA_BYTE, bytes)} hands it to C as is. The
     * last element is the terminator; {@link #modifiedUtf8Length} is the length without it.
     */
    public static byte[] toModifiedUtf8(String s) {
        byte[] bytes = new byte[modifiedUtf8Length(s) + 1];
        int p = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (encodedSize(c)) {
                case 1 -> bytes[p++] = (byte) c;
                case 2 -> {
                    bytes[p++] = (byte) (0xC0 | (c >> 6));
                    bytes[p++] = (byte) (0x80 | (c & 0x3F));
                }
                default -> {
                    bytes[p++] = (byte) (0xE0 | (c >> 12));
                    bytes[p++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                    bytes[p++] = (byte) (0x80 | (c & 0x3F));
                }
            }
        }
        return bytes;
    }

    /**
     * {@link #toModifiedUtf8} copied into memory from {@code allocator}: the C string {@code GetStringUTFChars} gave,
     * terminator included.
     */
    public static MemorySegment allocateModifiedUtf8(SegmentAllocator allocator, String s) {
        return allocator.allocateFrom(JAVA_BYTE, toModifiedUtf8(s));
    }

    /** {@code UNICODE::utf8_size}: one byte for U+0001..U+007F, two up to U+07FF (U+0000 included), else three. */
    private static int encodedSize(char c) {
        if (c != 0 && c <= 0x7F) {
            return 1;
        }
        return c <= 0x7FF ? 2 : 3;
    }

    /**
     * {@code NewStringUTF} of {@code bytes}: the characters of the bytes before the first {@code 00}, or of the
     * whole array when it has none, decoded as described above.
     */
    public static String fromNewStringUtf(byte[] bytes) {
        int end = 0;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        return decode(bytes, end);
    }

    /**
     * {@code NewStringUTF} of the C string at the start of {@code cString}: {@code null} for a null pointer (what
     * {@code NewStringUTF} returns for one), otherwise the characters of the bytes before the first {@code 00}. The
     * segment is a bounded view of the C string, made by the binding that owns the pointer; the terminator must lie
     * within it, so a view that is too small - the zero-length segment a downcall returns included - fails instead
     * of reading past its end.
     *
     * @throws IllegalArgumentException when no {@code 00} byte lies within {@code cString}
     */
    public static String fromNewStringUtf(MemorySegment cString) {
        if (cString.address() == 0) {
            return null;
        }
        long size = cString.byteSize();
        long end = 0;
        while (end < size && cString.get(JAVA_BYTE, end) != 0) {
            end++;
        }
        if (end == size) {
            throw new IllegalArgumentException("no terminator within the " + size + " bytes at 0x"
                    + Long.toHexString(cString.address()) + ": the view of the C string is too small");
        }
        byte[] bytes = cString.asSlice(0, end).toArray(JAVA_BYTE);
        return decode(bytes, bytes.length);
    }

    /**
     * {@code UTF8::unicode_length} then {@code UTF8::convert_to_unicode} over {@code bytes[0..end)}. {@code end} is
     * where the terminator is: a byte read at or past it is the terminator, {@code 00}, never a continuation byte,
     * which is what stops a sequence that runs into the end of the string.
     */
    private static String decode(byte[] bytes, int end) {
        int count = 0;
        for (int i = 0; i < end; i++) {
            if ((bytes[i] & 0xC0) != 0x80) {
                count++;
            }
        }
        char[] chars = new char[count];
        int p = 0;
        for (int i = 0; i < count; i++) {
            int b0 = byteAt(bytes, p, end);
            int value = b0;
            int length = 1;
            if (b0 >= 0xC0 && b0 <= 0xDF) {
                int b1 = byteAt(bytes, p + 1, end);
                if ((b1 & 0xC0) == 0x80) {
                    value = ((b0 & 0x1F) << 6) + (b1 & 0x3F);
                    length = 2;
                }
            } else if (b0 >= 0xE0 && b0 <= 0xEF) {
                int b1 = byteAt(bytes, p + 1, end);
                if ((b1 & 0xC0) == 0x80) {
                    int b2 = byteAt(bytes, p + 2, end);
                    if ((b2 & 0xC0) == 0x80) {
                        value = ((((b0 & 0x0F) << 6) + (b1 & 0x3F)) << 6) + (b2 & 0x3F);
                        length = 3;
                    }
                }
            }
            chars[i] = (char) value;
            p += length;
        }
        return new String(chars);
    }

    private static int byteAt(byte[] bytes, int index, int end) {
        return index < end ? bytes[index] & 0xFF : 0;
    }
}
