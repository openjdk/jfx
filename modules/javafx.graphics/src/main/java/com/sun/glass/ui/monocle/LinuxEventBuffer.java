/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A buffer holding raw Linux input events waiting to be processed
 */
class LinuxEventBuffer {

    interface EventStruct {
        int getTypeIndex();
        int getCodeIndex();
        int getValueIndex();
        int getSize();
    }

    class EventStruct32Bit implements EventStruct {
        public int getTypeIndex() { return 8; }
        public int getCodeIndex() { return 10; }
        public int getValueIndex() { return 12; }
        public int getSize() { return 16; }
    }

    class EventStruct64Bit implements EventStruct {
        public int getTypeIndex() { return 16; }
        public int getCodeIndex() { return 18; }
        public int getValueIndex() { return 20; }
        public int getSize() { return 24; }
    }

    /**
     * EVENT_BUFFER_SIZE controls the maximum number of event lines that can be
     * processed per device in one pulse. This value must be greater than the
     * number of lines in the largest event including the terminating EV_SYN
     * SYN_REPORT. However it should not be too large or a flood of events will
     * prevent rendering from happening until the buffer is full.
     */
    private static final int EVENT_BUFFER_SIZE = 1000;

    private final ByteBuffer bb;
    private final EventStruct eventStruct;
    private int positionOfLastSync;
    private int currentPosition;
    private int mark;

    LinuxEventBuffer(int osArchBits) {
        eventStruct = osArchBits == 64 ? new EventStruct64Bit() : new EventStruct32Bit();
        bb = ByteBuffer.allocate(eventStruct.getSize() * EVENT_BUFFER_SIZE);
        bb.order(ByteOrder.nativeOrder());
    }

    int getEventSize() {
        return eventStruct.getSize();
    }

    /**
     * Adds a raw Linux event to the buffer. Blocks if the buffer is full.
     * Checks whether this is a SYN SYN_REPORT event terminator.
     *
     * @param event A ByteBuffer containing the event to be added.
     * @return true if the event was "SYN SYN_REPORT", false otherwise
     * @throws InterruptedException if our thread was interrupted while waiting
     *                              for the buffer to empty.
     */
    synchronized boolean put(ByteBuffer event) throws
            InterruptedException {
        boolean isSync = event.getShort(eventStruct.getTypeIndex()) == 0
                && event.getInt(eventStruct.getValueIndex()) == 0;
        while (bb.limit() - bb.position() < event.limit()) {
            // Block if bb is full. This should be the
            // only time this thread waits for anything
            // except for more event lines.
            if (MonocleSettings.settings.traceEventsVerbose) {
                MonocleTrace.traceEvent(
                        "Event buffer %s is full, waiting for some space to become available",
                        bb);
                // wait for half the space to be available, to avoid excessive context switching?
            }
            wait();
        }
        if (isSync) {
            positionOfLastSync = bb.position();
        }
        bb.put(event);
        if (MonocleSettings.settings.traceEventsVerbose) {
            int index = bb.position() - eventStruct.getSize();
            MonocleTrace.traceEvent("Read %s [index=%d]",
                                    getEventDescription(index), index);
        }
        return isSync;
    }

    synchronized void startIteration() {
        currentPosition = 0;
        mark = 0;
        if (MonocleSettings.settings.traceEventsVerbose) {
            MonocleTrace.traceEvent("Processing %s [index=%d]", getEventDescription(), currentPosition);
        }
    }

    synchronized void compact() {
        positionOfLastSync -= currentPosition;
        int newLimit = bb.position();
        bb.position(currentPosition);
        bb.limit(newLimit);
        bb.compact();
        if (MonocleSettings.settings.traceEventsVerbose) {
            MonocleTrace.traceEvent("Compacted event buffer %s", bb);
        }
        // If put() is waiting for space in the buffer, wake it up
        notifyAll();
    }

    /**
     * Returns the type of the current event line. Call from the application
     * thread.
     *
     * @return the type of the current event line
     */
    synchronized short getEventType() {
        return bb.getShort(currentPosition + eventStruct.getTypeIndex());
    }

    /**
     * Returns the code of the current event line.  Call from the application
     * thread.
     *
     * @return the code of the event line
     */
    short getEventCode() {
        return bb.getShort(currentPosition + eventStruct.getCodeIndex());
    }

    /**
     * Returns the value of the current event line.  Call from the application
     * thread.
     *
     * @return the value of the current event line
     */
    synchronized int getEventValue() {
        return bb.getInt(currentPosition + eventStruct.getValueIndex());
    }

    /**
     * Returns a string describing the current event. Call from the application
     * thread.
     *
     * @return a string describing the event
     */
    synchronized String getEventDescription() {
        return getEventDescription(currentPosition);
    }

    private synchronized String getEventDescription(int position) {
        short type = bb.getShort(position + eventStruct.getTypeIndex());
        short code = bb.getShort(position + eventStruct.getCodeIndex());
        int value = bb.getInt(position + eventStruct.getValueIndex());
        String typeStr = LinuxInput.typeToString(type);
        return typeStr + " " + LinuxInput.codeToString(typeStr, code) + " " + value;
    }

    /**
     * Advances to the next event line.  Call from the application thread.
     */
    synchronized void nextEvent() {
        if (currentPosition > positionOfLastSync) {
            throw new IllegalStateException("Cannot advance past the last" +
                                                    " EV_SYN EV_SYN_REPORT 0");
        }
        currentPosition += eventStruct.getSize();
        if (MonocleSettings.settings.traceEventsVerbose && hasNextEvent()) {
            MonocleTrace.traceEvent("Processing %s [index=%d]",
                                    getEventDescription(), currentPosition);
        }
    }

    /**
     * Sets a mark on the buffer. A future call to reset() will return to this
     * point.
     */
    synchronized void mark() {
        mark = currentPosition;
    }

    /**
     * Returns iteration to the event set previously in a call to mark(), or to
     * the beginning of the buffer if no call to mark() was made.
     */
    synchronized void reset() {
        currentPosition = mark;
    }

    /**
     * Returns true iff another event line is available AND it is part of a
     * complete event. Call from the application thread.
     */
    synchronized boolean hasNextEvent() {
        return currentPosition <= positionOfLastSync;
    }

    /**
     * Returns true iff another event line is available. Call on the
     * application thread.
     */
    synchronized boolean hasData() {
        return bb.position() != 0;
    }

}
