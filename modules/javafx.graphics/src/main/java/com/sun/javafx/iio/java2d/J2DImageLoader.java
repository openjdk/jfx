/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.java2d;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage.ImageType;
import com.sun.javafx.iio.ImageStorageException;
import com.sun.javafx.iio.common.ImageDescriptor;
import com.sun.javafx.iio.common.ImageTools;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Objects;

import static java.awt.image.BufferedImage.*;

public class J2DImageLoader implements ImageLoader {

    private final ImageReader reader;
    private final ImageInputStream stream;
    private final ImageFormatDescription description;

    public J2DImageLoader(ImageReader reader, ImageInputStream stream) throws IOException {
        this.reader = reader;
        this.stream = stream;
        this.description = new ImageDescriptor(
            reader.getFormatName(), new String[0], new ImageFormatDescription.Signature[0],
            reader.getOriginatingProvider() == null ? new String[0] :
                Arrays.stream(reader.getOriginatingProvider().getMIMETypes())
                    .map(type -> type.substring(type.indexOf('/')))
                    .toArray(String[]::new));

        reader.setInput(stream);
    }

    @Override
    public ImageFormatDescription getFormatDescription() {
        return description;
    }

    @Override
    public void dispose() {
        reader.dispose();

        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void addListener(ImageLoadListener listener) {
        var listenerImpl = new LoadListenerImpl(listener);
        reader.addIIOReadProgressListener(listenerImpl);
        reader.addIIOReadWarningListener(listenerImpl);
    }

    @Override
    public void removeListener(ImageLoadListener listener) {
        var listenerImpl = new LoadListenerImpl(listener);
        reader.removeIIOReadProgressListener(listenerImpl);
        reader.removeIIOReadWarningListener(listenerImpl);
    }

    @Override
    public ImageFrame load(int imageIndex, double w, double h, boolean preserveAspectRatio, boolean smooth,
                           float screenPixelScale, float imagePixelScale) throws IOException {
        // We currently don't support animated images loaded via ImageIO
        if (imageIndex != 0) {
            return null;
        }

        int width, height;
        float pixelScale;
        ImageReadParam param = reader.getDefaultReadParam();

        if (param.canSetSourceRenderSize()) {
            int imageWidth = reader.getWidth(imageIndex);
            int imageHeight = reader.getHeight(imageIndex);
            ImageTools.validateMaxDimensions(imageWidth, imageHeight, screenPixelScale);
            ImageTools.validateMaxDimensions(w, h, screenPixelScale);

            int[] widthHeight = ImageTools.computeDimensions(
                (int)(imageWidth * screenPixelScale), (int)(imageHeight * screenPixelScale),
                (int)(w * screenPixelScale), (int)(h * screenPixelScale),
                preserveAspectRatio);

            width = widthHeight[0];
            height = widthHeight[1];
            pixelScale = screenPixelScale;
            param.setSourceRenderSize(new Dimension(width, height));
        } else {
            ImageTools.validateMaxDimensions(w, h, imagePixelScale);

            int[] widthHeight = ImageTools.computeDimensions(
                reader.getWidth(imageIndex), reader.getHeight(imageIndex),
                (int)(w * imagePixelScale), (int)(h * imagePixelScale),
                preserveAspectRatio);

            width = widthHeight[0];
            height = widthHeight[1];
            pixelScale = imagePixelScale;
        }

        BufferedImage image = reader.read(imageIndex, param);

        if (image.getWidth() != width || image.getHeight() != height) {
            image = (BufferedImage)image.getScaledInstance(
                width, height, smooth ? Image.SCALE_SMOOTH : Image.SCALE_DEFAULT);
        }

        var metadata = new ImageMetadata(
            null, true, null, null, null,
            null, null, image.getWidth(), image.getHeight(), null,
            null, null);

        // Scanline stride is measured in elements of the underlying buffer, which can be bytes or ints.
        int scanlineStride = switch(image.getSampleModel()) {
            case ComponentSampleModel m -> m.getScanlineStride();
            case MultiPixelPackedSampleModel m -> m.getScanlineStride();
            case SinglePixelPackedSampleModel m -> m.getScanlineStride();
            default -> throw new IllegalStateException("Unsupported sample model: " + image.getSampleModel());
        };

        return switch (image.getType()) {
            case TYPE_BYTE_GRAY -> new ImageFrame(ImageType.GRAY,
                    getByteBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_3BYTE_BGR -> new ImageFrame(ImageType.BGR,
                    getByteBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_4BYTE_ABGR -> new ImageFrame(ImageType.ABGR,
                    getByteBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_4BYTE_ABGR_PRE -> new ImageFrame(ImageType.ABGR_PRE,
                    getByteBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_INT_RGB -> new ImageFrame(ImageType.INT_RGB,
                    getIntBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_INT_BGR -> new ImageFrame(ImageType.INT_BGR,
                    getIntBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_INT_ARGB -> new ImageFrame(ImageType.INT_ARGB,
                    getIntBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_INT_ARGB_PRE -> new ImageFrame(ImageType.INT_ARGB_PRE,
                    getIntBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    pixelScale, metadata);

            case TYPE_BYTE_BINARY, TYPE_BYTE_INDEXED -> {
                IndexColorModel colorModel = (IndexColorModel)image.getColorModel();
                int[] palette = new int[colorModel.getMapSize()];
                colorModel.getRGBs(palette);

                ImageType imageType = colorModel.hasAlpha()
                    ? colorModel.isAlphaPremultiplied()
                        ? ImageType.PALETTE_ALPHA_PRE
                        : ImageType.PALETTE_ALPHA
                    : ImageType.PALETTE;

                yield new ImageFrame(
                    imageType, getByteBuffer(image.getRaster().getDataBuffer()),
                    image.getWidth(), image.getHeight(), scanlineStride,
                    palette, colorModel.getPixelSize(), pixelScale, metadata);
            }

            default ->
                throw new ImageStorageException("Unsupported image type: " + switch (image.getType()) {
                    case TYPE_CUSTOM -> "TYPE_CUSTOM";
                    case TYPE_USHORT_565_RGB -> "TYPE_USHORT_565_RGB";
                    case TYPE_USHORT_555_RGB -> "TYPE_USHORT_555_RGB";
                    case TYPE_USHORT_GRAY -> "TYPE_USHORT_GRAY";
                    default -> Integer.toString(image.getType());
                });
        };
    }

    private static ByteBuffer getByteBuffer(DataBuffer buffer) {
        DataBufferByte byteBuffer = (DataBufferByte)buffer;
        byte[] data = byteBuffer.getData();
        int offset = byteBuffer.getOffset();
        int size = byteBuffer.getSize();
        return ByteBuffer.wrap(data, offset, size);
    }

    private static IntBuffer getIntBuffer(DataBuffer buffer) {
        DataBufferInt byteBuffer = (DataBufferInt)buffer;
        int[] data = byteBuffer.getData();
        int offset = byteBuffer.getOffset();
        int size = byteBuffer.getSize();
        return IntBuffer.wrap(data, offset, size);
    }

    private final class LoadListenerImpl implements IIOReadProgressListener, IIOReadWarningListener {
        private final ImageLoadListener listener;
        private float lastProgress;

        LoadListenerImpl(ImageLoadListener listener) {
            this.listener = listener;
        }

        @Override
        public void warningOccurred(ImageReader source, String warning) {
            listener.imageLoadWarning(J2DImageLoader.this, warning);
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            if (percentageDone > lastProgress) {
                lastProgress = percentageDone;
                listener.imageLoadProgress(J2DImageLoader.this, percentageDone);
            }
        }

        @Override public void imageStarted(ImageReader source, int imageIndex) {
            listener.imageLoadProgress(J2DImageLoader.this, 0);
        }

        @Override
        public void imageComplete(ImageReader source) {
            if (lastProgress < 100) {
                listener.imageLoadProgress(J2DImageLoader.this, 100);
            }
        }

        @Override public void sequenceStarted(ImageReader source, int minIndex) {}
        @Override public void sequenceComplete(ImageReader source) {}
        @Override public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {}
        @Override public void thumbnailProgress(ImageReader source, float percentageDone) {}
        @Override public void thumbnailComplete(ImageReader source) {}
        @Override public void readAborted(ImageReader source) {}

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LoadListenerImpl impl && Objects.equals(impl.listener, listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }
    }
}
