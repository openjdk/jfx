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

#include "D3D12RingContainer.hpp"

#include "internal/D3D12DescriptorData.hpp"


namespace D3D12 {
namespace RenderThread {

/*
 * Ring Descriptor Heap follows similar principles as a Ring Buffer, but
 * applies them to D3D12's Descriptor Heaps.
 *
 * In some cases we need to dynamically allocate batches of Descriptor Heaps
 * which are only used during the lifetime of a Command List. Ring Descriptor
 * Heap is aiming to solve that problem.
 *
 * Logic behind Ring Descriptor Heap is a bit simpler than in Ring Buffer, as
 * it operates on same-size descriptors and has no need for memory alignment
 * requirements.
 */
class RingDescriptorHeap: public RingContainer
{
    D3D12DescriptorHeapPtr mHeap;
    D3D12_CPU_DESCRIPTOR_HANDLE mCPUHeapStart;
    D3D12_GPU_DESCRIPTOR_HANDLE mGPUHeapStart;
    bool mShaderVisible;
    UINT mIncrementSize;

public:
    RingDescriptorHeap(const NIPtr<NativeDevice>& device, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback);
    ~RingDescriptorHeap() = default;

    /**
     * Initializes Ring Descriptor Heap of given type and with given shader visibility.
     * @p size and @p flushThreshold should be measured in descriptor count.
     *
     * If @p size is set to 0 (aka. its default value) the total Container size will be
     * calculated to 3 times the @p flushThreshold to imitate "triple-buffering" of
     * mid-frame Descriptors which proved to be the most efficient. A different @p size
     * should realistically only be necessary when initializing the Sampler Heap which
     * has a hard-limited size by D3D12 API.
     */
    bool Init(D3D12_DESCRIPTOR_HEAP_TYPE type, bool shaderVisible, UINT flushThreshold, UINT size = 0);

    /*
     * Reserve @p count amount of Descriptors on the heap and get back the Data structure
     */
    Internal::DescriptorData Reserve(size_t count);

    void SetDebugName(const std::string& name);

    const D3D12DescriptorHeapPtr& GetHeap() const
    {
        return mHeap;
    }

};

} // namespace RenderThread
} // namespace D3D12
