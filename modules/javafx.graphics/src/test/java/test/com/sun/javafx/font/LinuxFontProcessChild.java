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
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Glyph;
import com.sun.javafx.font.LinuxFontOracleShim;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.freetype.OSFreetypeShim;
import com.sun.javafx.font.freetype.OSPangoShim;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.text.PrismTextLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import test.com.sun.javafx.font.LinuxFontGoldens.FontMaps;
import test.com.sun.javafx.font.LinuxFontGoldens.GoldenMap;

/**
 * The child JVM of {@link LinuxFontProcessGoldenTest}, {@link LinuxFontStressTest} and the ping of
 * {@link LinuxFontProcessGoldenTest#childJvmHarnessWorks()}: runs one scenario in a fresh process and writes what
 * it recorded in the golden format to the file named by its second argument. It is not a test class and has no
 * test of its own.
 * <p>
 * Usage: {@code LinuxFontProcessChild <scenario> <output file>}, working directory
 * {@code modules/javafx.graphics}, scenario files prepared by the parent under
 * {@code target/linux-font-golden/work/<scenario>}.
 */
final class LinuxFontProcessChild {

    private LinuxFontProcessChild() {
    }

    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        Path output = Path.of(args[1]);
        GoldenMap out = new GoldenMap();
        switch (scenario) {
            case "ping" -> ping(out);
            case LinuxFontProcessGoldenTest.REGISTRATION -> registration(out);
            case LinuxFontProcessGoldenTest.HOME_UNSET -> homeUnset(out);
            case LinuxFontProcessGoldenTest.HERMETIC, LinuxFontProcessGoldenTest.EMPTY_FONTS -> hermetic(out, scenario);
            case LinuxFontStressTest.DISPOSER -> stressDisposer(out);
            case LinuxFontStressTest.LAYOUTS -> stressLayouts(out);
            case LinuxFontStressTest.OUTLINES -> stressOutlines(out);
            case LinuxFontStressTest.SHAPE -> stressShape(out);
            case LinuxFontStressTest.FONTCONFIG -> stressFontconfig(out);
            case LinuxFontStressTest.TIMING -> timing(out);
            default -> throw new IllegalArgumentException("unknown scenario " + scenario);
        }
        Files.writeString(output, FontGoldens.format(out, List.of("LinuxFontProcessChild " + scenario)),
                          StandardCharsets.UTF_8);
        System.exit(0);
    }

    // ---------------------------------------------------------------------------------------------
    // Paths
    // ---------------------------------------------------------------------------------------------

    private static final class Paths {
        static final Path MODULE = Path.of("").toAbsolutePath();
        static final Path REPO = MODULE.resolve("../..").normalize();

        static Path work(String scenario) {
            return MODULE.resolve(LinuxFontGoldens.WORK_DIRECTORY).resolve("work").resolve(scenario);
        }
    }

    /** Replaces the scenario, module and repository directories (as given and resolved) with tokens. */
    private static UnaryOperator<String> normalizer(String scenario) {
        List<String[]> replacements = new ArrayList<>();
        addReplacement(replacements, Paths.work(scenario), "${WORK}");
        addReplacement(replacements, Paths.MODULE, "${MODULE}");
        addReplacement(replacements, Paths.REPO, "${REPO}");
        return text -> {
            if (text == null) {
                return null;
            }
            String normalized = text;
            for (String[] replacement : replacements) {
                normalized = normalized.replace(replacement[0], replacement[1]);
            }
            return normalized;
        };
    }

    private static void addReplacement(List<String[]> replacements, Path path, String token) {
        try {
            String real = path.toRealPath().toString();
            if (!real.equals(path.toString())) {
                replacements.add(new String[] {real, token});
            }
        } catch (IOException e) {
            // not present: only the given form can occur
        }
        replacements.add(new String[] {path.toString(), token});
    }

    /** {@code realpath}, as {@code populateMapsNative} applies it to file names; {@code null} if it fails. */
    private static String realPath(String file) {
        try {
            return file == null ? null : Path.of(file).toRealPath().toString();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int fontconfigMappings() throws IOException {
        int count = 0;
        for (String line : Files.readAllLines(Path.of("/proc/self/maps"), StandardCharsets.UTF_8)) {
            if (line.contains("libfontconfig")) {
                count++;
            }
        }
        return count;
    }

    // ---------------------------------------------------------------------------------------------
    // Golden scenarios
    // ---------------------------------------------------------------------------------------------

    private static void ping(GoldenMap out) {
        out.put("factory", PrismFontFactory.getFontFactory().getClass().getName());
        out.put("os.name", System.getProperty("os.name"));
        out.put("env.LANG", System.getenv("LANG"));
        out.put("env.LC_ALL", System.getenv("LC_ALL"));
        out.put("java.class.path", System.getProperty("java.class.path"));
    }

    /**
     * Registers the Ahem font from two paths (the repository file and a Latin-1 named copy), so that
     * {@code populateMapsNative} meets one full name twice and the last pattern in {@code FcFontList} order must win
     * the name to file map, while the family list keeps both. Which path that is depends on fontconfig's hash of
     * the absolute paths, hence on where the repository is checked out, so the winner is recorded relative to the
     * order {@link LinuxFontOracleShim#fcOutlineFontFiles} observes in the same process, never as a path.
     * <p>
     * {@code HOME} is inherited here, so after the fontconfig calls the C environment must still hold the value the
     * JVM started with ({@code home.libc.unchanged}); the value itself is gated by {@code machine.env} and never
     * recorded. The last rows load the Latin-1 and the supplementary-character copies through
     * {@code PrismFontFactory.loadEmbeddedFont}, which is {@code FT_New_Face} and {@code FcConfigAppFontAddFile}
     * on non-ASCII paths through production code rather than through a shim.
     */
    private static void registration(GoldenMap out) throws IOException {
        String homeAtStart = System.getenv("HOME");
        String scenario = LinuxFontProcessGoldenTest.REGISTRATION;
        UnaryOperator<String> normalize = normalizer(scenario);
        UnaryOperator<String> text = s -> LinuxFontGoldens.safeText(normalize.apply(s));
        Path work = Paths.work(scenario);
        FontConfigManagerShim.ensureLoaded();

        FontMaps before = FontMaps.populate(Locale.ENGLISH);
        TreeMap<String, String> beforeEntries = before.entries(LinuxFontGoldens::safeText, text);
        out.put("raw.pm.before", before.result() + "|" + before.counts() + "|" + FontGoldens.sha256Hex(
                String.join("\n", beforeEntries.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .toList()).getBytes(StandardCharsets.UTF_8)));

        String ahem = LinuxFontGoldens.pinned("ahem").toString();
        String[][] cases = {
            {"ahem", ahem}, {"ahemAgain", ahem}, {"missing", work.resolve("none.ttf").toString()},
            {"notAFont", work.resolve("notafont.ttf").toString()}, {"empty", ""}, {"null", null},
            {"eAcute", work.resolve("caf\u00E9.ttf").toString()},
            {"supplementary", work.resolve("smile\uD83D\uDE00.ttf").toString()}
        };
        for (String[] c : cases) {
            out.put("fcAdd." + c[0], Boolean.toString(OSPangoShim.FcConfigAppFontAddFile(0, c[1])));
        }

        FontMaps after = FontMaps.populate(Locale.ENGLISH);
        List<String> ahemFiles = LinuxFontOracleShim.fcOutlineFontFiles("Ahem");
        String lastAhemFile = ahemFiles.isEmpty() ? null : realPath(ahemFiles.getLast());
        System.out.println("Ahem files in FcFontList order: " + ahemFiles.stream().map(normalize).toList());
        out.put("raw.pm.after", after.result() + "|" + after.counts());
        out.put("raw.pm.after.ahem.fcFontListFiles", Integer.toString(ahemFiles.size()));
        for (Map.Entry<String, String> entry : after.entries(LinuxFontGoldens::safeText, text).entrySet()) {
            if (entry.getKey().equals("file.ahem")) {
                out.put("raw.pm.after.file.ahem.isLastInFcFontList",
                        Boolean.toString(lastAhemFile != null && lastAhemFile.equals(after.fontToFile().get("ahem"))));
            } else if (!entry.getValue().equals(beforeEntries.get(entry.getKey()))) {
                out.put("raw.pm.after." + entry.getKey(), entry.getValue());
            }
        }
        String[] names = FontConfigManagerShim.fontConfigNames();
        FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
        boolean result = FontConfigManagerShim.getFontConfig(FontConfigManagerShim.fcLocaleStr(), fonts, true);
        GoldenMap sans = new GoldenMap();
        LinuxFontGoldens.putFontConfig(sans, "raw.gfc.after.sans", result, fonts, text);
        for (Map.Entry<String, String> entry : sans.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("raw.gfc.after.sans.e00.") || key.equals("raw.gfc.after.sans.return")) {
                out.put(key, entry.getValue());
            }
        }
        for (int e = 1; e < fonts.length; e++) {
            String element = "raw.gfc.after.sans.e" + LinuxFontGoldens.pad(e, 2) + ".";
            StringBuilder content = new StringBuilder();
            sans.forEach((k, v) -> {
                if (k.startsWith(element)) {
                    content.append(k.substring(element.length())).append('=').append(v).append('\n');
                }
            });
            out.put(element + "sha256", FontGoldens.sha256Hex(content.toString().getBytes(StandardCharsets.UTF_8)));
        }
        out.put("home.set", Boolean.toString(homeAtStart != null));
        out.put("home.libc.unchanged",
                Boolean.toString(Objects.equals(homeAtStart, LinuxFontOracleShim.libcGetenv("HOME"))));

        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        PGFont[] embedded = factory.loadEmbeddedFont(null, LinuxFontGoldens.pinned("dejavusanscondensed").toString(),
                                                     12f, true, false);
        out.put("embedded.fonts", embedded == null ? FontGoldens.NULL : embedded.length + ";"
                + text.apply(embedded[0].getFullName()) + ";" + embedded[0].getFontResource().getClass().getName());
        out.put("embedded.count", Integer.toString(factory.test_getNumEmbeddedFonts()));
        if (embedded != null) {
            LinuxFontGoldens.putLayout(out, "embedded.layout.s03", embedded[0], LinuxFontGoldens.TEXTS.get(3), false);
            LinuxFontGoldens.putLayout(out, "embedded.layout.s04", embedded[0], LinuxFontGoldens.TEXTS.get(4), false);
            LinuxFontGoldens.putLayout(out, "embedded.layout.aAlefB", embedded[0], "a\u05D0b", false);
        }
        PGFont[] notAFont = factory.loadEmbeddedFont(null, work.resolve("notafont.ttf").toString(), 12f, true, false);
        out.put("embedded.notAFont", notAFont == null ? FontGoldens.NULL : Integer.toString(notAFont.length));
        out.put("embedded.countAfterNotAFont", Integer.toString(factory.test_getNumEmbeddedFonts()));

        PGFont ahemFont = factory.createFont("Ahem", 12f);
        out.put("createFont.ahem", text.apply(ahemFont.getFullName()) + ";isLastInFcFontList="
                + (lastAhemFile != null && lastAhemFile.equals(ahemFont.getFontResource().getFileName())));
        LinuxFontGoldens.putLayout(out, "createFont.ahem.layout", ahemFont, "AB", false);
        out.put("application.fonts", Integer.toString(LinuxFontOracleShim.fcApplicationFontCount()));

        PGFont[] eAcute = factory.loadEmbeddedFont(null, work.resolve("caf\u00E9.ttf").toString(), 12f, true, false);
        out.put("embedded.eAcute.fonts", embeddedSummary(eAcute, text));
        out.put("embedded.eAcute.count", Integer.toString(factory.test_getNumEmbeddedFonts()));
        PGFont[] supplementary = factory.loadEmbeddedFont(null, work.resolve("smile\uD83D\uDE00.ttf").toString(), 12f,
                                                          true, false);
        out.put("embedded.supplementary.fonts", embeddedSummary(supplementary, text));
        out.put("embedded.supplementary.count", Integer.toString(factory.test_getNumEmbeddedFonts()));
        PGFont[] nonAscii = eAcute != null ? eAcute : supplementary;
        if (nonAscii != null) {
            LinuxFontGoldens.putLayout(out, "embedded.nonAscii.layout", nonAscii[0], "AB", false);
        }
        out.put("application.fonts.afterEmbedded", Integer.toString(LinuxFontOracleShim.fcApplicationFontCount()));
    }

    /** {@code count;fullName;resourceClass;file} of a {@code loadEmbeddedFont} result, or {@code #null}. */
    private static String embeddedSummary(PGFont[] fonts, UnaryOperator<String> text) {
        if (fonts == null) {
            return FontGoldens.NULL;
        }
        FontResource resource = fonts[0].getFontResource();
        return fonts.length + ";" + text.apply(fonts[0].getFullName()) + ";" + resource.getClass().getName() + ";"
                + text.apply(resource.getFileName());
    }

    /** Strings of the {@code HOME=} size class made and freed to reuse a freed {@code putenv} string's chunk. */
    private static final int CHURN_STRINGS = 4096;

    /**
     * No {@code System.getenv} may run before the fontconfig calls: it would snapshot the environment early. The
     * {@code HOME=} string handed to {@code putenv} stays in the C environment for the rest of the process, so it
     * is read again after allocator churn of its size class, further fontconfig calls and a collection: a
     * binding that passed {@code putenv} a per-call buffer would lose or corrupt it there.
     */
    private static void homeUnset(GoldenMap out) throws IOException {
        out.put("maps.fontconfig.before", Integer.toString(fontconfigMappings()));
        FontConfigManagerShim.ensureLoaded();
        out.put("maps.fontconfig.afterLoad", Integer.toString(fontconfigMappings()));
        String[] names = FontConfigManagerShim.fontConfigNames();
        boolean result = FontConfigManagerShim.getFontConfig(FontConfigManagerShim.fcLocaleStr(),
                FontConfigManagerShim.newLogicalFontArray(names), true);
        out.put("gfc.return", Boolean.toString(result));
        out.put("maps.fontconfig.afterGfc", Integer.toString(fontconfigMappings()));
        out.put("home.libc", LinuxFontGoldens.safeText(LinuxFontOracleShim.libcGetenv("HOME")));
        out.put("home.jdk", LinuxFontGoldens.safeText(System.getenv("HOME")));
        LinuxFontOracleShim.mallocChurn(CHURN_STRINGS, "HOME=".length());
        out.put("home.libc.afterChurn", LinuxFontGoldens.safeText(LinuxFontOracleShim.libcGetenv("HOME")));
        FontMaps maps = FontMaps.populate(Locale.ENGLISH);
        out.put("pm.return", Boolean.toString(maps.result()));
        out.put("maps.fontconfig.afterPm", Integer.toString(fontconfigMappings()));
        boolean again = FontConfigManagerShim.getFontConfig(FontConfigManagerShim.fcLocaleStr(),
                FontConfigManagerShim.newLogicalFontArray(names), true);
        out.put("gfc.again.return", Boolean.toString(again));
        System.gc();
        LinuxFontOracleShim.mallocChurn(CHURN_STRINGS, "HOME=".length());
        out.put("home.libc.afterPm", LinuxFontGoldens.safeText(LinuxFontOracleShim.libcGetenv("HOME")));
    }

    private static void hermetic(GoldenMap out, String scenario) {
        UnaryOperator<String> normalize = normalizer(scenario);
        UnaryOperator<String> units = s -> s == null ? FontGoldens.NULL
                : "u:" + LinuxFontGoldens.unitsHex(normalize.apply(s));
        FontConfigManagerShim.ensureLoaded();
        String[] names = FontConfigManagerShim.fontConfigNames();
        for (boolean fallbacks : new boolean[] {true, false}) {
            FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
            boolean result = FontConfigManagerShim.getFontConfig("en", fonts, fallbacks);
            LinuxFontGoldens.putFontConfig(out, "gfc." + (fallbacks ? "fb" : "nofb"), result, fonts, units);
        }
        FontConfigManager.FcCompFont[] cff =
                FontConfigManagerShim.newLogicalFontArray(LinuxFontProcessGoldenTest.CFF_FAMILY);
        boolean cffResult = FontConfigManagerShim.getFontConfig("en", cff, true);
        LinuxFontGoldens.putFontConfig(out, "gfc.cff", cffResult, cff, units);
        FontMaps maps = FontMaps.populate(Locale.ENGLISH);
        out.put("pm.return", Boolean.toString(maps.result()));
        out.put("pm.counts", maps.counts());
        maps.entries(LinuxFontGoldens::safeText, units).forEach((k, v) -> out.put("pm." + k, v));
    }

    // ---------------------------------------------------------------------------------------------
    // Stress baselines
    // ---------------------------------------------------------------------------------------------

    /** Counts the {@code prism.debugfonts} lines the FreeType disposer prints, on whatever thread it runs. */
    private static final class DisposalCounter extends OutputStream {
        private final PrintStream original;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        final AtomicInteger faces = new AtomicInteger();
        final AtomicInteger libraries = new AtomicInteger();

        DisposalCounter(PrintStream original) {
            this.original = original;
        }

        @Override
        public synchronized void write(int b) {
            original.write(b);
            if (b == '\n') {
                String text = line.toString(StandardCharsets.UTF_8);
                if (text.startsWith("Done Face=")) {
                    faces.incrementAndGet();
                } else if (text.startsWith("Done Library=")) {
                    libraries.incrementAndGet();
                }
                line.reset();
            } else {
                line.write(b);
            }
        }
    }

    /**
     * Unregistered embedded fonts own an {@code FTDisposer}; dropping them must run {@code FT_Done_Face} and
     * {@code FT_Done_FreeType} once each on the disposer thread. Needs {@code -Dprism.debugfonts=true}. FreeType
     * maps the file of every face it opens ({@code mmap}) and unmaps it in {@code FT_Done_Face}, so the mappings
     * of the font file in {@code /proc/self/maps} are counted before the loads, while every font is still held,
     * and after the disposer ran: the prints alone would also count a disposer whose native calls release nothing.
     */
    private static void stressDisposer(GoldenMap out) throws Exception {
        DisposalCounter counter = new DisposalCounter(System.err);
        System.setErr(new PrintStream(counter, true, StandardCharsets.UTF_8));
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        Path file = LinuxFontGoldens.pinned("dejavuserif");
        String mapped = file.toRealPath().toString();
        out.put("maps.font.before", Integer.toString(fileMappings(mapped)));
        List<PGFont[]> held = new ArrayList<>();
        int rendered = 0;
        for (int i = 0; i < LinuxFontStressTest.DISPOSER_FONTS; i++) {
            PGFont[] fonts = factory.loadEmbeddedFont(null, file.toString(), 12f, false, false);
            if (fonts == null) {
                continue;
            }
            held.add(fonts);
            FontResource resource = fonts[0].getFontResource();
            FontStrike strike = resource.getStrike(12f, BaseTransform.IDENTITY_TRANSFORM, FontResource.AA_GREYSCALE);
            Glyph glyph = strike.getGlyph(resource.getGlyphMapper().charToGlyph('A'));
            if (glyph.getPixelData() != null && glyph.getShape() != null) {
                rendered++;
            }
        }
        int created = held.size();
        out.put("maps.font.held", Integer.toString(fileMappings(mapped)));
        held.clear();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
        while ((counter.faces.get() < created || counter.libraries.get() < created || fileMappings(mapped) > 0)
               && System.nanoTime() < deadline) {
            System.gc();
            Thread.sleep(50);
        }
        out.put("created", Integer.toString(created));
        out.put("rendered", Integer.toString(rendered));
        out.put("doneFace", Integer.toString(counter.faces.get()));
        out.put("doneLibrary", Integer.toString(counter.libraries.get()));
        out.put("maps.font.afterDispose", Integer.toString(fileMappings(mapped)));
    }

    /** Lines of {@code /proc/self/maps} that map {@code file}, given as the real path the kernel reports. */
    private static int fileMappings(String file) throws IOException {
        int count = 0;
        for (String line : Files.readAllLines(Path.of("/proc/self/maps"), StandardCharsets.UTF_8)) {
            if (line.contains(" " + file)) {
                count++;
            }
        }
        return count;
    }

    /** The C heap in use and the resident set, read after collections so that dropped Java objects do not count. */
    private record MemorySnapshot(long malloc, long resident) {

        static MemorySnapshot take() throws InterruptedException {
            System.gc();
            Thread.sleep(100);
            System.gc();
            return new MemorySnapshot(LinuxFontOracleShim.mallocInUseBytes(), LinuxFontOracleShim.residentSetBytes());
        }
    }

    /** {@code mem.malloc.*} and {@code mem.resident.*}: warm, end and growth in bytes; malloc {@code -1} if absent. */
    private static void putMemoryGrowth(GoldenMap out, MemorySnapshot warm, MemorySnapshot end) {
        out.put("mem.malloc.warm", Long.toString(warm.malloc()));
        out.put("mem.malloc.end", Long.toString(end.malloc()));
        out.put("mem.malloc.growth", Long.toString(warm.malloc() < 0 ? -1 : end.malloc() - warm.malloc()));
        out.put("mem.resident.warm", Long.toString(warm.resident()));
        out.put("mem.resident.end", Long.toString(end.resident()));
        out.put("mem.resident.growth", Long.toString(end.resident() - warm.resident()));
    }

    /**
     * Layouts handed between threads: {@code GlyphLayoutManager} gives the reusable {@code PangoGlyphLayout} to
     * whichever thread asks, which frees its UTF-8 buffers ({@code g_free}) in {@code dispose}. Pango itself is
     * not thread-safe, so the layouts are serialised by a lock and only the threads vary. The C heap and the
     * resident set are read after the warm-up rounds and at the end, each time after the pause of
     * {@link LinuxFontStressTest#LAYOUT_SETTLE_MILLIS} that lets HotSpot empty its pool of freed compiler arena
     * chunks (counted as C heap in use until then) and trim the native heap, so that memory retained per layout
     * (the UTF-8 copy of the text, the glyph string of each {@code pango_shape}) shows as growth.
     */
    private static void stressLayouts(GoldenMap out) throws Exception {
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        PGFont font = factory.createFont("DejaVu Sans", 12f);
        List<String> texts = LinuxFontGoldens.TEXTS;
        int[] reference = new int[texts.size()];
        for (int i = 0; i < reference.length; i++) {
            reference[i] = glyphCount(font, texts.get(i));
        }
        try (LayoutWorkload workload = new LayoutWorkload(font, texts, reference)) {
            workload.run(LinuxFontStressTest.LAYOUT_WARMUP_ROUNDS);
            Thread.sleep(LinuxFontStressTest.LAYOUT_SETTLE_MILLIS);
            MemorySnapshot warm = MemorySnapshot.take();
            workload.reset();
            workload.run(LinuxFontStressTest.LAYOUT_ROUNDS);
            Thread.sleep(LinuxFontStressTest.LAYOUT_SETTLE_MILLIS);
            MemorySnapshot end = MemorySnapshot.take();
            int total = 0;
            for (int count : reference) {
                total += count;
            }
            out.put("layouts", Integer.toString(workload.layouts.get()));
            out.put("mismatches", Integer.toString(workload.mismatches.get()));
            out.put("referenceGlyphs", Integer.toString(total));
            putMemoryGrowth(out, warm, end);
        }
    }

    /** The threads of {@link #stressLayouts}, kept between the warm-up and the measured rounds. */
    private static final class LayoutWorkload implements AutoCloseable {
        private final PGFont font;
        private final List<String> texts;
        private final int[] reference;
        private final ReentrantLock pango = new ReentrantLock();
        private final ExecutorService pool = Executors.newFixedThreadPool(LinuxFontStressTest.THREADS);
        final AtomicInteger layouts = new AtomicInteger();
        final AtomicInteger mismatches = new AtomicInteger();

        LayoutWorkload(PGFont font, List<String> texts, int[] reference) {
            this.font = font;
            this.texts = texts;
            this.reference = reference;
        }

        void reset() {
            layouts.set(0);
            mismatches.set(0);
        }

        /** {@code rounds} rounds over the corpus on each thread; the layouts are serialised by the lock. */
        void run(int rounds) throws Exception {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < LinuxFontStressTest.THREADS; t++) {
                futures.add(pool.submit(() -> {
                    for (int round = 0; round < rounds; round++) {
                        for (int i = 0; i < reference.length; i++) {
                            pango.lock();
                            try {
                                if (glyphCount(font, texts.get(i)) != reference[i]) {
                                    mismatches.incrementAndGet();
                                }
                                layouts.incrementAndGet();
                            } finally {
                                pango.unlock();
                            }
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        }

        @Override
        public void close() {
            pool.shutdown();
        }
    }

    private static int glyphCount(PGFont font, String text) {
        PrismTextLayout layout = new PrismTextLayout(0);
        layout.setContent(text, font);
        int count = 0;
        for (GlyphList run : layout.getRuns()) {
            count += run.getGlyphCount();
        }
        return count;
    }

    /**
     * Outlines built concurrently: one thread per font file (each {@code FTFontFile} has its own
     * {@code FT_Library}) plus two more threads sharing the first file, whose calls its monitor serialises. The
     * C heap and the resident set are read after a warm-up round and at the end, so that memory retained per
     * {@code FT_Outline_Decompose} (the accumulator, the callbacks handed to FreeType) shows as growth.
     */
    private static void stressOutlines(GoldenMap out) throws Exception {
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        List<FontResource> resources = new ArrayList<>();
        String[][] files = {
            {"dejavusans", "DejaVu Sans"}, {"dejavusansbold", "DejaVu Sans Bold"},
            {"dejavusansoblique", "DejaVu Sans Oblique"}, {"dejavuserif", "DejaVu Serif"},
            {"dejavusansmono", "DejaVu Sans Mono"}, {"latoregular", "Lato Regular"}
        };
        for (String[] file : files) {
            FontResource resource = factory.getFontResource(file[1], LinuxFontGoldens.pinned(file[0]).toString(),
                                                            false);
            if (resource != null) {
                resources.add(resource);
            }
        }
        try (OutlineWorkload workload = new OutlineWorkload(resources)) {
            workload.run(1);
            MemorySnapshot warm = MemorySnapshot.take();
            workload.reset();
            workload.run(LinuxFontStressTest.OUTLINE_ROUNDS);
            MemorySnapshot end = MemorySnapshot.take();
            out.put("fonts", Integer.toString(resources.size()));
            out.put("threads", Integer.toString(workload.threads()));
            out.put("outlines", Integer.toString(workload.outlines.get()));
            out.put("mismatches", Integer.toString(workload.mismatches.get()));
            putMemoryGrowth(out, warm, end);
        }
    }

    /** The threads of {@link #stressOutlines}, kept between the warm-up and the measured rounds. */
    private static final class OutlineWorkload implements AutoCloseable {
        private final List<FontResource> resources;
        private final String[] reference;
        private final List<Integer> assignment = new ArrayList<>();
        private final ExecutorService pool;
        final AtomicInteger outlines = new AtomicInteger();
        final AtomicInteger mismatches = new AtomicInteger();

        OutlineWorkload(List<FontResource> resources) {
            this.resources = resources;
            reference = new String[resources.size()];
            for (int f = 0; f < reference.length; f++) {
                reference[f] = outlineHash(resources.get(f));
                assignment.add(f);
            }
            assignment.add(0);
            assignment.add(0);
            pool = Executors.newFixedThreadPool(assignment.size());
        }

        int threads() {
            return assignment.size();
        }

        void reset() {
            outlines.set(0);
            mismatches.set(0);
        }

        /** {@code rounds} rounds of every glyph of the assigned font on each thread. */
        void run(int rounds) throws Exception {
            List<Future<?>> futures = new ArrayList<>();
            for (int f : assignment) {
                futures.add(pool.submit(() -> {
                    for (int round = 0; round < rounds; round++) {
                        if (!outlineHash(resources.get(f)).equals(reference[f])) {
                            mismatches.incrementAndGet();
                        }
                        outlines.addAndGet(LinuxFontStressTest.OUTLINE_GLYPHS);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(90, TimeUnit.SECONDS);
            }
        }

        @Override
        public void close() {
            pool.shutdown();
        }
    }

    /**
     * A raw {@code pango_shape} loop: the call sequence of {@code PangoGlyphLayout.layout} over the string
     * corpus, every per-call object released as production releases it ({@code pango_item_free},
     * {@code g_list_free}, {@code g_free}), with the C heap and the resident set read after the warm-up rounds
     * and at the end. A binding that kept the glyph string of each {@code pango_shape}, or its copy of the
     * UTF-16 input, shows as growth in proportion to the shape count.
     */
    private static void stressShape(GoldenMap out) throws Exception {
        OSPangoShim.ensureLoaded();
        long fontmap = OSPangoShim.pango_ft2_font_map_new();
        long context = OSPangoShim.pango_font_map_create_context(fontmap);
        long desc = OSPangoShim.pango_font_description_new();
        OSPangoShim.pango_font_description_set_family(desc, "DejaVu Sans");
        OSPangoShim.pango_font_description_set_absolute_size(desc, 12 * OSPangoShim.constant("PANGO_SCALE"));
        OSPangoShim.pango_font_description_set_stretch(desc, OSPangoShim.constant("PANGO_STRETCH_NORMAL"));
        OSPangoShim.pango_font_description_set_style(desc, OSPangoShim.constant("PANGO_STYLE_NORMAL"));
        OSPangoShim.pango_font_description_set_weight(desc, OSPangoShim.constant("PANGO_WEIGHT_NORMAL"));
        long attrs = OSPangoShim.pango_attr_list_new();
        OSPangoShim.pango_attr_list_insert(attrs, OSPangoShim.pango_attr_font_desc_new(desc));
        long[] reference = shapeCorpus(context, attrs);
        for (int round = 1; round < LinuxFontStressTest.SHAPE_WARMUP_ROUNDS; round++) {
            shapeCorpus(context, attrs);
        }
        MemorySnapshot warm = MemorySnapshot.take();
        long shapes = 0;
        int mismatches = 0;
        for (int round = 0; round < LinuxFontStressTest.SHAPE_ROUNDS; round++) {
            long[] counts = shapeCorpus(context, attrs);
            shapes += counts[0];
            if (counts[1] != reference[1]) {
                mismatches++;
            }
        }
        MemorySnapshot end = MemorySnapshot.take();
        OSPangoShim.pango_attr_list_unref(attrs);
        OSPangoShim.pango_font_description_free(desc);
        OSPangoShim.g_object_unref(context);
        OSPangoShim.g_object_unref(fontmap);
        out.put("shapes.perRound", Long.toString(reference[0]));
        out.put("shapes", Long.toString(shapes));
        out.put("referenceGlyphs", Long.toString(reference[1]));
        out.put("mismatches", Integer.toString(mismatches));
        putMemoryGrowth(out, warm, end);
    }

    /** One pass over the corpus: {@code {pango_shape calls, glyphs}}; strings GLib rejects are skipped. */
    private static long[] shapeCorpus(long context, long attrs) {
        long shapes = 0;
        long glyphs = 0;
        for (String text : LinuxFontGoldens.TEXTS) {
            long str = OSPangoShim.g_utf16_to_utf8(text.toCharArray());
            if (str == 0) {
                continue;
            }
            long length = OSPangoShim.g_utf8_strlen(str, -1);
            long end = OSPangoShim.g_utf8_offset_to_pointer(str, length);
            long items = OSPangoShim.pango_itemize(context, str, 0, (int) (end - str), attrs, 0);
            if (items != 0) {
                int count = OSPangoShim.g_list_length(items);
                for (int i = 0; i < count; i++) {
                    long item = OSPangoShim.g_list_nth_data(items, i);
                    if (item != 0) {
                        OSPangoShim.Shaped shaped = OSPangoShim.pango_shape(str, item);
                        shapes++;
                        if (shaped != null) {
                            glyphs += shaped.numGlyphs();
                        }
                        OSPangoShim.pango_item_free(item);
                    }
                }
                OSPangoShim.g_list_free(items);
            }
            OSPangoShim.g_free(str);
        }
        return new long[] {shapes, glyphs};
    }

    private static String outlineHash(FontResource resource) {
        FontStrike strike = resource.getStrike(100f, BaseTransform.IDENTITY_TRANSFORM, FontResource.AA_GREYSCALE);
        StringBuilder summaries = new StringBuilder();
        for (int glyph = 0; glyph < LinuxFontStressTest.OUTLINE_GLYPHS; glyph++) {
            Shape shape = strike.getGlyph(glyph).getShape();
            summaries.append(shape instanceof Path2D path ? LinuxFontGoldens.outlineSummary(path) : "#null")
                     .append('\n');
        }
        return FontGoldens.sha256Hex(summaries.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@code getFontConfig} and {@code populateMapsNative} concurrently, as production can call them under two
     * different monitors; each opens and closes fontconfig for itself.
     */
    private static void stressFontconfig(GoldenMap out) throws Exception {
        FontConfigManagerShim.ensureLoaded();
        String[] names = FontConfigManagerShim.fontConfigNames();
        String locale = FontConfigManagerShim.fcLocaleStr();
        String gfcReference = fontConfigSnapshot(names, locale);
        String pmReference = mapsSnapshot();
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger mismatches = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(LinuxFontStressTest.THREADS);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < LinuxFontStressTest.THREADS; t++) {
            boolean maps = t % 2 == 1;
            futures.add(pool.submit(() -> {
                for (int round = 0; round < LinuxFontStressTest.FONTCONFIG_ROUNDS; round++) {
                    String snapshot = maps ? mapsSnapshot() : fontConfigSnapshot(names, locale);
                    if (!snapshot.equals(maps ? pmReference : gfcReference)) {
                        mismatches.incrementAndGet();
                    }
                    calls.incrementAndGet();
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get(90, TimeUnit.SECONDS);
        }
        pool.shutdown();
        out.put("calls", Integer.toString(calls.get()));
        out.put("mismatches", Integer.toString(mismatches.get()));
    }

    private static String fontConfigSnapshot(String[] names, String locale) {
        FontConfigManager.FcCompFont[] fonts = FontConfigManagerShim.newLogicalFontArray(names);
        boolean result = FontConfigManagerShim.getFontConfig(locale, fonts, true);
        GoldenMap snapshot = new GoldenMap();
        LinuxFontGoldens.putFontConfig(snapshot, "gfc", result, fonts, LinuxFontGoldens::safeText);
        return FontGoldens.sha256Hex(FontGoldens.format(snapshot, List.of()).getBytes(StandardCharsets.UTF_8));
    }

    private static String mapsSnapshot() {
        FontMaps maps = FontMaps.populate(Locale.ENGLISH);
        TreeMap<String, String> entries = maps.entries(LinuxFontGoldens::safeText, LinuxFontGoldens::safeText);
        entries.put("return", Boolean.toString(maps.result()));
        return FontGoldens.sha256Hex(FontGoldens.format(entries, List.of()).getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------------------------------------
    // Timings
    // ---------------------------------------------------------------------------------------------

    /**
     * Wall-clock baselines, recorded and never asserted: every DejaVu Sans glyph decomposed at 100 px through
     * {@code FT_Load_Glyph} + {@code FT_Outline_Decompose}, and {@code PrismTextLayout} over the string corpus in
     * both directions with a composite font.
     */
    private static void timing(GoldenMap out) {
        int rounds = Integer.getInteger(LinuxFontStressTest.TIMING_ROUNDS_PROPERTY, 5);
        OSFreetypeShim.ensureLoaded();
        long[] library = new long[1];
        OSFreetypeShim.ftInitFreeType(library);
        long[] face = new long[1];
        String file = LinuxFontGoldens.pinned("dejavusans").toString();
        OSFreetypeShim.ftNewFace(library[0], (file + "\0").getBytes(), 0, face);
        OSFreetypeShim.ftSetCharSize(face[0], 0, 100 * 64, 72, 72);
        int flags = LinuxFreetypeGoldenTest.flags("outline");
        int glyphs = 0;
        long segments = 0;
        for (int round = 0; round < rounds; round++) {
            glyphs = 0;
            segments = 0;
            long start = System.nanoTime();
            for (int glyph = 0; OSFreetypeShim.ftLoadGlyph(face[0], glyph, flags) == 0; glyph++) {
                Path2D path = OSFreetypeShim.outlineDecompose(face[0]);
                glyphs++;
                segments += path == null ? 0 : path.getNumCommands();
            }
            out.put("outline.ns.r" + round, Long.toString(System.nanoTime() - start));
        }
        out.put("outline.glyphs", Integer.toString(glyphs));
        out.put("outline.segments", Long.toString(segments));
        OSFreetypeShim.ftDoneFace(face[0]);
        OSFreetypeShim.ftDoneFreeType(library[0]);

        PGFont font = PrismFontFactory.getFontFactory().createFont("DejaVu Sans", 12f);
        int layouts = 0;
        for (int round = 0; round < rounds; round++) {
            layouts = 0;
            long start = System.nanoTime();
            for (String text : LinuxFontGoldens.TEXTS) {
                for (boolean rtl : new boolean[] {false, true}) {
                    PrismTextLayout layout = new PrismTextLayout(0);
                    if (rtl) {
                        layout.setDirection(com.sun.javafx.scene.text.TextLayout.DIRECTION_RTL);
                    }
                    layout.setContent(text, font);
                    layout.getRuns();
                    layouts++;
                }
            }
            out.put("layout.ns.r" + round, Long.toString(System.nanoTime() - start));
        }
        out.put("layout.count", Integer.toString(layouts));
    }
}
