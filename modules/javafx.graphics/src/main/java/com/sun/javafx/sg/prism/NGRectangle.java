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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RectangularShape;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.RectShadowGraphics;
import com.sun.prism.paint.Color;
import com.sun.prism.shape.ShapeRep;
import com.sun.scenario.effect.Effect;
import static com.sun.javafx.geom.transform.BaseTransform.TYPE_MASK_SCALE;
import static com.sun.javafx.geom.transform.BaseTransform.TYPE_QUADRANT_ROTATION;
import static com.sun.javafx.geom.transform.BaseTransform.TYPE_TRANSLATION;

/**
 */
public class NGRectangle extends NGShape {

    private RoundRectangle2D rrect = new RoundRectangle2D();

    public void updateRectangle(float x, float y, float width, float height,
                                          float arcWidth, float arcHeight) {
        rrect.x = x;
        rrect.y = y;
        rrect.width = width;
        rrect.height = height;
        rrect.arcWidth = arcWidth;
        rrect.arcHeight = arcHeight;
        geometryChanged();
    }

    @Override
    protected boolean supportsOpaqueRegions() { return true; }

    @Override
    protected boolean hasOpaqueRegion() {
        return super.hasOpaqueRegion() && rrect.width > 1 && rrect.height > 1;
    }

    static final float HALF_MINUS_HALF_SQRT_HALF = 0.5f - NGCircle.HALF_SQRT_HALF;

    @Override
    protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
        // Normally the "opaque region" for a rectangle would be the
        // x, y, w, h unless it has rounded corners, in which case
        // we subtract the arc width and arc height.
        final float x = rrect.x;
        final float y = rrect.y;
        final float w = rrect.width;
        final float h = rrect.height;
        final float aw = rrect.arcWidth;
        final float ah = rrect.arcHeight;

        if (aw <= 0 || ah <= 0) {
            // This is the simple case of a rectangle. Note the "||" is correct in the if statement,
            // because regardless of the size of aw, if ah <= 0 (or vice versa) we draw a non-rounded
            // rectangle, so we should always enter this case for things that get drawn as rectangles
            return (RectBounds) opaqueRegion.deriveWithNewBounds(x, y, 0, x + w, y + h, 0);
        } else {
            // Gives us a reasonable number of pixels that are the interior of the rounded rectangle
            // including when aw / ah is massive (in which case we just render an ellipse with w/h
            // as the dimensions).
            final float arcInsetWidth = Math.min(w, aw) * HALF_MINUS_HALF_SQRT_HALF;
            final float arcInsetHeight = Math.min(h, ah) * HALF_MINUS_HALF_SQRT_HALF;
            return (RectBounds) opaqueRegion.deriveWithNewBounds(
                    x + arcInsetWidth, y + arcInsetHeight, 0,
                    x + w - arcInsetWidth, y + h - arcInsetHeight, 0);
        }
    }

    boolean isRounded() {
        return rrect.arcWidth > 0f && rrect.arcHeight > 0f;
    }

    @Override
    protected void renderEffect(Graphics g) {
        if (!(g instanceof RectShadowGraphics) || !renderEffectDirectly(g)) {
            super.renderEffect(g);
        }
    }

    private boolean renderEffectDirectly(Graphics g) {
        if (mode != Mode.FILL || isRounded()) {
            // TODO: Allow solid strokes that are square in the corners... (RT-26974)
            return false;
        }
        float alpha = g.getExtraAlpha();
        if (fillPaint instanceof Color) {
            alpha *= ((Color) fillPaint).getAlpha();
        } else {
            // TODO: Check if all colors in a gradient have same alpha... (RT-26974)
            return false;
        }
        Effect effect = getEffect();
        if (EffectUtil.renderEffectForRectangularNode(this, g, effect,
                                                      alpha, true /* antialiased */,
                                                      rrect.x, rrect.y,
                                                      rrect.width, rrect.height))
        {
            return true;
        }
        return false;
    }

    @Override
    public final Shape getShape() {
        return rrect;
    }

    @Override
    protected ShapeRep createShapeRep(Graphics g) {
        return g.getResourceFactory().createRoundRectRep();
    }

    private static final double SQRT_2 = Math.sqrt(2.0);
    private static boolean hasRightAngleMiterAndNoDashes(BasicStroke bs) {
        return (bs.getLineJoin() == BasicStroke.JOIN_MITER &&
                bs.getMiterLimit() >= SQRT_2 &&
                bs.getDashArray() == null);
    }

    static boolean rectContains(float x, float y,
                                NGShape node,
                                RectangularShape r)
    {
        double rw = r.getWidth();
        double rh = r.getHeight();
        if (rw < 0 || rh < 0) {
            return false;
        }
        Mode mode = node.mode;
        if (mode == Mode.EMPTY) {
            return false;
        }
        double rx = r.getX();
        double ry = r.getY();
        if (mode == Mode.FILL) {
            // shortcut for common case
            return (x >= rx && y >= ry && x < rx+rw && y < ry+rh);
        }
        // mode is STROKE or STROKE_FILL
        float outerpad = -1.0f; // check bounds+outerpad if >= 0.0
        float innerpad = -1.0f; // check bounds-innerpad if >= 0.0
        boolean checkstroke = false; // manually check stroke shape if true
        BasicStroke drawstroke = node.drawStroke;
        int type = drawstroke.getType();
        if (type == BasicStroke.TYPE_INNER) {
            if (mode == Mode.STROKE_FILL) {
                outerpad = 0.0f;
            } else {
                if (drawstroke.getDashArray() == null) {
                    outerpad = 0.0f;
                    innerpad = drawstroke.getLineWidth();
                } else {
                    checkstroke = true;
                }
            }
        } else if (type == BasicStroke.TYPE_OUTER) {
            if (hasRightAngleMiterAndNoDashes(drawstroke)) {
                outerpad = drawstroke.getLineWidth();
                if (mode == Mode.STROKE) {
                    innerpad = 0.0f;
                }
            } else {
                if (mode == Mode.STROKE_FILL) {
                    outerpad = 0.0f;
                }
                checkstroke = true;
            }
        } else if (type == BasicStroke.TYPE_CENTERED) {
            if (hasRightAngleMiterAndNoDashes(drawstroke)) {
                outerpad = drawstroke.getLineWidth() / 2.0f;
                if (mode == Mode.STROKE) {
                    innerpad = outerpad;
                }
            } else {
                if (mode == Mode.STROKE_FILL) {
                    outerpad = 0.0f;
                }
                checkstroke = true;
            }
        } else {
            // TODO: This should never happen... (RT-26974)
            if (mode == Mode.STROKE_FILL) {
                outerpad = 0.0f;
            }
            checkstroke = true;
        }
        if (outerpad >= 0.0f) {
            if (x >= rx   -outerpad && y >= ry   -outerpad &&
                x <  rx+rw+outerpad && y <  ry+rh+outerpad) {
                // point falls inside padded rectangle
                if (innerpad >= 0.0f &&
                    // we have an inner hole to test as well...
                    innerpad < rw/2.0f && innerpad < rh/2.0f &&
                    // and lw is small enough to make an inner hole
                    x >= rx   +innerpad && y >= ry   +innerpad &&
                    x <  rx+rw-innerpad && y <  ry+rh-innerpad)
                {
                    // point falls inside inner hole of stroked rectangle
                    return false;
                }
                return true;
            }
        }
        if (checkstroke) {
            return node.getStrokeShape().contains(x, y);
        }
        return false;
    }

    /**
     * Returns whether a clip represented by this node can be rendered using
     * axis aligned rect clip.
     *
     * The return value depends on whether the rectangle has arcs, current
     * rendering mode (stroked or not), and whether the transform is only
     * translate or scale or something more complex.
     *
     * @return whether this rectangle is axis aligned when rendered given node's
     * and rendering transform
     */
    @Override protected final boolean isRectClip(BaseTransform xform, boolean permitRoundedRectangle) {
        // must be a simple fill of a non-round rect with opaque paint
        // With more work we could optimize the case of a Rectangle with a
        // Rectangle as a clip, but that would likely slow down some more
        // common cases with an optimization of questionable value.
        if (mode != NGShape.Mode.FILL || getClipNode() != null || (getEffect() != null && getEffect().reducesOpaquePixels()) ||
            getOpacity() < 1f || (!permitRoundedRectangle && isRounded()) || !fillPaint.isOpaque())
        {
            return false;
        }

        BaseTransform nodeXform = getTransform();
        if (!nodeXform.isIdentity()) {
            // only bother concatenating if the passed xform is non-id
            // otherwise we can just use this node's tx
            if (!xform.isIdentity()) {
                TEMP_TRANSFORM.setTransform(xform);
                TEMP_TRANSFORM.concatenate(nodeXform);
                xform = TEMP_TRANSFORM;
            } else {
                xform = nodeXform;
            }
        }

        long t = xform.getType();
        return
            (t & ~(TYPE_TRANSLATION|TYPE_QUADRANT_ROTATION|TYPE_MASK_SCALE)) == 0;
    }
}
