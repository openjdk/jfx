package com.sun.glass.ui.monocle.linux;/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** A buffer holding raw Linux input events waiting to be processed */
public class LinuxEventBuffer {
    static final int EVENT_STRUCT_SIZE = 16;
    private static final int EVENT_STRUCT_TYPE_INDEX = 8;
    private static final int EVENT_STRUCT_CODE_INDEX = 10;
    private static final int EVENT_STRUCT_VALUE_INDEX = 12;

    /**
     * EVENT_BUFFER_SIZE controls the maximum number of event lines that can be
     * processed per device in one pulse. This value must be greater than the
     * number of lines in the largest event including the terminating EV_SYN
     * SYN_REPORT. However it sould not be too large or a flood of events will
     * prevent rendering from happening until the buffer is full.
     */
    private static final int EVENT_BUFFER_SIZE = 100;

    private final ByteBuffer bb;

    private int positionOfLastSync;
    private int currentPosition;
    private int mark;

    LinuxEventBuffer() {
        bb = ByteBuffer.allocate(EVENT_STRUCT_SIZE * EVENT_BUFFER_SIZE);
        bb.order(ByteOrder.nativeOrder());

    }

    /** Adds a raw Linux event to the buffer. Blocks if the buffer is full.
     * Checks whether this is a SYN SYN_REPORt event terminator.
     *
     *
     * @param event A ByteBuffer containing the event to be added.
     * @return true if the event was "SYN SYN_REPORT", false otherwise
     * @throws InterruptedException if our thread was interrupted while waiting
     * for the buffer to empty.
     */
    public synchronized boolean put(ByteBuffer event) throws InterruptedException {
        boolean isSync = event.getInt(EVENT_STRUCT_TYPE_INDEX) == 0
                && event.getInt(EVENT_STRUCT_CODE_INDEX) == 0;
        while (bb.limit() - bb.position() < event.limit()) {
            // Block if bb is full. This should be the
            // only time this thread waits for anything
            // except for more event lines.
            wait();
        }
        if (isSync) {
            positionOfLastSync = bb.position();
        }
        bb.put(event);
        return isSync;
    }

    public synchronized void startIteration() {
        currentPosition = 0;
        mark = 0;
    }

    public synchronized void compact() {
        positionOfLastSync -= currentPosition;
        int newLimit = bb.position();
        bb.position(currentPosition);
        bb.limit(newLimit);
        bb.compact();
        // If put() is waiting for space in the buffer, wake it up
        notifyAll();
    }

    /**
     * Returns the type of the current event line. Call from the application
     * thread.
     *
     * @return the type of the current event line
     */
    public synchronized short getEventType() {
        return bb.getShort(currentPosition + EVENT_STRUCT_TYPE_INDEX);
    }

    /**
     * Returns the code of the current event line.  Call from the application
     * thread.
     *
     * @return the code of the event line
     */
    public short getEventCode() {
        return bb.getShort(currentPosition + EVENT_STRUCT_CODE_INDEX);
    }

    /**
     * Returns the value of the current event line.  Call from the application
     * thread.
     *
     * @return the value of the current event line
     */
    public synchronized int getEventValue() {
        return bb.getInt(currentPosition + EVENT_STRUCT_VALUE_INDEX);
    }

    /**
     * Returns a string describing the current event. Call from the application
     * thread.
     *
     * @return a string describing the event
     */
    public synchronized String getEventDescription() {
        short type = getEventType();
        short code = getEventCode();
        int value = getEventValue();
        String typeStr = Input.typeToString(type);
        return typeStr + " " + Input.codeToString(typeStr, code) + " " + value;
    }

    /**
     * Advances to the next event line.  Call from the application thread.
     */
    public synchronized void nextEvent() {
        if (currentPosition > positionOfLastSync) {
            throw new IllegalStateException("Cannot advance past the last" +
                                                    " EV_SYN EV_SYN_REPORT 0");
        }
        currentPosition += EVENT_STRUCT_SIZE;
    }

    /**
     * Sets a mark on the buffer. A future call to reset() will return to this
     * point.
     */
    public synchronized void mark() {
        mark = currentPosition;
    }

    /** Returns iteration to the event set previously in a call to mark(), or
     * to the beginning of the buffer if no call to mark() was made.
     */
    public synchronized void reset() {
        currentPosition = mark;
    }

    /**
     * Returns true iff another event line is available AND it is part of a
     * complete event. Call from the application thread.
     */
    public synchronized boolean hasNextEvent() {
        return currentPosition <= positionOfLastSync;
    }

}
