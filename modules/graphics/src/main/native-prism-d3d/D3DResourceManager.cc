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

#include "D3DPipeline.h"
#include "D3DResourceManager.h"
#include "D3DPipelineManager.h"


/*
 * TODO: Missing types that proper handling for 3D
 * D3DRTYPE_VERTEXBUFFER
 * D3DRTYPE_INDEXBUFFER
 */
void
D3DResource::Init(IDirect3DResource9 *pRes, IDirect3DSwapChain9 *pSC)
{
    TraceLn(NWT_TRACE_INFO, "D3DResource::Init");

    pResource  = NULL;
    pSwapChain = pSC;
    pSurface   = NULL;
    pTexture   = NULL;
    ZeroMemory(&desc, sizeof(desc));
    desc.Format = D3DFMT_UNKNOWN;

    if (pRes != NULL) {
        pResource = pRes;

        D3DRESOURCETYPE type = pResource->GetType();
        switch (type) {
        case D3DRTYPE_TEXTURE:
            // addRef is needed because both pResource and pTexture will be
            // Release()d, and they point to the same object
            pResource->AddRef();
            pTexture = (IDirect3DTexture9*)pResource;
            pTexture->GetSurfaceLevel(0, &pSurface);
            break;
        case D3DRTYPE_SURFACE:
            pResource->AddRef();
            pSurface = (IDirect3DSurface9*)pResource;
            break;
        case D3DRTYPE_CUBETEXTURE:
            ((IDirect3DCubeTexture9*)pResource)->GetLevelDesc(0, &desc);
            break;
        default:
            TraceLn1(NWT_TRACE_VERBOSE, "  resource type=%d", type);
        }
    } else if (pSwapChain != NULL) {
        pSwapChain->GetBackBuffer(0, D3DBACKBUFFER_TYPE_MONO, &pSurface);
    } else {
        TraceLn(NWT_TRACE_VERBOSE, "  pResource == pSwapChain == NULL");
    }

    if (pSurface != NULL) {
        pSurface->GetDesc(&desc);
    }

    SAFE_PRINTLN(pResource);
    SAFE_PRINTLN(pSurface);
    SAFE_PRINTLN(pTexture);
    SAFE_PRINTLN(pSwapChain);
}

D3DResource::~D3DResource()
{
    Release();
}

BOOL
D3DResource::IsDefaultPool()
{
    if (desc.Format != D3DFMT_UNKNOWN) {
        return (desc.Pool == D3DPOOL_DEFAULT);
    }
    return TRUE;
}

void
D3DResource::Release()
{
    TraceLn(NWT_TRACE_INFO, "D3DResource::Release");

    SAFE_PRINTLN(pResource);
    SAFE_PRINTLN(pSurface);
    SAFE_PRINTLN(pTexture);
    SAFE_PRINTLN(pSwapChain);

    // note that it is normal for the SAFE_RELEASE here to complain about
    // remaining references (with debug build and tracing enabled) as long as
    // the last released resource is released cleanly since the resources here
    // depend on each other, for example, for a texture surface belongs to the
    // texture (the latter won't be released until the surface is released)

    SAFE_RELEASE(pSurface);
    SAFE_RELEASE(pTexture);
    SAFE_RELEASE(pResource);
    SAFE_RELEASE(pSwapChain);
}

D3DPixelShaderResource::D3DPixelShaderResource(IDirect3DPixelShader9 *pShader)
{
    TraceLn(NWT_TRACE_INFO, "D3DPixelShaderResource::D3DPixelShaderResource");
    TraceLn1(NWT_TRACE_VERBOSE, "  pShader=0x%x", pShader);

    this->pShader = pShader;
}

D3DPixelShaderResource::~D3DPixelShaderResource()
{
    Release();
}

void
D3DPixelShaderResource::Release()
{
    TraceLn(NWT_TRACE_INFO, "D3DPixelShaderResource::Release");

    SAFE_PRINTLN(pShader);

    SAFE_RELEASE(pShader);
}

D3DVertexBufferResource::D3DVertexBufferResource(IDirect3DVertexBuffer9 *pVB,
                                                 BOOL isDefaultPool)
{
    TraceLn(NWT_TRACE_INFO, "D3DVertexBufferResource::D3DVertexBufferResource");
    TraceLn1(NWT_TRACE_VERBOSE, "  pVertexBuffer=0x%x", pVB);

    this->pVertexBuffer = pVB;
    this->firstIndex = 0;
    this->bIsDefaultPool = isDefaultPool;
}

D3DVertexBufferResource::~D3DVertexBufferResource()
{
    Release();
}

void
D3DVertexBufferResource::Release()
{
    TraceLn(NWT_TRACE_INFO, "D3DVertexBufferResource::Release");

    SAFE_PRINTLN(pVertexBuffer);

    SAFE_RELEASE(pVertexBuffer);

    firstIndex = 0;
}


D3DResourceManager* D3DResourceManager::CreateInstance(D3DContext *pCtx) {
    TraceLn(NWT_TRACE_INFO, "D3DRM::CreateInstance");

    return new D3DResourceManager(pCtx);
}

D3DResourceManager::D3DResourceManager(D3DContext *pCtx) {
    this->pCtx = pCtx;
    this->pHead = NULL;
    pBlitOSPSurface = NULL;
}


D3DResourceManager::~D3DResourceManager()
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::~D3DRM");
    ReleaseAll();
}

void D3DResourceManager::ReleaseAll()
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::ReleaseAll");

    IManagedResource* pCurrent;
    while (pHead != NULL) {
        pCurrent = pHead;
        pHead = pHead->pNext;
        delete pCurrent;
    }
    pBlitOSPSurface       = NULL;
}

void
D3DResourceManager::ReleaseDefPoolResources()
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::ReleaseDefPoolResources");

    IManagedResource* pCurrent = pHead;
    IManagedResource* pNext = NULL;
    while (pCurrent != NULL) {
        pNext = pCurrent->pNext;
        if (pCurrent->IsDefaultPool()) {
            ReleaseResource(pCurrent);
        }
        pCurrent = pNext;
    }
}

HRESULT
D3DResourceManager::ReleaseResource(IManagedResource* pResource)
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::ReleaseResource");

    if (pResource != NULL) {
        TraceLn1(NWT_TRACE_VERBOSE, "  releasing pResource=%x", pResource);
        if (pResource->pPrev != NULL) {
            pResource->pPrev->pNext = pResource->pNext;
        } else {
            // it's the head
            pHead = pResource->pNext;
            if (pHead != NULL) {
                pHead->pPrev = NULL;
            }
        }
        if (pResource->pNext != NULL) {
            pResource->pNext->pPrev = pResource->pPrev;
        }
        delete pResource;
    }
    return S_OK;
}

HRESULT
D3DResourceManager::AddResource(IManagedResource* pResource)
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::AddResource");

    if (pResource != NULL) {
        TraceLn1(NWT_TRACE_VERBOSE, "  pResource=%x", pResource);
        pResource->pPrev = NULL;
        pResource->pNext = pHead;
        if (pHead != NULL) {
            pHead->pPrev = pResource;
        }
        pHead = pResource;
    }

    return S_OK;
}

HRESULT
D3DResourceManager::CreatePixelShader(DWORD *buf,
                                      D3DPixelShaderResource **ppPSRes)
{
    HRESULT res;
    IDirect3DPixelShader9 *pShader = NULL;

    TraceLn(NWT_TRACE_INFO, "D3DRM::CreatePixelShader");

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    if (SUCCEEDED(res = pd3dDevice->CreatePixelShader(buf, &pShader))) {
        TraceLn1(NWT_TRACE_VERBOSE, "  created pixel shader: 0x%x", pShader);
        *ppPSRes = new D3DPixelShaderResource(pShader);
        res = AddResource(*ppPSRes);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreatePixelShader failed");
        *ppPSRes = NULL;
    }

    return res;
}

HRESULT
D3DResourceManager::CreateVertexBuffer(D3DVertexBufferResource** ppVBRes)
{
    HRESULT res;
    IDirect3DVertexBuffer9 *pVertexBuffer = NULL;

    TraceLn(NWT_TRACE_INFO, "D3DRM::CreateVertexBuffer");

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    D3DPOOL pool = (pCtx->GetDeviceCaps()->DeviceType == D3DDEVTYPE_HAL) ?
        D3DPOOL_DEFAULT : D3DPOOL_SYSTEMMEM;
    // usage depends on whether we use hw or sw vertex processing
    res = pd3dDevice->CreateVertexBuffer(MAX_BATCH_QUADS * 4 * sizeof(PRISM_VERTEX_2D),
                                         D3DUSAGE_DYNAMIC|D3DUSAGE_WRITEONLY, 0,
                                         pool, &pVertexBuffer, NULL);
    if (SUCCEEDED(res)) {
        TraceLn1(NWT_TRACE_VERBOSE, "  created vertex buffer: 0x%x", pVertexBuffer);
        *ppVBRes = new D3DVertexBufferResource(pVertexBuffer, pool == D3DPOOL_DEFAULT);
        res = AddResource(*ppVBRes);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateVertexBuffer failed");
        *ppVBRes = NULL;
    }

    return res;
}

HRESULT
D3DResourceManager::CreateTexture(UINT width, UINT height,
                                  BOOL isRTT, BOOL isOpaque, /*BOOL autoMipMap,*/
                                  D3DFORMAT *pFormat, DWORD dwUsage,
                                  D3DResource **ppTextureResource)
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::CreateTexture");
    TraceLn4(NWT_TRACE_VERBOSE, "  w=%d h=%d isRTT=%d isOpaque=%d",
                width, height, isRTT, isOpaque);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();

    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    D3DFORMAT format;
    if (pFormat != NULL && *pFormat != D3DFMT_UNKNOWN) {
        format = *pFormat;
    } else {
        if (isOpaque) {
            format = D3DFMT_X8R8G8B8;
        } else {
            format = D3DFMT_A8R8G8B8;
        }
    }

    DWORD isDynamic = dwUsage & D3DUSAGE_DYNAMIC;

    D3DPOOL pool;
    if (isRTT) {
        dwUsage = D3DUSAGE_RENDERTARGET;
        pool = D3DPOOL_DEFAULT;
    } else {
        if (dwUsage == D3DUSAGE_DYNAMIC && !pCtx->IsDynamicTextureSupported()) {
            dwUsage = 0;
        }
        if (dwUsage == D3DUSAGE_DYNAMIC) {
            pool = D3DPOOL_DEFAULT;
        } else {
            pool = pCtx->getResourcePool();
        }
    }

    if (pCtx->IsPow2TexturesOnly()) {
          UINT w, h;
          for (w = 1; width  > w; w <<= 1);
          for (h = 1; height > h; h <<= 1);
          width = w;
          height = h;
    }
    if (pCtx->IsSquareTexturesOnly()) {
        if (width > height) {
            height = width;
        } else {
            width = height;
        }
    }

    // if (pool == D3DPOOL_MANAGED && !isDynamic && autoMipMap) {
    //     dwUsage |= D3DUSAGE_AUTOGENMIPMAP;
    // }

    IDirect3DTexture9 *pTexture = NULL;
    HRESULT res = pd3dDevice->CreateTexture(width, height, 1/*levels*/, dwUsage,
                                    format, pool, &pTexture, 0);
    if (SUCCEEDED(res)) {
        TraceLn1(NWT_TRACE_VERBOSE, "  created texture: 0x%x", pTexture);
        *ppTextureResource = new D3DResource((IDirect3DResource9*)pTexture);
        res = AddResource(*ppTextureResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateTexture failed");
        *ppTextureResource = NULL;
        format = D3DFMT_UNKNOWN;
    }

    if (pFormat != NULL) {
        *pFormat = format;
    }

    return res;
}


HRESULT D3DResourceManager::CreateOSPSurface(UINT width, UINT height,
                                         D3DFORMAT fmt,
                                         D3DResource** ppSurfaceResource/*out*/)
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::CreateOSPSurface");
    TraceLn2(NWT_TRACE_VERBOSE, "  w=%d h=%d", width, height);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    // since the off-screen plain surface is intended to be used with
    // the UpdateSurface() method, it is essential that it be created
    // in the same format as the destination and allocated in the
    // SYSTEMMEM pool (otherwise UpdateSurface() will fail)
    D3DFORMAT format;
    if (fmt == D3DFMT_UNKNOWN) {
        format = pCtx->GetPresentationParams()->BackBufferFormat;
    } else {
        format = fmt;
    }
    D3DPOOL pool = D3DPOOL_SYSTEMMEM;
    IDirect3DSurface9 *pSurface = NULL;

    HRESULT res = pd3dDevice->CreateOffscreenPlainSurface(width, height,
                                                  format, pool,
                                                  &pSurface, NULL);
    if (SUCCEEDED(res)) {
        TraceLn1(NWT_TRACE_VERBOSE, "  created OSP Surface: 0x%x ",pSurface);
        *ppSurfaceResource = new D3DResource((IDirect3DResource9*)pSurface);
        res = AddResource(*ppSurfaceResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateOSPSurface failed");
        ppSurfaceResource = NULL;
    }
    return res;
}

HRESULT
D3DResourceManager::CreateSwapChain(HWND hWnd, UINT numBuffers,
                                    UINT width, UINT height,
                                    D3DSWAPEFFECT swapEffect,
                                    UINT presentationInterval,
                                    D3DResource ** ppSwapChainResource)
{
    TraceLn(NWT_TRACE_INFO, "D3DRM::CreateSwapChain");
    TraceLn4(NWT_TRACE_VERBOSE, "  w=%d h=%d hwnd=%x numBuffers=%d",
                width, height, hWnd, numBuffers);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    D3DPRESENT_PARAMETERS newParams = {};

    newParams.BackBufferWidth = width;
    newParams.BackBufferHeight = height;
    newParams.hDeviceWindow = hWnd;
    newParams.Windowed = TRUE;
    newParams.BackBufferCount = numBuffers;
    newParams.SwapEffect = swapEffect;
    newParams.PresentationInterval = presentationInterval;

    IDirect3DSwapChain9 *pSwapChain = NULL;
    HRESULT res = pd3dDevice->CreateAdditionalSwapChain(&newParams, &pSwapChain);

    if (SUCCEEDED(res)) {
        TraceLn1(NWT_TRACE_VERBOSE,"  created swap chain: 0x%x ",pSwapChain);
        *ppSwapChainResource = new D3DResource(pSwapChain);
        res = AddResource(*ppSwapChainResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateSwapChain failed");
        *ppSwapChainResource = NULL;
    }
    return res;
}


HRESULT
D3DResourceManager::GetBlitOSPSurface(UINT width, UINT height, D3DFORMAT fmt,
                                      D3DResource **ppSurfaceResource)
{
    HRESULT res = S_OK;

    TraceLn(NWT_TRACE_INFO, "D3DRM::GetBlitOSPSurface");
    RETURN_STATUS_IF_NULL(ppSurfaceResource, E_FAIL);

    if (pBlitOSPSurface != NULL) {
        D3DSURFACE_DESC *pDesc = pBlitOSPSurface->GetDesc();
        if (width == pDesc->Width && height == pDesc->Height &&
            (fmt == pDesc->Format || fmt == D3DFMT_UNKNOWN))
        {
            *ppSurfaceResource = pBlitOSPSurface;
            return res;
        }
        // current surface doesn't fit, release and allocate a new one
        ReleaseResource(pBlitOSPSurface);
        pBlitOSPSurface = NULL;
    }

    res = CreateOSPSurface(width, height, fmt, &pBlitOSPSurface);
    *ppSurfaceResource = pBlitOSPSurface;

    return res;
}


