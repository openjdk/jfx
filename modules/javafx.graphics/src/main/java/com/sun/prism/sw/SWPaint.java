/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.sw;

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.pisces.GradientColorMap;
import com.sun.pisces.PiscesRenderer;
import com.sun.pisces.RendererBase;
import com.sun.pisces.Transform6;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.paint.Stop;

final class SWPaint {

    private final SWContext context;
    private final PiscesRenderer pr;

    private final BaseTransform paintTx = new Affine2D();
    private final Transform6 piscesTx = new Transform6();

    private float compositeAlpha = 1.0f;
    private float px, py, pw, ph;

    SWPaint(SWContext context, PiscesRenderer pr) {
        this.context = context;
        this.pr = pr;
    }

    float getCompositeAlpha() {
        return compositeAlpha;
    }

    void setCompositeAlpha(float newValue) {
        compositeAlpha = newValue;
    }

    void setColor(Color c, float compositeAlpha) {
        if (PrismSettings.debug) {
            System.out.println("PR.setColor: " + c);
        }
        this.pr.setColor((int) (c.getRed() * 255),
                (int) (255 * c.getGreen()),
                (int) (255 * c.getBlue()),
                (int) (255 * c.getAlpha() * compositeAlpha));
    }

    void setPaintFromShape(Paint p, BaseTransform tx, Shape shape, RectBounds nodeBounds,
                           float localX, float localY, float localWidth, float localHeight)
    {
        this.computePaintBounds(p, shape, nodeBounds, localX, localY, localWidth, localHeight);
        this.setPaintBeforeDraw(p, tx, px, py, pw, ph);
    }

    private void computePaintBounds(Paint p, Shape shape, RectBounds nodeBounds,
                                    float localX, float localY, float localWidth, float localHeight)
    {
        if (p.isProportional()) {
            if (nodeBounds != null) {
                px = nodeBounds.getMinX();
                py = nodeBounds.getMinY();
                pw = nodeBounds.getWidth();
                ph = nodeBounds.getHeight();
            } else if (shape != null) {
                final RectBounds bounds = shape.getBounds();
                px = bounds.getMinX();
                py = bounds.getMinY();
                pw = bounds.getWidth();
                ph = bounds.getHeight();
            } else {
                px = localX;
                py = localY;
                pw = localWidth;
                ph = localHeight;
            }
        } else {
            px = py = pw = ph = 0;
        }
    }

    void setPaintBeforeDraw(Paint p, BaseTransform tx, float x, float y, float width, float height) {
        switch (p.getType()) {
            case COLOR:
                this.setColor((Color)p, this.compositeAlpha);
                break;
            case LINEAR_GRADIENT:
                final LinearGradient lg = (LinearGradient)p;
                if (PrismSettings.debug) {
                    System.out.println("PR.setLinearGradient: " + lg.getX1() + ", " + lg.getY1() + ", " + lg.getX2() + ", " + lg.getY2());
                }

                paintTx.setTransform(tx);
                SWUtils.convertToPiscesTransform(paintTx, piscesTx);

                float x1 = lg.getX1();
                float y1 = lg.getY1();
                float x2 = lg.getX2();
                float y2 = lg.getY2();
                if (lg.isProportional()) {
                    x1 = x + width * x1;
                    y1 = y + height * y1;
                    x2 = x + width * x2;
                    y2 = y + height * y2;
                }
                this.pr.setLinearGradient((int)(SWUtils.TO_PISCES * x1), (int)(SWUtils.TO_PISCES * y1),
                        (int)(SWUtils.TO_PISCES * x2), (int)(SWUtils.TO_PISCES * y2),
                        getFractions(lg), getARGB(lg, this.compositeAlpha), getPiscesGradientCycleMethod(lg.getSpreadMethod()), piscesTx);
                break;
            case RADIAL_GRADIENT:
                final RadialGradient rg = (RadialGradient)p;
                if (PrismSettings.debug) {
                    System.out.println("PR.setRadialGradient: " + rg.getCenterX() + ", " + rg.getCenterY() + ", " + rg.getFocusAngle() + ", " + rg.getFocusDistance() + ", " + rg.getRadius());
                }

                paintTx.setTransform(tx);

                float cx = rg.getCenterX();
                float cy = rg.getCenterY();
                float r = rg.getRadius();
                if (rg.isProportional()) {
                    float dim = Math.min(width, height);
                    float bcx = x + width * 0.5f;
                    float bcy = y + height * 0.5f;
                    cx = bcx + (cx - 0.5f) * dim;
                    cy = bcy + (cy - 0.5f) * dim;
                    r *= dim;
                    if (width != height && width != 0.0 && height != 0.0) {
                        paintTx.deriveWithTranslation(bcx, bcy);
                        paintTx.deriveWithConcatenation(width / dim, 0, 0, height / dim, 0, 0);
                        paintTx.deriveWithTranslation(-bcx, -bcy);
                    }
                }
                SWUtils.convertToPiscesTransform(paintTx, piscesTx);

                final float fx = (float)(cx + rg.getFocusDistance() * r * Math.cos(Math.toRadians(rg.getFocusAngle())));
                final float fy = (float)(cy + rg.getFocusDistance() * r * Math.sin(Math.toRadians(rg.getFocusAngle())));

                this.pr.setRadialGradient((int) (SWUtils.TO_PISCES * cx), (int) (SWUtils.TO_PISCES * cy),
                        (int) (SWUtils.TO_PISCES * fx), (int) (SWUtils.TO_PISCES * fy), (int) (SWUtils.TO_PISCES * r),
                        getFractions(rg), getARGB(rg, this.compositeAlpha), getPiscesGradientCycleMethod(rg.getSpreadMethod()), piscesTx);
                break;
            case IMAGE_PATTERN:
                final ImagePattern ip = (ImagePattern)p;
                if (ip.getImage().getPixelFormat() == PixelFormat.BYTE_ALPHA) {
                    throw new UnsupportedOperationException("Alpha image is not supported as an image pattern.");
                } else {
                    this.computeImagePatternTransform(ip, tx, x, y, width, height);
                    final SWArgbPreTexture tex = context.validateImagePaintTexture(ip.getImage().getWidth(), ip.getImage().getHeight());
                    tex.update(ip.getImage());
                    if (this.compositeAlpha < 1.0f) {
                        tex.applyCompositeAlpha(this.compositeAlpha);
                    }

                    this.pr.setTexture(RendererBase.TYPE_INT_ARGB_PRE, tex.getDataNoClone(),
                            tex.getContentWidth(), tex.getContentHeight(), tex.getPhysicalWidth(),
                            piscesTx,
                            tex.getWrapMode() == Texture.WrapMode.REPEAT,
                            tex.getLinearFiltering(),
                            tex.hasAlpha());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown paint type: " + p.getType());
        }
    }

    private static int[] getARGB(Gradient grd, float compositeAlpha) {
        final int nstops = grd.getNumStops();
        final int argb[] = new int[nstops];
        for (int i = 0; i < nstops; i++) {
            final Stop stop = grd.getStops().get(i);
            final Color stopColor = stop.getColor();
            float alpha255 = 255 * stopColor.getAlpha() * compositeAlpha;
            argb[i] = ((((int)(alpha255)) & 0xFF) << 24) +
                    ((((int)(alpha255 * stopColor.getRed())) & 0xFF) << 16) +
                    ((((int)(alpha255 * stopColor.getGreen())) & 0xFF) << 8) +
                    (((int)(alpha255 * stopColor.getBlue())) & 0xFF);
        }
        return argb;
    }

    private static int[] getFractions(Gradient grd) {
        final int nstops = grd.getNumStops();
        final int fractions[] = new int[nstops];
        for (int i = 0; i < nstops; i++) {
            final Stop stop = grd.getStops().get(i);
            fractions[i] = (int)(SWUtils.TO_PISCES * stop.getOffset());
        }
        return fractions;
    }

    private static int getPiscesGradientCycleMethod(final int prismCycleMethod) {
        switch (prismCycleMethod) {
            case Gradient.PAD:
                return GradientColorMap.CYCLE_NONE;
            case Gradient.REFLECT:
                return GradientColorMap.CYCLE_REFLECT;
            case Gradient.REPEAT:
                return GradientColorMap.CYCLE_REPEAT;
        }
        return GradientColorMap.CYCLE_NONE;
    }

    Transform6 computeDrawTexturePaintTransform(BaseTransform tx, float dx1, float dy1, float dx2, float dy2,
                                                float sx1, float sy1, float sx2, float sy2)
    {
        paintTx.setTransform(tx);

        final float scaleX = computeScale(dx1, dx2, sx1, sx2);
        final float scaleY = computeScale(dy1, dy2, sy1, sy2);

        if (scaleX == 1 && scaleY == 1) {
            paintTx.deriveWithTranslation(-Math.min(sx1, sx2) + Math.min(dx1, dx2),
                    -Math.min(sy1, sy2) + Math.min(dy1, dy2));
        } else {
            paintTx.deriveWithTranslation(Math.min(dx1, dx2), Math.min(dy1, dy2));
            paintTx.deriveWithTranslation((scaleX >= 0) ? 0 : Math.abs(dx2 - dx1),
                    (scaleY >= 0) ? 0 : Math.abs(dy2 - dy1));
            paintTx.deriveWithConcatenation(scaleX, 0, 0, scaleY, 0, 0);
            paintTx.deriveWithTranslation(-Math.min(sx1, sx2), -Math.min(sy1, sy2));
        }

        SWUtils.convertToPiscesTransform(paintTx, piscesTx);
        return piscesTx;
    }

    private float computeScale(float dv1, float dv2, float sv1, float sv2) {
        final float dv_diff = dv2 - dv1;
        float scale = dv_diff / (sv2 - sv1);
        if (Math.abs(scale) > (Integer.MAX_VALUE >> 16)) {
            scale = Math.signum(scale) * (Integer.MAX_VALUE >> 16);
        }
        return scale;
    }

    Transform6 computeSetTexturePaintTransform(Paint p, BaseTransform tx, RectBounds nodeBounds,
                                               float localX, float localY, float localWidth, float localHeight)
    {
        this.computePaintBounds(p, null, nodeBounds, localX, localY, localWidth, localHeight);

        final ImagePattern ip = (ImagePattern)p;
        this.computeImagePatternTransform(ip, tx, px, py, pw, ph);
        return piscesTx;
    }

    private void computeImagePatternTransform(ImagePattern ip, BaseTransform tx, float x, float y, float width, float height) {
        final Image image = ip.getImage();
        if (PrismSettings.debug) {
            System.out.println("PR.setTexturePaint: " + image);
            System.out.println("imagePattern: x: " + ip.getX() + ", y: " + ip.getY() +
                    ", w: " + ip.getWidth() + ", h: " + ip.getHeight() + ", proportional: " + ip.isProportional());
        }

        paintTx.setTransform(tx);
        paintTx.deriveWithConcatenation(ip.getPatternTransformNoClone());
        if (ip.isProportional()) {
            paintTx.deriveWithConcatenation(width / image.getWidth() * ip.getWidth(), 0,
                    0, height / image.getHeight() * ip.getHeight(),
                    x + width * ip.getX(), y + height * ip.getY());
        } else {
            paintTx.deriveWithConcatenation(ip.getWidth() / image.getWidth(), 0,
                    0, ip.getHeight() / image.getHeight(),
                    x + ip.getX(), y + ip.getY());
        }
        SWUtils.convertToPiscesTransform(paintTx, piscesTx);
    }
}
