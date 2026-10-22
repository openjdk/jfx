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

#include "D3D12DescriptorHeap.hpp"

#include "../D3D12NativeDevice.hpp"

#include "D3D12Debug.hpp"
#include "D3D12Utils.hpp"


namespace D3D12 {
namespace Internal {

DescriptorHeap::DescriptorHeap(const D3D12DescriptorHeapPtr& heap, uint32_t incrementSize, uint32_t id, const std::string& name)
    : mHeap(heap)
    , mShaderVisible(false)
    , mCPUStartHandle{0}
    , mGPUStartHandle{0}
    , mIncrementSize(incrementSize)
    , mSlotAvailability()
    , mFirstFreeSlot(0)
    , mSize(0)
    , mAllocatedCountTotal(0)
    , mID(id)
    , mName(name)
{
    D3D12_DESCRIPTOR_HEAP_DESC desc = mHeap->GetDesc();

    mShaderVisible = (desc.Flags & D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE);
    mSize = desc.NumDescriptors;

    mCPUStartHandle = mHeap->GetCPUDescriptorHandleForHeapStart();
    if (mShaderVisible) mGPUStartHandle = mHeap->GetGPUDescriptorHandleForHeapStart();

    for (uint32_t i = 0; i < MAX_DESCRIPTOR_SLOT_COUNT; ++i) mSlotAvailability[i] = true;
}

DescriptorHeap::DescriptorHeap(DescriptorHeap&& other)
    : mHeap(std::move(other.mHeap))
    , mShaderVisible(other.mShaderVisible)
    , mCPUStartHandle(other.mCPUStartHandle)
    , mGPUStartHandle(other.mGPUStartHandle)
    , mIncrementSize(other.mIncrementSize)
    , mSlotAvailability(std::move(other.mSlotAvailability))
    , mFirstFreeSlot(other.mFirstFreeSlot)
    , mSize(other.mSize)
    , mAllocatedCountTotal(other.mAllocatedCountTotal)
    , mID(other.mID)
    , mName(std::move(other.mName))
{
    other.mCPUStartHandle.ptr = 0;
    other.mGPUStartHandle.ptr = 0;
    other.mIncrementSize = 0;
    other.mFirstFreeSlot = 0;
    other.mSize = 0;
    other.mAllocatedCountTotal = 0;
    other.mID = std::numeric_limits<uint32_t>::max();
}

DescriptorData DescriptorHeap::Allocate(UINT count)
{
    if (count > mSlotAvailability.size() - mAllocatedCountTotal)
    {
        D3D12NI_LOG_DEBUG("Too many descriptors requested for alloc");
        return DescriptorData();
    }

    size_t i = mFirstFreeSlot;

    do
    {
        // if we don't have enough slots till the end of the array to allocate requested
        // descriptor count, skip those and continue searching from the beginning
        if ((mSlotAvailability.size() - i) < count)
        {
            i = 0;
            continue;
        }

        bool enoughSpace = true;
        for (size_t x = i; x < i + count; ++x)
        {
            if (mSlotAvailability[x] == false)
            {
                // not enough space here, continue on
                i = x + 1;
                enoughSpace = false;
                break;
            }
        }

        if (enoughSpace)
        {
            D3D12_CPU_DESCRIPTOR_HANDLE retCPUHandle = mCPUStartHandle;
            retCPUHandle.ptr += i * mIncrementSize;

            D3D12_GPU_DESCRIPTOR_HANDLE retGPUHandle = mGPUStartHandle;
            if (mShaderVisible)
            {
                // increment GPU handle only if our Heap is shader-visible
                // otherwise we should keep it as NULL
                retGPUHandle.ptr += i * mIncrementSize;
            }

            // mark slots as used and update counters
            for (size_t x = i; x < i + count; ++x)
            {
                mSlotAvailability[x] = false;
            }
            mFirstFreeSlot = i + count;

            mAllocatedCountTotal += count;
            D3D12NI_LOG_TRACE("%s: Allocated %u descriptors, %u/%u taken", mName.c_str(), count, mAllocatedCountTotal, mSize);

            return DescriptorData(retCPUHandle, retGPUHandle, count, mIncrementSize, mID);
        }
    }
    while (i != mFirstFreeSlot);

    D3D12NI_LOG_WARN("Failed to find enough room to allocate %u descriptors", count);
    return DescriptorData();
}

void DescriptorHeap::Free(const DescriptorData& data)
{
    D3D12NI_ASSERT(data.allocatorId == mID, "Tried to free descriptor data block which does not belong to this allocator");

    size_t slot = (data.cpu.ptr - mCPUStartHandle.ptr) / mIncrementSize;
    size_t finalSlot = slot + data.count;

    uint32_t freed = 0;
    for (slot; slot < finalSlot; ++slot)
    {
        mSlotAvailability[slot] = true;
        freed++;
    }

    mAllocatedCountTotal -= freed;
    D3D12NI_LOG_TRACE("%s: Freed %u descriptors, %u/%u taken", mName.c_str(), freed, mAllocatedCountTotal, mSize);
}

void DescriptorHeap::SetName(const std::string& name)
{
    mName = name + "_" + std::to_string(mID);
    mHeap->SetName(Utils::ToWString(name).c_str());
}

} // namespace Internal
} // namespace D3D12
