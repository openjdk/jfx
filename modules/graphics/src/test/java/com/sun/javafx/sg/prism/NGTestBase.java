/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import java.lang.reflect.Field;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;
import com.sun.scenario.effect.Effect;

/**
 * Base class for all tests in this package. The tests in this package are all about
 * testing the NGNode and associated classes. These tests all need to be able to work
 * directly against the NGNodes and not rely on the upper level scene graph nodes.
 * However Node and the other classes do manage a lot of tasks (like setting up the bounds
 * and transforms appropriately) during synchronization. This class contains all of the
 * useful convenience methods for creating scene graphs of NG nodes and manipulating them
 * so that we can test the NG nodes.
 */
public class NGTestBase {

    /** Transforms the given node by the specified transform. */
    protected static <N extends NGNode> void transform(N node, BaseTransform tx) {
        // Concatenate this transform with the one already on the node
        tx = node.getTransform().deriveWithConcatenation(tx);
        // Compute & set the new transformed bounds for the node
        node.setTransformedBounds(node.getEffectBounds(new RectBounds(), tx), false);
        // Set the transform matrix
        node.setTransformMatrix(tx);
    }

    /** Translate the given node by the specified amount */
    protected static <N extends NGNode> void translate(N node, double tx, double ty) {
        transform(node, BaseTransform.getTranslateInstance(tx, ty));
    }

    /** Set the given effect on the node. effect must not be null. */
    protected static <N extends NGNode> void setEffect(N node, Effect effect) {
        node.setEffect(null); // so that when we ask for the getEffectBounds, it won't include an old effect
        BaseBounds effectBounds = new RectBounds();
        effectBounds = effectBounds.deriveWithNewBounds(effect.getBounds(BaseTransform.IDENTITY_TRANSFORM, new NodeEffectInput(node)));
        BaseBounds clippedBounds = node.getEffectBounds(new RectBounds(), BaseTransform.IDENTITY_TRANSFORM);
        node.setEffect(effect);
        // The new transformed bounds should be the union of the old effect bounds, new effect bounds, and
        // then transform those bounds. The reason I'm doing it this way is to expose any bugs in the
        // getEffectBounds() implementation when an effect is present.
        effectBounds = effectBounds.deriveWithUnion(clippedBounds);
        node.setTransformedBounds(node.getTransform().transform(effectBounds, effectBounds), false);
    }

    public static TestNGRectangle createRectangle(int x, int y, int width, int height) {
        TestNGRectangle rect = new TestNGRectangle();
        rect.updateRectangle(x, y, width, height, 0, 0);
        final RectBounds bounds = new RectBounds(x, y, x + width, y + height);
        rect.setContentBounds(bounds);
        rect.setFillPaint(new Color(0, 0, 0, 1.0f));
        rect.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        rect.setTransformedBounds(bounds, false);
        return rect;
    }

    public static TestNGCircle createCircle(int cx, int cy, int radius) {
        TestNGCircle c = new TestNGCircle();
        c.updateCircle(cx, cy, radius);
        final RectBounds bounds = new RectBounds(cx - radius, cy - radius, cx + radius, cy + radius);
        c.setContentBounds(bounds);
        c.setFillPaint(new Color(0, 0, 0, 1.0f));
        c.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        c.setTransformedBounds(bounds, false);
        return c;
    }

    public static TestNGRegion createOpaqueRegion(int x, int y, int width, int height, NGNode... children) {
        TestNGRegion r = createTransparentRegion(x, y, width, height, children);
        r.updateBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.BLACK, null, null)));
        r.setOpaqueInsets(0, 0, 0, 0);
        return r;
    }

    public static TestNGRegion createTransparentRegion(int x, int y, int width, int height, NGNode... children) {
        TestNGRegion r = new TestNGRegion();
        for (NGNode child : children) {
            r.add(-1, child);
        }
        r.setSize(width, height);
        final RectBounds bounds = new RectBounds(0, 0, width, height);
        r.setContentBounds(bounds);
        if (x != 0 || y != 0) {
            r.setTransformMatrix(BaseTransform.getTranslateInstance(x, y));
            r.setTransformedBounds(r.getTransform().transform(bounds, new RectBounds()), true);
        } else {
            r.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
            r.setTransformedBounds(bounds, false);
        }
        return r;
    }

    public static TestNGGroup createGroup(NGNode... children) {
        TestNGGroup group = new TestNGGroup();
        BaseBounds contentBounds = new RectBounds();
        for (NGNode child : children) {
            contentBounds = contentBounds.deriveWithUnion(
                    child.getCompleteBounds(
                            new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
            group.add(-1, child);
        }
        group.setContentBounds(contentBounds);
        group.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        group.setTransformedBounds(contentBounds, false);
        return group;
    }

    public static TestNGRegion createRegion(int w, int h, NGNode... children) {
        TestNGRegion region = new TestNGRegion();
        BaseBounds contentBounds = new RectBounds();
        for (NGNode child : children) {
            contentBounds = contentBounds.deriveWithUnion(
                    child.getCompleteBounds(
                            new RectBounds(), BaseTransform.IDENTITY_TRANSFORM));
            region.add(-1, child);
        }
        region.setContentBounds(contentBounds);
        region.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        region.setTransformedBounds(contentBounds, false);
        region.setSize(w, h);

        // I have to do this nasty reflection trickery because we don't have a Toolkit for creating
        // the Prism Color that is the platform peer.
        javafx.scene.paint.Color color = new javafx.scene.paint.Color(0, 0, 0, 1);
        try {
            Field platformPaint = color.getClass().getDeclaredField("platformPaint");
            platformPaint.setAccessible(true);
            platformPaint.set(color, new Color(0f, 0f, 0f, 1f));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Background background = new Background(new BackgroundFill[] {
                new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)}, null);
        region.updateBackground(background);
        region.setOpaqueInsets(0, 0, 0, 0);
        return region;
    }


    public static final class TestNGGroup extends NGGroup implements TestNGNode {
        private boolean askedToAccumulateDirtyRegion;
        private boolean computedDirtyRegion;
        private boolean rendered;

        @Override
        protected void renderContent(Graphics g) {
            super.renderContent(g);
            rendered = true;
        }

        @Override public int accumulateDirtyRegions(final RectBounds clip,
                RectBounds dirtyRegion, DirtyRegionPool pool, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx) {
            askedToAccumulateDirtyRegion = true;
            return super.accumulateDirtyRegions(clip, dirtyRegion, pool, drc, tx, pvTx);
        }
        @Override protected int accumulateGroupDirtyRegion(
                final RectBounds clip, RectBounds dirtyRegion, DirtyRegionPool pool, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx)
        {
            computedDirtyRegion = true;
            return super.accumulateGroupDirtyRegion(clip, dirtyRegion, pool, drc, tx, pvTx);
        }
        @Override protected int accumulateNodeDirtyRegion(
                final RectBounds clip, RectBounds dirtyRegion, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx)
        {
            computedDirtyRegion = true;
            return super.accumulateNodeDirtyRegion(clip, dirtyRegion, drc, tx, pvTx);
        }
        @Override public boolean askedToAccumulateDirtyRegion() { return askedToAccumulateDirtyRegion; }
        @Override public boolean computedDirtyRegion() { return computedDirtyRegion; }
        @Override public boolean rendered() { return rendered; }
    }

    public static final class TestNGRegion extends NGRegion implements TestNGNode {
        private boolean askedToAccumulateDirtyRegion;
        private boolean computedDirtyRegion;
        private boolean rendered;

        @Override
        protected void renderContent(Graphics g) {
            super.renderContent(g);
            rendered = true;
        }

        @Override public int accumulateDirtyRegions(final RectBounds clip,
                RectBounds dirtyRegion, DirtyRegionPool pool, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx) {
            askedToAccumulateDirtyRegion = true;
            return super.accumulateDirtyRegions(clip, dirtyRegion, pool, drc, tx, pvTx);
        }
        @Override protected int accumulateGroupDirtyRegion(
                final RectBounds clip, RectBounds dirtyRegion, DirtyRegionPool pool, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx)
        {
            computedDirtyRegion = true;
            return super.accumulateGroupDirtyRegion(clip, dirtyRegion, pool, drc, tx, pvTx);
        }
        @Override protected int accumulateNodeDirtyRegion(
                final RectBounds clip, RectBounds dirtyRegion, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx)
        {
            computedDirtyRegion = true;
            return super.accumulateNodeDirtyRegion(clip, dirtyRegion, drc, tx, pvTx);
        }
        @Override public boolean askedToAccumulateDirtyRegion() { return askedToAccumulateDirtyRegion; }
        @Override public boolean computedDirtyRegion() { return computedDirtyRegion; }
        @Override public boolean rendered() { return rendered; }
    }

    public static final class TestNGRectangle extends NGRectangle implements TestNGNode {
        private boolean askedToAccumulateDirtyRegion;
        private boolean computedDirtyRegion;
        private boolean rendered;

        @Override
        protected void renderContent(Graphics g) {
            rendered = true;
        }

        @Override public int accumulateDirtyRegions(final RectBounds clip,
                RectBounds dirtyRegion, DirtyRegionPool pool, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx) {
            askedToAccumulateDirtyRegion = true;
            return super.accumulateDirtyRegions(clip, dirtyRegion, pool, drc, tx, pvTx);
        }
        @Override protected int accumulateNodeDirtyRegion(
                final RectBounds clip, RectBounds dirtyRegion, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx)
        {
            computedDirtyRegion = true;
            return super.accumulateNodeDirtyRegion(clip, dirtyRegion, drc, tx, pvTx);
        }
        @Override public boolean askedToAccumulateDirtyRegion() { return askedToAccumulateDirtyRegion; }
        @Override public boolean computedDirtyRegion() { return computedDirtyRegion; }
        @Override public boolean rendered() { return rendered; }
    }

    public  static final class TestNGCircle extends NGCircle implements TestNGNode {
        private boolean askedToAccumulateDirtyRegion;
        private boolean computedDirtyRegion;
        private boolean rendered;

        @Override
        protected void renderContent(Graphics g) {
            rendered = true;
        }

        @Override public int accumulateDirtyRegions(final RectBounds clip,
                RectBounds dirtyRegion, DirtyRegionPool pool, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx) {
            askedToAccumulateDirtyRegion = true;
            return super.accumulateDirtyRegions(clip, dirtyRegion, pool, drc, tx, pvTx);
        }
        @Override protected int accumulateNodeDirtyRegion(
                final RectBounds clip, RectBounds dirtyRegion, DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx)
        {
            computedDirtyRegion = true;
            return super.accumulateNodeDirtyRegion(clip, dirtyRegion, drc,tx, pvTx);
        }
        @Override public boolean askedToAccumulateDirtyRegion() { return askedToAccumulateDirtyRegion; }
        @Override public boolean computedDirtyRegion() { return computedDirtyRegion; }
        @Override public boolean rendered() { return rendered; }

    }

    public interface TestNGNode {
        public boolean askedToAccumulateDirtyRegion();
        public boolean computedDirtyRegion();
        public boolean rendered();
    }

    public static abstract class Creator<N extends NGNode> {
        public abstract N create();
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
