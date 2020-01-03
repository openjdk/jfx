/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGDefaultCamera;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.MeshView;
import com.sun.prism.RTTexture;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.ps.Shader;

class D3DContext extends BaseShaderContext {

    public static final int D3DERR_DEVICENOTRESET   = 0x88760869;
    public static final int D3DERR_DEVICELOST       = 0x88760868;
    public static final int E_FAIL                  = 0x80004005;
    public static final int D3DERR_OUTOFVIDEOMEMORY = 0x8876017c;
    public static final int D3D_OK                  = 0x0;

    public static final int D3DCOMPMODE_CLEAR           = 0;
    public static final int D3DCOMPMODE_SRC             = 1;
    public static final int D3DCOMPMODE_SRCOVER         = 2;
    public static final int D3DCOMPMODE_DSTOUT          = 3;
    public static final int D3DCOMPMODE_ADD             = 4;

    public static final int D3DTADDRESS_NOP             = 0;
    public static final int D3DTADDRESS_WRAP            = 1;
    public static final int D3DTADDRESS_MIRROR          = 2;
    public static final int D3DTADDRESS_CLAMP           = 3;
    public static final int D3DTADDRESS_BORDER          = 4;

    // Use by face culling for 3D implementation
    public final static int CULL_BACK                  = 110;
    public final static int CULL_FRONT                 = 111;
    public final static int CULL_NONE                  = 112;
    /**
     * WIN32 COM bool FAILED(HRESULT hr) macro synonym
     * @param hr
     * @return
     */
    public static boolean FAILED(int hr) {
        return hr<0;
    }

    // Temp. variables (Not Thread Safe)
    private static GeneralTransform3D scratchTx = new GeneralTransform3D();
    private static final Affine3D scratchAffine3DTx = new Affine3D();
    private static double[] tempAdjustClipSpaceMat = new double[16];

    private State state;
    private boolean isLost = false;

    private final long pContext;

    private Vec3d cameraPos = new Vec3d();
    private GeneralTransform3D projViewTx = new GeneralTransform3D();
    private int targetWidth = 0, targetHeight = 0;

    private final D3DResourceFactory factory;

    public static final int NUM_QUADS = PrismSettings.superShader ? 4096 : 256;

    D3DContext(long pContext, Screen screen, D3DResourceFactory factory) {
        super(screen, factory, NUM_QUADS);
        this.pContext = pContext;
        this.factory = factory;
    }

    @Override
    public D3DResourceFactory getResourceFactory() {
        return factory;
    }

    protected void initState() {
        init();
        state = new State();
        validate(nSetBlendEnabled(pContext, D3DCOMPMODE_SRCOVER));
        validate(nSetDeviceParametersFor2D(pContext));
    }

    long getContextHandle() {
        return pContext;
    }

    /**
     * Returns whether the context is lost.
     * @return true if lost, false otherwise
     */
    boolean isLost() {
        return isLost;
    }

    /**
     * Does D3D native return value validation for DEBUG interests
     */
    static void validate(int res) {
        if (PrismSettings.verbose && FAILED(res)) {
            System.out.println("D3D hresult failed :" + hResultToString(res));
            new Exception("Stack trace").printStackTrace(System.out);
        }
    }

    /**
     * set device to lost state
     */
    private void setLost() {
        isLost = true;
    }

    /**
     * Validates the device, sets the context lost
     * status if necessary, and tries to restore the context if needed.
     */
    boolean testLostStateAndReset() {
        int hr = D3DResourceFactory.nTestCooperativeLevel(pContext);

        if (hr == D3DERR_DEVICELOST) {
            setLost();
        }

        if (hr == D3DERR_DEVICENOTRESET) {
            boolean wasLost = isLost();
            setLost();
            // disposing the lcd buffer because the device is about to be lost
            disposeLCDBuffer();
            factory.notifyReset();

            hr = D3DResourceFactory.nResetDevice(pContext);

            if (hr == D3D_OK) {
                isLost = false;
                initState();
                // Notify caller that the device was reset
                if (!wasLost) return false;
            }
        }

        return !FAILED(hr);
    }

    /**
     * Validates result of present operation,
     * sets the context lost status if necessary
     */
    boolean validatePresent(int res) {
        if (res == D3DERR_DEVICELOST || res == D3DERR_DEVICENOTRESET) {
            setLost();
        } else {
            validate(res);
        }

        return !FAILED(res);
    }

    /**
     * OpenGL projection transform use z-range of [-1, 1] while D3D expects it
     * to be [0, 1], so we need to adjust the matrix, see RT-32880.
     */
    private GeneralTransform3D adjustClipSpace(GeneralTransform3D projViewTx) {
        double[] m = projViewTx.get(tempAdjustClipSpaceMat);
        m[8] = (m[8] + m[12])/2;
        m[9] = (m[9] + m[13])/2;
        m[10] = (m[10] + m[14])/2;
        m[11] = (m[11] + m[15])/2;
        projViewTx.set(m);
        return projViewTx;
    }

    @Override
    protected State updateRenderTarget(RenderTarget target, NGCamera camera,
                                       boolean depthTest)  {
        long resourceHandle = ((D3DRenderTarget)target).getResourceHandle();
        int res = nSetRenderTarget(pContext, resourceHandle, depthTest, target.isMSAA());
        validate(res);
        // resetLastClip should be called only if render target was changed
        // return value is S_FALSE (success with negative result)
        // if render target wasn't changed
        if (res == D3D_OK) {
            resetLastClip(state);
        }

        targetWidth = target.getPhysicalWidth();
        targetHeight = target.getPhysicalHeight();

        // Need to validate the camera before getting its computed data.
        if (camera instanceof NGDefaultCamera) {
            ((NGDefaultCamera) camera).validate(targetWidth, targetHeight);
            projViewTx = adjustClipSpace(camera.getProjViewTx(projViewTx));
        } else {
            projViewTx = adjustClipSpace(camera.getProjViewTx(projViewTx));
            // TODO: verify that this is the right solution. There may be
            // other use-cases where rendering needs different viewport size.
            double vw = camera.getViewWidth();
            double vh = camera.getViewHeight();
            if (targetWidth != vw || targetHeight != vh) {
                projViewTx.scale(vw / targetWidth, vh / targetHeight, 1.0);
            }
        }

        // Set projection view matrix
        res = nSetProjViewMatrix(pContext, depthTest,
            projViewTx.get(0),  projViewTx.get(1),  projViewTx.get(2),  projViewTx.get(3),
            projViewTx.get(4),  projViewTx.get(5),  projViewTx.get(6),  projViewTx.get(7),
            projViewTx.get(8),  projViewTx.get(9),  projViewTx.get(10), projViewTx.get(11),
            projViewTx.get(12), projViewTx.get(13), projViewTx.get(14), projViewTx.get(15));
        validate(res);

        cameraPos = camera.getPositionInWorld(cameraPos);
//        System.err.println("Camera position in world = " + cameraPos);

        return state;
    }

    @Override
    protected void updateTexture(int texUnit, Texture tex) {
        long texHandle;
        boolean linear;
        int wrapMode;
        if (tex != null) {
            D3DTexture d3dtex = (D3DTexture)tex;
            texHandle = d3dtex.getNativeSourceHandle();
            linear = tex.getLinearFiltering();
            switch (tex.getWrapMode()) {
                case CLAMP_NOT_NEEDED:
                    wrapMode = D3DTADDRESS_NOP;
                    break;
                case CLAMP_TO_EDGE:
                case CLAMP_TO_EDGE_SIMULATED:
                case CLAMP_TO_ZERO_SIMULATED:
                    wrapMode = D3DTADDRESS_CLAMP;
                    break;
                case CLAMP_TO_ZERO:
                    wrapMode = D3DTADDRESS_BORDER;
                    break;
                case REPEAT:
                case REPEAT_SIMULATED:
                    wrapMode = D3DTADDRESS_WRAP;
                    break;
                default:
                    throw new InternalError("Unrecognized wrap mode: "+tex.getWrapMode());
            }
        } else {
            texHandle = 0L;
            linear = false;
            wrapMode = D3DTADDRESS_CLAMP;
        }
        validate(nSetTexture(pContext, texHandle, texUnit, linear, wrapMode));
    }

    @Override
    protected void updateShaderTransform(Shader shader, BaseTransform xform) {
        if (xform == null) {
            xform = BaseTransform.IDENTITY_TRANSFORM;
        }

        final GeneralTransform3D perspectiveTransform = getPerspectiveTransformNoClone();
        int res;
        if (xform.isIdentity() && perspectiveTransform.isIdentity()) {
            res = nResetTransform(pContext);
        } else if (perspectiveTransform.isIdentity()) {
            res = nSetTransform(pContext,
                xform.getMxx(), xform.getMxy(), xform.getMxz(), xform.getMxt(),
                xform.getMyx(), xform.getMyy(), xform.getMyz(), xform.getMyt(),
                xform.getMzx(), xform.getMzy(), xform.getMzz(), xform.getMzt(),
                0.0, 0.0, 0.0, 1.0);
        } else {
            scratchTx.setIdentity().mul(xform).mul(perspectiveTransform);
            res = nSetTransform(pContext,
                scratchTx.get(0), scratchTx.get(1), scratchTx.get(2), scratchTx.get(3),
                scratchTx.get(4), scratchTx.get(5), scratchTx.get(6), scratchTx.get(7),
                scratchTx.get(8), scratchTx.get(9), scratchTx.get(10), scratchTx.get(11),
                scratchTx.get(12), scratchTx.get(13), scratchTx.get(14), scratchTx.get(15));
        }
        validate(res);
    }

    @Override
    protected void updateWorldTransform(BaseTransform xform) {
        if ((xform == null) || xform.isIdentity()) {
            nSetWorldTransformToIdentity(pContext);
        } else {
            nSetWorldTransform(pContext,
                    xform.getMxx(), xform.getMxy(), xform.getMxz(), xform.getMxt(),
                    xform.getMyx(), xform.getMyy(), xform.getMyz(), xform.getMyt(),
                    xform.getMzx(), xform.getMzy(), xform.getMzz(), xform.getMzt(),
                    0.0, 0.0, 0.0, 1.0);
        }
    }

    @Override
    protected void updateClipRect(Rectangle clipRect) {
        int res;
        if (clipRect == null || clipRect.isEmpty()) {
            res = nResetClipRect(pContext);
        } else {
            int x1 = clipRect.x;
            int y1 = clipRect.y;
            int x2 = x1 + clipRect.width;
            int y2 = y1 + clipRect.height;
            res = nSetClipRect(pContext, x1, y1, x2, y2);
        }
        validate(res);
    }

    @Override
    protected void updateCompositeMode(CompositeMode mode) {
        int d3dmode;
        switch (mode) {
            case CLEAR:
                d3dmode = D3DCOMPMODE_CLEAR;
                break;
            case SRC:
                d3dmode = D3DCOMPMODE_SRC;
                break;
            case SRC_OVER:
                d3dmode = D3DCOMPMODE_SRCOVER;
                break;
            case DST_OUT:
                d3dmode = D3DCOMPMODE_DSTOUT;
                break;
            case ADD:
                d3dmode = D3DCOMPMODE_ADD;
                break;
            default:
                throw new InternalError("Unrecognized composite mode: "+mode);
        }
        validate(nSetBlendEnabled(pContext, d3dmode));
    }

    D3DFrameStats getFrameStats(boolean reset, D3DFrameStats result) {
        if (result == null) {
            result = new D3DFrameStats();
        }
        return nGetFrameStats(pContext, result, reset) ? result : null;
    }

    /*
     * @param depthBuffer if true will create and attach a depthBuffer,
     * if needed, of the same format as the render target. The depth test state
     * is handled elsewhere.
     */
    private static native int nSetRenderTarget(long pContext, long pDest, boolean depthBuffer, boolean msaa);
    private static native int nSetTexture(long pContext, long pTex, int texUnit,
        boolean linear, int wrapMode);
    private static native int nResetTransform(long pContext);
    private static native int nSetTransform(long pContext,
        double m00, double m01, double m02, double m03,
        double m10, double m11, double m12, double m13,
        double m20, double m21, double m22, double m23,
        double m30, double m31, double m32, double m33);
    private static native void nSetWorldTransformToIdentity(long pContext);
    private static native void nSetWorldTransform(long pContext,
            double m00, double m01, double m02, double m03,
            double m10, double m11, double m12, double m13,
            double m20, double m21, double m22, double m23,
            double m30, double m31, double m32, double m33);
    private static native int nSetCameraPosition(long pContext, double x, double y, double z);
    private static native int nSetProjViewMatrix(long pContext, boolean isOrtho,
        double m00, double m01, double m02, double m03,
        double m10, double m11, double m12, double m13,
        double m20, double m21, double m22, double m23,
        double m30, double m31, double m32, double m33);
    private static native int nResetClipRect(long pContext);
    private static native int nSetClipRect(long pContext,
        int x1, int y1, int x2, int y2);
    private static native int nSetBlendEnabled(long pContext, int mode);
    private static native int nSetDeviceParametersFor2D(long pContext);
    private static native int nSetDeviceParametersFor3D(long pContext);

    private static native long nCreateD3DMesh(long pContext);
    private static native void nReleaseD3DMesh(long pContext, long nativeHandle);
    private static native boolean nBuildNativeGeometryShort(long pContext, long nativeHandle,
            float[] vertexBuffer, int vertexBufferLength, short[] indexBuffer, int indexBufferLength);
    private static native boolean nBuildNativeGeometryInt(long pContext, long nativeHandle,
            float[] vertexBuffer, int vertexBufferLength, int[] indexBuffer, int indexBufferLength);
    private static native long nCreateD3DPhongMaterial(long pContext);
    private static native void nReleaseD3DPhongMaterial(long pContext, long nativeHandle);
    private static native void nSetDiffuseColor(long pContext, long nativePhongMaterial,
            float r, float g, float b, float a);
    private static native void nSetSpecularColor(long pContext, long nativePhongMaterial,
            boolean set, float r, float g, float b, float a);
    private static native void nSetMap(long pContext, long nativePhongMaterial,
            int mapType, long texID);
    private static native long nCreateD3DMeshView(long pContext, long nativeMesh);
    private static native void nReleaseD3DMeshView(long pContext, long nativeHandle);
    private static native void nSetCullingMode(long pContext, long nativeMeshView,
            int cullingMode);
    private static native void nSetMaterial(long pContext, long nativeMeshView,
            long nativePhongMaterialInfo);
    private static native void nSetWireframe(long pContext, long nativeMeshView,
            boolean wireframe);
    private static native void nSetAmbientLight(long pContext, long nativeMeshView,
            float r, float g, float b);
    private static native void nSetPointLight(long pContext, long nativeMeshView,
            int index, float x, float y, float z, float r, float g, float b, float w,
            float ca, float la, float qa, float maxRange);
    private static native void nRenderMeshView(long pContext, long nativeMeshView);
    private static native int nDrawIndexedQuads(long pContext,
            float coords[], byte colors[], int numVertices);


    /*
     * @param nSrcRTT must be valid native resource
     * @param nDstRTT can be NULL if a valide render target is set
     */
    private static native void nBlit(long pContext, long nSrcRTT, long nDstRTT,
            int srcX0, int srcY0, int srcX1, int srcY1,
            int dstX0, int dstY0, int dstX1, int dstY1);

    private static native boolean nGetFrameStats(long pContext,
            D3DFrameStats returnValue, boolean bReset);

    private static native boolean nIsRTTVolatile(long contextHandle);

    public boolean isRTTVolatile() {
        return nIsRTTVolatile(pContext);
    }

    public static String hResultToString(long hResult) {
        switch ((int)hResult) {
            case (int)D3DERR_DEVICENOTRESET:
                return "D3DERR_DEVICENOTRESET";
            case (int)D3DERR_DEVICELOST:
                return "D3DERR_DEVICELOST";
            case (int)D3DERR_OUTOFVIDEOMEMORY:
                return "D3DERR_OUTOFVIDEOMEMORY";
            case (int)D3D_OK:
                return "D3D_OK";
            default:
                return "D3D_ERROR " + Long.toHexString(hResult);
        }
    }

    @Override
    public void setDeviceParametersFor2D() {
        nSetDeviceParametersFor2D(pContext);
    }

    @Override
    protected void setDeviceParametersFor3D() {
        nSetDeviceParametersFor3D(pContext);
    }

    long createD3DMesh() {
        return nCreateD3DMesh(pContext);
    }

    // TODO: 3D - Should this be called dispose?
    void releaseD3DMesh(long nativeHandle) {
        nReleaseD3DMesh(pContext, nativeHandle);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer, int vertexBufferLength,
            short[] indexBuffer, int indexBufferLength) {
        return nBuildNativeGeometryShort(pContext, nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer, int vertexBufferLength,
            int[] indexBuffer, int indexBufferLength) {
        return nBuildNativeGeometryInt(pContext, nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    long createD3DPhongMaterial() {
        return nCreateD3DPhongMaterial(pContext);
    }

    // TODO: 3D - Should this be called dispose?
    void releaseD3DPhongMaterial(long nativeHandle) {
        nReleaseD3DPhongMaterial(pContext, nativeHandle);
    }

    void setDiffuseColor(long nativePhongMaterial, float r, float g, float b, float a) {
        nSetDiffuseColor(pContext, nativePhongMaterial, r, g, b, a);
    }

    void setSpecularColor(long nativePhongMaterial, boolean set, float r, float g, float b, float a) {
        nSetSpecularColor(pContext, nativePhongMaterial, set, r, g, b, a);
    }

    void setMap(long nativePhongMaterial, int mapType, long nativeTexture) {
        nSetMap(pContext, nativePhongMaterial, mapType, nativeTexture);
    }

    long createD3DMeshView(long nativeMesh) {
        return nCreateD3DMeshView(pContext, nativeMesh);
    }

    // TODO: 3D - Should this be called dispose?
    void releaseD3DMeshView(long nativeMeshView) {
        nReleaseD3DMeshView(pContext, nativeMeshView);
    }

    void setCullingMode(long nativeMeshView, int cullMode) {
        int cm;
        if (cullMode == MeshView.CULL_NONE) {
            cm = CULL_NONE;
        } else if (cullMode == MeshView.CULL_BACK) {
            cm = CULL_BACK;
        } else if (cullMode == MeshView.CULL_FRONT) {
            cm = CULL_FRONT;
        } else {
            throw new IllegalArgumentException("illegal value for CullMode: " + cullMode);
        }
        nSetCullingMode(pContext, nativeMeshView, cm);
    }

    void setMaterial(long nativeMeshView, long nativePhongMaterial) {
        nSetMaterial(pContext, nativeMeshView, nativePhongMaterial);
    }

    void setWireframe(long nativeMeshView, boolean wireframe) {
         nSetWireframe(pContext, nativeMeshView, wireframe);
    }

    void setAmbientLight(long nativeMeshView, float r, float g, float b) {
        nSetAmbientLight(pContext, nativeMeshView, r, g, b);
    }

    void setPointLight(long nativeMeshView, int index, float x, float y, float z,
            float r, float g, float b, float w, float ca, float la, float qa, float maxRange) {
        nSetPointLight(pContext, nativeMeshView, index, x, y, z, r, g, b, w, ca, la, qa, maxRange);
    }

    @Override
    protected void renderQuads(float coordArray[], byte colorArray[], int numVertices) {
        int res = nDrawIndexedQuads(pContext, coordArray, colorArray, numVertices);
        D3DContext.validate(res);
    }

    void renderMeshView(long nativeMeshView, Graphics g) {

        // Support retina display by scaling the projViewTx and pass it to the shader.
        scratchTx = scratchTx.set(projViewTx);
        float pixelScaleFactorX = g.getPixelScaleFactorX();
        float pixelScaleFactorY = g.getPixelScaleFactorY();
        if (pixelScaleFactorX != 1.0 || pixelScaleFactorY != 1.0) {
            scratchTx.scale(pixelScaleFactorX, pixelScaleFactorY, 1.0);
        }

        // Set projection view matrix
        int res = nSetProjViewMatrix(pContext, g.isDepthTest(),
                scratchTx.get(0), scratchTx.get(1), scratchTx.get(2), scratchTx.get(3),
                scratchTx.get(4), scratchTx.get(5), scratchTx.get(6), scratchTx.get(7),
                scratchTx.get(8), scratchTx.get(9), scratchTx.get(10), scratchTx.get(11),
                scratchTx.get(12), scratchTx.get(13), scratchTx.get(14), scratchTx.get(15));
        validate(res);

        res = nSetCameraPosition(pContext, cameraPos.x, cameraPos.y, cameraPos.z);
        validate(res);

        // Undo the SwapChain scaling done in createGraphics() because 3D needs
        // this information in the shader (via projViewTx)
        BaseTransform xform = g.getTransformNoClone();
        if (pixelScaleFactorX != 1.0 || pixelScaleFactorY != 1.0) {
            scratchAffine3DTx.setToIdentity();
            scratchAffine3DTx.scale(1.0 / pixelScaleFactorX, 1.0 / pixelScaleFactorY);
            scratchAffine3DTx.concatenate(xform);
            updateWorldTransform(scratchAffine3DTx);
        } else {
            updateWorldTransform(xform);
        }

        nRenderMeshView(pContext, nativeMeshView);
    }

    @Override
    public void blit(RTTexture srcRTT, RTTexture dstRTT,
                     int srcX0, int srcY0, int srcX1, int srcY1,
                     int dstX0, int dstY0, int dstX1, int dstY1) {
        long dstNativeHandle = dstRTT == null ? 0L : ((D3DTexture)dstRTT).getNativeSourceHandle();
        long srcNativeHandle = ((D3DTexture)srcRTT).getNativeSourceHandle();
        nBlit(pContext, srcNativeHandle, dstNativeHandle,
                          srcX0, srcY0, srcX1, srcY1,
                          dstX0, dstY0, dstX1, dstY1);
    }
}
