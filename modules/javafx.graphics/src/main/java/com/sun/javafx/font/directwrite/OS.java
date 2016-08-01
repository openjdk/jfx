/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.geom.Path2D;

class OS {
    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            NativeLibLoader.loadLibrary("javafx_font");
            return null;
        });
    }

    static final int S_OK = 0x0;
    static final int E_NOT_SUFFICIENT_BUFFER = 0x8007007A;

    /* Direct2D constants */
    static final int D2D1_FACTORY_TYPE_SINGLE_THREADED = 0;
    static final int D2D1_RENDER_TARGET_TYPE_DEFAULT    = 0;
    static final int D2D1_RENDER_TARGET_TYPE_SOFTWARE   = 1;
    static final int D2D1_RENDER_TARGET_TYPE_HARDWARE   = 2;
    static final int D2D1_RENDER_TARGET_USAGE_NONE                   = 0x00000000;
    static final int D2D1_RENDER_TARGET_USAGE_FORCE_BITMAP_REMOTING  = 0x00000001;
    static final int D2D1_RENDER_TARGET_USAGE_GDI_COMPATIBLE         = 0x00000002;
    static final int D2D1_FEATURE_LEVEL_DEFAULT  = 0;
    static final int D2D1_ALPHA_MODE_UNKNOWN        = 0;
    static final int D2D1_ALPHA_MODE_PREMULTIPLIED  = 1;
    static final int D2D1_ALPHA_MODE_STRAIGHT       = 2;
    static final int D2D1_ALPHA_MODE_IGNORE         = 3;
    static final int DXGI_FORMAT_UNKNOWN = 0;
    static final int DXGI_FORMAT_A8_UNORM  = 65;
    static final int DXGI_FORMAT_B8G8R8A8_UNORM  = 87;
    static final int D2D1_TEXT_ANTIALIAS_MODE_DEFAULT    = 0;
    static final int D2D1_TEXT_ANTIALIAS_MODE_CLEARTYPE  = 1;
    static final int D2D1_TEXT_ANTIALIAS_MODE_GRAYSCALE  = 2;
    static final int D2D1_TEXT_ANTIALIAS_MODE_ALIASED    = 3;

    /* WICImagining constants */
    static final int GUID_WICPixelFormat8bppGray = 1;
    static final int GUID_WICPixelFormat8bppAlpha = 2;
    static final int GUID_WICPixelFormat16bppGray = 3;
    static final int GUID_WICPixelFormat24bppRGB = 4;
    static final int GUID_WICPixelFormat24bppBGR = 5;
    static final int GUID_WICPixelFormat32bppBGR = 6;
    static final int GUID_WICPixelFormat32bppBGRA = 7;
    static final int GUID_WICPixelFormat32bppPBGRA = 8;
    static final int GUID_WICPixelFormat32bppGrayFloat = 9;
    static final int GUID_WICPixelFormat32bppRGBA = 10;
    static final int GUID_WICPixelFormat32bppPRGBA = 11;
    static final int WICBitmapNoCache       = 0;
    static final int WICBitmapCacheOnDemand = 0x1;
    static final int WICBitmapCacheOnLoad   = 0x2;
    static final int WICBitmapLockRead   = 0x00000001;
    static final int WICBitmapLockWrite  = 0x00000002;

    /* DirectWrite constants */
    static final int DWRITE_FONT_WEIGHT_THIN         = 100;
    static final int DWRITE_FONT_WEIGHT_EXTRA_LIGHT  = 200;
    static final int DWRITE_FONT_WEIGHT_ULTRA_LIGHT  = 200;
    static final int DWRITE_FONT_WEIGHT_LIGHT        = 300;
    static final int DWRITE_FONT_WEIGHT_SEMI_LIGHT   = 350;
    static final int DWRITE_FONT_WEIGHT_NORMAL       = 400;
    static final int DWRITE_FONT_WEIGHT_REGULAR      = 400;
    static final int DWRITE_FONT_WEIGHT_MEDIUM       = 500;
    static final int DWRITE_FONT_WEIGHT_DEMI_BOLD    = 600;
    static final int DWRITE_FONT_WEIGHT_SEMI_BOLD    = 600;
    static final int DWRITE_FONT_WEIGHT_BOLD         = 700;
    static final int DWRITE_FONT_WEIGHT_EXTRA_BOLD   = 800;
    static final int DWRITE_FONT_WEIGHT_ULTRA_BOLD   = 800;
    static final int DWRITE_FONT_WEIGHT_BLACK        = 900;
    static final int DWRITE_FONT_WEIGHT_HEAVY        = 900;
    static final int DWRITE_FONT_WEIGHT_EXTRA_BLACK  = 950;
    static final int DWRITE_FONT_WEIGHT_ULTRA_BLACK  = 950;
    static final int DWRITE_FONT_STRETCH_UNDEFINED        = 0;
    static final int DWRITE_FONT_STRETCH_ULTRA_CONDENSED  = 1;
    static final int DWRITE_FONT_STRETCH_EXTRA_CONDENSED  = 2;
    static final int DWRITE_FONT_STRETCH_CONDENSED        = 3;
    static final int DWRITE_FONT_STRETCH_SEMI_CONDENSED   = 4;
    static final int DWRITE_FONT_STRETCH_NORMAL           = 5;
    static final int DWRITE_FONT_STRETCH_MEDIUM           = 5;
    static final int DWRITE_FONT_STRETCH_SEMI_EXPANDED    = 6;
    static final int DWRITE_FONT_STRETCH_EXPANDED         = 7;
    static final int DWRITE_FONT_STRETCH_EXTRA_EXPANDED   = 8;
    static final int DWRITE_FONT_STRETCH_ULTRA_EXPANDED   = 9;
    static final int DWRITE_FONT_STYLE_NORMAL       = 0;
    static final int DWRITE_FONT_STYLE_OBLIQUE      = 1;
    static final int DWRITE_FONT_STYLE_ITALIC       = 2;
    static final int DWRITE_TEXTURE_ALIASED_1x1 = 0;
    static final int DWRITE_TEXTURE_CLEARTYPE_3x1 = 1;
    static final int DWRITE_RENDERING_MODE_DEFAULT                      = 0;
    static final int DWRITE_RENDERING_MODE_ALIASED                      = 1;
    static final int DWRITE_RENDERING_MODE_GDI_CLASSIC                  = 2;
    static final int DWRITE_RENDERING_MODE_GDI_NATURAL                  = 3;
    static final int DWRITE_RENDERING_MODE_NATURAL                      = 4;
    static final int DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC            = 5;
    static final int DWRITE_RENDERING_MODE_OUTLINE                      = 6;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_GDI_CLASSIC        = DWRITE_RENDERING_MODE_GDI_CLASSIC;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_GDI_NATURAL        = DWRITE_RENDERING_MODE_GDI_NATURAL;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_NATURAL            = DWRITE_RENDERING_MODE_NATURAL;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_NATURAL_SYMMETRIC  = DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC;
    static final int DWRITE_MEASURING_MODE_NATURAL = 0;
    static final int DWRITE_MEASURING_MODE_GDI_CLASSIC = 1;
    static final int DWRITE_MEASURING_MODE_GDI_NATURAL = 2;
    static final int DWRITE_FACTORY_TYPE_SHARED = 0;
    static final int DWRITE_READING_DIRECTION_LEFT_TO_RIGHT = 0;
    static final int DWRITE_READING_DIRECTION_RIGHT_TO_LEFT = 1;
    static final int DWRITE_FONT_SIMULATIONS_NONE     = 0x0000;
    static final int DWRITE_FONT_SIMULATIONS_BOLD     = 0x0001;
    static final int DWRITE_FONT_SIMULATIONS_OBLIQUE  = 0x0002;
    static final int DWRITE_INFORMATIONAL_STRING_NONE = 0;
    static final int DWRITE_INFORMATIONAL_STRING_COPYRIGHT_NOTICE = 1;
    static final int DWRITE_INFORMATIONAL_STRING_VERSION_STRINGS = 2;
    static final int DWRITE_INFORMATIONAL_STRING_TRADEMARK = 3;
    static final int DWRITE_INFORMATIONAL_STRING_MANUFACTURER = 4;
    static final int DWRITE_INFORMATIONAL_STRING_DESIGNER = 5;
    static final int DWRITE_INFORMATIONAL_STRING_DESIGNER_URL = 6;
    static final int DWRITE_INFORMATIONAL_STRING_DESCRIPTION = 7;
    static final int DWRITE_INFORMATIONAL_STRING_FONT_VENDOR_URL = 8;
    static final int DWRITE_INFORMATIONAL_STRING_LICENSE_DESCRIPTION = 9;
    static final int DWRITE_INFORMATIONAL_STRING_LICENSE_INFO_URL = 10;
    static final int DWRITE_INFORMATIONAL_STRING_WIN32_FAMILY_NAMES = 11;
    static final int DWRITE_INFORMATIONAL_STRING_WIN32_SUBFAMILY_NAMES = 12;
    static final int DWRITE_INFORMATIONAL_STRING_PREFERRED_FAMILY_NAMES = 13;
    static final int DWRITE_INFORMATIONAL_STRING_PREFERRED_SUBFAMILY_NAMES = 14;
    static final int DWRITE_INFORMATIONAL_STRING_SAMPLE_TEXT = 15;
    /* Only on newer versions of Dwrite */
    static final int DWRITE_INFORMATIONAL_STRING_FULL_NAME = 16;
    static final int DWRITE_INFORMATIONAL_STRING_POSTSCRIPT_NAME = 17;
    static final int DWRITE_INFORMATIONAL_STRING_POSTSCRIPT_CID_NAME = 18;

    /* Constructors */
    private static final native long _DWriteCreateFactory(int factoryType);
    static final IDWriteFactory DWriteCreateFactory(int factoryType) {
        long ptr = _DWriteCreateFactory(factoryType);
        return ptr != 0 ? new IDWriteFactory(ptr) : null;
    }

    private static final native long _D2D1CreateFactory(int factoryType);
    static final ID2D1Factory D2D1CreateFactory(int factoryType) {
        long ptr = _D2D1CreateFactory(factoryType);
        return ptr != 0 ? new ID2D1Factory(ptr) : null;
    }

    private static final native long _WICCreateImagingFactory();
    static final IWICImagingFactory WICCreateImagingFactory() {
        long ptr = _WICCreateImagingFactory();
        return ptr != 0 ? new IWICImagingFactory(ptr) : null;
    }

    private static final native long _NewJFXTextAnalysisSink(char[] text,
                                                             int start,
                                                             int length,
                                                             char[] locale,
                                                             int direction,
                                                             long numberSubstitution);

    static final JFXTextAnalysisSink NewJFXTextAnalysisSink(char[] text,
                                                            int start,
                                                            int length,
                                                            String locale,
                                                            int direction) {
        long ptr = _NewJFXTextAnalysisSink(text, //NOT NULL terminator
                                           start, length,
                                           (locale+'\0').toCharArray(),//NULL terminator
                                           direction, 0);
        return ptr != 0 ? new JFXTextAnalysisSink(ptr) : null;
    }

    private static final native long _NewJFXTextRenderer();
    static final JFXTextRenderer NewJFXTextRenderer() {
        long ptr = _NewJFXTextRenderer();
        return ptr != 0 ? new JFXTextRenderer(ptr) : null;
    }

    //JFXTextAnalysisSink
    static final native boolean Next(long ptr);
    static final native int GetStart(long ptr);
    static final native int GetLength(long ptr);
    static final native DWRITE_SCRIPT_ANALYSIS GetAnalysis(long ptr);

    //JFXTextRenderer
    static final native boolean JFXTextRendererNext(long ptr);
    static final native int JFXTextRendererGetStart(long ptr);
    static final native int JFXTextRendererGetLength(long ptr);
    static final native int JFXTextRendererGetGlyphCount(long ptr);
    static final native int JFXTextRendererGetTotalGlyphCount(long ptr);
    static final native long JFXTextRendererGetFontFace(long ptr);
    static final native int JFXTextRendererGetGlyphIndices(long ptr, int[] glyphs, int start, int slot);
    static final native int JFXTextRendererGetGlyphAdvances(long ptr, float[] advances, int start);
    static final native int JFXTextRendererGetGlyphOffsets(long ptr, float[] offsets, int start);
    static final native int JFXTextRendererGetClusterMap(long ptr, short[] clusterMap, int textStart, int glyphStart);

    //IDWriteFontFace
    static final native DWRITE_GLYPH_METRICS GetDesignGlyphMetrics(long ptr, short glyphIndex, boolean isSideways);
    static final native Path2D GetGlyphRunOutline(long ptr, float emSize, short glyphIndex, boolean isSideways);

    //IDWriteFont
    static final native long CreateFontFace(long ptr);
    static final native long GetFaceNames(long ptr);
    static final native long GetFontFamily(long ptr);
    static final native int GetStretch(long ptr);
    static final native int GetStyle(long ptr);
    static final native int GetWeight(long ptr);
    static final native long GetInformationalStrings(long ptr, int informationalStringID);
    static final native int GetSimulations(long ptr);

    //IDWriteFontList
    static final native int GetFontCount(long ptr);
    static final native long GetFont(long ptr, int index);

    //IDWriteFontFile
    static final native int Analyze(long ptr, boolean[] isSupportedFontType, int[] fontFileType, int[] fontFaceType, int[] numberOfFaces);

    //IDWriteLocalizedStrings
    static final native char[] GetString(long ptr, int index, int size);
    static final native int GetStringLength(long ptr, int index);
    static final native int FindLocaleName(long ptr, char[] locale);

    //IDWriteFontFamily
    static final native long GetFamilyNames(long ptr);
    static final native long GetFirstMatchingFont(long ptr, int weight, int stretch, int style);

    //IDWriteFontCollection
    static final native int GetFontFamilyCount(long ptr);
    static final native long GetFontFamily(long ptr, int index);
    static final native int FindFamilyName(long ptr, char[] familyName);
    static final native long GetFontFromFontFace (long ptr, long fontface);

    //IDWriteGlyphRunAnalysis
    static final native byte[] CreateAlphaTexture(long ptr, int textureType, RECT textureBounds);
    static final native RECT GetAlphaTextureBounds(long ptr, int textureType);

    //IDWriteFactory
    static final native long GetSystemFontCollection(long ptr, boolean checkforupdates);
    static final native long CreateGlyphRunAnalysis(long ptr,
                                                    DWRITE_GLYPH_RUN glyphRun,
                                                    float pixelsPerDip,
                                                    DWRITE_MATRIX transform,
                                                    int renderingMode,
                                                    int measuringMode,
                                                    float baselineOriginX,
                                                    float baselineOriginY);
    static final native long CreateTextAnalyzer(long ptr);
    static final native long CreateTextFormat(long ptr,
                                              char[] fontFamily,
                                              long fontCollection,
                                              int fontWeight,
                                              int fontStyle,
                                              int fontStretch,
                                              float fontSize,
                                              char[] localeName);
    static final native long CreateTextLayout(long ptr,
                                              char[] text,
                                              int stringStart,
                                              int stringLength,
                                              long textFormat,
                                              float maxWidth,
                                              float maxHeight);
    static final native long CreateFontFileReference(long ptr, char[] filePath);
    static final native long CreateFontFace(long ptr,
                                            int fontFaceType,
                                            long fontFiles,
                                            int faceIndex,
                                            int fontFaceSimulationFlags);

    //IUnknown
    static final native int AddRef(long ptr);
    static final native int Release(long ptr);

    //IDWriteTextAnalyzer
    static final native int AnalyzeScript(long ptr, long source, int start, int length, long sink);
    static final native int GetGlyphs(long ptr,
                                      char[] textString,
                                      int textStart,
                                      int textLength,
                                      long fontFace,
                                      boolean isSideways,
                                      boolean isRightToLeft,
                                      DWRITE_SCRIPT_ANALYSIS scriptAnalysis,
                                      char[] localeName,
                                      long numberSubstitution,
                                      long[] features,
                                      int[] featureRangeLengths,
                                      int featureRanges,
                                      int maxGlyphCount,
                                      short[] clusterMap,
                                      short[] textProps,
                                      short[] glyphIndices,
                                      short[] glyphProps,
                                      int[] actualGlyphCount);
    static final native int GetGlyphPlacements(long ptr,
                                               char[] textString,
                                               short[] clusterMap,
                                               short[] textProps,
                                               int textStart,
                                               int textLength,
                                               short[] glyphIndices,
                                               short[] glyphProps,
                                               int glyphCount,
                                               long fontFace,
                                               float fontEmSize,
                                               boolean isSideways,
                                               boolean isRightToLeft,
                                               DWRITE_SCRIPT_ANALYSIS scriptAnalysis,
                                               char[] localeName,
                                               long[] features,
                                               int[] featureRangeLengths,
                                               int featureRanges,
                                               float[] glyphAdvances,
                                               float[] glyphOffsets);

    //IDWriteTextLayout
    static final native int Draw(long ptr, long clientData, long renderer, float x , float y);

    //IWICImagingFactory
    static final native long CreateBitmap(long ptr, int uiWidth, int uiHeight, int pixelFormat, int options);

    //IWICBitmap
    static final native long Lock(long ptr, int x, int y, int width, int height, int flags);

    //IWICBitmapLock
    static final native byte[] GetDataPointer(long ptr);
    static final native int GetStride(long ptr);

    //ID2D1Factory
    static final native long CreateWicBitmapRenderTarget(long ptr, long target, D2D1_RENDER_TARGET_PROPERTIES renderTargetProperties);

    //ID2D1RenderTarget
    static final native void BeginDraw(long ptr);
    static final native int EndDraw(long ptr);
    static final native void Clear(long ptr, D2D1_COLOR_F clearColor);
    static final native void SetTextAntialiasMode(long ptr, int textAntialiasMode);
    static final native void SetTransform(long ptr, D2D1_MATRIX_3X2_F transform);
    static final native void DrawGlyphRun(long ptr, D2D1_POINT_2F baselineOrigin, DWRITE_GLYPH_RUN glyphRun, long foregroundBrush, int measuringMode);
    static final native long CreateSolidColorBrush(long ptr, D2D1_COLOR_F color);
}
