/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.network;

/**
 * An extended time consisting of a long "base time" and
 * an integer "subtime".
 */
final class ExtendedTime implements Comparable<ExtendedTime> {

    private final long baseTime;
    private final int subtime;


    /**
     * Creates a new {@code ExtendedTime}.
     */
    ExtendedTime(long baseTime, int subtime) {
        this.baseTime = baseTime;
        this.subtime = subtime;
    }


    /**
     * Returns the current extended time with the base time initialized
     * to System.currentTimeMillis() and the subtime initialized to zero.
     */
    static ExtendedTime currentTime() {
        return new ExtendedTime(System.currentTimeMillis(), 0);
    }


    /**
     * Returns the base time.
     */
    long baseTime() {
        return baseTime;
    }

    /**
     * Returns the subtime.
     */
    int subtime() {
        return subtime;
    }

    /**
     * Increments the subtime and returns the result as a new extended time.
     */
    ExtendedTime incrementSubtime() {
        return new ExtendedTime(baseTime, subtime + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ExtendedTime otherExtendedTime) {
        int d = (int) (baseTime - otherExtendedTime.baseTime);
        if (d != 0) {
            return d;
        }
        return subtime - otherExtendedTime.subtime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[baseTime=" + baseTime + ", subtime=" + subtime + "]";
    }
}
