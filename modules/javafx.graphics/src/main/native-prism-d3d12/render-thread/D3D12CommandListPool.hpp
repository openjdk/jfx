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

#include "D3D12IWaitableOperation.hpp"

#include <vector>

namespace D3D12 {
namespace RenderThread {

class CommandListPool: public IWaitableOperation
{
    enum class CommandListState
    {
        Available, // Ready-to-use CL that has not been reset yet
        Active, // CL was reset and is currently used for recording. There should only be one Active CL.
        Closed, // CL has finished recording, was closed and has to be/is executed
    };


    struct CommandListData
    {
        D3D12GraphicsCommandListPtr commandList;
        CommandListState state;
        uint64_t closedFenceValue;
    };

    struct CommandAllocatorData
    {
        D3D12CommandAllocatorPtr allocator;
        CommandListState state; // same three states as CLs
        uint64_t closedFenceValue;
    };

    NIPtr<NativeDevice> mNativeDevice;
    CheckpointCallback mWaitCallback;
    uint32_t mCommandListProfilerID;
    uint32_t mCommandAllocatorProfilerID;
    std::vector<CommandListData> mCommandLists;
    size_t mCurrentCommandList;
    std::vector<CommandAllocatorData> mCommandAllocators;
    size_t mCurrentCommandAllocator;
    size_t mUsedCLsWithCurrentAllocator;
    D3D12GraphicsCommandListPtr mNullCommandList; // for error returning

    void ResetCurrentCommandList();
    void AdvanceAllocator();
    void WaitForAvailableCommandList();
    void WaitForAvailableCommandAllocator();

    inline CommandListData& CurrentCommandListData()
    {
        return mCommandLists[mCurrentCommandList];
    }

public:
    CommandListPool(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& waitCallback);
    ~CommandListPool();

    bool Init(D3D12_COMMAND_LIST_TYPE type, size_t commandListCount, size_t commandAllocators);
    void Release();

    void OnQueueSignal(uint64_t fenceValue) override;
    void OnFenceSignaled(uint64_t fenceValue) override;
    const D3D12GraphicsCommandListPtr& AdvanceCommandList(bool advanceAllocator);

    inline const D3D12GraphicsCommandListPtr& CurrentCommandList()
    {
        if (mCommandLists[mCurrentCommandList].state == CommandListState::Closed) WaitForAvailableCommandList();

        D3D12NI_ASSERT(mCommandLists[mCurrentCommandList].state != CommandListState::Closed, "Attempted to access closed Command List");
        if (mCommandLists[mCurrentCommandList].state == CommandListState::Available) ResetCurrentCommandList();
        return CurrentCommandListRef();
    }

    inline const D3D12GraphicsCommandListPtr& CurrentCommandListRef()
    {
        return mCommandLists[mCurrentCommandList].commandList;
    }
};

} // namespace RenderThread
} // namespace D3D12
