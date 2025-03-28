/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.scenario.effect.DropShadow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A series of tests where we are checking for the dirty region on a grid
 * of nodes. This is a parameterized test (based on its base class).
 * Each time the test is run, it will be given a way to create nodes,
 * and a way to make them dirty. Each test then only has to invoke the
 * creator to make nodes and the polluter to make them dirty, and then
 * check that the right methods were called and the right dirty regions
 * computed. There is some magic here -- every node created by the creator
 * must implement TestNGNode; the base class must set the "root" in the
 * parent class.
 */
public class GridDirtyRegionTest extends DirtyRegionTestBase {
    /**
     * Specified to avoid magic numbers. There are several places where we
     * translate the root in order to make it dirty.
     */
    private static final float TRANSLATE_DELTA = 50;

    /**
     * Constructs a non-overlapping grid of nodes. Each node is a direct child
     * of the root node. They may end up overlapping when they become dirty,
     * but they don't start out that way! Each node is placed where it belongs
     * in the grid by translating them into place.
     *
     * NOTE: This was a parametrized test initializer with @BeforeEach flag, but
     * JUnit5 does not support parametrized classes yet. Make this a @BeforeEach
     * method once it does.
     */
    @Override
    protected void setUp(Creator creator, Polluter polluter) {
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

    @ParameterizedTest
    @MethodSource("createParameters")
    public void sanityCheck(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode node = root.getChildren().get(0);
        assertEquals(new RectBounds(0, 0, 100, 100), node.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        assertEquals(new RectBounds(0, 0, 100, 100), node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));

        node = root.getChildren().get(1);
        assertEquals(new RectBounds(0, 0, 100, 100), node.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        assertEquals(new RectBounds(110, 0, 210, 100), node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));

        node = root.getChildren().get(3);
        assertEquals(new RectBounds(0, 0, 100, 100), node.getContentBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
        assertEquals(new RectBounds(0, 110, 100, 210), node.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void cleanNodesShouldNotContributeToDirtyRegion(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        // By default the scene should be clean
        assertDirtyRegionEquals(root, new RectBounds());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void cleanChildNodesOnADirtyParentShouldNotContributeToDirtyRegion(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        // Now if I translate the root, none of the child nodes
        // should contribute to the dirty region
        translate(root, TRANSLATE_DELTA, TRANSLATE_DELTA);
        for (NGNode child : root.getChildren()) {
            assertDirtyRegionEquals(child, new RectBounds());
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void whenOnlyTheRootIsDirtyOnlyTheRootShouldBeAskedToAccumulateDirtyRegions(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        translate(root, TRANSLATE_DELTA, TRANSLATE_DELTA);
        assertOnlyTheseNodesAreAskedToAccumulateDirtyRegions(root);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void cleanChildNodesOnACleanParentShouldNotContributeToDirtyRegion(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        // If I make one of the children dirty, then the child should contribute
        // to the dirty region, but none of the other nodes should. This test just
        // checks this second part -- that none of the other child nodes contribute.
        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        polluter.pollute(middleChild);
        for (NGNode child : root.getChildren()) {
            if (child != middleChild) { // skip the dirty node
                assertDirtyRegionEquals(child, new RectBounds());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void whenOnlyASingleChildIsDirtyThenParentAndAllChildrenAreAskedToAccumulateDirtyRegions(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        polluter.pollute(middleChild);
        List<NGNode> nodes = new ArrayList<>(root.getChildren());
        nodes.add(root);
        NGNode[] arr = new NGNode[nodes.size()];
        for (int i=0; i<nodes.size(); i++) arr[i] = nodes.get(i);
        // The middle child should be changed
        assertOnlyTheseNodesAreAskedToAccumulateDirtyRegions(arr);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void whenOnlyASingleChildIsDirtyThenOnlyParentAndThatChildShouldComputeDirtyRegions(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        polluter.pollute(middleChild);
        /*
            Testing discovered a really great thing that happens -- if the dirty node is
            a group, but it has no dirty child nodes, then the group itself will not end
            up with accumulateGroupDirtyRegion getting called -- only the node variant.
            Which makes perfect sense and is something of an optimization.
         */
        assertOnlyTheseNodesAreAskedToComputeDirtyRegions(root, middleChild);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void aDirtyChildNodeShouldFormTheDirtyRegionWhenItIsTheOnlyDirtyNode(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        assertDirtyRegionEquals(root, polluter.polluteAndGetExpectedBounds(middleChild));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void theUnionOfTwoDirtyChildNodesDirtyRegionsShouldFormTheDirtyRegionWhenTheyAreTheOnlyDirtyNodes(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode firstChild = root.getChildren().get(0);
        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        RectBounds firstChildArea = polluter.polluteAndGetExpectedBounds(firstChild);
        RectBounds middleChildArea = polluter.polluteAndGetExpectedBounds(middleChild);
        RectBounds expected = (RectBounds)firstChildArea.deriveWithUnion(middleChildArea);
        assertDirtyRegionEquals(root, expected);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void whenTheParentIsDirtyAndSomeChildrenAreDirtyTheParentBoundsShouldFormTheDirtyRegion(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        BaseBounds original = root.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        translate(root, TRANSLATE_DELTA, TRANSLATE_DELTA);
        BaseBounds transformed = root.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        polluter.pollute(root.getChildren().get(0));
        polluter.pollute(root.getChildren().get(root.getChildren().size()/2));
        polluter.pollute(root.getChildren().get(root.getChildren().size()-1));
        RectBounds expected = (RectBounds)original.deriveWithUnion(transformed);
        assertDirtyRegionEquals(root, expected);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void anEffectShouldChangeTheTransformedBoundsOfAChild(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        BaseBounds oldTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        DropShadow shadow = new DropShadow();
        shadow.setGaussianWidth(21);
        shadow.setGaussianHeight(21);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        setEffect(middleChild, shadow);
        BaseBounds newTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        assertFalse(newTransformedBounds.equals(oldTransformedBounds));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void whenAnEffectIsSetTheChildBecomesDirtyAndTheDirtyRegionIncludesTheEffectBounds(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        DropShadow shadow = new DropShadow();
        shadow.setGaussianWidth(21);
        shadow.setGaussianHeight(21);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        BaseBounds oldTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        setEffect(middleChild, shadow);
        BaseBounds newTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        RectBounds expected = (RectBounds)oldTransformedBounds.deriveWithUnion(newTransformedBounds);
        assertDirtyRegionEquals(root, expected);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void whenAnEffectIsChangedOnTheChildTheDirtyRegionIncludesTheOldAndNewEffectBounds(Creator creator, Polluter polluter) {
        setUp(creator, polluter); // NOTE: JUnit5 does not (yet) support parametrized classes. Revert those changes once it does.

        NGNode middleChild = root.getChildren().get(root.getChildren().size()/2);
        DropShadow shadow = new DropShadow();
        shadow.setGaussianWidth(21);
        shadow.setGaussianHeight(21);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        BaseBounds oldTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        setEffect(middleChild, shadow);
        BaseBounds newTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        shadow.setOffsetX(20);
        shadow.setOffsetY(20);
        BaseBounds evenNewerTransformedBounds = middleChild.getCompleteBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        RectBounds expected = (RectBounds)oldTransformedBounds.deriveWithUnion(newTransformedBounds).deriveWithUnion(evenNewerTransformedBounds);
        assertDirtyRegionEquals(root, expected);
    }

    // TODO be sure to test changing properties on the node clip. For example, use a rect clip
    // and change its geometry (JDK-8091760)


    // TODO be sure to write a number of tests regarding the screen clip, and make sure that
    // I test that accumulating dirty regions is correct in the presence of a clip. (JDK-8091760)
}
