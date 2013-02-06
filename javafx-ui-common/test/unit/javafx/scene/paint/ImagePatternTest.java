/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;

import org.junit.Test;

import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

public class ImagePatternTest {

    private Image createImage() {
        final String url = "file:test.png";

        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        StubImageLoaderFactory imageLoaderFactory =
                toolkit.getImageLoaderFactory();

        imageLoaderFactory.registerImage(
                url, new StubPlatformImageInfo(100, 200));

        return new Image(url);
    }

    @Test
    public void testImagePatternShort() {
        Image image = createImage();
        ImagePattern pattern = new ImagePattern(image);

        assertEquals(image, pattern.getImage());
        assertEquals(0f, pattern.getX(), 0.0001);
        assertEquals(0f, pattern.getY(), 0.0001);
        assertEquals(1f, pattern.getWidth(), 0.0001);
        assertEquals(1f, pattern.getHeight(), 0.0001);
        assertTrue(pattern.isProportional());
    }

    @Test
    public void testImagePatternLong() {
        Image image = createImage();
        ImagePattern pattern = new ImagePattern(image, 1, 2, 3, 4, false);

        assertEquals(image, pattern.getImage());
        assertEquals(1f, pattern.getX(), 0.0001);
        assertEquals(2f, pattern.getY(), 0.0001);
        assertEquals(3f, pattern.getWidth(), 0.0001);
        assertEquals(4f, pattern.getHeight(), 0.0001);
        assertFalse(pattern.isProportional());
    }

    @Test
    public void testImpl_getPlatformPaint() {
        ImagePattern pattern = new ImagePattern(createImage());

        Object paint = Toolkit.getPaintAccessor().getPlatformPaint(pattern);
        assertNotNull(paint);
        assertSame(paint, Toolkit.getPaintAccessor().getPlatformPaint(pattern));
    }


}
