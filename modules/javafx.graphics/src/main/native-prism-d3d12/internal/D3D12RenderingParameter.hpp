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

#include "../D3D12Constants.hpp"
#include "../D3D12NativeShader.hpp"

#include "D3D12RenderingStep.hpp"
#include "D3D12Config.hpp"
#include "D3D12IRenderTarget.hpp"
#include "D3D12LinearAllocator.hpp"

#include "render-thread/D3D12RingDescriptorHeap.hpp"
#include "render-thread/D3D12RenderThreadExecutable.hpp"
#include "render-thread/D3D12RenderPayload.hpp"


namespace D3D12 {
namespace Internal {

// generic rendering parameter initialized directly from the parameter
template <typename T, typename Executable>
class RenderingParameter: public RenderingDataStep<T>
{
    RenderThread::RenderThreadExecutablePtr CreateExecutable(LinearAllocator& allocator) const
    {
        return RenderThread::CreateRTExec<Executable>(allocator, this->mParameter);
    }

public:
    RenderThread::RenderPayload::StepAddResult AddToPayload(LinearAllocator& allocator, const RenderThread::RenderPayloadPtr& payload)
    {
        if (this->CanBeSkipped()) return RenderThread::RenderPayload::StepAddResult::SUCCESS;

        RenderThread::RenderPayload::StepAddResult result = payload->AddStep(CreateExecutable(allocator));
        if (result != RenderThread::RenderPayload::StepAddResult::FAILED)
        {
            this->mIsApplied = true;
        }

        return result;
    }
};

// special case of data step for shader constants
template <typename Executable>
class ShaderConstantsResource: public RenderingDataStep<NIPtr<Shaders::Shader>>
{
    RenderThread::RenderThreadExecutablePtr CreateExecutable(LinearAllocator& allocator) const
    {
        RenderThread::ResourceManager::ShaderConstants constants(allocator, mParameter->GetConstantStorage().data(), mParameter->GetConstantStorage().size());
        if (!constants.buffer) return nullptr;

        return RenderThread::CreateRTExec<Executable>(allocator, std::move(constants));
    }

public:
    RenderThread::RenderPayload::StepAddResult AddToPayload(LinearAllocator& allocator, const RenderThread::RenderPayloadPtr& payload)
    {
        if (!mParameter) return RenderThread::RenderPayload::StepAddResult::SUCCESS; // no constants to set, shader is NULL
        if (RenderingDataStep<NIPtr<Shaders::Shader>>::CanBeSkipped() && !mParameter->AreConstantsDirty()) return RenderThread::RenderPayload::StepAddResult::SUCCESS;

        RenderThread::RenderPayload::StepAddResult result = payload->AddStep(CreateExecutable(allocator));
        if (result != RenderThread::RenderPayload::StepAddResult::FAILED)
        {
            mParameter->SetConstantsDirty(false);
            this->mIsApplied = true;
        }

        return result;
    }
};


// Graphics parameters //

class PipelineStateRenderingParameter: public RenderingParameter<RenderThread::GraphicsPSOParameters, RenderThread::ApplyPipelineState>
{
public:
    void SetVertexShader(const NIPtr<Shaders::Shader>& vertexShader)
    {
        mParameter.vertexShader = vertexShader;
        FlagSet();
    }

    void SetPixelShader(const NIPtr<Shaders::Shader>& pixelShader)
    {
        mParameter.pixelShader = pixelShader;
        FlagSet();
    }

    void SetCompositeMode(CompositeMode mode)
    {
        mParameter.compositeMode = mode;
        FlagSet();
    }

    void SetCullMode(D3D12_CULL_MODE mode)
    {
        mParameter.cullMode = mode;
        FlagSet();
    }

    void SetFillMode(D3D12_FILL_MODE mode)
    {
        mParameter.fillMode = mode;
        FlagSet();
    }

    // below Sets do the set-redundancy-check because they're used in multiple places
    // when setting a new RenderTarget in RenderingContext; this is to sometimes prevent
    // PipelineState change when RTT changes but its parameters (depth/MSAA) remain the
    // same as the old RTT.
    void SetDepthTest(bool enabled)
    {
        if (mParameter.enableDepthTest == enabled) return;

        mParameter.enableDepthTest = enabled;
        FlagSet();
    }

    void SetMSAASamples(UINT msaaSamples)
    {
        if (mParameter.msaaSamples == msaaSamples) return;

        mParameter.msaaSamples = msaaSamples;
        FlagSet();
    }
};

class PrimitiveTopologyRenderingParameter: public RenderingParameter<D3D12_PRIMITIVE_TOPOLOGY, RenderThread::ApplyPrimitiveTopology> {};
class RenderTargetRenderingParameter: public RenderingParameter<NIPtr<IRenderTarget>, RenderThread::ApplyRenderTarget> {};
class ScissorRenderingParameter: public RenderingParameter<D3D12_RECT, RenderThread::ApplyScissor> {};
class ViewportRenderingParameter: public RenderingParameter<D3D12_VIEWPORT, RenderThread::ApplyViewport> {};

class TexturesRenderingParameter: public RenderingParameter<TextureBank, RenderThread::SetTexturesAction>
{
public:
    void SetTexture(uint32_t unit, const NIPtr<TextureBase>& texture)
    {
        mParameter[unit] = texture;
        FlagSet();
    }

    const NIPtr<TextureBase>& GetTexture(uint32_t unit) const
    {
        return mParameter[unit];
    }
};

class VertexShaderRenderingParameter: public RenderingParameter<NIPtr<Shaders::Shader>, RenderThread::SetVertexShaderAction> {};
class PixelShaderRenderingParameter: public RenderingParameter<NIPtr<Shaders::Shader>, RenderThread::SetPixelShaderAction> {};
class VertexShaderConstantsRenderingParameter: public ShaderConstantsResource<RenderThread::SetVertexShaderConstantsAction> {};
class PixelShaderConstantsRenderingParameter: public ShaderConstantsResource<RenderThread::SetPixelShaderConstantsAction> {};

// Compute parameters //

class ComputePipelineStateRenderingParameter: public RenderingParameter<RenderThread::ComputePSOParameters, RenderThread::ApplyComputePipelineState>
{
public:
    void SetComputeShader(const NIPtr<Shaders::Shader>& shader)
    {
        mParameter.shader = shader;
        FlagSet();
    }
};

class ComputeShaderRenderingParameter: public RenderingParameter<NIPtr<Shaders::Shader>, RenderThread::SetComputeShaderAction> {};
class ComputeShaderConstantsRenderingParameter: public ShaderConstantsResource<RenderThread::SetComputeShaderConstantsAction> {};

} // namespace Internal
} // namespace D3D12
