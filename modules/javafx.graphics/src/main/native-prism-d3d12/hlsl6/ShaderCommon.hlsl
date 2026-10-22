/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12ShaderLimits.h"

#define JFX_INTERNAL_GRAPHICS_RS \
    "RootFlags(ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT)," \
    "CBV(b0, visibility = SHADER_VISIBILITY_VERTEX)," \
    "CBV(b0, visibility = SHADER_VISIBILITY_PIXEL)," \
    "DescriptorTable(" \
        "CBV(b1, numDescriptors = " STR(D3D12NI_SHADER_LIMITS_MAX_VERTEX_CBV_DTABLE_ENTRIES) ")," \
        "visibility = SHADER_VISIBILITY_VERTEX" \
    ")," \
    "DescriptorTable(" \
        "CBV(b1, numDescriptors = " STR(D3D12NI_SHADER_LIMITS_MAX_PIXEL_CBV_DTABLE_ENTRIES) ")," \
        "visibility = SHADER_VISIBILITY_PIXEL" \
    ")," \
    "DescriptorTable(" \
        "SRV(t0, numDescriptors = " STR(D3D12NI_SHADER_LIMITS_MAX_PIXEL_SRV_DTABLE_ENTRIES) ")," \
        "visibility = SHADER_VISIBILITY_PIXEL" \
    ")," \
    "DescriptorTable(" \
        "Sampler(s0, numDescriptors = " STR(D3D12NI_SHADER_LIMITS_MAX_PIXEL_SRV_DTABLE_ENTRIES) ")," \
        "visibility = SHADER_VISIBILITY_PIXEL" \
    "),"

#define JFX_INTERNAL_COMPUTE_RS \
    "RootFlags(0)," \
    "CBV(b0)," \
    "DescriptorTable(" \
        "UAV(u0, numDescriptors = " STR(D3D12NI_SHADER_LIMITS_MAX_COMPUTE_UAV_DTABLE_ENTRIES) ")" \
    ")," \
    "DescriptorTable(" \
        "SRV(t0, numDescriptors = " STR(D3D12NI_SHADER_LIMITS_MAX_COMPUTE_SRV_DTABLE_ENTRIES) ")" \
    ")," \
    "StaticSampler(s0),"

