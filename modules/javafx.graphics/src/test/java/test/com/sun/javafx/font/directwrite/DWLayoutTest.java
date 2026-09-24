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

import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.directwrite.DWFactory;
import com.sun.javafx.font.directwrite.DWNativeShim;
import com.sun.javafx.font.directwrite.DWNativeShim.Api;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.text.PrismTextLayout;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.ARABIC;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.E_FAIL;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.FACTORY_TYPE_SHARED;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.FONT_STRETCH_NORMAL;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.FONT_STYLE_ITALIC;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.FONT_WEIGHT_BOLD;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.HEBREW;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.LATIN;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.LOCALE;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.MIXED;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.READING_DIRECTION_LEFT_TO_RIGHT;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.READING_DIRECTION_RIGHT_TO_LEFT;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.S_OK;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.drainTextRenderer;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.requireDirectWrite;
import static test.com.sun.javafx.font.directwrite.DWLayoutFixture.wide;

/**
 * The FFM half of the DirectWrite shaping bindings, checked against everything except the JNI path: the vtable
 * slot table derived from {@code dwrite.h}, the IID bytes, the four struct layouts, the two COM
 * objects Java synthesizes for DirectWrite to call back into, and the behaviour of
 * {@code CreateTextLayout} that no other test in the tree would notice going wrong.
 * <p>
 * The comparison against the {@code OS} natives was a separate class, deleted in the change set that
 * flipped the peers onto this facade; what is here never had a JNI counterpart anyway - the C
 * objects are opaque, {@code AnalyzeBidi} was never bound, and neither {@code IDWriteTextFormat} nor
 * {@code IDWriteTextLayout} ever had an accessor in {@code OS.java} - plus, since the flip, the two
 * peers themselves.
 * <p>
 * Line numbers into {@code directwrite.cpp}, bare {@code :N} forms included, refer to it at commit
 * {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/directwrite.cpp});
 * line numbers into {@code dwrite.h} and {@code dwrite_2.h} refer to the Windows SDK 10.0.26100.0.
 */
@EnabledOnOs(OS.WINDOWS)
public class DWLayoutTest {

    private static final Api FFM = DWNativeShim.ffm();

    private static long factory;
    private static long collection;
    private static long analyzer;

    @BeforeAll
    static void createFactory() {
        requireDirectWrite();
        factory = FFM.createFactory(FACTORY_TYPE_SHARED);
        assertNotEquals(0L, factory);
        collection = FFM.getSystemFontCollection(factory, false);
        assertNotEquals(0L, collection);
        analyzer = FFM.createTextAnalyzer(factory);
        assertNotEquals(0L, analyzer, "IDWriteFactory::CreateTextAnalyzer, slot 21");
    }

    @AfterAll
    static void releaseFactory() {
        if (analyzer != 0) {
            FFM.release(analyzer);
            analyzer = 0;
        }
        if (collection != 0) {
            FFM.release(collection);
            collection = 0;
        }
        if (factory != 0) {
            FFM.release(factory);
            factory = 0;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 1. The table a reader cannot check by eye
     * ------------------------------------------------------------------------------------------- */

    /**
     * Every slot the shaping bindings use, against the count derived mechanically (not by reading) from
     * {@code dwrite.h} 10.0.26100.0. The arithmetic that matters:
     * {@code IDWriteTextFormat} declares 25 methods, so it fills 3..27 and {@code IDWriteTextLayout}
     * starts at 28, which puts {@code Draw} - ordinal 30 - at 58.
     */
    @Test
    void theSlotTableMatchesTheHeader() {
        assertEquals(15, slot("IDWriteFactory::CreateTextFormat"), "dwrite.h:4938, ord 12");
        assertEquals(18, slot("IDWriteFactory::CreateTextLayout"), "dwrite.h:4985, ord 15");
        assertEquals(21, slot("IDWriteFactory::CreateTextAnalyzer"), "dwrite.h:5052, ord 18");

        assertEquals(3, slot("IDWriteTextAnalyzer::AnalyzeScript"), "dwrite.h:2752, ord 0");
        assertEquals(4, slot("IDWriteTextAnalyzer::AnalyzeBidi"), "dwrite.h:2779, ord 1");
        assertEquals(5, slot("IDWriteTextAnalyzer::AnalyzeNumberSubstitution"), "dwrite.h:2807, ord 2");
        assertEquals(6, slot("IDWriteTextAnalyzer::AnalyzeLineBreakpoints"), "dwrite.h:2842, ord 3");
        assertEquals(7, slot("IDWriteTextAnalyzer::GetGlyphs"), "dwrite.h:2895, ord 4");
        assertEquals(8, slot("IDWriteTextAnalyzer::GetGlyphPlacements"), "dwrite.h:2947, ord 5");

        assertEquals(20, slot("IDWriteTextFormat::GetFontFamilyNameLength"), "dwrite.h:2241, ord 17");
        assertEquals(21, slot("IDWriteTextFormat::GetFontFamilyName"), "dwrite.h:2251, ord 18");
        assertEquals(22, slot("IDWriteTextFormat::GetFontWeight"), "dwrite.h:2259, ord 19");
        assertEquals(23, slot("IDWriteTextFormat::GetFontStyle"), "dwrite.h:2264, ord 20");
        assertEquals(24, slot("IDWriteTextFormat::GetFontStretch"), "dwrite.h:2269, ord 21");
        assertEquals(25, slot("IDWriteTextFormat::GetFontSize"), "dwrite.h:2274, ord 22");
        assertEquals(26, slot("IDWriteTextFormat::GetLocaleNameLength"), "dwrite.h:2279, ord 23");
        assertEquals(27, slot("IDWriteTextFormat::GetLocaleName"), "dwrite.h:2289, ord 24");

        assertEquals(42, slot("IDWriteTextLayout::GetMaxWidth"), "dwrite.h:3967, base 28 + ord 14");
        assertEquals(43, slot("IDWriteTextLayout::GetMaxHeight"), "dwrite.h:3972, base 28 + ord 15");
        assertEquals(58, slot("IDWriteTextLayout::Draw"), "dwrite.h:4200, base 28 + ord 30");

        assertEquals(3, slot("IDWriteTextAnalysisSink::SetScriptAnalysis"), "dwrite.h:2668");
        assertEquals(4, slot("IDWriteTextAnalysisSink::SetLineBreakpoints"), "dwrite.h:2684");
        assertEquals(5, slot("IDWriteTextAnalysisSink::SetBidiLevel"), "dwrite.h:2704");
        assertEquals(6, slot("IDWriteTextAnalysisSink::SetNumberSubstitution"), "dwrite.h:2728");

        assertEquals(3, slot("IDWriteTextAnalysisSource::GetTextAtPosition"), "dwrite.h:2576");
        assertEquals(4, slot("IDWriteTextAnalysisSource::GetTextBeforePosition"), "dwrite.h:2601");
        assertEquals(5, slot("IDWriteTextAnalysisSource::GetParagraphReadingDirection"), "dwrite.h:2610");
        assertEquals(6, slot("IDWriteTextAnalysisSource::GetLocaleName"), "dwrite.h:2624");
        assertEquals(7, slot("IDWriteTextAnalysisSource::GetNumberSubstitution"), "dwrite.h:2644");

        assertEquals(3, slot("IDWritePixelSnapping::IsPixelSnappingDisabled"), "dwrite.h:3597");
        assertEquals(4, slot("IDWritePixelSnapping::GetCurrentTransform"), "dwrite.h:3611");
        assertEquals(5, slot("IDWritePixelSnapping::GetPixelsPerDip"), "dwrite.h:3626");
        assertEquals(6, slot("IDWriteTextRenderer::DrawGlyphRun"), "dwrite.h:3662, base 6 + ord 0");
        assertEquals(7, slot("IDWriteTextRenderer::DrawUnderline"), "dwrite.h:3699");
        assertEquals(8, slot("IDWriteTextRenderer::DrawStrikethrough"), "dwrite.h:3730");
        assertEquals(9, slot("IDWriteTextRenderer::DrawInlineObject"), "dwrite.h:3758");
    }

    private static int slot(String name) {
        return DWNativeShim.layoutSlot(name);
    }

    /** The four IIDs of the two inbound objects, in {@code guiddef.h} byte order. */
    @Test
    void theFourInboundIidsHaveTheRightBytes() {
        assertArrayEquals(bytes(0x44, 0xcd, 0x10, 0x58, 0xa0, 0x0c, 0x01, 0x47, 0xb3, 0xfa, 0xbe, 0xc5,
                        0x18, 0x2a, 0xe4, 0xf6),
                DWNativeShim.guidBytes(DWNativeShim.iidAnalysisSink()), "IDWriteTextAnalysisSink");
        assertArrayEquals(bytes(0x58, 0x1a, 0x8e, 0x68, 0x94, 0x50, 0xc8, 0x47, 0xad, 0xc8, 0xfb, 0xce,
                        0xa6, 0x0a, 0xe9, 0x2b),
                DWNativeShim.guidBytes(DWNativeShim.iidAnalysisSource()), "IDWriteTextAnalysisSource");
        assertArrayEquals(bytes(0x35, 0x81, 0x8a, 0xef, 0xc6, 0x5c, 0xfe, 0x45, 0x88, 0x25, 0xc5, 0xa0,
                        0x72, 0x4e, 0xb8, 0x19),
                DWNativeShim.guidBytes(DWNativeShim.iidTextRenderer()), "IDWriteTextRenderer");
        assertArrayEquals(bytes(0xda, 0xa2, 0xf3, 0xea, 0xf4, 0xec, 0x24, 0x4d, 0xb6, 0x44, 0xb3, 0x4f,
                        0x68, 0x42, 0x02, 0x4b),
                DWNativeShim.guidBytes(DWNativeShim.iidPixelSnapping()), "IDWritePixelSnapping");
    }

    private static byte[] bytes(int... values) {
        byte[] raw = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            raw[i] = (byte) values[i];
        }
        return raw;
    }

    /**
     * The four layouts the shaping bindings add. {@code DWRITE_SCRIPT_ANALYSIS} is the one with padding a
     * reader would miss: {@code UINT16} then a 4-byte enum makes it 8 bytes, not 6.
     */
    @Test
    void theNewStructLayoutsMatchTheHeader() {
        assertEquals(8, DWNativeShim.layoutByteSize("DWRITE_SCRIPT_ANALYSIS"), "dwrite.h:2351");
        assertEquals(0, DWNativeShim.layoutOffset("DWRITE_SCRIPT_ANALYSIS", "script"));
        assertEquals(4, DWNativeShim.layoutOffset("DWRITE_SCRIPT_ANALYSIS", "shapes"));

        assertEquals(40, DWNativeShim.layoutByteSize("DWRITE_GLYPH_RUN_DESCRIPTION"), "dwrite.h:3092");
        assertEquals(0, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN_DESCRIPTION", "localeName"));
        assertEquals(8, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN_DESCRIPTION", "string"));
        assertEquals(16, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN_DESCRIPTION", "stringLength"));
        assertEquals(24, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN_DESCRIPTION", "clusterMap"));
        assertEquals(32, DWNativeShim.layoutOffset("DWRITE_GLYPH_RUN_DESCRIPTION", "textPosition"));

        assertEquals(24, DWNativeShim.layoutByteSize("ANALYSIS_SINK_OBJECT"));
        assertEquals(0, DWNativeShim.layoutOffset("ANALYSIS_SINK_OBJECT", "vtblSink"));
        assertEquals(8, DWNativeShim.layoutOffset("ANALYSIS_SINK_OBJECT", "vtblSource"));
        assertEquals(16, DWNativeShim.layoutOffset("ANALYSIS_SINK_OBJECT", "id"));

        assertEquals(16, DWNativeShim.layoutByteSize("TEXT_RENDERER_OBJECT"));
        assertEquals(0, DWNativeShim.layoutOffset("TEXT_RENDERER_OBJECT", "vtbl"));
        assertEquals(8, DWNativeShim.layoutOffset("TEXT_RENDERER_OBJECT", "id"));
    }

    /* ---------------------------------------------------------------------------------------------
     * 2. The analysis sink and source, in isolation
     * ------------------------------------------------------------------------------------------- */

    /**
     * The multi-IID question, answered by driving the object's own vtables: both interfaces answer
     * {@code S_OK} for the sink IID, the source IID and {@code IUnknown}, and {@code E_FAIL} with a
     * NULL out-pointer for anything else. Every arm hands back the block pointer, which is what
     * {@code JFXTextAnalysisSink::QueryInterface} did - {@code *ppvObject = this} applies no base
     * adjustment ({@code directwrite.cpp:1186-1192}) - so this is faithful rather than correct, and
     * deliberately so in a migration.
     */
    @Test
    void theSynthesizedSinkAnswersBothIidsFromBothVtables() {
        long handle = FFM.newAnalysisSink(LATIN.toCharArray(), 0, LATIN.length(), LOCALE,
                READING_DIRECTION_LEFT_TO_RIGHT);
        assertNotEquals(0L, handle);
        try {
            long[] result = DWNativeShim.analysisSinkSelfTest(handle);
            assertNotNull(result);
            assertEquals(S_OK, result[0], "QueryInterface(IDWriteTextAnalysisSink) on the sink");
            assertEquals(1, result[1], "it must write the block");
            assertEquals(S_OK, result[2], "QueryInterface(IDWriteTextAnalysisSource) on the sink");
            assertEquals(1, result[3], "the C answered this IID with the unadjusted this");
            assertEquals(S_OK, result[4], "QueryInterface(IUnknown) on the sink");
            assertEquals(1, result[5]);
            assertEquals(E_FAIL, result[6], "an IID it does not implement is E_FAIL, not E_NOINTERFACE");
            assertEquals(1, result[7], "and the out-pointer is NULL");
            assertEquals(S_OK, result[8], "QueryInterface(source IID) on the source pointer");
            assertEquals(1, result[9]);
            assertEquals(S_OK, result[10], "QueryInterface(sink IID) on the source pointer");
            assertEquals(1, result[11]);
            assertEquals(1, result[12], "AddRef through the sink vtable");
            assertEquals(2, result[13], "AddRef through the source vtable shares the counter");
            assertEquals(1, result[14], "Release through the source vtable");
            assertEquals(0, result[15], "Release through the sink vtable");
        } finally {
            FFM.analysisSinkDispose(handle);
        }
    }

    /**
     * The two interface pointers of one object differ by exactly 8 bytes, which is the C++ layout
     * this reproduces: {@code class JFXTextAnalysisSink : public IDWriteTextAnalysisSink, public
     * IDWriteTextAnalysisSource} ({@code directwrite.cpp:936}) puts the sink vtable at offset 0 and
     * the source vtable at offset 8.
     */
    @Test
    void theSinkAndSourcePointersDifferByEight() {
        long handle = FFM.newAnalysisSink(LATIN.toCharArray(), 0, LATIN.length(), LOCALE,
                READING_DIRECTION_LEFT_TO_RIGHT);
        try {
            assertEquals(8L, FFM.analysisSourcePointer(handle) - FFM.analysisSinkPointer(handle));
        } finally {
            FFM.analysisSinkDispose(handle);
        }
    }

    /**
     * The peer, not the facade: {@code JFXTextAnalysisSink} now carries a registry id rather than a
     * COM pointer, and {@code IDWriteTextAnalyzer::AnalyzeScript} must be handed the <b>source</b>
     * interface of that object and not its class pointer. The probe re-enacts
     * {@code DWGlyphLayout.addTextRun} call for call and asks each of the two pointers for a
     * paragraph reading direction: only the source can answer one, and eight bytes earlier the same
     * call lands on the sink's {@code SetBidiLevel}, which returns {@code S_OK}. Handing the wrong
     * pointer to DirectWrite dispatches {@code GetTextAtPosition} into {@code SetScriptAnalysis},
     * which is an access violation inside dwrite.dll rather than a failed assertion.
     */
    @Test
    void thePeerHandsAnalyzeScriptTheSourceInterfaceAndNotTheSink() {
        long[] probe = DWNativeShim.analysisSinkPeerProbe(MIXED.toCharArray(), 0, MIXED.length(),
                LOCALE, READING_DIRECTION_RIGHT_TO_LEFT);
        assertEquals(7, probe.length, "the peer must have produced a sink and an analyzer");
        assertEquals(8L, probe[1] - probe[0], "the source subinterface starts 8 bytes in");
        assertEquals(READING_DIRECTION_RIGHT_TO_LEFT, (int) probe[2],
                "asked as a source, the object answers the direction it was created with");
        assertEquals(S_OK, (int) probe[3],
                "asked eight bytes earlier it is the sink, whose slot 5 is SetBidiLevel: S_OK");
        assertEquals(S_OK, (int) probe[4], "AnalyzeScript through the peer");
        assertTrue(probe[5] > 1, "the mixed-script text must split into several runs; got " + probe[5]);
        assertEquals(0L, probe[6], "the peer disposed its sink before returning");
    }

    /**
     * All nine collectors and queries driven through the two vtables with known arguments. This is
     * where the descriptors are proven: the two {@code UINT8}s of {@code SetBidiLevel}, and the fact
     * that {@code GetLocaleName} and {@code GetNumberSubstitution} write the <b>length first</b>
     * while {@code GetTextAtPosition} writes the pointer first (dwrite.h:2576 against :2624).
     */
    @Test
    void theSinkAndSourceAnswerEveryCallbackAsTheCDid() {
        int length = LATIN.length();
        long handle = DWNativeShim.newRecordingAnalysisSink(LATIN.toCharArray(), 0, length, LOCALE,
                READING_DIRECTION_RIGHT_TO_LEFT);
        assertNotEquals(0L, handle);
        try {
            long[] r = DWNativeShim.analysisSinkDriveCallbacks(handle, 3, 4, (short) 17, 1, 2, 1);
            assertNotNull(r);
            assertEquals(S_OK, r[0], "SetScriptAnalysis");
            assertEquals(S_OK, r[1], "SetLineBreakpoints");
            assertEquals(S_OK, r[2], "SetBidiLevel");
            assertEquals(S_OK, r[3], "SetNumberSubstitution");

            assertEquals(S_OK, r[4], "GetTextAtPosition(0)");
            assertEquals(0, r[5], "it answers the start of the text copy");
            assertEquals(length, r[6], "and the whole remaining length");
            assertEquals(S_OK, r[7], "GetTextAtPosition(textLength)");
            assertEquals(1, r[8], "at the end it answers NULL");
            assertEquals(0, r[9], "and zero");

            assertEquals(S_OK, r[10], "GetTextBeforePosition(0)");
            assertEquals(1, r[11], "position 0 has nothing before it");
            assertEquals(0, r[12]);
            assertEquals(S_OK, r[13], "GetTextBeforePosition(2)");
            assertEquals(1, r[14], "it answers the base of the text copy");
            assertEquals(2, r[15], "and the length of the prefix");
            assertEquals(S_OK, r[16], "GetTextBeforePosition(textLength + 1)");
            assertEquals(1, r[17], "past the end is NULL");
            assertEquals(0, r[18]);

            assertEquals(READING_DIRECTION_RIGHT_TO_LEFT, r[19],
                    "GetParagraphReadingDirection returns the constructor's value, not an HRESULT");

            assertEquals(S_OK, r[20], "GetLocaleName(1)");
            assertEquals(1, r[21], "the locale copy, written into the second out-parameter");
            assertEquals(length - 1, r[22], "the length, written into the first, with no bounds check");

            assertEquals(S_OK, r[23], "GetNumberSubstitution(1)");
            assertEquals(1, r[24], "JavaFX always passes NULL, so NULL comes back");
            assertEquals(length - 1, r[25]);

            List<String> callbacks = DWNativeShim.analysisSinkCallbacks(handle);
            assertEquals(List.of("SetScriptAnalysis 3 4 17 1", "SetLineBreakpoints 3 4",
                            "SetBidiLevel 3 4 2 1", "SetNumberSubstitution 3 4 0",
                            "GetTextAtPosition 0", "GetTextBeforePosition 2",
                            "GetParagraphReadingDirection", "GetLocaleName 1",
                            "GetNumberSubstitution 1"),
                    callbacks, "every callback, in the order it arrived");
        } finally {
            FFM.analysisSinkDispose(handle);
        }
    }

    /**
     * The run cursor: {@code Next} pre-increments from -1 and never resets, so a second pass yields
     * nothing ({@code directwrite.cpp:1151-1154}), and out of range {@code GetStart} and
     * {@code GetLength} are 0 while {@code GetAnalysis} is a zero-filled object rather than null
     * ({@code :1166-1169} through {@code newDWRITE_SCRIPT_ANALYSIS(env, NULL)}).
     */
    @Test
    void theSinkCursorNeverResetsAndAnswersZeroOutOfRange() {
        long handle = FFM.newAnalysisSink(LATIN.toCharArray(), 0, LATIN.length(), LOCALE,
                READING_DIRECTION_LEFT_TO_RIGHT);
        try {
            assertEquals(0, FFM.analysisSinkGetStart(handle), "before the first Next");
            assertEquals(0, FFM.analysisSinkGetLength(handle));
            assertArrayEquals(new int[] { 0, 0 }, FFM.analysisSinkGetAnalysis(handle),
                    "zero-filled, not null");

            DWNativeShim.analysisSinkDriveCallbacks(handle, 3, 4, (short) 17, 1, 0, 0);
            DWNativeShim.analysisSinkDriveCallbacks(handle, 9, 2, (short) 8, 0, 0, 0);

            assertTrue(FFM.analysisSinkNext(handle));
            assertEquals(3, FFM.analysisSinkGetStart(handle));
            assertEquals(4, FFM.analysisSinkGetLength(handle));
            assertArrayEquals(new int[] { 17, 1 }, FFM.analysisSinkGetAnalysis(handle));
            assertTrue(FFM.analysisSinkNext(handle));
            assertEquals(9, FFM.analysisSinkGetStart(handle));
            assertEquals(2, FFM.analysisSinkGetLength(handle));
            assertArrayEquals(new int[] { 8, 0 }, FFM.analysisSinkGetAnalysis(handle));

            assertTrue(!FFM.analysisSinkNext(handle), "exhausted");
            assertEquals(0, FFM.analysisSinkGetStart(handle));
            assertArrayEquals(new int[] { 0, 0 }, FFM.analysisSinkGetAnalysis(handle));
            assertTrue(!FFM.analysisSinkNext(handle), "and it does not wrap around");
        } finally {
            FFM.analysisSinkDispose(handle);
        }
    }

    /** {@code _NewJFXTextAnalysisSink} answered 0 for a null text or locale ({@code :1203}). */
    @Test
    void aNullTextOrLocaleMakesNoSink() {
        assertEquals(0L, FFM.newAnalysisSink(null, 0, 0, LOCALE, READING_DIRECTION_LEFT_TO_RIGHT));
        assertEquals(0L, FFM.newAnalysisSink(LATIN.toCharArray(), 0, 1, null,
                READING_DIRECTION_LEFT_TO_RIGHT));
        assertEquals(0, DWNativeShim.analysisSinkRegistrySize(), "and nothing is registered");
    }

    /* ---------------------------------------------------------------------------------------------
     * 3. The text renderer, in isolation
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code IDWriteTextRenderer} derives {@code IDWritePixelSnapping}, so one vtable answers three
     * IIDs with the same pointer - correct COM, not a reproduction of a defect this time.
     */
    @Test
    void theSynthesizedRendererAnswersBothIids() {
        long handle = FFM.newTextRenderer();
        assertNotEquals(0L, handle);
        try {
            long[] r = DWNativeShim.textRendererSelfTest(handle);
            assertNotNull(r);
            assertEquals(S_OK, r[0], "QueryInterface(IDWriteTextRenderer)");
            assertEquals(1, r[1]);
            assertEquals(S_OK, r[2], "QueryInterface(IDWritePixelSnapping)");
            assertEquals(1, r[3]);
            assertEquals(S_OK, r[4], "QueryInterface(IUnknown)");
            assertEquals(1, r[5]);
            assertEquals(E_FAIL, r[6], "anything else is E_FAIL");
            assertEquals(1, r[7], "with a NULL out-pointer");
            assertEquals(1, r[8]);
            assertEquals(2, r[9]);
            assertEquals(1, r[10]);
            assertEquals(0, r[11]);
        } finally {
            FFM.textRendererDispose(handle);
        }
    }

    /**
     * The three pixel-snapping answers are behaviour, not boilerplate: they tell DirectWrite how to
     * round glyph positions, and the golden's fallback runs depend on them
     * ({@code directwrite.cpp:1376-1399}).
     */
    @Test
    void thePixelSnappingQueriesAnswerTheCsValues() {
        long handle = FFM.newTextRenderer();
        try {
            long[] r = DWNativeShim.textRendererDriveQueries(handle);
            assertNotNull(r);
            assertEquals(S_OK, r[0]);
            assertEquals(0, r[1], "IsPixelSnappingDisabled writes FALSE");
            assertEquals(S_OK, r[2]);
            float[] matrix = new float[6];
            for (int i = 0; i < 6; i++) {
                matrix[i] = Float.intBitsToFloat((int) r[3 + i]);
            }
            assertArrayEquals(new float[] { 1f, 0f, 0f, 1f, 0f, 0f }, matrix,
                    "GetCurrentTransform writes the identity");
            assertEquals(S_OK, r[9]);
            assertEquals(1.0f, Float.intBitsToFloat((int) r[10]), "GetPixelsPerDip writes 1.0f");
        } finally {
            FFM.textRendererDispose(handle);
        }
    }

    /** {@code DrawUnderline}, {@code DrawStrikethrough} and {@code DrawInlineObject}: S_OK, no record. */
    @Test
    void theIgnoredRendererSlotsAnswerSOkAndCollectNothing() {
        long handle = DWNativeShim.newRecordingTextRenderer();
        try {
            long[] r = DWNativeShim.textRendererDriveIgnored(handle);
            assertNotNull(r);
            assertArrayEquals(new long[] { S_OK, S_OK, S_OK }, r);
            assertEquals(0, DWNativeShim.textRendererRunCount(handle), "nothing was collected");
            assertEquals(List.of("DrawDecoration 1.5 -2.5", "DrawDecoration 3.5 -4.5",
                            "DrawInlineObject 5.5 -6.5 1 0"),
                    DWNativeShim.textRendererCallbacks(handle));
        } finally {
            FFM.textRendererDispose(handle);
        }
    }

    /**
     * The four drains, against a synthetic glyph run: the clamp computed from the whole array
     * length, the {@code | slot} merge with the caller's already-shifted slot, the rejection of an
     * odd offset count <em>after</em> the clamp, and the two {@code short} truncations of the
     * cluster map ({@code directwrite.cpp:1515-1610}, quirks q22-q26).
     */
    @Test
    void theRendererDrainsClampAndMergeExactlyAsTheCDid() {
        long handle = FFM.newTextRenderer();
        try {
            short[] indices = { 10, 20, 30 };
            float[] advances = { 1.5f, 2.5f, 3.5f };
            float[] offsets = { 0.5f, -0.5f, 1.5f, -1.5f, 2.5f, -2.5f };
            short[] clusterMap = { 0, 1, 1, 2 };
            assertEquals(S_OK, DWNativeShim.textRendererFeedRun(handle, 4f, -8f, 0, 0L, 12f, indices,
                    advances, offsets, clusterMap, 7, 4, 0, 0, true));

            assertEquals(3, FFM.textRendererGetTotalGlyphCount(handle), "before any Next");
            assertTrue(FFM.textRendererNext(handle));
            assertEquals(7, FFM.textRendererGetStart(handle), "the description's textPosition");
            assertEquals(4, FFM.textRendererGetLength(handle), "the description's stringLength");
            assertEquals(3, FFM.textRendererGetGlyphCount(handle));

            int[] glyphs = new int[5];
            assertEquals(3, FFM.textRendererGetGlyphIndices(handle, glyphs, 1, 2 << 24));
            assertArrayEquals(new int[] { 0, 10 | (2 << 24), 20 | (2 << 24), 30 | (2 << 24), 0 }, glyphs);

            int[] tooSmall = new int[2];
            assertEquals(2, FFM.textRendererGetGlyphIndices(handle, tooSmall, 0, 0),
                    "the clamp is min(glyphCount, length - start)");

            float[] someAdvances = new float[3];
            assertEquals(2, FFM.textRendererGetGlyphAdvances(handle, someAdvances, 1));
            assertArrayEquals(new float[] { 0f, 1.5f, 2.5f }, someAdvances);

            float[] allOffsets = new float[6];
            assertEquals(6, FFM.textRendererGetGlyphOffsets(handle, allOffsets, 0));
            assertArrayEquals(offsets, allOffsets, "advanceOffset then ascenderOffset per glyph");

            float[] oddRoom = new float[5];
            assertEquals(0, FFM.textRendererGetGlyphOffsets(handle, oddRoom, 0),
                    "an odd clamped count is rejected after the clamp, not before");
            assertArrayEquals(new float[5], oddRoom, "and nothing is written");

            short[] map = new short[6];
            assertEquals(4, FFM.textRendererGetClusterMap(handle, map, 1, 3));
            assertArrayEquals(new short[] { 0, 3, 4, 4, 5, 0 }, map, "every entry shifted by glyphStart");

            short[] truncated = new short[4];
            assertEquals(4, FFM.textRendererGetClusterMap(handle, truncated, 0, 65536));
            assertArrayEquals(clusterMap, truncated,
                    "glyphStart is truncated to a short before the addition, so 65536 shifts by 0");

            short[] wrapped = new short[4];
            assertEquals(4, FFM.textRendererGetClusterMap(handle, wrapped, 0, 32768));
            assertArrayEquals(new short[] { -32768, -32767, -32767, -32766 }, wrapped,
                    "and the sum is truncated again on store");
        } finally {
            FFM.textRendererDispose(handle);
        }
    }

    /** Every drain answers 0 for a null array or a negative index, as the C's early returns did. */
    @Test
    void theRendererDrainsRejectNullAndNegativeArguments() {
        long handle = FFM.newTextRenderer();
        try {
            DWNativeShim.textRendererFeedRun(handle, 0f, 0f, 0, 0L, 12f, new short[] { 1 },
                    new float[] { 1f }, new float[] { 1f, 1f }, new short[] { 0 }, 0, 1, 0, 0, true);
            assertTrue(FFM.textRendererNext(handle));
            assertEquals(0, FFM.textRendererGetGlyphIndices(handle, null, 0, 0));
            assertEquals(0, FFM.textRendererGetGlyphIndices(handle, new int[4], -1, 0));
            assertEquals(0, FFM.textRendererGetGlyphIndices(handle, new int[4], 0, -1));
            assertEquals(0, FFM.textRendererGetGlyphAdvances(handle, null, 0));
            assertEquals(0, FFM.textRendererGetGlyphAdvances(handle, new float[4], -1));
            assertEquals(0, FFM.textRendererGetGlyphOffsets(handle, null, 0));
            assertEquals(0, FFM.textRendererGetGlyphOffsets(handle, new float[4], -1));
            assertEquals(0, FFM.textRendererGetClusterMap(handle, null, 0, 0));
            assertEquals(0, FFM.textRendererGetClusterMap(handle, new short[4], -1, 0));
            assertEquals(0, FFM.textRendererGetClusterMap(handle, new short[4], 0, -1));
        } finally {
            FFM.textRendererDispose(handle);
        }
    }

    /**
     * {@code glyphAdvances}, {@code glyphOffsets} and {@code clusterMap} are documented optional
     * ({@code _Field_size_opt_}). The C would have dereferenced NULL in its drains; here the drain
     * answers 0, which is a fault turned into a no-op in a case DirectWrite has never produced.
     */
    @Test
    void optionalGlyphRunPointersBecomeEmptyDrains() {
        long handle = FFM.newTextRenderer();
        try {
            DWNativeShim.textRendererFeedRun(handle, 0f, 0f, 0, 0L, 12f, new short[] { 5, 6 }, null,
                    null, null, 3, 2, 0, 0, false);
            assertTrue(FFM.textRendererNext(handle));
            assertEquals(2, FFM.textRendererGetGlyphCount(handle));
            assertEquals(0, FFM.textRendererGetStart(handle), "no description, no text position");
            assertEquals(0, FFM.textRendererGetLength(handle));
            assertEquals(2, FFM.textRendererGetGlyphIndices(handle, new int[2], 0, 0));
            assertEquals(0, FFM.textRendererGetGlyphAdvances(handle, new float[2], 0));
            assertEquals(0, FFM.textRendererGetGlyphOffsets(handle, new float[4], 0));
            assertEquals(0, FFM.textRendererGetClusterMap(handle, new short[2], 0, 0));
        } finally {
            FFM.textRendererDispose(handle);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 4. Lifetime
     * ------------------------------------------------------------------------------------------- */

    /** A thousand of each, created and disposed: nothing is left in either registry. */
    @Test
    void createAndDisposeLeavesNoRegistryEntries() {
        int sinksBefore = DWNativeShim.analysisSinkRegistrySize();
        int renderersBefore = DWNativeShim.textRendererRegistrySize();
        for (int i = 0; i < 1000; i++) {
            long sink = FFM.newAnalysisSink(MIXED.toCharArray(), 2, 6, LOCALE,
                    READING_DIRECTION_LEFT_TO_RIGHT);
            long renderer = FFM.newTextRenderer();
            assertNotEquals(0L, sink);
            assertNotEquals(0L, renderer);
            FFM.analysisSinkAddRef(sink);
            FFM.analysisSinkRelease(sink);
            FFM.analysisSinkDispose(sink);
            FFM.textRendererDispose(renderer);
        }
        assertEquals(sinksBefore, DWNativeShim.analysisSinkRegistrySize());
        assertEquals(renderersBefore, DWNativeShim.textRendererRegistrySize());
    }

    /**
     * A COM client that kept a reference past dispose calls into freed memory. Nothing may fault and
     * nothing may be recorded: the id read out of the block misses the registry and every target
     * answers the sentinel the C would have answered. This is the case FFM cannot catch for us - an
     * upcall parameter has no liveness relationship with the arena that owned it - so it is checked
     * rather than assumed.
     */
    @Test
    void staleCallbacksAfterDisposeDoNothing() {
        long sink = FFM.newAnalysisSink(LATIN.toCharArray(), 0, LATIN.length(), LOCALE,
                READING_DIRECTION_RIGHT_TO_LEFT);
        long renderer = FFM.newTextRenderer();
        long sinkBlock = FFM.analysisSinkPointer(sink);
        long rendererBlock = FFM.textRendererPointer(renderer);
        FFM.analysisSinkDispose(sink);
        FFM.textRendererDispose(renderer);

        long[] r = DWNativeShim.staleCallbackProbe(sinkBlock, rendererBlock);
        assertNotNull(r);
        assertEquals(E_FAIL, r[0], "QueryInterface on a dead object cannot claim to succeed");
        assertEquals(1, r[1], "AddRef answers 1");
        assertEquals(0, r[2], "Release answers 0");
        assertEquals(S_OK, r[3], "SetScriptAnalysis is a no-op that still answers S_OK");
        assertEquals(READING_DIRECTION_LEFT_TO_RIGHT, r[4],
                "GetParagraphReadingDirection answers the zero value");
        assertEquals(S_OK, r[5], "DrawGlyphRun is a no-op that still answers S_OK");
    }

    /* ---------------------------------------------------------------------------------------------
     * 5. CreateTextFormat, CreateTextLayout and the slot arithmetic they pin
     * ------------------------------------------------------------------------------------------- */

    /**
     * Everything {@code CreateTextFormat} was given, read back through slots 20 to 27. This is what
     * makes {@code IDWriteTextFormat} demonstrably 25 methods wide: six different values in eight
     * adjacent slots, so no permutation of the table passes.
     */
    @Test
    void createTextFormatIsReadableBackThroughItsAccessors() {
        long format = FFM.createTextFormat(factory, wide("Arial"), collection, FONT_WEIGHT_BOLD,
                FONT_STYLE_ITALIC, FONT_STRETCH_NORMAL, 17.5f, wide(LOCALE));
        assertNotEquals(0L, format, "IDWriteFactory::CreateTextFormat, slot 15");
        try {
            assertEquals(FONT_WEIGHT_BOLD, DWNativeShim.textFormatFontWeight(format), "slot 22");
            assertEquals(FONT_STYLE_ITALIC, DWNativeShim.textFormatFontStyle(format), "slot 23");
            assertEquals(FONT_STRETCH_NORMAL, DWNativeShim.textFormatFontStretch(format), "slot 24");
            assertEquals(17.5f, DWNativeShim.textFormatFontSize(format),
                    "slot 25 returns a FLOAT in XMM0, not an int in EAX");
            assertEquals(5, DWNativeShim.textFormatFontFamilyNameLength(format), "slot 20");
            assertEquals("Arial", DWNativeShim.textFormatFontFamilyName(format), "slot 21");
            assertEquals(LOCALE.length(), DWNativeShim.textFormatLocaleNameLength(format), "slot 26");
            assertEquals(LOCALE, DWNativeShim.textFormatLocaleName(format), "slot 27");
        } finally {
            FFM.release(format);
        }
    }

    /**
     * The layout inherits every format slot and adds its own from 28, which is the arithmetic that
     * puts {@code Draw} at 58: {@code GetFontSize} at 25 answers on a layout pointer, and
     * {@code GetMaxWidth} at 42 answers the width {@code CreateTextLayout} was given.
     */
    @Test
    void theLayoutInheritsTheFormatSlotsAndAddsItsOwnFrom28() {
        long format = FFM.createTextFormat(factory, wide("Arial"), collection, FONT_WEIGHT_BOLD,
                FONT_STYLE_ITALIC, FONT_STRETCH_NORMAL, 17.5f, wide(LOCALE));
        assertNotEquals(0L, format);
        try {
            char[] text = LATIN.toCharArray();
            long layout = FFM.createTextLayout(factory, text, 0, text.length, format, 640.5f, 480.25f);
            assertNotEquals(0L, layout, "IDWriteFactory::CreateTextLayout, slot 18");
            try {
                assertEquals(17.5f, DWNativeShim.textFormatFontSize(layout),
                        "the layout answers IDWriteTextFormat::GetFontSize at slot 25");
                assertEquals("Arial", DWNativeShim.textFormatFontFamilyName(layout), "slot 21");
                assertEquals(640.5f, DWNativeShim.textLayoutMaxWidth(layout), "slot 42");
                assertEquals(480.25f, DWNativeShim.textLayoutMaxHeight(layout), "slot 43");
            } finally {
                FFM.release(layout);
            }
        } finally {
            FFM.release(format);
        }
    }

    /**
     * <b>The defect these bindings exist to avoid.</b> {@code IDWriteFactory::CreateTextLayout} has no
     * start offset: the C added one to the pinned array itself
     * ({@code directwrite.cpp:1904}), and {@code DWGlyphLayout.renderShape} relies on it for every
     * run that does not begin at 0. Three layouts over the same characters prove it: one from an
     * array where the text is at offset 4, one from an array where it is at offset 0, and one that
     * takes the first three characters of the padded array. The first two must produce identical
     * glyph runs and the third must not - if the offset were dropped, the first would equal the
     * third instead.
     */
    @Test
    void createTextLayoutHonoursTheStartOffset() {
        char[] padded = ("ZZZZ" + "AVX").toCharArray();
        char[] exact = "AVX".toCharArray();
        long format = FFM.createTextFormat(factory, wide("Arial"), collection, 400, 0,
                FONT_STRETCH_NORMAL, 12f, wide(LOCALE));
        assertNotEquals(0L, format);
        try {
            List<String> fromOffset = drawAndDrain(padded, 4, 3, format);
            List<String> fromStart = drawAndDrain(exact, 0, 3, format);
            List<String> firstThree = drawAndDrain(padded, 0, 3, format);
            assertEquals(fromStart, fromOffset,
                    "CreateTextLayout must shape text[start .. start+count), not text[0 .. count)");
            assertNotEquals(firstThree, fromOffset,
                    "and the comparison can tell the difference, so it is not vacuous");
        } finally {
            FFM.release(format);
        }
    }

    private static List<String> drawAndDrain(char[] text, int start, int count, long format) {
        long layout = FFM.createTextLayout(factory, text, start, count, format, 100000f, 100000f);
        assertNotEquals(0L, layout);
        long renderer = FFM.newTextRenderer();
        try {
            FFM.textRendererAddRef(renderer);
            assertEquals(S_OK, FFM.draw(layout, 0, FFM.textRendererPointer(renderer), 0f, 0f));
            return drainTextRenderer(FFM, renderer, count, 0);
        } finally {
            FFM.textRendererRelease(renderer);
            FFM.textRendererDispose(renderer);
            FFM.release(layout);
        }
    }

    /** The three range guards of {@code directwrite.cpp:1900-1902}, each answering 0. */
    @Test
    void createTextLayoutRejectsTheCsThreeRangeErrors() {
        long format = FFM.createTextFormat(factory, wide("Arial"), collection, 400, 0,
                FONT_STRETCH_NORMAL, 12f, wide(LOCALE));
        try {
            char[] text = LATIN.toCharArray();
            assertEquals(0L, FFM.createTextLayout(factory, text, -1, 2, format, 100f, 100f),
                    "a negative start");
            assertEquals(0L, FFM.createTextLayout(factory, text, 0, -1, format, 100f, 100f),
                    "a negative count");
            assertEquals(0L, FFM.createTextLayout(factory, text, 1, Integer.MAX_VALUE, format, 100f,
                    100f), "count > INT_MAX - start");
            assertEquals(0L, FFM.createTextLayout(factory, text, 2, text.length, format, 100f, 100f),
                    "start + count past the end of the array");
            assertEquals(0L, FFM.createTextLayout(factory, null, 0, 0, format, 100f, 100f),
                    "a null array, which the C reached GetArrayLength(NULL) with");
        } finally {
            FFM.release(format);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 6. What DirectWrite does to these objects
     * ------------------------------------------------------------------------------------------- */

    /**
     * The two {@code FLOAT}s of {@code IDWriteTextLayout::Draw} are its third and fourth arguments
     * and travel in XMM2 and XMM3; the two of {@code DrawGlyphRun} likewise. Drawing one layout at
     * three origins isolates both halves at once: moving the origin in x must move every reported
     * baseline in x by the same amount and leave y alone, and vice versa. A float taken from the
     * wrong register, or the two swapped, changes exactly this and nothing else.
     */
    @Test
    void drawMovesEveryBaselineByTheOrigin() {
        long format = FFM.createTextFormat(factory, wide("Arial"), collection, 400, 0,
                FONT_STRETCH_NORMAL, 12f, wide(LOCALE));
        assertNotEquals(0L, format);
        char[] text = (LATIN + HEBREW).toCharArray();
        long layout = FFM.createTextLayout(factory, text, 0, text.length, format, 100000f, 100000f);
        assertNotEquals(0L, layout);
        try {
            long[][] atOrigin = baselines(layout, 0f, 0f);
            long[][] movedInX = baselines(layout, 64f, 0f);
            long[][] movedInY = baselines(layout, 0f, -32f);
            assertTrue(atOrigin.length >= 1, "the layout must produce at least one glyph run");
            assertEquals(atOrigin.length, movedInX.length);
            assertEquals(atOrigin.length, movedInY.length);
            for (int i = 0; i < atOrigin.length; i++) {
                float x = Float.intBitsToFloat((int) atOrigin[i][0]);
                float y = Float.intBitsToFloat((int) atOrigin[i][1]);
                assertEquals(x + 64f, Float.intBitsToFloat((int) movedInX[i][0]), 0.01f,
                        "originX must reach baselineOriginX");
                assertEquals(y, Float.intBitsToFloat((int) movedInX[i][1]), 0.0f,
                        "and must not touch baselineOriginY");
                assertEquals(x, Float.intBitsToFloat((int) movedInY[i][0]), 0.0f,
                        "originY must not touch baselineOriginX");
                assertEquals(y - 32f, Float.intBitsToFloat((int) movedInY[i][1]), 0.01f,
                        "and must reach baselineOriginY");
                assertEquals(0L, atOrigin[i][2], "DWRITE_MEASURING_MODE_NATURAL, the first stack argument");
                assertEquals(12f, Float.intBitsToFloat((int) atOrigin[i][5]), 0.0f,
                        "fontEmSize, read out of DWRITE_GLYPH_RUN at offset 8");
            }
        } finally {
            FFM.release(layout);
            FFM.release(format);
        }
    }

    private static long[][] baselines(long layout, float originX, float originY) {
        long renderer = DWNativeShim.newRecordingTextRenderer();
        try {
            FFM.textRendererAddRef(renderer);
            assertEquals(S_OK, FFM.draw(layout, 0, FFM.textRendererPointer(renderer), originX, originY));
            int count = DWNativeShim.textRendererRunCount(renderer);
            long[][] geometry = new long[count][];
            for (int i = 0; i < count; i++) {
                geometry[i] = DWNativeShim.textRendererRunGeometry(renderer, i);
            }
            return geometry;
        } finally {
            FFM.textRendererRelease(renderer);
            FFM.textRendererDispose(renderer);
        }
    }

    /**
     * The three analyzer methods JavaFX never bound, driven so that the three sink slots
     * {@code AnalyzeScript} never touches are exercised by DirectWrite rather than by a test hook.
     * {@code SetBidiLevel} is the only slot in the package with {@code UINT8} arguments, and the
     * levels it reports are checkable: with a left-to-right paragraph the Latin part must resolve to
     * an even level and the Hebrew and Arabic parts to an odd one.
     */
    @Test
    void theOtherAnalyzeMethodsReachTheirSinkSlots() {
        char[] text = MIXED.toCharArray();
        long handle = DWNativeShim.newRecordingAnalysisSink(text, 0, text.length, LOCALE,
                READING_DIRECTION_LEFT_TO_RIGHT);
        assertNotEquals(0L, handle);
        try {
            FFM.analysisSinkAddRef(handle);
            long source = FFM.analysisSourcePointer(handle);
            long sink = FFM.analysisSinkPointer(handle);
            assertEquals(S_OK, FFM.analyzeScript(analyzer, source, 0, text.length, sink));
            assertEquals(S_OK, DWNativeShim.analyzeBidi(analyzer, source, 0, text.length, sink));
            assertEquals(S_OK,
                    DWNativeShim.analyzeLineBreakpoints(analyzer, source, 0, text.length, sink));
            assertEquals(S_OK,
                    DWNativeShim.analyzeNumberSubstitution(analyzer, source, 0, text.length, sink));

            List<String> callbacks = DWNativeShim.analysisSinkCallbacks(handle);
            assertTrue(callbacks.stream().anyMatch(c -> c.startsWith("SetScriptAnalysis ")),
                    "AnalyzeScript drives sink slot 3");
            assertTrue(callbacks.stream().anyMatch(c -> c.startsWith("SetBidiLevel ")),
                    "AnalyzeBidi drives sink slot 5");
            assertTrue(callbacks.stream().anyMatch(c -> c.startsWith("SetLineBreakpoints ")),
                    "AnalyzeLineBreakpoints drives sink slot 4");

            boolean sawEven = false;
            boolean sawOdd = false;
            for (String callback : callbacks) {
                if (!callback.startsWith("SetBidiLevel ")) {
                    continue;
                }
                String[] parts = callback.split(" ");
                int position = Integer.parseInt(parts[1]);
                int length = Integer.parseInt(parts[2]);
                int explicitLevel = Integer.parseInt(parts[3]);
                int resolvedLevel = Integer.parseInt(parts[4]);
                assertTrue(position >= 0 && length > 0 && position + length <= text.length,
                        () -> "SetBidiLevel reported a range outside the text: " + callback);
                assertTrue(explicitLevel <= 125 && resolvedLevel <= 125,
                        () -> "a bidi level is at most 125, so these are real UINT8s: " + callback);
                sawEven |= resolvedLevel % 2 == 0;
                sawOdd |= resolvedLevel % 2 == 1;
            }
            assertTrue(sawEven, "the Latin part of the string resolves to an even bidi level");
            assertTrue(sawOdd, "and the Hebrew and Arabic parts to an odd one");
        } finally {
            FFM.analysisSinkRelease(handle);
            FFM.analysisSinkDispose(handle);
        }
    }

    /**
     * A measurement that could not be made directly: whether DirectWrite ever calls
     * {@code QueryInterface} on the objects it is handed.
     * <p>
     * It does. On {@code dwrite.dll} 10.0.19041 the analysis sink is never queried, but the text
     * renderer is asked for {@code IDWriteTextRenderer1}
     * (d3e0e934-22a0-427e-aae4-7d9574b59db1, dwrite_2.h:83) once per {@code Draw}, and falls back to
     * the base interface when that is refused. The C refused it from the same {@code else} branch
     * ({@code directwrite.cpp:1422-1425}), so the two paths agree - which
     * The parity test that has since been deleted then showed
     * glyph for glyph. What is asserted here is the invariant rather than the version: every IID
     * this object claims is one of its own, and every other one is refused.
     */
    @Test
    void theQueryInterfaceLogSaysWhetherDirectWriteEverAsks() {
        char[] text = (LATIN + ARABIC).toCharArray();
        long sink = DWNativeShim.newRecordingAnalysisSink(text, 0, text.length, LOCALE,
                READING_DIRECTION_LEFT_TO_RIGHT);
        long format = FFM.createTextFormat(factory, wide("Arial"), collection, 400, 0,
                FONT_STRETCH_NORMAL, 12f, wide(LOCALE));
        long layout = FFM.createTextLayout(factory, text, 0, text.length, format, 100000f, 100000f);
        long renderer = DWNativeShim.newRecordingTextRenderer();
        try {
            FFM.analysisSinkAddRef(sink);
            FFM.textRendererAddRef(renderer);
            assertEquals(S_OK, FFM.analyzeScript(analyzer, FFM.analysisSourcePointer(sink), 0,
                    text.length, FFM.analysisSinkPointer(sink)));
            assertEquals(S_OK, FFM.draw(layout, 0, FFM.textRendererPointer(renderer), 0f, 0f));

            List<String> sinkIids = DWNativeShim.analysisSinkQueriedIids(sink);
            List<String> rendererIids = DWNativeShim.textRendererQueriedIids(renderer);
            System.out.println("DirectWrite queried the analysis sink for " + sinkIids);
            System.out.println("DirectWrite queried the text renderer for " + rendererIids);
            assertQueryInterfaceLog(sinkIids, List.of(DWNativeShim.iidAnalysisSink(),
                    DWNativeShim.iidAnalysisSource(), DWNativeShim.iidUnknown()));
            assertQueryInterfaceLog(rendererIids, List.of(DWNativeShim.iidTextRenderer(),
                    DWNativeShim.iidPixelSnapping(), DWNativeShim.iidUnknown()));
        } finally {
            FFM.analysisSinkRelease(sink);
            FFM.analysisSinkDispose(sink);
            FFM.textRendererRelease(renderer);
            FFM.textRendererDispose(renderer);
            FFM.release(layout);
            FFM.release(format);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * 7. The two objects under the whole font stack
     * ------------------------------------------------------------------------------------------- */

    /**
     * Shaping a string that forces both objects - an analysis sink for every {@code addTextRun}, and
     * the text renderer for the characters Arial does not have - must leave neither behind.
     * <p>
     * This is the property that the flip introduced and nothing else can check: the C++ objects
     * deleted themselves when their reference count reached zero, while these two own confined
     * arenas that only the thread that created them can close, so {@code DWGlyphLayout} has to
     * dispose them explicitly. A missing dispose is invisible to every other test - the shaping
     * output is identical - and leaks a registry entry and an arena per run.
     * <p>
     * The fallback assertion keeps it honest: without a glyph carrying a non-zero composite slot the
     * renderer half of the check would pass vacuously.
     * <p>
     * <b>The family is deliberately Verdana.</b> A composite font resource hands out its fallback
     * slots in order of first use, and those slot numbers are the high byte of every glyph code
     * {@code DirectWriteMetricsGoldenTest} records. Shaping any of the four families that golden
     * pins - Arial, Segoe UI, Times New Roman, Consolas - from here would renumber their slots in a
     * shared surefire JVM and fail the golden, several classes later, for no reason connected to
     * what went wrong.
     */
    @Test
    void shapingThroughTheFontStackDisposesEverySinkAndRenderer() {
        PrismFontFactory fontFactory = PrismFontFactory.getFontFactory();
        assertNotNull(fontFactory, "PrismFontFactory.getFontFactory()");
        assertTrue(fontFactory instanceof DWFactory,
                () -> "this test needs the DirectWrite factory, got " + fontFactory.getClass());
        PrismTextLayout textLayout = new PrismTextLayout(0);
        textLayout.setContent(MIXED, fontFactory.createFont("Verdana", 24f));
        GlyphList[] runs = textLayout.getRuns();
        assertTrue(runs.length > 1, () -> "the mixed-script string must split into runs: " + runs.length);

        boolean fallback = false;
        for (GlyphList run : runs) {
            for (int g = 0; g < run.getGlyphCount(); g++) {
                fallback |= (run.getGlyphCode(g) >>> 24) != 0;
            }
        }
        assertTrue(fallback, "Verdana has no CJK, Devanagari or U+1D11E, so the fallback path - "
                + "IDWriteTextLayout::Draw into the text renderer - must have run");

        assertEquals(0, DWNativeShim.analysisSinkRegistrySize(),
                "DWGlyphLayout.addTextRun must dispose the analysis sink it created");
        assertEquals(0, DWNativeShim.textRendererRegistrySize(),
                "DWGlyphLayout.renderShape must dispose the text renderer it created");
    }

    /** Every logged query is {@code "<uuid> S_OK"} for an IID of this object and E_FAIL otherwise. */
    private static void assertQueryInterfaceLog(List<String> log, List<String> own) {
        for (String entry : log) {
            String[] parts = entry.split(" ");
            assertEquals(2, parts.length, () -> "malformed QueryInterface log entry: " + entry);
            assertEquals(own.contains(parts[0]) ? "S_OK" : "E_FAIL", parts[1],
                    () -> "the wrong answer for " + entry);
        }
    }
}
