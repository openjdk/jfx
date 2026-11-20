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

#include "D3D12NativeShader.hpp"

#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Config.hpp"
#include "internal/D3D12Utils.hpp"
#include "internal/JNIBuffer.hpp"
#include "internal/JNIString.hpp"

#include "Decora_D3D12ShaderResourceDataHeader.hpp"
#include "Prism_D3D12ShaderResourceDataHeader.hpp"
#include "D3D12ShaderSlots.hpp"

#include <com_sun_prism_d3d12_ni_D3D12NativeShader.h>


namespace {

const char* ResourceTypeToString(D3D12::JSLC::ResourceBindingType type)
{
    switch (type)
    {
    case D3D12::JSLC::ResourceBindingType::CONSTANT_32BIT: return "CONSTANT_32BIT";
    case D3D12::JSLC::ResourceBindingType::CONSTANT_64BIT: return "CONSTANT_64BIT";
    case D3D12::JSLC::ResourceBindingType::CONSTANT_96BIT: return "CONSTANT_96BIT";
    case D3D12::JSLC::ResourceBindingType::CONSTANT_128BIT: return "CONSTANT_128BIT";
    case D3D12::JSLC::ResourceBindingType::TEXTURE: return "TEXTURE";
    case D3D12::JSLC::ResourceBindingType::SAMPLER: return "SAMPLER";
    default: return "UNKNOWN";
    }
}

} // namespace


namespace D3D12 {

uint32_t NativeShader::GetTotalBindingSize(const JSLC::ResourceBinding& binding) const
{
    uint32_t sizePerSlot;
    switch (binding.type)
    {
    case JSLC::ResourceBindingType::CONSTANT_32BIT:
        sizePerSlot = 1;
        break;
    case JSLC::ResourceBindingType::CONSTANT_64BIT:
    case JSLC::ResourceBindingType::TEXTURE:
    case JSLC::ResourceBindingType::SAMPLER:
        sizePerSlot = 2;
        break;
    case JSLC::ResourceBindingType::CONSTANT_96BIT:
        sizePerSlot = 3;
        break;
    case JSLC::ResourceBindingType::CONSTANT_128BIT:
        sizePerSlot = 4;
        break;
    default:
        sizePerSlot = 0;
    }

    return sizePerSlot * binding.count;
}

NativeShader::NativeShader()
    : Shader()
    , mShaderResources()
{
}

NativeShader::~NativeShader()
{
    D3D12NI_LOG_TRACE("--- NativeShader %s destroyed ---", mName.c_str());
}

bool NativeShader::Init(const std::string& name, const void* code, size_t size)
{
    // NativeShader-s are always 2D Pixel Shaders
    // All use common internal PassThroughVS vertex shader and are only used for UI rendering
    if (!Shader::Init(name, ShaderPipelineMode::UI_2D, D3D12_SHADER_VISIBILITY_PIXEL, code, size))
    {
        D3D12NI_LOG_ERROR("Failed to init base of NativeShader");
        return false;
    }

    auto resources = JSLC::DecoraShaders.find(mName.c_str());
    if (resources == JSLC::DecoraShaders.end())
    {
        resources = JSLC::PrismShaders.find(mName.c_str());
        if (resources == JSLC::PrismShaders.end())
        {
            D3D12NI_LOG_ERROR("Couldn't find %s shader resource data", mName.c_str());
            return false;
        }
    }

    mShaderResources = resources->second;
    if (Internal::Config::IsPrintShaderLoadEnabled())
    {
        if (!mShaderResources.empty())
        {
            D3D12NI_LOG_DEBUG("%s resources:", mName.c_str());

            for (auto& r: mShaderResources)
            {
                D3D12NI_LOG_DEBUG("  \\_ %s (%s, %d, %d)", r.name, ResourceTypeToString(r.type), r.slot, r.count);
            }
        }
        else
        {
            D3D12NI_LOG_DEBUG("Shader %s has no resources attached", mName.c_str());
        }
    }

    // size of constant buffer storage used by the shader
    uint32_t constantDataTotalSize = 0;
    mResourceData.textureCount = 0;
    mResourceData.uavCount = 0;
    mResourceData.cbufferDTableCount = 0;

    for (auto& binding: mShaderResources)
    {
        switch (binding.type)
        {
        case JSLC::ResourceBindingType::TEXTURE:
        {
            AddShaderResource(
                binding.name,
                ResourceAssignment(
                    ResourceAssignmentType::DESCRIPTOR_TABLE_TEXTURES, ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, binding.slot, 1, 0, 0
                )
            );
            mResourceData.textureCount++;
            break;
        }
        case JSLC::ResourceBindingType::SAMPLER:
        {
            AddShaderResource(
                binding.name,
                ResourceAssignment(
                    ResourceAssignmentType::DESCRIPTOR_TABLE_SAMPLERS, ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, binding.slot, 1, 0, 0
                )
            );
            break;
        }
        case JSLC::ResourceBindingType::CONSTANT_32BIT:
        case JSLC::ResourceBindingType::CONSTANT_64BIT:
        case JSLC::ResourceBindingType::CONSTANT_96BIT:
        case JSLC::ResourceBindingType::CONSTANT_128BIT:
        {
            uint32_t bindingSizeBytes = GetTotalBindingSize(binding) * sizeof(DWORD);
            uint32_t paddedBindingSize = Internal::Utils::Align<uint32_t>(bindingSizeBytes, 16);

            // emplace the assignment for future reference in SetConstants()
            AddShaderResource(
                binding.name,
                ResourceAssignment (
                    ResourceAssignmentType::DESCRIPTOR, ShaderSlots::GRAPHICS_RS_PS_DATA, 0, 1, paddedBindingSize, constantDataTotalSize
                )
            );

            constantDataTotalSize += paddedBindingSize;
            break;
        }
        default:
            continue;
        }
    }

    if (Internal::Config::IsPrintShaderLoadEnabled())
    {
        D3D12NI_LOG_DEBUG("Shader %s resource assignments:", mName.c_str());
        for (const auto& r: mShaderResourceAssignments)
        {
            const ResourceAssignment& ra = r.second;
            D3D12NI_LOG_DEBUG("  - %s: rsIndex %d:%d type %s @ offset %d size %d", r.first.data(), ra.rootIndex, ra.index, ResourceAssignmentTypeToString(ra.type), ra.offsetInCBStorage, ra.sizeInCBStorage);
        }
    }

    // NativeShader (Phong/Decora) assume we need only one big constant buffer for all data
    // so combine it all into one region
    mResourceData.cbufferDirectSize = constantDataTotalSize;
    mConstantBufferStorage.resize(constantDataTotalSize);

    return true;
}

bool NativeShader::PrepareDescriptors(const Internal::TextureBank& textures, void* data, size_t size)
{
    for (uint32_t i = 0; i < mResourceData.textureCount; ++i)
    {
        if (textures[i])
        {
            textures[i]->WriteSRVToDescriptor(mDescriptorData.SRVDescriptors.CPU(i));
        }
    }

    if (mConstantBufferStorage.size() > 0)
    {
        if (!mDescriptorData.ConstantDataDirectRegion)
        {
            // should not happen
            D3D12NI_LOG_ERROR("Native shader %s: Failed to allocate cbuffer descriptor", mName.c_str());
            return false;
        }

        memcpy(mDescriptorData.ConstantDataDirectRegion.cpu, data, size);
    }

    return true;
}

void NativeShader::ApplyDescriptors(const D3D12GraphicsCommandListPtr& commandList) const
{
    // NativeShaders are always Pixel shaders
    if (mResourceData.textureCount > 0)
    {
        commandList->SetGraphicsRootDescriptorTable(ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, mDescriptorData.SRVDescriptors.gpu);
        commandList->SetGraphicsRootDescriptorTable(ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, mDescriptorData.SamplerDescriptors.gpu);
    }

    if (mDescriptorData.ConstantDataDirectRegion)
    {
        commandList->SetGraphicsRootConstantBufferView(ShaderSlots::GRAPHICS_RS_PS_DATA, mDescriptorData.ConstantDataDirectRegion.gpu);
    }
}

} // namespace D3D12


#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeShader_nReleaseNativeObject
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    D3D12::FreeNIObject<D3D12::NativeShader>(ptr);
}

#ifdef __cplusplus
}
#endif
