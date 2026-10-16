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

#include "D3D12ShaderLibrary.hpp"

#include "D3D12InternalShader.hpp"
#include "D3D12BlitPixelShader.hpp"
#include "D3D12MipmapGenComputeShader.hpp"


namespace D3D12 {
namespace Shaders {

bool ShaderLibrary::Load(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize)
{
    try
    {
        NIPtr<Shader> shader;

        if (mode == ShaderPipelineMode::COMPUTE)
        {
            if (name == "MipmapGenCS")
            {
                shader = std::make_shared<MipmapGenComputeShader>();
            }
            else
            {
                D3D12NI_LOG_ERROR("ShaderLibrary: Unrecognized compute shader attempted loading: %s", name.c_str());
                return false;
            }
        }
        else
        {
            if (name == "BlitPS")
            {
                shader = std::make_shared<BlitPixelShader>();
            }
            else
            {
                shader = std::make_shared<InternalShader>();
            }
        }

        if (!shader->Init(name, mode, visibility, code, codeSize))
        {
            D3D12NI_LOG_ERROR("Failed to initialize Internal Shader %s", name.c_str());
            return false;
        }

        mShaders.emplace(std::make_pair(name, std::move(shader)));
    }
    catch (const std::exception& e)
    {
        (void)e; // silence unused warning when logs are compiled out
        D3D12NI_LOG_ERROR("Exception caught while loading ShaderLibrary shader: %s", e.what());
        return false;
    }

    return true;
}

NIPtr<ShaderLibrary> ShaderLibrary::Duplicate() const
{
    NIPtr<ShaderLibrary> library = std::make_shared<ShaderLibrary>();

    for (ShaderMap::const_iterator it = mShaders.begin(); it != mShaders.end(); ++it)
    {
        const NIPtr<Shader>& shader = it->second;
        if (!library->Load(shader->GetName(), shader->GetMode(), shader->GetVisibility(), shader->GetBytecode().pShaderBytecode, shader->GetBytecode().BytecodeLength))
        {
            D3D12NI_LOG_ERROR("Failed to duplicate Shader object %s from Shader Library", shader->GetName().c_str());
            return nullptr;
        }
    }

    return library;
}

} // namespace Shaders
} // namespace D3D12
