/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12.ni;

import com.sun.prism.PixelFormat;


/*
 * Converter from Prism's PixelFormat to DXGI_FORMAT constants.
 *
 * Values taken from dxgiformat.h header in Windows SDK and following
 * d3d9-to-d3d10 format mapping by MSDN:
 *   https://learn.microsoft.com/en-us/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-legacy-formats
 */
enum DXGIFormat {

    UNKNOWN(0),

    //FLOAT_XYZW     (DataType.FLOAT, 4, false, true);
    R32G32B32A32_FLOAT(2),

    // 4-BYTE types
    //INT_ARGB_PRE (DataType.INT,   1, true,  false),
    //BYTE_BGRA_PRE(DataType.BYTE,  4, true,  false),
    //BYTE_RGB     (DataType.BYTE,  3, true,  true),
    //BYTE_RGB     (DataType.BYTE,  3, true,  true),
    B8G8R8A8_UNORM(87),

    // L8, A8 types:
    //BYTE_GRAY    (DataType.BYTE,  1, true,  true),
    R8_UNORM(61), // TODO: D3D12: has to be emulated in the shader
    //BYTE_ALPHA   (DataType.BYTE,  1, false, false),
    A8_UNORM(65),

    // Media types
    //MULTI_YCbCr_420 (DataType.BYTE,  1, false, true), // Multitexture format, requires pixel shader support
    NV12(103);

    //BYTE_APPLE_422 (DataType.BYTE, 2, false, true),
    // UNUSED in D3D - will map to UNKNOWN for error reporting

    public final int format;

    DXGIFormat(int format) {
        this.format = format;
    }

    public static DXGIFormat fromPixelFormat(PixelFormat format) {
        switch (format) {
        case FLOAT_XYZW:
            return R32G32B32A32_FLOAT;
        case INT_ARGB_PRE:
        case BYTE_BGRA_PRE:
            return B8G8R8A8_UNORM;
        case BYTE_RGB:
            // D3D12 does not have a 3-byte format
            // closest one is B8G8R8X8, but it cannot be used in compute shaders as UAV
            // and we sometimes need that access to generate mipmaps for 3D
            // thus, we will simply upload it as BGRA and force alpha channel to 1.0
            return B8G8R8A8_UNORM;
        case BYTE_GRAY:
            return R8_UNORM;
        case BYTE_ALPHA:
            return A8_UNORM;
        case MULTI_YCbCr_420:
            return NV12;
        default:
            // also includes BYTE_APPLE_422 cause we're on Windows
            return UNKNOWN;
        }
    }
}
