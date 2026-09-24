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

import com.sun.glass.ui.monocle.EPDFrameBufferShim;
import com.sun.glass.ui.monocle.EPDSystemShim;
import com.sun.glass.ui.monocle.LinuxSystemShim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EPDFrameBuffer driven against a recording stand-in for the libc system calls, on every platform: the ioctl
 * sequence and the bytes it hands the EPDC driver for construction, {@code init()}, {@code clear()} and
 * {@code sync()}, the frame buffer mapping, and the failure path of the first {@code FBIOGET_VSCREENINFO}. The
 * EPD settings are the defaults of the {@code monocle.epd.*} properties: 32 bits per pixel, unrotated, waveform
 * mode AUTO, no flags. The e-paper hardware itself is unverified here.
 */
public class EPDFrameBufferFakeBackendTest {

    private static final String DEVICE = "/dev/fb0";
    private static final int XRES = 800;
    private static final int YRES = 600;
    private static final int XRES_VIRTUAL = 800;
    private static final int YRES_VIRTUAL = 640;
    private static final int BITS_PER_PIXEL = 32;
    private static final int FIRST_DESCRIPTOR = 3;

    private static final int XRES_INT = 0;
    private static final int BITS_PER_PIXEL_INT = 6;
    private static final int GRAYSCALE_INT = 7;
    private static final int RED_OFFSET_INT = 8;
    private static final int RED_LENGTH_INT = 9;
    private static final int TRANSP_OFFSET_INT = 17;
    private static final int TRANSP_LENGTH_INT = 18;
    private static final int ACTIVATE_INT = 21;
    private static final int ROTATE_INT = 34;

    private static final int UPDATE_WAVEFORM_MODE_INT = 4;
    private static final int UPDATE_MODE_INT = 5;
    private static final int UPDATE_MARKER_INT = 6;
    private static final int UPDATE_TEMP_INT = 7;
    private static final int UPDATE_FLAGS_INT = 8;

    private LinuxSystemShim.FakeBackend backend;

    @BeforeEach
    public void installTheFakeSystemCalls() {
        backend = new LinuxSystemShim.FakeBackend()
                .screenInfo(XRES, YRES, XRES_VIRTUAL, YRES_VIRTUAL, BITS_PER_PIXEL);
        LinuxSystemShim.installBackendForTesting(backend);
    }

    @AfterEach
    public void restoreLibc() {
        LinuxSystemShim.restoreLibcBackend();
    }

    private static List<Integer> constructionRequests() {
        return List.of(LinuxSystemShim.FBIOGET_VSCREENINFO, LinuxSystemShim.FBIOPUT_VSCREENINFO,
                LinuxSystemShim.FBIOGET_VSCREENINFO);
    }

    private static void assertUpdateRegion(int[] update) {
        assertEquals(0, update[0], "update_region.top");
        assertEquals(0, update[1], "update_region.left");
        assertEquals(XRES, update[2], "update_region.width");
        assertEquals(YRES, update[3], "update_region.height");
    }

    @Test
    public void constructionReadsSetsAndRereadsTheScreenInfo() throws IOException {
        var frameBuffer = new EPDFrameBufferShim(DEVICE);
        assertEquals(List.of(DEVICE), backend.openedPaths());
        assertEquals(FIRST_DESCRIPTOR, frameBuffer.getNativeHandle());
        assertEquals(constructionRequests(), backend.requests());
        int[] requested = backend.argumentInts(1);
        assertEquals(XRES, requested[XRES_INT], "xres as the device reported it");
        assertEquals(BITS_PER_PIXEL, requested[BITS_PER_PIXEL_INT]);
        assertEquals(0, requested[GRAYSCALE_INT]);
        assertEquals(16, requested[RED_OFFSET_INT], "rgba 8/16,8/8,8/0,8/24");
        assertEquals(8, requested[RED_LENGTH_INT]);
        assertEquals(24, requested[TRANSP_OFFSET_INT]);
        assertEquals(8, requested[TRANSP_LENGTH_INT]);
        assertEquals(EPDSystemShim.FB_ACTIVATE_FORCE, requested[ACTIVATE_INT]);
        assertEquals(0, requested[ROTATE_INT]);
        assertEquals(XRES_VIRTUAL, frameBuffer.getWidth());
        assertEquals(YRES, frameBuffer.getHeight());
        assertEquals(BITS_PER_PIXEL, frameBuffer.getBitDepth());
        assertEquals(0, frameBuffer.getByteOffset());
        assertEquals(XRES_VIRTUAL * YRES * Integer.BYTES, frameBuffer.getOffscreenBuffer().capacity());
        frameBuffer.close();
        assertEquals(List.of((long) FIRST_DESCRIPTOR), backend.closedDescriptors());
    }

    @Test
    public void initSendsTheFiveConfigurationRequestsWithTheirValues() throws IOException {
        var frameBuffer = new EPDFrameBufferShim(DEVICE);
        frameBuffer.init();
        List<Integer> requests = backend.requests();
        assertEquals(8, requests.size());
        assertEquals(List.of(EPDSystemShim.mxcfbSetWaveformModes(), EPDSystemShim.mxcfbSetTemperature(),
                EPDSystemShim.mxcfbSetAutoUpdateMode(), EPDSystemShim.mxcfbSetPwrdownDelay(),
                EPDSystemShim.mxcfbSetUpdateScheme()), requests.subList(3, 8));
        assertArrayEquals(new int[] {EPDSystemShim.WAVEFORM_MODE_INIT, EPDSystemShim.WAVEFORM_MODE_DU,
            EPDSystemShim.WAVEFORM_MODE_GC4, EPDSystemShim.WAVEFORM_MODE_GC16, EPDSystemShim.WAVEFORM_MODE_GC16,
            EPDSystemShim.WAVEFORM_MODE_GC16}, backend.argumentInts(3), "mxcfb_waveform_modes");
        assertArrayEquals(new int[] {EPDSystemShim.TEMP_USE_AMBIENT}, backend.argumentInts(4), "temperature");
        assertArrayEquals(new int[] {EPDSystemShim.AUTO_UPDATE_MODE_REGION_MODE}, backend.argumentInts(5));
        assertArrayEquals(new int[] {1000}, backend.argumentInts(6), "power-down delay in milliseconds");
        assertArrayEquals(new int[] {EPDSystemShim.UPDATE_SCHEME_SNAPSHOT}, backend.argumentInts(7));
    }

    @Test
    public void clearSendsTwoFullDirectUpdatesAndWaitsForTheSecond() throws IOException {
        var frameBuffer = new EPDFrameBufferShim(DEVICE);
        frameBuffer.clear();
        List<Integer> requests = backend.requests();
        assertEquals(List.of(EPDSystemShim.mxcfbSendUpdate(), EPDSystemShim.mxcfbSendUpdate(),
                EPDSystemShim.mxcfbWaitForUpdateComplete()), requests.subList(3, 6));
        int[] black = backend.argumentInts(3);
        assertEquals(17, black.length, "mxcfb_update_data as 17 ints");
        assertUpdateRegion(black);
        assertEquals(EPDSystemShim.WAVEFORM_MODE_DU, black[UPDATE_WAVEFORM_MODE_INT]);
        assertEquals(EPDSystemShim.UPDATE_MODE_FULL, black[UPDATE_MODE_INT]);
        assertEquals(1, black[UPDATE_MARKER_INT], "the first marker is 1: zero is refused by the driver");
        assertEquals(EPDSystemShim.TEMP_USE_AMBIENT, black[UPDATE_TEMP_INT]);
        assertEquals(0, black[UPDATE_FLAGS_INT]);
        int[] white = backend.argumentInts(4);
        assertUpdateRegion(white);
        assertEquals(EPDSystemShim.WAVEFORM_MODE_DU, white[UPDATE_WAVEFORM_MODE_INT]);
        assertEquals(EPDSystemShim.UPDATE_MODE_FULL, white[UPDATE_MODE_INT]);
        assertEquals(2, white[UPDATE_MARKER_INT]);
        assertEquals(EPDSystemShim.EPDC_FLAG_ENABLE_INVERSION, white[UPDATE_FLAGS_INT]);
        assertArrayEquals(new int[] {2}, backend.argumentInts(5), "waits for the second update");
    }

    @Test
    public void syncWaitsForThePreviousUpdateThenSendsThePartialAutoUpdate() throws IOException {
        var frameBuffer = new EPDFrameBufferShim(DEVICE);
        frameBuffer.sync();
        frameBuffer.sync();
        List<Integer> requests = backend.requests();
        assertEquals(List.of(EPDSystemShim.mxcfbWaitForUpdateComplete(), EPDSystemShim.mxcfbSendUpdate(),
                EPDSystemShim.mxcfbWaitForUpdateComplete(), EPDSystemShim.mxcfbSendUpdate()), requests.subList(3, 7));
        assertArrayEquals(new int[] {0}, backend.argumentInts(3), "nothing sent yet: waits for marker 0");
        int[] first = backend.argumentInts(4);
        assertUpdateRegion(first);
        assertEquals(EPDSystemShim.WAVEFORM_MODE_AUTO, first[UPDATE_WAVEFORM_MODE_INT]);
        assertEquals(EPDSystemShim.UPDATE_MODE_PARTIAL, first[UPDATE_MODE_INT]);
        assertEquals(1, first[UPDATE_MARKER_INT]);
        assertEquals(EPDSystemShim.TEMP_USE_AMBIENT, first[UPDATE_TEMP_INT]);
        assertEquals(0, first[UPDATE_FLAGS_INT]);
        assertArrayEquals(new int[] {1}, backend.argumentInts(5), "waits for the update it sent");
        assertEquals(2, backend.argumentInts(6)[UPDATE_MARKER_INT]);
    }

    @Test
    public void theMappingCoversTheVirtualWidthByTheHeightAndIsUnmappedAtItsBase() throws IOException {
        var frameBuffer = new EPDFrameBufferShim(DEVICE);
        ByteBuffer mapping = frameBuffer.getMappedBuffer();
        long size = (long) XRES_VIRTUAL * YRES * (BITS_PER_PIXEL / Byte.SIZE);
        assertEquals(1, backend.mappings().size());
        long[] mapped = backend.mappings().get(0);
        assertEquals(size, mapped[1], "length");
        assertEquals(LinuxSystemShim.PROT_WRITE, mapped[2], "prot");
        assertEquals(LinuxSystemShim.MAP_SHARED, mapped[3], "flags");
        assertEquals(FIRST_DESCRIPTOR, mapped[4], "fd");
        assertEquals(0, mapped[5], "offset");
        assertTrue(mapping.isDirect());
        assertEquals(size, mapping.capacity());
        assertEquals(mapped[0], LinuxSystemShim.getDirectBufferAddress(mapping));
        mapping.position(1234);
        frameBuffer.releaseMappedBuffer(mapping);
        assertEquals(1, backend.unmappings().size());
        assertArrayEquals(new long[] {mapped[0], size}, backend.unmappings().get(0),
                "the base address, not the position");
    }

    @Test
    public void aFailingGetScreenInfoClosesTheDescriptorAndThrows() {
        backend.result(LinuxSystemShim.FBIOGET_VSCREENINFO, -1).errno(25);
        IOException failed = assertThrows(IOException.class, () -> new EPDFrameBufferShim(DEVICE));
        assertEquals(backend.strerror(25), failed.getMessage());
        assertEquals(List.of((long) FIRST_DESCRIPTOR), backend.closedDescriptors());
        assertEquals(List.of(LinuxSystemShim.FBIOGET_VSCREENINFO), backend.requests());
    }

    @Test
    public void aFailingOpenThrowsBeforeAnyRequest() {
        backend.openFails().errno(2);
        IOException failed = assertThrows(IOException.class, () -> new EPDFrameBufferShim(DEVICE));
        assertEquals(backend.strerror(2), failed.getMessage());
        assertEquals(List.of(), backend.requests());
    }
}
