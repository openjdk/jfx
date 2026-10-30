/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12SamplerStorage.hpp"

#include "D3D12NativeDevice.hpp"

#include "../D3D12Constants.hpp"


namespace {

std::vector<D3D12::Internal::SamplerDesc> SAMPLER_VARIANTS = {
    { D3D12::TextureWrapMode::CLAMP_NOT_NEEDED, false },
    { D3D12::TextureWrapMode::CLAMP_TO_ZERO, false },
    { D3D12::TextureWrapMode::CLAMP_TO_EDGE, false },
    { D3D12::TextureWrapMode::REPEAT, false },
    { D3D12::TextureWrapMode::CLAMP_NOT_NEEDED, true },
    { D3D12::TextureWrapMode::CLAMP_TO_ZERO, true },
    { D3D12::TextureWrapMode::CLAMP_TO_EDGE, true },
    { D3D12::TextureWrapMode::REPEAT, true },
};

} // namespace


namespace D3D12 {
namespace Internal {

D3D12_SAMPLER_DESC SamplerStorage::BuildD3D12SamplerDesc(const SamplerDesc& sd) const
{
    D3D12_TEXTURE_ADDRESS_MODE addressMode = TranslateWrapMode(sd.wrapMode);

    D3D12_SAMPLER_DESC desc;
    D3D12NI_ZERO_STRUCT(desc);
    desc.AddressU = addressMode;
    desc.AddressV = addressMode;
    desc.AddressW = addressMode;
    desc.Filter = TranslateIsLinear(sd.isLinear);
    desc.BorderColor[0] = 0.0f;
    desc.BorderColor[1] = 0.0f;
    desc.BorderColor[2] = 0.0f;
    desc.BorderColor[3] = 0.0f;
    desc.MinLOD = 0;
    desc.MaxLOD = D3D12_FLOAT32_MAX;
    desc.MipLODBias = 0;
    desc.MaxAnisotropy = 1;
    desc.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;

    return desc;
}

D3D12_TEXTURE_ADDRESS_MODE SamplerStorage::TranslateWrapMode(TextureWrapMode wrapMode) const
{
    switch (wrapMode)
    {
    case TextureWrapMode::CLAMP_NOT_NEEDED:
    case TextureWrapMode::CLAMP_TO_ZERO:
        return D3D12_TEXTURE_ADDRESS_MODE_BORDER;
    case TextureWrapMode::CLAMP_TO_EDGE:
        return D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    case TextureWrapMode::REPEAT:
        return D3D12_TEXTURE_ADDRESS_MODE_WRAP;
    default:
        return D3D12_TEXTURE_ADDRESS_MODE_BORDER;
    }
}

D3D12_FILTER SamplerStorage::TranslateIsLinear(bool isLinear) const
{
    if (isLinear) return D3D12_FILTER_MIN_MAG_MIP_LINEAR;
    else return D3D12_FILTER_MIN_MAG_MIP_POINT;
}

SamplerStorage::SamplerStorage(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mSamplerAllocator(nativeDevice)
{
}

bool SamplerStorage::Init()
{
    if (!mSamplerAllocator.Init(D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER, false))
    {
        D3D12NI_LOG_ERROR("Failed to initialize Sampler Heap");
        return false;
    }

    for (const SamplerDesc& sd: SAMPLER_VARIANTS)
    {
        D3D12_SAMPLER_DESC desc = BuildD3D12SamplerDesc(sd);

        DescriptorData descriptor = mSamplerAllocator.Allocate(1);
        if (!descriptor)
        {
            D3D12NI_LOG_ERROR("Failed to allocate Sampler for variant: %s", sd.ToString().c_str());
            return false;
        }

        mNativeDevice->GetDevice()->CreateSampler(&desc, descriptor.CPU(0));

        mSamplerContainer.emplace(sd, descriptor);
    }

    return true;
}

const DescriptorData& SamplerStorage::GetSampler(const SamplerDesc& sd) const
{
    const auto& it = mSamplerContainer.find(sd);
    if (it == mSamplerContainer.end()) {
        D3D12NI_LOG_WARN("Requested unknown sampler desc: %s", sd.ToString().c_str());
        return mNullSampler;
    }

    return it->second;
}

} // namespace Internal
} // namespace D3D12
