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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GtkApplication._submitForLaterInvocation}, {@code enterNestedEventLoopImpl} and
 * {@code leaveNestedEventLoopImpl} on a running toolkit: what {@code GlassApplication.cpp} did at commit
 * {@code 033187ad90} ({@code call_runnable}, {@code gdk_threads_add_idle_full(G_PRIORITY_HIGH_IDLE + 30, ...)},
 * {@code gtk_main}, {@code gtk_main_quit}), pinned so that the {@code java.lang.foreign} replacement can be held
 * to it.
 * <ul>
 * <li>runnables handed to the peer from another thread run on the FX thread in submission order, at priority 130 -
 * after a GLib idle at 129 and before one at 131 that were added in the reverse order;</li>
 * <li>a runnable that throws is reported to the FX thread's uncaught-exception handler with the thrown instance
 * ({@code LOG_EXCEPTION}), and the next one still runs - through the peer directly and through
 * {@code Platform.runLater};</li>
 * <li>nested event loops three deep return the values their exits passed, in order; timer ticks, runnables and
 * exception reports keep flowing inside a nested loop.</li>
 * </ul>
 * All of it runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkInvokeLaterNativeTest {

    private static final int RUNNABLES = 200;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkInvokeLaterNativeTest.class, "invokeLaterScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void runnablesRunOnTheEventThreadInSubmissionOrder() {
        assertEquals(Integer.toString(RUNNABLES), value("invoke.fifo.count"));
        assertEquals("true", value("invoke.fifo.inOrder"));
        assertEquals("true", value("invoke.fifo.allOnEventThread"));
    }

    @Test
    public void idleSourceRunsAtHighIdlePriorityPlusThirty() {
        assertEquals("p129,glass-idle,p131", value("invoke.priorityOrder"));
    }

    @Test
    public void aThrowingRunnableIsReportedAndTheNextOneStillRuns() {
        assertEquals("1", value("invoke.throw.peer.reported"));
        assertEquals("true", value("invoke.throw.peer.sameInstance"));
        assertEquals("true", value("invoke.throw.peer.onEventThread"));
        assertEquals("true", value("invoke.throw.peer.nextRan"));
        assertEquals("1", value("invoke.throw.runLater.reported"));
        assertEquals("true", value("invoke.throw.runLater.sameInstance"));
        assertEquals("true", value("invoke.throw.runLater.nextRan"));
    }

    /** The C took a global reference to {@code NULL}; {@code CallVoidMethod} on it left a reported NPE. */
    @Test
    public void aNullRunnableIsReportedAsANullPointerException() {
        assertEquals("true", value("invoke.null.nextRan"));
        assertEquals("1", value("invoke.null.reported"));
        assertEquals("java.lang.NullPointerException", value("invoke.null.reportedType"));
        assertEquals("null", value("invoke.null.reportedMessage"));
    }

    @Test
    public void nestedEventLoopsReturnTheirExitValuesInOrder() {
        assertEquals("enter1,enter2,enter3,leave3,ret3=v3,ret2=v2,ret1=v1", value("nested.trace"));
        assertEquals("false", value("nested.runningAfter"));
    }

    @Test
    public void timerRunnablesAndReportsKeepFlowingInsideANestedLoop() {
        assertTrue(Integer.parseInt(value("nested.inner.timerTicks")) >= 3, value("nested.inner.timerTicks"));
        assertEquals("true", value("nested.inner.peerRunnableRan"));
        assertEquals("1", value("nested.inner.reported"));
        assertEquals("v", value("nested.inner.returned"));
    }

    /** Runs in {@link GtkGlassChild}. */
    static void invokeLaterScenario(Map<String, String> out) throws Exception {
        // FIFO from a foreign thread, straight to the peer.
        List<Integer> seen = new CopyOnWriteArrayList<>();
        AtomicBoolean offThread = new AtomicBoolean();
        CountDownLatch all = new CountDownLatch(RUNNABLES);
        for (int i = 0; i < RUNNABLES; i++) {
            int index = i;
            GtkGlassShim.submitForLaterInvocation(() -> {
                seen.add(index);
                if (!Application.isEventThread()) {
                    offThread.set(true);
                }
                all.countDown();
            });
        }
        all.await(30, TimeUnit.SECONDS);
        out.put("invoke.fifo.count", Integer.toString(seen.size()));
        out.put("invoke.fifo.inOrder", Boolean.toString(
                seen.equals(IntStream.range(0, RUNNABLES).boxed().collect(Collectors.toList()))));
        out.put("invoke.fifo.allOnEventThread", Boolean.toString(!offThread.get()));

        // Priority, with the three sources added in the reverse of their dispatch order.
        List<String> order = new CopyOnWriteArrayList<>();
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.addGlibIdle(131, () -> order.add("p131"));
            GtkGlassShim.submitForLaterInvocation(() -> order.add("glass-idle"));
            GtkGlassShim.addGlibIdle(129, () -> order.add("p129"));
            return null;
        });
        GtkGlassChild.waitFor(10_000, () -> order.size() >= 3);
        out.put("invoke.priorityOrder", String.join(",", order));

        // A throwing runnable, through the peer and through Platform.runLater.
        recordThrow(out, "invoke.throw.peer", GtkGlassShim::submitForLaterInvocation);
        recordThrow(out, "invoke.throw.runLater", Platform::runLater);

        // A null runnable handed to the peer.
        int reportsBeforeNull = GtkGlassChild.REPORTS.size();
        CountDownLatch afterNull = new CountDownLatch(1);
        GtkGlassShim.submitForLaterInvocation(null);
        GtkGlassShim.submitForLaterInvocation(afterNull::countDown);
        out.put("invoke.null.nextRan", Boolean.toString(afterNull.await(10, TimeUnit.SECONDS)));
        GtkGlassChild.waitFor(2_000, () -> GtkGlassChild.REPORTS.size() > reportsBeforeNull);
        List<GtkGlassChild.Report> allReports = new ArrayList<>(GtkGlassChild.REPORTS);
        List<GtkGlassChild.Report> nullReports = allReports.subList(reportsBeforeNull, allReports.size());
        out.put("invoke.null.reported", Integer.toString(nullReports.size()));
        out.put("invoke.null.reportedType", nullReports.isEmpty() ? "<none>"
                : nullReports.get(0).throwable().getClass().getName());
        out.put("invoke.null.reportedMessage", nullReports.isEmpty() ? "<none>"
                : String.valueOf(nullReports.get(0).throwable().getMessage()));

        // Nested loops three deep.
        List<String> trace = new CopyOnWriteArrayList<>();
        CountDownLatch nestedDone = new CountDownLatch(1);
        Platform.runLater(() -> {
            trace.add("enter1");
            Platform.runLater(() -> {
                trace.add("enter2");
                Platform.runLater(() -> {
                    trace.add("enter3");
                    Platform.runLater(() -> {
                        trace.add("leave3");
                        Platform.exitNestedEventLoop("k3", "v3");
                    });
                    trace.add("ret3=" + Platform.enterNestedEventLoop("k3"));
                    Platform.exitNestedEventLoop("k2", "v2");
                });
                trace.add("ret2=" + Platform.enterNestedEventLoop("k2"));
                Platform.exitNestedEventLoop("k1", "v1");
            });
            trace.add("ret1=" + Platform.enterNestedEventLoop("k1"));
            nestedDone.countDown();
        });
        nestedDone.await(30, TimeUnit.SECONDS);
        out.put("nested.trace", String.join(",", trace));
        out.put("nested.runningAfter", Boolean.toString(GtkGlassChild.onFx(Platform::isNestedLoopRunning)));

        // Inside one nested loop: a timer ticks, a peer runnable runs, a throwing runnable is reported.
        AtomicInteger ticks = new AtomicInteger();
        AtomicBoolean peerRan = new AtomicBoolean();
        int reportsBefore = GtkGlassChild.REPORTS.size();
        CountDownLatch innerDone = new CountDownLatch(1);
        List<Object> returned = new CopyOnWriteArrayList<>();
        Platform.runLater(() -> {
            Timer timer = Application.GetApplication().createTimer(ticks::incrementAndGet);
            timer.start(10);
            Platform.runLater(() -> {
                GtkGlassShim.submitForLaterInvocation(() -> peerRan.set(true));
                GtkGlassShim.submitForLaterInvocation(() -> {
                    throw new IllegalStateException("inside the nested loop");
                });
                GtkGlassShim.submitForLaterInvocation(() -> new Thread(() -> {
                    try {
                        GtkGlassChild.waitFor(10_000, () -> ticks.get() >= 5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    Platform.runLater(() -> Platform.exitNestedEventLoop("inner", "v"));
                }).start());
            });
            returned.add(Platform.enterNestedEventLoop("inner"));
            timer.stop();
            innerDone.countDown();
        });
        innerDone.await(30, TimeUnit.SECONDS);
        out.put("nested.inner.timerTicks", Integer.toString(ticks.get()));
        out.put("nested.inner.peerRunnableRan", Boolean.toString(peerRan.get()));
        out.put("nested.inner.reported", Integer.toString(GtkGlassChild.REPORTS.size() - reportsBefore));
        out.put("nested.inner.returned", returned.isEmpty() ? "<none>" : String.valueOf(returned.get(0)));
    }

    private interface Submitter {
        void submit(Runnable runnable);
    }

    private static void recordThrow(Map<String, String> out, String prefix, Submitter submitter) throws Exception {
        int reportsBefore = GtkGlassChild.REPORTS.size();
        RuntimeException thrown = new IllegalStateException(prefix);
        CountDownLatch next = new CountDownLatch(1);
        submitter.submit(() -> {
            throw thrown;
        });
        submitter.submit(next::countDown);
        boolean nextRan = next.await(10, TimeUnit.SECONDS);
        GtkGlassChild.waitFor(2_000, () -> GtkGlassChild.REPORTS.size() > reportsBefore);
        List<GtkGlassChild.Report> all = new ArrayList<>(GtkGlassChild.REPORTS);
        List<GtkGlassChild.Report> reports = all.subList(reportsBefore, all.size());
        out.put(prefix + ".reported", Integer.toString(reports.size()));
        out.put(prefix + ".sameInstance", Boolean.toString(!reports.isEmpty() && reports.get(0).throwable() == thrown));
        out.put(prefix + ".onEventThread", Boolean.toString(!reports.isEmpty() && reports.get(0).onEventThread()));
        out.put(prefix + ".nextRan", Boolean.toString(nextRan));
    }
}
