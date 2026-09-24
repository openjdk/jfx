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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The ioctl request codes LinuxSystem computes in Java, against the values the kernel headers give them:
 * {@code _IOC(dir, type, nr, size) = dir << 30 | size << 16 | type << 8 | nr} with {@code _IOC_WRITE 1} and
 * {@code _IOC_READ 2} from {@code asm-generic/ioctl.h} (the encoding of x86, x86-64, arm and arm64, whose
 * {@code asm/ioctl.h} all include the generic one); {@code EVIOCGABS(abs) = _IOR('E', 0x40 + (abs), struct
 * input_absinfo)} and {@code EVIOCGRAB = _IOW('E', 0x90, int)} from {@code linux/input.h}; the FBIO* requests
 * from {@code linux/fb.h}. Pure arithmetic: runs on every platform and binds nothing.
 */
public class LinuxSystemIoctlEncodingTest {

    @Test
    public void eviocgrabIsIowOfAnInt() {
        assertEquals(0x40044590, LinuxSystemShim.IOW('E', 0x90, 4));
    }

    @Test
    public void eviocgabsIsIorOfInputAbsinfoForEveryAxis() {
        for (int axis = 0; axis <= 0x3f; axis++) {
            assertEquals(0x80184540 + axis, LinuxSystemShim.EVIOCGABS(axis), "EVIOCGABS(" + axis + ")");
        }
    }

    @Test
    public void framebufferRequestsOfLinuxFrameBufferAndEpd() {
        assertEquals(0x40044620, LinuxSystemShim.IOW('F', 0x20, 4), "FBIO_WAITFORVSYNC");
        assertEquals(0x4018462B, LinuxSystemShim.IOW('F', 0x2B, 24), "MXCFB_SET_WAVEFORM_MODES");
        assertEquals(0x4044462E, LinuxSystemShim.IOW('F', 0x2E, 68), "MXCFB_SEND_UPDATE");
        assertEquals(0x80044631, LinuxSystemShim.IOR('F', 0x31, 4), "MXCFB_GET_PWRDOWN_DELAY");
        assertEquals(0xC0044624, LinuxSystemShim.IOWR('F', 0x24, 4), "_IOWR('F', 0x24, 4)");
    }

    @Test
    public void fbioConstantsMatchLinuxFbH() {
        assertEquals(0x4600, LinuxSystemShim.FBIOGET_VSCREENINFO);
        assertEquals(0x4601, LinuxSystemShim.FBIOPUT_VSCREENINFO);
        assertEquals(0x4606, LinuxSystemShim.FBIOPAN_DISPLAY);
        assertEquals(0x4611, LinuxSystemShim.FBIOBLANK);
    }
}
