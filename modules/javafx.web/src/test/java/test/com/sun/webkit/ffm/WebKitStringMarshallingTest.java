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

package test.com.sun.webkit.ffm;

import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import com.sun.webkit.dom.DomFacadeShim;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * UTF-16 marshalling in both directions, through real downcalls.
 * <p>
 * Inbound, the pair is {@code const uint16_t* s, int32_t s_len}, and the one thing observable at the
 * boundary is that a Java {@code null} arrives as a {@code NULL} pointer while {@code ""} arrives as
 * a valid pointer of length zero. The library collapses both to the empty {@code WTF::String}, which
 * is what the JNI constructor always did, but the facade must still make the distinction on the wire
 * (contract section 11.1).
 * <p>
 * Outbound, the string comes back through a buffer this side owns, with a {@code WKJ_STR_*} status,
 * so the facade's allocate, grow and retry logic is as much on test here as the encoding is.
 * <p>
 * Every non-ASCII value below is written as an escape, so this file stays ASCII.
 * <p>
 * There is no "larger than 2 GB" case: an input longer than {@code Integer.MAX_VALUE} code units is
 * not representable as a Java {@code String}, so the array size limit of the marshalling patterns
 * cannot arise on this ABI.
 */
@Tag("ffm")
public class WebKitStringMarshallingTest {

    private static final long PEER = 0x0BADC0DE02L;

    /** Past any assumption that a DOM string fits in a 64 KiB buffer. */
    private static final int LONG_LENGTH = 70_000;

    /** Three Japanese characters, none of them representable in one byte. */
    private static final String NON_ASCII = "\u65E5\u672C\u8A9E";

    /** Three code units, the middle one a NUL: the hazard modified UTF-8 had. */
    private static final String EMBEDDED_NUL = "a\0b";

    /** One code point, two code units. */
    private static final String SURROGATE_PAIR = "\uD83D\uDE00";

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void resetRecording() {
        WkjStubShim.reset();
    }

    static Stream<Arguments> inputs() {
        return Stream.of(
                Arguments.of("null", null),
                Arguments.of("empty", ""),
                Arguments.of("ascii", "abc"),
                Arguments.of("non-ascii", NON_ASCII),
                Arguments.of("embedded NUL", EMBEDDED_NUL),
                Arguments.of("surrogate pair", SURROGATE_PAIR),
                Arguments.of("lone high surrogate", "\uD83D"),
                Arguments.of("lone low surrogate", "\uDE00"),
                Arguments.of("long", "x".repeat(LONG_LENGTH)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputs")
    public void stringArgumentsArriveExactly(String label, String value) {
        DomFacadeShim.attrSetValue(PEER, value);

        assertEquals(1, WkjStubShim.callCount());
        assertEquals(DomFacadeShim.ATTR_SET_VALUE, WkjStubShim.callName(0));
        // void wkj_dom_Attr_setValue(int64_t peer, const uint16_t* value, int32_t value_length)
        assertEquals(3, WkjStubShim.callArgc(0));
        assertEquals('s', WkjStubShim.callArgKind(0, 1), "argument 1 is a UTF-16 string");
        assertEquals(value, WkjStubShim.callArgString(0, 1), "the value did not survive the boundary");
        assertEquals(value == null ? 0 : value.length(), (int) WkjStubShim.callArgBits(0, 2),
                "the length argument is in UTF-16 code units");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputs")
    public void onlyNullArrivesAsANullPointer(String label, String value) {
        DomFacadeShim.attrSetValue(PEER, value);

        assertEquals(value == null, WkjStubShim.callArgIsNull(0, 1),
                "a Java null must arrive as NULL and the empty string as a valid pointer of length"
                        + " zero; that is the only place the two are distinguishable inbound");
    }

    @Test
    public void theEmptyStringIsNotNullOnTheWayIn() {
        DomFacadeShim.attrSetValue(PEER, "");
        assertFalse(WkjStubShim.callArgIsNull(0, 1));
        assertEquals("", WkjStubShim.callArgString(0, 1));
        assertEquals(0, (int) WkjStubShim.callArgBits(0, 2));

        WkjStubShim.reset();
        DomFacadeShim.attrSetValue(PEER, null);
        assertTrue(WkjStubShim.callArgIsNull(0, 1));
        assertNull(WkjStubShim.callArgString(0, 1));
        assertEquals(0, (int) WkjStubShim.callArgBits(0, 2));
    }

    @Test
    public void anEmbeddedNulSurvivesAsThreeCodeUnits() {
        DomFacadeShim.attrSetValue(PEER, EMBEDDED_NUL);

        // Modified UTF-8 would have stopped at the NUL or encoded it as two bytes. UTF-16 with an
        // explicit length has neither problem, and this asserts it rather than assuming it.
        assertEquals(3, (int) WkjStubShim.callArgBits(0, 2));
        assertEquals(EMBEDDED_NUL, WkjStubShim.callArgString(0, 1));
        assertEquals(6, WkjStubShim.callArgBytes(0, 1).length);
    }

    @Test
    public void aSurrogatePairIsTwoCodeUnitsAndOneCodePoint() {
        assertEquals(2, SURROGATE_PAIR.length());
        assertEquals(1, SURROGATE_PAIR.codePointCount(0, SURROGATE_PAIR.length()));

        DomFacadeShim.attrSetValue(PEER, SURROGATE_PAIR);

        assertEquals(2, (int) WkjStubShim.callArgBits(0, 2),
                "the length argument counts UTF-16 code units, not code points");
        assertEquals(SURROGATE_PAIR, WkjStubShim.callArgString(0, 1));
    }

    @Test
    public void aLoneSurrogatePassesThroughUnchanged() {
        // The DOM can hold one, and a codec that "fixes" it into U+FFFD is a behaviour change.
        for (String value : new String[] { "\uD83D", "\uDE00" }) {
            WkjStubShim.reset();
            DomFacadeShim.attrSetValue(PEER, value);
            String seen = WkjStubShim.callArgString(0, 1);
            assertEquals(value, seen);
            assertEquals(value.charAt(0), seen.charAt(0), "no replacement character was substituted");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputs")
    public void programmedValuesComeBackExactly(String label, String value) {
        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, value);

        assertEquals(value, DomFacadeShim.attrGetName(PEER));
    }

    @Test
    public void nullComesBackAsNullAndEmptyAsEmpty() {
        // WKJ_STR_NULL is Java null, WKJ_STR_OK with length 0 is "". DOMTest asserts the difference
        // through Document.getDocumentURI, so collapsing them here would break a shipped API.
        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, null);
        assertNull(DomFacadeShim.attrGetName(PEER), "WKJ_STR_NULL must be Java null, not the empty"
                + " string");

        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, "");
        String empty = DomFacadeShim.attrGetName(PEER);
        assertNotNull(empty, "WKJ_STR_OK with length 0 must be the empty string, not null");
        assertEquals("", empty);
    }

    @Test
    public void overflowGrowsOnceAndRetriesWithABigEnoughBuffer() {
        int capacity = DomFacadeShim.stringCapacity();
        String value = "y".repeat(capacity + 37);
        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, value);

        assertEquals(value, DomFacadeShim.attrGetName(PEER),
                "the whole string must come back, not the prefix that fitted");

        assertEquals(2, WkjStubShim.countCalls(DomFacadeShim.ATTR_GET_NAME),
                "the facade must call the symbol exactly twice: once to learn the size, once to"
                        + " fill a buffer of that size");
        // int32_t wkj_dom_Attr_getName(int64_t, uint16_t* buf, int32_t cap, int32_t* len)
        assertEquals(capacity, (int) WkjStubShim.callArgBits(0, 2),
                "the first attempt offers the facade's standard capacity");
        assertTrue(WkjStubShim.callArgBits(1, 2) >= value.length(),
                "the retry must offer at least the capacity the library asked for, or it would"
                        + " overflow again and loop");
    }

    @Test
    public void aValueThatFitsIsFetchedInOneCall() {
        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, "short");

        assertEquals("short", DomFacadeShim.attrGetName(PEER));
        assertEquals(1, WkjStubShim.countCalls(DomFacadeShim.ATTR_GET_NAME),
                "a value that fits must not cost a second downcall");
    }

    @Test
    public void anEmptyValueFitsInAZeroCapacityBuffer() {
        // Reached through the raw call, because the facade always offers its standard capacity. A
        // library that answered OVERFLOW here would make every empty string cost two downcalls.
        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, "");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment length = arena.allocate(JAVA_INT);
            MemorySegment buffer = arena.allocate(JAVA_CHAR);
            int status = WkjStubShim.callAttrGetNameRaw(PEER, buffer, 0, length);

            assertEquals(0, status, "an empty value is WKJ_STR_OK, not WKJ_STR_OVERFLOW");
            assertEquals(0, length.get(JAVA_INT, 0L));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputs")
    public void theCodecRoundTripsWithoutADowncall(String label, String value) {
        assertEquals(value, WebKitNativeShim.toNativeAndBack(value));
    }
}
