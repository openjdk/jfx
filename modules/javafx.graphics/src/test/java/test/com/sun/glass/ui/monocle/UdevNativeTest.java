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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The netlink socket of Udev on Linux, without root and without a device event: the monitor opens on the
 * udev multicast group and closes; a second monitor in the same process is refused with {@code EADDRINUSE}
 * because both bind the process id (the behaviour of Udev.c of commit 21d5a654f6, kept); {@code recv} reports
 * through the C's exception text; and the two argument checks. Receiving a real event needs a device to
 * appear, so the blocking read and the close-while-blocked teardown of the monitor thread are exercised only by
 * the platform itself. The class binds libc in {@link #bindLibc}: a libc that does not bind fails every test
 * here, it never skips one. A kernel or sandbox that refuses an unprivileged AF_NETLINK bind (a seccomp
 * profile: EPERM) fails the monitor tests by design; such a failure is the environment, not the code.
 */
@EnabledOnOs(OS.LINUX)
public class UdevNativeTest {

    private static final int EBADF = 9;
    private static final int ENOTSOCK = 88;
    private static final int EADDRINUSE = 98;

    @BeforeAll
    public static void bindLibc() {
        LinuxSystemShim.loadLibrary();
    }

    @Test
    public void theMonitorSocketOpensWithoutPrivilegesAndCloses() throws IOException {
        long fd = UdevShim.openMonitor();
        assertTrue(fd > 0, "socket descriptor " + fd);
        UdevShim.closeMonitor(fd);
        // Assumes no concurrent open reused the number between the two closes; nothing in this JVM opens here.
        assertEquals(-1, LinuxSystemShim.close(fd), "closed: a second close is refused");
        assertEquals(EBADF, LinuxSystemShim.errno());
    }

    @Test
    public void aSecondMonitorInTheSameProcessIsRefusedWithEaddrinuse() throws IOException {
        long first = UdevShim.openMonitor();
        try {
            IOException refused = assertThrows(IOException.class, UdevShim::openMonitor);
            assertTrue(refused.getMessage().startsWith("Cannot bind netlink socket (errno=" + EADDRINUSE + ", "),
                    refused.getMessage());
        } finally {
            UdevShim.closeMonitor(first);
        }
    }

    @Test
    public void receiveOnADescriptorThatIsNotASocketReportsEnotsock() {
        long fd = LinuxSystemShim.open("/dev/null", LinuxSystemShim.O_RDWR);
        assertTrue(fd >= 0, () -> "open: " + LinuxSystemShim.getErrorMessage());
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(64);
            IOException failed = assertThrows(IOException.class, () -> UdevShim.receive(fd, buffer));
            assertTrue(failed.getMessage().startsWith("Error receiving event (errno=" + ENOTSOCK + ", "),
                    failed.getMessage());
        } finally {
            assertEquals(0, LinuxSystemShim.close(fd));
        }
    }

    @Test
    public void receiveRejectsAnInvalidDescriptorAndABufferThatIsNotDirect() {
        IOException descriptor = assertThrows(IOException.class,
                () -> UdevShim.receive(0, ByteBuffer.allocateDirect(64)));
        assertTrue(descriptor.getMessage().startsWith("Invalid socket descriptor (errno="), descriptor.getMessage());
        IOException heap = assertThrows(IOException.class, () -> UdevShim.receive(1, ByteBuffer.allocate(64)));
        assertTrue(heap.getMessage().startsWith("Invalid buffer (errno="), heap.getMessage());
    }
}
