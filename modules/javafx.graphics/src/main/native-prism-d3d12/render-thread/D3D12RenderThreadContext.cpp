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
    std::array<uint16_t, Constants::MAX_BATCH_QUADS * 6> indexBufferArray;

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

    if (!m2DIndexBuffer.Init(indexBufferArray.data(), indexBufferArray.size() * sizeof(uint16_t),
            D3D12_HEAP_TYPE_DEFAULT, D3D12_RESOURCE_STATE_INDEX_BUFFER))
    {
        D3D12NI_LOG_ERROR("Failed to build 2D Index Buffer");
        return false;
    }

    m2DIndexBuffer.SetName("2D Index Buffer");

    return true;
}

RenderThreadContext::QuadVertices RenderThreadContext::AssembleVertexQuadForBlit(const Coords_Box_UINT32& src, const Coords_Box_UINT32& dst)
{
    uint32_t srcWidth = src.x1 - src.x0;
    uint32_t srcHeight = src.y1 - src.y0;
    uint32_t dstWidth = dst.x1 - dst.x0;
    uint32_t dstHeight = dst.y1 - dst.y0;

    // default viewport coordinates are from -1 to 1
    // so destination has to be doubled and shifted from 0-2 range to -1-1 range
    Coords_UV_FLOAT srcTexelSize{ 1.0f / srcWidth, 1.0f / srcHeight };
    Coords_UV_FLOAT dstTexelSize{ 2.0f / dstWidth, 2.0f / dstHeight };

    // dstBox V coord is inverted because of d3d12's coordinate system
    Coords_UV_FLOAT srcBoxTexelsMin{ src.x0 * srcTexelSize.u, src.y0 * srcTexelSize.v };
    Coords_UV_FLOAT srcBoxTexelsMax{ src.x1 * srcTexelSize.u, src.y1 * srcTexelSize.v };
    Coords_UV_FLOAT dstBoxTexelsMin{ (dst.x0 * dstTexelSize.u) - 1.0f, (dst.y1 * dstTexelSize.v) - 1.0f };
    Coords_UV_FLOAT dstBoxTexelsMax{ (dst.x1 * dstTexelSize.u) - 1.0f, (dst.y0 * dstTexelSize.v) - 1.0f };

    QuadVertices result;

    // Build a fullscreen quad used for slow blit path
    result[0].pos.x = dstBoxTexelsMin.u;
    result[0].pos.y = dstBoxTexelsMin.v;
    result[0].uv1.u = srcBoxTexelsMin.u;
    result[0].uv1.v = srcBoxTexelsMin.v;

    result[1].pos.x = dstBoxTexelsMin.u;
    result[1].pos.y = dstBoxTexelsMax.v;
    result[1].uv1.u = srcBoxTexelsMin.u;
    result[1].uv1.v = srcBoxTexelsMax.v;

    result[2].pos.x = dstBoxTexelsMax.u;
    result[2].pos.y = dstBoxTexelsMin.v;
    result[2].uv1.u = srcBoxTexelsMax.u;
    result[2].uv1.v = srcBoxTexelsMin.v;

    result[3].pos.x = dstBoxTexelsMax.u;
    result[3].pos.y = dstBoxTexelsMax.v;
    result[3].uv1.u = srcBoxTexelsMax.u;
    result[3].uv1.v = srcBoxTexelsMax.v;

    for (uint32_t i = 0; i < result.size(); ++i)
    {
        result[i].pos.z = 0.0f;
        result[i].color.r = 255;
        result[i].color.g = 255;
        result[i].color.b = 255;
        result[i].color.a = 255;
        result[i].uv2 = result[i].uv1;
    }

    return result;
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

RenderThreadContext::VertexSubregion RenderThreadContext::GetNewRegionForVertices(uint32_t vertexCount)
{
    if (vertexCount > (Constants::MAX_BATCH_VERTICES / 2))
    {
        // rendering more vertices might utilize the Ring Buffer better if we just reserve a separate space for them
        mVertexRingBuffer.DeclareRequired(vertexCount * 8 * sizeof(float));
        RingBuffer::Region region = mVertexRingBuffer.Reserve(vertexCount * 8 * sizeof(float));
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
        // reserve space on Ring Buffer
        mVertexRingBuffer.DeclareRequired(Constants::MAX_BATCH_VERTICES * 8 * sizeof(float));

        RingBuffer::Region newVertexRegion = mVertexRingBuffer.Reserve(Constants::MAX_BATCH_VERTICES * 8 * sizeof(float));
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
    , m2DIndexBuffer(nativeDevice)
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
    if (!mCommandListPool.Init(D3D12_COMMAND_LIST_TYPE_DIRECT, 16, 4))
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
    // TODO error reporting
    resourceManager.DeclareRingResources();
    if (!resourceManager.PrepareResources())
    {
        D3D12NI_LOG_ERROR("RenderThread: Failed to prepare resources for draw call");
        return;
    }

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

void RenderThreadContext::UpdateSmallTexture(const NIPtr<Internal::ITrackedResource>& dstTexture, uint32_t dstx, uint32_t dsty,
                                             const void* srcData, size_t srcDataSize, const D3D12_TEXTURE_COPY_LOCATION& srcLoc)
{
    mDataRingBuffer.DeclareRequired(srcDataSize);
    RingBuffer::Region region = mDataRingBuffer.Reserve(srcDataSize);
    if (!region)
    {
        D3D12NI_LOG_ERROR("Failed to reserve space for small texture update");
        return;
    }

    memcpy(region.cpu, srcData, srcDataSize);

    D3D12_TEXTURE_COPY_LOCATION properSrcLoc(srcLoc);
    properSrcLoc.pResource = mDataRingBuffer.GetResource().Get();
    properSrcLoc.PlacedFootprint.Offset = region.offsetFromStart;

    D3D12_TEXTURE_COPY_LOCATION dstLoc;
    D3D12NI_ZERO_STRUCT(dstLoc);
    dstLoc.pResource = dstTexture->GetD3D12Resource().Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    CommandList()->CopyTextureRegion(&dstLoc, dstx, dsty, 0, &properSrcLoc, nullptr);
}

uint32_t RenderThreadContext::PrepareQuadsDraw(float* vertices, unsigned char* colors, uint32_t vertexCount)
{
    D3D12_INDEX_BUFFER_VIEW ibView;
    ibView.BufferLocation = m2DIndexBuffer.GetGPUPtr();
    ibView.SizeInBytes = static_cast<UINT>(m2DIndexBuffer.Size());
    ibView.Format = DXGI_FORMAT_R16_UINT;
    SetIndexBuffer(ibView);

    VertexSubregion vertexRegion = GetNewRegionForVertices(vertexCount);
    if (!vertexRegion) return std::numeric_limits<uint32_t>::max();

    AssembleVertexData(vertexRegion.subregion.cpu, vertices, colors, vertexCount);
    SetVertexBuffer(vertexRegion.view);

    return vertexRegion.startOffset;
}

void RenderThreadContext::PrepareMeshDraw(const NIPtr<Internal::Buffer>& vertexBuffer, const NIPtr<Internal::Buffer>& indexBuffer, DXGI_FORMAT ibFormat)
{
    D3D12_VERTEX_BUFFER_VIEW vbView;
    vbView.BufferLocation = vertexBuffer->GetGPUPtr();
    vbView.SizeInBytes = static_cast<UINT>(vertexBuffer->Size());
    vbView.StrideInBytes = 9 * sizeof(float); // 3 * modelVertexPos; 2 * texD; 4 * modelVertexNormal
    SetVertexBuffer(vbView);

    D3D12_INDEX_BUFFER_VIEW ibView;
    ibView.BufferLocation = indexBuffer->GetGPUPtr();
    ibView.SizeInBytes = static_cast<UINT>(indexBuffer->Size());
    ibView.Format = ibFormat;
    SetIndexBuffer(ibView);
}

} // namespace RenderThread
} // namespace D3D12
