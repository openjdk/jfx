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

package test.com.sun.javafx.font.freetype;

import com.sun.javafx.font.LinuxFontOracleShim;
import com.sun.javafx.font.freetype.OSPangoShim;
import com.sun.javafx.font.freetype.PangoNativeShim;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What no golden pins about {@code PangoNative}, the {@code java.lang.foreign} binding of Pango, GLib and
 * fontconfig that replaced the thirty-three JNI natives of {@code pango.c} (commit {@code 7b43255b30}): the
 * struct layouts it reads through against the {@code sizeof}/{@code offsetof} values measured with the Pango
 * 1.57.0 and GLib 2.88.0 headers on x86_64, the null guards the C carried, the outcomes of
 * {@code FcConfigAppFontAddFile} on the paths the C could and could not register, the error a missing
 * {@code libpango-1.0.so.0} makes the first call fail with, and its release paths - every {@code *_free},
 * {@code *_unref} and {@code g_free} that {@code PangoGlyphLayout} calls - under a layout loop long enough that
 * one small object retained per layout fails a memory bound ({@link #layoutLoopRetainsNoNativeMemory}). The
 * last three change the process's fontconfig state, its library set or need a quiet heap, so they run in child
 * JVMs. Every shaping value of the binding is pinned by {@code test.com.sun.javafx.font.LinuxPangoGoldenTest}
 * against the capture of that JNI build; its string conversions by
 * {@code test.com.sun.javafx.font.JniStringCodecTest} and {@code JniStringOracleTest}.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxPangoNativeTest {

    static final Path DEJAVU_SANS = Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");

    /**
     * Options of the leak child: a fixed, pre-touched Java heap and the serial collector, so that the C heap
     * moves only for native reasons (G1's remembered sets and refinement work are C-heap allocations that grow
     * with the Java allocation churn of {@code pango_shape}'s arrays); the native heap trimmed every second so
     * that the pages of freed C allocations leave the recorded resident figure, as
     * {@code LinuxFontStressTest.MEMORY_JVM_OPTIONS} does; and a fixed number of compiler threads, because a
     * dynamically added compiler thread retires after five idle seconds and releases its arenas, which showed as
     * a shrinking C heap inside a measured phase.
     */
    static final List<String> LEAK_JVM_OPTIONS = List.of("-Xms128m", "-Xmx128m", "-XX:+AlwaysPreTouch",
            "-XX:+UseSerialGC", "-XX:TrimNativeHeapInterval=1000", "-XX:-UseDynamicNumberOfCompilerThreads");

    /**
     * The pause before each memory snapshot of the leak child: longer than the 5 s period of the task that
     * empties HotSpot's pool of freed compiler-arena chunks, which count as C heap in use until then - the
     * lesson of {@code LinuxFontStressTest.LAYOUT_SETTLE_MILLIS}.
     */
    static final long LEAK_SETTLE_MILLIS = 8_000;

    /** {@code sizeof(PangoAttrList)}, the object the leaky phase of the layout loop retains once per layout. */
    static final long PANGO_ATTR_LIST_BYTES = 16;

    /**
     * The texts of the layout loop: Latin with ligature pairs, Hebrew and Arabic with marks, a mixed-direction
     * line of several items, Devanagari, CJK, supplementary pairs, two unpaired surrogates (which GLib rejects,
     * the release path of a failed conversion), an embedded {@code U+0000} (which ends the text) and format
     * characters - the shapes of the corpus of {@code LinuxFontGoldens.TEXTS}, plus the empty string, for which
     * {@code pango_itemize} returns no list.
     */
    static final List<String> CORPUS = List.of(
            "",
            "Hello, World! fi ffl 0123",
            u(0x05E9, 0x05B8, 0x05C1, 0x05DC, 0x05D5, 0x05B9, 0x05DD),
            u(0x0645, 0x0631, 0x062D, 0x0628, 0x0627, ' ', 0x0644, 0x0627),
            "abc " + u(0x05D0, 0x05D1, 0x05D2) + " 123 " + u(0x0645, 0x0631, 0x062D) + " def",
            u(0x0915, 0x094D, 0x0937, 0x0924, 0x094D, 0x0930, 0x093F, 0x092F),
            u(0x65E5, 0x672C, 0x8A9E, 0x3002),
            u(0xD83D, 0xDE00, 0xD83D, 0xDC4D),
            "ab" + u(0xD800),
            "a" + u(0xDC00) + "b",
            u(0x05D0, 0x0000, 0x05D1),
            "a\tb" + u(0x200D, 0x200C) + "c" + u(0x00AD));

    /**
     * Layouts per text and round: both directions, as a composite font (the fallback description path) and as a
     * single font (the {@code pango_attr_fallback_new} path).
     */
    static final int LAYOUT_VARIANTS = 4;

    /**
     * Rounds before the first snapshot and rounds between the snapshots, and the C-heap bound on the growth
     * between them. Measured in the test JVM before the child recipe was chosen (G1, no settle pause, a 19-text
     * corpus): 64 to 7,952 bytes of growth per 15,200 to 30,400 layouts after 30,400 warm-up layouts, once
     * 69,392 bytes over 30,400 layouts, at 0.6 to 0.7 ms per layout.
     * <p>
     * The resident set is read with the C heap, and its growth is recorded to the test output and to every C-heap
     * failure message, and not bounded, for the reason given at {@code LinuxFontStressTest.MALLOC_GROWTH_BOUND}.
     * Where libc exports no {@code mallinfo2} the C heap is not checked, and the test output says so.
     */
    static final int LEAK_WARMUP_ROUNDS = 190;
    static final int LEAK_ROUNDS = 320;
    static final long LEAK_MALLOC_BOUND = 192L << 10;

    @BeforeAll
    static void requireTheLibraries() {
        for (String soname : PangoNativeShim.libraries()) {
            require(LinuxFontOracleShim.libraryAvailable(soname), soname + " is not installed");
        }
        Long fonts = LinuxFontOracleShim.fontconfigFontSetReads().get("list.nfont");
        require(fonts != null && fonts > 0, "fontconfig lists no fonts on this machine");
    }

    /** Skips, or fails under {@code -Djfx.parity.require=true}. */
    private static void require(boolean condition, String message) {
        if (!condition && Boolean.getBoolean("jfx.parity.require")) {
            fail(message + " (jfx.parity.require)");
        }
        assumeTrue(condition, message);
    }

    // ---------------------------------------------------------------------------------------------
    // Layouts
    // ---------------------------------------------------------------------------------------------

    /**
     * The struct layouts {@code PangoNative} reads through, against the {@code sizeof}/{@code offsetof} values
     * measured with the Pango 1.57.0 and GLib 2.88.0 headers on x86_64 (identical on every LP64 platform: every
     * field is an int, a byte or a pointer), and against what {@link LinuxFontOracleShim} reads at the same
     * offsets.
     */
    @Test
    public void layoutsMatchTheMeasuredAbi() {
        Map<String, Long> layout = PangoNativeShim.layout();
        Map<String, Long> expected = new LinkedHashMap<>();
        expected.put("PangoAnalysis.size", 48L);
        expected.put("PangoAnalysis.align", 8L);
        expected.put("PangoAnalysis.font", 16L);
        expected.put("PangoAnalysis.level", 24L);
        expected.put("PangoAnalysis.script", 27L);
        expected.put("PangoItem.size", 64L);
        expected.put("PangoItem.align", 8L);
        expected.put("PangoItem.offset", 0L);
        expected.put("PangoItem.length", 4L);
        expected.put("PangoItem.num_chars", 8L);
        expected.put("PangoItem.analysis", 16L);
        expected.put("PangoItem.analysis.font", 32L);
        expected.put("PangoItem.analysis.level", 40L);
        expected.put("PangoItem.analysis.script", 43L);
        expected.put("PangoGlyphGeometry.size", 12L);
        expected.put("PangoGlyphGeometry.width", 0L);
        expected.put("PangoGlyphInfo.size", 20L);
        expected.put("PangoGlyphInfo.align", 4L);
        expected.put("PangoGlyphInfo.glyph", 0L);
        expected.put("PangoGlyphInfo.geometry", 4L);
        expected.put("PangoGlyphInfo.geometry.width", 4L);
        expected.put("PangoGlyphInfo.attr", 16L);
        expected.put("PangoGlyphString.size", 32L);
        expected.put("PangoGlyphString.align", 8L);
        expected.put("PangoGlyphString.num_glyphs", 0L);
        expected.put("PangoGlyphString.glyphs", 8L);
        expected.put("PangoGlyphString.log_clusters", 16L);
        expected.put("PangoGlyphString.space", 24L);
        assertEquals(expected, layout);
        assertEquals(536870911, PangoNativeShim.maxGlyphs(), "INT_MAX / sizeof(jint)");

        Map<String, Long> reads = LinuxFontOracleShim.pangoGlyphStringReads(7);
        assertEquals(7L, reads.get("sized.num_glyphs"), "PangoGlyphString.num_glyphs at 0");
        assertNotEquals(0L, reads.get("sized.glyphs"), "PangoGlyphString.glyphs at 8");
        assertNotEquals(0L, reads.get("sized.log_clusters"), "PangoGlyphString.log_clusters at 16");
    }

    // ---------------------------------------------------------------------------------------------
    // Guards
    // ---------------------------------------------------------------------------------------------

    /** The null guards pango.c carried (pango.c:187, :430, :454, :464 at commit {@code 7b43255b30}). */
    @Test
    public void nullGuardsHold() {
        OSPangoShim.ensureLoaded();
        assertEquals(0L, OSPangoShim.g_utf16_to_utf8(null), "g_utf16_to_utf8(null)");
        assertEquals(0L, OSPangoShim.g_utf8_strlen(0, -1), "g_utf8_strlen(NULL)");
        assertEquals(0L, OSPangoShim.g_utf8_offset_to_pointer(0, 5), "g_utf8_offset_to_pointer(NULL)");
        long desc = OSPangoShim.pango_font_description_new();
        OSPangoShim.pango_font_description_set_family(desc, null);
        assertNull(OSPangoShim.pango_font_description_get_family(desc), "set_family(null) leaves it unset");
        OSPangoShim.pango_font_description_free(desc);

        long fontmap = OSPangoShim.pango_ft2_font_map_new();
        long context = OSPangoShim.pango_font_map_create_context(fontmap);
        long attrs = OSPangoShim.pango_attr_list_new();
        long str = OSPangoShim.g_utf16_to_utf8("abc".toCharArray());
        long items = OSPangoShim.pango_itemize(context, str, 0, 3, attrs, 0);
        assertEquals(1, OSPangoShim.g_list_length(items), "items of abc");
        long item = OSPangoShim.g_list_nth_data(items, 0);
        assertNull(OSPangoShim.pango_shape(0, item), "pango_shape(NULL, item)");
        assertNull(OSPangoShim.pango_shape(str, 0), "pango_shape(str, NULL)");
        OSPangoShim.Shaped shaped = OSPangoShim.pango_shape(str, item);
        assertTrue(shaped != null && shaped.numGlyphs() == 3, "pango_shape of abc: " + shaped);
        OSPangoShim.pango_item_free(item);
        OSPangoShim.g_list_free(items);
        OSPangoShim.g_free(str);
        OSPangoShim.pango_attr_list_unref(attrs);
        OSPangoShim.g_object_unref(context);
        OSPangoShim.g_object_unref(fontmap);
    }

    // ---------------------------------------------------------------------------------------------
    // Child JVMs: font registration and library absence
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code FcConfigAppFontAddFile} on a font, the same font again, a missing file, a file that is not a font,
     * the empty string, {@code null}, copies of the font under a Latin-1 name and under a supplementary-character
     * name (the modified UTF-8 of the path does not exist), a path with an embedded {@code U+0000} and one with
     * a lone surrogate, and a directory. Registration changes the process's fontconfig state, so this runs in
     * a child JVM.
     */
    @Test
    public void fontFileRegistrationOutcomes() throws Exception {
        assumeTrue(Files.isRegularFile(DEJAVU_SANS), DEJAVU_SANS + " is not installed");
        Map<String, String> result = runChild(Child.FONT_FILES, Map.of(), List.of());
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("dejavu", true);
        expected.put("again", true);
        expected.put("missing", false);
        expected.put("notAFont", false);
        expected.put("empty", false);
        expected.put("null", false);
        expected.put("eAcute", true);
        expected.put("supplementary", false);
        expected.put("nul", false);
        expected.put("loneSurrogate", false);
        for (Map.Entry<String, Boolean> row : expected.entrySet()) {
            assertEquals(row.getValue().toString(), result.get("row." + row.getKey()),
                         "FcConfigAppFontAddFile " + row.getKey());
        }
        String directory = result.get("row.directory");
        assertTrue("true".equals(directory) || "false".equals(directory),
                   "FcConfigAppFontAddFile directory: " + directory);
        int before = Integer.parseInt(result.get("apps.before"));
        int after = Integer.parseInt(result.get("apps.after"));
        assertEquals(0, before, "application fonts before");
        assertTrue(after >= 3, "application fonts after the three successful registrations: " + after);
    }

    /**
     * With {@code libpango-1.0.so.0} unloadable (an empty file of that name first on {@code LD_LIBRARY_PATH} of a
     * child JVM), the first call fails with {@link UnsatisfiedLinkError} naming the library and the second with
     * {@link NoClassDefFoundError}: the binding fails its initializer where the JNI library, which needed Pango,
     * failed to load.
     */
    @Test
    public void absentLibraryFailsAtTheFirstCall() throws Exception {
        Path work = workDirectory(Child.ABSENT);
        Path bogus = work.resolve("libpango-1.0.so.0");
        Files.write(bogus, new byte[0]);
        Map<String, String> result = runChild(Child.ABSENT, Map.of("LD_LIBRARY_PATH", work.toString()), List.of());
        assertEquals("ok", result.get("child"));
        assertEquals(UnsatisfiedLinkError.class.getName(), result.get("first"),
                     "first call without libpango-1.0.so.0; message: " + result.get("message"));
        assertEquals(NoClassDefFoundError.class.getName(), result.get("second"),
                     "second call without libpango-1.0.so.0");
        assertTrue(result.get("message").contains("libpango-1.0.so.0"),
                   "names the library: " + result.get("message"));
    }

    // ---------------------------------------------------------------------------------------------
    // Child JVM: the release paths under a layout loop
    // ---------------------------------------------------------------------------------------------

    /**
     * The call sequence of {@code PangoGlyphLayout.layout} and {@code dispose} - a context, a description with
     * family, size, stretch, style and weight, an attribute list with the font attribute and, for a single font,
     * the fallback attribute; {@code g_utf16_to_utf8}, {@code g_utf8_strlen}, {@code g_utf8_offset_to_pointer},
     * {@code pango_itemize}; per item {@code pango_shape} (whose glyph string the binding frees before it returns)
     * and {@code pango_item_free}; {@code g_list_free}; for a composite font {@code pango_font_describe} of each
     * item's font with its family, style and weight read and {@code pango_font_description_free}; then
     * {@code pango_attr_list_unref}, {@code pango_font_description_free}, {@code g_object_unref} of the context
     * and {@code g_free} of the text (which {@code check} and {@code dispose} do), the same three releases on the
     * text GLib rejects - over every text of {@link #CORPUS} in {@value #LAYOUT_VARIANTS} variants,
     * {@value #LEAK_ROUNDS} rounds in a child JVM after {@value #LEAK_WARMUP_ROUNDS} warm-up rounds. The C heap
     * in use may grow by less than {@value #LEAK_MALLOC_BOUND} bytes between the two snapshots: with the 12 texts
     * that is 48 layouts per round, 15,360 layouts, and 196,608 / 15,360 = 12.8 bytes per layout. Every release
     * path frees at least one object per layout: the 16-byte {@code PangoAttrList} header (a {@code guint} count
     * and a {@code GPtrArray} pointer; {@code pango-attributes-private.h}), the 24-byte {@code GList} link, the
     * UTF-8 copy of the text (its bytes plus one, so one byte for the empty text of {@link #CORPUS}), the
     * 64-byte {@code PangoItem} ({@link #layoutsMatchTheMeasuredAbi}), the 32-byte {@code PangoGlyphString} with
     * its 24 bytes per glyph, and the description with its copied family name - each a {@code g_malloc}
     * ({@code g_slice} has been {@code g_malloc} since GLib 2.76) that glibc serves from a chunk of at least 32
     * bytes, which is what {@code mallinfo2} counts. It is the chunk that governs, not the request, however
     * small: a release path forgotten anywhere in the sequence costs at least 32 bytes per layout as counted,
     * above the headroom. The child then runs the same rounds once more, retaining one 16-byte allocation per
     * layout on purpose, and that growth must reach the bound: the measurement would have caught one
     * {@code PangoAttrList} per layout in this JVM, so a pass of the first phase is not the JIT or the allocator
     * quieting down.
     */
    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void layoutLoopRetainsNoNativeMemory() throws Exception {
        assumeTrue(Files.isRegularFile(DEJAVU_SANS), DEJAVU_SANS + " is not installed");
        Map<String, String> result = runChild(Child.LEAK, Map.of(), LEAK_JVM_OPTIONS);
        int layouts = LEAK_ROUNDS * LAYOUT_VARIANTS * CORPUS.size();
        assertEquals(Integer.toString(layouts), result.get("layouts"), "layouts");
        assertTrue(Long.parseLong(result.get("shapes")) > layouts / 2, "pango_shape calls: " + result.get("shapes"));
        assertTrue(Long.parseLong(result.get("referenceGlyphs")) > 0, "glyphs in the reference round");
        assertEquals("0", result.get("mismatches"), "rounds whose glyph count differed from the first one");
        long burnin = Long.parseLong(result.get("mem.malloc.burnin"));
        long malloc = Long.parseLong(result.get("mem.malloc.growth"));
        long injected = Long.parseLong(result.get("mem.malloc.injected"));
        long resident = Long.parseLong(result.get("mem.resident.growth"));
        String recorded = "the resident set changed by " + resident + " bytes over the " + layouts
                + " layouts of the clean interval (recorded, not bounded)";
        System.out.println("[LinuxPangoNativeTest] " + recorded);
        if (burnin >= 0 || malloc != -1) {
            assertTrue(malloc < LEAK_MALLOC_BOUND, "the C heap in use grew by " + malloc + " bytes over " + layouts
                    + " layouts (bound " + LEAK_MALLOC_BOUND + "): native memory is retained per layout; " + recorded);
            assertTrue(malloc > -LEAK_MALLOC_BOUND, "the C heap in use fell by " + -malloc + " bytes over " + layouts
                    + " layouts (bound " + LEAK_MALLOC_BOUND + "; " + burnin + " over the burn-in interval before"
                    + " it): the JVM was still settling, so the interval proves nothing; " + recorded);
            assertTrue(injected >= LEAK_MALLOC_BOUND, "retaining " + PANGO_ATTR_LIST_BYTES + " bytes (a PangoAttrList)"
                    + " per layout grew the C heap in use by only " + injected + " bytes over " + layouts
                    + " layouts (bound " + LEAK_MALLOC_BOUND + "): the measurement is not settled enough to catch"
                    + " it, so the clean interval proves nothing; " + recorded);
        } else {
            System.out.println("[LinuxPangoNativeTest] the C heap in use was not checked over the " + layouts
                    + " layouts of the clean interval: libc exports no mallinfo2");
        }
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    private static Path workDirectory(String scenario) throws IOException {
        Path work = moduleDirectory().resolve("target").resolve("linux-pango-native").resolve(scenario);
        Files.createDirectories(work);
        return work;
    }

    /**
     * Runs {@link Child} in a fresh JVM with this JVM's options (its collector choice excepted, so that
     * {@code jvmOptions} may pick one), class path and native library path.
     */
    private static Map<String, String> runChild(String scenario, Map<String, String> environment,
                                                List<String> jvmOptions) throws IOException, InterruptedException {
        Path work = workDirectory(scenario);
        Path output = work.resolve("result.txt");
        Path log = work.resolve("child.log");
        Path errors = work.resolve("hs_err");
        Files.deleteIfExists(output);
        Files.createDirectories(errors);
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                    || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                    || argument.startsWith("-XX:ErrorFile") || argument.matches("-XX:\\+Use\\w+GC")) {
                continue;
            }
            command.add(argument);
        }
        command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
        command.addAll(jvmOptions);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Child.class.getName());
        command.add(scenario);
        command.add(output.toString());
        command.add(work.toString());
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(moduleDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("LANG", "C.UTF-8");
        builder.environment().putAll(environment);
        Process process = builder.start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("child JVM " + scenario + " did not finish; log:\n" + tail(log));
        }
        List<Path> errorFiles;
        try (Stream<Path> files = Files.list(errors)) {
            errorFiles = files.sorted().toList();
        }
        if (process.exitValue() != 0 || !Files.isRegularFile(output) || !errorFiles.isEmpty()) {
            fail("child JVM " + scenario + " exited with " + process.exitValue() + ", error files " + errorFiles
                    + "; log:\n" + tail(log) + "\ncommand: " + command);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(output, StandardCharsets.UTF_8)) {
            int equals = line.indexOf('=');
            if (equals > 0) {
                result.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        System.out.println("[LinuxPangoNativeTest] " + scenario + " " + result);
        return result;
    }

    private static String tail(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 40), lines.size()));
        } catch (IOException e) {
            return "(unreadable: " + e + ")";
        }
    }

    /**
     * The child JVM: {@code LinuxPangoNativeTest$Child <scenario> <result file> <work directory>}, writing
     * {@code key=value} lines. Not a test class.
     */
    public static final class Child {

        static final String FONT_FILES = "fontFiles";
        static final String ABSENT = "absent";
        static final String LEAK = "leak";

        private Child() {
        }

        public static void main(String[] args) throws Exception {
            String scenario = args[0];
            Path output = Path.of(args[1]);
            Path work = Path.of(args[2]);
            Map<String, String> result = new LinkedHashMap<>();
            switch (scenario) {
                case FONT_FILES -> fontFiles(result, work);
                case ABSENT -> absent(result);
                case LEAK -> leak(result);
                default -> throw new IllegalArgumentException("unknown scenario " + scenario);
            }
            List<String> lines = new ArrayList<>();
            result.forEach((key, value) -> lines.add(key + "=" + value));
            Files.write(output, lines, StandardCharsets.UTF_8);
            System.exit(0);
        }

        private static void fontFiles(Map<String, String> result, Path work) throws IOException {
            Path notAFont = work.resolve("notafont.ttf");
            Files.writeString(notAFont, "not a font\n", StandardCharsets.US_ASCII);
            Path eAcute = work.resolve("caf" + u(0x00E9) + ".ttf");
            Path supplementary = work.resolve("smile" + u(0xD83D, 0xDE00) + ".ttf");
            Files.copy(DEJAVU_SANS, eAcute, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(DEJAVU_SANS, supplementary, StandardCopyOption.REPLACE_EXISTING);
            String[][] rows = {
                {"dejavu", DEJAVU_SANS.toString()}, {"again", DEJAVU_SANS.toString()},
                {"missing", work.resolve("none.ttf").toString()}, {"notAFont", notAFont.toString()},
                {"empty", ""}, {"null", null}, {"eAcute", eAcute.toString()},
                {"supplementary", supplementary.toString()},
                {"nul", work.toString() + "/a" + u(0x0000) + "b.ttf"},
                {"loneSurrogate", work.toString() + "/x" + u(0xD800) + ".ttf"},
                {"directory", work.toString()}
            };
            result.put("apps.before", Integer.toString(LinuxFontOracleShim.fcApplicationFontCount()));
            for (String[] row : rows) {
                result.put("row." + row[0], Boolean.toString(OSPangoShim.FcConfigAppFontAddFile(0, row[1])));
            }
            result.put("apps.after", Integer.toString(LinuxFontOracleShim.fcApplicationFontCount()));
        }

        private static void absent(Map<String, String> result) {
            result.put("child", "ok");
            result.put("first", attempt(result, "message"));
            result.put("second", attempt(result, null));
        }

        private static String attempt(Map<String, String> result, String messageKey) {
            try {
                long desc = OSPangoShim.pango_font_description_new();
                OSPangoShim.pango_font_description_free(desc);
                return "ok";
            } catch (Throwable t) {
                if (messageKey != null) {
                    result.put(messageKey, String.valueOf(t.getMessage()).replace('\n', ' '));
                }
                return t.getClass().getName();
            }
        }

        /** The layout loop of {@link #layoutLoopRetainsNoNativeMemory}, on one font map as production keeps one. */
        private static void leak(Map<String, String> result) throws Exception {
            OSPangoShim.ensureLoaded();
            long fontmap = OSPangoShim.pango_ft2_font_map_new();
            long[] reference = round(fontmap);
            for (int i = 1; i < LEAK_WARMUP_ROUNDS; i++) {
                round(fontmap);
            }
            long[] start = snapshot();
            for (int i = 0; i < LEAK_ROUNDS; i++) {
                round(fontmap);
            }
            long[] warm = snapshot();
            long layouts = 0;
            long shapes = 0;
            int mismatches = 0;
            for (int i = 0; i < LEAK_ROUNDS; i++) {
                long[] counts = round(fontmap);
                layouts += counts[0];
                shapes += counts[1];
                if (counts[2] != reference[2]) {
                    mismatches++;
                }
            }
            long[] end = snapshot();
            for (int i = 0; i < LEAK_ROUNDS; i++) {
                round(fontmap, PANGO_ATTR_LIST_BYTES);
            }
            long[] leaky = snapshot();
            OSPangoShim.g_object_unref(fontmap);
            result.put("layouts", Long.toString(layouts));
            result.put("shapes", Long.toString(shapes));
            result.put("referenceGlyphs", Long.toString(reference[2]));
            result.put("mismatches", Integer.toString(mismatches));
            result.put("mem.malloc.burnin", Long.toString(start[0] < 0 ? -1 : warm[0] - start[0]));
            result.put("mem.malloc.warm", Long.toString(warm[0]));
            result.put("mem.malloc.end", Long.toString(end[0]));
            result.put("mem.malloc.growth", Long.toString(warm[0] < 0 ? -1 : end[0] - warm[0]));
            result.put("mem.malloc.injected", Long.toString(warm[0] < 0 ? -1 : leaky[0] - end[0]));
            result.put("mem.resident.burnin", Long.toString(warm[1] - start[1]));
            result.put("mem.resident.warm", Long.toString(warm[1]));
            result.put("mem.resident.end", Long.toString(end[1]));
            result.put("mem.resident.growth", Long.toString(end[1] - warm[1]));
            result.put("mem.resident.injected", Long.toString(leaky[1] - end[1]));
        }

        /** One round over the corpus in every variant: {@code {layouts, pango_shape calls, glyphs}}. */
        private static long[] round(long fontmap) {
            return round(fontmap, 0);
        }

        /** {@link #round(long)}, retaining {@code retainedBytes} of native memory per layout when non-zero. */
        private static long[] round(long fontmap, long retainedBytes) {
            long[] counts = new long[3];
            for (String text : CORPUS) {
                for (int variant = 0; variant < LAYOUT_VARIANTS; variant++) {
                    long[] one = layout(fontmap, text, (variant & 1) != 0, (variant & 2) != 0);
                    counts[0]++;
                    counts[1] += one[0];
                    counts[2] += one[1];
                    if (retainedBytes != 0) {
                        Arena.global().allocate(retainedBytes);
                    }
                }
            }
            return counts;
        }

        /**
         * {@code PangoGlyphLayout.layout} followed by its {@code dispose}, call for call, with the glyph reads
         * of {@code pango_shape} and, for a composite font, the fallback description reads of {@code getSlot}:
         * {@code {pango_shape calls, glyphs}}.
         */
        private static long[] layout(long fontmap, String text, boolean rtl, boolean composite) {
            long context = OSPangoShim.pango_font_map_create_context(fontmap);
            if (rtl) {
                OSPangoShim.pango_context_set_base_dir(context, OSPangoShim.constant("PANGO_DIRECTION_RTL"));
            }
            long desc = OSPangoShim.pango_font_description_new();
            OSPangoShim.pango_font_description_set_family(desc, "DejaVu Sans");
            OSPangoShim.pango_font_description_set_absolute_size(desc, 12 * OSPangoShim.constant("PANGO_SCALE"));
            OSPangoShim.pango_font_description_set_stretch(desc, OSPangoShim.constant("PANGO_STRETCH_NORMAL"));
            OSPangoShim.pango_font_description_set_style(desc, OSPangoShim.constant("PANGO_STYLE_NORMAL"));
            OSPangoShim.pango_font_description_set_weight(desc, OSPangoShim.constant("PANGO_WEIGHT_NORMAL"));
            long attrList = OSPangoShim.pango_attr_list_new();
            OSPangoShim.pango_attr_list_insert(attrList, OSPangoShim.pango_attr_font_desc_new(desc));
            if (!composite) {
                OSPangoShim.pango_attr_list_insert(attrList, OSPangoShim.pango_attr_fallback_new(false));
            }
            long str = OSPangoShim.g_utf16_to_utf8(text.toCharArray());
            if (str == 0) {
                release(context, desc, attrList);
                return new long[] {0, 0};
            }
            long utflen = OSPangoShim.g_utf8_strlen(str, -1);
            long end = OSPangoShim.g_utf8_offset_to_pointer(str, utflen);
            long runs = OSPangoShim.pango_itemize(context, str, 0, (int) (end - str), attrList, 0);
            long shapes = 0;
            long glyphs = 0;
            if (runs != 0) {
                int count = OSPangoShim.g_list_length(runs);
                OSPangoShim.Shaped[] shaped = new OSPangoShim.Shaped[count];
                for (int i = 0; i < count; i++) {
                    long item = OSPangoShim.g_list_nth_data(runs, i);
                    if (item != 0) {
                        shaped[i] = OSPangoShim.pango_shape(str, item);
                        OSPangoShim.pango_item_free(item);
                    }
                }
                OSPangoShim.g_list_free(runs);
                for (OSPangoShim.Shaped g : shaped) {
                    if (g != null) {
                        shapes++;
                        glyphs += g.numGlyphs();
                        if (composite) {
                            long fallback = OSPangoShim.pango_font_describe(g.font());
                            OSPangoShim.pango_font_description_get_family(fallback);
                            OSPangoShim.pango_font_description_get_style(fallback);
                            OSPangoShim.pango_font_description_get_weight(fallback);
                            OSPangoShim.pango_font_description_free(fallback);
                        }
                    }
                }
            }
            release(context, desc, attrList);
            OSPangoShim.g_free(str);
            return new long[] {shapes, glyphs};
        }

        /** {@code PangoGlyphLayout.check(0, null, context, desc, attrList)}: the three releases, in its order. */
        private static void release(long context, long desc, long attrList) {
            OSPangoShim.pango_attr_list_unref(attrList);
            OSPangoShim.pango_font_description_free(desc);
            OSPangoShim.g_object_unref(context);
        }

        /**
         * The C heap in use and the resident set, after the settle pause and two collections so that dropped Java
         * objects and pooled compiler chunks do not count.
         */
        private static long[] snapshot() throws InterruptedException {
            Thread.sleep(LEAK_SETTLE_MILLIS);
            System.gc();
            Thread.sleep(100);
            System.gc();
            return new long[] {LinuxFontOracleShim.mallocInUseBytes(), LinuxFontOracleShim.residentSetBytes()};
        }
    }

    /** A string from code units, so that no source line carries a non-ASCII character or a unicode escape. */
    private static String u(int... units) {
        char[] chars = new char[units.length];
        for (int i = 0; i < units.length; i++) {
            chars[i] = (char) units[i];
        }
        return new String(chars);
    }
}
