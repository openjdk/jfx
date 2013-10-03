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

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class NGRegionTest {
    @Test public void setOpaqueInsetsInvalidatesOpaqueRegion() {
        NGRegion r = new NGRegion();
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.setOpaqueInsets(0, 0, 0, 0);
        assertTrue(r.isOpaqueRegionInvalid());
    }

    @Test public void updateShapeInvalidatesOpaqueRegion() {
        NGRegion r = new NGRegion();
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.updateShape(null, true, false, false); // Actual values don't matter
        assertTrue(r.isOpaqueRegionInvalid());
    }

    @Test public void setSizeInvalidatesOpaqueRegion() {
        NGRegion r = new NGRegion();
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.setSize(100, 100);
        assertTrue(r.isOpaqueRegionInvalid());
    }

    @Ignore ("This test fails because of a bug that needs to be filed")
    @Test public void updateBackgroundWithSameSizeButTransparentFillInvalidatesOpaqueInsets() {
        NGRegion r = new NGRegion();
        r.updateBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.updateBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        assertTrue(r.isOpaqueRegionInvalid());
    }

    @Ignore ("A real bug here. First because it was RED and is now TRANSPARENT, and second because " +
            "we have insets now, which should have impacted rendering and affected the opaque insets, right?")
    @Test public void updateBackgroundWithDifferentSizeBackgroundInvalidatesOpaqueInsets() {
        NGRegion r = new NGRegion();
        r.updateBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.updateBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, new Insets(10))));
        assertTrue(r.isOpaqueRegionInvalid());
    }

    @Ignore ("A real bug here. We have insets now, which should have impacted rendering and affected the opaque insets, right?")
    @Test public void updateBackgroundWithDifferentSizeBackgroundInvalidatesOpaqueInsets2() {
        NGRegion r = new NGRegion();
        r.updateBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.updateBackground(new Background(new BackgroundFill(Color.RED, null, new Insets(10))));
        assertTrue(r.isOpaqueRegionInvalid());
    }

    @Test public void updateBackgroundWithDifferentSizeBackgroundInvalidatesOpaqueInsets3() {
        NGRegion r = new NGRegion();
        r.updateBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(r.isOpaqueRegionInvalid()); // sanity check
        r.updateBackground(new Background(new BackgroundFill(Color.RED, null, new Insets(-10))));
        assertTrue(r.isOpaqueRegionInvalid());
    }
}
