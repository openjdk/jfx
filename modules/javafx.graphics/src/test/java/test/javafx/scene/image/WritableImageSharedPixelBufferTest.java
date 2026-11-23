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

package test.javafx.scene.image;

import com.sun.javafx.tk.Toolkit;

import java.nio.IntBuffer;

import javafx.scene.image.DrawingContext;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import test.com.sun.javafx.pgstub.StubWritablePlatformImage;

public class WritableImageSharedPixelBufferTest {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    @Test
    public void drawingContextChangeShouldNotifyAllImagesSharingThePixelBuffer() {
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(
            WIDTH,
            HEIGHT,
            IntBuffer.allocate(WIDTH * HEIGHT),
            PixelFormat.getIntArgbPreInstance()
        );

        WritableImage first = new WritableImage(pixelBuffer);
        WritableImage second = new WritableImage(pixelBuffer);

        // both images share the same platform image in the stub toolkit
        StubWritablePlatformImage firstPlatform = (StubWritablePlatformImage)Toolkit.getImageAccessor().getPlatformImage(first);
        StubWritablePlatformImage secondPlatform = (StubWritablePlatformImage)Toolkit.getImageAccessor().getPlatformImage(second);

        assertSame(firstPlatform, secondPlatform, "both images must share the same pixel storage");

        int dirtyCount = firstPlatform.getBufferDirtyCount();

        DrawingContext dc = first.getDrawingContext();

        dc.setFill(Color.RED);
        dc.fillRect(1, 1, 6, 6);

        // the change made through first's drawing context must reach second as well
        assertEquals(dirtyCount + 2, firstPlatform.getBufferDirtyCount());
        assertEquals(0xFFFF0000, secondPlatform.getArgb(3, 3));
    }
}
