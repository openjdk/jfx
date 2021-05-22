/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.iio;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.ImageStorageException;
import com.sun.javafx.iio.common.ImageTools;
import static org.junit.Assert.*;
import org.junit.ComparisonFailure;
import org.junit.Test;

public class ImageStorageTest {
    private String getResourcePath(String path) {
        return this.getClass().getResource(path).toString();
    }

    @Test
    public void createImageFromNoExtensionURL() throws ImageStorageException {
        String path = getResourcePath("testpngnoextension");
        assertNotNull(ImageStorage.loadAll(path, null, 0, 0, true, 2.0f, true));
    }

    @Test
    public void testImageNames() {
        String [][]imageNames = new String[][] {
            { "image", "image@2x" },
            { "image.ext", "image@2x.ext" },
            { "dir/image", "dir/image@2x" },
            { "/dir.ext/image.ext", "/dir.ext/image@2x.ext" },
            { "file:image", "file:image@2x" },
            { "file:image.ext", "file:image@2x.ext" },
            { "http://test.com/image", "http://test.com/image@2x" },
            { "http://test.com/dir.ext/image", "http://test.com/dir.ext/image@2x" },
            { "http://test.com/image.ext", "http://test.com/image@2x.ext" },
            { "http://test.com/dir.ext/image.ext", "http://test.com/dir.ext/image@2x.ext" },
        };
        for (String[] names : imageNames) {
            String name2x = ImageTools.getScaledImageName(names[0]);
            if (name2x.equals(names[1])) continue;
            throw new ComparisonFailure("Scaled image names don't match", names[1], name2x);
        }
    }

    @Test
    public void testCompleteAnimation() throws ImageStorageException {
        String path = getResourcePath("gif/animation/test3Frames.gif");
        ImageFrame[] frames = ImageStorage.loadAll(path, null, 0, 0, true, 1.0f, true);
        assertEquals(frames.length, 3);
    }

    @Test
    public void testIncompleteAnimation() throws ImageStorageException {
        String path = getResourcePath("gif/animation/test3rdFrameIncomplete.gif");
        ImageFrame[] frames = ImageStorage.loadAll(path, null, 0, 0, true, 1.0f, true);
        assertEquals(frames.length, 2);
    }

    @Test(expected = ImageStorageException.class)
    public void testCorruptFirstFrame() throws ImageStorageException  {
        String path = getResourcePath("gif/animation/testBad.gif");
        ImageStorage.loadAll(path, null, 0, 0, false, 1.0f, false);
    }

    @Test
    public void testLoadImageFromDataURI() throws ImageStorageException {
        String url =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAKCAIAAAA7N+mxAAAAAXNSR0IArs4c6QAAAAR"
            + "nQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAcSURBVChTY/jPwADBZACyNMHAqGYSwZDU/P8/AB"
            + "ieT81GAGKoAAAAAElFTkSuQmCC";

        ImageFrame[] frames = ImageStorage.loadAll(url, null, 20, 10, false, 1, false);
        assertEquals(1, frames.length);

        byte[] data = (byte[])frames[0].getImageData().array();
        assertEquals(-1, data[0]);
        assertEquals(0, data[1]);
        assertEquals(0, data[2]);

        assertEquals(0, data[3]);
        assertEquals(-1, data[4]);
        assertEquals(0, data[5]);

        assertEquals(0, data[6]);
        assertEquals(0, data[7]);
        assertEquals(-1, data[8]);
    }
}
