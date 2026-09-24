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
import com.sun.javafx.font.freetype.FTNativeShim;
import com.sun.javafx.font.freetype.OSFreetypeShim;
import com.sun.javafx.geom.Path2D;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What no golden pins about {@code FTNative}, the {@code java.lang.foreign} binding of {@code libfreetype.so.6}
 * that replaced the fourteen JNI natives of {@code freetype.c} (commit {@code 7b43255b30}): the record layouts
 * it dereferences against the {@code sizeof}/{@code offsetof} values measured with the FreeType 2.14.2 headers on
 * x86_64, its loader's error mapping for a library that cannot be opened, the failure path of its outline
 * callbacks, the build flags the Linux build defined, and its release paths - {@code FT_Done_Face},
 * {@code FT_Done_FreeType} and the per-call scratch of every downcall - under loops long enough that one small
 * object retained per call fails a memory bound ({@link #faceLoopRetainsNoNativeMemory},
 * {@link #outlineLoopRetainsNoNativeMemory}, each in a child JVM). Every value of the binding itself - glyph
 * slots, bitmaps, outlines, transforms, the {@code FT_New_Face} out parameter - is pinned by
 * {@code test.com.sun.javafx.font.LinuxFreetypeGoldenTest} against the capture of that JNI build.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFreetypeNativeTest {

    static final Path DEJAVU_SANS = Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");

    /** DejaVu Sans glyph id of {@code g}, which has conic segments. */
    static final int GLYPH_G = 74;

    /** A value no library or face handle can be, to see whether an out parameter was written. */
    static final long SENTINEL = 0x5EED5EED5EEDL;
    static final String NULL = "#null";
    static final String EMPTY_PATH = "0;0;0;0;0||";

    /**
     * Options of the leak children: a fixed, pre-touched Java heap and the serial collector, so that the C heap
     * moves only for native reasons (G1's remembered sets and refinement work are C-heap allocations that grow
     * with Java allocation churn); the native heap trimmed every second so that the pages of freed C allocations
     * leave the recorded resident figure, as {@code LinuxFontStressTest.MEMORY_JVM_OPTIONS} does; and a fixed
     * number of compiler threads, because a dynamically added compiler thread retires after five idle seconds and
     * releases its arenas, which showed as a shrinking C heap inside a measured phase.
     */
    static final List<String> LEAK_JVM_OPTIONS = List.of("-Xms128m", "-Xmx128m", "-XX:+AlwaysPreTouch",
            "-XX:+UseSerialGC", "-XX:TrimNativeHeapInterval=1000", "-XX:-UseDynamicNumberOfCompilerThreads");

    /**
     * The pause before each memory snapshot of a leak child: longer than the 5 s period of the task that empties
     * HotSpot's pool of freed compiler-arena chunks, which count as C heap in use until then - the lesson of
     * {@code LinuxFontStressTest.LAYOUT_SETTLE_MILLIS}, whose warm-up also passed the tier-4 compile thresholds
     * of the per-call methods before the first snapshot; the warm-up counts below do the same.
     */
    static final long LEAK_SETTLE_MILLIS = 8_000;

    /**
     * Face loop: warm-up faces before the first snapshot, faces between the snapshots, and its C-heap bound.
     * Measured in the test JVM itself before the child recipe was chosen (G1, no settle pause): 592 to 6,816
     * bytes of growth over 2,000 faces after 1,000 warm-up faces, at 110 us per face. A 512-face warm-up left
     * the compilation of the per-face methods inside the measured phase (the C heap fell by 6.8 MB there), so
     * the warm-up is twice the tier-4 invocation threshold of 5,000.
     */
    static final int FACE_WARMUP = 10_000;
    static final int FACE_LOOPS = 4_096;
    static final long FACE_MALLOC_BOUND = 128L << 10;

    /** {@code sizeof(FT_FaceRec)}, the object the leaky phase of the face loop retains once per face. */
    static final long FT_FACE_REC_BYTES = 248;

    /**
     * Outline loop: warm-up passes over every glyph of the face, measured passes, and its C-heap bound. Measured
     * the same way: 256 to 25,712 bytes of growth over 16 passes (100,048 decompositions) after 6 warm-up passes,
     * at 16 ms per pass; one sample was -21.5 MB, the chunk pool being emptied inside the measured phase, which
     * the settle pause before each snapshot rules out.
     */
    static final int OUTLINE_WARMUP_PASSES = 5;
    static final int OUTLINE_PASSES = 32;
    static final long OUTLINE_MALLOC_BOUND = 256L << 10;

    /**
     * {@code DEFAULT_LEN_TYPES} (freetype.c:495), the allocation the leaky phase of the outline loop retains per
     * decomposition.
     */
    static final long POINT_TYPES_BYTES = 10;

    /** The 16.16 matrix of a slight slant, so that {@code FT_Set_Transform} is called as {@code initGlyph} calls it. */
    static final long[] SLANT = {0x10000, 0x4000, 0, 0x10000};

    @Test
    public void layoutsMatchTheMeasuredAbi() {
        Map<String, Long> expected = new LinkedHashMap<>();
        expected.put("sizeof.FT_Vector", 16L);
        expected.put("sizeof.FT_Matrix", 32L);
        expected.put("sizeof.FT_Glyph_Metrics", 64L);
        expected.put("sizeof.FT_Bitmap", 40L);
        expected.put("sizeof.FT_Outline", 40L);
        expected.put("sizeof.FT_Outline_Funcs", 48L);
        expected.put("sizeof.FT_GlyphSlotRec", 304L);
        expected.put("sizeof.FT_FaceRec", 248L);
        expected.put("offsetof.FT_Vector.x", 0L);
        expected.put("offsetof.FT_Vector.y", 8L);
        expected.put("offsetof.FT_FaceRec.glyph", 152L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.width", 48L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.height", 56L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.horiBearingX", 64L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.horiBearingY", 72L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.horiAdvance", 80L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.vertBearingX", 88L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.vertBearingY", 96L);
        expected.put("offsetof.FT_GlyphSlotRec.metrics.vertAdvance", 104L);
        expected.put("offsetof.FT_GlyphSlotRec.linearHoriAdvance", 112L);
        expected.put("offsetof.FT_GlyphSlotRec.linearVertAdvance", 120L);
        expected.put("offsetof.FT_GlyphSlotRec.advance.x", 128L);
        expected.put("offsetof.FT_GlyphSlotRec.advance.y", 136L);
        expected.put("offsetof.FT_GlyphSlotRec.format", 144L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.rows", 152L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.width", 156L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.pitch", 160L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.buffer", 168L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.num_grays", 176L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.pixel_mode", 178L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.palette_mode", 179L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap.palette", 184L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap_left", 192L);
        expected.put("offsetof.FT_GlyphSlotRec.bitmap_top", 196L);
        expected.put("offsetof.FT_GlyphSlotRec.outline", 200L);
        expected.put("offsetof.FT_Outline_Funcs.move_to", 0L);
        expected.put("offsetof.FT_Outline_Funcs.line_to", 8L);
        expected.put("offsetof.FT_Outline_Funcs.conic_to", 16L);
        expected.put("offsetof.FT_Outline_Funcs.cubic_to", 24L);
        expected.put("offsetof.FT_Outline_Funcs.shift", 32L);
        expected.put("offsetof.FT_Outline_Funcs.delta", 40L);
        assertEquals(expected, FTNativeShim.layout());
    }

    @Test
    public void missingLibraryIsAnUnsatisfiedLinkError() {
        String absent = "libjavafx-absent-for-this-test.so.0";
        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class, () -> FTNativeShim.load(absent));
        assertTrue(error.getMessage().contains(absent), error.getMessage());
        assertNotNull(error.getCause(), "the loader's own exception is kept as the cause");
        FTNativeShim.load(FTNativeShim.library());
    }

    /** The Linux build of {@code freetype.c} defined {@code _ENABLE_PANGO} and never {@code _ENABLE_HARFBUZZ}. */
    @Test
    public void buildFlagsAreTheLinuxOnes() {
        OSFreetypeShim.ensureLoaded();
        assertTrue(OSFreetypeShim.pangoEnabled(), "isPangoEnabled");
        assertFalse(OSFreetypeShim.harfbuzzEnabled(), "isHarfbuzzEnabled");
    }

    /**
     * An outline callback that throws while FreeType is still calling aborts the decomposition with {@code null}
     * and no crash, records the failure, and leaves the slot decomposable: the next normal decomposition of the
     * same slot gives the outline it gave before.
     */
    @Test
    public void failingCallbackGivesNullNotACrash() {
        assumeTrue(Files.isRegularFile(DEJAVU_SANS), DEJAVU_SANS + " is not installed");
        OSFreetypeShim.ensureLoaded();
        long library = library();
        long face = face(library, DEJAVU_SANS);
        assertEquals(0, OSFreetypeShim.ftSetCharSize(face, 0, 12 * 64, 72, 72));
        assertEquals(0, OSFreetypeShim.ftLoadGlyph(face, GLYPH_G, outlineFlags()));
        String expected = outline(OSFreetypeShim.outlineDecompose(face));
        assertNotEquals(EMPTY_PATH, expected, "g at 12 pt has segments");
        assertNotEquals(NULL, expected);

        FTNativeShim.LimitedDecompose limited = FTNativeShim.decomposeLimited(face, 2);
        assertNull(limited.path(), "a callback that fails aborts the decomposition");
        assertEquals(2, limited.segmentsAccepted());
        assertNotNull(limited.failure());
        assertTrue(limited.failure().startsWith(IllegalStateException.class.getName()), limited.failure());
        FTNativeShim.LimitedDecompose none = FTNativeShim.decomposeLimited(face, 0);
        assertNull(none.path());
        assertEquals(0, none.segmentsAccepted());

        FTNativeShim.LimitedDecompose unlimited = FTNativeShim.decomposeLimited(face, Integer.MAX_VALUE);
        assertNull(unlimited.failure());
        assertEquals(expected, outline(unlimited.path()), "the same slot decomposes normally afterwards");
        assertEquals(expected, outline(OSFreetypeShim.outlineDecompose(face)));
        assertEquals(0, OSFreetypeShim.ftDoneFace(face));
        assertEquals(0, OSFreetypeShim.ftDoneFreeType(library));
    }

    // ---------------------------------------------------------------------------------------------
    // Release paths: leak loops in child JVMs
    // ---------------------------------------------------------------------------------------------

    /**
     * The life of an {@code FTFontFile} - {@code FT_Init_FreeType}, {@code FT_New_Face}, {@code FT_Set_Char_Size},
     * {@code FT_Load_Glyph} and {@code FT_Outline_Decompose} ({@code createGlyphOutline}), {@code FT_Set_Transform},
     * a rendering {@code FT_Load_Glyph} with {@code getGlyphSlot} and {@code getBitmapData} ({@code initGlyph}),
     * then {@code FT_Done_Face} and {@code FT_Done_FreeType} in the {@code FTDisposer} order - {@value #FACE_LOOPS}
     * times in a child JVM after {@value #FACE_WARMUP} warm-up faces. The C heap in use may grow by less than
     * {@value #FACE_MALLOC_BOUND} bytes between the two snapshots: 131,072 / 4,096 = 32 bytes per face, below the
     * 248-byte {@code FT_FaceRec} (its {@code sizeof}, {@link #layoutsMatchTheMeasuredAbi}), the smallest object
     * {@code FT_Done_Face} frees and the least a forgotten release would retain per face, before the glyph slot,
     * the size and the driver records that go with it. FreeType maps the font file once per open face and unmaps
     * it in {@code FT_Done_Face}, so the file's mappings must be back to none at the end. The binding's own
     * per-call scratch (the out parameters, the path bytes, the matrix) lives in confined arenas and may not
     * survive them either. The child then runs the same {@value #FACE_LOOPS} faces once more, retaining one
     * 248-byte allocation per face on purpose, and that growth must reach the bound: the measurement would have
     * caught one {@code FT_FaceRec} per face in this JVM, so a pass of the first phase is not the JIT or the
     * allocator quieting down.
     */
    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void faceLoopRetainsNoNativeMemory() throws Exception {
        assumeTrue(Files.isRegularFile(DEJAVU_SANS), DEJAVU_SANS + " is not installed");
        Map<String, String> result = runChild(Child.FACES, LEAK_JVM_OPTIONS);
        assertEquals(Integer.toString(FACE_LOOPS), result.get("faces"), "faces");
        assertEquals("0", result.get("failures"), "faces on which a FreeType call failed");
        assertEquals("0", result.get("mismatches"), "faces whose outline, slot or bitmap differed from the first");
        assertEquals("0", result.get("maps.font.end"), "mappings of the font file after the last FT_Done_Face");
        assertNativeMemoryBounded(result, FACE_MALLOC_BOUND, FACE_LOOPS + " faces",
                                  FT_FACE_REC_BYTES + " bytes (an FT_FaceRec) per face");
    }

    /**
     * Every glyph of DejaVu Sans at 100 px loaded and decomposed ({@code FT_Load_Glyph} + {@code FT_Outline_Decompose},
     * what {@code FTFontFile.createGlyphOutline} does) for {@value #OUTLINE_PASSES} passes over one face in a child
     * JVM, after {@value #OUTLINE_WARMUP_PASSES} warm-up passes. The C heap in use may grow by less than
     * {@value #OUTLINE_MALLOC_BOUND} bytes over the measured passes: with the 6,253 glyphs of DejaVu Sans 2.37 that
     * is 262,144 / 200,096 = 1.3 bytes per decomposition, below the 10-byte {@code pointTypes} buffer
     * ({@code DEFAULT_LEN_TYPES}, freetype.c:495), the smallest allocation the C freed after each decomposition
     * (glibc serves it from a 32-byte chunk, which is what {@code mallinfo2} counts). The binding allocates nothing
     * native per decomposition - the accumulator is Java, the callbacks and their table are process-wide - so any
     * growth in proportion to the count is a regression: a stub or a table made per call, or an arena not closed.
     * The child then runs the same passes once more, retaining one 10-byte allocation per decomposition on
     * purpose, and that growth must reach the bound.
     */
    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void outlineLoopRetainsNoNativeMemory() throws Exception {
        assumeTrue(Files.isRegularFile(DEJAVU_SANS), DEJAVU_SANS + " is not installed");
        Map<String, String> result = runChild(Child.OUTLINES, LEAK_JVM_OPTIONS);
        long glyphs = Long.parseLong(result.get("glyphs"));
        assertTrue(glyphs > 1000, "glyphs in the face: " + glyphs);
        assertTrue(Long.parseLong(result.get("segments")) > 0, "segments in a pass");
        assertEquals(Long.toString(glyphs * OUTLINE_PASSES), result.get("outlines"), "decompositions");
        assertEquals("0", result.get("mismatches"), "passes whose segment count differed from the first one");
        assertNativeMemoryBounded(result, OUTLINE_MALLOC_BOUND, result.get("outlines") + " decompositions",
                                  POINT_TYPES_BYTES + " bytes (the C's pointTypes buffer) per decomposition");
    }

    /**
     * The {@code mem.*} keys of a leak child against the bounds: the C heap in use may move by less than the bound
     * in either direction over the clean interval (a fall of more than the bound would be the JVM still settling
     * and could mask what the bound is for), and must grow by at least the bound over the leaky interval (one
     * {@code retained} allocation per iteration). The malloc figures are {@code -1} where libc lacks
     * {@code mallinfo2}; the C heap is then not checked, and the test output says so. The growth of the resident
     * set over the clean interval is recorded to the test output and to every C-heap failure message, and not
     * bounded, for the reason given at {@code LinuxFontStressTest.MALLOC_GROWTH_BOUND}.
     */
    private static void assertNativeMemoryBounded(Map<String, String> result, long mallocBound, String work,
                                                  String retained) {
        long burnin = Long.parseLong(result.get("mem.malloc.burnin"));
        long malloc = Long.parseLong(result.get("mem.malloc.growth"));
        long injected = Long.parseLong(result.get("mem.malloc.injected"));
        long resident = Long.parseLong(result.get("mem.resident.growth"));
        String recorded = "the resident set changed by " + resident + " bytes over the " + work
                + " of the clean interval (recorded, not bounded)";
        System.out.println("[LinuxFreetypeNativeTest] " + recorded);
        if (burnin >= 0 || malloc != -1) {
            assertTrue(malloc < mallocBound, "the C heap in use grew by " + malloc + " bytes over " + work
                    + " (bound " + mallocBound + "): native memory is retained per call; " + recorded);
            assertTrue(malloc > -mallocBound, "the C heap in use fell by " + -malloc + " bytes over " + work
                    + " (bound " + mallocBound + "; " + burnin + " over the burn-in interval before it): the JVM"
                    + " was still settling, so the interval proves nothing; " + recorded);
            assertTrue(injected >= mallocBound, "retaining " + retained + " grew the C heap in use by only "
                    + injected + " bytes over " + work + " (bound " + mallocBound + "): the measurement is not"
                    + " settled enough to catch it, so the clean interval proves nothing; " + recorded);
        } else {
            System.out.println("[LinuxFreetypeNativeTest] the C heap in use was not checked over the " + work
                    + " of the clean interval: libc exports no mallinfo2");
        }
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    /**
     * Runs {@link Child} in a fresh JVM with this JVM's options (its collector choice excepted, so that
     * {@code jvmOptions} may pick one) and class path.
     */
    private static Map<String, String> runChild(String scenario, List<String> jvmOptions)
            throws IOException, InterruptedException {
        Path work = moduleDirectory().resolve("target").resolve("linux-freetype-native").resolve(scenario);
        Files.createDirectories(work);
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
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(moduleDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("LANG", "C.UTF-8");
        Process process = builder.start();
        if (!process.waitFor(150, TimeUnit.SECONDS)) {
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
        System.out.println("[LinuxFreetypeNativeTest] " + scenario + " " + result);
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
     * The child JVM of the leak loops: {@code LinuxFreetypeNativeTest$Child <scenario> <result file>}, writing
     * {@code key=value} lines. Not a test class.
     */
    public static final class Child {

        static final String FACES = "faces";
        static final String OUTLINES = "outlines";

        private Child() {
        }

        public static void main(String[] args) throws Exception {
            String scenario = args[0];
            Path output = Path.of(args[1]);
            Map<String, String> result = new LinkedHashMap<>();
            OSFreetypeShim.ensureLoaded();
            switch (scenario) {
                case FACES -> faces(result);
                case OUTLINES -> outlines(result);
                default -> throw new IllegalArgumentException("unknown scenario " + scenario);
            }
            List<String> lines = new ArrayList<>();
            result.forEach((key, value) -> lines.add(key + "=" + value));
            Files.write(output, lines, StandardCharsets.UTF_8);
            System.exit(0);
        }

        private static void faces(Map<String, String> result) throws Exception {
            String mapped = DEJAVU_SANS.toRealPath().toString();
            byte[] path = cString(DEJAVU_SANS.toString());
            String reference = face(path);
            if (reference == null) {
                throw new IllegalStateException("the first face failed");
            }
            for (int i = 1; i < FACE_WARMUP; i++) {
                face(path);
            }
            long[] start = snapshot();
            for (int i = 0; i < FACE_LOOPS; i++) {
                face(path);
            }
            long[] warm = snapshot();
            int faces = 0;
            int failures = 0;
            int mismatches = 0;
            for (int i = 0; i < FACE_LOOPS; i++) {
                String summary = face(path);
                faces++;
                if (summary == null) {
                    failures++;
                } else if (!summary.equals(reference)) {
                    mismatches++;
                }
            }
            long[] end = snapshot();
            int mappings = fileMappings(mapped);
            for (int i = 0; i < FACE_LOOPS; i++) {
                face(path);
                retain(FT_FACE_REC_BYTES);
            }
            long[] leaky = snapshot();
            result.put("faces", Integer.toString(faces));
            result.put("failures", Integer.toString(failures));
            result.put("mismatches", Integer.toString(mismatches));
            result.put("maps.font.end", Integer.toString(mappings));
            putMemory(result, start, warm, end, leaky);
        }

        /**
         * One {@code FTFontFile} life on its own {@code FT_Library}; a summary of the outline, the slot and the
         * bitmap it read, or {@code null} when a FreeType call failed. The face is released before its library,
         * as {@code FTDisposer.dispose} releases them.
         */
        private static String face(byte[] path) {
            long[] library = {SENTINEL};
            if (OSFreetypeShim.ftInitFreeType(library) != 0) {
                return null;
            }
            long[] face = {SENTINEL};
            String summary = null;
            if (OSFreetypeShim.ftNewFace(library[0], path, 0, face) == 0) {
                if (OSFreetypeShim.ftSetCharSize(face[0], 0, 12 * 64, 72, 72) == 0
                        && OSFreetypeShim.ftLoadGlyph(face[0], GLYPH_G, outlineFlags()) == 0) {
                    Path2D outline = OSFreetypeShim.outlineDecompose(face[0]);
                    OSFreetypeShim.ftSetTransform(face[0], SLANT, 0, 0);
                    if (OSFreetypeShim.ftLoadGlyph(face[0], GLYPH_G, renderFlags()) == 0) {
                        long[] slot = OSFreetypeShim.glyphSlot(face[0]);
                        byte[] bitmap = OSFreetypeShim.bitmapData(face[0]);
                        summary = outline(outline) + "|" + Arrays.toString(slot) + "|"
                                + (bitmap == null ? -1 : bitmap.length) + ";" + Arrays.hashCode(bitmap);
                    }
                }
                OSFreetypeShim.ftDoneFace(face[0]);
            }
            OSFreetypeShim.ftDoneFreeType(library[0]);
            return summary;
        }

        private static void outlines(Map<String, String> result) throws Exception {
            byte[] path = cString(DEJAVU_SANS.toString());
            long[] library = {SENTINEL};
            long[] face = {SENTINEL};
            if (OSFreetypeShim.ftInitFreeType(library) != 0 || OSFreetypeShim.ftNewFace(library[0], path, 0, face) != 0
                    || OSFreetypeShim.ftSetCharSize(face[0], 0, 100 * 64, 72, 72) != 0) {
                throw new IllegalStateException("cannot open " + DEJAVU_SANS + " at 100 px");
            }
            long[] reference = pass(face[0]);
            for (int i = 1; i < OUTLINE_WARMUP_PASSES; i++) {
                pass(face[0]);
            }
            long[] start = snapshot();
            for (int i = 0; i < OUTLINE_PASSES; i++) {
                pass(face[0]);
            }
            long[] warm = snapshot();
            long outlines = 0;
            int mismatches = 0;
            for (int i = 0; i < OUTLINE_PASSES; i++) {
                long[] counts = pass(face[0]);
                outlines += counts[0];
                if (counts[1] != reference[1]) {
                    mismatches++;
                }
            }
            long[] end = snapshot();
            for (int i = 0; i < OUTLINE_PASSES; i++) {
                pass(face[0], POINT_TYPES_BYTES);
            }
            long[] leaky = snapshot();
            OSFreetypeShim.ftDoneFace(face[0]);
            OSFreetypeShim.ftDoneFreeType(library[0]);
            result.put("glyphs", Long.toString(reference[0]));
            result.put("segments", Long.toString(reference[1]));
            result.put("outlines", Long.toString(outlines));
            result.put("mismatches", Integer.toString(mismatches));
            putMemory(result, start, warm, end, leaky);
        }

        /** Every glyph of the face decomposed once: {@code {glyphs, segments}}. */
        private static long[] pass(long face) {
            return pass(face, 0);
        }

        /** {@link #pass(long)}, retaining {@code retainedBytes} of native memory per decomposition when non-zero. */
        private static long[] pass(long face, long retainedBytes) {
            long glyphs = 0;
            long segments = 0;
            for (int glyph = 0; OSFreetypeShim.ftLoadGlyph(face, glyph, outlineFlags()) == 0; glyph++) {
                Path2D outline = OSFreetypeShim.outlineDecompose(face);
                glyphs++;
                segments += outline == null ? 0 : outline.getNumCommands();
                if (retainedBytes != 0) {
                    retain(retainedBytes);
                }
            }
            return new long[] {glyphs, segments};
        }

        /**
         * Retains {@code bytes} of native memory for the rest of the process - {@code malloc} through the global
         * arena, which never frees - as a forgotten release of an object of that size would.
         */
        private static void retain(long bytes) {
            Arena.global().allocate(bytes);
        }

        /** The load flags of {@code FTFontFile.initGlyph} for a greyscale glyph under a non-identity transform. */
        private static int renderFlags() {
            return OSFreetypeShim.constant("FT_LOAD_RENDER") | OSFreetypeShim.constant("FT_LOAD_NO_HINTING")
                    | OSFreetypeShim.constant("FT_LOAD_NO_BITMAP") | OSFreetypeShim.constant("FT_LOAD_TARGET_NORMAL");
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

        /**
         * {@code mem.malloc.*} and {@code mem.resident.*}: {@code burnin}, the growth of the first, unasserted
         * clean interval (where a child that warmed up for seconds released 1.3 to 7.3 MB of C heap once, whatever
         * the settle pause and the collections before its first reading - the JVM retiring the warm-up's
         * superseded code and hidden classes); {@code warm}, {@code end} and {@code growth} of the asserted clean
         * interval; and {@code injected}, the growth of the leaky interval. Malloc figures are {@code -1} where
         * libc lacks {@code mallinfo2}.
         */
        private static void putMemory(Map<String, String> result, long[] start, long[] warm, long[] end,
                                      long[] leaky) {
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

        /** Lines of {@code /proc/self/maps} that map {@code file}, given as the real path the kernel reports. */
        private static int fileMappings(String file) throws IOException {
            int count = 0;
            for (String line : Files.readAllLines(Path.of("/proc/self/maps"), StandardCharsets.ISO_8859_1)) {
                if (line.contains(" " + file)) {
                    count++;
                }
            }
            return count;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static long library() {
        long[] library = {SENTINEL};
        assertEquals(0, OSFreetypeShim.ftInitFreeType(library), "FT_Init_FreeType");
        assertNotEquals(SENTINEL, library[0]);
        assertNotEquals(0L, library[0]);
        return library[0];
    }

    private static long face(long library, Path font) {
        long[] face = {SENTINEL};
        assertEquals(0, OSFreetypeShim.ftNewFace(library, cString(font.toString()), 0, face), "FT_New_Face " + font);
        assertNotEquals(SENTINEL, face[0]);
        return face[0];
    }

    /** The path bytes exactly as {@code FTFontFile.init} and {@code FTFactory.registerEmbeddedFont} pass them. */
    private static byte[] cString(String path) {
        return (path + "\0").getBytes();
    }

    /** The load flags {@code FTFontFile.createGlyphOutline} uses. */
    private static int outlineFlags() {
        return OSFreetypeShim.constant("FT_LOAD_NO_HINTING") | OSFreetypeShim.constant("FT_LOAD_NO_BITMAP")
                | OSFreetypeShim.constant("FT_LOAD_IGNORE_TRANSFORM");
    }

    /**
     * {@code windingRule;numCommands;commands.length;coords.length;pathIteratorSegments|types|raw float bits}:
     * the observables the golden pins, with every coordinate as {@link Float#floatToRawIntBits}.
     */
    static String outline(Path2D path) {
        if (path == null) {
            return NULL;
        }
        byte[] types = Arrays.copyOf(path.getCommandsNoClone(), path.getNumCommands());
        float[] coords = path.getFloatCoordsNoClone();
        StringBuilder typeText = new StringBuilder();
        int coordCount = 0;
        for (byte type : types) {
            typeText.append(typeText.length() == 0 ? "" : ";").append(type);
            coordCount += switch (type) {
                case 0, 1 -> 2;
                case 2 -> 4;
                case 3 -> 6;
                default -> 0;
            };
        }
        StringBuilder coordText = new StringBuilder();
        for (float coord : coords) {
            coordText.append(coordText.length() == 0 ? "" : ";")
                     .append(Integer.toHexString(Float.floatToRawIntBits(coord)));
        }
        return path.getWindingRule() + ";" + path.getNumCommands() + ";" + path.getCommandsNoClone().length + ";"
                + coords.length + ";" + coordCount + "|" + typeText + "|" + coordText;
    }
}
