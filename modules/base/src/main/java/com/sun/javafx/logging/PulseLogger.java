/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Comparator;
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
public class PulseLogger {
    /**
     * Specifies whether pulse logging is enabled or not. This is set via
     * a command line flag and defaults to false.
     */
    public static final boolean PULSE_LOGGING_ENABLED =
            AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override public Boolean run() {
                    return Boolean.getBoolean("javafx.pulseLogger");
                }
            });

    /**
     * A reference to the pulse logger. This will be null if pulse logging
     * is not enabled.
     */
    public static final PulseLogger PULSE_LOGGER = PULSE_LOGGING_ENABLED ? new PulseLogger() : null;

    /**
     * A time in milliseconds which defines the threshold. If a pulse lasts <em>longer</em> than
     * the threshold, then it is logged, otherwise an abbreviated representation including
     * only the time of the pulse is logged.
     */
    private static long THRESHOLD = (long)
            AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override public Integer run() {
                    return Integer.getInteger("javafx.pulseLogger.threshold", 17);
                }
            });

    /**
     * Optionally exit after a given number of pulses
     */
    private static final int EXIT_ON_PULSE =
            AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override
                public Integer run() {
                    return Integer.getInteger("javafx.pulseLogger.exitOnPulse", 0);
                }
            });

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

    /**
     * No logger activity is expected outside the renderStart..renderEnd interval.
     * This flag is for notification when that assumption is broken.
     */
    private boolean nullRenderFlag = false;

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
    AtomicInteger active = new AtomicInteger(0);
    
    /**
     * PulseData object states
     */
    private static final int AVAILABLE = 0;
    private static final int INCOMPLETE = 1;
    private static final int COMPLETE = 2;
    
    /**
     * Disallow instantiation.
     */
    private PulseLogger() {
        head = new PulseData();
        tail = new PulseData();
        head.next = tail;
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
    public void pulseStart() {
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
            fxData.interval = fxData.startTime - lastPulseStartTime;
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
    public void renderStart() {
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
    public void pulseEnd() {
        if (!fxData.pushedRender) {
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
    public void renderEnd() {
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
     * Adds a message to the log for the pulse. This method <strong>MUST ONLY
     * BE CALLED FROM THE FX THREAD</strong>.
     * 
     * @param message The message to log. A newline will be added automatically.
     */
    public void fxMessage(String message) {
        if (fxData == null) {
            fxData = allocate(INTER_PULSE_DATA);
        }
        fxData.message
            .append("T")
            .append(Thread.currentThread().getId())
            .append(" : ")
            .append(message)
            .append("\n");
    }

    /**
     * Adds a timing message to the log for the pulse. This method <strong>MUST ONLY
     * BE CALLED FROM THE FX THREAD</strong>. The milliseconds will be computed and
     * added to the message. A newline will be added to the message.
     * <p>
     * This is typically used as follows:
     * 
     * long start = PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
     * operationToBeTimed();
     * if (PULSE_LOGGING_ENABLED) {
     *     PULSE_LOGGER.fxMessage(start, System.currentTimeMillis(), "Operation Identifier");
     * }
     * 
     * @param start The time in milliseconds when this operation started
     * @param end The time in milliseconds when the operation ended
     * @param message The message to log. A newline will be added automatically.
     */
    public void fxMessage(long start, long end, String message) {
        if (fxData == null) {
            fxData = allocate(INTER_PULSE_DATA);
        }
        fxData.message
            .append("T")
            .append(Thread.currentThread().getId())
            .append(" (").append(start-fxData.startTime).append(" +").append(end - start).append("ms): ")
            .append(message)
            .append("\n");
    }

    /**
     * Increments the given named per-pulse counter. This method <strong>MUST
     * ONLY BE CALLED ON THE FX THREAD</strong>.
     * @param counter The name for the counter.
     */
    public void fxIncrementCounter(String counter) {
        if (fxData == null) {
            fxData = allocate(INTER_PULSE_DATA);
        }
        Map<String,Integer> counters = fxData.counters;
        if (counters.containsKey(counter)) {
            counters.put(counter, counters.get(counter) + 1);
        } else {
            counters.put(counter, 1);
        }
    }
    
    /**
     * Adds a message to the log for the pulse. This method <strong>MUST ONLY
     * BE CALLED FROM THE RENDER THREAD</strong>.
     * 
     * @param message The message to log. A newline will be added automatically.
     */
    public void renderMessage(String message) {
        if (renderData == null) {
            nullRenderFlag = true;
            return;
        }
        renderData.message
            .append("T")
            .append(Thread.currentThread().getId())
            .append(" : ")
            .append(message)
            .append("\n");
    }

    /**
     * Adds a timing message to the log for the pulse. This method <strong>MUST ONLY
     * BE CALLED FROM THE RENDER THREAD</strong>. The milliseconds will be computed and
     * added to the message. A newline will be added to the message.
     * <p>
     * This is typically used as follows:
     * 
     * long start = PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
     * operationToBeTimed();
     * if (PULSE_LOGGING_ENABLED) {
     *     PULSE_LOGGER.renderMessage(start, System.currentTimeMillis(), "Operation Identifier");
     * }
     * 
     * @param start The time in milliseconds when this operation started
     * @param end The time in milliseconds when the operation ended
     * @param message The message to log. A newline will be added automatically.
     */
    public void renderMessage(long start, long end, String message) {
        if (renderData == null) {
            nullRenderFlag = true;
            return;
        }
        renderData.message
            .append("T")
            .append(Thread.currentThread().getId())
            .append(" (").append(start-renderData.startTime).append(" +").append(end - start).append("ms): ")
            .append(message)
            .append("\n");
    }

    /**
     * Increments the given named per-pulse counter. This method <strong>MUST
     * ONLY BE CALLED ON THE RENDER THREAD</strong>.
     * @param counter The name for the counter.
     */
    public void renderIncrementCounter(String counter) {
        if (renderData == null) {
            nullRenderFlag = true;
            return;
        }
        Map<String,Integer> counters = renderData.counters;
        if (counters.containsKey(counter)) {
            counters.put(counter, counters.get(counter) + 1);
        } else {
            counters.put(counter, 1);
        }
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
        Map<String,Integer> counters = new ConcurrentHashMap<String, Integer>();

        void init(int n) {
            state = INCOMPLETE;
            pulseCount = n;
            startTime = System.currentTimeMillis();
            interval = 0;
            pushedRender = false;
        }
        
        void printAndReset() {
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            if (nullRenderFlag) {
                System.err.println("\nWARNING: unexpected render thread activity");
                nullRenderFlag = false;
            }
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
                    List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(counters.entrySet());
                    Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
                        public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) {
                            return a.getKey().compareTo(b.getKey());
                        }
                    });
                    for (Map.Entry<String, Integer> entry : entries) {
                        System.err.println("\t" + entry.getKey() + ": " + entry.getValue());
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
