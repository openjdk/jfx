/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Size;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class SoftwareCursor extends NativeCursor {

    private ByteBuffer cursorBuffer;
    private int renderX;
    private int renderY;
    private int hotspotX;
    private int hotspotY;

    @Override
    public Size getBestSize() {
        return new Size(16, 16);
    }

    @Override
    public void setVisibility(boolean visibility) {
        if (visibility != isVisible) {
            isVisible = visibility;
            MonocleWindowManager.getInstance().repaintAll();
        }
    }

    @Override
    public void setImage(byte[] cursorImage) {
        cursorBuffer = ByteBuffer.allocate(cursorImage.length);
        NativeCursors.colorKeyCursor(cursorImage, cursorBuffer.asIntBuffer(), 32, 0);
    }

    @Override
    public void setLocation(int x, int y) {
        int renderX = x - hotspotX;
        int renderY = y - hotspotY;
        if (renderX != this.renderX || renderY != this.renderY) {
            this.renderX = renderX;
            this.renderY = renderY;
            MonocleWindowManager.getInstance().repaintAll();
        }
    }

    @Override
    public void setHotSpot(int hotspotX, int hotspotY) {
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    @Override
    public void shutdown() {
    }

    public int getRenderX() {
        return renderX;
    }

    public int getRenderY() {
        return renderY;
    }

    public Buffer getCursorBuffer() {
        cursorBuffer.clear();
        return cursorBuffer;
    }

}
