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

#include "D3D12RootSignatureManager.hpp"

#include "shaders/D3D12Shader.hpp"

#include <array>
#include <unordered_map>


namespace D3D12 {
namespace RenderThread {

class GraphicsPSOParameters
{
public:
    NIPtr<Shaders::Shader> vertexShader;
    NIPtr<Shaders::Shader> pixelShader;
    CompositeMode compositeMode;
    D3D12_CULL_MODE cullMode = D3D12_CULL_MODE_NONE;
    D3D12_FILL_MODE fillMode = D3D12_FILL_MODE_SOLID;
    bool enableDepthTest;
    UINT msaaSamples;

    bool operator==(const GraphicsPSOParameters& other) const
    {
        return vertexShader == other.vertexShader &&
            pixelShader == other.pixelShader &&
            compositeMode == other.compositeMode &&
            cullMode == other.cullMode &&
            fillMode == other.fillMode &&
            enableDepthTest == other.enableDepthTest &&
            msaaSamples == other.msaaSamples;
    }
};

class ComputePSOParameters
{
public:
    NIPtr<Shaders::Shader> shader;

    bool operator==(const ComputePSOParameters& other) const
    {
        return shader == other.shader;
    }
};

} // namespace RenderThread
} // namespace D3D12


template <>
struct std::hash<D3D12::RenderThread::GraphicsPSOParameters>
{
    std::size_t operator()(const D3D12::RenderThread::GraphicsPSOParameters& k) const
    {
        return std::hash<D3D12::NIPtr<D3D12::Shaders::Shader>>()(k.vertexShader) ^
               std::hash<D3D12::NIPtr<D3D12::Shaders::Shader>>()(k.pixelShader) ^
               std::hash<int>()(static_cast<int>(k.compositeMode)) ^
               std::hash<uint32_t>()(k.cullMode) ^
               std::hash<uint32_t>()(k.fillMode) ^
               std::hash<bool>()(k.enableDepthTest) ^
               std::hash<UINT>()(k.msaaSamples);
    }
};

template <>
struct std::hash<D3D12::RenderThread::ComputePSOParameters>
{
    std::size_t operator()(const D3D12::RenderThread::ComputePSOParameters& k) const
    {
        return std::hash<D3D12::NIPtr<D3D12::Shaders::Shader>>()(k.shader);
    }
};


namespace D3D12 {
namespace RenderThread {

class PSOManager
{
    NIPtr<NativeDevice> mNativeDevice;
    RootSignatureManager mRootSignatureManager;
    std::array<D3D12_INPUT_ELEMENT_DESC, 4> m2DInputLayout;
    std::array<D3D12_INPUT_ELEMENT_DESC, 3> m3DInputLayout;
    std::unordered_map<GraphicsPSOParameters, D3D12PipelineStatePtr> mGraphicsPipelines;
    std::unordered_map<ComputePSOParameters, D3D12PipelineStatePtr> mComputePipelines;
    D3D12PipelineStatePtr mNullPipeline;

    D3D12_BLEND_DESC FormBlendState(CompositeMode mode);
    bool ConstructNewPSO(const GraphicsPSOParameters& params);
    bool ConstructNewPSO(const ComputePSOParameters& params);

public:
    PSOManager(const NIPtr<NativeDevice>& nativeDevice);
    ~PSOManager();

    bool Init();
    const D3D12PipelineStatePtr& GetPSO(const GraphicsPSOParameters& params);
    const D3D12PipelineStatePtr& GetPSO(const ComputePSOParameters& params);

    const RootSignatureManager& GetRootSignatureManager() const
    {
        return mRootSignatureManager;
    }
};

} // namespace RenderThread
} // namespace D3D12
