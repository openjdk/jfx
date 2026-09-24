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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.font.LinuxFontGoldens.GoldenMap;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Parity oracle for the process-wide effects of the Linux font layer, against a golden captured from the JNI
 * build of commit {@code 7b43255b30}. Every case runs in its own child JVM ({@link LinuxFontProcessChild}),
 * because each one changes state that all later font lookups in a process would see:
 * <ul>
 * <li>{@code p1.registration}: {@code FcConfigAppFontAddFile} ({@code pango.c}) for good, repeated, missing,
 * invalid, empty, null, Latin-1 and supplementary-character paths (the JNI passes modified UTF-8, so the last
 * does not reach the file), what the raw maps and logical fonts see afterwards, that {@code HOME} in the C
 * environment is still what the JVM started with, and {@code PrismFontFactory.loadEmbeddedFont} followed by
 * layouts in the embedded font, then of the Latin-1 and supplementary-named copies (the same two path
 * crossings through production code). Ahem is registered from two paths, so the maps meet one full name twice;
 * the winner is recorded as whether it is the last of them in fontconfig's own list order, which depends on the
 * checkout path, and never as a path;</li>
 * <li>{@code p2.homeUnset}: with {@code HOME} unset, {@code fontpath_linux.c} sets {@code HOME=""} in the C
 * environment, for good (read again after allocator churn, further calls and a collection), and unloads
 * fontconfig after every call (mappings counted in {@code /proc/self/maps});</li>
 * <li>{@code p3.hermetic}: a private {@code FONTCONFIG_FILE} whose directory holds a symbolic link, a Type 1
 * font, an OpenType font with CFF outlines named only in language {@code und}, and file names with 2- and 4-byte
 * UTF-8 sequences, every string recorded as code units: this pins how {@code NewStringUTF} decodes file names,
 * where {@code realpath} applies, and that both format filters accept {@code CFF} (one more
 * {@code getFontConfig} call names the CFF family, so it sorts first);</li>
 * <li>{@code p4.emptyFonts}: a configuration with an empty font directory.</li>
 * </ul>
 * Paths in the children's output are normalised to {@code ${WORK}}, {@code ${MODULE}} and {@code ${REPO}}. See
 * {@link LinuxFontGoldens}.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFontProcessGoldenTest {

    static final String GOLDEN = "linux-font-process-golden.txt";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(LinuxFontProcessGoldenTest.class);

    static final String REGISTRATION = "p1.registration";
    static final String HOME_UNSET = "p2.homeUnset";
    static final String HERMETIC = "p3.hermetic";
    static final String EMPTY_FONTS = "p4.emptyFonts";

    /** The family of {@link LinuxFontGoldens#FONT_WITH_FEATURES_OTF}, as fontconfig 2.17.1 reads it. */
    static final String CFF_FAMILY = "FontWithFeaturesOTF";

    @Test
    public void processStateMatchesTheJniGolden() throws Exception {
        LinuxFontGoldens.captureOrVerify(LinuxFontProcessGoldenTest.class, GOLDEN, header(), false,
                                         LinuxFontProcessGoldenTest::capture,
                                         LinuxFontProcessGoldenTest::assertCoverage);
    }

    /** The machine gate passed at least once, so at least one body key was compared ({@link ParityGate}). */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * Not vacuous, on any Linux: a child JVM started with this fork's module and native options, class path and
     * a cleared locale environment reaches the font factory.
     */
    @Test
    public void childJvmHarnessWorks() throws Exception {
        Map<String, String> ping = LinuxFontGoldens.runChild("ping", Map.of(), Set.of());
        assertEquals("com.sun.javafx.font.freetype.FTFactory", ping.get("factory"), "font factory in the child");
        assertEquals(System.getProperty("os.name"), ping.get("os.name"), "os.name in the child");
        assertEquals(FontGoldens.NULL, ping.get("env.LC_ALL"), "LC_ALL in the child");
        assertEquals("C.UTF-8", ping.get("env.LANG"), "LANG in the child");
        System.out.println("[LinuxFontProcessGoldenTest] child class path: " + ping.get("java.class.path"));
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    static void capture(GoldenMap out) throws IOException, InterruptedException {
        prepareRegistration(scenarioDirectory(REGISTRATION));
        merge(out, REGISTRATION, LinuxFontGoldens.runChild(REGISTRATION, Map.of(), Set.of()));

        merge(out, HOME_UNSET, LinuxFontGoldens.runChild(HOME_UNSET, Map.of(), Set.of("HOME")));

        Path hermetic = scenarioDirectory(HERMETIC);
        prepareConfiguration(hermetic, true);
        merge(out, HERMETIC, LinuxFontGoldens.runChild(HERMETIC,
                Map.of("FONTCONFIG_FILE", hermetic.resolve("fonts.conf").toString()), Set.of()));

        Path empty = scenarioDirectory(EMPTY_FONTS);
        prepareConfiguration(empty, false);
        merge(out, EMPTY_FONTS, LinuxFontGoldens.runChild(EMPTY_FONTS,
                Map.of("FONTCONFIG_FILE", empty.resolve("fonts.conf").toString()), Set.of()));
    }

    private static void merge(GoldenMap out, String scenario, Map<String, String> child) {
        for (Map.Entry<String, String> entry : child.entrySet()) {
            out.put("proc." + scenario + "." + entry.getKey(), entry.getValue());
        }
    }

    /** {@code target/linux-font-golden/work/<scenario>}, shared with {@link LinuxFontProcessChild}. */
    static Path scenarioDirectory(String scenario) throws IOException {
        return LinuxFontGoldens.workDirectory().resolve("work").resolve(scenario);
    }

    /** Copies of Ahem under a Latin-1 and a supplementary-character name, and 256 zero bytes as a non-font. */
    static void prepareRegistration(Path directory) throws IOException {
        LinuxFontGoldens.deleteTree(directory);
        Files.createDirectories(directory);
        Path ahem = LinuxFontGoldens.pinned("ahem");
        Files.copy(ahem, directory.resolve("caf\u00E9.ttf"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(ahem, directory.resolve("smile\uD83D\uDE00.ttf"), StandardCopyOption.REPLACE_EXISTING);
        Files.write(directory.resolve("notafont.ttf"), new byte[256]);
    }

    /**
     * A {@code fonts.conf} with one font directory and a private cache. With {@code withFonts} the directory holds
     * a plain file, 2- and 4-byte UTF-8 file names, a symbolic link to a font outside it, a Type 1 font and a CFF
     * font.
     */
    static void prepareConfiguration(Path directory, boolean withFonts) throws IOException {
        LinuxFontGoldens.deleteTree(directory);
        Path fonts = Files.createDirectories(directory.resolve("fonts"));
        Path cache = Files.createDirectories(directory.resolve("cache"));
        if (withFonts) {
            Path real = Files.createDirectories(directory.resolve("real"));
            Files.copy(LinuxFontGoldens.pinned("dejavusans"), fonts.resolve("plain.ttf"));
            Files.copy(LinuxFontGoldens.pinned("dejavusansmono"), fonts.resolve("caf\u00E9.ttf"));
            Files.copy(LinuxFontGoldens.pinned("dejavuserif"), fonts.resolve("smile\uD83D\uDE00.ttf"));
            Files.copy(LinuxFontGoldens.pinned("latoregular"), real.resolve("Lato-Regular.ttf"));
            Files.createSymbolicLink(fonts.resolve("link.ttf"), real.resolve("Lato-Regular.ttf"));
            Files.copy(LinuxFontGoldens.pinned("freeeuro"), fonts.resolve("freeeuro.pfa"));
            Files.copy(LinuxFontGoldens.pinned("fontwithfeaturesotf"), fonts.resolve("features.otf"));
        }
        String configuration = String.join("\n",
                "<?xml version=\"1.0\"?>",
                "<!DOCTYPE fontconfig SYSTEM \"urn:fontconfig:fonts.dtd\">",
                "<fontconfig>",
                "  <dir>" + fonts.toAbsolutePath() + "</dir>",
                "  <cachedir>" + cache.toAbsolutePath() + "</cachedir>",
                "</fontconfig>",
                "");
        Files.writeString(directory.resolve("fonts.conf"), configuration, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------------------------------
    // Coverage
    // ---------------------------------------------------------------------------------------------

    /**
     * The golden can tell the right bindings from the wrong ones the audits named: standard UTF-8 for the
     * fontconfig path (through the shim and through {@code loadEmbeddedFont}), a UTF-8 decoder for fontconfig
     * strings, a dropped {@code putenv("HOME=")}, a {@code putenv("HOME=")} made although {@code HOME} is set,
     * a {@code HOME=} string that does not outlive its call, fontconfig kept loaded between calls (which is a
     * later, separate behaviour change), a format filter without {@code CFF}, and a first-put-wins name to file
     * map.
     */
    static void assertCoverage(Map<String, String> values) {
        List<String> missing = new ArrayList<>();
        String registration = "proc." + REGISTRATION + ".";
        LinuxFreetypeGoldenTest.check(missing, "true".equals(values.get(registration + "fcAdd.eAcute")),
                "FcConfigAppFontAddFile of a Latin-1 path returning true");
        LinuxFreetypeGoldenTest.check(missing, "false".equals(values.get(registration + "fcAdd.supplementary")),
                "FcConfigAppFontAddFile of a supplementary-character path returning false (modified UTF-8)");
        LinuxFreetypeGoldenTest.check(missing,
                values.getOrDefault(registration + "embedded.eAcute.fonts", "").startsWith("1;")
                && FontGoldens.NULL.equals(values.get(registration + "embedded.supplementary.fonts")),
                "loadEmbeddedFont of a Latin-1 path returning a font and of a supplementary-character path"
                        + " returning null (FcConfigAppFontAddFile with modified UTF-8 through production)");
        LinuxFreetypeGoldenTest.check(missing, "true".equals(values.get(registration + "home.set"))
                && "true".equals(values.get(registration + "home.libc.unchanged")),
                "HOME left unchanged in the C environment by fontconfig calls made with HOME set");
        String homeUnset = "proc." + HOME_UNSET + ".";
        LinuxFreetypeGoldenTest.check(missing, "".equals(values.get(homeUnset + "home.libc.afterChurn"))
                && "".equals(values.get(homeUnset + "home.libc.afterPm")),
                "HOME still the empty string in the C environment after allocator churn, further fontconfig calls"
                        + " and a collection");
        String after = registration + "raw.pm.after.";
        LinuxFreetypeGoldenTest.check(missing, "2".equals(values.get(after + "ahem.fcFontListFiles"))
                && "Ahem;Ahem".equals(values.get(after + "list.ahem"))
                && "true".equals(values.get(after + "file.ahem.isLastInFcFontList"))
                && "Ahem;isLastInFcFontList=true".equals(values.get(registration + "createFont.ahem")),
                "one full name registered from two paths, the last in FcFontList order winning the file maps");
        String cffFile = "${WORK}/fonts/features.otf";
        String[] cffFirst = values.getOrDefault("proc." + HERMETIC + ".gfc.cff.e00.firstFont", "").split(";", -1);
        LinuxFreetypeGoldenTest.check(missing, "0".equals(values.get("proc." + HERMETIC + ".gfc.cff.e00.first"))
                && cffFirst.length == 4 && isUnits(cffFirst[3], cffFile),
                "getFontConfig accepting a CFF font as the first font");
        LinuxFreetypeGoldenTest.check(missing,
                isUnits(values.get("proc." + HERMETIC + ".pm.file.fontwithfeaturesotf"), cffFile),
                "populateMapsNative entering a CFF font");
        String smile = "${WORK}/fonts/smile\uD83D\uDE00.ttf";
        boolean jniDecodedName = false;
        boolean homeEmpty = false;
        boolean mappingsCounted = false;
        boolean mappingsZero = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("proc." + HERMETIC + ".") && value.startsWith("u:")) {
                for (String part : value.split("[;|]")) {
                    String text = part.startsWith("u:") ? LinuxFontGoldens.fromUnitsHex(part.substring(2)) : null;
                    jniDecodedName |= text != null && text.startsWith("${WORK}/fonts/smile") && !text.equals(smile);
                }
            } else if (key.equals("proc." + HOME_UNSET + ".home.libc")) {
                homeEmpty = value.isEmpty();
            } else if (key.startsWith("proc." + HOME_UNSET + ".maps.fontconfig.")) {
                mappingsCounted = true;
                mappingsZero &= value.equals("0");
            }
        }
        LinuxFreetypeGoldenTest.check(missing, jniDecodedName,
                "a hermetic file name whose Java string differs from its UTF-8 decode (NewStringUTF)");
        LinuxFreetypeGoldenTest.check(missing, homeEmpty, "HOME set to the empty string in the C environment");
        LinuxFreetypeGoldenTest.check(missing, mappingsCounted && mappingsZero,
                "libfontconfig unmapped after every fontconfig call");
        if (!missing.isEmpty()) {
            fail(GOLDEN + " is not discriminating; it lacks: " + missing);
        }
    }

    /** Whether {@code value} is {@code text} written as {@code u:} and its UTF-16 code units. */
    private static boolean isUnits(String value, String text) {
        return value != null && value.startsWith("u:")
                && text.equals(LinuxFontGoldens.fromUnitsHex(value.substring(2)));
    }

    private static List<String> header() {
        return LinuxFontGoldens.header("Linux font process golden (child JVMs: registration, HOME, fontconfig)",
                List.of(
                "  proc.<scenario>.*      what LinuxFontProcessChild <scenario> recorded; paths as ${WORK},",
                "                         ${MODULE} and ${REPO}; u:XXXX strings are UTF-16 code units",
                "  proc.p1.registration.fcAdd.<case>   FcConfigAppFontAddFile(0, path)",
                "  proc.p1.registration.raw.*          raw maps before (summary) and after (changed entries),",
                "                         getFontConfig after registration; the Ahem file winner as",
                "                         isLastInFcFontList (LinuxFontOracleShim.fcOutlineFontFiles order)",
                "  proc.p1.registration.embedded.*     loadEmbeddedFont and layouts in the embedded font;",
                "                         .eAcute/.supplementary: the Latin-1 and supplementary-named copies",
                "  proc.p1.registration.home.*         HOME set; C HOME unchanged after the fontconfig calls",
                "  proc.p2.homeUnset.*    libfontconfig mappings, C and JDK HOME, returns, with HOME unset;",
                "                         home.libc.afterChurn/.afterPm: C HOME after strdup/free churn, more calls",
                "  proc.p3.hermetic.*     getFontConfig and populateMapsNative over a private FONTCONFIG_FILE;",
                "                         gfc.cff.* is getFontConfig of the CFF family alone",
                "  proc.p4.emptyFonts.*   the same over an empty font directory"));
    }
}
