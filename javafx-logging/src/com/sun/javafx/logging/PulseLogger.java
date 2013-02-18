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
    private static long THRESHOLD =
            AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override public Integer run() {
                    return Integer.getInteger("javafx.pulseLogger.threshold", 17);
                }
            });

    /**
     * We have a simple counter that keeps track of the current pulse number.
     */
    private int pulseCount = 0;

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
     * We also have an "empty" pulseData which is available for use on the next
     * pulseStart. Basically, this is a classic double-buffered technique
     * where we are writing to two buffers simultaneously and use the "empty"
     * to hold the finished buffer for use later.
     */
    private volatile PulseData fxData, renderData, empty;

    /**
     * Sometimes log methods are called when there is no pulse
     * going on. I'd like to gather that information as well
     * (particularly for things like layout). So I have a special
     * interPulseData which is written to in case there is no
     * other data to write to. It is then flushed at the start
     * of the next real pulse.
     */
    private volatile PulseData interPulseData = new PulseData();

    /**
     * Keeps track of the start of the previous pulse, such that we can print out
     * the time interval between the start of pulses.
     */
    private long lastPulseStartTime;

    /**
     * Disallow instantiation.
     */
    private PulseLogger() {
        empty = new PulseData();
        fxData = renderData = null;
    }

    /**
     * <strong>MUST</strong> be called at the start of every pulse.
     * This method will initialize the fxData buffer so that subsequent
     * calls to fxMessage will write to this buffer.
     */
    public void pulseStart() {
        // Initialize the lastPulseTime, so that the first pulse will effectively
        // have 0 as the delay
        if (lastPulseStartTime == 0) lastPulseStartTime = System.currentTimeMillis();
        // At the start of the pulse, we take the waiting
        // PulseData and then we use it. But we have to reset it.
        fxData = empty == null ? fxData = new PulseData() : empty;
        empty = null;
        fxData.reset();
        fxData.pulseCount = pulseCount++;

        // If the interPulseData has been populated, then we need
        // to flush it to the log and reset it
        if (!interPulseData.counters.isEmpty() || interPulseData.message.length() != 0) {
            logPulseData(interPulseData);
            interPulseData.reset();
            interPulseData.pulseCount = fxData.pulseCount;
        }
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
        renderData = fxData;
    }

    /**
     * <strong>Must</strong> be called at the end of the pulse. If
     * there was no rendering started during this pulse, then this
     * method will cause the pulse data to be logged. Otherwise, the
     * pulse data is logged when rendering is ended. However, as soon
     * as pulseEnd is called, we are ready for another call to pulseStart.
     */
    public void pulseEnd() {
        if (renderData == null) {
            // There is no rendering for this pulse, so we can terminate it
            // now and not bother going any further.
            logPulseData(fxData);
            // The renderData is null, so the only reusable data is the fxData.
            empty = fxData;
        }
        fxData = null;
    }

    /**
     * <strong>Must</strong> be called at the end of rendering, if a previous
     * call to {@link #renderStart()} had been made. This will cause the pulse
     * data to be logged.
     */
    public void renderEnd() {
        if (renderData != null) {
            logPulseData(renderData);
            empty = renderData;
            renderData = null;
        }
    }

    /**
     * Adds a message to the log for the pulse. This method <strong>MUST ONLY
     * BE CALLED FROM THE FX THREAD</strong>.
     * 
     * @param message The message to log. A newline will be added automatically.
     */
    public void fxMessage(String message) {
        StringBuffer buffer = fxData == null ? interPulseData.message : fxData.message;
        buffer
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
        StringBuffer buffer = fxData == null ? interPulseData.message : fxData.message;
        buffer
            .append("T")
            .append(Thread.currentThread().getId())
            .append(" (").append(end - start).append("ms): ")
            .append(message)
            .append("\n");
    }

    /**
     * Increments the given named per-pulse counter. This method <strong>MUST
     * ONLY BE CALLED ON THE FX THREAD</strong>.
     * @param counter The name for the counter.
     */
    public void fxIncrementCounter(String counter) {
        Map<String,Integer> counters = fxData == null ? interPulseData.counters : fxData.counters;
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
        StringBuffer buffer = renderData == null ? interPulseData.message : renderData.message;
        buffer
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
        StringBuffer buffer = renderData == null ? interPulseData.message : renderData.message;
        buffer
            .append("T")
            .append(Thread.currentThread().getId())
            .append(" (").append(end - start).append("ms): ")
            .append(message)
            .append("\n");
    }

    /**
     * Increments the given named per-pulse counter. This method <strong>MUST
     * ONLY BE CALLED ON THE RENDER THREAD</strong>.
     * @param counter The name for the counter.
     */
    public void renderIncrementCounter(String counter) {
        Map<String,Integer> counters = renderData == null ? interPulseData.counters : renderData.counters;
        if (counters.containsKey(counter)) {
            counters.put(counter, counters.get(counter) + 1);
        } else {
            counters.put(counter, 1);
        }
    }

    /**
     * Logs the given pulse data.
     * @param data
     */
    private void logPulseData(PulseData data) {
        // We've finished with some pulse data completely, so
        // we need to print it all out if the length of time the
        // pulse took exceeded some threshold.
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - data.startTime;
        long interval = data.startTime - lastPulseStartTime;
        if (totalTime > THRESHOLD) {
            if (data == fxData) {
                System.out.println("\n\nPULSE: " + data.pulseCount +
                        " [" + totalTime + "ms:" + interval + "ms] Required No Rendering");
                lastPulseStartTime = data.startTime;
            } else if (data == interPulseData) {
                System.out.println("\n\nINTER PULSE LOG DATA");
            } else {
                System.out.println("\n\nPULSE: " + data.pulseCount +
                        " [" + totalTime + "ms:" + interval + "ms]");
                lastPulseStartTime = data.startTime;
            }
            System.out.print(data.message);
            if (!data.counters.isEmpty()) {
                System.out.println("Counters:");
                List<Map.Entry<String,Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(data.counters.entrySet());
                Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
                    public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) {
                        return a.getKey().compareTo(b.getKey());
                    }
                });
                for (Map.Entry<String, Integer> entry : entries) {
                    String message = entry.getKey();
                    int count = entry.getValue();
                    System.out.println("\t" + message + ": " + count);
                }
            }
            wrapCount = 0;
        } else {
            System.out.print((wrapCount++ % 20 == 0 ? "\n[" : "[") + totalTime + "ms:" + interval + "ms]");
            lastPulseStartTime = data.startTime;
        }
    }

    /**
     * The data we collect per pulse. We store the pulse number
     * associated with this pulse, along with what time it
     * started at. We also maintain the message buffer.
     */
    private static final class PulseData {
        long startTime = 0;
        int pulseCount = 0;
        StringBuffer message = new StringBuffer();
        Map<String,Integer> counters = new ConcurrentHashMap<String, Integer>();

        void reset() {
            startTime = System.currentTimeMillis();
            pulseCount = 0;
            message = new StringBuffer();
            counters.clear();
        }
    }
}
