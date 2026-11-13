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
#include "D3D12Constants.hpp"

#include "internal/D3D12DescriptorData.hpp"
#include "internal/D3D12TextureBase.hpp"


namespace D3D12 {

class NativeTexture: public Internal::TextureBase
{
    static uint64_t textureCounter;
    static uint64_t depthTextureCounter;
    static uint64_t rttextureCounter;

    NIPtr<NativeDevice> mNativeDevice;
    UINT mMipLevels;
    Internal::DescriptorData mSRVDescriptor;

    bool InitInternal(const D3D12_RESOURCE_DESC& desc);

public:
    NativeTexture(const NIPtr<NativeDevice>& nativeDevice);
    ~NativeTexture();

    bool Init(UINT width, UINT height, DXGI_FORMAT format, D3D12_RESOURCE_FLAGS flags,
              TextureUsage usage, TextureWrapMode wrapMode, int samples, bool useMipmap);
    UINT64 GetSize();
    bool Resize(UINT width, UINT height);
    void SetSamplerParameters(TextureWrapMode wrapMode, bool isLinear);

    virtual void WriteSRVToDescriptor(const D3D12_CPU_DESCRIPTOR_HANDLE& descriptorCpu, UINT mipLevels = 0, UINT mostDetailedMip = 0) override;
    virtual void WriteUAVToDescriptor(const D3D12_CPU_DESCRIPTOR_HANDLE& descriptorCpu, UINT mipSlice) override;

    inline const NIPtr<NativeDevice>& GetDevice() const
    {
        return mNativeDevice;
    }

    inline UINT GetMipLevels() const
    {
        return mMipLevels;
    }

    inline bool HasMipmaps() const
    {
        return (mMipLevels > 1);
    }
};

} // namespace D3D12
