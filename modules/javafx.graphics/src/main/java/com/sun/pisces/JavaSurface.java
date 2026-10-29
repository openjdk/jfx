/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

public final class JavaSurface extends AbstractSurface {

    private final int[] dataInt;  // referenced by name in JNI
    private final int dataOffset;  // referenced by name in JNI

    /**
     * Constructs a new surface over the given array. If the buffer is too small or
     * the selected range would exceed the buffer's capacity, an {@link IllegalArgumentException} is thrown.
     *
     * @param buffer a buffer to back the surface with, cannot be {@code null}
     * @param dataType the pixel format to use, must be {@code RendererBase.TYPE_INT_ARGB_PRE}
     * @param width the width, cannot be negative
     * @param height the height, cannot be negative
     * @param offset the offset into the buffer, cannot be negative
     * @throws NullPointerException if {@code buffer} is {@code null}
     * @throws IllegalArgumentException if {@code dataType} is unsupported, {@code offset} is negative
     *     or if the selected range exceeds the available range
     */
    public JavaSurface(int[] buffer, int dataType, int width, int height, int offset) {
        super(width, height);

        if (dataType != RendererBase.TYPE_INT_ARGB_PRE) {
            throw new IllegalArgumentException("dataType is unsupported: " + dataType);
        }

        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative: " + offset);
        }

        long end = offset + (long) width * height;

        if (end > buffer.length) {  // implicit null check for buffer
            throw new IllegalArgumentException("selected range exceeds allowed range: [" + offset + ", " + end + ") exceeds [0, " + buffer.length + ")");
        }

        this.dataInt = buffer;
        this.dataOffset = offset;

        initialize(dataType, width, height);
        // The native method initialize() creates the native object of
        // struct JavaSurface and saves it's reference in the super class
        // member AbstractSurface.nativePtr. This reference is needed for
        // creating disposer record hence the below call to addDisposerRecord()
        // is needed here and cannot be made in super class constructor.
        addDisposerRecord();
    }

    private native void initialize(int dataType, int width, int height);
}
