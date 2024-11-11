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

import static test.javafx.scene.image.ImageViewConfig.config;
import static test.javafx.scene.image.TestImages.TEST_IMAGE_100x200;
import static test.javafx.scene.image.TestImages.TEST_IMAGE_200x100;

import java.util.stream.Stream;
import javafx.scene.image.ImageView;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageView_verifyContains_Test {

    private ImageView imageView;

    /*
     * Parameters: [image view config], [x], [y], [result]
     *
     * Note:
     * Every test image has the following hit test pattern:
     *
     * TTT|FFF
     * TTT|FFF
     * ---+---
     * FFF|TTT
     * FFF|TTT
     */
    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( config(null, 0, 0), 0, 0, false ),
            Arguments.of( config(TEST_IMAGE_100x200, 0, 0), 25, 50, true ),
            Arguments.of( config(TEST_IMAGE_100x200, 25, 50), 25 + 75, 50 + 50, false ),
            Arguments.of( config(TEST_IMAGE_100x200, 25, 0), 25 + 25, 150, false ),
            Arguments.of( config(TEST_IMAGE_100x200, 0, 50), 75, 50 + 150, true ),
            Arguments.of( config(TEST_IMAGE_200x100, 0, 0), 250, 75, false ),
            Arguments.of( config(TEST_IMAGE_200x100, 50, 25), 50 + 150, 25 + 125, false ),
            Arguments.of( config(TEST_IMAGE_200x100, 50, 0), 0, 25, false ),
            Arguments.of( config(TEST_IMAGE_200x100, 0, 25), 50, 0, false ),
            Arguments.of(
                config(null, 0, 0, 400, 400, false),
                200, 200, false
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 50, 0, 0, 400, false),
                50 + 25, 100, true
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 0, 50, 400, 0, false),
                300, 50 + 75, true
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 0, 200, 400, 400, true),
                150, 200 + 300, true
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 200, 0, 400, 400, true),
                200 + 100, 50, true
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 0, 0,
                       50, 0, 100, 200,
                       0, 400, true),
                50, 300, true
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 0, 0,
                       -50, 0, 100, 200,
                       400, 400, true),
                50, 100, false
            ),
            Arguments.of(
                config(TEST_IMAGE_100x200, 0, 0,
                       50, 50, 50, 150,
                       400, 400, false),
                395, 395, true
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 20, 10,
                       0, 50, 200, 100,
                       400, 0, true),
                300, 50, true
            ),
            Arguments.of(
                config(TEST_IMAGE_200x100, 20, 10,
                       0, -50, 200, 100,
                       400, 0, true),
                100, 50, false
            )
//            /* tests for invalid viewport */
//            Arguments.of(
//                config(TEST_IMAGE_200x100, 0, 0,
//                       0, 0, 0, 100,
//                       400, 400, true),
//                0, 0, false
//            ),
//            Arguments.of(
//                config(TEST_IMAGE_100x200, 0, 0,
//                       0, 0, 100, 0,
//                       400, 400, true),
//                0, 0, false
//            )
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
    public void verifyContains(ImageViewConfig imageViewConfig,
                               float x,
                               float y,
                               boolean expectedContainsResult) {
        setUp(imageViewConfig);
        assertEquals(expectedContainsResult,
                     imageView.contains(x, y));
    }
}
