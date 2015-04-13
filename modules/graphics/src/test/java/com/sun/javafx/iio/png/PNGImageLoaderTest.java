/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.png;

import com.sun.javafx.iio.ImageTestHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

public class PNGImageLoaderTest {

    private void testImage(InputStream stream) throws IOException {
        PNGImageLoader2 loader = new PNGImageLoader2(stream);
        loader.load(0, 0, 0, true, true);
    }

    @Test
    public void testRT35133() throws IOException {
        InputStream stream = ImageTestHelper.createTestImageStream("png");
        InputStream testStream = ImageTestHelper.createStutteringInputStream(stream);
        testImage(testStream);
    }

    @Test(timeout = 1000, expected = IOException.class)
    public void testRT27010() throws IOException {
        int[] corruptedIDATLength = {
            137, 80, 78, 71, 13, 10, 26, 10, // signature
            0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0, 0, 4, 0, 0, 0, 4, 0, 8, 6, 0, 0, 0, // IHDR chunk data
            0x7f, 0x1d, 0x2b, 0x83, // IHDR chunk crc
            0x80, 0, 0x80, 0, 0x49, 0x44, 0x41, 0x54 // negative IDAT length
        };

        ByteArrayInputStream stream = ImageTestHelper.constructStreamFromInts(corruptedIDATLength);
        testImage(stream);
    }

    @Test(timeout = 1000, expected = IOException.class)
    public void testRT27010MultipleIDAT() throws IOException {
        int[] corruptedIDATLength = {
            137, 80, 78, 71, 13, 10, 26, 10, // signature
            0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0, 0, 4, 0, 0, 0, 4, 0, 8, 6, 0, 0, 0, // IHDR chunk data
            0x7f, 0x1d, 0x2b, 0x83, // IHDR chunk crc
            0, 0, 0, 1, 0x49, 0x44, 0x41, 0x54, // first IDAT
            0, // IDAT chunk data
            0, 0, 0, 0, // IDAT chunk crc
            0x80, 0, 0, 0, 0x49, 0x44, 0x41, 0x54, // second IDAT
        };

        ByteArrayInputStream stream = ImageTestHelper.constructStreamFromInts(corruptedIDATLength);
        testImage(stream);
    }
}
