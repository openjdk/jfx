/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d;

import com.sun.javafx.image.IntPixelGetter;
import com.sun.javafx.image.PixelConverter;
import com.sun.javafx.image.PixelGetter;
import com.sun.javafx.image.PixelSetter;
import com.sun.javafx.image.PixelUtils;
import com.sun.javafx.image.impl.ByteBgr;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.image.impl.ByteRgb;
import com.sun.javafx.image.impl.IntArgbPre;
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.j2d.J2DTexture.J2DTexResource;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

class J2DTexture extends BaseTexture<J2DTexResource> {

    private final PixelSetter setter;

    static class J2DTexResource extends ManagedResource<BufferedImage> {
        public J2DTexResource(BufferedImage bimg) {
            super(bimg, J2DTexturePool.instance);
        }

        @Override
        public void free() {
            resource.flush();
        }
    }

    static J2DTexture create(PixelFormat format, WrapMode wrapMode, int w, int h) {
        int type;
        PixelSetter setter;
        switch (format) {
            case BYTE_RGB:
                type = BufferedImage.TYPE_3BYTE_BGR;
                setter = ByteBgr.setter;
                break;
            case BYTE_GRAY:
                type = BufferedImage.TYPE_BYTE_GRAY;
                setter = ByteGray.setter;
                break;
            case INT_ARGB_PRE:
            case BYTE_BGRA_PRE:
                type = BufferedImage.TYPE_INT_ARGB_PRE;
                setter = IntArgbPre.setter;
                break;
            default:
                throw new InternalError("Unrecognized PixelFormat ("+format+")!");
        }
        J2DTexturePool pool = J2DTexturePool.instance;
        long size = J2DTexturePool.size(w, h, type);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }
        BufferedImage bimg = new BufferedImage(w, h, type);
        return new J2DTexture(bimg, format, setter, wrapMode);
    }

    J2DTexture(BufferedImage bimg, PixelFormat format,
               PixelSetter setter, WrapMode wrapMode)
    {
        super(new J2DTexResource(bimg), format, wrapMode,
              bimg.getWidth(), bimg.getHeight());
        this.setter = setter;
    }

    J2DTexture(J2DTexture sharedTex, WrapMode altMode) {
        super(sharedTex, altMode, false);
        this.setter = sharedTex.setter;
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return new J2DTexture(this, newMode);
    }

    BufferedImage getBufferedImage() {
        return resource.getResource();
    }

    private static PixelGetter getGetter(PixelFormat format) {
        switch (format) {
            case BYTE_RGB:
                return ByteRgb.getter;
            case BYTE_GRAY:
                return ByteGray.getter;
            case INT_ARGB_PRE:
                return IntArgbPre.getter;
            case BYTE_BGRA_PRE:
                return ByteBgraPre.getter;
            default:
                throw new InternalError("Unrecognized PixelFormat (" + format + ")!");
        }
    }

    private static Buffer getDstBuffer(BufferedImage bimg) {
        if (bimg.getType() == BufferedImage.TYPE_INT_ARGB_PRE) {
            int dstbuf[] = ((java.awt.image.DataBufferInt)
                            bimg.getRaster().getDataBuffer()).getData();
            return IntBuffer.wrap(dstbuf);
        } else {
            byte dstbuf[] = ((java.awt.image.DataBufferByte)
                             bimg.getRaster().getDataBuffer()).getData();
            return ByteBuffer.wrap(dstbuf);
        }
    }

    void updateFromBuffer(BufferedImage bimg, Buffer buffer,
            PixelFormat format,
            int dstx, int dsty,
            int srcx, int srcy, int srcw, int srch,
            int srcscan)
    {
        PixelGetter getter = getGetter(format);
        PixelConverter converter = PixelUtils.getConverter(getter, setter);
        if (PrismSettings.debug) {
            System.out.println("src = [" + srcx + ", " + srcy + "] x [" + srcw + ", " + srch + "], dst = [" + dstx + ", " + dsty + "]");
            System.out.println("bimg = " + bimg);
            System.out.println("format = " + format + ", buffer = " + buffer);
            System.out.println("getter = " + getter + ", setter = " + setter);
            System.out.println("converter = " + converter);
        }
        int dstscan = bimg.getWidth() * setter.getNumElements();
        int dstoffset = dsty * dstscan + dstx * setter.getNumElements();
        // original srcscan parameter is in bytes, but
        // converter expects srcscan to be in elements, not bytes
        if (getter instanceof IntPixelGetter) {
            srcscan /= 4;
        }
        int srcoffset = buffer.position() + srcy * srcscan + srcx * getter.getNumElements();
        converter.convert(buffer, srcoffset, srcscan, getDstBuffer(bimg), dstoffset, dstscan, srcw, srch);
    }

    @Override
    public void update(Buffer buffer, PixelFormat format,
                       int dstx, int dsty,
                       int srcx, int srcy, int srcw, int srch,
                       int srcscan,
                       boolean skipFlush)
    {
        BufferedImage bimg = getBufferedImage();
        buffer.position(0);
        updateFromBuffer(bimg, buffer, format,
                         dstx, dsty,
                         srcx, srcy, srcw, srch, srcscan);
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush)
    {
        frame.holdFrame();

        if (frame.getPixelFormat() != PixelFormat.INT_ARGB_PRE) {
            MediaFrame newFrame = frame.convertToFormat(PixelFormat.INT_ARGB_PRE);
            frame.releaseFrame(); // release or we'll leak
            frame = newFrame;

            if (null == frame) {
                // FIXME: error condition?
                return;
            }
        }

        ByteBuffer bbuf = frame.getBufferForPlane(0);
        BufferedImage bimg = getBufferedImage();
        updateFromBuffer(bimg, bbuf.asIntBuffer(), PixelFormat.INT_ARGB_PRE,
                0, 0, 0, 0, frame.getWidth(), frame.getHeight(),
                frame.strideForPlane(0));
        frame.releaseFrame();
    }
}
