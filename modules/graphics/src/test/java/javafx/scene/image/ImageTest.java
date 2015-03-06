/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.pgstub.*;
import com.sun.javafx.test.PropertyInvalidationCounter;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public final class ImageTest {
    private final StubToolkit toolkit;
    private final StubImageLoaderFactory imageLoaderFactory;

    public ImageTest() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        imageLoaderFactory = toolkit.getImageLoaderFactory();
    }

    @Before
    public void setUp() {
        imageLoaderFactory.reset();
    }

    @Test
    public void loadImageFromUrlBasicTest() {
        final String url = "file:test.png";
        registerImage(url, 100, 200);
        
        final Image image = new Image(url);

        assertEquals(url, image.impl_getUrl());
        verifyLoadedImage(image, 0, 0, false, false, 100, 200);
    }

    @Test
    public void loadImageFromUrlScaledTest() {
        final String url = "file:test.png";
        registerImage(url, 300, 100);

        final Image image = new Image(url, 200, 400, true, true);

        assertEquals(url, image.impl_getUrl());
        verifyLoadedImage(image, 200, 400, true, true, 300, 100);
    }

    @Test
    public void loadImageFromMissingUrlTest() {
        final Image image = new Image("file:missing.png");

        assertTrue(image.isError());
    }

    @Test
    public void loadImageFromStreamBasicTest() {
        final InputStream is = new ByteArrayInputStream(new byte[0]);
        registerImage(is, 100, 200);

        final Image image = new Image(is);

        assertEquals(is, image.getImpl_source());
        verifyLoadedImage(image, 0, 0, false, false, 100, 200);
    }

    @Test
    public void loadImageFromStreamScaledTest() {
        final InputStream is = new ByteArrayInputStream(new byte[0]);
        registerImage(is, 300, 100);

        final Image image = new Image(is, 200, 400, true, true);

        assertEquals(is, image.getImpl_source());
        verifyLoadedImage(image, 200, 400, true, true, 300, 100);
    }

    @Test
    public void fromPlatformImageTest() {
        final Object fakePlatformImage = new Object();
        registerImage(fakePlatformImage, 200, 500);

        final Image image = Image.impl_fromPlatformImage(fakePlatformImage);
        verifyLoadedImage(image, 0, 0, false, false, 200, 500);
    }

    @Test
    public void loadImageAsyncProgressTest() {
        final String url = "file:test.png";
        registerImage(url, 200, 100);

        final Image image = new Image(url, true);

        final StubAsyncImageLoader lastAsyncImageLoader =
                getLastAsyncImageLoader();

        lastAsyncImageLoader.setProgress(0, 100);
        final float p1 = (float) image.getProgress();

        lastAsyncImageLoader.setProgress(33, 100);
        final float p2 = (float) image.getProgress();

        lastAsyncImageLoader.setProgress(66, 100);
        final float p3 = (float) image.getProgress();

        lastAsyncImageLoader.setProgress(200, 100);
        final float p4 = (float) image.getProgress();

        assertTrue(p1 < p2);
        assertTrue(p2 < p3);
        assertTrue(p3 == p4);
    }

    /*
    @Test
    public void loadImageAsyncPlaceholderTest() {
        final Object fakePlatformImage1 = new Object();
        registerImage(fakePlatformImage1, 200, 500);
        final Object fakePlatformImage2 = new Object();
        registerImage(fakePlatformImage2, 200, 500);
        
        final String url = "file:test.png";
        registerImage(url, 200, 100);

        final Image placeholderImage1 =
                Image.impl_fromPlatformImage(fakePlatformImage1);
        final Image placeholderImage2 =
                Image.impl_fromPlatformImage(fakePlatformImage2);
        final Image image = new Image(url, 500, 200, true, false, 
                                      true);

        final StubAsyncImageLoader lastAsyncImageLoader =
                getLastAsyncImageLoader();

        final Object platformImage1 = getPlatformImage(image);
        final Object platformImage2 = getPlatformImage(image);
        lastAsyncImageLoader.finish();
        final Object platformImage3 = getPlatformImage(image);
        final Object platformImage4 = getPlatformImage(image);

        assertNotSame(platformImage1, platformImage2);
        assertNotSame(platformImage2, platformImage3);
        assertNotSame(platformImage1, platformImage3);
        assertSame(platformImage3, platformImage4);

        verifyLoadedImage(image, 500, 200, true, false, 200, 100);
    }

    @Test
    public void loadImageAsyncWithAsyncPlaceholderTest() {
        final String placeholderUrl = "file:placeholder.png";
        registerImage(placeholderUrl, 200, 100);
        final String finalUrl = "file:final.png";
        registerImage(finalUrl, 100, 200);

        final Image placeholderImage =
                new Image(placeholderUrl, true);
        final StubAsyncImageLoader placeholderLoader =
                getLastAsyncImageLoader();

        final Image finalImage =
                new Image(finalUrl, true);
        final StubAsyncImageLoader finalLoader =
                getLastAsyncImageLoader();

        assertNull(finalImage.impl_getPlatformImage());
        assertEquals(0, finalImage.getWidth(), 0);
        assertEquals(0, finalImage.getHeight(), 0);

        placeholderLoader.finish();
        assertEquals(placeholderUrl, getPlatformImage(finalImage).getSource());
        assertEquals(200, finalImage.getWidth(), 0);
        assertEquals(100, finalImage.getHeight(), 0);

        finalLoader.finish();
        assertEquals(finalUrl, getPlatformImage(finalImage).getSource());
        assertEquals(100, finalImage.getWidth(), 0);
        assertEquals(200, finalImage.getHeight(), 0);
    }
    */

    @Test
    public void loadImageAsyncCancelTest() {
        final String url = "file:test.png";
        registerImage(url, 200, 100);

        final Image image1 = new Image(url, true);
        final StubAsyncImageLoader lastAsyncImageLoader =
                getLastAsyncImageLoader();

        image1.cancel();
        assertTrue(lastAsyncImageLoader.isCancelled());
        assertTrue(image1.isError());

        final Image image2 = new Image(url);
        image2.cancel();
        verifyLoadedImage(image2, 0, 0, false, false, 200, 100);
    }

    @Test
    public void loadImageAsyncErrorTest() {
        final String url = "file:test.png";
        registerImage(url, 200, 100);

        final Image image = new Image(url, true);

        final StubAsyncImageLoader lastAsyncImageLoader =
                getLastAsyncImageLoader();

        final Exception testException = new Exception("Test exception");

        lastAsyncImageLoader.finish(testException);
        assertTrue(image.isError());
        assertEquals(testException, image.getException());
    }

    @Test
    public void loadMultipleImagesAsyncTest() {
        final int multiImageCount = 100;
        final Queue<StubAsyncImageLoader> asyncLoaders =
                new LinkedList<StubAsyncImageLoader>();
        final Image[] images = new Image[multiImageCount];

        StubAsyncImageLoader lastAsyncLoader = null;
        for (int i = 0; i < multiImageCount; ++i) {
            final String url = "file:multi" + i + ".png";
            registerImage(url, 100, 100);

            images[i] = new Image(url, true);

            StubAsyncImageLoader asyncLoader =
                    imageLoaderFactory.getLastAsyncImageLoader();
            assertNotNull(asyncLoader);
            if (lastAsyncLoader == asyncLoader) {
                asyncLoaders.poll().finish();
                asyncLoader = imageLoaderFactory.getLastAsyncImageLoader();
                assertNotSame(lastAsyncLoader, asyncLoader);
            }

            asyncLoaders.add(asyncLoader);
            lastAsyncLoader = asyncLoader;
        }

        for (final StubAsyncImageLoader asyncLoader: asyncLoaders) {
            asyncLoader.finish();
        }

        for (final Image image: images) {
            verifyLoadedImage(image, 0, 0, false, false, 100, 100);
        }
    }

    @Test
    public void loadMultipleImagesAsyncCancelTest() {
        final int multiImageCount = 100;
        final Image[] images = new Image[multiImageCount];

        for (int i = 0; i < multiImageCount; ++i) {
            final String url = "file:multi_cancel_" + i + ".png";
            registerImage(url, 100, 100);

            images[i] = new Image(url, true);
        }

        // traverse backwards to first cancel images which were queued because
        // of the thread limit
        for (int i = images.length - 1; i >= 0; --i) {
            final Image image = images[i];
            image.cancel();
            assertTrue(image.isError());
        }
    }

    @Test
    public void animatedImageTest() {
        // reset time
        toolkit.setAnimationTime(0);
        final Image animatedImage = 
                TestImages.createAnimatedTestImage(
                        300, 400,        // width, height
                        0,               // loop count
                        2000, 1000, 3000 // frame delays
                );

        verifyLoadedImage(animatedImage, 0, 0, false, false, 300, 400);

        toolkit.setAnimationTime(1000);
        assertEquals(0, getPlatformImage(animatedImage).getFrame());

        toolkit.setAnimationTime(2500);
        assertEquals(1, getPlatformImage(animatedImage).getFrame());
        
        toolkit.setAnimationTime(4500);
        assertEquals(2, getPlatformImage(animatedImage).getFrame());
        
        toolkit.setAnimationTime(7000);
        assertEquals(0, getPlatformImage(animatedImage).getFrame());

        TestImages.disposeAnimatedImage(animatedImage);
    }

    @Test
    public void animatedImageTestLoopOnce() {
        // reset time
        toolkit.setAnimationTime(0);
        final Image animatedImage = 
                TestImages.createAnimatedTestImage(
                        300, 400,        // width, height
                        1,               // loop count
                        2000, 1000, 3000 // frame delays
                );

        verifyLoadedImage(animatedImage, 0, 0, false, false, 300, 400);

        toolkit.setAnimationTime(1000);
        assertEquals(0, getPlatformImage(animatedImage).getFrame());

        toolkit.setAnimationTime(2500);
        assertEquals(1, getPlatformImage(animatedImage).getFrame());
        
        toolkit.setAnimationTime(4500);
        assertEquals(2, getPlatformImage(animatedImage).getFrame());
        
        toolkit.setAnimationTime(7000);
        assertEquals(2, getPlatformImage(animatedImage).getFrame());

        TestImages.disposeAnimatedImage(animatedImage);
    }

    @Test
    public void imagePropertyListenersCalledOnceTest() {
        final String url = "file:test.png";
        registerImage(url, 200, 100);

        final Image image = new Image(url, true);

        final StubAsyncImageLoader lastAsyncImageLoader =
                getLastAsyncImageLoader();

        final PropertyInvalidationCounter<Number> widthInvalidationCounter =
                new PropertyInvalidationCounter<Number>();
        final PropertyInvalidationCounter<Number> heightInvalidationCounter =
                new PropertyInvalidationCounter<Number>();
        final PropertyInvalidationCounter<Object> plImageInvalidationCounter =
                new PropertyInvalidationCounter<Object>();

        image.widthProperty().addListener(widthInvalidationCounter);
        image.heightProperty().addListener(heightInvalidationCounter);
        Toolkit.getImageAccessor().getImageProperty(image).addListener(plImageInvalidationCounter);

        assertEquals(0, image.getWidth(), 0);
        assertEquals(0, image.getHeight(), 0);
        assertEquals(null, image.impl_getPlatformImage());

        lastAsyncImageLoader.finish();

        assertEquals(1, widthInvalidationCounter.getCounter());
        assertEquals(1, heightInvalidationCounter.getCounter());
        assertEquals(1, plImageInvalidationCounter.getCounter());
    }

    @Test
    public void imagePropertiesChangedAtomicallyTest() {
        final String url = "file:test.png";
        registerImage(url, 200, 100);

        final Image image = new Image(url, true);
        final StubAsyncImageLoader lastAsyncImageLoader =
                getLastAsyncImageLoader();

        final InvalidationListener imageChecker =
                observable -> {
                    assertEquals(200, image.getWidth(), 0);
                    assertEquals(100, image.getHeight(), 0);
                    assertNotNull(image.impl_getPlatformImage());
                };

        image.widthProperty().addListener(imageChecker);
        image.heightProperty().addListener(imageChecker);
        Toolkit.getImageAccessor().getImageProperty(image).addListener(imageChecker);

        assertEquals(0, image.getWidth(), 0);
        assertEquals(0, image.getHeight(), 0);
        assertEquals(null, image.impl_getPlatformImage());

        lastAsyncImageLoader.finish();
    }

    private static void verifyLoadedImage(final Image image,
                                          final int loadWidth,
                                          final int loadHeight,
                                          final boolean preserveRatio,
                                          final boolean smooth,
                                          final int imageWidth,
                                          final int imageHeight) {
        assertFalse(image.isError());
        assertEquals(1, (int) image.getProgress());

        final StubPlatformImage platformImage =
                getPlatformImage(image);
        assertEquals(0, platformImage.getFrame());

        final StubImageLoader imageLoader =
                platformImage.getImageLoader();
        assertEquals(loadWidth, imageLoader.getLoadWidth());
        assertEquals(loadHeight, imageLoader.getLoadHeight());
        assertEquals(preserveRatio, imageLoader.getPreserveRatio());
        assertEquals(smooth, imageLoader.getSmooth());

        assertEquals(imageWidth, (int) image.getWidth());
        assertEquals(imageHeight, (int) image.getHeight());
    }

    private static StubPlatformImage getPlatformImage(final Image srcImage) {
        final Object unknownPlatformImage = srcImage.impl_getPlatformImage();
        assertTrue(unknownPlatformImage instanceof StubPlatformImage);
        return (StubPlatformImage) unknownPlatformImage;
    }

    private void registerImage(final Object source,
                               final int width,
                               final int height) {
        imageLoaderFactory.registerImage(
                source, new StubPlatformImageInfo(width, height));
    }

    private StubAsyncImageLoader getLastAsyncImageLoader() {
        final StubAsyncImageLoader lastAsyncImageLoader =
                imageLoaderFactory.getLastAsyncImageLoader();
        assertNotNull(lastAsyncImageLoader);
        assertTrue(lastAsyncImageLoader.isStarted());

        return lastAsyncImageLoader;
    }

    @Test
    public void createImageFromClasspathTest() {
        final String url = "javafx/scene/image/test.png";
        final String resolvedUrl = Thread.currentThread().getContextClassLoader().getResource(url).toString();
        registerImage(resolvedUrl, 100, 200);
        
        final Image image = new Image(url);

        assertEquals(resolvedUrl, image.impl_getUrl());
        verifyLoadedImage(image, 0, 0, false, false, 100, 200);
    }
    
    @Test
    public void createImageFromClasspathTest_withLeadingSlash() {
        final String url = "/javafx/scene/image/test.png";
        final String resolvedUrl = Thread.currentThread().getContextClassLoader().getResource(url.substring(1)).toString();
        registerImage(resolvedUrl, 100, 200);
        
        final Image image = new Image(url);

        assertEquals(resolvedUrl, image.impl_getUrl());
        verifyLoadedImage(image, 0, 0, false, false, 100, 200);
    }

    @Test(expected=NullPointerException.class)
    public void createImageFromNullUrlTest() {
        new Image((String) null);
    }

    @Test(expected=NullPointerException.class)
    public void createImageAsyncFromNullUrlTest() {
        new Image(null, true);
    }

    @Test(expected=NullPointerException.class)
    public void createImageFromNullInputStreamTest() {
        new Image((InputStream) null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createImageFromEmptyUrlTest() {
        new Image("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void createImageAsyncFromEmptyUrlTest() {
        new Image("", true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createImageFromInvalidUrlTest() {
        new Image(":");
    }

    @Test(expected=IllegalArgumentException.class)
    public void createImageAsyncFromInvalidUrlTest() {
        new Image(":", true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createImageFromUnsupportedUrlTest() {
        new Image("unsupported:image.png");
    }

    @Test(expected=IllegalArgumentException.class)
    public void createImageAsyncFromUnsupportedUrlTest() {
        new Image("unsupported:image.png", true);
    }
}
