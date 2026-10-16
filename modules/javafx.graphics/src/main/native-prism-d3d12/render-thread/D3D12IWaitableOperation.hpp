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

#include "D3D12Common.hpp"


namespace D3D12 {
namespace RenderThread {

/**
 * Interface which defines functions called at stages of Signal-Wait sequence
 * of GPU execution.
 *
 * Goal of it is to provide some way of easily hooking up routines which are
 * called when Command Queue has a Signal operation applied to it, and after
 * the associated Fence is signaled.
 *
 * Derived class implementing below methods must call NativeDevice::
 */
class IWaitableOperation
{
public:
    /**
     * Called when some part of the backend calls NativeDevice::Signal().
     * @p fenceValue marks the value which will be passed to D3D12 and later
     * on passed to OnFenceSignaled().
     *
     * Most commonly used to mark the point in time as "during execution" and
     * prepare to free it later on.
     */
    virtual void OnQueueSignal(uint64_t fenceValue) = 0;

    /**
     * Called when any backend part waits on a Waitable and the wait completes.
     * @p fenceValue will have the same value as during earlier OnQueueSignal()
     * call.
     *
     * Can also be thought as when GPU execution has completed. Can be used to
     * mark resources as freed and no longer under GPU utilization.
     */
    virtual void OnFenceSignaled(uint64_t fenceValue) = 0;
};

} // namespace RenderThread
} // namespace D3D12
