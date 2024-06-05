/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import test.com.sun.javafx.pgstub.StubImageLoaderFactory;
import test.com.sun.javafx.pgstub.StubPlatformImageInfo;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.test.CssMethodsTestBase;
import test.com.sun.javafx.test.ValueComparator;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

@RunWith(Parameterized.class)
public final class ImageView_cssMethods_Test extends CssMethodsTestBase {
    private static final ImageView TEST_IMAGE_VIEW = new ImageView();
    private static final String TEST_IMAGE_URL1 = "file:test_image_1.png";
    private static final String TEST_IMAGE_URL2 = "file:test_image_2.png";

    private static final ValueComparator IMAGE_COMPARATOR =
            new ValueComparator() {
                @Override
                public boolean equals(final Object expected,
                                      final Object actual) {
                    return ((actual instanceof Image)
                               && ((Image) actual).getUrl().equals(expected));
                }
            };


    @BeforeClass
    public static void configureImageLoaderFactory() {
        final StubImageLoaderFactory imageLoaderFactory =
                ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();
        imageLoaderFactory.reset();
        imageLoaderFactory.registerImage(
                TEST_IMAGE_URL1,
                new StubPlatformImageInfo(32, 32));
        imageLoaderFactory.registerImage(
                TEST_IMAGE_URL2,
                new StubPlatformImageInfo(48, 48));
    }

    @Parameters
    public static Collection data() {
        boolean smooth = ImageView.SMOOTH_DEFAULT;

        return Arrays.asList(new Object[] {
            config(TEST_IMAGE_VIEW, "image", null, "-fx-image", TEST_IMAGE_URL1, IMAGE_COMPARATOR),
            config(TEST_IMAGE_VIEW, "image", TestImages.TEST_IMAGE_32x32, "-fx-image", TEST_IMAGE_URL2, IMAGE_COMPARATOR),
            config(TEST_IMAGE_VIEW, "translateX", 0.0, "-fx-translate-x", 10.0),
            config(TEST_IMAGE_VIEW, "fitHeight", 0.0, "-fx-fit-height", 10.0),
            config(TEST_IMAGE_VIEW, "fitWidth", 0.0, "-fx-fit-width", 10.0),
            config(TEST_IMAGE_VIEW, "preserveRatio", false, "-fx-preserve-ratio", true),
            config(TEST_IMAGE_VIEW, "smooth", smooth, "-fx-smooth", !smooth),
        });
    }

    public ImageView_cssMethods_Test(final Configuration configuration) {
        super(configuration);
    }
}
