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

package com.sun.prism.es2;

import com.sun.glass.ui.Screen;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackRenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.PrismTrace;

import java.nio.Buffer;

class ES2RTTexture extends ES2Texture<ES2RTTextureData>
        implements ES2RenderTarget, RTTexture, ReadbackRenderTarget
{

    private boolean opaque;

    private ES2RTTexture(ES2Context context, ES2TextureResource<ES2RTTextureData> resource,
                         WrapMode wrapMode,
                         int physicalWidth, int physicalHeight,
                         int contentX, int contentY,
                         int contentWidth, int contentHeight,
                         int maxContentWidth, int maxContentHeight)
    {
        super(context, resource, PixelFormat.BYTE_BGRA_PRE, wrapMode,
                physicalWidth, physicalHeight,
                contentX, contentY,
                contentWidth, contentHeight,
                maxContentWidth, maxContentHeight, false);
        PrismTrace.rttCreated(resource.getResource().getFboID(),
                              physicalWidth, physicalHeight,
                              PixelFormat.BYTE_BGRA_PRE.getBytesPerPixelUnit());
        this.opaque = false;
    }

    /**
     * Attach a depth buffer to the currently bound FBO
     * @param context current context
     */
    void attachDepthBuffer(ES2Context context) {
        // Return immediately if this RTT already has a depth buffer
        ES2RTTextureData texData = resource.getResource();
        int dbID = texData.getDepthBufferID();
        if (dbID != 0) {
            return;
        }
        int msaaSamples = isMSAA() ? context.getGLContext().getSampleSize() : 0;
        dbID = context.getGLContext().createDepthBuffer(getPhysicalWidth(),
                getPhysicalHeight(), msaaSamples);

        // Add to disposer record so that we can cleanup the depth buffer when
        // this RTT is destroyed.
        texData.setDepthBufferID(dbID);
    }

    /**
     * Create and attach a color multisample render buffer to current FBO
     * @param context current context
     */
    private void createAndAttachMSAABuffer(ES2Context context) {
        // Assert isMSAA() must be true
        ES2RTTextureData texData = resource.getResource();
        int rbID = texData.getMSAARenderBufferID();
        if (rbID != 0) {
            return;
        }
        GLContext glContext = context.getGLContext();
        rbID = glContext.createRenderBuffer(getPhysicalWidth(),
                getPhysicalHeight(), glContext.getSampleSize());

        // Add to disposer record so that we can cleanup the MSAA
        // render buffer when this RTT is destroyed.
        texData.setMSAARenderBufferID(rbID);
    }

    static int getCompatibleDimension(ES2Context context, int dim, WrapMode wrapMode) {
        GLContext glContext = context.getGLContext();
        boolean pad;
        switch (wrapMode) {
            case CLAMP_NOT_NEEDED:
                pad = false;
                break;
            case CLAMP_TO_ZERO:
                pad = !glContext.canClampToZero();
                break;
            default:
            case CLAMP_TO_EDGE:
            case REPEAT:
                throw new IllegalArgumentException("wrap mode not supported for RT textures: "+wrapMode);
            case CLAMP_TO_EDGE_SIMULATED:
            case CLAMP_TO_ZERO_SIMULATED:
            case REPEAT_SIMULATED:
                throw new IllegalArgumentException("Cannot request simulated wrap mode: "+wrapMode);
        }

        int paddedDim = pad ? dim + 2 : dim;

        int maxSize = glContext.getMaxTextureSize();
        int texDim;
        if (glContext.canCreateNonPowTwoTextures()) {
            texDim = (paddedDim <= maxSize) ? paddedDim : 0;
        } else {
            texDim = nextPowerOfTwo(paddedDim, maxSize);
        }

        if (texDim == 0) {
            throw new RuntimeException(
                    "Requested texture dimension (" + dim + ") "
                    + "requires dimension (" + texDim + ") "
                    + "that exceeds maximum texture size (" + maxSize + ")");
        }

        // make sure the texture is not smaller than minimum size
        texDim = Math.max(texDim, PrismSettings.minRTTSize);

        // Note that ES2Context will set the viewport to the content
        // region of a RenderTarget (to ensure that we don't render into
        // the padded area of transparent pixels, if present).  Since
        // RTTextures are frequently recycled (i.e., reused by Decora) it
        // is imperative that we initialize the content region of the
        // RTTexture to be the physical size of the FBO modulo the padded
        // region.  (Suppose the caller asks for a 110x220 RTTexture, but
        // nonpow2 textures aren't supported; in this case, we will actually
        // create a 128x256 FBO.  If later that RTTexture gets reused for
        // a caller expecting to use 126x240 pixels, the viewport will be
        // setup correctly because it will set to the content region, or
        // 128x256 in this case, assuming no padding.)
        return pad ? texDim - 2 : texDim;
    }

    static ES2RTTexture create(ES2Context context, int w, int h, WrapMode wrapMode, boolean msaa) {
        // Normally we would use GL_CLAMP_TO_BORDER with a transparent border
        // color to implement our CLAMP_TO_ZERO edge mode, but unfortunately
        // that mode is not available in OpenGL ES.  The workaround is to pad
        // the fbo with 1 row/column of transparent pixels on each side if the
        // caller requires and requests it, and we have to be careful to use
        // the getContentX/Y/Width/Height() methods so that we access only the
        // content area of the texture.
        // The downside of this approach is that when npot textures are not
        // supported, the padding may cause us to cross the power-of-two
        // threshold more easily and therefore waste more VRAM.
        // Note that only CLAMP_NOT_NEEDED and CLAMP_TO_ZERO are supported
        // for RT textures.  The other modes could be supported on desktop
        // platforms, but emulating them on non-GL2 platforms would be
        // prohibitively difficult.

        GLContext glContext = context.getGLContext();
        boolean pad;
        switch (wrapMode) {
            case CLAMP_NOT_NEEDED:
                pad = false;
                break;
            case CLAMP_TO_ZERO:
                pad = !glContext.canClampToZero();
                break;
            default:
            case CLAMP_TO_EDGE:
            case REPEAT:
                throw new IllegalArgumentException("wrap mode not supported for RT textures: "+wrapMode);
            case CLAMP_TO_EDGE_SIMULATED:
            case CLAMP_TO_ZERO_SIMULATED:
            case REPEAT_SIMULATED:
                throw new IllegalArgumentException("Cannot request simulated wrap mode: "+wrapMode);
        }

        int contentX, contentY;
        int paddedW, paddedH;
        if (pad) {
            contentX = 1;
            contentY = 1;
            paddedW = w + 2;
            paddedH = h + 2;
            wrapMode = wrapMode.simulatedVersion();
        } else {
            contentX = 0;
            contentY = 0;
            paddedW = w;
            paddedH = h;
        }

        int maxSize = glContext.getMaxTextureSize();
        int texWidth, texHeight;
        if (glContext.canCreateNonPowTwoTextures()) {
            texWidth = (paddedW <= maxSize) ? paddedW : 0;
            texHeight = (paddedH <= maxSize) ? paddedH : 0;
        } else {
            texWidth = nextPowerOfTwo(paddedW, maxSize);
            texHeight = nextPowerOfTwo(paddedH, maxSize);
        }

        if (texWidth == 0 || texHeight == 0) {
            throw new RuntimeException(
                    "Requested texture dimensions (" + w + "x" + h + ") "
                    + "require dimensions (" + texWidth + "x" + texHeight + ") "
                    + "that exceed maximum texture size (" + maxSize + ")");
        }

        // make sure the texture is not smaller than minimum size
        int minSize = PrismSettings.minRTTSize;
        texWidth = Math.max(texWidth, minSize);
        texHeight = Math.max(texHeight, minSize);

        ES2VramPool pool = ES2VramPool.instance;
        long size = pool.estimateRTTextureSize(texWidth, texHeight, false);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        // Note that ES2Context will set the viewport to the content
        // region of a RenderTarget (to ensure that we don't render into
        // the padded area of transparent pixels, if present).  Since
        // RTTextures are frequently recycled (i.e., reused by Decora) it
        // is imperative that we initialize the content region of the
        // RTTexture to be the physical size of the FBO modulo the padded
        // region.  (Suppose the caller asks for a 110x220 RTTexture, but
        // nonpow2 textures aren't supported; in this case, we will actually
        // create a 128x256 FBO.  If later that RTTexture gets reused for
        // a caller expecting to use 126x240 pixels, the viewport will be
        // setup correctly because it will set to the content region, or
        // 128x256 in this case, assuming no padding.)
        int contentW, contentH;
        int maxContentW, maxContentH;

        if (pad) {
            maxContentW = texWidth - 2;
            maxContentH = texHeight - 2;
            contentW = w;
            contentH = h;
        } else {
            maxContentW = texWidth;
            maxContentH = texHeight;
            contentW = w;
            contentH = h;
        }

        // save current texture
        glContext.setActiveTextureUnit(0);
        int savedFBO = glContext.getBoundFBO();
        int savedTex = glContext.getBoundTexture();

        int nativeTexID = 0;
        if (!msaa) {
            // TODO Mac and some other platforms do not support multisample texture
            // thus forced to skip texture creation below, and rather create a
            // msaa render buffer
            nativeTexID = glContext.createTexture(texWidth, texHeight);
        }

        int nativeFBOID = 0;
        if (nativeTexID != 0 || msaa) {
            // Create FBO (this method will generate and bind a new FBO,
            // and if texture is valid, attach texture as color attribute)
            nativeFBOID = glContext.createFBO(nativeTexID);
            if (nativeFBOID == 0) {
                glContext.deleteTexture(nativeTexID);
                nativeTexID = 0;
            }
        }
        ES2RTTextureData texData =
            new ES2RTTextureData(context, nativeTexID, nativeFBOID,
                                 texWidth, texHeight, size);
        ES2TextureResource<ES2RTTextureData> texRes = new ES2TextureResource<ES2RTTextureData>(texData);

        ES2RTTexture es2RTT = new ES2RTTexture(context, texRes, wrapMode,
                                texWidth, texHeight,
                                contentX, contentY,
                                contentW, contentH,
                                maxContentW, maxContentH);
        if (msaa) {
            es2RTT.createAndAttachMSAABuffer(context);
        }
        // Restore previous FBO
        glContext.bindFBO(savedFBO);
        // restore previous texture
        glContext.setBoundTexture(savedTex);
        return es2RTT;
    }

    public Texture getBackBuffer() {
        return this;
    }

    public Graphics createGraphics() {
        return ES2Graphics.create(context, this);
    }

    public int[] getPixels() {
        return null;
    }

    public boolean readPixels(Buffer pixels, int x, int y, int width, int height) {
        context.flushVertexBuffer();
        GLContext glContext = context.getGLContext();
        int id = glContext.getBoundFBO();
        int fboID = getFboID();
        boolean changeBoundFBO = id != fboID;
        if (changeBoundFBO) {
            glContext.bindFBO(fboID);
        }
        boolean result = glContext.readPixels(pixels, x, y, width, height);
        if (changeBoundFBO) {
            glContext.bindFBO(id);
        }
        return result;
    }

    public boolean readPixels(Buffer pixels) {
        return readPixels(pixels, getContentX(), getContentY(),
                 getContentWidth(), getContentHeight());
    }

    public int getFboID() {
        return resource.getResource().getFboID();
    }

    public Screen getAssociatedScreen() {
        return context.getAssociatedScreen();
    }

    @Override
    public void update(Image img) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int w, int h) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int w, int h, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Buffer pixels, PixelFormat format,
            int dstx, int dsty,
            int srcx, int srcy, int srcw, int srch, int srcscan,
            boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    public boolean isVolatile() {
        return false;
    }

    public boolean isMSAA() {
        return resource.getResource().getMSAARenderBufferID() != 0;
    }
}