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

#include "D3D12RingBuffer.hpp"

#include "../D3D12NativeDevice.hpp"

#include "internal/D3D12Utils.hpp"
#include "internal/D3D12Debug.hpp"


namespace D3D12 {
namespace RenderThread {

RingBuffer::RingBuffer(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback)
    : RingContainer(nativeDevice, flushCallback, waitCallback)
    , mBufferResource()
    , mCPUPtr(nullptr)
    , mGPUPtr(0)
{
}

RingBuffer::~RingBuffer()
{
    if (mBufferResource)
    {
        if (mCPUPtr)
        {
            D3D12_RANGE range = { 0, 0 };
            mBufferResource->Unmap(0, &range);
        }

        mBufferResource.Reset();
    }
}

bool RingBuffer::Init(size_t flushThreshold, size_t alignment, size_t size)
{
    if (!InitInternal(flushThreshold, alignment, size)) return false;

    // create the Resource which will represent our Ring Buffer
    D3D12_RESOURCE_DESC resourceDesc;
    D3D12NI_ZERO_STRUCT(resourceDesc);
    resourceDesc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    resourceDesc.Width = static_cast<UINT64>(mSize);
    resourceDesc.Height = 1;
    resourceDesc.DepthOrArraySize = 1;
    resourceDesc.MipLevels = 1;
    resourceDesc.Format = DXGI_FORMAT_UNKNOWN;
    resourceDesc.Flags = D3D12_RESOURCE_FLAG_NONE;
    resourceDesc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    resourceDesc.SampleDesc.Count = 1;
    resourceDesc.SampleDesc.Quality = 0;

    // Ring Buffer is a CPU-accessible passthrough to GPU for small data,
    // hence it will always be an Upload Heap type resource
    D3D12_HEAP_PROPERTIES heapProps;
    D3D12NI_ZERO_STRUCT(heapProps);
    heapProps.Type = D3D12_HEAP_TYPE_UPLOAD;

    HRESULT hr = mNativeDevice->GetDevice()->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE,
        &resourceDesc, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&mBufferResource));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Buffer's Committed Resource");

    void* cpu;
    D3D12_RANGE range = { 0, 0 }; // this range means the CPU won't read any data
    hr = mBufferResource->Map(0, &range, &cpu);
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to Map ring buffer to acquire CPU pointer");

    mBufferResource->SetName(L"Ring Buffer Resource");

    mCPUPtr = reinterpret_cast<uint8_t*>(cpu);
    mGPUPtr = mBufferResource->GetGPUVirtualAddress();

    return true;
}

RingBuffer::Region RingBuffer::Reserve(size_t size)
{
    RingContainer::Region region = ReserveInternal(size);
    if (region.size == 0) return RingBuffer::Region();

    return RingBuffer::Region(mCPUPtr + region.offsetFromStart, mGPUPtr + region.offsetFromStart, region.size, region.offsetFromStart);
}

void RingBuffer::SetDebugName(const std::string& name)
{
    RingContainer::SetDebugName(name);

    if (mBufferResource)
    {
        mBufferResource->SetName(Internal::Utils::ToWString(name).c_str());
    }
}

} // namespace RenderThread
} // namespace D3D12
