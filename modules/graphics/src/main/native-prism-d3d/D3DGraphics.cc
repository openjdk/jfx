/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
#include "D3DResourceManager.h"
#include "D3DContext.h"

#include "com_sun_prism_d3d_D3DGraphics.h"
#include "com_sun_prism_d3d_D3DSwapChain.h"
#include "com_sun_prism_d3d_D3DContext.h"
#include "com_sun_prism_d3d_D3DVertexBuffer.h"

/*
 * Class:     com_sun_prism_d3d_D3DSwapChain
 * Method:    nPresent
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DSwapChain_nPresent
  (JNIEnv *, jclass, jlong ctx, jlong swapChain)
{
    TraceLn(NWT_TRACE_INFO, "D3DSwapChain_nPresent");
    
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pSwapChainRes = (D3DResource*)jlong_to_ptr(swapChain);

    RETURN_STATUS_IF_NULL(pSwapChainRes, E_FAIL);

    pCtx->EndScene();

    RECT r = { 0, 0, pSwapChainRes->GetDesc()->Width, pSwapChainRes->GetDesc()->Height };
    return pSwapChainRes->GetSwapChain()->Present(0, &r, 0, 0, 0);
}

void setIntField(JNIEnv *env, jobject object, jclass clazz, const char *name, int value);

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nGetFrameStats
 * Signature: (JLcom/sun/prism/d3d/D3DFrameStats;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d_D3DContext_nGetFrameStats
  (JNIEnv *env, jclass, jlong ctx, jobject pResultObject, jboolean bReset)
{
    RETURN_STATUS_IF_NULL(pResultObject, false);

#if defined PERF_COUNTERS
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    
    RETURN_STATUS_IF_NULL(pCtx, false);
    
    D3DContext::FrameStats &st = pCtx->getStats();

    jclass pResultClass = env->GetObjectClass(pResultObject);
    setIntField(env, pResultObject, pResultClass, "numTrianglesDrawn",  st.numTrianglesDrawn);
    setIntField(env, pResultObject, pResultClass, "numDrawCalls", st.numDrawCalls);
    setIntField(env, pResultObject, pResultClass, "numBufferLocks",  st.numBufferLocks);
    setIntField(env, pResultObject, pResultClass, "numTextureLocks", st.numTextureLocks);
    setIntField(env, pResultObject, pResultClass, "numTextureTransferBytes", st.numTextureTransferBytes);
    setIntField(env, pResultObject, pResultClass, "numSetTexture", st.numSetTexture);
    setIntField(env, pResultObject, pResultClass, "numSetPixelShader", st.numSetPixelShader);
    setIntField(env, pResultObject, pResultClass, "numRenderTargetSwitch", st.numRenderTargetSwitch);

    if (bReset) st.clear();

    return true;
#else
    return false;
#endif
}

/*
 * This is Prism VertexBuffer format for the FloatBuffer passed to nFlush
 */
struct PrismSourceVertex {
    float x, y, z;
    float tu1, tv1;
    float tu2, tv2;
};

/*
 * Class:     com_sun_prism_d3d_D3DVertexBuffer
 * Method:    nFlush
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DVertexBuffer_nDrawIndexedQuads
  (JNIEnv *env, jclass, jlong ctx, jfloatArray fbuf, jbyteArray bbuf, jint remainingVerts)
{
    TraceLn(NWT_TRACE_INFO, "D3DVertexBuffer_nDrawIndexedQuads");

    D3DContext *pCtx = (D3DContext *)jlong_to_ptr(ctx);
    
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    PrismSourceVertex *pSrcFloats = (PrismSourceVertex *)env->GetPrimitiveArrayCritical(fbuf, 0);
    BYTE *pSrcColors = (BYTE *)env->GetPrimitiveArrayCritical(bbuf, 0);

    HRESULT hr = (pSrcFloats && pSrcColors && remainingVerts > 0)
        ? pCtx->drawIndexedQuads(pSrcFloats, pSrcColors, remainingVerts) : E_FAIL;

    if (pSrcColors) env->ReleasePrimitiveArrayCritical(bbuf, pSrcColors, JNI_ABORT);
    if (pSrcFloats) env->ReleasePrimitiveArrayCritical(fbuf, pSrcFloats, JNI_ABORT);

    return hr;
}

/*
 * Class:     com_sun_prism_d3d_D3DVertexBuffer
 * Method:    nDrawTriangleList
*/
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DVertexBuffer_nDrawTriangleList
  (JNIEnv *env, jclass, jlong ctx, jfloatArray fbuf, jbyteArray bbuf, jint numTriangles)
{
    TraceLn(NWT_TRACE_INFO, "D3DVertexBuffer_nDrawTriangleList");

    D3DContext *pCtx = (D3DContext *)jlong_to_ptr(ctx);

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    PrismSourceVertex *pSrcFloats = (PrismSourceVertex *)env->GetPrimitiveArrayCritical(fbuf, 0);
    BYTE *pSrcColors = (BYTE *)env->GetPrimitiveArrayCritical(bbuf, 0);

    HRESULT hr = (pSrcFloats && pSrcColors && numTriangles > 0) ?
        pCtx->drawTriangleList(pSrcFloats, pSrcColors, numTriangles) : E_FAIL;

    if (pSrcColors) env->ReleasePrimitiveArrayCritical(bbuf, pSrcColors, JNI_ABORT);
    if (pSrcFloats) env->ReleasePrimitiveArrayCritical(fbuf, pSrcFloats, JNI_ABORT);
    return hr;
}

/*
 * Note: this method assumes that pVert, pSrcFloats and pSrcColors are not null
 */
void fillVB(PRISM_VERTEX_2D *pVert, PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, UINT numVerts) {
    for (UINT i = 0; i < numVerts; i++) {

        pVert->x = pSrcFloats->x;
        pVert->y = pSrcFloats->y;
        pVert->z = pSrcFloats->z;

        pVert->color =
            (pSrcColors[3]<<24) + (pSrcColors[0]<<16) +
            (pSrcColors[1]<<8 ) +  pSrcColors[2];

        pVert->tu1 = pSrcFloats->tu1;
        pVert->tv1 = pSrcFloats->tv1;

        pVert->tu2 = pSrcFloats->tu2;
        pVert->tv2 = pSrcFloats->tv2;

        pSrcFloats++;
        pSrcColors+=4;
        pVert++;
    }
}

inline UINT align4(UINT x) {
    return (x+3) & ~3;
}

void D3DContext::stretchRect(IDirect3DSurface9* pSrcSurface,
                               int srcX0, int srcY0, int srcX1, int srcY1,
                               IDirect3DSurface9* pDstSurface,
                               int dstX0, int dstY0, int dstX1, int dstY1)
{
    RETURN_IF_NULL(pd3dDevice);
    
    HRESULT res;
    IDirect3DSurface9* pDst = (pDstSurface == NULL) ? currentSurface : pDstSurface;
    RECT srcRect = {srcX0, srcY0, srcX1, srcY1};
    RECT dstRect = {dstX0, dstY0, dstX1, dstY1};
    res = pd3dDevice->StretchRect(pSrcSurface, &srcRect, pDst, &dstRect, D3DTEXF_NONE);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "D3DContext::stretchRect: error StretchRect");
    }
}

/*
 * Note: this method assumes that pSrcFloats and pSrcColors are not null and
 * numVerts is a positive number
 */
HRESULT D3DContext::drawIndexedQuads(PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, int numVerts) {

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
    
    // pVertexBufferRes and pVertexBuffer is never null
    // it is checked in D3DContext::InitDevice
    IDirect3DVertexBuffer9 *pVertexBuffer = pVertexBufferRes->GetVertexBuffer();

    HRESULT res = BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    UINT firstIndex = align4(pVertexBufferRes->GetFirstIndex());

    int numQuads = numVerts / 4;

    do {
        UINT quadsInBatch = min(MAX_BATCH_QUADS, numQuads);
        int vertsInBatch = quadsInBatch * 4;

        if ((firstIndex + vertsInBatch) > MAX_VERTICES) {
            firstIndex = 0;
        }

        DWORD dwLockFlags = firstIndex ? D3DLOCK_NOOVERWRITE : D3DLOCK_DISCARD;

        UINT lockIndex = firstIndex   * sizeof(PRISM_VERTEX_2D);
        UINT lockSize  = vertsInBatch * sizeof(PRISM_VERTEX_2D);

        PRISM_VERTEX_2D *pVert = 0;
        res = pVertexBuffer->Lock(lockIndex, lockSize, (void **)&pVert, dwLockFlags);
        if (SUCCEEDED(res)) {

            fillVB(pVert, pSrcFloats, pSrcColors, vertsInBatch);
            pSrcFloats += vertsInBatch;
            pSrcColors += vertsInBatch * 4;

            res = pVertexBuffer->Unlock();

#if defined PERF_COUNTERS
            D3DContext::FrameStats &stats = getStats();
            stats.numBufferLocks++;
            stats.numDrawCalls++;
            stats.numTrianglesDrawn += quadsInBatch * 2;
#endif

            res = pd3dDevice->DrawIndexedPrimitive(D3DPT_TRIANGLELIST, 0,
                firstIndex, numQuads * 4,
                (firstIndex / 4) * 6, quadsInBatch * 2);

            firstIndex += vertsInBatch;
            numQuads -= quadsInBatch;
        }
    } while (numQuads > 0 && SUCCEEDED(res));

    pVertexBufferRes->SetLastIndex(firstIndex);

    return res;
}

/*
 * Note: this method assumes that pSrcFloats and pSrcColors are not null and
 * numTriangles is a positive number
 */
HRESULT D3DContext::drawTriangleList(struct PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, int numTriangles) {

     RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
     
    // pVertexBufferRes and pVertexBuffer is never null
    // it is checked in D3DContext::InitDevice
    IDirect3DVertexBuffer9 *pVertexBuffer = pVertexBufferRes->GetVertexBuffer();

    HRESULT res = BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    UINT firstIndex = pVertexBufferRes->GetFirstIndex();

    const int maxTrisInbuffer = MAX_VERTICES / 3;

    do {
        int trisInBatch = min(maxTrisInbuffer, numTriangles);
        int vertsInBatch = trisInBatch * 3;

        if ((firstIndex + vertsInBatch) > MAX_VERTICES) {
            firstIndex = 0;
        }

        DWORD dwLockFlags = firstIndex ? D3DLOCK_NOOVERWRITE : D3DLOCK_DISCARD;

        UINT lockIndex = firstIndex   * sizeof(PRISM_VERTEX_2D);
        UINT lockSize  = vertsInBatch * sizeof(PRISM_VERTEX_2D);

        PRISM_VERTEX_2D *pVert = 0;
        res = pVertexBuffer->Lock(lockIndex, lockSize, (void **)&pVert, dwLockFlags);
        if (SUCCEEDED(res)) {
            fillVB(pVert, pSrcFloats, pSrcColors, vertsInBatch);
            pSrcFloats += vertsInBatch;
            pSrcColors += vertsInBatch*4;

            res = pVertexBuffer->Unlock();

#if defined PERF_COUNTERS
            D3DContext::FrameStats &stats = getStats();
            stats.numBufferLocks++;
            stats.numDrawCalls++;
            stats.numTrianglesDrawn += trisInBatch;
#endif

            res = pd3dDevice->DrawPrimitive(D3DPT_TRIANGLELIST, firstIndex, trisInBatch);

            firstIndex += vertsInBatch;
            numTriangles -= trisInBatch;
        }
    } while (numTriangles > 0 && SUCCEEDED(res));

    pVertexBufferRes->SetLastIndex(firstIndex);

    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DGraphics
 * Method:    nClear
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DGraphics_nClear
    (JNIEnv *, jclass, jlong ctx, jint colorArgbPre,
     jboolean clearDepth, jboolean ignoreScissor)
{
    TraceLn(NWT_TRACE_INFO, "D3DGraphics_nClear");
    
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    HRESULT res = pCtx->BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    return pCtx->Clear(colorArgbPre, clearDepth, ignoreScissor);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetBlendEnabled
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetBlendEnabled
  (JNIEnv *, jclass, jlong ctx, jint d3dmode)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res;
    D3DBLEND srcBlend, dstBlend;
    BOOL enable = TRUE;
    
    switch (d3dmode) {
        case com_sun_prism_d3d_D3DContext_D3DCOMPMODE_CLEAR:
            srcBlend = D3DBLEND_ZERO;
            dstBlend = D3DBLEND_ZERO;
            break;
        case com_sun_prism_d3d_D3DContext_D3DCOMPMODE_SRC:
            enable = FALSE;
            break;
        case com_sun_prism_d3d_D3DContext_D3DCOMPMODE_SRCOVER:
            srcBlend = D3DBLEND_ONE;
            dstBlend = D3DBLEND_INVSRCALPHA;
            break;
        case com_sun_prism_d3d_D3DContext_D3DCOMPMODE_DSTOUT:
            srcBlend = D3DBLEND_ZERO;
            dstBlend = D3DBLEND_INVSRCALPHA;
            break;
        case com_sun_prism_d3d_D3DContext_D3DCOMPMODE_ADD:
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

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetRenderTarget
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetRenderTarget
  (JNIEnv *, jclass, jlong ctx, jlong targetRes, jboolean depthBuffer, jboolean msaa)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pRes = (D3DResource *)jlong_to_ptr(targetRes);
    RETURN_STATUS_IF_NULL(pRes, E_FAIL);

    IDirect3DSurface9 *pRenderTarget = pRes->GetSurface();
    RETURN_STATUS_IF_NULL(pRenderTarget, E_FAIL);

    IDirect3DSurface9 *pDepthBuffer = pRes->GetDepthSurface();

    HRESULT res = pCtx->SetRenderTarget(pRenderTarget, &pDepthBuffer, depthBuffer, msaa);
    pRes->SetDepthSurface(pDepthBuffer);
    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetTexture
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetTexture
  (JNIEnv *, jclass, jlong ctx, jlong textureRes, jint texUnit,
   jboolean linear, jint wrapMode)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    D3DResource *pRes = (D3DResource *)jlong_to_ptr(textureRes);

#if defined PERF_COUNTERS
    pCtx->getStats().numSetTexture++;
#endif

    HRESULT res = pCtx->BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    IDirect3DTexture9 *pTex = pRes == NULL ? NULL : pRes->GetTexture();
    res = pd3dDevice->SetTexture(texUnit, pTex);
    RETURN_STATUS_IF_FAILED(res);

    if (pTex != NULL) {
        D3DTEXTUREFILTERTYPE fhint = linear ? D3DTEXF_LINEAR : D3DTEXF_POINT;
        pd3dDevice->SetSamplerState(texUnit, D3DSAMP_MAGFILTER, fhint);
        pd3dDevice->SetSamplerState(texUnit, D3DSAMP_MINFILTER, fhint);
        pd3dDevice->SetSamplerState(texUnit, D3DSAMP_MIPFILTER, fhint);
        if (wrapMode != 0) {
            pd3dDevice->SetSamplerState(texUnit, D3DSAMP_ADDRESSU, wrapMode);
            pd3dDevice->SetSamplerState(texUnit, D3DSAMP_ADDRESSV, wrapMode);
        }
    }

    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetProjViewMatrix
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetCameraPosition
    (JNIEnv *, jclass, jlong ctx,
    jdouble camPosX, jdouble camPosY, jdouble camPosZ)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    return pCtx->SetCameraPosition(camPosX, camPosY, camPosZ);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetProjViewMatrix
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetProjViewMatrix
  (JNIEnv *, jclass, jlong ctx, jboolean isOrtho,
   jdouble m00, jdouble m01, jdouble m02, jdouble m03,
   jdouble m10, jdouble m11, jdouble m12, jdouble m13,
   jdouble m20, jdouble m21, jdouble m22, jdouble m23,
   jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    return pCtx->SetProjViewMatrix(isOrtho,
        m00, m01, m02, m03,
        m10, m11, m12, m13,
        m20, m21, m22, m23,
        m30, m31, m32, m33);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetTransform
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetTransform
  (JNIEnv *, jclass, jlong ctx,
   jdouble m00, jdouble m01, jdouble m02, jdouble m03,
   jdouble m10, jdouble m11, jdouble m12, jdouble m13,
   jdouble m20, jdouble m21, jdouble m22, jdouble m23,
   jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    return pCtx->SetTransform(m00, m01, m02, m03,
                              m10, m11, m12, m13,
                              m20, m21, m22, m23,
                              m30, m31, m32, m33);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nResetTransform
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nResetTransform
  (JNIEnv *, jclass, jlong ctx)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    
    return pCtx->ResetTransform();
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetWorldTransformToIdentity
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetWorldTransformToIdentity
  (JNIEnv *, jclass, jlong ctx)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_IF_NULL(pCtx);
    
    pCtx->setWorldTransformIndentity();
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetWorldTransform
 * Signature: (JDDDDDDDDDDDDDDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetWorldTransform
  (JNIEnv *, jclass, jlong ctx,
   jdouble m00, jdouble m01, jdouble m02, jdouble m03,
   jdouble m10, jdouble m11, jdouble m12, jdouble m13,
   jdouble m20, jdouble m21, jdouble m22, jdouble m23,
   jdouble m30, jdouble m31, jdouble m32, jdouble m33) {
    D3DContext *pCtx = (D3DContext*) jlong_to_ptr(ctx);
    RETURN_IF_NULL(pCtx);

    pCtx->setWorldTransform(m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetClipRect
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetClipRect
  (JNIEnv *, jclass, jlong ctx,
   jint x1, jint y1, jint x2, jint y2)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->SetRectClip(x1, y1, x2, y2);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nResetClipRect
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nResetClipRect
  (JNIEnv *, jclass, jlong ctx)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    return pCtx->ResetClip();
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nIsRTTVolatile
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d_D3DContext_nIsRTTVolatile
  (JNIEnv *, jclass, jlong ctx)
{
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, false);
    
    return pCtx->Get3DExDevice() ? false : true;
}
