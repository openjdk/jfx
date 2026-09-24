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

import com.sun.glass.ui.monocle.EPDSystemShim;
import com.sun.glass.ui.monocle.LinuxSystemShim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The 43 accessors EPDSystem.FbVarScreenInfo adds over the 160-byte {@code struct fb_var_screeninfo} of
 * {@code linux/fb.h}, plus the seven it inherits: every getter reads its field at the offset the header gives it
 * and every setter writes exactly that field, checked against raw host-order ints in the struct's own memory.
 * The MXCFB request codes of {@code mxcfb.h} are checked against their {@code _IOW}/{@code _IOR} values, and the
 * {@code int}-by-address form of {@code EPDSystem.ioctl} against a recording backend. Runs on every platform: the
 * accessors need no libc, and the backend stands in for it. The e-paper hardware path itself is unverified here.
 */
public class EPDSystemLayoutTest {

    private static final int FB_VAR_SCREENINFO_BYTES = 160;

    @AfterEach
    public void restoreLibc() {
        LinuxSystemShim.restoreLibcBackend();
    }

    private static ByteBuffer raw(EPDSystemShim.FbVarScreenInfoShim screen) {
        return screen.buffer().order(ByteOrder.nativeOrder());
    }

    /** Every field of the struct is written with 1000 + its offset, so a getter reading the wrong field shows. */
    private static EPDSystemShim.FbVarScreenInfoShim numberedStruct() {
        var screen = new EPDSystemShim.FbVarScreenInfoShim();
        ByteBuffer raw = raw(screen);
        for (int offset = 0; offset < FB_VAR_SCREENINFO_BYTES; offset += Integer.BYTES) {
            raw.putInt(offset, 1000 + offset);
        }
        return screen;
    }

    @Test
    public void everyGetterReadsItsField() {
        var screen = numberedStruct();
        assertEquals(FB_VAR_SCREENINFO_BYTES, screen.sizeof());
        assertEquals(1000, screen.getXRes());
        assertEquals(1004, screen.getYRes());
        assertEquals(1008, screen.getXResVirtual());
        assertEquals(1012, screen.getYResVirtual());
        assertEquals(1016, screen.getOffsetX());
        assertEquals(1020, screen.getOffsetY());
        assertEquals(1024, screen.getBitsPerPixel());
        assertEquals(1028, screen.getGrayscale());
        assertEquals(1032, screen.getRedOffset());
        assertEquals(1036, screen.getRedLength());
        assertEquals(1040, screen.getRedMsbRight());
        assertEquals(1044, screen.getGreenOffset());
        assertEquals(1048, screen.getGreenLength());
        assertEquals(1052, screen.getGreenMsbRight());
        assertEquals(1056, screen.getBlueOffset());
        assertEquals(1060, screen.getBlueLength());
        assertEquals(1064, screen.getBlueMsbRight());
        assertEquals(1068, screen.getTranspOffset());
        assertEquals(1072, screen.getTranspLength());
        assertEquals(1076, screen.getTranspMsbRight());
        assertEquals(1080, screen.getNonstd());
        assertEquals(1084, screen.getActivate());
        assertEquals(1088, screen.getHeight());
        assertEquals(1092, screen.getWidth());
        assertEquals(1096, screen.getAccelFlags());
        assertEquals(1100, screen.getPixclock());
        assertEquals(1104, screen.getLeftMargin());
        assertEquals(1108, screen.getRightMargin());
        assertEquals(1112, screen.getUpperMargin());
        assertEquals(1116, screen.getLowerMargin());
        assertEquals(1120, screen.getHsyncLen());
        assertEquals(1124, screen.getVsyncLen());
        assertEquals(1128, screen.getSync());
        assertEquals(1132, screen.getVmode());
        assertEquals(1136, screen.getRotate());
    }

    @Test
    public void everySetterWritesItsField() {
        var screen = new EPDSystemShim.FbVarScreenInfoShim();
        screen.setRes(1, 2);
        screen.setVirtualRes(3, 4);
        screen.setOffset(5, 6);
        screen.setBitsPerPixel(7);
        screen.setGrayscale(8);
        screen.setRed(9, 10);
        screen.setGreen(11, 12);
        screen.setBlue(13, 14);
        screen.setTransp(15, 16);
        screen.setNonstd(17);
        screen.setActivate(18);
        screen.setHeight(19);
        screen.setWidth(20);
        screen.setAccelFlags(21);
        screen.setPixclock(22);
        screen.setLeftMargin(23);
        screen.setRightMargin(24);
        screen.setUpperMargin(25);
        screen.setLowerMargin(26);
        screen.setHsyncLen(27);
        screen.setVsyncLen(28);
        screen.setSync(29);
        screen.setVmode(30);
        screen.setRotate(31);
        int[] expected = {
            1, 2, 3, 4, 5, 6, 7, 8,
            10, 9, 0, 12, 11, 0, 14, 13, 0, 16, 15, 0,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            0, 0, 0, 0, 0
        };
        int[] actual = new int[FB_VAR_SCREENINFO_BYTES / Integer.BYTES];
        raw(screen).asIntBuffer().get(actual);
        assertArrayEquals(expected, actual, "fb_var_screeninfo as 40 host-order ints");
    }

    @Test
    public void mxcfbRequestCodesAreTheIowAndIorOfMxcfbH() {
        assertEquals(0x4018462B, EPDSystemShim.mxcfbSetWaveformModes(), "MXCFB_SET_WAVEFORM_MODES");
        assertEquals(0x4004462C, EPDSystemShim.mxcfbSetTemperature(), "MXCFB_SET_TEMPERATURE");
        assertEquals(0x4004462D, EPDSystemShim.mxcfbSetAutoUpdateMode(), "MXCFB_SET_AUTO_UPDATE_MODE");
        assertEquals(0x4044462E, EPDSystemShim.mxcfbSendUpdate(), "MXCFB_SEND_UPDATE");
        assertEquals(0x4004462F, EPDSystemShim.mxcfbWaitForUpdateComplete(), "MXCFB_WAIT_FOR_UPDATE_COMPLETE");
        assertEquals(0x40044630, EPDSystemShim.mxcfbSetPwrdownDelay(), "MXCFB_SET_PWRDOWN_DELAY");
        assertEquals(0x80044631, EPDSystemShim.mxcfbGetPwrdownDelay(), "MXCFB_GET_PWRDOWN_DELAY");
        assertEquals(0x40044632, EPDSystemShim.mxcfbSetUpdateScheme(), "MXCFB_SET_UPDATE_SCHEME");
    }

    @Test
    public void ioctlOfAnIntPassesTheAddressOfThatInt() {
        var backend = new LinuxSystemShim.FakeBackend();
        LinuxSystemShim.installBackendForTesting(backend);
        assertEquals(0, EPDSystemShim.ioctl(7, EPDSystemShim.mxcfbSetTemperature(), EPDSystemShim.TEMP_USE_AMBIENT));
        assertEquals(List.of(0x4004462C), backend.requests());
        assertArrayEquals(new int[] {EPDSystemShim.TEMP_USE_AMBIENT}, backend.argumentInts(0));
        backend.result(EPDSystemShim.mxcfbSetTemperature(), -1);
        assertEquals(-1, EPDSystemShim.ioctl(7, EPDSystemShim.mxcfbSetTemperature(), -20));
        assertArrayEquals(new int[] {-20}, backend.argumentInts(1));
    }
}
