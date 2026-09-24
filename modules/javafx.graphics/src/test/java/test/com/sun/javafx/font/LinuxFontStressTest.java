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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.font.LinuxFontGoldens.ChildRun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Lifetime and concurrency baselines of the Linux font layer, each in a child JVM ({@link LinuxFontProcessChild})
 * so that a crash is reported as a failure with its {@code hs_err} file instead of ending the test fork. They
 * passed against the JNI build of commit {@code 7b43255b30} and must pass unchanged against any replacement:
 * no fatal error, and every count identical to its single-threaded or expected value.
 * <ul>
 * <li>{@code FTDisposer}: dropped unregistered fonts release their face and library once each, on the
 * {@code Prism Font Disposer} thread, and the font file's mappings, which FreeType holds per open face, are
 * gone afterwards;</li>
 * <li>{@code PangoGlyphLayout.dispose}: layouts handed between threads, each freeing its UTF-8 buffers
 * ({@code g_free}) as it is disposed, still shape identically;</li>
 * <li>outline decomposition on several threads, one {@code FT_Library} per font file, plus threads sharing one
 * file;</li>
 * <li>a raw {@code pango_shape} loop with the call sequence of {@code PangoGlyphLayout.layout};</li>
 * <li>{@code getFontConfig} against {@code populateMapsNative} on concurrent threads.</li>
 * </ul>
 * The layout, outline and shape children read the C heap in use ({@code mallinfo2}) and the resident set
 * ({@code /proc/self/statm}) after a warm-up and at the end. The growth of the C heap must stay under a bound
 * measured on the JNI build, which fails on gross retention of native memory per call, not on one small object
 * per call ({@link #MALLOC_GROWTH_BOUND} gives the figures): one small object retained per call on the release
 * paths of the bindings is pinned by the two-sided loops with an injected-leak phase of
 * {@code LinuxFreetypeNativeTest} and {@code LinuxPangoNativeTest}, each in a settled child JVM. The growth of
 * the resident set is recorded to the test output, not bounded ({@link #MALLOC_GROWTH_BOUND} says why). Those
 * children run with a fixed, pre-touched Java heap, so that the Java heap does not grow the resident set.
 * {@link #recordsOutlineAndLayoutTimings()} records wall-clock timings to the test output and asserts nothing
 * about them: they are the numbers a later build is compared with by a reader, not by the test.
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFontStressTest {

    static final String DISPOSER = "stress.disposer";
    static final String LAYOUTS = "stress.layouts";
    static final String OUTLINES = "stress.outlines";
    static final String SHAPE = "stress.shape";
    static final String FONTCONFIG = "stress.fontconfig";
    static final String TIMING = "timing";

    /** Rounds of the timing child, default 5. */
    static final String TIMING_ROUNDS_PROPERTY = "jfx.font.timing.rounds";

    static final int DISPOSER_FONTS = 200;
    static final int THREADS = 4;
    static final int LAYOUT_ROUNDS = 20;

    /**
     * Warm-up rounds of the layouts child, ten times its measured rounds, and the pause before each of its two
     * memory snapshots. Both were added when the Pango, FreeType and fontconfig calls of this module moved from
     * JNI to {@code java.lang.foreign} bindings, whose method-handle chains give the JIT far more to compile.
     * With one warm-up round (76 layouts) the measured rounds fell inside that compilation: the compiler arenas
     * that HotSpot returns to its chunk pool, which a periodic task empties only every 5 seconds, counted as C
     * heap in use (growth of 1.1 to 11.5 MB over 1,520 layouts against the 2 MiB bound in 10 of 14 runs of
     * identical code; +69 KB with {@code -Xint}), and the pages they had touched stayed resident after the pool
     * was emptied (up to 23 MB against the 8 MiB bound the resident set then had). Nothing was retained per
     * call: the outline and shape children, with 50 and 8 times the calls, stayed near their JNI figures. The
     * warm-up now passes the tier-4 compile thresholds of the per-layout methods (5,000 invocations, 15,000 with
     * loops) before the first snapshot, and the pause lets the pool be emptied and the freed memory be returned to
     * the OS (see {@link #MEMORY_JVM_OPTIONS}) before each snapshot. Measured on the bindings, 8 child runs each:
     * with 20 warm-up rounds and a 6 s pause the C heap figure was stable (-0.96 to -0.80 MB) but the resident
     * figure was 4.8 to 23.4 MB; with 200 rounds, 8 s and trimming, -1.22 to -0.72 MB and -1.55 to -1.38 MB.
     */
    static final int LAYOUT_WARMUP_ROUNDS = 200;
    static final long LAYOUT_SETTLE_MILLIS = 8_000;
    static final int OUTLINE_ROUNDS = 10;
    static final int OUTLINE_GLYPHS = 1000;
    static final int SHAPE_WARMUP_ROUNDS = 20;
    static final int SHAPE_ROUNDS = 400;
    static final int FONTCONFIG_ROUNDS = 10;

    /**
     * Upper bound, in bytes, on the growth of the C heap in use between the warm-up and the end of the layout,
     * outline and shape children. Measured on the JNI build of commit {@code 7b43255b30} (glibc 2.43, FreeType
     * 2.14.2, Pango 1.57.0, x86_64) with the present recipe, the growth of the C heap and of the resident set
     * was: layouts -1,722,240 and -1,433,600; outlines -212,272 and -4,063,232; shape 39,760 and 106,496 (before
     * the layouts warm-up, the pauses and the trimming of {@link #MEMORY_JVM_OPTIONS} were added: 243,728 and
     * 724,992; -370,400 and 335,872; 41,248 and 122,880). The bound leaves room for JIT compilation and allocator
     * slack, so it catches gross retention only. Spread over the calls it is about 164 bytes per
     * {@code pango_shape} call in the shape child (2,097,152 / 12,800) and about 26 bytes per decomposition in the
     * outline child (2,097,152 / 80,000; counts where the corpus shapes in 32 calls per pass and all six outline
     * fonts are installed), the latter only when that interval does not fall. It can fall: the outline child takes
     * its first reading without a settle pause, and its C heap fell by 2.2 to 7.1 MB over the interval in several
     * local runs of that child at commit {@code 5a2886d8f7} (JDK 25 and 26, glibc 2.43, x86_64); its first reading
     * was then higher, by about the size of the fall, than in the runs that did not fall, and what held that
     * memory was not measured. One small object retained per call on the release paths of the bindings is pinned
     * by the two-sided loops with an injected-leak phase of {@code LinuxFreetypeNativeTest} and
     * {@code LinuxPangoNativeTest}, which read the C heap the same way in a settled child JVM.
     * <p>
     * The growth of the resident set is recorded to the test output and to the C-heap failure message, and not
     * bounded. Besides native memory still in use, the resident set counts the code cache and metaspace the JIT
     * and method-handle spinning fill, thread stacks, the pages of mapped font files and allocator pages the JVM
     * has not returned to the OS, all of which move with the JDK build, the number of cores and the library
     * versions. At commit {@code 5a2886d8f7} the layouts child grew it by 9,924,608 bytes over 1,520 layouts on a
     * GitHub ubuntu-24.04 runner with JDK 26.0.2 while its C heap grew by less than this bound, and the 8 MiB
     * bound the resident set had then failed.
     */
    static final long MALLOC_GROWTH_BOUND = 2L << 20;

    /**
     * A fixed, pre-touched Java heap, and the native heap trimmed every second: the recorded resident set then
     * grows neither for the Java heap nor for the pages that freed C allocations (the JIT compiler's arenas above
     * all) leave in glibc's heap.
     */
    static final List<String> MEMORY_JVM_OPTIONS =
            List.of("-Xms192m", "-Xmx192m", "-XX:+AlwaysPreTouch", "-XX:TrimNativeHeapInterval=1000");

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void disposerReleasesEveryFaceAndLibraryOnce() throws Exception {
        assumeFonts("dejavuserif");
        Map<String, String> counts = run(DISPOSER, List.of("-Dprism.debugfonts=true"));
        assertEquals(Integer.toString(DISPOSER_FONTS), counts.get("created"), "fonts created");
        assertEquals(counts.get("created"), counts.get("rendered"), "fonts rendered");
        assertEquals(counts.get("created"), counts.get("doneFace"), "FT_Done_Face calls by the disposer");
        assertEquals(counts.get("created"), counts.get("doneLibrary"), "FT_Done_FreeType calls by the disposer");
        assertEquals("0", counts.get("maps.font.before"), "mappings of the font file before the loads");
        assertEquals(counts.get("created"), counts.get("maps.font.held"),
                     "mappings of the font file while every font is held (one per open face)");
        assertEquals("0", counts.get("maps.font.afterDispose"),
                     "mappings of the font file after the disposer ran (FT_Done_Face unmaps it)");
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void layoutsHandedBetweenThreadsShapeIdentically() throws Exception {
        assumeFonts("dejavusans");
        Map<String, String> counts = run(LAYOUTS, MEMORY_JVM_OPTIONS);
        int layouts = THREADS * LAYOUT_ROUNDS * LinuxFontGoldens.TEXTS.size();
        assertEquals(Integer.toString(layouts), counts.get("layouts"), "layouts");
        assertEquals("0", counts.get("mismatches"), "layouts that shaped differently from the first one");
        assertTrue(Integer.parseInt(counts.get("referenceGlyphs")) > 0, "glyphs in the reference layouts");
        assertNativeMemoryBounded(counts, layouts + " layouts");
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void concurrentOutlinesMatchSingleThreadedOutlines() throws Exception {
        assumeFonts("dejavusans");
        Map<String, String> counts = run(OUTLINES, MEMORY_JVM_OPTIONS);
        int threads = Integer.parseInt(counts.get("threads"));
        int outlines = threads * OUTLINE_ROUNDS * OUTLINE_GLYPHS;
        assertEquals(Integer.toString(outlines), counts.get("outlines"), "outlines");
        assertEquals("0", counts.get("mismatches"), "outline sets that differ from the single-threaded ones");
        assertNativeMemoryBounded(counts, outlines + " outlines");
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void rawShapeLoopRetainsNoNativeMemory() throws Exception {
        assumeFonts("dejavusans");
        Map<String, String> counts = run(SHAPE, MEMORY_JVM_OPTIONS);
        long perRound = Long.parseLong(counts.get("shapes.perRound"));
        assertTrue(perRound > 0, "pango_shape calls per pass over the corpus");
        assertEquals(Long.toString(perRound * SHAPE_ROUNDS), counts.get("shapes"), "pango_shape calls");
        assertEquals("0", counts.get("mismatches"), "passes whose glyph count differs from the first one");
        assertTrue(Long.parseLong(counts.get("referenceGlyphs")) > 0, "glyphs in the reference pass");
        assertNativeMemoryBounded(counts, counts.get("shapes") + " pango_shape calls");
    }

    /**
     * The {@code mem.*} keys of a child: the growth of the resident set recorded to the test output and to the
     * failure message, that of the C heap in use against {@link #MALLOC_GROWTH_BOUND}. The malloc figure is
     * {@code -1} where libc exports no {@code mallinfo2}; the C heap is then not checked, and the test output
     * says so.
     */
    private static void assertNativeMemoryBounded(Map<String, String> counts, String work) {
        long malloc = Long.parseLong(counts.get("mem.malloc.growth"));
        long resident = Long.parseLong(counts.get("mem.resident.growth"));
        String recorded = "the resident set changed by " + resident + " bytes over " + work
                + " (recorded, not bounded)";
        System.out.println("[LinuxFontStressTest] " + recorded);
        if (malloc >= 0) {
            assertTrue(malloc < MALLOC_GROWTH_BOUND, "the C heap in use grew by " + malloc + " bytes over " + work
                    + " (bound " + MALLOC_GROWTH_BOUND + "): native memory is retained per call; " + recorded);
        } else if (malloc == -1) {
            System.out.println("[LinuxFontStressTest] the C heap in use was not checked over " + work
                    + ": libc exports no mallinfo2");
        }
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void concurrentFontconfigCallsMatchSequentialCalls() throws Exception {
        assumeFonts("dejavusans");
        Map<String, String> counts = run(FONTCONFIG, List.of());
        assertEquals(Integer.toString(THREADS * FONTCONFIG_ROUNDS), counts.get("calls"), "fontconfig calls");
        assertEquals("0", counts.get("mismatches"), "results that differ from the sequential ones");
    }

    /** Records only. The child's numbers go to the test output, one line per measurement. */
    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    public void recordsOutlineAndLayoutTimings() throws Exception {
        assumeFonts("dejavusans");
        String rounds = System.getProperty(TIMING_ROUNDS_PROPERTY);
        List<String> options = rounds == null ? List.of() : List.of("-D" + TIMING_ROUNDS_PROPERTY + "=" + rounds);
        Map<String, String> timings = run(TIMING, options);
        for (Map.Entry<String, String> entry : new TreeMap<>(timings).entrySet()) {
            System.out.println("[LinuxFontStressTest] timing " + entry.getKey() + "=" + entry.getValue());
        }
    }

    private static void assumeFonts(String... keys) {
        for (String key : keys) {
            assumeTrue(Files.isRegularFile(LinuxFontGoldens.pinned(key)),
                       "the font " + LinuxFontGoldens.pinned(key) + " is not installed");
        }
    }

    private static Map<String, String> run(String scenario, List<String> jvmOptions) throws Exception {
        ChildRun run = LinuxFontGoldens.launchChild(scenario, Map.of(), Set.of(), jvmOptions, 150);
        if (run.exitCode() != 0 || !run.errorFiles().isEmpty() || !Files.isRegularFile(run.output())) {
            fail(run.describe());
        }
        Map<String, String> values = FontGoldens.parse(Files.readString(run.output(), StandardCharsets.UTF_8));
        if (!scenario.equals(TIMING)) {
            System.out.println("[LinuxFontStressTest] " + scenario + " " + new TreeMap<>(values));
        }
        return values;
    }
}
