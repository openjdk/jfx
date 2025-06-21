/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

// Following two clearVF/FF functions are used only for drawing a clear rectangle.
// These are specific to metal implementation and not present for D3D/ES2.

typedef struct CLEAR_VS_INPUT
{
    packed_float2 position;
} CLEAR_VS_INPUT;

typedef struct CLEAR_VS_OUTPUT
{
    vector_float4 position [[position]];
} CLEAR_VS_OUTPUT;

[[vertex]] CLEAR_VS_OUTPUT clearVF(const    uint            v_id [[ vertex_id ]],
                                   constant CLEAR_VS_INPUT* v_in [[ buffer(0) ]])
{
    CLEAR_VS_OUTPUT out;
    out.position = vector_float4(v_in[v_id].position.xy, 0.0, 1.0);
    return out;
}

[[fragment]] float4 clearFF(constant float4& color [[ buffer(2) ]])
{
    return color;
}
