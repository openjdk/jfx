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

import com.sun.javafx.font.FontFallbackInfo;
import com.sun.javafx.font.LogicalFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFactoryShim;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * Parity oracle for the Windows font enumeration that {@code fontpath.c} performs for
 * {@link PrismFontFactory}, against goldens captured from the JNI build.
 * <p>
 * The golden {@value #GOLDEN} records, for this machine, everything the Windows natives feed into the
 * factory: the font directory string, the three raw maps {@code populateFontFileNameMap} fills from GDI
 * and the registry (before {@code resolveWindowsFonts} touches them), the {@code FontLink\SystemLink}
 * registry values, the EUDC file, the LCD contrast, the system LCID, the system font and its size, the
 * public name lists the factory derives from the maps, and the fallback list {@code DWFactory} builds.
 * <p>
 * It is machine-specific by nature - it lists the fonts installed here - so the comparison runs only when
 * the {@code machine.} header matches this machine (OS, architecture, fingerprints of the system and user
 * font directories); anywhere else it is skipped with the capture command - unless the run says
 * {@code -Djfx.parity.require=true}, which turns that skip into a failure on the machine that owns the golden
 * ({@link ParityGate}). Capture with {@code -Djfx.font.golden.capture=true}; see {@link FontGoldens}.
 */
@EnabledOnOs(OS.WINDOWS)
public class FontEnumerationGoldenTest {

    static final String GOLDEN = "windows-fonts-golden.txt";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(FontEnumerationGoldenTest.class);

    /** Names whose FontLink entries are read: the one DWFactory uses, common CJK/UI link sources, a miss. */
    private static final String[] FONT_LINK_NAMES = {
        "Tahoma", "Segoe UI", "Microsoft Sans Serif", "Arial", "Lucida Sans Unicode", "MS UI Gothic", "SimSun",
        "No Such Font (jfx golden)"
    };

    /** Bare file names resolved through getPathNameWindows: the two DWFactory hardcodes plus common ones. */
    private static final String[] PATH_NAME_FILES = {
        "arial.ttf", "ARIAL.TTF", "segoeui.ttf", "times.ttf", "consola.ttf", "mingliub.ttc", "seguisym.ttf",
        "no-such-file.ttf"
    };

    @Test
    public void windowsFontEnumerationMatchesTheJniGolden() throws IOException {
        Map<String, String> captured = capture();
        if (FontGoldens.captureRequested()) {
            FontGoldens.write(GOLDEN, captured, header());
            abort("golden captured to " + GOLDEN + "; a capture run verifies nothing");
        }
        FontGoldens golden = FontGoldens.load(GOLDEN, FontEnumerationGoldenTest.class);
        golden.assumeSameMachine(captured, FontEnumerationGoldenTest.class);
        golden.assertSameContent(captured);
    }

    /** The machine gate passed at least once, so at least one body key was compared ({@link ParityGate}). */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /** The oracle must not be vacuous: the raw maps have to describe a real font set. */
    @Test
    public void rawEnumerationIsNotEmpty() {
        HashMap<String, String> fontToFile = new HashMap<>();
        HashMap<String, String> fontToFamily = new HashMap<>();
        HashMap<String, ArrayList<String>> familyToFontList = new HashMap<>();
        PrismFontFactoryShim.populateFontFileNameMap(fontToFile, fontToFamily, familyToFontList, Locale.ENGLISH);
        assertTrue(fontToFile.size() > 10, "registry Fonts key yielded " + fontToFile.size() + " entries");
        assertTrue(fontToFamily.size() > 10, "GDI enumeration yielded " + fontToFamily.size() + " faces");
        assertTrue(familyToFontList.size() > 5, "GDI enumeration yielded " + familyToFontList.size() + " families");
        assertTrue(fontToFile.containsKey("arial"), "arial missing from the registry map: " + fontToFile.keySet());
        assertTrue(familyToFontList.containsKey("arial"), "arial missing from GDI families");
        String fontPath = PrismFontFactoryShim.getFontPath();
        assertNotNull(fontPath, "getFontPath");
        assertFalse(fontPath.isBlank(), "getFontPath");
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    static Map<String, String> capture() {
        Map<String, String> out = new LinkedHashMap<>();
        FontGoldens.putOperatingSystem(out);
        Path systemFonts = FontGoldens.systemFontDirectory();
        Path userFonts = FontGoldens.userFontDirectory();
        out.put("machine.font.dir", systemFonts.toString());
        out.put("machine.font.dir.fingerprint", FontGoldens.directoryFingerprint(systemFonts));
        out.put("machine.user.font.dir", userFonts == null ? FontGoldens.NULL : userFonts.toString());
        out.put("machine.user.font.dir.fingerprint", FontGoldens.directoryFingerprint(userFonts));

        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        assertNotNull(factory, "PrismFontFactory.getFontFactory()");
        out.put("info.factory", factory.getClass().getName());

        out.put("fontPath", FontGoldens.nullable(PrismFontFactoryShim.getFontPath()));
        for (String file : PATH_NAME_FILES) {
            out.put("pathName." + file, FontGoldens.nullable(PrismFontFactoryShim.getPathNameWindows(file)));
        }

        captureRawMaps(out);

        for (String name : FONT_LINK_NAMES) {
            String link = PrismFontFactoryShim.regReadFontLink(name);
            out.put("fontLink." + name, FontGoldens.nullable(link));
            out.put("fontLink." + name + ".length", link == null ? FontGoldens.NULL : Integer.toString(link.length()));
        }
        out.put("eudcFontFile", FontGoldens.nullable(PrismFontFactoryShim.getEUDCFontFile()));
        out.put("lcdContrastWin32", Integer.toString(PrismFontFactoryShim.getLCDContrastWin32()));
        out.put("lcdContrast", Float.toString(PrismFontFactory.getLCDContrast()));
        out.put("systemLCID", Short.toString(PrismFontFactoryShim.getSystemLCID()));
        out.put("systemFontSize", Float.toString(PrismFontFactory.getSystemFontSize()));
        out.put("systemFont", FontGoldens.nullable(PrismFontFactory.getSystemFont(LogicalFont.SYSTEM)));

        capturePublicViews(out, factory);
        captureFallbacks(out, factory);
        return out;
    }

    /** The maps exactly as the native fills them: fresh instances, Locale.ENGLISH as production passes. */
    private static void captureRawMaps(Map<String, String> out) {
        HashMap<String, String> fontToFile = new HashMap<>();
        HashMap<String, String> fontToFamily = new HashMap<>();
        HashMap<String, ArrayList<String>> familyToFontList = new HashMap<>();
        PrismFontFactoryShim.populateFontFileNameMap(fontToFile, fontToFamily, familyToFontList, Locale.ENGLISH);

        out.put("raw.fontToFile.count", Integer.toString(fontToFile.size()));
        for (Map.Entry<String, String> entry : new TreeMap<>(fontToFile).entrySet()) {
            out.put("raw.fontToFile." + entry.getKey(), FontGoldens.nullable(entry.getValue()));
        }
        out.put("raw.fontToFamily.count", Integer.toString(fontToFamily.size()));
        for (Map.Entry<String, String> entry : new TreeMap<>(fontToFamily).entrySet()) {
            out.put("raw.fontToFamily." + entry.getKey(), FontGoldens.nullable(entry.getValue()));
        }
        out.put("raw.familyToFontList.count", Integer.toString(familyToFontList.size()));
        for (Map.Entry<String, ArrayList<String>> entry : new TreeMap<>(familyToFontList).entrySet()) {
            out.put("raw.familyToFontList." + entry.getKey(), FontGoldens.joinList(entry.getValue()));
        }
    }

    /** What the public API derives from the maps after resolveWindowsFonts. */
    private static void capturePublicViews(Map<String, String> out, PrismFontFactory factory) {
        String[] families = factory.getFontFamilyNames();
        String[] fullNames = factory.getFontFullNames();
        out.put("api.familyNames.count", Integer.toString(families.length));
        out.put("api.familyNames", FontGoldens.joinList(Arrays.asList(families)));
        out.put("api.fullNames.count", Integer.toString(fullNames.length));
        out.put("api.fullNames", FontGoldens.joinList(Arrays.asList(fullNames)));
        for (String family : families) {
            out.put("api.family." + family.toLowerCase(Locale.ENGLISH),
                    FontGoldens.joinList(Arrays.asList(factory.getFontFullNames(family))));
        }
    }

    /** DWFactory.getFallbacks: FontLink("Tahoma") split, the EUDC file, then the two hardcoded entries. */
    private static void captureFallbacks(Map<String, String> out, PrismFontFactory factory) {
        FontFallbackInfo info = factory.getFallbacks(null);
        List<String> names = Arrays.asList(info.getFontNames());
        List<String> files = Arrays.asList(info.getFontFiles());
        out.put("fallbacks.count", Integer.toString(names.size()));
        out.put("fallbacks.names", FontGoldens.joinList(names));
        out.put("fallbacks.files", FontGoldens.joinList(files));
    }

    private static List<String> header() {
        return List.of(
                "Windows font enumeration golden, captured from the JNI build of javafx_font (fontpath.c)",
                "before any of it was rewritten. Machine-specific: it lists the fonts installed on the capturing",
                "machine. The comparison runs only where every machine.* key matches; elsewhere it is skipped.",
                "",
                "Capture:    " + FontGoldens.captureCommand(FontEnumerationGoldenTest.class),
                "Regenerate: add -D" + FontGoldens.REGENERATE_PROPERTY + "=true (reviewed as a behaviour change)",
                "",
                "Keys:",
                "  machine.*                  gate: os, arch, sha256 fingerprints of the font directories",
                "  info.* / capture.*         recorded, never compared",
                "  fontPath                   PrismFontFactory.getFontPath()",
                "  pathName.<file>            PrismFontFactory.getPathNameWindows(<file>)",
                "  raw.fontToFile.<lc name>   populateFontFileNameMap: registry Fonts value -> file (fresh map)",
                "  raw.fontToFamily.<lc name> populateFontFileNameMap: GDI face full name -> family",
                "  raw.familyToFontList.<lc>  populateFontFileNameMap: GDI family -> faces, enumeration order",
                "  fontLink.<name>            DWFactory.regReadFontLink(<name>), raw REG_MULTI_SZ (\\0 = NUL)",
                "  eudcFontFile               DWFactory.getEUDCFontFile()",
                "  lcdContrastWin32           PrismFontFactory.getLCDContrastWin32()",
                "  systemLCID                 PrismFontFactory.getSystemLCID()",
                "  systemFontSize/systemFont  PrismFontFactory.getSystemFontSize()/getSystemFont(\"System\")",
                "  api.*                      getFontFamilyNames()/getFontFullNames()/getFontFullNames(family)",
                "  fallbacks.*                DWFactory.getFallbacks(): names and files in order");
    }
}
