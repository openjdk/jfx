/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "../D3D12NativeMeshView.hpp"
#include "../D3D12NativeSwapChain.hpp"

#include "internal/D3D12LinearAllocator.hpp"
#include "internal/D3D12IRenderTarget.hpp"
#include "internal/D3D12ITrackedResource.hpp"
#include "internal/D3D12Debug.hpp"
#include "internal/D3D12SamplerDesc.hpp"
#include "internal/MemoryView.hpp"

#include "D3D12RenderThreadContext.hpp"

#include <memory>
#include <algorithm>
#include <list>


namespace D3D12 {
namespace RenderThread {

struct ClearRenderTargetArgs
{
    NIPtr<Internal::IRenderTarget> rt;
    Pixel_RGBA32_FLOAT rgba;
    D3D12_RECT clearRect;
    bool hasRect;

    ClearRenderTargetArgs(const NIPtr<Internal::IRenderTarget>& rt, float r, float g, float b, float a)
        : rt(rt), rgba(), clearRect(), hasRect(false)
    {
        rgba.r = r;
        rgba.g = g;
        rgba.b = b;
        rgba.a = a;
    }

    ClearRenderTargetArgs(const NIPtr<Internal::IRenderTarget>& rt, float r, float g, float b, float a, const D3D12_RECT& clearRect)
        : rt(rt), rgba(), clearRect(clearRect), hasRect(true)
    {
        rgba.r = r;
        rgba.g = g;
        rgba.b = b;
        rgba.a = a;
    }
};

struct ClearDepthStencilArgs
{
    NIPtr<Internal::ITrackedResource> dt;
    D3D12_CPU_DESCRIPTOR_HANDLE dsv;
    float depth;
    D3D12_RECT clearRect;
    bool hasRect;

    ClearDepthStencilArgs(const NIPtr<Internal::ITrackedResource>& dt, const D3D12_CPU_DESCRIPTOR_HANDLE& dsv, float depth)
        : dt(dt), dsv(dsv), depth(depth), clearRect(), hasRect(false)
    {}

    ClearDepthStencilArgs(const NIPtr<Internal::ITrackedResource>& dt, const D3D12_CPU_DESCRIPTOR_HANDLE& dsv, float depth, const D3D12_RECT& clearRect)
        : dt(dt), dsv(dsv), depth(depth), clearRect(clearRect), hasRect(true)
    {}
};

struct CopyTextureArgs
{
    NIPtr<Internal::ITrackedResource> dstResource;
    NIPtr<Internal::ITrackedResource> srcResource;
    D3D12_TEXTURE_COPY_LOCATION dstLoc;
    uint32_t dstx;
    uint32_t dsty;
    D3D12_TEXTURE_COPY_LOCATION srcLoc;
    D3D12_BOX srcBox;

    CopyTextureArgs(const NIPtr<Internal::ITrackedResource>& dstResource, const D3D12_TEXTURE_COPY_LOCATION& dstLoc, uint32_t dstx, uint32_t dsty, const NIPtr<Internal::ITrackedResource>& srcResource, const D3D12_TEXTURE_COPY_LOCATION& srcLoc, const D3D12_BOX& srcBox)
        : dstResource(dstResource), srcResource(srcResource), dstLoc(dstLoc), dstx(dstx), dsty(dsty), srcLoc(srcLoc), srcBox(srcBox)
    {}

    CopyTextureArgs(const NIPtr<Internal::ITrackedResource>& dstResource, const D3D12_TEXTURE_COPY_LOCATION& dstLoc, uint32_t dstx, uint32_t dsty, const NIPtr<Internal::ITrackedResource>& srcResource, const D3D12_TEXTURE_COPY_LOCATION& srcLoc)
        : dstResource(dstResource), dstLoc(dstLoc), dstx(dstx), dsty(dsty), srcResource(srcResource), srcLoc(srcLoc), srcBox()
    {}
};

struct CopyToTextureSmallArgs
{
private:
    Internal::LinearAllocator& allocator;
    size_t srcDataSize;
    void* srcData;

public:
    NIPtr<Internal::ITrackedResource> dstResource;
    uint32_t dstx;
    uint32_t dsty;
    D3D12_TEXTURE_COPY_LOCATION srcLoc;

    CopyToTextureSmallArgs(Internal::LinearAllocator& allocator, size_t srcDataSize, const NIPtr<Internal::ITrackedResource>& dstResource, uint32_t dstx, uint32_t dsty)
        : allocator(allocator)
        , srcDataSize(srcDataSize)
        , srcData(this->allocator.Allocate(static_cast<uint32_t>(srcDataSize)))
        , dstResource(dstResource)
        , dstx(dstx)
        , dsty(dsty)
        , srcLoc() // remains uninitialized, after upload we will provide it
    {}

    CopyToTextureSmallArgs(CopyToTextureSmallArgs&& other)
        : allocator(other.allocator)
        , srcDataSize(other.srcDataSize)
        , srcData(other.srcData)
        , dstResource(std::move(other.dstResource))
        , dstx(other.dstx)
        , dsty(other.dsty)
        , srcLoc(other.srcLoc)
    {
        other.srcData = nullptr;
    }

    ~CopyToTextureSmallArgs()
    {
        if (srcData)
        {
            allocator.Free(srcData);
            srcData = nullptr;
        }
    }

    inline void* GetSourceDataBuffer() const
    {
        return srcData;
    }

    inline size_t GetSourceDataSize() const
    {
        return srcDataSize;
    }
};

struct CopyBufferRegionArgs
{
    D3D12ResourcePtr dst;
    uint64_t dstOffset;
    D3D12ResourcePtr src;
    uint64_t srcOffset;
    uint64_t size;

    CopyBufferRegionArgs(const D3D12ResourcePtr& dst, uint64_t dstOffset, const D3D12ResourcePtr& src, uint64_t srcOffset, uint64_t size)
        : dst(dst), dstOffset(dstOffset), src(src), srcOffset(srcOffset), size(size)
    {}
};

struct CopyResourceArgs
{
    D3D12ResourcePtr dst;
    D3D12ResourcePtr src;

    CopyResourceArgs(const D3D12ResourcePtr& dst, const D3D12ResourcePtr& src)
        : dst(dst), src(src)
    {}
};

struct DrawArgs
{
    uint32_t elements;
    uint32_t vbOffset;

    DrawArgs(uint32_t elements, uint32_t vbOffset)
        : elements(elements), vbOffset(vbOffset)
    {}
};

struct DispatchArgs
{
    uint32_t x;
    uint32_t y;
    uint32_t z;

    DispatchArgs(uint32_t x, uint32_t y, uint32_t z)
        : x(x), y(y), z(z)
    {}
};

struct PrepareSwapChainArgs
{
    NIPtr<NativeSwapChain> swapChain;
    D3D12_RECT dirtyRegion;

    PrepareSwapChainArgs(const NIPtr<NativeSwapChain>& swapChain, const D3D12_RECT& dirtyRegion)
        : swapChain(swapChain)
        , dirtyRegion(dirtyRegion)
    {}
};

struct ResolveArgs
{
    NIPtr<Internal::ITrackedResource> dst;
    NIPtr<Internal::ITrackedResource> src;
    DXGI_FORMAT format;

    ResolveArgs(const NIPtr<Internal::ITrackedResource>& dst, const NIPtr<Internal::ITrackedResource>& src, DXGI_FORMAT format)
        : dst(dst), src(src), format(format)
    {}
};

struct ResolveRegionArgs
{
    NIPtr<Internal::ITrackedResource> dst;
    uint32_t dstx;
    uint32_t dsty;
    NIPtr<Internal::ITrackedResource> src;
    D3D12_RECT srcRect;
    DXGI_FORMAT format;

    ResolveRegionArgs(const NIPtr<Internal::ITrackedResource>& dst, uint32_t dstx, uint32_t dsty, const NIPtr<Internal::ITrackedResource>& src, const D3D12_RECT& srcRect, DXGI_FORMAT format)
        : dst(dst), dstx(dstx), dsty(dsty), src(src), srcRect(srcRect), format(format)
    {}
};

struct GraphicsShaders
{
    NIPtr<Shaders::Shader> vertexShader;
    NIPtr<Shaders::Shader> pixelShader;
};

struct DescriptorHeaps
{
    D3D12DescriptorHeapPtr heap;
    D3D12DescriptorHeapPtr samplerHeap;
};

struct ResourceTransitionArgs
{
    NIPtr<Internal::ITrackedResource> resource;
    D3D12_RESOURCE_STATES newState;
    uint32_t subresource;
};


// Descriptor bindings-related bits are in D3D12Common.hpp to make them visible to Shaders


class RenderThreadExecutable
{
public:
    virtual void Execute(const RenderThreadContextPtr& context) = 0;
    virtual ~RenderThreadExecutable() {}
};

template <typename T>
class RenderThreadDataExecutable: public RenderThreadExecutable
{
protected:
    T mData;

public:
    RenderThreadDataExecutable()
        : mData()
    {}

    RenderThreadDataExecutable(const T& data)
        : mData(data)
    {}

    RenderThreadDataExecutable(T&& data)
        : mData(std::move(data))
    {}

    template <typename ...Args>
    RenderThreadDataExecutable(Args&&... args)
        : mData(std::forward<Args>(args)...)
    {
    }
};

using RenderThreadExecutableDeleter = Internal::LinearAllocatorDeleter<RenderThreadExecutable>;
using RenderThreadExecutablePtr = std::unique_ptr<RenderThreadExecutable, RenderThreadExecutableDeleter>;

template <typename Executable, typename ...Args>
RenderThreadExecutablePtr CreateRTExec(Internal::LinearAllocator& allocator, Args&&... args)
{
    return RenderThreadExecutablePtr(allocator.Construct<Executable>(std::forward<Args>(args)...), RenderThreadExecutableDeleter(&allocator));
}


// Graphics render thread actions //

class ApplyIndexBuffer: public RenderThreadDataExecutable<D3D12_INDEX_BUFFER_VIEW>
{
public:
    ApplyIndexBuffer(const D3D12_INDEX_BUFFER_VIEW& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->indexBuffer.Set(mData);
    }
};

class ApplyPipelineState: public RenderThreadDataExecutable<GraphicsPSOParameters>
{
public:
    ApplyPipelineState(const GraphicsPSOParameters& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->pipelineState.Set(context->PSOManager.GetPSO(mData));
    }
};

class ApplyPrimitiveTopology: public RenderThreadDataExecutable<D3D12_PRIMITIVE_TOPOLOGY>
{
public:
    ApplyPrimitiveTopology(const D3D12_PRIMITIVE_TOPOLOGY& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->primitiveTopology.Set(mData);
    }
};

class ApplyRenderTarget: public RenderThreadDataExecutable<RenderTargetData>
{
public:
    ApplyRenderTarget(const NIPtr<Internal::IRenderTarget>& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        mData.renderTarget ? context->renderTarget.Set(mData) : context->renderTarget.Unset();
    }
};

class ApplyScissor: public RenderThreadDataExecutable<D3D12_RECT>
{
public:
    ApplyScissor(const D3D12_RECT& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->scissor.Set(mData);
    }
};

class ApplyVertexBuffer: public RenderThreadDataExecutable<D3D12_VERTEX_BUFFER_VIEW>
{
public:
    ApplyVertexBuffer(const D3D12_VERTEX_BUFFER_VIEW& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->vertexBuffer.Set(mData);
    }
};

class ApplyViewport: public RenderThreadDataExecutable<D3D12_VIEWPORT>
{
public:
    ApplyViewport(const D3D12_VIEWPORT& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->viewport.Set(mData);
    }
};


// Graphics Pipeline Actions //

class ClearRenderTargetAction: public RenderThreadDataExecutable<ClearRenderTargetArgs>
{
public:
    ClearRenderTargetAction(const NIPtr<Internal::IRenderTarget>& rt, float r, float g, float b, float a)
        : RenderThreadDataExecutable(ClearRenderTargetArgs(rt, r, g, b, a))
    {}

    ClearRenderTargetAction(const NIPtr<Internal::IRenderTarget>& rt, float r, float g, float b, float a, const D3D12_RECT& clearRect)
        : RenderThreadDataExecutable(ClearRenderTargetArgs(rt, r, g, b, a, clearRect))
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->QueueResourceTransition(mData.rt->GetTexture(), D3D12_RESOURCE_STATE_RENDER_TARGET);
        context->SubmitResourceTransitions();

        if (mData.hasRect)
        {
            context->CommandList()->ClearRenderTargetView(
                mData.rt->GetRTVDescriptorData().CPU(0),
                reinterpret_cast<const float*>(&mData.rgba),
                1, &mData.clearRect
            );
        }
        else
        {
            context->CommandList()->ClearRenderTargetView(
                mData.rt->GetRTVDescriptorData().CPU(0),
                reinterpret_cast<const float*>(&mData.rgba),
                0, nullptr
            );
        }
    }
};

class ClearDepthStencilAction: public RenderThreadDataExecutable<ClearDepthStencilArgs>
{
public:
    ClearDepthStencilAction(const NIPtr<Internal::ITrackedResource>& dt, const D3D12_CPU_DESCRIPTOR_HANDLE& dsv, float depth)
        : RenderThreadDataExecutable(ClearDepthStencilArgs(dt, dsv, depth))
    {}

    ClearDepthStencilAction(const NIPtr<Internal::ITrackedResource>& dt, const D3D12_CPU_DESCRIPTOR_HANDLE& dsv, float depth, const D3D12_RECT& clearRect)
        : RenderThreadDataExecutable(ClearDepthStencilArgs(dt, dsv, depth, clearRect))
    {}


    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->QueueResourceTransition(mData.dt, D3D12_RESOURCE_STATE_DEPTH_WRITE);
        context->SubmitResourceTransitions();

        if (mData.hasRect)
        {
            context->CommandList()->ClearDepthStencilView(mData.dsv, D3D12_CLEAR_FLAG_DEPTH, mData.depth, 0, 1, &mData.clearRect);
        }
        else
        {
            context->CommandList()->ClearDepthStencilView(mData.dsv, D3D12_CLEAR_FLAG_DEPTH, mData.depth, 0, 0, nullptr);
        }
    }
};

class CopyTextureAction: public RenderThreadDataExecutable<CopyTextureArgs>
{
    bool hasBox;

public:
    CopyTextureAction(const NIPtr<Internal::ITrackedResource>& dstResource, D3D12_TEXTURE_COPY_LOCATION dstLoc, uint32_t dstx, uint32_t dsty, const NIPtr<Internal::ITrackedResource>& srcResource, D3D12_TEXTURE_COPY_LOCATION srcLoc, D3D12_BOX srcBox)
        : RenderThreadDataExecutable(dstResource, dstLoc, dstx, dsty, srcResource, srcLoc, srcBox)
        , hasBox(true)
    {}

    CopyTextureAction(const NIPtr<Internal::ITrackedResource>& dstResource, D3D12_TEXTURE_COPY_LOCATION dstLoc, uint32_t dstx, uint32_t dsty, const NIPtr<Internal::ITrackedResource>& srcResource, D3D12_TEXTURE_COPY_LOCATION srcLoc)
        : RenderThreadDataExecutable(dstResource, dstLoc, dstx, dsty, srcResource, srcLoc)
        , hasBox(false)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        // resolve D3D12 resources if locations have them as null, this can matter for ex. SwapChain
        if (mData.dstLoc.pResource == nullptr)
        {
            mData.dstLoc.pResource = mData.dstResource->GetD3D12Resource().Get();
            context->QueueResourceTransition(mData.dstResource, D3D12_RESOURCE_STATE_COPY_DEST);
        }

        if (mData.srcLoc.pResource == nullptr)
        {
            mData.srcLoc.pResource = mData.srcResource->GetD3D12Resource().Get();
            context->QueueResourceTransition(mData.srcResource, D3D12_RESOURCE_STATE_COPY_SOURCE);
        }

        context->SubmitResourceTransitions();
        context->CommandList()->CopyTextureRegion(&mData.dstLoc, mData.dstx, mData.dsty, 0, &mData.srcLoc, hasBox ? &mData.srcBox : nullptr);
    }
};

class CopyBufferRegionAction: public RenderThreadDataExecutable<CopyBufferRegionArgs>
{
public:
    CopyBufferRegionAction(const D3D12ResourcePtr& dst, uint64_t dstOffset, const D3D12ResourcePtr& src, uint64_t srcOffset, uint64_t size)
        : RenderThreadDataExecutable(dst, dstOffset, src, srcOffset, size)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->CommandList()->CopyBufferRegion(mData.dst.Get(), mData.dstOffset, mData.src.Get(), mData.srcOffset, mData.size);
    }
};

class CopyResourceAction: public RenderThreadDataExecutable<CopyResourceArgs>
{
public:
    CopyResourceAction(const D3D12ResourcePtr& dst, const D3D12ResourcePtr& src)
        : RenderThreadDataExecutable(dst, src)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->CommandList()->CopyResource(mData.dst.Get(), mData.src.Get());
    }
};

class CopyToTextureSmallAction: public RenderThreadDataExecutable<CopyToTextureSmallArgs>
{
public:
    CopyToTextureSmallAction(CopyToTextureSmallArgs&& args)
        : RenderThreadDataExecutable(std::move(args))
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->QueueResourceTransition(mData.dstResource, D3D12_RESOURCE_STATE_COPY_DEST);
        context->SubmitResourceTransitions();

        context->UpdateSmallTexture(mData.dstResource, mData.dstx, mData.dsty, mData.GetSourceDataBuffer(), mData.GetSourceDataSize(), mData.srcLoc);
    }
};

class DrawAction: public RenderThreadDataExecutable<DrawArgs>
{
public:
    DrawAction(uint32_t elements, uint32_t vbOffset)
        : RenderThreadDataExecutable(elements, vbOffset)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->CommandList()->DrawIndexedInstanced(mData.elements, 1, 0, mData.vbOffset, 0);
    }
};

class PrepareSwapChainAction: public RenderThreadDataExecutable<PrepareSwapChainArgs>
{
public:
    PrepareSwapChainAction(const NIPtr<NativeSwapChain>& swapchain, const D3D12_RECT& dirtyRegion)
        : RenderThreadDataExecutable(swapchain, dirtyRegion)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        mData.swapChain->Prepare(mData.dirtyRegion);

        context->QueueResourceTransition(mData.swapChain->GetTexture(), D3D12_RESOURCE_STATE_PRESENT);
        context->SubmitResourceTransitions();
    }
};

class PresentAction: public RenderThreadDataExecutable<NIPtr<NativeSwapChain>>
{
public:
    PresentAction(const NIPtr<NativeSwapChain>& swapchain)
        : RenderThreadDataExecutable(swapchain)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        mData->Present(context);
    }
};

class ResolveAction: public RenderThreadDataExecutable<ResolveArgs>
{
public:
    ResolveAction(const NIPtr<Internal::ITrackedResource>& dst, const NIPtr<Internal::ITrackedResource>& src, DXGI_FORMAT format)
        : RenderThreadDataExecutable(dst, src, format)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->QueueResourceTransition(mData.src, D3D12_RESOURCE_STATE_RESOLVE_SOURCE);
        context->QueueResourceTransition(mData.dst, D3D12_RESOURCE_STATE_RESOLVE_DEST);
        context->SubmitResourceTransitions();

        context->CommandList()->ResolveSubresource(mData.dst->GetD3D12Resource().Get(), 0, mData.src->GetD3D12Resource().Get(), 0, mData.format);
    }
};

class ResolveRegionAction: public RenderThreadDataExecutable<ResolveRegionArgs>
{
public:
    ResolveRegionAction(const NIPtr<Internal::ITrackedResource>& dst, uint32_t dstx, uint32_t dsty, const NIPtr<Internal::ITrackedResource>& src, D3D12_RECT srcRect, DXGI_FORMAT format)
        : RenderThreadDataExecutable(dst, dstx, dsty, src, srcRect, format)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->QueueResourceTransition(mData.src, D3D12_RESOURCE_STATE_RESOLVE_SOURCE);
        context->QueueResourceTransition(mData.dst, D3D12_RESOURCE_STATE_RESOLVE_DEST);
        context->SubmitResourceTransitions();

        context->CommandList()->ResolveSubresourceRegion(mData.dst->GetD3D12Resource().Get(), 0, mData.dstx, mData.dsty,
                                              mData.src->GetD3D12Resource().Get(), 0, &mData.srcRect,
                                              mData.format, D3D12_RESOLVE_MODE_AVERAGE);
    }
};

// this is an external barrier coming by RenderingContext's demand, transitioning an ITrackedResource-derivative
class ResourceTransitionAction: public RenderThreadDataExecutable<ResourceTransitionArgs>
{
public:
    ResourceTransitionAction(const NIPtr<Internal::ITrackedResource>& resource, D3D12_RESOURCE_STATES newState)
        : ResourceTransitionAction(resource, newState, D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES)
    {}

    ResourceTransitionAction(const NIPtr<Internal::ITrackedResource>& resource, D3D12_RESOURCE_STATES newState, uint32_t subresource)
        : RenderThreadDataExecutable(resource, newState, subresource)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->QueueResourceTransition(mData.resource, mData.newState, mData.subresource);
        context->SubmitResourceTransitions();
    }
};

// this is a Barrier directly placed on the command list. This is separate as a helper when initializing
// resources like Default-heap Buffers with data
class ResourceBarrierAction: public RenderThreadDataExecutable<D3D12_RESOURCE_BARRIER>
{
public:
    ResourceBarrierAction(const D3D12_RESOURCE_BARRIER& barrier)
        : RenderThreadDataExecutable(barrier)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->CommandList()->ResourceBarrier(1, &mData);
    }
};



// Graphics resource-related actions

class SetTexturesAction: public RenderThreadDataExecutable<Internal::TextureBank>
{
    // RT Payload also has to snapshot sampler descriptors here, otherwise
    // main thread can modify them before RT consumes them
    std::array<Internal::SamplerDesc, Constants::MAX_TEXTURE_UNITS> mSamplerDescs;

public:
    SetTexturesAction(const Internal::TextureBank& bank)
        : RenderThreadDataExecutable(bank)
    {
        for (uint32_t i = 0; i < bank.size(); ++i)
        {
            if (bank[i]) mSamplerDescs[i] = bank[i]->GetSamplerDesc();
        }
    }

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetTextures(mData, mSamplerDescs);
    }
};

class SetVertexShaderAction: public RenderThreadDataExecutable<NIPtr<Shaders::Shader>>
{
public:
    SetVertexShaderAction(const NIPtr<Shaders::Shader>& shader)
        : RenderThreadDataExecutable(shader)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetVertexShader(mData);
    }
};

class SetPixelShaderAction: public RenderThreadDataExecutable<NIPtr<Shaders::Shader>>
{
public:
    SetPixelShaderAction(const NIPtr<Shaders::Shader>& shader)
        : RenderThreadDataExecutable(shader)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetPixelShader(mData);
    }
};

class SetVertexShaderConstantsAction: public RenderThreadDataExecutable<ResourceManager::ShaderConstants>
{
public:
    SetVertexShaderConstantsAction(ResourceManager::ShaderConstants&& constants)
        : RenderThreadDataExecutable(std::move(constants))
    {
    }

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetVertexShaderConstants(std::move(mData));
    }
};

class SetPixelShaderConstantsAction: public RenderThreadDataExecutable<ResourceManager::ShaderConstants>
{
public:
    SetPixelShaderConstantsAction(ResourceManager::ShaderConstants&& constants)
        : RenderThreadDataExecutable(std::move(constants))
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetPixelShaderConstants(std::move(mData));
    }
};


// Compute

class ApplyComputePipelineState: public RenderThreadDataExecutable<ComputePSOParameters>
{
public:
    ApplyComputePipelineState(const ComputePSOParameters& data)
        : RenderThreadDataExecutable(data)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        const D3D12PipelineStatePtr& pso = context->PSOManager.GetPSO(mData);
        context->pipelineState.Set(pso);
    }
};

class DispatchAction: public RenderThreadDataExecutable<DispatchArgs>
{
public:
    DispatchAction(uint32_t x, uint32_t y, uint32_t z)
        : RenderThreadDataExecutable(x, y, z)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->Dispatch(mData.x, mData.y, mData.z);
    }
};


// Compute resource actions

class SetComputeShaderAction: public RenderThreadDataExecutable<NIPtr<Shaders::Shader>>
{
public:
    SetComputeShaderAction(const NIPtr<Shaders::Shader>& shader)
        : RenderThreadDataExecutable(shader)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetComputeShader(mData);
    }
};

class SetComputeShaderConstantsAction: public RenderThreadDataExecutable<ResourceManager::ShaderConstants>
{
public:
    SetComputeShaderConstantsAction(ResourceManager::ShaderConstants&& constants)
        : RenderThreadDataExecutable(std::move(constants))
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        context->resourceManager.SetComputeShaderConstants(std::move(mData));
    }
};


// Other/internal Executables //

class InternalRenderThreadRoutine: public RenderThreadDataExecutable<std::function<void()>>
{
    // Only RenderThread (and Internal::LinearAllocator) should be allowed to construct these objects
    friend class RenderThread;
    friend class Internal::LinearAllocator;
    using Callback = std::function<void()>;

    InternalRenderThreadRoutine(Callback&& c)
        : RenderThreadDataExecutable(std::move(c))
    {}

public:
    void Execute(const RenderThreadContextPtr&) override final
    {
        mData();
    }
};

class DisposePageableAction: public RenderThreadDataExecutable<D3D12PageablePtr>
{
public:
    DisposePageableAction(const D3D12PageablePtr& pageable)
        : RenderThreadDataExecutable(pageable)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        if (context)
        {
            context->resourceDisposer.MarkDisposed(mData);
        }
    }
};

class DisposeResourceAction: public RenderThreadDataExecutable<NIPtr<Internal::ITrackedResource>>
{
public:
    DisposeResourceAction(const NIPtr<Internal::ITrackedResource>& resource)
        : RenderThreadDataExecutable(resource)
    {}

    void Execute(const RenderThreadContextPtr& context) override final
    {
        if (context)
        {
            context->resourceDisposer.MarkDisposed(mData);
        }
    }
};

class DrawQuadsAction: public RenderThreadExecutable
{
    Internal::LinearAllocator& mAllocator;
    uint32_t mVertexCount;
    uint32_t mVerticesBytes;
    uint32_t mColorsBytes;
    float* mVertexData;
    unsigned char* mColorData;

public:
    DrawQuadsAction(Internal::LinearAllocator& allocator, const Internal::MemoryView<float>& vertices, const Internal::MemoryView<unsigned char>& colors, uint32_t vertexCount)
        : mAllocator(allocator)
        , mVertexCount(vertexCount)
        , mVerticesBytes(vertexCount * D3D12NI_FLOATS_PER_2D_VERTEX * sizeof(float))
        , mColorsBytes(vertexCount * D3D12NI_CHARS_PER_2D_VERTEX * sizeof(signed char))
        , mVertexData(reinterpret_cast<float*>(allocator.Allocate(mVerticesBytes)))
        , mColorData(reinterpret_cast<unsigned char*>(allocator.Allocate(mColorsBytes)))
    {
        if (mVertexData) memcpy(mVertexData, vertices.Data(), mVerticesBytes);
        if (mColorData) memcpy(mColorData, colors.Data(), mColorsBytes);
    }

    ~DrawQuadsAction()
    {
        if (mVertexData)
        {
            mAllocator.Free(mVertexData);
            mVertexData = nullptr;
        }

        if (mColorData)
        {
            mAllocator.Free(mColorData);
            mColorData = nullptr;
        }
    }

    void Execute(const RenderThreadContextPtr& context) override final
    {
        // TODO: D3D12: RenderThread error reporting
        if (!mVertexData || !mColorData) return;

        uint32_t vbOffset = context->PrepareQuadsDraw(mVertexData, mColorData, mVertexCount);
        if (vbOffset != std::numeric_limits<uint32_t>::max())
        {
            context->Draw((mVertexCount / 4) * 6, vbOffset);
        }
    }
};

class DrawMeshViewAction: public RenderThreadExecutable
{
    NIPtr<Internal::Buffer> mVertexBuffer;
    NIPtr<Internal::Buffer> mIndexBuffer;
    DXGI_FORMAT mIBFormat;
    uint32_t mIndexCount;

public:
    DrawMeshViewAction(const NIPtr<NativeMeshView>& meshView)
        : RenderThreadExecutable()
        , mVertexBuffer()
        , mIndexBuffer()
        , mIBFormat(DXGI_FORMAT_UNKNOWN)
        , mIndexCount(0)
    {
        if (meshView && meshView->GetMesh())
        {
            mVertexBuffer = meshView->GetMesh()->GetVertexBuffer();
            mIndexBuffer = meshView->GetMesh()->GetIndexBuffer();
            mIBFormat = meshView->GetMesh()->GetIndexBufferFormat();
            mIndexCount = meshView->GetMesh()->GetIndexCount();
        }
    }

    void Execute(const RenderThreadContextPtr& context) override final
    {
        if (!mVertexBuffer || !mIndexBuffer || (mIndexCount == 0)) return;

        context->PrepareMeshDraw(mVertexBuffer, mIndexBuffer, mIBFormat);
        context->Draw(mIndexCount, 0);
    }
};

} // namespace RenderThread
} // namespace D3D12
