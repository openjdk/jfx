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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The GTK pulse timer, {@code GtkTimer._start} / {@code _stop} and the two period bounds of {@code GtkApplication},
 * observed through {@code com.sun.glass.ui.Timer} on a running toolkit: what {@code GlassTimer.cpp} and
 * {@code GlassApplication.cpp} ({@code staticTimer_getMinPeriod}, {@code staticTimer_getMaxPeriod}) did at commit
 * {@code 033187ad90}, pinned so that the {@code java.lang.foreign} replacement can be held to it.
 * <ul>
 * <li>the bounds are the constants 0 and 10000;</li>
 * <li>{@code gdk_threads_add_timeout_full(G_PRIORITY_HIGH_IDLE, period, ...)}: ticks run on the FX thread, never
 * more often than the period, at priority 100 - after a GLib source at 99 and before one at 101 that became ready
 * in the same iteration;</li>
 * <li>{@code _stop} on the FX thread lets no further tick through ({@code call_runnable_in_timer} sees the flag
 * first), and the tick after that frees the timer and removes its source ({@code FALSE});</li>
 * <li>a {@code Runnable} that throws is reported to the FX thread's uncaught-exception handler
 * ({@code LOG_EXCEPTION}), with the thrown instance, and the timer keeps firing.</li>
 * </ul>
 * All of it runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkTimerNativeTest {

    private static final int PERIOD = 25;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkTimerNativeTest.class, "timerScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void periodBoundsAreTheConstantsTheCReturned() {
        assertEquals("0", value("timer.minPeriod"));
        assertEquals("10000", value("timer.maxPeriod"));
    }

    @Test
    public void ticksRunOnTheEventThreadNoFasterThanThePeriod() {
        assertEquals("true", value("timer.ticks.allOnEventThread"));
        int ticks = Integer.parseInt(value("timer.ticks.atStop"));
        long elapsed = Long.parseLong(value("timer.ticks.elapsedMillis"));
        assertTrue(ticks >= 8, "fewer ticks than waited for: " + ticks);
        assertTrue(ticks <= elapsed / PERIOD + 1, ticks + " ticks in " + elapsed + " ms at a " + PERIOD + " ms period");
    }

    @Test
    public void stopOnTheEventThreadLetsNoFurtherTickThrough() {
        assertEquals("0", value("timer.ticks.afterStop"));
    }

    /**
     * {@code call_runnable_in_timer} freed the context and answered {@code FALSE}, removing the GLib source, in the
     * first tick after {@code _stop}: the facade's registry holds one more timer while it runs and is back to its
     * size afterwards, so no source keeps waking the loop for the life of the process.
     */
    @Test
    public void aStoppedTimerIsForgottenAtItsNextTick() {
        int before = Integer.parseInt(value("timer.registry.beforeStart"));
        assertEquals(Integer.toString(before + 1), value("timer.registry.running"));
        assertEquals(Integer.toString(before), value("timer.registry.afterStop"));
    }

    @Test
    public void timerSourceRunsAtHighIdlePriority() {
        assertEquals("p99,glass-timer,p101", value("timer.priorityOrder"));
    }

    @Test
    public void aThrowingRunnableIsReportedOnTheEventThreadAndTheTimerKeepsFiring() {
        assertTrue(Integer.parseInt(value("timer.throw.ticks")) >= 3, value("timer.throw.ticks"));
        assertEquals(value("timer.throw.ticks.atStop"), value("timer.throw.reported"));
        assertEquals("true", value("timer.throw.sameInstances"));
        assertEquals("true", value("timer.throw.reportedOnEventThread"));
        assertEquals("0", value("timer.throw.afterStop"));
    }

    /** Runs in {@link GtkGlassChild}. */
    static void timerScenario(Map<String, String> out) throws Exception {
        out.put("timer.minPeriod", Integer.toString(Timer.getMinPeriod()));
        out.put("timer.maxPeriod", Integer.toString(Timer.getMaxPeriod()));

        // Ticks: thread, rate, stop.
        AtomicInteger ticks = new AtomicInteger();
        AtomicBoolean offThread = new AtomicBoolean();
        Timer timer = GtkGlassChild.onFx(() -> Application.GetApplication().createTimer(() -> {
            ticks.incrementAndGet();
            if (!Application.isEventThread()) {
                offThread.set(true);
            }
        }));
        int registryBeforeStart = GtkGlassChild.onFx(GtkGlassShim::timerRegistrySize);
        long start = System.nanoTime();
        GtkGlassChild.onFx(() -> {
            timer.start(PERIOD);
            return null;
        });
        GtkGlassChild.waitFor(10_000, () -> ticks.get() >= 8);
        int registryRunning = GtkGlassChild.onFx(GtkGlassShim::timerRegistrySize);
        int atStop = GtkGlassChild.onFx(() -> {
            timer.stop();
            return ticks.get();
        });
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        Thread.sleep(10L * PERIOD);
        out.put("timer.ticks.allOnEventThread", Boolean.toString(!offThread.get()));
        out.put("timer.ticks.atStop", Integer.toString(atStop));
        out.put("timer.ticks.elapsedMillis", Long.toString(elapsed));
        out.put("timer.ticks.afterStop", Integer.toString(ticks.get() - atStop));
        out.put("timer.registry.beforeStart", Integer.toString(registryBeforeStart));
        out.put("timer.registry.running", Integer.toString(registryRunning));
        out.put("timer.registry.afterStop", Integer.toString(GtkGlassChild.onFx(GtkGlassShim::timerRegistrySize)));

        // Priority: three sources that all become ready while the loop is blocked run in priority order. They are
        // added in the reverse of that order, so that equal priorities (dispatched in insertion order) would show:
        // only a glass timer at exactly 100 gives "p99,glass-timer,p101".
        List<String> order = new CopyOnWriteArrayList<>();
        Timer prioritised = GtkGlassChild.onFx(() -> {
            AtomicBoolean first = new AtomicBoolean(true);
            Timer t = Application.GetApplication().createTimer(() -> {
                if (first.getAndSet(false)) {
                    order.add("glass-timer");
                }
            });
            GtkGlassShim.addGlibTimeout(101, 10, () -> order.add("p101"));
            t.start(10);
            GtkGlassShim.addGlibTimeout(99, 10, () -> order.add("p99"));
            Thread.sleep(150);
            return t;
        });
        GtkGlassChild.waitFor(10_000, () -> order.size() >= 3);
        GtkGlassChild.onFx(() -> {
            prioritised.stop();
            return null;
        });
        out.put("timer.priorityOrder", String.join(",", order));

        // A throwing Runnable.
        AtomicInteger throwTicks = new AtomicInteger();
        List<Throwable> thrown = new CopyOnWriteArrayList<>();
        int reportsBefore = GtkGlassChild.REPORTS.size();
        Timer thrower = GtkGlassChild.onFx(() -> Application.GetApplication().createTimer(() -> {
            RuntimeException e = new IllegalStateException("tick " + throwTicks.incrementAndGet());
            thrown.add(e);
            throw e;
        }));
        GtkGlassChild.onFx(() -> {
            thrower.start(PERIOD);
            return null;
        });
        GtkGlassChild.waitFor(10_000, () -> throwTicks.get() >= 3);
        int throwAtStop = GtkGlassChild.onFx(() -> {
            thrower.stop();
            return throwTicks.get();
        });
        Thread.sleep(10L * PERIOD);
        List<GtkGlassChild.Report> all = new ArrayList<>(GtkGlassChild.REPORTS);
        List<GtkGlassChild.Report> reports = all.subList(reportsBefore, all.size());
        boolean same = reports.size() == thrown.size();
        boolean onEventThread = true;
        for (int i = 0; i < reports.size() && same; i++) {
            same = reports.get(i).throwable() == thrown.get(i);
            onEventThread &= reports.get(i).onEventThread();
        }
        out.put("timer.throw.ticks", Integer.toString(throwTicks.get()));
        out.put("timer.throw.ticks.atStop", Integer.toString(throwAtStop));
        out.put("timer.throw.reported", Integer.toString(reports.size()));
        out.put("timer.throw.sameInstances", Boolean.toString(same));
        out.put("timer.throw.reportedOnEventThread", Boolean.toString(onEventThread));
        out.put("timer.throw.afterStop", Integer.toString(throwTicks.get() - throwAtStop));
    }
}
