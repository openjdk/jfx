/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Base64;
import java.util.Iterator;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import com.sun.javafx.webkit.UIClientImpl;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.webkit.graphics.WCImage;

/**
 * @author Alexey.Ushakov
 */
abstract class PrismImage extends WCImage {
    abstract Image getImage();
    abstract Graphics getGraphics();
    abstract void draw(Graphics g,
            int dstx1, int dsty1, int dstx2, int dsty2,
            int srcx1, int srcy1, int srcx2, int srcy2);
    abstract void dispose();

    @Override
    public Object getPlatformImage() {
       return getImage();
    }

    @Override
    public void deref() {
        super.deref();
        if (!hasRefs()) {
            dispose();
        }
    }

    @Override
    protected final byte[] toData(String mimeType) {
        final BufferedImage image = toBufferedImage(mimeType.equals("image/jpeg"));
        if (image != null) {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(mimeType);
            while (it.hasNext()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageWriter writer = it.next();
                try {
                    writer.setOutput(ImageIO.createImageOutputStream(output));
                    writer.write((BufferedImage) image);
                }
                catch (IOException exception) {
                    continue; // try next image writer
                }
                finally {
                    writer.dispose();
                }
                return output.toByteArray();
            }
        }
        return null;
    }

    @Override
    protected final String toDataURL(String mimeType) {
        final byte[] data = toData(mimeType);
        if (data != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("data:").append(mimeType).append(";base64,");
            sb.append(Base64.getMimeEncoder().encodeToString(data));
            return sb.toString();
        }
        return null;
    }

    private static int
            getBestBufferedImageType(PixelFormat<?> fxFormat)
    {
        switch (fxFormat.getType()) {
            default:
            case BYTE_BGRA_PRE:
            case INT_ARGB_PRE:
                return BufferedImage.TYPE_INT_ARGB_PRE;
            case BYTE_BGRA:
            case INT_ARGB:
                return BufferedImage.TYPE_INT_ARGB;
            case BYTE_RGB:
                return BufferedImage.TYPE_INT_RGB;
            case BYTE_INDEXED:
                return (fxFormat.isPremultiplied()
                        ? BufferedImage.TYPE_INT_ARGB_PRE
                        : BufferedImage.TYPE_INT_ARGB);
        }
    }

    private static WritablePixelFormat<IntBuffer>
        getAssociatedPixelFormat(BufferedImage bimg)
    {
        switch (bimg.getType()) {
            // We lie here for xRGB, but we vetted that the src data was opaque
            // so we can ignore the alpha.  We use ArgbPre instead of Argb
            // just to get a loop that does not have divides in it if the
            // PixelReader happens to not know the data is opaque.
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return PixelFormat.getIntArgbPreInstance();
            case BufferedImage.TYPE_INT_ARGB:
                return PixelFormat.getIntArgbInstance();
            default:
                // Should not happen...
                throw new InternalError("Failed to validate BufferedImage type");
        }
    }

    private static BufferedImage fromFXImage(Image img, boolean forceRGB) {
        final int iw = (int) img.getWidth();
        final int ih = (int) img.getHeight();
        final int destImageType = forceRGB ? BufferedImage.TYPE_INT_RGB : getBestBufferedImageType(img.getPlatformPixelFormat());
        final BufferedImage bimg = new BufferedImage(iw, ih, destImageType);
        final DataBufferInt db = (DataBufferInt) bimg.getRaster().getDataBuffer();
        final int data[] = db.getData();
        final int offset = bimg.getRaster().getDataBuffer().getOffset();
        int scan =  0;
        final SampleModel sm = bimg.getRaster().getSampleModel();
        if (sm instanceof SinglePixelPackedSampleModel) {
            scan = ((SinglePixelPackedSampleModel)sm).getScanlineStride();
        }

        final WritablePixelFormat<IntBuffer> pf = getAssociatedPixelFormat(bimg);
        img.getPixels(0, 0, iw, ih, pf, data, offset, scan);
        return bimg;
    }

    private BufferedImage toBufferedImage(boolean forceRGB) {
        try {
            return fromFXImage(getImage(), forceRGB);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

        // return null upon any exception
        return null;
    }

    @Override
    public BufferedImage toBufferedImage() {
        return toBufferedImage(false);
    }

}
