/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DContext.h"
#include "com_sun_prism_d3d_D3DShader.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_sun_prism_d3d_D3DShader_init
  (JNIEnv *env, jclass klass,
   jlong ctx, jobject bbuf, jint, jboolean, jboolean)
{
    TraceLn(NWT_TRACE_INFO, "D3DShader_init");

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, 0L);

    DWORD *buf = (DWORD *)env->GetDirectBufferAddress(bbuf);
    if (buf == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR,
                   "D3DShader_init: Could not get direct buffer address");
        return 0L;
    }

    D3DResourceManager *pMgr = pCtx->GetResourceManager();
    RETURN_STATUS_IF_NULL(pMgr, 0L);

    D3DPixelShaderResource *pPSResource;
    if (SUCCEEDED(pMgr->CreatePixelShader(buf, &pPSResource))) {
        return ptr_to_jlong(pPSResource);
    }
    return 0L;
}

JNIEXPORT jint JNICALL
Java_com_sun_prism_d3d_D3DShader_enable
  (JNIEnv *env, jclass klass,
   jlong ctx, jlong pData)
{
    TraceLn(NWT_TRACE_INFO, "D3DShader_enable");

    D3DPixelShaderResource *pPSResource =
        (D3DPixelShaderResource *)jlong_to_ptr(pData);
    RETURN_STATUS_IF_NULL(pPSResource, E_FAIL);

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

#if defined PERF_COUNTERS
    pCtx->getStats().numSetPixelShader++;
#endif

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    IDirect3DPixelShader9 *pShader = pPSResource->GetPixelShader();
    if (pShader == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR, "D3DShader_enable: pShader is null");
        return E_FAIL;
    }

    HRESULT res = pd3dDevice->SetPixelShader(pShader);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "D3DShader_enable: SetPixelShader failed");
    }
    return res;
}

JNIEXPORT jint JNICALL
Java_com_sun_prism_d3d_D3DShader_disable
  (JNIEnv *env, jclass klass,
   jlong ctx, jlong pData)
{
    TraceLn(NWT_TRACE_INFO, "D3DShader_disable");

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res = pd3dDevice->SetPixelShader(NULL);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "D3DShader_disable: SetPixelShader(NULL) failed");
    }

    return res;
}

JNIEXPORT jint JNICALL
Java_com_sun_prism_d3d_D3DShader_setConstantsI
  (JNIEnv *env, jclass klass,
   jlong ctx, jlong pData, jint reg, jobject ibuf, jint off, jint count)
{
    TraceLn3(NWT_TRACE_INFO, "D3DShader_setConstantsI (reg=%d, off=%d, count=%d)",
             reg, off, count);

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    jint *buf = (jint *)env->GetDirectBufferAddress(ibuf);

    //in bytes
    jlong capacity = env->GetDirectBufferCapacity(ibuf);

    if (off < 0 || count < 1 || off + count > capacity / sizeof(jint)) {
        RlsTraceLn(NWT_TRACE_ERROR, "  Array out of bounds access.");
        return E_FAIL;
    }

    if (buf == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR,
                   "D3DShader_setConstantsI: Could not get direct buffer address");
        return E_FAIL;
    }

    buf += off * sizeof(jint);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res = pd3dDevice->SetPixelShaderConstantI(reg, (const int *)buf, count);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "setConstantsI: SetPixelShaderConstantI failed");
    }

    return res;
}

JNIEXPORT jint JNICALL
Java_com_sun_prism_d3d_D3DShader_setConstantsF
  (JNIEnv *env, jclass klass,
   jlong ctx, jlong pData, jint reg, jobject fbuf, jint off, jint count)
{
    TraceLn3(NWT_TRACE_INFO, "D3DShader_setConstantsF (reg=%d, off=%d, count=%d)", reg, off, count);

    D3DContext *pCtx = (D3DContext*)jlong_to_ptr(ctx);
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    jfloat *buf = (jfloat *)env->GetDirectBufferAddress(fbuf);

    //in bytes
    jlong capacity = env->GetDirectBufferCapacity(fbuf);

    if (off < 0 || count < 1 || off + count > capacity / sizeof(jfloat)) {
        RlsTraceLn(NWT_TRACE_ERROR, "  Array out of bounds access.");
        return E_FAIL;
    }

    if (buf == NULL) {
        RlsTraceLn(NWT_TRACE_ERROR, "  Could not get direct buffer address");
        return E_FAIL;
    }

    buf += off * sizeof(jfloat);

    TraceLn4(NWT_TRACE_VERBOSE, "  vals: %f %f %f %f", buf[0], buf[1], buf[2], buf[3]);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res = pd3dDevice->SetPixelShaderConstantF(reg, (const float *)buf, count);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "setConstantsI: SetPixelShaderConstantF failed");
    }

    return res;
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DShader_nGetRegister
  (JNIEnv *, jclass, jlong, jlong, jstring)
{
    return -1;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL JNI_OnLoad_prism_d3d(JavaVM *vm, void *reserved) {
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif // JNI_VERSION_1_8
}
#endif // STATIC_BUILD

}
