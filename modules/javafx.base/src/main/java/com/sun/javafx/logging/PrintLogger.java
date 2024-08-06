/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.logging;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logs information on a per-pulse basis. When doing performance analysis, a very
 * easy thing to start with is to run with the PulseLogger enabled, such that various
 * statistics related to the scene graph and the pulse are recorded and dumped to
 * the log.
 * <p>
 * The pulse logger is designed in such a way as to gather all of the pulse statistics
 * together even though half of the pulse occurs on the FX thread and half on the
 * render thread, and therefore two sets of pulse data are being accumulated
 * concurrently. The {@code pulseStart}, {@code pulseEnd}, {@code renderStart},
 * and {@code renderEnd} methods must be called appropriately by the runtime
 * to ensure that the logging system works correctly.
 */
class PrintLogger extends Logger {

    /**
     * A time in milliseconds which defines the threshold. If a pulse lasts <em>longer</em> than
     * the threshold, then it is logged, otherwise an abbreviated representation including
     * only the time of the pulse is logged.
     */
    @SuppressWarnings("removal")
    private static long THRESHOLD = AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer.getInteger("javafx.pulseLogger.threshold", 17));

    /**
     * Optionally exit after a given number of pulses
     */
    @SuppressWarnings("removal")
    private static final int EXIT_ON_PULSE =
            AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer.getInteger("javafx.pulseLogger.exitOnPulse", 0));

    /**
     * We have a simple counter that keeps track of the current pulse number.
     * INTER_PULSE_DATA is used to mark data that comes between pulses.
     */
    private int pulseCount = 1;
    private static final int INTER_PULSE_DATA = -1;

    /**
     * When printing the truncated form of the pulse, we just print one truncated
     * form after another, such as:
     *
     * [5][2][4]
     *
     * This way we don't cause the console to scroll far vertically in the case of fast
     * pulses. We do this so that relevant information (pulses that exceed the threshold)
     * is easy to find and remains visible as long as possible in the console. However,
     * we don't want to scroll too far off to the right either, so we keep track of
     * how many "quick pulses" have happened in a row. When we've exceeded some fixed
     * number (20, say) then we will insert a newline into the log.
     */
    private volatile int wrapCount = 0;

    /**
     * References to PulseData for the FX thread (fxData) and the Render thread (renderData).
     */
    private volatile PulseData fxData, renderData;

    /**
     * Keeps track of the start of the previous pulse, such that we can print out
     * the time interval between the start of pulses.
     */
    private long lastPulseStartTime;

    class ThreadLocalData {
        String  phaseName;
        long    phaseStart;
    }

    private Thread fxThread;
    private final ThreadLocal<ThreadLocalData> phaseData =
        new ThreadLocal<>() {
            @Override
            public ThreadLocalData initialValue() {
                return new ThreadLocalData();
            }
        };


    /**
     * The queue of all PulseData objects, both available and those in use.
     * New PulseData objects are allocated from head if the state is AVAILABLE.
     * They are re-linked at tail with the state INCOMPLETE. Once fully processed
     * they will change their state back to AVAILABLE and will become ready for reuse.
     */
    private PulseData head;
    private PulseData tail;

    /**
     * A synchronization object for printing arbitrage.
     */
    private AtomicInteger active;

    /**
     * PulseData object states
     */
    private static final int AVAILABLE = 0;
    private static final int INCOMPLETE = 1;
    private static final int COMPLETE = 2;

    /**
     * Disallow instantiation.
     */
    private PrintLogger() {
        head = new PulseData();
        tail = new PulseData();
        head.next = tail;
        active = new AtomicInteger(0);
    }

    public static Logger createInstance() {
        boolean enabled = PulseLogger.isPulseLoggingRequested();
        if (enabled) {
            return new PrintLogger();
        }
        return null;
    }

    /**
     * Allocate and initialize a PulseData object
     */
    private PulseData allocate(int n) {
        PulseData res;
        if (head != tail && head.state == AVAILABLE) {
            res = head;
            head = head.next;
            res.next = null;
        }
        else {
            res = new PulseData();
        }
        tail.next = res;
        tail = res;
        res.init(n);
        return res;
    }

    /**
     * <strong>MUST</strong> be called at the start of every pulse.
     * This method will initialize the fxData buffer so that subsequent
     * calls to fxMessage will write to this buffer.
     */
    @Override
    public void pulseStart() {
        if (fxThread == null) {
            fxThread = Thread.currentThread();
        }
        if (fxData != null) {
            // Inter pulse data
            fxData.state = COMPLETE;
            if (active.incrementAndGet() == 1) {
                fxData.printAndReset();
                active.decrementAndGet();
            }
        }
        fxData = allocate(pulseCount++);
        if (lastPulseStartTime > 0) {
            fxData.interval = (fxData.startTime - lastPulseStartTime)/1000000L;
        }
        lastPulseStartTime = fxData.startTime;
    }

    /**
     * <strong>Must</strong> be called before any set of render jobs
     * for a given pulse begin. This method will initialize the
     * renderData buffer so that subsequent calls to renderMessage
     * will write to this buffer. I have found that sometimes renderMessage
     * is called without a pulse being started. Such cases are exceptional
     * and appear to happen only at startup, and such cases are simply not
     * logged.
     */
    @Override
    public void renderStart() {
        newPhase(null); // finish the current phase on the FX thread
        fxData.pushedRender = true;
        renderData = fxData;
        active.incrementAndGet();
    }

    /**
     * <strong>Must</strong> be called at the end of the pulse. If
     * there was no rendering started during this pulse, then this
     * method will cause the pulse data to be logged. Otherwise, the
     * pulse data is logged when rendering is ended. However, as soon
     * as pulseEnd is called, we are ready for another call to pulseStart.
     */
    @Override
    public void pulseEnd() {
        if (fxData != null && !fxData.pushedRender) {
            fxData.state = COMPLETE;
            if (active.incrementAndGet() == 1) {
                fxData.printAndReset();
                active.decrementAndGet();
            }
        }
        fxData = null;
    }

    /**
     * <strong>Must</strong> be called at the end of rendering, if a previous
     * call to {@link #renderStart()} had been made. This will cause the pulse
     * data to be logged.
     */
    @Override
    public void renderEnd() {
        newPhase(null); // finish the current phase on the render thread
        renderData.state = COMPLETE;
        for (;;) {
            renderData.printAndReset();
            if (active.decrementAndGet() == 0) {
                break;
            }
            renderData = renderData.next;
        }
        renderData = null;
    }

    /**
     * Adds a message to the log for the pulse.
     * @param message The message to log. A newline will be added automatically.
     */
    @Override
    public void addMessage(String message) {
        PulseData pulseData;
        if (fxThread == null || Thread.currentThread() == fxThread) {
            if (fxData == null) {
                fxData = allocate(INTER_PULSE_DATA);
            }
            pulseData = fxData;
        }
        else {
            pulseData = renderData;
        }
        if (pulseData == null) {
            return;
        }
        pulseData.message
            .append("T")
            .append(Thread.currentThread().threadId())
            .append(" : ")
            .append(message)
            .append("\n");
    }

    /**
     * Increments the given named per-pulse counter.
     * @param counter The name for the counter.
     */
    @Override
    public void incrementCounter(String counter) {
        PulseData pulseData;
        if (fxThread == null || Thread.currentThread() == fxThread) {
            if (fxData == null) {
                fxData = allocate(INTER_PULSE_DATA);
            }
            pulseData = fxData;
        }
        else {
            pulseData = renderData;
        }
        if (pulseData == null) {
            return;
        }
        Map<String,Counter> counters = pulseData.counters;
        Counter cval = counters.get(counter);
        if (cval == null) {
            cval = new Counter();
            counters.put(counter, cval);
        }
        cval.value += 1;
    }

    @Override
    public void newPhase(String name) {
        long curTime = System.nanoTime();

        ThreadLocalData curPhase = phaseData.get();
        if (curPhase.phaseName != null) {
            PulseData pulseData = Thread.currentThread() == fxThread ? fxData : renderData;
            if (pulseData != null) {
                pulseData.message
                    .append("T")
                    .append(Thread.currentThread().threadId())
                    .append(" (").append((curPhase.phaseStart-pulseData.startTime)/1000000L)
                    .append(" +").append((curTime - curPhase.phaseStart)/1000000L).append("ms): ")
                    .append(curPhase.phaseName)
                    .append("\n");
            }
        }
        curPhase.phaseName = name;
        curPhase.phaseStart = curTime;
    }

    /**
     *  A mutable integer to be used in the counter map
     */
    private static class Counter {
        int     value;
    }

    /**
     * The data we collect per pulse. We store the pulse number
     * associated with this pulse, along with what time it
     * started at and the interval since the previous pulse.
     * We also maintain the message buffer and counters.
     */
    private final class PulseData {
        PulseData next;
        volatile int state = AVAILABLE;
        long startTime;
        long interval;
        int pulseCount;
        boolean pushedRender;
        StringBuffer message = new StringBuffer();
        Map<String,Counter> counters = new ConcurrentHashMap<>();

        void init(int n) {
            state = INCOMPLETE;
            pulseCount = n;
            startTime = System.nanoTime();
            interval = 0;
            pushedRender = false;
        }

        void printAndReset() {
            long endTime = System.nanoTime();
            long totalTime = (endTime - startTime)/1000000L;

            if (state != COMPLETE) {
                System.err.println("\nWARNING: logging incomplete state");
            }

            if (totalTime <= THRESHOLD) {
                // Don't print inter pulse data
                if (pulseCount != INTER_PULSE_DATA) {
                    System.err.print((wrapCount++ % 10 == 0 ? "\n[" : "[") + pulseCount+ " " + interval + "ms:" + totalTime + "ms]");
                }
            }
            else {
                if (pulseCount == INTER_PULSE_DATA) {
                    System.err.println("\n\nINTER PULSE LOG DATA");
                }
                else {
                    System.err.print("\n\nPULSE: " + pulseCount +
                            " [" + interval + "ms:" + totalTime + "ms]");
                    if (!pushedRender) {
                        System.err.print(" Required No Rendering");
                    }
                    System.err.println();
                }
                System.err.print(message);
                if (!counters.isEmpty()) {
                    System.err.println("Counters:");
                    List<Map.Entry<String,Counter>> entries = new ArrayList<>(counters.entrySet());
                    Collections.sort(entries, (a, b) -> a.getKey().compareTo(b.getKey()));
                    for (Map.Entry<String, Counter> entry : entries) {
                        System.err.println("\t" + entry.getKey() + ": " + entry.getValue().value);
                    }
                }
                wrapCount = 0;
            }

            // Reset the state
            message.setLength(0);
            counters.clear();
            state = AVAILABLE;
            if (EXIT_ON_PULSE > 0 && pulseCount >= EXIT_ON_PULSE) {
                System.err.println("Exiting after pulse #" + pulseCount);
                System.exit(0);
            }
        }
    }
}
