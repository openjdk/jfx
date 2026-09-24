/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Pixels;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * MS Windows platform implementation class for Pixels.
 * <p>
 * All four natives are gone. {@code _initIDs} returned a constant (and cached the one
 * {@code jmethodID} that only {@code Pixels::Pixels(JNIEnv*, jobject)} read), {@code _fillDirectByteBuffer}
 * was a {@code memcpy} between two buffers Java can already reach, and {@code _attachInt} /
 * {@code _attachByte} were the return leg of the {@code Pixels.attachData} upcall that the JNI bodies of
 * {@code GlassView._uploadPixels} and {@code GlassWindow._setIcon} made. Both peers are flipped
 * ({@code WinView} resolves the buffer in {@code WinGlassNative.pixelBits}, {@code WinWindow} builds the
 * {@code HICON} with {@code WinGlassNative.iconCreate}) and the C constructor that dialled
 * {@code attachData} is deleted, so the two attach overrides below cannot be reached on Windows: they
 * throw, as {@code HeadlessPixels} does, rather than accept a handle with no native {@code Pixels} behind
 * it. {@code attachData} and its two abstract legs stay in {@code Pixels} for the GTK and macOS JNI.
 * <p>
 * Line numbers into {@code Pixels.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/Pixels.cpp}).
 */
final class WinPixels extends Pixels {

    /**
     * What {@code Java_com_sun_glass_ui_win_WinPixels__1initIDs} returned:
     * {@code com_sun_glass_ui_Pixels_Format_BYTE_BGRA_PRE} ({@code Pixels.cpp:242}), which is the
     * {@code @Native}-pinned {@code 1} of {@code Pixels.java:55} - the same constant the generated
     * header handed the C.
     */
    private static final int nativeFormat = Format.BYTE_BGRA_PRE;

    protected WinPixels(int width, int height, ByteBuffer data) {
        super(width, height, data);
    }

    protected WinPixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    protected WinPixels(int width, int height, IntBuffer data) {
        super(width, height, data);
    }

    protected WinPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    static int getNativeFormat_impl() {
        return nativeFormat;
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinPixels__1fillDirectByteBuffer}
     * ({@code Pixels.cpp:272-297}) in Java: {@code width * height * 4} bytes copied to the
     * <em>base</em> of {@code bb}, whatever its position, and whatever byte order either side
     * declares. Every rejection of the C is kept, silent and in its order; only the shape of the
     * "not a direct buffer" test differs, because {@code isDirect()} answers in one call what the C
     * discovered from {@code GetDirectBufferCapacity} returning -1 and then
     * {@code GetDirectBufferAddress} returning {@code NULL} ({@code Pixels.cpp:287-295}).
     * <p>
     * The int path copies raw memory, exactly as the {@code memcpy} of {@code Pixels.cpp:296} did:
     * the destination view is given the <em>source's</em> byte order, so an {@code IntBuffer} that
     * was viewed big-endian over its memory delivers the bytes it actually holds instead of having
     * every pixel silently byte-swapped. {@code com.sun.glass.ui.Pixels} makes byte order the
     * caller's business ({@code Pixels.java:44-52}); raw memory is the contract.
     * <p>
     * Two differences from the C, both of them refusals where the C wrote anyway, and neither
     * reachable from a valid caller: a read-only direct {@code bb} raises
     * {@code ReadOnlyBufferException} here where the C wrote straight through the read-only view, and
     * a read-only heap <em>source</em> works here where the JNI could not survive one at all
     * ({@code Pixels.attachData} calls {@code array()} on it, {@code Pixels.java:245}, and
     * {@code Pixels::Pixels} swallows the exception and then reads uninitialised fields).
     */
    @Override
    protected void _fillDirectByteBuffer(ByteBuffer bb) {
        if (bb == null || !bb.isDirect()) {
            return;
        }
        // The protected fields, not getWidth()/getHeight(): those add an Application.checkEventThread()
        // (Pixels.java:155-167) that the JNI path did not make.
        if (this.width <= 0 || this.height <= 0
                || this.width > ((Integer.MAX_VALUE / 4) / this.height)) {
            return;
        }
        final int size = this.width * this.height * 4;
        if (bb.capacity() < size) {
            return;
        }
        ByteBuffer destination = bb.duplicate();
        destination.clear();
        if (this.bytes != null) {
            ByteBuffer source = this.bytes.duplicate();
            source.clear();
            destination.put(0, source, 0, size);
        } else {
            IntBuffer source = this.ints.duplicate();
            source.clear();
            destination.order(source.order()).asIntBuffer().put(0, source, 0, size / 4);
        }
    }

    /**
     * Unreachable on Windows: {@code Pixels.attachData}, the only caller of the two attach legs, was
     * dialled by the deleted {@code Pixels::Pixels(JNIEnv*, jobject)} and by nothing in Java. Throws, as
     * {@code HeadlessPixels} does, rather than silently accept a handle that no native {@code Pixels}
     * stands behind any more.
     */
    @Override
    protected void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset) {
        throw new UnsupportedOperationException("WinPixels has no native Pixels to attach to");
    }

    @Override
    protected void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset) {
        throw new UnsupportedOperationException("WinPixels has no native Pixels to attach to");
    }
}
