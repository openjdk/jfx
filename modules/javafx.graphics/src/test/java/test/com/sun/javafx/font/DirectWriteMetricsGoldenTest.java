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

package test.com.sun.javafx.font;

import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * Parity oracle for what the DirectWrite path of {@code javafx_font} ({@code directwrite.cpp}) delivers to
 * Prism, against goldens captured from the JNI build.
 * <p>
 * For four fonts that ship with Windows 10 (Arial, Segoe UI, Times New Roman, Consolas) at two sizes, and one
 * fixed string mixing ASCII, a combining mark, Latin-1, Greek, Cyrillic, Hebrew and Arabic (right-to-left,
 * contextual shaping), CJK, a Devanagari conjunct and a supplementary-plane character, the golden
 * {@value #GOLDEN} records what crosses the JNI boundary today, through Prism's own font API:
 * <ul>
 * <li>the resolved font resource (names, file, units per em) and its metrics ({@link Metrics});</li>
 * <li>code point to glyph mapping ({@link CharToGlyphMapper}, composite fallback slots included);</li>
 * <li>per glyph: advance and bounding box ({@code IDWriteFontFace::GetDesignGlyphMetrics}), the alpha texture
 * bounds and the ClearType 3x1 texture ({@code IDWriteGlyphRunAnalysis::GetAlphaTextureBounds /
 * CreateAlphaTexture}, as length and sha256) and the outline ({@code GetGlyphRunOutline} via the
 * {@code JFXGeometrySink}, as segment count and sha256 of the exact float stream);</li>
 * <li>shaping: the runs {@link PrismTextLayout} produces ({@code IDWriteTextAnalyzer::AnalyzeScript} through the
 * {@code JFXTextAnalysisSink}, {@code GetGlyphs / GetGlyphPlacements}, and for glyphs the primary font lacks
 * the {@code IDWriteTextLayout::Draw / JFXTextRenderer} fallback path) with glyph codes, positions, cluster
 * offsets, script ids, and the fallback fonts the composite resource ended up holding.</li>
 * </ul>
 * The greyscale mask path ({@code ID2D1RenderTarget::DrawGlyphRun} into a WIC bitmap) is <em>not</em> pinned:
 * it needs a {@code GraphicsPipeline} for its dispose hook, which a headless unit test does not have. The
 * masks here are captured from an explicit {@link FontResource#AA_LCD} strike, which stays inside DirectWrite.
 * <p>
 * DirectWrite output depends on the installed {@code dwrite.dll} and on the font files' versions, so the
 * comparison runs only where the {@code machine.} header (OS, arch, sha256 of dwrite.dll and of the four font
 * files) matches; elsewhere it is skipped with the capture command - or failed, under
 * {@code -Djfx.parity.require=true}, on the machine that owns the golden ({@link ParityGate}). See
 * {@link FontGoldens}.
 */
@EnabledOnOs(OS.WINDOWS)
public class DirectWriteMetricsGoldenTest {

    static final String GOLDEN = "directwrite-metrics-golden.txt";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(DirectWriteMetricsGoldenTest.class);

    /**
     * ASCII with a possible fi ligature, digits and symbols; e + combining acute; Latin-1; Greek; Cyrillic;
     * Hebrew (RTL); Arabic (RTL, contextual forms); CJK (outside all four fonts: fallback); a Devanagari
     * conjunct (complex shaping, fallback); U+1D11E musical symbol G clef as a surrogate pair (fallback).
     */
    static final String TEXT = "Hello, World! fi 0123456789 @#&% e\u0301 \u00E9\u00F1 \u0391\u03B2\u0413"
            + " \u05E9\u05DC\u05D5\u05DD \u0645\u0631\u062D\u0628\u0627 \u65E5\u672C\u8A9E"
            + " \u0915\u094D\u0937 \uD834\uDD1E";

    static final float[] SIZES = {12f, 24f};

    /** Family name as an application would request it, key used in the golden, file that ships with Windows. */
    record FontCase(String name, String key, String file) {
    }

    static final List<FontCase> FONTS = List.of(
            new FontCase("Arial", "arial", "arial.ttf"),
            new FontCase("Segoe UI", "segoeui", "segoeui.ttf"),
            new FontCase("Times New Roman", "timesnewroman", "times.ttf"),
            new FontCase("Consolas", "consolas", "consola.ttf"));

    @Test
    public void directWriteMetricsMatchTheJniGolden() throws IOException {
        Map<String, String> captured = capture();
        if (FontGoldens.captureRequested()) {
            FontGoldens.write(GOLDEN, captured, header());
            abort("golden captured to " + GOLDEN + "; a capture run verifies nothing");
        }
        FontGoldens golden = FontGoldens.load(GOLDEN, DirectWriteMetricsGoldenTest.class);
        golden.assumeSameMachine(captured, DirectWriteMetricsGoldenTest.class);
        golden.assertSameContent(captured);
    }

    /** The machine gate passed at least once, so at least one body key was compared ({@link ParityGate}). */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /** The oracle must not be vacuous: the fonts resolve to their files and DirectWrite produces glyphs. */
    @Test
    public void fontsResolveAndShape() {
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        for (FontCase font : FONTS) {
            PGFont pgFont = factory.createFont(font.name(), 12f);
            FontResource resource = pgFont.getFontResource();
            assertEquals(font.name(), resource.getFamilyName(), font.name() + " resolved to " + resource.getFullName());
            assertTrue(resource.getFileName().toLowerCase(Locale.ROOT).endsWith(font.file()),
                    font.name() + " resolved to " + resource.getFileName());
            PrismTextLayout layout = new PrismTextLayout(0);
            layout.setContent("Hello", pgFont);
            GlyphList[] runs = layout.getRuns();
            assertEquals(1, runs.length, "runs for Hello in " + font.name());
            assertEquals(5, runs[0].getGlyphCount(), "glyphs for Hello in " + font.name());
            assertTrue(runs[0].getWidth() > 0, "width for Hello in " + font.name());
            FontStrike strike = resource.getStrike(12f, BaseTransform.IDENTITY_TRANSFORM, FontResource.AA_LCD);
            Glyph glyph = strike.getGlyph(runs[0].getGlyphCode(0));
            assertTrue(glyph.getAdvance() > 0, "advance of H in " + font.name());
            assertTrue(glyph.getPixelData().length > 0, "LCD mask of H in " + font.name());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    static Map<String, String> capture() {
        Map<String, String> out = new LinkedHashMap<>();
        FontGoldens.putOperatingSystem(out);
        Path dwrite = FontGoldens.windowsDirectory().resolve("System32").resolve("dwrite.dll");
        out.put("machine.dwrite.sha256",
                Files.isRegularFile(dwrite) ? FontGoldens.sha256Hex(dwrite) : FontGoldens.NULL);
        out.put("info.dwrite.path", dwrite.toString());
        for (FontCase font : FONTS) {
            Path file = FontGoldens.systemFontDirectory().resolve(font.file());
            boolean present = Files.isRegularFile(file);
            out.put("machine.font." + font.key() + ".sha256", present ? FontGoldens.sha256Hex(file) : FontGoldens.NULL);
            out.put("info.font." + font.key() + ".path", file.toString());
            out.put("info.font." + font.key() + ".version",
                    present ? FontGoldens.openTypeVersion(file) : FontGoldens.NULL);
        }

        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        assertNotNull(factory, "PrismFontFactory.getFontFactory()");
        out.put("info.factory", factory.getClass().getName());
        out.put("info.subPixelMode", Integer.toString(factory.getSubPixelMode()));
        out.put("info.lcdTextSupported", Boolean.toString(factory.isLCDTextSupported()));
        out.put("text", TEXT);
        out.put("text.length", Integer.toString(TEXT.length()));
        out.put("text.utf8.sha256", FontGoldens.sha256Hex(TEXT.getBytes(StandardCharsets.UTF_8)));

        for (FontCase font : FONTS) {
            for (float size : SIZES) {
                captureFont(out, factory, font, size);
            }
        }
        return out;
    }

    private static void captureFont(Map<String, String> out, PrismFontFactory factory, FontCase font, float size) {
        String prefix = "font." + font.key() + "." + Float.toString(size) + ".";
        PGFont pgFont = factory.createFont(font.name(), size);
        FontResource resource = pgFont.getFontResource();
        out.put(prefix + "pgFont.fullName", pgFont.getFullName());
        out.put(prefix + "pgFont.name", pgFont.getName());
        out.put(prefix + "resource.class", resource.getClass().getName());
        captureResource(out, prefix + "resource.", resource);

        FontStrike lcdStrike = resource.getStrike(size, BaseTransform.IDENTITY_TRANSFORM, FontResource.AA_LCD);
        out.put(prefix + "strike.class", lcdStrike.getClass().getName());
        out.put(prefix + "strike.aaMode", Integer.toString(lcdStrike.getAAMode()));
        out.put(prefix + "strike.drawAsShapes", Boolean.toString(lcdStrike.drawAsShapes()));
        captureMetrics(out, prefix + "metrics.", lcdStrike.getMetrics());

        TreeSet<Integer> glyphCodes = new TreeSet<>();
        CharToGlyphMapper mapper = resource.getGlyphMapper();
        TreeSet<Integer> codePoints = new TreeSet<>();
        TEXT.codePoints().forEach(codePoints::add);
        for (int codePoint : codePoints) {
            int glyphCode = mapper.charToGlyph(codePoint);
            glyphCodes.add(glyphCode);
            out.put(prefix + "map.U+" + String.format(Locale.ROOT, "%04X", codePoint), Integer.toString(glyphCode));
        }
        out.put(prefix + "map.missingGlyphCode", Integer.toString(mapper.getMissingGlyphCode()));

        captureLayout(out, prefix + "layout.", pgFont, glyphCodes);

        if (resource instanceof CompositeFontResource composite) {
            int slots = composite.getNumSlots();
            out.put(prefix + "slots.count", Integer.toString(slots));
            for (int slot = 0; slot < slots; slot++) {
                FontResource slotResource = composite.getSlotResource(slot);
                out.put(prefix + "slots." + slot, slotResource == null ? FontGoldens.NULL
                        : slotResource.getFullName() + "|" + slotResource.getFileName());
            }
        }

        out.put(prefix + "glyphs.count", Integer.toString(glyphCodes.size()));
        for (int glyphCode : glyphCodes) {
            captureGlyph(out, prefix + "glyph." + glyphCode + ".", lcdStrike, glyphCode);
        }
    }

    private static void captureResource(Map<String, String> out, String prefix, FontResource resource) {
        out.put(prefix + "fullName", FontGoldens.nullable(resource.getFullName()));
        out.put(prefix + "psName", FontGoldens.nullable(resource.getPSName()));
        out.put(prefix + "familyName", FontGoldens.nullable(resource.getFamilyName()));
        out.put(prefix + "styleName", FontGoldens.nullable(resource.getStyleName()));
        out.put(prefix + "localeFullName", FontGoldens.nullable(resource.getLocaleFullName()));
        out.put(prefix + "fileName", FontGoldens.nullable(resource.getFileName()));
        out.put(prefix + "bold", Boolean.toString(resource.isBold()));
        out.put(prefix + "italic", Boolean.toString(resource.isItalic()));
        out.put(prefix + "features", Integer.toString(resource.getFeatures()));
        out.put(prefix + "defaultAAMode", Integer.toString(resource.getDefaultAAMode()));
        out.put(prefix + "embedded", Boolean.toString(resource.isEmbeddedFont()));
    }

    private static void captureMetrics(Map<String, String> out, String prefix, Metrics metrics) {
        out.put(prefix + "ascent", Float.toString(metrics.getAscent()));
        out.put(prefix + "descent", Float.toString(metrics.getDescent()));
        out.put(prefix + "lineGap", Float.toString(metrics.getLineGap()));
        out.put(prefix + "lineHeight", Float.toString(metrics.getLineHeight()));
        out.put(prefix + "typoAscent", Float.toString(metrics.getTypoAscent()));
        out.put(prefix + "typoDescent", Float.toString(metrics.getTypoDescent()));
        out.put(prefix + "typoLineGap", Float.toString(metrics.getTypoLineGap()));
        out.put(prefix + "xHeight", Float.toString(metrics.getXHeight()));
        out.put(prefix + "capHeight", Float.toString(metrics.getCapHeight()));
        out.put(prefix + "strikethroughOffset", Float.toString(metrics.getStrikethroughOffset()));
        out.put(prefix + "strikethroughThickness", Float.toString(metrics.getStrikethroughThickness()));
        out.put(prefix + "underLineOffset", Float.toString(metrics.getUnderLineOffset()));
        out.put(prefix + "underLineThickness", Float.toString(metrics.getUnderLineThickness()));
    }

    /** Shaping through PrismTextLayout directly: the Toolkit's layout factory is a stub under StubToolkit. */
    private static void captureLayout(Map<String, String> out, String prefix, PGFont pgFont,
                                      TreeSet<Integer> glyphCodes) {
        PrismTextLayout layout = new PrismTextLayout(0);
        layout.setContent(TEXT, pgFont);
        GlyphList[] runs = layout.getRuns();
        BaseBounds bounds = layout.getBounds();
        out.put(prefix + "bounds", FontGoldens.joinFloats(new float[] {
            bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()}, 4));
        out.put(prefix + "lines", Integer.toString(layout.getLines().length));
        out.put(prefix + "runs", Integer.toString(runs.length));
        for (int r = 0; r < runs.length; r++) {
            GlyphList run = runs[r];
            String runPrefix = prefix + "run." + r + ".";
            int count = run.getGlyphCount();
            int[] codes = new int[count];
            int[] offsets = new int[count];
            float[] posX = new float[count + 1];
            float[] posY = new float[count + 1];
            for (int g = 0; g < count; g++) {
                codes[g] = run.getGlyphCode(g);
                offsets[g] = run.getCharOffset(g);
                posX[g] = run.getPosX(g);
                posY[g] = run.getPosY(g);
                glyphCodes.add(codes[g]);
            }
            posX[count] = run.getPosX(count);
            posY[count] = run.getPosY(count);
            out.put(runPrefix + "start", Integer.toString(run.getStart()));
            out.put(runPrefix + "glyphCount", Integer.toString(count));
            out.put(runPrefix + "complex", Boolean.toString(run.isComplex()));
            out.put(runPrefix + "width", Float.toString(run.getWidth()));
            out.put(runPrefix + "height", Float.toString(run.getHeight()));
            if (run instanceof TextRun textRun) {
                out.put(runPrefix + "length", Integer.toString(textRun.getLength()));
                out.put(runPrefix + "level", Byte.toString(textRun.getLevel()));
                out.put(runPrefix + "ltr", Boolean.toString(textRun.isLeftToRight()));
                out.put(runPrefix + "script", Integer.toString(textRun.getScript()));
                out.put(runPrefix + "slot", Integer.toString(textRun.getSlot()));
                out.put(runPrefix + "ascent", Float.toString(textRun.getAscent()));
                out.put(runPrefix + "descent", Float.toString(textRun.getDescent()));
                out.put(runPrefix + "leading", Float.toString(textRun.getLeading()));
            }
            out.put(runPrefix + "glyphs", FontGoldens.joinInts(codes, count));
            out.put(runPrefix + "charOffsets", FontGoldens.joinInts(offsets, count));
            out.put(runPrefix + "posX", FontGoldens.joinFloats(posX, count + 1));
            out.put(runPrefix + "posY", FontGoldens.joinFloats(posY, count + 1));
        }
    }

    /**
     * One glyph through the LCD strike: design metrics, alpha texture bounds and texture, outline. The glyph
     * code carries the composite slot in its high byte, so fallback glyphs resolve to their own font.
     */
    private static void captureGlyph(Map<String, String> out, String prefix, FontStrike strike, int glyphCode) {
        Glyph glyph = strike.getGlyph(glyphCode);
        out.put(prefix + "code", Integer.toString(glyph.getGlyphCode()));
        out.put(prefix + "advance", Float.toString(glyph.getAdvance()));
        RectBounds bbox = glyph.getBBox();
        out.put(prefix + "bbox", bbox == null ? FontGoldens.NULL : FontGoldens.joinFloats(new float[] {
            bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY()}, 4));
        out.put(prefix + "lcd", Boolean.toString(glyph.isLCDGlyph()));
        byte[] mask = glyph.getPixelData();
        out.put(prefix + "texture", FontGoldens.joinInts(new int[] {
            glyph.getOriginX(), glyph.getOriginY(), glyph.getWidth(), glyph.getHeight()}, 4));
        out.put(prefix + "mask.length", mask == null ? FontGoldens.NULL : Integer.toString(mask.length));
        out.put(prefix + "mask.sha256", mask == null ? FontGoldens.NULL : FontGoldens.sha256Hex(mask));
        out.put(prefix + "pixelAdvance", FontGoldens.joinFloats(new float[] {
            glyph.getPixelXAdvance(), glyph.getPixelYAdvance()}, 2));
        captureOutline(out, prefix + "outline.", glyph.getShape());
    }

    /** The outline as DirectWrite delivered it: segment count, winding rule, sha256 of the exact float stream. */
    private static void captureOutline(Map<String, String> out, String prefix, Shape shape) {
        if (shape == null) {
            out.put(prefix + "segments", FontGoldens.NULL);
            return;
        }
        PathIterator path = shape.getPathIterator(BaseTransform.IDENTITY_TRANSFORM);
        StringBuilder stream = new StringBuilder();
        float[] coords = new float[6];
        int segments = 0;
        String first = null;
        while (!path.isDone()) {
            int type = path.currentSegment(coords);
            int count = switch (type) {
                case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> 2;
                case PathIterator.SEG_QUADTO -> 4;
                case PathIterator.SEG_CUBICTO -> 6;
                default -> 0;
            };
            stream.append(type);
            for (int i = 0; i < count; i++) {
                stream.append(' ').append(Float.toString(coords[i]));
            }
            stream.append('\n');
            if (first == null) {
                first = stream.toString().trim();
            }
            segments++;
            path.next();
        }
        out.put(prefix + "class", shape.getClass().getName());
        out.put(prefix + "windingRule", Integer.toString(path.getWindingRule()));
        out.put(prefix + "segments", Integer.toString(segments));
        out.put(prefix + "first", first == null ? FontGoldens.NULL : first);
        out.put(prefix + "sha256", FontGoldens.sha256Hex(stream.toString().getBytes(StandardCharsets.UTF_8)));
        if (shape instanceof Path2D path2D) {
            out.put(prefix + "numTypes", Integer.toString(segments));
            RectBounds bounds = path2D.getBounds();
            out.put(prefix + "bounds", FontGoldens.joinFloats(new float[] {
                bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()}, 4));
        }
    }

    private static List<String> header() {
        return List.of(
                "DirectWrite metrics golden, captured from the JNI build of javafx_font (directwrite.cpp) before",
                "any of it was rewritten. Machine-specific: DirectWrite output depends on dwrite.dll and on the",
                "font files' versions. The comparison runs only where every machine.* key matches.",
                "",
                "Capture:    " + FontGoldens.captureCommand(DirectWriteMetricsGoldenTest.class),
                "Regenerate: add -D" + FontGoldens.REGENERATE_PROPERTY + "=true (reviewed as a behaviour change)",
                "",
                "Keys (prefix font.<font>.<size>.):",
                "  machine.*                  gate: os, arch, sha256 of dwrite.dll and of the four font files",
                "  info.* / capture.*         recorded, never compared (font name-table versions live here)",
                "  resource.*                 the FontResource createFont resolved: names, file, flags",
                "  metrics.*                  FontStrike.getMetrics(): PrismMetrics (Java, from hhea/OS/2 tables)",
                "  map.U+XXXX                 CharToGlyphMapper.charToGlyph (composite: slot in the high byte)",
                "  layout.*                   PrismTextLayout runs: AnalyzeScript, GetGlyphs/GetGlyphPlacements,",
                "                             and the IDWriteTextLayout::Draw fallback path for missing glyphs",
                "  slots.*                    fallback fonts the composite resource holds after layout",
                "  glyph.<code>.advance/bbox  IDWriteFontFace::GetDesignGlyphMetrics scaled to the size",
                "  glyph.<code>.texture       IDWriteGlyphRunAnalysis::GetAlphaTextureBounds +-1 (x, y, w*3, h)",
                "  glyph.<code>.mask.*        IDWriteGlyphRunAnalysis::CreateAlphaTexture CLEARTYPE_3x1 bytes",
                "  glyph.<code>.outline.*     IDWriteFontFace::GetGlyphRunOutline via JFXGeometrySink, as a",
                "                             sha256 of \"<type> <x> <y> ...\" lines with Float.toString",
                "  text                       the fixed string; \\0 = NUL, other escapes as in FontGoldens");
    }
}
