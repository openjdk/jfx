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

#include "D3D12InternalShader.hpp"

#include "internal/D3D12Utils.hpp"
#include "internal/D3D12Config.hpp"

#include "Internal_D3D12ShaderResourceDataHeader.hpp"


namespace D3D12 {
namespace Shaders {

// TODO: D3D12: this information should probably be pre-compiled somehow?
int32_t InternalShader::GetTextureCountFromVariant(const std::string& variant) const
{
    // we work this out in reverse, pattern is:
    //   'i' at the end == with self illumination, missing 'i' means no self illum
    //   'b' at the beginning == with bump mapping; 's' at the beginning == without bump map (simple)
    //   number in the middle == number of lights; no matter for us here
    //   't' or 'm' at the end == uses specular map; otherwise not
    if (variant[variant.size() - 1] == 'i') return 4; // self illum map takes the last spot so we need all of them
    if (variant[0] == 'b') return 3; // bump map is third out of four, we can skip the last spot
    if (variant[variant.size() - 1] == 't' || variant[variant.size() - 1] == 'm') return 2;
    return 1;
}

InternalShader::InternalShader()
    : Shader()
    , mCBufferDTableRegions()
    , mCBufferDirectRegion()
{
}

bool InternalShader::Init(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize)
{
    int32_t textureCountFromVariant = -1;

    // try an exact name search first
    auto resources = InternalShaderResource::InternalShaders.find(name.c_str());
    if (resources == InternalShaderResource::InternalShaders.end())
    {
        // It is possible we try to load a variant of an internal shader
        // Assume Shader variants are denoted by an _ sign
        // variants SHOULD have the same resource declarations though, so filter out "_" and suffix
        size_t underscore = name.find_last_of('_');
        if (underscore == std::string::npos)
        {
            D3D12NI_LOG_ERROR("Cannot locate resources for internal shader %s", name.c_str());
            return false;
        }

        std::string basename = name.substr(0, underscore);
        resources = InternalShaderResource::InternalShaders.find(basename.c_str());
        if (resources == InternalShaderResource::InternalShaders.end())
        {
            D3D12NI_LOG_ERROR("Cannot locate resources for internal shader %s", name.c_str());
            return false;
        }

        if (basename == "Mtl1PS")
        {
            std::string variant = name.substr(underscore + 1);
            textureCountFromVariant = GetTextureCountFromVariant(variant);
        }
    }

    if (!Shader::Init(name, mode, visibility, code, codeSize))
    {
        D3D12NI_LOG_ERROR("Failed to init base of NativeShader");
        return false;
    }

    InternalShaderResource::ShaderResources shaderResources = resources->second;

    // constant buffer preparation
    uint32_t constantBufferTotalSize = 0;
    for (const InternalShaderResource::ResourceBinding& constantBuffer: shaderResources.constantBuffers)
    {
        if (constantBuffer.type == ResourceAssignmentType::DESCRIPTOR_TABLE_CBUFFERS)
        {
            D3D12NI_ASSERT(mResourceData.cbufferDTableCount == 0, "%s: CBV DTable already declared. We can only fit one CBV DTable per shader.", mName.c_str());
            mResourceData.cbufferDTableSingleSize = constantBuffer.size;
            mResourceData.cbufferDTableCount = constantBuffer.count;

            // adding entire CBuffer table as one big blob - we will split it on SetConstants()
            uint32_t totalSize = constantBuffer.size * constantBuffer.count;
            ResourceAssignment assignment(constantBuffer.type, constantBuffer.rootIndex, 0, constantBuffer.count, constantBuffer.size, constantBufferTotalSize);

            AddShaderResource(constantBuffer.name, assignment);
            mCBufferDTableRegions.emplace_back(assignment);
            constantBufferTotalSize += totalSize;
        }
        else
        {
            D3D12NI_ASSERT(!mCBufferDirectRegion && mResourceData.cbufferDirectSize == 0, "%s: Direct CBV already declared. We can only fit one direct CBV per shader.", mName.c_str());
            mResourceData.cbufferDirectSize = constantBuffer.size;

            ResourceAssignment assignment(constantBuffer.type, constantBuffer.rootIndex, 0, 1, constantBuffer.size, constantBufferTotalSize);
            AddShaderResource(constantBuffer.name, assignment);

            mCBufferDirectRegion = CBufferRegion(assignment);
            constantBufferTotalSize += constantBuffer.size;
        }
    }

    mConstantBufferStorage.resize(constantBufferTotalSize);

    if (textureCountFromVariant >= 0)
    {
        mResourceData.textureCount = textureCountFromVariant;
    }
    else
    {
        // shader variant did not provide us with how many textures we need so
        // count how many DTable slots we need for textures (if any)
        mResourceData.textureCount = 0;
        for (size_t i = 0; i < shaderResources.textures.size(); ++i)
        {
            const InternalShaderResource::ResourceBinding& texture = shaderResources.textures[i];

            AddShaderResource(
                texture.name,
                ResourceAssignment(
                    texture.type, texture.rootIndex, static_cast<uint32_t>(i), 1, 0, 0
                )
            );

            mResourceData.textureCount++;
        }
    }

    if (Internal::Config::IsPrintShaderLoadEnabled())
    {
        // debug info
        D3D12NI_LOG_DEBUG("Internal Shader %s resource assignments (needs %d texture/sampler descriptors + %d cbv descriptors):", mName.c_str(), mResourceData.textureCount, mResourceData.cbufferDTableCount);
        for (const auto& r: mShaderResourceAssignments)
        {
            const ResourceAssignment& ra = r.second;
            D3D12NI_LOG_DEBUG("  - %s: rsIndex %d:%d type %s @ offset %d size %d", r.first.data(), ra.rootIndex, ra.index, ResourceAssignmentTypeToString(ra.type), ra.offsetInCBStorage, ra.sizeInCBStorage);
        }
    }

    return true;
}

bool InternalShader::PrepareDescriptors(const Internal::TextureBank& textures, void* data, size_t size)
{
    // populate the CBuffer regions reserved by ResourceManager
    size_t singleCBVSizeAligned = Internal::Utils::Align<size_t>(mResourceData.cbufferDTableSingleSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT);
    for (size_t i = 0; i < mCBufferDTableRegions.size(); ++i)
    {
        CBufferRegion& cbr = mCBufferDTableRegions[i];

        for (size_t el = 0; el < cbr.assignment.elementCount; ++el)
        {
            cbr.region = mDescriptorData.ConstantDataDTableRegions.Subregion(singleCBVSizeAligned * el, singleCBVSizeAligned);

            const uint8_t* src = reinterpret_cast<const uint8_t*>(data) + cbr.assignment.offsetInCBStorage + (el * mResourceData.cbufferDTableSingleSize);
            memcpy(cbr.region.cpu, src, cbr.assignment.sizeInCBStorage);
        }
    }

    if (mCBufferDirectRegion)
    {
        mCBufferDirectRegion.region = mDescriptorData.ConstantDataDirectRegion;

        const uint8_t* src = reinterpret_cast<const uint8_t*>(data) + mCBufferDirectRegion.assignment.offsetInCBStorage;
        memcpy(mCBufferDirectRegion.region.cpu, src, mCBufferDirectRegion.assignment.sizeInCBStorage);
    }

    // write in textures directly
    if (mResourceData.textureCount > 0)
    {
        for (uint32_t i = 0; i < mResourceData.textureCount; ++i)
        {
            if (textures[i])
            {
                textures[i]->WriteSRVToDescriptor(mDescriptorData.SRVDescriptors.CPU(i));
            }
        }
    }

    return true;
}

void InternalShader::ApplyDescriptors(const D3D12GraphicsCommandListPtr& commandList) const
{
    if (mCBufferDirectRegion)
    {
        commandList->SetGraphicsRootConstantBufferView(mCBufferDirectRegion.assignment.rootIndex, mCBufferDirectRegion.region.gpu);
    }

    if (mDescriptorData.CBufferTableDescriptors && mCBufferDTableRegions.size() > 0)
    {
        commandList->SetGraphicsRootDescriptorTable(mCBufferDTableRegions[0].assignment.rootIndex, mDescriptorData.CBufferTableDescriptors.gpu);
    }

    if (mResourceData.textureCount > 0)
    {
        // textures only apply to Pixel Shaders
        commandList->SetGraphicsRootDescriptorTable(ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, mDescriptorData.SRVDescriptors.GPU(0));
        commandList->SetGraphicsRootDescriptorTable(ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, mDescriptorData.SamplerDescriptors.GPU(0));
    }
}

} // namespace Shaders
} // namespace D3D12
