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

package test.com.sun.scenario.effect;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.HeapImage;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.BoxRenderState;
import com.sun.scenario.effect.impl.state.LinearConvolveRenderState;
import com.sun.scenario.effect.impl.sw.RendererDelegate;
import com.sun.scenario.effect.impl.sw.java.JSWBoxBlurPeer;
import com.sun.scenario.effect.impl.sw.java.JSWLinearConvolveShadowPeer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import test.com.sun.javafx.test.ParityGate;
import test.com.sun.scenario.effect.DecoraBackend.Result;
import test.com.sun.scenario.effect.DecoraCorpus.Cause;
import test.com.sun.scenario.effect.DecoraCorpus.GoldenRow;
import test.com.sun.scenario.effect.DecoraCorpus.NativePlatform;
import test.com.sun.scenario.effect.DecoraGoldens.Entry;
import test.com.sun.scenario.effect.DecoraGoldens.Index;
import test.com.sun.scenario.effect.DecoraGoldens.Tier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static test.com.sun.scenario.effect.DecoraCorpus.ONE_STEP;
import static test.com.sun.scenario.effect.DecoraCorpus.PRIMARY_SEED;
import static test.com.sun.scenario.effect.DecoraCorpus.channelDelta;
import static test.com.sun.scenario.effect.DecoraCorpus.pattern;

/**
 * The Java software peers of Decora against the native {@code decora_sse} output recorded in the golden
 * ({@link DecoraGoldens}), on every OS and without any native library.
 * <p>
 * Every {@link DecoraCorpus#rows() golden row} is rendered on a fresh Java backend and judged over its full frame:
 * the result bounds and transform equal the golden's; the native frame is decoded (P), reconstructed from the
 * capture's Java frame (S, only while the Java frame still hashes to the capture's) or taken to be the Java frame
 * (H, only while it hashes to the native one) and has to hash to the recorded native SHA-256; every pixel further
 * from native than the row's Windows bound has to be explained by the row's cause, unless that cause is fixed, and
 * no more pixels than at the capture; the Java render may not drift further from native than at the capture; and a
 * clipped render has to reproduce its own unclipped render as well as the native peer did. An S or H row whose Java
 * frame moved cannot be judged against native any more: it is unjudgeable, classified as "JDK math changed" when the
 * {@code Math}-derived kernel inputs (the Gaussian weights or the displacement float map) hash differently from the
 * capture, else as "Java peer output moved".
 * <p>
 * How hard a moved Java frame fails depends on {@code os.arch}. On {@code amd64} and {@code x86_64} the Java output
 * was proven identical on Windows with JDK 26 and on Linux with JDK 25, so there drift and an unjudgeable row are
 * failures. On any other architecture {@code Math.exp} (the Gaussian weights) and {@code Math.pow} (the lighting and
 * soft-light peers) may differ from x64 by one ulp, and CI also runs these tests on aarch64 Linux and macOS, so there
 * drift is a warning, and an unjudgeable row prints a {@code WARNING unjudgeable} line with its classification and
 * passes without being counted as compared. Every other check fails on every architecture, so a P row, whose native
 * frame is stored in full, is held to its bound and cause everywhere.
 * <p>
 * The golden never moves: {@link #INDEX_MD5} pins the index, the index pins the frames file and its own data
 * lines, and the bound, cause and explained columns pin what each row may tolerate. The negative controls prove that
 * every branch of the comparison can fail.
 */
public class DecoraJavaGoldenTest {

    /** The md5 of {@value DecoraGoldens#INDEX_RESOURCE}; moving the golden means editing this constant. */
    static final String INDEX_MD5 = "495dc54f7e43be95ee8134389222dd88";

    /** The golden was captured from the Windows library, so rows are judged by their Windows bounds on every OS. */
    private static final NativePlatform GOLDEN_PLATFORM = NativePlatform.WINDOWS;

    /**
     * Whether a moved Java frame (drift, or an S or H row that became unjudgeable) fails its row: only on x64, where
     * the Java output was proven platform independent; elsewhere it is a warning (see the class description).
     */
    private static final boolean MOVED_JAVA_IS_FAILURE = isX64();

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(DecoraJavaGoldenTest.class);
    private static final List<Judgement> JUDGEMENTS = new ArrayList<>();

    private static byte[] indexBytes;
    private static byte[] frames;
    private static Index index;
    private static Map<String, Entry> entries;
    private static Map<String, GoldenRow> corpus;
    private static String floatMapGolden;
    private static String floatMapNow;

    /** What a comparison found wrong. */
    enum Kind {
        BOUNDS, TRANSFORM, PIN_BOUND, PIN_CAUSE, RECORD, NATIVE_SHA, UNJUDGEABLE, CAUSE_SHAPE, BOUND_EXCEEDED,
        EXPLAINED_EXCEEDS_CAPTURE, DRIFT, SELF_CONSISTENCY, EDGE_ROWS
    }

    record Finding(Kind kind, String message) {
    }

    /** The verdict on one row, built by the comparison stages; the negative controls assert on it. */
    static final class Judgement {

        final String key;
        final Tier tier;
        int maxDelta = -1;
        long differing = -1;
        long pixels = -1;
        long explained = -1;
        int javaSelf = -1;
        String tx = "?";
        final List<Finding> findings = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        /** Why the row was unjudgeable where that is only a warning; null for a row that was compared. */
        String notCompared;

        Judgement(String key, Tier tier) {
            this.key = key;
            this.tier = tier;
        }

        void fail(Kind kind, String message) {
            findings.add(new Finding(kind, message));
        }

        /** An S or H row whose Java frame moved: a failure, or a warning that leaves the row not compared. */
        void unjudgeable(String classification, boolean isFailure) {
            if (isFailure) {
                fail(Kind.UNJUDGEABLE, "unjudgeable: " + classification);
            } else {
                notCompared = classification;
            }
        }

        boolean passed() {
            return findings.isEmpty();
        }

        boolean compared() {
            return notCompared == null;
        }

        boolean has(Kind kind) {
            return findings.stream().anyMatch(f -> f.kind() == kind);
        }

        String message(Kind kind) {
            return findings.stream().filter(f -> f.kind() == kind).map(Finding::message).findFirst().orElse("");
        }

        String describe() {
            StringBuilder text = new StringBuilder(key).append(" [").append(tier).append("]:");
            for (Finding f : findings) {
                text.append("\n  ").append(f.kind()).append(": ").append(f.message());
            }
            if (!compared()) {
                text.append("\n  WARNING unjudgeable: ").append(notCompared);
            }
            for (String w : warnings) {
                text.append("\n  WARNING ").append(w);
            }
            return text.toString();
        }
    }

    @BeforeAll
    static void loadGolden() {
        indexBytes = DecoraGoldens.resource(DecoraGoldens.INDEX_RESOURCE);
        frames = DecoraGoldens.resource(DecoraGoldens.FRAMES_RESOURCE);
        assertEquals(INDEX_MD5, DecoraGoldens.md5(indexBytes), () -> DecoraGoldens.INDEX_RESOURCE + " is not the"
                + " golden this test pins: the golden files are never regenerated");
        index = DecoraGoldens.parse(indexBytes);
        String framesProblem = framesProblem(index, frames);
        assertNull(framesProblem, framesProblem);
        entries = new LinkedHashMap<>();
        int[] tiers = new int[Tier.values().length];
        for (Entry e : index.entries()) {
            entries.put(e.key(), e);
            tiers[e.tier().ordinal()]++;
        }
        assertEquals(String.format(Locale.ROOT, "%d; tier P %d S %d H %d", index.entries().size(), tiers[0],
                tiers[1], tiers[2]), index.header("rows"), "golden rows header disagrees with its data lines");
        corpus = new LinkedHashMap<>();
        for (GoldenRow row : DecoraCorpus.rows()) {
            corpus.put(row.key(), row);
        }
        String rowSetProblem = rowSetProblem(new ArrayList<>(entries.keySet()), new ArrayList<>(corpus.keySet()));
        assertNull(rowSetProblem, rowSetProblem);
        TreeSet<String> sizes = new TreeSet<>();
        for (GoldenRow row : corpus.values()) {
            sizes.add(row.size());
        }
        TreeSet<String> patternSizes = new TreeSet<>();
        for (String line : index.headers("input pattern")) {
            String[] p = line.split(" ", -1);
            assertTrue(p.length == 5 && p[1].equals("seed") && p[3].equals("sha256"), () -> "input pattern: " + line);
            String[] wh = p[0].split("x", -1);
            String actual = DecoraGoldens.patternSha256(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]),
                    Long.parseLong(p[2], 16));
            assertEquals(p[4], actual, () -> "the corpus input pattern changed: " + line);
            patternSizes.add(p[0]);
        }
        assertEquals(sizes, patternSizes, "golden input pattern sizes differ from the corpus row sizes");
        floatMapGolden = index.header("input floatmap");
        floatMapNow = DecoraCorpus.FLOAT_MAP_SIZE + "x" + DecoraCorpus.FLOAT_MAP_SIZE + " sha256 "
                + DecoraGoldens.floatMapSha256();
        LEDGER.oracleAvailable();
    }

    /** Prints the per-row table and the per-group summary, then insists that rows were compared. */
    @AfterAll
    static void report() {
        StringBuilder text = new StringBuilder(32768);
        text.append(String.format(Locale.ROOT, "%nJava peers against the decora_sse Windows golden%n"));
        text.append(String.format(Locale.ROOT, "%-34s %-44s %-8s %-4s %-3s %5s %9s %5s %8s %8s %s%n", "effect",
                "params", "size", "tier", "tx", "maxD", "diff%", "bound", "fullMaxD", "javaSelf", "verdict"));
        Map<String, int[]> groups = new TreeMap<>();
        Map<String, Judgement> judged = new HashMap<>();
        synchronized (JUDGEMENTS) {
            for (Judgement j : JUDGEMENTS) {
                judged.put(j.key, j);
            }
        }
        for (GoldenRow row : corpus == null ? List.<GoldenRow>of() : corpus.values()) {
            Judgement j = judged.get(row.key());
            if (j == null) {
                continue;
            }
            Entry e = entries.get(j.key);
            String verdict = !j.passed() ? "FAILED " + j.findings.stream().map(f -> f.kind().name()).toList()
                    : !j.compared() ? "NOT COMPARED (unjudgeable)"
                    : j.maxDelta == 0 ? "exact" : j.maxDelta <= e.bound() ? "within bound"
                    : "explained by " + e.cause();
            text.append(String.format(Locale.ROOT, "%-34s %-44s %-8s %-4s %-3s %5s %8s%% %5d %8d %8s %s%n",
                    row.effect(), row.params(), row.size(), j.tier, j.tx,
                    j.compared() ? Integer.toString(j.maxDelta) : "-",
                    j.compared() ? DecoraGoldens.percent(j.differing, j.pixels) : "-", e.bound(), e.fullMaxD(),
                    j.javaSelf < 0 ? "-" : Integer.toString(j.javaSelf), verdict));
            int[] g = groups.computeIfAbsent(row.effect(), k -> new int[] {0, 0, 0, 0, 0});
            g[0] = Math.max(g[0], j.maxDelta);
            g[1] = Math.max(g[1], (int) Math.ceil(j.pixels <= 0 ? 0 : 100000.0 * j.differing / j.pixels));
            g[2] = Math.max(g[2], e.bound());
            g[3] += j.passed() ? 0 : 1;
            g[4] += j.compared() ? 0 : 1;
        }
        text.append(String.format(Locale.ROOT, "%n%-34s %5s %10s %5s %s%n", "effect (summary)", "maxD",
                "worst diff%", "bound", "gate"));
        for (Map.Entry<String, int[]> group : groups.entrySet()) {
            int[] g = group.getValue();
            String gate = g[3] > 0 ? "PARITY: EXCEEDED (" + g[3] + " rows failed)" : g[0] == 0 ? "PARITY: exact"
                    : g[0] <= g[2] ? "PARITY: tolerance " + g[2] : "PARITY: tolerance " + g[2] + " + explained cause";
            if (g[4] > 0) {
                gate += " (" + g[4] + " rows not compared)";
            }
            text.append(String.format(Locale.ROOT, "%-34s %5d %9.3f%% %5d %s%n", group.getKey(), g[0],
                    g[1] / 1000.0, g[2], gate));
        }
        System.out.println(text);
        LEDGER.assertOracleRan();
    }

    static Stream<Arguments> goldenRows() {
        return DecoraCorpus.rows().stream().map(row -> Arguments.of(row.key(), row));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenRows")
    void goldenRow(String key, GoldenRow row) {
        Entry entry = entries.get(key);
        Judgement j = judgeRow(row, entry, frames, DecoraBackend::java, MOVED_JAVA_IS_FAILURE);
        synchronized (JUDGEMENTS) {
            JUDGEMENTS.add(j);
        }
        if (j.compared()) {
            LEDGER.compared();
        } else {
            System.out.println("WARNING unjudgeable " + key + ": " + j.notCompared + " Not compared: os.arch "
                    + System.getProperty("os.arch") + " is not x64, and Math.exp and Math.pow may differ from x64 by"
                    + " one ulp.");
        }
        for (String warning : j.warnings) {
            System.out.println("WARNING " + key + ": " + warning);
        }
        if (!j.passed()) {
            fail(j.describe());
        }
    }

    /**
     * A golden frame one step off where the bound is exact, two steps off where it is one step, and one step off
     * on a pixel where Java matched it (within the bound, but more differing pixels than at the capture).
     */
    @Test
    void corruptedGoldenFrameIsReported() {
        GoldenRow exact = row("LinearConvolve/gaussian | radius=5.0 centered | 64x48");
        Entry e = entry(exact, Tier.P, 0);
        int[] javaFrame = DecoraGoldens.frame(exact.render(DecoraBackend.java()));
        int[] nativeFrame = DecoraGoldens.decodeFull(DecoraGoldens.record(e, frames), e.width(), e.height());
        assertTrue(comparePixels(exact, e, nativeFrame, javaFrame, e.explained()).passed());
        int i = firstEqualPixel(nativeFrame, javaFrame, exact, false);
        int[] corrupted = nativeFrame.clone();
        corrupted[i] = changeBlue(corrupted[i], 1);
        Judgement oneStep = comparePixels(exact, e, corrupted, javaFrame, e.explained());
        assertTrue(oneStep.has(Kind.BOUND_EXCEEDED), oneStep::describe);
        assertEquals(1, oneStep.maxDelta);

        GoldenRow sepia = row("SepiaTone | level=0.5 | 64x48");
        Entry s = entry(sepia, Tier.P, 1);
        javaFrame = DecoraGoldens.frame(sepia.render(DecoraBackend.java()));
        nativeFrame = DecoraGoldens.decodeFull(DecoraGoldens.record(s, frames), s.width(), s.height());
        assertTrue(comparePixels(sepia, s, nativeFrame, javaFrame, s.explained()).passed());
        i = firstEqualPixel(nativeFrame, javaFrame, sepia, false);
        corrupted = nativeFrame.clone();
        corrupted[i] = changeBlue(corrupted[i], 2);
        Judgement twoSteps = comparePixels(sepia, s, corrupted, javaFrame, s.explained());
        assertTrue(twoSteps.has(Kind.BOUND_EXCEEDED), twoSteps::describe);
        corrupted = nativeFrame.clone();
        corrupted[i] = changeBlue(corrupted[i], 1);
        Judgement drift = comparePixels(sepia, s, corrupted, javaFrame, s.explained());
        assertFalse(drift.has(Kind.BOUND_EXCEEDED), drift::describe);
        assertTrue(drift.has(Kind.DRIFT), drift::describe);
        assertEquals(s.fullDiff() + 1, drift.differing);
    }

    /** A Java BoxBlur peer that flips a blue bit of pixel (0,0) on pass 1 is reported as exceeding the bound. */
    @Test
    void mutatedJavaPeerIsReported() {
        GoldenRow box = row("BoxBlur | h=3 v=3 passes=1 | 64x48");
        Entry e = entry(box, Tier.P, 0);
        AtomicReference<DecoraBackend> used = new AtomicReference<>();
        Judgement mutated = judgeRow(box, e, frames, mutating("BoxBlur", OffByTwoBoxBlurPeer.class, used), true);
        assertTrue(used.get().ranPeers().contains(OffByTwoBoxBlurPeer.class.getName()), () -> "ran "
                + used.get().ranPeers());
        assertTrue(mutated.has(Kind.BOUND_EXCEEDED), mutated::describe);
        assertTrue(mutated.message(Kind.BOUND_EXCEEDED).contains("bound exceeded at (0,0)"), mutated::describe);
        assertTrue(mutated.message(Kind.BOUND_EXCEEDED).contains("delta 2"), mutated::describe);
        assertEquals(2, mutated.maxDelta);
        Judgement clean = judgeRow(box, e, frames, DecoraBackend::java, true);
        assertTrue(clean.passed(), clean::describe);
    }

    /** An index with one data character changed is rejected by its data md5 and no longer matches the pin. */
    @Test
    void tamperedIndexIsRejected() {
        byte[] tampered = indexBytes.clone();
        int position = firstDataLineOffset(tampered);
        int separators = 0;
        while (separators < 6) {
            if (tampered[position] == '|') {
                separators++;
            }
            position++;
        }
        position++;
        tampered[position] = (byte) (tampered[position] == '0' ? '1' : '0');
        AssertionError error = assertThrows(AssertionError.class, () -> DecoraGoldens.parse(tampered));
        assertTrue(error.getMessage().contains("data-md5"), error::getMessage);
        assertNotEquals(INDEX_MD5, DecoraGoldens.md5(tampered));
    }

    /** A frames file with one byte changed fails its md5; a decoded P record with one byte changed its SHA-256. */
    @Test
    void tamperedFramesAreRejected() {
        assertNull(framesProblem(index, frames));
        byte[] tampered = frames.clone();
        tampered[tampered.length / 2] ^= 0x10;
        String problem = framesProblem(index, tampered);
        assertTrue(problem != null && problem.contains("frames md5"), () -> "frames problem: " + problem);

        GoldenRow exact = row("LinearConvolve/gaussian | radius=5.0 centered | 64x48");
        Entry e = entry(exact, Tier.P, 0);
        byte[] filtered = DecoraGoldens.inflateFull(DecoraGoldens.record(e, frames), e.width(), e.height());
        Judgement intact = new Judgement(e.key(), e.tier());
        checkNativeSha(e, DecoraGoldens.unfilter(filtered, e.width(), e.height()), intact);
        assertTrue(intact.passed(), intact::describe);
        filtered[filtered.length / 2 + 1] ^= 0x01;
        Judgement changed = new Judgement(e.key(), e.tier());
        checkNativeSha(e, DecoraGoldens.unfilter(filtered, e.width(), e.height()), changed);
        assertTrue(changed.has(Kind.NATIVE_SHA), changed::describe);
    }

    /**
     * An H row whose Java frame moved cannot be judged and says why; where a moved Java frame only warns, the same
     * row passes with that classification and is not compared.
     */
    @Test
    void hashRowDetectsMovedJava() {
        GoldenRow box = row("BoxBlur | h=3 v=3 passes=1 | 257x129");
        Entry e = entry(box, Tier.H, 0);
        AtomicReference<DecoraBackend> used = new AtomicReference<>();
        Judgement mutated = judgeRow(box, e, frames, mutating("BoxBlur", OffByTwoBoxBlurPeer.class, used), true);
        assertTrue(mutated.has(Kind.UNJUDGEABLE), mutated::describe);
        assertTrue(mutated.message(Kind.UNJUDGEABLE).contains("unjudgeable: Java peer output moved"),
                mutated::describe);
        Judgement warned = judgeRow(box, e, frames, mutating("BoxBlur", OffByTwoBoxBlurPeer.class, used), false);
        assertTrue(warned.passed(), warned::describe);
        assertFalse(warned.compared(), warned::describe);
        assertTrue(warned.notCompared.startsWith("Java peer output moved"), warned::describe);
        Judgement clean = judgeRow(box, e, frames, DecoraBackend::java, true);
        assertTrue(clean.passed(), clean::describe);
    }

    /** An S row whose Java frame moved cannot be judged; an S record with a changed pixel fails its SHA-256. */
    @Test
    void sparseRowDetectsMovedJava() {
        GoldenRow shadow = row("LinearConvolveShadow/gaussian | radius=25.0 spread=0.0 black | 257x129");
        Entry e = entry(shadow, Tier.S, 1);
        AtomicReference<DecoraBackend> used = new AtomicReference<>();
        Judgement mutated = judgeRow(shadow, e, frames, mutating("LinearConvolveShadow",
                OffByTwoLinearConvolveShadowPeer.class, used), true);
        assertTrue(used.get().ranPeers().contains(OffByTwoLinearConvolveShadowPeer.class.getName()), () -> "ran "
                + used.get().ranPeers());
        assertTrue(mutated.has(Kind.UNJUDGEABLE), mutated::describe);
        assertTrue(mutated.message(Kind.UNJUDGEABLE).contains("unjudgeable"), mutated::describe);

        int[] javaFrame = DecoraGoldens.frame(shadow.render(DecoraBackend.java()));
        byte[] record = DecoraGoldens.record(e, frames);
        Judgement intact = new Judgement(e.key(), e.tier());
        assertTrue(nativeFrame(e, record, javaFrame, e.kernelSha256(), floatMapGolden, floatMapGolden, true,
                intact) != null, intact::describe);
        int[][] sparse = DecoraGoldens.sparseEntries(record, javaFrame.length);
        sparse[0][1] = changeBlue(sparse[0][1], 1);
        Judgement changed = new Judgement(e.key(), e.tier());
        assertNull(nativeFrame(e, DecoraGoldens.encodeSparseEntries(sparse), javaFrame, e.kernelSha256(),
                floatMapGolden, floatMapGolden, true, changed));
        assertTrue(changed.has(Kind.NATIVE_SHA), changed::describe);
    }

    /**
     * The full-contrast cause is fixed. At both sizes its predicate names exactly the pixels the capture explained,
     * the Java peer now equals the native golden on every one of them, and the rows compare without explaining a
     * pixel.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fullContrastSizes")
    void fixedCauseMatchesNativeOnItsPixels(String size) {
        GoldenRow contrast = row("ColorAdjust/full-contrast (JSL 0/0) | hue=1 sat=1 bri=1 con=1 | " + size);
        Entry e = entry(contrast, Tier.P, 1);
        assertEquals("F1", e.cause());
        assertTrue(contrast.cause().fixed(), "cause " + contrast.causeId() + " is not marked fixed");
        int[] javaFrame = DecoraGoldens.frame(contrast.render(DecoraBackend.java()));
        int[] nativeFrame = DecoraGoldens.decodeFull(DecoraGoldens.record(e, frames), e.width(), e.height());
        List<Integer> named = causePixels(contrast);
        assertEquals(e.explained(), named.size(), "pixels the cause names against pixels it explained at capture");
        List<String> differing = new ArrayList<>();
        for (int i : named) {
            if (nativeFrame[i] != javaFrame[i]) {
                differing.add(String.format(Locale.ROOT, "(%d,%d) golden=%08x java=%08x", i % e.width(),
                        i / e.width(), nativeFrame[i], javaFrame[i]));
            }
        }
        assertTrue(differing.isEmpty(), () -> "Java differs from the golden on the pixels of fixed cause F1: "
                + differing);
        Judgement intact = comparePixels(contrast, e, nativeFrame, javaFrame, e.explained());
        assertTrue(intact.passed(), intact::describe);
        assertEquals(0, intact.explained);
    }

    static Stream<String> fullContrastSizes() {
        return Stream.of("64x48", "257x129");
    }

    /**
     * A golden pixel moved on a pixel the fixed full-contrast cause names is reported against the bound, as is one
     * moved outside it. The same cause unfixed would have explained that pixel, and never more of them than at the
     * capture.
     */
    @Test
    void fixedCauseExplainsNoPixel() {
        GoldenRow contrast = row("ColorAdjust/full-contrast (JSL 0/0) | hue=1 sat=1 bri=1 con=1 | 64x48");
        Entry e = entry(contrast, Tier.P, 1);
        int[] javaFrame = DecoraGoldens.frame(contrast.render(DecoraBackend.java()));
        int[] nativeFrame = DecoraGoldens.decodeFull(DecoraGoldens.record(e, frames), e.width(), e.height());
        int named = causePixels(contrast).get(0);
        int[] corrupted = nativeFrame.clone();
        corrupted[named] = changeBlue(corrupted[named], 2);
        Judgement fixed = comparePixels(contrast, e, corrupted, javaFrame, e.explained());
        assertTrue(fixed.has(Kind.BOUND_EXCEEDED), fixed::describe);
        assertTrue(fixed.message(Kind.BOUND_EXCEEDED).contains(at(named, e)), fixed::describe);
        assertEquals(0, fixed.explained);

        int outside = firstEqualPixel(nativeFrame, javaFrame, contrast, true);
        corrupted = nativeFrame.clone();
        corrupted[outside] = changeBlue(corrupted[outside], 2);
        Judgement unexplained = comparePixels(contrast, e, corrupted, javaFrame, e.explained());
        assertTrue(unexplained.has(Kind.BOUND_EXCEEDED), unexplained::describe);
        assertTrue(unexplained.message(Kind.BOUND_EXCEEDED).contains(at(outside, e)), unexplained::describe);

        Cause cause = contrast.cause();
        GoldenRow unfixed = new GoldenRow(contrast.effect(), contrast.params(), contrast.width(), contrast.height(),
                contrast.bound(), new Cause(cause.id(), cause.appliesToSourcePixel(), false), contrast.run(),
                contrast.unclipped(), contrast.edgeRows());
        corrupted = nativeFrame.clone();
        corrupted[named] = changeBlue(corrupted[named], 2);
        Judgement explained = comparePixels(unfixed, e, corrupted, javaFrame, e.explained());
        assertTrue(explained.passed(), explained::describe);
        assertEquals(1, explained.explained);
        Judgement tooMany = comparePixels(unfixed, e, corrupted, javaFrame, e.explained() - 1);
        assertTrue(tooMany.has(Kind.EXPLAINED_EXCEEDS_CAPTURE), tooMany::describe);
        assertTrue(tooMany.message(Kind.EXPLAINED_EXCEEDS_CAPTURE).contains("explained count exceeds capture"),
                tooMany::describe);
    }

    /** A corpus with a row removed and a row invented does not match the golden, and both are named. */
    @Test
    void rowSetMismatchIsReported() {
        List<String> golden = new ArrayList<>(entries.keySet());
        List<String> changed = new ArrayList<>(corpus.keySet());
        assertNull(rowSetProblem(golden, changed));
        String removed = changed.remove(changed.size() / 2);
        String invented = "InventedEffect | nothing | 1x1";
        changed.add(invented);
        String problem = rowSetProblem(golden, changed);
        assertTrue(problem != null && problem.contains(removed) && problem.contains(invented), () -> "problem: "
                + problem);
    }

    /**
     * Renders a row on a fresh backend from {@code backends} and judges it against its golden entry: shape and pins,
     * the native frame, the pixels, and for a clipped row the consistency with its unclipped render (on another
     * fresh backend). {@code movedJavaIsFailure} decides whether drift and an unjudgeable S or H row fail the row or
     * only warn.
     */
    static Judgement judgeRow(GoldenRow row, Entry e, byte[] frames, Supplier<DecoraBackend> backends,
                              boolean movedJavaIsFailure) {
        Judgement j = new Judgement(row.key(), e.tier());
        DecoraBackend backend = backends.get();
        Result java = row.render(backend);
        if (!checkShape(row, e, java, j)) {
            return j;
        }
        int[] javaFrame = DecoraGoldens.frame(java);
        byte[] record = e.tier() == Tier.H ? null : DecoraGoldens.record(e, frames);
        String kernelNow = DecoraGoldens.kernelSha256(row, backend.gaussianPassWeights());
        int[] nativeFrame = nativeFrame(e, record, javaFrame, kernelNow, floatMapNow, floatMapGolden,
                movedJavaIsFailure, j);
        if (nativeFrame == null) {
            return j;
        }
        comparePixels(row, e, nativeFrame, javaFrame, e.explained(), movedJavaIsFailure, j);
        if (row.clipped()) {
            checkSelfConsistency(row, e, java, row.renderUnclipped(backends.get()), j);
        }
        return j;
    }

    /** Pins, bounds and transform. Returns false when the bounds differ, which leaves no pixels to compare. */
    static boolean checkShape(GoldenRow row, Entry e, Result java, Judgement j) {
        int bound = DecoraCorpus.bound(row, GOLDEN_PLATFORM);
        if (bound > e.bound()) {
            j.fail(Kind.PIN_BOUND, "corpus bound " + bound + " is looser than the golden's " + e.bound());
        }
        if (!row.causeId().equals(e.cause())) {
            j.fail(Kind.PIN_CAUSE, "corpus cause " + row.causeId() + " differs from the golden's " + e.cause());
        }
        j.tx = DecoraGoldens.tx(java.transform());
        String bounds = java.x() + "," + java.y() + "," + java.width() + "," + java.height();
        if (!bounds.equals(e.bounds())) {
            j.fail(Kind.BOUNDS, "result bounds " + bounds + " differ from the golden's " + e.bounds());
            return false;
        }
        if (!j.tx.equals(e.tx())) {
            j.fail(Kind.TRANSFORM, "result transform " + j.tx + " differs from the golden's " + e.tx());
        }
        return true;
    }

    /**
     * The native frame of a row by its tier, checked against the recorded native SHA-256; null, with a finding, when
     * the record is unreadable or the frame does not hash to the native SHA-256; null when an S or H row's Java frame
     * moved, with a finding if {@code unjudgeableIsFailure}, else with the row marked not compared.
     */
    static int[] nativeFrame(Entry e, byte[] record, int[] javaFrame, String kernelNow, String floatMapNow,
                             String floatMapGolden, boolean unjudgeableIsFailure, Judgement j) {
        String javaSha = DecoraGoldens.sha256(javaFrame);
        int[] nativeFrame;
        try {
            switch (e.tier()) {
                case P -> nativeFrame = DecoraGoldens.decodeFull(record, e.width(), e.height());
                case S -> {
                    if (!javaSha.equals(e.javaSha256())) {
                        j.unjudgeable(unjudgeableClassification(e, javaSha, e.javaSha256(), kernelNow, floatMapNow,
                                floatMapGolden), unjudgeableIsFailure);
                        return null;
                    }
                    nativeFrame = DecoraGoldens.applySparse(record, javaFrame);
                }
                default -> {
                    if (!javaSha.equals(e.nativeSha256())) {
                        j.unjudgeable(unjudgeableClassification(e, javaSha, e.nativeSha256(), kernelNow, floatMapNow,
                                floatMapGolden), unjudgeableIsFailure);
                        return null;
                    }
                    nativeFrame = javaFrame;
                }
            }
        } catch (AssertionError error) {
            j.fail(Kind.RECORD, error.getMessage());
            return null;
        }
        return checkNativeSha(e, nativeFrame, j) ? nativeFrame : null;
    }

    static boolean checkNativeSha(Entry e, int[] nativeFrame, Judgement j) {
        String sha = DecoraGoldens.sha256(nativeFrame);
        if (!sha.equals(e.nativeSha256())) {
            j.fail(Kind.NATIVE_SHA, "native frame sha256 " + sha + " is not the golden nativeSha256 "
                    + e.nativeSha256());
            return false;
        }
        return true;
    }

    /** "JDK math changed" or "Java peer output moved", with the hashes that tell them apart and what to do. */
    private static String unjudgeableClassification(Entry e, String javaSha, String expected, String kernelNow,
                                                    String floatMapNow, String floatMapGolden) {
        boolean kernelMoved = !kernelNow.equals(e.kernelSha256());
        boolean floatMapMoved = !floatMapNow.equals(floatMapGolden);
        String why = kernelMoved || floatMapMoved ? "JDK math changed" : "Java peer output moved";
        return why + " (Java frame sha256 " + javaSha + ", the golden needs " + expected
                + "; kernelSha256 now " + kernelNow + ", golden " + e.kernelSha256() + "; float map now "
                + floatMapNow + ", golden " + floatMapGolden + "). A " + e.tier() + " row can only be judged while"
                + " its Java frame is the capture's: revert the change, or mark the row unprovable in this test with"
                + " the reason; never regenerate the golden.";
    }

    /** {@link #comparePixels(GoldenRow, Entry, int[], int[], long, boolean, Judgement)} with drift a failure. */
    static Judgement comparePixels(GoldenRow row, Entry e, int[] nativeFrame, int[] javaFrame, long explainedLimit) {
        Judgement j = new Judgement(row.key(), e.tier());
        comparePixels(row, e, nativeFrame, javaFrame, explainedLimit, true, j);
        return j;
    }

    /**
     * The bound over every pixel (a pixel further from native than the golden bound has to be explained by the row's
     * cause unless it is fixed, and at most {@code explainedLimit} differing pixels may be explained) and the drift
     * (neither the largest delta nor the number of differing pixels may exceed the capture's).
     */
    static void comparePixels(GoldenRow row, Entry e, int[] nativeFrame, int[] javaFrame, long explainedLimit,
                              boolean driftIsFailure, Judgement j) {
        int width = Math.max(e.width(), 1);
        boolean sourceAligned = e.x() == 0 && e.y() == 0 && e.width() == row.width() && e.height() == row.height();
        int[] source = sourceAligned ? pattern(row.width(), row.height(), PRIMARY_SEED) : null;
        if (row.cause() != null && source == null) {
            j.fail(Kind.CAUSE_SHAPE, "cause " + row.causeId() + " needs a result with the bounds of its source");
            return;
        }
        int maxDelta = 0;
        long differing = 0;
        long explained = 0;
        long unexplained = 0;
        List<String> listed = new ArrayList<>();
        for (int i = 0; i < nativeFrame.length; i++) {
            if (nativeFrame[i] == javaFrame[i]) {
                continue;
            }
            int delta = channelDelta(nativeFrame[i], javaFrame[i]);
            differing++;
            maxDelta = Math.max(maxDelta, delta);
            if (row.cause() != null && row.cause().explainsSourcePixel(source[i])) {
                explained++;
            } else if (delta > e.bound()) {
                unexplained++;
                if (listed.size() < 16) {
                    listed.add(String.format(Locale.ROOT, "(%d,%d) src=%s golden=%08x java=%08x delta %d",
                            i % width, i / width, source == null ? "-" : String.format(Locale.ROOT, "%08x",
                            source[i]), nativeFrame[i], javaFrame[i], delta));
                }
            }
        }
        j.maxDelta = maxDelta;
        j.differing = differing;
        j.pixels = nativeFrame.length;
        j.explained = explained;
        if (unexplained > 0) {
            j.fail(Kind.BOUND_EXCEEDED, "bound exceeded at " + String.join(", ", listed)
                    + (unexplained > listed.size() ? ", ..." : "") + " (" + unexplained + " pixels above bound "
                    + e.bound() + " not explained by cause " + e.cause()
                    + (row.cause() != null && row.cause().fixed() ? " (fixed)" : "") + ")");
        }
        if (explained > explainedLimit) {
            j.fail(Kind.EXPLAINED_EXCEEDS_CAPTURE, "explained count exceeds capture: " + explained + " pixels"
                    + " explained by " + e.cause() + ", at most " + explainedLimit + " allowed");
        }
        if (maxDelta > e.fullMaxD() || differing > e.fullDiff()) {
            String drift = "drift away from native: maxD " + maxDelta + " (capture " + e.fullMaxD() + "), differing "
                    + differing + " (capture " + e.fullDiff() + ")";
            if (driftIsFailure) {
                j.fail(Kind.DRIFT, drift);
            } else {
                j.warnings.add(drift);
            }
        }
    }

    /**
     * A clipped render against its own unclipped render: no further apart than the native renders were, or the bound;
     * for a clipped blur, no pixel more than one step apart outside the rows next to the clip's top and bottom edges.
     */
    static void checkSelfConsistency(GoldenRow row, Entry e, Result clipped, Result full, Judgement j) {
        if (e.sseSelf() < 0) {
            j.fail(Kind.SELF_CONSISTENCY, "the golden has no sseSelf for this clipped row");
            return;
        }
        if (clipped.x() < full.x() || clipped.y() < full.y()
                || clipped.x() + clipped.width() > full.x() + full.width()
                || clipped.y() + clipped.height() > full.y() + full.height()) {
            j.fail(Kind.SELF_CONSISTENCY, "clipped result " + clipped.x() + "," + clipped.y() + " "
                    + clipped.width() + "x" + clipped.height() + " lies outside the unclipped result");
            return;
        }
        Result cropped = DecoraCorpus.crop(full, clipped);
        j.javaSelf = DecoraCorpus.maxDelta(clipped, cropped);
        int allowed = Math.max(e.sseSelf(), e.bound());
        if (j.javaSelf > allowed) {
            j.fail(Kind.SELF_CONSISTENCY, "Java clipped render differs from its unclipped render by " + j.javaSelf
                    + ", native by " + e.sseSelf() + ", bound " + e.bound());
        }
        if (row.edgeRows() >= 0) {
            List<String> outside = new ArrayList<>();
            int[] pc = clipped.pixels();
            int[] pf = cropped.pixels();
            for (int i = 0; i < pc.length; i++) {
                int y = i / clipped.width();
                if (channelDelta(pc[i], pf[i]) > ONE_STEP && y >= row.edgeRows()
                        && y < clipped.height() - row.edgeRows()) {
                    outside.add("(" + (i % clipped.width()) + "," + y + ")");
                }
            }
            if (!outside.isEmpty()) {
                j.fail(Kind.EDGE_ROWS, "Java clipped render differs from its unclipped render by more than one step"
                        + " outside the " + row.edgeRows() + " rows next to the clip edges: " + outside);
            }
        }
    }

    /** Null when the frames file matches the index's {@code frames} header, else what differs. */
    static String framesProblem(Index index, byte[] frames) {
        String expected = index.header("frames");
        String actual = DecoraGoldens.framesHeader(frames);
        return expected.equals(actual) ? null : "golden frames md5 or size mismatch: header '" + expected
                + "', file '" + actual + "'";
    }

    /** Null when the golden and the corpus have the same row keys, else both lists of differences. */
    static String rowSetProblem(List<String> goldenKeys, List<String> corpusKeys) {
        TreeSet<String> missing = new TreeSet<>(corpusKeys);
        missing.removeAll(goldenKeys);
        TreeSet<String> extra = new TreeSet<>(goldenKeys);
        extra.removeAll(corpusKeys);
        if (missing.isEmpty() && extra.isEmpty() && goldenKeys.size() == corpusKeys.size()) {
            return null;
        }
        return "golden rows differ from DecoraCorpus.rows(): in the corpus but not the golden " + missing
                + "; in the golden but not the corpus " + extra + " (golden " + goldenKeys.size() + " rows, corpus "
                + corpusKeys.size() + ")";
    }

    private static GoldenRow row(String key) {
        GoldenRow row = corpus.get(key);
        assertTrue(row != null, () -> "no corpus row " + key);
        return row;
    }

    private static Entry entry(GoldenRow row, Tier tier, int bound) {
        Entry e = entries.get(row.key());
        assertEquals(tier, e.tier(), () -> "tier of " + row.key());
        assertEquals(bound, e.bound(), () -> "golden bound of " + row.key());
        return e;
    }

    /** The first pixel where both frames agree; with {@code outsideCause}, one whose source the cause does not name. */
    private static int firstEqualPixel(int[] nativeFrame, int[] javaFrame, GoldenRow row, boolean outsideCause) {
        int[] source = pattern(row.width(), row.height(), PRIMARY_SEED);
        for (int i = 0; i < nativeFrame.length; i++) {
            if (nativeFrame[i] == javaFrame[i]
                    && (!outsideCause || !row.cause().appliesToSourcePixel().test(source[i]))) {
                return i;
            }
        }
        throw new AssertionError("no pixel where Java equals the golden in " + row.key());
    }

    /** The indices of the pixels whose primary source the row's cause names, in ascending order. */
    private static List<Integer> causePixels(GoldenRow row) {
        int[] source = pattern(row.width(), row.height(), PRIMARY_SEED);
        List<Integer> named = new ArrayList<>();
        for (int i = 0; i < source.length; i++) {
            if (row.cause().appliesToSourcePixel().test(source[i])) {
                named.add(i);
            }
        }
        return named;
    }

    private static String at(int index, Entry e) {
        return "(" + (index % e.width()) + "," + (index / e.width()) + ")";
    }

    /** The pixel with its blue channel moved by {@code steps}, up unless that leaves the 8-bit range. */
    private static int changeBlue(int argb, int steps) {
        int blue = argb & 0xFF;
        int moved = blue + steps <= 0xFF ? blue + steps : blue - steps;
        return (argb & ~0xFF) | moved;
    }

    private static int firstDataLineOffset(byte[] bytes) {
        int start = 0;
        while (bytes[start] == '#') {
            while (bytes[start] != '\n') {
                start++;
            }
            start++;
        }
        return start;
    }

    private static boolean isX64() {
        String arch = System.getProperty("os.arch", "");
        return arch.equals("amd64") || arch.equals("x86_64");
    }

    /**
     * A Java backend whose delegate names {@code peerClass} for {@code peerName} and the {@code JSW} peers for every
     * other name; the backend it creates is left in {@code used}.
     */
    private static Supplier<DecoraBackend> mutating(String peerName, Class<?> peerClass,
                                                    AtomicReference<DecoraBackend> used) {
        RendererDelegate delegate = new RendererDelegate() {
            @Override
            public AccelType getAccelType() {
                return AccelType.NONE;
            }

            @Override
            public String getPlatformPeerName(String name, int unrollCount) {
                return name.equals(peerName) ? peerClass.getName()
                        : Renderer.rootPkg + ".impl.sw.java.JSW" + name + "Peer";
            }
        };
        return () -> {
            DecoraBackend backend = DecoraBackend.javaWith(delegate);
            used.set(backend);
            return backend;
        };
    }

    /** Flips bit 1 of the blue channel of the result's pixel (0,0), which moves that channel by exactly 2. */
    private static void flipBlueBit(ImageData result) {
        ((HeapImage) result.getUntransformedImage()).getPixelArray()[0] ^= 0x2;
    }

    /** {@code JSWBoxBlurPeer} with pixel (0,0) of every pass-1 result two steps off. */
    public static final class OffByTwoBoxBlurPeer extends JSWBoxBlurPeer {

        public OffByTwoBoxBlurPeer(FilterContext fctx, Renderer r, String uniqueName) {
            super(fctx, r, uniqueName);
        }

        @Override
        public ImageData filter(Effect effect, BoxRenderState state, BaseTransform transform, Rectangle outputClip,
                                ImageData... inputs) {
            ImageData result = super.filter(effect, state, transform, outputClip, inputs);
            if (getPass() == 1) {
                flipBlueBit(result);
            }
            return result;
        }
    }

    /** {@code JSWLinearConvolveShadowPeer} with pixel (0,0) of every pass-1 result two steps off. */
    public static final class OffByTwoLinearConvolveShadowPeer extends JSWLinearConvolveShadowPeer {

        public OffByTwoLinearConvolveShadowPeer(FilterContext fctx, Renderer r, String uniqueName) {
            super(fctx, r, uniqueName);
        }

        @Override
        public ImageData filter(Effect effect, LinearConvolveRenderState state, BaseTransform transform,
                                Rectangle outputClip, ImageData... inputs) {
            ImageData result = super.filter(effect, state, transform, outputClip, inputs);
            if (getPass() == 1) {
                flipBlueBit(result);
            }
            return result;
        }
    }
}
