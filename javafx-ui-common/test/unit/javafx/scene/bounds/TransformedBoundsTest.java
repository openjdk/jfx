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
import static com.sun.javafx.test.TestHelper.assertSimilar;
import static com.sun.javafx.test.TestHelper.box;
import static org.junit.Assert.assertEquals;
import javafx.geometry.BoundingBox;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import org.junit.Test;

public class TransformedBoundsTest {

    /***************************************************************************
     * * Transform Tests * * These are tests related to transforming the basic
     * node types including * Groups and Regions. It is assumed that the
     * getBoundsInLocal() values * for these nodes are correct. * * /
     **************************************************************************/

    public @Test
    void testBoundsForTranslatedRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setTranslateX(10);
        rect.setTranslateY(10);

        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(box(10, 10, 100, 100), rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
    }

    // this test specifically checks to make sure the minX,, width, height
    // invariants are correct in this case
    public @Test
    void testBoundsForNegativeScaledRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setScaleX(-1);
        rect.setScaleY(-1);

        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(box(0, 0, 100, 100), rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
    }

    public @Test
    void testBoundsForRotatedRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setRotate(45);

        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertSimilar(box(-20.71, -20.71, 141.42, 141.42),
                rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
    }

    public @Test
    void testBoundsForRotatedGroupOfRectangles() {
        Group group = new Group(new Rectangle(100, 100), new Rectangle(2, 2,
                96, 96));
        group.setRotate(45);

        assertEquals(box(0, 0, 100, 100), group.getBoundsInLocal());
        assertSimilar(box(-20.71, -20.71, 141.42, 141.42),
                group.getBoundsInParent());
        assertEquals(group.getBoundsInLocal(), group.getLayoutBounds());
    }

    public @Test
    void testBoundsForRotatedRectangleUsingTransforms() {
        Rectangle rect = new Rectangle(100, 100);
        rect.getTransforms().add(new Rotate(45)); // rotates about 0, 0 rather
                                                  // than the center!

        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertSimilar(box(-70.71, 0, 141.42, 141.42), rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
    }

    public @Test
    void testBoundsForTransformedRectangleInTransformedGroup() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setTranslateX(100);
        Group group = new Group(rect);
        group.setTranslateX(50);

        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(box(100, 0, 100, 100), rect.getBoundsInParent());
        assertEquals(box(100, 0, 100, 100), group.getBoundsInLocal());
        assertEquals(box(150, 0, 100, 100), group.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
        assertEquals(rect.getBoundsInParent(), group.getLayoutBounds());
    }

    public @Test
    void testUnTransformedBounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        BoundingBox bb = box(3, 7, 17, 19);

        assertSimilar(bb, rect.getBoundsInLocal());
        assertSimilar(bb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.parentToLocal(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.sceneToLocal(rect.getBoundsInLocal()));
    }

    public @Test
    void testTranslateTxBounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        rect.getTransforms().add(new Translate(20, 30));
        BoundingBox tbb = box(23, 37, 17, 19);

        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(rect.getBoundsInParent(),
                rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
    }

    public @Test
    void testTranslatedBounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        rect.setTranslateX(20);
        rect.setTranslateY(30);
        BoundingBox bb = box(3, 7, 17, 19);
        BoundingBox tbb = box(23, 37, 17, 19);

        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(rect.getBoundsInParent(),
                rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb,
                rect.parentToLocal(rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb,
                rect.sceneToLocal(rect.localToScene(rect.getBoundsInLocal())));
    }

    public @Test
    void testScaleTxBounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        rect.getTransforms().add(new Scale(2, 3));

        BoundingBox bb = box(3, 7, 17, 19);
        BoundingBox tbb = box(6, 21, 34, 57);

        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(rect.getBoundsInParent(),
                rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb,
                rect.parentToLocal(rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb,
                rect.sceneToLocal(rect.localToScene(rect.getBoundsInLocal())));
    }

    public @Test
    void testScaledBounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        rect.setScaleX(2);
        rect.setScaleY(3);

        BoundingBox bb = box(3, 7, 17, 19);
        BoundingBox tbb = box(-5.5, -12, 34, 57);

        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(rect.getBoundsInParent(),
                rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb,
                rect.parentToLocal(rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb,
                rect.sceneToLocal(rect.localToScene(rect.getBoundsInLocal())));
    }

    public @Test
    void testRotateTx11Bounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        BoundingBox bb = box(3, 7, 17, 19);
        rect.getTransforms().add(new Rotate(11));

        BoundingBox tbb = box(-2.0161524, 7.443817, 20.313034, 21.89467);

        assertSimilar(bb, rect.getBoundsInLocal());
        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
    }

    public @Test
    void testRotateTx90Bounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        BoundingBox bb = box(3, 7, 17, 19);
        rect.getTransforms().add(new Rotate(90));

        BoundingBox tbb = box(-26, 3, 19, 17);

        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(rect.getBoundsInParent(),
                rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
    }

    public @Test
    void testRotated90Bounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        BoundingBox bb = box(3, 7, 17, 19);
        rect.setRotate(90);

        BoundingBox tbb = box(2.0, 8.0, 19, 17);

        assertSimilar(bb, rect.getBoundsInLocal());
        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(rect.getBoundsInParent(),
                rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
    }

    public @Test
    void testRotated120Bounds() {
        Rectangle rect = new Rectangle(3, 7, 17, 19);
        BoundingBox bb = box(3, 7, 17, 19);
        rect.setRotate(120);

        BoundingBox tbb = box(-0.97724134, 4.388784, 24.954483, 24.222431);

        assertSimilar(bb, rect.getBoundsInLocal());
        assertSimilar(tbb, rect.getBoundsInParent());
        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
    }

    public @Test
    void testNotificationOnBoundsChangeForLeafNode() {
        Rectangle rect = new Rectangle();

        rect.setX(50);
        assertBoundsEqual(box(50, 0, 0, 0), rect.getBoundsInLocal());

        rect.setY(50);
        rect.setWidth(100);
        rect.setHeight(30);
        assertBoundsEqual(box(50, 50, 100, 30), rect.getBoundsInLocal());
    }

    @Test
    public void testNotificationOnBoundsChangeForTransformedLeafNode() {
        final Rectangle rect = new Rectangle(-50, -50, 100, 100);
        rect.getTransforms().add(new Rotate(-45));

        assertSimilar(
                box(-Math.sqrt(2) * 50,
                    -Math.sqrt(2) * 50,
                    Math.sqrt(2) * 100,
                    Math.sqrt(2) * 100),
                rect.getBoundsInParent());

        rect.setX(-100);
        rect.setY(-100);
        rect.setWidth(200);
        rect.setHeight(200);

        assertSimilar(
                box(-Math.sqrt(2) * 100,
                    -Math.sqrt(2) * 100,
                    Math.sqrt(2) * 200,
                    Math.sqrt(2) * 200),
                rect.getBoundsInParent());
    }

    public @Test
    void testBoundsWithTransform() {
        Rectangle rect = new Rectangle();
        rect.setScaleX(0.5f);
        rect.setScaleY(0.5f);

        assertBoundsEqual(box(0, 0, 0, 0), rect.getBoundsInLocal());
        assertBoundsEqual(box(0, 0, 0, 0), rect.getBoundsInParent());

        rect.setX(50);
        rect.setY(50);
        rect.setWidth(100);
        rect.setHeight(30);

        assertBoundsEqual(box(50, 50, 100, 30), rect.getBoundsInLocal());
        assertBoundsEqual(box(75, 57.5, 50, 15), rect.getBoundsInParent());
    }

    public @Test
    void testNotificationOnBoundsChangeForTransforms() {
        Rectangle rect = new Rectangle();
        rect.setScaleX(0.5f);
        rect.setScaleY(0.5f);

        // changes to either the rectangle OR the scale should cause events
        // to fire

        rect.setX(50);
        rect.setY(50);
        rect.setWidth(100);
        rect.setHeight(30);
        assertBoundsEqual(box(75, 57.5, 50, 15), rect.getBoundsInParent());

        rect.setScaleX(1);
        rect.setScaleY(1);
        assertBoundsEqual(box(50, 50, 100, 30), rect.getBoundsInParent());
    }

}
