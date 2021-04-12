/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include "D3DBadHardware.h"
#include "D3DPipelineManager.h"

// state of the adapter prior to initialization
#define CONTEXT_NOT_INITED 0
// this state is set if adapter initialization had failed
#define CONTEXT_INIT_FAILED (-1)
// this state is set if adapter was successfully created
#define CONTEXT_CREATED 1

static const size_t MAX_WARNING_MESSAGE_LEN = 256;
static char messageBuffer[MAX_WARNING_MESSAGE_LEN];
static char* warningMessage = NULL;

D3DPipelineManager *D3DPipelineManager::pMgr = NULL;

bool OS::isWinverAtleast(int maj, int min) {
    DWORD winVer = ::GetVersion();
    return maj < LOBYTE(LOWORD(winVer)) ||
        maj == LOBYTE(LOWORD(winVer)) && min <= HIBYTE(LOWORD(winVer));
}

inline bool isForcedGPU(IConfig &cfg) { return cfg.getBool("forceGPU"); }

D3DPipelineManager * D3DPipelineManager::CreateInstance(IConfig &cfg) {
    pMgr = new D3DPipelineManager(cfg);
    if (FAILED(pMgr->InitD3D(cfg))) {
        SAFE_DELETE(pMgr);
    }

    return pMgr;
}

void D3DPipelineManager::DeleteInstance() {
    TraceLn(NWT_TRACE_INFO, "D3DPPLM::DeleteInstance()");
    pMgr->ReleaseD3D();
    delete pMgr;
    pMgr = 0;
}

D3DPipelineManager::D3DPipelineManager(IConfig &cfg)
{
    pd3d9 = NULL;
    pd3d9Ex = NULL;
    pAdapters = NULL;
    adapterCount = 0;
    isVsyncEnabled = cfg.getBool("isVsyncEnabled");

    devType = SelectDeviceType();

}

HRESULT D3DPipelineManager::ReleaseD3D()
{
    TraceLn(NWT_TRACE_INFO, "D3DPPLM::ReleaseD3D()");

    ReleaseAdapters();

    SAFE_RELEASE(pd3d9);
    SAFE_RELEASE(pd3d9Ex);

    return S_OK;
}

// Creates a Direct3D9 object and initializes adapters.
// If succeeded, returns S_OK, otherwise returns the error code.
HRESULT D3DPipelineManager::InitD3D(IConfig &cfg)
{
    bool useD3D9Ex = !cfg.getBool("disableD3D9Ex");
    bool verbose = cfg.getBool("verbose");

    pd3d9Ex = 0;
    if (useD3D9Ex && OS::isWindows7orNewer()) {
        pd3d9Ex = Direct3DCreate9Ex();
    }

    pd3d9 = pd3d9Ex ? addRef<IDirect3D9>(pd3d9Ex) : Direct3DCreate9();

    if (verbose) {
        if (pd3d9Ex) {
            fprintf(stderr, "D3DPipelineManager: Created D3D9Ex device\n");
        } else if (pd3d9) {
            fprintf(stderr, "D3DPipelineManager: Created D3D9 device\n");
        } else {
            fprintf(stderr, "D3DPipelineManager: Unable to create D3D9 device\n");
        }
        fflush(stderr);
    }

    if (pd3d9 == NULL) {
        SetErrorMessage("InitD3D: unable to create IDirect3D9 object");
        RlsTraceLn(NWT_TRACE_ERROR, GetErrorMessage());
        return E_FAIL;
    }

    HRESULT res = InitAdapters(cfg);
    if (FAILED(res)) {
        RlsTraceLn(NWT_TRACE_ERROR, "InitD3D: failed to init adapters");
        ReleaseD3D();
    }

    return res;
}

HRESULT D3DPipelineManager::ReleaseAdapters()
{
    TraceLn(NWT_TRACE_INFO, "D3DPPLM::ReleaseAdapters()");

    if (pAdapters != NULL) {
        for (UINT i = 0; i < adapterCount; i++) {
            if (pAdapters[i].pd3dContext != NULL) {
                pAdapters[i].pd3dContext->release();
            }
        }
        delete[] pAdapters;
        pAdapters = NULL;
    }
    return S_OK;
}

HRESULT D3DPipelineManager::InitAdapters(IConfig &cfg)
{
    TraceLn(NWT_TRACE_INFO, "D3DPPLM::InitAdapters()");

    adapterCount = pd3d9->GetAdapterCount();

    if (adapterCount == 0) {
        RlsTraceLn(NWT_TRACE_WARNING, "Zero adapters found");
    }
    pAdapters = new D3DAdapter[adapterCount];
    if (pAdapters == NULL) {
        SetErrorMessage("InitAdapters: out of memory");
        RlsTraceLn(NWT_TRACE_ERROR, GetErrorMessage());
        adapterCount = 0;
        return E_FAIL;
    }
    ZeroMemory(pAdapters, adapterCount * sizeof(D3DAdapter));

    HRESULT res = CheckAdaptersInfo(cfg);

    if (FAILED(res)) {
        SetErrorMessage("Adapter validation failed for all adapters");
    }

    return res;
}

// static
HRESULT
D3DPipelineManager::CheckOSVersion()
{
    // require Windows XP or newer OS
    if (OS::isWindowsXPorNewer()) {
        TraceLn(NWT_TRACE_INFO,
                   "D3DPPLM::CheckOSVersion: Windows XP or newer OS detected, passed");
        return S_OK;
    }
    RlsTraceLn(NWT_TRACE_ERROR,
                  "D3DPPLM::CheckOSVersion: Windows 2000 or earlier OS detected, failed");
    return E_FAIL;
}

BOOL D3DPPLM_OsVersionMatches(USHORT osInfo) {
    static USHORT currentOS = OS_UNDEFINED;

    if (currentOS == OS_UNDEFINED) {
        BOOL bVersOk;
        OSVERSIONINFOEX osvi;

        ZeroMemory(&osvi, sizeof(OSVERSIONINFOEX));
        osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);

        bVersOk = GetVersionEx((OSVERSIONINFO *) &osvi);

        RlsTrace(NWT_TRACE_INFO, "[I] OS Version = ");
        if (bVersOk && osvi.dwPlatformId == VER_PLATFORM_WIN32_NT &&
            osvi.dwMajorVersion > 4)
        {
            if (osvi.dwMajorVersion > 6 || (osvi.dwMajorVersion == 6 && osvi.dwMinorVersion >= 3)) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    RlsTrace(NWT_TRACE_INFO, "OS_WIN8.1 or newer\n");
                    currentOS = OS_WIN81;
                } else {
                    RlsTrace(NWT_TRACE_INFO, "OS_WINSERV_2012_R2 or newer\n");
                    currentOS = OS_WINSERV_2012_R2;
                }
            } else if (osvi.dwMajorVersion == 6 && osvi.dwMinorVersion == 2) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    RlsTrace(NWT_TRACE_INFO, "OS_WIN8\n");
                    currentOS = OS_WIN8;
                } else {
                    RlsTrace(NWT_TRACE_INFO, "OS_WINSERV_2012\n");
                    currentOS = OS_WINSERV_2012;
                }
            } else if (osvi.dwMajorVersion == 6 && osvi.dwMinorVersion == 1) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    RlsTrace(NWT_TRACE_INFO, "OS_WIN7\n");
                    currentOS = OS_WIN7;
                } else {
                    RlsTrace(NWT_TRACE_INFO, "OS_WINSERV_2008_R2\n");
                    currentOS = OS_WINSERV_2008_R2;
                }
            } else if (osvi.dwMajorVersion == 6 && osvi.dwMinorVersion == 0) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    RlsTrace(NWT_TRACE_INFO, "OS_VISTA\n");
                    currentOS = OS_VISTA;
                } else {
                    RlsTrace(NWT_TRACE_INFO, "OS_WINSERV_2008\n");
                    currentOS = OS_WINSERV_2008;
                }
            } else if (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 2) {
                if (osvi.wProductType == VER_NT_WORKSTATION) {
                    RlsTrace(NWT_TRACE_INFO, "OS_WINXP_64\n");
                    currentOS = OS_WINXP_64;
                } else {
                    RlsTrace(NWT_TRACE_INFO, "OS_WINSERV_2003\n");
                    currentOS = OS_WINSERV_2003;
                }
            } else if (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 1) {
                RlsTrace(NWT_TRACE_INFO, "OS_WINXP ");
                currentOS = OS_WINXP;
                if (osvi.wSuiteMask & VER_SUITE_PERSONAL) {
                    RlsTrace(NWT_TRACE_INFO, "Home\n");
                } else {
                    RlsTrace(NWT_TRACE_INFO, "Pro\n");
                }
            } else {
                RlsTrace2(NWT_TRACE_INFO,
                            "OS_UNKNOWN: dwMajorVersion=%d dwMinorVersion=%d\n",
                             osvi.dwMajorVersion, osvi.dwMinorVersion);
                currentOS = OS_UNKNOWN;
            }
        } else {
            if (bVersOk) {
                RlsTrace2(NWT_TRACE_INFO,
                             "OS_UNKNOWN: dwPlatformId=%d dwMajorVersion=%d\n",
                             osvi.dwPlatformId, osvi.dwMajorVersion);
            } else {
                RlsTrace(NWT_TRACE_INFO,"OS_UNKNOWN: GetVersionEx failed\n");
            }
            currentOS = OS_UNKNOWN;
        }
    }
    return (currentOS & osInfo);
}

// static
HRESULT
D3DPipelineManager::CheckForBadHardware(DWORD vId, DWORD dId, LONGLONG version)
{
    DWORD vendorId, deviceId;
    UINT adapterInfo = 0;

    TraceLn(NWT_TRACE_INFO, "D3DPPLM::CheckForBadHardware");

    SetErrorMessage(0);
    while ((vendorId = badHardware[adapterInfo].VendorId) != 0x0000 &&
           (deviceId = badHardware[adapterInfo].DeviceId) != 0x0000)
    {
        if (vendorId == vId && (deviceId == dId || deviceId == ALL_DEVICEIDS)) {
            LONGLONG goodVersion = badHardware[adapterInfo].DriverVersion;
            USHORT osInfo = badHardware[adapterInfo].OsInfo;
            // the hardware check fails if:
            // - we have an entry for this OS and
            // - hardware is bad for all driver versions (NO_VERSION), or
            //   we have a driver version which is older than the
            //   minimum required for this OS
            if (D3DPPLM_OsVersionMatches(osInfo) &&
                (goodVersion == NO_VERSION || version < goodVersion))
            {
                RlsTraceLn2(NWT_TRACE_ERROR,
                    "D3DPPLM::CheckForBadHardware: found matching "\
                    "hardware: VendorId=0x%04x DeviceId=0x%04x",
                    vendorId, deviceId);

                if (goodVersion != NO_VERSION) {
                    // this was a match by the driver version
                    LARGE_INTEGER li;
                    li.QuadPart = goodVersion;
                    SetErrorMessageV(
                            "WARNING: bad driver version detected, device disabled. "
                            "Please update your driver to at least version %d.%d.%d.%d",
                            HIWORD(li.HighPart), LOWORD(li.HighPart),
                            HIWORD(li.LowPart), LOWORD(li.LowPart));
                } else {
                    // this was a match by the device (no good driver for this device)
                    SetErrorMessage("WARNING: Unsupported video adapter found, device disabled");
                }
                RlsTraceLn(NWT_TRACE_ERROR, GetErrorMessage());

                return D3DERR_INVALIDDEVICE;
            }
        }
        adapterInfo++;
    }

    return S_OK;
}

char const * D3DPipelineManager::GetErrorMessage() {
    return warningMessage;
}

// we are safe about overrun for messageBuffer, its size is constant
// we use strncpy and _vsnprintf which takes the length of the buffer
#pragma warning(disable:4996)

void D3DPipelineManager::SetErrorMessage(char const *msg) {
    if (msg) {
        warningMessage = messageBuffer;
        strncpy(messageBuffer, msg, MAX_WARNING_MESSAGE_LEN-1);
        messageBuffer[MAX_WARNING_MESSAGE_LEN-1] = 0;
    } else {
        warningMessage = 0;
    }
}

void D3DPipelineManager::SetErrorMessageV(char const *msg, ...) {
    va_list argList; va_start(argList, msg);
    int retValue = _vsnprintf(messageBuffer, MAX_WARNING_MESSAGE_LEN, msg, argList);

    // Make sure a null-terminator is appended end of buffer in case
    // a truncation has occured.
    if (retValue < 0) {
        RlsTraceLn(NWT_TRACE_ERROR, "D3D: Waring message buffer overflow, message truncated.\n");
    }
    messageBuffer[MAX_WARNING_MESSAGE_LEN - 1] = '\0';
    warningMessage = messageBuffer;
}

#pragma warning(default:4996)

void traceAdapter(UINT Adapter, D3DADAPTER_IDENTIFIER9 const &aid, HMONITOR hMon) {
    RlsTraceLn1(NWT_TRACE_INFO, "Adapter Ordinal  : %d", Adapter);
    RlsTraceLn1(NWT_TRACE_INFO, "Adapter Handle   : 0x%x", hMon);
    RlsTraceLn1(NWT_TRACE_INFO, "Description      : %s",
                    aid.Description);
    RlsTraceLn2(NWT_TRACE_INFO, "GDI Name, Driver : %s, %s",
                    aid.DeviceName, aid.Driver);
    RlsTraceLn1(NWT_TRACE_INFO, "Vendor Id        : 0x%04x",
                    aid.VendorId);
    RlsTraceLn1(NWT_TRACE_INFO, "Device Id        : 0x%04x",
                    aid.DeviceId);
    RlsTraceLn1(NWT_TRACE_INFO, "SubSys Id        : 0x%x",
                    aid.SubSysId);
    RlsTraceLn4(NWT_TRACE_INFO, "Driver Version   : %d.%d.%d.%d",
                    HIWORD(aid.DriverVersion.HighPart),
                    LOWORD(aid.DriverVersion.HighPart),
                    HIWORD(aid.DriverVersion.LowPart),
                    LOWORD(aid.DriverVersion.LowPart));
    RlsTrace3(NWT_TRACE_INFO,
                    "[I] GUID             : {%08X-%04X-%04X-",
                    aid.DeviceIdentifier.Data1,
                    aid.DeviceIdentifier.Data2,
                    aid.DeviceIdentifier.Data3);
    RlsTrace4(NWT_TRACE_INFO, "%02X%02X-%02X%02X",
                    aid.DeviceIdentifier.Data4[0],
                    aid.DeviceIdentifier.Data4[1],
                    aid.DeviceIdentifier.Data4[2],
                    aid.DeviceIdentifier.Data4[3]);
    RlsTrace4(NWT_TRACE_INFO, "%02X%02X%02X%02X}\n",
                    aid.DeviceIdentifier.Data4[4],
                    aid.DeviceIdentifier.Data4[5],
                    aid.DeviceIdentifier.Data4[6],
                    aid.DeviceIdentifier.Data4[7]);
}

HRESULT D3DPipelineManager::CheckAdaptersInfo(IConfig &cfg)
{
    D3DADAPTER_IDENTIFIER9 aid;
    UINT failedAdaptersCount = 0;

    RlsTraceLn(NWT_TRACE_INFO, "CheckAdaptersInfo");
    RlsTraceLn(NWT_TRACE_INFO, "------------------");
    for (UINT Adapter = 0; Adapter < adapterCount; Adapter++) {

        if (FAILED(pd3d9->GetAdapterIdentifier(Adapter, 0, &aid))) {
            pAdapters[Adapter].state = CONTEXT_INIT_FAILED;
            failedAdaptersCount++;
            continue;
        }

        traceAdapter(Adapter, aid, pd3d9->GetAdapterMonitor(Adapter));

        if ((!isForcedGPU(cfg) && FAILED(CheckForBadHardware(aid))) ||
            FAILED(CheckDeviceCaps(Adapter))  ||
            FAILED(D3DEnabledOnAdapter(Adapter)))
        {
            pAdapters[Adapter].state = CONTEXT_INIT_FAILED;
            failedAdaptersCount++;
        }
        RlsTraceLn(NWT_TRACE_INFO, "------------------");
    }

    if (failedAdaptersCount == adapterCount) {
        RlsTraceLn(NWT_TRACE_ERROR,
                      "D3DPPLM::CheckAdaptersInfo: no suitable adapters found");
        return E_FAIL;
    }

    return S_OK;
}

D3DDEVTYPE D3DPipelineManager::SelectDeviceType()
{
    char *pRas = NULL;
    size_t size = 0;

    D3DDEVTYPE dtype = D3DDEVTYPE_HAL;
    if ((_dupenv_s(&pRas, &size, "NWT_D3D_RASTERIZER") == 0)
            && (pRas != NULL)) {
        RlsTrace(NWT_TRACE_WARNING, "[W] D3DPPLM::SelectDeviceType: ");
        if (strncmp(pRas, "ref", 3) == 0 || strncmp(pRas, "rgb", 3) == 0) {
            RlsTrace(NWT_TRACE_WARNING, "ref rasterizer selected");
            dtype = D3DDEVTYPE_REF;
        } else if (strncmp(pRas, "hal",3) == 0 || strncmp(pRas, "tnl",3) == 0) {
            RlsTrace(NWT_TRACE_WARNING, "hal rasterizer selected");
            dtype = D3DDEVTYPE_HAL;
        } else if (strncmp(pRas, "nul", 3) == 0) {
            RlsTrace(NWT_TRACE_WARNING, "nullref rasterizer selected");
            dtype = D3DDEVTYPE_NULLREF;
        } else {
            RlsTrace1(NWT_TRACE_WARNING,
                "unknown rasterizer: %s, only (ref|hal|nul) "\
                "supported, hal selected instead", pRas);
        }
        RlsTrace(NWT_TRACE_WARNING, "\n");
        free(pRas);
    }
    return dtype;
}

#define CHECK_CAP(FLAG, CAP) \
    do {    \
        if (!((FLAG)&CAP)) { \
            RlsTraceLn2(NWT_TRACE_ERROR, \
                           "D3DPPLM::CheckDeviceCaps: adapter %d: Failed "\
                           "(cap %s not supported)", \
                           adapter, #CAP); \
            return E_FAIL; \
        } \
    } while (0)

HRESULT D3DPipelineManager::CheckDeviceCaps(UINT adapter)
{
    HRESULT res;
    D3DCAPS9 d3dCaps;

    TraceLn(NWT_TRACE_INFO, "D3DPPLM::CheckDeviceCaps");

    res = pd3d9->GetDeviceCaps(adapter, devType, &d3dCaps);
    RETURN_STATUS_IF_FAILED(res);

    // we'll skip this check as we'd likely still benefit from hw acceleration
    // of effects in this case
//  CHECK_CAP(d3dCaps.DevCaps, D3DDEVCAPS_HWTRANSFORMANDLIGHT);
    if (d3dCaps.DeviceType == D3DDEVTYPE_HAL) {
        CHECK_CAP(d3dCaps.DevCaps, D3DDEVCAPS_HWRASTERIZATION);
    }

    CHECK_CAP(d3dCaps.RasterCaps, D3DPRASTERCAPS_SCISSORTEST);

    CHECK_CAP(d3dCaps.PrimitiveMiscCaps, D3DPMISCCAPS_CULLNONE);
    CHECK_CAP(d3dCaps.PrimitiveMiscCaps, D3DPMISCCAPS_BLENDOP);
    CHECK_CAP(d3dCaps.PrimitiveMiscCaps, D3DPMISCCAPS_MASKZ);

    CHECK_CAP(d3dCaps.ZCmpCaps, D3DPCMPCAPS_ALWAYS);
    CHECK_CAP(d3dCaps.ZCmpCaps, D3DPCMPCAPS_LESS);

    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_ZERO);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_ONE);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_SRCALPHA);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_DESTALPHA);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_INVSRCALPHA);
    CHECK_CAP(d3dCaps.SrcBlendCaps, D3DPBLENDCAPS_INVDESTALPHA);

    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_ZERO);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_ONE);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_SRCALPHA);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_DESTALPHA);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_INVSRCALPHA);
    CHECK_CAP(d3dCaps.DestBlendCaps, D3DPBLENDCAPS_INVDESTALPHA);

    CHECK_CAP(d3dCaps.TextureAddressCaps, D3DPTADDRESSCAPS_CLAMP);
    CHECK_CAP(d3dCaps.TextureAddressCaps, D3DPTADDRESSCAPS_WRAP);

    if (d3dCaps.PixelShaderVersion < D3DPS_VERSION(3,0)) {
        RlsTraceLn1(NWT_TRACE_ERROR,
                       "D3DPPLM::CheckDeviceCaps: adapter %d: Failed "\
                       "(pixel shaders 3.0 required)", adapter);
        return E_FAIL;
    }

    RlsTraceLn1(NWT_TRACE_INFO,
                   "D3DPPLM::CheckDeviceCaps: adapter %d: Passed", adapter);
    return S_OK;
}


HRESULT D3DPipelineManager::D3DEnabledOnAdapter(UINT adapter)
{
    HRESULT res;
    D3DDISPLAYMODE dm;

    res = pd3d9->GetAdapterDisplayMode(adapter, &dm);
    RETURN_STATUS_IF_FAILED(res);

    res = pd3d9->CheckDeviceType(adapter, devType, dm.Format, dm.Format, TRUE);
    if (FAILED(res)) {
        RlsTraceLn1(NWT_TRACE_ERROR,
                "D3DPPLM::D3DEnabledOnAdapter: no " \
                "suitable d3d device on adapter %d", adapter);
    }

    return res;
}

UINT D3DPipelineManager::GetAdapterOrdinalByHmon(HMONITOR hMon)
{
    UINT ret = D3DADAPTER_DEFAULT;

    if (pd3d9 != NULL) {
        UINT adapterCount = pd3d9->GetAdapterCount();
        for (UINT adapter = 0; adapter < adapterCount; adapter++) {
            HMONITOR hm = pd3d9->GetAdapterMonitor(adapter);
            if (hm == hMon) {
                ret = adapter;
                break;
            }
        }
    }
    return ret;
}

D3DFORMAT
D3DPipelineManager::GetMatchingDepthStencilFormat(UINT adapterOrdinal,
                                                  D3DFORMAT adapterFormat,
                                                  D3DFORMAT renderTargetFormat)
{
    static D3DFORMAT formats[] =
        { D3DFMT_D32, D3DFMT_D24S8, D3DFMT_D24X8, D3DFMT_D16 };
    D3DFORMAT newFormat = D3DFMT_UNKNOWN;
    HRESULT res;
    for (int i = 0; i < 4; i++) {
        res = pd3d9->CheckDeviceFormat(adapterOrdinal,
                devType, adapterFormat, D3DUSAGE_DEPTHSTENCIL,
                D3DRTYPE_SURFACE, formats[i]);
        if (FAILED(res)) continue;

        res = pd3d9->CheckDepthStencilMatch(adapterOrdinal,
                devType, adapterFormat, renderTargetFormat, formats[i]);
        if (FAILED(res)) continue;
        newFormat = formats[i];
        break;
    }
    return newFormat;
}

HRESULT D3DPipelineManager::GetD3DContext(UINT adapterOrdinal,
                                          D3DContext **ppd3dContext)
{
    TraceLn(NWT_TRACE_INFO, "D3DPPLM::GetD3DContext");

    HRESULT res = S_OK;
    if (adapterOrdinal < 0 || adapterOrdinal >= adapterCount ||
        pAdapters == NULL ||
        pAdapters[adapterOrdinal].state == CONTEXT_INIT_FAILED)
    {
        RlsTraceLn1(NWT_TRACE_ERROR,
            "D3DPPLM::GetD3DContext: invalid parameters or "\
            "failed init for adapter %d", adapterOrdinal);
        *ppd3dContext = NULL;
        return E_FAIL;
    }

    if (pAdapters[adapterOrdinal].state == CONTEXT_NOT_INITED) {
        D3DContext *pCtx = NULL;

        if (pAdapters[adapterOrdinal].pd3dContext != NULL) {
            TraceLn1(NWT_TRACE_ERROR, "  non-null context in "\
                        "uninitialized adapter %d", adapterOrdinal);
            res = E_FAIL;
        } else {
            TraceLn1(NWT_TRACE_VERBOSE,
                        "  initializing context for adapter %d",adapterOrdinal);

            if (SUCCEEDED(res = D3DEnabledOnAdapter(adapterOrdinal))) {
                res = D3DContext::CreateInstance(pd3d9, pd3d9Ex, adapterOrdinal, isVsyncEnabled, &pCtx);
                if (FAILED(res)) {
                    RlsTraceLn1(NWT_TRACE_ERROR,
                        "D3DPPLM::GetD3DContext: failed to create context "\
                        "for adapter=%d", adapterOrdinal);
                }
            } else {
                RlsTraceLn1(NWT_TRACE_ERROR,
                    "D3DPPLM::GetContext: no d3d on adapter %d",adapterOrdinal);
            }
        }
        pAdapters[adapterOrdinal].state =
            SUCCEEDED(res) ? CONTEXT_CREATED : CONTEXT_INIT_FAILED;
        pAdapters[adapterOrdinal].pd3dContext = pCtx;
    }
    *ppd3dContext = pAdapters[adapterOrdinal].pd3dContext;
    return res;
}
