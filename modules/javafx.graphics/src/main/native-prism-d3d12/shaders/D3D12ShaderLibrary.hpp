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

#include "../D3D12Common.hpp"

#include "D3D12Shader.hpp"

#include <string>
#include <string_view>
#include <unordered_map>


namespace D3D12 {
namespace Shaders {

/**
 * A library of internal shaders. These are used to store all 3D variants
 * and utility shaders like PassthroughVS.
 *
 * Because this stores only bytecode and has no correlation to any Device,
 * we can initialize all of it inside our D3D12NativeInstance.
 *
 * Bytecodes are deep-copied and stored within the Library to ensure access
 * to them at all times.
 */
class ShaderLibrary
{
private:
    using ShaderMap = std::map<std::string, NIPtr<Shader>, std::less<>>;
    ShaderMap mShaders;

public:
    ShaderLibrary() = default;
    ~ShaderLibrary() = default;

    ShaderLibrary(const ShaderLibrary&) = delete;
    ShaderLibrary(ShaderLibrary&&) = delete;
    ShaderLibrary& operator=(const ShaderLibrary&) = delete;
    ShaderLibrary& operator=(ShaderLibrary&&) = delete;

    bool Load(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize);
    NIPtr<ShaderLibrary> Duplicate() const;

    inline const NIPtr<Shader>& GetShaderData(const std::string_view& name) const
    {
        return mShaders.find(name)->second;
    }
};

} // namespace Shaders
} // namespace D3D12
