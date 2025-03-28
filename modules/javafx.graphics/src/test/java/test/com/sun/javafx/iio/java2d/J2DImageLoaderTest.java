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

package test.com.sun.javafx.iio.java2d;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage.ImageType;
import com.sun.javafx.iio.java2d.J2DImageLoader;
import com.sun.javafx.iio.java2d.J2DImageLoaderFactory;
import com.sun.prism.Image;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import com.sun.prism.PixelFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class J2DImageLoaderTest {

    private int color(int r, int g, int b) {
        return 255 << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    private void assertImageContent(Image image) {
        assertEquals(color(255, 0, 0), image.getArgb(0, 0));
        assertEquals(color(0, 255, 0), image.getArgb(5, 0));
        assertEquals(color(0, 0, 255), image.getArgb(9, 0));

        assertEquals(color(0, 255, 255), image.getArgb(0, 5));
        assertEquals(color(255, 0, 255), image.getArgb(4, 5));
        assertEquals(color(255, 255, 0), image.getArgb(9, 5));

        assertEquals(color(255, 128, 128), image.getArgb(0, 9));
        assertEquals(color(128, 128, 255), image.getArgb(4, 9));
        assertEquals(color(143, 240, 128), image.getArgb(9, 9));
    }

    @Test
    void loadImageBGR() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker.bmp")) {
            ImageLoader loader = J2DImageLoaderFactory.getInstance().createImageLoader(stream);
            ImageFrame frame = loader.load(0, -1, -1, true, false, 1, 1);
            Image image = Image.convertImageFrame(frame);

            assertEquals(12, frame.getWidth());
            assertEquals(12, frame.getHeight());
            assertEquals(1, frame.getPixelScale());
            assertEquals(ImageType.BGR, frame.getImageType());

            assertEquals(12, image.getWidth());
            assertEquals(12, image.getHeight());
            assertEquals(PixelFormat.BYTE_RGB, image.getPixelFormat());
            assertImageContent(image);
        }
    }

    @Test
    void loadImageABGR() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker.png")) {
            ImageLoader loader = J2DImageLoaderFactory.getInstance().createImageLoader(stream);
            ImageFrame frame = loader.load(0, -1, -1, true, false, 1, 1);
            Image image = Image.convertImageFrame(frame);

            assertEquals(12, frame.getWidth());
            assertEquals(12, frame.getHeight());
            assertEquals(1, frame.getPixelScale());
            assertEquals(ImageType.ABGR, frame.getImageType());

            assertEquals(12, image.getWidth());
            assertEquals(12, image.getHeight());
            assertEquals(PixelFormat.BYTE_BGRA_PRE, image.getPixelFormat());
            assertImageContent(image);
        }
    }

    @Test
    void loadImageABGR2x() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker@2x.png")) {
            ImageLoader loader = J2DImageLoaderFactory.getInstance().createImageLoader(stream);
            ImageFrame frame = loader.load(0, -1, -1, true, false, 1, 2);
            Image image = Image.convertImageFrame(frame);

            assertEquals(24, frame.getWidth());
            assertEquals(24, frame.getHeight());
            assertEquals(2, frame.getPixelScale());
            assertEquals(ImageType.ABGR, frame.getImageType());

            assertEquals(24, image.getWidth());
            assertEquals(24, image.getHeight());
            assertEquals(PixelFormat.BYTE_BGRA_PRE, image.getPixelFormat());
            assertImageContent(image);
        }
    }

    enum AwtImageTypes {
        TYPE_BYTE_GRAY(BufferedImage.TYPE_BYTE_GRAY, ImageType.GRAY),
        TYPE_3BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR, ImageType.BGR),
        TYPE_4BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR, ImageType.ABGR),
        TYPE_4BYTE_ABGR_PRE(BufferedImage.TYPE_4BYTE_ABGR_PRE, ImageType.ABGR_PRE),
        TYPE_INT_RGB(BufferedImage.TYPE_INT_RGB, ImageType.INT_RGB),
        TYPE_INT_BGR(BufferedImage.TYPE_INT_BGR, ImageType.INT_BGR),
        TYPE_INT_ARGB(BufferedImage.TYPE_INT_ARGB, ImageType.INT_ARGB),
        TYPE_INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE, ImageType.INT_ARGB_PRE);

        AwtImageTypes(int awtImageType, ImageType fxImageType) {
            this.awtIimageType = awtImageType;
            this.fxImageType = fxImageType;
        }

        final int awtIimageType;
        final ImageType fxImageType;
    }

    @ParameterizedTest
    @EnumSource(AwtImageTypes.class)
    void loadVariableDensityImage(AwtImageTypes types) throws IOException {
        class CustomImageReader extends ImageReader {
            final int imageType;

            protected CustomImageReader(int imageType) {
                super(null);
                this.imageType = imageType;
            }

            @Override public String getFormatName() { return "test"; }
            @Override public int getNumImages(boolean allowSearch) { return 1; }
            @Override public int getWidth(int imageIndex) { return 2; }
            @Override public int getHeight(int imageIndex) { return 2; }
            @Override public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) { return null; }
            @Override public IIOMetadata getStreamMetadata() { return null; }
            @Override public IIOMetadata getImageMetadata(int imageIndex) { return null; }

            @Override
            public ImageReadParam getDefaultReadParam() {
                return new ImageReadParam() {{ canSetSourceRenderSize = true; }};
            }

            @Override
            public BufferedImage read(int imageIndex, ImageReadParam param) {
                int w = (int)param.getSourceRenderSize().getWidth();
                int h = (int)param.getSourceRenderSize().getHeight();
                return new BufferedImage(w, h, imageType);
            }
        }

        var imageLoader = new J2DImageLoader(new CustomImageReader(types.awtIimageType), null);

        // Load an image with screenPixelScale == 1
        var imageFrame = imageLoader.load(0, -1, -1, false, false, 1, 1);
        assertEquals(2, imageFrame.getWidth());
        assertEquals(2, imageFrame.getHeight());
        assertEquals(1, imageFrame.getPixelScale());
        assertEquals(types.fxImageType, imageFrame.getImageType());

        // Load an image with screenPixelScale == 1.5
        imageFrame = imageLoader.load(0, -1, -1, false, false, 1.5f, 1);
        assertEquals(3, imageFrame.getWidth());
        assertEquals(3, imageFrame.getHeight());
        assertEquals(1.5, imageFrame.getPixelScale());
        assertEquals(types.fxImageType, imageFrame.getImageType());

        // Load an image with screenPixelScale == 2
        imageFrame = imageLoader.load(0, -1, -1, false, false, 2, 1);
        assertEquals(4, imageFrame.getWidth());
        assertEquals(4, imageFrame.getHeight());
        assertEquals(2, imageFrame.getPixelScale());
        assertEquals(types.fxImageType, imageFrame.getImageType());
    }

    @Test
    void addAndRemoveListener() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker.png")) {
            ImageReader pngReader = ImageIO.getImageReadersByFormatName("PNG").next();
            ImageInputStream input = ImageIO.createImageInputStream(stream);
            pngReader.setInput(input);
            J2DImageLoader loader = new J2DImageLoader(pngReader, input);
            ImageLoadListener listener = new ImageLoadListener() {
                @Override public void imageLoadProgress(ImageLoader loader, float percentageComplete) {}
                @Override public void imageLoadWarning(ImageLoader loader, String message) {}
                @Override public void imageLoadMetaData(ImageLoader loader, ImageMetadata metadata) {}
            };

            Field progressListenersField = ImageReader.class.getDeclaredField("progressListeners");
            Field warningListenersField = ImageReader.class.getDeclaredField("warningListeners");
            progressListenersField.setAccessible(true);
            warningListenersField.setAccessible(true);
            assertNull(progressListenersField.get(pngReader));
            assertNull(warningListenersField.get(pngReader));

            loader.addListener(listener);
            assertEquals(1, ((List<?>)progressListenersField.get(pngReader)).size());
            assertEquals(1, ((List<?>)warningListenersField.get(pngReader)).size());

            loader.removeListener(listener);
            assertNull(progressListenersField.get(pngReader));
            assertNull(warningListenersField.get(pngReader));
        }
    }

    @Test
    void testProgressListener() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker.png")) {
            List<Float> progress = new ArrayList<>();
            ImageLoader loader = J2DImageLoaderFactory.getInstance().createImageLoader(stream);
            loader.addListener(new ImageLoadListener() {
                @Override
                public void imageLoadProgress(ImageLoader loader, float percentageComplete) {
                    progress.add(percentageComplete);
                }

                @Override public void imageLoadWarning(ImageLoader loader, String message) {}
                @Override public void imageLoadMetaData(ImageLoader loader, ImageMetadata metadata) {}
            });

            loader.load(0, -1, -1, true, false, 1, 1);

            assertTrue(progress.size() > 1);
            assertEquals(0, progress.get(0));
            assertEquals(100, progress.get(progress.size() - 1));

            List<Float> orderedProgress = new ArrayList<>(progress);
            orderedProgress.sort(Comparator.naturalOrder());
            assertEquals(orderedProgress, progress);
        }
    }
}
