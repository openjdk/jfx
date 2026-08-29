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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The {@code WKJHost} table {@link com.sun.webkit.WebKitNative} builds and installs, as opposed to
 * the recording copy {@code WebKitNativeShim} drives in {@link WebKitHostTableTest}. This is the
 * table the library will actually call: the seven {@code WKJHostCore} slots are the ones every
 * {@code WKJHandle} in the C++ tree goes through, and until they were installed every callback the
 * page, chrome and frame loader tables carry arrived with a {@code wkj_ref} of 0, because
 * {@code WKJHostCore::retain} is documented to return 0 when the slot is NULL.
 * <p>
 * It is also where the {@code WKJHost} table is checked for completeness: 159 of its 168 callback
 * slots must carry a stub, and the nine that must not are named and justified in
 * {@link #DELIBERATELY_NULL_SLOTS}. That check matters because a NULL slot is not an error to the
 * library - it falls back to the documented default and carries on - so an unfilled slot is a
 * feature that silently does nothing rather than a crash.
 * <p>
 * Production installs the table once, from its class initializer. These tests re-install the same
 * table over a deliberately cleared library, because the recording table of the other suites may
 * have replaced it by the time this class runs.
 */
@Tag("ffm")
public class WebKitHostInstallTest {

    private static final int WKJ_INIT_OK = 0;

    /** {@code sizeof(WKJHost)} and {@code sizeof(WKJHostCore)}, as the C header lays them out. */
    private static final int HOST_SIZE = 1352;
    private static final int HOST_CORE_SIZE = 56;

    /**
     * The slots production deliberately leaves NULL, and why. Everything else in the table carries a
     * stub, which is what {@link #everySlotIsFilledExceptTheOnesWithNoJavaTarget} asserts - stated
     * this way round so that a slot added to a C header with no Java target fails here instead of
     * going unnoticed among 168 others.
     * <ul>
     * <li>The seven {@code *.reserved} members are the callback groups whose upcall audit has not
     *     been done: {@code webpage}, {@code frameloader}, {@code chrome}, {@code editor},
     *     {@code contextmenu}, {@code inspector} and {@code drag}. Each is
     *     {@code struct { void (*reserved)(void); }} in C because an empty struct is not valid
     *     there, and there is no Java method for a member that names nothing.
     * <li>{@code pal.system_beep} reached {@code java.awt.Toolkit} through {@code FindClass}, which
     *     in a module that does not require {@code java.desktop} returned null, so the call did
     *     nothing. The documented default for a NULL slot is "no-op", so leaving it NULL preserves
     *     the behaviour exactly; filling it is a behaviour change and its own commit.
     * <li>{@code theme.plugin_widget_paint} is handed a {@code WebCore::PlatformContextJava*} where
     *     {@code WCPluginWidget.paint} demands a {@code WCGraphicsContext}. The C header records
     *     that as a preserved bug and declares the parameter as the {@code int64_t} it really is;
     *     Java cannot turn one into the other, so the slot answers the documented no-op.
     * </ul>
     */
    private static final Set<String> DELIBERATELY_NULL_SLOTS = Set.of(
            "webpage.reserved", "frameloader.reserved", "chrome.reserved", "editor.reserved",
            "contextmenu.reserved", "inspector.reserved", "drag.reserved",
            "pal.system_beep", "theme.plugin_widget_paint");

    /** An object whose {@code hashCode} throws, to drive containment through a real target. */
    private static final class Hostile {
        @Override
        public int hashCode() {
            throw new IllegalStateException("deliberate hashCode failure");
        }
    }

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void installTheProductionTable() {
        WebKitNativeShim.installProductionHostTable();
        // Any failure recorded by an earlier test on this thread would otherwise be reported as this
        // test's, which is exactly the confusion check_and_clear_exception exists to avoid.
        WebKitNativeShim.checkAndClearUpcallFailure();
    }

    @AfterAll
    static void leaveTheRecordingTableInstalled() {
        WebKitNativeShim.installHostTable();
    }

    @Test
    public void theTableWasInstalledWhenTheClassWasInitialized() {
        assertEquals(WKJ_INIT_OK, WebKitNativeShim.productionHostInitResult(),
                "wkj_init must have succeeded from WebKitNative's static initializer, which is the"
                        + " only point that is guaranteed to precede every other wkj_* call");
    }

    @Test
    public void theTableCarriesTheSizeTheLibraryExpects() {
        assertTrue(WkjStubShim.hostInstalled());
        assertEquals(WkjStubShim.sizeOf("WKJHost"), WkjStubShim.hostSize(),
                "the library accepted a host_size that is not its own sizeof(WKJHost)");
        assertEquals(HOST_SIZE, WkjStubShim.hostSize(),
                "sizeof(WKJHost) is no longer 1352; the ABI version must be bumped");
        assertEquals(HOST_CORE_SIZE, WkjStubShim.sizeOf("WKJHostCore"),
                "sizeof(WKJHostCore) is no longer 56, so the seven core slots have changed");
        assertEquals(HOST_SIZE, WebKitNativeShim.productionHostSizeField(),
                "the table must declare its own size in its size field, which is what wkj_init"
                        + " checks against the host_size argument");
        assertEquals(WebKitNativeShim.abiVersionExpected(), WkjStubShim.hostAbiVersion());
    }

    @Test
    public void everyProductionSlotIsWhereTheCCompilerPutIt() {
        for (int i = 0, n = WkjStubShim.hostSlotCount(); i < n; i++) {
            String name = WkjStubShim.hostSlotName(i);
            assertEquals(WkjStubShim.hostSlotOffset(i), WebKitNativeShim.hostSlotOffset(name),
                    "the offset of host slot " + name + " disagrees with the C compiler");
        }
    }

    /**
     * Every callback slot the C header declares carries a stub, except the nine named in
     * {@link #DELIBERATELY_NULL_SLOTS}. Reading it that way round is the point: the C side tolerates
     * a NULL slot, so an unfilled one is silent at runtime, and this is the only place that says so.
     */
    @Test
    public void everySlotIsFilledExceptTheOnesWithNoJavaTarget() {
        List<String> unexpectedlyNull = new ArrayList<>();
        List<String> unexpectedlyFilled = new ArrayList<>();
        for (int i = 0, n = WkjStubShim.hostSlotCount(); i < n; i++) {
            String name = WkjStubShim.hostSlotName(i);
            boolean filled = WkjStubShim.hostSlotPointer(i) != 0L;
            if (DELIBERATELY_NULL_SLOTS.contains(name)) {
                if (filled) {
                    unexpectedlyFilled.add(name);
                }
            } else if (!filled) {
                unexpectedlyNull.add(name);
            }
        }
        assertTrue(unexpectedlyNull.isEmpty(),
                "these slots reach no Java target, so the library will fall back to the default"
                        + " documented on each and the client behind it is dead: " + unexpectedlyNull);
        assertTrue(unexpectedlyFilled.isEmpty(),
                "these slots are documented as deliberately NULL and something filled them: "
                        + unexpectedlyFilled);
    }

    /**
     * The nine NULL slots are the ones this test class documents and no others, so that removing a
     * name from that list without filling the slot fails rather than quietly widening the exemption.
     */
    @Test
    public void theDeliberatelyNullSlotsAllExist() {
        for (String name : DELIBERATELY_NULL_SLOTS) {
            assertNotEquals(-1, WkjStubShim.findHostSlot(name),
                    "the C header no longer declares " + name + ", so it must leave this list");
        }
    }

    /**
     * The whole point of the table: a {@code wkj_ref} the library retains resolves to the same Java
     * object, and survives until the library releases it. This is what {@code WKJHandle::retained}
     * does on every page, chrome and frame loader callback target.
     */
    @Test
    public void retainAndReleaseThroughTheProductionTableAreBalanced() {
        Object object = new Object();
        long owned = WebKitNativeShim.register(object);

        assertEquals(0, WkjStubShim.fireHost(slot("core.retain"), owned));
        long retained = WkjStubShim.lastFireResult();
        assertNotEquals(0L, retained);
        assertSame(object, WebKitNativeShim.lookup(retained));
        assertEquals(2, WebKitNativeShim.referenceCount(owned));

        assertEquals(0, WkjStubShim.fireHost(slot("core.is_live"), retained));
        assertEquals(1L, WkjStubShim.lastFireResult());

        assertEquals(0, WkjStubShim.fireHost(slot("core.release"), retained));
        assertSame(object, WebKitNativeShim.lookup(owned), "Java still owns its own id");

        WebKitNativeShim.releaseRef(owned);
        assertEquals(0, WkjStubShim.fireHost(slot("core.is_live"), owned));
        assertEquals(0L, WkjStubShim.lastFireResult(), "the last release freed the entry");
    }

    @Test
    public void retainOfNullThroughTheTableIsNull() {
        assertEquals(0, WkjStubShim.fireHost(slot("core.retain"), 0L));
        assertEquals(0L, WkjStubShim.lastFireResult(), "retain(0) is 0, as the header documents");

        assertEquals(0, WkjStubShim.fireHost(slot("core.retain_weak"), 0L));
        assertEquals(0L, WkjStubShim.lastFireResult());

        assertEquals(0, WkjStubShim.fireHost(slot("core.is_live"), 0L));
        assertEquals(0L, WkjStubShim.lastFireResult());

        // release(0) is a documented no-op, so this must simply return.
        assertEquals(0, WkjStubShim.fireHost(slot("core.release"), 0L));
    }

    @Test
    public void weakRetainThroughTheTableMintsAnIdThatDoesNotOwnTheObject() {
        Object object = new Object();
        long owned = WebKitNativeShim.register(object);

        assertEquals(0, WkjStubShim.fireHost(slot("core.retain_weak"), owned));
        long weak = WkjStubShim.lastFireResult();

        assertNotEquals(0L, weak);
        assertNotEquals(owned, weak, "a weak id is its own id");
        assertSame(object, WebKitNativeShim.lookup(weak));
        assertEquals(1, WebKitNativeShim.referenceCount(owned),
                "retain_weak must not have added an owner to the strong id");

        WebKitNativeShim.releaseRef(weak);
        WebKitNativeShim.releaseRef(owned);
    }

    @Test
    public void hashCodeAndEqualsAnswerForTheReferentsNotTheIds() {
        String text = new StringBuilder("wkj").toString();
        long first = WebKitNativeShim.register(text);
        long second = WebKitNativeShim.register(new StringBuilder("wkj").toString());
        long other = WebKitNativeShim.register("other");

        assertEquals(0, WkjStubShim.fireHost(slot("core.hash_code"), first));
        assertEquals(text.hashCode(), (int) WkjStubShim.lastFireResult());

        assertEquals(0, WkjStubShim.fireHost(slot("core.equals"), first, second));
        assertEquals(1L, WkjStubShim.lastFireResult(),
                "two ids naming equal objects are equal; the ids themselves differ, and the C"
                        + " header is explicit that these answer for the referents");

        assertEquals(0, WkjStubShim.fireHost(slot("core.equals"), first, other));
        assertEquals(0L, WkjStubShim.lastFireResult());

        WebKitNativeShim.releaseRef(first);
        WebKitNativeShim.releaseRef(second);
        WebKitNativeShim.releaseRef(other);
    }

    /**
     * A {@link Throwable} escaping an upcall stub terminates the JVM, so the target catches it,
     * returns the documented default and records the failure for {@code check_and_clear_exception}.
     * The failure is a real one - an object whose {@code hashCode} throws - rather than an injected
     * one, so this exercises the production catch rather than a test double of it.
     */
    @Test
    @Tag("crash-prone")
    public void aThrowableInATargetIsContainedAndThenReported() {
        long hostile = WebKitNativeShim.register(new Hostile());

        assertEquals(0, WkjStubShim.fireHost(slot("core.hash_code"), hostile),
                "the C side must see an ordinary return, not a failure");
        assertEquals(0L, WkjStubShim.lastFireResult(),
                "the documented default for a hash_code that could not be computed");

        assertEquals(0, WkjStubShim.fireHost(slot("core.check_and_clear_exception")));
        assertEquals(1L, WkjStubShim.lastFireResult(),
                "check_and_clear_exception replaces WTF::CheckAndClearException, which about a"
                        + " dozen C++ sites branch on");

        assertEquals(0, WkjStubShim.fireHost(slot("core.check_and_clear_exception")));
        assertEquals(0L, WkjStubShim.lastFireResult(), "and it clears the state it reports");

        WebKitNativeShim.releaseRef(hostile);
    }

    /**
     * The flag is per thread, because the JNI pending exception it replaces was per thread: a
     * failure on WebKit's network thread must not be reported to its main thread, which would send
     * one of the dozen branching call sites down the wrong path.
     */
    @Test
    @Tag("crash-prone")
    public void aFailureOnAnotherThreadIsNotReportedToThisOne() {
        long hostile = WebKitNativeShim.register(new Hostile());

        assertEquals(0, WkjStubShim.fireHostOnForeignThread(slot("core.hash_code"), hostile));
        assertEquals(0L, WkjStubShim.lastFireResult());

        assertEquals(0, WkjStubShim.fireHost(slot("core.check_and_clear_exception")));
        assertEquals(0L, WkjStubShim.lastFireResult(),
                "the upcall that failed ran on a thread the JVM had never seen; this thread has"
                        + " nothing to report");

        WebKitNativeShim.releaseRef(hostile);
    }

    private static int slot(String name) {
        int index = WkjStubShim.findHostSlot(name);
        assertTrue(index >= 0, "the library declares no host slot " + name);
        return index;
    }
}
