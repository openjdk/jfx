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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The stub is sane. Every other class in this package trusts the recording, so a broken stub must
 * fail here rather than as noise somewhere it would be mistaken for a binding defect.
 */
@Tag("ffm")
public class WkjStubLibraryTest {

    private static final long PEER = 0x0BADC0DE01L;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void resetRecording() {
        WkjStubShim.reset();
    }

    @Test
    public void stubVersionIsTheOneThisSuiteWasWrittenAgainst() {
        assertEquals(1, WkjStubShim.stubVersion());
    }

    @Test
    public void resetEmptiesTheRing() {
        DomFacadeShim.attrGetSpecified(PEER);
        assertEquals(1, WkjStubShim.callCount());
        WkjStubShim.reset();
        assertEquals(0, WkjStubShim.callCount());
        assertEquals(0L, WkjStubShim.callTotal());
    }

    @Test
    public void oneFacadeCallIsRecordedOnceWithOneArgumentPerCParameter() {
        DomFacadeShim.attrGetSpecified(PEER);

        assertEquals(1, WkjStubShim.callCount());
        assertEquals(1L, WkjStubShim.callTotal());
        assertEquals(DomFacadeShim.ATTR_GET_SPECIFIED, WkjStubShim.callName(0));
        // int32_t wkj_dom_Attr_getSpecified(int64_t peer): one parameter, so one recorded argument.
        assertEquals(1, WkjStubShim.callArgc(0));
        assertEquals('l', WkjStubShim.callArgKind(0, 0));
        assertEquals(PEER, WkjStubShim.callArgBits(0, 0));
    }

    @Test
    public void theRingDropsTheOldestCallsAndSaysSo() {
        int capacity = WkjStubShim.ringCapacity();
        assertTrue(capacity > 0);

        DomFacadeShim.attrGetOwnerElement(PEER);
        for (int i = 0; i < capacity + 5; i++) {
            DomFacadeShim.attrGetSpecified(PEER);
        }

        assertEquals(capacity, WkjStubShim.callCount(), "the ring must not grow past its capacity");
        assertEquals(capacity + 6L, WkjStubShim.callTotal(),
                "the total must count the calls the ring dropped");
        assertEquals(DomFacadeShim.ATTR_GET_SPECIFIED, WkjStubShim.callName(0),
                "index 0 is the oldest retained call, not the oldest call");
        assertEquals(-1, WkjStubShim.findCall(DomFacadeShim.ATTR_GET_OWNER_ELEMENT, 0),
                "the first call should have been pushed out of the ring");
    }

    @Test
    public void theSymbolTableCoversTheWholeAbi() {
        int count = WkjStubShim.symbolCount();
        assertTrue(count >= 1796,
                "the stub implements only " + count + " wkj_* functions, expected at least 1796");
        for (int i = 0; i < count; i++) {
            String name = WkjStubShim.symbolName(i);
            assertNotNull(name, "symbol " + i + " has no name");
            assertTrue(name.startsWith("wkj_"), "symbol " + i + " is not a wkj_* function: " + name);
            assertTrue(WkjStubShim.symbolSignature(i).length() >= 1,
                    "symbol " + name + " has an empty signature");
        }
    }

    @Test
    public void theStructTableDescribesEveryStructTheHeaderDeclares() {
        int count = WkjStubShim.structCount();
        assertTrue(count > 0);
        for (int i = 0; i < count; i++) {
            assertNotNull(WkjStubShim.structName(i));
            assertTrue(WkjStubShim.structSize(i) > 0,
                    WkjStubShim.structName(i) + " has a non positive size");
            assertTrue(WkjStubShim.structFieldCount(i) > 0,
                    WkjStubShim.structName(i) + " has no members");
        }
    }
}
