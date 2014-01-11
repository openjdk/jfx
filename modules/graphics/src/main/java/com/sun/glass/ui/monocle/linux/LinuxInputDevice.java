/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.monocle.input.InputDevice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * A LinuxInputDevice listens for events on a Linux
 * input device node, typically one of the files in /dev/input. When events are
 * waiting to be processed on the device it notifies its listener on a thread
 * provided by its executor object. The executor should be a single-threaded
 * ExecutorService that runs all tasks on the JavaFX application thread.
 * <p>
 * Event lines are accumulated in a buffer until an event "EV_SYN EV_SYN_REPORT
 * 0" is received. At this point the listener is notified. The listener can then
 * use the methods getEventType(), getEventCode() and getEventValue() to obtain
 * the details of the current event line to process. nextEvent() and
 * hasNextEvent() are used to iterate over pending events.
 * <p>
 * To save on RAM and GC, event lines are not objects.
 */
public class LinuxInputDevice implements Runnable, InputDevice {
    private static final int EVENT_STRUCT_SIZE = 16;
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

    private LinuxInputProcessor inputProcessor;
    private ReadableByteChannel in;
    private File devNode;
    private File sysPath;
    private Map<String, BitSet> capabilities;
    private Map<Integer, AbsoluteInputCapabilities> absCaps;
    private Map<String, String> udevManifest;
    private ByteBuffer bb = ByteBuffer.allocate(EVENT_STRUCT_SIZE * EVENT_BUFFER_SIZE);
    private ByteBuffer event = ByteBuffer.allocate(EVENT_STRUCT_SIZE);
    private ExecutorService executor;
    private EventProcessor processor = new EventProcessor();
    private int positionOfLastSync;
    private int currentPosition;

    /**
     * Create a new com.sun.glass.ui.monocle.input.LinuxInputDevice on the given
     * input node.
     *
     * @param devNode The node on which to listen for input
     * @param sysPath The sysfs path describing the device
     * @throws IOException
     */
    public LinuxInputDevice(
            File devNode,
            File sysPath,
            Map<String, String> udevManifest) throws IOException {
        this.devNode = devNode;
        this.sysPath = sysPath;
        this.udevManifest = udevManifest;
        this.capabilities = SysFS.readCapabilities(sysPath);
        this.absCaps = AbsoluteInputCapabilities.getCapabilities(
                devNode, capabilities.get("abs"));
        this.in = new FileInputStream(devNode).getChannel();
        this.executor = NativePlatformFactory.getNativePlatform().getExecutor();
    }

    public void setInputProcessor(LinuxInputProcessor inputProcessor) {
        this.inputProcessor = inputProcessor;
    }

    @Override
    public void run() {
        if (inputProcessor == null) {
            System.err.println("Error: no input processor set on " + devNode);
            return;
        }
        bb.order(ByteOrder.nativeOrder());
        while (true) {
            try {
                in.read(event);
                if (event.position() == event.limit()) {
                    event.flip();
                    boolean isSync = event.getInt(EVENT_STRUCT_TYPE_INDEX) == 0
                            && event.getInt(EVENT_STRUCT_CODE_INDEX) == 0;
                    synchronized (bb) {
                        while (bb.limit() - bb.position() < event.limit()) {
                            // Block if bb is full. This should be the
                            // only time this thread waits for anything
                            // except for more event lines.
                            bb.wait();
                        }
                        if (isSync) {
                            positionOfLastSync = bb.position();
                        }
                        bb.put(event);
                        if (isSync && !processor.scheduled) {
                            executor.submit(processor);
                            processor.scheduled = true;
                        }
                    }
                    event.rewind();
                }
            } catch (IOException | InterruptedException e) {
                // the device is disconnected
                return;
            }
        }
    }

    /**
     * The EventProcessor is used to notify listeners of pending events. It runs
     * on the executor thread.
     */
    class EventProcessor implements Runnable {
        boolean scheduled;

        public void run() {
            synchronized (bb) {
                processor.scheduled = false;
                currentPosition = 0;
            }
            // Do not lock bb while process events. We still want to be
            // able to add incoming events to it.
            inputProcessor.processEvents(LinuxInputDevice.this);
            synchronized (bb) {
                positionOfLastSync -= currentPosition;
                int newLimit = bb.position();
                bb.position(currentPosition);
                bb.limit(newLimit);
                bb.compact();
                bb.notifyAll();
            }
        }
    }

    /**
     * Returns the type of the current event line. Call from the application
     * thread.
     *
     * @return the type of the current event line
     */
    public short getEventType() {
        synchronized (bb) {
            return bb.getShort(currentPosition + EVENT_STRUCT_TYPE_INDEX);
        }
    }

    /**
     * Returns the code of the current event line.  Call from the application
     * thread.
     *
     * @return the code of the event line
     */
    public short getEventCode() {
        synchronized (bb) {
            return bb.getShort(currentPosition + EVENT_STRUCT_CODE_INDEX);
        }
    }

    /**
     * Returns the value of the current event line.  Call from the application
     * thread.
     *
     * @return the value of the current event line
     */
    public int getEventValue() {
        synchronized (bb) {
            return bb.getInt(currentPosition + EVENT_STRUCT_VALUE_INDEX);
        }
    }

    /**
     * Returns a string describing the current event. Call from the application
     * thread.
     *
     * @return a string describing the event
     */
    public String getEventDescription() {
        short type = getEventType();
        short code = getEventCode();
        int value = getEventValue();
        String typeStr = Input.typeToString(type);
        return typeStr + " " + Input.codeToString(typeStr, code) + " " + value;
    }

    /**
     * Advances to the next event line.  Call from the application thread.
     */
    public void nextEvent() {
        synchronized (bb) {
            if (currentPosition > positionOfLastSync) {
                throw new IllegalStateException("Cannot advance past the last" +
                        " EV_SYN EV_SYN_REPORT 0");
            }
            currentPosition += EVENT_STRUCT_SIZE;
        }
    }

    /**
     * Returns true iff another event line is available AND it is part of a
     * complete event. Call from the application thread.
     */
    public boolean hasNextEvent() {
        synchronized (bb) {
            return currentPosition <= positionOfLastSync;
        }
    }

    /**
     * @return a string describing this input device
     */
    public String toString() {
        return devNode.toString();
    }

    BitSet getCapability(String type) {
        return capabilities.get(type);
    }

    AbsoluteInputCapabilities getAbsoluteInputCapabilities(int axis) {
        return absCaps == null ? null : absCaps.get(axis);
    }

    @Override
    public boolean isTouch() {
        return "1".equals(udevManifest.get("ID_INPUT_TOUCHSCREEN"))
                || "1".equals(udevManifest.get("ID_INPUT_TABLET"));
    }

    @Override
    public boolean isMultiTouch() {
        if (isTouch()) {
            BitSet abs = capabilities.get("abs");
            if (abs == null) {
                return false;
            }
            return abs.get(Input.ABS_MT_SLOT)
                    || (abs.get(Input.ABS_MT_POSITION_X)
                        && abs.get(Input.ABS_MT_POSITION_Y));
        } else {
            return false;
        }
    }

    @Override
    public boolean isRelative() {
        return "1".equals(udevManifest.get("ID_INPUT_MOUSE"));
    }

    @Override
    public boolean is5Way() {
        BitSet key = capabilities.get("key");
        if (key == null) {
            return false;
        }
        // Make sure we have all arrow keys
        for (int i = 0; i < KeyBits.KEYBITS_ARROWS.length; i++) {
            if (!key.get(KeyBits.KEYBITS_ARROWS[i])) {
                return false;
            }
        }
        // and at least one select key
        for (int i = 0; i < KeyBits.KEYBITS_SELECT.length; i++) {
            if (key.get(KeyBits.KEYBITS_SELECT[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFullKeyboard() {
        BitSet key = capabilities.get("key");
        if (key == null) {
            return false;
        }
        // Make sure we have all alphanumeric keys
        for (int i = 0; i < KeyBits.KEYBITS_PC.length; i++) {
            if (!key.get(KeyBits.KEYBITS_PC[i])) {
                return false;
            }
        }
        // ...and the 5-way keys
        return is5Way();
    }

}
