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

#include "D3D12CommandListPool.hpp"

#include "internal/D3D12Config.hpp"
#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Profiler.hpp"

#include "../D3D12NativeDevice.hpp"


namespace D3D12 {
namespace RenderThread {

void CommandListPool::ResetCurrentCommandList()
{
    D3D12NI_ASSERT(CurrentCommandListData().state == CommandListState::Available, "Attempted to reset available command list #%d", mCurrentCommandList);

    HRESULT hr = CurrentCommandListData().commandList->Reset(mCommandAllocators[mCurrentCommandAllocator].allocator.Get(), nullptr);
    D3D12NI_DEV_VOID_RET_IF_FAILED(mNativeDevice, hr, "Failed to reset current command list");

    CurrentCommandListData().state = CommandListState::Active;
}

void CommandListPool::AdvanceAllocator()
{
    mCommandAllocators[mCurrentCommandAllocator].state = CommandListState::Closed;

    mCurrentCommandAllocator++;
    if (mCurrentCommandAllocator == mCommandAllocators.size()) mCurrentCommandAllocator = 0;

    // Command Allocators should be advanced per-frame by a NativeSwapChain instance.
    // If we run out of CAs we have to wait for one of previous frames to be completed.
    if (mCommandAllocators[mCurrentCommandAllocator].state != CommandListState::Available)
    {
        WaitForAvailableCommandAllocator();
    }

    D3D12NI_ASSERT(mCommandAllocators[mCurrentCommandAllocator].state == CommandListState::Available,
        "About to reset a Command Allocator that's still in use, which should never happen. Something is terribly wrong.");
    mCommandAllocators[mCurrentCommandAllocator].allocator->Reset();
    mUsedCLsWithCurrentAllocator = 0;
}


void CommandListPool::WaitForAvailableCommandList()
{
    Internal::Profiler::Instance().MarkEvent(mCommandListProfilerID, Internal::Profiler::Event::Wait);

    while (mCommandLists[mCurrentCommandList].state == CommandListState::Closed)
    {
        mWaitCallback(CheckpointType::ANY);
    }

    // if this assertion ever triggers, there is something terribly wrong
    D3D12NI_ASSERT(mCommandLists[mCurrentCommandList].state == CommandListState::Available,
        "Waited through the entire Queue, yet current Command List is still not available. Something has gone terribly wrong."
    );
}

void CommandListPool::WaitForAvailableCommandAllocator()
{
    Internal::Profiler::Instance().MarkEvent(mCommandAllocatorProfilerID, Internal::Profiler::Event::Wait);

    while (mCommandAllocators[mCurrentCommandAllocator].state == CommandListState::Closed)
    {
        mWaitCallback(CheckpointType::ANY);
    }

    // if this assertion ever triggers there is something terribly wrong
    D3D12NI_ASSERT(mCommandAllocators[mCurrentCommandAllocator].state == CommandListState::Available,
        "Waited through the entire Queue, yet current Command Allocator is still not available. Something has gone terribly wrong."
    );
}

CommandListPool::CommandListPool(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& waitCallback)
    : mNativeDevice(nativeDevice)
    , mWaitCallback(waitCallback)
    , mCommandListProfilerID(Internal::Profiler::Instance().RegisterSource("Command List Pool"))
    , mCommandAllocatorProfilerID(Internal::Profiler::Instance().RegisterSource("Command Allocator Pool"))
    , mCommandLists()
    , mCurrentCommandList(0)
    , mCommandAllocators()
    , mCurrentCommandAllocator(0)
    , mUsedCLsWithCurrentAllocator(0)
    , mNullCommandList()
{
    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(this);
}

CommandListPool::~CommandListPool()
{
    // freed as part of RenderThread before RenderingContext is fully cleaned up, so it will still be accessible
    mNativeDevice->GetRenderingContext()->UnregisterWaitableOperation(this);
}

bool CommandListPool::Init(D3D12_COMMAND_LIST_TYPE type, size_t commandListCount, size_t commandAllocators)
{
    #if DEBUG
    // for debugging
    const wchar_t* typeStr;
    switch (type)
    {
    case D3D12_COMMAND_LIST_TYPE_DIRECT:
        typeStr = L"Direct";
        break;
    case D3D12_COMMAND_LIST_TYPE_COMPUTE:
        typeStr = L"Compute";
        break;
    case D3D12_COMMAND_LIST_TYPE_COPY:
        typeStr = L"Copy";
        break;
    default:
        typeStr = L"UNKNOWN";
        break;
    }
    std::wstring caNameBase(typeStr);
    caNameBase += L" Command Allocator #";

    std::wstring clNameBase(typeStr);
    clNameBase += L" Command List #";
    #endif

    HRESULT hr = S_OK;

    mCommandAllocators.resize(commandAllocators);
    for (size_t i = 0; i < mCommandAllocators.size(); ++i) {
        hr = mNativeDevice->GetDevice()->CreateCommandAllocator(type, IID_PPV_ARGS(&mCommandAllocators[i].allocator));
        D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Command Allocator");

        mCommandAllocators[i].state = CommandListState::Available;
        mCommandAllocators[i].closedFenceValue = 0;

        #if DEBUG
        std::wstring caName = caNameBase + std::to_wstring(i);
        mCommandAllocators[i].allocator->SetName(caName.c_str());
        #endif // DEBUG
    }

    mCommandLists.resize(commandListCount);
    for (size_t i = 0; i < mCommandLists.size(); ++i)
    {
        hr = mNativeDevice->GetDevice()->CreateCommandList1(0, type, D3D12_COMMAND_LIST_FLAG_NONE, IID_PPV_ARGS(&mCommandLists[i].commandList));
        D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create a Command List for the pool");

        mCommandLists[i].state = CommandListState::Available;
        mCommandLists[i].closedFenceValue = 0;

        #if DEBUG
        std::wstring clName = clNameBase + std::to_wstring(i);
        mCommandLists[i].commandList->SetName(clName.c_str());
        #endif // DEBUG
    }

    mCurrentCommandList = 0;
    mCurrentCommandAllocator = 0;

    return true;
}

void CommandListPool::Release()
{
    for (int i = 0; i < mCommandLists.size(); ++i)
    {
        if (mCommandLists[i].state == CommandListState::Active)
            mCommandLists[i].commandList->Close();
    }
}

void CommandListPool::OnQueueSignal(uint64_t fenceValue)
{
    size_t counter = mCurrentCommandList == 0 ? mCommandLists.size() : mCurrentCommandList;
    --counter;

    while (counter != mCurrentCommandList &&
           mCommandLists[counter].state == CommandListState::Closed &&
           mCommandLists[counter].closedFenceValue == 0)
    {
        mCommandLists[counter].closedFenceValue = fenceValue;

        if (counter == 0) counter = mCommandLists.size();
        --counter;
    }

    size_t caCounter = mCurrentCommandAllocator == 0 ? mCommandAllocators.size() : mCurrentCommandAllocator;
    --caCounter;

    while (caCounter != mCurrentCommandAllocator &&
           mCommandAllocators[caCounter].state == CommandListState::Closed &&
           mCommandAllocators[caCounter].closedFenceValue == 0)
    {
        mCommandAllocators[caCounter].closedFenceValue = fenceValue;

        if (caCounter == 0) caCounter = mCommandAllocators.size();
        --caCounter;
    }
}

void CommandListPool::OnFenceSignaled(uint64_t fenceValue)
{
    for (size_t i = 0; i < mCommandLists.size(); ++i)
    {
        if (mCommandLists[i].closedFenceValue != 0 && mCommandLists[i].closedFenceValue <= fenceValue)
        {
            D3D12NI_ASSERT(mCommandLists[i].state == CommandListState::Closed, "Invalid command list state while refreshing post-fence");
            mCommandLists[i].state = CommandListState::Available;
            mCommandLists[i].closedFenceValue = 0;
        }
    }

    for (size_t i = 0; i < mCommandAllocators.size(); ++i)
    {
        if (mCommandAllocators[i].closedFenceValue != D3D12NI_INVALID_FENCE_VALUE && mCommandAllocators[i].closedFenceValue <= fenceValue)
        {
            D3D12NI_ASSERT(mCommandAllocators[i].state == CommandListState::Closed, "Invalid command allocator state while refreshing post-fence");
            mCommandAllocators[i].state = CommandListState::Available;
            mCommandAllocators[i].closedFenceValue = D3D12NI_INVALID_FENCE_VALUE;
        }
    }
}

const D3D12GraphicsCommandListPtr& CommandListPool::AdvanceCommandList(bool advanceAllocator)
{
    D3D12NI_ASSERT(CurrentCommandListData().state == CommandListState::Active, "Invalid Command List #%d state %d", mCurrentCommandList, CurrentCommandListData().state);

    HRESULT hr = CurrentCommandListData().commandList->Close();
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, mNullCommandList, "Failed to close Command List");

    CurrentCommandListData().state = CommandListState::Closed;

    D3D12GraphicsCommandListPtr& list = CurrentCommandListData().commandList;

    ++mCurrentCommandList;
    if (mCurrentCommandList == mCommandLists.size()) mCurrentCommandList = 0;

    ++mUsedCLsWithCurrentAllocator;
    if (mUsedCLsWithCurrentAllocator == mCommandLists.size() || advanceAllocator)
    {
        // Advance to the next Allocator when requested, or when we looped around the entire
        // Command List collection without advancing once
        AdvanceAllocator();
    }

    if (CurrentCommandListData().state == CommandListState::Available)
    {
        ResetCurrentCommandList();
    }

    return list;
}

} // namespace RenderThread
} // namespace D3D12
