/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.sun.javafx.util.ImageUtils;

/**
 * Tests ImageUtils.
 */
public class TestImageUtils {
    @Test
    public void writeImageNulls() {
        assertThrows(NullPointerException.class, () -> {
            ImageUtils.writeImage(null, "jpg");
        });
        assertThrows(NullPointerException.class, () -> {
            WritableImage im = new WritableImage(1, 1);
            ImageUtils.writeImage(im, null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "png", "gif" })
    public void writeImageTransparent(String format) throws IOException {
        WritableImage im = new WritableImage(1, 1);
        byte[] b = ImageUtils.writeImage(im, format);
        assertTrue(b.length > 0);
    }

    // NOTE: omitting JPEG format since StubToolkit does not support it
    @ParameterizedTest
    @ValueSource(strings = { "png", /* "jpg", "jpeg", */ "gif" })
    public void writeImageOpaque(String format) throws IOException {
        WritableImage im = new WritableImage(1, 1);
        im.getPixelWriter().setArgb(0, 0, 0xffffffff);
        byte[] b = ImageUtils.writeImage(im, format);
        assertTrue(b.length > 0);
    }

    @Test
    public void writeBadFormat() {
        WritableImage im = new WritableImage(1, 1);
        assertThrows(IOException.class, () -> {
            ImageUtils.writeImage(im, "BAD_F0Rmåt");
        });
    }
}
