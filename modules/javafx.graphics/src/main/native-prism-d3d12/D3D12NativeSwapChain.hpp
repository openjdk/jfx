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

#include "D3D12Common.hpp"

#include "internal/D3D12IRenderTarget.hpp"
#include "internal/D3D12TextureBase.hpp"

#include "render-thread/D3D12IWaitableOperation.hpp"
#include "render-thread/D3D12RenderThreadContext.hpp"

#include <vector>
#include <queue>
#include <mutex>
#include <condition_variable>


namespace D3D12 {

// TODO: D3D12: This class is a bit of a mixture between main thread code and RenderThread...
// Should probably be separated into separate objects - NativeSwapChain to interact with Java
// and an internal SwapChain payload with all stuff used by RenderThread.
class NativeSwapChain: public Internal::IRenderTarget, RenderThread::IWaitableOperation
{
    static uint64_t swapChainCounter;

    NIPtr<NativeDevice> mNativeDevice;
    std::string mDebugName;
    DXGISwapChainPtr mSwapChain;
    std::vector<NIPtr<Internal::TextureBase>> mTextureBuffers;
    std::vector<D3D12_RESOURCE_STATES> mTextureStates;
    std::vector<Internal::DescriptorData> mRTVs;
    std::queue<uint64_t> mWaitFenceValues; // for Render Thread-side tracking and synchronization
    std::condition_variable mAvailableBufferCV;
    std::mutex mAvailableBufferMutex;
    uint32_t mRecordedPresentCount; // for main thread-side tracking
    uint32_t mProfilerSourceID;
    UINT mBufferCount;
    UINT mCurrentBufferIdx;
    RECT mDirtyRegion;
    DXGI_FORMAT mFormat;
    bool mVSyncEnabled;
    UINT mSwapChainFlags;
    UINT mSwapInterval;
    UINT mPresentFlags;
    UINT mWidth;
    UINT mHeight;

    // for GetDepthResource()
    NIPtr<Internal::TextureBase> mNullTexture;

    bool GetSwapChainBuffers(UINT count);

public:
    NativeSwapChain(const NIPtr<NativeDevice>& nativeDevice);
    ~NativeSwapChain();

    bool Init(const DXGIFactoryPtr& factory, HWND hwnd);
    void Release();
    bool Resize(UINT width, UINT height);
    void WaitForAvailableBuffer();

    // runs on Render Thread
    bool Prepare(const D3D12_RECT& dirtyRegion);
    bool Present(const RenderThread::RenderThreadContextPtr& context);

    inline const D3D12ResourcePtr& GetBuffer(int index) const
    {
        return mTextureBuffers[index]->GetD3D12Resource();
    }

    inline UINT GetBufferCount() const
    {
        return static_cast<UINT>(mBufferCount);
    }

    inline UINT GetCurrentBufferIndex() const
    {
        return mCurrentBufferIdx;
    }

    // ITrackedResource overrides

    inline const D3D12ResourcePtr& GetD3D12Resource() const override final
    {
        return GetBuffer(GetCurrentBufferIndex());
    }

    inline D3D12_RESOURCE_STATES GetD3D12ResourceState(uint32_t subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES) const override final
    {
        // SwapChain only has one subresource, ignore the parameter
        D3D12NI_ASSERT(subresource == D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES, "SwapChain only has a single subresource.");
        return GetTexture()->GetD3D12ResourceState(subresource);
    }

    inline void SetD3D12ResourceState(D3D12_RESOURCE_STATES newState, uint32_t subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES) override final
    {
        // SwapChain only has one subresource, ignore the parameter
        D3D12NI_ASSERT(subresource == D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES, "SwapChain only has a single subresource.");
        GetTexture()->SetD3D12ResourceState(newState, subresource);
    }

    inline bool NeedsStateTransitions() const override final
    {
        return true; // SwapChain buffers are Default-heap textures
    }


    // IWaitableOperation overrides

    // runs on Render Thread
    void OnQueueSignal(uint64_t fenceValue) override;
    void OnFenceSignaled(uint64_t fenceValue) override;


    // IRenderTarget overrides

    inline const NIPtr<Internal::TextureBase>& GetTexture() const override
    {
        return mTextureBuffers[GetCurrentBufferIndex()];
    }

    inline const NIPtr<Internal::TextureBase>& GetDepthTexture() const override
    {
        D3D12NI_ASSERT(false, "NativeSwapChain has no depth texture. This should not happen.");
        return mNullTexture;
    }

    inline DXGI_FORMAT GetFormat() const
    {
        return mFormat;
    }

    inline uint64_t GetWidth() const override
    {
        return mWidth;
    }

    inline uint64_t GetHeight() const override
    {
        return mHeight;
    }

    inline bool HasDepthTexture() const
    {
        return false;
    }

    inline bool IsDepthTestEnabled() const
    {
        return false;
    }

    inline uint32_t GetMSAASamples() const
    {
        return 1;
    }

    inline const Internal::DescriptorData& GetRTVDescriptorData() const
    {
        return mRTVs[GetCurrentBufferIndex()];
    }

    inline const Internal::DescriptorData& GetDSVDescriptorData() const
    {
        return Internal::DescriptorData::NULL_DESCRIPTOR;
    }

};

} // namespace D3D12
