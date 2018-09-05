/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGDefaultCamera;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.RTTexture;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;

class ES2Context extends BaseShaderContext {

    // Temporary variables
    private static GeneralTransform3D scratchTx = new GeneralTransform3D();
    private static final GeneralTransform3D flipTx = new GeneralTransform3D();
    private static final Affine3D scratchAffine3DTx = new Affine3D();
    // contains the combined projection/modelview matrix (elements 0-15)
    private static float rawMatrix[] = new float[GLContext.NUM_MATRIX_ELEMENTS];

    private GeneralTransform3D projViewTx = new GeneralTransform3D();
    private GeneralTransform3D worldTx = new GeneralTransform3D();
    private Vec3d cameraPos = new Vec3d();

    private RenderTarget currentTarget;
    private final GLContext glContext;
    private final GLDrawable dummyGLDrawable;
    private final GLPixelFormat pixelFormat;
    private State state;
    private int quadIndices;
    // The drawable that is current to the glContext
    private GLDrawable currentDrawable = null;
    private int indexBuffer = 0;
    private int shaderProgram;

    public static final int NUM_QUADS = PrismSettings.superShader ? 4096 : 256;

    ES2Context(Screen screen, ShaderFactory factory) {
        super(screen, factory, NUM_QUADS);
        GLFactory glF = ES2Pipeline.glFactory;

        // NOTE: There is issue with the returned value of getNativeScreen.
        // HMonitor (Windows), GTKMonitor index (Linux) ...
        // We would prefer HDC (Windows) and screen number(index) (Linux)
        pixelFormat =
                glF.createGLPixelFormat(screen.getNativeScreen(),
                ES2Pipeline.pixelFormatAttributes);

        dummyGLDrawable = glF.createDummyGLDrawable(pixelFormat);
        glContext = glF.createGLContext(dummyGLDrawable, pixelFormat,
                glF.getShareContext(), PrismSettings.isVsyncEnabled);
        makeCurrent(dummyGLDrawable);

        glContext.enableVertexAttributes();
        quadIndices = genQuadsIndexBuffer(NUM_QUADS);
        setIndexBuffer(quadIndices);
        state = new State();
    }

    static short [] getQuadIndices16bit(int numQuads) {
        short data[] = new short[numQuads * 6];

        for (int i = 0; i != numQuads; ++i) {
            int vtx = i * 4;
            int idx = i * 6;
            data[idx+0] = (short) (vtx+0);
            data[idx+1] = (short) (vtx+1);
            data[idx+2] = (short) (vtx+2);

            data[idx+3] = (short) (vtx+2);
            data[idx+4] = (short) (vtx+1);
            data[idx+5] = (short) (vtx+3);
        }

        return data;
    }

    int genQuadsIndexBuffer(int numQuads) {
        if (numQuads * 6 > 0x10000)
            throw new IllegalArgumentException("vertex indices overflow");

        return glContext.createIndexBuffer16(getQuadIndices16bit(numQuads));
    }

    final void clearContext() {
        if (currentDrawable != null) {
            currentDrawable.swapBuffers(glContext);
        }
    }

    final void setIndexBuffer(int ib) {
        if (indexBuffer != ib) {
            glContext.setIndexBuffer(indexBuffer = ib);
        }
    }

    GLContext getGLContext() {
        return glContext;
    }

    GLPixelFormat getPixelFormat() {
        return pixelFormat;
    }

    ES2Shader getPhongShader(ES2MeshView meshView) {
        return ES2PhongShader.getShader(meshView, this);
    }

    void makeCurrent(GLDrawable drawable) {
        if (drawable == null) {
            drawable = dummyGLDrawable;
        }
        if (drawable != currentDrawable) {
            glContext.makeCurrent(drawable);
            // Need to restore FBO to on screen framebuffer
            glContext.bindFBO(0);
            currentDrawable = drawable;
        }
    }

    /**
     * Called from ES2Graphics.updateRenderTarget() in response to a window
     * resize event.  This method ensures that the context is made current
     * after the resize event, which is required on Mac OS X in order to
     * force a call to [NSOpenGLContext update].
     */
    void forceRenderTarget(ES2Graphics g) {
        updateRenderTarget(g.getRenderTarget(), g.getCameraNoClone(),
                g.isDepthTest() && g.isDepthBuffer());
    }

    int getShaderProgram() {
        return shaderProgram;
    }

    // Forcibly sets the current shader program to the given object.
    void setShaderProgram(int progid) {
        shaderProgram = progid;
        glContext.setShaderProgram(progid);
    }

    // Sets the current shader program to the given object only if it was
    // not already the current program.
    void updateShaderProgram(int progid) {
        if (progid != shaderProgram) {
            setShaderProgram(progid);
        }
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void releaseRenderTarget() {
        currentTarget = null;
        super.releaseRenderTarget();
    }

    @Override
    protected State updateRenderTarget(RenderTarget target, NGCamera camera,
            boolean depthTest) {
        int fboID = ((ES2RenderTarget)target).getFboID();
        glContext.bindFBO(fboID);

        boolean msaa = false;
        if (target instanceof ES2RTTexture) {
            // Attach a depth buffer to the currently bound FBO
            ES2RTTexture rtTarget = (ES2RTTexture)target;
            msaa = rtTarget.isMSAA();
            if (depthTest) {
                rtTarget.attachDepthBuffer(this);
            }
        }

        // update viewport
        int x = target.getContentX();
        int y = target.getContentY();
        int w = target.getContentWidth();
        int h = target.getContentHeight();
        glContext.updateViewportAndDepthTest(x, y, w, h, depthTest);
        glContext.updateMSAAState(msaa);

        if (camera instanceof NGDefaultCamera) {
            // update projection matrix; this will be uploaded to the shader
            // along with the modelview matrix in updateShaderTransform()
            ((NGDefaultCamera) camera).validate(w, h);
            scratchTx = camera.getProjViewTx(scratchTx);
        } else {
            scratchTx = camera.getProjViewTx(scratchTx);
            // TODO: verify that this is the right solution. There may be
            // other use-cases where rendering needs different viewport size.
            double vw = camera.getViewWidth();
            double vh = camera.getViewHeight();
            if (w != vw || h != vh) {
                scratchTx.scale(vw / w, vh / h, 1.0);
            }
        }

        if (target instanceof ES2RTTexture) {
            // Compute a flipped version of projViewTx
            projViewTx.set(flipTx);
            projViewTx.mul(scratchTx);
        } else {
            projViewTx.set(scratchTx);
        }

        // update camera position; this will be uploaded to the shader
        // when we switch to 3D state
        cameraPos = camera.getPositionInWorld(cameraPos);

        currentTarget = target;
        return state;
    }

    @Override
    protected void updateTexture(int texUnit, Texture tex) {
        glContext.updateActiveTextureUnit(texUnit);

        if (tex == null) {
            glContext.updateBoundTexture(0);
        } else {
            ES2Texture es2Tex = (ES2Texture)tex;
            glContext.updateBoundTexture(es2Tex.getNativeSourceHandle());
            es2Tex.updateWrapState();
            es2Tex.updateFilterState();
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
            updateRawMatrix(scratchTx.mul(xform));
        } else {
            updateRawMatrix(scratchTx.mul(xform).mul(perspectiveTransform));
        }

        ES2Shader es2shader = (ES2Shader) shader;
        es2shader.setMatrix("mvpMatrix", rawMatrix);
//        printRawMatrix("mvpMatrix");

        if (es2shader.isPixcoordUsed()) {
            // the gl_FragCoord variable is in window coordinates and
            // does not take the viewport origin into account (or the fact
            // that we do a y-flip of the projection matrix in the case
            // of onscreen windows for that matter); we need to update
            // the special jsl_pixCoordOffset param here so that the shader
            // can continue to treat pixcoord as if it were in the range
            // [0,0] to [contentWidth,contentHeight] of the destination surface
            float xoff = currentTarget.getContentX();
            float yoff = currentTarget.getContentY();
            float yinv, yflip;
            if (currentTarget instanceof ES2SwapChain) {
                // there is a y-flip in this case
                yinv = currentTarget.getPhysicalHeight();
                yflip = 1f;
            } else {
                // no y-flip for RTTextures
                yinv = 0f;
                yflip = -1f;
            }
            shader.setConstant("jsl_pixCoordOffset", xoff, yoff, yinv, yflip);
        }
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
            glContext.scissorTest(false, 0, 0, 0, 0);
        } else {
            // the scissor rectangle is specified using the lower-left
            // origin of the clip region (in the framebuffer's coordinate
            // space), so we must account for the x/y offsets of the
            // destination surface, and use a flipped y origin when rendering
            // to an ES2SwapChain
            int w = clipRect.width;
            int h = clipRect.height;
            int x = currentTarget.getContentX();
            int y = currentTarget.getContentY();
            if (currentTarget instanceof ES2RTTexture) {
                x += clipRect.x;
                y += clipRect.y;
            } else {
                int dsth = currentTarget.getPhysicalHeight();
                x += clipRect.x;
                y += dsth - (clipRect.y + h);
            }
            glContext.scissorTest(true, x, y, w, h);
        }
    }

    @Override
    protected void updateCompositeMode(CompositeMode mode) {
        switch (mode) {
            case CLEAR:
                glContext.blendFunc(GLContext.GL_ZERO, GLContext.GL_ZERO);
                break;
            case SRC:
                glContext.blendFunc(GLContext.GL_ONE, GLContext.GL_ZERO);
                break;
            case SRC_OVER:
                glContext.blendFunc(GLContext.GL_ONE, GLContext.GL_ONE_MINUS_SRC_ALPHA);
                break;
            case DST_OUT:
                glContext.blendFunc(GLContext.GL_ZERO, GLContext.GL_ONE_MINUS_SRC_ALPHA);
                break;
            case ADD:
                glContext.blendFunc(GLContext.GL_ONE, GLContext.GL_ONE);
                break;
            default:
                throw new InternalError("Unrecognized composite mode: " + mode);
        }
    }

    @Override
    public void setDeviceParametersFor2D() {
        // invalidate cache data
        indexBuffer = 0;
        shaderProgram = 0;
        glContext.setDeviceParametersFor2D();

        // Bind vertex attributes and index buffer
        glContext.enableVertexAttributes();
        setIndexBuffer(quadIndices);
    }

    @Override
    public void setDeviceParametersFor3D() {
        // unbind vertex attributes and index buffer
        glContext.disableVertexAttributes();
        glContext.setDeviceParametersFor3D();
    }

    long createES2Mesh() {
        return glContext.createES2Mesh();
    }

    // TODO: 3D - Should this be called dispose?
    void releaseES2Mesh(long nativeHandle) {
        glContext.releaseES2Mesh(nativeHandle);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer,
            int vertexBufferLength, short[] indexBuffer, int indexBufferLength) {
        return glContext.buildNativeGeometry(nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer,
            int vertexBufferLength, int[] indexBuffer, int indexBufferLength) {
        return glContext.buildNativeGeometry(nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    long createES2PhongMaterial() {
        return glContext.createES2PhongMaterial();
    }

    // TODO: 3D - Should this be called dispose?
    void releaseES2PhongMaterial(long nativeHandle) {
        glContext.releaseES2PhongMaterial(nativeHandle);
    }

    void setSolidColor(long nativeHandle, float r, float g, float b, float a) {
        glContext.setSolidColor(nativeHandle, r, g, b, a);
    }

    void setMap(long nativeHandle, int mapType, int texID) {
        glContext.setMap(nativeHandle, mapType, texID);
    }

    long createES2MeshView(ES2Mesh mesh) {
        return glContext.createES2MeshView(mesh.getNativeHandle());
    }

    // TODO: 3D - Should this be called dispose?
    void releaseES2MeshView(long nativeHandle) {
        glContext.releaseES2MeshView(nativeHandle);
    }

    void setCullingMode(long nativeHandle, int cullingMode) {
        // NOTE: Native code has set clockwise order as front-facing
        glContext.setCullingMode(nativeHandle, cullingMode);
    }

    void setMaterial(long nativeHandle, Material material) {
        ES2PhongMaterial es2Material = (ES2PhongMaterial)material;

        glContext.setMaterial(nativeHandle,
                (es2Material).getNativeHandle());
    }

    void setWireframe(long nativeHandle, boolean wireframe) {
       glContext.setWireframe(nativeHandle, wireframe);
    }

    void setAmbientLight(long nativeHandle, float r, float g, float b) {
        glContext.setAmbientLight(nativeHandle, r, g, b);
    }

    void setPointLight(long nativeHandle, int index, float x, float y, float z, float r, float g, float b, float w) {
        glContext.setPointLight(nativeHandle, index, x, y, z, r, g, b, w);
    }

    @Override
    public void blit(RTTexture srcRTT, RTTexture dstRTT,
                     int srcX0, int srcY0, int srcX1, int srcY1,
                     int dstX0, int dstY0, int dstX1, int dstY1)
    {
        // If dstRTT is null then will blit to currently bound fbo
        int dstFboID = dstRTT == null ? 0 : ((ES2RTTexture)dstRTT).getFboID();
        int srcFboID = ((ES2RTTexture)srcRTT).getFboID();
        glContext.blitFBO(srcFboID, dstFboID,
                          srcX0, srcY0, srcX1, srcY1,
                          dstX0, dstY0, dstX1, dstY1);
    }

    void renderMeshView(long nativeHandle, Graphics g, ES2MeshView meshView) {

        ES2Shader shader = (ES2Shader) getPhongShader(meshView);
        setShaderProgram(shader.getProgramObject());

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
        shader.setMatrix("viewProjectionMatrix", rawMatrix);
        shader.setConstant("camPos", (float) cameraPos.x,
                (float) cameraPos.y, (float)cameraPos.z);

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

        shader.setMatrix("worldMatrix", rawMatrix);
//        printRawMatrix("worldMatrix");

        ES2PhongShader.setShaderParamaters(shader, meshView, this);

        glContext.renderMeshView(nativeHandle);
    }

    @Override
    protected void renderQuads(float coordArray[], byte colorArray[], int numVertices) {
        glContext.drawIndexedQuads(coordArray, colorArray, numVertices);
    }

    void printRawMatrix(String mesg) {
        System.err.println(mesg + " = ");
        for (int i = 0; i < 4; i++) {
            System.err.println(rawMatrix[i] + ", " + rawMatrix[i+4]
                    + ", " + rawMatrix[i+8] + ", " + rawMatrix[i+12]);
        }
    }

    // Need to transpose the matrix because OpenGL stores its matrix in
    // column major (though matrix computation is done in row major)
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

    static {
        BaseTransform tx = Affine2D.getScaleInstance(1.0, -1.0);
        flipTx.setIdentity();
        flipTx.mul(tx);
    }
}
