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

#include "internal/D3D12RenderingStep.hpp"

#include "D3D12Buffer.hpp"
#include "D3D12PSOManager.hpp"
#include "D3D12CheckpointQueue.hpp"
#include "D3D12CommandListPool.hpp"
#include "D3D12IRenderTarget.hpp"
#include "D3D12ResourceManager.hpp"
#include "D3D12ResourceDisposer.hpp"


namespace D3D12 {
namespace RenderThread {

template <typename T>
class CommandListDataStep: public Internal::RenderingDataStep<T>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const = 0;

public:
    void Apply(const D3D12GraphicsCommandListPtr& commandList)
    {
        if (this->CanBeSkipped()) return;

        this->ApplyImpl(commandList);
        this->mIsApplied = true;
    }
};

class IndexBufferCommandListStep: public CommandListDataStep<D3D12_INDEX_BUFFER_VIEW>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->IASetIndexBuffer(&this->mParameter);
    }
};

class VertexBufferCommandListStep: public CommandListDataStep<D3D12_VERTEX_BUFFER_VIEW>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->IASetVertexBuffers(0, 1, &this->mParameter);
    }
};

class PrimitiveTopologyCommandListStep: public CommandListDataStep<D3D12_PRIMITIVE_TOPOLOGY>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->IASetPrimitiveTopology(this->mParameter);
    }
};

class ScissorCommandListStep: public CommandListDataStep<D3D12_RECT>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->RSSetScissorRects(1, &this->mParameter);
    }
};

class ViewportCommandListStep: public CommandListDataStep<D3D12_VIEWPORT>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->RSSetViewports(1, &this->mParameter);
    }
};

class PipelineStateCommandListStep: public CommandListDataStep<D3D12PipelineStatePtr>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->SetPipelineState(mParameter.Get());
    }
};

class DescriptorHeapsCommandListStep: public CommandListDataStep<std::array<D3D12DescriptorHeapPtr, 2>>
{
public:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        ID3D12DescriptorHeap* heaps[2] = {
            mParameter[0].Get(),
            mParameter[1].Get()
        };
        commandList->SetDescriptorHeaps(2, heaps);
    }
};

class GraphicsRootSignatureCommandListStep: public CommandListDataStep<D3D12RootSignaturePtr>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->SetGraphicsRootSignature(mParameter.Get());
    }
};

class ComputeRootSignatureCommandListStep: public CommandListDataStep<D3D12RootSignaturePtr>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        commandList->SetComputeRootSignature(mParameter.Get());
    }
};

struct RenderTargetData
{
    NIPtr<Internal::IRenderTarget> renderTarget;
    bool depthEnabled; // captured separately to prevent a race with Quantum thread
                       // Quantum thread can change the depth enabled flag before RT consumes the depth-using draw

    RenderTargetData() = default;
    RenderTargetData(const NIPtr<Internal::IRenderTarget>& rt)
        : renderTarget(rt), depthEnabled(rt ? rt->IsDepthTestEnabled() : false)
    {}
};

class RenderTargetCommandListStep: public CommandListDataStep<RenderTargetData>
{
protected:
    virtual void ApplyImpl(const D3D12GraphicsCommandListPtr& commandList) const override
    {
        // rtTexture/depthTexture will be used by EnsureBoundTextureStates
        commandList->OMSetRenderTargets(
            mParameter.renderTarget->GetRTVDescriptorData().count, &mParameter.renderTarget->GetRTVDescriptorData().cpu, true,
            mParameter.depthEnabled && mParameter.renderTarget->GetDSVDescriptorData() ? &mParameter.renderTarget->GetDSVDescriptorData().cpu : nullptr
        );
    }
};

/**
 * Stores information about current state of the Command List and records parameter
 * setting commands as needed.
 *
 * JFX assumes the "old API" behavior - API has a context and some things that were
 * set in the Context remain there. With D3D12, due to different API design this is
 * not the case:
 *   - There is no global Context which would be responsible for such behavior
 *   - Rendering parameters (ex. VBs, IBs, PSOs...) are set via a Command
 *     List and are assumed to be local only within that Command List
 *
 * That means, if we ever want to submit a Command List and start recording to a new
 * one, many objects related to current rendering state are lost and must be re-recorded.
 *
 * This class handles that problem while making sure we don't produce any redundant API
 * calls (aka. command records).
 */
class RenderThreadContext
{
    using QuadVertices = std::array<Vertex_2D, 4>;

    struct VertexSubregion
    {
        uint32_t startOffset; // counted in vertices since start of entire region
        RingBuffer::Region subregion;
        D3D12_VERTEX_BUFFER_VIEW view;

        VertexSubregion()
            : startOffset(0)
            , subregion()
            , view()
        {}

        operator bool() const
        {
            return subregion.operator bool();
        }
    };

    class VertexBatch
    {
        uint32_t mTaken;
        RingBuffer::Region mCPURegion;
        RingBuffer::Region mGPURegion;
        D3D12_VERTEX_BUFFER_VIEW mView;

        size_t ElementsToBytes(size_t elements)
        {
            return elements * sizeof(Vertex_2D);
        }

    public:
        VertexBatch()
            : mTaken(0)
            , mCPURegion()
            , mGPURegion()
            , mView()
        {}

        inline uint32_t Available() const
        {
            return (Constants::MAX_BATCH_VERTICES - mTaken);
        }

        inline bool Valid() const
        {
            return mCPURegion.operator bool();
        }

        inline void Invalidate()
        {
            mCPURegion = RingBuffer::Region();
            mGPURegion = RingBuffer::Region();
            mTaken = 0;
            D3D12NI_ZERO_STRUCT(mView);
        }

        void AssignNewRegion(const RingBuffer::Region& cpuRegion, const RingBuffer::Region& gpuRegion)
        {
            mCPURegion = cpuRegion;
            mGPURegion = gpuRegion;
            mTaken = 0;

            mView.BufferLocation = gpuRegion.gpu;
            mView.SizeInBytes = static_cast<UINT>(gpuRegion.size);
            mView.StrideInBytes = sizeof(Vertex_2D); // 3x pos, 1x uint32 color, 2x uv, 2x uv
        }

        VertexSubregion Subregion(uint32_t elements)
        {
            D3D12NI_ASSERT(elements <= (Constants::MAX_BATCH_VERTICES - mTaken), "Attempted to exceed VB Batch size");
            D3D12NI_ASSERT(mCPURegion == true, "No assigned vertex buffer region");

            VertexSubregion result;
            result.subregion = mCPURegion.Subregion(ElementsToBytes(mTaken), ElementsToBytes(elements));
            result.startOffset = mTaken;
            result.view = mView;

            mTaken += elements;
            return result;
        }
    };

    CommandListPool mCommandListPool;
    VertexBatch m2DVertexBatch;
    Internal::Buffer m2DIndexBuffer;
    RingBuffer mVertexRingBuffer; // used for 2D Vertex data
    RingBuffer mDataRingBuffer; // used for small Texture upload and other stuff

    bool Build2DIndexBuffer();
    QuadVertices AssembleVertexQuadForBlit(const Coords_Box_UINT32& src, const Coords_Box_UINT32& dst);
    void AssembleVertexData(void* buffer, const float* vertices, const unsigned char* colors, size_t elementCount);
    VertexSubregion GetNewRegionForVertices(uint32_t vertexCount);
    void EnsureBoundTexturesState(D3D12_RESOURCE_STATES textureState);
    void SetVertexBuffer(const D3D12_VERTEX_BUFFER_VIEW& vbView);
    void SetIndexBuffer(const D3D12_INDEX_BUFFER_VIEW& ibView);

public:
    PSOManager PSOManager;
    ResourceManager resourceManager;
    ResourceDisposer resourceDisposer;
    std::array<D3D12_RESOURCE_BARRIER, 8> barrierQueue;
    uint32_t barrierQueueSize;

    // RT callbacks to control command lists
    CheckpointCallback flushCommandList;
    CheckpointCallback waitForCheckpoint;
    SignalCallback signal;

    // RenderThread-side Command List parameters. This mostly clones what RenderingContext has.
    // RenderingContext updates these via RTExecutables and those are reflected on CommandLists.
    // The reason for duplicating these is to both reduce Command List records, as well as to
    // restore the Command List to its "known state" when internally RenderThread flushes
    // the Command List

    // Graphics
    IndexBufferCommandListStep indexBuffer;
    VertexBufferCommandListStep vertexBuffer;
    PrimitiveTopologyCommandListStep primitiveTopology;
    ScissorCommandListStep scissor;
    ViewportCommandListStep viewport;
    PipelineStateCommandListStep pipelineState;
    DescriptorHeapsCommandListStep descriptorHeaps;
    RenderTargetCommandListStep renderTarget;
    GraphicsRootSignatureCommandListStep graphicsRootSignature;

    // Compute
    ComputeRootSignatureCommandListStep computeRootSignature;

    RenderThreadContext(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback, const SignalCallback& signalCallback);
    bool Init();
    void Release();
    void ExecuteCurrentCommandList();

    void QueueResourceTransition(const NIPtr<Internal::ITrackedResource>& resource, D3D12_RESOURCE_STATES newState, uint32_t subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);
    void QueueD3D12ResourceTransition(const D3D12ResourcePtr& resource, D3D12_RESOURCE_STATES oldState, D3D12_RESOURCE_STATES newState, uint32_t subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);
    void SubmitResourceTransitions();

    void ApplySteps();
    void ApplyComputeSteps();
    void ClearAppliedSteps();

    uint32_t PrepareQuadsDraw(float* vertices, unsigned char* colors, uint32_t vertexCount);
    void PrepareMeshDraw(const NIPtr<Internal::Buffer>& vertexBuffer, const NIPtr<Internal::Buffer>& indexBuffer, DXGI_FORMAT ibFormat);
    void Draw(uint32_t elements, uint32_t vbOffset);
    void Dispatch(uint32_t x, uint32_t y, uint32_t z);
    void UpdateSmallTexture(const NIPtr<Internal::ITrackedResource>& dstTexture, uint32_t dstx, uint32_t dsty,
                            const void* srcData, size_t srcDataSize, const D3D12_TEXTURE_COPY_LOCATION& srcLoc);

    inline void Invalidate2DVertexBatch()
    {
        m2DVertexBatch.Invalidate();
    }

    inline const D3D12GraphicsCommandListPtr& CommandList()
    {
        return mCommandListPool.CurrentCommandList();
    }

    inline const D3D12GraphicsCommandListPtr& AdvanceCommandList(bool advanceAllocator)
    {
        return mCommandListPool.AdvanceCommandList(advanceAllocator);
    }
};

using RenderThreadContextPtr = std::unique_ptr<RenderThreadContext>;

} // namespace RenderThread
} // namespace D3D12
