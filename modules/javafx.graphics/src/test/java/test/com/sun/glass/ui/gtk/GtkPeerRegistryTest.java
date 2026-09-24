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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.ui.gtk.GtkPeerRegistryShim;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code com.sun.glass.ui.gtk.GtkPeerRegistry}, the id to peer registry of {@code GtkWindow} and {@code GtkView}
 * that every window, view and drag-and-drop slot of the GTK glass looks its peer up in: pure Java, so no display and
 * no GTK are needed.
 * <ul>
 * <li>a lookup allocates nothing, whatever the id - ids are never reused, so after 127 windows or views every id
 * is one that {@code Long.valueOf} does not cache;</li>
 * <li>it answers what a map would, through growth, replacement and removal;</li>
 * <li>a weak entry lets its peer be collected, a strong one keeps it, and either kind can replace the other;</li>
 * <li>readers on other threads never see a peer under an id it was not registered with while writers work.</li>
 * </ul>
 */
@Timeout(120)
public class GtkPeerRegistryTest {

    private static final int LOOKUPS = 2_000_000;

    /** Ids 128 to 300, which a {@code Map<Long, ...>} lookup boxes into a new {@code Long} every time. */
    private static long lookups(GtkPeerRegistryShim registry, int count) {
        long found = 0;
        for (int i = 0; i < count; i++) {
            if (registry.get(128 + (i % 173)) != null) {
                found++;
            }
        }
        return found;
    }

    private static long mapLookups(Map<Long, Object> map, int count) {
        long found = 0;
        for (int i = 0; i < count; i++) {
            if (map.get(128L + (i % 173)) != null) {
                found++;
            }
        }
        return found;
    }

    @Test
    public void aLookupAllocatesNothingForAnyId() {
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported(), "no per-thread allocation counter in this JVM");
        threads.setThreadAllocatedMemoryEnabled(true);
        GtkPeerRegistryShim registry = new GtkPeerRegistryShim();
        Map<Long, Object> map = new ConcurrentHashMap<>();
        for (long id = 1; id <= 300; id++) {
            registry.put(id, "peer " + id);
            map.put(id, "peer " + id);
        }
        assertEquals(LOOKUPS / 10, lookups(registry, LOOKUPS / 10));

        long before = threads.getCurrentThreadAllocatedBytes();
        long found = lookups(registry, LOOKUPS);
        long registryBytes = threads.getCurrentThreadAllocatedBytes() - before;

        mapLookups(map, LOOKUPS / 10);
        before = threads.getCurrentThreadAllocatedBytes();
        mapLookups(map, LOOKUPS);
        long mapBytes = threads.getCurrentThreadAllocatedBytes() - before;

        System.out.println("GtkPeerRegistry: " + registryBytes + " bytes for " + LOOKUPS + " lookups of ids 128 to 300;"
                + " ConcurrentHashMap<Long, ...>: " + mapBytes + " bytes");
        assertEquals(LOOKUPS, found);
        assertTrue(registryBytes < 4096, registryBytes + " bytes allocated by " + LOOKUPS + " lookups of ids 128 to"
                + " 300 (a ConcurrentHashMap<Long, ...> of the same ids here: " + mapBytes + " bytes)");
    }

    @Test
    public void itAnswersWhatAMapWouldThroughGrowthReplacementAndRemoval() {
        GtkPeerRegistryShim registry = new GtkPeerRegistryShim();
        Map<Long, Object> model = new HashMap<>();
        Random random = new Random(20260918L);
        for (int op = 0; op < 20_000; op++) {
            long id = 1 + random.nextInt(3000);
            if (random.nextInt(3) == 0) {
                assertSame(model.remove(id), registry.remove(id), "remove " + id);
            } else {
                Object peer = "peer " + id + " #" + op;
                model.put(id, peer);
                registry.put(id, peer);
            }
            if (op % 1000 == 0) {
                for (long probe = 0; probe <= 3001; probe++) {
                    assertSame(model.get(probe), registry.get(probe), "get " + probe + " after " + op + " ops");
                    assertEquals(model.containsKey(probe), registry.containsKey(probe), "containsKey " + probe);
                }
                assertEquals(model.size(), registry.size());
            }
        }
        for (long id = 1; id <= 10_000; id++) {
            registry.put(id, Long.toString(id));
        }
        for (long id = 1; id <= 10_000; id++) {
            assertEquals(Long.toString(id), registry.get(id));
        }
        assertEquals(10_000, registry.size());
        assertNull(registry.get(10_001));
        assertNull(registry.remove(10_001));
        assertEquals(10_000, registry.size());
    }

    @Test
    public void theIdZeroNamesNoPeer() {
        GtkPeerRegistryShim registry = new GtkPeerRegistryShim();
        assertNull(registry.get(0));
        assertNull(registry.remove(0));
        assertThrows(IllegalArgumentException.class, () -> registry.put(0, "peer"));
        assertThrows(IllegalArgumentException.class, () -> registry.putWeak(0, "peer"));
        assertThrows(NullPointerException.class, () -> registry.put(1, null));
        assertThrows(NullPointerException.class, () -> registry.putWeak(1, null));
        assertEquals(0, registry.size());
    }

    @Test
    public void aWeakEntryLetsItsPeerBeCollectedAndAStrongOneKeepsIt() throws InterruptedException {
        GtkPeerRegistryShim registry = new GtkPeerRegistryShim();
        WeakReference<Object> weak = registerFresh(registry, 1000, false);
        WeakReference<Object> strong = registerFresh(registry, 1001, true);
        WeakReference<Object> demoted = registerFresh(registry, 1002, true);
        registry.putWeak(1002, demoted.get());
        WeakReference<Object> promoted = registerFresh(registry, 1003, false);
        registry.put(1003, promoted.get());

        assertTrue(collect(weak), "the weakly registered peer was not collected");
        assertTrue(collect(demoted), "the peer re-registered weakly was not collected");
        assertNull(registry.get(1000));
        assertFalse(registry.containsKey(1002));
        assertSame(strong.get(), registry.get(1001));
        assertSame(promoted.get(), registry.get(1003));
        assertEquals(2, registry.size());
        // the next write drops the cleared entries; the live ones stay
        registry.put(1004, "peer");
        assertEquals(3, registry.size());
        assertSame(strong.get(), registry.get(1001));
        assertNull(registry.remove(1000));
    }

    /** Registers a new object under {@code id} and answers a weak reference to it; the caller keeps no other. */
    private static WeakReference<Object> registerFresh(GtkPeerRegistryShim registry, long id, boolean strong) {
        Object peer = new Object();
        if (strong) {
            registry.put(id, peer);
        } else {
            registry.putWeak(id, peer);
        }
        assertSame(peer, registry.get(id));
        return new WeakReference<>(peer);
    }

    /** Asks for garbage collections until {@code ref} is cleared, at most 50 times; answers whether it was. */
    static boolean collect(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 50 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        return ref.get() == null;
    }

    @Test
    public void readersSeeOnlyPeersUnderTheirOwnIdsWhileWritersWork() throws Exception {
        GtkPeerRegistryShim registry = new GtkPeerRegistryShim();
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<String> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int w = 0; w < 2; w++) {
            int writer = w;
            threads.add(Thread.ofPlatform().start(() -> {
                Random random = new Random(writer);
                while (!stop.get()) {
                    long id = 1 + random.nextInt(2000);
                    switch (random.nextInt(3)) {
                        case 0 -> registry.remove(id);
                        case 1 -> registry.putWeak(id, Long.valueOf(id));
                        default -> registry.put(id, Long.valueOf(id));
                    }
                }
            }));
        }
        for (int r = 0; r < 2; r++) {
            int reader = r;
            threads.add(Thread.ofPlatform().start(() -> {
                Random random = new Random(100 + reader);
                while (!stop.get()) {
                    long id = random.nextInt(2002);
                    Object peer = registry.get(id);
                    if (peer != null && !peer.equals(id)) {
                        failure.compareAndSet(null, "id " + id + " answered " + peer);
                    }
                }
            }));
        }
        Thread.sleep(500);
        stop.set(true);
        for (Thread thread : threads) {
            thread.join(10_000);
        }
        assertNull(failure.get());
    }
}
