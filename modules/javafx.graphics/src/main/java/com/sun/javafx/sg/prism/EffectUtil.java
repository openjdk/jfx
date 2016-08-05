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

import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.DropShadow;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.InnerShadow;

/**
 */
class EffectUtil {
    // We must use a power-of-2 size so that we can get true CLAMP_TO_EDGE on all platforms
    private static final int TEX_SIZE = 256;

    private static Texture itex;
    private static Texture dtex;

    /**
     * If possible, uses an optimized codepath to render the an
     * effect (InnerShadow or DropShadow) on the given rectangular node
     * (NGRectangle, NGImageView, etc).  If successful, returns true;
     * otherwise returns false to indicate that the caller should fall
     * back on existing methods to render the effect.
     */
    static boolean renderEffectForRectangularNode(NGNode node,
                                                  Graphics g,
                                                  Effect effect,
                                                  float alpha, boolean aa,
                                                  float rx, float ry,
                                                  float rw, float rh)
    {
        if (!g.getTransformNoClone().is2D() && g.isDepthBuffer() && g.isDepthTest()) {
            // TODO: Both of our optimizations below rely on layering of
            // 2 primitives that are dispatched with x and y coordinates
            // that are calculated displacements of the rx,ry,rw,rh.
            // Given that we are doing the calculations with IEEE floating
            // point (even if we used double precision) we are going to
            // end up with cases where interpolating between the calculated
            // coordinates generates Z values that are 1 ulp above or below
            // the Z value that is calculated for the primitive and it will
            // be essentially random whether we see the primitive or the
            // shadow on a per-pixel basis.  In practice, there tends to be
            // a sharp cutover from one to the other along some horizontal
            // or vertical threshold rather than random noise, but we cannot
            // force the latter rendered objects to overwrite the previously
            // rendered objects if we have depth buffers on.
            // Note that a Z offset could be used to force layering, but
            // that has 2 issues:
            // - What Z offset do we use?  It is dependent on the precision
            //   of the depth buffer and also on the 3D transform and probably
            //   the camera and near/far clipping planes.  We can try to
            //   install a pre-translation (translate by N device units),
            //   but we still may choose a value that does not quite "bump"
            //   the Z coordinates far enough since floating point has a
            //   scaled mantissa.
            // - If the primitive turns around it will lose its inner shadow
            //   or be covered completly by the drop shadow.  Both results
            //   are inconsistent with the image-based results delivered by
            //   Decora.  This could also be adjusted by examining the facing
            //   of the primitive and then changing the direction we bump the
            //   shadow, but the simplest and most compatible solution is to
            //   just turn off the optimizations when we have a 3D depth buffer.
            // (RT-26982)
            return false;
        }
        if (effect instanceof InnerShadow && !aa) {
            // TODO: Handle AA, or at least case when rectangle is pixel aligned...
            // (RT-26982)
            InnerShadow shadow = (InnerShadow)effect;
            float radius = shadow.getRadius();
            if (radius > 0f &&
                radius < rw/2 &&
                radius < rh/2 &&
                shadow.getChoke() == 0f &&
                shadow.getShadowSourceInput() == null &&
                shadow.getContentInput() == null)
            {
                node.renderContent(g);
                EffectUtil.renderRectInnerShadow(g, shadow, alpha, rx, ry, rw, rh);
                return true;
            }
        } else if (effect instanceof DropShadow) {
            DropShadow shadow = (DropShadow)effect;
            float radius = shadow.getRadius();
            if (radius > 0f &&
                radius < rw/2 &&
                radius < rh/2 &&
                shadow.getSpread() == 0f &&
                shadow.getShadowSourceInput() == null &&
                shadow.getContentInput() == null)
            {
                EffectUtil.renderRectDropShadow(g, shadow, alpha, rx, ry, rw, rh);
                node.renderContent(g);
                return true;
            }
        }
        return false;
    }

    static void renderRectInnerShadow(Graphics g, InnerShadow shadow, float alpha,
                                      float rx, float ry, float rw, float rh)
    {
        if (itex == null) {
            byte[] sdata = new byte[TEX_SIZE * TEX_SIZE];
            fillGaussian(sdata, TEX_SIZE, TEX_SIZE/2, shadow.getChoke(), true);
            Image img = Image.fromByteAlphaData(sdata, TEX_SIZE, TEX_SIZE);
            itex = g.getResourceFactory().createTexture(img,
                                                        Texture.Usage.STATIC,
                                                        Texture.WrapMode.CLAMP_TO_EDGE);
            // We use a power-of-2 size so that we can get true CLAMP_TO_EDGE on all platforms
            assert itex.getWrapMode() == Texture.WrapMode.CLAMP_TO_EDGE;
            itex.contentsUseful();
            itex.makePermanent();
        }
        float r = shadow.getRadius();
        int texsize = itex.getPhysicalWidth();
        int tcx1 = itex.getContentX();
        int tcx2 = tcx1 + itex.getContentWidth();
        float t1 = (tcx1 + 0.5f) / texsize;
        float t2 = (tcx2 - 0.5f) / texsize; // end of image
        float cx1 = rx;
        float cy1 = ry;
        float cx2 = rx + rw;
        float cy2 = ry + rh;
        float ox1 = cx1 + shadow.getOffsetX();
        float oy1 = cy1 + shadow.getOffsetY();
        float ox2 = ox1 + rw;
        float oy2 = oy1 + rh;
        g.setPaint(toPrismColor(shadow.getColor(), alpha));
        // TODO: The "outer edge" slices below overlap at the corners... (RT-26982)
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           cx1,   cy1,   cx2,   oy1-r,
                           t1,    t1,    t1,    t1); // outside above
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox1-r, oy1-r, ox1+r, oy1+r,
                           t1,    t1,    t2,    t2); // top-left corner
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox1+r, oy1-r, ox2-r, oy1+r,
                           t2,    t1,    t2,    t2); // top edge
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox2-r, oy1-r, ox2+r, oy1+r,
                           t2,    t1,    t1,    t2); // top-right corner
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           cx1,   oy1-r, ox1-r, oy2+r,
                           t1,    t1,    t1,    t1); // outside left
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox1-r, oy1+r, ox1+r, oy2-r,
                           t1,    t2,    t2,    t2); // left edge
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox2-r, oy1+r, ox2+r, oy2-r,
                           t2,    t2,    t1,    t2); // right edge
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox2+r, oy1-r, cx2,   oy2+r,
                           t1,    t1,    t1,    t1); // outside right
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox1-r, oy2-r, ox1+r, oy2+r,
                           t1,    t2,    t2,    t1); // bot-left corner
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox1+r, oy2-r, ox2-r, oy2+r,
                           t2,    t2,    t2,    t1); // bot edge
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           ox2-r, oy2-r, ox2+r, oy2+r,
                           t2,    t2,    t1,    t1); // bot-right corner
        drawClippedTexture(g, itex,
                           cx1,   cy1,   cx2,   cy2,
                           cx1,   oy2+r, cx2,   cy2,
                           t1,    t1,    t1,    t1); // outside below
    }

    static void drawClippedTexture(Graphics g, Texture tex,
                                   float cx1, float cy1, float cx2, float cy2,
                                   float ox1, float oy1, float ox2, float oy2,
                                   float tx1, float ty1, float tx2, float ty2)
    {
        if (ox1 >= ox2 || oy1 >= oy2 || cx1 >= cx2 || cy1 >= cy2) return;
        if (ox2 > cx1 && ox1 < cx2) {
            if (ox1 < cx1) {
                tx1 += (tx2 - tx1) * (cx1 - ox1) / (ox2 - ox1);
                ox1 = cx1;
            }
            if (ox2 > cx2) {
                tx2 -= (tx2 - tx1) * (ox2 - cx2) / (ox2 - ox1);
                ox2 = cx2;
            }
        } else {
            return;
        }
        if (oy2 > cy1 && oy1 < cy2) {
            if (oy1 < cy1) {
                ty1 += (ty2 - ty1) * (cy1 - oy1) / (oy2 - oy1);
                oy1 = cy1;
            }
            if (oy2 > cy2) {
                ty2 -= (ty2 - ty1) * (oy2 - cy2) / (oy2 - oy1);
                oy2 = cy2;
            }
        } else {
            return;
        }
        g.drawTextureRaw(tex, ox1, oy1, ox2, oy2, tx1, ty1, tx2, ty2);
    }

    static void renderRectDropShadow(Graphics g, DropShadow shadow, float alpha,
                                     float rx, float ry, float rw, float rh)
    {
        if (dtex == null) {
            byte[] sdata = new byte[TEX_SIZE * TEX_SIZE];
            fillGaussian(sdata, TEX_SIZE, TEX_SIZE / 2, shadow.getSpread(), false);
            //fillTestPattern(sdata, imgsize);
            Image img = Image.fromByteAlphaData(sdata, TEX_SIZE, TEX_SIZE);
            dtex = g.getResourceFactory().createTexture(img,
                                                        Texture.Usage.STATIC,
                                                        Texture.WrapMode.CLAMP_TO_EDGE);
            // We use a power-of-2 size so that we can get true CLAMP_TO_EDGE on all platforms
            assert dtex.getWrapMode() == Texture.WrapMode.CLAMP_TO_EDGE;
            dtex.contentsUseful();
            dtex.makePermanent();
        }
        float r = shadow.getRadius();
        int texsize = dtex.getPhysicalWidth();
        int cx1 = dtex.getContentX();
        int cx2 = cx1 + dtex.getContentWidth();
        float t1 = (cx1 + 0.5f) / texsize;
        float t2 = (cx2 - 0.5f) / texsize; // end of image
        float x1 = rx + shadow.getOffsetX();
        float y1 = ry + shadow.getOffsetY();
        float x2 = x1 + rw;
        float y2 = y1 + rh;
        g.setPaint(toPrismColor(shadow.getColor(), alpha));
        g.drawTextureRaw(dtex,
                         x1-r, y1-r, x1+r, y1+r,
                         t1,   t1,   t2,   t2); // top-left corner
        g.drawTextureRaw(dtex,
                         x2-r, y1-r, x2+r, y1+r,
                         t2,   t1,   t1,   t2); // top-right corner
        g.drawTextureRaw(dtex,
                         x2-r, y2-r, x2+r, y2+r,
                         t2,   t2,   t1,   t1); // bot-right corner
        g.drawTextureRaw(dtex,
                         x1-r, y2-r, x1+r, y2+r,
                         t1,   t2,   t2,   t1); // bot-left corner
        g.drawTextureRaw(dtex,
                         x1+r, y1+r, x2-r, y2-r,
                         t2,   t2,   t2,   t2); // center section
        g.drawTextureRaw(dtex,
                         x1-r, y1+r, x1+r, y2-r,
                         t1,   t2,   t2,   t2); // left edge
        g.drawTextureRaw(dtex,
                         x2-r, y1+r, x2+r, y2-r,
                         t2,   t2,   t1,   t2); // right edge
        g.drawTextureRaw(dtex,
                         x1+r, y1-r, x2-r, y1+r,
                         t2,   t1,   t2,   t2); // top edge
        g.drawTextureRaw(dtex,
                         x1+r, y2-r, x2-r, y2+r,
                         t2,   t2,   t2,   t1); // bottom edge
    }

    private static void fillGaussian(byte[] pixels, int dim,
                                     float r, float spread,
                                     boolean inner)
    {
        // Only works right if w==r and h==r
        float sigma = r / 3;
        float sigma22 = 2 * sigma * sigma;
        if (sigma22 < Float.MIN_VALUE) {
            // Avoid divide by 0 below (can generate NaN values)
            sigma22 = Float.MIN_VALUE;
        }
        // Really should just be new float[r]
        float kvals[] = new float[dim];
        int center = (dim+1)/2;
        float total = 0.0f;
        for (int i = 0; i < kvals.length; i++) {
            int d = center - i;
            total += (float) Math.exp(-(d * d) / sigma22);
            kvals[i] = total;
        }
//        total += (kvals[kvals.length-1] - total) * spread;
        for (int i = 0; i < kvals.length; i++) {
            kvals[i] /= total;
        }
        for (int y = 0; y < dim; y++) {
            for (int x = 0; x < dim; x++) {
                float v = kvals[y] * kvals[x];
                if (inner) {
                    // For inner shadow, invert
                    v = 1.0f - v;
                }
                int a = (int) (v * 255);
                //System.err.println(x + " " + y + " " + v + " " + kvals[x] + " " + kvals[y]);
                if (a < 0) a = 0; else if (a > 255) a = 255;
                pixels[y*dim+x] = (byte)a;
           }
        }
    }

    private static Color toPrismColor(Color4f decoraColor, float alpha) {
        float r = decoraColor.getRed();
        float g = decoraColor.getGreen();
        float b = decoraColor.getBlue();
        float a = decoraColor.getAlpha() * alpha;
        return new Color(r, g, b, a);
    }

    private EffectUtil() {
    }
}
