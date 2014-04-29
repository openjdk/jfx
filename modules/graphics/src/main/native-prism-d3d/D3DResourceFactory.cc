/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_prism_d3d_D3DResourceFactory.h"

#include "D3DPipelineManager.h"
#include "D3DResourceManager.h"
#include "D3DContext.h"

#include "TextureUploader.h"

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nGetContext
 * Signature: (I)J
 */

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nGetContext
  (JNIEnv *jEnv, jclass, jint adapterOrdinal)
{
    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    RETURN_STATUS_IF_NULL(pMgr, 0L);
    
    D3DContext *pCtx = NULL;
    HRESULT res = pMgr->GetD3DContext(adapterOrdinal, &pCtx);
    if (SUCCEEDED(res)) {
        pCtx->ResetClip();
        pCtx->ResetTransform();

        return ptr_to_jlong(pCtx);
    }

    return 0L;

}


/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nCreateTexture
 */
JNIEXPORT jlong JNICALL
Java_com_sun_prism_d3d_D3DResourceFactory_nCreateTexture
  (JNIEnv *env, jclass klass,
        jlong ctx, jint formatHint, jint usageHint, jboolean isRTT,
        jint width, jint height, jint samples)
{
    TraceLn5(NWT_TRACE_INFO,
             "nCreateTexture formatHint=%d usageHint=%d isRTT=%d w=%d h=%d",
             formatHint, usageHint, isRTT, width, height);

    D3DContext *pCtx = (D3DContext *)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, 0L);

    D3DResourceManager *pMgr = pCtx->GetResourceManager();
    RETURN_STATUS_IF_NULL(pMgr, 0L);
    
    D3DResource *pTexResource;
    D3DFORMAT format = D3DFMT_UNKNOWN;
    HRESULT res;
    
    // only considered when the format isn't explicitly requested
    BOOL isOpaque = FALSE;

    if (usageHint == 1) {
        OutputDebugStringA("Texture.Usage.DYNAMIC");
    }

    DWORD dwUsage = usageHint == 1/*Texture.Usage.DYNAMIC*/ ? D3DUSAGE_DYNAMIC : 0;

    // formatHint is the hint about the content of the texture, not a hard
    // requirement
    switch (formatHint) {
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
                        "nCreateTexture: unknown format hint: %d", formatHint);
            break;
    }

    if (samples) {
        // assert isRTT == true
        D3DMULTISAMPLE_TYPE msType = static_cast<D3DMULTISAMPLE_TYPE>(samples);
        res = pMgr->CreateRenderTarget(width, height, isOpaque,
                &format, msType, &pTexResource);
    } else {
        res = pMgr->CreateTexture(width, height, isRTT, isOpaque,
                &format, dwUsage, &pTexResource);
    }
    if (SUCCEEDED(res)) {
        return ptr_to_jlong(pTexResource);
    }

    return 0L;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nCreateSwapChain
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nCreateSwapChain
  (JNIEnv *jEnv, jclass, jlong ctx, jlong hwnd, jboolean isVsyncEnabled)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, 0L);
    
    HWND hWnd = (HWND)(jlong_to_ptr(hwnd));
    if (!::IsWindow(hWnd)) {
        TraceLn1(NWT_TRACE_ERROR, "nGetSwapChain: hwnd=%x is not a window\n", hWnd);
        return 0L;
    }

    D3DResource *pSwapChainRes = NULL;
    HRESULT res = pCtx->GetResourceManager()->
            CreateSwapChain(hWnd, 1,
            0, 0,
            // have to use COPY since we don't re-render the scene
            // if it didn't change
            D3DSWAPEFFECT_COPY,
            isVsyncEnabled ?
            D3DPRESENT_INTERVAL_ONE :
            D3DPRESENT_INTERVAL_IMMEDIATE,
            &pSwapChainRes);

    if (SUCCEEDED(res)) {
        return ptr_to_jlong(pSwapChainRes);
    }

    return 0L;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nReleaseResource
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nReleaseResource
  (JNIEnv *env, jclass, jlong ctx, jlong resource)
{
    IManagedResource *pResource = (IManagedResource*)jlong_to_ptr(resource);
    RETURN_STATUS_IF_NULL(pResource, D3D_OK);

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, S_FALSE);

    return pCtx->GetResourceManager()->ReleaseResource(pResource);
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nGetMaximumTextureSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nGetMaximumTextureSize
  (JNIEnv *, jclass, jlong ctx)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);    
    RETURN_STATUS_IF_NULL(pCtx, -1);

    D3DCAPS9 *caps = pCtx->GetDeviceCaps();
    RETURN_STATUS_IF_NULL(caps, -1);

    DWORD maxw = caps->MaxTextureWidth;
    DWORD maxh = caps->MaxTextureHeight;
    DWORD max = (maxw < maxh) ? maxw : maxh;
    return (jint)max;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nGetTextureWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nGetTextureWidth
  (JNIEnv *, jclass, jlong resource)
{
    D3DResource *pResource = (D3DResource*)jlong_to_ptr(resource);
    RETURN_STATUS_IF_NULL(pResource, -1);

    return (jint)pResource->GetDesc()->Width;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nGetTextureHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nGetTextureHeight
  (JNIEnv *, jclass, jlong resource)
{
    D3DResource *pResource = (D3DResource*)jlong_to_ptr(resource);
    RETURN_STATUS_IF_NULL(pResource, -1);

    return (jint)pResource->GetDesc()->Height;
}

/*
 * Note: this method assumes that pCtx, pTexResource and pixels are not null
 */
inline HRESULT updateTexture(
    D3DContext *pCtx, D3DResource *pTexResource, PBYTE pixels, jint size, jint format,
    jint dstx, jint dsty, jint srcx, jint srcy, jint srcw, jint srch, jint srcscan)
{

    D3DSURFACE_DESC * desc = pTexResource->GetDesc();

    bool paramsOK = TextureUpdater::validateArguments(
        dstx, dsty, desc->Width, desc->Height,
        srcx, srcy, srcw, srch,
        size, PFormat(format), srcscan);

    RETURN_STATUS_IF_NULL(paramsOK, E_INVALIDARG);

    TraceLn7(NWT_TRACE_VERBOSE, "updateTexture src = [%d, %d]-[%dx%d], pixels = %p, dst = [%dx%d]", srcx, srcy, srcw, srch, pixels, dstx, dsty);

    TextureUpdater updater;
    updater.setTarget(pTexResource->GetTexture(), pTexResource->GetSurface(), desc, dstx, dsty);
    updater.setSource(pixels, size, PFormat(format), srcx, srcy, srcw, srch, srcscan);

    int nBytes = pCtx->Get3DExDevice()
        ? updater.updateD3D9ExTexture(pCtx)
        : updater.updateLockableTexture();

#if defined PERF_COUNTERS
    D3DContext::FrameStats &stats = pCtx->getStats();
    stats.numTextureLocks++;
    stats.numTextureTransferBytes += nBytes;
#endif

    return nBytes ? S_OK : E_FAIL;
}


// note that
//  - Java_com_sun_prism_d3d_D3DResourceFactory_nUpdateTextureI
//  - Java_com_sun_prism_d3d_D3DResourceFactory_nUpdateTextureB
//  - Java_com_sun_prism_d3d_D3DResourceFactory_nUpdateTextureF
// are completely identical in the body text
// It happens because in JNI we have a baseclass for arrays : jarray,
// but we have not got something similar in java
// is it possible to optimize it somehow ?

typedef D3DContext* PD3DContext;
typedef D3DResource* PD3DResource;

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nUpdateTextureI
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nUpdateTextureI
  (JNIEnv *env, jclass, jlong ctx, jlong resource,
   jobject buf, jintArray pixelArray,
   jint dstx, jint dsty,
   jint srcx, jint srcy,
   jint srcw, jint srch, jint srcscan)
{
    RETURN_STATUS_IF_NULL(ctx, E_FAIL);
    RETURN_STATUS_IF_NULL(resource, E_FAIL);

    jint size = pixelArray ?
        env->GetArrayLength(pixelArray) * sizeof(jint) :
        jint(env->GetDirectBufferCapacity(buf));

    PBYTE pixels = PBYTE((pixelArray != NULL) ?
        env->GetPrimitiveArrayCritical(pixelArray, NULL) :
        env->GetDirectBufferAddress(buf));

    RETURN_STATUS_IF_NULL(pixels, E_OUTOFMEMORY);

    HRESULT res = updateTexture(
        PD3DContext(ctx), PD3DResource(resource), pixels, size, PFORMAT_INT_ARGB_PRE,
        dstx, dsty, srcx, srcy, srcw, srch, srcscan);

    if (pixelArray != NULL) {
        env->ReleasePrimitiveArrayCritical(pixelArray, pixels, JNI_ABORT);
    }

    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nUpdateTextureB
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nUpdateTextureB
  (JNIEnv *env, jclass, jlong ctx, jlong resource,
   jobject buf, jbyteArray pixelArray, jint formatHint,
   jint dstx, jint dsty,
   jint srcx, jint srcy,
   jint srcw, jint srch, jint srcscan)
{
    RETURN_STATUS_IF_NULL(ctx, E_FAIL);
    RETURN_STATUS_IF_NULL(resource, E_FAIL);

    jint size = pixelArray ?
        env->GetArrayLength(pixelArray) * sizeof(jbyte) :
        jint(env->GetDirectBufferCapacity(buf));

    PBYTE pixels = PBYTE((pixelArray != NULL) ?
        env->GetPrimitiveArrayCritical(pixelArray, NULL) :
        env->GetDirectBufferAddress(buf));

    RETURN_STATUS_IF_NULL(pixels, E_OUTOFMEMORY);

    HRESULT res = updateTexture(
        PD3DContext(ctx), PD3DResource(resource), pixels, size, formatHint,
        dstx, dsty, srcx, srcy, srcw, srch, srcscan);

    if (pixelArray != NULL) {
        env->ReleasePrimitiveArrayCritical(pixelArray, pixels, JNI_ABORT);
    }

    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nUpdateTextureF
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nUpdateTextureF
  (JNIEnv *env, jclass, jlong ctx, jlong resource,
   jobject buf, jfloatArray pixelArray,
   jint dstx, jint dsty,
   jint srcx, jint srcy,
   jint srcw, jint srch, jint srcscan)
{
    RETURN_STATUS_IF_NULL(ctx, E_FAIL);
    RETURN_STATUS_IF_NULL(resource, E_FAIL);

    jint size = pixelArray ?
        env->GetArrayLength(pixelArray) * sizeof(jfloat) :
        jint(env->GetDirectBufferCapacity(buf));

    PBYTE pixels = PBYTE((pixelArray != NULL) ?
        env->GetPrimitiveArrayCritical(pixelArray, NULL) :
        env->GetDirectBufferAddress(buf));

    RETURN_STATUS_IF_NULL(pixels, E_OUTOFMEMORY);

    HRESULT res = updateTexture(
        PD3DContext(ctx), PD3DResource(resource), pixels, size, PFORMAT_FLOAT_XYZW,
        dstx, dsty, srcx, srcy, srcw, srch, srcscan);

    if (pixelArray != NULL) {
        env->ReleasePrimitiveArrayCritical(pixelArray, pixels, JNI_ABORT);
    }

    return res;
}



static void copy_X8R8G8B8(DWORD *pDstPixels, DWORD const *pSrcPixels, int n) {
    for (int i = 0; i!=n; ++i) {
        pDstPixels[i] = pSrcPixels[i] | 0xff000000;
    }
}

/*
 * Note: this method assumes that pCtx, pResource and pixels are not null
 */
static HRESULT D3DResourceFactory_nReadPixels(D3DContext *pCtx, D3DResource *pResource, BYTE *pixels, int cntW, int cntH)
{

    TraceLn(NWT_TRACE_INFO, "D3DResourceFactory_nReadPixels");

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
    
    IDirect3DSurface9 *pSrc = pResource->GetSurface();
    RETURN_STATUS_IF_NULL(pSrc, E_FAIL);

    D3DFORMAT srcFmt = pResource->GetDesc()->Format;
    UINT srcw = pResource->GetDesc()->Width;
    UINT srch = pResource->GetDesc()->Height;

    if (srcFmt != D3DFMT_A8R8G8B8 && srcFmt != D3DFMT_X8R8G8B8) {
        RlsTraceLn1(NWT_TRACE_ERROR,
            "D3DResourceFactory_nReadPixels doesn't support format %d", srcFmt);
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
                    "D3DResourceFactory_nReadPixels lock failed res=%x", res);
                return res;
            }
            // assuming int (a|x)rgb type, and 0,0 source coordinates

            BYTE const *pSrcPixels = PBYTE(lockedRect.pBits);
            BYTE *pDstPixels = pixels;

            switch (srcFmt) {
            case D3DFMT_A8R8G8B8:
                for (int y=0; y!=cntH; ++y) {
                    // cntW, cntH are sanity checked in ReadPixelsHelper function
                    memcpy(pDstPixels, pSrcPixels, cntW*4);
                    pSrcPixels += lockedRect.Pitch;
                    pDstPixels += cntW*4;
                }
                break;
            case D3DFMT_X8R8G8B8:
                for (int y=0; y!=cntH; ++y) {
                    // cntW, cntH are sanity checked in ReadPixelsHelper function
                    copy_X8R8G8B8(PDWORD(pDstPixels), (DWORD const*)(pSrcPixels), cntW*4);
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

static HRESULT nReadPixelsHelper(
    JNIEnv *env, jlong context, jlong resource, jlong length,
    jobject buf, jarray pixelArray, jint cntW, jint cntH)
{
    BYTE *pixels = PBYTE( pixelArray ?
            env->GetPrimitiveArrayCritical(pixelArray, 0) :
            env->GetDirectBufferAddress(buf));

    RETURN_STATUS_IF_NULL(pixels, E_OUTOFMEMORY);

    // sanity check about we have enought memory
    // Since we are certain cntW and cntH are positive numbers
    if ( UINT(length)/4/cntW < UINT(cntH) ) {
        RlsTraceLn1(NWT_TRACE_ERROR,
                    "D3DResourceFactory_nReadPixels buffer too small: %ld",
                    length);
        return E_OUTOFMEMORY;
    }

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(context);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pResource = (D3DResource*)jlong_to_ptr(resource);
    RETURN_STATUS_IF_NULL(pResource, E_FAIL);

    HRESULT res = D3DResourceFactory_nReadPixels(pCtx, pResource, pixels, cntW, cntH);

    if (pixelArray) {
        env->ReleasePrimitiveArrayCritical(pixelArray, pixels, 0);
    }

    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nReadPixelsI and nReadPixelsB
 * 2Do: better  to make helper class rather than copying all parameters
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nReadPixelsI
(JNIEnv *env, jclass, jlong context, jlong resource, jlong length,
 jobject buf, jintArray pixelArray, jint cntW, jint cntH)
{
    TraceLn(NWT_TRACE_INFO, "D3DResourceFactory_nReadPixelsI");
    return nReadPixelsHelper(env, context, resource, length, buf, pixelArray, cntW, cntH);
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nReadPixelsB
(JNIEnv *env, jclass clazz, jlong context, jlong resource, jlong length,
 jobject buf, jbyteArray pixelArray, jint cntW, jint cntH)
{
    TraceLn(NWT_TRACE_INFO, "D3DResourceFactory_nReadPixelsB");
    return nReadPixelsHelper(env, context, resource, length, buf, pixelArray, cntW, cntH);
}



/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nSetDisposerRecord
 * Signature: (JLcom/sun/prism/d3d/D3DResource/D3DRecord;)V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nIsDefaultPool
  (JNIEnv *env, jclass clazz, jlong resource)
{
    IManagedResource *pResource = (IManagedResource*)jlong_to_ptr(resource);

    RETURN_STATUS_IF_NULL(pResource, FALSE);

    return pResource->IsDefaultPool();
}

/*
 * Class:     com_sun_prism_d3d_D3DResourceFactory
 * Method:    nTestCooperativeLevel
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nTestCooperativeLevel
  (JNIEnv *, jclass, jlong context)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(context);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    return pCtx->TestCooperativeLevel();
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nResetDevice
  (JNIEnv *env, jclass, jlong context)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(context);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    return pCtx->ResetContext();
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nGetDevice
  (JNIEnv *env, jclass, jlong context)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(context);
    RETURN_STATUS_IF_NULL(pCtx, 0L);

    return jlong(pCtx->Get3DDevice());
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DResourceFactory_nGetNativeTextureObject
  (JNIEnv *, jclass, jlong resource)
{
    D3DResource *pResource = (D3DResource*)jlong_to_ptr(resource);
    RETURN_STATUS_IF_NULL(pResource, 0L);

    return jlong(pResource->GetTexture());
}
