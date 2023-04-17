/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.javax;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.common.ImageDescriptor;
import com.sun.javafx.iio.common.ImageTools;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public class XImageLoader implements ImageLoader {

    private static final Map<ImageReaderSpi, String[]> mimeSubtypes = new WeakHashMap<>();

    private final ImageReader reader;
    private final ImageInputStream stream;
    private final ImageFormatDescription description;

    public XImageLoader(ImageReader reader, ImageInputStream stream) throws IOException {
        this.reader = reader;
        this.stream = stream;
        String[] subtypes;

        synchronized (mimeSubtypes) {
            ImageReaderSpi originatingProvider = reader.getOriginatingProvider();
            subtypes = mimeSubtypes.get(originatingProvider);
            if (subtypes == null) {
                subtypes = Arrays.stream(originatingProvider.getMIMETypes())
                    .map(type -> type.substring(type.indexOf('/')))
                    .toArray(String[]::new);

                mimeSubtypes.put(originatingProvider, subtypes);
            }
        }

        this.description = new ImageDescriptor(
            reader.getFormatName(), new String[0], new ImageFormatDescription.Signature[0], subtypes);
    }

    @Override
    public ImageFormatDescription getFormatDescription() {
        return description;
    }

    @Override
    public void dispose() {
        reader.dispose();

        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void addListener(ImageLoadListener listener) {
    }

    @Override
    public void removeListener(ImageLoadListener listener) {
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
            int[] widthHeight = ImageTools.computeDimensions(
                (int)(imageWidth * screenPixelScale), (int)(imageHeight * screenPixelScale),
                (int)(w * screenPixelScale), (int)(h * screenPixelScale),
                preserveAspectRatio);

            width = widthHeight[0];
            height = widthHeight[1];
            pixelScale = screenPixelScale;
            param.setSourceRenderSize(new Dimension(width, height));
        } else {
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

        ImageFrame frame = BufferedImageConverter.convert(image, metadata);
        frame.setPixelScale(pixelScale);

        return frame;
    }

}
