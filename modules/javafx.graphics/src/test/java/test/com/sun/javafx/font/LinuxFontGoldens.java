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

import com.sun.javafx.font.FontConfigManager;
import com.sun.javafx.font.FontConfigManagerShim;
import com.sun.javafx.font.LinuxFontOracleShim;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * Shared machinery of the Linux font goldens: what the JNI build of commit {@code 7b43255b30}
 * ({@code fontpath_linux.c}, {@code freetype.c}, {@code pango.c}) returned, captured before any of it was
 * rewritten, and compared against every later build with the same test code.
 * <p>
 * The file format, the capture and regenerate switches and the machine gate are those of {@link FontGoldens},
 * which is reused unchanged. What this class adds for Linux:
 * <ul>
 * <li>a machine gate computed without touching any native under test ({@link #machine()}): OS release, sha256 of
 * every system library and font file the outputs depend on, the fontconfig inventory and configuration,
 * environment, locale and properties. The comparison is skipped (or failed under
 * {@code -Djfx.parity.require=true}) before the corpus runs, so a machine with other fonts never reaches it;</li>
 * <li>a capture guard ({@link #requireJniBuild()}) that refuses to write a golden from anything but the JNI
 * libraries and the {@code native} methods that call them;</li>
 * <li>a binding witness ({@link #requireSameBinding()}), run in both modes, that fails when the shims the
 * goldens go through stop calling the entry points the production font classes call;</li>
 * <li>a pristine-process guard ({@link #requirePristineProcess()}): fontconfig application fonts are process
 * state that every later lookup sees, so no test in this fork may have registered one;</li>
 * <li>a child JVM launcher for the cases that change process state ({@link #runChild}).</li>
 * </ul>
 * All maps are sorted, every key is written once ({@link GoldenMap}), floats use {@link Float#toString} and
 * coordinate streams are hashed over {@link Float#floatToRawIntBits}, so equal behaviour gives byte-identical
 * files.
 */
final class LinuxFontGoldens {

    /** The commit whose JNI libraries the goldens record. */
    static final String CAPTURE_COMMIT = "7b43255b30";

    static final String WORK_DIRECTORY = "target/linux-font-golden";

    static final List<String> JNI_LIBRARIES =
            List.of("libjavafx_font.so", "libjavafx_font_freetype.so", "libjavafx_font_pango.so");

    /** Libraries whose code decides what the font layer returns; their sha256 is part of the gate. */
    static final List<String> SONAMES = List.of(
            "libfreetype.so.6", "libfontconfig.so.1", "libpango-1.0.so.0", "libpangoft2-1.0.so.0",
            "libharfbuzz.so.0", "libglib-2.0.so.0", "libgobject-2.0.so.0", "libfribidi.so.0", "libexpat.so.1",
            "libgraphite2.so.3", "libc.so.6", "libm.so.6");

    /** Java properties read once by the font layer's static initializers; passed to child JVMs explicitly. */
    static final List<String> FONT_PROPERTIES = List.of(
            "com.sun.javafx.fontSize", "prism.cacheLayoutSize", "prism.debugfonts", "prism.embeddedfonts",
            "prism.fontSizeLimit", "prism.fontdir", "prism.lcdtext", "prism.subpixeltext", "prism.useFontConfig");

    /** Environment the C reads with {@code getenv}, and the variables fontconfig expands paths from. */
    static final List<String> ENVIRONMENT = List.of(
            "FC_DEBUG", "FC_LANG", "FONTCONFIG_FILE", "FONTCONFIG_PATH", "FONTCONFIG_SYSROOT", "FREETYPE_PROPERTIES",
            "HOME", "LANG", "LANGUAGE", "LC_ALL", "LC_CTYPE", "PANGO_LANGUAGE", "PRISM_FONTCONFIG_DEBUG",
            "XDG_CACHE_HOME", "XDG_CONFIG_HOME", "XDG_DATA_HOME");

    /** Variables removed from every child JVM's environment, so that they cannot change what it records. */
    static final List<String> CHILD_CLEARED_ENVIRONMENT = List.of("FC_LANG", "LANGUAGE", "LC_ALL", "LC_CTYPE");

    /** A font file whose sha256 is gated. */
    record Pinned(String key, Path path) {
    }

    static final String AHEM = "../javafx.web/src/main/native/Tools/DumpRenderTree/fonts/AHEM____.TTF";

    /** An OpenType font with CFF outlines: fontconfig reports its format as {@code CFF}, not {@code TrueType}. */
    static final String FONT_WITH_FEATURES_OTF =
            "../javafx.web/src/main/native/Tools/DumpRenderTree/fonts/FontWithFeatures.otf";

    static final List<Pinned> PINNED = List.of(
            new Pinned("dejavusans", Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")),
            new Pinned("dejavusansbold", Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")),
            new Pinned("dejavusansoblique", Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf")),
            new Pinned("dejavusanscondensed", Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf")),
            new Pinned("dejavuserif", Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf")),
            new Pinned("dejavusansmono", Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf")),
            new Pinned("latoregular", Path.of("/usr/share/fonts/truetype/lato/Lato-Regular.ttf")),
            new Pinned("ubunturegular", Path.of("/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf")),
            new Pinned("freeeuro", Path.of("/usr/share/groff/1.23.0/font/devps/freeeuro.pfa")),
            new Pinned("ahem", Path.of(AHEM)),
            new Pinned("fontwithfeaturesotf", Path.of(FONT_WITH_FEATURES_OTF)));

    /**
     * The shared string corpus. Lone surrogates, NUL and bidi mixtures are deliberate; the golden records every
     * input as UTF-16 code units ({@code info.corpus.sNN}).
     */
    static final List<String> TEXTS = List.of(
            "",
            "Hello, World! fi ffl 0123",
            "e\u0301 a\u0308\u0323 A\u030A",
            "\u05E9\u05B8\u05C1\u05DC\u05D5\u05B9\u05DD",
            "\u0645\u0631\u062D\u0628\u0627 \u0644\u0627 \uFEFB",
            "abc \u05D0\u05D1\u05D2 123 \u0645\u0631\u062D def",
            "\u0915\u094D\u0937\u0924\u094D\u0930\u093F\u092F",
            "\u65E5\u672C\u8A9E\u3002",
            "\uD83D\uDE00\uD83D\uDC4D\uD83C\uDFFD \uD83E\uDD1D\u200D",
            "\uD835\uDC00\uD835\uDD04",
            "ab\uD800",
            "a\uDC00b",
            "\uDC00\uD800",
            "\u05D0\uD835\uDC00\u05D1",
            "\u0645\u0631 \uD835\uDC00 \u0627",
            "\u05D0\u0000\u05D1",
            "a\tb\u200D\u200Cc\u00AD",
            "\uFEFF\uE000\uFFFE\uFFFF",
            "\u05D0\uD83D\uDE00\u05D1");

    private static final HexFormat HEX = HexFormat.of();

    private static Map<String, String> machine;

    private LinuxFontGoldens() {
    }

    // ---------------------------------------------------------------------------------------------
    // The golden map
    // ---------------------------------------------------------------------------------------------

    /** A sorted map that rejects a second value for a key: a golden key written twice is a corpus bug. */
    static final class GoldenMap extends TreeMap<String, String> {

        GoldenMap() {
        }

        GoldenMap(Map<String, String> initial) {
            for (Map.Entry<String, String> entry : initial.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public String put(String key, String value) {
            if (containsKey(key)) {
                throw new IllegalStateException("golden key written twice: " + key);
            }
            return super.put(key, value == null ? FontGoldens.NULL : value);
        }
    }

    /** The body of a capture: fills {@code out}. */
    @FunctionalInterface
    interface Corpus {
        void capture(GoldenMap out) throws Exception;
    }

    /** A check that the values a golden holds can tell right from the wrong implementations it guards against. */
    @FunctionalInterface
    interface Coverage {
        void assertDiscriminating(Map<String, String> values);
    }

    // ---------------------------------------------------------------------------------------------
    // Capture / verify
    // ---------------------------------------------------------------------------------------------

    /**
     * The flow of every gated golden test. Both modes start with {@link #requireSameBinding()}, which needs no
     * native and runs on any Linux. The gate runs before the corpus: on another machine the pinned files may not
     * exist at all. Capture mode ({@code -Djfx.font.golden.capture=true}) refuses to run on anything but the JNI
     * build, asserts the corpus is discriminating before writing, and then aborts: a capture verifies nothing.
     *
     * @param inProcess whether the corpus runs in this JVM, and therefore needs the pristine-process guard
     */
    static void captureOrVerify(Class<?> test, String golden, List<String> header, boolean inProcess,
                                Corpus corpus, Coverage coverage) throws Exception {
        requireSameBinding();
        Map<String, String> gate = machine();
        if (FontGoldens.captureRequested()) {
            requireJniBuild();
            if (inProcess) {
                requirePristineProcess();
            }
            GoldenMap out = new GoldenMap(gate);
            putCaptureInfo(out);
            corpus.capture(out);
            coverage.assertDiscriminating(out);
            FontGoldens.write(golden, out, header);
            abort("golden captured to " + golden + "; a capture run verifies nothing");
        }
        FontGoldens loaded = FontGoldens.load(golden, test);
        loaded.assumeSameMachine(gate, test);
        if (inProcess) {
            requirePristineProcess();
        }
        GoldenMap out = new GoldenMap(gate);
        corpus.capture(out);
        coverage.assertDiscriminating(goldenValues(golden));
        loaded.assertSameContent(out);
    }

    /** All entries of the golden resource, including {@code machine.}, {@code info.} and {@code capture.} keys. */
    static Map<String, String> goldenValues(String golden) {
        try (InputStream in = FontGoldens.class.getResourceAsStream(golden)) {
            if (in == null) {
                throw new AssertionError("no golden file " + golden + " on the classpath");
            }
            return FontGoldens.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The header of a Linux golden: provenance, capture recipe, then {@code legend}. */
    static List<String> header(String title, List<String> legend) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add("Captured from the JNI build of commit " + CAPTURE_COMMIT + " (libjavafx_font.so,");
        lines.add("libjavafx_font_freetype.so, libjavafx_font_pango.so) before any of it was rewritten.");
        lines.add("Machine-specific: the values depend on the installed fonts, FreeType, fontconfig, Pango and");
        lines.add("HarfBuzz. The comparison runs only where every machine.* key matches; elsewhere it is skipped,");
        lines.add("or failed under -Djfx.parity.require=true.");
        lines.add("");
        lines.add("Capture, in a fresh clone of " + CAPTURE_COMMIT + " whose JNI libraries are built from source:");
        lines.add("  mvn -B -ntp -pl modules/javafx.graphics -am test -Dtest='Linux*GoldenTest'"
                + " -Dsurefire.failIfNoSpecifiedTests=false -D" + FontGoldens.CAPTURE_PROPERTY + "=true");
        lines.add("Regenerate: add -D" + FontGoldens.REGENERATE_PROPERTY + "=true (reviewed as a behaviour change)");
        lines.add("");
        lines.add("Keys:");
        lines.add("  machine.*    gate: os, library and font sha256, fontconfig inventory and configuration,");
        lines.add("               environment, locale, properties");
        lines.add("  info.*       recorded, never compared (library versions, package versions, corpus text)");
        lines.add("  capture.*    recorded, never compared (JVM, commit, JNI libraries)");
        lines.add("  #null        a null value; u:XXXX.XXXX is text as UTF-16 code units");
        lines.addAll(legend);
        return lines;
    }

    // ---------------------------------------------------------------------------------------------
    // Guards
    // ---------------------------------------------------------------------------------------------

    static final List<String> OS_FREETYPE_NATIVES = List.of(
            "FT_Outline_Decompose", "FT_Init_FreeType", "FT_Done_FreeType", "FT_Library_Version",
            "FT_Library_SetLcdFilter", "FT_New_Face", "FT_Done_Face", "FT_Set_Char_Size", "FT_Load_Glyph",
            "FT_Set_Transform", "getGlyphSlot", "getBitmapData", "isPangoEnabled", "isHarfbuzzEnabled");

    static final List<String> OS_PANGO_NATIVES = List.of(
            "pango_context_set_base_dir", "pango_ft2_font_map_new", "pango_font_map_create_context",
            "pango_font_describe", "pango_font_description_new", "pango_font_description_free",
            "pango_font_description_get_family", "pango_font_description_get_style",
            "pango_font_description_get_weight", "pango_font_description_set_family",
            "pango_font_description_set_absolute_size", "pango_font_description_set_stretch",
            "pango_font_description_set_style", "pango_font_description_set_weight", "pango_attr_list_new",
            "pango_attr_font_desc_new", "pango_attr_fallback_new", "pango_attr_list_unref", "pango_attr_list_insert",
            "pango_itemize", "pango_shape", "pango_item_free", "g_utf8_offset_to_pointer", "g_utf8_strlen",
            "g_utf16_to_utf8", "g_free", "g_list_length", "g_list_nth_data", "g_list_free", "g_object_unref",
            "FcConfigAppFontAddFile");

    static final List<String> FONT_CONFIG_MANAGER_NATIVES = List.of("getFontConfig", "populateMapsNative");

    private static final String OS_FREETYPE = "com/sun/javafx/font/freetype/OSFreetype";
    private static final String OS_PANGO = "com/sun/javafx/font/freetype/OSPango";
    private static final String FONT_CONFIG_MANAGER = "com/sun/javafx/font/FontConfigManager";

    /**
     * Every class that calls the JNI natives in the capture build, with its call sites: {@code Owner.native*count}
     * over its invoke instructions, measured on commit {@code 7b43255b30} with {@code FTFactory.registerEmbeddedFont}
     * no longer calling {@code FT_Done_Face} after a failed {@code FT_New_Face}. A shim or production caller that
     * has been re-pointed at another binding loses call sites here, while the natives and their libraries still
     * exist, so a capture from such a tree is refused.
     */
    static final Map<String, String> NATIVE_CALL_SITES = Map.of(
            "com/sun/javafx/font/freetype/OSFreetypeShim", "OSFreetype.FT_Done_Face OSFreetype.FT_Done_FreeType"
                    + " OSFreetype.FT_Init_FreeType OSFreetype.FT_Library_SetLcdFilter OSFreetype.FT_Library_Version"
                    + " OSFreetype.FT_Load_Glyph OSFreetype.FT_New_Face OSFreetype.FT_Outline_Decompose"
                    + " OSFreetype.FT_Set_Char_Size OSFreetype.FT_Set_Transform OSFreetype.getBitmapData"
                    + " OSFreetype.getGlyphSlot OSFreetype.isHarfbuzzEnabled OSFreetype.isPangoEnabled",
            "com/sun/javafx/font/freetype/OSPangoShim", "OSPango.FcConfigAppFontAddFile OSPango.g_free"
                    + " OSPango.g_list_free OSPango.g_list_length OSPango.g_list_nth_data OSPango.g_object_unref"
                    + " OSPango.g_utf16_to_utf8 OSPango.g_utf8_offset_to_pointer OSPango.g_utf8_strlen"
                    + " OSPango.pango_attr_fallback_new OSPango.pango_attr_font_desc_new OSPango.pango_attr_list_insert"
                    + " OSPango.pango_attr_list_new OSPango.pango_attr_list_unref OSPango.pango_context_set_base_dir"
                    + " OSPango.pango_font_describe OSPango.pango_font_description_free"
                    + " OSPango.pango_font_description_get_family OSPango.pango_font_description_get_style"
                    + " OSPango.pango_font_description_get_weight OSPango.pango_font_description_new"
                    + " OSPango.pango_font_description_set_absolute_size OSPango.pango_font_description_set_family"
                    + " OSPango.pango_font_description_set_stretch OSPango.pango_font_description_set_style"
                    + " OSPango.pango_font_description_set_weight OSPango.pango_font_map_create_context"
                    + " OSPango.pango_ft2_font_map_new OSPango.pango_item_free OSPango.pango_itemize"
                    + " OSPango.pango_shape",
            "com/sun/javafx/font/freetype/FTDisposer", "OSFreetype.FT_Done_Face OSFreetype.FT_Done_FreeType",
            "com/sun/javafx/font/freetype/FTFactory", "OSFreetype.FT_Done_FreeType*2 OSFreetype.FT_Init_FreeType*2"
                    + " OSFreetype.FT_Library_SetLcdFilter OSFreetype.FT_Library_Version OSFreetype.FT_New_Face"
                    + " OSFreetype.isHarfbuzzEnabled OSFreetype.isPangoEnabled*2 OSPango.FcConfigAppFontAddFile",
            "com/sun/javafx/font/freetype/FTFontFile", "OSFreetype.FT_Init_FreeType"
                    + " OSFreetype.FT_Library_SetLcdFilter OSFreetype.FT_Load_Glyph*3 OSFreetype.FT_New_Face"
                    + " OSFreetype.FT_Outline_Decompose OSFreetype.FT_Set_Char_Size*2 OSFreetype.FT_Set_Transform"
                    + " OSFreetype.getBitmapData OSFreetype.getGlyphSlot*2",
            "com/sun/javafx/font/freetype/PangoGlyphLayout", "OSPango.g_free OSPango.g_list_free"
                    + " OSPango.g_list_length OSPango.g_list_nth_data OSPango.g_object_unref OSPango.g_utf16_to_utf8"
                    + " OSPango.g_utf8_offset_to_pointer OSPango.g_utf8_strlen OSPango.pango_attr_fallback_new"
                    + " OSPango.pango_attr_font_desc_new OSPango.pango_attr_list_insert*2 OSPango.pango_attr_list_new"
                    + " OSPango.pango_attr_list_unref OSPango.pango_context_set_base_dir OSPango.pango_font_describe"
                    + " OSPango.pango_font_description_free*2 OSPango.pango_font_description_get_family"
                    + " OSPango.pango_font_description_get_style OSPango.pango_font_description_get_weight"
                    + " OSPango.pango_font_description_new OSPango.pango_font_description_set_absolute_size"
                    + " OSPango.pango_font_description_set_family OSPango.pango_font_description_set_stretch"
                    + " OSPango.pango_font_description_set_style OSPango.pango_font_description_set_weight"
                    + " OSPango.pango_font_map_create_context OSPango.pango_ft2_font_map_new OSPango.pango_item_free"
                    + " OSPango.pango_itemize OSPango.pango_shape",
            "com/sun/javafx/font/FontConfigManager", "FontConfigManager.getFontConfig"
                    + " FontConfigManager.populateMapsNative");

    /**
     * Refuses a capture from anything but the JNI build: the three JNI libraries must be on
     * {@code java.library.path}, every method the corpus reaches through the shims must still be {@code native},
     * every class of {@link #NATIVE_CALL_SITES} must still call exactly those natives, the reflective
     * {@code FontConfigManagerShim} must still name {@code FontConfigManager}'s two natives, and the observer
     * {@code LinuxFontOracleShim} must use no font class of the product. So neither a flipped tree nor stale
     * library copies can write a golden.
     */
    static void requireJniBuild() {
        for (String library : JNI_LIBRARIES) {
            if (findOnLibraryPath(library) == null) {
                fail("refusing to capture: " + library + " is not on java.library.path ("
                        + System.getProperty("java.library.path") + "). The Linux goldens record the JNI build of"
                        + " commit " + CAPTURE_COMMIT + " and are never captured from anything else.");
            }
        }
        requireNative("com.sun.javafx.font.freetype.OSFreetype", OS_FREETYPE_NATIVES);
        requireNative("com.sun.javafx.font.freetype.OSPango", OS_PANGO_NATIVES);
        requireNative("com.sun.javafx.font.FontConfigManager", FONT_CONFIG_MANAGER_NATIVES);
        for (Map.Entry<String, String> expected : new TreeMap<>(NATIVE_CALL_SITES).entrySet()) {
            String actual = nativeCallSites(classModel(expected.getKey()));
            if (!actual.equals(expected.getValue())) {
                fail("refusing to capture: " + expected.getKey() + " calls the natives [" + actual + "], but in the"
                        + " JNI build of commit " + CAPTURE_COMMIT + " it calls [" + expected.getValue() + "]: it has"
                        + " been re-pointed at another binding");
            }
        }
        ClassModel fontConfigShim = classModel("com/sun/javafx/font/FontConfigManagerShim");
        Set<String> strings = new TreeSet<>();
        for (PoolEntry entry : fontConfigShim.constantPool()) {
            if (entry instanceof StringEntry string) {
                strings.add(string.stringValue());
            }
        }
        Set<String> fontConfigShimClasses = fontClasses(fontConfigShim);
        if (!strings.containsAll(FONT_CONFIG_MANAGER_NATIVES) || !Set.of(FONT_CONFIG_MANAGER,
                FONT_CONFIG_MANAGER + "$FcCompFont", FONT_CONFIG_MANAGER + "$FontConfigFont",
                "com/sun/javafx/font/PrismFontFactory").containsAll(fontConfigShimClasses)) {
            fail("refusing to capture: FontConfigManagerShim no longer reaches " + FONT_CONFIG_MANAGER_NATIVES
                    + " of FontConfigManager alone (font classes " + fontConfigShimClasses + "): it has been"
                    + " re-pointed at another binding");
        }
        Set<String> observerClasses = fontClasses(classModel("com/sun/javafx/font/LinuxFontOracleShim"));
        if (!observerClasses.isEmpty()) {
            fail("refusing to capture: the observer LinuxFontOracleShim uses the font classes " + observerClasses
                    + "; it must never delegate to the bindings it observes");
        }
    }

    /** The pure delegate shims: every call they make into {@code com.sun.javafx.font} must be one production makes. */
    static final List<String> DELEGATE_SHIMS = List.of(
            "com/sun/javafx/font/freetype/OSFreetypeShim", "com/sun/javafx/font/freetype/OSPangoShim");

    /** The reflective shim: its string constants name the methods it invokes on the font classes it references. */
    static final String REFLECTIVE_SHIM = "com/sun/javafx/font/FontConfigManagerShim";

    /** The production classes that call the Linux font bindings. */
    static final List<String> PRODUCTION_CALLERS = List.of(
            "com/sun/javafx/font/freetype/FTFactory", "com/sun/javafx/font/freetype/FTFontFile",
            "com/sun/javafx/font/freetype/FTDisposer", "com/sun/javafx/font/freetype/PangoGlyphLayout",
            "com/sun/javafx/font/FontConfigManager");

    /**
     * Fails unless the shims reach the font layer through the entry points production reaches it through, in
     * capture and in verify mode alike. The rows the shims produce pin production only as far as both call the
     * same code: a shim re-pointed at a correct method while {@code FTFontFile} or {@code PangoGlyphLayout}
     * uses another one (an overload, a second binding class) would leave every golden green with production
     * diverging on exactly the inputs the shim rows exist for. No binding class is named here, so the check
     * survives the replacement of {@code OSFreetype}, {@code OSPango} and the {@code FontConfigManager} natives
     * and fails only when the shims and production stop sharing an entry point:
     * <ul>
     * <li>every method under {@code com.sun.javafx.font} that a delegate shim invokes (constructors and the
     * shim's own members aside), and every method of a referenced font class that the reflective shim names in
     * a string constant, is invoked by a production caller;</li>
     * <li>on the classes the delegate shims call, production invokes nothing the shims do not;</li>
     * <li>no production caller invokes a method with the name and descriptor of a shim target on another
     * class, which would be two bindings for one entry point.</li>
     * </ul>
     */
    static void requireSameBinding() {
        Set<String> shimTargets = new TreeSet<>();
        Set<String> delegateOwners = new TreeSet<>();
        for (String shim : DELEGATE_SHIMS) {
            for (String target : invokeTargets(classModel(shim), true)) {
                shimTargets.add(target);
                delegateOwners.add(owner(target));
            }
        }
        Set<String> reflective = reflectiveTargets(classModel(REFLECTIVE_SHIM));
        shimTargets.addAll(reflective);
        Set<String> productionTargets = new TreeSet<>();
        for (String caller : PRODUCTION_CALLERS) {
            productionTargets.addAll(invokeTargets(classModel(caller), false));
        }
        List<String> problems = new ArrayList<>();
        if (delegateOwners.isEmpty() || reflective.isEmpty()) {
            problems.add("the shims name no method of a font class at all (delegate targets on " + delegateOwners
                    + ", reflective targets " + reflective + ")");
        }
        for (String target : shimTargets) {
            if (!productionTargets.contains(target)) {
                problems.add("the shims call " + target + ", which no production caller calls");
            }
        }
        for (String target : productionTargets) {
            if (delegateOwners.contains(owner(target)) && !shimTargets.contains(target)) {
                problems.add("production calls " + target + ", which no shim exercises");
            }
            for (String shimTarget : shimTargets) {
                if (!owner(target).equals(owner(shimTarget)) && member(target).equals(member(shimTarget))) {
                    problems.add("production calls " + target + " while the shims call " + shimTarget
                            + ": two bindings for one entry point");
                }
            }
        }
        if (!problems.isEmpty()) {
            fail("the shims " + DELEGATE_SHIMS + " and " + REFLECTIVE_SHIM + " do not share one binding with the"
                    + " production callers " + PRODUCTION_CALLERS + ", so the goldens would not pin production:\n  "
                    + String.join("\n  ", problems));
        }
    }

    /**
     * {@code owner name(descriptor)} of every method under {@code com/sun/javafx/font/} the class invokes,
     * constructors excluded; with {@code excludeSelf}, the members of the class and its nested classes too.
     */
    static Set<String> invokeTargets(ClassModel model, boolean excludeSelf) {
        String self = model.thisClass().asInternalName();
        Set<String> targets = new TreeSet<>();
        for (MethodModel method : model.methods()) {
            method.code().ifPresent(code -> code.forEach(element -> {
                if (element instanceof InvokeInstruction invoke) {
                    String owner = invoke.owner().asInternalName();
                    String name = invoke.name().stringValue();
                    boolean own = owner.equals(self) || owner.startsWith(self + "$");
                    if (owner.startsWith("com/sun/javafx/font/") && !name.equals("<init>") && !(excludeSelf && own)) {
                        targets.add(target(owner, name, invoke.typeSymbol().descriptorString()));
                    }
                }
            }));
        }
        return targets;
    }

    /** The methods of the font classes a reflective shim references whose names are among its string constants. */
    private static Set<String> reflectiveTargets(ClassModel shim) {
        Set<String> names = new TreeSet<>();
        for (PoolEntry entry : shim.constantPool()) {
            if (entry instanceof StringEntry string) {
                names.add(string.stringValue());
            }
        }
        Set<String> targets = new TreeSet<>();
        for (String internalName : fontClasses(shim)) {
            Class<?> owner;
            try {
                owner = Class.forName(internalName.replace('/', '.'), false, PrismFontFactory.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                fail(shim.thisClass().asInternalName() + " references " + internalName + ", which does not exist", e);
                return targets;
            }
            for (Method method : owner.getDeclaredMethods()) {
                if (names.contains(method.getName())) {
                    String descriptor = MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                                                  .descriptorString();
                    targets.add(target(internalName, method.getName(), descriptor));
                }
            }
        }
        return targets;
    }

    private static String target(String owner, String name, String descriptor) {
        return owner + " " + name + descriptor;
    }

    private static String owner(String target) {
        return target.substring(0, target.indexOf(' '));
    }

    private static String member(String target) {
        return target.substring(target.indexOf(' ') + 1);
    }

    private static ClassModel classModel(String internalName) {
        try (InputStream in = PrismFontFactory.class.getModule().getResourceAsStream(internalName + ".class")) {
            if (in == null) {
                fail("no class file " + internalName + ".class in " + PrismFontFactory.class.getModule()
                        + ": the Linux font tests know the classes of commit " + CAPTURE_COMMIT + " and their"
                        + " replacements under these names");
            }
            return ClassFile.of().parse(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** {@code Owner.native} per invoke instruction of a binding native, sorted, a count above 1 as {@code *n}. */
    static String nativeCallSites(ClassModel model) {
        Map<String, Integer> counts = new TreeMap<>();
        for (MethodModel method : model.methods()) {
            method.code().ifPresent(code -> code.forEach(element -> {
                if (element instanceof InvokeInstruction invoke) {
                    String owner = invoke.owner().asInternalName();
                    String name = invoke.name().stringValue();
                    boolean bindingNative = switch (owner) {
                        case OS_FREETYPE -> OS_FREETYPE_NATIVES.contains(name);
                        case OS_PANGO -> OS_PANGO_NATIVES.contains(name);
                        case FONT_CONFIG_MANAGER -> FONT_CONFIG_MANAGER_NATIVES.contains(name);
                        default -> false;
                    };
                    if (bindingNative) {
                        counts.merge(owner.substring(owner.lastIndexOf('/') + 1) + "." + name, 1, Integer::sum);
                    }
                }
            }));
        }
        List<String> sites = new ArrayList<>();
        counts.forEach((site, count) -> sites.add(count == 1 ? site : site + "*" + count));
        return String.join(" ", sites);
    }

    /** The classes of {@code com/sun/javafx/font} and below a class file names, other than itself and its members. */
    private static Set<String> fontClasses(ClassModel model) {
        String self = model.thisClass().asInternalName();
        Set<String> classes = new TreeSet<>();
        for (PoolEntry entry : model.constantPool()) {
            if (entry instanceof ClassEntry classEntry) {
                String name = classEntry.asInternalName().replaceFirst("^\\[+L?", "").replaceFirst(";$", "");
                if (name.startsWith("com/sun/javafx/font/") && !name.equals(self) && !name.startsWith(self + "$")) {
                    classes.add(name);
                }
            }
        }
        return classes;
    }

    private static void requireNative(String className, List<String> names) {
        Class<?> owner;
        try {
            owner = Class.forName(className, false, PrismFontFactory.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            fail("refusing to capture: " + className + " does not exist, so this is not the JNI build", e);
            return;
        }
        List<Method> methods = Arrays.asList(owner.getDeclaredMethods());
        for (String name : names) {
            List<Method> matches = methods.stream().filter(m -> m.getName().equals(name)).toList();
            if (matches.isEmpty() || matches.stream().anyMatch(m -> !Modifier.isNative(m.getModifiers()))) {
                fail("refusing to capture: " + className + "." + name + " is not a native method, so this is not"
                        + " the JNI build of commit " + CAPTURE_COMMIT);
            }
        }
    }

    /**
     * Fails unless no fontconfig application font has been registered in this process and the factory has
     * loaded no embedded font: both change what every later font lookup returns.
     */
    static void requirePristineProcess() {
        int applicationFonts = LinuxFontOracleShim.fcApplicationFontCount();
        int embeddedFonts = PrismFontFactory.getFontFactory().test_getNumEmbeddedFonts();
        if (applicationFonts != 0 || embeddedFonts != 0) {
            fail("the process is not pristine: fontconfig reports " + applicationFonts + " application font(s)"
                    + " and the font factory " + embeddedFonts + " embedded font load(s). No test in the"
                    + " javafx.graphics fork may register fonts on Linux except in a child JVM; the Linux font"
                    + " goldens depend on the unmodified fontconfig configuration.");
        }
    }

    /**
     * {@code HOME} in the C environment when this class was initialised, read through libc rather than taken
     * from the JDK's start-up snapshot: {@link #captureOrVerify} initialises this class before it runs a
     * corpus, so for the gated tests this is the value before any of their fontconfig calls.
     */
    private static final String LIBC_HOME_AT_LOAD = LinuxFontOracleShim.libcGetenv("HOME");

    /**
     * With {@code HOME} set, the C environment's {@code HOME} still equals what libc answered when this class
     * was initialised, after the fontconfig calls made since. {@code fontpath_linux.c} set {@code HOME=} only
     * when {@code getenv("HOME")} was NULL; a binding that set it on every call would change, for the rest of
     * the process, where every later fontconfig initialisation looks for the user's configuration and cache.
     * The HOME-unset case is the child JVM {@code p2.homeUnset} of {@link LinuxFontProcessGoldenTest}.
     */
    static void assertLibcHomeUnchanged() {
        if (LIBC_HOME_AT_LOAD == null) {
            return;
        }
        String libcHome = LinuxFontOracleShim.libcGetenv("HOME");
        if (!LIBC_HOME_AT_LOAD.equals(libcHome)) {
            fail("HOME in the C environment is " + safeText(libcHome) + " after the fontconfig calls, but it was "
                    + safeText(LIBC_HOME_AT_LOAD) + " before them: a fontconfig binding changed HOME although it"
                    + " was set");
        }
    }

    static Path findOnLibraryPath(String library) {
        String path = System.getProperty("java.library.path", "");
        for (String entry : path.split(File.pathSeparator)) {
            if (!entry.isEmpty() && Files.isRegularFile(Path.of(entry, library))) {
                return Path.of(entry, library);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Machine gate
    // ---------------------------------------------------------------------------------------------

    /**
     * The {@code machine.} keys, computed once per JVM from files, properties and read-only commands, never
     * from a native under test, so that they are the same for every build of the font layer.
     */
    static synchronized Map<String, String> machine() {
        if (machine == null) {
            TreeMap<String, String> keys = new TreeMap<>();
            keys.put("machine.os.name", System.getProperty("os.name"));
            keys.put("machine.os.arch", System.getProperty("os.arch"));
            keys.put("machine.os.release", osRelease());
            Map<String, Path> libraries = resolveLibraries();
            for (String soname : SONAMES) {
                Path library = libraries.get(soname);
                keys.put("machine.lib." + soname + ".sha256",
                         library == null ? FontGoldens.NULL : FontGoldens.sha256Hex(library));
            }
            for (Pinned pinned : PINNED) {
                Path file = moduleDirectory().resolve(pinned.path()).normalize();
                keys.put("machine.font." + pinned.key() + ".sha256",
                         Files.isRegularFile(file) ? FontGoldens.sha256Hex(file) : FontGoldens.NULL);
            }
            putFontconfigInventory(keys);
            keys.put("machine.fontconfig.conf", fontconfigConfiguration());
            List<String> environment = new ArrayList<>();
            for (String name : ENVIRONMENT) {
                environment.add(name + "=" + FontGoldens.nullable(System.getenv(name)));
            }
            keys.put("machine.env", FontGoldens.joinList(environment));
            keys.put("machine.locale", FontGoldens.joinList(List.of(Locale.getDefault().toLanguageTag(),
                    FontGoldens.nullable(System.getProperty("file.encoding")),
                    FontGoldens.nullable(System.getProperty("sun.jnu.encoding")))));
            List<String> properties = new ArrayList<>();
            for (String name : FONT_PROPERTIES) {
                properties.add(name + "=" + FontGoldens.nullable(System.getProperty(name)));
            }
            keys.put("machine.props", FontGoldens.joinList(properties));
            keys.put("machine.jdk.fontdir", FontGoldens.directoryFingerprint(
                    Path.of(System.getProperty("java.home"), "lib", "fonts")));
            machine = Collections.unmodifiableMap(keys);
        }
        return machine;
    }

    private static String osRelease() {
        Path file = Path.of("/etc/os-release");
        if (!Files.isRegularFile(file)) {
            return FontGoldens.NULL;
        }
        String id = FontGoldens.NULL;
        String version = FontGoldens.NULL;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.startsWith("ID=")) {
                    id = unquote(line.substring(3));
                } else if (line.startsWith("VERSION_ID=")) {
                    version = unquote(line.substring(11));
                }
            }
        } catch (IOException e) {
            return "unreadable (" + e + ")";
        }
        return id + " " + version;
    }

    private static String unquote(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    /** soname to real path, from {@code ldconfig -p} for this architecture, else the multiarch directory. */
    static Map<String, Path> resolveLibraries() {
        String arch = System.getProperty("os.arch");
        String tag = "aarch64".equals(arch) ? "AArch64" : "x86-64";
        String triplet = "aarch64".equals(arch) ? "aarch64-linux-gnu" : "x86_64-linux-gnu";
        String cache = null;
        for (String ldconfig : List.of("/usr/sbin/ldconfig", "/sbin/ldconfig")) {
            if (Files.isExecutable(Path.of(ldconfig))) {
                cache = run(List.of(ldconfig, "-p"), true);
                if (cache != null) {
                    break;
                }
            }
        }
        Map<String, Path> resolved = new TreeMap<>();
        for (String soname : SONAMES) {
            Path candidate = null;
            if (cache != null) {
                for (String line : cache.split("\n")) {
                    String trimmed = line.trim();
                    int arrow = trimmed.indexOf(" => ");
                    if (arrow > 0 && trimmed.startsWith(soname + " (") && trimmed.contains(tag)) {
                        candidate = Path.of(trimmed.substring(arrow + 4).trim());
                        break;
                    }
                }
            }
            if (candidate == null) {
                candidate = Path.of("/usr/lib", triplet, soname);
            }
            try {
                if (Files.exists(candidate)) {
                    resolved.put(soname, candidate.toRealPath());
                }
            } catch (IOException e) {
                // unresolved: gated as #null
            }
        }
        return resolved;
    }

    static final String FC_LIST_FORMAT = "%{file}\\t%{index}\\t%{family}\\t%{familylang}\\t%{style}"
            + "\\t%{fullname}\\t%{fullnamelang}\\t%{fontformat}\\n";

    /** {@code machine.fontconfig.list} (the listing, in fontconfig's order) and {@code .files} (their content). */
    private static void putFontconfigInventory(Map<String, String> keys) {
        byte[] listing = runBytes(List.of("fc-list", "--format=" + FC_LIST_FORMAT), true);
        if (listing == null) {
            keys.put("machine.fontconfig.list", FontGoldens.NULL);
            keys.put("machine.fontconfig.files", FontGoldens.NULL);
            return;
        }
        String text = new String(listing, StandardCharsets.UTF_8);
        int lines = 0;
        TreeSet<String> files = new TreeSet<>();
        for (String line : text.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            lines++;
            int tab = line.indexOf('\t');
            files.add(tab < 0 ? line : line.substring(0, tab));
        }
        keys.put("machine.fontconfig.list", lines + ":" + FontGoldens.sha256Hex(listing));
        TreeSet<String> contents = new TreeSet<>();
        for (String file : files) {
            try {
                Path real = Path.of(file).toRealPath();
                contents.add(real + "\t" + FontGoldens.sha256Hex(real));
            } catch (IOException | RuntimeException e) {
                contents.add(file + "\tunreadable");
            }
        }
        byte[] joined = String.join("\n", contents).getBytes(StandardCharsets.UTF_8);
        keys.put("machine.fontconfig.files", contents.size() + ":" + FontGoldens.sha256Hex(joined));
    }

    /** sha256 of every file under {@code /etc/fonts} (links followed) plus the per-user configuration paths. */
    private static String fontconfigConfiguration() {
        TreeSet<String> entries = new TreeSet<>();
        Path system = Path.of("/etc/fonts");
        entries.add("/etc/fonts " + treeFingerprint(system, true));
        String home = System.getenv("HOME");
        String configHome = System.getenv("XDG_CONFIG_HOME");
        String dataHome = System.getenv("XDG_DATA_HOME");
        if (home != null) {
            Path config = configHome != null ? Path.of(configHome) : Path.of(home, ".config");
            Path data = dataHome != null ? Path.of(dataHome) : Path.of(home, ".local", "share");
            entries.add("config/fontconfig " + treeFingerprint(config.resolve("fontconfig"), true));
            entries.add("~/.fonts.conf " + treeFingerprint(Path.of(home, ".fonts.conf"), true));
            entries.add("~/.fonts " + treeFingerprint(Path.of(home, ".fonts"), false));
            entries.add("data/fonts " + treeFingerprint(data.resolve("fonts"), false));
        } else {
            entries.add("HOME unset");
        }
        return FontGoldens.sha256Hex(String.join("\n", entries).getBytes(StandardCharsets.UTF_8));
    }

    /** {@code #null} when absent; otherwise count and sha256 of relative path plus content hash (or size). */
    private static String treeFingerprint(Path root, boolean hashContent) {
        if (!Files.exists(root)) {
            return FontGoldens.NULL;
        }
        TreeSet<String> lines = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String relative = root.relativize(file).toString();
                    lines.add(relative + "\t" + (hashContent ? FontGoldens.sha256Hex(file) : Files.size(file)));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | RuntimeException e) {
            return "unreadable (" + e.getClass().getSimpleName() + ")";
        }
        return lines.size() + ":" + FontGoldens.sha256Hex(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    /** {@code info.} and {@code capture.} keys: recorded for the reader, never compared. */
    static void putCaptureInfo(GoldenMap out) {
        out.put("capture.provenance", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")
                + " on " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        out.put("capture.commit", gitCommit());
        List<String> libraries = new ArrayList<>();
        for (String library : JNI_LIBRARIES) {
            Path file = findOnLibraryPath(library);
            libraries.add(library + ":" + (file == null ? FontGoldens.NULL : sizeOf(file) + ":" + md5Hex(file)));
        }
        out.put("capture.jniLibraries", FontGoldens.joinList(libraries));
        out.put("info.os.version", System.getProperty("os.version"));
        out.put("info.java.version", System.getProperty("java.runtime.version"));
        Map<String, Path> resolved = resolveLibraries();
        for (String soname : SONAMES) {
            Path library = resolved.get(soname);
            out.put("info.lib." + soname + ".realpath", library == null ? FontGoldens.NULL : library.toString());
        }
        out.put("info.freetype.version", describe(LinuxFontOracleShim::freetypeVersion));
        out.put("info.pango.version", describe(LinuxFontOracleShim::pangoVersion));
        out.put("info.harfbuzz.version", describe(LinuxFontOracleShim::harfbuzzVersion));
        out.put("info.fontconfig.version", describe(() -> Integer.toString(LinuxFontOracleShim.fcVersion())));
        for (Map.Entry<String, String> entry : packageVersions(resolved.values()).entrySet()) {
            out.put("info.dpkg." + entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < TEXTS.size(); i++) {
            out.put("info.corpus.s" + pad(i, 2), unitsHex(TEXTS.get(i)));
        }
    }

    private interface Describer {
        String get() throws Exception;
    }

    private static String describe(Describer describer) {
        try {
            return describer.get();
        } catch (Throwable t) {
            return "unavailable (" + t.getClass().getSimpleName() + ")";
        }
    }

    /** {@code dpkg -S} of the libraries and pinned fonts, then {@code dpkg-query} of those packages. */
    private static Map<String, String> packageVersions(java.util.Collection<Path> libraries) {
        List<String> paths = new ArrayList<>();
        libraries.forEach(p -> paths.add(p.toString()));
        for (Pinned pinned : PINNED) {
            if (pinned.path().isAbsolute() && Files.isRegularFile(pinned.path())) {
                paths.add(pinned.path().toString());
            }
        }
        Map<String, String> versions = new TreeMap<>();
        List<String> command = new ArrayList<>(List.of("dpkg", "-S"));
        command.addAll(paths);
        String owners = run(command, false);
        if (owners == null) {
            return versions;
        }
        Set<String> packages = new LinkedHashSet<>();
        for (String line : owners.split("\n")) {
            int colon = line.indexOf(": ");
            if (colon > 0) {
                for (String owner : line.substring(0, colon).split(", ")) {
                    packages.add(owner.trim());
                }
            }
        }
        if (packages.isEmpty()) {
            return versions;
        }
        List<String> query = new ArrayList<>(List.of("dpkg-query", "-W", "-f=${binary:Package} ${Version}\\n"));
        query.addAll(packages);
        String listed = run(query, true);
        if (listed != null) {
            for (String line : listed.split("\n")) {
                int space = line.indexOf(' ');
                if (space > 0) {
                    versions.put(line.substring(0, space), line.substring(space + 1));
                }
            }
        }
        return versions;
    }

    private static String gitCommit() {
        String commit = run(List.of("git", "-C", repositoryDirectory().toString(), "log", "-1", "--format=%H"), true);
        return commit == null ? "unknown" : commit.trim();
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return -1;
        }
    }

    private static String md5Hex(Path file) {
        try {
            return HEX.formatHex(java.security.MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file)));
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            return "unreadable";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Child JVMs
    // ---------------------------------------------------------------------------------------------

    /** The outcome of a child JVM run. */
    record ChildRun(String scenario, int exitCode, Path output, Path stdout, Path stderr, List<Path> errorFiles,
                    List<String> command) {

        String describe() {
            return "child JVM '" + scenario + "' exited with " + exitCode + "; hs_err files: " + errorFiles
                    + "; stdout " + stdout + "; last stderr lines:\n" + tail(stderr, 40) + "\ncommand: " + command;
        }
    }

    /**
     * Runs {@link LinuxFontProcessChild} with {@code scenario} in a new JVM that has this fork's module and
     * native options, the font properties of {@link #FONT_PROPERTIES}, {@code LANG=C.UTF-8} and none of
     * {@link #CHILD_CLEARED_ENVIRONMENT}; then {@code envRemove} is removed and {@code envSet} applied. Fatal
     * error logs go to the scenario's own directory, never to the module directory.
     */
    static ChildRun launchChild(String scenario, Map<String, String> envSet, Set<String> envRemove,
                                List<String> jvmOptions, long timeoutSeconds) throws IOException, InterruptedException {
        Path work = workDirectory();
        Path output = work.resolve(scenario + ".txt");
        Path stdout = work.resolve(scenario + ".out");
        Path stderr = work.resolve(scenario + ".err");
        Path errors = work.resolve("hs_err").resolve(scenario);
        Files.deleteIfExists(output);
        deleteTree(errors);
        Files.createDirectories(errors);

        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                    || argument.startsWith("-Xrunjdwp")
                    || argument.startsWith("-Xdebug") || argument.startsWith("-XX:ErrorFile")) {
                continue;
            }
            command.add(argument);
        }
        for (String name : FONT_PROPERTIES) {
            String value = System.getProperty(name);
            if (value != null) {
                command.add("-D" + name + "=" + value);
            }
        }
        command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
        command.addAll(jvmOptions);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(LinuxFontProcessChild.class.getName());
        command.add(scenario);
        command.add(output.toString());

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(moduleDirectory().toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile());
        Map<String, String> environment = builder.environment();
        environment.put("LANG", "C.UTF-8");
        CHILD_CLEARED_ENVIRONMENT.forEach(environment::remove);
        envRemove.forEach(environment::remove);
        environment.putAll(envSet);
        Process process = builder.start();
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("child JVM '" + scenario + "' did not finish within " + timeoutSeconds + " s; stderr:\n"
                    + tail(stderr, 40));
        }
        List<Path> errorFiles;
        try (Stream<Path> files = Files.list(errors)) {
            errorFiles = files.sorted().toList();
        }
        return new ChildRun(scenario, process.exitValue(), output, stdout, stderr, errorFiles, command);
    }

    /** {@link #launchChild} that must succeed; returns the child's golden entries. */
    static Map<String, String> runChild(String scenario, Map<String, String> envSet, Set<String> envRemove)
            throws IOException, InterruptedException {
        ChildRun run = launchChild(scenario, envSet, envRemove, List.of(), 90);
        if (run.exitCode() != 0 || !run.errorFiles().isEmpty() || !Files.isRegularFile(run.output())) {
            fail(run.describe());
        }
        return FontGoldens.parse(Files.readString(run.output(), StandardCharsets.UTF_8));
    }

    static String tail(Path file, int lines) {
        try {
            List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
            return String.join("\n", all.subList(Math.max(0, all.size() - lines), all.size()));
        } catch (IOException | UncheckedIOException e) {
            return "(unreadable: " + e + ")";
        }
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Collections.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Paths
    // ---------------------------------------------------------------------------------------------

    /** Surefire runs with the module directory as its working directory; checked rather than assumed. */
    static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))
                || !Files.isDirectory(directory.resolve("src/test/resources"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    static Path repositoryDirectory() {
        return moduleDirectory().resolve("../..").normalize();
    }

    static Path workDirectory() throws IOException {
        Path work = moduleDirectory().resolve(WORK_DIRECTORY);
        Files.createDirectories(work);
        return work;
    }

    static Path pinned(String key) {
        for (Pinned pinned : PINNED) {
            if (pinned.key().equals(key)) {
                return moduleDirectory().resolve(pinned.path()).normalize();
            }
        }
        throw new IllegalArgumentException("no pinned font " + key);
    }

    // ---------------------------------------------------------------------------------------------
    // Formatting
    // ---------------------------------------------------------------------------------------------

    static String pad(int value, int width) {
        String digits = Integer.toString(Math.abs(value));
        StringBuilder text = new StringBuilder(value < 0 ? "m" : "");
        for (int i = digits.length(); i < width; i++) {
            text.append('0');
        }
        return text.append(digits).toString();
    }

    /** {@code g} and the glyph id, zero-padded; negative ids as {@code gm00001}. */
    static String gid(int glyph) {
        return "g" + pad(glyph, 5);
    }

    /** UTF-16 code units as upper-case hex joined by {@code .}; {@code #null} for null. */
    static String unitsHex(String text) {
        if (text == null) {
            return FontGoldens.NULL;
        }
        StringBuilder hex = new StringBuilder(text.length() * 5);
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                hex.append('.');
            }
            hex.append(String.format(Locale.ROOT, "%04X", (int) text.charAt(i)));
        }
        return hex.toString();
    }

    /** The inverse of {@link #unitsHex}. */
    static String fromUnitsHex(String hex) {
        if (FontGoldens.NULL.equals(hex)) {
            return null;
        }
        if (hex.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String unit : hex.split("\\.")) {
            text.append((char) Integer.parseInt(unit, 16));
        }
        return text.toString();
    }

    static String bytesHex(byte[] bytes) {
        return bytes == null ? FontGoldens.NULL : HEX.formatHex(bytes);
    }

    static byte[] parseBytesHex(String hex) {
        return FontGoldens.NULL.equals(hex) ? null : HEX.parseHex(hex);
    }

    /**
     * Plain text when every character is printable ASCII other than the golden's separators, otherwise
     * {@code u:} and the code units: a lone surrogate or a C1 control written as text would not survive UTF-8.
     */
    static String safeText(String text) {
        if (text == null) {
            return FontGoldens.NULL;
        }
        boolean plain = !text.startsWith("u:") && !text.startsWith("#");
        for (int i = 0; plain && i < text.length(); i++) {
            char c = text.charAt(i);
            plain = c >= 0x20 && c <= 0x7E && c != '|' && c != ';' && c != '\\';
        }
        return plain ? text : "u:" + unitsHex(text);
    }

    static String joinFloats(float... values) {
        return FontGoldens.joinFloats(values, values.length);
    }

    static String joinInts(int[] values) {
        return values == null ? FontGoldens.NULL : FontGoldens.joinInts(values, values.length);
    }

    static String joinLongs(long[] values) {
        if (values == null) {
            return FontGoldens.NULL;
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                joined.append(';');
            }
            joined.append(values[i]);
        }
        return joined.toString();
    }

    static String bitmapSummary(byte[] bitmap) {
        return bitmap == null ? FontGoldens.NULL : bitmap.length + ";" + FontGoldens.sha256Hex(bitmap);
    }

    /** Hex rows of {@code rowBytes} bytes separated by {@code /}. */
    static String bitmapDump(byte[] bitmap, int rowBytes) {
        if (bitmap == null) {
            return FontGoldens.NULL;
        }
        if (rowBytes <= 0) {
            return HEX.formatHex(bitmap);
        }
        StringBuilder dump = new StringBuilder(bitmap.length * 3);
        for (int row = 0; row * rowBytes < bitmap.length; row++) {
            if (row > 0) {
                dump.append('/');
            }
            int start = row * rowBytes;
            dump.append(HEX.formatHex(bitmap, start, Math.min(bitmap.length, start + rowBytes)));
        }
        return dump.toString();
    }

    /**
     * {@code numCommands;commandsArrayLength;coordsIterated;coordsArrayLength;winding;sha256}, the hash over one
     * line per segment, {@code <type>} then {@code  %08x} of {@link Float#floatToRawIntBits} per coordinate.
     */
    static String outlineSummary(Path2D path) {
        if (path == null) {
            return FontGoldens.NULL;
        }
        StringBuilder stream = new StringBuilder();
        int iterated = iterate(path, (type, coords, count) -> {
            stream.append(type);
            for (int i = 0; i < count; i++) {
                stream.append(' ').append(String.format(Locale.ROOT, "%08x", Float.floatToRawIntBits(coords[i])));
            }
            stream.append('\n');
        });
        return path.getNumCommands() + ";" + path.getCommandsNoClone().length + ";" + iterated + ";"
                + path.getFloatCoordsNoClone().length + ";" + path.getWindingRule() + ";"
                + FontGoldens.sha256Hex(stream.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Segment types joined by {@code ;}, then {@code |}, then every iterated coordinate as {@link Float#toString}. */
    static String outlineDump(Path2D path) {
        if (path == null) {
            return FontGoldens.NULL;
        }
        List<String> types = new ArrayList<>();
        List<String> coordinates = new ArrayList<>();
        iterate(path, (type, coords, count) -> {
            types.add(Integer.toString(type));
            for (int i = 0; i < count; i++) {
                coordinates.add(Float.toString(coords[i]));
            }
        });
        return String.join(";", types) + "|" + String.join(";", coordinates);
    }

    /** {@link #outlineSummary} plus {@code ;} and the four bounds, for a glyph shape. */
    static String shapeSummary(Shape shape) {
        if (shape == null) {
            return FontGoldens.NULL;
        }
        if (!(shape instanceof Path2D path)) {
            return "class " + shape.getClass().getName();
        }
        RectBounds bounds = path.getBounds();
        return outlineSummary(path) + ";" + joinFloats(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(),
                                                        bounds.getMaxY());
    }

    private interface SegmentSink {
        void segment(int type, float[] coords, int count);
    }

    private static int iterate(Path2D path, SegmentSink sink) {
        PathIterator iterator = path.getPathIterator(BaseTransform.IDENTITY_TRANSFORM);
        float[] coords = new float[6];
        int total = 0;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            int count = switch (type) {
                case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> 2;
                case PathIterator.SEG_QUADTO -> 4;
                case PathIterator.SEG_CUBICTO -> 6;
                default -> 0;
            };
            sink.segment(type, coords, count);
            total += count;
            iterator.next();
        }
        return total;
    }

    /**
     * One {@link PrismTextLayout} (no string cache) over {@code text}: {@code prefix} gets
     * {@code bounds|lines|runs}, and {@code prefix.rNN} per run
     * {@code start;length;level;script;slot;complex;ltr;glyphCount;width;height;ascent;descent;leading|codes|
     * charOffsets|posX|posY}, codes as hex.
     */
    static void putLayout(GoldenMap out, String prefix, PGFont font, String text, boolean rtl) {
        PrismTextLayout layout = new PrismTextLayout(0);
        if (rtl) {
            layout.setDirection(TextLayout.DIRECTION_RTL);
        }
        layout.setContent(text, font);
        GlyphList[] runs = layout.getRuns();
        BaseBounds bounds = layout.getBounds();
        out.put(prefix, joinFloats(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY())
                + "|" + layout.getLines().length + "|" + runs.length);
        for (int r = 0; r < runs.length; r++) {
            GlyphList run = runs[r];
            int count = run.getGlyphCount();
            StringBuilder value = new StringBuilder();
            value.append(run.getStart());
            if (run instanceof TextRun textRun) {
                value.append(';').append(textRun.getLength()).append(';').append(textRun.getLevel())
                     .append(';').append(textRun.getScript()).append(';').append(textRun.getSlot());
            } else {
                value.append(";#null;#null;#null;#null");
            }
            value.append(';').append(run.isComplex());
            if (run instanceof TextRun textRun) {
                value.append(';').append(textRun.isLeftToRight());
            } else {
                value.append(";#null");
            }
            value.append(';').append(count).append(';').append(Float.toString(run.getWidth()))
                 .append(';').append(Float.toString(run.getHeight()));
            if (run instanceof TextRun textRun) {
                value.append(';').append(Float.toString(textRun.getAscent()))
                     .append(';').append(Float.toString(textRun.getDescent()))
                     .append(';').append(Float.toString(textRun.getLeading()));
            } else {
                value.append(";#null;#null;#null");
            }
            List<String> codes = new ArrayList<>();
            List<String> offsets = new ArrayList<>();
            List<String> posX = new ArrayList<>();
            List<String> posY = new ArrayList<>();
            for (int g = 0; g < count; g++) {
                codes.add(Integer.toHexString(run.getGlyphCode(g)));
                offsets.add(Integer.toString(run.getCharOffset(g)));
            }
            if (count > 0) {
                for (int g = 0; g <= count; g++) {
                    posX.add(Float.toString(run.getPosX(g)));
                    posY.add(Float.toString(run.getPosY(g)));
                }
            }
            value.append('|').append(String.join(";", codes)).append('|').append(String.join(";", offsets))
                 .append('|').append(String.join(";", posX)).append('|').append(String.join(";", posY));
            out.put(prefix + ".r" + pad(r, 2), value.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // fontconfig results
    // ---------------------------------------------------------------------------------------------

    /**
     * A raw {@code getFontConfig} result: {@code prefix.return}; per element {@code .eNN.name}, {@code .eNN.first}
     * (index of {@code firstFont} in {@code allFonts} by identity, {@code #null}, or {@code notInAllFonts}),
     * {@code .eNN.firstFont} (its fields), {@code .eNN.count} ({@code allFonts.length} or {@code #null}) and
     * {@code .eNN.fMMM} per font: family, style, full name and file, each through {@code text}.
     */
    static void putFontConfig(GoldenMap out, String prefix, boolean result, FontConfigManager.FcCompFont[] fonts,
                              UnaryOperator<String> text) {
        out.put(prefix + ".return", Boolean.toString(result));
        for (int e = 0; e < fonts.length; e++) {
            FontConfigManager.FcCompFont font = fonts[e];
            String element = prefix + ".e" + pad(e, 2);
            out.put(element + ".name", text.apply(font.fcName));
            FontConfigManager.FontConfigFont[] all = font.allFonts;
            String first = FontGoldens.NULL;
            if (font.firstFont != null) {
                first = "notInAllFonts";
                for (int f = 0; all != null && f < all.length; f++) {
                    if (all[f] == font.firstFont) {
                        first = Integer.toString(f);
                        break;
                    }
                }
            }
            out.put(element + ".first", first);
            out.put(element + ".firstFont", fontFields(font.firstFont, text));
            out.put(element + ".count", all == null ? FontGoldens.NULL : Integer.toString(all.length));
            for (int f = 0; all != null && f < all.length; f++) {
                out.put(element + ".f" + pad(f, 3), fontFields(all[f], text));
            }
        }
    }

    static String fontFields(FontConfigManager.FontConfigFont font, UnaryOperator<String> text) {
        if (font == null) {
            return FontGoldens.NULL;
        }
        return FontGoldens.joinList(List.of(text.apply(font.familyName), text.apply(font.styleStr),
                                            text.apply(font.fullName), text.apply(font.fontFile)));
    }

    /** The three maps {@code populateMapsNative} fills. */
    record FontMaps(boolean result, HashMap<String, String> fontToFile, HashMap<String, String> fontToFamily,
                    HashMap<String, ArrayList<String>> familyToFontList) {

        static FontMaps populate(Locale locale) {
            HashMap<String, String> fontToFile = new HashMap<>();
            HashMap<String, String> fontToFamily = new HashMap<>();
            HashMap<String, ArrayList<String>> familyToFontList = new HashMap<>();
            boolean result = FontConfigManagerShim.populateMapsNative(fontToFile, fontToFamily, familyToFontList,
                                                                     locale);
            return new FontMaps(result, fontToFile, fontToFamily, familyToFontList);
        }

        String counts() {
            return fontToFile.size() + ";" + fontToFamily.size() + ";" + familyToFontList.size();
        }

        /** Every entry as {@code file.<key>}, {@code family.<key>}, {@code list.<key>} (list in insertion order). */
        TreeMap<String, String> entries(UnaryOperator<String> key, UnaryOperator<String> text) {
            TreeMap<String, String> entries = new TreeMap<>();
            fontToFile.forEach((k, v) -> entries.put("file." + key.apply(k), text.apply(v)));
            fontToFamily.forEach((k, v) -> entries.put("family." + key.apply(k), text.apply(v)));
            familyToFontList.forEach((k, v) -> {
                List<String> values = new ArrayList<>();
                v.forEach(name -> values.add(text.apply(name)));
                entries.put("list." + key.apply(k), FontGoldens.joinList(values));
            });
            return entries;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------------------------------

    static String run(List<String> command, boolean requireSuccess) {
        byte[] output = runBytes(command, requireSuccess);
        return output == null ? null : new String(output, StandardCharsets.UTF_8);
    }

    /** stdout of a read-only command, or {@code null} if it cannot run (or, when required, exits non-zero). */
    static byte[] runBytes(List<String> command, boolean requireSuccess) {
        try {
            Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            return !requireSuccess || process.exitValue() == 0 ? output : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
