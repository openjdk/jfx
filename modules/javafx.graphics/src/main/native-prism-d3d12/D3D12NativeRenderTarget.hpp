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

#include "D3D12NativeSwapChain.hpp"
#include "D3D12NativeTexture.hpp"

#include "internal/D3D12DescriptorData.hpp"
#include "internal/D3D12IRenderTarget.hpp"
#include "internal/D3D12TextureBase.hpp"


namespace D3D12 {

class NativeRenderTarget: public Internal::IRenderTarget
{
    NIPtr<NativeDevice> mNativeDevice;
    NIPtr<NativeTexture> mTexture;
    NIPtr<NativeTexture> mDepthTexture;
    NIPtr<Internal::TextureBase> mTextureBase;
    NIPtr<Internal::TextureBase> mDepthTextureBase;
    Internal::DescriptorData mDescriptors;
    Internal::DescriptorData mDSVDescriptor;
    uint64_t mWidth;
    uint64_t mHeight;
    bool mDepthTestEnabled;

public:
    NativeRenderTarget(const NIPtr<NativeDevice>& nativeDevice);
    ~NativeRenderTarget();

    bool Init(const NIPtr<NativeTexture>& texture, bool enableDirtyBBox);
    bool EnsureHasDepthBuffer();
    bool Refresh();
    void SetDepthTestEnabled(bool enabled);
    uint64_t GetSize() const;

    inline const NIPtr<NativeDevice>& GetDevice() const
    {
        return mNativeDevice;
    }

    // ITrackedResource overrides

    inline const D3D12ResourcePtr& GetD3D12Resource() const override final
    {
        return GetTexture()->GetD3D12Resource();
    }

    inline D3D12_RESOURCE_STATES GetD3D12ResourceState(uint32_t subresource) const override final
    {
        return GetTexture()->GetD3D12ResourceState(subresource);
    }

    inline void SetD3D12ResourceState(D3D12_RESOURCE_STATES newState, uint32_t subresource) override final
    {
        GetTexture()->SetD3D12ResourceState(newState, subresource);
    }

    inline bool NeedsStateTransitions() const override final
    {
        return true; // RTTs base on Default-heap Textures
    }


    // IRenderTarget overrides

    inline const NIPtr<Internal::TextureBase>& GetTexture() const override
    {
        return mTextureBase;
    }

    inline const NIPtr<Internal::TextureBase>& GetDepthTexture() const override
    {
        return mDepthTextureBase;
    }

    inline DXGI_FORMAT GetFormat() const override
    {
        return mTexture->GetFormat();
    }

    inline uint64_t GetWidth() const override
    {
        return mWidth;
    }

    inline uint64_t GetHeight() const override
    {
        return mHeight;
    }

    inline bool IsDepthTestEnabled() const override
    {
        return mDepthTestEnabled;
    }

    inline bool HasDepthTexture() const override
    {
        return (mDepthTexture != nullptr);
    }

    inline const Internal::DescriptorData& GetRTVDescriptorData() const override
    {
        return mDescriptors;
    }

    inline const Internal::DescriptorData& GetDSVDescriptorData() const override
    {
        return mDSVDescriptor;
    }

    inline uint32_t GetMSAASamples() const override
    {
        return mTexture->GetMSAASamples();
    }
};

} // namespace D3D12
