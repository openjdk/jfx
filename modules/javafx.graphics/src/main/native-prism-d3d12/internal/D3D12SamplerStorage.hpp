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

#pragma once

#include "../D3D12Common.hpp"

#include "D3D12DescriptorAllocator.hpp"
#include "D3D12SamplerDesc.hpp"

#include <unordered_map>


namespace D3D12 {
namespace Internal {

class SamplerStorage
{
    NIPtr<NativeDevice> mNativeDevice;
    DescriptorAllocator mSamplerAllocator;
    std::unordered_map<SamplerDesc, DescriptorData> mSamplerContainer;
    DescriptorData mNullSampler;

    D3D12_SAMPLER_DESC BuildD3D12SamplerDesc(const SamplerDesc& sd) const;
    D3D12_TEXTURE_ADDRESS_MODE TranslateWrapMode(TextureWrapMode wrapMode) const;
    D3D12_FILTER TranslateIsLinear(bool isLinear) const;

public:
    SamplerStorage(const NIPtr<NativeDevice>& nativeDevice);
    ~SamplerStorage() = default;

    bool Init();
    const DescriptorData& GetSampler(const SamplerDesc& sd) const;
};

} // namespace Internal
} // namespace D3D12
