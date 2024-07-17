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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGRectangle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DirtyRegionClipTest extends DirtyRegionTestBase {

    /**
     * Gets the test parameters to use when running these tests. The parameters
     * are a combination of a Polluter and a Creator. The Creator is used to
     * create the node to be tested (might be a rectangle, or Group, or something
     * more complex), while the Polluter is responsible for making the test node
     * dirty by some means. Since the Polluter knows what it did to make the node
     * dirty, it is also responsible for computing and returning what the expected
     * change to the node's geometry is, such that the test code can create the
     * union and test for the appropriate dirty region for this specific node.
     */
    @Parameterized.Parameters
    public static Collection createParameters() {
        // This polluter will translate the test node in a positive direction
        final Polluter pollutePositiveTranslation = new Polluter() {
            { tx = BaseTransform.getTranslateInstance(50, 50); }
            @Override public void pollute(NGNode node) { DirtyRegionTestBase.transform(node, tx); }
            @Override public String toString() { return "Pollute Positive Translation"; }
        };

        // We will populate this list with the parameters with which we will test.
        // Each Object[] within the params is composed of a Creator and a Polluter.
        List<Object[]> params = new ArrayList<>();
        // A standard list of polluters which applies to all tests
        List<Polluter> polluters = Arrays.asList(new Polluter[]{
                pollutePositiveTranslation
        });

        // Construct the Creator / Polluter pair for Groups
        for (final Polluter polluter : polluters) {
            params.add(new Object[] {new Creator() {
                @Override public NGNode create() { return createGroup(createRectangle(0, 0, 100, 100)); }
                @Override public String toString() { return "Group with one Rectangle"; }
            }, polluter});
        }

        // Construct the Creator / Polluter pair for Rectangles
        List<Polluter> rectanglePolluters = new ArrayList<>(polluters);
        rectanglePolluters.add(new Polluter() {
            @Override public void pollute(NGNode node) {
                NGRectangle rect = (NGRectangle)node;
                BaseBounds bounds = rect.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
                rect.updateRectangle(bounds.getMinX(), bounds.getMinY(), 25, 25, 0, 0);
            }
            @Override public String toString() { return "Pollute Rectangle Geometry"; }
        });
        for (final Polluter polluter : rectanglePolluters) {
            params.add(new Object[] {new Creator() {
                @Override public NGNode create() { return createRectangle(0, 0, 100, 100); }
                @Override public String toString() { return "Rectangle"; }
            }, polluter});
        }
        // Return the populated params collection
        return params;
    }

    public DirtyRegionClipTest(Creator creator, Polluter polluter) {
        super(creator, polluter);
    }

    /**
     * Constructs a non-overlapping grid of nodes. Each node is a direct child
     * of the root node. They may end up overlapping when they become dirty,
     * but they don't start out that way! Each node is placed where it belongs
     * in the grid by translating them into place.
     */
    @Before public void setUp() {
        // create the grid
        NGNode[] content = new NGNode[9];
        for (int row=0; row<3; row++) {
            for (int col=0; col<3; col++) {
                NGNode node = creator.create();
                BaseTransform tx = BaseTransform.IDENTITY_TRANSFORM;
                tx = tx.deriveWithTranslation((col * 110), (row * 110));
                transform(node, tx);
                content[(row * 3) + col] = node;
            }
        }
        root = createGroup(content);

        // The grid is created & populated. We'll now go through and manually
        // clean them all up so that when we perform the test, it is from the
        // starting point of a completely cleaned tree
        root.render(TestGraphics.TEST_GRAPHICS);
        root.clearDirty();
    }

    @Test public void sanityCheck() {
        NGNode node = root.getChildren().get(0);
        assertEquals(new RectBounds(0, 0, 100, 100), node.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        assertEquals(new RectBounds(0, 0, 100, 100), node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));

        node = root.getChildren().get(1);
        assertEquals(new RectBounds(0, 0, 100, 100), node.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        assertEquals(new RectBounds(110, 0, 210, 100), node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));

        node = root.getChildren().get(root.getChildren().size()/2); //middle child (index 4)
        assertEquals(new RectBounds(0, 0, 100, 100), node.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        assertEquals(new RectBounds(110, 110, 210, 210), node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
    }

    /**
     * Dirty region bounds are bigger than clip bounds and whole clip lies
     * inside the dirty region.
     */
    @Test public void dirtyRegionContainsClip() {
        windowClip = new RectBounds(115, 115, 120, 120);

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        assertContainsClip(root, polluter.polluteAndGetExpectedBounds(middleChild), DirtyRegionContainer.DTR_CONTAINS_CLIP);
    }

    /**
     * Dirty region bounds partially overlap with clip bounds.
     */
    @Test public void dirtyRegionPartiallyOverlapsClip() {
        windowClip = new RectBounds(90, 90, 120, 120);

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        assertContainsClip(root, polluter.polluteAndGetExpectedBounds(middleChild), DirtyRegionContainer.DTR_OK);
    }

    /**
     * Dirty region bounds are smaller than clip bounds and the dirty region
     * lies inside the clip.
     */
    @Test public void dirtyRegionDoesNotContainClip() {
        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        assertContainsClip(root, polluter.polluteAndGetExpectedBounds(middleChild), DirtyRegionContainer.DTR_OK);
    }

}