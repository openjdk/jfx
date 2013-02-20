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

package javafx.scene.image;

import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

public final class TestImages {
    public static final Image TEST_IMAGE_0x100;
    public static final Image TEST_IMAGE_100x0;
    public static final Image TEST_IMAGE_100x200;
    public static final Image TEST_IMAGE_200x100;

    public static final Image TEST_IMAGE_32x32;
    public static final Image TEST_IMAGE_32x64;
    public static final Image TEST_IMAGE_64x32;
    public static final Image TEST_IMAGE_64x64;

    public static final Image TEST_ERROR_IMAGE;

    private static final StubImageLoaderFactory imageLoaderFactory;

    private TestImages() {
    }

    static {
        imageLoaderFactory =
                ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

        TEST_IMAGE_0x100 = createTestImage(0, 100);
        TEST_IMAGE_100x0 = createTestImage(100, 0);
        TEST_IMAGE_100x200 = createTestImage(100, 200);
        TEST_IMAGE_200x100 = createTestImage(200, 100);

        TEST_IMAGE_32x32 = createTestImage(32, 32);
        TEST_IMAGE_32x64 = createTestImage(32, 64);
        TEST_IMAGE_64x32 = createTestImage(64, 32);
        TEST_IMAGE_64x64 = createTestImage(64, 64);

        TEST_ERROR_IMAGE = new Image("file:error.png");
    }

    public static Image createTestImage(
            final int width,
            final int height) {
        final String url = "file:testImg_" + width + "x" + height + ".png";
        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(width, height));

        return new Image(url);
    }

    public static Image createAnimatedTestImage(
            final int width,
            final int height,
            final int... frameDelays) {
        final String url = "file:testAnimImg_" + width + "x" + height + ".png";
        final StubPlatformImageInfo spii = 
                new StubPlatformImageInfo(width, height, frameDelays);
        imageLoaderFactory.registerImage(url, spii);

        return new Image(url);
    }

    /**
     * Stops animation timeline on the image. Used to prevent problems with
     * rewiding animation time.
     *
     * @param image the animated image to dispose
     */
    public static void disposeAnimatedImage(final Image image) {
        image.dispose();
    }
}
