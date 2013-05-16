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

package com.sun.prism.es2;

import com.sun.glass.ui.Screen;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.CompositeMode;
import com.sun.prism.Material;
import com.sun.prism.PixelFormat;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.camera.PrismDefaultCamera;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import java.util.HashMap;

class ES2Context extends BaseShaderContext {

    // Temporary variables
    private static GeneralTransform3D scratchTx = new GeneralTransform3D();
    private static final GeneralTransform3D flipTx = new GeneralTransform3D();
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
    private ES2RenderingContext currentRenderingContext = null;
    private int indexBuffer = 0;
    private int shaderProgram;

    public static final int NUM_QUADS = 256;

    private static ES2VertexBuffer createVertexBuffer() {
        return new ES2VertexBuffer(NUM_QUADS);
    }

    ES2Context(Screen screen, ShaderFactory factory) {
        super(screen, factory, createVertexBuffer());
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
        ES2VertexBuffer vb = (ES2VertexBuffer) getVertexBuffer();
        vb.enableVertexAttributes(glContext);

        quadIndices = vb.genQuadsIndexBuffer(NUM_QUADS);
        setIndexBuffer(quadIndices);
        state = new State();

        // JIRA: RT-21739
        // TODO: This is a temporary mechanism to work well with Glass on Mac due
        // to the CALayer work. Need to be removed in the early future for 3.0
        if (PlatformUtil.isMac() || PlatformUtil.isIOS()) {
           HashMap devDetails = (HashMap) ES2Pipeline.getInstance().getDeviceDetails();
           ES2Pipeline.glFactory.updateDeviceDetails(devDetails, glContext);
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

    GLDrawable getDummyDrawable() {
        return dummyGLDrawable;
    }

    GLPixelFormat getPixelFormat() {
        return pixelFormat;
    }

    ES2Shader getPhongShader(ES2MeshView meshView) {
        return ES2PhongShader.getShader(meshView, this);
    }

    void setCurrentRenderingContext(ES2RenderingContext rc, GLDrawable drawable) {
        if ((rc != null) && (drawable == null)) {
            System.err.println("Warning: ES2Context.setCurrentRenderingContext: "
                    + "rc = " + rc + ", drawable = " + drawable);
        }
        currentRenderingContext = rc;
        makeCurrent(drawable);
     }

    ES2RenderingContext getCurrentRenderingContext() {
        return currentRenderingContext;
    }

    // JIRA: RT-21738
    // TODO: If we can't resolve this platform specific treatment code
    // by 3.0, we need to refactor it to platform specific project
    private int savedFBO = 0;
    private void makeCurrent(GLDrawable drawable) {
        if (PlatformUtil.isMac() || PlatformUtil.isIOS()) {
            if (drawable != currentDrawable) {
                if (drawable == dummyGLDrawable) {
                    // Need to restore FBO to Glass' boundFBO
                    glContext.bindFBO(savedFBO);
                } else {
                    savedFBO = glContext.getBoundFBO();
                    glContext.makeCurrent(drawable);
                }
                currentDrawable = drawable;
            }
        } else { // Linux and Windows
            if (drawable != currentDrawable) {
                glContext.makeCurrent(drawable);
                // Need to restore FBO to on screen framebuffer
                glContext.bindFBO(0);
                currentDrawable = drawable;
            }
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
    public boolean isEdgeSmoothingSupported(PixelFormat format) {
        if (ES2Pipeline.isEmbededDevice) {
            // on an embeded device, the smoothing trick only works if the texture
            // contains an alpha channel
            return !format.isOpaque() && format.isRGB();
        } else {
            // on desktop, the smoothing trick is supported for any rgb format
            return format.isRGB();
        }
    }

    @Override
    protected void releaseRenderTarget() {
        currentTarget = null;
        super.releaseRenderTarget();
    }

    @Override
    protected State updateRenderTarget(RenderTarget target, PrismCameraImpl camera,
            boolean depthTest) {
        int fboID = ((ES2RenderTarget)target).getFboID();
        glContext.bindFBO(fboID);

        if (depthTest && target instanceof ES2RTTexture) {
            // Attach a depth buffer to the currently bound FBO
            ((ES2RTTexture) target).attachDepthBuffer(this);
        }

        // update viewport
        int x = target.getContentX();
        int y = target.getContentY();
        int w = target.getContentWidth();
        int h = target.getContentHeight();
        glContext.updateViewportAndDepthTest(x, y, w, h, depthTest);

        if (camera instanceof PrismDefaultCamera) {
            // update projection matrix; this will be uploaded to the shader
            // along with the modelview matrix in updateShaderTransform()
            ((PrismDefaultCamera) camera).validate(w, h);
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
        updateRawMatrix(scratchTx.mul(xform));

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

        ES2VertexBuffer vb = (ES2VertexBuffer) getVertexBuffer();

        // Bind vertex attributes and index buffer
        vb.enableVertexAttributes(glContext);
        setIndexBuffer(quadIndices);
    }

    @Override
    public void setDeviceParametersFor3D() {

        ES2VertexBuffer vb = (ES2VertexBuffer) getVertexBuffer();

        // unbind vertex attributes and index buffer
        vb.disableVertexAttributes(glContext);
        glContext.setDeviceParametersFor3D();
    }

    long createES2Mesh() {
        return glContext.createES2Mesh();
    }

    // TODO: 3D - Should this be called dispose?
    void releaseES2Mesh(long nativeHandle) {
        glContext.releaseES2Mesh(nativeHandle);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer, int[] indexBuffer) {
        return glContext.buildNativeGeometry(nativeHandle, vertexBuffer, indexBuffer);
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

    void setMap(long nativeHandle, int mapType, int texID,
            boolean isSpecularAlpha, boolean isBumpAlpha) {
        glContext.setMap(nativeHandle, mapType, texID, isSpecularAlpha, isBumpAlpha);
    }

    long createES2MeshView(ES2Mesh mesh) {
        return glContext.createES2MeshView(mesh.getNativeHandle());
    }

    // TODO: 3D - Should this be called dispose?
    void releaseES2MeshView(long nativeHandle) {
        glContext.releaseES2MeshView(nativeHandle);
    }

    void setCullingMode(long nativeHandle, int cullingMode) {
        // TODO: 3D - compute determinant whenever projViewTx or worldTx changes.
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

    void renderMeshView(long nativeHandle, BaseTransform xform, ES2MeshView meshView) {

        ES2Shader shader = (ES2Shader) getPhongShader(meshView);
        setShaderProgram(shader.getProgramObject());

        updateRawMatrix(projViewTx);
        shader.setMatrix("viewProjectionMatrix", rawMatrix);
        shader.setConstant("camPos", (float) cameraPos.x,
                (float) cameraPos.y, (float)cameraPos.z);

        updateWorldTransform(xform);
        updateRawMatrix(worldTx);

        shader.setMatrix("worldMatrix", rawMatrix);
//        printRawMatrix("worldMatrix");

        ES2PhongShader.setShaderParamaters(shader, meshView, this);

        glContext.renderMeshView(nativeHandle);
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
