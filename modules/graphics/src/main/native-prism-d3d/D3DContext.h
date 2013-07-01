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
#include "D3DResourceManager.h"
#include "D3DPhongShader.h"

#if !defined NO_PERF_COUNTERS && !defined PERF_COUNTERS
    #define PERF_COUNTERS
#endif

// allow for 256 quads to match the size of the D3DVertexBuffer's nio buffer

#define MAX_BATCH_QUADS 256
#define MAX_VERTICES (MAX_BATCH_QUADS*4)

struct PRISM_VERTEX_2D {
    float x, y, z;
    DWORD color;
    float tu1, tv1;
    float tu2, tv2;
};

struct PRISM_VERTEX_3D {
    float x, y, z;
    float tu, tv;
    float nx, ny, nz, nw;
};

const D3DVERTEXELEMENT9 PrismVDecl[5] = {
    { 0,  0, D3DDECLTYPE_FLOAT3,   D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_POSITION, 0 },
    { 0, 12, D3DDECLTYPE_D3DCOLOR, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_COLOR,    0 },
    { 0, 16, D3DDECLTYPE_FLOAT2,   D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_TEXCOORD, 0 },
    { 0, 24, D3DDECLTYPE_FLOAT2,   D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_TEXCOORD, 1 },
    D3DDECL_END()
};

#define RELEASE_ALL (0)
#define RELEASE_DEFAULT (1)

class D3DResource;
class D3DVertexBufferResource;
class D3DResourceManager;

// - D3DContext class  -----------------------------------------------

/**
 * This class provides the following functionality:
 *  - holds the state of D3DContext java class (current pixel color,
 *    alpha compositing mode, extra alpha)
 *  - provides access to IDirect3DDevice9 interface (creation,
 *    disposal, exclusive access)
 *  - handles state changes of the direct3d device (transform,
 *    compositing mode, current texture)
 *  - provides means of creating textures, plain surfaces
 *  - implements primitives batching mechanism
 */
class D3DContext {
public:

    HRESULT drawIndexedQuads(struct PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, int numVerts);

    HRESULT drawTriangleList(struct PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, int numTriangles);


    /**
     * Releases the old device (if there was one) and all associated
     * resources, re-creates, initializes and tests the new device.
     *
     * If the device doesn't pass the test, it's released.
     *
     * Used when the context is first created, and then after a
     * display change event.
     *
     * Note that this method also does the necessary registry checks,
     * and if the registry shows that we've crashed when attempting
     * to initialize and test the device last time, it doesn't attempt
     * to create/init/test the device.
     */
    static HRESULT CreateInstance(IDirect3D9 *pd3d9, IDirect3D9Ex *pd3d9Ex, UINT adapter, D3DContext **ppCtx);

    // desrtoys this instance
    /* virtual */ int release();

    // creates a new D3D windowed device with swap copy effect and default
    // present interval
    HRESULT InitContext();

    // resets existing D3D device with the current presentation parameters
    HRESULT ResetContext();

    HRESULT TestCooperativeLevel();

    void    ReleaseContextResources(int releaseType);

    D3DResourceManager *GetResourceManager() { return pResourceMgr; }
    D3DVertexBufferResource *GetVertexBufferRes() { return pVertexBufferRes; }

    // returns capabilities of the Direct3D device
    D3DCAPS9 *GetDeviceCaps() { return &devCaps; }


    D3DPRESENT_PARAMETERS *GetPresentationParams() { return &curParams; }

    IDirect3DDevice9 *Get3DDevice() { return pd3dDevice; }
    IDirect3DDevice9Ex *Get3DExDevice() { return pd3dDeviceEx; }

    IDirect3D9 *Get3DObject() { return pd3dObject; }

    D3DMATRIX *GetViewProjTx() { return &projection; }

    D3DMATRIX *GetWorldTx() { return &world; }

    D3DVECTOR *GetCamPos() { return &camPos; }

    D3DPOOL getResourcePool() { return defaulResourcePool; }

    HRESULT SetRenderTarget(IDirect3DSurface9 *pSurface);
    HRESULT SetCameraPosition(jdouble camPosX, jdouble camPosY, jdouble camPosZ);
    HRESULT SetProjViewMatrix(BOOL isOrtho,
                              jdouble m00, jdouble m01, jdouble m02, jdouble m03,
                              jdouble m10, jdouble m11, jdouble m12, jdouble m13,
                              jdouble m20, jdouble m21, jdouble m22, jdouble m23,
                              jdouble m30, jdouble m31, jdouble m32, jdouble m33);
    HRESULT SetTransform(jdouble m00, jdouble m01, jdouble m02, jdouble m03,
                         jdouble m10, jdouble m11, jdouble m12, jdouble m13,
                         jdouble m20, jdouble m21, jdouble m22, jdouble m23,
                         jdouble m30, jdouble m31, jdouble m32, jdouble m33);
    HRESULT ResetTransform();

    // clears the zbuffer, target or stencil depending on the passed flag,
    // disables scissor test if needed
    HRESULT Clear(DWORD colorArgbPre, BOOL clearDepth, BOOL ignoreScissor);

    // clipping-related methods
    HRESULT SetRectClip(int x1, int y1, int x2, int y2);
    HRESULT ResetClip();

    BOOL IsPow2TexturesOnly()
        { return devCaps.TextureCaps & D3DPTEXTURECAPS_POW2; };
    BOOL IsSquareTexturesOnly()
        { return devCaps.TextureCaps & D3DPTEXTURECAPS_SQUAREONLY; }
    BOOL IsHWRasterizer() { return bIsHWRasterizer; }

    BOOL IsDynamicTextureSupported()
        { return devCaps.Caps2 & D3DCAPS2_DYNAMICTEXTURES; }
    BOOL IsImmediateIntervalSupported()
        { return devCaps.PresentationIntervals & D3DPRESENT_INTERVAL_IMMEDIATE;}

    // primitives batching-related methods
    /**
     * Calls devices's BeginScene if there weren't one already pending,
     * sets the pending flag.
     */
    HRESULT BeginScene();

    HRESULT setDeviceParametersFor2D();

    HRESULT setDeviceParametersFor3D();

    void setWorldTransformIndentity();

    void setWorldTransform(jdouble m00, jdouble m01, jdouble m02, jdouble m03,
            jdouble m10, jdouble m11, jdouble m12, jdouble m13,
            jdouble m20, jdouble m21, jdouble m22, jdouble m23,
            jdouble m30, jdouble m31, jdouble m32, jdouble m33);

    /**
     * Flushes the vertex queue and does end scene if
     * a BeginScene is pending
     */
    HRESULT EndScene();

#if defined PERF_COUNTERS
    struct FrameStats {
        int numTrianglesDrawn;
        int numDrawCalls;
        int numBufferLocks;
        int numTextureLocks;
        int numTextureTransferBytes;
        int numSetTexture;
        int numSetPixelShader;
        int numRenderTargetSwitch;

        void clear() {
            numTrianglesDrawn = 0;
            numDrawCalls = 0;
            numBufferLocks = 0;
            numTextureLocks = 0;
            numTextureTransferBytes = 0;
            numSetTexture = 0;
            numSetPixelShader = 0;
            numRenderTargetSwitch = 0;
        }
    } stats;

    FrameStats& getStats() { return stats; }
#endif

    HMONITOR getAdapterMonitor() {
        return pd3dObject->GetAdapterMonitor(adapterOrdinal);
    }

    // Use for 3D Implementation
    D3DPhongShader *getPhongShader() { return phongShader; }

    // States use in 3D primitive rendering
    struct State {
        bool wireframe;
        int cullMode;
    } state;

private:
     ~D3DContext();


    IDirect3DVertexShader9 *pPassThroughVS;
    IDirect3DVertexDeclaration9 *pVertexDecl;
    IDirect3DIndexBuffer9 *pIndices;
    D3DVertexBufferResource  *pVertexBufferRes;

    D3DMATRIX world; // node local to world transform
    D3DMATRIX projection; // projection view transform (TODO: This should now include the camera's world to local tx?)
    D3DVECTOR camPos; // camera position in world coord.

    float pixadjustx, pixadjusty;

    // finds appropriate to the target surface depth format,
    // creates the depth buffer and installs it onto the device
    HRESULT InitDepthStencilBuffer(D3DSURFACE_DESC *pTargetDesc);
    // returns true if the current depth buffer is compatible
    // with the new target, and the dimensions fit, false otherwise
    BOOL IsDepthStencilBufferOk(D3DSURFACE_DESC *pTargetDesc);

    HRESULT UpdateVertexShaderTX();

    D3DContext(IDirect3D9 *pd3d, IDirect3D9Ex *pd3dEx, UINT adapter);
    HRESULT InitDevice(IDirect3DDevice9 *d3dDevice);
    HRESULT InitContextCaps();
    IDirect3DDevice9        *pd3dDevice;
    IDirect3DDevice9Ex      *pd3dDeviceEx;
    HWND                     deviceWindow;
    IDirect3D9              *pd3dObject;
    IDirect3D9Ex            *pd3dObjectEx;

    D3DPOOL defaulResourcePool;

    D3DResourceManager      *pResourceMgr;

    UINT adapterOrdinal;
    BOOL bIsHWRasterizer;

    D3DPRESENT_PARAMETERS   curParams;
    D3DCAPS9                devCaps;

    /**
     * Used to implement simple primitive batching.
     * See BeginScene/EndScene/ForceEndScene.
     */
    BOOL    bBeginScenePending;

    /**
     * 3D implementation
     */
    D3DPhongShader *phongShader;
};

#define DEVICE_RESET           0
#define DEVICE_DISPOSED        1
