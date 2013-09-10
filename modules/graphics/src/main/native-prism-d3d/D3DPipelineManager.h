/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DPipeline.h"
#include "D3DContext.h"

#include "interface.h"

struct D3DAdapter {
    D3DContext *pd3dContext;
    DWORD state;
    HWND fsFocusWindow;
};

interface IConfig {
    virtual int getInt(cstr name)=0;
    virtual bool getBool(cstr name)=0;
};

IDirect3D9 * Direct3DCreate9();
IDirect3D9Ex * Direct3DCreate9Ex();

template <class T> inline T * addRef(T *t) {
    t->AddRef();
    return t;
}

template <class T> inline int SafeRelease(T* t) {
    return t ? t->Release() : 0;
}


class D3DPipelineManager
{
public:
    // creates and initializes instance of D3DPipelineManager, may return NULL
    static D3DPipelineManager* CreateInstance(IConfig &);
    // deletes the single instance of the manager
    static void DeleteInstance();
    // returns the single instance of the manager, may return NULL
    static D3DPipelineManager* GetInstance(void) { return pMgr; }

    HRESULT GetD3DContext(UINT adapterOrdinal, D3DContext **ppd3dContext);

    HRESULT HandleLostDevices();
    // Checks if adapters were added or removed, or if the order had changed
    // (which may happen with primary display is changed). If that's the case
    // releases current adapters and d3d9 instance, reinitializes the pipeline.
    // @param *monHds list of monitor handles retrieved from GDI
    // @param monNum number of gdi monitors
    static
    HRESULT HandleAdaptersChange(HMONITOR *monHds, UINT monNum);
    // returns depth stencil buffer format matching adapterFormat and render target
    // format for the device specified by adapterOrdinal/devType
    D3DFORMAT GetMatchingDepthStencilFormat(UINT adapterOrdinal,
                                            D3DFORMAT adapterFormat,
                                            D3DFORMAT renderTargetFormat);

    LPDIRECT3D9 GetD3DObject() { return pd3d9; }
    D3DDEVTYPE GetDeviceType() { return devType; }

    D3DMULTISAMPLE_TYPE GetUserMultiSampleType() { return userMultiSampleType; };

    // returns adapterOrdinal given a HMONITOR handle
    UINT GetAdapterOrdinalByHmon(HMONITOR hMon);

    UINT GetAdapterCount() const { return adapterCount; }

    // returns warning message if warning is true during driver check.
    static char const * GetErrorMessage();
    static void SetErrorMessage(char const *msg);
    static void SetErrorMessageV(char const *msg, ...);

private:
    D3DPipelineManager(IConfig &);

    // Creates a Direct3D9 object and initializes adapters.
    HRESULT InitD3D(IConfig &);
    // Releases adapters, Direct3D9 object and the d3d9 library.
    HRESULT ReleaseD3D();

    // selects the device type based on user input and available
    // device types
    D3DDEVTYPE SelectDeviceType();

    // creates array of adapters (releases the old one first)
    HRESULT InitAdapters(IConfig &);
    // releases each adapter's context, and then releases the array
    HRESULT ReleaseAdapters();

    // returns S_OK if the adapter is capable of running the Direct3D
    // pipeline
    HRESULT D3DEnabledOnAdapter(UINT Adapter);
    HRESULT CheckAdaptersInfo(IConfig &);
    HRESULT CheckDeviceCaps(UINT Adapter);

public:
    // Check the OS, succeeds if the OS is XP or newer client-class OS
    static HRESULT CheckOSVersion();

    // given VendorId, DeviceId and driver version, checks against a database
    // of known bad hardware/driver combinations.
    // If the driver version is not known MAX_VERSION can be used
    // which is guaranteed to satisfy the check
    static HRESULT CheckForBadHardware(DWORD vId, DWORD dId, LONGLONG version);

    static HRESULT CheckForBadHardware(D3DADAPTER_IDENTIFIER9 const &id) {
        return CheckForBadHardware(id.VendorId, id.DeviceId, id.DriverVersion.QuadPart);
    }

private:

    // current adapter count
    UINT adapterCount;
    // Pointer to Direct3D9 Object mainained by the pipeline manager
    LPDIRECT3D9 pd3d9;
    IDirect3D9Ex * pd3d9Ex;

    D3DDEVTYPE devType;

    D3DAdapter *pAdapters;
    // instance of this object
    static D3DPipelineManager* pMgr;

    D3DMULTISAMPLE_TYPE userMultiSampleType;
};

#define OS_UNDEFINED    (0 << 0)
#define OS_VISTA        (1 << 0)
#define OS_WINSERV_2008 (1 << 1)
#define OS_WINXP        (1 << 2)
#define OS_WINXP_64     (1 << 3)
#define OS_WINSERV_2003 (1 << 4)
#define OS_ALL (OS_VISTA|OS_WINSERV_2008|OS_WINXP|OS_WINXP_64|OS_WINSERV_2003)
#define OS_UNKNOWN      (~OS_ALL)
BOOL D3DPPLM_OsVersionMatches(USHORT osInfo);

namespace OS {
    bool isWinverAtleast(int maj, int min);

    inline bool isWindowsXPorNewer() {
        return isWinverAtleast(5, 1);
    }

    inline bool isWindows7orNewer() {
        return isWinverAtleast(6, 1);
    }
}

