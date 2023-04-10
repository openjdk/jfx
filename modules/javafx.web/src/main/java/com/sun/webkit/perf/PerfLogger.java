/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.perf;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class PerfLogger {
    private static Thread shutdownHook;
    private static Map<PlatformLogger, PerfLogger> loggers;

    private final HashMap<String, ProbeStat> probes =
            new HashMap<>();
    private final PlatformLogger log;
    private final boolean isEnabled; // needed at shutdown time

    /**
     * Finds or creates a logger with the given {@code log}.
     * In order the logger be enabled the {@code log} should be loggable.
     *
     * @param log associated {@code Logger}
     */
    public synchronized static PerfLogger getLogger(PlatformLogger log) {
        if (loggers == null) {
            loggers = new HashMap<>();
        }
        PerfLogger l = loggers.get(log);
        if (l == null) {
            l = new PerfLogger(log);
            loggers.put(log, l);
        }
        // Add the hook if at least one logger is enabled.
        if (l.isEnabled() && shutdownHook == null) {
            shutdownHook = new Thread() {
                @Override
                public void run() {
                    for (PerfLogger l: loggers.values()) {
                        if (!l.isEnabled()) continue;
                        // Using stdout as logging might be off at shutdown time.
                        l.log(false);
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
        return l;
    }

    /**
     * Finds or creates a logger with {@code Logger} named
     * com.sun.webkit.perf.{@code name}.
     *
     * @param name the {@code PerfLogger} short name
     */
    public synchronized static PerfLogger getLogger(String name) {
        return getLogger(PlatformLogger.getLogger("com.sun.webkit.perf." + name));
    }

    private PerfLogger(PlatformLogger log) {
        this.log = log;
        this.isEnabled = log.isLoggable(Level.FINE);
        startCount("TOTALTIME");
    }

    /**
     * The class contains perf statistics for a registered probe.
     */
    public static final class ProbeStat {
        private final String probe;
        private int count;
        private long totalTime;
        private long startTime;
        private boolean isRunning = false;

        private ProbeStat(String probe) {
            this.probe = probe;
        }

        public String getProbe() {
            return probe;
        }

        public int getCount() {
            return count;
        }

        public long getTotalTime() {
            return totalTime;
        }

        private void reset() {
            count = 0;
            totalTime = startTime = 0;
        }

        private void suspend() {
            if (isRunning) {
                totalTime += System.currentTimeMillis() - startTime;
                isRunning = false;
            }
        }

        private void resume() {
            isRunning = true;
            count++;
            startTime = System.currentTimeMillis();
        }

        private void snapshot() {
            if (isRunning) {
                totalTime += System.currentTimeMillis() - startTime;
                startTime = System.currentTimeMillis();
            }
        }

        @Override
        public String toString() {
            return super.toString() + "[count=" + count + ", time=" + totalTime + "]";
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    private synchronized String fullName(String probe) {
        return log.getName() + "." + probe;
    }

    private final Comparator timeComparator = (arg0, arg1) -> {
        long t0 = probes.get(arg0).totalTime;
        long t1 = probes.get(arg1).totalTime;
        if (t0 > t1) {
            return 1;
        } else if (t0 < t1) {
            return -1;
        }
        return 0;
    };

    private final Comparator countComparator = (arg0, arg1) -> {
        long c0 = probes.get(arg0).count;
        long c1 = probes.get(arg1).count;
        if (c0 > c1) {
            return 1;
        } else if (c0 < c1) {
            return -1;
        }
        return 0;
    };

    /**
     * Resets perf statistics.
     */
    public synchronized void reset() {
        for (Map.Entry<String, ProbeStat> entry: probes.entrySet()) {
            entry.getValue().reset();
        }
        startCount("TOTALTIME");
    }

    public synchronized static void resetAll() {
        for (PerfLogger l: loggers.values()) {
            l.reset();
        }
    }

    private synchronized ProbeStat registerProbe(String probe) {
        String p = probe.intern();
        if (probes.containsKey(p)) {
            log.fine("Warning: \"" + fullName(p) + "\" probe already exists");
        } else {
            log.fine("Registering \"" + fullName(p) + "\" probe");
        }
        ProbeStat stat = new ProbeStat(p);
        probes.put(p, stat);
        return stat;
    }

    public synchronized ProbeStat getProbeStat(String probe) {
        String p = probe.intern();
        ProbeStat s = probes.get(p);
        if (s != null) {
            s.snapshot();
        }
        return s;
    }

    /**
     * Starts count statistics for the probe with zero initial data.
     */
    public synchronized void startCount(String probe) {
        if (!isEnabled()) {
            return;
        }
        String p = probe.intern();
        ProbeStat stat = probes.get(p);
        if (stat == null) {
            stat = registerProbe(p);
        }
        stat.reset();
        stat.resume();
    }

    /**
     * Suspends count statistics for the probe.
     */
    public synchronized void suspendCount(String probe) {
        if (!isEnabled()) {
            return;
        }
        String p = probe.intern();
        ProbeStat stat = probes.get(p);
        if (stat != null) {
            stat.suspend();
        } else {
            log.fine("Warning: \"" + fullName(p) + "\" probe is not registered");
        }
    }

    /**
     * Resumes count statistics for the probe, or starts if it's not yet started.
     */
    public synchronized void resumeCount(String probe) {
        if (!isEnabled()) {
            return;
        }
        String p = probe.intern();
        ProbeStat stat = probes.get(p);
        if (stat == null) {
            stat = registerProbe(p);
        }
        stat.resume();
    }

    /**
     * Prints perf statistics to the buffer.
     */
    public synchronized void log(StringBuffer buf) {
        if (!isEnabled()) {
            return;
        }
        buf.append("=========== Performance Statistics =============\n");

        ProbeStat total = getProbeStat("TOTALTIME");

        ArrayList<String> list = new ArrayList<>();
        list.addAll(probes.keySet());

        buf.append("\nTime:\n");
        Collections.sort(list, timeComparator);
        for (String p: list) {
            ProbeStat s = getProbeStat(p);
            buf.append(String.format("%s: %dms", fullName(p), s.totalTime));
            if (total.totalTime > 0){
                buf.append(String.format(", %.2f%%%n", (float)100*s.totalTime/total.totalTime));
            } else {
                buf.append("\n");
            }
        }

        buf.append("\nInvocations count:\n");
        Collections.sort(list, countComparator);
        for (String p: list) {
            buf.append(String.format("%s: %d%n", fullName(p), getProbeStat(p).count));
        }
        buf.append("================================================\n");
    }

    /**
     * Logs perf statistics.
     */
    public synchronized void log() {
        log(true);
    }

    private synchronized void log(boolean useLogger) {
        StringBuffer buf = new StringBuffer();
        log(buf);
        if (useLogger) {
            log.fine(buf.toString());
        } else {
            System.out.println(buf.toString());
            System.out.flush();
        }
    }

    /**
     * Logs perf statistics of all loggers.
     */
    public synchronized static void logAll() {
        for (PerfLogger l: loggers.values()) {
            l.log();
        }
    }
}
