/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * BoundsTest.fx
 *
 * Created on Sep 2, 2008, 00 5:28 PM
 */

package javafx.scene.bounds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import javafx.geometry.Bounds;

import org.junit.Test;

public class BoundsPerformanceTest {
   
    /***************************************************************************
     *                                                                         *
     *                            Performance Tests                            *
     *                                                                         *
     *  This section of the tests is for testing that the minimal amount of    *
     *  work is happening during various scenegraph manipulations. For example *
     *  if I change the translateX of a Node, that should not invalidate the   *
     *  geometric bounds or boundsInLocal, but it should lead to recomputing   *
     *  the boundsInParent (but should use the cached bounds).                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Tests that if a transform is changed on a leaf node, that the change
     * only invalidates and requires recomputation of boundsInParent --
     * boundsInLocal and geom bounds are not touched.
     */
    public @Test void testPerformance_TransformChangesOnlyAffectBoundsInParent() {
        PerfNode n = new PerfNode();
        Bounds originalBoundsInParent = n.getBoundsInParent();
        Bounds originalBoundsInLocal = n.getBoundsInLocal();
        Bounds originalLayoutBounds = n.getLayoutBounds();
        n.geomComputeCount = 0; // clear
        n.setTranslateX(100);
        Bounds newBoundsInParent = n.getBoundsInParent();
        Bounds newBoundsInLocal = n.getBoundsInLocal();
        Bounds newLayoutBounds = n.getLayoutBounds();
        // assert that the compute geom method didn't get called
        assertEquals(0, n.geomComputeCount);
        // the bounds in local and layout bounds shouldn't have changed
        assertSame(originalBoundsInLocal, newBoundsInLocal);
        assertSame(originalLayoutBounds, newLayoutBounds);
        // the boundsInParent should have changed
        assertNotSame(originalBoundsInParent, newBoundsInParent);
    }

    public @Test void testPerformance_GeomChangesAffectEverything() {
        PerfNode n = new PerfNode();
        Bounds originalBoundsInParent = n.getBoundsInParent();
        Bounds originalBoundsInLocal = n.getBoundsInLocal();
        Bounds originalLayoutBounds = n.getLayoutBounds();
        n.geomComputeCount = 0; // clear
        n.setX(100);
        Bounds newBoundsInParent = n.getBoundsInParent();
        Bounds newBoundsInLocal = n.getBoundsInLocal();
        Bounds newLayoutBounds = n.getLayoutBounds();
        // assert that the compute geom method was called once
        assertEquals(1, n.geomComputeCount);
        // all bounds should have changed
        assertNotSame(originalBoundsInLocal, newBoundsInLocal);
        assertNotSame(originalLayoutBounds, newLayoutBounds);
        assertNotSame(originalBoundsInParent, newBoundsInParent);
    }

    public @Test void testPerformance_ComputeGeomNotCalledDuringStartup() {
        PerfNode n = new PerfNode(100, 100, 10, 10);
        assertEquals(0, n.geomComputeCount);

        // sanity check -- should trigger call to compute geom on node
        n.getLayoutBounds();
        n.getBoundsInParent();
        assertEquals(1, n.geomComputeCount);
    }

    // Tests that if I set the x or y on the ResizablePerfNode, that it doesn't
    // * cause a new layout bounds to be created.
    public @Test void testPerformance_LayoutBoundsOfResizableNotAffectedByChangesToOtherGeom() {
        ResizablePerfNode n = new ResizablePerfNode();
        Bounds originalLayoutBounds = n.getLayoutBounds();
        n.setX(100);
        Bounds newLayoutBounds = n.getLayoutBounds();
        assertSame(originalLayoutBounds, newLayoutBounds);
        n.setWidth(50);
        newLayoutBounds = n.getLayoutBounds();
        assertNotSame(originalLayoutBounds, newLayoutBounds);
    }

    public @Test void testPerformance_ChangingMultipleGeomOnlyCallsComputeGeomOnce() {
        PerfNode n = new PerfNode();
        Bounds originalBoundsInParent = n.getBoundsInParent();
        Bounds originalBoundsInLocal = n.getBoundsInLocal();
        Bounds originalLayoutBounds = n.getLayoutBounds();
        n.geomComputeCount = 0; // clear
        n.setX(100);
        n.setY(100);
        n.setWidth(50);
        n.setHeight(50);
        Bounds newBoundsInParent = n.getBoundsInParent();
        Bounds newBoundsInLocal = n.getBoundsInLocal();
        Bounds newLayoutBounds = n.getLayoutBounds();
        // assert that the compute geom method was called once
        assertEquals(1, n.geomComputeCount);
        // all bounds should have changed
        assertNotSame(originalBoundsInLocal, newBoundsInLocal);
        assertNotSame(originalLayoutBounds, newLayoutBounds);
        assertNotSame(originalBoundsInParent, newBoundsInParent);
    }
}
