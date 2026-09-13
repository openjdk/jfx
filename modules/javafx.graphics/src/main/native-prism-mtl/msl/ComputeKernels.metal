/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

[[kernel]]  void uyvy422_to_rgba(const device uchar4 *YUV422Buff [[buffer(0)]],
                            texture2d<half, access::write> outTex [[texture(0)]],
                            uint2 gid [[thread_position_in_grid]])
{
    uchar4 uyvy = YUV422Buff[(gid.y * outTex.get_width()/2) + gid.x/2];

    half u = (uyvy.r/255.0) - 0.5;
    half v = (uyvy.b/255.0) - 0.5;

    half compR = (half)(1.402 * v);
    half compG = (half)(0.34414 * u + 0.71414 * v);
    half compB = (half)(1.772 * u);

    if (gid.x % 2 == 0) {
        // Pixel at even x position

        half y1 = uyvy.g/255.0;

        compR = clamp((y1 + compR), (half)0.0f, (half)1.0f);
        compG = clamp((y1 - compG), (half)0.0f, (half)1.0f);
        compB = clamp((y1 + compB), (half)0.0f, (half)1.0f);
    } else {
        // Pixel at odd x position

        half y2 = uyvy.a/255.0;

        compR = clamp((y2 + compR), (half)0.0f, (half)1.0f);
        compG = clamp((y2 - compG), (half)0.0f, (half)1.0f);
        compB = clamp((y2 + compB), (half)0.0f, (half)1.0f);
    }

    outTex.write(half4(compR, compG, compB, 1.0), gid);


/* // This is another way to write this shader -----

    if (gid.x % 2 != 0) {
        // 2 RGBA pixels are generated from 4-bytes of YUV422 data
        // A pixel at even x position in outTex, also generates adjacent pixel at x+1 (odd) position
        // Hence, pixels at odd x positions in outTex need not be calculated again
        return;
    }

    uchar4 uyvy = YUV422Buff[(gid.y * outTex.get_width()/2) + gid.x/2];

    half u = (uyvy.r/255.0) - 0.5;
    half y1 = uyvy.g/255.0;
    half v = (uyvy.b/255.0) - 0.5;
    half y2 = uyvy.a/255.0;

    half compR = (half)(1.402 * v);
    half compG = (half)(0.34414 * u + 0.71414 * v);
    half compB = (half)(1.772 * u);

    // Compute the color of 1st pixel ---------------------------

    half r = clamp((y1 + compR), (half)0.0f, (half)1.0f);
    half g = clamp((y1 - compG), (half)0.0f, (half)1.0f);
    half b = clamp((y1 + compB), (half)0.0f, (half)1.0f);

    outTex.write(half4(r, g, b, 1.0), gid);

    // Compute the color of 2nd pixel ---------------------------

    r = clamp((y2 + compR), (half)0.0f, (half)1.0f);
    g = clamp((y2 - compG), (half)0.0f, (half)1.0f);
    b = clamp((y2 + compB), (half)0.0f, (half)1.0f);

    outTex.write(half4(r, g, b, 1.0), uint2(gid.x+1, gid.y));
    */
}
