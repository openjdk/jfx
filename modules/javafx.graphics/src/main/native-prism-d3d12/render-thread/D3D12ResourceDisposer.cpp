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

#include "D3D12ResourceDisposer.hpp"

#include "../D3D12NativeDevice.hpp"


namespace D3D12 {
namespace RenderThread {

ResourceDisposer::ResourceDisposer(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mPurgeCheckpoints()
{
    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(this);
}

ResourceDisposer::~ResourceDisposer()
{
    mNativeDevice->GetRenderingContext()->UnregisterWaitableOperation(this);
}

void ResourceDisposer::MarkDisposed(const D3D12PageablePtr& pageable)
{
    // in case we receive a pageable from NativeBuffer/NativeTexture
    // which did not have Init() called for it
    if (!pageable) return;

    if (mPurgeCheckpoints.empty() || mPurgeCheckpoints.back().fenceValue > 0)
    {
        mPurgeCheckpoints.emplace_back();
    }

    mPurgeCheckpoints.back().pageables.emplace_back(std::move(pageable));
}

void ResourceDisposer::MarkDisposed(const NIPtr<Internal::ITrackedResource>& resource)
{
    if (!resource) return;

    if (mPurgeCheckpoints.empty() || mPurgeCheckpoints.back().fenceValue > 0)
    {
        mPurgeCheckpoints.emplace_back();
    }

    mPurgeCheckpoints.back().resources.emplace_back(std::move(resource));
}

void ResourceDisposer::OnQueueSignal(uint64_t fenceValue)
{
    if (!mPurgeCheckpoints.empty() && mPurgeCheckpoints.back().fenceValue == 0)
    {
        mPurgeCheckpoints.back().fenceValue = fenceValue;
    }
}

void ResourceDisposer::OnFenceSignaled(uint64_t fenceValue)
{
    while (!mPurgeCheckpoints.empty())
    {
        PurgeCheckpoint& checkpoint = mPurgeCheckpoints.front();

        // Skip this checkpoint if OnQueueSignal was not yet called (checkpoint's fence value is 0) or
        // if our command list has not yet finished (checkpoint's fence value is higher than the provided one)
        // We assume checkpoints will be allocated in always incremental order, like in RingContainer
        if (checkpoint.fenceValue == 0 || checkpoint.fenceValue > fenceValue) break;

        // pop the checkpoint which will destroy all smart pointers (both pageables and NI resources)
        mPurgeCheckpoints.pop_front();
    }
}

} // namespace RenderThread
} // namespace D3D12
