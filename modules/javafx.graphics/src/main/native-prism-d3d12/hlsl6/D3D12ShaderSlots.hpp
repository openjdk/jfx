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

#pragma once

namespace D3D12 {
namespace ShaderSlots {

// Below assingments must match root signature entry indexes in ShaderCommon.hlsl

// Graphics RS

const unsigned int GRAPHICS_RS_VS_DATA = 0; // Root Descriptor
const unsigned int GRAPHICS_RS_PS_DATA = 1; // Root Descriptor

const unsigned int GRAPHICS_RS_VS_DATA_DTABLE = 2; // Descriptor Table
const unsigned int GRAPHICS_RS_PS_DATA_DTABLE = 3; // Descriptor Table

const unsigned int GRAPHICS_RS_PS_TEXTURE_DTABLE = 4; // Descriptor Table
const unsigned int GRAPHICS_RS_PS_SAMPLER_DTABLE = 5; // Descriptor Table


// Compute RS

const unsigned int COMPUTE_RS_CONSTANT_DATA = 0; // Root Descriptor

const unsigned int COMPUTE_RS_UAV_DTABLE = 1; // Descriptor Table
const unsigned int COMPUTE_RS_TEXTURE_DTABLE = 2; // Descriptor Table

} // namespace ShaderSlots
} // namespace D3D12
