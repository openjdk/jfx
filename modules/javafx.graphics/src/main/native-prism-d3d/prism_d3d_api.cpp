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

/*
 * prism_d3d_api.cpp - implementation of the flat C ABI declared in prism_d3d_api.h.
 *
 * Every d3d_* function calls the same C++ (D3DPipelineManager, D3DContext, D3DResourceManager,
 * TextureUpdater, D3DMesh, D3DPhongMaterial, D3DMeshView) that the JNI entry points it replaced (formerly in
 * D3DPipeline.cc, D3DResourceFactory.cc, D3DGraphics.cc, D3DContext.cc and D3DShader.cc) called, in the
 * same order and with the same checks. The JNI-only parts (array pinning, direct-buffer lookup, Java
 * field reads and writes) are replaced by plain pointers, a flags word and out-param structs.
 *
 * The only C++ code reachable from here that can throw is a plain `new` (std::bad_alloc); there are
 * no throw statements and no std::string in the library. The exports that reach one catch it and
 * report the failure the way the surrounding code reports any other failure (E_OUTOFMEMORY or a NULL
 * handle) so that nothing unwinds through the FFM frame.
 */

#include "prism_d3d_api.h"

#include <new>
#include <stddef.h>
#include <string.h>

#include "D3DPipeline.h"
#include "D3DPipelineManager.h"
#include "D3DResourceManager.h"
#include "D3DContext.h"
#include "D3DMesh.h"
#include "D3DMeshView.h"
#include "D3DPhongMaterial.h"
#include "TextureUploader.h"

/* Struct layouts the Java MemoryLayouts mirror (D3DNativeTest compares them with d3d_sizeof_*). */
static_assert(sizeof(D3dDriverInfo) == 1364, "D3dDriverInfo layout changed - bump PRISM_D3D_ABI_VERSION");
static_assert(sizeof(D3dFrameStats) == 32, "D3dFrameStats layout changed - bump PRISM_D3D_ABI_VERSION");
// sizeof == 32 holds for ANY order of eight int32 fields; D3DNative.FRAME_STATS_LAYOUT reads each counter at
// the offset its declaration position gives it (OFFSET_NUM_*), so the order is pinned field by field.
static_assert(offsetof(D3dFrameStats, num_triangles_drawn) == 0, "D3DNative reads num_triangles_drawn at 0");
static_assert(offsetof(D3dFrameStats, num_draw_calls) == 4, "D3DNative reads num_draw_calls at 4");
static_assert(offsetof(D3dFrameStats, num_buffer_locks) == 8, "D3DNative reads num_buffer_locks at 8");
static_assert(offsetof(D3dFrameStats, num_texture_locks) == 12, "D3DNative reads num_texture_locks at 12");
static_assert(offsetof(D3dFrameStats, num_texture_transfer_bytes) == 16,
              "D3DNative reads num_texture_transfer_bytes at 16");
static_assert(offsetof(D3dFrameStats, num_set_texture) == 20, "D3DNative reads num_set_texture at 20");
static_assert(offsetof(D3dFrameStats, num_set_pixel_shader) == 24, "D3DNative reads num_set_pixel_shader at 24");
static_assert(offsetof(D3dFrameStats, num_render_target_switch) == 28,
              "D3DNative reads num_render_target_switch at 28");
static_assert(sizeof(D3dTextureInfo) == 24, "D3dTextureInfo layout changed - bump PRISM_D3D_ABI_VERSION");
static_assert(sizeof(D3dDriverInfo::device_description) == MAX_DEVICE_IDENTIFIER_STRING,
              "device_description must hold D3DADAPTER_IDENTIFIER9.Description");
static_assert(sizeof(D3dDriverInfo::driver_name) == MAX_DEVICE_IDENTIFIER_STRING,
              "driver_name must hold D3DADAPTER_IDENTIFIER9.Driver");
static_assert(sizeof(D3dDriverInfo::device_name) == sizeof(D3DADAPTER_IDENTIFIER9::DeviceName),
              "device_name must hold D3DADAPTER_IDENTIFIER9.DeviceName");
#if defined PERF_COUNTERS
static_assert(sizeof(D3dFrameStats) == sizeof(D3DContext::FrameStats), "D3dFrameStats mirrors D3DContext::FrameStats");
#endif

/* Constant tables pinned to their Java definitions (D3DContext.D3DCOMPMODE_*, D3DContext.CULL_*) and to
 * TextureUploader.h (PFormat == com.sun.prism.PixelFormat.ordinal()). */
static_assert(D3D_COMPMODE_CLEAR == 0 && D3D_COMPMODE_SRC == 1 && D3D_COMPMODE_SRCOVER == 2 &&
              D3D_COMPMODE_DSTOUT == 3 && D3D_COMPMODE_ADD == 4, "D3DContext.java D3DCOMPMODE_*");
static_assert(D3D_CULL_BACK == 110 && D3D_CULL_FRONT == 111 && D3D_CULL_NONE == 112, "D3DContext.java CULL_*");
static_assert(D3D_PFORMAT_INT_ARGB_PRE == PFORMAT_INT_ARGB_PRE && D3D_PFORMAT_BYTE_RGBA_PRE == PFORMAT_BYTE_RGBA_PRE &&
              D3D_PFORMAT_BYTE_RGB == PFORMAT_BYTE_RGB && D3D_PFORMAT_BYTE_GRAY == PFORMAT_BYTE_GRAY &&
              D3D_PFORMAT_BYTE_ALPHA == PFORMAT_BYTE_ALPHA && D3D_PFORMAT_MULTI_YV_12 == PFORMAT_MULTI_YV_12 &&
              D3D_PFORMAT_BYTE_APPL_422 == PFORMAT_BYTE_APPL_422 && D3D_PFORMAT_FLOAT_XYZW == PFORMAT_FLOAT_XYZW,
              "D3dPixelFormat mirrors PFormat");
static_assert(D3D_INDEX_16 == 16 && D3D_INDEX_32 == 32, "D3dIndexType");

/* Defined in D3DGraphics.cc; D3DContext::drawIndexedQuads only needs the pointer. */
struct PrismSourceVertex;

namespace {

/*
 * IConfig over the D3dInitFlags word. Replaces the former ConfigJavaStaticClass of D3DPipeline.cc, which
 * read PrismSettings.forceGPU / isVsyncEnabled / verbose through GetStaticFieldID. getInt is never
 * called by D3DPipelineManager (nor was the JNI version); an unknown name reads as false, as a
 * missing field did.
 */
struct FlagsConfig : IConfig {
    int32_t flags;
    explicit FlagsConfig(int32_t f) : flags(f) {}

    virtual int getInt(cstr) { return 0; }

    virtual bool getBool(cstr name) {
        if (strcmp(name, "forceGPU") == 0)       return (flags & D3D_INIT_FORCE_GPU) != 0;
        if (strcmp(name, "isVsyncEnabled") == 0) return (flags & D3D_INIT_VSYNC) != 0;
        if (strcmp(name, "verbose") == 0)        return (flags & D3D_INIT_VERBOSE) != 0;
        return false;
    }
};

/* NUL-terminated ANSI copy truncated to the array size; "" for a NULL source (the Java null the
 * JNI produced with NewStringUTF(NULL)). */
template <size_t N>
void copyString(char (&dst)[N], const char *src) {
    if (src == NULL) {
        dst[0] = 0;
        return;
    }
    size_t n = strnlen(src, N - 1);
    memcpy(dst, src, n);
    dst[n] = 0;
}

/* Mirrors the former fillDriverInformation of D3DPipeline.cc: same fields, same conditions. */
void fillDriverInformation(D3dDriverInfo *out, D3DADAPTER_IDENTIFIER9 &did, D3DCAPS9 &caps) {
    copyString(out->device_description, did.Description);
    copyString(out->device_name, did.DeviceName);
    copyString(out->driver_name, did.Driver);
    out->vendor_id = (int32_t) did.VendorId;
    out->device_id = (int32_t) did.DeviceId;
    out->subsys_id = (int32_t) did.SubSysId;
    out->product = HIWORD(did.DriverVersion.HighPart);
    out->version = LOWORD(did.DriverVersion.HighPart);
    out->sub_version = HIWORD(did.DriverVersion.LowPart);
    out->build_id = LOWORD(did.DriverVersion.LowPart);
    if (caps.PixelShaderVersion & 0xFFFF0000) {
        out->ps_version_major = (int32_t) ((caps.PixelShaderVersion >> 8) & 0xFF);
        out->ps_version_minor = (int32_t) ((caps.PixelShaderVersion) & 0xFF);
    }

    // execute CheckForBadHardware to have valid string
    D3DPipelineManager::CheckForBadHardware(did);
    copyString(out->warning_message, D3DPipelineManager::GetErrorMessage());
}

/* Mirrors the former fillOsVersionInformation of D3DPipeline.cc. */
void fillOsVersionInformation(D3dDriverInfo *out) {
    OSVERSIONINFO osInfo; osInfo.dwOSVersionInfoSize = sizeof(osInfo);
// GetVersionEx is deprecated (C4996). It is kept because the manifested version it reports is what
// D3DDriverInformation.getOsVersion() has always shown; the JNI code made the same call.
#pragma warning(push)
#pragma warning(disable: 4996)
    BOOL ok = GetVersionEx(&osInfo);
#pragma warning(pop)
    if (ok) {
        out->os_major = (int32_t) osInfo.dwMajorVersion;
        out->os_minor = (int32_t) osInfo.dwMinorVersion;
        out->os_build = (int32_t) osInfo.dwBuildNumber;
    }
}

/* Takes the IDirect3D9Ex the JNI took: the manager's (AddRef'd) when there is one, a fresh one
 * otherwise (as the former nGetDriverInformation / nGetMaxSampleSupport bodies did). The caller must Release() it. */
IDirect3D9Ex *acquireD3D9() {
    return D3DPipelineManager::GetInstance() ?
        addRef(D3DPipelineManager::GetInstance()->GetD3DObject()) : Direct3DCreate9Ex();
}

void clearTextureInfo(D3dTextureInfo *out) {
    out->handle = NULL;
    out->width = 0;
    out->height = 0;
    out->is_default_pool = 0;
    out->reserved = 0;
}

/* What nGetTextureWidth, nGetTextureHeight and nIsDefaultPool returned for a fresh resource. */
void fillTextureInfo(D3dTextureInfo *out, D3DResource *pRes) {
    out->handle = pRes;
    out->width = (int32_t) pRes->GetDesc()->Width;
    out->height = (int32_t) pRes->GetDesc()->Height;
    out->is_default_pool = pRes->IsDefaultPool() ? 1 : 0;
}

/*
 * Formerly the file-local updateTexture of D3DResourceFactory.cc (int parameters now int32_t).
 * Note: this method assumes that pCtx, pTexResource and pixels are not null
 */
HRESULT updateTextureImpl(
    D3DContext *pCtx, D3DResource *pTexResource, PBYTE pixels, int32_t size, int32_t format,
    int32_t dstx, int32_t dsty, int32_t srcx, int32_t srcy, int32_t srcw, int32_t srch, int32_t srcscan)
{

    D3DSURFACE_DESC * desc = pTexResource->GetDesc();

    bool paramsOK = TextureUpdater::validateArguments(
        dstx, dsty, desc->Width, desc->Height,
        srcx, srcy, srcw, srch,
        size, PFormat(format), srcscan);

    RETURN_STATUS_IF_NULL(paramsOK, E_INVALIDARG);

    TraceLn7(NWT_TRACE_VERBOSE, "updateTexture src = [%d, %d]-[%dx%d], pixels = %p, dst = [%dx%d]",
             srcx, srcy, srcw, srch, pixels, dstx, dsty);

    TextureUpdater updater;
    updater.setTarget(pTexResource->GetTexture(), pTexResource->GetSurface(), desc, dstx, dsty);
    updater.setSource(pixels, size, PFormat(format), srcx, srcy, srcw, srch, srcscan);

    int nBytes = updater.updateD3D9ExTexture(pCtx);

#if defined PERF_COUNTERS
    D3DContext::FrameStats &stats = pCtx->getStats();
    stats.numTextureLocks++;
    stats.numTextureTransferBytes += nBytes;
#endif

    return nBytes ? S_OK : E_FAIL;
}

/* Formerly copy_X8R8G8B8 of D3DResourceFactory.cc. */
void copyX8R8G8B8(DWORD *pDstPixels, DWORD const *pSrcPixels, int n) {
    for (int i = 0; i!=n; ++i) {
        pDstPixels[i] = pSrcPixels[i] | 0xff000000;
    }
}

/*
 * Formerly D3DResourceFactory_nReadPixels of D3DResourceFactory.cc.
 * Note: this method assumes that pCtx, pResource and pixels are not null
 */
HRESULT readPixelsImpl(D3DContext *pCtx, D3DResource *pResource, BYTE *pixels, int cntW, int cntH)
{

    TraceLn(NWT_TRACE_INFO, "d3d_texture_read_pixels");

    IDirect3DDevice9Ex *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    IDirect3DSurface9 *pSrc = pResource->GetSurface();
    RETURN_STATUS_IF_NULL(pSrc, E_FAIL);

    D3DFORMAT srcFmt = pResource->GetDesc()->Format;
    UINT srcw = pResource->GetDesc()->Width;
    UINT srch = pResource->GetDesc()->Height;

    if (srcFmt != D3DFMT_A8R8G8B8 && srcFmt != D3DFMT_X8R8G8B8) {
        RlsTraceLn1(NWT_TRACE_ERROR,
            "d3d_texture_read_pixels doesn't support format %d", srcFmt);
        return E_FAIL;
    }

    // the dest surface must have the same dimensions and format as
    // the source, GetBlitOSPSurface ensures that
    D3DResource *pLockableRes = 0;
    HRESULT res = pCtx->GetResourceManager()->
        GetBlitOSPSurface(srcw, srch, srcFmt, &pLockableRes);

    if (SUCCEEDED(res)) {
        IDirect3DSurface9 *pTmpSurface = pLockableRes->GetSurface();

        pCtx->EndScene();

        res = pd3dDevice->GetRenderTargetData(pSrc, pTmpSurface);
        if (SUCCEEDED(res)) {
            D3DLOCKED_RECT lockedRect;
            if (FAILED(res = pTmpSurface->LockRect(&lockedRect, NULL,
                                                    D3DLOCK_NOSYSLOCK)))
            {
                RlsTraceLn1(NWT_TRACE_ERROR,
                    "d3d_texture_read_pixels lock failed res=%x", res);
                return res;
            }
            // assuming int (a|x)rgb type, and 0,0 source coordinates

            BYTE const *pSrcPixels = PBYTE(lockedRect.pBits);
            BYTE *pDstPixels = pixels;

            switch (srcFmt) {
            case D3DFMT_A8R8G8B8:
                for (int y=0; y!=cntH; ++y) {
                    // cntW, cntH are sanity checked in d3d_texture_read_pixels
                    memcpy(pDstPixels, pSrcPixels, cntW*4);
                    pSrcPixels += lockedRect.Pitch;
                    pDstPixels += cntW*4;
                }
                break;
            case D3DFMT_X8R8G8B8:
                for (int y=0; y!=cntH; ++y) {
                    // cntW, cntH are sanity checked in d3d_texture_read_pixels
                    copyX8R8G8B8(PDWORD(pDstPixels), (DWORD const*)(pSrcPixels), cntW*4);
                    pSrcPixels += lockedRect.Pitch;
                    pDstPixels += cntW*4;
                }
                break;
            }

            res = pTmpSurface->UnlockRect();
        }
    }
    return res;
}

} // namespace

extern "C" {

/* ------------------------------------------------------------------------------------------------
 * Guards
 * ---------------------------------------------------------------------------------------------- */

int32_t d3d_abi_version(void)
{
    return PRISM_D3D_ABI_VERSION;
}

int64_t d3d_sizeof_driver_info(void)
{
    return (int64_t) sizeof(D3dDriverInfo);
}

int64_t d3d_sizeof_frame_stats(void)
{
    return (int64_t) sizeof(D3dFrameStats);
}

int64_t d3d_sizeof_texture_info(void)
{
    return (int64_t) sizeof(D3dTextureInfo);
}

/* ------------------------------------------------------------------------------------------------
 * Pipeline (formerly D3DPipeline.cc)
 * ---------------------------------------------------------------------------------------------- */

/* D3DPipeline.nInit; the STATIC_BUILD-only `load` argument is dropped. */
int32_t d3d_pipeline_init(int32_t flags)
{
    if (D3DPipelineManager::GetInstance()) {
        D3DPipelineManager::SetErrorMessage("Double D3DPipelineManager initialization");
        return 0;
    }

    if (FAILED(D3DPipelineManager::CheckOSVersion())) {
        D3DPipelineManager::SetErrorMessage("Wrong operating system version");
        return 0;
    }

    TraceLn(NWT_TRACE_INFO, "d3d_pipeline_init");
    FlagsConfig cfg(flags);
    D3DPipelineManager *pMgr = NULL;
    try {
        pMgr = D3DPipelineManager::CreateInstance(cfg);
    } catch (const std::bad_alloc&) {
        pMgr = NULL;
    }

    if (!pMgr && !D3DPipelineManager::GetErrorMessage()) {
        D3DPipelineManager::SetErrorMessage("Direct3D initialization failed");
    }

    return pMgr != 0 ? 1 : 0;
}

/* D3DPipeline.nGetErrorMessage */
int32_t d3d_pipeline_get_error_message(char* buf, int32_t cap)
{
    const char * msg = D3DPipelineManager::GetErrorMessage();
    if (msg == NULL || buf == NULL || cap <= 0) {
        return 0;
    }
    size_t n = strnlen(msg, (size_t) cap - 1);
    memcpy(buf, msg, n);
    buf[n] = 0;
    return (int32_t) n;
}

/* D3DPipeline.nDispose; the STATIC_BUILD-only `unload` argument is dropped. */
void d3d_pipeline_dispose(void)
{
    TraceLn(NWT_TRACE_INFO, "d3d_pipeline_dispose");
    if (D3DPipelineManager::GetInstance()) {
        D3DPipelineManager::DeleteInstance();
    }
}

/* D3DPipeline.nGetAdapterOrdinal */
int32_t d3d_pipeline_get_adapter_ordinal(int64_t hmonitor)
{
    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    if (!pMgr) {
        return 0;
    }
    return (int32_t) pMgr->GetAdapterOrdinalByHmon((HMONITOR)(intptr_t) hmonitor);
}

/* D3DPipeline.nGetAdapterCount */
int32_t d3d_pipeline_get_adapter_count(void)
{
    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    if (!pMgr) {
        return 0;
    }
    return (int32_t) pMgr->GetAdapterCount();
}

/* D3DPipeline.nGetDriverInformation; the 18 Set*Field calls become the struct. */
int32_t d3d_pipeline_get_driver_information(int32_t adapter, D3dDriverInfo* out)
{
    if (!out) {
        return 0;
    }
    // the Java fields the JNI did not write kept their defaults (0 / null)
    memset(out, 0, sizeof(*out));

    // if there is D3DPipelineManager take ready D3D9 object, otherwise create new
    IDirect3D9Ex * d3d9 = acquireD3D9();

    if (!d3d9) {
        return 0;
    }

    if (unsigned(adapter) >= d3d9->GetAdapterCount()) {
        d3d9->Release();
        return 0;
    }

    D3DADAPTER_IDENTIFIER9 d_id;
    D3DCAPS9 caps;
    if (FAILED(d3d9->GetAdapterIdentifier((UINT) adapter, 0, &d_id)) ||
        FAILED(d3d9->GetDeviceCaps((UINT) adapter, D3DDEVTYPE_HAL, &caps))) {
        d3d9->Release();
        return 0;
    }

    int maxSamples = getMaxSampleSupport(d3d9, (UINT) adapter);

    fillDriverInformation(out, d_id, caps);
    out->max_samples = maxSamples;
    fillOsVersionInformation(out);

    d3d9->Release();
    return 1;
}

/* D3DPipeline.nGetMaxSampleSupport */
int32_t d3d_pipeline_get_max_sample_support(int32_t adapter)
{
    // if there is D3DPipelineManager take ready D3D9 object, otherwise create new
    IDirect3D9Ex * d3d9 = acquireD3D9();

    if (!d3d9) {
        return 0;
    }

    if (unsigned(adapter) >= d3d9->GetAdapterCount()) {
        d3d9->Release();
        return 0;
    }

    int maxSamples = getMaxSampleSupport(d3d9, (UINT) adapter);

    d3d9->Release();
    return maxSamples;
}

/* ------------------------------------------------------------------------------------------------
 * Context and resources (formerly D3DResourceFactory.cc)
 * ---------------------------------------------------------------------------------------------- */

/* D3DResourceFactory.nGetContext */
void* d3d_context_get(int32_t adapter)
{
    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    RETURN_STATUS_IF_NULL(pMgr, NULL);

    D3DContext *pCtx = NULL;
    HRESULT res;
    try {
        res = pMgr->GetD3DContext((UINT) adapter, &pCtx);
    } catch (const std::bad_alloc&) {
        return NULL;
    }
    if (SUCCEEDED(res)) {
        pCtx->ResetClip();
        pCtx->ResetTransform();

        return pCtx;
    }

    return NULL;
}

/* D3DResourceFactory.nTestCooperativeLevel */
int32_t d3d_context_test_cooperative_level(void* ctx)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->TestCooperativeLevel();
}

/* D3DResourceFactory.nResetDevice */
int32_t d3d_context_reset_device(void* ctx)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    try {
        return pCtx->ResetContext();
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    }
}

/* D3DResourceFactory.nGetMaximumTextureSize */
int32_t d3d_context_get_max_texture_size(void* ctx)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, -1);

    D3DCAPS9 *caps = pCtx->GetDeviceCaps();
    RETURN_STATUS_IF_NULL(caps, -1);

    DWORD maxw = caps->MaxTextureWidth;
    DWORD maxh = caps->MaxTextureHeight;
    DWORD max = (maxw < maxh) ? maxw : maxh;
    return (int32_t) max;
}

/* D3DResourceFactory.nCreateTexture (+ nGetTextureWidth/Height, nIsDefaultPool) */
int32_t d3d_texture_create(void* ctx, int32_t format_hint, int32_t usage_hint, int32_t is_rtt,
                           int32_t width, int32_t height, int32_t samples, int32_t use_mipmap,
                           D3dTextureInfo* out)
{
    TraceLn6(NWT_TRACE_INFO,
             "d3d_texture_create formatHint=%d usageHint=%d isRTT=%d w=%d h=%d useMipmap=%d",
             format_hint, usage_hint, is_rtt, width, height, use_mipmap);

    RETURN_STATUS_IF_NULL(out, E_INVALIDARG);
    clearTextureInfo(out);

    D3DContext *pCtx = (D3DContext *) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResourceManager *pMgr = pCtx->GetResourceManager();
    RETURN_STATUS_IF_NULL(pMgr, E_FAIL);

    D3DResource *pTexResource = NULL;
    D3DFORMAT format = D3DFMT_UNKNOWN;
    HRESULT res;

    // only considered when the format isn't explicitly requested
    BOOL isOpaque = FALSE;

    if (usage_hint == 1) {
        OutputDebugStringA("Texture.Usage.DYNAMIC");
    }

    DWORD dwUsage = usage_hint == 1/*Texture.Usage.DYNAMIC*/ ? D3DUSAGE_DYNAMIC : 0;

    // formatHint is the hint about the content of the texture, not a hard
    // requirement
    switch (format_hint) {
        case PFORMAT_BYTE_RGBA_PRE:
        case PFORMAT_INT_ARGB_PRE:
            format = D3DFMT_A8R8G8B8;
            break;
        case PFORMAT_BYTE_RGB: // Note: this is actually 3-byte RGB
            format = D3DFMT_X8R8G8B8;
            break;
        case PFORMAT_BYTE_GRAY:
            format = D3DFMT_L8;
            break;
        case PFORMAT_BYTE_ALPHA:
            format = D3DFMT_A8;
            break;
        case PFORMAT_FLOAT_XYZW:
            format = D3DFMT_A32B32G32R32F;
            break;
        default:
            RlsTraceLn1(NWT_TRACE_WARNING,
                        "d3d_texture_create: unknown format hint: %d", format_hint);
            break;
    }

    try {
        if (samples) {
            // assert isRTT == true
            D3DMULTISAMPLE_TYPE msType = static_cast<D3DMULTISAMPLE_TYPE>(samples);
            res = pMgr->CreateRenderTarget(width, height, isOpaque,
                    &format, msType, &pTexResource);
        } else {
            res = pMgr->CreateTexture(width, height, is_rtt != 0, isOpaque, use_mipmap != 0,
                    &format, dwUsage, &pTexResource);
        }
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    }
    if (SUCCEEDED(res)) {
        fillTextureInfo(out, pTexResource);
    }

    return res;
}

/* D3DResourceFactory.nCreateSwapChain (+ nGetTextureWidth/Height, nIsDefaultPool) */
int32_t d3d_swapchain_create(void* ctx, int64_t hwnd, int32_t vsync, D3dTextureInfo* out)
{
    RETURN_STATUS_IF_NULL(out, E_INVALIDARG);
    clearTextureInfo(out);

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    HWND hWnd = (HWND)(intptr_t) hwnd;
    if (!::IsWindow(hWnd)) {
        TraceLn1(NWT_TRACE_ERROR, "d3d_swapchain_create: hwnd=%p is not a window\n", hWnd);
        return E_FAIL;
    }

    D3DResource *pSwapChainRes = NULL;
    HRESULT res;
    try {
        res = pCtx->GetResourceManager()->
                CreateSwapChain(hWnd, 1,
                0, 0,
                // have to use COPY since we don't re-render the scene
                // if it didn't change
                D3DSWAPEFFECT_COPY,
                vsync != 0 ?
                D3DPRESENT_INTERVAL_ONE :
                D3DPRESENT_INTERVAL_IMMEDIATE,
                &pSwapChainRes);
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    }

    if (SUCCEEDED(res)) {
        fillTextureInfo(out, pSwapChainRes);
    }

    return res;
}

/* D3DResourceFactory.nReleaseResource */
int32_t d3d_resource_release(void* ctx, void* res)
{
    IManagedResource *pResource = (IManagedResource*) res;
    RETURN_STATUS_IF_NULL(pResource, D3D_OK);

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, S_FALSE);

    return pCtx->GetResourceManager()->ReleaseResource(pResource);
}

/* D3DResourceFactory.nGetTextureWidth + nGetTextureHeight */
int32_t d3d_resource_get_size(void* res, int32_t* width, int32_t* height)
{
    D3DResource *pResource = (D3DResource*) res;
    RETURN_STATUS_IF_NULL(pResource, -1);

    if (width != NULL) {
        *width = (int32_t) pResource->GetDesc()->Width;
    }
    if (height != NULL) {
        *height = (int32_t) pResource->GetDesc()->Height;
    }
    return 0;
}

/* D3DResourceFactory.nIsDefaultPool */
int32_t d3d_resource_is_default_pool(void* res)
{
    IManagedResource *pResource = (IManagedResource*) res;

    RETURN_STATUS_IF_NULL(pResource, 0);

    return pResource->IsDefaultPool() ? 1 : 0;
}

/* D3DResourceFactory.nUpdateTextureI / nUpdateTextureB / nUpdateTextureF */
int32_t d3d_texture_update(void* ctx, void* res, const void* pixels, int64_t pixels_bytes,
                           int32_t format, int32_t dstx, int32_t dsty, int32_t srcx, int32_t srcy,
                           int32_t srcw, int32_t srch, int32_t srcscan)
{
    RETURN_STATUS_IF_NULL(ctx, E_FAIL);
    RETURN_STATUS_IF_NULL(res, E_FAIL);

    // the JNI reported a NULL pinned array / direct buffer address as E_OUTOFMEMORY
    RETURN_STATUS_IF_NULL(pixels, E_OUTOFMEMORY);

    // the JNI computed the byte count as a 32-bit int (array length * element size, or the direct buffer
    // capacity); the same 32-bit truncation applies here
    return updateTextureImpl(
        (D3DContext*) ctx, (D3DResource*) res, (PBYTE) pixels, (int32_t) pixels_bytes, format,
        dstx, dsty, srcx, srcy, srcw, srch, srcscan);
}

/* D3DResourceFactory.nReadPixelsI / nReadPixelsB (nReadPixelsHelper) */
int32_t d3d_texture_read_pixels(void* ctx, void* res, void* dst, int64_t dst_bytes, int32_t w, int32_t h)
{
    TraceLn(NWT_TRACE_INFO, "d3d_texture_read_pixels");

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pResource = (D3DResource*) res;
    RETURN_STATUS_IF_NULL(pResource, E_FAIL);

    // sanity check about we have enought memory
    // Since we are certain cntW and cntH are positive numbers
    if ( UINT(dst_bytes)/4/w < UINT(h) ) {
        RlsTraceLn1(NWT_TRACE_ERROR,
                    "d3d_texture_read_pixels buffer too small: %lld",
                    (long long) dst_bytes);
        return E_OUTOFMEMORY;
    }

    BYTE *pixels = PBYTE(dst);

    RETURN_STATUS_IF_NULL(pixels, E_OUTOFMEMORY);

    try {
        return readPixelsImpl(pCtx, pResource, pixels, w, h);
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    }
}

/* ------------------------------------------------------------------------------------------------
 * Pixel shaders (formerly D3DShader.cc)
 * ---------------------------------------------------------------------------------------------- */

/* D3DShader.init; its last three arguments were ignored by the C. */
void* d3d_shader_create(void* ctx, const void* bytecode, int64_t bytecode_bytes)
{
    TraceLn(NWT_TRACE_INFO, "d3d_shader_create");

    (void) bytecode_bytes; // D3D parses the token stream up to its END token; the JNI had no length either

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, NULL);

    DWORD *buf = (DWORD *) bytecode;
    if (buf == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR,
                   "d3d_shader_create: NULL shader bytecode");
        return NULL;
    }

    D3DResourceManager *pMgr = pCtx->GetResourceManager();
    RETURN_STATUS_IF_NULL(pMgr, NULL);

    D3DPixelShaderResource *pPSResource = NULL;
    try {
        if (SUCCEEDED(pMgr->CreatePixelShader(buf, &pPSResource))) {
            return pPSResource;
        }
    } catch (const std::bad_alloc&) {
        return NULL;
    }
    return NULL;
}

/* D3DShader.enable */
int32_t d3d_shader_enable(void* ctx, void* shader)
{
    TraceLn(NWT_TRACE_INFO, "d3d_shader_enable");

    D3DPixelShaderResource *pPSResource = (D3DPixelShaderResource *) shader;
    RETURN_STATUS_IF_NULL(pPSResource, E_FAIL);

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

#if defined PERF_COUNTERS
    pCtx->getStats().numSetPixelShader++;
#endif

    IDirect3DDevice9Ex *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    IDirect3DPixelShader9 *pShader = pPSResource->GetPixelShader();
    if (pShader == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR, "d3d_shader_enable: pShader is null");
        return E_FAIL;
    }

    HRESULT res = pd3dDevice->SetPixelShader(pShader);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "d3d_shader_enable: SetPixelShader failed");
    }
    return res;
}

/* D3DShader.disable */
int32_t d3d_shader_disable(void* ctx)
{
    TraceLn(NWT_TRACE_INFO, "d3d_shader_disable");

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    IDirect3DDevice9Ex *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res = pd3dDevice->SetPixelShader(NULL);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "d3d_shader_disable: SetPixelShader(NULL) failed");
    }

    return res;
}

/* D3DShader.setConstantsF; the caller passes the already offset pointer, so
 * the direct-buffer capacity check (and the off * sizeof(float) double scaling) has no equivalent. */
int32_t d3d_shader_set_constants_f(void* ctx, int32_t reg, const float* values, int32_t count)
{
    TraceLn2(NWT_TRACE_INFO, "d3d_shader_set_constants_f (reg=%d, count=%d)", reg, count);

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    if (count < 1) {
        RlsTraceLn(NWT_TRACE_ERROR, "  Array out of bounds access.");
        return E_FAIL;
    }

    if (values == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR, "  NULL constants");
        return E_FAIL;
    }

    TraceLn4(NWT_TRACE_VERBOSE, "  vals: %f %f %f %f", values[0], values[1], values[2], values[3]);

    IDirect3DDevice9Ex *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res = pd3dDevice->SetPixelShaderConstantF(reg, values, count);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "d3d_shader_set_constants_f: SetPixelShaderConstantF failed");
    }

    return res;
}

/* ------------------------------------------------------------------------------------------------
 * 2D context state (formerly D3DGraphics.cc, D3DContext.cc)
 * ---------------------------------------------------------------------------------------------- */

/* D3DSwapChain.nPresent */
int32_t d3d_swapchain_present(void* ctx, void* swapchain)
{
    TraceLn(NWT_TRACE_INFO, "d3d_swapchain_present");

    D3DContext *pCtx = (D3DContext*) ctx;

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pSwapChainRes = (D3DResource*) swapchain;

    RETURN_STATUS_IF_NULL(pSwapChainRes, E_FAIL);

    pCtx->EndScene();

    RECT r = { 0, 0, (LONG) pSwapChainRes->GetDesc()->Width, (LONG) pSwapChainRes->GetDesc()->Height };
    return pSwapChainRes->GetSwapChain()->Present(0, &r, 0, 0, 0);
}

/* D3DContext.nGetFrameStats; the 8 SetIntField calls become the struct. */
int32_t d3d_context_get_frame_stats(void* ctx, D3dFrameStats* out, int32_t reset)
{
    RETURN_STATUS_IF_NULL(out, 0);

#if defined PERF_COUNTERS
    D3DContext *pCtx = (D3DContext*) ctx;

    RETURN_STATUS_IF_NULL(pCtx, 0);

    D3DContext::FrameStats &st = pCtx->getStats();

    out->num_triangles_drawn = st.numTrianglesDrawn;
    out->num_draw_calls = st.numDrawCalls;
    out->num_buffer_locks = st.numBufferLocks;
    out->num_texture_locks = st.numTextureLocks;
    out->num_texture_transfer_bytes = st.numTextureTransferBytes;
    out->num_set_texture = st.numSetTexture;
    out->num_set_pixel_shader = st.numSetPixelShader;
    out->num_render_target_switch = st.numRenderTargetSwitch;

    if (reset) st.clear();

    return 1;
#else
    (void) ctx;
    (void) reset;
    return 0;
#endif
}

/* D3DContext.nDrawIndexedQuads */
int32_t d3d_context_draw_indexed_quads(void* ctx, const float* coords, const uint8_t* colors, int32_t num_verts)
{
    TraceLn(NWT_TRACE_INFO, "d3d_context_draw_indexed_quads");

    D3DContext *pCtx = (D3DContext *) ctx;

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    PrismSourceVertex const *pSrcFloats = (PrismSourceVertex const *) coords;
    BYTE const *pSrcColors = (BYTE const *) colors;

    return (pSrcFloats && pSrcColors && num_verts > 0)
        ? pCtx->drawIndexedQuads(pSrcFloats, pSrcColors, num_verts) : E_FAIL;
}

/* D3DGraphics.nClear */
int32_t d3d_context_clear(void* ctx, int32_t color_argb_pre, int32_t clear_depth, int32_t ignore_scissor)
{
    TraceLn(NWT_TRACE_INFO, "d3d_context_clear");

    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    HRESULT res = pCtx->BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    return pCtx->Clear((DWORD) color_argb_pre, clear_depth != 0, ignore_scissor != 0);
}

/* D3DContext.nSetBlendEnabled (carried verbatim, including the blend factors
 * that stay unset for a mode outside D3dCompMode - Java never passes one) */
int32_t d3d_context_set_blend_mode(void* ctx, int32_t comp_mode)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    IDirect3DDevice9Ex *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res;
    D3DBLEND srcBlend, dstBlend;
    BOOL enable = TRUE;

    switch (comp_mode) {
        case D3D_COMPMODE_CLEAR:
            srcBlend = D3DBLEND_ZERO;
            dstBlend = D3DBLEND_ZERO;
            break;
        case D3D_COMPMODE_SRC:
            enable = FALSE;
            break;
        case D3D_COMPMODE_SRCOVER:
            srcBlend = D3DBLEND_ONE;
            dstBlend = D3DBLEND_INVSRCALPHA;
            break;
        case D3D_COMPMODE_DSTOUT:
            srcBlend = D3DBLEND_ZERO;
            dstBlend = D3DBLEND_INVSRCALPHA;
            break;
        case D3D_COMPMODE_ADD:
            srcBlend = D3DBLEND_ONE;
            dstBlend = D3DBLEND_ONE;
            break;
    }
    res = pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, enable);
    if (enable) {
        res = pd3dDevice->SetRenderState(D3DRS_SRCBLEND, srcBlend);
        res = pd3dDevice->SetRenderState(D3DRS_DESTBLEND, dstBlend);
    }

    return res;
}

/* D3DContext.nSetRenderTarget */
int32_t d3d_context_set_render_target(void* ctx, void* target_res, int32_t depth_buffer, int32_t msaa)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pRes = (D3DResource *) target_res;
    RETURN_STATUS_IF_NULL(pRes, E_FAIL);

    IDirect3DSurface9 *pRenderTarget = pRes->GetSurface();
    RETURN_STATUS_IF_NULL(pRenderTarget, E_FAIL);

    IDirect3DSurface9 *pDepthBuffer = pRes->GetDepthSurface();

    HRESULT res = pCtx->SetRenderTarget(pRenderTarget, &pDepthBuffer, depth_buffer != 0, msaa != 0);
    pRes->SetDepthSurface(pDepthBuffer);
    return res;
}

/* D3DContext.nSetTexture */
int32_t d3d_context_set_texture(void* ctx, void* tex_res_or_null, int32_t unit, int32_t linear, int32_t wrap_mode)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pRes = (D3DResource *) tex_res_or_null;

#if defined PERF_COUNTERS
    pCtx->getStats().numSetTexture++;
#endif

    HRESULT res = pCtx->BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    IDirect3DDevice9Ex *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    IDirect3DTexture9 *pTex = pRes == NULL ? NULL : pRes->GetTexture();
    res = pd3dDevice->SetTexture(unit, pTex);
    RETURN_STATUS_IF_FAILED(res);

    if (pTex != NULL) {
        D3DTEXTUREFILTERTYPE fhint = linear ? D3DTEXF_LINEAR : D3DTEXF_POINT;
        pd3dDevice->SetSamplerState(unit, D3DSAMP_MAGFILTER, fhint);
        pd3dDevice->SetSamplerState(unit, D3DSAMP_MINFILTER, fhint);
        pd3dDevice->SetSamplerState(unit, D3DSAMP_MIPFILTER, fhint);
        if (wrap_mode != 0) {
            pd3dDevice->SetSamplerState(unit, D3DSAMP_ADDRESSU, wrap_mode);
            pd3dDevice->SetSamplerState(unit, D3DSAMP_ADDRESSV, wrap_mode);
        }
    }

    return res;
}

/* D3DContext.nSetCameraPosition */
int32_t d3d_context_set_camera_position(void* ctx, double x, double y, double z)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->SetCameraPosition(x, y, z);
}

/* D3DContext.nSetProjViewMatrix; the 16 by-value doubles come by pointer. */
int32_t d3d_context_set_proj_view_matrix(void* ctx, int32_t depth_test, const double* m16)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(m16, E_FAIL);

    return pCtx->SetProjViewMatrix(depth_test != 0,
        m16[0],  m16[1],  m16[2],  m16[3],
        m16[4],  m16[5],  m16[6],  m16[7],
        m16[8],  m16[9],  m16[10], m16[11],
        m16[12], m16[13], m16[14], m16[15]);
}

/* D3DContext.nSetTransform */
int32_t d3d_context_set_transform(void* ctx, const double* m16)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(m16, E_FAIL);

    return pCtx->SetTransform(m16[0],  m16[1],  m16[2],  m16[3],
                              m16[4],  m16[5],  m16[6],  m16[7],
                              m16[8],  m16[9],  m16[10], m16[11],
                              m16[12], m16[13], m16[14], m16[15]);
}

/* D3DContext.nResetTransform */
int32_t d3d_context_reset_transform(void* ctx)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->ResetTransform();
}

/* D3DContext.nSetWorldTransformToIdentity (NULL) / nSetWorldTransform */
void d3d_context_set_world_transform(void* ctx, const double* m16_or_null)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_IF_NULL(pCtx);

    if (m16_or_null == NULL) {
        pCtx->setWorldTransformIndentity();
    } else {
        const double *m16 = m16_or_null;
        pCtx->setWorldTransform(m16[0],  m16[1],  m16[2],  m16[3],
                                m16[4],  m16[5],  m16[6],  m16[7],
                                m16[8],  m16[9],  m16[10], m16[11],
                                m16[12], m16[13], m16[14], m16[15]);
    }
}

/* D3DContext.nSetClipRect */
int32_t d3d_context_set_clip_rect(void* ctx, int32_t x1, int32_t y1, int32_t x2, int32_t y2)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->SetRectClip(x1, y1, x2, y2);
}

/* D3DContext.nResetClipRect */
int32_t d3d_context_reset_clip_rect(void* ctx)
{
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->ResetClip();
}

/* D3DContext.nSetDeviceParametersFor2D */
int32_t d3d_context_set_device_parameters_2d(void* ctx)
{
    TraceLn(NWT_TRACE_INFO, "d3d_context_set_device_parameters_2d");
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, S_FALSE);

    return pCtx->setDeviceParametersFor2D();
}

/* D3DContext.nSetDeviceParametersFor3D */
int32_t d3d_context_set_device_parameters_3d(void* ctx)
{
    TraceLn(NWT_TRACE_INFO, "d3d_context_set_device_parameters_3d");
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, S_FALSE);

    try {
        return pCtx->setDeviceParametersFor3D();
    } catch (const std::bad_alloc&) {
        return E_OUTOFMEMORY;
    }
}

/* D3DContext.nBlit */
void d3d_context_blit(void* ctx, void* src_res, void* dst_res_or_null,
                      int32_t sx0, int32_t sy0, int32_t sx1, int32_t sy1,
                      int32_t dx0, int32_t dy0, int32_t dx1, int32_t dy1)
{
    TraceLn(NWT_TRACE_INFO, "d3d_context_blit");
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_IF_NULL(pCtx);

    D3DResource *srcRes = (D3DResource*) src_res;
    if (srcRes == NULL) {
        TraceLn(NWT_TRACE_INFO, "   error srcRes is NULL");
        return;
    }

    IDirect3DSurface9 *pSrcSurface = srcRes->GetSurface();
    if (pSrcSurface == NULL) {
        TraceLn(NWT_TRACE_INFO, "   error pSrcSurface is NULL");
        return;
    }

    D3DResource *dstRes = (D3DResource*) dst_res_or_null;
    IDirect3DSurface9 *pDstSurface = (dstRes == NULL) ? NULL : dstRes->GetSurface();

    pCtx->stretchRect(pSrcSurface, sx0, sy0, sx1, sy1,
                      pDstSurface, dx0, dy0, dx1, dy1);
}

/* ------------------------------------------------------------------------------------------------
 * 3D (formerly D3DContext.cc)
 * ---------------------------------------------------------------------------------------------- */

/* D3DContext.nCreateD3DMesh */
void* d3d_mesh_create(void* ctx)
{
    TraceLn(NWT_TRACE_INFO, "d3d_mesh_create");
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, NULL);

    try {
        D3DMesh *mesh = new D3DMesh(pCtx);
        return mesh;
    } catch (const std::bad_alloc&) {
        return NULL;
    }
}

/* D3DContext.nReleaseD3DMesh (the unused ctx argument is dropped) */
void d3d_mesh_release(void* mesh)
{
    TraceLn(NWT_TRACE_INFO, "d3d_mesh_release");
    D3DMesh *pMesh = (D3DMesh *) mesh;
    if (pMesh) {
        delete pMesh;
    }
}

/* D3DContext.nBuildNativeGeometryShort / nBuildNativeGeometryInt. The
 * JNI compared vbSize/ibSize against the Java array lengths; with plain pointers the caller (the
 * segment bounds) guarantees vb_len floats and ib_len indices are readable. */
int32_t d3d_mesh_build_geometry(void* mesh, const float* vb, int32_t vb_len,
                                const void* ib, int32_t ib_len, int32_t index_type)
{
    TraceLn(NWT_TRACE_INFO, "d3d_mesh_build_geometry");
    D3DMesh *pMesh = (D3DMesh *) mesh;
    RETURN_STATUS_IF_NULL(pMesh, 0);

    if (vb_len < 0 || ib_len < 0) {
        return 0;
    }

    UINT uvbSize = (UINT) vb_len;
    UINT uibSize = (UINT) ib_len;

    // D3DMesh::buildBuffers takes non-const pointers and only reads through them
    float *vertexBuffer = (float *) vb;
    if (vertexBuffer == NULL) {
        return 0;
    }

    if (ib == NULL) {
        return 0;
    }

    boolean result;
    switch (index_type) {
        case D3D_INDEX_16:
            result = pMesh->buildBuffers(vertexBuffer, uvbSize, (USHORT *) ib, uibSize);
            break;
        case D3D_INDEX_32:
            result = pMesh->buildBuffers(vertexBuffer, uvbSize, (UINT *) ib, uibSize);
            break;
        default:
            return 0;
    }

    return result ? 1 : 0;
}

/* D3DContext.nCreateD3DPhongMaterial */
void* d3d_material_create(void* ctx)
{
    TraceLn(NWT_TRACE_INFO, "d3d_material_create");
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, NULL);

    try {
        D3DPhongMaterial *phongMaterial = new D3DPhongMaterial(pCtx);
        return phongMaterial;
    } catch (const std::bad_alloc&) {
        return NULL;
    }
}

/* D3DContext.nReleaseD3DPhongMaterial (the unused ctx argument is dropped) */
void d3d_material_release(void* material)
{
    TraceLn(NWT_TRACE_INFO, "d3d_material_release");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) material;
    if (phongMaterial) {
        delete phongMaterial;
    }
}

/* D3DContext.nSetDiffuseColor */
void d3d_material_set_diffuse_color(void* material, float r, float g, float b, float a)
{
    TraceLn(NWT_TRACE_INFO, "d3d_material_set_diffuse_color");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) material;
    RETURN_IF_NULL(phongMaterial);

    phongMaterial->setDiffuseColor(r, g, b, a);
}

/* D3DContext.nSetSpecularColor */
void d3d_material_set_specular_color(void* material, int32_t set, float r, float g, float b, float a)
{
    TraceLn(NWT_TRACE_INFO, "d3d_material_set_specular_color");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) material;
    RETURN_IF_NULL(phongMaterial);

    phongMaterial->setSpecularColor(set ? true : false, r, g, b, a);
}

/* D3DContext.nSetMap with nGetNativeTextureObject folded in: the JNI took the
 * IDirect3DTexture9* that D3DResourceFactory_nGetNativeTextureObject read from the resource. */
void d3d_material_set_map(void* material, int32_t map_type, void* texture_res_or_null)
{
    TraceLn(NWT_TRACE_INFO, "d3d_material_set_map");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) material;
    D3DResource *pResource = (D3DResource *) texture_res_or_null;
    IDirect3DBaseTexture9 *texMap = pResource == NULL ? NULL : pResource->GetTexture();
    RETURN_IF_NULL(phongMaterial);

    phongMaterial->setMap(map_type, texMap);
}

/* D3DContext.nCreateD3DMeshView */
void* d3d_meshview_create(void* ctx, void* mesh)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_create");
    D3DContext *pCtx = (D3DContext*) ctx;
    RETURN_STATUS_IF_NULL(pCtx, NULL);

    D3DMesh *pMesh = (D3DMesh *) mesh;
    RETURN_STATUS_IF_NULL(pMesh, NULL);

    try {
        D3DMeshView *meshView = new D3DMeshView(pCtx, pMesh);
        return meshView;
    } catch (const std::bad_alloc&) {
        return NULL;
    }
}

/* D3DContext.nReleaseD3DMeshView (the unused ctx argument is dropped) */
void d3d_meshview_release(void* meshview)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_release");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    if (meshView) {
        delete meshView;
    }
}

/* D3DContext.nSetCullingMode */
void d3d_meshview_set_culling_mode(void* meshview, int32_t cull_mode)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_set_culling_mode");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    RETURN_IF_NULL(meshView);

    int cullMode = cull_mode;
    switch (cull_mode) {
        case D3D_CULL_BACK:
            cullMode = D3DCULL_CW;
            break;
        case D3D_CULL_FRONT:
            cullMode = D3DCULL_CCW;
            break;
        case D3D_CULL_NONE:
            cullMode = D3DCULL_NONE;
            break;
    }
    meshView->setCullingMode(cullMode);
}

/* D3DContext.nSetMaterial */
void d3d_meshview_set_material(void* meshview, void* material)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_set_material");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    RETURN_IF_NULL(meshView);

    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) material;
    meshView->setMaterial(phongMaterial);
}

/* D3DContext.nSetWireframe */
void d3d_meshview_set_wireframe(void* meshview, int32_t wireframe)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_set_wireframe");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    RETURN_IF_NULL(meshView);

    meshView->setWireframe(wireframe ? true : false);
}

/* D3DContext.nSetAmbientLight */
void d3d_meshview_set_ambient_light(void* meshview, float r, float g, float b)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_set_ambient_light");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    RETURN_IF_NULL(meshView);

    meshView->setAmbientLight(r, g, b);
}

/* D3DContext.nSetLight; the 18 by-value floats come by pointer. */
void d3d_meshview_set_light(void* meshview, int32_t index, const float* params18)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_set_light");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    RETURN_IF_NULL(meshView);
    RETURN_IF_NULL(params18);

    const float *p = params18;
    meshView->setLight(index, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11],
            p[12], p[13], p[14], p[15], p[16], p[17]);
}

/* D3DContext.nRenderMeshView */
void d3d_meshview_render(void* meshview)
{
    TraceLn(NWT_TRACE_INFO, "d3d_meshview_render");
    D3DMeshView *meshView = (D3DMeshView *) meshview;
    RETURN_IF_NULL(meshView);

    meshView->render();
}

} // extern "C"
