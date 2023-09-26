/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.Ellipse2D;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NodePath;
import com.sun.pisces.GradientColorMap;
import com.sun.pisces.PiscesRenderer;
import com.sun.pisces.RendererBase;
import com.sun.pisces.Transform6;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackGraphics;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.Paint;
import com.sun.javafx.font.CharToGlyphMapper;

final class SWGraphics implements ReadbackGraphics {

    private static final BasicStroke DEFAULT_STROKE =
        new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f);
    private static final Paint DEFAULT_PAINT = Color.WHITE;

    private final PiscesRenderer pr;
    private final SWContext context;
    private final SWRTTexture target;
    private final SWPaint swPaint;

    private final BaseTransform tx = new Affine2D();

    private CompositeMode compositeMode = CompositeMode.SRC_OVER;

    private Rectangle clip;
    private final Rectangle finalClip = new Rectangle();
    private RectBounds nodeBounds;

    private int clipRectIndex;

    private Paint paint = DEFAULT_PAINT;
    private BasicStroke stroke = DEFAULT_STROKE;

    private Ellipse2D ellipse2d;
    private Line2D line2d;
    private RoundRectangle2D rect2d;

    private boolean antialiasedShape = true;
    private boolean hasPreCullingBits = false;
    private float pixelScaleX = 1.0f;
    private float pixelScaleY = 1.0f;

    private NodePath renderRoot;
    @Override
    public void setRenderRoot(NodePath root) {
        this.renderRoot = root;
    }

    @Override
    public NodePath getRenderRoot() {
        return renderRoot;
    }

    public SWGraphics(SWRTTexture target, SWContext context, PiscesRenderer pr) {
        this.target = target;
        this.context = context;
        this.pr = pr;
        this.swPaint = new SWPaint(context, pr);

        this.setClipRect(null);
    }

    @Override
    public RenderTarget getRenderTarget() {
        return target;
    }

    @Override
    public SWResourceFactory getResourceFactory() {
        return target.getResourceFactory();
    }

    @Override
    public Screen getAssociatedScreen() {
        return target.getAssociatedScreen();
    }

    @Override
    public void sync() {
    }

    @Override
    public BaseTransform getTransformNoClone() {
        if (PrismSettings.debug) {
            System.out.println("+ getTransformNoClone " + this + "; tr: " + tx);
        }
        return tx;
    }

    @Override
    public void setTransform(BaseTransform xform) {
        if (xform == null) {
            xform = BaseTransform.IDENTITY_TRANSFORM;
        }
        if (PrismSettings.debug) {
            System.out.println("+ setTransform " + this + "; tr: " + xform);
        }
        tx.setTransform(xform);
    }

    @Override
    public void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12) {
        tx.restoreTransform(m00, m10, m01, m11, m02, m12);
        if (PrismSettings.debug) {
            System.out.println("+ restoreTransform " + this + "; tr: " + tx);
        }
    }

    @Override
    public void setTransform3D(double mxx, double mxy, double mxz, double mxt,
                               double myx, double myy, double myz, double myt,
                               double mzx, double mzy, double mzz, double mzt) {
        if (mxz != 0.0 || myz != 0.0 ||
            mzx != 0.0 || mzy != 0.0 || mzz != 1.0 || mzt != 0.0)
        {
            throw new UnsupportedOperationException("3D transforms not supported.");
        }
        setTransform(mxx, myx, mxy, myy, mxt, myt);
    }

    @Override
    public void transform(BaseTransform xform) {
        if (PrismSettings.debug) {
            System.out.println("+ concatTransform " + this + "; tr: " + xform);
        }
        tx.deriveWithConcatenation(xform);
    }

    @Override
    public void translate(float tx, float ty) {
        if (PrismSettings.debug) {
            System.out.println("+ concat translate " + this + "; tx: " + tx + "; ty: " + ty);
        }
        this.tx.deriveWithTranslation(tx, ty);
    }

    @Override
    public void translate(float tx, float ty, float tz) {
        throw new UnsupportedOperationException("translate3D: unimp");
    }

    @Override
    public void scale(float sx, float sy) {
        if (PrismSettings.debug) {
            System.out.println("+ concat scale " + this + "; sx: " + sx + "; sy: " + sy);
        }
        tx.deriveWithConcatenation(sx, 0, 0, sy, 0, 0);
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        throw new UnsupportedOperationException("scale3D: unimp");
    }

    @Override
    public void setCamera(NGCamera camera) {
    }

    @Override
    public void setPerspectiveTransform(GeneralTransform3D transform) {
    }

    @Override
    public NGCamera getCameraNoClone() {
        throw new UnsupportedOperationException("getCameraNoClone: unimp");
    }

    @Override
    public void setDepthTest(boolean depthTest) { }

    @Override
    public boolean isDepthTest() {
        return false;
    }

    @Override
    public void setDepthBuffer(boolean depthBuffer) { }

    @Override
    public boolean isDepthBuffer() {
        return false;
    }

    @Override
    public boolean isAlphaTestShader() {
        if (PrismSettings.verbose && PrismSettings.forceAlphaTestShader) {
            System.out.println("SW pipe doesn't support shader with alpha testing");
        }
        return false;
    }

    @Override
    public void setAntialiasedShape(boolean aa) {
        antialiasedShape = aa;
    }

    @Override
    public boolean isAntialiasedShape() {
        return antialiasedShape;
    }

    @Override
    public Rectangle getClipRect() {
        return (clip == null) ? null : new Rectangle(clip);
    }

    @Override
    public Rectangle getClipRectNoClone() {
        return clip;
    }

    @Override
    public RectBounds getFinalClipNoClone() {
        return finalClip.toRectBounds();
    }

    @Override
    public void setClipRect(Rectangle clipRect) {
        finalClip.setBounds(target.getDimensions());
        if (clipRect == null) {
            if (PrismSettings.debug) {
                System.out.println("+ PR.resetClip");
            }
            clip = null;
        } else {
            if (PrismSettings.debug) {
                System.out.println("+ PR.setClip: " + clipRect);
            }
            finalClip.intersectWith(clipRect);
            clip = new Rectangle(clipRect);
        }
        pr.setClip(finalClip.x, finalClip.y, finalClip.width, finalClip.height);
    }

    @Override
    public void setHasPreCullingBits(boolean hasBits) {
        this.hasPreCullingBits = hasBits;
    }

    @Override
    public boolean hasPreCullingBits() {
        return this.hasPreCullingBits;
    }

    @Override
    public int getClipRectIndex() {
        return clipRectIndex;
    }

    @Override
    public void setClipRectIndex(int index) {
        if (PrismSettings.debug) {
            System.out.println("+ PR.setClipRectIndex: " + index);
        }
        clipRectIndex = index;
    }

    @Override
    public float getExtraAlpha() {
        return swPaint.getCompositeAlpha();
    }

    @Override
    public void setExtraAlpha(float extraAlpha) {
        if (PrismSettings.debug) {
            System.out.println("PR.setCompositeAlpha, value: " + extraAlpha);
        }
        swPaint.setCompositeAlpha(extraAlpha);
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
    }



    @Override
    public BasicStroke getStroke() {
        return stroke;
    }

    @Override
    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
    }

    @Override
    public CompositeMode getCompositeMode() {
        return compositeMode;
    }

    @Override
    public void setCompositeMode(CompositeMode mode) {
        this.compositeMode = mode;

        int piscesComp;
        switch (mode) {
            case CLEAR:
                piscesComp = RendererBase.COMPOSITE_CLEAR;
                if (PrismSettings.debug) {
                    System.out.println("PR.setCompositeRule - CLEAR");
                }
                break;
            case SRC:
                piscesComp = RendererBase.COMPOSITE_SRC;
                if (PrismSettings.debug) {
                    System.out.println("PR.setCompositeRule - SRC");
                }
                break;
            case SRC_OVER:
                piscesComp = RendererBase.COMPOSITE_SRC_OVER;
                if (PrismSettings.debug) {
                    System.out.println("PR.setCompositeRule - SRC_OVER");
                }
                break;
            default:
                throw new InternalError("Unrecognized composite mode: "+mode);
        }
        this.pr.setCompositeRule(piscesComp);
    }

    @Override
    public void setNodeBounds(RectBounds bounds) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.setNodeBounds: " + bounds);
        }
        nodeBounds = bounds;
    }

    @Override
    public void clear() {
        this.clear(Color.TRANSPARENT);
    }

    /**
     * Clears the current {@code RenderTarget} with the given {@code Color}.
     * Note that this operation is affected by the current clip rectangle,
     * if set.  To clear the entire surface, call {@code setClipRect(null)}
     * prior to calling {@code clear()}.
     */
    @Override
    public void clear(Color color) {
        if (PrismSettings.debug) {
            System.out.println("+ PR.clear: " + color);
        }
        this.swPaint.setColor(color, 1f);
        pr.clearRect(0, 0, target.getPhysicalWidth(), target.getPhysicalHeight());
        getRenderTarget().setOpaque(color.isOpaque());
    }

    /**
     * Clears the region represented by the given quad with transparent pixels.
     * Note that this operation is affected by the current clip rectangle,
     * if set, as well as the current transform (the quad is specified in
     * user space).  Also note that unlike the {@code clear()} methods, this
     * method does not attempt to clear the depth buffer.
     */
    @Override
    public void clearQuad(float x1, float y1, float x2, float y2) {
        final CompositeMode cm = this.compositeMode;
        final Paint p = this.paint;
        this.setCompositeMode(CompositeMode.SRC);
        this.setPaint(Color.TRANSPARENT);
        this.fillQuad(x1, y1, x2, y2);
        this.setCompositeMode(cm);
        this.setPaint(p);
    }

    @Override
    public void fill(Shape shape) {
        if (PrismSettings.debug) {
            System.out.println("+ fill(Shape)");
        }
        paintShape(shape, null, this.tx);
    }

    @Override
    public void fillQuad(float x1, float y1, float x2, float y2) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.fillQuad");
        }
        this.fillRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.printf("+ SWG.fillRect, x: %f, y: %f, w: %f, h: %f\n", x, y, width, height);
        }
        if (tx.getMxy() == 0 && tx.getMyx() == 0) {
            if (PrismSettings.debug) {
                System.out.println("GR: " + this);
                System.out.println("target: " + target + " t.w: " + target.getPhysicalWidth() + ", t.h: " + target.getPhysicalHeight() +
                        ", t.dims: " + target.getDimensions());
                System.out.println("Tx: " + tx);
                System.out.println("Clip: " + finalClip);
                System.out.println("Composite rule: " + compositeMode);
            }

            final Point2D p1 = new Point2D(x, y);
            final Point2D p2 = new Point2D(x + width, y + height);
            tx.transform(p1, p1);
            tx.transform(p2, p2);

            if (this.paint.getType() == Paint.Type.IMAGE_PATTERN) {
                // we can call pr.drawImage(...) directly
                final ImagePattern ip = (ImagePattern)this.paint;
                if (ip.getImage().getPixelFormat() == PixelFormat.BYTE_ALPHA) {
                    throw new UnsupportedOperationException("Alpha image is not supported as an image pattern.");
                } else {
                    final Transform6 piscesTx = swPaint.computeSetTexturePaintTransform(this.paint, this.tx, this.nodeBounds, x, y, width, height);
                    final SWArgbPreTexture tex = context.validateImagePaintTexture(ip.getImage().getWidth(), ip.getImage().getHeight());
                    tex.update(ip.getImage());

                    final float compositeAlpha = swPaint.getCompositeAlpha();
                    final int imageMode;
                    if (compositeAlpha == 1f) {
                        imageMode = RendererBase.IMAGE_MODE_NORMAL;
                    } else {
                        imageMode = RendererBase.IMAGE_MODE_MULTIPLY;
                        this.pr.setColor(255, 255, 255, (int)(255 * compositeAlpha));
                    }

                    this.pr.drawImage(RendererBase.TYPE_INT_ARGB_PRE, imageMode,
                            tex.getDataNoClone(), tex.getContentWidth(), tex.getContentHeight(),
                            tex.getOffset(), tex.getPhysicalWidth(),
                            piscesTx,
                            tex.getWrapMode() == Texture.WrapMode.REPEAT,
                            tex.getLinearFiltering(),
                            (int)(Math.min(p1.x, p2.x) * SWUtils.TO_PISCES), (int)(Math.min(p1.y, p2.y) * SWUtils.TO_PISCES),
                            (int)(Math.abs(p2.x - p1.x) * SWUtils.TO_PISCES), (int)(Math.abs(p2.y - p1.y) * SWUtils.TO_PISCES),
                            RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                            RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                            0, 0, tex.getContentWidth()-1, tex.getContentHeight()-1,
                            tex.hasAlpha());
                }
            } else {
                swPaint.setPaintFromShape(this.paint, this.tx, null, this.nodeBounds, x, y, width, height);
                this.pr.fillRect((int)(Math.min(p1.x, p2.x) * SWUtils.TO_PISCES), (int)(Math.min(p1.y, p2.y) * SWUtils.TO_PISCES),
                        (int)(Math.abs(p2.x - p1.x) * SWUtils.TO_PISCES), (int)(Math.abs(p2.y - p1.y) * SWUtils.TO_PISCES));
            }
        } else {
            this.fillRoundRect(x, y, width, height, 0, 0);
        }
    }

    @Override
    public void fillRoundRect(float x, float y, float width, float height,
                              float arcw, float arch) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.fillRoundRect");
        }
        this.paintRoundRect(x, y, width, height, arcw, arch, null);
    }

    @Override
    public void fillEllipse(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.fillEllipse");
        }
        this.paintEllipse(x, y, width, height, null);
    }

    @Override
    public void draw(Shape shape) {
        if (PrismSettings.debug) {
            System.out.println("+ draw(Shape)");
        }
        paintShape(shape, this.stroke, this.tx);
    }

    private void paintShape(Shape shape, BasicStroke st, BaseTransform tr) {
        if (this.finalClip.isEmpty()) {
            if (PrismSettings.debug) {
                System.out.println("Final clip is empty: not rendering the shape: " + shape);
            }
            return;
        }
        swPaint.setPaintFromShape(this.paint, this.tx, shape, this.nodeBounds, 0,0,0,0);
        this.paintShapePaintAlreadySet(shape, st, tr);
    }

    private void paintShapePaintAlreadySet(Shape shape, BasicStroke st, BaseTransform tr) {
        if (this.finalClip.isEmpty()) {
            if (PrismSettings.debug) {
                System.out.println("Final clip is empty: not rendering the shape: " + shape);
            }
            return;
        }

        if (PrismSettings.debug) {
            System.out.println("GR: " + this);
            System.out.println("target: " + target + " t.w: " + target.getPhysicalWidth() + ", t.h: " + target.getPhysicalHeight() +
                    ", t.dims: " + target.getDimensions());
            System.out.println("Shape: " + shape);
            System.out.println("Stroke: " + st);
            System.out.println("Tx: " + tr);
            System.out.println("Clip: " + finalClip);
            System.out.println("Composite rule: " + compositeMode);
        }
        context.renderShape(this.pr, shape, st, tr, this.finalClip, isAntialiasedShape());
    }

    private void paintRoundRect(float x, float y, float width, float height, float arcw, float arch, BasicStroke st) {
        if (rect2d == null) {
            rect2d = new RoundRectangle2D(x, y, width, height, arcw, arch);
        } else {
            rect2d.setRoundRect(x, y, width, height, arcw, arch);
        }
        paintShape(this.rect2d, st, this.tx);
    }

    private void paintEllipse(float x, float y, float width, float height, BasicStroke st) {
        if (ellipse2d == null) {
            ellipse2d = new Ellipse2D(x, y, width, height);
        } else {
            ellipse2d.setFrame(x, y, width, height);
        }
        paintShape(this.ellipse2d, st, this.tx);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        if (PrismSettings.debug) {
            System.out.println("+ drawLine");
        }
        if (line2d == null) {
            line2d = new Line2D(x1, y1, x2, y2);
        } else {
            line2d.setLine(x1, y1, x2, y2);
        }
        paintShape(this.line2d, this.stroke, this.tx);
    }

    @Override
    public void drawRect(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawRect");
        }
        this.drawRoundRect(x, y, width, height, 0, 0);
    }

    @Override
    public void drawRoundRect(float x, float y, float width, float height,
                              float arcw, float arch) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawRoundRect");
        }
        this.paintRoundRect(x, y, width, height, arcw, arch, stroke);
    }

    @Override
    public void drawEllipse(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawEllipse");
        }
        this.paintEllipse(x, y, width, height, stroke);
    }

    @Override
    public void drawString(GlyphList gl, FontStrike strike, float x, float y,
                           Color selectColor, int selectStart, int selectEnd) {

        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawGlyphList, gl.Count: " + gl.getGlyphCount() +
                    ", x: " + x + ", y: " + y +
                    ", selectStart: " + selectStart + ", selectEnd: " + selectEnd);
        }

        if (strike.getFontResource().isColorGlyph(gl.getGlyphCode(0))) {
            drawColorGlyph(gl, strike, x, y, selectColor, selectStart, selectEnd);
            return;
        }

        final float bx, by, bw, bh;
        if (paint.isProportional()) {
            if (nodeBounds != null) {
                bx = nodeBounds.getMinX();
                by = nodeBounds.getMinY();
                bw = nodeBounds.getWidth();
                bh = nodeBounds.getHeight();
            } else {
                Metrics m = strike.getMetrics();
                bx = 0;
                by = m.getAscent();
                bw = gl.getWidth();
                bh = m.getLineHeight();
            }
        } else {
            bx = by = bw = bh = 0;
        }

        final boolean drawAsMasks = tx.isTranslateOrIdentity() && (!strike.drawAsShapes());
        final boolean doLCDText = drawAsMasks &&
                (strike.getAAMode() == FontResource.AA_LCD) &&
                getRenderTarget().isOpaque() &&
                (this.paint.getType() == Paint.Type.COLOR) &&
                tx.is2D();
        BaseTransform glyphTx = null;

        if (doLCDText) {
            this.pr.setLCDGammaCorrection(1f / PrismFontFactory.getLCDContrast());
        } else if (drawAsMasks) {
            final FontResource fr = strike.getFontResource();
            final float origSize = strike.getSize();
            final BaseTransform origTx = strike.getTransform();
            strike = fr.getStrike(origSize, origTx, FontResource.AA_GREYSCALE);
        } else {
            glyphTx = new Affine2D();
        }

        if (selectColor == null) {
            swPaint.setPaintBeforeDraw(this.paint, this.tx, bx, by, bw, bh);
            for (int i = 0; i < gl.getGlyphCount(); i++) {
                this.drawGlyph(strike, gl, i, glyphTx, drawAsMasks, x, y);
            }
        } else {
            for (int i = 0; i < gl.getGlyphCount(); i++) {
                final int offset = gl.getCharOffset(i);
                final boolean selected = selectStart <= offset && offset < selectEnd;
                swPaint.setPaintBeforeDraw(selected ? selectColor : this.paint, this.tx, bx, by, bw, bh);
                this.drawGlyph(strike, gl, i, glyphTx, drawAsMasks, x, y);
            }
        }
    }

    private void drawGlyph(FontStrike strike, GlyphList gl, int idx, BaseTransform glyphTx,
                           boolean drawAsMasks, float x, float y)
    {
        final Glyph g = strike.getGlyph(gl.getGlyphCode(idx));
        if (g.getGlyphCode() == CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
            return;
        }
        if (drawAsMasks) {
            final Point2D pt = new Point2D((float)(x + tx.getMxt() + gl.getPosX(idx)),
                                           (float)(y + tx.getMyt() + gl.getPosY(idx)));
            int subPixel = strike.getQuantizedPosition(pt);
            final byte pixelData[] = g.getPixelData(subPixel);
            if (pixelData != null) {
                final int intPosX = g.getOriginX() + (int)pt.x;
                final int intPosY = g.getOriginY() + (int)pt.y;
                if (g.isLCDGlyph()) {
                    this.pr.fillLCDAlphaMask(pixelData, intPosX, intPosY,
                            g.getWidth(), g.getHeight(),
                            0, g.getWidth());
                } else {
                    this.pr.fillAlphaMask(pixelData, intPosX, intPosY,
                            g.getWidth(), g.getHeight(),
                            0, g.getWidth());
                }
            }
        } else {
            Shape shape = g.getShape();
            if (shape != null) {
                glyphTx.setTransform(tx);
                glyphTx.deriveWithTranslation(x + gl.getPosX(idx), y + gl.getPosY(idx));
                this.paintShapePaintAlreadySet(shape, null, glyphTx);
            }
        }
    }

    @Override
    public void drawTexture(Texture tex, float x, float y, float w, float h) {
        if (PrismSettings.debug) {
            System.out.printf("+ drawTexture1, x: %f, y: %f, w: %f, h: %f\n", x, y, w, h);
        }
        this.drawTexture(tex, x, y, x + w, y + h, 0, 0, w, h);
    }

    @Override
    public void drawTexture(Texture tex,
                            float dx1, float dy1, float dx2, float dy2,
                            float sx1, float sy1, float sx2, float sy2)
    {
        this.drawTexture(tex, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP);
    }

    private void drawTexture(Texture tex,
                             float dx1, float dy1, float dx2, float dy2,
                             float sx1, float sy1, float sx2, float sy2,
                             int lEdge, int rEdge, int tEdge, int bEdge) {
        final int imageMode;
        final float compositeAlpha = swPaint.getCompositeAlpha();
        if (compositeAlpha == 1f) {
            imageMode = RendererBase.IMAGE_MODE_NORMAL;
        } else {
            imageMode = RendererBase.IMAGE_MODE_MULTIPLY;
            this.pr.setColor(255, 255, 255, (int)(255 * compositeAlpha));
        }
        this.drawTexture(tex, imageMode, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, lEdge, rEdge, tEdge, bEdge);
    }

    private void drawTexture(Texture tex, int imageMode,
                            float dx1, float dy1, float dx2, float dy2,
                            float sx1, float sy1, float sx2, float sy2,
                            int lEdge, int rEdge, int tEdge, int bEdge) {
        if (PrismSettings.debug) {
            System.out.println("+ drawTexture: " + tex + ", imageMode: " + imageMode +
                    ", tex.w: " + tex.getPhysicalWidth() + ", tex.h: " + tex.getPhysicalHeight() +
                    ", tex.cw: " + tex.getContentWidth() + ", tex.ch: " + tex.getContentHeight());
            System.out.println("target: " + target + " t.w: " + target.getPhysicalWidth() + ", t.h: " + target.getPhysicalHeight() +
                    ", t.dims: " + target.getDimensions());
            System.out.println("GR: " + this);
            System.out.println("dx1:" + dx1 + " dy1:" + dy1 + " dx2:" + dx2 + " dy2:" + dy2);
            System.out.println("sx1:" + sx1 + " sy1:" + sy1 + " sx2:" + sx2 + " sy2:" + sy2);
            System.out.println("Clip: " + finalClip);
            System.out.println("Composite rule: " + compositeMode);
        }

        final SWArgbPreTexture swTex = (SWArgbPreTexture) tex;
        int data[] = swTex.getDataNoClone();

        final RectBounds srcBBox = new RectBounds(Math.min(dx1, dx2), Math.min(dy1, dy2),
                Math.max(dx1, dx2), Math.max(dy1, dy2));
        final RectBounds dstBBox = new RectBounds();
        tx.transform(srcBBox, dstBBox);

        final Transform6 piscesTx = swPaint.computeDrawTexturePaintTransform(this.tx,
                dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);

        if (PrismSettings.debug) {
            System.out.println("tx: " + tx);
            System.out.println("piscesTx: " + piscesTx);

            System.out.println("srcBBox: " + srcBBox);
            System.out.println("dstBBox: " + dstBBox);
        }

        // texture coordinates range
        final int txMin = Math.max(0, SWUtils.fastFloor(Math.min(sx1, sx2)));
        final int tyMin = Math.max(0, SWUtils.fastFloor(Math.min(sy1, sy2)));
        final int txMax = Math.min(tex.getContentWidth() - 1, SWUtils.fastCeil(Math.max(sx1, sx2)) - 1);
        final int tyMax = Math.min(tex.getContentHeight() - 1, SWUtils.fastCeil(Math.max(sy1, sy2)) - 1);

        this.pr.drawImage(RendererBase.TYPE_INT_ARGB_PRE, imageMode,
                data, tex.getContentWidth(), tex.getContentHeight(),
                swTex.getOffset(), tex.getPhysicalWidth(),
                piscesTx,
                tex.getWrapMode() == Texture.WrapMode.REPEAT,
                tex.getLinearFiltering(),
                (int)(SWUtils.TO_PISCES * dstBBox.getMinX()), (int)(SWUtils.TO_PISCES * dstBBox.getMinY()),
                (int)(SWUtils.TO_PISCES * dstBBox.getWidth()), (int)(SWUtils.TO_PISCES * dstBBox.getHeight()),
                lEdge, rEdge, tEdge, bEdge,
                txMin, tyMin, txMax, tyMax,
                swTex.hasAlpha());

        if (PrismSettings.debug) {
            System.out.println("* drawTexture, DONE");
        }
    }

    @Override
    public void drawTexture3SliceH(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dh1, float dh2, float sh1, float sh2)
    {
        drawTexture(tex, dx1, dy1, dh1, dy2, sx1, sy1, sh1, sy2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP);
        drawTexture(tex, dh1, dy1, dh2, dy2, sh1, sy1, sh2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP);
        drawTexture(tex, dh2, dy1, dx2, dy2, sh2, sy1, sx2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP);
    }

    @Override
    public void drawTexture3SliceV(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dv1, float dv2, float sv1, float sv2)
    {
        drawTexture(tex, dx1, dy1, dx2, dv1, sx1, sy1, sx2, sv1,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD);
        drawTexture(tex, dx1, dv1, dx2, dv2, sx1, sv1, sx2, sv2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD);
        drawTexture(tex, dx1, dv2, dx2, dy2, sx1, sv2, sx2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP);
    }

    @Override
    public void drawTexture9Slice(Texture tex,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float sx1, float sy1, float sx2, float sy2,
                                  float dh1, float dv1, float dh2, float dv2,
                                  float sh1, float sv1, float sh2, float sv2)
    {
        drawTexture(tex, dx1, dy1, dh1, dv1, sx1, sy1, sh1, sv1,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD);
        drawTexture(tex, dh1, dy1, dh2, dv1, sh1, sy1, sh2, sv1,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD);
        drawTexture(tex, dh2, dy1, dx2, dv1, sh2, sy1, sx2, sv1,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD);

        drawTexture(tex, dx1, dv1, dh1, dv2, sx1, sv1, sh1, sv2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD);
        drawTexture(tex, dh1, dv1, dh2, dv2, sh1, sv1, sh2, sv2,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD);
        drawTexture(tex, dh2, dv1, dx2, dv2, sh2, sv1, sx2, sv2,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD);

        drawTexture(tex, dx1, dv2, dh1, dy2, sx1, sv2, sh1, sy2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP);
        drawTexture(tex, dh1, dv2, dh2, dy2, sh1, sv2, sh2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_PAD,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP);
        drawTexture(tex, dh2, dv2, dx2, dy2, sh2, sv2, sx2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP);
    }

    @Override
    public void drawTextureVO(Texture tex,
                              float topopacity, float botopacity,
                              float dx1, float dy1, float dx2, float dy2,
                              float sx1, float sy1, float sx2, float sy2)
    {
        if (PrismSettings.debug) {
            System.out.println("* drawTextureVO");
        }
        final int[] fractions = { 0x0000, 0x10000 };
        final int[] argb = { 0xffffff | (((int)(topopacity * 255)) << 24),
                             0xffffff | (((int)(botopacity * 255)) << 24) };
        final Transform6 t6 = new Transform6();
        SWUtils.convertToPiscesTransform(this.tx, t6);
        this.pr.setLinearGradient(0, (int)(SWUtils.TO_PISCES * dy1), 0, (int)(SWUtils.TO_PISCES * dy2), fractions, argb,
                                  GradientColorMap.CYCLE_NONE, t6);
        this.drawTexture(tex, RendererBase.IMAGE_MODE_MULTIPLY, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP);
    }

    @Override
    public void drawTextureRaw(Texture tex,
                               float dx1, float dy1, float dx2, float dy2,
                               float tx1, float ty1, float tx2, float ty2)
    {
        if (PrismSettings.debug) {
            System.out.println("+ drawTextureRaw");
        }

        int w = tex.getContentWidth();
        int h = tex.getContentHeight();
        tx1 *= w;
        ty1 *= h;
        tx2 *= w;
        ty2 *= h;
        drawTexture(tex, dx1, dy1, dx2, dy2, tx1, ty1, tx2, ty2);
    }

    @Override
    public void drawMappedTextureRaw(Texture tex,
                                     float dx1, float dy1, float dx2, float dy2,
                                     float tx11, float ty11, float tx21, float ty21,
                                     float tx12, float ty12, float tx22, float ty22)
    {
        if (PrismSettings.debug) {
            System.out.println("+ drawMappedTextureRaw");
        }

        final double _mxx, _myx, _mxy, _myy, _mxt, _myt;
        _mxx = tx.getMxx();
        _myx = tx.getMyx();
        _mxy = tx.getMxy();
        _myy = tx.getMyy();
        _mxt = tx.getMxt();
        _myt = tx.getMyt();

        try {
            final float mxx = tx21-tx11;
            final float myx = ty21-ty11;
            final float mxy = tx12-tx11;
            final float myy = ty12-ty11;

            final BaseTransform tmpTx = new Affine2D(mxx, myx, mxy, myy, tx11, ty11);
            tmpTx.invert();

            tx.setToIdentity();
            tx.deriveWithTranslation(dx1, dy1);
            tx.deriveWithConcatenation(dx2 - dx1, 0, 0, dy2 - dy2, 0, 0);
            tx.deriveWithConcatenation(tmpTx);
            this.drawTexture(tex, 0, 0, 1, 1, 0, 0, tex.getContentWidth(), tex.getContentHeight());
        } catch (NoninvertibleTransformException e) { }

        tx.restoreTransform(_mxx, _myx, _mxy, _myy, _mxt, _myt);
    }

    @Override
    public boolean canReadBack() {
        return true;
    }

    @Override
    public RTTexture readBack(Rectangle view) {
        if (PrismSettings.debug) {
            System.out.println("+ readBack, rect: " + view + ", target.dims: " + target.getDimensions());
        }

        final int w = Math.max(1, view.width);
        final int h = Math.max(1, view.height);
        final SWRTTexture rbb = context.validateRBBuffer(w, h);

        if (view.isEmpty()) {
            return rbb;
        }

        final int pixels[] = rbb.getDataNoClone();
        this.target.getSurface().getRGB(pixels, 0, rbb.getPhysicalWidth(), view.x, view.y, w, h);
        return rbb;
    }

    @Override
    public void releaseReadBackBuffer(RTTexture view) {
    }

    @Override
    public void setState3D(boolean flag) {
    }

    @Override
    public boolean isState3D() {
        return false;
    }

    @Override
    public void setup3DRendering() {
    }

    @Override
    public void setPixelScaleFactors(float pixelScaleX, float pixelScaleY) {
        this.pixelScaleX = pixelScaleX;
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
    public void setLights(NGLightBase[] lights) {
        // Light are not supported by SW pipeline
    }

    @Override
    public NGLightBase[] getLights() {
        // Light are not supported by SW pipeline
        return null;
    }

    @Override
    public void blit(RTTexture srcTex, RTTexture dstTex,
                    int srcX0, int srcY0, int srcX1, int srcY1,
                    int dstX0, int dstY0, int dstX1, int dstY1) {
        Graphics g = dstTex.createGraphics();
        g.drawTexture(srcTex,
                      dstX0, dstY0, dstX1, dstY1,
                      srcX0, srcY0, srcX1, srcY1);
    }
}
