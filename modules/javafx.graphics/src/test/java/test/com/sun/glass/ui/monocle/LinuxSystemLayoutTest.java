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
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.StructLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The struct layouts of LinuxSystem against {@code sizeof} and {@code offsetof} of {@code struct input_absinfo}
 * ({@code linux/input.h}) and {@code struct fb_var_screeninfo} / {@code struct fb_bitfield} ({@code linux/fb.h}):
 * every field is a 32-bit kernel integer, so the values hold on every architecture and this test runs on every
 * platform. The accessors are exercised over the direct buffer a Structure allocates, which needs no libc
 * either; the LP64 check is driven with both answers.
 */
public class LinuxSystemLayoutTest {

    private static final int FB_VAR_SCREENINFO_BYTES = 160;

    private static ByteBuffer nativeOrder(ByteBuffer buffer) {
        return buffer.order(ByteOrder.nativeOrder());
    }

    @Test
    public void inputAbsinfoIsSixInts() {
        StructLayout layout = LinuxSystemShim.inputAbsInfoLayout();
        assertEquals(24, layout.byteSize());
        assertEquals(0, layout.byteOffset(PathElement.groupElement("value")));
        assertEquals(4, layout.byteOffset(PathElement.groupElement("minimum")));
        assertEquals(8, layout.byteOffset(PathElement.groupElement("maximum")));
        assertEquals(12, layout.byteOffset(PathElement.groupElement("fuzz")));
        assertEquals(16, layout.byteOffset(PathElement.groupElement("flat")));
        assertEquals(20, layout.byteOffset(PathElement.groupElement("resolution")));
    }

    @Test
    public void fbBitfieldIsThreeInts() {
        StructLayout layout = LinuxSystemShim.fbBitfieldLayout();
        assertEquals(12, layout.byteSize());
        assertEquals(0, layout.byteOffset(PathElement.groupElement("offset")));
        assertEquals(4, layout.byteOffset(PathElement.groupElement("length")));
        assertEquals(8, layout.byteOffset(PathElement.groupElement("msb_right")));
    }

    @Test
    public void fbVarScreeninfoIs160BytesWithEveryFieldNamed() {
        StructLayout layout = LinuxSystemShim.fbVarScreenInfoLayout();
        assertEquals(FB_VAR_SCREENINFO_BYTES, layout.byteSize());
        Map<String, Long> offsets = new LinkedHashMap<>();
        offsets.put("xres", 0L);
        offsets.put("yres", 4L);
        offsets.put("xres_virtual", 8L);
        offsets.put("yres_virtual", 12L);
        offsets.put("xoffset", 16L);
        offsets.put("yoffset", 20L);
        offsets.put("bits_per_pixel", 24L);
        offsets.put("grayscale", 28L);
        offsets.put("red", 32L);
        offsets.put("green", 44L);
        offsets.put("blue", 56L);
        offsets.put("transp", 68L);
        offsets.put("nonstd", 80L);
        offsets.put("activate", 84L);
        offsets.put("height", 88L);
        offsets.put("width", 92L);
        offsets.put("accel_flags", 96L);
        offsets.put("pixclock", 100L);
        offsets.put("left_margin", 104L);
        offsets.put("right_margin", 108L);
        offsets.put("upper_margin", 112L);
        offsets.put("lower_margin", 116L);
        offsets.put("hsync_len", 120L);
        offsets.put("vsync_len", 124L);
        offsets.put("sync", 128L);
        offsets.put("vmode", 132L);
        offsets.put("rotate", 136L);
        offsets.put("colorspace", 140L);
        offsets.put("reserved", 144L);
        for (Map.Entry<String, Long> field : offsets.entrySet()) {
            assertEquals(field.getValue(), layout.byteOffset(PathElement.groupElement(field.getKey())),
                    "offsetof(struct fb_var_screeninfo, " + field.getKey() + ")");
        }
        assertEquals(offsets.size(), layout.memberLayouts().size(), "fields of struct fb_var_screeninfo");
        assertEquals(16, layout.select(PathElement.groupElement("reserved")).byteSize(), "__u32 reserved[4]");
    }

    @Test
    public void bitfieldsAreNestedAtTheirOffsets() {
        StructLayout layout = LinuxSystemShim.fbVarScreenInfoLayout();
        String[] bitfields = {"red", "green", "blue", "transp"};
        long[] starts = {32L, 44L, 56L, 68L};
        for (int i = 0; i < bitfields.length; i++) {
            PathElement bitfield = PathElement.groupElement(bitfields[i]);
            assertEquals(starts[i], layout.byteOffset(bitfield, PathElement.groupElement("offset")));
            assertEquals(starts[i] + 4, layout.byteOffset(bitfield, PathElement.groupElement("length")));
            assertEquals(starts[i] + 8, layout.byteOffset(bitfield, PathElement.groupElement("msb_right")));
        }
    }

    @Test
    public void structuresAreAllocatedAtLayoutSize() {
        var screen = new LinuxSystemShim.FbVarScreenInfoShim();
        assertEquals(FB_VAR_SCREENINFO_BYTES, screen.sizeof());
        assertEquals(FB_VAR_SCREENINFO_BYTES, screen.buffer().capacity());
        assertTrue(screen.buffer().isDirect());
        assertEquals(screen.address(), LinuxSystemShim.getDirectBufferAddress(screen.buffer()));
        var info = new LinuxSystemShim.InputAbsInfoShim();
        assertEquals(24, info.sizeof());
        assertEquals(24, info.buffer().capacity());
        assertEquals(info.address(), LinuxSystemShim.getDirectBufferAddress(info.buffer()));
    }

    @Test
    public void fbVarScreenInfoSettersWriteTheFieldsTheCWrote() {
        var screen = new LinuxSystemShim.FbVarScreenInfoShim();
        screen.setRes(640, 480);
        screen.setVirtualRes(1280, 960);
        screen.setOffset(3, 7);
        screen.setBitsPerPixel(32);
        screen.setActivate(16);
        screen.setRed(5, 11);
        screen.setGreen(6, 5);
        screen.setBlue(5, 0);
        screen.setTransp(1, 15);
        ByteBuffer raw = nativeOrder(screen.buffer());
        assertEquals(640, raw.getInt(0), "xres");
        assertEquals(480, raw.getInt(4), "yres");
        assertEquals(1280, raw.getInt(8), "xres_virtual");
        assertEquals(960, raw.getInt(12), "yres_virtual");
        assertEquals(3, raw.getInt(16), "xoffset");
        assertEquals(7, raw.getInt(20), "yoffset");
        assertEquals(32, raw.getInt(24), "bits_per_pixel");
        assertEquals(11, raw.getInt(32), "red.offset: the second argument of setRed");
        assertEquals(5, raw.getInt(36), "red.length: the first argument of setRed");
        assertEquals(0, raw.getInt(40), "red.msb_right is never written");
        assertEquals(5, raw.getInt(44), "green.offset");
        assertEquals(6, raw.getInt(48), "green.length");
        assertEquals(0, raw.getInt(56), "blue.offset");
        assertEquals(5, raw.getInt(60), "blue.length");
        assertEquals(15, raw.getInt(68), "transp.offset");
        assertEquals(1, raw.getInt(72), "transp.length");
        assertEquals(16, raw.getInt(84), "activate");
        assertEquals(640, screen.getXRes());
        assertEquals(480, screen.getYRes());
        assertEquals(1280, screen.getXResVirtual());
        assertEquals(960, screen.getYResVirtual());
        assertEquals(3, screen.getOffsetX());
        assertEquals(7, screen.getOffsetY());
        assertEquals(32, screen.getBitsPerPixel());
    }

    @Test
    public void inputAbsInfoGettersReadTheFields() {
        var info = new LinuxSystemShim.InputAbsInfoShim();
        ByteBuffer raw = nativeOrder(info.buffer());
        raw.putInt(0, 1);
        raw.putInt(4, -2);
        raw.putInt(8, 3);
        raw.putInt(12, 4);
        raw.putInt(16, 5);
        raw.putInt(20, 4096);
        assertEquals(1, info.getValue());
        assertEquals(-2, info.getMinimum());
        assertEquals(3, info.getMaximum());
        assertEquals(4, info.getFuzz());
        assertEquals(5, info.getFlat());
        assertEquals(4096, info.getResolution());
    }

    @Test
    public void aCLongOf8BytesIsRequired() {
        UnsatisfiedLinkError refused = assertThrows(UnsatisfiedLinkError.class,
                () -> LinuxSystemShim.requireLp64(4));
        assertTrue(refused.getMessage().contains("C long is 4 bytes"), refused.getMessage());
        assertTrue(refused.getMessage().contains("LP64"), refused.getMessage());
        assertDoesNotThrow(() -> LinuxSystemShim.requireLp64(8));
    }
}
