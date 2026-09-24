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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.fontFace;
import static test.com.sun.javafx.font.directwrite.DWFaceFixture.requireDirectWrite;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.BITMAP_HEIGHT;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.BITMAP_WIDTH;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.BLACK;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.IDENTITY;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.WHITE;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.createBitmap;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.createRenderTarget;
import static test.com.sun.javafx.font.directwrite.DWRenderFixture.inkBounds;

/**
 * The Direct2D + WIC binding checked against everything except the other implementation: the vtable
 * slots against the SDK headers, the struct layouts against the SDK declarations, the GUIDs against
 * their header values byte for byte, the COM apartment against its documented behaviour, and the two
 * argument shapes that no descriptor test can pin - the {@code D2D1_POINT_2F} that
 * {@code DrawGlyphRun} takes <b>by value</b> and the three 8-byte aggregates
 * {@code ID2D1RenderTarget} <b>returns by value</b>, which turned out not to travel in a register
 * at all - against measurements with an independent oracle.
 * <p>
 * The other half was a parity test that ran the same calls through the JNI natives and compared the
 * resulting greyscale mask bytes exactly - for every glyph of four families at two sizes, at every
 * sub-pixel offset, under every antialias mode and every transform. It was deleted with the flip,
 * because from that point both sides of the comparison were this code; the masks themselves stay
 * unpinned by any golden, which is why the shapes above are measured rather than assumed.
 * <p>
 * Line numbers into {@code directwrite.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/directwrite.cpp}); line numbers into
 * {@code d2d1.h} and {@code wincodec.h} refer to the Windows SDK 10.0.26100.0 ({@code Include/10.0.26100.0/um}).
 */
@EnabledOnOs(OS.WINDOWS)
public class DWRenderTest {

    private static final Api FFM = DWNativeShim.ffm();

    private static DWRenderFixture.Ctx ctx;
    private static long arial;

    @BeforeAll
    static void openFactories() {
        requireDirectWrite();
        DWRenderFixture.comInitialize(FFM);
        ctx = DWRenderFixture.open(FFM);
        long collection = FFM.getSystemFontCollection(ctx.dwriteFactory(), false);
        assertNotEquals(0L, collection);
        try {
            arial = fontFace(FFM, collection, "Arial");
        } finally {
            FFM.release(collection);
        }
    }

    @AfterAll
    static void closeFactories() {
        if (arial != 0) {
            FFM.release(arial);
            arial = 0;
        }
        DWRenderFixture.close(ctx);
        ctx = null;
        DWRenderFixture.comUninitialize(FFM);
    }

    /* ---------------------------------------------------------------------------------------------
     * The table, as data
     * ------------------------------------------------------------------------------------------- */

    /**
     * Every slot the mask path reaches, against the value read out of the Windows SDK headers. A slot
     * table is the one part of a COM binding that fails silently: the wrong index calls a different
     * method with the same argument count and produces plausible rubbish, so it is pinned as a literal
     * table here and behaviourally by the tests below.
     */
    @Test
    void theSlotTableIsWhatTheSdkHeadersSay() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("IWICImagingFactory::CreateBitmap", 17);           // ord 14, wincodec.h:6988
        expected.put("IWICBitmapSource::GetSize", 3);                   // ord 0,  wincodec.h:1361
        expected.put("IWICBitmapSource::GetPixelFormat", 4);            // ord 1,  wincodec.h:1365
        expected.put("IWICBitmap::Lock", 8);                            // ord 0,  wincodec.h:2334
        expected.put("IWICBitmapLock::GetSize", 3);                     // ord 0,  wincodec.h:2213
        expected.put("IWICBitmapLock::GetStride", 4);                   // ord 1,  wincodec.h:2217
        expected.put("IWICBitmapLock::GetDataPointer", 5);              // ord 2,  wincodec.h:2220
        expected.put("ID2D1Factory::CreateWicBitmapRenderTarget", 13);  // ord 10, d2d1.h:3423
        expected.put("ID2D1RenderTarget::CreateSolidColorBrush", 8);    // ord 4,  d2d1.h:2439
        expected.put("ID2D1RenderTarget::DrawGlyphRun", 29);            // ord 25, d2d1.h:2643
        expected.put("ID2D1RenderTarget::SetTransform", 30);            // ord 26, d2d1.h:2650
        expected.put("ID2D1RenderTarget::SetTextAntialiasMode", 34);    // ord 30, d2d1.h:2665
        expected.put("ID2D1RenderTarget::Clear", 47);                   // ord 43, d2d1.h:2754
        expected.put("ID2D1RenderTarget::BeginDraw", 48);               // ord 44, d2d1.h:2762
        expected.put("ID2D1RenderTarget::EndDraw", 49);                 // ord 45, d2d1.h:2769
        expected.put("ID2D1RenderTarget::GetPixelFormat", 50);          // ord 46, d2d1.h:2774
        expected.put("ID2D1RenderTarget::GetSize", 53);                 // ord 49, d2d1.h:2799
        expected.put("ID2D1RenderTarget::GetPixelSize", 54);            // ord 50, d2d1.h:2805
        expected.put("ID2D1RenderTarget::GetMaximumBitmapSize", 55);    // ord 51, d2d1.h:2812
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            assertEquals(entry.getValue().intValue(), DWNativeShim.renderSlot(entry.getKey()),
                    entry.getKey());
        }
    }

    /**
     * The base counts the slots above are built from, stated as the differences that produce them:
     * {@code ID2D1Resource} contributes one method, so {@code ID2D1RenderTarget} starts at 4 rather
     * than 3, and {@code IWICBitmapSource} contributes five, so {@code IWICBitmap::Lock} is 8.
     */
    @Test
    void theInheritedBasesAreOneAndFiveMethodsDeep() {
        // ID2D1Resource declares exactly one method, GetFactory (d2d1.h:1088), so an
        // ID2D1RenderTarget ordinal n is slot n + 4. Getting this wrong by one is the single most
        // likely defect in a COM binding and the hardest to see, so it is stated three times over a
        // 45-slot span rather than once.
        assertEquals(4 + 4, DWNativeShim.renderSlot("ID2D1RenderTarget::CreateSolidColorBrush"));
        assertEquals(4 + 25, DWNativeShim.renderSlot("ID2D1RenderTarget::DrawGlyphRun"));
        assertEquals(4 + 49, DWNativeShim.renderSlot("ID2D1RenderTarget::GetSize"));
        // IWICBitmapSource declares five methods (wincodec.h:1357), so an IWICBitmap ordinal n is
        // slot n + 8, and its own first method is Lock.
        assertEquals(8 + 0, DWNativeShim.renderSlot("IWICBitmap::Lock"));
        assertEquals(3, DWNativeShim.renderSlot("IWICBitmapSource::GetSize"));
        assertEquals(4, DWNativeShim.renderSlot("IWICBitmapSource::GetPixelFormat"));
        // IWICBitmapLock derives straight from IUnknown, so its three are consecutive from 3.
        assertEquals(1, DWNativeShim.renderSlot("IWICBitmapLock::GetStride")
                - DWNativeShim.renderSlot("IWICBitmapLock::GetSize"));
        assertEquals(1, DWNativeShim.renderSlot("IWICBitmapLock::GetDataPointer")
                - DWNativeShim.renderSlot("IWICBitmapLock::GetStride"));
    }

    /** Every layout the mask path added, against the SDK declaration it mirrors. */
    @Test
    void theStructLayoutsMatchTheSdkDeclarations() {
        assertEquals(16, DWNativeShim.renderLayoutByteSize("WICRect"));
        assertEquals(0, DWNativeShim.renderLayoutOffset("WICRect", "X"));
        assertEquals(4, DWNativeShim.renderLayoutOffset("WICRect", "Y"));
        assertEquals(8, DWNativeShim.renderLayoutOffset("WICRect", "Width"));
        assertEquals(12, DWNativeShim.renderLayoutOffset("WICRect", "Height"));

        assertEquals(16, DWNativeShim.renderLayoutByteSize("D2D1_COLOR_F"));
        assertEquals(0, DWNativeShim.renderLayoutOffset("D2D1_COLOR_F", "r"));
        assertEquals(12, DWNativeShim.renderLayoutOffset("D2D1_COLOR_F", "a"));

        assertEquals(24, DWNativeShim.renderLayoutByteSize("D2D1_MATRIX_3X2_F"));
        assertEquals(16, DWNativeShim.renderLayoutOffset("D2D1_MATRIX_3X2_F", "dx"));
        assertEquals(20, DWNativeShim.renderLayoutOffset("D2D1_MATRIX_3X2_F", "dy"));

        assertEquals(8, DWNativeShim.renderLayoutByteSize("D2D1_PIXEL_FORMAT"));
        assertEquals(4, DWNativeShim.renderLayoutOffset("D2D1_PIXEL_FORMAT", "alphaMode"));

        assertEquals(28, DWNativeShim.renderLayoutByteSize("D2D1_RENDER_TARGET_PROPERTIES"));
        assertEquals(0, DWNativeShim.renderLayoutOffset("D2D1_RENDER_TARGET_PROPERTIES", "type"));
        assertEquals(4, DWNativeShim.renderLayoutOffset("D2D1_RENDER_TARGET_PROPERTIES", "pixelFormat"));
        assertEquals(12, DWNativeShim.renderLayoutOffset("D2D1_RENDER_TARGET_PROPERTIES", "dpiX"));
        assertEquals(16, DWNativeShim.renderLayoutOffset("D2D1_RENDER_TARGET_PROPERTIES", "dpiY"));
        assertEquals(20, DWNativeShim.renderLayoutOffset("D2D1_RENDER_TARGET_PROPERTIES", "usage"));
        assertEquals(24, DWNativeShim.renderLayoutOffset("D2D1_RENDER_TARGET_PROPERTIES", "minLevel"));

        assertEquals(4, DWNativeShim.renderLayoutByteSize("D2D1_FACTORY_OPTIONS"));
        assertEquals(8, DWNativeShim.renderLayoutByteSize("D2D1_SIZE_F"));
        assertEquals(8, DWNativeShim.renderLayoutByteSize("D2D1_SIZE_U"));
        assertEquals(4, DWNativeShim.renderLayoutOffset("D2D1_SIZE_F", "height"));
        assertEquals(4, DWNativeShim.renderLayoutOffset("D2D1_SIZE_U", "height"));
    }

    /* ---------------------------------------------------------------------------------------------
     * The GUIDs
     * ------------------------------------------------------------------------------------------- */

    /**
     * The three class and interface IDs, as the exact 16 bytes {@code guiddef.h} lays out - Data1,
     * Data2 and Data3 little-endian, Data4 in writing order. These bytes were link-time data in the
     * JNI build (from {@code windowscodecs.lib} and {@code uuid.lib}), which is why they never appear
     * in its import table; Java has to carry them itself, so they are pinned here.
     */
    @Test
    void theClassAndInterfaceIdsAreTheHeaderValuesByteForByte() {
        assertArrayEquals(new byte[] { 0x62, (byte) 0xf2, (byte) 0xca, (byte) 0xca, 0x70, (byte) 0x93,
                0x15, 0x46, (byte) 0xa1, 0x3b, (byte) 0x9f, 0x55, 0x39, (byte) 0xda, 0x4c, 0x0a },
                DWNativeShim.guidBytes(DWNativeShim.clsidWicImagingFactory()),
                "CLSID_WICImagingFactory, wincodec.h:385");
        assertArrayEquals(new byte[] { (byte) 0xa9, (byte) 0xc8, 0x5e, (byte) 0xec, (byte) 0x95,
                (byte) 0xc3, 0x14, 0x43, (byte) 0x9c, 0x77, 0x54, (byte) 0xd7, (byte) 0xa9, 0x35,
                (byte) 0xff, 0x70 },
                DWNativeShim.guidBytes(DWNativeShim.iidWicImagingFactory()),
                "IID_IWICImagingFactory, wincodec.h:6927");
        assertArrayEquals(new byte[] { 0x47, 0x22, 0x15, 0x06, 0x50, 0x6f, 0x5a, 0x46, (byte) 0x92,
                0x45, 0x11, (byte) 0x8b, (byte) 0xfd, 0x3b, 0x60, 0x07 },
                DWNativeShim.guidBytes(DWNativeShim.iidD2D1Factory()),
                "IID_ID2D1Factory, d2d1.h:3341");
    }

    /**
     * The WIC CLSID is <b>factory1</b>, not factory2. wincodec.h:388 redefines
     * {@code CLSID_WICImagingFactory} to factory2 under
     * {@code #if(_WIN32_WINNT >= _WIN32_WINNT_WIN8) || defined(_WIN7_PLATFORM_UPDATE)};
     * {@code win.cmake:257} builds the font target with {@code /D_WIN32_WINNT=0x0601} and nothing
     * defines {@code _WIN7_PLATFORM_UPDATE}, so the shipped {@code javafx_font.dll} carries factory1
     * and only factory1 - which a byte search of the DLL confirms. Choosing factory2 would be a
     * behaviour change of unknown blast radius, so the two values are asserted to be different and
     * the one in use is asserted to be factory1.
     */
    @Test
    void theWicClsidIsFactoryOneAndNotFactoryTwo() {
        assertEquals("cacaf262-9370-4615-a13b-9f5539da4c0a", DWNativeShim.clsidWicImagingFactory());
        assertEquals("317d06e8-5f24-433d-bdf7-79ce68d8abc2", DWNativeShim.clsidWicImagingFactory2());
        assertFalse(Arrays.equals(
                DWNativeShim.guidBytes(DWNativeShim.clsidWicImagingFactory()),
                DWNativeShim.guidBytes(DWNativeShim.clsidWicImagingFactory2())));
    }

    /**
     * The eleven-way switch of {@code directwrite.cpp:2371-2384}, as a table: every
     * {@code OS.GUID_WICPixelFormat*} constant maps to the {@code wincodec.h} GUID of the same name,
     * and nothing else maps at all. Only {@code 32bppPBGRA} is reachable from JavaFX today; the rest
     * are kept so that the mapping stays complete when the C is deleted.
     */
    @Test
    void theWicPixelFormatTableIsTheElevenGuidsTheSwitchNames() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("GUID_WICPixelFormat8bppGray", "6fddc324-4e03-4bfe-b185-3d77768dc908");
        expected.put("GUID_WICPixelFormat8bppAlpha", "e6cd0116-eeba-4161-aa85-27dd9fb3a895");
        expected.put("GUID_WICPixelFormat16bppGray", "6fddc324-4e03-4bfe-b185-3d77768dc90b");
        expected.put("GUID_WICPixelFormat24bppRGB", "6fddc324-4e03-4bfe-b185-3d77768dc90d");
        expected.put("GUID_WICPixelFormat24bppBGR", "6fddc324-4e03-4bfe-b185-3d77768dc90c");
        expected.put("GUID_WICPixelFormat32bppBGR", "6fddc324-4e03-4bfe-b185-3d77768dc90e");
        expected.put("GUID_WICPixelFormat32bppBGRA", "6fddc324-4e03-4bfe-b185-3d77768dc90f");
        expected.put("GUID_WICPixelFormat32bppPBGRA", "6fddc324-4e03-4bfe-b185-3d77768dc910");
        expected.put("GUID_WICPixelFormat32bppGrayFloat", "6fddc324-4e03-4bfe-b185-3d77768dc911");
        expected.put("GUID_WICPixelFormat32bppRGBA", "f5c7ad2d-6a8d-43dd-a7a8-a29935261ae9");
        expected.put("GUID_WICPixelFormat32bppPRGBA", "3cc4a650-a527-4d37-a916-3142c7ebedba");
        assertEquals(expected.size(), DWNativeShim.wicPixelFormatCount());
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            int id = DWNativeShim.osConstant(entry.getKey());
            assertArrayEquals(DWNativeShim.guidBytes(entry.getValue()),
                    DWNativeShim.wicPixelFormatGuidBytes(id), entry.getKey());
        }
        assertNull(DWNativeShim.wicPixelFormatGuidBytes(0), "0 is not a listed pixel format");
        assertNull(DWNativeShim.wicPixelFormatGuidBytes(12), "12 is past the end of the switch");
        assertNull(DWNativeShim.wicPixelFormatGuidBytes(-1));
    }

    /* ---------------------------------------------------------------------------------------------
     * Linkage
     * ------------------------------------------------------------------------------------------- */

    /**
     * The four entry points the mask path binds by name. The vtable methods have no names to resolve -
     * they are found through the object - so these four plus {@code DWriteCreateFactory} are
     * the whole named surface of the DirectWrite port.
     */
    @Test
    void ole32AndD2D1ResolveTheirEntryPoints() {
        assertTrue(DWNativeShim.isOle32Available(),
                "ole32.dll must load and export CoInitializeEx, CoUninitialize and CoCreateInstance");
        assertTrue(DWNativeShim.isD2D1Available(), "d2d1.dll must load and export D2D1CreateFactory");
        Set<String> bound = new HashSet<>(DWNativeShim.boundSymbols());
        assertEquals(Set.of("dwrite.dll!DWriteCreateFactory", "ole32.dll!CoInitializeEx",
                "ole32.dll!CoUninitialize", "ole32.dll!CoCreateInstance",
                "d2d1.dll!D2D1CreateFactory"), bound,
                "the whole named surface of the DirectWrite port, and nothing else");
        assertEquals(bound.size(), DWNativeShim.boundSymbols().size(), "no symbol bound twice");
    }

    /* ---------------------------------------------------------------------------------------------
     * The COM apartment
     * ------------------------------------------------------------------------------------------- */

    /**
     * The three answers {@code CoInitializeEx} can give, on a thread that starts with no apartment:
     * {@code S_OK} the first time, {@code S_FALSE} the second (still
     * {@code true} - the C only ever reports {@code RPC_E_CHANGED_MODE} as failure), and
     * {@code RPC_E_CHANGED_MODE} for a different concurrency model - which does <b>not</b> take a
     * reference, so it must not be balanced by a {@code CoUninitialize}. After the two successful
     * initializations are balanced the apartment is gone and the multi-threaded model is accepted.
     * <p>
     * Every step happens on a fresh thread, because COM initialization is per thread and the surefire
     * main thread is shared with every other test in the module.
     */
    @Test
    void coInitializeExAnswersEveryApartmentOutcomeIncludingTheChangedMode() throws Exception {
        boolean[] ffm = onFreshThread(() -> apartmentSequence(FFM));
        assertArrayEquals(new boolean[] { true, true, false, true }, ffm, "DWNative");
    }

    /**
     * {@code S_OK}, then {@code S_FALSE}, then {@code RPC_E_CHANGED_MODE}, then - after both
     * references are dropped - {@code S_OK} again for the other model.
     */
    private static boolean[] apartmentSequence(Api api) {
        int apartment = DWNativeShim.osConstant("COINIT_APARTMENTTHREADED")
                | DWNativeShim.osConstant("COINIT_DISABLE_OLE1DDE");
        boolean first = api.coInitializeEx(apartment);
        boolean second = api.coInitializeEx(apartment);
        boolean changed = api.coInitializeEx(DWNativeShim.coinitMultiThreaded());
        api.coUninitialize();
        api.coUninitialize();
        boolean afterRelease = api.coInitializeEx(DWNativeShim.coinitMultiThreaded());
        api.coUninitialize();
        return new boolean[] { first, second, changed, afterRelease };
    }

    private static boolean[] onFreshThread(Supplier<boolean[]> body) throws Exception {
        AtomicReference<boolean[]> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                result.set(body.get());
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "dw-render-com");
        thread.start();
        thread.join(60_000);
        assertFalse(thread.isAlive(), "the COM probe thread must finish");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }

    /* ---------------------------------------------------------------------------------------------
     * The by-value argument: DrawGlyphRun takes its baseline origin as an 8-byte aggregate
     * ------------------------------------------------------------------------------------------- */

    /**
     * The one argument on the mask path that is <b>passed by value</b>. Under the Microsoft x64 ABI an
     * 8-byte aggregate travels in the integer register that argument occupies - RDX here, since
     * {@code this} takes RCX - so its two floats arrive packed in one register, not in XMM1 and XMM2
     * as a naive "two floats" descriptor would send them.
     * <p>
     * The measurement needs no knowledge of the glyph: the same glyph is drawn four times into a
     * cleared 256 x 256 bitmap at four baseline origins whose differences are whole pixels, and the
     * bounding box of its ink must move by exactly those differences - by 30 in x alone, by 40 in y
     * alone, and by both together. A point read from the wrong register would leave the ink where it
     * was or move it off the bitmap; a point with x and y transposed would fail the two single-axis
     * cases, which is why 30 and 40 are different numbers.
     */
    @Test
    void theBaselineOriginCrossesByValueAndMovesTheGlyphExactly() {
        short glyph = glyphOf(arial, 'H');
        long bitmap = createBitmap(FFM, ctx.wicFactory(), BITMAP_WIDTH, BITMAP_HEIGHT);
        long target = createRenderTarget(FFM, ctx.d2dFactory(), bitmap);
        try {
            int[] base = drawAndMeasure(bitmap, target, glyph, 60f, 100f);
            int[] movedInX = drawAndMeasure(bitmap, target, glyph, 90f, 100f);
            int[] movedInY = drawAndMeasure(bitmap, target, glyph, 60f, 140f);
            int[] movedInBoth = drawAndMeasure(bitmap, target, glyph, 90f, 140f);
            assertNotNull(base, "the glyph must actually leave ink");
            assertArrayEquals(shifted(base, 30, 0), movedInX, "x must travel in the low half");
            assertArrayEquals(shifted(base, 0, 40), movedInY, "y must travel in the high half");
            assertArrayEquals(shifted(base, 30, 40), movedInBoth);
        } finally {
            FFM.release(target);
            FFM.release(bitmap);
        }
    }

    private static int[] shifted(int[] box, int dx, int dy) {
        return new int[] { box[0] + dx, box[1] + dy, box[2] + dx, box[3] + dy };
    }

    /** One glyph drawn at one baseline origin into a freshly cleared target; the ink box it leaves. */
    private static int[] drawAndMeasure(long bitmap, long target, short glyph, float originX,
                                        float originY) {
        FFM.beginDraw(target);
        FFM.setTransform(target, IDENTITY);
        FFM.clear(target, WHITE);
        long brush = FFM.createSolidColorBrush(target, BLACK);
        assertNotEquals(0L, brush, "CreateSolidColorBrush must answer a brush, not a stale register");
        FFM.setTextAntialiasMode(target, DWNativeShim.osConstant("D2D1_TEXT_ANTIALIAS_MODE_GRAYSCALE"));
        FFM.drawGlyphRun(target, new float[] { originX, originY }, GlyphRun.of(arial, 48f, glyph),
                brush, DWNativeShim.osConstant("DWRITE_MEASURING_MODE_NATURAL"));
        int hr = FFM.endDraw(target);
        assertEquals(DWNativeShim.osConstant("S_OK"), hr, "EndDraw");
        assertEquals(0, FFM.release(brush), "the brush had exactly one reference");
        long bitmapLock = FFM.lock(bitmap, 0, 0, BITMAP_WIDTH, BITMAP_HEIGHT,
                DWNativeShim.osConstant("WICBitmapLockRead"));
        assertNotEquals(0L, bitmapLock);
        try {
            byte[] buffer = FFM.getDataPointer(bitmapLock);
            int stride = FFM.getStride(bitmapLock);
            assertNotNull(buffer);
            assertEquals(BITMAP_WIDTH * 4, stride, "32bppPBGRA over the full bitmap width");
            return inkBounds(buffer, stride, BITMAP_WIDTH, BITMAP_HEIGHT);
        } finally {
            FFM.release(bitmapLock);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * The by-value returns: three 8-byte aggregates come back in RAX
     * ------------------------------------------------------------------------------------------- */

    /**
     * The only struct <b>returns</b> in the whole DirectWrite port, isolated. None of the sixteen
     * migrated natives needs one, so this was new ground and it was proved rather than assumed: a
     * render target is built over a deliberately non-square 200 x 120 bitmap and asked its size three
     * ways, each answer being something the test already knows.
     * <p>
     * <b>The convention is not the one the Microsoft x64 rule for free functions would suggest.</b>
     * An aggregate of 1, 2, 4 or 8 bytes is a register aggregate, and FFM classifies these three as
     * such ({@code x64/windows/TypeClass.isRegisterAggregate}), so
     * {@code FunctionDescriptor.of(D2D1_SIZE_F_LAYOUT, ADDRESS)} looks right and links cleanly - and
     * faults inside {@code d2d1.dll}. The register dump of that fault named the real convention: the
     * callee had already computed {@code 0x0000000100000057} - {@code DXGI_FORMAT_B8G8R8A8_UNORM}
     * with {@code D2D1_ALPHA_MODE_PREMULTIPLIED}, the right answer for the right object - and was
     * storing those 8 bytes through a pointer taken from RDX that nothing had written. A COM method
     * that declares an aggregate return takes a caller-allocated buffer as a hidden argument after
     * {@code this}, and hands the same pointer back in RAX. Both halves are checked here.
     */
    @Test
    void theRenderTargetReturnsItsSizeThroughAHiddenBufferAndNotARegister() {
        long bitmap = createBitmap(FFM, ctx.wicFactory(), 200, 120);
        long target = createRenderTarget(FFM, ctx.d2dFactory(), bitmap);
        try {
            assertArrayEquals(new int[] { 200, 120 }, DWNativeShim.renderTargetPixelSize(target),
                    "GetPixelSize fills a D2D1_SIZE_U of two UINT32");
            float[] size = DWNativeShim.renderTargetSize(target);
            assertEquals(200f, size[0], "GetSize fills a D2D1_SIZE_F of two floats, width first");
            assertEquals(120f, size[1]);
            assertArrayEquals(new int[] { DWNativeShim.osConstant("DXGI_FORMAT_B8G8R8A8_UNORM"),
                    DWNativeShim.osConstant("D2D1_ALPHA_MODE_PREMULTIPLIED") },
                    DWNativeShim.renderTargetPixelFormat(target),
                    "a 32bppPBGRA WIC bitmap gives D2D a premultiplied BGRA target");
            assertTrue(DWNativeShim.renderTargetStructReturnEchoesItsBuffer(target),
                    "the callee must hand the hidden buffer back in RAX, as the ABI says");
            assertTrue(DWNativeShim.renderTargetMaximumBitmapSize(target) >= 2048,
                    "GetMaximumBitmapSize is a plain UINT32 in EAX and every device supports 2048");
        } finally {
            FFM.release(target);
            FFM.release(bitmap);
        }
    }

    /**
     * The WIC objects asked what they were created and locked with, through slots that surround the
     * ones the mask path needs: {@code IWICBitmapSource::GetSize} and {@code GetPixelFormat} are slots 3
     * and 4 of an {@code IWICBitmap}, which is what puts {@code Lock} at 8, and
     * {@code IWICBitmapLock::GetSize} is slot 3, next to {@code GetStride} at 4 and
     * {@code GetDataPointer} at 5. Each answers something the other two cannot, so no permutation of
     * the three passes.
     */
    @Test
    void theBitmapAndItsLockReportWhatTheyWereCreatedAndLockedWith() {
        long bitmap = createBitmap(FFM, ctx.wicFactory(), 200, 120);
        try {
            assertArrayEquals(new int[] { 200, 120 }, DWNativeShim.wicBitmapSize(bitmap));
            assertArrayEquals(
                    DWNativeShim.guidBytes("6fddc324-4e03-4bfe-b185-3d77768dc910"),
                    DWNativeShim.wicBitmapPixelFormat(bitmap),
                    "the bitmap carries the GUID_WICPixelFormat32bppPBGRA it was created with");
            long bitmapLock = FFM.lock(bitmap, 10, 20, 50, 30,
                    DWNativeShim.osConstant("WICBitmapLockRead"));
            assertNotEquals(0L, bitmapLock);
            try {
                assertArrayEquals(new int[] { 50, 30 }, DWNativeShim.wicBitmapLockSize(bitmapLock),
                        "the lock covers the rectangle that was asked for, not the bitmap");
                assertEquals(200 * 4, FFM.getStride(bitmapLock),
                        "the stride is the whole bitmap row, which is why getD2DMask needs it");
                byte[] data = FFM.getDataPointer(bitmapLock);
                assertNotNull(data);
                assertTrue(data.length >= 200 * 4 * 29 + 50 * 4 && data.length <= 200 * 4 * 30,
                        () -> "GetDataPointer spans the locked rows at the bitmap stride, not the "
                                + "locked width; got " + data.length);
            } finally {
                FFM.release(bitmapLock);
            }
        } finally {
            FFM.release(bitmap);
        }
    }

    /**
     * {@code CreateBitmap} refuses a pixel-format constant the switch does not list, and does so
     * before any COM call - so a bad value is {@code 0}, not a crash and not a WIC error.
     */
    @Test
    void anUnknownPixelFormatIsRefusedBeforeAnyComCall() {
        for (int unknown : new int[] { 0, 12, 99, -1, Integer.MIN_VALUE }) {
            assertEquals(0L, FFM.createBitmap(ctx.wicFactory(), 8, 8, unknown,
                    DWNativeShim.osConstant("WICBitmapCacheOnDemand")), () -> "pixel format " + unknown);
        }
    }

    /**
     * {@code Clear} with no colour is still issued, with {@code NULL} - the C {@code if (arg1)} guard
     * left the pointer null and made the call anyway ({@code directwrite.cpp:2456-2457}). Direct2D
     * documents that as clearing to transparent black, which is visibly not the white the same target
     * gets from {@code Clear(WHITE)}, so the two are compared rather than merely survived.
     */
    @Test
    void clearWithoutAColourIsStillIssuedAndClearsToTransparentBlack() {
        long bitmap = createBitmap(FFM, ctx.wicFactory(), 16, 16);
        long target = createRenderTarget(FFM, ctx.d2dFactory(), bitmap);
        try {
            byte[] white = clearAndRead(bitmap, target, WHITE);
            byte[] nothing = clearAndRead(bitmap, target, null);
            assertEquals((byte) 0xFF, white[0]);
            assertEquals((byte) 0xFF, white[3]);
            assertEquals(0, nothing[0]);
            assertEquals(0, nothing[3], "transparent black, not white");
        } finally {
            FFM.release(target);
            FFM.release(bitmap);
        }
    }

    private static byte[] clearAndRead(long bitmap, long target, float[] color) {
        FFM.beginDraw(target);
        FFM.setTransform(target, IDENTITY);
        FFM.clear(target, color);
        assertEquals(DWNativeShim.osConstant("S_OK"), FFM.endDraw(target));
        long bitmapLock = FFM.lock(bitmap, 0, 0, 16, 16, DWNativeShim.osConstant("WICBitmapLockRead"));
        assertNotEquals(0L, bitmapLock);
        try {
            return FFM.getDataPointer(bitmapLock);
        } finally {
            FFM.release(bitmapLock);
        }
    }

    /**
     * The render-target properties really cross: a target asked for
     * {@code DXGI_FORMAT_B8G8R8A8_UNORM} with {@code D2D1_ALPHA_MODE_PREMULTIPLIED} reports exactly
     * that, while the all-defaults properties {@code DWGlyph} uses report the same thing by
     * inference. A 28-byte struct read at the wrong offsets would answer something else or fail to
     * create the target at all.
     */
    @Test
    void theRenderTargetPropertiesCrossFieldForField() {
        RenderTargetProperties explicitFormat = RenderTargetProperties.defaults();
        explicitFormat.pixelFormatFormat = DWNativeShim.osConstant("DXGI_FORMAT_B8G8R8A8_UNORM");
        explicitFormat.pixelFormatAlphaMode = DWNativeShim.osConstant("D2D1_ALPHA_MODE_PREMULTIPLIED");
        explicitFormat.dpiX = 192f;
        explicitFormat.dpiY = 192f;
        long bitmap = createBitmap(FFM, ctx.wicFactory(), 200, 120);
        long target = FFM.createWicBitmapRenderTarget(ctx.d2dFactory(), bitmap, explicitFormat);
        try {
            assertNotEquals(0L, target);
            assertArrayEquals(new int[] { 200, 120 }, DWNativeShim.renderTargetPixelSize(target));
            float[] size = DWNativeShim.renderTargetSize(target);
            assertEquals(100f, size[0], "at 192 dpi a 200-pixel target is 100 DIPs wide");
            assertEquals(60f, size[1], "and the dpiY field landed at offset 16, not 12");
        } finally {
            if (target != 0) {
                FFM.release(target);
            }
            FFM.release(bitmap);
        }
    }

    private static short glyphOf(long fontFace, int codePoint) {
        short[] glyphs = DWNativeShim.glyphIndices(fontFace, new int[] { codePoint });
        assertNotNull(glyphs);
        assertNotEquals((short) 0, glyphs[0], () -> "the font must map U+" + codePoint);
        return glyphs[0];
    }
}
