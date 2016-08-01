/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

public enum PixelFormat {
    // Note: do not change the order of the formats as their ordinals may
    // be used for determining the format

    // 4-BYTE types
    INT_ARGB_PRE (DataType.INT,   1, true,  false),

    // the same format as INT_ARGB_PRE
    BYTE_BGRA_PRE(DataType.BYTE,  4, true,  false),

    // 3-BYTE types:
    BYTE_RGB     (DataType.BYTE,  3, true,  true),

    // L8, A8 types:
    // NOTE : we might need L8A8 16-bit type
    BYTE_GRAY    (DataType.BYTE,  1, true,  true),
    BYTE_ALPHA   (DataType.BYTE,  1, false, false),

    // Media types
    MULTI_YCbCr_420(DataType.BYTE,  1, false, true), // Multitexture format, requires pixel shader support
    BYTE_APPLE_422 (DataType.BYTE, 2, false, true),

    // flating point types:
    FLOAT_XYZW     (DataType.FLOAT, 4, false, true);

    /*
     * NOTE: BYTE_APPLE_422 is assumed to be '2vuy' component data, NOT 'yuvs'!
     */

    public enum DataType {
        BYTE (1),
        INT  (4),
        FLOAT(4);

        private int sizeInBytes;

        private DataType(int sizeInBytes) {
            this.sizeInBytes = sizeInBytes;
        }

        public int getSizeInBytes() {
            return sizeInBytes;
        }
    }

    // these need to match the plane indexes from JFXMedia
    public final static int YCBCR_PLANE_LUMA = 0;
    public final static int YCBCR_PLANE_CHROMARED = 1;
    public final static int YCBCR_PLANE_CHROMABLUE = 2;
    public final static int YCBCR_PLANE_ALPHA = 3;

    private DataType dataType;
    private int elemsPerPixelUnit;
    private boolean rgb;
    private boolean opaque;

    private PixelFormat(DataType dataType, int elemsPerPixelUnit,
                        boolean rgb, boolean opaque)
    {
        this.dataType = dataType;
        this.elemsPerPixelUnit = elemsPerPixelUnit;
        this.rgb = rgb;
        this.opaque = opaque;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int getBytesPerPixelUnit() {
        return elemsPerPixelUnit * dataType.getSizeInBytes();
    }

    public int getElemsPerPixelUnit() {
        return elemsPerPixelUnit;
    }

    public boolean isRGB() {
        return rgb;
    }

    public boolean isOpaque() {
        return opaque;
    }
}
