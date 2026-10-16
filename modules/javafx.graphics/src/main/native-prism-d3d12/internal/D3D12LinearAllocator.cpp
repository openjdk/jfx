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

#include "D3D12LinearAllocator.hpp"

#include "D3D12Utils.hpp"


namespace D3D12 {
namespace Internal {

void LinearAllocator::Expand()
{
    D3D12NI_ASSERT(std::this_thread::get_id() != mRenderThreadId, "Expand() can only be called by the initializing (main) thread");

    std::unique_lock<std::mutex> lock(mChunkResetMutex);

    if (mCurrentChunk && mCurrentChunk->FullyFreed())
    {
        // reuse current chunk since it's totally empty
        mCurrentChunk->Reclaim();
        return;
    }

    if (!mAvailableChunks.empty())
    {
        mChunksInUse.emplace_back(std::move(mAvailableChunks.front()));
        mAvailableChunks.pop_front();
        mCurrentChunk = &mChunksInUse.back();
        return;
    }

    // no available chunks, allocate a new one
    D3D12NI_LOG_TRACE("--- LinearAllocator - adding new chunk size %d (%d chunks in use) ---", mSizePerChunk, mChunksInUse.size());
    mChunksInUse.emplace_back(mSizePerChunk);
    mCurrentChunk = &mChunksInUse.back();
    return;
}

LinearAllocator::LinearAllocator(size_t sizePerChunk)
    : mCurrentChunk(nullptr)
    , mChunksInUse()
    , mAvailableChunks()
    , mEmptyChunks()
    , mChunkResetMutex()
    , mSizePerChunk(sizePerChunk)
    , mRenderThreadId()
{
    Expand();
}

LinearAllocator::~LinearAllocator()
{
}

// called when frame is finished; goal is to allocate a new large enough
// chunk to prevent further allocations
void LinearAllocator::MoveToNewChunk()
{
    Expand();
}

// TODO: D3D12: Should allocators use size_t instead of uint32_t? this would affect the chunk header size
void* LinearAllocator::Allocate(uint32_t size)
{
    D3D12NI_ASSERT(std::this_thread::get_id() != mRenderThreadId, "Allocate() can only be called by the initializing (main) thread");

    // align to 8 bytes (64-bits)
    uint32_t alignedSize = Utils::Align<uint32_t>(size, 8);
    if (alignedSize > mSizePerChunk)
    {
        D3D12NI_LOG_ERROR("Allocated data size %d won't fit the maximum chunk size %d.", size, mSizePerChunk);
        return nullptr;
    }

    if (!mCurrentChunk->Fits(alignedSize))
    {
        Expand();
    }

    return mCurrentChunk->Reserve(alignedSize);
}

void LinearAllocator::Free(void* ptr)
{
    // NOTE: Below assertion is right 99.9999% of the time. The only exception is when
    // we close the application and cleanup leftover resources, ex. in ResourceManager.
    // Those will be done after RenderThread completed and we rejoined it though.
    // For cleaner exit messaging in DebugNative the assertion is commented out. Restore
    // it if there are threading issues to be debugged in this area.
    //D3D12NI_ASSERT(std::this_thread::get_id() == mRenderThreadId, "Free() can only be called by the worker/render thread");

    uint8_t* dataPtr = reinterpret_cast<uint8_t*>(ptr);
    DataHeader* header = reinterpret_cast<DataHeader*>(dataPtr - sizeof(DataHeader));

    if (header->magic != DATA_MAGIC)
    {
        D3D12NI_LOG_ERROR("Cannot free, invalid magic at data header for pointer %p", ptr);
        return;
    }

    Chunk* parentChunk = header->parentChunk; // preserving the pointer, Free() will destroy it
    parentChunk->Free(ptr);
    if (parentChunk->FullyFreed())
    {
        std::unique_lock<std::mutex> lock(mChunkResetMutex);

        if (parentChunk == mCurrentChunk)
        {
            parentChunk->Reclaim();
            return;
        }

        // Clean-up Chunk and move it to empty chunks list
        // We will reclaim it via ResetChunks() after the frame ends
        for (std::list<Chunk>::iterator it = mChunksInUse.begin(); it != mChunksInUse.end(); ++it)
        {
            Chunk& c = *it;
            if (parentChunk->mPtr == c.mPtr)
            {
                mEmptyChunks.emplace_back(std::move(*it));
                mChunksInUse.erase(it);
                return;
            }
        }

        D3D12NI_ASSERT(0, "Couldn't find parent chunk on the chunks-in-use list. This should not happen.");
    }
}

void LinearAllocator::ResetChunks()
{
    D3D12NI_ASSERT(std::this_thread::get_id() != mRenderThreadId, "Allocate() can only be called by the initializing (main) thread");

    std::unique_lock<std::mutex> lock(mChunkResetMutex);

#if DEBUG
    uint32_t ctr = 0;

    if (mEmptyChunks.size() > 0)
    {
        D3D12NI_LOG_TRACE("--- LinearAllocator Reset - Garbage-collecting %d chunks: ---", mEmptyChunks.size());
        for (Chunk& c: mEmptyChunks)
        {
            D3D12NI_LOG_TRACE("        -> #%d: %d bytes", ctr, c.mSize);
            ctr++;
        }
    }

    ctr = 0;
#endif

    for (std::list<Chunk>::iterator it = mEmptyChunks.begin(); it != mEmptyChunks.end(); ++it)
    {
        if (it->mSize == mSizePerChunk)
        {
            D3D12NI_LOG_TRACE("--- LinearAllocator Reset - Preserving #%d - %d-byte chunk---", ctr, it->mSize);
            it->Reclaim();
            mAvailableChunks.emplace_back(std::move(*it));
        }

        #if DEBUG
        ctr++;
        #endif
    }

    mEmptyChunks.clear();

    D3D12NI_LOG_TRACE("--- LinearAllocator Reset - Garbage-collection complete ---");
}

} // namespace Internal
} // namespace D3D12
