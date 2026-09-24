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

package test.com.sun.glass.ui.win;

import com.sun.glass.ui.win.WinGlassNativeShim;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for the multimedia timer half of {@code com.sun.glass.ui.win.WinGlassNative}: the five
 * {@code winmm} functions that replaced {@code native-glass/win/Timer.cpp} and {@code Timer.h}, the
 * {@code TIMECAPS} layout, the {@code timeBeginPeriod} refcount, and the one process-wide
 * {@code LPTIMECALLBACK} upcall stub.
 * <p>
 * The test that carries the class is {@link #aTimerFiresRepeatedlyOnAThreadThatIsNotTheCaller()}: a
 * Java {@code Runnable} runs on a winmm worker thread with no thread-attach code anywhere, where the
 * JNI needed {@code AttachCurrentThreadAsDaemon} ({@code Timer.cpp:69-77}) - the only such site in
 * javafx.graphics.
 * <p>
 * Everything here is headless: {@code timeSetEvent} needs no window station, unlike the robot's GDI
 * capture, so there is no desktop probe. Waits are generous and every assertion about a tick count is
 * one-sided, because a build machine under load delivers late ticks and never early ones. Two windows
 * are documented rather than asserted away: a tick already dispatched when {@code timerStop} runs may
 * still complete afterwards ({@code TIME_KILL_SYNCHRONOUS} is deliberately not set), and a tick that
 * reads the registry after the entry has been removed is dropped where the C would in practice still
 * have delivered it.
 * <p>
 * Line numbers into {@code Timer.cpp}, {@code Timer.h} and {@code Utils.cpp} refer to those files at commit
 * {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
@EnabledOnOs(OS.WINDOWS)
@Timeout(60)
public class WinTimerNativeTest {

    /** The winmm symbols the facade binds, in binding order, after the ten of the robot. */
    static final List<String> WINMM_SYMBOLS = List.of(
            "winmm!timeGetDevCaps", "winmm!timeBeginPeriod", "winmm!timeEndPeriod",
            "winmm!timeSetEvent", "winmm!timeKillEvent");

    /** Fast enough to make the tests quick, far enough above 1 ms to survive a loaded machine. */
    private static final int PERIOD_MS = 10;

    /** How long a tick that should arrive is waited for. */
    private static final long WAIT_MS = 5_000L;

    /** Every id this test started, stopped again by {@link #stopEveryTimerThisTestStarted()}. */
    private final List<Long> started = new ArrayList<>();

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinTimerNativeTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        WinGlassNativeShim.bindTimerSymbols();
    }

    /** timeGetDevCaps answered at least once, so the period bounds were checked at least once. */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * Stops every timer the test started, whether or not it stopped it itself, so that no test can
     * leave a ticking timer for the next one and the registry and refcount assertions are not order
     * dependent. Stopping an already-stopped id is a no-op by construction.
     */
    @AfterEach
    void stopEveryTimerThisTestStarted() throws InterruptedException {
        for (Long id : started) {
            WinGlassNativeShim.timerStop(id);
        }
        started.clear();
        // Let any tick that was already dispatched finish before the next test samples the registry.
        Thread.sleep(3L * PERIOD_MS);
    }

    /** Starts a timer and records its id for {@link #stopEveryTimerThisTestStarted()}. */
    private long start(Runnable runnable, int period) {
        long id = WinGlassNativeShim.timerStart(runnable, period);
        if (id != 0L) {
            started.add(id);
        }
        return id;
    }

    /** Stops a timer the test started and forgets it. */
    private void stop(long id) {
        WinGlassNativeShim.timerStop(id);
        started.remove(id);
    }

    // ---------------------------------------------------------------------------------------------
    // Symbols, layout and the device capabilities
    // ---------------------------------------------------------------------------------------------

    @Test
    public void facadeBindsTheFiveWinmmSymbolsInOrderAfterTheEagerBlock() {
        List<String> bound = WinGlassNativeShim.boundSymbols();
        // Not "last": the cursor code added a second lazy block (gdi32), and which of the two binds first
        // depends on the order the test classes run in this shared JVM. What is fixed is that the
        // five arrive together and only after everything the class initializer bound.
        int at = Collections.indexOfSubList(bound, WINMM_SYMBOLS);
        assertTrue(at > bound.indexOf("user32!MapVirtualKeyW"),
                "the winmm block binds contiguously and lazily, after glass and user32: " + bound);
        for (String symbol : WINMM_SYMBOLS) {
            assertTrue(WinGlassNativeShim.resolves(symbol), symbol);
        }
        assertFalse(WinGlassNativeShim.resolves("winmm!timeSetEventEx"));
    }

    /**
     * The {@code TIMECAPS} of mmsystem.h. The size and the offsets restate the numbers the layout was
     * written from, so the values are read back as well: a swapped or shifted layout would put
     * {@code wPeriodMax} where {@code wPeriodMin} belongs, and Windows has documented
     * {@code wPeriodMin == 1} and {@code wPeriodMax == 1000000} since XP. The C-side half of this
     * check is a {@code static_assert} in {@code glass_win_api.cpp}.
     */
    @Test
    public void timecapsLayoutIsTheMmsystemStructAndTheValuesReadBackInOrder() {
        assertEquals(8, WinGlassNativeShim.layoutByteSize("TIMECAPS"));
        assertEquals(0, WinGlassNativeShim.offset("TIMECAPS", "wPeriodMin"));
        assertEquals(4, WinGlassNativeShim.offset("TIMECAPS", "wPeriodMax"));

        int min = WinGlassNativeShim.timerMinPeriod();
        int max = WinGlassNativeShim.timerMaxPeriod();
        LEDGER.requireOracle(max > 0, () -> "timeGetDevCaps failed on this machine: it reports " + min + ".."
                + max + ", so wPeriodMin and wPeriodMax cannot be checked.");
        LEDGER.compared();
        assertTrue(min >= 1 && min <= 50, "wPeriodMin out of any plausible range: " + min);
        assertTrue(max >= 1_000, "wPeriodMax out of any plausible range: " + max);
        assertTrue(min <= max, min + " > " + max);
    }

    /** {@code Timer::InitTC} memoises the query, so the pair never changes within a JVM. */
    @Test
    public void periodBoundsAreOrderedAndMemoised() {
        int min = WinGlassNativeShim.timerMinPeriod();
        int max = WinGlassNativeShim.timerMaxPeriod();
        assertEquals(min, WinGlassNativeShim.timerMinPeriod());
        assertEquals(max, WinGlassNativeShim.timerMaxPeriod());
        assertTrue(Integer.compareUnsigned(min, max) <= 0, min + " > " + max + " unsigned");
    }

    /**
     * {@code wTimerRes = min(max(wPeriodMin, 1), wPeriodMax)} ({@code Timer.h:90}), on unsigned
     * comparisons, which is what every {@code timeSetEvent} is given as its resolution - not the
     * period.
     */
    @Test
    public void resolutionIsTheClampedMinimumTheCComputed() {
        int min = WinGlassNativeShim.timerMinPeriod();
        int max = WinGlassNativeShim.timerMaxPeriod();
        LEDGER.requireOracle(max > 0, () -> "timeGetDevCaps failed on this machine: it reports " + min + ".."
                + max + ", so the clamped resolution cannot be checked.");
        LEDGER.compared();
        int atLeastOne = Integer.compareUnsigned(min, 1) > 0 ? min : 1;
        int expected = Integer.compareUnsigned(atLeastOne, max) < 0 ? atLeastOne : max;
        assertEquals(expected, WinGlassNativeShim.timerResolution());
    }

    // ---------------------------------------------------------------------------------------------
    // Ticks: the upcall stub, entered from a thread nothing attached
    // ---------------------------------------------------------------------------------------------

    /**
     * The test the timer's move to Java exists for. A {@code Runnable} that the JVM only knows as a registry entry
     * is called from a winmm multimedia-timer worker, with no {@code AttachCurrentThread} anywhere -
     * that whole mechanism ({@code Timer.cpp:66-77}) is gone. Nothing is asserted about the identity
     * of the tick thread beyond its not being the caller: {@code com.sun.glass.ui.Timer} promises only
     * that {@code run()} may be invoked off the UI thread.
     */
    @Test
    public void aTimerFiresRepeatedlyOnAThreadThatIsNotTheCaller() throws InterruptedException {
        int registryBefore = WinGlassNativeShim.timerRegistrySize();
        CountDownLatch ticks = new CountDownLatch(5);
        AtomicReference<Thread> tickThread = new AtomicReference<>();
        long id = start(() -> {
            tickThread.compareAndSet(null, Thread.currentThread());
            ticks.countDown();
        }, PERIOD_MS);

        assertNotEquals(0L, id, "timeSetEvent refused a " + PERIOD_MS + " ms periodic timer");
        assertEquals(registryBefore + 1, WinGlassNativeShim.timerRegistrySize());
        assertTrue(ticks.await(WAIT_MS, TimeUnit.MILLISECONDS),
                "fewer than 5 ticks of a " + PERIOD_MS + " ms timer in " + WAIT_MS + " ms");
        assertNotSame(Thread.currentThread(), tickThread.get(),
                "the timer ran its Runnable on the calling thread");
        stop(id);
        assertEquals(registryBefore, WinGlassNativeShim.timerRegistrySize());
    }

    /**
     * After {@code timerStop} the ticks stop. One further tick is tolerated between the stop and the
     * first sample: {@code TIME_KILL_SYNCHRONOUS} is deliberately not set, so a callback already
     * dispatched runs to completion (and one that has not yet read the registry is dropped instead).
     */
    @Test
    public void stopEndsTheTicks() throws InterruptedException {
        AtomicInteger ticks = new AtomicInteger();
        CountDownLatch running = new CountDownLatch(2);
        long id = start(() -> {
            ticks.incrementAndGet();
            running.countDown();
        }, PERIOD_MS);
        assertNotEquals(0L, id);
        assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS), "the timer never ticked");

        stop(id);
        int atStop = ticks.get();
        Thread.sleep(300L);
        int at300 = ticks.get();
        Thread.sleep(300L);
        int at600 = ticks.get();
        assertEquals(at300, at600, "the timer kept ticking after timerStop");
        assertTrue(at300 - atStop <= 1,
                "more than the one in-flight tick arrived after timerStop: " + (at300 - atStop));
    }

    /**
     * Stopping from inside the callback, which is what {@code QuantumToolkit} does in spirit when its
     * timer runnable pauses the pulse timer on the timer thread. It cannot deadlock precisely because
     * {@code timeKillEvent} does not join an in-flight callback - the reason
     * {@code TIME_KILL_SYNCHRONOUS} must not be added.
     */
    @Test
    public void stoppingFromInsideTheCallbackIsSafe() throws InterruptedException {
        int registryBefore = WinGlassNativeShim.timerRegistrySize();
        int refCountBefore = WinGlassNativeShim.timerPeriodRefCount();
        AtomicLong self = new AtomicLong();
        AtomicInteger ticks = new AtomicInteger();
        CountDownLatch stopped = new CountDownLatch(1);
        long id = start(() -> {
            long myId = self.get();
            if (myId != 0L && ticks.incrementAndGet() >= 2) {
                WinGlassNativeShim.timerStop(myId);
                stopped.countDown();
            }
        }, PERIOD_MS);
        assertNotEquals(0L, id);
        self.set(id);

        assertTrue(stopped.await(WAIT_MS, TimeUnit.MILLISECONDS), "the timer never stopped itself");
        started.remove(id);
        int atStop = ticks.get();
        Thread.sleep(300L);
        assertTrue(ticks.get() - atStop <= 1, "the timer kept ticking after stopping itself");
        assertEquals(registryBefore, WinGlassNativeShim.timerRegistrySize());
        assertEquals(refCountBefore, WinGlassNativeShim.timerPeriodRefCount());
    }

    /**
     * An exception out of the {@code Runnable} must not escape the upcall stub - if it did, the JVM
     * would terminate and this whole test class would take the run with it - and the timer must keep
     * ticking, as it did when {@code CheckAndClearException} ({@code Utils.cpp:50-71}) swallowed it.
     * The captured throwable is compared by identity, and the timer is stopped and drained before the
     * process-wide handler is put back, so a late tick from another test cannot be mistaken for this
     * one and this one cannot outlive the handler.
     */
    @Test
    public void aThrowingRunnableIsReportedAndTheTimerKeepsTicking() throws InterruptedException {
        RuntimeException deliberate = new RuntimeException("WinTimerNativeTest: deliberate");
        AtomicReference<Throwable> captured = new AtomicReference<>();
        AtomicInteger ticks = new AtomicInteger();
        CountDownLatch fourTicks = new CountDownLatch(4);
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        long id = 0L;
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                if (throwable == deliberate) {
                    captured.compareAndSet(null, throwable);
                } else if (previous != null) {
                    previous.uncaughtException(thread, throwable);
                }
            });
            id = start(() -> {
                fourTicks.countDown();
                if (ticks.incrementAndGet() == 1) {
                    throw deliberate;
                }
            }, PERIOD_MS);
            assertNotEquals(0L, id);
            assertTrue(fourTicks.await(WAIT_MS, TimeUnit.MILLISECONDS),
                    "the timer stopped after the throwing tick: only " + ticks.get() + " ticks");
            assertSame(deliberate, captured.get(),
                    "the throwable was not reported to the tick thread's uncaught-exception handler");
        } finally {
            if (id != 0L) {
                stop(id);
            }
            Thread.sleep(3L * PERIOD_MS);
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Ids, the registry and the timeBeginPeriod refcount
    // ---------------------------------------------------------------------------------------------

    @Test
    public void idsAreDistinctAndNeverZero() {
        int registryBefore = WinGlassNativeShim.timerRegistrySize();
        long first = start(() -> { }, PERIOD_MS);
        long second = start(() -> { }, PERIOD_MS);
        long third = start(() -> { }, PERIOD_MS);
        assertTrue(first > 0L && second > 0L && third > 0L, first + ", " + second + ", " + third);
        assertNotEquals(first, second);
        assertNotEquals(second, third);
        assertNotEquals(first, third);
        assertEquals(registryBefore + 3, WinGlassNativeShim.timerRegistrySize());
        stop(first);
        stop(second);
        stop(third);
        assertEquals(registryBefore, WinGlassNativeShim.timerRegistrySize());
    }

    /**
     * The registry and {@code Timer::timersCount} - the refcount that decides when
     * {@code timeBeginPeriod} and {@code timeEndPeriod} are called ({@code Timer.h:48,62}) - return to
     * their starting values on every cycle. The winmm calls themselves have no effect Java can
     * observe; the refcount is the C's own variable and is what the pairing hangs on.
     */
    @Test
    public void theRegistryAndThePeriodRefcountReturnToBaselineOnEveryCycle() {
        int registryBefore = WinGlassNativeShim.timerRegistrySize();
        int refCountBefore = WinGlassNativeShim.timerPeriodRefCount();
        for (int cycle = 0; cycle < 5; cycle++) {
            long id = start(() -> { }, PERIOD_MS);
            assertNotEquals(0L, id, "cycle " + cycle);
            assertEquals(registryBefore + 1, WinGlassNativeShim.timerRegistrySize(), "cycle " + cycle);
            assertEquals(refCountBefore + 1, WinGlassNativeShim.timerPeriodRefCount(), "cycle " + cycle);
            stop(id);
            assertEquals(registryBefore, WinGlassNativeShim.timerRegistrySize(), "cycle " + cycle);
            assertEquals(refCountBefore, WinGlassNativeShim.timerPeriodRefCount(), "cycle " + cycle);
        }
    }

    /**
     * The failure path {@code Timer.cpp:55-57} took: {@code timeSetEvent} refuses a delay outside
     * {@code wPeriodMin..wPeriodMax}, the derived constructor throws, and because it is the
     * <em>derived</em> body the base destructor still runs and releases the period. Nothing may be
     * left in the registry and the refcount must be back where it started.
     */
    @Test
    public void aStartWinmmRefusesLeavesNoEntryAndNoHeldPeriod() {
        int registryBefore = WinGlassNativeShim.timerRegistrySize();
        int refCountBefore = WinGlassNativeShim.timerPeriodRefCount();
        long id = start(() -> { }, 0);
        assertEquals(0L, id, "timeSetEvent accepted a 0 ms period, which is below wPeriodMin");
        assertEquals(registryBefore, WinGlassNativeShim.timerRegistrySize());
        assertEquals(refCountBefore, WinGlassNativeShim.timerPeriodRefCount());
    }

    /**
     * {@code Timer.java:129-135} makes a double stop unreachable through the public API, so the facade
     * is free to return silently where {@code Timer.cpp:50} would have deleted the same pointer twice.
     * What matters is that the refcount does not go with it.
     */
    @Test
    public void stoppingAnUnknownOrAlreadyStoppedIdIsANoOp() {
        int refCountBefore = WinGlassNativeShim.timerPeriodRefCount();
        WinGlassNativeShim.timerStop(0L);
        WinGlassNativeShim.timerStop(-1L);
        WinGlassNativeShim.timerStop(Long.MAX_VALUE);
        long id = start(() -> { }, PERIOD_MS);
        assertNotEquals(0L, id);
        stop(id);
        WinGlassNativeShim.timerStop(id);
        WinGlassNativeShim.timerStop(id);
        assertEquals(refCountBefore, WinGlassNativeShim.timerPeriodRefCount(),
                "a repeated stop decremented the timeBeginPeriod refcount");
    }

    /**
     * Parity with the C, and a warning to whoever reads the registry next: the map holds its entries
     * strongly, so a timer nobody stops keeps ticking and pins its {@code Runnable} for the life of
     * the JVM. The C leaked exactly the same way - an abandoned {@code RunnableTimer} kept its JNI
     * global ref and its {@code timeSetEvent} ({@code Timer.cpp:42,67}). Making the map weak would
     * silently stop such a timer, which is a behaviour change and not a leak fix.
     */
    @Test
    public void anAbandonedTimerKeepsTickingAndPinsItsRunnable() throws InterruptedException {
        AtomicInteger ticks = new AtomicInteger();
        CountDownLatch running = new CountDownLatch(2);
        WeakReference<Runnable> abandoned;
        long id;
        {
            Runnable runnable = () -> {
                ticks.incrementAndGet();
                running.countDown();
            };
            abandoned = new WeakReference<>(runnable);
            id = start(runnable, PERIOD_MS);
        }
        assertNotEquals(0L, id);
        assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS), "the timer never ticked");

        System.gc();
        System.gc();
        assertNotNull(abandoned.get(), "the registry no longer holds the Runnable strongly");

        int beforeGc = ticks.get();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MS);
        while (System.nanoTime() < deadline && ticks.get() - beforeGc < 2) {
            Thread.sleep(PERIOD_MS);
        }
        assertTrue(ticks.get() - beforeGc >= 2, "an abandoned timer stopped ticking after a GC");
    }
}
