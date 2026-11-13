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

#include "internal/D3D12DescriptorData.hpp"
#include "internal/D3D12TextureBase.hpp"
#include "render-thread/D3D12RingBuffer.hpp"


#include <map>
#include <string>


namespace D3D12 {
namespace Shaders {

/**
 * Common interface for all Shaders. Unifies some common functionalities
 * which will be used during rendering - see D3D12ResourceManager.hpp.
 *
 * Note that it is mostly only a container for Shader's bytecode. Shader
 * bytecode is compiled when a PSO is created.
 */
class Shader
{
public:
    using ConstantBuffer = std::vector<uint8_t>;

    struct ResourceData
    {
        uint32_t textureCount = 0;
        uint32_t uavCount = 0;
        uint32_t cbufferDTableCount = 0;    // amount of constant buffers that are accessed via a DTable
                                            // Directly-written descriptrors should NOT count towards this number
        size_t cbufferDTableSingleSize = 0; // size of a single entry in CBV DTable;
        size_t cbufferDirectSize = 0;       // size of a directly-written descriptor
    };

    struct DescriptorData
    {
        Internal::DescriptorData SRVDescriptors;                // Descirptor Table for SRVs
        Internal::DescriptorData UAVDescriptors;                // Descriptor Table for UAVs
        Internal::DescriptorData SamplerDescriptors;            // Descriptor Table for Samplers (allocated separately on Sampler heap)
        Internal::DescriptorData CBufferTableDescriptors;       // Descriptor Table for constants, if requested
        RenderThread::RingBuffer::Region ConstantDataDTableRegions; // Region for all DTable constant data, which is as big as:
                                                                    //   align(cbufferDTableSingleSize, CONSTANT_BUFFER_DATA_ALIGNMENT) * cbufferDTableCount
        RenderThread::RingBuffer::Region ConstantDataDirectRegion;  // Region for direct constant data, which is as big as:
                                                                    //   align(cbufferDirectSize, CONSTANT_BUFFER_DATA_ALIGNMENT)
    };

protected:
    struct ResourceAssignment
    {
        ResourceAssignmentType type; // where our resource was assigned to
        uint32_t rootIndex; // at which root signature index is our resource
        uint32_t index; // at which index in dtable is our resource - only valid for DESCRIPTOR_TABLE types
        uint32_t elementCount; // how many elements our assignment has; non-arrays have this set to one, arrays to element count
        uint32_t sizeInCBStorage; // size in storage in bytes per element
        uint32_t offsetInCBStorage; // at which spot in mConstantBufferStorage our data should be kept

        ResourceAssignment(ResourceAssignmentType type, uint32_t rootIndex, uint32_t index, uint32_t elementCount, uint32_t sizeInCBStorage, uint32_t offsetInCBStorage)
            : type(type)
            , rootIndex(rootIndex)
            , index(index)
            , elementCount(elementCount)
            , sizeInCBStorage(sizeInCBStorage)
            , offsetInCBStorage(offsetInCBStorage)
        {}
    };

    using ResourceAssignmentCollection = std::unordered_map<std::string_view, ResourceAssignment>;

    std::string mName;
    ShaderPipelineMode mMode;
    D3D12_SHADER_VISIBILITY mVisibility;
    D3D12_SHADER_BYTECODE mBytecode;
    std::vector<uint8_t> mBytecodeBuffer;
    ConstantBuffer mConstantBufferStorage;
    ResourceAssignmentCollection mShaderResourceAssignments;
    ResourceData mResourceData;
    DescriptorData mDescriptorData;
    bool mConstantsDirty;

    void SetConstantBufferData(void* data, size_t size, size_t storageOffset);
    void AddShaderResource(const char* name, const ResourceAssignment& resource);

public:
    Shader();

    virtual bool Init(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize);
    bool SetConstants(const std::string_view& name, const void* data, size_t size);
    bool SetConstantsInArray(const std::string_view& name, uint32_t idx, const void* data, size_t size);

    virtual bool PrepareDescriptors(const Internal::TextureBank& textures, void* data, size_t size) = 0;
    virtual void ApplyDescriptors(const D3D12GraphicsCommandListPtr& commandList) const = 0;

    // Inline getters
    inline const std::string& GetName() const
    {
        return mName;
    }

    inline ShaderPipelineMode GetMode() const
    {
        return mMode;
    }

    inline D3D12_SHADER_VISIBILITY GetVisibility() const
    {
        return mVisibility;
    }

    inline const D3D12_SHADER_BYTECODE& GetBytecode() const
    {
        return mBytecode;
    }

    // created during Init(), accessed by RenderThread's ResourceManager to allocate Descriptors on Heaps
    inline const ResourceData& GetResourceData() const
    {
        return mResourceData;
    }

    // created during Init(), set by QuantumRenderer (aka main thread)
    // RenderingContext will access this from the main thread to copy its contents for RenderThread
    inline const ConstantBuffer& GetConstantStorage() const
    {
        return mConstantBufferStorage;
    }

    // Shader-specific Descriptor storage, managed by RenderThread
    inline DescriptorData& GetDescriptorData()
    {
        return mDescriptorData;
    }

    inline bool AreConstantsDirty() const
    {
        return mConstantsDirty;
    }

    inline void SetConstantsDirty(bool dirty)
    {
        mConstantsDirty = dirty;
    }
};

} // namespace Shaders
} // namespace D3D12
