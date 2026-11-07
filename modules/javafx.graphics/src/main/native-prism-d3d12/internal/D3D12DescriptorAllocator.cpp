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

#include "D3D12DescriptorAllocator.hpp"

#include "../D3D12NativeDevice.hpp"

#include "D3D12Debug.hpp"


namespace D3D12 {
namespace Internal {

bool DescriptorAllocator::AddHeap()
{
    if (!mEmptiedHeaps.empty())
    {
        // reuse existing heaps if they were fully freed
        mHeaps.emplace_back(std::move(mEmptiedHeaps.front()));
        mEmptiedHeaps.pop_front();
        mCurrentHeap = &mHeaps.back();
        return true;
    }

    // allocate the new heap since we don't have any to reuse
    D3D12_DESCRIPTOR_HEAP_DESC desc;
    D3D12NI_ZERO_STRUCT(desc);
    desc.NumDescriptors = DescriptorHeap::MAX_DESCRIPTOR_SLOT_COUNT;
    desc.Type = mType;
    desc.Flags = (mShaderVisible ? D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE : D3D12_DESCRIPTOR_HEAP_FLAG_NONE);
    desc.NodeMask = 0;

    D3D12DescriptorHeapPtr heap;
    HRESULT hr = mNativeDevice->GetDevice()->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&heap));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to allocate new Descriptor Heap");

    uint32_t increment = mNativeDevice->GetDevice()->GetDescriptorHandleIncrementSize(mType);

    mHeaps.emplace_back(DescriptorHeap(heap, increment, mHeapCounter, mName));
    mCurrentHeap = &mHeaps.back();
    ++mHeapCounter;

    return true;
}

DescriptorAllocator::DescriptorAllocator(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mHeaps()
    , mEmptiedHeaps()
    , mCurrentHeap(nullptr)
    , mHeapCounter(0)
    , mType(D3D12_DESCRIPTOR_HEAP_TYPE_NUM_TYPES)
    , mShaderVisible(false)
    , mHeapAccessMutex()
    , mName("Descriptor Heap")
{
}

bool DescriptorAllocator::Init(D3D12_DESCRIPTOR_HEAP_TYPE type, bool shaderVisible)
{
    mType = type;
    mShaderVisible = shaderVisible;

    return AddHeap();
}

DescriptorData DescriptorAllocator::Allocate(uint32_t count)
{
    if (count > DescriptorHeap::MAX_DESCRIPTOR_SLOT_COUNT)
    {
        D3D12NI_LOG_ERROR("Tried to allocate too many descriptors (%d, max allowed %d)", count, DescriptorHeap::MAX_DESCRIPTOR_SLOT_COUNT);
        return DescriptorData();
    }

    std::unique_lock<std::mutex> lock(mHeapAccessMutex);

    DescriptorData data = mCurrentHeap->Allocate(count);
    if (!data)
    {
        D3D12NI_LOG_TRACE("Current heap must be full or too fragmented, advancing to a new one");
        if (!AddHeap())
        {
            D3D12NI_LOG_ERROR("Failed to add new Descriptor Heap for allocation of %d descriptors", count);
            return DescriptorData();
        }

        // retry the allocation
        data = mCurrentHeap->Allocate(count);
        if (!data)
        {
            D3D12NI_LOG_ERROR("Failed to allocate %d descriptors", count);
            return DescriptorData();
        }
    }

    return data;
}

void DescriptorAllocator::Free(const DescriptorData& data)
{
    std::unique_lock<std::mutex> lock(mHeapAccessMutex);

    if (mCurrentHeap->Owns(data))
    {
        mCurrentHeap->Free(data);
        return;
    }

    for (std::list<DescriptorHeap>::iterator it = mHeaps.begin(); it != mHeaps.end(); ++it)
    {
        if (it->Owns(data))
        {
            it->Free(data);
            if (it->Empty())
            {
                mEmptiedHeaps.emplace_back(std::move(*it));
                mHeaps.erase(it);
            }

            return;
        }
    }

    D3D12NI_ASSERT(false, "Tried to free descriptor data that does not belong to this allocator");
}

void DescriptorAllocator::SetName(const std::string& name)
{
    mName = name;
    for (auto& heap: mHeaps)
    {
        heap.SetName(name);
    }
}

} // namespace Internal
} // namespace D3D12
