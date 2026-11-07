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

package test.com.sun.prism;

import java.nio.IntBuffer;

import com.sun.javafx.geom.Rectangle;
import com.sun.prism.Image;

import javafx.util.Pair;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImageSerialTest {

    @Test
    public void burstOfWritesShouldCoalesceIntoASingleRegion() {
        Image image = Image.fromIntArgbPreData(IntBuffer.allocate(64), 8, 8);
        Image.Serial serial = image.getSerial();

        image.bufferDirty(new Rectangle(1, 2, 3, 4));
        image.bufferDirty(new Rectangle(10, 20, 5, 6));  // same burst, no read in between

        Pair<Integer, Rectangle> idRect = serial.getIdRect();

        assertEquals(1, idRect.getKey());  // a single generation for the whole burst
        assertEquals(new Rectangle(1, 2, 14, 24), idRect.getValue());  // the union of both regions
    }

    @Test
    public void writeAfterReadShouldStartANewGeneration() {
        Image image = Image.fromIntArgbPreData(IntBuffer.allocate(64), 8, 8);
        Image.Serial serial = image.getSerial();

        image.bufferDirty(new Rectangle(1, 2, 3, 4));
        serial.getIdRect();  // the burst is read
        image.bufferDirty(new Rectangle(10, 20, 5, 6));  // the next burst

        Pair<Integer, Rectangle> idRect = serial.getIdRect();

        assertEquals(2, idRect.getKey());  // a new generation
        assertEquals(new Rectangle(10, 20, 5, 6), idRect.getValue());
    }

    @Test
    public void fullInvalidationShouldClearTheAccumulatedRegion() {
        Image image = Image.fromIntArgbPreData(IntBuffer.allocate(64), 8, 8);
        Image.Serial serial = image.getSerial();

        image.bufferDirty(new Rectangle(1, 2, 3, 4));
        image.bufferDirty(null);  // a full invalidation

        Pair<Integer, Rectangle> idRect = serial.getIdRect();

        assertNull(idRect.getValue());
    }
}
