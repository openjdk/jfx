/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12CheckpointQueue.hpp"

#include "internal/D3D12Logger.hpp"

#include <type_traits>


namespace D3D12 {
namespace RenderThread {

bool CheckpointQueue::IsOnlyOneType(CheckpointType type)
{
    std::underlying_type_t<CheckpointType> t = static_cast<std::underlying_type_t<CheckpointType>>(type);

    uint32_t counter = 0;
    while (t != 0)
    {
        counter += (t & 0x1);
        if (counter > 1) return false;
        t >>= 1;
    }

    return (counter == 1);
}

bool CheckpointQueue::HasFlag(CheckpointType type, CheckpointType flag)
{
    std::underlying_type_t<CheckpointType> t = static_cast<std::underlying_type_t<CheckpointType>>(type);
    std::underlying_type_t<CheckpointType> x = static_cast<std::underlying_type_t<CheckpointType>>(flag);
    return ((t & x) != 0);
}

bool CheckpointQueue::AddCheckpoint(CheckpointType type, Internal::Waitable&& waitable)
{
    D3D12NI_ASSERT(IsOnlyOneType(type), "AddCheckpoint only allows one type as an argument");

    if (type == CheckpointType::ENDFRAME) mEndframeCount++;
    mTotalCheckpointCount++;

    mQueue.emplace_back(type, std::move(waitable));
    return true;
}

bool CheckpointQueue::WaitForNextCheckpoint(CheckpointType type)
{
    while (!mQueue.empty())
    {
        Checkpoint& point = mQueue.front();

        if (!point.waitable.Wait())
        {
            D3D12NI_LOG_ERROR("Failure while waiting for Checkpoint");
            return false;
        }

        CheckpointType awaitedType = point.type;
        mQueue.pop_front();

        if (HasFlag(awaitedType, type)) break;
    }

    return true;
}

void CheckpointQueue::PrintStats()
{
    D3D12NI_LOG_DEBUG("CheckpointQueue - Collected total %d checkpoints in %d frames (%f waits on average)",
        mTotalCheckpointCount, mEndframeCount, static_cast<float>(mTotalCheckpointCount) / static_cast<float>(mEndframeCount)
    );
}

} // namespace RenderThread
} // namespace D3D12
