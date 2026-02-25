/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class MTLContext extends BaseShaderContext {

    public static final int NUM_QUADS = PrismSettings.superShader ? 4096 : 256;

    private static final int MTL_COMPMODE_CLEAR   = 0;
    private static final int MTL_COMPMODE_SRC     = 1;
    private static final int MTL_COMPMODE_SRCOVER = 2;
    private static final int MTL_COMPMODE_DSTOUT  = 3;
    private static final int MTL_COMPMODE_ADD     = 4;

    private static final int MTL_SAMPLER_ADDR_MODE_NOP                   = -1;
    // CLAMP_TO_EDGE
    private static final int MTL_SAMPLER_ADDR_MODE_CLAMP_TO_EDGE         = 0; // MTLSamplerAddressModeClampToEdge
    private static final int MTL_SAMPLER_ADDR_MODE_MIRR_CLAMP_TO_EDGE    = 1; // MTLSamplerAddressModeMirrorClampToEdge
    // REPEAT
    private static final int MTL_SAMPLER_ADDR_MODE_REPEAT                = 2; // MTLSamplerAddressModeRepeat
    private static final int MTL_SAMPLER_ADDR_MODE_MIRR_REPEAT           = 3; // MTLSamplerAddressModeMirrorRepeat
    // CLAMP_TO_ZERO
    private static final int MTL_SAMPLER_ADDR_MODE_CLAMP_TO_ZERO         = 4; // MTLSamplerAddressModeClampToZero
    private static final int MTL_SAMPLER_ADDR_MODE_CLAMP_TO_BORDER_COLOR = 5; // MTLSamplerAddressModeClampToBorderColor

    private State state;
    private final long pContext;
    private MTLRTTexture renderTarget;
    private MTLResourceFactory resourceFactory;
    private MTLPipeline pipeline;

    private int targetWidth;
    private int targetHeight;

    private Vec3d cameraPos = new Vec3d();
    private static float rawMatrix[] = new float[16];
    private GeneralTransform3D worldTx = new GeneralTransform3D();
    private static final Affine3D scratchAffine3DTx = new Affine3D();
    private GeneralTransform3D scratchTx = new GeneralTransform3D(); // Column major matrix
    private GeneralTransform3D projViewTx = new GeneralTransform3D(); // Column major matrix

    private static double[] tempAdjustClipSpaceMat = new double[16];

    private static final ByteBuffer shaderLibBuffer;

    public final static int CULL_BACK  = 110;
    public final static int CULL_FRONT = 111;
    public final static int CULL_NONE  = 112;

    static {
        final String shaderLibName = "msl/jfxshaders.metallib";
        final Class<?> clazz = MTLContext.class;

        try {
            // Get the native shader library as a stream resource and read it into
            // an NIO ByteBuffer. This will be passed to the native MetalContext
            // initialization, which will load the shader library for each device.
            try (var in = new BufferedInputStream(clazz.getResourceAsStream(shaderLibName))) {
                byte[] data = in.readAllBytes();
                shaderLibBuffer = ByteBuffer.allocateDirect(data.length);
                shaderLibBuffer.put(data);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setRenderTargetTexture(MTLRTTexture rtt) {
        renderTarget = rtt;
    }

    public MTLRTTexture getRenderTargetTexture() {
        return renderTarget;
    }

    MTLContext(Screen screen, MTLResourceFactory factory) {
        super(screen, factory, NUM_QUADS);
        resourceFactory = factory;
        pContext = nInitialize(shaderLibBuffer);
    }

    @Override
    public MTLResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    protected void initState() {
        super.init();
        state = new State();
        nSetCompositeMode(getContextHandle(), MTL_COMPMODE_SRCOVER);
    }

    long getContextHandle() {
        return pContext;
    }

    /**
     * OpenGL projection transform use z-range of [-1, 1],
     * while Metal expects it to be [0, 1], so we need to adjust the matrix.
     * (comment from D3DContext, see JDK-8123305)
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
    protected State updateRenderTarget(RenderTarget target, NGCamera camera, boolean depthTest) {
        renderTarget = (MTLRTTexture)target;
        int res = nUpdateRenderTarget(pContext, renderTarget.getNativeHandle(), depthTest);

        if (res != -1) {
            resetLastClip(state);
        }

        targetWidth = target.getPhysicalWidth();
        targetHeight = target.getPhysicalHeight();

        // Validate the camera before getting its computed data
        if (camera instanceof NGDefaultCamera ngDefCam) {
            ngDefCam.validate(targetWidth, targetHeight);
            projViewTx = adjustClipSpace(camera.getProjViewTx(projViewTx));
        } else {
            projViewTx = adjustClipSpace(camera.getProjViewTx(projViewTx));
            double vw = camera.getViewWidth();
            double vh = camera.getViewHeight();
            if (targetWidth != vw || targetHeight != vh) {
                projViewTx.scale(vw / targetWidth, vh / targetHeight, 1.0);
            }
        }

        // Set projection view matrix
        nSetProjViewMatrix(pContext, depthTest,
            projViewTx.get(0),  projViewTx.get(1),  projViewTx.get(2),  projViewTx.get(3),
            projViewTx.get(4),  projViewTx.get(5),  projViewTx.get(6),  projViewTx.get(7),
            projViewTx.get(8),  projViewTx.get(9),  projViewTx.get(10), projViewTx.get(11),
            projViewTx.get(12), projViewTx.get(13), projViewTx.get(14), projViewTx.get(15));

        cameraPos = camera.getPositionInWorld(cameraPos);
        return state;
    }

    @Override
    protected void setTexture(int texUnit, Texture tex) {
        if (checkDisposed()) return;

        if (tex != null) tex.assertLocked();
        Texture[] lastTextures = state.getLastTextures();
        if (tex != lastTextures[texUnit]) {
            flushVertexBuffer();
            lastTextures[texUnit] = tex;
        }
        updateTexture(texUnit, tex);
    }

    @Override
    protected void updateTexture(int texUnit, Texture tex) {
        boolean linear;
        int wrapMode;
        if (tex != null) {
            linear = tex.getLinearFiltering();
            wrapMode = switch (tex.getWrapMode()) {
                case CLAMP_NOT_NEEDED -> MTL_SAMPLER_ADDR_MODE_NOP;

                case CLAMP_TO_EDGE,
                     CLAMP_TO_EDGE_SIMULATED,
                     CLAMP_TO_ZERO_SIMULATED -> MTL_SAMPLER_ADDR_MODE_CLAMP_TO_EDGE;

                case CLAMP_TO_ZERO -> MTL_SAMPLER_ADDR_MODE_CLAMP_TO_ZERO;

                case REPEAT,
                     REPEAT_SIMULATED -> MTL_SAMPLER_ADDR_MODE_REPEAT;
            };
            MTLShader.setTexture(texUnit, tex, linear, wrapMode);
        }
    }

    @Override
    protected void updateShaderTransform(Shader shader, BaseTransform xform) {
        if (xform == null) {
            xform = BaseTransform.IDENTITY_TRANSFORM;
        }

        scratchTx.set(projViewTx);
        final GeneralTransform3D perspectiveTransform = getPerspectiveTransformNoClone();
        if (perspectiveTransform.isIdentity()) {
            scratchTx = scratchTx.mul(xform);
        } else {
            scratchTx = scratchTx.mul(xform).mul(perspectiveTransform);
        }
        nSetTransform(pContext,
            scratchTx.get(0),  scratchTx.get(1),  scratchTx.get(2),  scratchTx.get(3),
            scratchTx.get(4),  scratchTx.get(5),  scratchTx.get(6),  scratchTx.get(7),
            scratchTx.get(8),  scratchTx.get(9),  scratchTx.get(10), scratchTx.get(11),
            scratchTx.get(12), scratchTx.get(13), scratchTx.get(14), scratchTx.get(15));
    }

    @Override
    protected void updateWorldTransform(BaseTransform xform) {
        worldTx.setIdentity();
        if ((xform != null) && (!xform.isIdentity())) {
            worldTx.mul(xform);
        }
    }

    @Override
    protected void updateClipRect(Rectangle clipRect) {
        if (clipRect == null || clipRect.isEmpty()) {
            nResetClipRect(pContext);
        } else {
            int x = clipRect.x;
            int y = clipRect.y;
            int width  = clipRect.width;
            int height = clipRect.height;
            nSetClipRect(pContext, x, y, width, height);
        }
    }

    @Override
    protected void updateCompositeMode(CompositeMode mode) {
        int mtlCompMode = switch (mode) {
            case CLEAR    -> MTL_COMPMODE_CLEAR;
            case SRC      -> MTL_COMPMODE_SRC;
            case SRC_OVER -> MTL_COMPMODE_SRCOVER;
            case DST_OUT  -> MTL_COMPMODE_DSTOUT;
            case ADD      -> MTL_COMPMODE_ADD;
        };
        nSetCompositeMode(getContextHandle(), mtlCompMode);
    }

    @Override
    public void blit(RTTexture srcRTT, RTTexture dstRTT,
                    int srcX0, int srcY0, int srcX1, int srcY1,
                    int dstX0, int dstY0, int dstX1, int dstY1) {
        // Verify whether we can avoid this blit when we are trying
        // to resolve MSAA texture into non-MSAA texture, because in case of Metal
        // we resolve the texture while rendering itself,
        // implement or change in future if necessary
        long dstNativeHandle = (dstRTT instanceof MTLTexture mtl) ? mtl.getNativeHandle() : 0L;
        long srcNativeHandle = (srcRTT instanceof MTLTexture mtl) ? mtl.getNativeHandle() : 0L;
        nBlit(pContext, srcNativeHandle, dstNativeHandle,
            srcX0, srcY0, srcX1, srcY1,
            dstX0, dstY0, dstX1, dstY1);
    }

    @Override
    protected void renderQuads(float[] coordArray, byte[] colorArray, int numVertices) {
        nDrawIndexedQuads(getContextHandle(), coordArray, colorArray, numVertices);
    }

    public void commitCurrentCommandBuffer() {
        nCommitCurrentCommandBuffer(pContext);
    }

    public long getMetalCommandQueue() {
        return nGetCommandQueue(pContext);
    }

    @Override
    protected void setDeviceParametersFor2D() {
        // There are no Metal rendering pipeline states changed as a
        // result of this call, hence the method is no-op.
        // But overriding the method here for any future reference.
    }

    @Override
    protected void setDeviceParametersFor3D() {
        // There are no Metal rendering pipeline states changed as a
        // result of this call, hence the method is no-op.
        // But overriding the method here for any future reference.
    }

    long createMTLMesh() {
        if (checkDisposed()) return 0;
        return nCreateMTLMesh(pContext);
    }

    void releaseMTLMesh(long nativeHandle) {
        nReleaseMTLMesh(pContext, nativeHandle);
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

    long createMTLPhongMaterial() {
        return nCreateMTLPhongMaterial(pContext);
    }

    void releaseMTLPhongMaterial(long nativeHandle) {
        nReleaseMTLPhongMaterial(pContext, nativeHandle);
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

    long createMTLMeshView(long nativeMesh) {
        return nCreateMTLMeshView(pContext, nativeMesh);
    }

    void releaseMTLMeshView(long nativeMeshView) {
        nReleaseMTLMeshView(pContext, nativeMeshView);
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

    void setLight(long nativeMeshView, int index, float x, float y, float z,
                  float r, float g, float b, float w,
                  float ca, float la, float qa, float isAttenuated, float maxRange,
                  float dirX, float dirY, float dirZ,
                  float innerAngle, float outerAngle, float falloff) {
        nSetLight(pContext, nativeMeshView, index, x, y, z, r, g, b, w,
                    ca, la, qa, isAttenuated, maxRange,
                    dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
    }

    void renderMeshView(long nativeMeshView, Graphics g) {
        // Support retina display by scaling the projViewTx and pass it to the shader.
        float pixelScaleFactorX = g.getPixelScaleFactorX();
        float pixelScaleFactorY = g.getPixelScaleFactorY();
        if (pixelScaleFactorX != 1.0 || pixelScaleFactorY != 1.0) {
            scratchTx = scratchTx.set(projViewTx);
            scratchTx.scale(pixelScaleFactorX, pixelScaleFactorY, 1.0);
            updateRawMatrix(scratchTx);
        } else {
            updateRawMatrix(projViewTx);
        }
        // printRawMatrix("Projection");
        // Set projection view matrix
        nSetProjViewMatrix(pContext, g.isDepthTest(),
            rawMatrix[0], rawMatrix[1], rawMatrix[2], rawMatrix[3],
            rawMatrix[4], rawMatrix[5], rawMatrix[6], rawMatrix[7],
            rawMatrix[8], rawMatrix[9], rawMatrix[10], rawMatrix[11],
            rawMatrix[12], rawMatrix[13], rawMatrix[14], rawMatrix[15]);

        nSetCameraPosition(pContext, cameraPos.x, cameraPos.y, cameraPos.z);

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

        updateRawMatrix(worldTx);
        // printRawMatrix("World");
        nSetWorldTransform(pContext,
            rawMatrix[0], rawMatrix[1], rawMatrix[2], rawMatrix[3],
            rawMatrix[4], rawMatrix[5], rawMatrix[6], rawMatrix[7],
            rawMatrix[8], rawMatrix[9], rawMatrix[10], rawMatrix[11],
            rawMatrix[12], rawMatrix[13], rawMatrix[14], rawMatrix[15]);
        nRenderMeshView(pContext, nativeMeshView);
    }

    private void printRawMatrix(String mesg) {
        System.err.println(mesg + " = ");
        for (int i = 0; i < 4; i++) {
            System.err.println(rawMatrix[i] + ", " + rawMatrix[i+4]
                + ", " + rawMatrix[i+8] + ", " + rawMatrix[i+12]);
        }
    }

    private void updateRawMatrix(GeneralTransform3D src) {
        rawMatrix[0]  = (float)src.get(0); // Scale X
        rawMatrix[1]  = (float)src.get(4); // Shear Y
        rawMatrix[2]  = (float)src.get(8);
        rawMatrix[3]  = (float)src.get(12);
        rawMatrix[4]  = (float)src.get(1); // Shear X
        rawMatrix[5]  = (float)src.get(5); // Scale Y
        rawMatrix[6]  = (float)src.get(9);
        rawMatrix[7]  = (float)src.get(13);
        rawMatrix[8]  = (float)src.get(2);
        rawMatrix[9]  = (float)src.get(6);
        rawMatrix[10] = (float)src.get(10);
        rawMatrix[11] = (float)src.get(14);
        rawMatrix[12] = (float)src.get(3);  // Translate X
        rawMatrix[13] = (float)src.get(7);  // Translate Y
        rawMatrix[14] = (float)src.get(11);
        rawMatrix[15] = (float)src.get(15);
    }

    public void disposeShader(long nMetalShaderRef) {
        nDisposeShader(nMetalShaderRef);
    }

    public boolean isCurrentRTT(long texPtr) {
        return nIsCurrentRTT(pContext, texPtr);
    }

    @Override
    public void dispose() {
        disposeLCDBuffer();
        nRelease(pContext);
        state = null;
        super.dispose();
    }

    // Native methods

    private static native long nInitialize(ByteBuffer shaderLibPathStr);
    private static native void nCommitCurrentCommandBuffer(long context);
    private static native long nGetCommandQueue(long context);
    private static native void nDrawIndexedQuads(long context, float coords[], byte volors[], int numVertices);
    private static native int  nUpdateRenderTarget(long context, long texPtr, boolean depthTest);

    private static native void nSetProjViewMatrix(long pContext, boolean isOrtho,
                                                    double m00, double m01, double m02, double m03,
                                                    double m10, double m11, double m12, double m13,
                                                    double m20, double m21, double m22, double m23,
                                                    double m30, double m31, double m32, double m33);
    private static native void nSetTransform(long pContext,
                                                  double m00, double m01, double m02, double m03,
                                                  double m10, double m11, double m12, double m13,
                                                  double m20, double m21, double m22, double m23,
                                                  double m30, double m31, double m32, double m33);

    private static native void nSetCompositeMode(long context, int mode);
    private static native void nResetClipRect(long context);
    private static native void nSetClipRect(long context, int x, int y, int width, int height);

    private static native void nSetWorldTransformToIdentity(long pContext);
    private static native void nSetWorldTransform(long pContext,
                                                  double m00, double m01, double m02, double m03,
                                                  double m10, double m11, double m12, double m13,
                                                  double m20, double m21, double m22, double m23,
                                                  double m30, double m31, double m32, double m33);
    private static native void nSetCameraPosition(long pContext, double x, double y, double z);
    private static native long nCreateMTLMesh(long pContext);
    private static native void nReleaseMTLMesh(long pContext, long nativeHandle);
    private static native boolean nBuildNativeGeometryShort(long pContext, long nativeHandle,
                                                            float[] vertexBuffer, int vertexBufferLength, short[] indexBuffer, int indexBufferLength);
    private static native boolean nBuildNativeGeometryInt(long pContext, long nativeHandle,
                                                          float[] vertexBuffer, int vertexBufferLength, int[] indexBuffer, int indexBufferLength);
    private static native long nCreateMTLPhongMaterial(long pContext);
    private static native void nReleaseMTLPhongMaterial(long pContext, long nativeHandle);
    private static native void nSetDiffuseColor(long pContext, long nativePhongMaterial,
                                                float r, float g, float b, float a);
    private static native void nSetSpecularColor(long pContext, long nativePhongMaterial,
                                                 boolean set, float r, float g, float b, float a);
    private static native void nSetMap(long pContext, long nativePhongMaterial,
                                       int mapType, long texID);
    private static native long nCreateMTLMeshView(long pContext, long nativeMesh);
    private static native void nReleaseMTLMeshView(long pContext, long nativeHandle);
    private static native void nSetCullingMode(long pContext, long nativeMeshView,
                                               int cullingMode);
    private static native void nSetMaterial(long pContext, long nativeMeshView,
                                            long nativePhongMaterialInfo);
    private static native void nSetWireframe(long pContext, long nativeMeshView,
                                             boolean wireframe);
    private static native void nSetAmbientLight(long pContext, long nativeMeshView,
                                                float r, float g, float b);
    private static native void nSetLight(long pContext, long nativeMeshView, int index,
                                         float x, float y, float z,
                                         float r, float g, float b, float w,
                                         float ca, float la, float qa,
                                         float isAttenuated, float maxRange,
                                         float dirX, float dirY, float dirZ,
                                         float innerAngle, float outerAngle, float falloff);
    private static native void nRenderMeshView(long pContext, long nativeMeshView);

    private static native void nBlit(long pContext, long nSrcRTT, long nDstRTT,
                                     int srcX0, int srcY0, int srcX1, int srcY1,
                                     int dstX0, int dstY0, int dstX1, int dstY1);

    private static native void nRelease(long pContext);

    private static native boolean nIsCurrentRTT(long pContext, long texPtr);
    private static native void nDisposeShader(long nMetalShaderRef);
}
