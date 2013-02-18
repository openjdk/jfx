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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

import java.util.Comparator;

import javafx.geometry.Rectangle2D;
import javafx.scene.NodeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class ImageViewTest {
    private ImageView imageView;

    @Before
    public void setUp() {
        imageView = new ImageView();
        imageView.setImage(TestImages.TEST_IMAGE_100x200);
    }

    @After
    public void tearDown() {
        imageView = null;
    }

    @Test
    public void testPropertyPropagation_x() throws Exception {
        NodeTest.testDoublePropertyPropagation(imageView, "X", 100, 200);
    }

    @Test
    public void testPropertyPropagation_y() throws Exception {
        NodeTest.testDoublePropertyPropagation(imageView, "Y", 100, 200);
    }

    @Test
    public void testPropertyPropagation_smooth() throws Exception {
        NodeTest.testBooleanPropertyPropagation(
                imageView, "smooth", false, true);
    }

    @Test
    public void testPropertyPropagation_viewport() throws Exception {
        NodeTest.testObjectPropertyPropagation(
                imageView, "viewport",
                new Rectangle2D(10, 20, 200, 100),
                new Rectangle2D(20, 10, 100, 200));
    }

    @Test
    public void testPropertyPropagation_image() throws Exception {
        NodeTest.testObjectPropertyPropagation(
                imageView, "image", "image",
                null,
                TestImages.TEST_IMAGE_200x100,
                new Comparator() {
                    @Override
                    public int compare(final Object sgValue,
                                       final Object pgValue) {
                        if (sgValue == null) {
                            assertNull(pgValue);
                        } else {
                            assertSame(
                                    ((Image) sgValue).impl_getPlatformImage(),
                                    pgValue);
                        }

                        return 0;
                    }
                });
    }

    @Test
    public void testUrlConstructor() {
        final StubImageLoaderFactory imageLoaderFactory =
                ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

        final String url = "file:img_view_image.png";
        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(50, 40));

        final ImageView newImageView = new ImageView(url);

        assertEquals(url, newImageView.getImage().impl_getUrl());
    }

    /*
    @Test
    public void testPlatformImageChangeForAsyncLoadedImage() {
        final StubImageLoaderFactory imageLoaderFactory =
                ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

        final String url = "file:async.png";
        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(100, 200));

        final Image placeholderImage =
                TestImages.TEST_IMAGE_200x100;
        final Image asyncImage =
                new Image(url, 200, 100, true, true, true);

        final StubAsyncImageLoader lastAsyncImageLoader =
                imageLoaderFactory.getLastAsyncImageLoader();

        final StubImageView pgImageView =
                (StubImageView) imageView.impl_getPGNode();

        imageView.setImage(asyncImage);

        NodeTest.callSyncPGNode(imageView);
        assertSame(placeholderImage.impl_getPlatformImage(),
                   pgImageView.getImage());
        assertBoundsEqual(box(0, 0, 200, 100), imageView.getBoundsInLocal());

        lastAsyncImageLoader.finish();

        NodeTest.callSyncPGNode(imageView);
        assertSame(asyncImage.impl_getPlatformImage(),
                   pgImageView.getImage());
        assertBoundsEqual(box(0, 0, 100, 200), imageView.getBoundsInLocal());
    }
    */
}
