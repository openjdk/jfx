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

import static test.javafx.scene.image.TestImages.TEST_ERROR_IMAGE;
import javafx.scene.image.Image;
import test.javafx.scene.image.TestImages;

import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Cursor;
import javafx.scene.CursorShim;
import javafx.scene.ImageCursor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class ImageCursor_getCurrentFrame_Test {
    private final StubToolkit toolkit;

    public ImageCursor_getCurrentFrame_Test() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
    }

    @Test
    public void specialCasesTest() {
        final Object defaultCursorFrame =
                CursorShim.getCurrentFrame(Cursor.DEFAULT);

        assertEquals(defaultCursorFrame,
                CursorShim.getCurrentFrame(new ImageCursor(null)));

        assertEquals(defaultCursorFrame,
                CursorShim.getCurrentFrame(new ImageCursor(TEST_ERROR_IMAGE)));
    }

    @Test
    public void animatedCursorTest() {
        // reset time
        toolkit.setAnimationTime(0);
        final Image animatedImage =
                TestImages.createAnimatedTestImage(
                        300, 400,        // width, height
                        0,               // loop count
                        2000, 1000, 3000 // frame delays
                );
        final ImageCursor animatedImageCursor = new ImageCursor(animatedImage);

        Object lastCursorFrame;
        Object currCursorFrame;

        CursorShim.activate(animatedImageCursor);

        lastCursorFrame = CursorShim.getCurrentFrame(animatedImageCursor);

        toolkit.setAnimationTime(1000);
        currCursorFrame = CursorShim.getCurrentFrame(animatedImageCursor);
        assertSame(lastCursorFrame, currCursorFrame);

        lastCursorFrame = currCursorFrame;

        toolkit.setAnimationTime(2500);
        currCursorFrame = CursorShim.getCurrentFrame(animatedImageCursor);
        assertNotSame(lastCursorFrame, currCursorFrame);

        lastCursorFrame = currCursorFrame;

        toolkit.setAnimationTime(4500);
        currCursorFrame = CursorShim.getCurrentFrame(animatedImageCursor);
        assertNotSame(lastCursorFrame, currCursorFrame);

        lastCursorFrame = currCursorFrame;

        toolkit.setAnimationTime(7000);
        currCursorFrame = CursorShim.getCurrentFrame(animatedImageCursor);
        assertNotSame(lastCursorFrame, currCursorFrame);

        CursorShim.deactivate(animatedImageCursor);

        TestImages.disposeAnimatedImage(animatedImage);
    }

    @Test
    public void animatedCursorCachingTest() {
        // reset time
        toolkit.setAnimationTime(0);
        final Image animatedImage =
                TestImages.createAnimatedTestImage(
                        300, 400,        // width, height
                        0,               // loop count
                        2000, 1000, 3000 // frame delays
                );
        final ImageCursor animatedImageCursor = new ImageCursor(animatedImage);

        CursorShim.activate(animatedImageCursor);

        toolkit.setAnimationTime(1000);
        final Object time1000CursorFrame =
                CursorShim.getCurrentFrame(animatedImageCursor);

        toolkit.setAnimationTime(2500);
        final Object time2500CursorFrame =
                CursorShim.getCurrentFrame(animatedImageCursor);

        toolkit.setAnimationTime(4500);
        final Object time4500CursorFrame =
                CursorShim.getCurrentFrame(animatedImageCursor);

        toolkit.setAnimationTime(6000 + 1000);
        assertSame(time1000CursorFrame,
                   CursorShim.getCurrentFrame(animatedImageCursor));

        toolkit.setAnimationTime(6000 + 2500);
        assertSame(time2500CursorFrame,
                   CursorShim.getCurrentFrame(animatedImageCursor));

        toolkit.setAnimationTime(6000 + 4500);
        assertSame(time4500CursorFrame,
                   CursorShim.getCurrentFrame(animatedImageCursor));

        CursorShim.deactivate(animatedImageCursor);

        TestImages.disposeAnimatedImage(animatedImage);
    }
}
