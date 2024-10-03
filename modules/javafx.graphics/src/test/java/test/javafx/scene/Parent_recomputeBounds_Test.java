/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import test.com.sun.javafx.test.TestHelper;
import javafx.geometry.BoundingBox;
import javafx.scene.transform.Rotate;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.NodeShim;
import javafx.scene.ParentShim;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Translate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Parent_recomputeBounds_Test {
    private static final Rectangle r1 = new Rectangle(100, 100, 100, 100);
    private static final Rectangle r2 = new Rectangle(250, 250, 50, 50);
    private static final Rectangle r3 = new Rectangle(50, 50, 10, 10);

    @Test
    public void shouldRecomputeBoundsWhenNodeAdded() {
        final Group g = new Group();
        Bounds b;

        b = g.getBoundsInParent();
        assertTrue(b.isEmpty());

        g.getChildren().add(r1);

        b = g.getBoundsInParent();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(100, b.getWidth(), 0.0001);
        assertEquals(100, b.getHeight(), 0.0001);

        g.getChildren().add(r2);

        b = g.getBoundsInParent();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        g.getChildren().add(r3);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldRecomputeBoundsWhenNodeRemoved() {
        final Group g = new Group();
        g.getChildren().add(r1);
        g.getChildren().add(r2);
        g.getChildren().add(r3);
        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        g.getChildren().remove(r3);

        b = g.getBoundsInParent();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        g.getChildren().remove(r2);

        b = g.getBoundsInParent();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(100, b.getWidth(), 0.0001);
        assertEquals(100, b.getHeight(), 0.0001);

        g.getChildren().remove(r1);

        b = g.getBoundsInParent();
        assertTrue(b.isEmpty());
    }

    @Test
    public void shouldRecomputeBoundsWhenNodeHiddenOrShown() {
        final Group g = new Group();
        g.getChildren().add(r1);
        g.getChildren().add(r2);
        g.getChildren().add(r3);
        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r3.setVisible(false);

        b = g.getBoundsInParent();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        r3.setVisible(true);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotRecomputeBoundsWhenHiddenNodeChanges() {
        final Group g = new Group();
        final Rectangle r = new Rectangle(200,200,1,1);
        g.getChildren().add(r2);
        g.getChildren().add(r);
        g.getChildren().add(r3);
        Bounds b;

        r.setVisible(false);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setX(10);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setWidth(500);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotRecomputeBoundsWhenNodeChangesInsideEdges() {
        final Group g = new Group();
        final Rectangle r = new Rectangle(200,200,1,1);
        g.getChildren().add(r2);
        g.getChildren().add(r);
        g.getChildren().add(r3);
        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setX(190);
        r.setY(190);
        r.setWidth(48);
        r.setHeight(48);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotRecomputeBoundsWhenEdgeNodeEnlargesInwards() {
        final Group g = new Group();
        final Rectangle r = new Rectangle(200,200,50,50);
        g.getChildren().add(r);
        g.getChildren().add(r3);
        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        r.setX(100);
        r.setY(100);
        r.setWidth(150);
        r.setHeight(150);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldRecomputeBoundsWhenNodeMoved() {
        final Group g = new Group();
        final Rectangle r = new Rectangle(200,200,1,1);

        g.getChildren().add(r2);
        g.getChildren().add(r);
        g.getChildren().add(r3);
        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setX(40);

        b = g.getBoundsInParent();
        assertEquals(40, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(260, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setX(200);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldRecomputeBoundsWhenNodeResized() {
        final Group g = new Group();
        final Rectangle r = new Rectangle(200,200,1,1);

        g.getChildren().add(r2);
        g.getChildren().add(r);
        g.getChildren().add(r3);
        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setWidth(200);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(350, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);

        r.setWidth(5);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(250, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotRecomputeBoundsWhenManyNodesChange() {
        final Group g = new Group();
        final Rectangle lt = new Rectangle(100, 100, 100, 100);
        final Rectangle rt = new Rectangle(200, 100, 100, 100);
        final Rectangle lb = new Rectangle(100, 200, 100, 100);
        final Rectangle rb = new Rectangle(200, 200, 100, 100);
        final Rectangle rm1 = new Rectangle(150, 150, 50, 50);
        final Rectangle rm2 = new Rectangle(150, 150, 50, 50);
        Bounds b;

        g.getChildren().add(lt);
        g.getChildren().add(rt);
        g.getChildren().add(lb);
        g.getChildren().add(rb);
        g.getChildren().add(rm1);
        g.getChildren().add(rm2);
        for (int i = 0; i < 20; i++) {
            g.getChildren().add(new Rectangle(150 + i, 150 + i, 50 + i, 50 + i));
        }

        b = g.getBoundsInParent();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        lt.setX(50);
        rt.setX(250);
        lb.setY(250);
        rb.setX(220);
        rm1.setY(50);
        rm2.setX(151);
        rm2.setY(151);

        b = g.getBoundsInParent();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(300, b.getWidth(), 0.0001);
        assertEquals(300, b.getHeight(), 0.0001);
    }

    @Test
    public void transformedBoundsCalculationShouldNotInfluenceUntransformed() {
        final Group g = new Group();
        final Rectangle lt = new Rectangle(100, 100, 100, 100);
        final Rectangle rt = new Rectangle(200, 100, 100, 100);
        final Rectangle lb = new Rectangle(100, 200, 100, 100);
        final Rectangle rb = new Rectangle(200, 200, 100, 100);
        final Rectangle rm1 = new Rectangle(150, 150, 50, 50);
        final Rectangle rm2 = new Rectangle(150, 150, 50, 50);
        Bounds b;

        g.getChildren().addAll(lt, rt, lb, rb, rm1, rm2);
        for (int i = 0; i < 20; i++) {
            g.getChildren().add(new Rectangle(150 + i, 150 + i, 50 + i, 50 + i));
        }

        b = g.getBoundsInLocal();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        g.getTransforms().add(new Rotate(45));

        lt.setX(50);
        rt.setX(250);
        lb.setY(250);
        rb.setX(220);
        rm1.setY(50);
        rm2.setX(151);
        rm2.setY(151);

        // this call gets transformed Parent bounds and shouldn't clear the
        // Parent's dirty children list
        g.getBoundsInParent();

        b = g.getBoundsInLocal();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(300, b.getWidth(), 0.0001);
        assertEquals(300, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotStartBoundsCalculationFromEmptyBounds() {
        final Group g = new Group();
        final Rectangle lt = new Rectangle(50, 50, 50, 50);
        final Rectangle rb = new Rectangle(100, 100, 50, 50);
        Bounds b;

        g.getChildren().addAll(lt, rb);

        b = g.getBoundsInLocal();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(100, b.getWidth(), 0.0001);
        assertEquals(100, b.getHeight(), 0.0001);

        // invalidate (empty) bounds
        rb.setVisible(false);

        // add new rectangle, bounds should not be recalculated by using
        // empty bounds
        g.getChildren().add(new Rectangle(150, 150, 50, 50));

        b = g.getBoundsInLocal();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(50, b.getMinY(), 0.0001);
        assertEquals(150, b.getWidth(), 0.0001);
        assertEquals(150, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotIgnoreMultipleAddedNodes() {
        final Group g = new Group();
        final Rectangle lt = new Rectangle(100, 100, 100, 100);
        final Rectangle mt = new Rectangle(200, 100, 100, 100);
        final Rectangle rt = new Rectangle(300, 100, 100, 100);
        final Rectangle rb = new Rectangle(300, 200, 100, 100);
        Bounds b;

        g.getChildren().addAll(lt, mt);

        b = g.getBoundsInLocal();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(100, b.getHeight(), 0.0001);

        NodeShim.set_boundsChanged(rb, false);
        //((Node) rb).NodeShim.boundsChanged(rb) = false;
        // rt, rb should be either incorporated into parent bounds directly
        // or marked as dirty for parent bounds calculation
        g.getChildren().addAll(rt, rb);

        b = g.getBoundsInLocal();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(300, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);
    }

    @Test
    public void shouldNotCreateEmptyDirtyChildrenList() {
        final Group g = new Group();
        final Rectangle lt = new Rectangle(100, 100, 100, 100);
        final Rectangle rt = new Rectangle(200, 100, 100, 100);
        final Rectangle lb = new Rectangle(100, 200, 100, 100);
        final Rectangle rb = new Rectangle(200, 200, 100, 100);
        Bounds b;

        g.getChildren().addAll(lt, rt, lb, rb);
        final int toAdd = ParentShim.DIRTY_CHILDREN_THRESHOLD - 4;
        for (int i = 0; i < toAdd; ++i) {
            g.getChildren().add(
                    new Rectangle(150 + i * 80 / (toAdd - 1), 190, 20, 20));
        }

        b = g.getBoundsInLocal();
        assertEquals(100, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(200, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);

        lt.setX(50);
        // this should create a dirty children list on Parent, even though
        // the added node doesn't change the group gemetry, the created
        // dirty children list should still contain the previously modified
        // corner node (lt)
        g.getChildren().add(new Rectangle(150, 150, 100, 100));

        b = g.getBoundsInLocal();
        assertEquals(50, b.getMinX(), 0.0001);
        assertEquals(100, b.getMinY(), 0.0001);
        assertEquals(250, b.getWidth(), 0.0001);
        assertEquals(200, b.getHeight(), 0.0001);
    }

    @Test
    public void
            shouldNotSkipGeomChangedForChildAdditionInsideUntransformedBounds()
    {
        final Group g = new Group(new Circle(0, -100, 0.001),
                                  new Circle(0, 100, 0.001),
                                  new Circle(-100, 0, 0.001),
                                  new Circle(100, 0, 0.001));
        g.getTransforms().add(new Rotate(-45));

        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(-100 * Math.sqrt(2) / 2, b.getMinX(), 0.1);
        assertEquals(-100 * Math.sqrt(2) / 2, b.getMinY(), 0.1);
        assertEquals(100 * Math.sqrt(2), b.getWidth(), 0.1);
        assertEquals(100 * Math.sqrt(2), b.getHeight(), 0.1);

        g.getChildren().add(new Circle(95, -95, 0.001));

        b = g.getBoundsInParent();
        assertEquals(-100 * Math.sqrt(2) / 2, b.getMinX(), 0.1);
        assertEquals(-95 * Math.sqrt(2), b.getMinY(), 0.1);
        assertEquals(100 * Math.sqrt(2), b.getWidth(), 0.1);
        assertEquals((50 + 95) * Math.sqrt(2), b.getHeight(), 0.1);
    }

    @Test
    public void
            shouldNotSkipGeomChangedForChildRemovalInsideUntransformedBounds()
    {
        final Circle toRemove = new Circle(95, -95, 0.001);
        final Group g = new Group(toRemove,
                                  new Circle(0, -100, 0.001),
                                  new Circle(0, 100, 0.001),
                                  new Circle(-100, 0, 0.001),
                                  new Circle(100, 0, 0.001));
        g.getTransforms().add(new Rotate(-45));

        Bounds b;

        b = g.getBoundsInParent();
        assertEquals(-100 * Math.sqrt(2) / 2, b.getMinX(), 0.1);
        assertEquals(-95 * Math.sqrt(2), b.getMinY(), 0.1);
        assertEquals(100 * Math.sqrt(2), b.getWidth(), 0.1);
        assertEquals((50 + 95) * Math.sqrt(2), b.getHeight(), 0.1);

        g.getChildren().remove(toRemove);

        b = g.getBoundsInParent();
        assertEquals(-100 * Math.sqrt(2) / 2, b.getMinX(), 0.1);
        assertEquals(-100 * Math.sqrt(2) / 2, b.getMinY(), 0.1);
        assertEquals(100 * Math.sqrt(2), b.getWidth(), 0.1);
        assertEquals(100 * Math.sqrt(2), b.getHeight(), 0.1);
    }

    @Test
    public void nodeShouldNotifyParentEvenIfItsTransformedBoundsAreDirty() {
        final Rectangle child = new Rectangle(0, 0, 100, 100);
        final Group parent = new Group(child);

        // ensures that child's getTransformedBounds will be called with
        // a non-identity transform argument during parent's bounds calculations
        parent.getTransforms().add(new Rotate(-45));

        // make the cached child's transformed bounds dirty
        child.getTransforms().add(new Translate(50, 0));

        // the cached child's transformed bounds will remain dirty because
        // of a non-trivial parent transformation
        TestHelper.assertSimilar(
                boundsOfRotatedRect(50, 0, 100, 100, -45),
                parent.getBoundsInParent());

        // during the following call the child bounds changed notification
        // should still be generated even though the child's cached transformed
        // bounds are already dirty
        child.getTransforms().add(new Translate(50, 0));

        TestHelper.assertSimilar(
                boundsOfRotatedRect(100, 0, 100, 100, -45),
                parent.getBoundsInParent());
    }

    private static Bounds boundsOfRotatedRect(
            final double x, final double y,
            final double width, final double height,
            final double angle) {
        final Point2D p1 = rotatePoint(x, y, angle);
        final Point2D p2 = rotatePoint(x + width, y, angle);
        final Point2D p3 = rotatePoint(x, y + height, angle);
        final Point2D p4 = rotatePoint(x + width, y + height, angle);

        final double minx = min(p1.getX(), p2.getX(), p3.getX(), p4.getX());
        final double miny = min(p1.getY(), p2.getY(), p3.getY(), p4.getY());
        final double maxx = max(p1.getX(), p2.getX(), p3.getX(), p4.getX());
        final double maxy = max(p1.getY(), p2.getY(), p3.getY(), p4.getY());

        return new BoundingBox(minx, miny, maxx - minx, maxy - miny);
    }

    private static Point2D rotatePoint(final double x, final double y,
                                       final double angle) {
        final double rada = Math.toRadians(angle);
        final double sina = Math.sin(rada);
        final double cosa = Math.cos(rada);

        return new Point2D(x * cosa - y * sina,
                           x * sina + y * cosa);
    }

    private static double min(final double... values) {
        double result = values[0];
        for (int i = 1; i < values.length; ++i) {
            if (result > values[i]) {
                result = values[i];
            }
        }

        return result;
    }

    private static double max(final double... values) {
        double result = values[0];
        for (int i = 1; i < values.length; ++i) {
            if (result < values[i]) {
                result = values[i];
            }
        }

        return result;
    }
}
