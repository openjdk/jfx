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

package com.sun.pisces;

import java.nio.IntBuffer;

/**
 * A software surface that renders into a direct {@link IntBuffer}.
 * <p>
 * The pixel data is always read and written starting at element zero of the
 * provided buffer, and the buffer must therefore provide storage for at least
 * {@code width * height} elements.
 */
public final class DirectBufferSurface extends AbstractSurface {

    private final IntBuffer dataBuffer;  // referenced by name in JNI

    /**
     * Constructs a new surface over the given direct buffer. If the buffer is too small for
     * the given width and height an {@link IllegalArgumentException} is thrown.
     *
     * @param buffer a buffer to back the surface with, cannot be {@code null}
     * @param dataType the pixel format to use, must be {@code RendererBase.TYPE_INT_ARGB_PRE}
     * @param width the width, cannot be negative
     * @param height the height, cannot be negative
     * @throws NullPointerException if {@code buffer} is {@code null}
     * @throws IllegalArgumentException if {@code dataType} is unsupported, {@code buffer} is not a direct
     *     writable buffer, or {@code buffer} is too small for the given {@code width} and {@code height}
     */
    public DirectBufferSurface(IntBuffer buffer, int dataType, int width, int height) {
        super(width, height);

        if (dataType != RendererBase.TYPE_INT_ARGB_PRE) {
            throw new IllegalArgumentException("dataType is unsupported: " + dataType);
        }

        if (!buffer.isDirect()) {  // implicit null check for buffer
            throw new IllegalArgumentException("buffer must be a direct buffer");
        }

        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("buffer must not be read-only");
        }

        if ((long) width * height > buffer.capacity()) {
            throw new IllegalArgumentException("width x height exceeds buffer capacity: " + width + "x" + height + " > " + buffer.capacity());
        }

        this.dataBuffer = buffer;

        initialize(dataType, width, height);
        // The native method initialize() creates the native object of
        // struct DirectBufferSurface and saves it's reference in the super class
        // member AbstractSurface.nativePtr. This reference is needed for
        // creating disposer record hence the below call to addDisposerRecord()
        // is needed here and cannot be made in super class constructor.
        addDisposerRecord();
    }

    /**
     * Returns the buffer that stores the pixel data of this surface.
     *
     * @return the direct buffer backing this surface, never {@code null}
     */
    public IntBuffer getDataBuffer() {
        return this.dataBuffer;
    }

    private native void initialize(int dataType, int width, int height);
}
