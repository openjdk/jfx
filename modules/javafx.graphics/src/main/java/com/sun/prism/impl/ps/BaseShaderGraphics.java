/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.ps;

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.AffineBase;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.MaskTextureGraphics;
import com.sun.prism.MultiTexture;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackGraphics;
import com.sun.prism.ReadbackRenderTarget;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.GlyphCache;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.impl.ps.BaseShaderContext.MaskType;
import com.sun.prism.impl.shape.MaskData;
import com.sun.prism.impl.shape.ShapeUtil;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderGraphics;

public abstract class BaseShaderGraphics
    extends BaseGraphics
    implements ShaderGraphics, ReadbackGraphics, MaskTextureGraphics
{
    private static Affine2D TEMP_TX2D = new Affine2D();
    private static Affine3D TEMP_TX3D = new Affine3D();

    private final BaseShaderContext context;
    private Shader externalShader;
    private boolean isComplexPaint;

    protected BaseShaderGraphics(BaseShaderContext context,
                                 RenderTarget renderTarget)
    {
        super(context, renderTarget);
        this.context = context;
    }

    BaseShaderContext getContext() {
        return context;
    }

    boolean isComplexPaint() {
        return isComplexPaint;
    }

    public void getPaintShaderTransform(Affine3D ret) {
        ret.setTransform(getTransformNoClone());
    }

    public Shader getExternalShader() {
        return externalShader;
    }

    public void setExternalShader(Shader shader) {
        this.externalShader = shader;
        context.setExternalShader(this, shader);
    }

    @Override
    public void setPaint(Paint paint) {
        if (paint.getType().isGradient()) {
            Gradient grad = (Gradient)paint;
            isComplexPaint = grad.getNumStops() > PaintHelper.MULTI_MAX_FRACTIONS;
        } else {
            isComplexPaint = false;
        }
        super.setPaint(paint);
    }

    private NGLightBase lights[] = null;

    public void setLights(NGLightBase lights[]) { this.lights = lights; }

    public final NGLightBase[] getLights() { return this.lights; }

    @Override
    public void clearQuad(float x1, float y1, float x2, float y2) {
        // note that unlike clear(), this method does not currently
        // attempt to clear the depth buffer...
        context.setRenderTarget(this);
        context.flushVertexBuffer();
        // set the blend mode to CLEAR and any regular Color as paint
        CompositeMode oldMode = getCompositeMode();
        setCompositeMode(CompositeMode.CLEAR);
        Paint oldPaint = getPaint();
        setPaint(Color.BLACK); // any color will do...
        fillQuad(x1, y1, x2, y2);
        context.flushVertexBuffer();
        // restore prior paint and blend mode
        setPaint(oldPaint);
        setCompositeMode(oldMode);
    }

    @Override
    public void drawTexture(Texture tex,
                            float dx1, float dy1, float dx2, float dy2,
                            float sx1, float sy1, float sx2, float sy2)
    {
        // intercept MultiTexture operations
        // FIXME: this should be pushed up to Graphics interface so that non-shader
        //        renderers can use MultiTexture too
        if (tex instanceof MultiTexture) {
            drawMultiTexture((MultiTexture)tex, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
        } else {
            super.drawTexture(tex, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
        }
    }

    @Override
    public void drawTexture3SliceH(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dh1, float dh2, float sh1, float sh2)
    {
        if (!(tex instanceof MultiTexture)) {
            super.drawTexture3SliceH(tex,
                                     dx1, dy1, dx2, dy2,
                                     sx1, sy1, sx2, sy2,
                                     dh1, dh2, sh1, sh2);
            return;
        }
        MultiTexture mtex = (MultiTexture) tex;
        drawMultiTexture(mtex, dx1, dy1, dh1, dy2, sx1, sy1, sh1, sy2);
        drawMultiTexture(mtex, dh1, dy1, dh2, dy2, sh1, sy1, sh2, sy2);
        drawMultiTexture(mtex, dh2, dy1, dx2, dy2, sh2, sy1, sx2, sy2);
    }

    @Override
    public void drawTexture3SliceV(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dv1, float dv2, float sv1, float sv2)
    {
        if (!(tex instanceof MultiTexture)) {
            super.drawTexture3SliceV(tex,
                                     dx1, dy1, dx2, dy2,
                                     sx1, sy1, sx2, sy2,
                                     dv1, dv2, sv1, sv2);
            return;
        }
        MultiTexture mtex = (MultiTexture) tex;
        drawMultiTexture(mtex, dx1, dy1, dx2, dv1, sx1, sy1, sx2, sv1);
        drawMultiTexture(mtex, dx1, dv1, dx2, dv2, sx1, sv1, sx2, sv2);
        drawMultiTexture(mtex, dx1, dv2, dx2, dy2, sx1, sv2, sx2, sy2);
    }

    @Override
    public void drawTexture9Slice(Texture tex,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float sx1, float sy1, float sx2, float sy2,
                                  float dh1, float dv1, float dh2, float dv2,
                                  float sh1, float sv1, float sh2, float sv2)
    {
        if (!(tex instanceof MultiTexture)) {
            super.drawTexture9Slice(tex,
                                    dx1, dy1, dx2, dy2,
                                    sx1, sy1, sx2, sy2,
                                    dh1, dv1, dh2, dv2,
                                    sh1, sv1, sh2, sv2);
            return;
        }
        MultiTexture mtex = (MultiTexture) tex;
        drawMultiTexture(mtex, dx1, dy1, dh1, dv1, sx1, sy1, sh1, sv1);
        drawMultiTexture(mtex, dh1, dy1, dh2, dv1, sh1, sy1, sh2, sv1);
        drawMultiTexture(mtex, dh2, dy1, dx2, dv1, sh2, sy1, sx2, sv1);

        drawMultiTexture(mtex, dx1, dv1, dh1, dv2, sx1, sv1, sh1, sv2);
        drawMultiTexture(mtex, dh1, dv1, dh2, dv2, sh1, sv1, sh2, sv2);
        drawMultiTexture(mtex, dh2, dv1, dx2, dv2, sh2, sv1, sx2, sv2);

        drawMultiTexture(mtex, dx1, dv2, dh1, dy2, sx1, sv2, sh1, sy2);
        drawMultiTexture(mtex, dh1, dv2, dh2, dy2, sh1, sv2, sh2, sy2);
        drawMultiTexture(mtex, dh2, dv2, dx2, dy2, sh2, sv2, sx2, sy2);
    }

    private static float calculateScaleFactor(float contentDim, float physicalDim) {
        if (contentDim == physicalDim) {
            return 1f;
        }
        // we have to subtract 1 to eliminate the "green line of death"
        return (contentDim-1) / physicalDim;
    }

    protected void drawMultiTexture(MultiTexture tex,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float sx1, float sy1, float sx2, float sy2)
    {
        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        if (isSimpleTranslate) {
            xform = IDENT;
            dx1 += transX;
            dy1 += transY;
            dx2 += transX;
            dy2 += transY;
        }

        Texture textures[] = ((MultiTexture)tex).getTextures();
        Shader shader = context.validateTextureOp(this, xform, textures, tex.getPixelFormat());

        if (null == shader) {
            // FIXME: throw exception?? We can't do anything without the shader..
            return;
        }

        if (tex.getPixelFormat() == PixelFormat.MULTI_YCbCr_420) {
            Texture lumaTex = textures[PixelFormat.YCBCR_PLANE_LUMA];
            Texture cbTex = textures[PixelFormat.YCBCR_PLANE_CHROMABLUE];
            Texture crTex = textures[PixelFormat.YCBCR_PLANE_CHROMARED];

            // sampler scaling factors
            float imgWidth = (float)tex.getContentWidth();
            float imgHeight = (float)tex.getContentHeight();
            float lumaScaleX, lumaScaleY;
            float alphaScaleX, alphaScaleY;
            float cbScaleX, cbScaleY;
            float crScaleX, crScaleY;

            lumaScaleX = calculateScaleFactor(imgWidth, (float)lumaTex.getPhysicalWidth());
            lumaScaleY = calculateScaleFactor(imgHeight, (float)lumaTex.getPhysicalHeight());

            if (textures.length > 3) {
                Texture alphaTex = textures[PixelFormat.YCBCR_PLANE_ALPHA];
                alphaScaleX = calculateScaleFactor(imgWidth, (float)alphaTex.getPhysicalWidth());
                alphaScaleY = calculateScaleFactor(imgHeight, (float)alphaTex.getPhysicalHeight());
            } else {
                alphaScaleX = alphaScaleY = 0f;
            }

            float chromaWidth = (float)Math.floor((double)imgWidth/2.0);
            float chromaHeight = (float)Math.floor((double)imgHeight/2.0);

            cbScaleX = calculateScaleFactor(chromaWidth, (float)cbTex.getPhysicalWidth());
            cbScaleY = calculateScaleFactor(chromaHeight, (float)cbTex.getPhysicalHeight());
            crScaleX = calculateScaleFactor(chromaWidth, (float)crTex.getPhysicalWidth());
            crScaleY = calculateScaleFactor(chromaHeight, (float)crTex.getPhysicalHeight());

            shader.setConstant("lumaAlphaScale", lumaScaleX, lumaScaleY, alphaScaleX, alphaScaleY);
            shader.setConstant("cbCrScale", cbScaleX, cbScaleY, crScaleX, crScaleY);

            float tx1 = sx1 / imgWidth;
            float ty1 = sy1 / imgHeight;
            float tx2 = sx2 / imgWidth;
            float ty2 = sy2 / imgHeight;

            VertexBuffer vb = context.getVertexBuffer();
            vb.addQuad(dx1, dy1, dx2, dy2, tx1, ty1, tx2, ty2);
        } else {
            // should have been caught by validateTextureOp, but just in case
            throw new UnsupportedOperationException("Unsupported multitexture format "+tex.getPixelFormat());
        }
    }

    public void drawTextureRaw2(Texture src1, Texture src2,
                                float dx1, float dy1, float dx2, float dy2,
                                float t1x1, float t1y1, float t1x2, float t1y2,
                                float t2x1, float t2y1, float t2x2, float t2y2)
    {
        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        if (isSimpleTranslate) {
            xform = IDENT;
            dx1 += transX;
            dy1 += transY;
            dx2 += transX;
            dy2 += transY;
        }
        context.validateTextureOp(this, xform, src1, src2,
                                  PixelFormat.INT_ARGB_PRE);

        VertexBuffer vb = context.getVertexBuffer();
        vb.addQuad(dx1, dy1, dx2, dy2,
                   t1x1, t1y1, t1x2, t1y2,
                   t2x1, t2y1, t2x2, t2y2);
    }

    public void drawMappedTextureRaw2(Texture src1, Texture src2,
                                      float dx1, float dy1, float dx2, float dy2,
                                      float t1x11, float t1y11, float t1x21, float t1y21,
                                      float t1x12, float t1y12, float t1x22, float t1y22,
                                      float t2x11, float t2y11, float t2x21, float t2y21,
                                      float t2x12, float t2y12, float t2x22, float t2y22)
    {
        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        if (isSimpleTranslate) {
            xform = IDENT;
            dx1 += transX;
            dy1 += transY;
            dx2 += transX;
            dy2 += transY;
        }
        context.validateTextureOp(this, xform, src1, src2,
                                  PixelFormat.INT_ARGB_PRE);

        VertexBuffer vb = context.getVertexBuffer();
        vb.addMappedQuad(dx1, dy1, dx2, dy2,
                         t1x11, t1y11, t1x21, t1y21,
                         t1x12, t1y12, t1x22, t1y22,
                         t2x11, t2y11, t2x21, t2y21,
                         t2x12, t2y12, t2x22, t2y22);
    }

    public void drawPixelsMasked(RTTexture imgtex, RTTexture masktex,
                                 int dx, int dy, int dw, int dh,
                                 int ix, int iy, int mx, int my)
    {
        if (dw <= 0 || dh <= 0) return;
        float iw = imgtex.getPhysicalWidth();
        float ih = imgtex.getPhysicalHeight();
        float mw = masktex.getPhysicalWidth();
        float mh = masktex.getPhysicalHeight();
        float dx1 = dx;
        float dy1 = dy;
        float dx2 = dx + dw;
        float dy2 = dy + dh;
        float ix1 = ix / iw;
        float iy1 = iy / ih;
        float ix2 = (ix + dw) / iw;
        float iy2 = (iy + dh) / ih;
        float mx1 = mx / mw;
        float my1 = my / mh;
        float mx2 = (mx + dw) / mw;
        float my2 = (my + dh) / mh;
        context.validateMaskTextureOp(this, IDENT, imgtex, masktex,
                                      PixelFormat.INT_ARGB_PRE);
        VertexBuffer vb = context.getVertexBuffer();
        vb.addQuad(dx1, dy1, dx2, dy2,
                   ix1, iy1, ix2, iy2,
                   mx1, my1, mx2, my2);
    }

    public void maskInterpolatePixels(RTTexture imgtex, RTTexture masktex,
                                      int dx, int dy, int dw, int dh,
                                      int ix, int iy, int mx, int my)
    {
        if (dw <= 0 || dh <= 0) return;
        float iw = imgtex.getPhysicalWidth();
        float ih = imgtex.getPhysicalHeight();
        float mw = masktex.getPhysicalWidth();
        float mh = masktex.getPhysicalHeight();
        float dx1 = dx;
        float dy1 = dy;
        float dx2 = dx + dw;
        float dy2 = dy + dh;
        float ix1 = ix / iw;
        float iy1 = iy / ih;
        float ix2 = (ix + dw) / iw;
        float iy2 = (iy + dh) / ih;
        float mx1 = mx / mw;
        float my1 = my / mh;
        float mx2 = (mx + dw) / mw;
        float my2 = (my + dh) / mh;
        CompositeMode oldmode = getCompositeMode();
        setCompositeMode(CompositeMode.DST_OUT);
        context.validateTextureOp(this, IDENT, masktex,
                                  PixelFormat.INT_ARGB_PRE);
        VertexBuffer vb = context.getVertexBuffer();
        vb.addQuad(dx1, dy1, dx2, dy2,
                   mx1, my1, mx2, my2);

        setCompositeMode(CompositeMode.ADD);
        context.validateMaskTextureOp(this, IDENT, imgtex, masktex,
                                      PixelFormat.INT_ARGB_PRE);
        vb.addQuad(dx1, dy1, dx2, dy2,
                   ix1, iy1, ix2, iy2,
                   mx1, my1, mx2, my2);

        setCompositeMode(oldmode);
    }

    private void renderWithComplexPaint(Shape shape, BasicStroke stroke,
                                        float bx, float by, float bw, float bh)
    {
        // creating/updating the mask texture may unset the current
        // texture used by pending vertices, so flush the vertex buffer first
        context.flushVertexBuffer();

        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        MaskData maskData =
            ShapeUtil.rasterizeShape(shape, stroke, getFinalClipNoClone(), xform, true, isAntialiasedShape());
        int maskW = maskData.getWidth();
        int maskH = maskData.getHeight();

        float dx1 = maskData.getOriginX();
        float dy1 = maskData.getOriginY();
        float dx2 = dx1 + maskW;
        float dy2 = dy1 + maskH;

        // Note that we could use multitexturing here (upload mask
        // data to texture unit 0 and paint data to texture unit 1
        // then set the texture mode to modulate) but this operation is
        // already plenty slow and not worth optimizing at this time,
        // so for now we will merge the mask with the paint data in software.
        Gradient grad = (Gradient)paint;
        TEMP_TX2D.setToTranslation(-dx1, -dy1);
        TEMP_TX2D.concatenate(xform);
        Texture tex = context.getGradientTexture(grad, TEMP_TX2D,
                                                 maskW, maskH, maskData,
                                                 bx, by, bw, bh);

        float tx1 = 0f;
        float ty1 = 0f;
        float tx2 = tx1 + ((float)maskW) / tex.getPhysicalWidth();
        float ty2 = ty1 + ((float)maskH) / tex.getPhysicalHeight();

        // the mask has been generated in device space, so we use
        // identity transform here
        VertexBuffer vb = context.getVertexBuffer();
        context.validateTextureOp(this, IDENT, tex, null, tex.getPixelFormat());
        vb.addQuad(dx1, dy1, dx2, dy2, tx1, ty1, tx2, ty2);
        tex.unlock();
    }

    private static RectBounds TMP_BOUNDS = new RectBounds();
    @Override
    protected void renderShape(Shape shape, BasicStroke stroke,
                               float bx, float by, float bw, float bh)
    {
        if (isComplexPaint) {
            renderWithComplexPaint(shape, stroke, bx, by, bw, bh);
            return;
        }

        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        MaskData maskData =
            ShapeUtil.rasterizeShape(shape, stroke, getFinalClipNoClone(), xform, true, isAntialiasedShape());
        Texture maskTex = context.validateMaskTexture(maskData, false);

        AffineBase paintTx;
        if (PrismSettings.primTextureSize != 0) {
            // the mask has been generated in device space, so we use
            // identity transform here
            Shader shader =
                context.validatePaintOp(this, IDENT, MaskType.ALPHA_TEXTURE, maskTex,
                                        bx, by, bw, bh);

            paintTx = getPaintTextureTx(xform, shader, bx, by, bw, bh);
        } else {
            // the mask has been generated in device space, so we use
            // identity transform here
            context.validatePaintOp(this, IDENT, maskTex, bx, by, bw, bh);
            paintTx = null;
        }

        context.updateMaskTexture(maskData, TMP_BOUNDS, false);

        float dx1 = maskData.getOriginX();
        float dy1 = maskData.getOriginY();
        float dx2 = dx1 + maskData.getWidth();
        float dy2 = dy1 + maskData.getHeight();
        float tx1 = TMP_BOUNDS.getMinX();
        float ty1 = TMP_BOUNDS.getMinY();
        float tx2 = TMP_BOUNDS.getMaxX();
        float ty2 = TMP_BOUNDS.getMaxY();

        VertexBuffer vb = context.getVertexBuffer();
        vb.addQuad(dx1, dy1, dx2, dy2, tx1, ty1, tx2, ty2, paintTx);

        maskTex.unlock();
    }

    private static float getStrokeExpansionFactor(BasicStroke stroke) {
        if (stroke.getType() == BasicStroke.TYPE_OUTER) {
            return 1f;
        } else if (stroke.getType() == BasicStroke.TYPE_CENTERED) {
            return 0.5f;
        } else {
            return 0f;
        }
    }

    private static final float FRINGE_FACTOR;
    static {
        @SuppressWarnings("removal")
        String v = (String) AccessController.doPrivileged((PrivilegedAction) () -> System.getProperty("prism.primshaderpad"));
        if (v == null) {
            FRINGE_FACTOR = -0.5f;
        } else {
            FRINGE_FACTOR = -Float.valueOf(v);
            System.out.println("Prism ShaderGraphics primitive shader pad = "+FRINGE_FACTOR);
        }
    }

    private BaseTransform extract3Dremainder(BaseTransform xform) {
        if (xform.is2D()) {
            return IDENT;
        }
        TEMP_TX3D.setTransform(xform);
        TEMP_TX2D.setTransform(xform.getMxx(), xform.getMyx(),
                               xform.getMxy(), xform.getMyy(),
                               xform.getMxt(), xform.getMyt());
        try {
            TEMP_TX2D.invert();
            TEMP_TX3D.concatenate(TEMP_TX2D);
        } catch (NoninvertibleTransformException ex) {
        }
        return TEMP_TX3D;
    }

    private void renderGeneralRoundedRect(float rx, float ry, float rw, float rh,
                                          float arcw, float arch,
                                          MaskType type, BasicStroke stroke)
    {
        // NOTE: using floats here for now, not sure if it's a problem yet...
        float bx, by, bw, bh, ifractw, ifracth;
        float ox, oy, wdx, wdy, hdx, hdy;
        if (stroke == null) {
            bx = rx;
            by = ry;
            bw = rw;
            bh = rh;
            ifractw = ifracth = 0f;
        } else {
            float sw = stroke.getLineWidth();
            float ow = getStrokeExpansionFactor(stroke) * sw;
            bx = rx - ow;
            by = ry - ow;
            ow *= 2f;
            bw = rw + ow;
            bh = rh + ow;
            if (arcw > 0 && arch > 0) {
                arcw += ow;
                arch += ow;
            } else {
                if (stroke.getLineJoin() == BasicStroke.JOIN_ROUND) {
                    arcw = arch = ow;
                    type = MaskType.DRAW_ROUNDRECT;
                } else {
                    arcw = arch = 0f;
                }
            }
            ifractw = (bw - sw * 2f) / bw;
            ifracth = (bh - sw * 2f) / bh;
            if (ifractw <= 0f || ifracth <= 0f) {
                type = type.getFillType();
            }
        }

        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        BaseTransform rendertx;
        if (isSimpleTranslate) {
            wdx = hdy = 1f;
            wdy = hdx = 0f;
            ox = bx + transX;
            oy = by + transY;
            rendertx = IDENT;
        } else {
            rendertx = extract3Dremainder(xform);
            wdx = (float)xform.getMxx();
            hdx = (float)xform.getMxy();
            wdy = (float)xform.getMyx();
            hdy = (float)xform.getMyy();
            ox = (bx * wdx) + (by * hdx) + (float)xform.getMxt();
            oy = (bx * wdy) + (by * hdy) + (float)xform.getMyt();
        }

        wdx *= bw;
        wdy *= bw;
        hdx *= bh;
        hdy *= bh;

        float arcfractw = arcw / bw;
        float arcfracth = arch / bh;
        renderGeneralRoundedPgram(ox, oy, wdx, wdy, hdx, hdy,
                                  arcfractw, arcfracth, ifractw, ifracth,
                                  rendertx, type, rx, ry, rw, rh);
    }

    private void renderGeneralRoundedPgram(float ox, float oy,
                                           float wvecx, float wvecy,
                                           float hvecx, float hvecy,
                                           float arcfractw, float arcfracth,
                                           float ifractw, float ifracth,
                                           BaseTransform rendertx, MaskType type,
                                           float rx, float ry, float rw, float rh)
    {
        float wlen = len(wvecx, wvecy);
        float hlen = len(hvecx, hvecy);
        if (wlen == 0 || hlen == 0) {
            // parallelogram has collapsed to a line or point
            return;
        }

        // Calculate the 4 corners of the pgram in device space.
        // Note that the UL,UR,LL,LR (Upper/Lower Left/Right) designations
        // are virtual since the wdxy and hdxy vectors can point in any
        // direction depending on the transform being applied.
        float xUL = ox;
        float yUL = oy;
        float xUR = ox + wvecx;
        float yUR = oy + wvecy;
        float xLL = ox + hvecx;
        float yLL = oy + hvecy;
        float xLR = xUR + hvecx;
        float yLR = yUR + hvecy;

        // Calculate the unit vectors along each side of the pgram as well
        // as the device space perpendicular dimension across the pgram
        // (which is different than the lengths of the two pairs of sides
        //  since it is measured perpendicular to the "other" sides, not
        //  along the original sides).
        // The perpendicular dimension is the dot product of the perpendicular
        // of the unit vector for one pair of sides with the displacement
        // vector for the other pair of sides.
        // The unit vector perpendicular to (dx,dy) is given by:
        // normx = dx / len(dx, dy);
        // normy = dy / len(dx, dy);
        // The (ccw) perpendicular vectors would then be:
        // unitperpx = +normy = +dy / len;
        // unitperpy = -normx = -dx / len;
        // Thus the perpendicular width and height distances are:
        // pwdist = wvec.uphvec = wvecx * (hvecy/hlen) - wvecy * (hvecx/hlen)
        // phdist = hvec.upwvec = hvecx * (wvecy/wlen) - hvecy * (wvecx/wlen)
        // If we factor out the divide by the lengths then we are left
        // with numerators that are simply negations of each other:
        // pwdist = (wvecx * hvecy - wvecy * hvecx) / hlen
        // phdist = (hvecx * wvecy - hvecy * wvecx) / wlen
        // Finally we multiply by 0.5 since we want the distance to the
        // edges of the parallelgram from the center point, not across the
        // whole pgram.  And, since we want the absolute value, we can
        // ignore the fact that the numerator is negated between the two
        // formulas since we will just have to negate one of the 2 values
        // afterwards anyway to make them both positive.
        // Note that the numerators are the formula for the area of the
        // parallelogram (without the abs() operation).  Dividing by the
        // length of one of the sides would then give the length of the
        // perpendicular across to the other side.
        float halfarea = (wvecx * hvecy - wvecy * hvecx) * 0.5f;
        float pwdist = halfarea / hlen;
        float phdist = halfarea / wlen;
        if (pwdist < 0) pwdist = -pwdist;
        if (phdist < 0) phdist = -phdist;

        // Now we calculate the normalized unit vectors.
        float nwvecx = wvecx / wlen;
        float nwvecy = wvecy / wlen;
        float nhvecx = hvecx / hlen;
        float nhvecy = hvecy / hlen;

        // Bias the parallelogram corner coordinates "outward" by half a
        // pixel distance.
        // The most general way to do this is to move each parallelogram
        // edge outward by a specified distance and then find the new
        // intersection point.
        // The general form for the intersection of 2 lines is:
        // line1 = (x1,y1) -> (x2,y2)
        // line2 = (x3,y3) -> (x4,y4)
        // t = ((x4-x3)(y1-y3) - (y4-y3)(x1-x3))
        //   / ((y4-y3)(x2-x1) - (x4-x3)(y2-y1))
        // intersection point = (x1 + t*(x2-x1), y1 + t*(y2-y1))
        // Now consider if these lines are displaced versions of 2
        // other lines which share a common end point (such as is the
        // case of pushing 2 adjacent edges of the pgram outward:
        // line1 = (xa+dx1, ya+dy1) -> (xb+dx1, yb+dy1)
        // line2 = (xa+dx2, ya+dy2) -> (xc+dx2, yc+dy2)
        // "x4-x3" = (xc+dx2) - (xa+dx2) = xc-xa
        // "y4-y3" = (yc+dy2) - (ya+dy2) = yc-ya
        // "x2-x1" = (xb+dx1) - (xa+dx1) = xb-xa
        // "y2-y1" = (yb+dy1) - (ya+dy1) = yb-ya
        // "y1-y3" = (y1+dy1) - (y1+dy2) = dy1 - dy2
        // "x1-x3" = (xa+dx1) - (xa+dx2) = dx1 - dx2
        // t = ((xc-xa)(dy1-dy2) - ((yc-ya)(dx1-dx2))
        //   / ((yc-ya)(xb-xa) - (xc-xa)(yb-ya))
        // Now we need to displace these 2 lines "outward" by half a
        // pixel distance.  We will start by looking at applying a unit
        // pixel distance and then cutting the adjustment in half (it
        // can be seen to scale linearly when you look at the final
        // equations).  This is achieved by applying one of our unit
        // vectors with a cw rotation to one line and the other unit
        // vector with a ccw rotation to the other line.  Ideally we
        // would choose which to apply cw vs. ccw by the way that this
        // corner of the pgram is turning, but as it turns out, the
        // consequences of getting it backward is that our "t" will
        // turn out negative (just negate all of the d[xy][12] values
        // in the t equation above and you will see that it changes
        // sign.  Since we are calculating the new corner using the
        // equation newx = (xa+dx1) + t*(xb-xa), we want the value of
        // t that drives the point "away from xb,yb", in other words,
        // we want the negative t.  So, if t is positive then we want
        // to use negative t and reverse the perpendicular offset we
        // applied to xa:
        // t < 0 => newx = (xa+dx) + t*(xb-xa) = xa + (dx + t*(xb-xa))
        // t > 0 => newx = (xa-dx) - t*(xb-xa) = xa - (dx + t*(xb-xa))
        // where (copying t equation from above again):
        // t = ((xc-xa)(dy1-dy2) - ((yc-ya)(dx1-dx2))
        //   / ((yc-ya)(xb-xa) - (xc-xa)(yb-ya))
        // For xa,ya = xUL,yUL and xb,yb = xUR,yUR and xc,yc = xLL,yLL:
        // [xy]b - [xy]a = [xy]UR - [xy]UL = wvec
        // [xy]c - [xy]a = [xy]LL - [xy]UL = hvec
        // dx1,dy1 = +nwvecy, -nwvecx  // ccw on xa->xb
        // dx2,dy2 = -nhvecy, +nhvecx  //  cw on xa->xc
        // dx1 - dx2 = +nwvecy - -nhvecy = nwvecy + nhvecy
        // dy1 - dy2 = -nwvecx - +nhvecx = -(nwvecx + nhvecx)
        float num = -hvecx*(nwvecx + nhvecx) - hvecy*(nwvecy + nhvecy);
        float den = hvecy*wvecx - hvecx*wvecy;
        float t = num / den;
        // Negating the sign of t and multiplying by 0.5 gets us the
        // proper sign for the offset and cuts it down to half a pixel
        float factor = FRINGE_FACTOR * Math.signum(t);
        float offx = (t * wvecx + nwvecy) * factor;
        float offy = (t * wvecy - nwvecx) * factor;
        xUL += offx; yUL += offy;
        // x22 is offset by the reverse amounts
        xLR -= offx; yLR -= offy;
        // For xa,ya = xUR,yUR and xb,yb = xLR,yLR and xc,yc = xUL,yUL
        // Note that xa,ya => xc,yc is negative of wvec
        // [xy]b - [xy]a = [xy]LR - [xy]UR = +hvec
        // [xy]c - [xy]a = [xy]UL - [xy]UR = -wvec
        // dx1,dy1                          = +nhvecy, -nhvecx
        // dx2,dy2 = -(-nwvecy), +(-nwvecx) = +nwvecy, -nwvecx
        // dx1 - dx2                     = nhvecy - nwvecy
        // dy1 - dy2 = -nhvecx - -nwvecx = nwvecx - nhvecx
        // den = -wvecy * hvecx - -wvecx * hvecy
        //     = hvecy * wvecx - hvecx * wvecy (already computed)
        // num = -wvecx * (nwvecx - nhvecx) - -wvecy * (nhvecy - nwvecy)
        //     = wvecy * (nhvecy - nwvecy) - wvecx * (nwvecx - nhvecx)
        num = wvecy * (nhvecy - nwvecy) - wvecx * (nwvecx - nhvecx);
        t = num / den;
        factor = FRINGE_FACTOR * Math.signum(t);
        offx = (t * hvecx + nhvecy) * factor;
        offy = (t * hvecy - nhvecx) * factor;
        xUR += offx; yUR += offy;
        xLL -= offx; yLL -= offy;

        // texture coordinates (uv) will be calculated using a transform
        // by the perpendicular unit vectors so that the texture U
        // coordinates measure our progress along the pwdist axis (i.e.
        // perpendicular to hvec and the texture V coordinates measure
        // our progress along phdist (perpendicular to wvec):
        // u = x * nhvecy - y * nhvecx;
        // v = x * nwvecy - y * nwvecx;

        // We now calculate the uv paramters for the 4 corners such that
        //     uv(cx,cy) = 0,0
        //     uv(corners - center) = +/-(wlen/2), +/-(hlen/2)
        // Note that:
        //     uv(corner - center) = uv(corner) - uv(center)
        // Calculate the center of the parallelogram and its uv values
        float xC = (xUL + xLR) * 0.5f;
        float yC = (yUL + yLR) * 0.5f;
        float uC = xC * nhvecy - yC * nhvecx;
        float vC = xC * nwvecy - yC * nwvecx;
        // Now use that to calculate the corner values relative to the center
        float uUL = xUL * nhvecy - yUL * nhvecx - uC;
        float vUL = xUL * nwvecy - yUL * nwvecx - vC;
        float uUR = xUR * nhvecy - yUR * nhvecx - uC;
        float vUR = xUR * nwvecy - yUR * nwvecx - vC;
        float uLL = xLL * nhvecy - yLL * nhvecx - uC;
        float vLL = xLL * nwvecy - yLL * nwvecx - vC;
        float uLR = xLR * nhvecy - yLR * nhvecx - uC;
        float vLR = xLR * nwvecy - yLR * nwvecx - vC;

        // the pgram params have been calculated in device space, so we use
        // identity transform here
        if (type == MaskType.DRAW_ROUNDRECT || type == MaskType.FILL_ROUNDRECT) {
            float oarcw = pwdist * arcfractw;
            float oarch = phdist * arcfracth;
            if (oarcw < 0.5 || oarch < 0.5) {
                // The pgram renderer fades the entire primitive if the arc
                // sizes fall below 0.5 pixels since the interiors act as if
                // they are sampled at the center of the indicated circle and
                // radii smaller than that produce less than full coverage
                // even at the circle center.  If the arcwh are less than
                // 0.5 then the difference in area of the corner pixels
                // compared to a PGRAM primitive of the same size is just the
                // tiny corner cutout of area (4-PI)/16 which is less than
                // .06 pixels.  Thus, we can convert the primitive to a
                // PGRAM without any loss of precision.
                type = (type == MaskType.DRAW_ROUNDRECT)
                    ? MaskType.DRAW_PGRAM : MaskType.FILL_PGRAM;
            } else {
                float flatw = pwdist - oarcw;
                float flath = phdist - oarch;
                float ivalw, ivalh;
                if (type == MaskType.DRAW_ROUNDRECT) {
                    float iwdist = pwdist * ifractw;
                    float ihdist = phdist * ifracth;
                    // First we calculate the inner arc radii and see if they
                    // are large enough to render.
                    ivalw = iwdist - flatw;
                    ivalh = ihdist - flath;
                    // As above we need to fix things if we get inner arc
                    // radii below half a pixel.  We have a special shader
                    // for doing a "semi round" rect which has a round outer
                    // shell and a rectangular (pgram) inner shell...
                    if (ivalw < 0.5f || ivalh < 0.5f) {
                        // inner val is idim
                        ivalw = iwdist;
                        ivalh = ihdist;
                        type = MaskType.DRAW_SEMIROUNDRECT;
                    } else {
                        // inner val is invarcradii
                        ivalw = 1.0f / ivalw;
                        ivalh = 1.0f / ivalh;
                    }
                } else {
                    // Not used by FILL_ROUNDRECT, but we need constant
                    // values that will not cause an unnecessary vertex
                    // buffer flush in the validateOp below.
                    ivalw = ivalh = 0f;
                }
                oarcw = 1.0f / oarcw;
                oarch = 1.0f / oarch;
                Shader shader =
                    context.validatePaintOp(this, rendertx, type,
                                            rx, ry, rw, rh,
                                            oarcw, oarch,
                                            ivalw, ivalh, 0, 0);
                shader.setConstant("oinvarcradii", oarcw, oarch);
                if (type == MaskType.DRAW_ROUNDRECT) {
                    shader.setConstant("iinvarcradii", ivalw, ivalh);
                } else if (type == MaskType.DRAW_SEMIROUNDRECT) {
                    shader.setConstant("idim", ivalw, ivalh);
                }
                pwdist = flatw;
                phdist = flath;
            }
        } // no else here as we may have converted an RRECT to a PGRAM above
        if (type == MaskType.DRAW_PGRAM || type == MaskType.DRAW_ELLIPSE) {
            float idimw = pwdist * ifractw;
            float idimh = phdist * ifracth;
            if (type == MaskType.DRAW_ELLIPSE) {
                if (Math.abs(pwdist - phdist) < .01) {
                    type = MaskType.DRAW_CIRCLE;
                    // The phdist and idimh parameters will not be used by this
                    // shader, but we do need the maximum single pixel coverage
                    // so we coopt them to be min(1.0, area):
                    phdist = (float) Math.min(1.0, phdist * phdist * Math.PI);
                    idimh = (float) Math.min(1.0, idimh * idimh * Math.PI);
                } else {
                    // the ellipse drawing shader uses inverted arc dimensions
                    // to turn divides into multiplies
                    pwdist = 1.0f / pwdist;
                    phdist = 1.0f / phdist;
                    idimw = 1.0f / idimw;
                    idimh = 1.0f / idimh;
                }
            }
            Shader shader =
                context.validatePaintOp(this, rendertx, type,
                                        rx, ry, rw, rh,
                                        idimw, idimh, 0f, 0f, 0f, 0f);
            shader.setConstant("idim", idimw, idimh);
        } else if (type == MaskType.FILL_ELLIPSE) {
            if (Math.abs(pwdist - phdist) < .01) {
                type = MaskType.FILL_CIRCLE;
                // The phdist parameter will not be used by this shader,
                // but we do need the maximum single pixel contribution
                // so we coopt the value to be min(1.0, area):
                phdist = (float) Math.min(1.0, phdist * phdist * Math.PI);
            } else {
                // the ellipse filling shader uses inverted arc dimensions to
                // turn divides into multiplies:
                pwdist = 1.0f / pwdist;
                phdist = 1.0f / phdist;
                uUL *= pwdist;    vUL *= phdist;
                uUR *= pwdist;    vUR *= phdist;
                uLL *= pwdist;    vLL *= phdist;
                uLR *= pwdist;    vLR *= phdist;
            }
            context.validatePaintOp(this, rendertx, type, rx, ry, rw, rh);
        } else if (type == MaskType.FILL_PGRAM) {
            context.validatePaintOp(this, rendertx, type, rx, ry, rw, rh);
        }

        context.getVertexBuffer().addMappedPgram(xUL, yUL, xUR, yUR,
                                                 xLL, yLL, xLR, yLR,
                                                 uUL, vUL, uUR, vUR,
                                                 uLL, vLL, uLR, vLR,
                                                 pwdist, phdist);
    }

    AffineBase getPaintTextureTx(BaseTransform renderTx, Shader shader,
                                 float rx, float ry, float rw, float rh)
    {
        switch (paint.getType()) {
            case COLOR:
                return null;
            case LINEAR_GRADIENT:
                return PaintHelper.getLinearGradientTx((LinearGradient) paint,
                                                       shader, renderTx,
                                                       rx, ry, rw, rh);
            case RADIAL_GRADIENT:
                return PaintHelper.getRadialGradientTx((RadialGradient) paint,
                                                       shader, renderTx,
                                                       rx, ry, rw, rh);
            case IMAGE_PATTERN:
                return PaintHelper.getImagePatternTx(this, (ImagePattern) paint,
                                                     shader, renderTx,
                                                     rx, ry, rw, rh);
        }
        throw new InternalError("Unrecogized paint type: "+paint);
    }

    // The second set of rectangular bounds are for validating a
    // proportional paint.  They should be identical to the first
    // set for a fillRect() operation, but they may be different
    // for a vertical or horizontal drawLine() operation.
    boolean fillPrimRect(float x, float y, float w, float h,
                         Texture rectTex, Texture wrapTex,
                         float bx, float by, float bw, float bh)
    {
        BaseTransform xform = getTransformNoClone();
        float mxx = (float) xform.getMxx();
        float mxy = (float) xform.getMxy();
        float mxt = (float) xform.getMxt();
        float myx = (float) xform.getMyx();
        float myy = (float) xform.getMyy();
        float myt = (float) xform.getMyt();
        float dxdist = len(mxx, myx);
        float dydist = len(mxy, myy);
        if (dxdist == 0.0f || dydist == 0.0f) {
            // entire path has collapsed and occupies no area
            return true;
        }
        float pixelw = 1.0f / dxdist;
        float pixelh = 1.0f / dydist;
        float x0 = x - pixelw * 0.5f;
        float y0 = y - pixelh * 0.5f;
        float x1 = x + w + pixelw * 0.5f;
        float y1 = y + h + pixelh * 0.5f;
        int cellw = (int) Math.ceil(w * dxdist - 1.0f/512.0f);
        int cellh = (int) Math.ceil(h * dydist - 1.0f/512.0f);
        VertexBuffer vb = context.getVertexBuffer();
        int max = context.getRectTextureMaxSize();
        if (cellw <= max && cellh <= max) {
            float u0 = ((cellw * (cellw + 1)) / 2) - 0.5f;
            float v0 = ((cellh * (cellh + 1)) / 2) - 0.5f;
            float u1 = u0 + cellw + 1.0f;
            float v1 = v0 + cellh + 1.0f;
            u0 /= rectTex.getPhysicalWidth();
            v0 /= rectTex.getPhysicalHeight();
            u1 /= rectTex.getPhysicalWidth();
            v1 /= rectTex.getPhysicalHeight();
            if (xform.isTranslateOrIdentity()) {
                x0 += mxt;
                y0 += myt;
                x1 += mxt;
                y1 += myt;
                xform = IDENT;
            } else if (xform.is2D()) {
                Shader shader =
                    context.validatePaintOp(this, IDENT, MaskType.ALPHA_TEXTURE, rectTex,
                                            bx, by, bw, bh);
                AffineBase paintTx = getPaintTextureTx(IDENT, shader, bx, by, bw, bh);
                if (paintTx == null) {
                    vb.addMappedPgram(x0 * mxx + y0 * mxy + mxt, x0 * myx + y0 * myy + myt,
                                      x1 * mxx + y0 * mxy + mxt, x1 * myx + y0 * myy + myt,
                                      x0 * mxx + y1 * mxy + mxt, x0 * myx + y1 * myy + myt,
                                      x1 * mxx + y1 * mxy + mxt, x1 * myx + y1 * myy + myt,
                                      u0, v0, u1, v0, u0, v1, u1, v1, 0, 0);
                } else {
                    vb.addMappedPgram(x0 * mxx + y0 * mxy + mxt, x0 * myx + y0 * myy + myt,
                                      x1 * mxx + y0 * mxy + mxt, x1 * myx + y0 * myy + myt,
                                      x0 * mxx + y1 * mxy + mxt, x0 * myx + y1 * myy + myt,
                                      x1 * mxx + y1 * mxy + mxt, x1 * myx + y1 * myy + myt,
                                      u0, v0, u1, v0, u0, v1, u1, v1,
                                      x0, y0, x1, y1, paintTx);
                }
                return true;
            } else {
                System.out.println("Not a 2d transform!");
                mxt = myt = 0.0f;
            }
            Shader shader =
                context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE, rectTex,
                                        bx, by, bw, bh);
            AffineBase paintTx = getPaintTextureTx(IDENT, shader, bx, by, bw, bh);
            if (paintTx == null) {
                vb.addQuad(x0, y0, x1, y1,
                           u0, v0, u1, v1);
            } else {
                paintTx.translate(-mxt, -myt);
                vb.addQuad(x0, y0, x1, y1,
                           u0, v0, u1, v1,
                           paintTx);
            }
            return true;
        }
        if (wrapTex == null) {
            return false;
        }
        float u0 = 0.5f / wrapTex.getPhysicalWidth();
        float v0 = 0.5f / wrapTex.getPhysicalHeight();
        float uc = (cellw * 0.5f + 1.0f) / wrapTex.getPhysicalWidth();
        float vc = (cellh * 0.5f + 1.0f) / wrapTex.getPhysicalHeight();
        float xc = x + w * 0.5f;
        float yc = y + h * 0.5f;
        if (xform.isTranslateOrIdentity()) {
            x0 += mxt;
            y0 += myt;
            xc += mxt;
            yc += myt;
            x1 += mxt;
            y1 += myt;
            xform = IDENT;
        } else if (xform.is2D()) {
            Shader shader =
                context.validatePaintOp(this, IDENT, MaskType.ALPHA_TEXTURE, wrapTex,
                                        bx, by, bw, bh);
            AffineBase paintTx = getPaintTextureTx(IDENT, shader, bx, by, bw, bh);
            float mxx_x0 = mxx * x0, myx_x0 = myx * x0;
            float mxy_y0 = mxy * y0, myy_y0 = myy * y0;
            float mxx_xc = mxx * xc, myx_xc = myx * xc;
            float mxy_yc = mxy * yc, myy_yc = myy * yc;
            float mxx_x1 = mxx * x1, myx_x1 = myx * x1;
            float mxy_y1 = mxy * y1, myy_y1 = myy * y1;
            // xcc,ycc used in all 4 quads
            float xcc = mxx_xc + mxy_yc + mxt;
            float ycc = myx_xc + myy_yc + myt;
            // xcn, ycn and xnc, ync all used in 2 quads each
            float xc0 = mxx_xc + mxy_y0 + mxt;
            float yc0 = myx_xc + myy_y0 + myt;
            float x0c = mxx_x0 + mxy_yc + mxt;
            float y0c = myx_x0 + myy_yc + myt;
            float xc1 = mxx_xc + mxy_y1 + mxt;
            float yc1 = myx_xc + myy_y1 + myt;
            float x1c = mxx_x1 + mxy_yc + mxt;
            float y1c = myx_x1 + myy_yc + myt;
            // Note that all quads use same 00->c0->0c->cc coordinates for
            // the inner and outer uv texture coordinates regardless of the
            // reflection of the quad of vertex coordinates

            if (paintTx == null) {
                // quad1 - 00 -> c0 -> 0c -> cc
                vb.addMappedPgram(x0 * mxx + y0 * mxy + mxt, x0 * myx + y0 * myy + myt,
                                           xc0, yc0, x0c, y0c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc, 0, 0);
                // quad2 - 10 -> c0 -> 1c -> cc (reflect quad1 around x=c)
                vb.addMappedPgram(x1 * mxx + y0 * mxy + mxt, x1 * myx + y0 * myy + myt,
                                           xc0, yc0, x1c, y1c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc, 0, 0);
                // quad3 - 01 -> c1 -> 0c -> cc (reflect quad1 around y=c)
                vb.addMappedPgram(x0 * mxx + y1 * mxy + mxt, x0 * myx + y1 * myy + myt,
                                           xc1, yc1, x0c, y0c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc, 0, 0);
                // quad4 - 11 -> c1 -> 1c -> cc (reflect quad1 around x=c and y=c)
                vb.addMappedPgram(x1 * mxx + y1 * mxy + mxt, x1 * myx + y1 * myy + myt,
                                           xc1, yc1, x1c, y1c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc, 0, 0);
            } else {
                // quad1 - 00 -> c0 -> 0c -> cc
                vb.addMappedPgram(x0 * mxx + y0 * mxy + mxt, x0 * myx + y0 * myy + myt,
                                           xc0, yc0, x0c, y0c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                                  x0, y0, xc, yc, paintTx);
                // quad2 - 10 -> c0 -> 1c -> cc (reflect quad1 around x=c)
                vb.addMappedPgram(x1 * mxx + y0 * mxy + mxt, x1 * myx + y0 * myy + myt,
                                           xc0, yc0, x1c, y1c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                                  x1, y0, xc, yc, paintTx);
                // quad3 - 01 -> c1 -> 0c -> cc (reflect quad1 around y=c)
                vb.addMappedPgram(x0 * mxx + y1 * mxy + mxt, x0 * myx + y1 * myy + myt,
                                           xc1, yc1, x0c, y0c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                                  x0, y1, xc, yc, paintTx);
                // quad4 - 11 -> c1 -> 1c -> cc (reflect quad1 around x=c and y=c)
                vb.addMappedPgram(x1 * mxx + y1 * mxy + mxt, x1 * myx + y1 * myy + myt,
                                           xc1, yc1, x1c, y1c, xcc, ycc,
                                  u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                                  x1, y1, xc, yc, paintTx);
            }
            return true;
        } else {
            System.out.println("Not a 2d transform!");
            mxt = myt = 0;
        }
        Shader shader =
            context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE, wrapTex,
                                    bx, by, bw, bh);
        AffineBase paintTx = getPaintTextureTx(IDENT, shader, bx, by, bw, bh);
        if (paintTx != null) {
            paintTx.translate(-mxt, -myt);
        }
        vb.addQuad(x0, y0, xc, yc,
                   u0, v0, uc, vc,
                   paintTx);
        vb.addQuad(x1, y0, xc, yc,
                   u0, v0, uc, vc,
                   paintTx);
        vb.addQuad(x0, y1, xc, yc,
                   u0, v0, uc, vc,
                   paintTx);
        vb.addQuad(x1, y1, xc, yc,
                   u0, v0, uc, vc,
                   paintTx);
        return true;
    }

    boolean drawPrimRect(float x, float y, float w, float h) {
        float lw = stroke.getLineWidth();
        float pad = getStrokeExpansionFactor(stroke) * lw;
        BaseTransform xform = getTransformNoClone();
        float mxx = (float) xform.getMxx();
        float mxy = (float) xform.getMxy();
        float mxt = (float) xform.getMxt();
        float myx = (float) xform.getMyx();
        float myy = (float) xform.getMyy();
        float myt = (float) xform.getMyt();
        float dxdist = len(mxx, myx);
        float dydist = len(mxy, myy);
        if (dxdist == 0.0f || dydist == 0.0f) {
            // entire path has collapsed and occupies no area
            return true;
        }
        float pixelw = 1.0f / dxdist;
        float pixelh = 1.0f / dydist;
        float x0 = x - pad - pixelw * 0.5f;
        float y0 = y - pad - pixelh * 0.5f;
        float xc = x + w * 0.5f;
        float yc = y + h * 0.5f;
        float x1 = x + w + pad + pixelw * 0.5f;
        float y1 = y + h + pad + pixelh * 0.5f;
        Texture rTex = context.getWrapRectTexture();
        float wscale = 1.0f / rTex.getPhysicalWidth();
        float hscale = 1.0f / rTex.getPhysicalHeight();
        float ou0 = 0.5f * wscale;
        float ov0 = 0.5f * hscale;
        float ouc = ((w * 0.5f + pad) * dxdist + 1.0f) * wscale;
        float ovc = ((h * 0.5f + pad) * dydist + 1.0f) * hscale;
        float offsetx = lw * dxdist * wscale;
        float offsety = lw * dydist * hscale;
        VertexBuffer vb = context.getVertexBuffer();
        if (xform.isTranslateOrIdentity()) {
            x0 += mxt;
            y0 += myt;
            xc += mxt;
            yc += myt;
            x1 += mxt;
            y1 += myt;
            xform = IDENT;
        } else if (xform.is2D()) {
            Shader shader =
                context.validatePaintOp(this, IDENT, MaskType.ALPHA_TEXTURE_DIFF,
                                        rTex, x, y, w, h,
                                        offsetx, offsety, 0, 0, 0, 0);
            shader.setConstant("innerOffset", offsetx, offsety);
            AffineBase paintTx = getPaintTextureTx(IDENT, shader, x, y, w, h);
            float mxx_x0 = mxx * x0, myx_x0 = myx * x0;
            float mxy_y0 = mxy * y0, myy_y0 = myy * y0;
            float mxx_xc = mxx * xc, myx_xc = myx * xc;
            float mxy_yc = mxy * yc, myy_yc = myy * yc;
            float mxx_x1 = mxx * x1, myx_x1 = myx * x1;
            float mxy_y1 = mxy * y1, myy_y1 = myy * y1;

            // xcc,ycc used in all 4 quads
            float xcc = mxx_xc + mxy_yc + mxt;
            float ycc = myx_xc + myy_yc + myt;
            // xcn, ycn and xnc, ync all used in 2 quads each
            float xc0 = mxx_xc + mxy_y0 + mxt;
            float yc0 = myx_xc + myy_y0 + myt;
            float x0c = mxx_x0 + mxy_yc + mxt;
            float y0c = myx_x0 + myy_yc + myt;
            float xc1 = mxx_xc + mxy_y1 + mxt;
            float yc1 = myx_xc + myy_y1 + myt;
            float x1c = mxx_x1 + mxy_yc + mxt;
            float y1c = myx_x1 + myy_yc + myt;
            // Note that all quads use same 00->c0->0c->cc coordinates for
            // the inner and outer uv texture coordinates regardless of the
            // reflection of the quad of vertex coordinates

            if (paintTx == null) {
                // quad1 - 00 -> c0 -> 0c -> cc
                vb.addMappedPgram(mxx_x0 + mxy_y0 + mxt, myx_x0 + myy_y0 + myt,
                                            xc0, yc0, x0c, y0c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  0, 0);
                // quad2 - 10 -> c0 -> 1c -> cc (reflect quad1 around x=c)
                vb.addMappedPgram(mxx_x1 + mxy_y0 + mxt, myx_x1 + myy_y0 + myt,
                                            xc0, yc0, x1c, y1c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  0, 0);
                // quad3 - 01 -> c1 -> 0c -> cc (reflect quad1 around y=c)
                vb.addMappedPgram(mxx_x0 + mxy_y1 + mxt, myx_x0 + myy_y1 + myt,
                                            xc1, yc1, x0c, y0c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  0, 0);
                // quad4 - 11 -> c1 -> 1c -> cc (reflect quad1 around x=c and y=c)
                vb.addMappedPgram(mxx_x1 + mxy_y1 + mxt, myx_x1 + myy_y1 + myt,
                                            xc1, yc1, x1c, y1c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  0, 0);
            } else {
                // quad1 - 00 -> c0 -> 0c -> cc
                vb.addMappedPgram(mxx_x0 + mxy_y0 + mxt, myx_x0 + myy_y0 + myt,
                                            xc0, yc0, x0c, y0c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  x0, y0, xc, yc, paintTx);
                // quad2 - 10 -> c0 -> 1c -> cc (reflect quad1 around x=c)
                vb.addMappedPgram(mxx_x1 + mxy_y0 + mxt, myx_x1 + myy_y0 + myt,
                                            xc0, yc0, x1c, y1c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  x1, y0, xc, yc, paintTx);
                // quad3 - 01 -> c1 -> 0c -> cc (reflect quad1 around y=c)
                vb.addMappedPgram(mxx_x0 + mxy_y1 + mxt, myx_x0 + myy_y1 + myt,
                                            xc1, yc1, x0c, y0c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  x0, y1, xc, yc, paintTx);
                // quad4 - 11 -> c1 -> 1c -> cc (reflect quad1 around x=c and y=c)
                vb.addMappedPgram(mxx_x1 + mxy_y1 + mxt, myx_x1 + myy_y1 + myt,
                                            xc1, yc1, x1c, y1c, xcc, ycc,
                                  ou0, ov0, ouc, ov0, ou0, ovc, ouc, ovc,
                                  x1, y1, xc, yc, paintTx);
            }
            rTex.unlock();
            return true;
        } else {
            System.out.println("Not a 2d transform!");
            mxt = myt = 0.0f;
        }
        Shader shader =
            context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE_DIFF,
                                    rTex, x, y, w, h,
                                    offsetx, offsety, 0, 0, 0, 0);
        shader.setConstant("innerOffset", offsetx, offsety);
        AffineBase paintTx = getPaintTextureTx(IDENT, shader, x, y, w, h);
        if (paintTx != null) {
            paintTx.translate(-mxt, -myt);
        }
        vb.addQuad( x0,  y0,  xc,  yc,
                   ou0, ov0, ouc, ovc,
                   paintTx);
        vb.addQuad( x1,  y0,  xc,  yc,
                   ou0, ov0, ouc, ovc,
                   paintTx);
        vb.addQuad( x0,  y1,  xc,  yc,
                   ou0, ov0, ouc, ovc,
                   paintTx);
        vb.addQuad( x1,  y1,  xc,  yc,
                   ou0, ov0, ouc, ovc,
                   paintTx);
        rTex.unlock();
        return true;
    }

    boolean drawPrimDiagonal(float x1, float y1, float x2, float y2,
                             float lw, int cap,
                             float bx, float by, float bw, float bh)
    {
        // assert x1 != x2 && y1 != y2, otherwise caller would have
        // vectored us to fillPrimRect()
        if (stroke.getType() == BasicStroke.TYPE_CENTERED) {
            lw *= 0.5f;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = len(dx, dy);
        dx /= len;
        dy /= len;
        float ldx = dx * lw;
        float ldy = dy * lw;
        // First expand perpendicularly using (ldy, -ldx)
        float xUL = x1 + ldy,  yUL = y1 - ldx;
        float xUR = x2 + ldy,  yUR = y2 - ldx;
        float xLL = x1 - ldy,  yLL = y1 + ldx;
        float xLR = x2 - ldy,  yLR = y2 + ldx;
        if (cap == BasicStroke.CAP_SQUARE) {
            // Then add SQUARE end caps using (ldx, ldy) if needed
            xUL -= ldx;  yUL -= ldy;
            xLL -= ldx;  yLL -= ldy;
            xUR += ldx;  yUR += ldy;
            xLR += ldx;  yLR += ldy;
        }

        float hdx, hdy, vdx, vdy;
        int cellw, cellh;
        BaseTransform xform = getTransformNoClone();
        float mxt = (float) xform.getMxt();
        float myt = (float) xform.getMyt();
        if (xform.isTranslateOrIdentity()) {
            hdx = dx;  hdy =  dy;
            vdx = dy;  vdy = -dx;
            cellw = (int) Math.ceil(len(xUR - xUL, yUR - yUL));
            cellh = (int) Math.ceil(len(xLL - xUL, yLL - yUL));
            xform = IDENT;
        } else if (xform.is2D()) {
            float mxx = (float) xform.getMxx();
            float mxy = (float) xform.getMxy();
            float myx = (float) xform.getMyx();
            float myy = (float) xform.getMyy();
            float tx, ty;
            tx = mxx * xUL + mxy * yUL;
            ty = myx * xUL + myy * yUL;
            xUL = tx;  yUL = ty;
            tx = mxx * xUR + mxy * yUR;
            ty = myx * xUR + myy * yUR;
            xUR = tx;  yUR = ty;
            tx = mxx * xLL + mxy * yLL;
            ty = myx * xLL + myy * yLL;
            xLL = tx;  yLL = ty;
            tx = mxx * xLR + mxy * yLR;
            ty = myx * xLR + myy * yLR;
            xLR = tx;  yLR = ty;
            // hdx, hdy are transformed unit vectors along the line
            hdx = mxx * dx + mxy * dy;
            hdy = myx * dx + myy * dy;
            float dlen = len(hdx, hdy);
            if (dlen == 0.0f) return true;
            hdx /= dlen;
            hdy /= dlen;
            // vdx, vdy are transformed perpendicular unit vectors
            // (perpendicular to the line in user space, but then transformed)
            vdx = mxx * dy - mxy * dx;
            vdy = myx * dy - myy * dx;
            dlen = len(vdx, vdy);
            if (dlen == 0.0f) return true;
            vdx /= dlen;
            vdy /= dlen;
            cellw = (int) Math.ceil(Math.abs((xUR - xUL) * hdx + (yUR - yUL) * hdy));
            cellh = (int) Math.ceil(Math.abs((xLL - xUL) * vdx + (yLL - yUL) * vdy));
            xform = IDENT;
        } else {
            System.out.println("Not a 2d transform!");
            return false;
        }
        hdx *= 0.5f;
        hdy *= 0.5f;
        vdx *= 0.5f;
        vdy *= 0.5f;
        xUL = xUL + mxt + vdx - hdx;
        yUL = yUL + myt + vdy - hdy;
        xUR = xUR + mxt + vdx + hdx;
        yUR = yUR + myt + vdy + hdy;
        xLL = xLL + mxt - vdx - hdx;
        yLL = yLL + myt - vdy - hdy;
        xLR = xLR + mxt - vdx + hdx;
        yLR = yLR + myt - vdy + hdy;
        VertexBuffer vb = context.getVertexBuffer();
        int cellmax = context.getRectTextureMaxSize();
        if (cellh <= cellmax) {
            float v0 = ((cellh * (cellh + 1)) / 2) - 0.5f;
            float v1 = v0 + cellh + 1.0f;
            Texture rTex = context.getRectTexture();
            v0 /= rTex.getPhysicalHeight();
            v1 /= rTex.getPhysicalHeight();
            if (cellw <= cellmax) {
                float u0 = ((cellw * (cellw + 1)) / 2) - 0.5f;
                float u1 = u0 + cellw + 1.0f;
                u0 /= rTex.getPhysicalWidth();
                u1 /= rTex.getPhysicalWidth();
                context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE, rTex,
                                        bx, by, bw, bh);
                vb.addMappedPgram(xUL, yUL, xUR, yUR, xLL, yLL, xLR, yLR,
                                   u0,  v0,  u1,  v0,  u0,  v1,  u1,  v1,
                                  0, 0);
//                System.out.print("1"); System.out.flush();
                rTex.unlock();
                return true;
            }
            // long thin line (cellw is along the line, cellh is across it)
            if (cellw <= cellmax * 2 - 1) {
                // 2-slice the line at its midpoint.
                // use the cellmax,cellh cell for maximum coverage of each half
                // we can use at most (cellmax-0.5) per half so that we do not
                // see the antialias drop off on the last half pixel of the far
                // end of the cell.  This lets us support a line of "length" up
                // to (cellmax-0.5 + cellmax-0.5) or (cellmax*2-1).
                float xUC = (xUL + xUR) * 0.5f;
                float yUC = (yUL + yUR) * 0.5f;
                float xLC = (xLL + xLR) * 0.5f;
                float yLC = (yLL + yLR) * 0.5f;
                float u0 = ((cellmax * (cellmax + 1)) / 2) - 0.5f;
                float u1 = u0 + 0.5f + cellw * 0.5f;
                u0 /= rTex.getPhysicalWidth();
                u1 /= rTex.getPhysicalWidth();
                context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE, rTex,
                                        bx, by, bw, bh);
                // first half of line x1,y1 -> midpoint
                vb.addMappedPgram(xUL, yUL, xUC, yUC, xLL, yLL, xLC, yLC,
                                   u0,  v0,  u1,  v0,  u0,  v1,  u1,  v1,
                                  0, 0);
                // second half of line midpoint -> x2,y2
                vb.addMappedPgram(xUR, yUR, xUC, yUC, xLR, yLR, xLC, yLC,
                                   u0,  v0,  u1,  v0,  u0,  v1,  u1,  v1,
                                  0, 0);
//                System.out.print("2"); System.out.flush();
                rTex.unlock();
                return true;
            }
            // Finally, 3-slice the line (left edge, huge middle, right edge)
            float u0 = 0.5f / rTex.getPhysicalWidth();
            float u1 = 1.5f / rTex.getPhysicalWidth();
            // The lower case L or R indicates "inner left" or "inner right"
            hdx *= 2.0f;
            hdy *= 2.0f;
            float xUl = xUL + hdx;
            float yUl = yUL + hdy;
            float xUr = xUR - hdx;
            float yUr = yUR - hdy;
            float xLl = xLL + hdx;
            float yLl = yLL + hdy;
            float xLr = xLR - hdx;
            float yLr = yLR - hdy;
            context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE, rTex,
                                    bx, by, bw, bh);
            // first pixel of line x1,y1 -> x1,y1+pixel
            vb.addMappedPgram(xUL, yUL, xUl, yUl, xLL, yLL, xLl, yLl,
                               u0,  v0,  u1,  v0,  u0,  v1,  u1,  v1,
                              0, 0);
            // middle part of line x1,y1+pixel -> x2,y2-pixel
            vb.addMappedPgram(xUl, yUl, xUr, yUr, xLl, yLl, xLr, yLr,
                               u1,  v0,  u1,  v0,  u1,  v1,  u1,  v1,
                              0, 0);
            // last part of line x2,y2-pixel -> x2,y2
            vb.addMappedPgram(xUr, yUr, xUR, yUR, xLr, yLr, xLR, yLR,
                               u1,  v0,  u0,  v0,  u1,  v1,  u0,  v1,
                              0, 0);
//            System.out.print("3"); System.out.flush();
            rTex.unlock();
            return true;
        }
        // we could 2 and 3 slice extremely wide short lines, but they
        // are very rare in practice so we just jump straight to a
        // standard 4-slice off of the wrap-rect texture
        float xUC = (xUL + xUR) * 0.5f;
        float yUC = (yUL + yUR) * 0.5f;
        float xLC = (xLL + xLR) * 0.5f;
        float yLC = (yLL + yLR) * 0.5f;
        float xCL = (xUL + xLL) * 0.5f;
        float yCL = (yUL + yLL) * 0.5f;
        float xCR = (xUR + xLR) * 0.5f;
        float yCR = (yUR + yLR) * 0.5f;
        float xCC = (xUC + xLC) * 0.5f;
        float yCC = (yUC + yLC) * 0.5f;
        Texture rTex = context.getWrapRectTexture();
        float u0 = 0.5f / rTex.getPhysicalWidth();
        float v0 = 0.5f / rTex.getPhysicalHeight();
        float uc = (cellw * 0.5f + 1.0f) / rTex.getPhysicalWidth();
        float vc = (cellh * 0.5f + 1.0f) / rTex.getPhysicalHeight();
        context.validatePaintOp(this, xform, MaskType.ALPHA_TEXTURE, rTex,
                                bx, by, bw, bh);
        vb.addMappedPgram(xUL, yUL, xUC, yUC, xCL, yCL, xCC, yCC,
                            u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                            0, 0);
        vb.addMappedPgram(xUR, yUR, xUC, yUC, xCR, yCR, xCC, yCC,
                            u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                            0, 0);
        vb.addMappedPgram(xLL, yLL, xLC, yLC, xCL, yCL, xCC, yCC,
                            u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                            0, 0);
        vb.addMappedPgram(xLR, yLR, xLC, yLC, xCR, yCR, xCC, yCC,
                            u0,  v0,  uc,  v0,  u0,  vc,  uc,  vc,
                            0, 0);
//        System.out.print("4"); System.out.flush();
        rTex.unlock();
        return true;
    }

    public void fillRect(float x, float y, float w, float h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (!isAntialiasedShape()) {
           fillQuad(x, y, x + w, y + h);
           return;
        }
        if (isComplexPaint) {
            scratchRRect.setRoundRect(x, y, w, h, 0, 0);
            renderWithComplexPaint(scratchRRect, null, x, y, w, h);
            return;
        }
        if (PrismSettings.primTextureSize != 0) {
            Texture rTex = context.getRectTexture();
            Texture wTex = context.getWrapRectTexture();
            boolean success = fillPrimRect(x, y, w, h, rTex, wTex, x, y, w, h);
            rTex.unlock();
            wTex.unlock();
            if (success) return;
        }
        renderGeneralRoundedRect(x, y, w, h, 0f, 0f,
                                 MaskType.FILL_PGRAM, null);
    }

    public void fillEllipse(float x, float y, float w, float h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (isComplexPaint) {
            scratchEllipse.setFrame(x, y, w, h);
            renderWithComplexPaint(scratchEllipse, null, x, y, w, h);
            return;
        }
        if (!isAntialiasedShape()) {
            scratchEllipse.setFrame(x, y, w, h);
            renderShape(scratchEllipse, null, x, y, w, h);
            return;
        }
        if (PrismSettings.primTextureSize != 0) {
            if (fillPrimRect(x, y, w, h,
                             context.getOvalTexture(),
                             null,
                             x, y, w, h))
            {
                return;
            }
        }
        renderGeneralRoundedRect(x, y, w, h, w, h,
                                 MaskType.FILL_ELLIPSE, null);
    }

    public void fillRoundRect(float x, float y, float w, float h,
                              float arcw, float arch)
    {
        arcw = Math.min(Math.abs(arcw), w);
        arch = Math.min(Math.abs(arch), h);

        if (w <= 0 || h <= 0) {
            return;
        }
        if (isComplexPaint) {
            scratchRRect.setRoundRect(x, y, w, h, arcw, arch);
            renderWithComplexPaint(scratchRRect, null, x, y, w, h);
            return;
        }
        if (!isAntialiasedShape()) {
            scratchRRect.setRoundRect(x, y, w, h, arcw, arch);
            renderShape(scratchRRect, null, x, y, w, h);
            return;
        }
        renderGeneralRoundedRect(x, y, w, h, arcw, arch,
                                 MaskType.FILL_ROUNDRECT, null);
    }

    public void fillQuad(float x1, float y1, float x2, float y2) {
        float bx, by, bw, bh;
        if (x1 <= x2) {
            bx = x1;
            bw = x2 - x1;
        } else {
            bx = x2;
            bw = x1 - x2;
        }
        if (y1 <= y2) {
            by = y1;
            bh = y2 - y1;
        } else {
            by = y2;
            bh = y1 - y2;
        }

        if (isComplexPaint) {
            scratchRRect.setRoundRect(bx, by, bw, bh, 0, 0);
            renderWithComplexPaint(scratchRRect, null, bx, by, bw, bh);
            return;
        }

        BaseTransform xform = getTransformNoClone();
        if (PrismSettings.primTextureSize != 0) {
            float mxt, myt;
            if (xform.isTranslateOrIdentity()) {
                mxt = (float) xform.getMxt();
                myt = (float) xform.getMyt();
                xform = IDENT;
                x1 += mxt;
                y1 += myt;
                x2 += mxt;
                y2 += myt;
            } else {
                mxt = myt = 0.0f;
            }
            Shader shader =
                context.validatePaintOp(this, xform, MaskType.ALPHA_ONE, null,
                                        bx, by, bw, bh);
            AffineBase paintTx = getPaintTextureTx(IDENT, shader, bx, by, bw, bh);
            if (paintTx != null) {
                paintTx.translate(-mxt, -myt);
            }
            context.getVertexBuffer().addQuad(x1, y1, x2, y2, 0, 0, 0, 0, paintTx);
            return;
        }
        if (isSimpleTranslate) {
            xform = IDENT;
            bx += transX;
            by += transY;
        }
        context.validatePaintOp(this, xform, MaskType.SOLID, bx, by, bw, bh);

        VertexBuffer vb = context.getVertexBuffer();
        vb.addQuad(bx, by, bx+bw, by+bh);
    }

    private static final double SQRT_2 = Math.sqrt(2.0);
    private static boolean canUseStrokeShader(BasicStroke bs) {
        // RT-27378
        // TODO: Expand the cases that renderGeneralRoundRect() can handle...
        return (!bs.isDashed() &&
                (bs.getType() == BasicStroke.TYPE_INNER ||
                 bs.getLineJoin() == BasicStroke.JOIN_ROUND ||
                 (bs.getLineJoin() == BasicStroke.JOIN_MITER &&
                  bs.getMiterLimit() >= SQRT_2)));
    }

    public void blit(RTTexture srcTex, RTTexture dstTex,
                     int srcX0, int srcY0, int srcX1, int srcY1,
                     int dstX0, int dstY0, int dstX1, int dstY1) {
        if (dstTex == null) {
            context.setRenderTarget(this);
        } else {
            context.setRenderTarget((BaseGraphics)dstTex.createGraphics());
        }
        context.blit(srcTex, dstTex, srcX0, srcY0, srcX1, srcY1,
                dstX0, dstY0, dstX1, dstY1);
    }

    public void drawRect(float x, float y, float w, float h) {
        if (w < 0 || h < 0) {
            return;
        }
        if (w == 0 || h == 0) {
            drawLine(x, y, x + w, y + h);
            return;
        }
        if (isComplexPaint) {
            scratchRRect.setRoundRect(x, y, w, h, 0, 0);
            renderWithComplexPaint(scratchRRect, stroke, x, y, w, h);
            return;
        }
        if (!isAntialiasedShape()) {
            scratchRRect.setRoundRect(x, y, w, h, 0, 0);
            renderShape(scratchRRect, stroke, x, y, w, h);
            return;
        }
        if (canUseStrokeShader(stroke)) {
            if (PrismSettings.primTextureSize != 0 &&
                stroke.getLineJoin() != BasicStroke.CAP_ROUND)
            {
                if (drawPrimRect(x, y, w, h)) {
                    return;
                }
            }
            renderGeneralRoundedRect(x, y, w, h, 0f, 0f,
                                     MaskType.DRAW_PGRAM, stroke);
            return;
        }
        scratchRRect.setRoundRect(x, y, w, h, 0, 0);
        renderShape(scratchRRect, stroke, x, y, w, h);
    }

    private boolean checkInnerCurvature(float arcw, float arch) {
        // Test to see if inner ellipse satisfies (flattening < 0.5)
        // otherwise it will not be approximated by a "parallel ellipse"
        // very well and we should just use shape rendering.

        // RT-27378
        // TODO: Implement better "distance to ellipse" formulas in the shaders
        float inset = stroke.getLineWidth() *
            (1f - getStrokeExpansionFactor(stroke));
        arcw -= inset;
        arch -= inset;
        // Note that if either inset arcw,h go to <= 0 then we will invoke the
        // fill primitive for ellipse, or the round rect primitive will
        // invoke its "tiny inner corner" fixes and we will be safe
        return (arcw <= 0 || arch <= 0 ||
                (arcw * 2f > arch && arch * 2f > arcw));
    }

    public void drawEllipse(float x, float y, float w, float h) {
        if (w < 0 || h < 0) {
            return;
        }
        if (!isComplexPaint && !stroke.isDashed() &&
            checkInnerCurvature(w, h) && isAntialiasedShape())
        {
            renderGeneralRoundedRect(x, y, w, h, w, h,
                                     MaskType.DRAW_ELLIPSE, stroke);
            return;
        }
        scratchEllipse.setFrame(x, y, w, h);
        renderShape(scratchEllipse, stroke, x, y, w, h);
    }

    public void drawRoundRect(float x, float y, float w, float h,
                              float arcw, float arch)
    {
        arcw = Math.min(Math.abs(arcw), w);
        arch = Math.min(Math.abs(arch), h);

        if (w < 0 || h < 0) {
            return;
        }
        if (!isComplexPaint && !stroke.isDashed() &&
            checkInnerCurvature(arcw, arch) && isAntialiasedShape())
        {
            renderGeneralRoundedRect(x, y, w, h, arcw, arch,
                                     MaskType.DRAW_ROUNDRECT, stroke);
            return;
        }
        scratchRRect.setRoundRect(x, y, w, h, arcw, arch);
        renderShape(scratchRRect, stroke, x, y, w, h);
    }

    public void drawLine(float x1, float y1, float x2, float y2) {
        float bx, by, bw, bh;
        if (x1 <= x2) {
            bx = x1;
            bw = x2 - x1;
        } else {
            bx = x2;
            bw = x1 - x2;
        }
        if (y1 <= y2) {
            by = y1;
            bh = y2 - y1;
        } else {
            by = y2;
            bh = y1 - y2;
        }

        // RT-27378
        // TODO: casting down to floats everywhere here; evaluate later
        // to see if this is enough precision...
        // TODO: stroke normalization control?
        if (stroke.getType() == BasicStroke.TYPE_INNER) {
            return;
        }
        if (isComplexPaint) {
            scratchLine.setLine(x1, y1, x2, y2);
            renderWithComplexPaint(scratchLine, stroke, bx, by, bw, bh);
            return;
        }
        if (!isAntialiasedShape()) {
            scratchLine.setLine(x1, y1, x2, y2);
            renderShape(scratchLine, stroke, bx, by, bw, bh);
            return;
        }
        int cap = stroke.getEndCap();
        if (stroke.isDashed()) {
            // NOTE: we could construct the GeneralPath directly
            // for CAP_ROUND and save a lot of processing in that case...
            // And again, we would need to deal with dropout control...
            scratchLine.setLine(x1, y1, x2, y2);
            renderShape(scratchLine, stroke, bx, by, bw, bh);
            return;
        }
        float lw = stroke.getLineWidth();
        if (PrismSettings.primTextureSize != 0 &&
            cap != BasicStroke.CAP_ROUND)
        {
            float pad = lw;
            if (stroke.getType() == BasicStroke.TYPE_CENTERED) {
                pad *= 0.5f;
            }
            if (bw == 0.0f || bh == 0.0f) {
                float padx, pady;
                if (cap == BasicStroke.CAP_SQUARE) {
                    // CAP_SQUARE pads the same on all sides
                    padx = pady = pad;
//                    System.out.print("S"); System.out.flush();
                } else if (bw != 0.0f) {
                    // Horizontal CAP_BUTT line - widen vertically
                    padx = 0.0f;
                    pady = pad;
//                    System.out.print("H"); System.out.flush();
                } else if (bh != 0.0f) {
                    // Vertical CAP_BUTT line - widen horizontally
                    padx = pad;
                    pady = 0.0f;
//                    System.out.print("V"); System.out.flush();
                } else {
//                    System.out.print("0"); System.out.flush();
                    // Zero length line - NOP for CAP_BUTT
                    return;
                }
                Texture rTex = context.getRectTexture();
                Texture wTex = context.getWrapRectTexture();
                boolean success = fillPrimRect(bx - padx,        by - pady,
                                               bw + padx + padx, bh + pady + pady,
                                               rTex, wTex, bx, by, bw, bh);
                rTex.unlock();
                wTex.unlock();
                if (success) return;
            } else {
                if (drawPrimDiagonal(x1, y1, x2, y2, lw, cap,
                                     bx, by, bw, bh))
                {
                    return;
                }
            }
        }
//        System.out.print("#"); System.out.flush();
        if (stroke.getType() == BasicStroke.TYPE_OUTER) {
            lw *= 2f;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = len(dx, dy);
        float ldx, ldy;  // lw length vector in direction of line in user space
        if (len == 0) {
            if (cap == BasicStroke.CAP_BUTT) {
                return;
            }
            ldx = lw;
            ldy = 0;
        } else {
            ldx = lw * dx / len;
            ldy = lw * dy / len;
        }
        // The following is safe; this method does not mutate the transform
        BaseTransform xform = getTransformNoClone();
        BaseTransform rendertx;
        float pdx, pdy;  // ldx,ldy rotated 90 in user space then transformed
        if (isSimpleTranslate) {
            double tx = xform.getMxt();
            double ty = xform.getMyt();
            x1 += tx;
            y1 += ty;
            x2 += tx;
            y2 += ty;
            pdx = ldy;
            pdy = -ldx;
            rendertx = IDENT;
        } else {
            rendertx = extract3Dremainder(xform);
            double coords[] = {x1, y1, x2, y2};
            xform.transform(coords, 0, coords, 0, 2);
            x1 = (float)coords[0];
            y1 = (float)coords[1];
            x2 = (float)coords[2];
            y2 = (float)coords[3];
            dx = x2 - x1;
            dy = y2 - y1;
            coords[0] = ldx;
            coords[1] = ldy;
            coords[2] = ldy;
            coords[3] = -ldx;
            xform.deltaTransform(coords, 0, coords, 0, 2);
            ldx = (float) coords[0];
            ldy = (float) coords[1];
            pdx = (float) coords[2];
            pdy = (float) coords[3];
        }
        float px = x1 - pdx / 2f;
        float py = y1 - pdy / 2f;
        float arcfractw, arcfracth;
        MaskType type;
        if (cap != BasicStroke.CAP_BUTT) {
            px -= ldx / 2f;
            py -= ldy / 2f;
            dx += ldx;
            dy += ldy;
            if (cap == BasicStroke.CAP_ROUND) {
                arcfractw = len(ldx, ldy) / len(dx, dy);
                arcfracth = 1f;
                type = MaskType.FILL_ROUNDRECT;
            } else {
                arcfractw = arcfracth = 0f;
                type = MaskType.FILL_PGRAM;
            }
        } else {
            arcfractw = arcfracth = 0f;
            type = MaskType.FILL_PGRAM;
        }
        renderGeneralRoundedPgram(px, py, dx, dy, pdx, pdy,
                                  arcfractw, arcfracth, 0f, 0f,
                                  rendertx, type,
                                  bx, by, bw, bh);
    }

    private static float len(float x, float y) {
        return ((x == 0f) ? Math.abs(y)
                : ((y == 0f) ? Math.abs(x)
                   : (float)Math.sqrt(x * x + y * y)));
    }

    private boolean lcdSampleInvalid = false;

    public void setNodeBounds(RectBounds bounds) {
        nodeBounds = bounds;
        lcdSampleInvalid = bounds != null;
    }

    private void initLCDSampleRT() {
        if (lcdSampleInvalid) {
            RectBounds textBounds = new RectBounds();
            getTransformNoClone().transform(nodeBounds, textBounds);
            Rectangle clipRect = getClipRectNoClone();
            if (clipRect != null && !clipRect.isEmpty()) {
                // Reduce sample area if there is any clipping bounds set
                textBounds.intersectWith(clipRect);
            }
            // LCD mixing with background will often lead to extra pixel at edge
            // thus adding a single pixel, as padding, at edges and bottom.
            float bx = textBounds.getMinX() - 1.0f;
            float by = textBounds.getMinY() - 1.0f;
            float bw = textBounds.getWidth() + 2.0f;
            float bh = textBounds.getHeight() + 2.0f;

            context.validateLCDBuffer(getRenderTarget());

            // Create a graphics for us to render into the LCDBuffer
            // Note this also sets the current RenderTarget as the LCDBuffer
            BaseShaderGraphics bsg = (BaseShaderGraphics) context.getLCDBuffer().createGraphics();
            bsg.setCompositeMode(CompositeMode.SRC);
            context.validateLCDOp(bsg, IDENT, (Texture) getRenderTarget(), null, true, null);

            int srch = getRenderTarget().getPhysicalHeight();
            int srcw = getRenderTarget().getPhysicalWidth();
            float tx1 = bx / srcw;
            float ty1 = by / srch;
            float tx2 = (bx + bw) / srcw;
            float ty2 = (by + bh) / srch;

            //sample the source RT in the following bounds and store it in the LCDBuffer RT.
            bsg.drawLCDBuffer(bx, by, bw, bh, tx1, ty1, tx2, ty2);
            context.setRenderTarget(this);
        }
        lcdSampleInvalid = false;
    }

    public void drawString(GlyphList gl, FontStrike strike, float x, float y,
                           Color selectColor, int selectStart, int selectEnd) {

        if (isComplexPaint ||
            paint.getType().isImagePattern() ||
            strike.drawAsShapes())
        {
            // FontStrike.drawAsShapes() may be true for very large font sizes
            // in which case so no glyph images are cached.
            // The Prism Text node handles such cases directly, but Webnode relies
            // upon Prism's drawString method, so we need to handle it here too.

            // this is not a very optimal approach and may not hit exactly
            // the same pixels that we would hit in the case where the
            // glyph cache is used, but the complex paint case is not
            // common enough to warrant further optimization
            BaseTransform xform = BaseTransform.getTranslateInstance(x, y);
            Shape shape = strike.getOutline(gl, xform);
            fill(shape);
            return;
        }

        BaseTransform xform = getTransformNoClone();

        Paint textPaint = getPaint();
        Color textColor = textPaint.getType() == Paint.Type.COLOR ?
                          (Color) textPaint : null;

        CompositeMode blendMode = getCompositeMode();
        // LCD support requires several attributes to function:
        // FontStrike supports LCD, SRC_OVER CompositeMode and Paint is a COLOR
        boolean lcdSupported = blendMode == CompositeMode.SRC_OVER &&
                               textColor != null &&
                               xform.is2D() &&
                               !getRenderTarget().isMSAA();

        /* If the surface can't support LCD text we need to replace an
         * LCD mode strike with the equivalent grey scale one.
         */
        if (strike.getAAMode() == FontResource.AA_LCD && !lcdSupported) {
            FontResource fr = strike.getFontResource();
            float size = strike.getSize();
            BaseTransform tx = strike.getTransform();
            strike = fr.getStrike(size, tx, FontResource.AA_GREYSCALE);
        }

        float bx = 0f, by = 0f, bw = 0f, bh = 0f;
        if (paint.getType().isGradient() && ((Gradient)paint).isProportional()) {
            // If drawString is called directly without using setNodeBounds,
            // then nodeBounds is null, and we must determine the bounds based
            // on the str(vs. the node).
            RectBounds textBounds = nodeBounds;
            if (textBounds == null) {
                Metrics m = strike.getMetrics();
                float pad = -m.getAscent() * 0.4f;
                textBounds = new RectBounds(-pad,
                                            m.getAscent(),
                                            gl.getWidth() + 2.0f *pad,
                                            m.getDescent() + m.getLineGap());
                bx = x;
                by = y;
            }

            bx += textBounds.getMinX();
            by += textBounds.getMinY();
            bw = textBounds.getWidth();
            bh = textBounds.getHeight();
        }

        BaseBounds clip = null;
        Point2D p2d = new Point2D(x, y);
        if (isSimpleTranslate) {
            /* Only use clip for simple transforms so that coordinates in the
             * glyph list (user space) and the coordinates of the clip
             * (device space) can be intersected.
             */
            clip = getFinalClipNoClone();
            xform = IDENT;
            p2d.x += transX;
            p2d.y += transY;
        }

        /* Cache look up needs to be on the font as rendered, including
         * AA mode, metrics mode, glyph transform (pt size combined with
         * the full graphics transform). Most of this info is expected to
         * be in the font, which here is close to being a full strike
         * description.
         */
        GlyphCache glyphCache = context.getGlyphCache(strike);
        Texture cacheTex = glyphCache.getBackingStore();

        //Since we currently cannot support LCD text on transparant surfaces, we
        //verify that we are drawing to an opaque surface.
        if (strike.getAAMode() == FontResource.AA_LCD) {
            if (nodeBounds == null) {
                // If drawString is called directly without using
                // setNodeBounds then we must determine the bounds of the str,
                // before we render background to texture.
                // This is slow, but required by webnode.

                Metrics m = strike.getMetrics();
                // Ruff guess for padding, since lots of glyphs exceed advance
                RectBounds textBounds =
                        new RectBounds(x - 2,
                                       y + m.getAscent(),
                                       x + 2 + gl.getWidth(),
                                       y + 1 + m.getDescent() + m.getLineGap());

                setNodeBounds(textBounds);
                initLCDSampleRT();
                setNodeBounds(null);
            } else {
                initLCDSampleRT();
            }
            float invgamma = PrismFontFactory.getLCDContrast();
            float gamma = 1.0f/invgamma;
            textColor = new Color((float)Math.pow(textColor.getRed(),   invgamma),
                                  (float)Math.pow(textColor.getGreen(), invgamma),
                                  (float)Math.pow(textColor.getBlue(),  invgamma),
                                  (float)Math.pow(textColor.getAlpha(), invgamma));
            if (selectColor != null) {
                selectColor = new Color(
                        (float)Math.pow(selectColor.getRed(),   invgamma),
                        (float)Math.pow(selectColor.getGreen(), invgamma),
                        (float)Math.pow(selectColor.getBlue(),  invgamma),
                        (float)Math.pow(selectColor.getAlpha(), invgamma));
            }

            // In order to handle transparency, the LCD shader need to manually
            // composite source with destination. Thus, SRC_OVER compositing
            // needs to be set to SRC, while shader is active.
            setCompositeMode(CompositeMode.SRC);

            //set our 2nd LCD shader.
            Shader shader = context.validateLCDOp(this, IDENT,
                                                context.getLCDBuffer(),
                                                cacheTex, false, textColor);

            float unitXCoord = 1.0f/((float)cacheTex.getPhysicalWidth());
            shader.setConstant("gamma", gamma, invgamma, unitXCoord);
            setCompositeMode(blendMode); // Restore composite mode
        } else {
            context.validatePaintOp(this, IDENT, cacheTex, bx, by, bw, bh);
        }
        if (isSimpleTranslate) {
            // Applying this rounding allows for smoother text animation,
            // when animating simple translated text.
            // Asking glyph textures to be rendered at non-integral
            // locations produces very poor text. This doesn't solve
            // the problem for scaled (etc) cases, but addresses a
            // common case.
            p2d.y = Math.round(p2d.y);
            p2d.x = Math.round(p2d.x);
        }
        glyphCache.render(context, gl, p2d.x, p2d.y, selectStart, selectEnd,
                          selectColor, textColor, xform, clip);
    }

    //This function is used by the LCD path to render a quad into the
    //LCD RTT Texture. here the presentable is set as input and
    //sampled using texture coordinates. This is later used in a
    //second pass to provide the dst color as input.
    private void drawLCDBuffer(float bx, float by, float bw, float bh,
            float tx1, float ty1, float tx2, float ty2)
    {
        context.setRenderTarget(this);
        context.getVertexBuffer().addQuad(bx, by, bx + bw, by + bh, tx1, ty1, tx2, ty2);
    }

    public boolean canReadBack() {
        RenderTarget rt = getRenderTarget();
        return rt instanceof ReadbackRenderTarget &&
            ((ReadbackRenderTarget) rt).getBackBuffer() != null;
    }

    public RTTexture readBack(Rectangle view) {
        RenderTarget rt = getRenderTarget();
        context.flushVertexBuffer();
        context.validateLCDBuffer(rt);
        RTTexture lcdrtt = context.getLCDBuffer();
        Texture bbtex = ((ReadbackRenderTarget) rt).getBackBuffer();

        float x1 = view.x;
        float y1 = view.y;
        float x2 = x1 + view.width;
        float y2 = y1 + view.height;

        // Create a graphics for us to render into the LCDBuffer
        // Note this also sets the current RenderTarget as the LCDBuffer
        BaseShaderGraphics bsg = (BaseShaderGraphics) lcdrtt.createGraphics();
        bsg.setCompositeMode(CompositeMode.SRC);
        context.validateTextureOp(bsg, IDENT, bbtex, bbtex.getPixelFormat());

        // sample the source RT in the following bounds and store it in the LCDBuffer RT.
        bsg.drawTexture(bbtex, 0, 0, view.width, view.height, x1, y1, x2, y2);
        context.flushVertexBuffer();

        // set the RenderTarget back to this.
        context.setRenderTarget(this);
        return lcdrtt;
    }

    public void releaseReadBackBuffer(RTTexture rtt) {
        // This will be needed when we track LCD buffer locks and uses.
        // (See RT-29488)
//        context.releaseLCDBuffer();
    }

    public void setup3DRendering() {
        context.setRenderTarget(this);
    }
}
