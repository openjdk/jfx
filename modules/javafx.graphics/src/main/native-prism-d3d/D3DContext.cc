/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <iostream>

#include "D3DPipeline.h"
#include "D3DContext.h"
#include "D3DPipelineManager.h"
#include "PassThroughVS.h"

#include "com_sun_prism_d3d_D3DContext.h"
#include "D3DLight.h"
#include "D3DMesh.h"
#include "D3DMeshView.h"
#include "D3DPhongMaterial.h"
using std::cout;
using std::endl;
/**
 * Note: this method assumes that r is different from a, b!
 */
inline void D3DUtils_MatrixMultTransposed(D3DMATRIX& r, const D3DMATRIX& a, const D3DMATRIX& b) {
    for (int i=0; i<4; i++) {
        for (int j=0; j<4; j++) {
            float t = 0;
            for (int k=0; k<4; k++) {
                // transpose on the fly
                t += a.m[i][k] * b.m[k][j];
            }
            r.m[j][i] = t;
        }
    }
}

inline void D3DUtils_MatrixTransposed(D3DMATRIX& r, const D3DMATRIX& a) {
    for (int i=0; i<4; i++) {
        for (int j=0; j<4; j++) {
            r.m[j][i] = a.m[i][j];
        }
    }
}

inline void D3DUtils_SetIdentityMatrix(D3DMATRIX *m) {
    m->_12 = m->_13 = m->_14 = m->_21 = m->_23 = m->_24 = 0.0f;
    m->_31 = m->_32 = m->_34 = m->_41 = m->_42 = m->_43 = 0.0f;
    m->_11 = m->_22 = m->_33 = m->_44 = 1.0f;
}

// static
HRESULT
D3DContext::CreateInstance(IDirect3D9 *pd3d9, IDirect3D9Ex *pd3d9Ex, UINT adapter, bool isVsyncEnabled, D3DContext **ppCtx)
{
    HRESULT res;
    *ppCtx = new D3DContext(pd3d9, pd3d9Ex, adapter);
    if (FAILED(res = (*ppCtx)->InitContext(isVsyncEnabled))) {
        delete *ppCtx;
        *ppCtx = NULL;
    }
    return res;
}

D3DContext::D3DContext(IDirect3D9 *pd3d, IDirect3D9Ex *pd3dEx, UINT adapter)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::D3DContext");
    TraceLn1(NWT_TRACE_VERBOSE, "  pd3d=0x%x", pd3d);
    pd3dObject = pd3d;
    pd3dObjectEx = pd3dEx;
    pd3dDevice = NULL;
    pd3dDeviceEx = NULL;
    adapterOrdinal = adapter;
    defaulResourcePool = D3DPOOL_SYSTEMMEM;

    pResourceMgr = NULL;

    pPassThroughVS = NULL;
    pVertexDecl = NULL;
    pIndices = NULL;
    pVertexBufferRes = NULL;

    bBeginScenePending = FALSE;
    phongShader = NULL;

    ZeroMemory(&devCaps, sizeof(D3DCAPS9));
    ZeroMemory(&curParams, sizeof(curParams));
    ZeroMemory(textureCache, sizeof(textureCache));
}

/**
 * This method releases context resources either from the default pool only
 * (basically from vram) or all of them, depending on the passed argument.
 *
 * Note that some resources are still not under ResourceManager control so we
 * have to handle them separately. Ideally we'd move every allocated resource
 * under RM control.
 *
 * The reason we have single method instead of a pair of methods (one for
 * default only and one for everything) is to reduce code duplication. It is
 * possible to call ReleaseDefPoolResources from ReleaseContextResources but
 * then we'd traverse the resources list twice (may not be a big deal).
 */
void D3DContext::ReleaseContextResources(int releaseType)
{
    TraceLn2(NWT_TRACE_INFO,
             "D3DContext::ReleaseContextResources: %d pd3dDevice = 0x%x",
             releaseType, pd3dDevice);

    if (releaseType != RELEASE_ALL && releaseType != RELEASE_DEFAULT) {
        TraceLn1(NWT_TRACE_ERROR,
                "D3DContext::ReleaseContextResources unknown type: %d",
                releaseType);
        return;
    }

    EndScene();

    if (releaseType == RELEASE_DEFAULT) {
        if (pVertexBufferRes != NULL && pVertexBufferRes->IsDefaultPool()) {
            // if VB is in the default pool it will be released by the RM
            pVertexBufferRes = NULL;
        }
        pResourceMgr->ReleaseDefPoolResources();
    } else if (releaseType == RELEASE_ALL){
        // will be released with the resource manager
        pVertexBufferRes = NULL;
        SAFE_RELEASE(pVertexDecl);
        SAFE_RELEASE(pIndices);
        SAFE_RELEASE(pPassThroughVS);
        SAFE_DELETE(pResourceMgr);
    }
}

D3DContext::~D3DContext() {}

int D3DContext::release() {

    TraceLn2(NWT_TRACE_INFO,
                "~D3DContext: pd3dDevice=0x%x, pd3dObject =0x%x",
                pd3dDevice, pd3dObject);
    ReleaseContextResources(RELEASE_ALL);
    for (int i = 0; i < NUM_TEXTURE_CACHE; i++) {
        SAFE_RELEASE(textureCache[i].surface);
        SAFE_RELEASE(textureCache[i].texture);
    }
    SAFE_RELEASE(pd3dDevice);
    SAFE_RELEASE(pd3dDeviceEx);

    if (phongShader) {
        delete phongShader;
        phongShader = NULL;
    }

    delete this;
    return 0;
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nCreateD3DMesh
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DContext_nCreateD3DMesh
  (JNIEnv *env, jclass, jlong ctx)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nCreateD3DMesh");
    D3DContext *pCtx = (D3DContext*) jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, 0L);

    D3DMesh *mesh = new D3DMesh(pCtx);
    return ptr_to_jlong(mesh);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nReleaseD3DMesh
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nReleaseD3DMesh
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMesh)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nReleaseD3DMesh");
    D3DMesh *mesh = (D3DMesh *) jlong_to_ptr(nativeMesh);
    if (mesh) {
        delete mesh;
    }
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nBuildNativeGeometryShort
 * Signature: (JJ[FI[SI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d_D3DContext_nBuildNativeGeometryShort
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMesh, jfloatArray vb, jint vbSize, jshortArray ib, jint ibSize)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nBuildNativeGeometryShort");
    D3DMesh *mesh = (D3DMesh *) jlong_to_ptr(nativeMesh);
    RETURN_STATUS_IF_NULL(mesh, JNI_FALSE);

    if (vbSize < 0 || ibSize < 0) {
        return JNI_FALSE;
    }

    UINT uvbSize = (UINT) vbSize;
    UINT uibSize = (UINT) ibSize;
    UINT vertexBufferSize = env->GetArrayLength(vb);
    UINT indexBufferSize = env->GetArrayLength(ib);

    if (uvbSize > vertexBufferSize || uibSize > indexBufferSize) {
        return JNI_FALSE;
    }

    float *vertexBuffer = (float *) (env->GetPrimitiveArrayCritical(vb, NULL));
    if (vertexBuffer == NULL) {
        return JNI_FALSE;
    }

    USHORT *indexBuffer = (USHORT *) (env->GetPrimitiveArrayCritical(ib, NULL));
    if (indexBuffer == NULL) {
        env->ReleasePrimitiveArrayCritical(vb, vertexBuffer, 0);
        return JNI_FALSE;
    }

    boolean result = mesh->buildBuffers(vertexBuffer, uvbSize, indexBuffer, uibSize);
    env->ReleasePrimitiveArrayCritical(ib, indexBuffer, 0);
    env->ReleasePrimitiveArrayCritical(vb, vertexBuffer, 0);

    return result;
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nBuildNativeGeometryInt
 * Signature: (JJ[FI[II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d_D3DContext_nBuildNativeGeometryInt
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMesh, jfloatArray vb, jint vbSize, jintArray ib, jint ibSize)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nBuildNativeGeometryInt");
    D3DMesh *mesh = (D3DMesh *) jlong_to_ptr(nativeMesh);
    RETURN_STATUS_IF_NULL(mesh, JNI_FALSE);

    if (vbSize < 0 || ibSize < 0) {
        return JNI_FALSE;
    }

    UINT uvbSize = (UINT) vbSize;
    UINT uibSize = (UINT) ibSize;
    UINT vertexBufferSize = env->GetArrayLength(vb);
    UINT indexBufferSize = env->GetArrayLength(ib);
    if (uvbSize > vertexBufferSize || uibSize > indexBufferSize) {
        return JNI_FALSE;
    }

    float *vertexBuffer = (float *) (env->GetPrimitiveArrayCritical(vb, NULL));
    if (vertexBuffer == NULL) {
        return JNI_FALSE;
    }

    UINT *indexBuffer = (UINT *) (env->GetPrimitiveArrayCritical(ib, NULL));
    if (indexBuffer == NULL) {
        env->ReleasePrimitiveArrayCritical(vb, vertexBuffer, 0);
        return JNI_FALSE;
    }

    boolean result = mesh->buildBuffers(vertexBuffer, uvbSize, indexBuffer, uibSize);
    env->ReleasePrimitiveArrayCritical(ib, indexBuffer, 0);
    env->ReleasePrimitiveArrayCritical(vb, vertexBuffer, 0);

    return result;
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nCreateD3DPhongMaterial
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DContext_nCreateD3DPhongMaterial
  (JNIEnv *env, jclass, jlong ctx)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nCreateD3DPhongMaterial");
    D3DContext *pCtx = (D3DContext*) jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, 0L);

    D3DPhongMaterial *phongMaterial = new D3DPhongMaterial(pCtx);
    return ptr_to_jlong(phongMaterial);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nReleaseD3DPhongMaterial
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nReleaseD3DPhongMaterial
  (JNIEnv *env, jclass, jlong ctx, jlong nativePhongMaterial)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nReleaseD3DPhongMaterial");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    if (phongMaterial) {
        delete phongMaterial;
    }
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetDiffuseColor
 * Signature: (JJFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetDiffuseColor
  (JNIEnv *env, jclass, jlong ctx, jlong nativePhongMaterial,
        jfloat r, jfloat g, jfloat b, jfloat a)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetDiffuseColor");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    RETURN_IF_NULL(phongMaterial);

    phongMaterial->setDiffuseColor(r, g, b, a);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetSpecularColor
 * Signature: (JJZFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetSpecularColor
  (JNIEnv *env, jclass, jlong ctx, jlong nativePhongMaterial,
        jboolean set, jfloat r, jfloat g, jfloat b, jfloat a)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetSpecularColor");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    RETURN_IF_NULL(phongMaterial);

    phongMaterial->setSpecularColor(set ? true : false, r, g, b, a);
}
/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetMap
 * Signature: (JJIJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetMap
  (JNIEnv *env, jclass, jlong ctx, jlong nativePhongMaterial,
        jint mapType, jlong nativeTexture)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetMap");
    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    IDirect3DBaseTexture9 *texMap = (IDirect3DBaseTexture9 *)  jlong_to_ptr(nativeTexture);
    RETURN_IF_NULL(phongMaterial);

    phongMaterial->setMap(mapType, texMap);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nCreateD3DMeshView
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DContext_nCreateD3DMeshView
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMesh)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nCreateD3DMeshView");
    D3DContext *pCtx = (D3DContext*) jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, 0L);

    D3DMesh *mesh = (D3DMesh *) jlong_to_ptr(nativeMesh);
    RETURN_STATUS_IF_NULL(mesh, 0L);

    D3DMeshView *meshView = new D3DMeshView(pCtx, mesh);
    return ptr_to_jlong(meshView);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nReleaseD3DMeshView
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nReleaseD3DMeshView
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nReleaseD3DMeshView");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    if (meshView) {
        delete meshView;
    }
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetCullingMode
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetCullingMode
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView, jint cullMode)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetCullingMode");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    RETURN_IF_NULL(meshView);

    switch (cullMode) {
        case com_sun_prism_d3d_D3DContext_CULL_BACK:
            cullMode = D3DCULL_CW;
            break;
        case com_sun_prism_d3d_D3DContext_CULL_FRONT:
            cullMode = D3DCULL_CCW;
            break;
        case com_sun_prism_d3d_D3DContext_CULL_NONE:
            cullMode = D3DCULL_NONE;
            break;
    }
    meshView->setCullingMode(cullMode);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nBlit
 * Signature: (JJJIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nBlit
  (JNIEnv *env, jclass, jlong ctx, jlong nSrcRTT, jlong nDstRTT,
            jint srcX0, jint srcY0, jint srcX1, jint srcY1,
            jint dstX0, jint dstY0, jint dstX1, jint dstY1)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nBlit");
    D3DContext *pCtx = (D3DContext*) jlong_to_ptr(ctx);
    RETURN_IF_NULL(pCtx);

    D3DResource *srcRes = (D3DResource*) jlong_to_ptr(nSrcRTT);
    if (srcRes == NULL) {
        TraceLn(NWT_TRACE_INFO, "   error srcRes is NULL");
        return;
    }

    IDirect3DSurface9 *pSrcSurface = srcRes->GetSurface();
    if (pSrcSurface == NULL) {
        TraceLn(NWT_TRACE_INFO, "   error pSrcSurface is NULL");
        return;
    }

    D3DResource *dstRes = (D3DResource*) jlong_to_ptr(nDstRTT);
    IDirect3DSurface9 *pDstSurface = (dstRes == NULL) ? NULL : dstRes->GetSurface();

    pCtx->stretchRect(pSrcSurface, srcX0, srcY0, srcX1, srcY1,
                      pDstSurface, dstX0, dstY0, dstX1, dstY1);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetMaterial
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetMaterial
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView, jlong nativePhongMaterial)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetMaterial");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    RETURN_IF_NULL(meshView);

    D3DPhongMaterial *phongMaterial = (D3DPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    meshView->setMaterial(phongMaterial);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetWireframe
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetWireframe
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView, jboolean wireframe)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetWireframe");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    RETURN_IF_NULL(meshView);

    meshView->setWireframe(wireframe ? true : false);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetAmbientLight
 * Signature: (JJFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetAmbientLight
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView,
        jfloat r, jfloat g, jfloat b)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetAmbientLight");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    RETURN_IF_NULL(meshView);

    meshView->setAmbientLight(r, g, b);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetLight
 * Signature: (JJIFFFFFFFFFFFFFFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nSetLight
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView, jint index,
        jfloat x, jfloat y, jfloat z, jfloat r, jfloat g, jfloat b, jfloat w,
        jfloat ca, jfloat la, jfloat qa, jfloat range,
        jfloat dirX, jfloat dirY, jfloat dirZ, jfloat innerAngle, jfloat outerAngle, jfloat falloff)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetLight");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    RETURN_IF_NULL(meshView);
    meshView->setLight(index, x, y, z, r, g, b, w, ca, la, qa, range, dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nRenderMeshView
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DContext_nRenderMeshView
  (JNIEnv *env, jclass, jlong ctx, jlong nativeMeshView)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nRenderMeshView");
    D3DMeshView *meshView = (D3DMeshView *) jlong_to_ptr(nativeMeshView);
    RETURN_IF_NULL(meshView);

    meshView->render();
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetDeviceParametersFor2D
 */

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetDeviceParametersFor2D
  (JNIEnv *, jclass, jlong ctx)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSetDeviceParametersFor2D");
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, S_FALSE);

    return pCtx->setDeviceParametersFor2D();
}

HRESULT D3DContext::setDeviceParametersFor2D() {

    RETURN_STATUS_IF_NULL(pd3dDevice, S_FALSE);

    HRESULT res = S_OK;

    IDirect3DVertexBuffer9 *vb = pVertexBufferRes->GetVertexBuffer();

    SUCCEEDED(res = pd3dDevice->SetVertexDeclaration(pVertexDecl)) &&
    SUCCEEDED(res = pd3dDevice->SetIndices(pIndices)) &&
    SUCCEEDED(res = pd3dDevice->SetVertexShader(pPassThroughVS)) &&
    SUCCEEDED(res = pd3dDevice->SetStreamSource(0, vb, 0, sizeof (PRISM_VERTEX_2D)));

    if (res == S_OK) {
        // Note: No need to restore blend and scissor states as the 2D states were
        //       invalidated on the Java side.
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_CULLMODE, D3DCULL_NONE)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_FILLMODE, D3DFILL_SOLID)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_LIGHTING, FALSE));
    }
    return res;
}

/*
 * Class:     com_sun_prism_d3d_D3DContext
 * Method:    nSetDeviceParametersFor3D
 */

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DContext_nSetDeviceParametersFor3D
  (JNIEnv *, jclass, jlong ctx)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext_nSet3DVShaderAndVertexBuffer");
    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, S_FALSE);

    return pCtx->setDeviceParametersFor3D();
}

HRESULT D3DContext::setDeviceParametersFor3D() {

    RETURN_STATUS_IF_NULL(pd3dDevice, S_FALSE);

    HRESULT res = S_OK;

    if (!phongShader) {
        phongShader = new D3DPhongShader(pd3dDevice);
    }

    // Reset 3D states
    state.wireframe = false;
    state.cullMode = D3DCULL_NONE;
    if (res == S_OK) {
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_CULLMODE, D3DCULL_NONE)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_FILLMODE, D3DFILL_SOLID)) &&
        // This setting matches 2D ((1,1-alpha); premultiplied alpha case.
        // Will need to evaluate when support proper 3D blending (alpha,1-alpha).
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_SRCBLEND, D3DBLEND_ONE)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_DESTBLEND, D3DBLEND_INVSRCALPHA)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, TRUE)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE)) &&
        SUCCEEDED(res = pd3dDevice->SetRenderState(D3DRS_LIGHTING, TRUE)) &&
        // TODO: 3D - RT-34415: [D3D 3D] Need a robust 3D states management for texture
        // Set texture unit 0 to its default texture addressing mode for Prism
        SUCCEEDED(res = pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSU, D3DTADDRESS_WRAP)) &&
        SUCCEEDED(res = pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSV, D3DTADDRESS_WRAP)) &&
        // Set texture filter to bilinear for 3D rendering
        SUCCEEDED(res = pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, D3DTEXF_LINEAR)) &&
        SUCCEEDED(res = pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, D3DTEXF_LINEAR));
    }
    return res;
}

/*
 * Note: this method assumes that pIndices is not null
 */
static HRESULT fillQuadIndices(IDirect3DIndexBuffer9 *pIndices, int maxQuads) {
    short * data = 0;
    HRESULT hr = pIndices->Lock(0, maxQuads * 6 * sizeof(short), (void **)&data, 0);
    if (SUCCEEDED(hr) && data) {
        for (int i = 0; i != maxQuads; ++i) {
            int vtx = i * 4;
            int idx = i * 6;
            data[idx + 0] = vtx + 0;
            data[idx + 1] = vtx + 1;
            data[idx + 2] = vtx + 2;
            data[idx + 3] = vtx + 2;
            data[idx + 4] = vtx + 1;
            data[idx + 5] = vtx + 3;
        }
        hr = pIndices->Unlock();
    }
    return hr;
}

HRESULT D3DContext::InitDevice(IDirect3DDevice9 *pd3dDevice)
{
#if defined PERF_COUNTERS
    stats.clear();
#endif

    RETURN_STATUS_IF_NULL(pd3dDevice, S_FALSE);

    HRESULT res = S_OK;

    pd3dDevice->GetDeviceCaps(&devCaps);

    RlsTraceLn1(NWT_TRACE_INFO,
                   "D3DContext::InitDevice: device %d", adapterOrdinal);

    // disable some of the unneeded and costly d3d functionality
    pd3dDevice->SetRenderState(D3DRS_SPECULARENABLE, FALSE);
    pd3dDevice->SetRenderState(D3DRS_LIGHTING,  FALSE);
    pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_FALSE);
    pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_FALSE);
    pd3dDevice->SetRenderState(D3DRS_COLORVERTEX, FALSE);
    pd3dDevice->SetRenderState(D3DRS_STENCILENABLE, FALSE);

    // set clipping to true inorder support near and far clipping
    pd3dDevice->SetRenderState(D3DRS_CLIPPING,  TRUE);

    pd3dDevice->SetRenderState(D3DRS_CULLMODE, D3DCULL_NONE);
    pd3dDevice->SetRenderState(D3DRS_FILLMODE, D3DFILL_SOLID);
    state.wireframe = false;
    state.cullMode = D3DCULL_NONE;

    if (pResourceMgr == NULL) {
        pResourceMgr = D3DResourceManager::CreateInstance(this);
    }

    D3DUtils_SetIdentityMatrix(&world);
    D3DUtils_SetIdentityMatrix(&projection);
    depthTest = FALSE;
    pixadjustx = pixadjusty = 0.0f;

    if (pVertexDecl == NULL) {
        res = pd3dDevice->CreateVertexDeclaration(PrismVDecl, &pVertexDecl);
        RETURN_STATUS_IF_FAILED(res);
    }
//    res = pd3dDevice->SetVertexDeclaration(pVertexDecl);
//    RETURN_STATUS_IF_FAILED(res);

    if (pIndices == NULL) {
        res = pd3dDevice->CreateIndexBuffer(sizeof(short) * 6 * MAX_BATCH_QUADS,
            D3DUSAGE_WRITEONLY, D3DFMT_INDEX16, getResourcePool(), &pIndices, 0);
        if (pIndices) {
            res = fillQuadIndices(pIndices, MAX_BATCH_QUADS);
        }
        RETURN_STATUS_IF_FAILED(res);
    }
//    res = pd3dDevice->SetIndices(pIndices);
//    RETURN_STATUS_IF_FAILED(res);

    if (pPassThroughVS == NULL) {
        res = pd3dDevice->CreateVertexShader((DWORD*)g_vs30_passThrough, &pPassThroughVS);
        RETURN_STATUS_IF_FAILED(res);
    }
//    res = pd3dDevice->SetVertexShader(pPassThroughVS);
//    RETURN_STATUS_IF_FAILED(res);

    if (pVertexBufferRes == NULL) {
        res = GetResourceManager()->CreateVertexBuffer(&pVertexBufferRes);
        RETURN_STATUS_IF_FAILED(res);
    }
//    res = pd3dDevice->SetStreamSource(0, pVertexBufferRes->GetVertexBuffer(),
//                                      0, sizeof(PRISM_VERTEX_2D));
//    RETURN_STATUS_IF_FAILED(res);

    bBeginScenePending = FALSE;

    RlsTraceLn1(NWT_TRACE_INFO,
                   "D3DContext::InitDevice: successfully initialized device %d",
                   adapterOrdinal);

    return res;
}

HRESULT
D3DContext::TestCooperativeLevel()
{
    TraceLn2(NWT_TRACE_INFO,
             "D3DContext::testCooperativeLevel pd3dDevice = 0x%x, pd3dDeviceEx = 0x%x",
             pd3dDevice, pd3dDeviceEx);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    //TODO: call CheckDeviceState only if Present fails
    HRESULT res = pd3dDeviceEx ?
        pd3dDeviceEx->CheckDeviceState(NULL) :
        pd3dDevice->TestCooperativeLevel();

    switch (res) {
    case S_OK: break;
    case D3DERR_DEVICELOST:
        TraceLn1(NWT_TRACE_INFO, "  device %d is still lost", adapterOrdinal);
        break;
    case D3DERR_DEVICENOTRESET:
        TraceLn1(NWT_TRACE_INFO, "  device %d needs to be reset", adapterOrdinal);
        break;
    case D3DERR_DEVICEREMOVED:
        TraceLn1(NWT_TRACE_INFO, "  device %d has been removed", adapterOrdinal);
        break;
    case S_PRESENT_OCCLUDED:
        break;
    case S_PRESENT_MODE_CHANGED:
        break;
    case E_FAIL:
        TraceLn(NWT_TRACE_VERBOSE, "  null device");
        break;
    default:
        TraceLn1(NWT_TRACE_ERROR, "D3DContext::testCooperativeLevel: "\
            "unknown error %x from TestCooperativeLevel", res);
    }

    return res;
}

HRESULT
D3DContext::Clear(DWORD colorArgbPre, BOOL clearDepth, BOOL ignoreScissor)
{
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res;
    DWORD bSE = FALSE;
    DWORD bDE = FALSE;
    DWORD flags = D3DCLEAR_TARGET;

    if (ignoreScissor) {
        // scissor test affects Clear so it needs to be disabled first
        pd3dDevice->GetRenderState(D3DRS_SCISSORTESTENABLE, &bSE);
        if (bSE) {
            pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE);
        }
    }
    if (clearDepth) {
        // Must ensure that there is a depth buffer before attempting to clear it
        IDirect3DSurface9 *pCurrentDepth = NULL;
        pd3dDevice->GetDepthStencilSurface(&pCurrentDepth);
        clearDepth = pCurrentDepth == NULL ? FALSE : clearDepth;
        SAFE_RELEASE(pCurrentDepth);
    }
    if (clearDepth) {
        flags |= D3DCLEAR_ZBUFFER;
        // also make sure depth writes are enabled for the clear operation
        pd3dDevice->GetRenderState(D3DRS_ZWRITEENABLE, &bDE);
        if (!bDE) {
            pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_TRUE);
        }
    }

    res = pd3dDevice->Clear(0, NULL, flags, colorArgbPre, 1.0f, 0x0L);

    // restore previous state
    if (ignoreScissor && bSE) {
        pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, TRUE);
    }
    if (clearDepth && !bDE) {
        pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_FALSE);
    }
    return res;
}

BOOL D3DContext::IsDepthStencilBufferOk(D3DSURFACE_DESC *pTargetDesc, IDirect3DSurface9 *pTargetDepth)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::IsDepthStencilBufferOk");

    RETURN_STATUS_IF_NULL(pTargetDepth, true);
    RETURN_STATUS_IF_NULL(pd3dDevice, false);
    RETURN_STATUS_IF_NULL(pd3dObject, false);

    D3DSURFACE_DESC descStencil;
    pTargetDepth->GetDesc(&descStencil);

    D3DDISPLAYMODE dm;
    return
        (SUCCEEDED(pd3dDevice->GetDisplayMode(0, &dm)) &&
         pTargetDesc->Width <= descStencil.Width &&
         pTargetDesc->Height <= descStencil.Height &&
         pTargetDesc->MultiSampleType == descStencil.MultiSampleType &&
         pTargetDesc->MultiSampleQuality == descStencil.MultiSampleQuality &&
         SUCCEEDED(pd3dObject->CheckDepthStencilMatch(
               adapterOrdinal,
               devCaps.DeviceType,
               dm.Format, pTargetDesc->Format,
               descStencil.Format)));
}

HRESULT
D3DContext::InitDepthStencilBuffer(D3DSURFACE_DESC *pTargetDesc, IDirect3DSurface9 **ppDepthSSurface)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::InitDepthStencilBuffer");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
    RETURN_STATUS_IF_NULL(pTargetDesc, E_FAIL);

    HRESULT res;
    D3DDISPLAYMODE dm;
    if (FAILED(res = pd3dDevice->GetDisplayMode(0, &dm))) {
        return res;
    }

    D3DFORMAT newFormat =
        D3DPipelineManager::GetInstance()->GetMatchingDepthStencilFormat(
            adapterOrdinal, dm.Format, pTargetDesc->Format);

    res = pd3dDevice->CreateDepthStencilSurface(
        pTargetDesc->Width, pTargetDesc->Height, newFormat,
        pTargetDesc->MultiSampleType, pTargetDesc->MultiSampleQuality, false, ppDepthSSurface, 0);

    return res;
}

HRESULT
D3DContext::UpdateVertexShaderTX()
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::UpdateVertexShaderTX");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    D3DMATRIX wvp;
    // create the WorldViewProj matrix
    // wvp = T(w * v * p);
    // since view is currently included in the projection matrix, wvp = T(w * p)
    D3DUtils_MatrixMultTransposed(wvp, world, projection);
    // Apply the pixel adjustment values for the current render target.
    // These values adjust our default (identity) coordinates so that the
    // pixel edges are at integer coordinate locations.
    wvp._14 += pixadjustx;
    wvp._24 += pixadjusty;

//    fprintf(stderr, "UpdateVertexShaderTX:\n");
//    fprintf(stderr, "  %5f %5f %5f %5f\n", wvp._11, wvp._12, wvp._13, wvp._14);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", wvp._21, wvp._22, wvp._23, wvp._24);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", wvp._31, wvp._32, wvp._33, wvp._34);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", wvp._41, wvp._42, wvp._43, wvp._44);

    return pd3dDevice->SetVertexShaderConstantF(0, (float*)wvp.m, 4);
}

HRESULT
D3DContext::SetRenderTarget(IDirect3DSurface9 *pSurface,
        IDirect3DSurface9 **ppTargetDepthSurface,
        BOOL depthBuffer, BOOL msaa)
{
    TraceLn1(NWT_TRACE_INFO,
                "D3DContext::SetRenderTarget: pSurface=0x%x",
                pSurface);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
    RETURN_STATUS_IF_NULL(pSurface, E_FAIL);

    HRESULT res;
    D3DSURFACE_DESC descNew;
    IDirect3DSurface9 *pCurrentTarget;
    bool renderTargetChanged = false;

    pSurface->GetDesc(&descNew);

    if (SUCCEEDED(res = pd3dDevice->GetRenderTarget(0, &pCurrentTarget))) {
        if (pCurrentTarget != pSurface) {
            renderTargetChanged = true;
#if defined PERF_COUNTERS
            getStats().numRenderTargetSwitch++;
#endif

            if (FAILED(res = pd3dDevice->SetRenderTarget(0, pSurface))) {
                DebugPrintD3DError(res, "D3DContext::SetRenderTarget: "\
                                        "error setting render target");
                SAFE_RELEASE(pCurrentTarget);
                return res;
            }

            currentSurface = pSurface;
        }
        SAFE_RELEASE(pCurrentTarget);

        IDirect3DSurface9 *pCurrentDepth;
        res = pd3dDevice->GetDepthStencilSurface(&pCurrentDepth);
        if (res == D3DERR_NOTFOUND) {
            pCurrentDepth = NULL;
            res = D3D_OK;
        } else if (FAILED(res)) {
            return res;
        }

        if (!IsDepthStencilBufferOk(&descNew, *ppTargetDepthSurface)) {
            *ppTargetDepthSurface = NULL;
        }
        bool depthIsNew = false;
        if (depthBuffer && (*ppTargetDepthSurface) == NULL) {
            if (FAILED(res = InitDepthStencilBuffer(&descNew, ppTargetDepthSurface))) {
                DebugPrintD3DError(res, "D3DContext::SetRenderTarget: error creating new depth buffer");
                return res;
            }
            depthIsNew = true;
        }
        if (pCurrentDepth != (*ppTargetDepthSurface)) {
            res = pd3dDevice->SetDepthStencilSurface(*ppTargetDepthSurface);
            if ((*ppTargetDepthSurface) != NULL && depthIsNew) {
                // Depth buffer must be cleared after it is created, also
                // if depth buffer was not attached when render target was
                // cleared, then the depth buffer will contain garbage
                pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_TRUE);
                res = pd3dDevice->Clear(0, NULL, D3DCLEAR_ZBUFFER, NULL , 1.0f, 0x0L);
                if (FAILED(res)) {
                    DebugPrintD3DError(res,
                            "D3DContext::SetRenderTarget: error clearing depth buffer");
                }
            }
        } else if (!renderTargetChanged) {
            SAFE_RELEASE(pCurrentDepth);
            return S_FALSE; // Indicates that call succeeded, but render target was not changed
        }
        SAFE_RELEASE(pCurrentDepth);
        pd3dDevice->SetRenderState(D3DRS_MULTISAMPLEANTIALIAS, msaa);
    }
    // NOTE PRISM: changed to only recalculate the matrix if current target is
    // different for now

    // we set the transform even if the render target didn't change;
    // this is because in some cases (fs mode) we use the default SwapChain of
    // the device, and its render target will be the same as the device's, and
    // we have to set the matrix correctly. This shouldn't be a performance
    // issue as render target changes are relatively rare

    // By default D3D has integer device coordinates at the center of pixels
    // but we want integer device coordinates to be at the edges of pixels.
    // Additionally, its default viewport is set so that coordinates on a
    // surface map onto (-1, +1) -> (+1, -1) as one moves from the upper left
    // corner to the lower right corner.  We need to move the values towards
    // -X and +Y by half a pixel using the following adjustment values:
    // half of (((+1) - (-1)) / dim), or half of (2 / dim) == (1 / dim).
    pixadjustx = -1.0f / descNew.Width;
    pixadjusty = +1.0f / descNew.Height;
    TraceLn1(NWT_TRACE_VERBOSE, "  current render target=0x%x", pSurface);
    TraceLn2(NWT_TRACE_VERBOSE, "      pixel adjustments=%f, %f", pixadjustx, pixadjusty);
    if (SUCCEEDED(res) && !renderTargetChanged)
        return S_FALSE; // Indicates that call succeeded, but render target was not changed
    return res;
}

HRESULT
D3DContext::SetCameraPosition(jdouble camPosX, jdouble camPosY, jdouble camPosZ)
{
    float cPos[4];
    HRESULT res = S_OK;
    TraceLn(NWT_TRACE_INFO, "D3DContext::SetCameraPosition");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    cPos[0] = (float) camPosX;
    cPos[1] = (float) camPosY;
    cPos[2] = (float) camPosZ;
    cPos[3] = 0;

    if (phongShader) {
        //    std::cerr << "Camera Position: " << cPos[0] << ", " << cPos[1] << ", " << cPos[2] << std::endl;
        SUCCEEDED(res = pd3dDevice->SetVertexShaderConstantF(VSR_CAMERAPOS, cPos, 1));
    }
    return res;
}

HRESULT
D3DContext::SetProjViewMatrix(BOOL depthTest,
                              jdouble m00, jdouble m01, jdouble m02, jdouble m03,
                              jdouble m10, jdouble m11, jdouble m12, jdouble m13,
                              jdouble m20, jdouble m21, jdouble m22, jdouble m23,
                              jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    D3DMATRIX mat;
    HRESULT res = S_OK;

    TraceLn(NWT_TRACE_INFO, "D3DContext::SetProjViewMatrix");
    TraceLn1(NWT_TRACE_VERBOSE, "  depthTest=%d", depthTest);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    projection._11 = (float)m00;    // Scale X
    projection._12 = (float)m10;    // Shear Y
    projection._13 = (float)m20;
    projection._14 = (float)m30;

    projection._21 = (float)m01;    // Shear X
    projection._22 = (float)m11;    // Scale Y
    projection._23 = (float)m21;
    projection._24 = (float)m31;

    projection._31 = (float)m02;
    projection._32 = (float)m12;
    projection._33 = (float)m22;
    projection._34 = (float)m32;

    projection._41 = (float)m03;    // Translate X
    projection._42 = (float)m13;    // Translate Y
    projection._43 = (float)m23;
    projection._44 = (float)m33;    // 1.0f;

    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", projection._11, projection._12, projection._13, projection._14);
    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", projection._21, projection._22, projection._23, projection._24);
    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", projection._31, projection._32, projection._33, projection._34);
    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", projection._41, projection._42, projection._43, projection._44);

//    fprintf(stderr, "SetProjViewMatrix: depthTest =  %d\n", depthTest);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", projection._11, projection._12, projection._13, projection._14);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", projection._21, projection._22, projection._23, projection._24);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", projection._31, projection._32, projection._33, projection._34);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", projection._41, projection._42, projection._43, projection._44);

    if (depthTest && !this->depthTest) {
        pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_TRUE);
        pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_TRUE);
        pd3dDevice->SetRenderState(D3DRS_ZFUNC, D3DCMP_LESSEQUAL);
    } else if (!depthTest && this->depthTest) {
        pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_FALSE);
        pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_FALSE);
    }
    this->depthTest = depthTest;

    if (phongShader) {
        D3DUtils_MatrixTransposed(mat, projection);
        SUCCEEDED(res = pd3dDevice->SetVertexShaderConstantF(VSR_VIEWPROJMATRIX, (float*) mat.m, 4));
    }

    return res;
}

void
D3DContext::setWorldTransformIndentity() {
    TraceLn(NWT_TRACE_INFO, "D3DContext::setWorldTransformIndentity");

    RETURN_IF_NULL(pd3dDevice);

    D3DUtils_SetIdentityMatrix(&world);
}

void
setWorldTx(D3DMATRIX &mat, jdouble m00, jdouble m01, jdouble m02, jdouble m03,
            jdouble m10, jdouble m11, jdouble m12, jdouble m13,
            jdouble m20, jdouble m21, jdouble m22, jdouble m23,
            jdouble m30, jdouble m31, jdouble m32, jdouble m33) {

    mat._11 = (float)m00;     // Scale X
    mat._12 = (float)m10;     // Shear Y
    mat._13 = (float)m20;
    mat._14 = (float)m30;

    mat._21 = (float)m01;    // Shear X
    mat._22 = (float)m11;    // Scale Y
    mat._23 = (float)m21;
    mat._24 = (float)m31;

    mat._31 = (float)m02;
    mat._32 = (float)m12;
    mat._33 = (float)m22;
    mat._34 = (float)m32;

    mat._41 = (float)m03;    // Translate X
    mat._42 = (float)m13;    // Translate Y
    mat._43 = (float)m23;
    mat._44 = (float)m33;    // 1.0f;

    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", mat._11, mat._12, mat._13, mat._14);
    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", mat._21, mat._22, mat._23, mat._24);
    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", mat._31, mat._32, mat._33, mat._34);
    TraceLn4(NWT_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", mat._41, mat._42, mat._43, mat._44);

//    fprintf(stderr, "World Matrix:\n");
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._11, mat._12, mat._13, mat._14);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._21, mat._22, mat._23, mat._24);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._31, mat._32, mat._33, mat._34);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._41, mat._42, mat._43, mat._44);
}

void
D3DContext::setWorldTransform(jdouble m00, jdouble m01, jdouble m02, jdouble m03,
            jdouble m10, jdouble m11, jdouble m12, jdouble m13,
            jdouble m20, jdouble m21, jdouble m22, jdouble m23,
            jdouble m30, jdouble m31, jdouble m32, jdouble m33) {

//    std::cerr << "D3DContext::setWorldTransform" << std::endl;
    TraceLn(NWT_TRACE_INFO, "D3DContext::setWorldTransform");

    RETURN_IF_NULL(pd3dDevice);

    setWorldTx(world,
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33);
}

HRESULT
D3DContext::ResetTransform()
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::ResetTransform");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    D3DUtils_SetIdentityMatrix(&world);
    return UpdateVertexShaderTX();
}

HRESULT
D3DContext::SetTransform(jdouble m00, jdouble m01, jdouble m02, jdouble m03,
                         jdouble m10, jdouble m11, jdouble m12, jdouble m13,
                         jdouble m20, jdouble m21, jdouble m22, jdouble m23,
                         jdouble m30, jdouble m31, jdouble m32, jdouble m33)

{

//    std::cerr << "D3DContext::SetTransform" << std::endl;
    TraceLn(NWT_TRACE_INFO, "D3DContext::SetTransform");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    setWorldTx(world,
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33);

    return UpdateVertexShaderTX();
}

HRESULT
D3DContext::SetRectClip(int x1, int y1, int x2, int y2)
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::SetRectClip");
    TraceLn4(NWT_TRACE_VERBOSE,
                "  x1=%-4d y1=%-4d x2=%-4d y2=%-4d",
                x1, y1, x2, y2);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    IDirect3DSurface9 *pCurrentTarget;
    HRESULT res = pd3dDevice->GetRenderTarget(0, &pCurrentTarget);
    RETURN_STATUS_IF_FAILED(res);

    D3DSURFACE_DESC desc;
    pCurrentTarget->GetDesc(&desc);
    SAFE_RELEASE(pCurrentTarget);

    if (x1 <= 0 && y1 <= 0 &&
        (UINT)x2 >= desc.Width && (UINT)y2 >= desc.Height)
    {
        TraceLn(NWT_TRACE_VERBOSE,
                   "  disabling clip (== render target dimensions)");
        return pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE);
    }

    // clip to the dimensions of the target surface, otherwise
    // SetScissorRect will fail
    if (x1 < 0)                 x1 = 0;
    if (y1 < 0)                 y1 = 0;
    if ((UINT)x2 > desc.Width)  x2 = desc.Width;
    if ((UINT)y2 > desc.Height) y2 = desc.Height;
    if (x1 > x2)                x2 = x1 = 0;
    if (y1 > y2)                y2 = y1 = 0;
    RECT newRect = { x1, y1, x2, y2 };
    if (SUCCEEDED(res = pd3dDevice->SetScissorRect(&newRect))) {
        res = pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, TRUE);
    } else {
        DebugPrintD3DError(res, "Error setting scissor rect");
        RlsTraceLn4(NWT_TRACE_ERROR,
                       "  x1=%-4d y1=%-4d x2=%-4d y2=%-4d",
                       x1, y1, x2, y2);
    }

    return res;
}

HRESULT
D3DContext::ResetClip()
{
    TraceLn(NWT_TRACE_INFO, "D3DContext::ResetClip");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    return pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE);
}

HRESULT D3DContext::BeginScene()
{
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    if (!bBeginScenePending) {
        bBeginScenePending = TRUE;
        HRESULT res = pd3dDevice->BeginScene();
        TraceLn(NWT_TRACE_INFO, "D3DContext::BeginScene");
        return res;
    }
    return S_OK;
}

HRESULT D3DContext::EndScene()
{
    if (bBeginScenePending) {
        bBeginScenePending = FALSE;
        TraceLn(NWT_TRACE_INFO, "D3DContext::EndScene");
        return pd3dDevice->EndScene();
    }
    return S_OK;
}

HRESULT D3DContext::InitContextCaps() {
    if (!IsPow2TexturesOnly()) {
        RlsTraceLn(NWT_TRACE_VERBOSE, "  CAPS_TEXNONPOW2");
    }
    if (!IsSquareTexturesOnly()) {
        RlsTraceLn(NWT_TRACE_VERBOSE, "  CAPS_TEXNONSQUARE");
    }
    return S_OK;
}

IDirect3DTexture9 *createTexture(D3DFORMAT format, int w, int h, IDirect3DSurface9 **pSurface, IDirect3DDevice9 *dev) {
    IDirect3DTexture9 *texture;
    HRESULT hr = dev->CreateTexture(w, h, 1, D3DUSAGE_DYNAMIC, format, D3DPOOL_SYSTEMMEM, &texture, 0);
    if (FAILED(hr)) {
        RlsTraceLn1(NWT_TRACE_ERROR, "Failed to create system memory texture for update operation: %08X", hr);
        return NULL;
    }
    TraceLn3(NWT_TRACE_VERBOSE, "Created system memory texture for update operation: %dx%d, format = %d", w, h, format);
    if (pSurface) {
        hr = texture->GetSurfaceLevel(0, pSurface);
        if (FAILED(hr)) {
            RlsTraceLn1(NWT_TRACE_ERROR, "Failed to get surface for update operation: %08X", hr);
            SAFE_RELEASE(texture);
            return NULL;
        }
    }
    return texture;
}

IDirect3DTexture9 *D3DContext::TextureUpdateCache::getTexture(
    D3DFORMAT format, int w, int h, IDirect3DSurface9 **pSurface, IDirect3DDevice9 *dev)
{
    if (w <= width && h <= height && texture != NULL) {
        if (pSurface) *pSurface = surface;
        return texture;
    }
    // grow the cache texture so that the new texture is
    // at least as large as the previous one
    if (w < width)  w = width;
    if (h < height) h = height;
    SAFE_RELEASE(surface);
    SAFE_RELEASE(texture);
    texture = createTexture(format, w, h, &surface, dev);
    if (texture == NULL) {
        return NULL;
    }
    width = w;
    height = h;
    if (pSurface) *pSurface = surface;
    return texture;
}

IDirect3DTexture9 *D3DContext::getTextureCache(int formatIndex, D3DFORMAT format, int width, int height, IDirect3DSurface9 **pSurface) {
    if (formatIndex < 0 || formatIndex >= NUM_TEXTURE_CACHE) {
        return createTexture(format, width, height, pSurface, pd3dDevice);
    }
    TextureUpdateCache &cache = textureCache[formatIndex];
    return cache.getTexture(format, width, height, pSurface, pd3dDevice);
}
