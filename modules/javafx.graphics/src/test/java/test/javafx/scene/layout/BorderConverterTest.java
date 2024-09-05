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

import com.sun.javafx.css.SubPropertyConverter;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderRepeat;
import javafx.scene.layout.BorderShim;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.pgstub.StubImageLoaderFactory;
import test.com.sun.javafx.pgstub.StubPlatformImageInfo;
import test.com.sun.javafx.pgstub.StubToolkit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BorderConverterTest {

    private static final String IMAGE_URL = "file:red.png";

    @SuppressWarnings("unchecked")
    private final SubPropertyConverter<Border> converter = (SubPropertyConverter<Border>)BorderShim.getConverter();
    private final StubImageLoaderFactory imageLoaderFactory = ((StubToolkit)Toolkit.getToolkit()).getImageLoaderFactory();
    private final Image image;

    BorderConverterTest() {
        imageLoaderFactory.reset();
        imageLoaderFactory.registerImage(IMAGE_URL, new StubPlatformImageInfo(57, 41));
        image = new Image(IMAGE_URL);
    }

    @Test
    void convertBorderImageFromURL() {
        var border = converter.convert(Map.of(BorderShim.BORDER_IMAGE_SOURCE, new String[] {IMAGE_URL}));
        assertEquals(1, border.getImages().size());
        assertEquals(IMAGE_URL, border.getImages().get(0).getImage().getUrl());
    }

    @Test
    void convertBorderImageFromImage() {
        var border = converter.convert(Map.of(BorderShim.BORDER_IMAGE_SOURCE, new Image[] { image }));
        assertEquals(1, border.getImages().size());
        assertSame(image, border.getImages().get(0).getImage());
    }

    @Test
    void convertBorderImageFromUnexpectedObjectFails() {
        assertThrows(IllegalArgumentException.class, () ->
            converter.convert(Map.of(BorderShim.BORDER_IMAGE_SOURCE, new Object[] { 1.0 })));
    }

    @Test
    void convertBackDoesNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> converter.convertBack(null));
    }

    @Test
    void reconstructedObjectMustBeEqual() {
        var expected = new Border(
            List.of(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.EMPTY),
                    new BorderStroke(new LinearGradient(0.1, 0.2, 0.3, 0.4, true, CycleMethod.REPEAT,
                                                        new Stop(0, Color.BLUE), new Stop(0.5, Color.YELLOW)),
                                     BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.EMPTY)),
            List.of(new BorderImage(image, BorderWidths.FULL, Insets.EMPTY, BorderWidths.DEFAULT,
                                    false, BorderRepeat.STRETCH, BorderRepeat.REPEAT))
        );

        var actual = converter.convert(converter.convertBack(expected));
        assertEquals(expected, actual);
    }
}
