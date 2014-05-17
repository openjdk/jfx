/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DPipeline.h"

#include "D3DContext.h"
#include "D3DPipelineManager.h"

HRESULT D3DContext::InitContext(bool isVsyncEnabled) {

    D3DDEVTYPE devType = D3DPipelineManager::GetInstance()->GetDeviceType();

    RlsTraceLn1(NWT_TRACE_INFO, "D3DContext::InitContext device %d", adapterOrdinal);

    D3DPRESENT_PARAMETERS params = {};

    params.Windowed = TRUE;
    params.SwapEffect = D3DSWAPEFFECT_DISCARD;
    params.hDeviceWindow = GetDesktopWindow();
    params.PresentationInterval = isVsyncEnabled ?
        D3DPRESENT_INTERVAL_ONE :
        D3DPRESENT_INTERVAL_IMMEDIATE;

    D3DCAPS9 d3dCaps;

    HRESULT hr = pd3dObject->GetDeviceCaps(adapterOrdinal, devType, &d3dCaps);

    if (FAILED(hr)) {
        DebugPrintD3DError(hr, "D3DContext::InitContext: failed to get caps");
        return hr;
    }

    DWORD hwVertexProcessing = d3dCaps.DevCaps & D3DDEVCAPS_HWTRANSFORMANDLIGHT;
    DWORD dwBehaviorFlags = D3DCREATE_FPU_PRESERVE |
        (hwVertexProcessing ? D3DCREATE_HARDWARE_VERTEXPROCESSING : D3DCREATE_SOFTWARE_VERTEXPROCESSING);

    RlsTraceLn(NWT_TRACE_VERBOSE, (hwVertexProcessing ? "\tHARDWARE_VERTEXPROCESSING": "\tSOFTWARE_VERTEXPROCESSING"));

    if (pd3dObjectEx) {
        hr = pd3dObjectEx->CreateDeviceEx(adapterOrdinal, devType, 0,
            dwBehaviorFlags, &params, 0, &pd3dDeviceEx);
        if (SUCCEEDED(hr)) {
            pd3dDevice = addRef<IDirect3DDevice9>(pd3dDeviceEx);
        }
    } else {
        hr = pd3dObject->CreateDevice(adapterOrdinal, devType, 0,
            dwBehaviorFlags, &params, &pd3dDevice);
    }

    if (FAILED(hr)) {
        DebugPrintD3DError(hr, "D3DContext::InitContext: error creating d3d device");
        return hr;
    }

    // we do not care about D3DPOOL_SYSTEMMEM if pCtx->IsHWRasterizer()
    defaulResourcePool = pd3dObjectEx ? D3DPOOL_DEFAULT : D3DPOOL_MANAGED;

    RlsTraceLn1(NWT_TRACE_INFO, "D3DContext::InitContext: successfully created device: %d", adapterOrdinal);
    bIsHWRasterizer = (devType == D3DDEVTYPE_HAL);
    curParams = params;

    if (FAILED(hr = InitDevice(pd3dDevice))) {
        ReleaseContextResources(RELEASE_ALL);
        return hr;
    }

    InitContextCaps();

    return S_OK;
}

HRESULT D3DContext::ResetContext() {
    TraceLn(NWT_TRACE_VERBOSE, "  resetting the device");

    ReleaseContextResources(RELEASE_DEFAULT);

    HRESULT res = pd3dDevice->Reset(&curParams);

    FAILED(res)
        ? TraceImpl(NWT_TRACE_INFO, 1, "D3DContext::ResetContext: cound not reset the device: hr=%08X", res)
        : TraceImpl(NWT_TRACE_INFO, 1, "D3DContext::ResetContext: successfully reset device: %d", adapterOrdinal);

    return SUCCEEDED(res) ? InitDevice(pd3dDevice) : res;
}
