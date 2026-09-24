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
import com.sun.javafx.font.FontConfigNative;
import com.sun.javafx.font.FontConfigNativeShim;
import com.sun.javafx.font.LinuxFontOracleShim;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.font.LinuxFontGoldens.GoldenMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What no golden pins about {@link FontConfigNative}, the {@code java.lang.foreign} binding of fontconfig that
 * replaced the two JNI natives of {@link FontConfigManager} ({@code fontpath_linux.c}, commit
 * {@code 7b43255b30}): the {@code false} contract when neither library name opens and the second name being
 * tried after the first, the null-argument returns, the process effects ({@code HOME} in the C environment, the
 * fontconfig mappings released after each call), that the shim route is the production entry point, and the
 * {@code PRISM_FONTCONFIG_DEBUG} output written through libc in a child JVM ({@link #main}). Every font the
 * binding returns is pinned by {@link LinuxFontConfigGoldenTest} against the capture of that JNI build.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFontConfigNativeTest {

    /**
     * {@code HOME} in the C environment when this class was initialised, read through libc before
     * {@link #loadTheBinding} and its fontconfig calls; the JDK's start-up snapshot is not consulted.
     */
    private static final String LIBC_HOME_AT_LOAD = LinuxFontOracleShim.libcGetenv("HOME");

    private static final List<String> ABSENT = List.of("libfontconfig-absent.so.1", "libfontconfig-absent.so");

    private static String[] names;

    private static String defaultLocale;

    @BeforeAll
    public static void loadTheBinding() {
        FontConfigManagerShim.ensureLoaded();
        names = FontConfigManagerShim.fontConfigNames();
        defaultLocale = FontConfigManagerShim.fcLocaleStr();
        assumeTrue(LinuxFontOracleShim.libraryAvailable("libfontconfig.so.1"), "no libfontconfig.so.1");
    }

    @Test
    public void nullArrayGivesFalse() {
        assertFalse(FontConfigNative.getLogicalFonts(defaultLocale, null, true), "null array");
        assertFalse(FontConfigNative.getLogicalFonts(defaultLocale, null, false), "null array nofb");
    }

    /**
     * An empty array: {@code fontpath_linux.c} opened the library, resolved its symbols, ran its loop zero times
     * and returned {@code JNI_TRUE} after closing the library (:311-329, :541-546), in both fallback modes; the
     * library is released and {@code HOME} left alone as after any other call.
     */
    @Test
    public void emptyArrayIsTrueAndTouchesNothing() {
        long before = fontconfigMappings();
        FontConfigManager.FcCompFont[] none = new FontConfigManager.FcCompFont[0];
        assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, none, true), "empty array");
        assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, none, false), "empty array nofb");
        assertEquals(before, fontconfigMappings(), "libfontconfig mappings after the calls");
        if (LIBC_HOME_AT_LOAD != null) {
            assertEquals(LIBC_HOME_AT_LOAD, LinuxFontOracleShim.libcGetenv("HOME"), "HOME after the calls");
        }
    }

    /**
     * Twenty-five entries - the twelve logical names twice, then the bare family {@code sans}: the C declared
     * every per-entry variable inside its loop body (:330-335) and parsed and sorted each entry on its own
     * ({@code FcNameParse} :347, {@code FcFontSort} :364), so a repeated name fills its entry exactly as the
     * twelve-name array fills the name's first occurrence, and the bare family exactly as a one-entry array
     * does; neither the array length nor an entry's neighbours matter. Compared entry by entry through
     * {@link LinuxFontGoldens#putFontConfig}, in both fallback modes.
     */
    @Test
    public void duplicatedNamesFillEachEntry() {
        String[] twentyFive = new String[2 * names.length + 1];
        System.arraycopy(names, 0, twentyFive, 0, names.length);
        System.arraycopy(names, 0, twentyFive, names.length, names.length);
        twentyFive[2 * names.length] = "sans";
        for (boolean fallbacks : new boolean[] {true, false}) {
            FontConfigManager.FcCompFont[] many = FontConfigManagerShim.newLogicalFontArray(twentyFive);
            FontConfigManager.FcCompFont[] twelve = FontConfigManagerShim.newLogicalFontArray(names);
            FontConfigManager.FcCompFont[] sans = FontConfigManagerShim.newLogicalFontArray("sans");
            assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, many, fallbacks), "25 entries " + fallbacks);
            assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, twelve, fallbacks), "12 entries " + fallbacks);
            assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, sans, fallbacks), "sans " + fallbacks);
            for (int e = 0; e < many.length; e++) {
                FontConfigManager.FcCompFont reference = e < 2 * names.length ? twelve[e % names.length] : sans[0];
                assertSameEntries("entry " + e + " fallbacks=" + fallbacks, entry(many[e]), entry(reference));
            }
        }
    }

    /** One entry's rows as the golden records them, under a fixed prefix so that the positions compare. */
    private static GoldenMap entry(FontConfigManager.FcCompFont font) {
        GoldenMap graph = new GoldenMap();
        LinuxFontGoldens.putFontConfig(graph, "e", true, new FontConfigManager.FcCompFont[] {font},
                                       LinuxFontGoldens::safeText);
        return graph;
    }

    @Test
    public void nullMapArgumentsGiveFalseAndTouchNothing() {
        for (int position = 0; position < 4; position++) {
            HashMap<String, String> file = new HashMap<>();
            HashMap<String, String> family = new HashMap<>();
            HashMap<String, ArrayList<String>> list = new HashMap<>();
            boolean result = FontConfigNative.populateFontMaps(position == 0 ? null : file,
                    position == 1 ? null : family, position == 2 ? null : list,
                    position == 3 ? null : Locale.ENGLISH);
            assertFalse(result, "null argument " + position);
            assertTrue(file.isEmpty() && family.isEmpty() && list.isEmpty(), "maps touched " + position);
        }
    }

    /**
     * {@code HOME} in the C environment is what libc answered before any fontconfig call of this class
     * ({@code fontpath_linux.c} set {@code HOME=} only when it was unset), and {@code libfontconfig.so.1} is
     * mapped no more often after the calls than before: the binding opens it per call and releases it with the
     * call's arena.
     */
    @Test
    public void processEffects() {
        assumeTrue(LIBC_HOME_AT_LOAD != null, "HOME is unset; the putenv case runs in the process golden's child JVM");
        long before = fontconfigMappings();
        FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
        assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, fonts, true));
        assertTrue(maps(Locale.ENGLISH, null, null).result);
        long after = fontconfigMappings();
        assertEquals(LIBC_HOME_AT_LOAD, LinuxFontOracleShim.libcGetenv("HOME"), "HOME after the calls");
        assertEquals(before, after, "libfontconfig mappings after the calls");
    }

    /**
     * Iterations of {@link #handleIsReleasedOnEveryCall}: one {@code getLogicalFonts} and one
     * {@code populateFontMaps} each.
     */
    static final int HANDLE_LOOPS = 16;

    /**
     * {@code getLogicalFonts} with fallbacks and {@code populateFontMaps}, {@value #HANDLE_LOOPS} times in a row,
     * as {@code FontConfigManager} calls them: after every one of the calls the library must be unmapped again
     * (the confined arena of the call closed, the C's {@code dlclose}), {@code HOME} must be what libc answered
     * before the loop, and every iteration must return the same graph and the same maps.
     * <p>
     * No bound is put on the C heap here, and the growth is only recorded: fontconfig keeps the configuration it
     * builds on every {@code dlopen} (the C compiled its {@code FcFini} call out, :104-123, and this class calls
     * none), {@code getLogicalFonts} with fallbacks never destroys its {@code FcCharSetUnion} results and
     * {@code populateFontMaps} never destroys its pattern (:667) or its object set (:668-670), exactly as the C
     * did. Measured on fontconfig 2.17.1 with 121 fonts: about 245 KB retained per {@code dlopen}/{@code dlclose}
     * cycle that touches the configuration - the same for the test oracle's own independent {@code FcFontList}
     * cycle - plus 330 to 600 KB of unions per twelve-name sort with fallbacks, against which a forgotten
     * {@code FcPatternDestroy} (24 bytes and its elements) or {@code FcFontSetDestroy} (16 bytes and its
     * pointers) is not measurable. The mapping count is exact, so the library's own release is pinned per call.
     */
    @Test
    public void handleIsReleasedOnEveryCall() {
        long mappingsBefore = fontconfigMappings();
        long mallocBefore = LinuxFontOracleShim.mallocInUseBytes();
        FontConfigManager.FcCompFont[] first = FontConfigManagerShim.newLogicalFontArray(names);
        assertTrue(FontConfigNative.getLogicalFonts(defaultLocale, first, true), "iteration 0");
        GoldenMap referenceGraph = graph(true, first);
        Maps referenceMaps = maps(Locale.ENGLISH, null, null);
        assertTrue(referenceMaps.result, "iteration 0 maps");
        for (int i = 1; i < HANDLE_LOOPS; i++) {
            FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
            boolean result = FontConfigNative.getLogicalFonts(defaultLocale, fonts, true);
            assertEquals(mappingsBefore, fontconfigMappings(), "libfontconfig mappings after getLogicalFonts " + i);
            assertSameEntries("iteration " + i, graph(result, fonts), referenceGraph);
            Maps result2 = maps(Locale.ENGLISH, null, null);
            assertEquals(mappingsBefore, fontconfigMappings(), "libfontconfig mappings after populateFontMaps " + i);
            assertSameMaps("iteration " + i, result2, referenceMaps);
        }
        if (LIBC_HOME_AT_LOAD != null) {
            assertEquals(LIBC_HOME_AT_LOAD, LinuxFontOracleShim.libcGetenv("HOME"), "HOME after the loop");
        }
        long mallocAfter = LinuxFontOracleShim.mallocInUseBytes();
        System.out.println("[LinuxFontConfigNativeTest] " + HANDLE_LOOPS + " iterations: C heap in use "
                + mallocBefore + " -> " + mallocAfter + " (" + (mallocAfter - mallocBefore) / HANDLE_LOOPS
                + " bytes per iteration retained by fontconfig's configuration and the never-destroyed unions,"
                + " as the C retained them)");
    }

    @Test
    public void absentLibraryReturnsFalseAndTouchesNothing() {
        FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
        assertFalse(FontConfigNativeShim.getLogicalFonts(ABSENT, defaultLocale, fonts, true));
        for (FontConfigManager.FcCompFont font : fonts) {
            assertNull(font.firstFont, font.fcName);
            assertNull(font.allFonts, font.fcName);
        }
        HashMap<String, String> file = new HashMap<>();
        HashMap<String, String> family = new HashMap<>();
        HashMap<String, ArrayList<String>> list = new HashMap<>();
        assertFalse(FontConfigNativeShim.populateFontMaps(ABSENT, file, family, list, Locale.ENGLISH));
        assertTrue(file.isEmpty() && family.isEmpty() && list.isEmpty());
        if (LIBC_HOME_AT_LOAD != null) {
            assertEquals(LIBC_HOME_AT_LOAD, LinuxFontOracleShim.libcGetenv("HOME"));
        }
    }

    @Test
    public void theSecondLibraryNameIsTriedWhenTheFirstFails() {
        List<String> fallback = List.of(ABSENT.get(0), FontConfigNativeShim.libraries().get(0));
        FontConfigManager.FcCompFont[] viaFallback = FontConfigManagerShim.newLogicalFontArray(names);
        FontConfigManager.FcCompFont[] direct = FontConfigManagerShim.newLogicalFontArray(names);
        boolean fallbackResult = FontConfigNativeShim.getLogicalFonts(fallback, defaultLocale, viaFallback, true);
        boolean directResult = FontConfigNative.getLogicalFonts(defaultLocale, direct, true);
        assertSameEntries("second library name", graph(fallbackResult, viaFallback), graph(directResult, direct));
        assertTrue(directResult);
        HashMap<String, String> file = new HashMap<>();
        HashMap<String, String> family = new HashMap<>();
        HashMap<String, ArrayList<String>> list = new HashMap<>();
        assertTrue(FontConfigNativeShim.populateFontMaps(fallback, file, family, list, Locale.ENGLISH));
        assertSameMaps("second library name maps", new Maps(true, file, family, list),
                       maps(Locale.ENGLISH, null, null));
    }

    @Test
    public void theShimReachesTheSameEntryPoints() {
        FontConfigManager.FcCompFont[] viaShim = FontConfigManagerShim.newLogicalFontArray(names);
        FontConfigManager.FcCompFont[] direct = FontConfigManagerShim.newLogicalFontArray(names);
        boolean shimResult = FontConfigManagerShim.getFontConfig(defaultLocale, viaShim, true);
        boolean directResult = FontConfigNative.getLogicalFonts(defaultLocale, direct, true);
        assertSameEntries("shim", graph(shimResult, viaShim), graph(directResult, direct));
        HashMap<String, String> file = new HashMap<>();
        HashMap<String, String> family = new HashMap<>();
        HashMap<String, ArrayList<String>> list = new HashMap<>();
        boolean shimMaps = FontConfigManagerShim.populateMapsNative(file, family, list, Locale.ENGLISH);
        assertSameMaps("shim maps", new Maps(shimMaps, file, family, list), maps(Locale.ENGLISH, null, null));
    }

    /**
     * The debug output of {@code populateFontMaps} under {@code PRISM_FONTCONFIG_DEBUG} ({@code fontpath_linux.c}
     * wrote it with {@code fprintf(stderr, ...)}; the binding writes the same bytes through libc): a child JVM
     * with the variable set runs the binding between markers on {@code stderr}; the section must carry the
     * enumeration's lines and end with the null-argument message, and {@code getLogicalFonts}, which the child
     * runs first in the section, must add nothing.
     */
    @Test
    public void debugOutputGoesToStderrInAChildJvm() throws Exception {
        Path work = Files.createTempDirectory("fontconfig-native-debug");
        try {
            Path out = work.resolve("child.out");
            Path err = work.resolve("child.err");
            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                        || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                        || argument.startsWith("-XX:ErrorFile")) {
                    continue;
                }
                command.add(argument);
            }
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(LinuxFontConfigNativeTest.class.getName());
            command.add(DEBUG_MODE);
            ProcessBuilder builder = new ProcessBuilder(command)
                    .redirectOutput(out.toFile())
                    .redirectError(err.toFile());
            builder.environment().put("PRISM_FONTCONFIG_DEBUG", "1");
            Process process = builder.start();
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
                assertTrue(false, "child JVM did not finish: " + command);
            }
            String stderr = Files.readString(err, StandardCharsets.ISO_8859_1);
            String stdout = Files.readString(out, StandardCharsets.ISO_8859_1);
            assertEquals(0, process.exitValue(), "child JVM failed; stdout:\n" + stdout + "\nstderr:\n" + stderr);
            int start = stderr.indexOf(START_MARKER);
            int end = stderr.indexOf(END_MARKER);
            assertTrue(start >= 0 && end > start, "markers missing in the child's stderr:\n" + stderr);
            String section = stderr.substring(start + START_MARKER.length(), end);
            assertTrue(section.startsWith("Fontconfig found "), "section:\n" + section);
            assertTrue(section.contains("\nRead FC font family="), "section:\n" + section);
            assertTrue(section.contains("Done enumerating fontconfig fonts\n"), "section:\n" + section);
            assertTrue(section.endsWith("Null arg to native fontconfig lookup"), "section:\n" + section);
            System.out.println("fontconfig debug output: " + section.split("\n").length + " lines; " + stdout.trim());
        } finally {
            try (Stream<Path> paths = Files.walk(work)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static final String DEBUG_MODE = "debug";

    private static final String START_MARKER = "#START\n";

    private static final String END_MARKER = "\n#END\n";

    /**
     * The child of {@link #debugOutputGoesToStderrInAChildJvm}: with {@code PRISM_FONTCONFIG_DEBUG} in its
     * environment, runs one warm-up enumeration (so that whatever fontconfig itself prints on first
     * initialisation lands before the markers), then the binding between markers written to {@code stderr}.
     */
    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals(DEBUG_MODE)) {
            System.err.println("usage: " + LinuxFontConfigNativeTest.class.getName() + " " + DEBUG_MODE);
            System.exit(2);
        }
        FontConfigManagerShim.ensureLoaded();
        String[] fcNames = FontConfigManagerShim.fontConfigNames();
        String locale = FontConfigManagerShim.fcLocaleStr();
        maps(Locale.ENGLISH, null, null);
        PrintStream err = System.err;
        err.print(START_MARKER);
        err.flush();
        boolean sort = FontConfigNative.getLogicalFonts(locale, FontConfigManagerShim.newLogicalFontArray(fcNames),
                                                        true);
        Maps result = maps(Locale.ENGLISH, null, null);
        boolean nullArgument = FontConfigNative.populateFontMaps(null, new HashMap<>(), new HashMap<>(),
                                                                 Locale.ENGLISH);
        err.print(END_MARKER);
        err.flush();
        boolean ok = sort && result.result && !result.fontToFile.isEmpty() && !nullArgument;
        System.out.println("sort=" + sort + " maps=" + result.result + "/" + result.fontToFile.size() + " null="
                + nullArgument + "; ok=" + ok);
        System.exit(ok ? 0 : 1);
    }

    // ---------------------------------------------------------------------------------------------
    // Comparison
    // ---------------------------------------------------------------------------------------------

    /** The graph as the fontconfig golden records it: return; per entry first (identity), firstFont, count, fonts. */
    private static GoldenMap graph(boolean result, FontConfigManager.FcCompFont[] fonts) {
        GoldenMap graph = new GoldenMap();
        LinuxFontGoldens.putFontConfig(graph, "gfc", result, fonts, LinuxFontGoldens::safeText);
        return graph;
    }

    private static void assertSameEntries(String label, Map<String, String> a, Map<String, String> b) {
        List<String> differences = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>(a.keySet());
        keys.addAll(b.keySet());
        for (String key : keys) {
            String left = a.get(key);
            String right = b.get(key);
            if (left == null ? right != null : !left.equals(right)) {
                differences.add(key + ": " + left + " vs " + right);
            }
        }
        List<String> shown = differences.subList(0, Math.min(differences.size(), 40));
        assertTrue(differences.isEmpty(), label + ": " + differences.size() + " of " + keys.size()
                + " entries differ:\n  " + String.join("\n  ", shown));
        assertFalse(keys.isEmpty(), label + ": nothing compared");
    }

    private record Maps(boolean result, HashMap<String, String> fontToFile, HashMap<String, String> fontToFamily,
                        HashMap<String, ArrayList<String>> familyToFontList) {
    }

    private static Maps maps(Locale locale, Map<String, String> seedFiles, Map<String, List<String>> seedLists) {
        HashMap<String, String> file = new HashMap<>();
        HashMap<String, String> family = new HashMap<>();
        HashMap<String, ArrayList<String>> list = new HashMap<>();
        if (seedFiles != null) {
            file.putAll(seedFiles);
        }
        if (seedLists != null) {
            seedLists.forEach((key, value) -> list.put(key, new ArrayList<>(value)));
        }
        boolean result = FontConfigNative.populateFontMaps(file, family, list, locale);
        return new Maps(result, file, family, list);
    }

    private static void assertSameMaps(String label, Maps a, Maps b) {
        assertEquals(a.result, b.result, label + " return");
        assertEquals(a.fontToFile, b.fontToFile, label + " fontToFileMap");
        assertEquals(a.fontToFamily, b.fontToFamily, label + " fontToFamilyNameMap");
        assertEquals(a.familyToFontList, b.familyToFontList, label + " familyToFontListMap (list order included)");
        assertEquals(new ArrayList<>(a.familyToFontList.keySet()), new ArrayList<>(b.familyToFontList.keySet()),
                     label + " familyToFontListMap key order");
    }

    private static long fontconfigMappings() {
        try {
            List<String> lines = Files.readAllLines(Path.of("/proc/self/maps"), StandardCharsets.ISO_8859_1);
            return lines.stream().filter(line -> line.contains("libfontconfig")).count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
