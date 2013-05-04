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

package com.sun.prism.sw;

import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PrismFontUtils;
import com.sun.javafx.scene.text.GlyphList;
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
import com.sun.pisces.GradientColorMap;
import com.sun.pisces.PiscesRenderer;
import com.sun.pisces.RendererBase;
import com.sun.pisces.Transform6;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.ReadbackGraphics;
import com.sun.prism.RenderTarget;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.paint.Stop;

final class SWGraphics implements ReadbackGraphics {

    private static final int TO_PISCES = 65536;

    private final PiscesRenderer pr;
    private final SWContext context;
    private final SWRTTexture target;

    private final BaseTransform tx = new Affine2D();
    private final BaseTransform paintTx = new Affine2D();

    private final Transform6 piscesTx = new Transform6();

    private CompositeMode compositeMode = CompositeMode.SRC_OVER;
    private float compositeAlpha = 1.0f;

    private Rectangle clip;
    private final Rectangle finalClip = new Rectangle();
    private RectBounds nodeBounds;

    private int clipRectIndex;

    private Paint paint;
    private BasicStroke stroke;

    private Ellipse2D ellipse2d;
    private Line2D line2d;
    private RoundRectangle2D rect2d;

    private boolean hasPreCullingBits = false;

    private Object renderRoot;
    @Override
    public void setRenderRoot(Object root) {
        this.renderRoot = root;
    }

    @Override
    public Object getRenderRoot() {
        return renderRoot;
    }

    public SWGraphics(SWRTTexture target, SWContext context, PiscesRenderer pr) {
        this.target = target;
        this.context = context;
        this.pr = pr;

        this.setClipRect(null);
    }

    public RenderTarget getRenderTarget() {
        return target;
    }

    public SWResourceFactory getResourceFactory() {
        return target.getResourceFactory();
    }

    public Screen getAssociatedScreen() {
        return target.getAssociatedScreen();
    }

    public void sync() {
    }

    public void reset() {
        throw new UnsupportedOperationException("unimp: SWG.reset");
    }

    private static void convertToPiscesTransform(BaseTransform prismTx, Transform6 piscesTx) {
        piscesTx.m00 = (int) (TO_PISCES * prismTx.getMxx());
        piscesTx.m10 = (int) (TO_PISCES * prismTx.getMyx());
        piscesTx.m01 = (int) (TO_PISCES * prismTx.getMxy());
        piscesTx.m11 = (int) (TO_PISCES * prismTx.getMyy());
        piscesTx.m02 = (int) (TO_PISCES * prismTx.getMxt());
        piscesTx.m12 = (int) (TO_PISCES * prismTx.getMyt());
    }

    public BaseTransform getTransformNoClone() {
        if (PrismSettings.debug) {
            System.out.println("+ getTransformNoClone " + this + "; tr: " + tx);
        }
        return tx;
    }

    public void setTransform(BaseTransform xform) {
        if (xform == null) {
            xform = BaseTransform.IDENTITY_TRANSFORM;
        }
        if (PrismSettings.debug) {
            System.out.println("+ setTransform " + this + "; tr: " + xform);
        }
        tx.setTransform(xform);
    }

    public void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12) {
        tx.restoreTransform(m00, m10, m01, m11, m02, m12);
        if (PrismSettings.debug) {
            System.out.println("+ restoreTransform " + this + "; tr: " + tx);
        }
    }

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

    public void transform(BaseTransform xform) {
        if (PrismSettings.debug) {
            System.out.println("+ concatTransform " + this + "; tr: " + xform);
        }
        tx.deriveWithConcatenation(xform);
    }

    public void translate(float tx, float ty) {
        if (PrismSettings.debug) {
            System.out.println("+ concat translate " + this + "; tx: " + tx + "; ty: " + ty);
        }
        this.tx.deriveWithTranslation(tx, ty);
    }

    public void translate(float tx, float ty, float tz) {
        throw new UnsupportedOperationException("translate3D: unimp");
    }

    public void scale(float sx, float sy) {
        if (PrismSettings.debug) {
            System.out.println("+ concat scale " + this + "; sx: " + sx + "; sy: " + sy);
        }
        tx.deriveWithConcatenation(sx, 0, 0, sy, 0, 0);
    }

    public void scale(float sx, float sy, float sz) {
        throw new UnsupportedOperationException("scale3D: unimp");
    }

    public void setWindowProjViewTx(GeneralTransform3D pvTx) {
        throw new UnsupportedOperationException("setWindowProjViewTx: unimp");
    }

    public GeneralTransform3D getWindowProjViewTxNoClone() {
        throw new UnsupportedOperationException("getWindowProjViewTxNoClone: unimp");
    }

    public boolean hasOrthoCamera() {
        throw new UnsupportedOperationException("hasOrthoCamera: unimp");
    }

    public void setCamera(PrismCameraImpl camera) {
    }

    public PrismCameraImpl getCameraNoClone() {
        throw new UnsupportedOperationException("getCameraNoClone: unimp");
    }

    public void setDepthTest(boolean depthTest) { }

    public boolean isDepthTest() {
        return false;
    }

    public void setDepthBuffer(boolean depthBuffer) { }

    public boolean isDepthBuffer() {
        return false;
    }

    public Rectangle getClipRect() {
        return (clip == null) ? null : new Rectangle(clip);
    }

    public Rectangle getClipRectNoClone() {
        return clip;
    }

    public RectBounds getFinalClipNoClone() {
        return finalClip.toRectBounds();
    }

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

    public void setHasPreCullingBits(boolean hasBits) {
        this.hasPreCullingBits = hasBits;
    }

    public boolean hasPreCullingBits() {
        return this.hasPreCullingBits;
    }

    public int getClipRectIndex() {
        return clipRectIndex;
    }

    public void setClipRectIndex(int index) {
        if (PrismSettings.debug) {
            System.out.println("+ PR.setClipRectIndex: " + index);
        }
        clipRectIndex = index;
    }

    public float getExtraAlpha() {
        return compositeAlpha;
    }

    public void setExtraAlpha(float extraAlpha) {
        if (PrismSettings.debug) {
            System.out.println("PR.setCompositeAlpha, value: " + extraAlpha);
        }
        this.compositeAlpha = extraAlpha;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    private void setColor(Color c, float compositeAlpha) {
        if (PrismSettings.debug) {
            System.out.println("PR.setColor: " + c);
        }
        this.pr.setColor((int)(c.getRed() * 255),
                (int)(255 * c.getGreen()),
                (int)(255 * c.getBlue()),
                (int)(255 * c.getAlpha() * compositeAlpha));
    }

    private void setPaintBeforeDraw(float x, float y, float width, float height) {
        this.setPaintBeforeDraw(this.paint, x, y, width, height);
    }

    private void setPaintBeforeDraw(Paint p, float x, float y, float width, float height) {
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
                if (lg.isProportional()) {
                    paintTx.deriveWithConcatenation(width, 0, 0, height, x, y);
                }
                convertToPiscesTransform(paintTx, piscesTx);
                this.pr.setLinearGradient((int)(TO_PISCES * lg.getX1()), (int)(TO_PISCES * lg.getY1()), (int)(TO_PISCES * lg.getX2()), (int)(TO_PISCES * lg.getY2()),
                    getFractions(lg), getARGB(lg, this.compositeAlpha), getPiscesGradientCycleMethod(lg.getSpreadMethod()), piscesTx);
                break;
            case RADIAL_GRADIENT:
                final RadialGradient rg = (RadialGradient)p;
                if (PrismSettings.debug) {
                    System.out.println("PR.setRadialGradient: " + rg.getCenterX() + ", " + rg.getCenterY() + ", " + rg.getFocusAngle() + ", " + rg.getFocusDistance() + ", " + rg.getRadius());
                }

                paintTx.setTransform(tx);
                if (rg.isProportional()) {
                    paintTx.deriveWithConcatenation(width, 0, 0, height, x, y);
                }
                convertToPiscesTransform(paintTx, piscesTx);
                final float fx = (float)(rg.getCenterX() + rg.getFocusDistance() * rg.getRadius() * Math.cos(Math.toRadians(rg.getFocusAngle())));
                final float fy = (float)(rg.getCenterY() + rg.getFocusDistance() * rg.getRadius() * Math.sin(Math.toRadians(rg.getFocusAngle())));
                this.pr.setRadialGradient((int) (TO_PISCES * rg.getCenterX()), (int) (TO_PISCES * rg.getCenterY()), (int) (TO_PISCES * fx), (int) (TO_PISCES * fy), (int) (TO_PISCES * rg.getRadius()),
                        getFractions(rg), getARGB(rg, this.compositeAlpha), getPiscesGradientCycleMethod(rg.getSpreadMethod()), piscesTx);
                break;
            case IMAGE_PATTERN:
                final ImagePattern ip = (ImagePattern)p;
                final Image image = ip.getImage();
                if (PrismSettings.debug) {
                    System.out.println("PR.setTexturePaint: " + image);
                    System.out.println("imagePattern: x: " + ip.getX() + ", y: " + ip.getY() +
                            ", w: " + ip.getWidth() + ", h: " + ip.getHeight() + ", proportional: " + ip.isProportional());
                }

                paintTx.setTransform(tx);
                if (ip.isProportional()) {
                    paintTx.deriveWithConcatenation(width / image.getWidth() * ip.getWidth(), 0,
                            0, height / image.getHeight() * ip.getHeight(),
                            x + width * ip.getX(), y + height * ip.getY());
                } else {
                    paintTx.deriveWithConcatenation(ip.getWidth() / image.getWidth(), 0,
                            0, ip.getHeight() / image.getHeight(),
                            x + ip.getX(), y + ip.getY());
                }
                convertToPiscesTransform(paintTx, piscesTx);
                if (ip.getImage().getPixelFormat() == PixelFormat.BYTE_ALPHA) {
                    throw new UnsupportedOperationException("Alpha image is not supported as an image pattern.");
                } else {
                    SWArgbPreTexture tex = (SWArgbPreTexture)getResourceFactory().createTexture(ip.getImage(),
                                                Texture.Usage.DEFAULT,
                                                Texture.WrapMode.REPEAT);
                    if (this.compositeAlpha < 1.0f) {
                        tex.applyCompositeAlpha(this.compositeAlpha);
                    }
                    this.pr.setTexture(RendererBase.TYPE_INT_ARGB_PRE, tex.getDataNoClone(),
                            tex.getPhysicalWidth(), tex.getPhysicalHeight(),
                            piscesTx, true, tex.hasAlpha());
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
            argb[i] = ((((int)(255 * stopColor.getAlpha() * compositeAlpha)) & 0xFF) << 24) +
                      ((((int)(255 * stopColor.getRed())) & 0xFF) << 16) +
                      ((((int)(255 * stopColor.getGreen())) & 0xFF) << 8) +
                      (((int)(255 * stopColor.getBlue())) & 0xFF);
        }
        return argb;
    }

    private static int[] getFractions(Gradient grd) {
        final int nstops = grd.getNumStops();
        final int fractions[] = new int[nstops];
        for (int i = 0; i < nstops; i++) {
            final Stop stop = grd.getStops().get(i);
            fractions[i] = (int)(TO_PISCES * stop.getOffset());
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

    public BasicStroke getStroke() {
        return stroke;
    }

    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
    }

    public CompositeMode getCompositeMode() {
        return compositeMode;
    }

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

    public void setNodeBounds(RectBounds bounds) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.setNodeBounds: " + bounds);
        }
        nodeBounds = bounds;
    }

    public void clear() {
        this.clear(Color.TRANSPARENT);
    }

    /**
     * Clears the current {@code RenderTarget} with the given {@code Color}.
     * Note that this operation is affected by the current clip rectangle,
     * if set.  To clear the entire surface, call {@code setClipRect(null)}
     * prior to calling {@code clear()}.
     */
    public void clear(Color color) {
        if (PrismSettings.debug) {
            System.out.println("+ PR.clear: " + color);
        }
        this.setColor(color, 1f);
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
    public void clearQuad(float x1, float y1, float x2, float y2) {
        final CompositeMode cm = this.compositeMode;
        final Paint p = this.paint;
        this.setCompositeMode(CompositeMode.SRC);
        this.setPaint(Color.TRANSPARENT);
        this.fillQuad(x1, y1, x2, y2);
        this.setCompositeMode(cm);
        this.setPaint(p);
    }

    public void fill(Shape shape) {
        if (PrismSettings.debug) {
            System.out.println("+ fill(Shape)");
        }
        paintShape(shape, null, this.tx);
    }

    public void fillQuad(float x1, float y1, float x2, float y2) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.fillQuad");
        }
        this.fillRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

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
            this.setPaintFromShape(null, tx, x, y, width, height);

            final Point2D p1 = new Point2D(x, y);
            final Point2D p2 = new Point2D(x + width, y + height);
            tx.transform(p1, p1);
            tx.transform(p2, p2);
            this.pr.fillRect((int)(Math.min(p1.x, p2.x) * TO_PISCES), (int)(Math.min(p1.y, p2.y) * TO_PISCES),
                    (int)(Math.abs(p2.x - p1.x) * TO_PISCES), (int)(Math.abs(p2.y - p1.y) * TO_PISCES));
        } else {
            this.fillRoundRect(x, y, width, height, 0, 0);
        }
    }

    public void fillRoundRect(float x, float y, float width, float height,
                              float arcw, float arch) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.fillRoundRect");
        }
        this.paintRoundRect(x, y, width, height, arcw, arch, null);
    }

    public void fillEllipse(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.fillEllipse");
        }
        this.paintEllipse(x, y, width, height, null);
    }

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

        this.setPaintFromShape(shape, tr, 0,0,0,0);

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
        context.renderShape(this.pr, shape, st, tr, this.finalClip);
    }

    private void setPaintFromShape(Shape shape, BaseTransform tr, float localX, float localY, float localWidth, float localHeight) {
        final float x, y, w, h;
        if (paint.isProportional()) {
            if (nodeBounds != null) {
                x = nodeBounds.getMinX();
                y = nodeBounds.getMinY();
                w = nodeBounds.getWidth();
                h = nodeBounds.getHeight();
            } else if (shape != null) {
                final RectBounds bounds = shape.getBounds();
                x = bounds.getMinX();
                y = bounds.getMinY();
                w = bounds.getWidth();
                h = bounds.getHeight();
            } else {
                x = localX;
                y = localY;
                w = localWidth;
                h = localHeight;
            }
        } else {
            x = y = w = h = 0;
        }
        this.setPaintBeforeDraw(x, y, w, h);
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

    public void drawRect(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawRect");
        }
        this.drawRoundRect(x, y, width, height, 0, 0);
    }

    public void drawRoundRect(float x, float y, float width, float height,
                              float arcw, float arch) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawRoundRect");
        }
        this.paintRoundRect(x, y, width, height, arcw, arch, stroke);
    }

    public void drawEllipse(float x, float y, float width, float height) {
        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawEllipse");
        }
        this.paintEllipse(x, y, width, height, stroke);
    }

    public void drawString(GlyphList gl, FontStrike strike, float x, float y,
                           Color selectColor, int selectStart, int selectEnd) {

        if (PrismSettings.debug) {
            System.out.println("+ SWG.drawGlyphList, gl.Count: " + gl.getGlyphCount() +
                    ", x: " + x + ", y: " + y +
                    ", selectStart: " + selectStart + ", selectEnd: " + selectEnd);
        }

        int selectGlyphStart = -1;
        int selectGlyphEnd = -1;
        if ((selectColor != null) && (selectStart < selectEnd)) {
            // convert selectStart / selectEnd from char indexes to glyph indexes
            int i = 0;
            while (i < gl.getGlyphCount()) {
                if (gl.getCharOffset(i) >= selectStart) {
                    selectGlyphStart = i;
                    selectGlyphEnd = gl.getGlyphCount();
                    break;
                }
                i++;
            }
            while (i < gl.getGlyphCount()) {
                if (gl.getCharOffset(i) >= selectEnd) {
                    selectGlyphEnd = i;
                    break;
                }
                i++;
            }
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

        // check for selection that is out of range of this line (TextArea)
        if ((selectGlyphStart < 0 && selectGlyphEnd < 0)||(selectGlyphStart >= gl.getGlyphCount() && selectGlyphEnd >= gl.getGlyphCount())) {
            this.drawStringInternal(gl, strike, x, y, 0, gl.getGlyphCount(), this.paint, bx, by, bw, bh);
        } else {
            float advanceX = 0;
            if (selectGlyphStart > 0) {
                advanceX = this.drawStringInternal(gl, strike, x, y, 0, selectGlyphStart, this.paint, bx, by, bw, bh);
            }
            advanceX += this.drawStringInternal(gl, strike, x+advanceX, y,
                                                Math.max(0, selectGlyphStart), Math.min(gl.getGlyphCount(), selectGlyphEnd),
                                                selectColor, 0, 0, 0, 0);
            if (selectGlyphEnd < gl.getGlyphCount()) {
                this.drawStringInternal(gl, strike, x+advanceX, y, selectGlyphEnd, gl.getGlyphCount(),
                                        this.paint, bx, by, bw, bh);
            }
        }
    }

    private float drawStringInternal(GlyphList gl, FontStrike strike, float x, float y, int strFrom, int strTo,
                                     Paint p, float bx, float by, float bw, float bh)
    {
        float advanceX = 0;
        if (tx.isTranslateOrIdentity() && (!strike.drawAsShapes())) {
            final boolean doLCDText = (strike.getAAMode() == FontResource.AA_LCD) &&
                    getRenderTarget().isOpaque() &&
                    (p.getType() == Paint.Type.COLOR) &&
                    tx.is2D();

            final float gamma, invgamma;
            if (doLCDText) {
                invgamma = PrismFontUtils.getLCDContrast();
                gamma = 1.0f/invgamma;
                final Color c = (Color)p;
                final Color correctedColor = new Color((float)Math.pow(c.getRed(), invgamma),
                                                       (float)Math.pow(c.getGreen(), invgamma),
                                                       (float)Math.pow(c.getBlue(), invgamma),
                                                       (float)Math.pow(c.getAlpha(), invgamma));
                this.setColor(correctedColor, this.compositeAlpha);
            } else {
                gamma = invgamma = 0;
                final FontResource fr = strike.getFontResource();
                final float origSize = strike.getSize();
                final BaseTransform origTx = strike.getTransform();
                strike = fr.getStrike(origSize, origTx, FontResource.AA_GREYSCALE);
                this.setPaintBeforeDraw(p, bx, by, bw, bh);
            }

            for (int i = strFrom; i < strTo; i++) {
                final Glyph g = strike.getGlyph(gl.getGlyphCode(i));
                if (doLCDText && g.isLCDGlyph()) {
                    final double posX = x + tx.getMxt() + g.getOriginX() + gl.getPosX(i) + 0.5f;
                    int subPosX = ((int)(3*posX)) % 3;
                    if (subPosX < 0) {
                        subPosX += 3;
                    }
                    this.pr.fillLCDAlphaMask(g.getPixelData(),
                            (int)posX,
                            (int)(y + tx.getMyt() + g.getOriginY() + gl.getPosY(i) + 0.5f),
                            subPosX,
                            g.getWidth(), g.getHeight(),
                            0, g.getWidth(),
                            gamma, invgamma);
                } else {
                    this.pr.fillAlphaMask(g.getPixelData(),
                            (int)(x + tx.getMxt() + g.getOriginX() + gl.getPosX(i) + 0.5f),
                            (int)(y + tx.getMyt() + g.getOriginY() + gl.getPosY(i) + 0.5f),
                            g.getWidth(), g.getHeight(),
                            0, g.getWidth());
                }
            }
        } else {
            final BaseTransform glyphTx = tx.copy();
            glyphTx.deriveWithTranslation(x, y);
            for (int i = strFrom; i < strTo; i++) {
                final Glyph g = strike.getGlyph(gl.getGlyphCode(i));
                this.paintShape(g.getShape(), null, glyphTx);
                advanceX += g.getAdvance();
                glyphTx.deriveWithTranslation(g.getAdvance(), 0);
            }
        }
        return advanceX;
    }

    public void drawTexture(Texture tex, float x, float y, float w, float h) {
        if (PrismSettings.debug) {
            System.out.printf("+ drawTexture1, x: %f, y: %f, w: %f, h: %f\n", x, y, w, h);
        }
        this.drawTexture(tex, x, y, x + w, y + h, 0, 0, w, h);
    }

    public void drawTexture(Texture tex,
                            float dx1, float dy1, float dx2, float dy2,
                            float sx1, float sy1, float sx2, float sy2) {
        if (PrismSettings.debug) {
            System.out.println("+ drawTexture2");
        }
        this.drawTextureVO(tex, 1, 1, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
    }

    private void computeScaleAndPixelCorrection(float[] target, float dv1, float dv2, float sv1, float sv2) {
        final float dv_diff = dv2 - dv1;
        float scale = dv_diff / (sv2 - sv1);
        float pixel_correction = 0;
        if (Math.abs(scale) > 2*Math.abs(dv_diff)) {
            // scaling "single" pixel
            // we need to "2*" since there is half-pixel shift for
            // the purpose of interpolation in the native
            scale = 2 * Math.signum(scale) * Math.abs(dv_diff);
            if ((int)sv2 != (int)sv1) {
                // scaling parts of two neighboring pixels
                final float sx_min = Math.min(sv1, sv2);
                final float pixel_reminder = (float)(Math.ceil(sx_min)) - sx_min;
                pixel_correction = pixel_reminder / (2*(sv2 - sv1));
            }
        }
        target[0] = scale;
        target[1] = pixel_correction;
    }

    private Rectangle srcBBox;
    private Rectangle dstBBox;

    public void drawTextureVO(Texture tex,
                              float topopacity, float botopacity,
                              float dx1, float dy1, float dx2, float dy2,
                              float sx1, float sy1, float sx2, float sy2)
    {
        if (PrismSettings.debug) {
            System.out.println("+ drawTextureVO: "+tex+", tOpacity: " + topopacity + ", bOpacity: " + botopacity +
                    ", tex.w: " + tex.getPhysicalWidth() + ", tex.h: " + tex.getPhysicalHeight());
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

        final int ix1 = SWUtils.fastFloor(dx1);
        final int iy1 = SWUtils.fastFloor(dy1);
        final int ix2 = SWUtils.fastFloor(dx2);
        final int iy2 = SWUtils.fastFloor(dy2);
        if (srcBBox == null) {
            srcBBox = new Rectangle();
        }
        srcBBox.setBounds(Math.min(ix1, ix2), Math.min(iy1, iy2),
                Math.abs(SWUtils.fastCeil(dx2 - dx1)), Math.abs(SWUtils.fastCeil(dy2 - dy1)));
        if (dstBBox == null) {
            dstBBox = new Rectangle();
        }

        tx.transform(srcBBox, dstBBox);

        paintTx.setTransform(tx);

        final float[] scale_correction = new float[2];
        computeScaleAndPixelCorrection(scale_correction, dx1, dx2, sx1, sx2);
        final float scaleX = scale_correction[0];
        final float x_pixel_correction = scale_correction[1];

        computeScaleAndPixelCorrection(scale_correction, dy1, dy2, sy1, sy2);
        final float scaleY = scale_correction[0];
        final float y_pixel_correction = scale_correction[1];

        if (scaleX == 1 && scaleY == 1) {
            paintTx.deriveWithTranslation(-Math.min(sx1, sx2) + Math.min(dx1, dx2),
                    -Math.min(sy1, sy2) + Math.min(dy1, dy2));
        } else {
            paintTx.deriveWithTranslation(Math.min(dx1, dx2), Math.min(dy1, dy2));
            paintTx.deriveWithTranslation((scaleX >= 0) ? 0 : Math.abs(dx2 - dx1),
                    (scaleY >= 0) ? 0 : Math.abs(dy2 - dy1));
            paintTx.deriveWithConcatenation(scaleX, 0, 0, scaleY, 0, 0);
            paintTx.deriveWithTranslation(-Math.min(sx1, sx2) + x_pixel_correction,
                                          -Math.min(sy1, sy2) + y_pixel_correction);
        }

        convertToPiscesTransform(paintTx, piscesTx);

        if (scaleY == -1) {
            final float swap = topopacity;
            topopacity = botopacity;
            botopacity = swap;
        }

        if (PrismSettings.debug) {
            System.out.println("tx: " + tx);
            System.out.println("piscesTx: " + piscesTx);

            System.out.println("srcBBox: " + srcBBox);
            System.out.println("dstBBox: " + dstBBox);
        }

        final int interpolateMinX = SWUtils.fastFloor(Math.min(sx1, sx2));
        final int interpolateMinY = SWUtils.fastFloor(Math.min(sy1, sy2));
        final int interpolateMaxX = SWUtils.fastCeil(Math.max(sx1, sx2)) - 1;
        final int interpolateMaxY = SWUtils.fastCeil(Math.max(sy1, sy2)) - 1;

        this.pr.drawImage(RendererBase.TYPE_INT_ARGB_PRE, data, tex.getPhysicalWidth(), tex.getPhysicalHeight(),
                swTex.getOffset(), swTex.getStride(),
                piscesTx, false,
                dstBBox.x, dstBBox.y, dstBBox.width, dstBBox.height,
                interpolateMinX, interpolateMinY, interpolateMaxX, interpolateMaxY,
                (int)(topopacity * this.compositeAlpha * 255), (int)(botopacity * this.compositeAlpha * 255),
                swTex.hasAlpha());

        if (PrismSettings.debug) {
            System.out.println("* drawTextureVO, DONE");
        }
    }

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
            this.drawTexture(tex, 0, 0, 1, 1, 0, 0, tex.getPhysicalWidth(), tex.getPhysicalHeight());
        } catch (NoninvertibleTransformException e) { }

        tx.restoreTransform(_mxx, _myx, _mxy, _myy, _mxt, _myt);
    }

    public boolean canReadBack() {
        return true;
    }

    public RTTexture readBack(Rectangle view) {
        context.validateRBBuffer(Math.max(0, view.width), Math.max(0, view.height));
        RTTexture rbb = context.getReadBackBuffer();

        if (view.width <= 0 || view.height <= 0) {
            return rbb;
        }

        int pixels[] = rbb.getPixels();
        this.target.getSurface().getRGB(pixels, 0, rbb.getPhysicalWidth(), view.x, view.y, view.width, view.height);
        return rbb;
    }

    public void releaseReadBackBuffer(RTTexture view) {
    }

    public void setState3D(boolean flag) {
    }

    public boolean isState3D() {
        return false;
    }

    public void setup3DRendering() {
    }

    @Override
    public void setLights(Object[] lights) {
        // Light are not supported by SW pipeline
    }

    @Override
    public Object[] getLights() {
        // Light are not supported by SW pipeline
        return null;
    }
}
