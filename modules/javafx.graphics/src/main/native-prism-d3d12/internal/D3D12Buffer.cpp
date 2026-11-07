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

bool Buffer::Init(const void* initialData, size_t size, D3D12_HEAP_TYPE heapType, D3D12_RESOURCE_STATES finalState)
{
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

    D3D12_RESOURCE_STATES initialState = D3D12_RESOURCE_STATE_COMMON;
    switch (mHeapType)
    {
    case D3D12_HEAP_TYPE_UPLOAD:
        initialState = D3D12_RESOURCE_STATE_GENERIC_READ;
        break;
    case D3D12_HEAP_TYPE_READBACK:
        initialState = D3D12_RESOURCE_STATE_COPY_DEST;
        break;
    }

    HRESULT hr = mNativeDevice->GetDevice()->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE,
        &resourceDesc, initialState, nullptr, IID_PPV_ARGS(&mResource));
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

    // harder path since we'll have to ask the GPU to initialize the Resource for us.
    // We might want to either preload it with data, or simply transition to the desired
    // state (aka. finalState). All this can be only done via a Command Queue execution.

    // CPU-accessible Resource, used to deliver data to D3D12 in case we preload a Default-heap resource
    D3D12ResourcePtr stagingResource;

    if (initialData != nullptr && mHeapType == D3D12_HEAP_TYPE_DEFAULT)
    {
        // create a staging CPU-readable buffer
        D3D12_HEAP_PROPERTIES stagingHeapProps;
        D3D12NI_ZERO_STRUCT(stagingHeapProps);
        stagingHeapProps.Type = D3D12_HEAP_TYPE_UPLOAD;

        HRESULT hr = mNativeDevice->GetDevice()->CreateCommittedResource(&stagingHeapProps, D3D12_HEAP_FLAG_NONE,
            &resourceDesc, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&stagingResource));
        D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Staging Buffer's Committed Resource");

        stagingResource->SetName(L"Buffer Init Staging Resource");

        void* bufPtr;
        hr = stagingResource->Map(0, nullptr, &bufPtr);
        D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to Map staging resource");

        memcpy(bufPtr, initialData, size);
        stagingResource->Unmap(0, nullptr);
    }

    // prepare a temporary Command List to transfer data and/or transition our resource
    D3D12_RESOURCE_STATES postCopyOldState = initialState;
    if (initialData != nullptr && mHeapType == D3D12_HEAP_TYPE_DEFAULT)
    {
        postCopyOldState = D3D12_RESOURCE_STATE_COPY_DEST;

        mNativeDevice->GetRenderingContext()->TransitionResource(mResource, initialState, postCopyOldState);
        mNativeDevice->GetRenderingContext()->CopyResource(mResource, stagingResource);
    }

    // transition our Default-heap resource to its desired state
    mNativeDevice->GetRenderingContext()->TransitionResource(mResource, postCopyOldState, finalState);

    // pass Staging Buffer along to release after the command list is flushed
    if (stagingResource) mNativeDevice->MarkDisposed(stagingResource);

    mDebugName = "Buffer_#";
    mDebugName += std::to_string(counter++);
    mResource->SetName(Utils::ToWString(mDebugName).c_str());

    stagingResource.Reset();

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
