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

package test.com.sun.javafx.iio.javax;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.javax.XImageLoaderFactory;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import org.junit.jupiter.api.Test;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class XImageLoaderTest {

    private int color(int r, int g, int b) {
        return 255 << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    @Test
    void testLoadImage() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker.png")) {
            ImageLoader loader = XImageLoaderFactory.getInstance().createImageLoader(stream);
            ImageFrame frame = loader.load(0, -1, -1, true, false, 1, 1);

            assertEquals(12, frame.getWidth());
            assertEquals(12, frame.getHeight());
            assertEquals(1, frame.getPixelScale());
            assertEquals(ImageStorage.ImageType.ABGR, frame.getImageType());

            Image image = Image.convertImageFrame(frame);
            assertEquals(PixelFormat.BYTE_BGRA_PRE, image.getPixelFormat());

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
    }

    @Test
    void testLoadImage2x() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/test/com/sun/javafx/iio/checker@2x.png")) {
            ImageLoader loader = XImageLoaderFactory.getInstance().createImageLoader(stream);
            ImageFrame frame = loader.load(0, -1, -1, true, false, 1, 2);

            assertEquals(24, frame.getWidth());
            assertEquals(24, frame.getHeight());
            assertEquals(2, frame.getPixelScale());
            assertEquals(ImageStorage.ImageType.ABGR, frame.getImageType());

            Image image = Image.convertImageFrame(frame);
            assertEquals(24, image.getWidth());
            assertEquals(24, image.getHeight());
            assertEquals(PixelFormat.BYTE_BGRA_PRE, image.getPixelFormat());

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
    }

}
