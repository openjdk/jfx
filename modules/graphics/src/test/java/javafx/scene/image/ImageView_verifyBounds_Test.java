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

import static com.sun.javafx.test.TestHelper.assertBoundsEqual;
import static com.sun.javafx.test.TestHelper.box;
import static javafx.scene.image.ImageViewConfig.config;
import static javafx.scene.image.TestImages.TEST_IMAGE_100x200;
import static javafx.scene.image.TestImages.TEST_IMAGE_200x100;

import java.util.Arrays;
import java.util.Collection;

import javafx.geometry.BoundingBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class ImageView_verifyBounds_Test {

    private final ImageViewConfig imageViewConfig;
    private final BoundingBox expectedBounds;

    private ImageView imageView;

    /*
     * Parameters: [image view config], [expected bounds]
     */
    @Parameters
    public static Collection data() {
        return Arrays.asList(new Object[][] {
            { config(TEST_IMAGE_100x200, 0, 0), box(0, 0, 100, 200) },
            { config(TEST_IMAGE_200x100, 20, 10), box(20, 10, 200, 100) },
            {
                config(null, 0, 0, 400, 400, false),
                box(0, 0, 400, 400)
            },
            {
                config(TEST_IMAGE_100x200, 10, 20, 0, 400, false),
                box(10, 20, 100, 400)
            },
            {
                config(TEST_IMAGE_200x100, 20, 10, 400, 0, false),
                box(20, 10, 400, 100)
            },
            {
                config(null, 0, 0, 400, 400, true),
                box(0, 0, 400, 400)
            },
            {
                config(TEST_IMAGE_100x200, 10, 20, 400, 400, true),
                box(10, 20, 200, 400)
            },
            {
                config(TEST_IMAGE_200x100, 20, 10, 400, 400, true),
                box(20, 10, 400, 200)
            },
            {
                config(TEST_IMAGE_100x200, 10, 20,
                       -50, 100, 200, 100,
                       400, 0, true),
                box(10, 20, 400, 200)
            },
            {
                config(TEST_IMAGE_200x100, 20, 10,
                       100, -50, 100, 200,
                       0, 400, true),
                box(20, 10, 200, 400)
            },
            /* tests for invalid viewport */
            {
                config(TEST_IMAGE_200x100, 0, 0,
                       0, 0, 0, 100,
                       400, 400, true),
                box(0, 0, 400, 200)
            },
            {
                config(TEST_IMAGE_100x200, 0, 0,
                       0, 0, 100, 0,
                       400, 400, true),
                box(0, 0, 200, 400)
            }
        });
    }

    public ImageView_verifyBounds_Test(final ImageViewConfig imageViewConfig,
                                       final BoundingBox expectedBounds) {
        this.imageViewConfig = imageViewConfig;
        this.expectedBounds = expectedBounds;
    }

    @Before
    public void setUp() {
        imageView = new ImageView();
        imageViewConfig.applyTo(imageView);
    }

    @After
    public void tearDown() {
        imageView = null;
    }

    @Test
    public void verifyBounds() {
        assertBoundsEqual(expectedBounds, imageView.getBoundsInLocal());
    }
}
