/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.tk.quantum;

import com.sun.javafx.tk.quantum.WindowStageShim;
import com.sun.prism.Image;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowStageTest {

    private void addImage(List<Object> images, int size) {
        byte[] pixels = new byte[size * size * 3];
        Image image = Image.fromByteRgbData(pixels, size, size);
        images.add(image);
    }

    @Test
    public void bestIconSizeTest() {
        List<Object> images = new ArrayList();
        addImage(images, 16);
        addImage(images, 32);
        addImage(images, 48);

        // sanity check
        Image image = WindowStageShim.findBestImage(images, 16, 16);
        assertEquals(16, image.getWidth());
        image = WindowStageShim.findBestImage(images, 48, 48);
        assertEquals(48, image.getWidth());

        //RT-39045
        image = WindowStageShim.findBestImage(images, 32, 32);
        assertEquals(32, image.getWidth());

        // scaling up 32 pixels by 4 is better than 48 by 2+2/3
        image = WindowStageShim.findBestImage(images, 128, 128);
        assertEquals(32, image.getWidth());
    }
}
