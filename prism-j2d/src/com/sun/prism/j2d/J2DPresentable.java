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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.prism.Graphics;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class J2DPresentable implements Presentable {
    static J2DPresentable create(PresentableState pState,
                                         J2DResourceFactory factory)
    {
        return new Glass(pState, factory);
    }

    static J2DPresentable create(BufferedImage buffer, J2DResourceFactory factory)
    {
        return new Bimg(buffer, factory);
    }


    private static class Glass extends J2DPresentable {
        private final PresentableState pState;
        private Pixels pixels;
        private IntBuffer pixBuf;
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private boolean opaque;

        Glass(PresentableState pState, J2DResourceFactory factory) {
            super(null, factory);
            this.pState = pState;
            setNeedsResize();
        }

        @Override
        public BufferedImage createBuffer(int w, int h) {
            pixels = null;
            pixBuf = null;
            int format = Pixels.getNativeFormat();
            if (PrismSettings.verbose) {
                System.out.println("Glass native format: "+format);
            }
            ByteOrder byteorder = ByteOrder.nativeOrder();
            switch (format) {
                case Pixels.Format.BYTE_BGRA_PRE:
                    if (byteorder == ByteOrder.LITTLE_ENDIAN) {
                        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                    } else {
                        throw new UnsupportedOperationException("BYTE_BGRA_PRE pixel format on BIG_ENDIAN");
                    }
                    /* NOTREACHED */
                case Pixels.Format.BYTE_ARGB:
                    if (byteorder == ByteOrder.BIG_ENDIAN) {
                        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    } else {
                        throw new UnsupportedOperationException("BYTE_ARGB pixel format on LITTLE_ENDIAN");
                    }
                    /* NOTREACHED */
                default:
                    throw new UnsupportedOperationException("unrecognized pixel format: "+format);
            }
        }

        private final Application app = Application.GetApplication();

        public boolean prepare(Rectangle dirty) {
            if (pState.isViewClosed() == false) {
                /*
                 * RT-27385
                 * TODO: make sure the imgrep matches the Pixels.getNativeFormat()
                 * TODO: dirty region support
                 */
                int w = getPhysicalWidth();
                int h = getPhysicalHeight();
                if (pixels == null || uploadCount.get() > 0) {
                    pixBuf = IntBuffer.allocate(w*h);
                    pixels = Application.GetApplication().createPixels(w, h, pixBuf);
                }
                assert ib.hasArray();
                System.arraycopy(ib.array(), 0, pixBuf.array(), 0, w*h);
                return true;
            } else {
                return (false);
            }
        }

        public boolean present() {
            final Pixels pixelsFin = pixels;
            uploadCount.incrementAndGet();
            Application.invokeLater(new Runnable() {
                @Override public void run() {
                   if (!pState.isViewClosed()) {
                        pState.getView().uploadPixels(pixelsFin);
                    }
                    uploadCount.decrementAndGet();
                }
            });
            return true;
        }

        public int getContentWidth() {
            return pState.getWidth();
        }

        public int getContentHeight() {
            return pState.getHeight();
        }

        public void setOpaque(boolean opaque) {
            this.opaque = opaque;
        }

        public boolean isOpaque() {
            return opaque;
        }
    }

    private static class Bimg extends J2DPresentable {
        private boolean opaque;
        public Bimg(java.awt.image.BufferedImage buffer,
                    J2DResourceFactory factory) {
            super(buffer, factory);
        }

        @Override
        public BufferedImage createBuffer(int w, int h) {
            throw new UnsupportedOperationException("cannot create new buffers for image");
        }

        public boolean prepare(Rectangle dirtyregion) {
            throw new UnsupportedOperationException("cannot prepare/present on image");
        }

        public boolean present() {
            throw new UnsupportedOperationException("cannot prepare/present on image");
        }

        public int getContentWidth() {
            return buffer.getWidth();
        }

        public int getContentHeight() {
            return buffer.getHeight();
        }

       public void setOpaque(boolean opaque) {
            this.opaque = opaque;
        }

        public boolean isOpaque() {
            return opaque;
        }
    }

    J2DResourceFactory factory;
    boolean needsResize;
    java.awt.image.BufferedImage buffer;
    IntBuffer ib;
    J2DRTTexture readbackBuffer;

    J2DPresentable(java.awt.image.BufferedImage buffer,
                   J2DResourceFactory factory)
    {
        this.buffer = buffer;
        this.factory = factory;
    }

    public boolean lockResources() {
        return false;
    }

    public void setNeedsResize() {
        this.needsResize = true;
    }

    ResourceFactory getResourceFactory() {
        return factory;
    }

    public abstract BufferedImage createBuffer(int w, int h);

    public Graphics createGraphics() {
        // RT-27385
        // TODO: Figure out why needsResize is not always set appropriately
        if (true || needsResize) {
            int w = getContentWidth();
            int h = getContentHeight();
            // TODO: Have Glass Pixels class relax its constraints
            // so we can use an oversized buffer if we want to...
            if (buffer == null ||
                buffer.getWidth() != w ||
                buffer.getHeight() != h)
            {
                buffer = null;
                readbackBuffer = null;
                buffer = createBuffer(w, h);
                java.awt.image.Raster r = buffer.getRaster();
                java.awt.image.DataBuffer db = r.getDataBuffer();
                java.awt.image.SinglePixelPackedSampleModel sppsm =
                    (java.awt.image.SinglePixelPackedSampleModel) r.getSampleModel();
                int pixels[] = ((java.awt.image.DataBufferInt) db).getData();
                ib = IntBuffer.wrap(pixels, db.getOffset(), db.getSize());
            }
            needsResize = false;
        }
        Graphics2D g2d = (Graphics2D) buffer.getGraphics();
        return new J2DPrismGraphics(this, g2d);
    }

    J2DRTTexture getReadbackBuffer() {
        if (readbackBuffer == null) {
            readbackBuffer = (J2DRTTexture)
                factory.createRTTexture(getContentWidth(), getContentHeight(),
                                        WrapMode.CLAMP_NOT_NEEDED);
            readbackBuffer.makePermanent();
        }
        return readbackBuffer;
    }

    BufferedImage getBackBuffer() {
        return buffer;
    }

    public Screen getAssociatedScreen() {
        return factory.getScreen();
    }

    public int getContentX() {
        return 0;
    }

    public int getContentY() {
        return 0;
    }

    public float getPixelScaleFactor() {
        return 1.0f;
    }

    public int getPhysicalWidth() {
        // If the buffer has not yet been created (typically that means
        // createGraphics() has not yet been called), we will plan to
        // create it at the content size initially.
        return (buffer == null) ? getContentWidth() : buffer.getWidth();
    }

    public int getPhysicalHeight() {
        // If the buffer has not yet been created (typically that means
        // createGraphics() has not yet been called), we will plan to
        // create it at the content size initially.
        return (buffer == null) ? getContentHeight() : buffer.getHeight();
    }

    public boolean recreateOnResize() {
        return false;
    }
}
