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

#pragma once

#include "../D3D12Common.hpp"

#include "internal/D3D12Waitable.hpp"

#include <deque>


namespace D3D12 {
namespace RenderThread {

// CheckpointType defined in D3D12Common.hpp

/**
 * This class collects Pipeline "checkpoints" - points in time where certain
 * amount of work has been done.
 *
 * All Signal operations and Waitables are registered here in a queue, in order
 * to be waited on later (if needed).
 */
class CheckpointQueue
{
private:
    struct Checkpoint
    {
        CheckpointType type;
        Internal::Waitable waitable;

        Checkpoint(CheckpointType type, Internal::Waitable&& waitable)
            : type(type)
            , waitable(std::move(waitable))
        {}
    };

    std::deque<Checkpoint> mQueue;
    uint32_t mTotalCheckpointCount = 0;
    uint32_t mEndframeCount = 0;

    bool IsOnlyOneType(CheckpointType type);
    bool HasFlag(CheckpointType type, CheckpointType flag);

public:
    CheckpointQueue() = default;
    ~CheckpointQueue() = default;

    bool AddCheckpoint(CheckpointType type, Internal::Waitable&& waitable);
    bool WaitForNextCheckpoint(CheckpointType type);
    void PrintStats();

    bool Empty() const
    {
        return mQueue.empty();
    }
};

} // namespace RenderThread
} // namespace D3D12
