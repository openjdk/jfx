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

package test.com.sun.pisces;

import com.sun.pisces.GradientColorMap;
import com.sun.pisces.JavaSurface;
import com.sun.pisces.PiscesRenderer;
import com.sun.pisces.RendererBase;
import com.sun.pisces.Transform6;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pixel-exact parity harness for the {@code prism_sw} (Pisces) compositor.
 * <p>
 * A fixed script drives {@link PiscesRenderer} and {@link JavaSurface} through every operation the
 * software pipeline ({@code com.sun.prism.sw}) uses, on a 64x64 {@code TYPE_INT_ARGB_PRE} surface, and
 * takes a full-frame snapshot of the surface after every step. The golden resource
 * {@value #GOLDEN_RESOURCE} is the raw big-endian concatenation of those snapshots, captured from the
 * <b>JNI build</b> at commit {@code a544256444} (the hash in its name) on Windows x64.
 * <p>
 * Every later change to the Java side of Pisces - the JNI to FFM flip first of all - has to reproduce
 * this file byte for byte: the C bodies do not change in a migration, so any difference is a
 * marshalling bug in Java and is fixed there, never by regenerating the golden. Regenerating it is a
 * behaviour change and needs its own commit with the reason stated.
 * <p>
 * The golden is exact across platforms only where the C is integer-only, which is most of it: blits,
 * masks, fill and clear, get and set RGB. Four groups of steps are not ({@link #FLOAT_STEPS}). Steps 6
 * to 11 (the linear and radial gradients) and 24 (alpha rows under a gradient paint) run float set-up
 * in {@code PiscesRenderer.inl} and a per-pixel {@code sqrt} and {@code frac += mx} in
 * {@code PiscesPaint.c}, contractable to FMA on aarch64; steps 12 to 19 (the four {@code setTexture}
 * paints and the four {@code drawImage} calls) invert their texture transform in
 * {@code pisces_transform_invert} ({@code native-prism-sw/PiscesTransform.c:55}, {@code :61-62} at commit
 * {@code a544256444}, the same lines at {@code 8492cb03b0}: {@code fdet} and the two
 * translation terms are float products and differences, divided by {@code fdet} and truncated back to
 * 16.16, so one ulp there moves the sampling grid) - texture sampling is integer, but only once the
 * inverse is; step 22 (the LCD mask at gamma 1.4) reads a lookup table built with libm {@code pow} in
 * {@code PiscesBlit.c}. The comparison runs every step and reports every mismatch in one failure, each
 * named with its class: a mismatch on another platform that is confined to float steps is C float
 * variance, not a Java marshalling bug, and is handled by a per-platform golden added in its own commit,
 * never by regenerating this one; a mismatch in an integer step, or on Windows x64 at all, is a bug on
 * the Java side, and is reported as such even when float steps differ alongside it.
 * <p>
 * The JNI glue that produced this golden ({@code native-prism-sw/JPiscesRenderer.c} and its siblings) was deleted in
 * commit {@code 45f18c168a}. To extend or re-verify this corpus, check out {@code a544256444} and capture there;
 * capturing on a later commit proves only that the FFM path agrees with itself. The
 * per-step comparisons are counted, and a run that compared none of them fails at the end of the class
 * ({@link ParityGate}); a capture run compares nothing by design and is reported as skipped.
 * <p>
 * Capture mode: with {@code -Dpisces.golden.capture=<absolute path>} the test writes the file to that
 * path instead of asserting against the resource, then aborts, so a capture run is reported as skipped
 * and can never be read as a green verification. The per-step layout exists so that a mismatch names
 * the operation that broke rather than only the pixel.
 */
public class PiscesGoldenRenderTest {

    static final String GOLDEN_RESOURCE = "pisces-golden-a544256444.bin";
    static final String CAPTURE_PROPERTY = "pisces.golden.capture";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(PiscesGoldenRenderTest.class);

    static final int W = 64;
    static final int H = 64;

    /** Coverage maximum of the synthetic alpha rows; independent of Marlin's configured subpixel grid. */
    static final int MAX_ALPHA = 64;

    /**
     * The steps whose C runs float arithmetic (see the class comment): 6-11 the gradients, 12-19 the
     * texture paints and {@code drawImage} calls through {@code pisces_transform_invert}, 22 the LCD mask at
     * gamma 1.4, 24 the alpha rows under a gradient paint. Every other step is integer-only and has to match
     * on every platform.
     */
    static final Set<Integer> FLOAT_STEPS = Set.of(6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 22, 24);

    @BeforeAll
    static void requireNatives() {
        PiscesNatives.require();
    }

    /** The golden runs on every machine: once the comparison started, every step must have been compared. */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    @Test
    public void rendersExactlyAsTheGoldenBuildDid() throws IOException {
        Script script = new Script();
        script.run();
        String capture = System.getProperty(CAPTURE_PROPERTY);
        if (capture != null && !capture.isBlank()) {
            script.writeGolden(Path.of(capture));
            Assumptions.abort("golden captured to " + capture + "; a capture run verifies nothing");
        }
        LEDGER.oracleAvailable();
        script.assertMatchesGolden();
    }

    /**
     * The report has to name an integer step that differs even when float steps differ too, and has to
     * class each. The script's own frames stand in for the golden; in a copy, one pixel of step 0
     * (integer), one of step 7 (a gradient) and one of step 13 (a texture paint, float through
     * {@code pisces_transform_invert}) are perturbed; the comparison must list all three, class them, and
     * count the integer one in the verdict rather than stop at, or fold it into, the float ones. The
     * golden resource is not involved.
     */
    @Test
    public void anIntegerStepMismatchIsReportedEvenWhenFloatStepsAlsoDiffer() {
        Script script = new Script();
        script.run();
        List<int[]> golden = script.steps.stream().map(step -> step.snapshot().clone()).toList();
        List<Step> perturbed = new ArrayList<>(script.steps);
        perturb(perturbed, 0, 4095);
        perturb(perturbed, 7, 100);
        perturb(perturbed, 13, 2049);
        assertEquals(List.of(), compare(script.steps, golden), "unperturbed frames compare clean");

        List<Mismatch> mismatches = compare(perturbed, golden);
        assertEquals(List.of(0, 7, 13), mismatches.stream().map(Mismatch::step).toList(),
                "every differing step, in step order");
        Mismatch integer = mismatches.get(0);
        assertFalse(integer.floatStep(), "step 0 is integer-only");
        assertEquals(4095, integer.firstPixel());
        assertEquals(1, integer.differing());
        assertEquals(golden.get(0)[4095], integer.expected());
        assertEquals(perturbed.get(0).snapshot()[4095], integer.actual());
        assertTrue(mismatches.get(1).floatStep(), "step 7 is a gradient");
        assertTrue(mismatches.get(2).floatStep(), "step 13 is a texture paint: pisces_transform_invert");

        String message = describe(mismatches, perturbed.size());
        String verdict = "3 of " + perturbed.size() + " steps differ from the golden: 1 in integer steps";
        assertTrue(message.startsWith(verdict), message);
        assertTrue(message.contains("step 0 '" + perturbed.get(0).name() + "' [integer]: pixel (63,63)"), message);
        assertTrue(message.contains("step 7 '" + perturbed.get(7).name() + "' [float]"), message);
        assertTrue(message.contains("step 13 '" + perturbed.get(13).name() + "' [float]"), message);
        assertTrue(message.indexOf("step 0 '") < message.indexOf("step 7 '"), "integer steps are listed first");

        assertTrue(describe(mismatches.subList(1, 3), perturbed.size()).contains("all in float steps"),
                "float-only mismatches are called out as such");
    }

    /** Replaces step {@code index} with a copy whose pixel {@code pixel} has one colour bit flipped. */
    private static void perturb(List<Step> steps, int index, int pixel) {
        int[] snapshot = steps.get(index).snapshot().clone();
        snapshot[pixel] ^= 0x00010000;
        steps.set(index, new Step(steps.get(index).name(), snapshot));
    }

    @Test
    public void getRGBAndSetRGBRoundTrip() {
        int[] pixels = new int[W * H];
        JavaSurface surface = new JavaSurface(pixels, RendererBase.TYPE_INT_ARGB_PRE, W, H);

        int[] block = new int[3 + 6 * 4];
        for (int i = 0; i < block.length; i++) {
            block[i] = 0xFF000000 | (i * 0x010203);
        }
        surface.setRGB(block, 3, 6, 10, 20, 4, 4);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                assertEquals(block[3 + row * 6 + col], pixels[(20 + row) * W + 10 + col],
                        "pixel (" + (10 + col) + "," + (20 + row) + ")");
            }
        }

        int[] back = new int[5 + 7 * 4];
        surface.getRGB(back, 5, 7, 10, 20, 4, 4);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                assertEquals(pixels[(20 + row) * W + 10 + col], back[5 + row * 7 + col],
                        "read-back (" + col + "," + row + ")");
            }
        }
        int[] all = new int[W * H];
        surface.getRGB(all, 0, W, 0, 0, W, H);
        assertArrayEquals(pixels, all);
    }

    /**
     * A range the Java-side {@code AbstractSurface.rgbCheck} rejects. The one check only the C side
     * made ({@code offset + height * scanLength} elements) is exercised in {@code PiscesNativeTest}: the
     * JNI build could not run it, because its {@code JNI_ThrowNew} treated the exception it had just
     * raised as a failure and aborted the JVM ({@code FatalError("Failed to throw an exception!")}).
     */
    @Test
    public void getRGBOutsideSurfaceThrowsTheJavaMessage() {
        JavaSurface surface = new JavaSurface(new int[W * H], RendererBase.TYPE_INT_ARGB_PRE, W, H);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> surface.getRGB(new int[W * H], 0, W, 60, 0, 8, 1));
        assertEquals("X+WIDTH is out of surface", e.getMessage());
    }

    @Test
    public void emitAndClearAlphaRowRejectsRangesBeyondTheDeltas() {
        int[] pixels = new int[W * H];
        PiscesRenderer pr = new PiscesRenderer(new JavaSurface(pixels, RendererBase.TYPE_INT_ARGB_PRE, W, H));
        byte[] alphaMap = alphaMap();
        int[] deltas = new int[16];
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> pr.emitAndClearAlphaRow(alphaMap, deltas, 3, 0, 20, 0));
        assertEquals("rendering range exceeds length of data", e.getMessage());
    }

    @Test
    public void emitAndClearAlphaRowZeroesTheDeltasInPlace() {
        int[] pixels = new int[W * H];
        PiscesRenderer pr = new PiscesRenderer(new JavaSurface(pixels, RendererBase.TYPE_INT_ARGB_PRE, W, H));
        pr.setClip(0, 0, W, H);
        pr.setColor(10, 20, 30, 255);
        int[] deltas = new int[80];
        int width = fillRampDeltas(deltas, 4);
        pr.emitAndClearAlphaRow(alphaMap(), deltas, 7, 3, 3 + width - 1, 4, 0);
        assertArrayEquals(new int[80], deltas, "the live delta array must be zeroed by the call");
        assertTrue(pixels[7 * W + 3] != 0 || pixels[7 * W + 10] != 0, "the row was rendered");
    }

    static int toS(float v) {
        return (int) (v * 65536f);
    }

    /** Marlin's {@code setMaxAlpha} mapping for {@link #MAX_ALPHA}. */
    static byte[] alphaMap() {
        byte[] map = new byte[MAX_ALPHA + 1];
        for (int i = 0; i <= MAX_ALPHA; i++) {
            map[i] = (byte) ((i * 255 + MAX_ALPHA / 2) / MAX_ALPHA);
        }
        return map;
    }

    /**
     * Writes a coverage ramp as relative deltas at {@code deltas[off..off+16)}: eight steps up to
     * {@link #MAX_ALPHA}, a plateau, four steps back to zero. Returns the number of pixels covered.
     */
    static int fillRampDeltas(int[] deltas, int off) {
        for (int i = 0; i < 8; i++) {
            deltas[off + i] = MAX_ALPHA / 8;
        }
        for (int i = 12; i < 16; i++) {
            deltas[off + i] = -(MAX_ALPHA / 4);
        }
        return 16;
    }

    static int[] opaqueTexture() {
        int[] tex = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                tex[y * 16 + x] = 0xFF000000 | ((x * 17) << 16) | ((y * 17) << 8) | ((x ^ y) * 17);
            }
        }
        return tex;
    }

    /** Premultiplied: every colour channel is at most the alpha channel. */
    static int[] translucentTexture() {
        int[] tex = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int a = x * 17;
                int r = (a * y) / 15;
                int g = a >> 1;
                int b = (a * (15 - y)) / 15;
                tex[y * 16 + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return tex;
    }

    static byte[] glyphMask(int w, int h) {
        byte[] mask = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mask[y * w + x] = (byte) ((x * 255) / Math.max(1, w - 1) ^ (y * 29));
            }
        }
        return mask;
    }

    private record Step(String name, int[] snapshot) {
    }

    /** One step whose snapshot differs from its golden frame: where it first differs, and by how much. */
    record Mismatch(int step, String name, int firstPixel, int expected, int actual, int differing) {

        boolean floatStep() {
            return FLOAT_STEPS.contains(step);
        }

        String describe() {
            return String.format("step %d '%s' [%s]: pixel (%d,%d) expected 0x%08X but was 0x%08X"
                    + " (first of %d differing pixels)", step, name, floatStep() ? "float" : "integer",
                    firstPixel % W, firstPixel / W, expected, actual, differing);
        }
    }

    /**
     * Compares every snapshot with its golden frame and reports every step that differs, in step order,
     * so that one run names every broken operation and an integer-step regression is never hidden behind
     * a float-step difference that happens to come first.
     */
    static List<Mismatch> compare(List<Step> steps, List<int[]> golden) {
        List<Mismatch> mismatches = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            int[] expected = golden.get(i);
            int[] actual = step.snapshot();
            int first = -1;
            int differing = 0;
            for (int p = 0; p < expected.length; p++) {
                if (expected[p] != actual[p]) {
                    if (first < 0) {
                        first = p;
                    }
                    differing++;
                }
            }
            if (first >= 0) {
                mismatches.add(new Mismatch(i, step.name(), first, expected[first], actual[first], differing));
            }
        }
        return mismatches;
    }

    /** The one failure message: the verdict, then every mismatch, integer steps before float steps. */
    static String describe(List<Mismatch> mismatches, int stepCount) {
        List<Mismatch> integer = mismatches.stream().filter(m -> !m.floatStep()).toList();
        List<Mismatch> floating = mismatches.stream().filter(Mismatch::floatStep).toList();
        StringBuilder message = new StringBuilder();
        message.append(mismatches.size()).append(" of ").append(stepCount).append(" steps differ from the golden: ");
        if (integer.isEmpty()) {
            message.append("all in float steps, which on a platform other than Windows x64 is C float variance"
                    + " (per-platform golden in its own commit, never a regeneration) and on Windows x64 is a"
                    + " Java-side bug");
        } else {
            message.append(integer.size()).append(" in integer steps, which is a Java-side bug on every platform");
            if (!floating.isEmpty()) {
                message.append(", and ").append(floating.size()).append(" in float steps");
            }
        }
        for (Mismatch mismatch : integer) {
            message.append(System.lineSeparator()).append("  ").append(mismatch.describe());
        }
        for (Mismatch mismatch : floating) {
            message.append(System.lineSeparator()).append("  ").append(mismatch.describe());
        }
        return message.toString();
    }

    /** {@code count} big-endian frames of {@code W * H} ints, as the golden stores them. */
    static List<int[]> frames(byte[] bytes, int count) {
        IntBuffer ints = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        List<int[]> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int[] frame = new int[W * H];
            ints.get(frame);
            frames.add(frame);
        }
        return frames;
    }

    /** The deterministic sequence of operations and the snapshot taken after each. */
    private static final class Script {

        private final int[] pixels = new int[W * H];
        private final JavaSurface surface = new JavaSurface(pixels, RendererBase.TYPE_INT_ARGB_PRE, W, H);
        private final PiscesRenderer pr = new PiscesRenderer(surface);
        private final List<Step> steps = new ArrayList<>();

        private void snapshot(String name) {
            steps.add(new Step(name, pixels.clone()));
        }

        void run() {
            int[] fractions = {0x0000, 0x8000, 0x10000};
            int[] rgba = {0xFFFF0000, 0x8000FF00, 0xFF0000FF};
            Transform6 gradientTx = new Transform6(toS(0.8f), toS(0.2f), toS(-0.3f), toS(1.1f), toS(2f), toS(-1f));
            int[] opaque = opaqueTexture();
            int[] translucent = translucentTexture();

            int[] background = new int[W * H];
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    background[y * W + x] = 0xFF000000 | ((x * 4) << 16) | ((y * 4) << 8) | ((x ^ y) * 4);
                }
            }
            surface.setRGB(background, 0, W, 0, 0, W, H);
            pr.setClip(0, 0, W, H);
            pr.clearRect(8, 8, 48, 48);
            snapshot("setRGB background + clearRect");

            pr.setClip(10, 10, 20, 20);
            pr.setCompositeRule(RendererBase.COMPOSITE_SRC_OVER);
            pr.setColor(20, 220, 20, 255);
            pr.fillRect(0, 0, toS(W), toS(H));
            pr.resetClip();
            snapshot("setClip + fillRect + resetClip");

            pr.setColor(200, 30, 60, 180);
            pr.fillRect(toS(10.25f), toS(10.5f), toS(20.5f), toS(15.75f));
            snapshot("fillRect SRC_OVER fractional edges");

            pr.setCompositeRule(RendererBase.COMPOSITE_SRC);
            pr.setColor(0, 128, 255, 128);
            pr.fillRect(toS(4.5f), toS(30.25f), toS(25.25f), toS(10.5f));
            snapshot("fillRect SRC translucent");

            pr.setCompositeRule(RendererBase.COMPOSITE_CLEAR);
            pr.fillRect(toS(12f), toS(12f), toS(6.5f), toS(6.5f));
            snapshot("fillRect CLEAR");

            pr.setCompositeRule(RendererBase.COMPOSITE_SRC_OVER);
            pr.setColor(255, 255, 0);
            pr.fillRect(toS(40.75f), toS(2.25f), toS(20f), toS(20f));
            snapshot("fillRect SRC_OVER opaque, clipped by the surface");

            pr.setLinearGradient(toS(5f), toS(5f), toS(40f), toS(30f), fractions, rgba,
                    GradientColorMap.CYCLE_NONE, gradientTx);
            pr.fillRect(toS(2f), toS(2f), toS(30f), toS(28f));
            snapshot("linear gradient CYCLE_NONE");

            pr.setLinearGradient(toS(10f), toS(10f), toS(20f), toS(15f), fractions, rgba,
                    GradientColorMap.CYCLE_REPEAT, gradientTx);
            pr.fillRect(toS(32f), toS(2f), toS(30f), toS(28f));
            snapshot("linear gradient CYCLE_REPEAT");

            pr.setLinearGradient(toS(4f), toS(36f), 0xFF00FFFF, toS(14f), toS(46f), 0x40FF0080,
                    GradientColorMap.CYCLE_REFLECT);
            pr.fillRect(toS(2f), toS(32f), toS(30f), toS(30f));
            snapshot("linear gradient CYCLE_REFLECT (two-colour overload, identity)");

            Transform6 radialTx = new Transform6(toS(1.2f), toS(-0.1f), toS(0.15f), toS(0.9f), toS(-3f), toS(4f));
            pr.setRadialGradient(toS(48f), toS(48f), toS(44f), toS(50f), toS(12f), fractions, rgba,
                    GradientColorMap.CYCLE_NONE, radialTx);
            pr.fillRect(toS(34f), toS(34f), toS(28f), toS(28f));
            snapshot("radial gradient CYCLE_NONE");

            pr.setRadialGradient(toS(48f), toS(48f), toS(46f), toS(47f), toS(5f), fractions, rgba,
                    GradientColorMap.CYCLE_REPEAT, radialTx);
            pr.fillRect(toS(34f), toS(34f), toS(28f), toS(28f));
            snapshot("radial gradient CYCLE_REPEAT");

            pr.setRadialGradient(toS(48f), toS(48f), toS(48f), toS(48f), toS(6f), fractions, rgba,
                    GradientColorMap.CYCLE_REFLECT, null);
            pr.fillRect(toS(34.5f), toS(34.5f), toS(27f), toS(27f));
            snapshot("radial gradient CYCLE_REFLECT (null transform)");

            Transform6 fitTx = new Transform6(toS(0.5f), 0, 0, toS(0.5f), toS(-1f), toS(-1f));
            pr.setTexture(RendererBase.TYPE_INT_ARGB_PRE, opaque, 16, 16, 16, fitTx, false, false, false);
            pr.fillRect(toS(2f), toS(2f), toS(32f), toS(32f));
            snapshot("setTexture repeat=false linear=false alpha=false");

            Transform6 tileTx = new Transform6(toS(1.3f), toS(0.05f), toS(-0.05f), toS(1.3f), toS(0.5f), toS(0.25f));
            pr.setTexture(RendererBase.TYPE_INT_ARGB_PRE, translucent, 16, 16, 16, tileTx, true, true, true);
            pr.fillRect(toS(30f), toS(30f), toS(34f), toS(34f));
            snapshot("setTexture repeat=true linear=true alpha=true");

            pr.setTexture(RendererBase.TYPE_INT_ARGB_PRE, opaque, 12, 12, 16, tileTx, true, false, false);
            pr.fillRect(toS(2.5f), toS(34f), toS(28f), toS(28f));
            snapshot("setTexture repeat=true linear=false alpha=false, stride > width");

            pr.setTexture(RendererBase.TYPE_INT_ARGB_PRE, translucent, 16, 16, 16, fitTx, false, true, true);
            pr.fillRect(toS(2f), toS(2f), toS(32f), toS(32f));
            snapshot("setTexture repeat=false linear=true alpha=true");

            pr.setColor(0, 0, 0, 255);
            float bx = 20.5f;
            float by = 20.25f;
            float bw = 24.5f;
            float bh = 20.75f;
            Transform6 imageTx = new Transform6(toS(16f / bw), toS(0.05f), toS(-0.03f), toS(16f / bh),
                    toS(-bx * 16f / bw), toS(-by * 16f / bh));
            drawImage(RendererBase.IMAGE_MODE_NORMAL, translucent, 16, 16, 0, 16, imageTx, false, true,
                    bx, by, bw, bh, RendererBase.IMAGE_FRAC_EDGE_KEEP);
            snapshot("drawImage NORMAL, edges KEEP");

            drawImage(RendererBase.IMAGE_MODE_NORMAL, opaque, 16, 16, 0, 16, imageTx, false, false,
                    bx + 0.5f, by + 12f, bw, bh, RendererBase.IMAGE_FRAC_EDGE_PAD);
            snapshot("drawImage NORMAL, edges PAD");

            drawImage(RendererBase.IMAGE_MODE_NORMAL, translucent, 16, 16, 0, 16, imageTx, true, true,
                    bx - 10.25f, by + 6.5f, bw, bh, RendererBase.IMAGE_FRAC_EDGE_TRIM);
            snapshot("drawImage NORMAL repeat, edges TRIM");

            pr.setColor(255, 255, 255, 128);
            drawImage(RendererBase.IMAGE_MODE_MULTIPLY, opaque, 12, 12, 34, 16, imageTx, false, true,
                    bx, by, bw, bh, RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD,
                    RendererBase.IMAGE_FRAC_EDGE_TRIM, RendererBase.IMAGE_FRAC_EDGE_KEEP, 0, 0, 11, 11);
            snapshot("drawImage MULTIPLY with composite alpha, offset and stride > width, mixed edges");

            pr.setColor(10, 200, 90, 255);
            byte[] mask = glyphMask(12, 10);
            pr.fillAlphaMask(mask, 5, 45, 12, 10, 0, 12);
            pr.fillAlphaMask(mask, 20, 45, 8, 6, 13, 8);
            snapshot("fillAlphaMask (whole mask, then offset sub-mask)");

            byte[] lcdMask = glyphMask(30, 8);
            pr.setLCDGammaCorrection(1f);
            pr.setColor(0, 0, 0, 255);
            pr.fillLCDAlphaMask(lcdMask, 30, 50, 30, 8, 0, 30);
            snapshot("fillLCDAlphaMask gamma 1.0");

            pr.setLCDGammaCorrection(1.4f);
            pr.setColor(255, 40, 40, 255);
            pr.fillLCDAlphaMask(lcdMask, 30, 56, 30, 8, 0, 30);
            snapshot("fillLCDAlphaMask gamma 1.4");

            pr.setColor(30, 60, 200, 255);
            byte[] alphaMap = alphaMap();
            int[] deltas = new int[80];
            for (int row = 0; row < 4; row++) {
                int width = fillRampDeltas(deltas, 4);
                pr.emitAndClearAlphaRow(alphaMap, deltas, 2 + row, 3, 3 + width - 1, 4, row);
                assertArrayEquals(new int[80], deltas, "deltas zeroed after row " + row);
            }
            int width = fillRampDeltas(deltas, 0);
            pr.emitAndClearAlphaRow(alphaMap, deltas, 6, 40, 40 + width - 1, 4);
            assertArrayEquals(new int[80], deltas, "deltas zeroed after the six-argument overload");
            snapshot("emitAndClearAlphaRow, solid colour");

            pr.setLinearGradient(toS(40f), toS(8f), toS(56f), toS(8f), fractions, rgba,
                    GradientColorMap.CYCLE_REFLECT, null);
            for (int row = 0; row < 3; row++) {
                fillRampDeltas(deltas, 4);
                pr.emitAndClearAlphaRow(alphaMap, deltas, 8 + row, 40, 55, 4, row);
                assertArrayEquals(new int[80], deltas, "deltas zeroed after gradient row " + row);
            }
            snapshot("emitAndClearAlphaRow, gradient paint");

            int[] block = new int[3 + 6 * 4];
            for (int i = 0; i < block.length; i++) {
                block[i] = 0xFF000000 | (i * 0x0A0B0C);
            }
            surface.setRGB(block, 3, 6, 58, 58, 4, 4);
            int[] back = new int[W * H];
            surface.getRGB(back, 0, W, 0, 0, W, H);
            assertArrayEquals(pixels, back, "getRGB of the whole surface");
            snapshot("setRGB block");
        }

        private void drawImage(int mode, int[] data, int w, int h, int offset, int stride, Transform6 tx,
                boolean repeat, boolean linear, float bx, float by, float bw, float bh, int edge) {
            drawImage(mode, data, w, h, offset, stride, tx, repeat, linear, bx, by, bw, bh,
                    edge, edge, edge, edge, 0, 0, w - 1, h - 1);
        }

        private void drawImage(int mode, int[] data, int w, int h, int offset, int stride, Transform6 tx,
                boolean repeat, boolean linear, float bx, float by, float bw, float bh,
                int lEdge, int rEdge, int tEdge, int bEdge, int txMin, int tyMin, int txMax, int tyMax) {
            pr.drawImage(RendererBase.TYPE_INT_ARGB_PRE, mode, data, w, h, offset, stride, tx, repeat, linear,
                    toS(bx), toS(by), toS(bw), toS(bh), lEdge, rEdge, tEdge, bEdge, txMin, tyMin, txMax, tyMax,
                    true);
        }

        void writeGolden(Path path) throws IOException {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(path)))) {
                for (Step step : steps) {
                    for (int pixel : step.snapshot()) {
                        out.writeInt(pixel);
                    }
                }
            }
            System.out.println("captured " + steps.size() + " snapshots (" + steps.size() * W * H * 4
                    + " bytes) to " + path);
        }

        void assertMatchesGolden() throws IOException {
            byte[] bytes;
            try (InputStream in = PiscesGoldenRenderTest.class.getResourceAsStream(GOLDEN_RESOURCE)) {
                assertNotNull(in, "golden resource " + GOLDEN_RESOURCE + " is missing next to "
                        + PiscesGoldenRenderTest.class.getName());
                bytes = in.readAllBytes();
            }
            assertEquals(steps.size() * W * H * 4, bytes.length, "golden size: " + steps.size()
                    + " snapshots of " + W + "x" + H + " ints expected; the script and the golden disagree");
            List<Mismatch> mismatches = compare(steps, frames(bytes, steps.size()));
            LEDGER.compared(steps.size());
            if (!mismatches.isEmpty()) {
                fail(describe(mismatches, steps.size()));
            }
        }
    }
}
