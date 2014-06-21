/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import static javafx.scene.image.TestImages.TEST_IMAGE_0x100;
import static javafx.scene.image.TestImages.TEST_IMAGE_100x0;
import static javafx.scene.image.TestImages.TEST_IMAGE_100x200;
import static javafx.scene.image.TestImages.TEST_IMAGE_200x100;
import static javafx.scene.image.TestImages.TEST_IMAGE_32x32;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.image.TestImages;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.pgstub.CursorSizeConverter;
import com.sun.javafx.pgstub.StubAsyncImageLoader;
import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.test.PropertyInvalidationCounter;
import com.sun.javafx.tk.Toolkit;

public final class ImageCursorTest {
    private static StubToolkit toolkit;

    private static CursorSizeConverter oldCursorSizeConverter;
    private static int oldMaximumCursorColors;

    @BeforeClass
    public static void setUpClass() {
        toolkit = (StubToolkit) Toolkit.getToolkit();

        oldCursorSizeConverter = toolkit.getCursorSizeConverter();
        oldMaximumCursorColors = toolkit.getMaximumCursorColors();
    }

    @AfterClass
    public static void tearDownClass() {
        toolkit.setCursorSizeConverter(oldCursorSizeConverter);
        toolkit.setMaximumCursorColors(oldMaximumCursorColors);
    }

    @Test
    public void constructionTest() {
        assertCursorEquals(new ImageCursor(), null, 0, 0);
        assertCursorEquals(new ImageCursor(null), null, 0, 0);
        assertCursorEquals(new ImageCursor(null, 100, 200), null, 0, 0);
        assertCursorEquals(new ImageCursor(TEST_IMAGE_0x100, 100, 200),
                           TestImages.TEST_IMAGE_0x100, 0, 0);
        assertCursorEquals(new ImageCursor(TEST_IMAGE_100x0, 200, 100),
                           TestImages.TEST_IMAGE_100x0, 0, 0);
        assertCursorEquals(new ImageCursor(TEST_IMAGE_100x200, 20, 10),
                           TestImages.TEST_IMAGE_100x200, 20, 10);
        assertCursorEquals(new ImageCursor(TEST_IMAGE_200x100, 10, 20),
                           TestImages.TEST_IMAGE_200x100, 10, 20);
        assertCursorEquals(new ImageCursor(TEST_IMAGE_100x200, -50, -100),
                           TestImages.TEST_IMAGE_100x200, 0, 0);
        assertCursorEquals(new ImageCursor(TEST_IMAGE_200x100, 300, 150),
                           TestImages.TEST_IMAGE_200x100, 199, 99);
    }

    @Test
    public void constructionAsyncImageTest() {
        final StubImageLoaderFactory imageLoaderFactory =
                toolkit.getImageLoaderFactory();

        final String url = "file:test.png";
        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(48, 48));

        final Image testImage = new Image(url, true);
        final StubAsyncImageLoader asyncImageLoader =
                imageLoaderFactory.getLastAsyncImageLoader();
        final ImageCursor testCursor = new ImageCursor(testImage, 100, 24);

        asyncImageLoader.finish();

        assertCursorEquals(testCursor, testImage, 47, 24);
    }
    
    @Test
    public void getBestSizeTest() {
        toolkit.setCursorSizeConverter(CursorSizeConverter.IDENTITY_CONVERTER);

        assertEquals(new Dimension2D(10, 20), ImageCursor.getBestSize(10, 20));
        assertEquals(new Dimension2D(20, 10), ImageCursor.getBestSize(20, 10));

        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(32, 24));
        assertEquals(new Dimension2D(32, 24), ImageCursor.getBestSize(10, 20));

        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(24, 32));
        assertEquals(new Dimension2D(24, 32), ImageCursor.getBestSize(20, 10));
    }

    @Test
    public void getMaximumColorsTest() {
        toolkit.setMaximumCursorColors(16);
        assertEquals(16, ImageCursor.getMaximumColors());

        toolkit.setMaximumCursorColors(256);
        assertEquals(256, ImageCursor.getMaximumColors());
    }

    @Test
    public void findBestImageSpecialCasesTest() {
        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(16, 16));
        assertCursorEquals(ImageCursor.chooseBestCursor(new Image[0], 10, 20),
                           null, 0, 0);

        final Image[] deformedImages = { TEST_IMAGE_0x100, TEST_IMAGE_100x0 };
        assertCursorEquals(ImageCursor.chooseBestCursor(deformedImages, 0, 0),
                           TEST_IMAGE_0x100, 0, 0);

        final Image[] singleImage = { TEST_IMAGE_200x100 };
        assertCursorEquals(ImageCursor.chooseBestCursor(singleImage, 10, 20),
                           TEST_IMAGE_200x100, 10, 20);

        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(0, 16));
        assertCursorEquals(ImageCursor.chooseBestCursor(singleImage, 10, 20),
                           null, 0, 0);

        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(16, 0));
        assertCursorEquals(ImageCursor.chooseBestCursor(singleImage, 10, 20),
                           null, 0, 0);

        toolkit.setCursorSizeConverter(
                new CursorSizeConverter() {
                    @Override
                    public Dimension2D getBestCursorSize(
                            final int preferredWidth,
                            final int preferredHeight) {
                        if (preferredWidth < preferredHeight) {
                            return new Dimension2D(preferredWidth, 0);
                        } else if (preferredWidth > preferredHeight) {
                            return new Dimension2D(0, preferredHeight);
                        }

                        return new Dimension2D(16, 16);
                    }
                });
        final Image[] twoImages = { TEST_IMAGE_100x200, TEST_IMAGE_200x100 };
        assertCursorEquals(ImageCursor.chooseBestCursor(twoImages, 0, 0),
                           TEST_IMAGE_100x200, 0, 0);
    }

    @Test
    public void findBestImageFromAsyncImagesTest() {
        final StubImageLoaderFactory imageLoaderFactory =
                toolkit.getImageLoaderFactory();

        final String url32x48 = "file:image_32x48.png";
        imageLoaderFactory.registerImage(
                url32x48, new StubPlatformImageInfo(32, 48));

        final String url48x48 = "file:image_48x48.png";
        imageLoaderFactory.registerImage(
                url48x48, new StubPlatformImageInfo(48, 48));

        final Image asyncImage32x48 = new Image(url32x48, true);
        final StubAsyncImageLoader asyncImage32x48loader =
                imageLoaderFactory.getLastAsyncImageLoader();
        
        final Image asyncImage48x48 = new Image(url48x48, true);
        final StubAsyncImageLoader asyncImage48x48loader =
                imageLoaderFactory.getLastAsyncImageLoader();

        final Image[] cursorImages = {
                TEST_IMAGE_32x32,
                asyncImage32x48,
                asyncImage48x48
        };

        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(48, 48));
        final ImageCursor selectedCursor =
                ImageCursor.chooseBestCursor(cursorImages, 32, 32);

        asyncImage32x48loader.setProgress(50, 100);
        asyncImage32x48loader.finish();
        asyncImage48x48loader.finish();

        assertCursorEquals(selectedCursor,
                           asyncImage48x48, 47, 47);
    }

    @Test
    public void imageCursorPropertyListenersCalledOnceTest() {
        final StubImageLoaderFactory imageLoaderFactory =
                toolkit.getImageLoaderFactory();

        final String url = "file:test.png";
        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(32, 32));

        final Image testImage = new Image(url, true);
        final StubAsyncImageLoader asyncImageLoader =
                imageLoaderFactory.getLastAsyncImageLoader();
        final ImageCursor testCursor = new ImageCursor(testImage, 16, 16);

        final PropertyInvalidationCounter<Number> hotspotXInvalidationCounter =
                new PropertyInvalidationCounter<Number>();
        final PropertyInvalidationCounter<Number> hotspotYInvalidationCounter =
                new PropertyInvalidationCounter<Number>();
        final PropertyInvalidationCounter<Object> imageInvalidationCounter =
                new PropertyInvalidationCounter<Object>();

        testCursor.hotspotXProperty().addListener(hotspotXInvalidationCounter);
        testCursor.hotspotYProperty().addListener(hotspotYInvalidationCounter);
        testCursor.imageProperty().addListener(imageInvalidationCounter);

        assertCursorEquals(testCursor, null, 0, 0);

        asyncImageLoader.finish();

        assertEquals(1, hotspotXInvalidationCounter.getCounter());
        assertEquals(1, hotspotYInvalidationCounter.getCounter());
        assertEquals(1, imageInvalidationCounter.getCounter());
    }

    @Test
    public void imageCursorPropertiesChangedAtomicallyTest() {
        final StubImageLoaderFactory imageLoaderFactory =
                toolkit.getImageLoaderFactory();

        final String url = "file:test.png";
        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(32, 32));

        final Image testImage = new Image(url, true);
        final StubAsyncImageLoader asyncImageLoader =
                imageLoaderFactory.getLastAsyncImageLoader();
        final ImageCursor testCursor = new ImageCursor(testImage, 16, 16);

        final InvalidationListener imageCursorChecker =
                observable -> assertCursorEquals(testCursor, testImage, 16, 16);

        testCursor.hotspotXProperty().addListener(imageCursorChecker);
        testCursor.hotspotYProperty().addListener(imageCursorChecker);
        testCursor.imageProperty().addListener(imageCursorChecker);

        asyncImageLoader.finish();
    }

    public static void assertCursorEquals(final ImageCursor cursor,
                                          final Image image,
                                          final float hotspotX,
                                          final float hotspotY) {
        assertSame(image, cursor.getImage());
        assertEquals(hotspotX, cursor.getHotspotX(), 0);
        assertEquals(hotspotY, cursor.getHotspotY(), 0);
    }
}
