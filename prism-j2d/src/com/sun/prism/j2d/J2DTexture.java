/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;
import com.sun.prism.j2d.J2DTexture.J2DTexResource;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

class J2DTexture extends BaseTexture<J2DTexResource> {

    private final Updater updater;

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
        Updater updater;
        switch (format) {
            case BYTE_RGB:
                type = BufferedImage.TYPE_3BYTE_BGR;
                updater = ThreeByteBgrUpdater.THREE_BYTE_BGR_INSTANCE;
                break;
            case BYTE_GRAY:
                type = BufferedImage.TYPE_BYTE_GRAY;
                updater = Updater.GENERAL_INSTANCE;
                break;
            case INT_ARGB_PRE:
            case BYTE_BGRA_PRE:
                type = BufferedImage.TYPE_INT_ARGB_PRE;
                updater = IntArgbPreUpdater.INT_ARGB_PRE_INSTANCE;
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
        return new J2DTexture(bimg, updater, wrapMode);
    }

    J2DTexture(BufferedImage bimg, Updater updater, WrapMode wrapMode) {
        super(new J2DTexResource(bimg), PixelFormat.BYTE_RGB, wrapMode,
              bimg.getWidth(), bimg.getHeight());
        this.updater = updater;
    }

    J2DTexture(J2DTexture sharedTex, WrapMode altMode) {
        super(sharedTex, altMode);
        this.updater = sharedTex.updater;
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return new J2DTexture(this, newMode);
    }

    BufferedImage getBufferedImage() {
        return resource.getResource();
    }

    public void update(Buffer buffer, PixelFormat format,
                       int dstx, int dsty,
                       int srcx, int srcy, int srcw, int srch,
                       int srcscan,
                       boolean skipFlush)
    {
        BufferedImage bimg = getBufferedImage();
        switch (format) {
            case BYTE_RGB:
                updater.updateFromByteBuffer(bimg, (ByteBuffer) buffer,
                                             dstx, dsty,
                                             srcx, srcy, srcw, srch, srcscan,
                                             0, 1, 2, -1, 3, true);
                return;
            case BYTE_GRAY:
                updater.updateFromByteBuffer(bimg, (ByteBuffer) buffer,
                                             dstx, dsty,
                                             srcx, srcy, srcw, srch, srcscan,
                                             0, 0, 0, -1, 1, true);
                return;
            case INT_ARGB_PRE:
                updater.updateFromIntBuffer(bimg, (IntBuffer) buffer,
                                            dstx, dsty,
                                            srcx, srcy, srcw, srch, srcscan/4,
                                            true, true);
                return;
            case BYTE_BGRA_PRE:
                updater.updateFromByteBuffer(bimg, (ByteBuffer) buffer,
                                             dstx, dsty,
                                             srcx, srcy, srcw, srch, srcscan,
                                             2, 1, 0, 3, 4, true);
                return;
            default:
                throw new UnsupportedOperationException("Pixel format "+format+" not supported yet.");
        }
    }

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

        ByteBuffer bbuf = frame.getBuffer();
        bbuf.position(frame.offsetForPlane(0));
        BufferedImage bimg = getBufferedImage();
        updater.updateFromIntBuffer(bimg, bbuf.asIntBuffer(),
                0, 0, 0, 0, frame.getWidth(), frame.getHeight(),
                frame.strideForPlane(0)/4,
                true, true);
        frame.releaseFrame();
    }

    static class Updater {
        static Updater GENERAL_INSTANCE = new Updater();

        private Updater() {}

        /**
         * The source data is copied from the {@code ByteBuffer bbuf} into
         * the destination {@code BufferedImage bimg} using a procedure
         * similar to the following pseudo-code:
         * <pre>
         *     bbuf.position(bbuf.position()
         *                   + srcy * srcrowelems
         *                   + srcx * srcpixelbytes);
         *     for (int y = 0; y < srch; y++) {
         *         for (int x = 0; x < srcw; x++) {
         *             red = bbuf.get(bbuf.position() + roff) & 0xff;
         *             green = bbuf.get(bbuf.position() + goff) & 0xff;
         *             blue = bbuf.get(bbuf.position() + boff) & 0xff;
         *             alpha = (aoff < 0) ? 0xff :
         *                 bbuf.get(bbuf.position() + aoff) & 0xff;
         *             int argb = {non-premultiplied combination of components};
         *             bimg.setRGB(dstx + x, dsty + y, argb);
         *             bbuf.position(bbuf.position() + srcpixelbytes);
         *         }
         *         bbuf.position(bbuf.position() + srcrowelems - srcw * srcpixelbytes);
         *     }
         * </pre>
         * <b>Note that the position of the {@code bbuf} buffer may or
         * may not be moved by the operation of this method!</b>
         *
         * @param bimg the destination "Texture" as a {@code BufferedImage}
         * @param bbuf the source {@code ByteBuffer}
         * @param dstx the X coordinate of the location to place the data in bimg
         * @param dsty the Y coordinate of the location to place the data in bimg
         * @param srcx the X coordinate of the source data in bbuf
         * @param srcy the Y coordinate of the source data in bbuf
         * @param srcw the width of the source data in pixels
         * @param srch the height of the source data in pixels
         * @param srcrowelems the number of source elements (bytes) per scanline
         * @param roff the offset in bytes of the red component in a pixel
         * @param goff the offset in bytes of the green component in a pixel
         * @param boff the offset in bytes of the blue component in a pixel
         * @param aoff the offset in bytes of the alpha component in a pixel,
         *             or -1 if there are no alpha values in the source pixels
         * @param srcpixelbytes the size of a source pixel in bytes
         * @param ispremult true if the source data is premultiplied
         */
        void updateFromByteBuffer(BufferedImage bimg, ByteBuffer bbuf,
                                  int dstx, int dsty,
                                  int srcx, int srcy,
                                  int srcw, int srch, int srcrowelems,
                                  int roff, int goff, int boff, int aoff,
                                  int srcpixelbytes, boolean ispremult)
        {
            byte srcbuf[];
            int srcoffset = srcy * srcrowelems + srcx * srcpixelbytes;
            int a = 0xff;
            if (bbuf.hasArray()) {
                srcbuf = bbuf.array();
                srcoffset += bbuf.arrayOffset();
            } else {
                srcbuf = new byte[srcw * srcpixelbytes];
                bbuf.position(bbuf.position() + srcoffset);
                srcoffset = 0;
            }
            srcrowelems -= srcpixelbytes * srcw;
            for (int sy = 0; sy < srch; sy++) {
                if (!bbuf.hasArray()) {
                    bbuf.get(srcbuf);
                    if (srcrowelems != 0) {
                        bbuf.position(bbuf.position() + srcrowelems);
                    }
                    srcoffset = 0;
                }
                for (int sx = 0; sx < srcw; sx++) {
                    int r = srcbuf[srcoffset + roff] & 0xff;
                    int g = srcbuf[srcoffset + goff] & 0xff;
                    int b = srcbuf[srcoffset + boff] & 0xff;
                    if (aoff >= 0) {
                        a = srcbuf[srcoffset + aoff] & 0xff;
                        if (ispremult && a != 0xff && a != 0) {
                            r = r * 255 / a;
                            g = g * 255 / a;
                            b = b * 255 / a;
                        }
                    }
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    bimg.setRGB(dstx + sx, dsty + sy, argb);
                    srcoffset += srcpixelbytes;
                }
                srcoffset += srcrowelems;
            }
        }

        /**
         * The source data is copied from the {@code IntBuffer ibuf} into
         * the destination {@code BufferedImage bimg} using a procedure
         * similar to the following pseudo-code:
         * <pre>
         *     ibuf.position(ibuf.position()
         *                   + srcy * srcrowelems
         *                   + srcx);
         *     for (int y = 0; y < srch; y++) {
         *         for (int x = 0; x < srcw; x++) {
         *             int argb = bbuf.get();
         *             if (!hasalpha) argb |= 0xff000000
         *             else {adjust premult of argb if necessary}
         *             bimg.setRGB(dstx + x, dsty + y, argb);
         *         }
         *         bbuf.position(bbuf.position() + srcrowelems - srcw);
         *     }
         * </pre>
         * <b>Note that the position of the {@code bbuf} buffer may or
         * may not be moved by the operation of this method!</b>
         *
         * @param bimg the destination "Texture" as a {@code BufferedImage}
         * @param ibuf the source {@code IntBuffer}
         * @param dstx the X coordinate of the location to place the data in bimg
         * @param dsty the Y coordinate of the location to place the data in bimg
         * @param srcx the X coordinate of the source data in bbuf
         * @param srcy the Y coordinate of the source data in bbuf
         * @param srcw the width of the source data in pixels
         * @param srch the height of the source data in pixels
         * @param srcrowelems the number of source elements (ints) per scanline
         * @param hasalpha true if the source pixels contain alpha
         * @param ispremult true if the source data is premultiplied
         */
        void updateFromIntBuffer(BufferedImage bimg, IntBuffer ibuf,
                                 int dstx, int dsty,
                                 int srcx, int srcy,
                                 int srcw, int srch, int srcrowelems,
                                 boolean hasalpha, boolean ispremult)
        {
            int srcbuf[];
            int srcoffset = srcy * srcrowelems + srcx;
            if (ibuf.hasArray()) {
                srcbuf = (int[]) ibuf.array();
                srcoffset += ibuf.arrayOffset();
            } else {
                srcbuf = new int[srcw];
                ibuf.position(ibuf.position() + srcoffset);
                srcoffset = 0;
            }
            for (int sy = 0; sy < srch; sy++) {
                if (!ibuf.hasArray()) {
                    ibuf.get(srcbuf);
                    if (srcrowelems - srcw != 0) {
                        ibuf.position(ibuf.position() + (srcrowelems - srcw));
                    }
                    srcoffset = 0;
                }
                for (int sx = 0; sx < srcw; sx++) {
                    int argb = srcbuf[srcoffset + sx];
                    if (!hasalpha) {
                        argb |= 0xff000000;
                    } else if (ispremult) {
                        int a = argb >>> 24;
                        if (a != 0xff && a != 0) {
                            int r = (argb >> 16) & 0xff;
                            int g = (argb >>  8) & 0xff;
                            int b = (argb      ) & 0xff;
                            r = r * 255 / a;
                            g = g * 255 / a;
                            b = b * 255 / a;
                            argb = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                    bimg.setRGB(dstx + sx, dsty + sy, argb);
                }
                srcoffset += srcrowelems;
            }
        }
    }

    static class IntArgbPreUpdater extends Updater {
        static IntArgbPreUpdater INT_ARGB_PRE_INSTANCE =
            new IntArgbPreUpdater();

        private IntArgbPreUpdater() {}

        @Override
        void updateFromByteBuffer(BufferedImage bimg, ByteBuffer bbuf,
                                  int dstx, int dsty,
                                  int srcx, int srcy,
                                  int srcw, int srch, int srcrowelems,
                                  int roff, int goff, int boff, int aoff,
                                  int srcpixelbytes, boolean ispremult)
        {
            int dstbuf[] = ((java.awt.image.DataBufferInt)
                            bimg.getRaster().getDataBuffer()).getData();
            int dstscan = bimg.getWidth();
            int dstoffset = dsty * dstscan + dstx;
            byte srcbuf[];
            int srcoffset = srcy * srcrowelems + srcx * srcpixelbytes;
            int a = 0xff;
            if (bbuf.hasArray()) {
                srcbuf = bbuf.array();
                srcoffset += bbuf.arrayOffset();
            } else {
                srcbuf = new byte[srcw * srcpixelbytes];
                bbuf.position(bbuf.position() + srcoffset);
                srcoffset = 0;
            }
            srcrowelems -= srcpixelbytes * srcw;
            for (int sy = 0; sy < srch; sy++) {
                if (!bbuf.hasArray()) {
                    bbuf.get(srcbuf);
                    if (srcrowelems != 0) {
                        bbuf.position(bbuf.position() + srcrowelems);
                    }
                    srcoffset = 0;
                }
                for (int sx = 0; sx < srcw; sx++) {
                    int r = srcbuf[srcoffset + roff] & 0xff;
                    int g = srcbuf[srcoffset + goff] & 0xff;
                    int b = srcbuf[srcoffset + boff] & 0xff;
                    if (aoff >= 0) {
                        a = srcbuf[srcoffset + aoff] & 0xff;
                        if (!ispremult && a != 0xff) {
                            if (a == 0) {
                                r = g = b = 0;
                            } else {
                                r = r * a / 255;
                                g = g * a / 255;
                                b = b * a / 255;
                            }
                        }
                    }
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    dstbuf[dstoffset + sx] = argb;
                    srcoffset += srcpixelbytes;
                }
                srcoffset += srcrowelems;
                dstoffset += dstscan;
            }
        }

        @Override
        void updateFromIntBuffer(BufferedImage bimg, IntBuffer ibuf,
                                 int dstx, int dsty,
                                 int srcx, int srcy,
                                 int srcw, int srch, int srcrowelems,
                                 boolean hasalpha, boolean ispremult)
        {
            int dstbuf[] = ((java.awt.image.DataBufferInt)
                            bimg.getRaster().getDataBuffer()).getData();
            int dstscan = bimg.getWidth();
            int dstoffset = dsty * dstscan + dstx;
            int srcbuf[];
            int srcoffset = srcy * srcrowelems + srcx;
            if (ibuf.hasArray()) {
                srcbuf = (int[]) ibuf.array();
                srcoffset += ibuf.arrayOffset();
            } else {
                srcbuf = new int[srcw];
                ibuf.position(ibuf.position() + srcoffset);
                srcoffset = 0;
            }
            for (int sy = 0; sy < srch; sy++) {
                if (!ibuf.hasArray()) {
                    ibuf.get(srcbuf);
                    if (srcrowelems - srcw != 0) {
                        ibuf.position(ibuf.position() + (srcrowelems - srcw));
                    }
                    srcoffset = 0;
                }
                for (int sx = 0; sx < srcw; sx++) {
                    int argb = srcbuf[srcoffset + sx];
                    if (!hasalpha) {
                        argb |= 0xff000000;
                    } else if (!ispremult) {
                        int a = argb >>> 24;
                        if (a == 0) {
                            argb = 0;
                        } else if (a != 0xff) {
                            int r = (argb >> 16) & 0xff;
                            int g = (argb >>  8) & 0xff;
                            int b = (argb      ) & 0xff;
                            r = r * a / 255;
                            g = g * a / 255;
                            b = b * a / 255;
                            argb = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                    dstbuf[dstoffset + sx] = argb;
                }
                srcoffset += srcrowelems;
                dstoffset += dstscan;
            }
        }
    }

    static class ThreeByteBgrUpdater extends Updater {
        static ThreeByteBgrUpdater THREE_BYTE_BGR_INSTANCE =
            new ThreeByteBgrUpdater();

        private ThreeByteBgrUpdater() {}

        @Override
        void updateFromByteBuffer(BufferedImage bimg, ByteBuffer bbuf,
                                  int dstx, int dsty,
                                  int srcx, int srcy,
                                  int srcw, int srch, int srcrowelems,
                                  int roff, int goff, int boff, int aoff,
                                  int srcpixelbytes, boolean ispremult)
        {
            byte dstbuf[] = ((java.awt.image.DataBufferByte)
                             bimg.getRaster().getDataBuffer()).getData();
            int dstscan = bimg.getWidth() * 3;
            int dstoffset = dsty * dstscan + dstx * 3;
            byte srcbuf[];
            int srcoffset = srcy * srcrowelems + srcx * srcpixelbytes;
            if (bbuf.hasArray()) {
                srcbuf = bbuf.array();
                srcoffset += bbuf.arrayOffset();
            } else {
                srcbuf = new byte[srcw * srcpixelbytes];
                bbuf.position(bbuf.position() + srcoffset);
                srcoffset = 0;
            }
            srcrowelems -= srcpixelbytes * srcw;
            dstscan -= 3 * srcw;
            for (int sy = 0; sy < srch; sy++) {
                if (!bbuf.hasArray()) {
                    bbuf.get(srcbuf);
                    if (srcrowelems != 0) {
                        bbuf.position(bbuf.position() + srcrowelems);
                    }
                    srcoffset = 0;
                }
                for (int sx = 0; sx < srcw; sx++) {
                    dstbuf[dstoffset    ] = srcbuf[srcoffset + boff];
                    dstbuf[dstoffset + 1] = srcbuf[srcoffset + goff];
                    dstbuf[dstoffset + 2] = srcbuf[srcoffset + roff];
                    srcoffset += srcpixelbytes;
                    dstoffset += 3;
                }
                srcoffset += srcrowelems;
                dstoffset += dstscan;
            }
        }
    }
}
