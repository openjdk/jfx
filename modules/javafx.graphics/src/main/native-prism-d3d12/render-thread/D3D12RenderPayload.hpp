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

#pragma once

#include "../D3D12Common.hpp"

#include "../D3D12NativeTexture.hpp"

#include "D3D12LinearAllocator.hpp"
#include "D3D12RenderThreadExecutable.hpp"
#include "D3D12Waitable.hpp"

#include <array>


namespace D3D12 {
namespace RenderThread {

// collects steps that need to be processed by the Rendering Thread
class RenderPayload
{
public:
    enum class StepAddResult: uint8_t
    {
        SUCCESS = 0,
        FAILED,
        PAYLOAD_AT_LIMIT
    };

private:
    static const uint32_t PAYLOAD_SIZE = 10240;

    // this limit should be big enough to fit the entire RenderingContext::Apply() or ApplyCompute()
    // this way we can in one batch apply all Pipeline changes at once and immediately after order a draw/dispatch call
    static const uint32_t PAYLOAD_LIMIT = PAYLOAD_SIZE - 24;
    using StepList = std::array<RenderThreadExecutablePtr, PAYLOAD_SIZE>;

    NIPtr<Internal::Waitable> mWaitable;
    StepList mSteps;
    uint32_t mCurrentStep;

public:
    RenderPayload()
        : mWaitable(std::make_shared<Internal::Waitable>())
        , mSteps()
        , mCurrentStep(0)
    {
    }

    StepAddResult AddStep(RenderThreadExecutablePtr&& executable)
    {
        if (!executable)
        {
            D3D12NI_LOG_ERROR("Provided a null executable. This should not happen and must be fixed.");
            return StepAddResult::FAILED;
        }

        if (mCurrentStep >= mSteps.size())
        {
            D3D12NI_LOG_ERROR("Render Thread Payload size exceeded. Please fix this.");
            return StepAddResult::FAILED;
        }

        mSteps[mCurrentStep] = std::move(executable);
        mCurrentStep++;
        return (mCurrentStep > PAYLOAD_LIMIT) ? StepAddResult::PAYLOAD_AT_LIMIT : StepAddResult::SUCCESS;
    }

    bool ApplySteps(const RenderThreadContextPtr& context)
    {
        for (uint32_t i = 0; i < mCurrentStep; ++i)
        {
            mSteps[i]->Execute(context);
        }

        return true;
    }

    bool HasWork() const
    {
        return (mCurrentStep > 0);
    }

    uint32_t StepsCount() const
    {
        return mCurrentStep;
    }

    inline const NIPtr<Internal::Waitable>& GetWaitable() const
    {
        return mWaitable;
    }
};

using RenderPayloadPtr = std::unique_ptr<RenderPayload, Internal::LinearAllocatorDeleter<RenderPayload>>;

} // namespace RenderThread
} // namespace D3D12
