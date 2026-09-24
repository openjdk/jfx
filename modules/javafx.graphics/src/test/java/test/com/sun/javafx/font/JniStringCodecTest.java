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

package test.com.sun.javafx.font;

import com.sun.javafx.font.JniStringCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JniStringCodec} on its own, on every platform: the eight byte strings measured through the JNI
 * {@code NewStringUTF} of JDK 25.0.4 (Linux) and JDK 26+35 (Windows), the rows derived from HotSpot's
 * {@code UTF8::unicode_length} and {@code UTF8::next}, the modified UTF-8 encoding of {@code U+0000}, surrogate
 * pairs and unpaired surrogates, and the identity of decoding after encoding for arbitrary strings.
 * {@code DataOutputStream.writeUTF}, whose payload is the same modified UTF-8, is the JDK's own second opinion on
 * the encoder. {@link JniStringOracleTest} compares both directions with the running JVM.
 */
public class JniStringCodecTest {

    /** The measured inputs, {@code NewStringUTF} code units and {@code new String(bytes, UTF_8)} code units. */
    static final byte[][] MEASURED_INPUTS = {
        bytes(0x41, 0xF0, 0x9F, 0x98, 0x80, 0x42),
        bytes(0x41, 0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80, 0x42),
        bytes(0x41, 0xC3, 0xA9, 0x42),
        bytes(0x41, 0xE2, 0x82, 0xAC, 0x42),
        bytes(0x41, 0xFF, 0x42),
        bytes(0x41, 0xC3, 0x42),
        bytes(0x41, 0xC0, 0x80, 0x42),
        bytes(0x41, 0xE0, 0x80, 0xAF, 0x42),
    };
    static final char[][] MEASURED_CHARS = {
        chars(0x0041, 0x00F0, 0x009F),
        chars(0x0041, 0xD83D, 0xDE00, 0x0042),
        chars(0x0041, 0x00E9, 0x0042),
        chars(0x0041, 0x20AC, 0x0042),
        chars(0x0041, 0x00FF, 0x0042),
        chars(0x0041, 0x00C3, 0x0042),
        chars(0x0041, 0x0000, 0x0042),
        chars(0x0041, 0x002F, 0x0042),
    };
    static final char[][] MEASURED_STANDARD_UTF8 = {
        chars(0x0041, 0xD83D, 0xDE00, 0x0042),
        chars(0x0041, 0xFFFD, 0xFFFD, 0x0042),
        chars(0x0041, 0x00E9, 0x0042),
        chars(0x0041, 0x20AC, 0x0042),
        chars(0x0041, 0xFFFD, 0x0042),
        chars(0x0041, 0xFFFD, 0x0042),
        chars(0x0041, 0xFFFD, 0xFFFD, 0x0042),
        chars(0x0041, 0xFFFD, 0xFFFD, 0xFFFD, 0x0042),
    };

    /**
     * Rows derived from the count rule and {@code UTF8::next}: a stray continuation byte is consumed as a character
     * but not counted (so a character is lost at the end), an incomplete sequence yields its lead byte, and every
     * complete two- or three-byte sequence is accepted whatever it encodes.
     */
    private static final byte[][] DERIVED_INPUTS = {
        bytes(),
        bytes(0x00, 0x41),
        bytes(0x41, 0x00, 0x42),
        bytes(0x80),
        bytes(0x80, 0x41),
        bytes(0x41, 0x80),
        bytes(0xC3),
        bytes(0xE2, 0x82),
        bytes(0xE2, 0x82, 0x41),
        bytes(0xE2, 0x41, 0x82),
        bytes(0xE2, 0x82, 0xAC, 0x80),
        bytes(0xF0, 0x9F, 0x98, 0x80),
        bytes(0xF4, 0x90, 0x80, 0x80),
        bytes(0xED, 0xA0, 0x80),
        bytes(0xED, 0xB0, 0x80),
        bytes(0xC1, 0xBF),
        bytes(0xE0, 0x80, 0x80),
        bytes(0xDF, 0xBF),
        bytes(0xEF, 0xBF, 0xBF),
        bytes(0xC0, 0x80, 0xC0, 0x80),
        bytes(0xFE, 0xFF),
        bytes(0xC3, 0xA9, 0xC3),
        bytes(0x7F),
        bytes(0xC0, 0x41),
        bytes(0xC3, 0xC3, 0xA9),
        bytes(0xF8, 0x80, 0x80, 0x80, 0x80),
        bytes(0x41, 0x80, 0x80, 0x42, 0x43, 0x44),
    };
    private static final char[][] DERIVED_CHARS = {
        chars(),
        chars(),
        chars(0x0041),
        chars(),
        chars(0x0080),
        chars(0x0041),
        chars(0x00C3),
        chars(0x00E2),
        chars(0x00E2, 0x0082),
        chars(0x00E2, 0x0041),
        chars(0x20AC),
        chars(0x00F0),
        chars(0x00F4),
        chars(0xD800),
        chars(0xDC00),
        chars(0x007F),
        chars(0x0000),
        chars(0x07FF),
        chars(0xFFFF),
        chars(0x0000, 0x0000),
        chars(0x00FE, 0x00FF),
        chars(0x00E9, 0x00C3),
        chars(0x007F),
        chars(0x00C0, 0x0041),
        chars(0x00C3, 0x00E9),
        chars(0x00F8),
        chars(0x0041, 0x0080, 0x0080, 0x0042),
    };

    @Test
    public void measuredRowsDecodeAsTheJniNewStringUtfDid() {
        for (int i = 0; i < MEASURED_INPUTS.length; i++) {
            assertArrayEquals(MEASURED_CHARS[i], JniStringCodec.fromNewStringUtf(MEASURED_INPUTS[i]).toCharArray(),
                    "row " + i + " " + hex(MEASURED_INPUTS[i]));
        }
    }

    @Test
    public void measuredRowsDifferFromStandardUtf8WhereTheJniDid() {
        for (int i = 0; i < MEASURED_INPUTS.length; i++) {
            assertArrayEquals(MEASURED_STANDARD_UTF8[i],
                    new String(MEASURED_INPUTS[i], StandardCharsets.UTF_8).toCharArray(),
                    "row " + i + " " + hex(MEASURED_INPUTS[i]));
        }
    }

    @Test
    public void derivedRowsDecodeLikeHotSpot() {
        assertEquals(DERIVED_INPUTS.length, DERIVED_CHARS.length);
        for (int i = 0; i < DERIVED_INPUTS.length; i++) {
            assertArrayEquals(DERIVED_CHARS[i], JniStringCodec.fromNewStringUtf(DERIVED_INPUTS[i]).toCharArray(),
                    "row " + i + " " + hex(DERIVED_INPUTS[i]));
        }
    }

    @Test
    public void decodingStopsAtTheFirstNulByteOrTheEndOfTheArray() {
        assertEquals("A", JniStringCodec.fromNewStringUtf(bytes(0x41, 0x00, 0x42)));
        assertEquals("", JniStringCodec.fromNewStringUtf(bytes(0x00)));
        assertArrayEquals(chars(0x00C3), JniStringCodec.fromNewStringUtf(bytes(0xC3, 0x00, 0xA9)).toCharArray());
        assertEquals("ABC", JniStringCodec.fromNewStringUtf(bytes(0x41, 0x42, 0x43)));
    }

    @Test
    public void encodesNulAsTwoBytes() {
        assertArrayEquals(bytes(0x61, 0xC0, 0x80, 0x62, 0x00), JniStringCodec.toModifiedUtf8(string(0x61, 0, 0x62)));
        assertArrayEquals(bytes(0xC0, 0x80, 0x00), JniStringCodec.toModifiedUtf8(string(0)));
        assertArrayEquals(bytes(0x00), JniStringCodec.toModifiedUtf8(""));
    }

    @Test
    public void encodesSupplementaryCharactersAsTwoEncodedSurrogates() {
        String smile = new String(Character.toChars(0x1F600));
        assertArrayEquals(bytes(0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80, 0x00), JniStringCodec.toModifiedUtf8(smile));
        assertEquals(6, JniStringCodec.modifiedUtf8Length(smile));
        String last = new String(Character.toChars(0x10FFFF));
        assertArrayEquals(bytes(0xED, 0xAF, 0xBF, 0xED, 0xBF, 0xBF, 0x00), JniStringCodec.toModifiedUtf8(last));
    }

    @Test
    public void encodesUnpairedSurrogatesAsThreeBytesEach() {
        assertArrayEquals(bytes(0xED, 0xA0, 0x80, 0x00), JniStringCodec.toModifiedUtf8(string(0xD800)));
        assertArrayEquals(bytes(0xED, 0xB0, 0x80, 0x00), JniStringCodec.toModifiedUtf8(string(0xDC00)));
        assertArrayEquals(bytes(0x41, 0xED, 0xBF, 0xBF, 0x00), JniStringCodec.toModifiedUtf8(string(0x41, 0xDFFF)));
        assertArrayEquals(bytes(0xED, 0xB8, 0x80, 0xED, 0xA0, 0xBD, 0x00),
                JniStringCodec.toModifiedUtf8(string(0xDE00, 0xD83D)));
    }

    @Test
    public void encodesTheRangeBoundariesOfEachLength() {
        assertArrayEquals(bytes(0x01, 0x00), JniStringCodec.toModifiedUtf8(string(0x0001)));
        assertArrayEquals(bytes(0x7F, 0x00), JniStringCodec.toModifiedUtf8(string(0x007F)));
        assertArrayEquals(bytes(0xC2, 0x80, 0x00), JniStringCodec.toModifiedUtf8(string(0x0080)));
        assertArrayEquals(bytes(0xDF, 0xBF, 0x00), JniStringCodec.toModifiedUtf8(string(0x07FF)));
        assertArrayEquals(bytes(0xE0, 0xA0, 0x80, 0x00), JniStringCodec.toModifiedUtf8(string(0x0800)));
        assertArrayEquals(bytes(0xEF, 0xBF, 0xBF, 0x00), JniStringCodec.toModifiedUtf8(string(0xFFFF)));
        assertArrayEquals(bytes(0x63, 0x61, 0x66, 0xC3, 0xA9, 0x00),
                JniStringCodec.toModifiedUtf8(string(0x63, 0x61, 0x66, 0xE9)));
        assertArrayEquals(bytes(0xE2, 0x82, 0xAC, 0x00), JniStringCodec.toModifiedUtf8(string(0x20AC)));
    }

    @Test
    public void modifiedUtf8LengthIsTheLengthWithoutTheTerminator() {
        assertEquals(0, JniStringCodec.modifiedUtf8Length(""));
        assertEquals(2, JniStringCodec.modifiedUtf8Length(string(0)));
        assertEquals(1, JniStringCodec.modifiedUtf8Length("A"));
        assertEquals(5, JniStringCodec.modifiedUtf8Length(string(0x63, 0x61, 0x66, 0xE9)));
        assertEquals(9, JniStringCodec.modifiedUtf8Length(string(0x65E5, 0x672C, 0x8A9E)));
        Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            String s = randomCodeUnits(random, 40);
            assertEquals(JniStringCodec.toModifiedUtf8(s).length - 1, JniStringCodec.modifiedUtf8Length(s), s);
        }
    }

    @Test
    public void encodingMatchesDataOutputStreamWriteUtf() {
        Random random = new Random(1);
        for (int i = 0; i < 500; i++) {
            String s = randomCodeUnits(random, 64);
            byte[] payload = writeUtfPayload(s);
            byte[] encoded = JniStringCodec.toModifiedUtf8(s);
            assertArrayEquals(payload, Arrays.copyOf(encoded, encoded.length - 1), "string " + i + " " + hex(payload));
            assertEquals(0, encoded[encoded.length - 1], "terminator of string " + i);
            assertEquals(s, readUtf(payload), "readUTF of string " + i);
        }
    }

    @Test
    public void decodingInvertsEncodingForEveryString() {
        assertEquals("", JniStringCodec.fromNewStringUtf(JniStringCodec.toModifiedUtf8("")));
        String[] fixed = {
            "DejaVu Sans", string(0x63, 0x61, 0x66, 0xE9), string(0x20AC), string(0x65E5, 0x672C, 0x8A9E),
            string(0x61, 0, 0x62), string(0xD800), string(0xDC00, 0x41), new String(Character.toChars(0x1F600)),
            string(0x0001, 0x007F, 0x0080, 0x07FF, 0x0800, 0xFFFF, 0xD7FF, 0xE000),
        };
        for (String s : fixed) {
            assertEquals(s, JniStringCodec.fromNewStringUtf(JniStringCodec.toModifiedUtf8(s)),
                    hex(s.getBytes(StandardCharsets.UTF_16BE)));
        }
        Random random = new Random(2);
        for (int i = 0; i < 1000; i++) {
            String s = randomCodeUnits(random, 48);
            byte[] encoded = JniStringCodec.toModifiedUtf8(s);
            assertEquals(s, JniStringCodec.fromNewStringUtf(encoded), "string " + i + " " + hex(encoded));
            assertEquals(s, readUtf(Arrays.copyOf(encoded, encoded.length - 1)), "readUTF of string " + i);
        }
    }

    @Test
    public void segmentViewDecodesUpToTheTerminatorWithinItsBounds() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment view = arena.allocateFrom(JAVA_BYTE, bytes(0x41, 0xC3, 0xA9, 0x42, 0x00, 0x58, 0x59, 0x00));
            assertArrayEquals(chars(0x0041, 0x00E9, 0x0042), JniStringCodec.fromNewStringUtf(view).toCharArray());
            assertArrayEquals(chars(0x0041, 0x00E9, 0x0042),
                    JniStringCodec.fromNewStringUtf(view.asSlice(0, 5)).toCharArray());
            assertEquals("", JniStringCodec.fromNewStringUtf(view.asSlice(4)));
            for (int i = 0; i < MEASURED_INPUTS.length; i++) {
                MemorySegment row = arena.allocateFrom(JAVA_BYTE, Arrays.copyOf(MEASURED_INPUTS[i],
                        MEASURED_INPUTS[i].length + 1));
                assertArrayEquals(MEASURED_CHARS[i], JniStringCodec.fromNewStringUtf(row).toCharArray(), "row " + i);
            }
            assertNull(JniStringCodec.fromNewStringUtf(MemorySegment.NULL));
            IllegalArgumentException tooSmall = assertThrows(IllegalArgumentException.class,
                    () -> JniStringCodec.fromNewStringUtf(view.asSlice(0, 4)));
            assertTrue(tooSmall.getMessage().startsWith("no terminator within the 4 bytes"), tooSmall.getMessage());
            MemorySegment unterminated = arena.allocateFrom(JAVA_BYTE, bytes(0x41, 0x42, 0x43));
            assertThrows(IllegalArgumentException.class, () -> JniStringCodec.fromNewStringUtf(unterminated));
        }
    }

    @Test
    public void allocateModifiedUtf8WritesTheTerminatedCString() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cString = JniStringCodec.allocateModifiedUtf8(arena, string(0x61, 0, 0xE9, 0xD800));
            assertArrayEquals(bytes(0x61, 0xC0, 0x80, 0xC3, 0xA9, 0xED, 0xA0, 0x80, 0x00), cString.toArray(JAVA_BYTE));
            assertEquals(string(0x61, 0, 0xE9, 0xD800), JniStringCodec.fromNewStringUtf(cString));
            assertEquals(1, JniStringCodec.allocateModifiedUtf8(arena, "").byteSize());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers shared with JniStringOracleTest
    // ---------------------------------------------------------------------------------------------

    static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    static char[] chars(int... values) {
        char[] chars = new char[values.length];
        for (int i = 0; i < values.length; i++) {
            chars[i] = (char) values[i];
        }
        return chars;
    }

    static String string(int... codeUnits) {
        return new String(chars(codeUnits));
    }

    static String hex(byte[] bytes) {
        return HexFormat.ofDelimiter(" ").withUpperCase().formatHex(bytes);
    }

    /**
     * Up to {@code maxLength} code units drawn from every class the encoder distinguishes: {@code U+0000}, ASCII,
     * Latin-1, two-byte, three-byte, valid surrogate pairs and unpaired high and low surrogates.
     */
    static String randomCodeUnits(Random random, int maxLength) {
        int length = random.nextInt(maxLength + 1);
        StringBuilder s = new StringBuilder(length + 1);
        while (s.length() < length) {
            switch (random.nextInt(9)) {
                case 0 -> s.append((char) 0);
                case 1 -> s.append((char) (0x01 + random.nextInt(0x7F)));
                case 2 -> s.append((char) (0x80 + random.nextInt(0x80)));
                case 3 -> s.append((char) (0x100 + random.nextInt(0x700)));
                case 4 -> s.append((char) (0x800 + random.nextInt(0xD800 - 0x800)));
                case 5 -> s.append((char) (0xE000 + random.nextInt(0x2000)));
                case 6 -> s.appendCodePoint(0x10000 + random.nextInt(0x100000));
                case 7 -> s.append((char) (0xD800 + random.nextInt(0x400)));
                default -> s.append((char) (0xDC00 + random.nextInt(0x400)));
            }
        }
        return s.toString();
    }

    /** The bytes {@code DataOutputStream.writeUTF} writes after its two-byte length: modified UTF-8. */
    static byte[] writeUtfPayload(String s) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] written = bytes.toByteArray();
        return Arrays.copyOfRange(written, 2, written.length);
    }

    /** {@code DataInputStream.readUTF} of a modified UTF-8 payload: the JDK's strict decoder. */
    static String readUtf(byte[] payload) {
        byte[] framed = new byte[payload.length + 2];
        framed[0] = (byte) (payload.length >> 8);
        framed[1] = (byte) payload.length;
        System.arraycopy(payload, 0, framed, 2, payload.length);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(framed))) {
            return in.readUTF();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
