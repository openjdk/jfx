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

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic pointer motions through the GTK glass event path: {@code XTestFakeMotionEvent} on a private X
 * connection, from a thread that is not the FX thread, over one window of a running toolkit. Every delivered motion
 * takes {@code process_events} -&gt; {@code Window.isEnabled} -&gt; {@code process_mouse_motion} -&gt;
 * {@code View.notifyMouse} -&gt; the view's handler: the two upcalls per event that the JNI boundary makes today.
 * <p>
 * Glass selects {@code GDK_POINTER_MOTION_HINT_MASK} (inside {@code GDK_ALL_EVENTS_MASK}): after each delivered
 * motion {@code gdk_event_request_motions} queries the pointer, and GDK drops every motion generated before that
 * query. So the view never sees a flood as such; two measurements, each the median of {@code -Dgtk.flood.rounds}
 * rounds (default 7) after one warm-up round:
 * <ul>
 * <li><b>paced</b>, the number to compare the replacement with: {@code -Dgtk.flood.paced} motions (default 2000),
 * each injected once the previous one reached the view and 200 us more (for the pointer query); a motion lost to
 * the hint anyway is injected again after 20 ms;</li>
 * <li><b>flood</b>: {@code -Dgtk.flood.events} motions (default 20000) injected as fast as XTest takes them; most are
 * dropped, and the X server is busy with the injection while the rest are delivered.</li>
 * </ul>
 * Per round: motions delivered and injected, delivered per second of wall time, the FX thread's CPU time and the
 * bytes it allocated per delivered motion - both also with the FX thread's rate over an idle 500 ms just before the
 * round subtracted, because the toolkit's pulse keeps running. The CPU time per motion is the figure least disturbed
 * by the X server.
 * <p>
 * Then the paced rounds once more, recorded as {@code manyPeers.*}, after {@code -Dgtk.flood.manyPeers} windows with
 * a view (default 300) have been created and closed: every window and view id is then above 127, beyond the ids that
 * {@code Long.valueOf} caches, which is where a registry lookup with a boxed id would allocate on the event path. A
 * measurement, not a pass/fail test, except that every round must deliver motions and that the idle-corrected median
 * of the bytes allocated per motion with those ids must stay below one boxed id ({@link #MAX_BYTES}).
 */
@EnabledOnOs(OS.LINUX)
@Timeout(900)
public class GtkMotionFloodBenchmarkTest {

    static final String EVENTS_PROPERTY = "gtk.flood.events";
    static final String PACED_PROPERTY = "gtk.flood.paced";
    static final String ROUNDS_PROPERTY = "gtk.flood.rounds";
    static final String MANY_PEERS_PROPERTY = "gtk.flood.manyPeers";

    /**
     * The ceiling of the idle-corrected bytes the FX thread may allocate per motion with window and view ids above
     * 127: half of the 16 bytes of one boxed {@code Long}. The JNI build of commit {@code 033187ad90} allocated
     * nothing per motion.
     */
    static final double MAX_BYTES = 8.0;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(3 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runFlood() {
        GtkGlassChildJvm.requireRobot();
        run = GtkGlassChildJvm.run(GtkMotionFloodBenchmarkTest.class, "floodScenario", List.of(
                "-D" + EVENTS_PROPERTY + "=" + Integer.getInteger(EVENTS_PROPERTY, 20_000),
                "-D" + PACED_PROPERTY + "=" + Integer.getInteger(PACED_PROPERTY, 2_000),
                "-D" + ROUNDS_PROPERTY + "=" + Integer.getInteger(ROUNDS_PROPERTY, 7),
                "-D" + MANY_PEERS_PROPERTY + "=" + Integer.getInteger(MANY_PEERS_PROPERTY, 300)));
        System.out.println("GTK motion benchmark (" + System.getProperty("java.vm.name") + " "
                + System.getProperty("java.version") + ")");
        for (Map.Entry<String, String> entry : run.values().entrySet()) {
            if (entry.getKey().startsWith("paced.") || entry.getKey().startsWith("flood.")
                    || entry.getKey().startsWith("manyPeers.")) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    @Test
    public void everyRoundDeliversMotions() {
        int rounds = Integer.parseInt(run.values().get("rounds"));
        assertTrue(rounds >= 1, run.describe());
        for (String mode : List.of("paced", "flood", "manyPeers.paced")) {
            for (int i = 1; i <= rounds; i++) {
                String round = run.values().get(mode + ".round." + i);
                assertTrue(round != null && !round.startsWith("delivered=0 "), mode + " round " + i + ": " + round);
            }
            assertTrue(Double.parseDouble(run.values().get(mode + ".median.eventsPerSecond")) > 0, run.describe());
        }
    }

    /**
     * With every window and view id above 127, the event path still allocates nothing per motion: the registry
     * lookups of the window and view slots take the id as a primitive.
     */
    @Test
    public void highPeerIdsAllocateNothingPerMotion() {
        String ids = run.values().get("manyPeers.ids");
        assertTrue(ids != null && ids.matches("[0-9]+/[0-9]+"), run.describe());
        for (String id : ids.split("/")) {
            assertTrue(Long.parseLong(id) > 127, "ids " + ids);
        }
        double bytes = Double.parseDouble(run.values().get("manyPeers.paced.median.bytesPerEventIdleCorrected"));
        assertTrue(bytes < MAX_BYTES, "window/view ids " + ids + ": " + bytes + " bytes per motion (idle-corrected"
                + " median); the JNI build allocated nothing per motion");
    }

    // ---------------------------------------------------------------------------------------------
    // The measurement (child JVM)
    // ---------------------------------------------------------------------------------------------

    /**
     * The view's handler: counts motions and remembers when the last one arrived. Written on the FX thread only and
     * read by the injecting thread directly - a round trip through the FX thread would allocate there.
     */
    static final class Counter extends View.EventHandler {
        volatile long moves;
        volatile long lastNanos;

        @Override
        public void handleMouseEvent(View view, long time, int type, int button, int x, int y, int xAbs,
                                     int yAbs, int modifiers, boolean isPopupTrigger, boolean isSynthesized) {
            if (type == MouseEvent.MOVE) {
                lastNanos = System.nanoTime();
                moves = moves + 1;
            }
        }
    }

    /** What one round measured; per-event figures are per delivered motion. */
    record Round(long delivered, long injected, double seconds, double cpuMicros, double correctedCpuMicros,
                 double bytes, double correctedBytes) {

        double eventsPerSecond() {
            return delivered / seconds;
        }

        String describe() {
            return String.format(Locale.ROOT, "delivered=%d injected=%d seconds=%.4f eventsPerSecond=%.1f"
                    + " fxCpuMicrosPerEvent=%.2f (idle-corrected %.2f) bytesPerEvent=%.1f (idle-corrected %.1f)",
                    delivered, injected, seconds, eventsPerSecond(), cpuMicros, correctedCpuMicros, bytes,
                    correctedBytes);
        }
    }

    /** Runs in {@link GtkGlassChild}. */
    static void floodScenario(Map<String, String> out) throws Exception {
        int events = Integer.getInteger(EVENTS_PROPERTY, 20_000);
        int paced = Integer.getInteger(PACED_PROPERTY, 2_000);
        int rounds = Integer.getInteger(ROUNDS_PROPERTY, 7);
        int manyPeers = Integer.getInteger(MANY_PEERS_PROPERTY, 300);
        out.put("rounds", Integer.toString(rounds));
        measure(out, "", rounds, measure -> {
            report(out, "paced", rounds, () -> measure.paced(paced));
            report(out, "flood", rounds, () -> measure.flood(events));
        });
        GtkGlassChild.onFx(() -> {
            for (int i = 0; i < manyPeers; i++) {
                Window w = Application.GetApplication().createWindow(null, Screen.getMainScreen(), Window.UNTITLED);
                w.setView(Application.GetApplication().createView());
                w.close();
            }
            return null;
        });
        measure(out, "manyPeers.", rounds, measure -> report(out, "manyPeers.paced", rounds,
                () -> measure.paced(paced)));
    }

    @FunctionalInterface
    interface Rounds {
        void run(Measure measure) throws Exception;
    }

    /**
     * Opens the measured window, with a {@link Counter} as its view's handler, and a private X connection, puts the
     * pointer into the window and runs {@code rounds} on them; records the window and view ids under
     * {@code <prefix>ids}.
     */
    private static void measure(Map<String, String> out, String prefix, int rounds, Rounds body) throws Exception {
        Counter counter = new Counter();
        Window window = GtkGlassChild.onFx(() -> {
            Window w = Application.GetApplication().createWindow(null, Screen.getMainScreen(), Window.UNTITLED);
            View view = Application.GetApplication().createView();
            view.setEventHandler(counter);
            w.setView(view);
            w.setBounds(100, 100, true, true, -1, -1, 400, 400, 0, 0);
            w.setVisible(true);
            out.put(prefix + "ids", GtkGlassShim.windowId(w) + "/" + GtkGlassShim.viewId(view));
            return w;
        });
        long fxThread = GtkGlassChild.onFx(() -> Thread.currentThread().threadId());
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long display = GtkGlassShim.openDisplay();
        if (display == 0) {
            throw new IllegalStateException("XOpenDisplay failed");
        }
        try {
            // the pointer into the window first, so that every motion lands in it
            GtkGlassShim.fakeMotion(display, 300, 300);
            GtkGlassShim.syncDisplay(display);
            Thread.sleep(500);
            body.run(new Measure(counter, threads, fxThread, display));
        } finally {
            GtkGlassShim.closeDisplay(display);
            GtkGlassChild.onFx(() -> {
                window.close();
                return null;
            });
        }
    }

    @FunctionalInterface
    interface RoundBody {
        Round run() throws Exception;
    }

    /** One warm-up round and {@code rounds} measured ones; records each and the medians under {@code mode.*}. */
    private static void report(Map<String, String> out, String mode, int rounds, RoundBody body) throws Exception {
        body.run();
        List<Round> measured = new ArrayList<>();
        for (int i = 1; i <= rounds; i++) {
            Round round = body.run();
            measured.add(round);
            out.put(mode + ".round." + i, round.describe());
        }
        out.put(mode + ".median.delivered", format("%.0f", median(measured, Round::delivered)));
        out.put(mode + ".median.eventsPerSecond", format("%.1f", median(measured, Round::eventsPerSecond)));
        out.put(mode + ".median.fxCpuMicrosPerEvent", format("%.2f", median(measured, Round::cpuMicros)));
        out.put(mode + ".median.fxCpuMicrosPerEventIdleCorrected",
                format("%.2f", median(measured, Round::correctedCpuMicros)));
        out.put(mode + ".median.bytesPerEvent", format("%.2f", median(measured, Round::bytes)));
        out.put(mode + ".median.bytesPerEventIdleCorrected", format("%.2f", median(measured, Round::correctedBytes)));
    }

    private static String format(String pattern, double value) {
        return String.format(Locale.ROOT, pattern, value);
    }

    private static double median(List<Round> rounds, ToDoubleFunction<Round> field) {
        double[] sorted = rounds.stream().mapToDouble(field).toArray();
        Arrays.sort(sorted);
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2]
                : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2;
    }

    /** The two kinds of round, on one window and one private X connection. */
    private record Measure(Counter counter, com.sun.management.ThreadMXBean threads, long fxThread, long display) {

        /** The FX thread's CPU nanoseconds and allocated bytes per wall nanosecond over an idle 500 ms. */
        private double[] idleRates() throws InterruptedException {
            long start = System.nanoTime();
            long cpu = threads.getThreadCpuTime(fxThread);
            long bytes = threads.getThreadAllocatedBytes(fxThread);
            Thread.sleep(500);
            double wall = System.nanoTime() - start;
            return new double[] {(threads.getThreadCpuTime(fxThread) - cpu) / wall,
                (threads.getThreadAllocatedBytes(fxThread) - bytes) / wall};
        }

        private Round round(long delivered, long injected, double[] idle, long start, long end, double seconds,
                            long cpu0, long bytes0) {
            long cpu = threads.getThreadCpuTime(fxThread) - cpu0;
            long bytes = threads.getThreadAllocatedBytes(fxThread) - bytes0;
            double n = Math.max(1, delivered);
            double wall = end - start;
            return new Round(delivered, injected, seconds, cpu / n / 1000, (cpu - idle[0] * wall) / n / 1000,
                    bytes / n, (bytes - idle[1] * wall) / n);
        }

        /**
         * {@code count} delivered motions, each injected once the previous one reached the view and 200 us more; a
         * motion that has not arrived after 20 ms is injected again (it was generated before GDK's pointer query).
         */
        Round paced(int count) throws Exception {
            double[] idle = idleRates();
            long before = counter.moves;
            long cpu0 = threads.getThreadCpuTime(fxThread);
            long bytes0 = threads.getThreadAllocatedBytes(fxThread);
            long start = System.nanoTime();
            long injected = 0;
            for (int i = 0; i < count; i++) {
                long target = before + i + 1;
                for (int attempt = 0; attempt < 100 && counter.moves < target; attempt++) {
                    GtkGlassShim.fakeMotion(display, 200 + (int) (injected & 1), 250);
                    GtkGlassShim.flushDisplay(display);
                    injected++;
                    long deadline = System.nanoTime() + 20_000_000L;
                    while (counter.moves < target && System.nanoTime() < deadline) {
                        Thread.onSpinWait();
                    }
                }
                if (counter.moves < target) {
                    break;
                }
                long settle = System.nanoTime() + 200_000L;
                while (System.nanoTime() < settle) {
                    Thread.onSpinWait();
                }
            }
            long end = System.nanoTime();
            return round(counter.moves - before, injected, idle, start, end, (end - start) / 1e9, cpu0, bytes0);
        }

        /** {@code count} motions as fast as XTest takes them, flushed every 64; then 500 ms without a delivery. */
        Round flood(int count) throws Exception {
            double[] idle = idleRates();
            long before = counter.moves;
            long cpu0 = threads.getThreadCpuTime(fxThread);
            long bytes0 = threads.getThreadAllocatedBytes(fxThread);
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                GtkGlassShim.fakeMotion(display, 150 + (i % 200), (i & 1) == 0 ? 200 : 300);
                if ((i & 63) == 63) {
                    GtkGlassShim.flushDisplay(display);
                }
            }
            GtkGlassShim.syncDisplay(display);
            long seen = -1;
            long quietSince = System.nanoTime();
            while (System.nanoTime() - quietSince < 500_000_000L) {
                Thread.sleep(5);
                long now = counter.moves;
                if (now != seen) {
                    seen = now;
                    quietSince = System.nanoTime();
                }
            }
            long end = System.nanoTime();
            return round(seen - before, count, idle, start, end, (counter.lastNanos - start) / 1e9, cpu0, bytes0);
        }
    }
}
