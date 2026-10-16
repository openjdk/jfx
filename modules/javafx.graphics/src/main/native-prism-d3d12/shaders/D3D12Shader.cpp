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

#include "D3D12Shader.hpp"


namespace D3D12 {
namespace Shaders {

void Shader::AddShaderResource(const char* name, const ResourceAssignment& resource)
{
    mShaderResourceAssignments.emplace(name, resource);
}

Shader::Shader()
    : mName()
    , mMode(ShaderPipelineMode::UI_2D)
    , mVisibility(D3D12_SHADER_VISIBILITY_ALL)
    , mBytecode()
    , mBytecodeBuffer()
    , mConstantBufferStorage()
    , mShaderResourceAssignments()
    , mResourceData()
    , mDescriptorData()
    , mConstantsDirty(true)
{
}

bool Shader::Init(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize)
{
    mName = name;
    mMode = mode;
    mVisibility = visibility;
    mBytecodeBuffer.resize(codeSize);

    memcpy(mBytecodeBuffer.data(), code, codeSize);
    mBytecode.BytecodeLength = codeSize;
    mBytecode.pShaderBytecode = mBytecodeBuffer.data();

    return true;
}

bool Shader::SetConstants(const std::string_view& name, const void* data, size_t size)
{
    // parameters were mostly validated at JNI entry point already
    // so we can only check if resource exists in the map
    const auto& resourceIt = mShaderResourceAssignments.find(name);
    if (resourceIt == mShaderResourceAssignments.end())
    {
        D3D12NI_LOG_ERROR("Shader resource named %s not found in shader %s", name.data(), mName.c_str());
        return false;
    }

    const ResourceAssignment& resource = resourceIt->second;
    if (size > resource.sizeInCBStorage)
    {
        D3D12NI_LOG_ERROR("SetConstants failed, data size %d bytes won't fit in expected Shader Constant storage space %d bytes", size, resource.sizeInCBStorage);
        return false;
    }

    uint8_t* dstDataPtr = mConstantBufferStorage.data() + resource.offsetInCBStorage;
    const uint8_t* srcDataPtr = reinterpret_cast<const uint8_t*>(data);
    memcpy(dstDataPtr, srcDataPtr, size);

    mConstantsDirty = true;

    return true;
}

bool Shader::SetConstantsInArray(const std::string_view& name, uint32_t idx, const void* data, size_t size)
{
    // parameters were mostly validated at JNI entry point already
    // so we can only check if resource exists in the map
    const auto& resourceIt = mShaderResourceAssignments.find(name);
    if (resourceIt == mShaderResourceAssignments.end())
    {
        D3D12NI_LOG_ERROR("Shader resource named %s not found in shader %s", name.data(), mName.c_str());
        return false;
    }

    const ResourceAssignment& resource = resourceIt->second;
    if (idx >= resource.elementCount)
    {
        D3D12NI_LOG_ERROR("SetConstantsInArray failed, index %d crosses array boundary %d", idx, resource.elementCount);
        return false;
    }

    if (size > resource.sizeInCBStorage)
    {
        D3D12NI_LOG_ERROR("SetConstantsInArray failed, data size %d bytes won't fit in expected Shader Constant element storage %d byte", size, resource.sizeInCBStorage);
        return false;
    }

    uint8_t* dstDataPtr = mConstantBufferStorage.data() + resource.offsetInCBStorage + (idx * resource.sizeInCBStorage);
    const uint8_t* srcDataPtr = reinterpret_cast<const uint8_t*>(data);
    memcpy(dstDataPtr, srcDataPtr, size);

    mConstantsDirty = true;
    return true;
}

} // namespace Shaders
} // namespace D3D12
