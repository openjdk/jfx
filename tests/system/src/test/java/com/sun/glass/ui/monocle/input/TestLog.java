/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import junit.framework.AssertionFailedError;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public class TestLog {

    private static final long DEFAULT_TIMEOUT = 3000l;

    private static final List<String> log = new ArrayList<>();
    private static final Object lock = new Object();

    private static long startTime = System.currentTimeMillis();

    public static void log(String s) {
        synchronized (lock) {
            if (TestApplication.isVerbose()) {
                System.out.println(timestamp() + " TestLog: " + s);
            }
            log.add(s);
            lock.notifyAll();
        }
    }

    public static void format(String format, Object... args) {
        log(new Formatter().format(format, args).toString());
    }

    public static  List<String> getLog() {
        return new ArrayList<>(log);
    }

    public static void clear() {
        synchronized (lock) {
            log.clear();
        }
    }

    public static void reset() {
        synchronized (lock) {
            log.clear();
            startTime = System.currentTimeMillis();
        }
    }

    public static String timestamp() {
        long time = System.currentTimeMillis() - startTime;
        StringBuffer sb = new StringBuffer().append(time);
        while (sb.length() < 4) {
            sb.insert(0, "0");
        }
        while (sb.length() < 8) {
            sb.insert(0, " ");
        }
        sb.insert(sb.length() - 3, ".");
        return sb.toString();
    }

    public static Object getLock() {
        return lock;
    }

    public static int countLog(String s, int startIndex, boolean exact) {
        int count = 0;
        for (int i = startIndex; i < log.size(); i++) {
            String line  = log.get(i);
            if (exact) {
                if (line.equals(s)) {
                    count ++;
                }
            } else {
                if (line.indexOf(s) >= 0) {
                    count ++;
                }
            }
        }
        return count;
    }

    public static int countLog(String s) {
        return countLog(s, 0, true);
    }

    public static int countLogContaining(String s) {
        return countLog(s, 0, false);
    }

    private static String checkLog(String s, int startIndex, boolean exact) {
        for (int i = startIndex; i < log.size(); i++) {
            String line  = log.get(i);
            if (exact) {
                if (line.equals(s)) {
                    return line;
                }
            } else {
                if (line.indexOf(s) >= 0) {
                    return line;
                }
            }
        }
        return null;
    }

    public static boolean checkLog(String s) {
        return checkLog(s, 0, true) != null;
    }

    public static boolean checkLogContaining(String s) {
        return checkLog(s, 0, false) != null;
    }

    public static void assertLog(String s) {
        synchronized (lock) {
            if (!checkLog(s)) {
                String err = "No line '" + s + "' in log";
                if (TestApplication.isVerbose()) {
                    System.out.println(err);
                }
                throw new AssertionFailedError(err);
            }
        }
    }

    public static void assertLogContaining(String s) {
        synchronized (lock) {
            if (!checkLogContaining(s)) {
                String err = "No line containing '" + s + "' in log";
                if (TestApplication.isVerbose()) {
                    System.out.println(err);
                }
                throw new AssertionFailedError(err);
            }
        }
    }

    private static String waitForLog(String s, long timeout, boolean exact) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeNow = startTime;
        long endTime = timeNow + (long) (timeout * TestApplication.getTimeScale());
        String line;
        synchronized (lock) {
            int index = 0;
            while ((line = checkLog(s, index, exact)) == null) {
                index = log.size();
                if (endTime - timeNow > 0) {
                    lock.wait(endTime - timeNow);
                }
                timeNow = System.currentTimeMillis();
                if (timeNow >= endTime) {
                    String message = "Timed out after " + (timeNow - startTime)
                            + "ms waiting for '" + s + "'";
                    if (!TestApplication.isVerbose()) {
                        System.out.flush();
                        System.err.flush();
                        for (String logLine: log) {
                            System.out.println(logLine);
                        }
                    }
                    System.out.println(message);
                    throw new AssertionFailedError(message);
                }
            }
        }
        long matchTime = System.currentTimeMillis() - startTime;
        if (TestApplication.isVerbose()) {
            if (exact) {
                System.out.println("TestLog matched '"
                        + s + "' in "
                        + matchTime + "ms");

            } else {
                System.out.println("TestLog matched '"
                        + s + "' with '"
                        + line + "' in "
                        + matchTime + "ms");
            }
        }
        return line;
    }

    public static String waitForLog(String s, long timeout) throws InterruptedException {
        return waitForLog(s, timeout, true);
    }

    public static String waitForLogContaining(String s, long timeout) throws InterruptedException {
        return waitForLog(s, timeout, false);
    }

    public static String waitForLog(String format, Object... args) throws InterruptedException {
        return waitForLog(new Formatter().format(format, args).toString(),
                          DEFAULT_TIMEOUT);

    }

    public static String waitForLogContaining(String format, Object... args) throws InterruptedException {
        return waitForLogContaining(new Formatter().format(format, args).toString(),
                          DEFAULT_TIMEOUT);

    }

}
