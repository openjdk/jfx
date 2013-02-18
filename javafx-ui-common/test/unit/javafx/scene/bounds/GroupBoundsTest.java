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

package javafx.scene.bounds;

import static com.sun.javafx.test.TestHelper.assertBoundsEqual;
import static com.sun.javafx.test.TestHelper.assertGroupBounds;
import static com.sun.javafx.test.TestHelper.box;
import static com.sun.javafx.test.TestHelper.formatBounds;
import static junit.framework.Assert.assertEquals;

import java.util.LinkedList;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

public class GroupBoundsTest {

    /***************************************************************************
     * * Group Tests * * These are tests related to transforming the basic node
     * types including * * /
     **************************************************************************/

    // test that an empty group has empty bounds
    public @Test
    void testGroupBounds_Empty() {
        assertGroupBounds(new Group());
    }

    // test that adding a node to a group updates the Groups bounds
    public @Test
    void testGroupBounds_AddRect() {
        Group g = new Group();
        assertGroupBounds(g);

        g.getChildren().add(new Rectangle(20, 20, 50, 50));
        assertGroupBounds(g);

        g.getChildren().add(new Rectangle(90, 20, 50, 50));
        assertGroupBounds(g);
    }

    // test that changing a node in a group updates the groups bounds
    public @Test
    void testGroupBounds_ChangeRect() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);

        assertGroupBounds(g);

        r1.setWidth(200);
        assertGroupBounds(g);
        r2.setWidth(20);
        assertGroupBounds(g);
    }

    // test that removing a node from the group updates the Groups bounds
    public @Test
    void testGroupBounds_RemoveRect() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);

        assertGroupBounds(g);

        g.getChildren().remove(r2);
        assertGroupBounds(g);
        g.getChildren().remove(r1);
        assertGroupBounds(g);
    }

    public @Test
    void testGroupBounds_RemoveAllChildNodesAndAddOneBack() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);

        assertGroupBounds(g);
        g.getChildren().clear();
        assertGroupBounds(g);
        g.getChildren().add(r1);
        assertGroupBounds(g);
    }

    // test that adding the first node to a Group, which node is an invisible
    // node, gets the right answer for the Group's bounds.
    public @Test
    void testGroupBounds_InitialNodeInvisible() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        r1.setVisible(false);
        Group g = new Group(r1);

        assertGroupBounds(g);
    }

    // test that adding invisible nodes to a group doesn't update the bounds
    public @Test
    void testGroupBounds_InvisibleNodes() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);
        assertGroupBounds(g);

        Rectangle invisible = new Rectangle(-100, -100, 1, 1);
        invisible.setVisible(true);
        g.getChildren().add(invisible);
        assertGroupBounds(g);
    }

    // test that adding zero sized nodes does update a Groups bounds
    public @Test
    void testGroupBounds_ZeroSizedNodes() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);
        assertGroupBounds(g);

        Rectangle small = new Rectangle(-100, -100, 0, 0);
        g.getChildren().add(small);
        assertGroupBounds(g);
    }

    // test that making a node invisible updates the groups bounds
    public @Test
    void testGroupBounds_NodeMadeInvisible() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);
        assertGroupBounds(g);

        r1.setVisible(false);
        assertGroupBounds(g);
    }

    public @Test
    void testGroupBounds_NodeWithNoWidthMadeInvisible() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 0, 50);
        Group g = new Group(r1, r2);
        assertGroupBounds(g);

        r2.setVisible(false);
        assertGroupBounds(g);
    }

    public @Test
    void testGroupBounds_FirstNodeHasNoWidthSecondNodeDoes() {
        Rectangle r1 = new Rectangle(20, 20, 0, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group(r1, r2);
        assertGroupBounds(g);
    }

    public @Test
    void testGroupBounds_FirstNodeHasNoWidthSecondNodeDoes_Added() {
        Rectangle r1 = new Rectangle(20, 20, 0, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group g = new Group();
        assertGroupBounds(g);
        g.getChildren().add(r1);
        assertGroupBounds(g);
        g.getChildren().add(r2);
        assertGroupBounds(g);
    }

    public @Test
    void testGroupBounds_FirstNodeHasNoWidthSecondNodeDoes_ClearedThenAdded() {
        Rectangle r1 = new Rectangle(20, 20, 0, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Rectangle r3 = new Rectangle(10, 15, 75, 20);
        Group g = new Group(r1, r2);
        assertGroupBounds(g);
        g.getChildren().remove(r2);
        assertGroupBounds(g);
        g.getChildren().add(r3);
        assertGroupBounds(g);
    }

    // this test uncovered a bug when using the GroupBoundsHelper. It needs
    // two invisible nodes to tickle it You also have to ask assert the bounds
    // after each change or it may not fail
    public @Test
    void testGroupBounds_TwoNodesMadeInvisible_ThenNodeAdded() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Rectangle r3 = new Rectangle(10, 15, 75, 20);
        Group g = new Group();

        assertGroupBounds(g);
        g.getChildren().add(r1);
        assertGroupBounds(g);
        r1.setVisible(false);
        assertGroupBounds(g);
        g.getChildren().add(r2);
        assertGroupBounds(g);
        r2.setVisible(false);
        assertGroupBounds(g);
        g.getChildren().add(r3);
        assertGroupBounds(g);
    }

    public @Test
    void testGroupBounds_TogglingVisiblityToFalseAndTrueWhileExpandingBounds() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Rectangle r3 = new Rectangle(10, 15, 75, 20);
        Group g = new Group();
        assertGroupBounds(g);
        g.getChildren().add(r1);
        assertGroupBounds(g);
        g.getChildren().add(r2);
        assertGroupBounds(g);
        r2.setVisible(false);
        assertGroupBounds(g);
        g.getChildren().add(r3);
        assertGroupBounds(g);
        r2.setVisible(true);
        assertGroupBounds(g);
    }
    
    private Group createGroupWithRects() {
        Group group = new Group();
        group.setId("group");
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                Rectangle r = new Rectangle(j * 100, i * 100, 50, 50);
                r.setId("rect[" + i + "," + j + "]");
                group.getChildren().add(r);
            }
        }
        
        // sanity test check that the size of the group is correct
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());

        return group;
    }

    // Started out as a simple test and now pretty well covers all the use
    // cases. Oops. Bad Richard.
    public @Test
    void testMovingNodesTowardCenterOfGroupAndOther() {
        Group group = createGroupWithRects();

        // move the 1, 3, 5, 7 nodes in towards the center. Doing so should not
        // change the group's bounds
        ((Rectangle) group.getChildren().get(1)).setX(200);
        ((Rectangle) group.getChildren().get(1)).setY(200);
        ((Rectangle) group.getChildren().get(3)).setX(200);
        ((Rectangle) group.getChildren().get(3)).setY(200);
        ((Rectangle) group.getChildren().get(5)).setX(200);
        ((Rectangle) group.getChildren().get(5)).setY(200);
        ((Rectangle) group.getChildren().get(7)).setX(200);
        ((Rectangle) group.getChildren().get(7)).setY(200);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());

        // move the 2 and 6 nodes in towards the center. Doing so should not
        // change the group's bounds
        ((Rectangle) group.getChildren().get(2)).setX(200);
        ((Rectangle) group.getChildren().get(2)).setY(200);
        ((Rectangle) group.getChildren().get(6)).setX(200);
        ((Rectangle) group.getChildren().get(6)).setY(200);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());

        // move the 0 node into the center. Doing so SHOULD change the bounds
        // such that all nodes are in the center (200, 200) except for the
        // number 8 node which is at (300, 300)
        ((Rectangle) group.getChildren().get(0)).setX(200);
        ((Rectangle) group.getChildren().get(0)).setY(200);
        assertEquals(box(200, 200, 150, 150), group.getBoundsInLocal());

        // move the 8 node a little towards the center (but not all the way)
        // just to test that moving the bottom-right will adjust things
        ((Rectangle) group.getChildren().get(8)).setX(250);
        ((Rectangle) group.getChildren().get(8)).setY(250);
        assertEquals(box(200, 200, 100, 100), group.getBoundsInLocal());

        // move the rest of the nodes down past #8
        for (int i = 0; i <= 7; i++) {
            ((Rectangle) group.getChildren().get(i)).setX(300);
            ((Rectangle) group.getChildren().get(i)).setY(300);
        }
        assertEquals(box(250, 250, 100, 100), group.getBoundsInLocal());

        // now move #8 onto the rest
        ((Rectangle) group.getChildren().get(8)).setX(300);
        ((Rectangle) group.getChildren().get(8)).setY(300);
        assertEquals(box(300, 300, 50, 50), group.getBoundsInLocal());
    }

    public @Test
    void testMovingNodesAwayFromCenterOfGroup() {
        Group group = createGroupWithRects();

        ((Rectangle) group.getChildren().get(4)).setX(600);
        ((Rectangle) group.getChildren().get(4)).setY(600);
        assertEquals(box(100, 100, 550, 550), group.getBoundsInLocal());
    }

    public @Test
    void testMovingNodesWithinInteriorOfGroup() {
        Group group = createGroupWithRects();

        ((Rectangle) group.getChildren().get(4)).setX(250);
        ((Rectangle) group.getChildren().get(4)).setY(250);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
    }

    public @Test
    void testMovingExteriorNodesOnExterior() {
        Group group = createGroupWithRects();

        ((Rectangle) group.getChildren().get(0)).setY(150);
        ((Rectangle) group.getChildren().get(1)).setX(100);
        ((Rectangle) group.getChildren().get(2)).setX(150);
        ((Rectangle) group.getChildren().get(3)).setY(120);
        ((Rectangle) group.getChildren().get(5)).setY(100);
        ((Rectangle) group.getChildren().get(6)).setX(250);
        ((Rectangle) group.getChildren().get(7)).setX(120);
        ((Rectangle) group.getChildren().get(8)).setY(200);

        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
    }

    public @Test
    void testRemovingInteriorNodes() {
        Group group = createGroupWithRects();

        group.getChildren().remove(4);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
    }

    public @Test
    void testRemovingExteriorNodes() {
        Group group = createGroupWithRects();

        group.getChildren().remove(7);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
        group.getChildren().remove(6);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
        group.getChildren().remove(5);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
        group.getChildren().remove(3);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
        group.getChildren().remove(2);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());
        group.getChildren().remove(1);
        assertEquals(box(100, 100, 250, 250), group.getBoundsInLocal());

        // and now the bounds should be changing
        group.getChildren().remove(0);
        assertEquals(box(200, 200, 150, 150), group.getBoundsInLocal());
        group.getChildren().remove(1);
        assertEquals(box(200, 200, 50, 50), group.getBoundsInLocal());
    }

    // run a set of tests that add 1000's of random sized and random positioned
    // rectangles to the Group, and check the groups bounds at each stage.
    // Also randomly make some invisible, some visible, remove some, toggle
    // visibility, and so forth.
    public @Test
    void testGroupBounds_Stress() {
        boolean fullTest = Boolean.getBoolean("test.everything");
        if (fullTest) {
            Group g = new Group();
            assertGroupBounds(g);
            for (int j = 0; j < 500; j++) {
                LinkedList<String> whatHappenedStack = new java.util.LinkedList<String>();
                for (int i = 0; i <= 50; i++) {
                    Bounds layoutBounds = g.getLayoutBounds();
                    int numChildren = g.getChildren().size();
                    String whatHappened = "";
                    double rnd = Math.random();
                    int index = (int) (Math.random() * numChildren);
                    if (rnd > .3 && rnd < .5 && numChildren > 0) {
                        // translate some rect
                        int direction = (Math.random() > .5) ? -1 : 1;
                        Bounds oldBoundsInParent = g.getChildren().get(index)
                                .getBoundsInParent();
                        g.getChildren()
                                .get(index)
                                .setTranslateX(
                                        (int) (Math.random() * 500 * direction));
                        g.getChildren()
                                .get(index)
                                .setTranslateY(
                                        (int) (Math.random() * 500 * direction));
                        whatHappened = "On iteration " + j + "-"+ i + "I translated a "
                                + g.getChildren().get(index) + " with boundsInParent "
                                + formatBounds(oldBoundsInParent) + " which was the "
                                + index+1 + "th item. Its new bounds are "
                                + formatBounds(g.getChildren().get(index).getBoundsInParent()) + ". "
                                + "The layoutBounds of the Group was "
                                + formatBounds(layoutBounds) + " and is now " + formatBounds(g.getLayoutBounds()) + " and had "
                                + numChildren + " number of child nodes";
                        whatHappenedStack.add(whatHappened);
                    } else if (rnd > .5 && rnd < .6 && numChildren > 0) {
                        // deletesome rect
                        whatHappened = "On iteration " + j + "-" + i+ " I deleted a "
                                + "" + g.getChildren().get(index) + " with boundsInParent "
                                + "" + formatBounds(g.getChildren().get(index).getBoundsInParent())+ " which was the "
                                + "" + index+1+ "th item. The layoutBounds of the Group was "
                                + "" + formatBounds(layoutBounds)+ " and is now " + formatBounds(g.getLayoutBounds())+ " and had "
                                + "" + numChildren+ " number of child nodes";
                        g.getChildren().remove(index);
                        whatHappenedStack.add(whatHappened);
                    } else if (rnd > .6 && rnd < .65 && numChildren > 0) {
                        // toggle visibility on some rect
                        g.getChildren().get(index)
                                .setVisible(!g.getChildren().get(index).isVisible());
                        whatHappened = "On iteration " + j+ "-" + i+ " I toggled the "
                                + "visibility of " + g.getChildren().get(index) + " with boundsInParent "
                                + "" + formatBounds(g.getChildren().get(index).getBoundsInParent())+ " to "
                                + "" + g.getChildren().get(index).isVisible() + ". The layoutBounds of the "
                                + "Group was " + formatBounds(layoutBounds)+ " and is now " + formatBounds(g.getLayoutBounds())+ " and "
                                + "had " + numChildren+ " number of child nodes";
                        whatHappenedStack.add(whatHappened);
                    } else {
                        Rectangle rect = new Rectangle(
                                ((int) (Math.random() * 1000)),
                                ((int) (Math.random() * 1000)),
                                ((int) (Math.random() * 100)),
                                ((int) (Math.random() * 100)));
                        g.getChildren().add(rect);
                        whatHappened = "On iteration " + j+ "-" + i+ " I added a " + rect+ " with "
                                + "boundsInParent " + formatBounds(rect.getBoundsInParent())+ " to the group. The "
                                + "layoutBounds of the Group was " + formatBounds(layoutBounds)+ " and is now "
                                + "" + formatBounds(g.getLayoutBounds())+ " and had " + numChildren+ " number of "
                                + "child nodes";
                        whatHappenedStack.add(whatHappened);
                    }
                    try {
                        assertGroupBounds(g);
                    } catch (Exception any) {
                        System.out.println("Something went wrong. Here's what happened:");
                        for (String item : whatHappenedStack) {
                            System.out.println("{item}");
                        }
                        throw new RuntimeException(any);
                    }
                }
            }
        }
    }

    // here is a special test for impl_getPivotX and impl_getPivotY
    @Test
    public void testPivotXAndPivotY() {
        Rectangle rect = new Rectangle(100, 100);
        assertEquals(50.0f, (float) rect.impl_getPivotX());
        assertEquals(50.0f, (float) rect.impl_getPivotY());
        rect.setWidth(70.0f);
        assertEquals(35.0f, (float) rect.impl_getPivotX());
        assertEquals(50.0f, (float) rect.impl_getPivotY());
    }

    /***************************************************************************
     * Group Bounds Sanity Tests * * These tests are for Group
     * to make sure that their * bounds are reported correctly
     * before effects or clips or transforms * are taken into account. * * The
     * group tests here are just basic tests, a full set of Group bounds * tests
     * are included in their own section later in this file. There are * many
     * edge cases to group bounds. The primary purpose of these next * few tests
     * are just to make sure that group's bounds are correct in the * simple
     * case. * *
     **************************************************************************/

    public @Test
    void testBoundsForGroup() {
        Rectangle r1 = new Rectangle(20, 20, 50, 50);
        Rectangle r2 = new Rectangle(90, 20, 50, 50);
        Group group = new Group(r1, r2);

        assertEquals(box(20, 20, 120, 50), group.getBoundsInLocal());
        assertEquals(group.getBoundsInLocal(), group.getBoundsInParent());
        assertEquals(group.getBoundsInLocal(), group.getLayoutBounds());
    }
    
    /**
     * Just a basic test to make sure basic group node bounds calculation works
     */
    public @Test
    void testBoundsForGroup_childGeomChanging() {
        Rectangle rect = new Rectangle();
        rect.setId("rect");
        Rectangle square = new Rectangle();
        square.setId("square");
        Group group = new Group(rect, square);
        group.setId("group");

        assertBoundsEqual(box(0, 0, 0, 0), rect.getBoundsInLocal());
        assertBoundsEqual(box(0, 0, 0, 0), square.getBoundsInLocal());
        assertBoundsEqual(box(0, 0, 0, 0), group.getBoundsInLocal());

        rect.setX(50);
        rect.setY(50);
        rect.setWidth(100);
        rect.setHeight(30);

        assertBoundsEqual(box(50, 50, 100, 30), rect.getBoundsInLocal());
        assertBoundsEqual(box(0, 0, 0, 0), square.getBoundsInLocal());
        assertBoundsEqual(box(0, 0, 150, 80), group.getBoundsInLocal());

        square.setX(25);
        square.setY(25);
        square.setWidth(50);
        square.setHeight(50);

        assertBoundsEqual(box(50, 50, 100, 30), rect.getBoundsInLocal());
        assertBoundsEqual(box(25, 25, 50, 50), square.getBoundsInLocal());
        assertBoundsEqual(box(25, 25, 125, 55), group.getBoundsInLocal());
    }

    public @Test
    void testBoundsInParentOfGroup() {
        Rectangle rect = new Rectangle(50, 50, 100, 30);
        rect.setId("rect");
        Group group = new Group(rect);
        group.setId("group");
        group.setTranslateX(100);
        group.setTranslateY(100);

        assertBoundsEqual(box(150, 150, 100, 30), group.getBoundsInParent());
    }

    /**
     * Tests that if I have a group with nobody listening to the bounds, and the
     * bounds change, and then I ask for the new bounds, that the bounds are
     * valid.
     */
    public @Test
    void testRequestingBoundsOnGroupWithNoListenersWorks() {
        Group group = new Group(new Rectangle(50, 50, 100, 30));

        assertBoundsEqual(box(50, 50, 100, 30), group.getBoundsInLocal());
    }

}
