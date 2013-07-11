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

package com.sun.javafx.sg;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A base class for testing the content bounds methods of BaseNode used
 * by all of the FX toolkits.
 */
public class ContentBoundsTest {
    public static final BaseTransform IDENTITY;
    public static final BaseTransform TRANSLATE;
    public static final BaseTransform SCALE;
    public static final BaseTransform ROTATE;
    public static final BaseTransform SCALE_TRANSLATE;
    public static final BaseTransform TRANSLATE_SCALE;

    public static BaseTransform translate(BaseTransform transform,
                                          double tx, double ty)
    {
        transform = BaseTransform.getInstance(transform);
        return transform.deriveWithConcatenation(1, 0, 0, 1, tx, ty);
    }

    public static BaseTransform scale(BaseTransform transform,
                                      double sx, double sy)
    {
        transform = BaseTransform.getInstance(transform);
        return transform.deriveWithConcatenation(sx, 0, 0, sy, 0, 0);
    }

    public static BaseTransform rotate(BaseTransform transform,
                                       double degrees)
    {
        Affine2D t2d = new Affine2D(transform);
        t2d.rotate(Math.toRadians(degrees));
        return t2d;
    }

    static {
        IDENTITY = BaseTransform.IDENTITY_TRANSFORM;
        TRANSLATE = translate(IDENTITY, 42.3, 16.5);
        SCALE = scale(IDENTITY, 0.7, 0.6);
        ROTATE = rotate(IDENTITY, 135);
        TRANSLATE_SCALE = scale(TRANSLATE, 0.8, 0.9);
        SCALE_TRANSLATE = translate(SCALE, 23.7, 83.5);
    }

    public static Node translate(double tx, double ty, Node n) {
        n.setTranslateX(tx);
        n.setTranslateY(ty);
        return n;
    }

    public static Node scale(double sx, double sy, Node n) {
        n.setScaleX(sx);
        n.setScaleY(sy);
        return n;
    }

    public static Node rotate(double rot, Node n) {
        n.setRotate(rot);
        return n;
    }

    public static Node group(Node... n) {
        Group g = new Group(n);
        return g;
    }

    public static Node makeRectangle(double x, double y, double w, double h) {
        return new Rectangle(x, y, w, h);
    }

    public static NGNode getValidatedPGNode(Node n) {
        if (n instanceof Parent) {
            for (Node child : ((Parent) n).getChildrenUnmodifiable()) {
                getValidatedPGNode(child);
            }
        }
        NGNode pgn = n.impl_getPeer();
        // Eeek, this is gross! I have to use reflection to invoke this
        // method so that bounds are updated...
        try {
            java.lang.reflect.Method method = Node.class.getDeclaredMethod("updateBounds");
            method.setAccessible(true);
            method.invoke(n);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update bounds", e);
        }
        n.impl_updatePeer();
        return pgn;
    }

    public static BaseBounds getBounds(Node n, BaseTransform tx) {
        Scene.impl_setAllowPGAccess(true);
        NGNode pgn = getValidatedPGNode(n);
        Scene.impl_setAllowPGAccess(false);
        return pgn.getContentBounds(new RectBounds(), tx);
    }

    public static class TestPoint {
        private float x;
        private float y;
        private boolean contains;

        public TestPoint(float x, float y, boolean contains) {
            this.x = x;
            this.y = y;
            this.contains = contains;
        }

        public boolean isContains() {
            return contains;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    public static void checkContentPoint(Node n, TestPoint tp,
                                         BaseTransform transform)
    {
        BaseBounds bounds = getBounds(n, transform);
        float c[] = new float[] {tp.getX(), tp.getY()};
        transform.transform(c, 0, c, 0, 1);
        boolean success = false;
        try {
            assertEquals(bounds.contains(c[0], c[1]), tp.isContains());
            success = true;
        } finally {
            if (!success) {
                System.err.println("Failed on bounds = "+bounds);
                System.err.println("with transform = "+transform);
                System.err.println("with  x,  y = "+tp.getX()+", "+tp.getY());
                System.err.println("and  tx, ty = "+c[0]+", "+c[1]);
            }
        }
    }

    // When a chain of transforms is involved, it can help to back off
    // slightly from the edges of a shape using this tiny constant
    // to avoid failing a test due to floating point rounding error.
    static final float EPSILON = 1e-6f;

    public TestPoint[] rectPoints(float x, float y, float w, float h) {
        return new TestPoint[] {
            new TestPoint(x  +EPSILON, y  +EPSILON, true),
            new TestPoint(x+w-EPSILON, y  +EPSILON, true),
            new TestPoint(x  +EPSILON, y+h-EPSILON, true),
            new TestPoint(x+w-EPSILON, y+h-EPSILON, true),
            new TestPoint(x+w, y+h+h, false)
        };
    }

    public BaseBounds getBounds(TestPoint... testpts) {
        RectBounds rb = new RectBounds();
        for (TestPoint tp : testpts) {
            if (tp.isContains()) {
                rb.add(tp.getX(), tp.getY());
            }
        }
        return rb;
    }

    public TestPoint[] translate(float tx, float ty, TestPoint... testpts) {
        TestPoint ret[] = new TestPoint[testpts.length];
        for (int i = 0; i < testpts.length; i++) {
            TestPoint tp = testpts[i];
            ret[i] = new TestPoint(tp.getX() + tx, tp.getY() + ty,
                                   tp.isContains());
        }
        return ret;
    }

    public TestPoint[] scale(float sx, float sy, TestPoint... testpts) {
        BaseBounds bounds = getBounds(testpts);
        float cx = (bounds.getMinX() + bounds.getMaxX()) / 2.0f;
        float cy = (bounds.getMinY() + bounds.getMaxY()) / 2.0f;
        TestPoint ret[] = new TestPoint[testpts.length];
        for (int i = 0; i < testpts.length; i++) {
            TestPoint tp = testpts[i];
            ret[i] = new TestPoint((tp.getX() - cx) * sx + cx,
                                   (tp.getY() - cy) * sy + cy,
                                   tp.isContains());
        }
        return ret;
    }

    public TestPoint[] rotate(double degrees, TestPoint... testpts) {
        BaseBounds bounds = getBounds(testpts);
        float cx = (bounds.getMinX() + bounds.getMaxX()) / 2.0f;
        float cy = (bounds.getMinY() + bounds.getMaxY()) / 2.0f;
        TestPoint ret[] = new TestPoint[testpts.length];
        double radians = Math.toRadians(degrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (int i = 0; i < testpts.length; i++) {
            TestPoint tp = testpts[i];
            float relx = tp.getX() - cx;
            float rely = tp.getY() - cy;
            ret[i] = new TestPoint(relx * cos - rely * sin + cx,
                                   relx * sin + rely * cos + cy,
                                   tp.isContains());
        }
        return ret;
    }

    public void checkPoints(Node n, TestPoint... testpts) {
        for (TestPoint tp : testpts) {
            checkContentPoint(n, tp, IDENTITY);
            checkContentPoint(n, tp, TRANSLATE);
            checkContentPoint(n, tp, SCALE);
            checkContentPoint(n, tp, ROTATE);
            checkContentPoint(n, tp, TRANSLATE_SCALE);
            checkContentPoint(n, tp, SCALE_TRANSLATE);
        }
    }

    @Test public void testRectangle() {
        Node r = makeRectangle(10, 10, 20, 20);
        checkPoints(r, rectPoints(10, 10, 20, 20));
    }

    @Test public void testTranslatedRectangle() {
        Node r = translate(234.7f, 176.3f, makeRectangle(10, 10, 20, 20));
        // Content bounds is local to the node, so we ignore the tx, ty
        checkPoints(r, rectPoints(10, 10, 20, 20));
    }

    @Test public void testScaledRectangle() {
        Node r = scale(1.3, 1.1, makeRectangle(10, 10, 20, 20));
        // Content bounds is local to the node, so we ignore the sx, sy
        checkPoints(r, rectPoints(10, 10, 20, 20));
    }

    @Test public void testRotatedRectangle() {
        Node r = rotate(15, makeRectangle(10, 10, 20, 20));
        // Content bounds is local to the node, so we ignore the rot
        checkPoints(r, rectPoints(10, 10, 20, 20));
    }

    @Test public void testGroupedRectangle() {
        Node r = group(makeRectangle(10, 10, 20, 20));
        checkPoints(r, rectPoints(10, 10, 20, 20));
    }

    @Test public void testGroupedTranslatedRectangle() {
        float tx = 234.7f;
        float ty = 165.3f;
        Node r = group(translate(tx, ty, makeRectangle(10, 10, 20, 20)));
        checkPoints(r, translate(tx, ty, rectPoints(10, 10, 20, 20)));
    }

    @Test public void testGroupedScaledRectangle() {
        float sx = 1.3f;
        float sy = 1.1f;
        Node n = group(scale(sx, sy, makeRectangle(10, 10, 20, 20)));
        checkPoints(n, scale(sx, sy, rectPoints(10, 10, 20, 20)));
    }

    @Test public void testGroupedScaledGroupedTranslatedGroupedRotatedRectangle() {
        float sx = 1.3f;
        float sy = 1.1f;
        float tx = 35.7f;
        float ty = 93.1f;
        float rot = 25;
        Node n = group(scale(sx, sy,
                    group(translate(tx, ty,
                        group(rotate(rot,
                            makeRectangle(10, 10, 20, 20)))))));
        checkPoints(n, scale(sx, sy,
                           translate(tx, ty,
                               rotate(rot,
                                   rectPoints(10, 10, 20, 20)))));
    }
}
