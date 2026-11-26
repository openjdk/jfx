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

#include <mutex>


namespace D3D12 {
namespace Internal {

class DeviceSpecificDebugContext
{
    D3D12DevicePtr mD3D12Device;
    D3D12DebugDevicePtr mD3D12DebugDevice;
    D3D12InfoQueuePtr mD3D12InfoQueue;
    DWORD mD3D12MessageCallbackCookie;
    std::mutex mErrorMessageReportMutex;
    bool mUsesMessageCallback;
    bool mIsEnabled;
    bool mIsDREDEnabled;

public:
    DeviceSpecificDebugContext(bool isEnabled, bool dredEnabled);

    bool InitDeviceDebug(const D3D12DevicePtr& device);
    void ReleaseAndReportLiveObjects();
    bool ExamineDeviceRemoved();
    void ReportErrorMessages();

    inline const D3D12DevicePtr& GetDevice() const
    {
        return mD3D12Device;
    }
};

using DebugContextPtr = std::shared_ptr<DeviceSpecificDebugContext>;

class Debug
{
    DXGIDebugPtr mDXGIDebug;
    D3D12DebugPtr mD3D12Debug;
    DXGIInfoQueuePtr mDXGIInfoQueue;
    std::list<DebugContextPtr> mDebugContexts;
    std::mutex mDebugContextsMutex;
    bool mIsEnabled;
    bool mIsDREDEnabled;

    Debug();
    ~Debug();

    Debug(const Debug&) = delete;
    Debug(Debug&&) = delete;
    Debug& operator=(const Debug&) = delete;
    Debug& operator=(Debug&&) = delete;

public:
    static Debug& Instance();

    bool Init();
    DebugContextPtr InitDeviceDebug(const D3D12DevicePtr& device);
    void ReleaseDeviceAndReportLiveObjects(const DebugContextPtr& device);
    void ReleaseInstanceAndReportLiveObjects();
    bool IsEnabled();
};

} // namespace Internal
} // namespace D3D12
