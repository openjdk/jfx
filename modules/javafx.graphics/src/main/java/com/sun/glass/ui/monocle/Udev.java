/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Udev connects to the udev system to get updates on sysfs devices that
 * are connected, disconnected and modified.
 * <p>
 * The connection is the kernel's {@code NETLINK_KOBJECT_UEVENT} socket, bound to the udev multicast group and
 * read through {@link LinuxSystem}, as Udev.c of commit 21d5a654f6 did through JNI: libudev is not involved,
 * then or now. The event header is parsed here, in Java, by the rules of that C.
 */
class Udev implements Runnable {
    private static Udev instance;

    private long fd;
    private ByteBuffer buffer;
    private Thread thread;
    private UdevListener[] listeners;

    /** The multicast group of {@code NETLINK_KOBJECT_UEVENT} that carries the events udev has processed. */
    static final int UDEV_MONITOR_GROUP = 2;

    /** The {@code SO_RCVBUF} the C asked for. */
    private static final int RECEIVE_BUFFER_SIZE = 16384;

    // The event header comes in two layouts, told apart once per process by their magic. A is libudev's
    // udev_monitor_netlink_header: char prefix[8]; unsigned magic, header_size, properties_off, properties_len.
    // B is the older one: char prefix[16]; unsigned magic; unsigned short properties_off, properties_len. The
    // magic is stored in network byte order, the other fields in host order.
    private static final int MAGIC_A = 0xfeedcafe;
    private static final int MAGIC_B = 0xcafe1dea;
    private static final int HEADER_A_MAGIC = 8;
    private static final int HEADER_A_PROPERTIES_OFFSET = 16;
    private static final int HEADER_A_PROPERTIES_LENGTH = 20;
    private static final int HEADER_B_MAGIC = 16;
    private static final int HEADER_B_PROPERTIES_OFFSET = 20;
    private static final int HEADER_B_PROPERTIES_LENGTH = 22;

    /** How much of an event of unknown format the C dumped to stderr: three rows of four words. */
    private static final int DUMP_WORDS_PER_ROW = 4;
    private static final int DUMP_ROWS = 3;

    private enum EventFormat {
        UNKNOWN, A, B, INVALID
    }

    /** Decided by the first event and kept for the rest of the process, as the C's static was. */
    private static EventFormat eventFormat = EventFormat.UNKNOWN;

    /** Gets the singleton Udev object */
    static synchronized Udev getInstance() {
        if (instance == null) {
            try {
                instance = new Udev();
            } catch (IOException e) {
                System.err.println("Udev: failed to open connection");
                e.printStackTrace();
            }
        }
        return instance;
    }

    /**
     * Creates a new Udev object
     */
    private Udev() throws IOException {
        // Open a connection to the udev monitor
        fd = openMonitor();
        buffer = ByteBuffer.allocateDirect(4096);
        buffer.order(ByteOrder.nativeOrder());
        thread = new Thread(this, "udev monitor");
        thread.setDaemon(true);
        thread.start();
    }

    synchronized void addListener(UdevListener listener) {
        if (listeners == null) {
            listeners = new UdevListener[] { listener };
        } else {
            listeners = Arrays.copyOf(listeners, listeners.length + 1);
            listeners[listeners.length - 1] = listener;
        }
    }

    /** An IOException in the form of the C: {@code "<message> (errno=<errno>, <strerror>)"}. */
    private static IOException ioException(LinuxSystem system, String message) {
        return ioException(system, message, system.errno());
    }

    /** The same, for an {@code errno} read before another call could change the captured value. */
    private static IOException ioException(LinuxSystem system, String message, int errno) {
        return new IOException(message + " (errno=" + errno + ", " + system.strerror(errno) + ")");
    }

    /**
     * Opens the udev monitor: a {@code NETLINK_KOBJECT_UEVENT} datagram socket with a 16 KiB receive buffer,
     * bound to this process id and the udev multicast group. The process id is the one the C got from
     * {@code getpid()}; {@code ProcessHandle} answers the same number without a further binding.
     *
     * @return the socket descriptor
     * @throws IOException if the socket cannot be created or bound
     */
    static long openMonitor() throws IOException {
        LinuxSystem system = LinuxSystem.getLinuxSystem();
        var address = new LinuxSystem.SockaddrNl();
        address.setFamily(address.p, LinuxSystem.AF_NETLINK);
        address.setPid(address.p, (int) ProcessHandle.current().pid());
        address.setGroups(address.p, UDEV_MONITOR_GROUP);
        int fd = system.socket(LinuxSystem.PF_NETLINK, LinuxSystem.SOCK_DGRAM, LinuxSystem.NETLINK_KOBJECT_UEVENT);
        if (fd == -1) {
            throw ioException(system, "Cannot create netlink socket");
        }
        system.setsockopt(fd, LinuxSystem.SOL_SOCKET, LinuxSystem.SO_RCVBUF, RECEIVE_BUFFER_SIZE);
        int bound = system.bind(fd, address.p, address.sizeof());
        // The struct is a Cleaner-backed direct buffer that nothing references past its address: keep it
        // reachable across the downcall, as the stack struct of the C was.
        Reference.reachabilityFence(address);
        if (bound != 0) {
            int errno = system.errno();
            closeMonitor(fd);
            throw ioException(system, "Cannot bind netlink socket", errno);
        }
        return fd;
    }

    /**
     * Receives one event into the whole of the buffer, whatever its position, as the C did.
     *
     * @return the length of the event in bytes
     * @throws IOException for a descriptor that is not positive, a buffer that is not direct, or a receive
     * that returned no data
     */
    static int receive(long fd, ByteBuffer buffer) throws IOException {
        LinuxSystem system = LinuxSystem.getLinuxSystem();
        if (fd <= 0) {
            throw ioException(system, "Invalid socket descriptor");
        }
        if (buffer == null || !buffer.isDirect()) {
            throw ioException(system, "Invalid buffer");
        }
        int length = (int) system.recv(fd, buffer, 0, buffer.capacity(), 0);
        if (length <= 0) {
            throw ioException(system, "Error receiving event");
        }
        return length;
    }

    /** Closes the monitor socket; a descriptor that is not positive is ignored, as the C ignored it. */
    static void closeMonitor(long fd) {
        if (fd > 0) {
            LinuxSystem.getLinuxSystem().close(fd);
        }
    }

    /**
     * The format of the events this process receives, decided by the first event seen. An event of neither
     * format is dumped to stderr once and every event after it is invalid too.
     */
    private static EventFormat decidedFormat(ByteBuffer event) {
        if (eventFormat == EventFormat.UNKNOWN) {
            ByteBuffer network = event.duplicate().order(ByteOrder.BIG_ENDIAN);
            if (network.getInt(HEADER_A_MAGIC) == MAGIC_A) {
                eventFormat = EventFormat.A;
            } else if (network.getInt(HEADER_B_MAGIC) == MAGIC_B) {
                eventFormat = EventFormat.B;
            } else {
                eventFormat = EventFormat.INVALID;
                dump(event);
            }
        }
        return eventFormat;
    }

    /** The C's dump of an unidentified event: the first 48 bytes as host-order words, 16 bytes a row. */
    private static void dump(ByteBuffer event) {
        ByteBuffer host = event.duplicate().order(ByteOrder.nativeOrder());
        System.err.println("Cannot identify udev event format:");
        for (int row = 0; row < DUMP_ROWS; row++) {
            int start = row * DUMP_WORDS_PER_ROW * Integer.BYTES;
            StringBuilder line = new StringBuilder(String.format("%02x", start));
            for (int word = 0; word < DUMP_WORDS_PER_ROW; word++) {
                line.append(String.format(" %08x", host.getInt(start + word * Integer.BYTES)));
            }
            System.err.println(line);
        }
    }

    /** Forgets the format decision, so that a test can present events of another format. */
    static void resetEventFormatForTesting() {
        eventFormat = EventFormat.UNKNOWN;
    }

    /**
     * The offset of the property list in an event, or -1 for an event whose format could not be identified.
     */
    static int propertiesOffset(ByteBuffer event) {
        ByteBuffer host = event.duplicate().order(ByteOrder.nativeOrder());
        return switch (decidedFormat(event)) {
            case A -> host.getInt(HEADER_A_PROPERTIES_OFFSET);
            case B -> Short.toUnsignedInt(host.getShort(HEADER_B_PROPERTIES_OFFSET));
            default -> -1;
        };
    }

    /**
     * The length of the property list in an event, or -1 for an event whose format could not be identified.
     */
    static int propertiesLength(ByteBuffer event) {
        ByteBuffer host = event.duplicate().order(ByteOrder.nativeOrder());
        return switch (decidedFormat(event)) {
            case A -> host.getInt(HEADER_A_PROPERTIES_LENGTH);
            case B -> Short.toUnsignedInt(host.getShort(HEADER_B_PROPERTIES_LENGTH));
            default -> -1;
        };
    }

    @Override
    public void run() {
        try {
            RunnableProcessor runnableProcessor =
                    NativePlatformFactory.getNativePlatform().getRunnableProcessor();
            while (true) {
                Map<String, String> event = readEvent();
                runnableProcessor.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String action = event.get("ACTION");
                        if (action != null) {
                            UdevListener[] uls;
                            synchronized (this) {
                                uls = listeners;
                            }
                            if (uls != null) {
                                for (int i = 0; i < uls.length; i++) {
                                    try {
                                        uls[i].udevEvent(action, event);
                                    } catch (RuntimeException e) {
                                        System.err.println(
                                                "Exception in udev listener:");
                                        e.printStackTrace();
                                    } catch (Error e) {
                                        System.err.println(
                                                "Error in udev listener, " +
                                                        "closing udev");
                                        e.printStackTrace();
                                        close();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                });
            }
        } catch (IOException e) {
            if (!thread.isInterrupted()) {
                System.err.println("Exception in udev thread:");
                e.printStackTrace();
                close();
            }
        }
    }

    /** Reads data from the udev monitor. Blocks until data is available */
    private Map<String, String> readEvent() throws IOException {
        Map<String, String> map = new HashMap<>();
        ByteBuffer b;
        synchronized (this) {
            b = buffer;
            if (b == null) {
                return map;
            }
        }
        int length = receive(fd, b);
        synchronized (this) {
            if (buffer == null) {
                return map;
            }
            int propertiesOffset = propertiesOffset(buffer);
            int propertiesLength = propertiesLength(buffer);
            int propertiesEnd = propertiesOffset + propertiesLength;
            if (length < propertiesEnd) {
                throw new IOException("Mismatched property segment length");
            }
            buffer.position(propertiesOffset);
            // Data read from the udev monitor is in the form of a list of
            // lines separated by null bytes.
            // Each line defines a key/value pair, with the
            // format: <key>=<value><null terminator>
            StringBuffer key = new StringBuffer();
            StringBuffer value = new StringBuffer();
            nextKey: while (buffer.position() < propertiesEnd) {
                key.setLength(0);
                value.setLength(0);
                boolean readKey = false;
                while (buffer.position() < length && !readKey) {
                    char ch = (char) buffer.get();
                    switch (ch) {
                        case '\000': // no value on this line
                            map.put(key.toString(), "");
                            continue nextKey;
                        case '=':
                            readKey = true;
                            break;
                        default:
                            key.append(ch);
                    }
                }
                while (buffer.position() < propertiesEnd) {
                    char ch = (char) buffer.get();
                    switch (ch) {
                        case '\000':
                            map.put(key.toString(), value.toString());
                            continue nextKey;
                        default:
                            value.append(ch);
                    }
                }
            }
            buffer.clear();
        }
        return map;
    }

    /** Closes the udev monitor connection */
    synchronized void close() {
        thread.interrupt();
        closeMonitor(fd);
        fd = 0l;
        buffer = null;
        thread = null;
    }

}
