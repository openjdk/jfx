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

#include "D3D12MipmapGenComputeShader.hpp"

#include "D3D12ShaderSlots.hpp"


namespace D3D12 {
namespace Shaders {

MipmapGenComputeShader::MipmapGenComputeShader()
    : Shader()
{
}

bool MipmapGenComputeShader::Init(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize)
{
    if (mode != ShaderPipelineMode::COMPUTE && visibility != D3D12_SHADER_VISIBILITY_ALL)
    {
        D3D12NI_LOG_ERROR("Mode and/or visibility are incompatible");
        return false;
    }

    if (name != "MipmapGenCS")
    {
        D3D12NI_LOG_ERROR("This shader class should only be used to load and operate MipmapGenCS shader");
        return false;
    }

    if (!Shader::Init(name, mode, visibility, code, codeSize))
    {
        D3D12NI_LOG_ERROR("Failed to init base of NativeShader");
        return false;
    }

    mConstantBufferStorage.resize(sizeof(CBuffer));

    AddShaderResource("gData", ResourceAssignment(ResourceAssignmentType::DESCRIPTOR, 0, 0, 1, sizeof(CBuffer), 0));

    mResourceData.textureCount = 1;
    mResourceData.uavCount = 4;
    mResourceData.cbufferDirectSize = sizeof(CBuffer);

    return true;
}

bool MipmapGenComputeShader::PrepareDescriptors(const Internal::TextureBank& textures, void* data, size_t size)
{
    if (size != sizeof(CBuffer))
    {
        D3D12NI_LOG_ERROR("MipmapGenCS: Invalid Constant Buffer data");
        return false;
    }

    if (!textures[0])
    {
        D3D12NI_LOG_ERROR("MipmapGenCS: Failed to prepare resources; a texture must be bound to slot 0");
        return false;
    }

    const CBuffer* cb = reinterpret_cast<const CBuffer*>(data);
    memcpy(mDescriptorData.ConstantDataDirectRegion.cpu, cb, sizeof(CBuffer));

    // write source mip level as SRV (our input)
    textures[0]->WriteSRVToDescriptor(mDescriptorData.SRVDescriptors.CPU(0), 1, cb->sourceLevel);

    // write destination mip levels as UAVs (output)
    // destination levels are one higher than source
    for (uint32_t i = 0; i < cb->numLevels; ++i)
    {
        textures[0]->WriteUAVToDescriptor(mDescriptorData.UAVDescriptors.CPU(i), (cb->sourceLevel + 1) + i);
    }

    return true;
}

void MipmapGenComputeShader::ApplyDescriptors(const D3D12GraphicsCommandListPtr& commandList) const
{
    commandList->SetComputeRootConstantBufferView(ShaderSlots::COMPUTE_RS_CONSTANT_DATA, mDescriptorData.ConstantDataDirectRegion.gpu);
    commandList->SetComputeRootDescriptorTable(ShaderSlots::COMPUTE_RS_UAV_DTABLE, mDescriptorData.UAVDescriptors.GPU(0));
    commandList->SetComputeRootDescriptorTable(ShaderSlots::COMPUTE_RS_TEXTURE_DTABLE, mDescriptorData.SRVDescriptors.GPU(0));
}

} // namespace Shaders
} // namespace D3D12
