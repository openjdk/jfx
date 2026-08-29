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

import com.sun.webkit.WkjStubShim;
import com.sun.webkit.dom.DomFacadeShim;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * One real downcall per shape in the DOM ABI, so that every code path the facade generator emits is
 * executed at least once against a real library.
 * <p>
 * The values are chosen for the places a sloppy conversion hides: {@code Long.MIN_VALUE} for a peer,
 * {@code Integer.MIN_VALUE} for a flag, and {@code -0.0} and {@code NaN} for the floating point
 * returns, which travel as raw bit patterns and compare equal to nothing under {@code ==}.
 */
@Tag("ffm")
public class WebKitDomFacadeTest {

    private static final long PEER = 0x0BADC0DE05L;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void resetRecording() {
        WkjStubShim.reset();
    }

    @Test
    public void voidOfLong() {
        DomFacadeShim.nodeDispose(Long.MIN_VALUE);

        assertEquals(1, WkjStubShim.callCount());
        assertEquals(DomFacadeShim.NODE_DISPOSE, WkjStubShim.callName(0));
        assertEquals(1, WkjStubShim.callArgc(0));
        assertEquals('l', WkjStubShim.callArgKind(0, 0));
        assertEquals(Long.MIN_VALUE, WkjStubShim.callArgBits(0, 0));
    }

    @Test
    public void int32OfLongDecodedAsBoolean() {
        WkjStubShim.setReturnLong(DomFacadeShim.ATTR_GET_SPECIFIED, 0L);
        assertFalse(DomFacadeShim.attrGetSpecified(PEER));

        WkjStubShim.setReturnLong(DomFacadeShim.ATTR_GET_SPECIFIED, 1L);
        assertTrue(DomFacadeShim.attrGetSpecified(PEER));

        // Any non zero int32_t is true, exactly as the JNI jboolean conversion behaved once the
        // value had been widened; narrowing it to a byte first is how 256 became false.
        WkjStubShim.setReturnLong(DomFacadeShim.ATTR_GET_SPECIFIED, Integer.MIN_VALUE);
        assertTrue(DomFacadeShim.attrGetSpecified(PEER));

        WkjStubShim.setReturnLong(DomFacadeShim.ATTR_GET_SPECIFIED, 256L);
        assertTrue(DomFacadeShim.attrGetSpecified(PEER));
    }

    @Test
    public void int64OfLong() {
        WkjStubShim.setReturnLong(DomFacadeShim.ATTR_GET_OWNER_ELEMENT, Long.MIN_VALUE);

        assertEquals(Long.MIN_VALUE, DomFacadeShim.attrGetOwnerElement(PEER));
        assertEquals(PEER, WkjStubShim.callArgBits(0, 0));
    }

    @Test
    public void int16OfLong() {
        WkjStubShim.setReturnLong(DomFacadeShim.NODE_GET_NODE_TYPE, Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, DomFacadeShim.nodeGetNodeType(PEER));

        WkjStubShim.setReturnLong(DomFacadeShim.NODE_GET_NODE_TYPE, 1L);
        assertEquals((short) 1, DomFacadeShim.nodeGetNodeType(PEER));
    }

    @Test
    public void floatOfLongAndShort() {
        short unitType = Short.MAX_VALUE;

        WkjStubShim.setReturnDouble(DomFacadeShim.CSS_GET_FLOAT_VALUE, 1.5);
        assertEquals(1.5f, DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, unitType));
        assertEquals('h', WkjStubShim.callArgKind(0, 1), "the unit type is an int16_t");
        assertEquals(unitType, (short) WkjStubShim.callArgBits(0, 1));

        WkjStubShim.setReturnDouble(DomFacadeShim.CSS_GET_FLOAT_VALUE, -0.0);
        assertEquals(Float.floatToRawIntBits(-0.0f),
                Float.floatToRawIntBits(DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, unitType)),
                "negative zero must survive as negative zero, not as zero");

        WkjStubShim.setReturnDouble(DomFacadeShim.CSS_GET_FLOAT_VALUE, Double.NaN);
        assertTrue(Float.isNaN(DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, unitType)));
    }

    @Test
    public void doubleOfLong() {
        WkjStubShim.setReturnDouble(DomFacadeShim.XPATH_GET_NUMBER_VALUE, Math.PI);
        assertEquals(Math.PI, DomFacadeShim.xpathResultGetNumberValue(PEER));

        WkjStubShim.setReturnDouble(DomFacadeShim.XPATH_GET_NUMBER_VALUE, -0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(DomFacadeShim.xpathResultGetNumberValue(PEER)));

        WkjStubShim.setReturnDouble(DomFacadeShim.XPATH_GET_NUMBER_VALUE, Double.NaN);
        assertTrue(Double.isNaN(DomFacadeShim.xpathResultGetNumberValue(PEER)));
    }

    @Test
    public void stringOutThroughTheCallersBuffer() {
        WkjStubShim.setReturnString(DomFacadeShim.ATTR_GET_NAME, "class");

        assertEquals("class", DomFacadeShim.attrGetName(PEER));
        // int32_t wkj_dom_Attr_getName(int64_t, uint16_t* buf, int32_t cap, int32_t* len)
        assertEquals(4, WkjStubShim.callArgc(0));
        assertEquals('l', WkjStubShim.callArgKind(0, 0));
        assertEquals('p', WkjStubShim.callArgKind(0, 1));
        assertEquals('i', WkjStubShim.callArgKind(0, 2));
        assertEquals('p', WkjStubShim.callArgKind(0, 3));
        assertFalse(WkjStubShim.callArgIsNull(0, 1), "the caller provides the buffer");
        assertFalse(WkjStubShim.callArgIsNull(0, 3), "and the cell the length comes back in");
    }

    @Test
    public void stringIn() {
        DomFacadeShim.attrSetValue(PEER, "value");

        assertEquals(3, WkjStubShim.callArgc(0));
        assertEquals(PEER, WkjStubShim.callArgBits(0, 0));
        assertEquals("value", WkjStubShim.callArgString(0, 1));
        assertEquals(5, (int) WkjStubShim.callArgBits(0, 2));
    }

    @Test
    public void threeStringPairsInOneCall() {
        WkjStubShim.setReturnLong(DomFacadeShim.CREATE_DOCUMENT_TYPE, 0x1234L);

        long created = DomFacadeShim.domImplementationCreateDocumentType(PEER, "html", "", null);

        assertEquals(0x1234L, created);
        // int64_t (int64_t, const uint16_t*, int32_t, const uint16_t*, int32_t, const uint16_t*,
        //          int32_t): the pairs must stay paired and in order.
        assertEquals(7, WkjStubShim.callArgc(0));
        assertEquals("html", WkjStubShim.callArgString(0, 1));
        assertEquals(4, (int) WkjStubShim.callArgBits(0, 2));
        assertEquals("", WkjStubShim.callArgString(0, 3));
        assertEquals(0, (int) WkjStubShim.callArgBits(0, 4));
        assertTrue(WkjStubShim.callArgIsNull(0, 5), "the null systemId stays null");
        assertEquals(0, (int) WkjStubShim.callArgBits(0, 6));
    }

    @Test
    public void aStringGetterThatOverflowsStillProducesTheWholeValue() {
        String value = "z".repeat(DomFacadeShim.stringCapacity() * 3);
        WkjStubShim.setReturnString(DomFacadeShim.NODE_GET_TEXT_CONTENT, value);

        assertEquals(value, DomFacadeShim.nodeGetTextContent(PEER));
        assertEquals(2, WkjStubShim.countCalls(DomFacadeShim.NODE_GET_TEXT_CONTENT));
    }
}
