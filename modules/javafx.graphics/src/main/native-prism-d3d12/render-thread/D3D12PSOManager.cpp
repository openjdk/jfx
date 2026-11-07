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

#include "D3D12PSOManager.hpp"

#include "../D3D12NativeDevice.hpp"
#include "../D3D12Constants.hpp"

#include "D3D12Debug.hpp"
#include "D3D12Utils.hpp"


namespace D3D12 {
namespace RenderThread {

D3D12_BLEND_DESC PSOManager::FormBlendState(CompositeMode mode)
{
    D3D12_BLEND_DESC state;
    state.AlphaToCoverageEnable = false;
    state.IndependentBlendEnable = false;

    D3D12NI_ZERO_STRUCT(state.RenderTarget[0]);
    state.RenderTarget[0].BlendEnable = true;
    state.RenderTarget[0].LogicOpEnable = false;
    // Blend Ops are ADD because D3D9 only supported Add
    // The equation is: (texel * SrcBlend) + (pixel * DestBlend)
    // https://learn.microsoft.com/en-us/windows/win32/direct3d9/alpha-texture-blending
    state.RenderTarget[0].BlendOp = D3D12_BLEND_OP_ADD;
    state.RenderTarget[0].BlendOpAlpha = D3D12_BLEND_OP_ADD;
    state.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;

    switch (mode)
    {
    case CompositeMode::CLEAR:
        state.RenderTarget[0].SrcBlend = D3D12_BLEND_ZERO;
        state.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ZERO;
        state.RenderTarget[0].DestBlend = D3D12_BLEND_ZERO;
        state.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_ZERO;
        break;
    case CompositeMode::SRC:
        state.RenderTarget[0].BlendEnable = false;
        break;
    case CompositeMode::SRC_OVER:
        state.RenderTarget[0].SrcBlend = D3D12_BLEND_ONE;
        state.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
        state.RenderTarget[0].DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
        state.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_INV_SRC_ALPHA;
        break;
    case CompositeMode::DST_OUT:
        state.RenderTarget[0].SrcBlend = D3D12_BLEND_ZERO;
        state.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ZERO;
        state.RenderTarget[0].DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
        state.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_INV_SRC_ALPHA;
        break;
    case CompositeMode::ADD:
        state.RenderTarget[0].SrcBlend = D3D12_BLEND_ONE;
        state.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
        state.RenderTarget[0].DestBlend = D3D12_BLEND_ONE;
        state.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_ONE;
        break;
    }

    return state;
}

bool PSOManager::ConstructNewPSO(const GraphicsPSOParameters& params)
{
    D3D12_GRAPHICS_PIPELINE_STATE_DESC desc;
    D3D12NI_ZERO_STRUCT(desc);

    if (params.vertexShader->GetMode() != params.pixelShader->GetMode())
    {
        // should not happen
        D3D12NI_LOG_ERROR("Tried to combine incompatible vertex and pixel shaders (vertex %s mode %d, pixel %s mode %d)",
            params.vertexShader->GetName().c_str(), static_cast<int>(params.vertexShader->GetMode()),
            params.pixelShader->GetName().c_str(), static_cast<int>(params.pixelShader->GetMode())
        );
        return false;
    }

    // shaders
    desc.VS = params.vertexShader->GetBytecode();
    desc.PS = params.pixelShader->GetBytecode();
    desc.pRootSignature = mRootSignatureManager.GetGraphicsRootSignature().Get();

    desc.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;

    if (params.vertexShader->GetMode() == ShaderPipelineMode::UI_2D)
    {
        desc.InputLayout.pInputElementDescs = m2DInputLayout.data();
        desc.InputLayout.NumElements = static_cast<UINT>(m2DInputLayout.size());
    }
    else if (params.vertexShader->GetMode() == ShaderPipelineMode::PHONG_3D)
    {
        desc.InputLayout.pInputElementDescs = m3DInputLayout.data();
        desc.InputLayout.NumElements = static_cast<UINT>(m3DInputLayout.size());
    }

    desc.NumRenderTargets = 1;
    desc.RTVFormats[0] = DXGI_FORMAT_B8G8R8A8_UNORM;
    desc.SampleDesc.Count = params.msaaSamples;
    desc.SampleDesc.Quality = 0;

    desc.SampleMask = 0xFFFFFFFF;

    desc.BlendState = FormBlendState(params.compositeMode);

    desc.RasterizerState.CullMode = params.cullMode;
    desc.RasterizerState.FillMode = params.fillMode;
    desc.RasterizerState.FrontCounterClockwise = true;
    desc.RasterizerState.DepthClipEnable = true;

    desc.DepthStencilState.StencilEnable = false;

    if (params.enableDepthTest)
    {
        desc.DSVFormat = DXGI_FORMAT_D32_FLOAT;
        desc.DepthStencilState.DepthEnable = true;
        desc.DepthStencilState.DepthWriteMask = D3D12_DEPTH_WRITE_MASK_ALL;
        desc.DepthStencilState.DepthFunc = D3D12_COMPARISON_FUNC_LESS_EQUAL;
    }
    else
    {
        desc.DSVFormat = DXGI_FORMAT_UNKNOWN;
        desc.DepthStencilState.DepthEnable = false;
        desc.DepthStencilState.DepthWriteMask = D3D12_DEPTH_WRITE_MASK_ZERO;
    }

    D3D12PipelineStatePtr pipelineState;
    HRESULT hr = mNativeDevice->GetDevice()->CreateGraphicsPipelineState(&desc, IID_PPV_ARGS(&pipelineState));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Graphics Pipeline State");

#if DEBUG
    std::wstring name = L"GPSO-";
    name += Internal::Utils::ToWString(params.vertexShader->GetName());
    name += L"-";
    name += Internal::Utils::ToWString(params.pixelShader->GetName());
    name += L"-";
    name += CompositeModeToWString(params.compositeMode);
    name += L"-";
    name += std::to_wstring(params.msaaSamples) + L"xMSAA";

    if (params.enableDepthTest)
    {
        name += L"-Depth";
    }

    pipelineState->SetName(name.c_str());

    D3D12NI_LOG_TRACE("--- Graphics PSO (%S) created ---", name.c_str());
#endif // DEBUG

    try
    {
        mGraphicsPipelines.emplace(std::make_pair(params, std::move(pipelineState)));
    }
    catch (const std::exception& e)
    {
        (void)e; // prevents "unused" warning when running a Release build
        D3D12NI_LOG_ERROR("Failed to emplace new Graphics PSO to cache: %s", e.what());
        return false;
    }

    return true;
}

bool PSOManager::ConstructNewPSO(const ComputePSOParameters& params)
{
    D3D12_COMPUTE_PIPELINE_STATE_DESC desc;
    D3D12NI_ZERO_STRUCT(desc);

    desc.CS = params.shader->GetBytecode();
    desc.pRootSignature = mRootSignatureManager.GetComputeRootSignature().Get();

    D3D12PipelineStatePtr pipelineState;
    HRESULT hr = mNativeDevice->GetDevice()->CreateComputePipelineState(&desc, IID_PPV_ARGS(&pipelineState));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Compute Pipeline State");

#if DEBUG
    std::wstring name = L"CPSO-";
    name += Internal::Utils::ToWString(params.shader->GetName());

    pipelineState->SetName(name.c_str());

    D3D12NI_LOG_TRACE("--- Compute PSO (%S) created ---", name.c_str());
#endif // DEBUG

    try
    {
        mComputePipelines.emplace(std::make_pair(params, std::move(pipelineState)));
    }
    catch (const std::exception& e)
    {
        (void)e; // prevents "unused" warning when running a Release build
        D3D12NI_LOG_ERROR("Failed to emplace new Compute PSO to cache: %s", e.what());
        return false;
    }

    return true;
}

PSOManager::PSOManager(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mRootSignatureManager(nativeDevice)
    , m2DInputLayout()
    , m3DInputLayout()
    , mGraphicsPipelines()
    , mComputePipelines()
    , mNullPipeline(nullptr)
{
    m2DInputLayout[0].SemanticName = "POSITION";
    m2DInputLayout[0].SemanticIndex = 0;
    m2DInputLayout[0].Format = DXGI_FORMAT_R32G32B32_FLOAT;
    m2DInputLayout[0].InputSlot = 0;
    m2DInputLayout[0].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m2DInputLayout[0].AlignedByteOffset = 0;

    m2DInputLayout[1].SemanticName = "COLOR";
    m2DInputLayout[1].SemanticIndex = 0;
    m2DInputLayout[1].Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    m2DInputLayout[1].InputSlot = 0;
    m2DInputLayout[1].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m2DInputLayout[1].AlignedByteOffset = D3D12_APPEND_ALIGNED_ELEMENT;

    m2DInputLayout[2].SemanticName = "TEXCOORD";
    m2DInputLayout[2].SemanticIndex = 0;
    m2DInputLayout[2].Format = DXGI_FORMAT_R32G32_FLOAT;
    m2DInputLayout[2].InputSlot = 0;
    m2DInputLayout[2].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m2DInputLayout[2].AlignedByteOffset = D3D12_APPEND_ALIGNED_ELEMENT;

    m2DInputLayout[3].SemanticName = "TEXCOORD";
    m2DInputLayout[3].SemanticIndex = 1;
    m2DInputLayout[3].Format = DXGI_FORMAT_R32G32_FLOAT;
    m2DInputLayout[3].InputSlot = 0;
    m2DInputLayout[3].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m2DInputLayout[3].AlignedByteOffset = D3D12_APPEND_ALIGNED_ELEMENT;


    m3DInputLayout[0].SemanticName = "POSITION";
    m3DInputLayout[0].SemanticIndex = 0;
    m3DInputLayout[0].Format = DXGI_FORMAT_R32G32B32_FLOAT;
    m3DInputLayout[0].InputSlot = 0;
    m3DInputLayout[0].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m3DInputLayout[0].AlignedByteOffset = 0;

    m3DInputLayout[1].SemanticName = "TEXCOORD";
    m3DInputLayout[1].SemanticIndex = 0;
    m3DInputLayout[1].Format = DXGI_FORMAT_R32G32_FLOAT;
    m3DInputLayout[1].InputSlot = 0;
    m3DInputLayout[1].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m3DInputLayout[1].AlignedByteOffset = D3D12_APPEND_ALIGNED_ELEMENT;

    m3DInputLayout[2].SemanticName = "TEXCOORD";
    m3DInputLayout[2].SemanticIndex = 1;
    m3DInputLayout[2].Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
    m3DInputLayout[2].InputSlot = 0;
    m3DInputLayout[2].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
    m3DInputLayout[2].AlignedByteOffset = D3D12_APPEND_ALIGNED_ELEMENT;
}

PSOManager::~PSOManager()
{
    for (auto& it: mComputePipelines)
    {
        it.second.Reset();
    }
    mComputePipelines.clear();

    for (auto& it: mGraphicsPipelines)
    {
        it.second.Reset();
    }
    mGraphicsPipelines.clear();

    mNativeDevice.reset();

    D3D12NI_LOG_DEBUG("PSOManager destroyed");
}

bool PSOManager::Init()
{
    if (!mRootSignatureManager.Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize Root Signature Manager");
        return false;
    }

    return true;
}

const D3D12PipelineStatePtr& PSOManager::GetPSO(const GraphicsPSOParameters& params)
{
    const auto psoIter = mGraphicsPipelines.find(params);
    if (psoIter == mGraphicsPipelines.end())
    {
        if (!ConstructNewPSO(params))
        {
            D3D12NI_LOG_ERROR("Failed to construct new Graphics PSO");
            return mNullPipeline;
        }

        return mGraphicsPipelines[params];
    }
    else
    {
        return psoIter->second;
    }
}

const D3D12PipelineStatePtr& PSOManager::GetPSO(const ComputePSOParameters& params)
{
    const auto psoIter = mComputePipelines.find(params);
    if (psoIter == mComputePipelines.end())
    {
        if (!ConstructNewPSO(params))
        {
            D3D12NI_LOG_ERROR("Failed to construct new Compute PSO");
            return mNullPipeline;
        }

        return mComputePipelines[params];
    }
    else
    {
        return psoIter->second;
    }
}

} // namespace RenderThread
} // namespace D3D12

