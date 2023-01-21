/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import com.sun.prism.Image;
import com.sun.prism.Texture;
import com.sun.prism.MediaFrame;
import com.sun.prism.MultiTexture;
import com.sun.prism.PixelFormat;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.BufferUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class ES2Texture<T extends ES2TextureData> extends BaseTexture<ES2TextureResource<T>> {

    final ES2Context context;

    ES2Texture(ES2Context context, ES2TextureResource<T> resource,
               PixelFormat format, WrapMode wrapMode,
               int physicalWidth, int physicalHeight,
               int contentX, int contentY, int contentWidth, int contentHeight, boolean useMipmap)
    {
        super(resource, format, wrapMode,
              physicalWidth, physicalHeight,
              contentX, contentY, contentWidth, contentHeight, useMipmap);
        this.context = context;
    }

    ES2Texture(ES2Context context, ES2TextureResource<T> resource,
               PixelFormat format, WrapMode wrapMode,
               int physicalWidth, int physicalHeight,
               int contentX, int contentY, int contentWidth, int contentHeight,
               int maxContentWidth, int maxContentHeight, boolean useMipmap)
    {
        super(resource, format, wrapMode,
              physicalWidth, physicalHeight,
              contentX, contentY, contentWidth, contentHeight,
              maxContentWidth, maxContentHeight, useMipmap);
        this.context = context;
    }


    // TODO: We don't handle mipmap in shared texture yet.
    private ES2Texture(ES2Texture sharedTex, WrapMode newMode) {
        super(sharedTex, newMode, false);
        this.context = sharedTex.context;
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return new ES2Texture(this, newMode);
    }

    /**
     * Given a starting value and a maximum limit, returns the first
     * power-of-two greater than or equal to the starting value.  If the
     * resulting value is greater than the maximum limit, zero is returned.
     */
    static int nextPowerOfTwo(int val, int max) {
        if (val > max) {
            return 0;
        }
        int i = 1;
        while (i < val) {
            i *= 2;
        }
        return i;
    }

    static ES2Texture create(ES2Context context, PixelFormat format,
            WrapMode wrapMode, int w, int h, boolean useMipmap) {
        if (!context.getResourceFactory().isFormatSupported(format)) {
            throw new UnsupportedOperationException(
                    "Pixel format " + format
                    + " not supported on this device");
        }

        if (format == PixelFormat.MULTI_YCbCr_420) {
            throw new IllegalArgumentException("Format requires multitexturing: " + format);
        }

        // Normally we would use GL_CLAMP_TO_BORDER with a transparent border
        // color to implement our CLAMP_TO_ZERO edge mode, but unfortunately
        // that mode is not available in OpenGL ES.  The workaround is to pad
        // the texture with 2 rows/columns of transparent pixels on each side,
        // and we have to be careful to use the getContentX/Y/Width/Height()
        // methods so that we access only the content area of the texture.
        // The downside of this approach is that when npot textures are not
        // supported, the padding may cause us to cross the power-of-two
        // threshold more easily and therefore waste more VRAM.  Another
        // issue is that the REPEAT and CLAMP_TO_EDGE wrap modes will not work
        // as expected, so the user is responsible for passing allowPad=false
        // if one of those wrap modes is needed.

        // RT-27443
        // TODO: we really only need the padding here to implement the
        // CLAMP_TO_ZERO mode on devices and/or to implement the edge
        // smoothing tricks; but currently neither of those are needed/used
        // for regular ES2Texture objects, so why bother with the padding?
        GLContext glCtx = context.getGLContext();
        switch (wrapMode) {
            case CLAMP_TO_ZERO:
                if (!glCtx.canClampToZero()) {
                    wrapMode = wrapMode.simulatedVersion();
                }
                break;
            case CLAMP_TO_EDGE:
            case REPEAT:
                if (!glCtx.canCreateNonPowTwoTextures() &&
                    ((w & (w-1)) != 0 || (h & (h-1)) != 0))
                {
                    wrapMode = wrapMode.simulatedVersion();
                }
                break;
            case CLAMP_NOT_NEEDED:
                break;
            case CLAMP_TO_EDGE_SIMULATED:
            case CLAMP_TO_ZERO_SIMULATED:
            case REPEAT_SIMULATED:
                throw new IllegalArgumentException("Cannot request simulated wrap mode: "+wrapMode);
        }
        int maxSize = glCtx.getMaxTextureSize();
        int contentX, contentY;
        int contentW = w;
        int contentH = h;
        int texWidth, texHeight;
        switch (wrapMode) {
            case CLAMP_TO_ZERO_SIMULATED:
                contentX = 1;
                contentY = 1;
                texWidth = contentW + 2;
                texHeight = contentH + 2;
                break;
            case CLAMP_TO_EDGE_SIMULATED:
            case REPEAT_SIMULATED:
                contentX = 0;
                contentY = 0;
                texWidth = contentW;
                texHeight = contentH;
                if ((w & (w-1)) != 0) texWidth++;
                if ((h & (h-1)) != 0) texHeight++;
                break;
            case CLAMP_NOT_NEEDED:
            case CLAMP_TO_ZERO:
            case CLAMP_TO_EDGE:
            case REPEAT:
            default:
                contentX = 0;
                contentY = 0;
                texWidth = contentW;
                texHeight = contentH;
                break;
        }

        if (texWidth > maxSize || texHeight > maxSize) {
            throw new RuntimeException(
                    "Requested texture dimensions (" + w + "x" + h + ") "
                    + "require dimensions (" + texWidth + "x" + texHeight + ") "
                    + "that exceed maximum texture size (" + maxSize + ")");
        }

        if (!glCtx.canCreateNonPowTwoTextures()) {
            texWidth = nextPowerOfTwo(texWidth, maxSize);
            texHeight = nextPowerOfTwo(texHeight, maxSize);
        }

        if (texWidth <= 0 || texHeight <= 0) {
            throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
        }

        int bpp = format.getBytesPerPixelUnit();
        if (texWidth >= (Integer.MAX_VALUE / texHeight / bpp)) {
            throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
        }

        ES2VramPool pool = ES2VramPool.instance;
        long size = pool.estimateTextureSize(texWidth, texHeight, format);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        // save current texture object for this texture unit
        int savedTex = glCtx.getBoundTexture();
        ES2TextureData texData =
            new ES2TextureData(context, glCtx.genAndBindTexture(),
                               texWidth, texHeight, size);
        ES2TextureResource texRes = new ES2TextureResource(texData);

        boolean result = uploadPixels(glCtx, GLContext.GL_TEXTURE_2D, null, format,
                                      texWidth, texHeight,
                                      contentX, contentY,
                                      0, 0, contentW, contentH, 0, true, useMipmap);
        glCtx.texParamsMinMax(GLContext.GL_LINEAR, useMipmap);

        // restore previous texture objects
        glCtx.setBoundTexture(savedTex);

        if (!result) {
            return null;
        }
        return new ES2Texture(context, texRes, format, wrapMode,
                              texWidth, texHeight,
                              contentX, contentY,
                              contentW, contentH, useMipmap);

    }

    public static Texture create(ES2Context context, MediaFrame frame) {
        frame.holdFrame();

        int texWidth;
        int texHeight;
        PixelFormat format = frame.getPixelFormat();

        if (frame.getPixelFormat() == PixelFormat.MULTI_YCbCr_420) {
            // use encoded dimensions for texture sizes
            int width = frame.getEncodedWidth();
            int height = frame.getEncodedHeight();
            int planeCount = frame.planeCount(); // already validated

            // Create a MultiTexture instead, using the actual image dimensions
            MultiTexture tex = new MultiTexture(format, WrapMode.CLAMP_TO_EDGE,
                    frame.getWidth(), frame.getHeight());

            // create/add the subtextures
            // plane indices: 0 = luma, 1 = Cb, 2 = Cr, 3 (optional) = alpha
            for (int index = 0; index < planeCount; index++) {
                int subWidth = width;
                int subHeight = height;

                if (index == PixelFormat.YCBCR_PLANE_CHROMABLUE
                        || index == PixelFormat.YCBCR_PLANE_CHROMARED) {
                    subWidth /= 2;
                    subHeight /= 2;
                }

                // Create using subWidth/subHeight then adjust content afterwards
                ES2Texture subTex =
                    create(context, PixelFormat.BYTE_ALPHA, WrapMode.CLAMP_TO_EDGE,
                           subWidth, subHeight, false);
                if (subTex != null) {
                    tex.setTexture(subTex, index);
                }
            }

            frame.releaseFrame();
            return tex;
        }

        int encodedHeight;
        GLContext glCtx = context.getGLContext();
        int maxSize = glCtx.getMaxTextureSize();

        // Use encoded size as it may contain padding necessary for
        // colorspace conversion
        encodedHeight = frame.getEncodedHeight();
        texWidth = frame.getEncodedWidth();
        texHeight = encodedHeight;

        format = frame.getPixelFormat();

        // enforce POT textures if needed
        if (!glCtx.canCreateNonPowTwoTextures()) {
            texWidth = nextPowerOfTwo(texWidth, maxSize);
            texHeight = nextPowerOfTwo(texHeight, maxSize);
        }

        if (texWidth <= 0 || texHeight <= 0) {
            frame.releaseFrame();
            throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
        }

        int bpp = format.getBytesPerPixelUnit();
        if (texWidth >= (Integer.MAX_VALUE / texHeight / bpp)) {
            frame.releaseFrame();
            throw new RuntimeException("Illegal texture dimensions (" + texWidth + "x" + texHeight + ")");
        }

        ES2VramPool pool = ES2VramPool.instance;
        long size = pool.estimateTextureSize(texWidth, texHeight, format);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }

        // save current texture object for this texture unit
        int savedTex = glCtx.getBoundTexture();

        ES2TextureData texData =
            new ES2TextureData(context, glCtx.genAndBindTexture(),
                               texWidth, texHeight, size);
        ES2TextureResource texRes = new ES2TextureResource(texData);

        boolean result = uploadPixels(context.getGLContext(), GLContext.GL_TEXTURE_2D,
                frame, texWidth, texHeight, true);

        glCtx.texParamsMinMax(GLContext.GL_LINEAR, false);

        // restore previous texture objects
        glCtx.setBoundTexture(savedTex);

        ES2Texture tex = null;
        if (result) {
            tex = new ES2Texture(context, texRes, format, WrapMode.CLAMP_TO_EDGE,
                                 texWidth, texHeight,
                                 0, 0, frame.getWidth(), frame.getHeight(), false);
        }
        frame.releaseFrame();
        return tex;
    }

    private static boolean uploadPixels(GLContext glCtx, int target,
            Buffer pixels, PixelFormat format, int texw, int texh,
            int dstx, int dsty, int srcx, int srcy, int srcw, int srch,
            int srcscan, boolean create, boolean useMipmap) {
        int alignment = 1;
        int internalFormat;
        int pixelFormat;
        int pixelType;
        boolean isGL2 = ES2Pipeline.glFactory.isGL2();

        switch (format) {
            case BYTE_BGRA_PRE:
            case INT_ARGB_PRE:
                alignment = 4;
                internalFormat = GLContext.GL_RGBA;
                // Note: GL_BGRA not supported in OpenGL ES; developers should
                // call ResourceFactory.isFormatSupported() to check availability.
                pixelFormat = GLContext.GL_BGRA;
                if (!isGL2) {
                    // BGRA supported on iOS
                    if (!PlatformUtil.isIOS()) {
                        // for OpenGLES, BGRA can be supported by extension - if
                        // we have it, use it
                        if (ES2Pipeline.glFactory.isGLExtensionSupported("GL_EXT_texture_format_BGRA8888"))
                        {
                            internalFormat = pixelFormat = GLContext.GL_BGRA;
                        } else {
                            pixelFormat = GLContext.GL_RGBA;
                        }
                    }
                    pixelType = GLContext.GL_UNSIGNED_BYTE;
                } else {
                    pixelType = GLContext.GL_UNSIGNED_INT_8_8_8_8_REV;
                }
                break;
            case BYTE_RGB:
                // In the desktop case, include alpha channel in the texture,
                // because the special code that expands the src/dst regions in
                // BaseGraphics.drawTextureVO() expects texels outside the image
                // content area to be transparent.  Note that this trick won't
                // work on the device because ES 2.0 requires that the
                // pixelFormat match the internalFormat, so in that case we just
                // use GL_RGB as the internal format and return false
                // from ES2Context.isEdgeSmoothingSupported()...
                internalFormat = isGL2 ? GLContext.GL_RGBA : GLContext.GL_RGB;
                pixelFormat = GLContext.GL_RGB;
                pixelType = GLContext.GL_UNSIGNED_BYTE;
                break;
            case BYTE_GRAY:
                internalFormat = GLContext.GL_LUMINANCE;
                pixelFormat = GLContext.GL_LUMINANCE;
                pixelType = GLContext.GL_UNSIGNED_BYTE;
                break;
            case BYTE_ALPHA:
                internalFormat = GLContext.GL_ALPHA;
                pixelFormat = GLContext.GL_ALPHA;
                pixelType = GLContext.GL_UNSIGNED_BYTE;
                break;
            case FLOAT_XYZW:
                alignment = 4;
                // Note: In OpenGL ES 2.0, GL_RGBA32F is not supported but
                // the ES equivalent (GL_RGBA/GL_FLOAT) is supported only if
                // the GL_OES_texture_float is available; developers should
                // call ResourceFactory.isFormatSupported() to check availability.
                internalFormat = isGL2 ? GLContext.GL_RGBA32F : GLContext.GL_RGBA;
                pixelFormat = GLContext.GL_RGBA;
                pixelType = GLContext.GL_FLOAT;
                break;
            case BYTE_APPLE_422:
                // This format requires GL_APPLE_ycbcr_422
                alignment = 2;
                internalFormat = GLContext.GL_RGB;
                pixelFormat = GLContext.GL_YCBCR_422_APPLE;
                pixelType = GLContext.GL_UNSIGNED_SHORT_8_8_APPLE;
                break;
            case MULTI_YCbCr_420:
            default:
                throw new InternalError("Image format not supported: " + format);
        }

        if (!isGL2 && (internalFormat != pixelFormat) && !PlatformUtil.isIOS()) {
            throw new InternalError(
                    "On ES 2.0 device, internalFormat must match pixelFormat");
        }

        boolean result = true;
        if (create) {
            // Note: SKIP_PIXELS, SKIP_ROWS, and ROW_LENGTH are not supported
            // in OpenGL ES 2.0, so in order to handle images with different
            // dimensions than the texture we first use glTexImage2D() to create
            // the (possibly pow2-sized texture) and then use glTexSubImage2D()
            // to upload the pixels.
            glCtx.pixelStorei(GLContext.GL_UNPACK_ALIGNMENT, 1);
            if (format == PixelFormat.FLOAT_XYZW && internalFormat == GLContext.GL_RGBA) {
                // With the GL_OES_texture_float extension, there is no
                // special internalFormat (like GL_RGBA32F on desktop), so
                // we use GL_RGBA as the internalFormat and the pixelFormat
                // and pixelType dictate that this is a floating point texture.
                result = glCtx.texImage2D(target, 0, GLContext.GL_RGBA,
                        texw, texh, 0,
                        pixelFormat, pixelType, null, useMipmap);
            } else {
                // Note that on desktop we can use the GL_ALPHA format to help
                // minimize data transfer and storage costs (only 1 byte per
                // pixel and works regardless of internalFormat) since desktop
                // OpenGL can deal with the conversion.  On OpenGL ES however,
                // the pixelFormat needs to match the internalFormat, so we
                // need to make the zero buffer big enough.
                int initPixelFormat;
                int initPixelType;
                int initBytesPerPixel;
                if (isGL2) {
                    initPixelFormat = GLContext.GL_ALPHA;
                    initPixelType = GLContext.GL_UNSIGNED_BYTE;
                    initBytesPerPixel = 1;
                } else {
                    initPixelFormat = pixelFormat;
                    initPixelType = pixelType;
                    initBytesPerPixel = format.getBytesPerPixelUnit();
                }
                Buffer initBuf = null;
                if (srcw != texw || srch != texh) {
                    // The texture contents are undefined after a call to
                    // glTexImage2D() (when the given buffer is null).  For
                    // the case where we pad the image the edges of the texture
                    // with transparent pixels, we need to make sure those
                    // areas are transparent by uploading an empty buffer here.
                    // Note that we use the GL_ALPHA format to help minimize
                    // data transfer and storage costs (only 1 byte per pixel,
                    // and works regardless of internalFormat).

                    // RT-27443
                    // TODO: consider caching this buffer as a static (probably
                    // fine for desktop, but might not be so good for tv)...
                    int initSize = texw * texh * initBytesPerPixel;
                    initBuf = BufferUtil.newByteBuffer(initSize);
                }
                if (isGL2) {
                    // these params may have been set for a previous texture upload
                    glCtx.pixelStorei(GLContext.GL_UNPACK_ROW_LENGTH, 0);
                    glCtx.pixelStorei(GLContext.GL_UNPACK_SKIP_PIXELS, 0);
                    glCtx.pixelStorei(GLContext.GL_UNPACK_SKIP_ROWS, 0);
                    glCtx.pixelStorei(GLContext.GL_UNPACK_ALIGNMENT, alignment);
                }
                result = glCtx.texImage2D(target, 0, internalFormat,
                        texw, texh, 0,
                        initPixelFormat, initPixelType, initBuf, useMipmap);
            }
        }
        if (pixels != null) {
            // Note: Due to the above restrictions (no ROW_LENGTH, etc) we
            // have to assume that the data in "pixels" is tightly packed, i.e.,
            // srcx==0, srcy==0, and no space between scanlines.  If this
            // is not the case, we have to create a tightly packed copy of
            // the image first before uploading.
            // On desktop we avoid the copy, since we can use ROW_LENGTH, etc on
            // desktop OpenGL...
            int rowLength = srcscan / format.getBytesPerPixelUnit();

            if (!isGL2) {
                if (srcx != 0 || srcy != 0 || srcw != rowLength) {
                    // System.err.println("ES2Texture: Creating a packed buffer of pixels!");
                    pixels = Image.createPackedBuffer(pixels, format,
                            srcx, srcy, srcw, srch,
                            srcscan);
                    srcx = srcy = 0;
                    srcscan = srcw;
                    rowLength = srcscan / format.getBytesPerPixelUnit();
                }
            }
            glCtx.pixelStorei(GLContext.GL_UNPACK_ALIGNMENT, alignment);
            if (isGL2) {
                if (srcw == rowLength) {
                    glCtx.pixelStorei(GLContext.GL_UNPACK_ROW_LENGTH, 0);
                } else {
                    glCtx.pixelStorei(GLContext.GL_UNPACK_ROW_LENGTH, rowLength);
                }
            }

            int pos = pixels.position();

            int bufferElementSizeLog = getBufferElementSizeLog(pixels);
            int elementsInPixel = format.getBytesPerPixelUnit() >> bufferElementSizeLog;
            pixels.position(srcx * elementsInPixel + srcy * (srcscan >> bufferElementSizeLog));

            glCtx.texSubImage2D(target, 0,
                    dstx, dsty, srcw, srch,
                    pixelFormat, pixelType, pixels);
            pixels.position(pos);
        }
        return result;
    }

    private static boolean uploadPixels(GLContext glCtx, int target,
            MediaFrame frame, int texw, int texh, boolean create) {
        frame.holdFrame();

        int alignment = 1;
        int internalFormat;
        int pixelFormat;
        int pixelType;
        int srcw = frame.getEncodedWidth();
        int srch = frame.getEncodedHeight();
        int adjHeight = srch;
        ByteBuffer pixels = frame.getBufferForPlane(0);

        switch (frame.getPixelFormat()) {
            case INT_ARGB_PRE:
                alignment = 4;
                internalFormat = GLContext.GL_RGBA;
                pixelFormat = GLContext.GL_BGRA;
                if (pixels.order() == ByteOrder.LITTLE_ENDIAN) {
                    pixelType = GLContext.GL_UNSIGNED_INT_8_8_8_8_REV;
                } else {
                    pixelType = GLContext.GL_UNSIGNED_INT_8_8_8_8;
                }
                break;
            case BYTE_APPLE_422:
                // This format requires GL_APPLE_ycbcr_422
                alignment = 2;
                internalFormat = GLContext.GL_RGB;
                pixelFormat = GLContext.GL_YCBCR_422_APPLE;
                pixelType = GLContext.GL_UNSIGNED_SHORT_8_8_APPLE;
                break;
            case MULTI_YCbCr_420: // this needs to go through MultiTexture
            default:
                frame.releaseFrame();
                throw new InternalError("Invalid video image format "
                        + frame.getPixelFormat());
        }

        boolean result = true;
        if (create) {
            glCtx.pixelStorei(GLContext.GL_UNPACK_ALIGNMENT, 1);
            Buffer initBuf = null;
            if (srcw != texw || adjHeight != texh) {
                int initSize = texw * texh;
                initBuf = BufferUtil.newByteBuffer(initSize);
            }
            result = glCtx.texImage2D(target, 0, internalFormat,
                    texw, texh, 0,
                    GLContext.GL_ALPHA, GLContext.GL_UNSIGNED_BYTE, initBuf, false);
        }

        if (pixels != null) {
            glCtx.pixelStorei(GLContext.GL_UNPACK_ALIGNMENT, alignment);
            glCtx.pixelStorei(GLContext.GL_UNPACK_ROW_LENGTH,
                    frame.strideForPlane(0) / alignment);
            glCtx.texSubImage2D(target, 0,
                    0, 0, srcw, frame.getHeight(),
                    pixelFormat, pixelType, pixels);
        }
        frame.releaseFrame();
        return result;
    }

    public static int getBufferElementSizeLog(Buffer b) {
        if (b instanceof ByteBuffer) {
            return 0;
        } else if (b instanceof IntBuffer || b instanceof FloatBuffer) {
            return 2;
        } else {
            throw new InternalError("Unsupported Buffer type: " + b.getClass());
        }
    }

    void updateWrapState() {
        WrapMode cWrapMode = getWrapMode();
        // Note that state set with glTexParameteri() stays with the texture
        // object (not part of the context state).  In most cases the currently
        // bound texture will already be this one, but in case not, we save
        // and restore the texture state.
        ES2TextureData texData = resource.getResource();
        if (texData.getWrapMode() != cWrapMode) {
            GLContext glCtx = context.getGLContext();
            int savedTex = glCtx.getBoundTexture();
            int texID = texData.getTexID();
            if (savedTex != texID) {
                glCtx.setBoundTexture(texID);
            }
            glCtx.updateWrapState(texID, cWrapMode);
            if (savedTex != texID) {
                glCtx.setBoundTexture(savedTex);
            }
            texData.setWrapMode(cWrapMode);
        }
    }

    void updateFilterState() {
        boolean cLFM = getLinearFiltering();
        // Note that state set with glTexParameteri() stays with the texture
        // object (not part of the context state).  In most cases the currently
        // bound texture will already be this one, but in case not, we save
        // and restore the texture state.
        ES2TextureData texData = resource.getResource();
        if (texData.isFiltered() != cLFM) {
            GLContext glCtx = context.getGLContext();
            int savedTex = glCtx.getBoundTexture();
            int texID = texData.getTexID();
            if (savedTex != texID) {
                glCtx.setBoundTexture(texID);
            }
            glCtx.updateFilterState(texID, cLFM);
            if (savedTex != texID) {
                glCtx.setBoundTexture(savedTex);
            }
            texData.setFiltered(cLFM);
        }
    }

    public int getNativeSourceHandle() {
        return resource.getResource().getTexID();
    }

    @Override
    public void update(Buffer pixels, PixelFormat format,
            int dstx, int dsty,
            int srcx, int srcy,
            int srcw, int srch,
            int srcscan,
            boolean skipFlush) {
        checkUpdateParams(pixels, format,
                dstx, dsty, srcx, srcy, srcw, srch, srcscan);

        if (!skipFlush) {
            context.flushVertexBuffer();
        }

        int texID = getNativeSourceHandle();
        if (texID != 0) {
            GLContext glCtx = context.getGLContext();
            // optimization: avoid setting the bound texture redundantly
            int savedUnit = glCtx.getActiveTextureUnit();
            int savedTex = glCtx.getBoundTexture();
            boolean alreadyBound = false;
            for (int i = 0; i < 2; i++) {
                if (glCtx.getBoundTexture(i) == texID) {
                    alreadyBound = true;
                    if (savedUnit != i) {
                        glCtx.setActiveTextureUnit(i);
                    }
                    break;
                }
            }
            if (!alreadyBound) {
                glCtx.setBoundTexture(texID);
            }

            int contentX = getContentX();
            int contentY = getContentY();
            int contentW = getContentWidth();
            int contentH = getContentHeight();
            int texWidth = getPhysicalWidth();
            int texHeight = getPhysicalHeight();
            boolean useMipmap = getUseMipmap();
            uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                         pixels, format,
                         texWidth, texHeight,
                         contentX + dstx, contentY + dsty,
                         srcx, srcy, srcw, srch, srcscan, false, useMipmap);

            switch (getWrapMode()) {
                case CLAMP_TO_EDGE:
                    break;
                case CLAMP_TO_EDGE_SIMULATED: {
                    boolean copyR = (contentW < texWidth  && dstx + srcw == contentW);
                    boolean copyL = (contentH < texHeight && dsty + srch == contentH);
                    // Repeat right edge, if it was modified
                    if (copyR) {
                        uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                                     pixels, format,
                                     texWidth, texHeight,
                                     contentX + contentW, contentY + dsty,
                                     srcx + srcw-1, srcy, 1, srch, srcscan, false, useMipmap);
                    }
                    // Repeat bottom edge, if it was modified
                    if (copyL) {
                        uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                                     pixels, format,
                                     texWidth, texHeight,
                                     contentX + dstx, contentY + contentH,
                                     srcx, srcy + srch-1, srcw, 1, srcscan, false, useMipmap);
                        // Repeat LR corner, if it was modified
                        if (copyR) {
                            uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                                        pixels, format,
                                        texWidth, texHeight,
                                        contentX + contentW, contentY + contentH,
                                        srcx + srcw-1, srcy + srch-1, 1, 1, srcscan, false, useMipmap);
                        }
                    }
                    break;
                }
                case REPEAT:
                    break;
                case REPEAT_SIMULATED: {
                    boolean repeatL = (contentW < texWidth  && dstx == 0);
                    boolean repeatT = (contentH < texHeight && dsty == 0);
                    // Repeat left edge on right, if it was modified
                    if (repeatL) {
                        uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                                     pixels, format,
                                     texWidth, texHeight,
                                     contentX + contentW, contentY + dsty,
                                     srcx, srcy, 1, srch, srcscan, false, useMipmap);
                    }
                    // Repeat top edge on bottom, if it was modified
                    if (repeatT) {
                        uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                                     pixels, format,
                                     texWidth, texHeight,
                                     contentX + dstx, contentY + contentH,
                                     srcx, srcy, srcw, 1, srcscan, false, useMipmap);
                        // Repeat UL pixel at LR, if it was modified
                        if (repeatL) {
                            uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                                         pixels, format,
                                         texWidth, texHeight,
                                         contentX + contentW, contentY + contentH,
                                         srcx, srcy, 1, 1, srcscan, false, useMipmap);
                        }
                    }
                    break;
                }
            }

            // restore the previous texture/unit state if it was changed above
            if (savedUnit != glCtx.getActiveTextureUnit()) {
                glCtx.setActiveTextureUnit(savedUnit);
            }
            if (savedTex != glCtx.getBoundTexture()) {
                glCtx.setBoundTexture(savedTex);
            }
        }
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        if (!skipFlush) {
            context.flushVertexBuffer();
        }

        int texID = getNativeSourceHandle();
        if (texID != 0) {
            GLContext glCtx = context.getGLContext();
            // optimization: avoid setting the bound texture redundantly
            int savedUnit = glCtx.getActiveTextureUnit();
            int savedTex = glCtx.getBoundTexture();
            boolean alreadyBound = false;
            for (int i = 0; i < 2; i++) {
                if (glCtx.getBoundTexture(i) == texID) {
                    alreadyBound = true;
                    if (savedUnit != i) {
                        glCtx.setActiveTextureUnit(i);
                    }
                    break;
                }
            }
            if (!alreadyBound) {
                glCtx.setBoundTexture(texID);
            }

            uploadPixels(glCtx, GLContext.GL_TEXTURE_2D,
                    frame,
                    getPhysicalWidth(), getPhysicalHeight(),
                    false);

            // restore the previous texture/unit state if it was changed above
            if (savedUnit != glCtx.getActiveTextureUnit()) {
                glCtx.setActiveTextureUnit(savedUnit);
            }
            if (savedTex != glCtx.getBoundTexture()) {
                glCtx.setBoundTexture(savedTex);
            }
        }
    }
}
