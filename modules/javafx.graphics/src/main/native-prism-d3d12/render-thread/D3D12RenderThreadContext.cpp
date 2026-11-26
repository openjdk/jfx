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

#include "D3D12RenderThreadContext.hpp"


namespace D3D12 {
namespace RenderThread {

bool RenderThreadContext::Build2DIndexBuffer()
{
    // For 2D, Index Buffer is provided by the backend and has the same structure.
    std::array<uint16_t, Constants::MAX_BATCH_INDICES> indexBufferArray;

    // stolen from D3DContext.cc
    for (uint16_t i = 0; i != Constants::MAX_BATCH_QUADS; ++i) {
        uint16_t vtx = i * 4;
        uint16_t idx = i * 6;
        indexBufferArray[idx + 0] = vtx + 0;
        indexBufferArray[idx + 1] = vtx + 1;
        indexBufferArray[idx + 2] = vtx + 2;
        indexBufferArray[idx + 3] = vtx + 2;
        indexBufferArray[idx + 4] = vtx + 1;
        indexBufferArray[idx + 5] = vtx + 3;
    }

    size_t ibSize = indexBufferArray.size() * sizeof(uint16_t);
    if (!m2DIndexBuffer->Init(nullptr, ibSize, D3D12_HEAP_TYPE_DEFAULT))
    {
        D3D12NI_LOG_ERROR("Failed to initialize 2D Index Buffer");
        return false;
    }

    UpdateSmallBuffer(m2DIndexBuffer, indexBufferArray.data(), ibSize);
    QueueResourceTransition(m2DIndexBuffer, D3D12_RESOURCE_STATE_INDEX_BUFFER);

    m2DIndexBuffer->SetName("2D Index Buffer");

    return true;
}

// NOTE technically we don't query buffer ptr's size, but this function assumes we reserved enough
// space already.
void RenderThreadContext::AssembleVertexData(void* buffer, const float* vertices,
                                             const unsigned char* colors, size_t elementCount)
{
    Vertex_2D* bufVertices = reinterpret_cast<Vertex_2D*>(buffer);

    size_t vertIdx = 0;
    size_t colorIdx = 0;
    for (UINT i = 0; i < elementCount; ++i)
    {
        bufVertices[i].pos.x = vertices[vertIdx++];
        bufVertices[i].pos.y = vertices[vertIdx++];
        bufVertices[i].pos.z = vertices[vertIdx++];
        bufVertices[i].color.r = colors[colorIdx++];
        bufVertices[i].color.g = colors[colorIdx++];
        bufVertices[i].color.b = colors[colorIdx++];
        bufVertices[i].color.a = colors[colorIdx++];
        bufVertices[i].uv1.u = vertices[vertIdx++];
        bufVertices[i].uv1.v = vertices[vertIdx++];
        bufVertices[i].uv2.u = vertices[vertIdx++];
        bufVertices[i].uv2.v = vertices[vertIdx++];
    }
}

void RenderThreadContext::DeclareVertexData(uint32_t vertexCount)
{
    if (vertexCount > (Constants::MAX_BATCH_VERTICES / 2))
    {
        // rendering more vertices might utilize the Ring Buffer better if we just reserve a separate space for them
        mVertexRingBuffer.DeclareRequired(vertexCount * 8 * sizeof(float));
        return;
    }

    if (!m2DVertexBatch.Valid() || vertexCount > m2DVertexBatch.Available())
    {
        // declare space for the new Vertex Buffer if current one does not exist, or if it is full
        mVertexRingBuffer.DeclareRequired(Constants::MAX_BATCH_VERTICES * 8 * sizeof(float));
    }
}

RenderThreadContext::VertexSubregion RenderThreadContext::GetNewRegionForVertices(uint32_t vertexCount)
{
    if (vertexCount > (Constants::MAX_BATCH_VERTICES / 2))
    {
        // rendering more vertices might utilize the Ring Buffer better if we just reserve a separate space for them
        RingBuffer::Region region = mVertexRingBuffer.Reserve(vertexCount * Constants::VERTEX_2D_SIZE_BYTES);
        if (!region)
        {
            D3D12NI_LOG_ERROR("2D Vertex Ring Buffer allocation failed");
            return VertexSubregion();
        }

        VertexSubregion separateRegion;
        separateRegion.subregion = region;
        separateRegion.view.BufferLocation = region.gpu;
        separateRegion.view.SizeInBytes = static_cast<UINT>(region.size);
        separateRegion.view.StrideInBytes = sizeof(Vertex_2D);

        return separateRegion;
    }

    if (!m2DVertexBatch.Valid() || vertexCount > m2DVertexBatch.Available())
    {
        // reserve space on Ring Buffer for the new small-quad-count VB
        RingBuffer::Region newVertexRegion = mVertexRingBuffer.Reserve(Constants::MAX_BATCH_VERTICES_BYTES);
        if (!newVertexRegion)
        {
            D3D12NI_LOG_ERROR("2D Vertex Ring Buffer allocation failed");
            return VertexSubregion();
        }

        m2DVertexBatch.AssignNewRegion(newVertexRegion, newVertexRegion);
    }

    return m2DVertexBatch.Subregion(vertexCount);
}

void RenderThreadContext::EnsureBoundTexturesState(D3D12_RESOURCE_STATES texturesState)
{
    const NIPtr<Internal::IRenderTarget>& rt = renderTarget.Get().renderTarget;

    QueueResourceTransition(rt->GetTexture(), D3D12_RESOURCE_STATE_RENDER_TARGET);
    if (renderTarget.Get().depthEnabled && rt->GetDepthTexture())
        QueueResourceTransition(rt->GetDepthTexture(), D3D12_RESOURCE_STATE_DEPTH_WRITE);

    for (uint32_t i = 0; i < Constants::MAX_TEXTURE_UNITS; ++i)
    {
        const NIPtr<Internal::TextureBase>& tex = resourceManager.GetTexture(i);
        if (tex) QueueResourceTransition(tex, texturesState);
    }

    SubmitResourceTransitions();
}

RingBuffer::Region RenderThreadContext::PushToDataRingBuffer(const void* data, size_t size)
{
    mDataRingBuffer.DeclareRequired(size);
    RingBuffer::Region region = mDataRingBuffer.Reserve(size);
    if (!region)
    {
        D3D12NI_LOG_ERROR("Failed to reserve space on Ring Buffer");
        return RingBuffer::Region();
    }

    memcpy(region.cpu, data, size);
    return region;
}

void RenderThreadContext::SetIndexBuffer(const D3D12_INDEX_BUFFER_VIEW& ibView)
{
    if (indexBuffer.Get().BufferLocation == ibView.BufferLocation &&
        indexBuffer.Get().SizeInBytes == ibView.SizeInBytes &&
        indexBuffer.Get().Format == ibView.Format)
        return;

    indexBuffer.Set(ibView);
}

void RenderThreadContext::SetVertexBuffer(const D3D12_VERTEX_BUFFER_VIEW& vbView)
{
    if (vertexBuffer.Get().BufferLocation == vbView.BufferLocation &&
        vertexBuffer.Get().SizeInBytes == vbView.SizeInBytes &&
        vertexBuffer.Get().StrideInBytes == vbView.StrideInBytes)
        return;

    vertexBuffer.Set(vbView);
}

RenderThreadContext::RenderThreadContext(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback, const SignalCallback& signalCallback)
    : mCommandListPool(nativeDevice, waitCallback)
    , m2DVertexBatch()
    , m2DIndexBuffer(std::make_shared<Internal::Buffer>(nativeDevice))
    , mVertexRingBuffer(nativeDevice, flushCallback, waitCallback)
    , mDataRingBuffer(nativeDevice, flushCallback, waitCallback)
    , PSOManager(nativeDevice)
    , resourceManager(nativeDevice, flushCallback, waitCallback)
    , resourceDisposer(nativeDevice)
    , barrierQueue()
    , barrierQueueSize(0)
    , flushCommandList(flushCallback)
    , waitForCheckpoint(waitCallback)
    , signal(signalCallback)
{
}

bool RenderThreadContext::Init()
{
    if (!PSOManager.Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize PSO Manager");
        return false;
    }

    graphicsRootSignature.Set(PSOManager.GetRootSignatureManager().GetGraphicsRootSignature());
    computeRootSignature.Set(PSOManager.GetRootSignatureManager().GetComputeRootSignature());

    if (!resourceManager.Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize Resource Manager");
        return false;
    }

    // Descriptor Heaps are set once from ResourceManager. They still need to be
    // re-applied every Command List, so we run them via CommandListSteps, but
    // Quantum-thread-side will not modify them.
    descriptorHeaps.Set({resourceManager.GetHeap(), resourceManager.GetSamplerHeap()});

    mVertexRingBuffer.SetDebugName("2D Vertex GPU Ring Buffer");
    if (!mVertexRingBuffer.Init(Internal::Config::MainRingBufferThreshold(), D3D12_DEFAULT_RESOURCE_PLACEMENT_ALIGNMENT))
    {
        D3D12NI_LOG_ERROR("Failed to initialize the Vertex Ring Buffer");
        return false;
    }

    mDataRingBuffer.SetDebugName("Extra Data Ring Buffer");
    if (!mDataRingBuffer.Init(Internal::Config::MainRingBufferThreshold(), D3D12_DEFAULT_RESOURCE_PLACEMENT_ALIGNMENT))
    {
        D3D12NI_LOG_ERROR("Failed to initialize the Extra Data Ring Buffer");
        return false;
    }

    // NOTE: Command List Pool requires to have as many Command Allocators as SwapChain buffers + 1
    // This is the minimum we need (one per frame) and also ensures an extra one to pre-record more commands
    // if both SwapChain buffers are full.
    if (!mCommandListPool.Init(
            D3D12_COMMAND_LIST_TYPE_DIRECT,
            Constants::COMMAND_LIST_POOL_MAX_LISTS,
            Constants::COMMAND_LIST_POOL_MAX_ALLOCATORS
        ))
    {
        D3D12NI_LOG_ERROR("Failed to initialize Command List Pool");
        return false;
    }

    if (!Build2DIndexBuffer())
    {
        return false;
    }

    return true;
}

void RenderThreadContext::Release()
{
    mCommandListPool.Release();
}

void RenderThreadContext::QueueResourceTransition(const NIPtr<Internal::ITrackedResource>& resource, D3D12_RESOURCE_STATES newState, uint32_t subresource)
{
    if (!resource->NeedsStateTransitions()) return;
    if (resource->GetD3D12ResourceState(subresource) == newState) return;

    QueueD3D12ResourceTransition(resource->GetD3D12Resource(), resource->GetD3D12ResourceState(subresource), newState, subresource);
    resource->SetD3D12ResourceState(newState, subresource);
}

void RenderThreadContext::QueueD3D12ResourceTransition(const D3D12ResourcePtr& resource, D3D12_RESOURCE_STATES oldState, D3D12_RESOURCE_STATES newState, uint32_t subresource)
{
    D3D12NI_ZERO_STRUCT(barrierQueue[barrierQueueSize]);
    barrierQueue[barrierQueueSize].Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrierQueue[barrierQueueSize].Transition.pResource = resource.Get();
    barrierQueue[barrierQueueSize].Transition.StateBefore = oldState;
    barrierQueue[barrierQueueSize].Transition.StateAfter = newState;
    barrierQueue[barrierQueueSize].Transition.Subresource = subresource;
    ++barrierQueueSize;

    if (barrierQueueSize == barrierQueue.size()) SubmitResourceTransitions();
}

void RenderThreadContext::SubmitResourceTransitions()
{
    if (barrierQueueSize == 0) return;

    CommandList()->ResourceBarrier(barrierQueueSize, barrierQueue.data());
    barrierQueueSize = 0;
}

void RenderThreadContext::ApplySteps()
{
    const D3D12GraphicsCommandListPtr& commandList = CommandList();

    indexBuffer.Apply(commandList);
    vertexBuffer.Apply(commandList);
    primitiveTopology.Apply(commandList);
    scissor.Apply(commandList);
    viewport.Apply(commandList);
    renderTarget.Apply(commandList);
    pipelineState.Apply(commandList);
    descriptorHeaps.Apply(commandList);
    graphicsRootSignature.Apply(commandList);

    resourceManager.ApplyResources(commandList);
}

void RenderThreadContext::ApplyComputeSteps()
{
    // TODO error reporting
    resourceManager.DeclareComputeRingResources();
    if (!resourceManager.PrepareComputeResources())
    {
        D3D12NI_LOG_ERROR("RenderThread: Failed to prepare resources for draw call");
        return;
    }

    const D3D12GraphicsCommandListPtr& commandList = CommandList();

    pipelineState.Apply(commandList);
    descriptorHeaps.Apply(commandList);
    computeRootSignature.Apply(commandList);

    resourceManager.ApplyComputeResources(commandList);
}

void RenderThreadContext::ClearAppliedSteps()
{
    indexBuffer.ClearApplied();
    vertexBuffer.ClearApplied();
    primitiveTopology.ClearApplied();
    scissor.ClearApplied();
    viewport.ClearApplied();
    renderTarget.ClearApplied();
    pipelineState.ClearApplied();
    descriptorHeaps.ClearApplied();
    graphicsRootSignature.ClearApplied();
    computeRootSignature.ClearApplied();
}

void RenderThreadContext::Draw(uint32_t elements, uint32_t vbOffset)
{
    // TODO error reporting
    ApplySteps();

    EnsureBoundTexturesState(D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);

    // Do the draw
    CommandList()->DrawIndexedInstanced(elements, 1, 0, vbOffset, 0);
}

void RenderThreadContext::Dispatch(uint32_t x, uint32_t y, uint32_t z)
{
    ApplyComputeSteps();

    CommandList()->Dispatch(x, y, z);
}

void RenderThreadContext::UpdateSmallBuffer(const NIPtr<Internal::ITrackedResource>& dstBuffer,
                                            const void* srcData, size_t srcDataSize)
{
    RingBuffer::Region region = PushToDataRingBuffer(srcData, srcDataSize);
    if (!region) return;

    QueueResourceTransition(dstBuffer, D3D12_RESOURCE_STATE_COPY_DEST);
    SubmitResourceTransitions();

    CommandList()->CopyBufferRegion(dstBuffer->GetD3D12Resource().Get(), 0, mDataRingBuffer.GetResource().Get(), region.offsetFromStart, srcDataSize);
}

void RenderThreadContext::UpdateSmallTexture(const NIPtr<Internal::ITrackedResource>& dstTexture, uint32_t dstx, uint32_t dsty,
                                             const void* srcData, size_t srcDataSize, const D3D12_TEXTURE_COPY_LOCATION& srcLoc)
{
    RingBuffer::Region region = PushToDataRingBuffer(srcData, srcDataSize);
    if (!region) return;

    D3D12_TEXTURE_COPY_LOCATION properSrcLoc(srcLoc);
    properSrcLoc.pResource = mDataRingBuffer.GetResource().Get();
    properSrcLoc.PlacedFootprint.Offset = region.offsetFromStart;

    D3D12_TEXTURE_COPY_LOCATION dstLoc;
    D3D12NI_ZERO_STRUCT(dstLoc);
    dstLoc.pResource = dstTexture->GetD3D12Resource().Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    QueueResourceTransition(dstTexture, D3D12_RESOURCE_STATE_COPY_DEST);
    SubmitResourceTransitions();

    CommandList()->CopyTextureRegion(&dstLoc, dstx, dsty, 0, &properSrcLoc, nullptr);
}

uint32_t RenderThreadContext::PrepareQuadsDraw(float* vertices, unsigned char* colors, uint32_t vertexCount)
{
    // TODO error reporting
    // We'll first declare resources used by Ring Containers. That can trigger a CL flush
    // and we want to ensure all data used for rendering will be available for the GPU.
    resourceManager.DeclareRingResources();
    DeclareVertexData(vertexCount);

    // now that we potentially flushed the pipeline and have space for data, prepare IB and VB
    D3D12_INDEX_BUFFER_VIEW ibView;
    ibView.BufferLocation = m2DIndexBuffer->GetGPUPtr();
    ibView.SizeInBytes = static_cast<UINT>(m2DIndexBuffer->Size());
    ibView.Format = DXGI_FORMAT_R16_UINT;
    SetIndexBuffer(ibView);

    VertexSubregion vertexRegion = GetNewRegionForVertices(vertexCount);
    if (!vertexRegion) return std::numeric_limits<uint32_t>::max();

    AssembleVertexData(vertexRegion.subregion.cpu, vertices, colors, vertexCount);
    SetVertexBuffer(vertexRegion.view);

    // finally, prepare remaining resources
    if (!resourceManager.PrepareResources())
    {
        D3D12NI_LOG_ERROR("RenderThread: Failed to prepare resources for draw quads call");
        return std::numeric_limits<uint32_t>::max();
    }

    return vertexRegion.startOffset;
}

void RenderThreadContext::PrepareMeshDraw(const NIPtr<Internal::Buffer>& vertexBuffer, const NIPtr<Internal::Buffer>& indexBuffer, DXGI_FORMAT ibFormat)
{
    // declare and prepare resources for rendering
    resourceManager.DeclareRingResources();
    if (!resourceManager.PrepareResources())
    {
        D3D12NI_LOG_ERROR("RenderThread: Failed to prepare resources for draw mesh call");
        return;
    }

    // set up VB and IB; those compared to PrepareQuadsDraw are preallocated and ready to directly use
    D3D12_VERTEX_BUFFER_VIEW vbView;
    vbView.BufferLocation = vertexBuffer->GetGPUPtr();
    vbView.SizeInBytes = static_cast<UINT>(vertexBuffer->Size());
    vbView.StrideInBytes = Constants::VERTEX_BUFFER_3D_STRIDE;
    SetVertexBuffer(vbView);

    D3D12_INDEX_BUFFER_VIEW ibView;
    ibView.BufferLocation = indexBuffer->GetGPUPtr();
    ibView.SizeInBytes = static_cast<UINT>(indexBuffer->Size());
    ibView.Format = ibFormat;
    SetIndexBuffer(ibView);
}

} // namespace RenderThread
} // namespace D3D12
