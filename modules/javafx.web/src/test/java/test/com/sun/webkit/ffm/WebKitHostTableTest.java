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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Upcalls through the process-wide host table (contract section 4). The table replaces about 135
 * cached {@code jmethodID}s invoked on a {@code jobject}, which maps exactly onto a
 * {@code (wkj_ref, function pointer)} pair.
 * <p>
 * The table under test is the recording one {@code WebKitNativeShim} installs, not the production
 * one: its targets are the production registry operations with a call log and a fault injector
 * wrapped round them, so that the mechanism - the layout, the stub creation, the C dispatch with the
 * right signature, the foreign thread case and the containment rule - can be observed from a test.
 * {@link WebKitHostInstallTest} covers the same slots as {@code WebKitNative} installs them, with no
 * scaffolding in the way.
 * <p>
 * The recording table fills the seven core slots and nothing else, which is why the placeholder
 * cases below use {@code chrome.reserved}: seven of the sixteen groups are still
 * {@code struct &#123; void (*reserved)(void); &#125;} in C, so a slot inside one of them is the
 * one thing that is guaranteed to stay NULL. The nine groups that carry real slots are driven,
 * through the production table, by {@link WebKitUpcallGroupTest}.
 */
@Tag("ffm")
public class WebKitHostTableTest {

    /** The slots Java fills. Anything else in the table is a placeholder and must stay NULL. */
    private static final Set<String> INSTALLED_SLOTS = Set.of(
            "core.retain", "core.retain_weak", "core.release", "core.is_live",
            "core.hash_code", "core.equals", "core.check_and_clear_exception");

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void installTheHostTable() {
        WebKitNativeShim.installHostTable();
        WebKitNativeShim.clearUpcalls();
    }

    @AfterEach
    void reinstallTheHostTable() {
        WebKitNativeShim.installHostTable();
    }

    @Test
    public void theTableIsInstalledWithTheSizeTheLibraryExpects() {
        assertTrue(WkjStubShim.hostInstalled());
        assertEquals(WkjStubShim.sizeOf("WKJHost"), WkjStubShim.hostSize());
        assertEquals(WebKitNativeShim.abiVersionExpected(), WkjStubShim.hostAbiVersion());
    }

    @Test
    public void exactlyTheExpectedSlotsAreFilled() {
        List<String> unexpectedlyNull = new ArrayList<>();
        List<String> unexpectedlyFilled = new ArrayList<>();
        for (int i = 0, n = WkjStubShim.hostSlotCount(); i < n; i++) {
            String name = WkjStubShim.hostSlotName(i);
            boolean filled = WkjStubShim.hostSlotPointer(i) != 0L;
            if (INSTALLED_SLOTS.contains(name) && !filled) {
                unexpectedlyNull.add(name);
            } else if (!INSTALLED_SLOTS.contains(name) && filled) {
                unexpectedlyFilled.add(name);
            }
        }
        assertTrue(unexpectedlyNull.isEmpty(), "these slots should carry a stub: " + unexpectedlyNull);
        // Listing the deliberately NULL slots is what makes adding a callback without wiring it fail
        // here rather than silently doing nothing at run time.
        assertTrue(unexpectedlyFilled.isEmpty(),
                "these slots are placeholders and must stay NULL: " + unexpectedlyFilled);
    }

    @Test
    public void everyInstalledSlotDispatchesWithItsDeclaredSignature() {
        Object object = new Object();
        long id = WebKitNativeShim.register(object);

        assertEquals(0, WkjStubShim.fireHost(slot("core.is_live"), id));
        assertEquals(1L, WkjStubShim.lastFireResult(), "a registered id is live");

        assertEquals(0, WkjStubShim.fireHost(slot("core.hash_code"), id));
        assertEquals(object.hashCode(), (int) WkjStubShim.lastFireResult());

        assertEquals(0, WkjStubShim.fireHost(slot("core.equals"), id, id));
        assertEquals(1L, WkjStubShim.lastFireResult());

        assertEquals(0, WkjStubShim.fireHost(slot("core.check_and_clear_exception")));
        assertEquals(0L, WkjStubShim.lastFireResult(), "no upcall has failed");

        assertEquals(List.of("core.is_live(" + id + ")", "core.hash_code(" + id + ")",
                        "core.equals(" + id + ", " + id + ")", "core.check_and_clear_exception()"),
                WebKitNativeShim.upcalls(),
                "the targets must see exactly the arguments C passed, in order");

        WebKitNativeShim.unregister(id);
    }

    @Test
    public void aWkjRefRoundTripsThroughTheRegistryAndBack() {
        Object object = new Object();
        long id = WebKitNativeShim.register(object);

        assertEquals(0, WkjStubShim.fireHost(slot("core.retain"), id));
        long retained = WkjStubShim.lastFireResult();

        assertNotEquals(0L, retained);
        assertSame(object, WebKitNativeShim.lookup(retained),
                "the id C received names the same Java object it was given");

        WebKitNativeShim.unregister(retained);
        WebKitNativeShim.unregister(id);
    }

    @Test
    public void theWrongArgumentCountIsRejectedRatherThanCallingTheStub() {
        assertEquals(-3, WkjStubShim.fireHost(slot("core.retain")),
                "core.retain takes one wkj_ref");
        assertEquals(-3, WkjStubShim.fireHost(slot("core.equals"), 1L),
                "core.equals takes two");
        assertTrue(WebKitNativeShim.upcalls().isEmpty(), "no target should have run");
    }

    @Test
    public void aNullSlotIsToleratedRatherThanCalled() {
        // The contract requires the library to check every pointer before using it: a placeholder
        // group must answer "nothing installed" and not jump to address zero.
        int placeholder = slot("chrome.reserved");
        assertEquals(0L, WkjStubShim.hostSlotPointer(placeholder));
        assertEquals(-2, WkjStubShim.fireHost(placeholder));
    }

    @Test
    public void anUnknownSlotIsRejected() {
        assertEquals(-1, WkjStubShim.fireHost(WkjStubShim.hostSlotCount() + 3, 1L));
    }

    @Test
    public void anUpcallReachesJavaFromAThreadTheJvmHasNeverSeen() {
        // The case JNI needed AttachCurrentThread for. WebKit calls Java back from its own worker
        // threads, and an FFM upcall stub has to survive that with no attach step at all.
        Object object = new Object();
        long id = WebKitNativeShim.register(object);
        String caller = Thread.currentThread().getName();

        assertEquals(0, WkjStubShim.fireHostOnForeignThread(slot("core.hash_code"), id));

        assertEquals(object.hashCode(), (int) WkjStubShim.lastFireResult());
        assertEquals(1, WebKitNativeShim.upcallThreads().size());
        assertNotEquals(caller, WebKitNativeShim.upcallThreads().get(0),
                "the target must have run on the foreign thread, not on the caller");

        WebKitNativeShim.unregister(id);
    }

    @Test
    public void noUpcallHappensAfterTheTableIsUninstalled() {
        WkjStubShim.clearHost();

        assertEquals(-4, WkjStubShim.fireHost(slot("core.is_live"), 1L));
        assertTrue(WebKitNativeShim.upcalls().isEmpty(), "nothing may run after dispose");
        assertFalse(WkjStubShim.hostInstalled());
    }

    /**
     * A {@link Throwable} escaping an upcall stub terminates the JVM, so every target catches and
     * logs. If containment is broken this test does not fail, it takes the fork down, and surefire
     * reports the crash: that is the intended loud failure rather than a quiet one.
     */
    @Test
    @Tag("crash-prone")
    public void aThrowableInsideATargetIsContainedAndLogged() {
        Object object = new Object();
        long id = WebKitNativeShim.register(object);
        WebKitNativeShim.setUpcallFailure(true);
        try {
            assertEquals(0, WkjStubShim.fireHost(slot("core.retain"), id),
                    "the C side must see an ordinary return, not a failure");
            assertEquals(0L, WkjStubShim.lastFireResult(),
                    "the documented default for a retain that could not be performed");

            List<String> contained = WebKitNativeShim.containedFailures();
            assertEquals(1, contained.size(), "exactly one throwable should have been caught");
            assertTrue(contained.get(0).contains("core.retain"),
                    "the record must name the slot: " + contained.get(0));
            assertTrue(contained.get(0).contains("deliberate failure"),
                    "the record must carry the cause: " + contained.get(0));
        } finally {
            WebKitNativeShim.setUpcallFailure(false);
            WebKitNativeShim.unregister(id);
        }
    }

    @Test
    public void aFailedUpcallIsReportedThroughCheckAndClearException() {
        WebKitNativeShim.setUpcallFailure(true);
        try {
            WkjStubShim.fireHost(slot("core.release"), 1L);
        } finally {
            WebKitNativeShim.setUpcallFailure(false);
        }

        assertEquals(0, WkjStubShim.fireHost(slot("core.check_and_clear_exception")));
        assertEquals(1L, WkjStubShim.lastFireResult(),
                "check_and_clear_exception replaces WTF::CheckAndClearException, which 269 call"
                        + " sites use to learn that the last upcall failed");

        assertEquals(0, WkjStubShim.fireHost(slot("core.check_and_clear_exception")));
        assertEquals(0L, WkjStubShim.lastFireResult(), "and it clears the state it reports");
    }

    private static int slot(String name) {
        int index = WkjStubShim.findHostSlot(name);
        assertTrue(index >= 0, "the library declares no host slot " + name);
        return index;
    }
}
