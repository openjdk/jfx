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

#include <string>

#include "D3D12Types.hpp"


namespace D3D12 {
namespace Constants {

const std::string PASSTHROUGH_VS_NAME("PassThroughVS");
const std::string PHONG_VS_NAME("Mtl1VS");
const std::string PHONG_PS_NAME("Mtl1PS");
const std::string MIPMAP_CS_NAME("MipmapCS");

// currently supported D3D12 Feature Level
const D3D_FEATURE_LEVEL FEATURE_LEVEL = D3D_FEATURE_LEVEL_12_0;
// to return errors from RenderThread::Signal()
const unsigned int DEFAULT_ADAPTER = 0;
// mimics D3D9's D3DADAPTER_DEFAULT macro to pick something if upper layers don't care
// or don't provide us with a native handle (see Web OpacityTest/ShadowTest tests)
const uint64_t INVALID_FENCE_VALUE = 0;

// 2D limitations and constants
const unsigned int MAX_BATCH_QUADS = 4096; // follows other backends and Prism
const unsigned int MAX_BATCH_VERTICES = MAX_BATCH_QUADS * 4;
const unsigned int MAX_BATCH_INDICES = MAX_BATCH_QUADS * 6;
const unsigned int VERTEX_2D_SIZE_BYTES = sizeof(Vertex_2D);
const unsigned int MAX_BATCH_VERTICES_BYTES = MAX_BATCH_VERTICES * VERTEX_2D_SIZE_BYTES;

// 3D + general constants used across the backend
const unsigned int VERTEX_BUFFER_3D_STRIDE = 9 * sizeof(float); // 3 * modelVertexPos; 2 * texD; 4 * modelVertexNormal
const unsigned int MAX_TEXTURE_UNITS = 4; // see BaseShaderContext.java validateTextureOp()
const unsigned int MAX_LIGHTS = 3; // for now we support only up to 3 lights per mesh view
const unsigned int MAX_MSAA_SAMPLE_COUNT = 16; // limit for MSAA sample count, same as D3D9
const unsigned int LINEAR_ALLOCATOR_CHUNK_SIZE = 1024 * 1024; // for Payloads 1M should be sufficient
const unsigned int COMMAND_LIST_POOL_MAX_LISTS = 16; // kind of a guess, could be increased if we exhaust CLs
const unsigned int COMMAND_LIST_POOL_MAX_ALLOCATORS = 4; // one allocator per ring container threshold + one extra to potentially keep recording while CLs execute
const unsigned int RESOURCE_BARRIERS_QUEUE_SIZE = 8; // should be big enough to fit all ResourceBarriers in one API call
const unsigned int SHADER_CONSTANT_DATA_ALIGNMENT_BYTES = 16; // internally D3D12 shader constants align to 16 bytes

// incoming vertex/quad data from Prism
const unsigned int FLOATS_PER_2D_VERTEX = 7; // vertices array incoming to DrawQuads contains 7 floats - XYZ, UV1 and UV2
const unsigned int COLORS_PER_2D_VERTEX = 4; // colors array incoming to DrawQuads contains 4 unsigned chars - RGBA
const unsigned int FLOATS_PER_2D_VERTEX_BYTES = FLOATS_PER_2D_VERTEX * sizeof(float);
const unsigned int COLORS_PER_2D_VERTEX_BYTES = COLORS_PER_2D_VERTEX * sizeof(signed char);
const unsigned int FLOATS_PER_2D_QUAD = FLOATS_PER_2D_VERTEX * 4; // 4 verts per quad
const unsigned int COLORS_PER_2D_QUAD = COLORS_PER_2D_VERTEX * 4; // 4 verts per quad

} // namespace Constants
} // namespace D3D12
