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

#include "D3D12Buffer.hpp"

#include "D3D12Debug.hpp"

#include "../D3D12NativeDevice.hpp"


namespace D3D12 {
namespace Internal {

uint64_t Buffer::counter = 0;

Buffer::Buffer(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mResource(nullptr)
    , mSize(0)
    , mHeapType(D3D12_HEAP_TYPE_DEFAULT)
    , mDebugName()
{
}

Buffer::~Buffer()
{
    mNativeDevice.reset();

    if (mResource)
    {
        // Trace log only if we actually allocated the resource
        // with mResource being null we never called Init (or it failed)
        D3D12NI_LOG_TRACE("--- Buffer %s destroyed (size %u) ---", mDebugName.c_str(), mSize);
    }
}

bool Buffer::Init(const void* initialData, size_t size, D3D12_HEAP_TYPE heapType)
{
    if (initialData != nullptr && size > 0 && heapType == D3D12_HEAP_TYPE_DEFAULT)
    {
        D3D12NI_LOG_WARN("Default-heap Buffers can only be initialized by RenderingContext::UpdateBuffer(); initialData will be ignored");
    }

    mHeapType = heapType;
    mSize = size;

    // create The Buffer
    D3D12_RESOURCE_DESC resourceDesc;
    D3D12NI_ZERO_STRUCT(resourceDesc);
    resourceDesc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    resourceDesc.Width = static_cast<UINT64>(size);
    resourceDesc.Height = 1;
    resourceDesc.DepthOrArraySize = 1;
    resourceDesc.MipLevels = 1;
    resourceDesc.Format = DXGI_FORMAT_UNKNOWN;
    resourceDesc.Flags = D3D12_RESOURCE_FLAG_NONE;
    resourceDesc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    resourceDesc.SampleDesc.Count = 1;
    resourceDesc.SampleDesc.Quality = 0;

    D3D12_HEAP_PROPERTIES heapProps;
    D3D12NI_ZERO_STRUCT(heapProps);
    heapProps.Type = mHeapType;

    if (mHeapType == D3D12_HEAP_TYPE_READBACK && initialData != nullptr)
    {
        D3D12NI_LOG_WARN("Readback buffer cannot have initial data. Initial data will be ignored.");
    }

    switch (mHeapType)
    {
    case D3D12_HEAP_TYPE_DEFAULT:
        mResourceState = D3D12_RESOURCE_STATE_COMMON;
        break;
    case D3D12_HEAP_TYPE_UPLOAD:
        mResourceState = D3D12_RESOURCE_STATE_GENERIC_READ;
        break;
    case D3D12_HEAP_TYPE_READBACK:
        mResourceState = D3D12_RESOURCE_STATE_COPY_DEST;
        break;
    }

    HRESULT hr = mNativeDevice->GetDevice()->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE,
        &resourceDesc, mResourceState, nullptr, IID_PPV_ARGS(&mResource));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Buffer's Committed Resource");

    if (mHeapType == D3D12_HEAP_TYPE_UPLOAD)
    {
        // easy path cause CPU has direct access to the buffer
        // preload with data if necessary
        if (initialData != nullptr)
        {
            void* bufPtr;
            hr = mResource->Map(0, nullptr, &bufPtr);
            D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to Map resource");

            memcpy(bufPtr, initialData, size);
            mResource->Unmap(0, nullptr);
        }

        return true;
    }

    if (mHeapType == D3D12_HEAP_TYPE_READBACK)
    {
        // nothing to process past this point for readback buffers, return
        return true;
    }

    mDebugName = "Buffer_#";
    mDebugName += std::to_string(counter++);
    mResource->SetName(Utils::ToWString(mDebugName).c_str());

    D3D12NI_LOG_TRACE("--- Buffer %s created (size %u) ---", mDebugName.c_str(), mSize);
    return true;
}

void* Buffer::Map()
{
    void* bufPtr;
    HRESULT hr = mResource->Map(0, nullptr, &bufPtr);
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, nullptr, "Failed to Map buffer");

    return bufPtr;
}

void Buffer::Unmap()
{
    mResource->Unmap(0, nullptr);
}

void Buffer::SetName(const std::string& name)
{
    if (mResource)
    {
        mDebugName = name;
        mResource->SetName(Utils::ToWString(name).c_str());
    }
}

} // namespace Internal
} // namespace D3D12
