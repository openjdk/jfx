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
#include "D3D12NativeDevice.hpp"
#include "D3D12NativeSwapChain.hpp"

#include "internal/D3D12DeviceInformation.hpp"
#include "shaders/D3D12ShaderLibrary.hpp"

#include <vector>
#include <memory>


namespace D3D12 {

class NativeInstance: public std::enable_shared_from_this<NativeInstance>
{
    DXGIFactoryPtr mDXGIFactory;
    std::vector<DXGIAdapterPtr> mDXGIAdapters;
    std::vector<DXGI_ADAPTER_DESC1> mDXGIAdapterDescs;
    NIPtr<Shaders::ShaderLibrary> mShaderLibrary;

public:
    NativeInstance();
    ~NativeInstance();

    bool Init();
    int GetAdapterCount();
    int GetAdapterOrdinal(HMONITOR monitor);
    bool CanCreateDevice(uint32_t adapterIdx, Internal::DeviceInformation& info) const;
    bool GetAdapterInformation(uint32_t adapterIdx, Internal::AdapterInformation& info) const;
    bool GetDeviceInformation(uint32_t adapterIdx, Internal::DeviceInformation& info) const;
    bool LoadInternalShader(const std::string& name, ShaderPipelineMode mode, D3D12_SHADER_VISIBILITY visibility, const void* code, size_t codeSize);
    NIPtr<NativeDevice>* CreateDevice(int adapterOrdinal);
    NIPtr<NativeSwapChain>* CreateSwapChain(const NIPtr<NativeDevice>& device, HWND hwnd);
};

} // namespace D3D12
