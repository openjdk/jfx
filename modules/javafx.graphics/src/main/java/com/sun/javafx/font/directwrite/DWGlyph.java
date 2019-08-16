/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;

public class DWGlyph implements Glyph {
    private DWFontStrike strike;
    private DWRITE_GLYPH_METRICS metrics;
    private DWRITE_GLYPH_RUN run;
    private float pixelXAdvance, pixelYAdvance;
    private RECT rect;
    private boolean drawShapes;
    private byte[][] pixelData;
    private RECT[] rects;

    private static final boolean CACHE_TARGET = true;
    private static IWICBitmap cachedBitmap;
    private static ID2D1RenderTarget cachedTarget;
    private static final int BITMAP_WIDTH = 256;
    private static final int BITMAP_HEIGHT = 256;
    private static final int BITMAP_PIXEL_FORMAT = OS.GUID_WICPixelFormat32bppPBGRA;
    private static D2D1_COLOR_F BLACK = new D2D1_COLOR_F(0f, 0f, 0f, 1f);
    private static D2D1_COLOR_F WHITE = new D2D1_COLOR_F(1f, 1f, 1f, 1f);
    private static D2D1_MATRIX_3X2_F D2D2_MATRIX_IDENTITY = new D2D1_MATRIX_3X2_F(1,0, 0,1, 0,0);

    public static final int SHORTMASK = 0x0000ffff;

    DWGlyph(DWFontStrike strike, int glyphCode, boolean drawShapes) {
        this.strike = strike;
        this.drawShapes = drawShapes;
        int size = DWFontStrike.SUBPIXEL_Y ? 9 : 3;
        this.pixelData = new byte[size][];
        this.rects = new RECT[size];

        IDWriteFontFace face = strike.getFontFace();
        run = new DWRITE_GLYPH_RUN();
        run.fontFace = face != null ? face.ptr : 0;
        run.fontEmSize = strike.getSize();
        run.glyphIndices = (short)glyphCode;
        run.glyphAdvances = 0;
        run.advanceOffset = 0;
        run.ascenderOffset = 0;
        run.bidiLevel = 0;
        run.isSideways = false;

        /* Note: glyphs can be created on the JFX thread to create shapes
         * for measuring. Therefore, avoid touching any native resource
         * (WICFactory or D2DFactory) here as they are not thread safe.
         */
    }

    void checkMetrics() {
        if (metrics != null) return;
        //TODO could the metrics cached in DWFontFile be used ?
        IDWriteFontFace face = strike.getFontFace();
        if (face == null) return;
        metrics = face.GetDesignGlyphMetrics(run.glyphIndices, false);
        if (metrics != null) {
            float upem = strike.getUpem();
            pixelXAdvance = metrics.advanceWidth * strike.getSize() / upem;
            pixelYAdvance = 0;
            if (strike.matrix != null) {
                Point2D pt = new Point2D(pixelXAdvance, pixelYAdvance);
                strike.getTransform().transform(pt, pt);
                pixelXAdvance = pt.x;
                pixelYAdvance = pt.y;
            }
        }
    }

    void checkBounds() {
        if (rect != null) return;
        /* Note that when generating the glyph image this bounds will be
         * recomputed (respecting the correct subpixel alignment).
         */
        int textureType = OS.DWRITE_TEXTURE_CLEARTYPE_3x1;
        IDWriteGlyphRunAnalysis runAnalysis = createAnalysis(0, 0);
        if (runAnalysis != null) {
            rect = runAnalysis.GetAlphaTextureBounds(textureType);
            if (rect == null || rect.right - rect.left == 0 || rect.bottom - rect.top == 0) {
                /* Check for both texture types due to some limitations with
                 * IDWriteGlyphRunAnalysis. See RT-31587.
                 */
                rect = runAnalysis.GetAlphaTextureBounds(OS.DWRITE_TEXTURE_ALIASED_1x1);
            }
            runAnalysis.Release();
        }
        if (rect == null) {
            rect = new RECT();
        } else {
            /* Increase the RECT */
            rect.left--;
            rect.top--;
            rect.right++;
            rect.bottom++;
        }
    }

    byte[] getLCDMask(float subPixelX, float subPixelY) {
        IDWriteGlyphRunAnalysis runAnalysis = createAnalysis(subPixelX, subPixelY);
        byte[] buffer = null;
        if (runAnalysis != null) {
            int textureType = OS.DWRITE_TEXTURE_CLEARTYPE_3x1;
            rect = runAnalysis.GetAlphaTextureBounds(textureType);
            if (rect != null && rect.right - rect.left != 0 && rect.bottom - rect.top != 0) {
                buffer = runAnalysis.CreateAlphaTexture(textureType, rect);
            } else {
                /* In some cases IDWriteGlyphRunAnalysis is unable to produce
                 * LCD masks. But as long as the size can determined D2D can be
                 * used to do the rendering. */
                rect = runAnalysis.GetAlphaTextureBounds(OS.DWRITE_TEXTURE_ALIASED_1x1);
                if (rect != null && rect.right - rect.left != 0 && rect.bottom - rect.top != 0) {
                    buffer = getD2DMask(subPixelX, subPixelY, true);
                }
            }
            runAnalysis.Release();
        }
        if (buffer == null) {
            buffer = new byte[0];
            rect = new RECT();
        }
        return buffer;
    }

    byte[] getD2DMask(float subPixelX, float subPixelY, boolean lcd) {
        checkBounds();
        if (getWidth() == 0 || getHeight() == 0 || run.fontFace == 0) {
            return new byte[0];
        }

        float glyphX = rect.left;
        float glyphY = rect.top;
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        boolean cache = CACHE_TARGET && BITMAP_WIDTH >= w && BITMAP_HEIGHT >= h;
        IWICBitmap bitmap;
        ID2D1RenderTarget target;
        if (cache) {
            bitmap = getCachedBitmap();
            target = getCachedRenderingTarget();
        } else {
            bitmap = createBitmap(w, h);
            target = createRenderingTarget(bitmap);
        }
        if (bitmap == null || target == null) {
            return new byte[0];
        }

        DWRITE_MATRIX matrix = strike.matrix;
        D2D1_MATRIX_3X2_F transform;
        if (matrix != null) {
            transform = new D2D1_MATRIX_3X2_F(matrix.m11, matrix.m12,
                                              matrix.m21, matrix.m22,
                                              -glyphX + subPixelX, -glyphY + subPixelY);
            glyphX = glyphY = 0;
        } else {
            transform = D2D2_MATRIX_IDENTITY;
            glyphX -= subPixelX;
            glyphY -= subPixelY;
        }

        target.BeginDraw();
        target.SetTransform(transform);
        target.Clear(WHITE);
        D2D1_POINT_2F pt = new D2D1_POINT_2F(-glyphX, -glyphY);
        ID2D1Brush brush = target.CreateSolidColorBrush(BLACK);
        if (!lcd) {
            target.SetTextAntialiasMode(OS.D2D1_TEXT_ANTIALIAS_MODE_GRAYSCALE);
        }
        target.DrawGlyphRun(pt, run, brush, OS.DWRITE_MEASURING_MODE_NATURAL);
        int hr = target.EndDraw();
        brush.Release();

        if (hr != OS.S_OK) {
            /* handling errors such as D2DERR_RECREATE_TARGET */
            bitmap.Release();
            cachedBitmap = null;
            target.Release();
            cachedTarget = null;
            if (PrismFontFactory.debugFonts) {
                System.err.println("Rendering failed=" + hr);
            }
            rect.left = rect.top = rect.right = rect.bottom = 0;
            return null;
        }

        byte[] result = null;
        IWICBitmapLock lock = bitmap.Lock(0, 0, w, h, OS.WICBitmapLockRead);
        if (lock != null) {
            byte[] buffer = lock.GetDataPointer();
            // TODO instead of copying the entire buffer to java it would
            // be faster to blit in native code.
            if (buffer != null) {
                int stride = lock.GetStride();
                int i = 0, j = 0;
                byte one = (byte)0xFF;
                if (lcd) {
                    result = new byte[w*h*3];
                    for (int y = 0; y < h; y++) {
                        int row = j;
                        for (int x = 0; x < w; x++) {
                            result[i++] = (byte)(one - buffer[row++]);
                            result[i++] = (byte)(one - buffer[row++]);
                            result[i++] = (byte)(one - buffer[row++]);
                            row++;
                        }
                        j += stride;
                    }
                } else {
                    result = new byte[w*h];
                    for (int y = 0; y < h; y++) {
                        int row = j;
                        for (int x = 0; x < w; x++) {
                            result[i++] = (byte)(one - buffer[row]);
                            row += 4;
                        }
                        j += stride;
                    }
                }
            }
            lock.Release();
        }

        if (!cache) {
            bitmap.Release();
            target.Release();
        }
        return result;
    }

    IDWriteGlyphRunAnalysis createAnalysis(float x, float y) {
        if (run.fontFace == 0) return null;
        IDWriteFactory factory = DWFactory.getDWriteFactory();
        int renderingMode = DWFontStrike.SUBPIXEL_Y ?
                            OS.DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC :
                            OS.DWRITE_RENDERING_MODE_NATURAL;
        int measuringMode = OS.DWRITE_MEASURING_MODE_NATURAL;
        DWRITE_MATRIX matrix = strike.matrix; /* can be null */
        float dpi = 1;  /* Assumes WICBitmap has 96 dpi */
        return factory.CreateGlyphRunAnalysis(run, dpi, matrix, renderingMode, measuringMode, x, y);
    }

    IWICBitmap getCachedBitmap() {
        if (cachedBitmap == null) {
            cachedBitmap = createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT);
        }
        return cachedBitmap;
    }

    ID2D1RenderTarget getCachedRenderingTarget() {
        if (cachedTarget == null) {
            cachedTarget = createRenderingTarget(getCachedBitmap());
        }
        return cachedTarget;
    }

    IWICBitmap createBitmap(int width, int height) {
        IWICImagingFactory factory = DWFactory.getWICFactory();
        return  factory.CreateBitmap(width, height, BITMAP_PIXEL_FORMAT, OS.WICBitmapCacheOnDemand);
    }

    ID2D1RenderTarget createRenderingTarget(IWICBitmap bitmap) {
        D2D1_RENDER_TARGET_PROPERTIES prop = new D2D1_RENDER_TARGET_PROPERTIES();
        /* All values set to defaults */
        prop.type = OS.D2D1_RENDER_TARGET_TYPE_DEFAULT;
        prop.pixelFormat.format = OS.DXGI_FORMAT_UNKNOWN;
        prop.pixelFormat.alphaMode = OS.D2D1_ALPHA_MODE_UNKNOWN;
        prop.dpiX = 0;
        prop.dpiY = 0;
        prop.usage = OS.D2D1_RENDER_TARGET_USAGE_NONE;
        prop.minLevel = OS.D2D1_FEATURE_LEVEL_DEFAULT;
        ID2D1Factory factory = DWFactory.getD2DFactory();
        return factory.CreateWicBitmapRenderTarget(bitmap, prop);
    }

    @Override
    public int getGlyphCode() {
        return (run.glyphIndices & SHORTMASK);
    }

    @Override
    public RectBounds getBBox() {
        return strike.getBBox(run.glyphIndices & SHORTMASK);
    }

    @Override
    public float getAdvance() {
        checkMetrics();
        if (metrics == null) return 0;
        float upem = strike.getUpem();
        return metrics.advanceWidth * strike.getSize() / upem;
    }

    @Override
    public Shape getShape() {
        return strike.createGlyphOutline(run.glyphIndices & SHORTMASK);
    }

    @Override
    public byte[] getPixelData() {
        return getPixelData(0);
    }

    @Override
    public byte[] getPixelData(int subPixel) {
        byte[] data = pixelData[subPixel];
        /* Caching all possible masks has an important performance impact on the
         * software pipeline (as it doesn't have a glyph cache).
         * Note: The same cache is not implemented on CTGlyph.
         */
        if (data == null) {
            float x = 0, y = 0;
            int index = subPixel;
            if (index >= 6) {
                index -= 6;
                y = 0.66f;
            } else if (index >= 3) {
                index -= 3;
                y = 0.33f;
            }
            if (index == 1) x = 0.33f;
            if (index == 2) x = 0.66f;
            pixelData[subPixel] = data = isLCDGlyph() ? getLCDMask(x, y) :
                                                        getD2DMask(x, y, false);
            rects[subPixel] = rect;
        } else {
            rect = rects[subPixel];
        }
        return data;
    }

    @Override
    public float getPixelXAdvance() {
        checkMetrics();
        return pixelXAdvance;
    }

    @Override
    public float getPixelYAdvance() {
        checkMetrics();
        return pixelYAdvance;
    }

    @Override
    public int getWidth() {
        checkBounds();
        return (rect.right - rect.left) * (isLCDGlyph() ? 3 : 1);
    }

    @Override
    public int getHeight() {
        checkBounds();
        return rect.bottom - rect.top;
    }

    @Override
    public int getOriginX() {
        checkBounds();
        return rect.left;
    }

    @Override
    public int getOriginY() {
        checkBounds();
        return rect.top;
    }

    @Override
    public boolean isLCDGlyph() {
        return strike.getAAMode() == FontResource.AA_LCD;
    }

}
