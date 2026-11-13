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

#include "D3D12RingContainer.hpp"

#include "../D3D12NativeDevice.hpp"

#include "internal/D3D12Profiler.hpp"
#include "internal/D3D12Utils.hpp"


namespace D3D12 {
namespace RenderThread {

void RingContainer::CheckThreshold()
{
    if (mUncommitted > mFlushThreshold)
    {
        // NOTE: This is just an extra check that should be considered an error.
        // If Ring Containers in any point in time loop-around _after_ the allocation this can have some
        // unintended consequences - at best we simply waste space in the Container and don't properly
        // triple-buffer it which can cause stutters or performance issues, at worst we will flush a Command List
        // while we're recording it. We treat this flush as a last-resort to keep the app going, but at some point
        // it should be properly fixed (usually it means DeclareRequired() mis-estimated something or was not called).
        D3D12NI_LOG_ERROR("%s: CheckThreshold wanted a midframe flush! Maybe it shouldn't do that", mDebugName.c_str());
        Internal::Profiler::Instance().MarkEvent(mProfilerSourceID, Internal::Profiler::Event::Signal);
        mFlushCallback(CheckpointType::MIDFRAME);
    }
}

bool RingContainer::AwaitNextCheckpoint(size_t needed)
{
    while (mSize - mUsed < needed)
    {
        if (mCheckpoints.empty())
        {
            // NOTE: we landed here because Ring Container couldn't allocate any more
            // data but we never set any checkpoints. This means either threshold is very
            // low, or we attempt to reserve large amount of data.
            // This is a last resort which shouldn't be triggered or it can cause issues.
            // If it is triggered, better to adjust Ring Container parameters (or fix something
            // somewhere else) to prevent it from triggering.
            D3D12NI_LOG_WARN("Triggered a mid-frame Command List flush right before waiting for next checkpoint."
                "This might cause some glitches and generally should be prevented");
            mFlushCallback(CheckpointType::MIDFRAME);
        }

        // await for any waitable set by FlushCommandList()
        // if it's not enough we will loop around
        //D3D12NI_LOG_TRACE("%s: must wait! (used %d uncommitted %d threshold %d size %d)",
        //    mDebugName.c_str(), mUsed, mUncommitted, mFlushThreshold, mSize
        //);
        Internal::Profiler::Instance().MarkEvent(mProfilerSourceID, Internal::Profiler::Event::Wait);
        mWaitCallback(CheckpointType::ANY);
    }

    return true;
}

void RingContainer::PrintHumanReadableSize()
{
    char unitSize, unitThreshold;
    size_t printedSize = Internal::Utils::PrettifySize(mSize, unitSize);
    size_t printedThreshold = Internal::Utils::PrettifySize(mFlushThreshold, unitThreshold);

    D3D12NI_LOG_INFO("%s - %d%c (%d) size, %d%c (%d) flush threshold",
        mDebugName.c_str(),
        printedSize, unitSize, mSize,
        printedThreshold, unitThreshold, mFlushThreshold
    );
}

RingContainer::RingContainer(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback)
    : mFlushThreshold(0)
    , mUsed(0)
    , mUncommitted(0)
    , mHead(0)
    , mFlushCallback(flushCallback)
    , mWaitCallback(waitCallback)
    , mCheckpoints()
    , mDebugName()
    , mNativeDevice(nativeDevice)
    , mSize(0)
    , mAlignment(0)
    , mTail(0)
    , mProfilerSourceID(0)
{
    mDebugName = "Ring Container";

    mNativeDevice->GetRenderingContext()->RegisterWaitableOperation(this);
    mProfilerSourceID = Internal::Profiler::Instance().RegisterSource(mDebugName);
}

RingContainer::~RingContainer()
{
    mNativeDevice->GetRenderingContext()->UnregisterWaitableOperation(this);
    mNativeDevice.reset();

    D3D12NI_LOG_TRACE("%s destroyed", mDebugName.c_str());
}

bool RingContainer::InitInternal(size_t flushThreshold, size_t alignment, size_t totalSize)
{
    // alignment has to be a power of two
    if ((alignment == 0) || (alignment & (alignment - 1)) != 0)
    {
        D3D12NI_LOG_ERROR("%s allocation alignment must be a power of two; was %ld", mDebugName.c_str(), alignment);
        return false;
    }

    totalSize = Internal::Utils::Align(totalSize, alignment);

    // Default Ring Container size is 3 times the flush threshold, which causes
    // mid-frame resources to triple-buffer. This should be the case in most
    // situations with Sampler Heap being the only exception.
    mSize = (totalSize > 0) ? totalSize : (3 * flushThreshold);
    mFlushThreshold = (flushThreshold > mSize) ? mSize : flushThreshold;
    mAlignment = alignment;
    mUsed = mUncommitted = mHead = mTail = 0;

    PrintHumanReadableSize();
    return true;
}

RingContainer::Region RingContainer::ReserveInternal(size_t size)
{
    if (size == 0)
    {
        D3D12NI_LOG_ERROR("%s: Attempted to allocate 0 ring container slots", mDebugName.c_str());
        return Region();
    }

    size_t alignedTail = Internal::Utils::Align(mTail, mAlignment);
    size = Internal::Utils::Align(size, mAlignment);

    if (size > mSize)
    {
        D3D12NI_LOG_ERROR("%s: Requested data too big after alignment: %ld", mDebugName.c_str(), size);
        return Region();
    }

    // check if this Reserve() will overfill the buffer and if so wait until it frees up
    size_t sizeToEnd = mSize - alignedTail;
    if (sizeToEnd < size)
    {
        // special case, we'll have to loop-around
        if (mUsed + sizeToEnd + size > mSize)
        {
            AwaitNextCheckpoint(sizeToEnd + size);
            if (mUsed + sizeToEnd + size > mSize)
            {
                D3D12NI_LOG_ERROR("%s fully allocated, cannot allocate %ld bytes (h: %ld t: %ld used: %ld size %ld)",
                    mDebugName.c_str(), size, mHead, mTail, mUsed, mSize);
                return Region();
            }
        }
    }
    else
    {
        // most cases - padding to alignment + requested size
        if (mUsed + (alignedTail - mTail) + size > mSize)
        {
            AwaitNextCheckpoint(size);
            if (mUsed + (alignedTail - mTail) + size > mSize)
            {
                D3D12NI_LOG_ERROR("%s fully allocated, cannot allocate %ld bytes (h: %ld t: %ld used: %ld size %ld)",
                    mDebugName.c_str(), size, mHead, mTail, mUsed, mSize);
                return Region();
            }
        }
    }

    if (alignedTail >= mHead)
    {
        // tail is past head, so we haven't "looped around" yet
        // figure out if we can still allocate, or do we have to loop
        if ((alignedTail + size) <= mSize)
        {
            // not crossing past buffer's total size, reserve and return
            size_t newTail = alignedTail + size;
            size_t allocSize = newTail - mTail;
            mUsed += allocSize;
            mUncommitted += allocSize;
            D3D12NI_ASSERT(mUsed <= mSize, "%s: Used is larger than size, probably underflowed (%ld vs %ld)", mUsed, mSize);

            Region r(size, alignedTail);
            mTail = newTail;
            CheckThreshold();
            return std::move(r);
        }
        else
        {
            // loop-around - beginning of ring Container still has enough room
            // also re-check - padding to the end of the buffer might've misled the Await condition above
            size_t newTail = size;
            size_t allocSize = size + mSize - mTail; // size + padding to the end of the buffer
            mUsed += allocSize;
            mUncommitted += allocSize;
            D3D12NI_ASSERT(mUsed <= mSize, "%s: Used is larger than size, probably underflowed (%ld vs %ld)", mUsed, mSize);

            Region r(size, 0);
            mTail = newTail;
            CheckThreshold();
            return std::move(r);
        }
    }
    else if (mTail < mHead)
    {
        // tail is before head but we have enough room to allocate the data
        size_t newTail = alignedTail + size;
        size_t allocSize = newTail - mTail;
        mUsed += allocSize;
        mUncommitted += allocSize;
        D3D12NI_ASSERT(mUsed <= mSize, "%s: Used is larger than size, probably underflowed (%ld vs %ld)", mUsed, mSize);

        Region r(size, alignedTail);
        mTail = newTail;
        CheckThreshold();
        return std::move(r);
    }

    // Another confidence check, but we should never end up here
    D3D12NI_LOG_ERROR("%s: overflow - tried to allocate past head (h: %ld, t: %ld, size: %ld)", mDebugName.c_str(), mHead, mTail, size);
    return Region();
}

// returns true if container flushed the command list
bool RingContainer::DeclareRequired(size_t size)
{
    // tail must be aligned like in ReserveInternal()
    size_t tailAligned = Internal::Utils::Align(mTail, mAlignment);

    // real size needed is aligned like tail + the space between actual tail and potential alignment
    size_t realSizeNeeded = Internal::Utils::Align(size, mAlignment) + (tailAligned - mTail);

    // count in loop-around if needed
    // (using mTail for this check instead of tailAligned because realSizeNeeded already accounted for tailAligned)
    if (mTail + realSizeNeeded > mSize)
    {
        // add the empty space from aligned tail to end of buffer
        realSizeNeeded += (mSize - tailAligned);
    }

    // if requested data will be more than the flush threshold we should flush the Command List now
    // that way we prevent potentially flushing it mid-preparing for draw
    if ((mUncommitted > 0) && (mUncommitted + realSizeNeeded > mFlushThreshold))
    {
        Internal::Profiler::Instance().MarkEvent(mProfilerSourceID, Internal::Profiler::Event::Signal);
        mFlushCallback(CheckpointType::MIDFRAME);
        return true;
    }

    return false;
}

void RingContainer::OnQueueSignal(uint64_t fenceValue)
{
    if (mUncommitted > 0)
    {
        mCheckpoints.emplace_back(mTail, fenceValue);
        mUncommitted = 0;
    }
}

void RingContainer::OnFenceSignaled(uint64_t fenceValue)
{
    while (!mCheckpoints.empty())
    {
        if (fenceValue < mCheckpoints.front().fenceValue)
        {
            // any remaining frames are not yet done
            break;
        }

        size_t frameTail = mCheckpoints.front().tail;

        // frameTail will be our new mHead - calculate new value of mUsed
        if (frameTail == mHead)
        {
            // corner-case - Ring Container got exactly 100% full.
            // mUsed can just drop to 0 in this case.
            mUsed = 0;
        }
        else if (frameTail > mHead)
        {
            // frame's tail is after head - not looped yet
            // subtract from frame's tail to old (current?) mHead
            D3D12NI_ASSERT(frameTail - mHead >= 0, "%s: Invalid frameTail - mHead %d", mDebugName.c_str(), frameTail - mHead);
            mUsed -= frameTail - mHead;
        }
        else
        {
            // frame's tail is before head - looped
            // subtract end-of-buffer data (size - mHeat) and beginning (frame's tail - 0)
            D3D12NI_ASSERT((mSize - mHead) + frameTail >= 0, "%s: Invalid size-head + tail %d", mDebugName.c_str(), (mSize - mHead) + frameTail);
            mUsed -= (mSize - mHead) + frameTail;
        }

        D3D12NI_ASSERT(mUsed <= mSize, "%s: Used is larger than size, probably underflowed (%ld vs %ld)", mDebugName.c_str(), mUsed, mSize);

        // update mHead to current frame's tail & discard frame checkpoint
        mHead = frameTail;
        mCheckpoints.pop_front();
    }
}

void RingContainer::SetDebugName(const std::string& name)
{
    mDebugName = "Ring Container '" + name + '\'';
    Internal::Profiler::Instance().RenameSource(mProfilerSourceID, mDebugName);
}

} // namespace RenderThread
} // namespace D3D12
