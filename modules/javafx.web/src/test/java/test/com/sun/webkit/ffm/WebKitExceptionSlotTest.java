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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.DOMException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The thread-local exception slot (contract section 2.2, as amended by section 13).
 * <p>
 * The slot replaces the JNI pending exception. C never throws: it writes a kind, a code and an
 * inline message into the calling thread's slot, and Java reads that from memory after a fallible
 * call. The design's whole justification is that the common case costs no downcall at all, so this
 * class asserts the cost as well as the behaviour.
 * <p>
 * All four kinds decode to {@code org.w3c.dom.DOMException}. The JNI enum named an event and a range
 * exception too, but no C code ever selected them: every raise path goes through
 * {@code raiseDOMErrorException}, so throwing an {@code EventException} here would invent behaviour
 * the bindings never had (contract section 13.1, finding 8).
 */
@Tag("ffm")
public class WebKitExceptionSlotTest {

    private static final long PEER = 0x0BADC0DE03L;

    private static final int WKJ_EXC_DOM = 1;
    private static final int WKJ_EXC_EVENT = 2;
    private static final int WKJ_EXC_RANGE = 3;
    private static final int WKJ_EXC_UNDEFINED = 4;

    private static final short UNIT_TYPE = 3;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void resetRecording() {
        WkjStubShim.reset();
    }

    @AfterEach
    void leaveTheSlotClean() {
        WkjStubShim.reset();
    }

    @Test
    public void aCleanSlotCostsNoExtraDowncall() {
        long before = WkjStubShim.callTotal();

        assertDoesNotThrow(() -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertEquals(before + 1, WkjStubShim.callTotal(),
                "the no-exception path must read the slot from memory and make no second call;"
                        + " a facade that called a helper to check would still pass every other test");
        assertEquals(0, WkjStubShim.exceptionPending());
    }

    @ParameterizedTest(name = "WKJ_EXC type {0}")
    @ValueSource(ints = { WKJ_EXC_DOM, WKJ_EXC_EVENT, WKJ_EXC_RANGE, WKJ_EXC_UNDEFINED })
    public void everyKnownKindBecomesADomException(int type) {
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, type, 8, "index size error");

        DOMException thrown = assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertEquals((short) 8, thrown.code);
        assertEquals("index size error", thrown.getMessage());
    }

    @Test
    public void anUnknownKindIsADefinedOutcomeThatNamesIt() {
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, 99, 4, "from the future");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertTrue(thrown.getMessage().contains("99"),
                "the failure must name the unknown kind: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("from the future"),
                "the failure must carry the message: " + thrown.getMessage());
    }

    @Test
    public void aVoidFunctionThrowsRatherThanLeavingItInTheSlot() {
        // 41 of the 116 throwing DOM functions return void. A missed check there would both swallow
        // the exception and leave the slot dirty, so the next unrelated call would throw someone
        // else's exception (contract section 13.1, finding 12).
        WkjStubShim.armException(DomFacadeShim.ELEMENT_SET_ATTRIBUTE, WKJ_EXC_DOM, 7, "no modify");

        DOMException thrown = assertThrows(DOMException.class,
                () -> DomFacadeShim.elementSetAttribute(PEER, "id", "x"));

        assertEquals((short) 7, thrown.code);
        assertEquals("no modify", thrown.getMessage());
        assertEquals(0, WkjStubShim.exceptionPending(), "the slot must be clean after the throw");
    }

    @Test
    public void aNonAsciiMessageSurvives() {
        String message = "\u65E5\u672C\u8A9E";
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, WKJ_EXC_DOM, 12, message);

        DOMException thrown = assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertEquals(message, thrown.getMessage());
    }

    @Test
    public void anEmptyMessageIsEmptyAndNotNull() {
        // The slot carries a length, not a NUL terminated pointer, so there is no null message on
        // this ABI: an absent message is a zero length one.
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, WKJ_EXC_DOM, 1, "");

        DOMException thrown = assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertEquals("", thrown.getMessage());
    }

    @Test
    public void aMessageIsTruncatedToTheCapacityTheCStructDeclares() {
        int struct = WkjStubShim.findStruct("WKJExceptionSlot");
        int field = WkjStubShim.findStructField(struct, "message");
        int capacity = WkjStubShim.structFieldElements(struct, field);
        assertTrue(capacity > 0);

        String message = "m".repeat(capacity + 44);
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, WKJ_EXC_DOM, 1, message);

        DOMException thrown = assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        // The bound comes from the C table rather than a 256 literal duplicated in Java: the point
        // of the assertion is that the two agree.
        assertEquals(capacity, thrown.getMessage().length());
        assertEquals(message.substring(0, capacity), thrown.getMessage());
    }

    @Test
    public void noTailOfALongMessageLeaksIntoAShortOne() {
        // Specific to an inline buffer, and new: while the message was a pointer this could not
        // happen. message_length is authoritative, and the buffer is not NUL terminated.
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, WKJ_EXC_DOM, 1, "L".repeat(200));
        assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, WKJ_EXC_DOM, 1, "xyz");
        DOMException second = assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertEquals("xyz", second.getMessage());
        assertEquals(3, second.getMessage().length());
    }

    @Test
    public void theSlotIsClearedAfterAThrowAndTheNextCallIsClean() {
        WkjStubShim.armException(DomFacadeShim.CSS_GET_FLOAT_VALUE, WKJ_EXC_DOM, 5, "once");
        assertThrows(DOMException.class,
                () -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE));

        assertEquals(0, WkjStubShim.exceptionPending());
        assertDoesNotThrow(() -> DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE),
                "the exception must be delivered once, not on every later call");
    }

    @Test
    public void theLibraryClearsTheSlotOnEntryToEveryCall() {
        // Guaranteed by the C side, and what bounds a missed check to the call that caused it.
        WkjStubShim.raise(WKJ_EXC_DOM, 3, "left over");
        assertEquals(WKJ_EXC_DOM, WkjStubShim.exceptionPending());

        DomFacadeShim.attrSetValue(PEER, "unrelated");

        assertEquals(0, WkjStubShim.exceptionPending(),
                "an unrelated wkj_* call must have cleared the slot on entry");
    }

    @Test
    public void theSlotIsPerThread() throws InterruptedException {
        WkjStubShim.raise(WKJ_EXC_DOM, 9, "belongs to this thread");
        assertEquals(WKJ_EXC_DOM, WkjStubShim.exceptionPending());

        AtomicReference<Throwable> onOther = new AtomicReference<>();
        AtomicReference<Integer> pendingOnOther = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                DomFacadeShim.cssPrimitiveValueGetFloatValue(PEER, UNIT_TYPE);
            } catch (Throwable t) {
                onOther.set(t);
            }
            pendingOnOther.set(WkjStubShim.exceptionPending());
        }, "wkj-slot-other");
        other.start();
        other.join();

        assertNull(onOther.get(), "the other thread must not see this thread's pending exception");
        assertEquals(0, pendingOnOther.get());
        assertEquals(WKJ_EXC_DOM, WkjStubShim.exceptionPending(),
                "this thread's slot must be untouched by the other thread's call");
    }

    @Test
    public void aFunctionThatCannotRaiseIsNotChecked() {
        // wkj_dom_Attr_setValue is not marked THROWS in the spec, so the facade does not check the
        // slot after it. If this fails the facade is checking every call, which is a performance
        // defect rather than a correctness one, but it is still a defect.
        WkjStubShim.armException(DomFacadeShim.ATTR_SET_VALUE, WKJ_EXC_DOM, 1, "should not surface");

        assertDoesNotThrow(() -> DomFacadeShim.attrSetValue(PEER, "value"),
                "a facade that checks a non-throwing function pays for the check 1707 times over");

        assertEquals(WKJ_EXC_DOM, WkjStubShim.exceptionPending(),
                "the slot is left set, and the next wkj_* call clears it on entry");
    }
}
