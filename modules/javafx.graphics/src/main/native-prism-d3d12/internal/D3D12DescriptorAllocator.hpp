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

#include "D3D12Common.hpp"
#include "D3D12DescriptorHeap.hpp"

#include <unordered_map>
#include <mutex>


namespace D3D12 {
namespace Internal {

class DescriptorAllocator
{
    NIPtr<NativeDevice> mNativeDevice;
    std::list<DescriptorHeap> mHeaps;
    std::list<DescriptorHeap> mEmptiedHeaps;
    DescriptorHeap* mCurrentHeap;
    uint32_t mHeapCounter; // for debug naming
    D3D12_DESCRIPTOR_HEAP_TYPE mType;
    bool mShaderVisible;
    std::mutex mHeapAccessMutex;
    std::string mName;

    bool AddHeap();

public:
    DescriptorAllocator(const NIPtr<NativeDevice>& nativeDevice);
    ~DescriptorAllocator() = default;

    bool Init(D3D12_DESCRIPTOR_HEAP_TYPE type, bool shaderVisible);
    DescriptorData Allocate(uint32_t count);
    void Free(const DescriptorData& data);
    void SetName(const std::string& name);
};

} // namespace Internal
} // namespace D3D12
