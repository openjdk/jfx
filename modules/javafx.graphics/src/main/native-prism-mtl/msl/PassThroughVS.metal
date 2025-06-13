/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

typedef struct VS_INPUT {
    packed_float3 position;
    packed_float2 texCoord0;
    packed_float2 texCoord1;
} VS_INPUT;

typedef struct VS_COLOR {
    uchar4 color;
} VS_COLOR;

typedef struct VS_OUTPUT
{
    vector_float4 position [[position]];
    vector_float4 fragColor;
    vector_float2 texCoord0;
    vector_float2 texCoord1;
} VS_OUTPUT;

[[vertex]] VS_OUTPUT passThrough(const uint v_id [[ vertex_id ]],
                      constant VS_INPUT * v_in [[ buffer(0) ]],
                      constant float4x4 & mvp_matrix [[ buffer(1) ]],
                      constant VS_COLOR * c_in [[ buffer(2) ]])
{
    VS_OUTPUT out;
    out.position    = vector_float4(v_in[v_id].position.xy, 0.0, 1.0) * mvp_matrix;
    out.fragColor.r = c_in[v_id].color.r / 255.0f;
    out.fragColor.g = c_in[v_id].color.g / 255.0f;
    out.fragColor.b = c_in[v_id].color.b / 255.0f;
    out.fragColor.a = c_in[v_id].color.a / 255.0f;
    out.texCoord0   = v_in[v_id].texCoord0;
    out.texCoord1   = v_in[v_id].texCoord1;
    return out;
}
