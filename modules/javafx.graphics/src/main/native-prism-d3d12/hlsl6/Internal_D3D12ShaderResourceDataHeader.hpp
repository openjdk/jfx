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

#include "D3D12Common.hpp"
#include "D3D12Constants.hpp"
#include "D3D12ShaderSlots.hpp"

#include <map>
#include <vector>


namespace D3D12 {
namespace InternalShaderResource {

struct ResourceBinding
{
    const char* name;
    ResourceAssignmentType type;
    uint32_t rootIndex; // NOTE: this value CAN be equal across multiple entries
                     // uniforms that are ex. together in a struct exist within the same shader slot
    uint32_t count; // for CBV assumes a multiple of 32-bits (ex. floats);
                    // for textures it is the amount of texture slots taken
    uint32_t size; // 0 for textures and samplers, size in bytes for CBVs per one CBV
};

using ResourceBindings = std::vector<ResourceBinding>;

struct ShaderResources
{
    ResourceBindings constantBuffers;
    ResourceBindings textures;
    ResourceBindings samplers;
};

using ShaderResourceCollection = std::map<const char*, ShaderResources, CStringComparer>;


ResourceBindings PassThroughVSConstantBuffers = {
    { "WorldViewProj", ResourceAssignmentType::DESCRIPTOR, ShaderSlots::GRAPHICS_RS_VS_DATA, 1, 16 * sizeof(float) }
};


ResourceBindings Mtl1VSConstantBuffers = {
    { "gData", ResourceAssignmentType::DESCRIPTOR, ShaderSlots::GRAPHICS_RS_VS_DATA, 1, (4 + 16 + 16) * sizeof(float) },
    { "gLight", ResourceAssignmentType::DESCRIPTOR_TABLE_CBUFFERS, ShaderSlots::GRAPHICS_RS_VS_DATA_DTABLE, Constants::MAX_LIGHTS, (4 + 4) * sizeof(float) },
};

ResourceBindings Mtl1PSConstantBuffers = {
    { "gColor", ResourceAssignmentType::DESCRIPTOR, ShaderSlots::GRAPHICS_RS_PS_DATA, 1, (4 + 4 + 4) * sizeof(float) },
    { "gLight", ResourceAssignmentType::DESCRIPTOR_TABLE_CBUFFERS, ShaderSlots::GRAPHICS_RS_PS_DATA_DTABLE, Constants::MAX_LIGHTS, (4 + 4 + 4 + 4) * sizeof(float) },
};

// TODO: D3D12: assumes all textures share the same DTable and it always has 4 spots. This might be reduced in shaders
//              where there some of the maps are not used (for now we allocate null descriptors there). This could optimize
//              memory consumption and frequency of Ring Descriptor Heap flushing.
ResourceBindings Mtl1PSTextures = {
    { "mapDiffuse", ResourceAssignmentType::DESCRIPTOR_TABLE_TEXTURES, ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, 1, 0 },
    { "mapSpecular", ResourceAssignmentType::DESCRIPTOR_TABLE_TEXTURES, ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, 1, 0 },
    { "mapBumpHeight", ResourceAssignmentType::DESCRIPTOR_TABLE_TEXTURES, ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, 1, 0 },
    { "mapSelfIllum", ResourceAssignmentType::DESCRIPTOR_TABLE_TEXTURES, ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, 1, 0 },
};

ResourceBindings Mtl1PSSamplers = {
    { "samplerDiffuse", ResourceAssignmentType::DESCRIPTOR_TABLE_SAMPLERS, ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, 1, 0 },
    { "samplerSpecular", ResourceAssignmentType::DESCRIPTOR_TABLE_SAMPLERS, ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, 1, 0 },
    { "samplerBumpHeight", ResourceAssignmentType::DESCRIPTOR_TABLE_SAMPLERS, ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, 1, 0 },
    { "samplerSelfIllum", ResourceAssignmentType::DESCRIPTOR_TABLE_SAMPLERS, ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, 1, 0 },
};

ShaderResourceCollection InternalShaders = {
    { "PassThroughVS", { PassThroughVSConstantBuffers, ResourceBindings(), ResourceBindings() } },
    { "Mtl1VS", { Mtl1VSConstantBuffers, ResourceBindings(), ResourceBindings() } },
    { "Mtl1PS", { Mtl1PSConstantBuffers, Mtl1PSTextures, Mtl1PSSamplers } },
};

} // namespace InternalShaderResource
} // namespace D3D12
