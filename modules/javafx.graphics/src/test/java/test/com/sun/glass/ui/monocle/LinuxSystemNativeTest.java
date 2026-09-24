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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The libc binding of LinuxSystem on Linux, without hardware: the symbol census, the variadic {@code open} and
 * {@code ioctl} descriptors, the captured {@code errno}, the position/limit handling of {@code read} and
 * {@code write} over direct buffers, the mapping and dl* calls, {@code setenv} as seen through libc's own
 * {@code environ}, and the struct accessors over live memory. The class binds libc in {@link #bindLibc}: a libc
 * that does not bind fails every test here, it never skips one - this build has no library to be missing, so a
 * binding failure is a broken build.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxSystemNativeTest {

    private static final int EBADF = 9;
    private static final int ENOTTY = 25;
    private static final int PAGE = 4096;
    private static final int POINTER_BYTES = 8;
    private static final int MAX_ENVIRON_ENTRIES = 4096;
    private static final int MAX_ENVIRON_STRING = 65536;

    @BeforeAll
    public static void bindLibc() {
        LinuxSystemShim.loadLibrary();
        assertTrue(LinuxSystemShim.isLibraryLoaded());
    }

    private static long openOrFail(String path, int flags) {
        long fd = LinuxSystemShim.open(path, flags);
        assertTrue(fd >= 0, () -> "open(" + path + "): " + LinuxSystemShim.getErrorMessage());
        return fd;
    }

    @Test
    public void everyLibcSymbolIsBound() {
        List<String> expected = List.of("setenv", "open", "close", "lseek", "write", "read", "sysconf", "ioctl",
                "strerror", "dlopen", "dlerror", "dlsym", "dlclose", "mmap", "munmap", "mkfifo",
                "socket", "setsockopt", "bind", "recv");
        List<String> bound = LinuxSystemShim.boundSymbols();
        for (String name : expected) {
            String qualified = "libc!" + name;
            assertTrue(bound.contains(qualified), () -> qualified + " is not bound; bound: " + bound);
            assertNotEquals(0L, LinuxSystemShim.boundAddress(qualified), qualified);
        }
        assertEquals(expected.size(), bound.size(), () -> "unexpected bindings: " + bound);
        assertEquals(0L, LinuxSystemShim.boundAddress("libc!memcpy"), "memcpy is Java");
        assertEquals(0L, LinuxSystemShim.boundAddress("libc!getpid"), "the process id comes from ProcessHandle");
    }

    @Test
    public void openWriteLseekReadCloseRoundTrip(@TempDir Path dir) throws IOException {
        Path file = Files.createFile(dir.resolve("round-trip"));
        long fd = openOrFail(file.toString(), LinuxSystemShim.O_RDWR);
        try {
            ByteBuffer out = ByteBuffer.allocateDirect(16);
            out.put("0123456789abcdef".getBytes(StandardCharsets.US_ASCII));
            assertEquals(16, out.position(), "the position of the buffer itself is ignored: the arguments rule");
            assertEquals(4L, LinuxSystemShim.writeExact(fd, out, 2, 6));
            assertEquals(0L, LinuxSystemShim.lseek(fd, 0, LinuxSystemShim.SEEK_SET));
            ByteBuffer in = ByteBuffer.allocateDirect(16);
            in.position(9);
            assertEquals(4L, LinuxSystemShim.read(fd, in, 4, 12));
            byte[] got = new byte[4];
            in.get(4, got);
            assertArrayEquals("2345".getBytes(StandardCharsets.US_ASCII), got);
            assertEquals(0L, LinuxSystemShim.read(fd, in, 8, 16), "end of file");
            assertArrayEquals("2345".getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(file));
        } finally {
            assertEquals(0, LinuxSystemShim.close(fd));
        }
    }

    @Test
    public void closeOfAnInvalidDescriptorCapturesEbadf() {
        assertEquals(-1, LinuxSystemShim.close(-1));
        assertEquals(EBADF, LinuxSystemShim.errno());
        String message = LinuxSystemShim.getErrorMessage();
        assertNotNull(message);
        assertFalse(message.isEmpty());
        assertEquals(LinuxSystemShim.strerror(EBADF), message);
        assertEquals(EBADF, LinuxSystemShim.errno(), "strerror does not disturb the captured errno");
    }

    @Test
    public void ioctlOnDevNullIsEnotty() {
        long fd = openOrFail("/dev/null", LinuxSystemShim.O_RDWR);
        try {
            var screen = new LinuxSystemShim.FbVarScreenInfoShim();
            assertEquals(-1, LinuxSystemShim.ioctlExact(fd, LinuxSystemShim.FBIOGET_VSCREENINFO, screen.address()));
            assertEquals(ENOTTY, LinuxSystemShim.errno());
        } finally {
            assertEquals(0, LinuxSystemShim.close(fd));
        }
    }

    @Test
    public void fifoWithoutAReaderIsEnxio(@TempDir Path dir) {
        Path fifo = dir.resolve("uinput");
        assertEquals(0, LinuxSystemShim.mkfifo(fifo.toString(), LinuxSystemShim.S_IRWXU),
                () -> "mkfifo: " + LinuxSystemShim.getErrorMessage());
        assertTrue(Files.exists(fifo));
        long fd = LinuxSystemShim.open(fifo.toString(), LinuxSystemShim.O_WRONLY | LinuxSystemShim.O_NONBLOCK);
        assertEquals(-1L, fd);
        assertEquals(LinuxSystemShim.ENXIO, LinuxSystemShim.errno());
    }

    @Test
    public void anonymousMappingIsWritableThroughANewDirectByteBuffer() {
        long addr = LinuxSystemShim.mmap(0L, PAGE, LinuxSystemShim.PROT_READ | LinuxSystemShim.PROT_WRITE,
                LinuxSystemShim.MAP_PRIVATE | LinuxSystemShim.MAP_ANONYMOUS, -1, 0);
        assertNotEquals(-1L, addr, () -> "mmap: " + LinuxSystemShim.getErrorMessage());
        assertNotEquals(0L, addr);
        ByteBuffer mapping = LinuxSystemShim.newDirectByteBuffer(addr, PAGE);
        assertTrue(mapping.isDirect());
        assertEquals(PAGE, mapping.capacity());
        assertEquals(ByteOrder.BIG_ENDIAN, mapping.order(), "as ByteBuffer.allocateDirect and NewDirectByteBuffer");
        mapping.putInt(PAGE - 4, 0x5eedf00d);
        assertEquals(0x5eedf00d, mapping.getInt(PAGE - 4));
        mapping.position(100);
        assertEquals(addr, LinuxSystemShim.getDirectBufferAddress(mapping), "the base address, whatever the position");
        assertEquals(0, LinuxSystemShim.munmap(addr, PAGE));
    }

    @Test
    public void dlopenDlsymDlcloseAndDlerror() {
        long handle = LinuxSystemShim.dlopen("libc.so.6", LinuxSystemShim.RTLD_LAZY | LinuxSystemShim.RTLD_GLOBAL);
        assertNotEquals(0L, handle, () -> "dlopen: " + LinuxSystemShim.dlerror());
        assertNotEquals(0L, LinuxSystemShim.dlsym(handle, "strlen"));
        assertNull(LinuxSystemShim.dlerror(), "no pending error is a null String, as NewStringUTF(NULL) was");
        assertEquals(0, LinuxSystemShim.dlclose(handle));
        String missing = "libjfx-monocle-no-such-library.so";
        assertEquals(0L, LinuxSystemShim.dlopen(missing, LinuxSystemShim.RTLD_LAZY));
        String error = LinuxSystemShim.dlerror();
        assertNotNull(error);
        assertTrue(error.contains(missing), error);
    }

    @Test
    public void sysconfAnswersALongOf64Bits() {
        assertEquals(64L, LinuxSystemShim.sysconf(LinuxSystemShim._SC_LONG_BIT));
    }

    @Test
    public void structAccessorsRoundTripAndMemcpyCopiesAStruct() {
        var a = new LinuxSystemShim.FbVarScreenInfoShim();
        a.setRes(1024, 768);
        a.setBitsPerPixel(16);
        a.setRed(5, 11);
        assertEquals(1024, a.getXRes());
        assertEquals(768, a.getYRes());
        assertEquals(16, a.getBitsPerPixel());
        var b = new LinuxSystemShim.FbVarScreenInfoShim();
        assertEquals(b.address(), LinuxSystemShim.memcpy(b.address(), a.address(), b.sizeof()));
        assertEquals(1024, b.getXRes());
        assertEquals(768, b.getYRes());
        assertEquals(16, b.getBitsPerPixel());
        ByteBuffer raw = b.buffer().order(ByteOrder.nativeOrder());
        assertEquals(11, raw.getInt(32), "red.offset");
        assertEquals(5, raw.getInt(36), "red.length");
        var info = new LinuxSystemShim.InputAbsInfoShim();
        info.buffer().order(ByteOrder.nativeOrder()).putInt(20, 4096);
        assertEquals(4096, info.getResolution());
    }

    @Test
    public void heapBuffersAreRejectedAndPositionMustNotPassLimit() {
        long fd = openOrFail("/dev/null", LinuxSystemShim.O_RDWR);
        try {
            ByteBuffer heap = ByteBuffer.allocate(8);
            assertThrows(IllegalArgumentException.class, () -> LinuxSystemShim.writeExact(fd, heap, 0, 8));
            assertThrows(IllegalArgumentException.class, () -> LinuxSystemShim.read(fd, heap, 0, 8));
            ByteBuffer direct = ByteBuffer.allocateDirect(8);
            assertThrows(IndexOutOfBoundsException.class, () -> LinuxSystemShim.writeExact(fd, direct, 6, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> LinuxSystemShim.writeExact(fd, direct, 0, 9));
            assertThrows(IndexOutOfBoundsException.class, () -> LinuxSystemShim.writeExact(fd, direct, -1, 4));
            assertEquals(0L, LinuxSystemShim.writeExact(fd, direct, 4, 4), "an empty range writes nothing");
            assertEquals(8L, LinuxSystemShim.writeExact(fd, direct, 0, 8));
        } finally {
            assertEquals(0, LinuxSystemShim.close(fd));
        }
    }

    /**
     * {@code setenv} is checked through libc's own {@code char **environ}, found with {@code dlsym} and walked
     * through the address-to-buffer bridge of {@code C}: {@code System.getenv} is a start-up snapshot, and
     * {@code /proc/self/environ} is the initial environment block, which {@code setenv} never touches. The
     * walk is safe only while no other thread calls setenv or putenv (glibc may reallocate the array);
     * nothing in this JVM does, and a wrong pointer fails at the caps rather than reading until a fault.
     */
    @Test
    public void setenvPutsTheVariableIntoTheProcessEnvironment() {
        LinuxSystemShim.setenvExact("JFX_MONOCLE_LINUXSYSTEM_TEST", "bound", true);
        long libc = LinuxSystemShim.dlopen("libc.so.6", LinuxSystemShim.RTLD_LAZY);
        assertNotEquals(0L, libc, () -> "dlopen: " + LinuxSystemShim.dlerror());
        try {
            long environ = LinuxSystemShim.dlsym(libc, "environ");
            assertNotEquals(0L, environ, "libc exports environ");
            long entries = pointerAt(environ);
            assertNotEquals(0L, entries, "environ is set");
            List<String> environment = new ArrayList<>();
            for (int i = 0; ; i++) {
                assertTrue(i < MAX_ENVIRON_ENTRIES, "no terminator within " + MAX_ENVIRON_ENTRIES + " entries");
                long entry = pointerAt(entries + (long) i * POINTER_BYTES);
                if (entry == 0) {
                    break;
                }
                environment.add(cString(entry));
            }
            assertTrue(environment.contains("JFX_MONOCLE_LINUXSYSTEM_TEST=bound"), environment::toString);
        } finally {
            assertEquals(0, LinuxSystemShim.dlclose(libc));
        }
    }

    private static long pointerAt(long address) {
        return LinuxSystemShim.newDirectByteBuffer(address, POINTER_BYTES).order(ByteOrder.nativeOrder()).getLong(0);
    }

    /** The NUL-terminated string at {@code address}, read a byte at a time so that nothing past it is touched. */
    private static String cString(long address) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (long p = address; ; p++) {
            assertTrue(p - address < MAX_ENVIRON_STRING, "no NUL within " + MAX_ENVIRON_STRING + " bytes");
            byte b = LinuxSystemShim.newDirectByteBuffer(p, 1).get(0);
            if (b == 0) {
                break;
            }
            bytes.write(b);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
