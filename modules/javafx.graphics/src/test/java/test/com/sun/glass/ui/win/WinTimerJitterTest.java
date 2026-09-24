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

import com.sun.glass.ui.win.WinTimerShim;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The A/B measurement behind the one behaviour risk of the {@code WinTimer} migration: the JNI
 * attached the winmm worker thread once and cached the {@code JNIEnv} for the life of the timer
 * ({@code Timer.cpp:69-77}, and its comment that it never detaches), while an FFM upcall stub leaves
 * attach and detach policy to the JDK. If the JDK detached the worker after every upcall, every pulse
 * of the toolkit would pay an attach plus a {@code java.lang.Thread} construction - a throughput and
 * jitter change inside a commit whose contract is behaviour neutrality.
 * <p>
 * It runs the same code before and after the flip - {@link WinTimerShim} drives whatever
 * {@code WinTimer._start} currently is - and reports the tick-to-tick deltas as p50/p99/max plus the
 * number of distinct {@code Thread} objects the ticks arrived on. One distinct thread over thousands
 * of ticks means the JDK keeps the worker attached; one per tick would mean it does not.
 * <p>
 * Opt-in, because it takes tens of seconds and measures the machine as much as the code:
 * <pre>
 * mvn -pl modules/javafx.graphics test -DskipNative=true -Dtest=WinTimerJitterTest \
 *     -Djfx.glass.timerJitter=true -Djfx.glass.timerJitter.period=16 -Djfx.glass.timerJitter.ticks=2000
 * </pre>
 * The numbers land in
 * {@code target/surefire-reports/test.com.sun.glass.ui.win.WinTimerJitterTest-output.txt}, because
 * surefire redirects test output to a file. Nothing here is asserted as a bound: a build machine under
 * load delivers late ticks, and a threshold would only turn that into a flaky test.
 * <p>
 * Line numbers into {@code Timer.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/Timer.cpp}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinTimerJitterTest {

    private static final String ENABLED_PROPERTY = "jfx.glass.timerJitter";

    /** The toolkit pulse interval is 1000/refreshRate, i.e. 16 ms at 60 Hz. */
    private static final int PERIOD_MS = Integer.getInteger(ENABLED_PROPERTY + ".period", 16);

    private static final int TICKS = Integer.getInteger(ENABLED_PROPERTY + ".ticks", 2000);

    /** Ticks dropped from the statistics while the timer settles. */
    private static final int WARMUP_TICKS = 20;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
    }

    @Test
    public void reportTickJitterAndThreadIdentity() throws InterruptedException {
        assumeTrue(Boolean.getBoolean(ENABLED_PROPERTY),
                "opt-in measurement; run with -D" + ENABLED_PROPERTY + "=true");
        assumeTrue(TICKS > WARMUP_TICKS + 100, "at least " + (WARMUP_TICKS + 100) + " ticks are needed");

        long[] tickNanos = new long[TICKS];
        Set<Thread> tickThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicInteger index = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        Runnable runnable = () -> {
            int i = index.getAndIncrement();
            if (i < TICKS) {
                tickNanos[i] = System.nanoTime();
                tickThreads.add(Thread.currentThread());
                if (i == TICKS - 1) {
                    done.countDown();
                }
            }
        };

        List<String> natives = WinTimerShim.nativeMethodsOfWinTimer();
        String implementation = natives.isEmpty() ? "FFM (winmm bound in Java)" : "JNI " + natives;
        if (!natives.isEmpty()) {
            // The JNI callback dereferenced javaIDs.Runnable.run, which only WinApplication's static
            // initializer filled in; without it the first tick killed the VM. The FFM path needs nothing.
            WinTimerShim.initGlassApplicationIds();
        }
        WinTimerShim timer = new WinTimerShim(runnable);
        long id = timer.start(runnable, PERIOD_MS);
        assertNotEquals(0L, id, "the timer did not start");
        try {
            long budgetMs = (long) TICKS * PERIOD_MS * 4 + 10_000L;
            assertTrue(done.await(budgetMs, TimeUnit.MILLISECONDS),
                    "only " + index.get() + " of " + TICKS + " ticks arrived in " + budgetMs + " ms");
        } finally {
            timer.stop();
        }

        long[] deltasMicros = new long[TICKS - WARMUP_TICKS - 1];
        for (int i = 0; i < deltasMicros.length; i++) {
            int tick = WARMUP_TICKS + i;
            deltasMicros[i] = (tickNanos[tick + 1] - tickNanos[tick]) / 1_000L;
        }
        long[] sorted = deltasMicros.clone();
        Arrays.sort(sorted);
        System.out.println("WinTimerJitterTest: " + implementation);
        System.out.println("  period " + PERIOD_MS + " ms, " + deltasMicros.length
                + " tick-to-tick deltas after " + WARMUP_TICKS + " warmup ticks");
        System.out.println("  min " + millis(sorted[0])
                + "  p50 " + millis(sorted[sorted.length / 2])
                + "  p99 " + millis(sorted[(int) (sorted.length * 0.99)])
                + "  max " + millis(sorted[sorted.length - 1]));
        System.out.println("  distinct tick threads: " + tickThreads.size() + " over " + TICKS + " ticks");
        for (Thread thread : tickThreads) {
            System.out.println("    " + thread + " group=" + thread.getThreadGroup()
                    + " daemon=" + thread.isDaemon());
        }
    }

    /** Microseconds as milliseconds with three decimals. */
    private static String millis(long micros) {
        return (micros / 1000L) + "." + String.format("%03d", micros % 1000L) + " ms";
    }
}
