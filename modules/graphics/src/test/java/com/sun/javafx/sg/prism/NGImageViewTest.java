/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.RectBounds;
import com.sun.prism.Image;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class NGImageViewTest extends NGTestBase {
    static final byte[] ICON_PIXELS = new byte[16*16];
    static final Image ICON = Image.fromByteRgbData(ICON_PIXELS, 16, 16);

    NGImageView imageView;

    @Before
    public void setup() {
        imageView = new NGImageView();
        imageView.setImage(ICON);
        imageView.setX(10);
        imageView.setY(10);
        imageView.setViewport(0, 0, 0, 0, 16, 16);
    }

    /**
     * ImageView always reports true for this
     */
    @Test
    public void testSupportsOpaqueRegion() {
        assertTrue(imageView.supportsOpaqueRegions());
    }

    /**
     * In the configured state it should report true
     */
    @Test
    public void hasOpaqueRegionWithNonEmptyImage() {
        assertTrue(imageView.hasOpaqueRegion());
    }

    /**
     * Tests when the view port is modified (not sure if I'm modifying the
     * view port exactly correctly...)
     */
    @Test
    public void hasOpaqueRegionIfViewPortGreaterThanOne() {
        assertTrue(imageView.hasOpaqueRegion());
        imageView.setViewport(0, 0, 2, 2, 16, 16);
        assertTrue(imageView.hasOpaqueRegion());
        imageView.setViewport(0, 0, 1, 1, 16, 16);
        assertTrue(imageView.hasOpaqueRegion());
        imageView.setViewport(0, 0, 0, 0, .1f, .1f);
        assertFalse(imageView.hasOpaqueRegion());
    }

    /**
     * Null images should return false for hasOpaqueImage
     */
    @Test
    public void doesNotHaveOpaqueRegionForNullImage() {
        imageView.setImage(null);
        assertFalse(imageView.hasOpaqueRegion());
    }

    /**
     * Simple test should match bounds for opaque image
     */
    @Test
    public void computeOpaqueRegionForWholeNumbers() {
        assertEquals(new RectBounds(10, 10, 26, 26), imageView.computeOpaqueRegion(new RectBounds()));
    }
}
