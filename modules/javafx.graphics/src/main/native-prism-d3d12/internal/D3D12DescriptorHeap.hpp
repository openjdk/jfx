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

#include "D3D12DescriptorData.hpp"

#include <array>


namespace D3D12 {
namespace Internal {

class DescriptorHeap
{
public:
    static constexpr uint32_t MAX_DESCRIPTOR_SLOT_COUNT = 2048;

private:
    D3D12DescriptorHeapPtr mHeap;
    bool mShaderVisible;
    uint32_t mSize;
    D3D12_CPU_DESCRIPTOR_HANDLE mCPUStartHandle;
    D3D12_GPU_DESCRIPTOR_HANDLE mGPUStartHandle;
    uint32_t mIncrementSize;

    std::array<bool, MAX_DESCRIPTOR_SLOT_COUNT> mSlotAvailability;
    size_t mFirstFreeSlot;
    uint32_t mAllocatedCountTotal;
    uint32_t mID;
    std::string mName;

public:
    DescriptorHeap(const D3D12DescriptorHeapPtr& heap, UINT incrementSize, uint32_t id, const std::string& name);
    DescriptorHeap(DescriptorHeap&& other);
    ~DescriptorHeap() = default;

    DescriptorHeap(const DescriptorHeap& other) = delete;
    DescriptorHeap& operator=(const DescriptorHeap& other) = delete;

    DescriptorData Allocate(UINT count);
    void Free(const DescriptorData& data);

    void SetName(const std::string& name);

    inline bool Owns(const DescriptorData& data) const
    {
        return data.allocatorId == mID;
    }

    inline const D3D12DescriptorHeapPtr& GetHeap() const
    {
        return mHeap;
    }

    inline bool Empty() const
    {
        return (mAllocatedCountTotal == 0);
    }
};

} // namespace Internal
} // namespace D3D12
