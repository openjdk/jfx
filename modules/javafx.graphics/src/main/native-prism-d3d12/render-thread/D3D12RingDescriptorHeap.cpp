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

#include "D3D12RingDescriptorHeap.hpp"

#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Utils.hpp"

#include "../D3D12NativeDevice.hpp"


namespace {

static const wchar_t* TranslateDescriptorHeapTypeToString(D3D12_DESCRIPTOR_HEAP_TYPE type)
{
    switch (type)
    {
    case D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV: return L"CBV_SRV_UAV";
    case D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER: return L"SAMPLER";
    case D3D12_DESCRIPTOR_HEAP_TYPE_RTV: return L"RTV";
    case D3D12_DESCRIPTOR_HEAP_TYPE_DSV: return L"DSV";
    default: return L"UNKNOWN";
    }
}

} // namespace

namespace D3D12 {
namespace RenderThread {

RingDescriptorHeap::RingDescriptorHeap(const NIPtr<NativeDevice>& device, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback)
    : RingContainer(device, flushCallback, waitCallback)
    , mHeap()
    , mShaderVisible(false)
    , mIncrementSize(0)
{
}

bool RingDescriptorHeap::Init(D3D12_DESCRIPTOR_HEAP_TYPE type, bool shaderVisible, UINT flushThreshold, UINT size)
{
    if (!InitInternal(flushThreshold, 1, size)) return false;

    D3D12_DESCRIPTOR_HEAP_DESC desc;
    D3D12NI_ZERO_STRUCT(desc);
    desc.Type = type;
    desc.NumDescriptors = static_cast<UINT>(mSize);
    desc.Flags = shaderVisible ? D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE : D3D12_DESCRIPTOR_HEAP_FLAG_NONE;
    // TODO: D3D12: for multi-adapters, we need to set below to non-zero.
    // See: https://learn.microsoft.com/en-us/windows/win32/api/d3d12/ns-d3d12-d3d12_descriptor_heap_desc
    desc.NodeMask = 0;

    HRESULT hr = mNativeDevice->GetDevice()->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&mHeap));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Descriptor Heap");

    if (Internal::Debug::Instance().IsEnabled())
    {
        std::wstring name(L"Ring Descriptor Heap - ");
        name += TranslateDescriptorHeapTypeToString(type);
        mHeap->SetName(name.c_str());
    }

    mShaderVisible = shaderVisible;
    mCPUHeapStart = mHeap->GetCPUDescriptorHandleForHeapStart();
    mIncrementSize = mNativeDevice->GetDevice()->GetDescriptorHandleIncrementSize(type);

    if (mShaderVisible)
    {
        mGPUHeapStart = mHeap->GetGPUDescriptorHandleForHeapStart();
    }
    else
    {
        mGPUHeapStart.ptr = 0;
    }

    return true;
}

Internal::DescriptorData RingDescriptorHeap::Reserve(size_t count)
{
    Region r = ReserveInternal(count);
    if (r.size == 0) return Internal::DescriptorData();

    return Internal::DescriptorData::Form(mCPUHeapStart.ptr, mGPUHeapStart.ptr, static_cast<UINT>(r.offsetFromStart), static_cast<UINT>(count), mIncrementSize, 0);
}

void RingDescriptorHeap::SetDebugName(const std::string& name)
{
    RingContainer::SetDebugName(name);

    if (mHeap)
    {
        mHeap->SetName(Internal::Utils::ToWString(name).c_str());
    }
}


} // namespace RenderThread
} // namespace D3D12
