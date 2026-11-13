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

#include "D3D12ResourceManager.hpp"

#include "../D3D12NativeDevice.hpp"

#include "D3D12Config.hpp"
#include "D3D12Profiler.hpp"
#include "D3D12Utils.hpp"


namespace D3D12 {
namespace RenderThread {

bool ResourceManager::PrepareConstants(const NIPtr<Shaders::Shader>& shader, ShaderConstantsData& constants)
{
    // quietly exit early if constants do not need to be re-prepared
    if (!constants.dirty) return true;

    const Shaders::Shader::ResourceData& resourceData = shader->GetResourceData();
    Shaders::Shader::DescriptorData& descriptors = shader->GetDescriptorData();

    if (resourceData.cbufferDirectSize > 0)
    {
        descriptors.ConstantDataDirectRegion = mConstantRingBuffer.Reserve(resourceData.cbufferDirectSize);
        if (!descriptors.ConstantDataDirectRegion)
        {
            D3D12NI_LOG_ERROR("Failed to reserve Constant Ring Buffer space for direct constant data");
            return false;
        }
    }

    if (resourceData.cbufferDTableCount > 0)
    {
        D3D12NI_ASSERT(resourceData.cbufferDTableSingleSize > 0, "Requested CBV DTable allocation, yet single size is zero");

        size_t singleCBufferSizeAligned = Internal::Utils::Align<size_t>(resourceData.cbufferDTableSingleSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT);
        descriptors.ConstantDataDTableRegions = mConstantRingBuffer.Reserve(singleCBufferSizeAligned * resourceData.cbufferDTableCount);
        if (!descriptors.ConstantDataDTableRegions)
        {
            D3D12NI_LOG_ERROR("Failed to reserve Constant Ring Buffer space for descriptor table constant data of size %zd", singleCBufferSizeAligned * resourceData.cbufferDTableCount);
            return false;
        }

        descriptors.CBufferTableDescriptors = mDescriptorHeap.Reserve(resourceData.cbufferDTableCount);
        if (!descriptors.CBufferTableDescriptors)
        {
            D3D12NI_LOG_ERROR("Failed to reserve Ring Descriptor Heap space for CBV DTable of size %d", resourceData.cbufferDTableCount);
            return false;
        }

        // Descriptor Table must be populated with CBViews, we have their locations and sizes so we will do that right now
        // Shader will later populate the Ring Buffer regions with constant data, but the CBViews can be created now
        for (uint32_t i = 0; i < resourceData.cbufferDTableCount; ++i)
        {
            size_t offset = i * singleCBufferSizeAligned;

            D3D12_CONSTANT_BUFFER_VIEW_DESC cbvDesc;
            D3D12NI_ZERO_STRUCT(cbvDesc);
            cbvDesc.BufferLocation = descriptors.ConstantDataDTableRegions.gpu + offset;
            cbvDesc.SizeInBytes = static_cast<UINT>(singleCBufferSizeAligned); // this has to be size aligned to 256 bytes, not the actual (smaller) unaligned size
            mNativeDevice->GetDevice()->CreateConstantBufferView(&cbvDesc, descriptors.CBufferTableDescriptors.CPU(i));
        }
    }

    constants.dirty = false;
    return true;
}

bool ResourceManager::PrepareTextureViews(const NIPtr<Shaders::Shader>& shader)
{
    const Shaders::Shader::ResourceData& resourceData = shader->GetResourceData();
    Shaders::Shader::DescriptorData& descriptors = shader->GetDescriptorData();

    if (resourceData.textureCount > 0)
    {
        descriptors.SRVDescriptors = mDescriptorHeap.Reserve(resourceData.textureCount);

        for (uint32_t i = 0; i < resourceData.textureCount; ++i)
        {
            // NULL textures will be auto-populated with null descriptors to prevent UB
            // We don't do other writes because of how Shaders might want to bind the textures
            // ex. MipmapGenComputeShader will bind subresources instead of Textures as a whole
            if (!mTextures[i])
            {
                D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc;
                D3D12NI_ZERO_STRUCT(srvDesc);
                srvDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
                srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
                srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
                mNativeDevice->GetDevice()->CreateShaderResourceView(nullptr, &srvDesc, descriptors.SRVDescriptors.CPU(i));
            }
        }
    }

    if (resourceData.uavCount > 0)
    {
        descriptors.UAVDescriptors = mDescriptorHeap.Reserve(resourceData.uavCount);
    }

    return true;
}

bool ResourceManager::PrepareSamplers(const NIPtr<Shaders::Shader>& shader)
{
    const Shaders::Shader::ResourceData& resourceData = shader->GetResourceData();
    Shaders::Shader::DescriptorData& descriptors = shader->GetDescriptorData();

    // Reserve samplers and write them if needed
    if (resourceData.textureCount > 0)
    {
        const auto& it = mLastSamplerDescriptors.find(mCurrentSamplerBinding);
        if (it == mLastSamplerDescriptors.cend())
        {
            Internal::Profiler::Instance().MarkEvent(mSamplerRegionReserveProfilerID, Internal::Profiler::Event::Event);
            Internal::DescriptorData descs = mSamplerHeap.Reserve(Constants::MAX_TEXTURE_UNITS);

            for (uint32_t i = 0; i < Constants::MAX_TEXTURE_UNITS; ++i)
            {
                const Internal::DescriptorData& sampler = mSamplerStorage.GetSampler(mCurrentSamplerBinding.descs[i]);

                mNativeDevice->GetDevice()->CopyDescriptorsSimple(
                    1, descs.CPU(i), sampler.CPU(0), D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER
                );
            }

            mLastSamplerDescriptors.emplace(std::make_pair(mCurrentSamplerBinding, descs));
            descriptors.SamplerDescriptors = descs;
        }
        else
        {
            descriptors.SamplerDescriptors = it->second;
        }
    }

    return true;
}

bool ResourceManager::PrepareShaderResources(const NIPtr<Shaders::Shader>& shader, ShaderConstantsData& constants)
{
    // We need to allocate space on respective heaps here:
    //  - Reserve RingBuffer space for CBVs, allocate and create CBV Descriptors for CBV DTable (if needed)
    //  - Allocate SRV Descriptors (don't create SRVs! those will be created by PrepareDescriptors() depending on how each shader uses them)
    //     - We need a way to inform the shaders to skip the SRV Descriptor write if there is no change in textures
    //  - Allocate Sampler descriptors, but only if they're needed
    //     - We need a way to inform the shaders to skip the Sampler Descriptor write
    // All these steps need to be done only if necessary:
    //  - Calling SetTexture() should dirty the SRV update and independently check if Samplers are dirty too
    //  - Setting Shader constants should dirty the Constant Data
    //     - Maybe we should do the constants set via ResourceManager? What about NativeShader API though...
    if (!PrepareConstants(shader, constants)) return false;
    if (!PrepareTextureViews(shader)) return false;
    if (!PrepareSamplers(shader)) return false;

    // we provide mTextures to Shader mostly because of each shader accessing Texture (sub)resources differently
    // ex. MipmapGenComputeShader wants to populate Texture's subresources when generating mip levels instead of
    // simply viewing Textures as a whole resource, including all of its subresources.
    // As such, it makes more sense to let Shaders decide how to write Views onto Descriptors we just prepared for them.
    if (!shader->PrepareDescriptors(mTextures, constants.constants.buffer.get(), constants.constants.size)) return false;

    return true;
}

ResourceManager::ResourceManager(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback)
    : mNativeDevice(nativeDevice)
    , mVertexShader()
    , mPixelShader()
    , mTextures()
    , mSamplerStorage(nativeDevice)
    , mDescriptorHeap(nativeDevice, flushCallback, waitCallback)
    , mSamplerHeap(nativeDevice, flushCallback, waitCallback)
    , mConstantRingBuffer(nativeDevice, flushCallback, waitCallback)
    , mCurrentSamplerBinding()
    , mLastSamplerDescriptors()
    , mSamplerRegionReserveProfilerID(UINT32_MAX)
{
    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(this);
    mSamplerRegionReserveProfilerID = Internal::Profiler::Instance().RegisterSource("ResourceManager Sampler Region Reserve");
}

ResourceManager::~ResourceManager()
{
    for (uint32_t i = 0; i < Constants::MAX_TEXTURE_UNITS; ++i)
    {
        mTextures[i].reset();
        mRuntimeParametersStash.textures[i].reset();
    }

    mVertexShader.reset();
    mPixelShader.reset();
    mRuntimeParametersStash.vertexShader.reset();
    mRuntimeParametersStash.pixelShader.reset();

    mNativeDevice->GetRenderingContext()->UnregisterWaitableOperation(this);
    mNativeDevice.reset();

    D3D12NI_LOG_DEBUG("ResourceManager destroyed");
}

bool ResourceManager::Init()
{
    if (!mSamplerStorage.Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize Sampler Storage");
        return false;
    }

    if (!mDescriptorHeap.Init(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV, true, Internal::Config::SRVRingHeapThreshold()))
    {
        D3D12NI_LOG_ERROR("Failed to initialize main Ring Descriptor Heap");
        return false;
    }

    // Maximum limit of Samplers is 2048
    //    https://learn.microsoft.com/en-us/windows/win32/direct3d12/hardware-support
    //    https://learn.microsoft.com/en-us/windows/win32/direct3d12/hardware-feature-levels
    // We use the max limit and raise the flush threshold to 3/4th of 2048 in order to
    // potentially never hit it (but to still have it as a backup - better to finish drawing
    // a frame slower than to completely fail).
    // If Profiler shows Sampler Heap is causing Command List flushing, there must be
    // more optimization work done to avoid it.
    if (!mSamplerHeap.Init(D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER, true, 1536, 2048))
    {
        D3D12NI_LOG_ERROR("Failed to initialize Sampler Ring Descriptor Heap");
        return false;
    }

    if (!mConstantRingBuffer.Init(Internal::Config::ConstantRingBufferThreshold(), D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT))
    {
        D3D12NI_LOG_ERROR("Failed to initialize constant data Ring Buffer");
        return false;
    }

    mDescriptorHeap.SetDebugName("CBV/SRV/UAV Descriptor Heap");
    mSamplerHeap.SetDebugName("Sampler Heap");
    mConstantRingBuffer.SetDebugName("Constant Ring Buffer");

    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(&mDescriptorHeap);
    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(&mSamplerHeap);
    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(&mConstantRingBuffer);

    return true;
}

void ResourceManager::DeclareRingResources()
{
    Shaders::Shader::ResourceData vsData = mVertexShader->GetResourceData();
    Shaders::Shader::ResourceData psData = mPixelShader->GetResourceData();

    // constant data vs
    size_t totalConstantDataSize =
        Internal::Utils::Align<size_t>(vsData.cbufferDirectSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) +
        Internal::Utils::Align<size_t>(vsData.cbufferDTableSingleSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) * vsData.cbufferDTableCount;

    // constant data ps
    totalConstantDataSize +=
        Internal::Utils::Align<size_t>(psData.cbufferDirectSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) +
        Internal::Utils::Align<size_t>(psData.cbufferDTableSingleSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) * psData.cbufferDTableCount;

    mConstantRingBuffer.DeclareRequired(totalConstantDataSize);

    // descriptors
    size_t totalDescriptorCount =
        vsData.textureCount + vsData.uavCount + vsData.cbufferDTableCount +
        psData.textureCount + psData.uavCount + psData.cbufferDTableCount;
    mDescriptorHeap.DeclareRequired(totalDescriptorCount);

    // NOTE: Samplers should never cross the threshold. This is a slower failsafe to keep rendering.
    const auto& checkNeededSamplerSlots = [this](const Shaders::Shader::ResourceData resData) -> uint32_t
    {
        if (resData.textureCount > 0)
        {
            const auto& it = mLastSamplerDescriptors.find(mCurrentSamplerBinding);
            if (it == mLastSamplerDescriptors.end())
            {
                // we will need to allocate 4 sampler slots
                return Constants::MAX_TEXTURE_UNITS;
            }
        }

        // no textures needed or sampler binding already exists on the heap
        return 0;
    };

    uint32_t neededSamplers = checkNeededSamplerSlots(vsData) + checkNeededSamplerSlots(psData);
    if (mSamplerHeap.DeclareRequired(neededSamplers))
    {
        // notify the failsafe was triggered - maybe samplers require more optimizaiton
        D3D12NI_LOG_WARN("ResourceManager: Sampler Heap flushed the command list. This might be slow, frequent flushes like these would require some more optimization to Sampler usage.");
    }
}

// Assumes it is called only if attached shader resources change or we switch to a new Command List
bool ResourceManager::PrepareResources()
{
    // collect how many slots of each Ring Container we will need and
    if (!PrepareShaderResources(mVertexShader, mVertexShaderConstants)) return false;
    if (!PrepareShaderResources(mPixelShader, mPixelShaderConstants)) return false;

    return true;
}

void ResourceManager::ApplyResources(const D3D12GraphicsCommandListPtr& commandList) const
{
    mVertexShader->ApplyDescriptors(commandList);
    mPixelShader->ApplyDescriptors(commandList);
}

void ResourceManager::DeclareComputeRingResources()
{
    Shaders::Shader::ResourceData csData = mComputeShader->GetResourceData();

    // constant data
    size_t totalConstantDataSize =
        Internal::Utils::Align<size_t>(csData.cbufferDirectSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) +
        Internal::Utils::Align<size_t>(csData.cbufferDTableSingleSize, D3D12_CONSTANT_BUFFER_DATA_PLACEMENT_ALIGNMENT) * csData.cbufferDTableCount;

    mConstantRingBuffer.DeclareRequired(totalConstantDataSize);

    // descriptors
    size_t totalDescriptorCount = csData.textureCount + csData.uavCount;
    mDescriptorHeap.DeclareRequired(totalDescriptorCount);
}

bool ResourceManager::PrepareComputeResources()
{
    return PrepareShaderResources(mComputeShader, mComputeShaderConstants);
}

void ResourceManager::ApplyComputeResources(const D3D12GraphicsCommandListPtr& commandList) const
{
    mComputeShader->ApplyDescriptors(commandList);
}

void ResourceManager::ClearTextureUnit(uint32_t slot)
{
    D3D12NI_ASSERT(slot < Constants::MAX_TEXTURE_UNITS, "Provided too high slot %u (max %u)", slot, Constants::MAX_TEXTURE_UNITS);

    mTextures[slot].reset();
}

void ResourceManager::SetVertexShader(const NIPtr<Shaders::Shader>& shader)
{
    if (shader == mVertexShader) return;

    if (!shader)
    {
        mVertexShader.reset();
        return;
    }

    mVertexShader = shader;
}

void ResourceManager::SetPixelShader(const NIPtr<Shaders::Shader>& shader)
{
    if (shader == mPixelShader) return;

    if (!shader)
    {
        mPixelShader.reset();
        return;
    }

    mPixelShader = shader;
}

void ResourceManager::SetComputeShader(const NIPtr<Shaders::Shader>& shader)
{
    if (shader == mComputeShader) return;

    if (!shader)
    {
        mComputeShader.reset();
        return;
    }

    mComputeShader = shader;
}

void ResourceManager::SetVertexShaderConstants(ShaderConstants&& constants)
{
    mVertexShaderConstants.dirty = true;
    mVertexShaderConstants.constants = std::move(constants);
}

void ResourceManager::SetPixelShaderConstants(ShaderConstants&& constants)
{
    mPixelShaderConstants.dirty = true;
    mPixelShaderConstants.constants = std::move(constants);
}

void ResourceManager::SetComputeShaderConstants(ShaderConstants&& constants)
{
    mComputeShaderConstants.dirty = true;
    mComputeShaderConstants.constants = std::move(constants);
}


void ResourceManager::SetTextures(const Internal::TextureBank& bank, const Internal::SamplerDescArray& samplers)
{
    for (uint32_t i = 0; i < bank.size(); ++i)
    {
        SetTexture(i, bank[i], samplers[i]);
    }
}

void ResourceManager::SetTexture(uint32_t slot, const NIPtr<Internal::TextureBase>& tex, const Internal::SamplerDesc& samplerDesc)
{
    D3D12NI_ASSERT(slot < Constants::MAX_TEXTURE_UNITS, "Provided too high slot %u (max %u)", slot, Constants::MAX_TEXTURE_UNITS);

    if (tex)
    {
        // always apply sampler changes, even if the Texture pointer is the same
        mCurrentSamplerBinding.descs[slot] = samplerDesc;
    }

    if (mTextures[slot] == tex) return;

    mTextures[slot] = tex;
}

void ResourceManager::FinishFrame()
{
    // invalidate shader constants, they will be reapplied and this will let us free space in the Allocator
}

void ResourceManager::StashParameters()
{
    mRuntimeParametersStash.vertexShader = mVertexShader;
    mRuntimeParametersStash.pixelShader = mPixelShader;

    for (size_t i = 0; i < mTextures.size(); ++i)
    {
        mRuntimeParametersStash.textures[i] = mTextures[i];
        mRuntimeParametersStash.samplerDescs[i] = mCurrentSamplerBinding.descs[i];
    }
}

void ResourceManager::RestoreStashedParameters()
{
    SetVertexShader(mRuntimeParametersStash.vertexShader);
    SetPixelShader(mRuntimeParametersStash.pixelShader);

    for (size_t i = 0; i < mTextures.size(); ++i)
    {
        if (mRuntimeParametersStash.textures[i])
        {
            SetTexture(static_cast<uint32_t>(i), mRuntimeParametersStash.textures[i], mRuntimeParametersStash.samplerDescs[i]);
        }
        else
        {
            ClearTextureUnit(static_cast<uint32_t>(i));
        }
    }
}

void ResourceManager::OnQueueSignal(uint64_t)
{
    // here we have to clear last used sampler descriptor regions
    // those belong to the previous command list now and should not be reused by us anymore
    mLastSamplerDescriptors.clear();

    // dirty the VS/PS constants
    mVertexShaderConstants.dirty = true;
    mPixelShaderConstants.dirty = true;
}

void ResourceManager::OnFenceSignaled(uint64_t)
{
    // noop
}

} // namespace RenderThread
} // namespace D3D12
