/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

public abstract class AbstractSurface implements Surface {
    
    private long nativePtr = 0L;
    private int width;
    private int height;

    AbstractSurface(int width, int height) {
        if (width < 0) {
            throw new IllegalArgumentException("WIDTH must be positive");
        }
        if (height < 0) {
            throw new IllegalArgumentException("HEIGHT must be positive");
        }
        final int nbits = 32-Integer.numberOfLeadingZeros(width) + 32-Integer.numberOfLeadingZeros(height);
        if (nbits > 31) {
            throw new IllegalArgumentException("WIDTH * HEIGHT is too large");
        }
        this.width = width;
        this.height = height;
    }
        
    public native void getRGB(int[] argb, int offset, int scanLength,
            int x, int y, int width, int height);
    
    public native void setRGB(int[] argb, int offset, int scanLength, 
            int x, int y, int width, int height);

    protected void finalize() {
        this.nativeFinalize();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private native void nativeFinalize();
}
