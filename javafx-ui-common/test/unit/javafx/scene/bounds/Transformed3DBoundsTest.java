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
package javafx.scene.bounds;

import static com.sun.javafx.test.TestHelper.assertSimilar;
import static com.sun.javafx.test.TestHelper.box;
import static org.junit.Assert.assertEquals;
import javafx.geometry.BoundingBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import org.junit.Test;

public class Transformed3DBoundsTest {

    /***************************************************************************
     * * Transform Tests * * These are tests related to transforming the basic
     * node types including * Groups and Regions. It is assumed that the
     * getBoundsInLocal() values * for these nodes are correct. * * /
     **************************************************************************/

    public @Test
    void test3DBoundsForTranslatedRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setTranslateX(10);
        rect.setTranslateY(10);
        rect.setTranslateZ(10);

        BoundingBox bb = box(0, 0, 0, 100, 100, 0);
        BoundingBox tbb = box(10, 10, 10, 100, 100, 0);

        assertEquals(bb, rect.getBoundsInLocal());
        assertEquals(tbb, rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());

        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.parentToLocal(
                              rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb, rect.sceneToLocal(
                              rect.localToScene(rect.getBoundsInLocal())));

    }


    // this test specifically checks to make sure the minX,, width, height
    // invariants are correct in this case
    public @Test
    void test3DBoundsForNegativeScaledRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setScaleX(2);
        rect.setScaleY(2);
        rect.setScaleZ(2);

        BoundingBox bb = box(0, 0, 0, 100, 100, 0);
        BoundingBox tbb = box(-50, -50, 200, 200);

        assertEquals(bb, rect.getBoundsInLocal());
        assertEquals(tbb, rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());

        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.parentToLocal(
                              rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb, rect.sceneToLocal(
                              rect.localToScene(rect.getBoundsInLocal())));
    }

    public @Test
    void testBoundsForXRotatedRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setRotate(90);
        rect.setRotationAxis(Rotate.X_AXIS);

        BoundingBox bb = box(0, 0, 0, 100, 100, 0);
        BoundingBox tbb = box(0, 50, -50, 100, 0, 100);

        assertEquals(bb, rect.getBoundsInLocal());
        assertSimilar(tbb, rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());

        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.parentToLocal(
                              rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb, rect.sceneToLocal(
                              rect.localToScene(rect.getBoundsInLocal())));
    }

    public @Test
    void testBoundsForYRotatedRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setRotate(90);
        rect.setRotationAxis(Rotate.Y_AXIS);

        BoundingBox bb = box(0, 0, 0, 100, 100, 0);
        BoundingBox tbb = box(50, 0, -50, 0, 100, 100);

        assertEquals(bb, rect.getBoundsInLocal());
        assertSimilar(tbb, rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());

        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.parentToLocal(
                              rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb, rect.sceneToLocal(
                              rect.localToScene(rect.getBoundsInLocal())));
    }

    public @Test
    void testBoundsForZRotatedRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setRotate(90);
        rect.setRotationAxis(Rotate.Z_AXIS);

        BoundingBox bb = box(0, 0, 0, 100, 100, 0);
        BoundingBox tbb = box(0, 0, 0, 100, 100, 0);

        assertEquals(bb, rect.getBoundsInLocal());
        assertSimilar(tbb, rect.getBoundsInParent());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());

        assertSimilar(tbb, rect.localToParent(rect.getBoundsInLocal()));
        assertSimilar(tbb, rect.localToScene(rect.getBoundsInLocal()));
        assertSimilar(bb, rect.parentToLocal(
                              rect.localToParent(rect.getBoundsInLocal())));
        assertSimilar(bb, rect.sceneToLocal(
                              rect.localToScene(rect.getBoundsInLocal())));
    }

    //TODO: Need to add more 3D Bounds testing here ...
}
