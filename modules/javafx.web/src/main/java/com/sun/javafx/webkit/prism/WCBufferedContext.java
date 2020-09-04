/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGRectangle;
import com.sun.javafx.sg.prism.NodeEffectInput;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.scenario.effect.DropShadow;
import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCTransform;

final class WCBufferedContext extends WCGraphicsPrismContext {

    private final PrismImage img;
    private boolean isInitialized;

    WCBufferedContext(PrismImage img) {
        this.img = img;
    }

    @Override
    public Type type() {
        return Type.DEDICATED;
    }

    @Override
    public WCImage getImage() {
        return img;
    }

    @Override
    Graphics getGraphics(boolean checkClip) {
        init();
        if (baseGraphics == null) {
            baseGraphics = img.getGraphics();
        }
        return super.getGraphics(checkClip);
    }

    //
    // The shouldRender* methods below are used to figure out whether a
    // primitive being rendered is clipped out. In case it is, nothing is rendered
    // saving us a graphics/texture creation.
    // See RT-34443.
    //

    private final RectBounds TEMP_BOUNDS = new RectBounds();
    private final NGRectangle TEMP_NGRECT = new NGRectangle();
    private final RoundRectangle2D TEMP_RECT = new RoundRectangle2D();
    private final float[] TEMP_COORDS = new float[6];

    @Override
    protected boolean shouldCalculateIntersection() {
        return baseGraphics == null;
    }

    @Override
    protected boolean shouldRenderRect(float x, float y, float w, float h,
                                       DropShadow shadow,
                                       BasicStroke stroke)
    {
        if (!shouldCalculateIntersection()) {
            // Not spending time in the intersection test.
            return true;
        }
        // If baseGraphics is null, check if the rect bounds intersect clip
        // and if they don't, the rendering should be ignored as a no-op.

        // SHADOW. For a shadow case use the shape-based routine.

        if (shadow != null) {
            TEMP_RECT.setFrame(x, y, w, h);
            return shouldRenderShape(TEMP_RECT, shadow, stroke);
        }

        // STROKE. Compute stroke bounds directly (optimized for a rect).

        if (stroke != null) {
            float s = 0f;
            float sx2 = 0f;
            switch (stroke.getType()) {
              case BasicStroke.TYPE_CENTERED:
                  sx2 = stroke.getLineWidth();
                  s = sx2 / 2;
                  break;
              case BasicStroke.TYPE_OUTER:
                  s = stroke.getLineWidth();
                  sx2 = s * 2;
                  break;
              case BasicStroke.TYPE_INNER:
                  break;
              default:
                  break;
            }
            x -= s;
            y -= s;
            w += sx2;
            h += sx2;
        }
        TEMP_BOUNDS.setBounds(x, y, x + w, y + h);

        // TRANSFORM/CLIP

        return trIntersectsClip(TEMP_BOUNDS, getTransformNoClone());
    }

    @Override
    protected boolean shouldRenderShape(Shape shape,
                                        DropShadow shadow,
                                        BasicStroke stroke)
    {
        if (!shouldCalculateIntersection()) {
            // Not spending time in the intersection test.
            return true;
        }
        // If baseGraphics is null, check if the shape bounds intersect clip
        // and if they don't, the rendering should be ignored as a no-op.

        BaseTransform accumTX = (shadow != null) ?
            BaseTransform.IDENTITY_TRANSFORM : getTransformNoClone();

        // STROKE

        TEMP_COORDS[0] = TEMP_COORDS[1] = Float.POSITIVE_INFINITY;
        TEMP_COORDS[2] = TEMP_COORDS[3] = Float.NEGATIVE_INFINITY;
        if (stroke == null) {
            Shape.accumulate(TEMP_COORDS, shape, accumTX);
        } else {
            stroke.accumulateShapeBounds(TEMP_COORDS, shape, accumTX);
        }
        TEMP_BOUNDS.setBounds(TEMP_COORDS[0], TEMP_COORDS[1],
                              TEMP_COORDS[2], TEMP_COORDS[3]);

        // SHADOW

        BaseTransform tx = null;
        if (shadow != null) {
            TEMP_NGRECT.updateRectangle(TEMP_BOUNDS.getMinX(), TEMP_BOUNDS.getMinY(),
                                        TEMP_BOUNDS.getWidth(), TEMP_BOUNDS.getHeight(),
                                        0, 0);
            TEMP_NGRECT.setContentBounds(TEMP_BOUNDS);
            BaseBounds bb = shadow.getBounds(BaseTransform.IDENTITY_TRANSFORM,
                                             new NodeEffectInput(TEMP_NGRECT));
            assert bb.getBoundsType() == BaseBounds.BoundsType.RECTANGLE;
            TEMP_BOUNDS.setBounds((RectBounds)bb);

            tx = getTransformNoClone(); // to apply further
        }

        // TRANSFORM/CLIP

        return trIntersectsClip(TEMP_BOUNDS, tx);
    }

    // Note: the method can modify the "bounds" object
    private boolean trIntersectsClip(RectBounds bounds, BaseTransform tx) {
        if (tx != null && !tx.isIdentity()) {
            tx.transform(bounds, bounds);
        }
        Rectangle clip = getClipRectNoClone();
        if (clip != null) {
            return bounds.intersects(clip.x, clip.y,
                                     clip.x + clip.width, clip.y + clip.height);
        } else if (img != null) {
            return bounds.intersects(0, 0,
                                     img.getWidth() * img.getPixelScale(),
                                     img.getHeight() * img.getPixelScale());
        }
        return false;
    }

    @Override public void saveState() {
        init();
        super.saveState();
    }

    @Override public void scale(float sx, float sy) {
        init();
        super.scale(sx, sy);
    }

    @Override public void setTransform(WCTransform tm) {
        init();
        super.setTransform(tm);
    }

    private void init() {
        if (!isInitialized) {
            BaseTransform t = PrismGraphicsManager.getPixelScaleTransform();
            initBaseTransform(t);
            setClip(0, 0, img.getWidth(), img.getHeight());
            isInitialized = true;
        }
    }

    @Override public void dispose() {
        // NOP
        // BufferedImage context is mainly used by WebKit to draw canvas and
        // tiled SVG images and it doesn't hold complex layers. Making this
        // method NOP helps to render tiled SVG images asynchronously.
    }
}
