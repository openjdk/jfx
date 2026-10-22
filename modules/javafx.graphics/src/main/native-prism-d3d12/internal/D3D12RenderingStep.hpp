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

#include "D3D12Config.hpp"

#include <functional>


namespace D3D12 {
namespace Internal {

class RenderingStep
{
public:
    // Dependency callback. Should return true if step should be applied.
    // We assume the step should be unconditionally applied if there is no dependency set.
    using StepDependency = std::function<bool()>;

private:
    const bool mOptimizeApply;
    StepDependency mDependency;

protected:
    bool mIsApplied;

    virtual bool CanBeSkipped() const
    {
        return (mOptimizeApply && mIsApplied) || (mDependency && !mDependency());
    }

public:
    RenderingStep()
        : mIsApplied(false)
        , mOptimizeApply(Config::IsApiOptsEnabled())
        , mDependency()
    {}

    virtual ~RenderingStep() {}

    void ClearApplied()
    {
        mIsApplied = false;
    }

    void SetDependency(const StepDependency& dependency)
    {
        mDependency = dependency;
    }
};

template <typename T>
class RenderingDataStep: public RenderingStep
{
    bool mIsSet;

protected:
    T mParameter;

    void FlagSet()
    {
        ClearApplied();
        mIsSet = true;
    }

    virtual bool CanBeSkipped() const override final
    {
        return (!mIsSet || RenderingStep::CanBeSkipped());
    }

public:
    RenderingDataStep()
        : RenderingStep()
        , mParameter()
        , mIsSet(false)
    {}

    ~RenderingDataStep() = default;

    void Set(const T& prop)
    {
        mParameter = prop;
        FlagSet();
    }

    void Unset()
    {
        mIsSet = false;
        mParameter = T();
    }

    T& Get()
    {
        return mParameter;
    }

    bool IsSet() const
    {
        return mIsSet;
    }
};

} // namespace Internal
} // namespace D3D12
