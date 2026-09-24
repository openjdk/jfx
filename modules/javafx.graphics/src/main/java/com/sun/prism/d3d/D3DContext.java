/*
 * Copyright (c) 2008, 2026, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;

class D3DContext extends BaseShaderContext {

    public static final int D3DERR_DEVICELOST       = 0x88760868;
    public static final int D3DERR_DEVICENOTRESET   = 0x88760869;
    public static final int D3DERR_DEVICEREMOVED    = 0x88760870;
    public static final int D3DERR_DEVICEHUNG       = 0X88760874;
    public static final int D3DERR_OUTOFVIDEOMEMORY = 0x8876017c;

    public static final int E_FAIL                  = 0x80004005;
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

    /** The native {@code D3DContext*}; the C++ owns it. */
    private final MemorySegment pContext;

    /*
     * Off-heap scratch for the calls that take a pointer: the 4x4 matrices and the light parameters the
     * JNI methods took as scalars, and the out-structs. Confined to the render thread, the only thread
     * that touches this context, and closed in dispose(). Nothing on the C side retains a pointer to it.
     */
    private final Arena scratchArena = Arena.ofConfined();
    private final MemorySegment matrixScratch = scratchArena.allocate(D3DNative.MATRIX_LAYOUT);
    private final MemorySegment lightScratch = scratchArena.allocate(D3DNative.LIGHT_LAYOUT);
    private final MemorySegment frameStatsScratch = scratchArena.allocate(D3DNative.FRAME_STATS_LAYOUT);
    private final MemorySegment textureInfoScratch = scratchArena.allocate(D3DNative.TEXTURE_INFO_LAYOUT);
    private final MemorySegment sizeScratch = scratchArena.allocate(D3DNative.SIZE_LAYOUT);

    private Vec3d cameraPos = new Vec3d();
    private GeneralTransform3D projViewTx = new GeneralTransform3D();
    private int targetWidth = 0, targetHeight = 0;

    private final D3DResourceFactory factory;

    public static final int NUM_QUADS = PrismSettings.superShader ? 4096 : 256;

    D3DContext(MemorySegment pContext, Screen screen, D3DResourceFactory factory) {
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
        validate(D3DNative.contextSetBlendMode(pContext, D3DCOMPMODE_SRCOVER));
        validate(D3DNative.contextSetDeviceParameters2D(pContext));
    }

    MemorySegment getContextHandle() {
        return pContext;
    }

    /**
     * The {@code D3dTextureInfo} out-struct the resource factory hands to the create calls. Valid only
     * until the next create call on this context.
     */
    MemorySegment textureInfoScratch() {
        return textureInfoScratch;
    }

    /**
     * The physical width of a resource, or -1 for a {@code NULL} handle - the answer
     * {@code nGetTextureWidth} gave. The NULL case never reaches the native side.
     */
    int getResourceWidth(MemorySegment resource) {
        return readResourceSize(resource) ? D3DNative.sizeWidth(sizeScratch) : -1;
    }

    /** The physical height of a resource, or -1 for a {@code NULL} handle; see {@link #getResourceWidth}. */
    int getResourceHeight(MemorySegment resource) {
        return readResourceSize(resource) ? D3DNative.sizeHeight(sizeScratch) : -1;
    }

    private boolean readResourceSize(MemorySegment resource) {
        if (resource.address() == 0L) {
            return false;
        }
        return D3DNative.resourceGetSize(resource, sizeScratch) == 0;
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
            System.err.println("D3D hresult failed :" + hResultToString(res));
            new Exception("Stack trace").printStackTrace(System.err);
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
     * If the device has been removed, then it reinitializes the device, which
     * disposes the existing factories and contexts.
     */
    boolean testLostStateAndReset() {
        if (isDisposed()) {
            return false;
        }

        int hr = D3DNative.contextTestCooperativeLevel(pContext);

        if (PrismSettings.verbose && FAILED(hr)) {
            System.err.print("D3DContext::testLostStateAndReset : ");
            switch (hr) {
                case D3D_OK:
                    System.err.println("D3D_OK");
                    break;
                case D3DERR_DEVICELOST:
                    System.err.println("D3DERR_DEVICELOST");
                    break;
                case D3DERR_DEVICEREMOVED:
                    System.err.println("D3DERR_DEVICEREMOVED");
                    break;
                case D3DERR_DEVICENOTRESET:
                    System.err.println("D3DERR_DEVICENOTRESET");
                    break;
                case D3DERR_DEVICEHUNG:
                    System.err.println("D3DERR_DEVICEHUNG");
                    break;
                case E_FAIL:
                    System.err.println("E_FAIL");
                    break;
                default:
                    System.err.println(String.format("Unknown D3D error 0x%x", hr));
            }
        }

        if (hr == D3DERR_DEVICELOST) {
            setLost();
        }

        if (hr == D3DERR_DEVICENOTRESET) {
            boolean wasLost = isLost();
            setLost();
            // disposing the lcd buffer because the device is about to be lost
            disposeLCDBuffer();
            factory.notifyReset();

            hr = D3DNative.contextResetDevice(pContext);

            if (hr == D3D_OK) {
                isLost = false;
                initState();
                // Notify caller that the device was reset
                if (!wasLost) return false;
            }
        }

        if (hr == D3DERR_DEVICEREMOVED || hr == D3DERR_DEVICEHUNG) {
            setLost();

            // Reinitialize the D3DPipeline. This will dispose and recreate
            // the resource factory and context for each adapter.
            D3DPipeline.getInstance().reinitialize();
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

    @Override
    public void dispose() {
        disposeLCDBuffer();
        state = null;

        super.dispose();
        if (scratchArena.scope().isAlive()) {
            scratchArena.close();
        }
    }

    /**
     * OpenGL projection transform use z-range of [-1, 1] while D3D expects it
     * to be [0, 1], so we need to adjust the matrix, see JDK-8123305.
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

    /**
     * Writes the 16 values of {@code tx} into the matrix scratch in {@code get(0..15)} order - the
     * order the JNI methods took them as scalars.
     */
    private MemorySegment fillMatrix(GeneralTransform3D tx) {
        for (int i = 0; i < 16; i++) {
            matrixScratch.setAtIndex(JAVA_DOUBLE, i, tx.get(i));
        }
        return matrixScratch;
    }

    /** Writes the affine {@code xform} into the matrix scratch with the last row {@code 0, 0, 0, 1}. */
    private MemorySegment fillMatrix(BaseTransform xform) {
        matrixScratch.setAtIndex(JAVA_DOUBLE, 0, xform.getMxx());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 1, xform.getMxy());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 2, xform.getMxz());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 3, xform.getMxt());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 4, xform.getMyx());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 5, xform.getMyy());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 6, xform.getMyz());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 7, xform.getMyt());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 8, xform.getMzx());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 9, xform.getMzy());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 10, xform.getMzz());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 11, xform.getMzt());
        matrixScratch.setAtIndex(JAVA_DOUBLE, 12, 0.0);
        matrixScratch.setAtIndex(JAVA_DOUBLE, 13, 0.0);
        matrixScratch.setAtIndex(JAVA_DOUBLE, 14, 0.0);
        matrixScratch.setAtIndex(JAVA_DOUBLE, 15, 1.0);
        return matrixScratch;
    }

    @Override
    protected State updateRenderTarget(RenderTarget target, NGCamera camera,
                                       boolean depthTest)  {
        if (checkDisposed()) return null;

        MemorySegment resourceHandle = ((D3DRenderTarget)target).getResourceHandle();
        int res = D3DNative.contextSetRenderTarget(pContext, resourceHandle, depthTest, target.isMSAA());
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
        res = D3DNative.contextSetProjViewMatrix(pContext, depthTest, fillMatrix(projViewTx));
        validate(res);

        cameraPos = camera.getPositionInWorld(cameraPos);
//        System.err.println("Camera position in world = " + cameraPos);

        return state;
    }

    @Override
    protected void updateTexture(int texUnit, Texture tex) {
        MemorySegment texHandle;
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
            texHandle = MemorySegment.NULL;
            linear = false;
            wrapMode = D3DTADDRESS_CLAMP;
        }
        validate(D3DNative.contextSetTexture(pContext, texHandle, texUnit, linear, wrapMode));
    }

    @Override
    protected void updateShaderTransform(Shader shader, BaseTransform xform) {
        if (xform == null) {
            xform = BaseTransform.IDENTITY_TRANSFORM;
        }

        final GeneralTransform3D perspectiveTransform = getPerspectiveTransformNoClone();
        int res;
        if (xform.isIdentity() && perspectiveTransform.isIdentity()) {
            res = D3DNative.contextResetTransform(pContext);
        } else if (perspectiveTransform.isIdentity()) {
            res = D3DNative.contextSetTransform(pContext, fillMatrix(xform));
        } else {
            scratchTx.setIdentity().mul(xform).mul(perspectiveTransform);
            res = D3DNative.contextSetTransform(pContext, fillMatrix(scratchTx));
        }
        validate(res);
    }

    @Override
    protected void updateWorldTransform(BaseTransform xform) {
        if ((xform == null) || xform.isIdentity()) {
            D3DNative.contextSetWorldTransform(pContext, MemorySegment.NULL);
        } else {
            D3DNative.contextSetWorldTransform(pContext, fillMatrix(xform));
        }
    }

    @Override
    protected void updateClipRect(Rectangle clipRect) {
        int res;
        if (clipRect == null || clipRect.isEmpty()) {
            res = D3DNative.contextResetClipRect(pContext);
        } else {
            int x1 = clipRect.x;
            int y1 = clipRect.y;
            int x2 = x1 + clipRect.width;
            int y2 = y1 + clipRect.height;
            res = D3DNative.contextSetClipRect(pContext, x1, y1, x2, y2);
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
        validate(D3DNative.contextSetBlendMode(pContext, d3dmode));
    }

    D3DFrameStats getFrameStats(boolean reset, D3DFrameStats result) {
        if (result == null) {
            result = new D3DFrameStats();
        }
        return D3DNative.contextGetFrameStats(pContext, frameStatsScratch, reset, result) ? result : null;
    }

    public static String hResultToString(long hResult) {
        switch ((int)hResult) {
            case D3DERR_DEVICELOST:
                return "D3DERR_DEVICELOST";
            case D3DERR_DEVICENOTRESET:
                return "D3DERR_DEVICENOTRESET";
            case D3DERR_DEVICEREMOVED:
                return "D3DERR_DEVICEREMOVED";
            case D3DERR_DEVICEHUNG:
                return "D3DERR_DEVICEHUNG";
            case D3DERR_OUTOFVIDEOMEMORY:
                return "D3DERR_OUTOFVIDEOMEMORY";
            case D3D_OK:
                return "D3D_OK";
            default:
                return "D3D_ERROR " + Long.toHexString(hResult);
        }
    }

    @Override
    public void setDeviceParametersFor2D() {
        if (checkDisposed()) return;

        D3DNative.contextSetDeviceParameters2D(pContext);
    }

    @Override
    protected void setDeviceParametersFor3D() {
        if (checkDisposed()) return;

        D3DNative.contextSetDeviceParameters3D(pContext);
    }

    /** The native {@code D3DMesh*}, or {@link MemorySegment#NULL} when disposed or when creation failed. */
    MemorySegment createD3DMesh() {
        if (checkDisposed()) return MemorySegment.NULL;

        return D3DNative.meshCreate(pContext);
    }

    // TODO: 3D - Should this be called dispose?
    void releaseD3DMesh(MemorySegment nativeHandle) {
        D3DNative.meshRelease(nativeHandle);
    }

    boolean buildNativeGeometry(MemorySegment nativeHandle, float[] vertexBuffer, int vertexBufferLength,
            short[] indexBuffer, int indexBufferLength) {
        return D3DNative.meshBuildGeometry(nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    boolean buildNativeGeometry(MemorySegment nativeHandle, float[] vertexBuffer, int vertexBufferLength,
            int[] indexBuffer, int indexBufferLength) {
        return D3DNative.meshBuildGeometry(nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    /** The native {@code D3DPhongMaterial*}, or {@link MemorySegment#NULL} when creation failed. */
    MemorySegment createD3DPhongMaterial() {
        return D3DNative.materialCreate(pContext);
    }

    // TODO: 3D - Should this be called dispose?
    void releaseD3DPhongMaterial(MemorySegment nativeHandle) {
        D3DNative.materialRelease(nativeHandle);
    }

    void setDiffuseColor(MemorySegment nativePhongMaterial, float r, float g, float b, float a) {
        D3DNative.materialSetDiffuseColor(nativePhongMaterial, r, g, b, a);
    }

    void setSpecularColor(MemorySegment nativePhongMaterial, boolean set, float r, float g, float b, float a) {
        D3DNative.materialSetSpecularColor(nativePhongMaterial, set, r, g, b, a);
    }

    /** @param nativeTexture the {@code D3DResource*} of the map's texture, or {@link MemorySegment#NULL} */
    void setMap(MemorySegment nativePhongMaterial, int mapType, MemorySegment nativeTexture) {
        D3DNative.materialSetMap(nativePhongMaterial, mapType, nativeTexture);
    }

    /** The native {@code D3DMeshView*}, or {@link MemorySegment#NULL} when creation failed. */
    MemorySegment createD3DMeshView(MemorySegment nativeMesh) {
        return D3DNative.meshviewCreate(pContext, nativeMesh);
    }

    // TODO: 3D - Should this be called dispose?
    void releaseD3DMeshView(MemorySegment nativeMeshView) {
        D3DNative.meshviewRelease(nativeMeshView);
    }

    void setCullingMode(MemorySegment nativeMeshView, int cullMode) {
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
        D3DNative.meshviewSetCullingMode(nativeMeshView, cm);
    }

    void setMaterial(MemorySegment nativeMeshView, MemorySegment nativePhongMaterial) {
        D3DNative.meshviewSetMaterial(nativeMeshView, nativePhongMaterial);
    }

    void setWireframe(MemorySegment nativeMeshView, boolean wireframe) {
         D3DNative.meshviewSetWireframe(nativeMeshView, wireframe);
    }

    void setAmbientLight(MemorySegment nativeMeshView, float r, float g, float b) {
        D3DNative.meshviewSetAmbientLight(nativeMeshView, r, g, b);
    }

    void setLight(MemorySegment nativeMeshView, int index, float x, float y, float z, float r, float g, float b,
            float w, float ca, float la, float qa, float isAttenuated, float maxRange, float dirX, float dirY,
            float dirZ, float innerAngle, float outerAngle, float falloff) {
        // the 18 parameters in the order the former nSetLight took them
        lightScratch.setAtIndex(JAVA_FLOAT, 0, x);
        lightScratch.setAtIndex(JAVA_FLOAT, 1, y);
        lightScratch.setAtIndex(JAVA_FLOAT, 2, z);
        lightScratch.setAtIndex(JAVA_FLOAT, 3, r);
        lightScratch.setAtIndex(JAVA_FLOAT, 4, g);
        lightScratch.setAtIndex(JAVA_FLOAT, 5, b);
        lightScratch.setAtIndex(JAVA_FLOAT, 6, w);
        lightScratch.setAtIndex(JAVA_FLOAT, 7, ca);
        lightScratch.setAtIndex(JAVA_FLOAT, 8, la);
        lightScratch.setAtIndex(JAVA_FLOAT, 9, qa);
        lightScratch.setAtIndex(JAVA_FLOAT, 10, isAttenuated);
        lightScratch.setAtIndex(JAVA_FLOAT, 11, maxRange);
        lightScratch.setAtIndex(JAVA_FLOAT, 12, dirX);
        lightScratch.setAtIndex(JAVA_FLOAT, 13, dirY);
        lightScratch.setAtIndex(JAVA_FLOAT, 14, dirZ);
        lightScratch.setAtIndex(JAVA_FLOAT, 15, innerAngle);
        lightScratch.setAtIndex(JAVA_FLOAT, 16, outerAngle);
        lightScratch.setAtIndex(JAVA_FLOAT, 17, falloff);
        D3DNative.meshviewSetLight(nativeMeshView, index, lightScratch);
    }

    @Override
    protected void renderQuads(float coordArray[], byte colorArray[], int numVertices) {
        int res = D3DNative.contextDrawIndexedQuads(pContext, coordArray, colorArray, numVertices);
        D3DContext.validate(res);
    }

    void renderMeshView(MemorySegment nativeMeshView, Graphics g) {

        // Support retina display by scaling the projViewTx and pass it to the shader.
        scratchTx = scratchTx.set(projViewTx);
        float pixelScaleFactorX = g.getPixelScaleFactorX();
        float pixelScaleFactorY = g.getPixelScaleFactorY();
        if (pixelScaleFactorX != 1.0 || pixelScaleFactorY != 1.0) {
            scratchTx.scale(pixelScaleFactorX, pixelScaleFactorY, 1.0);
        }

        // Set projection view matrix
        int res = D3DNative.contextSetProjViewMatrix(pContext, g.isDepthTest(), fillMatrix(scratchTx));
        validate(res);

        res = D3DNative.contextSetCameraPosition(pContext, cameraPos.x, cameraPos.y, cameraPos.z);
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

        D3DNative.meshviewRender(nativeMeshView);
    }

    @Override
    public void blit(RTTexture srcRTT, RTTexture dstRTT,
                     int srcX0, int srcY0, int srcX1, int srcY1,
                     int dstX0, int dstY0, int dstX1, int dstY1) {
        MemorySegment dstNativeHandle = dstRTT == null ? MemorySegment.NULL
                : ((D3DTexture)dstRTT).getNativeSourceHandle();
        MemorySegment srcNativeHandle = ((D3DTexture)srcRTT).getNativeSourceHandle();
        D3DNative.contextBlit(pContext, srcNativeHandle, dstNativeHandle,
                              srcX0, srcY0, srcX1, srcY1,
                              dstX0, dstY0, dstX1, dstY1);
    }
}
