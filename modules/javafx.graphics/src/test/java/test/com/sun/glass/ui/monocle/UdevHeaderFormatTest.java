/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.LinuxSystemShim;
import com.sun.glass.ui.monocle.UdevShim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.StructLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The udev event header parsing of Udev against synthetic datagrams: format A (libudev's
 * {@code udev_monitor_netlink_header}: {@code "libudev\0"}, the magic 0xfeedcafe in network order at 8, then
 * host-order header size at 12, properties offset at 16 and length at 20), format B (a 16-byte prefix, the magic
 * 0xcafe1dea at 16, host-order 16-bit properties offset at 20 and length at 22), the -1 answers and the one-time
 * stderr dump for anything else, and the process-wide one-shot decision the C made with a static. The
 * {@code sockaddr_nl} layout and the socket constants of {@code linux/netlink.h}, {@code bits/socket.h} and
 * {@code asm-generic/socket.h} are checked here too. Pure Java: runs on every platform and binds nothing.
 */
public class UdevHeaderFormatTest {

    private static final int EVENT_BYTES = 64;
    private static final int MAGIC_A = 0xfeedcafe;
    private static final int MAGIC_B = 0xcafe1dea;

    @BeforeEach
    @AfterEach
    public void forgetTheFormatDecision() {
        UdevShim.resetEventFormat();
    }

    private static ByteBuffer event() {
        return ByteBuffer.allocateDirect(EVENT_BYTES).order(ByteOrder.nativeOrder());
    }

    private static ByteBuffer formatA(int propertiesOffset, int propertiesLength) {
        ByteBuffer event = event();
        event.put(0, "libudev\0".getBytes(StandardCharsets.US_ASCII));
        event.duplicate().order(ByteOrder.BIG_ENDIAN).putInt(8, MAGIC_A);
        event.putInt(12, 40);
        event.putInt(16, propertiesOffset);
        event.putInt(20, propertiesLength);
        return event;
    }

    private static ByteBuffer formatB(int propertiesOffset, int propertiesLength) {
        ByteBuffer event = event();
        event.put(0, "libudev-legacy-\0".getBytes(StandardCharsets.US_ASCII));
        event.duplicate().order(ByteOrder.BIG_ENDIAN).putInt(16, MAGIC_B);
        event.putShort(20, (short) propertiesOffset);
        event.putShort(22, (short) propertiesLength);
        return event;
    }

    @Test
    public void formatAReadsHostOrderInts() {
        ByteBuffer event = formatA(40, 300);
        assertEquals(40, UdevShim.propertiesOffset(event));
        assertEquals(300, UdevShim.propertiesLength(event));
    }

    @Test
    public void formatBReadsUnsignedShorts() {
        ByteBuffer event = formatB(24, 0xfff0);
        assertEquals(24, UdevShim.propertiesOffset(event));
        assertEquals(65520, UdevShim.propertiesLength(event), "an unsigned short, zero-extended as the (jint) cast");
    }

    @Test
    public void thePositionOfTheEventBufferIsIgnored() {
        ByteBuffer event = formatA(40, 10);
        event.position(30);
        assertEquals(40, UdevShim.propertiesOffset(event));
        assertEquals(10, UdevShim.propertiesLength(event));
        assertEquals(30, event.position());
    }

    @Test
    public void anUnknownFormatAnswersMinusOneAndIsDumpedOnce() {
        ByteBuffer event = event();
        event.putInt(0, 0x11223344);
        event.putInt(44, 0x55667788);
        PrintStream stderr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            assertEquals(-1, UdevShim.propertiesOffset(event));
            assertEquals(-1, UdevShim.propertiesLength(event));
            assertEquals(-1, UdevShim.propertiesOffset(formatA(40, 10)), "decided for the process: still invalid");
        } finally {
            System.setErr(stderr);
        }
        String dump = captured.toString(StandardCharsets.UTF_8);
        assertEquals(1, dump.split("Cannot identify udev event format:", -1).length - 1, dump);
        assertTrue(dump.contains("00 11223344 00000000 00000000 00000000"), dump);
        assertTrue(dump.contains("10 00000000 00000000 00000000 00000000"), dump);
        assertTrue(dump.contains("20 00000000 00000000 00000000 55667788"), dump);
    }

    @Test
    public void theFirstEventDecidesTheFormatForTheProcess() {
        assertEquals(40, UdevShim.propertiesOffset(formatA(40, 10)));
        ByteBuffer legacy = formatB(24, 5);
        assertEquals(legacy.getInt(16), UdevShim.propertiesOffset(legacy), "read as format A: the magic bytes");
        assertEquals(legacy.getInt(20), UdevShim.propertiesLength(legacy), "read as format A: both shorts");
        UdevShim.resetEventFormat();
        assertEquals(24, UdevShim.propertiesOffset(legacy));
        assertEquals(5, UdevShim.propertiesLength(legacy));
    }

    @Test
    public void sockaddrNlMatchesLinuxNetlinkH() {
        StructLayout layout = LinuxSystemShim.sockaddrNlLayout();
        assertEquals(12, layout.byteSize());
        assertEquals(0, layout.byteOffset(PathElement.groupElement("nl_family")));
        assertEquals(2, layout.byteOffset(PathElement.groupElement("nl_pad")));
        assertEquals(4, layout.byteOffset(PathElement.groupElement("nl_pid")));
        assertEquals(8, layout.byteOffset(PathElement.groupElement("nl_groups")));
        var address = new LinuxSystemShim.SockaddrNlShim();
        assertEquals(12, address.sizeof());
        address.setFamily(LinuxSystemShim.AF_NETLINK);
        address.setPid(0x12345678);
        address.setGroups(UdevShim.UDEV_MONITOR_GROUP);
        ByteBuffer raw = address.buffer().order(ByteOrder.nativeOrder());
        assertEquals(16, raw.getShort(0));
        assertEquals(0, raw.getShort(2), "nl_pad stays zero");
        assertEquals(0x12345678, raw.getInt(4));
        assertEquals(2, raw.getInt(8));
        assertEquals(16, address.getFamily());
        assertEquals(0x12345678, address.getPid());
        assertEquals(2, address.getGroups());
    }

    @Test
    public void socketConstantsMatchTheHeaders() {
        assertEquals(16, LinuxSystemShim.PF_NETLINK, "bits/socket.h");
        assertEquals(16, LinuxSystemShim.AF_NETLINK, "bits/socket.h");
        assertEquals(2, LinuxSystemShim.SOCK_DGRAM, "bits/socket_type.h");
        assertEquals(15, LinuxSystemShim.NETLINK_KOBJECT_UEVENT, "linux/netlink.h");
        assertEquals(1, LinuxSystemShim.SOL_SOCKET, "asm-generic/socket.h");
        assertEquals(8, LinuxSystemShim.SO_RCVBUF, "asm-generic/socket.h");
        assertEquals(2, UdevShim.UDEV_MONITOR_GROUP, "the udev multicast group");
    }
}
