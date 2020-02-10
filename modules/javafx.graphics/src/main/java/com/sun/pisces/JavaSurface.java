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

import java.nio.IntBuffer;

public final class JavaSurface extends AbstractSurface {

    private IntBuffer dataBuffer;

    private int[] dataInt;

    public JavaSurface(int[] dataInt, int dataType, int width, int height) {
        super(width, height);
        if (dataInt.length / width < height) {
            throw new IllegalArgumentException("width(=" + width + ") * height(="
                    + height + ") is greater than dataInt.length(=" + dataInt.length + ")");
        }
        this.dataInt = dataInt;
        this.dataBuffer = IntBuffer.wrap(this.dataInt);

        initialize(dataType, width, height);
        // The native method initialize() creates the native object of
        // struct JavaSurface and saves it's reference in the super class
        // member AbstractSurface.nativePtr. This reference is needed for
        // creating disposer record hence the below call to addDisposerRecord()
        // is needed here and cannot be made in super class constructor.
        addDisposerRecord();
    }

    public IntBuffer getDataIntBuffer() {
        return this.dataBuffer;
    }

    private native void initialize(int dataType, int width, int height);
}
