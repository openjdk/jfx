/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.sg.prism;

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGNodeShim;
import com.sun.javafx.sg.prism.NGRegion;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
public class NGRegionTest {
    @Test
    public void setOpaqueInsetsInvalidatesOpaqueRegion() {
        NGRegion r = new NGRegion();
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(r)); // sanity check
        r.setOpaqueInsets(0, 0, 0, 0);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(r));
    }

    @Test
    public void updateShapeInvalidatesOpaqueRegion() {
        NGRegion r = new NGRegion();
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(r)); // sanity check
        r.updateShape(null, true, false, false); // Actual values don't matter
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(r));
    }

    // RT-13820: We change the shape internally and call this same method, so it
    // needs to invalidate the opaque region.
    @Test
    public void updateShapeToSameInstanceInvalidatesOpaqueRegion() {
        LineTo lineTo;
        Path p = new Path(
                new MoveTo(10, 20),
                lineTo = new LineTo(100, 100),
                new ClosePath()
        );

        NGRegion r = new NGRegion();
        r.updateShape(p, true, true, true);
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(r)); // sanity check
        lineTo.setX(200);
        r.updateShape(p, true, true, true);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(r));
    }

    @Test
    public void setSizeInvalidatesOpaqueRegion() {
        NGRegion r = new NGRegion();
        r.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(r)); // sanity check
        r.setSize(100, 100);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(r));
    }

    // Note: These tests are using a Region and doing a sync because it was found that
    // doing the check directly on the updateBackground method itself gave incorrect
    // results, but doing so via Region's sync worked correctly (because every time a
    // background is changed on the Region, setOpaqueInsets is called which invalidates
    // the opaque region).

    @Test
    public void updateBackgroundWithSameSizeButTransparentFillInvalidatesOpaqueInsets() {
        Region r = new Region();
        NGRegion peer = NodeHelper.getPeer(r);
        r.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        NodeHelper.updatePeer(r);
        peer.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(peer)); // sanity check
        r.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        NodeHelper.updatePeer(r);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(peer));
    }

    @Test
    public void updateBackgroundWithDifferentSizeBackgroundInvalidatesOpaqueInsets() {
        Region r = new Region();
        NGRegion peer = NodeHelper.getPeer(r);
        r.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        NodeHelper.updatePeer(r);
        peer.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(peer)); // sanity check
        r.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, new Insets(10))));
        NodeHelper.updatePeer(r);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(peer));
    }

    @Test
    public void updateBackgroundWithDifferentSizeBackgroundInvalidatesOpaqueInsets2() {
        Region r = new Region();
        NGRegion peer = NodeHelper.getPeer(r);
        r.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        NodeHelper.updatePeer(r);
        peer.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(peer)); // sanity check
        r.setBackground(new Background(new BackgroundFill(Color.RED, null, new Insets(10))));
        NodeHelper.updatePeer(r);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(peer));
    }

    @Test
    public void updateBackgroundWithDifferentSizeBackgroundInvalidatesOpaqueInsets3() {
        Region r = new Region();
        NGRegion peer = NodeHelper.getPeer(r);
        r.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
        NodeHelper.updatePeer(r);
        peer.getOpaqueRegion(); // Forces to validate the opaque region
        assertFalse(NGNodeShim.isOpaqueRegionInvalid(peer)); // sanity check
        r.setBackground(new Background(new BackgroundFill(Color.RED, null, new Insets(-10))));
        NodeHelper.updatePeer(r);
        assertTrue(NGNodeShim.isOpaqueRegionInvalid(peer));
    }
}
