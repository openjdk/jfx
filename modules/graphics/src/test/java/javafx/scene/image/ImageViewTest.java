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
import com.sun.javafx.sg.prism.NGImageView;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Rectangle2D;
import javafx.scene.NodeTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;

import static org.junit.Assert.*;

public final class ImageViewTest {
    private ImageView imageView;

    @Before
    public void setUp() {
        imageView = new StubImageView();
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
                (sgValue, pgValue) -> {
                    if (sgValue == null) {
                        assertNull(pgValue);
                    } else {
                        assertSame(
                                ((Image) sgValue).impl_getPlatformImage(),
                                pgValue);
                    }

                    return 0;
                }
        );
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

    @Test
    public void testNullImage() {
        imageView.setImage(null);
        assertNull(imageView.getImage());
    }

    @Test
    public void testNullViewport() {
        imageView.setViewport(null);
        assertNull(imageView.getViewport());
    }

    private static class BoundsChangedListener implements ChangeListener<Bounds> {
        private boolean wasCalled = false;

        public void changed(ObservableValue<? extends Bounds> ov, Bounds oldValue, Bounds newValue) {
                assertEquals(oldValue.getWidth(), 32, 1e-10);
                assertEquals(oldValue.getHeight(), 32, 1e-10);
                assertEquals(newValue.getWidth(), 200, 1e-10);
                assertEquals(newValue.getHeight(), 100, 1e-10);
                wasCalled = true;
        }
    }

    @Test
    public void testImageChangesBoundsWithListener() {
        BoundsChangedListener listener = new BoundsChangedListener();
        imageView.setImage(TestImages.TEST_IMAGE_32x32);
        imageView.boundsInParentProperty().addListener(listener);
        imageView.setImage(TestImages.TEST_IMAGE_200x100);
        assertTrue(listener.wasCalled);
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

    public class StubImageView extends ImageView {
        public StubImageView() {
            super();
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGImageView();
        }
    }

    public class StubNGImageView extends NGImageView {
        // for tests
        private Object image;
        private float x;
        private float y;
        private boolean smooth;

        private float cw;
        private float ch;
        private Rectangle2D viewport;

        @Override public void setImage(Object image) { this.image = image; }
        public Object getImage() { return image; }
        @Override public void setX(float x) { this.x = x; }
        public float getX() { return x; }
        @Override public void setY(float y) { this.y = y; }
        public float getY() { return y; }

        @Override public void setViewport(float vx, float vy, float vw, float vh,
                                float cw, float ch) {
            this.viewport = new Rectangle2D(vx, vy, vw, vh);
            this.cw = cw;
            this.ch = ch;
        }

        @Override public void setSmooth(boolean smooth) { this.smooth = smooth; }
        public boolean isSmooth() { return this.smooth; }
        public Rectangle2D getViewport() { return viewport; }
        public float getContentWidth() { return cw; }
        public float getContentHeight() { return ch; }
    }
}