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

#include "D3D12NativeTexture.hpp"

#include "D3D12NativeDevice.hpp"

#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Utils.hpp"

#include <com_sun_prism_d3d12_ni_D3D12NativeTexture.h>


namespace D3D12 {

uint64_t NativeTexture::textureCounter = 0;
uint64_t NativeTexture::depthTextureCounter = 0;
uint64_t NativeTexture::rttextureCounter = 0;

bool NativeTexture::InitInternal(const D3D12_RESOURCE_DESC& desc)
{
    D3D12_HEAP_PROPERTIES heapProps;
    D3D12NI_ZERO_STRUCT(heapProps);
    // Texture resources require DEFAULT heap type
    heapProps.Type = D3D12_HEAP_TYPE_DEFAULT;

    D3D12_CLEAR_VALUE clearValue;
    clearValue.Color[0] = 0.0f;
    clearValue.Color[1] = 0.0f;
    clearValue.Color[2] = 0.0f;
    clearValue.Color[3] = 0.0f;
    clearValue.DepthStencil.Depth = 0.0f;
    clearValue.DepthStencil.Stencil = 0;
    clearValue.Format = desc.Format;

    D3D12ResourcePtr resource;
    HRESULT hr = mNativeDevice->GetDevice()->CreateCommittedResource(
        &heapProps, D3D12_HEAP_FLAG_NONE,
        &desc, D3D12_RESOURCE_STATE_COMMON,
        (desc.Flags & D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET) ? &clearValue : nullptr,
        IID_PPV_ARGS(&resource)
    );
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Texture's Committed Resource");

    if (!IsDepthFormat(desc.Format))
    {
        UINT componentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
        if (desc.Format == DXGI_FORMAT_R8_UNORM)
        {
            // BYTE_GRAY should map R to RGB with A set to 1
            componentMapping = D3D12_ENCODE_SHADER_4_COMPONENT_MAPPING(
                D3D12_SHADER_COMPONENT_MAPPING_FROM_MEMORY_COMPONENT_0,
                D3D12_SHADER_COMPONENT_MAPPING_FROM_MEMORY_COMPONENT_0,
                D3D12_SHADER_COMPONENT_MAPPING_FROM_MEMORY_COMPONENT_0,
                D3D12_SHADER_COMPONENT_MAPPING_FORCE_VALUE_1
            );
        }

        // Allocate the most commonly used descriptor covering all mips
        // The only time we'll need something different is when calling MipmapGen
        D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc;
        D3D12NI_ZERO_STRUCT(srvDesc);
        srvDesc.Shader4ComponentMapping = componentMapping;
        srvDesc.Format = desc.Format;
        srvDesc.ViewDimension = (desc.SampleDesc.Count > 1) ? D3D12_SRV_DIMENSION_TEXTURE2DMS : D3D12_SRV_DIMENSION_TEXTURE2D;
        srvDesc.Texture2D.MipLevels = mMipLevels;
        srvDesc.Texture2D.MostDetailedMip = 0;
        srvDesc.Texture2D.PlaneSlice = 0;
        srvDesc.Texture2D.ResourceMinLODClamp = 0.0f;

        if (mSRVDescriptor)
        {
            mNativeDevice->GetSRVDescriptorAllocator()->Free(mSRVDescriptor);
        }

        mSRVDescriptor = mNativeDevice->GetSRVDescriptorAllocator()->Allocate(1);
        if (!mSRVDescriptor)
        {
            D3D12NI_LOG_ERROR("Failed to allocate base SRV descriptor for texture");
            return false;
        }

        mNativeDevice->GetDevice()->CreateShaderResourceView(resource.Get(), &srvDesc, mSRVDescriptor.CPU(0));
    }

    TextureBase::Init(resource, desc, D3D12_RESOURCE_STATE_COMMON);

    // Texture will be separately loaded with data via Java's Texture.update() calls
    // Fill in remaining members and leave
    if (mResourceDesc.Flags & D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET)
    {
        SetName("RTTexture_#" + std::to_string(rttextureCounter++));
    }
    else if (mResourceDesc.Flags & D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL)
    {
        SetName("DepthTexture_#" + std::to_string(depthTextureCounter++));
    }
    else
    {
        SetName("Texture_#" + std::to_string(textureCounter++));
    }

    D3D12NI_LOG_TRACE("--- Texture %s created (%ux%u %s %uxMSAA %s) ---",
        mDebugName.c_str(), mResourceDesc.Width, mResourceDesc.Height, Internal::DXGIFormatToString(mResourceDesc.Format),
        mResourceDesc.SampleDesc.Count, HasMipmaps() ? "Mip" : "NoMip");

    return true;
}

NativeTexture::NativeTexture(const NIPtr<NativeDevice>& nativeDevice)
    : TextureBase()
    , mNativeDevice(nativeDevice)
    , mMipLevels()
    , mSRVDescriptor()
{
}

NativeTexture::~NativeTexture()
{
    if (mSRVDescriptor && mNativeDevice->GetSRVDescriptorAllocator())
    {
        mNativeDevice->GetSRVDescriptorAllocator()->Free(mSRVDescriptor);
    }

    mNativeDevice.reset();

    if (mResource)
    {
        // Trace log only if we actually allocated the resource
        // with mBufferResource being null we never called Init (or it failed)
        D3D12NI_LOG_TRACE("--- Texture %s destroyed (%ux%u %s %uxMSAA %s) ---",
            mDebugName.c_str(), mResourceDesc.Width, mResourceDesc.Height, Internal::DXGIFormatToString(mResourceDesc.Format),
            mResourceDesc.SampleDesc.Count, HasMipmaps() ? "Mip" : "NoMip");
    }
}

bool NativeTexture::Init(UINT width, UINT height, DXGI_FORMAT format, D3D12_RESOURCE_FLAGS flags,
                         TextureUsage usage, TextureWrapMode wrapMode, int samples, bool useMipmap)
{
    if (width <= 0 || width > D3D12_REQ_TEXTURE2D_U_OR_V_DIMENSION ||
        height <= 0 || height > D3D12_REQ_TEXTURE2D_U_OR_V_DIMENSION)
    {
        D3D12NI_LOG_ERROR("Invalid width and/or height");
        return false;
    }

    mSamplerDesc.wrapMode = wrapMode;
    mSamplerDesc.isLinear = false;
    mMipLevels = 1;
    if (useMipmap) {
        mMipLevels = Internal::Utils::CalcMipmapLevels(width, height);

        // to use Compute Shader and generate mips later on
        flags |= D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;
    }

    D3D12_RESOURCE_DESC desc;
    D3D12NI_ZERO_STRUCT(desc);
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = width;
    desc.Height = height;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = mMipLevels;
    desc.Format = format;
    desc.Flags = flags;
    desc.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
    desc.SampleDesc.Count = samples;
    desc.SampleDesc.Quality = 0;

    mStates.resize(mMipLevels);

    return InitInternal(desc);
}

UINT64 NativeTexture::GetSize()
{
    if (!GetD3D12Resource()) return -1;

    D3D12_RESOURCE_DESC resDesc;
    resDesc = GetD3D12Resource()->GetDesc();

    return resDesc.Width * resDesc.Height * resDesc.DepthOrArraySize * GetDXGIFormatBPP(resDesc.Format) * resDesc.SampleDesc.Count;
}

bool NativeTexture::Resize(UINT width, UINT height)
{
    if (width == mResourceDesc.Width && height == mResourceDesc.Height) return true;

    // capture old resource to recover in case we fail new resource initialization
    D3D12ResourcePtr oldResource = GetD3D12Resource();

    mResourceDesc.Width = width;
    mResourceDesc.Height = height;
    if (!InitInternal(mResourceDesc))
    {
        // recover old resource and its desc before returning failure
        mResource = oldResource;
        mResourceDesc = mResource->GetDesc();
        return false;
    }

    // success, dispose of old resource and return
    mNativeDevice->MarkDisposed(oldResource);
    return true;
}

void NativeTexture::SetSamplerParameters(TextureWrapMode wrapMode, bool isLinear)
{
    mSamplerDesc.wrapMode = wrapMode;
    mSamplerDesc.isLinear = isLinear;
}

void NativeTexture::WriteSRVToDescriptor(const D3D12_CPU_DESCRIPTOR_HANDLE& descriptorCpu, UINT mipLevels, UINT mostDetailedMip)
{
    if (mSRVDescriptor && mipLevels == 0 && mostDetailedMip == 0)
    {
        // fast path for most use cases
        mNativeDevice->GetDevice()->CopyDescriptorsSimple(1, descriptorCpu, mSRVDescriptor.CPU(0), D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    }
    else
    {
        // slow path, usually should only be taken by MipmapGen
        D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc;
        D3D12NI_ZERO_STRUCT(srvDesc);
        srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
        srvDesc.Format = mResourceDesc.Format;
        srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
        srvDesc.Texture2D.MipLevels = (mipLevels == 0) ? mMipLevels : mipLevels;
        srvDesc.Texture2D.MostDetailedMip = mostDetailedMip;
        srvDesc.Texture2D.PlaneSlice = 0;
        srvDesc.Texture2D.ResourceMinLODClamp = 0.0f;

        mNativeDevice->GetDevice()->CreateShaderResourceView(GetD3D12Resource().Get(), &srvDesc, descriptorCpu);
    }
}

void NativeTexture::WriteUAVToDescriptor(const D3D12_CPU_DESCRIPTOR_HANDLE& descriptorCpu, UINT mipSlice)
{
    D3D12_UNORDERED_ACCESS_VIEW_DESC uavDesc;
    D3D12NI_ZERO_STRUCT(uavDesc);
    uavDesc.Format = mResourceDesc.Format;
    uavDesc.ViewDimension = D3D12_UAV_DIMENSION_TEXTURE2D;
    uavDesc.Texture2D.MipSlice = mipSlice;

    mNativeDevice->GetDevice()->CreateUnorderedAccessView(GetD3D12Resource().Get(), nullptr, &uavDesc, descriptorCpu);
}

} // namespace D3D12


#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeTexture_nReleaseNativeObject
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(ptr);
    tex->GetDevice()->MarkDisposed(tex);
    D3D12::FreeNIObject<D3D12::NativeTexture>(ptr);
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeTexture_nGetSize
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return -1;

    return static_cast<jlong>(D3D12::GetNIObject<D3D12::NativeTexture>(ptr)->GetSize());
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeTexture_nGetWidth
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return -1;

    return static_cast<jint>(D3D12::GetNIObject<D3D12::NativeTexture>(ptr)->GetWidth());
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeTexture_nGetHeight
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return -1;

    return static_cast<jint>(D3D12::GetNIObject<D3D12::NativeTexture>(ptr)->GetHeight());
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeTexture_nResize
    (JNIEnv* env, jobject obj, jlong ptr, jint width, jint height)
{
    if (!ptr) return false;
    if (width <= 0 || width > D3D12_REQ_TEXTURE2D_U_OR_V_DIMENSION) return false;
    if (height <= 0 || height > D3D12_REQ_TEXTURE2D_U_OR_V_DIMENSION) return false;

    return static_cast<jboolean>(D3D12::GetNIObject<D3D12::NativeTexture>(ptr)->Resize(static_cast<UINT>(width), static_cast<UINT>(height)));
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeTexture_nSetSamplerParameters
    (JNIEnv* env, jobject obj, jlong ptr, jint wrapMode, jboolean isLinear)
{
    if (!ptr) return;
    if (wrapMode < 0 || wrapMode >= static_cast<jint>(D3D12::TextureWrapMode::MAX_ENUM)) return;

    D3D12::GetNIObject<D3D12::NativeTexture>(ptr)->SetSamplerParameters(
        static_cast<D3D12::TextureWrapMode>(wrapMode), static_cast<bool>(isLinear)
    );
}

#ifdef __cplusplus
}
#endif
