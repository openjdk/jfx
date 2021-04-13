/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.image.ByteToBytePixelConverter;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.RenderTarget;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.paint.PaintUtil;
import com.sun.prism.impl.shape.MaskData;
import com.sun.prism.paint.Gradient;

public abstract class BaseContext {

    private final Screen screen;
    private final ResourceFactory factory;
    private final VertexBuffer vertexBuffer;

    private boolean disposed = false;

    private static final int MIN_MASK_DIM = 1024;
    private Texture maskTex;
    private ByteBuffer maskBuffer;
    private ByteBuffer clearBuffer;
    private int curMaskRow;
    private int nextMaskRow;
    private int curMaskCol;
    private int highMaskCol;
    private Texture paintTex;
    private int[] paintPixels;
    private ByteBuffer paintBuffer;

    private Texture rectTex;
    private int rectTexMax;
    private Texture wrapRectTex;
    private Texture ovalTex;

    private final GeneralTransform3D perspectiveTransform = new GeneralTransform3D();

    private final Map<FontStrike, GlyphCache>
        greyGlyphCaches = new HashMap<FontStrike, GlyphCache>();
    private final Map<FontStrike, GlyphCache>
        lcdGlyphCaches = new HashMap<FontStrike, GlyphCache>();

    protected BaseContext(Screen screen, ResourceFactory factory, int vbQuads) {
        this.screen = screen;
        this.factory = factory;
        this.vertexBuffer = new VertexBuffer(this, vbQuads);
    }

    protected void setDeviceParametersFor2D() {}
    protected void setDeviceParametersFor3D() {}

    public Screen getAssociatedScreen() {
        return screen;
    }

    public ResourceFactory getResourceFactory() {
        return factory;
    }

    public VertexBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public void flushVertexBuffer() {
        if (checkDisposed()) return;

        vertexBuffer.flush();
    }

    protected final void flushMask() {
        if (curMaskRow > 0 || curMaskCol > 0) {
            maskTex.lock();
            // assert !maskTex.isSurfaceLost();
            // since it was bound and unflushed...
            maskTex.update(maskBuffer, maskTex.getPixelFormat(),
                                       0, 0, 0, 0, highMaskCol, nextMaskRow,
                                       maskTex.getContentWidth(), true);
            maskTex.unlock();
            curMaskRow = curMaskCol = nextMaskRow = highMaskCol = 0;
        }
    }

    public void drawQuads(float coordArray[], byte colorArray[], int numVertices) {
        flushMask();
        renderQuads(coordArray, colorArray, numVertices);
    }

    protected GeneralTransform3D getPerspectiveTransformNoClone() {
        return perspectiveTransform;
    }

    protected void setPerspectiveTransform(GeneralTransform3D transform) {
        if (transform == null) {
            perspectiveTransform.setIdentity();
        } else {
            perspectiveTransform.set(transform);
        }
    }

    protected abstract void renderQuads(float coordArray[], byte colorArray[], int numVertices);

    /**
     *
     * This method will call releaseRenderTarget method to reset last
     * renderTarget and textures if g is null
     */
    public void setRenderTarget(BaseGraphics g) {
        if (g != null) {
            setRenderTarget(g.getRenderTarget(), g.getCameraNoClone(),
                    g.isDepthTest() && g.isDepthBuffer(), g.isState3D());
        } else {
            releaseRenderTarget();
        }
    }

    protected void releaseRenderTarget() {
        // Default implementation is a no-op. A pipeline may override if needed.
    }

    protected abstract void setRenderTarget(RenderTarget target, NGCamera camera,
                                            boolean depthTest, boolean state3D);

    public abstract void validateClearOp(BaseGraphics g);

    public abstract void validatePaintOp(BaseGraphics g, BaseTransform xform,
                                         Texture maskTex,
                                         float bx, float by, float bw, float bh);

    public abstract void validateTextureOp(BaseGraphics g, BaseTransform xform,
                                           Texture src, PixelFormat format);

    public void clearGlyphCaches() {
        clearCaches(greyGlyphCaches);
        clearCaches(lcdGlyphCaches);
    }

    private void clearCaches(Map<FontStrike, GlyphCache> glyphCaches) {
        for (Iterator<FontStrike> iter = glyphCaches.keySet().iterator(); iter.hasNext();) {
            iter.next().clearDesc();
        }

        for (GlyphCache cache : glyphCaches.values()) {
            if (cache != null) {
                cache.clear();
            }
        }
        glyphCaches.clear();
    }

    abstract public RTTexture getLCDBuffer();

    public GlyphCache getGlyphCache(FontStrike strike) {
        Map<FontStrike, GlyphCache> glyphCaches =
            (strike.getAAMode() ==FontResource.AA_LCD)
            ? lcdGlyphCaches : greyGlyphCaches;
        return getGlyphCache(strike, glyphCaches);
    }

    public boolean isSuperShaderEnabled() {
        return false;
    }

    private GlyphCache getGlyphCache(FontStrike strike,
                                     Map<FontStrike, GlyphCache> glyphCaches) {

        if (checkDisposed()) return null;

        GlyphCache glyphCache = glyphCaches.get(strike);
        if (glyphCache == null) {
            glyphCache = new GlyphCache(this, strike);
            glyphCaches.put(strike, glyphCache);
        }
        return glyphCache;
    }

    public Texture validateMaskTexture(MaskData maskData, boolean canScale) {
        if (checkDisposed()) return null;

        int pad = canScale ? 1 : 0;
        int needW = maskData.getWidth() + pad + pad;
        int needH = maskData.getHeight() + pad + pad;
        int texW = 0, texH = 0;

        if (maskTex != null) {
            maskTex.lock();
            if (maskTex.isSurfaceLost()) {
                maskTex = null;
            } else {
                texW = maskTex.getContentWidth();
                texH = maskTex.getContentHeight();
            }
        }

        if (maskTex == null || texW < needW || texH < needH) {
            if (maskTex != null) {
                flushVertexBuffer();
                maskTex.dispose();
                maskTex = null;
            }
            maskBuffer = null;

            // grow the mask texture so that the new one is always
            // at least as large as the previous one; this avoids
            // lots of creation/disposal when the shapes alternate
            // between narrow/tall and wide/short
            int newTexW = Math.max(MIN_MASK_DIM, Math.max(needW, texW));
            int newTexH = Math.max(MIN_MASK_DIM, Math.max(needH, texH));

            maskTex = getResourceFactory().
                createMaskTexture(newTexW, newTexH, WrapMode.CLAMP_NOT_NEEDED);
            maskBuffer = ByteBuffer.allocate(newTexW * newTexH);
            if (clearBuffer == null || clearBuffer.capacity() < newTexW) {
                clearBuffer = null;
                clearBuffer = ByteBuffer.allocate(newTexW);
            }
            curMaskRow = curMaskCol = nextMaskRow = highMaskCol = 0;
        }

        return maskTex;
    }

    public void updateMaskTexture(MaskData maskData, RectBounds maskBounds, boolean canScale) {
        if (checkDisposed()) return;

        // assert maskTex bound as texture 1...
        maskTex.assertLocked();
        int maskW = maskData.getWidth();
        int maskH = maskData.getHeight();
        int texW = maskTex.getContentWidth();
        int texH = maskTex.getContentHeight();
        int pad = canScale ? 1 : 0;
        int needW = maskW + pad + pad;
        int needH = maskH + pad + pad;
        if (curMaskCol + needW > texW) {
            curMaskCol = 0;
            curMaskRow = nextMaskRow;
        }
        if (curMaskRow + needH > texH) {
            flushVertexBuffer();
        }

        int offset = curMaskRow * texW + curMaskCol;
        ByteToBytePixelConverter b2bpc = ByteGray.ToByteGrayConverter();
        if (canScale) {
            // [UL => UR)
            int off = offset;
            b2bpc.convert(clearBuffer, 0, 0, maskBuffer, off, texW, maskW + 1, 1);
            // [UR => LR)
            off = offset + maskW + 1;
            b2bpc.convert(clearBuffer, 0, 0, maskBuffer, off, texW, 1, maskH + 1);
            // (UL => LL]
            off = offset + texW;  // UL corner + 1 row
            b2bpc.convert(clearBuffer, 0, 0, maskBuffer, off, texW, 1, maskH + 1);
            // (LL => LR]
            off = offset + (maskH + 1) * texW + 1; // LL corner + 1 col
            b2bpc.convert(clearBuffer, 0, 0, maskBuffer, off, texW, maskW + 1, 1);
            offset += texW + 1;
        }
        b2bpc.convert(maskData.getMaskBuffer(), 0, maskW,
                      maskBuffer, offset, texW,
                      maskW, maskH);

        float physW = maskTex.getPhysicalWidth();
        float physH = maskTex.getPhysicalHeight();
        maskBounds.setMinX((curMaskCol + pad        ) / physW);
        maskBounds.setMinY((curMaskRow + pad        ) / physH);
        maskBounds.setMaxX((curMaskCol + pad + maskW) / physW);
        maskBounds.setMaxY((curMaskRow + pad + maskH) / physH);

        curMaskCol = curMaskCol + needW;
        if (highMaskCol < curMaskCol) highMaskCol = curMaskCol;
        if (nextMaskRow < curMaskRow + needH) nextMaskRow = curMaskRow + needH;
    }

    public int getRectTextureMaxSize() {
        if (checkDisposed()) return 0;

        if (rectTex == null) {
            createRectTexture();
        }
        return rectTexMax;
    }

    public Texture getRectTexture() {
        if (checkDisposed()) return null;

        if (rectTex == null) {
            createRectTexture();
        }

        // rectTex is left permanent and locked so it never
        // goes away or needs to be checked for isSurfaceLost(), but we
        // add a lock here so that the caller can unlock without knowing
        // our inner implementation details
        rectTex.lock();
        return rectTex;
    }

    private void createRectTexture() {
        if (checkDisposed()) return;

        int texMax = PrismSettings.primTextureSize;
        if (texMax < 0) texMax = getResourceFactory().getMaximumTextureSize();
        int texDim = 3;
        int nextCellSize = 2;
        while (texDim + nextCellSize + 1 <= texMax) {
            rectTexMax = nextCellSize;
            texDim += ++nextCellSize;
        }
        byte mask[] = new byte[texDim * texDim];
        int cellY = 1;
        for (int cellH = 1; cellH <= rectTexMax; cellH++) {
            int cellX = 1;
            for (int cellW = 1; cellW <= rectTexMax; cellW++) {
                int index = cellY * texDim + cellX;
                for (int y = 0; y < cellH; y++) {
                    for (int x = 0; x < cellW; x++) {
                        mask[index + x] = (byte) 0xff;
                    }
                    index += texDim;
                }
                cellX += cellW + 1;
            }
            cellY += cellH + 1;
        }
        if (PrismSettings.verbose) {
            System.out.println("max rectangle texture cell size = "+rectTexMax);
        }
        Texture tex =
            getResourceFactory().createMaskTexture(texDim, texDim,
                                                   WrapMode.CLAMP_NOT_NEEDED);
        // rectTex remains permanently locked, useful, and permanent
        // an additional lock is added when a caller calls getWrapGreientTeture for
        // them to unlock
        tex.contentsUseful();
        tex.makePermanent();
        PixelFormat pf = tex.getPixelFormat();
        int scan = texDim * pf.getBytesPerPixelUnit();
        tex.update(ByteBuffer.wrap(mask), pf,
                   0, 0, 0, 0, texDim, texDim,
                   scan, false);
        rectTex = tex;
    }

    public Texture getWrapRectTexture() {
        if (checkDisposed()) return null;

        if (wrapRectTex == null) {
            Texture tex =
                getResourceFactory().createMaskTexture(2, 2, WrapMode.CLAMP_TO_EDGE);
            // wrapRectTex remains permanently locked, useful, and permanent
            // an additional lock is added when a caller calls getWrapGreientTeture for
            // them to unlock
            tex.contentsUseful();
            tex.makePermanent();
            int w = tex.getPhysicalWidth();
            int h = tex.getPhysicalHeight();
            if (PrismSettings.verbose) {
                System.out.println("wrap rectangle texture = "+w+" x "+h);
            }
            // assert w == 2 && h == 2?
            byte mask[] = new byte[w * h];
            int off = w;
            for (int y = 1; y < h; y++) {
                for (int x = 1; x < h; x++) {
                    mask[off + x] = (byte) 0xff;
                }
                off += w;
            }
            PixelFormat pf = tex.getPixelFormat();
            int scan = w * pf.getBytesPerPixelUnit();
            tex.update(ByteBuffer.wrap(mask), pf,
                       0, 0, 0, 0, w, h,
                       scan, false);
            wrapRectTex = tex;
        }

        // wrapRectTex is left permanent and locked so it never
        // goes away or needs to be checked for isSurfaceLost(), but we
        // add a lock here so that the caller can unlock without knowing
        // our inner implementation details
        wrapRectTex.lock();
        return wrapRectTex;
    }

    public Texture getOvalTexture() {
        if (checkDisposed()) return null;

        if (ovalTex == null) {
            int cellMax = getRectTextureMaxSize();
            int texDim = (cellMax * (cellMax + 1)) / 2;
            // size now points at the start of the max-sized cell
            texDim += cellMax + 1;
            // size now points just past the empty row on the far side of the
            // max-sized cell - which is the dimension we want the texture...
            byte mask[] = new byte[texDim * texDim];
            int cellY = 1;
            for (int cellH = 1; cellH <= cellMax; cellH++) {
                int cellX = 1;
                for (int cellW = 1; cellW <= cellMax; cellW++) {
                    int index = cellY * texDim + cellX;
//                    System.out.println("rasterizing "+cell_w+" x "+cell_h);
                    for (int y = 0; y < cellH; y++) {
                        if (y * 2 >= cellH) {
                            int reflecty = cellH - 1 - y;
                            // handle bottom half of ellipse via reflection
                            int rindex = index + (reflecty - y) * texDim;
                            for (int x = 0; x < cellW; x++) {
                                mask[index + x] = mask[rindex + x];
                            }
                        } else {
                            // Use 8 sub-row samples
                            float ovalY = y + 0.0625f;  // 1/16
                            for (int i = 0; i < 8; i++) {
                                float ovalX = (ovalY / cellH) - 0.5f;
                                ovalX = (float) Math.sqrt(0.25f - ovalX * ovalX);
                                int oxi = Math.round(cellW * 4.0f * (1.0f - ovalX * 2.0f));
                                int edgeX = oxi >> 3;
                                int subX = oxi & 0x7;
//                                System.out.println("y = "+oy+", mask["+rx+"] += "+(8-subx)+", mask["+(rx+1)+"] += "+subx);
                                mask[index + edgeX] += 8 - subX;
                                mask[index + edgeX + 1] += subX;
                                ovalY += 0.125f;  // 1/8
                            }
                            int accum = 0;
                            for (int x = 0; x < cellW; x++) {
                                if (x * 2 >= cellW) {
                                    // handle right half of ellipse via reflection
                                    mask[index + x] = mask[index + cellW - 1 - x];
                                } else {
                                    accum += mask[index + x];
//                                    System.out.println("accum["+rx+"] = "+accum);
                                    mask[index + x] = (byte) ((accum * 255 + 32) / 64);
                                }
                            }
                            // Sometimes for smaller ovals we leave some
                            // accumulation dirt just past the last cell
                            mask[index + cellW] = 0;
                        }
                        index += texDim;
                    }
                    cellX += cellW + 1;
                }
                cellY += cellH + 1;
            }
            if (false) {
                int index = 0;
                for (int y = 0; y < texDim; y++) {
                    for (int x = 0; x < texDim; x++) {
                        String s = Integer.toHexString((mask[index++] & 0xff) | 0x100);
                        System.out.print(s.substring(1)+" ");
                    }
                    System.out.println();
                }
            }
            Texture tex =
                getResourceFactory().createMaskTexture(texDim, texDim,
                                                       WrapMode.CLAMP_NOT_NEEDED);
            tex.contentsUseful();
            tex.makePermanent();
            PixelFormat pf = tex.getPixelFormat();
            int scan = texDim * pf.getBytesPerPixelUnit();
            tex.update(ByteBuffer.wrap(mask), pf,
                       0, 0, 0, 0, texDim, texDim,
                       scan, false);
            ovalTex = tex;
        }

        // ovalTex is left permanent and locked so it never
        // goes away or needs to be checked for isSurfaceLost(), but we
        // add a lock here so that the caller can unlock without knowing
        // our inner implementation details
        ovalTex.lock();
        return ovalTex;
    }

    public Texture getGradientTexture(Gradient grad, BaseTransform xform,
                                      int paintW, int paintH,
                                      MaskData maskData,
                                      float bx, float by, float bw, float bh)
    {
        if (checkDisposed()) return null;

        int sizeInPixels = paintW * paintH;
        int sizeInBytes = sizeInPixels * 4;
        if (paintBuffer == null || paintBuffer.capacity() < sizeInBytes) {
            paintPixels = new int[sizeInPixels];
            paintBuffer = ByteBuffer.wrap(new byte[sizeInBytes]);
        }

        if (paintTex != null) {
            paintTex.lock();
            if (paintTex.isSurfaceLost()) {
                paintTex = null;
            }
        }

        if (paintTex == null ||
            paintTex.getContentWidth()  < paintW ||
            paintTex.getContentHeight() < paintH)
        {
            int newTexW = paintW;
            int newTexH = paintH;
            if (paintTex != null) {
                // grow the paint texture so that the new one is always
                // at least as large as the previous one; this avoids
                // lots of creation/disposal when the shapes alternate
                // between narrow/tall and wide/short
                newTexW = Math.max(paintW, paintTex.getContentWidth());
                newTexH = Math.max(paintH, paintTex.getContentHeight());
                paintTex.dispose();
            }
            paintTex = getResourceFactory().
                createTexture(PixelFormat.BYTE_BGRA_PRE,
                              Texture.Usage.DEFAULT,
                              Texture.WrapMode.CLAMP_NOT_NEEDED,
                              newTexW, newTexH);
        }

        // note that the gradient will be tightly packed into paintImg
        // (i.e., no space at the end of each logical row) since there
        // is no way to control scanline stride for texture uploads in ES1
        PaintUtil.fillImageWithGradient(paintPixels, grad, xform,
                                        0, 0, paintW, paintH,
                                        bx, by, bw, bh);

        // RT-27421
        // TODO: could save some work here if we converted the *GradientContext
        // classes to produce ByteRgbaPre data instead of IntArgbPre data...
        byte[] bytePixels = paintBuffer.array();
        if (maskData != null) {
            // modulate with the mask pixels while we convert from
            // IntArgbPre to ByteRgbaPre
            byte[] maskPixels = maskData.getMaskBuffer().array();
            int j = 0;
            for (int i = 0; i < sizeInPixels; i++) {
                int pixel = paintPixels[i];
                int maskA = maskPixels[i] & 0xff;
                bytePixels[j++] = (byte)((((pixel       ) & 0xff) * maskA) / 255);
                bytePixels[j++] = (byte)((((pixel >>   8) & 0xff) * maskA) / 255);
                bytePixels[j++] = (byte)((((pixel >>  16) & 0xff) * maskA) / 255);
                bytePixels[j++] = (byte)((((pixel >>> 24)       ) * maskA) / 255);
            }
        } else {
            // just convert from IntArgbPre to ByteRgbaPre
            int j = 0;
            for (int i = 0; i < sizeInPixels; i++) {
                int pixel = paintPixels[i];
                bytePixels[j++] = (byte)((pixel       ) & 0xff);
                bytePixels[j++] = (byte)((pixel >>   8) & 0xff);
                bytePixels[j++] = (byte)((pixel >>  16) & 0xff);
                bytePixels[j++] = (byte)((pixel >>> 24)       );
            }
        }

        paintTex.update(paintBuffer, PixelFormat.BYTE_BGRA_PRE,
                        0, 0, 0, 0, paintW, paintH, paintW*4, false);

        return paintTex;
    }

    /**
     * Dispose of this context. Subclass implementations can override this
     * if needed. They must call super.dispose().
     */
    public void dispose() {
        clearGlyphCaches();
        GlyphCache.disposeForContext(this);

        if (maskTex != null) {
            maskTex.dispose();
            maskTex = null;
        }
        if (paintTex != null) {
            paintTex.dispose();
            paintTex = null;
        }
        if (rectTex != null) {
            rectTex.dispose();
            rectTex = null;
        }
        if (wrapRectTex != null) {
            wrapRectTex.dispose();
            wrapRectTex = null;
        }
        if (ovalTex != null) {
            ovalTex.dispose();
            ovalTex = null;
        }
        disposed = true;
    }

    /**
     * Returns a flag indicating whether this context has been disposed. A graphics
     * context is disposed by the associated ResourceFactory when it is disposed.
     * If a context has been disposed, it must be recreated, by a new ResourceFactory.
     * All draw calls will be ignored. An attempt to create a resource will be
     * ignored and will return null.
     *
     * @return true if this context has been disposed.
     */
    public final boolean isDisposed() {
        return disposed;
    }

    protected boolean checkDisposed() {
        if (PrismSettings.verbose && isDisposed()) {
            try {
                throw new IllegalStateException("attempt to use resource after context is disposed");
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }

        return isDisposed();
    }

}
