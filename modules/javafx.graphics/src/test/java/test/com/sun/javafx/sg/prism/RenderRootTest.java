/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGCircle;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGNodeShim;
import com.sun.javafx.sg.prism.NGRectangle;
import com.sun.javafx.sg.prism.NodePath;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the computation of the render root of a graph
 */
public class RenderRootTest extends NGTestBase {
    // NGNodes to test: NGRectangle, NGImageView, NGRegion, NGCircle, NGEllipse
    // Also thrown in 3D transforms and 2D transforms other than BASE_TRANSFORM
    // Structures to test: root, background, foreground
    //      - Foreground completely covers background.
    //      - Foreground partially overlaps background
    // Test each node has exactly the expected opaque region, given:
    //      - stroke
    //      - effect
    //      - clip (dirty region should only include the intersection of the clip & bounds)
    //      - transforms (transform onto a pixel boundary, or transform into a pixel crack)
    //      - fill
    //      - opaque image / transparent image
    //      - opacity
    //      - blend mode
    //      - x / y position on partial pixel boundaries
    //      - others?

    private NGRectangle rect;
    private NGGroup root;

    @Before
    public void setup() {
        rect = createRectangle(10, 10, 90, 90);
        root = createGroup(rect);
    }

    /**
     * Helper method to get the render root. We have to run both the markCullRegions and getRenderRoot methods
     * in order for getRenderRoot to return the correctly computed results.
     *
     * @param root The root node
     * @param dirtyX The x coordinate of the dirty region
     * @param dirtyY The y coordinate of the dirty region
     * @param dirtyWidth The width of the dirty region
     * @param dirtyHeight The height of the dirty region
     * @return The NodePath, or null if there are no path elements
     */
    private NodePath getRenderRoot(NGGroup root, int dirtyX, int dirtyY, int dirtyWidth, int dirtyHeight) {
        final DirtyRegionContainer drc = new DirtyRegionContainer(1);
        final RectBounds dirtyRegion = new RectBounds(dirtyX, dirtyY, dirtyX+dirtyWidth, dirtyY+dirtyHeight);
        drc.addDirtyRegion(dirtyRegion);
        final BaseTransform tx = BaseTransform.IDENTITY_TRANSFORM;
        final GeneralTransform3D pvTx = new GeneralTransform3D();
        NGNodeShim.markCullRegions(root, drc, -1, tx, pvTx);
        NodePath path = new NodePath();
        root.getRenderRoot(path, dirtyRegion, 0, tx, pvTx);
        return path;
    }

    /**
     * A quick note about how the NodePath works.
     *
     * If it is empty, then it means that nothing
     * needs to be painted (maybe there were some dirty nodes, but they were completely
     * occluded, so we don't need to paint anything).
     *
     * If it contains *only* the root node,
     * then either the root node itself is completely occluding the dirty region, or there was
     * no other render root to be found, so we have to paint the whole scene.
     *
     * If it contains something more than just the root node, then it will be the path from
     * the root node down to the render root child.
     *
     * This method takes the expected root (which may be null) and the rootPath (which can
     * never be null). If expectedRoot is null, rootPath must be empty. Otherwise,
     * expectedRoot must be the last item in rootPath.
     */
    private void assertRenderRoot(NGNode expectedRoot, NodePath rootPath) {
        if (expectedRoot == null) {
            assertTrue(rootPath.isEmpty());
        } else {
            // Get to the end
            while (rootPath.hasNext()) rootPath.next();
            assertSame(expectedRoot, rootPath.getCurrentNode());
        }
    }

    /**
     * Tests the case where the dirty region is completely within the opaque region.
     * The rect in this case is dirty.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegion() {
        NodePath rootPath = getRenderRoot(root, 20, 20, 70, 70);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Tests the case where the dirty region is completely within the opaque region.
     * The rect in this case is clean.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegion_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 20, 20, 70, 70);
        assertRenderRoot(null, rootPath);
    }

    /**
     * Tests the case where the dirty region exactly matches the opaque region.
     * The rect is dirty.
     */
    @Test
    public void dirtyRegionMatchesOpaqueRegion() {
        NodePath rootPath = getRenderRoot(root, 10, 10, 90, 90);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Tests the case where the dirty region exactly matches the opaque region.
     * The rect is clean.
     */
    @Test
    public void dirtyRegionMatchesOpaqueRegion_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 10, 10, 90, 90);
        assertRenderRoot(null, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same top edge. The rect is dirty.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesTop() {
        NodePath rootPath = getRenderRoot(root, 20, 10, 70, 70);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same top edge. The rect is clean.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesTop_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 20, 10, 70, 70);
        assertRenderRoot(null, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same right edge. The rect is dirty.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesRight() {
        NodePath rootPath = getRenderRoot(root, 20, 20, 80, 70);
        assertRenderRoot(rect, rootPath);
    }
    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same right edge. The rect is clean.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesRight_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 20, 20, 80, 70);
        assertRenderRoot(null, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same bottom edge. The rect is dirty.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesBottom() {
        NodePath rootPath = getRenderRoot(root, 20, 20, 70, 80);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same bottom edge. The rect is clean.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesBottom_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 20, 20, 70, 80);
        assertRenderRoot(null, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same left edge. The rect is dirty.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesLeft() {
        NodePath rootPath = getRenderRoot(root, 10, 20, 70, 70);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Tests the case where the dirty region is within the opaque region, but shares the
     * same left edge. The rect is clean.
     */
    @Test
    public void dirtyRegionWithinOpaqueRegionTouchesLeft_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 10, 20, 70, 70);
        assertRenderRoot(null, rootPath);
    }

    @Test
    public void opaqueRegionWithinDirtyRegion() {
        NodePath rootPath = getRenderRoot(root, 0, 0, 110, 110);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void opaqueRegionWithinDirtyRegion_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 0, 0, 110, 110);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionTop() {
        NodePath rootPath = getRenderRoot(root, 20, 0, 70, 30);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionTop_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 20, 0, 70, 30);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionRight() {
        NodePath rootPath = getRenderRoot(root, 90, 20, 30, 70);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionRight_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 90, 20, 30, 70);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionBottom() {
        NodePath rootPath = getRenderRoot(root, 20, 90, 70, 30);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionBottom_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 20, 90, 70, 30);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionLeft() {
        NodePath rootPath = getRenderRoot(root, 0, 20, 30, 70);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionIntersectsOpaqueRegionLeft_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 0, 20, 30, 70);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionCompletelyOutsideOfOpaqueRegion() {
        NodePath rootPath = getRenderRoot(root, 0, 0, 5, 5);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void dirtyRegionCompletelyOutsideOfOpaqueRegion_Clean() {
        root.clearDirtyTree();
        NodePath rootPath = getRenderRoot(root, 0, 0, 5, 5);
        assertRenderRoot(root, rootPath);
    }

    // A RectBounds is considered as empty only when maxX < minX or maxY < minY.
    // see RectBounds.isEmpty() and RectBounds.makeEmpty()
    // What is the right thing here?
    // 1. Empty dirty region should result in no rendering.
    //    It means that getRenderRoot() should return an empty NodePath when dirty region is an empty rect.
    // OR
    // 2. Empty dirty region should result in rendering entire root tree.(This is current behavior)
    //
    // Changing this behavior may cause rendering glitches for any application that
    // relies on this current behavior to render all root tree.
    // So we need to be extensive in testing before modifying the behavior.
    @Ignore("JDK-8265510")
    @Test
    public void emptyDirtyRegion1() {
        NodePath rootPath = getRenderRoot(root, 0, 0, -1, -1);
        assertRenderRoot(root, rootPath);
        // OR
        // assertRenderRoot(null, rootPath);
    }

    @Ignore("JDK-8265510")
    @Test
    public void emptyDirtyRegion2() {
        NodePath rootPath = getRenderRoot(root, -1, -1, -2, -2);
        assertRenderRoot(root, rootPath);
        // OR
        // assertRenderRoot(null, rootPath);
    }

    @Ignore("JDK-8265510")
    @Test
    public void invalidDirtyRegionOutsideOpaqueRegion() {
        NodePath rootPath = getRenderRoot(root, -10, -10, 5, 5);
        assertRenderRoot(root, rootPath);
        // OR
        // assertRenderRoot(null, rootPath);
    }

    // A RectBounds is considered as empty only when maxX < minX or maxY < minY.
    // see RectBounds.isEmpty() and RectBounds.makeEmpty()
    // So a RectBounds with 0 width and 0 height is not considered as an empty rect.
    @Test
    public void zeroSizeDirtyRegionWithinOpaqueRegion() {
        NodePath rootPath = getRenderRoot(root, 20, 20, 0, 0);
        assertRenderRoot(rect, rootPath);
    }

    @Test
    public void zeroSizeDirtyRegionOutsideOpaqueRegion1() {
        NodePath rootPath = getRenderRoot(root, 0, 0, 0, 0);
        assertRenderRoot(root, rootPath);
    }

    @Test
    public void zeroSizeDirtyRegionOutsideOpaqueRegion2() {
        NodePath rootPath = getRenderRoot(root, 5, 5, 0, 0);
        assertRenderRoot(root, rootPath);
    }

    /**
     * Tests that a clip works. Note that I send the dirty region to be the same
     * size as what I expect the clip to be, so that the test will fail if the
     * dirty region ends up being larger than the computed clip.
     */
    @Test
    public void withRectangularClip() {
        NGRectangle clip = createRectangle(20, 20, 70, 70);
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 20, 20, 70, 70);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * The negative test, where the clip is smaller than the dirty region
     */
    @Test
    public void withRectangularClip_negative() {
        NGRectangle clip = createRectangle(20, 20, 70, 70);
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 19, 20, 70, 70);
        assertRenderRoot(root, rootPath);
    }

    /**
     * Tests that a clip works when translated.
     */
    @Test
    public void withRectangularClipTranslated() {
        NGRectangle clip = createRectangle(20, 20, 70, 70);
        clip.setTransformMatrix(BaseTransform.getTranslateInstance(10, 10));
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 30, 30, 70, 70);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Tests that a clip works when translated.
     */
    @Test
    public void withRectangularClipTranslated_negative() {
        NGRectangle clip = createRectangle(20, 20, 70, 70);
        clip.setTransformMatrix(BaseTransform.getTranslateInstance(10, 10));
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 29, 30, 70, 70);
        assertRenderRoot(root, rootPath);
    }

    /**
     * Note, scales about origin, not center
     */
    @Test
    public void withRectangularClipScaled() {
        NGRectangle clip = createRectangle(20, 20, 70, 70);
        clip.setTransformMatrix(BaseTransform.getScaleInstance(.5, .5));
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 10, 10, 35, 35);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Note, scales about origin, not center
     */
    @Test
    public void withRectangularClipScaled_negative() {
        NGRectangle clip = createRectangle(20, 20, 70, 70);
        clip.setTransformMatrix(BaseTransform.getScaleInstance(.5, .5));
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 9, 10, 35, 35);
        assertRenderRoot(root, rootPath);
    }

    /**
     * We can now easily use ellipse and ellipse and images etc as clips
     * in addition to rect clips. Here I choose a dirty region that is
     * clearly in the center of the ellipse's area so as to pass the test.
     */
    @Test
    public void withCircleClip() {
        NGCircle clip = createCircle(50, 50, 45);
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 40, 40, 20, 20);
        assertRenderRoot(rect, rootPath);
    }

    /**
     * Make the dirty area larger than the clip so as to fail.
     */
    @Test
    public void withCircleClip_negative() {
        NGCircle clip = createCircle(50, 50, 45);
        rect.setClipNode(clip);
        NodePath rootPath = getRenderRoot(root, 10, 10, 90, 90);
        assertRenderRoot(root, rootPath);
    }
}
