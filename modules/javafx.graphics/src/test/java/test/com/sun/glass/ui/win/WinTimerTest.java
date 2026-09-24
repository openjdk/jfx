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
import com.sun.glass.ui.win.WinTimerShim;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The peer half of the timer: {@code com.sun.glass.ui.win.WinTimer} itself, driven through
 * {@link WinTimerShim} because both the class and its {@code _start}/{@code _stop} overrides are
 * package-private, and because {@code com.sun.glass.ui.Timer.start(int)} needs a running
 * {@code Application} that a headless unit test does not have.
 * <p>
 * {@link #winTimerDeclaresNoNativeMethod()} is the one that says the migration happened; the rest say
 * that nothing else about the peer moved - the cached period bounds still come from the platform, the
 * vsync overload still refuses, and {@code _pause}/{@code _resume} are still empty.
 */
@EnabledOnOs(OS.WINDOWS)
@Timeout(60)
public class WinTimerTest {

    private static final int PERIOD_MS = 10;

    private static final long WAIT_MS = 5_000L;

    private final List<WinTimerShim> started = new ArrayList<>();

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        WinGlassNativeShim.bindTimerSymbols();
    }

    @AfterEach
    void stopEveryTimerThisTestStarted() throws InterruptedException {
        for (WinTimerShim timer : started) {
            timer.stop();
        }
        started.clear();
        Thread.sleep(3L * PERIOD_MS);
    }

    private WinTimerShim newTimer(Runnable runnable) {
        WinTimerShim timer = new WinTimerShim(runnable);
        started.add(timer);
        return timer;
    }

    /**
     * The migration itself: {@code WinTimer} declares no {@code native} method any more, so
     * {@code javac -h} stops emitting {@code com_sun_glass_ui_win_WinTimer.h} and
     * {@code native-glass/win/Timer.cpp} has no entry point left to export.
     */
    @Test
    public void winTimerDeclaresNoNativeMethod() {
        assertEquals(List.of(), WinTimerShim.nativeMethodsOfWinTimer(),
                "WinTimer still declares native methods");
    }

    /**
     * {@code WinTimer} caches {@code getMinPeriod}/{@code getMaxPeriod} in its static initializer,
     * once, from the same {@code timeGetDevCaps} query the facade memoises.
     */
    @Test
    public void periodBoundsAreThePlatformOnesAndAreCached() {
        assertEquals(WinGlassNativeShim.timerMinPeriod(), WinTimerShim.minPeriod());
        assertEquals(WinGlassNativeShim.timerMaxPeriod(), WinTimerShim.maxPeriod());
        assertEquals(WinTimerShim.minPeriod(), WinTimerShim.minPeriod());
        assertTrue(Integer.compareUnsigned(WinTimerShim.minPeriod(), WinTimerShim.maxPeriod()) <= 0);
    }

    /** The peer starts a real periodic timer, its ticks arrive off the caller thread, and it stops. */
    @Test
    public void thePeerStartsTicksAndStops() throws InterruptedException {
        AtomicInteger ticks = new AtomicInteger();
        CountDownLatch running = new CountDownLatch(3);
        Runnable runnable = () -> {
            ticks.incrementAndGet();
            running.countDown();
        };
        WinTimerShim timer = newTimer(runnable);
        long id = timer.start(runnable, PERIOD_MS);
        assertNotEquals(0L, id, "WinTimer._start returned 0");
        assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS), "the peer never ticked");

        timer.stop();
        int atStop = ticks.get();
        Thread.sleep(300L);
        assertTrue(ticks.get() - atStop <= 1, "the peer kept ticking after _stop");
    }

    /** {@code WinTimer._start(Runnable)}: Windows has no vsync timer, and still says so. */
    @Test
    public void theVsyncStartStillRefuses() {
        WinTimerShim timer = newTimer(() -> { });
        RuntimeException failure = assertThrows(RuntimeException.class, () -> timer.startVsync(() -> { }));
        assertEquals("vsync timer not supported", failure.getMessage());
    }

    /**
     * {@code _pause} and {@code _resume} are empty on Windows - {@code QuantumToolkit} calls them on
     * every pulse ({@code QuantumToolkit.java:536,547}) - and a paused timer therefore keeps ticking.
     */
    @Test
    public void pauseAndResumeAreStillNoOps() throws InterruptedException {
        AtomicInteger ticks = new AtomicInteger();
        CountDownLatch running = new CountDownLatch(2);
        Runnable runnable = () -> {
            ticks.incrementAndGet();
            running.countDown();
        };
        WinTimerShim timer = newTimer(runnable);
        assertNotEquals(0L, timer.start(runnable, PERIOD_MS));
        assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS), "the peer never ticked");

        timer.pause();
        int atPause = ticks.get();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MS);
        while (System.nanoTime() < deadline && ticks.get() - atPause < 2) {
            Thread.sleep(PERIOD_MS);
        }
        assertTrue(ticks.get() - atPause >= 2, "_pause stopped the ticks; it is supposed to be empty");
        timer.resume();
        timer.stop();
    }
}
