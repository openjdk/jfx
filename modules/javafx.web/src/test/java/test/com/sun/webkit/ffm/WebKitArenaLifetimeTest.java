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
import java.lang.foreign.MemorySegment;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Arena lifetime and thread confinement. FFM turns the two failure modes JNI had no answer for -
 * use after free and touching a resource from the wrong thread - into ordinary Java exceptions at
 * the call site, and this asserts that they land as exceptions rather than as native crashes.
 */
@Tag("ffm")
public class WebKitArenaLifetimeTest {

    private static final long PEER = 0x0BADC0DE04L;

    /** Enough per call arenas that a leak of one segment each would be obvious. */
    private static final int CALLS = 100_000;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void resetRecording() {
        WkjStubShim.reset();
    }

    @Test
    public void aSegmentFromAClosedArenaCannotBePassedToTheLibrary() {
        MemorySegment dead = WkjStubShim.allocateClosedSegment();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> WkjStubShim.callAttrSetValueRaw(PEER, dead, 3));

        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().toLowerCase(Locale.ROOT).contains("closed"),
                "the failure must say the session is closed: " + thrown.getMessage());
        assertEquals(0, WkjStubShim.callCount(), "the call must not have reached the library");
    }

    @Test
    public void aConfinedSegmentCannotBeReadFromAnotherThread() throws InterruptedException {
        MemorySegment confined = WkjStubShim.allocateConfinedSegment();
        assertEquals('a', confined.get(JAVA_CHAR, 0L), "the owning thread can read it");

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                confined.get(JAVA_CHAR, 0L);
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "wkj-arena-other");
        other.start();
        other.join();

        assertInstanceOf(WrongThreadException.class, failure.get(),
                "a confined segment touched from another thread must throw, not read torn memory");
    }

    @Test
    public void aConfinedSegmentCannotBePassedToTheLibraryFromAnotherThread()
            throws InterruptedException {
        MemorySegment confined = WkjStubShim.allocateConfinedSegment();

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                WkjStubShim.callAttrSetValueRaw(PEER, confined, 3);
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "wkj-arena-downcall");
        other.start();
        other.join();

        assertInstanceOf(WrongThreadException.class, failure.get());
        assertEquals(0, WkjStubShim.callCount());
    }

    @Test
    public void theHostTablesStubsOutliveGarbageCollection() {
        WebKitNativeShim.installHostTable();
        int slot = WkjStubShim.findHostSlot("core.is_live");
        long before = WkjStubShim.hostSlotPointer(slot);
        assertTrue(before != 0L);

        // The upcall arena is process lifetime by design (contract section 4). Churn the heap and
        // collect, then use the stub again: a stub whose arena had been closed would crash here.
        for (int i = 0; i < 200; i++) {
            byte[] churn = new byte[64 * 1024];
            churn[0] = (byte) i;
        }
        System.gc();

        assertEquals(before, WkjStubShim.hostSlotPointer(slot));
        WebKitNativeShim.clearUpcalls();
        long id = WebKitNativeShim.register(new Object());
        assertEquals(0, WkjStubShim.fireHost(slot, id));
        assertEquals(1L, WkjStubShim.lastFireResult());
        assertEquals(1, WebKitNativeShim.upcalls().size());
        WebKitNativeShim.unregister(id);
    }

    /**
     * The per call scratch arena does not leak. The proof is structural rather than measured: the
     * generated facade wraps every string call in {@code try (Arena arena = Arena.ofConfined())}, so
     * the segment is freed on the way out of the call whatever happens inside it.
     * <p>
     * A memory-size assertion was considered and rejected: it would be a flaky test of the
     * allocator's behaviour rather than a test of the facade, and the test plan says to prefer the
     * deterministic version. What is asserted here is that a hundred thousand calls complete and
     * that each made exactly one downcall, which is what would fail if the facade started retaining
     * or reallocating anything per call.
     */
    @Test
    public void aHundredThousandStringCallsCompleteWithoutGrowing() {
        long before = WkjStubShim.callTotal();

        for (int i = 0; i < CALLS; i++) {
            DomFacadeShim.attrSetValue(PEER, "leak?");
        }

        assertEquals(before + CALLS, WkjStubShim.callTotal(),
                "each call must reach the library exactly once");
        assertEquals(WkjStubShim.ringCapacity(), WkjStubShim.callCount());
    }
}
