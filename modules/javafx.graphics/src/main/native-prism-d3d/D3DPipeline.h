/*
 * Copyright (c) 2007, 2024, Oracle and/or its affiliates. All rights reserved.
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

#if defined _DEBUG && !defined DEBUG
    #define DEBUG
#endif

#ifdef DEBUG
    #define D3D_DEBUG_INFO
#endif

#include <windows.h>
#include <d3d9.h>
#include <stddef.h>

#include "Trace.h"


#ifndef jlong_to_ptr
    #ifdef _WIN64
    #define jlong_to_ptr(a) ((void*)(a))
    #define ptr_to_jlong(a) ((jlong)(a))
    #else
    /* Double casting to avoid warning messages looking for casting of */
    /* smaller sizes into pointers */
    #define jlong_to_ptr(a) ((void*)(int)(a))
    #define ptr_to_jlong(a) ((jlong)(int)(a))
    #endif
#endif

// some helper macros
#define SAFE_RELEASE(RES) \
do {                      \
    if ((RES)!= NULL) {   \
        ULONG refs = (RES)->Release(); \
        if (refs == 0) {   \
            TraceLn1(NWT_TRACE_VERBOSE2, \
                     "Released resource " ## #RES ## "=0x%x successfully", \
                     (RES)); \
        } else {             \
            TraceLn2(NWT_TRACE_WARNING, \
                     "Release: resource " ## #RES ## "=0x%x not released: refs=%d", \
                     (RES), refs); \
        }                 \
        (RES) = NULL;     \
    }                     \
} while (0);

#define SAFE_DELETE(RES)  \
do {                      \
    if ((RES)!= NULL) {   \
        delete (RES);     \
        (RES) = NULL;     \
    }                     \
} while (0);

#ifdef DEBUG
#define SAFE_PRINTLN(RES) \
do {                      \
    if ((RES)!= NULL) {   \
        TraceLn1(NWT_TRACE_VERBOSE, "  " ## #RES ## "=0x%x", (RES)); \
    } else {              \
        TraceLn(NWT_TRACE_VERBOSE, "  " ## #RES ## "=NULL"); \
    }                     \
} while (0);
#else // DEBUG
#define SAFE_PRINTLN(RES)
#endif // DEBUG

/*
 * The following macros allow the caller to return (or continue) if the
 * provided value is NULL.  (The strange else clause is included below to
 * allow for a trailing ';' after RETURN/CONTINUE_IF_NULL() invocations.)
 */
#define ACT_IF_NULL(ACTION, value)         \
    if ((value) == NULL) {                 \
        TraceLn3(NWT_TRACE_ERROR,       \
                    "%s is null in %s:%d", #value, __FILE__, __LINE__); \
        ACTION;                            \
    } else do { } while (0)
#define RETURN_IF_NULL(value)   ACT_IF_NULL(return, value)
#define CONTINUE_IF_NULL(value) ACT_IF_NULL(continue, value)
#define RETURN_STATUS_IF_NULL(value, status) \
        ACT_IF_NULL(return (status), value)

#define DebugPrintD3DError(res, msg) \
    TraceLn1(NWT_TRACE_ERROR, "D3D Error: " ## msg ## " res=0x%08X", res)

#define RETURN_STATUS_IF_EXP_FAILED(EXPR) \
    if (FAILED(res = (EXPR))) {                    \
        DebugPrintD3DError(res, " " ## #EXPR ## " failed in " ## __FILE__); \
        return res;                   \
    } else do { } while (0)

#define RETURN_STATUS_IF_FAILED(status) \
    if (FAILED((status))) {                    \
        DebugPrintD3DError((status), " failed in " ## __FILE__ ## ", return;");\
        return (status);                   \
    } else do { } while (0)

// d3d9 must be valid and tested
int getMaxSampleSupport(IDirect3D9Ex *d3d9, UINT adapter);

inline void logD3DSurfaceDesc(D3DSURFACE_DESC const & dsk) {
    RlsTrace5(NWT_TRACE_INFO, "w=%d, h=%d, Format = %d, Pool=%d, Usage=%d\n",
        dsk.Width, dsk.Height, dsk.Format, dsk.Pool, dsk.Usage);
}

inline void logSurfaceDesk(IDirect3DSurface9 *surf) {
    D3DSURFACE_DESC  dsk;
    return (S_OK == surf->GetDesc( &dsk )) ?
        logD3DSurfaceDesc(dsk) : TraceImpl(NWT_TRACE_INFO, JNI_FALSE, "Error reading surface desk\n");
}

inline void logDeviceTargets(IDirect3DDevice9Ex *pd3dDevice) {
    IDirect3DSurface9 * pSurf=0, *pZB=0;
    HRESULT hr1 = pd3dDevice->GetRenderTarget(0, &pSurf);
    HRESULT hr2 = pd3dDevice->GetDepthStencilSurface(&pZB);

    if (pSurf) {
        TraceImpl(NWT_TRACE_INFO, JNI_FALSE, "RT: ");
        logSurfaceDesk(pSurf);
        int nCnt = pSurf->Release();
    }

    if (pZB) {
        TraceImpl(NWT_TRACE_INFO, JNI_FALSE, "Z: ");
        logSurfaceDesk(pZB);
        int nCnt = pZB->Release();
    }
}
