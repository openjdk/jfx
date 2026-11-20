/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12RenderThread.hpp"

#include "../D3D12NativeDevice.hpp"

#include "internal/D3D12Config.hpp"
#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Profiler.hpp"
#include "internal/D3D12Logger.hpp"


namespace D3D12 {
namespace RenderThread {



RenderPayloadPtr& RenderThread::FetchPayload()
{
    std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

    if (mPayloadQueue.size() > 0)
    {
        // we're here because there used to be an element on queue that we just finished
        // we should pop right now
        // Payload's Waitable should be signaled after we pop the Payload to mark we are completely done with it
        // which also includes freeing any shared_ptrs and references to used objects.
        // A hard copy is done here to ensure Waitable is valid after pop()
        NIPtr<Internal::Waitable> w(mPayloadQueue.front()->GetWaitable());
        mPayloadQueue.pop();

        // pop() destroyed the Payload (except for the Waitable) so now we can Signal it and move on
        // TODO: D3D12: Error-reporting from RT side
        w->Signal();
    }

    while (mPayloadQueue.size() == 0)
    {
        mQueueEmptyCV.notify_one();

        mPayloadAvailableCV.wait(lock);
        if (mWorkerDone) return mNullPayload;
    }

    return mPayloadQueue.front();
}

void RenderThread::WorkerMain()
{
    while (!mWorkerDone)
    {
        RenderPayloadPtr& curPayload = FetchPayload();
        if (!curPayload) continue;

        D3D12NI_LOG_TRACE("Executing payload %d steps", curPayload->StepsCount());
        // record what is needed on the Command List
        if (!curPayload->ApplySteps(mContext))
        {
            D3D12NI_LOG_ERROR("RenderThread: Failed to apply current payload's steps. This should not happen. Pausing execution.");

            std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

            // fast cleanup path from the worker thread in case of an error - wait for GPU, clear all payloads, exit
            mWorkerDone = true;
            mCheckpointQueue.WaitForNextCheckpoint(CheckpointType::ALL);
            while (!mPayloadQueue.empty()) mPayloadQueue.pop();

            mQueueEmptyCV.notify_all(); // in case main thread waits on mQueueEmptyCV
            D3D12NI_LOG_DEBUG("RenderThread done because of an error.");
            return;
        }
    }

    {
        // finish the payload queue before leaving to ensure everything gets delivered and done on D3D12 side
        // the only time we should be here (in normal conditions) is when main thread set mWorkerDone to true
        // and immediately after waits for RenderThread to join it, so we can safely wrap up the rest of our work
        std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

        mWorkerDone = true;

        // if mWorkerDone was set to true mid-payload, that means we just executed a payload but
        // FetchPayload() didn't have a chance to pop() it yet and clean it up, so we must do that now
        // if the queue has more than 0 elements pop the front queue piece and then continue on executing
        // the remainder of the queue
        if (mPayloadQueue.size() > 0)
        {
            mPayloadQueue.pop();
        }

        while (mPayloadQueue.size() > 0)
        {
            mPayloadQueue.front()->ApplySteps(mContext);
            mPayloadQueue.pop();
        }

        // all payloads are now processed, wait for GPU to be done before completing
        ExecuteCurrentCommandList(true);
        Signal(CheckpointType::ENDFRAME);
        mCheckpointQueue.WaitForNextCheckpoint(CheckpointType::ALL);
        mCheckpointQueue.PrintStats();
    }

    D3D12NI_LOG_DEBUG("RenderThread done");
}

void RenderThread::ExecuteCurrentCommandList(bool advanceAllocator)
{
    D3D12NI_ASSERT(std::this_thread::get_id() == mWorkerThread.get_id(), "This routine must be called from Render Thread");

    D3D12GraphicsCommandListPtr cmdList = mContext->AdvanceCommandList(advanceAllocator);
    if (!cmdList)
    {
        D3D12NI_LOG_ERROR("RenderThread: Received empty Command List, aborting execution.");
        return;
    }

    ID3D12CommandList* lists[1] = { cmdList.Get() };
    mCommandQueue->ExecuteCommandLists(1, lists);

    mContext->ClearAppliedSteps();
    mContext->Invalidate2DVertexBatch();
}

uint64_t RenderThread::Signal(CheckpointType type)
{
    mFenceValue++;
    if (mFenceValue == D3D12NI_INVALID_FENCE_VALUE) mFenceValue++;

    // mark this point in time in places that need it
    {
        std::unique_lock<std::mutex> lock(mWaitableOpsMutex);

        for (IWaitableOperation* op: mWaitableOps)
        {
            op->OnQueueSignal(mFenceValue);
        }
    }

    Internal::Waitable waitable(mFenceValue, [this](uint64_t fenceValue) -> bool
    {
        std::unique_lock<std::mutex> lock(mWaitableOpsMutex);

        for (IWaitableOperation* op: mWaitableOps)
        {
            op->OnFenceSignaled(fenceValue);
        }

        return true;
    });

    HRESULT hr = mCommandQueue->Signal(mCommandQueueFence.Get(), mFenceValue);
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, D3D12NI_INVALID_FENCE_VALUE, "Failed to signal event on completion");

    hr = mCommandQueueFence->SetEventOnCompletion(mFenceValue, waitable.GetHandle());
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, D3D12NI_INVALID_FENCE_VALUE, "Failed to set Fence event on completion");

    mCheckpointQueue.AddCheckpoint(type, std::move(waitable));
    return mFenceValue;
}

void RenderThread::FlushCommandListInternal(CheckpointType type)
{
    D3D12NI_ASSERT(std::this_thread::get_id() == mWorkerThread.get_id(), "This routine must be called from Render Thread");

    ExecuteCurrentCommandList(false);
    Signal(type);
}

void RenderThread::WaitForCheckpointInternal(CheckpointType type)
{
    D3D12NI_ASSERT(std::this_thread::get_id() == mWorkerThread.get_id(), "This routine must be called from Render Thread");
    mCheckpointQueue.WaitForNextCheckpoint(type);
}


RenderThread::RenderThread(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mCommandQueue()
    , mCommandQueueFence()
    , mFenceValue(0)
    , mWaitableOps()
    , mCheckpointQueue()
    , mContext()
    , mNullPayload(nullptr, Internal::LinearAllocatorDeleter<RenderPayload>(nullptr))
    , mPayloadAvailableCV()
    , mPayloadQueueMutex()
    , mPayloadQueue()
    , mWorkerDone(false)
    , mWorkerState(WorkerState::STOPPED)
    , mWorkerThread()
{
}

bool RenderThread::Init()
{
    mContext = std::make_unique<RenderThreadContext>(mNativeDevice,
        std::bind(&RenderThread::FlushCommandListInternal, this, std::placeholders::_1),
        std::bind(&RenderThread::WaitForCheckpointInternal, this, std::placeholders::_1),
        std::bind(&RenderThread::Signal, this, std::placeholders::_1)
    );
    if (!mContext)
    {
        D3D12NI_LOG_ERROR("Failed to create RenderThreadState");
        return false;
    }

    if (!mContext->Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize RenderThread's State object");
        return false;
    }

    D3D12_COMMAND_QUEUE_DESC cqDesc;
    D3D12NI_ZERO_STRUCT(cqDesc);
    cqDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    cqDesc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
    cqDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;

    // TODO Command Queue should probably reside in Command List Pool
    //      Same with all Execute/Signal logic & checkpoints
    HRESULT hr = mNativeDevice->GetDevice()->CreateCommandQueue(&cqDesc, IID_PPV_ARGS(&mCommandQueue));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create Direct Command Queue");

    hr = mCommandQueue->SetName(L"Main Command Queue");
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to name Direct Command Queue");

    hr = mNativeDevice->GetDevice()->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&mCommandQueueFence));
    D3D12NI_DEV_RET_IF_FAILED(mNativeDevice, hr, false, "Failed to create in-device Fence");

    {
        std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

        // start up the thread and wait until it's ready to accept payload
        try
        {
            D3D12NI_LOG_DEBUG("Starting Render Thread");
            mWorkerThread = std::thread(&RenderThread::WorkerMain, this);
        }
        catch (const std::exception& e)
        {
            D3D12NI_LOG_ERROR("RenderThread constructor threw an exception: %s", e.what());
            return false;
        }

        SetThreadDescription(mWorkerThread.native_handle(), L"JavaFX D3D12 RenderThread");
        mWorkerState = WorkerState::RUNNING;
        mQueueEmptyCV.wait(lock);
    }

    return true;
}

void RenderThread::RegisterWaitableOperation(IWaitableOperation* waitableOp)
{
    std::unique_lock<std::mutex> lock(mWaitableOpsMutex);

    mWaitableOps.emplace_back(waitableOp);
}

void RenderThread::UnregisterWaitableOperation(IWaitableOperation* waitableOp)
{
    std::unique_lock<std::mutex> lock(mWaitableOpsMutex);

    for (size_t i = 0; i < mWaitableOps.size(); ++i)
    {
        if (mWaitableOps[i] == waitableOp)
        {
            if (i != mWaitableOps.size() - 1)
            {
                mWaitableOps[i] = mWaitableOps[mWaitableOps.size() - 1];
            }

            mWaitableOps.pop_back();
        }
    }
}

NIPtr<Internal::Waitable> RenderThread::Execute(RenderPayloadPtr&& payload)
{
    D3D12NI_ASSERT(mWorkerThread.get_id() != std::this_thread::get_id(), "RenderThread::Execute() can only be called from main thread");

    std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

    if (mWorkerDone || mWorkerState == WorkerState::STOPPED) return nullptr;

    NIPtr<Internal::Waitable> waitable(payload->GetWaitable());
    mPayloadQueue.emplace(std::move(payload));
    mPayloadAvailableCV.notify_one();

    return waitable;
}

bool RenderThread::ScheduleCommandListSubmit(Internal::LinearAllocator& allocator, RenderPayloadPtr& payload, bool advanceAllocator = false)
{
    D3D12NI_ASSERT(mWorkerThread.get_id() != std::this_thread::get_id(), "RenderThread::ScheduleCommandListSubmit() can only be called from main thread");

    // this schedule and others are assumed to always fit due to the Payload limit being lower than actual size (and we want them in one Payload).
    // As such we only check here for errors in adding the step and not if the Payload is full
    return (payload->AddStep(CreateRTExec<InternalRenderThreadRoutine>(allocator, std::bind(&RenderThread::ExecuteCurrentCommandList, this, advanceAllocator))) != RenderPayload::StepAddResult::FAILED);
}

bool RenderThread::ScheduleSignal(Internal::LinearAllocator& allocator, RenderPayloadPtr& payload, CheckpointType type)
{
    D3D12NI_ASSERT(mWorkerThread.get_id() != std::this_thread::get_id(), "RenderThread::ScheduleSignal() can only be called from main thread");

    return (payload->AddStep(CreateRTExec<InternalRenderThreadRoutine>(allocator, std::bind(&RenderThread::Signal, this, type))) != RenderPayload::StepAddResult::FAILED);
}

bool RenderThread::ScheduleWaitForCheckpoint(Internal::LinearAllocator& allocator, RenderPayloadPtr& payload, CheckpointType type)
{
    D3D12NI_ASSERT(mWorkerThread.get_id() != std::this_thread::get_id(), "RenderThread::WaitForCheckpoint() can only be called from main thread");

    return (payload->AddStep(CreateRTExec<InternalRenderThreadRoutine>(allocator, std::bind(&RenderThread::WaitForCheckpointInternal, this, type))) != RenderPayload::StepAddResult::FAILED);
}

void RenderThread::WaitUntilIdle()
{
    std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

    while (mPayloadQueue.size() != 0)
    {
        mQueueEmptyCV.wait(lock);
    }
}

void RenderThread::Exit()
{
    D3D12NI_ASSERT(mWorkerThread.get_id() != std::this_thread::get_id(), "RenderThread::Exit() can only be called from main thread");

    {
        std::unique_lock<std::mutex> lock(mPayloadQueueMutex);

        D3D12NI_LOG_DEBUG("Exiting Render Thread");
        if (mWorkerState == WorkerState::RUNNING)
        {
            while (mPayloadQueue.size() != 0)
            {
                mQueueEmptyCV.wait(lock);
            }

            mWorkerDone = true;
            mPayloadAvailableCV.notify_one();
        }
    }

    if (mWorkerThread.joinable()) mWorkerThread.join();

    // free up RT resources
    mWorkerState = WorkerState::STOPPED;

    if (mContext)
    {
        mContext->Release();
        mContext.reset();
    }
}

} // namespace RenderThread
} // namespace D3D12
