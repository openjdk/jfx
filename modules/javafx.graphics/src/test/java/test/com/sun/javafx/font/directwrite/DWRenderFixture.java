/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.font.directwrite;

import com.sun.javafx.font.directwrite.DWNativeShim;
import com.sun.javafx.font.directwrite.DWNativeShim.Api;
import com.sun.javafx.font.directwrite.DWNativeShim.GlyphRun;
import com.sun.javafx.font.directwrite.DWNativeShim.RenderTargetProperties;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link DWRenderTest} needs: the COM apartment, the WIC and D2D
 * factories, and a faithful re-enactment of {@code DWGlyph.getD2DMask} over one {@link Api}.
 * <p>
 * The re-enactment matters more than usual here. {@code directwrite-metrics-golden.txt} pins the
 * {@code IDWriteGlyphRunAnalysis} LCD path and <b>deliberately does not pin the D2D/WIC greyscale
 * path</b>, which is the default one - {@code PrismFontFile.getDefaultAAMode()} is
 * {@code AA_GREYSCALE} and {@code DWGlyph.getPixelData} routes every non-LCD strike through
 * {@code getD2DMask}. So there is no golden to compare against, and the only available oracle is the
 * JNI implementation, which still exists. {@link #render} therefore performs exactly the call
 * sequence {@code DWGlyph.getD2DMask} performs - the same 256 x 256 cached-shape bitmap, the same
 * transform arithmetic, the same {@code Clear(WHITE)} then {@code CreateSolidColorBrush(BLACK)} then
 * optional {@code SetTextAntialiasMode} then {@code DrawGlyphRun}, the same lock rectangle and the
 * same stride-walking loops - so that a difference between the two paths is a difference in the
 * pixels JavaFX would ship.
 * <p>
 * Not named {@code *Test}, so surefire does not try to run it.
 */
final class DWRenderFixture {

    /** {@code DWGlyph.BITMAP_WIDTH} and {@code BITMAP_HEIGHT}: the cached target is always 256 x 256. */
    static final int BITMAP_WIDTH = 256;
    static final int BITMAP_HEIGHT = 256;

    /** {@code DWGlyph.BLACK} and {@code WHITE}. */
    static final float[] BLACK = { 0f, 0f, 0f, 1f };
    static final float[] WHITE = { 1f, 1f, 1f, 1f };

    /** {@code DWGlyph.D2D2_MATRIX_IDENTITY}. */
    static final float[] IDENTITY = { 1f, 0f, 0f, 1f, 0f, 0f };

    /** Passed to {@link #render} to mean "do not call SetTextAntialiasMode at all", as the LCD path. */
    static final int ANTIALIAS_MODE_UNSET = -1;

    /** The code points every render test draws, chosen so that all of them have ink in all four fonts. */
    static final int[] CODE_POINTS = { 0x0048, 0x0069, 0x0067, 0x0057, 0x0030, 0x0038, 0x0040, 0x004D };

    private DWRenderFixture() {
    }

    /* ---------------------------------------------------------------------------------------------
     * The COM apartment and the three factories
     * ------------------------------------------------------------------------------------------- */

    /**
     * The factories one {@link Api} needs to draw. The D2D factory is
     * {@code D2D1_FACTORY_TYPE_SINGLE_THREADED} exactly as {@code DWFactory.getD2DFactory} creates it,
     * so everything built from it must stay on the thread that created it - which is also what
     * {@code DWFactory.checkThread()} enforces in production.
     */
    record Ctx(Api api, long dwriteFactory, long wicFactory, long d2dFactory) {
    }

    /**
     * What {@code DWFactory.getWICFactory} does before it can create anything:
     * {@code CoInitializeEx(COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE)}. Balanced by
     * {@link #comUninitialize}, on this thread.
     */
    static void comInitialize(Api api) {
        int flags = DWNativeShim.osConstant("COINIT_APARTMENTTHREADED")
                | DWNativeShim.osConstant("COINIT_DISABLE_OLE1DDE");
        assertTrue(api.coInitializeEx(flags),
                () -> api.name() + ": CoInitializeEx must not report RPC_E_CHANGED_MODE here");
    }

    static void comUninitialize(Api api) {
        api.coUninitialize();
    }

    /** The three factories, on the calling thread, with COM already initialized on it. */
    static Ctx open(Api api) {
        long dwrite = api.createFactory(DWNativeShim.factoryTypeShared());
        assertNotEquals(0L, dwrite, () -> api.name() + ": DWriteCreateFactory");
        long wic = api.wicCreateImagingFactory();
        assertNotEquals(0L, wic, () -> api.name() + ": CoCreateInstance(CLSID_WICImagingFactory)");
        long d2d = api.d2d1CreateFactory(DWNativeShim.osConstant("D2D1_FACTORY_TYPE_SINGLE_THREADED"));
        assertNotEquals(0L, d2d, () -> api.name() + ": D2D1CreateFactory");
        return new Ctx(api, dwrite, wic, d2d);
    }

    static void close(Ctx ctx) {
        if (ctx == null) {
            return;
        }
        if (ctx.d2dFactory() != 0) {
            ctx.api().release(ctx.d2dFactory());
        }
        if (ctx.wicFactory() != 0) {
            ctx.api().release(ctx.wicFactory());
        }
        if (ctx.dwriteFactory() != 0) {
            ctx.api().release(ctx.dwriteFactory());
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * One glyph, drawn the way DWGlyph draws it
     * ------------------------------------------------------------------------------------------- */

    /**
     * Everything one call to {@code DWGlyph.getD2DMask} produces or depends on, so that a parity test
     * can compare the intermediate values and not only the final bytes.
     *
     * @param rect   the inflated {@code RECT} of {@code DWGlyph.checkBounds}, as left, top, right, bottom
     * @param stride {@code IWICBitmapLock::GetStride}
     * @param locked the whole buffer {@code IWICBitmapLock::GetDataPointer} handed back
     * @param mask   the greyscale or LCD mask {@code getD2DMask} would have returned
     * @param hr     what {@code EndDraw} answered
     */
    record Mask(int[] rect, int width, int height, int stride, byte[] locked, byte[] mask, int hr) {
    }

    /**
     * {@code DWGlyph.checkBounds} (:100-125): the CLEARTYPE_3x1 bounds of the glyph run, falling back
     * to ALIASED_1x1 when they are empty (JDK-8096940), inflated by one pixel on every side. Each
     * implementation computes its own, through its own {@code CreateGlyphRunAnalysis} and
     * {@code GetAlphaTextureBounds}, so a parity test compares the whole chain.
     */
    static int[] inflatedBounds(Ctx ctx, long fontFace, float emSize, short glyph, float[] matrix) {
        Api api = ctx.api();
        long analysis = api.createGlyphRunAnalysis(ctx.dwriteFactory(),
                GlyphRun.of(fontFace, emSize, glyph), 1f, matrix,
                DWNativeShim.osConstant("DWRITE_RENDERING_MODE_NATURAL"),
                DWNativeShim.osConstant("DWRITE_MEASURING_MODE_NATURAL"), 0f, 0f);
        int[] rect = null;
        if (analysis != 0) {
            rect = api.alphaTextureBounds(analysis, DWNativeShim.osConstant("DWRITE_TEXTURE_CLEARTYPE_3x1"));
            if (rect == null || rect[2] - rect[0] == 0 || rect[3] - rect[1] == 0) {
                rect = api.alphaTextureBounds(analysis, DWNativeShim.osConstant("DWRITE_TEXTURE_ALIASED_1x1"));
            }
            api.release(analysis);
        }
        if (rect == null) {
            return new int[4];
        }
        return new int[] { rect[0] - 1, rect[1] - 1, rect[2] + 1, rect[3] + 1 };
    }

    /**
     * {@code DWGlyph.getD2DMask} (:154-258), call for call. {@code matrix} is the strike transform -
     * {@code null} for the identity case, six floats otherwise - and {@code antialiasMode} is
     * {@link #ANTIALIAS_MODE_UNSET} to reproduce the LCD path, which never calls
     * {@code SetTextAntialiasMode} at all.
     */
    static Mask render(Ctx ctx, long fontFace, float emSize, short glyph, float subPixelX,
                       float subPixelY, boolean lcd, float[] matrix, int antialiasMode) {
        Api api = ctx.api();
        int[] rect = inflatedBounds(ctx, fontFace, emSize, glyph, matrix);
        int w = rect[2] - rect[0];
        int h = rect[3] - rect[1];
        if (w <= 0 || h <= 0) {
            return new Mask(rect, w, h, 0, new byte[0], new byte[0], 0);
        }
        assertTrue(w <= BITMAP_WIDTH && h <= BITMAP_HEIGHT,
                () -> "glyph " + glyph + " at " + emSize + " does not fit the cached bitmap");

        float glyphX = rect[0];
        float glyphY = rect[1];
        long bitmap = createBitmap(api, ctx.wicFactory(), BITMAP_WIDTH, BITMAP_HEIGHT);
        long target = createRenderTarget(api, ctx.d2dFactory(), bitmap);

        float[] transform;
        if (matrix != null) {
            transform = new float[] { matrix[0], matrix[1], matrix[2], matrix[3],
                    -glyphX + subPixelX, -glyphY + subPixelY };
            glyphX = 0;
            glyphY = 0;
        } else {
            transform = IDENTITY;
            glyphX -= subPixelX;
            glyphY -= subPixelY;
        }

        api.beginDraw(target);
        api.setTransform(target, transform);
        api.clear(target, WHITE);
        long brush = api.createSolidColorBrush(target, BLACK);
        assertNotEquals(0L, brush, () -> api.name() + ": CreateSolidColorBrush");
        if (antialiasMode != ANTIALIAS_MODE_UNSET) {
            api.setTextAntialiasMode(target, antialiasMode);
        }
        api.drawGlyphRun(target, new float[] { -glyphX, -glyphY },
                GlyphRun.of(fontFace, emSize, glyph), brush,
                DWNativeShim.osConstant("DWRITE_MEASURING_MODE_NATURAL"));
        int hr = api.endDraw(target);
        api.release(brush);

        int stride = 0;
        byte[] locked = null;
        byte[] mask = null;
        if (hr == DWNativeShim.osConstant("S_OK")) {
            long bitmapLock = api.lock(bitmap, 0, 0, w, h, DWNativeShim.osConstant("WICBitmapLockRead"));
            assertNotEquals(0L, bitmapLock, () -> api.name() + ": IWICBitmap::Lock");
            locked = api.getDataPointer(bitmapLock);
            stride = api.getStride(bitmapLock);
            if (locked != null) {
                mask = toMask(locked, stride, w, h, lcd);
            }
            api.release(bitmapLock);
        }
        api.release(bitmap);
        api.release(target);
        return new Mask(rect, w, h, stride, locked, mask, hr);
    }

    /**
     * The two stride-walking loops of {@code DWGlyph.getD2DMask} (:222-249), byte for byte: the bitmap
     * is 32bppPBGRA and black-on-white, so each output byte is {@code 0xFF - blue} and the LCD form
     * takes three consecutive channels per pixel while the greyscale form takes one and steps four.
     */
    static byte[] toMask(byte[] buffer, int stride, int w, int h, boolean lcd) {
        byte one = (byte) 0xFF;
        int i = 0;
        int j = 0;
        byte[] result = new byte[lcd ? w * h * 3 : w * h];
        for (int y = 0; y < h; y++) {
            int row = j;
            for (int x = 0; x < w; x++) {
                if (lcd) {
                    result[i++] = (byte) (one - buffer[row++]);
                    result[i++] = (byte) (one - buffer[row++]);
                    result[i++] = (byte) (one - buffer[row++]);
                    row++;
                } else {
                    result[i++] = (byte) (one - buffer[row]);
                    row += 4;
                }
            }
            j += stride;
        }
        return result;
    }

    /** {@code DWGlyph.createBitmap}: 32bppPBGRA, cache on demand. */
    static long createBitmap(Api api, long wicFactory, int width, int height) {
        long bitmap = api.createBitmap(wicFactory, width, height,
                DWNativeShim.osConstant("GUID_WICPixelFormat32bppPBGRA"),
                DWNativeShim.osConstant("WICBitmapCacheOnDemand"));
        assertNotEquals(0L, bitmap, () -> api.name() + ": IWICImagingFactory::CreateBitmap");
        return bitmap;
    }

    /** {@code DWGlyph.createRenderingTarget}: every property at its default. */
    static long createRenderTarget(Api api, long d2dFactory, long bitmap) {
        long target = api.createWicBitmapRenderTarget(d2dFactory, bitmap,
                RenderTargetProperties.defaults());
        assertNotEquals(0L, target, () -> api.name() + ": ID2D1Factory::CreateWicBitmapRenderTarget");
        return target;
    }

    /* ---------------------------------------------------------------------------------------------
     * Reading the ink back
     * ------------------------------------------------------------------------------------------- */

    /**
     * The bounding box of every pixel that is not pure white in a 32bppPBGRA buffer, as
     * {left, top, right, bottom} with the right and bottom exclusive, or {@code null} when the buffer
     * is blank. This is how the by-value baseline origin is measured: a glyph drawn at two origins
     * must produce two boxes that differ by exactly the difference of the origins.
     */
    static int[] inkBounds(byte[] buffer, int stride, int width, int height) {
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int y = 0; y < height; y++) {
            int row = y * stride;
            for (int x = 0; x < width; x++) {
                int blue = buffer[row + x * 4] & 0xFF;
                int green = buffer[row + x * 4 + 1] & 0xFF;
                int red = buffer[row + x * 4 + 2] & 0xFF;
                if (blue != 0xFF || green != 0xFF || red != 0xFF) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x + 1);
                    bottom = Math.max(bottom, y + 1);
                }
            }
        }
        return left == Integer.MAX_VALUE ? null : new int[] { left, top, right, bottom };
    }

    /** How many bytes of a mask are non-zero, i.e. how much ink it carries. */
    static int inkedBytes(byte[] mask) {
        int inked = 0;
        for (byte b : mask) {
            if (b != 0) {
                inked++;
            }
        }
        return inked;
    }
}
