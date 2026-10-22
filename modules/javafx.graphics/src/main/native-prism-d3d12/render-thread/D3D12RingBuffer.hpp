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


namespace D3D12 {
namespace RenderThread {

class RingBuffer: public RingContainer
{
public:
    struct Region
    {
        void* cpu;
        D3D12_GPU_VIRTUAL_ADDRESS gpu;
        size_t size;
        size_t offsetFromStart;

        Region()
            : Region(nullptr, 0, 0, 0)
        {}

        Region(void* c, D3D12_GPU_VIRTUAL_ADDRESS g, size_t s, size_t o)
            : cpu(c)
            , gpu(g)
            , size(s)
            , offsetFromStart(o)
        {}

        inline operator bool() const
        {
            return (cpu != 0);
        }

        inline Region Subregion(size_t offset, size_t size)
        {
            D3D12NI_ASSERT(offset >= 0 && size > 0, "Invalid Subregion parameters requested");
            D3D12NI_ASSERT(offset + size <= this->size, "Invalid Subregion parameters requested");

            return Region((uint8_t*)cpu + offset, gpu + offset, size, offsetFromStart + offset);
        }
    };

protected:
    D3D12ResourcePtr mBufferResource;
    uint8_t* mCPUPtr;
    D3D12_GPU_VIRTUAL_ADDRESS mGPUPtr;

public:
    RingBuffer(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback);
    virtual ~RingBuffer();

    /**
     * Initializes Ring Buffer with predefined @p size in bytes and preset
     * @p flushThreshold
     *
     * @p flushThreshold determines when Ring Buffer will trigger a Command List
     * flush on the attached device. Threshold is determined in bytes. If provided
     * threshold is larger than @p size it will be forced to @p size.
     *
     * If @p size is 0 bytes (aka. its default value) the size will be calculated
     * to 3 times the size of @p flushThreshold to implement "triple-buffering" of
     * mid-frame data, which seemed to provide the best results. Realistically,
     * the only RingContainer-based class that needs a custom size should be the
     * Sampler Heap.
     *
     * @p alignment has to be one of:
     *   - 256 (D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) - CBuffer data
     *   - 4KB (D3D12_SMALL_RESOURCE_PLACEMENT_ALIGNMENT) - small Textures
     *   - 64KB (D3D12_SMALL_MSAA_RESOURCE_PLACEMENT_ALIGNMENT) - small MSAA Textures
     *   - 64KB (D3D12_DEFAULT_RESOURCE_PLACEMENT_ALIGNMENT) - Buffers and Textures
     *   - 4MB (D3D12_DEFAULT_MSAA_RESOURCE_PLACEMENT_ALIGNMENT) - MSAA Textures
     *
     * Since the Ring Buffer should be as oblivious as possible to the Resource type being
     * allocated, it makes the most sense for us to provide the alignment per-reservation.
     * For more info on alignment and what "small Textures" are see:
     *    https://learn.microsoft.com/en-us/windows/win32/api/d3d12/ns-d3d12-d3d12_resource_desc#alignment
     */
    bool Init(size_t flushThreshold, size_t alignment, size_t size = 0);

    /**
     * Requests @p size bytes of space from the Ring Buffer which should
     * be aligned to @p alignment bytes.
     *
     * Returns a Region struct which contains both a CPU and a GPU pointer
     * to allocated spac, its size and offset from the start.
     *
     * If there is an error during reservation for some reason, returns a Region
     * struct filled with zeros (null CPU, null GPU, 0 size).
     */
    Region Reserve(size_t size);

    void SetDebugName(const std::string& name);

    inline const D3D12ResourcePtr& GetResource() const
    {
        return mBufferResource;
    }
};

} // namespace RenderThread
} // namespace D3D12
