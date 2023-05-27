/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.TreeSet;

final class GtkView extends View {
    private native void enableInputMethodEventsImpl(long ptr, boolean enable);

    @Override
    protected void _enableInputMethodEvents(long ptr, boolean enable) {
        enableInputMethodEventsImpl(ptr, enable);
    }

    @Override
    protected int _getNativeFrameBuffer(long ptr) {
        return 0;
    }

    @Override
    protected native long _create(Map caps);

    @Override
    protected native long _getNativeView(long ptr);

    @Override
    protected native int _getX(long ptr);

    @Override
    protected native int _getY(long ptr);

    @Override
    protected native void _setParent(long ptr, long parentPtr);

    @Override
    protected native boolean _close(long ptr);

    @Override
    protected native void _scheduleRepaint(long ptr);

    @Override
    protected void _begin(long ptr) {}

    @Override
    protected void _end(long ptr) {}

    @Override
    protected void _uploadPixels(long ptr, Pixels pixels) {
        Buffer data = pixels.getPixels();
        if (data.isDirect() == true) {
            _uploadPixelsDirect(ptr, data, pixels.getWidth(), pixels.getHeight());
        } else if (data.hasArray() == true) {
            if (pixels.getBytesPerComponent() == 1) {
                ByteBuffer bytes = (ByteBuffer)data;
                _uploadPixelsByteArray(ptr, bytes.array(), bytes.arrayOffset(), pixels.getWidth(), pixels.getHeight());
            } else {
                IntBuffer ints = (IntBuffer)data;
                _uploadPixelsIntArray(ptr, ints.array(), ints.arrayOffset(), pixels.getWidth(), pixels.getHeight());
            }
        } else {
            // gznote: what are the circumstances under which this can happen?
            _uploadPixelsDirect(ptr, pixels.asByteBuffer(), pixels.getWidth(), pixels.getHeight());
        }
    }
    private native void _uploadPixelsDirect(long viewPtr, Buffer pixels, int width, int height);
    private native void _uploadPixelsByteArray(long viewPtr, byte[] pixels, int offset, int width, int height);
    private native void _uploadPixelsIntArray(long viewPtr, int[] pixels, int offset, int width, int height);

    @Override
    protected native boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor);

    @Override
    protected native void _exitFullscreen(long ptr, boolean animate);

    @Override
    protected void _finishInputMethodComposition(long ptr) {
        //nothing
    }

    protected void notifyInputMethodLinux(String str, int attrib, int length,
                                          int cursor, int selStart, int selLength) {
        byte atts[] = new byte[1];
        atts[0] = (byte) attrib;
        int attBounds[] = new int[2];
        attBounds[0] = 0;
        attBounds[1] = length;

        if (attrib == 4) {
            // attrib == 4 means we are going to commit changes, so commitLength should be non-zero
            notifyInputMethod(str, null, attBounds, atts, length, cursor, 0);
        } else {
            // all other cases = just an update, update preview text but do not commit it
            if (selLength > 0
                    && str != null && str.length() > 0
                    && selStart >= 0
                    && selLength + selStart <= str.length()) {

                TreeSet<Integer> b = new TreeSet<>();
                b.add(0);
                b.add(selStart);
                b.add(selStart + selLength);
                b.add(str.length());

                int[] boundary = new int[b.size()];
                int i = 0;
                for (int e : b) {
                    boundary[i] = e;
                    i++;
                }

                byte[] values = new byte[boundary.length - 1];

                for (i = 0; i < boundary.length - 1; i++) {
                    values[i] = (boundary[i] == selStart)
                            ? IME_ATTR_TARGET_CONVERTED
                            : IME_ATTR_CONVERTED;
                }

                notifyInputMethod(str, boundary, boundary, values, 0, cursor, 0);
            } else {
                notifyInputMethod(str, null, attBounds, atts, 0, cursor, 0);
            }
        }
    }

    protected double[] notifyInputMethodCandidatePosRequest(int offset) {
        double[] pos = super.notifyInputMethodCandidatePosRequest(offset);

        var w = getWindow();
        //On Linux values are relative
        pos[0] -= (pos[0] > 0) ? w.getX() + getX() : 0;
        pos[1] -= (pos[1] > 0) ? w.getY() + getY() : 0;

        return pos;
    }
}
