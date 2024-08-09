/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import com.sun.javafx.tk.Toolkit;

import javafx.css.CompositeStyleConverter;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundShim;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.pgstub.StubImageLoaderFactory;
import test.com.sun.javafx.pgstub.StubPlatformImageInfo;
import test.com.sun.javafx.pgstub.StubToolkit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BackgroundConverterTest {

    private static final String IMAGE_URL = "file:red.png";

    private final StubImageLoaderFactory imageLoaderFactory = ((StubToolkit)Toolkit.getToolkit()).getImageLoaderFactory();
    private final CompositeStyleConverter<Background> converter = BackgroundShim.getConverter();
    private final Image image;

    BackgroundConverterTest() {
        imageLoaderFactory.reset();
        imageLoaderFactory.registerImage(IMAGE_URL, new StubPlatformImageInfo(57, 41));
        image = new Image(IMAGE_URL);
    }

    @Test
    void convertBackgroundImageFromURL() {
        var background = converter.convert(Map.of(BackgroundShim.BACKGROUND_IMAGE, new String[] {IMAGE_URL}));
        assertEquals(1, background.getImages().size());
        assertEquals(IMAGE_URL, background.getImages().get(0).getImage().getUrl());
    }

    @Test
    void convertBackgroundImageFromImage() {
        var background = converter.convert(Map.of(BackgroundShim.BACKGROUND_IMAGE, new Image[] { image }));
        assertEquals(1, background.getImages().size());
        assertSame(image, background.getImages().get(0).getImage());
    }

    @Test
    void reconstructedObjectMustBeEqual() {
        var expected = new Background(
            List.of(new BackgroundFill(Color.RED, new CornerRadii(5, true), new Insets(2)),
                    new BackgroundFill(Color.GREEN, new CornerRadii(10, false), new Insets(1, 2, 3, 4))),
            List.of(new BackgroundImage(image, BackgroundRepeat.REPEAT, BackgroundRepeat.ROUND,
                                        BackgroundPosition.CENTER, BackgroundSize.DEFAULT))
        );

        var actual = converter.convert(converter.convertBack(expected));
        assertEquals(expected, actual);
    }
}
