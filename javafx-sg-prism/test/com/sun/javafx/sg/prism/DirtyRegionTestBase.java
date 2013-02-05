/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.BaseNode;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.prism.NodeTestUtils.TestNGGroup;
import com.sun.prism.paint.Color;
import com.sun.scenario.effect.Effect;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * A base class for all testing of the dirty regions. This class contains
 * some useful infrastructure for testing dirty regions, such as the ability
 * to assert that a dirty region matches the expected region; the ability to
 * manage the state on an NG node appropriately (ensuring that the transform,
 * transformed bounds, effect, etc are all managed correctly); and ensuring
 * that all test are run over a set of common parameters, such as when a
 * Node becomes dirty due to opacity changing, visibility changing, geometry
 * changing, and so forth.
 * <p>
 * The DirtyRegionTestBase is parametrized, using different node types
 * (rectangle, circle, group) and different methods for becoming dirty
 * (visibility change, geometry change, etc). The cross product of these
 * forms the parameters for the test. Each test method is called using the
 * combination of a node type & dirty method.
 */
public class DirtyRegionTestBase {
    /**
     * Gets the test parameters to use when running these tests. The parameters
     * are a combination of a Polluter and a Creator. The Creator is used to
     * create the node to be tested (might be a rectangle, or Group, or something
     * more complex), while the Polluter is responsible for making the test node
     * dirty by some means. Since the Pollutor knows what it did to make the node
     * dirty, it is also responsible for computing and returning what the expected
     * change to the node's geometry is, such that the test code can create the
     * union and test for the appropriate dirty region for this specific node.
     */
    @Parameterized.Parameters
    public static Collection createParameters() {
        // This polluter will change the opacity of the test node
        final Polluter polluteOpacity = new Polluter() {
            @Override public void pollute(NGNode node) { node.setOpacity(.5f); }
            @Override public String toString() { return "Pollute Opacity"; }
        };
        // This polluter will restore the opacity of the test node. That is,
        // the test node did have 0 opacity and now it is going to be changed
        // to an opacity of 1.
        final Polluter restoreOpacity = new Polluter() {
            @Override public void pollute(NGNode node) {
                // I need to hide the node, and then clean up all the dirty
                // state associated with it, and then make it visible again.
                // This simulates making it invisible, painting, and then
                // making it visible again.
                node.setOpacity(0f);
                BaseNode parent = node;
                while(parent.getParent() != null) parent = parent.getParent();
                parent.render(TestGraphics.TEST_GRAPHICS);
                // Now we can go ahead and set the opacity
                node.setOpacity(1f);
            }
            @Override public String toString() { return "Restore Opacity"; }
        };
        // This polluter will change the fill of the node. This only works if
        // the test node is of a shape type (the code which creates the test
        // parameters will make sure to only use polluteFill with creators
        // which are create Shapes)
        final Polluter polluteFill = new Polluter() {
            @Override public void pollute(NGNode node) {
                if (node instanceof NGShape) {
                    com.sun.javafx.sg.prism.NGShape shape = (NGShape)node;
                    shape.setFillPaint(new Color(.43f, .23f, .66f, 1f));
                } else if (node instanceof NGRegion) {
                    NGRegion region = (NGRegion)node;
                    javafx.scene.paint.Color color = new javafx.scene.paint.Color(.43f, .23f, .66f, 1f);
                    // I have to do this nasty reflection trickery because we don't have a Toolkit for creating
                    // the Prism Color that is the platform peer.
                    try {
                        Field platformPaint = color.getClass().getDeclaredField("platformPaint");
                        platformPaint.setAccessible(true);
                        platformPaint.set(color, new Color(.43f, .23f, .66f, 1f));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    region.updateBackground(new Background(new BackgroundFill[] {
                        new BackgroundFill(
                                color,
                                CornerRadii.EMPTY, Insets.EMPTY)
                    }));
                } else {
                    throw new IllegalArgumentException("I don't know how to make the fill dirty on " + node);
                }
            }
            @Override public String toString() { return "Pollute Fill"; }
        };
        // This polluter will translate the test node in a positive direction
        final Polluter pollutePositiveTranslation = new Polluter() {
            { tx = BaseTransform.getTranslateInstance(50, 50); }
            @Override public void pollute(NGNode node) { DirtyRegionTestBase.transform(node, tx); }
            @Override public String toString() { return "Pollute Positive Translation"; }
        };
        // This polluter will translate the test node in a negative direction
        final Polluter polluteNegativeTranslation = new Polluter() {
            { tx = BaseTransform.getTranslateInstance(-50, -50); }
            @Override public void pollute(NGNode node) { DirtyRegionTestBase.transform(node, tx); }
            @Override public String toString() { return "Pollute Negative Translation"; }
        };
        // This polluter will give the test node a scale causing it to get bigger
        final Polluter polluteBiggerScale = new Polluter() {
            { tx = BaseTransform.getScaleInstance(2, 2); }
            @Override public void pollute(NGNode node) { DirtyRegionTestBase.transform(node, tx); }
            @Override public String toString() { return "Pollute Bigger Scale"; }
        };
        // This polluter will give the test node a scale causing it to get smaller
        final Polluter polluteSmallerScale = new Polluter() {
            { tx = BaseTransform.getScaleInstance(.5, .5); }
            @Override public void pollute(NGNode node) { DirtyRegionTestBase.transform(node, tx); }
            @Override public String toString() { return "Pollute Smaller Scale"; }
        };
        // This polluter will rotate the node about its center
        final Polluter polluteRotate = new Polluter() {
            @Override public void pollute(NGNode node) {
                BaseBounds effectBounds = node.getEffectBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
                BaseTransform tx = BaseTransform.getRotateInstance(45, effectBounds.getWidth()/2f, effectBounds.getHeight()/2f);
                DirtyRegionTestBase.transform(node, tx);
            }
            @Override public BaseBounds modifiedBounds(NGNode node) {
                BaseBounds effectBounds = node.getEffectBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
                BaseTransform tx = BaseTransform.getRotateInstance(45, effectBounds.getWidth() / 2f, effectBounds.getHeight() / 2f);
                return DirtyRegionTestBase.getWhatTransformedBoundsWouldBe(node, tx);
            }
            @Override public String toString() { return "Pollute Rotate"; }
        };
        // This polluter will make the test node invisible
        final Polluter polluteVisibility = new Polluter() {
            @Override public void pollute(NGNode node) {
                node.setVisible(false);
            }
            @Override public String toString() { return "Pollute Visibility"; }
        };
        // This polluter will make an invisible node visible again
        final Polluter restoreVisibility = new Polluter() {
            @Override public void pollute(NGNode node) {
                // I need to hide the node, and then clean up all the dirty
                // state associated with it, and then make it visible again.
                // This simulates making it invisible, painting, and then
                // making it visible again.
                node.setVisible(false);
                BaseNode parent = node;
                while(parent.getParent() != null) parent = parent.getParent();
                parent.render(TestGraphics.TEST_GRAPHICS);
                // Now we can go ahead and set the opacity
                node.setVisible(true);
            }
            @Override public String toString() { return "Restore Visibility"; }
        };

        // We will populate this list with the parameters with which we will test.
        // Each Object[] within the params is composed of a Creator and a Polluter.
        List<Object[]> params = new ArrayList<Object[]>();
        // A standard list of polluters which applies to all tests
        List<Polluter> polluters = Arrays.asList(new Polluter[]{
                polluteRotate,
                polluteOpacity,
                restoreOpacity,
                polluteVisibility,
                restoreVisibility,
                polluteSmallerScale,
                polluteNegativeTranslation,
                polluteBiggerScale,
                pollutePositiveTranslation
        });
        // Construct the Creator / Polluter pair for Groups
        for (final Polluter polluter : polluters) {
            params.add(new Object[] {new Creator() {
                @Override public NGNode create() { return NodeTestUtils.createGroup(NodeTestUtils.createRectangle(0, 0, 100, 100)); }
                @Override public String toString() { return "Group with one Rectangle"; }
            }, polluter});
        }
        // Construct the Creator / Polluter pair for Rectangles
        List<Polluter> rectanglePolluters = new ArrayList<Polluter>(polluters);
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
                @Override public NGNode create() { return NodeTestUtils.createRectangle(0, 0, 100, 100); }
                @Override public String toString() { return "Rectangle"; }
            }, polluter});
        }
        // Construct the Creator / Polluter pair for Circles
        List<Polluter> circlePolluters = new ArrayList<Polluter>(polluters);
        circlePolluters.add(new Polluter() {
            @Override public void pollute(NGNode node) {
                NGCircle c = (NGCircle)node;
                BaseBounds bounds = c.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
                c.updateCircle(
                        bounds.getMinX() + (bounds.getWidth()/2f),
                        bounds.getMinY() + (bounds.getHeight()/2f),
                        10);
            }
            @Override public String toString() { return "Pollute Circle Geometry"; }
        });
        for (final Polluter polluter : circlePolluters) {
            params.add(new Object[] {new Creator() {
                @Override public NGNode create() { return NodeTestUtils.createCircle(50, 50, 50); }
                @Override public String toString() { return "Circle"; }
            }, polluter});
        }
        // Return the populated params collection
        return params;
    }

    /**
     * The test node creator. This is called from within the "setUp" method in each
     * subclass to create the nodes that are going to be tested.
     */
    protected Creator creator;

    /**
     * The polluter. Subclasses will use the polluter to make a node dirty at the
     * appropriate time in the test method.
     */
    protected Polluter polluter;

    /**
     * The root node. This must be created during the "setUp" method in each sub
     * class. The root is needed for actually accumulating dirty regions.
     */
    protected TestNGGroup root;

    /**
     * The clip to use when accumulating dirty regions. By default it is
     * ridiculously large, such that none of the tests will ever bump up
     * against the clip. Subclasses should however implement some tests in
     * which they will set the clip to a specific value, and then test
     * whether accumulating dirty regions takes the clip into account.
     */
    protected RectBounds windowClip = new RectBounds(-100000, -100000, 100000, 10000);

    /**
     * Creates a new DirtyRegionTestBase. Each subclass must have an identical
     * constructor which simply passes the creator and polluter to this
     * constructor. These instances are passed to the constructor by JUnit,
     * so sub classes don't need to worry about creating these instances
     * (and in fact must not do so).
     */
    protected DirtyRegionTestBase(Creator creator, Polluter polluter) {
        this.creator = creator;
        this.polluter = polluter;
    }

    /**
     * Helper method for asserting that the dirty region of the node indicated
     * (start) matches the dirty region which is expected. This method will
     * invoke the accumulateDirtyRegions method on the start node. Accumulating
     * dirty regions requires a clip to be sent along as well. The windowClip is
     * specified on DirtyRegionTestBase (by default it is ridiculously large)
     * but sub classes can change the clip at any time.
     *
     */
    protected void assertDirtyRegionEquals(NGNode start, RectBounds expected) {
        // TODO The root might have changes to its bounds and we should reset these. (RT-26928)
        //DirtyRegionTestBase.resetGroupBounds(root);
        // Accumulate the dirty region, using the windowClip.
        // TODO if we wanted to, we could also make the device space transform parameterized
        // such that we could test that the dirty region accumulation logic all works
        // correctly even in the presence of a non-identity device space transform
        // (RT-26928)
		DirtyRegionPool pool = new DirtyRegionPool(1);
		DirtyRegionContainer drc = pool.checkOut();
        int status = start.accumulateDirtyRegions(
                windowClip,
                new RectBounds(), pool,
                drc,
                BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        
        RectBounds dirtyRegion = drc.getDirtyRegion(0) ;
        
        // The accumulation of dirty regions ends up with a slightly
        // padded dirty region, just to make up for any error when
        // transforming. Its quick and dirty but does the job.
        // Perhaps we could avoid adding the slop in the case where
        // there is no rotation or skew involved, but for now we
        // don't. I don't want to populate all my tests with this
        // assumption though in case it ever changes. So I am going
        // to pad the expected here.
        expected = new RectBounds(
                Math.max(expected.getMinX() - 1, dirtyRegion.getMinX()),
                Math.max(expected.getMinY() - 1, dirtyRegion.getMinY()),
                Math.min(expected.getMaxX() + 1, dirtyRegion.getMaxX()),
                Math.min(expected.getMaxY() + 1, dirtyRegion.getMaxY()));
        // Now make the check, and print useful error information in case it fails.
        assertEquals("creator=" + creator + ", polluter=" + polluter, expected, dirtyRegion);
    }

    /**
     * Accumulates the dirty region, and checks to make sure the the accumulateDirtyRegion
     * method was only called on the nodes supplied. If any node in the tree (starting with
     * and including root) have had accumulateDirtyRegion called and they are NOT in this
     * list of expected nodes, then the assertion fails.
     * <p>
     * This assertion is used to make sure that various performance optimizations are
     * implemented correctly, such that we are not asking nodes to accumulate dirty regions
     * who have no hope of being in the dirty region (such as children of a clean group).
     *
     * @param nodes A non-null array of nodes.
     */
    protected void assertOnlyTheseNodesAreAskedToAccumulateDirtyRegions(NGNode... nodes) {
        accumulateDirtyRegions();
        Set<NGNode> set = new HashSet<NGNode>(Arrays.asList(nodes));
        assertOnlyTheseNodesWereAskedToAccumulateDirtyRegions(root, set);
    }

    /**
     * Accumulates the dirty region, and checks to make sure that the dirty region
     * computation methods (accumulateNodeDirtyRegion, computeDirtyRegion,
     * accumulateGroupDirtyRegion) are only called on the nodes supplied.  If any
     * node in the tree (starting with and including the root) has had one or more
     * of these methods called and they are NOT in the array of expected nodes, then
     * the assertion fails.
     * <p>
     * This assertion is used to make sure various performance optimizations are
     * implemented correctly, such that even if a node is asked to accumulate its
     * dirty region, it doesn't actually do any work if the node is actually clean.
     *
     * @param nodes A non-null array of nodes.
     */
    protected void assertOnlyTheseNodesAreAskedToComputeDirtyRegions(NGNode... nodes) {
        accumulateDirtyRegions();
        Set<NGNode> set = new HashSet<NGNode>(Arrays.asList(nodes));
        assertOnlyTheseNodesWereAskedToComputeDirtyRegions(root, set);
    }

    /**
     * Helper method which will reset the group bounds of the root prior to accumulating
     * dirty regions. The root node might have new bounds due to changes in its children,
     * such as transforms or geometry changes.
     */
    private void accumulateDirtyRegions() {
	    DirtyRegionPool pool = new DirtyRegionPool(1);
        DirtyRegionTestBase.resetGroupBounds(root);
        root.accumulateDirtyRegions(
                new RectBounds(0, 0, 800, 600),
                new RectBounds(), pool,
                pool.checkOut(),
                BaseTransform.IDENTITY_TRANSFORM,
                new GeneralTransform3D());
    }

    /**
     * Helper which walks down the tree checking to see if accumulateDirtyRegion has been
     * called, and throws an exception if it was not expected.
     */
    private void assertOnlyTheseNodesWereAskedToAccumulateDirtyRegions(NGNode start, Set<NGNode> nodes) {
        assertEquals(
                "creator=" + creator + ", polluter=" + polluter,
                nodes.contains(start), ((TestNGNode)start).askedToAccumulateDirtyRegion());
        if (start instanceof NGGroup) {
            for (PGNode child : ((NGGroup)start).getChildren()) {
                assertOnlyTheseNodesWereAskedToAccumulateDirtyRegions((NGNode)child, nodes);
            }
        }
    }

    /**
     * Helper which walks down the tree checking to see if any of the methods which actually
     * compute the dirty region have been called, and throws an exception if it was not expected.
     */
    private void assertOnlyTheseNodesWereAskedToComputeDirtyRegions(NGNode start, Set<NGNode> nodes) {
        assertEquals(
                "creator=" + creator + ", polluter=" + polluter,
                nodes.contains(start), ((TestNGNode)start).computedDirtyRegion());
        if (start instanceof NGGroup) {
            for (PGNode child : ((NGGroup)start).getChildren()) {
                assertOnlyTheseNodesWereAskedToComputeDirtyRegions((NGNode)child, nodes);
            }
        }
    }

   

    static protected void resetGroupBounds(NGGroup group) {
        BaseBounds contentBounds = new RectBounds();
        for (PGNode c : group.getChildren()) {
            NGNode child = (NGNode)c;
            contentBounds = contentBounds.deriveWithUnion(
                    child.getCompleteBounds(
                            new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        }
        BaseBounds currentContentBounds = group.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        if (!contentBounds.equals(currentContentBounds)) {
            System.out.println("CurrentContentBounds=" + currentContentBounds + ", bounds=" + contentBounds);
            group.setContentBounds(contentBounds);
            group.setTransformedBounds(group.getEffectBounds(new RectBounds(), group.getTransform()), false);
        }
    }

    /**
     * Sort of a non-applying version of the transform method. This method will
     * compute and return what the transformed bounds of the given node would
     * be if the transform were applied to it.
     */
    static protected BaseBounds getWhatTransformedBoundsWouldBe(NGNode node, BaseTransform tx) {
        BaseTransform existing = BaseTransform.IDENTITY_TRANSFORM.deriveWithNewTransform(node.getTransform());
        tx = existing.deriveWithConcatenation(tx);
        return node.getEffectBounds(new RectBounds(), tx);
    }

    static protected void transform(NGNode node, BaseTransform tx) {
        // Concatenate this transform with the one already on the node
        tx = node.getTransform().deriveWithConcatenation(tx);
        // Compute & set the new transformed bounds for the node
        node.setTransformedBounds(node.getEffectBounds(new RectBounds(), tx), false);
        // Set the transform matrix
        node.setTransformMatrix(tx);
    }

    static protected void translate(NGNode node, double tx, double ty) {
        transform(node, BaseTransform.getTranslateInstance(tx, ty));
    }

    static protected void setEffect(NGNode node, Effect effect) {
        node.setEffect(null); // so that when we ask for the getEffectBounds, it won't include an old effect
        BaseBounds effectBounds = effect.getBounds();
        BaseBounds clippedBounds = node.getEffectBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        node.setEffect(effect);
        // The new transformed bounds should be the union of the old effect bounds, new effect bounds, and
        // then transform those bounds. The reason I'm doing it this way is to expose any bugs in the
        // getEffectBounds() implementation when an effect is present.
        effectBounds = effectBounds.deriveWithUnion(clippedBounds);
        node.setTransformedBounds(node.getTransform().transform(effectBounds, effectBounds), false);
    }

    public static abstract class Creator {
        public abstract NGNode create();
    }

    public static abstract class Polluter {
        protected BaseTransform tx = BaseTransform.IDENTITY_TRANSFORM;
        protected abstract void pollute(NGNode node);
        protected BaseBounds modifiedBounds(NGNode node) {
            return DirtyRegionTestBase.getWhatTransformedBoundsWouldBe(node, tx);
        }
        public RectBounds polluteAndGetExpectedBounds(NGNode node) {
            BaseBounds originalBounds = node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
            BaseBounds modifiedBounds = modifiedBounds(node);
            BaseBounds expected = originalBounds.deriveWithUnion(modifiedBounds);
            pollute(node);
            return (RectBounds)expected;
        }
    }
}
