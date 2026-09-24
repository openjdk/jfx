/*
 * Copyright (c) 2007, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include "D3DPipelineManager.h"


// d3d9.dll library dynamic load
HMODULE hLibD3D9 = 0;
typedef HRESULT WINAPI FnDirect3DCreate9Ex(UINT SDKVersion, IDirect3D9Ex**);

FnDirect3DCreate9Ex * pD3D9FactoryExFunction = 0;

void loadD3DLibrary() {
    wchar_t path[MAX_PATH];
    if (::GetSystemDirectory(path, sizeof(path) / sizeof(wchar_t)) != 0) {
        wcscat_s(path, MAX_PATH-1, L"\\d3d9.dll");
        hLibD3D9 = ::LoadLibrary(path);
    }
    if (hLibD3D9) {
        pD3D9FactoryExFunction = (FnDirect3DCreate9Ex*)GetProcAddress(hLibD3D9, "Direct3DCreate9Ex");
    }
}

void freeD3DLibrary() {
    if (hLibD3D9) {
        ::FreeLibrary(hLibD3D9);
        hLibD3D9 = 0;
        pD3D9FactoryExFunction = 0;
    }
}

IDirect3D9Ex * Direct3DCreate9Ex() {
    IDirect3D9Ex * pD3D = 0;
    HRESULT hr = pD3D9FactoryExFunction ? pD3D9FactoryExFunction(D3D_SDK_VERSION, &pD3D) : E_FAIL;
    return SUCCEEDED(hr) ? pD3D : 0;
}

#ifndef STATIC_BUILD
BOOL APIENTRY DllMain( HANDLE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved)
{
    switch (ul_reason_for_call) {
    case DLL_PROCESS_ATTACH:
        loadD3DLibrary();
        break;
    case DLL_PROCESS_DETACH:
        freeD3DLibrary();
        break;
    }
    return TRUE;
}
#endif // STATIC_BUILD

int getMaxSampleSupport(IDirect3D9Ex *d3d9, UINT adapter) {
    int maxSamples = 0;
    if (SUCCEEDED(d3d9->CheckDeviceMultiSampleType(adapter,
                    D3DDEVTYPE_HAL , D3DFMT_X8R8G8B8, FALSE,
                    D3DMULTISAMPLE_2_SAMPLES, NULL))) {
        const int MAX_SAMPLES_SEARCH = D3DMULTISAMPLE_16_SAMPLES;
        maxSamples = D3DMULTISAMPLE_2_SAMPLES;
        // Typically even samples are used, thus checking only even samples to
        // save time
        for (int i = maxSamples; i <= MAX_SAMPLES_SEARCH; i += 2) {
            D3DMULTISAMPLE_TYPE msType = static_cast<D3DMULTISAMPLE_TYPE>(i);
            if (SUCCEEDED(d3d9->CheckDeviceMultiSampleType(adapter,
                    D3DDEVTYPE_HAL, D3DFMT_X8R8G8B8, FALSE,
                    msType, NULL))) {
                maxSamples = i;
            } else {
                break;
            }
        }
    }
    return maxSamples;
}
