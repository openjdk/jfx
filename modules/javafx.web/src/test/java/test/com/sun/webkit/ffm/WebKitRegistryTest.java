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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The {@code wkj_ref} registry (contract section 3), which replaces {@code NewGlobalRef} and the
 * {@code JLObject} family. Native code holds an id, never a Java reference, so the registry is the
 * one thing standing between the library and a leak of every DOM object the page ever touched.
 * <p>
 * The registry counts references. That is the half of the ownership question the C header leaves to
 * the Java side, and it is not optional: {@code WKJHandle}'s copy constructor calls {@code retain}
 * and its destructor calls {@code release}, so one Java object routinely has several live handles,
 * and a registry whose {@code release} removed the entry unconditionally would invalidate the ids
 * the other handles still hold. Interning by object identity is <em>not</em> implemented, because
 * nothing in the C++ tree compares one handle with another.
 */
@Tag("ffm")
public class WebKitRegistryTest {

    private static final int CYCLES = 10_000;
    private static final int THREADS = 8;
    private static final int PER_THREAD = 1_000;

    /** How many extra owners the reference counting test hands the same id to. */
    private static final int HANDLES = 5;

    /** How many collection attempts a weak reference test makes before giving up. */
    private static final int COLLECT_ATTEMPTS = 50;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @BeforeEach
    void installTheHostTable() {
        WebKitNativeShim.installHostTable();
        WebKitNativeShim.clearUpcalls();
    }

    @Test
    public void registerReturnsANonZeroIdThatResolvesToTheSameObject() {
        Object object = new Object();
        long id = WebKitNativeShim.register(object);

        assertNotEquals(0L, id, "zero is reserved for null");
        assertSame(object, WebKitNativeShim.lookup(id));

        WebKitNativeShim.unregister(id);
    }

    /**
     * {@code register} mints a fresh id per registration rather than interning by identity. The C
     * header permits either (see the ownership note on {@code WKJHostCore}), and interning is not
     * needed here: a sweep of all 101 files naming a handle type found no site that compares one
     * handle with another, so nothing in the tree can tell the two models apart. What the header
     * does require, and what {@code retain} and {@code release} below implement, is that an id stays
     * valid until <em>every</em> owner has released it. The test name says which model is in force,
     * because the choice is observable from Java even where it is not from C++.
     */
    @Test
    public void registeringTheSameObjectTwiceMintsTwoIds() {
        Object object = new Object();
        long first = WebKitNativeShim.register(object);
        long second = WebKitNativeShim.register(object);

        assertNotEquals(first, second);
        assertSame(object, WebKitNativeShim.lookup(first));
        assertSame(object, WebKitNativeShim.lookup(second));

        WebKitNativeShim.unregister(first);
        WebKitNativeShim.unregister(second);
    }

    @Test
    public void unregisterDropsOneIdAndLeavesTheOthers() {
        Object object = new Object();
        long first = WebKitNativeShim.register(object);
        long second = WebKitNativeShim.register(object);

        WebKitNativeShim.unregister(first);

        assertNull(WebKitNativeShim.lookup(first));
        assertSame(object, WebKitNativeShim.lookup(second),
                "other ids for the same object are unaffected");

        WebKitNativeShim.unregister(second);
    }

    @Test
    public void zeroIsTheNullReference() {
        assertNull(WebKitNativeShim.lookup(0L));
        assertEquals(0L, WebKitNativeShim.register(null));
    }

    /**
     * The rule the ABI actually imposes: every id obtained from {@code retain} is released exactly
     * once, by whoever obtained it, and an id stays valid while anyone still holds it. This is why
     * the registry counts references rather than removing on the first release - {@code WKJHandle}'s
     * copy constructor retains and its destructor releases, so one Java object routinely has several
     * live handles, and a registry that dropped the entry on the first of them would leave every
     * other handle naming nothing and every upcall through it silently doing nothing.
     */
    @Test
    public void retainAndReleaseThroughTheHostTableFollowTheOwnershipRule() {
        int retain = WkjStubShim.findHostSlot("core.retain");
        int release = WkjStubShim.findHostSlot("core.release");
        assertTrue(retain >= 0 && release >= 0);

        Object object = new Object();
        long owned = WebKitNativeShim.register(object);
        assertEquals(1, WebKitNativeShim.referenceCount(owned));

        assertEquals(0, WkjStubShim.fireHost(retain, owned));
        long retained = WkjStubShim.lastFireResult();
        assertNotEquals(0L, retained, "retain must answer with an id for a live object");
        assertSame(object, WebKitNativeShim.lookup(retained));
        assertEquals(2, WebKitNativeShim.referenceCount(owned),
                "a retained strong id is the same id with one more owner");

        // The first release drops C's reference. Java still holds the id register() minted, so the
        // entry must survive: this is the case a remove-on-release registry gets wrong.
        assertEquals(0, WkjStubShim.fireHost(release, retained));
        assertSame(object, WebKitNativeShim.lookup(owned),
                "the id its owner still holds must not have been invalidated");
        assertEquals(1, WebKitNativeShim.referenceCount(owned));

        assertEquals(0, WkjStubShim.fireHost(release, owned));
        assertNull(WebKitNativeShim.lookup(owned), "the last release frees the entry");
        assertEquals(0, WebKitNativeShim.referenceCount(owned));
    }

    /**
     * The same balance driven straight through the registry, over more owners than one, so that the
     * count is exercised rather than only its two endpoints.
     */
    @Test
    public void anEntrySurvivesEveryHandleButTheLast() {
        int before = WebKitNativeShim.registrySize();
        Object object = new Object();
        long id = WebKitNativeShim.register(object);

        for (int i = 0; i < HANDLES; i++) {
            assertEquals(id, WebKitNativeShim.retainRef(id), "a strong retain answers the same id");
        }
        assertEquals(HANDLES + 1, WebKitNativeShim.referenceCount(id));

        for (int i = 0; i < HANDLES; i++) {
            WebKitNativeShim.releaseRef(id);
            assertSame(object, WebKitNativeShim.lookup(id),
                    "released " + (i + 1) + " of " + (HANDLES + 1) + " references, so the entry"
                            + " must still be there");
        }

        WebKitNativeShim.releaseRef(id);
        assertNull(WebKitNativeShim.lookup(id));
        assertEquals(before, WebKitNativeShim.registrySize(), "and the entry is gone from the map");
    }

    /**
     * {@code retain(0)} is zero. This mirrors the {@code (env && ref)} guard of {@code JLocalRef}
     * and {@code JGlobalRef}: null retained is null, not a new id for nothing.
     */
    @Test
    public void retainingNullIsNull() {
        assertEquals(0L, WebKitNativeShim.retainRef(0L));
        assertEquals(0L, WebKitNativeShim.retainWeakRef(0L));
        assertEquals(0, WebKitNativeShim.referenceCount(0L));
        WebKitNativeShim.releaseRef(0L);
    }

    /** Releasing more times than were retained must not resurrect the id or throw. */
    @Test
    public void releasingPastZeroIsANoOp() {
        long id = WebKitNativeShim.register(new Object());
        WebKitNativeShim.releaseRef(id);

        WebKitNativeShim.releaseRef(id);
        WebKitNativeShim.releaseRef(id);

        assertNull(WebKitNativeShim.lookup(id));
        assertEquals(0L, WebKitNativeShim.retainRef(id), "a dead id cannot be brought back");
    }

    /**
     * A weak id does not keep its object reachable. This is why {@code retain_weak} exists at all:
     * {@code Source/WebCore/bridge/jni/JobjectWrapper.cpp} takes {@code NewWeakGlobalRef} by default
     * - {@code useGlobalRef} defaults to false - so modelling every id as strong would pin every
     * Java object a page script has ever touched.
     * <p>
     * The strong id is checked first, and deliberately: it proves the collection this test waits for
     * is really the weak reference's doing and not a vacuous pass.
     */
    @Test
    public void aWeakIdDoesNotPinItsObjectAndAStrongOneDoes() throws InterruptedException {
        Object object = new Object();
        WeakReference<Object> witness = new WeakReference<>(object);
        long strong = WebKitNativeShim.register(object);
        long weak = WebKitNativeShim.retainWeakRef(strong);

        assertNotEquals(0L, weak);
        assertNotEquals(strong, weak, "a weak id is its own id, not the strong one");
        assertSame(object, WebKitNativeShim.lookup(weak));
        assertTrue(WebKitNativeShim.isRefLive(weak));

        object = null;
        assertFalse(collect(witness), "the strong id must keep the object reachable");

        WebKitNativeShim.releaseRef(strong);
        assertTrue(collect(witness), "with the strong id released, only the weak one is left and"
                + " the object must become collectable");

        assertFalse(WebKitNativeShim.isRefLive(weak),
                "is_live reports 0 once the referent is gone");
        assertNull(WebKitNativeShim.lookup(weak),
                "and lookup answers null rather than throwing");
        assertEquals(0L, WebKitNativeShim.retainRef(weak),
                "retaining a collected weak id is zero, not a strong id for a dead object");

        // The id itself stays valid to release, which is the promise made to whoever holds it.
        WebKitNativeShim.releaseRef(weak);
        assertEquals(0, WebKitNativeShim.referenceCount(weak));
    }

    /** Retaining a live weak id yields a strong id, which then keeps the object alive. */
    @Test
    public void retainingAWeakIdMintsAStrongOne() {
        Object object = new Object();
        long weak = WebKitNativeShim.retainWeakRef(WebKitNativeShim.register(object));
        long strong = WebKitNativeShim.retainRef(weak);

        assertNotEquals(0L, strong);
        assertNotEquals(weak, strong, "a strong id cannot be the weak id with a higher count");
        assertSame(object, WebKitNativeShim.lookup(strong));

        WebKitNativeShim.releaseRef(strong);
        WebKitNativeShim.releaseRef(weak);
    }

    /*
     * Waits for the witness to clear, allocating and collecting in between. A weak reference is not
     * required to clear on any particular System.gc(), so this loops; a true answer means collection
     * happened, a false one means it did not within the budget, and the caller says which it wanted.
     */
    private static boolean collect(WeakReference<?> witness) throws InterruptedException {
        for (int i = 0; i < COLLECT_ATTEMPTS; i++) {
            if (witness.get() == null) {
                return true;
            }
            byte[] churn = new byte[256 * 1024];
            churn[0] = (byte) i;
            System.gc();
            Thread.sleep(10L);
        }
        return witness.get() == null;
    }

    @Test
    public void retainingADeadIdYieldsZero() {
        int retain = WkjStubShim.findHostSlot("core.retain");
        long id = WebKitNativeShim.register(new Object());
        WebKitNativeShim.unregister(id);

        assertEquals(0, WkjStubShim.fireHost(retain, id));
        assertEquals(0L, WkjStubShim.lastFireResult());
    }

    @Test
    public void theRegistryEmptiesExactly() {
        int before = WebKitNativeShim.registrySize();

        for (int i = 0; i < CYCLES; i++) {
            long id = WebKitNativeShim.register(new Object());
            WebKitNativeShim.unregister(id);
        }
        System.gc();

        assertEquals(before, WebKitNativeShim.registrySize(),
                "the registry must return to exactly its starting size; there is no tolerance here,"
                        + " because a registry that grows is the leak this test exists to find");
    }

    @Test
    public void theRegistryEmptiesUnderConcurrentUse() throws InterruptedException {
        int before = WebKitNativeShim.registrySize();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        List<Thread> threads = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < PER_THREAD; i++) {
                        WebKitNativeShim.unregister(WebKitNativeShim.register(new Object()));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "wkj-registry-" + t);
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        done.await();
        for (Thread thread : threads) {
            thread.join();
        }
        System.gc();

        assertEquals(before, WebKitNativeShim.registrySize());
    }
}
