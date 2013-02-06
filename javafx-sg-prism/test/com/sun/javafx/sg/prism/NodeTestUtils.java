/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;

/**
 *
 */
public class NodeTestUtils {

    private NodeTestUtils() { }
    
    static public TestNGRectangle createRectangle(int x, int y, int width, int height) {
        TestNGRectangle rect = new TestNGRectangle();
        rect.updateRectangle(x, y, width, height, 0, 0);
        final RectBounds bounds = new RectBounds(x, y, x + width, y + height);
        rect.setContentBounds(bounds);
        rect.setFillPaint(new Color(0, 0, 0, 1.0f));
        rect.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        rect.setTransformedBounds(bounds, false);
        return rect;
    }

    static public TestNGCircle createCircle(int cx, int cy, int radius) {
        TestNGCircle c = new TestNGCircle();
        c.updateCircle(cx, cy, radius);
        final RectBounds bounds = new RectBounds(cx - radius, cy - radius, cx + radius, cy + radius);
        c.setContentBounds(bounds);
        c.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        c.setTransformedBounds(bounds, false);
        return c;
    }

    static public TestNGGroup createGroup(NGNode... children) {
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
    
    static public TestNGRegion createRegion(int w, int h, NGNode... children) {
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
}
