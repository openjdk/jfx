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

#include "internal/D3D12ITrackedResource.hpp"

#include "D3D12IWaitableOperation.hpp"

#include <unordered_set>
#include <deque>


namespace D3D12 {
namespace RenderThread {

class ResourceDisposer: public IWaitableOperation
{
    template <typename T>
    using ResourceList = std::list<T>;

    struct PurgeCheckpoint
    {
        // at which fence checkpoint we're supposed to purge the resource
        uint64_t fenceValue;
        // a list of pageables to clear at that checkpoint
        ResourceList<D3D12PageablePtr> pageables;
        ResourceList<NIPtr<Internal::ITrackedResource>> resources;

        PurgeCheckpoint()
            : fenceValue(0)
            , pageables()
            , resources()
        {}
    };

    NIPtr<NativeDevice> mNativeDevice;
    std::deque<PurgeCheckpoint> mPurgeCheckpoints;

public:
    ResourceDisposer(const NIPtr<NativeDevice>& nativeDevice);
    ~ResourceDisposer();

    void MarkDisposed(const D3D12PageablePtr& pageable);
    void MarkDisposed(const NIPtr<Internal::ITrackedResource>& resource);

    virtual void OnQueueSignal(uint64_t fenceValue) override;
    virtual void OnFenceSignaled(uint64_t fenceValue) override;
};

} // namespace RenderThread
} // namespace D3D12
