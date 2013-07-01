/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
