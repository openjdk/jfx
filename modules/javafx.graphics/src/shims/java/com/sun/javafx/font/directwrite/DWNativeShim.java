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

package com.sun.javafx.font.directwrite;

import com.sun.javafx.geom.Path2D;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.util.List;

/**
 * Test access to the DirectWrite facade of {@code com.sun.javafx.font.directwrite}, {@link DWNative},
 * and to the peer classes that now call it. The {@link Api} interface is what is left of the shape
 * that carried two implementations while the JNI natives of {@code OS} were still called: a test drove
 * both with the same arguments and compared them. Those comparisons were deleted with the flip - both
 * sides became the same code - and the interface stayed because the fixtures and tests are written
 * against it.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, so it may
 * call the package-private facade and the package-private natives; the tests in
 * {@code test.com.sun.javafx.font.directwrite} reach it through the
 * {@code --add-exports javafx.graphics/com.sun.javafx.font.directwrite=ALL-UNNAMED} line of
 * {@code src/test/addExports}. Every downcall a test makes goes through here, so the restricted
 * {@code java.lang.foreign} calls stay inside the module that {@code --enable-native-access} names.
 * <p>
 * Pointers cross as {@code long}, exactly as they do inside the package. Every method that returns a
 * COM interface pointer returns one that already carries a reference for the caller: the test owns it
 * and must {@link Api#release} it exactly once.
 * <p>
 * Line numbers into {@code directwrite.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/directwrite.cpp}).
 */
public final class DWNativeShim {

    private DWNativeShim() {
    }

    /**
     * The DirectWrite calls this port converts. Method names, arguments and return values are those of
     * the {@code OS} natives they replaced, so a test written against this interface is written against
     * what the JNI boundary did.
     */
    public interface Api {

        String name();

        long createFactory(int factoryType);

        int addRef(long self);

        int release(long self);

        long getSystemFontCollection(long self, boolean checkForUpdates);

        long createFontFileReference(long self, char[] filePath);

        long createFontFace(long self, int fontFaceType, long fontFile, int faceIndex, int simulations);

        int analyze(long self, boolean[] isSupportedFontType, int[] fontFileType, int[] fontFaceType,
                    int[] numberOfFaces);

        int getFontFamilyCount(long self);

        long getFontFamily(long self, int index);

        int findFamilyName(long self, char[] familyName);

        long getFontFromFontFace(long self, long fontFace);

        int getFontCount(long self);

        long getFont(long self, int index);

        long getFamilyNames(long self);

        long getFirstMatchingFont(long self, int weight, int stretch, int style);

        long createFontFace(long self);

        long getFaceNames(long self);

        long getFontFamily(long self);

        int getStretch(long self);

        int getStyle(long self);

        int getWeight(long self);

        long getInformationalStrings(long self, int informationalStringID);

        int getSimulations(long self);

        char[] getString(long self, int index, int size);

        int getStringLength(long self, int index);

        int findLocaleName(long self, char[] locale);

        /**
         * {@code IDWriteFontFace::GetDesignGlyphMetrics} as the seven fields of
         * {@code DWRITE_GLYPH_METRICS} in declaration order, or {@code null} when the call failed.
         * The mirror class is package-private, so the seven ints cross instead of the object.
         */
        int[] designGlyphMetrics(long fontFace, short glyphIndex, boolean isSideways);

        /** {@code IDWriteFontFace::GetGlyphRunOutline}, driving each path with its own geometry sink. */
        Path2D glyphRunOutline(long fontFace, float emSize, short glyphIndex, boolean isSideways);

        /** {@code IDWriteFactory::CreateGlyphRunAnalysis}; {@code transform} is six floats or null. */
        long createGlyphRunAnalysis(long factory, GlyphRun run, float pixelsPerDip, float[] transform,
                                    int renderingMode, int measuringMode, float baselineOriginX,
                                    float baselineOriginY);

        /** {@code IDWriteGlyphRunAnalysis::GetAlphaTextureBounds} as left, top, right, bottom. */
        int[] alphaTextureBounds(long analysis, int textureType);

        /** {@code IDWriteGlyphRunAnalysis::CreateAlphaTexture}; {@code bounds} is four ints or null. */
        byte[] alphaTexture(long analysis, int textureType, int[] bounds);

        /* ---- Shaping, text formats, text layouts and the two inbound COM objects ---- */

        /** {@code IDWriteFactory::CreateTextAnalyzer}. */
        long createTextAnalyzer(long factory);

        /** {@code IDWriteFactory::CreateTextFormat}; both names carry their terminator. */
        long createTextFormat(long factory, char[] fontFamily, long fontCollection, int fontWeight,
                              int fontStyle, int fontStretch, float fontSize, char[] localeName);

        /**
         * {@code IDWriteFactory::CreateTextLayout} over {@code text[start .. start+count)} - the
         * offset the C applied at {@code directwrite.cpp:1904} and the FFM path must apply too.
         */
        long createTextLayout(long factory, char[] text, int start, int count, long textFormat,
                              float maxWidth, float maxHeight);

        /** {@code IDWriteTextAnalyzer::AnalyzeScript}; the two pointers are interfaces of one object. */
        int analyzeScript(long analyzer, long source, int textPosition, int textLength, long sink);

        /** {@code IDWriteTextAnalyzer::GetGlyphs}; {@code scriptAnalysis} is {@code {script, shapes}}. */
        int getGlyphs(long analyzer, char[] textString, int textStart, int textLength, long fontFace,
                      boolean isSideways, boolean isRightToLeft, int[] scriptAnalysis,
                      char[] localeName, long numberSubstitution, long[] features,
                      int[] featureRangeLengths, int featureRanges, int maxGlyphCount,
                      short[] clusterMap, short[] textProps, short[] glyphIndices, short[] glyphProps,
                      int[] actualGlyphCount);

        /** {@code IDWriteTextAnalyzer::GetGlyphPlacements}. */
        int getGlyphPlacements(long analyzer, char[] textString, short[] clusterMap, short[] textProps,
                               int textStart, int textLength, short[] glyphIndices, short[] glyphProps,
                               int glyphCount, long fontFace, float fontEmSize, boolean isSideways,
                               boolean isRightToLeft, int[] scriptAnalysis, char[] localeName,
                               long[] features, int[] featureRangeLengths, int featureRanges,
                               float[] glyphAdvances, float[] glyphOffsets);

        /** {@code IDWriteTextLayout::Draw}: the call that drives the renderer. */
        int draw(long layout, long clientDrawingContext, long renderer, float originX, float originY);

        /**
         * The analysis sink and source object. <b>The returned value is a handle, not always a
         * pointer</b>: the JNI path answers the {@code JFXTextAnalysisSink*} it allocated, the FFM
         * path a registry id. {@link #analysisSinkPointer} and {@link #analysisSourcePointer} turn
         * either into the two interface pointers, which is what the caller passes to
         * {@link #analyzeScript}. {@code 0} means "not created", as {@code OS.java:195} means it.
         */
        long newAnalysisSink(char[] text, int start, int length, String locale, int direction);

        /** The {@code IDWriteTextAnalysisSink*} of a handle. */
        long analysisSinkPointer(long handle);

        /** The {@code IDWriteTextAnalysisSource*} of a handle - not the same pointer as the sink's. */
        long analysisSourcePointer(long handle);

        boolean analysisSinkNext(long handle);

        int analysisSinkGetStart(long handle);

        int analysisSinkGetLength(long handle);

        /** {@code GetAnalysis} as {@code {script, shapes}}; zero-filled, never null, out of range. */
        int[] analysisSinkGetAnalysis(long handle);

        int analysisSinkAddRef(long handle);

        int analysisSinkRelease(long handle);

        /**
         * Frees whatever the handle owns. The JNI object deleted itself when its count reached zero,
         * so this is a no-op there; the FFM object needs its arena closed and its registry entry
         * removed, on the thread that created it.
         */
        void analysisSinkDispose(long handle);

        /** The text renderer object, with the same handle convention as {@link #newAnalysisSink}. */
        long newTextRenderer();

        /** The {@code IDWriteTextRenderer*} of a handle, to pass to {@link #draw}. */
        long textRendererPointer(long handle);

        boolean textRendererNext(long handle);

        int textRendererGetStart(long handle);

        int textRendererGetLength(long handle);

        int textRendererGetGlyphCount(long handle);

        int textRendererGetTotalGlyphCount(long handle);

        /** The borrowed {@code IDWriteFontFace*} of the current run, or 0 out of range. */
        long textRendererGetFontFace(long handle);

        int textRendererGetGlyphIndices(long handle, int[] glyphs, int start, int slot);

        int textRendererGetGlyphAdvances(long handle, float[] advances, int start);

        int textRendererGetGlyphOffsets(long handle, float[] offsets, int start);

        int textRendererGetClusterMap(long handle, short[] clusterMap, int textStart, int glyphStart);

        int textRendererAddRef(long handle);

        int textRendererRelease(long handle);

        void textRendererDispose(long handle);
        /* ---- The COM apartment, and the Direct2D + WIC greyscale mask path ---- */

        /**
         * {@code CoInitializeEx(NULL, dwCoInit)}. Per <b>thread</b>, and reference counted: every
         * call that answers {@code true} other than one refused with {@code RPC_E_CHANGED_MODE} must
         * be balanced by a {@link #coUninitialize} on the same thread.
         */
        boolean coInitializeEx(int dwCoInit);

        /** {@code CoUninitialize()}, on the thread that initialized. */
        void coUninitialize();

        /** {@code CoCreateInstance(CLSID_WICImagingFactory, ..., IID_IWICImagingFactory, &p)}. */
        long wicCreateImagingFactory();

        /** {@code d2d1.dll!D2D1CreateFactory(factoryType, __uuidof(ID2D1Factory), &options, &p)}. */
        long d2d1CreateFactory(int factoryType);

        /** {@code IWICImagingFactory::CreateBitmap}; {@code pixelFormat} is an {@code OS} constant. */
        long createBitmap(long factory, int uiWidth, int uiHeight, int pixelFormat, int options);

        /** {@code IWICBitmap::Lock} over the rectangle, which is never {@code NULL}. */
        long lock(long bitmap, int x, int y, int width, int height, int flags);

        /** {@code IWICBitmapLock::GetDataPointer}: the whole locked buffer, or {@code null}. */
        byte[] getDataPointer(long bitmapLock);

        /** {@code IWICBitmapLock::GetStride}: the row stride, or {@code 0} on failure. */
        int getStride(long bitmapLock);

        /** {@code ID2D1Factory::CreateWicBitmapRenderTarget}; {@code null} properties means NULL. */
        long createWicBitmapRenderTarget(long factory, long bitmap, RenderTargetProperties properties);

        /** {@code ID2D1RenderTarget::BeginDraw}. */
        void beginDraw(long target);

        /** {@code ID2D1RenderTarget::EndDraw} with both tag out-parameters NULL. */
        int endDraw(long target);

        /** {@code ID2D1RenderTarget::Clear}; {@code color} is {r, g, b, a} or {@code null} for NULL. */
        void clear(long target, float[] color);

        /** {@code ID2D1RenderTarget::SetTextAntialiasMode}. */
        void setTextAntialiasMode(long target, int textAntialiasMode);

        /** {@code ID2D1RenderTarget::SetTransform}; six floats {m11, m12, m21, m22, dx, dy} or null. */
        void setTransform(long target, float[] transform);

        /**
         * {@code ID2D1RenderTarget::DrawGlyphRun}. {@code baselineOrigin} is {x, y} - the one
         * aggregate passed <b>by value</b> on the mask path - or {@code null}, which the C turned into
         * an uninitialised stack struct and the facade turns into zeroes.
         */
        void drawGlyphRun(long target, float[] baselineOrigin, GlyphRun run, long brush,
                          int measuringMode);

        /** {@code ID2D1RenderTarget::CreateSolidColorBrush}; {@code color} is {r, g, b, a} or null. */
        long createSolidColorBrush(long target, float[] color);

    }

    /** The facade every peer now calls: {@link DWNative}. */
    public static Api ffm() {
        return FFM;
    }

    /* ---------------------------------------------------------------------------------------------
     * Linkage and constants of the facade
     * ------------------------------------------------------------------------------------------- */

    /** Runs {@link DWNative}'s class initializer. */
    public static void ensureLoaded() {
        DWNative.ensureLoaded();
    }

    /** Whether {@code dwrite.dll} loaded and exports {@code DWriteCreateFactory}. */
    public static boolean isAvailable() {
        return DWNative.isAvailable();
    }

    /** The symbols the facade bound by name, as {@code <dll>!<name>}. */
    public static List<String> boundSymbols() {
        return DWNative.boundSymbols();
    }

    /** Whether {@code fileName} loads: the "DirectWrite may be absent" catch, exercised. */
    public static boolean canLoad(String fileName) {
        return DWNative.canLoad(fileName);
    }

    public static byte[] guidBytes(String uuid) {
        return DWNative.guidBytes(uuid);
    }

    public static long guidLayoutByteSize() {
        return DWNative.GUID_LAYOUT.byteSize();
    }

    public static long guidLayoutOffset(String field) {
        return DWNative.GUID_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    public static String iidUnknown() {
        return DWNative.IID_IUNKNOWN;
    }

    public static String iidDWriteFactory() {
        return DWNative.IID_IDWRITE_FACTORY;
    }

    public static int eFail() {
        return DWNative.E_FAIL;
    }

    public static int sOk() {
        return OS.S_OK;
    }

    public static int factoryTypeShared() {
        return OS.DWRITE_FACTORY_TYPE_SHARED;
    }

    /** {@code IUnknown::QueryInterface}, slot 0 - the facade only; {@code OS} never bound it. */
    public static long queryInterface(long self, String iid) {
        return DWNative.queryInterface(self, iid);
    }

    /* ---------------------------------------------------------------------------------------------
     * Peer behaviour that the lifetime rule depends on
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code IUnknown.Release()} twice on one peer: {@code {first, second, peer.ptr}}. The second call
     * must return 0 without touching native memory, and the peer must have zeroed its pointer - that is
     * what makes {@code DWDisposer.dispose()} and an explicit {@code Release()} safe together.
     */
    public static long[] releaseTwiceThroughPeer(long ptr) {
        IUnknown peer = new IUnknown(ptr);
        long first = peer.Release();
        long second = peer.Release();
        return new long[] { first, second, peer.ptr };
    }


    /* ---------------------------------------------------------------------------------------------
     * The single-glyph DWRITE_GLYPH_RUN, as a shape a test can build
     * ------------------------------------------------------------------------------------------- */

    /**
     * What {@code DWRITE_GLYPH_RUN} holds in this package: one glyph, one advance, one offset. The
     * mirror class is package-private and both paths need the same values, so this is the shape the
     * tests build and either implementation converts.
     */
    public static final class GlyphRun {

        public long fontFace;
        public float fontEmSize;
        public short glyphIndices;
        public float glyphAdvances;
        public float advanceOffset;
        public float ascenderOffset;
        public boolean isSideways;
        public int bidiLevel;

        /** The run {@code DWGlyph} builds in its constructor: a face, a size and a glyph code. */
        public static GlyphRun of(long fontFace, float fontEmSize, short glyphIndex) {
            GlyphRun run = new GlyphRun();
            run.fontFace = fontFace;
            run.fontEmSize = fontEmSize;
            run.glyphIndices = glyphIndex;
            return run;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontFace calls that were never JNI natives, so only the facade can make them
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteFontFace::GetType}, slot 3. */
    public static int fontFaceType(long fontFace) {
        return DWNative.getFontFaceType(fontFace);
    }

    /** {@code IDWriteFontFace::GetIndex}, slot 5. */
    public static int fontFaceIndex(long fontFace) {
        return DWNative.getFontFaceIndex(fontFace);
    }

    /** {@code IDWriteFontFace::GetSimulations}, slot 6. */
    public static int fontFaceSimulations(long fontFace) {
        return DWNative.getFontFaceSimulations(fontFace);
    }

    /** {@code IDWriteFontFace::IsSymbolFont}, slot 7. */
    public static boolean isSymbolFont(long fontFace) {
        return DWNative.isSymbolFont(fontFace);
    }

    /** {@code IDWriteFontFace::GetMetrics}, slot 8: the ten {@code DWRITE_FONT_METRICS} fields. */
    public static int[] fontFaceMetrics(long fontFace) {
        return DWNative.getFontFaceMetrics(fontFace);
    }

    /** {@code IDWriteFontFace::GetGlyphCount}, slot 9, widened from {@code UINT16}. */
    public static int glyphCount(long fontFace) {
        return DWNative.getGlyphCount(fontFace);
    }

    /** {@code IDWriteFontFace::GetGlyphIndices}, slot 11: the nominal cmap mapping. */
    public static short[] glyphIndices(long fontFace, int[] codePoints) {
        return DWNative.getGlyphIndices(fontFace, codePoints);
    }

    /** {@code IDWriteFontFace::TryGetFontTable} plus {@code ReleaseFontTable}, slots 12 and 13. */
    public static byte[] fontTable(long fontFace, int openTypeTableTag) {
        return DWNative.tryGetFontTable(fontFace, openTypeTableTag);
    }

    /* ---------------------------------------------------------------------------------------------
     * Struct layouts and the geometry sink
     * ------------------------------------------------------------------------------------------- */

    /** {@code DWRITE_TEXTURE_CLEARTYPE_3x1}, the texture type whose pixels are three bytes deep. */
    public static int textureClearType3x1() {
        return DWNative.DWRITE_TEXTURE_CLEARTYPE_3x1;
    }

    /** {@code ID2D1SimplifiedGeometrySink} / {@code IDWriteGeometrySink}. */
    public static String iidGeometrySink() {
        return DWNative.IID_ID2D1_SIMPLIFIED_GEOMETRY_SINK;
    }

    /** The size in bytes of one of the layouts named by {@link #layout}. */
    public static long layoutByteSize(String name) {
        return layout(name).byteSize();
    }

    /** The byte offset of {@code field} in the layout {@code name}. */
    public static long layoutOffset(String name, String field) {
        return layout(name).byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    private static StructLayout layout(String name) {
        return switch (name) {
            case "DWRITE_GLYPH_METRICS" -> DWNative.DWRITE_GLYPH_METRICS_LAYOUT;
            case "DWRITE_FONT_METRICS" -> DWNative.DWRITE_FONT_METRICS_LAYOUT;
            case "DWRITE_GLYPH_OFFSET" -> DWNative.DWRITE_GLYPH_OFFSET_LAYOUT;
            case "DWRITE_GLYPH_RUN" -> DWNative.DWRITE_GLYPH_RUN_LAYOUT;
            case "DWRITE_MATRIX" -> DWNative.DWRITE_MATRIX_LAYOUT;
            case "RECT" -> DWNative.RECT_LAYOUT;
            case "D2D1_POINT_2F" -> DWNative.D2D1_POINT_2F_LAYOUT;
            case "D2D1_BEZIER_SEGMENT" -> DWNative.D2D1_BEZIER_SEGMENT_LAYOUT;
            case "GUID" -> DWNative.GUID_LAYOUT;
            case "DWRITE_SCRIPT_ANALYSIS" -> DWNative.DWRITE_SCRIPT_ANALYSIS_LAYOUT;
            case "DWRITE_GLYPH_RUN_DESCRIPTION" -> DWNative.DWRITE_GLYPH_RUN_DESCRIPTION_LAYOUT;
            case "ANALYSIS_SINK_OBJECT" -> DWNative.ANALYSIS_SINK_OBJECT_LAYOUT;
            case "TEXT_RENDERER_OBJECT" -> DWNative.TEXT_RENDERER_OBJECT_LAYOUT;
            default -> throw new IllegalArgumentException("no such layout: " + name);
        };
    }

    /** Outline calls currently on some stack: 0 between calls. */
    public static int sinkRegistrySize() {
        return DWNative.sinkRegistrySize();
    }

    /** {@code QueryInterface}, {@code AddRef}, {@code Release} and {@code Close} through the sink vtable. */
    public static long[] sinkSelfTest() {
        return DWNative.sinkSelfTest();
    }

    /** One whole figure driven through the sink vtable, including the by-value point. */
    public static Path2D sinkSelfTestOutline() {
        return DWNative.sinkSelfTestOutline();
    }

    /** The same figure with a callback that throws: what the sink does with a failure. */
    public static long[] sinkSelfTestFailure() {
        return DWNative.sinkSelfTestFailure();
    }

    /** The twelve coordinates {@link #sinkSelfTestOutline} feeds the sink, in call order. */
    public static float[] sinkSelfTestFigure() {
        return DWNative.SINK_SELF_TEST_FIGURE.clone();
    }

    /* ---------------------------------------------------------------------------------------------
     * Conversions between the shapes the tests use and the package-private mirror classes
     * ------------------------------------------------------------------------------------------- */

    private static int[] fromGlyphMetrics(DWRITE_GLYPH_METRICS metrics) {
        if (metrics == null) {
            return null;
        }
        return new int[] { metrics.leftSideBearing, metrics.advanceWidth, metrics.rightSideBearing,
                metrics.topSideBearing, metrics.advanceHeight, metrics.bottomSideBearing,
                metrics.verticalOriginY };
    }

    private static DWRITE_GLYPH_RUN toGlyphRun(GlyphRun source) {
        if (source == null) {
            return null;
        }
        DWRITE_GLYPH_RUN run = new DWRITE_GLYPH_RUN();
        run.fontFace = source.fontFace;
        run.fontEmSize = source.fontEmSize;
        run.glyphIndices = source.glyphIndices;
        run.glyphAdvances = source.glyphAdvances;
        run.advanceOffset = source.advanceOffset;
        run.ascenderOffset = source.ascenderOffset;
        run.isSideways = source.isSideways;
        run.bidiLevel = source.bidiLevel;
        return run;
    }

    private static DWRITE_MATRIX toMatrix(float[] source) {
        if (source == null) {
            return null;
        }
        DWRITE_MATRIX matrix = new DWRITE_MATRIX();
        matrix.m11 = source[0];
        matrix.m12 = source[1];
        matrix.m21 = source[2];
        matrix.m22 = source[3];
        matrix.dx = source[4];
        matrix.dy = source[5];
        return matrix;
    }

    private static RECT toRect(int[] source) {
        if (source == null) {
            return null;
        }
        RECT rect = new RECT();
        rect.left = source[0];
        rect.top = source[1];
        rect.right = source[2];
        rect.bottom = source[3];
        return rect;
    }

    private static int[] fromRect(RECT rect) {
        return rect == null ? null : new int[] { rect.left, rect.top, rect.right, rect.bottom };
    }

    /* ---------------------------------------------------------------------------------------------
     * Shaping: what only the FFM path has
     *
     * The slot table, the four IIDs of the two inbound objects, the recording constructors and the
     * hooks that drive the synthesized vtables. None of these has a JNI counterpart - the C objects
     * are opaque - so they are statics here rather than rows of Api.
     * ------------------------------------------------------------------------------------------- */

    /** {@code IDWriteTextAnalysisSink}. */
    public static String iidAnalysisSink() {
        return DWNative.IID_IDWRITE_TEXT_ANALYSIS_SINK;
    }

    /** {@code IDWriteTextAnalysisSource}. */
    public static String iidAnalysisSource() {
        return DWNative.IID_IDWRITE_TEXT_ANALYSIS_SOURCE;
    }

    /** {@code IDWriteTextRenderer}. */
    public static String iidTextRenderer() {
        return DWNative.IID_IDWRITE_TEXT_RENDERER;
    }

    /** {@code IDWritePixelSnapping}, the renderer's base interface. */
    public static String iidPixelSnapping() {
        return DWNative.IID_IDWRITE_PIXEL_SNAPPING;
    }

    /** The vtable slot the facade uses for {@code name}, as derived from {@code dwrite.h}. */
    public static int layoutSlot(String name) {
        return DWNative.layoutSlot(name);
    }

    /** The distance from an analysis object's sink interface to its source interface. */
    public static long analysisSourceOffset() {
        return DWNative.analysisSourceOffset();
    }

    /** {@code IDWriteTextAnalysisSource::GetParagraphReadingDirection} on any source pointer. */
    public static int paragraphReadingDirection(long source) {
        return DWNative.getParagraphReadingDirection(source);
    }

    /**
     * Drives the flipped peers exactly as {@code DWGlyphLayout.addTextRun} does - create the
     * analysis sink, {@code AddRef}, {@code IDWriteTextAnalyzer::AnalyzeScript}, drain, release,
     * dispose - and reports what the peer handed DirectWrite:
     * <p>
     * {@code {sinkPointer, sourcePointer, readingDirectionAskedOfTheSource,
     * readingDirectionAskedOfTheSink, hresult, runs, sinksLeftInTheRegistry}}.
     * <p>
     * The third and fourth entries are the point: only a real {@code IDWriteTextAnalysisSource}
     * answers a reading direction, and the same call eight bytes earlier lands on the sink's slot 5,
     * {@code SetBidiLevel}, which answers {@code S_OK} and touches nothing.
     */
    public static long[] analysisSinkPeerProbe(char[] text, int start, int length, String locale,
                                               int direction) {
        IDWriteFactory factory = IDWriteFactory.create(OS.DWRITE_FACTORY_TYPE_SHARED);
        if (factory == null) {
            return new long[0];
        }
        try {
            IDWriteTextAnalyzer analyzer = factory.CreateTextAnalyzer();
            if (analyzer == null) {
                return new long[0];
            }
            JFXTextAnalysisSink sink = JFXTextAnalysisSink.create(text, start, length, locale, direction);
            if (sink == null) {
                analyzer.Release();
                return new long[0];
            }
            long[] answers;
            try {
                sink.AddRef();
                long sinkPointer = sink.sinkPointer();
                long sourcePointer = sink.sourcePointer();
                int hr = analyzer.AnalyzeScript(sink, 0, length, sink);
                int runs = 0;
                while (sink.Next()) {
                    runs++;
                }
                answers = new long[] { sinkPointer, sourcePointer,
                        DWNative.getParagraphReadingDirection(sourcePointer),
                        DWNative.getParagraphReadingDirection(sinkPointer), hr, runs, 0 };
            } finally {
                analyzer.Release();
                sink.Release();
                sink.dispose();
            }
            // Read after the dispose, which is the property the last entry reports.
            answers[6] = DWNative.analysisSinkRegistrySize();
            return answers;
        } finally {
            factory.Release();
        }
    }

    /** {@code IDWriteTextAnalyzer::AnalyzeBidi}: no JNI counterpart, it was never a native. */
    public static int analyzeBidi(long analyzer, long source, int position, int length, long sink) {
        return DWNative.analyzeBidi(analyzer, source, position, length, sink);
    }

    /** {@code IDWriteTextAnalyzer::AnalyzeLineBreakpoints}. */
    public static int analyzeLineBreakpoints(long analyzer, long source, int position, int length,
                                             long sink) {
        return DWNative.analyzeLineBreakpoints(analyzer, source, position, length, sink);
    }

    /** {@code IDWriteTextAnalyzer::AnalyzeNumberSubstitution}. */
    public static int analyzeNumberSubstitution(long analyzer, long source, int position, int length,
                                                long sink) {
        return DWNative.analyzeNumberSubstitution(analyzer, source, position, length, sink);
    }

    /* The format and layout accessors: never natives, bound to pin the slot arithmetic. */

    public static int textFormatFontWeight(long format) {
        return DWNative.getTextFormatFontWeight(format);
    }

    public static int textFormatFontStyle(long format) {
        return DWNative.getTextFormatFontStyle(format);
    }

    public static int textFormatFontStretch(long format) {
        return DWNative.getTextFormatFontStretch(format);
    }

    public static float textFormatFontSize(long format) {
        return DWNative.getTextFormatFontSize(format);
    }

    public static String textFormatFontFamilyName(long format) {
        return DWNative.getTextFormatFontFamilyName(format);
    }

    public static String textFormatLocaleName(long format) {
        return DWNative.getTextFormatLocaleName(format);
    }

    public static int textFormatFontFamilyNameLength(long format) {
        return DWNative.getTextFormatFontFamilyNameLength(format);
    }

    public static int textFormatLocaleNameLength(long format) {
        return DWNative.getTextFormatLocaleNameLength(format);
    }

    public static float textLayoutMaxWidth(long layout) {
        return DWNative.getTextLayoutMaxWidth(layout);
    }

    public static float textLayoutMaxHeight(long layout) {
        return DWNative.getTextLayoutMaxHeight(layout);
    }

    /* The two inbound objects, as only the FFM path can expose them. */

    /** Live analysis sinks: the leak check. */
    public static int analysisSinkRegistrySize() {
        return DWNative.analysisSinkRegistrySize();
    }

    /** Live text renderers: the leak check. */
    public static int textRendererRegistrySize() {
        return DWNative.textRendererRegistrySize();
    }

    /** An analysis sink that records every callback it receives, in order. */
    public static long newRecordingAnalysisSink(char[] text, int start, int length, String locale,
                                                int direction) {
        return DWNative.newRecordingAnalysisSink(text, start, length, terminated(locale), direction, 0L);
    }

    /** A text renderer that records every callback it receives, in order. */
    public static long newRecordingTextRenderer() {
        return DWNative.newRecordingTextRenderer();
    }

    public static List<String> analysisSinkCallbacks(long handle) {
        return DWNative.analysisSinkCallbacks(handle);
    }

    public static List<String> textRendererCallbacks(long handle) {
        return DWNative.textRendererCallbacks(handle);
    }

    /** The IIDs DirectWrite asked the object for - empty when it never asked. */
    public static List<String> analysisSinkQueriedIids(long handle) {
        return DWNative.analysisSinkQueriedIids(handle);
    }

    /** The IIDs DirectWrite asked the renderer for. */
    public static List<String> textRendererQueriedIids(long handle) {
        return DWNative.textRendererQueriedIids(handle);
    }

    /** {@code QueryInterface}, {@code AddRef} and {@code Release} through both analysis vtables. */
    public static long[] analysisSinkSelfTest(long handle) {
        return DWNative.analysisSinkSelfTest(handle);
    }

    /** All nine collectors and queries driven through the analysis vtables. */
    public static long[] analysisSinkDriveCallbacks(long handle, int position, int length, short script,
                                                    int shapes, int explicitLevel, int resolvedLevel) {
        return DWNative.analysisSinkDriveCallbacks(handle, position, length, script, shapes,
                explicitLevel, resolvedLevel);
    }

    /** {@code QueryInterface}, {@code AddRef} and {@code Release} through the renderer vtable. */
    public static long[] textRendererSelfTest(long handle) {
        return DWNative.textRendererSelfTest(handle);
    }

    /** The three pixel-snapping queries driven through the renderer vtable. */
    public static long[] textRendererDriveQueries(long handle) {
        return DWNative.textRendererDriveQueries(handle);
    }

    /** {@code DrawUnderline}, {@code DrawStrikethrough} and {@code DrawInlineObject}. */
    public static long[] textRendererDriveIgnored(long handle) {
        return DWNative.textRendererDriveIgnored(handle);
    }

    /** One synthetic glyph run fed through {@code DrawGlyphRun} with values the test chooses. */
    public static int textRendererFeedRun(long handle, float baselineOriginX, float baselineOriginY,
                                          int measuringMode, long fontFace, float fontEmSize,
                                          short[] glyphIndices, float[] glyphAdvances,
                                          float[] glyphOffsets, short[] clusterMap, int textPosition,
                                          int stringLength, int isSideways, int bidiLevel,
                                          boolean withDescription) {
        return DWNative.textRendererFeedRun(handle, baselineOriginX, baselineOriginY, measuringMode,
                fontFace, fontEmSize, glyphIndices, glyphAdvances, glyphOffsets, clusterMap,
                textPosition, stringLength, isSideways, bidiLevel, withDescription);
    }

    /** The baseline origin and measuring mode of one collected run, as raw float bits. */
    public static long[] textRendererRunGeometry(long handle, int index) {
        return DWNative.textRendererRunGeometry(handle, index);
    }

    /** How many runs the renderer collected, without moving its cursor. */
    public static int textRendererRunCount(long handle) {
        return DWNative.textRendererRunCount(handle);
    }

    /** Both objects called through pointers whose arenas are gone: no record, no crash. */
    public static long[] staleCallbackProbe(long sinkBlock, long rendererBlock) {
        return DWNative.staleCallbackProbe(sinkBlock, rendererBlock);
    }

    /** What the peers do to a locale before it crosses: append the terminator the C never added. */
    private static char[] terminated(String text) {
        return text == null ? null : (text + '\0').toCharArray();
    }

    private static DWRITE_SCRIPT_ANALYSIS toScriptAnalysis(int[] source) {
        if (source == null) {
            return null;
        }
        DWRITE_SCRIPT_ANALYSIS analysis = new DWRITE_SCRIPT_ANALYSIS();
        analysis.script = (short) source[0];
        analysis.shapes = source[1];
        return analysis;
    }

    private static int[] fromScriptAnalysis(DWRITE_SCRIPT_ANALYSIS analysis) {
        return analysis == null ? null : new int[] { analysis.script & 0xFFFF, analysis.shapes };
    }
    /* ---------------------------------------------------------------------------------------------
     * Direct2D + WIC: shared shapes, the OS constants, and what only the FFM path has
     * ------------------------------------------------------------------------------------------- */

    /**
     * What {@code D2D1_RENDER_TARGET_PROPERTIES} holds, with its nested {@code D2D1_PIXEL_FORMAT}
     * flattened. The mirror classes are package-private, so this is the shape the tests build and
     * either implementation converts.
     */
    public static final class RenderTargetProperties {

        public int type;
        public int pixelFormatFormat;
        public int pixelFormatAlphaMode;
        public float dpiX;
        public float dpiY;
        public int usage;
        public int minLevel;

        /** Exactly what {@code DWGlyph.createRenderingTarget} builds: every value at its default. */
        public static RenderTargetProperties defaults() {
            RenderTargetProperties properties = new RenderTargetProperties();
            properties.type = OS.D2D1_RENDER_TARGET_TYPE_DEFAULT;
            properties.pixelFormatFormat = OS.DXGI_FORMAT_UNKNOWN;
            properties.pixelFormatAlphaMode = OS.D2D1_ALPHA_MODE_UNKNOWN;
            properties.dpiX = 0;
            properties.dpiY = 0;
            properties.usage = OS.D2D1_RENDER_TARGET_USAGE_NONE;
            properties.minLevel = OS.D2D1_FEATURE_LEVEL_DEFAULT;
            return properties;
        }
    }

    private static D2D1_RENDER_TARGET_PROPERTIES toRenderTargetProperties(RenderTargetProperties src) {
        if (src == null) {
            return null;
        }
        D2D1_RENDER_TARGET_PROPERTIES properties = new D2D1_RENDER_TARGET_PROPERTIES();
        properties.type = src.type;
        properties.pixelFormat.format = src.pixelFormatFormat;
        properties.pixelFormat.alphaMode = src.pixelFormatAlphaMode;
        properties.dpiX = src.dpiX;
        properties.dpiY = src.dpiY;
        properties.usage = src.usage;
        properties.minLevel = src.minLevel;
        return properties;
    }

    private static D2D1_COLOR_F toColor(float[] source) {
        return source == null ? null : new D2D1_COLOR_F(source[0], source[1], source[2], source[3]);
    }

    private static D2D1_MATRIX_3X2_F toMatrix3x2(float[] source) {
        if (source == null) {
            return null;
        }
        return new D2D1_MATRIX_3X2_F(source[0], source[1], source[2], source[3], source[4], source[5]);
    }

    private static D2D1_POINT_2F toPoint(float[] source) {
        return source == null ? null : new D2D1_POINT_2F(source[0], source[1]);
    }

    /**
     * An {@code OS} constant by name. {@code OS} is package-private and the tests need a dozen of its
     * values; naming them here keeps the numbers in one place instead of copied into the tests.
     */
    public static int osConstant(String name) {
        return switch (name) {
            case "S_OK" -> OS.S_OK;
            case "COINIT_APARTMENTTHREADED" -> OS.COINIT_APARTMENTTHREADED;
            case "COINIT_DISABLE_OLE1DDE" -> OS.COINIT_DISABLE_OLE1DDE;
            case "D2D1_FACTORY_TYPE_SINGLE_THREADED" -> OS.D2D1_FACTORY_TYPE_SINGLE_THREADED;
            case "D2D1_RENDER_TARGET_TYPE_DEFAULT" -> OS.D2D1_RENDER_TARGET_TYPE_DEFAULT;
            case "D2D1_RENDER_TARGET_USAGE_NONE" -> OS.D2D1_RENDER_TARGET_USAGE_NONE;
            case "D2D1_FEATURE_LEVEL_DEFAULT" -> OS.D2D1_FEATURE_LEVEL_DEFAULT;
            case "D2D1_ALPHA_MODE_UNKNOWN" -> OS.D2D1_ALPHA_MODE_UNKNOWN;
            case "D2D1_ALPHA_MODE_PREMULTIPLIED" -> OS.D2D1_ALPHA_MODE_PREMULTIPLIED;
            case "DXGI_FORMAT_UNKNOWN" -> OS.DXGI_FORMAT_UNKNOWN;
            case "DXGI_FORMAT_B8G8R8A8_UNORM" -> OS.DXGI_FORMAT_B8G8R8A8_UNORM;
            case "D2D1_TEXT_ANTIALIAS_MODE_DEFAULT" -> OS.D2D1_TEXT_ANTIALIAS_MODE_DEFAULT;
            case "D2D1_TEXT_ANTIALIAS_MODE_CLEARTYPE" -> OS.D2D1_TEXT_ANTIALIAS_MODE_CLEARTYPE;
            case "D2D1_TEXT_ANTIALIAS_MODE_GRAYSCALE" -> OS.D2D1_TEXT_ANTIALIAS_MODE_GRAYSCALE;
            case "D2D1_TEXT_ANTIALIAS_MODE_ALIASED" -> OS.D2D1_TEXT_ANTIALIAS_MODE_ALIASED;
            case "GUID_WICPixelFormat8bppGray" -> OS.GUID_WICPixelFormat8bppGray;
            case "GUID_WICPixelFormat8bppAlpha" -> OS.GUID_WICPixelFormat8bppAlpha;
            case "GUID_WICPixelFormat16bppGray" -> OS.GUID_WICPixelFormat16bppGray;
            case "GUID_WICPixelFormat24bppRGB" -> OS.GUID_WICPixelFormat24bppRGB;
            case "GUID_WICPixelFormat24bppBGR" -> OS.GUID_WICPixelFormat24bppBGR;
            case "GUID_WICPixelFormat32bppBGR" -> OS.GUID_WICPixelFormat32bppBGR;
            case "GUID_WICPixelFormat32bppBGRA" -> OS.GUID_WICPixelFormat32bppBGRA;
            case "GUID_WICPixelFormat32bppPBGRA" -> OS.GUID_WICPixelFormat32bppPBGRA;
            case "GUID_WICPixelFormat32bppGrayFloat" -> OS.GUID_WICPixelFormat32bppGrayFloat;
            case "GUID_WICPixelFormat32bppRGBA" -> OS.GUID_WICPixelFormat32bppRGBA;
            case "GUID_WICPixelFormat32bppPRGBA" -> OS.GUID_WICPixelFormat32bppPRGBA;
            case "WICBitmapNoCache" -> OS.WICBitmapNoCache;
            case "WICBitmapCacheOnDemand" -> OS.WICBitmapCacheOnDemand;
            case "WICBitmapCacheOnLoad" -> OS.WICBitmapCacheOnLoad;
            case "WICBitmapLockRead" -> OS.WICBitmapLockRead;
            case "WICBitmapLockWrite" -> OS.WICBitmapLockWrite;
            case "DWRITE_MEASURING_MODE_NATURAL" -> OS.DWRITE_MEASURING_MODE_NATURAL;
            case "DWRITE_RENDERING_MODE_NATURAL" -> OS.DWRITE_RENDERING_MODE_NATURAL;
            case "DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC" -> OS.DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC;
            case "DWRITE_TEXTURE_ALIASED_1x1" -> OS.DWRITE_TEXTURE_ALIASED_1x1;
            case "DWRITE_TEXTURE_CLEARTYPE_3x1" -> OS.DWRITE_TEXTURE_CLEARTYPE_3x1;
            default -> throw new IllegalArgumentException("no such OS constant: " + name);
        };
    }

    /** {@code RPC_E_CHANGED_MODE}: the one HRESULT {@code CoInitializeEx} reports as failure. */
    public static int rpcEChangedMode() {
        return DWNative.RPC_E_CHANGED_MODE;
    }

    /** {@code S_FALSE}, what a second {@code CoInitializeEx} on one thread answers. */
    public static int sFalse() {
        return DWNative.S_FALSE;
    }

    /** {@code COINIT_MULTITHREADED}, which is 0 and which no production code passes. */
    public static int coinitMultiThreaded() {
        return 0;
    }

    public static String clsidWicImagingFactory() {
        return DWNative.CLSID_WIC_IMAGING_FACTORY;
    }

    public static String clsidWicImagingFactory2() {
        return DWNative.CLSID_WIC_IMAGING_FACTORY2;
    }

    public static String iidWicImagingFactory() {
        return DWNative.IID_IWIC_IMAGING_FACTORY;
    }

    public static String iidD2D1Factory() {
        return DWNative.IID_ID2D1_FACTORY;
    }

    /** Whether ole32.dll loaded and exports the three COM entry points; loads it if it has not been. */
    public static boolean isOle32Available() {
        return DWNative.isOle32Available();
    }

    /** Whether d2d1.dll loaded and exports {@code D2D1CreateFactory}; loads it if it has not been. */
    public static boolean isD2D1Available() {
        return DWNative.isD2D1Available();
    }

    /** The 16 bytes of a {@code GUID_WICPixelFormat*}, or {@code null} for an unlisted value. */
    public static byte[] wicPixelFormatGuidBytes(int pixelFormat) {
        return DWNative.wicPixelFormatGuidBytes(pixelFormat);
    }

    /** How many pixel formats the facade table carries. */
    public static int wicPixelFormatCount() {
        return DWNative.wicPixelFormatCount();
    }

    /** The vtable slot a named Direct2D/WIC method occupies, as the facade records it. */
    public static int renderSlot(String name) {
        return DWNative.renderSlot(name);
    }

    /** The byte size of a named Direct2D/WIC struct layout. */
    public static long renderLayoutByteSize(String name) {
        return DWNative.layoutByteSize(name);
    }

    /** The byte offset of a named field of a named Direct2D/WIC struct layout. */
    public static long renderLayoutOffset(String name, String field) {
        return DWNative.layoutOffset(name, field);
    }

    /** {@code ID2D1RenderTarget::GetSize} - the only 8-byte struct return in the port. FFM only. */
    public static float[] renderTargetSize(long target) {
        return DWNative.renderTargetSize(target);
    }

    /**
     * Whether the callee hands back the hidden struct-return buffer in RAX, measured on a live
     * render target. FFM only - the JNI path never made a struct-returning COM call.
     */
    public static boolean renderTargetStructReturnEchoesItsBuffer(long target) {
        return DWNative.renderTargetStructReturnEchoesItsBuffer(target);
    }

    /** {@code ID2D1RenderTarget::GetPixelSize}, as {width, height}. FFM only. */
    public static int[] renderTargetPixelSize(long target) {
        return DWNative.renderTargetPixelSize(target);
    }

    /** {@code ID2D1RenderTarget::GetPixelFormat}, as {DXGI_FORMAT, D2D1_ALPHA_MODE}. FFM only. */
    public static int[] renderTargetPixelFormat(long target) {
        return DWNative.renderTargetPixelFormat(target);
    }

    /** {@code ID2D1RenderTarget::GetMaximumBitmapSize}. FFM only. */
    public static int renderTargetMaximumBitmapSize(long target) {
        return DWNative.renderTargetMaximumBitmapSize(target);
    }

    /** {@code IWICBitmapSource::GetSize} through an {@code IWICBitmap} pointer. FFM only. */
    public static int[] wicBitmapSize(long bitmap) {
        return DWNative.wicBitmapSize(bitmap);
    }

    /** {@code IWICBitmapSource::GetPixelFormat}, as the raw 16 GUID bytes. FFM only. */
    public static byte[] wicBitmapPixelFormat(long bitmap) {
        return DWNative.wicBitmapPixelFormat(bitmap);
    }

    /** {@code IWICBitmapLock::GetSize}, as {width, height}. FFM only. */
    public static int[] wicBitmapLockSize(long bitmapLock) {
        return DWNative.wicBitmapLockSize(bitmapLock);
    }

    private static final Api FFM = new Api() {

        @Override
        public String name() {
            return "DWNative";
        }

        @Override
        public long createFactory(int factoryType) {
            return DWNative.dwriteCreateFactory(factoryType);
        }

        @Override
        public int addRef(long self) {
            return DWNative.addRef(self);
        }

        @Override
        public int release(long self) {
            return DWNative.release(self);
        }

        @Override
        public long getSystemFontCollection(long self, boolean checkForUpdates) {
            return DWNative.getSystemFontCollection(self, checkForUpdates);
        }

        @Override
        public long createFontFileReference(long self, char[] filePath) {
            return DWNative.createFontFileReference(self, filePath);
        }

        @Override
        public long createFontFace(long self, int fontFaceType, long fontFile, int faceIndex, int simulations) {
            return DWNative.createFontFace(self, fontFaceType, fontFile, faceIndex, simulations);
        }

        @Override
        public int analyze(long self, boolean[] isSupportedFontType, int[] fontFileType, int[] fontFaceType,
                           int[] numberOfFaces) {
            return DWNative.analyze(self, isSupportedFontType, fontFileType, fontFaceType, numberOfFaces);
        }

        @Override
        public int getFontFamilyCount(long self) {
            return DWNative.getFontFamilyCount(self);
        }

        @Override
        public long getFontFamily(long self, int index) {
            return DWNative.getFontFamily(self, index);
        }

        @Override
        public int findFamilyName(long self, char[] familyName) {
            return DWNative.findFamilyName(self, familyName);
        }

        @Override
        public long getFontFromFontFace(long self, long fontFace) {
            return DWNative.getFontFromFontFace(self, fontFace);
        }

        @Override
        public int getFontCount(long self) {
            return DWNative.getFontCount(self);
        }

        @Override
        public long getFont(long self, int index) {
            return DWNative.getFont(self, index);
        }

        @Override
        public long getFamilyNames(long self) {
            return DWNative.getFamilyNames(self);
        }

        @Override
        public long getFirstMatchingFont(long self, int weight, int stretch, int style) {
            return DWNative.getFirstMatchingFont(self, weight, stretch, style);
        }

        @Override
        public long createFontFace(long self) {
            return DWNative.createFontFace(self);
        }

        @Override
        public long getFaceNames(long self) {
            return DWNative.getFaceNames(self);
        }

        @Override
        public long getFontFamily(long self) {
            return DWNative.getFontFamily(self);
        }

        @Override
        public int getStretch(long self) {
            return DWNative.getStretch(self);
        }

        @Override
        public int getStyle(long self) {
            return DWNative.getStyle(self);
        }

        @Override
        public int getWeight(long self) {
            return DWNative.getWeight(self);
        }

        @Override
        public long getInformationalStrings(long self, int informationalStringID) {
            return DWNative.getInformationalStrings(self, informationalStringID);
        }

        @Override
        public int getSimulations(long self) {
            return DWNative.getSimulations(self);
        }

        @Override
        public char[] getString(long self, int index, int size) {
            return DWNative.getString(self, index, size);
        }

        @Override
        public int getStringLength(long self, int index) {
            return DWNative.getStringLength(self, index);
        }

        @Override
        public int findLocaleName(long self, char[] locale) {
            return DWNative.findLocaleName(self, locale);
        }

        @Override
        public int[] designGlyphMetrics(long fontFace, short glyphIndex, boolean isSideways) {
            return fromGlyphMetrics(DWNative.getDesignGlyphMetrics(fontFace, glyphIndex, isSideways));
        }

        @Override
        public Path2D glyphRunOutline(long fontFace, float emSize, short glyphIndex, boolean isSideways) {
            return DWNative.getGlyphRunOutline(fontFace, emSize, glyphIndex, isSideways);
        }

        @Override
        public long createGlyphRunAnalysis(long factory, GlyphRun run, float pixelsPerDip,
                                           float[] transform, int renderingMode, int measuringMode,
                                           float baselineOriginX, float baselineOriginY) {
            return DWNative.createGlyphRunAnalysis(factory, toGlyphRun(run), pixelsPerDip,
                    toMatrix(transform), renderingMode, measuringMode, baselineOriginX, baselineOriginY);
        }

        @Override
        public int[] alphaTextureBounds(long analysis, int textureType) {
            return fromRect(DWNative.getAlphaTextureBounds(analysis, textureType));
        }

        @Override
        public byte[] alphaTexture(long analysis, int textureType, int[] bounds) {
            return DWNative.createAlphaTexture(analysis, textureType, toRect(bounds));
        }

        @Override
        public long createTextAnalyzer(long factory) {
            return DWNative.createTextAnalyzer(factory);
        }

        @Override
        public long createTextFormat(long factory, char[] fontFamily, long fontCollection,
                                     int fontWeight, int fontStyle, int fontStretch, float fontSize,
                                     char[] localeName) {
            return DWNative.createTextFormat(factory, fontFamily, fontCollection, fontWeight,
                    fontStyle, fontStretch, fontSize, localeName);
        }

        @Override
        public long createTextLayout(long factory, char[] text, int start, int count, long textFormat,
                                     float maxWidth, float maxHeight) {
            return DWNative.createTextLayout(factory, text, start, count, textFormat, maxWidth,
                    maxHeight);
        }

        @Override
        public int analyzeScript(long analyzer, long source, int textPosition, int textLength,
                                 long sink) {
            return DWNative.analyzeScript(analyzer, source, textPosition, textLength, sink);
        }

        @Override
        public int getGlyphs(long analyzer, char[] textString, int textStart, int textLength,
                             long fontFace, boolean isSideways, boolean isRightToLeft,
                             int[] scriptAnalysis, char[] localeName, long numberSubstitution,
                             long[] features, int[] featureRangeLengths, int featureRanges,
                             int maxGlyphCount, short[] clusterMap, short[] textProps,
                             short[] glyphIndices, short[] glyphProps, int[] actualGlyphCount) {
            return DWNative.getGlyphs(analyzer, textString, textStart, textLength, fontFace,
                    isSideways, isRightToLeft, toScriptAnalysis(scriptAnalysis), localeName,
                    numberSubstitution, features, featureRangeLengths, featureRanges, maxGlyphCount,
                    clusterMap, textProps, glyphIndices, glyphProps, actualGlyphCount);
        }

        @Override
        public int getGlyphPlacements(long analyzer, char[] textString, short[] clusterMap,
                                      short[] textProps, int textStart, int textLength,
                                      short[] glyphIndices, short[] glyphProps, int glyphCount,
                                      long fontFace, float fontEmSize, boolean isSideways,
                                      boolean isRightToLeft, int[] scriptAnalysis, char[] localeName,
                                      long[] features, int[] featureRangeLengths, int featureRanges,
                                      float[] glyphAdvances, float[] glyphOffsets) {
            return DWNative.getGlyphPlacements(analyzer, textString, clusterMap, textProps, textStart,
                    textLength, glyphIndices, glyphProps, glyphCount, fontFace, fontEmSize,
                    isSideways, isRightToLeft, toScriptAnalysis(scriptAnalysis), localeName, features,
                    featureRangeLengths, featureRanges, glyphAdvances, glyphOffsets);
        }

        @Override
        public int draw(long layout, long clientDrawingContext, long renderer, float originX,
                        float originY) {
            return DWNative.draw(layout, clientDrawingContext, renderer, originX, originY);
        }

        @Override
        public long newAnalysisSink(char[] text, int start, int length, String locale, int direction) {
            return DWNative.newAnalysisSink(text, start, length, terminated(locale), direction, 0L);
        }

        @Override
        public long analysisSinkPointer(long handle) {
            return DWNative.analysisSinkPointer(handle);
        }

        @Override
        public long analysisSourcePointer(long handle) {
            return DWNative.analysisSourcePointer(handle);
        }

        @Override
        public boolean analysisSinkNext(long handle) {
            return DWNative.analysisSinkNext(handle);
        }

        @Override
        public int analysisSinkGetStart(long handle) {
            return DWNative.analysisSinkGetStart(handle);
        }

        @Override
        public int analysisSinkGetLength(long handle) {
            return DWNative.analysisSinkGetLength(handle);
        }

        @Override
        public int[] analysisSinkGetAnalysis(long handle) {
            return fromScriptAnalysis(DWNative.analysisSinkGetAnalysis(handle));
        }

        @Override
        public int analysisSinkAddRef(long handle) {
            return DWNative.analysisSinkAddRef(handle);
        }

        @Override
        public int analysisSinkRelease(long handle) {
            return DWNative.analysisSinkRelease(handle);
        }

        @Override
        public void analysisSinkDispose(long handle) {
            DWNative.disposeAnalysisSink(handle);
        }

        @Override
        public long newTextRenderer() {
            return DWNative.newTextRenderer();
        }

        @Override
        public long textRendererPointer(long handle) {
            return DWNative.textRendererPointer(handle);
        }

        @Override
        public boolean textRendererNext(long handle) {
            return DWNative.textRendererNext(handle);
        }

        @Override
        public int textRendererGetStart(long handle) {
            return DWNative.textRendererGetStart(handle);
        }

        @Override
        public int textRendererGetLength(long handle) {
            return DWNative.textRendererGetLength(handle);
        }

        @Override
        public int textRendererGetGlyphCount(long handle) {
            return DWNative.textRendererGetGlyphCount(handle);
        }

        @Override
        public int textRendererGetTotalGlyphCount(long handle) {
            return DWNative.textRendererGetTotalGlyphCount(handle);
        }

        @Override
        public long textRendererGetFontFace(long handle) {
            return DWNative.textRendererGetFontFace(handle);
        }

        @Override
        public int textRendererGetGlyphIndices(long handle, int[] glyphs, int start, int slot) {
            return DWNative.textRendererGetGlyphIndices(handle, glyphs, start, slot);
        }

        @Override
        public int textRendererGetGlyphAdvances(long handle, float[] advances, int start) {
            return DWNative.textRendererGetGlyphAdvances(handle, advances, start);
        }

        @Override
        public int textRendererGetGlyphOffsets(long handle, float[] offsets, int start) {
            return DWNative.textRendererGetGlyphOffsets(handle, offsets, start);
        }

        @Override
        public int textRendererGetClusterMap(long handle, short[] clusterMap, int textStart,
                                             int glyphStart) {
            return DWNative.textRendererGetClusterMap(handle, clusterMap, textStart, glyphStart);
        }

        @Override
        public int textRendererAddRef(long handle) {
            return DWNative.textRendererAddRef(handle);
        }

        @Override
        public int textRendererRelease(long handle) {
            return DWNative.textRendererRelease(handle);
        }

        @Override
        public void textRendererDispose(long handle) {
            DWNative.disposeTextRenderer(handle);
        }

        @Override
        public boolean coInitializeEx(int dwCoInit) {
            return DWNative.coInitializeEx(dwCoInit);
        }

        @Override
        public void coUninitialize() {
            DWNative.coUninitialize();
        }

        @Override
        public long wicCreateImagingFactory() {
            return DWNative.wicCreateImagingFactory();
        }

        @Override
        public long d2d1CreateFactory(int factoryType) {
            return DWNative.d2d1CreateFactory(factoryType);
        }

        @Override
        public long createBitmap(long factory, int uiWidth, int uiHeight, int pixelFormat, int options) {
            return DWNative.createBitmap(factory, uiWidth, uiHeight, pixelFormat, options);
        }

        @Override
        public long lock(long bitmap, int x, int y, int width, int height, int flags) {
            return DWNative.lock(bitmap, x, y, width, height, flags);
        }

        @Override
        public byte[] getDataPointer(long bitmapLock) {
            return DWNative.getDataPointer(bitmapLock);
        }

        @Override
        public int getStride(long bitmapLock) {
            return DWNative.getStride(bitmapLock);
        }

        @Override
        public long createWicBitmapRenderTarget(long factory, long bitmap,
                                                RenderTargetProperties properties) {
            return DWNative.createWicBitmapRenderTarget(factory, bitmap,
                    toRenderTargetProperties(properties));
        }

        @Override
        public void beginDraw(long target) {
            DWNative.beginDraw(target);
        }

        @Override
        public int endDraw(long target) {
            return DWNative.endDraw(target);
        }

        @Override
        public void clear(long target, float[] color) {
            DWNative.clear(target, toColor(color));
        }

        @Override
        public void setTextAntialiasMode(long target, int textAntialiasMode) {
            DWNative.setTextAntialiasMode(target, textAntialiasMode);
        }

        @Override
        public void setTransform(long target, float[] transform) {
            DWNative.setTransform(target, toMatrix3x2(transform));
        }

        @Override
        public void drawGlyphRun(long target, float[] baselineOrigin, GlyphRun run, long brush,
                                 int measuringMode) {
            DWNative.drawGlyphRun(target, toPoint(baselineOrigin), toGlyphRun(run), brush,
                    measuringMode);
        }

        @Override
        public long createSolidColorBrush(long target, float[] color) {
            return DWNative.createSolidColorBrush(target, toColor(color));
        }
    };
}
