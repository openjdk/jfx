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
#include "../D3D12Constants.hpp"

#include "D3D12IWaitableOperation.hpp"
#include "D3D12RingBuffer.hpp"
#include "D3D12RingDescriptorHeap.hpp"

#include "internal/D3D12LinearAllocator.hpp"
#include "internal/D3D12SamplerStorage.hpp"
#include "internal/D3D12TextureBase.hpp"

#include "shaders/D3D12Shader.hpp"

#include <unordered_map>


namespace D3D12 {
namespace RenderThread {

struct SamplerBindingIdentifier
{
    Internal::SamplerDescArray descs;

    inline bool operator==(const SamplerBindingIdentifier& other) const
    {
        for (uint32_t i = 0; i < Constants::MAX_TEXTURE_UNITS; ++i)
        {
            if (descs[i] != other.descs[i]) return false;
        }
        return true;
    }
};

} // namespace RenderThread
} // namespace D3D12

template<>
struct std::hash<D3D12::RenderThread::SamplerBindingIdentifier>
{
    std::size_t operator()(const D3D12::RenderThread::SamplerBindingIdentifier& k) const
    {
        static_assert(
            (D3D12::Internal::SamplerDesc::TOTAL_BITS * D3D12::Constants::MAX_TEXTURE_UNITS) <= (sizeof(std::size_t) * 8),
            "Too many sampler settings used or too many texture units could potentially be used. "
            "Consider lowering those or rewriting the hashing functions"
        );

        std::size_t result = 0;
        for (uint32_t i = 0; i < D3D12::Constants::MAX_TEXTURE_UNITS; ++i)
        {
            result <<= D3D12::Internal::SamplerDesc::TOTAL_BITS;
            result |= std::hash<D3D12::Internal::SamplerDesc>()(k.descs[i]);
        }
        return result;
    }
};

namespace D3D12 {
namespace RenderThread {

class ResourceManager: public IWaitableOperation
{
public:
    struct ShaderConstants
    {
        // NOTE: This temporary buffer will technically be freed when Executable is freed. However, the
        // free happens when RenderPayload gets freed and deallocates all steps added to it.
        // Because we ensure that each Payload executes all steps needed to draw/dispatch, ResourceManager
        // will consume these pointers while they're still allocated, making this safe to use.
        size_t size;
        std::unique_ptr<void, Internal::LinearAllocatorDeleter<void>> buffer;

        ShaderConstants()
            : size(0)
            , buffer(nullptr, Internal::LinearAllocatorDeleter<void>(nullptr))
        {}

        ShaderConstants(Internal::LinearAllocator& allocator, const void* srcData, size_t srcSize)
            : size(srcSize)
            , buffer(allocator.Allocate(static_cast<uint32_t>(size)), Internal::LinearAllocatorDeleter<void>(&allocator))
        {
            if (buffer) memcpy(buffer.get(), srcData, srcSize);
        }

        ShaderConstants(const ShaderConstants& other) = delete;
        ShaderConstants& operator=(const ShaderConstants& other) = delete;

        ShaderConstants(ShaderConstants&& other)
            : size(other.size)
            , buffer(std::move(other.buffer))
        {
        }

        ShaderConstants& operator=(ShaderConstants&& other)
        {
            size = other.size;
            buffer = std::move(other.buffer);
            return *this;
        }
    };

private:
    struct RuntimeParametersStash
    {
        NIPtr<Shaders::Shader> vertexShader;
        NIPtr<Shaders::Shader> pixelShader;
        Internal::TextureBank textures;
        Internal::SamplerDescArray samplerDescs;
    } mRuntimeParametersStash;

    struct ShaderConstantsData
    {
        bool dirty;
        ShaderConstants constants;
    };

    NIPtr<NativeDevice> mNativeDevice;
    NIPtr<Shaders::Shader> mVertexShader;
    NIPtr<Shaders::Shader> mPixelShader;
    ShaderConstantsData mVertexShaderConstants;
    ShaderConstantsData mPixelShaderConstants;
    Internal::TextureBank mTextures;
    Internal::SamplerStorage mSamplerStorage;
    RingDescriptorHeap mDescriptorHeap;
    RingDescriptorHeap mSamplerHeap;
    RingBuffer mConstantRingBuffer;
    SamplerBindingIdentifier mCurrentSamplerBinding;
    std::unordered_map<SamplerBindingIdentifier, Internal::DescriptorData> mLastSamplerDescriptors;
    uint32_t mSamplerRegionReserveProfilerID;

    // Compute Resources
    NIPtr<Shaders::Shader> mComputeShader;
    ShaderConstantsData mComputeShaderConstants;

    void UpdateTextureDescriptorTable(const Internal::DescriptorData& dtable);
    bool PrepareConstants(const NIPtr<Shaders::Shader>& shaderResourceData, ShaderConstantsData& constants);
    bool PrepareTextureViews(const NIPtr<Shaders::Shader>& shaderResourceData);
    bool PrepareSamplers(const NIPtr<Shaders::Shader>& shaderResourceData);
    bool PrepareShaderResources(const NIPtr<Shaders::Shader>& shader, ShaderConstantsData& constants);

public:
    // callbacks are provided by RenderThread
    ResourceManager(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback);
    ~ResourceManager();

    bool Init();
    void DeclareRingResources();
    void DeclareComputeRingResources();
    bool PrepareResources();
    bool PrepareComputeResources();
    void ApplyResources(const D3D12GraphicsCommandListPtr& commandList) const;
    void ApplyComputeResources(const D3D12GraphicsCommandListPtr& commandList) const;
    void ClearTextureUnit(uint32_t slot);
    void SetVertexShader(const NIPtr<Shaders::Shader>& shader);
    void SetPixelShader(const NIPtr<Shaders::Shader>& shader);
    void SetComputeShader(const NIPtr<Shaders::Shader>& shader);
    void SetVertexShaderConstants(ShaderConstants&& constants);
    void SetPixelShaderConstants(ShaderConstants&& constants);
    void SetComputeShaderConstants(ShaderConstants&& constants);
    void SetTextures(const Internal::TextureBank& bank, const Internal::SamplerDescArray& samplers);
    void SetTexture(uint32_t slot, const NIPtr<Internal::TextureBase>& tex, const Internal::SamplerDesc& samplerDesc);
    void FinishFrame();

    void StashParameters();
    void RestoreStashedParameters();

    virtual void OnQueueSignal(uint64_t) override;
    virtual void OnFenceSignaled(uint64_t) override;

    inline const NIPtr<Internal::TextureBase>& GetTexture(uint32_t slot) const
    {
        return mTextures[slot];
    }

    inline const D3D12DescriptorHeapPtr& GetHeap() const
    {
        return mDescriptorHeap.GetHeap();
    }

    inline const D3D12DescriptorHeapPtr& GetSamplerHeap() const
    {
        return mSamplerHeap.GetHeap();
    }
};

} // namespace RenderThread
} // namespace D3D12
