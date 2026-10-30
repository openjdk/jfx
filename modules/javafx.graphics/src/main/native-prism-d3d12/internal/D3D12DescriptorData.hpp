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


namespace D3D12 {
namespace Internal {

struct DescriptorData
{
    static DescriptorData NULL_DESCRIPTOR;

    D3D12_CPU_DESCRIPTOR_HANDLE cpu; // CPU pointer to start of available descriptors
    D3D12_GPU_DESCRIPTOR_HANDLE gpu; // GPU pointer to start of available descriptors
    UINT count; // how many descriptors we can take
    size_t singleSize; // by how much increase the pointer to reach further descriptors
    uint32_t allocatorId; // which allocator/heap this data belongs to

    DescriptorData()
        : DescriptorData(0, 0, 0, 0, 0)
    {}

    DescriptorData(SIZE_T cpuPtr, D3D12_GPU_VIRTUAL_ADDRESS gpuPtr, UINT c, size_t single, uint32_t allocatorId)
        : cpu{cpuPtr}
        , gpu{gpuPtr}
        , count(c)
        , singleSize(single)
        , allocatorId(allocatorId)
    {}

    DescriptorData(D3D12_CPU_DESCRIPTOR_HANDLE cpuHandle, D3D12_GPU_DESCRIPTOR_HANDLE gpuHandle, UINT c, size_t single, uint32_t allocatorId)
        : cpu(cpuHandle)
        , gpu(gpuHandle)
        , count(c)
        , singleSize(single)
        , allocatorId(allocatorId)
    {}

    static DescriptorData Form(SIZE_T cpuStart, D3D12_GPU_VIRTUAL_ADDRESS gpuStart, UINT offset, UINT count, size_t singleSize, uint32_t allocatorId)
    {
        SIZE_T offsetBytes = offset * singleSize;
        SIZE_T cpu = cpuStart + offsetBytes;
        D3D12_GPU_VIRTUAL_ADDRESS gpu = (gpuStart > 0) ? gpuStart + offsetBytes : 0;
        return DescriptorData(cpu, gpu, count, singleSize, allocatorId);
    }

    inline D3D12_CPU_DESCRIPTOR_HANDLE CPU(UINT i) const
    {
        D3D12NI_ASSERT(i < count, "Requested descriptor handle is too big");
        return { cpu.ptr + (i * singleSize) };
    }

    inline D3D12_CPU_DESCRIPTOR_HANDLE CPU(int i) const
    {
        D3D12NI_ASSERT(i >= 0, "Requested descriptor handle cannot be negative");
        return CPU(static_cast<UINT>(i));
    }

    inline D3D12_GPU_DESCRIPTOR_HANDLE GPU(UINT i) const
    {
        D3D12NI_ASSERT(i < count, "Requested descriptor handle is too big");
        D3D12NI_ASSERT(gpu.ptr > 0, "Descriptor is not shader-visible, GPU pointer should not be accessed");
        return { (gpu.ptr > 0) ? (gpu.ptr + (i * singleSize)) : 0 };
    }

    inline D3D12_GPU_DESCRIPTOR_HANDLE GPU(int i) const
    {
        D3D12NI_ASSERT(i >= 0, "Requested descriptor handle cannot be negative");
        return GPU(static_cast<UINT>(i));
    }

    // creates a separate "sub-DescriptorData" object out of one of selected descriptors
    // useful for ex. picking a single RTV from a SwapChain
    inline DescriptorData Single(UINT i) const
    {
        return Range(i, 1);
    }

    inline DescriptorData Range(UINT from, UINT amount) const
    {
        D3D12NI_ASSERT(from < count, "Requested Descriptor range \"from\" is too big - from %u available %u", from, count);
        D3D12NI_ASSERT(from + amount <= count, "Requested Descriptor range (from + amount) crosses boundaries - from %u amount %u available %u", from, amount, count);

        if (gpu.ptr > 0)
        {
            return DescriptorData(CPU(from), GPU(from), amount, singleSize, allocatorId);
        }
        else
        {
            return DescriptorData(CPU(from), {0}, amount, singleSize, allocatorId);
        }
    }

    inline operator bool() const
    {
        // only checks for cpu.ptr, singleSize and count
        // it can happen that the Descriptor is CPU-only - then gpu.ptr can be 0
        return (cpu.ptr != 0 && singleSize != 0 && count != 0);
    }
};

} // namespace Internal
} // namespace D3D12
