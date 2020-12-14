/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d;

import java.awt.LinearGradientPaint;
import java.awt.font.GlyphVector;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.glass.ui.Screen;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.font.CompositeGlyphMapper;
import com.sun.javafx.font.CompositeStrike;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NodePath;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.MaskTextureGraphics;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackGraphics;
import com.sun.prism.RenderTarget;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.j2d.paint.MultipleGradientPaint.ColorSpaceType;
import com.sun.prism.j2d.paint.RadialGradientPaint;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.paint.Stop;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_OFF;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

public class J2DPrismGraphics
    // Do not subclass BaseGraphics without fixing drawTextureVO below...
    implements ReadbackGraphics, MaskTextureGraphics
    // Do not implement RectShadowGraphics without fixing RT-15016 (note that
    // BaseGraphics implements RectShadowGraphics).
{
    static {
        // Assuming direct translation of BasicStroke enums:
        assert(com.sun.prism.BasicStroke.CAP_BUTT == java.awt.BasicStroke.CAP_BUTT);
        assert(com.sun.prism.BasicStroke.CAP_ROUND == java.awt.BasicStroke.CAP_ROUND);
        assert(com.sun.prism.BasicStroke.CAP_SQUARE == java.awt.BasicStroke.CAP_SQUARE);
        assert(com.sun.prism.BasicStroke.JOIN_BEVEL == java.awt.BasicStroke.JOIN_BEVEL);
        assert(com.sun.prism.BasicStroke.JOIN_MITER == java.awt.BasicStroke.JOIN_MITER);
        assert(com.sun.prism.BasicStroke.JOIN_ROUND == java.awt.BasicStroke.JOIN_ROUND);
        // Assuming direct translation of PathIterator enums:
        assert(com.sun.javafx.geom.PathIterator.WIND_EVEN_ODD == java.awt.geom.PathIterator.WIND_EVEN_ODD);
        assert(com.sun.javafx.geom.PathIterator.WIND_NON_ZERO == java.awt.geom.PathIterator.WIND_NON_ZERO);
        assert(com.sun.javafx.geom.PathIterator.SEG_MOVETO == java.awt.geom.PathIterator.SEG_MOVETO);
        assert(com.sun.javafx.geom.PathIterator.SEG_LINETO == java.awt.geom.PathIterator.SEG_LINETO);
        assert(com.sun.javafx.geom.PathIterator.SEG_QUADTO == java.awt.geom.PathIterator.SEG_QUADTO);
        assert(com.sun.javafx.geom.PathIterator.SEG_CUBICTO == java.awt.geom.PathIterator.SEG_CUBICTO);
        assert(com.sun.javafx.geom.PathIterator.SEG_CLOSE == java.awt.geom.PathIterator.SEG_CLOSE);
    }
    static final LinearGradientPaint.CycleMethod LGP_CYCLE_METHODS[] = {
        LinearGradientPaint.CycleMethod.NO_CYCLE,
        LinearGradientPaint.CycleMethod.REFLECT,
        LinearGradientPaint.CycleMethod.REPEAT,
    };
    static final RadialGradientPaint.CycleMethod RGP_CYCLE_METHODS[] = {
        RadialGradientPaint.CycleMethod.NO_CYCLE,
        RadialGradientPaint.CycleMethod.REFLECT,
        RadialGradientPaint.CycleMethod.REPEAT,
    };

    private static final BasicStroke DEFAULT_STROKE =
        new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f);
    private static final Paint DEFAULT_PAINT = Color.WHITE;
    static java.awt.geom.AffineTransform J2D_IDENTITY =
        new java.awt.geom.AffineTransform();
    private int clipRectIndex;
    private boolean hasPreCullingBits = false;
    private float pixelScaleX = 1.0f;
    private float pixelScaleY = 1.0f;

    static java.awt.Color toJ2DColor(Color c) {
        return new java.awt.Color(c.getRed(),
                                  c.getGreen(),
                                  c.getBlue(),
                                  c.getAlpha());
    }

    /*
     * Ensure that no fractions are equal
     *
     * Note that the J2D objects reject equal fractions, but the FX versions
     * allow them.
     *
     * The FX version treats values with equal fractions such that as you
     * approach the fractional value from below it interpolates to the
     * first color associated with that fraction and as you interpolate
     * away from it from above it interpolates the last such color.
     *
     * To get the J2D version to exhibit the FX behavior we collapse all
     * adjacent fractional values into a pair of values that are stored
     * with a pair of immediately adjacent floating point values.  This way
     * they have unique fractions, but no fractional value can be generated
     * which fits between them.  Yet, as you approach from below it will
     * interpolate to the first of the pair of colors and as you move away
     * above it, the second value will take precedence for interpolation.
     *
     * Math.ulp() is used to generate an "immediately adjacent fp value".
     */
    static int fixFractions(float fractions[], java.awt.Color colors[]) {
        float fprev = fractions[0];
        int i = 1;  // index of next incoming color/fractions we will examine
        int n = 1;  // index of next outgoing color/fraction we will store
        while (i < fractions.length) {
            float f = fractions[i];
            java.awt.Color c = colors[i++];
            if (f <= fprev) {
                // If we find any duplicates after we reach 1.0 we can
                // just ignore the rest of the array.  Not only is there
                // no more "fraction room" to assign them to, but we will
                // never generate a fraction >1.0 to access them anyway
                if (f >= 1.0f) break;
                // Find all fractions that are either fprev or fprev+ulp
                // and collapse them into two entries, the first at fprev
                // which is already stored, and the last matching entry
                // will be stored with fraction fprev+ulp
                f = fprev + Math.ulp(fprev);
                while (i < fractions.length) {
                    if (fractions[i] > f) break;
                    // We continue to remember the color of the last
                    // "matching" entry so it can be stored below
                    c = colors[i++];
                }
            }
            fractions[n] = fprev = f;
            colors[n++] = c;
        }
        return n;
    }

    java.awt.Paint toJ2DPaint(Paint p, java.awt.geom.Rectangle2D b) {
        if (p instanceof Color) {
            return toJ2DColor((Color) p);
        } else if (p instanceof Gradient) {
            Gradient g = (Gradient) p;
            if (g.isProportional()) {
                if (b == null) {
                    return null;
                }
            }
            List<Stop> stops = g.getStops();
            int n = stops.size();
            float fractions[] = new float[n];
            java.awt.Color colors[] = new java.awt.Color[n];
            float prevf = -1f;
            boolean needsFix = false;
            for (int i = 0; i < n; i++) {
                Stop stop = stops.get(i);
                float f = stop.getOffset();
                needsFix = (needsFix || f <= prevf);
                fractions[i] = prevf = f;
                colors[i] = toJ2DColor(stop.getColor());
            }
            if (needsFix) {
                n = fixFractions(fractions, colors);
                if (n < fractions.length) {
                    float newf[] = new float[n];
                    System.arraycopy(fractions, 0, newf, 0, n);
                    fractions = newf;
                    java.awt.Color newc[] = new java.awt.Color[n];
                    System.arraycopy(colors, 0, newc, 0, n);
                    colors = newc;
                }
            }
            if (g instanceof LinearGradient) {
                LinearGradient lg = (LinearGradient) p;
                float x1 = lg.getX1();
                float y1 = lg.getY1();
                float x2 = lg.getX2();
                float y2 = lg.getY2();
                if (g.isProportional()) {
                    float x = (float) b.getX();
                    float y = (float) b.getY();
                    float w = (float) b.getWidth();
                    float h = (float) b.getHeight();
                    x1 = x + w * x1;
                    y1 = y + h * y1;
                    x2 = x + w * x2;
                    y2 = y + h * y2;
                }
                if (x1 == x2 && y1 == y2) {
                    // Hardware pipelines use an inverse transform of
                    // all zeros to choose colors when the start and end
                    // point are the same so that the first color is
                    // always chosen...
                    return colors[0];
                }
                java.awt.geom.Point2D p1 =
                    new java.awt.geom.Point2D.Float(x1, y1);
                java.awt.geom.Point2D p2 =
                    new java.awt.geom.Point2D.Float(x2, y2);
                LinearGradientPaint.CycleMethod method =
                    LGP_CYCLE_METHODS[g.getSpreadMethod()];
                return new LinearGradientPaint(p1, p2, fractions, colors, method);
            } else if (g instanceof RadialGradient) {
                RadialGradient rg = (RadialGradient) g;
                float cx = rg.getCenterX();
                float cy = rg.getCenterY();
                float r = rg.getRadius();
                double fa = Math.toRadians(rg.getFocusAngle());
                float fd = rg.getFocusDistance();
                java.awt.geom.AffineTransform at = J2D_IDENTITY;
                if (g.isProportional()) {
                    float x = (float) b.getX();
                    float y = (float) b.getY();
                    float w = (float) b.getWidth();
                    float h = (float) b.getHeight();
                    float dim = Math.min(w, h);
                    float bcx = x + w * 0.5f;
                    float bcy = y + h * 0.5f;
                    cx = bcx + (cx - 0.5f) * dim;
                    cy = bcy + (cy - 0.5f) * dim;
                    r *= dim;
                    if (w != h && w != 0.0 && h != 0.0) {
                        at = java.awt.geom.AffineTransform.getTranslateInstance(bcx, bcy);
                        at.scale(w / dim, h / dim);
                        at.translate(-bcx, -bcy);
                    }
                }
                java.awt.geom.Point2D center =
                    new java.awt.geom.Point2D.Float(cx, cy);
                float fx = (float) (cx + fd * r * Math.cos(fa));
                float fy = (float) (cy + fd * r * Math.sin(fa));
                java.awt.geom.Point2D focus =
                    new java.awt.geom.Point2D.Float(fx, fy);
                RadialGradientPaint.CycleMethod method =
                    RGP_CYCLE_METHODS[g.getSpreadMethod()];
                return new RadialGradientPaint(center, r, focus, fractions, colors,
                                               method, ColorSpaceType.SRGB, at);
            }
        } else if (p instanceof ImagePattern) {
            ImagePattern imgpat = (ImagePattern) p;
            float x = imgpat.getX();
            float y = imgpat.getY();
            float w = imgpat.getWidth();
            float h = imgpat.getHeight();
            if (p.isProportional()) {
                if (b == null) {
                    return null;
                }
                float bx = (float) b.getX();
                float by = (float) b.getY();
                float bw = (float) b.getWidth();
                float bh = (float) b.getHeight();
                w += x;
                h += y;
                x = bx + x * bw;
                y = by + y * bh;
                w = bx + w * bw;
                h = by + h * bh;
                w -= x;
                h -= y;
            }
            Texture tex =
                getResourceFactory().getCachedTexture(imgpat.getImage(), WrapMode.REPEAT);
            java.awt.image.BufferedImage bimg = ((J2DTexture) tex).getBufferedImage();
            tex.unlock();
            return new java.awt.TexturePaint(bimg, tmpRect(x, y, w, h));
        }
        throw new UnsupportedOperationException("Paint "+p+" not supported yet.");
    }

    static java.awt.Stroke toJ2DStroke(BasicStroke stroke) {
        float lineWidth = stroke.getLineWidth();
        int type = stroke.getType();
        if (type != BasicStroke.TYPE_CENTERED) {
            lineWidth *= 2;
        }
        java.awt.BasicStroke bs =
                new java.awt.BasicStroke(lineWidth,
                                         stroke.getEndCap(),
                                         stroke.getLineJoin(),
                                         stroke.getMiterLimit(),
                                         stroke.getDashArray(),
                                         stroke.getDashPhase());
        if (type == BasicStroke.TYPE_INNER) {
            return new InnerStroke(bs);
        } else if (type == BasicStroke.TYPE_OUTER) {
            return new OuterStroke(bs);
        } else {
            return bs;
        }
    }

    private static ConcurrentHashMap<java.awt.Font,
                                     WeakReference<java.awt.Font>>
        fontMap = new ConcurrentHashMap<java.awt.Font,
                                        WeakReference<java.awt.Font>>();
    private static volatile int cleared = 0;

    private static java.awt.Font toJ2DFont(FontStrike strike) {
        FontResource fr = strike.getFontResource();
        java.awt.Font j2dfont;
        Object peer = fr.getPeer();
        if (peer == null && fr.isEmbeddedFont()) {
            J2DFontFactory.registerFont(fr);
            peer = fr.getPeer();
        }
        if (peer != null && peer instanceof java.awt.Font) {
            j2dfont = (java.awt.Font)peer;
        } else {
            if (PlatformUtil.isMac()) {
                // Looking up J2D fonts via full name is not reliable on the
                // Mac, however using the PostScript font name is. The likely
                // cause is Mac platform internals heavy reliance on PostScript
                // names for font identification.
                String psName = fr.getPSName();
                // dummy size
                j2dfont = new java.awt.Font(psName, java.awt.Font.PLAIN, 12);

                // REMIND: Due to bugs in j2d font lookup, these two workarounds
                // are required to ensure the correct font is used. Once fixed
                // in the jdk these workarounds should be removed.
                if (!j2dfont.getPSName().equals(psName)) {
                    // 1. Lookup font via family and style. This covers the
                    // case when the J2D PostScript name does not match psName
                    // in font file. For example "HelveticaCYBold" has the
                    // psName "HelveticaCY-Bold" in j2d.
                    int style = fr.isBold() ? java.awt.Font.BOLD : 0;
                    style = style | (fr.isItalic() ? java.awt.Font.ITALIC : 0);
                    j2dfont = new java.awt.Font(fr.getFamilyName(), style, 12);

                    if(!j2dfont.getPSName().equals(psName)) {
                        // 2. J2D seems to be unable to find a few fonts where
                        // psName == familyName.  Workaround is an exhaustive
                        // search of all fonts.
                        java.awt.Font[] allj2dFonts =
                                java.awt.GraphicsEnvironment.
                                getLocalGraphicsEnvironment().getAllFonts();
                        for (java.awt.Font f : allj2dFonts) {
                            if (f.getPSName().equals(psName)) {
                                j2dfont = f;
                                break;
                            }
                        }
                    }
                }
            } else {
                // dummy size
                j2dfont = new java.awt.Font(fr.getFullName(),
                                            java.awt.Font.PLAIN, 12);
            }

            // Adding j2dfont as peer is OK since fr is a decomposed
            // FontResource. Thus preventing font lookup next time we render.
            fr.setPeer(j2dfont);
        }
        // deriveFont(...) still has a bug and will cause #2 problem to occur
        j2dfont = j2dfont.deriveFont(strike.getSize()); // exact float font size
        java.awt.Font compFont = null;
        WeakReference<java.awt.Font> ref = fontMap.get(j2dfont);
        if (ref != null) {
            compFont = ref.get();
            if (compFont == null) {
                cleared++;
            }
        }
        if (compFont == null) {
            if (fontMap.size() > 100 && cleared > 10) { // purge the map.
                for (java.awt.Font key : fontMap.keySet()) {
                    ref = fontMap.get(key);
                    if (ref == null || ref.get() == null) {
                        fontMap.remove(key);
                    }
                }
                cleared = 0;
            }
            compFont = J2DFontFactory.getCompositeFont(j2dfont);
            ref = new WeakReference(compFont);
            fontMap.put(j2dfont, ref);
        }
        return compFont;
    }

    public static java.awt.geom.AffineTransform
        toJ2DTransform(BaseTransform t)
    {
        return new java.awt.geom.AffineTransform(t.getMxx(), t.getMyx(),
                                                 t.getMxy(), t.getMyy(),
                                                 t.getMxt(), t.getMyt());
    }

    private static java.awt.geom.AffineTransform tmpAT =
        new java.awt.geom.AffineTransform();
    static java.awt.geom.AffineTransform tmpJ2DTransform(BaseTransform t)
    {
        tmpAT.setTransform(t.getMxx(), t.getMyx(),
                           t.getMxy(), t.getMyy(),
                           t.getMxt(), t.getMyt());
        return tmpAT;
    }

    static BaseTransform toPrTransform(java.awt.geom.AffineTransform t)
    {
        return BaseTransform.getInstance(t.getScaleX(), t.getShearY(),
                                         t.getShearX(), t.getScaleY(),
                                         t.getTranslateX(), t.getTranslateY());
    }

    static Rectangle toPrRect(java.awt.Rectangle r)
    {
        return new Rectangle(r.x, r.y, r.width, r.height);
    }

    private static java.awt.geom.Path2D tmpQuadShape =
        new java.awt.geom.Path2D.Float();
    private static java.awt.Shape tmpQuad(float x1, float y1,
                                          float x2, float y2)
    {
        tmpQuadShape.reset();
        tmpQuadShape.moveTo(x1, y1);
        tmpQuadShape.lineTo(x2, y1);
        tmpQuadShape.lineTo(x2, y2);
        tmpQuadShape.lineTo(x1, y2);
        tmpQuadShape.closePath();
        return tmpQuadShape;
    }

    private static java.awt.geom.Rectangle2D.Float tmpRect =
        new java.awt.geom.Rectangle2D.Float();
    private static java.awt.geom.Rectangle2D tmpRect(float x, float y, float w, float h) {
        tmpRect.setRect(x, y, w, h);
        return tmpRect;
    }

    private static java.awt.geom.Ellipse2D tmpEllipse =
        new java.awt.geom.Ellipse2D.Float();
    private static java.awt.Shape tmpEllipse(float x, float y, float w, float h) {
        tmpEllipse.setFrame(x, y, w, h);
        return tmpEllipse;
    }

    private static java.awt.geom.RoundRectangle2D tmpRRect =
        new java.awt.geom.RoundRectangle2D.Float();
    private static java.awt.Shape tmpRRect(float x, float y, float w, float h,
                                           float aw, float ah)
    {
        tmpRRect.setRoundRect(x, y, w, h, aw, ah);
        return tmpRRect;
    }

    private static java.awt.geom.Line2D tmpLine =
        new java.awt.geom.Line2D.Float();
    private static java.awt.Shape tmpLine(float x1, float y1, float x2, float y2) {
        tmpLine.setLine(x1, y1, x2, y2);
        return tmpLine;
    }

    private static AdaptorShape tmpAdaptor = new AdaptorShape();
    private static java.awt.Shape tmpShape(Shape s) {
        tmpAdaptor.setShape(s);
        return tmpAdaptor;
    }

    private boolean antialiasedShape = true;
    J2DPresentable target;
    java.awt.Graphics2D g2d;
    Affine2D transform;
    Rectangle clipRect;
    RectBounds devClipRect;
    RectBounds finalClipRect;
    Paint paint;
    boolean paintWasProportional;
    BasicStroke stroke;
    boolean cull;

    J2DPrismGraphics(J2DPresentable target, java.awt.Graphics2D g2d) {
        this(g2d, target.getContentWidth(), target.getContentHeight());
        this.target = target;
    }

    J2DPrismGraphics(java.awt.Graphics2D g2d, int width, int height) {
        this.g2d = g2d;
        captureTransform(g2d);
        this.transform = new Affine2D();
        this.devClipRect = new RectBounds(0, 0, width, height);
        this.finalClipRect = new RectBounds(0, 0, width, height);
        this.cull = true;

        g2d.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL,
                             java.awt.RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                              java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                             java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        /* Set the text hints to those most equivalent to FX rendering.
         * Will need to revisit this since its unlikely to be sufficient.
         */
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS,
                           java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


        setTransform(BaseTransform.IDENTITY_TRANSFORM);
        setPaint(DEFAULT_PAINT);
        setStroke(DEFAULT_STROKE);
    }

    public RenderTarget getRenderTarget() {
        return target;
    }

    public Screen getAssociatedScreen() {
        return target.getAssociatedScreen();
    }

    public ResourceFactory getResourceFactory() {
        return target.getResourceFactory();
    }

    public void reset() {
    }

    public Rectangle getClipRect() {
        return clipRect == null ? null : new Rectangle(clipRect);
    }

    public Rectangle getClipRectNoClone() {
        return clipRect;
    }

    public RectBounds getFinalClipNoClone() {
        return finalClipRect;
    }

    public void setClipRect(Rectangle clipRect) {
        this.finalClipRect.setBounds(devClipRect);
        if (clipRect == null) {
            this.clipRect = null;
            g2d.setClip(null);
        } else {
            this.clipRect = new Rectangle(clipRect);
            this.finalClipRect.intersectWith(clipRect);
            setTransformG2D(J2D_IDENTITY);
            g2d.setClip(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
            setTransformG2D(tmpJ2DTransform(transform));
        }
    }

    private java.awt.AlphaComposite getAWTComposite() {
        return (java.awt.AlphaComposite) g2d.getComposite();
    }

    public float getExtraAlpha() {
        return getAWTComposite().getAlpha();
    }

    public void setExtraAlpha(float extraAlpha) {
        g2d.setComposite(getAWTComposite().derive(extraAlpha));
    }

    public CompositeMode getCompositeMode() {
        int rule = getAWTComposite().getRule();
        switch (rule) {
            case java.awt.AlphaComposite.CLEAR:
                return CompositeMode.CLEAR;
            case java.awt.AlphaComposite.SRC:
                return CompositeMode.SRC;
            case java.awt.AlphaComposite.SRC_OVER:
                return CompositeMode.SRC_OVER;
            default:
                throw new InternalError("Unrecognized AlphaCompsite rule: "+rule);
        }
    }

    public void setCompositeMode(CompositeMode mode) {
        java.awt.AlphaComposite awtComp = getAWTComposite();
        switch (mode) {
            case CLEAR:
                awtComp = awtComp.derive(java.awt.AlphaComposite.CLEAR);
                break;
            case SRC:
                awtComp = awtComp.derive(java.awt.AlphaComposite.SRC);
                break;
            case SRC_OVER:
                awtComp = awtComp.derive(java.awt.AlphaComposite.SRC_OVER);
                break;
            default:
                throw new InternalError("Unrecognized composite mode: "+mode);
        }
        g2d.setComposite(awtComp);
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
        java.awt.Paint j2dpaint = toJ2DPaint(paint, null);
        if (j2dpaint == null) {
            paintWasProportional = true;
        } else {
            paintWasProportional = false;
            g2d.setPaint(j2dpaint);
        }
    }

    public BasicStroke getStroke() {
        return stroke;
    }

    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
        g2d.setStroke(toJ2DStroke(stroke));
    }

    public BaseTransform getTransformNoClone() {
        return transform;
    }

    public void translate(float tx, float ty) {
        transform.translate(tx, ty);
        g2d.translate(tx, ty);
    }

    public void scale(float sx, float sy) {
        transform.scale(sx, sy);
        g2d.scale(sx, sy);
    }

    public void transform(BaseTransform xform) {
        if (!xform.is2D()) {
            // No-op until we support 3D
            return;
        }
        transform.concatenate(xform);
        setTransformG2D(tmpJ2DTransform(transform));
    }

    public void setTransform(BaseTransform xform) {
        // TODO: Modify PrEffectHelper to not pass a null... (RT-27384)
        if (xform == null) xform = BaseTransform.IDENTITY_TRANSFORM;
        transform.setTransform(xform);
        setTransformG2D(tmpJ2DTransform(transform));
    }

    public void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12)
    {
        transform.setTransform(m00, m10, m01, m11, m02, m12);
        setTransformG2D(tmpJ2DTransform(transform));
    }

    public void clear() {
        clear(Color.TRANSPARENT);
    }

    public void clear(Color color) {
        this.getRenderTarget().setOpaque(color.isOpaque());
        clear(toJ2DColor(color));
    }

    void clear(java.awt.Color c) {
        java.awt.Graphics2D gtmp = (java.awt.Graphics2D) g2d.create();
        gtmp.setTransform(J2D_IDENTITY);
        gtmp.setComposite(java.awt.AlphaComposite.Src);
        gtmp.setColor(c);
        gtmp.fillRect(0, 0, target.getContentWidth(), target.getContentHeight());
        gtmp.dispose();
    }

    public void clearQuad(float x1, float y1, float x2, float y2) {
        g2d.setComposite(java.awt.AlphaComposite.Clear);
        g2d.fill(tmpQuad(x1, y1, x2, y2));
    }

    void fill(java.awt.Shape shape) {
        if (paintWasProportional) {
            if (nodeBounds != null) {
                g2d.setPaint(toJ2DPaint(paint, nodeBounds));
            } else {
                g2d.setPaint(toJ2DPaint(paint, shape.getBounds2D()));
            }
        }
        if (this.paint.getType() == Paint.Type.IMAGE_PATTERN) {
            ImagePattern imgpat = (ImagePattern) this.paint;
            java.awt.geom.AffineTransform at = toJ2DTransform(imgpat.getPatternTransformNoClone());

            if (!at.isIdentity()) {
                g2d.setClip(shape);
                g2d.transform(at);
                tmpAT.setTransform(at);
                try {
                    tmpAT.invert();
                    g2d.fill(tmpAT.createTransformedShape(shape));
                } catch (NoninvertibleTransformException e) {
                }
                setTransform(transform);
                setClipRect(clipRect);
                return;
            }
        }
        g2d.fill(shape);
    }

    public void fill(Shape shape) {
        fill(tmpShape(shape));
    }

    public void fillRect(float x, float y, float width, float height) {
        fill(tmpRect(x, y, width, height));
    }

    public void fillRoundRect(float x, float y, float width, float height,
                              float arcw, float arch)
    {
        fill(tmpRRect(x, y, width, height, arcw, arch));
    }

    public void fillEllipse(float x, float y, float width, float height) {
        fill(tmpEllipse(x, y, width, height));
    }

    public void fillQuad(float x1, float y1, float x2, float y2) {
        fill(tmpQuad(x1, y1, x2, y2));
    }

    void draw(java.awt.Shape shape) {
        if (paintWasProportional) {
            if (nodeBounds != null) {
                g2d.setPaint(toJ2DPaint(paint, nodeBounds));
            } else {
                g2d.setPaint(toJ2DPaint(paint, shape.getBounds2D()));
            }
        }
        try {
            g2d.draw(shape);
        } catch (Throwable t) {
            // Workaround for JDK bug 6670624
            // We may get a Ductus PRError (extends RuntimeException)
            // or we may get an InternalError (extends Error)
            // The only common superclass of the two is Throwable...
        }
    }

    public void draw(Shape shape) {
        draw(tmpShape(shape));
    }

    public void drawLine(float x1, float y1, float x2, float y2) {
        draw(tmpLine(x1, y1, x2, y2));
    }

    public void drawRect(float x, float y, float width, float height) {
        draw(tmpRect(x, y, width, height));
    }

    public void drawRoundRect(float x, float y, float width, float height, float arcw, float arch) {
        draw(tmpRRect(x, y, width, height, arcw, arch));
    }

    public void drawEllipse(float x, float y, float width, float height) {
        draw(tmpEllipse(x, y, width, height));
    }

    Rectangle2D nodeBounds = null;

    public void setNodeBounds(RectBounds bounds) {
        nodeBounds = bounds != null ?
                new Rectangle2D.Float(bounds.getMinX(), bounds.getMinY(),
                                      bounds.getWidth(),bounds.getHeight()) :
                null;
    }

    private void drawString(GlyphList gl, int start, int end,
                            FontStrike strike, float x, float y) {
        if (start == end) return;
        int count = end - start;
        int[] glyphs = new int[count];
        for (int i = 0; i < count; i++) {
            glyphs[i] = gl.getGlyphCode(start + i) & CompositeGlyphMapper.GLYPHMASK;
        }
        java.awt.Font j2dfont = toJ2DFont(strike);
        GlyphVector gv = j2dfont.createGlyphVector(g2d.getFontRenderContext(), glyphs);
        java.awt.geom.Point2D pt = new java.awt.geom.Point2D.Float();
        for (int i = 0; i < count; i++) {
            pt.setLocation(gl.getPosX(start + i), gl.getPosY(start + i));
            gv.setGlyphPosition(i, pt);
        }
        g2d.drawGlyphVector(gv, x, y);
    }

    public void drawString(GlyphList gl, FontStrike strike, float x, float y,
                           Color selectColor, int start, int end) {

        int count = gl.getGlyphCount();
        if (count == 0) return;

        // In JDK6, setting graphics AA disables fast text loops
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);

        // If the surface has Alpha, JDK will ignore the LCD loops.
        // So for this to have any effect we need to fix JDK, or
        // ensure an opaque surface type.
        if (strike.getAAMode() == FontResource.AA_LCD) {
            g2d.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        }

        if (paintWasProportional) {
            Rectangle2D rectBounds = nodeBounds;
            if (rectBounds == null) {
                Metrics m = strike.getMetrics();
                rectBounds = new Rectangle2D.Float(0,
                                                   m.getAscent(),
                                                   gl.getWidth(),
                                                   m.getLineHeight());
            }
            g2d.setPaint(toJ2DPaint(paint, rectBounds));
        }

        CompositeStrike cStrike = null;
        int slot = 0;
        if (strike instanceof CompositeStrike) {
            cStrike = (CompositeStrike)strike;
            int glyphCode = gl.getGlyphCode(0);
            slot = cStrike.getStrikeSlotForGlyph(glyphCode);
        }
        java.awt.Color sColor = null;
        java.awt.Color tColor = null;
        boolean selected = false;
        if (selectColor != null) {
            sColor = toJ2DColor(selectColor);
            tColor = g2d.getColor();
            int offset = gl.getCharOffset(0);
            selected = start <= offset && offset < end;
        }
        int index = 0;
        if (sColor != null || cStrike != null) {
            /* Draw a segment every time selection or font changes */
            for (int i = 1; i < count; i++) {
                if (sColor != null) {
                    int offset = gl.getCharOffset(i);
                    boolean glyphSelected = start <= offset && offset < end;
                    if (selected != glyphSelected) {
                        if (cStrike != null) {
                            strike = cStrike.getStrikeSlot(slot);
                        }
                        g2d.setColor(selected ? sColor : tColor);
                        drawString(gl, index, i, strike, x, y);
                        index = i;
                        selected = glyphSelected;
                    }
                }
                if (cStrike != null) {
                    int glyphCode = gl.getGlyphCode(i);
                    int glyphSlot = cStrike.getStrikeSlotForGlyph(glyphCode);
                    if (slot != glyphSlot) {
                        strike = cStrike.getStrikeSlot(slot);
                        if (sColor != null) {
                            g2d.setColor(selected ? sColor : tColor);
                        }
                        drawString(gl, index, i, strike, x, y);
                        index = i;
                        slot = glyphSlot;
                    }
                }
            }

            /* Set strike and color to draw the last segment */
            if (cStrike != null) {
                strike = cStrike.getStrikeSlot(slot);
            }
            if (sColor != null) {
                g2d.setColor(selected ? sColor : tColor);
            }
        }
        drawString(gl, index, count, strike, x, y);

        /* Always restore the graphics to its initial color */
        if (selectColor != null) {
            g2d.setColor(tColor);
        }

        // Set hints back to the default.
        g2d.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    /**
     * Overridden by printing subclass to preserve the printer graphics
     * transform.
     */
    protected void setTransformG2D(java.awt.geom.AffineTransform tx) {
        g2d.setTransform(tx);
    }

    /**
     * Needed only by printing subclass, which over-rides it.
     */
    protected void captureTransform(java.awt.Graphics2D g2d) {
        return;
    }

    public void drawMappedTextureRaw(Texture tex,
                                     float dx1, float dy1, float dx2, float dy2,
                                     float tx11, float ty11, float tx21, float ty21,
                                     float tx12, float ty12, float tx22, float ty22)
    {
        java.awt.Image img = ((J2DTexture) tex).getBufferedImage();
        float mxx = tx21-tx11;
        float myx = ty21-ty11;
        float mxy = tx12-tx11;
        float myy = ty12-ty11;
//        assert(Math.abs(mxx - (tx22-tx12)) < .000001);
//        assert(Math.abs(myx - (ty22-ty12)) < .000001);
//        assert(Math.abs(mxy - (tx22-tx21)) < .000001);
//        assert(Math.abs(myy - (ty22-ty21)) < .000001);
        setTransformG2D(J2D_IDENTITY);
        tmpAT.setTransform(mxx, myx, mxy, myy, tx11, ty11);
        try {
            tmpAT.invert();
            g2d.translate(dx1, dy1);
            g2d.scale(dx2-dx1, dy2-dy1);
            g2d.transform(tmpAT);
            g2d.drawImage(img, 0, 0, 1, 1, null);
        } catch (NoninvertibleTransformException e) {
        }
        setTransform(transform);
    }

    public void drawTexture(Texture tex, float x, float y, float w, float h) {
        java.awt.Image img = ((J2DTexture) tex).getBufferedImage();
        g2d.drawImage(img, (int) x, (int) y, (int) (x+w), (int) (y+h), 0, 0, (int)w, (int) h, null);
    }

    public void drawTexture(Texture tex,
                            float dx1, float dy1, float dx2, float dy2,
                            float sx1, float sy1, float sx2, float sy2)
    {
        java.awt.Image img = ((J2DTexture) tex).getBufferedImage();
        // Simply casting the subimage coordinates to integers does not
        // produce the same behavior as the Prism hw pipelines (see RT-19270).
        g2d.drawImage(img,
                (int) dx1, (int) dy1, (int) dx2, (int) dy2,
                (int) sx1, (int) sy1, (int) sx2, (int) sy2,
                null);
    }

    @Override
    public void drawTexture3SliceH(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dh1, float dh2, float sh1, float sh2)
    {
        // Workaround for problems in NGRegion which may pass zero-width
        // source image area.
        if (sh1 + 0.1f > sh2) sh2 += 1;
        drawTexture(tex, dx1, dy1, dh1, dy2, sx1, sy1, sh1, sy2);
        drawTexture(tex, dh1, dy1, dh2, dy2, sh1, sy1, sh2, sy2);
        drawTexture(tex, dh2, dy1, dx2, dy2, sh2, sy1, sx2, sy2);
    }

    @Override
    public void drawTexture3SliceV(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dv1, float dv2, float sv1, float sv2)
    {
        // Workaround for problems in NGRegion which may pass zero-height
        // source image area.
        if (sv1 +0.1f > sv2) sv2 += 1;
        drawTexture(tex, dx1, dy1, dx2, dv1, sx1, sy1, sx2, sv1);
        drawTexture(tex, dx1, dv1, dx2, dv2, sx1, sv1, sx2, sv2);
        drawTexture(tex, dx1, dv2, dx2, dy2, sx1, sv2, sx2, sy2);
    }

    @Override
    public void drawTexture9Slice(Texture tex,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float sx1, float sy1, float sx2, float sy2,
                                  float dh1, float dv1, float dh2, float dv2,
                                  float sh1, float sv1, float sh2, float sv2)
    {
        // Workaround for problems in NGRegion which may pass zero-width
        // or zero height source image area.
        if (sh1 + 0.1f > sh2) sh2 += 1;
        if (sv1 + 0.1f > sv2) sv2 += 1;
        drawTexture(tex, dx1, dy1, dh1, dv1, sx1, sy1, sh1, sv1);
        drawTexture(tex, dh1, dy1, dh2, dv1, sh1, sy1, sh2, sv1);
        drawTexture(tex, dh2, dy1, dx2, dv1, sh2, sy1, sx2, sv1);

        drawTexture(tex, dx1, dv1, dh1, dv2, sx1, sv1, sh1, sv2);
        drawTexture(tex, dh1, dv1, dh2, dv2, sh1, sv1, sh2, sv2);
        drawTexture(tex, dh2, dv1, dx2, dv2, sh2, sv1, sx2, sv2);

        drawTexture(tex, dx1, dv2, dh1, dy2, sx1, sv2, sh1, sy2);
        drawTexture(tex, dh1, dv2, dh2, dy2, sh1, sv2, sh2, sy2);
        drawTexture(tex, dh2, dv2, dx2, dy2, sh2, sv2, sx2, sy2);
    }

    public void drawTextureRaw(Texture tex,
                               float dx1, float dy1, float dx2, float dy2,
                               float tx1, float ty1, float tx2, float ty2)
    {
        int w = tex.getContentWidth();
        int h = tex.getContentHeight();
        tx1 *= w;
        ty1 *= h;
        tx2 *= w;
        ty2 *= h;
        drawTexture(tex, dx1, dy1, dx2, dy2, tx1, ty1, tx2, ty2);
    }

    public void drawTextureVO(Texture tex,
                              float topopacity, float botopacity,
                              float dx1, float dy1, float dx2, float dy2,
                              float sx1, float sy1, float sx2, float sy2)
    {
        // assert(caller is PrReflectionPeer and buffer is cleared to transparent)
        // NOTE: the assert conditions are true because that is the only
        // place where this method is used (unless we subclass BaseGraphics),
        // but there is no code here to verify that information.
        // The workarounds to do this for the general case would cost a lot
        // because they would involve creating a temporary intermediate buffer,
        // doing the operations below into the buffer, and then applying the
        // buffer to the destination.  That is not hard, but it costs a lot
        // of buffer allocation (or caching) when it is not really necessary
        // given the way this method is called currently.
        // Note that isoEdgeMask is ignored here, but since this is only ever
        // called by PrReflectionPeer and that code always uses ISOLATE_NONE
        // then we would only need to support ISOLATE_NONE.  The code below
        // does not yet verify if the results will be compatible with
        // ISOLATE_NONE, but given that the source coordinates are rounded to
        // integers in drawTexture() there is not much it can do to get exact
        // edge condition behavior until that deficiency is fixed (see
        // RT-19270 and RT-19271).
        java.awt.Paint savepaint = g2d.getPaint();
        java.awt.Composite savecomp = g2d.getComposite();
        java.awt.Color c1 = new java.awt.Color(1f, 1f, 1f, topopacity);
        java.awt.Color c2 = new java.awt.Color(1f, 1f, 1f, botopacity);
        g2d.setPaint(new java.awt.GradientPaint(0f, dy1, c1, 0f, dy2, c2, true));
        g2d.setComposite(java.awt.AlphaComposite.Src);
        int x = (int) Math.floor(Math.min(dx1, dx2));
        int y = (int) Math.floor(Math.min(dy1, dy2));
        int w = (int) Math.ceil(Math.max(dx1, dx2)) - x;
        int h = (int) Math.ceil(Math.max(dy1, dy2)) - y;
        g2d.fillRect(x, y, w, h);
        g2d.setComposite(java.awt.AlphaComposite.SrcIn);
        drawTexture(tex, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
        g2d.setComposite(savecomp);
        g2d.setPaint(savepaint);
    }

    public void drawPixelsMasked(RTTexture imgtex, RTTexture masktex,
                                 int dx, int dy, int dw, int dh,
                                 int ix, int iy, int mx, int my)
    {
        doDrawMaskTexture((J2DRTTexture) imgtex, (J2DRTTexture) masktex,
                          dx, dy, dw, dh,
                          ix, iy, mx, my,
                          true);
    }

    public void maskInterpolatePixels(RTTexture imgtex, RTTexture masktex, int dx,
                                      int dy, int dw, int dh, int ix, int iy,
                                      int mx, int my) {
        doDrawMaskTexture((J2DRTTexture) imgtex, (J2DRTTexture) masktex,
                          dx, dy, dw, dh,
                          ix, iy, mx, my,
                          false);
    }

    private void doDrawMaskTexture(J2DRTTexture imgtex, J2DRTTexture masktex,
                                   int dx, int dy, int dw, int dh,
                                   int ix, int iy, int mx, int my,
                                   boolean srcover)
    {
        int cx0 = clipRect.x;
        int cy0 = clipRect.y;
        int cx1 = cx0 + clipRect.width;
        int cy1 = cy0 + clipRect.height;

        if (dw <= 0 || dh <= 0) return;
        if (dx < cx0) {
            int bump = cx0 - dx;
            if ((dw -= bump) <= 0) return;
            ix += bump;
            mx += bump;
            dx = cx0;
        }
        if (dy < cy0) {
            int bump = cy0 - dy;
            if ((dh -= bump) <= 0) return;
            iy += bump;
            my += bump;
            dy = cy0;
        }
        if (dx + dw > cx1 && (dw = cx1 - dx) <= 0) return;
        if (dy + dh > cy1 && (dh = cy1 - dy) <= 0) return;

        int iw = imgtex.getContentWidth();
        int ih = imgtex.getContentHeight();
        if (ix < 0) {
            if ((dw += ix) <= 0) return;
            dx -= ix;
            mx -= ix;
            ix = 0;
        }
        if (iy < 0) {
            if ((dh += iy) <= 0) return;
            dy -= iy;
            my -= iy;
            iy = 0;
        }
        if (ix + dw > iw && (dw = iw - ix) <= 0) return;
        if (iy + dh > ih && (dh = ih - iy) <= 0) return;

        int mw = masktex.getContentWidth();
        int mh = masktex.getContentHeight();
        if (mx < 0) {
            if ((dw += mx) <= 0) return;
            dx -= mx;
            ix -= mx;
            mx = 0;
        }
        if (my < 0) {
            if ((dh += my) <= 0) return;
            dy -= my;
            iy -= my;
            my = 0;
        }
        if (mx + dw > mw && (dw = mw - mx) <= 0) return;
        if (my + dh > mh && (dh = mh - my) <= 0) return;

        int imgbuf[] = imgtex.getPixels();
        int maskbuf[] = masktex.getPixels();
        java.awt.image.DataBuffer db = target.getBackBuffer().getRaster().getDataBuffer();
        int dstbuf[] = ((java.awt.image.DataBufferInt) db).getData();
        int iscan = imgtex.getBufferedImage().getWidth();
        int mscan = masktex.getBufferedImage().getWidth();
        int dscan = target.getBackBuffer().getWidth();
        int ioff = iy * iscan + ix;
        int moff = my * mscan + mx;
        int doff = dy * dscan + dx;
        if (srcover) {
            for (int y = 0; y < dh; y++) {
                for (int x = 0; x < dw; x++) {
                    int a, r, g, b;
                    int maskalpha = maskbuf[moff+x] >>> 24;
                    if (maskalpha == 0) continue;
                    int imgpix = imgbuf[ioff+x];
                    a = (imgpix >>> 24);
                    if (a == 0) continue;
                    if (maskalpha < 0xff) {
                        maskalpha += (maskalpha >> 7);
                        a *= maskalpha;
                        r = ((imgpix >>  16) & 0xff) * maskalpha;
                        g = ((imgpix >>   8) & 0xff) * maskalpha;
                        b = ((imgpix       ) & 0xff) * maskalpha;
                    } else if (a < 0xff) {
                        a <<= 8;
                        r = ((imgpix >>  16) & 0xff) << 8;
                        g = ((imgpix >>   8) & 0xff) << 8;
                        b = ((imgpix       ) & 0xff) << 8;
                    } else {
                        dstbuf[doff+x] = imgpix;
                        continue;
                    }
                    maskalpha = ((a + 128) >> 8);
                    maskalpha += (maskalpha >> 7);
                    maskalpha = 256 - maskalpha;
                    imgpix = dstbuf[doff+x];
                    a += ((imgpix >>> 24)       ) * maskalpha + 128;
                    r += ((imgpix >>  16) & 0xff) * maskalpha + 128;
                    g += ((imgpix >>   8) & 0xff) * maskalpha + 128;
                    b += ((imgpix       ) & 0xff) * maskalpha + 128;
                    imgpix = ((a >> 8) << 24) +
                             ((r >> 8) << 16) +
                             ((g >> 8) <<  8) +
                             ((b >> 8)      );
                    dstbuf[doff+x] = imgpix;
                }
                ioff += iscan;
                moff += mscan;
                doff += dscan;
            }
        } else {
            for (int y = 0; y < dh; y++) {
                for (int x = 0; x < dw; x++) {
                    int maskalpha = maskbuf[moff+x] >>> 24;
                    if (maskalpha == 0) continue;
                    int imgpix = imgbuf[ioff+x];
                    if (maskalpha < 0xff) {
                        maskalpha += (maskalpha >> 7);
                        int a = ((imgpix >>> 24)       ) * maskalpha;
                        int r = ((imgpix >>  16) & 0xff) * maskalpha;
                        int g = ((imgpix >>   8) & 0xff) * maskalpha;
                        int b = ((imgpix       ) & 0xff) * maskalpha;
                        maskalpha = 256 - maskalpha;
                        imgpix = dstbuf[doff+x];
                        a += ((imgpix >>> 24)       ) * maskalpha + 128;
                        r += ((imgpix >>  16) & 0xff) * maskalpha + 128;
                        g += ((imgpix >>   8) & 0xff) * maskalpha + 128;
                        b += ((imgpix       ) & 0xff) * maskalpha + 128;
                        imgpix = ((a >> 8) << 24) +
                                 ((r >> 8) << 16) +
                                 ((g >> 8) <<  8) +
                                 ((b >> 8)      );
                    }
                    dstbuf[doff+x] = imgpix;
                }
                ioff += iscan;
                moff += mscan;
                doff += dscan;
            }
        }
    }

    public boolean canReadBack() {
        return true;
    }

    public RTTexture readBack(Rectangle view) {
        J2DRTTexture rtt = target.getReadbackBuffer();
        java.awt.Graphics2D rttg2d = rtt.createAWTGraphics2D();
        rttg2d.setComposite(java.awt.AlphaComposite.Src);
        int x0 = view.x;
        int y0 = view.y;
        int w = view.width;
        int h = view.height;
        int x1 = x0 + w;
        int y1 = y0 + h;
        rttg2d.drawImage(target.getBackBuffer(),
                          0,  0,  w,  h,
                         x0, y0, x1, y1, null);
        rttg2d.dispose();
        return rtt;
    }

    public void releaseReadBackBuffer(RTTexture view) {
        // This will be needed when we track LCD buffer locks and uses.
        // (See RT-29488)
//        target.getReadbackBuffer().unlock();
    }

    public NGCamera getCameraNoClone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPerspectiveTransform(GeneralTransform3D transform) {
    }


    public boolean isDepthBuffer() {
        return false;
    }

    public boolean isDepthTest() {
        return false;
    }

    public boolean isAlphaTestShader() {
        if (PrismSettings.verbose && PrismSettings.forceAlphaTestShader) {
            System.out.println("J2D pipe doesn't support shader with alpha testing");
        }
        return false;
    }

    public void setAntialiasedShape(boolean aa) {
        antialiasedShape = aa;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                antialiasedShape ? java.awt.RenderingHints.VALUE_ANTIALIAS_ON
                        : java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    public boolean isAntialiasedShape() {
        return antialiasedShape;
    }

    public void scale(float sx, float sy, float sz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setTransform3D(double mxx, double mxy, double mxz, double mxt,
                               double myx, double myy, double myz, double myt,
                               double mzx, double mzy, double mzz, double mzt)
    {
        if (mxz != 0.0 || myz != 0.0 ||
            mzx != 0.0 || mzy != 0.0 || mzz != 1.0 || mzt != 0.0)
        {
            throw new UnsupportedOperationException("3D transforms not supported.");
        }
        setTransform(mxx, myx, mxy, myy, mxt, myt);
    }

    public void setCamera(NGCamera camera) {
        // No-op until we support 3D
        /*
        if (!(camera instanceof PrismParallelCameraImpl)) {

            throw new UnsupportedOperationException(camera+" not supported.");
        }
        */
    }

    public void setDepthBuffer(boolean depthBuffer) {
        // No-op until we support 3D
    }

    public void setDepthTest(boolean depthTest) {
        // No-op until we support 3D
    }

    public void sync() {
    }

    public void translate(float tx, float ty, float tz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCulling(boolean cull) {
        this.cull = cull;
    }

    public boolean isCulling() {
        return this.cull;
    }

 public void setClipRectIndex(int index) {
        this.clipRectIndex = index;
    }
    public int getClipRectIndex() {
        return this.clipRectIndex;
    }

    public void setHasPreCullingBits(boolean hasBits) {
        this.hasPreCullingBits = hasBits;
    }

    public boolean hasPreCullingBits() {
        return hasPreCullingBits;
    }

    private NodePath renderRoot;
    @Override
    public void setRenderRoot(NodePath root) {
        this.renderRoot = root;
    }

    @Override
    public NodePath getRenderRoot() {
        return renderRoot;
    }

    public void setState3D(boolean flag) {
    }

    public boolean isState3D() {
        return false;
    }

    public void setup3DRendering() {
    }

    @Override
    public void setPixelScaleFactors(float pixelScaleX, float pixelScaleY) {
        this.pixelScaleX = pixelScaleX;
        this.pixelScaleY = pixelScaleY;
    }

    @Override
    public float getPixelScaleFactorX() {
        return pixelScaleX;
    }

    @Override
    public float getPixelScaleFactorY() {
        return pixelScaleY;
    }

    @Override
    public void blit(RTTexture srcTex, RTTexture dstTex,
            int srcX0, int srcY0, int srcX1, int srcY1,
            int dstX0, int dstY0, int dstX1, int dstY1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class AdaptorShape implements java.awt.Shape {
        private Shape prshape;

        public void setShape(Shape prshape) {
            this.prshape = prshape;
        }

        public boolean contains(double x, double y) {
            return prshape.contains((float) x, (float) y);
        }

        public boolean contains(java.awt.geom.Point2D p) {
            return contains(p.getX(), p.getY());
        }

        public boolean contains(double x, double y, double w, double h) {
            return prshape.contains((float) x, (float) y, (float) w, (float) h);
        }

        public boolean contains(java.awt.geom.Rectangle2D r) {
            return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        public boolean intersects(double x, double y, double w, double h) {
            return prshape.intersects((float) x, (float) y, (float) w, (float) h);
        }

        public boolean intersects(java.awt.geom.Rectangle2D r) {
            return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        public java.awt.Rectangle getBounds() {
            return getBounds2D().getBounds();
        }

        public java.awt.geom.Rectangle2D getBounds2D() {
            RectBounds b = prshape.getBounds();
            java.awt.geom.Rectangle2D r2d =
                new java.awt.geom.Rectangle2D.Float();
            r2d.setFrameFromDiagonal(b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
            return r2d;
        }

        private static AdaptorPathIterator tmpAdaptor =
                new AdaptorPathIterator();
        private static java.awt.geom.PathIterator tmpAdaptor(PathIterator pi) {
            tmpAdaptor.setIterator(pi);
            return tmpAdaptor;
        }

        public java.awt.geom.PathIterator
            getPathIterator(java.awt.geom.AffineTransform at)
        {
            BaseTransform tx = (at == null) ? null : toPrTransform(at);
            return tmpAdaptor(prshape.getPathIterator(tx));
        }

        public java.awt.geom.PathIterator
            getPathIterator(java.awt.geom.AffineTransform at,
                            double flatness)
        {
            BaseTransform tx = (at == null) ? null : toPrTransform(at);
            return tmpAdaptor(prshape.getPathIterator(tx, (float) flatness));
        }
    }

    private static class AdaptorPathIterator
        implements java.awt.geom.PathIterator
    {
        private static int NUM_COORDS[] = { 2, 2, 4, 6, 0 };
        PathIterator priterator;
        float tmpcoords[];

        public void setIterator(PathIterator priterator) {
            this.priterator = priterator;
        }

        public int currentSegment(float[] coords) {
            return priterator.currentSegment(coords);
        }

        public int currentSegment(double[] coords) {
            if (tmpcoords == null) {
                tmpcoords = new float[6];
            }
            int ret = priterator.currentSegment(tmpcoords);
            for (int i = 0; i < NUM_COORDS[ret]; i++) {
                coords[i] = (double) tmpcoords[i];
            }
            return ret;
        }

        public int getWindingRule() {
            return priterator.getWindingRule();
        }

        public boolean isDone() {
            return priterator.isDone();
        }

        public void next() {
            priterator.next();
        }
    }

    static abstract class FilterStroke implements java.awt.Stroke {
        protected java.awt.BasicStroke stroke;

        FilterStroke(java.awt.BasicStroke stroke) {
            this.stroke = stroke;
        }

        abstract protected java.awt.Shape makeStrokedRect(java.awt.geom.Rectangle2D r);
        abstract protected java.awt.Shape makeStrokedShape(java.awt.Shape s);

        public java.awt.Shape createStrokedShape(java.awt.Shape p) {
            if (p instanceof java.awt.geom.Rectangle2D) {
                java.awt.Shape s = makeStrokedRect((java.awt.geom.Rectangle2D) p);
                if (s != null) {
                    return s;
                }
            }
            return makeStrokedShape(p);
        }

        // ArcIterator.btan(Math.PI/2)
        static final double CtrlVal = 0.5522847498307933;

        static java.awt.geom.Point2D cornerArc(java.awt.geom.GeneralPath gp,
                                                      float x0, float y0,
                                                      float xc, float yc,
                                                      float x1, float y1)
        {
            return cornerArc(gp, x0, y0, xc, yc, x1, y1, 0.5f);
        }

        static java.awt.geom.Point2D cornerArc(java.awt.geom.GeneralPath gp,
                                                      float x0, float y0,
                                                      float xc, float yc,
                                                      float x1, float y1, float t)
        {
            float xc0 = (float) (x0 + CtrlVal * (xc - x0));
            float yc0 = (float) (y0 + CtrlVal * (yc - y0));
            float xc1 = (float) (x1 + CtrlVal * (xc - x1));
            float yc1 = (float) (y1 + CtrlVal * (yc - y1));
            gp.curveTo(xc0, yc0, xc1, yc1, x1, y1);

            return new java.awt.geom.Point2D.Float(eval(x0, xc0, xc1, x1, t),
                                                   eval(y0, yc0, yc1, y1, t));
        }

        static float eval(float c0, float c1, float c2, float c3, float t) {
            c0 = c0 + (c1-c0) * t;
            c1 = c1 + (c2-c1) * t;
            c2 = c2 + (c3-c2) * t;
            c0 = c0 + (c1-c0) * t;
            c1 = c1 + (c2-c1) * t;
            return c0 + (c1-c0) * t;
        }
    }

    static class InnerStroke extends FilterStroke {
        InnerStroke(java.awt.BasicStroke stroke) {
            super(stroke);
        }

        protected java.awt.Shape makeStrokedRect(java.awt.geom.Rectangle2D r) {
            if (stroke.getDashArray() != null) {
                return null;
            }
            float pad = stroke.getLineWidth() / 2f;
            if (pad >= r.getWidth() || pad >= r.getHeight()) {
                return r;
            }
            float rx0 = (float) r.getX();
            float ry0 = (float) r.getY();
            float rx1 = rx0 + (float) r.getWidth();
            float ry1 = ry0 + (float) r.getHeight();
            java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
            gp.moveTo(rx0, ry0);
            gp.lineTo(rx1, ry0);
            gp.lineTo(rx1, ry1);
            gp.lineTo(rx0, ry1);
            gp.closePath();
            rx0 += pad;
            ry0 += pad;
            rx1 -= pad;
            ry1 -= pad;
            gp.moveTo(rx0, ry0);
            gp.lineTo(rx0, ry1);
            gp.lineTo(rx1, ry1);
            gp.lineTo(rx1, ry0);
            gp.closePath();
            return gp;
        }

        // NOTE: This is a work in progress, not used yet
        protected java.awt.Shape makeStrokedEllipse(java.awt.geom.Ellipse2D e) {
            if (stroke.getDashArray() != null) {
                return null;
            }
            float pad = stroke.getLineWidth() / 2f;
            float w = (float) e.getWidth();
            float h = (float) e.getHeight();
            if (w - 2*pad > h * 2 || h - 2*pad > w * 2) {
                // If the inner ellipse is too "squashed" then we can not
                // approximate it with just a single cubic per quadrant.
                // NOTE: measure so we can relax this restriction and
                // also consider modifying the code below to insert
                // more cubics in those cases.
                return null;
            }
            if (pad >= w || pad >= h) {
                return e;
            }
            float x0 = (float) e.getX();
            float y0 = (float) e.getY();
            float xc = x0 + w / 2f;
            float yc = y0 + h / 2f;
            float x1 = x0 + w;
            float y1 = y0 + h;
            java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
            gp.moveTo(xc, y0);
            cornerArc(gp, xc, y0, x1, y0, x1, yc);
            cornerArc(gp, x1, yc, x1, y1, xc, y1);
            cornerArc(gp, xc, y1, x0, y1, x0, yc);
            cornerArc(gp, x0, yc, x0, y0, xc, y0);
            gp.closePath();
            x0 += pad;
            y0 += pad;
            x1 -= pad;
            y1 -= pad;
            gp.moveTo(xc, y0);
            cornerArc(gp, xc, y0, x0, y0, x0, yc);
            cornerArc(gp, x0, yc, x0, y1, xc, y1);
            cornerArc(gp, xc, y1, x1, y1, x1, yc);
            cornerArc(gp, x1, yc, x1, y0, xc, y0);
            gp.closePath();
            return gp;
        }

        @Override
        protected java.awt.Shape makeStrokedShape(java.awt.Shape s) {
            java.awt.Shape ss = stroke.createStrokedShape(s);
            java.awt.geom.Area b = new java.awt.geom.Area(ss);
            b.intersect(new java.awt.geom.Area(s));
            return b;
        }
    }

    static class OuterStroke extends FilterStroke {
        static double SQRT_2 = Math.sqrt(2);

        OuterStroke(java.awt.BasicStroke stroke) {
            super(stroke);
        }

        protected java.awt.Shape makeStrokedRect(java.awt.geom.Rectangle2D r) {
            if (stroke.getDashArray() != null) {
                return null;
            }
            float pad = stroke.getLineWidth() / 2f;
            float rx0 = (float) r.getX();
            float ry0 = (float) r.getY();
            float rx1 = rx0 + (float) r.getWidth();
            float ry1 = ry0 + (float) r.getHeight();
            java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
            // clockwise
            gp.moveTo(rx0, ry0);
            gp.lineTo(rx1, ry0);
            gp.lineTo(rx1, ry1);
            gp.lineTo(rx0, ry1);
            gp.closePath();
            float ox0 = rx0 - pad;
            float oy0 = ry0 - pad;
            float ox1 = rx1 + pad;
            float oy1 = ry1 + pad;
            switch (stroke.getLineJoin()) {
            case BasicStroke.JOIN_MITER:
                // A miter limit of less than sqrt(2) bevels right angles...
                if (stroke.getMiterLimit() >= SQRT_2) {
                    // counter-clockwise
                    gp.moveTo(ox0, oy0);
                    gp.lineTo(ox0, oy1);
                    gp.lineTo(ox1, oy1);
                    gp.lineTo(ox1, oy0);
                    gp.closePath();
                    break;
                }
                // NO BREAK
            case BasicStroke.JOIN_BEVEL:
                // counter-clockwise
                gp.moveTo(ox0, ry0);
                gp.lineTo(ox0, ry1);  // left edge
                gp.lineTo(rx0, oy1);  // ll corner
                gp.lineTo(rx1, oy1);  // bottom edge
                gp.lineTo(ox1, ry1);  // lr corner
                gp.lineTo(ox1, ry0);  // right edge
                gp.lineTo(rx1, oy0);  // ur corner
                gp.lineTo(rx0, oy0);  // top edge
                gp.closePath();       // ul corner
                break;
            case BasicStroke.JOIN_ROUND:
                // counter-clockwise
                gp.moveTo(ox0, ry0);
                gp.lineTo(ox0, ry1);                          // left edge
                cornerArc(gp, ox0, ry1, ox0, oy1, rx0, oy1);  // ll corner
                gp.lineTo(rx1, oy1);                          // bottom edge
                cornerArc(gp, rx1, oy1, ox1, oy1, ox1, ry1);  // lr corner
                gp.lineTo(ox1, ry0);                          // right edge
                cornerArc(gp, ox1, ry0, ox1, oy0, rx1, oy0);  // ur corner
                gp.lineTo(rx0, oy0);                          // top edge
                cornerArc(gp, rx0, oy0, ox0, oy0, ox0, ry0);  // ul corner
                gp.closePath();
                break;
            default:
                throw new InternalError("Unrecognized line join style");
            }
            return gp;
        }

        // NOTE: This is a work in progress, not used yet
        protected java.awt.Shape makeStrokedEllipse(java.awt.geom.Ellipse2D e) {
            if (stroke.getDashArray() != null) {
                return null;
            }
            float pad = stroke.getLineWidth() / 2f;
            float w = (float) e.getWidth();
            float h = (float) e.getHeight();
            if (w > h * 2 || h > w * 2) {
                // If the inner ellipse is too "squashed" then we can not
                // approximate it with just a single cubic per quadrant.
                // NOTE: measure so we can relax this restriction and
                // also consider modifying the code below to insert
                // more cubics in those cases.
                return null;
            }
            float x0 = (float) e.getX();
            float y0 = (float) e.getY();
            float xc = x0 + w / 2f;
            float yc = y0 + h / 2f;
            float x1 = x0 + w;
            float y1 = y0 + h;
            java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
            gp.moveTo(xc, y0);
            cornerArc(gp, xc, y0, x1, y0, x1, yc);
            cornerArc(gp, x1, yc, x1, y1, xc, y1);
            cornerArc(gp, xc, y1, x0, y1, x0, yc);
            cornerArc(gp, x0, yc, x0, y0, xc, y0);
            gp.closePath();
            x0 -= pad;
            y0 -= pad;
            x1 += pad;
            y1 += pad;
            gp.moveTo(xc, y0);
            cornerArc(gp, xc, y0, x0, y0, x0, yc);
            cornerArc(gp, x0, yc, x0, y1, xc, y1);
            cornerArc(gp, xc, y1, x1, y1, x1, yc);
            cornerArc(gp, x1, yc, x1, y0, xc, y0);
            gp.closePath();
            return gp;
        }

        @Override
        protected java.awt.Shape makeStrokedShape(java.awt.Shape s) {
            java.awt.Shape ss = stroke.createStrokedShape(s);
            java.awt.geom.Area b = new java.awt.geom.Area(ss);
            b.subtract(new java.awt.geom.Area(s));
            return b;
        }
    }

    @Override
    public void setLights(NGLightBase[] lights) {
        // Light are not supported by J2d
    }

    @Override
    public NGLightBase[] getLights() {
        // Light are not supported by J2d
        return null;
    }
}
