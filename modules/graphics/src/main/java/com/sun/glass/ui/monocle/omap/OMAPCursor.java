/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.omap;

import com.sun.glass.ui.Size;
import com.sun.glass.ui.monocle.NativeCursor;
import com.sun.glass.ui.monocle.linux.SysFS;

import java.io.IOException;

/** Cursor using a framebuffer overlay on OMAP3.
 * TODO: Can we mmap the image and location files?
 */
public class OMAPCursor implements NativeCursor {

    private int hotspotX;
    private int hotspotY;

    public OMAPCursor() {
        try {
            SysFS.write("/sys/class/graphics/fb1/virtual_size", "16,16");
            SysFS.write("/sys/devices/platform/omapdss/overlay1/output_size", "16,16");
        } catch (IOException e) {
            System.err.println("Failed to initialize OMAP cursor");
        }
    }

    @Override
    public Size getBestSize() {
        return new Size(16, 16);
    }

    @Override
    public void setVisibility(boolean visibility) {
        try {
            SysFS.write("/sys/devices/platform/omapdss/overlay1/enabled",
                        visibility ? "1" : "0");
        } catch (IOException e) {
            System.err.format("Failed to %s OMAP cursor\n",
                              (visibility ? "enable" : "disable"));
        }
    }

    @Override
    public void setImage(byte[] cursorImage) {
        try {
            SysFS.write("/dev/fb1", cursorImage);
        } catch (IOException e) {
            System.err.println("Failed to write OMAP cursor image");
        }
    }

    @Override
    public void setLocation(int x, int y) {
        try {
            SysFS.write("/sys/devices/platform/omapdss/overlay1/position",
                        (x - hotspotX) + "," + (y - hotspotY));
        } catch (IOException e) {
            System.err.println("Failed to set OMAP cursor position");
        }
    }

    @Override
    public void setHotSpot(int hotspotX, int hotspotY) {
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    @Override
    public void shutdown() {
        try {
            SysFS.write("/sys/devices/platform/omapdss/overlay1/enabled", "0");
        } catch (IOException e) {
            System.err.println("Failed to shut down OMAP cursor");
        }
    }
}
