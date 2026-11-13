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
#include "D3D12Utils.hpp"

#include <deque>


namespace D3D12 {
namespace RenderThread {

/*
 * Base class for implementations like Ring Buffer or Ring Descriptor Heap.
 *
 * It's goal is to unify common operations which are done to achieve the
 * "ringness" of the resource.
 */
class RingContainer: public IWaitableOperation
{
private:
    size_t mFlushThreshold;
    size_t mUsed; // total data used inside the ring buffer
    size_t mUncommitted; // total data that has not been committed yet
    size_t mHead; // head of all used data
    CheckpointCallback mFlushCallback;
    CheckpointCallback mWaitCallback;

    struct Checkpoint
    {
        size_t tail;
        uint64_t fenceValue;

        Checkpoint(size_t t, uint64_t f)
            : tail(t)
            , fenceValue(f)
        {}
    };
    std::deque<Checkpoint> mCheckpoints;

    std::string mDebugName;

    void FlushCommandList();
    void CheckThreshold();
    bool AwaitNextCheckpoint(size_t needed);
    void PrintHumanReadableSize();

protected:
    /*
     * Internal Region structure, which returns the offset at which the Ring container
     * has reserved some space and how big that space is.
     *
     * Derived classes should use this to form their own return objects (ex. by mixing
     * them in with CPU/GPU pointers).
     */
    struct Region
    {
        size_t size; // reserved region's size
        size_t offsetFromStart; // region's offset from the start of the buffer
                                // pointing at the beginning of the reserved area

        Region()
            : Region(0, 0)
        {}

        Region(size_t s, size_t o)
            : size(s)
            , offsetFromStart(o)
        {}
    };

    NIPtr<NativeDevice> mNativeDevice;
    size_t mSize;
    size_t mAlignment;
    size_t mTail; // tail of all used data
    uint32_t mProfilerSourceID;

    // Initializes some internal common fields. Should be called at
    // the dedicated Init() call
    bool InitInternal(size_t flushThreshold, size_t alignment, size_t totalSize);

    // Internal offset-pointer calculation function which updates head/tail
    // offsets and reserves some space on the Ring container. It is up to the
    // derived class to interpret these offsets onto its resource.
    Region ReserveInternal(size_t size);

public:
    RingContainer(const NIPtr<NativeDevice>& nativeDevice, const CheckpointCallback& flushCallback, const CheckpointCallback& waitCallback);
    virtual ~RingContainer() = 0;

    /**
     * Informs Ring Container that we will require @p size bytes/slots of data in order to
     * successfully submit the next Draw call. Ring Container will then ensure that allocation
     * of next @p size data will not be interrupted by a mid-frame flush.
     *
     * @return true when Command List flush was performed, false otherwise.
     */
    bool DeclareRequired(size_t size);

    /**
     * Set a checkpoint in the ring resource. This will register the block of memory that was
     * used up to this point as belonging to just-recorded Command List and free it up after
     * associated Fence is signaled.
     */
    void OnQueueSignal(uint64_t fenceValue) override;

    /**
     * Called when appropriate fence is signaled and the wait is successful. This will inform
     * the ring container that previously registered block of memory is now available to be reused.
     */
    void OnFenceSignaled(uint64_t fenceValue) override;

    void SetDebugName(const std::string& name);

    inline size_t Size() const
    {
        return mSize;
    }

    inline size_t FlushThreshold() const
    {
        return mFlushThreshold;
    }

    inline bool HasUncommittedData() const
    {
        return (mUncommitted > 0);
    }
};

} // namespace RenderThread
} // namespace D3D12
