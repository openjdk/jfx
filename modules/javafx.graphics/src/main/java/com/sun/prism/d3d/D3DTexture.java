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

import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class D3DTexture extends BaseTexture<D3DTextureResource>
    implements D3DContextSource
{

    D3DTexture(D3DContext context, PixelFormat format, WrapMode wrapMode,
               long pResource,
               int physicalWidth, int physicalHeight,
               int contentWidth, int contentHeight, boolean isRTT)
    {
        this(context, format, wrapMode, pResource, physicalWidth, physicalHeight,
                0, 0, contentWidth, contentHeight, isRTT, 0, false);
    }

    D3DTexture(D3DContext context, PixelFormat format, WrapMode wrapMode,
               long pResource,
               int physicalWidth, int physicalHeight,
               int contentX, int contentY, int contentWidth, int contentHeight,
               boolean isRTT, int samples, boolean useMipmap)
    {
        super(new D3DTextureResource(new D3DTextureData(context, pResource, isRTT,
                                                        physicalWidth, physicalHeight,
                                                        format, samples)),
              format, wrapMode,
              physicalWidth, physicalHeight,
              contentX, contentY, contentWidth, contentHeight,
              physicalWidth, physicalHeight, useMipmap);
    }

    // TODO: We don't handle mipmap in shared texture yet.
    D3DTexture(D3DTexture sharedTex, WrapMode altMode) {
        super(sharedTex, altMode, false);
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return new D3DTexture(this, newMode);
    }

    public long getNativeSourceHandle() {
        return resource.getResource().getResource();
    }

    public long getNativeTextureObject() {
        return D3DResourceFactory.nGetNativeTextureObject(getNativeSourceHandle());
    }

    @Override
    public D3DContext getContext() {
        return resource.getResource().getContext();
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush)
    {
        if (frame.getPixelFormat() == PixelFormat.MULTI_YCbCr_420) {
            // shouldn't have gotten this far
            throw new IllegalArgumentException("Unsupported format "+frame.getPixelFormat());
        }
        frame.holdFrame();

        ByteBuffer pixels = frame.getBufferForPlane(0);
        int result;

        // FIXME: checkVideoParams since they differ from normal params slightly

        D3DContext ctx = getContext();

        if (!skipFlush) {
            ctx.flushVertexBuffer();
        }

        PixelFormat targetFormat = frame.getPixelFormat();

        // always do plane 0 since it's used for packed formats
        if (targetFormat.getDataType() == PixelFormat.DataType.INT) {
            result = D3DResourceFactory.nUpdateTextureI(
                    ctx.getContextHandle(),
                    getNativeSourceHandle(),
                    pixels.asIntBuffer(), null,
                    0, 0, 0, 0, frame.getEncodedWidth(), frame.getEncodedHeight(),
                    frame.strideForPlane(0));
        } else {
            result = D3DResourceFactory.nUpdateTextureB(
                    ctx.getContextHandle(),
                    getNativeSourceHandle(),
                    pixels, null,
                    targetFormat.ordinal(),
                    0, 0,
                    0, 0, frame.getEncodedWidth(), frame.getEncodedHeight(),
                    frame.strideForPlane(0));
        }

        D3DContext.validate(result);
        frame.releaseFrame();
    }

    @Override
    public void update(Buffer pixels, PixelFormat format,
                       int dstx, int dsty,
                       int srcx, int srcy,
                       int srcw, int srch,
                       int srcscan,
                       boolean skipFlush)
    {
        checkUpdateParams(pixels, format,
                          dstx, dsty, srcx, srcy, srcw, srch, srcscan);

        if (!skipFlush) {
            getContext().flushVertexBuffer();
        }

        int contentX = getContentX();
        int contentY = getContentY();
        int contentW = getContentWidth();
        int contentH = getContentHeight();
        int texWidth = getPhysicalWidth();
        int texHeight = getPhysicalHeight();
        update(pixels, format, contentX + dstx, contentY + dsty,
               srcx, srcy, srcw, srch, srcscan);
        switch (getWrapMode()) {
            case CLAMP_TO_EDGE:
                break;
            case CLAMP_TO_EDGE_SIMULATED: {
                boolean copyR = (contentW < texWidth  && dstx + srcw == contentW);
                boolean copyL = (contentH < texHeight && dsty + srch == contentH);
                // Repeat right edge, if it was modified
                if (copyR) {
                    update(pixels, format, contentX + contentW, contentY + dsty,
                           srcx + srcw-1, srcy, 1, srch, srcscan);
                }
                // Repeat bottom edge, if it was modified
                if (copyL) {
                    update(pixels, format, contentX + dstx, contentY + contentH,
                           srcx, srcy + srch-1, srcw, 1, srcscan);
                    // Repeat LR corner, if it was modified
                    if (copyR) {
                        update(pixels, format, contentX + contentW, contentY + contentH,
                               srcx + srcw-1, srcy + srch-1, 1, 1, srcscan);
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
                    update(pixels, format, contentX + contentW, contentY + dsty,
                            srcx, srcy, 1, srch, srcscan);
                }
                // Repeat top edge on bottom, if it was modified
                if (repeatT) {
                    update(pixels, format, contentX + dstx, contentY + contentH,
                            srcx, srcy, srcw, 1, srcscan);
                    // Repeat UL pixel at LR, if it was modified
                    if (repeatL) {
                        update(pixels, format, contentX + contentW, contentY + contentH,
                                srcx, srcy, 1, 1, srcscan);
                    }
                }
                break;
            }
        }
    }

    public void update(Buffer pixels, PixelFormat format,
                       int dstx, int dsty,
                       int srcx, int srcy,
                       int srcw, int srch,
                       int srcscan)
    {
        D3DContext ctx = getContext();
        int res;
        if (format.getDataType() == PixelFormat.DataType.INT) {
            IntBuffer buf = (IntBuffer)pixels;
            int[] arr = buf.hasArray() ? buf.array() : null;
            res = D3DResourceFactory.nUpdateTextureI(ctx.getContextHandle(),
                                                     getNativeSourceHandle(),
                                                     buf, arr, dstx, dsty,
                                                     srcx, srcy, srcw, srch, srcscan);
        } else if (format.getDataType() == PixelFormat.DataType.FLOAT) {
            FloatBuffer buf = (FloatBuffer)pixels;
            float[] arr = buf.hasArray() ? buf.array() : null;
            res = D3DResourceFactory.nUpdateTextureF(ctx.getContextHandle(),
                                                     getNativeSourceHandle(),
                                                     buf, arr, dstx, dsty,
                                                     srcx, srcy, srcw, srch, srcscan);
        } else {
            ByteBuffer buf = (ByteBuffer)pixels;
            buf.rewind();
            byte[] arr = buf.hasArray() ? buf.array() : null;
            res = D3DResourceFactory.nUpdateTextureB(ctx.getContextHandle(),
                                                     getNativeSourceHandle(),
                                                     buf, arr, format.ordinal(),
                                                     dstx, dsty,
                                                     srcx, srcy, srcw, srch, srcscan);
        }
        D3DContext.validate(res);
    }
}
