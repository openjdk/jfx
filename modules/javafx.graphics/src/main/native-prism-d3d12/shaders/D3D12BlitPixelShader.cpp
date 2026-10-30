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

#include "D3D12BlitPixelShader.hpp"

#include "D3D12ShaderSlots.hpp"


namespace D3D12 {
namespace Shaders {

BlitPixelShader::BlitPixelShader()
    : Shader()
    , mSourceTextureDTable()
{
}

bool BlitPixelShader::Init(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize)
{
    if (mode != ShaderPipelineMode::UI_2D && visibility != D3D12_SHADER_VISIBILITY_PIXEL)
    {
        D3D12NI_LOG_ERROR("Mode and/or visibility are incompatible for BlitPixelShader");
        return false;
    }

    if (name.find("BlitPS") == std::string::npos)
    {
        D3D12NI_LOG_ERROR("This shader class should only be used to load and operate BlitPS shader");
        return false;
    }

    if (!Shader::Init(name, mode, visibility, code, codeSize))
    {
        D3D12NI_LOG_ERROR("Failed to init base of NativeShader");
        return false;
    }

    mResourceData.textureCount = 1;

    return true;
}

bool BlitPixelShader::PrepareDescriptors(const Internal::TextureBank& textures, void*, size_t)
{
    if (!textures[0])
    {
        D3D12NI_LOG_ERROR("BlitPS: Failed to prepare resources; source texture must be bound to slot 0");
        return false;
    }

    // write textrue descriptor table entry
    // slot 0 is source and destination is current Render Target
    textures[0]->WriteSRVToDescriptor(mDescriptorData.SRVDescriptors.CPU(0));

    return true;
}

void BlitPixelShader::ApplyDescriptors(const D3D12GraphicsCommandListPtr& commandList) const
{
    commandList->SetGraphicsRootDescriptorTable(ShaderSlots::GRAPHICS_RS_PS_TEXTURE_DTABLE, mDescriptorData.SRVDescriptors.GPU(0));
    commandList->SetGraphicsRootDescriptorTable(ShaderSlots::GRAPHICS_RS_PS_SAMPLER_DTABLE, mDescriptorData.SamplerDescriptors.GPU(0));
}

} // namespace Shaders
} // namespace D3D12
