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

#include "../D3D12NativeTexture.hpp"
#include "../D3D12NativeSwapChain.hpp"

#include "D3D12Buffer.hpp"
#include "D3D12IRenderTarget.hpp"
#include "D3D12LinearAllocator.hpp"
#include "D3D12Matrix.hpp"
#include "D3D12RenderingParameter.hpp"
#include "MemoryView.hpp"

#include "render-thread/D3D12CheckpointQueue.hpp"
#include "render-thread/D3D12RenderThread.hpp"

#include <unordered_set>


namespace D3D12 {
namespace Internal {

class RenderingContext
{
    NIPtr<NativeDevice> mNativeDevice;
    LinearAllocator mExtraPayloadDataAllocator; // for 2D Vertex data and small texture updates
    LinearAllocator mPayloadAllocator;
    RenderThread::RenderThread mRenderThread;
    RenderThread::RenderPayloadPtr mRTPayload;

    struct ClearOptState
    {
        bool clearDelayed;
        D3D12_RECT clearRect;
        bool clearDepth;
    } mClearOptState;

    // some parameters are set by the Java Runtime ex. transforms, composite mode, textures
    // whenever we need to execute some internal operation (ex. BlitTexture()) we have to
    // temporarily "stash" them to restore them later. This struct helps with that.
    // We probably won't need more than one "stash".
    struct RuntimeParametersStash
    {
        PipelineStateRenderingParameter pipelineState;
        PrimitiveTopologyRenderingParameter primitiveTopology;
        RenderTargetRenderingParameter renderTarget;
        TexturesRenderingParameter textures;
        VertexShaderRenderingParameter vertexShader;
        PixelShaderRenderingParameter pixelShader;

        void Clear()
        {
            pipelineState.Unset();
            primitiveTopology.Unset();
            renderTarget.Unset();
            textures.Unset();
            vertexShader.Unset();
            pixelShader.Unset();
        }
    } mRuntimeParametersStash;

    // Graphics Pipeline
    PipelineStateRenderingParameter mPipelineState;
    PrimitiveTopologyRenderingParameter mPrimitiveTopology;
    RenderTargetRenderingParameter mRenderTarget;
    ScissorRenderingParameter mScissor; // used when explicitly set by updateClipRect()
    ScissorRenderingParameter mDefaultScissor; // used when scissor testing is disabled
    ViewportRenderingParameter mViewport;

    TexturesRenderingParameter mTextures;
    VertexShaderRenderingParameter mVertexShader;
    PixelShaderRenderingParameter mPixelShader;
    VertexShaderConstantsRenderingParameter mVertexShaderConstants;
    PixelShaderConstantsRenderingParameter mPixelShaderConstants;

    inline ScissorRenderingParameter& GetScissor()
    {
        if (mScissor.IsSet()) return mScissor;
        else return mDefaultScissor;
    }

    // Compute Pipeline
    ComputePipelineStateRenderingParameter mComputePipelineState;
    ComputeShaderRenderingParameter mComputeShader;
    ComputeShaderConstantsRenderingParameter mComputeShaderConstants;

    // Used RTTs for finish-frame-time BBox invalidation
    // Prism can "juggle" the RTTs between frames, so our dirty-bbox optimization can
    // only apply for one frame - afterwards we need to invalidate those bboxes and
    // start tracking anew
    std::unordered_set<NIPtr<Internal::IRenderTarget>> mUsedRTs;

    RenderThread::RenderPayloadPtr ReplaceRTPayload(); // creates new payload, returns old one
    bool SubmitRTPayload();
    bool AddToRTPayload(RenderThread::RenderThreadExecutablePtr&& executable);
    template <typename T, typename Executable>
    bool AddToRTPayload(RenderingParameter<T, Executable>& parameter);
    void RecordClear(float r, float g, float b, float a, bool clearDepth, const D3D12_RECT& clearRect);
    BBox EstimateDirtyBBox(const MemoryView<float>& vertices, uint32_t vertexCount, const Internal::Matrix<float>& worldTransform);
    void ClearAppliedFlags();

public:
    RenderingContext(const NIPtr<NativeDevice>& nativeDevice);
    ~RenderingContext() = default;

    bool Init();
    void Release();

    void Dispose(const D3D12PageablePtr& pageable);
    void Dispose(const NIPtr<Internal::ITrackedResource>& resource);

    bool Apply();
    bool ApplyCompute();
    void Clear(float r, float g, float b, float a, bool clearDepth);
    void ClearDepth(const NIPtr<NativeTexture>& depthTexture, const D3D12_CPU_DESCRIPTOR_HANDLE& dsv);
    void DrawQuads(const Internal::MemoryView<float>& vertices, const Internal::MemoryView<unsigned char>& colors, uint32_t vertexCount, const Internal::Matrix<float>& worldTransform);
    void DrawMeshView(const NIPtr<NativeMeshView>& meshView);
    void Dispatch(uint32_t x, uint32_t y, uint32_t z);
    bool PrepareSwapChain(const NIPtr<NativeSwapChain>& swapChain, const D3D12_RECT& dirtyRegion);
    bool Present(const NIPtr<NativeSwapChain>& swapChain);

    void Resolve(const NIPtr<ITrackedResource>& dstTexture, const NIPtr<ITrackedResource>& srcTexture, DXGI_FORMAT resolveFormat);
    void ResolveRegion(const NIPtr<ITrackedResource>& dstTexture, uint32_t dstx, uint32_t dsty,
                       const NIPtr<ITrackedResource>& srcTexture, uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch,
                       DXGI_FORMAT resolveFormat);
    void ResolveRegion(const NIPtr<IRenderTarget>& dstRT, uint32_t dstx, uint32_t dsty,
                       const NIPtr<TextureBase>& srcTexture, uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch,
                       DXGI_FORMAT resolveFormat);
    void CopyTexture(const NIPtr<ITrackedResource>& dstTexture, uint32_t dstx, uint32_t dsty,
                     const NIPtr<ITrackedResource>& srcTexture, uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch);
    void CopyTexture(const NIPtr<IRenderTarget>& dstRT, uint32_t dstx, uint32_t dsty,
                     const NIPtr<TextureBase>& srcTexture, uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch);
    void CopyTextureToBuffer(const NIPtr<Buffer>& dstBuffer, uint32_t dstStride, const NIPtr<NativeTexture>& srcTexture,
                             uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch);
    bool UpdateTexture(const NIPtr<NativeTexture>& dstTexture, uint32_t dstx, uint32_t dsty,
                       const void* srcData, size_t srcDataBytes, uint32_t srcx, uint32_t srcy,
                       uint32_t srcw, uint32_t srch, uint32_t srcStride, PixelFormat srcFormat);
    void CopyBufferRegion(const D3D12ResourcePtr& dst, uint64_t dstOffset, const D3D12ResourcePtr& src, uint64_t srcOffset, uint64_t size);
    void CopyResource(const D3D12ResourcePtr& dst, const D3D12ResourcePtr& src);
    bool GenerateMipmaps(const NIPtr<NativeTexture>& texture);

    void TransitionTrackedResource(const NIPtr<ITrackedResource>& resource, D3D12_RESOURCE_STATES newState, uint32_t subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);
    void TransitionResource(const D3D12ResourcePtr& resource,  D3D12_RESOURCE_STATES oldState, D3D12_RESOURCE_STATES newState, uint32_t subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);

    void ClearTextureUnit(uint32_t unit);
    void UnsetRenderTargetIfSet(IRenderTarget* rt);
    void SetRenderTarget(const NIPtr<IRenderTarget>& renderTarget);
    void SetScissor(bool enabled, const D3D12_RECT& scissor);
    void SetTexture(uint32_t unit, const NIPtr<TextureBase>& texture);

    void SetCompositeMode(CompositeMode mode);
    void SetCullMode(D3D12_CULL_MODE mode);
    void SetFillMode(D3D12_FILL_MODE mode);
    void SetVertexShader(const NIPtr<Shaders::Shader>& vertexShader);
    void SetPixelShader(const NIPtr<Shaders::Shader>& pixelShader);
    void SetComputeShader(const NIPtr<Shaders::Shader>& computeShader);

    void StashParamters();
    void RestoreStashedParameters();

    bool FlushAndWait(CheckpointType type);
    bool WaitForNextCheckpoint(CheckpointType type);
    bool WaitForGPU();
    void FinishFrame();

    // exposed for SwapChain
    inline const D3D12CommandQueuePtr& GetCommandQueue()
    {
        return mRenderThread.GetCommandQueue();
    }

    inline void WaitUntilIdle()
    {
        mRenderThread.WaitUntilIdle();
    }

    inline void RegisterWaitableOperation(RenderThread::IWaitableOperation* waitableOp)
    {
        mRenderThread.RegisterWaitableOperation(waitableOp);
    }

    inline void UnregisterWaitableOperation(RenderThread::IWaitableOperation* waitableOp)
    {
        mRenderThread.UnregisterWaitableOperation(waitableOp);
    }
};

} // namespace Internal
} // namespace D3D12
