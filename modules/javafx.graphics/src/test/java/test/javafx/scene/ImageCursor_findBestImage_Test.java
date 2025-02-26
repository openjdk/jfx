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

package test.javafx.scene;

import java.util.stream.Stream;

import javafx.scene.image.Image;
import test.javafx.scene.image.TestImages;

import test.com.sun.javafx.pgstub.CursorSizeConverter;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.ImageCursor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class ImageCursor_findBestImage_Test {
    private static final Image[] TEST_CURSOR_IMAGES = {
        TestImages.TEST_IMAGE_32x32,
        TestImages.TEST_IMAGE_64x64,
        TestImages.TEST_IMAGE_32x64,
        TestImages.TEST_IMAGE_64x32
    };

    private static StubToolkit toolkit;
    private static CursorSizeConverter oldCursorSizeConverter;


    /*
     * Parameters: [bestWidth], [bestHeight], [hotspotX], [hotspotY],
     *             [expected index],
     *             [expected hotspotX],
     *             [expected hotspotY]
     */
    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( 32, 64, 0, 0, 2, 0, 0 ),
            Arguments.of( 64, 32, 32, 32, 3, 63, 31 ),
            Arguments.of( 48, 64, 16, 16, 2, 16, 32 ),
            Arguments.of( 64, 48, 16, 16, 3, 32, 16 ),
            Arguments.of( 92, 92, 16, 4, 1, 32, 8 ),
            Arguments.of( 16, 16, 4, 16, 0, 4, 16 ),
            Arguments.of( 16, 32, 0, 0, 0, 0, 0 ),
            Arguments.of( 32, 16, 0, 0, 0, 0, 0 )
        );
    }

    @BeforeAll
    public static void setUpClass() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        oldCursorSizeConverter = toolkit.getCursorSizeConverter();
    }

    @AfterAll
    public static void tearDownClass() {
        toolkit.setCursorSizeConverter(oldCursorSizeConverter);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void findBestImageTest(int bestWidth,
                                  int bestHeight,
                                  float hotspotX,
                                  float hotspotY,
                                  int expectedIndex,
                                  float expectedHotspotX,
                                  float expectedHotspotY) {
        toolkit.setCursorSizeConverter(
                CursorSizeConverter.createConstantConverter(bestWidth,
                                                            bestHeight));
        final ImageCursor selectedCursor = ImageCursor.chooseBestCursor(
                                                   TEST_CURSOR_IMAGES,
                                                   hotspotX, hotspotY);
        ImageCursorTest.assertCursorEquals(selectedCursor,
                                           TEST_CURSOR_IMAGES[expectedIndex],
                                           expectedHotspotX, expectedHotspotY);
    }
}
