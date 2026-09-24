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

package test.com.sun.javafx.iio;

import com.sun.javafx.iio.ImageStorage.ImageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.iio.JpegTestSupport.Decoded;
import test.com.sun.javafx.iio.JpegTestSupport.Variant;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Decodes the JPEG corpus and compares every decoded byte against values captured from the JNI
 * implementation.
 * <p>
 * Before this test existed, nothing in the repository asserted a single decoded JPEG pixel: the only
 * end-to-end coverage was {@code tests/system}'s {@code LoadCorruptJPEGTest}, which loads one corrupt
 * file, checks that the JVM survives, and only runs under {@code -DFULL_TEST=true}. A decoder can be
 * rewritten from scratch, get the colour conversion wrong, drop the last partial MCU row or ignore
 * an embedded profile, and no test here would have noticed.
 * <p>
 * The goldens are SHA-256 of the decoded {@code byte[]} plus width, height, {@code ImageType} and
 * stride - the pixels are not checked in, only their digest, so the evidence costs a few hundred
 * bytes and is still exact. They were captured by {@link JpegCorpusGenerator} from the pre-migration
 * JNI build; see that class for why they are never regenerated to make a rewrite pass.
 * <p>
 * The assertions divide into two kinds, and the difference matters when one of them fails:
 * <ul>
 * <li><b>Goldens</b> - the digests, dimensions and stride. A mismatch means the decoder produces
 *     different pixels than the C did. Nothing about the migration justifies that, so a mismatch is
 *     the finding and the fix belongs in the decoder.</li>
 * <li><b>Derived invariants</b> - the relationships checked by the last three tests: stride is
 *     width times the band count, a half-size request really does halve, a downscale really does
 *     produce different bytes. These follow from the API and hold for any correct decoder, so they
 *     are readable on their own and stay meaningful if the corpus is ever recaptured.</li>
 * </ul>
 * The golden and the corpus fail loudly when absent ({@link JpegGoldens#load()},
 * {@code JpegTestSupport.requireResource}); they are never a reason to skip. And the two golden
 * comparisons are counted key by key, so a run that compared nothing - an empty corpus, a golden
 * with no {@code decode.} keys - fails at the end of the class ({@link ParityGate}).
 */
public class JpegDecodeParityTest {

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(JpegDecodeParityTest.class);

    private static JpegGoldens goldens;
    private static Map<String, byte[]> corpus;

    @BeforeAll
    static void loadCorpusAndGoldens() {
        JpegNatives.require();
        goldens = JpegGoldens.load();
        List<String> images = JpegGoldens.splitList(goldens.require("images"));
        List<String> missing = new ArrayList<>(JpegTestSupport.REQUIRED_IMAGES);
        missing.removeAll(images);
        assertTrue(missing.isEmpty(), () -> "the golden file was captured without the mandatory"
                + " corpus members " + missing + ", so those decode paths have no evidence behind"
                + " them. Recapture with JpegCorpusGenerator.");
        corpus = JpegTestSupport.assembleCorpus(images, JpegTestSupport::requireResource);
    }

    /** A class that compared nothing against the golden must not be reported green. */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * The corpus files themselves are hashed, so a corpus that has been regenerated, re-encoded by a
     * different JDK or mangled by a text-mode checkout fails here - with an explanation - instead of
     * failing as a wall of pixel mismatches.
     */
    @Test
    void corpusFilesAreTheOnesTheGoldensWereCapturedFrom() {
        LEDGER.oracleAvailable();
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            byte[] jpeg = member.getValue();
            compare(mismatches, "image." + member.getKey() + ".length",
                    Integer.toString(jpeg.length));
            compare(mismatches, "image." + member.getKey() + ".sha256",
                    JpegTestSupport.sha256Hex(jpeg));
        }
        if (!mismatches.isEmpty()) {
            fail("the corpus no longer matches the files the goldens were captured from, so the"
                    + " decoded-pixel comparison below would be meaningless:\n  "
                    + String.join("\n  ", mismatches) + "\n" + provenance());
        }
    }

    /**
     * The parity assertion: every corpus member, at every captured variant, decoded and reduced to
     * the same summary the generator recorded.
     * <p>
     * All mismatches are collected and reported together. One decoder change usually moves many
     * images at once, and the shape of that set - every image, or only the progressive one, or only
     * the odd-sized one - is what points at the cause.
     */
    @Test
    void decodesEveryCorpusMemberExactlyAsTheJniBuildDid() {
        LEDGER.oracleAvailable();
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            for (Variant variant : JpegTestSupport.variantsFor(member.getKey())) {
                String prefix = "decode." + member.getKey() + "." + variant.name() + ".";
                Map<String, String> captured = JpegTestSupport.captureVariant(member.getValue(),
                        variant);
                for (Map.Entry<String, String> entry : captured.entrySet()) {
                    compare(mismatches, prefix + entry.getKey(), entry.getValue());
                }
                for (String key : goldens.keysWithPrefix(prefix)) {
                    if (!captured.containsKey(key.substring(prefix.length()))) {
                        mismatches.add(key + ": golden \"" + goldens.get(key) + "\", but this decode"
                                + " produced no such value (the outcome itself changed)");
                    }
                }
            }
        }
        if (!mismatches.isEmpty()) {
            fail("JPEG decoding no longer matches the JNI build:\n  " + String.join("\n  ", mismatches)
                    + "\n" + provenance());
        }
    }

    /**
     * Every corpus member and variant this test would check is actually covered by the golden file.
     * Without this, deleting a golden entry would quietly turn its case into a no-op that passes.
     */
    @Test
    void goldenFileCoversEveryCorpusMemberAndVariant() {
        Set<String> expected = new LinkedHashSet<>();
        for (String name : corpus.keySet()) {
            for (Variant variant : JpegTestSupport.variantsFor(name)) {
                expected.add(name + "." + variant.name());
            }
        }
        Set<String> captured = new LinkedHashSet<>();
        for (String key : goldens.keysWithPrefix("decode.")) {
            String rest = key.substring("decode.".length());
            int lastDot = rest.lastIndexOf('.');
            captured.add(rest.substring(0, lastDot));
        }
        assertEquals(new TreeSet<>(expected), new TreeSet<>(captured),
                "the golden file covers a different set of (image, variant) pairs than this test"
                        + " decodes; a case is either untested or untestable. " + provenance());
    }

    /**
     * Derived invariant: stride is width times the number of bands the frame's {@code ImageType}
     * implies, for every image in the corpus that decodes. The C returns the band count separately
     * from the colour space and {@code JPEGImageLoader} multiplies it out by hand; if the two ever
     * disagree, every consumer of the frame reads the rows at the wrong offset.
     */
    @Test
    void strideAlwaysMatchesWidthTimesBandCount() throws IOException {
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            if (!"ok".equals(goldens.get("decode." + member.getKey() + ".full.outcome"))) {
                continue; // This member is captured as a failure; the failure itself is asserted above.
            }
            Decoded decoded = JpegTestSupport.decode(member.getValue(), JpegTestSupport.FULL);
            int bands = bandCount(decoded.type());
            assertEquals(decoded.width() * bands, decoded.stride(),
                    () -> member.getKey() + ": stride does not match width x bands for "
                            + decoded.type());
            assertEquals(decoded.stride() * decoded.height(), decoded.pixels().length,
                    () -> member.getKey() + ": the pixel buffer is not stride x height");
        }
    }

    /**
     * Derived invariant for the grayscale member: one band, so the buffer is exactly width x height.
     * Pinned separately from the digest because a decoder that returned three identical grey
     * components would still be "an image", just not this one.
     */
    @Test
    void grayscaleDecodesToASingleBand() throws IOException {
        Decoded decoded = JpegTestSupport.decode(corpus.get(JpegTestSupport.GRAY),
                JpegTestSupport.FULL);
        assertEquals(ImageType.GRAY, decoded.type(), "a JCS_GRAYSCALE JPEG must decode to GRAY");
        assertEquals(32, decoded.width());
        assertEquals(32, decoded.height());
        assertEquals(32, decoded.stride());
        assertEquals(32 * 32, decoded.pixels().length);
    }

    /**
     * Derived invariants of the downscaling path, which the goldens pin byte for byte and this test
     * describes in terms of the API.
     * <p>
     * 64x48 to 32x24 is exactly the case {@code startDecompression} serves with libjpeg's own
     * {@code scale_denom = 2}, so no Java resampling runs at all. 64x48 to 40x30 is the other side of
     * that decision - {@code max_scale} of 0.625 keeps {@code scale_denom} at 1 - so libjpeg returns
     * the full size and {@code ImageTools.scaleImage} finishes the job, once smooth and once not.
     * The two filters must not agree: if they do, the smooth flag stopped reaching the resampler.
     */
    @Test
    void downscalingProducesTheRequestedSizeThroughBothPaths() throws IOException {
        byte[] baseline = corpus.get(JpegTestSupport.BASELINE);
        Decoded full = JpegTestSupport.decode(baseline, JpegTestSupport.FULL);
        assertEquals(64, full.width());
        assertEquals(48, full.height());

        Decoded half = JpegTestSupport.decode(baseline, JpegTestSupport.HALF);
        assertEquals(32, half.width(), "a 32x24 request from 64x48 must come back at 32x24");
        assertEquals(24, half.height());
        assertEquals(32 * bandCount(half.type()), half.stride());
        assertEquals(half.stride() * 24, half.pixels().length);
        assertNotEquals(JpegTestSupport.sha256Hex(full.pixels()),
                JpegTestSupport.sha256Hex(half.pixels()),
                "the half-size decode returned the full-size bytes, so scale_denom was ignored");

        Decoded smooth = JpegTestSupport.decode(baseline, JpegTestSupport.W40H30_SMOOTH);
        Decoded rough = JpegTestSupport.decode(baseline, JpegTestSupport.W40H30_ROUGH);
        for (Decoded decoded : List.of(smooth, rough)) {
            assertEquals(40, decoded.width(), "a 40x30 request from 64x48 must come back at 40x30");
            assertEquals(30, decoded.height());
            assertEquals(40 * bandCount(decoded.type()), decoded.stride());
            assertEquals(decoded.stride() * 30, decoded.pixels().length);
        }
        assertNotEquals(JpegTestSupport.sha256Hex(smooth.pixels()),
                JpegTestSupport.sha256Hex(rough.pixels()),
                "smooth and rough downscaling produced identical bytes, so the smooth flag is not"
                        + " reaching ImageTools.scaleImage");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static void compare(List<String> mismatches, String key, String actual) {
        LEDGER.compared();
        String expected = goldens.get(key);
        if (expected == null) {
            mismatches.add(key + ": not in the golden file, but this run produced \"" + actual + "\"");
        } else if (!expected.equals(actual)) {
            mismatches.add(key + ": golden \"" + expected + "\", now \"" + actual + "\"");
        }
    }

    /**
     * Bands per pixel for the image types a JPEG can produce. Spelled out rather than taken from
     * {@code ImageStorage}, so this test states the expectation instead of restating the
     * implementation.
     */
    private static int bandCount(ImageType type) {
        return switch (type) {
            case GRAY -> 1;
            case RGB -> 3;
            case RGBA, RGBA_PRE -> 4;
            default -> throw new AssertionError("a JPEG decoded to " + type + ", which"
                    + " JPEGImageLoader.setInputAttributes cannot produce");
        };
    }

    private static String provenance() {
        String captured = goldens.get("capture.provenance");
        return "The goldens were captured on: " + (captured == null ? "(not recorded)" : captured)
                + ". They are evidence of what the JNI decoder did; do not regenerate them to make"
                + " this pass.";
    }
}
