/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.image;

import static test.com.sun.javafx.test.TestHelper.assertBoundsEqual;
import static test.com.sun.javafx.test.TestHelper.box;
import static test.javafx.scene.image.ImageViewConfig.config;
import static test.javafx.scene.image.TestImages.TEST_IMAGE_100x200;
import static test.javafx.scene.image.TestImages.TEST_IMAGE_200x100;

import java.util.stream.Stream;

import javafx.geometry.BoundingBox;
import javafx.scene.image.ImageView;

import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class ImageView_verifyBounds_Test {

    private ImageView imageView;

    /*
     * Parameters: [image view config], [expected bounds]
     */
    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( config(TEST_IMAGE_100x200, 0, 0), box(0, 0, 100, 200) ),
            Arguments.of( config(TEST_IMAGE_200x100, 20, 10), box(20, 10, 200, 100) ),
            Arguments.of(
                config(null, 0, 0, 400, 400, false),
                box(0, 0, 400, 400)
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 10, 20, 0, 400, false),
                box(10, 20, 100, 400)
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 20, 10, 400, 0, false),
                box(20, 10, 400, 100)
            ),
            Arguments.of(
                config(null, 0, 0, 400, 400, true),
                box(0, 0, 400, 400)
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 10, 20, 400, 400, true),
                box(10, 20, 200, 400)
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 20, 10, 400, 400, true),
                box(20, 10, 400, 200)
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 10, 20,
                       -50, 100, 200, 100,
                       400, 0, true),
                box(10, 20, 400, 200)
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 20, 10,
                       100, -50, 100, 200,
                       0, 400, true),
                box(20, 10, 200, 400)
            ),
            /* tests for invalid viewport */
            Arguments.of(
                config(TEST_IMAGE_200x100, 0, 0,
                       0, 0, 0, 100,
                       400, 400, true),
                box(0, 0, 400, 200)
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 0, 0,
                       0, 0, 100, 0,
                       400, 400, true),
                box(0, 0, 200, 400)
            )
        );
    }

    // NOTE: This should be reverted once parametrized class tests are added to JUnit5
    //       For now, tests call this manually
    // @BeforeEach
    public void setUp(ImageViewConfig imageViewConfig) {
        imageView = new ImageView();
        imageViewConfig.applyTo(imageView);
    }

    @AfterEach
    public void tearDown() {
        imageView = null;
    }

    @ParameterizedTest
    @MethodSource("data")
    public void verifyBounds(ImageViewConfig imageViewConfig,
                             BoundingBox expectedBounds) {
        setUp(imageViewConfig);
        assertBoundsEqual(expectedBounds, imageView.getBoundsInLocal());
    }
}
